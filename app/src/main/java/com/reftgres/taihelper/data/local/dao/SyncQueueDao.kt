package com.reftgres.taihelper.data.local.dao

import androidx.room.*
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun getAllSyncItems(): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueEntity): Long

    @Delete
    suspend fun deleteSyncItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE entityId = :entityId AND entityType = :entityType")
    suspend fun deleteSyncItemsByEntityId(entityId: String, entityType: String)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("SELECT * FROM sync_queue LIMIT :limit")
    suspend fun getPendingSyncItems(limit: Int): List<SyncQueueEntity>
}