package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPlaybackParametersState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val playbackParametersState = rememberPlaybackParametersState(player)
    val accentColor = MaterialTheme.colorScheme.primary

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_playback_speed),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val currentSpeed = playbackParametersState.speed
            val minSpeed = 0.25f
            val maxSpeed = 4.0f
            val step = 0.05f

            // 1. Unified Glass Prism Speed Controller Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPrism(
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color.White.copy(alpha = 0.09f),
                        borderAlpha = 0.28f,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pressScaleBounce(pressedScale = 0.88f)
                            .glassPrism(
                                shape = CircleShape,
                                containerColor = Color.White.copy(alpha = 0.14f),
                                borderAlpha = 0.38f,
                            )
                            .clickable {
                                val newSpeed = (currentSpeed - step).coerceAtLeast(minSpeed)
                                val rounded = (round(newSpeed * 20f) / 20f)
                                playbackParametersState.setPlaybackSpeed(rounded)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_remove),
                            contentDescription = stringResource(R.string.playback_speed_slower),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f×", currentSpeed),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                            ),
                            color = Color.White,
                        )

                        val isNormal = abs(currentSpeed - 1.0f) < 0.01f
                        if (!isNormal) {
                            Row(
                                modifier = Modifier
                                    .pressScaleBounce(pressedScale = 0.90f)
                                    .glassPrism(
                                        shape = CircleShape,
                                        containerColor = accentColor.copy(alpha = 0.30f),
                                        borderAlpha = 0.55f,
                                        selected = true,
                                        accentColor = accentColor,
                                    )
                                    .clickable {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        playbackParametersState.setPlaybackSpeed(1.0f)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_reset),
                                    contentDescription = stringResource(R.string.reset_speed),
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "1.0×",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pressScaleBounce(pressedScale = 0.88f)
                            .glassPrism(
                                shape = CircleShape,
                                containerColor = Color.White.copy(alpha = 0.14f),
                                borderAlpha = 0.38f,
                            )
                            .clickable {
                                val newSpeed = (currentSpeed + step).coerceAtMost(maxSpeed)
                                val rounded = (round(newSpeed * 20f) / 20f)
                                playbackParametersState.setPlaybackSpeed(rounded)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.playback_speed_faster),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                CleanSpeedSliderTrack(
                    value = currentSpeed.coerceIn(minSpeed, maxSpeed),
                    minSpeed = minSpeed,
                    maxSpeed = maxSpeed,
                    onValueChange = { rawValue ->
                        val snapped = (round(rawValue * 20f) / 20f).coerceIn(minSpeed, maxSpeed)
                        if (abs(snapped - currentSpeed) >= 0.05f) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        playbackParametersState.setPlaybackSpeed(snapped)
                    },
                )
            }

            // 2. Compact Glass Prism Preset Speed Pills
            val presetSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presetSpeeds.forEach { speed ->
                    val isSelected = abs(currentSpeed - speed) < 0.01f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .pressScaleBounce(pressedScale = 0.92f)
                            .glassPrism(
                                shape = RoundedCornerShape(11.dp),
                                containerColor = if (isSelected) {
                                    accentColor.copy(alpha = 0.34f)
                                } else {
                                    Color.White.copy(alpha = 0.09f)
                                },
                                borderAlpha = if (isSelected) 0.62f else 0.26f,
                                selected = isSelected,
                                accentColor = accentColor,
                            )
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                playbackParametersState.setPlaybackSpeed(speed)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0×" else "${speed}×",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
                        )
                    }
                }
            }

            // 3. Skip Silence Toggle Card with Glass Prism NextSwitch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPrism(
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color.White.copy(alpha = 0.09f),
                        borderAlpha = 0.28f,
                    )
                    .toggleable(
                        value = playbackParametersState.skipSilenceEnabled,
                        onValueChange = { playbackParametersState.setIsSkipSilenceEnabled(it) },
                        role = Role.Switch,
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .glassPrism(
                            shape = CircleShape,
                            containerColor = accentColor.copy(alpha = 0.22f),
                            borderAlpha = 0.36f,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = NextIcons.Audio,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.skip_silence),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.skip_silence_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                }

                NextSwitch(
                    checked = playbackParametersState.skipSilenceEnabled,
                    onCheckedChange = null,
                )
            }
        }
    }
}

@Composable
private fun CleanSpeedSliderTrack(
    value: Float,
    minSpeed: Float,
    maxSpeed: Float,
    onValueChange: (Float) -> Unit,
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val thumbSize = 18.dp
    val thumbRadiusPx = with(density) { (thumbSize / 2).toPx() }
    val fraction = ((value - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0f, 1f)

    fun updateFromX(xPx: Float) {
        if (trackWidthPx <= 0) return
        val newFraction = (xPx / trackWidthPx.toFloat()).coerceIn(0f, 1f)
        val newValue = minSpeed + newFraction * (maxSpeed - minSpeed)
        onValueChange(newValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(minSpeed, maxSpeed) {
                    detectTapGestures { offset ->
                        updateFromX(offset.x)
                    }
                }
                .pointerInput(minSpeed, maxSpeed) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        updateFromX(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            if (trackWidthPx > 0) {
                val thumbOffsetX = (fraction * trackWidthPx - thumbRadiusPx)
                    .roundToInt()
                    .coerceIn(0, (trackWidthPx - thumbRadiusPx * 2).roundToInt().coerceAtLeast(0))
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetX, 0) }
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0.25×",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
            Text(
                text = "1.0×",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
            Text(
                text = "4.0×",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}
