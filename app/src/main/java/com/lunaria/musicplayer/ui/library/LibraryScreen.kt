package com.lunaria.musicplayer.ui.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import kotlin.math.abs
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lunaria.musicplayer.R
import com.lunaria.musicplayer.domain.model.AudioTrack
import com.lunaria.musicplayer.domain.model.MusicAlbum
import com.lunaria.musicplayer.domain.model.MusicArtist
import com.lunaria.musicplayer.domain.model.MusicFolder
import com.lunaria.musicplayer.domain.model.MusicGenre
import com.lunaria.musicplayer.player.PlaybackManager
import com.lunaria.musicplayer.ui.components.FastScroller
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.ElectricViolet
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.NeonPink
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.SurfaceVariantDark
import kotlinx.coroutines.launch

fun getStoragePermissionString(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

fun hasStoragePermission(context: Context): Boolean {
    val permission = getStoragePermissionString()
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private sealed class DetailScreenTarget {
    data object None : DetailScreenTarget()
    data class Album(val album: MusicAlbum) : DetailScreenTarget()
    data class Folder(val folder: MusicFolder) : DetailScreenTarget()
}

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
    onSwitchMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val permissionString = getStoragePermissionString()

    var lastDetailSwipeDirection by remember { mutableIntStateOf(1) }
    var lastCategorySwipeDirection by remember { mutableIntStateOf(1) }

    // Hoisted scroll states surviving category transitions, back navigation, and recompositions
    val albumsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val allSongsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val foldersGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadAudioFiles(context)
        } else {
            viewModel.setPermissionRequired()
        }
    }

    LaunchedEffect(Unit) {
        if (hasStoragePermission(context)) {
            viewModel.loadAudioFiles(context)
        } else {
            permissionLauncher.launch(permissionString)
        }
    }

    // Intercept back button when drilled down into a category, folder, or album
    BackHandler(enabled = (uiState as? LibraryUiState.Success)?.currentCategory != LibraryCategory.Categories || (uiState as? LibraryUiState.Success)?.selectedAlbum != null || (uiState as? LibraryUiState.Success)?.selectedFolder != null) {
        viewModel.handleBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    LoadingStateView()
                }
                is LibraryUiState.PermissionRequired -> {
                    PermissionRequiredView(
                        onGrantClick = { permissionLauncher.launch(permissionString) }
                    )
                }
                is LibraryUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = {
                            if (hasStoragePermission(context)) {
                                viewModel.loadAudioFiles(context)
                            } else {
                                permissionLauncher.launch(permissionString)
                            }
                        }
                    )
                }
                is LibraryUiState.Success -> {
                    val detailTarget = when {
                        state.selectedAlbum != null -> DetailScreenTarget.Album(state.selectedAlbum)
                        state.selectedFolder != null -> DetailScreenTarget.Folder(state.selectedFolder)
                        else -> DetailScreenTarget.None
                    }

                    // Lateral animated transition between Detail views (Album/Folder) and Library grid
                    AnimatedContent(
                        targetState = detailTarget,
                        transitionSpec = {
                            if (targetState != DetailScreenTarget.None) {
                                // Opening detail view: slides in from right, library retreats left
                                (slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(260))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(220)))
                            } else {
                                // Exiting detail view via back button or horizontal swipe
                                (slideInHorizontally(
                                    initialOffsetX = { (-it / 3) * lastDetailSwipeDirection },
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(240))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { it * lastDetailSwipeDirection },
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(220)))
                            }
                        },
                        label = "DetailNavigation"
                    ) { target ->
                        when (target) {
                            is DetailScreenTarget.Album -> {
                                AlbumDetailScreen(
                                    album = target.album,
                                    onBackClick = {
                                        lastDetailSwipeDirection = 1
                                        viewModel.selectAlbum(null)
                                    },
                                    onSwipeBack = { direction ->
                                        lastDetailSwipeDirection = direction
                                        viewModel.selectAlbum(null)
                                    }
                                )
                            }
                            is DetailScreenTarget.Folder -> {
                                FolderDetailScreen(
                                    folder = target.folder,
                                    onBackClick = {
                                        lastDetailSwipeDirection = 1
                                        viewModel.selectFolder(null)
                                    },
                                    onSwipeBack = { direction ->
                                        lastDetailSwipeDirection = direction
                                        viewModel.selectFolder(null)
                                    }
                                )
                            }
                            DetailScreenTarget.None -> {
                                // Main Library content
                                Column(modifier = Modifier.fillMaxSize()) {
                                    LibraryHeaderBar(
                                        title = when (state.currentCategory) {
                                            LibraryCategory.Categories -> "LIBRARY"
                                            LibraryCategory.AllSongs -> "ALL SONGS"
                                            LibraryCategory.Folders -> "FOLDERS"
                                            LibraryCategory.Albums -> "ALBUMS"
                                            LibraryCategory.Artists -> "ARTISTS"
                                            LibraryCategory.Genres -> "GENRES"
                                        },
                                        subtitle = when (state.currentCategory) {
                                            LibraryCategory.Categories -> "${state.allTracks.size} Tracks • ${state.albums.size} Albums"
                                            LibraryCategory.AllSongs -> "${state.allTracks.size} Tracks"
                                            LibraryCategory.Folders -> "${state.folders.size} Folders"
                                            LibraryCategory.Albums -> "${state.albums.size} Albums"
                                            LibraryCategory.Artists -> "${state.artists.size} Artists"
                                            LibraryCategory.Genres -> "${state.genres.size} Genres"
                                        },
                                        showBackButton = state.currentCategory != LibraryCategory.Categories,
                                        onBackClick = {
                                            lastCategorySwipeDirection = 1
                                            viewModel.handleBack()
                                        },
                                        onRefreshClick = {
                                            if (hasStoragePermission(context)) {
                                                viewModel.loadAudioFiles(context)
                                            } else {
                                                permissionLauncher.launch(permissionString)
                                            }
                                        },
                                        onSwitchMode = onSwitchMode
                                    )

                                    AnimatedContent(
                                        targetState = state.currentCategory,
                                        transitionSpec = {
                                            if (targetState != LibraryCategory.Categories) {
                                                // Drilled down into Category: slide in from right
                                                (slideInHorizontally(
                                                    initialOffsetX = { it },
                                                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                                ) + fadeIn(animationSpec = tween(260))) togetherWith
                                                (slideOutHorizontally(
                                                    targetOffsetX = { -it / 3 },
                                                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                                ) + fadeOut(animationSpec = tween(220)))
                                            } else {
                                                // Swiped or clicked back to Categories root menu
                                                (slideInHorizontally(
                                                    initialOffsetX = { (-it / 3) * lastCategorySwipeDirection },
                                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                                ) + fadeIn(animationSpec = tween(240))) togetherWith
                                                (slideOutHorizontally(
                                                    targetOffsetX = { it * lastCategorySwipeDirection },
                                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                                ) + fadeOut(animationSpec = tween(220)))
                                            }
                                        },
                                        label = "CategoryNavigation"
                                    ) { category ->
                                        when (category) {
                                            LibraryCategory.Categories -> {
                                                CategoriesMenu(
                                                    state = state,
                                                    onCategoryClick = { targetCategory ->
                                                        lastCategorySwipeDirection = 1
                                                        viewModel.navigateToCategory(targetCategory)
                                                    }
                                                )
                                            }
                                            LibraryCategory.AllSongs -> {
                                                AllSongsListView(
                                                    tracks = state.allTracks,
                                                    listState = allSongsListState,
                                                    onSwipeBack = { direction ->
                                                        lastCategorySwipeDirection = direction
                                                        viewModel.navigateToCategory(LibraryCategory.Categories)
                                                    }
                                                )
                                            }
                                            LibraryCategory.Folders -> {
                                                FoldersGridView(
                                                    folders = state.folders,
                                                    gridState = foldersGridState,
                                                    onFolderClick = { folder ->
                                                        lastDetailSwipeDirection = 1
                                                        viewModel.selectFolder(folder)
                                                    },
                                                    onSwipeBack = { direction ->
                                                        lastCategorySwipeDirection = direction
                                                        viewModel.navigateToCategory(LibraryCategory.Categories)
                                                    }
                                                )
                                            }
                                            LibraryCategory.Albums -> {
                                                AlbumsGridView(
                                                    albums = state.albums,
                                                    gridState = albumsGridState,
                                                    onAlbumClick = { album ->
                                                        lastDetailSwipeDirection = 1
                                                        viewModel.selectAlbum(album)
                                                    },
                                                    onSwipeBack = { direction ->
                                                        lastCategorySwipeDirection = direction
                                                        viewModel.navigateToCategory(LibraryCategory.Categories)
                                                    }
                                                )
                                            }
                                            LibraryCategory.Artists -> {
                                                ArtistsListView(
                                                    artists = state.artists,
                                                    onSwipeBack = { direction ->
                                                        lastCategorySwipeDirection = direction
                                                        viewModel.navigateToCategory(LibraryCategory.Categories)
                                                    }
                                                )
                                            }
                                            LibraryCategory.Genres -> {
                                                GenresListView(
                                                    genres = state.genres,
                                                    onSwipeBack = { direction ->
                                                        lastCategorySwipeDirection = direction
                                                        viewModel.navigateToCategory(LibraryCategory.Categories)
                                                    }
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

@Composable
private fun LibraryHeaderBar(
    title: String,
    subtitle: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSwitchMode: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, PrimaryGlow.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SecondaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_lunaria),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = title,
                    color = MetallicText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = subtitle,
                    color = SecondaryAccent.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onSwitchMode != null) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSwitchMode() }
                        .border(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(PrimaryGlow.copy(alpha = 0.6f), SecondaryAccent.copy(alpha = 0.6f))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    color = SurfaceDark.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Mode",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MetallicText
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, PrimaryGlow.copy(alpha = 0.35f), CircleShape)
                    .clickable { onRefreshClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = SecondaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Main categorized list view matching Poweramp (Reference: 1000087451.jpg).
 */
@Composable
private fun CategoriesMenu(
    state: LibraryUiState.Success,
    onCategoryClick: (LibraryCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CategoryMenuItem(
                title = "All Songs",
                subtitle = "${state.allTracks.size} tracks",
                icon = Icons.Default.MusicNote,
                circleColor = SecondaryAccent,
                onClick = { onCategoryClick(LibraryCategory.AllSongs) }
            )
        }
        item {
            CategoryMenuItem(
                title = "Folders",
                subtitle = "${state.folders.size} music folders",
                icon = Icons.Default.Folder,
                circleColor = PrimaryGlow,
                onClick = { onCategoryClick(LibraryCategory.Folders) }
            )
        }
        item {
            CategoryMenuItem(
                title = "Albums",
                subtitle = "${state.albums.size} albums",
                icon = Icons.Default.Album,
                circleColor = NeonPink,
                onClick = { onCategoryClick(LibraryCategory.Albums) }
            )
        }
        item {
            CategoryMenuItem(
                title = "Artists",
                subtitle = "${state.artists.size} artists",
                icon = Icons.Default.Person,
                circleColor = Color(0xFF3877FF),
                onClick = { onCategoryClick(LibraryCategory.Artists) }
            )
        }
        item {
            CategoryMenuItem(
                title = "Genres",
                subtitle = "${state.genres.size} genres",
                icon = Icons.Default.GraphicEq,
                circleColor = ElectricViolet,
                onClick = { onCategoryClick(LibraryCategory.Genres) }
            )
        }
    }
}

@Composable
private fun CategoryMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    circleColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = SurfaceDark.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = circleColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distinct colorful circular icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.18f))
                    .border(1.2.dp, circleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = circleColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MetallicText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MetallicMuted,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CopyrightColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * All Songs list view with A-Z fast scroller pinned to the right edge.
 */
@Composable
private fun AllSongsListView(
    tracks: List<AudioTrack>,
    listState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 32.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackItemRow(
                    track = track,
                    onClick = { PlaybackManager.playTrack(track, tracks) }
                )
            }
        }

        // Pinned FastScroller on right edge
        FastScroller(
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { char ->
                val targetIndex = tracks.indexOfFirst {
                    if (char == '#') !it.title.first().isLetter()
                    else it.title.startsWith(char, ignoreCase = true)
                }
                if (targetIndex >= 0) {
                    scope.launch { listState.scrollToItem(targetIndex) }
                }
            }
        )
    }
}

/**
 * Albums Grid view matching Poweramp (Reference: 1000087452.jpg).
 * Features pinch-to-zoom gesture allowing dynamic columns from 1 (large showcase) to 4 (dense grid)
 * and horizontal swipe-to-back to return to the Categories menu.
 */
@Composable
private fun AlbumsGridView(
    albums: List<MusicAlbum>,
    gridState: LazyGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() },
    onAlbumClick: (MusicAlbum) -> Unit,
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var gridColumns by rememberSaveable { mutableIntStateOf(3) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                zoomAccumulator *= zoom
                                if (zoomAccumulator > 1.22f) {
                                    // Pinch out -> Zoom in -> Fewer columns (larger artwork)
                                    if (gridColumns > 1) {
                                        gridColumns--
                                    }
                                    zoomAccumulator = 1f
                                } else if (zoomAccumulator < 0.82f) {
                                    // Pinch in -> Zoom out -> More columns (denser grid)
                                    if (gridColumns < 4) {
                                        gridColumns++
                                    }
                                    zoomAccumulator = 1f
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else {
                            zoomAccumulator = 1f
                        }
                        if (event.changes.all { !it.pressed }) {
                            zoomAccumulator = 1f
                            break
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (gridColumns == 1) 16.dp else 12.dp,
                end = if (gridColumns == 1) 32.dp else 26.dp,
                top = 8.dp,
                bottom = 120.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(if (gridColumns == 1) 16.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (gridColumns == 1) 16.dp else 12.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    columns = gridColumns,
                    onClick = { onAlbumClick(album) }
                )
            }
        }

        FastScroller(
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { char ->
                val targetIndex = albums.indexOfFirst {
                    if (char == '#') !it.name.first().isLetter()
                    else it.name.startsWith(char, ignoreCase = true)
                }
                if (targetIndex >= 0) {
                    scope.launch { gridState.scrollToItem(targetIndex) }
                }
            }
        )
    }
}

@Composable
private fun AlbumCard(
    album: MusicAlbum,
    columns: Int = 3,
    onClick: () -> Unit
) {
    val cornerRadius = when (columns) {
        1 -> 16.dp
        2 -> 14.dp
        3 -> 12.dp
        else -> 10.dp
    }
    val titleSize = when (columns) {
        1 -> 16.sp
        2 -> 13.sp
        3 -> 11.sp
        else -> 9.5.sp
    }
    val artistSize = when (columns) {
        1 -> 13.sp
        2 -> 11.sp
        3 -> 10.sp
        else -> 8.5.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() }
    ) {
        // Square 1:1 Aspect Ratio Album Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(cornerRadius))
                .background(SurfaceDark)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(NeonPink.copy(alpha = 0.5f), PrimaryGlow.copy(alpha = 0.35f))
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.albumArtUri ?: album.tracks.firstOrNull()?.uri)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                error = painterResource(id = R.drawable.ic_logo_lunaria),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(if (columns == 1) 8.dp else 4.dp))

        Text(
            text = album.name,
            color = MetallicText,
            fontSize = titleSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = album.artist,
                color = MetallicMuted,
                fontSize = artistSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (columns == 1) {
                Text(
                    text = "${album.tracks.size} tracks",
                    color = PrimaryGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Folders Grid view with Fast Scroller and horizontal swipe-to-back navigation.
 */
@Composable
private fun FoldersGridView(
    folders: List<MusicFolder>,
    gridState: LazyGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() },
    onFolderClick: (MusicFolder) -> Unit,
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 26.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders, key = { it.path }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }

        FastScroller(
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { char ->
                val targetIndex = folders.indexOfFirst {
                    if (char == '#') !it.name.first().isLetter()
                    else it.name.startsWith(char, ignoreCase = true)
                }
                if (targetIndex >= 0) {
                    scope.launch { gridState.scrollToItem(targetIndex) }
                }
            }
        )
    }
}

