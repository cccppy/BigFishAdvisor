package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Explains event choices. It is deliberately conservative: the baseline comes
 * from what an option costs/gives, then a small table covers the classic events
 * where option order is stable.
 */
public final class EventAdvisor {
    public static final class Option {
        public LargeDialogOptionButton button;
        public String text;
        public double score;
        public String note;
        public String verdict;
        public int index;
    }

    public static final class Result {
        public String title;
        public String kicker;
        public List<Option> options = new ArrayList<Option>();
        public Option best;
        public List<String> lines = new ArrayList<String>();
    }

    private static final Map<String, String> TITLES = new HashMap<String, String>();
    private static final Map<String, double[]> SCORES = new HashMap<String, double[]>();
    private static final Map<String, String[]> NOTES = new HashMap<String, String[]>();

    static {
        title("NeowEvent", "开局祝福（Neow）");
        title("BigFish", "大肥鱼");
        title("Cleric", "牧师");
        title("DeadAdventurer", "死去的冒险者");
        title("GoldenIdolEvent", "金像");
        title("LivingWall", "活墙");
        title("ScrapOoze", "废料史莱姆");
        title("ShiningLight", "耀眼之光");
        title("Bonfire", "营火精灵");
        title("Duplicator", "复制者");
        title("FaceTrader", "换脸商人");
        title("ForgottenAltar", "被遗忘的祭坛");
        title("Ghosts", "幽灵");
        title("Lab", "实验室");
        title("TheLibrary", "图书馆");
        title("MaskedBandits", "蒙面强盗");
        title("MindBloom", "心灵绽放");
        title("Nloth", "恩洛斯");
        title("NoteForYourself", "给自己的笔记");
        title("Beggar", "老乞丐");
        title("Portal", "传送门");
        title("PurificationShrine", "净化神龛");
        title("SensoryStone", "感官之石");
        title("Sssserpent", "蛇");
        title("Transmogrifier", "变形者");
        title("Vampires", "吸血鬼");
        title("WindingHalls", "蜿蜒走廊");
        title("MoaiHead", "摩艾石像");
        title("Nest", "鸟巢");
        title("TheMausoleum", "陵墓");
        title("GoldenWing", "金翼");
        title("GoopPuddle", "黏液");
        title("Mushrooms", "蘑菇");
        title("AccursedBlacksmith", "被诅咒的铁匠");
        title("GoldShrine", "金币神龛");
        title("UpgradeShrine", "升级神龛");
        title("WomanInBlue", "蓝衣女子");
        title("TheJoust", "马上长矛");
        title("Colosseum", "角斗场");
        title("CursedTome", "诅咒之书");
        title("DrugDealer", "毒贩");
        title("Designer", "设计师");
        title("BackToBasics", "回归基础");
        title("WeMeetAgain", "再会");
        title("Addict", "成瘾者");
        title("Falling", "坠落");
        title("GremlinMatchGame", "地精翻牌");
        title("GremlinWheelGame", "地精转盘");

        scores("BigFish", 6.6, 7.8, 8.2);
        notes("BigFish", "回复 1/3 最大生命；血线低时最稳。", "+5 最大生命；长期更耐打。",
                "随机遗物；前期上限最高。");
        scores("Cleric", 7.0, 8.4, 5.0);
        notes("Cleric", "35 金币换 25% 生命；血低才值。", "50 金币删一张牌；牌组越精简越强。",
                "不花金币，保守离开。");
        scores("LivingWall", 8.2, 7.4, 6.8);
        notes("LivingWall", "删一张牌，优先诅咒/基础牌。", "转化一张牌，可能变好也可能变差。",
                "升级一张牌，稳定提升。");
        scores("ShiningLight", 5.4, 6.0);
        notes("ShiningLight", "失去生命升级两张牌；血量够才进。", "离开，不冒险。");
        scores("Duplicator", 8.6, 5.0);
        notes("Duplicator", "复制关键牌，核心牌直接翻倍。", "离开。");
        notes("Vampires", "失去最大生命换 5 张噬咬；长战续航。", "拒绝，保血量上限。");
        scores("Vampires", 5.0, 5.8);
        scores("Nloth", 7.2, 5.2);
        notes("Nloth", "用一个遗物换两个遗物，通常赚。", "离开。");
        scores("PurificationShrine", 8.4, 5.0);
        notes("PurificationShrine", "删牌，优先诅咒/基础牌。", "离开。");
        scores("Transmogrifier", 8.2, 5.0);
        notes("Transmogrifier", "转化一张牌，可能变好也可能变差。", "离开。");
        scores("UpgradeShrine", 7.8, 5.0);
        notes("UpgradeShrine", "升级关键牌，长期收益。", "离开。");
        scores("GoldShrine", 7.2);
        scores("TheLibrary", 8.0, 5.0);
        notes("TheLibrary", "从 20 张牌里选一张，补核心。", "离开。");
        scores("Lab", 7.2, 5.0);
        notes("Lab", "三瓶药水，短期保命/爆发。", "离开。");
        scores("NoteForYourself", 7.0, 5.0);
        notes("NoteForYourself", "拿回上局的一张牌，可能很强。", "离开。");
        scores("Beggar", 7.2, 5.0);
        notes("Beggar", "75 金币删一张牌，牌组精简。", "离开。");
        scores("Portal", 6.4, 5.0);
        notes("Portal", "跳过本幕；少打层数也少奖励，高风险。", "离开。");
        scores("FaceTrader", 6.0, 5.2);
        scores("Ghosts", 5.2, 6.0);
        notes("Ghosts", "失去最大生命拿 5 张灵体，防御强。", "拒绝，保血量上限。");
        scores("MindBloom", 5.4, 6.2, 5.8);
        scores("MaskedBandits", 5.2, 5.6);
        scores("ScrapOoze", 4.8, 5.2);
        notes("ScrapOoze", "伸手可能掉血拿遗物，血量低别摸。", "离开。");
        scores("GoldenIdolEvent", 5.6, 4.6, 5.2);
        scores("TheMausoleum", 6.0, 5.0);
        scores("Nest", 6.2, 5.0);
        scores("GoldenWing", 6.2, 5.4);
        scores("GoopPuddle", 6.2, 5.0);
        scores("Mushrooms", 6.0, 5.0);
        scores("AccursedBlacksmith", 6.4, 5.0);
        scores("CursedTome", 5.2, 6.0);
        scores("DrugDealer", 6.0, 5.0);
        scores("WeMeetAgain", 6.0, 5.0);
        scores("Addict", 6.2, 5.0);
        scores("WindingHalls", 5.2, 6.0, 6.0);
        scores("Bonfire", 8.0, 6.4);
        scores("Sssserpent", 5.2, 6.2);
        scores("MoaiHead", 6.6, 5.0);
        scores("TheJoust", 5.0, 5.0);
        scores("Colosseum", 5.2, 5.2);
        scores("WomanInBlue", 6.0, 5.0);
    }

