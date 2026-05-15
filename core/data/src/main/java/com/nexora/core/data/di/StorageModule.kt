package com.nexora.core.data.di

import android.content.Context
import com.nexora.core.data.storage.AndroidStorageAccessRepository
import com.nexora.core.data.storage.DocumentMetadataResolver
import com.nexora.core.data.storage.FileOpenManager
import com.nexora.core.data.storage.RecentFilesRepository
import com.nexora.core.data.storage.RoomRecentFilesRepository
import com.nexora.core.data.storage.StorageAccessRepository
import com.nexora.core.data.storage.StorageJsonStore
import com.nexora.core.database.PersistedPermissionsDao
import com.nexora.core.database.RecentFilesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideStorageJsonStore(@ApplicationContext context: Context): StorageJsonStore =
        StorageJsonStore(context)

    @Provides
    @Singleton
    fun provideStorageAccessRepository(
        @ApplicationContext context: Context,
        permissionsDao: PersistedPermissionsDao
    ): StorageAccessRepository = AndroidStorageAccessRepository(context, permissionsDao)

    @Provides
    @Singleton
    fun provideRecentFilesRepository(
        dao: RecentFilesDao,
        resolver: DocumentMetadataResolver
    ): RecentFilesRepository = RoomRecentFilesRepository(dao, resolver)

    @Provides
    @Singleton
    fun provideMetadataResolver(@ApplicationContext context: Context): DocumentMetadataResolver =
        DocumentMetadataResolver(context)

    @Provides
    @Singleton
    fun provideFileOpenManager(
        @ApplicationContext context: Context,
        resolver: DocumentMetadataResolver
    ): FileOpenManager = FileOpenManager(context, resolver)
}
