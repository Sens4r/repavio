package de.gymminimal.app

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** Nährwerte. Je 100 g/ml bei Lebensmitteln, absolut bei Tagebucheinträgen. Kohlenhydrate ohne Ballaststoffe (EU). */
data class Nutrients(
    val kcal: Double = 0.0, val protein: Double = 0.0, val fat: Double = 0.0, val carbs: Double = 0.0,
    val fiber: Double = 0.0, val sugar: Double = 0.0, val salt: Double = 0.0
) {
    operator fun plus(o: Nutrients) = Nutrients(kcal + o.kcal, protein + o.protein, fat + o.fat, carbs + o.carbs,
        fiber + o.fiber, sugar + o.sugar, salt + o.salt)
    fun scaled(factor: Double) = Nutrients(kcal * factor, protein * factor, fat * factor, carbs * factor,
        fiber * factor, sugar * factor, salt * factor)
    val valid: Boolean get() = listOf(kcal, protein, fat, carbs, fiber, sugar, salt).all { it.isFinite() && it >= 0 }
}

data class FoodPortion(val label: String, val grams: Double)

data class Food(
    val id: String, val name: String, val per100: Nutrients,
    val ml: Boolean = false, val portions: List<FoodPortion> = emptyList(), val custom: Boolean = false,
    val recipe: Boolean = false,
    /** Gramm (bei Getränken ml) je Teelöffel und Esslöffel; null = Richtwert 5 / 15. */
    val tl: Double? = null, val el: Double? = null,
    /** Gramm je Stück (Ei, Banane, Scheibe …); null = Einheit „Stück“ nicht anbieten. */
    val piece: Double? = null
) {
    val unit: String get() = if (ml) "ml" else "g"
}

data class DiaryEntry(
    val id: String, val day: String, val meal: Int, val name: String,
    /** null bei Schnelleingabe. */
    val amount: Double?, val ml: Boolean, val values: Nutrients, val foodId: String? = null
)

/** Zutat eines Rezepts: Schnappschuss der Werte zur gewählten Menge. */
data class Ingredient(val foodId: String?, val name: String, val amount: Double, val ml: Boolean, val values: Nutrients)

data class Recipe(val id: String, val name: String, val portions: Int, val ingredients: List<Ingredient>)

data class FoodLog(
    val entries: List<DiaryEntry> = emptyList(),
    val water: Map<String, Int> = emptyMap(),
    val customFoods: List<Food> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val recent: List<String> = emptyList(),
    val recipes: List<Recipe> = emptyList()
)

val MEALS = listOf("Frühstück", "Mittagessen", "Abendessen", "Snacks")
const val FIBER_GOAL_G = 30
const val SALT_LIMIT_G = 6
const val WATER_STEP_ML = 250
const val MAX_RECENT = 30
const val MAX_AMOUNT = 5000.0

fun dayKey(date: LocalDate): String = date.toString()

/** Wasser-Richtwert: 35 ml je kg Körpergewicht, auf 0,25 L gerundet; ohne Gewicht 2,5 L. */
fun waterGoalMl(weightKg: Double?): Int {
    if (weightKg == null || !validWeight(weightKg)) return 2500
    return ((weightKg * 35 / 250.0).roundToInt() * 250).coerceIn(1500, 5000)
}

fun entryFor(food: Food, amount: Double, meal: Int, day: String, id: String): DiaryEntry? {
    if (!amount.isFinite() || amount <= 0 || amount > MAX_AMOUNT || meal !in MEALS.indices) return null
    return DiaryEntry(id, day, meal, food.name, amount, food.ml, food.per100.scaled(amount / 100.0), food.id)
}

fun quickEntry(name: String, values: Nutrients, meal: Int, day: String, id: String): DiaryEntry? {
    if (!values.valid || values.kcal <= 0 && values.protein <= 0 && values.fat <= 0 && values.carbs <= 0) return null
    if (meal !in MEALS.indices) return null
    return DiaryEntry(id, day, meal, name.trim().ifBlank { "Schnelleingabe" }.take(60), null, false, values)
}

/** Menge ändern: Nährwerte proportional zur bisherigen Menge. Schnelleingaben bleiben unverändert. */
fun withAmount(entry: DiaryEntry, amount: Double): DiaryEntry? {
    val old = entry.amount ?: return null
    if (!amount.isFinite() || amount <= 0 || amount > MAX_AMOUNT || old <= 0) return null
    return entry.copy(amount = amount, values = entry.values.scaled(amount / old))
}

