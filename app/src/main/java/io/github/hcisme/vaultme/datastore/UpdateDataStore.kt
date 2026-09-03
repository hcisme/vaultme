package io.github.hcisme.vaultme.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.concurrent.Volatile

class UpdateDataStore private constructor(context: Context) {
    companion object {
        @Volatile
        private var instance: UpdateDataStore? = null

        fun getInstance(context: Context): UpdateDataStore =
            instance ?: synchronized(this) {
                instance ?: UpdateDataStore(context).also { instance = it }
            }

        private object Keys {
            val IGNORE_UPDATE_DIALOG = booleanPreferencesKey("ignore_update_dialog")
            val IGNORE_PERMISSION_DIALOG = booleanPreferencesKey("ignore_permission_dialog")
        }
    }

    private val appContext = context.applicationContext

    val ignoreUpdateDialog: Flow<Boolean> = 
        context.appDataStore.data.map { it[Keys.IGNORE_UPDATE_DIALOG] ?: false }
    
    val ignorePermissionDialog: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.IGNORE_PERMISSION_DIALOG] ?: false }

    suspend fun setIgnoreUpdateDialog(ignore: Boolean) {
        appContext.appDataStore.edit { it[Keys.IGNORE_UPDATE_DIALOG] = ignore }
    }

    suspend fun setIgnorePermissionDialog(ignore: Boolean) {
        appContext.appDataStore.edit { it[Keys.IGNORE_PERMISSION_DIALOG] = ignore }
    }
}
