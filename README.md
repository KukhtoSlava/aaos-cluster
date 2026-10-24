# Cluster — AAOS Instrument Cluster

A privileged system application for Android Automotive OS (AAOS) that reads live
vehicle data through the Car Property API and renders an instrument cluster with
speed, RPM, gear, fuel and range, coolant temperature, odometer, outside
temperature and telltales.

The layout is a 1848x792 screen (the AutoBox emulator cluster display) with a
full-bleed map area and two switchable gauge layouts.

| Round dials, parked | Round dials, all indicators on |
|:-:|:-:|
| ![Round dials, parked](design/cluster_round_off.png) | ![Round dials, all indicators on](design/cluster_round_on.png) |

| Arc scales, parked | Arc scales, all indicators on |
|:-:|:-:|
| ![Arc scales, parked](design/cluster_arc_off.png) | ![Arc scales, all indicators on](design/cluster_arc_on.png) |

"Parked": the map follows the parked car, no route.
"All indicators on": D at 82 km/h over a 60 km/h limit, SPORT, hazard lights,
every warning telltale, low and high beam (parking lights exclude the low beam
in `HEADLIGHTS_STATE`, so they stay off), and guidance on the map: the route,
the instruction arrows and the next maneuver card.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Repository pattern |
| DI | Dagger 2 |
| Async | Kotlin Coroutines + StateFlow |
| Platform | Android Automotive OS (AAOS), AOSP |
| Car APIs | `CarPropertyManager` |
| Build | Soong (`Android.bp`), platform-signed privileged app |

---

## Architecture

```
ClusterActivity
    └── ClusterViewModel          (StateFlow<ClusterUiState>)
            ├── ClusterRepository (interface)
            │       └── ClusterRepositoryImpl
            │               └── CarPropertyManager -> VHAL property subscription
            └── ClusterSettings   (interface)
                    └── ClusterSettingsImpl -> device-protected SharedPreferences
```

Data flow: VHAL events -> `CarPropertyManager.CarPropertyEventCallback` ->
`MutableStateFlow<ClusterData>` -> ViewModel `combine()` with the gauge style ->
Compose recomposition.

`ClusterRepositoryImpl` connects to CarService asynchronously with
`CAR_WAIT_TIMEOUT_DO_NOT_WAIT` and subscribes to the VHAL properties; each
subscription delivers the current value first, then the changes.

---

## Features

- Two gauge styles, switched with the control in the bottom bar (cross-fade).
  The choice is an app setting in device-protected SharedPreferences, so it
  survives restarts and is available before unlock.
  Only the speedometer and tachometer change; the top bar, speed limit sign,
  telltales and bottom bar stay in place:
  - **Round dials**: speedometer (0-220 km/h) left, tachometer (0-8 ×1000 RPM,
    redline from 6.5) right
  - **Arc scales**: segmented arcs on the screen edges with large speed and RPM readouts
- Top bar: turn signals, P R N D strip, clock, outside temperature, drive mode
- Map area under the gauges: a TomTom map drawn by the Navigation app
  (`vendor/autobox/apps/Navigation`). `MapView` hands the surface of its
  SurfaceView to Navigation's `ClusterMapService` (`IClusterMap`, signature
  permission `com.autobox.navigation.permission.CLUSTER_MAP`) together with the
  insets the gauges cover (`ClusterDimens.Map`). Cluster runs as the system user
  and binds Navigation of the current user, rebinding on a user switch. Until
  Navigation draws, the area shows the plain map colour. The AIDL comes from
  the Navigation checkout (filegroup `autobox-navigation-cluster-aidl`), so
  Cluster needs it to build.
- Speed turns amber above the speed limit
- Fuel bar with remaining range, amber on low fuel
- Coolant bar in °C, red at the engine warning temperature
- Speed limit sign
- Odometer in the bottom bar
- Telltales in fixed slots, dimmed when off: parking lights, low beam, high beam,
  seat belt, engine under the speedometer; ABS, battery, oil, parking brake,
  TPMS, low fuel under the tachometer; turn signals with a shared 0.9 s flasher
- Layout is drawn on a fixed 1848x792 canvas and scaled uniformly to the display
- Saira font (OFL, `OFL-Saira.txt`), OEM-overridable colours in `res/values/colors.xml`

