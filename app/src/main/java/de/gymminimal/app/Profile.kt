package de.gymminimal.app

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** One weighing. Only real entries are stored; there are no gaps or zero values. */
data class WeightEntry(val timestamp: Long, val kg: Double)

/**
 * Personal data. Stored only on the device, encrypted (see ProfileStore).
 * The age is never stored: it is derived from the birth date on every display.
 */
data class Profile(
    val name: String = "",
    val birthDate: String = "",      // ISO yyyy-MM-dd, empty = unknown
    val sex: String = "",            // "m" | "w" (Mifflin-St Jeor knows these two variants)
    val heightCm: Int = 0,
    val activity: String = "mittel",
    val goal: String = "aufbau",
    val avatar: String = "",         // key of a built-in avatar, empty = none
    val hasPhoto: Boolean = false,
    val weights: List<WeightEntry> = emptyList()
) {
    val currentWeight: Double? get() = weights.maxByOrNull { it.timestamp }?.kg
}

data class Choice(val key: String, val label: String, val detail: String, val factor: Double)

val ACTIVITY_LEVELS = listOf(
    Choice("wenig", "Wenig", "Sitzend, kaum Bewegung", 1.2),
    Choice("leicht", "Leicht", "1–3 Trainings pro Woche", 1.375),
    Choice("mittel", "Mittel", "3–5 Trainings pro Woche", 1.55),
    Choice("hoch", "Hoch", "6–7 Trainings pro Woche", 1.725),
    Choice("sehr_hoch", "Sehr hoch", "Körperlicher Beruf und Training", 1.9)
)

val NUTRITION_GOALS = listOf(
    Choice("aufbau", "Aufbau", "Erhaltungsbedarf + 10 %", 1.10),
    Choice("halten", "Halten", "Erhaltungsbedarf", 1.0),
    Choice("definition", "Definition", "Erhaltungsbedarf − 20 %", 0.80)
)

const val PROTEIN_G_PER_KG = 2.0
const val FAT_G_PER_KG = 0.9

fun activityLevel(key: String): Choice = ACTIVITY_LEVELS.firstOrNull { it.key == key } ?: ACTIVITY_LEVELS[2]
fun nutritionGoal(key: String): Choice = NUTRITION_GOALS.firstOrNull { it.key == key } ?: NUTRITION_GOALS[0]

private val GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)

/** "TT.MM.JJJJ" → ISO date, or null if invalid or implausible. */
fun parseBirthDate(input: String, today: LocalDate = LocalDate.now()): String? {
    val date = runCatching { LocalDate.parse(input.trim(), GERMAN_DATE) }.getOrNull() ?: return null
    val age = Period.between(date, today).years
    return if (date.isAfter(today) || age !in 10..110) null else date.toString()
}

fun formatBirthDate(iso: String): String =
    runCatching { LocalDate.parse(iso).format(GERMAN_DATE) }.getOrDefault("")

/** Age in full years, recalculated from the birth date each time. */
fun ageOn(birthDate: String, today: LocalDate = LocalDate.now()): Int? {
    val date = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
    if (date.isAfter(today)) return null
    return Period.between(date, today).years
}

fun validHeight(cm: Int): Boolean = cm in 100..250
fun validWeight(kg: Double): Boolean = kg.isFinite() && kg in 25.0..400.0

private fun dayOf(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

/** Logs a weight. A second entry on the same calendar day replaces the first one. */
fun addWeight(profile: Profile, kg: Double, timestamp: Long): Profile? {
    if (!validWeight(kg)) return null
    val day = dayOf(timestamp)
    val rounded = (kg * 10).roundToInt() / 10.0
    val kept = profile.weights.filterNot { dayOf(it.timestamp) == day }
    return profile.copy(weights = (kept + WeightEntry(timestamp, rounded)).sortedBy { it.timestamp })
}

fun deleteWeight(profile: Profile, timestamp: Long): Profile =
    profile.copy(weights = profile.weights.filterNot { it.timestamp == timestamp })

data class NutritionResult(
    val bmr: Int, val maintenance: Int, val kcal: Int,
    val proteinG: Int, val fatG: Int, val carbsG: Int,
    val activity: Choice, val goal: Choice, val weightKg: Double, val age: Int
)

/** Which profile fields the calculation still needs. */
fun missingForNutrition(profile: Profile, today: LocalDate = LocalDate.now()): List<String> = buildList {
    if (ageOn(profile.birthDate, today) == null) add("Geburtsdatum")
    if (profile.sex !in listOf("m", "w")) add("Geschlecht")
    if (!validHeight(profile.heightCm)) add("Größe")
    if (profile.currentWeight?.let(::validWeight) != true) add("Gewicht")
}

/**
 * Mifflin-St Jeor: 10 × kg + 6,25 × cm − 5 × Jahre + 5 (m) bzw. − 161 (w).
 * Bedarf = Grundumsatz × Aktivitätsfaktor × Zielfaktor.
 * Protein 2,0 g/kg, Fett 0,9 g/kg, Kohlenhydrate = verbleibende Kalorien / 4.
 */
fun nutritionFor(profile: Profile, today: LocalDate = LocalDate.now()): NutritionResult? {
    if (missingForNutrition(profile, today).isNotEmpty()) return null
    val age = ageOn(profile.birthDate, today)!!
    val kg = profile.currentWeight!!
    val bmr = 10 * kg + 6.25 * profile.heightCm - 5 * age + if (profile.sex == "m") 5 else -161
    val activity = activityLevel(profile.activity)
    val goal = nutritionGoal(profile.goal)
    val maintenance = bmr * activity.factor
    val kcal = (maintenance * goal.factor).roundToInt()
    val protein = (kg * PROTEIN_G_PER_KG).roundToInt()
    val fat = (kg * FAT_G_PER_KG).roundToInt()
    val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
    return NutritionResult(bmr.roundToInt(), maintenance.roundToInt(), kcal, protein, fat, carbs,
        activity, goal, kg, age)
}

data class AvatarOption(val key: String, val label: String)

val AVATARS = listOf(
    AvatarOption("hantel", "Hantel"), AvatarOption("blitz", "Blitz"),
    AvatarOption("flamme", "Flamme"), AvatarOption("berg", "Berg"),
    AvatarOption("lauf", "Laufen"), AvatarOption("herz", "Herz"),
    AvatarOption("stern", "Stern"), AvatarOption("blatt", "Blatt")
)
