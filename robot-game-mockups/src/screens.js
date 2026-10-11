// Each function returns the HTML body for one mockup screen (540x960 CSS px, rendered at 2x).
const { OL, robot, I, sparkle, confetti, rnd, starPath, gearPath } = require('./lib');

const pill = (x, y, icon, val, w = 150) =>
  `<div class="pill" style="left:${x}px;top:${y}px;width:${w}px"><span class="ic">${icon}</span><span class="ol-s">${val}</span><span class="plus">+</span></div>`;
const rbtn = (x, y, cls, icon, size = 68, extra = '') =>
  `<div class="rbtn ${cls}" style="left:${x}px;top:${y}px;width:${size}px;height:${size}px;border-radius:${size * 0.36}px">${icon}${extra}</div>`;

const bigStar = (x, y, s, rot = 0, lit = true) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px;transform:rotate(${rot}deg);filter:drop-shadow(0 8px 0 ${OL})" viewBox="0 0 100 100">
  <defs><linearGradient id="gs${x}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${lit ? '#FFF6A0' : '#8E8BB5'}"/><stop offset=".55" stop-color="${lit ? '#FFC928' : '#625E91'}"/><stop offset="1" stop-color="${lit ? '#FF9A1F' : '#4A4677'}"/></linearGradient></defs>
  <path d="${starPath(50, 54, 46, 22)}" fill="url(#gs${x})" stroke="${OL}" stroke-width="6" stroke-linejoin="round"/>
  ${lit ? `<path d="M30 40 Q34 32 44 30" stroke="#fff" stroke-width="6" fill="none" stroke-linecap="round" opacity=".85"/>` : ''}</svg>`;

const ribbon = (x, y, w, text, c1 = '#FF5C8A', c2 = '#E0306A', fs = 46) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${w}px;height:${w * 0.26}px" viewBox="0 0 460 120">
  <path d="M10 44 H70 V104 H10 L30 74Z" fill="${c2}" stroke="${OL}" stroke-width="5" stroke-linejoin="round"/>
  <path d="M450 44 H390 V104 H450 L430 74Z" fill="${c2}" stroke="${OL}" stroke-width="5" stroke-linejoin="round"/>
  <path d="M50 20 Q230 0 410 20 V92 Q230 72 50 92Z" fill="${c1}" stroke="${OL}" stroke-width="6" stroke-linejoin="round"/>
  <path d="M60 30 Q230 12 400 30" stroke="#fff" stroke-width="5" fill="none" opacity=".45" stroke-linecap="round"/>
  <text x="230" y="${58 + fs * 0.32}" text-anchor="middle" font-family="Lilita" font-size="${fs}" fill="#fff" stroke="${OL}" stroke-width="9" paint-order="stroke" stroke-linejoin="round">${text}</text></svg>`;

const deco = (n, w, h, items) => { let s = ''; for (let i = 0; i < n; i++) { const it = items[i % items.length]; s += `<div class="abs" style="left:${rnd() * w}px;top:${rnd() * h}px;opacity:.5;transform:rotate(${rnd() * 360}deg) scale(${0.8 + rnd()})">${it}</div>`; } return s; };

