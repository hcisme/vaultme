package io.github.hcisme.vaultme.ui.screen.addcredential

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hcisme.vaultme.R
import io.github.hcisme.vaultme.ui.theme.VaultMeTheme
import io.github.hcisme.vaultme.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel: AddCredentialViewModel = viewModel()
    val navController = LocalNavController.current

    AddCredentialContent(
        uiState = viewModel.form,
        onFormChange = { viewModel.onFormChange(it.platformName, it.account, it.password) },
        onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
        onSave = {
            viewModel.save {
                navController.popBackStack()
            }
        },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialContent(
    uiState: AddCredentialState,
    onFormChange: (AddCredentialState) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("添加凭据", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "返回",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 平台名称
                TextField(
                    value = uiState.platformName,
                    onValueChange = { onFormChange(uiState.copy(platformName = it)) },
                    label = { Text("平台名称") },
                    placeholder = { Text("例如：GitHub、微信") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                )

                // 账号
                TextField(
                    value = uiState.account,
                    onValueChange = { onFormChange(uiState.copy(account = it)) },
                    label = { Text("账号") },
                    placeholder = { Text("邮箱或手机号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                )

                // 密码
                TextField(
                    value = uiState.password,
                    onValueChange = { onFormChange(uiState.copy(password = it)) },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val iconId =
                            if (uiState.isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                painter = painterResource(id = iconId),
                                contentDescription = if (uiState.isPasswordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                )
            }

            // 保存按钮
            Button(
                onClick = onSave,
                enabled = uiState.canSave && !uiState.isLoading,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("保存", style = MaterialTheme.typography.titleMedium)
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddCredentialScreenPreview() {
    VaultMeTheme {
        AddCredentialContent(
            uiState = AddCredentialState().copy(
                platformName = "GitHub",
                account = "test@example.com",
                password = "password",
                isLoading = true
            ),
            onFormChange = {},
            onTogglePasswordVisibility = {},
            onSave = {},
            onBack = {}
        )
    }
}
