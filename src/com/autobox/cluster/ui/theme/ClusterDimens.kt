package com.autobox.cluster.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Every size and position of the cluster, in units of its 1848x792 layout.
 * ScaledCanvas makes 1.dp and 1.sp one layout unit, so these are also the
 * pixels of the emulator cluster display.
 *
 * Kept in code rather than in dimens.xml: dimensionResource() divides by
 * LocalDensity, which ScaledCanvas overrides to scale the whole layout.
 *
 * Positions are the top-left corner of an element on the cluster, or inside
 * its dial for the Round.Dial* values. Line heights are multiples of the font
 * size, as CSS line-height.
 */
internal object ClusterDimens {
    val Width = 1848.dp
    val Height = 792.dp

    /** Gradients that fade the map into the ground under the gauges. */
    object Fade {
        val TopHeight = 230.dp
        const val TOP_SOLID = 0.25f
        val SideWidth = 640.dp
        const val SIDE_SOLID = 0.46f
        val BottomHeight = 120.dp
        const val BOTTOM_SOLID = 0.8f
    }

    object TurnSignal {
        val Size = 46.dp
    }

    object ClockAndTemp {
        val Gap = 14.dp
    }

    /** Fuel and coolant: icon, thin bar, value. */
    object LevelBar {
        val IconSize = 20.dp
        val Gap = 10.dp
        val BarHeight = 5.dp
        val BarCorner = 3.dp
        val TextSize = 15.sp
    }

    /** Value marker and needle glow, CSS box-shadow 0 0 14px. */
    object Glow {
        // 14px CSS blur is sigma 7; Android maps a shadow radius r to sigma 0.577r + 0.5
        val ShadowRadius = 11.dp
    }

    /** Floating panels of the bottom bar. */
    object Panel {
        val Corner = 16.dp
        val Border = 1.dp
    }

    object BottomBar {
        val Y = 694.dp
        val Gap = 12.dp

        val OdoPaddingH = 22.dp
        val OdoPaddingV = 10.dp
        val OdoGap = 10.dp
        val OdoLabelSize = 17.sp
        val OdoLabelSpacing = 2.sp
        val OdoValueSize = 26.sp

        val SwitcherPadding = 4.dp
        val SwitcherGap = 4.dp
        val ButtonWidth = 52.dp
        val ButtonHeight = 44.dp
        val ButtonCorner = 12.dp
        val ButtonIconSize = 26.dp
    }

    /**
     * Everything that stays in place when the gauge style changes: the top bar,
     * the speed limit sign and the telltale rows. The left and right groups line
     * up with the round dials.
     */
    object Chrome {
        val SideMargin = 60.dp
        val GroupWidth = 440.dp
        val RightX = ClusterDimens.Width - SideMargin - GroupWidth // 1348

        // Top bar: turn signal and gear strip | clock, drive mode and turn signal
        val TopBarY = 32.dp
        val GearGap = 24.dp
        val GearSize = 34.sp
        const val GEAR_LINE_HEIGHT = 1f
        val GearUnderlineGap = 5.dp
        val GearUnderlineWidth = 22.dp
        val GearUnderlineHeight = 3.dp
        val GearUnderlineCorner = 2.dp
        val ClockSize = 30.sp
        val TempSize = 22.sp
        val ModeGap = 10.dp
        val ModeLabelSize = 18.sp
        val ModeLabelSpacing = 2.sp
        val ModeValueSize = 24.sp

        // Speed limit sign, right of the speedometer
        val LimitX = 560.dp
        val LimitY = 672.dp
        val LimitSize = 76.dp
        val LimitRing = 8.dp
        val LimitTextSize = 28.sp

        // Telltale rows under the gauges
        val TelltaleY = 652.dp
        val TelltaleSize = 34.dp
        val TelltaleGap = 30.dp
    }

    /** Round dials layout: speedometer left, tachometer right. */
    object Round {
        // Placement on the cluster, aligned with the Chrome groups
        val Dial = Chrome.GroupWidth
        val LeftX = Chrome.SideMargin
        val RightX = Chrome.RightX
        val DialY = 166.dp

