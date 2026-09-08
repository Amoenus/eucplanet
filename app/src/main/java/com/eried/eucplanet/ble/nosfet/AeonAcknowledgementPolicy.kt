package com.eried.eucplanet.ble.nosfet

import com.eried.eucplanet.ble.VeteranCommands
import com.eried.eucplanet.ble.VeteranControlProfile

/** Command-path selection, NOT beep-volume scaling or a universal firmware mute bit. */
internal object AeonAcknowledgementPolicy {
    fun soundLevel(settings: com.eried.eucplanet.data.model.AeonSettings?, nowNanos: Long = System.nanoTime()): Int? =
        settings?.takeIf { it.isFresh(nowNanos) }?.value(com.eried.eucplanet.data.model.AeonSetting.KEY_TONE)

    fun select(snd: Int?, audible: List<ByteArray>, silent: List<ByteArray>?): List<ByteArray> =
        if (snd != null && snd > 0) audible else silent ?: audible
}

/** Snapshot the chosen light transaction so telemetry cannot mix its two command paths. */
internal class AeonAcknowledgedControlProfile(private val sound: () -> Int?) :
    VeteranControlProfile by AeonControlProfile {
    private var lightTransaction: Pair<Boolean, List<ByteArray>>? = null

    @Synchronized
    override fun setLight(on: Boolean): ByteArray {
        val frames = AeonAcknowledgementPolicy.select(sound(),
            audible = listOf(VeteranCommands.setHighBeam(on), VeteranCommands.setHighBeamCompanion(on)),
            silent = listOf(VeteranCommands.setLight(on)))
        lightTransaction = on to frames
        return frames.first()
    }

    @Synchronized
    override fun setLightFollowup(on: Boolean): ByteArray? {
        val transaction = lightTransaction
        lightTransaction = null
        return transaction?.takeIf { it.first == on }?.second?.getOrNull(1)
    }

    @Synchronized
    fun reset() { lightTransaction = null }

}
