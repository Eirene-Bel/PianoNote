import test from 'node:test';
import assert from 'node:assert/strict';
import {QuestionTimer} from '../site/assets/question-timer.mjs';
import {setLocale} from '../site/assets/i18n.mjs';
import {scoreSvg} from '../site/assets/score.mjs';

test('only a started unanswered question counts, including thinking longer than a minute',()=>{
 let now=0;const timer=new QuestionTimer(()=>now);
 now=5000;assert.equal(timer.tick(),0);assert.equal(timer.running,false);
 timer.start();now+=75000;assert.equal(timer.tick(),75);
 now+=250;assert.equal(timer.finish(),.25);assert.equal(timer.running,false);
 now+=6000;timer.resume();assert.equal(timer.tick(),0);assert.equal(timer.running,false);
});
test('pause excludes time away and resumes only an unfinished question; fractions are retained',()=>{
 let now=0;const timer=new QuestionTimer(()=>now);timer.start();
 now=250;let elapsed=timer.pause();now=10000;timer.resume();now=10250;elapsed+=timer.finish();
 timer.start();now=10750;elapsed+=timer.finish();assert.equal(elapsed,1);
 now=20000;timer.resume();assert.equal(timer.tick(),0);
});
test('score does not reveal names before grading and marks individual mistakes above the note',()=>{
 setLocale('ja');
 const q={clef:'treble',notes:[28,32,42],answers:['レ','ソ','ド'],firstJudged:false,revealed:false};
 assert.doesNotMatch(scoreSvg(q),/data-note-label/);
 q.firstJudged=true;const result=scoreSvg(q);
 assert.equal((result.match(/data-note-label=/g)||[]).length,3);
 assert.match(result,/× ド/);assert.match(result,/○ ソ/);assert.match(result,/is-wrong/);
 assert.match(result,/viewBox="0 -20 360 170"/);
 const corrected=scoreSvg({...q,answers:['ド','ソ','ド']});assert.doesNotMatch(corrected,/×/);
 const revealed=scoreSvg({...q,firstJudged:false,revealed:true,answers:[]});assert.match(revealed,/data-note-label/);assert.doesNotMatch(revealed,/[×○]/);
});
