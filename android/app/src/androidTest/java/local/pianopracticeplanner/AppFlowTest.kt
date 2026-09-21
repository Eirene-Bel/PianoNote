package local.pianopracticeplanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun recordCanBeSavedAndFoundAndCardAnswerControlsAreAvailable() {
        val title="Android試験-${System.currentTimeMillis()}"
        compose.onNodeWithTag("nav-practice").assertExists()
        compose.onNodeWithTag("piece").performTextInput(title)
        compose.onNodeWithTag("minutes").performTextInput("15")
        compose.onNodeWithTag("save").assertIsDisplayed().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("記録を保存しました。").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("nav-review").performClick()
        compose.onNodeWithText("練習記録を一覧で見る").performScrollTo().performClick()
        compose.onNodeWithTag("search").performTextInput(title)
        compose.onNode(hasText(title) and !hasSetTextAction()).assertExists()
        if(androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true){
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(5000){compose.onAllNodesWithTag("nav-cards").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("nav-review").assertIsSelected()
        }
        compose.onNodeWithTag("nav-cards").performClick()
        compose.onNodeWithTag("card-response").assertExists()
        compose.onNodeWithTag("note-ド").assertIsDisplayed()
        compose.onNodeWithTag("card-next").assertIsDisplayed()
    }
}
