package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;
import java.util.List;

/**
 * Silent mechanics: poison is delayed damage, shivs are a card-count engine,
 * discard is a resource trigger, and dexterity scales every block card.
 */
public final class SilentAdvisor {
    private SilentAdvisor() {
    }

    public static CharacterAdvisor.Read readCard(AbstractPlayer p, AbstractCard c,
                                                 List<AbstractMonster> alive, AbstractMonster target,
                                                 boolean aoe, int energy, int incoming) {
        CharacterAdvisor.Read r = new CharacterAdvisor.Read();
        if (p == null || c == null) {
            return r;
        }
        String id = CardDatabase.norm(c.cardID);
        int poison = poisonOn(target);
        int deckPoison = countTag(p, "poison");
        int shiv = countTag(p, "shiv");
        int discard = countTag(p, "discard");
        int dex = dexterity(p);
        double bonus = 0.0;

        if ("catalyst".equals(id)) {
            if (poison <= 0) {
                bonus -= 3.2;
                note(r, "目标现在没有毒，【催化剂】打出去是空的；先上毒再催化。");
            } else {
                bonus += 3.8 + Math.min(4.0, poison * 0.18);
                note(r, "目标已有 " + poison + " 层毒；【催化剂】是“当前毒量 ×2/×3”，"
                        + "不是加固定值。毒越厚越赚，先算这回合能不能叠起来再交。");
            }
        } else if ("bane".equals(id)) {
            if (poison > 0) {
                bonus += 2.8;
                note(r, "目标有毒，【灾祸】会打出双倍伤害；先上毒再打它，顺序反过来就少一半。");
            } else {
                bonus -= 1.6;
                note(r, "目标没毒，【灾祸】只有普通伤害；先打毒牌再打它。");
            }
        } else if (poisonApplied(c) > 0) {
            int gain = poisonApplied(c);
            bonus += 0.9 + Math.min(2.2, gain * 0.14);
            if (poison > 0) {
                bonus += 0.35;
            }
            if (aoe && alive.size() >= 2) {
                bonus += 0.5;
            }
            if ("noxiousfumes".equals(id)) {
                note(r, "【毒雾】是每回合开始给 " + gain + " 层毒，不是打出去立刻生效；"
                        + "越早开越赚，但要先确认这回合不会被打穿。");
            } else if ("envenom".equals(id)) {
                note(r, "【涂毒】是能力：之后每次攻击造成未格挡伤害时额外加 1 层毒，"
                        + "不是这一张立刻上毒；小刀/多段攻击最能放大它。");
            } else if ("bouncingflask".equals(id)) {
                note(r, "【弹跳药瓶】会随机给敌人上 " + gain + " 层毒，共 3 次；"
                        + "毒可能分散，单目标时更集中，多目标时别指望它精准补刀。");
            } else {
                note(r, "这张上 " + gain + " 层毒。毒是敌人回合开始先掉血、然后减 1，"
                        + "而且毒伤无视格挡；所以它不是即时伤害，但打高血量/多回合目标很赚。"
                        + (poison > 0 ? "目标已经有毒，继续叠不会浪费。" : "目标现在没毒，先铺第一层。"));
            }
        }

        if (tag(p, c, "shiv") || id.contains("bladedance") || id.contains("cloakanddagger")
                || id.contains("stormofsteel") || id.contains("infiniteblades")) {
            int accuracy = countId(p, "accuracy");
            bonus += 0.6 + Math.min(1.5, shiv * 0.08);
            if (accuracy > 0) {
                bonus += 0.8;
                note(r, "你有 " + accuracy + " 张【精准】；每张小刀都加伤害，"
                        + "小刀越多，精准越像一张成长核心。");
            } else if (shiv >= 3) {
                note(r, "你牌组有 " + shiv + " 张小刀/小刀来源；小刀是 0 费多段，"
                        + "配【余像】/【精准】/【苦无】能把张数变成格挡/伤害。");
            }
        }
        if ("afterimage".equals(id) || "infiniteblades".equals(id)
                || "accuracy".equals(id) || "envenom".equals(id)) {
            if (shiv >= 3) {
                bonus += 0.7;
                note(r, "你的小刀牌有 " + shiv + " 张；【余像】每张牌给格挡，"
                        + "【精准】加小刀伤害，【涂毒】让小刀附带毒，都是小刀流的核心配合。");
            }
        }
        if ("finisher".equals(id)) {
            int played = attacksPlayedThisTurn();
            bonus += Math.min(1.6, played * 0.15);
            note(r, "【终结技】按这回合已经打出的攻击次数算，不是牌组里的攻击数；"
                    + "本回合已打 " + played + " 张攻击，先打小刀/低费攻击再收尾。");
        }

        if (tag(p, c, "discard") || id.contains("calculatedgamble")
                || id.contains("acrobatics") || id.contains("prepared")
                || id.contains("daggerthrow") || id.contains("survivor")
                || id.contains("concentrate") || id.contains("eviscerate")
                || id.contains("sneakystrike")) {
            bonus += 0.7 + Math.min(1.2, discard * 0.08);
            if (p.hasRelic("Hovering Kite")) {
                bonus += 0.7;
                note(r, "你有【悬浮风筝】：每回合第一次弃牌给 1 能量，"
                        + "弃牌不是纯亏牌，是在换费用。");
            } else if (p.hasRelic("Tingsha")) {
                bonus += 0.5;
                note(r, "你有【叮当响】：每弃一张牌造成 3 伤，弃牌越多越像输出。");
            } else if (p.hasRelic("Tough Bandages")) {
                bonus += 0.5;
                note(r, "你有【绷带】：每弃一张牌给 3 格挡，弃牌也能转防御。");
            } else if (discard >= 3) {
                note(r, "你牌组有 " + discard + " 张弃牌/弃牌收益牌；"
                        + "弃牌流要有“弃了以后有收益”的牌，不能只为了弃而弃。");
            }
        }
        if ("calculatedgamble".equals(id)) {
            int hand = p.hand == null ? 0 : p.hand.size();
            bonus += hand >= 5 ? 0.8 : 0.1;
            note(r, "计算下注弃掉整手再抽同样数量；手牌 " + hand
                    + " 张时，先把要留的牌打掉/用掉，再弃牌换资源。");
        }

        if ("footwork".equals(id)) {
            bonus += 1.9 + Math.min(1.5, dex * 0.12);
            note(r, "【步法】给敏捷；敏捷让每张格挡牌多挡 1 点，"
                    + "不是只加一张牌，所以越早开、格挡牌越多越值。");
        } else if ("blur".equals(id)) {
            if (dex > 0) {
                bonus += 0.4;
            }
            note(r, "【模糊】的格挡会保留到下一回合；适合在敌人要打大伤害前叠一次，"
                    + "下回合还能接着用。");
        } else if ("legsweep".equals(id)) {
            bonus += 0.7;
            note(r, "【扫腿】给虚弱，直接削对面这回合输出；它既是格挡也是减伤。");
        } else if ("wraithform".equals(id)) {
            if (incoming > p.currentBlock + p.currentHealth * 0.25) {
                bonus += 1.2;
            }
            note(r, "【幽魂形态】给无形，几乎免疫伤害，但每回合掉敏捷；"
                    + "它是保命牌，不是输出牌，别在安全回合乱开。");
        }

        if ("nightmare".equals(id) || "phantasmalkiller".equals(id)
                || "welllaidplans".equals(id)) {
            bonus += 0.5;
            note(r, "这是铺垫牌：先花一张牌/能量，后面才回收；"
                    + "确认这回合不会被打穿，再投资。");
        }
        r.bonus += bonus;
        return r;
    }