@Composable
private fun FolderCard(
    folder: MusicFolder,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Square 1:1 Aspect Ratio Folder Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(PrimaryGlow.copy(alpha = 0.6f), SecondaryAccent.copy(alpha = 0.4f))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (folder.representativeArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(folder.representativeArtUri ?: folder.tracks.firstOrNull()?.uri)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                    error = painterResource(id = R.drawable.ic_logo_lunaria),
                    contentDescription = folder.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SecondaryAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = folder.name,
            color = MetallicText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${folder.trackCount} Tracks",
            color = SecondaryAccent.copy(alpha = 0.8f),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun FolderDetailScreen(
    folder: MusicFolder,
    onBackClick: () -> Unit,
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            if (onSwipeBack != null) onSwipeBack(dir) else onBackClick()
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            if (onSwipeBack != null) onSwipeBack(dir) else onBackClick()
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, PrimaryGlow.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SecondaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = folder.name,
                    color = MetallicText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.trackCount} Tracks • ${folder.totalDurationFormatted}",
                    color = SecondaryAccent,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folder.tracks, key = { it.id }) { track ->
                TrackItemRow(
                    track = track,
                    onClick = { PlaybackManager.playTrack(track, folder.tracks) }
                )
            }
        }
    }
}

@Composable
private fun ArtistsListView(
    artists: List<MusicArtist>,
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(artists, key = { it.name }) { artist ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (artist.tracks.isNotEmpty()) {
                                PlaybackManager.playTrack(artist.tracks.first(), artist.tracks)
                            }
                        },
                    color = SurfaceDark.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3877FF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF3877FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF3877FF))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(artist.name, color = MetallicText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${artist.trackCount} Tracks • ${artist.albumCount} Albums", color = MetallicMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresListView(
    genres: List<MusicGenre>,
    onSwipeBack: ((direction: Int) -> Unit)? = null
) {
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onSwipeBack?.invoke(dir)
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres, key = { it.name }) { genre ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (genre.tracks.isNotEmpty()) {
                                PlaybackManager.playTrack(genre.tracks.first(), genre.tracks)
                            }
                        },
                    color = SurfaceDark.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet.copy(alpha = 0.2f))
                                .border(1.dp, ElectricViolet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ElectricViolet)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(genre.name, color = MetallicText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${genre.trackCount} Tracks", color = MetallicMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItemRow(
    track: AudioTrack,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = SurfaceDark.copy(alpha = 0.75f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = PrimaryGlow.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(PrimaryGlow.copy(alpha = 0.5f), SecondaryAccent.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.albumArtUri ?: track.uri)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.ic_logo_lunaria),
                    error = painterResource(id = R.drawable.ic_logo_lunaria),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = MetallicText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        color = MetallicMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = " • ${track.album}",
                        color = CopyrightColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariantDark)
                    .border(0.8.dp, SecondaryAccent.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = track.format,
                    color = SecondaryAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = track.formattedDuration,
                color = CopyrightColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LoadingStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = SecondaryAccent,
            trackColor = PrimaryGlow.copy(alpha = 0.2f),
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "SCANNING AUDIO REPOSITORY…",
            color = SecondaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun PermissionRequiredView(
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, PrimaryGlow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = SecondaryAccent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "STORAGE ACCESS REQUIRED",
            color = MetallicText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "To discover and organize your music library, Lunaria requires audio storage access.",
            color = MetallicMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGlow),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("GRANT PERMISSION", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
private fun ErrorStateView(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SCAN FAILED",
            color = Color(0xFFFF4D4D),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            color = MetallicMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGlow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("RETRY SCAN", fontWeight = FontWeight.Bold)
        }
    }
}
