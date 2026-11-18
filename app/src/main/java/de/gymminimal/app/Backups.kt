package de.gymminimal.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** No network or cloud: backup files stay on the device or in the user-chosen document folder. */
class BackupManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("gymminimal_backup_v1", Context.MODE_PRIVATE)
    private val directory = File(context.filesDir, "backups").apply { mkdirs() }

    fun recentFiles(): List<File> = directory.listFiles()?.filter { it.extension == "json" }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** Reset app-only backup copies and settings. External files remain untouched. */
    fun clearLocalBackups() {
        directory.listFiles()?.forEach { it.delete() }
        selectedFolder()?.let { uri ->
            try {
                context.contentResolver.releasePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (_: Exception) { /* The external backup files themselves are deliberately preserved. */ }
        }
        prefs.edit().clear().commit()
    }

    fun selectedFolder(): Uri? = prefs.getString("folder_uri", null)?.let(Uri::parse)

    fun chooseFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        prefs.edit().putString("folder_uri", uri.toString()).remove("last_external_date").apply()
        backupNow()
    }

    fun removeFolder() { prefs.edit().remove("folder_uri").apply() }

    /** A dated daily snapshot, with 14 retained versions and an optional external copy. */
    fun backupNow(): Boolean {
        return try {
            val today = LocalDate.now().toString()
            val content = LocalStore(context).exportJson()
            val target = File(directory, "repavio_$today.json")
            val temp = File(directory, "repavio_$today.tmp")
            temp.writeText(content, Charsets.UTF_8)
            if (!temp.renameTo(target)) { target.writeText(content, Charsets.UTF_8); temp.delete() }
            recentFiles().filter { it.name.startsWith("gymminimal_") || it.name.startsWith("repavio_") }.drop(14).forEach { it.delete() }
            prefs.edit().putLong("last_internal", System.currentTimeMillis()).apply()
            backupExternalIfNeeded(today, content)
            true
        } catch (_: Exception) { false }
    }

    private fun backupExternalIfNeeded(today: String, content: String) {
        if (prefs.getString("last_external_date", null) == today) return
        val tree = selectedFolder() ?: return
        try {
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val created = DocumentsContract.createDocument(resolver, parent, "application/json", "repavio_$today.json")
                ?: return
            resolver.openOutputStream(created, "w")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: return
            prefs.edit().putString("last_external_date", today).apply()
        } catch (_: Exception) {
            // An inaccessible or revoked folder must not break training logging.
        }
    }

    /** Before any import, preserve an unmodified copy of the previous state. */
    fun createManualSafetyCopy(content: String) {
        File(directory, "before_import_${System.currentTimeMillis()}.json")
            .writeText(content, Charsets.UTF_8)
    }

    fun lastBackupTime(): Long = prefs.getLong("last_internal", 0L)

    companion object {
        fun schedule(context: Context) {
            val work = PeriodicWorkRequest.Builder(DailyBackupWorker::class.java, 1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "gymminimal_daily_backup", ExistingPeriodicWorkPolicy.KEEP, work)
        }
    }
}

class DailyBackupWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = if (BackupManager(applicationContext).backupNow())
        Result.success() else Result.retry()
}
