package com.autobox.cluster.model

enum class Gear(val label: String) {
    P("P"),
    R("R"),
    N("N"),
    D("D"),
}

data class ClusterData(
    val speedKmh: Int = 0,
    val rpmX1000: Float = 0f,
    val gear: Gear = Gear.P,
    val fuelPercent: Int = 72,
    val coolantTempC: Int = 45,
    val odometerKm: Int = 123,
    // Telltales
    val absEngaged: Boolean = false,  // ABS_ACTIVE: ABS is currently braking (transient)
    val absFault: Boolean = false,    // VENDOR_ABS_FAULT: ABS system fault (persistent)
    val engineWarning: Boolean = false,
    val batteryWarning: Boolean = false,
    val oilWarning: Boolean = false,
    val fuelWarning: Boolean = false,
    val brakeWarning: Boolean = false,
    val seatbeltWarning: Boolean = false,
    val tpmsWarning: Boolean = false,
    // Lights
    val parkingLightsOn: Boolean = false,
    val lowBeamOn: Boolean = false,
    val highBeamOn: Boolean = false,
    // Turn signals
    val turnSignalLeft: Boolean = false,
    val turnSignalRight: Boolean = false,
) {
    val absActive: Boolean get() = absEngaged || absFault
}
