package local.pianopracticeplanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import local.pianopracticeplanner.ui.PianoViewModel
import local.pianopracticeplanner.domain.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class UxReviewRegressionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:PianoViewModel
    @Before fun start(){compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]};compose.waitUntil(10000){vm.loaded};compose.runOnIdle{vm.clearInput()}}
    @After fun end(){compose.runOnIdle{vm.stopAuto();vm.cancelImport();vm.applyCardSettings(TrainingSettings());vm.clearInput()}}
    @Test fun resumeAutoDoesNotLoseAPendingModeChange(){
        compose.runOnIdle{vm.switchTab("cards");vm.applyCardSettings(TrainingSettings(mode=TrainingMode.AUTO));vm.nextCard()}
        compose.waitUntil(15000){vm.scoreReady}
        compose.runOnIdle{vm.applyCardSettings(TrainingSettings(mode=TrainingMode.ANSWER));vm.toggleAuto();assertEquals(TrainingMode.ANSWER,vm.configuredMode);vm.nextCard(stop=false);assertEquals(TrainingMode.ANSWER,vm.mode);assertFalse(vm.auto)}
    }
    @Test fun deletedHistoryOnlyBackupCanBeImportedFromTheScreen(){
        val json=JSONObject().put("format","piano-practice").put("formatVersion",1).put("records",JSONArray()).put("deletedIds",JSONArray(listOf(UUID.randomUUID().toString()))).toString()
        compose.runOnIdle{vm.prepareImport(json)}
        compose.waitUntil(10000){vm.importReport!=null}
        compose.onNode(hasClickAction() and hasText("取り込む",substring=true)).assertIsEnabled()
    }
    @Test fun editingPastRecordFinishesOnTodaysNewEntry()=runBlocking{
        val original=vm.repository.save(PracticeInput("2020-01-01","UX編集日付","15"),null,null,UUID.randomUUID().toString())
        compose.runOnIdle{vm.edit(original);vm.changeInput(vm.input.copy(minutes="20"));vm.save()}
        compose.waitUntil(10000){!vm.busy}
        compose.runOnIdle{assertEquals("practice",vm.tab);assertEquals(java.time.LocalDate.now().toString(),vm.input.date);assertNull(vm.editingId)}
        assertEquals("2020-01-01",vm.repository.records().first{it.id==original.id}.date)
    }
}
