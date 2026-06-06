package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatteryViewModel
import com.example.data.Translations

@Composable
fun BatteryHealthScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.currentSnapshot.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val scrollState = rememberScrollState()

    val animatedProgress by animateFloatAsState(
        targetValue = snapshot.healthPercent / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "healthProgress"
    )

    // Grade cycles
    val cycleRating = when {
        snapshot.cycleCount < 150 -> Translations.getString("cycle_rating_new", appLanguage)
        snapshot.cycleCount < 450 -> Translations.getString("cycle_rating_mid", appLanguage)
        else -> Translations.getString("cycle_rating_high", appLanguage)
    }
    val cycleColor = when {
        snapshot.cycleCount < 150 -> Color(0xFF10B981)
        snapshot.cycleCount < 450 -> Color(0xFF3B82F6)
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Text(
            text = Translations.getString("health_report_header", appLanguage),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B1B1F)
        )

        // 1. Health Index percentage card
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Translations.getString("health_report_header", appLanguage),
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful custom semi-circular Health Gauge Dashboard!
                com.example.ui.components.SohGaugeChart(
                    healthPercent = snapshot.healthPercent,
                    language = appLanguage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = Translations.getString("health_soh_desc", appLanguage),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }
        }

        // 2. Capacity Margin Cards (Design vs Actual)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = Translations.getString("health_design_cap", appLanguage),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${snapshot.designCapacityMah} mAh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = Translations.getString("health_actual_cap", appLanguage),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${snapshot.currentCapacityMah} mAh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3. Cycle and wear stats
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Translations.getString("health_cycles_title", appLanguage),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${snapshot.cycleCount} ${Translations.getString("health_unit_times", appLanguage)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(cycleColor.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cycleRating,
                            color = cycleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Translations.getString("health_wear_desc", appLanguage),
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }
        }

        // 4. Lock capacity diagnostic panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock capacity diagnostic icon",
                        tint = if (snapshot.isCapLocked) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.getString("health_lock_title", appLanguage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Translations.getString("health_lock_label", appLanguage), fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(
                        text = if (snapshot.isCapLocked) {
                            Translations.getString("health_lock_warn", appLanguage)
                        } else {
                            Translations.getString("health_lock_safe", appLanguage)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (snapshot.isCapLocked) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Translations.getString("health_lock_desc", appLanguage),
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
