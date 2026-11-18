package com.droidscope.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_events")
data class DiagnosticEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val title: String,
    val description: String,
    val severity: String,
    val evidence: String,
    val recommendation: String
)
