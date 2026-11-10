package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Handles "put on top/bottom", discard, exhaust, tutor and copy prompts. */
public final class CardSelectAdvisor {
    public enum Direction {TOP, BOTTOM, DISCARD, EXHAUST, HAND, RETAIN, UPGRADE, TRANSFORM, COPY, CHOOSE}

    public static final class Result {
        public Direction direction = Direction.CHOOSE;
        public String title = "选牌建议";
        public String kicker = "选牌 · 先看方向";
        public String compact = "";
        public List<String> lines = new ArrayList<String>();
        public AbstractCard best;
        public String tag = "选";
        public boolean stop;
    }

    public Result analyzeHand(AbstractPlayer p, HandCardSelectScreen hs) {
        if (p == null || hs == null) {
            return null;
        }
        String prompt = hs.selectionReason == null ? "" : hs.selectionReason;
        if (prompt.length() == 0) {
            prompt = String.valueOf(read(hs, "message", ""));
        }
        boolean forUpgrade = Boolean.TRUE.equals(read(hs, "forUpgrade", Boolean.FALSE));
        boolean forTransform = Boolean.TRUE.equals(read(hs, "forTransform", Boolean.FALSE));
        boolean anyNumber = Boolean.TRUE.equals(read(hs, "anyNumber", Boolean.FALSE));
        Direction dir = classify(prompt, forUpgrade, forTransform);
        // Gambling Chip uses 99 as the "any number" sentinel.  Some game
        // builds do not expose the private anyNumber field through reflection,
        // so treat the sentinel itself as unlimited too.
        boolean unlimited = anyNumber || hs.numCardsToSelect >= 90;
        if (unlimited && dir == Direction.DISCARD) {
            return anyNumberDiscard(p, hs);
        }
        int count = Math.max(1, hs.numCardsToSelect);
        // Well-Laid Plans / "retain up to N" is an up-to screen.  Once the
        // player has selected the allowed number, stop pointing at another
        // card: the old code kept recommending the next-best card and made
        // people select past the cap.
        if (!unlimited && hs.numSelected >= count) {
            return limitReached(dir, count, hs.numSelected);
        }
        List<AbstractCard> hand = new ArrayList<AbstractCard>();
        if (p.hand != null) {
            for (AbstractCard c : p.hand.group) {
                if (hs.selectedCards == null || !containsUuid(hs.selectedCards.group, c)) {
                    hand.add(c);
                }
            }
        }
        // numCardsToSelect is the TOTAL allowed for the screen, not the
        // number still available.  After selecting one of two retain targets
        // the panel must say "one left", otherwise it names two more cards
        // and looks like it is asking for a third.
        int shown = dir == Direction.RETAIN
                ? Math.max(1, count - Math.max(0, hs.numSelected)) : count;
        return build(p, hand, dir, shown, anyNumber);
    }

    private Result limitReached(Direction dir, int count, int selected) {
        Result r = new Result();
        r.direction = dir;
        r.stop = true;
        r.best = null;
        r.kicker = "选牌 · 已经选够";
        r.title = (dir == Direction.RETAIN ? "够了：已保留 " : "够了：已选 ")
                + selected + " 张";
        r.compact = "够了，点确认";
        r.lines.add("这次最多只能选 " + count + " 张，已经选够了；不要再往下选。");
        if (dir == Direction.RETAIN) {
            r.lines.add("再选不会多保留一张，只会占掉位置；直接点确认进入下一回合。");
        } else {
            r.lines.add("再选会改变这次效果的目标；直接点确认。");
        }
        return r;
    }

    /**
     * "Discard any number" (Gambling Chip) is not a request to keep selecting
     * until the hand is empty.  Pick a small, explicit set: curses/statuses,
     * discard-payoff cards, and at most two basic Strike/Defend cards.  Once
     * that set is selected, tell the player to stop; never highlight another
     * card and never imply that every card should go.
     */
    private Result anyNumberDiscard(AbstractPlayer p, HandCardSelectScreen hs) {
        List<AbstractCard> all = new ArrayList<AbstractCard>();
        if (p.hand != null) {
            all.addAll(p.hand.group);
        }
        if (hs.selectedCards != null) {
            for (AbstractCard c : hs.selectedCards.group) {
                if (!containsUuid(all, c)) {
                    all.add(c);
                }
            }
        }
        List<AbstractCard> planned = new ArrayList<AbstractCard>();
        int basics = 0;
        for (AbstractCard c : all) {
            if (c == null) {
                continue;
            }
            if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS
                    || isDiscardPayoff(c)) {
                planned.add(c);
            } else if (isBasic(c) && basics < 2) {
                planned.add(c);
                basics++;
            }
        }
        List<AbstractCard> keep = new ArrayList<AbstractCard>();
        for (AbstractCard c : all) {
            if (c != null && !containsUuid(planned, c)) {
                keep.add(c);
            }
        }
        Collections.sort(keep, new Comparator<AbstractCard>() {
            @Override
            public int compare(AbstractCard a, AbstractCard b) {
                return Double.compare(score(b), score(a));
            }
        });

