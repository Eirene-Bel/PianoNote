package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import local.pianopracticeplanner.data.PracticeRecord
import java.time.LocalDate

@Composable internal fun PracticeScreen(vm:PianoViewModel,rows:List<PracticeRecord>,clear:()->Unit,retry:()->Unit){
    val context=LocalContext.current
    val keyboard=LocalSoftwareKeyboardController.current
    val wide=LocalConfiguration.current.screenWidthDp>=600
    var recent by remember{mutableStateOf(false)}
    var replacement by remember{mutableStateOf<PracticeRecord?>(null)}
    val input=vm.input
    Column(Modifier.fillMaxSize()){
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            if(!vm.loaded){Text(tr("s194"));TextButton(onClick=vm::loadDraft){Text(tr("s195"))}}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                val isToday=input.date==LocalDate.now().toString()
                TextButton(onClick={val d=LocalDate.parse(input.date);DatePickerDialog(context,{_,y,m,day->vm.changeInput(input.copy(date=LocalDate.of(y,m+1,day).toString()))},d.year,d.monthValue-1,d.dayOfMonth).show()},enabled=!vm.busy){Text(if(isToday)tr("s196" ,input.date)else tr("s197" ,input.date),maxLines=1)}
                if(!isToday)TextButton(onClick={vm.changeInput(input.copy(date=LocalDate.now().toString()))},enabled=!vm.busy){Text(tr("s198"),maxLines=1,softWrap=false)}
                if(wide&&vm.recentRecords.isNotEmpty()&&vm.editingId==null)TextButton(onClick={recent=true},enabled=!vm.busy,modifier=Modifier.testTag("recent-practice")){Text(tr("s199"))}
            }
            if(input.date!=LocalDate.now().toString())Text(tr("s200" ,LocalDate.now()),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary,modifier=Modifier.testTag("past-date-notice"))
            if(wide)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){ContentField(vm,Modifier.weight(1f));TimeField(vm,Modifier.weight(1f))}
            else{
                ContentField(vm,Modifier.fillMaxWidth())
                if(vm.recentRecords.isNotEmpty()&&vm.editingId==null)TextButton(onClick={recent=true},enabled=!vm.busy,modifier=Modifier.testTag("recent-practice")){Text(tr("s199"))}
                TimeField(vm,Modifier.fillMaxWidth())
            }
            Text(tr("s201"),style=MaterialTheme.typography.bodySmall)
            Reflection(vm,"difficult",tr("s024"),tr("s202"),input.difficultParts)
            Reflection(vm,"unpracticed",tr("s203"),tr("s204"),input.unpracticedParts)
            Reflection(vm,"finishing",tr("s205"),tr("s206"),input.finishingImage)
            if(vm.hasDraft())TextButton(onClick=clear,enabled=!vm.busy){Text(if(vm.editingId==null)tr("s181")else tr("s207"))}
            val entries=rows.filter{it.date==input.date}
            TextButton(onClick={vm.query=input.date;vm.switchTab("history")}){Text(tr("s208" ,entries.size,entries.sumOf{it.minutes}),style=MaterialTheme.typography.bodySmall)}
            Spacer(Modifier.height(8.dp))
        }
        Surface(shadowElevation=4.dp){Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            val status=when{vm.error.isNotBlank()->vm.error;vm.notice.isNotBlank()->vm.notice;vm.composing->tr("s209");else->vm.draftStatus}
            if(status.isNotBlank())Text(status,style=MaterialTheme.typography.bodySmall,color=if(vm.error.isNotBlank())MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,modifier=Modifier.testTag(if(vm.error.isNotBlank())"global-error"else "draft-status"))
            if(vm.error.isNotBlank()&&vm.retryKind.isNotBlank())TextButton(onClick=retry){Text(if(vm.retryKind=="import")tr("s210")else if(vm.retryKind=="export")tr("s211")else tr("s065"))}
            Button(onClick={keyboard?.hide();vm.save()},enabled=vm.loaded&&!vm.busy&&!vm.composing,modifier=Modifier.fillMaxWidth().testTag("save")){Text(if(vm.busy)tr("s212")else if(vm.editingId==null)tr("s213")else tr("s214"))}
        }}
    }
    if(recent)AlertDialog(onDismissRequest={recent=false},title={Text(tr("s215"))},text={LazyColumn(Modifier.heightIn(max=400.dp)){
        items(vm.recentRecords,key={it.id}){record->TextButton(onClick={recent=false;if(vm.hasDraft())replacement=record else vm.useRecent(record)},modifier=Modifier.fillMaxWidth().testTag("reuse-${record.id}")){
            Column(Modifier.fillMaxWidth()){Text(record.piece,maxLines=2,overflow=TextOverflow.Ellipsis);Text(tr("s216" ,record.minutes,record.date),style=MaterialTheme.typography.bodySmall)}
        }}
    }},confirmButton={TextButton(onClick={recent=false}){Text(tr("s030"))}})
    replacement?.let{record->AlertDialog(onDismissRequest={replacement=null},title={Text(tr("s177"))},text={Text(tr("s217" ,record.piece))},confirmButton={TextButton(onClick={vm.useRecent(record);replacement=null}){Text(tr("s218"))}},dismissButton={TextButton(onClick={replacement=null}){Text(tr("s176"))}})}
}

