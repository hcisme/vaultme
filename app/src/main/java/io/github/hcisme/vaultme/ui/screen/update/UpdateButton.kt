package io.github.hcisme.vaultme.ui.screen.update

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hcisme.vaultme.R

@Composable
fun UpdateButton(
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<UpdateViewModel>()
    val dialogType = viewModel.dialogType

    IconButton(
        onClick = { viewModel.checkUpdate(isAutoCheck = false) },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.update),
            contentDescription = "检查更新",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp)
        )
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
