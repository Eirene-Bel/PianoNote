package local.pianopracticeplanner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardLifecycleTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun answerAndBackgroundStopAutoWithoutResumingOnReturn() {
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java];vm.switchTab("cards");vm.applyCardSettings(local.pianopracticeplanner.domain.TrainingSettings(mode=local.pianopracticeplanner.domain.TrainingMode.AUTO,interval=3));vm.nextCard()}
        compose.waitUntil(15000){vm.scoreReady}
        compose.runOnIdle{vm.toggleAuto()}
        compose.waitUntil(7000){vm.revealed}
        compose.runOnIdle{vm.answerChanged(TextFieldValue("ど"));assertFalse(vm.auto)}
        val id=vm.card.id
        Thread.sleep(3200)
        compose.runOnIdle{assertEquals(id,vm.card.id);vm.toggleAuto()}
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(vm.auto)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle{assertFalse(vm.auto)}
    }
    @Test fun autoRevealKeepsLoopAliveAndNextStartsTimingAgain(){
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.progressLoaded}
        compose.runOnIdle{vm.switchTab("cards");vm.applyCardSettings(local.pianopracticeplanner.domain.TrainingSettings(mode=local.pianopracticeplanner.domain.TrainingMode.AUTO,interval=3));vm.nextCard()}
        compose.waitUntil(15000){vm.scoreReady}
        compose.runOnIdle{vm.toggleAuto()}
        val id=vm.card.id
        compose.waitUntil(7000){vm.revealed}
        compose.runOnIdle{assertTrue(vm.auto);assertFalse(vm.tracking);assertEquals(id,vm.card.id)}
        compose.waitUntil(7000){vm.card.id!=id}
        compose.runOnIdle{assertTrue(vm.auto);assertTrue(vm.tracking);vm.stopAuto();vm.pausePractice()}
    }
}
