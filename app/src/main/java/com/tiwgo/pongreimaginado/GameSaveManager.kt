package com.tiwgo.pongreimaginado

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedBallState(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val auraRadius: Float
)

data class SavedPowerUpState(
    val x: Float,
    val y: Float,
    val radius: Float,
    val auraRadius: Float,
    val isActive: Boolean
)

data class SavedGameState(
    val score: Int,
    val playerX: Float,
    val cpuX: Float,
    val balls: List<SavedBallState>,
    val powerUp: SavedPowerUpState?
)

class GameSaveManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "pong_eclipse_save_prefs"
        private const val KEY_HAS_SAVE = "has_saved_game"
        private const val KEY_SAVE_JSON = "saved_game_json"
        const val EXTRA_LOAD_SAVED_GAME = "EXTRA_LOAD_SAVED_GAME"
    }

    fun hasSavedGame(): Boolean {
        return prefs.getBoolean(KEY_HAS_SAVE, false) && prefs.getString(KEY_SAVE_JSON, null) != null
    }

    fun saveGame(state: SavedGameState) {
        val root = JSONObject().apply {
            put("score", state.score)
            put("playerX", state.playerX.toDouble())
            put("cpuX", state.cpuX.toDouble())

            val ballsArray = JSONArray()
            for (ball in state.balls) {
                ballsArray.put(
                    JSONObject().apply {
                        put("x", ball.x.toDouble())
                        put("y", ball.y.toDouble())
                        put("vx", ball.vx.toDouble())
                        put("vy", ball.vy.toDouble())
                        put("radius", ball.radius.toDouble())
                        put("auraRadius", ball.auraRadius.toDouble())
                    }
                )
            }
            put("balls", ballsArray)

            state.powerUp?.let { orb ->
                put(
                    "powerUp",
                    JSONObject().apply {
                        put("x", orb.x.toDouble())
                        put("y", orb.y.toDouble())
                        put("radius", orb.radius.toDouble())
                        put("auraRadius", orb.auraRadius.toDouble())
                        put("isActive", orb.isActive)
                    }
                )
            }
        }

        prefs.edit()
            .putString(KEY_SAVE_JSON, root.toString())
            .putBoolean(KEY_HAS_SAVE, true)
            .apply()
    }

    fun loadGame(): SavedGameState? {
        if (!hasSavedGame()) return null
        val rawJson = prefs.getString(KEY_SAVE_JSON, null) ?: return null

        return try {
            val root = JSONObject(rawJson)
            val score = root.optInt("score", 0)
            val playerX = root.optDouble("playerX", 0.0).toFloat()
            val cpuX = root.optDouble("cpuX", 0.0).toFloat()

            val ballsList = mutableListOf<SavedBallState>()
            val ballsArray = root.optJSONArray("balls") ?: JSONArray()
            for (i in 0 until ballsArray.length()) {
                val item = ballsArray.getJSONObject(i)
                ballsList.add(
                    SavedBallState(
                        x = item.getDouble("x").toFloat(),
                        y = item.getDouble("y").toFloat(),
                        vx = item.getDouble("vx").toFloat(),
                        vy = item.getDouble("vy").toFloat(),
                        radius = item.optDouble("radius", 26.0).toFloat(),
                        auraRadius = item.optDouble("auraRadius", 44.0).toFloat()
                    )
                )
            }

            val powerUpObj = root.optJSONObject("powerUp")
            val powerUp = powerUpObj?.let {
                SavedPowerUpState(
                    x = it.getDouble("x").toFloat(),
                    y = it.getDouble("y").toFloat(),
                    radius = it.optDouble("radius", 20.0).toFloat(),
                    auraRadius = it.optDouble("auraRadius", 34.0).toFloat(),
                    isActive = it.optBoolean("isActive", false)
                )
            }

            SavedGameState(
                score = score,
                playerX = playerX,
                cpuX = cpuX,
                balls = ballsList,
                powerUp = powerUp
            )
        } catch (_: Exception) {
            clearSave()
            null
        }
    }

    fun clearSave() {
        prefs.edit()
            .remove(KEY_SAVE_JSON)
            .putBoolean(KEY_HAS_SAVE, false)
            .apply()
    }
}