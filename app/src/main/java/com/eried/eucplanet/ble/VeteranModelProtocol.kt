package com.eried.eucplanet.ble

import com.eried.eucplanet.ble.nosfet.NosfetAeonProtocol
import com.eried.eucplanet.data.model.WheelData
import com.eried.eucplanet.data.model.WheelSettingChange
import com.eried.eucplanet.diagnostics.AeonDiagnostics
import com.eried.eucplanet.diagnostics.DiagnosticCatalog

/** Per-model behavior and readback state, composed with one shared parser/connection.
 * Receives complete CRC-validated frames only; it never owns or writes to BLE. */
internal interface VeteranModelProtocol {
    val controls: VeteranControlProfile
    val capabilities: WheelCapabilities
    val telemetryTracePrefix: String get() = "Veteran realtime"

    /** Newly received binary light state, or null when this frame has no known readback. */
    fun acceptFrame(frame: ByteArray): Boolean? = null
    fun decorateTelemetry(data: WheelData): WheelData = data
    fun buildSettingChange(change: WheelSettingChange): List<ByteArray>? = null
    fun reset() {}
}

/** No model-specific settings readback for the unspecialized family members. */
internal class DefaultVeteranProtocol : VeteranModelProtocol {
    override val controls = DefaultVeteranControlProfile
    override val capabilities = WheelCapabilities.VETERAN
}

/** The single model-specialization boundary. Unknown models retain existing behavior. */
internal object VeteranModelProtocols {
    fun additionalCatalogs(text: (Int) -> String): List<DiagnosticCatalog> = listOf(
        DiagnosticCatalog(AeonDiagnostics.displayName, AeonDiagnostics.commands(text), AeonDiagnostics.inspectPrefixes)
    )

    fun create(model: VeteranModel?): VeteranModelProtocol = when (model) {
        VeteranModel.NOSFET_AEON -> NosfetAeonProtocol()
        else -> DefaultVeteranProtocol()
    }
}
