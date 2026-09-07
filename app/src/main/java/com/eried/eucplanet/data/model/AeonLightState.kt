package com.eried.eucplanet.data.model

/** Aeon wire value, not a brightness percentage. Unknown values remain intact. */
data class AeonLightState(val rawValue: Int) {
    enum class Level { OFF, LOW, MEDIUM, HIGH }

    val level: Level? get() = Level.entries.getOrNull(rawValue)
    val isOn: Boolean? get() = level?.let { it != Level.OFF }
}
