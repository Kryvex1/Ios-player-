package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

/**
 * Apple-inspired Glass Prism toggle switch with translucent frosted track,
 * 45° specular prism border, and spring-animated glossy thumb.
 */
@Composable
fun NextSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedIcon: ImageVector = NextIcons.Check,
) {
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp

    val colorScheme = MaterialTheme.colorScheme
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            colorScheme.primary.copy(alpha = 0.88f)
        } else {
            colorScheme.onSurface.copy(alpha = 0.14f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "glass_switch_track_color",
    )

    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding * 2 else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "glass_switch_thumb_offset",
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "glass_switch_icon_alpha",
    )

    val trackBorderBrush = remember(checked, colorScheme.primary) {
        if (checked) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    colorScheme.primary.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.25f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.22f),
                ),
            )
        }
    }

    val thumbGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                Color(0xFFF2F5FA),
            ),
        )
    }

    val interactionModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .pressScaleBounce(enabled = enabled && onCheckedChange != null, pressedScale = 0.94f)
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(color = trackColor, shape = CircleShape)
            .border(
                border = BorderStroke(1.dp, trackBorderBrush),
                shape = CircleShape,
            )
            .then(interactionModifier)
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Subtle top glass sheen inside track
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .size(thumbSize)
                .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(brush = thumbGradient, shape = CircleShape)
                .border(
                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.85f)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (iconAlpha > 0.05f) {
                Icon(
                    imageVector = checkedIcon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(iconAlpha),
                )
            }
        }
    }
}
