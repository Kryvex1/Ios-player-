package dev.anilbeesetti.nextplayer.core.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

/**
 * Provides the nearest live [LayerBackdrop] (e.g., video TextureView layer in the player,
 * or screen content layer in MainActivity) so controls, pills, overlays, and docks can render
 * genuine iOS 26 style Liquid Glass blur + AGSL lens refraction over live content.
 */
val LocalLiquidGlassBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Tracks active dialogs/popups so the root composable can apply an in-app GPU Compose
 * RenderEffect blur that stays active even when Android Battery Saver mode disables
 * system cross-window blur (`FLAG_BLUR_BEHIND`).
 */
@Stable
class DialogBlurController {
    private var activeCount by mutableIntStateOf(0)

    val isBlurred: Boolean
        get() = activeCount > 0

    fun acquire() {
        activeCount += 1
    }

    fun release() {
        activeCount = (activeCount - 1).coerceAtLeast(0)
    }
}

val LocalDialogBlurController = compositionLocalOf<DialogBlurController?> { null }

/**
 * Wraps the root activity content so any [NextDialog] or popup triggers a smooth,
 * hardware-accelerated in-app GPU blur (`RenderEffect.createBlurEffect`) that is
 * unaffected by Android Battery Saver mode.
 */
@Composable
fun DialogBlurHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val controller = remember { DialogBlurController() }
    val blurRadius by animateDpAsState(
        targetValue = if (controller.isBlurred) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "dialog_gpu_blur_radius",
    )

    CompositionLocalProvider(LocalDialogBlurController provides controller) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.1.dp) {
                        val radiusPx = blurRadius.toPx()
                        renderEffect = RenderEffect.createBlurEffect(
                            radiusPx,
                            radiusPx,
                            Shader.TileMode.CLAMP,
                        ).asComposeRenderEffect()
                    }
                },
        ) {
            content()
        }
    }
}

/**
 * Applies an iOS 26 style Liquid Glass surface using Kyant0 Backdrop (`io.github.kyant0:backdrop:2.0.1`).
 *
 * - Android 13+ (API 33+): Live in-app GPU backdrop vibrancy + blur + AGSL lens refraction
 *   (`refractionHeight` + `refractionAmount`) + top-left 45° edge-light highlight + readable scrim.
 *   Because it uses in-app GPU rendering, it remains active even in Android Battery Saver mode.
 * - Android 12 (API 31–32): Live in-app GPU backdrop blur + 45° specular edge-light + frosted scrim.
 * - Android 11 and below (API <= 30): High-craft frosted translucent glass fallback + adaptive
 *   45° top-left specular border.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    backdrop: LayerBackdrop? = LocalLiquidGlassBackdrop.current,
    blurRadius: Dp = 2.5.dp,
    refractionHeight: Dp = 8.dp,
    refractionAmount: Dp = 14.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.34f),
    fallbackColor: Color = Color(0xFF14161D).copy(alpha = 0.72f),
    highlightAlpha: Float = 0.48f,
    highlightWidth: Dp = 0.9.dp,
    chromaticAberration: Boolean = false,
    adaptiveLightBorder: Boolean = false,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val useLightBorder = adaptiveLightBorder && !isDark

    val edgeLightBrush = remember(highlightAlpha, useLightBorder) {
        if (useLightBorder) {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.09f),
                    Color.Black.copy(alpha = 0.12f),
                    Color.Black.copy(alpha = 0.10f),
                    Color.Black.copy(alpha = 0.08f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = highlightAlpha),
                    Color.White.copy(alpha = highlightAlpha * 0.22f),
                    Color.White.copy(alpha = highlightAlpha * 0.08f),
                    Color.White.copy(alpha = highlightAlpha * 0.28f),
                ),
            )
        }
    }

    if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val baseModifier = this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    vibrancy()
                }
                if (blurRadius > 0.dp) {
                    blur(blurRadius.toPx())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    refractionHeight > 0.dp &&
                    refractionAmount > 0.dp
                ) {
                    lens(
                        refractionHeight.toPx(),
                        refractionAmount.toPx(),
                        false,
                        chromaticAberration,
                    )
                }
            },
            highlight = {
                Highlight(
                    width = highlightWidth,
                    blurRadius = 1.dp,
                    alpha = highlightAlpha,
                    style = HighlightStyle.Default(angle = 45f),
                )
            },
            shadow = { null },
            onDrawSurface = {
                if (scrimColor.alpha > 0f) {
                    drawRect(scrimColor)
                }
            },
        )
        // In Light Mode, add a crisp subtle outer contrast border so glass stays defined against bright backgrounds
        return if (useLightBorder) {
            baseModifier.border(
                border = BorderStroke(highlightWidth, edgeLightBrush),
                shape = shape,
            )
        } else {
            baseModifier
        }
    }

    // Android 11 and below (or when no live LayerBackdrop is mounted):
    // High-craft frosted translucent fallback with subtle sheen + 45° specular edge highlight.
    return this
        .clip(shape)
        .background(color = fallbackColor, shape = shape)
        .border(
            border = BorderStroke(highlightWidth, edgeLightBrush),
            shape = shape,
        )
}

/**
 * Adds a tactile iOS-style press-scale spring bounce without consuming click/tap events.
 */
