package com.eried.eucplanet.data.model

/** Optional model-provided level readback. Null level means supported but not yet known. */
data class HeadlightReadback(val level: Level?, val receivedAtNanos: Long) {
    enum class Level { OFF, LOW, MEDIUM, HIGH }

    fun freshLevel(nowNanos: Long): Level? =
        level.takeIf { nowNanos - receivedAtNanos in 0..8_000_000_000L }
}
