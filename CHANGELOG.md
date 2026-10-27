# Changelog

## 1.0.0

- First public build.
- Map route planner with a highlighted route and step numbers.
- Card reward scoring with deck diagnosis, skip advice, and per-card reasons.
- Neow/event choice advice with recommended option, short verdicts for every option, and reasons.
- Campfire advice: rest/smith/dig/lift/toke/recall recommendation, plus the best card to upgrade and a gold marker in the upgrade grid.
- Compact speech bubble by default; click +/title to expand full reasons, click − to collapse.
- Drag the bubble by its title; position is saved. Fixed the per-frame default-position reset that made dragging appear broken.
- The small idle/hidden fish is now independently draggable and stores its own position (`fishX`/`fishY`).
- Card removal advice (campfire Toke/event/shop purge): recommends which card to remove and marks it with a red frame in the grid.
- Shop advisor: buy card/relic, purge, or skip, with prices and gold-aware reasoning.
- Potion advisor: use-now vs save-for-elite/Boss, including emergency/lethal checks.
- Card-select advisor for put-on-top/bottom, discard, exhaust, tutor, copy, transform, and upgrade screens.
- Act 1 route scoring now values early normal fights for deck growth.
- Card base scores blended with SpireSpy community tier lists (284 cards matched), with `tier` shown in reward/hover text.
- Character-specific archetype detection and anti-synergy penalties (poison vs shiv, focus vs claw, etc.).
- Shop priorities rechecked against Wiki prices; skip threshold tightened and purge valued by deck junk.
- Added 145 community relic tiers for shop comparisons.
- Expanded every advisor with a detailed "ledger": combat math/comparison, all three reward cards, full route breakdown, every event option, every campfire option, shop shelf, card-select alternatives, and per-potion advice.
- Removed literal `...` from route/event/campfire text and compact mode; panel size now measures content instead of a fixed huge size.
- Removed body ellipsis entirely. If a full-screen panel still cannot hold all text, it paginates (`1/2`, `<<`, `>>`) so every line remains reachable.
- Rewrote advice in a gentle maid voice; removed "主人别急" phrasing.
- Added per-enemy profiles: current intent, passives, common patterns and counters, with community nicknames.
- Added package commitment: off-archetype commons are penalized after 5 matching cards, and skipped in thick decks.
- Added relic-aware combat: energy/attribute relics, Pen Nib/Kunai/Shuriken/Letter Opener counters, Necronomicon/Chemical X/Dead Branch/Mummified Hand/Bird-Faced Urn/Tungsten Rod synergy.
- Added boss relic three-choice advice (effect, downside, deck fit) and shop relic fit weighting.
- Compact combat bubble now includes urgent potion use together with the recommended card (e.g. "先喝能量药水，再打...").
- Boss relic screen now has a dedicated advisor mode with the recommended relic circled.
- Added boss/elite scouting to map and card rewards (AOE vs Slime Boss/Collector, block vs Guardian/Heart, attack vs Nob, etc.).
- Potion sequencing now respects affordable cards: Flex/Strength/Duplication only recommended with a playable payoff; temporary buffs are never advised after the attack.
- Added hand-retention reminders for Runic Pyramid and Retain cards.
- Fixed upgraded-card handling: smith advice excludes `upgraded`/`timesUpgraded > 0`, reward notes stop saying "还没升级", and upgraded cards get an explicit "已升级" marker.
- Act 2 survival: block threshold, AOE check, and contextual scoring for Barricade/Body Slam, Power Through, Impervious, Flame Barrier, Feel No Pain, Corruption + Dark Embrace.
- Act 1 route warns when a path has too few normal fights or zero elites.
- Fixed Neow/all-event option detection: read the event's instance `roomEventText`, not only the static RoomEventDialog; one-option events no longer disappear, unknown events use generic scoring.
- Added GUIDES.md documenting sources, price tables, route logic and limitations.
- Reading mode enlarged to 72% width / 84% height; long text auto-fits instead of ellipsizing.
- Removed the thin top highlight line and added a framed, layered panel style.
- Combat advisor with recommended card, target, lethal range, block/energy explanations.
- Combat lookahead for 0-cost energy cards (Bloodletting/Seeing Red/Offering/etc.):
  no longer ends the turn when selling HP for energy enables a lethal attack.
- Removed the false "always end turn at 0 energy" rule so 0-cost plays are considered.
- Dynamic speech-bubble height so long explanations are not clipped.
- Local `mts-launcher.jar` shim: Steam's **Play with Mods** now opens the local
  ModTheSpire selector instead of demanding a Steam Workshop install.
- Desktop shortcut and uninstaller restore path for the original `mts-launcher.jar`.
- Fish speech bubble with Chinese/English card names and a main-menu welcome.
- F8 toggle, close button, hidden fish tab, and persisted settings.
- No BaseMod/StSLib dependency; ModTheSpire only.
