package local.pianopracticeplanner.domain

import local.pianopracticeplanner.data.NoteAttempt
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ProgressTest {
    private fun attempt(time:Long,correct:Boolean,mode:String="read",assisted:Boolean=false,clef:String="treble")=
        NoteAttempt(UUID.randomUUID().toString(),"2026-09-21",time,mode,clef,1,"staff","treble","30",correct,assisted)

    @Test fun recentAndPreviousUseMatchingUnassistedFirstAnswers() {
        val rows=listOf(attempt(1,true),attempt(2,false),attempt(3,true),attempt(4,false),attempt(5,true),
            attempt(6,false,assisted=true),attempt(7,false,mode="listen"),attempt(8,false,clef="both"))
        val result=ProgressStats.calculate(rows,"read",CardOptions(),2)
        assertEquals(listOf(5L,4L),result.recent.map{it.answeredAt})
        assertEquals(listOf(3L,2L),result.previous.map{it.answeredAt})
        assertEquals(.5,result.rate!!,0.0001)
        assertEquals(.5,result.previousRate!!,0.0001)
        assertNull(ProgressStats.calculate(rows,"read",CardOptions(),3).previousRate)
        assertNull(ProgressStats.calculate(emptyList(),"read",CardOptions(),2).rate)
    }

    @Test fun toneAndCalendarThresholdsAreStable() {
        assertEquals(440.0,ToneMath.frequency(33),0.0001)
        assertEquals(261.625565,ToneMath.frequency(28),0.001)
        assertEquals(listOf(0,1,1,2,2,3,3,4),listOf(0,1,899,900,1799,1800,3599,3600).map{ProgressStats.heatLevel(it)})
    }
}
