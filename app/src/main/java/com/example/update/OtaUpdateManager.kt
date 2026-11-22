package com.example.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val versionName: String,
    val tagName: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val publishedAt: String = ""
)

sealed class OtaCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : OtaCheckResult()
    data class UpToDate(val currentVersion: String, val latestVersion: String) : OtaCheckResult()
    data object Throttled : OtaCheckResult()
    data class Error(val message: String) : OtaCheckResult()
}

/**
 * In-App GitHub Releases OTA Updater for Caliper.
 * Queries https://api.github.com/repos/IERT007/CampusTrack02/releases/latest,
 * compares semantic versions against BuildConfig.VERSION_NAME, downloads APK
 * into context.cacheDir/updates/caliper-update.apk with progress callbacks,
 * and hands off installation via Android FileProvider.
 */
class OtaUpdateManager(
    private val context: Context,
    private val dataStoreManager: DataStoreManager = DataStoreManager(context.applicationContext)
) {
    companion object {
        const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/IERT007/CampusTrack02/releases/latest"
        private const val THROTTLE_WINDOW_MILLIS = 12L * 60L * 60L * 1000L // 12 hours

        /**
         * Returns > 0 if remote > current, 0 if equal, < 0 if remote < current.
         */
        fun compareSemanticVersions(remote: String, current: String): Int {
            val remoteParts = normalizeVersionParts(remote)
            val currentParts = normalizeVersionParts(current)
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r != c) return r.compareTo(c)
            }
            return 0
        }

        private fun normalizeVersionParts(version: String): List<Int> {
            val cleaned = version.trim().removePrefix("v").removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
            return cleaned.split(".").map { segment ->
                segment.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkForUpdate(force: Boolean = false): OtaCheckResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force) {
            val lastCheck = dataStoreManager.getOtaLastCheckMillisDirect()
            if (lastCheck > 0L && (now - lastCheck) < THROTTLE_WINDOW_MILLIS) {
                return@withContext OtaCheckResult.Throttled
            }
        }

        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API_URL)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("User-Agent", "Caliper-Android-OTA/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext OtaCheckResult.Error(
                        if (response.code == 404) "No published GitHub releases yet."
                        else "GitHub release check failed (HTTP ${response.code})."
                    )
                }

                dataStoreManager.setOtaLastCheckMillis(now)

                val root = JSONObject(bodyStr)
                val rawTag = root.optString("tag_name", "").trim()
                val remoteVersion = rawTag.removePrefix("v").removePrefix("V")
                val changelog = root.optString("body", "").ifBlank { "Performance improvements and bug fixes." }
                val publishedAt = root.optString("published_at", "")

                if (remoteVersion.isBlank()) {
                    return@withContext OtaCheckResult.Error("Release metadata is missing a version tag.")
                }

                val assets = root.optJSONArray("assets")
                var apkUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && isTrustedGithubReleaseUrl(downloadUrl)) {
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val cmp = compareSemanticVersions(remoteVersion, currentVersion)
                if (cmp > 0 && apkUrl.isNotBlank()) {
                    OtaCheckResult.UpdateAvailable(
                        ReleaseInfo(
                            versionName = remoteVersion,
                            tagName = rawTag,
                            changelog = changelog,
                            apkDownloadUrl = apkUrl,
                            publishedAt = publishedAt
                        )
                    )
                } else if (cmp > 0 && apkUrl.isBlank()) {
                    OtaCheckResult.Error("Version v$remoteVersion found, but no APK asset is attached.")
                } else {
                    OtaCheckResult.UpToDate(currentVersion = currentVersion, latestVersion = remoteVersion)
                }
            }
        } catch (e: Exception) {
            OtaCheckResult.Error("Unable to reach GitHub Releases: ${e.message ?: "Offline"}")
        }
    }

    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!isTrustedGithubReleaseUrl(apkUrl)) {
            return@withContext Result.failure(SecurityException("Blocked untrusted APK download URL."))
        }

        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "caliper-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val request = Request.Builder()
                .url(apkUrl)
                .addHeader("User-Agent", "Caliper-Android-OTA/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("APK download failed (HTTP ${response.code})."))
                }
                val body = response.body
                    ?: return@withContext Result.failure(Exception("Empty APK download stream."))

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val buffer = ByteArray(16 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0L) {
                                onProgress((downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }
                onProgress(1f)
                Result.success(apkFile)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Download failed: ${e.message ?: "Network error"}"))
        }
    }

    fun launchApkInstaller(apkFile: File): Result<Unit> {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Unable to launch Android installer: ${e.message}"))
        }
    }

    private fun isTrustedGithubReleaseUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase().orEmpty()
            scheme == "https" && (
                host == "github.com" ||
                    host == "objects.githubusercontent.com" ||
                    host == "release-assets.githubusercontent.com" ||
                    host.endsWith(".githubusercontent.com")
                )
        } catch (_: Exception) {
            false
        }
    }
}
