"""Builds the 6 Play Store screenshot pages (1080x1920) from the app's screen mockups.

Usage: python3 build_screens.py <mockup project dir> <out dir>
"""
import re
import sys
from pathlib import Path

MOCK = Path(sys.argv[1])
OUT = Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)


def screen(name: str) -> str:
    html = (MOCK / f"{name}.dc.html").read_text()
    body = html.split("</helmet>", 1)[1].split("</x-dc>", 1)[0]
    return body.strip()


def trends() -> str:
    s = screen("Trends")
    sys_ = [138,141,136,140,142,137,139,135,141,138,136,134,137,133,131,135,130,132,129,133,128,131,127,130,126,129,131,127,126,128]
    dia = [88,90,87,89,91,88,89,86,90,88,87,85,87,84,83,86,82,84,81,84,80,83,80,82,79,81,83,80,79,82]
    y = lambda v: f"{170 - (v - 60) * 1.7:.1f}"
    x = lambda i: f"{30 + i * (328 / 29):.1f}"
    s = s.replace("{{sysPts}}", " ".join(f"{x(i)},{y(v)}" for i, v in enumerate(sys_)))
    s = s.replace("{{diaPts}}", " ".join(f"{x(i)},{y(v)}" for i, v in enumerate(dia)))
    return s


def insights() -> str:
    s = screen("Insights")
    cards = [
        ("#E7F4EC", "#14633F", "−8", "Systolic down 8 mmHg since starting Amlodipine", "Average 137 before 26 Sep, 129 since. Share this with your doctor."),
        ("#E8EEFC", "#1F4FD8", "9", "Evenings run 9 mmHg higher", "Morning average 127/81, evening average 136/87 over the last 30 days."),
        ("#FDF0D5", "#7A4D06", "+7", "Readings tagged “After coffee” are 7 higher", "Average 136/86 with this tag vs 129/82 without it."),
        ("#FDF0D5", "#7A4D06", "12", "Your arms differ by 12 mmHg", "A gap over 10 mmHg is worth mentioning at your next visit."),
        ("#E7F4EC", "#14633F", "7", "Measured on 7 of the last 7 days", "Great consistency. Regular readings make your averages more reliable."),
    ]
    out = []
    for bg, fg, badge, title, body in cards:
        out.append(
            f'<article style="background:#FFFFFF;border:1px solid #E2E7EE;border-radius:18px;padding:14px;display:flex;gap:12px">'
            f'<div style="width:40px;height:40px;border-radius:12px;background:{bg};color:{fg};display:flex;align-items:center;justify-content:center;flex-shrink:0;font-weight:800;font-size:13px">{badge}</div>'
            f'<div style="display:flex;flex-direction:column;gap:4px"><span style="font-size:15px;font-weight:800;line-height:1.3">{title}</span>'
            f'<span style="font-size:13px;font-weight:600;color:#3B4A5E;line-height:1.4">{body}</span></div></article>'
        )
    return re.sub(r"<sc-for.*?</sc-for>", "".join(out), s, flags=re.S)


CHECK = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>'
BELL = '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/></svg>'
PDF = '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/></svg>'
BULB = '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6M10 22h4M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2z"/></svg>'
SHIELD = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>'

SLIDES = [
    dict(file="01-scan", dark=True, screen=screen("Scan"), title="Snap your BP monitor", sub="The numbers fill in by themselves. Works with any digital blood pressure monitor.",
         stickers=[(f'<span style="color:#1E8E5A">{CHECK}</span><b>128 / 82</b><span>read in 1 sec</span>', "left:24px;top:1010px;transform:rotate(-5deg)"),
                   (f'<span style="color:#1F4FD8">{SHIELD}</span><b>Works offline</b>', "right:22px;top:1420px;transform:rotate(4deg)")]),
    dict(file="02-home", dark=False, screen=screen("Main"), title="Know what your numbers mean", sub="Colour-coded ranges based on AHA and ESC guidelines.",
         stickers=[('<span class="pill" style="background:#FDF0D5;color:#7A4D06">Elevated</span><span>120–129 / under 80</span>', "left:22px;top:1590px;transform:rotate(-4deg)")]),
    dict(file="03-trends", dark=True, screen=trends(), title="See your trends at a glance", sub="Averages, highs and lows, morning vs evening and time in range.",
         stickers=[('<span class="pill" style="background:#E7F4EC;color:#14633F">−8 mmHg</span><span>since new meds</span>', "right:22px;top:1600px;transform:rotate(4deg)")]),
    dict(file="04-insights", dark=False, screen=insights(), title="Spot patterns automatically", sub="Simple, private analysis of your own readings. No AI service, nothing uploaded.",
         stickers=[(f'<span style="color:#E0A526">{BULB}</span><b>5 new insights</b>', "right:24px;top:640px;transform:rotate(4deg)")]),
    dict(file="05-report", dark=True, screen=screen("Report"), title="A clean PDF for your doctor", sub="Charts, averages, medications and every reading. Share in one tap.",
         stickers=[(f'<span style="color:#C2272D">{PDF}</span><b>report.pdf</b><span>ready to share</span>', "left:22px;top:1120px;transform:rotate(-5deg)")]),
    dict(file="06-meds", dark=False, screen=screen("Meds"), title="Never miss a dose or reading", sub="Medication and measurement reminders, adherence and refill alerts.",
         stickers=[(f'<span style="color:#1F4FD8">{BELL}</span><b>8:00 PM</b><span>Evening reading</span>', "right:22px;top:1640px;transform:rotate(4deg)")]),
]

