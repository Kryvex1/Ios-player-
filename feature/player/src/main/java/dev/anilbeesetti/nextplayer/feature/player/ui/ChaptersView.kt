package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.extractor.metadata.Chapter
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BoxScope.ChaptersView(
    show: Boolean,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterSelected: (Chapter) -> Unit,
    onDismiss: () -> Unit = {},
) {
    val accentColor = MaterialTheme.colorScheme.primary

    OverlayView(
        show = show,
        title = stringResource(R.string.chapters),
        onDismiss = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberLazyListState(initialFirstVisibleItemIndex = currentChapterIndex.coerceAtLeast(0)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(chapters, key = { index, chapter -> "${index}_${chapter.startTimeMs}" }) { index, chapter ->
                val isSelected = index == currentChapterIndex
                val cardShape = RoundedCornerShape(16.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScaleBounce(pressedScale = 0.98f)
                        .glassPrism(
                            shape = cardShape,
                            containerColor = if (isSelected) {
                                accentColor.copy(alpha = 0.25f)
                            } else {
                                Color.White.copy(alpha = 0.08f)
                            },
                            borderAlpha = if (isSelected) 0.58f else 0.26f,
                            selected = isSelected,
                            accentColor = accentColor,
                        )
                        .clickable {
                            onChapterSelected(chapter)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .glassPrism(
                                shape = CircleShape,
                                containerColor = if (isSelected) {
                                    accentColor.copy(alpha = 0.85f)
                                } else {
                                    Color.White.copy(alpha = 0.14f)
                                },
                                borderAlpha = if (isSelected) 0.70f else 0.30f,
                                selected = isSelected,
                                accentColor = accentColor,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                        )
                    }

                    Text(
                        text = chapter.titleOrDefault(index),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Box(
                        modifier = Modifier
                            .glassPrism(
                                shape = RoundedCornerShape(9.dp),
                                containerColor = if (isSelected) {
                                    accentColor.copy(alpha = 0.28f)
                                } else {
                                    Color.White.copy(alpha = 0.12f)
                                },
                                borderAlpha = if (isSelected) 0.50f else 0.28f,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = chapter.startTimeMs.milliseconds.formatted(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun Chapter.titleOrDefault(index: Int): String =
    title?.value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chapter_number, index + 1)
