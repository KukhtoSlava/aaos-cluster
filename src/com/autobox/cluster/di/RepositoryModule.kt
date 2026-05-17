package com.autobox.cluster.di

import com.autobox.cluster.repository.ClusterRepository
import com.autobox.cluster.repository.impl.ClusterRepositoryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindClusterRepository(impl: ClusterRepositoryImpl): ClusterRepository
}
