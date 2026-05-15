package com.nexora.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val type: String,
    val lastOpenedAt: Long,
    val lastModifiedAt: Long?,
    val sizeBytes: Long?,
    val isPinned: Boolean,
    val mimeType: String?
)

@Entity(tableName = "document_sessions")
data class DocumentSessionEntity(
    @PrimaryKey val sessionId: String,
    val uri: String?,
    val title: String,
    val type: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val dirty: Boolean,
    val autosavePayload: String?,
    val autosaveAt: Long?,
    val recoveryPayload: String?,
    val recoveryAt: Long?
)

@Entity(tableName = "persisted_permissions")
data class PersistedPermissionEntity(
    @PrimaryKey val uri: String,
    val isTree: Boolean,
    val accessMode: String,
    val persistedAt: Long
)
