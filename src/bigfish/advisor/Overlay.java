package bigfish.advisor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;

import java.util.ArrayList;
import java.util.List;

/** Owns the in-game fish bubble, the toggle, and the per-screen advice caches. */
public final class Overlay {
    private enum Mode {NONE, MAP, CARD, COMBAT, EVENT, CAMPFIRE, UPGRADE, PURGE, SHOP, HANDSELECT, CARDSELECT, BOSSRELIC}

    private final CombatAdvisor combatAdvisor = new CombatAdvisor();
    private final CardRewardAdvisor cardAdvisor = new CardRewardAdvisor();
    private final MapAdvisor mapAdvisor = new MapAdvisor();
    private final EventAdvisor eventAdvisor = new EventAdvisor();
    private final CampfireAdvisor campfireAdvisor = new CampfireAdvisor();
    private final ShopAdvisor shopAdvisor = new ShopAdvisor();
    private final CardSelectAdvisor cardSelectAdvisor = new CardSelectAdvisor();

    private Ui ui;
    private boolean initTried;
    private boolean failed;
    private boolean loggedError;
    private Mode mode = Mode.NONE;
    private long lastUpdate;
    private CombatAdvisor.Result combat;
    private CardRewardAdvisor.Result cards;
    private MapAdvisor.Result map;
    private EventAdvisor.Result event;
    private CampfireAdvisor.Result campfire;
    private ShopAdvisor.Result shop;
    private CardSelectAdvisor.Result cardSelect;
    private RelicAdvisor.BossChoice bossRelic;
    private AbstractCard hoveredCard;
    private long lastCombatSeen;

    private boolean dragging;
    private boolean dragMoved;
    private boolean mouseWasDown;
    private boolean mousePressedPanel;
    private float dragOffX;
    private float dragOffY;
    private float headerX;
    private float headerY;
    private float headerW;
    private float headerH;
    private float toggleX;
    private float toggleY;
    private float toggleSize;

    private float hiddenX;
    private float hiddenY;
    private float hiddenSize;
    private float fishSize;
    private boolean fishDragging;
    private boolean fishDragMoved;
    private float fishOffX;
    private float fishOffY;

