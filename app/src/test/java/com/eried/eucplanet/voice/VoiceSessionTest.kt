package com.eried.eucplanet.voice

import com.eried.eucplanet.voice.VoiceAnswer.Answer
import com.eried.eucplanet.voice.VoiceAnswer.Reason
import com.eried.eucplanet.voice.VoiceCommandMatcher.VoiceMatch
import com.eried.eucplanet.voice.VoiceVocabulary.Kind
import com.eried.eucplanet.voice.VoiceVocabulary.SpokenTerm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A whole session, from the phrase a rider spoke to the sentence they hear.
 *
 * Run through [FakeVoiceListener] rather than a microphone, which is the point
 * of that fixture: an emulator has nothing to speak into, and a unit test has
 * no Android at all, but the chain in between is the part worth testing.
 */
class VoiceSessionTest {

    private val vocabulary = VoiceVocabulary.build(
        metricNames = mapOf(
            "BATTERY" to "Battery",
            "WH_PER_KM" to "Consumption",
            "MOTOR_TEMP" to "Motor temperature",
            "TIRE_PRESSURE" to "Tyre pressure",
            "RANGE_ESTIMATE" to "Range",
            "UNTRANSLATED" to "   ",
        ),
        reports = listOf("PWM"),
        splitName = "Last split",
    )

    /** What the app does with one spoken phrase. */
    private fun ask(
        phrase: String,
        values: Map<String, String> = emptyMap(),
        unavailable: Map<String, Reason> = emptyMap(),
        onDashboard: Set<String> = emptySet(),
    ): Answer {
        val heard = FakeVoiceListener(phrase).let { listener ->
            listener.start()
            (listener.state.value as ListenState.Final).text
        }
        return when (val m = VoiceCommandMatcher.match(heard, vocabulary, onDashboard)) {
            is VoiceMatch.Hit ->
                VoiceAnswer.answerFor(m.term, values[m.term.key], unavailable[m.term.key])
            is VoiceMatch.Ambiguous -> Answer.NeedsChoice(m.candidates.map { it.name })
            VoiceMatch.None -> VoiceAnswer.notUnderstood(vocabulary, onDashboard)
        }
    }

    @Test
    fun `the fake walks the same states a real session does`() {
        val script = FakeVoiceListener("what is my battery").script()
        assertEquals(ListenState.Listening, script.first())
        assertTrue(script[1] is ListenState.Partial)
        assertEquals(ListenState.Final("what is my battery"), script.last())
        // The partials grow a word at a time, which is what the transcript pill
        // renders while the rider is still speaking.
        assertEquals(ListenState.Partial("what"), script[1])
        assertEquals(ListenState.Partial("what is"), script[2])
    }

    @Test
    fun `a question with a value gets the value`() {
        val a = ask("what is my battery", values = mapOf("BATTERY" to "86 percent"))
        assertEquals(Answer.Say("Battery", "86 percent"), a)
    }

    @Test
    fun `a wheel that cannot report it says so`() {
        val a = ask("tyre pressure", unavailable = mapOf("TIRE_PRESSURE" to Reason.UNSUPPORTED_BY_WHEEL))
        assertEquals(Answer.Unavailable("Tyre pressure", Reason.UNSUPPORTED_BY_WHEEL), a)
    }

    @Test
    fun `a feature switched off says which setting`() {
        val a = ask("last split", unavailable = mapOf(VoiceVocabulary.SPLIT_KEY to Reason.OFF_IN_SETTINGS))
        assertEquals(Answer.Unavailable("Last split", Reason.OFF_IN_SETTINGS), a)
    }

    @Test
    fun `something that needs setting up says what is missing`() {
        val a = ask("range", unavailable = mapOf("RANGE_ESTIMATE" to Reason.NEEDS_SETUP))
        assertEquals(Answer.Unavailable("Range", Reason.NEEDS_SETUP), a)
    }

    @Test
    fun `a stale reading is not passed off as current`() {
        // A reason always beats a value. A metric can hold the last number it
        // saw before the sensor dropped out, and speaking that as if it were
        // now is worse than saying the sensor is gone.
        val a = ask(
            "tyre pressure",
            values = mapOf("TIRE_PRESSURE" to "2.4 bar"),
            unavailable = mapOf("TIRE_PRESSURE" to Reason.NO_DATA_YET),
        )
        assertEquals(Answer.Unavailable("Tyre pressure", Reason.NO_DATA_YET), a)
    }

    @Test
    fun `no value and no reason is still not silence`() {
        val a = ask("consumption")
        assertEquals(Answer.Unavailable("Consumption", Reason.NO_DATA_YET), a)
    }

    @Test
    fun `a phrase it did not catch offers examples from the rider's own tiles`() {
        val a = ask("what is the weather like", onDashboard = setOf("WH_PER_KM", "MOTOR_TEMP"))
        assertTrue(a is Answer.NotUnderstood)
        val examples = (a as Answer.NotUnderstood).examples
        assertEquals(2, examples.size)
        assertTrue("offered $examples", examples.all { it == "Consumption" || it == "Motor temperature" })
    }

    @Test
    fun `an untranslated name never reaches the vocabulary`() {
        // A label with no translation yet would otherwise sit in the What can I
        // say list as a term nothing can match.
        assertTrue(vocabulary.none { it.key == "UNTRANSLATED" })
    }

    @Test
    fun `the vocabulary covers metrics, reports and the split`() {
        assertTrue(vocabulary.any { it.kind == Kind.METRIC && it.key == "BATTERY" })
        assertTrue(vocabulary.any { it.kind == Kind.REPORT && it.key == "PWM" })
        assertTrue(vocabulary.any { it.kind == Kind.SPLIT && it.key == VoiceVocabulary.SPLIT_KEY })
        assertEquals(vocabulary.map { it.key }.distinct().size, vocabulary.size)
    }

    @Test
    fun `asking for the specific temperature does not get the general one`() {
        val withBoth = vocabulary + SpokenTerm("TEMPERATURE", Kind.METRIC, "Temperature")
        val m = VoiceCommandMatcher.match("motor temperature", withBoth)
        assertEquals("MOTOR_TEMP", (m as VoiceMatch.Hit).term.key)
    }
}
