package local.pianopracticeplanner

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.*
import java.io.File

class VisualAcceptanceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun failedSaveShowsItsReasonWithoutSearchingAboveTheForm() {
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java];vm.switchTab("practice")}
        compose.waitUntil(10000){vm.loaded}
        compose.runOnIdle{vm.clearInput()}
        compose.onNodeWithTag("save").assertIsDisplayed().performClick()
        compose.onNodeWithTag("global-error").assertIsDisplayed()
    }
    @Test fun captureActualCardScreenAfterLocalRendering() {
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java];vm.switchTab("cards")}
        compose.waitUntil(15000){vm.scoreReady}
        val inst=InstrumentationRegistry.getInstrumentation()
        val dir=File(inst.targetContext.getExternalFilesDir(null),"evidence").apply{mkdirs()}
        inst.uiAutomation.takeScreenshot().let{bitmap->File(dir,"cards.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
        compose.onNodeWithTag("card-next").assertIsDisplayed()
        inst.uiAutomation.takeScreenshot().let{bitmap->File(dir,"answer.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    }
}
