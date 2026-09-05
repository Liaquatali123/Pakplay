package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PakPlayApplication
import com.example.data.local.dao.FolderSummary
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.VideoEntity
import com.example.data.preferences.AppSettings
import com.example.data.preferences.PlayerSettingsState
import com.example.data.repository.VideoRepository
import com.example.data.scanner.MediaScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SortType {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    DURATION_DESC,
    DURATION_ASC,
    SIZE_DESC,
    SIZE_ASC
}

enum class DurationFilter {
    ALL,
    SHORT, // < 5 min
    MEDIUM, // 5 - 20 min
    LONG // > 20 min
}

enum class ResolutionFilter {
    ALL,
    HD, // 720p
    FHD, // 1080p
    UHD_4K // 4K
}

enum class WatchStatusFilter {
    ALL,
    UNWATCHED,
    PARTIALLY_WATCHED,
    WATCHED
}

data class FilterOptions(
    val folder: String? = null,
    val durationFilter: DurationFilter = DurationFilter.ALL,
    val resolutionFilter: ResolutionFilter = ResolutionFilter.ALL,
    val watchStatusFilter: WatchStatusFilter = WatchStatusFilter.ALL,
    val favoritesOnly: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PakPlayApplication
    val repository: VideoRepository = app.repository
    val appSettings: AppSettings = app.appSettings

    val settings: StateFlow<PlayerSettingsState> = appSettings.settings

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedVideoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVideoIds: StateFlow<Set<Long>> = _selectedVideoIds.asStateFlow()

    val isMultiSelectMode: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch {
            _selectedVideoIds.collect { value = it.isNotEmpty() }
        }
    }

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Data streams from Room
    val allVideosRaw: StateFlow<List<VideoEntity>> = repository.allVideos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val recentlyPlayed = repository.recentlyPlayed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentlyAdded = repository.recentlyAdded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val continueWatching = repository.continueWatching.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folderSummaries = repository.folderSummaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Processed and Filtered Videos list
    val filteredVideos: StateFlow<List<VideoEntity>> = combine(
        allVideosRaw,
        _searchQuery,
        _sortType,
        _filterOptions
    ) { videos, query, sort, filter ->
        var list = videos

        // Search Query
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.folder.contains(query, ignoreCase = true)
            }
        }

        // Folder Filter
        if (filter.folder != null) {
            list = list.filter { it.folder.equals(filter.folder, ignoreCase = true) }
        }

        // Duration Filter
        list = when (filter.durationFilter) {
            DurationFilter.SHORT -> list.filter { it.duration < 300000L } // < 5 min
            DurationFilter.MEDIUM -> list.filter { it.duration in 300000L..1200000L } // 5-20 min
            DurationFilter.LONG -> list.filter { it.duration > 1200000L } // > 20 min
            DurationFilter.ALL -> list
        }

        // Resolution Filter
        list = when (filter.resolutionFilter) {
            ResolutionFilter.HD -> list.filter {
                val maxDim = maxOf(it.width, it.height)
                val minDim = minOf(it.width, it.height)
                ((maxDim in 1280..1919 && minDim >= 600) || (minDim in 720..1079)) && maxDim < 1920 && minDim < 1080
            }
            ResolutionFilter.FHD -> list.filter {
                val maxDim = maxOf(it.width, it.height)
                val minDim = minOf(it.width, it.height)
                ((maxDim in 1920..2559 && minDim >= 900) || (minDim in 1080..1439)) && maxDim < 2560 && minDim < 1440
            }
            ResolutionFilter.UHD_4K -> list.filter {
                val maxDim = maxOf(it.width, it.height)
                val minDim = minOf(it.width, it.height)
                (maxDim >= 3840 && minDim >= 1600) || minDim >= 2160 || (maxDim >= 2560 && minDim >= 1440)
            }
            ResolutionFilter.ALL -> list
        }

        // Watch Status
        list = when (filter.watchStatusFilter) {
            WatchStatusFilter.UNWATCHED -> list.filter { it.watchedPercentage == 0 }
            WatchStatusFilter.PARTIALLY_WATCHED -> list.filter { it.watchedPercentage in 1..89 }
            WatchStatusFilter.WATCHED -> list.filter { it.watchedPercentage >= 90 }
            WatchStatusFilter.ALL -> list
        }

        // Favorites only
        if (filter.favoritesOnly) {
            list = list.filter { it.isFavorite }
        }

        // Sorting
        when (sort) {
            SortType.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            SortType.DATE_ASC -> list.sortedBy { it.dateAdded }
            SortType.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortType.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortType.DURATION_DESC -> list.sortedByDescending { it.duration }
            SortType.DURATION_ASC -> list.sortedBy { it.duration }
            SortType.SIZE_DESC -> list.sortedByDescending { it.size }
            SortType.SIZE_ASC -> list.sortedBy { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeScanJob: Job? = null

    init {
        // Startup media sync runs asynchronously in background without blocking Home UI
        scanMedia(force = false)
    }

    fun scanMedia(force: Boolean = false) {
        // Debounce / coalesce: if a background scan is already running and not forced, do not spawn duplicate jobs
        if (activeScanJob?.isActive == true && !force) {
            return
        }

        activeScanJob = viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                repository.scanMedia(forceFullScan = force)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Background media scan error", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sort: SortType) {
        _sortType.value = sort
        appSettings.updateSortOption(sort.name)
    }

    fun setFilterFolder(folder: String?) {
        _filterOptions.value = _filterOptions.value.copy(folder = folder)
    }

    fun setFilterOptions(options: FilterOptions) {
        _filterOptions.value = options
    }

    fun resetFilters() {
        _filterOptions.value = FilterOptions()
    }

    fun toggleGridView() {
        val next = !_isGridView.value
        _isGridView.value = next
        appSettings.updateGridView(next)
    }

    fun toggleFavorite(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(video.id, video.isFavorite)
        }
    }

    // Multi-selection management
    fun toggleSelection(videoId: Long) {
        val current = _selectedVideoIds.value.toMutableSet()
        if (current.contains(videoId)) {
            current.remove(videoId)
        } else {
            current.add(videoId)
        }
        _selectedVideoIds.value = current
    }

    fun selectAll(videos: List<VideoEntity>) {
        _selectedVideoIds.value = videos.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedVideoIds.value = emptySet()
    }

    fun deleteSelectedVideos() {
        val ids = _selectedVideoIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deleteVideo(id)
            }
            clearSelection()
        }
    }

    fun favoriteSelectedVideos(isFavorite: Boolean) {
        val ids = _selectedVideoIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                repository.toggleFavorite(id, !isFavorite)
            }
            clearSelection()
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video.id)
        }
    }

    fun renameVideo(video: VideoEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateVideo(video.copy(title = newTitle))
        }
    }

    // Playlist actions
    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            onCreated(id)
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addVideosToPlaylist(playlistId: Long, videoIds: Set<Long>) {
        viewModelScope.launch {
            videoIds.forEach { vid ->
                repository.addVideoToPlaylist(playlistId, vid)
            }
            clearSelection()
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearResumePositions() {
        viewModelScope.launch {
            repository.clearResumePositions()
        }
    }

    fun shareVideo(context: Context, video: VideoEntity) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = video.mimeType.ifEmpty { "video/*" }
                if (video.path.startsWith("http")) {
                    putExtra(Intent.EXTRA_TEXT, "${video.title}\n${video.path}")
                } else {
                    val file = File(video.path)
                    val uri = Uri.parse(video.path)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error sharing video", e)
        }
    }
}
