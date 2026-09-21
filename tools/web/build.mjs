import {catalogBundle} from './catalogs.mjs';
import {readFile,writeFile,copyFile,mkdir,readdir,lstat} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {execFileSync} from 'node:child_process';

const root=fileURLToPath(new URL('../../',import.meta.url));
const source=path.join(root,'site'),output=path.join(root,'artifacts','web-site');
const files=['index.html','demo/index.html','privacy.html','licenses.html','assets/demo.mjs','assets/i18n.mjs','assets/page-i18n.mjs','assets/catalogs.mjs',...['ja','en','zh-Hans','zh-Hant','ko','es','fr','de','pt'].map(locale=>`assets/locales/${locale}.json`),'assets/core.mjs','assets/storage.mjs','assets/question-timer.mjs','assets/score.mjs','assets/app.css','assets/landing.css','assets/icon.svg','assets/bravura.woff2','assets/bravura-LICENSE.txt'];
const onlyCheck=process.argv.includes('--check');
async function walk(dir,prefix=''){let names=[];for(const e of await readdir(dir,{withFileTypes:true})){const rel=prefix+e.name;const info=await lstat(path.join(dir,e.name));if(info.isSymbolicLink())throw new Error(`Symlink is not publishable: ${rel}`);names.push(...(e.isDirectory()?await walk(path.join(dir,e.name),rel+'/'):[rel]));}return names;}
if((await readFile(path.join(source,'assets/catalogs.mjs'),'utf8')).replace(/\r\n?/g,'\n')!==await catalogBundle())throw new Error('Stale catalogs: run node tools/web/catalogs.mjs');
const actual=await walk(source);
const unexpected=actual.filter(f=>!files.includes(f));
if(unexpected.length)throw new Error(`Unreviewed public files: ${unexpected.join(', ')}`);
const hashes={},contents=new Map();
for(const rel of files){
 const full=path.join(source,rel);let bytes=await readFile(full);
 if(!rel.endsWith('.woff2'))bytes=Buffer.from(bytes.toString('utf8').replace(/^\uFEFF/,'').replace(/\r\n?/g,'\n'));
 hashes[rel]=createHash('sha256').update(bytes).digest('hex');contents.set(rel,bytes);
 if(rel.endsWith('.woff2'))continue;
 const text=bytes.toString('utf8');
 const forbidden=[/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY/,/github_pat_[A-Za-z0-9_]{15,}/,/gh[pousr]_[A-Za-z0-9]{20,}/,/sk-proj-[A-Za-z0-9_-]{15,}/,/C:[\\/]+Users[\\/]/i,/\.superpowers[\\/]/,/storePassword\s*[=:]/,/keyPassword\s*[=:]/];
 if(forbidden.some(pattern=>pattern.test(text)))throw new Error(`Private material in ${rel}`);
 if(rel.endsWith('.html')){
  if(!/<html\s+lang="ja"/.test(text)||!text.includes('Content-Security-Policy'))throw new Error(`Missing language/security policy: ${rel}`);
  if(/<(?:script|iframe)[^>]+(?:src|href)="https?:/i.test(text))throw new Error(`External runtime resource: ${rel}`);
  for(const match of text.matchAll(/(?:src|href)="([^"#]+)"/g)){
   const value=match[1];if(/^(?:https?:|data:|mailto:)/.test(value))continue;
   if(value.startsWith('/'))throw new Error(`Root-relative URL breaks project Pages: ${rel} ${value}`);
   const target=path.posix.normalize(path.posix.join(path.posix.dirname(rel),value)).replace(/\/$/,'');
   if(target.startsWith('../')||(!files.includes(target)&&!files.includes(target.replace(/\/$/,'')+'/index.html')&&target!=='.'))throw new Error(`Broken relative URL: ${rel} ${value}`);
  }
 }
 if(/\.(mjs|js)$/.test(rel))execFileSync(process.execPath,['--check',full],{stdio:'pipe'});
}
if(!onlyCheck){
 await mkdir(output,{recursive:true});
 const old=(await walk(output)).filter(f=>!files.includes(f)&&f!=='public-manifest.json');if(old.length)throw new Error('Unexpected files in output; choose a clean output directory.');
 for(const rel of files){await mkdir(path.dirname(path.join(output,rel)),{recursive:true});await writeFile(path.join(output,rel),contents.get(rel));}
 await writeFile(path.join(output,'public-manifest.json'),JSON.stringify({version:'0.4.0',files:hashes},null,2)+'\n');
}
console.log(`${onlyCheck?'Checked':'Built'} ${files.length} approved public files; no private paths, signing material or external runtime scripts.`);
