package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr
import local.pianopracticeplanner.i18n.I18n

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import local.pianopracticeplanner.domain.*
import kotlin.math.roundToInt

@Composable internal fun CardsWorkspace(vm:PianoViewModel){
    var changeMode by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(changeMode){if(changeMode!=null)vm.pausePractice()else vm.resumeQuestion()}
    val attempts by vm.attempts.collectAsStateWithLifecycle()
    val stats=ProgressStats.calculate(attempts,vm.quizMode,vm.options,vm.recentN)
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("read" to tr("s031"),"listen" to tr("s032")).forEach{(key,label)->
                FilterChip(vm.quizMode==key,onClick={if(vm.answer.text.isNotBlank()&&vm.correct==null)changeMode=key else vm.changeQuizMode(key)},label={Text(label)},enabled=vm.progressLoaded,modifier=Modifier.weight(1f).testTag("mode-$key"))
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
            val landscape=maxWidth>maxHeight*1.2f
            val short=maxHeight<470.dp
            if(landscape)Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                ScorePanel(vm,Modifier.weight(.44f).fillMaxHeight(),true)
                AnswerPanel(vm,Modifier.weight(.56f).fillMaxHeight(),true)
            }else Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(5.dp)){
                if(!short)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    TextButton(onClick={vm.switchTab("review")},modifier=Modifier.weight(1f)){
                        Text(tr("s033" ,vm.recentN,stats.rate?.let{(it*100).roundToInt().toString()+"%"}?:tr("s034")))
                    }
                    Text("${vm.activeSeconds/60}:${(vm.activeSeconds%60).toString().padStart(2,'0')}",style=MaterialTheme.typography.labelSmall)
                    Text(if(vm.tracking)tr("s035") else tr("s036"),style=MaterialTheme.typography.labelSmall)
                }
                if(!short)Text(tr("s037"),style=MaterialTheme.typography.labelSmall,color=PianoPalette.Muted)
                ScorePanel(vm,Modifier.weight(1f).fillMaxWidth(),short)
                AnswerPanel(vm,Modifier.fillMaxWidth().heightIn(max=if(short)346.dp else 390.dp),short)
            }
        }
    }
    changeMode?.let{value->AlertDialog(onDismissRequest={changeMode=null},title={Text(tr("s038"))},text={Text(tr("s039"))},confirmButton={TextButton(onClick={vm.changeQuizMode(value);changeMode=null}){Text(tr("s040"))}},dismissButton={TextButton(onClick={changeMode=null}){Text(tr("s041"))}})}
}

@Composable private fun ScorePanel(vm:PianoViewModel,modifier:Modifier,compact:Boolean){
    OutlinedCard(modifier){
        Column(Modifier.fillMaxSize().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
            if(!compact||vm.settingsPending||!vm.scoreVisible)Text(if(vm.quizMode=="listen"&&!vm.scoreVisible)tr("s042")else tr("s043" ,if(vm.card.clef=="treble")tr("s044")else tr("s045"),vm.card.notes.size,if(vm.settingsPending)tr("s046")else ""),style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center)
            if(vm.scoreVisible){
                val card=vm.card
                ScoreWebView(card,vm.scoreFeedback,Modifier.weight(1f).fillMaxWidth().testTag("score")){vm.rendered(card.id,it)}
            }else Box(Modifier.weight(1f).fillMaxWidth().testTag("hidden-score"),contentAlignment=Alignment.Center){
                Text(if(vm.playing)tr("s047")else tr("s048"),textAlign=TextAlign.Center,style=MaterialTheme.typography.titleMedium,color=PianoPalette.Muted)
            }
        }
    }
}

