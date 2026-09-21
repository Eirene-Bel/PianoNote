package local.pianopracticeplanner.data

import local.pianopracticeplanner.i18n.tr

import androidx.room.withTransaction
import local.pianopracticeplanner.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ImportConflict(val id:String,val date:String,val piece:String,val detail:String,val kind:String="record")
data class ImportReport(val added:Int,val skipped:Int,val conflicts:List<ImportConflict> = emptyList(),val deletedCount:Int=0,
    val attemptsAdded:Int=0,val sessionsAdded:Int=0,val settingsAvailable:Boolean=false,val draftAvailable:Boolean=false,
    val settingsPreserved:Boolean=false,val draftPreserved:Boolean=false)
data class TransferPayload(val records: List<PracticeRecord>,val deletedIds: List<String>,
    val attempts:List<NoteAttempt> = emptyList(),val sessions:List<NoteSession> = emptyList(),
    val settings:ProgressSettings?=null,val draft:String?=null)

class PracticeRepository(private val db: PracticeDatabase) {
    private val dao=db.practice()
    val flow=dao.observe()
    val attempts=dao.observeAttempts()
    val sessions=dao.observeSessions()
    suspend fun records()=dao.all()
    suspend fun recordAttempt(row:NoteAttempt) { validateAttempt(row);db.withTransaction {
        val old=dao.attempt(row.id)
        require(old==null || old==row){tr("s275")}
        if(old==null)dao.addAttempt(row)
    } }
    suspend fun recordSession(row:NoteSession) { validateSession(row);db.withTransaction {
        val old=dao.session(row.id)
        require(old==null || old==row){tr("s276")}
        if(old==null)dao.addSession(row)
    } }
    suspend fun readProgressSettings():ProgressSettings?=dao.progressSettings()?.let { parseSettings(JSONObject(it.payload)) }
    suspend fun writeProgressSettings(settings:ProgressSettings) { validateSettings(settings);dao.putProgressSettings(ProgressSettingsRow(payload=settingsJson(settings).toString())) }
    suspend fun readDraft()=dao.draft()?.payload
    suspend fun writeDraft(payload: String)=dao.putDraft(DraftRow(payload=payload))
    suspend fun clearDraft()=dao.clearDraft()

    suspend fun save(input: PracticeInput,id: String?,expectedRevision: Long?,operationId: String): PracticeRecord {
        val clean=PracticeValidator.validate(input)
        val payload=inputJson(clean).put("id",id?:JSONObject.NULL).put("expectedRevision",expectedRevision?:JSONObject.NULL).toString()
        return db.withTransaction {
            val operation=dao.operation(operationId)
            if(operation!=null) {
                require(operation.payload==payload){tr("s277")}
                return@withTransaction requireNotNull(dao.find(operation.recordId)){tr("s278")}
            }
            val old=if(id!=null)requireNotNull(dao.find(id)){tr("s279")} else null
            require(old==null || old.revision==expectedRevision){tr("s280")}
            val record=(old?:PracticeRecord(UUID.randomUUID().toString(),clean.date,clean.piece,clean.minutes.toInt())).copy(
                date=clean.date,piece=clean.piece,minutes=clean.minutes.toInt(),difficultParts=clean.difficultParts,
                unpracticedParts=clean.unpracticedParts,finishingImage=clean.finishingImage,revision=(old?.revision?:0)+1)
            if(old==null)dao.insert(record) else dao.update(record)
            dao.addOperation(SaveOperation(operationId,payload,record.id));dao.clearDraft();record
        }
    }
    suspend fun delete(record: PracticeRecord)=db.withTransaction {
        val current=requireNotNull(dao.find(record.id)){tr("s279")}
        require(current.revision==record.revision){tr("s281")}
        dao.tombstone(DeletedRecord(record.id));dao.delete(record.id)
    }
    suspend fun exportJson(): String=db.withTransaction {
        JSONObject().put("format","piano-practice").put("formatVersion",2)
            .put("sourcePlatform","android").put("sourceSchema",2).put("exportedAt",Instant.now().toString())
            .put("records",JSONArray(dao.all().map{recordJson(it)})).put("deletedIds",JSONArray(dao.deletedIds()))
            .put("attempts",JSONArray(dao.allAttempts().map{attemptJson(it)}))
            .put("sessions",JSONArray(dao.allSessions().map{sessionJson(it)}))
            .put("settings",dao.progressSettings()?.let{JSONObject(it.payload)}?:JSONObject.NULL)
            .put("draft",dao.draft()?.payload?:JSONObject.NULL).toString(2)
    }

