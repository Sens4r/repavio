package de.gymminimal.app

import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.text.Normalizer
import java.text.Collator
import java.util.Locale

fun makeId(): String = UUID.randomUUID().toString()

val DAY_NAMES = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
val DAY_SHORT = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
val MUSCLE_GROUPS = listOf("Bauch", "Beine", "Bizeps", "Brust", "Rücken", "Schulter")
val DAY_MUSCLE_OPTIONS = MUSCLE_GROUPS

fun chosenMuscles(text: String): Set<String> = text.split("·", ",").map { it.trim() }
    .map { if (it == "Schultern") "Schulter" else it }.filter { it.isNotBlank() }.toSet()
fun alphabeticTags(tags: Collection<String>): List<String> {
    val collator = Collator.getInstance(Locale.GERMAN)
    collator.strength = Collator.PRIMARY
    return tags.filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.GERMAN) }
        .sortedWith(Comparator { a, b -> collator.compare(a, b) })
}
fun formattedMuscles(selection: Set<String>): String = alphabeticTags(selection).joinToString(" · ")
/** Fixed body parts first, then user-defined tags in a separate section. */
fun fixedMuscleGroups(): List<String> = alphabeticTags(MUSCLE_GROUPS)
fun customMuscleGroups(data: AppData): List<String> = alphabeticTags(data.customTags.filterNot { tag -> MUSCLE_GROUPS.any { it.equals(tag, true) } })
fun allMuscleGroups(data: AppData): List<String> = fixedMuscleGroups() + customMuscleGroups(data)
/** Remove one custom tag globally in the active plans without altering historical logs. */
fun removeCustomTags(data: AppData, tags: Set<String>): AppData {
    if (tags.isEmpty()) return data
    val removable = data.customTags.filter { tag -> tags.any { it.equals(tag, ignoreCase = true) } }.toSet()
    if (removable.isEmpty()) return data
    return data.copy(
        customTags = data.customTags.filterNot { it in removable },
        plans = data.plans.map { plan -> plan.copy(
            muscles = formattedMuscles(chosenMuscles(plan.muscles) - removable),
            exercises = plan.exercises.map { exercise ->
                if (exercise.muscleGroup in removable) exercise.copy(muscleGroup = "Sonstige") else exercise
            }
        ) }
    )
}
fun normalizeMuscleGroup(value: String): String = if (value == "Schultern") "Schulter" else value

