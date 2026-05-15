package com.nexora.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentSessionDao {
    @Query("SELECT * FROM document_sessions ORDER BY lastActiveAt DESC")
    fun observeSessions(): Flow<List<DocumentSessionEntity>>

    @Query("SELECT * FROM document_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun findById(sessionId: String): DocumentSessionEntity?

    @Query("SELECT * FROM document_sessions WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): DocumentSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: DocumentSessionEntity)

    @Query("UPDATE document_sessions SET dirty = :dirty, lastActiveAt = :lastActiveAt WHERE sessionId = :sessionId")
    suspend fun updateDirty(sessionId: String, dirty: Boolean, lastActiveAt: Long)

    @Query("UPDATE document_sessions SET autosavePayload = :payload, autosaveAt = :autosaveAt, recoveryPayload = :recoveryPayload, recoveryAt = :recoveryAt WHERE sessionId = :sessionId")
    suspend fun updateAutosave(
        sessionId: String,
        payload: String?,
        autosaveAt: Long?,
        recoveryPayload: String?,
        recoveryAt: Long?
    )

    @Query("UPDATE document_sessions SET uri = :uri, title = :title, type = :type, lastActiveAt = :lastActiveAt WHERE sessionId = :sessionId")
    suspend fun updateFileMetadata(
        sessionId: String,
        uri: String,
        title: String,
        type: String,
        lastActiveAt: Long
    )

    @Query("UPDATE document_sessions SET lastActiveAt = :lastActiveAt WHERE sessionId = :sessionId")
    suspend fun updateLastActive(sessionId: String, lastActiveAt: Long)

    @Query("DELETE FROM document_sessions WHERE sessionId = :sessionId")
    suspend fun remove(sessionId: String)

    @Query("DELETE FROM document_sessions")
    suspend fun clear()
}
