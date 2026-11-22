package de.gymminimal.app

import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Settings
import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ScreenBackground = Color(0xFF101114)
val CardBackground = Color(0xFF1B1D21)
val FieldBackground = Color(0xFF2A2D32)
val SubtleText = Color(0xFF9DA1A9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(16, 17, 20)
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.rgb(16, 17, 20)
        setContent { RepavioApp() }
    }
}

@Composable
fun MinimalTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Material You follows the user's wallpaper/system palette, but surfaces stay neutral.
    val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(primary = Color(0xFFAFC9B5))
    }
    val colors = dynamic.copy(
        background = ScreenBackground,
        surface = CardBackground,
        onBackground = Color(0xFFF3F4F5),
        onSurface = Color(0xFFF3F4F5),
        surfaceContainer = CardBackground,
        surfaceContainerLow = CardBackground,
        surfaceContainerHigh = FieldBackground
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun RepavioApp() {
    val context = LocalContext.current
    val store = remember(context) { LocalStore(context) }
    val backup = remember(context) { BackupManager(context) }
    val sounds = remember(context) { RepavioSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    var showIntro by remember { mutableStateOf(!store.introductionSeen()) }
    var app by remember { mutableStateOf(store.load()) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var resetRequested by remember { mutableStateOf(false) }
    var showingBrand by remember { mutableStateOf(true) }
    var trainingFocus by remember { mutableStateOf(false) }
    val profileStore = remember(context) { ProfileStore(context) }
    var profile by remember { mutableStateOf(profileStore.load()) }
    var photoBytes by remember { mutableStateOf(profileStore.loadPhoto()) }
    val photo = remember(photoBytes) {
        photoBytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() }
    }
    var backupPasswordSet by remember { mutableStateOf(profileStore.hasBackupPassword()) }
    var passwordBusy by remember { mutableStateOf(false) }
    var pendingProfileBlob by remember { mutableStateOf<JSONObject?>(null) }
    var restoreBusy by remember { mutableStateOf(false) }
    var restoreFailed by remember { mutableStateOf(false) }
    val updateProfile: (Profile) -> Unit = { next -> profile = next; profileStore.save(next) }
    val scope = rememberCoroutineScope()
    var availableUpdate by remember { mutableStateOf<RepavioRelease?>(null) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var downloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Int?>(null) }
    var downloadFailure by remember { mutableStateOf<String?>(null) }
    var showDownloadReady by remember { mutableStateOf(false) }

    val checkUpdates: () -> Unit = {
        if (!checkingUpdates) {
            checkingUpdates = true
            updateStatus = "Suche nach Updates …"
            scope.launch {
                when (val result = RepavioUpdates.check(context)) {
                    is RepavioUpdateResult.Available -> {
                        availableUpdate = result.release
                        updateStatus = null
                    }
                    RepavioUpdateResult.Current -> updateStatus = "repavio ist aktuell."
                    RepavioUpdateResult.Offline -> updateStatus = "Updateprüfung derzeit nicht möglich."
                }
                checkingUpdates = false
            }
        }
    }
    LaunchedEffect(Unit) {
        val result = RepavioUpdates.check(context)
        if (result is RepavioUpdateResult.Available) availableUpdate = result.release
    }
    LaunchedEffect(Unit) { delay(850); showingBrand = false }
    LaunchedEffect(Unit) { BackupManager.schedule(context); backup.backupNow() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(store.exportJson().toByteArray(Charsets.UTF_8))
            } ?: error("Datei konnte nicht geöffnet werden")
        } catch (_: Exception) { importError = "Export fehlgeschlagen." }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) try {
            pendingImport = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Datei konnte nicht geöffnet werden")
        } catch (_: Exception) { importError = "Datei konnte nicht gelesen werden." }
    }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) try { backup.chooseFolder(uri) }
        catch (_: Exception) { importError = "Ordner konnte nicht für Backups freigegeben werden." }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val bytes = withContext(Dispatchers.IO) { profileStore.preparePhoto(uri) }
            if (bytes == null) importError = "Bild konnte nicht gelesen werden."
            else {
                profileStore.savePhoto(bytes)
                photoBytes = bytes
                updateProfile(profile.copy(hasPhoto = true, avatar = ""))
            }
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val update: ((AppData) -> AppData) -> Unit = { transform ->
        val next = transform(app)
        if (next !== app) {
            app = next
            store.save(next)
        }
    }

    MinimalTheme {
        CompositionLocalProvider(LocalRepavioSounds provides sounds) {
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = ScreenBackground,
            bottomBar = {
                // Icons only. Hidden in the training fullscreen mode.
                if (!(tab == 0 && trainingFocus)) {
                    IconTabBar(tab, onSelect = { tab = it })
                }
            }
        ) { inset ->
            Box(
                Modifier.fillMaxSize()
                    .padding(inset)
                    .background(ScreenBackground)
            ) {
                when (tab) {
                    0 -> TrainingScreen(app, update, focus = trainingFocus,
                        onFocusChange = { trainingFocus = it })
                    1 -> AnalyzeScreen(app, update)
                    2 -> NutritionScreen(profile, onGoal = { updateProfile(profile.copy(goal = it)) },
                        onOpenProfile = { tab = 3 })
                    else -> SettingsScreen(backup, sounds,
                        onExport = { exportLauncher.launch("repavio_Backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onChooseFolder = { folderLauncher.launch(null) },
                        onRestore = { file ->
                            try { pendingImport = file.readText(Charsets.UTF_8) }
                            catch (_: Exception) { importError = "Sicherung konnte nicht gelesen werden." }
                        },
                        onReset = { resetRequested = true },
                        onCheckUpdates = checkUpdates,
                        updateStatus = updateStatus,
                        profile = profile, photo = photo,
                        onProfileChange = updateProfile,
                        onPickPhoto = {
                            photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onAvatar = { key ->
                            profileStore.savePhoto(null); photoBytes = null
                            updateProfile(profile.copy(avatar = key, hasPhoto = false))
                        },
                        onRemovePicture = {
                            profileStore.savePhoto(null); photoBytes = null
                            updateProfile(profile.copy(avatar = "", hasPhoto = false))
                        },
                        backupPasswordSet = backupPasswordSet,
                        passwordBusy = passwordBusy,
                        onSetBackupPassword = { password ->
                            passwordBusy = true
                            scope.launch {
                                withContext(Dispatchers.Default) { profileStore.setBackupPassword(password) }
                                backupPasswordSet = true
                                passwordBusy = false
                                backup.backupNow()
                            }
                        },
                        onRemoveBackupPassword = {
                            profileStore.removeBackupPassword()
                            backupPasswordSet = false
                        })
                }
            }
        }
        // Android's download manager handles transfer; installation remains user-initiated.
        if (availableUpdate != null && !showIntro && !showingBrand && !resetRequested) {
            val release = availableUpdate!!
            val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            AlertDialog(
                onDismissRequest = { if (!downloadingUpdate) availableUpdate = null },
                containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
                title = { Text("V${release.version} verfügbar", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when {
                            downloadingUpdate -> {
                                val progress = downloadProgress
                                if (progress != null) LinearProgressIndicator(
                                    progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                                else LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(progress?.let { "$it %" } ?: "Wird vorbereitet …",
                                    fontSize = 13.sp, color = SubtleText)
                            }
                            downloadFailure != null -> Text(downloadFailure!!, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error)
                            else -> Text(if (modern) "Wird geladen, geprüft und in Downloads abgelegt."
                                else "Download im Browser.", fontSize = 14.sp, color = SubtleText)
                        }
                        if (downloadFailure != null && !downloadingUpdate) {
                            TextButton(onClick = {
                                try {
                                    RepavioUpdates.openDownloadInBrowser(context, release)
                                    availableUpdate = null
                                    updateStatus = "GitHub als Alternative geöffnet."
                                } catch (_: Exception) {
                                    downloadFailure = "GitHub konnte nicht geöffnet werden."
                                }
                            }, contentPadding = PaddingValues(0.dp)) { Text("Stattdessen auf GitHub laden") }
                        }
                    }
                },
                confirmButton = {
                    Button(enabled = !downloadingUpdate, shape = RoundedCornerShape(24.dp), onClick = {
                        downloadFailure = null
                        if (!modern) {
                            try {
                                RepavioUpdates.openDownloadInBrowser(context, release)
                                availableUpdate = null
                                updateStatus = "GitHub-Download im Browser geöffnet."
                            } catch (_: Exception) {
                                downloadFailure = "Browser konnte nicht geöffnet werden."
                            }
                        } else {
                            downloadingUpdate = true
                            downloadProgress = null
                            scope.launch {
                                try {
                                    RepavioUpdates.downloadAndVerify(context, release) { progress ->
                                        downloadProgress = progress
                                    }
                                    availableUpdate = null
                                    showDownloadReady = true
                                    updateStatus = "APK geprüft und im Download-Ordner gespeichert."
                                } catch (_: Exception) {
                                    downloadFailure = "Download oder Prüfung fehlgeschlagen."
                                } finally {
                                    downloadingUpdate = false
                                }
                            }
                        }
                    }) { Text(if (downloadFailure != null) "Erneut" else "Laden", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(enabled = !downloadingUpdate, onClick = { availableUpdate = null }) { Text("Später") }
                }
            )
        }
        if (showDownloadReady) {
            AlertDialog(
                onDismissRequest = { showDownloadReady = false },
                containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
                title = { Text("Update bereit", fontWeight = FontWeight.ExtraBold) },
                text = { Text("Geprüft. In Downloads öffnen und installieren.", fontSize = 14.sp, color = SubtleText) },
                confirmButton = {
                    Button(shape = RoundedCornerShape(24.dp), onClick = {
                        showDownloadReady = false
                        try {
                            RepavioUpdates.openDownloads(context)
                        } catch (_: Exception) {
                            updateStatus = "Öffne den Dateimanager und gehe zu Downloads."
                        }
                    }) { Text("Öffnen", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDownloadReady = false }) { Text("Später") }
                }
            )
        }
        if (pendingImport != null) {
            AlertDialog(onDismissRequest = { pendingImport = null },
                title = { Text("Backup wiederherstellen?") },
                text = { Text("Der gesamte aktuelle Trainingsstand wird durch dieses Backup ersetzt. Vorher wird eine Sicherheitskopie angelegt.") },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            val restored = store.importJson(pendingImport!!)
                            app = restored
                            tab = 0
                            pendingProfileBlob = runCatching {
                                JSONObject(pendingImport!!).optJSONObject("profileEncrypted")
                            }.getOrNull()
                            restoreFailed = false
                        } catch (_: Exception) { importError = "Backup ungültig oder beschädigt." }
                        pendingImport = null
                    }) { Text("Wiederherstellen") }
                },
                dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Abbrechen") } })
        }
        pendingProfileBlob?.let { blob ->
            RestoreProfileDialog(restoreBusy, restoreFailed,
                onRestore = { password ->
                    restoreBusy = true
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            profileStore.restoreFromBackup(blob, password).also { success ->
                                // Same password keeps future backups restorable on this device too.
                                if (success) profileStore.setBackupPassword(password)
                            }
                        }
                        restoreBusy = false
                        if (ok) {
                            profile = profileStore.load()
                            photoBytes = profileStore.loadPhoto()
                            backupPasswordSet = true
                            pendingProfileBlob = null
                            tab = 3
                        } else restoreFailed = true
                    }
                },
                onSkip = { pendingProfileBlob = null })
        }
        if (importError != null) {
            AlertDialog(onDismissRequest = { importError = null },
                title = { Text("Hinweis") }, text = { Text(importError!!) },
                confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } })
        }
        if (resetRequested) {
            AlertDialog(
                onDismissRequest = { resetRequested = false },
                title = { Text("repavio zurücksetzen?") },
                text = { Text("Alle Trainingsdaten, Pläne, Phasen, Tags, Entwürfe, Profil, Gewichtsverlauf, Einstellungen und interne Backups werden dauerhaft entfernt. Bereits exportierte Dateien bleiben erhalten. Vorher ein Backup exportieren, falls du etwas behalten möchtest.") },
                confirmButton = {
                    TextButton(onClick = {
                        store.clear()
                        backup.clearLocalBackups()
                        profileStore.clear()
                        profile = Profile()
                        photoBytes = null
                        backupPasswordSet = false
                        app = initialData()
                        store.save(app)
                        sounds.setEnabled(true)
                        showIntro = true
                        tab = 0
                        resetRequested = false
                    }) { Text("Alles löschen", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { resetRequested = false }) { Text("Abbrechen") } }
            )
        }
        if (showIntro && !showingBrand && !resetRequested) {
            IntroTourDialog(onClose = {
                store.setIntroductionSeen(true)
                showIntro = false
            })
        }
        if (showingBrand) {
            Box(Modifier.fillMaxSize().background(ScreenBackground), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(R.drawable.repavio_wordmark_white),
                    contentDescription = "repavio", modifier = Modifier.fillMaxWidth(.75f).height(90.dp))
            }
        }
        }
        }
    }
}

@Composable
fun SectionEyebrow(text: String) {
    Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Medium,
        color = SubtleText, letterSpacing = 0.8.sp)
}

/** Icon-only tab bar, 64 dp above the system navigation area. */
@Composable
private fun IconTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Icons.Outlined.FitnessCenter to "Training",
        Icons.Outlined.BarChart to "Analyse",
        Icons.Outlined.Restaurant to "Ernährung",
        Icons.Outlined.Person to "Profil"
    )
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(CardBackground).navigationBarsPadding().height(64.dp)
        .selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { index, (icon, label) ->
            val active = index == selected
            Box(Modifier.weight(1f).fillMaxHeight()
                .selectable(selected = active, onClick = { onSelect(index) }, role = Role.Tab),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 64.dp, height = 32.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (active) scheme.primaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = if (active) scheme.onPrimaryContainer else SubtleText,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
