package de.gymminimal.app

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Stoppuhr im Vollbild: Zeit, Start/Pause, Zurücksetzen und die Einstellungen in derselben Kachel. */
@Composable
fun StopwatchTile(stopwatch: StopwatchController, config: StopwatchConfig, onConfig: (StopwatchConfig) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var open by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(stopwatch.running) {
        while (stopwatch.running) { now = SystemClock.elapsedRealtime(); delay(200) }
        now = SystemClock.elapsedRealtime()
    }
    val elapsed = stopwatch.elapsed(now)
    val state = when { stopwatch.running -> "Läuft"; elapsed > 0 -> "Pausiert"; else -> "Bereit" }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground)
        .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(formatStopwatch(elapsed), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp, lineHeight = 38.sp, maxLines = 1,
                    color = if (stopwatch.running) scheme.primary else scheme.onSurface,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                Text("$state · ${stopwatchSummary(config)}", fontSize = 12.sp, color = SubtleText, maxLines = 1)
            }
            if (!stopwatch.running && elapsed > 0) {
                RoundIcon(Icons.Outlined.RestartAlt, "Stoppuhr zurücksetzen", FieldBackground, scheme.onSurface, 44) {
                    stopwatch.reset()
                }
            }
            RoundIcon(if (stopwatch.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if (stopwatch.running) "Stoppuhr pausieren" else "Stoppuhr starten",
                scheme.primary, scheme.onPrimary, 56) {
                if (stopwatch.running) stopwatch.pause() else stopwatch.start()
            }
            RoundIcon(Icons.Outlined.Tune, if (open) "Stoppuhr-Einstellungen schließen" else "Stoppuhr-Einstellungen",
                if (open) scheme.primaryContainer else FieldBackground,
                if (open) scheme.onPrimaryContainer else scheme.onSurface, 44) { open = !open }
        }
        if (open) {
            HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(end = 8.dp))
            IntervalRow("Kurzer Piep", "einmal piepen alle …", config.singleEvery, STOPWATCH_SINGLE_MAX) {
                onConfig(config.copy(singleEvery = it))
            }
            IntervalRow("Doppelter Piep", "zweimal piepen alle …", config.doubleEvery, STOPWATCH_DOUBLE_MAX) {
                onConfig(config.copy(doubleEvery = it))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ton", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Pieptöne der Stoppuhr", fontSize = 12.sp, color = SubtleText)
                }
                Switch(checked = config.sound, onCheckedChange = { onConfig(config.copy(sound = it)) },
                    modifier = Modifier.padding(end = 6.dp))
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
                      background: androidx.compose.ui.graphics.Color, tint: androidx.compose.ui.graphics.Color,
                      size: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size / 2).dp))
        .background(background)) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(if (size >= 56) 28.dp else 22.dp))
    }
}

@Composable
private fun IntervalRow(label: String, hint: String, value: Int, max: Int, onValue: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(hint, fontSize = 12.sp, color = SubtleText)
        }
        StepButton("−", "$label verringern", value > 0) { onValue((value - 1).coerceAtLeast(0)) }
        Text(if (value == 0) "aus" else "$value min", modifier = Modifier.width(58.dp), textAlign = TextAlign.Center,
            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        StepButton("+", "$label erhöhen", value < max) { onValue((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(text: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = FieldBackground,
            contentColor = MaterialTheme.colorScheme.onSurface)) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { contentDescription = label })
    }
}
