package com.autobox.cluster.repository.constants

import com.autobox.cluster.model.DriveMode

internal const val LOW_FUEL_PERCENT = 15
internal const val ENGINE_WARNING_TEMP_C = 110
internal const val DEFAULT_FUEL_CAPACITY_ML = 15_000f
internal const val TIRE_PRESSURE_WARNING_RATIO = 0.8f
internal const val DEFAULT_TIRE_PRESSURE_MAX_KPA = 225f

// The emulator VHAL has no speed limit or drive mode property. These mock
// values stand in until vendor properties for them are defined.
internal const val MOCK_SPEED_LIMIT_KMH = 50
internal val MOCK_DRIVE_MODE = DriveMode.ECO
