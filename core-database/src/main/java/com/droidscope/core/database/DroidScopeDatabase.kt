package com.droidscope.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.droidscope.core.database.dao.TelemetryDao
import com.droidscope.core.database.entity.BatterySampleEntity
import com.droidscope.core.database.entity.BenchmarkResultEntity
import com.droidscope.core.database.entity.DiagnosticEventEntity

@Database(
    entities = [
        BatterySampleEntity::class,
        BenchmarkResultEntity::class,
        DiagnosticEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DroidScopeDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
