import {emptyState} from './core.mjs';

const clone=value=>structuredClone(value);
export class ConflictError extends Error {constructor(){super('保存内容が別の画面で更新されました。読み込み直してください。');this.name='ConflictError';}}

export function memoryStore(initial=emptyState()){
  let state=clone(initial),revision=0,closed=false;
  const ensure=()=>{if(closed)throw new Error('Store is closed');};
  return {
    async read(){ensure();return {revision,state:clone(state)};},
    async commit(expectedRevision,mutator){ensure();if(revision!==expectedRevision)throw new ConflictError();const next=clone(state);const returned=mutator(next);if(returned&&typeof returned.then==='function')throw new TypeError('Mutator must be synchronous');state=next;revision++;return {revision,state:clone(state)};},
    close(){closed=true;}
  };
}
