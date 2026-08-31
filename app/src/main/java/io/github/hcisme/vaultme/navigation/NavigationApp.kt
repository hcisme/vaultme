package io.github.hcisme.vaultme.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.hcisme.vaultme.ui.screen.edit.CredentialEditScreen
import io.github.hcisme.vaultme.ui.screen.home.HomeScreen
import io.github.hcisme.vaultme.ui.screen.settings.SettingsScreen
import io.github.hcisme.vaultme.utils.LocalNavController

@Composable
fun NavigationApp(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current

    // 新页面从右侧滑入（前进动画）
    val slideInFromRight = remember {
        slideInHorizontally(
            animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
            initialOffsetX = { it }
        )
    }
    // 页面向右侧滑出（前进退出动画）
    val slideOutToRight = remember {
        slideOutHorizontally(
            animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
            targetOffsetX = { it }
        )
    }
    // 返回时页面从左侧滑入（后退进入动画）
    val slideInFromLeft = remember {
        slideInHorizontally(
            animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
            initialOffsetX = { -it }
        )
    }
    // 返回时页面向左侧滑出（后退退出动画）
    val slideOutToLeft = remember {
        slideOutHorizontally(
            animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
            targetOffsetX = { -it }
        )
    }

    NavHost(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        navController = navController,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        startDestination = NavigationName.HOME_PAGE
    ) {
        composable(
            route = NavigationName.HOME_PAGE,
            popEnterTransition = { slideInFromLeft },
            exitTransition = { slideOutToLeft },
            popExitTransition = null
        ) {
            HomeScreen()
        }

        composable(
            route = "${NavigationName.EDIT_CREDENTIAL_PAGE}?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { slideInFromRight },
            popExitTransition = { slideOutToRight }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            CredentialEditScreen(id = id)
        }

        composable(
            route = NavigationName.SETTINGS_PAGE,
            enterTransition = { slideInFromRight },
            popExitTransition = { slideOutToRight }
        ) {
            SettingsScreen()
        }
    }
}
