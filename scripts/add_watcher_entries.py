"""Add the two generated/special Watcher cards and their archetype tags.

The community card dump omits Miracle (a token) and Devotion, and the mantra
cards were tagged only by their secondary effect.  Without these tags the
Watcher mantra package looks empty to DeckSummary and the card reward advisor.
Run from the project root; the JSON is rewritten as UTF-8.
"""
import io
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "resources" / "bigfish" / "cards.json"

with io.open(PATH, encoding="utf-8") as f:
    data = json.load(f)
cards = data["cards"]


def add_tags(key, *tags):
    card = cards.setdefault(key, {"s": 5.0, "n": "", "t": []})
    current = card.setdefault("t", [])
    for tag in tags:
        if tag not in current:
            current.append(tag)


add_tags("Worship", "mantra", "retain")
add_tags("Pray", "mantra", "draw")
add_tags("Prostrate", "mantra", "zero", "block")
add_tags("Devotion", "mantra", "power")
add_tags("Collect", "miracle", "energy")
add_tags("DeceiveReality", "miracle", "block", "energy")

if "Miracle" not in cards:
    cards["Miracle"] = {
        "s": 6.0,
        "n": "0 费、保留、给 1 能量；来自纯水/收集，不缺能量时可以留到进愤怒/神性前。",
        "t": ["zero", "energy", "retain", "miracle"],
        "tier": "C",
    }
if "Devotion" not in cards:
    cards["Devotion"] = {
        "s": 6.8,
        "n": "每回合开始获得 2 点真言；真言到 10 进神性，是慢速神性流的引擎。",
        "t": ["mantra", "power"],
        "tier": "B",
    }

with io.open(PATH, "w", encoding="utf-8", newline="\n") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
    f.write("\n")

print("cards:", len(cards))
for key in ("Worship", "Pray", "Prostrate", "Devotion", "Miracle", "Collect", "DeceiveReality"):
    print(key, cards[key]["t"])
