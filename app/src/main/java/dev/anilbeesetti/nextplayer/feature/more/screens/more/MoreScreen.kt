package dev.anilbeesetti.nextplayer.feature.more.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.media.services.MediaOperationsService
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.components.BindTopLevelFab
import dev.anilbeesetti.nextplayer.core.ui.components.GlassIconButton
import dev.anilbeesetti.nextplayer.core.ui.components.LocalNavigationBottomPadding
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabKey
import dev.anilbeesetti.nextplayer.core.ui.components.adaptiveGlassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.core.ui.components.rememberRestorableFocusState
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusGroup
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusItem
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.VideoGridItem

@Composable
fun MoreScreen(
    viewModel: MoreViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun MoreScreenContent(
    state: MoreUiState,
    onAction: (MoreAction) -> Unit,
) {
    BindTopLevelFab(TopLevelFabKey.MORE, NextIcons.Settings) { onAction(MoreAction.OpenSettings) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
    }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val buttonInkColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF10181C)
    val historyItems = state.history.result.orEmpty().take(10)
    val showEmptyHistory = state.history !is DataState.Loading && historyItems.isEmpty()

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.more),
                fontWeight = FontWeight.Bold,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            val bottomPadding = scaffoldPadding.calculateBottomPadding() + LocalNavigationBottomPadding.current + 96.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .tvListFocus()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val glassActionColors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = buttonInkColor,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    FilledTonalButton(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("more_vault_button")
                            .pressScaleBounce(pressedScale = 0.94f)
                            .adaptiveGlassPrism(shape = CircleShape, borderWidth = 1.dp)
                            .tvFocusRing(),
                        onClick = { onAction(MoreAction.OpenVault) },
                        colors = glassActionColors,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = NextIcons.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.vault),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    FilledTonalButton(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("more_pick_file_button")
                            .pressScaleBounce(pressedScale = 0.94f)
                            .adaptiveGlassPrism(shape = CircleShape, borderWidth = 1.dp)
                            .tvFocusRing(),
                        onClick = { filePicker.launch("video/*") },
                        colors = glassActionColors,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = NextIcons.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.pick_file),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (MediaOperationsService.supportsTrash()) {
                        FilledTonalButton(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("more_trash_button")
                                .pressScaleBounce(pressedScale = 0.94f)
                                .adaptiveGlassPrism(shape = CircleShape, borderWidth = 1.dp)
                                .tvFocusRing(),
                            onClick = { onAction(MoreAction.OpenTrash) },
                            colors = glassActionColors,
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = NextIcons.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.trash),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (showEmptyHistory) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyHistoryState(isDark = isDark)
                    }
                } else {
                    HistorySection(
                        history = historyItems,
                        preferences = state.preferences,
                        onMoreClick = { onAction(MoreAction.OpenHistory) },
                        onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF10181C)
    val subtitleColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4B5862)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Borderless minimal history/clock icon with translucent iOS Glass Prism finish inside the strokes itself
        Canvas(
            modifier = Modifier
                .size(62.dp)
                .testTag("more_empty_history_icon"),
        ) {
            val w = size.width
            val h = size.height
            val strokeW = w * 0.105f
            val innerSpecularW = strokeW * 0.36f
            val pad = strokeW * 0.95f
            val arcSize = Size(w - pad * 2f, h - pad * 2f)
            val arcTopLeft = Offset(pad, pad)
            val cx = w * 0.5f
            val cy = h * 0.5f

            val glassBodyBrush = if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.52f),
                        primaryColor.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.24f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1D2B36).copy(alpha = 0.42f),
                        primaryColor.copy(alpha = 0.50f),
                        Color(0xFF1D2B36).copy(alpha = 0.26f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                )
            }

            val specularEdgeBrush = if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.48f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.65f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                )
            }

            // Outer history ring arc (285 degrees clockwise from 220° leaving sleek top-left arrow gap)
            drawArc(
                brush = glassBodyBrush,
                startAngle = 218f,
                sweepAngle = 296f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
            // Specular glass prism highlight tracing inside the ring stroke
            drawArc(
                brush = specularEdgeBrush,
                startAngle = 220f,
                sweepAngle = 292f,
                useCenter = false,
                topLeft = Offset(pad + strokeW * 0.08f, pad + strokeW * 0.08f),
                size = Size(arcSize.width - strokeW * 0.16f, arcSize.height - strokeW * 0.16f),
                style = Stroke(width = innerSpecularW, cap = StrokeCap.Round),
            )

            // Counter-clockwise history arrowhead at top-left
            val arrowTip = Offset(w * 0.17f, h * 0.28f)
            val arrowPath = Path().apply {
                moveTo(w * 0.145f, h * 0.125f)
                lineTo(arrowTip.x, arrowTip.y)
                lineTo(w * 0.33f, h * 0.275f)
            }
            drawPath(
                path = arrowPath,
                brush = glassBodyBrush,
                style = Stroke(width = strokeW * 0.88f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = arrowPath,
                brush = specularEdgeBrush,
                style = Stroke(width = innerSpecularW * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Minimal clock hands with frosted glass body + inner specular edge
            val handsPath = Path().apply {
                moveTo(cx, cy - h * 0.19f)
                lineTo(cx, cy)
                lineTo(cx + w * 0.145f, cy + h * 0.095f)
            }
            drawPath(
                path = handsPath,
                brush = glassBodyBrush,
                style = Stroke(width = strokeW * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = handsPath,
                brush = specularEdgeBrush,
                style = Stroke(width = innerSpecularW, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.no_history_yet),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_history_yet_description),
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistorySection(
    history: List<Video>,
    preferences: ApplicationPreferences,
    onMoreClick: () -> Unit,
    onVideoClick: (Video) -> Unit,
) {
    if (history.isEmpty()) return
    val isTv = LocalContext.current.isTelevision
    val historyButtonFocusRequester = remember { FocusRequester() }
    val historyFocusState = rememberRestorableFocusState()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            // Keep the full header in the vertical focus path, including its empty title area.
            modifier = Modifier.fillMaxWidth().thenIf(isTv) { focusGroup() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            GlassIconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .focusRequester(historyButtonFocusRequester)
                    .focusProperties { if (isTv) down = historyFocusState.requester }
                    .tvFocusRing(),
            ) {
                Icon(imageVector = NextIcons.ArrowForward, contentDescription = stringResource(R.string.history))
            }
        }
        LazyRow(
            // The screen owns initial focus; this region restores a video only when entered.
            modifier = Modifier
                .focusProperties {
                    onExit = {
                        if (isTv && requestedFocusDirection == FocusDirection.Up) {
                            historyButtonFocusRequester.requestFocus()
                        }
                    }
                }
                .restorableFocusGroup(historyFocusState, ready = false),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(history, key = { it.uriString }) { video ->
                VideoGridItem(
                    video = video,
                    isRecentlyPlayedVideo = false,
                    textStyle = MaterialTheme.typography.bodySmall,
                    preferences = preferences,
                    modifier = Modifier
                        .width(140.dp)
                        .restorableFocusItem(historyFocusState, video.uriString),
                    onClick = { onVideoClick(video) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun MoreScreenContentPreview() {
    MoreScreenContent(state = MoreUiState(), onAction = {})
}
