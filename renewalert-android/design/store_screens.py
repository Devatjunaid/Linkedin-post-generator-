#!/usr/bin/env python3
"""Builds the Play Store screenshot pages (1080x1920) as HTML.

Render with headless Chromium (see design/render_store.sh). Service names are
generic on purpose: Play listings must not show other companies' trademarks.
"""
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "store_html")

CSS = """
*{box-sizing:border-box;margin:0;padding:0}
body{width:1080px;height:1920px;overflow:hidden;font-family:'DM Sans',sans-serif;color:#15171C;
  background:linear-gradient(160deg,#0B1026 0%,#1B1F55 55%,#3A2A8F 100%);position:relative}
.orb{position:absolute;border-radius:50%;filter:blur(2px)}
.head{position:absolute;top:110px;left:0;right:0;text-align:center;padding:0 70px;color:#fff}
.kicker{display:inline-block;font:700 26px 'DM Sans';letter-spacing:.14em;text-transform:uppercase;color:#FFB547;
  background:rgba(255,181,71,.12);border:1px solid rgba(255,181,71,.35);padding:10px 22px;border-radius:999px;margin-bottom:26px}
h1{font:700 78px/1.05 'Space Grotesk';letter-spacing:-2px}
h1 em{font-style:normal;background:linear-gradient(90deg,#8FA6FF,#C9A8FF);-webkit-background-clip:text;color:transparent}
.sub{margin-top:22px;font:400 32px/1.35 'DM Sans';color:#C9CCF2}
.phone{position:absolute;left:50%;top:560px;width:820px;height:1700px;margin-left:-410px;border-radius:96px;
  background:#05070F;padding:22px;box-shadow:0 40px 120px rgba(0,0,0,.55),0 0 0 3px #2B2F45 inset}
.screen{width:776px;height:1660px;border-radius:76px;overflow:hidden;background:#F3F4F0;position:relative}
.ui{width:390px;transform:scale(1.99);transform-origin:0 0;position:absolute;top:0;left:0}
.status{height:34px;display:flex;align-items:center;justify-content:space-between;padding:0 26px;font:600 13px 'DM Sans'}
.notch{position:absolute;top:10px;left:50%;margin-left:-45px;width:90px;height:24px;border-radius:14px;background:#05070F;z-index:5}
.card{background:#fff;border:1px solid #E2E4DE;border-radius:20px}
.row{display:flex;align-items:center;gap:12px}
.tile{width:40px;height:40px;border-radius:12px;display:flex;align-items:center;justify-content:center;font:700 16px 'DM Sans';flex-shrink:0}
.money{font:600 15px 'Space Grotesk'}
.muted{color:#5B616B}
.pill{font:600 11px 'DM Sans';padding:3px 9px;border-radius:999px}
.badge{position:absolute;z-index:6;background:#fff;border-radius:22px;box-shadow:0 24px 60px rgba(0,0,0,.35);padding:22px 26px;
  display:flex;align-items:center;gap:18px;font:600 28px 'DM Sans'}
.badge .ic{width:64px;height:64px;border-radius:18px;display:flex;align-items:center;justify-content:center;font:700 30px 'Space Grotesk';color:#fff}
"""

FONTS = '<link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap" rel="stylesheet">'

C = {
    "video": ("#F0E8FB", "#7A3FCB"), "music": ("#DFF3F1", "#0F7F77"), "prod": ("#E6ECFD", "#2A55E5"),
    "cloud": ("#E3F0FB", "#1F6FB2"), "health": ("#FDEEDF", "#C2560C"), "news": ("#ECEDEA", "#4E545E"),
    "edu": ("#E1F2E8", "#1B7A4B"), "shop": ("#FFF4D6", "#8A6100"),
}


def tile(letter, cat, size=40):
    bg, fg = C[cat]
    return (f'<span class="tile" style="width:{size}px;height:{size}px;background:{bg};color:{fg};'
            f'font-size:{int(size * 0.4)}px">{letter}</span>')


