package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatterySnapshot
import com.example.data.AppLanguage
import com.example.data.TemperatureUnit
import com.example.data.Translations
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BatteryPowerRing(
    snapshot: BatterySnapshot,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    // 1. Double Wave phase infinite animations
    val infiniteTransition = rememberInfiniteTransition(label = "ring_waves")
    val wavePhase1 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val wavePhase2 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    // Bubble simulation progress
    val bubbleProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbles"
    )

    val levelSweep = animateFloatAsState(
        targetValue = snapshot.level * 3.6f, // 360 degrees
        animationSpec = tween(durationMillis = 800),
        label = "levelSweep"
    )

    // Status color selection
    val ringColor = when {
        snapshot.status == "充电中" -> Color(0xFF3B82F6)  // Energetic Blue
        snapshot.level <= 15 -> Color(0xFFEF4444)       // Critical low warning
        snapshot.level <= 35 -> Color(0xFFF97316)       // Mild low orange
        else -> Color(0xFF10B981)                        // Stable Green
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.width.coerceAtMost(size.height) / 2f) - 16.dp.toPx()
            val innerRingRadius = outerRadius - 12.dp.toPx()
            val waveBoundsRadius = innerRingRadius - 4.dp.toPx()

            // 1. Draw central background circle
            drawCircle(
                color = Color.White,
                radius = innerRingRadius,
                center = center
            )

            // 2. Draw outer gauge ring track (Base grey track)
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw level sweep accent arc of current charge
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor.copy(alpha = 0.3f), ringColor),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = levelSweep.value,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2)
            )

            // 4. Wave Fill clipping container (Clipping to a circle)
            clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, waveBoundsRadius)) }) {
                // Wave background tint
                drawCircle(
                    color = ringColor.copy(alpha = 0.05f),
                    radius = waveBoundsRadius,
                    center = center
                )

                // Level water height formula (bottom-up fill)
                val chargeRatio = snapshot.level / 100f
                val waterLevelY = center.y + waveBoundsRadius - (2 * waveBoundsRadius * chargeRatio)

                // SINE WAVE 1 DRAW
                val wavePath1 = Path().apply {
                    moveTo(center.x - waveBoundsRadius, center.y + waveBoundsRadius)
                    val step = 4f
                    var xVal = center.x - waveBoundsRadius
                    while (xVal <= center.x + waveBoundsRadius) {
                        val relativeX = xVal - (center.x - waveBoundsRadius)
                        val angle = (relativeX / (waveBoundsRadius * 2f)) * (2 * Math.PI.toFloat() * 1.3f) + wavePhase1.value
                        val waveHeight = 8.dp.toPx() * kotlin.math.sin(angle)
                        lineTo(xVal, waterLevelY + waveHeight)
                        xVal += step
                    }
                    lineTo(center.x + waveBoundsRadius, center.y + waveBoundsRadius)
                    close()
                }

                drawPath(
                    path = wavePath1,
                    brush = Brush.verticalGradient(
                        colors = listOf(ringColor.copy(alpha = 0.35f), ringColor.copy(alpha = 0.12f)),
                        startY = waterLevelY - 8.dp.toPx(),
                        endY = center.y + waveBoundsRadius
                    )
                )

                // SINE WAVE 2 DRAW (Fitted with phase 2, slightly offsetting)
                val wavePath2 = Path().apply {
                    moveTo(center.x - waveBoundsRadius, center.y + waveBoundsRadius)
                    val step = 4f
                    var xVal = center.x - waveBoundsRadius
                    while (xVal <= center.x + waveBoundsRadius) {
                        val relativeX = xVal - (center.x - waveBoundsRadius)
                        val angle = (relativeX / (waveBoundsRadius * 2f)) * (2 * Math.PI.toFloat() * 1.0f) + wavePhase2.value
                        val waveHeight = 5.dp.toPx() * kotlin.math.cos(angle)
                        lineTo(xVal, waterLevelY + waveHeight)
                        xVal += step
                    }
                    lineTo(center.x + waveBoundsRadius, center.y + waveBoundsRadius)
                    close()
                }

                drawPath(
                    path = wavePath2,
                    brush = Brush.verticalGradient(
                        colors = listOf(ringColor.copy(alpha = 0.22f), ringColor.copy(alpha = 0.04f)),
                        startY = waterLevelY - 5.dp.toPx(),
                        endY = center.y + waveBoundsRadius
                    )
                )

                // 5. Rising electric particle bubble generators
                if (snapshot.status == "充电中") {
                    val relativeOffsetXs = listOf(-35.dp.toPx(), 15.dp.toPx(), -10.dp.toPx(), 40.dp.toPx(), -5.dp.toPx())
                    val speeds = listOf(1.1f, 1.4f, 0.9f, 1.6f, 1.2f)
                    val sizes = listOf(3.5f.dp.toPx(), 4.5f.dp.toPx(), 2.5f.dp.toPx(), 3.8f.dp.toPx(), 3.0f.dp.toPx())

                    relativeOffsetXs.forEachIndexed { i, offX ->
                        val p = (bubbleProgress.value * speeds[i]) % 1.0f
                        val pStartY = center.y + waveBoundsRadius
                        val pEndY = waterLevelY.coerceAtLeast(center.y - waveBoundsRadius)
                        val pCurrentY = pStartY - (pStartY - pEndY) * p

                        drawCircle(
                            color = Color.White.copy(alpha = 0.65f * (1f - p)),
                            radius = sizes[i],
                            center = Offset(center.x + offX, pCurrentY)
                        )
                    }
                }
            }
        }

        // Center display numeric typography (perfectly spaced out, clean centerpiece layout)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${snapshot.level}%",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = Color(0xFF1B1B1F),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Clean, uncluttered single-line status rating badge
            val statusString = when (snapshot.status) {
                "充电中" -> Translations.getString("val_charging", language)
                "放电中" -> Translations.getString("val_discharging", language)
                "已充满" -> Translations.getString("val_full", language)
                "未充电" -> Translations.getString("val_not_charging", language)
                else -> snapshot.status
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ringColor.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ringColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SohGaugeChart(
    healthPercent: Int,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val animatedPercent = animateFloatAsState(
        targetValue = healthPercent.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "sohPercent"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height - 12.dp.toPx())
            // Safe bounding limit for the radius to prevent visual vertical and horizontal clipping!
            val radius = (size.height - 20.dp.toPx()).coerceAtMost(size.width / 2f - 24.dp.toPx())

            // 1. Draw semicircle background tracking (from 180 to 360 degrees)
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 2. Brush colors for visual deterioration (Excellent: green, Fair: yellow, Poor: red)
            val gaugeBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFEF4444), // Wear red
                    Color(0xFFF59E0B), // Attention orange/yellow
                    Color(0xFF10B981)  // Healed green
                ),
                startX = center.x - radius,
                endX = center.x + radius
            )

            // Draw active level sweep arc
            drawArc(
                brush = gaugeBrush,
                startAngle = 180f,
                sweepAngle = animatedPercent.value * 1.8f, // 100% = 180 degree semicircle
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 3. Draw needle vector indicator
            val needleAngleRad = Math.toRadians((180f + animatedPercent.value * 1.8f).toDouble())
            val needleLength = radius - 8.dp.toPx()
            val needleEndX = center.x + needleLength * kotlin.math.cos(needleAngleRad).toFloat()
            val needleEndY = center.y + needleLength * kotlin.math.sin(needleAngleRad).toFloat()

            // Needle lever line
            drawLine(
                color = Color(0xFF1E293B),
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle center base pin
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 2f.dp.toPx(),
                center = center
            )
        }

        // Inner status rating typography (reduced sizing to perfectly fit without crowding)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${healthPercent}%",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = Color(0xFF1B1B1F),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = Translations.getString("health_soh_title", language),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B), 
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun ChargingSpeedometer(
    powerW: Double,
    isCharging: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val absPower = abs(powerW).toFloat()
    val maxWattHz = 65f // Dashboard top-speed marker
    val animatedPower = animateFloatAsState(
        targetValue = absPower.coerceAtMost(maxWattHz),
        animationSpec = tween(durationMillis = 800),
        label = "powerIndicator"
    )

    // Semicircle layout starting from SW (140 deg) to SE (400 deg) -> 260 deg sweep
    val startAngle = 140f
    val sweepAngle = 260f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width.coerceAtMost(size.height) / 2f) - 16.dp.toPx()

            // 1. Draw dashboard base frame track
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 2. Draw Speed Gradient Line
            val powerRatio = animatedPower.value / maxWattHz
            val speedSweep = powerRatio * sweepAngle

            drawArc(
                brush = Brush.horizontalGradient(
                    colors = if (isCharging) {
                        listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6))
                    } else {
                        listOf(Color(0xFF64748B), Color(0xFF94A3B8))
                    },
                    startX = center.x - radius,
                    endX = center.x + radius
                ),
                startAngle = startAngle,
                sweepAngle = speedSweep,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 3. Draw tick indicators
            val ticks = 11
            for (i in 0 until ticks) {
                val tickFraction = i / (ticks - 1).toFloat()
                val angleDeg = startAngle + (sweepAngle * tickFraction)
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val tickInnerLen = radius - 12.dp.toPx()
                val tickOuterLen = radius - 8.dp.toPx()

                val tickStartX = center.x + tickInnerLen * kotlin.math.cos(angleRad).toFloat()
                val tickStartY = center.y + tickInnerLen * kotlin.math.sin(angleRad).toFloat()
                val tickEndX = center.x + tickOuterLen * kotlin.math.cos(angleRad).toFloat()
                val tickEndY = center.y + tickOuterLen * kotlin.math.sin(angleRad).toFloat()

                drawLine(
                    color = Color(0xFF64748B).copy(alpha = 0.4f),
                    start = Offset(tickStartX, tickStartY),
                    end = Offset(tickEndX, tickEndY),
                    strokeWidth = 1.5f.dp.toPx()
                )
            }

            // 4. Center hub and Needle lever drawing
            val needleAngleDeg = startAngle + speedSweep
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = radius - 10.dp.toPx()
            val needleEndX = center.x + needleLength * kotlin.math.cos(needleAngleRad).toFloat()
            val needleEndY = center.y + needleLength * kotlin.math.sin(needleAngleRad).toFloat()

            drawLine(
                color = if (isCharging) Color(0xFF3B82F6) else Color(0xFF64748B),
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 2.5f.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(
                color = if (isCharging) Color(0xFF3B82F6) else Color(0xFF64748B),
                radius = 5.dp.toPx(),
                center = center
            )
        }

        // Speed text inside (repositioned and sized down to look perfectly spacious and clean!)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 34.dp)
        ) {
            Text(
                text = if (isCharging) "+${String.format(Locale.US, "%.1f", absPower)} W" else "${String.format(Locale.US, "%.1f", absPower)} W",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (isCharging) Color(0xFF3B82F6) else Color(0xFF1B1B1F)
            )
            Text(
                text = if (isCharging) {
                    if (absPower >= 15f) Translations.getString("speed_fast", language)
                    else if (absPower >= 6.5f) Translations.getString("speed_normal", language)
                    else Translations.getString("speed_slow", language)
                } else {
                    Translations.getString("speed_discharging", language)
                },
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

enum class ChartMetric {
    POWER,
    CURRENT,
    VOLTAGE,
    TEMPERATURE
}

@Composable
fun RealtimeCurvePlotter(
    history: List<BatterySnapshot>,
    selectedMetric: ChartMetric,
    tempUnit: TemperatureUnit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var widthPx by remember { mutableStateOf(0f) }
    var activeIndex by remember { mutableStateOf<Int?>(null) }

    if (history.isEmpty()) {
        Box(
            modifier = modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Translations.getString("chart_no_data", language),
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }
        return
    }

    // Capture min/max limits
    var maxVal = Float.MIN_VALUE
    var minVal = Float.MAX_VALUE

    history.forEach { snapshot ->
        val v = when (selectedMetric) {
            ChartMetric.POWER -> abs(snapshot.powerW).toFloat()
            ChartMetric.CURRENT -> snapshot.currentMa.toFloat()
            ChartMetric.VOLTAGE -> snapshot.voltageMv.toFloat()
            ChartMetric.TEMPERATURE -> {
                when (tempUnit) {
                    TemperatureUnit.CELSIUS -> snapshot.temperatureC
                    TemperatureUnit.FAHRENHEIT -> snapshot.temperatureC * 1.8 + 32.0
                    TemperatureUnit.KELVIN -> snapshot.temperatureC + 273.15
                }.toFloat()
            }
        }
        if (v > maxVal) maxVal = v
        if (v < minVal) minVal = v
    }

    if (maxVal == minVal) {
        maxVal += 1f
        minVal -= 1f
    } else {
        val pad = (maxVal - minVal) * 0.15f
        maxVal += pad
        minVal -= pad
    }

    val chartColor = when (selectedMetric) {
        ChartMetric.POWER -> Color(0xFF3B82F6)      // Charge blue
        ChartMetric.CURRENT -> Color(0xFF10B981)    // Emerald green
        ChartMetric.VOLTAGE -> Color(0xFF6366F1)    // Indigo voltage
        ChartMetric.TEMPERATURE -> Color(0xFFF97316)// Alert orange
    }

    val labelUnit = when (selectedMetric) {
        ChartMetric.POWER -> "W"
        ChartMetric.CURRENT -> "mA"
        ChartMetric.VOLTAGE -> "mV"
        ChartMetric.TEMPERATURE -> tempUnit.symbol
    }

    val labelStyle = MaterialTheme.typography.bodySmall.copy(
        color = Color(0xFF64748B),
        fontSize = 10.sp
    )

    val tooltipTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { size -> widthPx = size.width.toFloat() }
                .pointerInput(history.size) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (anyPressed) {
                                val position = event.changes.first().position
                                val leftMargin = 120f
                                val chartWidth = widthPx - leftMargin - 40f
                                if (chartWidth > 0 && history.size > 1) {
                                    val idx = ((position.x - leftMargin) / chartWidth * (history.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, history.size - 1)
                                    activeIndex = idx
                                }
                            } else {
                                activeIndex = null
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val leftMargin = 120f
            val bottomMargin = 60f
            val chartWidth = canvasWidth - leftMargin - 40f
            val chartHeight = canvasHeight - bottomMargin - 20f

            // 1. Subtle horizontal references (Grid counting)
            val gridCount = 4
            for (i in 0..gridCount) {
                val gridY = 20f + (chartHeight * i / gridCount)
                val gridVal = maxVal - ((maxVal - minVal) * i / gridCount)

                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(leftMargin, gridY),
                    end = Offset(canvasWidth - 40f, gridY),
                    strokeWidth = 1f
                )

                val formattedVal = String.format(Locale.getDefault(), "%.1f", gridVal)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "$formattedVal $labelUnit",
                    topLeft = Offset(10f, gridY - 18f),
                    style = labelStyle
                )
            }

            // 2. Map coordinates points
            val points = mutableListOf<Offset>()
            val segmentWidth = if (history.size > 1) chartWidth / (history.size - 1) else chartWidth

            history.forEachIndexed { index, snapshot ->
                val v = when (selectedMetric) {
                    ChartMetric.POWER -> abs(snapshot.powerW).toFloat()
                    ChartMetric.CURRENT -> snapshot.currentMa.toFloat()
                    ChartMetric.VOLTAGE -> snapshot.voltageMv.toFloat()
                    ChartMetric.TEMPERATURE -> {
                        when (tempUnit) {
                            TemperatureUnit.CELSIUS -> snapshot.temperatureC
                            TemperatureUnit.FAHRENHEIT -> snapshot.temperatureC * 1.8 + 32.0
                            TemperatureUnit.KELVIN -> snapshot.temperatureC + 273.15
                        }.toFloat()
                    }
                }

                val x = leftMargin + (index * segmentWidth)
                val fraction = (v - minVal) / (maxVal - minVal)
                val y = 20f + chartHeight * (1f - fraction)
                points.add(Offset(x, y))
            }

            // 3. Draw gorgeous smooth Bezier curve line
            if (points.isNotEmpty()) {
                val linePath = Path()
                linePath.moveTo(points.first().x, points.first().y)

                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val controlX1 = prev.x + (curr.x - prev.x) / 2f
                    val controlY1 = prev.y
                    val controlX2 = prev.x + (curr.x - prev.x) / 2f
                    val controlY2 = curr.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                }

                // Copy for gradient filling below curve line
                val filledPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, 20f + chartHeight)
                    lineTo(points.first().x, 20f + chartHeight)
                    close()
                }

                drawPath(
                    path = filledPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(chartColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = points.first().y.coerceAtMost(20f),
                        endY = 20f + chartHeight
                    )
                )

                drawPath(
                    path = linePath,
                    color = chartColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight the very endpoint
                val finalNode = points.last()
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = finalNode)
                drawCircle(color = chartColor, radius = 3.dp.toPx(), center = finalNode)

                // 4. Dynamic Crosshair Rendering
                activeIndex?.let { activeIdx ->
                    if (activeIdx in points.indices) {
                        val touchPoint = points[activeIdx]
                        val snapshot = history[activeIdx]

                        // Draw dashed physical vertical crosshair line using explicit interval dashes
                        val dashHeight = 12f
                        var dashStartY = 20f
                        while (dashStartY < 20f + chartHeight) {
                            val nextY = (dashStartY + dashHeight).coerceAtMost(20f + chartHeight)
                            drawLine(
                                color = Color(0xFF94A3B8),
                                start = Offset(touchPoint.x, dashStartY),
                                end = Offset(touchPoint.x, nextY),
                                strokeWidth = 1.5f.dp.toPx()
                            )
                            dashStartY += dashHeight * 2f
                        }

                        // Outer glowing touch anchor ring
                        drawCircle(color = chartColor, radius = 6.dp.toPx(), center = touchPoint)
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = touchPoint)

                        // 5. Draw real-time popover tooltip overlay box
                        val exactVal = when (selectedMetric) {
                            ChartMetric.POWER -> abs(snapshot.powerW)
                            ChartMetric.CURRENT -> snapshot.currentMa.toDouble()
                            ChartMetric.VOLTAGE -> snapshot.voltageMv.toDouble()
                            ChartMetric.TEMPERATURE -> {
                                when (tempUnit) {
                                    TemperatureUnit.CELSIUS -> snapshot.temperatureC
                                    TemperatureUnit.FAHRENHEIT -> snapshot.temperatureC * 1.8 + 32.0
                                    TemperatureUnit.KELVIN -> snapshot.temperatureC + 273.15
                                }
                            }
                        }

                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val timeString = sdf.format(Date(snapshot.timestamp))
                        val textValue = String.format(Locale.getDefault(), "%.1f %s (%s)", exactVal, labelUnit, timeString)

                        val textLayoutResult = textMeasurer.measure(textValue, tooltipTextStyle)
                        val bubbleW = textLayoutResult.size.width + 24f
                        val bubbleH = textLayoutResult.size.height + 16f

                        // Place box intelligently on screen horizontally to avoid overflow boundaries
                        val bubbleX = (touchPoint.x - bubbleW / 2f).coerceIn(leftMargin, canvasWidth - bubbleW - 10f)
                        val bubbleY = (touchPoint.y - bubbleH - 12f).coerceAtLeast(10f)

                        // Draw Popover Container with rounded corners
                        drawRoundRect(
                            color = Color(0xFF1E293B).copy(alpha = 0.92f),
                            topLeft = Offset(bubbleX, bubbleY),
                            size = Size(bubbleW, bubbleH),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Draw Popup label
                        drawText(
                            textMeasurer = textMeasurer,
                            text = textValue,
                            topLeft = Offset(bubbleX + 12f, bubbleY + 8f),
                            style = tooltipTextStyle
                        )
                    }
                }
            }
        }
    }
}
