package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.liquidGlass
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.buttons.LoopButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlaybackSpeedButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButtonBlackAlpha
import dev.anilbeesetti.nextplayer.feature.player.buttons.RotateButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.ShuffleButton
import dev.anilbeesetti.nextplayer.feature.player.extensions.drawableRes
import dev.anilbeesetti.nextplayer.feature.player.state.ChaptersState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberChaptersState
import dev.anilbeesetti.nextplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.anilbeesetti.nextplayer.feature.player.ui.titleOrDefault

private const val MILLISECONDS_PER_SECOND = 1_000L

@OptIn(UnstableApi::class)
@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player?,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    showRemainingTime: Boolean,
    wavingTimeline: Boolean = false,
    showTimelineGlassBackground: Boolean = false,
    onToggleTimeDisplay: () -> Unit,
    onChaptersClick: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
) {
    // Full edge-to-edge width into camera notch/cutout area; keep only bottom nav-bar clearance
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val timeButtonFocusRequester = remember { FocusRequester() }
    val glassShape = RoundedCornerShape(22.dp)

    val containerModifier = if (showTimelineGlassBackground) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = (navBarBottom + 8.dp).coerceAtLeast(10.dp))
            .liquidGlass(
                shape = glassShape,
                blurRadius = 6.dp,
                refractionHeight = 10.dp,
                refractionAmount = 16.dp,
                scrimColor = Color.Black.copy(alpha = 0.26f),
                fallbackColor = Color(0xFF10131A).copy(alpha = 0.66f),
                highlightAlpha = 0.44f,
                highlightWidth = 1.dp,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = (navBarBottom + 8.dp).coerceAtLeast(10.dp))
    }

    Column(
        modifier = containerModifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { timeButtonFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerButton(
                    modifier = Modifier.focusRequester(timeButtonFocusRequester),
                    onClick = onToggleTimeDisplay,
                    containerColor = PlayerButtonBlackAlpha,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    val timeTextPositionMs by remember(progressState) {
                        derivedStateOf {
                            val wholeSeconds = progressState.currentPositionMs.coerceAtLeast(0L) / MILLISECONDS_PER_SECOND
                            wholeSeconds * MILLISECONDS_PER_SECOND
                        }
                    }
                    Text(
                        text = buildString {
                            val hasValidDuration = progressState.durationMs != C.TIME_UNSET && progressState.durationMs > 0L
                            val positionText = when (showRemainingTime) {
                                true -> if (hasValidDuration) {
                                    val remainingMs = timeTextPositionMs - progressState.durationMs
                                    getStringForTime(remainingMs)
                                } else {
                                    getStringForTime(C.TIME_UNSET)
                                }

                                false -> getStringForTime(timeTextPositionMs)
                            }
                            append(positionText)
                            append(" / ")
                            append(getStringForTime(if (hasValidDuration) progressState.durationMs else C.TIME_UNSET))
                        },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }

                if (chaptersState.chapters.isNotEmpty()) {
                    PlayerButton(
                        modifier = Modifier.weight(1f, fill = false),
                        onClick = onChaptersClick,
                        containerColor = PlayerButtonBlackAlpha,
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 10.dp).copy(end = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = chaptersState.chapters.getOrNull(chaptersState.currentChapterIndex)
                                    ?.titleOrDefault(chaptersState.currentChapterIndex)
                                    ?: stringResource(R.string.chapters),
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackSpeedButton(
                    player = player,
                    onClick = onPlaybackSpeedClick,
                )

                if (!isTv) {
                    RotateButton()
                }
            }
        }

        val isNetworkStream = remember(player?.currentMediaItem) {
            val uri = player?.currentMediaItem?.localConfiguration?.uri
                ?: player?.currentMediaItem?.mediaId?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
            val scheme = uri?.scheme?.lowercase()
            scheme != null && scheme != "file" && scheme != "content"
        }
        var liveBufferedPositionMs by remember(player, isNetworkStream) {
            mutableLongStateOf(if (isNetworkStream) (player?.bufferedPosition ?: 0L) else 0L)
        }
        var isPlaying by remember(player) {
            mutableStateOf(player?.isPlaying == true)
        }
        DisposableEffect(player) {
            if (player == null) return@DisposableEffect onDispose {}
            isPlaying = player.isPlaying
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
            }
        }
        LaunchedEffect(player, isNetworkStream) {
            if (player != null && isNetworkStream) {
                while (true) {
                    liveBufferedPositionMs = player.bufferedPosition
                    kotlinx.coroutines.delay(250L)
                }
            } else {
                liveBufferedPositionMs = 0L
            }
        }
        val hasKnownDuration = progressState.durationMs != C.TIME_UNSET && progressState.durationMs > 0L
        val safePositionMs = if (hasKnownDuration) progressState.currentPositionMs.coerceAtLeast(0L) else 0L
        val bufferedPosition = if (isNetworkStream && hasKnownDuration) {
            maxOf(liveBufferedPositionMs, player?.bufferedPosition ?: 0L, safePositionMs)
                .coerceAtMost(progressState.durationMs)
                .toFloat()
        } else {
            0f
        }
        PlayerSeekbar(
            position = safePositionMs.toFloat(),
            duration = if (hasKnownDuration) progressState.durationMs.toFloat() else 0f,
            bufferedPosition = bufferedPosition,
            wavingTimeline = wavingTimeline,
            isPlaying = isPlaying,
            chapters = chaptersState.chapters,
            onSeek = { if (hasKnownDuration) onSeek(it.toLong()) },
            onSeekFinished = { if (hasKnownDuration) onSeekEnd() },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = controlsAlignment),
        ) {
            PlayerButton(onClick = onLockControlsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_open),
                    contentDescription = stringResource(R.string.controls_lock),
                    tint = Color.White,
                )
            }
            PlayerButton(
                onClick = onVideoContentScaleClick,
                onLongClick = onVideoContentScaleLongClick,
            ) {
                Icon(
                    painter = painterResource(videoContentScale.drawableRes()),
                    contentDescription = stringResource(R.string.video_zoom),
                    tint = Color.White,
                )
            }
            if (isPipSupported) {
                PlayerButton(onClick = onPictureInPictureClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pip),
                        contentDescription = stringResource(R.string.pip_settings),
                        tint = Color.White,
                    )
                }
            }
            LoopButton(player = player)
            ShuffleButton(player = player)
        }
    }
}

@OptIn(UnstableApi::class)
@Preview
@Composable
private fun ControlsBottomViewPreview() {
    val player = rememberPreviewPlayer()
    val progressState = rememberProgressStateWithTickInterval(player)
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsBottomView(
                player = player,
                progressState = progressState,
                chaptersState = rememberChaptersState(player, progressState),
                controlsAlignment = Alignment.Start,
                videoContentScale = VideoContentScale.BEST_FIT,
                isPipSupported = true,
                showRemainingTime = false,
                showTimelineGlassBackground = false,
                onToggleTimeDisplay = {},
                onChaptersClick = {},
                onVideoContentScaleClick = {},
                onVideoContentScaleLongClick = {},
                onLockControlsClick = {},
                onPictureInPictureClick = {},
                onPlaybackSpeedClick = {},
                onPlayInBackgroundClick = {},
                onSeek = {},
                onSeekEnd = {},
            )
        }
    }
}
