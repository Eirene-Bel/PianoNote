package local.pianopracticeplanner.ui

import org.junit.Assert.*
import org.junit.Test

class QuestionClockTest {
    @Test fun initialAndCompletedQuestionNeverStartWithoutNext(){
        var time=0L;val clock=QuestionClock{time}
        clock.resume(true);assertFalse(clock.running)
        clock.next(true);time=350;clock.stop();clock.resume(true)
        assertFalse(clock.running);assertEquals(0,clock.wholeSeconds())
        clock.next(true);time=1000;clock.stop()
        assertEquals(1,clock.wholeSeconds())
    }
    @Test fun hiddenTimeIsExcludedAndFractionsCarryAcrossQuestions(){
        var time=0L;val clock=QuestionClock{time}
        clock.next(true);time=400;clock.pause();time=100_000;clock.resume(true)
        time=100_200;clock.stop();assertEquals(0,clock.wholeSeconds())
        clock.next(true);time=100_700;clock.stop();assertEquals(1,clock.wholeSeconds())
        assertEquals(100,clock.accumulatedMillis)
    }
}
