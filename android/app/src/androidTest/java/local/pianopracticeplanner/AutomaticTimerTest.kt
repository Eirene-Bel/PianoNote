package local.pianopracticeplanner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import local.pianopracticeplanner.domain.CardOptions
import local.pianopracticeplanner.domain.NoteEngine
import local.pianopracticeplanner.domain.TrainingSettings
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AutomaticTimerTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()

    @Test fun nextStartsPartialAnswerRunsAndFirstGradeStopsBeforeReplay(){
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.progressLoaded}
        compose.runOnIdle{
            vm.switchTab("cards");vm.changeQuizMode("read")
            assertFalse(vm.tracking)
            vm.applyCardSettings(TrainingSettings(CardOptions("treble",2,"staff")))
            vm.nextCard()
            assertTrue(vm.tracking)
        }
        compose.waitUntil(15000){vm.scoreReady}
        compose.runOnIdle{
            vm.note(NoteEngine.name(vm.card.notes[0]))
            assertTrue(vm.tracking)
            assertNull(vm.correct)
            vm.note(NoteEngine.name(vm.card.notes[1]))
            assertNotNull(vm.correct)
            assertFalse(vm.tracking)
            vm.playSound()
            assertFalse(vm.tracking)
            vm.stopAudio()
            vm.backspace()
            assertFalse(vm.tracking)
        }
    }

    @Test fun settingsRoutePausesOnlyEligibleUnansweredQuestion(){
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.progressLoaded}
        compose.runOnIdle{
            vm.switchTab("cards");vm.changeQuizMode("read");vm.applyCardSettings(TrainingSettings());vm.nextCard()
            assertTrue(vm.tracking)
            vm.switchTab("settings");assertFalse(vm.tracking)
            vm.switchTab("cards");assertTrue(vm.tracking)
        }
        compose.waitUntil(15000){vm.scoreReady}
        compose.runOnIdle{
            vm.note(NoteEngine.name(vm.card.notes.single()))
            assertFalse(vm.tracking)
            vm.switchTab("settings");vm.switchTab("cards")
            assertFalse(vm.tracking)
        }
    }
}
