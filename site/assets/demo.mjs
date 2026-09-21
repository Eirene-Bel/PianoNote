import {t,formatCount,formatDay,formatMonth,weekdays,formatDuration,displayNote} from './i18n.mjs';
import * as Core from './core.mjs';
import {memoryStore} from './storage.mjs';
import {QuestionTimer} from './question-timer.mjs';
import {scoreSvg} from './score.mjs';
const answerTimer=new QuestionTimer();
const $=id=>document.getElementById(id);
const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let TODAY=Core.localDay();
const dayLabel=formatDay;
const duration=formatDuration;
const clock=s=>`${Math.floor(s/60)}:${String(Math.floor(s%60)).padStart(2,'0')}`;
const configKey=c=>`${c.clef}|${c.count}|${c.range}`;
const configName=key=>{const [clef,count,range]=key.split('|');return `${({treble:t("w007"),bass:t("w008"),both:t("w009")})[clef]}${esc(t("w010"))}${formatCount(count==='0'?'1–3':Number(count),'notes')} · ${({staff:t("w012"),basic:t("w013"),wide:t("w014")})[range]}`;};
const noteName=Core.noteName;
const emptyDraft=()=>({day:Core.localDay(),piece:'',minutes:'',difficult:'',unpracticed:'',finishing:''});
let progressError='',recordSaving=false;
let store,revision=0,state=Core.emptyState(),data,ready=false,storageError='',stale=false,queue=Promise.resolve(),pendingWrites=0;
let tab='notes',reviewView='accuracy',statsKey='treble|1|staff',statsMode='read',timeFilter='all',month=TODAY.slice(0,7),selectedDay=TODAY;
let draft=emptyDraft(),editingId=null,editingRevision=null,draftDirty=false,draftTimer,historyQuery='',pendingDelete=null;
let question={...Core.nextQuestion(Core.defaultSettings()),answers:[],firstJudged:false,selected:null,mode:'read',heard:false,playCount:0,assisted:false,pending:null,revealed:false};
let sessionSeconds=0,running=false,sessionDay=TODAY,unflushed=0,pendingSessions=[],toastTimer,autoTimer=null;
let pendingConfig=null,pendingMode=null,noteNotice='';
function draftEnvelope(){return JSON.stringify({input:{date:draft.day,piece:draft.piece,minutes:draft.minutes,difficult_parts:draft.difficult,unpracticed_parts:draft.unpracticed,finishing_image:draft.finishing},editingId:editingId||'',revision:editingRevision||0,operationId:crypto.randomUUID(),goalTouched:true});}
function applyDraft(raw){
 const j=raw?JSON.parse(raw):null,i=j?.input;
 draft=i?{day:i.date,piece:i.piece,minutes:i.minutes,difficult:i.difficult_parts||'',unpracticed:i.unpracticed_parts||'',finishing:i.finishing_image||''}:emptyDraft();
 editingId=j?.editingId||null;editingRevision=j?.revision||null;draftDirty=false;
}
function hasDraft(){return !!editingId||Object.entries(draft).some(([key,value])=>key!=='day'&&value.trim());}
function project(){
 const settings=state.settings||Core.defaultSettings();
 data={records:state.records.map(r=>({...r,day:r.date,difficult:r.difficult_parts,unpracticed:r.unpracticed_parts,finishing:r.finishing_image})),
  attempts:state.attempts.map(a=>({...a,day:a.date,at:a.answeredAt,config:`${a.clefOption}|${a.countOption}|${a.range}`})),
  noteTime:{},n:settings.recentN,quizConfig:{clef:settings.clef,count:settings.count,range:settings.range},quizMode:settings.mode,audioVolume:settings.volume,trainingMode:settings.trainingMode||'ANSWER',interval:settings.interval||5,draft};
 for(const row of state.sessions)data.noteTime[row.date]=(data.noteTime[row.date]||0)+row.seconds;
 if(unflushed>0)data.noteTime[sessionDay]=(data.noteTime[sessionDay]||0)+unflushed;
}
function status(){
 const el=$('storage-status'),messageKey=storageError||progressError,message=messageKey?t(messageKey):'';el.hidden=!message;
 el.innerHTML=(message?`${esc(message)} <button data-action="retry-save">${stale?t("w015"):t("w016")}</button>`:'');
 document.querySelectorAll('[data-action="next-question"]').forEach(b=>b.disabled=!!question.pending);
 const el2=$('save-status');if(el2)el2.textContent=draftDirty?t("w017"):hasDraft()?t("w018"):'';
}
function write(mutate){
 pendingWrites++;
 const task=queue.then(async()=>{
  if(stale)throw new Error(t("w019"));
  const result=await store.commit(revision,mutate);state=result.state;revision=result.revision;storageError='';project();return state;
 });
 queue=task.catch(error=>{stale=error.name==='ConflictError'||stale;storageError=stale?'w020':'w021';consumeTime(answerTimer.pause());running=false;stopAuto();stopSound();status();});
 return task.finally(()=>{pendingWrites--;status();});
}
async function savePending(){
 try{
 if(question.pending){
  const row=structuredClone(question.pending);
  await write(s=>{const old=s.attempts.find(a=>a.id===row.id);if(!old)s.attempts.push(row);else if(JSON.stringify(old)!==JSON.stringify(row))throw new Error(t("w022"));});
  if(question.pending?.id===row.id)question.pending=null;
 }
 if(pendingSessions.length){const batch=structuredClone(pendingSessions);await write(s=>{for(const row of batch)if(!s.sessions.some(a=>a.id===row.id))s.sessions.push(row);});pendingSessions=pendingSessions.filter(s=>!batch.some(a=>a.id===s.id));}
 progressError='';status();
 }catch(error){progressError='w023';status();throw error;}
}
function scheduleDraft(){
 draftDirty=true;clearTimeout(draftTimer);
 status();draftTimer=setTimeout(()=>persistDraft().catch(()=>{}),200);
}
async function persistDraft(){
 clearTimeout(draftTimer);if(!draftDirty)return;
 const fingerprint=JSON.stringify(draft),id=editingId,payload=hasDraft()?draftEnvelope():null;
 await write(s=>{s.draft=payload;});
 if(fingerprint===JSON.stringify(draft)&&id===editingId){draftDirty=false;}status();
}
async function setSettings(values){await write(s=>{s.settings={...(s.settings||Core.defaultSettings()),...values};});}
function updateClock(){if($('session-clock'))$('session-clock').textContent=clock(sessionSeconds);if($('timer-state'))$('timer-state').textContent=answerTimer.running?t("w024"):t("w025");}
function stopAuto(){if(autoTimer)clearInterval(autoTimer);autoTimer=null;}
function queueTime(){const seconds=Math.floor(unflushed+1e-9);if(seconds<=0)return;pendingSessions.push({id:crypto.randomUUID(),date:sessionDay,seconds});unflushed=Math.max(0,unflushed-seconds);savePending().catch(()=>{});}
function consumeTime(seconds){
 const day=Core.localDay();if(day!==sessionDay){queueTime();sessionDay=day;}
 unflushed+=seconds;sessionSeconds+=seconds;if(unflushed>=10)queueTime();TODAY=day;
}
function tick(){consumeTime(answerTimer.tick());running=answerTimer.running;updateClock();}
function finishTiming(){consumeTime(answerTimer.finish());running=false;queueTime();updateClock();}
function resumeTiming(){if(tab==='notes'&&!document.hidden&&!$('sheet').open&&!question.firstJudged&&!question.revealed){answerTimer.resume();running=answerTimer.running;updateClock();}}
function beginTiming(){answerTimer.start();running=answerTimer.running;sessionDay=Core.localDay();updateClock();}
function pause(flush=true){consumeTime(answerTimer.pause());running=false;stopAuto();stopSound();if(flush)queueTime();updateClock();}
function navigate(next){pause();tab=next;render();resumeTiming();}
function toast(message){clearTimeout(toastTimer);if(tab==='notes'){noteNotice=message;refreshSoundUi();return;}const el=$('toast');el.textContent=message;el.hidden=false;toastTimer=setTimeout(()=>{el.hidden=true;},4000);}
function openSheet(title,body,actions=''){pause();$('sheet-content').innerHTML=`<div class="dialog-heading"><h2 id="dialog-title">${esc(title)}</h2><button data-action="close-sheet" aria-label="${esc(t("w026"))}">✕</button></div>${body}${actions?`<div class="dialog-actions">${actions}</div>`:''}`;if(!$('sheet').open)$('sheet').showModal();}
function windowStats(n,key,source=data.attempts,mode=statsMode){
 const [clef,count,range]=key.split('|');const r=Core.windowStats(state.attempts,{mode,clef,count:Number(count),range},n);
 const eligible=source.filter(a=>a.config===key&&a.mode===mode&&!a.assisted).sort((a,b)=>a.at-b.at||a.id.localeCompare(b.id));
 const map=a=>({...a,day:a.date,at:a.answeredAt});return {...r,all:eligible,rate:r.rate===null?null:Math.round(r.rate),previousRate:r.previousRate===null?null:Math.round(r.previousRate),recent:r.recent.map(map).reverse(),previous:r.previous.map(map).reverse(),correct:r.recent.filter(a=>a.correct).length};
}
function makeQuestion(startClock=false){finishTiming();stopSound();stopAuto();question={...Core.nextQuestion(data.quizConfig,question),answers:[],firstJudged:false,selected:null,mode:data.quizMode,heard:false,playCount:0,assisted:false,pending:null,revealed:false,trainingMode:data.quizMode==='listen'?'ANSWER':data.trainingMode};if(startClock)beginTiming();}
function render(){
 if(!ready)return;project();
 const dock=$('note-dock-scroll')?.scrollTop||0,study=$('study-area')?.scrollTop||0;
 $('screen').classList.toggle('note-screen',tab==='notes');
 $('screen-title').textContent=({record:editingId?t("w027"):t("w028"),notes:data.quizMode==='listen'?t("w029"):t("w030"),review:t("w031"),settings:t("w032"),history:t("w033")})[tab];
 $('brand-icon').innerHTML=icon('piano');$('header-tools').innerHTML=`<a class="small" href="../">${esc(t("w034"))}</a>`;
 $('navigation').innerHTML=[['record',t("w035")],['notes',t("w036")],['review',t("w031")],['settings',t("w032")]].map(([id,label])=>`<button data-nav="${id}" ${(tab===id||tab==='history'&&id==='review')?'aria-current="page"':''}>${icon(id)}<span>${esc(label)}</span></button>`).join('');
 $('screen').innerHTML=tab==='review'?renderReview():tab==='notes'?renderNotes():tab==='record'?renderRecord():tab==='history'?renderHistory():renderSettings();
 if($('note-dock-scroll'))$('note-dock-scroll').scrollTop=dock;if($('study-area'))$('study-area').scrollTop=study;
 updateClock();refreshSoundUi();status();
}

