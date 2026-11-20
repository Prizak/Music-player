package com.example.musicplayer.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.Track
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** Cache de bitmaps já no tamanho alvo. Limite = 1/8 de Runtime.maxMemory(), entre 4 MB e 32 MB. */
private object CoverCache {
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8).coerceIn(4L * 1024 * 1024, 32L * 1024 * 1024).toInt()
    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    // Falhas recentes: limitadas (LRU de 256) e com TTL, para permitir nova tentativa.
    private const val MISS_TTL_MS = 60_000L
    private val misses = object : java.util.LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 256
    }
    private val inFlight = ConcurrentHashMap<String, Deferred<Bitmap?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(3))

    fun get(k: String): Bitmap? = cache.get(k)
    fun isMiss(k: String): Boolean = synchronized(misses) {
        val t = misses[k] ?: return false
        if (System.currentTimeMillis() - t > MISS_TTL_MS) { misses.remove(k); false } else true
    }
    fun clearMisses() = synchronized(misses) { misses.clear() }

    /** Uma única decodificação por chave, mesmo com vários pedidos simultâneos. */
    suspend fun load(key: String, block: () -> Bitmap?): Bitmap? {
        cache.get(key)?.let { return it }
        val job = inFlight.getOrPut(key) {
            // LAZY: só inicia no await(), depois de registrado em inFlight (evita o finally remover antes do put).
            scope.async(start = CoroutineStart.LAZY) {
                try {
                    block()?.also { cache.put(key, it) } ?: run { synchronized(misses) { misses[key] = System.currentTimeMillis() }; null }
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return job.await()
    }
}

private fun Bitmap.fitTo(targetPx: Int): Bitmap {
    val maxSide = maxOf(width, height)
    if (maxSide <= targetPx) return this
    val f = targetPx.toFloat() / maxSide
    val out = Bitmap.createScaledBitmap(this, (width * f).toInt().coerceAtLeast(1), (height * f).toInt().coerceAtLeast(1), true)
    if (out !== this) recycle()
    return out
}

/**
 * Carrega a capa (chamar em Dispatchers.IO). API 29+: thumbnail da URI da FAIXA (contentUri).
 * API 24-28: decodifica albumArtUri com downsample. Sempre devolve bitmap com lado maior <= [targetPx].
 */
private fun loadCover(context: Context, contentUri: String?, albumArtUri: String?, targetPx: Int): Bitmap? {
    val resolver = context.contentResolver
    return try {
        if (Build.VERSION.SDK_INT >= 29) {
            contentUri?.let { resolver.loadThumbnail(Uri.parse(it), Size(targetPx, targetPx), null).fitTo(targetPx) }
        } else {
            val uri = Uri.parse(albumArtUri ?: return null)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            while (opts.outWidth / (sample * 2) >= targetPx && opts.outHeight / (sample * 2) >= targetPx) sample *= 2
            val decode = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) }?.fitTo(targetPx)
        }
    } catch (_: Exception) {
        null
    }
}

/** Limpa falhas registradas (ex.: ao ganhar permissão de áudio) para permitir nova tentativa. */
fun resetCoverMisses() = CoverCache.clearMisses()

/** Tamanhos alvo em pixels reais (não dp). */
object CoverSize {
    const val LIST = 128
    const val NOW_PLAYING = 512
}

/** Atalho para uma [Track]. */
@Composable
fun AlbumArt(
    track: Track?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 24.dp,
    targetPx: Int = CoverSize.LIST
) = AlbumArt(track?.contentUri, track?.albumArtUri, modifier, cornerRadius, iconSize, targetPx)

/**
 * Capa da faixa. Placeholder em gradiente somente se não houver capa (ou falhar).
 * [contentUri] = Track.contentUri (usado na API 29+); [albumArtUri] só no fallback API 24-28.
 */
@Composable
fun AlbumArt(
    contentUri: String?,
    albumArtUri: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 24.dp,
    targetPx: Int = CoverSize.LIST
) {
    val context = LocalContext.current.applicationContext
    val source = if (Build.VERSION.SDK_INT >= 29) contentUri else albumArtUri
    val key = source?.let { "$it@$targetPx" }
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = key?.let { CoverCache.get(it)?.asImageBitmap() }, key
    ) {
        if (key == null) { value = null; return@produceState }
        // O estado do produceState sobrevive à troca de key: reavalia o cache para não manter a capa anterior.
        value = CoverCache.get(key)?.asImageBitmap()
        if (value == null && !CoverCache.isMiss(key)) {
            val bmp = CoverCache.load(key) { loadCover(context, contentUri, albumArtUri, targetPx) }
            value = bmp?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(cornerRadius)
    val img = bitmap
    if (img != null) {
        Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(shape))
    } else {
        val cs = MaterialTheme.colorScheme
        Box(
            modifier = modifier.clip(shape)
                .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.secondaryContainer))),
            contentAlignment = Alignment.Center
        ) {
            Icon(ResIcons.MusicNote, contentDescription = null, tint = cs.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(iconSize))
        }
    }
}
