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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

/** EK-Schalter. Calisthenics-Übungen sind automatisch EK, lassen sich aber abschalten. */
@Composable
private fun BodyweightSwitch(checked: Boolean, auto: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Eigenkörpergewicht (EK)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(when {
                !checked -> "Gewicht in kg eintragen"
                auto -> "Automatisch erkannt · Zusatz oder Band in der Kachel"
                else -> "Zusatzgewicht oder Band in der Kachel wählen"
            }, fontSize = 11.sp, color = SubtleText)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Local suggestions, matching beginnings first. */
fun exerciseMatches(name: String, suggestions: List<String>): List<String> =
    if (name.isBlank()) emptyList() else suggestions.distinctBy(::exerciseNameKey)
        .filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }
        .sortedWith(compareBy<String> { !exerciseNameKey(it).startsWith(exerciseNameKey(name)) }
            .thenBy { exerciseNameKey(it) })
        .take(5)

/** Name field with suggestions INLINE, not a focusable popup. The keyboard and the
 *  cursor stay on the TextField while the user keeps typing (fixed in V1.1). */
@Composable
private fun ExerciseNameField(
    value: String, suggestions: List<String>, label: String,
    onValueChange: (String) -> Unit, onPick: (String) -> Unit = onValueChange
) {
    var showSuggestions by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions) { exerciseMatches(value, suggestions) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EditorText(value, { input ->
            showSuggestions = input.isNotBlank()
            onValueChange(input)
        }, label, Modifier.fillMaxWidth())
        if (showSuggestions && matches.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(FieldBackground, RoundedCornerShape(10.dp))) {
                matches.forEachIndexed { index, suggested ->
                    Row(Modifier.fillMaxWidth().clickable {
                        showSuggestions = false
                        onPick(suggested)
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
}

@Composable
fun ExerciseEditor(
    existing: Exercise?, defaultGroup: String, suggestions: List<String>,
    muscleOptions: List<String>,
    openNewAlternative: Boolean = false,
    initialNote: String = "",
    onDismiss: () -> Unit, onSave: (Exercise) -> Unit, onDelete: (() -> Unit)?,
    onNote: (name: String, note: String) -> Unit = { _, _ -> }
) {
    var note by remember(existing?.id) { mutableStateOf(initialNote) }
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
    var bodyweight by remember(existing?.id) { mutableStateOf(existing?.bodyweight) }
    val isEk = bodyweightFor(name, bodyweight)
    var openAlt by remember(existing?.id) { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var showGroups by remember { mutableStateOf(false) }

    LaunchedEffect(openNewAlternative) {
        if (openNewAlternative) {
            val newAlt = Alternative(name = "")
            alts = alts + newAlt
            openAlt = newAlt.id
        }
    }
    fun loadOk(value: String, ek: Boolean) = if (ek) parseBodyLoad(value) != null else value.toWeightOrNull() != null
    val valid = name.isNotBlank() && group.isNotBlank() && group in muscleOptions && loadOk(weight, isEk) &&
        reps.toRepsOrNull() != null && (sets.toIntOrNull() ?: 0) > 0 &&
        (rest.toIntOrNull() ?: -1) >= 0 && alts.all {
            it.name.isNotBlank() && loadOk(it.weight, it.isBodyweight) &&
                it.reps.toRepsOrNull() != null && it.sets > 0 && it.rest >= 0
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = CardBackground,
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (existing == null) "Neue Übung" else "Übung bearbeiten",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                ExerciseNameField(name, suggestions, "Name",
                    onValueChange = { input ->
                        name = input
                        if (existing == null && group.isBlank()) {
                            val guess = inferredMuscleGroup(input, "")
                            if (guess in muscleOptions) group = guess
                        }
                    },
                    onPick = { suggested ->
                        name = suggested
                        if (existing == null) {
                            val guess = inferredMuscleGroup(suggested, "")
                            if (guess in muscleOptions) group = guess
                        }
                    })
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
                BodyweightSwitch(isEk, auto = bodyweight == null && isCalisthenics(name)) { on ->
                    bodyweight = on
                    if (on && parseBodyLoad(weight) == null) weight = "0"
                    if (!on && weight.toWeightOrNull() == null) weight = "0"
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isEk) EditorText(weight, { weight = it }, "Gewicht (kg)", Modifier.weight(1f), true)
                    EditorText(reps, { reps = it.filter(Char::isDigit) }, "Wiederholungen", Modifier.weight(1f), true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorText(sets, { sets = it.filter(Char::isDigit) }, "Sätze", Modifier.weight(1f), true)
                    EditorText(rest, { rest = it.filter(Char::isDigit) }, "Pause (Sek.)", Modifier.weight(1f), true)
                }
                OutlinedTextField(note, onValueChange = { note = it.replace('\n', ' ').take(NOTE_MAX) },
                    label = { Text("Notiz (optional)") }, placeholder = { Text("z. B. Sitz 4, enger Griff") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Text("Erscheint in der Kachel. Gilt für die Übung mit diesem Namen – auch an anderen Tagen und in späteren Phasen.",
                    fontSize = 11.5.sp, color = SubtleText, lineHeight = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Sitz", "Lehne", "Griff", "Stufe").forEach { chip ->
                        Text("+ $chip", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(FieldBackground)
                                .clickable { note = ((if (note.isBlank()) "" else note.trimEnd() + " · ") + "$chip ").take(NOTE_MAX) }
                                .padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
                HorizontalDivider(color = FieldBackground)
                SectionEyebrow("Alternativübungen – nur als Ersatz")
                alts.forEach { alt -> key(alt.id) {
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
                            ExerciseNameField(alt.name, suggestions, "Name",
                                onValueChange = { v -> updateAlt { it.copy(name = v) } })
                            BodyweightSwitch(alt.isBodyweight, auto = alt.bodyweight == null && isCalisthenics(alt.name)) { on ->
                                updateAlt { it.copy(bodyweight = on, weight = if (on && parseBodyLoad(it.weight) == null ||
                                    !on && it.weight.toWeightOrNull() == null) "0" else it.weight) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!alt.isBodyweight) EditorText(alt.weight, { v -> updateAlt { it.copy(weight = v) } },
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
                } }
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
                        IconButton(onClick = { onDelete() }) {
                            Icon(Icons.Outlined.DeleteOutline, "Übung entfernen", tint = SubtleText)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(Modifier.width(6.dp))
                    Button(enabled = valid, shape = RoundedCornerShape(10.dp), onClick = {
                        onNote(name.trim(), note)
                        onSave(Exercise(id = existing?.id ?: makeId(), name = name.trim(),
                            weight = weight, reps = reps, sets = sets.toInt(), rest = rest.toInt(),
                            alternatives = alts, muscleGroup = group, bodyweight = bodyweight))
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
    onUpdate: (Phase, Set<Int>) -> Unit, onNew: (Phase, Set<Int>, List<CarryOver>) -> Unit,
    startWithNew: Boolean = false
) {
    var name by remember { mutableStateOf(if (startWithNew) { if (app.phase.isConfigured && !app.phase.openEnded) "Muskelausdauer" else "" } else app.phase.name) }
    var weeks by remember { mutableStateOf(if (startWithNew) { if (app.phase.isConfigured && !app.phase.openEnded) "6" else "" } else app.phase.weeks.toString()) }
    var newPhase by remember { mutableStateOf(startWithNew) }
    var phaseMenu by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(selectedDays(app.plans)) }
    val parsedWeeks = weeks.toIntOrNull()
    val openEnded = name == NO_PHASE_NAME
    // Übungen der bisherigen Phase mit Startgewichten aus dem geschätzten 1RM übernehmen.
    var carryOn by remember { mutableStateOf(true) }
    var excluded by remember { mutableStateOf(emptySet<String>()) }
    val carryAvailable = newPhase && !openEnded && app.plans.any { it.day in days && it.exercises.isNotEmpty() }
    val suggestions = remember(name, days, app.history, app.plans) {
        if (carryAvailable) carryOverSuggestions(app, name, days) else emptyList()
    }
    // Switching between ILB and "Keine Phase" always starts a new period; editing keeps the kind.
    val typeChoices = when {
        newPhase -> PHASE_TYPES.map { it.first } + NO_PHASE_NAME
        app.phase.openEnded -> listOf(NO_PHASE_NAME)
        else -> PHASE_TYPES.map { it.first }
    }
    val valid = name.isNotBlank() && days.isNotEmpty() &&
        (openEnded || parsedWeeks != null && parsedWeeks in 1..52)
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
                        (typeChoices + listOfNotNull(name.takeUnless {
                            n -> n.isBlank() || n in typeChoices
                        })).distinct().forEach { label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                name = label
                                PHASE_TYPES.firstOrNull { it.first == label }?.let { weeks = it.second.toString() }
                                phaseMenu = false
                            })
                        }
                    }
                }
                if (openEnded) {
                    Text("Training ohne ILB-Phase: kein Wochenziel, kein Phasenabschluss.",
                        fontSize = 12.sp, color = SubtleText)
                } else {
                    EditorText(weeks, { weeks = it.filter(Char::isDigit) }, "Dauer in Wochen", Modifier.fillMaxWidth(), true)
                }
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
                Text(if (openEnded) "${days.size} Einheiten pro Woche"
                    else "${days.size} Einheiten pro Woche · ${((parsedWeeks ?: 0) * days.size)} insgesamt",
                    fontSize = 12.sp, color = SubtleText)
                if (!newPhase) {
                    Text(if (app.phase.openEnded) "${app.completedInPhase} Trainings abgeschlossen"
                        else "${app.completedInPhase} von ${app.phase.target} Einheiten abgeschlossen",
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
                    if (carryAvailable && suggestions.isNotEmpty()) {
                        CarryOverSection(name, suggestions, carryOn, excluded,
                            onToggle = { carryOn = it },
                            onExclude = { id -> excluded = if (id in excluded) excluded - id else excluded + id })
                    }
                    Text(if (carryAvailable && carryOn) "Muskelgruppen und Trainingstage bleiben erhalten. Nicht übernommene Übungen werden entfernt. Alte Trainingsleistungen bleiben gespeichert."
                        else "Die Muskelgruppen und Trainingstage bleiben erhalten. Die Übungskacheln werden geleert. Alte Trainingsleistungen bleiben gespeichert.",
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
                val weekCount = if (openEnded) 0 else parsedWeeks!!
                val phase = if (newPhase)
                    Phase(name = name, weeks = weekCount, unitsPerWeek = days.size, openEnded = openEnded)
                else app.phase.copy(name = name, weeks = weekCount, unitsPerWeek = days.size)
                val carry = if (carryAvailable && carryOn) suggestions.filter { it.exercise.id !in excluded } else emptyList()
                if (newPhase) onNew(phase, days, carry) else onUpdate(phase, days)
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


@Composable
private fun CarryOverSection(phaseName: String, items: List<CarryOver>, on: Boolean, excluded: Set<String>,
                             onToggle: (Boolean) -> Unit, onExclude: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(FieldBackground.copy(alpha = .5f))
        .padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Übungen übernehmen", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(PHASE_REP_RANGE[phaseName]?.let { "Startgewichte aus deinem geschätzten 1RM für $it Wdh." }
                    ?: "Mit den zuletzt trainierten Werten", fontSize = 12.sp, color = SubtleText, lineHeight = 16.sp)
            }
            Switch(checked = on, onCheckedChange = onToggle)
        }
        if (on) {
            items.groupBy { it.day }.forEach { (day, list) ->
                Text(WEEKDAY_LONG[day].uppercase(java.util.Locale.GERMANY), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, color = SubtleText, modifier = Modifier.padding(top = 4.dp))
                list.forEach { c ->
                    val keep = c.exercise.id !in excluded
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onExclude(c.exercise.id) },
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = keep, onCheckedChange = { onExclude(c.exercise.id) })
                        Column(Modifier.weight(1f).alpha(if (keep) 1f else .45f)) {
                            Text(c.exercise.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text(when {
                                c.bodyweight -> "Eigengewicht · Wdh. wie zuletzt"
                                c.lastWeight != null && c.lastReps != null ->
                                    "zuletzt ${carryNum(c.lastWeight)} kg × ${c.lastReps}" + (c.oneRm?.let { " · 1RM ≈ ${carryNum(Math.round(it).toDouble())} kg" } ?: "")
                                else -> "noch nicht trainiert"
                            }, fontSize = 11.5.sp, color = SubtleText)
                        }
                        Text(if (c.bodyweight) "EK × ${c.reps}" else "${carryNum(c.weight.toDoubleOrNull() ?: 0.0)} kg × ${c.reps}",
                            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (keep) scheme.onSurface else SubtleText,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(FieldBackground)
                                .padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }
            Text("Auf 2,5 kg abgerundet (unter 20 kg auf 1 kg). Danach wie gewohnt in der Kachel anpassbar.",
                fontSize = 11.sp, color = SubtleText, lineHeight = 15.sp)
        }
    }
}

private fun carryNum(v: Double): String =
    String.format(java.util.Locale.GERMANY, if (v % 1.0 == 0.0) "%.0f" else "%.1f", v)
