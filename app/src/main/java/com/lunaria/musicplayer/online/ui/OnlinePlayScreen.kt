package com.lunaria.musicplayer.online.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lunaria.musicplayer.online.model.OnlinePlaylist
import com.lunaria.musicplayer.online.model.OnlineTrack
import com.lunaria.musicplayer.player.AudioFxManager
import com.lunaria.musicplayer.player.PlaybackManager
import com.lunaria.musicplayer.ui.components.MiniPlayer
import com.lunaria.musicplayer.ui.equalizer.EqualizerScreen
import com.lunaria.musicplayer.ui.player.NowPlayingScreen
import com.lunaria.musicplayer.ui.theme.LocalThemeColors

@Composable
fun OnlinePlayScreen(
    onSwitchMode: () -> Unit,
    viewModel: OnlineViewModel = viewModel()
) {
    val themeColors = LocalThemeColors.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchErrorMessage by viewModel.searchErrorMessage.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val savedTracks by viewModel.savedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val isExtracting by viewModel.isExtracting.collectAsState()
    val extractingTrackTitle by viewModel.extractingTrackTitle.collectAsState()
    val extractionErrorMessage by viewModel.extractionErrorMessage.collectAsState()

    val currentTrack by PlaybackManager.currentTrack.collectAsState()

    var trackForPlaylistDialog by remember { mutableStateOf<OnlineTrack?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }

    // Disable EQ by default when entering Online Play mode unless user manually enabled it during this session
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!AudioFxManager.userManuallyEnabledEqInOnline) {
            AudioFxManager.setEnabled(false)
            AudioFxManager.setPreampGain(AudioFxManager.BASELINE_PREAMP_GAIN_MB)
            PlaybackManager.setMasterVolume(1.0f)
        }
    }

    BackHandler(enabled = isNowPlayingExpanded || showEqualizerSheet) {
        if (showEqualizerSheet) {
            showEqualizerSheet = false
        } else {
            isNowPlayingExpanded = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundDark)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Title & Switch Mode Button
            OnlineTopBar(
                onSwitchMode = onSwitchMode,
                themeColors = themeColors
            )

            // Neon Audio Extraction Loading Banner
            AnimatedVisibility(
                visible = isExtracting,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(themeColors.primaryGlow, themeColors.secondaryAccent)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    color = themeColors.surfaceDark.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = themeColors.secondaryAccent,
                            strokeWidth = 2.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Extracting audio stream…",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            extractingTrackTitle?.let { title ->
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    color = themeColors.metallicMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Stream Extraction Error Banner
            AnimatedVisibility(
                visible = extractionErrorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color(0xFF2A1515),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = extractionErrorMessage ?: "Extraction failed",
                            fontSize = 12.sp,
                            color = Color(0xFFFFCDD2),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearExtractionError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = themeColors.metallicMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Mode Tabs (Search, Saved, Playlists)
            OnlineTabRow(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                themeColors = themeColors
            )

            // Content Area based on Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    OnlineTab.SEARCH -> {
                        SearchTabContent(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.performSearch()
                            },
                            isSearching = isSearching,
                            results = searchResults,
                            errorMessage = searchErrorMessage,
                            history = searchHistory,
                            onSelectHistory = {
                                focusManager.clearFocus()
                                viewModel.performSearch(it)
                            },
                            onRemoveHistory = { viewModel.removeSearchHistory(it) },
                            onClearHistory = { viewModel.clearSearchHistory() },
                            savedTracks = savedTracks,
                            onToggleSave = { viewModel.toggleSaveTrack(it) },
                            onPlayTrack = { viewModel.playTrack(it) },
                            onAddToPlaylist = { trackForPlaylistDialog = it },
                            themeColors = themeColors
                        )
                    }
                    OnlineTab.SAVED -> {
                        SavedTabContent(
                            savedTracks = savedTracks,
                            onPlayTrack = { viewModel.playTrack(it) },
                            onRemoveSave = { viewModel.toggleSaveTrack(it) },
                            onAddToPlaylist = { trackForPlaylistDialog = it },
                            themeColors = themeColors
                        )
                    }
                    OnlineTab.PLAYLISTS -> {
                        PlaylistsTabContent(
                            playlists = playlists,
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlayTrack = { viewModel.playTrack(it) },
                            onRemoveTrack = { pId, vId -> viewModel.removeTrackFromPlaylist(pId, vId) },
                            themeColors = themeColors
                        )
                    }
                }
            }

            // Persistent Mini-Player strip (visible when any track is playing/loaded in ExoPlayer)
            if (currentTrack != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    MiniPlayer(
                        onClick = { isNowPlayingExpanded = true }
                    )
                }
            }
        }

        // Add to Playlist Dialog
        trackForPlaylistDialog?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = playlists,
                onDismiss = { trackForPlaylistDialog = null },
                onSelectPlaylist = { playlistId ->
                    viewModel.addTrackToPlaylist(playlistId, track)
                    trackForPlaylistDialog = null
                    Toast.makeText(context, "Added to playlist!", Toast.LENGTH_SHORT).show()
                },
                onCreateNewPlaylist = { name ->
                    viewModel.createPlaylist(name)
                },
                themeColors = themeColors
            )
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name ->
                    viewModel.createPlaylist(name)
                    showCreatePlaylistDialog = false
                    Toast.makeText(context, "Playlist created!", Toast.LENGTH_SHORT).show()
                },
                themeColors = themeColors
            )
        }

        // Fullscreen "Now Playing" Screen Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                onDismiss = { isNowPlayingExpanded = false },
                onNavigateToEqualizer = {
                    isNowPlayingExpanded = false
                    showEqualizerSheet = true
                }
            )
        }

        // Equalizer Overlay
        AnimatedVisibility(
            visible = showEqualizerSheet,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            EqualizerScreen(
                onNavigateBack = {
                    showEqualizerSheet = false
                }
            )
        }
    }
}

