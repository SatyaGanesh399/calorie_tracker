package com.satya.calorietracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class ChartPoint(val label: String, val value: Float)

/**
 * Smooth line chart drawn on a Canvas — no charting dependency, no XML interop.
 * Handles the awkward cases a weight chart actually hits: one point, all-identical
 * values, and very small ranges (where a naive auto-scale makes noise look dramatic).
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fill: Boolean = true,
    goalValue: Float? = null,
    minPadding: Float = 0.5f,
    valueFormatter: (Float) -> String = { it.toInt().toString() }
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.tertiary
    val animated by animateFloatAsState(
        targetValue = if (points.isEmpty()) 0f else 1f,
        animationSpec = tween(700),
        label = "line"
    )

    if (points.isEmpty()) {
        ChartEmpty(modifier.height(height), "Not enough data yet")
        return
    }

    val values = points.map { it.value }
    val rawMin = values.min()
    val rawMax = values.max()
    // Never let a flat or nearly-flat series fill the whole chart height.
    val span = (rawMax - rawMin).coerceAtLeast(minPadding * 2)
    val minY = rawMin - (span - (rawMax - rawMin)) / 2f
    val maxY = minY + span

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                valueFormatter(rawMax),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valueFormatter(rawMin),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 10f
            val padBottom = 10f
            val usable = h - padTop - padBottom

            fun yFor(v: Float): Float = padTop + usable * (1f - ((v - minY) / (maxY - minY)))
            fun xFor(i: Int): Float =
                if (points.size == 1) w / 2f else w * (i.toFloat() / (points.size - 1))

            // Horizontal guides
            repeat(4) { i ->
                val y = padTop + usable * i / 3f
                drawLine(
                    color = outline.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            goalValue?.let { g ->
                if (g in minY..maxY) {
                    drawLine(
                        color = goalColor,
                        start = Offset(0f, yFor(g)),
                        end = Offset(w, yFor(g)),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                }
            }

            val coords = points.mapIndexed { i, p -> Offset(xFor(i), yFor(p.value)) }
            val visible = (coords.size * animated).toInt().coerceAtLeast(if (coords.isEmpty()) 0 else 1)
            val shown = coords.take(visible)
            if (shown.isEmpty()) return@Canvas

            val path = Path().apply {
                moveTo(shown.first().x, shown.first().y)
                for (i in 1 until shown.size) {
                    val prev = shown[i - 1]
                    val cur = shown[i]
                    val midX = (prev.x + cur.x) / 2f
                    cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
            }

            if (fill && shown.size > 1) {
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(shown.last().x, h)
                    lineTo(shown.first().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f))
                    )
                )
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Emphasise the endpoints; interior dots only when the series is short.
            shown.forEachIndexed { i, o ->
                val isEnd = i == 0 || i == shown.lastIndex
                if (isEnd || shown.size <= 14) {
                    drawCircle(color = lineColor, radius = if (isEnd) 6f else 4f, center = o)
                    if (isEnd) drawCircle(color = Color.White, radius = 2.5f, center = o)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                points.first().label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (points.size > 1) {
                Text(
                    points.last().label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Vertical bars, used for calories per day. Bars past the target turn amber. */
@Composable
fun BarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    overColor: Color = MaterialTheme.colorScheme.error,
    target: Float? = null,
    showLabels: Boolean = true
) {
    if (points.isEmpty()) {
        ChartEmpty(modifier.height(height), "Nothing logged in this range")
        return
    }

    val outline = MaterialTheme.colorScheme.outlineVariant
    val maxValue = maxOf(points.maxOf { it.value }, target ?: 0f, 1f)
    val animated by animateFloatAsState(targetValue = 1f, animationSpec = tween(600), label = "bars")

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val w = size.width
            val h = size.height
            val slot = w / points.size
            val barWidth = (slot * 0.55f).coerceAtMost(34f)

            target?.let { t ->
                val y = h * (1f - t / maxValue)
                drawLine(
                    color = outline,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            points.forEachIndexed { i, p ->
                val barHeight = h * (p.value / maxValue) * animated
                val left = i * slot + (slot - barWidth) / 2f
                val color = if (target != null && p.value > target * 1.1f) overColor else barColor
                drawRoundRect(
                    color = if (p.value <= 0f) outline.copy(alpha = 0.4f) else color,
                    topLeft = Offset(left, h - barHeight.coerceAtLeast(2f)),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f)
                )
            }
        }

        if (showLabels) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Only show a handful of labels so they never collide.
                val step = (points.size / 6).coerceAtLeast(1)
                points.filterIndexed { i, _ -> i % step == 0 }.forEach {
                    Text(
                        it.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Horizontal stacked bar showing the protein / carbs / fat energy split. */
@Composable
fun MacroSplitBar(
    proteinPct: Int,
    carbsPct: Int,
    fatPct: Int,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    proteinColor: Color,
    carbsColor: Color,
    fatColor: Color
) {
    val total = (proteinPct + carbsPct + fatPct).coerceAtLeast(1)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        listOf(
            proteinPct to proteinColor,
            carbsPct to carbsColor,
            fatPct to fatColor
        ).forEach { (pct, color) ->
            if (pct > 0) {
                Box(
                    Modifier
                        .weight(pct.toFloat() / total)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun ChartEmpty(modifier: Modifier, message: String) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Difference indicator: "↓ 0.6 kg", coloured by whether it's moving toward the goal. */
@Composable
fun DeltaText(
    delta: Double,
    unit: String,
    goodWhenNegative: Boolean = true,
    modifier: Modifier = Modifier
) {
    val neutral = abs(delta) < 0.05
    val good = if (goodWhenNegative) delta < 0 else delta > 0
    val color = when {
        neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        good -> com.satya.calorietracker.ui.theme.GoalGood
        else -> com.satya.calorietracker.ui.theme.GoalOver
    }
    val arrow = when {
        neutral -> "—"
        delta < 0 -> "↓"
        else -> "↑"
    }
    Text(
        text = if (neutral) "No change" else "$arrow ${com.satya.calorietracker.util.Format.decimal(abs(delta))} $unit",
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier
    )
}
