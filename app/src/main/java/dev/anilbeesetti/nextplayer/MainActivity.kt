package dev.anilbeesetti.nextplayer

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.anilbeesetti.nextplayer.core.common.cache.ThemePreferenceCache
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.common.service.system.SystemService
import dev.anilbeesetti.nextplayer.core.media.services.MediaOperationsService
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.ui.components.DialogBlurHost
import dev.anilbeesetti.nextplayer.core.ui.components.LocalLiquidGlassBackdrop
import dev.anilbeesetti.nextplayer.core.ui.components.LocalNavigationBottomPadding
import dev.anilbeesetti.nextplayer.core.ui.components.LocalTopLevelBottomBarVisibleSetter
import dev.anilbeesetti.nextplayer.core.ui.components.LocalTopLevelFabSetter
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabState
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.navigation.NextNavigationBar
import dev.anilbeesetti.nextplayer.navigation.NextNavigationRail
import dev.anilbeesetti.nextplayer.navigation.TopLevelDestination
import dev.anilbeesetti.nextplayer.navigation.TopLevelNavState
import dev.anilbeesetti.nextplayer.navigation.mediaNavGraph
import dev.anilbeesetti.nextplayer.navigation.moreNavGraph
import dev.anilbeesetti.nextplayer.navigation.navigationTransition
import dev.anilbeesetti.nextplayer.navigation.networkNavGraph
import dev.anilbeesetti.nextplayer.navigation.playlistNavGraph
import dev.anilbeesetti.nextplayer.navigation.rememberTopLevelNavState
import dev.anilbeesetti.nextplayer.navigation.settingsNavGraph
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mediaOperationsService: MediaOperationsService by inject()

    private val systemService: SystemService by inject()

    private val viewModel: MainViewModel by viewModel()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Synchronously read cached theme BEFORE super.onCreate/setContent to eliminate any white/light startup flash
        val cachedThemeConfigName = ThemePreferenceCache.getThemeConfigName(this)
        val cachedThemeConfig = ThemeConfig.entries.firstOrNull { it.name == cachedThemeConfigName } ?: ThemeConfig.SYSTEM
        val cachedHighContrast = ThemePreferenceCache.getHighContrastDark(this)
        val cachedDynamicColors = ThemePreferenceCache.getDynamicColors(this)
        val systemIsDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val initialDarkTheme = when (cachedThemeConfig) {
            ThemeConfig.ON -> true
            ThemeConfig.OFF -> false
            ThemeConfig.SYSTEM -> systemIsDark
        }

        AppCompatDelegate.setDefaultNightMode(
            when (cachedThemeConfig) {
                ThemeConfig.ON -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeConfig.OFF -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeConfig.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            },
        )

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Immediately paint window background to match resolved dark/light mode before first Compose frame
        window.setBackgroundDrawable(
            ColorDrawable(if (initialDarkTheme) Color.parseColor("#0F1115") else Color.parseColor("#F5F7FA")),
        )
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
                detectDarkMode = { initialDarkTheme },
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
                detectDarkMode = { initialDarkTheme },
            ),
        )

        requestHighRefreshRate()
        systemService.initialize(this@MainActivity)
        mediaOperationsService.initialize(this@MainActivity)

        splashScreen.setKeepOnScreenCondition {
            viewModel.state.value is MainActivityUiState.Loading
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val shouldUseDarkTheme = shouldUseDarkTheme(state = state, fallbackThemeConfig = cachedThemeConfig)
            val useHighContrast = shouldUseHighContrastDarkTheme(state = state, fallback = cachedHighContrast)
            val useDynamicColors = shouldUseDynamicTheming(state = state, fallback = cachedDynamicColors)

            // Keep synchronous cache up to date whenever DataStore preferences load or change
            LaunchedEffect(state) {
                if (state is MainActivityUiState.Success) {
                    val prefs = (state as MainActivityUiState.Success).preferences
                    ThemePreferenceCache.save(
                        context = this@MainActivity,
                        themeConfigName = prefs.themeConfig.name,
                        highContrastDark = prefs.useHighContrastDarkTheme,
                        dynamicColors = prefs.useDynamicColors,
                    )
                }
            }

            var showOnboarding by remember {
                mutableStateOf(
                    !this@MainActivity.isTelevision &&
                        dev.anilbeesetti.nextplayer.onboarding.OnboardingManager.shouldShowOnboarding(this@MainActivity),
                )
            }

            LaunchedEffect(shouldUseDarkTheme) {
                window.setBackgroundDrawable(
                    ColorDrawable(if (shouldUseDarkTheme) Color.parseColor("#0F1115") else Color.parseColor("#F5F7FA")),
                )
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { shouldUseDarkTheme },
                    ),
                )
            }

            NextPlayerTheme(
                darkTheme = shouldUseDarkTheme,
                highContrastDarkTheme = useHighContrast,
                dynamicColor = useDynamicColors,
            ) {
                DialogBlurHost {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showOnboarding) {
                            dev.anilbeesetti.nextplayer.onboarding.TelegramOnboardingScreen(
                                onFinished = { showOnboarding = false },
                            )
                        } else {
                            val navState = rememberTopLevelNavState()

                            val mediaStack = navState.backStacks.getValue(TopLevelDestination.MEDIA.route)
                            val playlistStack = navState.backStacks.getValue(TopLevelDestination.PLAYLISTS.route)
                            val networkStack = navState.backStacks.getValue(TopLevelDestination.NETWORK.route)
                            val moreStack = navState.backStacks.getValue(TopLevelDestination.MORE.route)

                            val provider = entryProvider {
                                mediaNavGraph(context = this@MainActivity, backStack = mediaStack)
                                playlistNavGraph(context = this@MainActivity, backStack = playlistStack)
                                networkNavGraph(context = this@MainActivity, backStack = networkStack)
                                moreNavGraph(context = this@MainActivity, backStack = moreStack)
                                settingsNavGraph(backStack = navState.currentStack)
                            }

                            val topLevelFabStates = remember { mutableStateMapOf<String, TopLevelFabState>() }
                            var showTopLevelBottomBar by remember { mutableStateOf(true) }
                            val bottomBarVisibleSetter: (Boolean) -> Unit = remember {
                                { visible: Boolean ->
                                    if (showTopLevelBottomBar != visible) {
                                        showTopLevelBottomBar = visible
                                    }
                                }
                            }
                            val fabSetter: (String, TopLevelFabState?) -> Unit = remember {
                                { key: String, fabState: TopLevelFabState? ->
                                    if (fabState == null) {
                                        topLevelFabStates.remove(key)
                                    } else {
                                        topLevelFabStates[key] = fabState
                                    }
                                }
                            }
                            NavigationLayout(
                                state = navState,
                                fabStates = topLevelFabStates,
                                showBottomBar = showTopLevelBottomBar,
                            ) { layoutPaddingValues ->
                                val topLevelKeys = navState.topLevelContentKeys
                                val navigationInsetsDecorator = remember(topLevelKeys) {
                                    NavEntryDecorator<NavKey> { entry ->
                                        Box(
                                            Modifier.thenIf(topLevelKeys.contains(entry.contentKey)) {
                                                consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                                            },
                                        ) {
                                            entry.Content()
                                        }
                                    }
                                }
                                CompositionLocalProvider(
                                    LocalNavigationBottomPadding provides layoutPaddingValues.calculateBottomPadding(),
                                    LocalTopLevelBottomBarVisibleSetter provides bottomBarVisibleSetter,
                                    LocalTopLevelFabSetter provides fabSetter,
                                ) {
                                    NavDisplay(
                                        entries = navState.rememberEntries(provider, navigationInsetsDecorator),
                                        onBack = { navState.goBack() },
                                        transitionSpec = { navState.navigationTransition(initialState, targetState, isPop = false) },
                                        popTransitionSpec = { navState.navigationTransition(initialState, targetState, isPop = true) },
                                        predictivePopTransitionSpec = { navState.navigationTransition(initialState, targetState, isPop = true) },
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    private fun requestHighRefreshRate() {
        try {
            val currentDisplay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return
            val currentMode = currentDisplay.mode
            val bestMode = currentDisplay.supportedModes
                .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                .maxByOrNull { it.refreshRate }
                ?: currentDisplay.supportedModes.maxByOrNull { it.refreshRate }
                ?: return
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = bestMode.modeId
                preferredRefreshRate = bestMode.refreshRate
            }
        } catch (_: Exception) {
        }
    }
}

@Composable
fun NavigationLayout(
    modifier: Modifier = Modifier,
    state: TopLevelNavState,
    fabStates: SnapshotStateMap<String, TopLevelFabState>,
    showBottomBar: Boolean,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
    content: @Composable (PaddingValues) -> Unit,
) {
    val isTv = LocalContext.current.isTelevision
    val density = LocalDensity.current
    var railWidth by remember(density) { mutableStateOf(0.dp) }
    val contentFocusRequester = remember { FocusRequester() }
    val fabFocusRequester = remember { FocusRequester() }
    val showNavRail = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val showNavigation = state.currentStack.lastOrNull()?.let { state.topLevelContentKeys.contains(it) } == true
    val selectedFabState = state.destinations[state.selectedIndex].fabKey?.let(fabStates::get)
    var displayedFabState by remember { mutableStateOf<TopLevelFabState?>(null) }
    LaunchedEffect(selectedFabState) {
        selectedFabState?.let { displayedFabState = it }
    }

    // Live LayerBackdrop capturing app content underneath the floating Liquid Glass navigation dock
    val navBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                CompositionLocalProvider(LocalLiquidGlassBackdrop provides navBackdrop) {
                    AnimatedVisibility(
                        visible = showNavigation && showBottomBar,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                    ) {
                        NextNavigationBar(
                            state = state,
                            fabState = displayedFabState,
                            contentFocusRequester = contentFocusRequester,
                            fabFocusRequester = fabFocusRequester,
                            showFabOnly = showNavRail,
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = WindowInsets(0.dp),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .layerBackdrop(navBackdrop)
                        .thenIf(isTv) {
                            focusRequester(contentFocusRequester)
                                .focusProperties {
                                    onExit = {
                                        if (requestedFocusDirection == FocusDirection.Down &&
                                            showNavigation && showBottomBar && displayedFabState != null
                                        ) {
                                            fabFocusRequester.requestFocus()
                                        }
                                    }
                                }
                                .focusGroup()
                        },
                ) {
                    content(
                        PaddingValues(
                            start = if (showNavRail) railWidth else 0.dp,
                            bottom = it.calculateBottomPadding(),
                        ),
                    )
                }
            },
        )
        AnimatedVisibility(
            visible = showNavRail && showNavigation,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            NextNavigationRail(
                state = state,
                modifier = Modifier.onSizeChanged { railWidth = with(density) { it.width.toDp() } },
            )
        }
    }
}

@Composable
fun shouldUseDarkTheme(
    state: MainActivityUiState,
    fallbackThemeConfig: ThemeConfig = ThemeConfig.SYSTEM,
): Boolean {
    val config = when (state) {
        MainActivityUiState.Loading -> fallbackThemeConfig
        is MainActivityUiState.Success -> state.preferences.themeConfig
    }
    return when (config) {
        ThemeConfig.SYSTEM -> isSystemInDarkTheme()
        ThemeConfig.OFF -> false
        ThemeConfig.ON -> true
    }
}

@Composable
fun shouldUseHighContrastDarkTheme(
    state: MainActivityUiState,
    fallback: Boolean = false,
): Boolean = when (state) {
    MainActivityUiState.Loading -> fallback
    is MainActivityUiState.Success -> state.preferences.useHighContrastDarkTheme
}

@Composable
fun shouldUseDynamicTheming(
    state: MainActivityUiState,
    fallback: Boolean = false,
): Boolean = when (state) {
    MainActivityUiState.Loading -> fallback
    is MainActivityUiState.Success -> state.preferences.useDynamicColors
}
