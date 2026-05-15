package com.nexora.core.data.storage

import com.nexora.core.database.RecentFileEntity
import com.nexora.core.database.RecentFilesDao
import com.nexora.core.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

interface RecentFilesRepository {
    fun observeRecentFiles(): Flow<List<WorkspaceFile>>
    suspend fun addRecent(file: WorkspaceFile)
    suspend fun removeRecent(id: String)
    suspend fun clear()
}

class RoomRecentFilesRepository(
    private val dao: RecentFilesDao,
    private val metadataResolver: DocumentMetadataResolver
) : RecentFilesRepository {
    override fun observeRecentFiles(): Flow<List<WorkspaceFile>> =
        dao.observeRecentFiles().map { entities ->
            entities.map { entity ->
                WorkspaceFile(
                    id = entity.uri,
                    name = entity.name,
                    path = entity.uri,
                    type = runCatching { com.nexora.core.model.DocumentType.valueOf(entity.type) }
                        .getOrDefault(com.nexora.core.model.DocumentType.DOC),
                    lastModified = entity.lastModifiedAt?.let { it.toString() } ?: "",
                    sizeLabel = entity.sizeBytes?.let { com.nexora.core.common.file.FileSizeFormatter.format(it) } ?: "",
                    isPinned = entity.isPinned
                )
            }
        }

    override suspend fun addRecent(file: WorkspaceFile) {
        val now = System.currentTimeMillis()
        val metadata = runCatching { metadataResolver.resolve(android.net.Uri.parse(file.path)) }.getOrNull()
        val lastModifiedAt = metadata?.lastModified
        dao.upsert(
            RecentFileEntity(
                uri = file.path,
                name = file.name,
                type = file.type.name,
                lastOpenedAt = now,
                lastModifiedAt = lastModifiedAt,
                sizeBytes = metadata?.sizeBytes,
                isPinned = file.isPinned,
                mimeType = metadata?.mimeType
            )
        )
    }

    override suspend fun removeRecent(id: String) {
        dao.remove(id)
    }

    override suspend fun clear() {
        dao.clear()
    }
}

class DataStoreRecentFilesRepository(
    private val store: StorageJsonStore
) : RecentFilesRepository {
    override fun observeRecentFiles(): Flow<List<WorkspaceFile>> = store.observeRecents()

    override suspend fun addRecent(file: WorkspaceFile) {
        val current = store.observeRecents().firstOrEmpty()
        val updated = listOf(file) + current.filterNot { it.path == file.path || it.id == file.id }
        store.replaceRecents(updated.take(48))
    }

    override suspend fun removeRecent(id: String) {
        val current = store.observeRecents().firstOrEmpty()
        store.replaceRecents(current.filterNot { it.id == id })
    }

    override suspend fun clear() {
        store.replaceRecents(emptyList())
    }

    private suspend fun Flow<List<WorkspaceFile>>.firstOrEmpty(): List<WorkspaceFile> =
        firstOrNull() ?: emptyList()
}
