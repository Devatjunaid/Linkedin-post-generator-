// After-build screens: launch pad, two flying worlds, and the crash → repair moment.
const { OL, robot, CAST, I, sparkle, confetti, rnd, starPath } = require('./lib');
const { pill, rbtn, ribbon, bigStar } = require('./screens');

const ROBOT = { ...CAST.junky };
const FLY_VB = '-10 -14 220 300';

// ---- extra props
const rocketIcon = (s = 40) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24"><path d="M12 1.5 Q18 6 17 15 H7 Q6 6 12 1.5Z" fill="#fff" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/><circle cx="12" cy="9" r="2.4" fill="#4FE3FF" stroke="${OL}" stroke-width="1.6"/><path d="M7 11 L3.5 16 L7 15Z M17 11 L20.5 16 L17 15Z" fill="#FF5C5C" stroke="${OL}" stroke-width="1.6" stroke-linejoin="round"/><path d="M9.5 15.5 Q12 23 14.5 15.5Z" fill="#FFB21E" stroke="${OL}" stroke-width="1.4"/></svg>`;
const wrench = (s = 40) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24"><path d="M14.5 3 a5.5 5.5 0 0 0 -5 7.3 L3 16.8 a2.3 2.3 0 0 0 3.2 3.2 L12.7 13.5 a5.5 5.5 0 0 0 7.3 -5 L17 11 L13.4 10.6 L13 7Z" fill="#fff" stroke="${OL}" stroke-width="2" stroke-linejoin="round"/></svg>`;
const battery = (lvl, s = 44) => `<svg width="${s}" height="${s * 0.6}" viewBox="0 0 40 24"><rect x="2" y="3" width="32" height="18" rx="5" fill="${lvl ? '#1E1B4B' : '#3A3670'}" stroke="${OL}" stroke-width="2.5"/>
  <rect x="34" y="8" width="4" height="8" rx="1.5" fill="#C3CAD5" stroke="${OL}" stroke-width="1.6"/>${lvl ? `<rect x="5" y="6" width="26" height="12" rx="3" fill="#5BE36A"/><rect x="7" y="7.5" width="22" height="3" rx="1.5" fill="#fff" opacity=".5"/>` : ''}</svg>`;

const cloud = (x, y, s = 1, op = 1) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${200 * s}px;opacity:${op}" viewBox="0 0 200 90">
  <path d="M30 80 Q4 80 8 60 Q10 42 34 44 Q36 16 66 18 Q84 2 108 14 Q130 2 146 22 Q176 18 178 44 Q198 50 192 68 Q190 82 170 80Z" fill="#fff"/>
  <path d="M30 80 Q12 80 12 68 Q60 76 110 70 Q160 66 190 70 Q190 82 170 80Z" fill="#D7E9FA"/></svg>`;

const comet = (x, y, s, ang) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${220 * s}px;height:${70 * s}px;transform:rotate(${ang}deg);transform-origin:85% 50%" viewBox="0 0 220 70">
  <defs><linearGradient id="ct${x}" x1="1" y1="0" x2="0" y2="0"><stop offset="0" stop-color="#FFF3A0"/><stop offset=".25" stop-color="#FF9A1F" stop-opacity=".9"/><stop offset="1" stop-color="#FF4F2A" stop-opacity="0"/></linearGradient>
  <radialGradient id="cr${x}" cx=".35" cy=".3"><stop offset="0" stop-color="#B9A592"/><stop offset="1" stop-color="#4A3A33"/></radialGradient></defs>
  <path d="M190 18 Q100 20 4 35 Q100 50 190 52Z" fill="url(#ct${x})"/>
  <path d="M168 20 L186 12 L204 22 L210 40 L198 56 L178 58 L164 46Z" fill="url(#cr${x})" stroke="${OL}" stroke-width="3" stroke-linejoin="round"/>
  <circle cx="186" cy="30" r="5" fill="#3A2C26" opacity=".7"/><circle cx="196" cy="44" r="3.5" fill="#3A2C26" opacity=".7"/><circle cx="176" cy="44" r="3" fill="#3A2C26" opacity=".6"/></svg>`;

