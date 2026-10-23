package com.autobox.cluster.model

enum class Gear(val label: String) {
    P("P"),
    R("R"),
    N("N"),
    D("D"),
}

enum class DriveMode(val vhalValue: Int) {
    ECO(0),
    NORMAL(1),
    SPORT(2);

    companion object {
        /** Maps a VENDOR_DRIVE_MODE value, null if unknown. */
        fun fromVhalValue(value: Int): DriveMode? = entries.firstOrNull { it.vhalValue == value }
    }
}

enum class GaugeStyle(val prefValue: String) {
    ROUND("round"), // two round dials, speedometer left, tachometer right
    ARC("arc");     // vertical arc scales on the screen edges with large readouts

    companion object {
        /** Maps a stored or adb-supplied value, null if unknown. */
        fun fromPrefValue(value: String?): GaugeStyle? = entries.firstOrNull { it.prefValue == value }
    }
}

data class ClusterData(
    val speedKmh: Int = 0,
    val rpmX1000: Float = 0f,
    val gear: Gear = Gear.P,
    val fuelPercent: Int = 0,
    val coolantTempC: Int = 0,
    val odometerKm: Int = 0,
    val rangeKm: Int? = null,         // RANGE_REMAINING; null until the VHAL reports it
    val outsideTempC: Int? = null,    // ENV_OUTSIDE_TEMPERATURE; null until the VHAL reports it
    val speedLimitKmh: Int? = null,   // VENDOR_SPEED_LIMIT; null when there is no limit
    val driveMode: DriveMode = DriveMode.ECO, // VENDOR_DRIVE_MODE
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
