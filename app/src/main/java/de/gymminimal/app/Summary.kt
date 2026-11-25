package de.gymminimal.app

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

// ------------------------------------------------------------------ Neue Phase: Startgewichte

/** Start-Wiederholungen je ILB-Phasentyp (Bereiche 15–20, 8–12, 4–6). */
val PHASE_START_REPS = mapOf("Muskelausdauer" to 15, "Hypertrophie" to 10, "Muskelkraft" to 5)
val PHASE_REP_RANGE = mapOf("Muskelausdauer" to "15–20", "Hypertrophie" to "8–12", "Muskelkraft" to "4–6")

/** Eine Übung, die in die neue Phase übernommen wird, mit vorgeschlagenen Startwerten. */
data class CarryOver(
    val day: Int, val exercise: Exercise,
    val lastWeight: Double?, val lastReps: Int?, val bodyweight: Boolean,
    val weight: String, val reps: String
) {
    val oneRm: Double? get() = if (lastWeight != null && lastReps != null && lastWeight > 0) epley(lastWeight, lastReps) else null
}

/** Auf 2,5 kg abgerundet, unter 20 kg auf 1 kg. */
fun roundStartWeight(kg: Double): Double = if (kg < 20) floor(kg) else floor(kg / 2.5) * 2.5

private fun plain(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

/**
 * Vorschläge für alle Übungen der gewählten Tage. Gewicht = 1RM ÷ (1 + Wdh./30) aus der letzten Einheit.
 * Eigengewicht-Übungen behalten Belastung und Wiederholungen. Unbekannter Phasentyp: letzte Werte bleiben.
 */
fun carryOverSuggestions(data: AppData, phaseName: String, selected: Set<Int>): List<CarryOver> {
    val target = PHASE_START_REPS[phaseName]
    return data.plans.filter { it.day in selected }.sortedBy { it.day }.flatMap { plan ->
        plan.exercises.map { ex ->
            val identity = ex.exerciseKey.ifBlank { ex.id }
            val last = data.history.sortedBy { it.timestamp }.flatMap { it.entries }
                .lastOrNull { it.isPerformed && it.performedId == identity && it.status == "completed" }
            val ek = ex.isBodyweight
            val lastW = last?.weight?.takeIf { it > 0 }
            val lastR = last?.reps?.takeIf { it > 0 }
            when {
                ek -> CarryOver(plan.day, ex, lastW, lastR, true, ex.weight, (lastR ?: ex.reps.toIntOrNull() ?: 10).toString())
                lastW != null && lastR != null && target != null -> {
                    val w = roundStartWeight(epley(lastW, lastR) / (1 + target / 30.0)).coerceAtLeast(0.0)
                    CarryOver(plan.day, ex, lastW, lastR, false, plain(w), target.toString())
                }
                else -> CarryOver(plan.day, ex, lastW, lastR, false, lastW?.let(::plain) ?: ex.weight,
                    (lastR ?: ex.reps.toIntOrNull() ?: 10).toString())
            }
        }
    }
}

// ------------------------------------------------------------------ Notiz pro Übung

const val NOTE_MAX = 120

fun noteFor(data: AppData, name: String): String? = data.notes[exerciseNameKey(name)]?.takeIf { it.isNotBlank() }

fun setNote(data: AppData, name: String, note: String): AppData {
    val key = exerciseNameKey(name)
    if (key.isBlank()) return data
    val clean = note.replace(Regex("\\s+"), " ").trim().take(NOTE_MAX)
    return data.copy(notes = if (clean.isEmpty()) data.notes - key else data.notes + (key to clean))
}

// ------------------------------------------------------------------ Bilanz nach dem Training

data class ExerciseChange(val name: String, val now: String, val delta: String, val up: Boolean)
data class WorkoutSummary(
    val unit: Int?, val target: Int?, val phaseName: String,
    val durationMs: Long?, val durationDeltaMs: Long?,
    val volume: Double, val volumeDeltaPercent: Double?,
    val performed: Int, val total: Int,
    val best: BestLift?, val changes: List<ExerciseChange>
)

private fun num(v: Double) = String.format(Locale.GERMANY, if (v % 1.0 == 0.0) "%.0f" else "%.1f", v)

fun loggedValueText(e: LoggedExercise): String =
    if (e.bodyweight && e.extraKg == 0.0 && e.bandKg == 0.0) "EK × ${e.reps}" else "${num(e.weight)} kg × ${e.reps}"

fun workoutSummary(data: AppData, workout: Workout): WorkoutSummary {
    val earlier = data.history.filter { it.timestamp < workout.timestamp }.sortedBy { it.timestamp }
    val previousSameDay = earlier.lastOrNull { it.planDay == workout.planDay }
    val volume = workout.entries.sumOf { it.volume }
    val prevVolume = previousSameDay?.entries?.sumOf { it.volume }
    val lastByKey = mutableMapOf<String, LoggedExercise>()
    val bestByKey = mutableMapOf<String, Double>()
    earlier.forEach { w -> w.entries.filter { it.isPerformed }.forEach { e ->
        val k = exerciseNameKey(e.name)
        lastByKey[k] = e
        if (e.weight > 0 && e.reps > 0) bestByKey[k] = maxOf(bestByKey[k] ?: 0.0, epley(e.weight, e.reps))
    } }
    val performed = workout.entries.filter { it.isPerformed }
    val changes = performed.map { e ->
        val prev = lastByKey[exerciseNameKey(e.name)]
        val (delta, up) = when {
            prev == null -> "neu" to false
            !e.bodyweight && abs(e.weight - prev.weight) >= 0.05 ->
                ((if (e.weight > prev.weight) "+" else "−") + num(abs(e.weight - prev.weight)) + " kg") to (e.weight > prev.weight)
            e.reps != prev.reps -> ((if (e.reps > prev.reps) "+" else "−") + abs(e.reps - prev.reps) + " Wdh.") to (e.reps > prev.reps)
            e.bodyweight && abs(e.weight - prev.weight) >= 0.05 ->
                ((if (e.weight > prev.weight) "+" else "−") + num(abs(e.weight - prev.weight)) + " kg") to (e.weight > prev.weight)
            else -> "gleich" to false
        }
        ExerciseChange(e.name, loggedValueText(e), delta, up)
    }
    val best = performed.filter { it.weight > 0 && it.reps > 0 }.mapNotNull { e ->
        val before = bestByKey[exerciseNameKey(e.name)] ?: return@mapNotNull null
        val rm = epley(e.weight, e.reps)
        if (rm > before + 0.05) (rm / before) to BestLift(e.name, e.weight, e.reps, e.bodyweight, rm, workout.timestamp) else null
    }.maxByOrNull { it.first }?.second
    val phase = visiblePhases(data).firstOrNull { it.id == workout.phaseId }
    return WorkoutSummary(
        unit = if (phase != null && !phase.openEnded) data.history.count { it.phaseId == workout.phaseId } else null,
        target = phase?.target?.takeIf { it > 0 }, phaseName = phase?.name ?: "",
        durationMs = workout.durationMs,
        durationDeltaMs = workout.durationMs?.let { d -> previousSameDay?.durationMs?.let { d - it } },
        volume = volume,
        volumeDeltaPercent = prevVolume?.takeIf { it > 0 }?.let { (volume / it - 1) * 100 },
        performed = performed.size, total = workout.entries.size,
        best = best, changes = changes
    )
}

// ------------------------------------------------------------------ Verlauf einer Übung (Übungsinfo)

data class HistoryPoint(val timestamp: Long, val entry: LoggedExercise, val oneRm: Double?)
data class ExerciseHistory(val points: List<HistoryPoint>, val best: HistoryPoint?)

fun exerciseHistory(data: AppData, name: String): ExerciseHistory {
    val key = exerciseNameKey(name)
    val points = data.history.sortedBy { it.timestamp }.flatMap { w ->
        w.entries.filter { it.isPerformed && exerciseNameKey(it.name) == key }
            .map { HistoryPoint(w.timestamp, it, if (it.weight > 0 && it.reps > 0) epley(it.weight, it.reps) else null) }
    }
    return ExerciseHistory(points, points.filter { it.oneRm != null }.maxByOrNull { it.oneRm!! })
}
