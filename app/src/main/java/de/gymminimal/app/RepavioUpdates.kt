package de.gymminimal.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.net.ssl.HttpsURLConnection

/** Public GitHub Releases are queried without a login. Trainingsdaten werden nie gesendet. */
data class RepavioRelease(val version: String, val notes: String, val apkUrl: String, val sha256: String?)

sealed interface RepavioUpdateResult {
    data class Available(val release: RepavioRelease) : RepavioUpdateResult
    data object Current : RepavioUpdateResult
    data object Offline : RepavioUpdateResult
}

object RepavioUpdates {
    private const val LATEST = "https://api.github.com/repos/Sens4r/repavio/releases/latest"
    private const val MAX_APK_BYTES = 150L * 1024 * 1024

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
    private fun installedVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

    suspend fun check(context: Context): RepavioUpdateResult = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(LATEST).openConnection() as HttpsURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "repavio-android-update-check")
            }
            try {
                // GitHub /latest intentionally ignores draft and prerelease versions.
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
                    if (!name.matches(Regex("repavio-v[0-9]+\\.[0-9]+\\.[0-9]+-release\\.apk"))) continue
                    val url = asset.optString("browser_download_url")
                    val expected = "https://github.com/Sens4r/repavio/releases/download/$tag/$name"
                    if (url != expected) continue // Never follow an arbitrary URL from metadata.
                    val hash = asset.optString("digest").takeIf { it.matches(Regex("sha256:[a-fA-F0-9]{64}")) }
                        ?.removePrefix("sha256:")
                    return@withContext RepavioUpdateResult.Available(
                        RepavioRelease(tag.removePrefix("v"), release.optString("body"), url, hash))
                }
                RepavioUpdateResult.Current
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            // Offline or GitHub not reachable: never block normal use of the app.
            RepavioUpdateResult.Offline
        }
    }

    /** Downloads only after the user explicitly accepts. No silent installation. */
    suspend fun download(context: Context, release: RepavioRelease): File = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "updates").apply { mkdirs() }
        val tmp = File(folder, "repavio-update.part")
        val apk = File(folder, "repavio-update.apk")
        tmp.delete()
        val connection = (URL(release.apkUrl).openConnection() as HttpsURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "repavio-android-updater")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("Download fehlgeschlagen")
            val declaredSize = connection.contentLengthLong
            if (declaredSize > MAX_APK_BYTES) error("Die APK ist zu groß")
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            connection.inputStream.use { source ->
                tmp.outputStream().use { destination ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = source.read(buffer)
                        if (count == -1) break
                        size += count
                        if (size > MAX_APK_BYTES) error("Die APK ist zu groß")
                        digest.update(buffer, 0, count)
                        destination.write(buffer, 0, count)
                    }
                }
            }
            if (declaredSize >= 0 && declaredSize != size) error("Unvollständiger Download")
            if (release.sha256 != null) {
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(release.sha256, ignoreCase = true)) error("APK-Prüfsumme stimmt nicht")
            }
            ZipFile(tmp).use { zip ->
                if (zip.getEntry("AndroidManifest.xml") == null) error("Keine gültige APK")
            }
            if (apk.exists()) apk.delete()
            if (!tmp.renameTo(apk)) error("Download konnte nicht gespeichert werden")
            apk
        } finally {
            connection.disconnect()
            tmp.delete()
        }
    }

    fun openAndroidInstaller(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
