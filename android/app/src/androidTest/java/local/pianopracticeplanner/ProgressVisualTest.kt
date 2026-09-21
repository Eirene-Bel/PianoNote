package local.pianopracticeplanner

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import local.pianopracticeplanner.domain.*
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProgressVisualTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name:String){
        val start=SystemClock.elapsedRealtime();compose.waitUntil(2000){SystemClock.elapsedRealtime()-start>350}
        val inst=InstrumentationRegistry.getInstrumentation()
        val dir=File(inst.targetContext.getExternalFilesDir(null),"progress-evidence").apply{mkdirs()}
        inst.uiAutomation.takeScreenshot().let{bitmap->File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    }
    @Test fun captureNativeScreensAndCheckControlsRemainReachable(){
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded&&vm.progressLoaded}
        compose.runOnIdle{vm.switchTab("cards");vm.changeQuizMode("read");vm.applyCardSettings(TrainingSettings(CardOptions("both",3,"staff")));vm.nextCard()}
        compose.waitUntil(15000){vm.scoreReady}
        compose.onNodeWithTag("score").assertIsDisplayed()
        compose.onNodeWithTag("card-next").assertIsDisplayed()
        compose.onNodeWithTag("card-settings").assertDoesNotExist()
        compose.onNodeWithTag("note-シ").assertIsDisplayed()
        capture("notes")
        compose.runOnIdle{vm.card.notes.forEachIndexed{index,note->val name=NoteEngine.name(note);vm.note(if(index==0)if(name=="ド")"レ"else "ド"else name)}}
        compose.waitUntil(5000){!vm.hasPendingResponses}
        compose.onNodeWithTag("note-シ").assertIsDisplayed()
        compose.onNodeWithTag("card-next").assertIsDisplayed()
        capture("score-feedback")
        compose.onNodeWithTag("mode-listen").performClick()
        compose.onNodeWithTag("hidden-score").assertIsDisplayed()
        compose.onNodeWithTag("play-notes").assertIsDisplayed()
        compose.onNodeWithTag("card-next").assertIsDisplayed()
        capture("listening")
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithText("出題・音量").performClick()
        capture("question-settings")
        compose.onNodeWithTag("apply-card-settings").performClick()
        compose.onNodeWithTag("nav-review").performClick();capture("accuracy")
        compose.onNodeWithText("練習カレンダー").performClick();capture("calendar")
        compose.onNodeWithTag("nav-settings").performClick();capture("transfer")
        compose.runOnIdle{vm.changeQuizMode("read");vm.applyCardSettings(TrainingSettings());vm.nextCard()}
    }
}
