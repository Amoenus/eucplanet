package com.eried.eucplanet.diagnostics

import com.eried.eucplanet.R
import com.eried.eucplanet.ble.nosfet.AeonControlProfile
import com.eried.eucplanet.ble.nosfet.NosfetAeonProtocol

/** Model catalogue, not a second BLE adapter. Only existing single-frame controls belong here. */
internal object AeonDiagnostics {
    const val displayName = "NOSFET"

    // Inspect matches the shared parser's actual log prefix, not the catalogue name.
    val inspectPrefixes = listOf(NosfetAeonProtocol.TRACE_PREFIX)

    fun commands(text: (Int) -> String): List<DiagnosticCommand> = listOf(
        DiagnosticCommand("SetLightON", text(R.string.notif_btn_light_on),
            AeonControlProfile.setLight(true), DiagnosticCommand.Category.LIGHT),
        DiagnosticCommand("SetLightOFF", text(R.string.notif_btn_light_off),
            AeonControlProfile.setLight(false), DiagnosticCommand.Category.LIGHT),
        DiagnosticCommand("Horn_LkAp", text(R.string.notif_btn_horn),
            AeonControlProfile.horn(), DiagnosticCommand.Category.HORN),
    )
}
