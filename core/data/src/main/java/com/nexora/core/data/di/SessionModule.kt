package com.nexora.core.data.di

import com.nexora.core.data.session.DocumentSessionRepository
import com.nexora.core.data.session.RoomDocumentSessionRepository
import com.nexora.core.database.DocumentSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideDocumentSessionRepository(dao: DocumentSessionDao): DocumentSessionRepository =
        RoomDocumentSessionRepository(dao)
}
