package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_logs")
data class BatteryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: Int,
    val voltage: Int,        // mV
    val current: Int,        // mA
    val power: Double,       // W
    val temperature: Double, // °C
    val isCharging: Boolean,
    val rState: String,      // e.g., "充电中", "放电中", "已充满", "未连接"
    val plugType: String     // e.g., "AC", "USB", "无线", "未知"
)

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val maxPower: Double,
    val maxTemp: Double,
    val durationMs: Long
)
