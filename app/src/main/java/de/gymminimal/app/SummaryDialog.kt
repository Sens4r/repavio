package de.gymminimal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val SummaryBackground = Color(0xFF24272D)
private val SummaryBody = Color(0xFFD2D5DC)

private fun n1(v: Double) = String.format(Locale.GERMANY, if (v % 1.0 == 0.0) "%.0f" else "%.1f", v)

/** Kurze Bilanz nach „Training beenden“ (Vorschau vom 25.11.2026). */
@Composable
fun WorkoutSummaryDialog(s: WorkoutSummary, dayName: String, onClose: (neverAgain: Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var never by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { onClose(never) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(max = 720.dp)
                .clip(RoundedCornerShape(28.dp)).background(SummaryBackground)) {
                Column(Modifier.fillMaxWidth().background(scheme.primaryContainer)
                    .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 18.dp)) {
                    val eyebrow = listOfNotNull(dayName, s.phaseName.ifBlank { null },
                        s.unit?.let { u -> "Einheit $u" + (s.target?.let { " / $it" } ?: "") }).joinToString(" · ")
                    Text(eyebrow.uppercase(Locale.GERMANY), color = scheme.onPrimaryContainer.copy(alpha = .85f), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Training gespeichert", color = scheme.onPrimaryContainer, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp, lineHeight = 31.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                        StatBox("Dauer", s.durationMs?.let(::durationText) ?: "–",
                            s.durationDeltaMs?.let { d -> (if (d >= 0) "+" else "−") + "${(abs(d) / 60_000.0).roundToInt()} min" },
                            false, Modifier.weight(1f))
                        StatBox("Volumen", "${n1(Math.round(s.volume / 100) / 10.0)} t",
                            s.volumeDeltaPercent?.let { String.format(Locale.GERMANY, "%+.0f %%", it) },
                            (s.volumeDeltaPercent ?: 0.0) > 0.5, Modifier.weight(1f))
                        StatBox("Übungen", "${s.performed} / ${s.total}", if (s.performed == s.total) "alle" else null,
                            false, Modifier.weight(1f))
                    }
                    s.best?.let { b ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground)
                            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(scheme.primary),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.EmojiEvents, null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Neuer Bestwert", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${b.name} · ${n1(b.weight)} kg × ${b.reps} · 1RM ≈ ${n1(Math.round(b.oneRm).toDouble())} kg",
                                    fontSize = 12.5.sp, color = SummaryBody)
                            }
                        }
                    }
                    if (s.changes.isNotEmpty()) Column {
                        Text("ZUR LETZTEN EINHEIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SubtleText,
                            modifier = Modifier.padding(bottom = 4.dp))
                        s.changes.forEachIndexed { i, c ->
                            if (i > 0) HorizontalDivider(color = CardBackground)
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(c.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Text(c.now, fontSize = 13.sp, color = SummaryBody)
                                Text(c.delta, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (c.up) scheme.primary else SubtleText,
                                    modifier = Modifier.widthIn(min = 64.dp).padding(start = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 18.dp, top = 8.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { never = !never }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = never, onCheckedChange = { never = it })
                    Text("Nicht mehr anzeigen", fontSize = 14.sp, color = SummaryBody)
                }
                Button(onClick = { onClose(never) }, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp)
                    .fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(26.dp)) {
                    Text("Fertig", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Wieder einschalten unter Profil → Bedienung.", fontSize = 11.5.sp, color = SubtleText,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, delta: String?, highlight: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(CardBackground).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, fontSize = 11.sp, color = SubtleText)
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        if (delta != null) Text(delta, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else SubtleText)
    }
}
