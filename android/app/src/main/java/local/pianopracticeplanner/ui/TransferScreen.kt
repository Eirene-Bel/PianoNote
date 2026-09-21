package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import local.pianopracticeplanner.i18n.I18n
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun TransferScreen(vm:PianoViewModel,export:()->Unit,import:()->Unit,openCardSettings:()->Unit){
    val context = LocalContext.current
    val versionName = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        LanguageSelector(vm)
        Text(tr("s106"),style=MaterialTheme.typography.titleLarge)
        OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(tr("s107"))
            Button(onClick=openCardSettings,modifier=Modifier.fillMaxWidth()){Text(tr("s108"))}
        }}
        HorizontalDivider()
        Text(tr("s109"),style=MaterialTheme.typography.titleLarge)
        Text(tr("s110"),style=MaterialTheme.typography.titleMedium)
        Text(tr("s111"),color=PianoPalette.Muted)
        OutlinedCard(Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text(tr("s112"),style=MaterialTheme.typography.titleSmall)
                Button(onClick=export,enabled=vm.loaded&&!vm.busy,modifier=Modifier.fillMaxWidth()){Text(tr("s113"))}
                OutlinedButton(onClick=import,enabled=vm.loaded&&!vm.busy,modifier=Modifier.fillMaxWidth()){Text(tr("s114"))}
            }
        }
        Text(tr("s115"),style=MaterialTheme.typography.titleMedium)
        Text(tr("s116"))
        Text(tr("s117"),style=MaterialTheme.typography.bodySmall,color=PianoPalette.Muted)
        HorizontalDivider()
        Text(tr("s118"),style=MaterialTheme.typography.bodySmall,color=PianoPalette.Muted)
        Text("PianoNote $versionName",style=MaterialTheme.typography.labelMedium,color=PianoPalette.Muted)
    }
}

@Composable private fun LanguageSelector(vm: PianoViewModel) {
    val context = LocalContext.current
    val manager = context.getSystemService(android.app.LocaleManager::class.java)
    var expanded by remember { mutableStateOf(false) }
    val selected = manager.applicationLocales.toLanguageTags().substringBefore(',')
    Column {
        Text(tr("language"), style = MaterialTheme.typography.titleLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected.isEmpty()) tr("system") else I18n.languages[I18n.resolve(selected)] ?: selected)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                I18n.languages.forEach { (tag, label) ->
                    DropdownMenuItem(text = { Text(if (tag.isEmpty()) tr("system") else label) }, onClick = {
                        expanded = false
                        vm.flushDraft()
                        manager.applicationLocales = android.os.LocaleList.forLanguageTags(tag)
                    })
                }
            }
        }
    }
}
