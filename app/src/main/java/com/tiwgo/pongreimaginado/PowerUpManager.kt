package com.tiwgo.pongreimaginado

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.hypot
import kotlin.random.Random

data class PowerUpOrb(
    var x: Float = 0f,
    var y: Float = 0f,
    val radius: Float = 20f,
    val auraRadius: Float = 34f,
    var isActive: Boolean = false
)

class PowerUpManager(
    private val random: Random = Random.Default
) {
    val orb = PowerUpOrb()

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F7FA")
        style = Paint.Style.FILL
    }

    fun spawnRandomly(
        bounds: GameBounds,
        topSafeMargin: Float = 180f,
        bottomSafeMargin: Float = 180f
    ) {
        val minX = bounds.minBounds.x + orb.auraRadius + 16f
        val maxX = bounds.maxBounds.x - orb.auraRadius - 16f

        val minY = bounds.minBounds.y + topSafeMargin
        val maxY = bounds.maxBounds.y - bottomSafeMargin

        if (maxX <= minX || maxY <= minY) {
            orb.x = (bounds.minBounds.x + bounds.maxBounds.x) / 2f
            orb.y = (bounds.minBounds.y + bounds.maxBounds.y) / 2f
        } else {
            orb.x = random.nextFloat() * (maxX - minX) + minX
            orb.y = random.nextFloat() * (maxY - minY) + minY
        }

        orb.isActive = true
        updateShaders()
    }

    fun isHitByBall(ballX: Float, ballY: Float, ballRadius: Float): Boolean {
        if (!orb.isActive) return false
        val distance = hypot(ballX - orb.x, ballY - orb.y)
        return distance <= (ballRadius + orb.radius)
    }

    private fun updateShaders() {
        auraPaint.shader = RadialGradient(
            orb.x, orb.y, orb.auraRadius,
            intArrayOf(
                Color.parseColor("#B300E5FF"),
                Color.parseColor("#0000E5FF")
            ),
            floatArrayOf(0.25f, 1f),
            Shader.TileMode.CLAMP
        )

        corePaint.shader = RadialGradient(
            orb.x - orb.radius * 0.25f,
            orb.y - orb.radius * 0.25f,
            orb.radius * 1.15f,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#26C6DA"),
                Color.parseColor("#7C4DFF")
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun draw(canvas: Canvas) {
        if (!orb.isActive) return

        canvas.drawCircle(orb.x, orb.y, orb.auraRadius, auraPaint)
        canvas.drawCircle(orb.x, orb.y, orb.radius, corePaint)
        canvas.drawCircle(
            orb.x - orb.radius * 0.35f,
            orb.y - orb.radius * 0.35f,
            orb.radius * 0.18f,
            highlightPaint
        )
    }
}