package local.pianopracticeplanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.*
import org.junit.Assert.*

class TimeIncrementTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var vm:PianoViewModel
    @Before fun setup(){
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded}
        compose.runOnIdle{vm.clearInput();vm.switchTab("practice")}
    }
    @After fun cleanup(){compose.runOnIdle{vm.clearInput()}}
    private fun tap(minutes:Int){compose.onNode(hasClickAction() and (hasText(minutes.toString()) or hasText("＋$minutes"))).performClick()}
    @Test fun fifteenThenThirtyAddsUpToFortyFive(){
        tap(15);tap(30)
        compose.onNodeWithTag("minutes").assertTextContains("45")
        tap(5);tap(5)
        compose.onNodeWithTag("minutes").assertTextContains("55")
        compose.waitUntil(5000){vm.draftStatus.contains("下書き保存済み")}
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("minutes").assertTextContains("55")
    }
    @Test fun manualEntryIsTheStartingPointAndLimitDoesNotDiscardIt(){
        compose.runOnIdle{vm.changeInput(vm.input.copy(minutes="10"))}
        tap(15);compose.onNodeWithTag("minutes").assertTextContains("25")
        compose.runOnIdle{vm.changeInput(vm.input.copy(minutes="1430"))}
        tap(30);compose.runOnIdle{assertEquals("1430",vm.input.minutes);assertTrue(vm.error.contains("1440"))}
        compose.runOnIdle{vm.changeInput(vm.input.copy(minutes="1.5"))}
        tap(5);compose.runOnIdle{assertEquals("1.5",vm.input.minutes);assertTrue(vm.error.isNotBlank())}
    }
    @Test fun resetClearsOnlyMinutesAndAddStartsFromZero(){
        compose.runOnIdle{vm.changeInput(vm.input.copy(piece="練習曲",minutes="40",difficultParts="左手",finishingImage="静かに"))}
        compose.onNodeWithTag("reset-minutes").performClick()
        compose.runOnIdle{
            assertEquals("",vm.input.minutes)
            assertEquals("練習曲",vm.input.piece)
            assertEquals("左手",vm.input.difficultParts)
            assertEquals("静かに",vm.input.finishingImage)
        }
        tap(15)
        compose.onNodeWithTag("minutes").assertTextContains("15")
    }
}
