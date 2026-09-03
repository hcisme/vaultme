package io.github.hcisme.vaultme.ui.screen.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hcisme.vaultme.datastore.UpdateDataStore
import io.github.hcisme.vaultme.utils.UpdateManager
import io.github.hcisme.vaultme.utils.UpdateResult
import kotlinx.coroutines.launch

/**
 * 更新流程对话框：负责 检查 -> 下载 -> 安装
 */
@Composable
fun UpdateDialog(
    initialState: UpdateState = UpdateState.Checking,
    onDismiss: () -> Unit,
    onNeedPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateDataStore = remember { UpdateDataStore.getInstance(context) }
    var state by remember(initialState) { mutableStateOf(initialState) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var dontShowAgain by remember { mutableStateOf(false) }

    LaunchedEffect(initialState) {
        if (state is UpdateState.Checking) {
            val result = UpdateManager.checkUpdate(isAutoCheck = false)
            state = when (result) {
                is UpdateResult.HasUpdate -> UpdateState.HasUpdate(
                    result.tagName, result.body, result.downloadUrl
                )

                is UpdateResult.UpToDate -> UpdateState.UpToDate
                is UpdateResult.Error -> UpdateState.Error(result.message)
            }
        }
    }

    AlertDialog(
        onDismissRequest = if (state is UpdateState.Downloading) ({}) else onDismiss,
        title = {
            Text(
                text = when (state) {
                    is UpdateState.Checking -> "正在检查"
                    is UpdateState.HasUpdate -> "发现新版本"
                    is UpdateState.Downloading -> "正在下载"
                    is UpdateState.UpToDate -> "已经是最新"
                    is UpdateState.Error -> "检查失败"
                }
            )
        },
        text = {
            Column {
                when (val current = state) {
                    is UpdateState.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在获取版本信息...")
                    }

                    is UpdateState.HasUpdate -> {
                        Text("新版本 ${current.tagName}")
                        Spacer(modifier = Modifier.height(4.dp))
                        if (current.body.isNotBlank()) {
                            Text(current.body, style = MaterialTheme.typography.bodySmall)
                        }
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
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("下载进度: ${(downloadProgress * 100).toInt()}%")
                    }

                    is UpdateState.UpToDate -> Text("当前已是最新版本，无需更新。")
                    is UpdateState.Error -> Text(current.message)
                }
            }
        },
        confirmButton = {
            when (val current = state) {
                is UpdateState.HasUpdate -> {
                    Button(onClick = {
                        if (UpdateManager.isInstallPermissionGranted(context)) {
                            state = UpdateState.Downloading
                            scope.launch {
                                val result = UpdateManager.downloadAndInstall(
                                    context, current.downloadUrl
                                ) { downloadProgress = it }
                                if (result.isSuccess) onDismiss()
                                else state =
                                    UpdateState.Error("安装失败: ${result.exceptionOrNull()?.message}")
                            }
                        } else {
                            onNeedPermission()
                        }
                    }) {
                        Text("立即更新")
                    }
                }

                is UpdateState.Downloading -> {}
                else -> {
                    TextButton(onClick = onDismiss) { Text("确定") }
                }
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading && (state is UpdateState.HasUpdate || state is UpdateState.Checking)) {
                TextButton(onClick = {
                    scope.launch { updateDataStore.setIgnoreUpdateDialog(dontShowAgain) }
                    onDismiss()
                }) { Text("取消") }
            }
        }
    )
}
