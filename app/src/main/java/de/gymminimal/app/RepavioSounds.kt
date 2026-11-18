package de.gymminimal.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf

/** Nur der dezente Sound beim Wischen ueber Gewicht oder Wiederholungen. */
class RepavioSounds(context: Context) {
    // Der bisherige Schluessel bleibt erhalten, damit die Einstellung bei Updates bestehen bleibt.
    private val prefs = context.applicationContext.getSharedPreferences(
        "gymminimal_local_v1", Context.MODE_PRIVATE)
    private val pool = SoundPool.Builder().setMaxStreams(1)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val wheel: Int
    private val ready = mutableSetOf<Int>()

    init {
        pool.setOnLoadCompleteListener { _, soundId, status ->
            if (status == 0) synchronized(ready) { ready.add(soundId) }
        }
        wheel = pool.load(context, R.raw.ui_wheel, 1)
    }

    fun enabled(): Boolean = prefs.getBoolean("sounds_enabled", true)
    fun setEnabled(value: Boolean) { prefs.edit().putBoolean("sounds_enabled", value).apply() }
    fun tick() {
        if (enabled() && synchronized(ready) { wheel in ready })
            pool.play(wheel, .19f, .19f, 1, 0, 1f)
    }
    fun release() { pool.release() }
}

val LocalRepavioSounds = staticCompositionLocalOf<RepavioSounds?> { null }
