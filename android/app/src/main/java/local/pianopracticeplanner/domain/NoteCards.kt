package local.pianopracticeplanner.domain

import local.pianopracticeplanner.i18n.tr
import local.pianopracticeplanner.i18n.I18n

import java.text.Normalizer
import java.util.UUID
import kotlin.random.Random

data class CardOptions(val clef: String="treble",val count: Int=1,val range: String="staff") {
    init { require(clef in listOf("treble","bass","both") && count in 0..3 && range in listOf("staff","basic","wide")) }
}
enum class TrainingMode(private val labelKey:String) { ANSWER("s001"), FLASHCARD("s002"), AUTO("s003"); val label: String get() = tr(labelKey) }
data class TrainingSettings(val options:CardOptions=CardOptions(),val mode:TrainingMode=TrainingMode.ANSWER,val interval:Int=5)
data class NoteCard(val clef: String,val notes: List<Int>,val id: String=UUID.randomUUID().toString()) {
    val signature get() = "$clef:${notes.joinToString(",")}"
}
class NoteEngine(private val random: Random=Random.Default) {
    fun next(options: CardOptions,last: NoteCard?=null): NoteCard {
        val clef=if(options.clef=="both") if(random.nextBoolean())"treble" else "bass" else options.clef
        val count=if(options.count==0)random.nextInt(1,4) else options.count
        val allowed=range(clef,options.range)
        val notes=MutableList(count){random.nextInt(allowed.first,allowed.last+1)}
        if(last?.clef==clef && last.notes==notes)notes[0]=allowed.first+(notes[0]-allowed.first+1)%allowed.count()
        return NoteCard(clef,notes)
    }
    companion object {
        private val names=listOf("ド","レ","ミ","ファ","ソ","ラ","シ")
        private val letters=listOf("c","d","e","f","g","a","b")
        fun name(step: Int): String { require(step in 0..69);return names[step%7] }
        fun key(step: Int): String { require(step in 0..69);return "${letters[step%7]}/${step/7}" }
        fun scientific(step: Int)=key(step).replace("/","").uppercase()
        fun range(clef: String,range: String): IntRange {
            require(clef in listOf("treble","bass"))
            val extension=when(range){"staff"->0;"basic"->2;"wide"->4;else->throw IllegalArgumentException("Invalid range")}
            val bottom=if(clef=="treble")30 else 18
            return (bottom-extension)..(bottom+8+extension)
        }
        fun parse(raw: String): List<String> {
            val aliases = mapOf("dó" to "ド", "fá" to "ファ", "lá" to "ラ", "do" to "ド", "re" to "レ", "ré" to "レ", "mi" to "ミ", "fa" to "ファ", "sol" to "ソ", "so" to "ソ", "la" to "ラ", "si" to "シ", "ti" to "シ", "c" to "ド", "d" to "レ", "e" to "ミ", "f" to "ファ", "g" to "ソ", "a" to "ラ", "b" to "シ", "h" to "シ", "도" to "ド", "레" to "レ", "미" to "ミ", "파" to "ファ", "솔" to "ソ", "라" to "ラ", "시" to "シ")
            // Letter notation must consume one letter at a time: FA means F + A, not solfège Fa.
            val pattern = if (I18n.language in listOf("en", "de")) "[cdefgabh도레미파솔라시]"
                else "dó|fá|lá|do|ré|re|mi|fa|sol|so|la|si|ti|[cdefgabh도레미파솔라시]"
            val localized = Regex(pattern, RegexOption.IGNORE_CASE).replace(Normalizer.normalize(raw,Normalizer.Form.NFKC)) { aliases[it.value.lowercase(java.util.Locale.ROOT)] ?: it.value }
            val normalized=Normalizer.normalize(localized,Normalizer.Form.NFKC).map{if(it in 'ぁ'..'ゖ')(it.code+0x60).toChar() else it}.joinToString("")
                .replace(Regex("[\\s、,・/／→-]"),"")
            require(normalized.isNotEmpty()) { tr("s004") }
            val result=Regex("ファ|ド|レ|ミ|ソ|ラ|シ").findAll(normalized).map{it.value}.toList()
            require(result.joinToString("")==normalized) { tr("s005") }
            return result
        }
        fun check(card: NoteCard,answer: String): Boolean {
            val names=parse(answer)
            require(names.size==card.notes.size) { tr("s006" ,card.notes.size) }
            return names==card.notes.map{ name(it) }
        }
    }
}
