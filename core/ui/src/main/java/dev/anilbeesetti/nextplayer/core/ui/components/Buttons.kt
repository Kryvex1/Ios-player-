package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun GlassDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    GlassActionButton(
        onClick = onClick,
        enabled = enabled,
        emphasized = emphasized,
        shape = RoundedCornerShape(12.dp),
        horizontalPadding = 16.dp,
        verticalPadding = 8.dp,
        modifier = modifier
            .heightIn(min = 36.dp)
            .tvFocusRing(shape = RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun DoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassDialogButton(
        text = stringResource(R.string.done),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        emphasized = true,
    )
}

@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassDialogButton(
        text = stringResource(R.string.cancel),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        emphasized = false,
    )
}
