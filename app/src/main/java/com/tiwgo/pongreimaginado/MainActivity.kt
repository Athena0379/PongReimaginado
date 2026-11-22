package com.tiwgo.pongreimaginado

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var saveManager: GameSaveManager
    private lateinit var btnStart: Button
    private lateinit var btnNew: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        saveManager = GameSaveManager(this)
        btnStart = findViewById(R.id.btn_start)
        btnNew = findViewById(R.id.btn_new)

        btnStart.setOnClickListener {
            saveManager.clearSave()
            val intent = Intent(this, Tela_de_Jogo::class.java).apply {
                putExtra(GameSaveManager.EXTRA_LOAD_SAVED_GAME, false)
            }
            startActivity(intent)
        }

        btnNew.setOnClickListener {
            if (!saveManager.hasSavedGame()) return@setOnClickListener
            val intent = Intent(this, Tela_de_Jogo::class.java).apply {
                putExtra(GameSaveManager.EXTRA_LOAD_SAVED_GAME, true)
            }
            startActivity(intent)
        }

        updateSavedGameButtonState()
    }

    override fun onResume() {
        super.onResume()
        updateSavedGameButtonState()
        SoundManager.onScreenResume(this)
    }

    override fun onPause() {
        super.onPause()
        SoundManager.onScreenPause()
    }

    private fun updateSavedGameButtonState() {
        val hasSave = saveManager.hasSavedGame()
        btnNew.isEnabled = hasSave
        btnNew.alpha = if (hasSave) 1.0f else 0.45f
    }
}