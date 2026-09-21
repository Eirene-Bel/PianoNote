package local.pianopracticeplanner.domain

import local.pianopracticeplanner.i18n.tr

import java.time.LocalDate

data class PracticeInput(
    val date: String = LocalDate.now().toString(),
    val piece: String = "", val minutes: String = "",
    val difficultParts: String = "", val unpracticedParts: String = "", val finishingImage: String = ""
)

object PracticeValidator {
    fun text(value: String, limit: Int, label: String): String {
        val clean=value.trim()
        require('\u0000' !in clean && clean.codePointCount(0,clean.length)<=limit) { tr("s007" ,label,limit) }
        return clean
    }
    fun validate(value: PracticeInput): PracticeInput {
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(value.date)) { tr("s008") }
        val date=runCatching { LocalDate.parse(value.date) }.getOrNull()
        require(date!=null && date.year in 1..9999) { tr("s009") }
        val piece=text(value.piece,80,tr("s010"))
        require(piece.isNotEmpty()) { tr("s011") }
        val minutes=value.minutes.toIntOrNull()
        require(minutes!=null && minutes in 1..1440) { tr("s012") }
        return value.copy(piece=piece,minutes=minutes.toString(),
            difficultParts=text(value.difficultParts,2000,tr("s013")),
            unpracticedParts=text(value.unpracticedParts,2000,tr("s014")),
            finishingImage=text(value.finishingImage,2000,tr("s015")))
    }
}
