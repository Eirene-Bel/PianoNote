package local.pianopracticeplanner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import local.pianopracticeplanner.ui.PianoViewModel
import local.pianopracticeplanner.domain.*
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.*
import org.junit.Assert.*

class UxRegressionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun model():PianoViewModel {
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java]}
        compose.waitUntil(10000){vm.loaded}
        return vm
    }
    @Test fun selectingCurrentOptionsDoesNotDestroyTheQuestion() {
        val vm=model()
        compose.runOnIdle{
            val id=vm.card.id
            vm.configure(vm.options)
            assertEquals(id,vm.card.id)
        }
    }
    @Test fun newOptionsPreserveCurrentAnswerUntilNextQuestion() {
        val vm=model()
        compose.runOnIdle{
            vm.answerChanged(TextFieldValue("ド"));val id=vm.card.id
            vm.configure(CardOptions("bass",3,"wide"))
            assertEquals(id,vm.card.id)
            assertEquals("ド",vm.answer.text)
            vm.nextCard()
            assertEquals("bass",vm.card.clef);assertEquals(3,vm.card.notes.size)
        }
    }
    @Test fun noteButtonsGradeWhenTheLastNoteIsEntered() {
        val vm=model()
        compose.runOnIdle{vm.switchTab("cards");vm.applyCardSettings(TrainingSettings());vm.nextCard()}
        compose.waitUntil(15000){vm.scoreReady}
        compose.runOnIdle{vm.note(NoteEngine.name(vm.card.notes.single()));assertEquals(true,vm.correct)}
    }
    @Test fun savingPastRecordReturnsToTodaysNewEntry() {
        val vm=model()
        compose.runOnIdle{vm.switchTab("practice");vm.clearInput();vm.changeInput(PracticeInput("2020-01-01","UX日付確認",15.toString()));vm.save()}
        compose.waitUntil(10000){!vm.busy}
        compose.runOnIdle{assertEquals(java.time.LocalDate.now().toString(),vm.input.date)}
    }
}
