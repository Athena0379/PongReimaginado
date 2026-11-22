package com.tiwgo.pongreimaginado

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Tela_Final_com_Pontuacao : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_final_com_pontuacao)

        val btn_restart = findViewById<Button>(R.id.btn_restart)
        val btn_iniciar = findViewById<Button>(R.id.btn_iniciar)

        val pontuacao_calculada = intent.getIntExtra("EXTRA_SCORE", 0)
        val pontuacao: TextView = findViewById(R.id.Pontos)
        pontuacao.text = "$pontuacao_calculada"

        btn_restart.setOnClickListener {
            val intent = Intent(this, Tela_de_Jogo::class.java)
            startActivity(intent)
            finish()
        }
        btn_iniciar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        SoundManager.onScreenResume(this)
    }

    override fun onPause() {
        super.onPause()
        SoundManager.onScreenPause()
    }
}