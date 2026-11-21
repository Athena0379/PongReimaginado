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
import kotlin.math.abs

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

    // Posição central, velocidade (px/s) e raio da bola
    private var ballX: Float = 0f
    private var ballY: Float = 0f
    private var ballVx: Float = 420f
    private var ballVy: Float = 680f
    private val baseSpeed: Float = 760f
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
                resetBall(w.toFloat(), h.toFloat(), launchDown = true)
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

    private fun resetBall(w: Float, h: Float, launchDown: Boolean) {
        ballX = w / 2f
        ballY = h * 0.45f
        val dirX = if (ballVx >= 0f) 1f else -1f
        ballVx = dirX * (baseSpeed * 0.55f)
        ballVy = if (launchDown) baseSpeed else -baseSpeed
    }

    private fun updateBall(deltaTime: Float, currentBounds: GameBounds) {
        val leftBound = currentBounds.minBounds.x
        val rightBound = currentBounds.maxBounds.x
        val topBound = currentBounds.minBounds.y
        val bottomBound = currentBounds.maxBounds.y

        ballX += ballVx * deltaTime
        ballY += ballVy * deltaTime

        // Colisão com paredes laterais
        if (ballX - ballRadius < leftBound) {
            ballX = leftBound + ballRadius
            ballVx = abs(ballVx)
        } else if (ballX + ballRadius > rightBound) {
            ballX = rightBound - ballRadius
            ballVx = -abs(ballVx)
        }

        // Colisão com a raquete lunar (CPU no topo) quando a bola está subindo
        val cpuLeft = cpu.rect.x
        val cpuRight = cpu.rect.x + cpu.rect.width
        val cpuTop = cpu.rect.y
        val cpuBottom = cpu.rect.y + cpu.rect.height

        if (ballVy < 0f &&
            ballY - ballRadius <= cpuBottom &&
            ballY + ballRadius >= cpuTop &&
            ballX + ballRadius >= cpuLeft &&
            ballX - ballRadius <= cpuRight
        ) {
            ballY = cpuBottom + ballRadius
            val paddleCenter = cpuLeft + cpu.rect.width / 2f
            val hitOffset = ((ballX - paddleCenter) / (cpu.rect.width / 2f)).coerceIn(-1f, 1f)
            ballVx = hitOffset * (baseSpeed * 0.85f)
            ballVy = abs(ballVy)
        }

        // Colisão com a raquete solar (Player embaixo) quando a bola está descendo
        val pLeft = player.rect.x
        val pRight = player.rect.x + player.rect.width
        val pTop = player.rect.y
        val pBottom = player.rect.y + player.rect.height

        if (ballVy > 0f &&
            ballY + ballRadius >= pTop &&
            ballY - ballRadius <= pBottom &&
            ballX + ballRadius >= pLeft &&
            ballX - ballRadius <= pRight
        ) {
            ballY = pTop - ballRadius
            val paddleCenter = pLeft + player.rect.width / 2f
            val hitOffset = ((ballX - paddleCenter) / (player.rect.width / 2f)).coerceIn(-1f, 1f)
            ballVx = hitOffset * (baseSpeed * 0.85f)
            ballVy = -abs(ballVy)
        }

        // Saiu por cima (ponto do Player) ou por baixo (ponto da CPU) -> reinicia no centro
        if (ballY + ballRadius < topBound) {
            resetBall(rightBound - leftBound, bottomBound - topBound, launchDown = true)
        } else if (ballY - ballRadius > bottomBound) {
            resetBall(rightBound - leftBound, bottomBound - topBound, launchDown = false)
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

        // 2. Bola se movendo e quicando
        updateBall(deltaTime, currentBounds)

        // 3. CPU (Lunar - topo) acompanhando a bola
        cpu.updateTowardBall(ballX, currentBounds)

        updateEnemyPaddleVisual()
        updatePaddleShader()
        updateBallShaders()
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
        val pLeft = player.rect.x
        val pTop = player.rect.y
        val pRight = pLeft + player.rect.width
        val pBottom = pTop + player.rect.height

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