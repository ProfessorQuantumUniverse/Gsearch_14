package com.olafsapp.gsearch14.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import com.olafsapp.gsearch14.MainActivity
import com.olafsapp.gsearch14.R

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

    @Composable
    private fun WidgetContent(context: Context) {
        // A plain intent rather than action parameters: the flag has to survive as a real
        // extra so MainActivity can focus the input and pop the keyboard.
        val openSearch = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
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
