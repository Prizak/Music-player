package com.example.musicplayer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Ícones próprios (evita depender de material-icons-extended). */
private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = block)
    }.build()

object ResIcons {
    val Play = icon("Play") { moveTo(8f, 5f); verticalLineTo(19f); lineTo(19f, 12f); close() }
    val Pause = icon("Pause") {
        moveTo(6f, 5f); horizontalLineTo(10f); verticalLineTo(19f); horizontalLineTo(6f); close()
        moveTo(14f, 5f); horizontalLineTo(18f); verticalLineTo(19f); horizontalLineTo(14f); close()
    }
    val SkipNext = icon("SkipNext") {
        moveTo(6f, 18f); lineTo(14.5f, 12f); lineTo(6f, 6f); close()
        moveTo(16f, 6f); horizontalLineTo(18f); verticalLineTo(18f); horizontalLineTo(16f); close()
    }
    val SkipPrevious = icon("SkipPrevious") {
        moveTo(6f, 6f); horizontalLineTo(8f); verticalLineTo(18f); horizontalLineTo(6f); close()
        moveTo(9.5f, 12f); lineTo(18f, 18f); verticalLineTo(6f); close()
    }
    val Search = icon("Search") {
        moveTo(15.5f, 14f); horizontalLineTo(14.71f); lineTo(14.43f, 13.73f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
        curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
        lineTo(14f, 14.71f); verticalLineTo(15.5f); lineTo(19f, 20.49f); lineTo(20.49f, 19f); lineTo(15.5f, 14f); close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
        curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f); close()
    }
    val Close = icon("Close") {
        moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f); lineTo(5f, 6.41f)
        lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f); lineTo(12f, 13.41f); lineTo(17.59f, 19f)
        lineTo(19f, 17.59f); lineTo(13.41f, 12f); close()
    }
    val ExpandMore = icon("ExpandMore") {
        moveTo(7.41f, 8.59f); lineTo(12f, 13.17f); lineTo(16.59f, 8.59f); lineTo(18f, 10f); lineTo(12f, 16f); lineTo(6f, 10f); close()
    }
    val MusicNote = icon("MusicNote") {
        moveTo(12f, 3f); verticalLineTo(13.55f)
        curveTo(11.41f, 13.21f, 10.73f, 13f, 10f, 13f)
        curveTo(7.79f, 13f, 6f, 14.79f, 6f, 17f)
        curveTo(6f, 19.21f, 7.79f, 21f, 10f, 21f)
        curveTo(12.21f, 21f, 14f, 19.21f, 14f, 17f)
        verticalLineTo(7f); horizontalLineTo(18f); verticalLineTo(3f); close()
    }
    val Favorite = icon("Favorite") {
        moveTo(12f, 21.35f); lineTo(10.55f, 20.03f)
        curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
        curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
        curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
        curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
        curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
        curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
        close()
    }
    val FavoriteBorder = androidx.compose.ui.graphics.vector.ImageVector.Builder("FavoriteBorder", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd) {
            moveTo(12f, 21.35f); lineTo(10.55f, 20.03f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
            close()
            moveTo(12f, 18.55f); lineTo(12.1f, 18.45f)
            curveTo(16.86f, 14.14f, 20f, 11.29f, 20f, 8.5f)
            curveTo(20f, 6.5f, 18.5f, 5f, 16.5f, 5f)
            curveTo(14.96f, 5f, 13.46f, 6f, 12.94f, 7.36f)
            horizontalLineTo(11.07f)
            curveTo(10.54f, 6f, 9.04f, 5f, 7.5f, 5f)
            curveTo(5.5f, 5f, 4f, 6.5f, 4f, 8.5f)
            curveTo(4f, 11.29f, 7.14f, 14.14f, 11.9f, 18.45f)
            close()
        }
    }.build()
    val Shuffle = icon("Shuffle") {
        moveTo(10.59f, 9.17f); lineTo(5.41f, 4f); lineTo(4f, 5.41f); lineTo(9.17f, 10.58f); close()
        moveTo(14.5f, 4f); lineTo(16.54f, 6.04f); lineTo(4f, 18.59f); lineTo(5.41f, 20f); lineTo(17.96f, 7.46f)
        lineTo(20f, 9.5f); verticalLineTo(4f); close()
        moveTo(14.83f, 13.41f); lineTo(13.42f, 14.82f); lineTo(16.55f, 17.95f); lineTo(14.5f, 20f); horizontalLineTo(20f)
        verticalLineTo(14.5f); lineTo(17.96f, 16.54f); close()
    }
    val Repeat = icon("Repeat") {
        moveTo(7f, 7f); horizontalLineTo(17f); verticalLineTo(10f); lineTo(21f, 6f); lineTo(17f, 2f); verticalLineTo(5f)
        horizontalLineTo(5f); verticalLineTo(11f); horizontalLineTo(7f); close()
        moveTo(17f, 17f); horizontalLineTo(7f); verticalLineTo(14f); lineTo(3f, 18f); lineTo(7f, 22f); verticalLineTo(19f)
        horizontalLineTo(19f); verticalLineTo(13f); horizontalLineTo(17f); close()
    }
    /** Repeat com "1" no centro (modo ONE). */
    val RepeatOne = icon("RepeatOne") {
        moveTo(7f, 7f); horizontalLineTo(17f); verticalLineTo(10f); lineTo(21f, 6f); lineTo(17f, 2f); verticalLineTo(5f)
        horizontalLineTo(5f); verticalLineTo(11f); horizontalLineTo(7f); close()
        moveTo(17f, 17f); horizontalLineTo(7f); verticalLineTo(14f); lineTo(3f, 18f); lineTo(7f, 22f); verticalLineTo(19f)
        horizontalLineTo(19f); verticalLineTo(13f); horizontalLineTo(17f); close()
        moveTo(13f, 15f); verticalLineTo(9f); horizontalLineTo(11.5f); lineTo(10f, 10.2f); lineTo(10.6f, 11.3f)
        lineTo(11.5f, 10.7f); verticalLineTo(15f); close()
    }
    val Tune = icon("Tune") {
        moveTo(3f, 17f); verticalLineTo(19f); horizontalLineTo(9f); verticalLineTo(17f); close()
        moveTo(3f, 5f); verticalLineTo(7f); horizontalLineTo(13f); verticalLineTo(5f); close()
        moveTo(13f, 21f); verticalLineTo(19f); horizontalLineTo(21f); verticalLineTo(17f); horizontalLineTo(13f)
        verticalLineTo(15f); horizontalLineTo(11f); verticalLineTo(21f); close()
        moveTo(7f, 9f); verticalLineTo(11f); horizontalLineTo(3f); verticalLineTo(13f); horizontalLineTo(7f)
        verticalLineTo(15f); horizontalLineTo(9f); verticalLineTo(9f); close()
        moveTo(21f, 13f); verticalLineTo(11f); horizontalLineTo(11f); verticalLineTo(13f); close()
        moveTo(15f, 9f); horizontalLineTo(17f); verticalLineTo(7f); horizontalLineTo(21f); verticalLineTo(5f)
        horizontalLineTo(17f); verticalLineTo(3f); horizontalLineTo(15f); close()
    }
    val Queue = icon("Queue") {
        moveTo(3f, 6f); horizontalLineTo(15f); verticalLineTo(8f); horizontalLineTo(3f); close()
        moveTo(3f, 10f); horizontalLineTo(15f); verticalLineTo(12f); horizontalLineTo(3f); close()
        moveTo(3f, 14f); horizontalLineTo(11f); verticalLineTo(16f); horizontalLineTo(3f); close()
        moveTo(17f, 10f); verticalLineTo(18.55f); curveTo(16.59f, 18.21f, 16.06f, 18f, 15.5f, 18f)
        curveTo(14.12f, 18f, 13f, 19.12f, 13f, 20.5f); curveTo(13f, 21.88f, 14.12f, 23f, 15.5f, 23f)
        curveTo(16.88f, 23f, 18f, 21.88f, 18f, 20.5f); verticalLineTo(12f); horizontalLineTo(21f); verticalLineTo(10f); close()
    }
    val DarkMode = icon("DarkMode") {
        moveTo(12f, 3f); curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f); curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
        curveTo(15.9f, 21f, 19.2f, 18.5f, 20.4f, 15f); curveTo(15.5f, 16f, 10.5f, 11.5f, 12f, 3f); close()
    }
    val LightMode = icon("LightMode") {
        moveTo(12f, 7f); curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f); curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
        curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f); curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f); close()
        moveTo(11f, 1f); horizontalLineTo(13f); verticalLineTo(4f); horizontalLineTo(11f); close()
        moveTo(11f, 20f); horizontalLineTo(13f); verticalLineTo(23f); horizontalLineTo(11f); close()
        moveTo(1f, 11f); horizontalLineTo(4f); verticalLineTo(13f); horizontalLineTo(1f); close()
        moveTo(20f, 11f); horizontalLineTo(23f); verticalLineTo(13f); horizontalLineTo(20f); close()
    }
    val MoreVert = icon("MoreVert") {
        moveTo(12f, 8f); curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f); curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
        curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f); curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f); close()
        moveTo(12f, 10f); curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f); curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
        curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f); curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f); close()
        moveTo(12f, 16f); curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f); curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
        curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f); curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f); close()
    }
    val Add = icon("Add") {
        moveTo(19f, 13f); horizontalLineTo(13f); verticalLineTo(19f); horizontalLineTo(11f); verticalLineTo(13f)
        horizontalLineTo(5f); verticalLineTo(11f); horizontalLineTo(11f); verticalLineTo(5f); horizontalLineTo(13f)
        verticalLineTo(11f); horizontalLineTo(19f); close()
    }
    val ArrowBack = icon("ArrowBack") {
        moveTo(20f, 11f); horizontalLineTo(7.83f); lineTo(13.42f, 5.41f); lineTo(12f, 4f); lineTo(4f, 12f)
        lineTo(12f, 20f); lineTo(13.41f, 18.59f); lineTo(7.83f, 13f); horizontalLineTo(20f); close()
    }
    val Leaderboard = icon("Leaderboard") {
        moveTo(7.5f, 21f); horizontalLineTo(2f); verticalLineTo(9f); horizontalLineTo(7.5f); close()
        moveTo(14.75f, 3f); horizontalLineTo(9.25f); verticalLineTo(21f); horizontalLineTo(14.75f); close()
        moveTo(22f, 11f); horizontalLineTo(16.5f); verticalLineTo(21f); horizontalLineTo(22f); close()
    }
    val Settings = icon("Settings") {
        moveTo(19.14f, 12.94f); curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f); curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
        lineTo(21.16f, 9.48f); curveTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f); lineTo(19.36f, 5.55f)
        curveTo(19.24f, 5.33f, 18.99f, 5.26f, 18.77f, 5.33f); lineTo(16.38f, 6.29f); curveTo(15.88f, 5.91f, 15.35f, 5.59f, 14.76f, 5.35f)
        lineTo(14.4f, 2.81f); curveTo(14.36f, 2.57f, 14.16f, 2.4f, 13.92f, 2.4f); horizontalLineTo(10.08f)
        curveTo(9.84f, 2.4f, 9.65f, 2.57f, 9.61f, 2.81f); lineTo(9.25f, 5.35f); curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
        lineTo(5.24f, 5.33f); curveTo(5.02f, 5.25f, 4.77f, 5.33f, 4.65f, 5.55f); lineTo(2.74f, 8.87f)
        curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f); lineTo(4.89f, 11.06f); curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12f)
        curveTo(4.8f, 12.31f, 4.82f, 12.64f, 4.87f, 12.94f); lineTo(2.84f, 14.52f); curveTo(2.66f, 14.66f, 2.61f, 14.93f, 2.72f, 15.13f)
        lineTo(4.64f, 18.45f); curveTo(4.76f, 18.67f, 5.01f, 18.74f, 5.23f, 18.67f); lineTo(7.62f, 17.71f)
        curveTo(8.12f, 18.09f, 8.65f, 18.41f, 9.24f, 18.65f); lineTo(9.6f, 21.19f); curveTo(9.65f, 21.43f, 9.84f, 21.6f, 10.08f, 21.6f)
        horizontalLineTo(13.92f); curveTo(14.16f, 21.6f, 14.36f, 21.43f, 14.39f, 21.19f); lineTo(14.75f, 18.65f)
        curveTo(15.34f, 18.41f, 15.88f, 18.09f, 16.37f, 17.71f); lineTo(18.76f, 18.67f); curveTo(18.98f, 18.75f, 19.23f, 18.67f, 19.35f, 18.45f)
        lineTo(21.27f, 15.13f); curveTo(21.39f, 14.91f, 21.34f, 14.66f, 21.15f, 14.52f); lineTo(19.14f, 12.94f); close()
        moveTo(12f, 15.6f); curveTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12f); curveTo(8.4f, 10.02f, 10.02f, 8.4f, 12f, 8.4f)
        curveTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12f); curveTo(15.6f, 13.98f, 13.98f, 15.6f, 12f, 15.6f); close()
    }
}
