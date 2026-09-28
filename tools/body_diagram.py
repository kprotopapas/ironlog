# Generates the body diagram: body.svg (a preview) and BodyShapes.kt (the shapes the app draws).
# Run from this folder, then copy BodyShapes.kt to app/src/main/java/com/example/gymtracker/ui/.

import math

# Figure space: x 0..100 (centre 50), y 0..220. Right-side parts are mirrored from the left.
def out(pts, dx=-1.5): return [(x + dx, y) for x, y in pts]

def mirror(pts): return [(100 - x, y) for x, y in reversed(pts)]

def rounded(pts, r=3.0):
    """Closed polygon with rounded corners (quadratic curve through each vertex)."""
    n = len(pts); d = []
    for i in range(n):
        p0, p1, p2 = pts[i - 1], pts[i], pts[(i + 1) % n]
        def toward(a, b, dist):
            l = math.dist(a, b); t = min(dist / l, 0.5) if l else 0
            return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
        a = toward(p1, p0, r); b = toward(p1, p2, r)
        d.append(("M" if i == 0 else "L") + f"{a[0]:.1f},{a[1]:.1f} Q{p1[0]:.1f},{p1[1]:.1f} {b[0]:.1f},{b[1]:.1f}")
    return " ".join(d) + " Z"

# ── Shared outer pieces ──
HEAD = "M50,3 C57,3 60,9 60,16 C60,24 56,29 50,29 C44,29 40,24 40,16 C40,9 43,3 50,3 Z"
FOREARM = out([(18, 89), (27, 89), (26, 104), (23, 118), (17, 117), (16, 103)])
HAND = out([(17, 121), (23, 121), (23, 131), (19, 133), (16, 128)])
SHIN = [(37, 163), (47, 163), (46, 184), (44, 205), (39, 205), (36, 184)]
FOOT = [(38, 208), (45, 208), (47, 215), (37, 215)]

# ── Front ──
FRONT = {
    "Shoulders": [out([(35, 33), (38, 36), (34, 50), (29, 55), (21, 56), (21, 45), (26, 37)])],
    "Chest":     [[(39.5, 33), (60.5, 33), (61.5, 36.5), (65.5, 50), (63, 59), (50, 61.5), (37, 59), (34.5, 50), (38.5, 36.5)]],
    "Core":      [[(37, 63), (50, 65.5), (63, 63), (63, 90), (50, 95), (37, 90)]],
    "Arms":      [out([(21, 59), (29, 58), (32, 64), (28, 86), (19, 86), (19, 70)]), FOREARM],
    "Legs":      [[(36, 110), (49, 117), (48, 140), (46, 160), (37, 160), (34, 135)], SHIN],
    "_neutral":  [[(36, 94), (50, 99), (64, 94), (66, 107), (50, 114), (34, 107)], HAND, FOOT],
}
# ── Back ──
BACK = {
    "Shoulders": [[(43, 31), (57, 31), (64, 36), (50, 42), (36, 36)],                       # traps
                  out([(35, 36), (37, 39), (33, 51), (28, 55), (21, 56), (21, 45), (27, 38)])],   # rear delt
    "Back":      [[(38, 41), (50, 45), (62, 41), (67, 49), (64, 72), (61.5, 88), (38.5, 88), (36, 72), (33, 49)]],
    "Arms":      [out([(21, 59), (29, 58), (32, 64), (28, 86), (19, 86), (19, 70)]), FOREARM],   # triceps
    "Core":      [[(38, 91), (62, 91), (64.5, 98), (50, 101.5), (35.5, 98)]],                      # lower back
    "Legs":      [[(35, 103), (49, 104.5), (49, 120), (33.5, 117.5)],                              # glute
                  [(35, 122), (49, 123.5), (48, 141), (46, 160), (37, 160), (34, 138)],      # hamstring
                  SHIN],                                                                     # calf
    "_neutral":  [HAND, FOOT],
}