SCALE = 1.72
PAGE = """<!doctype html><html lang="en"><head><meta charset="utf-8"><title>{title}</title>
<style>
@font-face{{font-family:Manrope;src:url("fonts/manrope.ttf");font-weight:200 800}}
@font-face{{font-family:'Share Tech Mono';src:url("fonts/ShareTechMono.ttf")}}
html,body{{margin:0;width:1080px;height:1920px;overflow:hidden}}
body{{font-family:Manrope,sans-serif;position:relative;background:{bg};color:{ink}}}
a{{color:inherit;text-decoration:none}}
.ring{{position:absolute;border-radius:50%;border:3px solid {ring}}}
.head{{position:absolute;left:80px;right:80px;top:110px;text-align:center}}
.head h1{{margin:0;font-size:80px;line-height:1.08;font-weight:800;letter-spacing:-2px}}
.head p{{margin:26px auto 0;max-width:820px;font-size:36px;line-height:1.35;font-weight:600;opacity:.86}}
.phone{{position:absolute;left:50%;top:430px;transform:translateX(-50%);width:{pw}px;height:{ph}px;background:#0B1220;border-radius:84px;padding:16px;box-sizing:border-box;box-shadow:0 40px 90px rgba(10,20,60,.38)}}
.screen{{width:{sw}px;height:{sh}px;border-radius:70px;overflow:hidden;position:relative;background:#F4F6F8}}
.screen>.inner{{width:390px;height:844px;transform:scale({scale});transform-origin:0 0}}
.sticker{{position:absolute;background:#FFFFFF;color:#0F1B2D;border-radius:28px;padding:22px 28px;display:flex;align-items:center;gap:14px;font-size:30px;box-shadow:0 18px 40px rgba(10,20,60,.25);z-index:3}}
.sticker b{{font-weight:800}}
.sticker span{{font-weight:600;color:#556274}}
.sticker .pill{{font-weight:800;padding:6px 16px;border-radius:999px}}
.ecg{{position:absolute;left:0;right:0;top:1650px;opacity:{ecg}}}
</style></head><body>
<div class="ring" style="width:1300px;height:1300px;left:-110px;top:700px"></div>
<div class="ring" style="width:900px;height:900px;left:90px;top:900px"></div>
<svg class="ecg" width="1080" height="120" viewBox="0 0 1080 120"><polyline points="0,80 400,80 440,40 480,110 520,10 560,80 1080,80" fill="none" stroke="{ecgc}" stroke-width="5" stroke-linejoin="round"/></svg>
<div class="head"><h1>{title}</h1><p>{sub}</p></div>
<div class="phone"><div class="screen"><div class="inner">{screen}</div></div></div>
{stickers}
</body></html>"""

for sl in SLIDES:
    dark = sl["dark"]
    stickers = "".join(f'<div class="sticker" style="{pos}">{inner}</div>' for inner, pos in sl["stickers"])
    sw, sh = round(390 * SCALE), round(844 * SCALE)
    html = PAGE.format(
        title=sl["title"], sub=sl["sub"], screen=sl["screen"], stickers=stickers,
        bg="linear-gradient(160deg,#2F6BFF 0%,#1F4FD8 45%,#1638A8 100%)" if dark else "linear-gradient(160deg,#F2F6FF 0%,#E3EBFF 100%)",
        ink="#FFFFFF" if dark else "#0F1B2D",
        ring="rgba(255,255,255,.10)" if dark else "rgba(31,79,216,.08)",
        ecg=".22" if dark else ".5", ecgc="#FFFFFF" if dark else "#C9D6FF",
        pw=sw + 32, ph=sh + 32, sw=sw, sh=sh, scale=SCALE,
    )
    (OUT / f"{sl['file']}.html").write_text(html)
print("built", len(SLIDES))
