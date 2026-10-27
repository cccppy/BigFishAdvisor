package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
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
    public enum Direction {TOP, BOTTOM, DISCARD, EXHAUST, HAND, UPGRADE, TRANSFORM, COPY, CHOOSE}

    public static final class Result {
        public Direction direction = Direction.CHOOSE;
        public String title = "选牌建议";
        public String kicker = "选牌 · 先看方向";
        public String compact = "";
        public List<String> lines = new ArrayList<String>();
        public AbstractCard best;
        public String tag = "选";
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
        int count = anyNumber ? Math.max(1, hs.numCardsToSelect) : Math.max(1, hs.numCardsToSelect);
        return build(p, new ArrayList<AbstractCard>(p.hand.group), dir, count, anyNumber);
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
        Collections.sort(cards, new Comparator<AbstractCard>() {
            @Override
            public int compare(AbstractCard a, AbstractCard b) {
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
                r.lines.add("为什么呢：优先弃诅咒/状态/基础打击防御；如果牌组有弃牌收益，再考虑弃能触发效果的牌。");
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
        if (cards.size() > 1) {
            AbstractCard second = cards.get(1);
            r.lines.add("另一个选择是：【" + CardDatabase.shortName(second) + "】"
                    + "（评分 " + String.format("%.1f", value(second, dir)) + "）；"
                    + (bad ? "除非它更废，否则首选更稳。" : "除非它更关键，否则首选收益更高。"));
        }
        r.lines.add("选之前想一下：先确认这张牌是不是你愿意失去/放回的那张；放顶会改变下一抽，"
                + "弃牌/消耗会改变本回合手牌，复制/变化要注意费用与牌组厚度。");
        if (many && cards.size() > 1) {
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

    private Direction classify(String prompt, boolean forUpgrade, boolean forTransform) {
        String p = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        if (p.contains("bottom") || p.contains("底部")) {
            return Direction.BOTTOM;
        }
        if (p.contains("top") || p.contains("draw pile") || p.contains("牌堆顶") || p.contains("顶部")) {
            return Direction.TOP;
        }
        if (p.contains("into your hand") || p.contains("add") && p.contains("hand")
                || p.contains("回到手牌") || p.contains("拿回")) {
            return Direction.HAND;
        }
        if (p.contains("discard") || p.contains("弃牌")) {
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
