package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderCard
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: MainViewModel,
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val folderSummaries by viewModel.folderSummaries.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("folders_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Video Folders",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (folderSummaries.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Folder,
                    title = "No Folders Found",
                    description = "No folders containing video files were discovered on your device.",
                    actionButtonText = "Scan Folders",
                    onActionClick = { viewModel.scanMedia() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folderSummaries, key = { it.folder }) { summary ->
                        FolderCard(
                            folderSummary = summary,
                            onClick = { onFolderClick(summary.folder) }
                        )
                    }
                }
            }
        }
    }
}
