package local.pianopracticeplanner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import local.pianopracticeplanner.data.*
import local.pianopracticeplanner.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProgressRepositoryTest {
    private lateinit var db:PracticeDatabase
    private lateinit var repo:PracticeRepository
    @Before fun open(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build();repo=PracticeRepository(db)}
    @After fun close(){db.close()}

    @Test fun firstAnswerAndSessionAreIdempotentAndConflictingRetryFails()=runBlocking {
        val a=NoteAttempt(UUID.randomUUID().toString(),"2026-09-21",100,"read","treble",1,"staff","treble","30",false,false)
        repo.recordAttempt(a);repo.recordAttempt(a)
        assertEquals(1,repo.attempts.first().size)
        try{repo.recordAttempt(a.copy(correct=true));fail("First answer changed")}catch(_:IllegalArgumentException){}
        assertFalse(repo.attempts.first().single().correct)
        val s=NoteSession(UUID.randomUUID().toString(),"2026-09-21",15)
        repo.recordSession(s);repo.recordSession(s)
        try{repo.recordSession(s.copy(seconds=30));fail("Session changed")}catch(_:IllegalArgumentException){}
        assertEquals(15,repo.sessions.first().single().seconds)
    }

    @Test fun v2TransferMergesWithoutReplacingLocalSettingsOrDraft()=runBlocking {
        val a=NoteAttempt(UUID.randomUUID().toString(),"2026-09-21",100,"read","treble",1,"staff","treble","30",true,false)
        val s=NoteSession(UUID.randomUUID().toString(),"2026-09-21",90)
        repo.recordAttempt(a);repo.recordSession(s)
        repo.writeProgressSettings(ProgressSettings(mode="listen",recentN=50,trainingMode="AUTO",interval=8))
        repo.writeDraft(JSONObject().put("input",PracticeRepository.inputJson(PracticeInput("2026-09-21","練習","15")))
            .put("editingId","").put("revision",0).put("operationId",UUID.randomUUID().toString()).put("goalTouched",false).toString())
        val json=repo.exportJson()
        assertEquals(2,JSONObject(json).getInt("formatVersion"))
        val other=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build()
        try {
            val target=PracticeRepository(other)
            target.writeProgressSettings(ProgressSettings(recentN=10))
            target.writeDraft("local")
            val preview=target.previewImport(json)
            assertEquals(1,preview.attemptsAdded);assertEquals(1,preview.sessionsAdded)
            assertTrue(preview.settingsAvailable);assertTrue(preview.draftAvailable)
            target.importJson(json)
            assertEquals(10,target.readProgressSettings()!!.recentN)
            assertEquals("local",target.readDraft())
            assertEquals(a,target.attempts.first().single())
            assertEquals(s,target.sessions.first().single())
            assertEquals(0,target.importJson(json).attemptsAdded)
            val empty=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build()
            try {
                val restored=PracticeRepository(empty)
                restored.importJson(json)
                assertEquals("AUTO",restored.readProgressSettings()!!.trainingMode)
                assertEquals(8,restored.readProgressSettings()!!.interval)
            }finally{empty.close()}
        }finally{other.close()}
    }

    @Test fun malformedV2PayloadNeverPartiallyImports()=runBlocking {
        repo.recordSession(NoteSession(UUID.randomUUID().toString(),"2026-09-21",20))
        val root=JSONObject(repo.exportJson())
        root.getJSONArray("sessions").getJSONObject(0).put("seconds",-1)
        try{repo.importJson(root.toString());fail("Invalid seconds accepted")}catch(_:IllegalArgumentException){}
        assertTrue(repo.attempts.first().isEmpty())
        assertEquals(1,repo.sessions.first().size)
    }

    @Test fun malformedDraftIsRejectedBeforeFreshDeviceMerge()=runBlocking {
        val source=JSONObject(repo.exportJson())
        source.put("draft","hello")
        val target=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build()
        try {
            val imported=PracticeRepository(target)
            try{imported.importJson(source.toString());fail("Malformed draft accepted")}catch(_:IllegalArgumentException){}
            assertNull(imported.readDraft())
            assertTrue(imported.records().isEmpty())
        }finally{target.close()}
    }

    @Test fun conflictsAreScopedPerTableAndPreviewCountsMatchMerge()=runBlocking {
        val id=UUID.randomUUID().toString()
        val local=PracticeRecord(id,"2026-09-21","端末側",15)
        db.practice().insert(local)
        val incoming=local.copy(piece="ファイル側")
        val attempt=NoteAttempt(id,"2026-09-21",100,"read","treble",1,"staff","treble","30",true,false)
        val source=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build()
        val json=try {
            val foreign=PracticeRepository(source)
            source.practice().insert(incoming)
            foreign.recordAttempt(attempt)
            foreign.exportJson()
        }finally{source.close()}
        val preview=repo.previewImport(json)
        assertEquals(1,preview.conflicts.size)
        assertEquals(1,preview.attemptsAdded)
        repo.importJson(json,skipConflicts=true)
        assertEquals(local,repo.records().single())
        assertEquals(attempt,repo.attempts.first().single())
    }

    @Test fun importNormalizesPaddedRecordText()=runBlocking {
        val row=PracticeRecord(UUID.randomUUID().toString(),"2026-09-21","  ハノン  ",15,practiceRange="  8小節 ",memo=" メモ ")
        val source=JSONObject().put("format","piano-practice").put("formatVersion",1)
            .put("records",JSONArray(listOf(PracticeRepository.recordJson(row)))).toString()
        repo.importJson(source)
        val actual=repo.records().single()
        assertEquals("ハノン",actual.piece)
        assertEquals("8小節",actual.practiceRange)
        assertEquals("メモ",actual.memo)
    }
}
