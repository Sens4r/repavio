package de.gymminimal.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private fun newId(prefix: String) = prefix + UUID.randomUUID().toString().replace("-", "").take(16)
private fun kcalText(v: Double) = String.format(Locale.GERMANY, "%,d", v.roundToInt())
private fun g(v: Double) = formatGrams(v)
private val dayFormat = DateTimeFormatter.ofPattern("EE, dd.MM.", Locale.GERMANY)

private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Heute"
    today.minusDays(1) -> "Gestern"
    else -> date.format(dayFormat)
}

private fun foodDetail(f: Food): String {
    val portion = f.portions.firstOrNull()
    if (f.recipe && portion != null) {
        val n = f.per100.scaled(portion.grams / 100.0)
        return "1 Portion · ${kcalText(n.kcal)} kcal · ${g(n.protein)} g Protein"
    }
    return "${kcalText(f.per100.kcal)} kcal · ${g(f.per100.protein)} g Protein / 100 ${f.unit}"
}

/** Ernährung: Tagebuch mit Bedarf als Ziel. Alles lokal, verschlüsselt wie das Profil. */
@Composable
fun NutritionScreen(
    profile: Profile, log: FoodLog, onLog: (FoodLog) -> Unit,
    onGoal: (String) -> Unit, onOpenProfile: () -> Unit
) {
    var showNeed by rememberSaveable { mutableStateOf(false) }
    var addMeal by rememberSaveable { mutableStateOf(-1) }
    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val date = today.minusDays(dayOffset.toLong())
    val day = dayKey(date)

    when {
        showNeed -> {
            BackHandler { showNeed = false }
            NeedScreen(profile, onGoal, onOpenProfile, onBack = { showNeed = false })
        }
        addMeal >= 0 -> {
            BackHandler { addMeal = -1 }
            AddFoodScreen(log, day, addMeal, onLog, onClose = { addMeal = -1 })
        }
        else -> DiaryPage(profile, log, onLog, date, today,
            onDay = { dayOffset = (dayOffset + it).coerceAtLeast(0) },
            onAdd = { addMeal = it }, onNeed = { showNeed = true })
    }
}

