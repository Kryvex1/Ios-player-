package dev.anilbeesetti.nextplayer.feature.player.state

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Process
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.media3.common.Player
import androidx.media3.common.listen
import dev.anilbeesetti.nextplayer.core.common.extensions.isPipFeatureSupported
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun rememberPictureInPictureState(
    player: Player,
    autoEnter: Boolean = true,
): PictureInPictureState {
    val activity = LocalActivity.current
    val pictureInPictureState = remember(player, activity) {
        PictureInPictureState(
            player = player,
            activity = activity as ComponentActivity,
            autoEnter = autoEnter,
        )
    }
    LaunchedEffect(autoEnter) {
        pictureInPictureState.updateAutoEnter(autoEnter)
    }
    DisposableEffect(activity, pictureInPictureState) {
        pictureInPictureState.handleListeners(this)
    }
    LaunchedEffect(player, pictureInPictureState) {
        pictureInPictureState.observe()
    }
    return pictureInPictureState
}

/**
 * Reliable Picture-in-Picture controller built directly on Android's native
 * [PictureInPictureParams.Builder] and [ComponentActivity.addOnPictureInPictureModeChangedListener].
 * - Safely clamps aspect ratios to Android's valid PiP range (0.45..2.35) so extreme/ultrawide
 *   or vertical videos never throw IllegalArgumentException.
 * - Enables `setAutoEnterEnabled` + `setSeamlessResizeEnabled` on Android 12+ (API 31+).
 * - Syncs actions, video size, and playback state live.
 */
