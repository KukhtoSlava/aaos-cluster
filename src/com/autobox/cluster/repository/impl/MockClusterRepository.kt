package com.autobox.cluster.repository.impl

import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.model.Gear
import com.autobox.cluster.repository.ClusterRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Simulated data source for UI development and testing.
 * Cycles through driving scenarios every ~60 s and toggles telltales
 * so every visual state can be verified without real hardware.
 */
@Singleton
class MockClusterRepository @Inject constructor() : ClusterRepository {

    private val _data = MutableStateFlow(
        ClusterData(
            absFault        = true,
            engineWarning   = true,
            batteryWarning  = true,
            oilWarning      = true,
            fuelWarning     = true,
            brakeWarning    = true,
            seatbeltWarning = true,
            tpmsWarning     = true,
            parkingLightsOn = true,
            lowBeamOn       = false,
            highBeamOn      = false,
            turnSignalLeft  = true,
            turnSignalRight = true,
        )
    )
    override val data: StateFlow<ClusterData> = _data.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun connect() {
        if (job != null) return
        job = scope.launch {
            var tick = 0L
            while (isActive) {
                delay(200L)
                tick++
                _data.update { cur -> next(cur, tick) }
            }
        }
    }

    override fun disconnect() {
        job?.cancel()
        job = null
    }

    private fun next(cur: ClusterData, tick: Long): ClusterData {
        // 300 ticks × 200 ms = 60-second scenario loop
        val scenarioTick = (tick % 300).toInt()
        val phase = tick * 0.03

        val (speed, gear) = when {
            scenarioTick < 60 -> {
                // City stop-and-go: 0–60 km/h
                val s = ((sin(phase) * 0.5 + 0.5) * 60).roundToInt().coerceIn(0, 60)
                s to if (s == 0) Gear.P else if (s < 8) Gear.N else Gear.D
            }
            scenarioTick < 150 -> {
                // Highway cruise: 80–140 km/h
                val s = (80 + (sin(phase * 0.5) * 0.5 + 0.5) * 60).roundToInt().coerceIn(80, 140)
                s to Gear.D
            }
            scenarioTick < 200 -> {
                // Hard acceleration: 0–180 km/h
                val s = ((scenarioTick - 150).toFloat() / 50f * 180f).roundToInt().coerceIn(0, 180)
                s to if (s < 8) Gear.N else Gear.D
            }
            scenarioTick < 250 -> {
                // Reverse parking
                val s = ((sin(phase * 2) * 0.5 + 0.5) * 6).roundToInt().coerceIn(0, 6)
                s to Gear.R
            }
            else -> 0 to Gear.P  // parked idle
        }

        val rpmX1000 = when (gear) {
            Gear.P, Gear.N -> (0.8f + sin(phase * 0.7).toFloat() * 0.15f).coerceIn(0.6f, 1.0f)
            Gear.R         -> (1.2f + sin(phase).toFloat() * 0.2f).coerceIn(0.8f, 1.5f)
            Gear.D         -> (speed / 32f + sin(phase * 2).toFloat() * 0.15f).coerceIn(0.8f, 7.5f)
        }

        // Fuel drains slowly; auto-refuel at 10%
        val fuel = when {
            cur.fuelPercent <= 10          -> 100
            speed > 20 && tick % 30 == 0L -> (cur.fuelPercent - 1).coerceAtLeast(10)
            else                           -> cur.fuelPercent
        }

        val coolant = (40 + speed * 0.28f).roundToInt().coerceIn(35, 98)
        val odo     = cur.odometerKm + if (speed > 0 && tick % 18 == 0L) 1 else 0

        return cur.copy(
            speedKmh           = speed,
            rpmX1000           = rpmX1000,
            gear               = gear,
            fuelPercent        = fuel,
            fuelWarning        = true,
            coolantTempC       = coolant,
            odometerKm         = odo,
            absFault           = true,
            engineWarning      = true,
            batteryWarning     = true,
            oilWarning         = true,
            brakeWarning       = true,
            seatbeltWarning    = true,
            tpmsWarning        = true,
            parkingLightsOn    = gear == Gear.P && speed == 0,
            lowBeamOn          = speed > 0,
            highBeamOn         = speed > 110,
            // turn signals alternate: left first half, right second half of cycle
            turnSignalLeft     = scenarioTick in 0..29 || scenarioTick in 60..89,
            turnSignalRight    = scenarioTick in 30..59 || scenarioTick in 90..119,
        )
    }
}
