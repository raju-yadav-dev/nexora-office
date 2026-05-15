package com.nexora.core.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.nexora.core.model.PersistedUriPermission
import com.nexora.core.model.WorkspaceFile
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StorageJsonStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeRecents(): Flow<List<WorkspaceFile>> =
        context.storageDataStore.data.map { prefs ->
            prefs[StorageKeys.RECENT_FILES]?.let { payload ->
                runCatching { json.decodeFromString<List<WorkspaceFile>>(payload) }.getOrDefault(emptyList())
            } ?: emptyList()
        }

    suspend fun replaceRecents(files: List<WorkspaceFile>) {
        context.storageDataStore.edit { prefs ->
            prefs[StorageKeys.RECENT_FILES] = json.encodeToString(files)
        }
    }

    fun observePermissions(): Flow<List<PersistedUriPermission>> =
        context.storageDataStore.data.map { prefs ->
            prefs[StorageKeys.PERSISTED_PERMISSIONS]?.let { payload ->
                runCatching { json.decodeFromString<List<PersistedUriPermission>>(payload) }.getOrDefault(emptyList())
            } ?: emptyList()
        }

    suspend fun upsertPermission(permission: PersistedUriPermission) {
        val current = observePermissionsOnce()
        val updated = current.filterNot { it.uri == permission.uri } + permission
        context.storageDataStore.edit { prefs ->
            prefs[StorageKeys.PERSISTED_PERMISSIONS] = json.encodeToString(updated)
        }
    }

    suspend fun removePermission(uri: String) {
        val current = observePermissionsOnce()
        val updated = current.filterNot { it.uri == uri }
        context.storageDataStore.edit { prefs ->
            prefs[StorageKeys.PERSISTED_PERMISSIONS] = json.encodeToString(updated)
        }
    }

    private suspend fun observePermissionsOnce(): List<PersistedUriPermission> =
        observePermissions().firstOrEmpty()

    private suspend fun Flow<List<PersistedUriPermission>>.firstOrEmpty(): List<PersistedUriPermission> =
        firstOrNull() ?: emptyList()
}