fun dayEntries(log: FoodLog, day: String): List<DiaryEntry> = log.entries.filter { it.day == day }

fun dayTotals(log: FoodLog, day: String): Nutrients =
    dayEntries(log, day).fold(Nutrients()) { acc, e -> acc + e.values }

fun mealTotals(log: FoodLog, day: String, meal: Int): Nutrients =
    dayEntries(log, day).filter { it.meal == meal }.fold(Nutrients()) { acc, e -> acc + e.values }

fun addEntry(log: FoodLog, entry: DiaryEntry): FoodLog = log.copy(
    entries = log.entries + entry,
    recent = entry.foodId?.let { id -> (listOf(id) + log.recent.filterNot { it == id }).take(MAX_RECENT) } ?: log.recent
)

fun replaceEntry(log: FoodLog, entry: DiaryEntry): FoodLog =
    log.copy(entries = log.entries.map { if (it.id == entry.id) entry else it })

fun removeEntry(log: FoodLog, id: String): FoodLog = log.copy(entries = log.entries.filterNot { it.id == id })

/** Kopiert eine Mahlzeit eines Tages in eine Mahlzeit eines anderen Tages. */
fun copyMeal(log: FoodLog, fromDay: String, toDay: String, meal: Int, newId: (Int) -> String): FoodLog {
    val source = dayEntries(log, fromDay).filter { it.meal == meal }
    if (source.isEmpty()) return log
    return log.copy(entries = log.entries + source.mapIndexed { i, e -> e.copy(id = newId(i), day = toDay) })
}

/** Übernimmt ausgewählte Mahlzeiten (und auf Wunsch das Wasser) eines Tages; vorhandene Einträge bleiben. */
fun copyDay(log: FoodLog, fromDay: String, toDay: String, meals: Set<Int>, water: Boolean, newId: (Int) -> String): FoodLog {
    if (fromDay == toDay) return log
    val source = dayEntries(log, fromDay).filter { it.meal in meals }
    val withEntries = log.copy(entries = log.entries + source.mapIndexed { i, e -> e.copy(id = newId(i), day = toDay) })
    val ml = log.water[fromDay] ?: 0
    return if (water && ml > 0) addWater(withEntries, toDay, ml) else withEntries
}

fun addWater(log: FoodLog, day: String, deltaMl: Int): FoodLog {
    val next = ((log.water[day] ?: 0) + deltaMl).coerceIn(0, 10_000)
    return log.copy(water = if (next == 0) log.water - day else log.water + (day to next))
}

fun toggleFavorite(log: FoodLog, foodId: String): FoodLog =
    log.copy(favorites = if (foodId in log.favorites) log.favorites - foodId else log.favorites + foodId)

/** Mengeneinheiten bei der Eingabe: Gramm (bzw. ml), Teelöffel, Esslöffel, Stück (nur mit Stückgewicht). */
fun amountUnits(food: Food?): List<String> = listOf("g", "TL", "EL") + if (food?.piece != null) listOf("Stück") else emptyList()
const val DEFAULT_TL = 5.0
const val DEFAULT_EL = 15.0

/** Gramm (bzw. ml) je Einheit. Ohne eigene Angabe gelten 5 / 15 als Richtwert (Haushaltslöffel, wie Wasser). */
fun gramsPerUnit(food: Food?, unit: String): Double = when (unit) {
    "TL" -> food?.tl ?: DEFAULT_TL
    "EL" -> food?.el ?: DEFAULT_EL
    "Stück" -> food?.piece ?: 1.0
    else -> 1.0
}

private fun validSpoon(v: Double?) = v == null || v.isFinite() && v in 0.5..100.0

fun validCustomFood(food: Food): Boolean =
    food.name.isNotBlank() && food.per100.valid && food.per100.kcal <= 950 && validSpoon(food.tl) && validSpoon(food.el) &&
        (food.piece == null || food.piece.isFinite() && food.piece in 0.5..MAX_AMOUNT) &&
        food.per100.protein + food.per100.fat + food.per100.carbs + food.per100.fiber <= 100.5 &&
        food.portions.all { it.label.isNotBlank() && it.grams > 0 && it.grams <= MAX_AMOUNT }

fun upsertCustomFood(log: FoodLog, food: Food): FoodLog {
    val f = food.copy(name = food.name.trim().take(60), custom = true)
    return log.copy(customFoods = log.customFoods.filterNot { it.id == f.id } + f)
}

