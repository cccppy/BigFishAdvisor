package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Recommends a campfire option and, for smith, the best card to upgrade. */
public final class CampfireAdvisor {
    public static final class Option {
        public AbstractCampfireOption button;
        public String name;
        public double score;
        public String note;
    }

    public static final class Upgrade {
        public AbstractCard card;
        public double score;
        public String note;
    }

    public static final class Result {
        public String title = "营火建议";
        public String kicker = "营火 · 先选这个";
        public String compact = "";
        public List<Option> options = new ArrayList<Option>();
        public Option best;
        public List<Upgrade> upgrades = new ArrayList<Upgrade>();
        public Upgrade upgradeBest;
        public List<Upgrade> purgeOptions = new ArrayList<Upgrade>();
        public Upgrade purgeBest;
        public List<String> lines = new ArrayList<String>();
    }

    private static Field buttonsField;
    private static boolean buttonsTried;

    public Result analyze(RestRoom room, AbstractPlayer p) {
        if (room == null || room.campfireUI == null || p == null) {
            return null;
        }
        List<AbstractCampfireOption> buttons = buttons(room.campfireUI);
        if (buttons.isEmpty()) {
            return null;
        }
        Result r = new Result();
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        r.upgrades = upgradeOptions(p);
        r.upgradeBest = r.upgrades.isEmpty() ? null : r.upgrades.get(0);

        for (AbstractCampfireOption b : buttons) {
            if (!b.usable) {
                continue;
            }
            Option o = new Option();
            o.button = b;
            o.name = name(b);
            o.note = note(b, p, r.upgradeBest);
            o.score = score(b, p, hpPct, r.upgradeBest);
            r.options.add(o);
        }
        if (r.options.isEmpty()) {
            return null;
        }
        for (Option o : r.options) {
            if (r.best == null || o.score > r.best.score) {
                r.best = o;
            }
        }
        if (r.best != null) {
            r.title = "营火：选" + r.best.name;
            r.compact = r.best.name;
            if (r.best.name.equals("锻造") && r.upgradeBest != null) {
                r.compact = "锻造 " + CardDatabase.shortName(r.upgradeBest.card);
            }
        }
        buildText(r, p, hpPct);
        return r;
    }

    public Result analyzePurge(AbstractPlayer p) {
        return analyzePurge(p, p == null ? null : p.masterDeck.group);
    }

    public Result analyzePurge(AbstractPlayer p, List<AbstractCard> available) {
        if (p == null) {
            return null;
        }
        Result r = new Result();
        r.title = "删除哪张牌";
        r.kicker = "删牌 · 选择最该丢的牌";
        List<AbstractCard> cards = available == null || available.isEmpty()
                ? p.masterDeck.group : available;
        for (AbstractCard c : cards) {
            if (c == null) {
                continue;
            }
            Upgrade u = new Upgrade();
            u.card = c;
            u.score = purgeScore(c, p);
            u.note = purgeNote(c);
            r.purgeOptions.add(u);
        }
        Collections.sort(r.purgeOptions, new Comparator<Upgrade>() {
            @Override
            public int compare(Upgrade a, Upgrade b) {
                return Double.compare(b.score, a.score);
            }
        });
        if (r.purgeOptions.isEmpty()) {
            return null;
        }
        r.purgeBest = r.purgeOptions.get(0);
        r.compact = "删 " + CardDatabase.shortName(r.purgeBest.card);
        r.lines.add("大肥鱼建议：删除【" + CardDatabase.shortName(r.purgeBest.card) + "】");
        r.lines.add("为什么呢：" + r.purgeBest.note);
        if (r.purgeOptions.size() > 1) {
            Upgrade second = r.purgeOptions.get(1);
            r.lines.add("备选：【" + CardDatabase.shortName(second.card) + "】"
                    + "（" + String.format("%.1f", second.score) + " 分）");
        }
        r.lines.add("大肥鱼的想法：优先删诅咒/状态牌，其次删基础打击/防御或重复牌；不要删核心能力/关键攻击。");
        return r;
    }