const icon=name=>`<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${({record:'<rect x="4" y="3" width="16" height="18" rx="3"/><path d="M8 8h8M8 12h8M8 16h5"/>',notes:'<path d="M10 17V5l10-2v12M10 9l10-2"/><ellipse cx="7" cy="18" rx="3" ry="2"/><ellipse cx="17" cy="16" rx="3" ry="2"/>',review:'<path d="M4 4v16h16M8 15l4-5 4 2 4-7"/>',settings:'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="3" fill="white"/><circle cx="15" cy="17" r="3" fill="white"/>',export:'<path d="M12 15V3m-4 4 4-4 4 4M4 14v6h16v-6"/>',import:'<path d="M12 3v12m-4-4 4 4 4-4M4 16v4h16v-4"/>',chevron:'<path d="m9 6 6 6-6 6"/>',piano:'<rect x="3" y="3" width="18" height="18" rx="3"/><path d="M9 3v18M15 3v18M7 3v9M13 3v9M19 3v9"/>'})[name]||''}</svg>`;

const speakerIcon='<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 9h4l5-4v14l-5-4H4zM16 8c3 2 3 6 0 8M19 5c5 4 5 10 0 14"/></svg>';
// SOUND_ENGINE_START: also exercised with an OfflineAudioContext in the mock checks.
function noteFrequency(step){
 const semitones=[0,2,4,5,7,9,11],midi=12*(Math.floor(step/7)+1)+semitones[step%7];
 return 440*Math.pow(2,(midi-69)/12);
}
function scheduleTone(context,destination,step,start){
 const nodes=[];
 [1,0.3,0.12,0.05].forEach((weight,index)=>{
  const oscillator=context.createOscillator(),envelope=context.createGain();
  oscillator.type='sine';oscillator.frequency.setValueAtTime(noteFrequency(step)*(index+1),start);
  envelope.gain.setValueAtTime(0,start);
  envelope.gain.linearRampToValueAtTime(weight,start+0.008);
  envelope.gain.exponentialRampToValueAtTime(0.0001,start+0.68/(1+index*0.35));
  envelope.gain.linearRampToValueAtTime(0,start+0.74);
  oscillator.connect(envelope);envelope.connect(destination);
  oscillator.start(start);oscillator.stop(start+0.78);
  oscillator.onended=()=>{oscillator.disconnect();envelope.disconnect();};
  nodes.push(oscillator);
 });
 return nodes;
}
// SOUND_ENGINE_END
const sound={context:null,master:null,nodes:[],timers:[],epoch:0,playing:false,index:-1,status:'',kind:null};
function stopSound(message=''){
 sound.epoch++;sound.timers.forEach(clearTimeout);sound.timers=[];
 sound.nodes.forEach(node=>{try{node.stop();}catch{}});sound.nodes=[];
 sound.playing=false;sound.index=-1;sound.kind=null;sound.status=message;refreshSoundUi();
}
function feedbackText(){
 if(question.pending)return storageError?t(storageError):t("w037");
 if(noteNotice)return noteNotice;
 if(sound.status)return sound.status;
 if(question.firstJudged){
  if(question.assisted)return t("w038");
  return question.answers.length===question.notes.length&&question.answers.every((a,i)=>a===noteName(question.notes[i]))?t("w039"):t("w040");
 }
 if(question.assisted)return t("w038");
 if(question.mode==='listen')return question.heard?t("w041"):t("w042");
 return t("w043");
}
function refreshSoundUi(){
 const play=$('listen-button');if(play){play.disabled=sound.playing;play.textContent=sound.playing?t("w044"):question.playCount?t("w045"):t("w046");}
 const stop=$('stop-sound');if(stop)stop.disabled=!sound.playing;
 const reference=$('reference-sound');if(reference)reference.disabled=sound.playing;
 const message=$('sound-status');if(message)message.textContent=feedbackText();
 if(typeof document.querySelectorAll==='function'){
  document.querySelectorAll('[data-sound-index]').forEach(node=>node.classList.toggle('is-playing',Number(node.dataset.soundIndex)===sound.index&&sound.kind==='question'));
  document.querySelectorAll('[data-note]').forEach(node=>node.disabled=!!question.pending||sound.playing||(question.mode==='listen'&&!question.heard));
 }
 const prompt=$('ear-prompt');if(prompt)prompt.textContent=question.heard?t("w047"):t("w048");
}
async function playSound(kind='question'){
 stopSound();const token=sound.epoch,id=question.id;
 if(data.audioVolume<=0){sound.status=t("w049");refreshSoundUi();return;}
 sound.playing=true;sound.kind=kind;sound.status=t("w050");refreshSoundUi();
 try{
  const Audio=window.AudioContext||window.webkitAudioContext;
  if(!Audio)throw new Error('unsupported');
  if(!sound.context||sound.context.state==='closed'){sound.context=new Audio();sound.master=sound.context.createGain();sound.master.connect(sound.context.destination);sound.context.onstatechange=()=>{if(sound.context.state!=='running'&&sound.playing)stopSound(t("w051"));};}
  if(sound.context.state!=='running')await sound.context.resume();
  if(token!==sound.epoch||id!==question.id)return;
  if(tab!=='notes'||document.hidden){stopSound();return;}
  if(sound.context.state!=='running')throw new Error('suspended');
  sound.master.gain.setValueAtTime(data.audioVolume*0.24,sound.context.currentTime);
  const steps=kind==='reference'?[question.clef==='bass'?21:28]:question.notes;
  const start=sound.context.currentTime+0.02;
  steps.forEach((step,index)=>{
   sound.nodes.push(...scheduleTone(sound.context,sound.master,step,start+index*0.85));
   sound.timers.push(setTimeout(()=>{if(token!==sound.epoch)return;sound.index=index;sound.status=kind==='reference'?t("w052"):t('audioProgress',{current:index+1,total:steps.length});refreshSoundUi();},20+index*850));
  });
  if(kind==='question'){
   question.playCount++;
   if(question.mode==='read'&&!question.firstJudged)question.assisted=true;
  }
  sound.timers.push(setTimeout(()=>{
   if(token!==sound.epoch||id!==question.id)return;
   sound.playing=false;sound.index=-1;sound.kind=null;sound.nodes=[];sound.timers=[];sound.status='';
   if(kind==='question'&&sound.context.state==='running'&&sound.context.currentTime>=start+(steps.length-1)*0.85+0.77)question.heard=true;
   else sound.status=t("w054");
   if(tab==='notes')render();
  },Math.ceil((0.02+(steps.length-1)*0.85+0.8)*1000)));
 }catch(error){
  if(token!==sound.epoch)return;
  stopSound(t("w055"));
 }
}

