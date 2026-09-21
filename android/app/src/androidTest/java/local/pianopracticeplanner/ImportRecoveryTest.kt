package local.pianopracticeplanner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import local.pianopracticeplanner.data.*
import local.pianopracticeplanner.domain.PracticeInput
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class ImportRecoveryTest {
    private lateinit var db:PracticeDatabase
    private lateinit var repository:PracticeRepository
    @Before fun open(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build();repository=PracticeRepository(db)}
    @After fun close(){db.close()}
    @Test fun conflictsIdentifyTheRecordAndCanBeSkippedWithoutOverwriting()=runBlocking{
        val old=repository.save(PracticeInput("2026-09-21","ハノン","15"),null,null,UUID.randomUUID().toString())
        val fresh=old.copy(id=UUID.randomUUID().toString(),piece="新しい記録")
        val text=JSONObject().put("format","piano-practice").put("formatVersion",1).put("records",JSONArray(listOf(PracticeRepository.recordJson(old.copy(minutes=30)),PracticeRepository.recordJson(fresh)))).toString()
        val report=repository.previewImport(text)
        assertEquals(1,report.added);assertEquals("ハノン",report.conflicts.single().piece);assertTrue(report.conflicts.single().detail.contains("15分／ファイル30分"))
        try{repository.importJson(text);fail("Conflict silently accepted")}catch(_:IllegalArgumentException){}
        assertEquals(1,repository.records().size)
        assertEquals(1,repository.importJson(text,skipConflicts=true).added)
        assertEquals(old,repository.records().first{it.id==old.id});assertEquals(2,repository.records().size)
    }
    @Test fun invalidRecordReportsItsPositionAndPracticeName()=runBlocking{
        val row=PracticeRecord(UUID.randomUUID().toString(),"2026-09-21","エリーゼ",15)
        val invalid=PracticeRepository.recordJson(row).put("minutes",0)
        val text=JSONObject().put("format","piano-practice").put("formatVersion",1).put("records",JSONArray(listOf(invalid))).toString()
        try{repository.previewImport(text);fail("Invalid record accepted")}catch(e:IllegalArgumentException){assertTrue(e.message!!.contains("1件目"));assertTrue(e.message!!.contains("エリーゼ"))}
        assertTrue(repository.records().isEmpty())
    }
}
