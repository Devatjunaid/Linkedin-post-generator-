// Art-kit sheet and the overview board that stitches the screens together.
const path = require('path');
const { OL, robot, CAST, I } = require('./lib');

const section = (x, y, w, h, title, inner, col = '#FF5C8A') => `<div class="panel" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px;border-radius:32px">
  <div class="abs lil ol-s" style="left:24px;top:-24px;padding:6px 20px;border-radius:16px;background:${col};border:4px solid ${OL};font-size:26px;color:#fff;box-shadow:0 5px 0 ${OL}">${title}</div>${inner}</div>`;
const label = (x, y, w, t) => `<div class="abs" style="left:${x}px;top:${y}px;width:${w}px;text-align:center;font-size:17px;color:#6B6894">${t}</div>`;

function artKit() {
  const friends = [{ ...CAST.sparky, pose: 'wave' }, CAST.nimbus, CAST.telly, CAST.rex, CAST.canny, { ...CAST.zippy, pose: 'cheer' }];
  const names = ['Sparky', 'Nimbus', 'Telly', 'Rex', 'Canny', 'Zippy'];
  const heads = [{ head: 'binocular', color: '#E8692A' }, { head: 'dome', color: '#E9EDF2', mood: 'wow' }, { head: 'crt', color: '#2BA3A0' },
    { head: 'cyclops', color: '#D9433B', eye: '#FF9A3C', mood: 'focus' }, { head: 'bucket', color: '#3567B5', mood: 'wink' }, { head: 'binocular', color: '#7C4DDB', mood: 'wow' }];
  const bodies = [{ body: 'box', color: '#F2B705' }, { body: 'barrel', color: '#2BA3A0' }, { body: 'sphere', color: '#E9EDF2', arms: 'bellows' }];
  const legs = ['legs', 'wheels', 'treads', 'spider', 'hover'];
  const ants = ['ball', 'double', 'dish', 'beacon', 'prop'];
  const sounds = [['Boing-clack!', 'a part snaps on'], ['Bleep-bloop', 'robot wakes up & talks'], ['Ding-ding-ding', 'nuts pour into the pile'],
    ['Ta-daaa!', 'drum roll + fanfare for stars'], ['Whoosh', 'rocket boots & screen swipes'], ['Chiptune loop', 'bouncy music, 3 world themes']];
  const palette = ['#E8692A', '#F2B705', '#2BA3A0', '#3567B5', '#D9433B', '#7C4DDB', '#E9EDF2', '#B9C0CC'];
  return `<div class="screen" style="width:1080px;height:1500px;background:#2A2470;background-image:linear-gradient(rgba(255,255,255,.05) 2px,transparent 2px),linear-gradient(90deg,rgba(255,255,255,.05) 2px,transparent 2px);background-size:40px 40px">
  <div class="abs lil ol" style="left:40px;top:28px;font-size:54px;color:#FFD43B">BOLT BUDDIES <span style="color:#5CE1FF">ART KIT</span></div>
  <div class="abs" style="left:640px;top:44px;font-size:20px;opacity:.85;width:400px;text-align:right">Painted metal, rivets, lens eyes and real treads. Every robot is built from mix-and-match parts, so kids can make hundreds of combos.</div>
  ${section(40, 150, 1000, 300, 'ROBOT FRIENDS', friends.map((f, i) => robot({ ...f, style: `position:absolute;left:${24 + i * 162}px;top:30px;width:150px` }) + label(24 + i * 162, 236, 150, `<b style="color:${OL};font-size:21px">${names[i]}</b>`)).join(''))}
  ${section(40, 500, 560, 460, 'HEADS', heads.map((h, i) => `<div class="abs" style="left:${26 + (i % 3) * 176}px;top:${36 + Math.floor(i / 3) * 206}px;width:160px;height:190px;border-radius:24px;background:#fff;border:4px solid #EADBB8;display:flex;align-items:center;justify-content:center">${robot({ ...h, antenna: ['ball', 'dish', 'double', 'beacon', 'prop', 'none'][i], only: ['antenna', 'head'], viewBox: '10 -14 180 156', style: 'width:140px' })}</div>`).join(''), '#38B6FF')}
  ${section(640, 500, 400, 140, 'BODIES + ARMS', bodies.map((b, i) => robot({ ...b, only: ['body', 'arms'], shadow: false, viewBox: '0 118 200 112', style: `position:absolute;left:${20 + i * 124}px;top:26px;width:118px` })).join(''), '#57D96A')}
  ${section(640, 680, 400, 120, 'LEGS & WHEELS', legs.map((l, i) => robot({ legs: l, color: '#3567B5', only: ['legs'], shadow: false, viewBox: '16 194 168 66', style: `position:absolute;left:${12 + i * 76}px;top:36px;width:72px` })).join(''), '#FF8A1F')}
  ${section(640, 840, 400, 120, 'TOPPERS', ants.map((a, i) => robot({ antenna: a, head: 'dome', accent: '#F2B705', only: ['antenna'], viewBox: '50 -10 100 56', style: `position:absolute;left:${12 + i * 76}px;top:32px;width:72px` })).join(''), '#B46BFF')}
  ${section(40, 1010, 1000, 220, 'UI KIT', `
    <div class="btn green lil" style="left:30px;top:44px;width:220px;height:80px;font-size:40px"><span class="ol">PLAY!</span>${I.play(34)}</div>
    <div class="btn yellow lil" style="left:30px;top:142px;width:220px;height:56px;font-size:26px"><span class="ol-s">Rate Us!</span></div>
    ${[['blue', I.gear(36)], ['pink', I.gift(36)], ['orange', I.home(34)], ['purple', I.music(32)], ['red', I.close(28)], ['green', I.check(32)]].map(([c, ic], i) => `<div class="rbtn ${c}" style="left:${284 + (i % 3) * 78}px;top:${40 + Math.floor(i / 3) * 80}px;width:64px;height:64px">${ic}</div>`).join('')}
    <div class="pill" style="left:540px;top:46px;width:150px;color:#fff"><span class="ic">${I.nut(44)}</span><span class="ol-s">1,250</span><span class="plus">+</span></div>
    <div class="pill" style="left:540px;top:110px;width:150px;color:#fff"><span class="ic">${I.crystal(44)}</span><span class="ol-s">48</span><span class="plus">+</span></div>
    ${[I.star(46), I.chest(46), I.lock(44), I.bulb(44), I.paint(44), I.heart(44), I.star(46, '#8E8BB5'), I.hand(46)].map((ic, i) => `<div class="abs" style="left:${730 + (i % 4) * 62}px;top:${44 + Math.floor(i / 4) * 74}px">${ic}</div>`).join('')}`, '#FFB21E')}
  ${section(40, 1280, 640, 190, 'SOUNDS', sounds.map(([s, d], i) => `<div class="abs" style="left:${24 + (i % 2) * 306}px;top:${34 + Math.floor(i / 2) * 50}px;display:flex;align-items:center;gap:10px">
      <div class="rbtn ${['blue', 'purple', 'yellow', 'pink', 'orange', 'green'][i]}" style="position:relative;width:42px;height:42px;border-radius:14px;border-width:3px">${I.speaker(24)}</div>
      <div style="font-size:19px;line-height:1.05">${s}<br><span style="font-size:15px;color:#6B6894;font-weight:500">${d}</span></div></div>`).join(''), '#7B45F0')}
  ${section(720, 1280, 320, 190, 'COLOURS', palette.map((c, i) => `<div class="abs" style="left:${26 + (i % 4) * 70}px;top:${36 + Math.floor(i / 4) * 70}px;width:56px;height:56px;border-radius:18px;background:${c};border:4px solid ${OL};box-shadow:inset 0 -6px 0 rgba(0,0,0,.15)"></div>`).join(''), '#22D3C5')}
  </div>`;
}