function renderReview(){return `<div class="segment" aria-label="${esc(t("w056"))}"><button data-review="accuracy" aria-pressed="${reviewView==='accuracy'}">${esc(t("w057"))}</button><button data-review="time" aria-pressed="${reviewView==='time'}">${esc(t("w058"))}</button></div>${reviewView==='accuracy'?renderAccuracy():renderCalendar()}<button class="text-button full" data-action="open-history">${esc(t("w059"))}</button>`;}
function renderAccuracy(){
 const s=windowStats(data.n,statsKey),keys=[...new Set(['treble|1|staff','bass|1|staff',configKey(data.quizConfig),...data.attempts.map(a=>a.config)])];
 const comparison=s.previousRate===null?`<span class="small muted">${esc(t("w060"))}${data.n}${esc(t("w061"))}</span>`:`<strong class="${s.rate>s.previousRate?'good':'muted'}">${s.rate===s.previousRate?t("w062"):`${s.rate>s.previousRate?'↑':'↓'} ${Math.abs(s.rate-s.previousRate)}${esc(t("w063"))}${s.rate>s.previousRate?t("w064"):t("w065")}`}</strong><span class="small muted">${esc(t("w060"))}${data.n}${esc(t("w066"))} ${s.previousRate}%</span>`;
 return `<div class="row between stats-heading"><h3>${esc(t("w067"))}</h3><span class="small muted">${esc(t("w068"))}</span></div><div class="mode-scope" aria-label="${esc(t("w069"))}"><button data-stats-mode="read" aria-pressed="${statsMode==='read'}">${esc(t("w070"))}</button><button data-stats-mode="listen" aria-pressed="${statsMode==='listen'}">${esc(t("w029"))}</button></div><div class="chips" aria-label="${esc(t("w071"))}">${[10,20,50,100].map(n=>`<button data-n="${n}" aria-pressed="${data.n===n}">${formatCount(n,'questions')}</button>`).join('')}<button data-action="custom-n" aria-label="${esc(t("w072"))}">${[10,20,50,100].includes(data.n)?t("w073"):formatCount(data.n,'questions')}</button></div><label class="visually-hidden" for="stats-config">${esc(t("w074"))}</label><select id="stats-config" class="context-select">${keys.map(key=>`<option value="${key}" ${key===statsKey?'selected':''}>${esc(configName(key))}</option>`).join('')}</select><section class="card accuracy-card" aria-label="${esc(t("w075"))}"><div class="row between"><span class="small muted">${esc(t("w077",{count:s.recent.length||data.n}))}</span><span class="count-pill">${s.correct} / ${s.recent.length}${esc(t("w078"))}</span></div><div class="accuracy-number ${s.rate===null?'no-data':''}" id="accuracy-rate">${s.rate===null?t("w079"):s.rate+'<span>%</span>'}</div><div class="delta">${comparison}</div>${s.rate!==null?trendChart(s.all):`<p class="small muted" style="margin-top:18px">${esc(t("w080"))}</p>`}${s.recent.length<data.n?`<p class="small muted" style="margin-top:10px">${s.recent.length} / ${data.n}${esc(t("w081"))}</p>`:''}</section><p class="rate-help">${esc(t("w082"))}</p><section class="card" style="margin-top:14px"><div class="row between"><h3>${esc(t("w083"))}</h3><span class="small muted">${esc(t("w084"))}</span></div>${s.recent.length?`<div class="dots" aria-label="${esc(t("w085"))}${s.correct}${esc(t("w086"))}${s.recent.length-s.correct}${esc(t("w087"))}">${s.recent.slice(-100).map(a=>`<span class="dot ${a.correct?'':'wrong'}" title="${esc(dayLabel(a.day))} ${a.correct?t("w088"):t("w089")}"></span>`).join('')}</div><div class="legend-small"><span><i class="dot"></i>${esc(t("w088"))}</span><span><i class="dot wrong"></i>${esc(t("w089"))}</span>${data.n>100?`<span>${esc(t("w090"))}</span>`:''}</div>`:`<p class="small muted" style="margin-top:8px">${esc(t("w091"))}</p>`}</section><button class="secondary full" data-nav="notes" style="margin-top:14px">${esc(t("w092"))}</button>`;
}
function trendChart(all){
 if(all.length<data.n)return '';
 const start=Math.max(data.n,all.length-60),ends=[...new Set(Array.from({length:7},(_,i)=>Math.round(start+(all.length-start)*i/6)))];
 const points=ends.map((end,i)=>{const block=all.slice(end-data.n,end),rate=block.filter(a=>a.correct).length/block.length*100;return {x:26+(ends.length===1?1:i/(ends.length-1))*274,y:90-rate*.72,rate};});
 const line=points.map(p=>`${p.x},${p.y}`).join(' '),last=points.at(-1);
 return `<svg class="trend" viewBox="0 0 320 108" role="img" aria-label="${esc(t("w076"))}${data.n}${esc(t("w093"))}${Math.round(last.rate)}${esc(t("w094"))}"><path d="M26 18H307M26 54H307M26 90H307" fill="none" stroke="var(--line)" stroke-dasharray="3 4"/><text x="0" y="21" fill="var(--sub)" font-size="9">100</text><text x="7" y="57" fill="var(--sub)" font-size="9">50</text><text x="12" y="93" fill="var(--sub)" font-size="9">0</text><polygon points="${points[0].x},90 ${line} ${last.x},90" fill="var(--chart-fill)"/><polyline points="${line}" fill="none" stroke="var(--blue)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="${last.x}" cy="${last.y}" r="4" fill="var(--blue)" stroke="white" stroke-width="2"/></svg><div class="chart-caption"><span>${formatDay(all[ends[0]-1].day,{month:'short',day:'numeric'})}</span><span>${formatDay(all.at(-1).day,{month:'short',day:'numeric'})}</span></div>`;
}
function daySeconds(day,filter=timeFilter){const piano=data.records.filter(r=>r.day===day).reduce((sum,r)=>sum+r.minutes*60,0),notes=data.noteTime[day]||0;return filter==='piano'?piano:filter==='notes'?notes:piano+notes;}
function heat(seconds){return seconds<=0?0:seconds<15*60?1:seconds<30*60?2:seconds<60*60?3:4;}
function renderCalendar(){
 const [year,m]=month.split('-').map(Number),offset=new Date(year,m-1,1).getDay(),days=new Date(year,m,0).getDate();let cells='<span></span>'.repeat(offset),total=0,count=0;
 for(let day=1;day<=days;day++){const key=`${month}-${String(day).padStart(2,'0')}`,seconds=daySeconds(key),future=key>TODAY;total+=seconds;if(seconds>0)count++;cells+=`<button class="day l${heat(seconds)} ${key===TODAY?'today':''}" data-day="${key}" aria-pressed="${key===selectedDay}" aria-label="${dayLabel(key)} · ${seconds>0?duration(seconds):t("w096")}" ${future?'disabled':''}><span>${day}</span><small>${seconds>=60?Math.floor(seconds/60)+t("w005"):seconds>0?t("w097"):'−'}</small></button>`;}
 return `<div class="chips" aria-label="${esc(t("w098"))}">${[['all',t("w099")],['piano',t("w100")],['notes',t("w036")]].map(([key,label])=>`<button data-filter="${key}" aria-pressed="${timeFilter===key}">${esc(label)}</button>`).join('')}</div><div class="summary-pair"><div><small>${esc(t("w101"))}</small><strong>${duration(total)}</strong></div><div><small>${esc(t("w102"))}</small><strong>${formatCount(count,'days')}</strong></div></div><section class="card" style="padding:13px"><div class="row between calendar-title"><button data-month="-1" aria-label="${esc(t("w104"))}">‹</button><strong>${formatMonth(month)}</strong><button data-month="1" aria-label="${esc(t("w106"))}" ${month>=TODAY.slice(0,7)?'disabled':''}>›</button></div><div class="weekdays" aria-hidden="true">${weekdays().map(d=>`<span>${d}</span>`).join('')}</div><div class="calendar-grid">${cells}</div><div class="heat-legend"><i class="l0"></i>${esc(t("w112"))} <i class="l1"></i>${esc(t("w113"))} <i class="l2"></i>15〜29 <i class="l3"></i>30〜59 <i class="l4"></i>${esc(t("w114"))}</div><p class="small muted">${esc(t("w115"))}</p></section><div class="daily-summary"><div class="row between"><strong>${dayLabel(selectedDay)}</strong><strong>${duration(daySeconds(selectedDay))}</strong></div><p class="small muted">${esc(t("w100"))} ${duration(daySeconds(selectedDay,'piano'))} ${esc(t("w116"))} ${duration(daySeconds(selectedDay,'notes'))}</p><button data-day="${selectedDay}">${esc(t("w117"))}</button></div><p class="rate-help">${esc(t("w118"))}</p>`;
}



