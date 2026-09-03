package io.github.hcisme.vaultme.ui.screen.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AutoUpdateChecker() {
    val viewModel = viewModel<UpdateViewModel>()
    val dialogType = viewModel.dialogType

    LaunchedEffect(Unit) {
        viewModel.checkUpdate(isAutoCheck = true)
    }

    when (dialogType) {
        UpdateDialogType.Update -> {
            UpdateDialog()
        }

        UpdateDialogType.Permission -> {
            InstallPermissionDialog()
        }

        UpdateDialogType.None -> null
    }
}