### Vendor properties

The vendor properties are defined in `vendor/autobox/vhal/AutoBoxProperties.json`
(module `AutoBoxVhalProperties`). The reference and emulator VHALs load it from
`/vendor/etc/automotive/vhalconfig/`. All of them are global, READ and
on-change: the vehicle reports them, Android only displays them. The app needs
`CAR_VENDOR_EXTENSION` to read them. Values with a standard `VehicleProperty`
(outside temperature, seat belt) use the standard property.

---

## Subscribed Vehicle Properties

| Property | Area / Notes | Unit | Rate | UI field |
|----------|-------------|------|------|----------|
| `PERF_VEHICLE_SPEED_DISPLAY` | global | m/s | UI | `speedKmh` (× 3.6); `PERF_VEHICLE_SPEED` if the VHAL lacks it |
| `ENGINE_RPM` | global | rpm | UI | `rpmX1000` (÷ 1000) |
| `GEAR_SELECTION` | global | enum | on-change | `gear` — P=4 R=2 N=1 D=8 |
| `ENGINE_COOLANT_TEMP` | global | °C | normal | `coolantTempC`; `engineWarning` at ≥ 110 °C |
| `ENGINE_OIL_LEVEL` | global | enum | on-change | `oilWarning` — active when ≠ 2 (normal) |
| `PERF_ODOMETER` | global | km | normal | `odometerKm` |
| `FUEL_LEVEL` | global | mL | normal | `fuelPercent`; `fuelWarning` at ≤ 15% |
| `RANGE_REMAINING` | global | m | normal | `rangeKm` (÷ 1000) |
| `ENV_OUTSIDE_TEMPERATURE` | global | °C | normal | `outsideTempC` |
| `INFO_FUEL_CAPACITY` | global | mL | read once | divisor for `fuelPercent` |
| `INFO_DRIVER_SEAT` | global | seat area | read once | area of the `SEAT_BELT_BUCKLED` subscription |
| `ABS_ACTIVE` | global | boolean | on-change | `absEngaged` — ABS currently braking (transient) |
| `0x21400002` (vendor) | global | INT32 | on-change | `absFault` — ABS fault requiring service (persistent); `absActive = absEngaged \|\| absFault` |
| `SEAT_BELT_BUCKLED` | driver seat (`INFO_DRIVER_SEAT`) | boolean | on-change | `seatbeltWarning` — warning when false |
| `0x21400004` (vendor) | global | INT32 | on-change | `batteryWarning` — battery/charging fault; 1 = warning, 0 = normal |
| `0x21400005` (vendor) | global | INT32, km/h | on-change | `speedLimitKmh` — speed limit sign; 0 = no limit, sign hidden |
| `0x21400006` (vendor) | global | INT32 | on-change | `driveMode` — 0 = ECO, 1 = NORMAL, 2 = SPORT |
| `PARKING_BRAKE_ON` | global | boolean | on-change | `brakeWarning` |
| `HEADLIGHTS_STATE` | global | enum | on-change | `lowBeamOn` (1), `parkingLightsOn` (2) |
| `HIGH_BEAM_LIGHTS_STATE` | global | enum | on-change | `highBeamOn` (≠ 0) |
| `TURN_SIGNAL_LIGHT_STATE` | global | bit flags | on-change | `turnSignalLeft` (bit 2), `turnSignalRight` (bit 1) |
| `TIRE_PRESSURE` | `0x1` FL `0x2` FR `0x4` RL `0x8` RR | kPa | normal | `tpmsWarning` — any wheel below its configured min pressure; fallback to 80% of configured max |

### Emulator note — telltales

The telltales start from the emulator VHAL defaults: the parking brake is on
and the driver is unbuckled, so both warnings are lit after boot. The others
stay dark until a value is injected. Run `./test_properties.sh telltales` to
exercise every telltale and light in order.

---

## Testing

Run the property walkthrough on the host while the emulator/device is running:

