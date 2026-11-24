package com.lunaria.musicplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunaria.musicplayer.ui.theme.PrimaryGlow
import com.lunaria.musicplayer.ui.theme.SecondaryAccent
import com.lunaria.musicplayer.ui.theme.SurfaceDark

val AlphabetList = listOf(
    '#', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
)

@Composable
fun FastScroller(
    modifier: Modifier = Modifier,
    onLetterSelected: (Char) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var columnHeightPx by remember { mutableStateOf(1f) }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Floating preview bubble while scrubbing
        AnimatedVisibility(
            visible = isDragging && activeLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(2.dp, SecondaryAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter?.toString() ?: "",
                    color = SecondaryAccent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Alphabet letter strip
        Column(
            modifier = Modifier
                .width(22.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(vertical = 4.dp)
                .onGloballyPositioned { coordinates ->
                    columnHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(AlphabetList) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            resolveLetter(offset.y, columnHeightPx)?.let { char ->
                                activeLetter = char
                                onLetterSelected(char)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            activeLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            resolveLetter(change.position.y, columnHeightPx)?.let { char ->
                                if (char != activeLetter) {
                                    activeLetter = char
                                    onLetterSelected(char)
                                }
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlphabetList.forEach { letter ->
                val isSelected = letter == activeLetter
                Text(
                    text = letter.toString(),
                    color = if (isSelected) SecondaryAccent else Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun resolveLetter(y: Float, totalHeight: Float): Char? {
    if (totalHeight <= 0) return null
    val clampedY = y.coerceIn(0f, totalHeight)
    val ratio = clampedY / totalHeight
    val index = (ratio * AlphabetList.size).toInt().coerceIn(0, AlphabetList.size - 1)
    return AlphabetList[index]
}

