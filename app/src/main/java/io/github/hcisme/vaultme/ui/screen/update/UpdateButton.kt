package io.github.hcisme.vaultme.ui.screen.update

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.hcisme.vaultme.R

@Composable
fun UpdateButton(modifier: Modifier = Modifier) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    fun switchDialog(update: Boolean = false, permission: Boolean = false) {
        showUpdateDialog = update
        showPermissionDialog = permission
    }

    IconButton(onClick = { switchDialog(update = true) }, modifier = modifier) {
        Icon(
            painter = painterResource(id = R.drawable.update),
            contentDescription = "检查更新",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp)
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismiss = { switchDialog() },
            onNeedPermission = { switchDialog(permission = true) }
        )
    }

    if (showPermissionDialog) {
        InstallPermissionDialog(
            onDismiss = { switchDialog() },
            onPermissionGranted = { switchDialog(update = true) }
        )
    }
}