def status():
    return ('<div class="notch"></div><div class="status"><span>9:41</span>'
            '<span style="display:flex;gap:6px;align-items:center">'
            '<svg width="17" height="11" viewBox="0 0 17 11"><rect x="0" y="7" width="3" height="4" rx="1" fill="#15171C"/>'
            '<rect x="4.5" y="5" width="3" height="6" rx="1" fill="#15171C"/><rect x="9" y="2.5" width="3" height="8.5" rx="1" fill="#15171C"/>'
            '<rect x="13.5" y="0" width="3" height="11" rx="1" fill="#15171C"/></svg>'
            '<svg width="24" height="12" viewBox="0 0 24 12"><rect x=".5" y=".5" width="20" height="11" rx="3" fill="none" stroke="#15171C"/>'
            '<rect x="2" y="2" width="15" height="8" rx="2" fill="#15171C"/><rect x="21.5" y="4" width="2" height="4" rx="1" fill="#15171C"/></svg>'
            '</span></div>')


def nav(active):
    items = [("Home", '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>'),
             ("Subs", '<path d="M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01"/>'),
             ("Calendar", '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/>'),
             ("Insights", '<path d="M3 20h18M6 16v-5M11 16V6M16 16v-8M21 16v-3"/>'),
             ("Settings", '<path d="M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1"/><circle cx="15" cy="6" r="2"/><circle cx="9" cy="12" r="2"/><circle cx="17" cy="18" r="2"/>')]
    out = ['<div style="position:absolute;left:0;right:0;top:758px;height:76px;background:#fff;border-top:1px solid #E2E4DE;display:flex;justify-content:space-around;align-items:center">']
    for name, path in items:
        on = name == active
        col = "#3355EE" if on else "#5B616B"
        bg = "background:#E6ECFD;" if on else ""
        out.append(f'<div style="display:flex;flex-direction:column;align-items:center;gap:4px;font:{"700" if on else "600"} 11px DM Sans;color:{col}">'
                   f'<span style="{bg}border-radius:999px;padding:4px 16px;display:flex"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" '
                   f'stroke="{col}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{path}</svg></span>{name}</div>')
    out.append("</div>")
    return "".join(out)


def upcoming_row(letter, cat, name, when, price, first=False):
    border = "" if first else "border-top:1px solid #EEEFEB;"
    return (f'<div class="row" style="padding:10px 0;{border}">{tile(letter, cat)}'
            f'<div style="flex:1"><div style="font:600 15px DM Sans">{name}</div><div class="muted" style="font-size:12px">{when}</div></div>'
            f'<span class="money">{price}</span></div>')


def screen_home():
    return f"""{status()}
<div style="padding:10px 20px 0;display:flex;flex-direction:column;gap:14px">
 <div class="row"><div style="flex:1"><div class="muted" style="font-size:13px">Tue, Oct 13</div>
   <div style="font:700 28px 'Space Grotesk';letter-spacing:-.5px">Overview</div></div>
   <span class="card" style="width:44px;height:44px;border-radius:12px;display:flex;align-items:center;justify-content:center">
   <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#15171C" stroke-width="2"><path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10 21a2 2 0 0 0 4 0"/></svg></span></div>
 <div style="background:linear-gradient(135deg,#15171C,#232845);color:#fff;border-radius:24px;padding:20px;display:flex;flex-direction:column;gap:12px">
   <div class="row" style="justify-content:space-between"><span style="font-size:13px;color:#C9CCD2">Monthly spend</span>
   <span style="font:600 12px DM Sans;color:#C9CCD2;border:1px solid #3A3E47;border-radius:999px;padding:5px 12px">USD · Monthly ⇄</span></div>
   <div style="font:700 46px 'Space Grotesk';letter-spacing:-1.5px">$169.03</div>
   <div style="font-size:13px;color:#C9CCD2">≈ $2,028.36 per year · 9 active · 1 trial</div>
   <div style="height:8px;border-radius:999px;background:#2C3039;display:flex;overflow:hidden"><span style="width:88.7%;background:#5C7BFF"></span><span style="width:11.3%;background:#FF8A3D"></span></div>
   <div class="row" style="justify-content:space-between;font-size:12px;color:#C9CCD2"><span>Budget $150.00</span><span style="color:#FFB547;font-weight:600">$19.03 over</span></div>
 </div>
 <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
   <div class="card" style="border-radius:16px;padding:14px"><div class="muted" style="font:600 12px DM Sans">Due next 7 days</div>
   <div style="font:700 22px 'Space Grotesk'">$27.48</div><div class="muted" style="font-size:12px">2 charges</div></div>
   <div style="background:#FDEEDF;border:1px solid #F5D4B5;border-radius:16px;padding:14px"><div style="font:600 12px DM Sans;color:#8A3D08">Trial ending</div>
   <div style="font:700 22px 'Space Grotesk'">3 days</div><div style="font-size:12px;color:#8A3D08">Language course</div></div>
 </div>
 <div class="card" style="padding:6px 16px 8px">
   <div class="row" style="justify-content:space-between;padding:10px 0 4px"><b style="font-size:16px">Upcoming</b><span style="color:#3355EE;font:600 13px DM Sans">See all</span></div>
   {upcoming_row("V", "video", "Video streaming", "Thu, Oct 15 · in 2 days", "$15.49", True)}
   {upcoming_row("M", "music", "Music premium", "Sun, Oct 18 · in 5 days", "$11.99")}
   {upcoming_row("C", "cloud", "Cloud storage", "Wed, Oct 21 · in 8 days", "$2.99")}
   {upcoming_row("D", "prod", "Design suite", "Tue, Oct 27 · in 14 days", "$59.99")}
 </div>
</div>
<div style="position:absolute;right:20px;top:680px;width:58px;height:58px;border-radius:18px;background:#3355EE;display:flex;align-items:center;justify-content:center;box-shadow:0 10px 24px rgba(51,85,238,.4)">
<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg></div>
{nav("Home")}"""


