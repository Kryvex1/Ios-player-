package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.Chapter
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.feature.player.state.SubtitleOptionsEvent
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: OverlayView?,
    videoDecoderMode: DecoderMode?,
    audioDecoderMode: DecoderMode?,
    videoContentScale: VideoContentScale,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterSelected: (Chapter) -> Unit,
    onDismiss: () -> Unit = {},
    onVideoDecoderModeSelected: (DecoderMode) -> Unit = {},
    onAudioDecoderModeSelected: (DecoderMode) -> Unit = {},
    onSelectSubtitleClick: () -> Unit = {},
    onSelectAudioClick: () -> Unit = {},
    onSubtitleOptionEvent: (SubtitleOptionsEvent) -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Full-screen scrim behind floating panels: consumes ALL pointer events (down, move, up)
    // so tapping outside ONLY dismisses the floating overlay and NEVER leaks through to toggle
    // player controls or gestures underneath.
    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = overlayView != null,
        enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var dismissedOnRelease = false
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                            if (event.changes.all { !it.pressed }) {
                                dismissedOnRelease = true
                                break
                            }
                        }
                        if (dismissedOnRelease) {
                            currentOnDismiss()
                        }
                    }
                },
        )
    }

    AudioTrackSelectorView(
        show = overlayView == OverlayView.AUDIO_SELECTOR,
        player = player,
        onSelectAudioClick = onSelectAudioClick,
        onDismiss = onDismiss,
    )

    DecoderSelectorView(
        show = overlayView == OverlayView.DECODER_SELECTOR,
        videoMode = videoDecoderMode,
        audioMode = audioDecoderMode,
        onVideoModeSelected = onVideoDecoderModeSelected,
        onAudioModeSelected = onAudioDecoderModeSelected,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == OverlayView.SUBTITLE_SELECTOR,
        player = player,
        onSelectSubtitleClick = onSelectSubtitleClick,
        onEvent = onSubtitleOptionEvent,
        onDismiss = onDismiss,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayView.PLAYBACK_SPEED,
        player = player,
        onDismiss = onDismiss,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayView.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )

    PlaylistView(
        show = overlayView == OverlayView.PLAYLIST,
        player = player,
        onDismiss = onDismiss,
    )

    ChaptersView(
        show = overlayView == OverlayView.CHAPTERS,
        chapters = chapters,
        currentChapterIndex = currentChapterIndex,
        onChapterSelected = onChapterSelected,
        onDismiss = onDismiss,
    )

    MediaDetailsView(
        show = overlayView == OverlayView.MEDIA_DETAILS,
        player = player,
        title = player.mediaMetadata.title?.toString() ?: "",
        onDismiss = onDismiss,
    )
}

val Configuration.isPortrait: Boolean
    get() = orientation == Configuration.ORIENTATION_PORTRAIT

enum class OverlayView {
    DECODER_SELECTOR,
    AUDIO_SELECTOR,
    SUBTITLE_SELECTOR,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    PLAYLIST,
    CHAPTERS,
    MEDIA_DETAILS,
}
