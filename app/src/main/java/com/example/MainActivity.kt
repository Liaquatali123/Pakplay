package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.MiniPlayerBar
import com.example.ui.navigation.Screen
import com.example.ui.navigation.UriPathHelper
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PakPlayTheme
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home")
    data object Videos : BottomNavItem(Screen.Videos.route, "Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_videos")
    data object Folders : BottomNavItem(Screen.Folders.route, "Folders", Icons.Filled.Folder, Icons.Outlined.Folder, "nav_folders")
    data object Playlists : BottomNavItem(Screen.Playlists.route, "Playlists", Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay, "nav_playlists")
    data object Favorites : BottomNavItem(Screen.Favorites.route, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites")
    data object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PakPlayApp(
                mainViewModel = mainViewModel,
                playerViewModel = playerViewModel
            )
        }
    }
}

@Composable
fun PakPlayApp(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel
) {
    PakPlayTheme {
        val navController = rememberNavController()

        // Request only video-library access. Android 14+ supports either full
        // video access or the user's selected-video access; audio/storage-write
        // permissions are not needed for a local video player.
        val videoPermissions = remember {
            when {
                Build.VERSION.SDK_INT >= 34 -> arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.values.any { it }) {
                mainViewModel.scanMedia()
            }
        }

        LaunchedEffect(Unit) {
            val hasFullVideoAccess = ContextCompat.checkSelfPermission(
                navController.context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_VIDEO
                else Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val hasSelectedVideoAccess = Build.VERSION.SDK_INT >= 34 &&
                ContextCompat.checkSelfPermission(
                    navController.context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED

            if (!hasFullVideoAccess && !hasSelectedVideoAccess) {
                permissionLauncher.launch(videoPermissions)
            } else {
                mainViewModel.scanMedia()
            }
        }

        MainAppScaffold(
            navController = navController,
            mainViewModel = mainViewModel,
            playerViewModel = playerViewModel
        )
    }
}


@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Videos,
        BottomNavItem.Playlists,
        BottomNavItem.Settings
    )

    val isPlayerScreen = currentRoute?.startsWith("player") == true || currentRoute == Screen.Player.route
    val isSplashScreen = currentRoute == Screen.Splash.route

    val isMainTab = currentRoute in listOf(
        Screen.Home.route,
        Screen.Videos.route,
        Screen.Folders.route,
        Screen.Playlists.route,
        Screen.Favorites.route,
        Screen.Settings.route
    )
    val showBottomBar = isMainTab

    val onPlayVideo = { video: VideoEntity, playlist: List<VideoEntity> ->
        playerViewModel.playVideo(video, playlist)
        navController.navigate(Screen.Player.createRoute(video.id))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (isPlayerScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
        bottomBar = {
            if (!isPlayerScreen && !isSplashScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                            )
                        )
                ) {
                    // Mini Player Bar: always visible above system navigation/IME when playing
                    MiniPlayerBar(
                        playerViewModel = playerViewModel,
                        onExpandToFullScreen = {
                            val currentVidId = playerViewModel.playerUiState.value.currentVideoId
                            if (currentVidId != 0L) {
                                navController.navigate(Screen.Player.createRoute(currentVidId))
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shadowElevation = 8.dp,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bottom_nav_bar")
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                fontSize = 10.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyanNeon,
                                            selectedTextColor = CyanNeon,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            indicatorColor = CyanNeon.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = mainViewModel,
                    onPlayVideo = { video -> onPlayVideo(video, emptyList()) },
                    onNavigateToVideos = { navController.navigate(Screen.Videos.route) },
                    onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                    onNavigateToFolderDetail = { folder ->
                        navController.navigate(Screen.FolderDetail.createRoute(folder))
                    },
                    onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) }
                )
            }

            // Videos Library Screen
            composable(Screen.Videos.route) {
                VideosScreen(
                    viewModel = mainViewModel,
                    onPlayVideo = onPlayVideo
                )
            }

            // Folders Screen
            composable(Screen.Folders.route) {
                FoldersScreen(
                    viewModel = mainViewModel,
                    onFolderClick = { folder ->
                        navController.navigate(Screen.FolderDetail.createRoute(folder))
                    }
                )
            }

            // Folder Detail Screen
            composable(
                route = Screen.FolderDetail.route,
                arguments = listOf(navArgument("folderName") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedFolder = backStackEntry.arguments?.getString("folderName") ?: ""
                val folderName = UriPathHelper.decode(encodedFolder)
                FolderDetailScreen(
                    folderName = folderName,
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo
                )
            }

            // Playlists Screen
            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    viewModel = mainViewModel,
                    onPlaylistClick = { playlistId ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }

            // Playlist Detail Screen
            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    viewModel = mainViewModel,
                    onBack = { navController.popBackStack() },
                    onPlayVideo = onPlayVideo
                )
            }

            // Favorites Screen
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = mainViewModel,
                    onPlayVideo = onPlayVideo
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = mainViewModel)
            }

            // Video Player Screen
            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                PlayerScreen(
                    videoId = videoId,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
