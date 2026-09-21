/** Monotonic answering intervals. Pausing retains eligibility; finishing never does. */
export class QuestionTimer {
 constructor(now=()=>performance.now()){this.now=now;this.running=false;this.eligible=false;this.last=0;}
 start(){this.eligible=true;this.resume();}
 resume(){if(this.eligible&&!this.running){this.last=this.now();this.running=true;}}
 tick(){if(!this.running)return 0;const now=this.now(),elapsed=Math.max(0,now-this.last)/1000;this.last=now;return elapsed;}
 pause(){const elapsed=this.tick();this.running=false;return elapsed;}
 finish(){const elapsed=this.pause();this.eligible=false;return elapsed;}
}
