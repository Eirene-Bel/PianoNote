package local.pianopracticeplanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import local.pianopracticeplanner.ui.PianoViewModel
import local.pianopracticeplanner.domain.*

class ProgressFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:PianoViewModel
    @Before fun setup(){
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded&&vm.progressLoaded}
        compose.runOnIdle{vm.switchTab("cards");vm.changeQuizMode("read");vm.applyCardSettings(TrainingSettings());vm.nextCard()}
        compose.waitUntil(15000){vm.scoreReady}
    }
    @After fun finish(){compose.runOnIdle{vm.pausePractice();vm.changeQuizMode("read")}}
    @Test fun reviewAndTransferAreDedicatedDestinations(){
        compose.onNodeWithTag("nav-review").performClick()
        compose.onNodeWithText("音符の正答率").assertExists()
        compose.onNodeWithText("練習カレンダー").performClick()
        compose.onNodeWithTag("practice-calendar").assertExists()
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithText("データを書き出す").assertExists()
        compose.onNodeWithText("データを読み込む").assertExists()
    }
    @Test fun retryCannotRewriteTheFirstAnswerAndFooterStaysPut(){
        val id=vm.card.id
        val correct=NoteEngine.name(vm.card.notes.single())
        val wrong=if(correct=="ド")"レ"else "ド"
        val before=compose.onNodeWithTag("card-next").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("note-$wrong").performClick()
        compose.waitUntil(5000){vm.attempts.value.any{it.id==id}}
        compose.onNodeWithTag("answer-slot-0").performClick()
        compose.onNodeWithTag("note-$correct").performClick()
        compose.runOnIdle{assertTrue(vm.correct==true);assertFalse(vm.attempts.value.single{it.id==id}.correct)}
        val after=compose.onNodeWithTag("card-next").fetchSemanticsNode().boundsInRoot
        assertEquals(before.bottom,after.bottom,1f)
        compose.activityRule.scenario.recreate()
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(15000){vm.scoreReady&&vm.progressLoaded}
        assertEquals(id,vm.card.id)
        compose.onNodeWithTag("answer-slot-0").performClick()
        compose.onNodeWithTag("note-$wrong").performClick()
        compose.runOnIdle{assertEquals(1,vm.attempts.value.count{it.id==id});assertFalse(vm.attempts.value.single{it.id==id}.correct)}
    }
    @Test fun listeningHidesScoreAndReferenceDoesNotUnlockAnswer(){
        compose.onNodeWithTag("mode-listen").performClick()
        compose.onNodeWithTag("hidden-score").assertExists()
        compose.onNodeWithTag("score").assertDoesNotExist()
        compose.onNodeWithTag("note-ド").assertIsNotEnabled()
        compose.onNodeWithTag("reference-c").performClick()
        compose.waitUntil(10000){!vm.playing}
        compose.runOnIdle{assertFalse(vm.heard)}
        compose.onNodeWithTag("play-notes").performClick()
        compose.waitUntil(10000){vm.heard&&!vm.playing}
        compose.onNodeWithTag("note-${NoteEngine.name(vm.card.notes.single())}").performClick()
        compose.onNodeWithTag("hidden-score").assertDoesNotExist()
        compose.onNodeWithTag("score").assertExists()
        compose.waitUntil(5000){vm.attempts.value.any{it.id==vm.card.id&&it.mode=="listen"&&it.correct}}
    }
    @Test fun audioHintIsExcludedAndPauseStopsAudioAndTime(){
        val id=vm.card.id
        val before=vm.activeSeconds
        compose.onNodeWithTag("play-notes").performClick()
        compose.waitUntil(10000){vm.heard&&!vm.playing}
        compose.waitUntil(5000){vm.activeSeconds>=before+2}
        compose.onNodeWithTag("note-${NoteEngine.name(vm.card.notes.single())}").performClick()
        compose.waitUntil(5000){vm.attempts.value.any{it.id==id}}
        compose.waitUntil(5000){!vm.hasPendingProgress}
        var gradedSeconds=0
        compose.runOnIdle{assertTrue(vm.attempts.value.single{it.id==id}.assisted);assertFalse(vm.tracking);gradedSeconds=vm.activeSeconds}
        compose.onNodeWithTag("play-notes").performClick()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        InstrumentationRegistry.getInstrumentation().runOnMainSync{assertFalse(vm.playing);assertFalse(vm.tracking)}
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle{assertFalse(vm.tracking);assertEquals(gradedSeconds,vm.activeSeconds)}
    }
}
