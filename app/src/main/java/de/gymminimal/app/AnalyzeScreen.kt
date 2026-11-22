package de.gymminimal.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.outlined.ChevronRight
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Point(val workoutId: String, val whenLogged: Long, val logged: LoggedExercise)

@Composable
fun AnalyzeScreen(app: AppData, update: ((AppData) -> AppData) -> Unit) {
    var detailId by remember { mutableStateOf<String?>(null) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var metric by remember { mutableStateOf("weight") }
    var selectedPhaseId by remember { mutableStateOf(app.phase.id) }
    var phaseDropdown by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var editingPoint by remember { mutableStateOf<Point?>(null) }
    var editingLegacy by remember { mutableStateOf(false) }
    val phases = visiblePhases(app)
    if (phases.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Noch keine Trainingsdaten vorhanden. Beginne im Trainingstab mit einer Phase.",
                color = SubtleText, fontSize = 13.sp)
        }
        return
    }
    val selectedPhase = phases.firstOrNull { it.id == selectedPhaseId } ?: phases.firstOrNull() ?: app.phase
    val selectedIsCurrent = selectedPhase.id == app.phase.id && app.phase.isConfigured

    // Only real completed workouts are measured; gaps in the calendar are not samples.
    val phaseWorkouts = remember(app.history, selectedPhase.id) {
        app.history.filter { it.phaseId == selectedPhase.id }.sortedBy { it.timestamp }
    }
    val points = remember(phaseWorkouts) {
        phaseWorkouts.flatMap { work ->
            work.entries.filter { it.isPerformed }.map { Point(work.id, work.timestamp, it) }
        }
    }
    val totalVolume = points.sumOf { it.logged.volume }
    val groups = points.groupBy { it.logged.performedId }
    val selected = detailId?.let { groups[it] }
    val byMuscle = groups.entries.groupBy { it.value.last().logged.muscleGroup }
    val muscleEntries = selectedMuscle?.let { byMuscle[it] }

    // Android back goes one level up: detail → muscle group → overview.
    BackHandler(enabled = detailId != null || selectedMuscle != null) {
        if (detailId != null) detailId = null else selectedMuscle = null
    }

    if (selected != null && selected.isNotEmpty()) {
        val latest = selected.last()
        val values = selected.map {
            when (metric) {
                "reps" -> it.logged.reps.toDouble()
                "volume" -> it.logged.volume
                else -> it.logged.weight
            }
        }
        val suffix = if (metric == "reps") " Wdh." else " kg"
        val scheme = MaterialTheme.colorScheme
        LazyColumn(Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ScreenHeader(latest.logged.name, "${latest.logged.muscleGroup} · ${selectedPhase.name}") { detailId = null }
            }
            item {
                Row(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("weight" to "Gewicht", "reps" to "Wdh.", "volume" to "Volumen").forEach { (key, label) ->
                        FilterChip(selected = metric == key, onClick = { metric = key },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(18.dp), border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = FieldBackground, labelColor = scheme.onSurface,
                                selectedContainerColor = scheme.primary, selectedLabelColor = scheme.onPrimary))
                    }
                }
            }
            item {
                Column(Modifier.padding(start = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(CardBackground).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Aktuell")
                            Spacer(Modifier.height(4.dp))
                            Text(buildAnnotatedString {
                                append(formatNumber(values.last()))
                                withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                    color = SubtleText)) { append(suffix) }
                            }, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp,
                                lineHeight = 40.sp)
                        }
                        if (values.size > 1) {
                            val delta = values.last() - values.first()
                            val sign = if (delta > 0) "+" else if (delta < 0) "−" else "±"
                            Text("$sign${formatNumber(abs(delta))}$suffix seit Start", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(14.dp))
                                    .background(scheme.primaryContainer).padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                    BarChart(values)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Erste: ${formatNumber(values.first())}$suffix", color = SubtleText, fontSize = 11.sp)
                        Text("Letzte: ${formatNumber(values.last())}$suffix", color = SubtleText, fontSize = 11.sp)
                    }
                }
            }
            item {
                Text("Verlauf", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 2.dp))
            }
            items(selected.asReversed()) { p ->
                Row(Modifier.padding(start = 8.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(CardBackground).clickable { editingPoint = p }
                    .padding(start = 16.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(p.whenLogged), color = SubtleText, fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    Text("${formatNumber(p.logged.weight)} kg × ${p.logged.reps} × ${p.logged.sets}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Outlined.Edit, "Trainingswerte korrigieren", modifier = Modifier.size(17.dp),
                        tint = SubtleText)
                }
            }
        }
        editingPoint?.let { point ->
            EditLoggedPointDialog(point,
                onDismiss = { editingPoint = null },
                onSave = { weight, reps ->
                    update { data -> correctLoggedExercise(data, point.workoutId,
                        point.logged.performedId, weight, reps) ?: data }
                    editingPoint = null
                })
        }
        return
    }

    if (selectedMuscle != null && !muscleEntries.isNullOrEmpty()) {
        val entries = muscleEntries.sortedBy { it.value.last().logged.name }
        Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 16.dp, top = 12.dp)) {
            ScreenHeader(selectedMuscle!!, selectedPhase.name) { selectedMuscle = null }
            Spacer(Modifier.height(14.dp))
            LazyColumn(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)) {
                items(entries.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { entry ->
                            val e = entry.value.last().logged
                            Column(Modifier.weight(1f).height(100.dp)
                                .clip(RoundedCornerShape(20.dp)).background(CardBackground)
                                .clickable { detailId = entry.key; metric = "weight" }
                                .padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(e.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatNumber(e.weight)} kg", fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold)
                                Text("${entry.value.size} Einheiten", fontSize = 11.sp, color = SubtleText)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        return
    }

    val progress = remember(app.history, selectedPhase.id) { phaseProgress(app, selectedPhase.id) }
    val muscles = alphabeticTags(MUSCLE_GROUPS + byMuscle.keys).filter { byMuscle[it]?.isNotEmpty() == true }
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(FieldBackground)
                        .clickable(onClickLabel = "Phase auswählen") { phaseDropdown = true }
                        .padding(start = 16.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedPhase.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(" · ${phaseDateLabel(selectedPhase, selectedIsCurrent)}", fontSize = 13.sp,
                            color = SubtleText, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.Outlined.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = phaseDropdown, onDismissRequest = { phaseDropdown = false },
                        containerColor = CardBackground) {
                        phases.forEach { phase ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(phase.name, fontSize = 13.sp)
                                        Text(phaseDateLabel(phase, phase.id == app.phase.id),
                                            fontSize = 10.sp, color = SubtleText)
                                    }
                                },
                                onClick = {
                                    selectedPhaseId = phase.id
                                    detailId = null
                                    selectedMuscle = null
                                    phaseDropdown = false
                                }
                            )
                        }
                    }
                }
                if (!selectedIsCurrent) {
                    if (selectedPhase.name == "Frühere Phase (ältere Daten)") {
                        IconButton(onClick = { editingLegacy = true }) {
                            Icon(Icons.Outlined.Edit, "Alte Phase benennen", tint = SubtleText)
                        }
                    }
                    IconButton(onClick = { deleteConfirmation = true }) {
                        Icon(Icons.Outlined.DeleteOutline, "Archivierte Phase löschen", tint = SubtleText)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatisticCard("Einheiten", phaseWorkouts.size.toString(), Modifier.weight(1f))
                StatisticCard("Volumen", "${formatNumber(totalVolume / 1000)} t", Modifier.weight(1f))
            }
        }
        item { ProgressCard(progress) }
        if (muscles.isEmpty()) {
            item {
                Text("In dieser Phase wurden noch keine Trainingseinheiten abgeschlossen.",
                    color = SubtleText, fontSize = 13.sp)
            }
        } else {
            item {
                Text("Muskelgruppen", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp, modifier = Modifier.padding(start = 2.dp, top = 8.dp))
            }
            items(muscles.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { muscle ->
                        val count = byMuscle[muscle].orEmpty().size
                        Row(Modifier.weight(1f).height(76.dp)
                            .clip(RoundedCornerShape(20.dp)).background(CardBackground)
                            .clickable { selectedMuscle = muscle }
                            .padding(start = 14.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(muscle, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (count == 1) "1 Übung" else "$count Übungen",
                                    fontSize = 12.sp, color = SubtleText)
                            }
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(FieldBackground),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.ChevronRight, null, tint = SubtleText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (editingLegacy && !selectedIsCurrent) {
        LegacyPhaseDialog(onDismiss = { editingLegacy = false }, onSave = { name, date ->
            update { data -> nameLegacyPhase(data, selectedPhase.id, name, date) ?: data }
            editingLegacy = false
        })
    }
    if (deleteConfirmation && !selectedIsCurrent) {
        AlertDialog(onDismissRequest = { deleteConfirmation = false },
            containerColor = CardBackground,
            title = { Text("Phase endgültig löschen?") },
            text = { Text("${selectedPhase.name} und sämtliche Trainingseinträge dieser Phase werden unwiderruflich vom Gerät gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    update { deletePreviousPhase(it, selectedPhase.id) }
                    selectedPhaseId = app.phase.id
                    detailId = null
                    selectedMuscle = null
                    deleteConfirmation = false
                }) { Text("Endgültig löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Abbrechen") } }
        )
    }
}

/** Overall progress: the same weight index as the phase summary (each exercise starts at 100 %). */
@Composable
private fun ProgressCard(progress: PhaseProgress) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(CardBackground).padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Trainingsfortschritt")
                if (progress.indices.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(String.format(Locale.GERMANY, "%+.1f %%", progress.gainPercent),
                        fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp,
                        lineHeight = 40.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (progress.indices.size >= 2) {
                Text("Start = 100 %", color = SubtleText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (progress.indices.size < 2) {
            Text("Sichtbar ab zwei Trainings mit Gewichtsangabe.", color = SubtleText, fontSize = 12.sp)
        } else {
            IndexChart(progress.indices)
            Text("Ø Gewichtsentwicklung aus ${progress.exerciseCount} Übungen · Übungen mit 0 kg zählen nicht",
                color = SubtleText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.ChevronLeft, "Zurück", modifier = Modifier.size(26.dp))
        }
        Column {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp,
                lineHeight = 27.sp)
            Text(subtitle, fontSize = 12.sp, color = SubtleText)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text.uppercase(Locale.GERMANY), color = SubtleText, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

/** Index line with a dashed 100 % baseline, a light area fill and the latest point highlighted. */
@Composable
private fun IndexChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    val dash = SubtleText.copy(alpha = .35f)
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val lo = min(values.minOrNull() ?: 100.0, 100.0) - 2.0
        val hi = max(values.maxOrNull() ?: 100.0, 100.0) + 2.0
        val pad = 8.dp.toPx()
        fun point(i: Int) = Offset(
            pad + i * (size.width - pad * 2) / (values.size - 1).coerceAtLeast(1),
            pad + ((hi - values[i]) / (hi - lo) * (size.height - pad * 2)).toFloat())
        val baseY = pad + ((hi - 100.0) / (hi - lo) * (size.height - pad * 2)).toFloat()
        drawLine(dash, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())))
        val line = Path().apply { values.indices.forEach { i -> point(i).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } } }
        val area = Path().apply {
            addPath(line)
            lineTo(point(values.lastIndex).x, size.height)
            lineTo(point(0).x, size.height)
            close()
        }
        drawPath(area, color.copy(alpha = .14f))
        drawPath(line, color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = point(values.lastIndex)
        drawCircle(color, radius = 6.dp.toPx(), center = last)
        drawCircle(CardBackground, radius = 2.5.dp.toPx(), center = last)
    }
}

/** One bar per logged workout; the latest bar uses the primary colour. Only real entries, no gaps. */
@Composable
private fun BarChart(values: List<Double>) {
    val scheme = MaterialTheme.colorScheme
    val shown = values.takeLast(30)
    val hi = shown.maxOrNull() ?: 0.0
    val lowest = shown.minOrNull() ?: 0.0
    val lo = (lowest - max(hi - lowest, hi * 0.1)).coerceAtLeast(0.0)
    Row(Modifier.fillMaxWidth().height(130.dp), verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(if (shown.size > 16) 3.dp else 6.dp)) {
        shown.forEachIndexed { index, v ->
            val fraction = if (hi <= lo) 1f else ((v - lo) / (hi - lo)).toFloat().coerceIn(0.06f, 1f)
            Box(Modifier.weight(1f).fillMaxHeight(fraction).clip(RoundedCornerShape(8.dp))
                .background(if (index == shown.lastIndex) scheme.primary else scheme.primaryContainer))
        }
    }
}

@Composable
private fun StatisticCard(title: String, value: String, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = SubtleText, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun formatNumber(value: Double): String =
    String.format(Locale.GERMANY, if (value % 1.0 == 0.0) "%.0f" else "%.1f", value)

private fun formatDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))


private fun phaseDateLabel(phase: Phase, current: Boolean): String {
    if (current) return "Aktuelle Phase"
    if (phase.finishedAt != null) return "Abgeschlossen am ${formatDate(phase.finishedAt)}"
    if (phase.archivedAt != null) return "Archiviert am ${formatDate(phase.archivedAt)}"
    return "Abschlussdatum unbekannt"
}

@Composable
private fun EditLoggedPointDialog(point: Point, onDismiss: () -> Unit,
                                  onSave: (Double, Int) -> Unit) {
    var weight by remember(point) { mutableStateOf(formatNumber(point.logged.weight)) }
    var reps by remember(point) { mutableStateOf(point.logged.reps.toString()) }
    val w = weight.toWeightOrNull()
    val r = reps.toRepsOrNull()
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground,
        title = { Text("Trainingswerte korrigieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${point.logged.name} · ${formatDate(point.whenLogged)}", color = SubtleText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(weight, onValueChange = { weight = it },
                        label = { Text("Gewicht (kg)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(reps, onValueChange = { reps = it.filter(Char::isDigit) },
                        label = { Text("Wdh.") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
                Text("Nur Gewicht und Wiederholungen werden angepasst. Die Trainingsanzahl bleibt gleich.",
                    color = SubtleText, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(enabled = w != null && r != null,
            onClick = { if (w != null && r != null) onSave(w, r) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}

@Composable
private fun LegacyPhaseDialog(onDismiss: () -> Unit, onSave: (String, Long?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    val parsed = if (date.isBlank()) null else runCatching {
        LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground,
        title = { Text("Alte Phase benennen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Diese Angaben wurden in der alten Version nicht gespeichert. Trage sie bei Bedarf selbst ein.",
                    color = SubtleText, fontSize = 12.sp)
                OutlinedTextField(name, onValueChange = { name = it }, label = { Text("Phasenname") })
                OutlinedTextField(date, onValueChange = { date = it },
                    label = { Text("Abschlussdatum (optional)") },
                    placeholder = { Text("TT.MM.JJJJ") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && (date.isBlank() || parsed != null),
            onClick = { onSave(name, parsed) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}