function renderNotes(){
 const s=windowStats(data.n,configKey(question.config),data.attempts,question.mode),hidden=question.mode==='listen'&&!question.firstJudged;
 const notation=hidden?`<div class="ear-placeholder" role="img" aria-label="${esc(t("w119"))}${question.notes.length}${esc(t("w120"))}">${speakerIcon}<div class="sound-dots" aria-hidden="true">${question.notes.map((_,i)=>`<i data-sound-index="${i}"></i>`).join('')}</div><p id="ear-prompt">${question.heard?t("w047"):t("w048")}</p></div>`:scoreSvg(question);
 const slots=question.notes.map((step,i)=>{const answer=question.answers[i],graded=question.firstJudged,ok=answer===noteName(step);return `<button class="answer-slot ${graded?(ok?'ok':'wrong'):''} ${question.selected===i?'selected':''}" data-slot="${i}" aria-label="${i+1}${esc(t("w121"))} ${esc(answer?displayNote(answer):t("w122"))}${graded&&!ok?t("w123")+displayNote(noteName(step)):''}">${graded?(ok?'○ ':'× '):''}${(answer?displayNote(answer):'')||`${i+1}${esc(t("w121"))}`}${graded&&!ok?`<small>${esc(t("w088"))} ${displayNote(noteName(step))}</small>`:''}</button>`;}).join('');
 const keys=['ド','レ','ミ','ファ','ソ','ラ','シ','⌫'].map(n=>`<button data-note="${n}" ${n==='⌫'?`aria-label="${esc(t("w124"))}"`:''} ${sound.playing||(question.mode==='listen'&&!question.heard)?'disabled':''}>${displayNote(n)}</button>`).join('');
 return `<div class="note-view audio-note-view">
  <div class="study-area" id="study-area">
   <div class="segment note-mode" aria-label="${esc(t("w125"))}"><button data-quiz-mode="read" aria-pressed="${data.quizMode==='read'}">${esc(t("w070"))}</button><button data-quiz-mode="listen" aria-pressed="${data.quizMode==='listen'}">${esc(t("w029"))}</button></div>
   <div class="note-summary"><button class="recent-mini" data-action="view-current-rate"><span>${esc(t("w076"))}${s.recent.length||data.n}${esc(t("w066"))}<strong id="mini-rate">${s.rate===null?'—':s.rate+'%'}</strong></span><span>›</span></button><div class="timer-compact"><span class="mono" id="session-clock">${clock(sessionSeconds)}</span><span class="small muted" id="timer-state">${answerTimer.running?t("w024"):t("w025")}</span></div></div>
   <div class="score audio-score"><div class="score-label">${esc(configName(configKey(question.config)))}${pendingConfig?t("w126"):''}</div><div class="notation-visual">${notation}</div></div>
  </div>
  <div class="answer-dock" aria-label="${esc(t("w127"))}">
   <div class="dock-content" id="note-dock-scroll">
    ${question.trainingMode==='ANSWER'?`<div class="answer-slots">${slots}</div>`:`<div class="training-feedback">${question.revealed?question.notes.map(step=>displayNote(noteName(step))).join(' · '):t("w128")}</div><button class="secondary full" data-action="${question.trainingMode==='AUTO'?'auto':'reveal'}">${question.trainingMode==='AUTO'?(autoTimer?t("w129"):t("w130")):(question.revealed?t("w131"):t("w132"))}</button>`}
    <div class="note-feedback" id="sound-status" aria-live="polite">${esc(feedbackText())}</div>
    <div class="play-controls"><button class="${question.mode==='listen'&&!question.heard?'primary':'outline'}" data-action="play-sound" id="listen-button">${esc(t("w046"))}</button>${question.mode==='listen'?`<button class="outline reference" data-action="reference-sound" id="reference-sound">${esc(t("w133"))}</button>`:''}<button class="outline stop-sound" data-action="stop-sound" id="stop-sound" aria-label="${esc(t("w134"))}" disabled>■</button></div>
    ${question.trainingMode==='ANSWER'?`<div class="note-keys">${keys}</div>`:''}
   </div>
   <div class="dock-footer"><button class="dock-next ${question.firstJudged?'primary':'secondary'}" data-action="next-question">${question.mode==='listen'?t("w135"):t("w136")}</button></div>
  </div>
 </div>`;
}

