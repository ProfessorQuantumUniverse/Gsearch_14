package com.olafsapp.gsearch14.core

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Opens result URLs outside the in-app WebView. */
object UrlLauncher {

    /**
     * Opens [url] in a Chrome Custom Tab, tinted to match the app.
     *
     * Falls back to a plain view intent when no browser supports custom tabs.
     */
    fun openCustomTab(context: Context, url: String, toolbarColor: Int) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setDefaultColorSchemeParams(
                androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColor)
                    .build(),
            )
            .build()
        try {
            intent.launchUrl(context, url.toUri())
        } catch (_: RuntimeException) {
            // ActivityNotFoundException, or a SecurityException from a misbehaving browser.
            openExternal(context, url)
        }
    }

    /** Hands [url] to the user's default browser. Returns false if nothing can open it. */
    fun openExternal(context: Context, url: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: RuntimeException) {
        false
    }

    /** Opens the system share sheet for [url]. */
    fun share(context: Context, url: String, subject: String?) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        runCatching { context.startActivity(Intent.createChooser(send, null)) }
    }

    /** Best-effort readable label for a URL: the host without a leading `www.`. */
    fun hostLabel(url: String): String = runCatching {
        url.toUri().host?.removePrefix("www.").orEmpty()
    }.getOrDefault("")
}
