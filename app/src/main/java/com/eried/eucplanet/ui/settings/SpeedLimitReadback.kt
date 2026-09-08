package com.eried.eucplanet.ui.settings

/** Firmware-reported thresholds, never the app's editable presets or command intent. */
internal data class SpeedLimitReadback(
    val tiltbackKmh: Float = -1f,
    val alarmKmh: Float = -1f,
    val receivedAtMs: Long = 0L,
) {
    val hasReportedValues: Boolean get() = valid(tiltbackKmh) || valid(alarmKmh)

    fun visibleValues(connected: Boolean, nowMs: Long, observingSinceMs: Long): Pair<Float?, Float?> {
        val fresh = connected && receivedAtMs >= observingSinceMs &&
            nowMs >= receivedAtMs && nowMs - receivedAtMs <= 3_000L
        return Pair(tiltbackKmh.takeIf { fresh && valid(it) }, alarmKmh.takeIf { fresh && valid(it) })
    }

    private fun valid(value: Float): Boolean = value.isFinite() && value >= 0f
}
