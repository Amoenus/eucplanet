package com.eried.eucplanet.voice

/**
 * Everything a rider can ask for, and the name they would say for it.
 *
 * Generated rather than written. The names come from the catalogs the app
 * already ships: `MetricCatalog` carries 52 metrics whose labels are already
 * translated into every locale the app speaks, so a German rider asks for
 * "Verbrauch" and a Japanese one for "電費" without anybody writing a synonym
 * list. The tile abbreviations are the dashboard renderer shortening those same
 * labels for space; underneath they are the full words.
 *
 * That is also why this file takes the names as a parameter instead of reading
 * resources itself. It stays free of Android, so the rules can be tested
 * directly, and the caller does the one thing that needs a Context.
 */
object VoiceVocabulary {

    /** What a term refers to, which decides how the answer is produced. */
    enum class Kind {
        /** A live dashboard metric, answered from its current value. */
        METRIC,

        /** One of the spoken reports VoiceReportPlan already assembles. */
        REPORT,

        /** An acceleration split, rendered by AccelSplitVoice. */
        SPLIT,
    }

    /**
     * One thing a rider can ask for: a stable key to act on, and the name in
     * the rider's own language to listen for.
     */
    data class SpokenTerm(val key: String, val kind: Kind, val name: String)

    /** The key the split terms use, so callers do not repeat the literal. */
    const val SPLIT_KEY = "LAST_SPLIT"

    /**
     * Build the vocabulary.
     *
     * @param metricNames metric key to its localised label
     * @param reportNames report key to its localised label. A map, not the bare
     *                    key list: VoiceReportPlan's keys are English
     *                    identifiers, and feeding those in as spoken names put
     *                    "Battery", "Current" and "Distance" in the middle of a
     *                    German rider's list, next to Akku and Energie. The
     *                    report_* strings have been translated all along.
     * @param splitName   the localised name for the last acceleration split
     */
    fun build(
        metricNames: Map<String, String>,
        reportNames: Map<String, String>,
        splitName: String,
    ): List<SpokenTerm> {
        val terms = mutableListOf<SpokenTerm>()
        val seen = mutableSetOf<String>()

        // A blank label is a resource that has not been translated yet. Listing
        // it would put a term in "What can I say" that nothing can ever match.
        for ((key, name) in metricNames) {
            if (name.isBlank() || !seen.add(key)) continue
            terms += SpokenTerm(key, Kind.METRIC, name.trim())
        }
        for ((key, name) in reportNames) {
            if (name.isBlank() || !seen.add(key)) continue
            terms += SpokenTerm(key, Kind.REPORT, name.trim())
        }
        if (splitName.isNotBlank() && seen.add(SPLIT_KEY)) {
            terms += SpokenTerm(SPLIT_KEY, Kind.SPLIT, splitName.trim())
        }
        return terms
    }
}
