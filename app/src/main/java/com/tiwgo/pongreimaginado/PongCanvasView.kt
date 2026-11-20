package com.tiwgo.pongreimaginado

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class PongCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.bg_eclipse)

    val player = Player(
        customWidth = 220f,
        customHeight = 45f
    )

    val cpu = CpuPaddle(
        customWidth = 220f,
        customHeight = 45f,
        topOffset = 90f,
        maxSpeed = 11f
    )

    var bounds: GameBounds? = null
        private set

    var isInitialized: Boolean = false
        private set

    var accelerationX: Float = 0f

    // Posição central e raio da bola
    private var ballX: Float = 0f
    private var ballY: Float = 0f
    private val ballRadius: Float = 26f
    private val auraRadius: Float = 44f

    // Raquete lunar (CPU no topo)
    private val enemyRect = RectF()
    private val enemyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val enemyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E3F2FD")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // Paints reutilizados no onDraw
    private val paddleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFB300")
        style = Paint.Style.FILL
    }
    private val paddleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paddleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF8E1")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val ballAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ballCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ballHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        style = Paint.Style.FILL
    }

    private val paddleGlowRect = RectF()
    private val paddleRectF = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val newBounds = GameBounds(
                screenSize = Vector2(w.toFloat(), h.toFloat()),
                margin = 0f
            )
            bounds = newBounds

            if (!isInitialized) {
                player.spawnAtStartLine(newBounds)
                cpu.spawnAtTopLine(newBounds)
                ballX = w / 2f
                ballY = h * 0.35f
                isInitialized = true
            } else {
                bounds?.keepInside(player.rect)
                bounds?.keepInside(cpu.rect)
                ballX = ballX.coerceIn(ballRadius, w - ballRadius)
                ballY = ballY.coerceIn(ballRadius, h - ballRadius)
            }

            updateEnemyPaddleVisual()
            updatePaddleShader()
            updateBallShaders()
        }
    }

    private fun updateEnemyPaddleVisual() {
        val left = cpu.rect.left
        val top = cpu.rect.top
        val right = cpu.rect.right
        val bottom = cpu.rect.bottom

        enemyRect.set(left, top, right, bottom)

        if (right > left) {
            enemyFillPaint.shader = LinearGradient(
                left, top, right, bottom,
                intArrayOf(
                    Color.parseColor("#546E7A"),
                    Color.parseColor("#CFD8DC"),
                    Color.parseColor("#546E7A")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun updatePaddleShader() {
        val left = player.rect.left
        val right = player.rect.right
        val top = player.rect.top
        val bottom = player.rect.bottom

        if (right > left) {
            paddleFillPaint.shader = LinearGradient(
                left, top, right, bottom,
                intArrayOf(
                    Color.parseColor("#FF8F00"),
                    Color.parseColor("#FFD54F"),
                    Color.parseColor("#FF8F00")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun updateBallShaders() {
        if (ballX == 0f && ballY == 0f) return

        ballAuraPaint.shader = RadialGradient(
            ballX, ballY, auraRadius,
            intArrayOf(
                Color.parseColor("#AAFF9100"),
                Color.parseColor("#00FF9100")
            ),
            floatArrayOf(0.25f, 1f),
            Shader.TileMode.CLAMP
        )

        ballCorePaint.shader = RadialGradient(
            ballX - ballRadius * 0.25f,
            ballY - ballRadius * 0.25f,
            ballRadius * 1.1f,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#FFD54F"),
                Color.parseColor("#FF6F00")
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun update(deltaTime: Float) {
        val currentBounds = bounds ?: return
        if (!isInitialized) return

        // 1. Player (Solar - embaixo) via acelerômetro
        player.updateWithAccelerometer(Vector2(accelerationX, 0f), currentBounds)

        // 2. CPU (Lunar - topo) acompanhando a bola
        cpu.updateTowardBall(ballX, currentBounds)

        updateEnemyPaddleVisual()
        updatePaddleShader()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Background
        backgroundDrawable?.setBounds(0, 0, width, height)
        backgroundDrawable?.draw(canvas)

        if (!isInitialized) return

        // 2. Raquete Lunar (CPU - topo)
        canvas.drawRoundRect(enemyRect, 14f, 14f, enemyFillPaint)
        canvas.drawRoundRect(enemyRect, 14f, 14f, enemyBorderPaint)

        // 3. Raquete Solar (Player - embaixo)
        val pLeft = player.rect.left
        val pTop = player.rect.top
        val pRight = player.rect.right
        val pBottom = player.rect.bottom

        paddleGlowRect.set(pLeft - 6f, pTop - 6f, pRight + 6f, pBottom + 6f)
        canvas.drawRoundRect(paddleGlowRect, 18f, 18f, paddleGlowPaint)

        paddleRectF.set(pLeft, pTop, pRight, pBottom)
        canvas.drawRoundRect(paddleRectF, 14f, 14f, paddleFillPaint)
        canvas.drawRoundRect(paddleRectF, 14f, 14f, paddleBorderPaint)

        // 4. Bola (Eclipse)
        canvas.drawCircle(ballX, ballY, auraRadius, ballAuraPaint)
        canvas.drawCircle(ballX, ballY, ballRadius, ballCorePaint)
        canvas.drawCircle(
            ballX - ballRadius * 0.35f,
            ballY - ballRadius * 0.35f,
            ballRadius * 0.18f,
            ballHighlightPaint
        )
    }
}