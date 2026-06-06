package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BatteryViewModel
import com.example.data.Translations
import com.example.data.AppLanguage
import com.example.ui.screens.BatteryDashboardScreen
import com.example.ui.screens.BatteryHealthScreen
import com.example.ui.screens.ChargingProcessScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

enum class BatteryTab(
    val title: String,
    val icon: ImageVector
) {
    MONITOR("监测", Icons.Default.Dashboard),
    CHARGING("充电", Icons.Default.BatteryChargingFull),
    HEALTH("健康", Icons.Default.Favorite),
    SETTINGS("设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val viewModel: BatteryViewModel = viewModel()
    val snapshot by viewModel.currentSnapshot.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    var currentTab by remember { mutableStateOf(BatteryTab.MONITOR) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9)),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (currentTab) {
                                    BatteryTab.MONITOR -> Translations.getString("title_monitor", currentLang)
                                    BatteryTab.CHARGING -> Translations.getString("title_charging", currentLang)
                                    BatteryTab.HEALTH -> Translations.getString("title_health", currentLang)
                                    BatteryTab.SETTINGS -> Translations.getString("title_settings", currentLang)
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = Color(0xFF1B1B1F)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF3F4F9),
                    titleContentColor = Color(0xFF1B1B1F)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                BatteryTab.values().forEach { tab ->
                    val tabLabel = when (tab) {
                        BatteryTab.MONITOR -> Translations.getString("tab_monitor", currentLang)
                        BatteryTab.CHARGING -> Translations.getString("tab_charging", currentLang)
                        BatteryTab.HEALTH -> Translations.getString("tab_health", currentLang)
                        BatteryTab.SETTINGS -> Translations.getString("tab_settings", currentLang)
                    }
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tabLabel,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tabLabel,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF3B82F6),
                            selectedTextColor = Color(0xFF3B82F6),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = Color(0xFFEFF6FF)
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F9))
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BatteryTab.MONITOR -> BatteryDashboardScreen(viewModel = viewModel)
                BatteryTab.CHARGING -> ChargingProcessScreen(viewModel = viewModel)
                BatteryTab.HEALTH -> BatteryHealthScreen(viewModel = viewModel)
                BatteryTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
