package com.satya.calorietracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * The big calorie ring on the dashboard. Sweeps clockwise from 12 o'clock, animates
 * whenever the value changes, and overflows into a second, darker arc when you go
 * past your target instead of silently clamping.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    strokeWidth: Dp = 16.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    overflowColor: Color = MaterialTheme.colorScheme.error,
    content: @Composable () -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 2f),
        animationSpec = tween(durationMillis = 700),
        label = "ring"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val first = animated.coerceAtMost(1f)
            if (first > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(progressColor, progressColor.copy(alpha = 0.75f), progressColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * first,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            val over = (animated - 1f).coerceAtLeast(0f)
            if (over > 0f) {
                drawArc(
                    color = overflowColor,
                    startAngle = -90f,
                    sweepAngle = 360f * over.coerceAtMost(1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 0.6f, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Small ring used in the macro cards and the widget-preview tiles. */
@Composable
fun MiniRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    strokeWidth: Dp = 6.dp,
    label: String? = null
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "miniRing"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = color.copy(alpha = 0.18f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Rounded, animated linear bar used for macros and water. */
@Composable
fun StatBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackAlpha: Float = 0.18f
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color.copy(alpha = trackAlpha))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/** "Protein  82 / 120 g" with its bar. The workhorse row of the whole app. */
@Composable
fun MacroRow(
    label: String,
    consumed: Double,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target <= 0) 0f else (consumed / target).coerceIn(0.0, 1.0).toFloat()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${com.satya.calorietracker.util.Format.grams(consumed)} / $target $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        StatBar(progress = progress, color = color)
    }
}

/** A compact labelled figure, e.g. "1,420" over "Consumed". */
@Composable
fun StatValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, height: Dp = 32.dp) {
    Box(
        modifier
            .width(1.dp)
            .height(height)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
