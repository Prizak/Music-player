package com.example.musicplayer.ui.components

fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L)) / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

/** Tempo total ouvido: "12 h 34 min", "34 min" ou "menos de 1 min". */
fun formatListenTime(ms: Long): String {
    val totalMin = ms.coerceAtLeast(0L) / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 -> "$h h %02d min".format(m)
        totalMin > 0 -> "$m min"
        else -> "menos de 1 min"
    }
}
