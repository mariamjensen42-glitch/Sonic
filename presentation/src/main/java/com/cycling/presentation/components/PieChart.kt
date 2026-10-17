package com.cycling.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2

data class PieChartData(
    val value: Float,
    val color: Color,
    val label: String
)

/**
 * 默认饼图调色板。全部取自主题色彩角色，随明暗主题自适应；
 * 调用方按需取色，不再硬编码一组十六进制色。
 */
@Composable
fun defaultPieChartColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    onSliceClick: ((Int) -> Unit)? = null
) {
    var internalSelectedIndex by remember { mutableStateOf(-1) }
    val effectiveSelectedIndex = if (selectedIndex >= 0) selectedIndex else internalSelectedIndex
    
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawCircle(
                    color = trackColor,
                    radius = size.minDimension / 2 - 20.dp.toPx(),
                    style = Stroke(width = 40.dp.toPx())
                )
            }
        }
        return
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        if (onSliceClick != null) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            val radius = minOf(size.width, size.height) / 2 - 20.dp.toPx()
                            val innerRadius = radius - 40.dp.toPx()
                            
                            if (distance in innerRadius..radius) {
                                var angle = atan2(dy, dx)
                                if (angle < 0) angle += 2 * kotlin.math.PI.toFloat()
                                
                                var startAngle = -kotlin.math.PI.toFloat() / 2
                                var clickedIndex = -1
                                
                                for ((index, item) in data.withIndex()) {
                                    val sweepAngle = (item.value / total) * 2 * kotlin.math.PI.toFloat()
                                    var normalizedStart = startAngle
                                    if (normalizedStart < 0) normalizedStart += 2 * kotlin.math.PI.toFloat()
                                    
                                    val endAngle = startAngle + sweepAngle
                                    var normalizedEnd = endAngle
                                    if (normalizedEnd < 0) normalizedEnd += 2 * kotlin.math.PI.toFloat()
                                    
                                    if (isAngleInRange(angle, normalizedStart, normalizedEnd)) {
                                        clickedIndex = index
                                        break
                                    }
                                    startAngle = endAngle
                                }
                                
                                if (clickedIndex >= 0) {
                                    internalSelectedIndex = clickedIndex
                                    onSliceClick(clickedIndex)
                                }
                            }
                        }
                    }
                }
        ) {
            val radius = size.minDimension / 2 - 20.dp.toPx()
            val strokeWidth = 40.dp.toPx()
            val innerRadius = radius - strokeWidth
            
            var startAngle = -90f
            
            data.forEachIndexed { index, item ->
                val sweepAngle = (item.value / total) * 360f
                val isSelected = index == effectiveSelectedIndex
                val expandRadius = if (isSelected) 8.dp.toPx() else 0f
                
                val color = if (isSelected) {
                    item.color
                } else {
                    item.color.copy(alpha = 0.8f)
                }
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = Size(
                        width = (radius + expandRadius) * 2,
                        height = (radius + expandRadius) * 2
                    ),
                    topLeft = Offset(
                        x = center.x - radius - expandRadius,
                        y = center.y - radius - expandRadius
                    )
                )
                
                startAngle += sweepAngle
            }
        }
    }
}

private fun isAngleInRange(angle: Float, start: Float, end: Float): Boolean {
    var normalizedAngle = angle
    var normalizedStart = start
    var normalizedEnd = end
    
    if (normalizedStart < 0) normalizedStart += 2 * kotlin.math.PI.toFloat()
    if (normalizedEnd < 0) normalizedEnd += 2 * kotlin.math.PI.toFloat()
    if (normalizedAngle < 0) normalizedAngle += 2 * kotlin.math.PI.toFloat()
    
    return if (normalizedStart <= normalizedEnd) {
        normalizedAngle in normalizedStart..normalizedEnd
    } else {
        normalizedAngle >= normalizedStart || normalizedAngle <= normalizedEnd
    }
}
