package com.autobox.cluster.di

import android.content.Context
import com.autobox.cluster.ClusterActivity
import com.autobox.cluster.settings.ClusterSettings
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [RepositoryModule::class, SettingsModule::class])
interface AppComponent {

    fun inject(activity: ClusterActivity)

    fun clusterSettings(): ClusterSettings

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}
