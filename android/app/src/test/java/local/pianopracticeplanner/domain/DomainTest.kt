package local.pianopracticeplanner.domain

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DomainTest {
    private fun fixture(name: String) = JSONObject(javaClass.classLoader!!.getResource(name)!!.readText())

    @Test fun knownPitchesAndRangesMatchDesktopFixtures() {
        val data = fixture("note-cases.json")
        val pitches = data.getJSONArray("pitches")
        for (i in 0 until pitches.length()) {
            val row = pitches.getJSONObject(i)
            assertEquals(row.getString("name"), NoteEngine.name(row.getInt("step")))
            assertEquals(row.getString("key"), NoteEngine.key(row.getInt("step")))
        }
        val ranges = data.getJSONArray("ranges")
        for (i in 0 until ranges.length()) {
            val row = ranges.getJSONObject(i)
            assertEquals(row.getInt("low")..row.getInt("high"), NoteEngine.range(row.getString("clef"), row.getString("range")))
        }
    }

    @Test fun answersNormalizeJapaneseAndFaWithoutDroppingInvalidCharacters() {
        assertEquals(listOf("ド", "ファ", "シ"), NoteEngine.parse("ど ふぁ・し"))
        assertEquals(listOf("ド", "レ", "ミ"), NoteEngine.parse("ﾄﾞﾚﾐ"))
        assertThrows(IllegalArgumentException::class.java) { NoteEngine.parse("ドXYZミ") }
        assertThrows(IllegalArgumentException::class.java) { NoteEngine.parse("") }
        val card = NoteCard("treble", listOf(28, 30, 32))
        assertTrue(NoteEngine.check(card, "ド ミ ソ"))
        assertFalse(NoteEngine.check(card, "ソ ミ ド"))
        assertThrows(IllegalArgumentException::class.java) { NoteEngine.check(card, "ド") }
    }

    @Test fun unlimitedGeneratedCardsStayWithinAllOptionsAndAvoidConsecutiveDuplicates() {
        for (clef in listOf("treble", "bass", "both")) for (size in listOf(0,1,2,3)) for (range in listOf("staff","basic","wide")) {
            val engine = NoteEngine(Random(7))
            var last: NoteCard? = null
            repeat(250) {
                val card = engine.next(CardOptions(clef,size,range),last)
                assertTrue(clef == "both" || card.clef == clef)
                assertTrue(card.notes.all { it in NoteEngine.range(card.clef, range) })
                assertTrue(if (size == 0) card.notes.size in 1..3 else card.notes.size == size)
                assertNotEquals(last?.signature, card.signature)
                last = card
            }
        }
    }

    @Test fun recordValidationHandlesDatesBoundsAndOptionalFields() {
        val valid = PracticeInput(date="2024-02-29",piece="  ハノン  ",minutes="15",difficultParts="右手\n改善なし")
        assertEquals("ハノン", PracticeValidator.validate(valid).piece)
        for (bad in listOf(valid.copy(date="2026-02-30"),valid.copy(piece="　"),valid.copy(minutes="0"),valid.copy(minutes="1441"),valid.copy(minutes="1.5"))) {
            assertThrows(IllegalArgumentException::class.java) { PracticeValidator.validate(bad) }
        }
        assertEquals("",PracticeValidator.validate(valid).unpracticedParts)
        assertThrows(IllegalArgumentException::class.java) { PracticeValidator.validate(valid.copy(finishingImage="あ".repeat(2001))) }
    }

    @Test fun supplementaryCharactersUseCodePointLimitRatherThanUtf16Length() {
        val input=PracticeInput(date="2026-09-21",piece="🎹".repeat(80),minutes="15")
        assertEquals(input.piece,PracticeValidator.validate(input).piece)
        assertThrows(IllegalArgumentException::class.java) { PracticeValidator.validate(input.copy(piece="🎹".repeat(81))) }
    }
}
