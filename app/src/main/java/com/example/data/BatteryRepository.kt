package com.example.data

import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val batteryDao: BatteryDao) {
    val allLogs: Flow<List<BatteryLogEntity>> = batteryDao.getAllLogsFlow()
    val allSessions: Flow<List<ChargingSessionEntity>> = batteryDao.getAllSessionsFlow()

    fun getRecentLogs(limit: Int): Flow<List<BatteryLogEntity>> = batteryDao.getRecentLogsFlow(limit)

    suspend fun insertLog(log: BatteryLogEntity): Long = batteryDao.insertLog(log)

    suspend fun clearLogs() = batteryDao.clearLogs()

    suspend fun pruneLogs(threshold: Long) = batteryDao.pruneLogs(threshold)

    suspend fun insertSession(session: ChargingSessionEntity): Long = batteryDao.insertSession(session)

    suspend fun deleteSession(session: ChargingSessionEntity) = batteryDao.deleteSession(session)

    suspend fun clearSessions() = batteryDao.clearSessions()
}
