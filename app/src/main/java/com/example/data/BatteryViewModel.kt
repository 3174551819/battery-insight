package com.example.data

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import kotlin.math.abs

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val dao = database.batteryDao()
    val repository = BatteryRepository(dao)

    // Flow of historical records & sessions
    val allLogs: StateFlow<List<BatteryLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<ChargingSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time states
    private val _currentSnapshot = MutableStateFlow(BatterySnapshot())
    val currentSnapshot: StateFlow<BatterySnapshot> = _currentSnapshot.asStateFlow()

    // Sliding window of logs in-memory for smooth curves (max 100 entries)
    private val _inMemoryHistory = MutableStateFlow<List<BatterySnapshot>>(emptyList())
    val inMemoryHistory: StateFlow<List<BatterySnapshot>> = _inMemoryHistory.asStateFlow()

    private val prefs = context.getSharedPreferences("battery_monitor_prefs", Context.MODE_PRIVATE)

    // Configuration / settings
    private val _refreshIntervalMs = MutableStateFlow(prefs.getLong("refresh_interval", 1000L))
    val refreshIntervalMs: StateFlow<Long> = _refreshIntervalMs.asStateFlow()

    private val _tempUnit = MutableStateFlow(TemperatureUnit.fromCode(prefs.getString("temperature_unit", "C") ?: "C"))
    val tempUnit: StateFlow<TemperatureUnit> = _tempUnit.asStateFlow()

    private val _isDemoMode = MutableStateFlow(prefs.getBoolean("is_demo_mode", false))
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.fromCode(prefs.getString("app_language", "zh") ?: "zh"))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Active session status while charging is ongoing
    private var sessionStartTime: Long? = null
    private var sessionStartLevel: Int = 0
    private var sessionMaxPower: Double = 0.0
    private var sessionMaxTemp: Double = 0.0

    // Background polling job
    private var pollingJob: Job? = null
    private var dbLoggingCounter = 0

    // Alert lists
    private val _activeAlerts = MutableStateFlow<List<String>>(emptyList())
    val activeAlerts: StateFlow<List<String>> = _activeAlerts.asStateFlow()

    init {
        // Automatically check if running in emulator to enable simulation by default unless already set
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
        
        if (isEmulator && !prefs.contains("is_demo_mode")) {
            _isDemoMode.value = true
            prefs.edit().putBoolean("is_demo_mode", true).apply()
        }

        startPolling()
    }

    fun setRefreshInterval(ms: Long) {
        _refreshIntervalMs.value = ms
        prefs.edit().putLong("refresh_interval", ms).apply()
        startPolling() // restart with new interval
    }

    fun toggleTemperatureUnit() {
        val nextUnit = when (_tempUnit.value) {
            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.KELVIN
            TemperatureUnit.KELVIN -> TemperatureUnit.CELSIUS
        }
        _tempUnit.value = nextUnit
        prefs.edit().putString("temperature_unit", nextUnit.code).apply()
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        _tempUnit.value = unit
        prefs.edit().putString("temperature_unit", unit.code).apply()
    }

    fun formatTemperature(tempC: Double, unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> String.format(Locale.US, "%.1f ℃", tempC)
            TemperatureUnit.FAHRENHEIT -> String.format(Locale.US, "%.1f ℉", tempC * 1.8 + 32.0)
            TemperatureUnit.KELVIN -> String.format(Locale.US, "%.1f K", tempC + 273.15)
        }
    }

    fun convertTemperature(tempC: Double, unit: TemperatureUnit): Double {
        return when (unit) {
            TemperatureUnit.CELSIUS -> tempC
            TemperatureUnit.FAHRENHEIT -> tempC * 1.8 + 32.0
            TemperatureUnit.KELVIN -> tempC + 273.15
        }
    }

    fun toggleDemoMode() {
        val newVal = !_isDemoMode.value
        _isDemoMode.value = newVal
        prefs.edit().putBoolean("is_demo_mode", newVal).apply()
        // Reset in-memory on model toggles
        _inMemoryHistory.value = emptyList()
    }

    fun setLanguage(language: AppLanguage) {
        val oldLang = _appLanguage.value
        if (oldLang == language) return

        _appLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()

        // Dynamic local app name/icon label switching via activity-alias
        updateLauncherAlias(language)
    }

    private fun updateLauncherAlias(language: AppLanguage) {
        val pm = context.packageManager
        val packageName = context.packageName

        val aliases = mapOf(
            AppLanguage.SIMPLIFIED_CHINESE to "$packageName.MainActivity_zh",
            AppLanguage.TRADITIONAL_CHINESE to "$packageName.MainActivity_zhtw",
            AppLanguage.ENGLISH to "$packageName.MainActivity_en",
            AppLanguage.JAPANESE to "$packageName.MainActivity_ja",
            AppLanguage.GERMAN to "$packageName.MainActivity_de",
            AppLanguage.KOREAN to "$packageName.MainActivity_ko",
            AppLanguage.FRENCH to "$packageName.MainActivity_fr",
            AppLanguage.SPANISH to "$packageName.MainActivity_es"
        )

        val targetAliasName = aliases[language] ?: "$packageName.MainActivity_zh"

        // 1. Enable the targeted alias first
        try {
            pm.setComponentEnabledSetting(
                ComponentName(context, targetAliasName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e("BatteryViewModel", "Failed to enable target alias $targetAliasName: ${e.message}")
        }

        // 2. Disable all other aliases so only one remains active and registered
        aliases.values.forEach { aliasName ->
            if (aliasName != targetAliasName) {
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(context, aliasName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    Log.e("BatteryViewModel", "Failed to disable alias $aliasName: ${e.message}")
                }
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                val snapshot = if (_isDemoMode.value) {
                    generateSimulatedSnapshot()
                } else {
                    BatteryCollector.takeSnapshot(context)
                }

                _currentSnapshot.value = snapshot

                // Maintain in-memory lists (max 100 items)
                val currentList = _inMemoryHistory.value.toMutableList()
                currentList.add(snapshot)
                if (currentList.size > 100) {
                    currentList.removeAt(0)
                }
                _inMemoryHistory.value = currentList

                // Analyze anomalies
                checkAnomalies(snapshot)

                // Track charging session life-cycle
                handleChargingSessionTracking(snapshot)

                // Persist stats periodically: e.g., every 10 ticks to keep DB light,
                // or immediately on status / plug transitions.
                val prevSnapshot = if (currentList.size > 1) currentList[currentList.size - 2] else null
                dbLoggingCounter++
                val shouldSaveToDb = dbLoggingCounter >= 10 ||
                        prevSnapshot == null ||
                        prevSnapshot.status != snapshot.status ||
                        prevSnapshot.level != snapshot.level

                if (shouldSaveToDb) {
                    dbLoggingCounter = 0
                    saveSnapshotToDatabase(snapshot)
                }

                delay(_refreshIntervalMs.value)
            }
        }
    }

    private fun saveSnapshotToDatabase(snapshot: BatterySnapshot) {
        viewModelScope.launch {
            try {
                repository.insertLog(
                    BatteryLogEntity(
                        timestamp = snapshot.timestamp,
                        level = snapshot.level,
                        voltage = snapshot.voltageMv,
                        current = snapshot.currentMa,
                        power = snapshot.powerW,
                        temperature = snapshot.temperatureC,
                        isCharging = snapshot.status == "充电中" || snapshot.status == "已充满",
                        rState = snapshot.status,
                        plugType = snapshot.plugType
                    )
                )

                // Auto prune records older than 48 hours to conserve phone memory
                val threshold = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
                repository.pruneLogs(threshold)
            } catch (e: Exception) {
                Log.e("BatteryViewModel", "Failed DB save: ${e.message}")
            }
        }
    }

    private fun handleChargingSessionTracking(snapshot: BatterySnapshot) {
        val isChargingState = snapshot.status == "充电中"
        if (isChargingState) {
            if (sessionStartTime == null) {
                // Session started!
                sessionStartTime = snapshot.timestamp
                sessionStartLevel = snapshot.level
                sessionMaxPower = snapshot.powerW
                sessionMaxTemp = snapshot.temperatureC
            } else {
                // Session ongoing, aggregate maxima
                if (snapshot.powerW > sessionMaxPower) sessionMaxPower = snapshot.snapshotPowerMax()
                if (snapshot.temperatureC > sessionMaxTemp) sessionMaxTemp = snapshot.temperatureC
            }
        } else {
            // Charging stopped or unplugged
            val start = sessionStartTime
            if (start != null) {
                val end = snapshot.timestamp
                val duration = end - start
                val startPct = sessionStartLevel
                val endPct = snapshot.level

                // Only record meaningful sessions (charged at least 1% or lasted 5 seconds)
                if (endPct > startPct || duration > 5000) {
                    viewModelScope.launch {
                        try {
                            repository.insertSession(
                                ChargingSessionEntity(
                                    startTime = start,
                                    endTime = end,
                                    startLevel = startPct,
                                    endLevel = endPct,
                                    maxPower = sessionMaxPower,
                                    maxTemp = sessionMaxTemp,
                                    durationMs = duration
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("BatteryViewModel", "Failed insert session: ${e.message}")
                        }
                    }
                }
                // Clear session
                sessionStartTime = null
            }
        }
    }

    private fun checkAnomalies(snapshot: BatterySnapshot) {
        val alerts = mutableListOf<String>()
        val lang = _appLanguage.value
        val currentUnit = _tempUnit.value
        val formattedTemp = formatTemperature(snapshot.temperatureC, currentUnit)

        if (snapshot.temperatureC > 45.0) {
            alerts.add(String.format(Translations.getString("alert_high_temp", lang), formattedTemp))
        }
        if (snapshot.status == "放电中" && snapshot.currentMa < -800) {
            alerts.add(String.format(Translations.getString("alert_high_current", lang), snapshot.currentMa.toString()))
        }
        if (snapshot.voltageMv > 4450) {
            alerts.add(String.format(Translations.getString("alert_high_voltage", lang), snapshot.voltageMv.toString()))
        }
        if (snapshot.isCapLocked) {
            alerts.add(Translations.getString("alert_cap_lock", lang))
        }
        _activeAlerts.value = alerts
    }

    // Interactive simulated data generator for smooth curves & testing
    private var simLevel = 45.0
    private var simTemp = 31.2
    private var simIsCharging = false
    private var simCurrent = -180.0
    private var simVoltage = 3850.0

    private fun generateSimulatedSnapshot(): BatterySnapshot {
        val timeNow = System.currentTimeMillis()

        // Slowly modify parameters
        if (simIsCharging) {
            simLevel += 0.05 // Increment level
            if (simLevel >= 100.0) {
                simLevel = 100.0
                simIsCharging = false
            }
            simCurrent = 1200.0 + Random.nextDouble(-50.0, 50.0)
            if (simLevel > 80.0) {
                // Trickle charger current tapering
                val factor = (100.0 - simLevel) / 20.0
                simCurrent *= factor
            }
            simTemp += 0.02 * (simCurrent / 500.0)
            simTemp = simTemp.coerceAtMost(46.8)
            simVoltage = 4220.0 + (simLevel - 80) * 1.5 + Random.nextDouble(-5.0, 5.0)
        } else {
            simLevel -= 0.015 // Discharging
            if (simLevel <= 1.0) {
                simLevel = 1.0
                simIsCharging = true
            }
            simCurrent = -220.0 + Random.nextDouble(-80.0, 80.0)
            simTemp -= 0.005
            simTemp = simTemp.coerceAtLeast(29.5)
            simVoltage = 3700.0 + (simLevel * 4.5) + Random.nextDouble(-3.0, 3.0)
        }

        val power = (simVoltage * simCurrent) / 1000000.0
        val plugText = if (simIsCharging) "车载/快充(18W以上)" else "未插电"
        val statusText = if (simLevel >= 100.0) "已充满" else if (simIsCharging) "充电中" else "放电中"

        val dynamicSysfs = mapOf(
            "电量级别 (capacity)" to "${simLevel.toInt()}",
            "实时电压 (voltage_now)" to "${(simVoltage * 1000).toLong()}",
            "真实电流 (current_now)" to "${(simCurrent * 1000).toLong()}",
            "电池温度 (temp)" to "${(simTemp * 10).toInt()}",
            "循环次数 (cycle_count)" to "162"
        )

        return BatterySnapshot(
            timestamp = timeNow,
            level = simLevel.toInt(),
            voltageMv = simVoltage.toInt(),
            currentMa = simCurrent.toInt(),
            powerW = Math.round(power * 100.0) / 100.0,
            temperatureC = Math.round(simTemp * 10.0) / 10.0,
            status = statusText,
            health = "良好",
            plugType = plugText,
            technology = "Li-poly",
            healthPercent = 94,
            designCapacityMah = 4500,
            currentCapacityMah = 4230,
            cycleCount = 162,
            isCapLocked = simTemp > 45.0,
            apiSource = "引擎底层物理仿真 (Demo)",
            sysfsAttributes = dynamicSysfs
        )
    }

    // Actions triggering simulated charge plug
    fun triggerSimulatedPlug(plugged: Boolean) {
        if (_isDemoMode.value) {
            simIsCharging = plugged
            if (plugged) {
                simCurrent = 1500.0
            } else {
                simCurrent = -200.0
            }
        }
    }

    // Helper functions for session peaks
    private fun BatterySnapshot.snapshotPowerMax() = abs(powerW)

    // Clear Database Logs
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _inMemoryHistory.value = emptyList()
        }
    }

    fun clearSessions() {
        viewModelScope.launch {
            repository.clearSessions()
        }
    }

    // Generate CSV and return string contents or write to cached file
    fun exportLogsToCsv(): File? {
        val allCurrentLogs = allLogs.value
        if (allCurrentLogs.isEmpty()) return null

        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "BatteryMonitor_Export_${System.currentTimeMillis()}.csv")
            val writer = FileWriter(file)

            // Header block
            writer.append("记录时间,当前电量(%),电压(mV),电流(mA),功率(W),温度(℃),充电状态,充电类型\n")

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (log in allCurrentLogs) {
                val date = sdf.format(Date(log.timestamp))
                writer.append("$date,${log.level},${log.voltage},${log.current},${log.power},${log.temperature},${log.rState},${log.plugType}\n")
            }

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            Log.e("BatteryViewModel", "Failed exporting logs: ${e.message}")
            null
        }
    }
}
