package com.olafsapp.gsearch14.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.olafsapp.gsearch14.BuildConfig
import com.olafsapp.gsearch14.MainActivity
import com.olafsapp.gsearch14.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Home screen widget, rewritten with Glance.
 *
 * Replaces the old RemoteViews layout: Glance picks up Material You colours on Android 12+
 * for free, and the widget now states plainly what tapping it does.
 */
class SearchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    /**
     * The picker preview on Android 15+. Rendering the real composable means the preview
     * can never drift from the widget the user actually gets.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        // A plain intent rather than action parameters: the flag has to survive as a real
        // extra so MainActivity can open its quick-search layout with the keyboard up.
        val openSearch: Action = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_QUICK_SEARCH)
                .putExtra(MainActivity.EXTRA_FROM_WIDGET, true),
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(28.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(openSearch),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Brand mark
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "G",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Box(modifier = GlanceModifier.width(12.dp)) {}

                Text(
                    text = context.getString(R.string.widget_hint),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

class SearchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SearchWidget()
}

/**
 * Publishes the generated widget preview to the launcher's widget picker (Android 15+).
 *
 * Older releases fall back to `previewLayout` / `previewImage` in `search_widget_info.xml`.
 * The platform rate-limits this call, so it runs once per app version and only counts as
 * done after the system accepted it.
 */
object SearchWidgetPreviews {

    private const val PREFS = "widget_prefs"
    private const val KEY_PUBLISHED_VERSION = "preview_published_version"

    fun publishIfNeeded(context: Context, scope: CoroutineScope) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val appContext = context.applicationContext
        scope.launch {
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getInt(KEY_PUBLISHED_VERSION, 0) == BuildConfig.VERSION_CODE) return@launch

            val result = runCatching {
                GlanceAppWidgetManager(appContext).setWidgetPreviews(SearchWidgetReceiver::class)
            }.onFailure { Log.w("Gsearch", "Publishing widget preview failed", it) }
                .getOrNull()

            if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                prefs.edit { putInt(KEY_PUBLISHED_VERSION, BuildConfig.VERSION_CODE) }
            }
        }
    }
}