fun Modifier.pressScaleBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
): Modifier = composed {
    if (!enabled) return@composed this
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press_scale_bounce",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isPressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    isPressed = false
                }
            }
        }
}

/**
 * Dark-surface Apple Glass Prism surface used inside the video player overlays and dark controls.
 * Uses a translucent frosted glass fill with a crisp 1dp top-left 45° specular prism border.
 */
@Composable
fun Modifier.glassPrism(
    shape: Shape,
    containerColor: Color = Color.White.copy(alpha = 0.12f),
    borderAlpha: Float = 0.34f,
    borderWidth: Dp = 1.dp,
    selected: Boolean = false,
    accentColor: Color = Color.White,
): Modifier {
    val prismBorderBrush = remember(borderAlpha, selected, accentColor) {
        if (selected) {
            Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.45f),
                    accentColor.copy(alpha = 0.25f),
                    accentColor.copy(alpha = 0.65f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.28f),
                    Color.White.copy(alpha = borderAlpha * 0.12f),
                    Color.White.copy(alpha = borderAlpha * 0.38f),
                ),
            )
        }
    }
    return this
        .clip(shape)
        .background(color = containerColor, shape = shape)
        .border(
            border = BorderStroke(borderWidth, prismBorderBrush),
            shape = shape,
        )
}

/**
 * Adaptive Light & Dark Mode iOS Liquid Glass / Glass Prism surface for outer app screens
 * (Home, Playlists, Network, More, Top App Bars, Cards, Buttons).
 *
 * - Dark Mode: translucent dark frosted fill + subtle bright white 1dp 45° specular edge border.
 * - Light Mode: crisp translucent frosted white fill (`Color.White` with balanced alpha) +
 *   continuous subtle dark-tinted outer edge (`Black(0.09f..0.12f)`) so every button and card
 *   stands out cleanly against light backgrounds without washing out.
 */
@Composable
fun Modifier.adaptiveGlassPrism(
    shape: Shape,
    selected: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    borderWidth: Dp = 1.dp,
    containerTint: Color = Color.Unspecified,
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f

    val fillBrush = remember(isDark, selected, accentColor, containerTint, colorScheme.surfaceContainerHigh) {
        when {
            containerTint != Color.Unspecified -> Brush.linearGradient(
                colors = listOf(containerTint, containerTint.copy(alpha = (containerTint.alpha * 0.88f).coerceIn(0f, 1f))),
            )
            selected && isDark -> Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.32f),
                    accentColor.copy(alpha = 0.18f),
                ),
            )
            selected && !isDark -> Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.18f),
                    accentColor.copy(alpha = 0.10f),
                ),
            )
            isDark -> Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    colorScheme.surfaceContainerHigh.copy(alpha = 0.56f),
                ),
            )
            else -> Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.86f),
                    Color.White.copy(alpha = 0.68f),
                ),
            )
        }
    }

    val borderBrush = remember(isDark, selected, accentColor) {
        when {
            selected -> Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isDark) 0.78f else 0.54f),
                    if (isDark) Color.White.copy(alpha = 0.36f) else accentColor.copy(alpha = 0.32f),
                    accentColor.copy(alpha = if (isDark) 0.26f else 0.42f),
                ),
            )
            isDark -> Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.24f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.15f),
                ),
            )
            else -> Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.09f),
                    Color.Black.copy(alpha = 0.12f),
                    Color.Black.copy(alpha = 0.11f),
                    Color.Black.copy(alpha = 0.08f),
                ),
            )
        }
    }

    return this
        .clip(shape)
        .background(brush = fillBrush, shape = shape)
        .border(
            border = BorderStroke(borderWidth, borderBrush),
            shape = shape,
        )
}

