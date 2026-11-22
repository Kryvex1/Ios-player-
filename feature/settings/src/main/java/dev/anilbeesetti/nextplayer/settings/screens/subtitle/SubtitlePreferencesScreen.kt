package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.SubtitleAnimation
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.GlassDialogButton
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.SFProBoldFontFamily
import dev.anilbeesetti.nextplayer.core.ui.theme.SFProMediumFontFamily
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name
import dev.anilbeesetti.nextplayer.settings.utils.LocalesHelper
import java.nio.charset.Charset
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun SubtitlePreferencesScreen(
    viewModel: SubtitlePreferencesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SubtitlePreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubtitlePreferencesScreenContent(
    state: SubtitlePreferencesUiState,
    onAction: (SubtitlePreferencesUiEvent) -> Unit,
) {
    val languages = remember { listOf(Pair("None", "")) + LocalesHelper.getAvailableLocales() }
    val charsetResource = stringArrayResource(id = R.array.charsets_list)
    val context = LocalContext.current
    val customStyleEnabled = state.preferences.useSystemCaptionStyle.not()

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.subtitle),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = { onAction(SubtitlePreferencesUiEvent.NavigateUp) },
                        modifier = Modifier.tvFocusDown(listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(listFocusRequester)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_subtitle_lang),
                    description = LocalesHelper.getLocaleDisplayLanguage(state.preferences.preferredSubtitleLanguage)
                        .takeIf { it.isNotBlank() } ?: stringResource(R.string.preferred_subtitle_lang_description),
                    icon = NextIcons.Language,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleLanguageDialog)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.subtitle_text_encoding),
                    description = charsetResource.first { it.contains(state.preferences.subtitleTextEncoding) },
                    icon = NextIcons.Subtitle,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleEncodingDialog)) },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.appearance_name))

            // Live Subtitle Preview Box (tap to replay animation, updates live with all style & background controls)
            SubtitleLivePreviewBox(
                preferences = state.preferences,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.system_caption_style),
                    description = stringResource(R.string.system_caption_style_desc),
                    icon = NextIcons.Caption,
                    isChecked = state.preferences.useSystemCaptionStyle,
                    onChecked = { onAction(SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle) },
                    onClick = { context.startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS)) },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_text_bold),
                    description = stringResource(id = R.string.subtitle_text_bold_desc),
                    icon = NextIcons.Bold,
                    enabled = customStyleEnabled,
                    isChecked = state.preferences.subtitleTextBold,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleSubtitleTextBold) },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_text_color),
                    description = subtitleColorLabel(state.preferences.subtitleTextColor),
                    icon = NextIcons.Appearance,
                    enabled = customStyleEnabled,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleColorDialog)) },
                    trailingContent = {
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(state.preferences.subtitleTextColor))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.22f),
                                    shape = CircleShape,
                                ),
                        )
                    },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_text_size),
                    description = "${state.preferences.subtitleTextSize} sp",
                    icon = NextIcons.FontSize,
                    enabled = customStyleEnabled,
                    value = state.preferences.subtitleTextSize.toFloat(),
                    valueRange = 8f..36f,
                    onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(it.toInt())) },
                    trailingContent = {
                        FilledIconButton(
                            enabled = customStyleEnabled,
                            onClick = {
                                onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE))
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(id = R.string.reset),
                            )
                        }
                    },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_bottom_padding),
                    description = "${state.preferences.subtitleBottomPadding} dp",
                    icon = NextIcons.Length,
                    enabled = customStyleEnabled,
                    value = state.preferences.subtitleBottomPadding.toFloat(),
                    valueRange = 0f..80f,
                    onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBottomPadding(it.toInt())) },
                    trailingContent = {
                        FilledIconButton(
                            enabled = customStyleEnabled,
                            onClick = {
                                onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBottomPadding(PlayerPreferences.DEFAULT_SUBTITLE_BOTTOM_PADDING))
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(id = R.string.reset),
                            )
                        }
                    },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_animation),
                    description = when (state.preferences.subtitleAnimation) {
                        SubtitleAnimation.NONE -> stringResource(R.string.subtitle_animation_none)
                        SubtitleAnimation.FADE -> stringResource(R.string.subtitle_animation_fade)
                        SubtitleAnimation.TYPEWRITER -> stringResource(R.string.subtitle_animation_typewriter)
                        SubtitleAnimation.SLIDE_UP -> stringResource(R.string.subtitle_animation_slide_up)
                        SubtitleAnimation.POP_IN -> stringResource(R.string.subtitle_animation_pop_in)
                        SubtitleAnimation.WORD_BY_WORD -> stringResource(R.string.subtitle_animation_word_by_word)
                        SubtitleAnimation.SLIDE_HORIZONTAL -> stringResource(R.string.subtitle_animation_slide_horizontal)
                        SubtitleAnimation.BOUNCE_DROP -> stringResource(R.string.subtitle_animation_bounce_drop)
                    },
                    icon = NextIcons.Speed,
                    enabled = customStyleEnabled,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleAnimationDialog)) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_background),
                    description = stringResource(id = R.string.subtitle_background_desc),
                    icon = NextIcons.Background,
                    enabled = customStyleEnabled,
                    isChecked = state.preferences.subtitleBackground,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleSubtitleBackground) },
                )
                AnimatedVisibility(
                    visible = state.preferences.subtitleBackground && customStyleEnabled,
                    enter = fadeIn(tween(220)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(160)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        PreferenceSlider(
                            title = stringResource(id = R.string.subtitle_background_opacity),
                            description = "${(state.preferences.subtitleBackgroundOpacity * 100).roundToInt()}%",
                            icon = NextIcons.Contrast,
                            enabled = customStyleEnabled,
                            value = state.preferences.subtitleBackgroundOpacity,
                            valueRange = 0.05f..1.0f,
                            onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundOpacity(it)) },
                            trailingContent = {
                                FilledIconButton(
                                    enabled = customStyleEnabled,
                                    onClick = {
                                        onAction(
                                            SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundOpacity(
                                                PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_OPACITY,
                                            ),
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector = NextIcons.History,
                                        contentDescription = stringResource(id = R.string.reset),
                                    )
                                }
                            },
                        )
                        PreferenceSlider(
                            title = stringResource(id = R.string.subtitle_background_corner_radius),
                            description = "${state.preferences.subtitleBackgroundCornerRadius} dp",
                            icon = NextIcons.Style,
                            enabled = customStyleEnabled,
                            value = state.preferences.subtitleBackgroundCornerRadius.toFloat(),
                            valueRange = 0f..24f,
                            onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundCornerRadius(it.roundToInt())) },
                            trailingContent = {
                                FilledIconButton(
                                    enabled = customStyleEnabled,
                                    onClick = {
                                        onAction(
                                            SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundCornerRadius(
                                                PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_CORNER_RADIUS,
                                            ),
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector = NextIcons.History,
                                        contentDescription = stringResource(id = R.string.reset),
                                    )
                                }
                            },
                        )
                        PreferenceSlider(
                            title = stringResource(id = R.string.subtitle_background_vertical_padding),
                            description = "${state.preferences.subtitleBackgroundVerticalPadding} dp",
                            icon = NextIcons.Length,
                            enabled = customStyleEnabled,
                            value = state.preferences.subtitleBackgroundVerticalPadding.toFloat(),
                            valueRange = 0f..18f,
                            onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundVerticalPadding(it.roundToInt())) },
                            trailingContent = {
                                FilledIconButton(
                                    enabled = customStyleEnabled,
                                    onClick = {
                                        onAction(
                                            SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundVerticalPadding(
                                                PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_VERTICAL_PADDING,
                                            ),
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector = NextIcons.History,
                                        contentDescription = stringResource(id = R.string.reset),
                                    )
                                }
                            },
                        )
                    }
                }
                PreferenceSwitch(
                    title = stringResource(R.string.embedded_styles),
                    description = stringResource(R.string.embedded_styles_desc),
                    icon = NextIcons.Style,
                    isChecked = state.preferences.applyEmbeddedStyles,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles) },
                    isLastItem = true,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                SubtitlePreferenceDialog.SubtitleLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_subtitle_lang),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == state.preferences.preferredSubtitleLanguage,
                                onClick = {
                                    onAction(SubtitlePreferencesUiEvent.UpdateSubtitleLanguage(it.second))
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleFontDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_font),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(Font.entries.toTypedArray()) {
                            RadioTextButton(
                                text = if (it == Font.DEFAULT) "SF Pro (Default)" else it.name(),
                                selected = it == state.preferences.subtitleFont,
                                onClick = {
                                    onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFont(it))
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleAnimationDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_animation),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(SubtitleAnimation.entries.toTypedArray()) {
                            RadioTextButton(
                                text = when (it) {
                                    SubtitleAnimation.NONE -> stringResource(R.string.subtitle_animation_none)
                                    SubtitleAnimation.FADE -> stringResource(R.string.subtitle_animation_fade)
                                    SubtitleAnimation.TYPEWRITER -> stringResource(R.string.subtitle_animation_typewriter)
                                    SubtitleAnimation.SLIDE_UP -> stringResource(R.string.subtitle_animation_slide_up)
                                    SubtitleAnimation.POP_IN -> stringResource(R.string.subtitle_animation_pop_in)
                                    SubtitleAnimation.WORD_BY_WORD -> stringResource(R.string.subtitle_animation_word_by_word)
                                    SubtitleAnimation.SLIDE_HORIZONTAL -> stringResource(R.string.subtitle_animation_slide_horizontal)
                                    SubtitleAnimation.BOUNCE_DROP -> stringResource(R.string.subtitle_animation_bounce_drop)
                                },
                                selected = it == state.preferences.subtitleAnimation,
                                onClick = {
                                    onAction(SubtitlePreferencesUiEvent.UpdateSubtitleAnimation(it))
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleEncodingDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_text_encoding),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(charsetResource) {
                            val currentCharset = it.substringAfterLast("(", "").removeSuffix(")")
                            if (currentCharset.isEmpty() || Charset.isSupported(currentCharset)) {
                                RadioTextButton(
                                    text = it,
                                    selected = currentCharset == state.preferences.subtitleTextEncoding,
                                    onClick = {
                                        onAction(SubtitlePreferencesUiEvent.UpdateSubtitleEncoding(currentCharset))
                                        onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                    },
                                )
                            }
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleColorDialog -> {
                    SubtitleColorPickerDialog(
                        selectedColor = state.preferences.subtitleTextColor,
                        onColorSelected = { color ->
                            onAction(SubtitlePreferencesUiEvent.UpdateSubtitleTextColor(color))
                        },
                        onDismiss = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    )
                }
            }
        }
    }
}