// -------------------------------------------------------------- HOME
function home() {
  return `<div class="screen" style="background:radial-gradient(circle at 50% 46%,#FFF3B0 0,#FFB8E8 17%,#9C7BFF 46%,#5A35D8 76%,#341A93 100%)">
  <div class="abs" style="inset:0;background:repeating-conic-gradient(from 0deg at 50% 50%,rgba(255,255,255,.17) 0 8deg,transparent 8deg 18deg);-webkit-mask:radial-gradient(circle at 50% 50%,#000 8%,transparent 60%)"></div>
  ${deco(8, 90, 640, [I.gear(30, '#C9B8FF'), I.nut(26), I.gear(22, '#8FE9FF'), I.star(20)])}<div class="abs" style="left:450px;top:80px">${deco(8, 70, 560, [I.nut(26), I.gear(28, '#C9B8FF'), I.star(20)])}</div>
  ${pill(30, 26, I.nut(46), '1,250')}${pill(208, 26, I.crystal(46), '48', 120)}
  ${rbtn(450, 18, 'blue', I.gear(40))}
  <svg class="abs" style="left:20px;top:84px;width:500px;transform:rotate(-4deg)" viewBox="0 0 500 200">
    <defs><linearGradient id="lg1" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF8B8"/><stop offset=".5" stop-color="#FFD43B"/><stop offset="1" stop-color="#FF8A1F"/></linearGradient>
    <linearGradient id="lg2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#C8FAFF"/><stop offset=".5" stop-color="#5CE1FF"/><stop offset="1" stop-color="#2D7BFF"/></linearGradient></defs>
    <g font-family="Lilita" text-anchor="middle" stroke-linejoin="round">
      <text x="250" y="98" font-size="104" fill="${OL}" stroke="${OL}" stroke-width="20" transform="translate(0 10)">BOLT</text>
      <text x="250" y="98" font-size="104" fill="url(#lg1)" stroke="${OL}" stroke-width="16" paint-order="stroke">BOLT</text>
      <text x="250" y="186" font-size="86" fill="${OL}" stroke="${OL}" stroke-width="20" transform="translate(0 10)">BUDDIES</text>
      <text x="250" y="186" font-size="86" fill="url(#lg2)" stroke="${OL}" stroke-width="16" paint-order="stroke">BUDDIES</text>
    </g>
    <path d="M150 34 Q160 24 176 24" stroke="#fff" stroke-width="7" fill="none" stroke-linecap="round" opacity=".9"/>
  </svg>
  ${sparkle(40, 120, 34)}${sparkle(470, 210, 28, '#FFF3B0')}${sparkle(90, 300, 22)}${sparkle(430, 330, 36)}
  <!-- pedestal -->
  <div class="abs" style="left:110px;top:560px;width:320px;height:90px;border-radius:50%;background:radial-gradient(ellipse at 50% 40%,#9FF7FF,#3AB8FF 55%,#2D3FB8 100%);border:5px solid ${OL};box-shadow:0 12px 0 ${OL},0 0 60px 20px rgba(120,240,255,.55)"></div>
  <div class="abs" style="left:150px;top:574px;width:240px;height:52px;border-radius:50%;border:4px dashed rgba(255,255,255,.7)"></div>
  ${robot({ color: '#FF7A45', accent: '#FFD43B', head: 'round', eyes: 'big', mouth: 'open', wave: true, legs: 'legs', antenna: 'ball', style: 'position:absolute;left:142px;top:268px;width:256px;filter:drop-shadow(0 12px 18px rgba(0,0,0,.25))' })}
  ${robot({ color: '#FF6FB5', accent: '#5CF2FF', head: 'dome', eyes: 'visor', mouth: 'grin', antenna: 'double', legs: 'wheel', body: 'barrel', arms: 'claw', style: 'position:absolute;left:-14px;top:470px;width:150px;transform:rotate(-6deg)' })}
  ${robot({ color: '#57D96A', accent: '#FF8A1F', head: 'square', eyes: 'cyclops', mouth: 'smile', antenna: 'prop', legs: 'treads', body: 'tri', style: 'position:absolute;left:398px;top:476px;width:150px;transform:rotate(5deg)' })}
  <svg class="abs" style="left:0;top:640px;width:540px;height:320px" viewBox="0 0 540 320" preserveAspectRatio="none">
    <path d="M0 80 Q130 30 270 70 T540 60 V320 H0Z" fill="#3A1E9E" opacity=".6"/>
    <path d="M0 140 Q140 100 280 130 T540 120 V320 H0Z" fill="#2A1478"/>
  </svg>
  <div class="btn green lil" style="left:110px;top:690px;width:320px;height:110px;font-size:58px;border-radius:38px"><span class="ol">PLAY!</span>${I.play(50)}</div>
  ${sparkle(400, 680, 30)}${sparkle(116, 776, 20)}
  ${[['Robots', 'purple', robot({ only: ['antenna', 'head', 'face'], color: '#38B6FF', viewBox: '14 -10 172 150' }).replace('<svg', '<svg width="54" height="48"')],
     ['Gift', 'pink', I.gift(46)], ['Spin', 'orange', `<svg width="50" height="50" viewBox="0 0 50 50"><circle cx="25" cy="25" r="21" fill="#fff" stroke="${OL}" stroke-width="3"/>${['#FF5C8A', '#FFD43B', '#5CE1FF', '#7CF25A', '#B46BFF', '#FF8A1F'].map((c, i) => { const a1 = i * Math.PI / 3, a2 = a1 + Math.PI / 3; return `<path d="M25 25 L${25 + 19 * Math.cos(a1)} ${25 + 19 * Math.sin(a1)} A19 19 0 0 1 ${25 + 19 * Math.cos(a2)} ${25 + 19 * Math.sin(a2)}Z" fill="${c}"/>`; }).join('')}<circle cx="25" cy="25" r="6" fill="#fff" stroke="${OL}" stroke-width="3"/></svg>`]]
    .map(([t, c, ic], i) => `${rbtn(80 + i * 150, 834, c, ic, 84)}<div class="abs lil ol-s" style="left:${62 + i * 150}px;top:922px;width:120px;text-align:center;font-size:20px">${t}</div>`).join('')}
  <div class="badge" style="left:${80 + 150 + 66}px;top:824px">!</div>
  <div class="badge" style="left:${80 + 300 + 66}px;top:824px;background:#2EC24A">1</div>
  </div>`;
}

