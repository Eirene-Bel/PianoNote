import {t,displayNote,escapeHtml} from './i18n.mjs';
import {noteName} from './core.mjs';
export function scoreSvg(q){
 const bottom=q.clef==='treble'?30:18,showNames=!!q.firstJudged||!!q.revealed;
 let svg=[50,62,74,86,98].map(y=>`<line x1="20" y1="${y}" x2="340" y2="${y}" stroke="var(--ink)" stroke-width="1.2"/>`).join('');
 svg+=`<text x="27" y="${q.clef==='treble'?86:62}" font-family="Bravura" font-size="49" fill="var(--ink)">${q.clef==='treble'?'&#xe050;':'&#xe062;'}</text>`;
 q.notes.forEach((step,i)=>{
  const x=q.notes.length===1?198:120+i*(185/(q.notes.length-1)),y=98-(step-bottom)*6;
  const name=noteName(step),wrong=!!q.firstJudged&&q.answers[i]!==name;
  const color=wrong?'var(--wrong)':'var(--ink)';
  for(let ledger=110;ledger<=y;ledger+=12)svg+=`<line x1="${x-12}" x2="${x+12}" y1="${ledger}" y2="${ledger}" stroke="var(--ink)" stroke-width="1.2"/>`;
  for(let ledger=38;ledger>=y;ledger-=12)svg+=`<line x1="${x-12}" x2="${x+12}" y1="${ledger}" y2="${ledger}" stroke="var(--ink)" stroke-width="1.2"/>`;
  svg+=`<g class="sound-note ${wrong?'is-wrong':''}" data-sound-index="${i}"><ellipse cx="${x}" cy="${y}" rx="7" ry="4.8" transform="rotate(-18 ${x} ${y})" fill="${color}"/><line x1="${x+(y>=74?6:-6)}" x2="${x+(y>=74?6:-6)}" y1="${y}" y2="${y+(y>=74?-34:34)}" stroke="${color}" stroke-width="1.5"/></g>`;
  if(showNames){const labelY=Math.min(28,y-18,y>=74?y-44:28),mark=q.firstJudged?(wrong?'× ':'○ '):'';
   svg+=`<text class="note-name ${wrong?'is-wrong':''}" data-note-label="${i}" x="${x}" y="${labelY}" text-anchor="middle" font-size="20" font-weight="700" fill="${wrong?'var(--wrong)':'var(--blue)'}" aria-label="${i+1}${escapeHtml(t("w121"))} ${escapeHtml(q.firstJudged?(wrong?t("w234"):t("w235")):'')}${displayNote(name)}">${mark}${displayNote(name)}</text>`;
  }
 });
 return `<svg viewBox="0 -20 360 170" role="img" aria-label="${escapeHtml(q.clef==='treble'?t("w007"):t("w008"))}${escapeHtml(t("w236"))}${q.notes.length}${escapeHtml(t("w237"))}${escapeHtml(showNames?t("w238"):'')}">${svg}</svg>`;
}
