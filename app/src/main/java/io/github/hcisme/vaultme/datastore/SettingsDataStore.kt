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
import kotlin.concurrent.Volatile

class SettingsDataStore private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: SettingsDataStore? = null

        fun getInstance(context: Context): SettingsDataStore =
            instance ?: synchronized(this) {
                instance ?: SettingsDataStore(context).also { instance = it }
            }
    }

    private val appContext = context.applicationContext

    private object Keys {
        val ACCOUNT = stringPreferencesKey(Constant.DATASTORE_KEY_JIANGUOYUN_ACCOUNT)
        val APP_PASSWORD = stringPreferencesKey(Constant.DATASTORE_KEY_JIANGUOYUN_APP_PASSWORD)
        val WEBDAV_URL = stringPreferencesKey(Constant.DATASTORE_KEY_JIANGUOYUN_WEBDAV_URL)
    }

    val jianguoyunSettings: Flow<JianguoyunSettings> =
        appContext.appDataStore.data
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

    suspend fun saveJianguoyunSettings(settings: JianguoyunSettings) {
        val encryptedPassword = withContext(Dispatchers.Default) {
            AesUtils.encrypt(settings.appPassword)
        }
        appContext.appDataStore.edit { prefs ->
            prefs[Keys.ACCOUNT] = settings.account
            prefs[Keys.APP_PASSWORD] = encryptedPassword
            prefs[Keys.WEBDAV_URL] = settings.webdavUrl
        }
    }
}

data class JianguoyunSettings(
    val account: String = "",
    val appPassword: String = "",
    val webdavUrl: String = Constant.DEFAULT_WEBDAV_URL
) {

    val isConfigured: Boolean
        get() = account.isNotBlank() && appPassword.isNotBlank()
}
