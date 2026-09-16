package com.eried.eucplanet.data.model

/**
 * The lock code for wheels whose unlock wants one.
 *
 * Only KingSong today: the rider sets six digits in the KingSong app, the wheel
 * locks on a plain command and unlocks only when those digits come with it
 * (decoded from a KS-18XL capture, issue #19, 2026-09-16). The code belongs to
 * the wheel, so it is mirrored into its profile and follows it on reconnect,
 * like the pack size and the battery calibration.
 */
data class WheelLockSettings(
    /** Six digits, or "" when the rider has not entered one. */
    val code: String = "",
)