private data class SubtitleColorPreset(
    val color: Long,
    val labelRes: Int,
)

private val SubtitleColorPresets = listOf(
    SubtitleColorPreset(0xFFFFFFFFL, R.string.subtitle_color_white),
    SubtitleColorPreset(0xFFFFEB3BL, R.string.subtitle_color_yellow),
    SubtitleColorPreset(0xFF00E5FFL, R.string.subtitle_color_cyan),
    SubtitleColorPreset(0xFF69F0AEL, R.string.subtitle_color_green),
    SubtitleColorPreset(0xFFFFB74DL, R.string.subtitle_color_amber),
    SubtitleColorPreset(0xFF80D8FFL, R.string.subtitle_color_sky_blue),
    SubtitleColorPreset(0xFFFF80ABL, R.string.subtitle_color_pink),
    SubtitleColorPreset(0xFFD6DCE5L, R.string.subtitle_color_silver),
)

@Composable
private fun subtitleColorLabel(colorValue: Long): String {
    val preset = SubtitleColorPresets.firstOrNull { it.color == colorValue }
    return if (preset != null) {
        stringResource(preset.labelRes)
    } else {
        val hex = String.format("#%06X", (colorValue and 0xFFFFFFL))
        "${stringResource(R.string.subtitle_color_custom)} ($hex)"
    }
}