@Composable
private fun OnlineTopBar(
    onSwitchMode: () -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "ONLINE PLAY",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Stream • Discover • Cloud Playlists",
                fontSize = 11.sp,
                color = themeColors.metallicMuted
            )
        }

        // Switch Mode Button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSwitchMode() }
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            themeColors.primaryGlow.copy(alpha = 0.6f),
                            themeColors.secondaryAccent.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            color = themeColors.surfaceDark.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Switch Mode",
                    tint = themeColors.secondaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Switch Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = themeColors.metallicText
                )
            }
        }
    }
}

@Composable
private fun OnlineTabRow(
    selectedTab: OnlineTab,
    onTabSelected: (OnlineTab) -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OnlineTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) }
                    .border(
                        width = 1.dp,
                        color = if (isSelected) themeColors.primaryGlow else themeColors.surfaceVariantDark,
                        shape = RoundedCornerShape(12.dp)
                    ),
                color = if (isSelected) themeColors.primaryGlow.copy(alpha = 0.2f) else themeColors.surfaceDark
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) themeColors.secondaryAccent else themeColors.metallicMuted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchTabContent(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    results: List<OnlineTrack>,
    errorMessage: String?,
    history: List<String>,
    onSelectHistory: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    savedTracks: List<OnlineTrack>,
    onToggleSave: (OnlineTrack) -> Unit,
    onPlayTrack: (OnlineTrack) -> Unit,
    onAddToPlaylist: (OnlineTrack) -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Neon Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Search songs, artists, albums...", fontSize = 13.sp, color = themeColors.metallicMuted)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = themeColors.secondaryAccent
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = themeColors.metallicMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primaryGlow,
                    unfocusedBorderColor = themeColors.surfaceVariantDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = themeColors.metallicText,
                    cursorColor = themeColors.secondaryAccent,
                    focusedContainerColor = themeColors.surfaceDark,
                    unfocusedContainerColor = themeColors.surfaceDark
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSearch() }
                    .border(
                        width = 1.dp,
                        color = themeColors.primaryGlow,
                        shape = RoundedCornerShape(14.dp)
                    ),
                color = themeColors.primaryGlow.copy(alpha = 0.25f)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Submit Search",
                        tint = themeColors.secondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Loading Indicator
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColors.secondaryAccent)
            }
        }

        // Error message banner
        errorMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFF2A1515),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = Color(0xFFFFCDD2)
                    )
                }
            }
        }

        // Search History Section (Displayed when results are empty and not actively searching)
        if (results.isEmpty() && !isSearching && history.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = themeColors.secondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Recent Searches (${history.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.metallicText
                        )
                    }

                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = "Clear All",
                            fontSize = 11.sp,
                            color = Color(0xFFFF5252)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    history.forEach { queryItem ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectHistory(queryItem) }
                                .border(
                                    width = 1.dp,
                                    color = themeColors.surfaceVariantDark,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            color = themeColors.surfaceDark
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = queryItem,
                                    fontSize = 12.sp,
                                    color = themeColors.metallicText,
                                    maxLines = 1
                                )
                                IconButton(
                                    onClick = { onRemoveHistory(queryItem) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = themeColors.metallicMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Results List
        if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.videoId }) { track ->
                    val isSaved = savedTracks.any { it.videoId == track.videoId }
                    OnlineTrackRowItem(
                        track = track,
                        isSaved = isSaved,
                        onPlay = { onPlayTrack(track) },
                        onToggleSave = { onToggleSave(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                        themeColors = themeColors
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedTabContent(
    savedTracks: List<OnlineTrack>,
    onPlayTrack: (OnlineTrack) -> Unit,
    onRemoveSave: (OnlineTrack) -> Unit,
    onAddToPlaylist: (OnlineTrack) -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    if (savedTracks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = themeColors.secondaryAccent.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No saved songs yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tap the bookmark icon on any track to save it here for instant direct playback.",
                    fontSize = 12.sp,
                    color = themeColors.metallicMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedTracks, key = { it.videoId }) { track ->
                OnlineTrackRowItem(
                    track = track,
                    isSaved = true,
                    onPlay = { onPlayTrack(track) },
                    onToggleSave = { onRemoveSave(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    themeColors = themeColors
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<OnlinePlaylist>,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayTrack: (OnlineTrack) -> Unit,
    onRemoveTrack: (playlistId: String, videoId: String) -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Create Playlist Row Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Cloud Playlists",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCreatePlaylistClick() }
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(themeColors.primaryGlow, themeColors.secondaryAccent)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                color = themeColors.primaryGlow.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = themeColors.secondaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "New Playlist",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.metallicText
                    )
                }
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = themeColors.primaryGlow.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No custom playlists yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Create a custom playlist to group your favorite online music streams.",
                        fontSize = 12.sp,
                        color = themeColors.metallicMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val isExpanded = expandedPlaylistId == playlist.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = if (isExpanded) themeColors.primaryGlow else themeColors.surfaceVariantDark,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        color = themeColors.surfaceDark
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedPlaylistId = if (isExpanded) null else playlist.id
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = themeColors.surfaceVariantDark
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = themeColors.secondaryAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = playlist.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${playlist.tracks.size} tracks",
                                            fontSize = 12.sp,
                                            color = themeColors.metallicMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onDeletePlaylist(playlist.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Playlist",
                                            tint = Color(0xFFFF5252).copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = themeColors.metallicMuted
                                    )
                                }
                            }

                            // Expanded Playlist Tracks
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (playlist.tracks.isEmpty()) {
                                        Text(
                                            text = "No tracks in this playlist yet. Add songs using the + button in Search or Saved.",
                                            fontSize = 11.sp,
                                            color = themeColors.metallicMuted,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        playlist.tracks.forEach { track ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(themeColors.surfaceVariantDark.copy(alpha = 0.4f))
                                                    .clickable { onPlayTrack(track) }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = track.thumbnailUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = track.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = track.channel,
                                                            fontSize = 10.sp,
                                                            color = themeColors.metallicMuted,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { onRemoveTrack(playlist.id, track.videoId) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove from playlist",
                                                        tint = themeColors.metallicMuted,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineTrackRowItem(
    track: OnlineTrack,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToPlaylist: () -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .border(
                width = 1.dp,
                color = themeColors.surfaceVariantDark,
                shape = RoundedCornerShape(12.dp)
            ),
        color = themeColors.surfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title & Channel
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.channel,
                    fontSize = 11.sp,
                    color = themeColors.secondaryAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions: Bookmark Save toggle & Add to playlist (+)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Unsave" else "Save",
                        tint = if (isSaved) themeColors.secondaryAccent else themeColors.metallicMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = themeColors.metallicMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    track: OnlineTrack,
    playlists: List<OnlinePlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    var showCreateSubdialog by remember { mutableStateOf(false) }

    if (showCreateSubdialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateSubdialog = false },
            onCreate = { name ->
                onCreateNewPlaylist(name)
                showCreateSubdialog = false
            },
            themeColors = themeColors
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surfaceDark,
        title = {
            Text(
                text = "Add to Playlist",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = track.title,
                    fontSize = 12.sp,
                    color = themeColors.secondaryAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists found. Create your first playlist below:",
                        fontSize = 12.sp,
                        color = themeColors.metallicMuted
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPlaylist(pl.id) }
                                    .border(
                                        width = 1.dp,
                                        color = themeColors.surfaceVariantDark,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                color = themeColors.surfaceVariantDark.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pl.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${pl.tracks.size} tracks",
                                        fontSize = 11.sp,
                                        color = themeColors.metallicMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showCreateSubdialog = true }) {
                Text("+ New Playlist", color = themeColors.primaryGlow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = themeColors.metallicMuted)
            }
        }
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    themeColors: com.lunaria.musicplayer.ui.theme.ThemeColors
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surfaceDark,
        title = {
            Text(
                text = "New Playlist",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Playlist name", fontSize = 13.sp, color = themeColors.metallicMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primaryGlow,
                        unfocusedBorderColor = themeColors.surfaceVariantDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = themeColors.metallicText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onCreate(playlistName.trim())
                    }
                }
            ) {
                Text("Create", color = themeColors.secondaryAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = themeColors.metallicMuted)
            }
        }
    )
}
