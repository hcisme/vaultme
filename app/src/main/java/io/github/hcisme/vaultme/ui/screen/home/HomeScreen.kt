package io.github.hcisme.vaultme.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hcisme.vaultme.R
import io.github.hcisme.vaultme.components.CredentialItem
import io.github.hcisme.vaultme.navigation.navigateToAddCredential
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import io.github.hcisme.vaultme.ui.theme.VaultMeTheme
import io.github.hcisme.vaultme.utils.LocalNavController

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    val viewModel: HomeViewModel = viewModel()

    val credentials by viewModel.credentialsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    HomeContent(
        credentials = credentials,
        searchQuery = viewModel.searchQuery,
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onAddClick = { navController.navigateToAddCredential() },
        onItemClick = { /* TODO: 处理点击事件 */ },
        onRefreshClick = { /* TODO */ },
        onClearFocus = { focusManager.clearFocus() },
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    credentials: List<CredentialEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (CredentialEntity) -> Unit,
    onRefreshClick: () -> Unit,
    onClearFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClearFocus()
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .size(64.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "添加凭据",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Header(onRefreshClick = onRefreshClick)

            Spacer(modifier = Modifier.height(20.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(count = credentials.size)

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(credentials) { credential ->
                    CredentialItem(
                        platform = credential.platform,
                        username = credential.account,
                        iconColor = Color.Gray,
                        onClick = { onItemClick(credential) }
                    )
                }
            }
        }
    }
}

@Composable
fun Header(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "密码管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        IconButton(onClick = onRefreshClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        placeholder = {
            Text(
                text = "搜索账号或平台…",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        shape = RoundedCornerShape(30.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )
}

@Composable
fun SectionHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = null,
            tint = Color(0xFF757575),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "已保存的凭据 ($count)",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    VaultMeTheme {
        HomeContent(
            credentials = listOf(
                CredentialEntity(1, "Google", "alex.worker@gmail.com", "****"),
                CredentialEntity(2, "GitHub", "dev_pro_2024", "****")
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onAddClick = {},
            onItemClick = {},
            onRefreshClick = {},
            onClearFocus = {}
        )
    }
}
