import test from 'node:test';
import assert from 'node:assert/strict';
import {emptyState,defaultSettings,parseBackup,exportBackup,previewImport,mergeImport,windowStats,nextQuestion,noteFrequency,heatLevel,localDay} from '../site/assets/core.mjs';

const id='11111111-1111-4111-8111-111111111111';
const id2='22222222-2222-4222-8222-222222222222';
const record={id,date:'2026-09-21',piece:'Sonata',minutes:30,range:'bars 1-4',memo:'legacy',next:'tomorrow',revision:2,created_at:'2026-09-21T00:00:00Z',difficult_parts:'left',unpracticed_parts:'end',finishing_image:'soft'};
const attempt={id:id2,date:'2026-09-21',answeredAt:10,mode:'read',clefOption:'both',countOption:0,range:'staff',actualClef:'treble',notes:'30,31',correct:true,assisted:false};
function payload(overrides={}) {return JSON.stringify({format:'piano-practice',formatVersion:2,records:[record],deletedIds:[],attempts:[attempt],sessions:[],settings:defaultSettings(),draft:null,...overrides});}

test('v2 roundtrip preserves all Android fields and nullable settings',()=>{
  const parsed=parseBackup(payload());
  assert.deepEqual(parsed.records[0],record);
  assert.deepEqual(parsed.attempts[0],attempt);
  assert.deepEqual(parseBackup(exportBackup(parsed)).records[0],record);
  assert.equal(emptyState().settings,null);
});
test('v1 import accepts omitted v2 sections',()=>{
  const parsed=parseBackup(JSON.stringify({format:'piano-practice',formatVersion:1,records:[record]}));
  assert.deepEqual(parsed.attempts,[]);
  assert.equal(parsed.settings,null);
});
test('malformed payload rejects before merge',()=>{
  assert.throws(()=>parseBackup(payload({attempts:[{...attempt,notes:'99'}]})));
  assert.throws(()=>parseBackup(payload({records:[record,{...record,id}]})));
  assert.throws(()=>parseBackup(payload({deletedIds:[id]})));
  assert.throws(()=>parseBackup(payload({records:[{...record,revision:2147483648}]})));
  assert.throws(()=>parseBackup(payload({records:[{...record,created_at:'2026-09-21T00:00:00'}]})));
  assert.throws(()=>parseBackup(payload({records:[{...record,created_at:'2026-02-30T00:00:00Z'}]})));
});
test('Android import trims every record text field before merge and export',()=>{
  const padded={...record,piece:'  Sonata  ',range:'  bars 1-4 ',memo:' legacy ',next:' tomorrow ',difficult_parts:' left ',unpracticed_parts:' end ',finishing_image:' soft '};
  const parsed=parseBackup(payload({records:[padded]}));
  assert.deepEqual(parsed.records[0],record);
  assert.equal(previewImport(parseBackup(payload()),parsed).conflicts.length,0);
  assert.deepEqual(parseBackup(exportBackup(parsed)).records[0],record);
});
test('import is idempotent and protects local conflicts and tombstones',()=>{
  const incoming=parseBackup(payload());
  const first=mergeImport(emptyState(),incoming);
  assert.deepEqual(mergeImport(first,incoming),first);
  const local={...first,records:[{...record,piece:'local'}],settings:{...defaultSettings(),mode:'listen'},draft:JSON.stringify({input:{date:'2026-09-21',piece:'local',minutes:'30'}})};
  const preview=previewImport(local,incoming);
  assert.equal(preview.conflicts.length,1);
  assert.equal(preview.settingsPreserved,true);
  assert.throws(()=>mergeImport(local,incoming));
  assert.equal(mergeImport(local,incoming,{skipConflicts:true}).records[0].piece,'local');
  const tomb={...emptyState(),deletedIds:[id]};
  assert.equal(mergeImport(tomb,incoming).records.length,0);
});
test('stats use first unassisted answers and 0-100 rates',()=>{
  const rows=[1,2,3,4].map((n)=>({...attempt,id:`${n}1111111-1111-4111-8111-111111111111`,answeredAt:n,correct:n<3}));
  rows.push({...attempt,id:'33333333-3333-4333-8333-333333333333',answeredAt:9,assisted:true});
  const result=windowStats(rows,{mode:'read',clef:'both',count:0,range:'staff'},2);
  assert.equal(result.rate,0);
  assert.equal(result.previousRate,100);
  assert.equal(result.recent.length,2);
});
test('random count resolves at question generation and note math matches Android',()=>{
  const q=nextQuestion({clef:'treble',count:0,range:'staff'},null,()=>0);
  assert.equal(q.notes.length,1);
  assert.deepEqual(q.config,{clef:'treble',count:0,range:'staff'});
  const mixed=nextQuestion({clef:'both',count:0,range:'wide'},null,()=>0);
  assert.deepEqual(mixed.config,{clef:'both',count:0,range:'wide'});
  assert.equal(mixed.clef,'treble');
  assert.equal(q.notes[0],30);
  assert.ok(Math.abs(noteFrequency(33)-440)<0.001);
  assert.equal(heatLevel(1800),3);
  assert.equal(localDay(new Date(2026,8,21,12)),'2026-09-21');
});
test('Android v2 draft envelope and old record fields survive export',()=>{
  const draft=JSON.stringify({input:{date:'2026-09-21',piece:'Prelude',minutes:'25',difficult_parts:'a',unpracticed_parts:'b',finishing_image:'c'},editingId:id,revision:2,operationId:id2,goalTouched:true});
  const parsed=parseBackup(payload({draft}));
  const restored=parseBackup(exportBackup(parsed));
  assert.equal(restored.draft,draft);
  assert.deepEqual(restored.records[0],record);
});
test('question ID stays a valid UUID when randomUUID is unavailable',()=>{
  const original=Object.getOwnPropertyDescriptor(globalThis,'crypto');
  try {
    Object.defineProperty(globalThis,'crypto',{configurable:true,value:{getRandomValues(bytes){bytes.fill(0);return bytes;}}});
    const q=nextQuestion({clef:'treble',count:1,range:'staff'},null,()=>0);
    assert.match(q.id,/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  } finally {if(original)Object.defineProperty(globalThis,'crypto',original);else delete globalThis.crypto;}
});
