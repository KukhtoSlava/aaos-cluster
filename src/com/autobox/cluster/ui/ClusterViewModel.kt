package com.autobox.cluster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.repository.ClusterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ClusterViewModel(
    private val repository: ClusterRepository,
) : ViewModel() {

    val data: StateFlow<ClusterData> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ClusterData(),
    )

    init {
        repository.connect()
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}
