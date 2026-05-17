package com.autobox.cluster

import android.app.Application
import com.autobox.cluster.di.AppComponent
import com.autobox.cluster.di.DaggerAppComponent

class ClusterApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(applicationContext)
    }
}
