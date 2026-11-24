package com.lunaria.musicplayer.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunaria.musicplayer.player.PlaybackManager
import com.lunaria.musicplayer.ui.components.MiniPlayer
import com.lunaria.musicplayer.ui.equalizer.EqualizerScreen
import com.lunaria.musicplayer.ui.library.LibraryScreen
import com.lunaria.musicplayer.ui.settings.SettingsScreen
import com.lunaria.musicplayer.ui.player.NowPlayingScreen
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark

enum class MainTab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.GridView),
    Equalizer("Equalizer", Icons.Default.Equalizer),
    Search("Search", Icons.Default.Search),
    Menu("Menu", Icons.Default.Menu)
}

@Composable
fun MainScaffold(
    modifier: Modifier = Modifier,
    onSwitchMode: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MainTab.Library) }
    var previousTab by remember { mutableStateOf(MainTab.Library) }
    var slideDirection by remember { mutableIntStateOf(1) } // 1: slide from right, -1: slide from left
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()

    fun navigateToTab(newTab: MainTab, direction: Int? = null) {
        if (newTab != selectedTab) {
            slideDirection = direction ?: if (newTab.ordinal > selectedTab.ordinal) 1 else -1
            previousTab = selectedTab
            selectedTab = newTab
        }
    }

    // Intercept system Back when on Equalizer tab to smoothly slide back to previous tab
    BackHandler(enabled = selectedTab == MainTab.Equalizer) {
        val targetTab = if (previousTab != MainTab.Equalizer) previousTab else MainTab.Library
        slideDirection = -1
        selectedTab = targetTab
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Main Screen Content based on active tab with lateral slide animations
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (slideInHorizontally(
                            initialOffsetX = { it * slideDirection },
                            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(260))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { -it * slideDirection },
                            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(220)))
                    },
                    label = "MainTabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        MainTab.Library -> {
                            LibraryScreen(onSwitchMode = onSwitchMode)
                        }
                        MainTab.Equalizer -> {
                            EqualizerScreen(
                                onNavigateBack = { swipeDirection ->
                                    val targetTab = if (previousTab != MainTab.Equalizer) previousTab else MainTab.Library
                                    // When swiping right (swipeDirection = 1), EQ slides right and target slides in from left
                                    slideDirection = -swipeDirection
                                    selectedTab = targetTab
                                }
                            )
                        }
                        MainTab.Search -> {
                            SearchPlaceholderView()
                        }
                        MainTab.Menu -> {
                            SettingsScreen(onSwitchMode = onSwitchMode)
                        }
                    }
                }
            }

            // Bottom Area: Persistent Mini-Player + Bottom Navigation Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .navigationBarsPadding()
            ) {
                // Persistent Mini-Player strip (only visible when a track is loaded/playing)
                if (currentTrack != null) {
                    MiniPlayer(
                        onClick = { isNowPlayingExpanded = true }
                    )
                }

                // 4-Tab Bottom Navigation Bar
                PowerampBottomNav(
                    currentTab = selectedTab,
                    onTabSelected = { newTab ->
                        navigateToTab(newTab)
                    }
                )
            }
        }

        // Fullscreen "Now Playing" Screen Modal Overlay with slide & swipe-to-exit gestures
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
                    navigateToTab(MainTab.Equalizer, direction = 1)
                }
            )
        }
    }
}

@Composable
private fun PowerampBottomNav(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        color = SurfaceDark.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, PrimaryGlow.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val tint = if (isSelected) SecondaryAccent else CopyrightColor

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerPlaceholderView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, SecondaryAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Equalizer, contentDescription = null, tint = SecondaryAccent, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("CELESTIAL EQUALIZER", color = MetallicText, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("10-Band Precision Studio EQ & Bass Resonance Engine", color = MetallicMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SearchPlaceholderView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
            Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGlow, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("SEARCH REPOSITORY", color = MetallicText, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Instant query across all tracks, albums, artists, and folders", color = MetallicMuted, fontSize = 12.sp)
    }
}

@Composable
private fun MenuPlaceholderView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
            Icon(Icons.Default.Menu, contentDescription = null, tint = PrimaryGlow, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("LUNARIA SETTINGS", color = MetallicText, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Audio Output • Rescan Media • Midnight & Neon Theme Preferences", color = MetallicMuted, fontSize = 12.sp)
    }
}

