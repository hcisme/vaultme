package io.github.hcisme.vaultme.ui.screen.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.hcisme.vaultme.datastore.UpdateDataStore
import io.github.hcisme.vaultme.utils.UpdateManager
import io.github.hcisme.vaultme.utils.UpdateResult
import kotlinx.coroutines.flow.first

@Composable
fun AutoUpdateChecker() {
    val context = LocalContext.current
    val updateDataStore = remember { UpdateDataStore.getInstance(context) }
    var updateResult by remember { mutableStateOf<UpdateResult.HasUpdate?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    fun switchDialog(update: Boolean = false, permission: Boolean = false) {
        showUpdateDialog = update
        showPermissionDialog = permission
    }

    LaunchedEffect(Unit) {
        val result = UpdateManager.checkUpdate(isAutoCheck = true)
        if (result is UpdateResult.HasUpdate) {
            val ignoreUpdate = updateDataStore.ignoreUpdateDialog.first()
            if (ignoreUpdate) return@LaunchedEffect

            updateResult = result
            if (UpdateManager.isInstallPermissionGranted(context)) {
                switchDialog(update = true)
            } else {
                val ignorePermission = updateDataStore.ignorePermissionDialog.first()
                if (!ignorePermission) {
                    switchDialog(permission = true)
                }
            }
        }
    }

    if (showPermissionDialog) {
        InstallPermissionDialog(
            onDismiss = { switchDialog() },
            onPermissionGranted = { switchDialog(update = true) }
        )
    }

    val res = updateResult
    if (showUpdateDialog && res != null) {
        UpdateDialog(
            initialState = UpdateState.HasUpdate(
                res.tagName,
                res.body,
                res.downloadUrl
            ),
            onDismiss = { switchDialog() },
            onNeedPermission = { switchDialog(permission = true) }
        )
    }
}
