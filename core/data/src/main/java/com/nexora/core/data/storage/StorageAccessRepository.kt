package com.nexora.core.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.nexora.core.database.PersistedPermissionEntity
import com.nexora.core.database.PersistedPermissionsDao
import com.nexora.core.model.DocumentAccessMode
import com.nexora.core.model.PersistedUriPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StorageAccessRepository {
    fun observePersistedPermissions(): Flow<List<PersistedUriPermission>>
    suspend fun persistUriPermission(uri: Uri, accessMode: DocumentAccessMode = DocumentAccessMode.READ_WRITE): Result<PersistedUriPermission>
    suspend fun persistTreePermission(uri: Uri, accessMode: DocumentAccessMode = DocumentAccessMode.READ_WRITE): Result<PersistedUriPermission>
    suspend fun releaseUriPermission(uri: Uri)
}

class AndroidStorageAccessRepository(
    private val context: Context,
    private val permissionsDao: PersistedPermissionsDao
) : StorageAccessRepository {
    private val contentResolver: ContentResolver = context.contentResolver

    override fun observePersistedPermissions(): Flow<List<PersistedUriPermission>> =
        permissionsDao.observePermissions().map { entities ->
            entities.map { entity ->
                PersistedUriPermission(
                    uri = entity.uri,
                    persistedAt = java.time.Instant.ofEpochMilli(entity.persistedAt).toString(),
                    accessMode = runCatching { DocumentAccessMode.valueOf(entity.accessMode) }
                        .getOrDefault(DocumentAccessMode.READ_WRITE),
                    isTree = entity.isTree
                )
            }
        }

    override suspend fun persistUriPermission(
        uri: Uri,
        accessMode: DocumentAccessMode
    ): Result<PersistedUriPermission> {
        return runCatching {
            val flags = when (accessMode) {
                DocumentAccessMode.READ -> IntentFlags.read
                DocumentAccessMode.READ_WRITE -> IntentFlags.readWrite
            }
            contentResolver.takePersistableUriPermission(uri, flags)
            val permission = PersistedUriPermission(
                uri = uri.toString(),
                accessMode = accessMode,
                isTree = false
            )
            permissionsDao.upsert(
                PersistedPermissionEntity(
                    uri = permission.uri,
                    isTree = false,
                    accessMode = permission.accessMode.name,
                    persistedAt = System.currentTimeMillis()
                )
            )
            permission
        }
    }

    override suspend fun persistTreePermission(
        uri: Uri,
        accessMode: DocumentAccessMode
    ): Result<PersistedUriPermission> {
        return runCatching {
            val flags = when (accessMode) {
                DocumentAccessMode.READ -> IntentFlags.read
                DocumentAccessMode.READ_WRITE -> IntentFlags.readWrite
            }
            contentResolver.takePersistableUriPermission(uri, flags)
            val permission = PersistedUriPermission(
                uri = uri.toString(),
                accessMode = accessMode,
                isTree = true
            )
            permissionsDao.upsert(
                PersistedPermissionEntity(
                    uri = permission.uri,
                    isTree = true,
                    accessMode = permission.accessMode.name,
                    persistedAt = System.currentTimeMillis()
                )
            )
            permission
        }
    }

    override suspend fun releaseUriPermission(uri: Uri) {
        runCatching {
            contentResolver.releasePersistableUriPermission(uri, IntentFlags.readWrite)
        }
        permissionsDao.remove(uri.toString())
    }

    private object IntentFlags {
        const val read = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        const val readWrite = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