/**
 * Applies a sleek iOS-style translucent 1dp gradient glass edge highlight and subtle diagonal
 * specular glass sheen around folder icons and media thumbnail cards in both Light and Dark mode.
 */
@Composable
fun Modifier.glassEdgeHighlight(
    shape: Shape,
    borderWidth: Dp = 1.dp,
    showSheen: Boolean = true,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val borderBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.20f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.08f),
                    Color.Black.copy(alpha = 0.12f),
                    Color.Black.copy(alpha = 0.11f),
                    Color.Black.copy(alpha = 0.08f),
                ),
            )
        }
    }
    val sheenAlpha = if (isDark) 0.14f else 0.18f

    return this
        .clip(shape)
        .then(
            if (showSheen) {
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = sheenAlpha),
                                Color.White.copy(alpha = sheenAlpha * 0.25f),
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.65f, size.height * 0.65f),
                        ),
                    )
                }
            } else {
                Modifier
            },
        )
        .border(
            border = BorderStroke(borderWidth, borderBrush),
            shape = shape,
        )
}

/**
 * Adaptive Light/Dark iOS Glass Prism icon button for Top App Bars (Search, Layout/Grid, Settings, Back).
 * Keeps a full 48dp accessible touch target while rendering a crisp 40dp frosted circular glass pill
 * with high-contrast dark icons in Light Mode.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val resolvedContentColor = when {
        contentColor != Color.Unspecified -> contentColor
        isDark -> colorScheme.onSurface
        else -> Color(0xFF10181C)
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .padding(horizontal = 3.dp)
            .size(40.dp)
            .pressScaleBounce(enabled = enabled, pressedScale = 0.88f)
            .adaptiveGlassPrism(
                shape = CircleShape,
                borderWidth = 1.dp,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) resolvedContentColor else resolvedContentColor.copy(alpha = 0.38f),
        ) {
            content()
        }
    }
}

/**
 * Adaptive Light/Dark iOS Glass Prism primary or secondary action button (e.g. Open Network Stream,
 * Test & Save, Dialog Confirm/Cancel). Uses crisp frosted fill + dark-tinted outer edge in Light Mode
 * and glowing translucent specular edge in Dark Mode.
 */
@Composable
fun GlassActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 11.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }

    val tint = when {
        !enabled && isDark -> Color.White.copy(alpha = 0.06f)
        !enabled && !isDark -> Color.White.copy(alpha = 0.50f)
        emphasized && isDark -> colorScheme.primary.copy(alpha = 0.30f)
        emphasized && !isDark -> colorScheme.primary.copy(alpha = 0.18f)
        else -> Color.Unspecified
    }
    val contentColor = when {
        !enabled -> (if (isDark) colorScheme.onSurface else Color(0xFF10181C)).copy(alpha = 0.42f)
        emphasized && isDark -> colorScheme.primaryContainer
        emphasized && !isDark -> colorScheme.primary
        isDark -> colorScheme.onSurface
        else -> Color(0xFF10181C)
    }

    Box(
        modifier = modifier
            .pressScaleBounce(enabled = enabled, pressedScale = 0.95f)
            .adaptiveGlassPrism(
                shape = shape,
                selected = emphasized && enabled,
                accentColor = colorScheme.primary,
                borderWidth = 1.dp,
                containerTint = tint,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Adaptive Light/Dark frosted iOS Glass Prism Floating Action Button (Play / Add / Settings FAB).
 * Renders live GPU backdrop blur + AGSL lens refraction when mounted over a [LayerBackdrop],
 * plus an adaptive frosted glass tint and crisp 1dp border in both Light and Dark mode.
 */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    size: Dp = 56.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }

    val scrimTint = if (isDark) {
        colorScheme.primaryContainer.copy(alpha = 0.62f)
    } else {
        colorScheme.primaryContainer.copy(alpha = 0.84f)
    }
    val fallbackTint = if (isDark) {
        colorScheme.primaryContainer.copy(alpha = 0.84f)
    } else {
        colorScheme.primaryContainer.copy(alpha = 0.92f)
    }

    Box(
        modifier = modifier
            .size(size)
            .pressScaleBounce(pressedScale = 0.90f)
            .liquidGlass(
                shape = shape,
                blurRadius = 6.dp,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp,
                scrimColor = scrimTint,
                fallbackColor = fallbackTint,
                highlightAlpha = if (isDark) 0.52f else 0.72f,
                highlightWidth = 1.dp,
                adaptiveLightBorder = true,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onPrimaryContainer) {
            content()
        }
    }
}
