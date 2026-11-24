package com.lunaria.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.lunaria.musicplayer.online.ui.OnlinePlayScreen
import com.lunaria.musicplayer.player.AudioFxManager
import com.lunaria.musicplayer.player.PlaybackManager
import com.lunaria.musicplayer.player.VisualizerManager
import com.lunaria.musicplayer.ui.main.MainScaffold
import com.lunaria.musicplayer.ui.mode.ModeSelectionScreen
import com.lunaria.musicplayer.ui.splash.SplashScreen
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.LocalThemeColors
import com.lunaria.musicplayer.ui.theme.LunariaTheme
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.ThemeManager

enum class AppDestination {
    Splash,
    ModeSelection,
    LocalMain,
    OnlinePlay
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize theme preferences and audio engine
        ThemeManager.initialize(this)
        PlaybackManager.initialize(this)

        setContent {
            LunariaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    LunariaApp()
                }
            }
        }
    }
}

@Composable
fun LunariaApp() {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppDestination.Splash) }
    var showRecordAudioRationale by remember { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        VisualizerManager.updatePermission(isGranted)
        if (isGranted) {
            VisualizerManager.attachToAudioSession(PlaybackManager.getAudioSessionId(), true)
        }
    }

    // Intercept Back press from LocalMain or OnlinePlay to return to Mode Selection
    BackHandler(enabled = currentDestination == AppDestination.LocalMain || currentDestination == AppDestination.OnlinePlay) {
        currentDestination = AppDestination.ModeSelection
    }

    // Check permission when transitioning into LocalMain screen
    LaunchedEffect(currentDestination) {
        if (currentDestination == AppDestination.LocalMain) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPerm) {
                VisualizerManager.updatePermission(true)
                VisualizerManager.attachToAudioSession(PlaybackManager.getAudioSessionId(), true)
            } else {
                showRecordAudioRationale = true
            }
        }
    }

    // Smooth, cinematic zoom-through crossfade transition into destination screens
    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            (fadeIn(animationSpec = tween(durationMillis = 750, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.93f, animationSpec = tween(durationMillis = 750, easing = LinearOutSlowInEasing)))
                .togetherWith(
                    fadeOut(animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)) +
                            scaleOut(targetScale = 1.07f, animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing))
                )
        },
        label = "CinematicScreenTransition"
    ) { destination ->
        when (destination) {
            AppDestination.Splash -> {
                SplashScreen(
                    onTimeout = {
                        currentDestination = AppDestination.ModeSelection
                    }
                )
            }
            AppDestination.ModeSelection -> {
                ModeSelectionScreen(
                    onSelectLocal = {
                        AudioFxManager.userManuallyEnabledEqInOnline = false
                        currentDestination = AppDestination.LocalMain
                    },
                    onSelectOnline = {
                        currentDestination = AppDestination.OnlinePlay
                    }
                )
            }
            AppDestination.LocalMain -> {
                MainScaffold(
                    onSwitchMode = {
                        currentDestination = AppDestination.ModeSelection
                    }
                )
            }
            AppDestination.OnlinePlay -> {
                OnlinePlayScreen(
                    onSwitchMode = {
                        currentDestination = AppDestination.ModeSelection
                    }
                )
            }
        }
    }

    // Graceful permission educational prompt for Live Visualizer
    if (showRecordAudioRationale) {
        RecordAudioRationaleDialog(
            onConfirm = {
                showRecordAudioRationale = false
                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = {
                showRecordAudioRationale = false
                VisualizerManager.updatePermission(false)
            }
        )
    }
}

@Composable
private fun RecordAudioRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = LocalThemeColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.2.dp, themeColors.primaryGlow, RoundedCornerShape(20.dp)),
            color = SurfaceDark,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(themeColors.secondaryAccent.copy(alpha = 0.15f))
                        .border(1.dp, themeColors.secondaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = themeColors.secondaryAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "ENABLE LIVE VISUALIZER",
                    color = MetallicText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Android requires the Audio Recording permission exclusively to capture frequency data from ExoPlayer for drawing real-time neon visualizer waves on your screen.\n\nLunaria strictly does not record, transmit, or store any microphone audio.",
                    color = MetallicMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "NOT NOW",
                            color = CopyrightColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryGlow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "ENABLE",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
