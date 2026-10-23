package com.autobox.cluster.di

import com.autobox.cluster.settings.ClusterSettings
import com.autobox.cluster.settings.impl.ClusterSettingsImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindClusterSettings(impl: ClusterSettingsImpl): ClusterSettings
}
