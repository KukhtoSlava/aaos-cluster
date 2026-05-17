package com.autobox.cluster.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autobox.cluster.repository.ClusterRepository
import com.autobox.cluster.ui.ClusterViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClusterViewModelFactory @Inject constructor(
    private val repository: ClusterRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(ClusterViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ClusterViewModel(repository) as T
    }
}
