package com.tiwgo.pongreimaginado

import kotlin.math.abs

class CpuPaddle(
    customWidth: Float = 220f,
    customHeight: Float = 45f,
    private val topOffset: Float = 90f,
    private val maxSpeed: Float = 11f
) {
    val rect: GameRect = GameRect(
        position = Vector2(0f, 0f),
        size = Vector2(customWidth, customHeight)
    )

    private var velocityX: Float = 0f

    fun spawnAtTopLine(bounds: GameBounds) {
        val centerX = bounds.minBounds.x + (bounds.maxBounds.x - bounds.minBounds.x) / 2f
        rect.x = centerX
        rect.y = bounds.minBounds.y + topOffset + (rect.height / 2f)
        bounds.keepInside(rect)
    }

    fun updateTowardBall(targetBallX: Float, bounds: GameBounds) {
        val paddleCenterX = rect.x + (rect.width / 2f)
        val diffX = targetBallX - paddleCenterX
        val deadZone = 12f

        val desiredVelocityX = if (abs(diffX) > deadZone) {
            (diffX * 0.14f).coerceIn(-maxSpeed, maxSpeed)
        } else {
            0f
        }

        // Suavização para o movimento da CPU não ficar robótico demais
        velocityX += (desiredVelocityX - velocityX) * 0.2f
        rect.x += velocityX

        bounds.keepInside(rect)
    }
}