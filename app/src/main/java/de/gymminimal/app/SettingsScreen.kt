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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    tabConfig: TabConfig = TabConfig(),
    onTabConfig: (TabConfig) -> Unit = {},
    newsEnabled: Boolean = true,
    onNewsEnabled: (Boolean) -> Unit = {},
    weighConfig: WeighConfig = WeighConfig(),
    onWeighConfig: (WeighConfig) -> Unit = {},
    pureBlack: Boolean = false,
    onPureBlack: (Boolean) -> Unit = {},
    summaryEnabled: Boolean = true,
    onSummaryEnabled: (Boolean) -> Unit = {},
    onExport: () -> Unit,
    onImport: () -> Unit,
    onChooseFolder: () -> Unit,
    onRestore: (File) -> Unit,
    onReset: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateStatus: String?,
    profile: Profile = Profile(),
    photo: androidx.compose.ui.graphics.ImageBitmap? = null,
    onProfileChange: (Profile) -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onAvatar: (String) -> Unit = {},
    onRemovePicture: () -> Unit = {},
    backupPasswordSet: Boolean = false,
    passwordBusy: Boolean = false,
    onSetBackupPassword: (String) -> Unit = {},
    onRemoveBackupPassword: () -> Unit = {}
) {
    var backups by remember { mutableStateOf(backup.recentFiles()) }
    var showLocal by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var feedbackMode by remember { mutableStateOf(sounds.mode()) }
    var vibStrength by remember { mutableStateOf(sounds.strength()) }
    val undo = LocalUndo.current
    val latestProfile by rememberUpdatedState(profile)
    var section by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val appVersion = remember { runCatching { RepavioUpdates.installedVersion(context) }.getOrDefault("?") }
    var avatarDialog by remember { mutableStateOf(false) }
    var editDialog by remember { mutableStateOf(false) }
    var weightDialog by remember { mutableStateOf(false) }
    var deleteWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var passwordDialog by remember { mutableStateOf(false) }
    LaunchedEffect(passwordBusy, backupPasswordSet) { if (!passwordBusy) passwordDialog = false }
    BackHandler(enabled = section != null) { section = null }

    if (avatarDialog) AvatarDialog(profile,
        onPickPhoto = { avatarDialog = false; onPickPhoto() },
        onAvatar = { onAvatar(it) },
        onRemove = { onRemovePicture(); avatarDialog = false },
        onDismiss = { avatarDialog = false })
    if (editDialog) ProfileEditDialog(profile, onSave = { onProfileChange(it); editDialog = false },
        onDismiss = { editDialog = false })
    if (weightDialog) WeightDialog(profile.currentWeight, onSave = { kg ->
        addWeight(profile, kg, System.currentTimeMillis())?.let(onProfileChange)
        weightDialog = false
    }, onDismiss = { weightDialog = false })
    // Rückgängig statt Nachfrage: sofort löschen, kurz „Rückgängig“ anbieten.
    LaunchedEffect(deleteWeight) {
        val entry = deleteWeight ?: return@LaunchedEffect
        onProfileChange(deleteWeight(profile, entry.timestamp))
        deleteWeight = null
        undo?.offer("Gewichtseintrag gelöscht") {
            val p = latestProfile
            if (p.weights.none { it.timestamp == entry.timestamp })
                onProfileChange(p.copy(weights = (p.weights + entry).sortedBy { it.timestamp }))
        }
    }
    if (passwordDialog) BackupPasswordDialog(backupPasswordSet, passwordBusy,
        onSet = onSetBackupPassword,
        onRemove = { onRemoveBackupPassword(); passwordDialog = false },
        onDismiss = { passwordDialog = false })

    if (section == null) {
        val rows = listOf(
            Triple("backup", Icons.Outlined.Backup, "Backup" to "${backups.size} lokale Sicherungen" +
                if (backupPasswordSet) " · Profil verschlüsselt" else ""),
            Triple("bedienung", Icons.Outlined.TouchApp, "Bedienung" to "Wischen: " +
                (SWIPE_FEEDBACK.firstOrNull { it.first == feedbackMode }?.second ?: "Ton") + " · Tabs · Pop-ups"),
            Triple("app", Icons.Outlined.SystemUpdate, "App & Updates" to "Version $appVersion"),
            Triple("reset", Icons.Outlined.DeleteForever, "Zurücksetzen" to "Alle Daten auf dem Gerät löschen")
        )
        LazyColumn(Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            profileOverview(profile, photo,
                onAvatar = { avatarDialog = true }, onEdit = { editDialog = true }, onWeight = { section = "weight" })
            item {
                Text("Einstellungen", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(start = 2.dp, top = 10.dp))
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)
                    .padding(vertical = 4.dp)) {
                    rows.forEachIndexed { index, (key, icon, texts) ->
                        if (index > 0) HorizontalDivider(color = FieldBackground, modifier = Modifier.padding(start = 66.dp))
                        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { section = key }
                            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(FieldBackground),
                                contentAlignment = Alignment.Center) {
                                Icon(icon, null, modifier = Modifier.size(19.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(texts.first, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(texts.second, fontSize = 12.sp, color = SubtleText)
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = SubtleText, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        return
    }

    val titles = mapOf("backup" to "Backup", "bedienung" to "Bedienung", "app" to "App & Updates",
        "reset" to "Zurücksetzen", "weight" to "Gewicht")
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
            "weight" -> weightPage(profile, onAdd = { weightDialog = true }, onDelete = { deleteWeight = it })
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
                item {
                    SettingRow("Backup-Passwort", if (backupPasswordSet) "Gesetzt · Profil und Ernährung verschlüsselt mitgesichert"
                        else "Nicht gesetzt · Profil und Ernährung nicht mitgesichert", Icons.Outlined.Key) { passwordDialog = true }
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
            "bedienung" -> {
                item { TabsCard(tabConfig, onTabConfig) }
                item { WeighSettingsCard(weighConfig, onWeighConfig) }
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(CardBackground).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.NewReleases, null, modifier = Modifier.size(22.dp), tint = SubtleText)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Neuigkeiten nach Updates", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Kurzes Pop-up bei neuen Funktionen", fontSize = 12.sp, color = SubtleText)
                        }
                        Switch(checked = newsEnabled, onCheckedChange = onNewsEnabled)
                    }
                }
                item {
                    SwipeFeedbackCard(feedbackMode, vibStrength,
                        onMode = { feedbackMode = it; sounds.setMode(it) },
                        onStrength = { vibStrength = it; sounds.setStrength(it) })
                }
                item { SwitchRow(Icons.Outlined.Contrast, "Reines Schwarz", "Hintergrund #000 für OLED-Displays, Karten bleiben grau",
                    pureBlack, onPureBlack) }
                item { SwitchRow(Icons.Outlined.EmojiEvents, "Bilanz nach dem Training", "Kurzes Pop-up nach „Training beenden“",
                    summaryEnabled, onSummaryEnabled) }
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

/** Tabs ein- und ausblenden und mit Pfeilen umsortieren. Profil bleibt immer sichtbar. */
@Composable
private fun TabsCard(config: TabConfig, onChange: (TabConfig) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)) {
        Text("Tabs", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Reihenfolge und Sichtbarkeit der Tab-Leiste. Profil enthält die Einstellungen und bleibt sichtbar.",
            fontSize = 12.sp, color = SubtleText, modifier = Modifier.padding(end = 10.dp, bottom = 4.dp))
        config.order.forEachIndexed { index, tab ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(TAB_NAMES[tab], fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    color = if (tab in config.hidden) SubtleText else MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { onChange(moveTab(config, tab, -1)) }, enabled = index > 0) {
                    Icon(Icons.Outlined.KeyboardArrowUp, "${TAB_NAMES[tab]} nach vorne")
                }
                IconButton(onClick = { onChange(moveTab(config, tab, 1)) }, enabled = index < config.order.lastIndex) {
                    Icon(Icons.Outlined.KeyboardArrowDown, "${TAB_NAMES[tab]} nach hinten")
                }
                Switch(checked = tab !in config.hidden, enabled = tab != TAB_PROFILE,
                    onCheckedChange = { onChange(toggleTab(config, tab)) })
            }
        }
    }
}


