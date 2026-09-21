package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import local.pianopracticeplanner.data.*
import local.pianopracticeplanner.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import android.os.SystemClock
import local.pianopracticeplanner.audio.NoteAudioPlayer

@OptIn(ExperimentalCoroutinesApi::class)
class PianoViewModel(val repository:PracticeRepository,private val saved:SavedStateHandle,
    private val preferences:CardPreferences?=null,private val today:()->String={LocalDate.now().toString()},
    private val audio:NoteAudioPlayer?=null,private val outbox:ProgressOutboxStore?=null):ViewModel() {
    var tab by mutableStateOf(saved.get<String>("tab")?:"practice");private set
    var input by mutableStateOf(PracticeInput(date=today()));private set
    private val fieldValues=mutableStateMapOf<String,TextFieldValue>()
    val composing get()=fieldValues.values.any{it.composition!=null}
    var editingId by mutableStateOf<String?>(null);private set
    private var revision:Long?=null
    private var operationId=UUID.randomUUID().toString()
    private var draftWrite:Job?=null
    private var observedDay=today()
    private var goalTouched=false
    var draftStatus by mutableStateOf("");private set
    var entryGeneration by mutableStateOf(0);private set
    var loaded by mutableStateOf(false);private set
    var busy by mutableStateOf(false);private set
    var error by mutableStateOf("");private set
    var notice by mutableStateOf("");private set
    var retryKind by mutableStateOf("");private set
    private val reload=MutableStateFlow(0)
    val records=reload.flatMapLatest{repository.flow.catch{fail(it,tr("s119"),"load");emit(emptyList())}}
        .stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    var query by mutableStateOf("")
    var pendingImport by mutableStateOf<String?>(null);private set
    var importReport by mutableStateOf<ImportReport?>(null);private set
    val recentRecords get()=records.value.distinctBy{it.piece.trim()}.take(12)
    val previousRecord get()=records.value.firstOrNull{it.id!=editingId&&it.piece.trim()==input.piece.trim()}
    private val initial=preferences?.read()?:TrainingSettings()
    private val engine=NoteEngine()
    var options by mutableStateOf(initial.options);private set
    var configuredMode by mutableStateOf(initial.mode);private set
    var mode by mutableStateOf(initial.mode);private set
    private var currentOptions=options
    var card by mutableStateOf(engine.next(options));private set
    var answer by mutableStateOf(TextFieldValue());private set
    var selectedAnswerIndex by mutableStateOf<Int?>(null);private set
    val answerTokens get()=runCatching{NoteEngine.parse(answer.text)}.getOrDefault(emptyList())
    var revealed by mutableStateOf(false);private set
    var feedback by mutableStateOf("");private set
    var correct by mutableStateOf<Boolean?>(null);private set
    var number by mutableStateOf(1);private set
    var auto by mutableStateOf(false);private set
    var interval by mutableStateOf(initial.interval);private set
    var scoreReady by mutableStateOf(false);private set
    var scoreError by mutableStateOf<String?>(null);private set
    val settingsPending get()=options!=currentOptions||mode!=configuredMode
    private var loop:Job?=null
    private var epoch=0

    val attempts=repository.attempts.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val sessions=repository.sessions.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    var quizMode by mutableStateOf(saved.get<String>("quizMode")?:"read");private set
    var volume by mutableFloatStateOf(.35f);private set
    var recentN by mutableIntStateOf(20);private set
    var progressLoaded by mutableStateOf(false);private set
    var playing by mutableStateOf(false);private set
    var heard by mutableStateOf(saved.get<Boolean>("heard")?:false);private set
    private var assisted=saved.get<Boolean>("assisted")?:false
    private var firstJudged=saved.get<Boolean>("firstJudged")?:false
    val scoreVisible get()=quizMode=="read"||revealed||firstJudged
    val scoreFeedback:List<String> get()=if(!revealed&&!firstJudged) emptyList() else card.notes.mapIndexed{index,note->
        if(!firstJudged) "neutral" else if(answerTokens.getOrNull(index)==NoteEngine.name(note)) "correct" else "wrong"
    }
    val canAnswer get()=progressLoaded&&mode==TrainingMode.ANSWER&&(if(quizMode=="listen")heard else scoreReady)
    var tracking by mutableStateOf(false);private set
    var activeSeconds by mutableIntStateOf(0);private set
    private var audioJob:Job?=null
    private var audioEpoch=0
    private val questionClock=QuestionClock(SystemClock::elapsedRealtime)
    private var appVisible=false
    private var sessionDay=today()
    private var unflushedSeconds=0
    private var ticker:Job?=null
    private var settingsWrite:Job?=null
    private val pendingAttempts=mutableStateMapOf<String,NoteAttempt>()
    val hasPendingResponses get()=pendingAttempts.isNotEmpty()
    private val pendingSessions=mutableStateMapOf<String,NoteSession>()
    val hasPendingProgress get()=hasPendingResponses||pendingSessions.isNotEmpty()
    private var progressWrite:Job?=null

    init {
        saved.get<String>("cardState")?.let{raw->runCatching{
            val j=JSONObject(raw)
            options=CardOptions(j.getString("clefOption"),j.getInt("count"),j.getString("range"))
            currentOptions=CardOptions(j.optString("currentClef",options.clef),j.optInt("currentCount",options.count),j.optString("currentRange",options.range))
            val notes=j.getJSONArray("notes");card=NoteCard(j.getString("clef"),(0 until notes.length()).map{notes.getInt(it)},j.getString("id"))
            answer=TextFieldValue(j.optString("answer"));revealed=j.optBoolean("revealed");number=j.optInt("number",1)
            interval=j.optInt("interval",initial.interval).takeIf{it in listOf(3,5,8)}?:5
            mode=TrainingMode.valueOf(j.optString("mode",initial.mode.name));configuredMode=TrainingMode.valueOf(j.optString("configuredMode",mode.name))
            if(j.has("correct")&&!j.isNull("correct"))correct=j.getBoolean("correct")
            feedback=when(correct) { true -> tr("s058"); false -> tr("s145"); null -> "" }
        }}
        (outbox?.read()?:saved.get<String>("progressOutbox"))?.let{raw->runCatching{
            val payload=PracticeRepository.parseTransfer(raw)
            payload.attempts.forEach{pendingAttempts[it.id]=it};payload.sessions.forEach{pendingSessions[it.id]=it}
            flushProgress()
        }.onFailure{fail(it,tr("s120"))}}
        loadDraft()
        viewModelScope.launch {
            try {
                repository.readProgressSettings()?.let { value ->
                    volume=value.volume;recentN=value.recentN
                    if(!saved.contains("cardState")){mode=TrainingMode.valueOf(value.trainingMode);configuredMode=mode;interval=value.interval;preferences?.write(TrainingSettings(value.options,mode,interval))}
                    if(!saved.contains("cardState")){
                        options=value.options;currentOptions=options;quizMode=value.mode
                        if(quizMode=="listen"){mode=TrainingMode.ANSWER;configuredMode=mode}
                        card=engine.next(options);scoreReady=false
                    }
                }
                progressLoaded=true
            } catch(e:Exception){fail(e,tr("s121"),"progress-load")}
        }
    }
    private fun fail(e:Throwable,operation:String,retry:String="") {
        if(e is CancellationException)throw e
        error=when(e){
            is android.database.sqlite.SQLiteFullException->tr("s122" ,operation)
            is android.database.sqlite.SQLiteDatabaseLockedException->tr("s123" ,operation)
            is SecurityException->tr("s124")
            is IllegalArgumentException->e.message?:tr("s125")
            else->tr("s126" ,operation)
        };retryKind=retry;notice=""
    }
    private var messageLanguage = local.pianopracticeplanner.i18n.I18n.language
    fun refreshLanguage() {
        if (messageLanguage == local.pianopracticeplanner.i18n.I18n.language) return
        messageLanguage = local.pianopracticeplanner.i18n.I18n.language
        error = local.pianopracticeplanner.i18n.refreshTranslation(error)
        notice = local.pianopracticeplanner.i18n.refreshTranslation(notice)
        draftStatus = local.pianopracticeplanner.i18n.refreshTranslation(draftStatus)
        feedback = local.pianopracticeplanner.i18n.refreshTranslation(feedback)
        scoreError = scoreError?.let { local.pianopracticeplanner.i18n.refreshTranslation(it) }
        val text = pendingImport
        if (text != null && !busy) viewModelScope.launch {
            try {
                val report = withContext(Dispatchers.IO) { repository.previewImport(text) }
                if (pendingImport == text) importReport = report
            } catch (e: Exception) { fail(e, tr("s142"), "import") }
        }
    }
    fun dismissMessage(){error="";notice="";retryKind=""}
    fun retry(){if(hasPendingProgress){persistOutbox();flushProgress()};when(retryKind){"save"->save();"load"->{dismissMessage();reload.value++;if(!loaded)loadDraft()};"draft"->{dismissMessage();flushDraft()};"progress"->{dismissMessage();flushProgress()};"progress-settings"->{dismissMessage();persistProgressSettings()};"progress-load"->{dismissMessage();viewModelScope.launch{try{repository.readProgressSettings()?.let{volume=it.volume;recentN=it.recentN};progressLoaded=true}catch(e:Exception){fail(e,tr("s121"),"progress-load")}}}}}
    fun onResume(){val date=today();if(observedDay!=date&&!hasDraft()){input=PracticeInput(date=date);fieldValues.clear()};observedDay=date}
    fun loadDraft(){viewModelScope.launch {
        try {
            repository.readDraft()?.let{raw->
                val j=JSONObject(raw);input=PracticeRepository.inputFromJson(j.getJSONObject("input"));fieldValues.clear()
                editingId=j.optString("editingId").ifEmpty{null};revision=if(editingId!=null)j.optLong("revision",1)else null
                operationId=j.optString("operationId").ifEmpty{UUID.randomUUID().toString()};goalTouched=j.optBoolean("goalTouched",input.finishingImage.isNotBlank())
            }
            if(!hasDraft()){input=PracticeInput(date=today());repository.clearDraft();draftStatus=""}
            else draftStatus=tr("s127")
            loaded=true
        }catch(e:Exception){fail(e,tr("s128"),"load")}
    }}
    fun textValue(key:String):TextFieldValue=fieldValues[key]?:TextFieldValue(when(key){"piece"->input.piece;"minutes"->input.minutes;"difficult"->input.difficultParts;"unpracticed"->input.unpracticedParts;"finishing"->input.finishingImage;else->""})
    fun changeText(key:String,value:TextFieldValue){
        if(busy||!loaded)return
        if(key=="finishing")goalTouched=true
        val updated=when(key){"piece"->input.copy(piece=value.text);"minutes"->input.copy(minutes=value.text);"difficult"->input.copy(difficultParts=value.text);"unpracticed"->input.copy(unpracticedParts=value.text);"finishing"->input.copy(finishingImage=value.text);else->input}
        changeInput(updated);fieldValues[key]=value
        if(key=="piece"&&value.composition==null&&!goalTouched&&editingId==null){
            val goal=previousRecord?.finishingImage.orEmpty()
            if(input.finishingImage!=goal){input=input.copy(finishingImage=goal);fieldValues.remove("finishing");persistDraft()}
        }
    }
    fun changeInput(value:PracticeInput){
        if(busy||!loaded)return
        val values=mapOf("piece" to value.piece,"minutes" to value.minutes,"difficult" to value.difficultParts,"unpracticed" to value.unpracticedParts,"finishing" to value.finishingImage)
        values.forEach{(key,text)->if(fieldValues[key]?.text!=text)fieldValues.remove(key)}
        input=value;dismissMessage();persistDraft()
    }
    fun addMinutes(amount:Int){
        if(busy||!loaded)return
        require(amount in listOf(5,15,30))
        val current=input.minutes.trim().ifEmpty{"0"}.toIntOrNull()
        if(current==null||current !in 0..1440){reportIo(failure=tr("s129"));return}
        if(current+amount>1440){reportIo(failure=tr("s130"));return}
        changeInput(input.copy(minutes=(current+amount).toString()))
    }
    fun resetMinutes(){
        if(busy||!loaded)return
        changeInput(input.copy(minutes=""))
    }
    fun useRecent(record:PracticeRecord){
        if(busy||!loaded||editingId!=null)return
        fieldValues.clear();goalTouched=false;entryGeneration++
        val goal=records.value.firstOrNull{it.piece.trim()==record.piece.trim()}?.finishingImage?:record.finishingImage
        changeInput(PracticeInput(date=input.date,piece=record.piece,minutes=record.minutes.toString(),finishingImage=goal))
    }
    fun inheritGoal(){previousRecord?.let{goalTouched=true;changeInput(input.copy(finishingImage=it.finishingImage))}}
    private fun draftJson()=JSONObject().put("input",PracticeRepository.inputJson(input)).put("editingId",editingId?:"")
        .put("revision",revision?:0).put("operationId",operationId).put("goalTouched",goalTouched).toString()
    private fun persistDraft(delayMillis:Long=250){
        draftWrite?.cancel();val payload=if(hasDraft())draftJson()else null
        draftStatus=if(payload!=null)tr("s131")else ""
        draftWrite=viewModelScope.launch {
            delay(delayMillis)
            try{if(payload==null)repository.clearDraft()else repository.writeDraft(payload);draftStatus=if(payload!=null)tr("s132")else ""}
            catch(e:Exception){if(e is CancellationException)throw e;draftStatus=tr("s133");fail(e,tr("s134"),"draft")}
        }
    }
    fun flushDraft(){if(loaded&&!busy)persistDraft(0)}
    fun switchTab(value:String){if(busy||tab==value)return;pausePractice();tab=value;saved["tab"]=value;if(!hasPendingProgress)dismissMessage();onResume();resumeQuestion()}
    fun save(){
        if(busy||!loaded)return
        if(composing){error=tr("s135");return}
        runCatching{PracticeValidator.validate(input)}.onFailure{error=it.message?:tr("s136");retryKind="";return}
        busy=true;dismissMessage();val value=input;val id=editingId;val rev=revision;val op=operationId
        viewModelScope.launch {
            draftWrite?.cancelAndJoin()
            try{
                repository.save(value,id,rev,op)
                input=PracticeInput(date=today());observedDay=today();fieldValues.clear();editingId=null;revision=null;goalTouched=false
                operationId=UUID.randomUUID().toString();draftStatus="";entryGeneration++
                notice=if(id==null)tr("s137")else tr("s138")
            }catch(e:Exception){fail(e,tr("s139"),"save");runCatching{repository.writeDraft(draftJson())}}
            finally{busy=false}
        }
    }
    fun edit(record:PracticeRecord){
        if(busy)return
        stopAuto();fieldValues.clear();goalTouched=true;entryGeneration++
        input=PracticeInput(record.date,record.piece,record.minutes.toString(),record.difficultParts,record.unpracticedParts,record.finishingImage)
        editingId=record.id;revision=record.revision;operationId=UUID.randomUUID().toString();tab="practice";saved["tab"]=tab;dismissMessage();persistDraft()
    }
    fun clearInput(){if(busy)return;fieldValues.clear();input=PracticeInput(date=today());editingId=null;revision=null;goalTouched=false;entryGeneration++;operationId=UUID.randomUUID().toString();dismissMessage();persistDraft()}
    fun hasDraft()=editingId!=null||input.piece.isNotBlank()||input.minutes.isNotBlank()||input.difficultParts.isNotBlank()||input.unpracticedParts.isNotBlank()||input.finishingImage.isNotBlank()
    fun delete(record:PracticeRecord){if(busy)return;busy=true;viewModelScope.launch{try{repository.delete(record);if(editingId==record.id){busy=false;clearInput()};notice=tr("s140")}catch(e:Exception){fail(e,tr("s141"))}finally{busy=false}}}
    fun prepareImport(text:String){if(busy)return;busy=true;dismissMessage();viewModelScope.launch{try{draftWrite?.join();settingsWrite?.join();if(loaded&&hasDraft())repository.writeDraft(draftJson());importReport=withContext(Dispatchers.IO){repository.previewImport(text)};pendingImport=text}catch(e:Exception){fail(e,tr("s142"),"import")}finally{busy=false}}}
    fun cancelImport(){pendingImport=null;importReport=null}
    fun applyImport(skipConflicts:Boolean=false){val text=pendingImport?:return;if(busy)return;busy=true;viewModelScope.launch{try{
        val r=withContext(Dispatchers.IO){repository.importJson(text,skipConflicts)};notice=tr("s143" ,r.added,r.attemptsAdded,r.sessionsAdded);cancelImport()
        repository.readProgressSettings()?.let{volume=it.volume;recentN=it.recentN;options=it.options;quizMode=it.mode;configuredMode=TrainingMode.valueOf(it.trainingMode);interval=it.interval;preferences?.write(TrainingSettings(options,configuredMode,interval));stopAudio()}
        if(!hasDraft())loadDraft()
    }catch(e:Exception){cancelImport();fail(e,tr("s142"),"import")}finally{busy=false}}}
    fun reportIo(success:String?=null,failure:String?=null,retry:String=""){notice=success?:"";error=failure?:"";retryKind=retry}

    private fun persistCard(){saved["cardState"]=JSONObject().put("clefOption",options.clef).put("count",options.count).put("range",options.range)
        .put("currentClef",currentOptions.clef).put("currentCount",currentOptions.count).put("currentRange",currentOptions.range)
        .put("id",card.id).put("clef",card.clef).put("notes",JSONArray(card.notes)).put("answer",answer.text).put("revealed",revealed)
        .put("number",number).put("interval",interval).put("mode",mode.name).put("configuredMode",configuredMode.name)
        .put("correct",correct?:JSONObject.NULL).put("feedback",feedback).toString()
        saved["quizMode"]=quizMode;saved["heard"]=heard;saved["assisted"]=assisted;saved["firstJudged"]=firstJudged
    }
    fun configure(value:CardOptions)=applyCardSettings(TrainingSettings(value,configuredMode,interval))
    fun applyCardSettings(value:TrainingSettings){
        if(value==TrainingSettings(options,configuredMode,interval))return
        stopAuto();try{preferences?.write(value);options=value.options;configuredMode=value.mode;interval=value.interval;if(quizMode=="listen"&&configuredMode!=TrainingMode.ANSWER)quizMode="read";persistCard();persistProgressSettings()}catch(e:Exception){fail(e,tr("s144"))}
    }
    fun nextCard(stop:Boolean=true,timed:Boolean=true){
        suspendQuestion()
        stopAudio();heard=false;assisted=false;firstJudged=false
        if(stop)stopAuto();currentOptions=options;mode=configuredMode;if(mode!=TrainingMode.AUTO)stopAuto();card=engine.next(options,card)
        answer=TextFieldValue();selectedAnswerIndex=null;revealed=false;feedback="";correct=null;number++;scoreReady=false;scoreError=null;persistCard()
        if(timed){questionClock.next(appVisible&&tab=="cards");startTicker()}else questionClock.stop()
    }
    fun answerChanged(value:TextFieldValue){
        stopAuto();answer=value;selectedAnswerIndex=null;revealed=false;feedback="";correct=null
        if(mode==TrainingMode.ANSWER&&value.composition==null&&canAnswer&&runCatching{NoteEngine.parse(value.text).size==card.notes.size}.getOrDefault(false))submitAnswer()
        persistCard()
    }
    fun selectAnswer(index:Int){if(index in 0 until card.notes.size)selectedAnswerIndex=index}
    fun note(value:String){
        if(!canAnswer)return
        val values=answerTokens.toMutableList();val index=selectedAnswerIndex
        if(index!=null&&index<values.size)values[index]=value else if(values.size<card.notes.size)values.add(value)else return
        val text=values.joinToString("");answerChanged(TextFieldValue(text,TextRange(text.length)))
    }
    fun backspace(){
        val values=answerTokens.toMutableList();if(values.isEmpty()){answerChanged(TextFieldValue());return}
        val index=selectedAnswerIndex?.takeIf{it in values.indices}?:values.lastIndex;values.removeAt(index)
        val text=values.joinToString("");answerChanged(TextFieldValue(text,TextRange(text.length)))
    }
    fun submitAnswer(){
        if(!canAnswer||answer.composition!=null)return
        stopAuto();try{val result=NoteEngine.check(card,answer.text);pausePractice();questionClock.stop();correct=result;revealed=true
            if(!firstJudged){
                firstJudged=true
                val result=NoteAttempt(card.id,today(),System.currentTimeMillis(),quizMode,currentOptions.clef,currentOptions.count,currentOptions.range,card.clef,card.notes.joinToString(","),correct==true,assisted)
                pendingAttempts[result.id]=result;persistOutbox();flushProgress()
            }
            feedback=if(correct==true)tr("s058")else tr("s145")}
        catch(e:IllegalArgumentException){correct=null;revealed=false;feedback=e.message?:tr("s146")};persistCard()
    }
    fun reveal(){stopAuto();if(!revealed){pausePractice();questionClock.stop()};revealed=!revealed;persistCard()}
    fun changeInterval(value:Int){require(value in listOf(3,5,8));stopAuto();interval=value;persistCard()}
    fun rendered(id:String,message:String?){if(id!=card.id)return;scoreError=message;scoreReady=message==null;if(message!=null)stopAuto()}
    fun stopAuto(){epoch++;auto=false;loop?.cancel();loop=null}
    fun toggleAuto(){
        if(auto){stopAuto();suspendQuestion();return};if(!scoreReady||mode!=TrainingMode.AUTO)return
        if(revealed)nextCard(stop=false)
        answer=TextFieldValue();revealed=false;correct=null;feedback="";auto=true;questionClock.next(appVisible&&tab=="cards");startTicker();val generation=++epoch
        loop=viewModelScope.launch{while(isActive&&auto&&epoch==generation){delay(interval*1000L);if(!auto||epoch!=generation)break;if(!scoreReady)continue;if(revealed)nextCard(stop=false)else{suspendQuestion();questionClock.stop();revealed=true;persistCard()}}}
    }
    fun changeQuizMode(value:String){
        require(value in listOf("read","listen"));if(value==quizMode||!progressLoaded)return
        if(hasPendingResponses){feedback=tr("s147");return}
        pausePractice();quizMode=value;configuredMode=TrainingMode.ANSWER;nextCard(timed=false)
        persistProgressSettings()
    }
    fun changeVolume(value:Float){volume=value.coerceIn(.01f,1f);persistProgressSettings()}
    fun changeRecentN(value:Int){require(value in 1..1000);recentN=value;persistProgressSettings()}
    private fun persistProgressSettings(){
        val value=ProgressSettings(options,quizMode,volume,recentN,configuredMode.name,interval)
        settingsWrite?.cancel()
        settingsWrite=viewModelScope.launch{try{repository.writeProgressSettings(value)}catch(e:Exception){fail(e,tr("s144"),"progress-settings")}}
    }
    fun stopAudio(){audioEpoch++;audioJob?.cancel();audioJob=null;audio?.stop();playing=false}
    fun playSound(reference:Boolean=false){
        if(!progressLoaded)return
        if(audio==null){feedback=tr("s148");return}
        stopAudio();stopAuto()
        if(quizMode=="read"&&!firstJudged){assisted=true;persistCard()}
        val token=audioEpoch;val id=card.id;playing=true
        audioJob=viewModelScope.launch {
            try {
                val notes=if(reference)listOf(if(card.clef=="treble")28 else 21)else card.notes
                val finished=audio.play(notes,volume){stopAudio();feedback=tr("s149")}
                if(finished&&token==audioEpoch&&card.id==id&&!reference){heard=true;persistCard()}
            }catch(e:Exception){if(e !is CancellationException)feedback=e.message?:tr("s150")}
            finally{if(token==audioEpoch)playing=false}
        }
    }
    fun nextQuestion(){if(hasPendingResponses){feedback=tr("s151");return};nextCard();if(quizMode=="listen")playSound()}
    fun setAppVisible(value:Boolean){appVisible=value;if(value)resumeQuestion()else pausePractice()}
    fun resumeQuestion(){if(tab!="cards"||!appVisible)return;questionClock.resume(true);startTicker()}
    private fun startTicker(){if(!questionClock.running||tracking)return;tracking=true;sessionDay=today();ticker=viewModelScope.launch{while(isActive&&tracking){delay(250);collectTime()}}}
    private fun collectTime(){
        if(!tracking)return
        questionClock.pause()
        val elapsed=questionClock.wholeSeconds()
        if(elapsed>0){unflushedSeconds+=elapsed;activeSeconds+=elapsed}
        questionClock.resume(appVisible&&tab=="cards")
        if(unflushedSeconds>=10||sessionDay!=today()){queueTime();sessionDay=today()}
    }
    private fun queueTime(){
        if(unflushedSeconds<=0)return
        val value=NoteSession(UUID.randomUUID().toString(),sessionDay,unflushedSeconds)
        pendingSessions[value.id]=value;unflushedSeconds=0;persistOutbox();flushProgress()
    }
    private fun persistOutbox(){
        val attempts=JSONArray(pendingAttempts.values.map{a->JSONObject().put("id",a.id).put("date",a.date).put("answeredAt",a.answeredAt).put("mode",a.mode)
            .put("clefOption",a.clefOption).put("countOption",a.countOption).put("range",a.range).put("actualClef",a.actualClef).put("notes",a.notes).put("correct",a.correct).put("assisted",a.assisted)})
        val sessions=JSONArray(pendingSessions.values.map{s->JSONObject().put("id",s.id).put("date",s.date).put("seconds",s.seconds)})
        val payload=JSONObject().put("format","piano-practice").put("formatVersion",2).put("records",JSONArray()).put("attempts",attempts).put("sessions",sessions).put("settings",JSONObject.NULL).put("draft",JSONObject.NULL).toString()
        saved["progressOutbox"]=payload
        try{outbox?.write(payload)}catch(e:Exception){fail(e,tr("s152"),"progress")}
    }
    private fun flushProgress(){
        if(progressWrite?.isActive==true)return
        progressWrite=viewModelScope.launch{
            try {
                while(pendingAttempts.isNotEmpty()||pendingSessions.isNotEmpty()){
                    if(pendingAttempts.isNotEmpty()){
                        val value=pendingAttempts.values.first();repository.recordAttempt(value);pendingAttempts.remove(value.id)
                    }else{
                        val value=pendingSessions.values.first();repository.recordSession(value);pendingSessions.remove(value.id)
                    }
                    persistOutbox()
                }
            }catch(e:Exception){tracking=false;ticker?.cancel();fail(e,tr("s153"),"progress")}
        }
    }
    private fun suspendQuestion(){questionClock.pause();val elapsed=questionClock.wholeSeconds();if(elapsed>0){unflushedSeconds+=elapsed;activeSeconds+=elapsed};queueTime();tracking=false;ticker?.cancel();ticker=null}
    fun pausePractice(){stopAuto();stopAudio();suspendQuestion()}
    suspend fun exportBackup():String{
        pausePractice();draftWrite?.cancelAndJoin()
        if(loaded){if(hasDraft())repository.writeDraft(draftJson())else repository.clearDraft()}
        settingsWrite?.join();flushProgress();progressWrite?.join()
        check(pendingAttempts.isEmpty()&&pendingSessions.isEmpty()){ tr("s154") }
        repository.writeProgressSettings(ProgressSettings(options,quizMode,volume,recentN,configuredMode.name,interval))
        return repository.exportJson()
    }
    override fun onCleared(){audio?.stop();super.onCleared()}

}
