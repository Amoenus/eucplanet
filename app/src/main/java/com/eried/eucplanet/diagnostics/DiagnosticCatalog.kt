package com.eried.eucplanet.diagnostics

/** Browsable model catalogue, independent of the number of runtime BLE adapters. */
data class DiagnosticCatalog(
    val displayName: String,
    val commands: List<DiagnosticCommand>,
    val inspectPrefixes: List<String>,
)
