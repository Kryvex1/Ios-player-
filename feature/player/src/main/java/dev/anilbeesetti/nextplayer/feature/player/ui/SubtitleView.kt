package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView as Media3SubtitleView
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.SubtitleAnimation
import dev.anilbeesetti.nextplayer.core.ui.theme.KohinoorDevanagariFontFamily
import dev.anilbeesetti.nextplayer.core.ui.theme.SFProBoldFontFamily
import dev.anilbeesetti.nextplayer.core.ui.theme.SFProMediumFontFamily
import dev.anilbeesetti.nextplayer.feature.player.state.rememberCuesState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun SubtitleView(
    modifier: Modifier = Modifier,
    player: Player,
    isInPictureInPictureMode: Boolean,
    configuration: SubtitleConfiguration,
) {
    val cuesState = rememberCuesState(player)
    val cues = cuesState.currentCues?.cues

    val paddingFraction = configuration.bottomPadding.toFloat() / 400f

    Box(modifier = modifier.fillMaxSize()) {
        if (configuration.useSystemCaptionStyle && configuration.animation == SubtitleAnimation.NONE) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    Media3SubtitleView(context).apply {
                        setBottomPaddingFraction(paddingFraction)
                        applySystemSubtitleStyle(
                            context = context,
                            subtitleView = this,
                            configuration = configuration,
                        )
                    }
                },
                update = { subtitleView ->
                    subtitleView.setCues(cues)
                    subtitleView.setBottomPaddingFraction(paddingFraction)
                    applySystemSubtitleStyle(
                        context = subtitleView.context,
                        subtitleView = subtitleView,
                        configuration = configuration,
                    )
                    if (isInPictureInPictureMode) {
                        subtitleView.setFractionalTextSize(Media3SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 0.75f)
                    } else {
                        val isPortrait = subtitleView.context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                        val targetSize = if (isPortrait) {
                            (configuration.textSize.toFloat() * 0.85f).coerceAtLeast(8f)
                        } else {
                            configuration.textSize.toFloat()
                        }
                        subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, targetSize)
                    }
                },
            )
        } else {
            ComposeSubtitleRenderer(
                modifier = Modifier,
                cues = cues,
                isInPictureInPictureMode = isInPictureInPictureMode,
                configuration = configuration,
            )
        }
    }
}

