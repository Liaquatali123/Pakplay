package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleElectric
import com.example.ui.viewmodel.DurationFilter
import com.example.ui.viewmodel.FilterOptions
import com.example.ui.viewmodel.ResolutionFilter
import com.example.ui.viewmodel.SortType
import com.example.ui.viewmodel.WatchStatusFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortFilterBottomSheet(
    sheetState: SheetState,
    currentSort: SortType,
    currentFilter: FilterOptions,
    availableFolders: List<String>,
    onApply: (SortType, FilterOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }
    var selectedFolder by remember { mutableStateOf(currentFilter.folder) }
    var selectedDuration by remember { mutableStateOf(currentFilter.durationFilter) }
    var selectedResolution by remember { mutableStateOf(currentFilter.resolutionFilter) }
    var selectedWatchStatus by remember { mutableStateOf(currentFilter.watchStatusFilter) }
    var favoritesOnly by remember { mutableStateOf(currentFilter.favoritesOnly) }

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
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort & Filter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Section
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CyanNeon
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("Episode: 1 → 50", selectedSort == SortType.EPISODE_ASC) { selectedSort = SortType.EPISODE_ASC }
                SortChip("Episode: 50 → 1", selectedSort == SortType.EPISODE_DESC) { selectedSort = SortType.EPISODE_DESC }
                SortChip("Date: Newest", selectedSort == SortType.DATE_DESC) { selectedSort = SortType.DATE_DESC }
                SortChip("Date: Oldest", selectedSort == SortType.DATE_ASC) { selectedSort = SortType.DATE_ASC }
                SortChip("Name: A to Z", selectedSort == SortType.NAME_ASC) { selectedSort = SortType.NAME_ASC }
                SortChip("Name: Z to A", selectedSort == SortType.NAME_DESC) { selectedSort = SortType.NAME_DESC }
                SortChip("Duration: Longest", selectedSort == SortType.DURATION_DESC) { selectedSort = SortType.DURATION_DESC }
                SortChip("Duration: Shortest", selectedSort == SortType.DURATION_ASC) { selectedSort = SortType.DURATION_ASC }
                SortChip("Size: Largest", selectedSort == SortType.SIZE_DESC) { selectedSort = SortType.SIZE_DESC }
                SortChip("Size: Smallest", selectedSort == SortType.SIZE_ASC) { selectedSort = SortType.SIZE_ASC }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Duration Filter
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CyanNeon
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("All Durations", selectedDuration == DurationFilter.ALL) { selectedDuration = DurationFilter.ALL }
                SortChip("Short (< 5 min)", selectedDuration == DurationFilter.SHORT) { selectedDuration = DurationFilter.SHORT }
                SortChip("Medium (5-20 min)", selectedDuration == DurationFilter.MEDIUM) { selectedDuration = DurationFilter.MEDIUM }
                SortChip("Long (> 20 min)", selectedDuration == DurationFilter.LONG) { selectedDuration = DurationFilter.LONG }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Resolution Filter
            Text(
                text = "Resolution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CyanNeon
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("All Resolutions", selectedResolution == ResolutionFilter.ALL) { selectedResolution = ResolutionFilter.ALL }
                SortChip("720p HD", selectedResolution == ResolutionFilter.HD) { selectedResolution = ResolutionFilter.HD }
                SortChip("1080p FHD", selectedResolution == ResolutionFilter.FHD) { selectedResolution = ResolutionFilter.FHD }
                SortChip("4K UHD", selectedResolution == ResolutionFilter.UHD_4K) { selectedResolution = ResolutionFilter.UHD_4K }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Watch Status
            Text(
                text = "Watch Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CyanNeon
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("All", selectedWatchStatus == WatchStatusFilter.ALL) { selectedWatchStatus = WatchStatusFilter.ALL }
                SortChip("Unwatched", selectedWatchStatus == WatchStatusFilter.UNWATCHED) { selectedWatchStatus = WatchStatusFilter.UNWATCHED }
                SortChip("Partially Watched", selectedWatchStatus == WatchStatusFilter.PARTIALLY_WATCHED) { selectedWatchStatus = WatchStatusFilter.PARTIALLY_WATCHED }
                SortChip("Completed", selectedWatchStatus == WatchStatusFilter.WATCHED) { selectedWatchStatus = WatchStatusFilter.WATCHED }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions: Reset & Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedSort = SortType.DATE_DESC
                        selectedFolder = null
                        selectedDuration = DurationFilter.ALL
                        selectedResolution = ResolutionFilter.ALL
                        selectedWatchStatus = WatchStatusFilter.ALL
                        favoritesOnly = false
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = {
                        onApply(
                            selectedSort,
                            FilterOptions(
                                folder = selectedFolder,
                                durationFilter = selectedDuration,
                                resolutionFilter = selectedResolution,
                                watchStatusFilter = selectedWatchStatus,
                                favoritesOnly = favoritesOnly
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).testTag("apply_sort_filter_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
            selectedLabelColor = CyanNeon,
            selectedLeadingIconColor = CyanNeon,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
