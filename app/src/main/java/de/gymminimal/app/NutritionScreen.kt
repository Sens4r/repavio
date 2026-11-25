package de.gymminimal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private fun thousands(value: Int): String = String.format(Locale.GERMANY, "%,d", value)
private fun factorText(value: Double): String = String.format(Locale.GERMANY, "%.3f", value).trimEnd('0').trimEnd(',')
private fun perKg(value: Double): String = String.format(Locale.GERMANY, "%.1f g/kg", value)

/** Tagesbedarf aus dem Profil (Unterseite des Ernährungstagebuchs). */
@Composable
fun NeedScreen(profile: Profile, log: FoodLog, onGoal: (String) -> Unit, onOpenProfile: () -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val today = java.time.LocalDate.now()
    val target = remember(profile, log) { nutritionTarget(profile, log, today) }
    val result = target?.result
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubPageHeader("Bedarf & Trend", when {
            target == null -> "Ziel für das Tagebuch"
            target.adaptive -> "aus ${target.current.loggedDays} Tagen Gewicht und Ernährung"
            else -> "Nach Formel · Anpassung in etwa ${target.next.daysMissing} Tagen"
        }, onBack)

        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground).padding(4.dp)
            .selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NUTRITION_GOALS.forEach { goal ->
                val active = profile.goal == goal.key
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (active) scheme.primary else Color.Transparent)
                    .selectable(selected = active, onClick = { onGoal(goal.key) }, role = Role.RadioButton),
                    contentAlignment = Alignment.Center) {
                    Text(goal.label, fontSize = 14.sp, fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (active) scheme.onPrimary else scheme.onSurface)
                }
            }
        }

        if (result == null) {
            val missing = missingForNutrition(profile)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Profil ergänzen", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Für die Berechnung fehlen noch: ${missing.joinToString(", ")}.", fontSize = 14.sp, color = SubtleText)
                Button(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp)) { Text("Zum Profil", fontWeight = FontWeight.Bold) }
            }
            Disclaimer()
        } else {

        val onContainer = scheme.onPrimaryContainer
        val t = target!!
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.primaryContainer)
            .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (t.adaptive) "GESCHÄTZTER VERBRAUCH" else "ERHALTUNGSBEDARF (FORMEL)", color = onContainer.copy(alpha = .8f),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(buildAnnotatedString {
                        append(thousands(result.maintenance))
                        withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = onContainer.copy(alpha = .75f))) { append(" kcal") }
                    }, color = onContainer, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp, lineHeight = 40.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Formel: ${thousands(t.current.formulaKcal)}", color = onContainer.copy(alpha = .85f), fontSize = 11.5.sp)
                    Text(if (t.adaptive) "Daten: ${if (t.current.shift >= 0) "+" else "−"}${thousands(kotlin.math.abs(t.current.shift))}"
                        else "Daten: noch ${t.next.daysMissing} Tage", color = onContainer.copy(alpha = .85f), fontSize = 11.5.sp)
                }
            }
            HorizontalDivider(color = onContainer.copy(alpha = .18f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ziel diese Woche · ${result.goal.label}", color = onContainer.copy(alpha = .85f), fontSize = 13.sp,
                    modifier = Modifier.weight(1f))
                Text("${thousands(result.kcal)} kcal", color = onContainer, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (t.next.ready && t.nextKcal != result.kcal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Neues Ziel ab Montag", color = onContainer.copy(alpha = .85f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("${thousands(t.nextKcal)} kcal", color = onContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroTile("Protein", result.proteinG, perKg(PROTEIN_G_PER_KG), scheme.primary, Modifier.weight(1f))
            MacroTile("Fett", result.fatG, perKg(FAT_G_PER_KG), onContainer.copy(alpha = .55f), Modifier.weight(1f))
            MacroTile("Kohlenh.", result.carbsG, "Rest", onContainer.copy(alpha = .25f), Modifier.weight(1f))
        }

        WeightTrendCard(profile, t)
        WeekCard(log, result.kcal, today)

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(vertical = 4.dp)) {
            InfoRow("Grundumsatz", "${thousands(result.bmr)} kcal")
            HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(horizontal = 16.dp))
            InfoRow("Aktivität · ${result.activity.label.lowercase(Locale.GERMANY)}", "× ${factorText(result.activity.factor)}")
            HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(horizontal = 16.dp))
            InfoRow("Erhaltungsbedarf nach Formel", "${thousands(t.current.formulaKcal)} kcal")
            HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpenProfile)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                val weight = String.format(Locale.GERMANY, if (result.weightKg % 1.0 == 0.0) "%.0f" else "%.1f", result.weightKg)
                Text("Basis: $weight kg · ${profile.heightCm} cm · ${result.age} J. · ${profile.sex}",
                    fontSize = 13.sp, color = SubtleText, modifier = Modifier.weight(1f))
                Text("Profil", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                Icon(Icons.Outlined.ChevronRight, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Disclaimer()
        }
    }
}

@Composable
fun SubPageHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-8).dp)) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück")
        }
        Column {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = SubtleText, maxLines = 1)
        }
    }
}

