package dev.anilbeesetti.nextplayer.feature.player

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton

@Composable
fun PlayerLockedControls(
    onUnlockControls: () -> Unit,
    modifier: Modifier = Modifier,
    showStatusBar: Boolean = false,
    unlockButtonModifier: Modifier = Modifier,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val targetTopPadding = when {
        showStatusBar && isLandscape -> if (statusBarTop > 0.dp) {
            statusBarTop.coerceIn(22.dp, 26.dp)
        } else {
            24.dp
        }
        showStatusBar -> if (statusBarTop > 0.dp) {
            statusBarTop + 2.dp
        } else {
            26.dp
        }
        isLandscape -> 8.dp
        else -> 12.dp
    }
    val topPadding by animateDpAsState(
        targetValue = targetTopPadding,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "locked_controls_top_padding",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topPadding, start = 14.dp, end = 14.dp, bottom = 14.dp),
    ) {
        PlayerButton(
            modifier = unlockButtonModifier,
            containerColor = Color.Black.copy(0.45f),
            onClick = onUnlockControls,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.controls_unlock),
                tint = Color.White,
            )
        }
    }
}
