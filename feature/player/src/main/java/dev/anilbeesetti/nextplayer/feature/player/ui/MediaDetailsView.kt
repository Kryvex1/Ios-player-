package dev.anilbeesetti.nextplayer.feature.player.ui

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.common.cache.CachedAudioTrack
import dev.anilbeesetti.nextplayer.core.common.cache.CachedSubtitleTrack
import dev.anilbeesetti.nextplayer.core.common.cache.CachedVideoSpecs
import dev.anilbeesetti.nextplayer.core.common.cache.MediaMetadataCache
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import io.github.anilbeesetti.nextlib.mediainfo.MediaInfoBuilder
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@kotlin.OptIn(ExperimentalLayoutApi::class)
@OptIn(UnstableApi::class)
@Composable
fun BoxScope.MediaDetailsView(
    show: Boolean,
    player: Player,
    title: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val durationMs = remember(player.duration) { player.duration.coerceAtLeast(0L) }
    val durationFormatted = remember(durationMs) { durationMs.milliseconds.formatted() }
    val videoSize = player.videoSize
    val resolutionLabel = remember(videoSize) {
        val w = videoSize.width
        val h = videoSize.height
        val shortEdge = if (w > 0 && h > 0) minOf(w, h) else h
        if (shortEdge > 0) "${shortEdge}p" else null
    }

    val mediaItem = player.currentMediaItem
    val mediaUri = remember(mediaItem) {
        mediaItem?.localConfiguration?.uri
            ?: mediaItem?.mediaId?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }
    val isLocalUri = remember(mediaUri) {
        val scheme = mediaUri?.scheme?.lowercase()
        scheme == null || scheme == "file" || scheme == "content"
    }
    val cacheKey = remember(mediaUri, mediaItem) {
        mediaUri?.toString() ?: mediaItem?.mediaId.orEmpty()
    }

    // 1. Instant track info directly from ExoPlayer (0 extra network bytes)
    val trackSpecs = remember(player.currentTracks) {
        val videoGroup = player.currentTracks.groups.firstOrNull {
            it.type == C.TRACK_TYPE_VIDEO && it.isSelected
        } ?: player.currentTracks.groups.firstOrNull {
            it.type == C.TRACK_TYPE_VIDEO
        }
        val videoFormat = if ((videoGroup?.length ?: 0) > 0) videoGroup?.getTrackFormat(0) else null
        val fps = (0 until (videoGroup?.length ?: 0))
            .map { videoGroup!!.getTrackFormat(it).frameRate }
            .firstOrNull { it > 0f }

        val audioTracks = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
            .flatMap { group ->
                (0 until group.length).map { idx ->
                    val fmt = group.getTrackFormat(idx)
                    CachedAudioTrack(
                        title = fmt.label,
                        codec = fmt.sampleMimeType?.substringAfter("audio/").orEmpty(),
                        language = fmt.language,
                        channels = fmt.channelCount.coerceAtLeast(0),
                        channelLayout = null,
                        sampleRate = fmt.sampleRate.coerceAtLeast(0),
                        bitRate = fmt.bitrate.toLong().coerceAtLeast(0L),
                    )
                }
            }

        val subtitleTracks = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .flatMap { group ->
                (0 until group.length).map { idx ->
                    val fmt = group.getTrackFormat(idx)
                    CachedSubtitleTrack(
                        title = fmt.label,
                        codec = fmt.sampleMimeType?.substringAfter("/").orEmpty(),
                        language = fmt.language,
                    )
                }
            }

        CachedVideoSpecs(
            fps = fps,
            videoCodec = videoFormat?.sampleMimeType?.substringAfter("video/"),
            videoBitrate = videoFormat?.bitrate?.toLong()?.coerceAtLeast(0L) ?: 0L,
            format = videoFormat?.containerMimeType?.substringAfter("/"),
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
        )
    }

    val trackFrameRate = trackSpecs.fps

    // 2. Instant cached specs for local files only
    val initialCached = remember(cacheKey, isLocalUri) {
        if (isLocalUri && cacheKey.isNotBlank()) MediaMetadataCache.get(context, cacheKey) else null
    }

    // 3. Fast FPS + Metadata resolver (strictly local files; NEVER starts a second network download)
    val specs by produceState<CachedVideoSpecs?>(
        initialValue = initialCached ?: trackSpecs,
        key1 = cacheKey,
        key2 = show,
        key3 = trackSpecs,
    ) {
        if (!isLocalUri) {
            value = trackSpecs
            return@produceState
        }
        if (cacheKey.isBlank()) return@produceState
        val cached = MediaMetadataCache.get(context, cacheKey)
        if (cached != null) {
            value = cached
            if (cached.fps != null && cached.fps!! > 0f && (cached.audioTracks.isNotEmpty() || cached.videoBitrate > 0L)) {
                return@produceState
            }
        }
        if (!show) return@produceState
        val uri = mediaUri ?: return@produceState

        val fastFps = trackFrameRate ?: cached?.fps ?: withContext(Dispatchers.IO) {
            runCatching {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(context, uri, null)
                    var fps: Float? = null
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                        if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            fps = runCatching { format.getFloat(MediaFormat.KEY_FRAME_RATE) }
                                .getOrElse { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                            if (fps != null && fps > 0f) break
                        }
                    }
                    fps
                } finally {
                    extractor.release()
                }
            }.getOrNull()
        }

        if (fastFps != null && fastFps > 0f) {
            val quickSpecs = (value ?: CachedVideoSpecs()).copy(fps = fastFps)
            value = quickSpecs
            MediaMetadataCache.put(context, cacheKey, quickSpecs)
        }

        val fullSpecs = withContext(Dispatchers.IO) {
            runCatching {
                val info = MediaInfoBuilder().from(context = context, uri = uri).build()
                try {
                    if (info == null) return@runCatching null
                    CachedVideoSpecs(
                        fps = info.videoStream?.frameRate?.toFloat()?.takeIf { it > 0f } ?: fastFps,
                        videoCodec = info.videoStream?.codecName?.takeIf { it.isNotBlank() },
                        videoBitrate = info.videoStream?.bitRate ?: 0L,
                        format = info.format,
                        audioTracks = info.audioStreams.map { stream ->
                            CachedAudioTrack(
                                title = stream.title,
                                codec = stream.codecName,
                                language = stream.language,
                                channels = stream.channels,
                                channelLayout = stream.channelLayout,
                                sampleRate = stream.sampleRate,
                                bitRate = stream.bitRate,
                            )
                        },
                        subtitleTracks = info.subtitleStreams.map { stream ->
                            CachedSubtitleTrack(
                                title = stream.title,
                                codec = stream.codecName,
                                language = stream.language,
                            )
                        },
                    )
                } finally {
                    info?.release()
                }
            }.getOrNull()
        }

        if (fullSpecs != null) {
            value = fullSpecs
            MediaMetadataCache.put(context, cacheKey, fullSpecs)
        }
    }

    val resolvedFrameRate = trackFrameRate ?: specs?.fps
    val frameRateFormatted = remember(resolvedFrameRate) {
        val fps = resolvedFrameRate
        if (fps != null && fps > 0f) {
            val rounded = round(fps * 100f) / 100f
            if (rounded % 1f == 0f) "${rounded.toInt()} FPS" else "$rounded FPS"
        } else {
            null
        }
    }

    val videoCodecLabel = remember(trackSpecs, specs) {
        trackSpecs.videoCodec?.takeIf { it.isNotBlank() }?.uppercase()
            ?: specs?.videoCodec?.takeIf { it.isNotBlank() }?.uppercase()
    }

    val uriString = remember(mediaItem) {
        mediaItem?.localConfiguration?.uri?.path?.takeIf { isLocalUri && it.isNotBlank() }
            ?: mediaItem?.localConfiguration?.uri?.toString()
            ?: ""
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(18.dp)

    OverlayView(
        show = show,
        title = stringResource(R.string.info),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Header Card with Title and Quality/FPS/Codec Glass Prism Badges
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPrism(
                        shape = cardShape,
                        containerColor = Color.White.copy(alpha = 0.09f),
                        borderAlpha = 0.28f,
                    )
                    .padding(14.dp),
            ) {
                Text(
                    text = title.ifBlank { uriString.substringAfterLast("/") },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (resolutionLabel != null || frameRateFormatted != null || videoCodecLabel != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (resolutionLabel != null) {
                            GlassSpecBadge(
                                text = resolutionLabel,
                                tint = accentColor,
                            )
                        }
                        if (frameRateFormatted != null) {
                            GlassSpecBadge(
                                text = frameRateFormatted,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (videoCodecLabel != null) {
                            GlassSpecBadge(
                                text = videoCodecLabel,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            // 2. Video Specs Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPrism(
                        shape = cardShape,
                        containerColor = Color.White.copy(alpha = 0.08f),
                        borderAlpha = 0.26f,
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.video_track),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )

                if (videoSize.width > 0 && videoSize.height > 0) {
                    PlayerDetailItem(
                        icon = NextIcons.Size,
                        label = stringResource(R.string.resolution),
                        value = "${videoSize.width} × ${videoSize.height}",
                    )
                }
                if (frameRateFormatted != null) {
                    PlayerDetailItem(
                        icon = NextIcons.Speed,
                        label = stringResource(R.string.frame_rate),
                        value = frameRateFormatted,
                    )
                }
                if (videoCodecLabel != null) {
                    PlayerDetailItem(
                        icon = NextIcons.Decoder,
                        label = stringResource(R.string.codec),
                        value = videoCodecLabel,
                    )
                }
                val bitrate = specs?.videoBitrate?.takeIf { it > 0L } ?: trackSpecs.videoBitrate.takeIf { it > 0L }
                bitrate?.let { bitRate ->
                    Utils.formatBitrate(bitRate)?.takeIf { it.isNotBlank() }?.let {
                        PlayerDetailItem(
                            icon = NextIcons.Tune,
                            label = stringResource(R.string.bitrate),
                            value = it,
                        )
                    }
                }
                val containerFormat = specs?.format?.takeIf { it.isNotBlank() } ?: trackSpecs.format?.takeIf { it.isNotBlank() }
                containerFormat?.let { format ->
                    PlayerDetailItem(
                        icon = NextIcons.Movie,
                        label = stringResource(R.string.format),
                        value = format.uppercase(),
                    )
                }
                if (durationMs > 0) {
                    PlayerDetailItem(
                        icon = NextIcons.History,
                        label = stringResource(R.string.duration),
                        value = durationFormatted,
                    )
                }
                if (uriString.isNotBlank()) {
                    PlayerDetailItem(
                        icon = NextIcons.Folder,
                        label = stringResource(R.string.location),
                        value = uriString,
                    )
                }
            }

            // 3. Audio Streams Card (If available)
            val audioStreams = specs?.audioTracks?.takeIf { it.isNotEmpty() } ?: trackSpecs.audioTracks
            if (audioStreams.isNotEmpty()) {
                audioStreams.forEachIndexed { idx, stream ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPrism(
                                shape = cardShape,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                borderAlpha = 0.26f,
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val audioTitle = if (audioStreams.size > 1) {
                            "${stringResource(R.string.audio_track)} #${idx + 1}"
                        } else {
                            stringResource(R.string.audio_track)
                        }
                        Text(
                            text = audioTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                        stream.title?.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Title, label = stringResource(R.string.title), value = it)
                        }
                        stream.codec.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Audio, label = stringResource(R.string.codec), value = it.uppercase())
                        }
                        Utils.formatLanguage(stream.language)?.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Language, label = stringResource(R.string.language), value = it)
                        }
                        val channels = stream.channelLayout ?: stream.channels.takeIf { it > 0 }?.let { "$it channels" }
                        channels?.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Headset, label = stringResource(R.string.channels), value = it)
                        }
                        if (stream.sampleRate > 0) {
                            PlayerDetailItem(icon = NextIcons.Speed, label = stringResource(R.string.sample_rate), value = "${stream.sampleRate} Hz")
                        }
                        if (stream.bitRate > 0L) {
                            Utils.formatBitrate(stream.bitRate)?.takeIf { it.isNotBlank() }?.let {
                                PlayerDetailItem(icon = NextIcons.Tune, label = stringResource(R.string.bitrate), value = it)
                            }
                        }
                    }
                }
            }

            // 4. Subtitle Streams Card (If available)
            val subtitleStreams = specs?.subtitleTracks?.takeIf { it.isNotEmpty() } ?: trackSpecs.subtitleTracks
            if (subtitleStreams.isNotEmpty()) {
                subtitleStreams.forEachIndexed { idx, stream ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPrism(
                                shape = cardShape,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                borderAlpha = 0.26f,
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val subTitle = if (subtitleStreams.size > 1) {
                            "${stringResource(R.string.subtitle_track)} #${idx + 1}"
                        } else {
                            stringResource(R.string.subtitle_track)
                        }
                        Text(
                            text = subTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                        stream.title?.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Title, label = stringResource(R.string.title), value = it)
                        }
                        stream.codec.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Subtitle, label = stringResource(R.string.codec), value = it.uppercase())
                        }
                        Utils.formatLanguage(stream.language)?.takeIf { it.isNotBlank() }?.let {
                            PlayerDetailItem(icon = NextIcons.Language, label = stringResource(R.string.language), value = it)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GlassSpecBadge(
    text: String,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .glassPrism(
                shape = RoundedCornerShape(9.dp),
                containerColor = tint.copy(alpha = 0.26f),
                borderAlpha = 0.45f,
                selected = true,
                accentColor = tint,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Composable
private fun PlayerDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .glassPrism(
                    shape = CircleShape,
                    containerColor = Color.White.copy(alpha = 0.12f),
                    borderAlpha = 0.28f,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
            )
        }
    }
}
