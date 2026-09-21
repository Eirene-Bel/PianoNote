package local.pianopracticeplanner.ui

import local.pianopracticeplanner.i18n.tr

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.*
import local.pianopracticeplanner.domain.NoteCard
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

private const val ORIGIN="https://appassets.androidplatform.net"

@SuppressLint("SetJavaScriptEnabled")
@Composable fun ScoreWebView(card: NoteCard,feedback:List<String> = emptyList(),modifier: Modifier=Modifier,onRendered: (String?)->Unit={}) {
    val context=LocalContext.current
    val current by rememberUpdatedState(card)
    val currentFeedback by rememberUpdatedState(feedback)
    val report by rememberUpdatedState(onRendered)
    var ready by remember { mutableStateOf(false) }
    val sentId=remember{arrayOfNulls<String>(1)}
    fun send(view: WebView,value: NoteCard,labels:List<String>) {
        val key=value.id+":"+labels.joinToString(",")
        if(sentId[0]==key)return
        sentId[0]=key
        val json=JSONObject().put("schema",1).put("cardId",value.id).put("clef",value.clef).put("notes",JSONArray(value.notes)).put("feedback",JSONArray(labels)).put("language",local.pianopracticeplanner.i18n.I18n.language).put("noteNames",JSONArray((0..6).map { local.pianopracticeplanner.i18n.I18n.note(local.pianopracticeplanner.domain.NoteEngine.name(it)) })).put("description",tr("s043",tr(if(value.clef=="treble")"s044"else "s045"),value.notes.size, "")).toString()
        WebViewCompat.postWebMessage(view,WebMessageCompat(json),Uri.parse(ORIGIN))
    }
    val web=remember {
        WebView(context).apply {
            settings.javaScriptEnabled=true
            settings.allowFileAccess=false;settings.allowContentAccess=false
            settings.domStorageEnabled=false;settings.blockNetworkLoads=true
            isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            val loader=WebViewAssetLoader.Builder().addPathHandler("/assets/",WebViewAssetLoader.AssetsPathHandler(context)).build()
            webViewClient=object: WebViewClientCompat(){
                override fun shouldInterceptRequest(view: WebView,request: WebResourceRequest): WebResourceResponse =
                    loader.shouldInterceptRequest(request.url) ?: WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream(byteArrayOf()))
                override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest)=true
                override fun onReceivedError(view: WebView,request: WebResourceRequest,error: WebResourceErrorCompat){if(request.isForMainFrame)report(tr("s155"))}
            }
            if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
                WebViewCompat.addWebMessageListener(this,"AndroidScore",setOf(ORIGIN)){view,message,origin,isMainFrame,_ ->
                    if(!isMainFrame||origin.toString()!=ORIGIN)return@addWebMessageListener
                    runCatching {
                        val result=JSONObject(message.data?:"")
                        when(result.optString("type")){
                            "ready"->{ready=true;send(view,current,currentFeedback)}
                            "rendered"->if(result.optString("cardId")==current.id)report(null)
                            "error"->if(result.optString("cardId")==current.id||result.optString("cardId").isEmpty())report(tr("s156"))
                        }
                    }
                }
                loadUrl("$ORIGIN/assets/score/index.html")
            }else report(tr("s157"))
        }
    }
    AndroidView(factory={web},modifier=modifier,update={if(ready)send(it,card,feedback)})
    DisposableEffect(web){onDispose{web.stopLoading();web.destroy()}}
}