    private static void title(String id, String name) {
        TITLES.put(id, name);
    }

    private static void scores(String id, double... values) {
        SCORES.put(id, values);
    }

    private static void notes(String id, String... values) {
        NOTES.put(id, values);
    }

    public static boolean hasOptions(AbstractEvent event) {
        return !options(event).isEmpty();
    }

    private static List<LargeDialogOptionButton> options(AbstractEvent event) {
        List<LargeDialogOptionButton> out = new ArrayList<LargeDialogOptionButton>();
        if (event == null) {
            return out;
        }
        try {
            if (event.imageEventText != null && event.imageEventText.optionList != null) {
                out.addAll(event.imageEventText.optionList);
            }
        } catch (Throwable ignored) {
        }
        if (out.isEmpty()) {
            try {
                // Neow and several other events keep options on the event's
                // own RoomEventDialog instance, not the static fallback.
                if (event.roomEventText != null && event.roomEventText.optionList != null) {
                    out.addAll(event.roomEventText.optionList);
                }
            } catch (Throwable ignored) {
            }
        }
        if (out.isEmpty()) {
            try {
                if (RoomEventDialog.optionList != null) {
                    out.addAll(RoomEventDialog.optionList);
                }
            } catch (Throwable ignored) {
            }
        }
        List<LargeDialogOptionButton> visible = new ArrayList<LargeDialogOptionButton>();
        for (LargeDialogOptionButton b : out) {
            if (b == null || b.isDisabled) {
                continue;
            }
            String msg = clean(b.msg);
            if (msg.length() > 0) {
                visible.add(b);
            }
        }
        return visible;
    }

