package com.pluto.core.model

/**
 * ContentType — discriminator for movies vs series.
 * Used for deep links, history records, favorites, and notifications.
 */
enum class ContentType(val apiValue: String, val label: String) {
    MOVIE("movie", "Movie"),
    SERIES("serie", "Series");

    companion object {
        fun fromApiValue(value: String): ContentType? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

/**
 * DeepLink routes supported by PLUTO.
 *
 * Per Section 59 of the master spec:
 *   pluto://movie/{id}
 *   pluto://series/{id}
 *   pluto://series/{id}/season/{season}/episode/{episode}
 */
object PlutoDeepLinks {
    const val SCHEME = "pluto"
    const val HOST = "app"

    fun movie(id: Int): String = "$SCHEME://movie/$id"
    fun series(id: Int): String = "$SCHEME://series/$id"
    fun episode(seriesId: Int, season: Int, episode: Int): String =
        "$SCHEME://series/$seriesId/season/$season/episode/$episode"
}

/**
 * External player / downloader identifiers.
 *
 * Per Section 29-30 of the master spec: PLUTO offers to open content
 * in MX Player / VLC / KMPlayer, and to download via ADM / Browser /
 * System downloader. Each package is tried in order — if none are
 * installed, the user is shown a clean "not installed" state.
 */
object ExternalApps {
    val VIDEO_PLAYERS = listOf(
        Triple("MX Player (Free)", "com.mxtech.videoplayer.ad", "video/*"),
        Triple("MX Player (Pro)", "com.mxtech.videoplayer.pro", "video/*"),
        Triple("VLC", "org.videolan.vlc", "video/*"),
        Triple("KM Player", "com.kmplayer", "video/*"),
        Triple("KM Player Pro", "com.kmplayerpro", "video/*")
    )

    val DOWNLOADERS = listOf(
        Pair("ADM", "com.dv.adm"),
        Pair("ADM (Pay)", "com.dv.adm.pay"),
        Pair("1DM+", "com.idm.internet.download.manager.plus"),
        Pair("1DM", "com.idm.internet.download.manager")
    )
}
