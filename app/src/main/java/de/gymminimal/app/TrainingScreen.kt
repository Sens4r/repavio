package de.gymminimal.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

@Composable
fun TrainingScreen(
    app: AppData, update: ((AppData) -> AppData) -> Unit,
    focus: Boolean = false, onFocusChange: (Boolean) -> Unit = {},
    stopwatchTile: (@Composable () -> Unit)? = null, onWorkoutSaved: () -> Unit = {}
) {
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
    // Fullscreen: only the exercises and the finish button. Not persisted; Back leaves it.
    val focusActive = focus && app.phase.isConfigured && plan.enabled && plan.exercises.isNotEmpty()
    LaunchedEffect(focus, focusActive) { if (focus && !focusActive) onFocusChange(false) }
    BackHandler(enabled = focusActive) { onFocusChange(false) }

    if (!app.phase.isConfigured) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(CardBackground).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Noch keine Phase ausgewählt", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Button(onClick = { newPhaseEditor = true; phaseEditorOpen = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
                    Text("Phase auswählen", fontWeight = FontWeight.Bold)
                }
            }
            Text("Wähle zuerst eine Trainingsphase – oder „Keine Phase“ – und deine Trainingstage aus.",
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
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            if (focusActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${DAY_NAMES[selectedDay]} · ${plan.exercises.size} Übungen", color = SubtleText,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    FocusButton(active = true) { onFocusChange(false) }
                }
            } else {
                PhaseCard(app.phase, app.completedInPhase,
                    onClick = { newPhaseEditor = false; phaseEditorOpen = true },
                    onFinish = {
                        update { completePhase(it, System.currentTimeMillis()) }
                        phaseSummaryOpen = true
                    },
                    onSummary = { phaseSummaryOpen = true })
                Spacer(Modifier.height(16.dp))
                WeekStrip(selectedDay, app.plans, onSelect = { selectedDay = it; feedback = null })
                Spacer(Modifier.height(18.dp))
                DayHeader(selectedDay, plan,
                    onEdit = { dayEditorOpen = true },
                    onFocus = { onFocusChange(true) })
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (plan.enabled) {
                itemsIndexed(plan.exercises, key = { _, it -> it.id }) { index, exercise ->
                    ExerciseTile(
                        number = index + 1,
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
                if (!focusActive) item {
                    OutlinedButton(
                        onClick = { editingExercise = null; addingAlternative = false; exerciseEditorOpen = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0xFF45484D)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SubtleText)
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Übung hinzufügen", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (focusActive && stopwatchTile != null) {
                    stopwatchTile()
                    Spacer(Modifier.height(10.dp))
                }
                Button(
                    onClick = { finishConfirm = true }, enabled = canFinish,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(when {
                        !app.phase.openEnded && app.remainingInPhase == 0 -> "Phase vollständig"
                        finishedThisWeek -> "Diese Woche bereits geloggt"
                        else -> "Training beenden"
                    }, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
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
            text = { Text("$performedCount Übungen werden mit dem heutigen Datum gespeichert." +
                if (app.phase.openEnded) "" else " Der Phasenzähler steigt um eine Einheit.") },
            confirmButton = {
                TextButton(onClick = {
                    val result = finishWorkout(app, selectedDay, System.currentTimeMillis())
                    if (result != null) {
                        update { result }
                        onWorkoutSaved()
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
private fun FocusButton(active: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
        .background(FieldBackground)) {
        Icon(if (active) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
            if (active) "Vollbild verlassen" else "Vollbild: nur Übungen anzeigen",
            tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DayHeader(day: Int, plan: DayPlan, onEdit: () -> Unit, onFocus: () -> Unit) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 2.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(DAY_NAMES[day], fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp, lineHeight = 32.sp)
                if (plan.enabled) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Edit, "Muskelgruppen für ${DAY_NAMES[day]} bearbeiten",
                            tint = SubtleText, modifier = Modifier.size(18.dp))
                    }
                }
            }
            val tags = alphabeticTags(chosenMuscles(plan.muscles))
            when {
                !plan.enabled -> Text("Kein Trainingstag", color = SubtleText, fontSize = 14.sp)
                tags.isEmpty() -> Text("Muskelgruppen auswählen", color = SubtleText, fontSize = 14.sp)
                else -> Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(FieldBackground)
                                .padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                }
            }
        }
        if (plan.enabled && plan.exercises.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${plan.exercises.size} Übungen", color = SubtleText, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
                FocusButton(active = false, onClick = onFocus)
            }
        }
    }
}

@Composable
private fun PhaseCard(phase: Phase, completed: Int,
    onClick: () -> Unit, onFinish: () -> Unit, onSummary: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val onContainer = scheme.onPrimaryContainer
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.primaryContainer)
        .clickable(onClick = onClick).padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(phase.name, color = onContainer, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp, lineHeight = 28.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(if (phase.openEnded) "TRAININGS" else "WOCHE", color = onContainer.copy(alpha = .8f),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                if (phase.openEnded) {
                    Text("$completed", color = onContainer, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp)
                } else {
                    val target = phase.target.coerceAtLeast(1)
                    val currentWeek = if (completed >= target) phase.weeks else
                        (completed / phase.unitsPerWeek.coerceAtLeast(1)) + 1
                    Text(buildAnnotatedString {
                        append("$currentWeek")
                        withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = onContainer.copy(alpha = .75f))) { append(" / ${phase.weeks}") }
                    }, color = onContainer, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                }
            }
        }
        if (!phase.openEnded) {
            val target = phase.target.coerceAtLeast(1)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp))
                .background(onContainer.copy(alpha = .14f))) {
                Box(Modifier.fillMaxWidth((completed.toFloat() / target).coerceIn(0f, 1f))
                    .fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(scheme.primary))
            }
            Spacer(Modifier.height(10.dp))
            Text("$completed von ${phase.target} Trainingseinheiten", color = onContainer.copy(alpha = .85f),
                fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (completed >= target && phase.target > 0) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = if (phase.finishedAt == null) onFinish else onSummary,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = onContainer)) {
                    Icon(Icons.Outlined.CheckCircleOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (phase.finishedAt == null) "Phase abschließen" else "Zusammenfassung ansehen",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(selected: Int, plans: List<DayPlan>, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DAY_SHORT.forEachIndexed { index, label ->
            val day = plans[index]
            val available = day.enabled
            val hasExercises = available && day.exercises.isNotEmpty()
            val highlighted = selected == index
            val background = when {
                highlighted && available -> scheme.primary
                highlighted -> FieldBackground
                available -> CardBackground
                else -> Color.Transparent
            }
            val foreground = when {
                highlighted && available -> scheme.onPrimary
                available -> scheme.onSurface
                highlighted -> SubtleText
                else -> SubtleText.copy(alpha = .55f)
            }
            Column(
                modifier = Modifier.weight(1f).height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(background)
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(label, color = foreground, fontSize = 13.sp,
                    fontWeight = if (highlighted && available) FontWeight.ExtraBold else FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(5.dp)).background(
                    if (hasExercises) {
                        if (highlighted) scheme.onPrimary else SubtleText
                    } else Color.Transparent
                ))
            }
        }
    }
}

