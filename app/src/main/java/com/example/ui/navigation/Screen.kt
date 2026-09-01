package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Videos : Screen("videos")
    data object Folders : Screen("folders")
    data object FolderDetail : Screen("folder_detail/{folderName}") {
        fun createRoute(folderName: String) = "folder_detail/${UriPathHelper.encode(folderName)}"
    }
    data object Playlists : Screen("playlists")
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: Long) = "player/$videoId"
    }
}

object UriPathHelper {
    fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
    fun decode(value: String): String = java.net.URLDecoder.decode(value, "UTF-8")
}
