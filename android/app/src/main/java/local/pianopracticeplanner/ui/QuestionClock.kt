package local.pianopracticeplanner.ui

/** Monotonic question clock; fractional milliseconds remain across questions. */
internal class QuestionClock(private val now:()->Long) {
    private var startedAt:Long?=null
    private var eligible=false
    var accumulatedMillis:Long=0; private set
    val running get()=startedAt!=null
    fun next(visible:Boolean){stop();eligible=true;if(visible)startedAt=now()}
    fun pause(){startedAt?.let{accumulatedMillis+=(now()-it).coerceAtLeast(0)};startedAt=null}
    fun resume(visible:Boolean){if(eligible&&visible&&startedAt==null)startedAt=now()}
    fun stop(){pause();eligible=false}
    fun wholeSeconds():Int {val seconds=(accumulatedMillis/1000).toInt();accumulatedMillis%=1000;return seconds}
}
