package de.gymminimal.app

import androidx.compose.foundation.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

@Composable
fun TrainingScreen(app: AppData, update: ((AppData) -> AppData) -> Unit) {
    var selectedDay by remember { mutableIntStateOf((java.time.LocalDate.now().dayOfWeek.value - 1).coerceIn(0, 6)) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseEditorOpen by remember { mutableStateOf(false) }
    var addingAlternative by remember { mutableStateOf(false) }
    var dayEditorOpen by remember { mutableStateOf(false) }
    var phaseEditorOpen by remember { mutableStateOf(false) }
    var newPhaseEditor by remember { mutableStateOf(false) }
    var phaseSummaryOpen by remember { mutableStateOf(false) }
    var finishConfirm by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val plan = app.plan(selectedDay)
    val finishedThisWeek = hasWorkoutThisWeek(app, selectedDay, System.currentTimeMillis())
    val canFinish = plan.enabled && plan.exercises.isNotEmpty() && app.remainingInPhase > 0 && !finishedThisWeek &&
        plan.exercises.any { app.choice(it.id).status !in listOf("skipped", "unavailable") }

    if (!app.phase.isConfigured) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Training", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(CardBackground).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionEyebrow("Aktuelle Phase")
                Text("Noch keine Phase ausgewählt", color = SubtleText, fontSize = 14.sp)
                Button(onClick = { newPhaseEditor = true; phaseEditorOpen = true },
                    modifier = Modifier.fillMaxWidth()) { Text("Phase auswählen") }
            }
            Text("Wähle zuerst eine Trainingsphase und deine Trainingstage aus.",
                color = SubtleText, fontSize = 13.sp)
        }
        if (phaseEditorOpen) {
            PhaseDialog(app, onDismiss = { phaseEditorOpen = false },
                onUpdate = { phase, days ->
                    update { it.copy(phase = phase, plans = withSelectedDays(it.plans, days)) }
                    phaseEditorOpen = false
                },
                onNew = { phase, days ->
                    update { beginNewPhase(it, phase, days) }
                    phaseEditorOpen = false
                }, startWithNew = true)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("Training", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PhaseCard(app.phase, app.completedInPhase,
                onClick = { newPhaseEditor = false; phaseEditorOpen = true },
                onFinish = {
                    update { completePhase(it, System.currentTimeMillis()) }
                    phaseSummaryOpen = true
                },
                onSummary = { phaseSummaryOpen = true })
            Spacer(Modifier.height(18.dp))
            WeekStrip(selectedDay, app.plans, onSelect = { selectedDay = it; feedback = null })
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DAY_NAMES[selectedDay], style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        if (plan.enabled) {
                            IconButton(onClick = { dayEditorOpen = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.Edit, "Muskelgruppen für ${DAY_NAMES[selectedDay]} bearbeiten",
                                    tint = SubtleText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Text(if (!plan.enabled) "Kein Trainingstag" else plan.muscles.ifBlank { "Muskelgruppen auswählen" },
                        color = if (plan.enabled) Color(0xFFCFD1D6) else SubtleText,
                        fontSize = 14.sp)
                }
                if (plan.enabled && plan.exercises.isNotEmpty()) {
                    Text("${plan.exercises.size} Übungen", color = SubtleText, fontSize = 12.sp)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (plan.enabled) {
                items(plan.exercises, key = { it.id }) { exercise ->
                    ExerciseTile(
                        exercise = exercise,
                        choice = app.choice(exercise.id),
                        data = app,
                        onEdit = { editingExercise = exercise; addingAlternative = false; exerciseEditorOpen = true },
                        onAddAlternative = {
                            editingExercise = exercise
                            addingAlternative = true
                            exerciseEditorOpen = true
                        },
                        onChoice = { value ->
                            update { state -> state.copy(choices = state.choices + (exercise.id to value)) }
                        },
                        onValues = { id, values ->
                            update { state -> state.copy(drafts = state.drafts + (id to values)) }
                        }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { editingExercise = null; addingAlternative = false; exerciseEditorOpen = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, Color(0xFF45484D)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SubtleText)
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Übung hinzufügen", fontSize = 13.sp)
                    }
                }
                if (plan.exercises.isEmpty()) {
                    item { Text("Noch keine Übungen eingetragen.",
                        modifier = Modifier.padding(top = 14.dp), fontSize = 12.sp, color = SubtleText) }
                }
            } else {
                item {
                    Text("Dieser Tag ist nicht als Trainingstag ausgewählt. Wähle ihn in der Phasenverwaltung aus.",
                        fontSize = 12.sp, color = SubtleText, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        if (plan.enabled && plan.exercises.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Button(
                    onClick = { finishConfirm = true }, enabled = canFinish,
                    modifier = Modifier.fillMaxWidth().height(51.dp),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text(when {
                        app.remainingInPhase == 0 -> "Phase vollständig"
                        finishedThisWeek -> "Diese Woche bereits geloggt"
                        else -> "Training beenden"
                    }, fontWeight = FontWeight.SemiBold)
                }
                feedback?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }

    if (exerciseEditorOpen) {
        val dayFallback = plan.muscles.split("·", ",").firstOrNull()?.trim().orEmpty()
        val suggestions = EXERCISE_SUGGESTIONS + app.exerciseCatalog.map { it.name } +
            app.plans.flatMap { it.exercises.map { e -> e.name } +
                it.exercises.flatMap { e -> e.alternatives.map { alt -> alt.name } } } +
            app.history.flatMap { it.entries.map { e -> e.name } }
        ExerciseEditor(
            existing = editingExercise,
            defaultGroup = dayFallback,
            suggestions = suggestions,
            muscleOptions = allMuscleGroups(app),
            openNewAlternative = addingAlternative,
            onDismiss = { exerciseEditorOpen = false; addingAlternative = false },
            onSave = { ex ->
                val previouslyKnown = editingExercise?.alternatives.orEmpty().map { it.id }.toSet()
                val newlyAdded = ex.alternatives.firstOrNull { it.id !in previouslyKnown }
                val chooseReplacement = addingAlternative && newlyAdded != null
                update { state ->
                    val saved = saveExerciseInPlan(state, selectedDay, ex)
                    saved.copy(choices = if (chooseReplacement) saved.choices +
                        (ex.id to ExerciseChoice("replaced", newlyAdded!!.id)) else saved.choices)
                }
                exerciseEditorOpen = false
                addingAlternative = false
            },
            onDelete = editingExercise?.let { old ->
                {
                    update { state -> state.copy(plans = state.plans.map { p ->
                        if (p.day == selectedDay) p.copy(exercises = p.exercises.filterNot { it.id == old.id }) else p
                    }, choices = state.choices - old.id,
                        drafts = state.drafts - (listOf(old.id) + old.alternatives.map { it.id }).toSet()) }
                    exerciseEditorOpen = false
                    addingAlternative = false
                }
            }
        )
    }
    if (dayEditorOpen) {
        EditDayDialog(plan, app.plans, app.customTags,
            onDismiss = { dayEditorOpen = false },
            onSave = { muscles, tags, removed ->
                update { state ->
                    val withoutRemoved = removeCustomTags(state, removed)
                    withoutRemoved.copy(
                        plans = withoutRemoved.plans.map {
                            if (it.day == selectedDay) it.copy(muscles = formattedMuscles(chosenMuscles(muscles) - removed)) else it
                        },
                        customTags = alphabeticTags(withoutRemoved.customTags + tags)
                    )
                }
                dayEditorOpen = false
            },
            onMove = { destination ->
                val previous = selectedDay
                update { state -> moveTrainingDay(state, previous, destination) ?: state }
                selectedDay = destination
                dayEditorOpen = false
            },
            onDuplicate = { destination ->
                val source = selectedDay
                update { state -> duplicateTrainingDay(state, source, destination) ?: state }
                selectedDay = destination
                dayEditorOpen = false
            })
    }
    if (phaseEditorOpen) {
        PhaseDialog(app, onDismiss = { phaseEditorOpen = false; newPhaseEditor = false }, onUpdate = { phase, days ->
            update { it.copy(phase = phase, plans = withSelectedDays(it.plans, days)) }
            phaseEditorOpen = false
            newPhaseEditor = false
        }, onNew = { phase, days ->
            update { beginNewPhase(it, phase, days) }
            phaseEditorOpen = false
            newPhaseEditor = false
        }, startWithNew = newPhaseEditor)
    }
    if (phaseSummaryOpen) {
        PhaseSummaryDialog(app.phase, summarizePhase(app, app.phase.id), phaseProgress(app, app.phase.id),
            onDismiss = { phaseSummaryOpen = false },
            onContinue = {
                phaseSummaryOpen = false
                newPhaseEditor = true
                phaseEditorOpen = true
            })
    }
    if (finishConfirm) {
        val performedCount = plan.exercises.count {
            app.choice(it.id).status !in listOf("skipped", "unavailable")
        }
        AlertDialog(
            onDismissRequest = { finishConfirm = false }, containerColor = CardBackground,
            title = { Text("Training abschließen?") },
            text = { Text("$performedCount Übungen werden mit dem heutigen Datum gespeichert. Der Phasenzähler steigt um eine Einheit.") },
            confirmButton = {
                TextButton(onClick = {
                    val result = finishWorkout(app, selectedDay, System.currentTimeMillis())
                    if (result != null) {
                        update { result }
                        feedback = "Training gespeichert."
                    } else {
                        feedback = "Bitte Gewichte und Wiederholungen prüfen."
                    }
                    finishConfirm = false
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { finishConfirm = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun PhaseCard(phase: Phase, completed: Int,
    onClick: () -> Unit, onFinish: () -> Unit, onSummary: () -> Unit
) {
    val target = phase.target.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
        .background(CardBackground).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Column(Modifier.weight(1f)) {
                SectionEyebrow("Aktuelle Phase")
                Spacer(Modifier.height(3.dp))
                Text(phase.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
            val currentWeek = if (completed >= target) phase.weeks else
                (completed / phase.unitsPerWeek.coerceAtLeast(1)) + 1
            Text("$currentWeek / ${phase.weeks}",
                color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.Outlined.ChevronRight, "Phase verwalten", tint = SubtleText,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(6.dp)).background(FieldBackground)) {
            Box(Modifier.fillMaxWidth((completed.toFloat() / target).coerceIn(0f, 1f))
                .fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.height(8.dp))
        Text("$completed von ${phase.target} Trainingseinheiten", color = SubtleText, fontSize = 11.sp)
        if (completed >= target && phase.target > 0) {
            Spacer(Modifier.height(9.dp))
            TextButton(onClick = if (phase.finishedAt == null) onFinish else onSummary,
                contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Outlined.CheckCircleOutline, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (phase.finishedAt == null) "Phase abschließen" else "Zusammenfassung ansehen",
                    fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WeekStrip(selected: Int, plans: List<DayPlan>, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DAY_SHORT.forEachIndexed { index, label ->
            val day = plans[index]
            val available = day.enabled
            val hasExercises = available && day.exercises.isNotEmpty()
            val highlighted = selected == index
            val accent = MaterialTheme.colorScheme.primary
            Column(
                modifier = Modifier.weight(1f).height(54.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(when {
                        highlighted && available -> accent.copy(alpha = .15f)
                        highlighted -> FieldBackground.copy(alpha = .40f)
                        available -> FieldBackground.copy(alpha = .18f)
                        else -> Color.Transparent
                    })
                    .border(1.dp, when {
                        highlighted && available -> accent.copy(alpha = .32f)
                        else -> Color.Transparent
                    }, RoundedCornerShape(9.dp))
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(label,
                    color = when {
                        highlighted && available -> accent
                        available -> Color(0xFFBCC0C7)
                        else -> SubtleText.copy(alpha = .27f)
                    }, fontSize = 12.sp,
                    fontWeight = if (highlighted && available) FontWeight.SemiBold else FontWeight.Normal)
                Spacer(Modifier.height(5.dp))
                Box(Modifier.size(4.dp).clip(RoundedCornerShape(4.dp)).background(
                    if (hasExercises) {
                        if (highlighted) accent else SubtleText.copy(alpha = .75f)
                    } else Color.Transparent
                ))
            }
        }
    }
}

@Composable
private fun ExerciseTile(
    exercise: Exercise, choice: ExerciseChoice, data: AppData,
    onEdit: () -> Unit, onAddAlternative: () -> Unit,
    onChoice: (ExerciseChoice) -> Unit, onValues: (String, Values) -> Unit
) {
    var showAlternatives by remember { mutableStateOf(false) }
    val alternative = exercise.alternatives.firstOrNull { it.id == choice.alternativeId }
    val replaced = choice.status == "replaced" && alternative != null
    val selectedId = if (replaced) alternative!!.id else exercise.id
    val base = if (replaced) Values(alternative!!.weight, alternative.reps)
        else Values(exercise.weight, exercise.reps)
    val current = data.valueFor(selectedId, base)
    val unavailable = choice.status == "unavailable" || choice.status == "skipped"

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
        .background(CardBackground).padding(start = 12.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (replaced) alternative!!.name else exercise.name,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Text(if (replaced) "${alternative!!.sets} Sätze · ${alternative.rest} s Pause"
                    else "${exercise.sets} Sätze · ${exercise.rest} s Pause",
                    fontSize = 10.sp, color = SubtleText)
                if (replaced) Text("Ersatz für ${exercise.name}", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.Edit, "${exercise.name} bearbeiten", tint = SubtleText,
                    modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = { showAlternatives = true }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.SwapHoriz, "${exercise.name} nicht ausführbar / Ersatz wählen",
                    tint = if (unavailable || replaced) MaterialTheme.colorScheme.primary else SubtleText,
                    modifier = Modifier.size(21.dp))
            }
        }
        if (unavailable) {
            Text("Nicht ausgeführt", color = SubtleText, fontSize = 12.sp,
                modifier = Modifier.padding(top = 7.dp, bottom = 3.dp))
        } else {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SwipeNumberField(current.weight, "kg", Modifier.weight(1f), false) { value ->
                    onValues(selectedId, current.copy(weight = value))
                }
                SwipeNumberField(current.reps, "Wdh.", Modifier.weight(1f), true) { value ->
                    onValues(selectedId, current.copy(reps = value))
                }
            }
        }
    }
    if (showAlternatives) {
        AlertDialog(
            onDismissRequest = { showAlternatives = false }, containerColor = CardBackground,
            title = { Text("${exercise.name} nicht ausführbar?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Alternative als Ersatz wählen:", color = SubtleText, fontSize = 12.sp)
                    exercise.alternatives.forEach { alt ->
                        TextButton(onClick = {
                            onChoice(ExerciseChoice("replaced", alt.id))
                            showAlternatives = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(alt.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = { showAlternatives = false; onAddAlternative() },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Alternative hinzufügen")
                    }
                    TextButton(onClick = {
                        onChoice(ExerciseChoice("unavailable"))
                        showAlternatives = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ohne Ersatz nicht ausgeführt", modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onChoice(ExerciseChoice())
                    showAlternatives = false
                }) { Text("Zurücksetzen") }
            },
            dismissButton = { TextButton(onClick = { showAlternatives = false }) { Text("Schließen") } }
        )
    }
}

// Tap = numerical input. Long press + vertical drag = quick wheel-like changes.
@Composable
private fun SwipeNumberField(
    value: String, suffix: String, modifier: Modifier, integer: Boolean,
    onChange: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var editValue by remember(value, editing) { mutableStateOf(value) }
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)
    val stepPx = with(LocalDensity.current) { 11.dp.toPx() }
    val sounds = LocalRepavioSounds.current
    var accumulated by remember { mutableFloatStateOf(0f) }
    var dragNumber by remember { mutableDoubleStateOf(0.0) }
    Row(modifier.height(40.dp).clip(RoundedCornerShape(7.dp)).background(FieldBackground)
        .pointerInput(integer, stepPx) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    accumulated = 0f
                    dragNumber = currentValue.replace(',', '.').toDoubleOrNull() ?: if (integer) 1.0 else 0.0
                },
                onDrag = { change, amount ->
                    change.consume()
                    accumulated -= amount.y
                    if (abs(accumulated) >= stepPx) {
                        val moves = (abs(accumulated) / stepPx).toInt()
                        val direction = if (accumulated > 0f) 1 else -1
                        accumulated -= direction * moves * stepPx
                        dragNumber = (dragNumber + direction * moves * if (integer) 1.0 else 0.5)
                            .coerceAtLeast(if (integer) 1.0 else 0.0)
                        val formatted = if (dragNumber % 1.0 == 0.0) dragNumber.toInt().toString()
                            else String.format(Locale.GERMANY, "%.1f", dragNumber)
                        if (formatted != currentValue) sounds?.tick()
                        currentOnChange(formatted)
                    }
                }
            )
        }
        .clickable { editValue = value; editing = true }
        .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End) {
        AnimatedContent(targetState = value, label = "Zahlenrad", transitionSpec = {
            val goesUp = (targetState.replace(',', '.').toDoubleOrNull() ?: 0.0) >=
                (initialState.replace(',', '.').toDoubleOrNull() ?: 0.0)
            val offset = if (goesUp) 1 else -1
            (slideInVertically { it * offset } + fadeIn()) togetherWith
                (slideOutVertically { -it * offset } + fadeOut())
        }) { displayed ->
            Text(displayed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(6.dp))
        Text(suffix, fontSize = 10.sp, color = SubtleText)
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false }, containerColor = CardBackground,
            title = { Text(if (integer) "Wiederholungen" else "Gewicht (kg)") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = if (integer) it.filter(Char::isDigit)
                        else it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(enabled = if (integer) editValue.toRepsOrNull() != null else editValue.toWeightOrNull() != null,
                    onClick = { currentOnChange(editValue); editing = false }) { Text("Übernehmen") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Abbrechen") } }
        )
    }
}
