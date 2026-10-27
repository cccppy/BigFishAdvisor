#!/usr/bin/env python3
"""Small, dependency-light validation for the data files shipped with the mod."""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "resources" / "bigfish"
errors = []


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"{path}: {exc}")
        return {}


cards = load(RES / "cards.json").get("cards", {})
names = load(RES / "names_zh.json")
if len(cards) < 100:
    errors.append(f"cards.json has only {len(cards)} cards")
if len(names) < 100:
    errors.append(f"names_zh.json has only {len(names)} names")

for key, card in cards.items():
    if not isinstance(card, dict):
        errors.append(f"{key}: not an object")
        continue
    score = card.get("s")
    if not isinstance(score, (int, float)) or not 0 <= score <= 10:
        errors.append(f"{key}: score out of range: {score!r}")
    note = card.get("n", "")
    if not isinstance(note, str) or len(note) < 4:
        errors.append(f"{key}: missing note")
    tags = card.get("t", [])
    if not isinstance(tags, list):
        errors.append(f"{key}: tags must be a list")

required = ["avatar.png", "avatar_small.png", "panel.png", "ring.png", "glow.png",
            "font.ttf", "charset.txt", "cards.json", "names_zh.json"]
for name in required:
    if not (RES / name).exists():
        errors.append(f"missing resource: {name}")

charset = (RES / "charset.txt").read_text(encoding="utf-8") if (RES / "charset.txt").exists() else ""
# Every runtime string drawn by the UI must be available in the subset font.
runtime_text = []
for path in list((ROOT / "src").rglob("*.java")):
    runtime_text.append(path.read_text(encoding="utf-8"))
runtime_text.append(json.dumps(names, ensure_ascii=False))
runtime_text.append(json.dumps(cards, ensure_ascii=False))
all_chars = set("".join(runtime_text))
missing = sorted(c for c in all_chars if ord(c) >= 32 and c not in charset and c not in "\r\n\t")
if missing:
    # Comments sometimes contain rare characters; report but only fail for CJK.
    cjk_missing = [c for c in missing if "\u4e00" <= c <= "\u9fff"]
    if cjk_missing:
        errors.append("charset.txt missing CJK glyphs: " + "".join(cjk_missing[:50]))

for modfile in ["build.ps1", "build.sh", "install.ps1", "README.md", "LICENSE"]:
    if not (ROOT / modfile).exists():
        errors.append(f"missing project file: {modfile}")

if errors:
    print("Validation failed:")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print(f"OK: {len(cards)} cards, {len(names)} Chinese names, {len(charset)} font characters")
