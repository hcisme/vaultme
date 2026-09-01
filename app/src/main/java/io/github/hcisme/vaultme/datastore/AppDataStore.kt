package io.github.hcisme.vaultme.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.hcisme.vaultme.utils.Constant

internal val Context.appDataStore by preferencesDataStore(name = Constant.DATASTORE_NAME)