    public Result analyze(AbstractEvent event, AbstractPlayer player) {
        List<LargeDialogOptionButton> buttons = options(event);
        if (buttons.size() < 1 || player == null) {
            return null;
        }
        String id = event.getClass().getSimpleName();
        Result r = new Result();
        r.title = TITLES.containsKey(id) ? TITLES.get(id) : "事件：" + id;
        r.kicker = id.equals("NeowEvent") ? "开局祝福 · 先选这个" : "事件选择 · 先选这个";

        double[] override = SCORES.get(id);
        String[] overrideNotes = NOTES.get(id);
        int hpPct = player.currentHealth * 100 / Math.max(1, player.maxHealth);
        for (int i = 0; i < buttons.size(); i++) {
            LargeDialogOptionButton b = buttons.get(i);
            Option o = new Option();
            o.button = b;
            o.index = i;
            o.text = clean(b.msg);
            o.score = id.equals("NeowEvent") ? neowScore(o.text, player, hpPct)
                    : genericScore(o.text, player, hpPct);
            if (override != null && i < override.length) {
                o.score = override[i] + contextAdjust(o.text, player, hpPct);
            }
            o.score = Math.max(0.0, Math.min(10.0, o.score));
            o.note = overrideNotes != null && i < overrideNotes.length
                    ? overrideNotes[i] : note(o.text, player, hpPct);
            o.verdict = verdict(o);
            r.options.add(o);
        }
        if (id.equals("Falling")) {
            falling(r, player);
        }
        for (Option o : r.options) {
            if (r.best == null || o.score > r.best.score) {
                r.best = o;
            }
        }
        buildText(r, player, hpPct, id);
        return r;
    }