// -------------------------------------------------------------- MAP (no level numbers anywhere)
function mapScreen() {
  const lolly = (x, y, c) => `<g transform="translate(${x} ${y})"><path d="M0 0 V60" stroke="${OL}" stroke-width="9"/><path d="M0 0 V60" stroke="#fff" stroke-width="4"/><circle r="24" fill="${c}" stroke="${OL}" stroke-width="4"/><path d="M0 0 m-14 0 a14 14 0 1 1 14 14 a8 8 0 1 1 -8 -8" stroke="#fff" stroke-width="4" fill="none"/></g>`;
  const bush = (x, y, s = 1) => `<g transform="translate(${x} ${y}) scale(${s})"><path d="M-40 10 Q-44 -14 -20 -16 Q-14 -36 6 -30 Q24 -38 32 -18 Q50 -12 42 10Z" fill="#3FBF5A" stroke="${OL}" stroke-width="4" stroke-linejoin="round"/><path d="M-22 -6 Q-16 -14 -8 -12" stroke="#9CF28A" stroke-width="4" fill="none" stroke-linecap="round"/></g>`;
  const gflower = (x, y, c) => `<g transform="translate(${x} ${y})"><path d="M0 0 V34" stroke="#2E8B46" stroke-width="5"/><path d="${gearPath(0, 0, 8, 18, 13)}" fill="${c}" stroke="${OL}" stroke-width="3.5" stroke-linejoin="round"/><circle r="6" fill="#FFF3B0" stroke="${OL}" stroke-width="3"/></g>`;
  const planet = (x, y, r, c) => `<g transform="translate(${x} ${y})"><circle r="${r}" fill="${c}" stroke="${OL}" stroke-width="4"/><ellipse rx="${r * 1.7}" ry="${r * 0.4}" fill="none" stroke="#FFD43B" stroke-width="6" transform="rotate(-18)"/><circle cx="${-r * 0.35}" cy="${-r * 0.35}" r="${r * 0.22}" fill="#fff" opacity=".4"/></g>`;
  let starsBg = ''; for (let i = 0; i < 60; i++) starsBg += `<circle cx="${rnd() * 540}" cy="${rnd() * 330}" r="${rnd() * 2.4 + 0.6}" fill="#fff" opacity="${0.4 + rnd() * 0.6}"/>`;

  // node list along the road: done / current / locked / chest / mega
  const head = (c, ex = {}) => robot({ only: ['antenna', 'head', 'face'], color: c, viewBox: '10 -14 180 156', ...ex });
  const stars3 = n => `<div class="abs" style="left:50%;top:-30px;transform:translateX(-50%);display:flex;gap:0">${[0, 1, 2].map(i => `<div style="transform:translateY(${i === 1 ? -6 : 0}px)">${I.star(28, i < n ? '#FFD43B' : '#8E8BB5')}</div>`).join('')}</div>`;
  const done = (t, c, n, ex) => `<div class="abs" data-t="${t}" style="z-index:3;width:76px;height:76px;border-radius:50%;background:radial-gradient(circle at 40% 35%,#FFF6C2,#FFD43B 55%,#F29A12);border:5px solid ${OL};box-shadow:0 7px 0 ${OL};display:flex;align-items:center;justify-content:center">${head(c, ex).replace('<svg', '<svg width="56" height="50"')}${stars3(n)}</div>`;
  const locked = t => `<div class="abs" data-t="${t}" style="width:62px;height:62px;border-radius:50%;background:radial-gradient(circle at 40% 35%,#C9CCE8,#7D7FB0);border:5px solid ${OL};box-shadow:0 6px 0 ${OL};display:flex;align-items:center;justify-content:center">${I.lock(32)}</div>`;
  const chest = (t, open) => `<div class="abs" data-t="${t}" style="width:84px;height:84px;border-radius:26px;background:radial-gradient(circle at 40% 35%,#FFE6A8,#FF9F43);border:5px solid ${OL};box-shadow:0 7px 0 ${OL},0 0 30px 8px rgba(255,220,100,.8);display:flex;align-items:center;justify-content:center;transform:rotate(-6deg)">${I.chest(58)}</div>`;
  const current = t => `<div class="abs" data-t="${t}" style="width:112px;height:112px">
      <div class="abs" style="inset:-22px;border-radius:50%;border:6px solid rgba(255,255,255,.55)"></div>
      <div class="abs" style="inset:-40px;border-radius:50%;border:4px solid rgba(255,255,255,.25)"></div>
      <div class="abs" style="inset:0;border-radius:50%;background:radial-gradient(circle at 40% 35%,#B9FBFF,#3AD0FF 50%,#1F6DFF);border:6px solid ${OL};box-shadow:0 8px 0 ${OL},0 0 40px 14px rgba(140,250,255,.9);display:flex;align-items:center;justify-content:center">
        ${robot({ only: ['antenna', 'head'], color: '#1E1B4B', accent: '#1E1B4B', metal: '#1E1B4B', viewBox: '10 -14 180 156', style: 'width:78px;opacity:.85' })}
        <div class="abs lil ol" style="font-size:44px;top:22px">?</div></div>
      <div class="abs" style="left:66px;top:74px;transform:rotate(-20deg)">${I.hand(58)}</div>
      <div class="abs lil" style="left:-34px;top:-78px;width:180px;height:46px;background:#fff;border:4px solid ${OL};border-radius:23px;color:${OL};font-size:24px;display:flex;align-items:center;justify-content:center;box-shadow:0 5px 0 ${OL}">Build me!</div>
      <div class="abs" style="left:46px;top:-36px;width:18px;height:18px;background:#fff;border-right:4px solid ${OL};border-bottom:4px solid ${OL};transform:rotate(45deg)"></div>
    </div>`;
  const mega = t => `<div class="abs" data-t="${t}" style="width:140px;height:140px">
      <div class="abs" style="inset:0;border-radius:40px;background:radial-gradient(circle at 40% 35%,#FFD7F0,#C04BFF 60%,#6A1FC9);border:6px solid ${OL};box-shadow:0 8px 0 ${OL},0 0 40px 10px rgba(220,120,255,.8);transform:rotate(45deg) scale(.82)"></div>
      ${robot({ only: ['antenna', 'head', 'arms', 'body'], color: '#2A1666', accent: '#2A1666', metal: '#2A1666', eye: '#2A1666', viewBox: '0 -14 200 230', style: 'position:absolute;left:24px;top:10px;width:92px' })}
      <div class="abs" style="left:46px;top:46px">${I.lock(46)}</div>
      ${ribbon(-30, 112, 200, 'MEGA BOT!', '#FFD43B', '#FF9A1F', 50)}</div>`;
  const nodes = [
    done(0.075, '#FF7A45', 3), done(0.14, '#38B6FF', 3, { head: 'square', eyes: 'visor' }), done(0.205, '#57D96A', 2, { eyes: 'cyclops', antenna: 'prop' }),
    chest(0.27), done(0.335, '#FF6FB5', 3, { head: 'dome', eyes: 'hearts' }), done(0.4, '#B46BFF', 1, { antenna: 'double' }),
    current(0.505), locked(0.59), locked(0.648), chest(0.708), locked(0.768), mega(0.855)
  ].join('');

  return `<div class="screen" style="background:linear-gradient(to top,#7BE36A 0%,#45C46F 22%,#FFC1E3 34%,#FF8FC8 52%,#B45CF0 64%,#4B23B5 76%,#170D4A 100%)">
  <svg class="abs" style="left:0;top:0" width="540" height="960" viewBox="0 0 540 960">
    ${starsBg}
    ${planet(80, 120, 34, '#FF8A5C')}${planet(490, 340, 20, '#5CE1FF')}
    <g transform="translate(480 190) rotate(30)"><path d="M0 -40 Q16 -20 14 20 H-14 Q-16 -20 0 -40Z" fill="#F1F3FA" stroke="${OL}" stroke-width="4"/><circle cy="-8" r="7" fill="#5CE1FF" stroke="${OL}" stroke-width="3"/><path d="M-14 8 L-26 26 H-14Z M14 8 L26 26 H14Z" fill="#FF5C8A" stroke="${OL}" stroke-width="3" stroke-linejoin="round"/><path d="M-8 22 Q0 50 8 22Z" fill="#FFB21E"/></g>
        ${lolly(505, 585, '#FF5C8A')}${lolly(470, 700, '#FFD43B')}${lolly(40, 650, '#5CE1FF')}
    ${bush(470, 900, 1.1)}${bush(60, 760)}${bush(500, 770, 0.8)}${gflower(30, 880, '#FFD43B')}${gflower(110, 920, '#FF6FB5')}${gflower(420, 850, '#5CE1FF')}
    <path id="road" d="M280 1010 C120 960 70 880 170 830 S470 760 400 680 S110 610 150 530 S460 450 390 370 S100 300 160 220 S420 150 360 70 S230 -10 260 -60" fill="none" stroke="${OL}" stroke-width="54" stroke-linecap="round" opacity=".35"/>
    <path d="M280 1010 C120 960 70 880 170 830 S470 760 400 680 S110 610 150 530 S460 450 390 370 S100 300 160 220 S420 150 360 70 S230 -10 260 -60" fill="none" stroke="#FFF6E3" stroke-width="40" stroke-linecap="round"/>
    <path d="M280 1010 C120 960 70 880 170 830 S470 760 400 680 S110 610 150 530 S460 450 390 370 S100 300 160 220 S420 150 360 70 S230 -10 260 -60" fill="none" stroke="#F3C98B" stroke-width="6" stroke-dasharray="4 18" stroke-linecap="round"/>
  </svg>
  ${[['GEAR GARDEN', 330, 916, '#2EC24A'], ['CANDY FACTORY', 330, 536, '#FF4F9A'], ['MOON BASE', 336, 236, '#7B45F0']].map(([t, x, y, c]) => `<div class="abs lil ol-s" style="left:${x}px;top:${y}px;padding:4px 14px;border-radius:14px;background:${c};border:4px solid ${OL};font-size:19px;box-shadow:0 5px 0 ${OL};transform:rotate(-3deg)">${t}</div>`).join('')}
  ${nodes}
  <div class="abs" style="left:0;top:0;width:540px;height:96px;background:linear-gradient(#170D4A,rgba(23,13,74,0))"></div>
  ${rbtn(18, 18, 'orange', I.home(38), 60)}
  ${pill(110, 28, I.star(44), '42', 118)}${pill(258, 28, I.nut(44), '1,250', 140)}${pill(428, 28, I.crystal(44), '48', 100)}
  ${sparkle(250, 380, 26)}${sparkle(150, 690, 20)}${sparkle(440, 160, 22)}
  <script>const p=document.getElementById('road'),L=p.getTotalLength();document.querySelectorAll('[data-t]').forEach(n=>{const q=p.getPointAtLength(L*n.dataset.t);n.style.left=(q.x-n.offsetWidth/2)+'px';n.style.top=(q.y-n.offsetHeight/2)+'px';});</script>
  </div>`;
}

