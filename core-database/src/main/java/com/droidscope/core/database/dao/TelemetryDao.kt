package com.droidscope.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.droidscope.core.database.entity.BatterySampleEntity
import com.droidscope.core.database.entity.BenchmarkResultEntity
import com.droidscope.core.database.entity.DiagnosticEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insertBatterySample(sample: BatterySampleEntity)

    @Query("SELECT * FROM battery_samples ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecentBatterySamples(limit: Int = 200): Flow<List<BatterySampleEntity>>

    @Query("DELETE FROM battery_samples WHERE timestampMs < :olderThanMs")
    suspend fun pruneBatterySamples(olderThanMs: Long)

    @Insert
    suspend fun insertBenchmarkResult(result: BenchmarkResultEntity)

    @Query("SELECT * FROM benchmark_results ORDER BY timestampMs DESC")
    fun observeBenchmarkResults(): Flow<List<BenchmarkResultEntity>>

    @Insert
    suspend fun insertDiagnosticEvent(event: DiagnosticEventEntity)

    @Query("SELECT * FROM diagnostic_events ORDER BY timestampMs DESC LIMIT :limit")
    fun observeDiagnosticEvents(limit: Int = 100): Flow<List<DiagnosticEventEntity>>

    @Query("DELETE FROM battery_samples")
    suspend fun clearBatteryHistory()

    @Query("DELETE FROM benchmark_results")
    suspend fun clearBenchmarkHistory()

    @Query("DELETE FROM diagnostic_events")
    suspend fun clearDiagnosticHistory()
}
