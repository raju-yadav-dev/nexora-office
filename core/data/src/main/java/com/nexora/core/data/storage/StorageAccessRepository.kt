package com.nexora.core.data.storage

import android.content.ContentResolver
import android.content.Context
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import com.nexora.core.database.PersistedPermissionEntity
import com.nexora.core.database.PersistedPermissionsDao
import com.nexora.core.model.DocumentAccessMode
import com.nexora.core.model.PersistedUriPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StorageAccessRepository {
    fun observePersistedPermissions(): Flow<List<PersistedUriPermission>>
    fun validatePersistedUriPermission(
        uri: Uri,
        accessMode: DocumentAccessMode = DocumentAccessMode.READ
    ): SafPermissionValidation
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

    override fun validatePersistedUriPermission(
        uri: Uri,
        accessMode: DocumentAccessMode
    ): SafPermissionValidation =
        contentResolver.validatePersistedSafPermission(uri, accessMode)

    override suspend fun persistUriPermission(
        uri: Uri,
        accessMode: DocumentAccessMode
    ): Result<PersistedUriPermission> {
        return runCatching {
            val flags = when (accessMode) {
                DocumentAccessMode.READ -> IntentFlags.read
                DocumentAccessMode.READ_WRITE -> IntentFlags.readWrite
            }
            com.nexora.core.common.logging.NexoraLogger.i(
                logTag,
                "event=saf_persist_request kind=document uri=$uri mode=${accessMode.name}"
            )
            contentResolver.takePersistableUriPermission(uri, flags)
            contentResolver.requirePersistedSafPermission(uri, accessMode)
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
            com.nexora.core.common.logging.NexoraLogger.i(
                logTag,
                "event=saf_persist_success kind=document uri=$uri mode=${accessMode.name}"
            )
            permission
        }.onFailure { error ->
            com.nexora.core.common.logging.NexoraLogger.w(
                logTag,
                "event=saf_persist_failure kind=document uri=$uri mode=${accessMode.name}",
                error
            )
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
            com.nexora.core.common.logging.NexoraLogger.i(
                logTag,
                "event=saf_persist_request kind=tree uri=$uri mode=${accessMode.name}"
            )
            contentResolver.takePersistableUriPermission(uri, flags)
            contentResolver.requirePersistedSafPermission(uri, accessMode)
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
            com.nexora.core.common.logging.NexoraLogger.i(
                logTag,
                "event=saf_persist_success kind=tree uri=$uri mode=${accessMode.name}"
            )
            permission
        }.onFailure { error ->
            com.nexora.core.common.logging.NexoraLogger.w(
                logTag,
                "event=saf_persist_failure kind=tree uri=$uri mode=${accessMode.name}",
                error
            )
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

    private companion object {
        const val logTag = "StorageAccessRepository"
    }
}

data class SafPermissionValidation(
    val requestedUri: Uri,
    val persistedUri: Uri?,
    val readGranted: Boolean,
    val writeGranted: Boolean,
    val grantedByTree: Boolean
) {
    val isGranted: Boolean
        get() = readGranted

    fun allows(accessMode: DocumentAccessMode): Boolean =
        when (accessMode) {
            DocumentAccessMode.READ -> readGranted
            DocumentAccessMode.READ_WRITE -> readGranted && writeGranted
        }
}

class SafPermissionMissingException(
    val requestedUri: Uri,
    val accessMode: DocumentAccessMode
) : SecurityException("Document access needs to be restored. Reopen the document to continue.")

fun ContentResolver.requirePersistedSafPermission(
    uri: Uri,
    accessMode: DocumentAccessMode = DocumentAccessMode.READ
): SafPermissionValidation {
    val validation = validatePersistedSafPermission(uri, accessMode)
    if (!validation.allows(accessMode)) {
        throw SafPermissionMissingException(uri, accessMode)
    }
    return validation
}

fun ContentResolver.validatePersistedSafPermission(
    uri: Uri,
    accessMode: DocumentAccessMode = DocumentAccessMode.READ
): SafPermissionValidation {
    val matchingPermission = persistedUriPermissions
        .filter { it.matches(uri) }
        .firstOrNull { permission ->
            when (accessMode) {
                DocumentAccessMode.READ -> permission.isReadPermission
                DocumentAccessMode.READ_WRITE -> permission.isReadPermission && permission.isWritePermission
            }
        }
        ?: persistedUriPermissions.firstOrNull { it.matches(uri) }

    return SafPermissionValidation(
        requestedUri = uri,
        persistedUri = matchingPermission?.uri,
        readGranted = matchingPermission?.isReadPermission == true,
        writeGranted = matchingPermission?.isWritePermission == true,
        grantedByTree = matchingPermission != null && matchingPermission.uri != uri
    )
}

private fun UriPermission.matches(target: Uri): Boolean {
    if (uri == target) return true
    if (uri.authority != target.authority) return false

    val persistedTreeId = runCatching {
        if (DocumentsContract.isTreeUri(uri)) DocumentsContract.getTreeDocumentId(uri) else null
    }.getOrNull() ?: return false

    val targetDocumentId = runCatching { DocumentsContract.getDocumentId(target) }.getOrNull()
    val targetTreeId = runCatching {
        if (DocumentsContract.isTreeUri(target)) DocumentsContract.getTreeDocumentId(target) else null
    }.getOrNull()

    return listOfNotNull(targetDocumentId, targetTreeId).any { documentId ->
        documentId == persistedTreeId || documentId.startsWith("$persistedTreeId/")
    }
}
