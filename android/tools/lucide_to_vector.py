"""Convierte íconos SVG de Lucide a VectorDrawable de Android.

Uso (desde android/):  python tools/lucide_to_vector.py <carpeta con los .svg> app/src/main/res/drawable

Los íconos del diseño (Figma SENDA APP) son de Lucide (licencia ISC, third_party/lucide/LICENSE). Cada SVG se
convierte en res/drawable/ic_<nombre>.xml con trazo de 2 px, extremos y uniones redondeados. El color lo pone
quien lo usa (Icon con tint), por eso el trazo es negro opaco. Soporta path, circle, rect, line, polyline,
polygon y ellipse, que son los elementos que usa Lucide.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = "{http://www.w3.org/2000/svg}"


def fmt(value: float) -> str:
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return "0" if text in ("", "-0") else text


def circle(cx: float, cy: float, rx: float, ry: float) -> str:
    return (
        f"M{fmt(cx - rx)},{fmt(cy)}"
        f"a{fmt(rx)},{fmt(ry)} 0 1,0 {fmt(2 * rx)},0"
        f"a{fmt(rx)},{fmt(ry)} 0 1,0 {fmt(-2 * rx)},0z"
    )


def rect(x: float, y: float, w: float, h: float, rx: float, ry: float) -> str:
    if rx <= 0 and ry <= 0:
        return f"M{fmt(x)},{fmt(y)}H{fmt(x + w)}V{fmt(y + h)}H{fmt(x)}z"
    rx, ry = min(rx or ry, w / 2), min(ry or rx, h / 2)
    return (
        f"M{fmt(x + rx)},{fmt(y)}H{fmt(x + w - rx)}A{fmt(rx)},{fmt(ry)} 0 0 1 {fmt(x + w)},{fmt(y + ry)}"
        f"V{fmt(y + h - ry)}A{fmt(rx)},{fmt(ry)} 0 0 1 {fmt(x + w - rx)},{fmt(y + h)}"
        f"H{fmt(x + rx)}A{fmt(rx)},{fmt(ry)} 0 0 1 {fmt(x)},{fmt(y + h - ry)}"
        f"V{fmt(y + ry)}A{fmt(rx)},{fmt(ry)} 0 0 1 {fmt(x + rx)},{fmt(y)}z"
    )


def points(raw: str, close: bool) -> str:
    nums = [float(n) for n in raw.replace(",", " ").split()]
    pairs = list(zip(nums[0::2], nums[1::2]))
    path = f"M{fmt(pairs[0][0])},{fmt(pairs[0][1])}" + "".join(f"L{fmt(x)},{fmt(y)}" for x, y in pairs[1:])
    return path + ("z" if close else "")


def path_data(element: ET.Element) -> str | None:
    tag = element.tag.removeprefix(NS)
    a = element.attrib
    f = lambda key, default=0.0: float(a.get(key, default))  # noqa: E731
    if tag == "path":
        return a["d"]
    if tag == "circle":
        return circle(f("cx"), f("cy"), f("r"), f("r"))
    if tag == "ellipse":
        return circle(f("cx"), f("cy"), f("rx"), f("ry"))
    if tag == "rect":
        return rect(f("x"), f("y"), f("width"), f("height"), f("rx"), f("ry"))
    if tag == "line":
        return f"M{fmt(f('x1'))},{fmt(f('y1'))}L{fmt(f('x2'))},{fmt(f('y2'))}"
    if tag == "polyline":
        return points(a["points"], close=False)
    if tag == "polygon":
        return points(a["points"], close=True)
    return None


def convert(svg: Path) -> str:
    root = ET.parse(svg).getroot()
    view_w, view_h = root.attrib["viewBox"].split()[2:4]
    stroke = root.attrib.get("stroke-width", "2")
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Lucide «{svg.stem}» (ISC). Generado con tools/lucide_to_vector.py: no editar a mano. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        f'    android:viewportWidth="{view_w}"',
        f'    android:viewportHeight="{view_h}">',
    ]
    for element in root:
        data = path_data(element)
        if data is None:
            raise SystemExit(f"{svg.name}: elemento no soportado <{element.tag}>")
        lines += [
            "    <path",
            f'        android:pathData="{data}"',
            '        android:strokeColor="#FF000000"',
            f'        android:strokeWidth="{stroke}"',
            '        android:strokeLineCap="round"',
            '        android:strokeLineJoin="round" />',
        ]
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def main() -> None:
    source, target = Path(sys.argv[1]), Path(sys.argv[2])
    target.mkdir(parents=True, exist_ok=True)
    for svg in sorted(source.glob("*.svg")):
        out = target / f"ic_{svg.stem.replace('-', '_')}.xml"
        out.write_text(convert(svg), encoding="utf-8", newline="\n")
        print(out.name)


if __name__ == "__main__":
    main()
