import {catalogs} from './catalogs.mjs';
export const locales=['ja','en','zh-Hans','zh-Hant','ko','es','fr','de','pt'];
export const systemNames={ja:'自動（ブラウザーの言語）',en:'Automatic (browser language)','zh-Hans':'自动（浏览器语言）','zh-Hant':'自動（瀏覽器語言）',ko:'자동 (브라우저 언어)',es:'Automático (idioma del navegador)',fr:'Automatique (langue du navigateur)',de:'Automatisch (Browsersprache)',pt:'Automático (idioma do navegador)'};
export const nativeNames={'ja':'日本語','en':'English','zh-Hans':'简体中文','zh-Hant':'繁體中文','ko':'한국어','es':'Español','fr':'Français','de':'Deutsch','pt':'Português'};
export function resolveLocale(preferences=[]){
 for(const preference of preferences){
  const value=String(preference).replaceAll('_','-').toLowerCase();
  if(value.startsWith('zh')){if(/(?:^|-)hans(?:-|$)/.test(value))return 'zh-Hans';if(/(?:^|-)hant(?:-|$)/.test(value))return 'zh-Hant';return /(?:^|-)(?:tw|hk|mo)(?:-|$)/.test(value)?'zh-Hant':'zh-Hans';}
  const found=locales.find(l=>l.toLowerCase()===value)||locales.find(l=>l===value.split('-')[0]);
  if(found)return found;
 }
 return 'en';
}
function preferredLocale(){try{const saved=globalThis.localStorage?.getItem('pianonote.language');if(locales.includes(saved))return saved;}catch{}return resolveLocale(globalThis.navigator?.languages||[globalThis.navigator?.language||'ja']);}
let locale=preferredLocale();
export const getLocale=()=>locale;
export function setLocale(value){if(!locales.includes(value))throw new RangeError('Unsupported locale');locale=value;try{globalThis.localStorage?.setItem('pianonote.language',value);}catch{}if(globalThis.document)document.documentElement.lang=locale;}
export function t(key,values={}){const value=catalogs[locale]?.[key];if(typeof value!=='string')throw new Error(`Missing translation: ${locale}/${key}`);return value.replace(/\{(\w+)\}/g,(_,name)=>{if(!(name in values))throw new Error(`Missing message parameter: ${key}/${name}`);return String(values[name]);});}
export const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export function formatDay(day,options={month:'short',day:'numeric',weekday:'short'}){return new Intl.DateTimeFormat(locale,options).format(new Date(day+'T12:00:00'));}
export function formatMonth(month){return formatDay(month+'-01',{year:'numeric',month:'long'});}
export function weekdays(){return Array.from({length:7},(_,i)=>new Intl.DateTimeFormat(locale,{weekday:'short'}).format(new Date(2026,8,20+i)));}
export function formatDuration(seconds){const n=Math.max(0,Math.floor(seconds));const unit=(v,u)=>new Intl.NumberFormat(locale,{style:'unit',unit:u,unitDisplay:'short'}).format(v);return n>=3600?unit(Math.floor(n/3600),'hour')+' '+unit(Math.floor(n%3600/60),'minute'):n>=60?unit(Math.floor(n/60),'minute')+(n%60?' '+unit(n%60,'second'):''):unit(n,n?'second':'minute');}
const countWords={ja:{notes:['音','音'],questions:['問','問'],days:['日','日'],records:['件','件']},en:{notes:[' note',' notes'],questions:[' question',' questions'],days:[' day',' days'],records:[' entry',' entries']},'zh-Hans':{notes:['个音','个音'],questions:['题','题'],days:['天','天'],records:['条','条']},'zh-Hant':{notes:['個音','個音'],questions:['題','題'],days:['天','天'],records:['筆','筆']},ko:{notes:['개 음','개 음'],questions:['문제','문제'],days:['일','일'],records:['개','개']},es:{notes:[' nota',' notas'],questions:[' pregunta',' preguntas'],days:[' día',' días'],records:[' registro',' registros']},fr:{notes:[' note',' notes'],questions:[' question',' questions'],days:[' jour',' jours'],records:[' entrée',' entrées']},de:{notes:[' Note',' Noten'],questions:[' Frage',' Fragen'],days:[' Tag',' Tage'],records:[' Eintrag',' Einträge']},pt:{notes:[' nota',' notas'],questions:[' questão',' questões'],days:[' dia',' dias'],records:[' registro',' registros']}};
export function formatCount(value,kind){const singular=typeof value==='number'&&new Intl.PluralRules(locale).select(value)==='one';return (typeof value==='number'?new Intl.NumberFormat(locale).format(value):value)+countWords[locale][kind][singular?0:1];}
const canonical=['ド','レ','ミ','ファ','ソ','ラ','シ'];
export function displayNote(token){const index=canonical.indexOf(token);if(index<0)return token;return ({ja:canonical,en:['C','D','E','F','G','A','B'],'zh-Hans':['Do','Re','Mi','Fa','Sol','La','Si'],'zh-Hant':['Do','Re','Mi','Fa','Sol','La','Si'],ko:['도','레','미','파','솔','라','시'],es:['Do','Re','Mi','Fa','Sol','La','Si'],fr:['Do','Ré','Mi','Fa','Sol','La','Si'],de:['C','D','E','F','G','A','H'],pt:['Dó','Ré','Mi','Fá','Sol','Lá','Si']})[locale][index];}
export function translatePage(){document.documentElement.lang=locale;document.querySelectorAll('[data-i18n]').forEach(el=>{el.textContent=t(el.dataset.i18n);});for(const attr of ['aria-label','placeholder','content'])document.querySelectorAll(`[data-i18n-${attr}]`).forEach(el=>el.setAttribute(attr,t(el.getAttribute(`data-i18n-${attr}`))));}
export function mountLanguageSelector(){
 if(document.getElementById('language-selector'))return;
 const label=document.createElement('label');label.className='language-control';label.textContent='🌐 ';const select=document.createElement('select');select.id='language-selector';select.setAttribute('aria-label',locales.map(l=>nativeNames[l]).join(' / '));
 for(const id of locales){const option=document.createElement('option');option.value=id;option.lang=id;option.textContent=nativeNames[id];select.append(option);}const automatic=document.createElement('option');automatic.value='auto';automatic.textContent=systemNames[locale];select.prepend(automatic);let manual=false;try{manual=locales.includes(globalThis.localStorage?.getItem('pianonote.language'));}catch{}select.value=manual?locale:'auto';label.append(select);
 (document.querySelector('.mode-banner')||document.querySelector('.intro')||document.querySelector('body > header')||document.body).append(label);
 select.addEventListener('change',()=>{if(select.value==='auto'){setLocale(resolveLocale(globalThis.navigator?.languages||[globalThis.navigator?.language||'ja']));try{globalThis.localStorage?.removeItem('pianonote.language');}catch{}}else setLocale(select.value);automatic.textContent=systemNames[locale];translatePage();document.dispatchEvent(new Event('pianonote-language'));});
}
