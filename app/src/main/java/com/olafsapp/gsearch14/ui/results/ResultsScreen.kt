package com.olafsapp.gsearch14.ui.results

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.UrlLauncher
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.ui.components.EngineBadge
import com.olafsapp.gsearch14.ui.components.EmptyState
import com.olafsapp.gsearch14.ui.components.MorphingLoader
import com.olafsapp.gsearch14.ui.theme.Motion

/**
 * The in-app reader.
 *
 * Notable differences from version 3: no hardcoded user agent — the old one claimed to be
 * Chrome 91 on a 2019 Galaxy, which made engines serve stale layouts — plus a real loading
 * bar, a real error state, algorithmic darkening on dark themes, and optional third-party
 * cookie blocking.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ResultsScreen(
    url: String,
    query: String,
    engine: SearchEngine,
    bookmarkedUrls: Set<String>,
    blockThirdPartyCookies: Boolean,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: (title: String, url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var progress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf(query) }
    // Only the very first load gets the full-screen indicator; later navigations already
    // have a painted page underneath and just use the progress bar.
    var firstPaintDone by remember { mutableStateOf(false) }

    // Checked against the URL actually on screen, not the one this screen was opened with.
    // Wikipedia redirects a search straight to the article, and Google appends tracking
    // parameters, so the two diverge as soon as the page loads — which is why the bookmark
    // state used to be wrong on exactly those engines.
    val isBookmarked = currentUrl in bookmarkedUrls

    val spinnerColor = MaterialTheme.colorScheme.primary.toArgb()
    val spinnerTrack = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                // Deliberately left at the platform default: a truthful user agent gets the
                // current mobile layout from every engine.
                mediaPlaybackRequiresUserGesture = true
                safeBrowsingEnabled = true
            }
        }
    }

    val swipeRefresh = remember {
        SwipeRefreshLayout(context).apply {
            addView(webView)
            setOnRefreshListener { webView.reload() }
        }
    }

    // Keep the WebView's own dark rendering in step with the app theme.
    LaunchedEffect(darkTheme) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, darkTheme)
        }
    }

    LaunchedEffect(blockThirdPartyCookies) {
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(webView, !blockThirdPartyCookies)
    }

    LaunchedEffect(webView) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                loading = true
                failed = false
                pageUrl?.let { currentUrl = it }
            }

            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                loading = false
                firstPaintDone = true
                swipeRefresh.isRefreshing = false
                pageUrl?.let { currentUrl = it }
                view?.title?.takeIf { it.isNotBlank() }?.let { pageTitle = it }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                // Sub-resource failures are noise; only a failed main frame is an error the
                // user needs to see.
                if (request?.isForMainFrame == true) {
                    failed = true
                    loading = false
                    swipeRefresh.isRefreshing = false
                }
            }

            /**
             * The renderer runs in its own process and can be killed under memory pressure.
             * Without handling this the whole app is terminated, so the dead view is
             * discarded and the error state is shown instead.
             */
            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?,
            ): Boolean {
                swipeRefresh.removeAllViews()
                view?.destroy()
                loading = false
                failed = true
                return true
            }
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress / 100f
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.takeIf { it.isNotBlank() }?.let { pageTitle = it }
            }
        }
    }

    LaunchedEffect(url) {
        webView.loadUrl(url)
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            swipeRefresh.removeAllViews()
            webView.destroy()
        }
    }

    // In-page history first, then out of the screen — the behaviour a browser has.
    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ResultsTopBar(
            query = query,
            host = UrlLauncher.hostLabel(currentUrl),
            engine = engine,
            isBookmarked = isBookmarked,
            onBack = onBack,
            onReload = { webView.reload() },
            onShare = { UrlLauncher.share(context, currentUrl, pageTitle) },
            onOpenExternal = { UrlLauncher.openExternal(context, currentUrl) },
            onToggleBookmark = { onToggleBookmark(pageTitle, currentUrl) },
        )

        LoadingBar(progress = progress, visible = loading)

        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { swipeRefresh },
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                update = { layout ->
                    layout.setColorSchemeColors(spinnerColor)
                    layout.setProgressBackgroundColorSchemeColor(spinnerTrack)
                },
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = loading && !firstPaintDone && !failed,
                enter = fadeIn(Motion.effectsFast()),
                exit = fadeOut(Motion.effectsDefault()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    MorphingLoader(size = 44.dp)
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = failed,
                enter = fadeIn(Motion.effectsDefault()),
                exit = fadeOut(Motion.effectsFast()),
            ) {
                ErrorState(
                    onRetry = {
                        failed = false
                        webView.reload()
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultsTopBar(
    query: String,
    host: String,
    engine: SearchEngine,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarAction(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.results_back),
            onClick = onBack,
        )

        Spacer(Modifier.width(4.dp))
        EngineBadge(engine = engine, size = 30.dp)
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = query,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (host.isNotEmpty()) {
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BookmarkAction(isBookmarked = isBookmarked, onClick = onToggleBookmark)
        BarAction(
            icon = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.results_reload),
            onClick = onReload,
        )
        BarAction(
            icon = Icons.Rounded.Share,
            contentDescription = stringResource(R.string.results_share),
            onClick = onShare,
        )
        BarAction(
            icon = Icons.Rounded.OpenInBrowser,
            contentDescription = stringResource(R.string.results_open_external),
            onClick = onOpenExternal,
        )
    }
}

@Composable
private fun BarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The bookmark toggle pops slightly when it turns on — small reward, no delay. */
@Composable
private fun BookmarkAction(isBookmarked: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked) 1.15f else 1f,
        animationSpec = Motion.spatialBouncy(),
        label = "bookmarkScale",
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isBookmarked) {
                Icons.Rounded.Bookmark
            } else {
                Icons.Rounded.BookmarkBorder
            },
            contentDescription = stringResource(
                if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add,
            ),
            tint = if (isBookmarked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(20.dp)
                .scale(scale),
        )
    }
}

/** A determinate bar that fades out once the page is done rather than snapping away. */
@Composable
private fun LoadingBar(progress: Float, visible: Boolean) {
    val loadingDescription = stringResource(R.string.results_loading)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = Motion.effectsFast(),
        label = "loadProgress",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.effectsDefault(),
        label = "loadAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .graphicsLayer { this.alpha = alpha }
            .semantics { if (visible) contentDescription = loadingDescription }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Rounded.CloudOff,
            title = stringResource(R.string.results_offline_title),
            body = stringResource(R.string.results_offline_body),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary)
                .pressable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 13.dp),
        ) {
            Text(
                text = stringResource(R.string.results_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
