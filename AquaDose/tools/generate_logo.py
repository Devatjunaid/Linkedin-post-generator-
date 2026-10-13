"""Generates the AquaDose logo as SVG (store assets) and Android VectorDrawables."""
import math, os, sys

OUT = sys.argv[1]  # project root

NAVY = "#0E2A3B"
NAVY2 = "#15405A"
WHITE = "#FFFFFF"

def circle(cx, cy, r):
    return f"M{cx-r:.2f},{cy:.2f} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0 Z"

def ellipse(cx, cy, rx, ry):
    return f"M{cx-rx:.2f},{cy:.2f} a{rx},{ry} 0 1,0 {2*rx},0 a{rx},{ry} 0 1,0 {-2*rx},0 Z"

def rrect(x, y, w, h, r):
    return (f"M{x+r:.2f},{y:.2f} H{x+w-r:.2f} A{r},{r} 0 0,1 {x+w:.2f},{y+r:.2f} V{y+h-r:.2f} "
            f"A{r},{r} 0 0,1 {x+w-r:.2f},{y+h:.2f} H{x+r:.2f} A{r},{r} 0 0,1 {x:.2f},{y+h-r:.2f} "
            f"V{y+r:.2f} A{r},{r} 0 0,1 {x+r:.2f},{y:.2f} Z")

def arc(cx, cy, r, a1, a2):
    p = lambda a: (cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
    x1, y1 = p(a1); x2, y2 = p(a2)
    return f"M{x1:.2f},{y1:.2f} A{r},{r} 0 0,1 {x2:.2f},{y2:.2f}"

def wave(y, amp, start=-4, end=112, step=12):
    d = f"M{start},{y} Q{start+step/2},{y-amp} {start+step},{y} "
    x = start + step
    while x < end:
        x += step
        d += f"T{x},{y} "
    return d + f"V112 H{start} Z"

# ---- foreground shapes (108 x 108 adaptive icon grid) ----
fg = []
# pH colour scale arc
ph_colors = ["#E5484D", "#F2834A", "#F5C842", "#7BD08A", "#2FC5C9", "#3E8BF5", "#7C5CFF"]
start, span, gap = 200, 140, 3.2
seg = span / len(ph_colors)
for i, c in enumerate(ph_colors):
    a1 = start + i * seg + gap / 2
    a2 = start + (i + 1) * seg - gap / 2
    fg.append(dict(d=arc(54, 57, 33, a1, a2), stroke=c, sw=3.6, cap="round"))
# sun-glint dot at the top of the arc (marks "7.0")

# test strip (drawn before water so its lower half shows through the water)
fg.append(dict(d=rrect(62.4, 41, 5.2, 40, 1.6), fill=WHITE))
for i, c in enumerate(["#F5C842", "#F2834A", "#E86A92", "#7C5CFF"]):
    fg.append(dict(d=rrect(63.3, 52.5 + i * 6.2, 3.4, 4.2, 0.8), fill=c))

# water layers
fg.append(dict(d=wave(67.5, 3.2), fill="#2BB7CF", alpha=0.88))
fg.append(dict(d=wave(76, 2.6, start=-10), fill="#0B7A93", alpha=0.92))
fg.append(dict(d=wave(86, 2.2, start=-7), fill="#085E73"))
# ripples where the strip enters the water
fg.append(dict(d=ellipse(65, 68.6, 8, 1.7), stroke=WHITE, sw=0.9, alpha=0.85))
fg.append(dict(d=ellipse(65, 69.6, 14, 3.0), stroke=WHITE, sw=0.7, alpha=0.5))
# light sparkles on the water
fg.append(dict(d="M30,80 h7", stroke=WHITE, sw=1.1, cap="round", alpha=0.45))
fg.append(dict(d="M76,84 h9", stroke=WHITE, sw=1.1, cap="round", alpha=0.45))
fg.append(dict(d="M48,91 h5", stroke=WHITE, sw=1.1, cap="round", alpha=0.35))

# pool coping / deck the person kneels on
fg.append(dict(d="M-4,60.2 H55.5 A2.4,2.4 0 0,1 55.5,65 H-4 Z", fill="#DCEBF0"))
fg.append(dict(d="M-4,65 H55.5 V66.6 H-4 Z", fill="#9DBBC6"))
# ladder rails
fg.append(dict(d="M19,60.5 V52.5 Q19,48.5 23,48.5 Q27,48.5 27,52.5 V54", stroke="#DCEBF0", sw=1.6, cap="round"))

# person kneeling at the edge, dipping the strip
P = WHITE
fg.append(dict(d="M38.2,43.5 L34.6,53", stroke=P, sw=6.6, cap="round"))            # torso
fg.append(dict(d="M34.6,53 L44,51.2 L45,58.6", stroke=P, sw=4.6, cap="round", join="round"))  # front leg
fg.append(dict(d="M34.6,53 L31.4,58.8 L25.2,58.8", stroke=P, sw=4.6, cap="round", join="round"))  # back leg
fg.append(dict(d="M38.8,45 Q50,50.5 61.6,44.6", stroke=P, sw=3.4, cap="round"))      # arm
fg.append(dict(d=circle(63.4, 44.2, 2.3), fill=P))                                     # hand
fg.append(dict(d=circle(40.6, 35.6, 5.2), fill=P))                                     # head
fg.append(dict(d="M35.6,33.6 Q40.4,27.6 45.8,33 L48.4,33.8 Q43,32.2 35.6,33.6 Z", fill="#2BB7CF"))  # cap

# ---- background ----
bg = [dict(d="M0,0 H108 V108 H0 Z", fill=NAVY), dict(d=circle(54, 50, 40), fill=NAVY2, alpha=0.55)]

def svg_el(s):
    a = f'd="{s["d"]}" '
    if "fill" in s:
        a += f'fill="{s["fill"]}" '
    else:
        a += 'fill="none" '
    if "stroke" in s:
        a += f'stroke="{s["stroke"]}" stroke-width="{s["sw"]}" '
        a += f'stroke-linecap="{s.get("cap", "butt")}" stroke-linejoin="{s.get("join", "miter")}" '
        if "alpha" in s: a += f'stroke-opacity="{s["alpha"]}" '
    if "fill" in s and "alpha" in s:
        a += f'fill-opacity="{s["alpha"]}" '
    return f"<path {a}/>"

def vd_el(s, indent="    "):
    a = f'{indent}<path\n{indent}    android:pathData="{s["d"]}"'
    if "fill" in s:
        a += f'\n{indent}    android:fillColor="{s["fill"]}"'
        if "alpha" in s: a += f'\n{indent}    android:fillAlpha="{s["alpha"]}"'
    if "stroke" in s:
        a += f'\n{indent}    android:strokeColor="{s["stroke"]}"\n{indent}    android:strokeWidth="{s["sw"]}"'
        a += f'\n{indent}    android:strokeLineCap="{s.get("cap", "butt")}"\n{indent}    android:strokeLineJoin="{s.get("join", "miter")}"'
        if "alpha" in s: a += f'\n{indent}    android:strokeAlpha="{s["alpha"]}"'
    return a + " />"

def vector(shapes, size_dp=108, group=None, clip=None):
    body = []
    ind = "    "
    if group:
        body.append(f'    <group {group}>')
        ind = "        "
    if clip:
        body.append(f'{ind}<clip-path android:pathData="{clip}" />')
    body += [vd_el(s, ind) for s in shapes]
    if group:
        body.append("    </group>")
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size_dp}dp"\n    android:height="{size_dp}dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            + "\n".join(body) + "\n</vector>\n")

