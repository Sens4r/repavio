package de.gymminimal.app

import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------- Eigenkörpergewicht (EK)

/** Übungen, die ohne eigene Einstellung automatisch als Eigenkörpergewicht gelten. */
private val CALISTHENICS_NAMES = listOf(
    "Klimmzüge", "Klimmzüge im Untergriff", "Liegestütze", "Liegestütze erhöht", "Liegestütze eng", "Dips",
    "Australian Pull-ups", "Ring Rows", "Negative Klimmzüge", "Archer Pull-ups", "Typewriter Pull-ups",
    "Scapula Pull-ups", "Muscle-ups", "Dead Hang", "Front Lever", "Back Lever", "Skin the Cat",
    "Pike Push-ups", "Handstand Push-ups", "Handstand", "Archer Push-ups", "Diamond Push-ups",
    "Decline Push-ups", "Clap Push-ups", "Pseudo Planche Push-ups", "Planche Lean", "Tuck Planche",
    "Ring Dips", "Bank-Dips", "Straight Bar Dips", "Pistol Squats", "Shrimp Squats", "Sissy Squats",
    "Cossack Squats", "Jump Squats", "L-Sit", "Dragon Flag", "Toes to Bar", "Windshield Wipers", "Burpees",
    "Hängendes Beinheben", "Hängendes Knieheben", "Hollow Hold", "Plank", "Side Plank", "Mountain Climbers",
    "Nordic Hamstring Curls"
).map(::exerciseNameKey).toSet()

fun isCalisthenics(name: String): Boolean = exerciseNameKey(name) in CALISTHENICS_NAMES

/** null = automatisch nach Name. */
fun bodyweightFor(name: String, setting: Boolean?): Boolean = setting ?: isCalisthenics(name)
val Exercise.isBodyweight: Boolean get() = bodyweightFor(name, bodyweight)
val Alternative.isBodyweight: Boolean get() = bodyweightFor(name, bodyweight)

data class Band(val id: String = makeId(), val color: String, val kg: Double)

/** Bandfarben mit Anzeigefarbe (ARGB). */
val BAND_COLORS: List<Pair<String, Long>> = listOf(
    "Gelb" to 0xFFF2C94C, "Orange" to 0xFFF2994A, "Rot" to 0xFFE5484D, "Grün" to 0xFF46A758,
    "Blau" to 0xFF3E7BFA, "Lila" to 0xFF8E4EC6, "Schwarz" to 0xFF3A3A3F, "Grau" to 0xFF8B8D98
)
fun bandArgb(color: String): Long = BAND_COLORS.firstOrNull { it.first == color }?.second ?: 0xFF8B8D98
fun validBand(band: Band): Boolean = band.color in BAND_COLORS.map { it.first } && band.kg.isFinite() && band.kg in 0.5..200.0

/**
 * Belastung einer EK-Übung im Eingabefeld:
 * "0" = nur Eigengewicht, "12,5" = Zusatzgewicht, "band:<id>" = Unterstützung durch ein Band.
 */
data class BodyLoad(val extraKg: Double = 0.0, val bandId: String = "")

const val BAND_PREFIX = "band:"
fun parseBodyLoad(value: String): BodyLoad? =
    if (value.startsWith(BAND_PREFIX)) BodyLoad(bandId = value.removePrefix(BAND_PREFIX)).takeIf { it.bandId.isNotBlank() }
    else if (value.isBlank()) BodyLoad() else value.toWeightOrNull()?.let { BodyLoad(extraKg = it) }
fun bandLoad(id: String) = BAND_PREFIX + id

fun loadKg(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.GERMANY, "%.1f", value).removeSuffix(",0")

/** Kurzlabel für Kachel und Analyse, z. B. "EK", "EK +10 kg", "EK − Rot 15 kg". */
fun bodyLoadLabel(load: BodyLoad, bands: List<Band>): String {
    val band = bands.firstOrNull { it.id == load.bandId }
    return when {
        band != null -> "EK − ${band.color} ${loadKg(band.kg)} kg"
        load.bandId.isNotBlank() -> "EK (Band fehlt)"
        load.extraKg > 0 -> "EK +${loadKg(load.extraKg)} kg"
        else -> "EK"
    }
}

fun loggedLoadLabel(e: LoggedExercise): String = when {
    !e.bodyweight -> "${loadKg(e.weight)} kg"
    e.bandKg > 0 -> "EK − ${e.bandName} ${loadKg(e.bandKg)} kg"
    e.extraKg > 0 -> "EK +${loadKg(e.extraKg)} kg"
    else -> "EK"
}

/** Effektive Last in kg: Körpergewicht + Zusatz − Band, nie negativ. */
fun effectiveLoad(bodyKg: Double, extraKg: Double, bandKg: Double): Double =
    (bodyKg + extraKg - bandKg).coerceAtLeast(0.0)

// ---------------------------------------------------------------- Prognose

data class Prediction(val weight: Double, val reps: Int, val trend: Int)

