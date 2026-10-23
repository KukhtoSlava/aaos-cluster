package com.autobox.cluster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.model.GaugeStyle
import com.autobox.cluster.repository.ClusterRepository
import com.autobox.cluster.settings.ClusterSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ClusterViewModel(
    private val repository: ClusterRepository,
    private val settings: ClusterSettings,
) : ViewModel() {

    val uiState: StateFlow<ClusterUiState> = combine(
        repository.data,
        settings.gaugeStyle,
        ::ClusterUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        // Start from the current values, not empty data
        initialValue = ClusterUiState(repository.data.value, settings.gaugeStyle.value),
    )

    init {
        repository.connect()
    }

    fun setGaugeStyle(style: GaugeStyle) {
        settings.setGaugeStyle(style)
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}

data class ClusterUiState(
    val data: ClusterData,
    val gaugeStyle: GaugeStyle,
)
