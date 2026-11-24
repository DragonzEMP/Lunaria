package com.lunaria.musicplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunaria.musicplayer.ui.components.CosmicParticleEffect
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.LocalThemeColors
import com.lunaria.musicplayer.ui.theme.LunariaPalette
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.SurfaceVariantDark
import com.lunaria.musicplayer.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    onRescanClick: () -> Unit = {},
    onSwitchMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val themeColors = LocalThemeColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        CosmicParticleEffect(
            modifier = Modifier.fillMaxSize(),
            particleCount = 20
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, themeColors.primaryGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = themeColors.secondaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "LUNARIA PREFERENCES",
                            color = MetallicText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Theme Customization & Engine Controls",
                            color = themeColors.secondaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Section 1: Neon Theme Customization
            item {
                SettingsSectionHeader(title = "NEON COLOR PALETTE", icon = Icons.Default.ColorLens)

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeManager.Presets.forEach { palette ->
                        val isSelected = currentTheme.paletteName == palette.title
                        PaletteCard(
                            palette = palette,
                            isSelected = isSelected,
                            onSelect = { ThemeManager.applyPalette(palette) }
                        )
                    }
                }
            }

            // Section 2: Audio DSP Engine Status
            item {
                SettingsSectionHeader(title = "AUDIO ENGINE STATUS", icon = Icons.Default.GraphicEq)

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, themeColors.primaryGlow.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    color = SurfaceDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EngineStatusRow("Equalizer DSP", "5-Band Parametric Hardware DSP Active")
                        Spacer(modifier = Modifier.height(8.dp))
                        EngineStatusRow("3D Surround", "Stereo Virtualizer Widening Active")
                        Spacer(modifier = Modifier.height(8.dp))
                        EngineStatusRow("Bass Boost", "Sub-Bass Harmonic Resonator Active")
                        Spacer(modifier = Modifier.height(8.dp))
                        EngineStatusRow("Live FFT Visualizer", "60 FPS Spectrum Analyzer Hooked")

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onRescanClick,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, themeColors.secondaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = themeColors.secondaryAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RESCAN AUDIO REPOSITORY",
                                color = themeColors.secondaryAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Section: Playback Mode
            if (onSwitchMode != null) {
                item {
                    SettingsSectionHeader(title = "PLAYBACK MODE", icon = Icons.Default.SwapHoriz)

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSwitchMode() }
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(themeColors.primaryGlow, themeColors.secondaryAccent)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        color = SurfaceDark.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Switch Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Choose between Local Play (Offline) and Online Play (YouTube)",
                                    fontSize = 12.sp,
                                    color = MetallicMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Mode",
                                tint = themeColors.secondaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Section 3: About
            item {
                SettingsSectionHeader(title = "ABOUT LUNARIA", icon = Icons.Default.Info)

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = SurfaceDark.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LUNARIA AUDIO ENGINE",
                            color = MetallicText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version ${com.lunaria.musicplayer.BuildConfig.VERSION_NAME} • Studio Master Audio Architecture",
                            color = MetallicMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "© 2026 DRAGONzEMP. ALL RIGHTS RESERVED.",
                            color = CopyrightColor,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalThemeColors.current.secondaryAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MetallicText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun PaletteCard(
    palette: LunariaPalette,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) palette.secondaryAccent else palette.primaryGlow.copy(alpha = 0.25f)
    val glowWidth = if (isSelected) 1.8.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(glowWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        color = SurfaceDark.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Swatch preview dual pill
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(palette.primaryGlow, palette.secondaryAccent)
                            )
                        )
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = palette.title,
                    color = if (isSelected) palette.secondaryAccent else MetallicText,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.secondaryAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        tint = SurfaceDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineStatusRow(label: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MetallicText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = detail, color = CopyrightColor, fontSize = 11.sp)
    }
}

