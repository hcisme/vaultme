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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 更新流程对话框：仅负责展示
 */
@Composable
fun UpdateDialog() {
    val viewModel = viewModel<UpdateViewModel>()
    val state = viewModel.updateState
    val downloadProgress = viewModel.downloadProgress
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = if (state is UpdateState.Downloading) ({}) else viewModel::dismissDialog,
        title = {
            Text(
                text = when (state) {
                    is UpdateState.Checking -> "正在检查"
                    is UpdateState.HasUpdate -> "发现新版本"
                    is UpdateState.Downloading -> "正在下载"
                    is UpdateState.UpToDate -> "已经是最新"
                    is UpdateState.Error -> "检查失败"
                    else -> ""
                }
            )
        },
        text = {
            Column {
                when (state) {
                    is UpdateState.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在获取版本信息...")
                    }

                    is UpdateState.HasUpdate -> {
                        Text("新版本 ${state.tagName}")
                        Spacer(modifier = Modifier.height(4.dp))
                        if (state.body.isNotBlank()) {
                            Text(state.body, style = MaterialTheme.typography.bodySmall)
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
                                modifier = Modifier.padding(start = 2.dp)
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
                    is UpdateState.Error -> Text(state.message)
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.HasUpdate -> {
                    Button(onClick = { viewModel.startDownload(state.downloadUrl) }) {
                        Text("立即更新")
                    }
                }

                is UpdateState.Downloading -> {}
                else -> {
                    TextButton(onClick = { viewModel.dismissDialog() }) { Text("确定") }
                }
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading && (state is UpdateState.HasUpdate || state is UpdateState.Checking)) {
                TextButton(onClick = {
                    viewModel.ignoreUpdate(dontShowAgain)
                }) { Text("取消") }
            }
        }
    )
}
