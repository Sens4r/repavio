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
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.graphics.Color
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
data class NewsItem(val icon: String, val title: String, val text: String = "")
/** kind: Neue Funktionen (löst das Pop-up aus) oder Korrekturen/Sicherheit (nur im Versionsverlauf zum Blättern). */
data class ReleaseNews(val version: String, val headline: String, val items: List<NewsItem>, val kind: String = FEATURES)

const val FEATURES = "Neue Funktionen"

val RELEASE_NEWS = listOf(
    ReleaseNews("1.4.4", "Sicherheit", listOf(
        NewsItem("download", "Update-Download in einem geprüften Schritt"),
        NewsItem("lock", "Backup-Passwort stärker abgesichert"),
        NewsItem("seal", "Releases nur nach Freigabe signiert")
    ), kind = "Sicherheit"),
    ReleaseNews("1.4.5", "Neu in repavio", listOf(
        NewsItem("timer", "Stoppuhr im Vollbild", "Mit Pieptönen in einstellbaren Abständen.")
    )),
    ReleaseNews("1.5.1", "Korrekturen", listOf(
        NewsItem("text", "Kacheltext beim Hinzufügen nicht mehr abgeschnitten"),
        NewsItem("scale", "Gewicht auch über den Stift im Profil änderbar")
    ), kind = "Korrekturen"),
    ReleaseNews("1.5.2", "Neu in repavio", listOf(
        NewsItem("food", "Ernährungstagebuch", "Mahlzeiten eintragen – mit Grundliste, eigenen Lebensmitteln und Rezepten."),
        NewsItem("ek", "Eigengewicht & Bänder", "„EK“ antippen für Zusatzgewicht oder Band."),
        NewsItem("trend", "Prognose", "Pfeil und Zahl im Feld zeigen, was als Nächstes drin ist."),
        NewsItem("timer", "Trainingsdauer", "Die Stoppuhrzeit wird mitgespeichert."),
        NewsItem("tabs", "Tabs anpassen", "Unter Profil → Bedienung ausblenden und sortieren.")
    )),
    ReleaseNews("1.5.3", "Neu in repavio", listOf(
        NewsItem("trend", "Bedarf & Trend", "Ab zwei Wochen Daten passt sich dein Kalorienziel wöchentlich an deinen echten Verbrauch an."),
        NewsItem("spoon", "Löffel & Stück", "Mengen auch in TL, EL oder Stück eintragen – im Tagebuch und bei Rezepten."),
        NewsItem("list", "Mehr Lebensmittel", "Über 200 Lebensmittel in der Grundliste, jedes mit Gramm je TL und EL.")
    )),
    ReleaseNews("1.5.4", "Korrekturen", listOf(
        NewsItem("sheet", "Mengen-Fenster öffnet ganz, Button immer erreichbar"),
        NewsItem("spoon", "Einheiten g / TL / EL / Stück nicht mehr abgeschnitten"),
        NewsItem("bars", "Wochenbalken und Farbpunkte in Bedarf & Trend lesbar")
    ), kind = "Korrekturen"),
    ReleaseNews("1.6.0", "Neu in repavio", listOf(
        NewsItem("info", "Übungsinfo", "Namen einer Übung antippen: beanspruchte Muskeln, Ausführung und häufige Fehler."),
        NewsItem("insights", "Alle Phasen", "In der Analyse: Kraftentwicklung über alle Phasen, Phasen im Vergleich und Bestwerte."),
        NewsItem("scale", "Gewichtsabfrage", "Erinnert ans Wiegen, mit Verlauf. Häufigkeit unter Profil → Bedienung."),
        NewsItem("history", "Versionsverlauf", "Hier mit ‹ › durch frühere Versionen blättern.")
    ))
)

private fun sameMinor(a: String, b: String): Boolean =
    a.removePrefix("v").split(".").take(2) == b.removePrefix("v").split(".").take(2)

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
    val features = all.filter { it.kind == FEATURES }
    val relevant = when {
        // Erstes Update mit dieser Funktion: alle Neuigkeiten der aktuellen Hauptversion (z. B. 1.5.x).
        lastSeen == null -> if (updated) features.filter { sameMinor(it.version, current) && compareVersions(it.version, current) <= 0 }
            else emptyList()
        else -> features.filter { compareVersions(it.version, lastSeen) > 0 && compareVersions(it.version, current) <= 0 }
    }
    return relevant.sortedWith { x, y -> compareVersions(y.version, x.version) }
}

