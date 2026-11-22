package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.feature.player.model.DecoderTrackType
import dev.anilbeesetti.nextplayer.feature.player.model.descriptionRes
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import dev.anilbeesetti.nextplayer.feature.player.model.selectableDecoderModes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoMode: DecoderMode?,
    audioMode: DecoderMode?,
    onVideoModeSelected: (DecoderMode) -> Unit,
    onAudioModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit = {},
) {
    var selectedTrackType by rememberSaveable { mutableStateOf(DecoderTrackType.VIDEO) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_decoders),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DecoderTrackTypeSelector(
                selectedTrackType = selectedTrackType,
                onTrackTypeSelected = { selectedTrackType = it },
            )
            DecoderModeGroup(
                trackType = selectedTrackType,
                selectedMode = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> videoMode
                    DecoderTrackType.AUDIO -> audioMode
                },
                onModeSelected = { mode ->
                    when (selectedTrackType) {
                        DecoderTrackType.VIDEO -> onVideoModeSelected(mode)
                        DecoderTrackType.AUDIO -> onAudioModeSelected(mode)
                    }
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun DecoderTrackTypeSelector(
    selectedTrackType: DecoderTrackType,
    onTrackTypeSelected: (DecoderTrackType) -> Unit,
) {
    val trackTypes = DecoderTrackType.entries
    val selectedIndex = trackTypes.indexOf(selectedTrackType).coerceAtLeast(0)
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .glassPrism(
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White.copy(alpha = 0.08f),
                borderAlpha = 0.28f,
                borderWidth = 1.dp,
            )
            .padding(3.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val segmentWidth = maxWidth / trackTypes.size
            val indicatorOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "decoder_track_pill_offset",
            )

            // Smooth iOS-style sliding Glass Prism pill behind the tabs
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .glassPrism(
                        shape = RoundedCornerShape(13.dp),
                        containerColor = accentColor.copy(alpha = 0.32f),
                        borderAlpha = 0.62f,
                        borderWidth = 1.dp,
                        selected = true,
                        accentColor = accentColor,
                    ),
            )

            // Strictly locked 50/50 split tabs with identical bounds & zero resize jitter
            Row(modifier = Modifier.fillMaxSize()) {
                trackTypes.forEach { trackType ->
                    val isSelected = selectedTrackType == trackType
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                        animationSpec = tween(durationMillis = 160),
                        label = "decoder_tab_color",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(13.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) { onTrackTypeSelected(trackType) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(
                                when (trackType) {
                                    DecoderTrackType.VIDEO -> R.string.video
                                    DecoderTrackType.AUDIO -> R.string.audio
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = textColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecoderModeGroup(
    trackType: DecoderTrackType,
    selectedMode: DecoderMode?,
    onModeSelected: (DecoderMode) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        selectableDecoderModes.forEach { mode ->
            DecoderModeRow(
                trackType = trackType,
                mode = mode,
                selected = mode == (selectedMode ?: DecoderMode.AUTO),
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun DecoderModeRow(
    trackType: DecoderTrackType,
    mode: DecoderMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val titleText = if (trackType == DecoderTrackType.AUDIO) {
        when (mode) {
            DecoderMode.AUTO -> "Auto (Default)"
            DecoderMode.HARDWARE -> "HW (System Audio)"
            DecoderMode.SOFTWARE -> "SW (Android Codec)"
            DecoderMode.FFMPEG -> "FFmpeg (Dolby / EAC3 / DTS)"
        }
    } else {
        stringResource(mode.labelRes)
    }
    val descText = if (trackType == DecoderTrackType.AUDIO) {
        when (mode) {
            DecoderMode.AUTO -> "Automatically selects best hardware or FFmpeg audio decoder"
            DecoderMode.HARDWARE -> "Direct device hardware audio decoder"
            DecoderMode.SOFTWARE -> "Standard Android software audio decoder"
            DecoderMode.FFMPEG -> "Built-in multi-channel audio engine for AC3, EAC3, DTS, AAC, TrueHD"
        }
    } else {
        stringResource(mode.descriptionRes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleBounce(pressedScale = 0.98f)
            .glassPrism(
                shape = RoundedCornerShape(16.dp),
                containerColor = if (selected) {
                    accentColor.copy(alpha = 0.24f)
                } else {
                    Color.White.copy(alpha = 0.08f)
                },
                borderAlpha = if (selected) 0.55f else 0.26f,
                selected = selected,
                accentColor = accentColor,
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor,
                unselectedColor = Color.White.copy(alpha = 0.65f),
            ),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = Color.White,
            )
            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = if (selected) 0.85f else 0.68f),
            )
        }
    }
}
