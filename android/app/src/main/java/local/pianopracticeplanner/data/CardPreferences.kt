package local.pianopracticeplanner.data

import local.pianopracticeplanner.i18n.tr

import android.content.Context
import local.pianopracticeplanner.domain.*

class CardPreferences(context:Context,name:String="training-preferences") {
    private val preferences=context.getSharedPreferences(name,Context.MODE_PRIVATE)
    fun read():TrainingSettings=runCatching {
        TrainingSettings(CardOptions(preferences.getString("clef","treble")!!,preferences.getInt("count",1),preferences.getString("range","staff")!!),
            TrainingMode.valueOf(preferences.getString("mode","ANSWER")!!),preferences.getInt("interval",5).takeIf{it in listOf(3,5,8)}?:5)
    }.getOrDefault(TrainingSettings())
    fun write(settings:TrainingSettings) {
        check(preferences.edit().putString("clef",settings.options.clef).putInt("count",settings.options.count).putString("range",settings.options.range)
            .putString("mode",settings.mode.name).putInt("interval",settings.interval).commit()) { tr("s328") }
    }
}
