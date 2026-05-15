package com.nexora.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFilesDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC")
    fun observeRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Query("DELETE FROM recent_files")
    suspend fun clear()
}
