package com.nexora.core.data.storage

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

internal val Context.storageDataStore by preferencesDataStore("storage_access")

internal object StorageKeys {
    val RECENT_FILES = stringPreferencesKey("recent_files_json")
    val PERSISTED_PERMISSIONS = stringPreferencesKey("persisted_permissions_json")
}
