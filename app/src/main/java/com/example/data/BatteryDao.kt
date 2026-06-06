package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<BatteryLogEntity>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int): Flow<List<BatteryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatteryLogEntity): Long

    @Query("DELETE FROM battery_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM battery_logs WHERE timestamp < :threshold")
    suspend fun pruneLogs(threshold: Long)

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<ChargingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: ChargingSessionEntity)

    @Query("DELETE FROM charging_sessions")
    suspend fun clearSessions()
}
