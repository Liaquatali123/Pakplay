package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderSortBottomSheet
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SortType
import com.example.util.EpisodeSorter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayVideo: (VideoEntity, List<VideoEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allVideos by viewModel.allVideosRaw.collectAsStateWithLifecycle(emptyList())
    val folderVideos = remember(allVideos, folderName) {
        allVideos.filter { it.folder.equals(folderName, ignoreCase = true) }
    }
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var folderSort by remember { mutableStateOf(SortType.EPISODE_ASC) }
    var showSortSheet by remember { mutableStateOf(false) }

    val sortedFolderVideos = remember(folderVideos, folderSort) {
        when (folderSort) {
            SortType.EPISODE_ASC -> folderVideos.sortedWith { a, b -> EpisodeSorter.compare(a.title, b.title) }
            SortType.EPISODE_DESC -> folderVideos.sortedWith { a, b -> EpisodeSorter.compare(b.title, a.title) }
            SortType.NAME_ASC -> folderVideos.sortedBy { it.title.lowercase() }
            SortType.NAME_DESC -> folderVideos.sortedByDescending { it.title.lowercase() }
            SortType.DATE_DESC -> folderVideos.sortedByDescending { it.dateAdded }
            SortType.DATE_ASC -> folderVideos.sortedBy { it.dateAdded }
            SortType.DURATION_DESC -> folderVideos.sortedByDescending { it.duration }
            SortType.DURATION_ASC -> folderVideos.sortedBy { it.duration }
            SortType.SIZE_DESC -> folderVideos.sortedByDescending { it.size }
            SortType.SIZE_ASC -> folderVideos.sortedBy { it.size }
        }
    }

    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var selectedVideoForPlaylist by remember { mutableStateOf<VideoEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("folder_detail_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folderName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${sortedFolderVideos.size} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.testTag("sort_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Options",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Play All & Shuffle Play Buttons
            if (sortedFolderVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (sortedFolderVideos.isNotEmpty()) {
                                onPlayVideo(sortedFolderVideos.first(), sortedFolderVideos)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("play_all_folder_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (sortedFolderVideos.isNotEmpty()) {
                                val shuffled = sortedFolderVideos.shuffled()
                                onPlayVideo(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("shuffle_folder_button")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }

                // Quick Sort Indicator Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sequence Order",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        onClick = { showSortSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("folder_sort_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            val sortLabel = when (folderSort) {
                                SortType.EPISODE_ASC -> "Episode: 1 → 50"
                                SortType.EPISODE_DESC -> "Episode: 50 → 1"
                                SortType.NAME_ASC -> "Name: A to Z"
                                SortType.NAME_DESC -> "Name: Z to A"
                                SortType.DATE_DESC -> "Date: Newest"
                                SortType.DATE_ASC -> "Date: Oldest"
                                SortType.DURATION_DESC -> "Duration: Longest"
                                SortType.DURATION_ASC -> "Duration: Shortest"
                                SortType.SIZE_DESC -> "Size: Largest"
                                SortType.SIZE_ASC -> "Size: Smallest"
                            }
                            Text(
                                text = sortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanNeon
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (sortedFolderVideos.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.VideoLibrary,
                        title = "Folder is Empty",
                        description = "No video files found in $folderName.",
                        actionButtonText = "Back to Folders",
                        onActionClick = onBack
                    )
                } else {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 148.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sortedFolderVideos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGrid = true,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = { onPlayVideo(video, sortedFolderVideos) },
                                    onLongClick = {},
                                    onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                    onShare = { viewModel.shareVideo(context, video) },
                                    onRename = {},
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
                            items(sortedFolderVideos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGrid = false,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = { onPlayVideo(video, sortedFolderVideos) },
                                    onLongClick = {},
                                    onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                    onShare = { viewModel.shareVideo(context, video) },
                                    onRename = {},
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
    }

    if (showSortSheet) {
        FolderSortBottomSheet(
            currentSort = folderSort,
            onSortSelected = { folderSort = it },
            onDismiss = { showSortSheet = false }
        )
    }

    selectedVideoForInfo?.let { video ->
        VideoInfoDialog(
            video = video,
            onDismiss = { selectedVideoForInfo = null },
            onPlay = { onPlayVideo(video, sortedFolderVideos) },
            onShare = { viewModel.shareVideo(context, video) },
            onRename = {},
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
