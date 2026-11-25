package de.gymminimal.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val InfoBody = Color(0xFFD2D5DC)
private val MuscleIdle = Color(0xFF3A3E46)

/** Übungsinfo: beanspruchte Muskeln (vorne/hinten), Ausführung, häufige Fehler. Öffnet über den Namen der Übungskachel. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseInfoScreen(info: ExerciseInfo, history: ExerciseHistory? = null, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val levels = remember(info) { regionLevels(info) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.ChevronLeft, "Zurück", modifier = Modifier.size(26.dp))
            }
            Column {
                Text(info.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp, lineHeight = 27.sp)
                Text(info.equipment, fontSize = 12.sp, color = SubtleText)
            }
        }
        if (history != null && history.points.isNotEmpty()) HistoryCard(history)
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoEyebrow("Beanspruchte Muskeln", Modifier.weight(1f))
                LegendDot(scheme.primary, "Haupt")
                Spacer(Modifier.width(10.dp))
                LegendDot(secondaryMuscle(scheme.primary), "Mit")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                MuscleFigure(front = true, levels = levels)
                Spacer(Modifier.width(28.dp))
                MuscleFigure(front = false, levels = levels)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                info.primary.forEach { Chip(it, true) }
                info.secondary.forEach { Chip(it, false) }
            }
        }
        InfoCard {
            InfoEyebrow("Ausführung")
            info.steps.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center) {
                        Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = scheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(step, fontSize = 15.sp, lineHeight = 21.sp, color = InfoBody, modifier = Modifier.weight(1f))
                }
            }
        }
        InfoCard {
            InfoEyebrow("Häufige Fehler")
            info.errors.forEach { error ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("✕", color = Color(0xFFFF8A80), fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(error, fontSize = 14.5.sp, lineHeight = 20.sp, color = InfoBody, modifier = Modifier.weight(1f))
                }
            }
        }
        Text("Eigene, vereinfachte Darstellung. Beschreibung als Orientierung, kein Ersatz für eine Einweisung.",
            fontSize = 11.sp, color = SubtleText, lineHeight = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
    }
}

private fun secondaryMuscle(primary: Color) = lerp(MuscleIdle, primary, .5f)

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(start = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun InfoEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(Locale.GERMANY), color = SubtleText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = modifier)
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = SubtleText)
    }
}

@Composable
private fun Chip(label: String, main: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Text(label, fontSize = 12.5.sp, fontWeight = if (main) FontWeight.ExtraBold else FontWeight.SemiBold,
        color = if (main) scheme.onPrimary else scheme.onSurface,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (main) scheme.primary else FieldBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp))
}

// ------------------------------------------------------------- Muskelkarte (vereinfachte Figur, 100 × 215)

private sealed class BodyPart(val region: String?) {
    class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float, region: String? = null) : BodyPart(region)
    class Box(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float, region: String? = null) : BodyPart(region)
    class Shape(val d: String, region: String? = null) : BodyPart(region)
}

private val SILHOUETTE = listOf(
    BodyPart.Oval(50f, 15f, 10f, 11f), BodyPart.Box(45f, 25f, 10f, 8f, 3f),
    BodyPart.Shape("M28 36 Q50 30 72 36 L75 64 Q72 92 66 104 L34 104 Q28 92 25 64 Z"),
    BodyPart.Box(15f, 40f, 11f, 62f, 5.5f), BodyPart.Box(74f, 40f, 11f, 62f, 5.5f),
    BodyPart.Box(33f, 102f, 15f, 104f, 7f), BodyPart.Box(52f, 102f, 15f, 104f, 7f)
)

private val FRONT = listOf(
    BodyPart.Oval(29f, 44f, 7.5f, 7f, "Schulter"), BodyPart.Oval(71f, 44f, 7.5f, 7f, "Schulter"),
    BodyPart.Oval(41f, 52f, 9.5f, 7f, "Brust"), BodyPart.Oval(59f, 52f, 9.5f, 7f, "Brust"),
    BodyPart.Oval(20.5f, 62f, 4.5f, 10f, "Bizeps"), BodyPart.Oval(79.5f, 62f, 4.5f, 10f, "Bizeps"),
    BodyPart.Box(42f, 62f, 16f, 34f, 5f, "Bauch"),
    BodyPart.Oval(40.5f, 132f, 7f, 22f, "Oberschenkel vorne"), BodyPart.Oval(59.5f, 132f, 7f, 22f, "Oberschenkel vorne")
)

private val BACK = listOf(
    BodyPart.Shape("M38 34 Q50 28 62 34 L56 48 L44 48 Z", "Nacken"),
    BodyPart.Oval(29f, 44f, 7.5f, 7f, "Schulter"), BodyPart.Oval(71f, 44f, 7.5f, 7f, "Schulter"),
    BodyPart.Oval(39f, 66f, 9f, 15f, "Rücken"), BodyPart.Oval(61f, 66f, 9f, 15f, "Rücken"),
    BodyPart.Box(44f, 80f, 12f, 18f, 4f, "Unterer Rücken"),
    BodyPart.Oval(20.5f, 62f, 4.5f, 10f, "Trizeps"), BodyPart.Oval(79.5f, 62f, 4.5f, 10f, "Trizeps"),
    BodyPart.Oval(41.5f, 110f, 8f, 8f, "Gesäß"), BodyPart.Oval(58.5f, 110f, 8f, 8f, "Gesäß"),
    BodyPart.Oval(40.5f, 140f, 6.5f, 17f, "Oberschenkel hinten"), BodyPart.Oval(59.5f, 140f, 6.5f, 17f, "Oberschenkel hinten"),
    BodyPart.Oval(40.5f, 180f, 5f, 12f, "Waden"), BodyPart.Oval(59.5f, 180f, 5f, 12f, "Waden")
)

@Composable
private fun MuscleFigure(front: Boolean, levels: Map<String, Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = secondaryMuscle(primary)
    val edge = MaterialTheme.colorScheme.background
    val parts = if (front) FRONT else BACK
    val active = parts.mapNotNull { it.region }.distinct().filter { (levels[it] ?: 0) > 0 }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(width = 96.dp, height = 206.dp).semantics {
            contentDescription = (if (front) "Vorderseite" else "Rückseite") +
                if (active.isEmpty()) ", keine Hauptmuskeln" else ": " + active.joinToString()
        }) {
            val k = size.width / 100f
            scale(k, k, pivot = Offset.Zero) {
                SILHOUETTE.forEach { drawPart(it, FieldBackground, edge) }
                parts.forEach { part ->
                    val color = when (levels[part.region] ?: 0) { 2 -> primary; 1 -> secondary; else -> MuscleIdle }
                    drawPart(part, color, edge)
                }
            }
        }
        Text(if (front) "vorne" else "hinten", fontSize = 11.sp, color = SubtleText)
    }
}

private fun DrawScope.drawPart(part: BodyPart, fill: Color, edge: Color) {
    val stroke = Stroke(width = 1.2f)
    when (part) {
        is BodyPart.Oval -> {
            val tl = Offset(part.cx - part.rx, part.cy - part.ry); val s = Size(part.rx * 2, part.ry * 2)
            drawOval(fill, tl, s); drawOval(edge, tl, s, style = stroke)
        }
        is BodyPart.Box -> {
            val tl = Offset(part.x, part.y); val s = Size(part.w, part.h); val r = CornerRadius(part.r, part.r)
            drawRoundRect(fill, tl, s, r); drawRoundRect(edge, tl, s, r, style = stroke)
        }
        is BodyPart.Shape -> {
            val path = PathParser().parsePathString(part.d).toPath()
            drawPath(path, fill); drawPath(path, edge, style = stroke)
        }
    }
}


private fun infoNum(v: Double) = String.format(Locale.GERMANY, if (v % 1.0 == 0.0) "%.0f" else "%.1f", v)

/** „Dein Verlauf“: Bestwert (geschätztes 1RM), Kurve der letzten Einheiten, die letzten drei Einträge. */
@Composable
private fun HistoryCard(history: ExerciseHistory) {
    val primary = MaterialTheme.colorScheme.primary
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoEyebrow("Dein Verlauf", Modifier.weight(1f))
            Text("alle Phasen · 1RM geschätzt", fontSize = 11.sp, color = SubtleText)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                history.best?.let { b ->
                    Text("Bestwert", fontSize = 12.sp, color = SubtleText)
                    Text("${infoNum(Math.round(b.oneRm!!).toDouble())} kg", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp, lineHeight = 32.sp)
                    Text("${loggedValueText(b.entry)} · ${java.time.Instant.ofEpochMilli(b.timestamp).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Locale.GERMANY))}", fontSize = 12.sp, color = InfoBody)
                } ?: Text("Noch ohne Gewichtsangabe", fontSize = 13.sp, color = SubtleText)
            }
            val values = history.points.mapNotNull { it.oneRm }.takeLast(12)
            if (values.size >= 2) Canvas(Modifier.size(width = 150.dp, height = 60.dp)) {
                val lo = values.min(); val hi = values.max()
                val pad = 5.dp.toPx()
                fun x(i: Int) = pad + i * (size.width - pad * 2) / (values.size - 1)
                fun y(v: Double) = pad + ((hi - v) / (hi - lo).coerceAtLeast(.1) * (size.height - pad * 2)).toFloat()
                val path = androidx.compose.ui.graphics.Path().apply {
                    values.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
                }
                drawPath(path, primary, style = Stroke(width = 2.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawCircle(primary, 4.5.dp.toPx(), Offset(x(values.lastIndex), y(values.last())))
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FieldBackground).padding(horizontal = 12.dp)) {
            history.points.takeLast(3).asReversed().forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = CardBackground)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(java.time.Instant.ofEpochMilli(p.timestamp).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("EE, dd.MM.yy", Locale.GERMANY)),
                        fontSize = 13.sp, color = SubtleText, modifier = Modifier.weight(1f))
                    Text(loggedValueText(p.entry), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
