package de.gymminimal.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private val WeighDialogBackground = Color(0xFF24272D)
private val WeighBody = Color(0xFFD2D5DC)

private fun kgText(v: Double) = String.format(Locale.GERMANY, "%.1f", v)

/** Pop-up „Kurz auf die Waage?“ mit Verlauf der letzten 8 Wochen (Vorschau vom 25.11.2026). */
@Composable
fun WeighDialog(weights: List<WeightEntry>, config: WeighConfig, today: LocalDate,
                onSave: (Double) -> Unit, onLater: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val lastKg = weights.maxByOrNull { it.timestamp }?.kg ?: 75.0
    var input by remember { mutableStateOf(kgText(lastKg)) }
    val value = input.replace(',', '.').toDoubleOrNull()?.takeIf(::validWeight)
    val recent = remember(weights) {
        weights.map { dayOfTimestamp(it.timestamp) to it.kg }.filter { !it.first.isBefore(today.minusDays(56)) }.sortedBy { it.first }
    }
    val trend = remember(recent) { weightTrend(recent) }
    val change = remember(weights) { weeklyChange(weights, today) }
    fun step(delta: Double) { input = kgText(((value ?: lastKg) + delta).coerceIn(25.0, 400.0)) }

    Dialog(onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(WeighDialogBackground).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(weighEyebrow(config, today), color = SubtleText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Kurz auf die Waage?", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp,
                        lineHeight = 30.sp, modifier = Modifier.padding(top = 2.dp))
                    Text("Am besten morgens, vor dem Frühstück.", fontSize = 14.sp, color = WeighBody, modifier = Modifier.padding(top = 4.dp))
                }
                if (recent.size >= 2) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("LETZTE 8 WOCHEN", color = SubtleText, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Text("${kgText(trend.last().trend)} kg", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        change?.let {
                            Text(" · ${if (it >= 0) "+" else "−"}${kgText(abs(it))} kg/Woche", fontSize = 12.sp, color = SubtleText)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val dot = SubtleText.copy(alpha = .6f)
                    Canvas(Modifier.fillMaxWidth().height(84.dp).semantics {
                        contentDescription = "Gewichtsverlauf der letzten 8 Wochen"
                    }) {
                        val start = today.minusDays(56)
                        val all = recent.map { it.second } + listOfNotNull(value)
                        val lo = (all.min()) - .5
                        val hi = (all.max()) + .5
                        val pad = 6.dp.toPx()
                        fun x(d: LocalDate) = pad + ChronoUnit.DAYS.between(start, d).toFloat() / 56f * (size.width - pad * 2)
                        fun y(v: Double) = pad + ((hi - v) / max(hi - lo, .1) * (size.height - pad * 2)).toFloat()
                        recent.forEach { (d, kg) -> drawCircle(dot, 4.dp.toPx(), Offset(x(d), y(kg))) }
                        val line = Path().apply { trend.forEachIndexed { i, p -> if (i == 0) moveTo(x(p.day), y(p.trend)) else lineTo(x(p.day), y(p.trend)) } }
                        drawPath(line, scheme.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        value?.let { drawCircle(scheme.primary, 6.dp.toPx(), Offset(x(today), y(it))) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.weight(1f).heightIn(min = 68.dp).clip(RoundedCornerShape(20.dp)).background(FieldBackground)
                        .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        BasicTextField(input, onValueChange = { input = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5) },
                            singleLine = true, cursorBrush = SolidColor(scheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(color = scheme.onSurface, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp, textAlign = TextAlign.End),
                            modifier = Modifier.weight(1f))
                        Text(" kg", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SubtleText)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StepButton("+", "0,1 kg mehr") { step(.1) }
                        StepButton("−", "0,1 kg weniger") { step(-.1) }
                    }
                }
                val last = weights.maxByOrNull { it.timestamp }
                Text(when {
                    value == null -> "Bitte ein Gewicht zwischen 25 und 400 kg eingeben"
                    last == null -> "Wird als Eintrag für heute gespeichert"
                    else -> {
                        val d = value - last.kg
                        (if (abs(d) < .05) "Gleich wie zuletzt" else "${if (d > 0) "+" else "−"}${kgText(abs(d))} kg zur letzten Wiegung") +
                            " · Eintrag für heute"
                    }
                }, fontSize = 12.5.sp, color = SubtleText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onLater, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text("Heute nicht", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WeighBody)
                    }
                    Button(onClick = { value?.let(onSave) }, enabled = value != null,
                        modifier = Modifier.weight(1.4f).heightIn(min = 52.dp), shape = RoundedCornerShape(26.dp)) {
                        Text("Speichern", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text("Erinnerung: ${weighReminderText(config)} · ändern unter Profil → Bedienung", fontSize = 11.5.sp,
                    color = SubtleText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StepButton(label: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(width = 48.dp, height = 31.dp).clip(RoundedCornerShape(10.dp)).background(FieldBackground)
        .clickable(onClickLabel = description, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

/** Einstellung unter Profil → Bedienung: Häufigkeit und Wochentag. */
@Composable
fun WeighSettingsCard(config: WeighConfig, onChange: (WeighConfig) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("Gewichtsabfrage", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Kurzes Pop-up beim Öffnen der App, mit Verlauf. Aktiv, sobald ein Gewicht im Profil steht.",
                fontSize = 12.sp, color = SubtleText)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FieldBackground).padding(4.dp)) {
            WEIGH_FREQUENCIES.forEach { (key, label) ->
                val on = config.frequency == key
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) CardBackground else Color.Transparent)
                    .clickable(onClickLabel = label) { onChange(config.copy(frequency = key)) }
                    .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = on, onClick = null)
                    Spacer(Modifier.width(10.dp))
                    Text(label, fontSize = 14.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (key == "weekly") Text("empfohlen", fontSize = 12.sp, color = SubtleText)
                }
            }
        }
        if (weighUsesDay(config.frequency)) {
            Text("Wochentag", fontSize = 12.sp, color = SubtleText)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WEEKDAY_SHORT.forEachIndexed { i, l ->
                    val on = config.day == i
                    Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (on) scheme.primary else FieldBackground)
                        .clickable(onClickLabel = WEEKDAY_LONG[i]) { onChange(config.copy(day = i)) },
                        contentAlignment = Alignment.Center) {
                        Text(l, fontSize = 13.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (on) scheme.onPrimary else scheme.onSurface)
                    }
                }
            }
        }
        Text("Wer am Tag schon gewogen hat, wird nicht gefragt. „Heute nicht“ fragt am nächsten Tag erneut.",
            fontSize = 11.5.sp, color = SubtleText, lineHeight = 16.sp)
    }
}

