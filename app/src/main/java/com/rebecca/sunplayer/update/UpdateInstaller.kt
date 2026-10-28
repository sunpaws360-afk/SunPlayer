package com.rebecca.sunplayer.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a release APK into app-private cache and hands it to the system
 * package installer. No storage permission is needed because the file is served
 * through a [FileProvider] with a one-shot read grant.
 */
object UpdateInstaller {
    private const val UPDATE_DIR = "updates"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val HTTP_TIMEOUT_MS = 30_000
    private const val BUFFER_SIZE = 16 * 1024

    /**
     * Downloads [info] to the cache directory.
     *
     * @param onProgress Called with a 0..100 percentage on the main thread.
     * @return the downloaded APK, or `null` on any network/IO failure.
     */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        // Drop APKs from earlier attempts so the cache cannot grow unbounded.
        updateDir.listFiles()?.forEach { stale ->
            if (stale.isFile) stale.delete()
        }

        val target = File(updateDir, "sunplayer-${info.versionName}.apk")
        val partial = File(updateDir, "${target.name}.part")

        try {
            val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = HTTP_TIMEOUT_MS
                readTimeout = HTTP_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SunPlayer-Updater")
                instanceFollowRedirects = true
            }

            val downloaded = try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    false
                } else {
                    val totalBytes = connection.contentLengthLong
                    var copiedBytes = 0L
                    var lastPercent = -1

                    connection.inputStream.use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                copiedBytes += read
                                if (totalBytes > 0) {
                                    val percent = ((copiedBytes * 100) / totalBytes).toInt()
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        withContext(Dispatchers.Main) { onProgress(percent) }
                                    }
                                }
                            }
                        }
                    }
                    // Reject truncated downloads when the server declared a size.
                    totalBytes <= 0 || copiedBytes == totalBytes
                }
            } finally {
                connection.disconnect()
            }

            if (!downloaded) {
                partial.delete()
                return@withContext null
            }
            if (!partial.renameTo(target)) {
                partial.delete()
                return@withContext null
            }
            target
        } catch (error: CancellationException) {
            partial.delete()
            throw error
        } catch (error: Exception) {
            partial.delete()
            null
        }
    }

    /**
     * Opens the system package installer for [apkFile].
     *
     * @return `false` when the installer could not be launched (for example when
     *         the user has not allowed SunPlayer to install unknown apps).
     */
    fun install(context: Context, apkFile: File): Boolean {
        if (!apkFile.isFile) return false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (error: ActivityNotFoundException) {
            false
        } catch (error: IllegalArgumentException) {
            false
        } catch (error: SecurityException) {
            false
        }
    }
}
