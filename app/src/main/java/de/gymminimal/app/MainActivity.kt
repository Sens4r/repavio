package de.gymminimal.app

import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Settings
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalView
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

    override fun onResume() { super.onResume(); resumed.value = true }
    override fun onPause() { resumed.value = false; super.onPause() }

    companion object {
        /** Stoppuhr piept nur, solange die App im Vordergrund ist. */
        val resumed = mutableStateOf(false)
    }
}

@Composable
fun MinimalTheme(black: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Material You follows the user's wallpaper/system palette, but surfaces stay neutral.
    val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme(primary = Color(0xFFAFC9B5))
    }
    val colors = dynamic.copy(
        background = if (black) Color.Black else ScreenBackground,
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
    val currentVersion = remember { runCatching { RepavioUpdates.installedVersion(context) }.getOrDefault("0.0.0") }
    val wasUpdated = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).let { it.lastUpdateTime > it.firstInstallTime } }
            .getOrDefault(false)
    }
    var newsEnabled by remember { mutableStateOf(!store.newsDisabled()) }
    var pendingNews by remember { mutableStateOf(newsToShow(currentVersion, store.newsSeen(), wasUpdated, store.newsDisabled())) }
    // Ohne Neuigkeiten (Neuinstallation, Bugfix-Version) gilt die Version sofort als gesehen.
    LaunchedEffect(Unit) { if (pendingNews.isEmpty()) store.setNewsSeen(currentVersion) }
    var app by remember { mutableStateOf(store.load()) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var resetRequested by remember { mutableStateOf(false) }
    var showingBrand by remember { mutableStateOf(true) }
    var trainingFocus by remember { mutableStateOf(false) }
    val stopwatch = rememberSaveable(saver = StopwatchController.Saver) { StopwatchController() }
    val beeper = remember(context) { StopwatchBeeper(context) }
    DisposableEffect(beeper) { onDispose { beeper.release() } }
    var stopwatchConfig by remember { mutableStateOf(beeper.load()) }
    val view = LocalView.current
    DisposableEffect(stopwatch.running) {
        view.keepScreenOn = stopwatch.running
        onDispose { view.keepScreenOn = false }
    }
    val appResumed by MainActivity.resumed
    LaunchedEffect(stopwatch.running, appResumed) {
        // Nur volle Minuten, die im Vordergrund erreicht werden; keine nachgeholten Pieptöne.
        var lastMinute = (stopwatch.elapsed() / 60_000L).toInt()
        while (stopwatch.running && appResumed) {
            delay(250)
            val minute = (stopwatch.elapsed() / 60_000L).toInt()
            if (minute > lastMinute) {
                val count = stopwatchBeeps(minute, stopwatchConfig)
                if (count > 0) beeper.beep(count)
                lastMinute = minute
            }
        }
    }
    val profileStore = remember(context) { ProfileStore(context) }
    var profile by remember { mutableStateOf(profileStore.load()) }
    var foodLog by remember { mutableStateOf(profileStore.loadFoodLog()) }
    val updateFoodLog: (FoodLog) -> Unit = { next -> foodLog = next; profileStore.saveFoodLog(next) }
    var photoBytes by remember { mutableStateOf(profileStore.loadPhoto()) }
    val photo = remember(photoBytes) {
        profileStore.photoBitmap(photoBytes)?.asImageBitmap()
    }
    var backupPasswordSet by remember { mutableStateOf(profileStore.hasBackupPassword()) }
    var passwordBusy by remember { mutableStateOf(false) }
    var pendingProfileBlob by remember { mutableStateOf<JSONObject?>(null) }
    var restoreBusy by remember { mutableStateOf(false) }
    var restoreFailed by remember { mutableStateOf(false) }
    val updateProfile: (Profile) -> Unit = { next -> profile = next; profileStore.save(next) }
    var weighConfig by remember { mutableStateOf(store.loadWeighConfig()) }
    var pureBlack by remember { mutableStateOf(store.pureBlack()) }
    var summaryEnabled by remember { mutableStateOf(!store.summaryDisabled()) }
    val snackbar = remember { SnackbarHostState() }
    val undoScope = rememberCoroutineScope()
    val undo = remember(snackbar) { UndoController(snackbar, undoScope) }
    // Einmal beim Start geprüft; erscheint erst, wenn keine anderen Pop-ups offen sind.
    var weighPrompt by remember { mutableStateOf(weighDue(weighConfig, profile.weights, java.time.LocalDate.now(), store.weighSnoozed())) }
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
            pendingImport = context.contentResolver.openInputStream(uri)?.use { readLimited(it) }
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
    var tabConfig by remember { mutableStateOf(store.loadTabs()) }
    val visibleTabs = tabConfig.visible
    var tabState by rememberSaveable { mutableIntStateOf(visibleTabs.first()) }
    // Ausgeblendete Tabs werden nie angezeigt; dann gilt der erste sichtbare.
    val tab = if (tabState in visibleTabs) tabState else visibleTabs.first()
    val update: ((AppData) -> AppData) -> Unit = { transform ->
        val next = transform(app)
        if (next !== app) {
            app = next
            store.save(next)
        }
    }

    MinimalTheme(black = pureBlack) {
        CompositionLocalProvider(LocalRepavioSounds provides sounds, LocalUndo provides undo) {
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(data, shape = RoundedCornerShape(16.dp), containerColor = Color(0xFFE2E2E9),
                        contentColor = Color(0xFF1B1D22), actionColor = MaterialTheme.colorScheme.inversePrimary)
                }
            },
            bottomBar = {
                // Icons only. Hidden in the training fullscreen mode.
                if (!(tab == TAB_TRAINING && trainingFocus)) {
                    IconTabBar(visibleTabs, tab, onSelect = { tabState = it })
                }
            }
        ) { inset ->
            Box(
                Modifier.fillMaxSize()
                    .padding(inset)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (tab) {
                    0 -> TrainingScreen(app, update, focus = trainingFocus,
                        onFocusChange = { trainingFocus = it },
                        stopwatchTile = {
                            StopwatchTile(stopwatch, stopwatchConfig) { next ->
                                stopwatchConfig = next; beeper.save(next)
                            }
                        },
                        onWorkoutSaved = { stopwatch.reset() },
                        bodyKg = profile.currentWeight,
                        workoutDuration = { stopwatch.elapsed().takeIf { it > 0 } },
                        summaryEnabled = summaryEnabled,
                        onSummaryDisabled = { summaryEnabled = false; store.setSummaryDisabled(true) })
                    1 -> AnalyzeScreen(app, update)
                    2 -> NutritionScreen(profile, foodLog, updateFoodLog, onGoal = { updateProfile(profile.copy(goal = it)) },
                        onOpenProfile = { tabState = 3 })
                    else -> SettingsScreen(backup, sounds,
                        tabConfig = tabConfig,
                        newsEnabled = newsEnabled,
                        onNewsEnabled = { on -> newsEnabled = on; store.setNewsDisabled(!on) },
                        weighConfig = weighConfig,
                        pureBlack = pureBlack,
                        onPureBlack = { on -> pureBlack = on; store.setPureBlack(on) },
                        summaryEnabled = summaryEnabled,
                        onSummaryEnabled = { on -> summaryEnabled = on; store.setSummaryDisabled(!on) },
                        onWeighConfig = { next -> weighConfig = next; store.saveWeighConfig(next) },
                        onTabConfig = { next -> tabConfig = sanitizeTabs(next); store.saveTabs(tabConfig) },
                        onExport = { exportLauncher.launch("repavio_Backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onChooseFolder = { folderLauncher.launch(null) },
                        onRestore = { file ->
                            try { pendingImport = file.inputStream().use { readLimited(it) } }
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
        if (availableUpdate != null && pendingNews.isEmpty() && !showingBrand && !resetRequested) {
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
                            tabState = 0
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
                            foodLog = profileStore.loadFoodLog()
                            backupPasswordSet = true
                            pendingProfileBlob = null
                            tabState = 3
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
                text = { Text("Alle Trainingsdaten, Pläne, Phasen, Tags, Entwürfe, Profil, Gewichtsverlauf, Ernährungstagebuch, Einstellungen und interne Backups werden dauerhaft entfernt. Bereits exportierte Dateien bleiben erhalten. Vorher ein Backup exportieren, falls du etwas behalten möchtest.") },
                confirmButton = {
                    TextButton(onClick = {
                        store.clear()
                        backup.clearLocalBackups()
                        profileStore.clear()
                        profile = Profile()
                        foodLog = FoodLog()
                        tabConfig = TabConfig()
                        photoBytes = null
                        backupPasswordSet = false
                        app = initialData()
                        store.save(app)
                        sounds.setEnabled(true)
                        store.setNewsSeen(currentVersion)
                        newsEnabled = true
                        tabState = 0
                        resetRequested = false
                    }) { Text("Alles löschen", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { resetRequested = false }) { Text("Abbrechen") } }
            )
        }
        if (pendingNews.isNotEmpty() && !showingBrand && !resetRequested) {
            NewsDialog(pendingNews, currentVersion) { neverAgain ->
                store.setNewsSeen(currentVersion)
                if (neverAgain) { store.setNewsDisabled(true); newsEnabled = false }
                pendingNews = emptyList()
            }
        }
        if (weighPrompt && pendingNews.isEmpty() && availableUpdate == null && pendingImport == null &&
            pendingProfileBlob == null && !showingBrand && !resetRequested) {
            WeighDialog(profile.weights, weighConfig, java.time.LocalDate.now(),
                onSave = { kg ->
                    addWeight(profile, kg, System.currentTimeMillis())?.let(updateProfile)
                    weighPrompt = false
                },
                onLater = { store.setWeighSnoozed(java.time.LocalDate.now()); weighPrompt = false })
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
private fun IconTabBar(visible: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    val all = listOf(
        Icons.Outlined.FitnessCenter to "Training",
        Icons.Outlined.BarChart to "Analyse",
        Icons.Outlined.Restaurant to "Ernährung",
        Icons.Outlined.Person to "Profil"
    )
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(CardBackground).navigationBarsPadding().height(64.dp)
        .selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        visible.forEach { index ->
            val (icon, label) = all[index]
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

/** Backups are untrusted input: refuse files larger than 25 MB instead of reading them into memory. */
private fun readLimited(input: java.io.InputStream, maxBytes: Int = 25 * 1024 * 1024): String {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val n = input.read(buffer)
        if (n == -1) break
        out.write(buffer, 0, n)
        require(out.size() <= maxBytes) { "Datei zu groß" }
    }
    return out.toString(Charsets.UTF_8.name())
}