@Composable
private fun SubtitleColorPickerDialog(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val initialHue = remember(selectedColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(Color(selectedColor).toArgb(), hsv)
        hsv[0]
    }
    var customHue by remember(selectedColor) { mutableFloatStateOf(initialHue) }

    NextDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.subtitle_text_color)) },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Live dark preview pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF141B2D), Color(0xFF0E1320)),
                            ),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.subtitle_preview_sample_text),
                        color = Color(selectedColor),
                        fontFamily = SFProBoldFontFamily,
                        fontSize = 15.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                offset = Offset(1.5f, 1.5f),
                                blurRadius = 3f,
                            ),
                        ),
                    )
                }

                // 2x4 Preset Swatch Grid
                SubtitleColorPresets.chunked(4).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = selectedColor == preset.color
                            val swatchColor = Color(preset.color)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .testTag("subtitle_color_${preset.color}")
                                    .pressScaleBounce(pressedScale = 0.90f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onColorSelected(preset.color) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.2.dp,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isDark -> Color.White.copy(alpha = 0.35f)
                                                else -> Color.Black.copy(alpha = 0.18f)
                                            },
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = NextIcons.Check,
                                            contentDescription = null,
                                            tint = if (swatchColor.luminance() > 0.5f) Color(0xFF10181C) else Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(preset.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // Compact Custom Hue Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.subtitle_color_custom),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = String.format("#%06X", (selectedColor and 0xFFFFFFL)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF4D4D),
                                            Color(0xFFFFD600),
                                            Color(0xFF00E676),
                                            Color(0xFF00E5FF),
                                            Color(0xFF448AFF),
                                            Color(0xFFE040FB),
                                            Color(0xFFFF4D4D),
                                        ),
                                    ),
                                )
                                .border(
                                    width = 0.8.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.14f),
                                    shape = CircleShape,
                                ),
                        )
                        Slider(
                            value = customHue,
                            onValueChange = { hue ->
                                customHue = hue
                                val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.78f, 1.0f))
                                onColorSelected(argb.toLong() and 0xFFFFFFFFL)
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(selectedColor),
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subtitle_color_hue_slider"),
                        )
                    }
                }
            }
        },
        dismissButton = {
            GlassDialogButton(
                text = stringResource(id = R.string.reset),
                emphasized = false,
                onClick = { onColorSelected(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR) },
            )
        },
        confirmButton = {
            GlassDialogButton(
                text = stringResource(id = R.string.okay),
                emphasized = true,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun SubtitleLivePreviewBox(
    preferences: PlayerPreferences,
    modifier: Modifier = Modifier,
) {
    val sampleText = stringResource(id = R.string.subtitle_preview_sample_text)
    var replayTick by remember { mutableIntStateOf(0) }
    val words = remember(sampleText) { sampleText.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    var revealCount by remember(preferences.subtitleAnimation, replayTick) { mutableIntStateOf(sampleText.length) }

    LaunchedEffect(preferences.subtitleAnimation, replayTick) {
        when (preferences.subtitleAnimation) {
            SubtitleAnimation.TYPEWRITER -> {
                revealCount = 1
                val step = (480L / sampleText.length.coerceAtLeast(1)).coerceIn(18L, 46L)
                for (i in 1..sampleText.length) {
                    revealCount = i
                    delay(step)
                }
            }
            SubtitleAnimation.WORD_BY_WORD -> {
                revealCount = 1
                val step = (560L / words.size.coerceAtLeast(1)).coerceIn(70L, 150L)
                for (i in 1..words.size) {
                    revealCount = i
                    delay(step)
                }
            }
            else -> revealCount = sampleText.length
        }
    }

    val displayText = when (preferences.subtitleAnimation) {
        SubtitleAnimation.TYPEWRITER -> sampleText.take(revealCount)
        SubtitleAnimation.WORD_BY_WORD -> words.take(revealCount).joinToString(" ")
        else -> sampleText
    }
    val fontFamily = if (preferences.subtitleTextBold) SFProBoldFontFamily else SFProMediumFontFamily
    val previewFontSize = preferences.subtitleTextSize.coerceIn(10, 26).sp
    val bgAlpha = preferences.subtitleBackgroundOpacity.coerceIn(0f, 1f)
    val cornerRadius = preferences.subtitleBackgroundCornerRadius.coerceIn(0, 28).dp
    val verticalPadding = preferences.subtitleBackgroundVerticalPadding.coerceIn(0, 20).dp

    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF141B2D),
                        Color(0xFF1F2A44),
                        Color(0xFF0E1320),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { replayTick++ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(id = R.string.subtitle_preview),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = Pair(preferences.subtitleAnimation, replayTick),
                transitionSpec = {
                    when (preferences.subtitleAnimation) {
                        SubtitleAnimation.FADE ->
                            fadeIn(tween(320)) togetherWith fadeOut(tween(180)) using SizeTransform(clip = false)
                        SubtitleAnimation.SLIDE_UP ->
                            (slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { 44 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(150)) { -24 } + fadeOut(tween(140))) using SizeTransform(clip = false)
                        SubtitleAnimation.POP_IN ->
                            (scaleIn(initialScale = 0.45f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200))) togetherWith
                                (scaleOut(targetScale = 0.72f, animationSpec = tween(150)) + fadeOut(tween(140))) using SizeTransform(clip = false)
                        SubtitleAnimation.SLIDE_HORIZONTAL ->
                            (slideInHorizontally(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { -72 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(160)) { 56 } + fadeOut(tween(140))) using SizeTransform(clip = false)
                        SubtitleAnimation.BOUNCE_DROP ->
                            (slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -52 } + scaleIn(initialScale = 0.82f) + fadeIn(tween(200))) togetherWith
                                (slideOutVertically(tween(150)) { 24 } + fadeOut(tween(140))) using SizeTransform(clip = false)
                        else ->
                            fadeIn(tween(160)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false)
                    }
                },
                label = "subtitle_settings_preview",
            ) { _ ->
                Surface(
                    color = if (preferences.subtitleBackground) Color.Black.copy(alpha = bgAlpha) else Color.Transparent,
                    shape = RoundedCornerShape(cornerRadius),
                ) {
                    Text(
                        text = displayText,
                        fontFamily = fontFamily,
                        fontWeight = if (preferences.subtitleTextBold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = previewFontSize,
                        color = Color(preferences.subtitleTextColor),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
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
        }
    }
}