SUCCESS, DANGER = "#15803D", "#DC2626"
STATES = {"Chest": "up", "Back": "up", "Shoulders": "down", "Arms": "up"}   # Legs, Core: stay / no data

def style(group):
    s = STATES.get(group)
    if group == "_neutral": return 'fill="none" stroke="#E2E8F0" stroke-width="1"'
    if s == "up":   return f'fill="{SUCCESS}" fill-opacity="0.2" stroke="{SUCCESS}" stroke-width="1.3"'
    if s == "down": return f'fill="{DANGER}" fill-opacity="0.2" stroke="{DANGER}" stroke-width="1.3"'
    return 'fill="#F1F5F9" stroke="#CBD5E1" stroke-width="1.1"'

def figure(parts, dx, label):
    out = [f'<g transform="translate({dx},18)">',
           f'<path d="{HEAD}" fill="none" stroke="#E2E8F0" stroke-width="1"/>']
    for group, shapes in parts.items():
        for pts in shapes:
            for poly in ([pts] if pts in (HAND, FOOT) and False else [pts, mirror(pts)] if pts[0][0] < 50 and max(x for x, _ in pts) <= 50 else [pts]):
                out.append(f'<path d="{rounded(poly)}" {style(group)} stroke-linejoin="round"/>')
        # A glyph on one piece of each changed group, so state never relies on colour alone.
        s = STATES.get(group)
        if s and group != "_neutral":
            pts = shapes[0]
            if max(x for x, _ in pts) <= 50: pts = mirror(pts)  # label the right-hand copy
            cx = sum(x for x, _ in pts) / len(pts); cy = sum(y for _, y in pts) / len(pts)
            out.append(f'<text x="{cx:.1f}" y="{cy + 2.2:.1f}" font-size="6.5" text-anchor="middle" '
                       f'fill="{SUCCESS if s == "up" else DANGER}" font-family="sans-serif">{"▲" if s == "up" else "▼"}</text>')
    out.append(f'<text x="50" y="232" font-size="7" text-anchor="middle" fill="#64748B" '
               f'font-family="monospace" letter-spacing="1.5">{label}</text></g>')
    return "\n".join(out)

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 262" width="720" height="786">
<rect width="240" height="262" fill="#FFFFFF"/>
{figure(FRONT, 10, "FRONT")}
{figure(BACK, 130, "BACK")}
</svg>'''
open("body.svg", "w").write(svg)

# ── Kotlin export: the same shapes, one SVG path string per piece ──
def pieces(parts):
    rows, glyphs = [], {}
    for group, shapes in parts.items():
        g = "" if group == "_neutral" else group
        for pts in shapes:
            polys = [pts, mirror(pts)] if max(x for x, _ in pts) <= 50 else [pts]
            rows += [(g, rounded(p)) for p in polys]
        if g:
            pts = shapes[0]
            if max(x for x, _ in pts) <= 50: pts = mirror(pts)
            glyphs[g] = (sum(x for x, _ in pts) / len(pts), sum(y for _, y in pts) / len(pts))
    return rows, glyphs

def kt_view(name, parts):
    rows, glyphs = pieces(parts)
    body = ",\n".join(f'        BodyPart("{g}", "{d}")' for g, d in [("", HEAD)] + rows)
    gl = ",\n".join(f'        "{g}" to Offset({x:.1f}f, {y:.1f}f)' for g, (x, y) in glyphs.items())
    return f'''val {name} = BodyView(
    parts = listOf(
{body},
    ),
    glyphAt = mapOf(
{gl},
    ),
)'''

open("BodyShapes.kt", "w").write(f'''package com.example.gymtracker.ui

import androidx.compose.ui.geometry.Offset

// Generated by tools/body_diagram.py: a 100 × 220 figure, one closed path per piece. Edit the script, not this file.
// Group names match exercise categories; "" is decoration (head, hips, hands, feet).

class BodyPart(val group: String, val path: String)

class BodyView(val parts: List<BodyPart>, val glyphAt: Map<String, Offset>)

{kt_view("FrontView", FRONT)}

{kt_view("BackView", BACK)}
''')