def screen_trials():
    alerts = "".join(
        f'<div class="row" style="gap:8px;font-size:13px">'
        f'<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="{c}" stroke-width="2.4" stroke-linecap="round">{p}</svg>{t}</div>'
        for c, p, t in [("#1B7A4B", '<path d="m5 12 5 5 9-10"/>', "3 days before · sent today, 9:00"),
                        ("#5B616B", '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>', "1 day before · Thu, 9:00"),
                        ("#5B616B", '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>', "Morning it ends · Fri, 8:00")])
    cancelled = "".join(
        f'<div class="card row" style="border-radius:16px;padding:12px 14px">{tile(l, cat)}'
        f'<div style="flex:1"><div style="font:600 15px DM Sans">{n}</div><div class="muted" style="font-size:12px">{m}</div></div>'
        f'<b style="font-size:13px;color:#1B7A4B">{s}</b></div>'
        for l, cat, n, m, s in [("P", "prod", "Photo editor pro", "Trial cancelled in time · Sep 2", "$119.99"),
                                ("T", "video", "TV streaming", "Cancelled Jun 14 · 3 charges skipped", "$23.97")])
    return f"""{status()}
<div style="padding:8px 20px 0;display:flex;flex-direction:column;gap:14px">
 <div class="row" style="gap:6px"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#15171C" stroke-width="2.2" stroke-linecap="round"><path d="m15 18-6-6 6-6"/></svg>
 <b style="font:700 20px 'Space Grotesk'">Trials &amp; savings</b></div>
 <div class="muted" style="font:700 12px DM Sans;letter-spacing:.08em">ACTIVE TRIAL</div>
 <div style="background:#fff;border:1px solid #F5C9A2;border-radius:20px;padding:16px;display:flex;flex-direction:column;gap:13px">
   <div class="row">{tile("L", "edu", 46)}<div><div style="font:700 16px DM Sans">Language course</div><div class="muted" style="font-size:13px">Then $84.00 per year · App Store</div></div></div>
   <div class="row" style="justify-content:space-between;font-size:13px"><b style="color:#8A3D08">3 days left</b><span class="muted">Ends Fri, Oct 16 · day 11 of 14</span></div>
   <div style="height:10px;border-radius:999px;background:#FDEEDF"><div style="width:78.6%;height:10px;border-radius:999px;background:#E07A1F"></div></div>
   {alerts}
   <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
     <span style="height:46px;border-radius:14px;border:1px solid #D5D8D0;display:flex;align-items:center;justify-content:center;font:600 14px DM Sans">Keep it</span>
     <span style="height:46px;border-radius:14px;background:#15171C;color:#fff;display:flex;align-items:center;justify-content:center;font:700 14px DM Sans">I cancelled it</span></div>
 </div>
 <div style="background:linear-gradient(135deg,#0B1026,#24206B);color:#fff;border-radius:22px;padding:18px;display:flex;flex-direction:column;gap:8px">
   <span style="font-size:13px;color:#C9CCD2">Saved by cancelling</span>
   <span style="font:700 38px 'Space Grotesk'">$143.96</span>
   <span style="font:600 13px DM Sans;color:#9FE0B9">$215.87 a year no longer charged</span></div>
 <div class="muted" style="font:700 12px DM Sans;letter-spacing:.08em">CANCELLED</div>
 {cancelled}
</div>"""


