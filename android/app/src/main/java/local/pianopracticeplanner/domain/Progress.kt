package local.pianopracticeplanner.domain

import local.pianopracticeplanner.data.NoteAttempt
import kotlin.math.pow

data class ProgressSettings(val options:CardOptions=CardOptions(),val mode:String="read",val volume:Float=.35f,val recentN:Int=20,
    val trainingMode:String="ANSWER",val interval:Int=5)

data class ProgressResult(val recent:List<NoteAttempt>,val previous:List<NoteAttempt>,val rate:Double?,val previousRate:Double?)

object ProgressStats {
    fun calculate(attempts:List<NoteAttempt>,mode:String,options:CardOptions,n:Int):ProgressResult {
        require(mode=="read" || mode=="listen")
        require(n in 1..1000)
        val eligible=attempts.asSequence().filter { !it.assisted && it.mode==mode &&
            it.clefOption==options.clef && it.countOption==options.count && it.range==options.range }
            .sortedWith(compareByDescending<NoteAttempt>{it.answeredAt}.thenByDescending{it.id}).toList()
        val recent=eligible.take(n)
        val previous=eligible.drop(n).take(n)
        fun rate(rows:List<NoteAttempt>):Double?=if(rows.isEmpty())null else rows.count{it.correct}.toDouble()/rows.size
        return ProgressResult(recent,previous,rate(recent),if(previous.size==n)rate(previous) else null)
    }
    fun heatLevel(seconds:Int):Int=when {
        seconds<=0->0
        seconds<900->1
        seconds<1800->2
        seconds<3600->3
        else->4
    }
}

object ToneMath {
    // Diatonic steps use C0=0; A4 is step 33.
    private val semitones=intArrayOf(0,2,4,5,7,9,11)
    fun frequency(step:Int):Double {
        require(step in 0..69)
        val midi=12+12*(step/7)+semitones[step%7]
        return 440.0*2.0.pow((midi-69)/12.0)
    }
}
