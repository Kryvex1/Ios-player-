package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPlaylistState
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaylistView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val playlistState = rememberPlaylistState(player)
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        playlistState.moveItem(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    // Auto-scroll to current item when playlist opens
    LaunchedEffect(show) {
        if (show && playlistState.timeline.windowCount != 0) {
            val currentIndex = playlistState.currentMediaItemIndex
            if (currentIndex < playlistState.mediaItemCount) {
                lazyListState.scrollToItem(currentIndex)
            }
        }
    }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.now_playing),
        onDismiss = onDismiss,
    ) {
        if (playlistState.mediaItemCount == 0) {
            EmptyPlaylistView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    count = playlistState.mediaItemCount,
                    key = { index -> playlistState.getMediaItemAt(index).mediaId },
                ) { index ->
                    val mediaItem = playlistState.getMediaItemAt(index)
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = mediaItem.mediaId,
                    ) {
                        val isCurrentItem = index == playlistState.currentMediaItemIndex
                        PlaylistItemView(
                            mediaItem = mediaItem,
                            isCurrentItem = isCurrentItem,
                            canDelete = playlistState.mediaItemCount > 1,
                            onClick = { playlistState.seekToItem(index) },
                            onDelete = { playlistState.removeItem(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PlaylistItemView(
    mediaItem: MediaItem,
    isCurrentItem: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(16.dp)
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleBounce(pressedScale = 0.98f)
            .glassPrism(
                shape = cardShape,
                containerColor = if (isCurrentItem) {
                    accentColor.copy(alpha = 0.24f)
                } else {
                    Color.White.copy(alpha = 0.08f)
                },
                borderAlpha = if (isCurrentItem) 0.55f else 0.26f,
                selected = isCurrentItem,
                accentColor = accentColor,
            )
            .draggableHandle(
                onDragStarted = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                },
                onDragStopped = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                },
                interactionSource = interactionSource,
                dragGestureDetector = DragGestureDetector.LongPress,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = stringResource(R.string.reorder),
            tint = if (isCurrentItem) accentColor else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )

        ThumbnailView(
            mediaItem = mediaItem,
            modifier = Modifier
                .width(min(92.dp, LocalConfiguration.current.screenWidthDp.dp * 0.26f)),
        )

        Text(
            text = mediaItem.mediaMetadata.title?.toString() ?: stringResource(R.string.unknown),
            maxLines = 2,
            style = MaterialTheme.typography.titleSmall.copy(
                lineBreak = LineBreak.Heading,
                fontWeight = if (isCurrentItem) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (canDelete) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .pressScaleBounce(pressedScale = 0.88f)
                    .glassPrism(
                        shape = CircleShape,
                        containerColor = Color.White.copy(alpha = 0.10f),
                        borderAlpha = 0.28f,
                    )
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.remove),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun ThumbnailView(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
) {
    val context = LocalContext.current
    val localThumbData = remember(mediaItem.mediaId) {
        val id = mediaItem.mediaId.trim()
        val isLocal = id.startsWith("content://", ignoreCase = true) ||
            id.startsWith("file://", ignoreCase = true) ||
            id.startsWith("/")
        if (isLocal) id else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .aspectRatio(16f / 10f),
    ) {
        Icon(
            imageVector = NextIcons.Video,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.45f),
        )

        // Only decode thumbnails for local media; never let Coil HTTP-download remote video streams.
        if (localThumbData != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(localThumbData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        mediaItem.mediaMetadata.durationMs?.let { durationMs ->
            if (durationMs > 0) {
                Text(
                    text = Utils.formatDurationMillis(durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(vertical = 1.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = NextIcons.Video,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.no_videos_in_queue),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}