def screen_calendar():
    charges = {1: "health", 2: "prod", 5: "news", 15: "video", 16: "trial", 18: "music", 21: "cloud", 24: "video", 27: "prod"}
    cells = ['<span></span>'] * 3
    for d in range(1, 32):
        sel, today = d == 15, d == 13
        bg = "#15171C" if sel else ("#E6ECFD" if today else "transparent")
        col = "#fff" if sel else ("#9AA0A8" if d < 13 else "#15171C")
        dot = ""
        if d in charges:
            dc = "#E07A1F" if charges[d] == "trial" else ("#fff" if sel else C[charges[d]][1])
            dot = f'<span style="width:6px;height:6px;border-radius:50%;background:{dc}"></span>'
        cells.append(f'<span style="height:44px;border-radius:12px;background:{bg};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px">'
                     f'<span style="font:{"700" if sel or today else "500"} 14px DM Sans;color:{col}">{d}</span><span style="height:6px;display:flex">{dot}</span></span>')
    cells.append('<span></span>')
    grid = "".join(cells)
    return f"""{status()}
<div style="padding:8px 20px 0;display:flex;flex-direction:column;gap:14px">
 <div class="row" style="justify-content:space-between"><b style="font:700 28px 'Space Grotesk';letter-spacing:-.5px">Calendar</b>
 <span class="row" style="gap:6px"><span class="card" style="width:40px;height:40px;border-radius:12px;display:flex;align-items:center;justify-content:center">‹</span>
 <span class="card" style="width:40px;height:40px;border-radius:12px;display:flex;align-items:center;justify-content:center">›</span></span></div>
 <div class="card" style="padding:14px 12px">
   <div class="row" style="justify-content:space-between;padding:0 4px 8px"><b style="font-size:17px">October 2026</b><span class="muted" style="font-size:13px">9 charges</span></div>
   <div style="display:grid;grid-template-columns:repeat(7,1fr);text-align:center;font:600 11px DM Sans;color:#5B616B;margin-bottom:4px">
   <span>M</span><span>T</span><span>W</span><span>T</span><span>F</span><span>S</span><span>S</span></div>
   <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:4px">{grid}</div>
 </div>
 <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px">
   <div class="card" style="border-radius:14px;padding:12px"><div class="muted" style="font-size:11px">Paid</div><b style="font:700 17px 'Space Grotesk'">$53.00</b></div>
   <div class="card" style="border-radius:14px;padding:12px"><div class="muted" style="font-size:11px">Still to pay</div><b style="font:700 17px 'Space Grotesk'">$104.45</b></div>
   <div style="background:#FDEEDF;border:1px solid #F5D4B5;border-radius:14px;padding:12px"><div style="font-size:11px;color:#8A3D08">If trial converts</div><b style="font:700 17px 'Space Grotesk'">+$84.00</b></div>
 </div>
 <div class="card" style="padding:6px 16px 8px"><b style="display:block;font-size:15px;padding:8px 0 2px">Thu, Oct 15</b>
 {upcoming_row("V", "video", "Video streaming", "Monthly · reminder on Oct 14", "$15.49", True)}</div>
</div>
{nav("Calendar")}"""