const ufo = (x, y, s = 1) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${150 * s}px" viewBox="0 0 150 130">
  <defs><linearGradient id="ub${x}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#F2F4F8"/><stop offset=".5" stop-color="#A7AFBD"/><stop offset="1" stop-color="#4E5463"/></linearGradient>
  <linearGradient id="beam${x}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#B8FF7A" stop-opacity=".7"/><stop offset="1" stop-color="#B8FF7A" stop-opacity="0"/></linearGradient></defs>
  <path d="M55 58 L25 128 H125 L95 58Z" fill="url(#beam${x})"/>
  <ellipse cx="75" cy="34" rx="28" ry="24" fill="#8CF5FF" stroke="${OL}" stroke-width="3" opacity=".9"/><ellipse cx="66" cy="24" rx="9" ry="5" fill="#fff" opacity=".8" transform="rotate(-25 66 24)"/>
  <circle cx="75" cy="38" r="9" fill="#7CE35A" stroke="${OL}" stroke-width="2.5"/><circle cx="72" cy="36" r="2.6" fill="${OL}"/><circle cx="79" cy="36" r="2.6" fill="${OL}"/>
  <ellipse cx="75" cy="50" rx="70" ry="16" fill="url(#ub${x})" stroke="${OL}" stroke-width="3.5"/>
  ${[22, 48, 75, 102, 128].map((cx, i) => `<circle cx="${cx}" cy="${i % 4 ? 56 : 52}" r="4.5" fill="${['#FF5C5C', '#FFD43B', '#5CE1FF', '#FFD43B', '#FF5C5C'][i]}" stroke="${OL}" stroke-width="1.6"/>`).join('')}</svg>`;

const balloon = (x, y, s, c1, c2) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${110 * s}px" viewBox="0 0 110 160">
  <defs><clipPath id="bc${x}"><path d="M55 4 C100 4 108 46 96 74 C88 92 70 102 66 112 H44 C40 102 22 92 14 74 C2 46 10 4 55 4Z"/></clipPath>
  <radialGradient id="bs${x}" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#fff" stop-opacity=".45"/><stop offset="1" stop-color="#000" stop-opacity=".25"/></radialGradient></defs>
  <g clip-path="url(#bc${x})"><rect width="110" height="120" fill="${c1}"/>${[10, 42, 74].map(sx => `<path d="M${sx} 0 Q${sx + 8} 60 ${sx + 4} 120 H${sx + 22} Q${sx + 28} 60 ${sx + 22} 0Z" fill="${c2}"/>`).join('')}<rect width="110" height="120" fill="url(#bs${x})"/></g>
  <path d="M55 4 C100 4 108 46 96 74 C88 92 70 102 66 112 H44 C40 102 22 92 14 74 C2 46 10 4 55 4Z" fill="none" stroke="${OL}" stroke-width="3.5"/>
  <path d="M46 112 L44 132 M64 112 L66 132" stroke="${OL}" stroke-width="2"/><rect x="40" y="130" width="30" height="22" rx="4" fill="#B97A3E" stroke="${OL}" stroke-width="3"/><path d="M40 138 H70" stroke="#8A5427" stroke-width="2"/></svg>`;

const ring = (x, y, w, glow = true) => `<svg class="abs" style="left:${x}px;top:${y}px;width:${w}px;height:${w * 0.42}px;${glow ? 'filter:drop-shadow(0 0 10px rgba(255,220,80,.9))' : ''}" viewBox="0 0 120 50">
  <ellipse cx="60" cy="25" rx="54" ry="18" fill="none" stroke="${OL}" stroke-width="13"/><ellipse cx="60" cy="25" rx="54" ry="18" fill="none" stroke="#FFC928" stroke-width="7"/>
  <path d="M14 20 Q30 8 60 7" fill="none" stroke="#FFF6B0" stroke-width="2.6" stroke-linecap="round"/></svg>`;

const orb = (x, y, s, col, inner) => `<div class="abs" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px;border-radius:50%;border:4px solid ${OL};
  background:radial-gradient(circle at 35% 30%,#fff 0,${col} 45%,${col} 70%);box-shadow:0 0 22px 8px ${col}99;display:flex;align-items:center;justify-content:center">${inner}</div>`;

const glowStar = (x, y, s) => `<div class="abs" style="left:${x}px;top:${y}px;filter:drop-shadow(0 0 8px rgba(255,220,80,.95))">${I.star(s)}</div>`;

const shield = (x, y, s) => `<div class="abs" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px;border-radius:50%;border:3px solid rgba(170,250,255,.95);
  background:radial-gradient(circle at 35% 28%,rgba(255,255,255,.55) 0,rgba(140,240,255,.12) 30%,rgba(90,210,255,.08) 60%,rgba(90,210,255,.4) 100%);box-shadow:0 0 30px 6px rgba(120,240,255,.6),inset 0 0 26px rgba(150,245,255,.6)"></div>`;

