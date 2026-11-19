package de.gymminimal.app

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
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
        contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Einstellungen", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            SectionEyebrow("Datensicherung")
        }
        item {
            SettingRow("Automatische Backups", "Täglich, lokal auf dem Gerät", Icons.Outlined.Backup, null)
            Spacer(Modifier.height(6.dp))
            Text("Backups innerhalb der App gehen bei einer Deinstallation verloren. " +
                "Für eine dauerhafte Sicherung wähle zusätzlich einen Ordner oder exportiere eine Datei.",
                fontSize = 11.sp, color = SubtleText)
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
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                    .background(FieldBackground).clickable { onRestore(file) }.padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(file.name, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Restore, "Sicherung wiederherstellen", tint = SubtleText)
                }
            }
        }
        item {
            OutlinedButton(onClick = {
                message = if (backup.backupNow()) "Lokale Sicherung erstellt." else "Backup fehlgeschlagen."
                backups = backup.recentFiles()
            }, modifier = Modifier.fillMaxWidth()) { Text("Jetzt sichern") }
            if (message != null) Text(message!!, fontSize = 12.sp, color = SubtleText)
            Spacer(Modifier.height(12.dp))
            SectionEyebrow("Daten & Zurücksetzen")
        }
        item { SettingRow("App vollständig zurücksetzen",
            "Trainingsdaten, Pläne, Tags und interne Sicherungen löschen",
            Icons.Outlined.DeleteForever, onReset) }
        item { SectionEyebrow("Bedienung") }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(CardBackground).padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VolumeUp, null, modifier = Modifier.size(20.dp), tint = SubtleText)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Wisch-Sound", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Nur bei Gewicht und Wiederholungen", fontSize = 11.sp, color = SubtleText)
                }
                Switch(checked = soundEnabled, onCheckedChange = {
                    soundEnabled = it
                    sounds.setEnabled(it)
                })
            }
        }
        item { SectionEyebrow("Über die App") }
        item {
            SettingRow("Nach Updates suchen", "Prüft öffentliche GitHub-Releases", Icons.Outlined.SystemUpdate, onCheckUpdates)
            if (updateStatus != null) Text(updateStatus, fontSize = 12.sp, color = SubtleText)
        }
        item { SettingRow("repavio V1.3.3", "Offline nutzbar · Material You", Icons.Outlined.Info, null) }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardBackground)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = SubtleText)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = SubtleText)
        }
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null,
            modifier = Modifier.size(19.dp), tint = SubtleText)
    }
}
