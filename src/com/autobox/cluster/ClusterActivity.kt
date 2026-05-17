package com.autobox.cluster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.autobox.cluster.di.ClusterViewModelFactory
import com.autobox.cluster.ui.ClusterScreen
import com.autobox.cluster.ui.ClusterTheme
import com.autobox.cluster.ui.ClusterViewModel
import javax.inject.Inject

class ClusterActivity : ComponentActivity() {

    @Inject
    lateinit var viewModelFactory: ClusterViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as ClusterApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this, viewModelFactory)[ClusterViewModel::class.java]
        setContent {
            ClusterTheme {
                ClusterScreen(viewModel = viewModel)
            }
        }
    }
}