/** Eigenes Lebensmittel löschen. Tagebucheinträge behalten ihre gespeicherten Werte. */
fun deleteCustomFood(log: FoodLog, id: String): FoodLog = log.copy(
    customFoods = log.customFoods.filterNot { it.id == id },
    favorites = log.favorites - id, recent = log.recent - id
)

fun allFoods(log: FoodLog): List<Food> = log.customFoods + log.recipes.mapNotNull(::recipeFood) + BASE_FOODS

// ---------------------------------------------------------------- Rezepte

const val MAX_PORTIONS = 50
const val MAX_INGREDIENTS = 50

fun recipeTotal(recipe: Recipe): Nutrients = recipe.ingredients.fold(Nutrients()) { acc, i -> acc + i.values }
fun recipeWeight(recipe: Recipe): Double = recipe.ingredients.sumOf { it.amount }

/** Ein Rezept wird wie ein Lebensmittel eingetragen: Werte je 100 g des Gesamtgewichts, Portion = Gesamt / Portionen. */
fun recipeFood(recipe: Recipe): Food? {
    val weight = recipeWeight(recipe)
    if (weight <= 0 || recipe.portions !in 1..MAX_PORTIONS) return null
    return Food(recipe.id, recipe.name, recipeTotal(recipe).scaled(100.0 / weight), false,
        listOf(FoodPortion("1 Portion", weight / recipe.portions)), recipe = true)
}

fun ingredientFor(food: Food, amount: Double): Ingredient? {
    if (food.recipe || !amount.isFinite() || amount <= 0 || amount > MAX_AMOUNT) return null
    return Ingredient(food.id, food.name, amount, food.ml, food.per100.scaled(amount / 100.0))
}

fun validRecipe(recipe: Recipe): Boolean =
    recipe.name.isNotBlank() && recipe.portions in 1..MAX_PORTIONS &&
        recipe.ingredients.size in 1..MAX_INGREDIENTS &&
        recipe.ingredients.all { it.amount > 0 && it.amount <= MAX_AMOUNT && it.values.valid }

fun upsertRecipe(log: FoodLog, recipe: Recipe): FoodLog {
    val r = recipe.copy(name = recipe.name.trim().take(60))
    return log.copy(recipes = log.recipes.filterNot { it.id == r.id } + r)
}

/** Rezept löschen. Bereits eingetragene Portionen behalten ihre Werte. */
fun deleteRecipe(log: FoodLog, id: String): FoodLog = log.copy(
    recipes = log.recipes.filterNot { it.id == id }, favorites = log.favorites - id, recent = log.recent - id
)

fun normalizeFood(text: String): String {
    val lower = text.lowercase(Locale.GERMANY)
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
    return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}

/** Suchschlüssel: "Hähnchen", "Haehnchen" und "Hahnchen" sind gleich. */
fun searchKey(text: String): String =
    normalizeFood(text).replace("ae", "a").replace("oe", "o").replace("ue", "u")

/** Suche: alle Wörter müssen vorkommen. Zuletzt genutzte und eigene zuerst, dann Namensanfang. */
fun searchFoods(log: FoodLog, query: String, limit: Int = 40): List<Food> {
    val tokens = searchKey(query).split(Regex("[\\s,]+")).filter { it.isNotBlank() }
    val foods = allFoods(log)
    if (tokens.isEmpty()) return recentFoods(log).ifEmpty { foods }.take(limit)
    return foods.map { it to searchKey(it.name) }
        .filter { (_, n) -> tokens.all { n.contains(it) } }
        .sortedWith(compareBy(
            { (f, _) -> log.recent.indexOf(f.id).let { if (it < 0) Int.MAX_VALUE else it } },
            { (f, _) -> if (f.custom || f.recipe) 0 else 1 },
            { (_, n) -> if (n.startsWith(tokens.first())) 0 else 1 },
            { (_, n) -> n.length }))
        .map { it.first }.take(limit)
}

fun recentFoods(log: FoodLog): List<Food> {
    val byId = allFoods(log).associateBy { it.id }
    return log.recent.mapNotNull { byId[it] }
}

fun favoriteFoods(log: FoodLog): List<Food> = allFoods(log).filter { it.id in log.favorites }

fun foodById(log: FoodLog, id: String?): Food? = id?.let { i -> allFoods(log).firstOrNull { it.id == i } }

fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.GERMANY, "%.1f", value)

fun formatGrams(value: Double): String =
    if (value >= 10) value.roundToInt().toString() else String.format(Locale.GERMANY, "%.1f", value).removeSuffix(",0")

fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
