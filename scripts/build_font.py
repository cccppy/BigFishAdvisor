#!/usr/bin/env python3
"""Rebuild the bundled Chinese font subset from a full TTF.

Usage:
    python scripts/build_font.py [--font assets_src/font1.ttf] [--root .]

Requires Python 3 and fonttools:
    pip install fonttools

The game uses a small glyph subset to keep the jar small. Whenever a Java
string, cards.json note or names_zh.json entry adds a new Chinese character,
run this script and then scripts/validate_assets.py.
"""
from __future__ import annotations

import argparse
import glob
import os
import sys

try:
    from fontTools import subset
    from fontTools.ttLib import TTFont
except ImportError:
    print("fonttools is required: pip install fonttools", file=sys.stderr)
    raise SystemExit(2)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--font", default="")
    parser.add_argument("--root", default="")
    args = parser.parse_args()

    root = os.path.abspath(args.root) if args.root else os.path.dirname(
        os.path.dirname(os.path.abspath(__file__))
    )
    font_path = os.path.abspath(args.font) if args.font else os.path.join(
        root, "assets_src", "font1.ttf"
    )
    if not os.path.isfile(font_path):
        print(f"font not found: {font_path}", file=sys.stderr)
        return 2

    chars: set[str] = set()
    for path in glob.glob(os.path.join(root, "src", "**", "*.java"), recursive=True):
        with open(path, encoding="utf-8") as fh:
            chars.update(fh.read())
    for path in (
        os.path.join(root, "resources", "bigfish", "cards.json"),
        os.path.join(root, "resources", "bigfish", "names_zh.json"),
    ):
        if os.path.isfile(path):
            with open(path, encoding="utf-8") as fh:
                chars.update(fh.read())

    chars = {c for c in chars if ord(c) >= 32 and c not in "\r\n\t"}
    chars.update(chr(i) for i in range(32, 127))

    full = TTFont(font_path)
    cmap: set[int] = set()
    for table in full["cmap"].tables:
        cmap.update(table.cmap.keys())
    present = {c for c in chars if ord(c) in cmap}
    missing = "".join(sorted(chars - present))
    if missing:
        print(f"warning: missing glyphs: {missing}", file=sys.stderr)

    out_charset = os.path.join(root, "resources", "bigfish", "charset.txt")
    out_font = os.path.join(root, "resources", "bigfish", "font.ttf")
    with open(out_charset, "w", encoding="utf-8") as fh:
        fh.write("".join(sorted(present)))

    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    options.recalc_bounds = True
    options.drop_tables += ["DSIG"]
    font = subset.load_font(font_path, options)
    subsetter = subset.Subsetter(options=options)
    subsetter.populate(text="".join(sorted(present)))
    subsetter.subset(font)
    subset.save_font(font, out_font, options)
    print(f"charset: {len(present)} chars, missing: {len(missing)}")
    print(f"wrote {out_font} ({os.path.getsize(out_font)} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