private fun slope(values: List<Double>): Double {
    val n = values.size
    if (n < 2) return 0.0
    val mx = (n - 1) / 2.0
    val my = values.average()
    var num = 0.0; var den = 0.0
    values.forEachIndexed { i, v -> num += (i - mx) * (v - my); den += (i - mx) * (i - mx) }
    return if (den == 0.0) 0.0 else num / den
}

/**
 * Schätzung für die nächste Einheit aus den letzten (bis zu 6) Einheiten, älteste zuerst.
 * Mit Gewicht: geschätztes 1RM nach Epley (w × (1 + Wdh./30)), linearer Trend, je Einheit
 * auf ±5 % begrenzt. Daraus Wiederholungen beim letzten Gewicht; wären es 2 oder mehr als zuletzt,
 * wird das nächste Gewicht (increment) vorgeschlagen. Ohne Gewicht: Trend der Wiederholungen (±2).
 * Erst ab 2 Einheiten, sonst null.
 */
fun predictNext(sessions: List<Pair<Double, Int>>, increment: Double): Prediction? {
    val s = sessions.filter { it.second > 0 && it.first.isFinite() && it.first >= 0 }.takeLast(6)
    if (s.size < 2) return null
    val (w, r) = s.last()
    if (w <= 0.0) {
        val next = (r + slope(s.map { it.second.toDouble() }).coerceIn(-2.0, 2.0)).roundToInt().coerceIn(1, 100)
        return Prediction(0.0, next, (next - r).coerceIn(-1, 1))
    }
    val e1 = s.map { it.first * (1 + it.second / 30.0) }
    val last = e1.last()
    val predicted = last + slope(e1).coerceIn(-0.05 * last, 0.05 * last)
    val trend = when { predicted > last * 1.005 -> 1; predicted < last * 0.995 -> -1; else -> 0 }
    fun repsAt(weight: Double) = (30 * (predicted / weight - 1)).roundToInt()
    var reps = when (trend) { 1 -> maxOf(repsAt(w), r + 1); 0 -> r; else -> minOf(repsAt(w), r - 1) }.coerceIn(1, 30)
    var weight = w
    if (trend > 0 && increment > 0 && reps >= r + 2) {
        weight = w + increment
        reps = repsAt(weight).coerceIn(1, 30)
    }
    return Prediction(weight, reps, trend)
}

fun weightIncrement(weight: Double): Double = if (weight >= 20) 2.5 else 1.0

/** Prognose für eine Übungsidentität aus allen gespeicherten Trainings. */
fun predictionFor(data: AppData, performedId: String, bodyweight: Boolean): Prediction? {
    val sessions = data.history.sortedBy { it.timestamp }.mapNotNull { w ->
        w.entries.firstOrNull { it.performedId == performedId && it.isPerformed }
    }.filter { it.bodyweight == bodyweight }.map { it.weight to it.reps }
    val base = predictNext(sessions, if (bodyweight) 0.0 else weightIncrement(sessions.lastOrNull()?.first ?: 0.0))
        ?: return null
    // Bei EK zählt nur die Wiederholungszahl; die Last bleibt wie eingestellt.
    return if (bodyweight) base.copy(weight = 0.0) else base
}

// ---------------------------------------------------------------- Trainingsdauer

fun durationText(ms: Long): String {
    val minutes = (ms / 60_000L).toInt()
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

// ---------------------------------------------------------------- Tabs

const val TAB_TRAINING = 0
const val TAB_ANALYSE = 1
const val TAB_NUTRITION = 2
const val TAB_PROFILE = 3
val TAB_NAMES = listOf("Training", "Analyse", "Ernährung", "Profil")

/** Reihenfolge aller vier Tabs plus ausgeblendete. Profil ist nie ausblendbar (dort liegen die Einstellungen). */
data class TabConfig(val order: List<Int> = listOf(0, 1, 2, 3), val hidden: Set<Int> = emptySet()) {
    val visible: List<Int> get() = order.filter { it !in hidden }
}

fun sanitizeTabs(config: TabConfig): TabConfig {
    val order = (config.order.filter { it in 0..3 }.distinct() + (0..3)).distinct()
    return TabConfig(order, config.hidden.filter { it in 0..3 && it != TAB_PROFILE }.toSet())
}

fun moveTab(config: TabConfig, tab: Int, delta: Int): TabConfig {
    val order = config.order.toMutableList()
    val i = order.indexOf(tab)
    val j = i + delta
    if (i < 0 || j !in order.indices) return config
    order[i] = order[j]; order[j] = tab
    return config.copy(order = order)
}

fun toggleTab(config: TabConfig, tab: Int): TabConfig =
    if (tab == TAB_PROFILE) config
    else config.copy(hidden = if (tab in config.hidden) config.hidden - tab else config.hidden + tab)

fun encodeTabs(config: TabConfig): String = config.order.joinToString(",") + ";" + config.hidden.sorted().joinToString(",")
fun decodeTabs(text: String?): TabConfig {
    if (text.isNullOrBlank()) return TabConfig()
    val parts = text.split(";")
    fun ints(s: String?) = s.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }
    return sanitizeTabs(TabConfig(ints(parts.getOrNull(0)), ints(parts.getOrNull(1)).toSet()))
}
