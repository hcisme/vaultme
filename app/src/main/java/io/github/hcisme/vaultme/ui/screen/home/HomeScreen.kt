package io.github.hcisme.vaultme.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hcisme.vaultme.R
import io.github.hcisme.vaultme.components.CredentialItem
import io.github.hcisme.vaultme.navigation.navigateToEditCredential
import io.github.hcisme.vaultme.utils.ColorUtils
import io.github.hcisme.vaultme.utils.LocalNavController

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    val viewModel = viewModel<HomeViewModel>()
    val credentials by viewModel.credentialsFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadInitial()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigateToEditCredential() },
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
        topBar = { HomeHeader() },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            HomeSearchBar()

            Spacer(modifier = Modifier.height(20.dp))

            HomeCredentialSectionHeader()

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(credentials, key = { it.uuid }) { credential ->
                    CredentialItem(
                        platform = credential.platform,
                        username = credential.account,
                        iconColor = ColorUtils.getPlatformColor(credential.platform),
                        onClick = {
                            navController.navigateToEditCredential(id = credential.id)
                        },
                        onDelete = {
                            viewModel.deleteCredential(credential)
                        }
                    )
                }
            }
        }
    }
}