        // Scale: angles from 12 o'clock, clockwise; the gap is at the bottom
        const val START_ANGLE = -135f
        const val SWEEP = 270f
        val FaceRadius = 212.dp
        val RimRadius = 211.5.dp
        val RimWidth = 1.dp
        val TrackRadius = 212.dp
        val TrackWidth = 5.dp
        val RedzoneRadius = 192.dp
        val RedzoneWidth = 18.dp
        val TickRadius = 200.dp
        val MajorTickLength = 16.dp
        val MajorTickWidth = 3.dp
        val MinorTickLength = 8.dp
        val MinorTickWidth = 2.dp
        val LabelRadius = 160.dp
        val LabelBoxWidth = 56.dp
        val LabelBoxHeight = 30.dp
        val SpeedLabelSize = 19.sp
        val RpmLabelSize = 21.sp

        // Needle: from NeedleTip above the hub to NeedleTail below it
        val NeedleWidth = 5.dp
        val NeedleTip = 179.5.dp
        val NeedleTail = 19.5.dp
        val HubRadius = 25.dp
        val HubRimWidth = 3.dp
        val HubDotRadius = 5.5.dp

        // Readouts inside the dial
        val ReadoutWidth = 250.dp
        val ReadoutX = 95.dp
        val SpeedReadoutY = 258.dp
        val SpeedValueSize = 82.sp
        val SpeedValueSpacing = (-2).sp
        val SpeedUnitSize = 16.sp
        val SpeedUnitGap = 4.dp
        val RpmReadoutY = 266.dp
        val RpmValueSize = 64.sp
        val RpmValueSpacing = (-1).sp
        val RpmUnitSize = 15.sp
        val RpmUnitGap = 8.dp
        const val VALUE_LINE_HEIGHT = 1f
        val UnitSpacing = 3.sp

        // Fuel / coolant bar inside the dial
        val LevelBarX = 125.dp
        val LevelBarY = 392.dp
        val LevelBarWidth = 190.dp
    }

    /** Arc scales layout: segmented arcs on the screen edges, large readouts. */
    object Arc {
        // Scale: arcs of one circle radius, angles as drawArc (3 o'clock, clockwise)
        val Radius = 520.dp
        val CenterY = ClusterDimens.Height / 2 // 396
        val SpeedCenterX = 620.dp
        // ±26° around the horizontal: the lower ends stay above the telltale rows
        const val SPEED_START_ANGLE = 154f
        const val SPEED_END_ANGLE = 206f
        val RpmCenterX = ClusterDimens.Width - SpeedCenterX // 1228
        const val RPM_START_ANGLE = 26f
        const val RPM_END_ANGLE = -26f

        val TrackWidth = 16.dp
        val ValueGlowWidth = 34.dp
        // Segments: ground-coloured dashes across the band, as fractions of the
        // arc length in percent (design: stroke-dasharray 0.35 1.465, offset 0.6)
        val SegmentCutWidth = 40.dp
        const val SEGMENT_CUT = 0.35f
        const val SEGMENT_GAP = 1.465f
        const val SEGMENT_PHASE = 0.6f

        val TickInset = 16.dp
        val MajorTickLength = 18.dp
        val MajorTickWidth = 3.dp
        val MinorTickLength = 9.dp
        val MinorTickWidth = 2.dp
        val LabelInset = 58.dp
        val LabelBoxWidth = 56.dp
        val LabelBoxHeight = 28.dp
        val LabelSize = 19.sp

        // Value marker: a bar across the arc, from MarkerOuter outside the arc
        // to MarkerInner inside it
        val MarkerOuter = 17.dp
        val MarkerInner = 23.dp
        val MarkerWidth = 6.dp

        // Readouts: speed on the left, RPM on the right
        val ReadoutY = 270.dp
        val ReadoutWidth = 300.dp
        val LeftReadoutX = 270.dp
        val RightReadoutX = ClusterDimens.Width - LeftReadoutX - ReadoutWidth // 1278
        val ReadoutGap = 14.dp
        val BigValueSize = 150.sp
        const val BIG_VALUE_LINE_HEIGHT = 0.95f
        val SpeedSpacing = (-4).sp
        val RpmSpacing = (-2).sp
        val UnitSize = 20.sp
        val UnitSpacing = 4.sp
        val UnitGap = 6.dp
        val LevelBarGap = 20.dp
        val LevelBarWidth = 220.dp
    }
}
