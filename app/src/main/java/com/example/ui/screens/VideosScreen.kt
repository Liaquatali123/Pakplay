package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.SearchBarHeader
import com.example.ui.components.SortFilterBottomSheet
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: MainViewModel,
    onPlayVideo: (VideoEntity, List<VideoEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedVideoIds.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val folderSummaries by viewModel.folderSummaries.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var showSortFilterSheet by remember { mutableStateOf(false) }
    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var selectedVideoForPlaylist by remember { mutableStateOf<VideoEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val sortFilterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("videos_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                AnimatedVisibility(visible = isMultiSelectMode) {
                    MultiSelectActionBar(
                        selectedCount = selectedIds.size,
                        onSelectAll = { viewModel.selectAll(videos) },
                        onFavorite = { viewModel.favoriteSelectedVideos(isFavorite = false) },
                        onAddToPlaylist = {
                            if (videos.isNotEmpty()) {
                                selectedVideoForPlaylist = videos.firstOrNull { selectedIds.contains(it.id) }
                            }
                        },
                        onDelete = { viewModel.deleteSelectedVideos() },
                        onClose = { viewModel.clearSelection() }
                    )
                }

                if (!isMultiSelectMode) {
                    SearchBarHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        isGridView = isGridView,
                        onToggleView = { viewModel.toggleGridView() },
                        onOpenSortFilter = { showSortFilterSheet = true },
                        hasActiveFilters = filterOptions.folder != null || filterOptions.favoritesOnly
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (videos.isEmpty()) {
                if (isScanning) {
                    EmptyStateView(
                        icon = Icons.Default.VideoLibrary,
                        title = "Updating Library...",
                        description = "Scanning your device for video files in the background.",
                        actionButtonText = "",
                        onActionClick = {}
                    )
                } else {
                    EmptyStateView(
                        icon = Icons.Default.VideoLibrary,
                        title = if (searchQuery.isNotEmpty()) "No Matching Videos" else "No Videos Found",
                        description = if (searchQuery.isNotEmpty()) "Try adjusting your search terms or filters." else "Scan your device to find video files.",
                        actionButtonText = if (searchQuery.isNotEmpty()) "Clear Search" else "Scan Videos",
                        onActionClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.setSearchQuery("")
                                viewModel.resetFilters()
                            } else {
                                viewModel.scanMedia(force = true)
                            }
                        }
                    )
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                isGrid = true,
                                isSelected = selectedIds.contains(video.id),
                                isSelectionMode = isMultiSelectMode,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        viewModel.toggleSelection(video.id)
                                    } else {
                                        onPlayVideo(video, videos)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(video.id)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                onShare = { viewModel.shareVideo(context, video) },
                                onRename = { /* Handle rename */ },
                                onDelete = { viewModel.deleteVideo(video) },
                                onAddToPlaylist = { selectedVideoForPlaylist = video },
                                onShowInfo = { selectedVideoForInfo = video }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                isGrid = false,
                                isSelected = selectedIds.contains(video.id),
                                isSelectionMode = isMultiSelectMode,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        viewModel.toggleSelection(video.id)
                                    } else {
                                        onPlayVideo(video, videos)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelection(video.id)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                onShare = { viewModel.shareVideo(context, video) },
                                onRename = { /* Handle rename */ },
                                onDelete = { viewModel.deleteVideo(video) },
                                onAddToPlaylist = { selectedVideoForPlaylist = video },
                                onShowInfo = { selectedVideoForInfo = video }
                            )
                        }
                    }
                }
            }
        }
    }

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
            onPlay = { onPlayVideo(video, videos) },
            onShare = { viewModel.shareVideo(context, video) },
            onRename = { /* Rename */ },
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
                val targetIds = if (selectedIds.isNotEmpty()) selectedIds else setOf(video.id)
                viewModel.addVideosToPlaylist(plId, targetIds)
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
                        val targetIds = if (selectedIds.isNotEmpty()) selectedIds else setOf(v.id)
                        viewModel.addVideosToPlaylist(newId, targetIds)
                    }
                }
                showCreatePlaylistDialog = false
            }
        )
    }
}
