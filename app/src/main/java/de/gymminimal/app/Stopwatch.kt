package de.gymminimal.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/** Pieptöne der Stoppuhr. 0 = aus. Doppelter Piep hat Vorrang, wenn beide zusammenfallen. */
data class StopwatchConfig(val singleEvery: Int = 1, val doubleEvery: Int = 5, val sound: Boolean = true)

const val STOPWATCH_SINGLE_MAX = 30
const val STOPWATCH_DOUBLE_MAX = 60

/** How many beeps at the start of [minute] (1, 2, …): 0, 1 or 2. */
fun stopwatchBeeps(minute: Int, config: StopwatchConfig): Int = when {
    !config.sound || minute <= 0 -> 0
    config.doubleEvery > 0 && minute % config.doubleEvery == 0 -> 2
    config.singleEvery > 0 && minute % config.singleEvery == 0 -> 1
    else -> 0
}

/** HH:MM:SS, hours keep counting past 99. */
fun formatStopwatch(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    return "%02d:%02d:%02d".format(total / 3600, (total / 60) % 60, total % 60)
}

fun stopwatchSummary(config: StopwatchConfig): String {
    if (!config.sound) return "Ton aus"
    val parts = buildList {
        if (config.singleEvery > 0) add("Piep alle ${config.singleEvery} min")
        if (config.doubleEvery > 0) add("doppelt alle ${config.doubleEvery} min")
    }
    return parts.joinToString(" · ").ifBlank { "Keine Pieptöne" }
}

/**
 * Stoppuhr-Zustand. Läuft auf der monotonen Uhr (elapsedRealtime), daher korrekt auch nach
 * Bildschirm aus/an. Nicht gespeichert: Ein Neustart der App beginnt bei 00:00:00.
 */
class StopwatchController(running: Boolean = false, base: Long = 0L, startedAt: Long = 0L) {
    var running by mutableStateOf(running); private set
    var base by mutableLongStateOf(base); private set
    var startedAt by mutableLongStateOf(startedAt); private set

    fun elapsed(now: Long = SystemClock.elapsedRealtime()): Long =
        base + if (running) (now - startedAt).coerceAtLeast(0L) else 0L

    fun start() { if (!running) { startedAt = SystemClock.elapsedRealtime(); running = true } }
    fun pause() { if (running) { base = elapsed(); running = false } }
    fun reset() { running = false; base = 0L; startedAt = 0L }

    companion object {
        val Saver = listSaver<StopwatchController, Long>(
            save = { listOf(if (it.running) 1L else 0L, it.base, it.startedAt) },
            restore = { StopwatchController(it[0] == 1L, it[1], it[2]) }
        )
    }
}

/** Settings and beep output. Beeps use the media volume and briefly lower other audio (music). */
class StopwatchBeeper(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("repavio_stopwatch_v1", Context.MODE_PRIVATE)
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var tone: ToneGenerator? = null
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()

    fun load(): StopwatchConfig = StopwatchConfig(
        singleEvery = prefs.getInt("single", 1).coerceIn(0, STOPWATCH_SINGLE_MAX),
        doubleEvery = prefs.getInt("double", 5).coerceIn(0, STOPWATCH_DOUBLE_MAX),
        sound = prefs.getBoolean("sound", true)
    )

    fun save(config: StopwatchConfig) {
        prefs.edit().putInt("single", config.singleEvery).putInt("double", config.doubleEvery)
            .putBoolean("sound", config.sound).apply()
    }

    fun beep(count: Int) {
        if (count <= 0) return
        try {
            audio.requestAudioFocus(focus)
            val generator = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 85).also { tone = it }
            generator.startTone(if (count >= 2) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, 300)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ audio.abandonAudioFocusRequest(focus) }, 700)
        } catch (_: Exception) {
            // Kein Ton ist besser als ein Absturz mitten im Training.
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        try { audio.abandonAudioFocusRequest(focus) } catch (_: Exception) { }
        tone?.release(); tone = null
    }
}
