package com.eried.eucplanet.ble.nosfet

import com.eried.eucplanet.ble.VeteranModel
import com.eried.eucplanet.ble.VeteranParser
import com.eried.eucplanet.ble.VeteranModelProtocol
import com.eried.eucplanet.ble.WheelCapabilities
import com.eried.eucplanet.data.model.AeonLightState
import com.eried.eucplanet.data.model.HeadlightReadback
import com.eried.eucplanet.data.model.AeonSettingChange
import com.eried.eucplanet.data.model.AeonSettings
import com.eried.eucplanet.data.model.WheelData
import com.eried.eucplanet.data.model.WheelSettingChange
import com.eried.eucplanet.data.model.WheelPreferenceChange
import com.eried.eucplanet.data.model.aeonSetting
import java.util.concurrent.atomic.AtomicLong

/** First-class Aeon model component. No parser, transport or common telemetry duplication.
 * Each identified model session owns its readbacks; reset invalidates in-flight confirmations. */
internal class NosfetAeonProtocol : VeteranModelProtocol {
    override val controls = AeonAcknowledgedControlProfile { acknowledgementSound() }
    override val telemetryTracePrefix = TRACE_PREFIX

    // Explicit compatibility declaration, not a claim of physical verification.
    // Speed/alarm exposure predates this refactor and remains unverified on Aeon.
    // Remote lock is unavailable until Aeon support is established.
    // Menu sound is not the generic volume control; DRL writes are still unknown.
    override val capabilities = WheelCapabilities(
        hasHorn = true, hasLight = true, hasLock = false,
        hasMaxSpeed = true, hasAlarmSpeed = true,
    )

    @Volatile private var lightState: AeonLightState? = null
    private var lightReceivedAtNanos: Long = 0L
    @Volatile private var settings: AeonSettings? = null
    private var sessionId = nextSessionId.incrementAndGet()
    private var receivedAeonFrame = false
    private var clockSyncAttempted = false

    @Synchronized
    override fun acceptFrame(frame: ByteArray): Boolean? {
        // Caller supplies CRC-validated complete frames. A name match alone is not enough.
        if (VeteranParser.mVerOf(frame) == 44) receivedAeonFrame = true
        AeonTelemetryDecoder.settings(frame, VeteranModel.NOSFET_AEON)?.let {
            settings = it.copy(sessionId = sessionId)
        }
        val receivedLight = AeonTelemetryDecoder.lightState(frame, VeteranModel.NOSFET_AEON)
        if (receivedLight != null) {
            lightState = receivedLight
            lightReceivedAtNanos = System.nanoTime()
        }
        return receivedLight?.isOn
    }

    @Synchronized
    override fun decorateTelemetry(data: WheelData): WheelData = data.copy(
        headlightReadback = HeadlightReadback(
            when (lightState?.level) {
                AeonLightState.Level.OFF -> HeadlightReadback.Level.OFF
                AeonLightState.Level.LOW -> HeadlightReadback.Level.LOW
                AeonLightState.Level.MEDIUM -> HeadlightReadback.Level.MEDIUM
                AeonLightState.Level.HIGH -> HeadlightReadback.Level.HIGH
                null -> null
            },
            lightReceivedAtNanos,
        ),
        aeonLightState = lightState,
        aeonSettings = settings,
    )

    @Synchronized
    override fun buildSettingChange(change: WheelSettingChange): List<ByteArray>? {
        val request = when (change) {
            is AeonSettingChange -> change
            is WheelPreferenceChange -> AeonSettingChange(change.preference.aeonSetting(), change.value)
        }
        val current = settings ?: return null
        if (current.value(request.setting) == null) return null
        val frames = AeonCommands.setting(request.setting, request.value) ?: return null
        // No silent variant is established for these settings. Keep the known command.
        return AeonAcknowledgementPolicy.select(acknowledgementSound(), frames, silent = null)
    }

    private fun acknowledgementSound(): Int? = AeonAcknowledgementPolicy.soundLevel(settings)

    @Synchronized
    override fun takeDeferredInitCommand(): ByteArray? {
        if (!receivedAeonFrame || clockSyncAttempted) return null
        clockSyncAttempted = true
        return AeonCommands.synchronizeClock()
    }

    @Synchronized
    override fun reset() {
        controls.reset()
        receivedAeonFrame = false
        clockSyncAttempted = false
        lightState = null
        lightReceivedAtNanos = 0L
        settings = null
        sessionId = nextSessionId.incrementAndGet()
    }

    companion object {
        const val TRACE_PREFIX = "NOSFET Aeon"
        // Unique even when an unexpected model change creates a replacement component.
        private val nextSessionId = AtomicLong()
    }
}
