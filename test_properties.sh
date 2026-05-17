#!/usr/bin/env bash
# test_properties.sh - VHAL walkthrough for the Cluster system app.
#
# Usage:
#   chmod +x test_properties.sh
#   ./test_properties.sh            # full UI walkthrough
#   ./test_properties.sh drive      # speed, gear, rpm
#   ./test_properties.sh engine     # coolant temperature and engine warning
#   ./test_properties.sh fuel       # fuel bar and fuel warning
#   ./test_properties.sh odometer   # odometer
#   ./test_properties.sh telltales  # UI telltales and lights
#   ./test_properties.sh check      # only VHAL configs used by this UI

set -euo pipefail

CAR="adb shell cmd car_service"

inject() { $CAR inject-vhal-event "$@"; }
get_prop() { $CAR get-property-value "$@"; }
inject_silent() { inject "$@" >/dev/null 2>&1 || true; }

readback() {
    local prop="$1"
    if [ "$#" -eq 3 ]; then
        echo "   readback: ${prop} area ${2}"
        get_prop "$prop" "$2"
    else
        echo "   readback: ${prop}"
        get_prop "$prop"
    fi
}

inject_and_read() {
    echo "   inject: $*"
    if ! inject "$@"; then
        echo "   inject failed"
        return 0
    fi
    if ! readback "$@"; then
        echo "   readback failed"
    fi
}

check_prop() {
    local prop="$1"
    echo ""
    echo "-- ${prop} --"
    if $CAR get-carpropertyconfig "$prop"; then
        readback "$prop" || echo "   current value is not available"
    else
        echo "   not configured in this VHAL"
    fi
}

# For properties that require an area ID (per-seat, per-wheel, etc.)
check_prop_area() {
    local prop="$1"
    shift
    local areas=("$@")
    echo ""
    echo "-- ${prop} (areas: ${areas[*]}) --"
    if $CAR get-carpropertyconfig "$prop"; then
        for area in "${areas[@]}"; do
            echo "   area ${area}:"
            get_prop "$prop" "$area" || echo "   area ${area}: not available"
        done
    else
        echo "   not configured in this VHAL"
    fi
}

STEP=0
step() {
    STEP=$((STEP + 1))
    echo ""
    echo "-- Step $STEP: $* --"
}

pause() {
    local secs=${1:-3}
    echo "   waiting ${secs}s..."
    sleep "$secs"
}

header() {
    echo ""
    echo "========================================"
    echo "  $*"
    echo "========================================"
}

section_drive() {
    header "DRIVE - speed, gear, rpm"

    step "Park, stopped"
    inject_and_read GEAR_SELECTION 4
    inject_and_read PERF_VEHICLE_SPEED 0.0
    inject_and_read ENGINE_RPM 800
    pause 3

    step "Drive, 30 km/h, 1800 rpm"
    inject_and_read GEAR_SELECTION 8
    inject_and_read PERF_VEHICLE_SPEED 8.33
    inject_and_read ENGINE_RPM 1800
    pause 3

    step "Drive, 90 km/h, 2800 rpm"
    inject_and_read PERF_VEHICLE_SPEED 25.0
    inject_and_read ENGINE_RPM 2800
    pause 3

    step "Reverse"
    inject_and_read GEAR_SELECTION 2
    inject_and_read PERF_VEHICLE_SPEED 1.4
    inject_and_read ENGINE_RPM 1200
    pause 3

    step "Neutral"
    inject_and_read GEAR_SELECTION 1
    inject_and_read PERF_VEHICLE_SPEED 0.0
    inject_and_read ENGINE_RPM 900
    pause 3

    step "Back to Park"
    inject_and_read GEAR_SELECTION 4
    inject_and_read PERF_VEHICLE_SPEED 0.0
    inject_and_read ENGINE_RPM 800
    pause 2
}

section_engine() {
    header "ENGINE - coolant temperature and engine warning"

    step "Cold coolant, blue temperature bar"
    inject_and_read ENGINE_COOLANT_TEMP 35
    pause 3

    step "Normal coolant, green temperature bar"
    inject_and_read ENGINE_COOLANT_TEMP 75
    pause 3

    step "High coolant, red temperature bar and engine warning"
    inject_and_read ENGINE_COOLANT_TEMP 110
    pause 3

    step "Reset coolant"
    inject_and_read ENGINE_COOLANT_TEMP 75
    pause 2
}

section_fuel() {
    header "FUEL - bar and warning"
    echo "Assumes emulator INFO_FUEL_CAPACITY is 15000 mL."

    step "Full tank, green fuel bar"
    inject_and_read FUEL_LEVEL 15000
    pause 3

    step "72 percent"
    inject_and_read FUEL_LEVEL 10800
    pause 3

    step "Low fuel by level, red fuel bar and warning"
    inject_and_read FUEL_LEVEL 1500
    pause 3

    step "Reset fuel"
    inject_and_read FUEL_LEVEL 10800
    pause 2
}

