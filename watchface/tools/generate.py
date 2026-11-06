#!/usr/bin/env python3
"""Genera las manecillas (PNG) y res/raw/watchface.xml de la carátula. Sin dependencias.

Uso: python3 watchface/tools/generate.py  (desde la raíz del repositorio)
"""
import math
import pathlib
import struct
import zlib

RES = pathlib.Path(__file__).resolve().parent.parent / "src/main/res"
SIZE = 450          # Lienzo de la carátula.
CENTER = SIZE / 2
SCALE = 3           # Las manecillas se dibujan a 3x para que queden nítidas.
WHITE = (255, 255, 255)
VIOLET = (140, 112, 255)  # Acento de la app, un poco más claro para que se lea sobre negro.


def capsule(px, py, x, y0, y1, half):
    """Distancia con signo a una cápsula vertical."""
    return math.hypot(px - x, py - min(max(py, y0), y1)) - half


def write_png(path, width, height, pixels):
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))

    raw = b"".join(b"\0" + bytes(pixels[y * width * 4:(y + 1) * width * 4]) for y in range(height))
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def hand(name, width, height, pivot_y, shapes):
    """shapes: (desde, hasta, media anchura, color), medidas en px de carátula desde el pivote hacia la punta."""
    w, h = width * SCALE, height * SCALE
    cx, cy = w / 2, pivot_y * SCALE
    pixels = bytearray(w * h * 4)
    for y in range(h):
        for x in range(w):
            for start, end, half, color in shapes:
                d = capsule(x + 0.5, y + 0.5, cx, cy - end * SCALE, cy - start * SCALE, half * SCALE)
                alpha = max(0.0, min(1.0, 0.5 - d))
                if alpha > 0:
                    # Cada forma se pinta encima de la anterior.
                    i = (y * w + x) * 4
                    under = pixels[i + 3] / 255
                    out = alpha + under * (1 - alpha)
                    for k in range(3):
                        pixels[i + k] = round((color[k] * alpha + pixels[i + k] * under * (1 - alpha)) / out)
                    pixels[i + 3] = round(out * 255)
    write_png(RES / "drawable-nodpi" / f"{name}.png", w, h, pixels)


BLACK = (0, 0, 0)
HALO = 2.5  # Borde negro: separa la manecilla de lo que tenga debajo (fecha, km, otra manecilla).

HOUR = dict(width=20, height=132, pivot=124)
MINUTE = dict(width=16, height=196, pivot=188)
SECOND = dict(width=10, height=252, pivot=202)

# Vástago fino que sale del centro y cuerpo alargado, separado del centro.
hand("hand_hour", HOUR["width"], HOUR["height"], HOUR["pivot"], [(0, 34, 1.6 + HALO, BLACK), (34, 116, 5.5 + HALO, BLACK), (0, 34, 1.6, WHITE), (34, 116, 5.5, WHITE)])
hand("hand_minute", MINUTE["width"], MINUTE["height"], MINUTE["pivot"], [(0, 34, 1.6 + HALO, BLACK), (34, 180, 4.0 + HALO, BLACK), (0, 34, 1.6, WHITE), (34, 180, 4.0, WHITE)])
hand("hand_second", SECOND["width"], SECOND["height"], SECOND["pivot"], [(-46, 198, 0.95, VIOLET), (-46, -18, 2.6, VIOLET)])


def ticks():
    lines = []
    for i in range(60):
        if i % 5 == 0:
            continue
        a = math.radians(i * 6)
        s, c = math.sin(a), math.cos(a)
        lines.append(line(s, c, 207, 214, 1.5, "#ff55555f"))
    minor = "\n".join(lines)
    major = "\n".join(
        line(math.sin(math.radians(i * 30)), math.cos(math.radians(i * 30)), 178, 214, 3.5, "#ffffffff")
        for i in range(12)
    )
    return minor, major


def line(s, c, r0, r1, thickness, color):
    return (
        f'        <Line startX="{CENTER + s * r0:.2f}" startY="{CENTER - c * r0:.2f}" '
        f'endX="{CENTER + s * r1:.2f}" endY="{CENTER - c * r1:.2f}">'
        f'<Stroke color="{color}" thickness="{thickness}" cap="ROUND"/></Line>'
    )


DAYS = ["DOM", "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"]  # [DAY_OF_WEEK]: domingo = 1.


def date_parts():
    expressions = "\n".join(
        f'          <Expression name="d{i + 1}"><![CDATA[[DAY_OF_WEEK] == {i + 1}]]></Expression>'
        for i in range(7)
    )
    compares = "\n".join(
        f'''        <Compare expression="d{i + 1}">
          <PartText x="125" y="118" width="200" height="40">
            <Text align="CENTER"><Font family="SYNC_TO_DEVICE" size="27" weight="SEMI_BOLD" color="#ffffffff"><Template>{day} %d<Parameter expression="[DAY]"/></Template></Font></Text>
          </PartText>
        </Compare>'''
        for i, day in enumerate(DAYS)
    )
    return f"""      <Condition>
        <Expressions>
{expressions}
        </Expressions>
{compares}
        <Default/>
      </Condition>"""


