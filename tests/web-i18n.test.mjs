import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import * as Core from '../site/assets/core.mjs';
import * as I18n from '../site/assets/i18n.mjs';
import {catalogs} from '../site/assets/catalogs.mjs';
import {scoreSvg} from '../site/assets/score.mjs';
import {QuestionTimer} from '../site/assets/question-timer.mjs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8').replace(/\r\n?/g,'\n');

test('all nine offline catalogs are complete; no fallback masks missing messages',()=>{
 assert.deepEqual(Object.keys(catalogs).sort(),[...I18n.locales].sort());
 for(const locale of I18n.locales){assert.deepEqual(Object.keys(catalogs[locale]).sort(),Object.keys(catalogs.ja).sort());I18n.setLocale(locale);for(const key of Object.keys(catalogs.ja))assert.ok(I18n.t(key,{current:1,total:3,count:1}).trim(),`${locale}/${key}`);}
 assert.throws(()=>I18n.t('audioProgress'),/Missing message parameter/);
 assert.throws(()=>I18n.t('missing-key'),/Missing translation/);
});
test('browser preferences support regional languages and Chinese script variants',()=>{
 for(const [input,want] of [['ja-JP','ja'],['en-US','en'],['zh-CN','zh-Hans'],['zh-SG','zh-Hans'],['zh-TW','zh-Hant'],['zh-HK','zh-Hant'],['zh-Hant-CN','zh-Hant'],['zh-Hans-TW','zh-Hans'],['ko-KR','ko'],['es-MX','es'],['fr-CA','fr'],['de-AT','de'],['pt-BR','pt']])assert.equal(I18n.resolveLocale([input]),want);
 assert.equal(I18n.resolveLocale(['it-IT','fr-FR']),'fr');assert.equal(I18n.resolveLocale(['it-IT']),'en');
});
test('one-answer labels and audio progress use complete messages',()=>{
 I18n.setLocale('en');
 assert.equal(I18n.formatCount(1,'questions'),'1 question');
 assert.equal(I18n.formatCount(2,'questions'),'2 questions');
 assert.equal(I18n.t('audioProgress',{current:1,total:3}),'Playing note 1 of 3');
 assert.equal(I18n.t('w077',{count:1}),'Accuracy · recent answers: 1');
});
test('localized dates, duration and display notes preserve canonical scoring tokens',()=>{
 for(const locale of I18n.locales){I18n.setLocale(locale);assert.ok(I18n.formatDay('2026-09-21'));assert.ok(I18n.formatMonth('2026-09'));assert.equal(I18n.weekdays().length,7);assert.ok(I18n.formatDuration(3661));assert.equal(Core.noteName(28),'ド');
  const html=scoreSvg({clef:'treble',notes:[28,34],answers:['ド','シ'],firstJudged:true});assert.doesNotMatch(html,/is-wrong/);assert.ok(html.includes(I18n.displayNote('ド')));assert.ok(html.includes(I18n.displayNote('シ')));
 }
 I18n.setLocale('de');assert.equal(I18n.displayNote('シ'),'H');I18n.setLocale('en');assert.equal(I18n.displayNote('ド'),'C');assert.equal(I18n.formatCount(1,'notes'),'1 note');assert.equal(I18n.formatCount(2,'notes'),'2 notes');assert.equal(I18n.t('audioProgress',{current:1,total:3,count:1}),'Playing note 1 of 3');
});
test('every static page explicitly marks authored text and loads the selector',()=>{
 for(const page of ['index.html','demo/index.html','privacy.html','licenses.html']){const html=read('site/'+page);assert.match(html,/page-i18n.mjs/);for(const match of html.matchAll(/data-i18n(?:-[a-z-]+)?="(w\d+)"/g))assert.ok(catalogs.ja[match[1]],`${page}/${match[1]}`);assert.doesNotMatch(html,/<option[^>]*><span/);}
});
test('all main demo screens render in every locale without translating user record text',()=>{
 const elements=new Map();const element=id=>{if(!elements.has(id))elements.set(id,{addEventListener(){},classList:{toggle(){}},open:false,hidden:true});return elements.get(id);};
 const context={...I18n,Core,scoreSvg,QuestionTimer,crypto:globalThis.crypto,Date,Intl,structuredClone,setTimeout:()=>0,clearTimeout(){},setInterval:()=>0,clearInterval(){},document:{getElementById:element,addEventListener(){},querySelectorAll:()=>[]},window:{addEventListener(){}}};
 vm.createContext(context);
 const source=read('site/assets/demo.mjs').replace(/^import .*;\n/gm,'').replace(/\nstart\(\);\s*$/,'');
 vm.runInContext(source+`\nglobalThis.demo={seed(){state=createSample();state.records[0].piece='音符 <script>';state.records[0].difficult_parts='設定';project();question={...question,notes:[28,34],answers:['ド','シ'],firstJudged:true,config:data.quizConfig,trainingMode:'ANSWER'};},views(){return [renderNotes(),renderRecord(),renderAccuracy(),renderCalendar(),renderHistory(),renderSettings()];},feedbackText,error(key){storageError=key;status();return $('storage-status').innerHTML;}};`,context);
 for(const locale of I18n.locales){I18n.setLocale(locale);context.demo.seed();const views=context.demo.views();for(const html of views){assert.doesNotMatch(html,/undefined|\[object Object\]/);assert.doesNotMatch(html,/\$\{t\(/);}assert.ok(views[4].includes('音符 &lt;script&gt;'));assert.ok(views[0].includes(`data-note="ド"`));assert.ok(views[0].includes(I18n.displayNote('シ')));assert.equal(context.demo.feedbackText(),catalogs[locale].w039);assert.ok(context.demo.error('w020').includes(I18n.escapeHtml(catalogs[locale].w020)));context.demo.error('');}
});

test('demo language selector stays outside the intro hidden on mobile',()=>{
 const make=()=>({children:[],append(child){this.children.push(child);child.parent=this;},prepend(child){this.children.unshift(child);},setAttribute(){},addEventListener(){}});
 const intro=make(), banner=make(), body=make();
 const previous=globalThis.document;
 globalThis.document={getElementById:()=>null,createElement:make,body,querySelector:s=>s==='.intro'?intro:s==='.mode-banner'?banner:null};
 try { I18n.mountLanguageSelector(); assert.equal(banner.children.length,1); assert.equal(intro.children.length,0); }
 finally { if(previous)globalThis.document=previous;else delete globalThis.document; }
});
