package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.BatterySnapshot
import com.example.data.BatteryViewModel
import com.example.data.ChargingSessionEntity
import com.example.data.TemperatureUnit
import com.example.data.Translations
import com.example.ui.components.ChartMetric
import com.example.ui.components.RealtimeCurvePlotter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun ChargingProcessScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.currentSnapshot.collectAsState()
    val rawHistory by viewModel.inMemoryHistory.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var activeMetric by remember { mutableStateOf(ChartMetric.POWER) }

    // Dynamic charging speed rating
    val speedText = when {
        snapshot.status != "充电中" -> Translations.getString("speed_discharging", appLanguage)
        abs(snapshot.powerW) >= 15.0 -> "${Translations.getString("speed_fast", appLanguage)} (PD/QC: ${snapshot.powerW}W)"
        abs(snapshot.powerW) >= 6.5 -> "${Translations.getString("speed_normal", appLanguage)} (${snapshot.powerW}W)"
        else -> "${Translations.getString("speed_slow", appLanguage)} (${snapshot.powerW}W)"
    }

    val speedColor = when {
        snapshot.status != "充电中" -> Color(0xFF64748B)       // slate gray
        abs(snapshot.powerW) >= 15.0 -> Color(0xFF3B82F6)    // Professional Royal Blue
        abs(snapshot.powerW) >= 6.5 -> Color(0xFF10B981)     // emerald green
        else -> Color(0xFFF97316)                            // alert orange
    }

    // Physics equations to calculate remaining time
    val timingEstimate = remember(snapshot, appLanguage) {
        val capMah = snapshot.designCapacityMah
        val lvl = snapshot.level
        val current = snapshot.currentMa

        if (snapshot.status == "充电中") {
            if (current <= 10) {
                Translations.getString("timing_calculating", appLanguage)
            } else {
                val remainingMah = (100 - lvl) * 0.01 * capMah
                val hours = remainingMah / current
                val minutesTotal = (hours * 60).toInt()
                if (minutesTotal <= 0) {
                    Translations.getString("timing_almost_full", appLanguage)
                } else if (minutesTotal > 600) {
                    Translations.getString("timing_slow", appLanguage)
                } else {
                    "${minutesTotal / 60}${Translations.getString("unit_hours", appLanguage)}${minutesTotal % 60}${Translations.getString("unit_minutes", appLanguage)}"
                }
            }
        } else if (snapshot.status == "放电中" && current < 0) {
            val remainingMah = lvl * 0.01 * capMah
            val hours = remainingMah / abs(current)
            val minutesTotal = (hours * 60).toInt()
            if (minutesTotal <= 0) {
                Translations.getString("timing_depleted", appLanguage)
            } else {
                "${minutesTotal / 60}${Translations.getString("unit_hours", appLanguage)}${minutesTotal % 60}${Translations.getString("unit_minutes", appLanguage)}"
            }
        } else if (snapshot.status == "已充满") {
            Translations.getString("val_full", appLanguage)
        } else {
            Translations.getString("timing_unplugged", appLanguage)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Stats Cards (Speed Indicator & Speedometer in top-to-bottom layout to ensure absolute zero crowding)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Status Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translations.getString("charging_eval_title", appLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(speedColor.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (snapshot.status == "充电中") {
                                    Translations.getString("charging_rate_rating", appLanguage)
                                } else {
                                    Translations.getString("charging_discharge_mode", appLanguage)
                                },
                                color = speedColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Perfect center-framed speedometer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.ui.components.ChargingSpeedometer(
                            powerW = snapshot.powerW,
                            isCharging = snapshot.status == "充电中",
                            language = appLanguage,
                            modifier = Modifier
                                .size(160.dp)
                        )
                    }

                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    // Decoupled stats block below the gauge speedometer with rich spaces and perfect contrast
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "实时评估状态",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = speedText,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = Translations.getString("charging_time_est", appLanguage),
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = timingEstimate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // 2. Real-time Graphic curves
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translations.getString("chart_title", appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricSelectorButton(
                            label = Translations.getString("chart_label_power", appLanguage),
                            isSelected = activeMetric == ChartMetric.POWER,
                            activeColor = Color(0xFF3B82F6)
                        ) { activeMetric = ChartMetric.POWER }

                        MetricSelectorButton(
                            label = Translations.getString("chart_label_current", appLanguage),
                            isSelected = activeMetric == ChartMetric.CURRENT,
                            activeColor = Color(0xFF10B981)
                        ) { activeMetric = ChartMetric.CURRENT }

                        MetricSelectorButton(
                            label = Translations.getString("chart_label_voltage", appLanguage),
                            isSelected = activeMetric == ChartMetric.VOLTAGE,
                            activeColor = Color(0xFF6366F1)
                        ) { activeMetric = ChartMetric.VOLTAGE }

                        MetricSelectorButton(
                            label = "${Translations.getString("chart_label_temp", appLanguage)} (${tempUnit.symbol})",
                            isSelected = activeMetric == ChartMetric.TEMPERATURE,
                            activeColor = Color(0xFFF97316)
                        ) { activeMetric = ChartMetric.TEMPERATURE }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Canvas Chart Render
                    RealtimeCurvePlotter(
                        history = rawHistory,
                        selectedMetric = activeMetric,
                        tempUnit = tempUnit,
                        language = appLanguage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }

        // 3. History section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Translations.getString("charging_cycles_record", appLanguage)} (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                if (sessions.isNotEmpty()) {
                    Text(
                        text = Translations.getString("clear_records", appLanguage),
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.clickable { viewModel.clearSessions() }
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(24.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty sessions",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Translations.getString("charging_empty_desc", appLanguage),
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sessions) { session ->
                SessionHistoryCard(session = session, language = appLanguage, tempUnit = tempUnit, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MetricSelectorButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) activeColor else Color(0xFF64748B),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SessionHistoryCard(
    session: ChargingSessionEntity,
    language: AppLanguage,
    tempUnit: TemperatureUnit,
    viewModel: BatteryViewModel
) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val startTimeStr = sdf.format(Date(session.startTime))
    val endTimeStr = sdf.format(Date(session.startTime + session.durationMs))
    val minutes = (session.durationMs / 60000).toInt()

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize() // Smooth slide expand animation!
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Charging item",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${Translations.getString("charging_charged_amount", language)} +${session.endLevel - session.startLevel}%",
                            color = Color(0xFF1B1B1F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$startTimeStr • " + String.format(Locale.getDefault(), Translations.getString("charging_duration_mins", language), minutes),
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${session.maxPower} W",
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${Translations.getString("charging_max_temp", language)} ${viewModel.formatTemperature(session.maxTemp.toDouble(), tempUnit)}",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand icon",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isExpanded) {
                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "充放阶段区间", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "${session.startLevel}% ➔ ${session.endLevel}%", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "起止物理时刻", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "$startTimeStr 至 ${endTimeStr.substringAfter(" ")}", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "最高安全温升", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "${viewModel.formatTemperature(session.maxTemp.toDouble(), tempUnit)}", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "电荷积分核算", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "容量设计 4500 mAh", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
