package dev.anilbeesetti.nextplayer.feature.player.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.ui.components.liquidGlass
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.feature.player.LocalUseMaterialYouControls
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = Color.Black.copy(alpha = 0.30f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }

    LaunchedEffect(interactionSource) {
        var isLongPressClicked = false
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongPressClicked = false
                    delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                    onLongClick?.let {
                        isLongPressClicked = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        it.invoke()
                    }
                }

                is PressInteraction.Release -> {
                    if (!isLongPressClicked) {
                        onClick()
                    }
                }
            }
        }
    }

    val rippleConfiguration = RippleConfiguration(
        color = Color.White,
        rippleAlpha = RippleAlpha(
            pressedAlpha = 0.45f,
            focusedAlpha = 0.45f,
            draggedAlpha = 0.45f,
            hoveredAlpha = 0.45f,
        ),
    )

    val isMaterialYou = LocalUseMaterialYouControls.current
    // Consistent Apple Glass Prism on all player buttons unless explicitly transparent
    val showGlassBackground = containerColor != Color.Transparent

    val scrimTint = when {
        containerColor != Color.Unspecified && containerColor != Color.Transparent -> containerColor
        isMaterialYou -> Color.Black.copy(alpha = 0.36f)
        else -> Color.Black.copy(alpha = 0.28f)
    }
    val effectiveScrim = if (enabled) scrimTint else scrimTint.copy(alpha = 0.14f)
    val effectiveFallback = if (enabled) {
        Color(0xFF141822).copy(alpha = 0.56f)
    } else {
        Color(0xFF141822).copy(alpha = 0.28f)
    }

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalRippleConfiguration provides rippleConfiguration,
    ) {
        Box(
            modifier = modifier
                .pressScaleBounce(enabled = enabled, pressedScale = 0.90f)
                .then(
                    if (showGlassBackground) {
                        Modifier.liquidGlass(
                            shape = CircleShape,
                            blurRadius = 2.5.dp,
                            refractionHeight = 8.dp,
                            refractionAmount = 14.dp,
                            scrimColor = effectiveScrim,
                            fallbackColor = effectiveFallback,
                            highlightAlpha = 0.50f,
                            highlightWidth = 1.dp,
                        )
                    } else {
                        Modifier.clip(CircleShape)
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    enabled = enabled,
                    onClick = {},
                )
                .tvFocusRing(isTv),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}
