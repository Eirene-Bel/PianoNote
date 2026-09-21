'use strict';
const report=value=>{if(window.AndroidScore)window.AndroidScore.postMessage(JSON.stringify(value));};
let fontsReady=false;
function fitScore(){
  const svg=document.querySelector('#score svg');
  if(svg&&window.innerHeight>0){
    // Android WebView can resolve vh/% to zero during its first layout.
    const height=window.innerHeight+'px';
    document.getElementById('score').style.height=height;svg.style.height=height;
  }
}
window.addEventListener('resize',fitScore);
function draw(value){
  if(!fontsReady)return;
  if(value.schema!==1||!['treble','bass'].includes(value.clef)||typeof value.cardId!=='string'||value.cardId.length>128||!Array.isArray(value.notes)||value.notes.length<1||value.notes.length>3||!value.notes.every(n=>Number.isInteger(n)&&n>=14&&n<=42)||!Array.isArray(value.feedback)||!(value.feedback.length===0||value.feedback.length===value.notes.length)||!value.feedback.every(x=>['neutral','correct','wrong'].includes(x)))return;
  try{
    const host=document.getElementById('score');host.replaceChildren();
    const renderer=new VexFlow.Renderer(host,VexFlow.Renderer.Backends.SVG);renderer.resize(390,230);
    const context=renderer.getContext();const stave=new VexFlow.Stave(20,88,350);
    stave.addClef(value.clef).setContext(context).draw();
    const letters=['c','d','e','f','g','a','b'];
    const notes=value.notes.map(n=>new VexFlow.StaveNote({clef:value.clef,keys:[`${letters[n%7]}/${Math.floor(n/7)}`],duration:'q',autoStem:true}));
    VexFlow.Formatter.FormatAndDraw(context,stave,notes);
    const svg=host.querySelector('svg');svg.setAttribute('viewBox','0 50 390 150');svg.style.removeProperty('width');svg.style.removeProperty('height');
    if(value.feedback.length){
      const names=Array.isArray(value.noteNames)&&value.noteNames.length===7?value.noteNames:['C','D','E','F','G','A','B'];
      value.feedback.forEach((status,index)=>{
        const label=document.createElementNS('http://www.w3.org/2000/svg','text');
        label.setAttribute('class','score-feedback-label');
        label.setAttribute('x',String(notes[index].getAbsoluteX()));label.setAttribute('y','85');
        label.setAttribute('text-anchor','middle');label.setAttribute('stroke','none');label.setAttribute('font-size','24');
        label.setAttribute('font-weight','700');label.setAttribute('fill',status==='wrong'?'#B42332':'#2058A8');
        label.textContent=(status==='wrong'?'× ':status==='correct'?'○ ':'')+names[value.notes[index]%7];
        svg.appendChild(label);
      });
    }
    svg.setAttribute('role','img');svg.setAttribute('aria-label',value.description||'');
    host.setAttribute('aria-label',value.description||'');document.documentElement.lang=value.language||'en';
    host.dataset.cardId=value.cardId;
    fitScore();requestAnimationFrame(fitScore);
    report({type:'rendered',cardId:value.cardId});
  }catch(error){report({type:'error',cardId:value.cardId});}
}
window.addEventListener('message',event=>{
  // Android's native postWebMessage has an empty sender origin. The host fixes
  // the target origin, serves only bundled assets and blocks all other requests.
  if(event.origin!=='')return;
  try{draw(JSON.parse(event.data));}catch(error){report({type:'error',cardId:''});}
});
Promise.all([document.fonts.load('20px Bravura'),document.fonts.load('20px Academico')]).then(()=>{VexFlow.setFonts('Bravura','Academico');fontsReady=true;report({type:'ready'});}).catch(()=>report({type:'error',cardId:''}));
