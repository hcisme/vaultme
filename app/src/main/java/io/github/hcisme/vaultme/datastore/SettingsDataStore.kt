package io.github.hcisme.vaultme.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.hcisme.vaultme.utils.AesUtils
import io.github.hcisme.vaultme.utils.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 坚果云配置的 DataStore 存取封装
 *
 * 账号和应用密码是敏感信息，其中应用密码会用 AesUtils 加密后再写入 DataStore。
 */
class SettingsDataStore(private val context: Context) {
    private object Keys {
        val ACCOUNT = stringPreferencesKey(Constant.DATASTORE_KEY_JIANGUOYUN_ACCOUNT)
        val APP_PASSWORD = stringPreferencesKey(Constant.DATASTORE_KEY_JIANGUOYUN_APP_PASSWORD)
        val WEBDAV_URL = stringPreferencesKey(Constant.DATASTORE_KEY_JIANGUOYUN_WEBDAV_URL)
    }

    /**
     * 监听坚果云配置变化，读取时自动解密应用密码
     */
    val jianguoyunSettings: Flow<JianguoyunSettings> =
        context.appDataStore.data
            .map { prefs ->
                val encrypted = prefs[Keys.APP_PASSWORD]
                JianguoyunSettings(
                    account = prefs[Keys.ACCOUNT] ?: "",
                    appPassword = encrypted?.let {
                        runCatching { AesUtils.decrypt(it) }.getOrDefault("")
                    } ?: "",
                    webdavUrl = prefs[Keys.WEBDAV_URL] ?: Constant.DEFAULT_WEBDAV_URL
                )
            }
            .flowOn(Dispatchers.Default)

    /**
     * 保存坚果云配置，应用密码加密后写入
     */
    suspend fun saveJianguoyunSettings(settings: JianguoyunSettings) {
        val encryptedPassword = withContext(Dispatchers.Default) {
            AesUtils.encrypt(settings.appPassword)
        }
        context.appDataStore.edit { prefs ->
            prefs[Keys.ACCOUNT] = settings.account
            prefs[Keys.APP_PASSWORD] = encryptedPassword
            prefs[Keys.WEBDAV_URL] = settings.webdavUrl
        }
    }
}

/**
 * 坚果云 WebDAV 同步配置
 */
data class JianguoyunSettings(
    val account: String = "",
    val appPassword: String = "",
    val webdavUrl: String = Constant.DEFAULT_WEBDAV_URL
) {
    /**
     * 是否已配置完成（账号和应用密码都填了）
     */
    val isConfigured: Boolean
        get() = account.isNotBlank() && appPassword.isNotBlank()
}
