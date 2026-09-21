package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import local.pianopracticeplanner.data.*
import local.pianopracticeplanner.domain.*
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

internal fun optionsLabel(value:CardOptions)=
    tr("s226" ,when(value.clef){"treble"->tr("s044");"bass"->tr("s045");else->tr("s227")},if(value.count==0)"1〜3" else value.count,when(value.range){"staff"->tr("s228");"basic"->tr("s229");else->tr("s230")})
internal fun durationLabel(seconds:Long):String=when{
    seconds==0L->tr("s231")
    seconds<60->tr("s232" ,seconds)
    seconds<3600->tr("s233" ,seconds/60,if(seconds%60>0)tr("s232" ,seconds%60)else "")
    else->tr("s234" ,seconds/3600,(seconds%3600)/60)
}

@Composable internal fun ReviewScreen(vm:PianoViewModel,rows:List<PracticeRecord>){
    val attempts by vm.attempts.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var view by rememberSaveable{mutableStateOf("accuracy")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(view=="accuracy",{view="accuracy"},{Text(tr("s235"))},modifier=Modifier.weight(1f))
            FilterChip(view=="calendar",{view="calendar"},{Text(tr("s236"))},modifier=Modifier.weight(1f))
        }
        if(view=="accuracy")AccuracyPanel(vm,attempts)else PracticeCalendar(vm,rows,sessions)
    }
}