private fun newsIcon(key: String): ImageVector = when (key) {
    "food" -> Icons.Outlined.Restaurant
    "ek" -> Icons.Outlined.FitnessCenter
    "trend" -> Icons.Outlined.TrendingUp
    "timer" -> Icons.Outlined.Timer
    "tabs" -> Icons.Outlined.ViewWeek
    "spoon" -> Icons.Outlined.SoupKitchen
    "list" -> Icons.Outlined.FormatListBulleted
    "sheet" -> Icons.Outlined.ViewAgenda
    "bars" -> Icons.Outlined.BarChart
    "text" -> Icons.Outlined.TextFields
    "scale" -> Icons.Outlined.MonitorWeight
    "download" -> Icons.Outlined.Download
    "lock" -> Icons.Outlined.Lock
    "seal" -> Icons.Outlined.VerifiedUser
    "info" -> Icons.Outlined.Info
    "insights" -> Icons.Outlined.Insights
    "history" -> Icons.Outlined.History
    else -> Icons.Outlined.NewReleases
}

/** Pop-up „Was ist neu“: stark abgedunkelt, hellere Dialogfläche, gut lesbarer Fließtext (Vorschau vom 25.11.2026). */
private val NewsDialogBackground = Color(0xFF24272D)
private val NewsTile = Color(0xFF30343B)
private val NewsBody = Color(0xFFD2D5DC)

/** Seite des Versionsverlaufs; Seite 0 sind die neuen Funktionen dieses Updates. */
data class NewsPage(val eyebrow: String, val title: String, val items: List<NewsItem>)

fun newsPages(news: List<ReleaseNews>, current: String, all: List<ReleaseNews> = RELEASE_NEWS): List<NewsPage> {
    val shown = news.map { it.version }.toSet()
    val first = NewsPage("WAS IST NEU · VERSION " + news.first().version.split(".").take(2).joinToString("."),
        news.first().headline, news.flatMap { it.items }.distinctBy { it.title }.take(8))
    val older = all.filter { it.version !in shown && compareVersions(it.version, current) <= 0 }
        .sortedWith { x, y -> compareVersions(y.version, x.version) }
        .map { NewsPage("${it.kind.uppercase()} · VERSION ${it.version}", "Version ${it.version}", it.items) }
    return listOf(first) + older
}

@Composable
fun NewsDialog(news: List<ReleaseNews>, current: String, onClose: (neverAgain: Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var neverAgain by remember { mutableStateOf(false) }
    val pages = remember(news, current) { newsPages(news, current) }
    var page by remember { mutableIntStateOf(0) }
    val shown = pages[page.coerceIn(0, pages.lastIndex)]
    val items = shown.items
    Dialog(onDismissRequest = { onClose(neverAgain) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(max = 700.dp)
            .clip(RoundedCornerShape(28.dp)).background(NewsDialogBackground)) {
            Column(Modifier.fillMaxWidth().background(scheme.primaryContainer).padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.take(6).forEach { item ->
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .12f)),
                            contentAlignment = Alignment.Center) {
                            Icon(newsIcon(item.icon), null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(shown.eyebrow, color = scheme.onPrimaryContainer.copy(alpha = .85f), fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text(shown.title, color = scheme.onPrimaryContainer, fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp, lineHeight = 31.sp)
                    }
                    if (pages.size > 1) {
                        PageButton(Icons.Outlined.ChevronLeft, "Neuere Version", page > 0) { page-- }
                        Spacer(Modifier.width(6.dp))
                        PageButton(Icons.Outlined.ChevronRight, "Ältere Version", page < pages.lastIndex) { page++ }
                    }
                }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(NewsTile),
                            contentAlignment = Alignment.Center) {
                            Icon(newsIcon(item.icon), null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = scheme.onSurface,
                                modifier = Modifier.padding(top = if (item.text.isEmpty()) 10.dp else 0.dp))
                            if (item.text.isNotEmpty()) Text(item.text, fontSize = 15.sp, color = NewsBody, lineHeight = 21.sp,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp).clip(RoundedCornerShape(12.dp))
                .clickable { neverAgain = !neverAgain }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = neverAgain, onCheckedChange = { neverAgain = it })
                Text("Nicht mehr anzeigen", fontSize = 14.sp, color = NewsBody)
            }
            Button(onClick = { onClose(neverAgain) }, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 6.dp)
                .fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
                Text("Los geht's", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        }
    }
}

@Composable
private fun PageButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .12f))
        .clickable(enabled = enabled, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (enabled) 1f else .35f),
            modifier = Modifier.size(20.dp))
    }
}