def screen_insights():
    data = [("May", 138), ("Jun", 138), ("Jul", 149), ("Aug", 155), ("Sep", 165), ("Oct", 169)]
    bars = "".join(
        f'<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;gap:4px;height:100%">'
        f'<span class="muted" style="font:600 10px DM Sans">${v}</span>'
        f'<span style="width:100%;border-radius:7px 7px 0 0;height:{int(v / 169 * 96)}px;background:{"linear-gradient(#3D63FF,#6A3DE8)" if i == 5 else "#D5DCFB"}"></span></div>'
        for i, (m, v) in enumerate(data))
    labels = "".join(f'<span style="flex:1;text-align:center" class="muted">{m}</span>' for m, _ in data)

    def tip(bg, fg, icon, title, body):
        return (f'<div class="card row" style="border-radius:18px;padding:14px;align-items:flex-start">'
                f'<span style="width:40px;height:40px;border-radius:12px;background:{bg};display:flex;align-items:center;justify-content:center;flex-shrink:0">'
                f'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="{fg}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{icon}</svg></span>'
                f'<div><b style="font-size:15px">{title}</b><div class="muted" style="font-size:13px;line-height:1.45;margin-top:2px">{body}</div>'
                f'<div style="color:#3355EE;font:700 13px DM Sans;margin-top:6px">Review</div></div></div>')
    return f"""{status()}
<div style="padding:8px 20px 0;display:flex;flex-direction:column;gap:14px">
 <div class="row" style="justify-content:space-between"><b style="font:700 28px 'Space Grotesk';letter-spacing:-.5px">Insights</b>
 <span style="display:flex;background:#E8E9E4;border-radius:11px;padding:3px;font:600 13px DM Sans"><span style="background:#fff;border-radius:9px;padding:7px 12px">6 months</span><span class="muted" style="padding:7px 12px">Years</span></span></div>
 <div class="card" style="padding:16px">
   <div class="muted" style="font-size:13px">Spent in the last 6 months</div>
   <div style="font:700 28px 'Space Grotesk'">$914.21</div><div style="font:600 12px DM Sans;color:#8A3D08">▲ 22% since May</div>
   <div style="height:120px;display:flex;align-items:flex-end;gap:8px;margin-top:12px;border-bottom:1px solid #E2E4DE">{bars}</div>
   <div style="display:flex;gap:8px;font-size:11px;margin-top:6px">{labels}</div>
 </div>
 <div style="background:linear-gradient(135deg,#0B1026,#24206B);color:#fff;border-radius:20px;padding:16px">
   <div style="font-size:13px;color:#C9CCD2">Possible savings found</div>
   <div style="font:700 26px 'Space Grotesk'">Up to $653.88 / yr</div>
   <div style="font-size:12px;color:#C9CCD2">You decide. Nothing is ever cancelled for you.</div></div>
 {tip("#FDEEDF", "#C2560C", '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>', "Gym membership is rarely used", "You marked it as rarely used. It costs $468.00 a year.")}
 {tip("#E6ECFD", "#2A55E5", '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>', "Design suite raised its price", "$54.99 → $59.99 since August. That's $60.00 more a year.")}
</div>
{nav("Insights")}"""


def screen_privacy():
    def sw(title, sub, on=True):
        track = "#3355EE" if on else "#C9CCD2"
        just = "flex-end" if on else "flex-start"
        return (f'<div class="row" style="min-height:60px;padding:0 16px;border-top:1px solid #EEEFEB">'
                f'<div style="flex:1"><div style="font:500 15px DM Sans">{title}</div><div class="muted" style="font-size:12px">{sub}</div></div>'
                f'<span style="width:52px;height:32px;border-radius:999px;background:{track};padding:3px;display:flex;justify-content:{just}">'
                f'<span style="width:26px;height:26px;border-radius:50%;background:#fff"></span></span></div>')
    return f"""{status()}
<div style="padding:8px 20px 0;display:flex;flex-direction:column;gap:14px">
 <b style="font:700 28px 'Space Grotesk';letter-spacing:-.5px">Settings</b>
 <div style="background:linear-gradient(135deg,#0B1026,#24206B);color:#fff;border-radius:20px;padding:16px;display:flex;gap:12px">
   <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#9FE0B9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0"><path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/><path d="m9 12 2 2 4-4"/></svg>
   <div><b style="font-size:16px">Private by design</b><div style="font-size:12.5px;color:#C9CCD2;line-height:1.45;margin-top:3px">No bank link, no account. Your subscriptions are stored only on this phone and never sent anywhere.</div></div></div>
 <div class="muted" style="font:700 12px DM Sans;letter-spacing:.08em">SECURITY</div>
 <div class="card" style="overflow:hidden">{sw("App lock", "Fingerprint, face or screen lock").replace("border-top:1px solid #EEEFEB", "")}{sw("Hide amounts", "Show •••• instead of prices", False)}{sw("Block screenshots", "Also hides the app in recent apps")}</div>
 <div class="muted" style="font:700 12px DM Sans;letter-spacing:.08em">YOUR DATA</div>
 <div class="card" style="overflow:hidden">
   <div class="row" style="min-height:60px;padding:0 16px"><div style="flex:1"><div style="font:500 15px DM Sans">Encrypted backup</div><div class="muted" style="font-size:12px">AES-256 file, protected by your password</div></div><span class="muted">›</span></div>
   <div class="row" style="min-height:60px;padding:0 16px;border-top:1px solid #EEEFEB"><div style="flex:1"><div style="font:500 15px DM Sans">Export CSV &amp; PDF report</div><div class="muted" style="font-size:12px">Open in Excel, Sheets or print</div></div><span class="muted">›</span></div>
   <div class="row" style="min-height:60px;padding:0 16px;border-top:1px solid #EEEFEB"><div style="flex:1"><div style="font:500 15px DM Sans;color:#B3261E">Delete all data</div><div class="muted" style="font-size:12px">Removes everything from this phone</div></div></div>
 </div>
</div>
{nav("Settings")}"""


