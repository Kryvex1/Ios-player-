package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    videoQualityBadge: String? = null,
    showStatusBar: Boolean = false,
    onBackClick: () -> Unit = {},
    onDecoderClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
) {
    val firstControlFocusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val decoderDescription = stringResource(R.string.select_decoders)

    // True horizontal edge-to-edge width across the camera notch, sitting tightly right beneath
    // the system status bar when "Show status bar" is ON with no wasted vertical gap, and collapsing when OFF.
    val targetTopPadding = when {
        showStatusBar && isLandscape -> if (statusBarTop > 0.dp) {
            statusBarTop.coerceIn(22.dp, 26.dp)
        } else {
            24.dp
        }
        showStatusBar -> if (statusBarTop > 0.dp) {
            statusBarTop + 2.dp
        } else {
            26.dp
        }
        isLandscape -> 8.dp
        else -> 12.dp
    }
    val topPadding by animateDpAsState(
        targetValue = targetTopPadding,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "controls_top_status_bar_padding",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 8.dp)
            .padding(horizontal = 14.dp)
            .focusProperties {
                onEnter = { firstControlFocusRequester.requestFocus() }
            }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlayerButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.navigate_up),
                tint = Color.White,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    role = Role.Button,
                    onClick = onTitleClick,
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(lineBreak = LineBreak.Heading),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!videoQualityBadge.isNullOrBlank()) {
                Box(
                    modifier = Modifier.glassPrism(
                        shape = RoundedCornerShape(8.dp),
                        containerColor = Color.White.copy(alpha = 0.14f),
                        borderAlpha = 0.36f,
                        borderWidth = 0.85.dp,
                    ),
                ) {
                    Text(
                        text = videoQualityBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerButton(
                modifier = Modifier
                    .semantics { contentDescription = decoderDescription }
                    .focusRequester(firstControlFocusRequester),
                onClick = onDecoderClick,
            ) {
                Text(
                    text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                )
            }
            PlayerButton(onClick = onPlaylistClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_playlist),
                    contentDescription = stringResource(R.string.now_playing),
                    tint = Color.White,
                )
            }
            PlayerButton(onClick = onAudioClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_audio_track),
                    contentDescription = stringResource(R.string.select_audio_track),
                    tint = Color.White,
                )
            }
            PlayerButton(onClick = onSubtitleClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_subtitle_track),
                    contentDescription = stringResource(R.string.select_subtitle_track),
                    tint = Color.White,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ControlsTopViewPreview() {
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsTopView(
                title = "Title",
                videoDecoderMode = DecoderMode.HARDWARE,
                showStatusBar = true,
                onBackClick = {},
            )
        }
    }
}
