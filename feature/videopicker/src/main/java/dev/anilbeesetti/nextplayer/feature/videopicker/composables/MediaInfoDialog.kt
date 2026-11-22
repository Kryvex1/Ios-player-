package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.model.AudioStreamInfo
import dev.anilbeesetti.nextplayer.core.model.MediaInfo
import dev.anilbeesetti.nextplayer.core.model.SubtitleStreamInfo
import dev.anilbeesetti.nextplayer.core.model.VideoStreamInfo
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.GlassDialogButton
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.adaptiveGlassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import kotlin.math.round
import kotlinx.coroutines.launch

@Composable
fun MediaInfoDialog(
    mediaInfo: MediaInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    if (isTv) {
        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val video = mediaInfo.video
    val videoStream = mediaInfo.videoStream

    // Resolution calculation
    val resolutionStr = if (videoStream != null && videoStream.frameWidth > 0 && videoStream.frameHeight > 0) {
        "${videoStream.frameWidth} × ${videoStream.frameHeight}"
    } else null

    val qualityPill = if (videoStream != null && videoStream.frameHeight > 0) {
        val shortEdge = if (videoStream.frameWidth > 0) minOf(videoStream.frameWidth, videoStream.frameHeight) else videoStream.frameHeight
        "${shortEdge}p"
    } else null

    // FPS calculation
    val fpsStr = if (videoStream != null && videoStream.frameRate > 0) {
        val rounded = round(videoStream.frameRate * 100) / 100
        if (rounded % 1.0 == 0.0) "${rounded.toInt()} FPS" else "$rounded FPS"
    } else null

    NextDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.info),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(vertical = 2.dp)
                    .thenIf(isTv) {
                        focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                val amount = (scrollState.viewportSize * 0.8f).takeIf { it > 0f } ?: 400f
                                when (event.key) {
                                    Key.DirectionDown -> if (scrollState.canScrollForward) {
                                        scope.launch { scrollState.animateScrollBy(amount) }
                                        true
                                    } else {
                                        false
                                    }
                                    Key.DirectionUp -> if (scrollState.canScrollBackward) {
                                        scope.launch { scrollState.animateScrollBy(-amount) }
                                        true
                                    } else {
                                        false
                                    }
                                    else -> false
                                }
                            }
                    },
            ) {
                // 1. Header Card with File Name and Quality/FPS Badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .adaptiveGlassPrism(shape = RoundedCornerShape(16.dp)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = video.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (qualityPill != null || fpsStr != null || videoStream?.codecName?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (qualityPill != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = qualityPill,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }

                                if (fpsStr != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = fpsStr,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    }
                                }

                                if (videoStream != null && videoStream.codecName.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = videoStream.codecName.uppercase(),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. File Overview Section
                InfoSectionCard(title = stringResource(R.string.file)) {
                    video.nameWithExtension.takeIf { it.isNotBlank() }?.let {
                        InfoDetailRow(icon = NextIcons.Title, label = stringResource(R.string.file), value = it)
                    }
                    video.formattedDuration.takeIf { it.isNotBlank() }?.let {
                        InfoDetailRow(icon = NextIcons.History, label = stringResource(R.string.duration), value = it)
                    }
                    video.formattedFileSize.takeIf { it.isNotBlank() }?.let {
                        InfoDetailRow(icon = NextIcons.Storage, label = stringResource(R.string.size), value = it)
                    }
                    video.parentPath.takeIf { it.isNotBlank() }?.let {
                        InfoDetailRow(icon = NextIcons.Folder, label = stringResource(R.string.location), value = it)
                    }
                    video.format?.takeIf { it.isNotBlank() }?.let {
                        InfoDetailRow(icon = NextIcons.Movie, label = stringResource(R.string.format), value = it.uppercase())
                    }
                }

                // 3. Video Stream Section (Shown only if present)
                if (videoStream != null) {
                    InfoSectionCard(title = stringResource(R.string.video_track)) {
                        videoStream.title?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Title, label = stringResource(R.string.title), value = it)
                        }
                        videoStream.codecName.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Decoder, label = stringResource(R.string.codec), value = it.uppercase())
                        }
                        resolutionStr?.let {
                            InfoDetailRow(icon = NextIcons.Size, label = stringResource(R.string.resolution), value = it)
                        }
                        fpsStr?.let {
                            InfoDetailRow(icon = NextIcons.Speed, label = stringResource(R.string.frame_rate), value = it)
                        }
                        Utils.formatBitrate(videoStream.bitRate)?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Tune, label = stringResource(R.string.bitrate), value = it)
                        }
                    }
                }

                // 4. Audio Streams Section (Shown only if tracks exist)
                mediaInfo.audioStreams.forEachIndexed { index, audioStream ->
                    val audioTitle = if (mediaInfo.audioStreams.size > 1) {
                        "${stringResource(R.string.audio_track)} #${index + 1}"
                    } else {
                        stringResource(R.string.audio_track)
                    }
                    InfoSectionCard(title = audioTitle) {
                        audioStream.title?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Title, label = stringResource(R.string.title), value = it)
                        }
                        audioStream.codecName.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Audio, label = stringResource(R.string.codec), value = it.uppercase())
                        }
                        Utils.formatLanguage(audioStream.language)?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Language, label = stringResource(R.string.language), value = it)
                        }
                        val channelStr = audioStream.channelLayout ?: audioStream.channels.takeIf { it > 0 }?.let { "$it channels" }
                        channelStr?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Headset, label = stringResource(R.string.channels), value = it)
                        }
                        if (audioStream.sampleRate > 0) {
                            InfoDetailRow(icon = NextIcons.Speed, label = stringResource(R.string.sample_rate), value = "${audioStream.sampleRate} Hz")
                        }
                        audioStream.sampleFormat?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Style, label = stringResource(R.string.sample_format), value = it)
                        }
                        Utils.formatBitrate(audioStream.bitRate)?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Tune, label = stringResource(R.string.bitrate), value = it)
                        }
                    }
                }

                // 5. Subtitle Streams Section (Shown only if tracks exist)
                mediaInfo.subtitleStreams.forEachIndexed { index, subtitleStream ->
                    val subtitleTitle = if (mediaInfo.subtitleStreams.size > 1) {
                        "${stringResource(R.string.subtitle_track)} #${index + 1}"
                    } else {
                        stringResource(R.string.subtitle_track)
                    }
                    InfoSectionCard(title = subtitleTitle) {
                        subtitleStream.title?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Title, label = stringResource(R.string.title), value = it)
                        }
                        subtitleStream.codecName.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Subtitle, label = stringResource(R.string.codec), value = it.uppercase())
                        }
                        Utils.formatLanguage(subtitleStream.language)?.takeIf { it.isNotBlank() }?.let {
                            InfoDetailRow(icon = NextIcons.Language, label = stringResource(R.string.language), value = it)
                        }
                    }
                }
            }
        },
        confirmButton = {
            GlassDialogButton(
                text = stringResource(id = R.string.okay),
                emphasized = true,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun InfoSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .adaptiveGlassPrism(shape = RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun InfoDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
