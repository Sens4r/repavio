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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

/** Daily need from the profile. Only a calculation: no food logging. */
@Composable
fun NutritionScreen(profile: Profile, onGoal: (String) -> Unit, onOpenProfile: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val result = nutritionFor(profile)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.primaryContainer)
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp)) {
            Text("TAGESBEDARF", color = onContainer.copy(alpha = .8f), fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(buildAnnotatedString {
                append(thousands(result.kcal))
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = onContainer.copy(alpha = .75f))) { append(" kcal") }
            }, color = onContainer, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.2).sp, lineHeight = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("${result.goal.label} · ${result.goal.detail}", color = onContainer.copy(alpha = .85f),
                fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            val proteinShare = (result.proteinG * 4f / result.kcal).coerceIn(0f, 1f)
            val fatShare = (result.fatG * 9f / result.kcal).coerceIn(0f, 1f - proteinShare)
            Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.weight(proteinShare.coerceAtLeast(.01f)).fillMaxHeight().background(scheme.primary))
                Box(Modifier.weight(fatShare.coerceAtLeast(.01f)).fillMaxHeight().background(onContainer.copy(alpha = .55f)))
                Box(Modifier.weight((1f - proteinShare - fatShare).coerceAtLeast(.01f)).fillMaxHeight()
                    .background(onContainer.copy(alpha = .25f)))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroTile("Protein", result.proteinG, perKg(PROTEIN_G_PER_KG), scheme.primary, Modifier.weight(1f))
            MacroTile("Fett", result.fatG, perKg(FAT_G_PER_KG), onContainer.copy(alpha = .55f), Modifier.weight(1f))
            MacroTile("Kohlenh.", result.carbsG, "Rest", onContainer.copy(alpha = .25f), Modifier.weight(1f))
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(vertical = 4.dp)) {
            InfoRow("Grundumsatz", "${thousands(result.bmr)} kcal")
            HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(horizontal = 16.dp))
            InfoRow("Aktivität · ${result.activity.label.lowercase(Locale.GERMANY)}", "× ${factorText(result.activity.factor)}")
            HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(horizontal = 16.dp))
            InfoRow("Erhaltungsbedarf", "${thousands(result.maintenance)} kcal")
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

@Composable
private fun Disclaimer() {
    Text("Schätzung nach Mifflin-St Jeor. Richtwert, kein Ersatz für ärztliche oder ernährungsfachliche Beratung.",
        fontSize = 11.sp, color = SubtleText, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
}
