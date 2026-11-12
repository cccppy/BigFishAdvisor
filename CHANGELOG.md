# Changelog

## 1.2.4

- Removal safety: shop/campfire/card-select removal now uses the game's own purgeability rules and never recommends Ascender's Bane, Necronomicurse, or Curse of the Bell. If no disposable card exists, it says "don't remove anything" instead of sacrificing a core card to fill the slot.
- Curse choices are audited as a deck cost, not a HP trade: event scoring now distinguishes "add a curse" from "remove a curse", lowers max-HP/heal weights, and applies a larger penalty when the deck has no reliable removal, exhaust package, Omamori, or curse payoff. It will explicitly warn against taking a curse just for max HP or a relic.
- Text cleanup: `#9` and all short StS color codes (`#b/#g/#r/#y/#p/#gold`) are stripped from event and potion text, and repeated whitespace is collapsed, so numbers are no longer mixed with formatting fragments.
- Boss preparation: map and card-reward advice now includes a deck check (attack / block / AOE / curses). It warns when block is below 5, AOE is missing in a 15+ card deck, the deck is 26+ cards, or curses are piling up, instead of only explaining the boss pattern.
- Kept 1.2.3: cross-turn setup, X-cost modeling, boss target ring fix, Gambling Chip finite discard, retain choice with remaining slots, lethal leader priority, and single-target special-group focus.

## 1.2.3

- Cross-turn setup is now scored as a real plan, not a flat "power" bonus: Phantasmal Killer, Nightmare, Doppelganger, Predator, Burst, Well-Laid Plans, Wraith Form, Echo Form, Biased Cognition, Electrodynamics, Loop, Demon Form, Limit Break, Barricade, Corruption and similar cards get a higher score on a safe turn and a lower score when the incoming damage is dangerous. The panel explains "set up now, cash in next turn" and the estimated follow-up damage.
- X-cost cards now use the actual X value: `energy + 2` with Chemical X, including playing an X-cost card at 0 energy for the Chemical X bonus. Whirlwind, Skewer, Malaise, Reinforced Body, Tempest, Multi-Cast, Doppelganger, Collect, Conjure Blade and Transmutation have per-point or per-effect estimates instead of a fixed tag bonus.
- Boss target marker reduced: the gold ring now uses a small capped radius and a faint glow, and its label moves beside the boss instead of covering intent, HP or buffs.
- Gambling Chip / any-number discard gets a finite plan: it names the exact cards to discard (curses/statuses, Tactician/Reflex, at most two basic cards), lists cards to keep, and stops recommending after the planned count with "够了：点确认". It no longer walks the player through the whole hand one card at a time.
- Retain selection now recommends specific cards from the current fight situation, respects the remaining number of slots, and can say "这次不用保留" when nothing is worth retaining. This applies to every "保留" selection screen, not just Well-Laid Plans; automatic retain effects such as Runic Pyramid/Equilibrium have no selection screen.
- Elite leader fights only prioritize the leader when the current hit can actually kill it and end the encounter; otherwise the panel goes back to clearing the minions first.
- Special group focus now also controls the actual target of single-target attacks, not only AOE labels: Awakened One cultists, Collector torch heads, Bronze Automaton orbs, Reptomancer daggers and other special groups are targeted in their proper order.

## 1.2.2

- Character-specific combat reads for all four characters. Watcher: Calm/Wrath/Divinity entries and exits, Mantra, Miracle timing and source, conditional stance cards, and attack estimates that do not multiply the current stance twice. Defect: orb slots, rightmost orb, Focus, channel/evoke/remove, Fission, Dualcast, Multi-Cast, Tempest, Barrage. Silent: poison timing, Catalyst/Bane, shivs, discard payoffs, and Finisher counting attacks actually played this turn. Ironclad: Strength, Body Slam, Barricade, exhaust, self-damage, temporary Flex, and Spot Weakness requiring an attacking target.
- Group-fight focus orders, not just "highest HP": sentries, byrds, gremlin gang, Gremlin Leader, Slavers, Bronze Automaton, Collector, Awakened One, Reptomancer, Spire Shield/Spear, bandits, shapes, louses, fungi, cultists, thieves, and slime splits now get a priority target plus an explicit 1→2→3 order.
- Special single-enemy notes for Spheric Guardian, Shelled Parasite, Snecko, Nemesis, Giant Head, Writhing Mass, Bronze Orb, Torch Head, Dagger, Taskmaster and others.
- Potion reward screen: when all potion slots are full, compares the new potion with the current three, recommends which to drop/drink or whether to skip it, and warns that claiming with full slots destroys the new potion in the base game. The mod never discards or claims automatically.
- Existing 1.2.1 fixes remain: vulnerability setup before follow-up attacks, potion advice paired with the highlighted card, current-cost calculations, and the two-line compact / 82%-height expanded panel.

## 1.2.1

- Combat order: when a vulnerability card (e.g. Thunderclap) can raise the damage of attacks still playable this turn, it is now recommended first, with the follow-up card named in the same sentence. The estimate respects current energy and sums AOE damage across every target that is not yet vulnerable.
- Potion/card consistency: strength, duplication, speed and other potion advice now pairs with the same card the combat panel highlights. Speed potion no longer recommends a block card while the panel recommends an attack.
- Compact panel: the collapsed bubble is capped at two wrapped lines instead of five, so it no longer grows into a tall card over the player's buff icons. Click `+` or the title to read the full text.
- Expanded panel: width/height are capped at 86%/82% of the screen; overflow still uses the existing page buttons, never an ellipsis.
- Current-cost calculations: combat affordability and lookahead use `costForTurn`, so Snecko Eye/Confusion, Corruption, Mummified Hand and similar in-combat modifiers are respected; X-cost cards are handled separately.
- Necronomicon now checks the card's current cost (or X energy spent), not its printed cost.
- Card rewards with Snecko Eye/Confusion use an expected-value bonus based on printed cost and show a short reminder, instead of the old fixed high-cost penalty.

## 1.2.0

- Neow opening choices: fixed empty option labels and the panel disappearing after the first dialogue.
- Neow panel is drawn from `NeowRoom.renderAboveTopPanel` so the opening dialogue cannot cover it.
- Map screen: stop rendering the Neow panel after leaving the room, so route advice and route highlighting are visible again.
- F8 now toggles reliably: hidden once, pressed again restores the assistant.
- Search keywords added to the mod metadata and README: 杀戮尖塔、Slay the Spire、Mod、中文助手、新手、路线、抓牌、流派、卡组、战斗.

## 1.1.0

- Added a deck-building stage model: foundation → find direction → core → refine → finish.
- Early Act 1 now prioritizes immediately usable damage/block/AOE/draw and warns against "naked" core cards before their enablers exist.
- Act 1 late / Act 2 now checks whether 2+ enablers exist before recommending a core, and penalizes switching direction without a real pivot.
- Added character-specific core/enabler recognition for Ironclad, Silent, Defect and Watcher.
- Card reward and map panels now show the current stage, direction, and a concrete next-step plan.
- Renamed the in-game mod title to **杀戮尖塔大肥鱼助手** and bumped to 1.1.0.

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
