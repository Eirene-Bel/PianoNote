import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const base=process.argv[2]||'https://eirene-bel.github.io/PianoNote/';
if(!base.startsWith('https://'))throw new Error('Verify the public HTTPS URL.');
const expected=JSON.parse(await readFile(new URL('../../artifacts/web-site/public-manifest.json',import.meta.url),'utf8'));
const results=await Promise.allSettled(Object.entries(expected.files).map(async([file,hash])=>{
 const response=await fetch(new URL(file+'?verification='+hash.slice(0,12),base));
 if(!response.ok)throw new Error(file+': '+response.status);
 const actual=createHash('sha256').update(Buffer.from(await response.arrayBuffer())).digest('hex');
 if(actual!==hash)throw new Error(file+': published bytes differ from approved artifact');
 return {file,status:response.status,sha256:actual};
}));
const failures=results.filter(result=>result.status==='rejected');
if(failures.length)throw new Error(failures.map(result=>result.reason.message).join('\n'));
const checks=results.map(result=>result.value);
const output=new URL('../../artifacts/web-evidence/',import.meta.url);await mkdir(output,{recursive:true});
await writeFile(new URL('https-verification.json',output),JSON.stringify({base,checkedAt:new Date().toISOString(),checks},null,2));
console.log(`${checks.length} live HTTPS files match the approved public artifact.`);
