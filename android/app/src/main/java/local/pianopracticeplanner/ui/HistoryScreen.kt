package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import local.pianopracticeplanner.data.PracticeRecord

@Composable internal fun HistoryScreen(rows:List<PracticeRecord>,query:String,setQuery:(String)->Unit,edit:(PracticeRecord)->Unit,delete:(PracticeRecord)->Unit){
    var detail by remember{mutableStateOf<PracticeRecord?>(null)}
    val visible=rows.filter{record->listOf(record.date,record.piece,record.practiceRange,record.memo,record.next,record.difficultParts,record.unpracticedParts,record.finishingImage).any{it.contains(query.trim(),ignoreCase=true)}}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        OutlinedTextField(value=query,onValueChange=setQuery,label={Text(tr("s016"))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("search"))
        Text(tr("s017" ,visible.size),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(visible.isEmpty())item{Text(if(query.isBlank())tr("s018")else tr("s019"))}
            visible.groupBy{it.date}.forEach{(date,records)->
                item(key="day-$date"){Text(tr("s020" ,date,records.sumOf{it.minutes}),style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=8.dp))}
                items(records,key={it.id}){record->OutlinedCard(onClick={detail=record},modifier=Modifier.fillMaxWidth().testTag("record-${record.id}")){
                    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Row{Text(record.piece,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Text(tr("s021" ,record.minutes))}
                        val summary=listOf(record.difficultParts,record.unpracticedParts,record.finishingImage,record.memo).firstOrNull{it.isNotBlank()}
                        if(summary!=null)Text(summary,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
                        Text(tr("s022"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    }
                }}
            }
            item{Spacer(Modifier.height(12.dp))}
        }
    }
    detail?.let{record->AlertDialog(onDismissRequest={detail=null},title={Text(record.piece)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text(tr("s023" ,record.date,record.minutes))
        listOf(tr("s024") to record.difficultParts,tr("s014") to record.unpracticedParts,tr("s015") to record.finishingImage,
            tr("s025") to record.practiceRange,tr("s026") to record.memo,tr("s027") to record.next).filter{it.second.isNotBlank()}.forEach{(title,text)->Text(title,style=MaterialTheme.typography.titleSmall);Text(text)}
        TextButton(onClick={detail=null;delete(record)},modifier=Modifier.testTag("delete-${record.id}")){Text(tr("s028"),color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={detail=null;edit(record)},modifier=Modifier.testTag("edit-${record.id}")){Text(tr("s029"))}},dismissButton={TextButton(onClick={detail=null}){Text(tr("s030"))}})}
}