@Composable
private fun SwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String,
                      checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = SubtleText)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, fontSize = 12.sp, color = SubtleText)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Rückmeldung beim Wischen: Ton, Vibration, beides oder aus; bei Vibration die Stärke. */
@Composable
private fun SwipeFeedbackCard(mode: String, strength: String, onMode: (String) -> Unit, onStrength: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    val sounds = LocalRepavioSounds.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Vibration, null, modifier = Modifier.size(22.dp), tint = SubtleText)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Rückmeldung beim Wischen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Gewicht und Wiederholungen · pro Stufe", fontSize = 12.sp, color = SubtleText)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SWIPE_FEEDBACK.forEach { (key, label) ->
                val on = mode == key
                val icon = when (key) {
                    "sound" -> Icons.Outlined.VolumeUp
                    "vib" -> Icons.Outlined.Vibration
                    "both" -> Icons.Outlined.SurroundSound
                    else -> Icons.Outlined.VolumeOff
                }
                Column(Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (on) scheme.primary else FieldBackground)
                    .clickable(onClickLabel = label) { onMode(key); sounds?.tick(view) }
                    .padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                    Icon(icon, null, tint = if (on) scheme.onPrimary else scheme.onSurface, modifier = Modifier.size(18.dp))
                    Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (on) scheme.onPrimary else scheme.onSurface, maxLines = 1)
                }
            }
        }
        if (mode == "vib" || mode == "both") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Stärke", fontSize = 13.sp, color = SubtleText, modifier = Modifier.weight(1f))
                VIBRATION_STRENGTHS.forEach { s ->
                    val on = strength == s
                    Text(s, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (on) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) scheme.primary else FieldBackground)
                            .clickable { onStrength(s); sounds?.tick(view) }.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
            Text("Kurzer Tick bei jeder Stufe, etwas kräftiger bei vollen 10 kg. Folgt der Android-Einstellung „Vibration bei Berührung“.",
                fontSize = 11.5.sp, color = SubtleText, lineHeight = 16.sp)
        }
    }
}
