package de.gymminimal.app

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
import androidx.compose.ui.graphics.StrokeCap
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
            Text("Analyse", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold)
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

    if (selected != null && selected.isNotEmpty()) {
        val latest = selected.last()
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { detailId = null }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.ChevronLeft, "Zurück")
                }
                Column {
                    Text(latest.logged.name, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(selectedPhase.name, fontSize = 11.sp, color = SubtleText)
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("weight" to "Gewicht", "reps" to "Wdh.", "volume" to "Volumen").forEach { (key, label) ->
                    FilterChip(selected = metric == key, onClick = { metric = key }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            val values = selected.map {
                when (metric) {
                    "reps" -> it.logged.reps.toDouble()
                    "volume" -> it.logged.volume
                    else -> it.logged.weight
                }
            }
            val suffix = if (metric == "reps") " Wdh." else " kg"
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(CardBackground).padding(16.dp)) {
                SectionEyebrow("Leistungsentwicklung")
                Spacer(Modifier.height(18.dp))
                SmallLineChart(values)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Erste: ${formatNumber(values.first())}$suffix", color = SubtleText, fontSize = 11.sp)
                    Text("Letzte: ${formatNumber(values.last())}$suffix", color = SubtleText, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Verlauf", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selected.asReversed()) { p ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(CardBackground).clickable { editingPoint = p }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDate(p.whenLogged), color = SubtleText, fontSize = 12.sp,
                            modifier = Modifier.weight(1f))
                        Text("${formatNumber(p.logged.weight)} kg × ${p.logged.reps} × ${p.logged.sets}",
                            fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Edit, "Trainingswerte korrigieren", modifier = Modifier.size(17.dp),
                            tint = SubtleText)
                    }
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

    val byMuscle = groups.entries.groupBy { it.value.last().logged.muscleGroup }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("Analyse", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                TextButton(onClick = { phaseDropdown = true },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                    Text("${selectedPhase.name} · ${phaseDateLabel(selectedPhase, selectedIsCurrent)}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Outlined.ArrowDropDown, "Phase auswählen", modifier = Modifier.size(19.dp))
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
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatisticCard("Trainingseinheiten", phaseWorkouts.size.toString(), Modifier.weight(1f))
            StatisticCard("Trainingsvolumen", "${formatNumber(totalVolume / 1000)} t", Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        Text("Übungsfortschritt", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        if (groups.isEmpty()) {
            Text("In dieser Phase wurden noch keine Trainingseinheiten abgeschlossen.",
                color = SubtleText, fontSize = 13.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alphabeticTags(MUSCLE_GROUPS + byMuscle.keys).filter { byMuscle[it]?.isNotEmpty() == true }
                    .forEach { muscle ->
                        item(key = "group-$muscle") {
                            Text(muscle, fontSize = 12.sp, color = SubtleText,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        }
                        val entries = byMuscle[muscle].orEmpty().sortedBy { it.value.last().logged.name }
                        items(entries.chunked(2)) { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()) {
                                pair.forEach { entry ->
                                    val e = entry.value.last().logged
                                    Column(Modifier.weight(1f).height(90.dp)
                                        .clip(RoundedCornerShape(10.dp)).background(CardBackground)
                                        .clickable { detailId = entry.key; metric = "weight" }
                                        .padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Text(e.name, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${formatNumber(e.weight)} kg", fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold)
                                        Text("${entry.value.size} Einheiten", fontSize = 10.sp, color = SubtleText)
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
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
                    deleteConfirmation = false
                }) { Text("Endgültig löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun StatisticCard(title: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(11.dp)).background(CardBackground).padding(14.dp)) {
        Text(title, color = SubtleText, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmallLineChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        if (values.isEmpty()) return@Canvas
        val lo = values.minOrNull() ?: 0.0
        val hi = values.maxOrNull() ?: 0.0
        val span = (hi - lo).coerceAtLeast(1.0)
        val side = 12.dp.toPx()
        val heightAvailable = size.height - side * 2
        val xAvailable = size.width - side * 2
        fun location(index: Int): Offset {
            val x = if (values.size == 1) size.width / 2 else
                side + index * xAvailable / (values.size - 1)
            val y = side + ((hi - values[index]) / span * heightAvailable).toFloat()
            return Offset(x, y)
        }
        if (values.size > 1) for (i in 0 until values.size - 1) {
            drawLine(color = color, start = location(i), end = location(i + 1),
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        values.indices.forEach { drawCircle(color = color, radius = 3.5.dp.toPx(), center = location(it)) }
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
