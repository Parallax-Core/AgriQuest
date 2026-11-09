package com.agriquest.data.remote

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.agriquest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppRelease(val versionCode: Int, val versionName: String, val apkPath: String,
    val sha256: String, val sizeBytes: Long)

/** Public release metadata only. Publishing credentials never belong in the app. */
class AppUpdateApi {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()

    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        if (!baseUrl.startsWith("https://") || BuildConfig.SUPABASE_ANON_KEY.isBlank()) return@withContext null
        val request = Request.Builder().url("$baseUrl/rest/v1/app_releases?select=version_code,version_name,apk_path,sha256,size_bytes&order=version_code.desc&limit=1")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Update check unavailable.")
            val row = JSONArray(response.body?.string().orEmpty()).optJSONObject(0) ?: return@withContext null
            val release = AppRelease(row.getInt("version_code"), row.getString("version_name"),
                row.getString("apk_path"), row.getString("sha256"), row.getLong("size_bytes"))
            require(release.apkPath == "releases/${release.versionCode}/AgriQuest.apk" &&
                release.sha256.matches(Regex("[0-9a-f]{64}")) && release.sizeBytes in 1..52_428_800L) { "Invalid update metadata." }
            release.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }
    }

    suspend fun download(context: Context, release: AppRelease, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val temporary = File(directory, "AgriQuest-${release.versionCode}.download.apk")
        val target = File(directory, "AgriQuest-${release.versionCode}.apk")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val request = Request.Builder().url("$baseUrl/storage/v1/object/public/app-updates/${release.apkPath}").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Update download failed. Please try again.")
                val body = response.body ?: throw IOException("Empty update download.")
                var received = 0L
                body.byteStream().use { input -> temporary.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > release.sizeBytes) throw IOException("Update size verification failed.")
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        onProgress((received * 100 / release.sizeBytes).toInt())
                    }
                } }
                if (received != release.sizeBytes || digest.digest().hex() != release.sha256) {
                    throw IOException("Update verification failed. Please download again.")
                }
            }
            verifyPackage(context, temporary, release.versionCode)
            if (target.exists() && !target.delete()) throw IOException("Cannot replace cached update.")
            if (!temporary.renameTo(target)) throw IOException("Cannot prepare update installation.")
            target
        } finally { temporary.delete() }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(context: Context, file: File, expectedVersion: Int) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val manager = context.packageManager
        val downloaded = manager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("The downloaded file is not a valid Android app.")
        val installed = manager.getPackageInfo(context.packageName, flags)
        val version = if (Build.VERSION.SDK_INT >= 28) downloaded.longVersionCode else downloaded.versionCode.toLong()
        if (downloaded.packageName != context.packageName || version != expectedVersion.toLong()) {
            throw IOException("This update does not match AgriQuest.")
        }
        fun certificates(info: PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex() }.toSet()
        }
        val currentSigners = certificates(installed)
        if (currentSigners.isEmpty() || currentSigners != certificates(downloaded)) {
            throw IOException("This installation uses a different signing key. Install the release version once before using app updates.")
        }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 255) }
    fun cancel() { client.dispatcher.cancelAll() }
}
