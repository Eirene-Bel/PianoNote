package local.pianopracticeplanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import local.pianopracticeplanner.data.NoteAttempt
import local.pianopracticeplanner.ui.PianoTheme
import local.pianopracticeplanner.ui.RecentAnswerGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecentAnswerGridTest {
    @get:Rule val compose=createComposeRule()

    @Test fun mixedTwentyAnswersKeepEqualCellsCenteredAcrossWrappedRows(){
        val answers=(0 until 20).map{index->
            NoteAttempt("answer-$index","2026-09-21",index.toLong(),"read","treble",1,"staff","treble","30",index%2==0,false)
        }
        compose.setContent { PianoTheme { Box(Modifier.width(320.dp)){RecentAnswerGrid(answers)} } }
        val grid=compose.onNodeWithTag("recent-answer-grid").fetchSemanticsNode().boundsInRoot
        val cells=(0 until 20).map{compose.onNodeWithTag("recent-answer-$it",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot}
        val width=cells.first().width
        val height=cells.first().height
        cells.forEachIndexed{index,cell->
            assertEquals(width,cell.width,1f)
            assertEquals(height,cell.height,1f)
            val glyph=compose.onNodeWithTag("recent-answer-glyph-$index",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals(cell.center.x,glyph.center.x,1f)
            assertEquals(cell.center.y,glyph.center.y,1f)
        }
        val rows=cells.groupBy{it.top}
        assertTrue("20 cells should wrap",rows.size>1)
        rows.values.forEach{row->
            assertEquals(grid.center.x,(row.first().left+row.last().right)/2f,2f)
        }
    }
}
