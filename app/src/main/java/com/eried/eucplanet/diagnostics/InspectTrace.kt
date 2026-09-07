package com.eried.eucplanet.diagnostics

/** Match a named trace without assuming len= immediately follows its name.
 * Malformed bytes reject the frame instead of silently shifting byte offsets. */
internal fun inspectTraceBytes(text: String, prefix: String): List<Int>? {
    if (prefix.isBlank() || !text.startsWith("$prefix ")) return null
    val bodyStart = text.indexOf(" body=", prefix.length)
    if (bodyStart < 0) return null
    val header = text.substring(prefix.length, bodyStart).trim().split(' ')
    if (header.none { it.startsWith("len=") && it.removePrefix("len=").toIntOrNull() != null }) return null
    val body = text.substring(bodyStart + " body=".length).trim()
    if (body.isEmpty()) return null
    return body.split(' ').filter { it.isNotEmpty() }.map {
        it.toIntOrNull(16)?.takeIf { byte -> byte in 0..255 } ?: return null
    }
}
