package com.lunaria.musicplayer.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunaria.musicplayer.player.EqBand
import com.lunaria.musicplayer.ui.components.CosmicParticleEffect
import com.lunaria.musicplayer.ui.theme.BackgroundDark
import com.lunaria.musicplayer.ui.theme.CopyrightColor
import com.lunaria.musicplayer.ui.theme.LocalThemeColors
import com.lunaria.musicplayer.ui.theme.MetallicMuted
import com.lunaria.musicplayer.ui.theme.MetallicText
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark
import com.lunaria.musicplayer.ui.theme.SurfaceVariantDark
import com.lunaria.musicplayer.ui.theme.ThemeManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = viewModel(),
    onNavigateBack: (direction: Int) -> Unit = {}
) {
    val isEnabled by viewModel.isEnabled.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val bassStrength by viewModel.bassStrength.collectAsState()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val eqWarningCount by ThemeManager.eqWarningShownCount.collectAsState()

    var presetDropdownExpanded by remember { mutableStateOf(false) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    var showWarningDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragOffset = 0f },
                    onDragEnd = {
                        if (abs(horizontalDragOffset) > 100f) {
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onNavigateBack(dir)
                        }
                        horizontalDragOffset = 0f
                    },
                    onDragCancel = { horizontalDragOffset = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        horizontalDragOffset += dragAmount
                        if (abs(horizontalDragOffset) > 120f) {
                            change.consume()
                            val dir = if (horizontalDragOffset > 0) 1 else -1
                            onNavigateBack(dir)
                            horizontalDragOffset = 0f
                        }
                    }
                )
            }
    ) {
        // Celestial background particles
        CosmicParticleEffect(
            modifier = Modifier.fillMaxSize(),
            particleCount = 25
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header: Master Power Switch & Preset Dropdown Selector
            EqualizerHeader(
                isEnabled = isEnabled,
                currentPreset = currentPreset,
                availablePresets = viewModel.availablePresets,
                dropdownExpanded = presetDropdownExpanded,
                onDropdownDismiss = { presetDropdownExpanded = false },
                onDropdownToggle = { presetDropdownExpanded = !presetDropdownExpanded },
                onTogglePower = { enable ->
                    if (enable) {
                        if (eqWarningCount < 3) {
                            showWarningDialog = true
                        } else {
                            viewModel.togglePower(true)
                        }
                    } else {
                        viewModel.togglePower(false)
                    }
                },
                onPresetSelected = {
                    viewModel.selectPreset(it)
                    presetDropdownExpanded = false
                }
            )

            // 2. The Dynamic Vertical Band Sliders
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bands.forEach { band ->
                        VerticalEqSlider(
                            band = band,
                            isEnabled = isEnabled,
                            modifier = Modifier.weight(1f),
                            onLevelChanged = { newDb ->
                                viewModel.setBandLevel(band.index, newDb)
                            }
                        )
                    }
                }
            }

            // 3. The Metallic Rotary Dials (Bass Boost & Surround Virtualizer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Dial: Bass Boost
                RotaryDial(
                    label = "BASS BOOST",
                    value = bassStrength,
                    maxValue = 1000,
                    isEnabled = isEnabled,
                    onValueChanged = { viewModel.setBassStrength(it) }
                )

                // Right Dial: Surround Virtualizer
                RotaryDial(
                    label = "SURROUND",
                    value = virtualizerStrength,
                    maxValue = 1000,
                    isEnabled = isEnabled,
                    onValueChanged = { viewModel.setVirtualizerStrength(it) }
                )
            }
        }

        // 3-Time EQ Development Warning Dialog
        if (showWarningDialog) {
            val themeColors = LocalThemeColors.current
            AlertDialog(
                onDismissRequest = {
                    showWarningDialog = false
                },
                containerColor = themeColors.surfaceDark,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(themeColors.primaryGlow, themeColors.secondaryAccent)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
                title = {
                    Text(
                        text = "Equalizer (In Development)",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "The EQ is currently in development and might not give you a pleasant experience. Are you sure you want to use it?",
                        color = themeColors.metallicText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            ThemeManager.incrementEqWarningCount()
                            showWarningDialog = false
                            viewModel.togglePower(true)
                        }
                    ) {
                        Text(
                            text = "Yes",
                            color = themeColors.secondaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            ThemeManager.incrementEqWarningCount()
                            showWarningDialog = false
                        }
                    ) {
                        Text(
                            text = "No",
                            color = themeColors.metallicMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun EqualizerHeader(
    isEnabled: Boolean,
    currentPreset: String,
    availablePresets: List<String>,
    dropdownExpanded: Boolean,
    onDropdownDismiss: () -> Unit,
    onDropdownToggle: () -> Unit,
    onTogglePower: (Boolean) -> Unit,
    onPresetSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Master Power Switch with neon aura
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(
                        1.dp,
                        if (isEnabled) SecondaryAccent else CopyrightColor.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Master Power",
                    tint = if (isEnabled) SecondaryAccent else CopyrightColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onTogglePower,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SecondaryAccent,
                    checkedTrackColor = PrimaryGlow,
                    uncheckedThumbColor = CopyrightColor,
                    uncheckedTrackColor = SurfaceDark
                )
            )
        }

        // Preset Dropdown Spinner
        Box {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.2.dp,
                        if (isEnabled) PrimaryGlow else CopyrightColor.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = isEnabled) { onDropdownToggle() },
                color = SurfaceDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentPreset,
                        color = if (isEnabled) MetallicText else CopyrightColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Presets",
                        tint = if (isEnabled) SecondaryAccent else CopyrightColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = onDropdownDismiss,
                modifier = Modifier
                    .background(SurfaceDark)
                    .border(1.dp, PrimaryGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                availablePresets.forEach { preset ->
                    val isSelected = preset == currentPreset
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset,
                                    color = if (isSelected) SecondaryAccent else MetallicText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SecondaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
        }
    }
}

/**
 * Custom Vertical Slider styled with Lunaria's theme.
 * Displays the current dB at the top and the frequency label at the bottom.
 */
@Composable
private fun VerticalEqSlider(
    band: EqBand,
    isEnabled: Boolean,
    onLevelChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (isEnabled) 1f else 0.35f
    val minDb = -12f
    val maxDb = 12f

    var internalFraction by remember(band.levelDb) {
        mutableFloatStateOf(((band.levelDb - minDb) / (maxDb - minDb)).coerceIn(0f, 1f))
    }

    Column(
        modifier = modifier.alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current dB value at the top
        val sign = if (band.levelDb > 0.05f) "+" else ""
        Text(
            text = "$sign${String.format("%.1f", band.levelDb)}",
            color = if (band.levelDb != 0f) SecondaryAccent else MetallicMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Vertical Slider Track with Glowing Neon Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val height = size.height.toFloat()
                            val fraction = (1f - (offset.y / height)).coerceIn(0f, 1f)
                            internalFraction = fraction
                            val newDb = minDb + fraction * (maxDb - minDb)
                            onLevelChanged(newDb)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val height = size.height.toFloat()
                            val fraction = (1f - (change.position.y / height)).coerceIn(0f, 1f)
                            internalFraction = fraction
                            val newDb = minDb + fraction * (maxDb - minDb)
                            onLevelChanged(newDb)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 5.dp.toPx()
                val centerX = size.width / 2f
                val topY = 12.dp.toPx()
                val bottomY = size.height - 12.dp.toPx()
                val trackLength = bottomY - topY
                val centerY = topY + trackLength / 2f

                // 1. Background Track
                drawLine(
                    color = SurfaceVariantDark,
                    start = Offset(centerX, topY),
                    end = Offset(centerX, bottomY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )

                // 2. Center Zero-dB Tick Mark
                drawLine(
                    color = CopyrightColor,
                    start = Offset(centerX - 6.dp.toPx(), centerY),
                    end = Offset(centerX + 6.dp.toPx(), centerY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 3. Active Colored Portion from 0 dB to Thumb
                val thumbY = bottomY - internalFraction * trackLength
                val activeBrush = Brush.verticalGradient(
                    colors = listOf(SecondaryAccent, PrimaryGlow),
                    startY = topY,
                    endY = bottomY
                )
                drawLine(
                    brush = activeBrush,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )

                // 4. Glowing Neon Thumb / Knob
                val knobRadius = 7.5.dp.toPx()
                // Outer glow halo
                drawCircle(
                    color = SecondaryAccent.copy(alpha = 0.35f),
                    radius = knobRadius + 3.5.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                // Core metallic knob
                drawCircle(
                    color = SurfaceDark,
                    radius = knobRadius,
                    center = Offset(centerX, thumbY)
                )
                // Glowing border
                drawCircle(
                    color = SecondaryAccent,
                    radius = knobRadius,
                    center = Offset(centerX, thumbY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frequency Label at the bottom
        Text(
            text = band.displayFrequency,
            color = MetallicText,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Large Metallic Circular Rotary Knob with Neon Cyan Indicator Ring.
 * Allows circular or horizontal/vertical touch drag adjustment.
 */
@Composable
private fun RotaryDial(
    label: String,
    value: Int,
    maxValue: Int,
    isEnabled: Boolean,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (isEnabled) 1f else 0.35f
    val fraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)

    val currentOnValueChanged by rememberUpdatedState(onValueChanged)
    val currentMaxValue by rememberUpdatedState(maxValue)

    Column(
        modifier = modifier
            .alpha(alpha)
            .width(130.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    fun updateAngle(x: Float, y: Float) {
                        val dx = x - centerX
                        val dy = y - centerY
                        val rad = atan2(dy.toDouble(), dx.toDouble())
                        var deg = Math.toDegrees(rad).toFloat()
                        if (deg < 0f) deg += 360f

                        // Arc runs clockwise from 135° to 45° (sweep 270°)
                        var relativeAngle = deg - 135f
                        if (relativeAngle < 0f) relativeAngle += 360f

                        if (relativeAngle <= 270f) {
                            val newFraction = (relativeAngle / 270f).coerceIn(0f, 1f)
                            currentOnValueChanged((newFraction * currentMaxValue).toInt())
                        } else {
                            if (relativeAngle > 315f) {
                                currentOnValueChanged(0)
                            } else {
                                currentOnValueChanged(currentMaxValue)
                            }
                        }
                    }

                    detectDragGestures(
                        onDragStart = { offset ->
                            updateAngle(offset.x, offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateAngle(change.position.x, change.position.y)
                        }
                    )
                }
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    detectTapGestures { offset ->
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val rad = atan2(dy.toDouble(), dx.toDouble())
                        var deg = Math.toDegrees(rad).toFloat()
                        if (deg < 0f) deg += 360f

                        var relativeAngle = deg - 135f
                        if (relativeAngle < 0f) relativeAngle += 360f

                        if (relativeAngle <= 270f) {
                            val newFraction = (relativeAngle / 270f).coerceIn(0f, 1f)
                            currentOnValueChanged((newFraction * currentMaxValue).toInt())
                        } else {
                            if (relativeAngle > 315f) currentOnValueChanged(0) else currentOnValueChanged(currentMaxValue)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val diameter = size.minDimension - strokeWidth * 2 - 8.dp.toPx()
                val radius = diameter / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                val startAngle = 135f
                val sweepAngle = 270f
                val activeSweep = sweepAngle * fraction

                // 1. Inactive background arc
                drawArc(
                    color = Color(0xFF4F5970).copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Active Glowing Neon Cyan Arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to PrimaryGlow,
                        0.5f to SecondaryAccent,
                        1.0f to Color.White
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep.coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Metallic Circular Knob Face
                val knobRadius = radius - 10.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4F5970), SurfaceDark),
                        center = centerOffset,
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = centerOffset
                )
                drawCircle(
                    color = PrimaryGlow.copy(alpha = 0.4f),
                    radius = knobRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 4. Glowing Indicator Dot on the knob edge pointing along the angle
                val currentAngleRad = (startAngle + activeSweep) * (PI / 180f)
                val dotDist = knobRadius - 8.dp.toPx()
                val dotX = centerOffset.x + (dotDist * cos(currentAngleRad)).toFloat()
                val dotY = centerOffset.y + (dotDist * sin(currentAngleRad)).toFloat()

                drawCircle(
                    color = SecondaryAccent,
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // Percentage value centered inside the knob
            Text(
                text = "${(fraction * 100).toInt()}%",
                color = SecondaryAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label below dial
        Text(
            text = label,
            color = MetallicText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

