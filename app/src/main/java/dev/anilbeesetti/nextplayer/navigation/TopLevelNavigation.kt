package dev.anilbeesetti.nextplayer.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.GlassFab
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabKey
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabState
import dev.anilbeesetti.nextplayer.core.ui.components.adaptiveGlassPrism
import dev.anilbeesetti.nextplayer.core.ui.components.liquidGlass
import dev.anilbeesetti.nextplayer.core.ui.components.pressScaleBounce
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.more.navigation.MoreRoute
import dev.anilbeesetti.nextplayer.feature.network.navigation.NetworkRoute
import dev.anilbeesetti.nextplayer.feature.playlist.navigation.PlaylistListRoute
import dev.anilbeesetti.nextplayer.feature.videopicker.navigation.MediaPickerRoute
import kotlin.math.roundToInt

/**
 * Top-level destinations shown in the bottom bar / nav rail. The first entry is the start (exit)
 * destination — pressing back from any other tab returns here before leaving the app.
 */
enum class TopLevelDestination(
    val route: NavKey,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    val fabKey: String? = null,
) {
    MEDIA(MediaPickerRoute(), NextIcons.Home, R.string.home, TopLevelFabKey.MEDIA),
    PLAYLISTS(PlaylistListRoute, NextIcons.Playlist, R.string.playlists, TopLevelFabKey.PLAYLISTS),
    NETWORK(NetworkRoute, NextIcons.Network, R.string.network, TopLevelFabKey.NETWORK),
    MORE(MoreRoute, NextIcons.More, R.string.more, TopLevelFabKey.MORE),
}

@Composable
fun rememberTopLevelNavState(): TopLevelNavState {
    val destinations = TopLevelDestination.entries
    val backStacks = destinations.associate { dest ->
        val backStack = rememberNavBackStack(dest.route)
        backStack.ensureRoot(dest.route)
        dest.route to backStack
    }
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }
    return remember(backStacks, selectedIndex) {
        TopLevelNavState(destinations, backStacks, selectedIndex)
    }
}

@Stable
class TopLevelNavState(
    val destinations: List<TopLevelDestination>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
    private val selectedIndexState: MutableIntState,
) {
    var selectedIndex by selectedIndexState
        private set

    private val startRoute: NavKey get() = destinations.first().route

    val topLevelRoute: NavKey get() = destinations[selectedIndex].route

    val currentStack: NavBackStack<NavKey> get() = backStacks.getValue(topLevelRoute)

    private val stacksInUse: List<NavKey>
        get() = if (selectedIndex == 0) listOf(startRoute) else listOf(startRoute, topLevelRoute)

    val topLevelContentKeys: Set<Any> = destinations.flatMap { dest ->
        listOf(
            dest.route,
            dest.route.toString(),
            Pair("${dest.route}", "${dest.route::class}"),
        )
    }.toSet()

    fun switchTo(route: NavKey) {
        val index = destinations.indexOfFirst { it.route == route }
        if (index >= 0) selectedIndex = index
    }

    fun goBack() {
        val stack = currentStack
        when {
            stack.size > 1 -> stack.removeLastOrNull()
            selectedIndex != 0 -> selectedIndex = 0
        }
    }

    @Composable
    fun rememberEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
        entryDecorator: NavEntryDecorator<NavKey>,
    ): SnapshotStateList<NavEntry<NavKey>> {
        val saveableDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
        val viewModelDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
        val decorators = remember(entryDecorator, saveableDecorator, viewModelDecorator) {
            listOf(saveableDecorator, viewModelDecorator, entryDecorator)
        }

        val decoratedByRoute = destinations.associate { dest ->
            dest.route to rememberDecoratedNavEntries(
                backStack = backStacks.getValue(dest.route),
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        val activeStacks = stacksInUse
        return remember(activeStacks, decoratedByRoute) {
            activeStacks.flatMap { decoratedByRoute.getValue(it) }.toMutableStateList()
        }
    }
}

fun TopLevelNavState.indexOfDestination(contentKey: Any?): Int {
    if (contentKey == null) return -1
    val keyStr = contentKey.toString()
    return destinations.indexOfFirst { dest ->
        contentKey == dest.route ||
            keyStr == dest.route.toString() ||
            keyStr.contains(dest.route::class.java.simpleName)
    }
}

fun TopLevelNavState.isNavigationBetweenTopLevelDestinations(initialState: Scene<NavKey>, targetState: Scene<NavKey>): Boolean =
    topLevelContentKeys.run { contains(initialState.entries.lastOrNull()?.contentKey) && contains(targetState.entries.lastOrNull()?.contentKey) }

internal fun TopLevelNavState.navigationTransition(
    initialState: Scene<NavKey>,
    targetState: Scene<NavKey>,
    isPop: Boolean = false,
): ContentTransform {
    if (isNavigationBetweenTopLevelDestinations(initialState, targetState)) {
        return ContentTransform(
            targetContentEnter = fadeIn(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            ) + scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            ),
            initialContentExit = fadeOut(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            ),
            targetContentZIndex = 1f,
        )
    }

    val resolvedPop = isPop ||
        initialState.previousEntries.any { it.contentKey == targetState.entries.lastOrNull()?.contentKey } ||
        targetState.entries.size < initialState.entries.size

    return if (resolvedPop) {
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + fadeIn(
                initialAlpha = 0.85f,
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
            ),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + fadeOut(
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
            ),
            targetContentZIndex = -1f,
        )
    } else {
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + fadeIn(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            ),
            initialContentExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + fadeOut(
                targetAlpha = 0.85f,
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
            ),
            targetContentZIndex = 1f,
        )
    }
}

