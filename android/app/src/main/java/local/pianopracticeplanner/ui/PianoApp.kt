package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import local.pianopracticeplanner.data.PracticeRecord
import java.time.LocalDate



@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PianoApp(vm:PianoViewModel) {
    val rows by vm.records.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val lifecycle=LocalLifecycleOwner.current
    val keyboardOpen=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val compactKeyboard=keyboardOpen&&LocalConfiguration.current.screenHeightDp<500
    var menu by remember{mutableStateOf(false)}
    var settings by remember{mutableStateOf(false)}
    var delete by remember{mutableStateOf<PracticeRecord?>(null)}
    var edit by remember{mutableStateOf<PracticeRecord?>(null)}
    var clear by remember{mutableStateOf(false)}
    val snackbar=remember{SnackbarHostState()}
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)scope.launch {
            try{val text=vm.exportBackup();withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openOutputStream(uri,"w")).use{it.write(text.toByteArray(Charsets.UTF_8));it.flush()}};vm.reportIo(success=tr("s158"))}
            catch(e:Exception){vm.reportIo(failure=tr("s159"),retry="export")}
        }
    }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch {
            try{val text=withContext(Dispatchers.IO){requireNotNull(context.contentResolver.openInputStream(uri)).use{val bytes=it.readNBytes(20*1024*1024+1);require(bytes.size<=20*1024*1024);Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()}};vm.prepareImport(text)}
            catch(e:Exception){vm.reportIo(failure=tr("s160"),retry="import")}
        }
    }
    val retry:()->Unit={when(vm.retryKind){"import"->importer.launch(arrayOf("application/json","text/plain","application/octet-stream"));"export"->exporter.launch("piano-practice-${LocalDate.now()}.json");else->vm.retry()}}
    DisposableEffect(lifecycle){
        val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_PAUSE->{vm.setAppVisible(false);vm.flushDraft()};Lifecycle.Event.ON_RESUME->{vm.onResume();vm.setAppVisible(true)};else->{}}}
        lifecycle.lifecycle.addObserver(observer)
        if(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))vm.setAppVisible(true)
        onDispose{lifecycle.lifecycle.removeObserver(observer);vm.setAppVisible(false)}
    }
    LaunchedEffect(vm.error,vm.notice,vm.tab){
        snackbar.currentSnackbarData?.dismiss()
        if(vm.tab!="practice"&&vm.tab!="cards"&&(vm.error.isNotBlank()||vm.notice.isNotBlank())){
            val result=snackbar.showSnackbar(vm.error.ifBlank{vm.notice},actionLabel=if(vm.retryKind.isNotEmpty())tr("s065")else null,withDismissAction=true,duration=if(vm.error.isNotBlank())SnackbarDuration.Indefinite else SnackbarDuration.Short)
            if(result==SnackbarResult.ActionPerformed)retry()
        }
    }
    BackHandler(enabled=vm.tab!="practice"){vm.switchTab("practice")}
    PianoTheme{
        Scaffold(
            topBar={if(!compactKeyboard&&!(vm.tab=="cards"&&LocalConfiguration.current.screenHeightDp<700))TopAppBar(title={Text(when(vm.tab){"history"->tr("s161");"review"->tr("s162");"settings"->tr("s163");"cards"->if(vm.quizMode=="listen")tr("s032")else tr("s164");else->if(vm.editingId!=null)tr("s165")else tr("s166")})},actions={
                if(vm.tab=="history")TextButton(onClick={vm.switchTab("review")}){Text(tr("s167"))}
                else if(vm.tab=="practice") Box{TextButton(onClick={menu=true},enabled=!vm.busy){Text(tr("s168"))};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                    DropdownMenuItem(text={Text(tr("s169"))},onClick={menu=false;exporter.launch("piano-practice-${LocalDate.now()}.json")},enabled=vm.loaded)
                    DropdownMenuItem(text={Text(tr("s170"))},onClick={menu=false;importer.launch(arrayOf("application/json","text/plain","application/octet-stream"))},enabled=vm.loaded)
                }}
            })},
            snackbarHost={SnackbarHost(snackbar)},
            bottomBar={if(!keyboardOpen)NavigationBar{listOf(Triple("practice",tr("s171"),"＋"),Triple("cards",tr("s172"),"♪"),Triple("review",tr("s162"),"▥"),Triple("settings",tr("s163"),"⚙")).forEach{(key,label,icon)->
                NavigationBarItem(selected=vm.tab==key||(key=="review"&&vm.tab=="history"),onClick={vm.switchTab(key)},enabled=!vm.busy&&(key=="cards"||vm.loaded),icon={Text(icon)},label={Text(label)},modifier=Modifier.testTag("nav-$key"))
            }}}
        ){padding->
            Box(Modifier.fillMaxSize().padding(padding).imePadding()){
                when(vm.tab){
                    "cards"->CardsWorkspace(vm)
                    "review"->ReviewScreen(vm,rows)
                    "settings"->TransferScreen(vm,{exporter.launch("piano-practice-${LocalDate.now()}.json")},{importer.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){settings=true}
                    "history"->HistoryScreen(rows,vm.query,{vm.query=it},{record->if(vm.hasDraft())edit=record else vm.edit(record)},{delete=it})
                    else->PracticeScreen(vm,rows,{clear=true},retry)
                }
            }
        }
        if(settings)CardSettingsSheet(vm){settings=false;vm.resumeQuestion()}
        delete?.let{record->AlertDialog(onDismissRequest={delete=null},title={Text(tr("s173"))},text={Text(tr("s174" ,record.date,record.piece))},confirmButton={TextButton(onClick={delete=null;vm.delete(record)}){Text(tr("s175"))}},dismissButton={TextButton(onClick={delete=null}){Text(tr("s176"))}})}
        edit?.let{record->AlertDialog(onDismissRequest={edit=null},title={Text(tr("s177"))},text={Text(tr("s178"))},confirmButton={TextButton(onClick={edit=null;vm.edit(record)}){Text(tr("s029"))}},dismissButton={TextButton(onClick={edit=null}){Text(tr("s176"))}})}
        if(clear)AlertDialog(onDismissRequest={clear=false},title={Text(tr("s179"))},text={Text(tr("s180"))},confirmButton={TextButton(onClick={clear=false;vm.clearInput()}){Text(tr("s181"))}},dismissButton={TextButton(onClick={clear=false}){Text(tr("s176"))}})
        vm.importReport?.let{report->AlertDialog(onDismissRequest={if(!vm.busy)vm.cancelImport()},title={Text(tr("s182"))},text={
            Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text(tr("s183" ,report.attemptsAdded,report.sessionsAdded,report.added,report.deletedCount,report.skipped))
                if(report.settingsAvailable)Text(if(report.settingsPreserved)tr("s184")else tr("s185"))
                if(report.draftAvailable)Text(if(report.draftPreserved)tr("s186")else tr("s187"))
                Text(tr("s188"),style=MaterialTheme.typography.bodySmall)
                if(report.conflicts.isNotEmpty()){
                    Text(tr("s189" ,report.conflicts.size))
                    report.conflicts.forEach{conflict->Text("${conflict.date}  ${conflict.piece}",style=MaterialTheme.typography.titleSmall);Text(conflict.detail,style=MaterialTheme.typography.bodySmall)}
                    Text(tr("s190"))
                }
            }
        },confirmButton={TextButton(onClick={vm.applyImport(skipConflicts=report.conflicts.isNotEmpty())},enabled=!vm.busy&&(report.added>0||report.deletedCount>0||report.attemptsAdded>0||report.sessionsAdded>0||(report.settingsAvailable&&!report.settingsPreserved)||(report.draftAvailable&&!report.draftPreserved))){Text(if(report.conflicts.isNotEmpty())tr("s191")else if(report.added==0&&report.deletedCount>0)tr("s192")else tr("s193"))}},dismissButton={TextButton(onClick=vm::cancelImport,enabled=!vm.busy){Text(tr("s030"))}})}
    }
}
