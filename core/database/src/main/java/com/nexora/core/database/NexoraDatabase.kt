package com.nexora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecentFileEntity::class,
        DocumentSessionEntity::class,
        PersistedPermissionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun recentFilesDao(): RecentFilesDao
    abstract fun documentSessionDao(): DocumentSessionDao
    abstract fun persistedPermissionsDao(): PersistedPermissionsDao
}
