package io.github.hcisme.vaultme.repository

import android.util.Log
import android.util.Xml
import io.github.hcisme.vaultme.datastore.JianguoyunSettings
import io.github.hcisme.vaultme.datastore.SettingsDataStore
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import io.github.hcisme.vaultme.utils.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.TimeUnit

/**
 * 坚果云 WebDAV 同步仓库：上传 / 下载 / 删除凭据。
 * 未配置坚果云时静默跳过，失败不抛异常（不影响本地操作）。
 */
class WebDavRepository(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val settingsStore: SettingsDataStore
) {
    /**
     * 上传一条凭据到坚果云。文件名用 uuid，避免多设备冲突。
     */
    suspend fun uploadCredential(entity: CredentialEntity) {
        if (entity.uuid.isBlank()) return

        val config = settingsStore.jianguoyunSettings.first()
        if (!config.isConfigured) {
            Log.d(TAG, "未配置坚果云，跳过上传: ${entity.uuid}")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val base = config.webdavUrl.toHttpUrl()
                ensureRemoteDirectory(base, config)

                val fileUrl = buildRemoteUrl(base, "${entity.uuid}.json")

                val json = buildJsonObject {
                    put("id", entity.id)
                    put("uuid", entity.uuid)
                    put("platform", entity.platform)
                    put("account", entity.account)
                    put("password", entity.password)
                    put("updatedAt", entity.updatedAt)
                }.toString()

                val request = Request.Builder()
                    .url(fileUrl)
                    .put(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("Authorization", Credentials.basic(config.account, config.appPassword))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "上传成功 ${entity.uuid} -> HTTP ${response.code}")
                    } else {
                        val body = response.body?.string().orEmpty()
                        Log.w(TAG, "上传失败 ${entity.uuid} -> HTTP ${response.code}: $body")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "上传异常 ${entity.uuid}", e)
            }
        }
    }

    /**
     * 从坚果云拉取全部凭据（PROPFIND 列目录 + 逐个 GET）。
     * 返回解析出的凭据列表，由调用方决定如何合并进本地库。
     */
    suspend fun downloadCredentials(): List<CredentialEntity> {
        val config = settingsStore.jianguoyunSettings.first()
        if (!config.isConfigured) {
            Log.d(TAG, "未配置坚果云，跳过拉取")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                val base = config.webdavUrl.toHttpUrl()
                val dirUrl = buildRemoteUrl(base, "")

                // 1. PROPFIND 列出目录下的文件
                val propfind = Request.Builder()
                    .url(dirUrl)
                    .method("PROPFIND", null)
                    .header("Depth", "1")
                    .header("Authorization", Credentials.basic(config.account, config.appPassword))
                    .build()

                val xml = try {
                    okHttpClient.newCall(propfind).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body.string()
                        } else {
                            Log.w(TAG, "列目录失败 $dirUrl -> HTTP ${response.code}")
                            ""
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "PROPFIND 异常 $dirUrl", e)
                    ""
                }
                if (xml.isBlank()) {
                    return@withContext emptyList()
                }

                val uuids = parseHrefs(xml)
                    .mapNotNull { href ->
                        val name = href.substringAfterLast('/')
                        if (name.endsWith(".json")) name.removeSuffix(".json") else null
                    }
                    .distinct()

                // 2. 逐个下载并解析
                val result = mutableListOf<CredentialEntity>()
                for (uuid in uuids) {
                    val fileUrl = buildRemoteUrl(base, "$uuid.json")

                    val get = Request.Builder()
                        .url(fileUrl)
                        .get()
                        .header(
                            "Authorization",
                            Credentials.basic(config.account, config.appPassword)
                        )
                        .build()

                    okHttpClient.newCall(get).execute().use { response ->
                        if (response.isSuccessful) {
                            val entity = parseCredential(response.body.string())
                            if (entity != null) {
                                result.add(entity)
                            } else {
                                Log.w(TAG, "解析失败 $uuid")
                            }
                        } else {
                            Log.w(TAG, "下载失败 $uuid -> HTTP ${response.code}")
                        }
                    }
                }
                Log.i(TAG, "拉取完成: 远程 ${uuids.size} 个，成功解析 ${result.size} 个")
                result
            } catch (e: Exception) {
                Log.e(TAG, "拉取异常", e)
                emptyList()
            }
        }
    }

    /**
     * 删除云端的一条凭据文件。
     */
    suspend fun deleteCredential(uuid: String) {
        if (uuid.isBlank()) return

        val config = settingsStore.jianguoyunSettings.first()
        if (!config.isConfigured) {
            Log.d(TAG, "未配置坚果云，跳过删除: $uuid")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val base = config.webdavUrl.toHttpUrl()
                val fileUrl = buildRemoteUrl(base, "$uuid.json")

                val request = Request.Builder()
                    .url(fileUrl)
                    .delete()
                    .header("Authorization", Credentials.basic(config.account, config.appPassword))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "删除成功 $uuid -> HTTP ${response.code}")
                    } else {
                        Log.w(
                            TAG,
                            "删除失败 $uuid -> HTTP ${response.code}: ${response.body.string()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除异常 $uuid", e)
            }
        }
    }

    /**
     * 基于 WebDAV 根地址拼接远程文件/目录 URL（自动带上同步目录路径）。
     */
    private fun buildRemoteUrl(base: HttpUrl, vararg segments: String): HttpUrl {
        return base.newBuilder().apply {
            for (segment in Constant.SYNC_REMOTE_PATH.split('/')) {
                addPathSegment(segment)
            }
            for (segment in segments) {
                addPathSegment(segment)
            }
        }.build()
    }

    /**
     * 逐级创建远程目录（MKCOL）。目录已存在时返回 405，同样视为成功。
     */
    private fun ensureRemoteDirectory(base: HttpUrl, config: JianguoyunSettings) {
        val builder = base.newBuilder()
        for (segment in Constant.SYNC_REMOTE_PATH.split('/')) {
            if (segment.isBlank()) continue
            builder.addPathSegment(segment)
            val dirUrl = builder.build()
            try {
                val request = Request.Builder()
                    .url(dirUrl)
                    .method("MKCOL", null)
                    .header("Authorization", Credentials.basic(config.account, config.appPassword))
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 201 || response.code == 405) {
                        Log.d(TAG, "目录就绪 $dirUrl -> HTTP ${response.code}")
                    } else {
                        Log.w(TAG, "创建目录 $dirUrl -> HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MKCOL 异常 $dirUrl", e)
            }
        }
    }

    /**
     * 从 PROPFIND 的 XML 响应里解析出所有 <href> 文本。
     */
    private fun parseHrefs(xml: String): List<String> {
        val hrefs = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var event = parser.eventType
            var inHref = false
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> if (parser.name == "href") inHref = true
                    XmlPullParser.TEXT -> if (inHref) parser.text?.let { hrefs.add(it) }
                    XmlPullParser.END_TAG -> if (parser.name == "href") inHref = false
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析 PROPFIND 响应失败", e)
        }
        return hrefs
    }

    /**
     * 把远程 JSON 解析回凭据实体（password 是本地已加密的密文，直接存）。
     */
    private fun parseCredential(body: String): CredentialEntity? {
        return try {
            val json = Json.parseToJsonElement(body).jsonObject
            CredentialEntity(
                id = json["id"]?.jsonPrimitive?.longOrNull ?: 0,
                uuid = json["uuid"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                platform = json["platform"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                account = json["account"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                password = json["password"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                updatedAt = json["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析远程凭据失败", e)
            null
        }
    }

    private companion object {
        const val TAG = "WebDav"
    }
}