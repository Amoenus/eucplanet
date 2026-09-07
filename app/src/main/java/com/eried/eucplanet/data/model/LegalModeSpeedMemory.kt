package com.eried.eucplanet.data.model

import kotlin.math.abs

/** Temporary, per-wheel restore points. Telemetry must never overwrite these values. */
internal class LegalModeSpeedMemory {
    data class Limits(val tiltback: Float, val alarm: Float)
    private data class Saved(val limits: Limits, val restoringSince: Long? = null)
    private val previous = mutableMapOf<String, Saved>()

    @Synchronized fun enter(wheel: String, normal: Limits) {
        // Repeated enable, or re-enable after a failed restore, retains the original pair.
        previous[wheel] = Saved(previous[wheel]?.limits ?: normal)
    }

    @Synchronized fun restore(wheel: String, fallback: Limits, nowMs: Long): Limits {
        val limits = previous[wheel]?.limits ?: fallback
        previous[wheel] = Saved(limits, nowMs)
        return limits
    }

    @Synchronized fun protects(wheel: String?): Boolean = previous.containsKey(wheel)

    @Synchronized fun requestedLegal(wheel: String?): Boolean? =
        previous[wheel]?.let { it.restoringSince == null }

    /** Retain the restore point on missing/partial readback or failed writes.
     * A later matching pair permits a fresh snapshot on the next Legal-mode cycle. */
    @Synchronized fun observe(wheel: String?, tiltback: Float, alarm: Float, receivedAtMs: Long) {
        val saved = previous[wheel] ?: return
        val restoringSince = saved.restoringSince ?: return
        if (receivedAtMs <= restoringSince || tiltback <= 0f || alarm <= 0f) return
        if (abs(tiltback - saved.limits.tiltback) < 0.5f && abs(alarm - saved.limits.alarm) < 0.5f) {
            previous.remove(wheel)
        }
    }
}
