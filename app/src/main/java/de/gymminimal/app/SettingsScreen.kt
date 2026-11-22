package de.gymminimal.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    backup: BackupManager,
    sounds: RepavioSounds,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onChooseFolder: () -> Unit,
    onRestore: (File) -> Unit,
    onReset: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateStatus: String?
) {
    var backups by remember { mutableStateOf(backup.recentFiles()) }
    var showLocal by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var soundEnabled by remember { mutableStateOf(sounds.enabled()) }
    var section by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val appVersion = remember { runCatching { RepavioUpdates.installedVersion(context) }.getOrDefault("?") }
    BackHandler(enabled = section != null) { section = null }

    if (section == null) {
        val tiles = listOf(
            Triple("backup", "Backup", Icons.Outlined.Backup),
            Triple("bedienung", "Bedienung", Icons.Outlined.TouchApp),
            Triple("app", "App & Updates", Icons.Outlined.SystemUpdate),
            Triple("reset", "Zurücksetzen", Icons.Outlined.DeleteForever)
        )
        val subtitles = mapOf(
            "backup" to "${backups.size} lokale Sicherungen",
            "bedienung" to if (soundEnabled) "Wisch-Sound an" else "Wisch-Sound aus",
            "app" to "Version $appVersion",
            "reset" to "Alle Daten löschen"
        )
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (key, title, icon) ->
                        Column(Modifier.weight(1f).height(112.dp).clip(RoundedCornerShape(22.dp))
                            .background(CardBackground).clickable { section = key }.padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(subtitles.getValue(key), fontSize = 12.sp, color = SubtleText, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val titles = mapOf("backup" to "Backup", "bedienung" to "Bedienung", "app" to "App & Updates",
        "reset" to "Zurücksetzen")
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                IconButton(onClick = { section = null }, modifier = Modifier.offset(x = (-8).dp).size(48.dp)) {
                    Icon(Icons.Outlined.ChevronLeft, "Zurück", modifier = Modifier.size(26.dp))
                }
                Text(titles.getValue(section!!), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp, modifier = Modifier.offset(x = (-8).dp))
            }
        }
        when (section) {
            "backup" -> {
                item {
                    SettingRow("Automatische Backups", "Täglich, lokal auf dem Gerät", Icons.Outlined.Backup, null)
                    Spacer(Modifier.height(6.dp))
                    Text("Backups innerhalb der App gehen bei einer Deinstallation verloren. " +
                        "Für eine dauerhafte Sicherung wähle zusätzlich einen Ordner oder exportiere eine Datei.",
                        fontSize = 11.sp, color = SubtleText, modifier = Modifier.padding(horizontal = 4.dp))
                }
                item {
                    SettingRow("Backup-Ordner", if (backup.selectedFolder() == null)
                        "Optional: Ordner für automatische Kopien auswählen" else "Externer Ordner ausgewählt",
                        Icons.Outlined.FolderOpen, onChooseFolder)
                }
                item { SettingRow("Backup exportieren", "Sicherungsdatei speichern", Icons.Outlined.FileUpload, onExport) }
                item { SettingRow("Backup importieren", "Eine Sicherungsdatei einlesen", Icons.Outlined.FileDownload, onImport) }
                item {
                    SettingRow("Lokale Sicherungen", "${backups.size} Sicherungsdateien im App-Speicher",
                        Icons.Outlined.History, { backups = backup.recentFiles(); showLocal = !showLocal })
                }
                if (showLocal) {
                    items(backups) { file ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(FieldBackground).clickable { onRestore(file) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(file.name, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.Restore, "Sicherung wiederherstellen", tint = SubtleText)
                        }
                    }
                }
                item {
                    Button(onClick = {
                        message = if (backup.backupNow()) "Lokale Sicherung erstellt." else "Backup fehlgeschlagen."
                        backups = backup.recentFiles()
                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp),
                        shape = RoundedCornerShape(26.dp)) {
                        Text("Jetzt sichern", fontWeight = FontWeight.Bold)
                    }
                    if (message != null) Text(message!!, fontSize = 12.sp, color = SubtleText,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                }
            }
            "bedienung" -> item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(CardBackground).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VolumeUp, null, modifier = Modifier.size(22.dp), tint = SubtleText)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Wisch-Sound", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Nur bei Gewicht und Wiederholungen", fontSize = 12.sp, color = SubtleText)
                    }
                    Switch(checked = soundEnabled, onCheckedChange = {
                        soundEnabled = it
                        sounds.setEnabled(it)
                    })
                }
            }
            "app" -> {
                item {
                    SettingRow("Nach Updates suchen", "Prüft öffentliche GitHub-Releases", Icons.Outlined.SystemUpdate, onCheckUpdates)
                    if (updateStatus != null) Text(updateStatus, fontSize = 12.sp, color = SubtleText,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                }
                item { SettingRow("repavio V$appVersion", "Offline nutzbar · Material You", Icons.Outlined.Info, null) }
            }
            else -> item {
                SettingRow("App vollständig zurücksetzen",
                    "Trainingsdaten, Pläne, Tags und interne Sicherungen löschen",
                    Icons.Outlined.DeleteForever, onReset)
                Spacer(Modifier.height(6.dp))
                Text("Externe Backups im gewählten Ordner bleiben erhalten. Vor dem Zurücksetzen wird nachgefragt.",
                    fontSize = 11.sp, color = SubtleText, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = SubtleText)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = SubtleText)
        }
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null,
            modifier = Modifier.size(19.dp), tint = SubtleText)
    }
}