// -------------------------------------------------------------- WORKSHOP (building)
function workshop() {
  const headCard = (x, ex, state = '') => `<div class="abs" style="left:${x}px;top:744px;width:114px;height:136px;border-radius:24px;background:linear-gradient(#FFFFFF,#E6EEFF);border:4px solid ${OL};box-shadow:inset 0 -7px 0 #C9D6F5,0 6px 0 ${OL};display:flex;align-items:center;justify-content:center;${state === 'empty' ? 'opacity:.45;border-style:dashed' : ''}">
     ${state === 'empty' ? '' : robot({ only: ['antenna', 'head', 'face'], viewBox: '10 -14 180 156', style: 'width:94px', ...ex })}
     ${state === 'locked' ? `<div class="abs" style="inset:0;border-radius:20px;background:rgba(30,27,75,.55);display:flex;align-items:center;justify-content:center">${I.lock(48)}</div>
       <div class="abs lil ol-s" style="bottom:-16px;left:14px;width:80px;height:34px;border-radius:17px;background:${OL};display:flex;align-items:center;justify-content:center;gap:4px;font-size:19px">${I.nut(22)}300</div>` : ''}
     ${state === 'new' ? `<div class="badge lil" style="right:-10px;top:-12px;font-size:15px;transform:rotate(12deg)">NEW</div>` : ''}</div>`;
  const tab = (x, label, sel) => `<div class="abs lil ${sel ? 'ol-s' : ''}" style="left:${x}px;top:672px;width:118px;height:50px;border-radius:18px 18px 10px 10px;border:4px solid ${OL};background:${sel ? 'linear-gradient(#FFE66B,#FFB21E)' : 'linear-gradient(#4E4A8C,#36326E)'};display:flex;align-items:center;justify-content:center;font-size:22px;color:${sel ? '#fff' : '#B9B6E8'}">${label}</div>`;
  return `<div class="screen" style="background:linear-gradient(#FFC867,#FF8A5C)">
  <div class="abs" style="inset:0;background-image:radial-gradient(circle,rgba(120,40,0,.22) 3px,transparent 3.5px);background-size:28px 28px"></div>
  <!-- HUD -->
  ${rbtn(18, 18, 'blue', I.pause(32), 58)}
  <div class="abs" style="left:96px;top:30px;width:270px;height:34px;border-radius:17px;background:${OL};border:4px solid ${OL};overflow:hidden">
    <div style="width:72%;height:100%;border-radius:13px;background:linear-gradient(#FFF08A,#FFB21E);box-shadow:inset 0 -5px 0 rgba(0,0,0,.15)"></div></div>
  <div class="abs" style="left:156px;top:16px">${I.star(40)}</div><div class="abs" style="left:250px;top:16px">${I.star(40)}</div><div class="abs" style="left:338px;top:16px">${I.star(40, '#8E8BB5')}</div>
  ${pill(398, 28, I.nut(44), '1,250', 126)}
  <!-- blueprint -->
  <div class="abs" style="left:26px;top:96px;width:488px;height:548px;border-radius:30px;border:6px solid ${OL};background-color:#2F6BFF;
     background-image:linear-gradient(rgba(255,255,255,.18) 2px,transparent 2px),linear-gradient(90deg,rgba(255,255,255,.18) 2px,transparent 2px);background-size:32px 32px;box-shadow:inset 0 0 0 6px #6FA0FF,0 10px 0 ${OL}"></div>
  <div class="abs" style="left:10px;top:86px;width:90px;height:30px;background:rgba(255,255,255,.75);transform:rotate(-28deg)"></div>
  <div class="abs" style="left:442px;top:86px;width:90px;height:30px;background:rgba(255,255,255,.75);transform:rotate(28deg)"></div>
  ${robot({ color: '#57D96A', accent: '#FF8A1F', ghost: ['head', 'antenna'], legs: 'wheel', body: 'barrel', arms: 'claw', style: 'position:absolute;left:135px;top:184px;width:270px' })}
  <div class="abs" style="left:150px;top:166px;width:240px;height:200px;border-radius:50%;background:radial-gradient(rgba(255,255,255,.35),transparent 65%)"></div>
  <!-- dragged head -->
  <svg class="abs" style="left:250px;top:110px;width:200px;height:120px" viewBox="0 0 200 120"><path d="M170 100 Q120 10 40 50" fill="none" stroke="#fff" stroke-width="6" stroke-dasharray="2 14" stroke-linecap="round"/><path d="M52 34 L36 52 L58 60" fill="none" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/></svg>
  ${robot({ only: ['antenna', 'head', 'face'], color: '#57D96A', accent: '#FF8A1F', eyes: 'big', mouth: 'open', viewBox: '10 -14 180 156', cls: 'glow', style: 'position:absolute;left:356px;top:196px;width:170px;transform:rotate(14deg)' })}
  <div class="abs" style="left:470px;top:300px;transform:rotate(-15deg)">${I.hand(64)}</div>
  ${sparkle(350, 190, 26)}${sparkle(500, 180, 18)}${sparkle(470, 380, 22, '#FFF3B0')}
  <!-- helper bot with speech bubble -->
  ${robot({ color: '#FFD43B', accent: '#FF5C8A', head: 'round', eyes: 'wink', mouth: 'open', antenna: 'bolt', legs: 'wheel', wave: true, style: 'position:absolute;left:34px;top:530px;width:84px' })}
  <div class="abs" style="left:116px;top:572px;padding:8px 16px;background:#fff;border:4px solid ${OL};border-radius:22px;color:${OL};font-size:21px;box-shadow:0 5px 0 ${OL}">Snap the head on!</div>
  ${rbtn(446, 476, 'yellow', I.bulb(40), 58)}${rbtn(446, 548, 'pink', I.paint(38), 58)}
  <!-- parts tray -->
  <div class="abs" style="left:0;top:700px;width:540px;height:260px;background:linear-gradient(#3B3680,#24205A);border-top:6px solid ${OL};border-radius:36px 36px 0 0"></div>
  ${tab(18, 'Heads', true)}${tab(146, 'Arms')}${tab(274, 'Legs')}${tab(402, 'Fun ✦')}
  ${headCard(20, {}, 'empty')}${headCard(148, { color: '#38B6FF', head: 'square', eyes: 'visor', antenna: 'double' })}${headCard(276, { color: '#FF6FB5', head: 'dome', eyes: 'hearts', antenna: 'prop', mouth: 'o' }, 'new')}${headCard(404, { color: '#B46BFF', eyes: 'cyclops', antenna: 'bolt' }, 'locked')}
  <div class="abs" style="left:0;top:908px;width:540px;height:52px;background:repeating-linear-gradient(-45deg,#FFD43B 0 22px,${OL} 22px 44px);border-top:5px solid ${OL}"></div>
  </div>`;
}

