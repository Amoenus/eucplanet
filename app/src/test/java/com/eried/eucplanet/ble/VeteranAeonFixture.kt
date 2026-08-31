package com.eried.eucplanet.ble

/**
 * Public WheelLog Aeon telemetry fixture, split at the original ATT boundaries.
 * Source: https://github.com/Wheellog/Wheellog.Android/commit/dcf56672
 */
internal fun aeonFrameChunks(): List<ByteArray> = listOf(
    "dc5a5c5337f000002342000025150000fff80a66",
    "04ae0000025807d0acd907c8ffe8015080c80000",
    "808080808080068080808080800f870f870f800f",
    "940f920f930f940f930f910f930f940f950f950f",
    "960f91d94e297a",
).map(String::hexToByteArray)

private fun String.hexToByteArray(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()