const flyHud = (stars, lives, prog) => `
  ${rbtn(18, 18, 'blue', I.pause(32), 58)}
  ${pill(98, 28, I.star(44), stars, 110).replace('<span class="plus">+</span>', '')}
  <div class="abs" style="left:232px;top:30px;display:flex;gap:4px">${[0, 1, 2].map(i => battery(i < lives)).join('')}</div>
  <div class="abs" style="left:488px;top:120px;width:22px;height:560px;border-radius:11px;background:rgba(20,16,60,.55);border:3px solid ${OL}">
    <div class="abs" style="left:0;bottom:0;width:100%;height:${prog}%;border-radius:8px;background:linear-gradient(#8DF56A,#2EC24A)"></div></div>
  <div class="abs" style="left:478px;top:94px;width:44px;height:30px">${flag()}</div>
  <div class="abs" style="left:475px;top:${120 + 560 * (1 - prog / 100) - 22}px;width:48px;height:48px;border-radius:50%;background:#fff;border:3px solid ${OL};display:flex;align-items:center;justify-content:center">
    ${robot({ ...ROBOT, only: ['antenna', 'head'], viewBox: '18 -14 164 156', style: 'width:36px' })}</div>`;
const flag = () => `<svg width="44" height="34" viewBox="0 0 44 34"><path d="M12 32 V3" stroke="${OL}" stroke-width="4" stroke-linecap="round"/><path d="M13 4 H36 L31 11 L36 18 H13Z" fill="#FF5C8A" stroke="${OL}" stroke-width="2.6" stroke-linejoin="round"/></svg>`;
const popup = (x, y, t, col = '#FFD43B') => `<div class="abs lil ol-s" style="left:${x}px;top:${y}px;font-size:30px;color:${col};transform:rotate(-8deg)">${t}</div>`;

