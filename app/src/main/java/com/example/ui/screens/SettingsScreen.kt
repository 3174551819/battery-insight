package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatteryViewModel
import com.example.data.Translations
import com.example.data.AppLanguage
import com.example.data.TemperatureUnit

@Composable
fun SettingsScreen(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val snapshot by viewModel.currentSnapshot.collectAsState()
    val refreshMs by viewModel.refreshIntervalMs.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()
    val isDemo by viewModel.isDemoMode.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Settings Banner
        Text(
            text = Translations.getString("settings_panel_title", currentLang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B1B1F)
        )

        // 0. Language Selector Card (Supporting 8 languages gracefully)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language settings icon",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Translations.getString("settings_language_title", currentLang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Translations.getString("settings_language_desc", currentLang),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LanguageSelectionGrid(
                    selectedLang = currentLang,
                    onLangSelected = { viewModel.setLanguage(it) }
                )
            }
        }

        // 1. General Config Card (Temp Scale unit selector & Simulation Config)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translations.getString("settings_general_config", currentLang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Temperature Scale Selector with Celsius, Fahrenheit, and Kelvin options
                Text(
                    text = Translations.getString("settings_temp_unit", currentLang),
                    fontSize = 14.sp,
                    color = Color(0xFF1B1B1F),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Translations.getString("settings_temp_unit_desc", currentLang),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TemperatureUnitBadge(
                        label = "Celsius (℃)",
                        isSelected = tempUnit == TemperatureUnit.CELSIUS
                    ) {
                        viewModel.setTemperatureUnit(TemperatureUnit.CELSIUS)
                    }
                    TemperatureUnitBadge(
                        label = "Fahrenheit (℉)",
                        isSelected = tempUnit == TemperatureUnit.FAHRENHEIT
                    ) {
                        viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
                    }
                    TemperatureUnitBadge(
                        label = "Kelvin (K)",
                        isSelected = tempUnit == TemperatureUnit.KELVIN
                    ) {
                        viewModel.setTemperatureUnit(TemperatureUnit.KELVIN)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Demo Simulation mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Translations.getString("settings_demo_mode", currentLang),
                            fontSize = 14.sp,
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = Translations.getString("settings_demo_mode_desc", currentLang),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = isDemo,
                        onCheckedChange = { viewModel.toggleDemoMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF3B82F6),
                            checkedTrackColor = Color(0xFF93C5FD),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                // Poll interval selection
                Text(
                    text = Translations.getString("settings_refresh_rate", currentLang),
                    fontSize = 14.sp,
                    color = Color(0xFF1B1B1F),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RefreshRateBadge(
                        label = Translations.getString("refresh_1s", currentLang),
                        isSelected = refreshMs == 1000L
                    ) {
                        viewModel.setRefreshInterval(1000L)
                    }
                    RefreshRateBadge(
                        label = Translations.getString("refresh_2s", currentLang),
                        isSelected = refreshMs == 2000L
                    ) {
                        viewModel.setRefreshInterval(2000L)
                    }
                    RefreshRateBadge(
                        label = Translations.getString("refresh_5s", currentLang),
                        isSelected = refreshMs == 5000L
                    ) {
                        viewModel.setRefreshInterval(5000L)
                    }
                }
            }
        }

        // 2. CSV exporter & cache databases management
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translations.getString("settings_log_export", currentLang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val file = viewModel.exportLogsToCsv()
                        if (file != null) {
                            Toast.makeText(
                                context,
                                "${Translations.getString("toast_exported", currentLang)}${file.name}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                Translations.getString("toast_no_logs", currentLang),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp), // Height class optimized touch target
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share CSV icon", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.getString("settings_export_btn", currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearLogs()
                            Toast.makeText(
                                context,
                                Translations.getString("toast_cache_cleared", currentLang),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text(
                            text = Translations.getString("settings_clear_cache_btn", currentLang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearSessions()
                            Toast.makeText(
                                context,
                                Translations.getString("toast_sessions_cleared", currentLang),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                    ) {
                        Text(
                            text = Translations.getString("settings_clear_sessions_btn", currentLang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Hardware Sysfs Diagnostics node tracker
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeveloperMode,
                        contentDescription = "Debug sysfs diagnostic icon",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Translations.getString("settings_sysfs_diagnostics", currentLang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Translations.getString("settings_sysfs_path", currentLang),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (snapshot.sysfsAttributes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Translations.getString("settings_no_sysfs", currentLang),
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        snapshot.sysfsAttributes.forEach { (key, valString) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF6366F1)
                                )
                                Text(
                                    text = valString,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionGrid(
    selectedLang: AppLanguage,
    onLangSelected: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val row1 = listOf(
            AppLanguage.SIMPLIFIED_CHINESE,
            AppLanguage.TRADITIONAL_CHINESE,
            AppLanguage.ENGLISH,
            AppLanguage.SPANISH
        )
        val row2 = listOf(
            AppLanguage.JAPANESE,
            AppLanguage.GERMAN,
            AppLanguage.KOREAN,
            AppLanguage.FRENCH
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row1.forEach { lang ->
                LanguageBadge(
                    lang = lang,
                    isSelected = selectedLang == lang,
                    onClick = { onLangSelected(lang) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row2.forEach { lang ->
                LanguageBadge(
                    lang = lang,
                    isSelected = selectedLang == lang,
                    onClick = { onLangSelected(lang) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LanguageBadge(
    lang: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.08f) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(vertical = 12.dp), // Padded at least 48dp minimum click boundary
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lang.displayName,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF475569),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun RowScope.TemperatureUnitBadge(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.08f) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF475569),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun RowScope.RefreshRateBadge(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.08f) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
