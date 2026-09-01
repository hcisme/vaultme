package io.github.hcisme.vaultme.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import io.github.hcisme.vaultme.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val body: String? = null,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 检查更新
     */
    suspend fun checkUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(Constant.GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateResult.Error("请求失败: HTTP ${response.code}")
                }

                val bodyString = response.body.string()
                val release = json.decodeFromString<GitHubRelease>(bodyString)

                val remoteVersionCode = release.tagName.lowercase()
                    .removePrefix("v")
                    .substringBefore(".")
                    .toIntOrNull() ?: 0

                if (remoteVersionCode > BuildConfig.VERSION_CODE) {
                    val downloadUrl =
                        release.assets.find { it.name == Constant.GITHUB_RELEASE_APK_NAME }?.browserDownloadUrl
                            ?: release.assets.firstOrNull()?.browserDownloadUrl
                            ?: ""

                    UpdateResult.HasUpdate(
                        tagName = release.tagName,
                        body = release.body ?: "",
                        downloadUrl = downloadUrl
                    )
                } else {
                    UpdateResult.UpToDate
                }
            }
        } catch (e: Exception) {
            UpdateResult.Error("网络错误: ${e.localizedMessage}")
        }
    }

    /**
     * 下载并安装 APK
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = NetworkClient.okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("下载失败: HTTP ${response.code}"))
            }

            val body = response.body
            val totalBytes = body.contentLength()
            val apkFile = File(context.externalCacheDir, Constant.GITHUB_RELEASE_APK_NAME)

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead.toFloat() / totalBytes)
                        }
                    }
                }
            }

            installApk(context, apkFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "下载安装失败", e)
            Result.failure(e)
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class HasUpdate(val tagName: String, val body: String, val downloadUrl: String) :
        UpdateResult

    data class Error(val message: String) : UpdateResult
}
