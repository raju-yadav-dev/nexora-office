package com.nexora.core.data.session

import com.nexora.core.database.DocumentSessionDao
import com.nexora.core.database.DocumentSessionEntity
import com.nexora.core.model.DocumentSession
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface DocumentSessionRepository {
    fun observeSessions(): Flow<List<DocumentSession>>
    suspend fun openSession(file: WorkspaceFile): DocumentSession
    suspend fun openOrCreateUntitled(type: DocumentType, title: String): DocumentSession
    suspend fun findSession(sessionId: String): DocumentSession?
    suspend fun markDirty(sessionId: String, dirty: Boolean)
    suspend fun updateAutosave(sessionId: String, payload: String?, recoveryPayload: String?)
    suspend fun updateFileMetadata(sessionId: String, uri: String, title: String, type: DocumentType)
    suspend fun touch(sessionId: String)
    suspend fun close(sessionId: String)
}

class RoomDocumentSessionRepository(
    private val dao: DocumentSessionDao
) : DocumentSessionRepository {
    override fun observeSessions(): Flow<List<DocumentSession>> =
        dao.observeSessions().map { entities -> entities.map { it.toModel() } }

    override suspend fun openSession(file: WorkspaceFile): DocumentSession {
        val existing = dao.findByUri(file.path)
        if (existing != null) {
            val updated = existing.copy(lastActiveAt = System.currentTimeMillis())
            dao.upsert(updated)
            return updated.toModel()
        }
        val now = System.currentTimeMillis()
        val session = DocumentSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            uri = file.path,
            title = file.name,
            type = file.type.name,
            createdAt = now,
            lastActiveAt = now,
            dirty = false,
            autosavePayload = null,
            autosaveAt = null,
            recoveryPayload = null,
            recoveryAt = null
        )
        dao.upsert(session)
        return session.toModel()
    }

    override suspend fun openOrCreateUntitled(type: DocumentType, title: String): DocumentSession {
        val now = System.currentTimeMillis()
        val session = DocumentSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            uri = null,
            title = title,
            type = type.name,
            createdAt = now,
            lastActiveAt = now,
            dirty = false,
            autosavePayload = null,
            autosaveAt = null,
            recoveryPayload = null,
            recoveryAt = null
        )
        dao.upsert(session)
        return session.toModel()
    }

    override suspend fun findSession(sessionId: String): DocumentSession? =
        dao.findById(sessionId)?.toModel()

    override suspend fun markDirty(sessionId: String, dirty: Boolean) {
        dao.updateDirty(sessionId, dirty, System.currentTimeMillis())
    }

    override suspend fun updateAutosave(sessionId: String, payload: String?, recoveryPayload: String?) {
        val now = System.currentTimeMillis()
        dao.updateAutosave(
            sessionId = sessionId,
            payload = payload,
            autosaveAt = payload?.let { now },
            recoveryPayload = recoveryPayload,
            recoveryAt = recoveryPayload?.let { now }
        )
    }

    override suspend fun updateFileMetadata(sessionId: String, uri: String, title: String, type: DocumentType) {
        dao.updateFileMetadata(sessionId, uri, title, type.name, System.currentTimeMillis())
    }

    override suspend fun touch(sessionId: String) {
        dao.updateLastActive(sessionId, System.currentTimeMillis())
    }

    override suspend fun close(sessionId: String) {
        dao.remove(sessionId)
    }
}

private fun DocumentSessionEntity.toModel(): DocumentSession = DocumentSession(
    sessionId = sessionId,
    fileUri = uri,
    title = title,
    type = runCatching { DocumentType.valueOf(type) }.getOrDefault(DocumentType.DOC),
    lastActiveAt = lastActiveAt,
    dirty = dirty,
    autosavePayload = autosavePayload,
    recoveryPayload = recoveryPayload
)
