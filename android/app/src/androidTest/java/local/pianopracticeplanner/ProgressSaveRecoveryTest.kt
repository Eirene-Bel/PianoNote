package local.pianopracticeplanner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import local.pianopracticeplanner.data.PracticeDatabase
import local.pianopracticeplanner.data.PracticeRepository
import local.pianopracticeplanner.data.ProgressOutboxStore
import local.pianopracticeplanner.domain.NoteEngine
import local.pianopracticeplanner.ui.PianoViewModel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ProgressSaveRecoveryTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Test fun failedFirstAnswerSurvivesWithoutSavedStateAndIsStoredOnceAfterRecreation() {
        val db=Room.inMemoryDatabaseBuilder(compose.activity,PracticeDatabase::class.java).build()
        val repo=PracticeRepository(db)
        val firstStore=ViewModelStore()
        val outboxName="recovery-${UUID.randomUUID()}"
        val outbox=ProgressOutboxStore(compose.activity,outboxName)
        lateinit var first:PianoViewModel
        val day="2026-09-21"
        try {
            compose.runOnIdle {
                first=PianoViewModel(repo,SavedStateHandle(),today={day},outbox=outbox)
                firstStore.put("practice",first)
            }
            compose.waitUntil(10000){first.loaded && first.progressLoaded}
            compose.runOnIdle {
                first.switchTab("cards")
                first.rendered(first.card.id,null)
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_first_answer BEFORE INSERT ON note_attempts BEGIN SELECT RAISE(ABORT, 'injected'); END")
            }
            val id=first.card.id
            val correct=NoteEngine.name(first.card.notes.single())
            val wrong=if(correct=="ド")"レ" else "ド"
            compose.runOnIdle {first.note(wrong)}
            compose.waitUntil(10000){first.hasPendingResponses && first.retryKind=="progress"}
            compose.runOnIdle {
                assertEquals(false,first.correct)
                assertTrue(first.hasPendingResponses)
                first.nextQuestion()
                assertEquals(id,first.card.id)
            }
            assertTrue(runBlocking{db.practice().allAttempts()}.isEmpty())
            val savedPayload=outbox.read()!!
            val pending=JSONObject(savedPayload).getJSONArray("attempts").getJSONObject(0)
            assertEquals(id,pending.getString("id"))
            assertFalse(pending.getBoolean("correct"))
            compose.runOnIdle {firstStore.clear();db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_first_answer")}

            val secondStore=ViewModelStore()
            lateinit var second:PianoViewModel
            try {
                compose.runOnIdle {
                    second=PianoViewModel(repo,SavedStateHandle(),today={day},outbox=outbox)
                    secondStore.put("practice",second)
                }
                compose.waitUntil(10000){second.loaded && second.progressLoaded && !second.hasPendingResponses && second.attempts.value.any{it.id==id}}
                compose.runOnIdle {
                    assertEquals(1,second.attempts.value.count{it.id==id})
                    assertFalse(second.attempts.value.single{it.id==id}.correct)
                }
                assertEquals(1,runBlocking{db.practice().allAttempts()}.count{it.id==id})
                assertEquals(0,JSONObject(outbox.read()!!).getJSONArray("attempts").length())
            } finally {compose.runOnIdle{secondStore.clear()}}
        } finally {compose.runOnIdle{firstStore.clear()};db.close();compose.activity.deleteSharedPreferences(outboxName)}
    }
}