// Local suggestion catalogue. User-created exercises are added automatically at runtime.
// These are labels only, not a remote database, and they never require a connection.
val EXERCISE_SUGGESTIONS = listOf(
    // Brust
    "Bankdrücken", "Bankdrücken mit Kurzhanteln", "Schrägbankdrücken", "Schrägbankdrücken mit Kurzhanteln",
    "Negativbankdrücken", "Brustpresse", "Brustpresse einarmig", "Butterfly", "Butterfly Reverse",
    "Kabel-Flys", "Kabel-Flys von oben", "Kabel-Flys von unten", "Kurzhantel-Flys",
    "Liegestütze", "Liegestütze erhöht", "Liegestütze eng", "Dips", "Überzüge mit Kurzhantel",
    // Rücken
    "Latziehen", "Latziehen eng", "Latziehen breit", "Latziehen im Untergriff", "Klimmzüge",
    "Klimmzüge im Untergriff", "Klimmzüge assistiert", "Rudern am Kabel", "Rudern am Kabel eng",
    "Langhantelrudern", "Kurzhantelrudern", "T-Bar-Rudern", "Maschinenrudern", "Rudern aufrecht",
    "Kreuzheben", "Rumänisches Kreuzheben", "Hyperextensions", "Face Pulls", "Gerader Armzug",
    "Latzug einarmig", "Shrugs", "Kettlebell-Rudern",
    // Beine
    "Kniebeugen", "Frontkniebeugen", "Goblet Squats", "Hackenschmidt-Kniebeugen",
    "Bulgarian Split Squats", "Ausfallschritte", "Gehende Ausfallschritte", "Step-ups",
    "Beinpresse", "Beinpresse einbeinig", "Beinstrecker", "Beinbeuger", "Beinbeuger liegend",
    "Beinbeuger sitzend", "Hip Thrusts", "Glute Bridge", "Abduktorenmaschine",
    "Adduktorenmaschine", "Wadenheben", "Wadenheben sitzend", "Wadenheben stehend",
    "Sumo-Kniebeugen", "Nordic Hamstring Curls", "Good Mornings", "Kickbacks am Kabel",
    // Schulter
    "Schulterdrücken", "Kurzhantel-Schulterdrücken", "Arnold Press", "Military Press",
    "Seitheben", "Seitheben am Kabel", "Seitheben an der Maschine", "Frontheben",
    "Reverse Flys", "Reverse Flys am Kabel", "Vorgebeugtes Seitheben", "Schulterdrücken an der Maschine",
    // Bizeps / Trizeps / Arme
    "Bizepscurls", "Langhantelcurls", "Kurzhantelcurls", "Hammer Curls", "Scottcurls",
    "Konzentrationscurls", "Kabelcurls", "Curls am Kabel", "Incline Curls", "Preacher Curls",
    "Reverse Curls", "Zottman Curls", "Trizepsdrücken", "Trizepsdrücken am Seil",
    "Trizepsdrücken einarmig", "French Press", "Skull Crushers", "Overhead-Trizepsstrecken",
    "Trizeps-Kickbacks", "Enges Bankdrücken", "Unterarmcurls",
    // Bauch / Rumpf
    "Crunches", "Crunches am Kabel", "Reverse Crunches", "Plank", "Side Plank", "Sit-ups",
    "Beinheben", "Hängendes Beinheben", "Hängendes Knieheben", "Russian Twists", "Dead Bug",
    "Bicycle Crunches", "Ab Wheel Rollout", "Bauchpresse", "Hollow Hold", "Mountain Climbers",
    "Pallof Press", "Farmer's Walk",
    // Calisthenics / Körpergewicht
    "Australian Pull-ups", "Ring Rows", "Negative Klimmzüge", "Archer Pull-ups", "Typewriter Pull-ups",
    "Scapula Pull-ups", "Muscle-ups", "Dead Hang", "Front Lever", "Back Lever", "Skin the Cat",
    "Pike Push-ups", "Handstand Push-ups", "Handstand", "Archer Push-ups", "Diamond Push-ups",
    "Decline Push-ups", "Clap Push-ups", "Pseudo Planche Push-ups", "Planche Lean", "Tuck Planche",
    "Ring Dips", "Bank-Dips", "Straight Bar Dips", "Pistol Squats", "Shrimp Squats", "Sissy Squats",
    "Cossack Squats", "Jump Squats", "L-Sit", "Dragon Flag", "Toes to Bar", "Windshield Wipers", "Burpees"
).distinct()

/** A stable exercise identity distinct from the ID of a tile in a weekday plan. */
data class ExerciseDefinition(val id: String = makeId(), val name: String)

/** Identically named exercises share one identity, regardless of weekday or whitespace/case. */
fun exerciseNameKey(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFKC)
    .trim().replace(Regex("\\s+"), " ").lowercase(Locale.GERMAN)

