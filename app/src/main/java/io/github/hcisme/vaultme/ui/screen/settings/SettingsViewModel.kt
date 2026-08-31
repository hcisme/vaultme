package io.github.hcisme.vaultme.ui.screen.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hcisme.vaultme.datastore.JianguoyunSettings
import io.github.hcisme.vaultme.datastore.SettingsDataStore
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    var form by mutableStateOf(JianguoyunSettings())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var isAppPasswordVisible by mutableStateOf(false)
        private set

    fun onSettingsLoaded(settings: JianguoyunSettings) {
        form = settings
        isLoading = false
    }

    fun onFormChange(
        account: String = form.account,
        appPassword: String = form.appPassword,
        webdavUrl: String = form.webdavUrl
    ) {
        form = form.copy(
            account = account,
            appPassword = appPassword,
            webdavUrl = webdavUrl
        )
    }

    fun toggleAppPasswordVisibility() {
        isAppPasswordVisible = !isAppPasswordVisible
    }

    fun save(settingsStore: SettingsDataStore, onSuccess: (() -> Unit)? = null) {
        if (!form.isConfigured || isSaving) return

        viewModelScope.launch {
            isSaving = true
            try {
                settingsStore.saveJianguoyunSettings(form)
                onSuccess?.invoke()
            } finally {
                isSaving = false
            }
        }
    }
}
