# Contributing

Thanks for helping the fish!

## Good first contributions

- Improve a card note or score in `resources/bigfish/cards.json` and explain the reasoning.
- Add Chinese/English name mappings or fix a translation.
- Add a new heuristic for a specific archetype (Strength, Exhaust, Poison, Shivs, Orbs, Stance).
- Improve UI positioning for unusual resolutions or controller mode.
- Report a crash with `sendToDevs/logs/SlayTheSpire.log` and the exact game state.

## Design rules

1. **Explain, don't just command.** Every recommendation should have a short reason.
2. **No auto-play.** The mod must never click, play cards, or choose rewards for the player.
3. **Fail safe.** Any UI or advisor exception must be caught; never crash a run.
4. **No game assets.** Do not commit `desktop-1.0.jar`, extracted art, audio, or localization dumps beyond the small name map needed for bilingual UI.
5. **No hidden telemetry.** No network calls from the mod.

## Build

```powershell
.\build.ps1 -GameDir "D:\steam\steamapps\common\SlayTheSpire" -MtsJar ".\tools\ModTheSpire.jar"
```

The build needs JDK 8 and a legally owned copy of the game (compile-time API only).

## Pull requests

- Keep changes focused.
- If you change a score, include a one-line rationale and the situation where it applies.
- Test at at least 1280x720 and 1920x1080 if you change UI.
- Do not bump the version in multiple files; `resources/ModTheSpire.json` is the source of truth for releases.
