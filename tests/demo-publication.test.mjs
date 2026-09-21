import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
test('Pages exposes an introduction and demo, without a personal notebook or persistent browser store',()=>{
 assert.equal(existsSync(new URL('../site/app/index.html',import.meta.url)),false);
 assert.equal(existsSync(new URL('../site/sw.js',import.meta.url)),false);
 for(const p of ['site/index.html','site/demo/index.html','site/assets/demo.mjs','site/privacy.html'])assert.doesNotMatch(read(p),/自分のノートを始める|自分の練習を記録する|href=["'][^"']*\/app\//);
 for(const p of ['site/assets/demo.mjs','site/assets/storage.mjs'])assert.doesNotMatch(read(p),/localStorage|indexedDB|openStore|serviceWorker/);
 assert.match(read('site/assets/demo.mjs'),/store=memoryStore\(createSample\(\)\)/);
 assert.match(read('site/index.html'),/href="\.\/demo\/"/);
 assert.doesNotMatch(read('site/demo/index.html'),/type="file"/);
 assert.doesNotMatch(read('site/assets/demo.mjs'),/data-action="(?:import|export)"/);
});
