package io.github.hcisme.vaultme.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.hcisme.vaultme.R
import io.github.hcisme.vaultme.utils.UpdateManager
import io.github.hcisme.vaultme.utils.UpdateResult
import kotlinx.coroutines.launch


@Composable
fun AutoUpdateChecker() {
    var showDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }

    LaunchedEffect(Unit) {
        val result = UpdateManager.checkUpdate(isAutoCheck = true)
        if (result is UpdateResult.HasUpdate) {
            updateResult = result
            showDialog = true
        }
    }

    if (showDialog && updateResult is UpdateResult.HasUpdate) {
        val res = updateResult as UpdateResult.HasUpdate
        UpdateDialog(
            initialState = UpdateState.HasUpdate(res.tagName, res.body, res.downloadUrl),
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun UpdateButton(
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.update),
            contentDescription = "检查更新",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp)
        )
    }

    if (showDialog) {
        UpdateDialog(
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun UpdateDialog(
    initialState: UpdateState = UpdateState.Checking,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(initialState) { mutableStateOf(initialState) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(initialState) {
        if (state is UpdateState.Checking) {
            val result = UpdateManager.checkUpdate(isAutoCheck = false)
            state = when (result) {
                is UpdateResult.HasUpdate -> UpdateState.HasUpdate(
                    tagName = result.tagName,
                    body = result.body,
                    downloadUrl = result.downloadUrl
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
                        Text(
                            "更新内容：\n${current.body}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("下载进度: ${(downloadProgress * 100).toInt()}%")
                    }

                    is UpdateState.UpToDate -> {
                        Text("当前已是最新版本，无需更新。")
                    }

                    is UpdateState.Error -> {
                        Text(current.message)
                    }
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
                                    context,
                                    current.downloadUrl
                                ) { progress ->
                                    downloadProgress = progress
                                }
                                if (result.isSuccess) {
                                    onDismiss()
                                } else {
                                    state =
                                        UpdateState.Error("下载安装失败: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        } else {
                            // 未授予“安装未知应用”：先引导去系统设置，并说明原因（Android 8+ 必须）
                            state = UpdateState.Error(
                                "需要先在系统设置中允许 VaultMe “安装未知应用”，授权后请重新点击更新"
                            )
                            UpdateManager.openInstallPermissionSettings(context)
                        }
                    }) {
                        Text("立即更新")
                    }
                }

                is UpdateState.Downloading -> {}
                is UpdateState.Checking -> {}

                else -> {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading && (state is UpdateState.HasUpdate || state is UpdateState.Checking)) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

sealed interface UpdateState {
    data object Checking : UpdateState
    data class HasUpdate(
        val tagName: String,
        val body: String,
        val downloadUrl: String
    ) : UpdateState

    data object Downloading : UpdateState
    data object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}
