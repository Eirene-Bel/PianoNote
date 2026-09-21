package local.pianopracticeplanner.i18n

import local.pianopracticeplanner.domain.NoteCard
import local.pianopracticeplanner.domain.NoteEngine
import local.pianopracticeplanner.domain.PracticeInput
import local.pianopracticeplanner.domain.PracticeValidator
import local.pianopracticeplanner.domain.TrainingMode
import org.junit.Assert.*
import org.junit.After
import org.junit.Test

class I18nTest {
    @After fun resetLocale() { I18n.language = "ja" }

    @Test fun regionalAndScriptLocalesResolveWithoutMistakingTraditionalChinese() {
        mapOf("ja-JP" to "ja", "en-GB" to "en", "zh-CN" to "zh-Hans", "zh-TW" to "zh-Hant",
            "zh-HK" to "zh-Hant", "zh-MO" to "zh-Hant", "zh-Hans-TW" to "zh-Hans",
            "zh-Hant-CN" to "zh-Hant", "pt-BR" to "pt", "fr-CA" to "fr", "ar" to "en")
            .forEach { (tag, expected) -> assertEquals(expected, I18n.resolve(tag)) }
    }

    @Test fun everyLocaleHasEveryMessageAndPreservesPlaceholders() {
        val source = Catalogs.get("ja")
        val placeholder = Regex("\\{\\d+\\}")
        I18n.languages.keys.filter { it.isNotEmpty() }.forEach { language ->
            val messages = Catalogs.get(language)
            assertEquals(language, source.keys, messages.keys)
            source.forEach { (key, value) ->
                assertTrue("$language/$key", messages.getValue(key).isNotBlank())
                assertEquals("$language/$key", placeholder.findAll(value).map { it.value }.sorted().toList(),
                    placeholder.findAll(messages.getValue(key)).map { it.value }.sorted().toList())
            }
        }
    }

    @Test fun languageChangesLabelsButNeverStoredNotesOrUserContent() {
        val canonical = (0..6).map(NoteEngine::name)
        val input = PracticeInput(date="2026-09-21", piece="日本語 / Français {1}", minutes="15")
        I18n.languages.keys.filter { it.isNotEmpty() }.forEach { language ->
            I18n.language = language
            assertEquals(canonical, (0..6).map(NoteEngine::name))
            assertEquals(canonical, NoteEngine.parse(canonical.joinToString(" ") { I18n.note(it) }))
            assertTrue(NoteEngine.check(NoteCard("treble", listOf(28,30,32)), listOf("ド","ミ","ソ").joinToString(" ") { I18n.note(it) }))
            assertEquals(input, PracticeValidator.validate(input))
        }
        I18n.language = "ja"
        val japanese = TrainingMode.ANSWER.label
        I18n.language = "en"
        assertNotEquals(japanese, TrainingMode.ANSWER.label)
    }

    @Test fun retainedMessagesRefreshWhileEqualUserStringsStayUntouched() {
        I18n.language = "ja"
        val label = tr("s010")
        val userContent = String(label.toCharArray())
        val generated = tr("s007", label, 80)
        val withUserContent = tr("s007", userContent, 80)
        I18n.language = "en"
        assertEquals(tr("s007", tr("s010"), 80), refreshTranslation(generated))
        assertEquals(tr("s007", userContent, 80), refreshTranslation(withUserContent))
        assertEquals(userContent, refreshTranslation(userContent))
        assertEquals(listOf("ド", "レ", "ミ"), NoteEngine.parse("Ｃ Ｄ Ｅ"))
    }

    @Test fun adjacentLetterNotesFollowActiveNotation() {
        for (language in listOf("en", "de")) {
            I18n.language = language
            for (answer in listOf("FA", "fa", "ＦＡ", "F A")) {
                assertEquals(listOf("ファ", "ラ"), NoteEngine.parse(answer))
                assertTrue(NoteEngine.check(NoteCard("treble", listOf(31,33)), answer))
            }
            assertEquals(listOf("ファ", "ラ", "シ"), NoteEngine.parse(if (language == "de") "FAH" else "FAB"))
        }
        for (language in listOf("ja", "zh-Hans", "zh-Hant", "ko", "es", "fr", "pt")) {
            I18n.language = language
            assertEquals(listOf("ファ"), NoteEngine.parse("Fa"))
            assertEquals(listOf("ファ", "ラ"), NoteEngine.parse("FaLa"))
        }
    }

    @Test fun formattingNeverInterpretsPlaceholdersInsideUserText() {
        I18n.language = "en"
        assertTrue(tr("s007", "My {1} piece", 80).contains("My {1} piece"))
        assertThrows(IllegalArgumentException::class.java) { NoteEngine.parse("C XYZ E") }
    }
}