@Composable
private fun DiaryPage(
    profile: Profile, log: FoodLog, onLog: (FoodLog) -> Unit, date: LocalDate, today: LocalDate,
    onDay: (Int) -> Unit, onAdd: (Int) -> Unit, onNeed: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val day = dayKey(date)
    val need = nutritionFor(profile)
    val total = dayTotals(log, day)
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDay(1) }) { Icon(Icons.Outlined.ChevronLeft, "Vorheriger Tag", tint = SubtleText) }
            Text(buildAnnotatedString {
                append(dayLabel(date, today))
                if (date == today || date == today.minusDays(1)) withStyle(SpanStyle(color = SubtleText,
                    fontWeight = FontWeight.Medium)) { append(" · " + date.format(dayFormat)) }
            }, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            IconButton(onClick = { onDay(-1) }, enabled = date < today) {
                Icon(Icons.Outlined.ChevronRight, "Nächster Tag", tint = if (date < today) SubtleText else Color.Transparent)
            }
        }

        // Übersicht: verbleibende Kalorien und Makros.
        val onC = scheme.onPrimaryContainer
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.primaryContainer)
            .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    val rest = need?.let { it.kcal - total.kcal }
                    Text(when { rest == null -> "GEGESSEN"; rest >= 0 -> "NOCH ÜBRIG"; else -> "ÜBER DEM ZIEL" },
                        color = onC.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(buildAnnotatedString {
                        append(kcalText(if (rest == null) total.kcal else kotlin.math.abs(rest)))
                        withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = onC.copy(alpha = .75f))) { append(" kcal") }
                    }, color = onC, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp,
                        lineHeight = 42.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (need != null) Text("${kcalText(total.kcal)} gegessen", color = onC.copy(alpha = .85f), fontSize = 12.sp)
                    Text(if (need != null) "Ziel ${kcalText(need.kcal.toDouble())} ›" else "Ziel festlegen ›",
                        color = onC, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onNeed).padding(4.dp))
                }
            }
            if (need != null) Bar((total.kcal / need.kcal).toFloat(), 10, scheme.primary, onC.copy(alpha = .14f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Triple("Protein", total.protein, need?.proteinG), Triple("Fett", total.fat, need?.fatG),
                    Triple("Kohlenhydrate", total.carbs, need?.carbsG)).forEachIndexed { i, (label, value, goal) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(label, color = onC.copy(alpha = .85f), fontSize = 11.sp, maxLines = 1)
                        if (goal != null) Bar((value / goal).toFloat(), 6, if (i == 0) scheme.primary else onC,
                            onC.copy(alpha = .14f))
                        Text(buildAnnotatedString {
                            append(value.roundToInt().toString())
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = onC.copy(alpha = .75f))) {
                                append(if (goal != null) " / $goal g" else " g")
                            }
                        }, color = onC, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }

        // Wasser und Zusatzwerte.
        val water = log.water[day] ?: 0
        val waterGoal = waterGoalMl(profile.currentWeight)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            Row(Modifier.weight(1.2f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(CardBackground)
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.WaterDrop, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("${liters(water)} / ${liters(waterGoal)} L", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1)
                    Text("Wasser", fontSize = 11.sp, color = SubtleText)
                }
                if (water > 0) SmallRound("Wasser −$WATER_STEP_ML ml", Icons.Outlined.Remove) {
                    onLog(addWater(log, day, -WATER_STEP_ML))
                }
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(FieldBackground)
                    .clickable { onLog(addWater(log, day, WATER_STEP_ML)) }, contentAlignment = Alignment.Center) {
                    Text("+$WATER_STEP_ML", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(CardBackground)
                .padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp,
                Alignment.CenterVertically)) {
                MicroRow("Ballaststoffe", "${total.fiber.roundToInt()}/$FIBER_GOAL_G g")
                MicroRow("Zucker", "${total.sugar.roundToInt()} g")
                MicroRow("Salz", "${String.format(Locale.GERMANY, "%.1f", total.salt)} g",
                    if (total.salt > SALT_LIMIT_G) scheme.error else null)
            }
        }

        MEALS.forEachIndexed { meal, name ->
            val items = dayEntries(log, day).filter { it.meal == meal }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = if (items.isEmpty()) 6.dp else 10.dp)) {
                Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text(if (items.isEmpty()) "–" else "${kcalText(items.sumOf { it.values.kcal })} kcal",
                        fontSize = 12.sp, color = SubtleText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    SmallRound("Zu $name hinzufügen", Icons.Outlined.Add, size = 36) { onAdd(meal) }
                }
                items.forEach { e ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { editing = e }
                        .padding(top = 4.dp, bottom = 4.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(buildAnnotatedString {
                            append(e.name)
                            withStyle(SpanStyle(color = SubtleText)) {
                                append(" · " + (e.amount?.let { "${formatAmount(it)} ${if (e.ml) "ml" else "g"}" } ?: "Schnell"))
                            }
                        }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(e.values.kcal.roundToInt().toString(), fontSize = 13.sp, color = SubtleText)
                    }
                }
            }
        }
        Text("Grundliste mit gerundeten Richtwerten je 100 g. Für genaue Werte eigene Lebensmittel vom Etikett anlegen.",
            fontSize = 11.sp, color = SubtleText, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }

    editing?.let { entry ->
        val food = foodById(log, entry.foodId)
        PortionSheet(
            title = entry.name, food = food, entry = entry, log = log, initialMeal = entry.meal,
            onDismiss = { editing = null },
            onConfirm = { amount, meal ->
                val updated = (if (amount != null) withAmount(entry, amount) else entry)?.copy(meal = meal)
                if (updated != null) onLog(replaceEntry(log, updated))
                editing = null
            },
            onRemove = { onLog(removeEntry(log, entry.id)); editing = null },
            onFavorite = { food?.let { onLog(toggleFavorite(log, it.id)) } }
        )
    }
}

private fun liters(ml: Int) = String.format(Locale.GERMANY, "%.2f", ml / 1000.0).trimEnd('0').trimEnd(',')

@Composable
private fun MicroRow(label: String, value: String, color: Color? = null) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, color = SubtleText, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Bar(fraction: Float, height: Int, color: Color, track: Color) {
    Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(height.dp)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(height.dp))
            .background(color))
    }
}

@Composable
private fun SmallRound(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, size: Int = 40,
                       onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size / 2).dp))
        .background(FieldBackground)) {
        Icon(icon, label, modifier = Modifier.size(18.dp))
    }
}

