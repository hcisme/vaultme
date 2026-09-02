package io.github.hcisme.vaultme.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
    private var hasAutoChecked = false

    /**
     * 检查更新
     * @param isAutoCheck 是否为启动时的自动检查。如果是，则同一个生命周期内只检查一次。
     */
    suspend fun checkUpdate(isAutoCheck: Boolean = false): UpdateResult =
        withContext(Dispatchers.IO) {
            if (isAutoCheck && hasAutoChecked) {
                return@withContext UpdateResult.UpToDate
            }

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

                    val remoteVersionCode = parseVersionToCode(release.tagName)

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
                    }.also {
                        if (isAutoCheck && it !is UpdateResult.Error) {
                            hasAutoChecked = true
                        }
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
            val apkFile = File(
                context.externalCacheDir,
                "vaultme_update_${System.currentTimeMillis()}.apk"
            ).apply {
                parentFile?.mkdirs()
                context.externalCacheDir?.listFiles { f -> f.name.startsWith("vaultme_update_") }
                    ?.forEach { it.delete() }
            }

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

            if (!isSameSigner(context, apkFile.absolutePath)) {
                throw Exception("APK 签名校验失败，已拒绝安装")
            }

            installApk(context, apkFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "下载安装失败", e)
            Result.failure(e)
        }
    }

    /**
     * 是否已具备“安装未知应用”的 per-app 授权。
     * Android 8+ 下，App 无论走 ACTION_VIEW 还是 PackageInstaller 发起安装都需要它；
     * 未授权时系统会直接拦下安装（部分 ROM 会表现为“已安装更高版本/原地不生效”等怪现象）。
     */
    fun isInstallPermissionGranted(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /**
     * 跳转到本应用“安装未知应用”的系统设置页（Android 8+ 可用）。
     */
    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw IllegalStateException("当前系统没有可用的 APK 安装程序", e)
        } catch (e: SecurityException) {
            throw IllegalStateException("安装被系统拦截，请先允许“安装未知应用”后重试", e)
        }
    }

    private fun isSameSigner(context: Context, apkPath: String): Boolean {
        val installed = installedSigners(context) ?: return false
        val apk = apkSigners(context, apkPath) ?: return false
        return installed.any { installedSig ->
            apk.any { it.contentEquals(installedSig) }
        }
    }

    @Suppress("DEPRECATION")
    private fun installedSigners(context: Context): List<ByteArray>? {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.map { it.toByteArray() }
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.map { it.toByteArray() }
        }
    }

    @Suppress("DEPRECATION")
    private fun apkSigners(context: Context, apkPath: String): List<ByteArray>? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = pm.getPackageArchiveInfo(apkPath, flags) ?: return null
        val appInfo = info.applicationInfo ?: return null
        appInfo.sourceDir = apkPath
        appInfo.publicSourceDir = apkPath
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
        } else {
            info.signatures?.map { it.toByteArray() }
        }
    }

    private fun parseVersionToCode(tagName: String): Int {
        return try {
            val versionStr = tagName.lowercase().removePrefix("v")
            val parts = versionStr.split(".")
            if (parts.size >= 3) {
                // 新版格式：YYYY.Major.Minor -> YYYYMMDD
                val year = parts[0].toInt()
                val major = parts[1].toInt()
                val minor = parts[2].toInt()
                year * 10000 + major * 100 + minor
            } else {
                // 兼容旧版格式：v7 -> 7
                parts[0].toInt()
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析版本号失败: $tagName", e)
            0
        }
    }
}

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class HasUpdate(val tagName: String, val body: String, val downloadUrl: String) :
        UpdateResult

    data class Error(val message: String) : UpdateResult
}
