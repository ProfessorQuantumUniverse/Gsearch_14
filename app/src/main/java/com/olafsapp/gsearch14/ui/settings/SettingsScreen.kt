package com.olafsapp.gsearch14.ui.settings

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.BuildConfig
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.UrlLauncher
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.repo.AccentPalette
import com.olafsapp.gsearch14.data.repo.AppSettings
import com.olafsapp.gsearch14.data.repo.OpenTarget
import com.olafsapp.gsearch14.data.repo.ThemeMode
import com.olafsapp.gsearch14.ui.components.ActionRow
import com.olafsapp.gsearch14.ui.components.EngineListItem
import com.olafsapp.gsearch14.ui.components.OptionGroup
import com.olafsapp.gsearch14.ui.components.SectionHeader
import com.olafsapp.gsearch14.ui.components.SurfaceGroup
import com.olafsapp.gsearch14.ui.components.SwitchRow
import com.olafsapp.gsearch14.ui.theme.Motion
import com.olafsapp.gsearch14.ui.theme.accentSwatch
import kotlinx.coroutines.launch

// Taken from the git remote, not the account page: this must land on the repository.
private const val SOURCE_URL = "https://github.com/ProfessorQuantumUniverse/Gsearch_14"

@Composable
fun SettingsScreen(
    settings: AppSettings,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAccentChange: (AccentPalette) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onEngineChange: (SearchEngine) -> Unit,
    onSuggestionsChange: (Boolean) -> Unit,
    onIncognitoChange: (Boolean) -> Unit,
    onBlockCookiesChange: (Boolean) -> Unit,
    onOpenTargetChange: (OpenTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clearedMessage = stringResource(R.string.settings_browsing_data_cleared)

    // Material You is only available from Android 12; below that the accent picker is the
    // only way to change colours, so it is always shown there.
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accentPickerVisible = !dynamicColorAvailable || !settings.dynamicColor

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            SettingsHeader(onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // --- Look and feel ---
                item {
                    SectionHeader(text = stringResource(R.string.settings_section_look))
                }
                item {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                item {
                    OptionGroup(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        label = { stringResourceFor(it) },
                        body = { null },
                        onSelect = onThemeModeChange,
                    )
                }
                item {
                    SurfaceGroup {
                        if (dynamicColorAvailable) {
                            SwitchRow(
                                title = stringResource(R.string.settings_dynamic_color),
                                body = stringResource(R.string.settings_dynamic_color_body),
                                checked = settings.dynamicColor,
                                onCheckedChange = onDynamicColorChange,
                            )
                        }
                        AnimatedVisibility(
                            visible = accentPickerVisible,
                            enter = fadeIn(Motion.effectsFast()) +
                                expandVertically(Motion.sizeSpring),
                            exit = fadeOut(Motion.effectsFast()) +
                                shrinkVertically(Motion.sizeSpring),
                        ) {
                            AccentPicker(
                                selected = settings.accent,
                                darkTheme = darkTheme,
                                onSelect = onAccentChange,
                            )
                        }
                        SwitchRow(
                            title = stringResource(R.string.settings_pure_black),
                            body = stringResource(R.string.settings_pure_black_body),
                            checked = settings.pureBlackDark,
                            onCheckedChange = onPureBlackChange,
                        )
                        SwitchRow(
                            title = stringResource(R.string.settings_haptics),
                            body = stringResource(R.string.settings_haptics_body),
                            checked = settings.hapticsEnabled,
                            onCheckedChange = onHapticsChange,
                        )
                    }
                }

                // --- Search ---
                item {
                    SectionHeader(text = stringResource(R.string.settings_section_search))
                }
                item {
                    Text(
                        text = stringResource(R.string.engine_switcher_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                items(SearchEngine.ALL.size) { index ->
                    val engine = SearchEngine.ALL[index]
                    EngineListItem(
                        engine = engine,
                        selected = engine.id == settings.engine.id,
                        onClick = { onEngineChange(engine) },
                    )
                }
                item {
                    SurfaceGroup(modifier = Modifier.padding(top = 6.dp)) {
                        SwitchRow(
                            title = stringResource(R.string.settings_suggestions),
                            body = stringResource(R.string.settings_suggestions_body),
                            checked = settings.suggestionsEnabled,
                            onCheckedChange = onSuggestionsChange,
                        )
                    }
                }
                item {
                    SectionHeader(text = stringResource(R.string.settings_open_target))
                }
                item {
                    OptionGroup(
                        options = OpenTarget.entries,
                        selected = settings.openTarget,
                        label = { stringResourceFor(it).first },
                        body = { stringResourceFor(it).second },
                        onSelect = onOpenTargetChange,
                    )
                }

                // --- Privacy ---
                item {
                    SectionHeader(text = stringResource(R.string.settings_section_privacy))
                }
                item {
                    SurfaceGroup {
                        SwitchRow(
                            title = stringResource(R.string.settings_incognito),
                            body = stringResource(R.string.settings_incognito_body),
                            checked = settings.incognito,
                            onCheckedChange = onIncognitoChange,
                        )
                        SwitchRow(
                            title = stringResource(R.string.settings_block_cookies),
                            body = stringResource(R.string.settings_block_cookies_body),
                            checked = settings.blockThirdPartyCookies,
                            onCheckedChange = onBlockCookiesChange,
                        )
                        ActionRow(
                            title = stringResource(R.string.settings_clear_browsing_data),
                            body = stringResource(R.string.settings_clear_browsing_data_body),
                            icon = Icons.Rounded.DeleteForever,
                            onClick = {
                                clearBrowsingData(context)
                                scope.launch { snackbarHostState.showSnackbar(clearedMessage) }
                            },
                        )
                    }
                }

                // --- About ---
                item {
                    SectionHeader(text = stringResource(R.string.settings_section_about))
                }
                item {
                    SurfaceGroup {
                        ActionRow(
                            title = stringResource(
                                R.string.settings_version,
                                BuildConfig.VERSION_NAME,
                            ),
                            body = stringResource(R.string.settings_licence),
                            icon = null,
                            onClick = {},
                        )
                        ActionRow(
                            title = stringResource(R.string.settings_source),
                            body = SOURCE_URL.removePrefix("https://"),
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = { UrlLauncher.openExternal(context, SOURCE_URL) },
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .pressable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.results_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Accent swatches.
 *
 * Plain circles: a colour swatch is a colour, and the polygon silhouette the rest of the
 * app uses read as a smudge at this size. The selection ring sits in a fixed gap around
 * the swatch so it stays visible against light and dark colours alike.
 */
@Composable
private fun AccentPicker(
    selected: AccentPalette,
    darkTheme: Boolean,
    onSelect: (AccentPalette) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_accent),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentPalette.entries.forEach { accent ->
                val isSelected = accent == selected
                // Critically damped and clamped: this value feeds Modifier.border, which
                // rejects a negative width, and a bouncy spring undershoots past zero.
                val ringWidth by animateDpAsState(
                    targetValue = if (isSelected) 2.5.dp else 0.dp,
                    animationSpec = Motion.spatialSettle(),
                    label = "accentRing",
                )
                val swatchScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.86f,
                    animationSpec = Motion.spatialBouncy(),
                    label = "accentScale",
                )
                val name = accentName(accent)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = ringWidth.coerceAtLeast(0.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        )
                        .pressable(onClick = { onSelect(accent) })
                        .semantics {
                            contentDescription = name
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .scale(swatchScale)
                            .clip(CircleShape)
                            .background(accentSwatch(accent, darkTheme)),
                    )
                }
            }
        }
    }
}

@Composable
private fun accentName(accent: AccentPalette): String = stringResource(
    when (accent) {
        AccentPalette.SIGNAL -> R.string.accent_signal
        AccentPalette.FOREST -> R.string.accent_forest
        AccentPalette.EMBER -> R.string.accent_ember
        AccentPalette.INK -> R.string.accent_ink
        AccentPalette.VIOLET -> R.string.accent_violet
    },
)

@Composable
private fun stringResourceFor(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun stringResourceFor(target: OpenTarget): Pair<String, String> = when (target) {
    OpenTarget.IN_APP ->
        stringResource(R.string.open_in_app) to stringResource(R.string.open_in_app_body)

    OpenTarget.CUSTOM_TAB ->
        stringResource(R.string.open_custom_tab) to stringResource(R.string.open_custom_tab_body)

    OpenTarget.EXTERNAL_BROWSER ->
        stringResource(R.string.open_external) to stringResource(R.string.open_external_body)
}

/** Wipes what the in-app reader stored — cookies, HTML5 storage and the page cache. */
private fun clearBrowsingData(context: android.content.Context) {
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    WebStorage.getInstance().deleteAllData()
    WebView(context).apply {
        clearCache(true)
        clearHistory()
        destroy()
    }
}
