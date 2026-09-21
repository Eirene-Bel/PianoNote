package local.pianopracticeplanner.audio

import local.pianopracticeplanner.i18n.tr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import local.pianopracticeplanner.domain.ToneMath
import kotlin.math.*

/** Offline, sequential pitched PCM; no microphone or external sound files. */
object NoteWave {
    const val SAMPLE_RATE=24000
    const val STEP_SECONDS=.85
    fun synthesize(notes:List<Int>):ShortArray {
        require(notes.size in 1..3)
        val step=(SAMPLE_RATE*STEP_SECONDS).toInt()
        val result=ShortArray(step*notes.size)
        notes.forEachIndexed { index,note ->
            val frequency=ToneMath.frequency(note)
            for(i in 0 until (SAMPLE_RATE*.78).toInt()){
                val time=i.toDouble()/SAMPLE_RATE
                val attack=min(1.0,time/.008)
                val release=min(1.0,(.78-time)/.05).coerceAtLeast(0.0)
                val envelope=attack*release*exp(-3.7*time)
                val phase=2*PI*frequency*time
                val sample=(sin(phase)+.3*sin(2*phase)+.12*sin(3*phase)+.05*sin(4*phase))*envelope*.55
                result[index*step+i]=(sample*Short.MAX_VALUE).roundToInt().coerceIn(-32767,32767).toShort()
            }
        }
        return result
    }
}

class NoteAudioPlayer(context:Context) {
    private val manager=context.applicationContext.getSystemService(AudioManager::class.java)
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private var track:AudioTrack?=null
    private var focus:AudioFocusRequest?=null
    private var generation=0
    private var loss:(()->Unit)?=null

    /** Called on Main; returns true only after all frames really passed the playback head. */
    suspend fun play(notes:List<Int>,volume:Float,onFocusLost:()->Unit):Boolean {
        stop()
        require(volume in .01f..1f){tr("s271")}
        val token=generation
        val samples=withContext(Dispatchers.Default){NoteWave.synthesize(notes)}
        if(token!=generation)return false
        loss=onFocusLost
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener({change->
                if(change!=AudioManager.AUDIOFOCUS_GAIN){val callback=loss;stop();callback?.invoke()}
            },Handler(Looper.getMainLooper())).build()
        check(manager.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){tr("s272")}
        focus=request
        try {
            val output=AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(NoteWave.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(samples.size*2).build()
            track=output
            check(output.state==AudioTrack.STATE_NO_STATIC_DATA||output.state==AudioTrack.STATE_INITIALIZED)
            check(output.write(samples,0,samples.size)==samples.size){tr("s273")}
            output.setVolume(volume*.4f)
            output.play()
            withTimeout(6000){while(token==generation&&output.playbackHeadPosition<samples.size){delay(25)}}
            return token==generation
        } finally { if(token==generation)stop() }
    }
    fun stop(){
        generation++
        track?.let{runCatching{it.pause()};runCatching{it.release()}};track=null
        focus?.let{manager.abandonAudioFocusRequest(it)};focus=null;loss=null
    }
}
