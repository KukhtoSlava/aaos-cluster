package com.autobox.cluster.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.repository.ClusterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

enum class GaugeStyle {
    ROUND, // two round dials, speedometer left, tachometer right
    ARC,   // vertical arc scales on the screen edges with large readouts
}

class ClusterViewModel(
    private val repository: ClusterRepository,
) : ViewModel() {

    val data: StateFlow<ClusterData> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ClusterData(),
    )

    private val _gaugeStyle = MutableStateFlow(GaugeStyle.ROUND)
    val gaugeStyle: StateFlow<GaugeStyle> = _gaugeStyle.asStateFlow()

    init {
        repository.connect()
    }

    fun setGaugeStyle(style: GaugeStyle) {
        _gaugeStyle.value = style
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}