    public static List<String> lines(AbstractPlayer p, List<AbstractMonster> alive,
                                     int energy, int incoming) {
        List<String> out = new ArrayList<String>();
        if (p == null) {
            return out;
        }
        int totalPoison = 0;
        for (AbstractMonster m : alive) {
            totalPoison += poisonOn(m);
        }
        out.add("毒：场上总毒 " + totalPoison + " 层。毒在敌人回合开始先造成等于层数的伤害，"
                + "然后层数减 1；所以它是“延迟但稳定”的伤害，不是打出去立刻结算。");
        out.add("【催化剂】是把目标当前毒量翻倍/三倍，不是加固定毒；"
                + "目标毒越厚越赚，没毒时打它等于空牌。"
                + "【灾祸】只对有毒目标打双倍，先上毒再打它。");
        int shiv = countTag(p, "shiv");
        int accuracy = countId(p, "accuracy");
        out.add("小刀：牌组 " + shiv + " 张相关牌，精准 " + accuracy + " 张。"
                + "小刀是 0 费多段，配精准加伤害、余像加格挡、涂毒加毒；"
                + "但小刀会消耗，别把关键小刀当普通攻击随手打光。");
        int discard = countTag(p, "discard");
        out.add("弃牌：牌组 " + discard + " 张弃牌/弃牌收益牌。"
                + (p.hasRelic("Hovering Kite") ? "你有悬浮风筝，第一次弃牌换 1 能量。"
                : p.hasRelic("Tingsha") ? "你有叮当响，每弃一张打 3。"
                : p.hasRelic("Tough Bandages") ? "你有绷带，每弃一张给 3 格挡。"
                : "没有弃牌遗物时，弃牌要有“弃了有收益”的牌才值得。"));
        int dex = dexterity(p);
        out.add("敏捷：现在 " + dex + "。每点敏捷让每张格挡牌多 1 格挡，"
                + "所以【步法】不是加一张牌，而是放大整套防御；"
                + "但敌人打你时先算格挡，再算血，别只看当前血量。");
        out.add("这回合有 " + incoming + " 伤；如果毒/小刀能在这回合收掉，优先收，"
                + "收不掉就先把毒铺满/格挡叠够，别为了追伤害把血线打穿。");
        return out;
    }

