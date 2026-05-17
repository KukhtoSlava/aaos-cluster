package com.autobox.cluster.repository.constants

// Vendor property: ABS system fault requiring service (0x21400002).
// Distinct from ABS_ACTIVE which only signals ABS engaging during braking.
internal const val VENDOR_ABS_FAULT = 0x21400002

// Vendor property: driver seatbelt status (0x21400003).
// 0 = buckled (no warning), 1 = unbuckled (seatbeltWarning active).
// Replaces SEAT_BELT_BUCKLED which uses per-seat area IDs that are unreliable
// with subscribePropertyEvents when no area ID is explicitly specified.
internal const val VENDOR_SEATBELT_DRIVER = 0x21400003

// Vendor property: battery/charging system fault (0x21400004).
// 0 = no fault, 1 = battery warning active.
internal const val VENDOR_BATTERY_FAULT = 0x21400004
