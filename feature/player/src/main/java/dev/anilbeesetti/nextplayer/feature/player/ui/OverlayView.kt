package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.ui.components.liquidGlass
import dev.anilbeesetti.nextplayer.core.ui.components.requestFocusUntilLanded
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

val LocalPlayerShowStatusBar = compositionLocalOf { false }

/**
 * Direct floating Apple-style Glass Prism overlay rendered directly on top of the live playing
 * video. Dismissed cleanly by tapping anywhere outside the panel (the scrim consumes the touch
 * event so controls underneath never pop up).
 */
@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    @Suppress("UNUSED_PARAMETER") onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(show) {
        if (show && isTv) {
            focusRequester.requestFocusUntilLanded(attempts = 5)
        }
    }

    val slideSpring = spring<IntOffset>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val scaleSpring = spring<Float>(
        dampingRatio = 0.76f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val origin = if (configuration.isPortrait) {
        TransformOrigin(0.5f, 1f)
    } else {
        TransformOrigin(1f, 0.5f)
    }

    val floatingCardShape = RoundedCornerShape(28.dp)
    val panelScrim = Color(0xFF0E1118).copy(alpha = 0.56f)
    val panelFallback = Color(0xFF121620).copy(alpha = 0.86f)

    // Register the floating panel as a pointer-hit barrier on Final pass WITHOUT consuming
    // Main-pass events so child LazyColumns and verticalScroll lists scroll smoothly while
    // taps inside the panel never leak through to the backdrop scrim or video gestures.
    val consumeInsideTouchModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Final)
            }
        }
    }

    AnimatedVisibility(
        modifier = Modifier
            .align(
                if (configuration.isPortrait) {
                    Alignment.BottomCenter
                } else {
                    Alignment.CenterEnd
                },
            )
            .imePadding(),
        visible = show,
        enter = (if (configuration.isPortrait) {
            slideInVertically(animationSpec = slideSpring) { (it * 0.65f).toInt() }
        } else {
            slideInHorizontally(animationSpec = slideSpring) { (it * 0.65f).toInt() }
        }) + scaleIn(
            initialScale = 0.92f,
            transformOrigin = origin,
            animationSpec = scaleSpring,
        ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
        exit = (if (configuration.isPortrait) {
            slideOutVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) { (it * 0.65f).toInt() }
        } else {
            slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { (it * 0.65f).toInt() }
        }) + scaleOut(
            targetScale = 0.93f,
            transformOrigin = origin,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
        ) + fadeOut(animationSpec = tween(160)),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            if (configuration.isPortrait) {
                // Portrait: Floating Glass Prism Overlay Card over playing video
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .heightIn(max = (configuration.screenHeightDp * 0.68f).dp)
                        .wrapContentHeight()
                        .liquidGlass(
                            shape = floatingCardShape,
                            blurRadius = 18.dp,
                            refractionHeight = 14.dp,
                            refractionAmount = 22.dp,
                            scrimColor = panelScrim,
                            fallbackColor = panelFallback,
                            highlightAlpha = 0.44f,
                            highlightWidth = 1.dp,
                        )
                        .then(consumeInsideTouchModifier),
                ) {
                    Column(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusGroup(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier.size(width = 36.dp, height = 4.dp),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.36f),
                            ) {}
                        }

                        OverlayHeader(title = title)
                        content()
                    }
                }
            } else {
                // Landscape: Direct Floating Glass Prism Overlay Card on top of full-screen playing video.
                // Uses locked constant top/bottom coordinates based on the Show status bar preference so
                // opening or closing never triggers an inset jump or vertical bounce.
                val showStatusBar = LocalPlayerShowStatusBar.current
                Box(
                    modifier = modifier
                        .padding(
                            start = 12.dp,
                            top = if (showStatusBar) 28.dp else 12.dp,
                            end = 14.dp,
                            bottom = 12.dp,
                        )
                        .widthIn(min = 310.dp, max = 390.dp)
                        .fillMaxHeight()
                        .liquidGlass(
                            shape = floatingCardShape,
                            blurRadius = 18.dp,
                            refractionHeight = 14.dp,
                            refractionAmount = 22.dp,
                            scrimColor = panelScrim,
                            fallbackColor = panelFallback,
                            highlightAlpha = 0.44f,
                            highlightWidth = 1.dp,
                        )
                        .then(consumeInsideTouchModifier),
                ) {
                    Column(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusGroup()
                            .padding(top = 8.dp),
                    ) {
                        OverlayHeader(title = title)
                        Spacer(modifier = Modifier.size(4.dp))
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Preview
@Composable
private fun PreviewOverlayView() {
    NextPlayerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OverlayView(modifier = Modifier.align(Alignment.BottomCenter), title = "Selector view", show = true) {
                Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum")
            }
        }
    }
}
