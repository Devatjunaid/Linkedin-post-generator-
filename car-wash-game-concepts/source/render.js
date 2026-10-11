const {chromium}=require('/opt/node-tools/node_modules/playwright');
(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:2000,height:1200},deviceScaleFactor:2});
p.on('console',m=>console.log('console',m.text()));p.on('pageerror',e=>console.log('ERR',e.message));
await p.goto('file://'+process.cwd()+'/concepts.html');await p.evaluate(()=>document.fonts.ready);await p.waitForTimeout(500);
const ids=['home','wash','detail','reward','map','settings','board'];
for(const [i,id] of ids.entries()){await p.locator('#'+id).screenshot({path:`out/${i+1}-${id}.png`});}
await b.close();})();
