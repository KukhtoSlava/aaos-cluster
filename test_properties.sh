#!/usr/bin/env bash
# test_properties.sh - VHAL walkthrough for the Cluster system app.
#
# Sections follow the UI, from the largest elements to the smallest.
#
# Usage:
#   chmod +x test_properties.sh
#   ./test_properties.sh            # full UI walkthrough
#   ./test_properties.sh style      # gauge layout: round dials / arc scales
#   ./test_properties.sh drive      # speed, gear, rpm
#   ./test_properties.sh fuel       # fuel bar, range and fuel warning
#   ./test_properties.sh engine     # coolant temperature and engine warning
#   ./test_properties.sh limit      # speed limit sign
#   ./test_properties.sh mode       # drive mode in the top bar
#   ./test_properties.sh ambient    # outside temperature in the top bar
#   ./test_properties.sh telltales  # telltales and lights
#   ./test_properties.sh odometer   # odometer in the bottom bar
#   ./test_properties.sh check      # VHAL configs and current values used by this UI
#
# Values are injected with `cmd car_service inject-vhal-event`: CarService
# delivers them to the app as property events, but the VHAL itself keeps its
# stored value, so get-property-value does not read the injected value back.
# BOOLEAN properties take true/false. Continuous properties (speed, rpm, fuel,
# tyre pressure...) pass at most one event per subscription period, so the
# script waits between two values of the same property.
# The gauge style is an app setting, not a VHAL property: the script switches
# it with a broadcast to the cluster app.

set -euo pipefail

CAR="adb shell cmd car_service"

# Vendor properties (vendor/autobox/vhal/AutoBoxProperties.json)
VENDOR_ABS_FAULT=0x21400002      # 0 = no fault, 1 = ABS fault
VENDOR_BATTERY_FAULT=0x21400004  # 0 = no fault, 1 = battery warning
VENDOR_SPEED_LIMIT=0x21400005    # km/h, 0 = no limit sign
VENDOR_DRIVE_MODE=0x21400006     # 0 = ECO, 1 = NORMAL, 2 = SPORT

# Driver seat area for SEAT_BELT_BUCKLED: the emulator's INFO_DRIVER_SEAT is
# SEAT_1_LEFT.
DRIVER_SEAT=0x1

# Failed injections and unconfigured properties. The walkthrough goes on,
# but the script exits non-zero at the end.
FAILED=0
MISSING=0

inject() {
    echo "   inject: $*"
    if ! $CAR inject-vhal-event "$@" >/dev/null; then
        echo "   inject failed"
        FAILED=$((FAILED + 1))
    fi
}

# Resets before a step: no output, but a failure still counts.
inject_silent() { $CAR inject-vhal-event "$@" >/dev/null 2>&1 || FAILED=$((FAILED + 1)); }

# The cluster shows PERF_VEHICLE_SPEED_DISPLAY; CarService derives the
# driving state from PERF_VEHICLE_SPEED. Set both, in m/s.
speed() {
    inject PERF_VEHICLE_SPEED "$1"
    inject PERF_VEHICLE_SPEED_DISPLAY "$1"
}

# Gauge style app setting: round or arc
gauge_style() {
    echo "   gauge style: $1"
    if ! adb shell am broadcast --user 0 \
            -n com.autobox.cluster/.settings.GaugeStyleReceiver \
            -a com.autobox.cluster.action.SET_GAUGE_STYLE --es style "$1" >/dev/null; then
        echo "   broadcast failed"
        FAILED=$((FAILED + 1))
    fi
}

check_prop() {
    local prop="$1" name="${2:-$1}"
    echo ""
    echo "-- ${name} --"
    local config
    config=$($CAR get-carpropertyconfig "$prop" 2>&1 || true)
    if [[ "$config" != *"Property:"* ]]; then
        echo "   not configured in this VHAL"
        MISSING=$((MISSING + 1))
        return
    fi
    echo "$config"
    $CAR get-property-value "$prop" || echo "   current value is not available"
}