    private double purgeScore(AbstractCard c, AbstractPlayer p) {
        if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
            return 10.0;
        }
        CardDatabase.Entry e = CardDatabase.get(c);
        double base = e == null ? 5.5 : e.score;
        double score;
        if (CardDatabase.norm(c.cardID).contains("strike")
                || CardDatabase.norm(c.cardID).contains("defend")) {
            score = 9.0;
        } else {
            score = 10.0 - base;
        }
        if (c.upgraded) {
            score -= 0.7;
        }
        int copies = 0;
        for (AbstractCard d : p.masterDeck.group) {
            if (d.cardID != null && d.cardID.equals(c.cardID)) {
                copies++;
            }
        }
        if (copies >= 2) {
            score += 0.7;
        }
        return score;
    }

    private String purgeNote(AbstractCard c) {
        if (c.type == AbstractCard.CardType.CURSE) {
            return "诅咒牌，优先删掉；它只会卡手。";
        }
        if (c.type == AbstractCard.CardType.STATUS) {
            return "状态牌，优先删掉；它只会占手牌。";
        }
        if (CardDatabase.norm(c.cardID).contains("strike")
                || CardDatabase.norm(c.cardID).contains("defend")) {
            return "基础打击/防御后期价值低，删掉能提高关键牌上手率。";
        }
        CardDatabase.Entry e = CardDatabase.get(c);
        double base = e == null ? 5.5 : e.score;
        if (base < 5.0) {
            return "这张牌对当前牌组贡献较小，删掉比留着更精简。";
        }
        return "非核心牌；如果没有诅咒/基础牌可删，再考虑它。";
    }

    public Result analyzeUpgrade(AbstractPlayer p) {
        if (p == null) {
            return null;
        }
        Result r = new Result();
        r.title = "锻造哪张牌";
        r.kicker = "营火 · 升级选择";
        r.upgrades = upgradeOptions(p);
        if (r.upgrades.isEmpty()) {
            return null;
        }
        r.upgradeBest = r.upgrades.get(0);
        r.compact = "锻造 " + CardDatabase.shortName(r.upgradeBest.card);
        r.lines.add("大肥鱼建议：升级【" + CardDatabase.shortName(r.upgradeBest.card) + "】");
        r.lines.add("为什么呢：" + r.upgradeBest.note);
        if (r.upgrades.size() > 1) {
            Upgrade second = r.upgrades.get(1);
            r.lines.add("备选：【" + CardDatabase.shortName(second.card) + "】"
                    + "（" + String.format("%.1f", second.score) + " 分）");
        }
        r.lines.add("大肥鱼的想法：优先升级核心攻击/能力，而不是基础打击/防御；费用降低、伤害/格挡提升最大的牌最赚。");
        return r;
    }

    private List<AbstractCampfireOption> buttons(CampfireUI ui) {
        try {
            if (!buttonsTried) {
                buttonsTried = true;
                buttonsField = CampfireUI.class.getDeclaredField("buttons");
                buttonsField.setAccessible(true);
            }
            Object value = buttonsField.get(ui);
            if (value instanceof List) {
                List<AbstractCampfireOption> out = new ArrayList<AbstractCampfireOption>();
                for (Object o : (List<?>) value) {
                    if (o instanceof AbstractCampfireOption) {
                        out.add((AbstractCampfireOption) o);
                    }
                }
                return out;
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<AbstractCampfireOption>();
    }

    private String name(AbstractCampfireOption b) {
        String cls = b.getClass().getSimpleName();
        if (cls.equals("RestOption")) {
            return "休息";
        }
        if (cls.equals("SmithOption")) {
            return "锻造";
        }
        if (cls.equals("DigOption")) {
            return "挖掘";
        }
        if (cls.equals("LiftOption")) {
            return "举铁";
        }
        if (cls.equals("TokeOption")) {
            return "抽烟（删牌）";
        }
        if (cls.equals("RecallOption")) {
            return "回忆（红钥匙）";
        }
        return cls.replace("Option", "");
    }

    private double score(AbstractCampfireOption b, AbstractPlayer p, int hpPct, Upgrade up) {
        String name = name(b);
        double s;
        if (name.equals("休息")) {
            s = hpPct < 45 ? 9.4 : hpPct < 60 ? 8.0 : hpPct < 75 ? 6.2 : 4.6;
        } else if (name.equals("锻造")) {
            s = 7.2;
            if (up != null) {
                s += Math.min(1.6, up.score * 0.12);
            }
            if (hpPct < 50) {
                s -= 1.0;
            }
        } else if (name.equals("挖掘")) {
            s = 8.2;
        } else if (name.equals("抽烟（删牌）")) {
            s = 8.4;
            if (p.masterDeck.size() < 15) {
                s -= 0.8;
            }
        } else if (name.equals("举铁")) {
            s = DeckSummary.of(p).tag("str") >= 3 ? 7.5 : 5.8;
        } else if (name.equals("回忆（红钥匙）")) {
            s = 5.8;
        } else {
            s = 6.0;
        }
        return s;
    }

    private String note(AbstractCampfireOption b, AbstractPlayer p, Upgrade up) {
        String name = name(b);
        int heal = (int) Math.floor(p.maxHealth * 0.3);
        if (name.equals("休息")) {
            return "回复约 " + heal + " 生命；血量低时最稳。";
        }
        if (name.equals("锻造")) {
            return up == null ? "升级一张牌，但当前没有可升级牌。"
                    : "升级【" + CardDatabase.shortName(up.card) + "】收益最高。";
        }
        if (name.equals("挖掘")) {
            return "获得一个遗物；遗物是永久提升。";
        }
        if (name.equals("抽烟（删牌）")) {
            return "删掉一张牌，优先诅咒/基础打击/防御；牌组越精简越强。";
        }
        if (name.equals("举铁")) {
            return "永久 +1 力量；力量战很赚，其它流派一般。";
        }
        if (name.equals("回忆（红钥匙）")) {
            return "拿红钥匙，为进入心脏路线做准备；普通通关可以先不拿。";
        }
        return "可选营火操作。";
    }

    private void buildText(Result r, AbstractPlayer p, int hpPct) {
        r.lines.add(String.format("现在的情况：生命 %d/%d（%d%%），牌组 %d 张；营火选项 %d 个。",
                p.currentHealth, p.maxHealth, hpPct, p.masterDeck.size(), r.options.size()));
        r.lines.add("大肥鱼建议：" + (r.best == null ? "先休息" : r.best.name)
                + (r.best == null ? "" : "（" + String.format("%.1f", r.best.score) + " 分）"));
        if (r.best != null) {
            r.lines.add("为什么呢：" + r.best.note);
        }
        for (Option o : r.options) {
            r.lines.add("选项「" + o.name + "」：" + (o.note == null ? "" : o.note)
                    + " · 评分 " + String.format("%.1f", o.score)
                    + (o == r.best ? "（推荐）" : ""));
        }
        if (r.upgradeBest != null) {
            r.lines.add("锻造首选：【" + CardDatabase.shortName(r.upgradeBest.card) + "】"
                    + "（未升级 · " + String.format("%.1f", r.upgradeBest.score) + " 分）："
                    + r.upgradeBest.note);
            for (int i = 1; i < r.upgrades.size() && i < 3; i++) {
                Upgrade u = r.upgrades.get(i);
                r.lines.add("锻造备选：" + CardDatabase.shortName(u.card)
                        + "（" + String.format("%.1f", u.score) + " 分）");
            }
        }
        r.lines.add("我会这么选：血量低于 55% 或下一战是精英/Boss 时，休息的保命价值会上升；"
                + "否则升级核心牌更赚。升级只选一张，不要把升级浪费在基础打击/防御上。");
    }

    private List<Upgrade> upgradeOptions(AbstractPlayer p) {
        List<Upgrade> out = new ArrayList<Upgrade>();
        for (AbstractCard c : p.masterDeck.group) {
            if (c == null || c.upgraded || c.timesUpgraded > 0 || !c.canUpgrade()) {
                continue;
            }
            Upgrade u = new Upgrade();
            u.card = c;
            u.score = upgradeScore(c);
            u.note = upgradeNote(c, u.score);
            out.add(u);
        }
        Collections.sort(out, new Comparator<Upgrade>() {
            @Override
            public int compare(Upgrade a, Upgrade b) {
                return Double.compare(b.score, a.score);
            }
        });
        return out;
    }

    private double upgradeScore(AbstractCard c) {
        CardDatabase.Entry entry = CardDatabase.get(c);
        double base = entry == null ? 5.4 : entry.score;
        if (CardDatabase.norm(c.cardID).contains("strike")
                || CardDatabase.norm(c.cardID).contains("defend")) {
            base -= 1.3;
        }
        AbstractCard copy = c.makeStatEquivalentCopy();
        copy.upgrade();
        double delta = Math.max(0, copy.baseDamage - c.baseDamage) * 0.20
                + Math.max(0, copy.baseBlock - c.baseBlock) * 0.16
                + Math.max(0, copy.baseMagicNumber - c.baseMagicNumber) * 0.14;
        if (copy.cost >= 0 && copy.cost < c.cost) {
            delta += 1.3;
        }
        if (c.exhaust && !copy.exhaust) {
            delta += 0.8;
        }
        if (c.type == AbstractCard.CardType.POWER) {
            delta += 0.45;
        }
        if (c.rarity == AbstractCard.CardRarity.RARE) {
            delta += 0.35;
        }
        return base + 1.0 + delta;
    }

    private String upgradeNote(AbstractCard c, double score) {
        AbstractCard copy = c.makeStatEquivalentCopy();
        copy.upgrade();
        StringBuilder sb = new StringBuilder("这张还没升级；升级后");
        boolean any = false;
        if (copy.baseDamage != c.baseDamage) {
            sb.append(" 伤害 ").append(c.baseDamage).append("->").append(copy.baseDamage);
            any = true;
        }
        if (copy.baseBlock != c.baseBlock) {
            sb.append(" 格挡 ").append(c.baseBlock).append("->").append(copy.baseBlock);
            any = true;
        }
        if (copy.cost >= 0 && copy.cost < c.cost) {
            sb.append(" 费用 ").append(c.cost).append("->").append(copy.cost);
            any = true;
        }
        if (!any) {
            sb.append("效果增强");
        }
        sb.append("；综合升级价值 ").append(String.format("%.1f", score)).append(" 分。");
        return sb.toString();
    }

    private static String shortText(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "...";
    }
}
