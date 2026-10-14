#!/usr/bin/env python3
"""Generates Renew Alert's vector graphics.

Writes Android VectorDrawable / AnimatedVectorDrawable XML into
app/src/main/res/drawable and matching SVGs into design/svg (used to render
Play Store PNGs). Run from the renewalert-android folder:

    python3 design/generate_assets.py
"""
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DRAWABLE = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
SVG_DIR = os.path.join(ROOT, "design", "svg")

BLUE = "#3D63FF"
VIOLET = "#6A3DE8"
ORANGE = "#FF8A3D"
GOLD = "#FFB547"
INK = "#15171C"
WHITE = "#FFFFFF"
LINE = "#E1E5EF"
MUTED = "#C9CFE0"
GREEN = "#22A06B"

BRAND_GRAD = ("lin", 0.0, 0.0, 1.0, 1.0, [(0, BLUE), (1, VIOLET)])
COIN_GRAD = ("lin", 0.0, 0.0, 1.0, 1.0, [(0, GOLD), (1, ORANGE)])


def f(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s == "-0" else s


# ---------------------------------------------------------------- primitives
def rrect(x, y, w, h, r, **kw):
    r = min(r, w / 2, h / 2)
    if r <= 0:
        d = f"M{f(x)},{f(y)} H{f(x + w)} V{f(y + h)} H{f(x)} Z"
        return dict(d=d, bbox=(x, y, w, h), **kw)
    d = (f"M{f(x + r)},{f(y)} H{f(x + w - r)} A{f(r)},{f(r)} 0 0 1 {f(x + w)},{f(y + r)} "
         f"V{f(y + h - r)} A{f(r)},{f(r)} 0 0 1 {f(x + w - r)},{f(y + h)} H{f(x + r)} "
         f"A{f(r)},{f(r)} 0 0 1 {f(x)},{f(y + h - r)} V{f(y + r)} A{f(r)},{f(r)} 0 0 1 {f(x + r)},{f(y)} Z")
    return dict(d=d, bbox=(x, y, w, h), **kw)


def circle(cx, cy, r, **kw):
    d = (f"M{f(cx - r)},{f(cy)} A{f(r)},{f(r)} 0 1 1 {f(cx + r)},{f(cy)} "
         f"A{f(r)},{f(r)} 0 1 1 {f(cx - r)},{f(cy)} Z")
    return dict(d=d, bbox=(cx - r, cy - r, 2 * r, 2 * r), **kw)


def path(d, bbox=(0, 0, 1, 1), **kw):
    return dict(d=d, bbox=bbox, **kw)


def arc(cx, cy, r, a0, a1, **kw):
    """Arc from angle a0 to a1 (degrees, clockwise, 0 = 3 o'clock)."""
    p0 = (cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0)))
    p1 = (cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1)))
    large = 1 if (a1 - a0) % 360 > 180 else 0
    d = f"M{f(p0[0])},{f(p0[1])} A{f(r)},{f(r)} 0 {large} 1 {f(p1[0])},{f(p1[1])}"
    return dict(d=d, bbox=(cx - r, cy - r, 2 * r, 2 * r), **kw)


def star(cx, cy, s, **kw):
    q = s * 0.16
    d = (f"M{f(cx)},{f(cy - s)} Q{f(cx + q)},{f(cy - q)} {f(cx + s)},{f(cy)} "
         f"Q{f(cx + q)},{f(cy + q)} {f(cx)},{f(cy + s)} Q{f(cx - q)},{f(cy + q)} {f(cx - s)},{f(cy)} "
         f"Q{f(cx - q)},{f(cy - q)} {f(cx)},{f(cy - s)} Z")
    return dict(d=d, bbox=(cx - s, cy - s, 2 * s, 2 * s), **kw)


