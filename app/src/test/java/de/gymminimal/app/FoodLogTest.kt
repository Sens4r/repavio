package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodLogTest {
    private val day = "2026-11-24"
    private val chicken = BASE_FOODS.first { it.name == "Hähnchenbrust, roh" }

    @Test fun baseListIsConsistent() {
        assertTrue(BASE_FOODS.size >= 100)
        assertEquals(BASE_FOODS.size, BASE_FOODS.map { it.id }.toSet().size)
        BASE_FOODS.forEach { f ->
            assertTrue(f.name, f.per100.valid)
            val n = f.per100
            assertTrue(f.name, n.protein + n.fat + n.carbs + n.fiber <= 100.5)
            assertTrue(f.name, n.sugar <= n.carbs + 0.01)
            if (!f.name.startsWith("Bier")) {
                val energy = 4 * n.protein + 9 * n.fat + 4 * n.carbs + 2 * n.fiber
                assertTrue(f.name, kotlin.math.abs(energy - n.kcal) <= maxOf(12.0, n.kcal * 0.1))
            }
        }
    }

    @Test fun idsAreStable() {
        assertEquals("b:haehnchenbrust-roh", chicken.id)
        assertEquals("b:milch-1-5", BASE_FOODS.first { it.name == "Milch 1,5 %" }.id)
    }

    @Test fun entryScalesPer100() {
        val e = entryFor(chicken, 200.0, 1, day, "e1")!!
        assertEquals(220.0, e.values.kcal, 0.01)
        assertEquals(46.0, e.values.protein, 0.01)
        assertNull(entryFor(chicken, 0.0, 1, day, "x"))
        assertNull(entryFor(chicken, 100.0, 7, day, "x"))
        assertNull(entryFor(chicken, Double.NaN, 1, day, "x"))
    }

    @Test fun totalsAndRecent() {
        var log = addEntry(FoodLog(), entryFor(chicken, 200.0, 1, day, "e1")!!)
        log = addEntry(log, quickEntry("", Nutrients(kcal = 100.0, protein = 10.0), 3, day, "e2")!!)
        log = addEntry(log, entryFor(chicken, 100.0, 1, "2026-11-23", "e3")!!)
        assertEquals(320.0, dayTotals(log, day).kcal, 0.01)
        assertEquals(220.0, mealTotals(log, day, 1).kcal, 0.01)
        assertEquals(listOf(chicken.id), log.recent)
        assertEquals("Schnelleingabe", log.entries[1].name)
    }

    @Test fun quickEntryNeedsValues() {
        assertNull(quickEntry("x", Nutrients(), 0, day, "q"))
        assertNull(quickEntry("x", Nutrients(kcal = -5.0), 0, day, "q"))
        assertNotNull(quickEntry("x", Nutrients(protein = 20.0), 0, day, "q"))
    }

    @Test fun amountChangeAndRemove() {
        val e = entryFor(chicken, 200.0, 1, day, "e1")!!
        val half = withAmount(e, 100.0)!!
        assertEquals(110.0, half.values.kcal, 0.01)
        assertNull(withAmount(quickEntry("q", Nutrients(kcal = 50.0), 0, day, "q")!!, 10.0))
        val log = removeEntry(replaceEntry(addEntry(FoodLog(), e), half), "e1")
        assertTrue(log.entries.isEmpty())
    }

    @Test fun copyMealToToday() {
        var log = addEntry(FoodLog(), entryFor(chicken, 200.0, 1, "2026-11-23", "e1")!!)
        log = copyMeal(log, "2026-11-23", day, 1) { "c$it" }
        assertEquals(2, log.entries.size)
        assertEquals(220.0, mealTotals(log, day, 1).kcal, 0.01)
        assertEquals(log, copyMeal(log, "2026-11-20", day, 1) { "z" })
    }

    @Test fun waterAndFavorites() {
        var log = addWater(FoodLog(), day, 250)
        log = addWater(log, day, 250)
        assertEquals(500, log.water[day])
        log = addWater(log, day, -900)
        assertFalse(day in log.water)
        log = toggleFavorite(log, chicken.id)
        assertEquals(listOf(chicken), favoriteFoods(log))
        assertTrue(toggleFavorite(log, chicken.id).favorites.isEmpty())
        assertEquals(2500, waterGoalMl(null))
        assertEquals(2750, waterGoalMl(80.0))
    }

    @Test fun searchIgnoresUmlautsAndPrefersRecentAndCustom() {
        assertTrue(searchFoods(FoodLog(), "hahnchen").any { it.id == chicken.id })
        assertTrue(searchFoods(FoodLog(), "Hähnchen roh").first().name.startsWith("Hähnchen"))
        val own = Food("c:1", "Hähnchen-Aufschnitt", Nutrients(105.0, 21.0, 2.0, 1.0), custom = true)
        val log = addEntry(upsertCustomFood(FoodLog(), own),
            entryFor(BASE_FOODS.first { it.name == "Hähnchenbrust, gebraten" }, 100.0, 0, day, "e")!!)
        val result = searchFoods(log, "hähnchen")
        assertEquals("Hähnchenbrust, gebraten", result[0].name)
        assertEquals("Hähnchen-Aufschnitt", result[1].name)
        assertTrue(searchFoods(log, "").first().name == "Hähnchenbrust, gebraten")
    }

    @Test fun customFoodValidation() {
        val ok = Food("c:1", "Riegel", Nutrients(360.0, 30.0, 12.0, 30.0, 5.0, 3.0, 0.5),
            portions = listOf(FoodPortion("1 Riegel", 50.0)))
        assertTrue(validCustomFood(ok))
        assertFalse(validCustomFood(ok.copy(name = " ")))
        assertFalse(validCustomFood(ok.copy(per100 = Nutrients(kcal = 1200.0))))
        assertFalse(validCustomFood(ok.copy(per100 = Nutrients(100.0, 60.0, 30.0, 30.0))))
        val log = deleteCustomFood(toggleFavorite(upsertCustomFood(FoodLog(), ok), "c:1"), "c:1")
        assertTrue(log.customFoods.isEmpty() && log.favorites.isEmpty())
    }

    @Test fun numberParsing() {
        assertEquals(1.5, parseNumber("1,5")!!, 0.0)
        assertNull(parseNumber("abc"))
        assertEquals("12,5", formatAmount(12.5))
        assertEquals("200", formatAmount(200.0))
    }
}