res = os.path.join(OUT, "app/src/main/res/drawable")
os.makedirs(res, exist_ok=True)
open(os.path.join(res, "ic_launcher_foreground.xml"), "w").write(vector(fg))
open(os.path.join(res, "ic_launcher_background.xml"), "w").write(vector(bg))
# full logo (rounded square) used inside the app; crops the 72dp visible area
open(os.path.join(res, "ic_app_logo.xml"), "w").write(
    vector(bg + fg, size_dp=96,
           group='android:scaleX="1.35" android:scaleY="1.35" android:translateX="-18.9" android:translateY="-18.9"',
           clip=rrect(14, 14, 80, 80, 18)))
# Android 12+ system splash icon: logo inside the 2/3 circle-safe area
open(os.path.join(res, "ic_splash_logo.xml"), "w").write(
    vector(bg + fg,
           group='android:scaleX="0.92" android:scaleY="0.92" android:translateX="4.32" android:translateY="4.32"',
           clip=circle(54, 54, 36)))

# SVG for the store / preview (crop to visible 72dp, rounded)
assets = os.path.join(OUT, "store-assets")
os.makedirs(assets, exist_ok=True)
svg = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="14 14 80 80" width="512" height="512">'
       f'<defs><clipPath id="c"><path d="{rrect(14, 14, 80, 80, 18)}"/></clipPath></defs>'
       '<g clip-path="url(#c)">' + "".join(svg_el(s) for s in bg + fg) + "</g></svg>")
open(os.path.join(assets, "logo.svg"), "w").write(svg)
svg_sq = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="14 14 80 80" width="512" height="512">'
          + "".join(svg_el(s) for s in bg + fg) + "</svg>")
open(os.path.join(assets, "logo_square.svg"), "w").write(svg_sq)
print("ok")
