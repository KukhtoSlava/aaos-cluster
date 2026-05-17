package com.autobox.cluster.repository.impl

import android.car.Car
import android.car.VehicleAreaWheel
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.Subscription
import android.content.Context
import android.util.Log
import com.autobox.cluster.model.ClusterData
import com.autobox.cluster.model.Gear
import com.autobox.cluster.repository.ClusterRepository
import com.autobox.cluster.repository.constants.DEFAULT_FUEL_CAPACITY_ML
import com.autobox.cluster.repository.constants.DEFAULT_TIRE_PRESSURE_MAX_KPA
import com.autobox.cluster.repository.constants.ENGINE_WARNING_TEMP_C
import com.autobox.cluster.repository.constants.LOW_FUEL_PERCENT
import com.autobox.cluster.repository.constants.TIRE_PRESSURE_WARNING_RATIO
import com.autobox.cluster.repository.constants.VENDOR_ABS_FAULT
import com.autobox.cluster.repository.constants.VENDOR_BATTERY_FAULT
import com.autobox.cluster.repository.constants.VENDOR_SEATBELT_DRIVER
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class ClusterRepositoryImpl @Inject constructor(
    private val context: Context,
) : ClusterRepository {

    private val _data = MutableStateFlow(ClusterData())
    override val data: StateFlow<ClusterData> = _data.asStateFlow()

    private val lock = Any()

    // Guarded by lock
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    // Read once on connect to compute fuelPercent from FUEL_LEVEL (mL)
    @Volatile private var fuelCapacityMl = 0f
    @Volatile private var batteryFaultSignal = false
    private val tirePressureKpa = mutableMapOf<Int, Float>()
    private val tirePressureThresholdKpa = mutableMapOf<Int, Float>()

    private val lifecycleListener = Car.CarServiceLifecycleListener { car, ready ->
        if (ready) {
            val cpm = car.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            synchronized(lock) { propertyManager = cpm }
            readStaticProperties(cpm)
            registerCallbacks(cpm)
            readInitialPropertyValues(cpm)
            Log.i(TAG, "Car service connected")
        } else {
            Log.w(TAG, "Car service disconnected")
            synchronized(lock) { propertyManager = null }
        }
    }

    private fun readInitialPropertyValues(cpm: CarPropertyManager?) {
        cpm ?: return
        SUBSCRIPTIONS.forEach { subscription ->
            val propId = subscription.propertyId
            try {
                val config = cpm.getCarPropertyConfig(propId)
                val areaIds = config?.areaIds?.takeIf { it.isNotEmpty() } ?: intArrayOf(0)
                areaIds.forEach { areaId ->
                    try {
                        val value = cpm.getProperty<Any>(propId, areaId)
                        if (value?.propertyStatus == CarPropertyValue.STATUS_AVAILABLE) {
                            handleEvent(value)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not read initial value for propId=0x${propId.toString(16)} area=$areaId", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not inspect config for propId=0x${propId.toString(16)}", e)
            }
        }
    }

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.propertyStatus == CarPropertyValue.STATUS_AVAILABLE) {
                handleEvent(value)
            }
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w(TAG, "Property error: propId=0x${propId.toString(16)} zone=$zone")
        }
    }

    override fun connect() {
        synchronized(lock) {
            if (car != null) return
            car = Car.createCar(
                context,
                /* handler= */ null,
                Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
                lifecycleListener,
            )
        }
    }

    override fun disconnect() {
        val carToDisconnect: Car?
        val cpmToUnregister: CarPropertyManager?
        synchronized(lock) {
            carToDisconnect = car
            cpmToUnregister = propertyManager
            car = null
            propertyManager = null
        }
        try { cpmToUnregister?.unsubscribePropertyEvents(propertyCallback) } catch (e: Exception) {
            Log.e(TAG, "unsubscribePropertyEvents failed", e)
        }
        carToDisconnect?.disconnect()
    }

    private fun readStaticProperties(cpm: CarPropertyManager?) {
        cpm ?: return
        try {
            fuelCapacityMl = cpm.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, /* areaId= */ 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read INFO_FUEL_CAPACITY", e)
        }
        readTirePressureThresholds(cpm)
    }

    private fun readTirePressureThresholds(cpm: CarPropertyManager) {
        WHEEL_AREA_IDS.forEach { areaId ->
            try {
                val range = cpm.getMinMaxSupportedValue<Float>(
                    VehiclePropertyIds.TIRE_PRESSURE,
                    areaId,
                )
                val thresholdKpa = range.minValue
                    ?: range.maxValue?.let { it * TIRE_PRESSURE_WARNING_RATIO }
                    ?: (DEFAULT_TIRE_PRESSURE_MAX_KPA * TIRE_PRESSURE_WARNING_RATIO)
                synchronized(lock) { tirePressureThresholdKpa[areaId] = thresholdKpa }
                Log.i(TAG, "TIRE_PRESSURE config area=${wheelAreaName(areaId)} threshold=${thresholdKpa}kPa")
            } catch (e: Exception) {
                val fallback = DEFAULT_TIRE_PRESSURE_MAX_KPA * TIRE_PRESSURE_WARNING_RATIO
                synchronized(lock) { tirePressureThresholdKpa[areaId] = fallback }
                Log.w(TAG, "Could not read TIRE_PRESSURE config for area=${wheelAreaName(areaId)}, fallback threshold=${fallback}kPa", e)
            }
        }
    }

    private fun registerCallbacks(cpm: CarPropertyManager?) {
        cpm ?: return
        SUBSCRIPTIONS.forEach { subscription ->
            try {
                cpm.subscribePropertyEvents(
                    listOf(subscription),
                    /* callbackExecutor= */ null,
                    propertyCallback,
                )
            } catch (e: Exception) {
                Log.e(TAG, "subscribePropertyEvents failed for propId=0x${subscription.propertyId.toString(16)}", e)
            }
        }
    }

    private fun handleEvent(value: CarPropertyValue<*>) {
        val v = value.value
        when (value.propertyId) {
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                val speedKmh = abs((v as? Float ?: return) * 3.6f).toInt()
                _data.update { it.copy(speedKmh = speedKmh) }
            }
            VehiclePropertyIds.ENGINE_RPM -> {
                val rpmX1000 = ((v as? Float ?: return) / 1000f).coerceAtLeast(0f)
                _data.update { it.copy(rpmX1000 = rpmX1000) }
            }
            VehiclePropertyIds.GEAR_SELECTION -> {
                val gear = vehicleGearToGear(v as? Int ?: return)
                _data.update { it.copy(gear = gear) }
            }
            VehiclePropertyIds.ENGINE_COOLANT_TEMP -> {
                val tempC = (v as? Float ?: return).toInt()
                _data.update {
                    it.copy(
                        coolantTempC = tempC,
                        engineWarning = tempC >= ENGINE_WARNING_TEMP_C,
                    )
                }
            }
            VehiclePropertyIds.PERF_ODOMETER -> {
                _data.update { it.copy(odometerKm = (v as? Float ?: return).toInt()) }
            }
            VehiclePropertyIds.FUEL_LEVEL -> {
                val ml = v as? Float ?: return
                if (fuelCapacityMl == 0f) {
                    val cpm = synchronized(lock) { propertyManager }
                    try { fuelCapacityMl = cpm?.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, 0) ?: 0f }
                    catch (e: Exception) { Log.w(TAG, "INFO_FUEL_CAPACITY retry failed", e) }
                }
                val capacity = fuelCapacityMl.takeIf { it > 0f } ?: DEFAULT_FUEL_CAPACITY_ML
                if (capacity > 0f) {
                    val pct = ((ml / capacity) * 100f).toInt().coerceIn(0, 100)
                    _data.update {
                        it.copy(
                            fuelPercent = pct,
                            fuelWarning = pct <= LOW_FUEL_PERCENT,
                        )
                    }
                }
            }
            VENDOR_BATTERY_FAULT -> {
                batteryFaultSignal = booleanValue(v)
                Log.i(TAG, "VENDOR_BATTERY_FAULT event value=$v fault=$batteryFaultSignal")
                updateBatteryWarning()
            }
            VehiclePropertyIds.ABS_ACTIVE -> {
                _data.update { it.copy(absEngaged = booleanValue(v)) }
            }
            VENDOR_ABS_FAULT -> {
                _data.update { it.copy(absFault = booleanValue(v)) }
            }
            VehiclePropertyIds.PARKING_BRAKE_ON -> {
                _data.update { it.copy(brakeWarning = booleanValue(v)) }
            }
            VENDOR_SEATBELT_DRIVER -> {
                _data.update { it.copy(seatbeltWarning = booleanValue(v)) }
            }
            VehiclePropertyIds.ENGINE_OIL_LEVEL -> {
                val oilLevel = intValue(v) ?: return
                _data.update { it.copy(oilWarning = oilLevel != OIL_LEVEL_NORMAL) }
            }
            VehiclePropertyIds.HEADLIGHTS_STATE -> {
                when (intValue(v) ?: return) {
                    LIGHT_STATE_ON -> _data.update {
                        it.copy(
                            lowBeamOn = true,
                            parkingLightsOn = false,
                        )
                    }
                    LIGHT_STATE_DAYTIME_RUNNING -> _data.update {
                        it.copy(
                            lowBeamOn = false,
                            parkingLightsOn = true,
                        )
                    }
                    else -> _data.update {
                        it.copy(
                            lowBeamOn = false,
                            parkingLightsOn = false,
                        )
                    }
                }
            }
            VehiclePropertyIds.HIGH_BEAM_LIGHTS_STATE -> {
                val lightsOn = (intValue(v) ?: return) != LIGHT_STATE_OFF
                _data.update { it.copy(highBeamOn = lightsOn) }
            }
            VehiclePropertyIds.TURN_SIGNAL_LIGHT_STATE -> {
                val turnSignal = intValue(v) ?: return
                _data.update {
                    it.copy(
                        turnSignalLeft = (turnSignal and TURN_SIGNAL_LEFT) != 0,
                        turnSignalRight = (turnSignal and TURN_SIGNAL_RIGHT) != 0,
                    )
                }
            }
            VehiclePropertyIds.TIRE_PRESSURE -> {
                val pressureKpa = v as? Float ?: return
                synchronized(lock) { tirePressureKpa[value.areaId] = pressureKpa }
                val tpmsWarning = hasLowTirePressure()
                Log.i(TAG, "TIRE_PRESSURE event area=${wheelAreaName(value.areaId)} pressure=${pressureKpa}kPa threshold=${tirePressureThreshold(value.areaId)}kPa tpmsWarning=$tpmsWarning all=${tirePressureSnapshot()}")
                _data.update { it.copy(tpmsWarning = tpmsWarning) }
            }
        }
    }

    private fun updateBatteryWarning() {
        _data.update { it.copy(batteryWarning = batteryFaultSignal) }
    }

    private fun booleanValue(value: Any?): Boolean = when (value) {
        is Boolean -> value
        is Int -> value != 0
        is Float -> value != 0f
        is String -> value == "1" || value.equals("true", ignoreCase = true)
        else -> false
    }

    private fun intValue(value: Any?): Int? = when (value) {
        is Int -> value
        is Boolean -> if (value) 1 else 0
        is Float -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    private fun hasLowTirePressure(): Boolean = synchronized(lock) {
        WHEEL_AREA_IDS.any { areaId ->
            val pressureKpa = tirePressureKpa[areaId] ?: return@any false
            val threshold = tirePressureThresholdKpa[areaId] ?: return@any false
            pressureKpa < threshold
        }
    }

    private fun tirePressureThreshold(areaId: Int): Float = synchronized(lock) {
        tirePressureThresholdKpa[areaId] ?: DEFAULT_TIRE_PRESSURE_MAX_KPA * TIRE_PRESSURE_WARNING_RATIO
    }

    private fun tirePressureSnapshot(): String = synchronized(lock) {
        WHEEL_AREA_IDS.joinToString(prefix = "[", postfix = "]") { areaId ->
            val threshold = tirePressureThresholdKpa[areaId] ?: DEFAULT_TIRE_PRESSURE_MAX_KPA * TIRE_PRESSURE_WARNING_RATIO
            "${wheelAreaName(areaId)}=${tirePressureKpa[areaId] ?: "n/a"}<${threshold}"
        }
    }

    private fun wheelAreaName(areaId: Int): String = when (areaId) {
        VehicleAreaWheel.WHEEL_LEFT_FRONT -> "FL(0x1)"
        VehicleAreaWheel.WHEEL_RIGHT_FRONT -> "FR(0x2)"
        VehicleAreaWheel.WHEEL_LEFT_REAR -> "RL(0x4)"
        VehicleAreaWheel.WHEEL_RIGHT_REAR -> "RR(0x8)"
        else -> "0x${areaId.toString(16)}"
    }

    private fun vehicleGearToGear(vehicleGear: Int): Gear = when (vehicleGear) {
        VehicleGear.GEAR_PARK -> Gear.P
        VehicleGear.GEAR_REVERSE -> Gear.R
        VehicleGear.GEAR_NEUTRAL -> Gear.N
        else -> Gear.D
    }

    companion object {
        private const val TAG = "ClusterRepository"
        private const val LIGHT_STATE_OFF = 0
        private const val LIGHT_STATE_ON = 1
        private const val LIGHT_STATE_DAYTIME_RUNNING = 2
        private const val OIL_LEVEL_NORMAL = 2
        private const val TURN_SIGNAL_RIGHT = 1
        private const val TURN_SIGNAL_LEFT = 2
        private val WHEEL_AREA_IDS = listOf(
            VehicleAreaWheel.WHEEL_LEFT_FRONT,
            VehicleAreaWheel.WHEEL_RIGHT_FRONT,
            VehicleAreaWheel.WHEEL_LEFT_REAR,
            VehicleAreaWheel.WHEEL_RIGHT_REAR,
        )

        private val SUBSCRIPTIONS = listOf(
            Subscription.Builder(VehiclePropertyIds.PERF_VEHICLE_SPEED).setUpdateRateUi().build(),
            Subscription.Builder(VehiclePropertyIds.ENGINE_RPM).setUpdateRateUi().build(),
            Subscription.Builder(VehiclePropertyIds.GEAR_SELECTION).build(),
            Subscription.Builder(VehiclePropertyIds.ENGINE_COOLANT_TEMP).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.ENGINE_OIL_LEVEL).build(),
            Subscription.Builder(VehiclePropertyIds.PERF_ODOMETER).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.FUEL_LEVEL).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.ABS_ACTIVE).build(),
            Subscription.Builder(VENDOR_ABS_FAULT).build(),
            Subscription.Builder(VENDOR_SEATBELT_DRIVER).build(),
            Subscription.Builder(VENDOR_BATTERY_FAULT).build(),
            Subscription.Builder(VehiclePropertyIds.PARKING_BRAKE_ON).build(),
            Subscription.Builder(VehiclePropertyIds.HEADLIGHTS_STATE).build(),
            Subscription.Builder(VehiclePropertyIds.HIGH_BEAM_LIGHTS_STATE).build(),
            Subscription.Builder(VehiclePropertyIds.TURN_SIGNAL_LIGHT_STATE).build(),
            // Subscribe to all wheel areas explicitly so TPMS reacts to any tire.
            Subscription.Builder(VehiclePropertyIds.TIRE_PRESSURE)
                .setUpdateRateNormal()
                .addAreaId(VehicleAreaWheel.WHEEL_LEFT_FRONT)
                .addAreaId(VehicleAreaWheel.WHEEL_RIGHT_FRONT)
                .addAreaId(VehicleAreaWheel.WHEEL_LEFT_REAR)
                .addAreaId(VehicleAreaWheel.WHEEL_RIGHT_REAR)
                .build(),
        )
    }
}