@Composable
private fun ComposeSubtitleRenderer(
    modifier: Modifier = Modifier,
    cues: List<Cue>?,
    isInPictureInPictureMode: Boolean,
    configuration: SubtitleConfiguration,
) {
    val screenConfig = LocalConfiguration.current
    val isPortrait = screenConfig.orientation == Configuration.ORIENTATION_PORTRAIT

    val rawText = remember(cues) {
        cues?.joinToString("\n") { it.text?.toString().orEmpty() }?.trim().orEmpty()
    }

    val targetSize = remember(configuration.textSize, isPortrait, isInPictureInPictureMode) {
        when {
            isInPictureInPictureMode -> (configuration.textSize.toFloat() * 0.65f).coerceAtLeast(8f)
            isPortrait -> (configuration.textSize.toFloat() * 0.85f).coerceAtLeast(8f)
            else -> configuration.textSize.toFloat()
        }
    }

    val containsDevanagari = remember(rawText) { rawText.containsDevanagari() }
    val resolvedFontFamily = remember(
        containsDevanagari,
        configuration.textBold,
    ) {
        if (containsDevanagari) {
            KohinoorDevanagariFontFamily
        } else {
            if (configuration.textBold) SFProBoldFontFamily else SFProMediumFontFamily
        }
    }

    val words = remember(rawText) { rawText.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    var revealCount by remember(rawText, configuration.animation) { mutableIntStateOf(0) }
    LaunchedEffect(rawText, configuration.animation) {
        if (rawText.isBlank()) {
            revealCount = 0
            return@LaunchedEffect
        }
        when (configuration.animation) {
            SubtitleAnimation.TYPEWRITER -> {
                revealCount = 1
                val totalChars = rawText.length
                val stepDelay = (480L / totalChars.coerceAtLeast(1)).coerceIn(18L, 52L)
                for (i in 1..totalChars) {
                    revealCount = i
                    delay(stepDelay)
                }
            }
            SubtitleAnimation.WORD_BY_WORD -> {
                revealCount = 1
                val totalWords = words.size
                val stepDelay = (560L / totalWords.coerceAtLeast(1)).coerceIn(70L, 160L)
                for (i in 1..totalWords) {
                    revealCount = i
                    delay(stepDelay)
                }
            }
            else -> {
                revealCount = rawText.length
            }
        }
    }

    val displayText = when (configuration.animation) {
        SubtitleAnimation.TYPEWRITER -> rawText.take(revealCount)
        SubtitleAnimation.WORD_BY_WORD -> words.take(revealCount).joinToString(" ")
        else -> rawText
    }

    // Keep subtitle hidden directly when player controls are shown (without sliding up),
    // but keep AnimatedContent mounted between empty and non-empty cues so every cue animates smoothly!
    val hideForControls = configuration.controlsVisible && !isInPictureInPictureMode

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = configuration.bottomPadding.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (hideForControls) return@Box

        when (configuration.animation) {
            SubtitleAnimation.FADE -> {
                AnimatedContent(
                    targetState = rawText,
                    transitionSpec = {
                        fadeIn(tween(320, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(220, easing = FastOutSlowInEasing)) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    label = "subtitle_fade",
                ) { text ->
                    SubtitleOrSpacer(
                        text = text,
                        fontFamily = resolvedFontFamily,
                        fontSize = targetSize,
                        configuration = configuration,
                    )
                }
            }
            SubtitleAnimation.SLIDE_UP -> {
                AnimatedContent(
                    targetState = rawText,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) { fullHeight -> (fullHeight * 1.1f).toInt().coerceAtLeast(48) } +
                            fadeIn(tween(260))) togetherWith
                            (slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { fullHeight ->
                                -(fullHeight * 0.5f).toInt().coerceAtLeast(24)
                            } + fadeOut(tween(180))) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    label = "subtitle_slide_up",
                ) { text ->
                    SubtitleOrSpacer(
                        text = text,
                        fontFamily = resolvedFontFamily,
                        fontSize = targetSize,
                        configuration = configuration,
                    )
                }
            }
            SubtitleAnimation.POP_IN -> {
                AnimatedContent(
                    targetState = rawText,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.45f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) + fadeIn(tween(220))) togetherWith
                            (scaleOut(targetScale = 0.72f, animationSpec = tween(180)) + fadeOut(tween(160))) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    label = "subtitle_pop_in",
                ) { text ->
                    SubtitleOrSpacer(
                        text = text,
                        fontFamily = resolvedFontFamily,
                        fontSize = targetSize,
                        configuration = configuration,
                    )
                }
            }
            SubtitleAnimation.SLIDE_HORIZONTAL -> {
                AnimatedContent(
                    targetState = rawText,
                    transitionSpec = {
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) { fullWidth -> -(fullWidth * 0.75f).toInt().coerceAtLeast(72) } +
                            fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { fullWidth ->
                                (fullWidth * 0.65f).toInt().coerceAtLeast(56)
                            } + fadeOut(tween(160))) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    label = "subtitle_slide_horizontal",
                ) { text ->
                    SubtitleOrSpacer(
                        text = text,
                        fontFamily = resolvedFontFamily,
                        fontSize = targetSize,
                        configuration = configuration,
                    )
                }
            }
            SubtitleAnimation.BOUNCE_DROP -> {
                AnimatedContent(
                    targetState = rawText,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) { fullHeight -> -(fullHeight * 1.35f).toInt().coerceAtLeast(60) } +
                            scaleIn(initialScale = 0.80f, animationSpec = tween(220)) +
                            fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(180)) { fullHeight ->
                                (fullHeight * 0.55f).toInt().coerceAtLeast(28)
                            } + fadeOut(tween(150))) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    label = "subtitle_bounce_drop",
                ) { text ->
                    SubtitleOrSpacer(
                        text = text,
                        fontFamily = resolvedFontFamily,
                        fontSize = targetSize,
                        configuration = configuration,
                    )
                }
            }
            SubtitleAnimation.TYPEWRITER,
            SubtitleAnimation.WORD_BY_WORD,
            -> {
                AnimatedContent(
                    targetState = rawText.isNotBlank(),
                    transitionSpec = {
                        (scaleIn(initialScale = 0.90f, animationSpec = tween(200)) + fadeIn(tween(180))) togetherWith
                            fadeOut(tween(160)) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.BottomCenter,
                    label = "subtitle_progressive_reveal",
                ) { hasCue ->
                    if (hasCue && displayText.isNotBlank()) {
                        SubtitlePill(
                            text = displayText,
                            fontFamily = resolvedFontFamily,
                            fontSize = targetSize,
                            configuration = configuration,
                        )
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }
                }
            }
            SubtitleAnimation.NONE -> {
                SubtitleOrSpacer(
                    text = rawText,
                    fontFamily = resolvedFontFamily,
                    fontSize = targetSize,
                    configuration = configuration,
                )
            }
        }
    }
}