const SHOTS = [['01-home', 'Home'], ['02-adventure-map', 'Adventure Map'], ['03-robot-workshop', 'Build Workshop'],
  ['04-reward-stars', 'Stars & Rewards'], ['05-robot-garage', 'Robot Garage'], ['06-settings', 'Settings']];

function overview() {
  const dir = path.join(__dirname, '..');
  return `<div class="screen" style="width:1040px;height:1400px;background:radial-gradient(circle at 50% 0,#8B6CFF,#3A1E9E 60%,#1E1166)">
  <div class="abs lil ol" style="left:0;top:34px;width:100%;text-align:center;font-size:58px;color:#FFD43B">BOLT BUDDIES</div>
  <div class="abs ol-s" style="left:0;top:110px;width:100%;text-align:center;font-size:22px">Robot-building adventure for kids · screen concepts</div>
  ${SHOTS.map(([f, t], i) => {
    const x = 40 + (i % 3) * 330, y = 170 + Math.floor(i / 3) * 610;
    return `<img class="abs" src="file://${dir}/${f}.png" style="left:${x}px;top:${y}px;width:300px;height:533px;border-radius:28px;border:5px solid ${OL};box-shadow:0 10px 0 ${OL},0 20px 40px rgba(0,0,0,.4)">
      <div class="abs lil ol-s" style="left:${x}px;top:${y + 552}px;width:300px;text-align:center;font-size:26px">${i + 1}. ${t}</div>`;
  }).join('')}
  </div>`;
}

module.exports = { jobs: [['07-art-kit', artKit, [1080, 1500], 1.5], ['00-overview', overview, [1040, 1400], 1.5]] };
