package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.glassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.liquidGlass
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

/**
 * Compact translucent Liquid Glass vertical progress indicator for volume/brightness.
 * Sized to sit cleanly between the top bar and bottom seekbar with zero overlap.
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 26.dp,
    icon: Painter,
    @IntRange(from = 0, to = 200) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    boostColor: Color = Color(0xFFFC6E6E),
) {
    val normalizedValue = value.coerceIn(0, maxValue)
    val fillFraction = normalizedValue.toFloat() / maxValue.toFloat()
    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && value > NORMAL_MAX_PERCENTAGE
    val activeFillColor = if (isBoostActive) boostColor else Color.White

    Column(
        modifier = modifier
            .heightIn(min = 124.dp, max = 168.dp)
            .liquidGlass(
                shape = CircleShape,
                blurRadius = 6.dp,
                refractionHeight = 8.dp,
                refractionAmount = 14.dp,
                scrimColor = Color(0xFF10141D).copy(alpha = 0.56f),
                fallbackColor = Color(0xFF121620).copy(alpha = 0.80f),
                highlightAlpha = 0.42f,
                highlightWidth = 0.9.dp,
            )
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier.size(width),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = normalizedValue.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .width(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .clip(CircleShape)
                    .background(activeFillColor),
            )
        }
        Box(
            modifier = Modifier.size(width),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Compact horizontal glass HUD pill shown at the top-center when a portrait overlay sheet
 * is open, preventing any overlap with bottom sheets or sidebars.
 */
@Composable
fun HorizontalProgressHudPill(
    modifier: Modifier = Modifier,
    icon: Painter,
    @IntRange(from = 0, to = 200) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
    boostColor: Color = Color(0xFFFC6E6E),
) {
    val normalizedValue = value.coerceIn(0, maxValue)
    val fillFraction = normalizedValue.toFloat() / maxValue.toFloat()
    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && value > NORMAL_MAX_PERCENTAGE
    val activeFillColor = if (isBoostActive) boostColor else Color.White

    Row(
        modifier = modifier
            .glassPrism(
                shape = CircleShape,
                containerColor = Color(0xFF121620).copy(alpha = 0.78f),
                borderAlpha = 0.36f,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Box(
            modifier = Modifier
                .width(92.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(activeFillColor),
            )
        }
        Text(
            text = "$normalizedValue%",
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.widthIn(min = 34.dp),
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    NextPlayerTheme {
        VerticalProgressView(
            value = 50,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}

@Preview
@Composable
private fun VerticalProgressBoostPreview() {
    NextPlayerTheme {
        VerticalProgressView(
            value = 150,
            maxValue = 200,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