@Composable private fun AnswerPanel(vm:PianoViewModel,modifier:Modifier,compact:Boolean){
    val large=LocalDensity.current.fontScale>=1.3f
    var textInput by remember{mutableStateOf(false)}
    LaunchedEffect(textInput){if(textInput)vm.pausePractice()else vm.resumeQuestion()}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)){
        Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)){
            if(vm.mode==TrainingMode.ANSWER){
                Row(Modifier.fillMaxWidth().heightIn(min=if(compact)48.dp else if(vm.correct!=null) if(large)72.dp else 60.dp else 56.dp).testTag("card-response"),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    vm.card.notes.forEachIndexed{index,note->
                        val canonical=vm.answerTokens.getOrNull(index);val entered=canonical?.let(I18n::note);val judged=vm.correct!=null;val matches=canonical==NoteEngine.name(note)
                        FilledTonalButton(onClick={vm.selectAnswer(index)},enabled=vm.canAnswer,modifier=Modifier.weight(1f).fillMaxHeight().testTag("answer-slot-$index").semantics{contentDescription=if(judged)tr("s049" ,index+1,entered?:tr("s050"),if(matches)tr("s051")else tr("s052" ,I18n.note(NoteEngine.name(note))))else tr("s053" ,index+1,entered?:tr("s050"))},contentPadding=PaddingValues(2.dp)){
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                Text(if(judged)"${if(matches)"○"else "×"} $entered"else entered?:tr("s054" ,index+1),maxLines=1,style=MaterialTheme.typography.titleMedium)
                                if(judged&&!matches&&!compact)Text(tr("s055" ,I18n.note(NoteEngine.name(note))),style=MaterialTheme.typography.labelSmall,maxLines=1)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().heightIn(min=if(compact)40.dp else 56.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(vm.error.ifBlank{vm.scoreError?:if(compact)when{
                        !vm.progressLoaded->tr("s056")
                        vm.quizMode=="listen"&&!vm.heard->tr("s057")
                        vm.correct==true->tr("s058")
                        vm.correct==false->tr("s059")
                        vm.selectedAnswerIndex!=null->tr("s060")
                        else->tr("s061")
                    }else vm.feedback.ifBlank{if(!vm.progressLoaded)tr("s056")else if(vm.quizMode=="listen"&&!vm.heard)tr("s062")else if(vm.selectedAnswerIndex!=null)tr("s063")else tr("s064")}},style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f).testTag("card-feedback"))
                    if(vm.error.isNotBlank()||vm.hasPendingProgress)TextButton(onClick=vm::retry,contentPadding=PaddingValues(2.dp)){Text(tr("s065"))}
                    else if(vm.quizMode=="listen"&&compact)TextButton(onClick={vm.playSound(reference=true)},enabled=!vm.playing,contentPadding=PaddingValues(2.dp),modifier=Modifier.testTag("reference-c")){Text(tr("s066"),maxLines=1)}
                    else if(vm.quizMode=="read")TextButton(onClick={textInput=true},enabled=vm.canAnswer,contentPadding=PaddingValues(2.dp),modifier=Modifier.semantics{contentDescription=tr("s067")}){Text(tr("s068"))}
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                val label=if(vm.playing)tr("s069")else if(vm.heard)tr("s070")else tr("s071")
                if(vm.quizMode=="listen"&&!vm.heard)Button(onClick={vm.playSound()},enabled=!vm.playing&&vm.progressLoaded,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("play-notes")){Text(label,maxLines=1)}
                else OutlinedButton(onClick={vm.playSound()},enabled=!vm.playing&&vm.progressLoaded,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("play-notes")){Text(label,maxLines=1)}
                OutlinedButton(onClick=vm::stopAudio,enabled=vm.playing,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(48.dp).testTag("stop-notes").semantics{contentDescription=tr("s072")}){Text("■")}
            }
            if(vm.quizMode=="listen"&&!compact)TextButton(onClick={vm.playSound(reference=true)},enabled=!vm.playing,modifier=Modifier.fillMaxWidth().testTag("reference-c")){Text(tr("s066"))}
            if(vm.mode==TrainingMode.ANSWER){
                listOf("ド","レ","ミ","ファ","ソ","ラ","シ","⌫").chunked(4).forEach{notes->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    notes.forEach{note->OutlinedButton(onClick={if(note=="⌫")vm.backspace()else vm.note(note)},enabled=vm.canAnswer&&(note!="⌫"||vm.answer.text.isNotEmpty()),modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag(if(note=="⌫")"answer-backspace"else "note-$note"),contentPadding=PaddingValues(0.dp)){Text(I18n.note(note),maxLines=1,softWrap=false)}}
                }}
            }else{
                Text(if(vm.revealed)vm.card.notes.joinToString(" · "){I18n.note(NoteEngine.name(it))}else if(vm.mode==TrainingMode.AUTO)tr("s073" ,vm.interval)else tr("s074"),textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).testTag("card-answer"))
                if(vm.mode==TrainingMode.FLASHCARD)OutlinedButton(onClick=vm::reveal,enabled=vm.scoreReady,modifier=Modifier.fillMaxWidth().testTag("card-reveal")){Text(if(vm.revealed)tr("s075")else tr("s076"))}
                else Button(onClick=vm::toggleAuto,enabled=vm.scoreReady,modifier=Modifier.fillMaxWidth().testTag("card-auto")){Text(if(vm.auto)tr("s077")else tr("s078"))}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Button(onClick=vm::nextQuestion,enabled=vm.progressLoaded&&!vm.hasPendingResponses&&(vm.quizMode=="listen"||vm.scoreReady||vm.scoreError!=null),modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("card-next"),contentPadding=PaddingValues(6.dp)){
                Text(if(vm.settingsPending)tr("s079")else if(vm.quizMode=="listen")tr("s080")else tr("s081"),maxLines=1)
            }
        }
    }
    if(textInput)AlertDialog(onDismissRequest={textInput=false},title={Text(tr("s082"))},text={OutlinedTextField(vm.answer,{vm.answerChanged(it)},singleLine=true,modifier=Modifier.testTag("card-text-input"))},confirmButton={TextButton(onClick={vm.submitAnswer();if(vm.correct!=null)textInput=false}){Text(tr("s083"))}},dismissButton={TextButton(onClick={textInput=false}){Text(tr("s030"))}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun CardSettingsSheet(vm:PianoViewModel,close:()->Unit){
    var selected by remember{mutableStateOf(TrainingSettings(vm.options,vm.configuredMode,vm.interval))}
    var volume by remember{mutableFloatStateOf(vm.volume)}
    ModalBottomSheet(onDismissRequest=close,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal=20.dp)){
            Text(tr("s084"),style=MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text(tr("s085"),style=MaterialTheme.typography.bodySmall)
                SettingChoice(tr("s086"),selected.options.clef,listOf("treble" to tr("s044"),"bass" to tr("s045"),"both" to tr("s087"))){selected=selected.copy(options=selected.options.copy(clef=it))}
                SettingChoice(tr("s088"),selected.options.count.toString(),listOf("1" to tr("s089"),"2" to tr("s090"),"3" to tr("s091"),"0" to tr("s092"))){selected=selected.copy(options=selected.options.copy(count=it.toInt()))}
                SettingChoice(tr("s093"),selected.options.range,listOf("staff" to tr("s094"),"basic" to tr("s095"),"wide" to tr("s096"))){selected=selected.copy(options=selected.options.copy(range=it))}
                Text(tr("s097" ,(volume*100).roundToInt()),style=MaterialTheme.typography.labelMedium)
                Slider(volume,{volume=it},valueRange=.01f..1f,modifier=Modifier.semantics{contentDescription=tr("s098")})
                if(vm.quizMode=="read"){
                    SettingChoice(tr("s099"),selected.mode.name,TrainingMode.entries.map{it.name to it.label}){selected=selected.copy(mode=TrainingMode.valueOf(it))}
                    if(selected.mode==TrainingMode.AUTO)SettingChoice(tr("s100"),selected.interval.toString(),listOf("3" to tr("s101"),"5" to tr("s102"),"8" to tr("s103"))){selected=selected.copy(interval=it.toInt())}
                }
                Text(tr("s104"),style=MaterialTheme.typography.bodySmall)
            }
            Button(onClick={vm.applyCardSettings(selected);vm.changeVolume(volume);close()},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp).testTag("apply-card-settings")){Text(tr("s105"))}
        }
    }
}
@Composable private fun SettingChoice(label:String,value:String,items:List<Pair<String,String>>,change:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    Column{Text(label,style=MaterialTheme.typography.labelMedium);Box{
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(items.first{it.first==value}.second)}
        DropdownMenu(expanded,{expanded=false}){items.forEach{(key,title)->DropdownMenuItem(text={Text(title)},onClick={change(key);expanded=false})}}
    }}
}