class RecipeTest {
    private val oats = BASE_FOODS.first { it.name == "Haferflocken" }
    private val milk = BASE_FOODS.first { it.name == "Milch 1,5 %" }

    @Test fun recipeBecomesFoodWithPortion() {
        val r = Recipe("r:1", "Porridge", 2, listOf(ingredientFor(oats, 100.0)!!, ingredientFor(milk, 300.0)!!))
        assertTrue(validRecipe(r))
        val food = recipeFood(r)!!
        assertTrue(food.recipe)
        assertEquals(200.0, food.portions.single().grams, 0.001)
        val entry = entryFor(food, 200.0, 0, "2026-11-24", "e")!!
        assertEquals((372.0 + 141.0) / 2, entry.values.kcal, 0.01)
    }

    @Test fun recipesAreSearchableAndDeletable() {
        val r = Recipe("r:1", "Porridge", 1, listOf(ingredientFor(oats, 80.0)!!))
        var log = upsertRecipe(FoodLog(), r)
        assertEquals("Porridge", searchFoods(log, "porr").first().name)
        log = deleteRecipe(toggleFavorite(log, "r:1"), "r:1")
        assertTrue(log.recipes.isEmpty() && log.favorites.isEmpty())
    }

    @Test fun invalidRecipes() {
        assertFalse(validRecipe(Recipe("r:1", "", 1, listOf(ingredientFor(oats, 80.0)!!))))
        assertFalse(validRecipe(Recipe("r:1", "X", 0, listOf(ingredientFor(oats, 80.0)!!))))
        assertFalse(validRecipe(Recipe("r:1", "X", 1, emptyList())))
        assertNull(ingredientFor(recipeFood(Recipe("r:1", "X", 1, listOf(ingredientFor(oats, 80.0)!!)))!!, 10.0))
    }
}
