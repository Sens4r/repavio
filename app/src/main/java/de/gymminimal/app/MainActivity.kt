package de.gymminimal.app

import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Settings
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
                NavigationBar(
                    containerColor = ScreenBackground,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Outlined.FitnessCenter, "Training") },
                        label = { Text("Training") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = CardBackground,
                            unselectedTextColor = SubtleText,
                            unselectedIconColor = SubtleText
                        )
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(Icons.Outlined.BarChart, "Analyse") },
                        label = { Text("Analyse") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = CardBackground,
                            unselectedTextColor = SubtleText,
                            unselectedIconColor = SubtleText
                        )
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Outlined.Settings, "Einstellungen") },
                        label = { Text("Einstellungen") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = CardBackground,
                            unselectedTextColor = SubtleText,
                            unselectedIconColor = SubtleText
                        )
                    )
                }
            }
        ) { inset ->
            Box(
                Modifier.fillMaxSize()
                    .padding(inset)
                    .background(ScreenBackground)
            ) {
                when (tab) {
                    0 -> TrainingScreen(app, update)
                    1 -> AnalyzeScreen(app, update)
                    else -> SettingsScreen(backup, sounds,
                        onExport = { exportLauncher.launch("repavio_Backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onChooseFolder = { folderLauncher.launch(null) },
                        onRestore = { file ->
                            try { pendingImport = file.readText(Charsets.UTF_8) }
                            catch (_: Exception) { importError = "Sicherung konnte nicht gelesen werden." }
                        },
                        onReset = { resetRequested = true })
                }
            }
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
                        } catch (_: Exception) { importError = "Backup ungültig oder beschädigt." }
                        pendingImport = null
                    }) { Text("Wiederherstellen") }
                },
                dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Abbrechen") } })
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
                text = { Text("Alle Trainingsdaten, Pläne, Phasen, Tags, Entwürfe, Einstellungen und interne Backups werden dauerhaft entfernt. Bereits exportierte Dateien bleiben erhalten. Vorher ein Backup exportieren, falls du etwas behalten möchtest.") },
                confirmButton = {
                    TextButton(onClick = {
                        store.clear()
                        backup.clearLocalBackups()
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
