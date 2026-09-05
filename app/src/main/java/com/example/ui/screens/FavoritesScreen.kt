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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onPlayVideo: (VideoEntity, List<VideoEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var selectedVideoForPlaylist by remember { mutableStateOf<VideoEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("favorites_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Favorites",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${favorites.size} favorite videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            if (favorites.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (favorites.isNotEmpty()) {
                                onPlayVideo(favorites.first(), favorites)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("play_all_favorites_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (favorites.isNotEmpty()) {
                                val shuffled = favorites.shuffled()
                                onPlayVideo(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("shuffle_favorites_button")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (favorites.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Favorite,
                        title = "No Favorites Yet",
                        description = "Tap the heart icon on any video to add it to your favorites list for quick access."
                    )
                } else {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 165.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGrid = true,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = { onPlayVideo(video, favorites) },
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favorites, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGrid = false,
                                    isSelected = false,
                                    isSelectionMode = false,
                                    onClick = { onPlayVideo(video, favorites) },
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

    selectedVideoForInfo?.let { video ->
        VideoInfoDialog(
            video = video,
            onDismiss = { selectedVideoForInfo = null },
            onPlay = { onPlayVideo(video, favorites) },
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
