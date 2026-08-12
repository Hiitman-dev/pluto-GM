package com.pluto.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * PlutoRoute — type-safe navigation routes.
 *
 * Implements Section 81 ("NAVIGATION") + Section 59 ("DEEP LINKS")
 * of the master spec.
 *
 * Primary destinations: Home, Discover/Search, Downloads, Favorites/Library,
 * Profile/Settings.
 *
 * Deep links (Section 59):
 *   pluto://movie/{id}
 *   pluto://series/{id}
 *   pluto://series/{id}/season/{season}/episode/{episode}
 */
object PlutoRoute {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"

    const val HOME = "home"
    const val SEARCH = "search"
    const val DOWNLOADS = "downloads"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    // Detail screens with arguments
    const val MOVIE_DETAILS = "movie/{movieId}"
    const val SERIES_DETAILS = "series/{seriesId}"
    const val EPISODE_DETAILS = "series/{seriesId}/season/{seasonNumber}/episode/{episodeNumber}"

    const val PLAYER = "player"

    fun movieDetails(id: Int) = "movie/$id"
    fun seriesDetails(id: Int) = "series/$id"
    fun episodeDetails(seriesId: Int, season: Int, episode: Int) =
        "series/$seriesId/season/$season/episode/$episode"
}

/**
 * Top-level destinations shown in the floating navigation.
 */
enum class PlutoTopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    HOME(PlutoRoute.HOME, "Home", Icons.Outlined.Home, "Home"),
    SEARCH(PlutoRoute.SEARCH, "Discover", Icons.Outlined.Search, "Discover"),
    DOWNLOADS(PlutoRoute.DOWNLOADS, "Downloads", Icons.Outlined.Download, "Downloads"),
    FAVORITES(PlutoRoute.FAVORITES, "Library", Icons.Outlined.FavoriteBorder, "Library"),
    SETTINGS(PlutoRoute.SETTINGS, "Settings", Icons.Outlined.Settings, "Settings")
}
