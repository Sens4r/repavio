package de.gymminimal.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.min
import kotlin.math.max
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
private fun EditorText(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, numeric: Boolean = false
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier, singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
fun ExerciseEditor(
    existing: Exercise?, defaultGroup: String, suggestions: List<String>,
    muscleOptions: List<String>,
    openNewAlternative: Boolean = false,
    onDismiss: () -> Unit, onSave: (Exercise) -> Unit, onDelete: (() -> Unit)?
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var weight by remember(existing?.id) { mutableStateOf(existing?.weight ?: "0") }
    var reps by remember(existing?.id) { mutableStateOf(existing?.reps ?: "10") }
    var sets by remember(existing?.id) { mutableStateOf((existing?.sets ?: 3).toString()) }
    var rest by remember(existing?.id) { mutableStateOf((existing?.rest ?: 90).toString()) }
    var group by remember(existing?.id) {
        mutableStateOf(existing?.muscleGroup?.takeIf { it in muscleOptions }
            ?: if (defaultGroup in muscleOptions) defaultGroup else "")
    }
    var alts by remember(existing?.id) { mutableStateOf(existing?.alternatives ?: emptyList()) }
    var openAlt by remember(existing?.id) { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var showGroups by remember { mutableStateOf(false) }

    LaunchedEffect(openNewAlternative) {
        if (openNewAlternative) {
            val newAlt = Alternative(name = "")
            alts = alts + newAlt
            openAlt = newAlt.id
        }
    }
    val matches = remember(name, suggestions) {
        if (name.isBlank()) emptyList() else suggestions.distinctBy(::exerciseNameKey)
            .filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }
            .sortedWith(compareBy<String> { !exerciseNameKey(it).startsWith(exerciseNameKey(name)) }
                .thenBy { exerciseNameKey(it) })
            .take(5)
    }
    val valid = name.isNotBlank() && group.isNotBlank() && group in muscleOptions && weight.toWeightOrNull() != null &&
        reps.toRepsOrNull() != null && (sets.toIntOrNull() ?: 0) > 0 &&
        (rest.toIntOrNull() ?: -1) >= 0 && alts.all {
            it.name.isNotBlank() && it.weight.toWeightOrNull() != null &&
                it.reps.toRepsOrNull() != null && it.sets > 0 && it.rest >= 0
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = CardBackground,
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (existing == null) "Neue Übung" else "Übung bearbeiten",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                // Suggestions are INLINE, not a focusable popup. The keyboard and the
                // cursor stay on the name TextField while the user keeps typing.
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EditorText(name, { input ->
                        name = input
                        showSuggestions = input.isNotBlank()
                        if (existing == null && group.isBlank()) {
                            val guess = inferredMuscleGroup(input, "")
                            if (guess in muscleOptions) group = guess
                        }
                    }, "Name", Modifier.fillMaxWidth())
                    if (showSuggestions && matches.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().background(
                            FieldBackground, RoundedCornerShape(10.dp))) {
                            matches.forEachIndexed { index, suggested ->
                                Row(Modifier.fillMaxWidth().clickable {
                                    name = suggested
                                    showSuggestions = false
                                    if (existing == null) {
                                        val guess = inferredMuscleGroup(suggested, "")
                                        if (guess in muscleOptions) group = guess
                                    }
                                }.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(suggested, fontSize = 13.sp)
                                }
                                if (index != matches.lastIndex) {
                                    HorizontalDivider(color = SubtleText.copy(alpha = .15f))
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Muskelgruppe", fontSize = 12.sp, color = SubtleText, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { showGroups = true }) { Text("${group.ifBlank { "Auswählen" }}  ▾", fontSize = 13.sp) }
                        DropdownMenu(expanded = showGroups, onDismissRequest = { showGroups = false },
                            containerColor = FieldBackground) {
                            val fixed = fixedMuscleGroups()
                            fixed.forEach { g ->
                                DropdownMenuItem(text = { Text(g) }, onClick = { group = g; showGroups = false })
                            }
                            val custom = muscleOptions.filterNot { it in fixed }
                            if (custom.isNotEmpty()) HorizontalDivider(color = SubtleText.copy(alpha = .42f))
                            custom.forEach { g ->
                                DropdownMenuItem(text = { Text(g) }, onClick = { group = g; showGroups = false })
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorText(weight, { weight = it }, "Gewicht (kg)", Modifier.weight(1f), true)
                    EditorText(reps, { reps = it.filter(Char::isDigit) }, "Wiederholungen", Modifier.weight(1f), true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorText(sets, { sets = it.filter(Char::isDigit) }, "Sätze", Modifier.weight(1f), true)
                    EditorText(rest, { rest = it.filter(Char::isDigit) }, "Pause (Sek.)", Modifier.weight(1f), true)
                }
                HorizontalDivider(color = FieldBackground)
                SectionEyebrow("Alternativübungen – nur als Ersatz")
                alts.forEach { alt ->
                    Column(Modifier.fillMaxWidth().background(FieldBackground, RoundedCornerShape(10.dp))
                        .padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(alt.name.ifBlank { "Neue Alternative" }, fontSize = 13.sp,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = { openAlt = if (openAlt == alt.id) null else alt.id }) {
                                Text(if (openAlt == alt.id) "Fertig" else "Bearbeiten", fontSize = 12.sp)
                            }
                        }
                        if (openAlt == alt.id) {
                            val updateAlt: ((Alternative) -> Alternative) -> Unit = { change ->
                                alts = alts.map { if (it.id == alt.id) change(it) else it }
                            }
                            EditorText(alt.name, { v -> updateAlt { it.copy(name = v) } }, "Name", Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EditorText(alt.weight, { v -> updateAlt { it.copy(weight = v) } },
                                    "Gewicht", Modifier.weight(1f), true)
                                EditorText(alt.reps, { v -> updateAlt { it.copy(reps = v.filter(Char::isDigit)) } },
                                    "Wdh.", Modifier.weight(1f), true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EditorText(alt.sets.toString(), { v ->
                                    v.toIntOrNull()?.let { parsed -> updateAlt { it.copy(sets = parsed) } }
                                }, "Sätze", Modifier.weight(1f), true)
                                EditorText(alt.rest.toString(), { v ->
                                    v.toIntOrNull()?.let { parsed -> updateAlt { it.copy(rest = parsed) } }
                                }, "Pause", Modifier.weight(1f), true)
                            }
                            TextButton(onClick = { alts = alts.filterNot { it.id == alt.id }; openAlt = null }) {
                                Text("Alternative entfernen", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    val added = Alternative(name = "")
                    alts = alts + added
                    openAlt = added.id
                }) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Alternative hinzufügen")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = { askDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, "Übung entfernen", tint = SubtleText)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(Modifier.width(6.dp))
                    Button(enabled = valid, shape = RoundedCornerShape(10.dp), onClick = {
                        onSave(Exercise(id = existing?.id ?: makeId(), name = name.trim(),
                            weight = weight, reps = reps, sets = sets.toInt(), rest = rest.toInt(),
                            alternatives = alts, muscleGroup = group))
                    }) { Text("Speichern") }
                }
            }
        }
    }
    if (askDelete && onDelete != null) {
        AlertDialog(onDismissRequest = { askDelete = false }, containerColor = CardBackground,
            title = { Text("Übung entfernen?") },
            text = { Text("Nur aus dem Plan. Alte Trainingsleistungen bleiben gespeichert.") },
            confirmButton = { TextButton(onClick = { askDelete = false; onDelete() }) { Text("Entfernen") } },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Abbrechen") } })
    }
}

@Composable
fun EditDayDialog(
    plan: DayPlan, allPlans: List<DayPlan>, customTags: List<String>,
    onDismiss: () -> Unit, onSave: (String, List<String>, Set<String>) -> Unit,
    onMove: (Int) -> Unit, onDuplicate: (Int) -> Unit
) {
    var selected by remember(plan.day) { mutableStateOf(chosenMuscles(plan.muscles)) }
    var newTags by remember(plan.day) { mutableStateOf(emptyList<String>()) }
    var removedTags by remember(plan.day) { mutableStateOf(emptySet<String>()) }
    var tagText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var moveExpanded by remember { mutableStateOf(false) }
    var duplicateExpanded by remember { mutableStateOf(false) }
    val defaultChoices = fixedMuscleGroups()
    val customChoices = alphabeticTags((customTags + newTags).filterNot { it in removedTags || it in MUSCLE_GROUPS })
    val choices = defaultChoices + customChoices
    val destinations = allPlans.filter {
        it.day != plan.day && !it.enabled && it.exercises.isEmpty() && it.muscles.isBlank()
    }
    // A selected but still-empty day can also receive the duplicated plan.
    val duplicateDestinations = allPlans.filter {
        it.day != plan.day && it.exercises.isEmpty() && it.muscles.isBlank()
    }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBackground,
        title = { Text("${DAY_NAMES[plan.day]} bearbeiten") },
        text = {
            Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Muskelgruppen und Tags", fontSize = 12.sp, color = SubtleText)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(formattedMuscles(selected).ifBlank { "Muskelgruppen wählen" },
                            modifier = Modifier.weight(1f), maxLines = 2)
                        Text(" ▾")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                        containerColor = FieldBackground) {
                        defaultChoices.forEach { muscle ->
                            DropdownMenuItem(
                                text = { Text(muscle, fontSize = 14.sp) },
                                leadingIcon = { Checkbox(checked = muscle in selected, onCheckedChange = null,
                                    modifier = Modifier.size(24.dp)) },
                                onClick = { selected = if (muscle in selected) selected - muscle else selected + muscle }
                            )
                        }
                        if (customChoices.isNotEmpty()) {
                            HorizontalDivider(color = SubtleText.copy(alpha = .42f))
                        }
                        customChoices.forEach { muscle ->
                            DropdownMenuItem(
                                text = { Text(muscle, fontSize = 14.sp) },
                                leadingIcon = { Checkbox(checked = muscle in selected,
                                    onCheckedChange = null, modifier = Modifier.size(24.dp)) },
                                trailingIcon = if (muscle in customTags || muscle in newTags) ({
                                    IconButton(onClick = {
                                        removedTags = removedTags + muscle
                                        newTags = newTags - muscle
                                        selected = selected - muscle
                                    }, modifier = Modifier.size(38.dp)) {
                                        Icon(Icons.Outlined.Close, "Eigenen Tag $muscle entfernen",
                                            tint = SubtleText, modifier = Modifier.size(18.dp))
                                    }
                                }) else null,
                                onClick = { selected = if (muscle in selected) selected - muscle
                                    else selected + muscle }
                            )
                        }
                        HorizontalDivider()
                        Row(Modifier.width(245.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(tagText, onValueChange = { tagText = it },
                                label = { Text("Eigenen Tag hinzufügen", fontSize = 11.sp) },
                                singleLine = true, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val trimmed = tagText.trim()
                                if (trimmed.isNotEmpty() && trimmed.length <= 32) {
                                    removedTags = removedTags.filterNot { it.equals(trimmed, ignoreCase = true) }.toSet()
                                    if (choices.none { it.equals(trimmed, ignoreCase = true) }) newTags = newTags + trimmed
                                    selected = selected + trimmed
                                    tagText = ""
                                }
                            }, enabled = tagText.isNotBlank()) {
                                Icon(Icons.Outlined.Add, "Tag übernehmen")
                            }
                        }
                        DropdownMenuItem(text = { Text("Fertig", color = MaterialTheme.colorScheme.primary) },
                            onClick = { expanded = false })
                    }
                }
                Text("Mehrfachauswahl möglich.", fontSize = 11.sp, color = SubtleText)
                if (destinations.isNotEmpty()) {
                    HorizontalDivider()
                    Box {
                        TextButton(onClick = { moveExpanded = true }) {
                            Text("Gesamten Trainingstag verschieben ▾")
                        }
                        DropdownMenu(expanded = moveExpanded,
                            onDismissRequest = { moveExpanded = false }, containerColor = FieldBackground) {
                            destinations.forEach { target ->
                                DropdownMenuItem(text = { Text(DAY_NAMES[target.day]) },
                                    onClick = { moveExpanded = false; onMove(target.day) })
                            }
                        }
                    }
                    Text("Verschiebt Muskelgruppen und alle Übungen auf einen freien Wochentag.",
                        fontSize = 11.sp, color = SubtleText)
                }
                if (plan.exercises.isNotEmpty() && duplicateDestinations.isNotEmpty()) {
                    HorizontalDivider()
                    Box {
                        TextButton(onClick = { duplicateExpanded = true }) {
                            Text("Trainingsplan duplizieren ▾")
                        }
                        DropdownMenu(expanded = duplicateExpanded,
                            onDismissRequest = { duplicateExpanded = false }, containerColor = FieldBackground) {
                            duplicateDestinations.forEach { target ->
                                DropdownMenuItem(text = { Text(DAY_NAMES[target.day]) },
                                    onClick = { duplicateExpanded = false; onDuplicate(target.day) })
                            }
                        }
                    }
                    Text("Kopiert alle Übungen auf einen leeren Tag. Die Übungen teilen sich " +
                        "ihre Leistungswerte, lassen sich aber getrennt bearbeiten. " +
                        "Ein neuer Trainingstag erhöht die Einheiten pro Woche.",
                        fontSize = 11.sp, color = SubtleText)
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            onSave(formattedMuscles(selected), newTags, removedTags)
        }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

private val PHASE_TYPES = listOf(
    "Muskelausdauer" to 6,
    "Hypertrophie" to 8,
    "Muskelkraft" to 6
)

@Composable
fun PhaseDialog(
    app: AppData, onDismiss: () -> Unit,
    onUpdate: (Phase, Set<Int>) -> Unit, onNew: (Phase, Set<Int>) -> Unit,
    startWithNew: Boolean = false
) {
    var name by remember { mutableStateOf(if (startWithNew) { if (app.phase.isConfigured) "Muskelausdauer" else "" } else app.phase.name) }
    var weeks by remember { mutableStateOf(if (startWithNew) { if (app.phase.isConfigured) "6" else "" } else app.phase.weeks.toString()) }
    var newPhase by remember { mutableStateOf(startWithNew) }
    var phaseMenu by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(selectedDays(app.plans)) }
    val parsedWeeks = weeks.toIntOrNull()
    val valid = name.isNotBlank() && parsedWeeks != null && parsedWeeks in 1..52 && days.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text(if (newPhase) "Neue Trainingsphase" else "Phase verwalten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Phase", fontSize = 12.sp, color = SubtleText)
                Box {
                    OutlinedButton(onClick = { phaseMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(name.ifBlank { "Phase auswählen" }, modifier = Modifier.weight(1f))
                        Text("▾")
                    }
                    DropdownMenu(expanded = phaseMenu, onDismissRequest = { phaseMenu = false },
                        containerColor = FieldBackground) {
                        (PHASE_TYPES.map { it.first } + listOfNotNull(name.takeUnless {
                            n -> PHASE_TYPES.any { it.first == n }
                        })).distinct().forEach { label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                name = label
                                PHASE_TYPES.firstOrNull { it.first == label }?.let { weeks = it.second.toString() }
                                phaseMenu = false
                            })
                        }
                    }
                }
                EditorText(weeks, { weeks = it.filter(Char::isDigit) }, "Dauer in Wochen", Modifier.fillMaxWidth(), true)
                Text("Trainingstage auswählen", fontSize = 12.sp, color = SubtleText)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DAY_SHORT.forEachIndexed { index, day ->
                        val enabled = index in days
                        Box(Modifier.weight(1f).height(44.dp)
                            .background(if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = .22f)
                                else FieldBackground.copy(alpha = .45f), RoundedCornerShape(8.dp))
                            .clickable { days = if (enabled) days - index else days + index },
                            contentAlignment = Alignment.Center) {
                            Text(day, fontSize = 12.sp,
                                color = if (enabled) MaterialTheme.colorScheme.primary else SubtleText)
                        }
                    }
                }
                Text("${days.size} Einheiten pro Woche · ${((parsedWeeks ?: 0) * days.size)} insgesamt",
                    fontSize = 12.sp, color = SubtleText)
                if (!newPhase) {
                    Text("${app.completedInPhase} von ${app.phase.target} Einheiten abgeschlossen",
                        fontSize = 12.sp, color = SubtleText)
                    TextButton(onClick = {
                        newPhase = true
                        name = "Muskelausdauer"
                        weeks = "6"
                        // Existing weekdays are inherited; a blank installation starts with none selected.
                    }) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                        Text("Neue Phase beginnen")
                    }
                } else {
                    Text("Die Muskelgruppen und Trainingstage bleiben erhalten. Die Übungskacheln werden geleert. Alte Trainingsleistungen bleiben gespeichert.",
                        fontSize = 12.sp, color = SubtleText)
                    TextButton(onClick = {
                        newPhase = false
                        name = app.phase.name
                        weeks = app.phase.weeks.toString()
                        days = selectedDays(app.plans)
                    }) { Text("Zurück") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val phase = if (newPhase)
                    Phase(name = name, weeks = parsedWeeks!!, unitsPerWeek = days.size)
                else app.phase.copy(name = name, weeks = parsedWeeks!!, unitsPerWeek = days.size)
                if (newPhase) onNew(phase, days) else onUpdate(phase, days)
            }) { Text(if (newPhase) "Phase starten" else "Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
fun PhaseSummaryDialog(
    phase: Phase, summary: PhaseSummary, progress: PhaseProgress,
    onDismiss: () -> Unit, onContinue: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = CardBackground, shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(.94f).heightIn(max = 700.dp)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("${phase.name} abgeschlossen", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("Deine Gewichtsentwicklung in dieser Phase", fontSize = 13.sp, color = SubtleText)
                if (progress.indices.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().background(FieldBackground, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val percent = String.format(java.util.Locale.GERMANY, "%+.1f", progress.gainPercent)
                        Text("$percent %", style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("Durchschnittliche Veränderung des Trainingsgewichts", fontSize = 11.sp, color = SubtleText)
                        PhaseProgressChart(progress.indices)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Start · 100 %", color = SubtleText, fontSize = 11.sp)
                            Text("Ende · ${String.format(java.util.Locale.GERMANY, "%.1f", 100.0 + progress.gainPercent)} %",
                                color = SubtleText, fontSize = 11.sp)
                        }
                    }
                    Text("Ein gemeinsamer Index aus ${progress.exerciseCount} Übungen. Jede Übung startet bei 100 %. " +
                        "Körpergewichtsübungen mit 0 kg bleiben unberücksichtigt.", color = SubtleText, fontSize = 11.sp)
                } else {
                    Text("Für diese Phase sind keine positiven Gewichtsangaben vorhanden.",
                        color = SubtleText, fontSize = 12.sp)
                }
                Text("${summary.workouts} Einheiten abgeschlossen", fontSize = 13.sp)
                if (summary.firstWorkout != null && summary.lastWorkout != null) {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", java.util.Locale.GERMANY)
                    fun dated(timestamp: Long) = java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter)
                    Text("${dated(summary.firstWorkout)} – ${dated(summary.lastWorkout)}",
                        fontSize = 12.sp, color = SubtleText)
                }
                Text("Die vollständige Trainingshistorie bleibt in der Analyse erhalten.",
                    fontSize = 11.sp, color = SubtleText)
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text("Weiter zur neuen Phase")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Später") }
            }
        }
    }
}

@Composable
private fun PhaseProgressChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(204.dp)) {
        if (values.isEmpty()) return@Canvas
        val low = min(95.0, (values.minOrNull() ?: 100.0) - 3.0)
        val high = max(105.0, (values.maxOrNull() ?: 100.0) + 3.0)
        val span = (high - low).coerceAtLeast(1.0)
        val inset = 12.dp.toPx()
        fun position(i: Int): Offset = Offset(
            if (values.size == 1) size.width / 2 else inset +
                i.toFloat() * (size.width - inset * 2) / (values.size - 1),
            inset + ((high - values[i]) / span * (size.height - inset * 2)).toFloat()
        )
        if (values.size > 1) (0 until values.size - 1).forEach { i ->
            drawLine(color, position(i), position(i + 1), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
        }
        values.indices.forEach { i -> drawCircle(color, radius = 3.0.dp.toPx(), center = position(i)) }
    }
}

@Composable
private fun SummaryValue(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(FieldBackground, RoundedCornerShape(9.dp)).padding(10.dp)) {
        Text(title, fontSize = 10.sp, color = SubtleText)
        Spacer(Modifier.height(5.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
