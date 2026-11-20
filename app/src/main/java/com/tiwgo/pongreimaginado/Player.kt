package com.tiwgo.pongreimaginado

import kotlin.math.abs

class Player(
    customWidth: Float = 220f,
    customHeight: Float = 45f
) {
    val rect: GameRect = GameRect(
        position = Vector2(0f, 0f),
        size = Vector2(customWidth, customHeight)
    )

    private val speedMultiplier: Float = 15f
    private var velocity: Vector2 = Vector2(0f, 0f)

    fun spawnAtStartLine(bounds: GameBounds) {
        val centerX = bounds.minBounds.x + (bounds.maxBounds.x - bounds.minBounds.x) / 2f

        rect.x = centerX
        rect.y = bounds.maxBounds.y - (rect.height / 2f)

        bounds.keepInside(rect)
    }

    fun updateWithAccelerometer(acceleration: Vector2, bounds: GameBounds) {
        val deadZone = 0.5f
        val rawX = if (abs(acceleration.x) > deadZone) acceleration.x else 0f

        val smoothing = 0.1f
        val targetVelocityX = -rawX * speedMultiplier

        velocity.x += (targetVelocityX - velocity.x) * smoothing

        val maxSpeed = 25f
        velocity.x = velocity.x.coerceIn(-maxSpeed, maxSpeed)

        val oldX = rect.x

        rect.x += velocity.x

        bounds.keepInside(rect)

        val touchedLeft = (rect.x == bounds.minBounds.x && oldX <= bounds.minBounds.x)
        val touchedRight = (rect.x == bounds.maxBounds.x - rect.width && oldX >= bounds.maxBounds.x - rect.width)

        if (touchedLeft || touchedRight) {
            velocity.x = 0f
        }
    }
}