// -------------------------------------------------------------- REWARD
function reward() {
  const card = (x, inner, label, cls) => `<div class="abs" style="left:${x}px;top:662px;width:146px;height:140px;border-radius:28px;border:5px solid ${OL};box-shadow:inset 0 -8px 0 rgba(0,0,0,.15),inset 0 6px 0 rgba(255,255,255,.4),0 7px 0 ${OL};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px" class="${cls}">${inner}<div class="lil ol-s" style="font-size:26px">${label}</div></div>`;
  return `<div class="screen" style="background:radial-gradient(circle at 50% 45%,#C77DFF 0,#6A2FE0 45%,#2A0F86 100%)">
  <div class="abs" style="inset:0;background:repeating-conic-gradient(from 0deg at 50% 46%,rgba(255,220,90,.35) 0 9deg,transparent 9deg 20deg);-webkit-mask:radial-gradient(circle at 50% 46%,#000 10%,transparent 70%)"></div>
  ${confetti(70, 540, 960)}
  ${ribbon(40, 60, 460, 'AWESOME!', '#FF5C8A', '#D92E6B', 60)}
  ${bigStar(50, 216, 130, -16)}${bigStar(180, 150, 180, 0)}${bigStar(360, 216, 130, 16)}
  ${sparkle(170, 150, 34)}${sparkle(360, 170, 26)}${sparkle(40, 200, 24, '#FFF3B0')}${sparkle(480, 330, 30)}
  <div class="abs" style="left:110px;top:350px;width:320px;height:320px;border-radius:50%;background:radial-gradient(rgba(255,255,255,.65),rgba(255,255,255,0) 65%)"></div>
  ${robot({ color: '#57D96A', accent: '#FF8A1F', head: 'round', eyes: 'happy', mouth: 'open', cheer: true, legs: 'rocket', body: 'barrel', arms: 'claw', antenna: 'ball', shadow: false, style: 'position:absolute;left:158px;top:338px;width:224px;transform:rotate(-4deg)' })}
  <div class="abs" style="left:24px;top:662px;width:146px;height:140px;border-radius:28px;border:5px solid ${OL};box-shadow:inset 0 -8px 0 rgba(0,0,0,.15),inset 0 6px 0 rgba(255,255,255,.4),0 7px 0 ${OL};display:flex;flex-direction:column;align-items:center;justify-content:center" class="x">
    <div class="abs" style="inset:0;border-radius:24px;background:linear-gradient(#FFE66B,#FFB21E)"></div>
    <div style="position:relative">${I.nut(70)}</div><div class="lil ol-s" style="position:relative;font-size:30px">+150</div></div>
  <div class="abs" style="left:197px;top:662px;width:146px;height:140px;border-radius:28px;border:5px solid ${OL};box-shadow:0 7px 0 ${OL};background:linear-gradient(#D7B4FF,#8B4FF0);display:flex;flex-direction:column;align-items:center;justify-content:center">
    ${I.crystal(70)}<div class="lil ol-s" style="font-size:30px">+5</div></div>
  <div class="abs" style="left:370px;top:662px;width:146px;height:140px;border-radius:28px;border:5px solid ${OL};box-shadow:0 7px 0 ${OL},0 0 30px 8px rgba(120,240,255,.8);background:linear-gradient(#9FF7FF,#2D9CFF);display:flex;flex-direction:column;align-items:center;justify-content:center;overflow:visible">
    ${robot({ only: ['legs'], color: '#FF5C8A', legs: 'rocket', shadow: false, viewBox: '44 196 112 70', style: 'width:96px' })}
    <div class="lil ol-s" style="font-size:22px;margin-top:6px">NEW PART!</div>
    <div class="badge lil" style="right:-12px;top:-14px;transform:rotate(14deg);font-size:15px;background:#FFD43B;color:${OL}">WOW</div></div>
  ${rbtn(40, 838, 'blue', I.replay(42), 78)}
  <div class="btn green lil" style="left:146px;top:830px;width:248px;height:94px;font-size:48px"><span class="ol">NEXT</span>${I.play(42)}</div>
  ${rbtn(422, 838, 'orange', I.home(42), 78)}
  </div>`;
}

