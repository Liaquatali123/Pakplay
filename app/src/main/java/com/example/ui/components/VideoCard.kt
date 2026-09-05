package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.VideoEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.util.EpisodeSorter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: VideoEntity,
    isGrid: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val episodeBadge = remember(video.title) { EpisodeSorter.getEpisodeBadge(video.title) }

    if (isGrid) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("video_card_${video.id}"),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CyanNeon, PurpleElectric)),
                width = 2.dp
            ) else null
        ) {
            Column {
                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(video.thumbnailUri ?: video.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 50f
                                )
                            )
                    )

                    // Selection indicator
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                                .align(Alignment.TopStart)
                                .background(
                                    if (isSelected) CyanNeon else Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else if (episodeBadge != null) {
                        // Episode Badge (e.g. Ep. 1, Ep. 50, #12, S1:E2)
                        Surface(
                            modifier = Modifier
                                .padding(6.dp)
                                .align(Alignment.TopStart),
                            color = PurpleElectric.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = episodeBadge,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Resolution badge
                    if (video.resolutionLabel.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .padding(6.dp)
                                .align(Alignment.TopEnd),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = video.resolutionLabel,
                                color = CyanNeon,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Duration badge
                    Surface(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Watched progress bar at bottom of thumbnail
                    if (video.watchedPercentage > 0) {
                        LinearProgressIndicator(
                            progress = { video.watchedPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = CyanNeon,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                // Info Section
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${video.folder} • ${video.formattedSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            VideoCardDropdownMenu(
                                expanded = showMenu,
                                onDismiss = { showMenu = false },
                                isFavorite = video.isFavorite,
                                onFavoriteToggle = onFavoriteToggle,
                                onShare = onShare,
                                onRename = onRename,
                                onDelete = onDelete,
                                onAddToPlaylist = onAddToPlaylist,
                                onShowInfo = onShowInfo
                            )
                        }
                    }
                }
            }
        }
    } else {
        // List Item Layout
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("video_card_list_${video.id}"),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(video.thumbnailUri ?: video.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Duration badge
                    Surface(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (video.watchedPercentage > 0) {
                        LinearProgressIndicator(
                            progress = { video.watchedPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = CyanNeon,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (episodeBadge != null) {
                            Surface(
                                color = PurpleElectric.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = episodeBadge,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (video.resolutionLabel.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = video.resolutionLabel,
                                    color = CyanNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${video.formattedSize} • ${video.folder}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (video.watchedPercentage > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${video.watchedPercentage}% watched",
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Favorite and Menu
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) Color(0xFFFF3366) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    VideoCardDropdownMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        isFavorite = video.isFavorite,
                        onFavoriteToggle = onFavoriteToggle,
                        onShare = onShare,
                        onRename = onRename,
                        onDelete = onDelete,
                        onAddToPlaylist = onAddToPlaylist,
                        onShowInfo = onShowInfo
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCardDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowInfo: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove Favorite" else "Add to Favorites") },
            onClick = { onDismiss(); onFavoriteToggle() }
        )
        DropdownMenuItem(
            text = { Text("Add to Playlist") },
            onClick = { onDismiss(); onAddToPlaylist() }
        )
        DropdownMenuItem(
            text = { Text("Share Video") },
            onClick = { onDismiss(); onShare() }
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text("Video Info") },
            onClick = { onDismiss(); onShowInfo() }
        )
        DropdownMenuItem(
            text = { Text("Delete Video", color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}
