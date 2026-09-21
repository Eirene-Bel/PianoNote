import {readFile,writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {resolve} from 'node:path';
const locales=['ja','en','zh-Hans','zh-Hant','ko','es','fr','de','pt'];
export async function catalogBundle(){
 const catalogs={};for(const locale of locales)catalogs[locale]=JSON.parse(await readFile(new URL(`../../site/assets/locales/${locale}.json`,import.meta.url),'utf8'));
 const keys=Object.keys(catalogs.ja).sort();for(const [locale,messages] of Object.entries(catalogs)){if(JSON.stringify(Object.keys(messages).sort())!==JSON.stringify(keys)||Object.values(messages).some(v=>typeof v!=='string'||!v.trim()))throw new Error(`Incomplete locale: ${locale}`);}
 const parameters=text=>[...text.matchAll(/\{(\w+)\}/g)].map(match=>match[1]).sort().join(',');
 for(const [locale,messages] of Object.entries(catalogs))for(const key of keys)if(parameters(messages[key])!==parameters(catalogs.ja[key]))throw new Error(`Message parameters differ: ${locale}/${key}`);
 return '// Generated from locales/*.json by node tools/web/catalogs.mjs.\nexport const catalogs='+JSON.stringify(catalogs)+';\n';
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){await writeFile(new URL('../../site/assets/catalogs.mjs',import.meta.url),await catalogBundle());}
