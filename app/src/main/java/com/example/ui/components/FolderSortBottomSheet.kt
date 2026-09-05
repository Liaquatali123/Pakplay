package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.viewmodel.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSortBottomSheet(
    currentSort: SortType,
    onSortSelected: (SortType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sort Videos & Episodes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Arrange files in exact sequential or custom order",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SortOptionItem(
                title = "Episode: 1 → 50 (Line-wise / Sequence)",
                subtitle = "Natural chronological order for series, anime & numbered parts",
                icon = Icons.Default.FormatListNumbered,
                isSelected = currentSort == SortType.EPISODE_ASC,
                isRecommended = true,
                onClick = {
                    onSortSelected(SortType.EPISODE_ASC)
                    onDismiss()
                }
            )

            SortOptionItem(
                title = "Episode: 50 → 1 (Reverse)",
                subtitle = "Newest or latest episodes at top",
                icon = Icons.Default.FormatListNumbered,
                isSelected = currentSort == SortType.EPISODE_DESC,
                onClick = {
                    onSortSelected(SortType.EPISODE_DESC)
                    onDismiss()
                }
            )

            SortOptionItem(
                title = "Name: A → Z (Alphanumeric)",
                subtitle = "Alphabetical by filename",
                icon = Icons.Default.SortByAlpha,
                isSelected = currentSort == SortType.NAME_ASC,
                onClick = {
                    onSortSelected(SortType.NAME_ASC)
                    onDismiss()
                }
            )

            SortOptionItem(
                title = "Date: Newest First",
                subtitle = "Recently added files first",
                icon = Icons.Default.Today,
                isSelected = currentSort == SortType.DATE_DESC,
                onClick = {
                    onSortSelected(SortType.DATE_DESC)
                    onDismiss()
                }
            )

            SortOptionItem(
                title = "Date: Oldest First",
                subtitle = "Earliest added files first",
                icon = Icons.Default.Today,
                isSelected = currentSort == SortType.DATE_ASC,
                onClick = {
                    onSortSelected(SortType.DATE_ASC)
                    onDismiss()
                }
            )

            SortOptionItem(
                title = "Duration: Longest First",
                subtitle = "Full movies and long recordings first",
                icon = Icons.Default.Schedule,
                isSelected = currentSort == SortType.DURATION_DESC,
                onClick = {
                    onSortSelected(SortType.DURATION_DESC)
                    onDismiss()
                }
            )

            SortOptionItem(
                title = "File Size: Largest First",
                subtitle = "Highest bitrate or storage size",
                icon = Icons.Default.Storage,
                isSelected = currentSort == SortType.SIZE_DESC,
                onClick = {
                    onSortSelected(SortType.SIZE_DESC)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SortOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CyanNeon.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("sort_option_${title.substringBefore(' ')}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = CyanNeon.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                color = CyanNeon,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CyanNeon,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
