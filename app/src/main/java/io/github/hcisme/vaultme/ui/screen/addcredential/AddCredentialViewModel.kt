package io.github.hcisme.vaultme.ui.screen.addcredential

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hcisme.vaultme.room.credentialDao
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import io.github.hcisme.vaultme.utils.AesUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AddCredentialViewModel(private val application: Application) : AndroidViewModel(application) {
    var form by mutableStateOf(AddCredentialState())
        private set

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
            delay(800.milliseconds)
            val encryptedPassword = AesUtils.encrypt(form.password)
            val entity = CredentialEntity(
                platform = form.platformName,
                account = form.account,
                password = encryptedPassword
            )
            application.credentialDao.insertCredential(entity)
            form = form.copy(isLoading = false)
            onSuccess()
        }
    }
}

data class AddCredentialState(
    val platformName: String = "",
    val account: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
) {
    val canSave: Boolean
        get() = platformName.isNotBlank() && account.isNotBlank() && password.isNotBlank()
}
