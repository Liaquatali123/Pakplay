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
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VideoEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayVideo: (VideoEntity, List<VideoEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlistVideos by viewModel.repository.getVideosInPlaylist(playlistId).collectAsStateWithLifecycle(emptyList())
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("playlist_detail_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Playlist Details",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${playlistVideos.size} videos",
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
            if (playlistVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (playlistVideos.isNotEmpty()) {
                                onPlayVideo(playlistVideos.first(), playlistVideos)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("play_all_playlist_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (playlistVideos.isNotEmpty()) {
                                val shuffled = playlistVideos.shuffled()
                                onPlayVideo(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("shuffle_playlist_button")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (playlistVideos.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PlaylistPlay,
                        title = "Playlist is Empty",
                        description = "Add videos to this playlist from your library or folders.",
                        actionButtonText = "Back",
                        onActionClick = onBack
                    )
                } else {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 165.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(playlistVideos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGrid = true,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = { onPlayVideo(video, playlistVideos) },
                                    onLongClick = {},
                                    onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                    onShare = { viewModel.shareVideo(context, video) },
                                    onRename = {},
                                    onDelete = { viewModel.removeVideoFromPlaylist(playlistId, video.id) },
                                    onAddToPlaylist = {},
                                    onShowInfo = { selectedVideoForInfo = video }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlistVideos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGrid = false,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = { onPlayVideo(video, playlistVideos) },
                                    onLongClick = {},
                                    onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                    onShare = { viewModel.shareVideo(context, video) },
                                    onRename = {},
                                    onDelete = { viewModel.removeVideoFromPlaylist(playlistId, video.id) },
                                    onAddToPlaylist = {},
                                    onShowInfo = { selectedVideoForInfo = video }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedVideoForInfo?.let { video ->
        VideoInfoDialog(
            video = video,
            onDismiss = { selectedVideoForInfo = null },
            onPlay = { onPlayVideo(video, playlistVideos) },
            onShare = { viewModel.shareVideo(context, video) },
            onRename = {},
            onFavoriteToggle = { viewModel.toggleFavorite(video) },
            onAddToPlaylist = {},
            onDelete = { viewModel.removeVideoFromPlaylist(playlistId, video.id) }
        )
    }
}