@Composable
private fun MacroTile(label: String, grams: Int, detail: String, dot: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dot))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = SubtleText, maxLines = 1)
        }
        Text(buildAnnotatedString {
            append("$grams")
            withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SubtleText)) { append(" g") }
        }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(detail, fontSize = 11.sp, color = SubtleText, maxLines = 1)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = SubtleText, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Waage-Punkte und geglätteter Trend der letzten 8 Wochen. */
@Composable
private fun WeightTrendCard(profile: Profile, t: NutritionTarget) {
    val scheme = MaterialTheme.colorScheme
    val from = java.time.LocalDate.now().minusDays(55)
    val points = remember(profile.weights) {
        weightTrend(profile.weights.map { dayOfTimestamp(it.timestamp) to it.kg }).filter { !it.day.isBefore(from) }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardBackground).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("GEWICHTSTREND", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SubtleText,
                modifier = Modifier.weight(1f))
            t.next.trendKg?.let { kg ->
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(String.format(Locale.GERMANY, "%.1f kg", kg)) }
                    t.next.kgPerWeek?.let { w ->
                        withStyle(SpanStyle(color = SubtleText)) { append(String.format(Locale.GERMANY, " · %+.1f kg/Woche", w)) }
                    }
                }, fontSize = 13.sp)
            }
        }
        if (points.size < 2) {
            Text("Trage dein Gewicht regelmäßig im Profil ein – am besten morgens. Ab zwei Wochen Daten passt sich dein Bedarf an.",
                fontSize = 13.sp, color = SubtleText, lineHeight = 18.sp)
        } else {
            val lo = points.minOf { minOf(it.scale, it.trend) } - 0.3
            val hi = points.maxOf { maxOf(it.scale, it.trend) } + 0.3
            val first = points.first().day
            val days = java.time.temporal.ChronoUnit.DAYS.between(first, points.last().day).coerceAtLeast(1).toFloat()
            val line = scheme.primary
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                fun x(p: TrendPoint) = java.time.temporal.ChronoUnit.DAYS.between(first, p.day) / days * (size.width - 12f) + 6f
                fun y(v: Double) = ((hi - v) / (hi - lo)).toFloat() * (size.height - 12f) + 6f
                points.forEach { drawCircle(SubtleText.copy(alpha = .55f), radius = 6f, center = androidx.compose.ui.geometry.Offset(x(it), y(it.scale))) }
                val path = androidx.compose.ui.graphics.Path()
                points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p), y(p.trend)) else path.lineTo(x(p), y(p.trend)) }
                drawPath(path, line, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            Text("Punkte = Waage · Linie = geglätteter Trend", fontSize = 11.sp, color = SubtleText)
        }
    }
}

/** Kalorien je Tag dieser Woche gegen das Ziel (gestrichelte Linie). Tage ohne Einträge bleiben leer. */
@Composable
private fun WeekCard(log: FoodLog, goalKcal: Int, today: java.time.LocalDate) {
    val scheme = MaterialTheme.colorScheme
    val monday = weekStart(today)
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val totals = days.map { d -> dayTotals(log, dayKey(d)).takeIf { dayEntries(log, dayKey(d)).isNotEmpty() } }
    val logged = totals.filterNotNull()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardBackground).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("DIESE WOCHE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SubtleText,
                modifier = Modifier.weight(1f))
            if (logged.isNotEmpty()) Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("Ø ${thousands(logged.map { it.kcal }.average().toInt())} kcal") }
                withStyle(SpanStyle(color = SubtleText)) { append(" · Ø ${logged.map { it.protein }.average().toInt()} g Protein") }
            }, fontSize = 13.sp)
        }
        val maxKcal = maxOf(goalKcal.toDouble(), logged.maxOfOrNull { it.kcal } ?: 0.0) * 1.08
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            val goalFraction = (goalKcal / maxKcal).toFloat()
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val yGoal = size.height * (1 - goalFraction)
                drawLine(SubtleText.copy(alpha = .6f), androidx.compose.ui.geometry.Offset(0f, yGoal),
                    androidx.compose.ui.geometry.Offset(size.width, yGoal), strokeWidth = 3f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val v = totals[i]
                        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.BottomCenter) {
                            if (v != null) Box(Modifier.fillMaxWidth().fillMaxHeight((v.kcal / maxKcal).toFloat().coerceIn(.03f, 1f))
                                .clip(RoundedCornerShape(7.dp)).background(if (d == today) scheme.primary else scheme.primaryContainer))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")[i], fontSize = 10.sp,
                            color = if (d == today) scheme.onSurface else SubtleText)
                    }
                }
            }
        }
    }
}

@Composable
private fun Disclaimer() {
    Text("Formel nach Mifflin-St Jeor; ab zwei Wochen mit Gewicht und Tagebuch wird der Bedarf wöchentlich an deinen tatsächlichen Verbrauch angepasst (höchstens ±25 %). Richtwert, kein Ersatz für ärztliche oder ernährungsfachliche Beratung.",
        fontSize = 11.sp, color = SubtleText, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
}