@Composable private fun AccuracyPanel(vm:PianoViewModel,attempts:List<NoteAttempt>){
    var selectedMode by rememberSaveable{mutableStateOf(vm.quizMode)}
    var selected by remember{mutableStateOf(vm.options)}
    var conditions by remember{mutableStateOf(false)}
    var custom by remember{mutableStateOf(false)}
    var countText by remember{mutableStateOf(vm.recentN.toString())}
    val keys=(listOf(selected)+attempts.filter{it.mode==selectedMode}.map{CardOptions(it.clefOption,it.countOption,it.range)}).distinct()
    val result=ProgressStats.calculate(attempts,selectedMode,selected,vm.recentN)
    Text(tr("s237"),style=MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FilterChip(selectedMode=="read",{selectedMode="read"},{Text(tr("s031"))})
        FilterChip(selectedMode=="listen",{selectedMode="listen"},{Text(tr("s032"))})
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
        listOf(10,20,50,100).forEach{n->FilterChip(vm.recentN==n,{vm.changeRecentN(n)},{Text("$n",maxLines=1)},modifier=Modifier.weight(1f))}
        TextButton(onClick={countText=vm.recentN.toString();custom=true},contentPadding=PaddingValues(2.dp)){Text(tr("s222"))}
    }
    Text(tr("s238" ,vm.recentN),style=MaterialTheme.typography.bodySmall)
    Box {
        OutlinedButton(onClick={conditions=true},modifier=Modifier.fillMaxWidth()){Text(optionsLabel(selected))}
        DropdownMenu(conditions,{conditions=false}){keys.forEach{key->DropdownMenuItem(text={Text(optionsLabel(key))},onClick={selected=key;conditions=false})}}
    }
    Card(colors=CardDefaults.cardColors(containerColor=PianoPalette.Soft),modifier=Modifier.fillMaxWidth().testTag("accuracy-card")){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(tr("s239" ,result.recent.size),style=MaterialTheme.typography.bodySmall)
            Text(result.rate?.let{"${(it*100).roundToInt()}%"}?:tr("s240"),style=MaterialTheme.typography.displayMedium)
            Text(tr("s241" ,result.recent.count{it.correct},result.recent.size))
            if(result.previousRate!=null&&result.rate!=null){
                val delta=((result.rate-result.previousRate)*100).roundToInt()
                Text(tr("s242" ,if(delta>0)"↑ +"else if(delta<0)"↓ "else "",delta,vm.recentN,(result.previousRate*100).roundToInt()),style=MaterialTheme.typography.bodySmall)
            }else Text(tr("s243" ,vm.recentN),style=MaterialTheme.typography.bodySmall)
            AccuracyTrend(attempts,selectedMode,selected,vm.recentN)
        }
    }
    Text(tr("s244"),style=MaterialTheme.typography.bodySmall,color=PianoPalette.Muted)
    if(result.recent.isNotEmpty()){
        Text(tr("s245"),style=MaterialTheme.typography.titleSmall)
        RecentAnswerGrid(result.recent.take(100).reversed())
    }
    Button(onClick={vm.switchTab("cards")},modifier=Modifier.fillMaxWidth()){Text(tr("s246"))}
    TextButton(onClick={vm.query="";vm.switchTab("history")}){Text(tr("s247"))}
    if(custom)AlertDialog(onDismissRequest={custom=false},title={Text(tr("s248"))},text={
        OutlinedTextField(countText,{countText=it},label={Text(tr("s249"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError=countText.toIntOrNull() !in 1..1000)
    },confirmButton={TextButton(onClick={vm.changeRecentN(countText.toInt());custom=false},enabled=countText.toIntOrNull() in 1..1000){Text(tr("s250"))}},dismissButton={TextButton(onClick={custom=false}){Text(tr("s176"))}})
}

@Composable internal fun RecentAnswerGrid(attempts:List<NoteAttempt>){
    FlowRow(Modifier.fillMaxWidth().testTag("recent-answer-grid"),horizontalArrangement=Arrangement.spacedBy(3.dp,Alignment.CenterHorizontally),verticalArrangement=Arrangement.spacedBy(3.dp),maxItemsInEachRow=10){
        attempts.forEachIndexed{index,attempt->
            Box(Modifier.size(30.dp).background(if(attempt.correct)PianoPalette.Soft else PianoPalette.Background,RoundedCornerShape(4.dp)).border(1.dp,PianoPalette.Line,RoundedCornerShape(4.dp)).testTag("recent-answer-$index").semantics{contentDescription="${attempt.date} ${if(attempt.correct)tr("s051")else tr("incorrect")}"},contentAlignment=Alignment.Center){
                Text(if(attempt.correct)"○"else "×",color=PianoPalette.Blue,style=MaterialTheme.typography.labelMedium,modifier=Modifier.testTag("recent-answer-glyph-$index"))
            }
        }
    }
}

@Composable private fun AccuracyTrend(attempts:List<NoteAttempt>,mode:String,options:CardOptions,n:Int){
    val eligible=attempts.filter{!it.assisted&&it.mode==mode&&it.clefOption==options.clef&&it.countOption==options.count&&it.range==options.range}.sortedWith(compareByDescending<NoteAttempt>{it.answeredAt}.thenByDescending{it.id})
    val windows=eligible.chunked(n).filterIndexed{index,window->index==0||window.size==n}.take(6).reversed()
    if(windows.isEmpty())return
    val rates=windows.map{it.count{a->a.correct}.toFloat()/it.size}
    Canvas(Modifier.fillMaxWidth().height(95.dp).semantics{contentDescription=tr("s251" ,n,rates.joinToString{(it*100).roundToInt().toString()+"%"})}){
        val inset=8.dp.toPx();val width=size.width-inset*2;val height=size.height-inset*2
        listOf(0f,.5f,1f).forEach{drawLine(PianoPalette.Line,Offset(inset,inset+height*it),Offset(inset+width,inset+height*it),1.dp.toPx())}
        val points=rates.mapIndexed{i,v->Offset(inset+width*(if(rates.size==1)1f else i.toFloat()/(rates.size-1)),inset+height*(1-v))}
        points.zipWithNext().forEach{(a,b)->drawLine(PianoPalette.Blue,a,b,3.dp.toPx())}
        points.forEach{drawCircle(PianoPalette.Blue,4.dp.toPx(),it)}
    }
}

@Composable private fun PracticeCalendar(vm:PianoViewModel,rows:List<PracticeRecord>,sessions:List<NoteSession>){
    var monthText by rememberSaveable{mutableStateOf(YearMonth.now().toString())}
    val month=YearMonth.parse(monthText)
    var day by rememberSaveable{mutableStateOf(LocalDate.now().toString())}
    var type by rememberSaveable{mutableStateOf("all")}
    val piano=rows.groupBy{it.date}.mapValues{(_,v)->v.sumOf{it.minutes.toLong()*60}}
    val notes=sessions.groupBy{it.date}.mapValues{(_,v)->v.sumOf{it.seconds.toLong()}}
    fun total(date:String)= (if(type!="notes")piano[date]?:0L else 0L)+(if(type!="piano")notes[date]?:0L else 0L)
    val dates=(1..month.lengthOfMonth()).map{month.atDay(it).toString()}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("all" to tr("s252"),"piano" to tr("s253"),"notes" to tr("s172")).forEach{(key,label)->FilterChip(type==key,{type=key},{Text(label)})}
    }
    Text(tr("s254" ,durationLabel(dates.sumOf{total(it)}),dates.count{total(it)>0}),style=MaterialTheme.typography.titleMedium)
    OutlinedCard(Modifier.fillMaxWidth().testTag("practice-calendar")){
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                TextButton(onClick={monthText=month.minusMonths(1).toString()},modifier=Modifier.semantics{contentDescription=tr("s255")}){Text("‹")}
                Text(tr("s256" ,month.year,month.monthValue),style=MaterialTheme.typography.titleMedium)
                TextButton(onClick={monthText=month.plusMonths(1).toString()},enabled=month<YearMonth.now(),modifier=Modifier.semantics{contentDescription=tr("s257")}){Text("›")}
            }
            Row{listOf(tr("s258"),tr("s259"),tr("s260"),tr("s261"),tr("s262"),tr("s263"),tr("s264")).forEach{Text(it,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall)}}
            val offset=month.atDay(1).dayOfWeek.value%7
            val cells=List(offset){0}+(1..month.lengthOfMonth()).toList()
            cells.chunked(7).forEach{week->Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){
                week.forEach{number->if(number==0)Spacer(Modifier.weight(1f))else{
                    val date=month.atDay(number);val seconds=total(date.toString());val level=ProgressStats.heatLevel(seconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    val color=PianoPalette.Heat[level]
                    Surface(onClick={day=date.toString()},enabled=date<=LocalDate.now(),shape=RoundedCornerShape(8.dp),color=color,
                        contentColor=if(level==4)Color.White else PianoPalette.Ink,
                        border=if(day==date.toString())androidx.compose.foundation.BorderStroke(2.dp,PianoPalette.Blue)else null,
                        modifier=Modifier.weight(1f).heightIn(min=52.dp).semantics{contentDescription="$date ${durationLabel(seconds)}"}){
                        Column(Modifier.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Text(number.toString(),style=MaterialTheme.typography.bodySmall)
                            Text(if(seconds==0L)"−"else if(seconds<60)tr("s265")else tr("s021" ,seconds/60),style=MaterialTheme.typography.labelSmall,maxLines=1)
                        }
                    }
                }}
                repeat(7-week.size){Spacer(Modifier.weight(1f))}
            }}
            Text(tr("s266"),style=MaterialTheme.typography.labelSmall,color=PianoPalette.Muted,modifier=Modifier.padding(top=8.dp))
        }
    }
    OutlinedCard(Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(day,style=MaterialTheme.typography.titleMedium)
            Text(tr("s267" ,durationLabel(piano[day]?:0),durationLabel(notes[day]?:0)))
            rows.filter{it.date==day}.forEach{Text(tr("s023" ,it.piece,it.minutes),style=MaterialTheme.typography.bodySmall)}
            TextButton(onClick={vm.query=day;vm.switchTab("history")}){Text(tr("s268"))}
        }
    }
    Text(tr("s269"),style=MaterialTheme.typography.bodySmall,color=PianoPalette.Muted)
    TextButton(onClick={vm.query="";vm.switchTab("history")}){Text(tr("s270"))}
}
