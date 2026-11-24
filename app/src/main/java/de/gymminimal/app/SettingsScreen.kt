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
    bands: List<Band> = emptyList(),
    onBands: (List<Band>) -> Unit = {},
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
    var soundEnabled by remember { mutableStateOf(sounds.enabled()) }
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
    deleteWeight?.let { entry ->
        AlertDialog(onDismissRequest = { deleteWeight = null }, containerColor = CardBackground,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Eintrag löschen?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Der Gewichtseintrag wird aus dem Verlauf entfernt.", color = SubtleText) },
            confirmButton = { TextButton(onClick = {
                onProfileChange(deleteWeight(profile, entry.timestamp)); deleteWeight = null
            }) { Text("Löschen", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteWeight = null }) { Text("Abbrechen") } })
    }
    if (passwordDialog) BackupPasswordDialog(backupPasswordSet, passwordBusy,
        onSet = onSetBackupPassword,
        onRemove = { onRemoveBackupPassword(); passwordDialog = false },
        onDismiss = { passwordDialog = false })

    if (section == null) {
        val rows = listOf(
            Triple("backup", Icons.Outlined.Backup, "Backup" to "${backups.size} lokale Sicherungen" +
                if (backupPasswordSet) " · Profil verschlüsselt" else ""),
            Triple("bedienung", Icons.Outlined.TouchApp, "Bedienung" to (if (soundEnabled) "Wisch-Sound an" else "Wisch-Sound aus") +
                " · Tabs · ${bands.size} Bänder"),
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
                item { BandsCard(bands, onBands) }
                item {
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

/** Widerstandsbänder: Farbe und Unterstützung in kg, für EK-Übungen. */
@Composable
private fun BandsCard(bands: List<Band>, onChange: (List<Band>) -> Unit) {
    var editing by remember { mutableStateOf<Band?>(null) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBackground)
        .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)) {
        Text("Widerstandsbänder", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Für Eigengewicht-Übungen. Die Unterstützung wird in der Analyse vom Körpergewicht abgezogen.",
            fontSize = 12.sp, color = SubtleText, modifier = Modifier.padding(end = 10.dp, bottom = 4.dp))
        bands.sortedBy { it.kg }.forEach { band ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable { editing = band }
                .padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color(bandArgb(band.color))))
                Spacer(Modifier.width(12.dp))
                Text(band.color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("−${loadKg(band.kg)} kg", fontSize = 13.sp, color = SubtleText)
            }
        }
        TextButton(onClick = { editing = Band(color = BAND_COLORS.first().first, kg = 0.0) }) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("Band hinzufügen")
        }
    }
    editing?.let { band ->
        val isNew = bands.none { it.id == band.id }
        var color by remember(band.id) { mutableStateOf(band.color) }
        var kg by remember(band.id) { mutableStateOf(if (band.kg > 0) loadKg(band.kg) else "") }
        val value = kg.toWeightOrNull()
        val candidate = band.copy(color = color, kg = value ?: 0.0)
        AlertDialog(onDismissRequest = { editing = null }, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
            title = { Text(if (isNew) "Band hinzufügen" else "Band bearbeiten", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Farbe", fontSize = 12.sp, color = SubtleText)
                    BAND_COLORS.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (name, argb) ->
                                val active = color == name
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else FieldBackground)
                                    .clickable { color = name }.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(Color(argb)))
                                    Text(name, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    OutlinedTextField(kg, { kg = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5) },
                        label = { Text("Unterstützung (kg)") }, singleLine = true, isError = !validBand(candidate),
                        supportingText = { Text("Etwa so viel Last nimmt das Band ab (Herstellerangabe oder Schätzung).") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    if (!isNew) TextButton(onClick = { onChange(bands.filterNot { it.id == band.id }); editing = null }) {
                        Text("Band löschen", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(enabled = validBand(candidate), shape = RoundedCornerShape(24.dp), onClick = {
                    onChange(if (isNew) bands + candidate else bands.map { if (it.id == band.id) candidate else it })
                    editing = null
                }) { Text("Speichern", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Abbrechen") } })
    }
}