    /** Falling shows three cards from the deck; the least valuable one is best to lose. */
    private void falling(Result r, AbstractPlayer p) {
        for (Option o : r.options) {
            for (AbstractCard c : p.masterDeck.group) {
                String en = c.name == null ? "" : c.name;
                String zh = CardDatabase.shortName(c);
                if ((en.length() > 0 && o.text.contains(en))
                        || (zh.length() > 0 && o.text.contains(zh))) {
                    CardDatabase.Entry e = CardDatabase.get(c);
                    double value = e == null ? 5.0 : e.score;
                    if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
                        value = 0.5;
                    }
                    o.score = 10.0 - value;
                    o.note = "舍弃价值最低的牌；这张对当前牌组作用最小，优先丢掉。";
                    o.verdict = "最该丢";
                    break;
                }
            }
        }
    }

    private double neowScore(String text, AbstractPlayer p, int hpPct) {
        String t = text.toLowerCase(Locale.ROOT);
        if (has(t, "boss relic") || has(t, "boss遗物") || has(t, "boss 遗物")) {
            return 5.0;
        }
        if (has(t, "remove") || has(t, "移除") || has(t, "删")) {
            return 8.8;
        }
        if (has(t, "max hp") || has(t, "最大生命") || has(t, "最大血量")) {
            return has(t, "lose") || has(t, "失去") ? 5.2 : 8.5;
        }
        if (has(t, "rare relic") || has(t, "稀有遗物")) {
            return 8.4;
        }
        if (has(t, "rare card") || has(t, "稀有牌")) {
            return 8.0;
        }
        if (has(t, "upgrade") || has(t, "升级")) {
            return 7.6;
        }
        if (has(t, "gold") || has(t, "金币")) {
            return 7.3 + (p.gold < 100 ? 0.3 : 0.0);
        }
        if (has(t, "transform") || has(t, "转化")) {
            return 7.0;
        }
        if (has(t, "relic") || has(t, "遗物")) {
            return 7.8;
        }
        if (has(t, "three cards") || has(t, "三张牌") || has(t, "3 cards")) {
            return 6.0;
        }
        if (has(t, "curse") || has(t, "诅咒")) {
            return 4.8;
        }
        if (has(t, "card") || has(t, "牌")) {
            return 6.8;
        }
        return 6.0;
    }

    private double genericScore(String text, AbstractPlayer p, int hpPct) {
        String t = text.toLowerCase(Locale.ROOT);
        double s = 5.0;
        if (has(t, "remove") || has(t, "移除") || has(t, "删除")) {
            s += 3.0;
        }
        if (has(t, "upgrade") || has(t, "升级")) {
            s += 2.0;
        }
        if (has(t, "max hp") || has(t, "最大生命") || has(t, "最大血量")) {
            s += has(t, "lose") || has(t, "失去") ? -2.0 : 2.6;
        }
        if (has(t, "relic") || has(t, "遗物")) {
            s += 2.2;
        }
        if (has(t, "rare") || has(t, "稀有")) {
            s += 1.8;
        }
        if (has(t, "transform") || has(t, "转化")) {
            s += 1.6;
        }
        if (has(t, "duplicate") || has(t, "复制")) {
            s += 2.4;
        }
        if (has(t, "heal") || has(t, "回复") || has(t, "治疗")) {
            s += hpPct < 60 ? 2.4 : 0.8;
        }
        if (has(t, "gold") || has(t, "金币")) {
            s += p.gold < 100 ? 1.5 : 0.8;
        }
        if (has(t, "potion") || has(t, "药水")) {
            s += 0.8;
        }
        if (has(t, "curse") || has(t, "诅咒")) {
            s -= 2.8;
        }
        if (has(t, "lose max hp") || has(t, "失去最大生命") || has(t, "失去最大血量")) {
            s -= 2.2;
        }
        if (has(t, "lose hp") || has(t, "失去生命") || has(t, "失去血量")) {
            s -= hpPct < 55 ? 2.2 : 0.8;
        }
        if (has(t, "lose all gold") || has(t, "失去所有金币")) {
            s -= 2.0;
        }
        if (has(t, "lose") && (has(t, "relic") || has(t, "遗物"))) {
            s -= 2.4;
        }
        if (has(t, "fight") || has(t, "战斗") || has(t, "精英") || has(t, "elite") || has(t, "boss")) {
            s -= hpPct < 65 ? 2.0 : 0.4;
        }
        if (has(t, "leave") || has(t, "离开") || has(t, "proceed") || has(t, "继续") || has(t, "不要")) {
            s = 5.2;
        }
        if (has(t, "pay") || has(t, "支付") || has(t, "金币")) {
            s -= 0.3;
        }
        return s;
    }

    private double contextAdjust(String text, AbstractPlayer p, int hpPct) {
        String t = text.toLowerCase(Locale.ROOT);
        double adjust = 0.0;
        if (has(t, "heal") || has(t, "回复")) {
            adjust += hpPct < 55 ? 0.8 : -0.4;
        }
        if (has(t, "remove") || has(t, "移除")) {
            if (p.masterDeck.size() > 18) {
                adjust += 0.3;
            }
        }
        if (has(t, "fight") || has(t, "战斗") || has(t, "elite") || has(t, "精英")) {
            adjust -= hpPct < 60 ? 1.2 : 0.2;
        }
        return adjust;
    }

    private String note(String text, AbstractPlayer p, int hpPct) {
        String t = text.toLowerCase(Locale.ROOT);
        if (has(t, "remove") || has(t, "移除") || has(t, "删除")) {
            return "删掉一张废牌，牌组更精简；优先删诅咒/基础打击/防御。";
        }
        if (has(t, "upgrade") || has(t, "升级")) {
            return "升级关键牌，长期收益；优先升级核心攻击/能力。";
        }
        if (has(t, "max hp") || has(t, "最大生命") || has(t, "最大血量")) {
            if (has(t, "lose") || has(t, "失去")) {
                return "永久降低血量上限，长线代价大；血量低时更危险。";
            }
            return "提高最大生命，最稳的长期收益。";
        }
        if (has(t, "relic") || has(t, "遗物")) {
            return "遗物是永久提升；注意交换/战斗代价。";
        }
        if (has(t, "gold") || has(t, "金币")) {
            return "金币能去商店买关键牌/遗物；当前金币 " + p.gold + "。";
        }
        if (has(t, "heal") || has(t, "回复") || has(t, "治疗")) {
            return hpPct < 60 ? "回血让你更安全；血线低时优先。" : "回血稳，但不会提升上限。";
        }
        if (has(t, "curse") || has(t, "诅咒")) {
            return "会往牌组塞诅咒，增加卡手风险；短期收益换长期负担。";
        }
        if (has(t, "transform") || has(t, "转化")) {
            return "随机转化，可能变好也可能变差；牌组越精简越值得赌。";
        }
        if (has(t, "fight") || has(t, "战斗") || has(t, "精英") || has(t, "elite") || has(t, "boss")) {
            return hpPct < 65 ? "会进入战斗，当前血量偏低，先掂量。"
                    : "会进入战斗；血量和牌组够强时，战斗奖励通常值得。";
        }
        if (has(t, "lose") || has(t, "失去")) {
            return "有永久/不可逆代价，新手优先保守。";
        }
        if (has(t, "leave") || has(t, "离开") || has(t, "proceed") || has(t, "继续")) {
            return "保守离开，不赚但也不冒险。";
        }
        if (has(t, "card") || has(t, "牌")) {
            return "给牌/换牌；注意牌组厚度，不要什么都拿。";
        }
        return "收益/代价不明显，按当前血量与牌组决定。";
    }

    private void buildText(Result r, AbstractPlayer p, int hpPct, String id) {
        r.lines.add("大肥鱼建议：选项 " + (r.best.index + 1) + "「" + oneLine(r.best.text) + "」");
        r.lines.add("为什么呢：" + r.best.note);
        for (Option o : r.options) {
            String mark = o == r.best ? "（推荐）" : "";
            r.lines.add("选项 " + (o.index + 1) + "「" + oneLine(o.text) + "」："
                    + o.verdict + " · 评分 " + String.format("%.1f", o.score) + mark);
            r.lines.add("说明：" + o.note);
        }
        r.lines.add("我会这么选：先排除会直接致死/无法翻盘的选项，再比较即时收益与不可逆代价；"
                + "同一档收益时选更稳定、后续更容易补回来的。");
        if (id.equals("NeowEvent")) {
            r.lines.add("大肥鱼的想法：开局优先稳定收益——删牌/最大生命/金币/普通遗物；Boss 遗物换风险高，新手别第一选。");
        } else {
            r.lines.add("大肥鱼的想法：先看不可逆代价（最大生命、诅咒、强制战斗），再看即时收益；血量 " + hpPct + "%。");
        }
    }

    private String verdict(Option o) {
        String t = o.text.toLowerCase(Locale.ROOT);
        if (has(t, "leave") || has(t, "离开") || has(t, "proceed") || has(t, "不要")) {
            return "保守离开";
        }
        if (o.score >= 8.4) {
            return "强，优先";
        }
        if (o.score >= 7.3) {
            return "不错，值得";
        }
        if (o.score >= 6.2) {
            return "看情况";
        }
        if (o.score >= 5.2) {
            return "保守";
        }
        return "风险高";
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replace("\n", " ").trim();
    }

    private static String shortText(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\n", " ").trim();
        return t.length() <= max ? t : t.substring(0, Math.max(0, max - 1)) + "...";
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\[[^\\]]*\\]", "").replace("\n", " ").trim();
    }

    private static boolean has(String haystack, String needle) {
        return haystack.contains(needle);
    }
}
