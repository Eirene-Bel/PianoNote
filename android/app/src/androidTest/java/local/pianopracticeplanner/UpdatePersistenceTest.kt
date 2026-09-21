package local.pianopracticeplanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import local.pianopracticeplanner.data.PracticeRecord
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdatePersistenceTest {
    private val row=PracticeRecord("10000000-0000-4000-8000-000000000036","2026-09-21","更新保持の検証用",15,
        memo="旧メモも保持",createdAt="2026-09-21T00:00:00Z",difficultParts="右手\nゆっくり練習",finishingImage="軽やかに")
    private val db get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PianoApplication).database
    @Test fun seedBeforeUpdate()=runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("updateStage")=="seed")
        val old=db.practice().find(row.id)
        if(old==null)db.practice().insert(row) else assertEquals(row,old)
        assertEquals(row,db.practice().find(row.id))
    }
    @Test fun verifyAfterUpdate()=runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("updateStage")=="verify")
        assertEquals(row,db.practice().find(row.id))
        db.practice().delete(row.id)
        assertNull(db.practice().find(row.id))
    }
}
