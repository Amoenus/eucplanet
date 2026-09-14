package com.eried.eucplanet.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rule 12 says every user-facing string is translated into every locale the app
 * ships. That is easy to honour on the day and easy to forget on the next one,
 * so this reads the resource files rather than trusting anybody's memory.
 *
 * It also checks the format arguments, because a translation that drops a
 * `%1$s` does not fail to build: it throws at the moment a rider asks a
 * question the app cannot answer, which is the worst possible moment.
 */
class VoiceStringCoverageTest {

    private val resDir = File("src/main/res")

    private val voiceKeys = listOf(
        "action_chip_voice_listen",
        "voice_commands_title",
        "voice_commands_enable_desc",
        "voice_command_prompt",
        "voice_prompt_beep",
        "voice_prompt_voice",
        "voice_prompt_none",
        "voice_command_window",
        "voice_command_vocabulary",
        "voice_listening",
        "voice_answer_off",
        "voice_answer_unsupported",
        "voice_answer_nodata",
        "voice_answer_setup",
        "voice_answer_unknown",
        "voice_help_terms",
        "voice_answer_examples",
        "voice_answer_mic_busy",
        "voice_answer_which",
    )

    /** Every values* directory that carries a translation. */
    private fun localeDirs(): List<File> =
        resDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("values") }
            ?.filter { File(it, "strings.xml").exists() }
            ?.sortedBy { it.name }
            ?: emptyList()

    private fun stringsIn(dir: File): Map<String, String> {
        val text = File(dir, "strings.xml").readText()
        val re = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return re.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun `the app really does ship the locales this test thinks it does`() {
        // A guard on the guard: if the res folder moves, the loop below would
        // pass by checking nothing at all.
        val dirs = localeDirs()
        assertTrue("found only ${dirs.size} locale folders under $resDir", dirs.size >= 20)
    }

    @Test
    fun `every voice string exists in every locale`() {
        val missing = mutableListOf<String>()
        for (dir in localeDirs()) {
            val strings = stringsIn(dir)
            for (key in voiceKeys) {
                if (strings[key].isNullOrBlank()) missing += "${dir.name}/$key"
            }
        }
        assertEquals("untranslated: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `no translation is left as the English text`() {
        // Copying English in is the usual way a locale looks complete and is
        // not. Short words legitimately coincide across languages, so this only
        // looks at the sentences, where a match is not a coincidence.
        val sentences = listOf(
            "voice_answer_off", "voice_answer_unsupported",
            "voice_answer_unknown", "voice_commands_enable_desc",
        )
        val english = stringsIn(File(resDir, "values"))
        val copied = mutableListOf<String>()
        for (dir in localeDirs()) {
            if (dir.name == "values") continue
            val strings = stringsIn(dir)
            for (key in sentences) {
                if (strings[key] == english[key]) copied += "${dir.name}/$key"
            }
        }
        assertEquals("still English: $copied", emptyList<String>(), copied)
    }

    @Test
    fun `every translation keeps the format arguments it is given`() {
        // A dropped %1$s does not fail the build. It throws when a rider asks
        // something the app cannot answer, which is the worst moment for it.
        val argRe = Regex("""%\d\$[sd]""")
        val english = stringsIn(File(resDir, "values"))
        val broken = mutableListOf<String>()
        for (dir in localeDirs()) {
            val strings = stringsIn(dir)
            for (key in voiceKeys) {
                val expected = argRe.findAll(english[key] ?: "").map { it.value }.toSortedSet()
                val actual = argRe.findAll(strings[key] ?: "").map { it.value }.toSortedSet()
                if (expected != actual) broken += "${dir.name}/$key expected $expected got $actual"
            }
        }
        assertEquals("format arguments lost: $broken", emptyList<String>(), broken)
    }
}