SHOTS = [
    ("01_home", "TRACK EVERYTHING", "All your subscriptions, <em>one place</em>", "Monthly &amp; yearly totals, budget and every upcoming bill.", screen_home,
     '<div class="badge" style="right:30px;top:1700px"><span class="ic" style="background:linear-gradient(135deg,#FFB547,#FF8A3D)">$</span><span>Budget alert<br><span style="font:500 22px DM Sans;color:#5B616B">$19 over this month</span></span></div>'),
    ("02_trials", "FREE TRIAL ALERTS", "Cancel trials <em>before</em> they charge", "Alerts 3 days, 1 day and the morning before a trial turns paid.", screen_trials,
     '<div class="badge" style="left:30px;top:1760px"><span class="ic" style="background:linear-gradient(135deg,#3D63FF,#6A3DE8)"><svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round"><path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10 21a2 2 0 0 0 4 0"/></svg></span><span>Trial ends tomorrow<br><span style="font:500 22px DM Sans;color:#5B616B">Language course · $84/yr</span></span></div>'),
    ("03_calendar", "RENEWAL CALENDAR", "Every bill on a <em>calendar</em>", "See what's paid, what's next and what a trial will cost.", screen_calendar, ""),
    ("04_insights", "SMART SAVINGS", "Find money you're <em>wasting</em>", "Spot unused apps, overlaps and sneaky price increases.", screen_insights,
     '<div class="badge" style="right:36px;top:1180px"><span class="ic" style="background:linear-gradient(135deg,#22A06B,#12938A)">↓</span><span>Save up to<br><span style="font:700 30px Space Grotesk;color:#1B7A4B">$653 / year</span></span></div>'),
    ("05_privacy", "100% PRIVATE", "No bank link. <em>No account.</em>", "Your data stays on your phone, locked with your fingerprint.", screen_privacy,
     '<div class="badge" style="left:36px;top:1770px"><span class="ic" style="background:#15171C"><svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg></span><span>Fingerprint lock<br><span style="font:500 22px DM Sans;color:#5B616B">Encrypted backups</span></span></div>'),
]


def page(kicker, title, sub, screen_fn, badge):
    return f"""<!doctype html><html><head><meta charset="utf-8">{FONTS}<style>{CSS}</style></head><body>
<div class="orb" style="width:700px;height:700px;right:-260px;top:-200px;background:radial-gradient(circle,rgba(106,61,232,.55),transparent 65%)"></div>
<div class="orb" style="width:800px;height:800px;left:-320px;bottom:-260px;background:radial-gradient(circle,rgba(61,99,255,.45),transparent 65%)"></div>
<svg style="position:absolute;left:70px;top:60px;opacity:.5" width="40" height="40" viewBox="0 0 24 24"><path d="M12 2 Q13 11 22 12 Q13 13 12 22 Q11 13 2 12 Q11 11 12 2Z" fill="#FFB547"/></svg>
<svg style="position:absolute;right:90px;top:430px;opacity:.6" width="28" height="28" viewBox="0 0 24 24"><path d="M12 2 Q13 11 22 12 Q13 13 12 22 Q11 13 2 12 Q11 11 12 2Z" fill="#fff"/></svg>
<div class="head"><span class="kicker">{kicker}</span><h1>{title}</h1><div class="sub">{sub}</div></div>
<div class="phone"><div class="screen"><div class="ui">{screen_fn()}</div></div></div>
{badge}
</body></html>"""


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, k, t, s, fn, badge in SHOTS:
        with open(os.path.join(OUT, name + ".html"), "w") as f:
            f.write(page(k, t, s, fn, badge))
    print("written", len(SHOTS))
