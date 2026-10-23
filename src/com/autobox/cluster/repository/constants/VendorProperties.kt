package com.autobox.cluster.repository.constants

// Vendor properties from vendor/autobox/vhal/AutoBoxProperties.json. Read-only:
// the vehicle reports them, Android only displays them.

// ABS system fault requiring service (0x21400002).
// Distinct from ABS_ACTIVE which only signals ABS engaging during braking.
internal const val VENDOR_ABS_FAULT = 0x21400002

// Battery/charging system fault (0x21400004).
// 0 = no fault, 1 = battery warning active.
internal const val VENDOR_BATTERY_FAULT = 0x21400004

// Current speed limit in km/h (0x21400005).
// 0 = no limit known, the speed limit sign is hidden.
internal const val VENDOR_SPEED_LIMIT = 0x21400005

// Drive mode (0x21400006): DriveMode.vhalValue, 0 = ECO, 1 = NORMAL, 2 = SPORT.
internal const val VENDOR_DRIVE_MODE = 0x21400006
