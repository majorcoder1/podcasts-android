package com.podcasts.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.podcasts.app.ui.activity.ActivityScreen
import com.podcasts.app.ui.components.MiniPlayer
import com.podcasts.app.ui.explore.ExploreScreen
import com.podcasts.app.ui.home.HomeScreen
import com.podcasts.app.ui.player.PlayerScreen
import com.podcasts.app.ui.player.PlayerViewModel
import com.podcasts.app.ui.settings.SettingsScreen
import com.podcasts.app.ui.show.ShowScreen

private enum class TopLevel(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE(Routes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    ACTIVITY(Routes.ACTIVITY, "Activity", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
}

/**
 * Three bottom tabs — Home, Explore, Activity — with the mini player docked
 * above them, exactly the shell the original app used.
 */
@Composable
fun PodcastsApp(
    shortcut: Shortcut? = null,
    onShortcutHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()

    // Launcher shortcuts. Resume starts the last episode rather than just
    // opening a screen, which is the whole point of that one.
    LaunchedEffect(shortcut) {
        when (shortcut) {
            null -> return@LaunchedEffect
            Shortcut.RESUME -> {
                playerViewModel.resumeLastPlayed()
                navController.navigate(Routes.PLAYER)
            }
            Shortcut.NEW_EPISODES -> navController.navigate(Routes.HOME)
            Shortcut.QUEUE -> navController.navigate(Routes.ACTIVITY)
            Shortcut.EXPLORE -> navController.navigate(Routes.EXPLORE)
        }
        onShortcutHandled()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showChrome = TopLevel.entries.any { top ->
        currentDestination?.hierarchy?.any { it.route == top.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showChrome) {
                Column {
                    MiniPlayer(
                        nowPlaying = nowPlaying,
                        onExpand = { navController.navigate(Routes.PLAYER) },
                        onPlayPause = playerViewModel::playPause,
                        onSkipForward = playerViewModel::skipForward,
                    )
                    BottomBar(navController, currentDestination)
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    playerViewModel = playerViewModel,
                    onShowClick = { navController.navigate(Routes.show(it)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onSearchClick = { navController.navigate(Routes.EXPLORE) },
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onShowClick = { navController.navigate(Routes.show(it)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.ACTIVITY) {
                ActivityScreen(
                    playerViewModel = playerViewModel,
                    onShowClick = { navController.navigate(Routes.show(it)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                route = Routes.SHOW,
                arguments = listOf(navArgument("feedUrl") { type = NavType.StringType }),
            ) { entry ->
                ShowScreen(
                    playerViewModel = playerViewModel,
                    onBack = navController::popBackStack,
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(
                    playerViewModel = playerViewModel,
                    onCollapse = navController::popBackStack,
                    onShowClick = { navController.navigate(Routes.show(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = navController::popBackStack)
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
) {
    NavigationBar {
        TopLevel.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}