// -------------------------------------------------------------- GARAGE (collection + paint)
function garage() {
  const bots = [
    { color: '#FF7A45' }, { color: '#38B6FF', head: 'square', eyes: 'visor', antenna: 'double', legs: 'treads' },
    { color: '#57D96A', eyes: 'cyclops', antenna: 'prop', legs: 'wheel', body: 'barrel' }, { color: '#FF6FB5', head: 'dome', eyes: 'hearts', body: 'tri', arms: 'claw' },
    { color: '#B46BFF', antenna: 'bolt', eyes: 'wink', mouth: 'grin', legs: 'rocket' }, { color: '#FFD43B', accent: '#FF5C8A', head: 'square', eyes: 'big', mouth: 'o', legs: 'treads' }
  ];
  const starsRow = n => `<div style="display:flex;justify-content:center;margin-top:-4px">${[0, 1, 2].map(i => I.star(22, i < n ? '#FFD43B' : '#8E8BB5')).join('')}</div>`;
  const cell = (i, b) => {
    const x = 22 + (i % 4) * 126, y = 584 + Math.floor(i / 4) * 170;
    const locked = !b;
    return `<div class="abs" style="left:${x}px;top:${y}px;width:116px;height:156px;border-radius:24px;border:4px solid ${OL};box-shadow:0 6px 0 ${OL};background:${locked ? 'linear-gradient(#5C5899,#3B3680)' : i === 0 ? 'linear-gradient(#FFF3A8,#FFC531)' : 'linear-gradient(#FFFFFF,#DCE7FF)'};padding-top:8px;${i === 0 ? 'box-shadow:0 6px 0 ' + OL + ',0 0 0 5px #fff,0 0 24px 6px rgba(255,255,255,.8)' : ''}">
      ${locked ? `${robot({ color: '#2A2660', accent: '#2A2660', metal: '#2A2660', eye: '#2A2660', shadow: false, style: 'width:96px;margin-left:6px;opacity:.9' })}<div class="abs lil ol" style="left:0;top:36px;width:100%;text-align:center;font-size:54px">?</div>`
        : robot({ ...b, shadow: false, style: 'width:88px;height:116px;margin-left:10px' }) + starsRow([3, 3, 2, 3, 1, 3][i])}</div>`;
  };
  const swatch = (i, c) => `<div class="abs" style="left:${44 + i * 66}px;top:486px;width:52px;height:52px;border-radius:50%;background:radial-gradient(circle at 35% 30%,#fff 0,${c} 30%);border:4px solid ${OL};${i === 0 ? 'box-shadow:0 0 0 5px #fff,0 0 0 9px ' + OL : 'box-shadow:0 4px 0 ' + OL}"></div>`;
  return `<div class="screen" style="background:linear-gradient(#36E0F5,#2B6CF0 55%,#2A2BB0)">
  <div class="abs" style="left:120px;top:60px;width:300px;height:420px;background:linear-gradient(rgba(255,255,255,.55),rgba(255,255,255,0));clip-path:polygon(38% 0,62% 0,100% 100%,0 100%)"></div>
  ${deco(10, 520, 460, [I.gear(26, '#9FF7FF'), I.star(18)])}
  ${rbtn(18, 18, 'orange', I.back(36), 60)}
  ${ribbon(100, 10, 340, 'MY ROBOTS', '#FFD43B', '#FF9A1F', 50)}
  ${pill(398, 112, I.nut(40), '1,250', 128).replace('font-size:20px', 'font-size:17px')}
  <div class="abs" style="left:130px;top:380px;width:280px;height:80px;border-radius:50%;background:radial-gradient(ellipse at 50% 40%,#FFF3B0,#FFB21E 60%,#C96A0A);border:5px solid ${OL};box-shadow:0 10px 0 ${OL}"></div>
  ${robot({ color: '#FF7A45', accent: '#FFD43B', eyes: 'big', mouth: 'open', wave: true, style: 'position:absolute;left:160px;top:118px;width:220px' })}
  <div class="abs lil ol-s" style="left:180px;top:442px;width:180px;height:44px;border-radius:22px;background:${OL};display:flex;align-items:center;justify-content:center;font-size:26px;color:#FFD43B">SPARKY</div>
  ${rbtn(30, 200, 'pink', I.paint(40), 66)}${rbtn(30, 284, 'purple', I.music(38), 66)}${rbtn(444, 200, 'green', I.share(38), 66)}${rbtn(444, 284, 'blue', I.star(40), 66)}
  ${['#FF7A45', '#38B6FF', '#57D96A', '#FF6FB5', '#B46BFF', '#FFD43B', '#22D3C5'].map((c, i) => swatch(i, c)).join('')}
  ${bots.map((b, i) => cell(i, b)).join('')}${cell(6, null)}${cell(7, null)}
  </div>`;
}

