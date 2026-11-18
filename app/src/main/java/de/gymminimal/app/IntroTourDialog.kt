package de.gymminimal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** A short first-run explanation; no workout/phase is created automatically. */
@Composable
fun IntroTourDialog(onClose: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var showIlb by remember { mutableStateOf(false) }
    val headings = listOf(
        "Willkommen bei repavio",
        "Trainingsphase auswählen",
        "Trainingstage festlegen",
        "Übungen hinzufügen",
        "Gewicht und Wiederholungen",
        "Alles im Überblick"
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBackground, shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(.92f).heightIn(max = 570.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SectionEyebrow("ERSTE SCHRITTE · ${step + 1} / ${headings.size}")
                    TextButton(onClick = { onClose() }) { Text("Überspringen") }
                }
                Text(headings[step], style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    when (step) {
                        0 -> {
                            Text("Dein minimalistisches Trainingstagebuch nach der ILB-Methode. " +
                                "Ohne Konto, ohne Internet und ohne unnötige Funktionen.")
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(FieldBackground)
                                .clickable { showIlb = !showIlb }
                                .padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Was bedeutet ILB?", modifier = Modifier.weight(1f),
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Icon(if (showIlb) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    null, tint = SubtleText)
                            }
                            if (showIlb) Text("Beim Training nach der ILB-Methode strukturierst du dein Training " +
                                "in Phasen wie Muskelausdauer, Hypertrophie und Muskelkraft. " +
                                "Die Übungen und Belastungen kannst du je Phase selbst anpassen.",
                                fontSize = 13.sp, color = SubtleText)
                        }
                        1 -> Text("Tippe im Trainingstab auf „Phase auswählen“. Wähle " +
                            "Muskelausdauer, Hypertrophie oder Muskelkraft und lege fest, " +
                            "wie viele Trainingswochen die Phase dauern soll.")
                        2 -> Text("Wähle im selben Fenster die Wochentage aus, an denen du trainieren willst. " +
                            "Nur mit „Training beenden“ abgeschlossene Einheiten zählen zum Fortschritt. " +
                            "Ausgefallene Tage bleiben einfach leer.")
                        3 -> Text("Öffne einen ausgewählten Trainingstag und tippe auf " +
                            "„Übung hinzufügen“. Trage Name, Gewicht, Wiederholungen, Sätze und Pause ein. " +
                            "Mit dem Stift am Tag kannst du Muskelgruppen zuordnen.")
                        4 -> Text("Einmal auf ein Gewichts- oder Wiederholungsfeld tippen, um " +
                            "eine Zahl direkt einzugeben. Oder länger gedrückt halten " +
                            "und nach oben bzw. unten wischen, um den Wert schnell zu ändern.")
                        5 -> Text("Im Tab „Analyse“ siehst du deine gespeicherten Trainingsfortschritte. " +
                            "Unter „Einstellungen“ findest du Datensicherung, den Wisch-Sound und das " +
                            "Zurücksetzen der App. Du kannst jetzt deine erste Phase anlegen.")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (step > 0) {
                        TextButton(onClick = { step-- }) { Text("Zurück") }
                    } else Spacer(Modifier.width(72.dp))
                    Button(onClick = {
                        if (step == headings.lastIndex) onClose() else step++
                    }, shape = RoundedCornerShape(11.dp)) {
                        Text(if (step == headings.lastIndex) "Los geht’s" else "Weiter")
                    }
                }
            }
        }
    }
}