```bash
cd vendor/autobox/apps/Cluster

./test_properties.sh            # full run, sections in UI order, largest elements first
./test_properties.sh style      # gauge layout: round dials, arc scales
./test_properties.sh drive      # speed (km/h), all 4 gear positions, RPM
./test_properties.sh fuel       # fuel bar, range, fuel warning
./test_properties.sh engine     # coolant temperature bar and engine warning
./test_properties.sh limit      # speed limit sign, hidden sign, amber speed over the limit
./test_properties.sh mode       # drive mode: SPORT, NORMAL, ECO
./test_properties.sh ambient    # outside temperature
./test_properties.sh telltales  # turn signals, lights, seat belt, brake, ABS,
                                #   battery, oil, TPMS
./test_properties.sh odometer   # odometer km
./test_properties.sh check      # VHAL property config + current values for every UI field
                                #   (TIRE_PRESSURE areas: 0x1, 0x2, 0x4, 0x8)
```

`inject-vhal-event` hands the value to CarService subscribers only. The VHAL keeps
its stored value, so `get-property-value` does not show injected values; check
the UI instead.
The script exits non-zero if an injection fails or `check` finds an unconfigured
property. BOOLEAN properties take `true`/`false` (`1` is parsed as false).
Continuous properties pass at most one event per subscription period (about 1 s),
so a second value injected right after the first one is dropped.

The script forces transitions (off → on → off) so each indicator can be visually
confirmed without duplicate checks.

The engine warning icon maps to high coolant temperature (≥ 110 °C) because AAOS
does not expose a generic `CHECK_ENGINE` boolean. The coolant bar turns red at
the same temperature.

If `adb` is not on PATH:

```bash
export PATH=$PATH:$(pwd)/out/host/linux-x86/bin
```

### Manual adb commands

```bash
# Speed is injected in m/s. UI displays km/h.
adb shell cmd car_service inject-vhal-event PERF_VEHICLE_SPEED_DISPLAY 25.0

# Gear: P=4, R=2, N=1, D=8.
adb shell cmd car_service inject-vhal-event GEAR_SELECTION 8

# RPM is injected in rpm. UI displays x1000 RPM.
adb shell cmd car_service inject-vhal-event ENGINE_RPM 2800

# Coolant temperature in °C. Engine warning is shown at 110°C+.
adb shell cmd car_service inject-vhal-event ENGINE_COOLANT_TEMP 110

# Fuel level in mL. Emulator capacity is commonly 15000 mL.
adb shell cmd car_service inject-vhal-event FUEL_LEVEL 1500

# Range in metres. UI displays km.
adb shell cmd car_service inject-vhal-event RANGE_REMAINING 482000

# Outside temperature in °C.
adb shell cmd car_service inject-vhal-event ENV_OUTSIDE_TEMPERATURE 12.0

# Speed limit in km/h (vendor property 0x21400005), 0 hides the sign.
adb shell cmd car_service inject-vhal-event 0x21400005 120

# Drive mode (vendor property 0x21400006): 0 = ECO, 1 = NORMAL, 2 = SPORT.
adb shell cmd car_service inject-vhal-event 0x21400006 2

# Gauge style (app setting): round or arc.
adb shell am broadcast --user 0 -n com.autobox.cluster/.settings.GaugeStyleReceiver \
    -a com.autobox.cluster.action.SET_GAUGE_STYLE --es style arc

# Battery/charging warning (vendor property 0x21400004).
adb shell cmd car_service inject-vhal-event 0x21400004 1   # warning on
adb shell cmd car_service inject-vhal-event 0x21400004 0   # warning off

# Odometer in km.
adb shell cmd car_service inject-vhal-event PERF_ODOMETER 85432

# ABS fault (vendor property 0x21400002 = VENDOR_ABS_FAULT).
# ABS_ACTIVE signals transient braking; this signals a persistent fault requiring service.
adb shell cmd car_service inject-vhal-event 0x21400002 1   # fault on
adb shell cmd car_service inject-vhal-event 0x21400002 0   # fault cleared

# Driver seat belt, area 0x1 = the emulator's driver seat. false = unbuckled.
adb shell cmd car_service inject-vhal-event SEAT_BELT_BUCKLED 0x1 false

# Parking brake. BOOLEAN properties take true/false: "1" is parsed as false.
adb shell cmd car_service inject-vhal-event PARKING_BRAKE_ON true

# Lights: 0 = off, 1 = low beam, 2 = parking/daytime lights.
adb shell cmd car_service inject-vhal-event HEADLIGHTS_STATE 2
adb shell cmd car_service inject-vhal-event HEADLIGHTS_STATE 1
adb shell cmd car_service inject-vhal-event HIGH_BEAM_LIGHTS_STATE 1

# Turn signals: none=0, right=1, left=2, hazards=3.
adb shell cmd car_service inject-vhal-event TURN_SIGNAL_LIGHT_STATE 2

# Engine oil level: critically low=0, low=1, normal=2, high=3, error=4.
adb shell cmd car_service inject-vhal-event ENGINE_OIL_LEVEL 1

# TPMS, area IDs: 0x1 FL, 0x2 FR, 0x4 RL, 0x8 RR.
# Warning below the wheel's configured min pressure, or 80% of its max without one.
adb shell cmd car_service inject-vhal-event TIRE_PRESSURE 0x1 160.0   # FL low -> TPMS on
adb shell cmd car_service inject-vhal-event TIRE_PRESSURE 0x1 250.0   # restore FL

# Inspect properties.
adb shell cmd car_service get-property-value ABS_ACTIVE
adb shell cmd car_service get-property-value PARKING_BRAKE_ON
adb shell cmd car_service get-property-value SEAT_BELT_BUCKLED 0x1
adb shell cmd car_service get-property-value ENGINE_COOLANT_TEMP
adb shell cmd car_service get-property-value ENGINE_OIL_LEVEL
adb shell cmd car_service get-property-value 0x21400004
adb shell cmd car_service get-property-value PERF_VEHICLE_SPEED_DISPLAY
adb shell cmd car_service get-property-value ENGINE_RPM
adb shell cmd car_service get-property-value GEAR_SELECTION
adb shell cmd car_service get-property-value PERF_ODOMETER
adb shell cmd car_service get-property-value TIRE_PRESSURE 0x1   # FL
adb shell cmd car_service get-property-value TIRE_PRESSURE 0x2   # FR
adb shell cmd car_service get-property-value TIRE_PRESSURE 0x4   # RL
adb shell cmd car_service get-property-value TIRE_PRESSURE 0x8   # RR
adb shell cmd car_service get-carpropertyconfig FUEL_LEVEL
adb shell cmd car_service get-carpropertyconfig TURN_SIGNAL_LIGHT_STATE
adb shell dumpsys car_service --services CarPropertyService
```

