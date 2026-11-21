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
import kotlin.random.Random

data class GameBall(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float = 26f,
    val auraRadius: Float = 44f
)

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
        topOffset = 90f
    )

    val powerUpManager = PowerUpManager()

    var onGameOver: (() -> Unit)? = null

    var score: Int = 0
        private set

    var isPaused: Boolean = false
        private set

    var bounds: GameBounds? = null
        private set

    var isInitialized: Boolean = false
        private set

    var accelerationX: Float = 0f

    private val baseSpeed: Float = 760f
    private val balls = mutableListOf<GameBall>()
    private var isGameOverTriggered = false

    // Raquete lunar (topo - alinhada ao X do player)
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
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFFFFF")
        textSize = 72f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
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
    private val pauseOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }
    private val pauseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 76f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val pauseHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F7FA")
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val paddleGlowRect = RectF()
    private val paddleRectF = RectF()

    fun togglePause(): Boolean {
        isPaused = !isPaused
        invalidate()
        return isPaused
    }

    fun setPaused(paused: Boolean) {
        if (isPaused == paused) return
        isPaused = paused
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val newBounds = GameBounds(
                screenSize = Vector2(w.toFloat(), h.toFloat()),
                margin = 0f
            )
            bounds = newBounds

            if (!isInitialized) {
                score = 0
                player.spawnAtStartLine(newBounds)
                cpu.spawnAtTopLine(newBounds)
                syncCpuXWithPlayer()
                balls.clear()
                createBall(
                    x = w / 2f,
                    y = h * 0.45f,
                    vx = baseSpeed * 0.55f,
                    vy = baseSpeed
                )
                powerUpManager.spawnRandomly(newBounds)
                isInitialized = true
            } else {
                bounds?.keepInside(player.rect)
                syncCpuXWithPlayer()
                bounds?.keepInside(cpu.rect)
                balls.forEach { ball ->
                    ball.x = ball.x.coerceIn(ball.radius, w - ball.radius)
                    ball.y = ball.y.coerceIn(ball.radius, h - ball.radius)
                }
            }

            updateEnemyPaddleVisual()
            updatePaddleShader()
        }
    }

    /**
     * Cria e adiciona uma nova bolinha amarela na partida.
     */
    fun createBall(
        x: Float,
        y: Float,
        vx: Float? = null,
        vy: Float? = null
    ) {
        val dirX = if (Random.nextBoolean()) 1f else -1f
        val dirY = if (Random.nextBoolean()) 1f else -1f

        val newBall = GameBall(
            x = x,
            y = y,
            vx = vx ?: (dirX * baseSpeed * 0.6f),
            vy = vy ?: (dirY * baseSpeed)
        )
        balls.add(newBall)
    }

    private fun spawnNewBallFromPowerUp(sourceBall: GameBall, currentBounds: GameBounds) {
        createBall(
            x = sourceBall.x,
            y = sourceBall.y,
            vx = -sourceBall.vx,
            vy = if (sourceBall.vy != 0f) -sourceBall.vy else baseSpeed
        )
        powerUpManager.spawnRandomly(currentBounds)
    }

    private fun isPastPunishmentZone(ball: GameBall, currentBounds: GameBounds): Boolean {
        val passedTop = (ball.y + ball.radius) < currentBounds.minBounds.y
        val passedBottom = (ball.y - ball.radius) > currentBounds.maxBounds.y
        return passedTop || passedBottom
    }

    private fun syncCpuXWithPlayer() {
        cpu.rect.x = player.rect.x
        bounds?.keepInside(cpu.rect)
    }

    private fun updateBalls(deltaTime: Float, currentBounds: GameBounds) {
        if (isGameOverTriggered) return

        val leftBound = currentBounds.minBounds.x
        val rightBound = currentBounds.maxBounds.x

        val cpuLeft = cpu.rect.x
        val cpuRight = cpu.rect.x + cpu.rect.width
        val cpuTop = cpu.rect.y
        val cpuBottom = cpu.rect.y + cpu.rect.height

        val pLeft = player.rect.x
        val pRight = player.rect.x + player.rect.width
        val pTop = player.rect.y
        val pBottom = player.rect.y + player.rect.height

        val ballsThatConsumedPowerUp = mutableListOf<GameBall>()
        val iterator = balls.iterator()

        while (iterator.hasNext()) {
            val ball = iterator.next()

            ball.x += ball.vx * deltaTime
            ball.y += ball.vy * deltaTime

            // Colisão com paredes laterais
            if (ball.x - ball.radius < leftBound) {
                ball.x = leftBound + ball.radius
                ball.vx = abs(ball.vx)
            } else if (ball.x + ball.radius > rightBound) {
                ball.x = rightBound - ball.radius
                ball.vx = -abs(ball.vx)
            }

            // Colisão com a raquete do topo quando a bola está subindo -> +1 ponto
            if (ball.vy < 0f &&
                ball.y - ball.radius <= cpuBottom &&
                ball.y + ball.radius >= cpuTop &&
                ball.x + ball.radius >= cpuLeft &&
                ball.x - ball.radius <= cpuRight
            ) {
                ball.y = cpuBottom + ball.radius
                val paddleCenter = cpuLeft + cpu.rect.width / 2f
                val hitOffset = ((ball.x - paddleCenter) / (cpu.rect.width / 2f)).coerceIn(-1f, 1f)
                ball.vx = hitOffset * (baseSpeed * 0.85f)
                ball.vy = abs(ball.vy)
                score++
            }

            // Colisão com a raquete de baixo quando a bola está descendo -> +1 ponto
            if (ball.vy > 0f &&
                ball.y + ball.radius >= pTop &&
                ball.y - ball.radius <= pBottom &&
                ball.x + ball.radius >= pLeft &&
                ball.x - ball.radius <= pRight
            ) {
                ball.y = pTop - ball.radius
                val paddleCenter = pLeft + player.rect.width / 2f
                val hitOffset = ((ball.x - paddleCenter) / (player.rect.width / 2f)).coerceIn(-1f, 1f)
                ball.vx = hitOffset * (baseSpeed * 0.85f)
                ball.vy = -abs(ball.vy)
                score++
            }

            // Colisão com o power-up
            if (powerUpManager.isHitByBall(ball.x, ball.y, ball.radius)) {
                ballsThatConsumedPowerUp.add(ball)
            }

            // Zona de punição: destrói a bolinha em vez de resetar
            if (isPastPunishmentZone(ball, currentBounds)) {
                iterator.remove()
            }
        }

        // Gera nova bolinha e reposiciona o power-up após iterar com segurança
        for (source in ballsThatConsumedPowerUp) {
            spawnNewBallFromPowerUp(source, currentBounds)
        }

        // Sem bolinhas restantes -> encerra o jogo e vai para a tela de resultados
        if (balls.isEmpty() && !isGameOverTriggered) {
            isGameOverTriggered = true
            powerUpManager.orb.isActive = false
            onGameOver?.invoke()
        }
    }

    private fun updateEnemyPaddleVisual() {
        val left = cpu.rect.x
        val top = cpu.rect.y
        val right = left + cpu.rect.width
        val bottom = top + cpu.rect.height

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
        val left = player.rect.x
        val right = left + player.rect.width
        val top = player.rect.y
        val bottom = top + player.rect.height

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

    private fun applyBallShaders(ball: GameBall) {
        ballAuraPaint.shader = RadialGradient(
            ball.x, ball.y, ball.auraRadius,
            intArrayOf(
                Color.parseColor("#AAFF9100"),
                Color.parseColor("#00FF9100")
            ),
            floatArrayOf(0.25f, 1f),
            Shader.TileMode.CLAMP
        )

        ballCorePaint.shader = RadialGradient(
            ball.x - ball.radius * 0.25f,
            ball.y - ball.radius * 0.25f,
            ball.radius * 1.1f,
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
        if (isPaused) {
            invalidate()
            return
        }

        // 1. Player (Solar - embaixo) via acelerômetro
        player.updateWithAccelerometer(Vector2(accelerationX, 0f), currentBounds)

        // 2. Raquete do topo acompanha apenas o X do player
        syncCpuXWithPlayer()

        // 3. Atualiza bolinhas, power-up, pontuação e zona de punição
        updateBalls(deltaTime, currentBounds)

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

        // 2. Pontuação única centralizada
        canvas.drawText(score.toString(), width / 2f, height / 2f + 24f, scorePaint)

        // 3. Power-up
        powerUpManager.draw(canvas)

        // 4. Raquete Lunar (topo)
        canvas.drawRoundRect(enemyRect, 14f, 14f, enemyFillPaint)
        canvas.drawRoundRect(enemyRect, 14f, 14f, enemyBorderPaint)

        // 5. Raquete Solar (Player - embaixo)
        val pLeft = player.rect.x
        val pTop = player.rect.y
        val pRight = pLeft + player.rect.width
        val pBottom = pTop + player.rect.height

        paddleGlowRect.set(pLeft - 6f, pTop - 6f, pRight + 6f, pBottom + 6f)
        canvas.drawRoundRect(paddleGlowRect, 18f, 18f, paddleGlowPaint)

        paddleRectF.set(pLeft, pTop, pRight, pBottom)
        canvas.drawRoundRect(paddleRectF, 14f, 14f, paddleFillPaint)
        canvas.drawRoundRect(paddleRectF, 14f, 14f, paddleBorderPaint)

        // 6. Bolinhas amarelas ativas
        for (ball in balls) {
            applyBallShaders(ball)
            canvas.drawCircle(ball.x, ball.y, ball.auraRadius, ballAuraPaint)
            canvas.drawCircle(ball.x, ball.y, ball.radius, ballCorePaint)
            canvas.drawCircle(
                ball.x - ball.radius * 0.35f,
                ball.y - ball.radius * 0.35f,
                ball.radius * 0.18f,
                ballHighlightPaint
            )
        }

        // 7. Overlay de pausa
        if (isPaused) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), pauseOverlayPaint)
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.drawText("PAUSADO", centerX, centerY - 16f, pauseTitlePaint)
            canvas.drawText("Toque na tela para continuar", centerX, centerY + 46f, pauseHintPaint)
        }
    }
}