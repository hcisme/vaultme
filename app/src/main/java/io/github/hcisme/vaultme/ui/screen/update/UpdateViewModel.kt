package io.github.hcisme.vaultme.ui.screen.update

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.hcisme.vaultme.datastore.UpdateDataStore
import io.github.hcisme.vaultme.utils.UpdateManager
import io.github.hcisme.vaultme.utils.UpdateResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updateDataStore = UpdateDataStore.getInstance(application)

    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    var dialogType by mutableStateOf(UpdateDialogType.None)
        private set

    var downloadProgress by mutableFloatStateOf(0f)
        private set

    fun checkUpdate(isAutoCheck: Boolean) {
        viewModelScope.launch {
            if (isAutoCheck) {
                val ignoreUpdate = updateDataStore.ignoreUpdateDialog.first()
                if (ignoreUpdate) return@launch
            }

            updateState = UpdateState.Checking
            when (val result = UpdateManager.checkUpdate(isAutoCheck)) {
                is UpdateResult.HasUpdate -> {
                    updateState = UpdateState.HasUpdate(
                        result.tagName, result.body, result.downloadUrl
                    )

                    // 首次检查
                    if (isAutoCheck) {
                        // 自动检查逻辑：如果有更新，决定弹哪个对话框
                        if (UpdateManager.isInstallPermissionGranted(application)) {
                            setDialog(UpdateDialogType.Update)
                        } else {
                            val ignorePermission = updateDataStore.ignorePermissionDialog.first()
                            if (!ignorePermission) {
                                setDialog(UpdateDialogType.Permission)
                            }
                        }
                    } else {
                        // 手动检查逻辑：直接弹更新对话框
                        setDialog(UpdateDialogType.Update)
                    }
                }

                is UpdateResult.UpToDate -> {
                    updateState = UpdateState.UpToDate
                    if (!isAutoCheck) setDialog(UpdateDialogType.Update)
                }

                is UpdateResult.Error -> {
                    updateState = UpdateState.Error(result.message)
                    if (!isAutoCheck) setDialog(UpdateDialogType.Update)
                }
            }
        }
    }

    fun startDownload(downloadUrl: String) {
        if (!UpdateManager.isInstallPermissionGranted(application)) {
            setDialog(UpdateDialogType.Permission)
            return
        }

        viewModelScope.launch {
            updateState = UpdateState.Downloading
            downloadProgress = 0f
            val result = UpdateManager.downloadAndInstall(application, downloadUrl) {
                downloadProgress = it
            }
            if (result.isSuccess) {
                dismissDialog()
            } else {
                updateState = UpdateState.Error("安装失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun dismissDialog() {
        setDialog(UpdateDialogType.None)
        updateState = UpdateState.Idle
    }

    fun ignoreUpdate(dontShowAgain: Boolean) {
        viewModelScope.launch {
            updateDataStore.setIgnoreUpdateDialog(dontShowAgain)
            dismissDialog()
        }
    }

    fun ignorePermission(dontShowAgain: Boolean) {
        viewModelScope.launch {
            updateDataStore.setIgnorePermissionDialog(dontShowAgain)
            dismissDialog()
        }
    }

    fun openPermissionSettings() {
        UpdateManager.openInstallPermissionSettings(application)
    }

    fun onPermissionGranted() {
        if (dialogType == UpdateDialogType.Permission) {
            setDialog(UpdateDialogType.Update)
        }
    }

    private fun setDialog(type: UpdateDialogType) {
        dialogType = type
    }
}
