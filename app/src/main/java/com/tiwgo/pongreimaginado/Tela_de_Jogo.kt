package com.tiwgo.pongreimaginado

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Choreographer
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Tela_de_Jogo : AppCompatActivity(), Choreographer.FrameCallback, SensorEventListener {

    var isRunning: Boolean = false
        private set

    private var lastFrameTimeNanos: Long = 0L

    private lateinit var pongCanvasView: PongCanvasView
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_de_jogo)

        pongCanvasView = findViewById(R.id.pongCanvasView)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        start()
    }

    override fun onPause() {
        super.onPause()
        stop()
        sensorManager.unregisterListener(this)
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        lastFrameTimeNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
        }

        val rawDelta = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0f
        val deltaTime = rawDelta.coerceIn(0f, 0.05f)
        lastFrameTimeNanos = frameTimeNanos

        onUpdate(deltaTime)

        if (isRunning) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun onUpdate(deltaTime: Float) {
        pongCanvasView.update(deltaTime)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            pongCanvasView.accelerationX = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sem necessidade no teste inicial
    }
}