// -------------------------------------------------------------- LAUNCH PAD
function launch() {
  let stars = ''; for (let i = 0; i < 50; i++) stars += `<div class="abs" style="left:${rnd() * 540}px;top:${rnd() * 380}px;width:${2 + rnd() * 3}px;height:${2 + rnd() * 3}px;border-radius:50%;background:#fff;opacity:${0.4 + rnd() * 0.6}"></div>`;
  const tower = x => `<svg class="abs" style="left:${x}px;top:330px;width:90px;height:420px" viewBox="0 0 90 420">
    <rect x="20" y="20" width="50" height="400" fill="none" stroke="#3A2F6E" stroke-width="8"/>
    ${Array.from({ length: 10 }, (_, i) => `<path d="M20 ${20 + i * 40} L70 ${60 + i * 40} M70 ${20 + i * 40} L20 ${60 + i * 40}" stroke="#3A2F6E" stroke-width="5"/>`).join('')}
    <rect x="12" y="8" width="66" height="16" rx="4" fill="#4A3D86"/><circle cx="45" cy="0" r="9" fill="#FF4040" stroke="${OL}" stroke-width="3"/><circle cx="45" cy="0" r="24" fill="#FF4040" opacity=".3"/></svg>`;
  const smoke = (x, y, s) => `<div class="abs" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px;border-radius:50%;background:radial-gradient(circle at 40% 35%,#fff,#D9DCEB 60%,#A7ABC6);border:3px solid rgba(30,27,75,.25)"></div>`;
  return `<div class="screen" style="background:linear-gradient(#140E45 0%,#3B228F 35%,#8A4FD8 58%,#FF9A6A 74%,#FFC77A 82%)">
  ${stars}
  <div class="abs" style="left:380px;top:120px;width:90px;height:90px;border-radius:50%;background:radial-gradient(circle at 35% 30%,#fff,#E3E6F5 50%,#A9AED0);box-shadow:0 0 50px 10px rgba(255,255,255,.35)"></div>
  <svg class="abs" style="left:0;top:560px" width="540" height="200" viewBox="0 0 540 200"><path d="M0 120 L40 80 L60 100 L90 50 L120 90 L150 70 L190 110 L230 60 L260 95 L300 40 L330 85 L370 65 L400 100 L440 55 L480 95 L540 70 V200 H0Z" fill="#2A1A6B"/>
    ${[100, 300, 440].map(x => `<rect x="${x}" y="${100}" width="10" height="10" fill="#FFD86B"/>`).join('')}</svg>
  ${tower(10)}${tower(440)}
  <div class="abs" style="left:0;top:740px;width:540px;height:220px;background:linear-gradient(#3B2A7A,#1E1452)"></div>
  <div class="abs" style="left:70px;top:700px;width:400px;height:80px;border-radius:50%;background:repeating-conic-gradient(from 0deg at 50% 50%,#F2B705 0 15deg,${OL} 15deg 30deg);border:5px solid ${OL};box-shadow:0 10px 0 ${OL}"></div>
  <div class="abs" style="left:120px;top:712px;width:300px;height:56px;border-radius:50%;background:radial-gradient(ellipse,#C9CED8,#7D8596);border:4px solid ${OL}"></div>
  <div class="abs" style="left:170px;top:720px;width:200px;height:40px;border-radius:50%;background:radial-gradient(ellipse,rgba(255,200,90,.95),rgba(255,140,40,.4) 60%,transparent 70%)"></div>
  ${robot({ ...ROBOT, flame: true, pose: 'cheer', shadow: false, style: 'position:absolute;left:150px;top:410px;width:240px' })}
  ${smoke(60, 730, 90)}${smoke(130, 760, 110)}${smoke(330, 755, 110)}${smoke(400, 725, 95)}${smoke(-20, 770, 100)}${smoke(460, 770, 100)}${smoke(220, 790, 100)}
  ${ribbon(70, 90, 400, 'GET READY!', '#38B6FF', '#1F6DFF', 50)}
  <div class="abs lil" style="left:0;top:170px;width:540px;text-align:center;font-size:200px;line-height:1;color:#FFD43B;
     text-shadow:0 8px 0 ${OL},6px 0 0 ${OL},-6px 0 0 ${OL},0 -6px 0 ${OL},5px 5px 0 ${OL},-5px 5px 0 ${OL},5px -5px 0 ${OL},-5px -5px 0 ${OL},0 14px 0 ${OL},0 0 40px rgba(255,220,80,.9)">3</div>
  ${sparkle(110, 210, 30)}${sparkle(410, 300, 26)}${sparkle(160, 360, 20, '#FFF3B0')}
  <div class="btn orange lil" style="left:90px;top:836px;width:360px;height:100px;font-size:52px;border-radius:36px">${rocketIcon(54)}<span class="ol">LAUNCH!</span></div>
  <div class="abs" style="left:392px;top:880px;transform:rotate(-18deg)">${I.hand(66)}</div>
  </div>`;
}