    private suspend fun inspect(payload: TransferPayload): ImportReport {
        var add=0;var skip=0;var deletedCount=0;val conflicts=mutableListOf<ImportConflict>()
        for(id in payload.deletedIds){
            val old=dao.find(id)
            if(old!=null)conflicts.add(ImportConflict(id,old.date,old.piece,tr("s282")))
            else if(dao.wasDeleted(id)==0)deletedCount++
        }
        for(row in payload.records){
            if(dao.wasDeleted(row.id)>0){skip++;continue}
            val old=dao.find(row.id)
            if(old!=null&&old!=row){
                val changes=listOf(tr("s283") to (old.date!=row.date),tr("s010") to (old.piece!=row.piece),tr("s284" ,old.minutes,row.minutes) to (old.minutes!=row.minutes),
                    tr("s013") to (old.difficultParts!=row.difficultParts),tr("s014") to (old.unpracticedParts!=row.unpracticedParts),
                    tr("s015") to (old.finishingImage!=row.finishingImage),tr("s026") to (old.memo!=row.memo||old.practiceRange!=row.practiceRange||old.next!=row.next))
                    .filter{it.second}.joinToString("、"){it.first}.ifEmpty{tr("s285")}
                conflicts.add(ImportConflict(row.id,old.date,old.piece,tr("s286" ,changes)))
            }else if(old==null)add++ else skip++
        }
        var attemptsAdded=0;var sessionsAdded=0
        for(row in payload.attempts){val old=dao.attempt(row.id);if(old==null)attemptsAdded++ else if(old!=row)conflicts.add(ImportConflict(row.id,row.date,tr("s287"),tr("s288"),"attempt"))}
        for(row in payload.sessions){val old=dao.session(row.id);if(old==null)sessionsAdded++ else if(old!=row)conflicts.add(ImportConflict(row.id,row.date,tr("s289"),tr("s290"),"session"))}
        return ImportReport(add,skip,conflicts,deletedCount,attemptsAdded,sessionsAdded,payload.settings!=null,payload.draft!=null,
            payload.settings!=null && dao.progressSettings()!=null,payload.draft!=null && dao.draft()!=null)
    }
    suspend fun previewImport(text: String): ImportReport {val payload=parseTransfer(text);return db.withTransaction{inspect(payload)}}
    suspend fun importJson(text:String,skipConflicts:Boolean=false):ImportReport {
        val payload=parseTransfer(text)
        return db.withTransaction {
            val report=inspect(payload)
            require(skipConflicts||report.conflicts.isEmpty()){tr("s291" ,report.conflicts.size)}
            val recordExcluded=report.conflicts.filter{it.kind=="record"}.map{it.id}.toSet()
            val attemptExcluded=report.conflicts.filter{it.kind=="attempt"}.map{it.id}.toSet()
            val sessionExcluded=report.conflicts.filter{it.kind=="session"}.map{it.id}.toSet()
            for(row in payload.records)if(row.id !in recordExcluded&&dao.wasDeleted(row.id)==0&&dao.find(row.id)==null)dao.insert(row)
            for(id in payload.deletedIds)if(id !in recordExcluded)dao.tombstone(DeletedRecord(id))
            for(row in payload.attempts)if(row.id !in attemptExcluded && dao.attempt(row.id)==null)dao.addAttempt(row)
            for(row in payload.sessions)if(row.id !in sessionExcluded && dao.session(row.id)==null)dao.addSession(row)
            if(payload.settings!=null && dao.progressSettings()==null)dao.putProgressSettings(ProgressSettingsRow(payload=settingsJson(payload.settings).toString()))
            if(payload.draft!=null && dao.draft()==null)dao.putDraft(DraftRow(payload=payload.draft))
            report.copy(skipped=report.skipped+report.conflicts.size)
        }
    }