// ---------------------------------------------------------------- Hinzufügen

@Composable
private fun AddFoodScreen(log: FoodLog, day: String, meal: Int, onLog: (FoodLog) -> Unit, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Food?>(null) }
    var quick by remember { mutableStateOf(false) }
    var customEditor by remember { mutableStateOf<Food?>(null) }
    var newCustom by remember { mutableStateOf(false) }
    var recipeEditor by remember { mutableStateOf<Recipe?>(null) }
    val yesterday = dayKey(LocalDate.parse(day).minusDays(1))
    val yesterdayMeal = mealTotals(log, yesterday, meal)
    val hasYesterday = dayEntries(log, yesterday).any { it.meal == meal }

    val results = when (filter) {
        1 -> recentFoods(log)
        2 -> favoriteFoods(log)
        3 -> log.customFoods.sortedBy { normalizeFood(it.name) }
        4 -> log.recipes.mapNotNull(::recipeFood).sortedBy { normalizeFood(it.name) }
        else -> if (query.isBlank()) recentFoods(log).take(8) else searchFoods(log, query)
    }.let { list -> if (filter != 0 && query.isNotBlank()) list.filter { f ->
        searchKey(query).split(Regex("[\\s,]+")).filter(String::isNotBlank).all { searchKey(f.name).contains(it) } } else list }

    recipeEditor?.let { recipe ->
        BackHandler { recipeEditor = null }
        RecipeEditor(recipe, log, isNew = log.recipes.none { it.id == recipe.id },
            onClose = { recipeEditor = null },
            onSave = { r -> onLog(upsertRecipe(log, r)); recipeEditor = null; filter = 4 },
            onDelete = { id -> onLog(deleteRecipe(log, id)); recipeEditor = null })
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SubPageHeader(MEALS[meal], "${kcalText(mealTotals(log, day, meal).kcal)} kcal bisher", onClose)
        }
        item {
            TextField(query, { query = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Lebensmittel suchen") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = SubtleText) },
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground, focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent))
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Alle", "Verlauf", "Favoriten", "Eigene", "Rezepte").forEachIndexed { i, label ->
                    FilterChip(selected = filter == i, onClick = { filter = i },
                        label = { Text(label, fontWeight = if (filter == i) FontWeight.ExtraBold else FontWeight.SemiBold) },
                        shape = RoundedCornerShape(17.dp), border = null,
                        colors = FilterChipDefaults.filterChipColors(containerColor = CardBackground,
                            selectedContainerColor = scheme.primary, selectedLabelColor = scheme.onPrimary))
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(vertical = 4.dp)) {
                if (results.isEmpty()) {
                    Text(when {
                        query.isNotBlank() -> "Nichts gefunden. Lege es als eigenes Lebensmittel an."
                        filter == 2 -> "Noch keine Favoriten. Markiere ein Lebensmittel mit dem Stern."
                        filter == 3 -> "Noch keine eigenen Lebensmittel."
                        filter == 4 -> "Noch keine Rezepte. Lege unten eines an."
                        else -> "Noch kein Verlauf. Suche oben nach einem Lebensmittel."
                    }, fontSize = 13.sp, color = SubtleText, modifier = Modifier.padding(16.dp))
                }
                results.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = FieldBackground)
                    FoodRow(f, tag = when {
                        f.recipe -> "Rezept"
                        f.id in log.recent.take(10) -> "Zuletzt"
                        f.custom -> "Eigenes"
                        f.id in log.favorites -> "Favorit"
                        else -> null
                    }) { picked = f }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionTile("Schnelleingabe", "kcal & Makros direkt", Modifier.weight(1f)) { quick = true }
                    ActionTile("Von gestern kopieren",
                        if (hasYesterday) "${MEALS[meal]} · ${kcalText(yesterdayMeal.kcal)} kcal" else "Gestern nichts eingetragen",
                        Modifier.weight(1f), enabled = hasYesterday) {
                        onLog(copyMeal(log, yesterday, day, meal) { newId("e") }); onClose()
                    }
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionTile("Eigenes Lebensmittel", "Nährwerte vom Etikett", Modifier.weight(1f)) { newCustom = true }
                    ActionTile("Rezept anlegen", "aus mehreren Zutaten", Modifier.weight(1f)) {
                        recipeEditor = Recipe(newId("r:"), "", 1, emptyList())
                    }
                }
            }
        }
    }

    picked?.let { food ->
        PortionSheet(title = food.name, food = food, entry = null, log = log, initialMeal = meal,
            onDismiss = { picked = null },
            onConfirm = { amount, m ->
                entryFor(food, amount ?: 100.0, m, day, newId("e"))?.let { onLog(addEntry(log, it)) }
                picked = null; onClose()
            },
            onFavorite = { onLog(toggleFavorite(log, food.id)) },
            onEditCustom = when {
                food.custom -> ({ picked = null; customEditor = food })
                food.recipe -> log.recipes.firstOrNull { it.id == food.id }?.let { r -> { picked = null; recipeEditor = r } }
                else -> null
            })
    }
    if (quick) QuickEntryDialog(onDismiss = { quick = false }) { name, values ->
        quickEntry(name, values, meal, day, newId("e"))?.let { onLog(addEntry(log, it)) }
        quick = false; onClose()
    }
    if (newCustom || customEditor != null) {
        CustomFoodDialog(customEditor, onDismiss = { newCustom = false; customEditor = null },
            onSave = { food -> onLog(upsertCustomFood(log, food)); newCustom = false; customEditor = null; picked = food },
            onDelete = { id -> onLog(deleteCustomFood(log, id)); customEditor = null })
    }
}