// -------------------------------------------------------------- SPACE FLIGHT
function spaceFlight() {
  let stars = ''; for (let i = 0; i < 110; i++) stars += `<div class="abs" style="left:${rnd() * 540}px;top:${rnd() * 960}px;width:${1.5 + rnd() * 3}px;height:${1.5 + rnd() * 3}px;border-radius:50%;background:#fff;opacity:${0.3 + rnd() * 0.7}"></div>`;
  let streaks = ''; for (let i = 0; i < 14; i++) streaks += `<div class="abs" style="left:${rnd() * 540}px;top:${rnd() * 960}px;width:2px;height:${40 + rnd() * 70}px;background:linear-gradient(rgba(255,255,255,0),rgba(255,255,255,.55));border-radius:2px"></div>`;
  const arc = [[250, 470], [218, 410], [206, 346], [218, 282], [250, 222], [290, 172]];
  return `<div class="screen" style="background:linear-gradient(#050826,#140B47 55%,#2A0F5C)">
  <div class="abs" style="left:-120px;top:120px;width:420px;height:300px;border-radius:50%;background:radial-gradient(rgba(255,90,200,.35),transparent 65%)"></div>
  <div class="abs" style="left:250px;top:420px;width:420px;height:340px;border-radius:50%;background:radial-gradient(rgba(60,220,255,.28),transparent 65%)"></div>
  ${stars}${streaks}
  <div class="abs" style="left:300px;top:740px;width:340px;height:340px;border-radius:50%;background:radial-gradient(circle at 30% 25%,#FFB67A,#E8692A 45%,#7A2A12);border:5px solid ${OL};box-shadow:inset -30px -30px 60px rgba(0,0,0,.35)"></div>
  <div class="abs" style="left:250px;top:850px;width:440px;height:110px;border-radius:50%;border:12px solid #FFD43B;transform:rotate(-14deg);border-bottom-color:transparent;border-left-color:#E8B20A"></div>
  <div class="abs" style="left:40px;top:150px;width:60px;height:60px;border-radius:50%;background:radial-gradient(circle at 35% 30%,#CFF7FF,#4FB7E3 55%,#1B5A8A);border:4px solid ${OL}"></div>
  ${ufo(24, 260, 1)}
  ${comet(250, 120, 0.9, -28)}${comet(-60, 600, 0.75, 20)}
  <svg class="abs" style="left:380px;top:420px;width:60px" viewBox="0 0 60 60"><path d="M10 20 L28 6 L50 14 L56 36 L40 54 L16 50 L4 34Z" fill="#7A6A60" stroke="${OL}" stroke-width="4" stroke-linejoin="round"/><circle cx="30" cy="26" r="6" fill="#4A3A33"/><circle cx="40" cy="40" r="4" fill="#4A3A33"/></svg>
  ${arc.map(([x, y], i) => glowStar(x, y, i === 0 ? 44 : 50)).join('')}
  ${popup(300, 500, '+1')}
  ${orb(116, 470, 70, '#38B6FF', I.shield(40))}
  ${orb(390, 300, 62, '#B46BFF', `<span class="lil ol-s" style="font-size:24px">x2</span>`)}
  ${shield(152, 556, 236)}
  ${robot({ ...ROBOT, flame: 'big', pose: 'cheer', mood: 'wow', shadow: false, viewBox: FLY_VB, style: 'position:absolute;left:176px;top:570px;width:190px;transform:rotate(-8deg)' })}
  ${sparkle(150, 840, 18)}${sparkle(330, 880, 14)}${sparkle(240, 900, 22, '#FFF3B0')}
  ${flyHud('18', 2, 62)}
  <div class="abs" style="left:24px;top:880px;display:flex;align-items:center;gap:10px;padding:8px 16px;border-radius:20px;background:rgba(20,16,60,.6);border:3px solid ${OL}">
    <span class="lil ol-s" style="font-size:22px">◀ Drag to fly ▶</span></div>
  </div>`;
}

// -------------------------------------------------------------- SKY FLIGHT
function skyFlight() {
  return `<div class="screen" style="background:linear-gradient(#4FB9F5 0%,#8FD6FF 45%,#CDEFFF 75%,#FFF2D6 100%)">
  <div class="abs" style="left:380px;top:60px;width:260px;height:260px;border-radius:50%;background:radial-gradient(#FFF7C2 0,#FFE066 25%,rgba(255,224,102,.35) 40%,transparent 62%)"></div>
  <div class="abs" style="left:-140px;top:420px;width:820px;height:820px;border-radius:50%;
     background:radial-gradient(circle,transparent 54%,rgba(255,92,92,.55) 54%,rgba(255,92,92,.55) 57%,rgba(255,170,60,.55) 57%,rgba(255,170,60,.55) 60%,rgba(255,230,80,.55) 60%,rgba(255,230,80,.55) 63%,rgba(100,220,120,.55) 63%,rgba(100,220,120,.55) 66%,rgba(80,170,255,.55) 66%,rgba(80,170,255,.55) 69%,rgba(150,110,240,.5) 69%,rgba(150,110,240,.5) 72%,transparent 72%)"></div>
  ${cloud(-40, 140, 1.1, 0.9)}${cloud(330, 330, 0.9, 0.95)}${cloud(-30, 620, 1.3)}${cloud(300, 760, 1.4)}${cloud(150, 40, 0.7, 0.7)}
  <svg class="abs" style="left:0;top:860px" width="540" height="100" viewBox="0 0 540 100"><path d="M0 40 Q70 0 140 30 T280 30 T420 20 T540 30 V100 H0Z" fill="#7BD66A"/><path d="M0 70 Q90 40 180 62 T360 60 T540 55 V100 H0Z" fill="#4FBF5A"/></svg>
  ${balloon(16, 250, 0.95, '#FF5C8A', '#FFD43B')}${balloon(400, 520, 0.8, '#38B6FF', '#fff')}
  <svg class="abs" style="left:70px;top:700px;width:90px" viewBox="0 0 90 70"><path d="M4 30 L86 6 L60 64 L42 40Z" fill="#fff" stroke="${OL}" stroke-width="3.5" stroke-linejoin="round"/><path d="M86 6 L42 40 L40 58" fill="none" stroke="${OL}" stroke-width="3"/></svg>
  ${ring(200, 120, 150)}${ring(214, 270, 130)}${ring(230, 400, 112)}
  ${glowStar(256, 152, 40)}${glowStar(262, 298, 36)}${glowStar(150, 420, 40)}${glowStar(350, 650, 40)}
  <div class="abs lil" style="left:40px;top:470px;padding:6px 18px;border-radius:20px;background:#FF5C8A;border:4px solid ${OL};font-size:28px;transform:rotate(8deg);box-shadow:0 5px 0 ${OL}"><span class="ol-s">x3 COMBO!</span></div>
  ${robot({ ...ROBOT, color: '#E8692A', flame: 'big', pose: 'cheer', shadow: false, viewBox: FLY_VB, style: 'position:absolute;left:170px;top:530px;width:200px;transform:rotate(6deg)' })}
  <svg class="abs" style="left:130px;top:520px;width:280px;height:300px" viewBox="0 0 280 300">${[[30, 60], [250, 80], [20, 160], [262, 170]].map(([x, y]) => `<path d="M${x} ${y} v40" stroke="#fff" stroke-width="5" stroke-linecap="round" opacity=".8"/>`).join('')}</svg>
  ${flyHud('27', 3, 38)}
  </div>`;
}