@Composable
private fun ExerciseTile(
    number: Int, exercise: Exercise, choice: ExerciseChoice, data: AppData,
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

    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(CardBackground).padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                .background(if (unavailable) FieldBackground else scheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Text("$number", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (unavailable) SubtleText else scheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (replaced) alternative!!.name else exercise.name,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Text(if (replaced) "${alternative!!.sets} Sätze · ${alternative.rest} s Pause"
                    else "${exercise.sets} Sätze · ${exercise.rest} s Pause",
                    fontSize = 12.sp, color = SubtleText)
                if (replaced) Text("Ersatz für ${exercise.name}", fontSize = 11.sp,
                    color = scheme.primary)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.Edit, "${exercise.name} bearbeiten", tint = SubtleText,
                    modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = { showAlternatives = true }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.SwapHoriz, "${exercise.name} nicht ausführbar / Ersatz wählen",
                    tint = if (unavailable || replaced) MaterialTheme.colorScheme.primary else SubtleText,
                    modifier = Modifier.size(21.dp))
            }
        }
        if (unavailable) {
            Text("Nicht ausgeführt", color = SubtleText, fontSize = 13.sp,
                modifier = Modifier.padding(start = 52.dp, top = 8.dp))
        } else {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(end = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
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
    // Finger travel per step. 11 dp (up to V1.3.6) was reported as too sensitive.
    val stepPx = with(LocalDensity.current) { 22.dp.toPx() }
    val sounds = LocalRepavioSounds.current
    var accumulated by remember { mutableFloatStateOf(0f) }
    var dragNumber by remember { mutableDoubleStateOf(0.0) }
    Row(modifier.height(52.dp).clip(RoundedCornerShape(16.dp)).background(FieldBackground)
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
        .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        AnimatedContent(targetState = value, label = "Zahlenrad", transitionSpec = {
            val goesUp = (targetState.replace(',', '.').toDoubleOrNull() ?: 0.0) >=
                (initialState.replace(',', '.').toDoubleOrNull() ?: 0.0)
            val offset = if (goesUp) 1 else -1
            (slideInVertically { it * offset } + fadeIn()) togetherWith
                (slideOutVertically { -it * offset } + fadeOut())
        }) { displayed ->
            Text(displayed, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(5.dp))
        Text(suffix, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SubtleText,
            modifier = Modifier.padding(top = 5.dp))
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
