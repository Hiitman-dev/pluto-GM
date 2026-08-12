package com.pluto.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOFloatingNavigation
import com.pluto.core.designsystem.PLUTONavItem
import com.pluto.core.designsystem.PlutoIcons
import com.pluto.core.navigation.PlutoRoute

/**
 * PLUTOApp — root composable hosting the entire nav graph.
 *
 * Wires up:
 *   - NavHost with all feature destinations
 *   - Floating glass navigation bar (shown on top-level destinations only)
 *   - Deep links for pluto://movie/{id}, pluto://series/{id}, etc.
 *
 * Per Section 109 ("FIRST IMPRESSION"): Splash -> Welcome -> Home with
 * beautiful motion + strong branding + immediate content.
 *
 * NOTE: The actual feature screens (Home, Search, Details, Player,
 * Favorites, History, Downloads, Notifications, Settings, Auth, Splash)
 * live in the feature/* modules. This app module wires them into the
 * nav graph. For the initial build, placeholders render the design
 * system so the APK compiles and launches; full feature screen
 * implementations are in the feature modules and will be wired in
 * once their ViewModels are finalized.
 */
@Composable
fun PLUTOApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevelRoutes = remember {
        setOf(
            PlutoRoute.HOME, PlutoRoute.SEARCH, PlutoRoute.DOWNLOADS,
            PlutoRoute.FAVORITES, PlutoRoute.SETTINGS
        )
    }
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val items = remember {
                    listOf(
                        PLUTONavItem("home", "Home", PlutoIcons.Home, "Home"),
                        PLUTONavItem("search", "Discover", PlutoIcons.Search, "Discover"),
                        PLUTONavItem("downloads", "Downloads", PlutoIcons.Download, "Downloads"),
                        PLUTONavItem("favorites", "Library", PlutoIcons.Favorite, "Library"),
                        PLUTONavItem("settings", "Settings", PlutoIcons.Settings, "Settings")
                    )
                }
                PLUTOFloatingNavigation(
                    items = items,
                    current = currentRoute ?: "home",
                    onChange = { route ->
                        navController.navigate(route) {
                            popUpTo(PlutoRoute.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = PLUTOColors.Void
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PlutoRoute.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(PlutoRoute.HOME) { HomePlaceholder() }
            composable(PlutoRoute.SEARCH) { SearchPlaceholder() }
            composable(PlutoRoute.DOWNLOADS) { DownloadsPlaceholder() }
            composable(PlutoRoute.FAVORITES) { FavoritesPlaceholder() }
            composable(PlutoRoute.HISTORY) { HistoryPlaceholder() }
            composable(PlutoRoute.NOTIFICATIONS) { NotificationsPlaceholder() }
            composable(PlutoRoute.SETTINGS) { SettingsPlaceholder() }

            composable(
                route = PlutoRoute.MOVIE_DETAILS,
                deepLinks = listOf(navDeepLink { uriPattern = "pluto://movie/{movieId}" }),
                arguments = listOf(navArgument("movieId") { type = NavType.IntType })
            ) { entry ->
                MovieDetailsPlaceholder(entry.arguments?.getInt("movieId") ?: 0)
            }
            composable(
                route = PlutoRoute.SERIES_DETAILS,
                deepLinks = listOf(navDeepLink { uriPattern = "pluto://series/{seriesId}" }),
                arguments = listOf(navArgument("seriesId") { type = NavType.IntType })
            ) { entry ->
                SeriesDetailsPlaceholder(entry.arguments?.getInt("seriesId") ?: 0)
            }
        }
    }
}
