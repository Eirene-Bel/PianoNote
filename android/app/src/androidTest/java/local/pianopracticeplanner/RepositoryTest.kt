package local.pianopracticeplanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import local.pianopracticeplanner.data.*
import local.pianopracticeplanner.domain.PracticeInput
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: PracticeDatabase
    private lateinit var repository: PracticeRepository
    @Before fun open(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build();repository=PracticeRepository(db)}
    @After fun close(){db.close()}
    @Test fun writesAreIdempotentAndEditsPreserveLegacyColumns()=runBlocking {
        val input=PracticeInput(date="2026-09-21",piece="ハノン",minutes="15",difficultParts="右手\n改善なし")
        val op=UUID.randomUUID().toString()
        val first=repository.save(input,null,null,op)
        db.practice().update(first.copy(practiceRange="8小節",memo="旧メモ",next="左手"))
        assertEquals(first.id,repository.save(input,null,null,op).id)
        assertEquals(1,repository.records().size)
        val updated=repository.save(input.copy(minutes="30"),first.id,first.revision,UUID.randomUUID().toString())
        assertEquals(30,updated.minutes);assertEquals(first.revision+1,updated.revision)
        assertEquals("8小節",updated.practiceRange);assertEquals("旧メモ",updated.memo);assertEquals("左手",updated.next)
        try {repository.save(input,first.id,first.revision,UUID.randomUUID().toString());fail("Stale edit accepted")}catch(_:IllegalArgumentException){}
        repository.delete(updated)
        assertTrue(repository.records().isEmpty())
        try {repository.save(input,null,null,op);fail("Deleted retry resurrected")}catch(_:IllegalArgumentException){}
    }
    @Test fun exportImportIsAtomicAndDoesNotResurrectDeletedImports()=runBlocking {
        repository.save(PracticeInput(date="2026-09-21",piece="練習",minutes="20",finishingImage="軽やかに"),null,null,UUID.randomUUID().toString())
        val json=repository.exportJson()
        assertEquals(0,repository.importJson(json).added)
        val row=repository.records().single();repository.delete(row)
        assertEquals(0,repository.importJson(json).added);assertTrue(repository.records().isEmpty())
        try {repository.importJson("{\"formatVersion\":99}");fail("Unknown format accepted")}catch(_:IllegalArgumentException){}
        assertTrue(repository.records().isEmpty())
    }
    @Test fun restoredBackupRetainsDeletedImportSuppression()=runBlocking {
        val row=repository.save(PracticeInput(date="2026-09-21",piece="移行対象",minutes="15"),null,null,UUID.randomUUID().toString())
        val original=repository.exportJson();repository.delete(row);val backup=repository.exportJson()
        val second=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PracticeDatabase::class.java).build()
        try {
            val restored=PracticeRepository(second);restored.importJson(backup)
            assertEquals(0,restored.importJson(original).added)
            assertTrue(restored.records().isEmpty())
        }finally{second.close()}
    }
    @Test fun importRollsBackAllRowsIfDatabaseInsertionFails()=runBlocking {
        val a=PracticeRecord(UUID.randomUUID().toString(),"2026-09-21","成功候補",15)
        val b=PracticeRecord(UUID.randomUUID().toString(),"2026-09-21","失敗候補",20)
        val payload=org.json.JSONObject().put("format","piano-practice").put("formatVersion",1)
            .put("records",org.json.JSONArray(listOf(PracticeRepository.recordJson(a),PracticeRepository.recordJson(b)))).toString()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER injected_failure BEFORE INSERT ON records WHEN NEW.piece='失敗候補' BEGIN SELECT RAISE(ABORT,'injected'); END")
        try{repository.importJson(payload);fail("Partial import accepted")}catch(_:android.database.sqlite.SQLiteException){}
        assertTrue(repository.records().isEmpty())
    }
    @Test fun fileDraftSurvivesDatabaseReopening()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val name="draft-test-${UUID.randomUUID()}.db"
        val first=Room.databaseBuilder(context,PracticeDatabase::class.java,name).build()
        try{PracticeRepository(first).writeDraft("日本語の下書き\n次の行")}finally{first.close()}
        val reopened=Room.databaseBuilder(context,PracticeDatabase::class.java,name).build()
        try{assertEquals("日本語の下書き\n次の行",PracticeRepository(reopened).readDraft())}finally{reopened.close();context.deleteDatabase(name)}
    }
}
