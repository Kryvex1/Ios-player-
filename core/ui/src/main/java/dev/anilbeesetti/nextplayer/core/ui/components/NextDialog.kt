package dev.anilbeesetti.nextplayer.core.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
fun NextDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    dialogProperties: DialogProperties = NextDialogDefaults.dialogProperties,
) {
    val configuration = LocalConfiguration.current
    val maxDialogWidth = minOf(352.dp, configuration.screenWidthDp.dp - NextDialogDefaults.dialogMargin * 2)
    val maxDialogHeight = (configuration.screenHeightDp * 0.78f).dp
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val dialogShape = RoundedCornerShape(24.dp)

    // Activate in-app GPU Compose RenderEffect blur on the underlying activity layer.
    // Unlike Android's system Window.FLAG_BLUR_BEHIND, in-app RenderEffect blur never
    // disappears when Battery Saver mode is enabled.
    val dialogBlurController = LocalDialogBlurController.current
    val hasInAppGpuBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialogBlurController != null
    DisposableEffect(dialogBlurController) {
        dialogBlurController?.acquire()
        onDispose {
            dialogBlurController?.release()
        }
    }

    // Adaptive frosted Liquid Glass surface + crisp border for Light & Dark Mode
    val frostedBrush = remember(isDark, hasInAppGpuBlur, colorScheme.surfaceContainerHigh) {
        if (isDark) {
            val topAlpha = if (hasInAppGpuBlur) 0.84f else 0.95f
            val bottomAlpha = if (hasInAppGpuBlur) 0.76f else 0.91f
            Brush.linearGradient(
                colors = listOf(
                    colorScheme.surfaceContainerHigh.copy(alpha = topAlpha),
                    Color(0xFF10141D).copy(alpha = bottomAlpha),
                ),
            )
        } else {
            val topAlpha = if (hasInAppGpuBlur) 0.92f else 0.97f
            val bottomAlpha = if (hasInAppGpuBlur) 0.86f else 0.94f
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = topAlpha),
                    Color(0xFFF3F6F9).copy(alpha = bottomAlpha),
                ),
            )
        }
    }

    val edgeLightBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.18f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.09f),
                    Color.Black.copy(alpha = 0.12f),
                    Color.Black.copy(alpha = 0.10f),
                    Color.Black.copy(alpha = 0.08f),
                ),
            )
        }
    }

    val titleColor = if (isDark) colorScheme.onSurface else Color(0xFF10181C)
    val bodyColor = if (isDark) colorScheme.onSurface else Color(0xFF182226)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
    ) {
        val view = LocalView.current
        DisposableEffect(view, hasInAppGpuBlur) {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            (parent as? DialogWindowProvider)?.window?.let { window ->
                try {
                    window.setLayout(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                    )
                    window.setDimAmount(if (hasInAppGpuBlur) 0.28f else 0.44f)
                    window.setWindowAnimations(android.R.style.Animation_Dialog)
                } catch (_: Exception) {
                }
            }
            onDispose { }
        }

        Column(
            modifier = modifier
                .widthIn(min = 280.dp, max = maxDialogWidth)
                .heightIn(max = maxDialogHeight)
                .wrapContentHeight()
                .clip(dialogShape)
                .background(brush = frostedBrush, shape = dialogShape)
                .border(
                    border = BorderStroke(1.dp, edgeLightBrush),
                    shape = dialogShape,
                ),
        ) {
            // Compact iOS Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides titleColor) {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    ) {
                        title()
                    }
                }
            }

            // Compact Content Slot — weight(1f, fill = false) wraps tightly on short content
            // and constrains scrolling cleanly on tall content without clipping top items.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 18.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides bodyColor) {
                    ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                        content()
                    }
                }
            }

            // Compact Footer Row — zero wasted bottom space around Cancel/Okay/Done buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

@Composable
fun NextDialogWithDoneAndCancelButtons(
    title: String,
    onDoneClick: () -> Unit,
    onDismissClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    NextDialog(
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        confirmButton = { DoneButton(onClick = onDoneClick) },
        dismissButton = { CancelButton(onClick = onDismissClick) },
        onDismissRequest = onDismissClick,
        content = content,
    )
}

object NextDialogDefaults {
    val dialogProperties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = true,
    )
    val dialogMargin: Dp = 24.dp
}
