package local.pianopracticeplanner.domain

import local.pianopracticeplanner.audio.NoteWave
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class NoteWaveTest {
    @Test fun pitchEnvelopeAndSequentialSpacingAreAudibleWithoutClipping(){
        val wave=NoteWave.synthesize(listOf(28,30,32))
        val step=(NoteWave.SAMPLE_RATE*.85).toInt()
        assertEquals(step*3,wave.size)
        assertTrue(wave.maxOf{abs(it.toInt())} in 1000..30000)
        listOf(261.6256,329.6276,391.9954).forEachIndexed{index,expected->
            val section=wave.sliceArray(index*step until index*step+12000)
            fun power(frequency:Double):Double{
                var x=0.0;var y=0.0
                section.forEachIndexed{i,value->val phase=2*PI*frequency*i/NoteWave.SAMPLE_RATE;x+=value*cos(phase);y+=value*sin(phase)}
                return x*x+y*y
            }
            val peak=((expected-5).roundToInt()..(expected+5).roundToInt()).maxBy{power(it.toDouble())}
            assertEquals(expected,peak.toDouble(),1.0)
            assertTrue(wave.slice(index*step+19000 until (index+1)*step).all{it==0.toShort()})
        }
    }
}