    public static String plan(AbstractPlayer p, int energy, int incoming) {
        if (p == null) {
            return "";
        }
        int poison = countTag(p, "poison");
        int shiv = countTag(p, "shiv");
        int discard = countTag(p, "discard");
        if (poison >= Math.max(3, shiv) && poison >= discard) {
            return "先铺毒、用灾祸/催化剂放大、格挡拖回合；毒是延迟伤害，别急着这回合全打光。";
        }
        if (shiv >= Math.max(3, discard)) {
            return "先开精准/余像/无限刀刃，再打小刀；小刀多段，配合每张牌/每次攻击的收益。";
        }
        if (discard >= 3) {
            return "先打要留的牌，再用弃牌换能量/触发悬浮风筝/叮当响；别把关键牌弃掉。";
        }
        return "先看敌人这回合伤害：能收就收，收不掉先叠毒/格挡，别为了输出裸吃。";
    }

    public static double pickBonus(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return 0.0;
        }
        String id = CardDatabase.norm(c.cardID);
        double b = 0.0;
        if (id.contains("catalyst") || id.contains("bane")) {
            b += d.tag("poison") >= 3 ? 0.45 : -0.1;
        }
        if (id.contains("deadlypoison") || id.contains("noxiousfumes")
                || id.contains("bouncingflask") || id.contains("corpseexplosion")) {
            b += d.tag("poison") < 4 ? 0.4 : 0.2;
        }
        if (id.contains("accuracy") || id.contains("afterimage")
                || id.contains("infiniteblades") || id.contains("envenom")) {
            b += d.tag("shiv") >= 3 ? 0.5 : 0.1;
        }
        if (id.contains("footwork")) {
            b += d.blockCards >= 5 ? 0.45 : 0.2;
        }
        if (id.contains("calculatedgamble") || id.contains("acrobatics")
                || id.contains("prepared")) {
            b += p.hasRelic("Hovering Kite") || p.hasRelic("Tingsha")
                    || p.hasRelic("Tough Bandages") ? 0.45 : 0.1;
        }
        if (id.contains("wraithform")) {
            b += d.blockCards < 5 ? 0.3 : 0.1;
        }
        return b;
    }

    public static String pickNote(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return "";
        }
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("catalyst")) {
            return "你牌组有 " + d.tag("poison") + " 张毒牌；催化剂是乘算，"
                    + "毒越厚越强，但要有稳定上毒才拿。";
        }
        if (id.contains("accuracy") || id.contains("afterimage")
                || id.contains("infiniteblades") || id.contains("envenom")) {
            return "你牌组有 " + d.tag("shiv") + " 张小刀相关牌；"
                    + "小刀是 0 费多段，精准/余像/涂毒分别把张数变成伤害/格挡/毒。";
        }
        if (id.contains("footwork")) {
            return "你牌组有 " + d.blockCards + " 张格挡牌；敏捷放大每一张格挡，"
                    + "格挡牌越多，步法越值。";
        }
        if (id.contains("calculatedgamble") || id.contains("acrobatics")
                || id.contains("prepared")) {
            return "你有 " + d.tag("discard") + " 张弃牌相关牌；"
                    + "弃牌流要靠悬浮风筝/叮当响/绷带把弃牌变成资源，不能只弃不赚。";
        }
        if (id.contains("wraithform")) {
            return "你牌组格挡牌 " + d.blockCards + " 张；幽魂形态是保命牌，"
                    + "适合格挡不够、需要拖回合的牌组。";
        }
        return "";
    }

    private static int attacksPlayedThisTurn() {
        int n = 0;
        if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.actionManager == null) {
            return 0;
        }
        for (AbstractCard played : com.megacrit.cardcrawl.dungeons.AbstractDungeon.actionManager.cardsPlayedThisTurn) {
            if (played != null && played.type == AbstractCard.CardType.ATTACK) {
                n++;
            }
        }
        return n;
    }

    private static int poisonOn(AbstractMonster m) {
        if (m == null) {
            return 0;
        }
        AbstractPower power = m.getPower("Poison");
        return power == null ? 0 : Math.max(0, power.amount);
    }

    private static int poisonApplied(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("deadlypoison")) {
            return c.upgraded ? 7 : 5;
        }
        if (id.contains("noxiousfumes")) {
            return c.upgraded ? 3 : 2;
        }
        if (id.contains("bouncingflask")) {
            return c.upgraded ? 4 : 3;
        }
        if (id.contains("cripplingcloud")) {
            return c.upgraded ? 7 : 4;
        }
        if (id.contains("corpseexplosion")) {
            return c.upgraded ? 7 : 5;
        }
        if (id.contains("envenom")) {
            return 1;
        }
        return 0;
    }

    private static int dexterity(AbstractPlayer p) {
        AbstractPower power = p == null ? null : p.getPower("Dexterity");
        return power == null ? 0 : power.amount;
    }

    private static boolean tag(AbstractPlayer p, AbstractCard c, String tag) {
        return c != null && CardDatabase.hasTag(c, tag);
    }

    private static int countTag(AbstractPlayer p, String tag) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (CardDatabase.hasTag(c, tag)) {
                n++;
            }
        }
        return n;
    }

    private static int countId(AbstractPlayer p, String part) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (CardDatabase.norm(c.cardID).contains(part)) {
                n++;
            }
        }
        return n;
    }

    private static int countType(AbstractPlayer p, AbstractCard.CardType type) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c.type == type) {
                n++;
            }
        }
        return n;
    }

    private static void note(CharacterAdvisor.Read r, String text) {
        if (r.note == null || r.note.length() == 0) {
            r.note = text;
        } else {
            r.note += " " + text;
        }
    }
}