    companion object {
        private fun uuid(value:String) { require(UUID.fromString(value).toString()==value){tr("s292")} }
        private fun date(value:String) { require(LocalDate.parse(value).toString()==value){tr("s293")} }
        private fun validateAttempt(row:NoteAttempt) {
            uuid(row.id);date(row.date)
            require(row.answeredAt>0 && row.mode in listOf("read","listen") && row.actualClef in listOf("treble","bass")){tr("s294")}
            val options=CardOptions(row.clefOption,row.countOption,row.range)
            require(options.clef=="both" || options.clef==row.actualClef){tr("s295")}
            val steps=row.notes.split(',').map{require(it.matches(Regex("[0-9]+")));it.toInt()}
            require(steps.size in 1..3 && (options.count==0 || steps.size==options.count) && steps.all{it in NoteEngine.range(row.actualClef,row.range)}){tr("s296")}
        }
        private fun validateSession(row:NoteSession) {uuid(row.id);date(row.date);require(row.seconds in 1..86400){tr("s297")}}
        private fun validateSettings(s:ProgressSettings) {require(s.mode in listOf("read","listen") && s.volume.isFinite() && s.volume in 0f..1f && s.recentN in 1..1000 &&
            s.trainingMode in listOf("ANSWER","FLASHCARD","AUTO") && s.interval in listOf(3,5,8)){tr("s298")}}
        private fun settingsJson(s:ProgressSettings)=JSONObject().put("clef",s.options.clef).put("count",s.options.count).put("range",s.options.range)
            .put("mode",s.mode).put("volume",s.volume.toDouble()).put("recentN",s.recentN)
            .put("trainingMode",s.trainingMode).put("interval",s.interval)
        private fun parseSettings(v:JSONObject):ProgressSettings {
            val countRaw=integer(v,"count");val nRaw=integer(v,"recentN")
            require(countRaw in 0..3 && nRaw in 1..1000){tr("s298")}
            val count=countRaw.toInt();val n=nRaw.toInt()
            val volume=v.opt("volume");require(volume is Number){tr("s299")}
            val intervalRaw=if(v.has("interval"))integer(v,"interval") else 5L
            require(intervalRaw in listOf(3L,5L,8L)){tr("s298")}
            val interval=intervalRaw.toInt()
            val s=ProgressSettings(CardOptions(string(v,"clef"),count,string(v,"range")),string(v,"mode"),volume.toFloat(),n,
                if(v.has("trainingMode"))string(v,"trainingMode") else "ANSWER",interval)
            validateSettings(s);return s
        }
        private fun attemptJson(a:NoteAttempt)=JSONObject().put("id",a.id).put("date",a.date).put("answeredAt",a.answeredAt)
            .put("mode",a.mode).put("clefOption",a.clefOption).put("countOption",a.countOption).put("range",a.range)
            .put("actualClef",a.actualClef).put("notes",a.notes).put("correct",a.correct).put("assisted",a.assisted)
        private fun sessionJson(s:NoteSession)=JSONObject().put("id",s.id).put("date",s.date).put("seconds",s.seconds)
        private fun boolean(v:JSONObject,key:String):Boolean {val x=v.opt(key);require(x is Boolean){tr("s300" ,key)};return x}
        private fun parseAttempt(v:JSONObject):NoteAttempt {
            val count=integer(v,"countOption");require(count in 0..3){tr("s301")}
            return NoteAttempt(string(v,"id"),string(v,"date"),integer(v,"answeredAt"),string(v,"mode"),string(v,"clefOption"),count.toInt(),string(v,"range"),string(v,"actualClef"),string(v,"notes"),boolean(v,"correct"),boolean(v,"assisted")).also{validateAttempt(it)}
        }
        private fun parseSession(v:JSONObject):NoteSession {
            val seconds=integer(v,"seconds");require(seconds in 1..86400){tr("s297")}
            return NoteSession(string(v,"id"),string(v,"date"),seconds.toInt()).also{validateSession(it)}
        }
        private fun validateDraft(raw:String):String {
            require(raw.length<=100000){tr("s302")}
            val root=try{JSONObject(raw)}catch(e:Exception){throw IllegalArgumentException(tr("s303"),e)}
            val input=root.opt("input")
            require(input is JSONObject){tr("s304")}
            date(string(input,"date"))
            string(input,"piece");string(input,"minutes")
            for(key in listOf("difficult_parts","unpracticed_parts","finishing_image"))if(input.has(key))string(input,key)
            if(root.has("editingId")){
                val id=string(root,"editingId");if(id.isNotEmpty())uuid(id)
            }
            if(root.has("revision"))require(integer(root,"revision")>=0){tr("s305")}
            if(root.has("operationId"))uuid(string(root,"operationId"))
            if(root.has("goalTouched"))boolean(root,"goalTouched")
            return raw
        }
        fun inputJson(v: PracticeInput)=JSONObject().put("date",v.date).put("piece",v.piece).put("minutes",v.minutes)
            .put("difficult_parts",v.difficultParts).put("unpracticed_parts",v.unpracticedParts).put("finishing_image",v.finishingImage)
        fun inputFromJson(v: JSONObject)=PracticeInput(v.getString("date"),v.getString("piece"),v.getString("minutes"),
            v.optString("difficult_parts"),v.optString("unpracticed_parts"),v.optString("finishing_image"))
        fun recordJson(r: PracticeRecord)=JSONObject().put("id",r.id).put("date",r.date).put("piece",r.piece).put("minutes",r.minutes)
            .put("range",r.practiceRange).put("memo",r.memo).put("next",r.next).put("revision",r.revision).put("created_at",r.createdAt)
            .put("difficult_parts",r.difficultParts).put("unpracticed_parts",r.unpracticedParts).put("finishing_image",r.finishingImage)
        private fun string(v: JSONObject,key: String,optional: Boolean=false): String {
            if(optional&&!v.has(key))return ""
            val value=v.opt(key);require(value is String){tr("s306" ,key)};return value
        }
        private fun integer(v: JSONObject,key: String): Long {
            val value=v.opt(key);require(value is Int||value is Long){tr("s307" ,key)};return (value as Number).toLong()
        }
        fun parseTransfer(text: String): TransferPayload {
            require(text.toByteArray(Charsets.UTF_8).size<=20*1024*1024){tr("s308")}
            try {
                val root=JSONObject(text)
                require(root.optString("format")=="piano-practice" && integer(root,"formatVersion") in 1L..2L){tr("s309")}
                val version=integer(root,"formatVersion")
                val array=root.getJSONArray("records");require(array.length()<=10000){tr("s310")}
                val seen=mutableSetOf<String>()
                val records=(0 until array.length()).map { i ->
                    val v=array.getJSONObject(i)
                    try {
                    val id=string(v,"id")
                    require(UUID.fromString(id).toString()==id && seen.add(id)){tr("s311")}
                    val minutes=integer(v,"minutes");require(minutes in 1..1440){tr("s312")}
                    val rev=integer(v,"revision");require(rev in 1..Int.MAX_VALUE.toLong()){ tr("s313") }
                    val created=string(v,"created_at");Instant.parse(created)
                    val input=PracticeInput(string(v,"date"),string(v,"piece"),minutes.toString(),string(v,"difficult_parts",true),string(v,"unpracticed_parts",true),string(v,"finishing_image",true))
                    val clean=PracticeValidator.validate(input)
                    val range=string(v,"range",true);val memo=string(v,"memo",true);val next=string(v,"next",true)
                    val cleanRange=PracticeValidator.text(range,120,tr("s314"))
                    val cleanMemo=PracticeValidator.text(memo,1000,tr("s315"))
                    val cleanNext=PracticeValidator.text(next,160,tr("s316"))
                    PracticeRecord(id,clean.date,clean.piece,minutes.toInt(),cleanRange,cleanMemo,cleanNext,rev,created,clean.difficultParts,clean.unpracticedParts,clean.finishingImage)
                    }catch(e:Exception){throw IllegalArgumentException(tr("s317" ,i+1,v.optString("date").take(10),v.optString("piece").take(40),e.message?:tr("s318")),e)}
                }
                val deleted=if(root.has("deletedIds"))root.getJSONArray("deletedIds")else JSONArray()
                require(deleted.length()<=100000){tr("s319")}
                val deletedIds=(0 until deleted.length()).map{i->
                    val id=deleted.get(i);require(id is String&&UUID.fromString(id).toString()==id){tr("s320")}
                    require(id !in seen){tr("s321")};id
                }.distinct()
                if(version==1L)return TransferPayload(records,deletedIds)
                fun array(key:String,max:Int):JSONArray {val a=root.getJSONArray(key);require(a.length()<=max){tr("s322" ,key)};return a}
                val attemptsArray=array("attempts",100000)
                val attemptIds=mutableSetOf<String>()
                val attempts=(0 until attemptsArray.length()).map{i->parseAttempt(attemptsArray.getJSONObject(i)).also{require(attemptIds.add(it.id)){tr("s323")}}}
                val sessionsArray=array("sessions",100000)
                val sessionIds=mutableSetOf<String>()
                val sessions=(0 until sessionsArray.length()).map{i->parseSession(sessionsArray.getJSONObject(i)).also{require(sessionIds.add(it.id)){tr("s324")}}}
                val settings=when(val v=root.get("settings")){JSONObject.NULL->null;is JSONObject->parseSettings(v);else->throw IllegalArgumentException(tr("s325"))}
                val draft=when(val v=root.get("draft")){JSONObject.NULL->null;is String->validateDraft(v);else->throw IllegalArgumentException(tr("s326"))}
                return TransferPayload(records,deletedIds,attempts,sessions,settings,draft)
            }catch(e:IllegalArgumentException){throw e}
            catch(e:Exception){throw IllegalArgumentException(tr("s327"),e)}
        }
    }
}
