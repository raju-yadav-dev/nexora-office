package com.nexora.core.common.di

import com.nexora.core.common.permissions.AndroidPermissionManager
import com.nexora.core.common.permissions.PermissionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    @Singleton
    abstract fun bindPermissionManager(impl: AndroidPermissionManager): PermissionManager
}
