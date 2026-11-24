package de.gymminimal.app

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

/** Public GitHub Releases are queried without a login. Trainingsdaten werden nie gesendet. */
data class RepavioRelease(val version: String, val notes: String, val apkUrl: String, val sha256: String)

sealed interface RepavioUpdateResult {
    data class Available(val release: RepavioRelease) : RepavioUpdateResult
    data object Current : RepavioUpdateResult
    data object Offline : RepavioUpdateResult
}

object RepavioUpdates {
    private const val LATEST = "https://api.github.com/repos/Sens4r/repavio/releases/latest"
    private const val MAX_APK_BYTES = 150L * 1024 * 1024
    private const val APK_MIME = "application/vnd.android.package-archive"

    private fun versionParts(value: String): List<Int>? {
        val parts = value.removePrefix("v").split(".")
        if (parts.size != 3 || parts.any { it.isEmpty() || it.any { ch -> !ch.isDigit() } }) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    fun isNewer(candidate: String, installed: String): Boolean {
        val remote = versionParts(candidate) ?: return false
        val local = versionParts(installed) ?: return false
        for (index in 0..2) {
            if (remote[index] > local[index]) return true
            if (remote[index] < local[index]) return false
        }
        return false
    }

    @Suppress("DEPRECATION")
    fun installedVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

    private fun checkedDownloadUrl(release: RepavioRelease): String {
        require(versionParts(release.version) != null) { "Ungültige Versionsnummer" }
        require(release.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Ungültige Prüfsumme" }
        val tag = "v${release.version}"
        val expected = "https://github.com/Sens4r/repavio/releases/download/$tag/repavio-$tag-release.apk"
        require(release.apkUrl == expected) { "Unbekannte Downloadadresse" }
        return expected
    }

    suspend fun check(context: Context): RepavioUpdateResult = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(LATEST).openConnection() as HttpsURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "repavio-android-update-check")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext RepavioUpdateResult.Offline
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val release = JSONObject(json)
                if (release.optBoolean("draft") || release.optBoolean("prerelease"))
                    return@withContext RepavioUpdateResult.Current
                val tag = release.optString("tag_name")
                if (!isNewer(tag, installedVersion(context))) return@withContext RepavioUpdateResult.Current
                val assets = release.optJSONArray("assets") ?: return@withContext RepavioUpdateResult.Current
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name != "repavio-$tag-release.apk") continue
                    val url = asset.optString("browser_download_url")
                    val expected = "https://github.com/Sens4r/repavio/releases/download/$tag/$name"
                    if (url != expected) continue
                    if (asset.optLong("size") !in 1L..MAX_APK_BYTES) continue
                    val hash = asset.optString("digest").takeIf {
                        it.matches(Regex("sha256:[a-fA-F0-9]{64}"))
                    }?.removePrefix("sha256:") ?: return@withContext RepavioUpdateResult.Offline
                    return@withContext RepavioUpdateResult.Available(
                        RepavioRelease(tag.removePrefix("v"), release.optString("body"), url, hash)
                    )
                }
                RepavioUpdateResult.Current
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            RepavioUpdateResult.Offline
        }
    }

    /**
     * On Android 10+, download via Android DownloadManager to app-private storage,
     * verify SHA-256, then publish only the verified APK in public Downloads.
     * Installation is performed manually from Downloads, never by repavio.
     */
    suspend fun downloadAndVerify(
        context: Context,
        release: RepavioRelease,
        onProgress: (Int?) -> Unit
    ) = withContext(Dispatchers.IO) {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Auf diesem Gerät bitte den GitHub-Download verwenden."
        }
        val url = checkedDownloadUrl(release)
        require(isNewer(release.version, installedVersion(context))) { "Keine neuere Version" }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val tag = "v${release.version}"
        val filename = "repavio-$tag-release.apk"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("repavio $tag – Update")
            setDescription("APK wird heruntergeladen und geprüft")
            setMimeType("application/octet-stream")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverRoaming(false)
            setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS,
                "repavio-$tag-${System.currentTimeMillis()}.partial"
            )
        }
        val id = manager.enqueue(request)
        try {
            withTimeout(12 * 60 * 1000L) {
                var finished = false
                while (!finished) {
                    val stats = manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                        if (!cursor.moveToFirst()) error("Download wurde abgebrochen")
                        Triple(
                            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        )
                    }
                    if (stats.second > MAX_APK_BYTES || stats.third > MAX_APK_BYTES)
                        error("Die APK ist zu groß")
                    val percent = if (stats.third > 0L)
                        (stats.second * 100L / stats.third).coerceIn(0L, 100L).toInt()
                        else null
                    withContext(Dispatchers.Main) { onProgress(percent) }
                    when (stats.first) {
                        DownloadManager.STATUS_SUCCESSFUL -> finished = true
                        DownloadManager.STATUS_FAILED -> error("System-Download fehlgeschlagen")
                        else -> delay(850)
                    }
                }
            }
            // Hash and copy in ONE pass from the same file descriptor, into a still-pending
            // (invisible) Downloads entry. Only a file whose bytes matched the expected SHA-256
            // is ever made visible, so nothing can be swapped between check and publication.
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, APK_MIME)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val published = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Speichern im Download-Ordner fehlgeschlagen")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                manager.openDownloadedFile(id).use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).use { source ->
                        val output = resolver.openOutputStream(published)
                            ?: error("Speichern im Download-Ordner fehlgeschlagen")
                        output.use { sink ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                val count = source.read(buffer)
                                if (count == -1) break
                                total += count
                                if (total > MAX_APK_BYTES) error("Die APK ist zu groß")
                                digest.update(buffer, 0, count)
                                sink.write(buffer, 0, count)
                            }
                        }
                    }
                }
                require(total > 0L) { "Leere Datei" }
                val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                require(actualHash.equals(release.sha256, ignoreCase = true)) {
                    "APK-Prüfsumme stimmt nicht überein"
                }
                if (resolver.update(published, ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }, null, null) != 1) {
                    error("Download konnte nicht freigegeben werden")
                }
            } catch (e: Exception) {
                resolver.delete(published, null, null)
                throw e
            }
        } finally {
            manager.remove(id)
        }
    }

    fun openDownloadInBrowser(context: Context, release: RepavioRelease) {
        val url = checkedDownloadUrl(release)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openDownloads(context: Context) {
        context.startActivity(
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