@Composable
private fun FoodRow(food: Food, tag: String?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(food.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(foodDetail(food), fontSize = 12.sp, color = SubtleText, maxLines = 1)
        }
        if (tag != null) Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(scheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp))
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(FieldBackground),
            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    // Mindesthöhe statt fester Höhe: Bei großer Systemschrift umbrechen statt abschneiden.
    Column(modifier.heightIn(min = 64.dp).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else SubtleText)
        Text(subtitle, fontSize = 11.5.sp, lineHeight = 14.sp, color = SubtleText)
    }
}

// ---------------------------------------------------------------- Portion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortionSheet(
    title: String, food: Food?, entry: DiaryEntry?, log: FoodLog, initialMeal: Int,
    onDismiss: () -> Unit, onConfirm: (Double?, Int) -> Unit,
    onRemove: (() -> Unit)? = null, onFavorite: () -> Unit = {}, onEditCustom: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val quickOnly = entry != null && entry.amount == null
    val ml = entry?.ml ?: food?.ml ?: false
    val unit = if (ml) "ml" else "g"
    val defaultAmount = entry?.amount ?: food?.portions?.firstOrNull()?.grams ?: 100.0
    var amountText by remember { mutableStateOf(formatAmount(defaultAmount)) }
    var meal by remember { mutableIntStateOf(initialMeal) }
    val amount = parseNumber(amountText)?.takeIf { it > 0 && it <= MAX_AMOUNT }
    // Werte je Einheit: aus dem Lebensmittel oder, bei Einträgen, aus der gespeicherten Menge.
    val perUnit: Nutrients? = when {
        quickOnly -> null
        entry?.amount != null -> entry.values.scaled(1.0 / entry.amount)
        food != null -> food.per100.scaled(0.01)
        else -> null
    }
    val shown = when {
        quickOnly -> entry!!.values
        amount != null && perUnit != null -> perUnit.scaled(amount)
        else -> null
    }
    val step = if (ml) 50.0 else 10.0

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
                    Text(food?.let(::foodDetail) ?: if (quickOnly) "Schnelleingabe" else "Gespeicherter Eintrag",
                        fontSize = 12.sp, color = SubtleText)
                }
                onEditCustom?.let { IconButton(onClick = it) { Icon(Icons.Outlined.Edit, "Eigenes Lebensmittel bearbeiten") } }
                if (food != null) {
                    val fav = food.id in log.favorites
                    IconButton(onClick = onFavorite) {
                        Icon(if (fav) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                            if (fav) "Favorit entfernen" else "Als Favorit markieren", tint = scheme.primary)
                    }
                }
            }
            if (!quickOnly) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) },
                        singleLine = true, modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        suffix = { Text(unit, fontSize = 15.sp, color = SubtleText) }, isError = amount == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = FieldBackground,
                            unfocusedContainerColor = FieldBackground, errorContainerColor = FieldBackground,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StepBox("+", "Mehr") { amountText = formatAmount(((amount ?: 0.0) + step).coerceAtMost(MAX_AMOUNT)) }
                        StepBox("−", "Weniger") { amountText = formatAmount(((amount ?: step) - step).coerceAtLeast(step / 2)) }
                    }
                }
                val presets = buildList {
                    add("100 $unit" to 100.0)
                    food?.portions?.forEach { add("${it.label} (${formatAmount(it.grams)} $unit)" to it.grams) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (label, value) ->
                        val active = amount == value
                        FilterChip(selected = active, onClick = { amountText = formatAmount(value) },
                            label = { Text(label, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold) },
                            shape = RoundedCornerShape(17.dp), border = null,
                            colors = FilterChipDefaults.filterChipColors(containerColor = FieldBackground,
                                selectedContainerColor = scheme.primary, selectedLabelColor = scheme.onPrimary))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    (shown?.kcal?.roundToInt()?.toString() ?: "–") to "kcal",
                    (shown?.let { g(it.protein) + " g" } ?: "–") to "Protein",
                    (shown?.let { g(it.fat) + " g" } ?: "–") to "Fett",
                    (shown?.let { g(it.carbs) + " g" } ?: "–") to "KH"
                ).forEach { (v, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(FieldBackground).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(l, fontSize = 11.sp, color = SubtleText)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MEALS.forEachIndexed { i, name ->
                    FilterChip(selected = meal == i, onClick = { meal = i },
                        label = { Text(name, fontWeight = if (meal == i) FontWeight.ExtraBold else FontWeight.SemiBold) },
                        shape = RoundedCornerShape(17.dp), border = null,
                        colors = FilterChipDefaults.filterChipColors(containerColor = FieldBackground,
                            selectedContainerColor = scheme.primaryContainer, selectedLabelColor = scheme.onPrimaryContainer))
                }
            }
            Button(onClick = { onConfirm(if (quickOnly) null else amount, meal) },
                enabled = quickOnly || amount != null,
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
                Text(if (entry == null) "Hinzufügen · ${shown?.kcal?.roundToInt() ?: 0} kcal" else "Speichern",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            onRemove?.let {
                TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                    Text("Eintrag entfernen", color = scheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepBox(text: String, label: String, onClick: () -> Unit) {
    Box(Modifier.size(width = 44.dp, height = 29.dp).clip(RoundedCornerShape(10.dp)).background(FieldBackground)
        .clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------------------------------------------------------------- Dialoge

@Composable
private fun NumberField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier = Modifier,
                        isError: Boolean = false) {
    OutlinedTextField(value, { onValue(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7)) },
        label = { Text(label, fontSize = 12.sp) }, singleLine = true, isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp), modifier = modifier)
}

private fun optionalNumber(text: String): Double? = if (text.isBlank()) 0.0 else parseNumber(text)?.takeIf { it >= 0 }

@Composable
private fun QuickEntryDialog(onDismiss: () -> Unit, onSave: (String, Nutrients) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    val values = listOf(kcal, protein, fat, carbs).map(::optionalNumber)
    val nutrients = if (values.any { it == null }) null else Nutrients(values[0]!!, values[1]!!, values[2]!!, values[3]!!)
    // Ohne kcal-Angabe werden die Kalorien aus den Makros berechnet.
    val filled = nutrients?.let { if (kcal.isBlank()) it.copy(kcal = 4 * it.protein + 9 * it.fat + 4 * it.carbs) else it }
    val valid = filled != null && filled.kcal <= 10_000 && (filled.kcal > 0 || filled.protein > 0)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
        title = { Text("Schnelleingabe", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Bezeichnung (optional)", fontSize = 12.sp) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                NumberField(kcal, { kcal = it }, "kcal (leer = aus Makros)", Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(protein, { protein = it }, "Protein g", Modifier.weight(1f))
                    NumberField(fat, { fat = it }, "Fett g", Modifier.weight(1f))
                    NumberField(carbs, { carbs = it }, "KH g", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(enabled = valid, shape = RoundedCornerShape(24.dp), onClick = { onSave(name, filled!!) }) {
                Text("Hinzufügen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}

@Composable
private fun CustomFoodDialog(existing: Food?, onDismiss: () -> Unit, onSave: (Food) -> Unit, onDelete: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    fun t(v: Double?) = v?.takeIf { it > 0 }?.let(::formatAmount).orEmpty()
    val n = existing?.per100
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var ml by remember { mutableStateOf(existing?.ml ?: false) }
    var kcal by remember { mutableStateOf(t(n?.kcal)) }
    var protein by remember { mutableStateOf(t(n?.protein)) }
    var fat by remember { mutableStateOf(t(n?.fat)) }
    var carbs by remember { mutableStateOf(t(n?.carbs)) }
    var fiber by remember { mutableStateOf(t(n?.fiber)) }
    var sugar by remember { mutableStateOf(t(n?.sugar)) }
    var salt by remember { mutableStateOf(t(n?.salt)) }
    val firstPortion = existing?.portions?.firstOrNull()
    var portionLabel by remember { mutableStateOf(firstPortion?.label.orEmpty()) }
    var portionAmount by remember { mutableStateOf(t(firstPortion?.grams)) }
    var confirmDelete by remember { mutableStateOf(false) }

    val numbers = listOf(kcal, protein, fat, carbs, fiber, sugar, salt).map(::optionalNumber)
    val portion = parseNumber(portionAmount)
    val food = if (numbers.any { it == null }) null else Food(existing?.id ?: newId("c:"), name.trim(),
        Nutrients(numbers[0]!!, numbers[1]!!, numbers[2]!!, numbers[3]!!, numbers[4]!!, numbers[5]!!, numbers[6]!!), ml,
        if (portionLabel.isNotBlank() && portion != null) listOf(FoodPortion(portionLabel.trim().take(30), portion))
        else emptyList(), custom = true)
    val portionOk = portionLabel.isBlank() && portionAmount.isBlank() || portionLabel.isNotBlank() && portion != null && portion > 0
    val valid = food != null && kcal.isNotBlank() && validCustomFood(food) && portionOk
    val unit = if (ml) "ml" else "g"

    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
        title = { Text(if (existing == null) "Eigenes Lebensmittel" else "Lebensmittel bearbeiten", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Name", fontSize = 12.sp) }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Angaben je 100", fontSize = 13.sp, color = SubtleText, modifier = Modifier.weight(1f))
                    listOf(false to "g", true to "ml").forEach { (value, label) ->
                        FilterChip(selected = ml == value, onClick = { ml = value }, label = { Text(label, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(17.dp), border = null,
                            colors = FilterChipDefaults.filterChipColors(containerColor = FieldBackground,
                                selectedContainerColor = scheme.primary, selectedLabelColor = scheme.onPrimary))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(kcal, { kcal = it }, "kcal", Modifier.weight(1f))
                    NumberField(protein, { protein = it }, "Protein g", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(fat, { fat = it }, "Fett g", Modifier.weight(1f))
                    NumberField(carbs, { carbs = it }, "Kohlenh. g", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(fiber, { fiber = it }, "Ballastst.", Modifier.weight(1f))
                    NumberField(sugar, { sugar = it }, "Zucker", Modifier.weight(1f))
                    NumberField(salt, { salt = it }, "Salz", Modifier.weight(1f))
                }
                Text("Portion (optional)", fontSize = 12.sp, color = SubtleText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(portionLabel, { portionLabel = it.take(30) }, label = { Text("z. B. 1 Riegel", fontSize = 12.sp) },
                        singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1.4f))
                    NumberField(portionAmount, { portionAmount = it }, unit, Modifier.weight(1f), isError = !portionOk)
                }
                if (food != null && kcal.isNotBlank() && !validCustomFood(food) && name.isNotBlank()) {
                    Text("Werte prüfen: max. 950 kcal und zusammen max. 100 g Nährstoffe je 100 $unit.",
                        fontSize = 12.sp, color = scheme.error)
                }
                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Lebensmittel löschen", color = scheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = valid, shape = RoundedCornerShape(24.dp), onClick = { onSave(food!!) }) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })

    if (confirmDelete && existing != null) {
        AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = CardBackground,
            title = { Text("„${existing.name}“ löschen?") },
            text = { Text("Bereits eingetragene Mahlzeiten behalten ihre Werte.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(existing.id) }) {
                Text("Löschen", color = scheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } })
    }
}

// ---------------------------------------------------------------- Rezept-Editor

@Composable
private fun RecipeEditor(
    initial: Recipe, log: FoodLog, isNew: Boolean,
    onClose: () -> Unit, onSave: (Recipe) -> Unit, onDelete: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var portions by rememberSaveable { mutableIntStateOf(initial.portions) }
    var ingredients by remember { mutableStateOf(initial.ingredients) }
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf<Food?>(null) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var confirmDelete by remember { mutableStateOf(false) }
    val recipe = Recipe(initial.id, name, portions, ingredients)
    val total = recipeTotal(recipe)
    val perPortion = total.scaled(1.0 / portions)
    val results = if (query.isBlank()) emptyList() else searchFoods(log, query, 8).filterNot { it.recipe }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SubPageHeader(if (isNew) "Neues Rezept" else "Rezept bearbeiten",
                "${ingredients.size} Zutaten · ${kcalText(total.kcal)} kcal gesamt", onClose)
        }
        item {
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Name") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Portionen", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Je Portion: ${kcalText(perPortion.kcal)} kcal · ${g(perPortion.protein)} P · ${g(perPortion.fat)} F · " +
                        "${g(perPortion.carbs)} KH", fontSize = 12.sp, color = SubtleText)
                }
                SmallRound("Weniger Portionen", Icons.Outlined.Remove) { portions = (portions - 1).coerceAtLeast(1) }
                Text("$portions", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                SmallRound("Mehr Portionen", Icons.Outlined.Add) { portions = (portions + 1).coerceAtMost(MAX_PORTIONS) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(vertical = 4.dp)) {
                if (ingredients.isEmpty()) Text("Noch keine Zutaten. Suche unten nach Lebensmitteln.",
                    fontSize = 13.sp, color = SubtleText, modifier = Modifier.padding(16.dp))
                ingredients.forEachIndexed { i, ing ->
                    if (i > 0) HorizontalDivider(color = FieldBackground)
                    Row(Modifier.fillMaxWidth().clickable { editIndex = i }
                        .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(buildAnnotatedString {
                            append(ing.name)
                            withStyle(SpanStyle(color = SubtleText)) { append(" · ${formatAmount(ing.amount)} ${if (ing.ml) "ml" else "g"}") }
                        }, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(ing.values.kcal.roundToInt().toString(), fontSize = 13.sp, color = SubtleText)
                        IconButton(onClick = { ingredients = ingredients.filterIndexed { k, _ -> k != i } }) {
                            Icon(Icons.Outlined.Remove, "${ing.name} entfernen", tint = SubtleText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        item {
            TextField(query, { query = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Zutat suchen") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = SubtleText) },
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground, focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent))
        }
        if (results.isNotEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(vertical = 4.dp)) {
                results.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = FieldBackground)
                    FoodRow(f, tag = if (f.custom) "Eigenes" else null) { adding = f }
                }
            }
        }
        item {
            Button(onClick = { onSave(recipe) }, enabled = validRecipe(recipe),
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
                Text("Rezept speichern", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (!isNew) TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Rezept löschen", color = scheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }

    adding?.let { food ->
        AmountDialog(food.name, food.portions.firstOrNull()?.grams ?: 100.0, if (food.ml) "ml" else "g",
            onDismiss = { adding = null }) { amount ->
            ingredientFor(food, amount)?.let { ingredients = ingredients + it }
            adding = null; query = ""
        }
    }
    ingredients.getOrNull(editIndex)?.let { ing ->
        AmountDialog(ing.name, ing.amount, if (ing.ml) "ml" else "g", onDismiss = { editIndex = -1 }) { amount ->
            ingredients = ingredients.mapIndexed { k, it ->
                if (k == editIndex) it.copy(amount = amount, values = it.values.scaled(amount / it.amount)) else it
            }
            editIndex = -1
        }
    }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = CardBackground,
            title = { Text("Rezept löschen?") },
            text = { Text("Bereits eingetragene Portionen behalten ihre Werte.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(initial.id) }) {
                Text("Löschen", color = scheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } })
    }
}

@Composable
private fun AmountDialog(title: String, initial: Double, unit: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatAmount(initial)) }
    val value = parseNumber(text)?.takeIf { it > 0 && it <= MAX_AMOUNT }
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
        title = { Text(title, fontWeight = FontWeight.ExtraBold, maxLines = 2) },
        text = { NumberField(text, { text = it }, "Menge ($unit)", Modifier.fillMaxWidth(), isError = value == null) },
        confirmButton = {
            Button(enabled = value != null, shape = RoundedCornerShape(24.dp), onClick = { onSave(value!!) }) {
                Text("Übernehmen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}
