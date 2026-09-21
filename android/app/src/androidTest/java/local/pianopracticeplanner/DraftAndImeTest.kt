package local.pianopracticeplanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftAndImeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun compositionBlocksSavingUntilConfirmedAndDraftSurvivesRecreation() {
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java];vm.switchTab("practice")}
        compose.waitUntil(10000){vm.loaded}
        compose.runOnIdle{vm.changeInput(vm.input.copy(piece="IME検証",minutes="15"));vm.changeText("piece",TextFieldValue("IME検証",composition=TextRange(0,5)))}
        compose.onNodeWithTag("save").assertIsNotEnabled()
        compose.runOnIdle{vm.save();assertFalse(vm.busy);vm.changeText("piece",TextFieldValue("IME検証"))}
        compose.onNodeWithTag("save").assertIsEnabled()
        Thread.sleep(500)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000){compose.onAllNodesWithTag("piece").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("piece").assertTextContains("IME検証")
        compose.runOnIdle{ViewModelProvider(compose.activity)[PianoViewModel::class.java].clearInput()}
    }
    @Test fun emptyDraftDoesNotKeepAnOldDefaultDate() {
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded}
        val empty=local.pianopracticeplanner.data.PracticeRepository.inputJson(local.pianopracticeplanner.domain.PracticeInput(date="2020-01-01"))
        kotlinx.coroutines.runBlocking{vm.repository.writeDraft(org.json.JSONObject().put("input",empty).put("editingId","").put("revision",0).put("operationId",java.util.UUID.randomUUID().toString()).toString())}
        compose.runOnIdle{vm.loadDraft()}
        Thread.sleep(500)
        compose.runOnIdle{assertEquals(java.time.LocalDate.now().toString(),vm.input.date)}
    }
}