@Composable
fun NextNavigationBar(
    state: TopLevelNavState,
    fabState: TopLevelFabState?,
    contentFocusRequester: FocusRequester,
    fabFocusRequester: FocusRequester,
    showFabOnly: Boolean = false,
) {
    val isTv = LocalContext.current.isTelevision
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(condition = !showFabOnly) {
                background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                        ),
                    ),
                )
            }
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(bottom = 12.dp)
            .padding(horizontal = 16.dp)
            .thenIf(condition = !showFabOnly) {
                pointerInput(Unit) {}
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!showFabOnly) {
                NavigationView(
                    destinations = state.destinations,
                    isSelected = { it.route == state.topLevelRoute },
                    onDestinationClick = { state.switchTo(it.route) },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            fabState?.let { fab ->
                GlassFab(
                    onClick = fab.onClick,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .testTag("top_level_fab")
                        .focusRequester(fabFocusRequester)
                        .tvFocusRing(shape = RoundedCornerShape(18.dp))
                        .focusProperties {
                            if (isTv) {
                                up = fab.upFocusRequester.takeUnless { it == FocusRequester.Default }
                                    ?: contentFocusRequester
                            }
                        },
                ) {
                    AnimatedContent(
                        targetState = fab.icon,
                        transitionSpec = {
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.4f) togetherWith
                                fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                                scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 0.4f)
                        },
                        label = "fab_icon_swap",
                    ) { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private data class TabBounds(val leftPx: Float, val rightPx: Float, val heightPx: Float)

@Composable
fun NavigationView(
    modifier: Modifier = Modifier,
    destinations: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    val selectedIndex = destinations.indexOfFirst { isSelected(it) }.coerceAtLeast(0)
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val density = LocalDensity.current

    val activeContentColor = colorScheme.onPrimaryContainer
    val inactiveContentColor = if (isDark) colorScheme.onSurfaceVariant else Color(0xFF283238)
    val pillTint = colorScheme.primaryContainer.copy(alpha = if (isDark) 0.88f else 0.94f)
    val dockShape = RoundedCornerShape(28.dp)
    val pillShape = RoundedCornerShape(22.dp)

    // Track slide direction for dual-spring elastic stretch (leading edge fast, trailing edge elastic)
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    val movingRight = selectedIndex >= previousIndex
    LaunchedEffect(selectedIndex) {
        previousIndex = selectedIndex
    }

    val tabBounds = remember { mutableStateMapOf<Int, TabBounds>() }
    val activeBounds = tabBounds[selectedIndex]

    val leadingSpring = spring<Float>(
        dampingRatio = 0.66f,
        stiffness = 460f,
    )
    val trailingSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 210f,
    )

    val animatedLeftPx by animateFloatAsState(
        targetValue = activeBounds?.leftPx ?: 0f,
        animationSpec = if (movingRight) trailingSpring else leadingSpring,
        label = "liquid_pill_left",
    )
    val animatedRightPx by animateFloatAsState(
        targetValue = activeBounds?.rightPx ?: 0f,
        animationSpec = if (movingRight) leadingSpring else trailingSpring,
        label = "liquid_pill_right",
    )

    val pillHighlightBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.48f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.24f),
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

    // iOS 26 Liquid Glass navigation dock with adaptive Light/Dark contrast, live backdrop refraction & dual-spring elastic pill
    Box(
        modifier = modifier
            .liquidGlass(
                shape = dockShape,
                blurRadius = 5.dp,
                refractionHeight = 10.dp,
                refractionAmount = 18.dp,
                scrimColor = if (isDark) {
                    Color(0xFF141822).copy(alpha = 0.62f)
                } else {
                    Color.White.copy(alpha = 0.80f)
                },
                fallbackColor = if (isDark) {
                    Color(0xFF161A24).copy(alpha = 0.90f)
                } else {
                    Color.White.copy(alpha = 0.94f)
                },
                highlightAlpha = if (isDark) 0.48f else 0.78f,
                highlightWidth = 1.dp,
                adaptiveLightBorder = true,
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        // Dual-spring elastic sliding glass pill drawn cleanly behind tab labels
        if (activeBounds != null && animatedRightPx > animatedLeftPx) {
            val pillWidthDp = with(density) { (animatedRightPx - animatedLeftPx).coerceAtLeast(0f).toDp() }
            val pillHeightDp = with(density) { activeBounds.heightPx.coerceAtLeast(0f).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(animatedLeftPx.roundToInt(), 0) }
                    .width(pillWidthDp)
                    .height(pillHeightDp)
                    .clip(pillShape)
                    .background(pillTint)
                    .border(
                        border = BorderStroke(1.dp, pillHighlightBrush),
                        shape = pillShape,
                    ),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            destinations.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (selected) activeContentColor else inactiveContentColor,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "nav_item_content_$index",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "nav_icon_scale_$index",
                )
                val horizontalPad by animateDpAsState(
                    targetValue = if (selected) 18.dp else 14.dp,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                    ),
                    label = "nav_item_pad_$index",
                )

                Row(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val left = coords.positionInParent().x
                            val right = left + coords.size.width.toFloat()
                            val height = coords.size.height.toFloat()
                            val prev = tabBounds[index]
                            if (prev == null ||
                                kotlin.math.abs(prev.leftPx - left) > 0.5f ||
                                kotlin.math.abs(prev.rightPx - right) > 0.5f ||
                                kotlin.math.abs(prev.heightPx - height) > 0.5f
                            ) {
                                tabBounds[index] = TabBounds(left, right, height)
                            }
                        }
                        .pressScaleBounce(pressedScale = 0.90f)
                        .clip(pillShape)
                        .selectable(
                            selected = selected,
                            onClick = { onDestinationClick(destination) },
                            role = Role.Tab,
                            indication = null,
                            interactionSource = null,
                        )
                        .tvFocusRing(shape = pillShape)
                        .padding(horizontal = horizontalPad, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(180)) + expandHorizontally(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                ),
                            ),
                            exit = fadeOut(tween(120)) + shrinkHorizontally(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                ),
                            ),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = stringResource(destination.labelRes),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NextNavigationRail(state: TopLevelNavState, modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            state.destinations.forEach { dest ->
                val selected = state.topLevelRoute == dest.route
                Box(
                    Modifier
                        .pressScaleBounce(pressedScale = 0.90f)
                        .tvFocusRing(shape = RoundedCornerShape(99.dp))
                        .then(
                            if (selected) {
                                Modifier.adaptiveGlassPrism(
                                    shape = CircleShape,
                                    selected = true,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Modifier.clip(CircleShape)
                            },
                        )
                        .selectable(
                            selected = state.topLevelRoute == dest.route,
                            onClick = { state.switchTo(dest.route) },
                            role = Role.Tab,
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                    propagateMinConstraints = true,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = stringResource(dest.labelRes),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
