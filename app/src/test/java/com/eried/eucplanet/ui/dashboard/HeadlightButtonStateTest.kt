package com.eried.eucplanet.ui.dashboard

import com.eried.eucplanet.R
import com.eried.eucplanet.data.model.HeadlightReadback
import com.eried.eucplanet.data.model.WheelData
import org.junit.Assert.*
import org.junit.Test

class HeadlightButtonStateTest {
    private val received = 10_000_000_000L

    @Test fun `all reported levels select the existing button label and highlight`() {
        val labels = listOf(R.string.headlight_state_off, R.string.headlight_state_low,
            R.string.headlight_state_medium, R.string.headlight_state_high)
        HeadlightReadback.Level.entries.forEachIndexed { index, level ->
            val data = WheelData(lightOn = false, headlightReadback = HeadlightReadback(level, received))
            val state = headlightButtonState(data, true, received)
            assertEquals(labels[index], state.labelRes)
            assertEquals(level != HeadlightReadback.Level.OFF, state.active)
            assertEquals(state, headlightButtonState(data.copy(lightOn = true), true, received))
        }
    }

    @Test fun `missing unknown disconnected and stale readbacks never display off`() {
        val data = WheelData(lightOn = true,
            headlightReadback = HeadlightReadback(HeadlightReadback.Level.HIGH, received))
        val unknown = HeadlightButtonState(R.string.headlight_state_unknown, false)
        assertEquals(unknown, headlightButtonState(data, false, received))
        assertEquals(unknown, headlightButtonState(data, true, received + 8_000_000_001L))
        assertEquals(unknown, headlightButtonState(data, true, received - 1))
        assertEquals(unknown, headlightButtonState(data.copy(
            headlightReadback = HeadlightReadback(null, received)), true, received))
        assertTrue(headlightButtonState(data, true, received + 8_000_000_000L).active)
    }

    @Test fun `models without level readback keep their original label and state`() {
        for (on in listOf(false, true)) {
            assertEquals(HeadlightButtonState(R.string.action_light, on),
                headlightButtonState(WheelData(lightOn = on), true, received))
        }
    }
}
