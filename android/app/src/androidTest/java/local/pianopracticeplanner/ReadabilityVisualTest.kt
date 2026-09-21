package local.pianopracticeplanner

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import local.pianopracticeplanner.data.NoteAttempt
import local.pianopracticeplanner.domain.*
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID

class ReadabilityVisualTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name:String){
        val start=android.os.SystemClock.elapsedRealtime();compose.waitUntil(1500){android.os.SystemClock.elapsedRealtime()-start>350}
        val inst=InstrumentationRegistry.getInstrumentation();val dir=File(inst.targetContext.getExternalFilesDir(null),"readability-evidence").apply{mkdirs()}
        inst.uiAutomation.takeScreenshot().let{image->File(dir,"$name.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()}
    }
    @Test fun twentyAnswersAndMinuteResetRemainReadable(){
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded&&vm.progressLoaded}
        val ids=(0 until 20).map{UUID.randomUUID().toString()}
        val now=System.currentTimeMillis()
        val app=compose.activity.application as PianoApplication
        try{
            runBlocking{ids.forEachIndexed{i,id->vm.repository.recordAttempt(NoteAttempt(id,LocalDate.now().toString(),now+i,"read","treble",1,"staff","treble","30",i%2==0||i==19,false))}}
            compose.waitUntil(5000){vm.attempts.value.count{it.id in ids}==20}
            compose.runOnIdle{vm.changeQuizMode("read");vm.applyCardSettings(TrainingSettings());vm.nextCard(timed=false);vm.changeRecentN(20);vm.switchTab("review")}
            compose.onNodeWithTag("recent-answer-grid").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("recent-answer-0",useUnmergedTree=true).assertIsDisplayed()
            compose.onNodeWithTag("recent-answer-19",useUnmergedTree=true).assertIsDisplayed()
            capture("recent-answers")
            compose.runOnIdle{vm.switchTab("practice");vm.clearInput();vm.changeInput(vm.input.copy(piece="ハノン・左手",minutes="45",difficultParts="ゆっくり練習"))}
            compose.onNodeWithTag("reset-minutes").performScrollTo().assertIsDisplayed().performClick()
            compose.runOnIdle{check(vm.input.minutes.isEmpty());check(vm.input.piece=="ハノン・左手");check(vm.input.difficultParts=="ゆっくり練習")}
            compose.onNodeWithTag("save").assertIsDisplayed()
            capture("minutes-reset")
        }finally{
            compose.runOnIdle{vm.clearInput();vm.pausePractice()}
            runBlocking{ids.forEach{id->app.database.openHelper.writableDatabase.execSQL("DELETE FROM note_attempts WHERE id=?",arrayOf(id))}}
        }
    }
}
