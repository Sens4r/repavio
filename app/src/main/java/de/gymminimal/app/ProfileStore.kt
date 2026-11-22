package de.gymminimal.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

/**
 * Personal data, encrypted at rest with an AES-256-GCM key from the Android Keystore.
 * The key never leaves the device, so this data is unreadable after a reinstall.
 *
 * Backups: only if the user set a backup password. The password is stretched with
 * PBKDF2-HMAC-SHA256; the derived key itself is stored wrapped by the Keystore so that the
 * daily background backup can encrypt without asking. A backup carries salt and iteration
 * count, so it can be restored on another device with the password alone.
 */
class ProfileStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("repavio_profile_v1", Context.MODE_PRIVATE)
    private val photoFile = File(context.filesDir, "profile_photo.bin")

    // ---------- Profile ----------

    fun load(): Profile = try {
        prefs.getString("profile", null)?.let { decode(JSONObject(String(deviceDecrypt(it), Charsets.UTF_8))) }
            ?: Profile()
    } catch (_: Exception) {
        Profile() // e.g. key lost: never crash, start with an empty profile
    }

    fun save(profile: Profile) {
        prefs.edit().putString("profile", deviceEncrypt(encode(profile).toString().toByteArray(Charsets.UTF_8))).apply()
    }

    fun loadPhoto(): ByteArray? = try {
        if (photoFile.exists()) deviceDecrypt(photoFile.readText(Charsets.UTF_8)) else null
    } catch (_: Exception) { null }

    fun savePhoto(jpeg: ByteArray?) {
        if (jpeg == null) photoFile.delete()
        else photoFile.writeText(deviceEncrypt(jpeg), Charsets.UTF_8)
    }

    /** Full reset: profile, photo and backup password. */
    fun clear() {
        photoFile.delete()
        prefs.edit().clear().commit()
    }

    // ---------- Backup password ----------

    fun hasBackupPassword(): Boolean = prefs.contains("backup_key")

    /** Slow (PBKDF2): call off the main thread. */
    fun setBackupPassword(password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = derive(password, salt, ITERATIONS)
        prefs.edit()
            .putString("backup_salt", b64(salt))
            .putInt("backup_iter", ITERATIONS)
            .putString("backup_key", deviceEncrypt(key))
            .commit()
    }

    fun removeBackupPassword() {
        prefs.edit().remove("backup_salt").remove("backup_iter").remove("backup_key").commit()
    }

    /** Encrypted profile (with photo) for a backup file, or null if no password is set. */
    fun backupBlob(): JSONObject? = try {
        val wrapped = prefs.getString("backup_key", null)
        val salt = prefs.getString("backup_salt", null)
        if (wrapped == null || salt == null) null else {
            val key = SecretKeySpec(deviceDecrypt(wrapped), "AES")
            val payload = encode(load()).apply { loadPhoto()?.let { put("photo", b64(it)) } }
            val cipher = Cipher.getInstance(AES_GCM).apply { init(Cipher.ENCRYPT_MODE, key) }
            val data = cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))
            JSONObject().apply {
                put("v", 1); put("kdf", "PBKDF2WithHmacSHA256")
                put("iter", prefs.getInt("backup_iter", ITERATIONS)); put("salt", salt)
                put("iv", b64(cipher.iv)); put("data", b64(data))
            }
        }
    } catch (_: Exception) { null }

    /** Slow (PBKDF2). Returns false on a wrong password or a damaged block. */
    fun restoreFromBackup(blob: JSONObject, password: String): Boolean = try {
        val key = derive(password, unb64(blob.getString("salt")), blob.getInt("iter"))
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, unb64(blob.getString("iv"))))
        }
        val json = JSONObject(String(cipher.doFinal(unb64(blob.getString("data"))), Charsets.UTF_8))
        val photo = json.optString("photo").takeIf { it.isNotBlank() }?.let(::unb64)
        save(decode(json).copy(hasPhoto = photo != null))
        savePhoto(photo)
        true
    } catch (_: Exception) { false }

    // ---------- Photo processing ----------

    /** Square, max 512 px, EXIF-rotated JPEG. No copy of the original is kept. */
    fun preparePhoto(uri: Uri): ByteArray? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 512) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Bild nicht lesbar")
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val side = min(decoded.width, decoded.height)
        val scale = min(1f, 512f / side)
        val matrix = Matrix().apply { postRotate(rotation); postScale(scale, scale) }
        val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2,
            side, side, matrix, true)
        ByteArrayOutputStream().use { out ->
            square.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    } catch (_: Exception) { null }

    // ---------- Internals ----------

    private fun deviceKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    private fun deviceEncrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(AES_GCM).apply { init(Cipher.ENCRYPT_MODE, deviceKey()) }
        return b64(cipher.iv + cipher.doFinal(plain))
    }

    private fun deviceDecrypt(stored: String): ByteArray {
        val bytes = unb64(stored)
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(128, bytes, 0, 12))
        }
        return cipher.doFinal(bytes, 12, bytes.size - 12)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }

    private fun encode(p: Profile) = JSONObject().apply {
        put("name", p.name); put("birthDate", p.birthDate); put("sex", p.sex)
        put("heightCm", p.heightCm); put("activity", p.activity); put("goal", p.goal)
        put("avatar", p.avatar); put("hasPhoto", p.hasPhoto)
        put("weights", JSONArray().apply {
            p.weights.forEach { w -> put(JSONObject().apply { put("t", w.timestamp); put("kg", w.kg) }) }
        })
    }

    private fun decode(j: JSONObject): Profile {
        val weights = j.optJSONArray("weights")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { WeightEntry(it.getLong("t"), it.getDouble("kg")) }
            }
        }.orEmpty().filter { validWeight(it.kg) }.sortedBy { it.timestamp }
        return Profile(
            name = j.optString("name").take(40),
            birthDate = j.optString("birthDate").takeIf { ageOn(it) != null }.orEmpty(),
            sex = j.optString("sex").takeIf { it == "m" || it == "w" }.orEmpty(),
            heightCm = j.optInt("heightCm").takeIf(::validHeight) ?: 0,
            activity = activityLevel(j.optString("activity")).key,
            goal = nutritionGoal(j.optString("goal")).key,
            avatar = j.optString("avatar").takeIf { key -> AVATARS.any { it.key == key } }.orEmpty(),
            hasPhoto = j.optBoolean("hasPhoto"),
            weights = weights
        )
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(text: String) = Base64.decode(text, Base64.NO_WRAP)

    companion object {
        private const val KEY_ALIAS = "repavio_profile_key"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val ITERATIONS = 150_000
    }
}
