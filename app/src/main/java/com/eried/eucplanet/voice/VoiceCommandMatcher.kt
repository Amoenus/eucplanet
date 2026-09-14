package com.eried.eucplanet.voice

import com.eried.eucplanet.voice.VoiceVocabulary.SpokenTerm

/**
 * What the rider meant, out of what the recogniser heard.
 *
 * A rider does not say a key, they say a sentence: "what is my battery", "how
 * hot is the motor", "consumption". So this looks for any known name inside the
 * phrase rather than demanding the phrase be a name.
 *
 * The hard part is not matching, it is the near neighbours. There are three
 * temperatures in the catalog (motor, controller, battery) and two speed
 * limits, so "temperature" on its own is genuinely ambiguous and "motor
 * temperature" must not be answered with the plain temperature. Two rules
 * handle both: the longest name wins, and where several still tie, the rider's
 * own dashboard decides, because a tile they chose to look at is the one they
 * meant. Neither rule needs a setting.
 *
 * Free of Android, so every rule below is a plain unit test.
 */
object VoiceCommandMatcher {

    sealed interface VoiceMatch {
        /** Exactly one thing was meant. */
        data class Hit(val term: SpokenTerm) : VoiceMatch

        /** Several were equally plausible and the dashboard did not decide. */
        data class Ambiguous(val candidates: List<SpokenTerm>) : VoiceMatch

        /** Nothing in the vocabulary appeared in the phrase. */
        data object None : VoiceMatch
    }

    /**
     * @param heard        what the recogniser returned
     * @param vocabulary   from [VoiceVocabulary.build]
     * @param onDashboard  metric keys the rider currently has as tiles, used
     *                     only to break a tie
     */
    fun match(
        heard: String,
        vocabulary: List<SpokenTerm>,
        onDashboard: Set<String> = emptySet(),
    ): VoiceMatch {
        val phrase = normalise(heard)
        if (phrase.isBlank()) return VoiceMatch.None

        val present = vocabulary.filter { term ->
            val name = normalise(term.name)
            name.isNotBlank() && phrase.contains(name)
        }
        if (present.isEmpty()) return VoiceMatch.None

        // Longest first: "motor temperature" beats "temperature", which is the
        // whole reason a rider can ask for a specific one at all.
        val longest = present.maxOf { normalise(it.name).length }
        val best = present.filter { normalise(it.name).length == longest }
        if (best.size == 1) return VoiceMatch.Hit(best.first())

        // Still tied. The rider's own tiles decide.
        val onTiles = best.filter { it.key in onDashboard }
        if (onTiles.size == 1) return VoiceMatch.Hit(onTiles.first())

        return VoiceMatch.Ambiguous(best)
    }

    /**
     * Lower case, punctuation out, runs of blanks collapsed. Recognisers differ
     * on commas and question marks, and none of that changes what was asked.
     */
    private fun normalise(text: String): String =
        text.lowercase()
            .map { if (it.isLetterOrDigit() || it.isWhitespace()) it else ' ' }
            .joinToString("")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
