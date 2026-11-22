package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderCard
import com.example.ui.components.SearchBarHeader
import com.example.ui.components.SortFilterBottomSheet
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlayVideo: (VideoEntity) -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToFolderDetail: (String) -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideosRaw.collectAsStateWithLifecycle(emptyList())
    val folderSummaries by viewModel.folderSummaries.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredVideos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()

    var showSortFilterSheet by remember { mutableStateOf(false) }
    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var selectedVideoForPlaylist by remember { mutableStateOf<VideoEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var videoToRename by remember { mutableStateOf<VideoEntity?>(null) }

    val sortFilterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header: Brand & Scan button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(listOf(CyanNeon, PurpleElectric)),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "VeyraPlay",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${allVideos.size} videos on device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isScanning) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CyanNeon.copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanNeon
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Updating library...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CyanNeon
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            IconButton(
                                onClick = { viewModel.scanMedia(force = true) },
                                modifier = Modifier.testTag("scan_media_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan Media",
                                    tint = CyanNeon
                                )
                            }
                        }
                    }
                }
            }

            // Search Header
            item {
                SearchBarHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    isGridView = isGridView,
                    onToggleView = {},
                    showViewToggle = false,
                    onOpenSortFilter = { showSortFilterSheet = true },
                    placeholder = "Search videos..."
                )
            }

            // Instant In-Place Search Results (when searching)
            if (searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Results (${filteredVideos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = { viewModel.setSearchQuery("") }) {
                            Text("Clear", color = CyanNeon, fontSize = 13.sp)
                        }
                    }
                }

                if (filteredVideos.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No Matching Videos",
                            description = "No videos match \"$searchQuery\". Try checking your spelling.",
                            actionButtonText = "Clear Search",
                            onActionClick = { viewModel.setSearchQuery("") }
                        )
                    }
                } else {
                    items(filteredVideos, key = { "home_search_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = false,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onPlayVideo(video) },
                            onLongClick = {},
                            onFavoriteToggle = { viewModel.toggleFavorite(video) },
                            onShare = { viewModel.shareVideo(context, video) },
                            onRename = { videoToRename = video },
                            onDelete = { viewModel.deleteVideo(video) },
                            onAddToPlaylist = { selectedVideoForPlaylist = video },
                            onShowInfo = { selectedVideoForInfo = video },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Continue Watching Carousel
                if (continueWatching.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Continue Watching",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${continueWatching.size} in progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanNeon,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(continueWatching, key = { it.id }) { video ->
                                    ContinueWatchingCard(
                                        video = video,
                                        onClick = { onPlayVideo(video) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Category Cards Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickCategoryButton(
                            title = "Videos",
                            count = "${allVideos.size}",
                            icon = Icons.Default.VideoLibrary,
                            tint = CyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToVideos
                        )
                        QuickCategoryButton(
                            title = "Folders",
                            count = "${folderSummaries.size}",
                            icon = Icons.Default.Folder,
                            tint = PurpleElectric,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFolders
                        )
                        QuickCategoryButton(
                            title = "Playlists",
                            count = "${playlists.size}",
                            icon = Icons.Default.PlaylistPlay,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPlaylists
                        )
                        QuickCategoryButton(
                            title = "Favorites",
                            count = "${allVideos.count { it.isFavorite }}",
                            icon = Icons.Default.Favorite,
                            tint = Color(0xFFFF3366),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFavorites
                        )
                    }
                }

                // Recently Played Section
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Played",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onNavigateToVideos) {
                                Text("See All", color = CyanNeon, fontSize = 13.sp)
                            }
                        }
                    }

                    items(recentlyPlayed.take(4), key = { "recent_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = false,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onPlayVideo(video) },
                            onLongClick = {},
                            onFavoriteToggle = { viewModel.toggleFavorite(video) },
                            onShare = { viewModel.shareVideo(context, video) },
                            onRename = { videoToRename = video },
                            onDelete = { viewModel.deleteVideo(video) },
                            onAddToPlaylist = { selectedVideoForPlaylist = video },
                            onShowInfo = { selectedVideoForInfo = video },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Folders Preview
                if (folderSummaries.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Folders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onNavigateToFolders) {
                                Text("View All", color = CyanNeon, fontSize = 13.sp)
                            }
                        }
                    }

                    items(folderSummaries.take(3), key = { "folder_${it.folder}" }) { summary ->
                        FolderCard(
                            folderSummary = summary,
                            onClick = { onNavigateToFolderDetail(summary.folder) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Recently Added / Library preview
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Added",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onNavigateToVideos) {
                                Text("All Videos", color = CyanNeon, fontSize = 13.sp)
                            }
                        }
                    }

                    items(recentlyAdded.take(6), key = { "added_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = false,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onPlayVideo(video) },
                            onLongClick = {},
                            onFavoriteToggle = { viewModel.toggleFavorite(video) },
                            onShare = { viewModel.shareVideo(context, video) },
                            onRename = { videoToRename = video },
                            onDelete = { viewModel.deleteVideo(video) },
                            onAddToPlaylist = { selectedVideoForPlaylist = video },
                            onShowInfo = { selectedVideoForInfo = video },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                } else if (allVideos.isEmpty() && isScanning) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                    color = CyanNeon
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Indexing your video library...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (allVideos.isEmpty() && !isScanning) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.VideoLibrary,
                            title = "No Videos Found",
                            description = "Scan your device to find and play local video files.",
                            actionButtonText = "Scan Videos",
                            onActionClick = { viewModel.scanMedia(force = true) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSortFilterSheet) {
        SortFilterBottomSheet(
            sheetState = sortFilterSheetState,
            currentSort = sortType,
            currentFilter = filterOptions,
            availableFolders = folderSummaries.map { it.folder },
            onApply = { sort, filter ->
                viewModel.setSortType(sort)
                viewModel.setFilterOptions(filter)
            },
            onDismiss = { showSortFilterSheet = false }
        )
    }

    selectedVideoForInfo?.let { video ->
        VideoInfoDialog(
            video = video,
            onDismiss = { selectedVideoForInfo = null },
            onPlay = { onPlayVideo(video) },
            onShare = { viewModel.shareVideo(context, video) },
            onRename = { videoToRename = video },
            onFavoriteToggle = { viewModel.toggleFavorite(video) },
            onAddToPlaylist = { selectedVideoForPlaylist = video },
            onDelete = { viewModel.deleteVideo(video) }
        )
    }

    selectedVideoForPlaylist?.let { video ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { selectedVideoForPlaylist = null },
            onSelectPlaylist = { plId ->
                viewModel.addVideosToPlaylist(plId, setOf(video.id))
            },
            onCreateNewPlaylist = { showCreatePlaylistDialog = true }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name) { newId ->
                    selectedVideoForPlaylist?.let { v ->
                        viewModel.addVideosToPlaylist(newId, setOf(v.id))
                    }
                }
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
fun QuickCategoryButton(
    title: String,
    count: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = count,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}