@Composable
private fun SubtitleOrSpacer(
    text: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: Float,
    configuration: SubtitleConfiguration,
) {
    if (text.isNotBlank()) {
        SubtitlePill(
            text = text,
            fontFamily = fontFamily,
            fontSize = fontSize,
            configuration = configuration,
        )
    } else {
        Spacer(modifier = Modifier.size(1.dp))
    }
}

@Composable
private fun SubtitlePill(
    text: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: Float,
    configuration: SubtitleConfiguration,
) {
    val bgAlpha = configuration.backgroundOpacity.coerceIn(0f, 1f)
    val cornerRadius = configuration.backgroundCornerRadius.coerceIn(0, 32).dp
    val verticalPadding = configuration.backgroundVerticalPadding.coerceIn(0, 24).dp

    Surface(
        color = if (configuration.showBackground) Color.Black.copy(alpha = bgAlpha) else Color.Transparent,
        shape = RoundedCornerShape(cornerRadius),
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = if (configuration.textBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = fontSize.sp,
            color = Color(configuration.textColor),
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f,
                ),
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = verticalPadding),
        )
    }
}

@OptIn(UnstableApi::class)
private fun applySystemSubtitleStyle(
    context: Context,
    subtitleView: Media3SubtitleView,
    configuration: SubtitleConfiguration,
) {
    val captioningManager = getSystemService(context, CaptioningManager::class.java)
    if (captioningManager != null) {
        val systemCaptionStyle = CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        subtitleView.setStyle(systemCaptionStyle)
    }
    subtitleView.setApplyEmbeddedStyles(configuration.applyEmbeddedStyles)
}

private fun CharSequence?.containsDevanagari(): Boolean {
    if (this == null) return false
    for (i in 0 until length) {
        val code = this[i].code
        if (code in 0x0900..0x097F || code in 0xA8E0..0xA8FF || code in 0x1CD0..0x1CFF) return true
    }
    return false
}

@Stable
data class SubtitleConfiguration(
    val useSystemCaptionStyle: Boolean,
    val showBackground: Boolean,
    val font: Font,
    val textSize: Int,
    val textBold: Boolean,
    val applyEmbeddedStyles: Boolean,
    val customRegularFontUri: String? = null,
    val customBoldFontUri: String? = null,
    val customHindiFontUri: String? = null,
    val animation: SubtitleAnimation = SubtitleAnimation.NONE,
    val bottomPadding: Int = PlayerPreferences.DEFAULT_SUBTITLE_BOTTOM_PADDING,
    val backgroundOpacity: Float = PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_OPACITY,
    val backgroundCornerRadius: Int = PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_CORNER_RADIUS,
    val backgroundVerticalPadding: Int = PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_VERTICAL_PADDING,
    val controlsVisible: Boolean = false,
    val textColor: Long = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR,
)
