package com.rebecca.sunplayer.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * A newer release found on GitHub.
 *
 * @param versionName        Release version without the leading `v` (e.g. `1.4.0`).
 * @param releaseNotes       Release body, used verbatim from the GitHub Release.
 * @param downloadUrl        Direct URL of the release APK asset.
 * @param releasePageUrl     Human-readable release page.
 */
data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

/**
 * Checks GitHub Releases for a newer SunPlayer build.
 *
 * This is the app's only network consumer: it reads the public `releases/latest`
 * endpoint once per launch. No account, identifier or library data is sent, and
 * nothing is downloaded until the user explicitly taps "Download".
 */
object UpdateChecker {
    private const val RELEASES_API =
        "https://api.github.com/repos/sunpaws360-afk/SunPlayer/releases/latest"
    private const val HTTP_TIMEOUT_MS = 10_000

    /** Version name of the installed package, e.g. `1.4.0`. */
    fun currentVersionName(context: Context): String = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "0"
    } catch (error: PackageManager.NameNotFoundException) {
        "0"
    }

    /**
     * @return the newest release if it is strictly newer than [currentVersion],
     *         otherwise `null`. Never throws: offline devices simply get `null`.
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val currentVersion = currentVersionName(context)
        try {
            val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = HTTP_TIMEOUT_MS
                readTimeout = HTTP_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "SunPlayer/$currentVersion")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    null
                } else {
                    val payload = connection.inputStream.bufferedReader().use { it.readText() }
                    parseRelease(payload, currentVersion)
                }
            } finally {
                connection.disconnect()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
    }

    internal fun parseRelease(payload: String, currentVersion: String): UpdateInfo? {
        val json = JSONObject(payload)
        val latestVersion = json.optString("tag_name").trim()
            .removePrefix("v")
            .removePrefix("V")
        if (latestVersion.isBlank() || !isNewerVersion(latestVersion, currentVersion)) {
            return null
        }

        val assets = json.optJSONArray("assets") ?: return null
        var downloadUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                downloadUrl = asset.optString("browser_download_url")
                break
            }
        }
        if (downloadUrl.isNullOrBlank()) return null

        return UpdateInfo(
            versionName = latestVersion,
            releaseNotes = json.optString("body").trim(),
            downloadUrl = downloadUrl,
            releasePageUrl = json.optString("html_url")
        )
    }

    /** Compares dot/hyphen separated versions numerically; `1.4.0` newer than `1.3.0.1`. */
    internal fun isNewerVersion(candidate: String, current: String): Boolean {
        val candidateParts = numericParts(candidate)
        val currentParts = numericParts(current)
        if (candidateParts.isEmpty()) return false
        val length = maxOf(candidateParts.size, currentParts.size)
        for (index in 0 until length) {
            val newer = candidateParts.getOrElse(index) { 0 }
            val installed = currentParts.getOrElse(index) { 0 }
            if (newer != installed) return newer > installed
        }
        return false
    }

    private fun numericParts(version: String): List<Int> =
        version.split('.', '-', '_')
            .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
}
