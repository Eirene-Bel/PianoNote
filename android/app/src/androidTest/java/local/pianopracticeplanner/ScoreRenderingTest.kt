package local.pianopracticeplanner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import local.pianopracticeplanner.domain.NoteCard
import local.pianopracticeplanner.ui.ScoreWebView
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

@RunWith(AndroidJUnit4::class)
class ScoreRenderingTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun findWebView(view:View):WebView?=when(view){is WebView->view;is ViewGroup->(0 until view.childCount).firstNotNullOfOrNull{findWebView(view.getChildAt(it))};else->null}
    @Test fun localFontsAndBothClefsRenderWithoutNetwork() {
        val result=AtomicReference<String?>("pending")
        val card=mutableStateOf(NoteCard("treble",listOf(28,30,32)))
        val feedback=mutableStateOf(emptyList<String>())
        compose.runOnUiThread { compose.activity.setContent { MaterialTheme { ScoreWebView(card.value,feedback.value,Modifier.fillMaxWidth().height(200.dp)){result.set(it)} } } }
        compose.waitUntil(20000){result.get()!="pending"};assertNull(result.get())
        val labels=AtomicReference<String?>(null)
        compose.runOnIdle{findWebView(compose.activity.window.decorView)!!.evaluateJavascript("document.querySelectorAll('.score-feedback-label').length"){labels.set(it)}}
        compose.waitUntil(5000){labels.get()!=null};assertEquals("0",labels.get())
        compose.runOnIdle {result.set("pending");feedback.value=listOf("wrong","correct","wrong")}
        compose.waitUntil(10000){result.get()!="pending"};assertNull(result.get())
        labels.set(null)
        compose.runOnIdle{findWebView(compose.activity.window.decorView)!!.evaluateJavascript("document.querySelectorAll('.score-feedback-label').length"){labels.set(it)}}
        compose.waitUntil(5000){labels.get()!=null};assertEquals("3",labels.get())
        compose.runOnIdle {result.set("pending");card.value=NoteCard("bass",listOf(14,28,30))}
        compose.waitUntil(10000){result.get()!="pending"};assertNull(result.get())
    }
}
