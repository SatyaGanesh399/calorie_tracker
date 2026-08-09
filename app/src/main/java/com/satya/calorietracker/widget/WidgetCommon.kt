package com.satya.calorietracker.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satya.calorietracker.MainActivity

/** Shared card chrome so all six widgets look like one family. */
@Composable
fun WidgetCard(
    style: WidgetStyle,
    onClick: Action = openApp(MainActivity.ACTION_HOME),
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(style.background)
                .cornerRadius(22.dp)
                .padding(14.dp)
                .clickable(onClick)
        ) {
            content()
        }
    }
}

@Composable
fun WidgetTitle(text: String, style: WidgetStyle) {
    Text(
        text = text,
        style = TextStyle(
            color = style.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 1
    )
}

@Composable
fun WidgetBigValue(text: String, style: WidgetStyle, size: Int = 24) {
    Text(
        text = text,
        style = TextStyle(
            color = style.onBackground,
            fontSize = size.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
}

@Composable
fun WidgetCaption(text: String, style: WidgetStyle) {
    Text(
        text = text,
        style = TextStyle(color = style.muted, fontSize = 11.sp),
        maxLines = 1
    )
}

@Composable
fun WidgetBar(
    progress: Float,
    color: ColorProvider,
    style: WidgetStyle,
    height: Int = 8
) {
    LinearProgressIndicator(
        progress = progress.coerceIn(0f, 1f),
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height.dp)
            .cornerRadius((height / 2).dp),
        color = color,
        backgroundColor = style.trackProvider
    )
}

@Composable
fun WidgetStatRow(
    label: String,
    value: String,
    progress: Float,
    color: ColorProvider,
    style: WidgetStyle
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(color = style.onBackground, fontSize = 12.sp),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            Text(
                text = value,
                style = TextStyle(
                    color = style.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        WidgetBar(progress = progress, color = color, style = style, height = 6)
    }
}

/**
 * Opens the app straight at a specific action, e.g. the add-food sheet.
 * Built from LocalContext so it keeps working with the debug applicationId suffix.
 */
@Composable
fun openApp(action: String): Action {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ACTION, action)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        // A unique action string stops Android reusing a stale PendingIntent.
        this.action = "com.satya.calorietracker.WIDGET_$action"
    }
    return actionStartActivity(intent)
}