# For properties that require an area ID (per-seat, per-wheel, etc.)
check_prop_area() {
    local prop="$1"
    shift
    local areas=("$@")
    echo ""
    echo "-- ${prop} (areas: ${areas[*]}) --"
    local config
    config=$($CAR get-carpropertyconfig "$prop" 2>&1 || true)
    if [[ "$config" != *"Property:"* ]]; then
        echo "   not configured in this VHAL"
        MISSING=$((MISSING + 1))
        return
    fi
    echo "$config"
    for area in "${areas[@]}"; do
        echo "   area ${area}:"
        $CAR get-property-value "$prop" "$area" || echo "   area ${area}: not available"
    done
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

section_style() {
    header "STYLE - gauge layout"

    step "Arc scales on the screen edges"
    gauge_style arc
    pause 3

    step "Round dials (default)"
    gauge_style round
    pause 2
}

section_drive() {
    header "DRIVE - speed, gear, rpm"

    step "Park, stopped"
    inject GEAR_SELECTION 4
    speed 0.0
    inject ENGINE_RPM 800
    pause 3

    step "Drive, 30 km/h, 1800 rpm"
    inject GEAR_SELECTION 8
    speed 8.33
    inject ENGINE_RPM 1800
    pause 3

    step "Drive, 90 km/h, 2800 rpm - amber speed above the 50 km/h limit"
    speed 25.0
    inject ENGINE_RPM 2800
    pause 3

    step "Same in the arc layout"
    gauge_style arc
    pause 3
    gauge_style round

    step "Reverse"
    inject GEAR_SELECTION 2
    speed 1.4
    inject ENGINE_RPM 1200
    pause 3

    step "Neutral"
    inject GEAR_SELECTION 1
    speed 0.0
    inject ENGINE_RPM 900
    pause 3

    step "Back to Park"
    inject GEAR_SELECTION 4
    speed 0.0
    inject ENGINE_RPM 800
    pause 2
}

section_fuel() {
    header "FUEL - bar, range and warning"
    echo "Assumes emulator INFO_FUEL_CAPACITY is 15000 mL."

    step "Full tank, full fuel bar, 620 km range"
    inject FUEL_LEVEL 15000
    inject RANGE_REMAINING 620000
    pause 3

    step "72 percent, 482 km range"
    inject FUEL_LEVEL 10800
    inject RANGE_REMAINING 482000
    pause 3

    step "Low fuel, amber fuel bar and warning, 60 km range"
    inject FUEL_LEVEL 1500
    inject RANGE_REMAINING 60000
    pause 3

    step "Reset fuel"
    inject FUEL_LEVEL 10800
    inject RANGE_REMAINING 482000
    pause 2
}

section_engine() {
    header "ENGINE - coolant temperature and engine warning"

    step "Cold coolant, short coolant bar"
    inject ENGINE_COOLANT_TEMP 35
    pause 3

    step "Normal coolant"
    inject ENGINE_COOLANT_TEMP 75
    pause 3

    step "High coolant, red coolant bar and engine warning"
    inject ENGINE_COOLANT_TEMP 110
    pause 3

    step "Reset coolant"
    inject ENGINE_COOLANT_TEMP 75
    pause 2
}

section_limit() {
    header "LIMIT - speed limit sign (vendor property $VENDOR_SPEED_LIMIT)"

    step "City, 50 km/h"
    inject $VENDOR_SPEED_LIMIT 50
    pause 3

    step "Highway, 120 km/h"
    inject $VENDOR_SPEED_LIMIT 120
    pause 3

    step "No limit known - sign hidden"
    inject $VENDOR_SPEED_LIMIT 0
    pause 3

    step "Speed above the limit - amber speed"
    inject $VENDOR_SPEED_LIMIT 30
    inject GEAR_SELECTION 8
    speed 13.9
    pause 3

    step "Reset: Park, 50 km/h limit"
    speed 0.0
    inject GEAR_SELECTION 4
    inject $VENDOR_SPEED_LIMIT 50
    pause 2
}

section_mode() {
    header "MODE - drive mode (vendor property $VENDOR_DRIVE_MODE)"

    step "SPORT"
    inject $VENDOR_DRIVE_MODE 2
    pause 3

    step "NORMAL"
    inject $VENDOR_DRIVE_MODE 1
    pause 3

    step "ECO (default)"
    inject $VENDOR_DRIVE_MODE 0
    pause 2
}

section_ambient() {
    header "AMBIENT - outside temperature"

    step "-7 C"
    inject ENV_OUTSIDE_TEMPERATURE -7.0
    pause 3

    step "3 C"
    inject ENV_OUTSIDE_TEMPERATURE 3.0
    pause 3

    step "32 C"
    inject ENV_OUTSIDE_TEMPERATURE 32.0
    pause 3

    step "Reset to the default, 25 C"
    inject ENV_OUTSIDE_TEMPERATURE 25.0
    pause 2
}

section_telltales() {
    header "TELLTALES - indicators and lights"

    step "Turn signals"
    inject_silent TURN_SIGNAL_LIGHT_STATE 0
    inject TURN_SIGNAL_LIGHT_STATE 2
    pause 3
    inject TURN_SIGNAL_LIGHT_STATE 1
    pause 3
    inject TURN_SIGNAL_LIGHT_STATE 3
    pause 3
    inject TURN_SIGNAL_LIGHT_STATE 0
    pause 2

    step "Parking lights"
    inject_silent HEADLIGHTS_STATE 0
    inject HEADLIGHTS_STATE 2
    pause 3

    step "Low beam"
    inject HEADLIGHTS_STATE 1
    pause 3

    step "High beam"
    inject HIGH_BEAM_LIGHTS_STATE 1
    pause 3
    inject HIGH_BEAM_LIGHTS_STATE 0
    pause 2

    step "Lights off"
    inject HEADLIGHTS_STATE 0
    pause 2

    # BOOLEAN, per seat: true = buckled. The emulator default is unbuckled,
    # so buckle up first and let the warning go dark.
    step "Seat belt warning - driver unbuckled"
    inject SEAT_BELT_BUCKLED $DRIVER_SEAT true
    pause 2
    inject SEAT_BELT_BUCKLED $DRIVER_SEAT false
    pause 3
    inject SEAT_BELT_BUCKLED $DRIVER_SEAT true
    pause 2

    # BOOLEAN property: CarService parses the value with Boolean.parseBoolean,
    # so it takes true/false ("1" would be false). The emulator default is true,
    # so release the brake first and let the icon go dark.
    step "Parking brake warning"
    inject PARKING_BRAKE_ON false
    pause 2
    inject PARKING_BRAKE_ON true
    pause 3
    inject PARKING_BRAKE_ON false
    pause 2

    # VENDOR_ABS_FAULT: persistent ABS system fault (requires service).
    # ABS_ACTIVE (standard) only signals transient ABS engagement during braking.
    step "ABS fault (vendor property $VENDOR_ABS_FAULT)"
    inject_silent $VENDOR_ABS_FAULT 0
    inject $VENDOR_ABS_FAULT 1
    pause 3
    inject $VENDOR_ABS_FAULT 0
    pause 2

    step "Battery warning (vendor property $VENDOR_BATTERY_FAULT)"
    inject_silent $VENDOR_BATTERY_FAULT 0
    inject $VENDOR_BATTERY_FAULT 1
    pause 3
    inject $VENDOR_BATTERY_FAULT 0
    pause 2

    step "Oil warning"
    inject_silent ENGINE_OIL_LEVEL 2
    inject ENGINE_OIL_LEVEL 1
    pause 3
    inject ENGINE_OIL_LEVEL 2
    pause 2

    # Cluster shows TPMS when any wheel is below its configured min pressure.
    # Fallback: 80% of configured max (default 225 kPa -> 180 kPa).
    # TIRE_PRESSURE is continuous: CarService passes at most one event per
    # subscription period (about 1 s) per wheel, so wait after the reset.
    step "TPMS warning - left front below threshold"
    inject_silent TIRE_PRESSURE 0x1 250.0
    inject_silent TIRE_PRESSURE 0x2 250.0
    inject_silent TIRE_PRESSURE 0x4 250.0
    inject_silent TIRE_PRESSURE 0x8 250.0
    pause 2
    inject TIRE_PRESSURE 0x1 160.0
    pause 3
    inject TIRE_PRESSURE 0x1 250.0
    pause 2
}

section_odometer() {
    header "ODOMETER"

    step "12 345 km"
    inject PERF_ODOMETER 12345
    pause 3

    step "85 432 km"
    inject PERF_ODOMETER 85432
    pause 3

    step "Reset odometer"
    inject PERF_ODOMETER 12345
    pause 2
}

section_check() {
    header "CHECK - VHAL properties used by the Cluster UI"

    check_prop PERF_VEHICLE_SPEED_DISPLAY
    check_prop PERF_VEHICLE_SPEED
    check_prop GEAR_SELECTION
    check_prop ENGINE_RPM
    check_prop FUEL_LEVEL
    check_prop INFO_FUEL_CAPACITY
    check_prop RANGE_REMAINING
    check_prop ENGINE_COOLANT_TEMP
    check_prop $VENDOR_SPEED_LIMIT VENDOR_SPEED_LIMIT
    check_prop $VENDOR_DRIVE_MODE VENDOR_DRIVE_MODE
    check_prop ENV_OUTSIDE_TEMPERATURE
    check_prop TURN_SIGNAL_LIGHT_STATE
    check_prop HEADLIGHTS_STATE
    check_prop HIGH_BEAM_LIGHTS_STATE
    check_prop_area SEAT_BELT_BUCKLED $DRIVER_SEAT
    check_prop PARKING_BRAKE_ON
    check_prop ABS_ACTIVE
    check_prop $VENDOR_ABS_FAULT VENDOR_ABS_FAULT
    check_prop $VENDOR_BATTERY_FAULT VENDOR_BATTERY_FAULT
    check_prop ENGINE_OIL_LEVEL
    check_prop_area TIRE_PRESSURE 0x1 0x2 0x4 0x8   # FL FR RL RR
    check_prop PERF_ODOMETER
}

SECTION="${1:-all}"

case "$SECTION" in
    style)     section_style     ;;
    drive)     section_drive     ;;
    fuel)      section_fuel      ;;
    engine)    section_engine    ;;
    limit)     section_limit     ;;
    mode)      section_mode      ;;
    ambient)   section_ambient   ;;
    telltales) section_telltales ;;
    odometer)  section_odometer  ;;
    check)     section_check     ;;
    all)
        section_style
        section_drive
        section_fuel
        section_engine
        section_limit
        section_mode
        section_ambient
        section_telltales
        section_odometer
        ;;
    *)
        echo "Usage: $0 [all|style|drive|fuel|engine|limit|mode|ambient|telltales|odometer|check]"
        exit 1
        ;;
esac

echo ""
if [ "$FAILED" -gt 0 ] || [ "$MISSING" -gt 0 ]; then
    echo "Done - $SECTION: $FAILED failed injection(s), $MISSING unconfigured propert(y/ies)."
    exit 1
fi
echo "Done - $SECTION walkthrough complete."