# ---------------------------------------------------------------- logo
def logo_parts(cx, cy, R, ring=WHITE, coin=COIN_GRAD, check=WHITE, spark=WHITE, with_check=True):
    """The Renew Alert mark: a renewal ring with arrowhead around a coin."""
    k = R / 20.0
    sw = 7 * k
    a0, a1 = -40, 250
    parts = {"ring": arc(cx, cy, R, a0, a1, stroke=ring, sw=sw, cap="round")}
    end = (cx + R * math.cos(math.radians(a1)), cy + R * math.sin(math.radians(a1)))
    t = (-math.sin(math.radians(a1)), math.cos(math.radians(a1)))
    n = (-t[1], t[0])
    tip = (end[0] + 7.5 * k * t[0], end[1] + 7.5 * k * t[1])
    base = (end[0] - 1.5 * k * t[0], end[1] - 1.5 * k * t[1])
    b1 = (base[0] + 6.5 * k * n[0], base[1] + 6.5 * k * n[1])
    b2 = (base[0] - 6.5 * k * n[0], base[1] - 6.5 * k * n[1])
    parts["arrow"] = path(f"M{f(tip[0])},{f(tip[1])} L{f(b1[0])},{f(b1[1])} L{f(b2[0])},{f(b2[1])} Z",
                          bbox=(min(tip[0], b1[0], b2[0]), min(tip[1], b1[1], b2[1]), 14 * k, 14 * k),
                          fill=ring, join="round", stroke=ring, sw=1.6 * k)
    parts["arrow_pivot"] = ((tip[0] + b1[0] + b2[0]) / 3, (tip[1] + b1[1] + b2[1]) / 3)
    parts["coin"] = circle(cx, cy, 8.5 * k, fill=coin)
    if with_check:
        parts["check"] = path(
            f"M{f(cx - 3.6 * k)},{f(cy + 0.2 * k)} L{f(cx - 0.9 * k)},{f(cy + 2.9 * k)} L{f(cx + 4 * k)},{f(cy - 2.6 * k)}",
            stroke=check, sw=2.3 * k, cap="round", join="round")
    sx, sy = cx + 15 * k, cy - 23 * k
    parts["spark"] = star(sx, sy, 4.6 * k, fill=spark)
    parts["spark_pivot"] = (sx, sy)
    parts["dot"] = circle(cx + 23.5 * k, cy - 13 * k, 1.5 * k, fill=spark, alpha=0.75)
    return parts


def logo_list(parts):
    order = ["ring", "arrow", "coin", "check", "spark", "dot"]
    return [parts[k] for k in order if k in parts]


# ---------------------------------------------------------------- writers
def grad_xml(g, bbox, indent):
    _, x1, y1, x2, y2, stops = g
    x, y, w, h = bbox
    items = "\n".join(
        f'{indent}    <item android:offset="{o}" android:color="{c}"/>' for o, c in stops)
    return (f'{indent}<aapt:attr name="android:fillColor">\n'
            f'{indent}  <gradient android:type="linear" android:startX="{f(x + x1 * w)}" android:startY="{f(y + y1 * h)}" '
            f'android:endX="{f(x + x2 * w)}" android:endY="{f(y + y2 * h)}">\n{items}\n'
            f'{indent}  </gradient>\n{indent}</aapt:attr>\n')


def path_xml(s, indent="    ", name=None):
    a = [f'android:pathData="{s["d"]}"']
    if name:
        a.insert(0, f'android:name="{name}"')
    fill = s.get("fill")
    if isinstance(fill, str):
        a.append(f'android:fillColor="{fill}"')
    if s.get("stroke"):
        a.append(f'android:strokeColor="{s["stroke"]}"')
        a.append(f'android:strokeWidth="{f(s["sw"])}"')
        a.append(f'android:strokeLineCap="{s.get("cap", "butt")}"')
        a.append(f'android:strokeLineJoin="{s.get("join", "round")}"')
    if "alpha" in s:
        if fill:
            a.append(f'android:fillAlpha="{s["alpha"]}"')
        if s.get("stroke"):
            a.append(f'android:strokeAlpha="{s["alpha"]}"')
    attrs = ("\n" + indent + "    ").join(a)
    if isinstance(fill, tuple):
        return f"{indent}<path\n{indent}    {attrs}>\n{grad_xml(fill, s['bbox'], indent + '  ')}{indent}</path>\n"
    return f"{indent}<path\n{indent}    {attrs} />\n"


