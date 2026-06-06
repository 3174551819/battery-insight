package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.data.BatterySnapshot
import com.example.data.BatteryViewModel
import com.example.data.Translations
import com.example.data.TemperatureUnit
import com.example.ui.components.BatteryPowerRing

@Composable
fun BatteryDashboardScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.currentSnapshot.collectAsState()
    val isDemo by viewModel.isDemoMode.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val alerts by viewModel.activeAlerts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Warning Banner for anomalies
        AnimatedVisibility(
            visible = alerts.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert icon",
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${Translations.getString("dashboard_alerts_title", appLanguage)} (${alerts.size})",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                alerts.forEach { alert ->
                    Text(
                        text = "• $alert",
                        color = Color(0xFF1B1B1F),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }

        // Demo interactive panel when simulation is active
        if (isDemo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF3B82F6).copy(alpha = 0.06f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hardware,
                            contentDescription = "Demo icon",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Translations.getString("dashboard_demo_notice_title", appLanguage),
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Translations.getString("dashboard_demo_notice_desc", appLanguage),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))

                val isCharging = snapshot.status == "充电中"
                Button(
                    onClick = { viewModel.triggerSimulatedPlug(!isCharging) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCharging) Color(0xFFEF4444) else Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp) // Optimized touch target
                ) {
                    Text(
                        text = if (isCharging) Translations.getString("dashboard_btn_disconnect", appLanguage) else Translations.getString("dashboard_btn_connect", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 1. Dashboard Ring Dial
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            contentAlignment = Alignment.Center
        ) {
            BatteryPowerRing(
                snapshot = snapshot,
                language = appLanguage,
                modifier = Modifier
                    .size(240.dp)
                    .aspectRatio(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Decoupled Core Stats Row - Fully spacious and elegant, eliminating internal ring crowding!
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expanded Power Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (snapshot.status == "充电中") {
                            Translations.getString("gauge_charging_rate", appLanguage)
                        } else {
                            Translations.getString("gauge_discharging_rate", appLanguage)
                        },
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (snapshot.status == "充电中") "+${snapshot.powerW} W" else "${snapshot.powerW} W",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (snapshot.status == "充电中") Color(0xFF3B82F6) else Color(0xFF1B1B1F)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Current Flow Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "物理电流",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${snapshot.currentMa} mA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Voltage Core Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "硬件电压",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.3f V", snapshot.voltageMv / 1000.0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Metrics grid list
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Translations.getString("dashboard_hardware_metrics_title", appLanguage),
                fontSize = 15.sp,
                color = Color(0xFF1B1B1F),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                MetricGridCard(
                    title = Translations.getString("metric_temperature", appLanguage),
                    value = viewModel.formatTemperature(snapshot.temperatureC, tempUnit),
                    icon = Icons.Default.Thermostat,
                    iconColor = when {
                        snapshot.temperatureC > 40.0 -> Color(0xFFEF4444)
                        snapshot.temperatureC > 35.0 -> Color(0xFFF97316)
                        else -> Color(0xFF10B981)
                    }
                )
            }

            item {
                MetricGridCard(
                    title = Translations.getString("metric_voltage", appLanguage),
                    value = "${snapshot.voltageMv} mV",
                    icon = Icons.Default.FlashOn,
                    iconColor = Color(0xFF6366F1)
                )
            }

            item {
                MetricGridCard(
                    title = Translations.getString("metric_health", appLanguage),
                    value = Translations.translateValue(snapshot.health, appLanguage),
                    icon = Icons.Default.Favorite,
                    iconColor = if (snapshot.health == "良好") Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            item {
                MetricGridCard(
                    title = Translations.getString("metric_plug", appLanguage),
                    value = Translations.translateValue(snapshot.plugType, appLanguage),
                    icon = Icons.Default.Power,
                    iconColor = Color(0xFFF59E0B)
                )
            }

            item {
                MetricGridCard(
                    title = Translations.getString("metric_tech", appLanguage),
                    value = snapshot.technology,
                    icon = Icons.Default.Hub,
                    iconColor = Color(0xFF0EA5E9)
                )
            }

            item {
                MetricGridCard(
                    title = Translations.getString("metric_source", appLanguage),
                    value = Translations.translateValue(snapshot.apiSource, appLanguage),
                    icon = Icons.Default.Source,
                    iconColor = Color(0xFF8B5CF6)
                )
            }
        }
    }
}

@Composable
fun MetricGridCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1B1B1F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