@Composable private fun ContentField(vm:PianoViewModel,modifier:Modifier){
    OutlinedTextField(value=vm.textValue("piece"),onValueChange={vm.changeText("piece",it)},label={Text(tr("s010"))},placeholder={Text(tr("s219"))},singleLine=true,enabled=vm.loaded&&!vm.busy,modifier=modifier.testTag("piece"))
}
@Composable private fun TimeField(vm:PianoViewModel,modifier:Modifier){
    Column(modifier){
        OutlinedTextField(value=vm.textValue("minutes"),onValueChange={vm.changeText("minutes",it)},label={Text(tr("s220"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,enabled=vm.loaded&&!vm.busy,modifier=Modifier.fillMaxWidth().testTag("minutes"))
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            listOf(5,15,30).forEach{n->TextButton(onClick={vm.addMinutes(n)},enabled=vm.loaded&&!vm.busy,contentPadding=PaddingValues(horizontal=8.dp)){Text("＋$n",maxLines=1,softWrap=false)}}
            TextButton(onClick=vm::resetMinutes,enabled=vm.loaded&&!vm.busy,modifier=Modifier.testTag("reset-minutes")){Text(tr("s221"))}
        }
    }
}

@Composable private fun Reflection(vm:PianoViewModel,key:String,title:String,example:String,value:String){
    var expanded by rememberSaveable(vm.editingId,key,vm.entryGeneration){mutableStateOf(vm.editingId!=null&&value.isNotBlank())}
    OutlinedCard(Modifier.fillMaxWidth()){
        TextButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth().testTag("expand-$key")){
            Column(Modifier.weight(1f)){
                Text(title,modifier=Modifier.fillMaxWidth())
                Text(if(value.isBlank())tr("s222")else "${value.take(45)}${if(value.length>45)"…"else ""}",style=MaterialTheme.typography.bodySmall,modifier=Modifier.fillMaxWidth(),maxLines=1,overflow=TextOverflow.Ellipsis)
            };Text(if(expanded)"−"else "＋")
        }
        if(expanded)Column(Modifier.padding(start=12.dp,end=12.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(key=="finishing")vm.previousRecord?.takeIf{it.finishingImage.isNotBlank()}?.let{previous->
                Text(tr("s223" ,previous.date,previous.finishingImage),style=MaterialTheme.typography.bodySmall)
                if(value!=previous.finishingImage)TextButton(onClick=vm::inheritGoal){Text(tr("s224"))}
                Text(tr("s225"),style=MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(value=vm.textValue(key),onValueChange={vm.changeText(key,it)},placeholder={Text(example)},minLines=2,maxLines=6,enabled=!vm.busy,modifier=Modifier.fillMaxWidth().testTag(key))
        }
    }
}
