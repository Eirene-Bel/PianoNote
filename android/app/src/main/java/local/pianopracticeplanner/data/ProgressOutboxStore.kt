package local.pianopracticeplanner.data

import local.pianopracticeplanner.i18n.tr

import android.content.Context

/** Small write-ahead copy independent of Room; committed before first-answer UI state is saved. */
class ProgressOutboxStore(context:Context,name:String="progress-outbox") {
    private val preferences=context.applicationContext.getSharedPreferences(name,Context.MODE_PRIVATE)
    fun read():String?=preferences.getString("payload",null)
    fun write(payload:String){
        check(preferences.edit().putString("payload",payload).commit()){
            tr("s274")
        }
    }
}
