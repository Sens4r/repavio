package de.gymminimal.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

/** Rückmeldung beim Wischen über Gewicht oder Wiederholungen: Ton, Vibration, beides oder aus. */
val SWIPE_FEEDBACK = listOf("sound" to "Ton", "vib" to "Vibration", "both" to "Beides", "off" to "Aus")
val VIBRATION_STRENGTHS = listOf("leicht", "mittel", "kräftig")

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

    /** Bis 1.6.0 gab es nur den Schalter „Wisch-Sound“ (sounds_enabled); er wird übernommen. */
    fun mode(): String = prefs.getString("swipe_feedback", null)?.takeIf { k -> SWIPE_FEEDBACK.any { it.first == k } }
        ?: if (prefs.getBoolean("sounds_enabled", true)) "sound" else "off"
    fun setMode(value: String) { prefs.edit().putString("swipe_feedback", value).apply() }
    fun strength(): String = prefs.getString("vibration_strength", null)?.takeIf { it in VIBRATION_STRENGTHS } ?: "mittel"
    fun setStrength(value: String) { prefs.edit().putString("vibration_strength", value).apply() }

    fun enabled(): Boolean = mode() != "off"
    fun setEnabled(value: Boolean) { setMode(if (value) "sound" else "off") }

    /** Eine Stufe beim Wischen. strong = runder Wert (volle 10 kg), etwas kräftiger. */
    fun tick(view: View? = null, strong: Boolean = false) {
        val mode = mode()
        if ((mode == "sound" || mode == "both") && synchronized(ready) { wheel in ready })
            pool.play(wheel, .19f, .19f, 1, 0, 1f)
        if ((mode == "vib" || mode == "both") && view != null) {
            val levels = listOf(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.LONG_PRESS)
            val base = VIBRATION_STRENGTHS.indexOf(strength()).coerceAtLeast(0)
            // Folgt der Systemeinstellung „Vibration bei Berührung“.
            view.performHapticFeedback(levels[(base + if (strong) 1 else 0).coerceAtMost(levels.lastIndex)])
        }
    }
    fun release() { pool.release() }
}

val LocalRepavioSounds = staticCompositionLocalOf<RepavioSounds?> { null }