@Stable
class PictureInPictureState(
    private val player: Player,
    private val activity: ComponentActivity,
    autoEnter: Boolean = true,
) {
    companion object {
        private const val PIP_INTENT_ACTION = "dev.anilbeesetti.nextplayer.PIP_ACTION"
        private const val PIP_INTENT_ACTION_CODE = "pip_action_code"
        private const val PIP_ACTION_PLAY = 1
        private const val PIP_ACTION_PAUSE = 2
        private const val PIP_ACTION_NEXT = 3
        private const val PIP_ACTION_PREVIOUS = 4
        private const val MIN_ASPECT_RATIO = 0.45f
        private const val MAX_ASPECT_RATIO = 2.35f
    }

    private var autoEnterEnabled: Boolean = autoEnter

    val isPipSupported: Boolean = activity.isPipFeatureSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val hasPipPermission: Boolean
        get() = if (isPipSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOps = getSystemService(activity, AppOpsManager::class.java) ?: return true
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), activity.packageName)
            } else {
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), activity.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }

    var isInPictureInPictureMode: Boolean by mutableStateOf(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode,
    )
        private set

    private var aspectRatio: Rational = Rational(16, 9)
    private var sourceRectHint: Rect? = null
    private var actions: List<RemoteAction> = emptyList()
    private var receiverRegistered: Boolean = false

    fun updateAutoEnter(enabled: Boolean) {
        if (autoEnterEnabled != enabled) {
            autoEnterEnabled = enabled
            applyPictureInPictureParams()
        }
    }

    fun setVideoViewRect(rect: Rect) {
        if (!isPipSupported) return
        if (rect.width() <= 0 || rect.height() <= 0) return

        val prev = sourceRectHint
        if (prev != null &&
            abs(prev.left - rect.left) < 8 &&
            abs(prev.top - rect.top) < 8 &&
            abs(prev.right - rect.right) < 8 &&
            abs(prev.bottom - rect.bottom) < 8
        ) {
            return
        }

        val newSourceRectHint = Rect(rect)
        val newAspectRatio = resolveSafeAspectRatio(rect.width(), rect.height())

        var changed = false
        if (newAspectRatio != aspectRatio) {
            aspectRatio = newAspectRatio
            changed = true
        }
        if (newSourceRectHint != sourceRectHint) {
            sourceRectHint = newSourceRectHint
            changed = true
        }

        if (changed) applyPictureInPictureParams()
    }

    fun enterPictureInPictureMode(): Boolean {
        if (!isPipSupported || !hasPipPermission) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (activity.isInPictureInPictureMode || activity.isFinishing || activity.isDestroyed) return false

        return runCatching {
            updateAspectRatioFromVideoSize()
            updatePictureInPictureActions()
            val params = buildPictureInPictureParams()
            activity.setPictureInPictureParams(params)
            val entered = activity.enterPictureInPictureMode(params)
            if (entered) {
                isInPictureInPictureMode = true
            }
            entered
        }.getOrDefault(false)
    }

    fun openPictureInPictureSettings() {
        runCatching {
            val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS").apply {
                data = "package:${activity.packageName}".toUri()
            }
            activity.startActivity(intent)
        }
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult = with(disposableEffectScope) {
        val pipBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || intent.action != PIP_INTENT_ACTION) return
                when (intent.getIntExtra(PIP_INTENT_ACTION_CODE, 0)) {
                    PIP_ACTION_PLAY -> player.play()
                    PIP_ACTION_PAUSE -> player.pause()
                    PIP_ACTION_NEXT -> player.seekToNext()
                    PIP_ACTION_PREVIOUS -> player.seekToPrevious()
                }
            }
        }

        val pipModeListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            if (info.isInPictureInPictureMode) {
                onEnterPictureInPictureMode(pipBroadcastReceiver)
            } else {
                onExitPictureInPictureMode(pipBroadcastReceiver)
            }
        }

        activity.addOnPictureInPictureModeChangedListener(pipModeListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode) {
            onEnterPictureInPictureMode(pipBroadcastReceiver)
        }

        return onDispose {
            activity.removeOnPictureInPictureModeChangedListener(pipModeListener)
            if (receiverRegistered) {
                runCatching { activity.unregisterReceiver(pipBroadcastReceiver) }
                receiverRegistered = false
            }
        }
    }

    suspend fun observe() {
        updateAspectRatioFromVideoSize()
        updatePictureInPictureActions()
        applyPictureInPictureParams()

        player.listen { events ->
            val relevant = events.containsAny(
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_VIDEO_SIZE_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
            )
            if (relevant) {
                updateAspectRatioFromVideoSize()
                updatePictureInPictureActions()
                applyPictureInPictureParams()
            }
        }
    }

    private fun onEnterPictureInPictureMode(receiver: BroadcastReceiver) {
        isInPictureInPictureMode = true
        if (!receiverRegistered) {
            runCatching {
                ContextCompat.registerReceiver(
                    activity,
                    receiver,
                    IntentFilter(PIP_INTENT_ACTION),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                receiverRegistered = true
            }
        }
    }

    private fun onExitPictureInPictureMode(receiver: BroadcastReceiver) {
        isInPictureInPictureMode = false
        if (receiverRegistered) {
            runCatching { activity.unregisterReceiver(receiver) }
            receiverRegistered = false
        }
    }

    private fun updateAspectRatioFromVideoSize() {
        val size = player.videoSize
        if (size.width > 0 && size.height > 0) {
            aspectRatio = resolveSafeAspectRatio(size.width, size.height)
        }
    }

    private fun resolveSafeAspectRatio(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return Rational(16, 9)
        val raw = width.toFloat() / height.toFloat()
        val clamped = raw.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
        return Rational((clamped * 1000f).roundToInt().coerceAtLeast(1), 1000)
    }

    private fun updatePictureInPictureActions() {
        if (!isPipSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        actions = listOf(
            createPipAction(
                context = activity,
                title = "Previous",
                icon = coreUiR.drawable.ic_skip_prev,
                actionCode = PIP_ACTION_PREVIOUS,
            ),
            if (player.isPlaying || player.playWhenReady) {
                createPipAction(
                    context = activity,
                    title = "Pause",
                    icon = coreUiR.drawable.ic_pause,
                    actionCode = PIP_ACTION_PAUSE,
                )
            } else {
                createPipAction(
                    context = activity,
                    title = "Play",
                    icon = coreUiR.drawable.ic_play,
                    actionCode = PIP_ACTION_PLAY,
                )
            },
            createPipAction(
                context = activity,
                title = "Next",
                icon = coreUiR.drawable.ic_skip_next,
                actionCode = PIP_ACTION_NEXT,
            ),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPictureInPictureParams(): PictureInPictureParams =
        PictureInPictureParams.Builder().apply {
            setActions(actions)
            setAspectRatio(aspectRatio)
            sourceRectHint?.takeIf { it.width() > 0 && it.height() > 0 }?.let {
                setSourceRectHint(it)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setSeamlessResizeEnabled(true)
                setAutoEnterEnabled(autoEnterEnabled && (player.isPlaying || player.playWhenReady))
            }
        }.build()

    private fun applyPictureInPictureParams() {
        if (!isPipSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (activity.isFinishing || activity.isDestroyed) return
        runCatching {
            activity.setPictureInPictureParams(buildPictureInPictureParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipAction(
        context: Context,
        title: String,
        @DrawableRes icon: Int,
        actionCode: Int,
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(context, icon),
            title,
            title,
            PendingIntent.getBroadcast(
                context,
                actionCode,
                Intent(PIP_INTENT_ACTION).apply {
                    putExtra(PIP_INTENT_ACTION_CODE, actionCode)
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
