package com.pluto.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pluto.feature.auth.AuthScreen
import com.pluto.feature.details.DetailsScreen
import com.pluto.feature.downloads.DownloadsScreen
import com.pluto.feature.favorites.FavoritesScreen
import com.pluto.feature.history.HistoryScreen
import com.pluto.feature.home.HomeScreen
import com.pluto.feature.notifications.NotificationsScreen
import com.pluto.feature.player.PlayerScreen
import com.pluto.feature.search.SearchScreen
import com.pluto.feature.settings.SettingsScreen
import com.pluto.feature.splash.SplashScreen

/**
 * PLUTOApp — root composable hosting the entire nav graph.
 *
 * Wires up:
 *   - NavHost with all feature destinations (Splash, Auth, Home, Search,
 *     Details, Player, Downloads, Favorites, History, Notifications, Settings)
 *   - Floating glass navigation bar (shown on top-level destinations only)
 *   - Deep links for pluto://movie/{id}, pluto://series/{id}, etc.
 *
 * The Player routes accept the content id + resume position (ms) so the
 * player can restore the user's previous position (per the spec's
 * "Smart Resume" requirement).
 */
@Composable
fun PLUTOApp(
    pipController: PipController? = null,
    isInPip: Boolean = false,
    onNavControllerReady: ((androidx.navigation.NavController) -> Unit)? = null
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Publish the nav controller to MainActivity so it can dispatch deep links.
    LaunchedEffect(navController) {
        onNavControllerReady?.invoke(navController)
    }

    val topLevelRoutes = remember {
        setOf(
            PlutoRoute.HOME, PlutoRoute.SEARCH, PlutoRoute.DOWNLOADS,
            PlutoRoute.FAVORITES, PlutoRoute.SETTINGS, PlutoRoute.HISTORY,
            PlutoRoute.NOTIFICATIONS
        )
    }
    val showBottomBar = currentRoute in topLevelRoutes && !isInPip

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = PlutoRoute.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Top-level destinations ───────────────────────────────────
                composable(PlutoRoute.HOME) {
                    HomeScreen(
                        onOpenMovie = { id -> navController.navigate(PlutoRoute.movieDetails(id)) },
                        onOpenSeries = { id -> navController.navigate(PlutoRoute.seriesDetails(id)) },
                        onOpenSearch = { navController.navigate(PlutoRoute.SEARCH) },
                        onOpenContinueWatching = { navController.navigate(PlutoRoute.HISTORY) }
                    )
                }
                composable(
                    route = PlutoRoute.SEARCH,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://search" })
                ) {
                    SearchScreen(
                        onOpenMovie = { id -> navController.navigate(PlutoRoute.movieDetails(id)) },
                        onOpenSeries = { id -> navController.navigate(PlutoRoute.seriesDetails(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PlutoRoute.DOWNLOADS,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://downloads" })
                ) {
                    DownloadsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PlutoRoute.FAVORITES,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://favorites" })
                ) {
                    FavoritesScreen(
                        onOpenMovie = { id -> navController.navigate(PlutoRoute.movieDetails(id)) },
                        onOpenSeries = { id -> navController.navigate(PlutoRoute.seriesDetails(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PlutoRoute.HISTORY,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://history" })
                ) {
                    HistoryScreen(
                        onOpenMovie = { id -> navController.navigate(PlutoRoute.movieDetails(id)) },
                        onOpenSeries = { id -> navController.navigate(PlutoRoute.seriesDetails(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PlutoRoute.NOTIFICATIONS,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://notifications" })
                ) {
                    NotificationsScreen(
                        onOpenSeries = { id -> navController.navigate(PlutoRoute.seriesDetails(id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PlutoRoute.SETTINGS,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://settings" })
                ) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }

                // ── Auth / Splash ────────────────────────────────────────────
                composable(PlutoRoute.SPLASH) {
                    SplashScreen(
                        onCompleted = { navController.navigate(PlutoRoute.HOME) {
                            popUpTo(PlutoRoute.SPLASH) { inclusive = true }
                        } }
                    )
                }
                composable(PlutoRoute.AUTH) {
                    AuthScreen(
                        onAuthenticated = { navController.navigate(PlutoRoute.HOME) {
                            popUpTo(PlutoRoute.AUTH) { inclusive = true }
                        } },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Details ──────────────────────────────────────────────────
                composable(
                    route = PlutoRoute.MOVIE_DETAILS,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://movie/{movieId}" }),
                    arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                ) { entry ->
                    val movieId = entry.arguments?.getInt("movieId") ?: 0
                    DetailsScreen(
                        contentType = "movie",
                        contentId = movieId,
                        onPlay = { resumeMs ->
                            navController.navigate(PlutoRoute.playerForMovie(movieId, resumeMs))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PlutoRoute.SERIES_DETAILS,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluto://series/{seriesId}" }),
                    arguments = listOf(navArgument("seriesId") { type = NavType.IntType })
                ) { entry ->
                    val seriesId = entry.arguments?.getInt("seriesId") ?: 0
                    DetailsScreen(
                        contentType = "series",
                        contentId = seriesId,
                        onPlay = { resumeMs ->
                            // For series without episode info, just open the details screen
                            // (the user picks an episode there).
                        },
                        onPlayEpisode = { epId, season, ep ->
                            navController.navigate(PlutoRoute.playerForEpisode(seriesId, season, ep, epId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                // ── Episode deep-link route (notification taps land here) ──────────
                // Notifications deep-link to pluto://series/{id}/season/{s}/episode/{e}.
                // Route to the series details screen — the user picks the episode there.
                composable(
                    route = PlutoRoute.EPISODE_DETAILS,
                    deepLinks = listOf(navDeepLink {
                        uriPattern = "pluto://series/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}"
                    }),
                    arguments = listOf(
                        navArgument("seriesId") { type = NavType.IntType },
                        navArgument("seasonNumber") { type = NavType.IntType },
                        navArgument("episodeNumber") { type = NavType.IntType }
                    )
                ) { entry ->
                    val seriesId = entry.arguments?.getInt("seriesId") ?: 0
                    // Episode deep links land on the series details page; the user
                    // taps the specific episode from there. This avoids needing a
                    // dedicated episode-details screen and matches the user's
                    // mental model ("open the series, find the episode").
                    DetailsScreen(
                        contentType = "series",
                        contentId = seriesId,
                        onPlay = { /* no-op for series */ },
                        onPlayEpisode = { epId, season, ep ->
                            navController.navigate(PlutoRoute.playerForEpisode(seriesId, season, ep, epId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── Player ───────────────────────────────────────────────────
                composable(
                    route = PlutoRoute.PLAYER_MOVIE,
                    arguments = listOf(
                        navArgument("movieId") { type = NavType.IntType },
                        navArgument("positionMs") { type = NavType.LongType; defaultValue = 0L }
                    )
                ) { entry ->
                    val movieId = entry.arguments?.getInt("movieId") ?: 0
                    val positionMs = entry.arguments?.getLong("positionMs") ?: 0L
                    PlayerScreen(
                        contentType = "movie",
                        contentId = movieId,
                        episodeId = null,
                        seasonNumber = null,
                        episodeNumber = null,
                        startPositionMs = positionMs,
                        pipController = pipController,
                        onExit = { navController.popBackStack() },
                        onOpenSeries = { /* not applicable for movie playback */ }
                    )
                }
                composable(
                    route = PlutoRoute.PLAYER_EPISODE,
                    arguments = listOf(
                        navArgument("seriesId") { type = NavType.IntType },
                        navArgument("episodeId") { type = NavType.IntType },
                        navArgument("seasonNumber") { type = NavType.IntType },
                        navArgument("episodeNumber") { type = NavType.IntType },
                        navArgument("positionMs") { type = NavType.LongType; defaultValue = 0L }
                    )
                ) { entry ->
                    val seriesId = entry.arguments?.getInt("seriesId") ?: 0
                    val episodeId = entry.arguments?.getInt("episodeId") ?: 0
                    val seasonNumber = entry.arguments?.getInt("seasonNumber") ?: 1
                    val episodeNumber = entry.arguments?.getInt("episodeNumber") ?: 1
                    val positionMs = entry.arguments?.getLong("positionMs") ?: 0L
                    PlayerScreen(
                        contentType = "series",
                        contentId = seriesId,
                        episodeId = episodeId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        startPositionMs = positionMs,
                        pipController = pipController,
                        onExit = { navController.popBackStack() },
                        onOpenSeries = { id -> navController.navigate(PlutoRoute.seriesDetails(id)) }
                    )
                }
            }
        }
    }
}