// -------------------------------------------------------------- SETTINGS
function settings() {
  const toggle = on => `<div class="abs" style="right:0;top:12px;width:100px;height:50px;border-radius:25px;border:4px solid ${OL};background:${on ? 'linear-gradient(#8DF56A,#2EC24A)' : 'linear-gradient(#C9CCE0,#9AA0BE)'};box-shadow:inset 0 4px 0 rgba(0,0,0,.15)">
      <div class="abs lil" style="${on ? 'left:12px' : 'right:10px'};top:9px;font-size:20px;color:#fff">${on ? 'ON' : 'OFF'}</div>
      <div class="abs" style="${on ? 'right:2px' : 'left:2px'};top:2px;width:38px;height:38px;border-radius:50%;background:linear-gradient(#fff,#E2E6F3);border:3px solid ${OL}"></div></div>`;
  const row = (y, cls, icon, label, on) => `<div class="abs" style="left:72px;top:${y}px;width:396px;height:74px">
      <div class="rbtn ${cls}" style="left:0;top:4px;width:62px;height:62px;border-radius:20px">${icon}</div>
      <div class="abs" style="left:80px;top:18px;font-size:28px;color:${OL}">${label}</div>${toggle(on)}</div>`;
  return `<div class="screen">
  <div class="abs" style="inset:0;filter:blur(6px) saturate(1.1)">${home()}</div>
  <div class="abs" style="inset:0;background:rgba(25,12,80,.55)"></div>
  <div class="panel" style="left:36px;top:130px;width:468px;height:760px"></div>
  ${ribbon(60, 82, 420, 'SETTINGS', '#38B6FF', '#1F6DFF', 52)}
  ${rbtn(452, 112, 'red', I.close(34), 62)}
  ${row(214, 'purple', I.music(38), 'Music', true)}
  ${row(298, 'blue', I.speaker(38), 'Sounds', true)}
  ${row(382, 'green', I.voice(38), 'Voice Helper', true)}
  ${row(466, 'orange', I.vibrate(38), 'Vibration', false)}
  <div class="abs" style="left:72px;top:556px;width:396px;border-top:4px dashed #E2C996"></div>
  <div class="btn yellow lil" style="left:72px;top:578px;width:396px;height:86px;font-size:32px;flex-direction:column;gap:0;border-radius:26px">
     <span class="ol-s">Rate Us!</span><div style="display:flex;gap:2px">${[0, 1, 2, 3, 4].map(() => I.star(26)).join('')}</div>
     <div class="abs" style="right:12px;top:12px">${I.heart(34)}</div></div>
  <div class="btn blue lil" style="left:72px;top:688px;width:190px;height:70px;font-size:26px;gap:8px;border-radius:24px">${I.share(28)}<span class="ol-s">Share</span></div>
  <div class="btn purple lil" style="left:278px;top:688px;width:190px;height:70px;font-size:24px;gap:6px;border-radius:24px">${I.pad(30)}<span class="ol-s">More Games</span></div>
  <div class="btn grey lil" style="left:72px;top:776px;width:190px;height:62px;font-size:22px;gap:8px;border-radius:22px">${I.shield(26)}<span class="ol-s">Privacy</span></div>
  <div class="btn grey lil" style="left:278px;top:776px;width:190px;height:62px;font-size:22px;gap:8px;border-radius:22px">${I.globe(26)}<span class="ol-s">English</span></div>
  <div class="abs" style="left:36px;top:906px;width:468px;display:flex;justify-content:center;align-items:center;gap:8px;font-size:17px;color:#fff;opacity:.9">${I.lock(22)}<span>Grown-up check before links &amp; purchases · v1.0</span></div>
  </div>`;
}

module.exports = { home, mapScreen, workshop, reward, garage, settings };
