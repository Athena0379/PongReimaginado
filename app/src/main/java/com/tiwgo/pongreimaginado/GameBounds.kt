package com.tiwgo.pongreimaginado

class GameBounds(
    val screenSize: Vector2,
    val margin: Float = 0f
) {
    val minBounds: Vector2 = Vector2(margin, margin)
    val maxBounds: Vector2 = Vector2(screenSize.x - margin, screenSize.y - margin)

    fun keepInside(rect: GameRect) {
        val rightLimit = (maxBounds.x - rect.width).coerceAtLeast(minBounds.x)
        val bottomLimit = (maxBounds.y - rect.height).coerceAtLeast(minBounds.y)

        if (rect.x < minBounds.x) {
            rect.x = minBounds.x
        } else if (rect.x > rightLimit) {
            rect.x = rightLimit
        }

        if (rect.y < minBounds.y) {
            rect.y = minBounds.y
        } else if (rect.y > bottomLimit) {
            rect.y = bottomLimit
        }
    }
}