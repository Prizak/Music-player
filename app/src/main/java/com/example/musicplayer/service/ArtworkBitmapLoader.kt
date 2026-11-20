package com.example.musicplayer.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors

/**
 * Carrega a capa da notificação/tela de bloqueio. Sem cache próprio (cada faixa pede a sua),
 * então nunca reaproveita o bitmap de outra faixa. API 29+: `loadThumbnail` da URI da faixa;
 * antes: stream da URI legada de álbum.
 */
class ArtworkBitmapLoader(context: Context) : BitmapLoader {
    private val appContext = context.applicationContext
    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = executor.submit<Bitmap> {
        BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Bitmap inválido")
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = executor.submit<Bitmap> {
        val resolver = appContext.contentResolver
        if (Build.VERSION.SDK_INT >= 29) {
            resolver.loadThumbnail(uri, Size(SIZE, SIZE), null)
        } else {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: error("Sem capa")
        }
    }

    fun shutdown() { executor.shutdown() }

    private companion object { const val SIZE = 512 }
}