        Result r = new Result();
        r.direction = Direction.DISCARD;
        r.kicker = "选牌 · 任意数量弃牌";
        int selected = Math.max(0, hs.numSelected);
        if (planned.isEmpty()) {
            r.stop = true;
            r.best = null;
            r.tag = "停";
            r.title = "这次不用弃";
            r.compact = "这次不弃";
            r.lines.add("大肥鱼建议：这次别弃。手里没有值得主动丢的诅咒/状态/基础牌，"
                    + "也没有弃牌收益牌；为了换牌把好牌全弃不划算。");
            addKeepLine(r, keep);
            return r;
        }
        if (selected >= planned.size()) {
            r.stop = true;
            r.best = null;
            r.tag = "停";
            r.title = "够了：已选 " + selected + " 张，点确认";
            r.compact = "够了，点确认";
            r.lines.add("大肥鱼建议：已经选够 " + planned.size() + " 张了，别再往下选；"
                    + "再选就会开始丢好牌。点确认结束。");
            addKeepLine(r, keep);
            return r;
        }

        List<AbstractCard> remaining = new ArrayList<AbstractCard>();
        for (AbstractCard c : planned) {
            if (!containsUuid(hs.selectedCards == null ? new ArrayList<AbstractCard>() : hs.selectedCards.group, c)) {
                remaining.add(c);
            }
        }
        if (remaining.isEmpty()) {
            r.stop = true;
            r.best = null;
            r.tag = "停";
            r.title = "够了：点确认";
            r.compact = "够了，点确认";
            r.lines.add("大肥鱼建议：要丢的牌已经选完了，别再往下选。");
            addKeepLine(r, keep);
            return r;
        }
        Collections.sort(remaining, new Comparator<AbstractCard>() {
            @Override
            public int compare(AbstractCard a, AbstractCard b) {
                return Double.compare(value(a, Direction.DISCARD), value(b, Direction.DISCARD));
            }
        });
        AbstractCard pick = remaining.get(0);
        String name = CardDatabase.shortName(pick);
        r.best = pick;
        r.tag = "弃";
        r.title = "任意数量弃牌：丢【" + name + "】（共 " + planned.size() + " 张）";
        r.compact = "丢 " + name + "（共 " + planned.size() + " 张）";
        r.lines.add("大肥鱼建议：这次只丢 " + planned.size() + " 张，不是全弃；按顺序先丢【"
                + name + "】。");
        r.lines.add("为什么呢：" + discardReason(pick));
        StringBuilder names = new StringBuilder();
        for (AbstractCard c : planned) {
            if (names.length() > 0) {
                names.append("、");
            }
            names.append("【").append(CardDatabase.shortName(c)).append("】");
        }
        r.lines.add("这次要丢的名单：" + names + "。选够 " + planned.size()
                + " 张后点确认，别继续往下选。");
        addKeepLine(r, keep);
        return r;
    }

    private static boolean containsUuid(List<AbstractCard> cards, AbstractCard card) {
        if (cards == null || card == null) {
            return false;
        }
        for (AbstractCard c : cards) {
            if (c != null && c.uuid != null && c.uuid.equals(card.uuid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBasic(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        return id.startsWith("strike") || id.startsWith("defend");
    }

    private static boolean isDiscardPayoff(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        return id.contains("tactician") || id.contains("reflex");
    }

    private static double score(AbstractCard c) {
        CardDatabase.Entry e = c == null ? null : CardDatabase.get(c);
        return e == null ? 0.0 : e.score;
    }

    private static String discardReason(AbstractCard c) {
        if (c == null) {
            return "这张牌现在用不上。";
        }
        if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
            return "诅咒/状态留在手里只会占位或卡节奏，丢了最干净。";
        }
        if (isDiscardPayoff(c)) {
            return "这张牌被弃掉时会触发收益（战术大师给能量/反射抽牌），"
                    + "所以主动丢它反而不亏。";
        }
        if (isBasic(c)) {
            return "基础打击/防御最容易替换；这次最多丢两张，别把整手基础牌都丢光。";
        }
        return "这张牌当前用不上，丢了也不心疼。";
    }

    private static void addKeepLine(Result r, List<AbstractCard> keep) {
        if (keep.isEmpty()) {
            r.lines.add("保留：手里没有需要特别保护的好牌，但也不要全选。");
            return;
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < keep.size() && i < 3; i++) {
            if (s.length() > 0) {
                s.append("、");
            }
            s.append("【").append(CardDatabase.shortName(keep.get(i))).append("】");
        }
        r.lines.add("保留：" + s + " 这些不要选；如果你觉得某张更该丢，再手动替换。");
    }

    public Result analyzeGrid(AbstractPlayer p, GridCardSelectScreen gs) {
        if (p == null || gs == null || gs.targetGroup == null) {
            return null;
        }
        String prompt = String.valueOf(read(gs, "tipMsg", ""));
        Direction dir = classify(prompt, gs.forUpgrade, gs.forTransform);
        int count = 1;
        Object n = read(gs, "numCards", Integer.valueOf(1));
        if (n instanceof Integer) {
            count = Math.max(1, ((Integer) n).intValue());
        }
        return build(p, new ArrayList<AbstractCard>(gs.targetGroup.group), dir, count, gs.anyNumber || count > 1);
    }

    private Result build(AbstractPlayer p, List<AbstractCard> cards, Direction dir, int count, boolean many) {
        if (cards.isEmpty()) {
            return null;
        }
        final boolean bad = dir == Direction.DISCARD || dir == Direction.EXHAUST
                || dir == Direction.BOTTOM || dir == Direction.TRANSFORM;
        final boolean retain = dir == Direction.RETAIN;
        Collections.sort(cards, new Comparator<AbstractCard>() {
            @Override
            public int compare(AbstractCard a, AbstractCard b) {
                if (retain) {
                    return Double.compare(retainValue(p, b), retainValue(p, a));
                }
                return Double.compare(value(b, dir), value(a, dir));
            }
        });
        Result r = new Result();
        r.direction = dir;
        r.best = cards.get(0);
        String name = CardDatabase.shortName(r.best);
        switch (dir) {
            case TOP:
                r.title = "放牌堆顶：选【" + name + "】";
                r.compact = "放顶 " + name;
                r.tag = "放顶";
                r.lines.add("大肥鱼建议：把【" + name + "】放到牌堆顶，下一抽必定抽到它。");
                r.lines.add("为什么呢：放顶等于“预约下一张关键牌”；优先高影响力攻击/能力/保命牌，而不是普通打击防御。");
                break;
            case BOTTOM:
                r.title = "放牌堆底：选【" + name + "】";
                r.compact = "放底 " + name;
                r.tag = "放底";
                r.lines.add("大肥鱼建议：把【" + name + "】放到牌堆底，相当于这轮先不看它。");
                r.lines.add("为什么呢：优先把诅咒/状态/当前用不上的牌压到底，别把关键牌藏起来。");
                break;
            case DISCARD:
                r.title = "弃牌：丢【" + name + "】";
                r.compact = "弃 " + name;
                r.tag = "弃";
                r.lines.add("大肥鱼建议：弃掉【" + name + "】。");
                if (discardPayoff(r.best)) {
                    r.lines.add("为什么呢：这张牌被弃掉时会触发收益（【战术大师】给能量/"
                            + "【反射】抽牌），所以这里反而要主动丢它，"
                            + "不是丢最差的那张。");
                } else if (r.best.type == AbstractCard.CardType.CURSE
                        || r.best.type == AbstractCard.CardType.STATUS) {
                    r.lines.add("为什么呢：诅咒/状态留在手里只会占位或卡节奏，"
                            + "弃掉它最干净；别为了保留一张普通攻击而把诅咒留在手里。");
                } else if (CardDatabase.norm(r.best.cardID).contains("strike")
                        || CardDatabase.norm(r.best.cardID).contains("defend")) {
                    r.lines.add("为什么呢：基础打击/防御是牌组里最容易替换的牌；"
                            + "弃它不会拆掉核心 combo，也不会浪费关键能力。");
                } else {
                    r.lines.add("为什么呢：在没有弃牌触发收益时，弃牌要选“现在最不需要、"
                            + "丢了也不心疼”的那张；不要弃核心能力/关键攻击。"
                            + "如果手里有【战术大师】/【反射】这类弃牌收益牌，优先弃它们。");
                }
                break;
            case EXHAUST:
                r.title = "消耗：选【" + name + "】";
                r.compact = "消耗 " + name;
                r.tag = "消耗";
                r.lines.add("大肥鱼建议：消耗【" + name + "】。");
                r.lines.add("为什么呢：优先消耗诅咒/状态/一轮用不上的牌；有 Dark Embrace/Feel No Pain 时，消耗本身也能变成收益。");
                break;
            case HAND:
                r.title = "拿回手牌：选【" + name + "】";
                r.compact = "拿 " + name;
                r.tag = "拿";
                r.lines.add("大肥鱼建议：把【" + name + "】拿回手牌。");
                r.lines.add("为什么呢：优先拿回能立即改变局面的关键牌；如果只是补充普通攻击，先看能量够不够。");
                break;
            case RETAIN:
                String secondName = count > 1 && cards.size() > 1
                        ? CardDatabase.shortName(cards.get(1)) : null;
                r.title = "保留：选【" + name + "】"
                        + (secondName != null ? "，再选【" + secondName + "】" : "");
                r.compact = "留 " + name
                        + (secondName != null ? " + " + secondName : "");
                r.tag = "保留";
                r.lines.add("大肥鱼建议：这张【" + name + "】留到下回合。"
                        + (secondName != null ? "第二张留【" + secondName + "】。" : ""));
                r.lines.add("为什么留它：" + retainReason(p, r.best));
                if (secondName != null) {
                    r.lines.add("为什么留第二张【" + secondName + "】："
                            + retainReason(p, cards.get(1)));
                }
                if (count > 1) {
                    r.lines.add("这次还能保留 " + count + " 张；选够后我会提示“点确认”，"
                            + "不会再让你继续选。只留一张也可以，空着不丢牌。");
                } else {
                    r.lines.add("这次还能保留 1 张；选够后直接点确认，不会再多推荐一张。");
                }
                r.lines.add("别选：诅咒/状态、虚无牌、已经带保留的牌；普通打击防御只有在没牌可留时才考虑。");
                break;
            case UPGRADE:
                r.title = "升级：选【" + name + "】";
                r.compact = "升级 " + name;
                r.tag = "升级";
                r.lines.add("大肥鱼建议：升级【" + name + "】。");
                r.lines.add("为什么呢：优先升级核心攻击/能力，费用降低或数值提升最大的牌；不要升级基础打击防御。");
                break;
            case TRANSFORM:
                r.title = "变化：选【" + name + "】";
                r.compact = "变化 " + name;
                r.tag = "变化";
                r.lines.add("大肥鱼建议：把【" + name + "】变化掉。");
                r.lines.add("为什么呢：优先变诅咒/状态/基础牌；关键牌不要变，随机结果通常不如保留核心。");
                break;
            case COPY:
                r.title = "复制：选【" + name + "】";
                r.compact = "复制 " + name;
                r.tag = "复制";
                r.lines.add("大肥鱼建议：复制【" + name + "】。");
                r.lines.add("为什么呢：优先复制核心能力/关键攻击，而不是普通打击防御；先确认复制后费用和手牌空间。");
                break;
            default:
                r.title = "选牌：选【" + name + "】";
                r.compact = "选 " + name;
                r.tag = "选";
                r.lines.add("大肥鱼建议：选【" + name + "】。");
                r.lines.add("为什么呢：没有识别出明确方向，按当前牌组价值选最关键的一张。");
                break;
        }
        if (dir != Direction.RETAIN && cards.size() > 1) {
            AbstractCard second = cards.get(1);
            r.lines.add("另一个选择是：【" + CardDatabase.shortName(second) + "】"
                    + "（评分 " + String.format("%.1f", value(second, dir)) + "）；"
                    + (dir == Direction.DISCARD
                    ? "如果它也有弃牌收益（战术大师/反射），或者你更想保留首选，再选它。"
                    : (bad ? "除非它更废，否则首选更稳。" : "除非它更关键，否则首选收益更高。")));
        }
        r.lines.add("选之前想一下：先确认这张牌是不是你愿意失去/放回的那张；放顶会改变下一抽，"
                + "弃牌/消耗会改变本回合手牌，复制/变化要注意费用与牌组厚度。");
        if (dir != Direction.RETAIN && many && cards.size() > 1) {
            StringBuilder alt = new StringBuilder();
            for (int i = 1; i < cards.size() && i < 4; i++) {
                if (alt.length() > 0) {
                    alt.append("、");
                }
                alt.append("【").append(CardDatabase.shortName(cards.get(i))).append("】");
            }
            r.lines.add("还可以考虑：" + alt + "。");
        }
        return r;
    }

    private double retainValue(AbstractPlayer p, AbstractCard c) {
        if (c == null) {
            return -999;
        }
        if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
            return -100;
        }
        if (c.isEthereal) {
            return -90; // ethereal is not actually retained; wasted slot
        }
        if (c.retain) {
            return -75; // already retained by another effect
        }
        if (isBasic(c)) {
            return -8;
        }
        if (isDiscardPayoff(c)) {
            return -12; // Tactician/Reflex prefer being discarded, not held
        }
        double v = 3.0;
        CardDatabase.Entry e = CardDatabase.get(c);
        if (e != null) {
            v += e.score * 0.25;
        }
        if (c.type == AbstractCard.CardType.POWER) {
            v += 6.0; // long-term scaling; play it next turn
        }
        if (c.cost >= 2) {
            v += 2.0; // too expensive this turn, save it
        }
        if (c.cost == 0) {
            v -= 0.5; // easy to replay, less need to spend a retain slot
        }
        if (c.type == AbstractCard.CardType.ATTACK) {
            v += Math.min(5.0, c.damage * 0.08);
        }
        int incoming = 0;
        if (AbstractDungeon.getMonsters() != null) {
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m != null && !m.isDeadOrEscaped() && m.getIntentDmg() > 0) {
                    incoming += m.getIntentDmg();
                }
            }
        }
        if (incoming > p.currentBlock + 4 && c.block > 0) {
            v += 5.0; // next turn's block matters more when this turn ends exposed
        }
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("catalyst") || id.contains("wraithform")
                || id.contains("limitbreak") || id.contains("barricade")
                || id.contains("echoform") || id.contains("biasedcognition")
                || id.contains("demonform") || id.contains("feed")
                || id.contains("ritualdagger") || id.contains("geneticalgorithm")
                || id.contains("lessonlearned") || id.contains("corruption")
                || id.contains("feelnopain") || id.contains("darkembrace")) {
            v += 4.0; // key scaling/combo piece: never let it go to discard
        }
        return v;
    }

    private String retainReason(AbstractPlayer p, AbstractCard c) {
        if (c == null) {
            return "没有可保留的牌。";
        }
        if (c.type == AbstractCard.CardType.POWER) {
            return "能力牌是长期收益；这回合费用不够/时机不对，留到下回合打，比丢进弃牌堆再抽回来稳。";
        }
        if (c.isEthereal) {
            return "虚无牌本来就会消耗，选它只会浪费保留位。";
        }
        if (c.retain) {
            return "它已经带保留，不需要再占一次保留位。";
        }
        if (isBasic(c)) {
            return "基础打击/防御价值低；只有手里实在没牌可留时才考虑。";
        }
        if (isDiscardPayoff(c)) {
            return "战术大师/反射更适合被弃掉触发收益，留在手里反而浪费。";
        }
        if (c.cost >= 2) {
            return "这回合费用不够，留到下回合能直接打出来；比现在硬打一张低价值牌好。";
        }
        if (c.block > 0) {
            return "下回合可能还需要它挡伤害；先留住，掉血时能救急。";
        }
        if (c.type == AbstractCard.CardType.ATTACK) {
            return "它是手里最值得留到下一回合的输出牌；下一回合配合姿态/易伤/能量能打出更高伤害。";
        }
        return "按当前牌组价值，它比普通攻击防御更值得留到下一回合。";
    }

    private Direction classify(String prompt, boolean forUpgrade, boolean forTransform) {
        String p = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        // Put "into hand" first: prompts such as "从弃牌堆拿回手牌" contain
        // both "弃牌堆" and "手牌", but they are not a discard prompt.
        if (p.contains("into your hand") || (p.contains("add") && p.contains("hand"))
                || p.contains("回到手牌") || p.contains("拿回") || p.contains("加入手牌")) {
            return Direction.HAND;
        }
        // Gambling Chip's Chinese selectionReason is "替换（赌博筹码）" and the
        // English one is "Gambling Chip"; neither contains discard/弃牌, so it
        // used to fall through to CHOOSE and recommend throwing away the best
        // card.  Treat the chip's replacement prompt as a discard-any-number
        // screen explicitly.
        if (p.contains("赌博筹码") || p.contains("gamblingchip") || p.contains("gambling chip")) {
            return Direction.DISCARD;
        }
        // RetainCardsAction (Well-Laid Plans / "retain up to N") uses the
        // Chinese prompt "保留" with no discard/top keyword.  It used to fall
        // through to CHOOSE, so the panel kept naming the next best card and
        // never respected the two-card cap.
        if (p.contains("retain") || p.contains("保留")) {
            return Direction.RETAIN;
        }
        if (p.contains("bottom") || p.contains("底部")
                || (p.contains("弃牌堆") && !p.contains("手牌"))) {
            return Direction.BOTTOM;
        }
        if (p.contains("top") || p.contains("draw pile") || p.contains("牌堆顶") || p.contains("顶部")) {
            return Direction.TOP;
        }
        // Chinese discard prompts are usually "选择一张牌丢弃/弃掉", not the
        // noun "弃牌".  Missing "丢弃" made the screen fall through to CHOOSE,
        // which sorts by card quality and recommended discarding the best card.
        if (p.contains("discard") || p.contains("丢弃") || p.contains("弃掉")
                || p.contains("弃置") || p.contains("舍弃") || p.contains("弃一张")
                || (p.contains("弃牌") && !p.contains("弃牌堆"))) {
            return Direction.DISCARD;
        }
        if (p.contains("exhaust") || p.contains("消耗")) {
            return Direction.EXHAUST;
        }
        if (p.contains("copy") || p.contains("duplicate") || p.contains("复制")) {
            return Direction.COPY;
        }
        if (forUpgrade || p.contains("upgrade") || p.contains("升级")) {
            return Direction.UPGRADE;
        }
        if (forTransform || p.contains("transform") || p.contains("变化")) {
            return Direction.TRANSFORM;
        }
        return Direction.CHOOSE;
    }

    private double value(AbstractCard c, Direction dir) {
        if (c == null) {
            return -999;
        }
        if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
            return dir == Direction.DISCARD || dir == Direction.EXHAUST
                    || dir == Direction.BOTTOM || dir == Direction.TRANSFORM ? 100 : -100;
        }
        if (dir == Direction.DISCARD && discardPayoff(c)) {
            // Tactician/Reflex want to be discarded; this is the one case
            // where the "best" card to throw away is a good card.
            return 120;
        }
        CardDatabase.Entry e = CardDatabase.get(c);
        double v = e == null ? 5.0 : e.score;
        if (dir == Direction.UPGRADE) {
            v += upgradeDelta(c);
        }
        if (dir == Direction.DISCARD || dir == Direction.EXHAUST
                || dir == Direction.BOTTOM || dir == Direction.TRANSFORM) {
            if (CardDatabase.norm(c.cardID).contains("strike")
                    || CardDatabase.norm(c.cardID).contains("defend")) {
                return 80;
            }
            return 50 - v;
        }
        return v;
    }

    private boolean discardPayoff(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        return id.contains("tactician") || id.contains("reflex");
    }

    private double upgradeDelta(AbstractCard c) {
        if (!c.canUpgrade()) {
            return -5;
        }
        AbstractCard copy = c.makeStatEquivalentCopy();
        copy.upgrade();
        double d = 0;
        d += Math.max(0, copy.baseDamage - c.baseDamage) * 0.35;
        d += Math.max(0, copy.baseBlock - c.baseBlock) * 0.25;
        d += Math.max(0, copy.baseMagicNumber - c.baseMagicNumber) * 0.18;
        d += Math.max(0, c.cost - copy.cost) * 1.2;
        return d;
    }

    private Object read(Object target, String name, Object fallback) {
        if (target == null) {
            return fallback;
        }
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(target);
            return v == null ? fallback : v;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
