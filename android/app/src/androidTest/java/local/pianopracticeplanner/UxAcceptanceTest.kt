package local.pianopracticeplanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import local.pianopracticeplanner.ui.PianoViewModel
import local.pianopracticeplanner.data.*
import local.pianopracticeplanner.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID
import java.io.File
import android.graphics.Bitmap

class UxAcceptanceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:PianoViewModel
    @Before fun prepare(){
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded}
        compose.runOnIdle{androidx.core.view.WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).hide(androidx.core.view.WindowInsetsCompat.Type.ime());compose.activity.currentFocus?.clearFocus()}
        compose.waitUntil(5000){androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())!=true}
        val hidden=android.os.SystemClock.elapsedRealtime();compose.waitUntil(2000){android.os.SystemClock.elapsedRealtime()-hidden>400}
        compose.runOnIdle{vm.clearInput();vm.applyCardSettings(TrainingSettings());vm.nextCard()}
    }
    @After fun cleanup(){compose.runOnIdle{vm.stopAuto();vm.clearInput();vm.applyCardSettings(TrainingSettings())}}
    private fun cards(){compose.runOnIdle{vm.switchTab("cards")};compose.waitUntil(15000){vm.scoreReady}}
    private fun capture(name:String){
        val frame=android.os.SystemClock.elapsedRealtime();compose.waitUntil(1500){android.os.SystemClock.elapsedRealtime()-frame>250}
        val inst=InstrumentationRegistry.getInstrumentation();val dir=File(inst.targetContext.getExternalFilesDir(null),"ux-evidence").apply{mkdirs()}
        inst.uiAutomation.takeScreenshot().let{bitmap->File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    }
    @Test fun scoreHasAVisibleDrawingAreaAfterAnswer(){
        cards()
        compose.runOnIdle{vm.note(NoteEngine.name(vm.card.notes.single()))}
        val dimensions=java.util.concurrent.atomic.AtomicReference<String?>(null)
        fun find(view:android.view.View):android.webkit.WebView?{
            if(view is android.webkit.WebView)return view
            if(view is android.view.ViewGroup)for(i in 0 until view.childCount)find(view.getChildAt(i))?.let{return it}
            return null
        }
        compose.runOnIdle{find(compose.activity.window.decorView)!!.evaluateJavascript("JSON.stringify({svg:document.querySelector('svg').getBoundingClientRect().toJSON(),viewport:innerHeight})"){dimensions.set(it)}}
        compose.waitUntil(5000){dimensions.get()!=null}
        val raw=org.json.JSONTokener(dimensions.get()).nextValue() as String
        val svg=org.json.JSONObject(raw).getJSONObject("svg")
        assertTrue("Score drawing has no height",svg.getDouble("height")>50)
        assertTrue("Score drawing has no width",svg.getDouble("width")>100)
        val start=android.os.SystemClock.elapsedRealtime();compose.waitUntil(2000){android.os.SystemClock.elapsedRealtime()-start>400}
        val bounds=compose.onNodeWithTag("score").fetchSemanticsNode().boundsInRoot
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        var ink=0
        for(y in bounds.top.toInt().coerceAtLeast(0) until bounds.bottom.toInt().coerceAtMost(bitmap.height) step 3)
            for(x in bounds.left.toInt().coerceAtLeast(0) until bounds.right.toInt().coerceAtMost(bitmap.width) step 3){
                val pixel=bitmap.getPixel(x,y)
                if(android.graphics.Color.red(pixel)<100&&android.graphics.Color.green(pixel)<120&&android.graphics.Color.blue(pixel)<140)ink++
            }
        bitmap.recycle();assertTrue("The notation area is blank",ink>20);capture("score-settled")
    }
    @Test fun oneNoteNeedsOnlyNoteAndNextAndNothingMoves(){
        cards()
        compose.onNodeWithTag("score").assertIsDisplayed()
        listOf("ド","レ","ミ","ファ","ソ","ラ","シ").forEach{compose.onNodeWithTag("note-$it").assertIsDisplayed()}
        compose.onNodeWithTag("answer-backspace").assertIsDisplayed()
        val next=compose.onNodeWithTag("card-next").fetchSemanticsNode().boundsInRoot
        val key=compose.onNodeWithTag("note-ド").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("note-${NoteEngine.name(vm.card.notes.single())}").performClick()
        compose.runOnIdle{assertEquals(true,vm.correct)}
        assertEquals(next,compose.onNodeWithTag("card-next").fetchSemanticsNode().boundsInRoot)
        assertEquals(key,compose.onNodeWithTag("note-ド").fetchSemanticsNode().boundsInRoot)
        capture("cards-answer")
        val previous=vm.card.id;compose.onNodeWithTag("card-next").performClick()
        compose.runOnIdle{assertNotEquals(previous,vm.card.id);assertEquals("",vm.answer.text)}
    }
    @Test fun incorrectNoteCanBeCorrectedIndividually(){
        compose.runOnIdle{vm.configure(CardOptions("treble",3,"staff"));vm.nextCard()};cards()
        val correct=vm.card.notes.map{NoteEngine.name(it)};val wrong=if(correct[1]=="ド")"レ"else "ド"
        compose.runOnIdle{vm.note(correct[0]);vm.note(wrong);vm.note(correct[2]);assertEquals(false,vm.correct)}
        compose.onNodeWithTag("answer-slot-1").assertContentDescriptionContains("正解は${correct[1]}",substring=true)
        compose.onNodeWithTag("answer-slot-1").performClick();compose.onNodeWithTag("note-${correct[1]}").performClick()
        compose.runOnIdle{assertEquals(true,vm.correct)}
        compose.onNodeWithTag("answer-backspace").performClick()
        compose.runOnIdle{assertEquals(2,vm.answerTokens.size);assertNull(vm.correct)}
    }
    @Test fun saveStaysVisibleWhileOptionalReflectionIsOpen(){
        compose.runOnIdle{vm.switchTab("practice")}
        compose.onNodeWithTag("piece").assertIsDisplayed()
        compose.onNodeWithTag("minutes").assertIsDisplayed()
        val before=compose.onNodeWithTag("save").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        capture("practice-minimal")
        compose.onNodeWithTag("expand-difficult").performScrollTo().performClick()
        compose.runOnIdle{vm.changeText("difficult",TextFieldValue("練習したこと\n".repeat(20)))}
        compose.onNodeWithTag("save").assertIsDisplayed()
        // Draft status may grow upward; the bottom action keeps its position.
        assertEquals(before.bottom,compose.onNodeWithTag("save").fetchSemanticsNode().boundsInRoot.bottom)
        compose.waitUntil(5000){vm.draftStatus.contains("下書き保存済み")}
        compose.onNodeWithTag("draft-status").assertTextContains("記録は未保存",substring=true)
        capture("practice-draft")
        compose.runOnIdle{vm.clearInput()}
        compose.onNodeWithTag("difficult").assertDoesNotExist()
    }
    @Test fun keyboardDoesNotHideTheFocusedInputOrSave(){
        compose.runOnIdle{vm.switchTab("practice")}
        compose.onNodeWithTag("piece").performClick().performTextInput("右手の練習")
        compose.waitUntil(10000){androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true}
        val shown=android.os.SystemClock.elapsedRealtime();compose.waitUntil(2000){android.os.SystemClock.elapsedRealtime()-shown>600}
        assertTrue(androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true)
        val field=compose.onNodeWithTag("piece").assertIsDisplayed()
        val full=field.getUnclippedBoundsInRoot()
        val fullHeight=(full.bottom-full.top).value*compose.activity.resources.displayMetrics.density
        assertTrue("Focused field is clipped by the keyboard/footer",field.fetchSemanticsNode().boundsInRoot.height>=fullHeight-2f)
        compose.onNodeWithTag("save").assertIsDisplayed();capture("practice-keyboard")
    }
    @Test fun modesExposeOnlyTheirOwnActions(){
        cards();compose.onNodeWithTag("card-auto").assertDoesNotExist();compose.onNodeWithTag("card-reveal").assertDoesNotExist()
        compose.runOnIdle{vm.applyCardSettings(TrainingSettings(mode=TrainingMode.FLASHCARD));vm.nextCard()}
        compose.waitUntil(10000){vm.scoreReady};compose.onNodeWithTag("card-reveal").assertIsDisplayed();compose.onNodeWithTag("note-ド").assertDoesNotExist()
        compose.onNodeWithTag("card-reveal").performClick();capture("flashcard")
        compose.runOnIdle{vm.applyCardSettings(TrainingSettings(mode=TrainingMode.AUTO));vm.nextCard()}
        compose.waitUntil(10000){vm.scoreReady};compose.onNodeWithTag("card-auto").assertIsDisplayed();compose.onNodeWithTag("card-response").assertDoesNotExist();capture("auto")
    }
    @Test fun recentPracticeCarriesTimeAndGoalWithoutEditingItsSource()=runBlocking{
        val original=vm.repository.save(PracticeInput("2026-09-20","UX継続-${UUID.randomUUID()}","20",finishingImage="歌うように"),null,null,UUID.randomUUID().toString())
        compose.waitUntil(5000){vm.records.value.any{it.id==original.id}}
        compose.runOnIdle{vm.useRecent(original);assertEquals("20",vm.input.minutes);assertEquals("歌うように",vm.input.finishingImage);assertEquals(java.time.LocalDate.now().toString(),vm.input.date);vm.changeText("finishing",TextFieldValue("より軽やかに"));vm.save()}
        compose.waitUntil(10000){!vm.busy}
        assertEquals("歌うように",vm.repository.records().first{it.id==original.id}.finishingImage)
        assertTrue(vm.repository.records().any{it.id!=original.id&&it.piece==original.piece&&it.finishingImage=="より軽やかに"})
    }
    @Test fun dayRolloverUpdatesEmptyInputButPreservesRealDraft(){
        val db=Room.inMemoryDatabaseBuilder(compose.activity,PracticeDatabase::class.java).build();val store=ViewModelStore()
        var day="2026-09-20";lateinit var model:PianoViewModel
        try{
            compose.runOnIdle{model=PianoViewModel(PracticeRepository(db),SavedStateHandle(),today={day});store.put("date",model)}
            compose.waitUntil(5000){model.loaded}
            compose.runOnIdle{day="2026-09-21";model.onResume();assertEquals(day,model.input.date);model.changeInput(model.input.copy(piece="入力途中"));day="2026-09-22";model.onResume();assertEquals("2026-09-21",model.input.date);assertEquals("入力途中",model.input.piece)}
        }finally{compose.runOnIdle{store.clear()};db.close()}
    }
    @Test fun settingsPersistAndStartWithAnEasyPreset(){
        val name="test-settings-${UUID.randomUUID()}";val prefs=CardPreferences(compose.activity,name)
        try{assertEquals(TrainingSettings(),prefs.read());val changed=TrainingSettings(CardOptions("bass",2,"basic"),TrainingMode.FLASHCARD,8);prefs.write(changed);assertEquals(changed,CardPreferences(compose.activity,name).read())}
        finally{compose.activity.deleteSharedPreferences(name)}
    }
    @Test fun historyShowsSummariesAndOpensFullDetail()=runBlocking{
        val record=vm.repository.save(PracticeInput("2026-09-20","UX履歴-${UUID.randomUUID()}","15",difficultParts="短い見出し\n詳しい振り返りの本文"),null,null,UUID.randomUUID().toString())
        compose.waitUntil(5000){vm.records.value.any{it.id==record.id}}
        compose.runOnIdle{vm.switchTab("history");vm.query=record.piece}
        compose.onNodeWithTag("record-${record.id}").assertIsDisplayed().performClick()
        compose.onNode(hasText("短い見出し\n詳しい振り返りの本文") and hasAnyAncestor(isDialog())).assertIsDisplayed();capture("history-detail")
    }
}
