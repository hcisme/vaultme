package io.github.hcisme.vaultme.ui.screen.edit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.hcisme.vaultme.datastore.SettingsDataStore
import io.github.hcisme.vaultme.repository.WebDavRepository
import io.github.hcisme.vaultme.room.credentialDao
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import io.github.hcisme.vaultme.utils.AesUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class CredentialEditViewModel(application: Application) : AndroidViewModel(application) {
    var form by mutableStateOf(CredentialEditState())
        private set

    private val webDavRepository = WebDavRepository(
        settingsStore = SettingsDataStore(application)
    )

    fun loadCredential(id: Long?) {
        if (id == null) {
            // 重置为添加模式
            form = CredentialEditState()
            return
        }

        viewModelScope.launch {
            val entity = application.credentialDao.getCredentialById(id)
            if (entity != null) {
                // 将解密操作也放到计算线程，因为这也是耗时操作
                val decryptedPassword = withContext(Dispatchers.Default) {
                    try {
                        AesUtils.decrypt(entity.password)
                    } catch (_: Exception) {
                        ""
                    }
                }
                form = form.copy(
                    id = entity.id,
                    uuid = entity.uuid,
                    platformName = entity.platform,
                    account = entity.account,
                    password = decryptedPassword
                )
            }
        }
    }

    fun onFormChange(
        platformName: String = form.platformName,
        account: String = form.account,
        password: String = form.password
    ) {
        form = form.copy(
            platformName = platformName,
            account = account,
            password = password
        )
    }

    fun togglePasswordVisibility() {
        form = form.copy(isPasswordVisible = !form.isPasswordVisible)
    }

    fun save(onSuccess: () -> Unit) {
        if (!form.canSave || form.isLoading) return

        viewModelScope.launch {
            form = form.copy(isLoading = true)
            try {
                val encryptedPassword = withContext(Dispatchers.Default) {
                    AesUtils.encrypt(form.password)
                }
                val entity = CredentialEntity(
                    id = form.id ?: 0,
                    uuid = form.uuid.ifBlank { UUID.randomUUID().toString() },
                    platform = form.platformName,
                    account = form.account,
                    password = encryptedPassword,
                    updatedAt = System.currentTimeMillis()
                )
                application.credentialDao.insertCredential(entity)
                runCatching { webDavRepository.uploadCredential(entity) }
                onSuccess()
            } finally {
                form = form.copy(isLoading = false)
            }
        }
    }
}

data class CredentialEditState(
    val id: Long? = null,
    val uuid: String = "",
    val platformName: String = "",
    val account: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
) {
    val canSave: Boolean
        get() = platformName.isNotBlank() && account.isNotBlank() && password.isNotBlank()

    val isEditing: Boolean
        get() = id != null
}