function renderRecordBase(){const d=data.draft;return `<form id="record-form" class="record-body"><div class="row between"><span class="badge">${esc(t("w137"))}</span><label class="visually-hidden" for="record-day">${esc(t("w138"))}</label><input id="record-day" name="day" type="date" value="${d.day}" required style="width:158px;padding:7px"></div><div><label class="field-label" for="piece">${esc(t("w139"))}</label><input class="full" id="piece" name="piece" maxlength="80" placeholder="${esc(t("w140"))}" value="${esc(d.piece)}" required></div><button type="button" class="text-button" data-action="recent-record" style="justify-self:start;padding:2px 0;min-height:28px">${esc(t("w141"))}</button><div><div class="time-heading"><label class="field-label" for="minutes">${esc(t("w142"))}</label><button type="button" class="text-button" data-action="reset-minutes" aria-label="${esc(t("w143"))}">${esc(t("w144"))}</button></div><div class="record-time"><input id="minutes" name="minutes" type="number" min="1" max="1440" step="1" value="${esc(d.minutes)}" placeholder="0" required>${[5,15,30].map(n=>`<button type="button" data-add="${n}">＋${n}</button>`).join('')}</div></div><p class="small muted">${esc(t("w145"))}</p>${[['difficult',t("w146")],['unpracticed',t("w147")],['finishing',t("w148")]].map(([key,title])=>`<details class="reflection"><summary>${esc(title)} <small class="muted">${esc(t("w149"))}</small></summary><label class="visually-hidden" for="${key}">${esc(title)}</label><textarea id="${key}" name="${key}" maxlength="2000" placeholder="${esc(t("w150"))}">${esc(d[key])}</textarea></details>`).join('')}<div class="record-actions"><p class="form-error" id="record-error" role="alert"></p><button class="primary full" type="submit">${esc(t("w151"))}</button></div></form>`;}

function showQuizSettings(){
 const c=pendingConfig||data.quizConfig;
 openSheet(t("w152"),`<form id="quiz-form" class="stack"><p class="small muted">${esc(t("w153"))}</p><label class="field-label">${esc(t("w154"))}<select name="clef" class="full"><option value="treble" ${c.clef==='treble'?'selected':''}>${esc(t("w007"))}</option><option value="bass" ${c.clef==='bass'?'selected':''}>${esc(t("w008"))}</option><option value="both" ${c.clef==='both'?'selected':''}>${esc(t("w155"))}</option></select></label><label class="field-label">${esc(t("w156"))}<select name="count" class="full">${[1,2,3,0].map(n=>`<option value="${n}" ${c.count===n?'selected':''}>${n===0?t("w157"):formatCount(n,'notes')}</option>`).join('')}</select></label><label class="field-label">${esc(t("w159"))}<select name="range" class="full">${[['staff',t("w012")],['basic',t("w160")],['wide',t("w161")]].map(([key,label])=>`<option value="${key}" ${c.range===key?'selected':''}>${esc(label)}</option>`).join('')}</select></label><label class="field-label">${esc(t("w162"))}<select class="full" name="trainingMode" ${data.quizMode==='listen'?'disabled':''}>${[['ANSWER',t("w163")],['FLASHCARD',t("w164")],['AUTO',t("w165")]].map(([key,label])=>`<option value="${key}" ${data.trainingMode===key?'selected':''}>${esc(label)}</option>`).join('')}</select></label><label class="field-label">${esc(t("w166"))}<select class="full" name="interval">${[3,5,8].map(n=>`<option value="${n}" ${data.interval===n?'selected':''}>${n}${esc(t("w006"))}</option>`).join('')}</select></label><div><label class="field-label" for="sound-volume">${esc(t("w167"))}</label><div class="volume-row"><input id="sound-volume" type="range" min="0" max="100" step="5" value="${Math.round(data.audioVolume*100)}"><span id="volume-output">${Math.round(data.audioVolume*100)}%</span></div></div><p class="small muted">${esc(t("w168"))}</p><button class="primary full" type="submit">${esc(t("w169"))}</button></form>`);
}


