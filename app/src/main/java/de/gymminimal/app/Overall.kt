package de.gymminimal.app

/**
 * Analyse über alle Phasen. Phasen mit unterschiedlichen Wiederholungsbereichen werden über das
 * geschätzte 1RM (Epley) vergleichbar. Nur gespeicherte Trainings zählen, Pausen erzeugen keine Werte.
 * Übungen werden über den Namen zusammengeführt (gleiche Identität wie `exerciseKey`).
 */
fun epley(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1 + reps / 30.0)

data class StrengthPoint(val timestamp: Long, val phaseId: String, val index: Double)
data class PhaseTypeStats(val name: String, val phases: Int, val workouts: Int, val avgGain: Double?)
data class BestLift(val name: String, val weight: Double, val reps: Int, val bodyweight: Boolean,
                    val oneRm: Double, val timestamp: Long)
data class MuscleGain(val group: String, val exercises: Int, val gain: Double?)
data class OverallStats(
    val workouts: Int, val phases: Int, val volume: Double, val avgDurationMs: Long?, val since: Long?,
    val strength: List<StrengthPoint>, val gain: Double?,
    val types: List<PhaseTypeStats>, val best: List<BestLift>, val muscles: List<MuscleGain>
)

private val LoggedExercise.rated: Boolean get() = isPerformed && weight > 0.0 && weight.isFinite() && reps > 0

/** Kraftindex: jede Übung beginnt bei 100 %, Wert = Ø (aktuelles 1RM / erstes 1RM) nach jedem Training. */
fun strengthIndex(workouts: List<Workout>): List<StrengthPoint> {
    val baseline = linkedMapOf<String, Double>()
    val latest = linkedMapOf<String, Double>()
    val out = mutableListOf<StrengthPoint>()
    for (w in workouts.sortedBy { it.timestamp }) {
        for (e in w.entries) {
            if (!e.rated) continue
            val key = exerciseNameKey(e.name)
            val rm = epley(e.weight, e.reps)
            if (key !in baseline) baseline[key] = rm
            latest[key] = rm
        }
        if (baseline.isNotEmpty()) out += StrengthPoint(w.timestamp, w.phaseId,
            baseline.keys.map { latest.getValue(it) / baseline.getValue(it) * 100.0 }.average())
    }
    return out
}

fun overallStats(data: AppData): OverallStats {
    val workouts = data.history.sortedBy { it.timestamp }
    val performed = workouts.flatMap { w -> w.entries.filter { it.isPerformed }.map { w to it } }
    val strength = strengthIndex(workouts)
    val durations = workouts.mapNotNull { it.durationMs }

    val phasesById = visiblePhases(data).associateBy { it.id }
    val byPhase = workouts.groupBy { it.phaseId }
    val types = byPhase.entries.groupBy { phasesById[it.key]?.name ?: "Frühere Phase" }.map { (name, list) ->
        val gains = list.mapNotNull { (_, ws) -> strengthIndex(ws).takeIf { it.size >= 2 }?.last()?.index?.minus(100.0) }
        PhaseTypeStats(name, list.size, list.sumOf { it.value.size }, gains.takeIf { it.isNotEmpty() }?.average())
    }.sortedWith(compareByDescending<PhaseTypeStats> { it.phases }.thenBy { it.name })

    val rated = performed.filter { it.second.rated }
    val best = rated.groupBy { exerciseNameKey(it.second.name) }.values.map { list ->
        val (w, e) = list.maxWith(compareBy<Pair<Workout, LoggedExercise>> { epley(it.second.weight, it.second.reps) }
            .thenBy { it.first.timestamp })
        BestLift(e.name, e.weight, e.reps, e.bodyweight, epley(e.weight, e.reps), w.timestamp)
    }.sortedByDescending { it.oneRm }.take(5)

    val byName = performed.groupBy { exerciseNameKey(it.second.name) }
    val muscles = byName.values.groupBy { it.last().second.muscleGroup }.map { (group, exercises) ->
        val gains = exercises.mapNotNull { list ->
            val r = list.filter { it.second.rated }
            if (r.size < 2) null else {
                val first = epley(r.first().second.weight, r.first().second.reps)
                val last = epley(r.last().second.weight, r.last().second.reps)
                (last / first - 1.0) * 100.0
            }
        }
        MuscleGain(group, exercises.size, gains.takeIf { it.isNotEmpty() }?.average())
    }.sortedBy { it.group.lowercase() }

    return OverallStats(
        workouts = workouts.size, phases = byPhase.size,
        volume = performed.sumOf { it.second.volume },
        avgDurationMs = durations.takeIf { it.isNotEmpty() }?.average()?.toLong(),
        since = workouts.firstOrNull()?.timestamp,
        strength = strength, gain = strength.takeIf { it.size >= 2 }?.last()?.index?.minus(100.0),
        types = types, best = best, muscles = muscles
    )
}

/** Für die Grafik: höchstens [max] Punkte, gleichmäßig ausgedünnt, letzter Punkt bleibt erhalten. */
fun <T> thinOut(points: List<T>, max: Int): List<T> {
    if (points.size <= max || max < 2) return points
    val step = (points.size - 1).toDouble() / (max - 1)
    return (0 until max).map { points[Math.round(it * step).toInt()] }
}