def clock_hand(tag, spec, extra=""):
    x = CENTER - spec["width"] / 2
    y = CENTER - spec["pivot"]
    pivot_x, pivot_y = 0.5, spec["pivot"] / spec["height"]
    resource = {"HourHand": "hand_hour", "MinuteHand": "hand_minute", "SecondHand": "hand_second"}[tag]
    head = (
        f'<{tag} resource="{resource}" x="{x:g}" y="{y:g}" width="{spec["width"]}" height="{spec["height"]}" '
        f'pivotX="{pivot_x}" pivotY="{pivot_y:.5f}"'
    )
    return f"{head}>{extra}</{tag}>" if extra else f"{head}/>"


COMPLICATION_TEXT = """          <PartText x="0" y="26" width="100" height="48">
            <Text align="CENTER"><Font family="SYNC_TO_DEVICE" size="27" weight="BOLD" color="#ffffffff"><Template>%s<Parameter expression="[COMPLICATION.TEXT]"/></Template></Font></Text>
          </PartText>"""

minor, major = ticks()
xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por watchface/tools/generate.py: cambia ese archivo, no este. -->
<WatchFace width="{SIZE}" height="{SIZE}">
  <Metadata key="CLOCK_TYPE" value="ANALOG"/>
  <Metadata key="PREVIEW_TIME" value="10:08:32"/>
  <Scene backgroundColor="#ff000000">
    <PartDraw name="minutos" x="0" y="0" width="{SIZE}" height="{SIZE}">
{minor}
      <Variant mode="AMBIENT" target="alpha" value="0"/>
    </PartDraw>
    <PartDraw name="horas" x="0" y="0" width="{SIZE}" height="{SIZE}">
{major}
      <Variant mode="AMBIENT" target="alpha" value="150"/>
    </PartDraw>
    <Group name="fecha" x="0" y="0" width="{SIZE}" height="{SIZE}">
{date_parts()}
    </Group>
    <ComplicationSlot slotId="0" x="170" y="285" width="110" height="110" supportedTypes="RANGED_VALUE SHORT_TEXT EMPTY" isCustomizable="TRUE">
      <DefaultProviderPolicy defaultSystemProvider="WATCH_BATTERY" defaultSystemProviderType="RANGED_VALUE" primaryProvider="com.tanqueo/com.tanqueo.complication.RangeComplicationService" primaryProviderType="RANGED_VALUE"/>
      <BoundingOval x="0" y="0" width="110" height="110"/>
      <Complication type="RANGED_VALUE">
        <PartDraw x="5" y="5" width="100" height="100">
          <Arc centerX="50" centerY="50" width="92" height="92" startAngle="-135" endAngle="135" direction="CLOCKWISE"><Stroke color="#ff2a2a30" thickness="7" cap="ROUND"/></Arc>
          <Arc centerX="50" centerY="50" width="92" height="92" startAngle="-135" endAngle="135" direction="CLOCKWISE">
            <Transform target="endAngle" value="-135 + 270 * ([COMPLICATION.RANGED_VALUE_VALUE] - [COMPLICATION.RANGED_VALUE_MIN]) / ([COMPLICATION.RANGED_VALUE_MAX] - [COMPLICATION.RANGED_VALUE_MIN])"/>
            <Stroke color="{"#%02x%02x%02x%02x" % (255, *VIOLET)}" thickness="7" cap="ROUND"/>
          </Arc>
        </PartDraw>
        <Group x="5" y="5" width="100" height="100">
{COMPLICATION_TEXT}
        </Group>
      </Complication>
      <Complication type="SHORT_TEXT">
        <Group x="5" y="5" width="100" height="100">
{COMPLICATION_TEXT}
        </Group>
      </Complication>
      <Complication type="EMPTY"/>
    </ComplicationSlot>
    <AnalogClock x="0" y="0" width="{SIZE}" height="{SIZE}">
      {clock_hand("HourHand", HOUR)}
      {clock_hand("MinuteHand", MINUTE)}
      {clock_hand("SecondHand", SECOND, '<Variant mode="AMBIENT" target="alpha" value="0"/><Tick duration="0.18" strength="0.6"/>')}
    </AnalogClock>
    <PartDraw name="centro" x="{CENTER - 8:g}" y="{CENTER - 8:g}" width="16" height="16">
      <Ellipse x="0" y="0" width="16" height="16"><Fill color="#ffffffff"/></Ellipse>
      <Ellipse x="4.5" y="4.5" width="7" height="7"><Fill color="#ff000000"/></Ellipse>
    </PartDraw>
  </Scene>
</WatchFace>
"""
(RES / "raw" / "watchface.xml").write_text(xml, encoding="utf-8")
