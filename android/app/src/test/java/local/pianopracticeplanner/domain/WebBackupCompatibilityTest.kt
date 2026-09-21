package local.pianopracticeplanner.domain

import local.pianopracticeplanner.data.PracticeRepository
import org.junit.Assert.*
import org.junit.Test

class WebBackupCompatibilityTest {
    @Test fun webExportIsAcceptedWithoutLosingAndroidFields(){
        val text=requireNotNull(javaClass.classLoader!!.getResourceAsStream("web-transfer-v2.json")).bufferedReader().use{it.readText()}
        val payload=PracticeRepository.parseTransfer(text)
        assertEquals("旧形式の範囲",payload.records.single().practiceRange)
        assertEquals("旧形式のメモ",payload.records.single().memo)
        assertEquals("左手をゆっくり",payload.records.single().difficultParts)
        assertEquals(2L,payload.records.single().revision)
        assertEquals("both",payload.attempts.single().clefOption)
        assertEquals("18,20,22",payload.attempts.single().notes)
        assertEquals("listen",payload.settings!!.mode)
        assertEquals(90,payload.sessions.single().seconds)
        assertTrue(payload.draft!!.contains("引き継ぎ途中"))
    }
}
