package com.nexora.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistedPermissionsDao {
    @Query("SELECT * FROM persisted_permissions ORDER BY persistedAt DESC")
    fun observePermissions(): Flow<List<PersistedPermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(permission: PersistedPermissionEntity)

    @Query("DELETE FROM persisted_permissions WHERE uri = :uri")
    suspend fun remove(uri: String)
}