def vector(name, w, h, shapes, dp=None, tint=None):
    dw, dh = dp or (w, h)
    body = "".join(path_xml(s) for s in shapes)
    tint_attr = f'\n    android:tint="{tint}"' if tint else ""
    xml = (f'<?xml version="1.0" encoding="utf-8"?>\n'
           f'<!-- Generated by design/generate_assets.py - edit the script, not this file. -->\n'
           f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           f'    xmlns:aapt="http://schemas.android.com/aapt"\n'
           f'    android:width="{dw}dp"\n    android:height="{dh}dp"\n'
           f'    android:viewportWidth="{w}"\n    android:viewportHeight="{h}"{tint_attr}>\n{body}</vector>\n')
    with open(os.path.join(DRAWABLE, name + ".xml"), "w") as fh:
        fh.write(xml)


GRAD_ID = [0]


def svg_shape(s, defs):
    fill = s.get("fill")
    attrs = [f'd="{s["d"]}"']
    if isinstance(fill, tuple):
        GRAD_ID[0] += 1
        gid = f"g{GRAD_ID[0]}"
        _, x1, y1, x2, y2, stops = fill
        x, y, w, h = s["bbox"]
        st = "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in stops)
        defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{f(x + x1 * w)}" y1="{f(y + y1 * h)}" '
                    f'x2="{f(x + x2 * w)}" y2="{f(y + y2 * h)}">{st}</linearGradient>')
        attrs.append(f'fill="url(#{gid})"')
    elif fill:
        attrs.append(f'fill="{fill}"')
    else:
        attrs.append('fill="none"')
    if s.get("stroke"):
        attrs.append(f'stroke="{s["stroke"]}" stroke-width="{f(s["sw"])}" '
                     f'stroke-linecap="{s.get("cap", "butt")}" stroke-linejoin="{s.get("join", "round")}"')
    if "alpha" in s:
        attrs.append(f'opacity="{s["alpha"]}"')
    return f'<path {" ".join(attrs)}/>'


def svg(name, w, h, shapes, out_w=None, out_h=None, extra=""):
    defs = []
    body = "\n".join(svg_shape(s, defs) for s in shapes)
    ow, oh = out_w or w, out_h or h
    doc = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{ow}" height="{oh}" viewBox="0 0 {w} {h}">\n'
           f'<defs>{"".join(defs)}</defs>\n{body}\n{extra}</svg>\n')
    with open(os.path.join(SVG_DIR, name + ".svg"), "w") as fh:
        fh.write(doc)


# ---------------------------------------------------------------- assets
def launcher():
    bg = [
        rrect(0, 0, 108, 108, 0, fill=("lin", 0, 0, 1, 1, [(0, "#4A6BFF"), (0.55, "#4F4BF0"), (1, VIOLET)])),
        circle(18, 16, 30, fill=WHITE, alpha=0.08),
        circle(96, 98, 26, fill=WHITE, alpha=0.06),
        circle(88, 22, 6, fill=WHITE, alpha=0.08),
    ]
    vector("ic_launcher_background", 108, 108, bg, dp=(108, 108))
    fg = logo_list(logo_parts(54, 54, 20))
    vector("ic_launcher_foreground", 108, 108, fg, dp=(108, 108))
    mono = logo_list(logo_parts(54, 54, 20, ring="#000000", coin="#000000", check=None, spark="#000000",
                                with_check=False))
    vector("ic_launcher_monochrome", 108, 108, mono, dp=(108, 108))
    return bg, fg


def notification_icon():
    p = logo_parts(12, 12.6, 7.4, ring=WHITE, coin=WHITE, spark=WHITE, with_check=False)
    shapes = [p["ring"], p["arrow"], p["coin"], p["spark"]]
    vector("ic_notification", 24, 24, shapes, dp=(24, 24))


