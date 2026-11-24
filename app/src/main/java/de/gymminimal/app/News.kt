package de.gymminimal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Neuigkeiten nach einem Update. Nur Versionen mit neuen Funktionen bekommen einen Eintrag;
 * Bugfix- und Performance-Versionen nicht, dann erscheint kein Pop-up. Kurz halten: höchstens 5 Punkte.
 */
data class NewsItem(val icon: String, val title: String, val text: String)
data class ReleaseNews(val version: String, val headline: String, val items: List<NewsItem>)

val RELEASE_NEWS = listOf(
    ReleaseNews("1.5.2", "Neu in repavio 1.5", listOf(
        NewsItem("food", "Ernährungstagebuch", "Mahlzeiten eintragen – mit Grundliste, eigenen Lebensmitteln und Rezepten. Offline und verschlüsselt."),
        NewsItem("ek", "Eigengewicht & Bänder", "Calisthenics zählen mit Körpergewicht. „EK“ antippen für Zusatzgewicht oder Band."),
        NewsItem("trend", "Prognose", "Pfeil und Zahl im Feld zeigen, was beim nächsten Training drin ist."),
        NewsItem("timer", "Trainingsdauer", "Die Zeit der Stoppuhr wird mit dem Training gespeichert."),
        NewsItem("tabs", "Tabs anpassen", "Unter Profil → Bedienung ausblenden und umsortieren.")
    ))
)

fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
    val pa = parts(a); val pb = parts(b)
    for (i in 0..2) if (pa[i] != pb[i]) return pa[i].compareTo(pb[i])
    return 0
}

/**
 * Welche Neuigkeiten jetzt zu zeigen sind.
 * lastSeen = zuletzt bestätigte Version (null = Funktion zum ersten Mal aktiv), updated = App wurde schon einmal aktualisiert.
 * Neuinstallation: nichts. Erstes Mal nach Update ohne gespeicherten Stand: nur die aktuelle Version.
 */
fun newsToShow(current: String, lastSeen: String?, updated: Boolean, disabled: Boolean,
               all: List<ReleaseNews> = RELEASE_NEWS): List<ReleaseNews> {
    if (disabled) return emptyList()
    val relevant = when {
        lastSeen == null -> if (updated) all.filter { compareVersions(it.version, current) == 0 } else emptyList()
        else -> all.filter { compareVersions(it.version, lastSeen) > 0 && compareVersions(it.version, current) <= 0 }
    }
    return relevant.sortedWith { x, y -> compareVersions(y.version, x.version) }
}

private fun newsIcon(key: String): ImageVector = when (key) {
    "food" -> Icons.Outlined.Restaurant
    "ek" -> Icons.Outlined.FitnessCenter
    "trend" -> Icons.Outlined.TrendingUp
    "timer" -> Icons.Outlined.Timer
    "tabs" -> Icons.Outlined.ViewWeek
    else -> Icons.Outlined.NewReleases
}

/** Pop-up „Was ist neu“: Kopfbereich in primaryContainer, darunter Punkte mit Symbol. */
@Composable
fun NewsDialog(news: List<ReleaseNews>, onClose: (neverAgain: Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var neverAgain by remember { mutableStateOf(false) }
    val items = news.flatMap { it.items }.distinctBy { it.title }.take(6)
    Dialog(onDismissRequest = { onClose(neverAgain) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(max = 640.dp)
            .clip(RoundedCornerShape(28.dp)).background(CardBackground)) {
            Column(Modifier.fillMaxWidth().background(scheme.primaryContainer).padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.take(5).forEach { item ->
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(scheme.onPrimaryContainer.copy(alpha = .14f)),
                            contentAlignment = Alignment.Center) {
                            Icon(newsIcon(item.icon), null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(17.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("WAS IST NEU", color = scheme.onPrimaryContainer.copy(alpha = .8f), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(news.first().headline, color = scheme.onPrimaryContainer, fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, lineHeight = 30.sp)
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(FieldBackground),
                            contentAlignment = Alignment.Center) {
                            Icon(newsIcon(item.icon), null, tint = scheme.primary, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text(item.text, fontSize = 13.sp, color = SubtleText, lineHeight = 17.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 18.dp, top = 10.dp).clip(RoundedCornerShape(12.dp))
                .clickable { neverAgain = !neverAgain }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = neverAgain, onCheckedChange = { neverAgain = it })
                Text("Nicht mehr anzeigen", fontSize = 13.sp, color = SubtleText)
            }
            Button(onClick = { onClose(neverAgain) }, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp, top = 4.dp)
                .fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
                Text("Los geht's", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