// -------------------------------------------------------------- CRASH → REPAIR
function repair() {
  const puff = (x, y, s) => `<div class="abs" style="left:${x}px;top:${y}px;width:${s}px;height:${s}px;border-radius:50%;background:radial-gradient(circle at 40% 35%,#F2F3F8,#A9ADC4);opacity:.9"></div>`;
  return `<div class="screen" style="background:linear-gradient(#FFC867,#FF8A5C)">
  <div class="abs" style="inset:0;background-image:radial-gradient(circle,rgba(120,40,0,.22) 3px,transparent 3.5px);background-size:28px 28px"></div>
  <div class="abs" style="left:0;top:690px;width:540px;height:270px;background:linear-gradient(#8A5A3C,#5E3B26);border-top:6px solid ${OL}"></div>
  <div class="abs" style="left:0;top:690px;width:540px;height:22px;background:repeating-linear-gradient(90deg,#9C6A48 0 60px,#8A5A3C 60px 62px)"></div>
  ${rbtn(18, 18, 'orange', I.home(36), 58)}${pill(398, 28, I.nut(44), '1,250', 126)}
  ${ribbon(70, 84, 400, 'OOPS!', '#FF5C5C', '#D63A3A', 58)}
  <div class="abs" style="left:60px;top:200px;width:420px;padding:12px 18px;background:#fff;border:4px solid ${OL};border-radius:24px;color:${OL};font-size:24px;text-align:center;box-shadow:0 6px 0 ${OL}">A comet bumped Junky's arm off!</div>
  ${puff(300, 330, 44)}${puff(326, 306, 32)}${puff(282, 312, 26)}
  ${robot({ ...ROBOT, missing: 'armR', mood: 'focus', style: 'position:absolute;left:110px;top:316px;width:250px' })}
  <div class="abs" style="left:376px;top:600px;transform:rotate(-70deg);filter:drop-shadow(0 6px 0 rgba(0,0,0,.2))">
    ${robot({ ...ROBOT, only: ['arms'], pose: 'down', viewBox: '138 132 52 104', style: 'width:64px' })}</div>
  <svg class="abs" style="left:330px;top:440px;width:150px;height:200px" viewBox="0 0 150 200"><path d="M90 180 Q140 100 30 40" fill="none" stroke="#fff" stroke-width="6" stroke-dasharray="2 14" stroke-linecap="round"/><path d="M44 30 L26 38 L40 54" fill="none" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/></svg>
  <div class="abs" style="left:440px;top:640px;transform:rotate(-20deg)">${I.hand(60)}</div>
  ${sparkle(120, 400, 22)}${sparkle(400, 360, 18)}
  <div class="panel" style="left:60px;top:730px;width:420px;height:84px;display:flex;align-items:center;justify-content:center;gap:14px;font-size:24px">
    <span>Stars you kept:</span>${I.star(40)}${I.star(40)}${I.star(40, '#8E8BB5')}</div>
  <div class="btn green lil" style="left:110px;top:848px;width:320px;height:90px;font-size:46px">${wrench(46)}<span class="ol">FIX IT!</span></div>
  </div>`;
}

module.exports = { launch, spaceFlight, skyFlight, repair };