// Existing data from v0.1 has no explicit muscle group. Best-effort fallback, only when needed.
fun inferredMuscleGroup(name: String, fallback: String = "Sonstige"): String {
    val n = name.lowercase()
    return when {
        listOf("bizeps", "curl", "hammer").any { it in n } -> "Bizeps"
        listOf("trizeps", "dips", "pushdown").any { it in n } -> "Trizeps"
        listOf("bankdrück", "butterfly", "brust", "fly", "liegestütz").any { it in n } -> "Brust"
        listOf("latzieh", "klimm", "ruder", "kreuzheb").any { it in n } -> "Rücken"
        listOf("kniebeug", "bein", "waden", "squat").any { it in n } -> "Beine"
        listOf("schulter", "seitheb", "frontheb").any { it in n } -> "Schulter"
        listOf("bauch", "crunch", "plank").any { it in n } -> "Bauch"
        // Calisthenics keywords. Checked after the original rules so older inferences never change.
        listOf("pull-up", "chin-up", "muscle-up", "row", "lever", "dead hang", "skin the cat").any { it in n } -> "Rücken"
        listOf("pike", "handstand").any { it in n } -> "Schulter"
        listOf("push-up", "planche").any { it in n } -> "Brust"
        listOf("l-sit", "dragon flag", "toes to bar", "windshield").any { it in n } -> "Bauch"
        else -> fallback.ifBlank { "Sonstige" }
    }
}

// A plan is a reusable template. It does not create scheduled workout records.
data class Alternative(
    val id: String = makeId(),
    val name: String = "",
    val weight: String = "0",
    val reps: String = "10",
    val sets: Int = 3,
    val rest: Int = 90,
    val exerciseKey: String = "",
    /** Eigenkörpergewicht: null = automatisch nach Name (Calisthenics). */
    val bodyweight: Boolean? = null
)

data class Exercise(
    val id: String = makeId(),
    val name: String,
    val weight: String = "0",
    val reps: String = "10",
    val sets: Int = 3,
    val rest: Int = 90,
    val alternatives: List<Alternative> = emptyList(),
    val muscleGroup: String = "Sonstige",
    val exerciseKey: String = "",
    val bodyweight: Boolean? = null
)

data class DayPlan(
    val day: Int,
    val muscles: String = "",
    val exercises: List<Exercise> = emptyList(),
    val enabled: Boolean = false
)

data class Phase(
    val id: String = makeId(),
    val name: String,
    val weeks: Int,
    val unitsPerWeek: Int,
    val finishedAt: Long? = null,
    val archivedAt: Long? = null,
    /** "Keine Phase": training without ILB target, week counter or completion. */
    val openEnded: Boolean = false
) {
    val target: Int get() = if (openEnded) 0 else weeks * unitsPerWeek
    val isConfigured: Boolean get() = name.isNotBlank() && unitsPerWeek > 0 && (openEnded || weeks > 0)
}

const val NO_PHASE_NAME = "Keine Phase"

// Only completed workouts are data. No zero-valued records for missed days.
data class LoggedExercise(
    val originalId: String,
    val performedId: String,
    val name: String,
    val status: String, // completed | replaced | unavailable (legacy skipped is readable)
    val weight: Double = 0.0,
    val reps: Int = 0,
    val sets: Int = 0,
    val muscleGroup: String = "Sonstige",
    /** EK-Übung: weight ist die effektive Last (Körpergewicht + Zusatz − Band). */
    val bodyweight: Boolean = false,
    val bodyKg: Double = 0.0,
    val extraKg: Double = 0.0,
    val bandKg: Double = 0.0,
    val bandName: String = "",
    val bandId: String = ""
) {
    val isPerformed: Boolean get() = status == "completed" || status == "replaced"
    val volume: Double get() = if (isPerformed) weight * reps * sets else 0.0
}

data class Workout(
    val id: String = makeId(),
    val phaseId: String,
    val planDay: Int,
    val timestamp: Long,
    val entries: List<LoggedExercise>,
    /** Gestoppte Trainingsdauer, falls die Stoppuhr lief. */
    val durationMs: Long? = null
)

data class ExerciseChoice(val status: String = "completed", val alternativeId: String = "")
data class Values(val weight: String, val reps: String)