def logo_tile():
    tile = rrect(0, 0, 108, 108, 30, fill=BRAND_GRAD)
    gloss = path("M0,30 A30,30 0 0 1 30,0 H78 A30,30 0 0 1 108,30 V44 C76,58 32,58 0,44 Z",
                 bbox=(0, 0, 108, 60), fill=WHITE, alpha=0.08)
    shapes = [tile, gloss] + logo_list(logo_parts(54, 56, 27))
    vector("ic_logo_tile", 108, 108, shapes, dp=(96, 96))
    return shapes


def splash_avd():
    """Animated splash icon (Android 12+). Viewport 108, content inside the 72dp safe circle."""
    p = logo_parts(54, 54, 19)
    tile = circle(54, 54, 33, fill=BRAND_GRAD)
    ax, ay = p["arrow_pivot"]
    sx, sy = p["spark_pivot"]
    vec = f'''<vector
            android:width="108dp"
            android:height="108dp"
            android:viewportWidth="108"
            android:viewportHeight="108">
            <group android:name="tile" android:pivotX="54" android:pivotY="54" android:scaleX="0.5" android:scaleY="0.5">
{path_xml(tile, "                ")}            </group>
{path_xml(p["ring"], "            ", "ring").replace('android:pathData', 'android:trimPathEnd="0"\n                android:pathData', 1)}            <group android:name="arrow" android:pivotX="{f(ax)}" android:pivotY="{f(ay)}" android:scaleX="0" android:scaleY="0">
{path_xml(p["arrow"], "                ")}            </group>
            <group android:name="coin" android:pivotX="54" android:pivotY="54" android:scaleX="0" android:scaleY="0">
{path_xml(p["coin"], "                ")}{path_xml(p["check"], "                ", "check").replace('android:pathData', 'android:trimPathEnd="0"\n                    android:pathData', 1)}            </group>
            <group android:name="spark" android:pivotX="{f(sx)}" android:pivotY="{f(sy)}" android:scaleX="0" android:scaleY="0">
{path_xml(p["spark"], "                ")}{path_xml(p["dot"], "                ")}            </group>
        </vector>'''

    def scale(target, start, dur, frm, to, interp):
        return f'''    <target android:name="{target}">
        <aapt:attr name="android:animation">
            <set>
                <objectAnimator android:propertyName="scaleX" android:startOffset="{start}" android:duration="{dur}"
                    android:valueFrom="{frm}" android:valueTo="{to}" android:valueType="floatType"
                    android:interpolator="{interp}" />
                <objectAnimator android:propertyName="scaleY" android:startOffset="{start}" android:duration="{dur}"
                    android:valueFrom="{frm}" android:valueTo="{to}" android:valueType="floatType"
                    android:interpolator="{interp}" />
            </set>
        </aapt:attr>
    </target>
'''

    def trim(target, start, dur):
        return f'''    <target android:name="{target}">
        <aapt:attr name="android:animation">
            <objectAnimator android:propertyName="trimPathEnd" android:startOffset="{start}" android:duration="{dur}"
                android:valueFrom="0" android:valueTo="1" android:valueType="floatType"
                android:interpolator="@android:interpolator/fast_out_slow_in" />
        </aapt:attr>
    </target>
'''

    overshoot = "@android:anim/overshoot_interpolator"
    targets = (scale("tile", 0, 420, 0.5, 1, overshoot)
               + trim("ring", 140, 560)
               + scale("arrow", 600, 220, 0, 1, overshoot)
               + scale("coin", 380, 360, 0, 1, overshoot)
               + trim("check", 700, 260)
               + f'''    <target android:name="spark">
        <aapt:attr name="android:animation">
            <set>
                <objectAnimator android:propertyName="scaleX" android:startOffset="720" android:duration="260"
                    android:valueFrom="0" android:valueTo="1" android:valueType="floatType"
                    android:interpolator="{overshoot}" />
                <objectAnimator android:propertyName="scaleY" android:startOffset="720" android:duration="260"
                    android:valueFrom="0" android:valueTo="1" android:valueType="floatType"
                    android:interpolator="{overshoot}" />
                <objectAnimator android:propertyName="rotation" android:startOffset="720" android:duration="280"
                    android:valueFrom="-90" android:valueTo="0" android:valueType="floatType"
                    android:interpolator="@android:interpolator/fast_out_slow_in" />
            </set>
        </aapt:attr>
    </target>
''')
    xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by design/generate_assets.py - edit the script, not this file. -->
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        {vec}
    </aapt:attr>
{targets}</animated-vector>
'''
    with open(os.path.join(DRAWABLE, "avd_splash_logo.xml"), "w") as fh:
        fh.write(xml)


def ill_privacy():
    s = [
        circle(160, 122, 100, fill="#E8EDFF"),
        circle(60, 62, 11, fill="#FFE9DA"),
        circle(264, 184, 15, fill="#F1EAFE"),
        circle(272, 56, 6, fill=ORANGE, alpha=0.7),
        rrect(110, 28, 100, 184, 20, fill=INK),
        rrect(117, 42, 86, 156, 13, fill=WHITE),
        rrect(146, 34, 28, 4, 2, fill="#3A3E47"),
        path("M160,70 L190,81 V104 C190,123 177,136 160,142 C143,136 130,123 130,104 V81 Z",
             bbox=(130, 70, 60, 72), fill=BRAND_GRAD),
        path("M153,101 V95 A7,7 0 0 1 167,95 V101", stroke=WHITE, sw=3.2, cap="round"),
        rrect(148, 100, 24, 19, 4, fill=WHITE),
        circle(160, 108.5, 2.6, fill=BLUE),
        rrect(158.8, 109, 2.4, 5, 1.2, fill=BLUE),
        rrect(132, 156, 56, 6, 3, fill=LINE),
        rrect(141, 168, 38, 6, 3, fill=LINE),
        rrect(34, 118, 66, 46, 10, fill=WHITE, stroke=LINE, sw=2),
        path("M67,127 L84,135 H50 Z", fill="#9AA3B5", join="round", stroke="#9AA3B5", sw=1.5),
        rrect(53, 138, 4, 14, 2, fill="#9AA3B5"),
        rrect(65, 138, 4, 14, 2, fill="#9AA3B5"),
        rrect(77, 138, 4, 14, 2, fill="#9AA3B5"),
        rrect(50, 154, 34, 3.5, 1.75, fill="#9AA3B5"),
        path("M42,124 L92,160", stroke=ORANGE, sw=4.5, cap="round"),
        rrect(218, 90, 78, 32, 16, fill=WHITE, stroke=LINE, sw=2),
        circle(235, 106, 9, fill=GREEN),
        path("M231,106 L234,109 L239.5,103", stroke=WHITE, sw=2.2, cap="round"),
        rrect(250, 103, 36, 6, 3, fill=MUTED),
        star(238, 38, 7, fill=ORANGE),
        star(90, 56, 5, fill=BLUE),
        star(244, 150, 4, fill=VIOLET),
    ]
    vector("ill_privacy", 320, 240, s, dp=(320, 240))
    svg("ill_privacy", 320, 240, s)


def ill_reminders():
    s = [
        circle(160, 122, 100, fill="#FFF0E5"),
        circle(56, 70, 10, fill="#E8EDFF"),
        circle(272, 188, 12, fill="#F1EAFE"),
        rrect(60, 50, 150, 142, 18, fill=WHITE, stroke=LINE, sw=2),
        rrect(60, 50, 150, 36, 18, fill=BLUE),
        rrect(60, 70, 150, 16, 0, fill=BLUE),
        rrect(92, 40, 7, 20, 3.5, fill=INK),
        rrect(171, 40, 7, 20, 3.5, fill=INK),
        rrect(78, 62, 50, 7, 3.5, fill=WHITE, alpha=0.85),
    ]
    for j in range(3):
        for i in range(5):
            x, y = 85 + i * 25, 108 + j * 26
            if (i, j) == (3, 1):
                s.append(circle(x, y, 12, fill=ORANGE))
                s.append(circle(x, y, 4, fill=WHITE))
            else:
                s.append(circle(x, y, 7, fill="#EEF0F5"))
    s += [
        path("M240,64 C227,64 220,75 220,88 V104 L213,113 H267 L260,104 V88 C260,75 253,64 240,64 Z",
             bbox=(213, 64, 54, 49), fill=("lin", 0, 0, 1, 1, [(0, "#8A63F2"), (1, VIOLET)])),
        circle(240, 119, 6, fill=VIOLET),
        circle(240, 62, 4, fill=VIOLET),
        path("M272,78 A18,18 0 0 1 272,104", stroke=VIOLET, sw=3.2, cap="round", alpha=0.5),
        path("M281,70 A30,30 0 0 1 281,112", stroke=VIOLET, sw=3.2, cap="round", alpha=0.3),
        path("M208,78 A18,18 0 0 0 208,104", stroke=VIOLET, sw=3.2, cap="round", alpha=0.5),
        rrect(146, 166, 150, 48, 14, fill=WHITE, stroke=LINE, sw=2),
        rrect(157, 177, 26, 26, 8, fill=BRAND_GRAD),
        circle(170, 190, 4.5, fill=WHITE),
        rrect(192, 180, 86, 7, 3.5, fill=MUTED),
        rrect(192, 193, 56, 7, 3.5, fill=LINE),
        star(42, 150, 6, fill=ORANGE),
        star(286, 48, 5, fill=BLUE),
    ]
    vector("ill_reminders", 320, 240, s, dp=(320, 240))
    svg("ill_reminders", 320, 240, s)


def ill_insights():
    s = [
        circle(160, 120, 100, fill="#F1EAFE"),
        circle(58, 186, 12, fill="#FFE9DA"),
        circle(270, 50, 9, fill="#E8EDFF"),
        rrect(46, 46, 156, 136, 18, fill=WHITE, stroke=LINE, sw=2),
        rrect(64, 62, 60, 7, 3.5, fill=MUTED),
        rrect(64, 128, 20, 36, 6, fill="#C9D3FF"),
        rrect(92, 110, 20, 54, 6, fill="#C9D3FF"),
        rrect(120, 96, 20, 68, 6, fill="#AEBBFF"),
        rrect(148, 78, 20, 86, 6, fill=BRAND_GRAD),
        rrect(60, 166, 126, 3, 1.5, fill=LINE),
        path("M74,118 L102,100 L130,86 L158,64", stroke=ORANGE, sw=3.4, cap="round"),
        circle(158, 64, 5.5, fill=ORANGE),
        circle(158, 64, 2.2, fill=WHITE),
        circle(240, 150, 40, fill=WHITE, stroke=LINE, sw=2),
        arc(240, 150, 26, -90, 125, stroke=BLUE, sw=13),
        arc(240, 150, 26, 129, 215, stroke=ORANGE, sw=13),
        arc(240, 150, 26, 219, 266, stroke=VIOLET, sw=13),
        rrect(220, 78, 56, 14, 7, fill=GOLD),
        rrect(226, 66, 56, 14, 7, fill=ORANGE),
        rrect(220, 54, 56, 14, 7, fill=GOLD),
        star(212, 34, 6, fill=ORANGE),
        star(34, 96, 5, fill=VIOLET),
    ]
    vector("ill_insights", 320, 240, s, dp=(320, 240))
    svg("ill_insights", 320, 240, s)


def ill_empty():
    s = [
        circle(160, 128, 92, fill="#E8EDFF"),
        circle(70, 70, 8, fill="#FFE9DA"),
        circle(258, 190, 10, fill="#F1EAFE"),
        path("M98,118 L160,142 L222,118 V180 L160,206 L98,180 Z", fill=WHITE, stroke=MUTED, sw=2.4),
        path("M98,118 L160,142 V206 L98,180 Z", fill="#F4F6FB", stroke=MUTED, sw=2.4),
        path("M98,118 L128,96 L190,120 L160,142 Z", fill=LINE, stroke=MUTED, sw=2.4),
        path("M222,118 L238,100 L176,78 L160,96", stroke=MUTED, sw=2.4, cap="round"),
        rrect(120, 160, 26, 6, 3, fill=MUTED),
        circle(226, 72, 22, fill=BRAND_GRAD),
        path("M226,62 V82 M216,72 H236", stroke=WHITE, sw=3.6, cap="round"),
        star(92, 92, 6, fill=ORANGE),
        star(268, 120, 5, fill=BLUE),
    ]
    vector("ill_empty", 320, 240, s, dp=(320, 240))
    svg("ill_empty", 320, 240, s)


def store_assets(bg, fg):
    # 512x512 Play Store icon: full-bleed square (Play applies its own mask).
    k = 512 / 108
    icon_bg = [rrect(0, 0, 512, 512, 0, fill=("lin", 0, 0, 1, 1, [(0, "#4A6BFF"), (0.55, "#4F4BF0"), (1, VIOLET)])),
               circle(18 * k, 16 * k, 30 * k, fill=WHITE, alpha=0.08),
               circle(96 * k, 98 * k, 26 * k, fill=WHITE, alpha=0.06)]
    icon = icon_bg + logo_list(logo_parts(256, 256, 20 * k * 1.25))
    svg("play_icon_512", 512, 512, icon)

    # 1024x500 feature graphic.
    fg_shapes = [
        rrect(0, 0, 1024, 500, 0, fill=("lin", 0, 0, 1, 1, [(0, "#0B1026"), (1, "#24206B")])),
        circle(860, 90, 220, fill=VIOLET, alpha=0.25),
        circle(120, 470, 180, fill=BLUE, alpha=0.22),
        rrect(92, 150, 200, 200, 56, fill=BRAND_GRAD),
    ] + logo_list(logo_parts(192, 254, 50))
    cards = [
        (720, 100, "#F0E8FB", "#7A3FCB", "S", "Streaming", "$15.49", -4),
        (752, 200, "#DFF3F1", "#0F7F77", "M", "Music", "$11.99", 3),
        (720, 300, "#FFE9DA", "#C2560C", "T", "Trial ends Fri", "$84", -2),
    ]
    extra = ['<style>text{font-family:"Space Grotesk","DM Sans",sans-serif}</style>',
             '<text x="330" y="236" fill="#FFFFFF" font-size="60" font-weight="700" letter-spacing="-1.5">Renew Alert</text>',
             '<text x="334" y="290" fill="#C9CCF2" font-size="28" font-weight="500">Subscriptions, under control.</text>',
             '<text x="334" y="336" fill="#9FA6D9" font-size="22">Private · No bank link · Reminders</text>']
    for x, y, bgc, fgc, letter, label, price, rot in cards:
        extra.append(
            f'<g transform="rotate({rot} {x + 130} {y + 38})">'
            f'<rect x="{x}" y="{y}" width="260" height="76" rx="20" fill="#FFFFFF"/>'
            f'<rect x="{x + 16}" y="{y + 16}" width="44" height="44" rx="12" fill="{bgc}"/>'
            f'<text x="{x + 38}" y="{y + 46}" text-anchor="middle" fill="{fgc}" font-size="22" font-weight="700">{letter}</text>'
            f'<text x="{x + 74}" y="{y + 46}" fill="#15171C" font-size="20" font-weight="600">{label}</text>'
            f'<text x="{x + 244}" y="{y + 46}" text-anchor="end" fill="#15171C" font-size="20" font-weight="700">{price}</text>'
            f'</g>')
    svg("play_feature_1024x500", 1024, 500, fg_shapes, extra="\n".join(extra))


if __name__ == "__main__":
    os.makedirs(DRAWABLE, exist_ok=True)
    os.makedirs(SVG_DIR, exist_ok=True)
    bg, fg = launcher()
    notification_icon()
    tile = logo_tile()
    svg("logo_tile", 108, 108, tile)
    splash_avd()
    ill_privacy()
    ill_reminders()
    ill_insights()
    ill_empty()
    store_assets(bg, fg)
    print("assets written")
