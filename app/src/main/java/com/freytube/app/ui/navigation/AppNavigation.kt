package com.freytube.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.freytube.app.ui.components.BottomNavBar
import com.freytube.app.ui.screens.*
import com.freytube.app.viewmodel.*

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Search : Screen("search", "Search")
    data object Downloads : Screen("downloads", "Downloads")
    data object Settings : Screen("settings", "Settings")
    data object Player : Screen("player/{videoId}", "Player") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    data object Channel : Screen("channel/{channelId}", "Channel") {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    initialVideoId: String? = null
) {
    val playerViewModel: PlayerViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Downloads.route,
        Screen.Settings.route
    )

    // Navigate to video if launched from intent
    LaunchedEffect(initialVideoId) {
        initialVideoId?.let {
            navController.navigate(Screen.Player.createRoute(it))
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { 100 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { 100 },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("videoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                PlayerScreen(
                    videoId = videoId,
                    playerViewModel = playerViewModel,
                    onBackClick = { navController.popBackStack() },
                    onVideoClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id)) {
                            popUpTo(Screen.Player.route) { inclusive = true }
                        }
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }

            composable(
                route = Screen.Channel.route,
                arguments = listOf(
                    navArgument("channelId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                ChannelScreen(
                    channelId = channelId,
                    onBackClick = { navController.popBackStack() },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    }
                )
            }
        }
    }
}