---

## Project Structure

```
Cluster/
├── src/com/autobox/cluster/
│   ├── ClusterActivity.kt
│   ├── ClusterApplication.kt
│   ├── di/                         Dagger component, modules, factory
│   ├── model/                      ClusterData, Gear, DriveMode, GaugeStyle
│   ├── settings/                   ClusterSettings (gauge style) + impl, adb receiver
│   ├── repository/
│   │   ├── ClusterRepository.kt    interface
│   │   ├── constants/
│   │   │   ├── ClusterDefaults.kt  thresholds and fallback values
│   │   │   └── VendorProperties.kt vendor property IDs (0x21400xxx)
│   │   └── impl/
│   │       ├── ClusterRepositoryImpl.kt  real Car API implementation
│   │       └── MockClusterRepository.kt simulation for local dev/reference
│   └── ui/
│       ├── ClusterViewModel.kt     ClusterUiState: vehicle data + gauge style
│       ├── ClusterScreen.kt        scaled canvas, layer cross-fade
│       ├── theme/                  ClusterTheme (Saira), ClusterDimens (all sizes), ClusterMotion
│       ├── gauges/                 GaugeScale, RoundGauges, ArcScales layouts
│       ├── chrome/                 top bar, speed limit, telltales: shared by both styles
│       ├── components/             ClusterText, telltales, level bars, clock, speed limit, layout helpers
│       ├── map/                    map surface, Navigation connection, legibility fades
│       └── bottombar/              odometer and gauge style switcher
├── design/                         screenshots: round and arc, parked and all indicators on
├── res/
│   ├── drawable/                   telltale and control icons (from the design)
│   ├── font/                       Saira 400/500/600/700
│   └── values/                     colours, strings, theme
├── test_properties.sh              VHAL property walkthrough script
├── Android.bp                      Soong build definition
├── AndroidManifest.xml
└── privapp-permissions-cluster.xml
```
