package io.github.hcisme.vaultme.ui.screen.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hcisme.vaultme.components.EnhancedLifecycleAware
import io.github.hcisme.vaultme.utils.UpdateManager

/**
 * 权限引导对话框：仅负责权限
 */
@Composable
fun InstallPermissionDialog() {
    val context = LocalContext.current
    val viewModel = viewModel<UpdateViewModel>()
    var dontShowAgain by remember { mutableStateOf(false) }

    EnhancedLifecycleAware(
        onResumed = {
            if (UpdateManager.isInstallPermissionGranted(context)) {
                viewModel.onPermissionGranted()
            }
        }
    )

    AlertDialog(
        onDismissRequest = { viewModel.ignorePermission(dontShowAgain) },
        title = { Text("需要安装权限") },
        text = {
            Column {
                Text("需要您手动授予“安装未知应用”权限才能进行更新 授权后请返回应用")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dontShowAgain = !dontShowAgain }
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = "不再提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.openPermissionSettings() }
            ) {
                Text("去授权")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.ignorePermission(dontShowAgain) }
            ) {
                Text("取消")
            }
        }
    )
}
