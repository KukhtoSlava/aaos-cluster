package com.autobox.cluster.repository

import com.autobox.cluster.model.ClusterData
import kotlinx.coroutines.flow.StateFlow

interface ClusterRepository {
    val data: StateFlow<ClusterData>
    fun connect()
    fun disconnect()
}
