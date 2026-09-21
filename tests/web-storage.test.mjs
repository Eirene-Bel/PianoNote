import test from 'node:test';
import assert from 'node:assert/strict';
import {memoryStore} from '../site/assets/storage.mjs';

test('memory store isolates instances, commits clones, and rejects stale revisions',async()=>{
  const a=memoryStore(),b=memoryStore();
  const original=await a.read();
  assert.equal(original.revision,0);
  const saved=await a.commit(0,state=>{state.records.push({id:'x'});});
  assert.equal(saved.revision,1);
  saved.state.records.push({id:'y'});
  assert.equal((await a.read()).state.records.length,1);
  assert.equal((await b.read()).state.records.length,0);
  await assert.rejects(a.commit(0,()=>{}),{name:'ConflictError'});
  await assert.rejects(a.commit(1,()=>{throw Error('abort');}),/abort/);
  assert.equal((await a.read()).revision,1);
});