function renderRecord(){return renderRecordBase().replace(`<button class="primary full" type="submit">${esc(t("w151"))}</button>`,`<p class="saving-indicator" id="save-status"></p><button class="primary full" type="submit">${editingId?t("w170"):t("w151")}</button><div class="button-row"><button type="button" class="text-button" data-action="open-history">${esc(t("w171"))}</button>${hasDraft()?`<button type="button" class="text-button" data-action="clear-draft">${esc(t("w172"))}</button>`:''}</div>`);}
function renderHistory(){
 const q=historyQuery.trim().toLowerCase();const rows=[...data.records].filter(r=>[r.day,r.piece,r.difficult,r.unpracticed,r.finishing,r.memo,r.range,r.next].some(v=>String(v).toLowerCase().includes(q))).sort((a,b)=>b.day.localeCompare(a.day)||b.created_at.localeCompare(a.created_at));
 return `<label class="field-label" for="history-search">${esc(t("w173"))}</label><input id="history-search" class="history-search" value="${esc(historyQuery)}" placeholder="${esc(t("w174"))}"><p class="small muted">${formatCount(rows.length,'records')}</p><div id="history-results">${rows.length?rows.map(r=>`<button class="history-item" data-record="${r.id}"><strong>${esc(r.piece)} · ${r.minutes}${esc(t("w005"))}</strong><small>${esc(dayLabel(r.day))} · ${esc((r.difficult||r.finishing||r.memo||t("w176")).slice(0,60))}</small></button>`).join(''):`<p class="empty">${esc(t("w177"))}</p>`}</div>`;
}
function renderSettings(){return `<div class="stack"><section><h3>${esc(t("w178"))}</h3><div class="card settings-card"><button class="settings-row" data-action="quiz-settings"><span class="settings-icon">${icon('notes')}</span><span class="grow"><strong>${esc(t("w179"))}</strong><small>${esc(configName(configKey(data.quizConfig)))}</small></span>${icon('chevron')}</button></div><p class="rate-help">${esc(t("w180"))}</p></section><section class="card"><h3>${esc(t("w181"))}</h3><p class="small muted">${esc(t("w182"))}</p></section><section class="card"><h3>${esc(t("w183"))}</h3><p class="small muted">${esc(t("w184"))}</p><button class="text-button" data-action="reset">${esc(t("w185"))}</button><a class="link-button primary" href="https://github.com/Eirene-Bel/PianoNote/releases/latest">${esc(t("w186"))}</a></section><p class="small"><a href="../privacy.html">${esc(t("w187"))}</a> · <a href="../licenses.html">${esc(t("w188"))}</a> · <a href="../">${esc(t("w034"))}</a></p><p class="footer-info">${esc(t("w189"))}</p></div>`;}
function recordDetails(id){
 const row=data.records.find(r=>r.id===id);if(!row)return;
 const fields=[[t("w146"),row.difficult],[t("w147"),row.unpracticed],[t("w148"),row.finishing],[t("w190"),row.range],[t("w191"),row.memo],[t("w192"),row.next]].filter(([,text])=>text);
 openSheet(row.piece,`<p>${esc(dayLabel(row.day))} · ${row.minutes}${esc(t("w005"))}</p>${fields.map(([title,text])=>`<h3 style="margin-top:14px">${esc(title)}</h3><p class="record-detail">${esc(text)}</p>`).join('')}`,`<button class="primary" data-edit="${id}">${esc(t("w193"))}</button><button class="outline" data-delete="${id}">${esc(t("w194"))}</button>`);
}
function showDay(day){
 selectedDay=day;const rows=data.records.filter(r=>r.day===day),attempts=data.attempts.filter(a=>a.day===day);
 openSheet(dayLabel(day),`<div class="summary-pair"><div><small>${esc(t("w100"))}</small><strong>${duration(daySeconds(day,'piano'))}</strong></div><div><small>${esc(t("w036"))}</small><strong>${duration(daySeconds(day,'notes'))}</strong></div></div>${rows.map(r=>`<button class="history-item" data-record="${r.id}"><strong>${esc(r.piece)} · ${r.minutes}${esc(t("w005"))}</strong><small>${esc(r.difficult||r.finishing||t("w176"))}</small></button>`).join('')}${attempts.length?`<p>${attempts.length}${esc(t("w195"))}${attempts.filter(a=>a.assisted).length}${esc(t("w066"))}</p>`:''}${!rows.length&&!attempts.length?`<p class="empty">${esc(t("w196"))}</p>`:''}`,`<button class="secondary" data-record-day="${day}">${esc(t("w197"))}</button>`);
}
function validDay(day){try{const r={id:crypto.randomUUID(),date:day,piece:t("w198"),minutes:1,revision:1,created_at:new Date().toISOString()};Core.validateRecord(r);return true;}catch{return false;}}
async function applyQuizMode(mode){
 if(question.pending){toast(t("w199"));return;}
 pause();await setSettings({mode,trainingMode:'ANSWER'});statsMode=mode;pendingMode=null;pendingConfig=null;statsKey=configKey(data.quizConfig);makeQuestion();render();
}
function changeQuizMode(mode){if(mode===data.quizMode)return;if(question.answers.length&&!question.firstJudged){pendingMode=mode;openSheet(t("w200"),`<p>${esc(t("w201"))}</p>`,`<button class="outline" data-action="close-sheet">${esc(t("w202"))}</button><button class="primary" data-action="confirm-mode">${esc(t("w203"))}</button>`);}else applyQuizMode(mode).catch(()=>{});}
async function answerNote(name){
 if(sound.playing||question.pending||(question.mode==='listen'&&!question.heard))return;stopAuto();sound.status='';tick();
 if(name==='⌫'){if(question.answers.length)question.answers.splice(question.selected??question.answers.length-1,1);question.selected=null;}
 else if(question.selected!==null&&question.selected<question.answers.length){question.answers[question.selected]=name;question.selected=null;}
 else if(question.answers.length<question.notes.length)question.answers.push(name);
 if(question.answers.length===question.notes.length&&!question.firstJudged){
  finishTiming();question.firstJudged=true;const cfg=question.config;
  question.pending={id:question.id,date:Core.localDay(),answeredAt:Date.now(),mode:question.mode,clefOption:cfg.clef,countOption:cfg.count,range:cfg.range,actualClef:question.clef,notes:question.notes.join(','),correct:question.answers.every((a,i)=>a===noteName(question.notes[i])),assisted:question.assisted};
  render();await savePending();
 }
 render();
}
async function resetDemo(empty=false){pause();await savePending();await queue;store.close();store=memoryStore(createSample(empty));const loaded=await store.read();state=loaded.state;revision=loaded.revision;unflushed=0;pendingSessions=[];applyDraft(null);sessionSeconds=0;project();makeQuestion();render();}
function createSample(empty=false){
 const result=Core.emptyState();result.settings=Core.defaultSettings();if(empty)return result;
 const today=new Date();const dayOf=ago=>Core.localDay(new Date(today.getFullYear(),today.getMonth(),today.getDate()-ago));
 const times=[45,45,60,35,0,50,15,30,45,35,0,30,60,25,0,10,45,30,15,0,20];
 for(let i=0;i<times.length;i++)if(times[i])result.records.push({id:crypto.randomUUID(),date:dayOf(i),piece:i%3===0?'ハノン':'エリーゼのために',minutes:times[i],range:'',memo:'',next:'',revision:1,created_at:new Date(dayOf(i)+'T08:00:00Z').toISOString(),difficult_parts:i===0?'左手のつなぎをゆっくり。昨日より滑らかに。':'',unpracticed_parts:'',finishing_image:'やわらかく、歌うように'});
 for(let i=0;i<120;i++){const day=dayOf(9-Math.floor(i/12));let correct=(i*37)%100<48+Math.floor(i/3);if(i>=80&&i<100)correct=i-80>=7;if(i>=100)correct=![0,8,12].includes(i-100);result.attempts.push({id:crypto.randomUUID(),date:day,answeredAt:Date.parse(day+'T08:00:00')+(i%12)*18000,mode:'read',clefOption:'treble',countOption:1,range:'staff',actualClef:'treble',notes:'32',correct,assisted:false});}
 for(let i=0;i<10;i++)result.sessions.push({id:crypto.randomUUID(),date:dayOf(i),seconds:216});return result;
}