data class AppData(
    val phase: Phase,
    val plans: List<DayPlan>,
    val history: List<Workout> = emptyList(),
    val choices: Map<String, ExerciseChoice> = emptyMap(),
    val drafts: Map<String, Values> = emptyMap(),
    val pastPhases: List<Phase> = emptyList(),
    val customTags: List<String> = emptyList(),
    val exerciseCatalog: List<ExerciseDefinition> = emptyList(),
    val bands: List<Band> = emptyList(),
    /** Notiz je Übung (Schlüssel: normalisierter Name), z. B. Geräteeinstellung. */
    val notes: Map<String, String> = emptyMap()
) {
    val completedInPhase: Int get() = history.count { it.phaseId == phase.id }
    val remainingInPhase: Int get() = if (phase.openEnded) Int.MAX_VALUE
        else (phase.target - completedInPhase).coerceAtLeast(0)
    fun plan(day: Int): DayPlan = plans.first { it.day == day }
    fun choice(exerciseId: String): ExerciseChoice = choices[exerciseId] ?: ExerciseChoice()
    // Drafts belong to the tile. Completed values belong to the canonical exercise ID.
    // This keeps Monday/Thursday drafts independent but syncs both after a completed workout.
    fun valueFor(id: String, base: Values): Values {
        var bodyweight = false
        val canonicalId = plans.asSequence().flatMap { it.exercises.asSequence() }.firstNotNullOfOrNull { exercise ->
            when {
                exercise.id == id -> { bodyweight = exercise.isBodyweight; exercise.exerciseKey.ifBlank { exercise.id } }
                else -> exercise.alternatives.firstOrNull { it.id == id }?.let { alt ->
                    bodyweight = alt.isBodyweight
                    alt.exerciseKey.ifBlank { alt.id }
                }
            }
        } ?: id
        val newest = history.asReversed().asSequence()
            .filter { it.phaseId == phase.id }
            .flatMap { it.entries.asSequence() }
            .firstOrNull { it.performedId == canonicalId && it.isPerformed }
        drafts[id]?.let { return it }
        if (newest == null) return base
        val reps = newest.reps.toString()
        return when {
            // EK an- oder ausgeschaltet: alte Last passt nicht, nur die Wiederholungen übernehmen.
            newest.bodyweight != bodyweight -> Values(base.weight, reps)
            bodyweight -> Values(if (newest.bandId.isNotBlank() && bands.any { it.id == newest.bandId })
                bandLoad(newest.bandId) else plainNumber(newest.extraKg), reps)
            else -> Values(plainNumber(newest.weight), reps)
        }
    }
}