section_odometer() {
    header "ODOMETER"

    step "12 345 km"
    inject_and_read PERF_ODOMETER 12345
    pause 3

    step "85 432 km"
    inject_and_read PERF_ODOMETER 85432
    pause 3

    step "Reset odometer"
    inject_and_read PERF_ODOMETER 12345
    pause 2
}

section_telltales() {
    header "TELLTALES - only indicators visible in Cluster UI"

    # 0x21400002 = VENDOR_ABS_FAULT: persistent ABS system fault (requires service).
    # ABS_ACTIVE (standard) only signals transient ABS engagement during braking.
    step "ABS fault (vendor property 0x21400002)"
    inject_silent 0x21400002 0
    inject_and_read 0x21400002 1
    pause 3
    inject_and_read 0x21400002 0
    pause 2

    # 0x21400003 = VENDOR_SEATBELT_DRIVER: 0 = buckled (no warning), 1 = unbuckled (warning).
    step "Seat belt warning (vendor property 0x21400003)"
    inject_silent 0x21400003 0
    inject_and_read 0x21400003 1
    pause 3
    inject_and_read 0x21400003 0
    pause 2

    step "Parking brake warning"
    inject_silent PARKING_BRAKE_ON 0
    inject_and_read PARKING_BRAKE_ON 1
    pause 3
    inject_and_read PARKING_BRAKE_ON 0
    pause 2

    step "Oil warning"
    inject_silent ENGINE_OIL_LEVEL 2
    inject_and_read ENGINE_OIL_LEVEL 1
    pause 3
    inject_and_read ENGINE_OIL_LEVEL 2
    pause 2

    # Cluster shows TPMS when any wheel is below its configured min pressure.
    # Fallback: 80% of configured max (default 225 kPa -> 180 kPa).
    step "TPMS warning -- left front below threshold"
    inject_silent TIRE_PRESSURE 0x1 250.0
    inject_silent TIRE_PRESSURE 0x2 250.0
    inject_silent TIRE_PRESSURE 0x4 250.0
    inject_silent TIRE_PRESSURE 0x8 250.0
    inject_and_read TIRE_PRESSURE 0x1 160.0
    pause 3
    inject_and_read TIRE_PRESSURE 0x1 250.0
    pause 2

    # 0x21400004 = VENDOR_BATTERY_FAULT: battery/charging system fault.
    step "Battery warning (vendor property 0x21400004)"
    inject_silent 0x21400004 0
    inject_and_read 0x21400004 1
    pause 3
    inject_and_read 0x21400004 0
    pause 2

    step "Parking lights"
    inject_silent HEADLIGHTS_STATE 0
    inject_and_read HEADLIGHTS_STATE 2
    pause 3

    step "Low beam"
    inject_and_read HEADLIGHTS_STATE 1
    pause 3

    step "High beam"
    inject_and_read HIGH_BEAM_LIGHTS_STATE 1
    pause 3
    inject_and_read HIGH_BEAM_LIGHTS_STATE 0
    pause 2

    step "Turn signals"
    inject_silent TURN_SIGNAL_LIGHT_STATE 0
    inject_and_read TURN_SIGNAL_LIGHT_STATE 2
    pause 3
    inject_and_read TURN_SIGNAL_LIGHT_STATE 1
    pause 3
    inject_and_read TURN_SIGNAL_LIGHT_STATE 3
    pause 3
    inject_and_read TURN_SIGNAL_LIGHT_STATE 0
    pause 2

    step "Lights off"
    inject_and_read HEADLIGHTS_STATE 0
    pause 2
}

section_check() {
    header "CHECK - only VHAL properties used by Cluster UI"

    check_prop PERF_VEHICLE_SPEED
    check_prop ENGINE_RPM
    check_prop GEAR_SELECTION
    check_prop ENGINE_COOLANT_TEMP
    check_prop PERF_ODOMETER
    check_prop FUEL_LEVEL
    check_prop INFO_FUEL_CAPACITY
    check_prop ABS_ACTIVE
    check_prop 0x21400002   # VENDOR_ABS_FAULT
    check_prop 0x21400003   # VENDOR_SEATBELT_DRIVER
    check_prop 0x21400004   # VENDOR_BATTERY_FAULT
    check_prop PARKING_BRAKE_ON
    check_prop ENGINE_OIL_LEVEL
    check_prop_area TIRE_PRESSURE 0x1 0x2 0x4 0x8   # FL FR RL RR
    check_prop HEADLIGHTS_STATE
    check_prop HIGH_BEAM_LIGHTS_STATE
    check_prop TURN_SIGNAL_LIGHT_STATE
}

SECTION="${1:-all}"

case "$SECTION" in
    drive) section_drive ;;
    engine) section_engine ;;
    fuel) section_fuel ;;
    odometer) section_odometer ;;
    telltales) section_telltales ;;
    check) section_check ;;
    all)
        section_drive
        section_engine
        section_fuel
        section_odometer
        section_telltales
        ;;
    *)
        echo "Usage: $0 [all|drive|engine|fuel|odometer|telltales|check]"
        exit 1
        ;;
esac

echo ""
echo "Done - $SECTION walkthrough complete."
