package io.github.hcisme.vaultme.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import io.github.hcisme.vaultme.BuildConfig
import io.github.hcisme.vaultme.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

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
            val apkFile = File(context.externalCacheDir, Constant.GITHUB_RELEASE_APK_NAME).apply {
                parentFile?.mkdirs()
            }

            if (apkFile.exists()) {
                apkFile.delete()
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
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(context.packageName)
        }
        val session = try {
            packageInstaller.openSession(packageInstaller.createSession(params))
        } catch (e: SecurityException) {
            throw IllegalStateException("缺少安装权限，请先在系统设置中允许“安装未知应用”", e)
        }
        session.use { session ->
            val output = session.openWrite("vaultme_update", 0, file.length())
            output.use { output ->
                file.inputStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
                session.fsync(output)
            }

            // 安装完成后自动回到 App
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                // "the commit status receiver should come from a mutable pending intent"
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(pendingIntent.intentSender)
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
}

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class HasUpdate(val tagName: String, val body: String, val downloadUrl: String) :
        UpdateResult

    data class Error(val message: String) : UpdateResult
}
