const {chromium}=require('playwright');
const dir=__dirname;
const jobs=[['logo',1000,360,2,true,'logo-transparent-2000x720.png'],['logo-square',1024,1024,1,false,'logo-square-1024.png'],['feature',1024,500,1,false,'feature-graphic-1024x500.png'],
 ...[1,2,3,4,5].map(i=>['shot'+i,360,640,3,false,`screenshot-${i}-1080x1920.png`])];
(async()=>{const b=await chromium.launch();
for(const [a,w,h,dsf,transparent,name] of jobs){
 const p=await b.newPage({viewport:{width:w,height:h},deviceScaleFactor:dsf});
 await p.goto(`file://${dir}/brand.html?a=${a}`,{waitUntil:'networkidle'});
 await p.evaluate(()=>document.fonts.ready); await p.waitForTimeout(300);
 await p.screenshot({path:`${dir}/out/${name}`,omitBackground:transparent,clip:{x:0,y:0,width:w,height:h}}); await p.close();}
await b.close();})();
