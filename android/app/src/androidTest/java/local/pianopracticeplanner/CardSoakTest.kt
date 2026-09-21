package local.pianopracticeplanner

import android.os.Bundle
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import local.pianopracticeplanner.ui.PianoViewModel
import org.junit.*
import org.junit.Assert.*

class CardSoakTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun thirtyMinuteForegroundAutoplay() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("runSoak")=="true")
        lateinit var vm:PianoViewModel
        compose.runOnIdle{vm=ViewModelProvider(compose.activity)[PianoViewModel::class.java];vm.switchTab("cards");vm.applyCardSettings(local.pianopracticeplanner.domain.TrainingSettings(mode=local.pianopracticeplanner.domain.TrainingMode.AUTO,interval=3));vm.nextCard()}
        compose.waitUntil(20000){vm.scoreReady}
        val start=SystemClock.elapsedRealtime();val initial=vm.number
        compose.runOnIdle{vm.toggleAuto()}
        try {
            while(SystemClock.elapsedRealtime()-start<30*60*1000L){
                val checkpoint=SystemClock.elapsedRealtime()
                // Pump the Compose test frame clock while real Android time passes.
                // Sleeping the test thread starves recomposition/score readiness.
                compose.waitUntil(35000){SystemClock.elapsedRealtime()-checkpoint>=30000}
                compose.runOnIdle{assertTrue("Autoplay unexpectedly stopped",vm.auto);assertNull(vm.scoreError)}
                InstrumentationRegistry.getInstrumentation().sendStatus(0,Bundle().apply{putString("stream","Soak ${(SystemClock.elapsedRealtime()-start)/1000}s; cards=${vm.number-initial}\n")})
            }
            assertTrue("Cards did not keep advancing",vm.number-initial>=250)
        }finally{compose.runOnIdle{vm.stopAuto()}}
    }
}