async function beginEdit(id){
 const row=state.records.find(r=>r.id===id);if(!row)return;
 if(hasDraft()&&editingId!==id){openSheet(t("w204"),`<p>${esc(t("w205"))}</p>`,`<button class="outline" data-action="close-sheet">${esc(t("w206"))}</button><button class="primary" data-confirm-edit="${id}">${esc(t("w193"))}</button>`);return;}
 await loadEdit(row);
}
async function loadEdit(row){await persistDraft();draft={day:row.date,piece:row.piece,minutes:String(row.minutes),difficult:row.difficult_parts,unpracticed:row.unpracticed_parts,finishing:row.finishing_image};editingId=row.id;editingRevision=row.revision;draftDirty=true;await persistDraft();$('sheet').close();navigate('record');}
async function handleClick(event){
 const button=event.target.closest('button');if(!button||button.disabled||!ready||recordSaving)return;noteNotice='';
 if(button.dataset.quizMode){changeQuizMode(button.dataset.quizMode);return;}
 if(button.dataset.statsMode){statsMode=button.dataset.statsMode;render();return;}
 if(button.dataset.nav){await persistDraft();navigate(button.dataset.nav);return;}
 if(button.dataset.review){reviewView=button.dataset.review;render();return;}
 if(button.dataset.n){await setSettings({recentN:Number(button.dataset.n)});render();return;}
 if(button.dataset.filter){timeFilter=button.dataset.filter;render();return;}
 if(button.dataset.month){const [y,m]=month.split('-').map(Number),date=new Date(y,m-1+Number(button.dataset.month),1);month=Core.localDay(date).slice(0,7);render();return;}
 if(button.dataset.day){showDay(button.dataset.day);return;}
 if(button.dataset.recordDay){draft.day=button.dataset.recordDay;scheduleDraft();$('sheet').close();navigate('record');return;}
 if(button.dataset.slot!==undefined){question.selected=Number(button.dataset.slot);render();return;}
 if(button.dataset.note){await answerNote(button.dataset.note);return;}
 if(button.dataset.record){recordDetails(button.dataset.record);return;}
 if(button.dataset.edit){await beginEdit(button.dataset.edit);return;}
 if(button.dataset.confirmEdit){await loadEdit(state.records.find(r=>r.id===button.dataset.confirmEdit));return;}
 if(button.dataset.delete){pendingDelete=button.dataset.delete;openSheet(t("w207"),`<p>${esc(t("w208"))}</p>`,`<button class="outline" data-action="close-sheet">${esc(t("w206"))}</button><button class="primary" data-action="confirm-delete">${esc(t("w194"))}</button>`);return;}
 if(button.dataset.add){const current=Number(draft.minutes||0),amount=Number(button.dataset.add);if(!Number.isInteger(current)||current<0||current+amount>1440){$('record-error').textContent=t("w209");return;}draft.minutes=String(current+amount);$('minutes').value=draft.minutes;scheduleDraft();return;}
 switch(button.dataset.action){
 case 'reset':await resetDemo($('scenario')?.value==='new');toast(t("w210"));break;
 case 'close-sheet':$('sheet').close();break;
 case 'reset-minutes':draft.minutes='';$('minutes').value='';$('record-error').textContent='';scheduleDraft();break;
 case 'open-history':await persistDraft();historyQuery='';navigate('history');break;
 case 'clear-draft':openSheet(t("w211"),`<p>${esc(t("w212"))}</p>`,`<button class="outline" data-action="close-sheet">${esc(t("w206"))}</button><button class="primary" data-action="confirm-clear-draft">${esc(t("w172"))}</button>`);break;
 case 'confirm-clear-draft':clearTimeout(draftTimer);await queue;await write(s=>{s.draft=null;});applyDraft(null);$('sheet').close();render();break;
 case 'custom-n':openSheet(t("w213"),`<form id="window-form" class="stack"><label class="field-label" for="custom-n">${esc(t("w214"))}</label><input id="custom-n" name="n" type="number" min="1" max="1000" step="1" value="${data.n}" required><button class="primary full">${esc(t("w215"))}</button></form>`);break;
 case 'view-current-rate':statsMode=question.mode;statsKey=configKey(question.config);reviewView='accuracy';navigate('review');break;
 case 'play-sound':stopAuto();await playSound();break;
 case 'reference-sound':await playSound('reference');break;
 case 'stop-sound':stopSound(t("w216"));break;
 case 'confirm-mode':{const mode=pendingMode;$('sheet').close();if(mode)await applyQuizMode(mode);break;}
 case 'next-question':if(question.pending){await savePending();}pendingConfig=null;makeQuestion(true);render();if(question.mode==='listen')await playSound();break;
 case 'quiz-settings':showQuizSettings();break;
 case 'recent-record':{const r=[...data.records].sort((a,b)=>b.day.localeCompare(a.day)||b.created_at.localeCompare(a.created_at))[0];if(!r){toast(t("w217"));break;}if(hasDraft()){openSheet(t("w204"),`<p>${esc(t("w218"))}</p>`,`<button class="outline" data-action="close-sheet">${esc(t("w206"))}</button><button class="primary" data-action="confirm-copy">${esc(t("w219"))}</button>`);break;}await copyRecent(r);break;}
 case 'confirm-copy':{const r=[...data.records].sort((a,b)=>b.day.localeCompare(a.day)||b.created_at.localeCompare(a.created_at))[0];if(r)await copyRecent(r);$('sheet').close();break;}
 case 'confirm-delete':{const id=pendingDelete;await write(s=>{s.records=s.records.filter(r=>r.id!==id);if(!s.deletedIds.includes(id))s.deletedIds.push(id);if(editingId===id)s.draft=null;});if(editingId===id)applyDraft(null);pendingDelete=null;$('sheet').close();render();toast(t("w220"));break;}
 case 'retry-save':if(stale){openSheet(t("w221"),`<p>${esc(t("w222"))}</p>`,`<button class="outline" data-action="close-sheet">${esc(t("w206"))}</button><button class="primary" data-action="reload-state">${esc(t("w223"))}</button>`);}else{await savePending();await persistDraft();render();}break;
 case 'reload-state':{const latest=await store.read();state=latest.state;revision=latest.revision;stale=false;storageError='';$('sheet').close();project();render();break;}
 case 'reveal':question.revealed=!question.revealed;if(question.revealed)finishTiming();render();break;
 case 'auto':if(autoTimer){pause();render();}else{if(question.revealed||question.firstJudged)makeQuestion(true);else beginTiming();autoTimer=setInterval(()=>{if(document.hidden||tab!=='notes'){pause();return;}if(question.revealed){const timer=autoTimer;autoTimer=null;makeQuestion(true);autoTimer=timer;}else{question.revealed=true;finishTiming();}render();},data.interval*1000);render();}break;
 }
}
async function copyRecent(r){draft={...emptyDraft(),day:draft.day,piece:r.piece,minutes:String(r.minutes),finishing:r.finishing};editingId=null;editingRevision=null;draftDirty=true;await persistDraft();render();toast(t("w224"));}
document.addEventListener('click',event=>{handleClick(event).catch(error=>{storageError=storageError||'w225';status();});});
document.addEventListener('input',event=>{
 if(event.target.id==='sound-volume'){const value=Number(event.target.value)/100;$('volume-output').textContent=event.target.value+'%';return;}
 if(event.target.closest('#record-form')&&event.target.name){draft[event.target.name]=event.target.value;scheduleDraft();}
 if(event.target.id==='history-search'){historyQuery=event.target.value;const shell=document.createElement('div');shell.innerHTML=renderHistory();$('history-results').innerHTML=shell.querySelector('#history-results').innerHTML;}
});
document.addEventListener('change',event=>{if(event.target.id==='stats-config'){statsKey=event.target.value;render();}if(event.target.id==='scenario')resetDemo(event.target.value==='new').catch(()=>{});});
document.addEventListener('submit',event=>{event.preventDefault();submitForm(event.target).catch(error=>{if($('record-error'))$('record-error').textContent=t("w226");else toast(t("w227"));});});
async function submitForm(form){
 if(!form.reportValidity())return;
 if(form.id==='window-form'){await setSettings({recentN:Number(new FormData(form).get('n'))});$('sheet').close();render();}
 if(form.id==='record-form'){
  const button=form.querySelector('[type="submit"]'),controls=[...form.querySelectorAll('input,textarea,button')];controls.forEach(e=>e.disabled=true);recordSaving=true;clearTimeout(draftTimer);
  const editTarget=editingId,expectedRevision=editingRevision;
  const old=state.records.find(r=>r.id===editingId),id=editingId||crypto.randomUUID();
  const row={...(old||{id,range:'',memo:'',next:'',created_at:new Date().toISOString()}),date:draft.day,piece:draft.piece.trim(),minutes:Number(draft.minutes),revision:old?old.revision+1:1,difficult_parts:draft.difficult.trim(),unpracticed_parts:draft.unpracticed.trim(),finishing_image:draft.finishing.trim()};
  try{Core.validateRecord(row);await write(s=>{const existing=s.records.find(r=>r.id===id);if(editTarget&&(!existing||existing.revision!==expectedRevision))throw new Error(t("w228"));if(s.deletedIds.includes(id))throw new Error(t("w229"));s.records=s.records.filter(r=>r.id!==id);s.records.push(row);s.draft=null;});selectedDay=row.date;month=row.date.slice(0,7);applyDraft(null);reviewView='time';navigate('review');toast(t("w230"));}finally{recordSaving=false;controls.forEach(e=>e.disabled=false);}
 }
 if(form.id==='quiz-form'){const values=new FormData(form);pendingConfig={clef:values.get('clef'),count:Number(values.get('count')),range:values.get('range')};await setSettings({...pendingConfig,volume:Number($('sound-volume').value)/100,trainingMode:data.quizMode==='listen'?'ANSWER':values.get('trainingMode'),interval:Number(values.get('interval'))});$('sheet').close();render();toast(t("w231"));}
}
document.addEventListener('pianonote-language',()=>{pause();noteNotice='';sound.status='';if($('sheet').open)$('sheet').close();render();status();resumeTiming();});
document.addEventListener('visibilitychange',()=>{if(document.hidden){pause();persistDraft().catch(()=>{});}else resumeTiming();});
window.addEventListener('pagehide',()=>{pause();persistDraft().catch(()=>{});if(sound.context)sound.context.close().catch(()=>{});});
$('sheet').addEventListener('close',()=>{pendingMode=null;pendingDelete=null;resumeTiming();});
async function start(){
 try{
  store=memoryStore(createSample());
  const loaded=await store.read();state=loaded.state;revision=loaded.revision;applyDraft(state.draft);project();makeQuestion();ready=true;
  render();setInterval(tick,1000);
 }catch{storageError='w232';$('screen').innerHTML=`<p class="empty">${esc(t("w233"))}</p>`;status();}
}
start();