    private int page;
    private int pageCount = 1;
    private int pageMaxLines;
    private String pageKey = "";
    private float pageNextX;
    private float pageNextY;
    private float pageNextW;
    private float pageNextH;
    private float pagePrevX;
    private float pagePrevY;
    private float pagePrevW;
    private float pagePrevH;
    private float closeX;
    private float closeY;
    private float closeSize;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    public void preUpdate() {
        if (BigFishAdvisor.config == null) {
            return;
        }
        ensureInit();
        if (failed || ui == null) {
            return;
        }
        try {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
                BigFishAdvisor.config.enabled = !BigFishAdvisor.config.enabled;
                dragging = false;
                fishDragging = false;
                BigFishAdvisor.config.save();
            }
            handleMouse();
        } catch (Throwable t) {
            logOnce(t);
        }
    }

    /**
     * Uses raw left-button state instead of InputHelper.justClickedLeft so the
     * game cannot consume the click before the bubble sees it.
     */
    private void handleMouse() {
        boolean down = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        float gw = Math.max(1, Gdx.graphics.getWidth());
        float gh = Math.max(1, Gdx.graphics.getHeight());
        float mx = Gdx.input.getX() * Settings.WIDTH / gw;
        float my = Settings.HEIGHT - Gdx.input.getY() * Settings.HEIGHT / gh;

        if (down && !mouseWasDown) {
            mouseWasDown = true;
            mousePressedPanel = false;
            if (fishSize > 0 && inside(mx, my, hiddenX, hiddenY, fishSize, fishSize)) {
                // The little fish is independently draggable, even while the
                // assistant is collapsed or hidden with F8.
                fishDragging = true;
                fishDragMoved = false;
                fishOffX = mx - hiddenX;
                fishOffY = my - hiddenY;
                mousePressedPanel = true;
            } else if (BigFishAdvisor.config.enabled && pageCount > 1
                    && inside(mx, my, pagePrevX, pagePrevY, pagePrevW, pagePrevH)) {
                page = (page - 1 + pageCount) % pageCount;
                mousePressedPanel = true;
            } else if (BigFishAdvisor.config.enabled && pageCount > 1
                    && inside(mx, my, pageNextX, pageNextY, pageNextW, pageNextH)) {
                page = (page + 1) % pageCount;
                mousePressedPanel = true;
            } else if (BigFishAdvisor.config.enabled
                    && toggleSize > 0 && inside(mx, my, toggleX, toggleY, toggleSize, toggleSize)) {
                BigFishAdvisor.config.expanded = !BigFishAdvisor.config.expanded;
                BigFishAdvisor.config.save();
                mousePressedPanel = true;
            } else if (BigFishAdvisor.config.enabled
                    && inside(mx, my, headerX, headerY, headerW, headerH)) {
                dragging = true;
                dragMoved = false;
                dragOffX = mx - panelX;
                dragOffY = my - panelY;
                mousePressedPanel = true;
            }
            if (mousePressedPanel) {
                consumeClick();
            }
        }

        if (down && fishDragging) {
            float nx = mx - fishOffX;
            float ny = my - fishOffY;
            if (Math.abs(nx - hiddenX) > 3 * ui.s || Math.abs(ny - hiddenY) > 3 * ui.s) {
                fishDragMoved = true;
            }
            hiddenX = clamp(nx, 6 * ui.s, Settings.WIDTH - fishSize - 6 * ui.s);
            hiddenY = clamp(ny, 6 * ui.s, Settings.HEIGHT - fishSize - 6 * ui.s);
            consumeClick();
        }

        if (down && dragging) {
            float nx = mx - dragOffX;
            float ny = my - dragOffY;
            if (Math.abs(nx - panelX) > 3 * ui.s || Math.abs(ny - panelY) > 3 * ui.s) {
                dragMoved = true;
            }
            panelX = clamp(nx, 6 * ui.s, Settings.WIDTH - panelW - 6 * ui.s);
            panelY = clamp(ny, 6 * ui.s, Settings.HEIGHT - panelH - 6 * ui.s);
            consumeClick();
        }

        if (!down && mouseWasDown) {
            mouseWasDown = false;
            if (fishDragging) {
                fishDragging = false;
                if (!fishDragMoved) {
                    if (!BigFishAdvisor.config.enabled) {
                        BigFishAdvisor.config.enabled = true;
                    }
                } else {
                    saveFishPosition();
                }
                BigFishAdvisor.config.save();
            }
            if (dragging) {
                dragging = false;
                if (!dragMoved) {
                    BigFishAdvisor.config.expanded = !BigFishAdvisor.config.expanded;
                } else {
                    savePanelPosition();
                }
                BigFishAdvisor.config.save();
            }
            if (mousePressedPanel) {
                consumeClick();
            }
            mousePressedPanel = false;
        }
    }

    public void update() {
        if (BigFishAdvisor.config == null) {
            return;
        }
        ensureInit();
        if (failed || ui == null || !BigFishAdvisor.config.enabled) {
            return;
        }
        try {
            Mode newMode = detectMode();
            long now = System.currentTimeMillis();
            if (newMode == Mode.COMBAT) {
                lastCombatSeen = now;
            }
            if (newMode == Mode.NONE && mode == Mode.COMBAT
                    && AbstractDungeon.getCurrRoom() != null
                    && AbstractDungeon.getCurrRoom().phase == com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT
                    && now - lastCombatSeen < 900) {
                return;
            }
            if (newMode != mode || now - lastUpdate > 220) {
                mode = newMode;
                lastUpdate = now;
                rebuild();
            }
        } catch (Throwable t) {
            logOnce(t);
        }
    }

    private Mode detectMode() {
        if (AbstractDungeon.player == null || AbstractDungeon.getCurrRoom() == null) {
            return Mode.NONE;
        }
        if (BigFishAdvisor.config.cardSelect
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT
                && AbstractDungeon.handCardSelectScreen != null) {
            return Mode.HANDSELECT;
        }
        if (BigFishAdvisor.config.map && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP
                && AbstractDungeon.dungeonMapScreen != null) {
            return Mode.MAP;
        }
        if (BigFishAdvisor.config.relics
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD
                && AbstractDungeon.bossRelicScreen != null
                && AbstractDungeon.bossRelicScreen.relics != null
                && !AbstractDungeon.bossRelicScreen.relics.isEmpty()) {
            return Mode.BOSSRELIC;
        }
        if (BigFishAdvisor.config.shop && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP
                && AbstractDungeon.shopScreen != null && AbstractDungeon.shopScreen.isActive) {
            return Mode.SHOP;
        }
        if (BigFishAdvisor.config.cardReward && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD
                && AbstractDungeon.cardRewardScreen != null
                && AbstractDungeon.cardRewardScreen.rewardGroup != null
                && !AbstractDungeon.cardRewardScreen.rewardGroup.isEmpty()) {
            return Mode.CARD;
        }
        if (BigFishAdvisor.config.campfire
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID
                && AbstractDungeon.gridSelectScreen != null
                && AbstractDungeon.gridSelectScreen.forUpgrade) {
            return Mode.UPGRADE;
        }
        if (BigFishAdvisor.config.campfire
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID
                && AbstractDungeon.gridSelectScreen != null
                && AbstractDungeon.gridSelectScreen.forPurge) {
            return Mode.PURGE;
        }
        if (BigFishAdvisor.config.cardSelect
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID
                && AbstractDungeon.gridSelectScreen != null) {
            return Mode.CARDSELECT;
        }
        if (BigFishAdvisor.config.campfire
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE
                && AbstractDungeon.getCurrRoom() instanceof RestRoom) {
            RestRoom rr = (RestRoom) AbstractDungeon.getCurrRoom();
            if (rr.campfireUI != null && !CampfireUI.hidden && !rr.campfireUI.somethingSelected) {
                return Mode.CAMPFIRE;
            }
        }
        if (BigFishAdvisor.config.events
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE
                && AbstractDungeon.getCurrRoom().phase == com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.EVENT
                && AbstractDungeon.getCurrRoom().event != null
                && EventAdvisor.hasOptions(AbstractDungeon.getCurrRoom().event)) {
            return Mode.EVENT;
        }
        if (BigFishAdvisor.config.combat
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE
                && AbstractDungeon.getCurrRoom().phase == com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT
                && AbstractDungeon.actionManager != null
                && !AbstractDungeon.actionManager.turnHasEnded
                && AbstractDungeon.getMonsters() != null
                && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()
                && AbstractDungeon.player.hand != null) {
            return Mode.COMBAT;
        }
        return Mode.NONE;
    }

    private void rebuild() {
        CombatAdvisor.Result previousCombat = combat;
        combat = null;
        cards = null;
        map = null;
        event = null;
        campfire = null;
        shop = null;
        cardSelect = null;
        bossRelic = null;
        hoveredCard = null;
        AbstractPlayer p = AbstractDungeon.player;
        if (mode == Mode.BOSSRELIC) {
            bossRelic = RelicAdvisor.chooseBoss(p, AbstractDungeon.bossRelicScreen.relics);
        } else if (mode == Mode.HANDSELECT) {
            cardSelect = cardSelectAdvisor.analyzeHand(p, AbstractDungeon.handCardSelectScreen);
        } else if (mode == Mode.CARDSELECT) {
            cardSelect = cardSelectAdvisor.analyzeGrid(p, AbstractDungeon.gridSelectScreen);
        } else if (mode == Mode.SHOP) {
            shop = shopAdvisor.analyze(p, AbstractDungeon.shopScreen);
        } else if (mode == Mode.CAMPFIRE && AbstractDungeon.getCurrRoom() instanceof RestRoom) {
            campfire = campfireAdvisor.analyze((RestRoom) AbstractDungeon.getCurrRoom(), p);
        } else if (mode == Mode.UPGRADE) {
            campfire = campfireAdvisor.analyzeUpgrade(p);
        } else if (mode == Mode.PURGE) {
            List<AbstractCard> purgeable = AbstractDungeon.gridSelectScreen == null
                    || AbstractDungeon.gridSelectScreen.targetGroup == null
                    ? null : AbstractDungeon.gridSelectScreen.targetGroup.group;
            campfire = campfireAdvisor.analyzePurge(p, purgeable);
        } else if (mode == Mode.EVENT && AbstractDungeon.getCurrRoom() != null) {
            event = eventAdvisor.analyze(AbstractDungeon.getCurrRoom().event, p);
        } else if (mode == Mode.COMBAT) {
            CombatAdvisor.Result fresh = combatAdvisor.analyze(p, AbstractDungeon.getMonsters());
            if (fresh != null) {
                combat = fresh;
            } else if (AbstractDungeon.getCurrRoom() != null
                    && AbstractDungeon.getCurrRoom().phase
                    == com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT) {
                // During a card's animation the monster/hand arrays can be
                // transiently unusable; keep the last advice instead of
                // shrinking to the little idle fish mid-play.
                combat = previousCombat;
            }
        } else if (mode == Mode.CARD && AbstractDungeon.cardRewardScreen != null) {
            List<AbstractCard> offered = new ArrayList<AbstractCard>(AbstractDungeon.cardRewardScreen.rewardGroup);
            cards = cardAdvisor.analyze(p, offered, AbstractDungeon.cardRewardScreen.rItem);
        } else if (mode == Mode.MAP) {
            map = mapAdvisor.analyze(p);
        }
    }

    public void renderWelcome(SpriteBatch sb) {
        if (BigFishAdvisor.config == null) {
            return;
        }
        ensureInit();
        if (failed || ui == null || !BigFishAdvisor.config.enabled) {
            return;
        }
        try {
            List<String> lines = new ArrayList<String>();
            lines.add("主人，以后就让我陪您一起爬塔吧。");
            lines.add("进游戏后，我会在地图、选牌、事件、战斗时冒泡。");
            lines.add("点击标题展开理由；拖动标题可移动到任意位置；F8 隐藏。");
            showAdvice(sb, "大肥鱼助手已启动", "我是大肥鱼", null, lines, null,
                    "不是自动打牌 · 只是把思路讲给你听", "大肥鱼已就绪",
                    Settings.WIDTH - 490 * ui.s, 34 * ui.s, 470, 270);
        } catch (Throwable t) {
            logOnce(t);
        }
    }

    public void render(SpriteBatch sb) {
        if (BigFishAdvisor.config == null) {
            return;
        }
        ensureInit();
        if (failed || ui == null) {
            return;
        }
        try {
            if (!BigFishAdvisor.config.enabled) {
                drawHiddenTab(sb);
                return;
            }
            if (mode == Mode.NONE || (mode == Mode.COMBAT && combat == null)
                    || (mode == Mode.CARD && cards == null) || (mode == Mode.MAP && map == null)
                    || (mode == Mode.EVENT && event == null)
                    || (mode == Mode.SHOP && shop == null)
                    || (mode == Mode.BOSSRELIC && bossRelic == null)
                    || ((mode == Mode.HANDSELECT || mode == Mode.CARDSELECT) && cardSelect == null)
                    || ((mode == Mode.CAMPFIRE || mode == Mode.UPGRADE || mode == Mode.PURGE)
                    && campfire == null)) {
                drawIdleTab(sb);
                return;
            }
            if (mode == Mode.COMBAT) {
                renderCombat(sb);
            } else if (mode == Mode.CARD) {
                renderCards(sb);
            } else if (mode == Mode.EVENT) {
                renderEvent(sb);
            } else if (mode == Mode.CAMPFIRE || mode == Mode.UPGRADE || mode == Mode.PURGE) {
                renderCampfire(sb);
            } else if (mode == Mode.SHOP) {
                renderShop(sb);
            } else if (mode == Mode.BOSSRELIC) {
                renderBossRelic(sb);
            } else if (mode == Mode.HANDSELECT || mode == Mode.CARDSELECT) {
                renderCardSelect(sb);
            } else {
                renderMap(sb);
            }
        } catch (Throwable t) {
            failed = true;
            logOnce(t);
        }
    }

    private void renderCombat(SpriteBatch sb) {
        if (combat.best != null) {
            ui.drawCardCorners(sb, combat.best.card, Ui.GOLD);
            ui.drawCardTag(sb, combat.best.card, "推荐", Ui.INK, Ui.GOLD, Ui.GOLD);
            if (combat.best.target != null && !combat.best.aoe) {
                ui.drawTarget(sb, combat.best.target);
            }
        } else if (combat.endTurn) {
            ui.drawBadge(sb, Settings.WIDTH * 0.72f, 186 * ui.s, 210 * ui.s, 38 * ui.s,
                    Ui.INK, Ui.GOLD, "大肥鱼建议：结束回合", ui.smallFont, Ui.GOLD);
        }

        showAdvice(sb, "战斗建议", combat.title, combat.context, combat.lines, combat.plan,
                "点击 - 收起理由 · 拖动标题移动 · 数字为估算", combat.title,
                18 * ui.s, Settings.HEIGHT * 0.42f - 165 * ui.s, 540, 330);
    }

    private void renderCards(SpriteBatch sb) {
        AbstractCard hovered = null;
        for (AbstractCard c : AbstractDungeon.cardRewardScreen.rewardGroup) {
            if (c != null && c.hb != null && c.hb.hovered) {
                hovered = c;
                break;
            }
        }
        hoveredCard = hovered;
        if (cards.top == null) {
            return;
        }
        CardRewardAdvisor.Scored focus = cards.top;
        if (hovered != null) {
            CardRewardAdvisor.Scored h = cards.byId.get(hovered.uuid);
            if (h != null) {
                focus = h;
            }
        }
        for (CardRewardAdvisor.Scored s : cards.cards) {
            AbstractCard c = s.card;
            if (c.current_x < -100 * ui.s || c.current_x > Settings.WIDTH + 100 * ui.s) {
                continue;
            }
            Color border = s.rank == 1 ? Ui.GOLD : (s.rank == 2 ? Ui.BLUE : Ui.MUTED);
            ui.drawCardCorners(sb, c, border);
            String label;
            if (cards.skip && s.rank == 1) {
                label = "建议跳过";
            } else if (s.score < 4.2) {
                label = String.format("%d 不推荐", s.rank);
            } else if (s.rank == 1) {
                label = "1 推荐";
            } else if (s.rank == 2) {
                label = "2 备选";
            } else {
                label = String.format("%d", s.rank);
            }
            ui.drawCardTag(sb, c, label, s.rank == 1 ? Ui.INK : Ui.CREAM, border,
                    s.rank == 1 ? Ui.GOLD : Ui.INK);
        }

        String title = cards.title;
        String kicker = cards.kicker;
        List<String> lines = new ArrayList<String>(cards.lines);
        String footer = "F8 收起 · 悬停卡牌看理由 · 分数 1-10";
        if (hovered != null) {
            CardRewardAdvisor.Scored h = cards.byId.get(hovered.uuid);
            if (h != null) {
                title = "看牌：" + CardDatabase.shortName(h.card);
                kicker = String.format("选牌建议 · 排名 #%d · %.1f 分", h.rank, h.score);
                lines = new ArrayList<String>();
                lines.add(cards.deck.countsLine());
                lines.add("这张：" + h.note);
                if (h.tier != null && h.tier.length() > 0) {
                    lines.add("玩家 Tier " + h.tier + "（SpireSpy）· 本局评分 " + String.format("%.1f", h.score));
                }
                lines.add("当前首选：" + (cards.skip ? "跳过" : CardDatabase.shortName(cards.top.card)
                        + "（" + String.format("%.1f", cards.top.score) + "）"));
                lines.add("大肥鱼的想法：" + (cards.top.score >= 8.0
                        ? "先拿核心，再补配合；不要因为稀有度拿不适合牌组的牌。"
                        : "拿能解决当前短板的牌；牌组越厚，普通牌越不值得拿。"));
            }
        }
        showAdvice(sb, kicker, title, null, lines, null, footer,
                cards.skip ? "这组牌跳过" : cards.title,
                10 * ui.s, Settings.HEIGHT * 0.30f, 450, 420);
    }

    private void renderCardSelect(SpriteBatch sb) {
        if (cardSelect.best != null && cardSelect.best.current_x > 0 && cardSelect.best.current_y > 0) {
            boolean bad = cardSelect.direction == CardSelectAdvisor.Direction.DISCARD
                    || cardSelect.direction == CardSelectAdvisor.Direction.EXHAUST
                    || cardSelect.direction == CardSelectAdvisor.Direction.BOTTOM
                    || cardSelect.direction == CardSelectAdvisor.Direction.TRANSFORM;
            Color c = bad ? Ui.RED : Ui.GOLD;
            ui.drawCardCorners(sb, cardSelect.best, c);
            ui.drawCardTag(sb, cardSelect.best, cardSelect.tag, Ui.CREAM, c, c);
        }
        showAdvice(sb, cardSelect.kicker, cardSelect.title, null, cardSelect.lines, null,
                "点击 - 收起理由 · 拖动标题移动 · 高亮牌为推荐", cardSelect.compact,
                14 * ui.s, Settings.HEIGHT * 0.34f, 500, 360);
    }

    private void renderBossRelic(SpriteBatch sb) {
        if (bossRelic.best != null && bossRelic.best.hb != null) {
            ui.drawGlow(sb, bossRelic.best.hb.cX, bossRelic.best.hb.cY, 72 * ui.s, Ui.GOLD, 0.12f);
            ui.drawRing(sb, bossRelic.best.hb.cX, bossRelic.best.hb.cY, 42 * ui.s, Ui.GOLD, 0.9f);
        }
        String name = bossRelic.best == null ? "?" : bossRelic.best.name;
        showAdvice(sb, "Boss 遗物 · 三选一", "大肥鱼建议选【" + name + "】", null,
                bossRelic.lines, null,
                "点击 - 收起理由 · 拖动标题移动 · 金框为推荐遗物", "选 " + name,
                Settings.WIDTH - 520 * ui.s, Settings.HEIGHT * 0.20f, 500, 360);
    }

    private void renderShop(SpriteBatch sb) {
        if (shop.bestCard != null && shop.bestCard.current_x > 0 && shop.bestCard.current_y > 0) {
            ui.drawCardCorners(sb, shop.bestCard, Ui.GOLD);
            ui.drawCardTag(sb, shop.bestCard, "买", Ui.INK, Ui.GOLD, Ui.GOLD);
        }
        showAdvice(sb, shop.kicker, shop.title, null, shop.lines, null,
                "点击 - 收起理由 · 拖动标题移动 · 金框为推荐购买", shop.compact,
                Settings.WIDTH - 480 * ui.s, Settings.HEIGHT * 0.18f, 480, 360);
    }

    private void renderCampfire(SpriteBatch sb) {
        if (mode == Mode.CAMPFIRE) {
            if (campfire.best != null && campfire.best.button != null && campfire.best.button.hb != null) {
                ui.drawOptionHighlight(sb, campfire.best.button.hb, Ui.GOLD);
            }
        } else if ((mode == Mode.UPGRADE || mode == Mode.PURGE)
                && AbstractDungeon.gridSelectScreen != null
                && AbstractDungeon.gridSelectScreen.targetGroup != null) {
            CampfireAdvisor.Upgrade best = mode == Mode.UPGRADE
                    ? campfire.upgradeBest : campfire.purgeBest;
            if (best != null) {
                for (AbstractCard c : AbstractDungeon.gridSelectScreen.targetGroup.group) {
                    if (c == best.card || (c != null && best.card != null && c.uuid.equals(best.card.uuid))) {
                        if (c.current_x > 0 && c.current_y > 0) {
                            if (mode == Mode.UPGRADE) {
                                ui.drawCardCorners(sb, c, Ui.GOLD);
                                ui.drawCardTag(sb, c, "锻造", Ui.INK, Ui.GOLD, Ui.GOLD);
                            } else {
                                ui.drawCardCorners(sb, c, Ui.RED);
                                ui.drawCardTag(sb, c, "删除", Ui.CREAM, Ui.RED, Ui.CREAM);
                            }
                        }
                        break;
                    }
                }
            }
        }
        boolean upgrade = mode == Mode.UPGRADE || mode == Mode.PURGE;
        showAdvice(sb, campfire.kicker, campfire.title, null, campfire.lines, null,
                "点击 - 收起理由 · 拖动标题移动 · 金框为推荐", campfire.compact,
                upgrade ? Settings.WIDTH - 470 * ui.s : 18 * ui.s,
                upgrade ? Settings.HEIGHT * 0.25f : Settings.HEIGHT * 0.28f,
                450, upgrade ? 320 : 350);
    }

    private void renderEvent(SpriteBatch sb) {
        if (event.best != null && event.best.button != null && event.best.button.hb != null) {
            ui.drawOptionHighlight(sb, event.best.button.hb, Ui.GOLD);
        }
        String compact = event.lines.isEmpty() ? event.title : event.lines.get(0).replace("大肥鱼建议：", "");
        showAdvice(sb, event.kicker, event.title, null, event.lines, null,
                "点击 - 收起理由 · 拖动标题移动 · 金色选项为推荐", compact,
                Settings.WIDTH - 500 * ui.s - 14 * ui.s, 20 * ui.s, 500, 360);
    }

    private void renderMap(SpriteBatch sb) {
        drawRoute(sb, map.path);
        String compact = map.lines.isEmpty() ? map.title : map.lines.get(0).replace("大肥鱼建议：", "");
        showAdvice(sb, map.kicker, map.title, null, map.lines, null,
                "点击 - 收起理由 · 拖动标题移动 · 金色路线为推荐", compact,
                18 * ui.s, Settings.HEIGHT * 0.31f, 500, 372);
    }

    private void drawRoute(SpriteBatch sb, List<MapRoomNode> path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        float prevX = 0;
        float prevY = 0;
        boolean hasPrev = false;
        if (AbstractDungeon.currMapNode != null && AbstractDungeon.firstRoomChosen) {
            prevX = AbstractDungeon.currMapNode.hb.cX;
            prevY = AbstractDungeon.currMapNode.hb.cY;
            hasPrev = prevX > 0 && prevY > 0;
        }
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 360.0);
        for (int i = 0; i < path.size(); i++) {
            MapRoomNode n = path.get(i);
            if (n == null || n.hb == null) {
                continue;
            }
            float x = n.hb.cX;
            float y = n.hb.cY;
            if (x < 8 || x > Settings.WIDTH - 8 || y < 8 || y > Settings.HEIGHT - 8) {
                hasPrev = false;
                continue;
            }
            if (hasPrev) {
                ui.drawLine(sb, prevX, prevY, x, y, 5 * ui.s,
                        new Color(0.98f, 0.77f, 0.30f, 0.50f));
            }
            ui.drawGlow(sb, x, y, 50 * ui.s, Ui.GOLD, 0.10f + 0.07f * pulse);
            ui.drawRing(sb, x, y, 33 * ui.s, Ui.GOLD, 0.88f);
            if (i < 6) {
                String num = String.valueOf(i + 1);
                float bw = 24 * ui.s;
                ui.drawBadge(sb, x + 22 * ui.s, y + 22 * ui.s, bw, bw, Ui.INK, Ui.GOLD, num,
                        ui.smallFont, Ui.GOLD);
            }
            prevX = x;
            prevY = y;
            hasPrev = true;
        }
    }

    private void showAdvice(SpriteBatch sb, String kicker, String title, String subtitle,
                            List<String> lines, String plan, String footer, String compactText,
                            float defaultX, float defaultY, float defaultW, float minH) {
        showAdvice(sb, kicker, title, subtitle, lines, plan, footer, compactText,
                defaultX, defaultY, defaultW, minH, false);
    }

    private void showAdvice(SpriteBatch sb, String kicker, String title, String subtitle,
                            List<String> lines, String plan, String footer, String compactText,
                            float defaultX, float defaultY, float defaultW, float minH,
                            boolean forceExpanded) {
        boolean expanded = forceExpanded || BigFishAdvisor.config.expanded;
        // Start from a comfortable reading width. drawBubble() widens the box
        // only when the text genuinely needs it, and grows the height only to
        // the measured content size (no empty giant panel).
        panelW = expanded ? Math.max(defaultW * ui.s, 620 * ui.s) : 380 * ui.s;
        panelH = expanded ? 160 * ui.s : 104 * ui.s;
        // While dragging, keep the live position. Re-applying defaults here was
        // snapping the panel back every frame, which is why dragging looked dead.
        if (!dragging) {
            if (BigFishAdvisor.config.panelX >= 0 && BigFishAdvisor.config.panelY >= 0) {
                panelX = BigFishAdvisor.config.panelX * Settings.WIDTH;
                panelY = BigFishAdvisor.config.panelY * Settings.HEIGHT;
            } else {
                panelX = defaultX;
                panelY = defaultY;
            }
        }
        panelX = clamp(panelX, 6 * ui.s, Settings.WIDTH - panelW - 6 * ui.s);
        panelY = clamp(panelY, 6 * ui.s, Settings.HEIGHT - panelH - 6 * ui.s);
        List<String> allLines = new ArrayList<String>();
        if (lines != null) {
            allLines.addAll(lines);
        }
        String quickPotion = "";
        if (mode == Mode.COMBAT && BigFishAdvisor.config.potions && AbstractDungeon.player != null
                && AbstractDungeon.getCurrRoom() != null) {
            String room = AbstractDungeon.getCurrRoom().getClass().getSimpleName();
            boolean eliteOrBoss = room.contains("Elite") || room.contains("Boss");
            int incoming = combat == null ? 0 : combat.incoming;
            List<String> potionLines = PotionAdvisor.lines(AbstractDungeon.player, true,
                    incoming, eliteOrBoss);
            allLines.addAll(potionLines);
            quickPotion = PotionAdvisor.quickUse(AbstractDungeon.player, true,
                    incoming, eliteOrBoss, combat == null || combat.best == null ? null : combat.best.card);
        }
        if (mode != Mode.NONE && !allLines.isEmpty()) {
            allLines.add(0, personaOpening());
            allLines.add("主人，先按这个试试看；不合适再叫我。");
        }
        String key = mode + "|" + title + "|" + (lines == null ? 0 : lines.size());
        if (!key.equals(pageKey)) {
            pageKey = key;
            page = 0;
        }
        if (expanded) {
            drawBubble(sb, kicker, title, subtitle, allLines, plan, footer);
        } else {
            String compact = compactText == null ? title : compactText;
            if (quickPotion.length() > 0) {
                if (quickPotion.contains("再打")) {
                    compact = quickPotion;
                } else {
                    compact = quickPotion + (compact.startsWith("打") ? "，再" + compact : "；" + compact);
                }
            }
            drawCompact(sb, title, "主人，" + compact);
        }
    }

    private String personaOpening() {
        switch (mode) {
            case COMBAT:
                return "主人，我来帮您看看这回合怎么打吧。";
            case MAP:
                return "主人，地图我帮您理好了，慢慢看就好。";
            case CARD:
                return "主人，这三张牌我帮您比较一下。";
            case EVENT:
                return "主人，这个事件我陪您一起选。";
            case CAMPFIRE:
                return "主人，营火这边我帮您想好了。";
            case SHOP:
                return "主人，商店的钱要花得值，我帮您看看。";
            case BOSSRELIC:
                return "主人，这三个 Boss 遗物我帮您一个个看，选最合牌组的。";
            case HANDSELECT:
            case CARDSELECT:
                return "主人，这张牌放哪里，我帮您看。";
            case UPGRADE:
            case PURGE:
                return "主人，这次强化/删牌，我帮您挑合适的。";
            default:
                return "主人，我帮您看过了。";
        }
    }

    private int countBodyLines(List<String> lines, BitmapFont font, float width) {
        int n = 0;
        for (String raw : lines) {
            if (raw != null && raw.length() > 0) {
                n += ui.wrap(font, raw, width).size();
            }
        }
        return n;
    }

    private void drawCompact(SpriteBatch sb, String title, String compactText) {
        fishSize = 0;
        pageCount = 1;
        pageNextW = 0;
        pagePrevW = 0;
        float av = 60 * ui.s;
        float tx = panelX + av + 22 * ui.s;
        float tw = panelW - av - 52 * ui.s;
        String body = compactText == null || compactText.length() == 0 ? title : compactText;
        List<String> lines = ui.wrap(ui.bodyFont, body, tw);
        int show = Math.min(5, lines.size());
        panelH = Math.max(panelH, 42 * ui.s + show * ui.bodyFont.getLineHeight() + 18 * ui.s);
        panelY = clamp(panelY, 6 * ui.s, Settings.HEIGHT - panelH - 6 * ui.s);
        ui.drawPanel(sb, panelX, panelY, panelW, panelH);
        ui.drawAvatar(sb, panelX + 10 * ui.s, panelY + panelH - av - 10 * ui.s, av);
        ui.smallFont.setColor(Ui.GOLD);
        ui.smallFont.draw(sb, "大肥鱼", tx, panelY + panelH - 16 * ui.s);
        ui.bodyFont.setColor(Ui.INK);
        float y = panelY + panelH - 42 * ui.s;
        for (int i = 0; i < show; i++) {
            ui.bodyFont.draw(sb, lines.get(i), tx, y);
            y -= ui.bodyFont.getLineHeight();
        }
        ui.smallFont.setColor(Ui.MUTED);
        ui.smallFont.draw(sb, "点击看理由 · 拖动可移动", panelX + 12 * ui.s, panelY + 9 * ui.s);

        headerX = panelX;
        headerY = panelY;
        headerW = panelW - 36 * ui.s;
        headerH = panelH;
        toggleSize = 25 * ui.s;
        toggleX = panelX + panelW - toggleSize - 8 * ui.s;
        toggleY = panelY + panelH - toggleSize - 7 * ui.s;
        ui.drawBadge(sb, toggleX, toggleY, toggleSize, toggleSize, Ui.INK, Ui.GOLD, "+",
                ui.smallFont, Ui.GOLD);
    }

    private void drawBubble(SpriteBatch sb, String kicker, String title, String subtitle,
                            List<String> lines, String plan, String footer) {
        fishSize = 0;
        float av = 112 * ui.s;
        List<String> all = new ArrayList<String>();
        if (lines != null) {
            all.addAll(lines);
        }
        if (plan != null && plan.length() > 0) {
            all.add(plan);
        }
        BitmapFont body = ui.bodyFont;
        float maxW = Settings.WIDTH - 12 * ui.s;
        float maxH = Settings.HEIGHT - 12 * ui.s;
        float textW;
        int titleLines;
        int subtitleLines;
        int bodyLines;
        float header;
        float required;
        while (true) {
            textW = panelW - av - 48 * ui.s;
            titleLines = Math.min(2, ui.wrap(ui.titleFont, title, textW).size());
            subtitleLines = subtitle == null ? 0 : ui.wrap(ui.smallFont, subtitle, textW).size();
            bodyLines = countBodyLines(all, body, textW);
            header = 26 * ui.s + titleLines * ui.titleFont.getLineHeight() + 2 * ui.s
                    + (subtitleLines > 0 ? subtitleLines * ui.smallFont.getLineHeight() + 4 * ui.s : 0)
                    + 44 * ui.s;
            required = header + bodyLines * body.getLineHeight();
            if (required <= maxH) {
                break;
            }
            if (panelW < maxW - 1 * ui.s) {
                // Widen first: a shorter, wider paragraph often avoids a
                // needlessly tall panel. Only as wide as the text needs.
                panelW = Math.min(maxW, panelW + 170 * ui.s);
                panelX = clamp(panelX, 6 * ui.s, Settings.WIDTH - panelW - 6 * ui.s);
                continue;
            }
            if (body == ui.bodyFont) {
                body = ui.smallFont;
                continue;
            }
            break;
        }
        // Grow to the measured text size. If even a full-screen panel cannot
        // hold everything, keep the panel at screen height and paginate below;
        // no line is ever replaced by an ellipsis.
        panelH = Math.max(160 * ui.s, Math.min(required, maxH));
        panelX = clamp(panelX, 6 * ui.s, Settings.WIDTH - panelW - 6 * ui.s);
        panelY = clamp(panelY, 6 * ui.s, Settings.HEIGHT - panelH - 6 * ui.s);

        ui.drawPanel(sb, panelX, panelY, panelW, panelH);
        ui.drawAvatar(sb, panelX + 12 * ui.s, panelY + panelH - av - 12 * ui.s, av);
        float textX = panelX + av + 28 * ui.s;
        float topY = panelY + panelH - 26 * ui.s;
        ui.drawTitle(sb, kicker, title, textX, topY, textW);
        float y = topY - 26 * ui.s - titleLines * ui.titleFont.getLineHeight() - 2 * ui.s;
        if (subtitle != null && subtitle.length() > 0) {
            ui.smallFont.setColor(Ui.MUTED);
            for (String line : ui.wrap(ui.smallFont, subtitle, textW)) {
                ui.smallFont.draw(sb, line, textX, y);
                y -= ui.smallFont.getLineHeight();
            }
            y -= 4 * ui.s;
        }
        float available = y - (panelY + 42 * ui.s);
        pageMaxLines = Math.max(1, (int) (available / body.getLineHeight()));
        pageCount = Math.max(1, (bodyLines + pageMaxLines - 1) / pageMaxLines);
        if (pageCount > 1) {
            // Reserve the bottom strip for the page button.
            available -= 34 * ui.s;
            pageMaxLines = Math.max(1, (int) (available / body.getLineHeight()));
            pageCount = Math.max(1, (bodyLines + pageMaxLines - 1) / pageMaxLines);
        }
        page = Math.max(0, Math.min(page, pageCount - 1));
        int startLine = page * pageMaxLines;
        ui.drawBody(sb, all, textX, y, textW, pageMaxLines, Ui.INK, body, startLine);
        pagePrevW = 0;
        pageNextW = 0;
        if (pageCount > 1) {
            float bw = 52 * ui.s;
            float gap = 8 * ui.s;
            pageNextW = bw;
            pageNextH = 28 * ui.s;
            pageNextX = panelX + panelW - bw - 12 * ui.s;
            pageNextY = panelY + 34 * ui.s;
            pagePrevW = bw;
            pagePrevH = pageNextH;
            pagePrevX = pageNextX - bw - gap;
            pagePrevY = pageNextY;
            ui.smallFont.setColor(Ui.MUTED);
            ui.smallFont.draw(sb, (page + 1) + "/" + pageCount,
                    pagePrevX - 42 * ui.s, pagePrevY + 9 * ui.s);
            ui.drawBadge(sb, pagePrevX, pagePrevY, pagePrevW, pagePrevH,
                    Ui.INK, Ui.GOLD, "<<", ui.smallFont, Ui.GOLD);
            ui.drawBadge(sb, pageNextX, pageNextY, pageNextW, pageNextH,
                    Ui.INK, Ui.GOLD, ">>", ui.smallFont, Ui.GOLD);
        }
        ui.drawFooter(sb, footer, panelX + 16 * ui.s, panelY + 15 * ui.s,
                panelW - (pageCount > 1 ? 250 : 72) * ui.s);

        headerX = panelX;
        headerY = panelY + panelH - 92 * ui.s;
        headerW = panelW - 38 * ui.s;
        headerH = 92 * ui.s;
        toggleSize = 25 * ui.s;
        toggleX = panelX + panelW - toggleSize - 8 * ui.s;
        toggleY = panelY + panelH - toggleSize - 8 * ui.s;
        ui.drawBadge(sb, toggleX, toggleY, toggleSize, toggleSize, Ui.INK, Ui.GOLD, "-",
                ui.smallFont, Ui.GOLD);
    }

    private void drawHiddenTab(SpriteBatch sb) {
        placeFish(64 * ui.s);
        pageCount = 1;
        pageNextW = 0;
        pagePrevW = 0;
        closeSize = 0;
        headerW = 0;
        toggleSize = 0;
        ui.drawHiddenTab(sb, hiddenX, hiddenY, fishSize, false);
    }

    private void drawIdleTab(SpriteBatch sb) {
        placeFish(54 * ui.s);
        pageCount = 1;
        pageNextW = 0;
        pagePrevW = 0;
        closeSize = 0;
        headerW = 0;
        toggleSize = 0;
        ui.drawHiddenTab(sb, hiddenX, hiddenY, fishSize, false);
    }

    private void placeFish(float size) {
        fishSize = size;
        hiddenSize = size;
        if (!fishDragging) {
            if (BigFishAdvisor.config.fishX >= 0 && BigFishAdvisor.config.fishY >= 0) {
                hiddenX = BigFishAdvisor.config.fishX * Settings.WIDTH;
                hiddenY = BigFishAdvisor.config.fishY * Settings.HEIGHT;
            } else if (panelW > 0 && panelH > 0) {
                hiddenX = panelX + panelW - size - 8 * ui.s;
                hiddenY = panelY + panelH - size - 8 * ui.s;
            } else {
                hiddenX = 8 * ui.s;
                hiddenY = Settings.HEIGHT * 0.52f;
            }
        }
        hiddenX = clamp(hiddenX, 6 * ui.s, Settings.WIDTH - size - 6 * ui.s);
        hiddenY = clamp(hiddenY, 6 * ui.s, Settings.HEIGHT - size - 6 * ui.s);
    }

    private void saveFishPosition() {
        if (hiddenX < 0 || hiddenY < 0 || Settings.WIDTH <= 0 || Settings.HEIGHT <= 0) {
            return;
        }
        BigFishAdvisor.config.fishX = hiddenX / Settings.WIDTH;
        BigFishAdvisor.config.fishY = hiddenY / Settings.HEIGHT;
    }

    private boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private float clamp(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private void savePanelPosition() {
        if (panelX < 0 || panelY < 0 || Settings.WIDTH <= 0 || Settings.HEIGHT <= 0) {
            return;
        }
        BigFishAdvisor.config.panelX = panelX / Settings.WIDTH;
        BigFishAdvisor.config.panelY = panelY / Settings.HEIGHT;
    }

    private void consumeClick() {
        InputHelper.justClickedLeft = false;
        InputHelper.isMouseDown = false;
        InputHelper.justReleasedClickLeft = false;
    }

    private void ensureInit() {
        if (initTried) {
            return;
        }
        initTried = true;
        try {
            CardDatabase.load();
            ui = new Ui();
        } catch (Throwable t) {
            failed = true;
            BigFishAdvisor.log("UI init failed: " + t);
            t.printStackTrace();
        }
    }

    private void logOnce(Throwable t) {
        if (!loggedError) {
            loggedError = true;
            BigFishAdvisor.log("Overlay error: " + t);
            t.printStackTrace();
        }
    }
}
