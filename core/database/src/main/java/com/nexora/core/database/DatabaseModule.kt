package com.nexora.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NexoraDatabase =
        Room.databaseBuilder(context, NexoraDatabase::class.java, "nexora.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRecentFilesDao(database: NexoraDatabase): RecentFilesDao =
        database.recentFilesDao()

    @Provides
    fun provideDocumentSessionDao(database: NexoraDatabase): DocumentSessionDao =
        database.documentSessionDao()

    @Provides
    fun providePersistedPermissionsDao(database: NexoraDatabase): PersistedPermissionsDao =
        database.persistedPermissionsDao()
}
