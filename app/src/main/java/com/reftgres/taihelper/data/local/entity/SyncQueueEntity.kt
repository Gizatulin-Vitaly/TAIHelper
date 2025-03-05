package com.reftgres.taihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityId: String,
    val entityType: String,
    val operation: String,
    val jsonData: String,
    val timestamp: Date = Date(),
    val retryCount: Int = 0
)