private fun plainNumber(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

// Explicitly selected weekdays are the only source for sessions/week.
fun selectedDays(plans: List<DayPlan>): Set<Int> = plans.filter { it.enabled }.map { it.day }.toSet()
fun withSelectedDays(plans: List<DayPlan>, selected: Set<Int>): List<DayPlan> =
    plans.map { it.copy(enabled = it.day in selected) }

// Keep the weekday split (days / muscle groups), start the new phase with empty exercises.
// Historical workouts are untouched; old phase sessions remain associated with their phase ID.
// Optional werden ausgewählte Übungen mit neuen Startwerten übernommen (CarryOver).
fun beginNewPhase(data: AppData, phase: Phase, selected: Set<Int>, timestamp: Long = System.currentTimeMillis(),
                  carry: List<CarryOver> = emptyList()): AppData = data.copy(
    phase = phase.copy(unitsPerWeek = selected.size, finishedAt = null),
    pastPhases = if (data.phase.isConfigured) data.pastPhases + data.phase.copy(archivedAt = timestamp)
        else data.pastPhases,
    plans = withSelectedDays(data.plans, selected).map { plan ->
        plan.copy(exercises = carry.filter { it.day == plan.day && plan.day in selected }
            .map { it.exercise.copy(weight = it.weight, reps = it.reps) })
    },
    choices = emptyMap(),
    drafts = emptyMap()
)

fun initialData(): AppData = AppData(
    phase = Phase(name = "", weeks = 0, unitsPerWeek = 0),
    plans = (0..6).map { DayPlan(day = it) }
)

fun sampleData(): AppData {
    val plans = (0..6).map { DayPlan(it) }.toMutableList()
    plans[0] = DayPlan(0, "Brust · Trizeps", listOf(
        Exercise(name = "Bankdrücken", weight = "70", reps = "10", sets = 3, rest = 120,
            alternatives = listOf(Alternative(name = "Brustpresse", weight = "55", reps = "12")), muscleGroup = "Brust"),
        Exercise(name = "Schrägbankdrücken", weight = "24", reps = "12", muscleGroup = "Brust"),
        Exercise(name = "Butterfly", weight = "45", reps = "12", sets = 2, rest = 60, muscleGroup = "Brust")
    ), enabled = true)
    plans[2] = DayPlan(2, "Rücken · Bizeps", listOf(
        Exercise(name = "Latziehen", weight = "65", reps = "10", muscleGroup = "Rücken"),
        Exercise(name = "Bizepscurls", weight = "14", reps = "12", rest = 60, muscleGroup = "Bizeps")
    ), enabled = true)
    plans[4] = DayPlan(4, "Beine · Schulter", listOf(
        Exercise(name = "Kniebeugen", weight = "90", reps = "8", rest = 150, muscleGroup = "Beine"),
        Exercise(name = "Seitheben", weight = "10", reps = "12", rest = 60, muscleGroup = "Schulter")
    ), enabled = true)
    // Four active days by default. A day with no exercise stays grey until configured.
    plans[5] = DayPlan(5, "", emptyList(), enabled = true)
    return AppData(phase = Phase(name = "Hypertrophie", weeks = 8, unitsPerWeek = 4), plans = plans)
}

fun String.toWeightOrNull(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
fun String.toRepsOrNull(): Int? = toIntOrNull()?.takeIf { it > 0 }

// Counts a workout once regardless of how many exercise tiles it contains.
// A plan weekday can only be logged once per real ISO calendar week. Illness does not
// create missing workouts, nor does it advance the phase counter.
fun sameCalendarWeek(a: Long, b: Long): Boolean {
    val fields = WeekFields.ISO
    fun key(ms: Long): Pair<Int, Int> {
        val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.get(fields.weekBasedYear()) to date.get(fields.weekOfWeekBasedYear())
    }
    return key(a) == key(b)
}
fun hasWorkoutThisWeek(data: AppData, day: Int, timestamp: Long): Boolean {
    val exerciseIds = data.plan(day).exercises.map { it.id }.toSet()
    return data.history.any { workout ->
        workout.phaseId == data.phase.id && sameCalendarWeek(workout.timestamp, timestamp) &&
            (workout.planDay == day || (exerciseIds.isNotEmpty() &&
                workout.entries.any { it.originalId in exerciseIds }))
    }
}

/** Ein Eintrag aus den Eingabewerten. EK: Last = Körpergewicht (Profil) + Zusatz − Band. */
private fun loggedFrom(data: AppData, originalId: String, performedId: String, name: String, status: String,
                       values: Values, sets: Int, muscle: String, bodyweight: Boolean, bodyKg: Double?): LoggedExercise? {
    val reps = values.reps.toRepsOrNull() ?: return null
    if (!bodyweight) return LoggedExercise(originalId, performedId, name, status,
        values.weight.toWeightOrNull() ?: return null, reps, sets, muscle)
    val load = parseBodyLoad(values.weight) ?: return null
    val band = data.bands.firstOrNull { it.id == load.bandId }
    val body = bodyKg?.takeIf { it.isFinite() && it > 0 } ?: 0.0
    val extra = if (band == null) load.extraKg else 0.0
    return LoggedExercise(originalId, performedId, name, status, effectiveLoad(body, extra, band?.kg ?: 0.0), reps, sets,
        muscle, bodyweight = true, bodyKg = body, extraKg = extra, bandKg = band?.kg ?: 0.0,
        bandName = band?.color.orEmpty(), bandId = band?.id.orEmpty())
}

fun finishWorkout(data: AppData, day: Int, timestamp: Long, bodyKg: Double? = null, durationMs: Long? = null): AppData? {
    if (!data.phase.isConfigured || data.remainingInPhase == 0 || data.phase.finishedAt != null || hasWorkoutThisWeek(data, day, timestamp)) return null
    val plan = data.plan(day)
    if (!plan.enabled || plan.exercises.isEmpty()) return null
    val entries = plan.exercises.map { exercise ->
        val choice = data.choice(exercise.id)
        when (choice.status) {
            "skipped", "unavailable" -> LoggedExercise(
                originalId = exercise.id, performedId = exercise.id,
                name = exercise.name, status = "unavailable", muscleGroup = exercise.muscleGroup
            )
            "replaced" -> {
                val alt = exercise.alternatives.firstOrNull { it.id == choice.alternativeId } ?: return null
                val values = data.valueFor(alt.id, Values(alt.weight, alt.reps))
                loggedFrom(data, exercise.id, alt.exerciseKey.ifBlank { alt.id }, alt.name, "replaced",
                    values, alt.sets, exercise.muscleGroup, alt.isBodyweight, bodyKg) ?: return null
            }
            else -> {
                val values = data.valueFor(exercise.id, Values(exercise.weight, exercise.reps))
                loggedFrom(data, exercise.id, exercise.exerciseKey.ifBlank { exercise.id }, exercise.name, "completed",
                    values, exercise.sets, exercise.muscleGroup, exercise.isBodyweight, bodyKg) ?: return null
            }
        }
    }
    if (entries.none { it.isPerformed }) return null
    val performedIds = entries.filter { it.isPerformed }.map { it.performedId }.toSet()
    val syncedTileIds = data.plans.flatMap { other -> other.exercises.flatMap { ex ->
        buildList {
            if (ex.exerciseKey.ifBlank { ex.id } in performedIds) add(ex.id)
            ex.alternatives.forEach { alt ->
                if (alt.exerciseKey.ifBlank { alt.id } in performedIds) add(alt.id)
            }
        }
    } }.toSet()
    return data.copy(
        history = data.history + Workout(phaseId = data.phase.id, planDay = day,
            timestamp = timestamp, entries = entries,
            durationMs = durationMs?.takeIf { it in 60_000L..(12 * 3_600_000L) }),
        choices = data.choices - plan.exercises.map { it.id }.toSet(),
        drafts = data.drafts - (syncedTileIds + plan.exercises.flatMap {
            listOf(it.id) + it.alternatives.map { a -> a.id }
        })
    )
}

// A phase completes only when the user explicitly confirms it after all units are logged.
fun completePhase(data: AppData, timestamp: Long): AppData =
    if (data.phase.isConfigured && !data.phase.openEnded && data.remainingInPhase == 0 && data.phase.finishedAt == null)
        data.copy(phase = data.phase.copy(finishedAt = timestamp)) else data

// Old v0.1/v0.2 logs may reference a former phase whose descriptive settings were
// not persisted. Keep that history accessible without inventing a phase name/duration.
fun visiblePhases(data: AppData): List<Phase> {
    val known = (listOf(data.phase) + data.pastPhases).map { it.id }.toSet()
    val legacy = data.history.map { it.phaseId }.distinct().filterNot { it in known }
        .map { Phase(id = it, name = "Frühere Phase (ältere Daten)", weeks = 0, unitsPerWeek = 0) }
    return listOfNotNull(data.phase.takeIf { it.isConfigured }) + data.pastPhases.asReversed() + legacy
}

fun deletePreviousPhase(data: AppData, phaseId: String): AppData {
    val archived = visiblePhases(data).drop(1).any { it.id == phaseId }
    return if (!archived || phaseId == data.phase.id) data else data.copy(
        pastPhases = data.pastPhases.filterNot { it.id == phaseId },
        history = data.history.filterNot { it.phaseId == phaseId }
    )
}

data class PhaseProgress(val indices: List<Double>, val exerciseCount: Int, val gainPercent: Double)

/** One index across all weight-bearing exercises: each exercise starts at 100%.
 * We never combine unrelated raw kilogram totals, nor insert zero values for missed days.
 * Only training logs belonging to the selected phase are inspected. */
fun phaseProgress(data: AppData, phaseId: String): PhaseProgress {
    val workouts = data.history.filter { it.phaseId == phaseId }.sortedBy { it.timestamp }
    val baseline = linkedMapOf<String, Double>()
    val latest = linkedMapOf<String, Double>()
    val points = mutableListOf<Double>()
    for (workout in workouts) {
        for (entry in workout.entries) {
            if (!entry.isPerformed || entry.weight <= 0.0 || !entry.weight.isFinite()) continue
            if (entry.performedId !in baseline) baseline[entry.performedId] = entry.weight
            latest[entry.performedId] = entry.weight
        }
        if (baseline.isNotEmpty()) {
            points += baseline.keys.map { id -> latest.getValue(id) / baseline.getValue(id) * 100.0 }.average()
        }
    }
    return PhaseProgress(points, baseline.size, (points.lastOrNull() ?: 100.0) - 100.0)
}

data class PhaseSummary(val workouts: Int, val totalVolume: Double, val exercises: Int,
                        val maxWeight: Double, val firstWorkout: Long?, val lastWorkout: Long?)

fun summarizePhase(data: AppData, phaseId: String): PhaseSummary {
    val workouts = data.history.filter { it.phaseId == phaseId }.sortedBy { it.timestamp }
    val entries = workouts.flatMap { it.entries }.filter { it.isPerformed }
    return PhaseSummary(workouts.size, entries.sumOf { it.volume },
        entries.map { it.performedId }.distinct().size,
        entries.maxOfOrNull { it.weight } ?: 0.0,
        workouts.firstOrNull()?.timestamp, workouts.lastOrNull()?.timestamp)
}

// A workout's numbers may be corrected later. Neither the phase counter nor sets change.
fun correctLoggedExercise(data: AppData, workoutId: String, performedId: String,
                          weight: Double, reps: Int): AppData? {
    if (!weight.isFinite() || weight < 0 || reps <= 0) return null
    val workout = data.history.firstOrNull { it.id == workoutId } ?: return null
    if (workout.entries.none { it.performedId == performedId && it.isPerformed }) return null
    return data.copy(history = data.history.map { w ->
        if (w.id != workoutId) w else w.copy(entries = w.entries.map { e ->
            if (e.performedId == performedId && e.isPerformed) e.copy(weight = weight, reps = reps) else e
        })
    })
}

// A day is moved with its entire plan and remains a single selected training day.
// The destination must be unused so no existing day is overwritten.
fun moveTrainingDay(data: AppData, from: Int, to: Int): AppData? {
    if (from == to || from !in 0..6 || to !in 0..6) return null
    val src = data.plan(from); val dst = data.plan(to)
    if (!src.enabled || dst.enabled || dst.exercises.isNotEmpty() || dst.muscles.isNotBlank()) return null
    return data.copy(plans = data.plans.map {
        when (it.day) {
            from -> it.copy(enabled = false, muscles = "", exercises = emptyList())
            to -> it.copy(enabled = true, muscles = src.muscles, exercises = src.exercises)
            else -> it
        }
    })
}

// Existing real logs can be given metadata manually if an old app version did not save it.
fun nameLegacyPhase(data: AppData, phaseId: String, name: String, finishedAt: Long?): AppData? {
    if (name.isBlank() || phaseId == data.phase.id || data.pastPhases.any { it.id == phaseId } ||
        data.history.none { it.phaseId == phaseId }) return null
    return data.copy(pastPhases = data.pastPhases + Phase(id = phaseId, name = name.trim(),
        weeks = 0, unitsPerWeek = 0, finishedAt = finishedAt, archivedAt = finishedAt))
}

/** Store an exercise in one day while reusing the ID for identical names elsewhere.
 *  Tile IDs never change on edit, so dragging, status and same-week protection remain safe. */
fun saveExerciseInPlan(data: AppData, day: Int, edited: Exercise): AppData {
    val catalog = data.exerciseCatalog.toMutableList()
    fun resolve(name: String): String {
        val key = exerciseNameKey(name)
        val found = catalog.firstOrNull { exerciseNameKey(it.name) == key }
        if (found != null) return found.id
        val added = ExerciseDefinition(name = name.trim())
        catalog += added
        return added.id
    }
    val merged = edited.copy(
        exerciseKey = resolve(edited.name),
        alternatives = edited.alternatives.map { alt -> alt.copy(exerciseKey = resolve(alt.name)) }
    )
    return data.copy(
        exerciseCatalog = catalog,
        plans = data.plans.map { plan ->
            if (plan.day != day) plan else plan.copy(exercises =
                if (plan.exercises.any { it.id == merged.id })
                    plan.exercises.map { if (it.id == merged.id) merged else it }
                else plan.exercises + merged)
        }
    )
}

/** Duplicate a weekday's complete plan. The new tiles are independently editable,
 *  but both days point at the SAME canonical exercise identities. No destination overwrite. */
fun duplicateTrainingDay(data: AppData, from: Int, to: Int): AppData? {
    if (from == to || from !in 0..6 || to !in 0..6 || !data.phase.isConfigured ||
        data.phase.finishedAt != null) return null
    val src = data.plan(from)
    val dst = data.plan(to)
    if (!src.enabled || src.exercises.isEmpty() || dst.exercises.isNotEmpty() || dst.muscles.isNotBlank()) return null
    val copy = src.exercises.map { ex ->
        ex.copy(id = makeId(), alternatives = ex.alternatives.map { it.copy(id = makeId()) })
    }
    val plans = data.plans.map { plan ->
        if (plan.day == to) plan.copy(enabled = true, muscles = src.muscles, exercises = copy) else plan
    }
    return data.copy(plans = plans, phase = data.phase.copy(unitsPerWeek = selectedDays(plans).size))
}

/** Upgrade older app data: assign one persistent exercise ID to each unique name.
 * All real historic numbers, timestamps, sets and tile/original IDs stay untouched.
 * Performed IDs are unified so historic Monday and Thursday logs share one analysis. */
fun migrateExerciseIdentities(data: AppData): AppData {
    val byName = linkedMapOf<String, ExerciseDefinition>()
    data.exerciseCatalog.forEach { definition ->
        val key = exerciseNameKey(definition.name)
        if (key.isNotBlank() && key !in byName) byName[key] = definition
    }
    fun register(name: String, preferredId: String = ""): String {
        val key = exerciseNameKey(name)
        if (key.isBlank()) return preferredId.ifBlank { makeId() }
        val found = byName[key]
        if (found != null) return found.id
        // A legacy tile could have been renamed while keeping its old ID.
        // Two different names must never accidentally share one canonical ID.
        val id = preferredId.takeIf { candidate ->
            candidate.isNotBlank() && byName.values.none { it.id == candidate }
        } ?: makeId()
        byName[key] = ExerciseDefinition(id, name.trim())
        return id
    }
    // Historic names first retain their existing identity wherever possible.
    data.history.sortedBy { it.timestamp }.forEach { workout ->
        workout.entries.forEach { entry -> register(entry.name, entry.performedId) }
    }
    val plans = data.plans.map { plan -> plan.copy(exercises = plan.exercises.map { ex ->
        ex.copy(exerciseKey = register(ex.name, ex.exerciseKey),
            alternatives = ex.alternatives.map { alt ->
                alt.copy(exerciseKey = register(alt.name, alt.exerciseKey))
            })
    }) }
    val history = data.history.map { workout -> workout.copy(entries = workout.entries.map { entry ->
        val id = register(entry.name, entry.performedId)
        if (entry.performedId == id) entry else entry.copy(performedId = id)
    }) }
    return data.copy(plans = plans, history = history, exerciseCatalog = byName.values.toList())
}
