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
            int shivDamage = Math.max(0, 4 + strength(p) + accuracyBonus(p)
                    + (p.hasRelic("Wrist Blade") ? 4 : 0));
            int generated = shivsGenerated(c, p);
            bonus += 0.6 + Math.min(1.5, shiv * 0.08);
            if (generated > 0) {
                bonus += Math.min(3.2, generated * shivDamage * 0.18);
                if (!id.contains("infiniteblades")) {
                    // Blade Dance/Cloak and Dagger/Storm of Steel create the
                    // shivs now.  Show their generated damage as a delayed
                    // line: it needs the 0-cost shivs to be played before it
                    // becomes real, so it must never be treated as a lethal hit.
                    r.damage = generated * shivDamage;
                    r.delayedDamage = true;
                }
                note(r, "这张能生成 " + generated + " 把小刀；按当前力量/精准/袖剑，"
                        + "每把约 " + shivDamage + " 伤，小刀本身还是 0 费攻击，"
                        + "能喂【终结技】/【凌迟】/【神气制胜】，也能触发苦无/手里剑。"
                        + (r.delayedDamage ? " 真正伤害要先把这些 0 费小刀打出去。" : ""));
            }
            if (accuracy > 0) {
                bonus += 0.8;
                note(r, "你有 " + accuracy + " 张【精准】；每把小刀 +4 伤，"
                        + "小刀越多，精准越像一张成长核心。");
            } else if (shiv >= 3) {
                note(r, "你牌组有 " + shiv + " 张小刀/小刀来源；小刀是 0 费多段，"
                        + "配【余像】/【精准】/【苦无】能把张数变成格挡/伤害。");
            }
            if (id.contains("athousandcuts") || id.contains("panache")) {
                bonus += 0.9;
                note(r, "这张是“每打出一张牌/每 5 张牌”触发；小刀是 0 费牌，"
                        + "能快速堆触发次数，和无限刀刃/刀刃之舞一起才是小刀流的输出核心。");
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
            if (played <= 0) {
                bonus -= 2.4;
                note(r, "【终结技】按这回合已经打出的攻击次数算；现在一张攻击都没打，"
                        + "打出去就是 0 伤。先用小刀/低费攻击铺次数，最后再收尾。");
            } else {
                bonus += Math.min(1.6, played * 0.15);
                note(r, "【终结技】按这回合已经打出的攻击次数算，不是牌组里的攻击数；"
                        + "本回合已打 " + played + " 张攻击，现在打可以吃到 " + played + " 段。");
            }
        }
        if ("flechettes".equals(id)) {
            int skills = skillsInHand(p, c);
            if (skills <= 0) {
                bonus -= 2.4;
                note(r, "【飞镖】按手里剩下的技能牌数量打段数；现在手里没有技能牌，"
                        + "打出去就是 0 伤。先留/抽技能牌，再打它。");
            } else {
                bonus += 0.3 + Math.min(1.4, skills * 0.16);
                note(r, "手里还有 " + skills + " 张技能牌；【飞镖】会按这个数量打 "
                        + skills + " 段，技能越多越强。");
            }
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
            int junk = handJunk(p);
            int payoff = handDiscardPayoff(p);
            boolean relics = p.hasRelic("Hovering Kite") || p.hasRelic("Tingsha")
                    || p.hasRelic("Tough Bandages");
            boolean worth = hand >= 3 && (junk * 2 >= hand || payoff >= 2);
            worth = worth || (hand >= 4 && relics);
            bonus += worth ? 0.8 : -0.9;
            note(r, "【计算下注】会弃掉整手再抽同样数量，不是免费换牌；"
                    + (worth
                    ? "现在手里废牌/弃牌收益够多，可以换。"
                    : "现在手里好牌多，别为了换牌把整手全弃；先打掉要留的牌，或干脆不打。"));
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

        // Grand Finale is a real condition, not a normal attack: it can only
        // be played with an empty draw pile.  The combat filter already hides
        // it when unplayable; this branch explains the payoff/ordering when it
        // is actually available.
        if ("grandfinale".equals(id)) {
            int draw = p.drawPile == null ? 0 : p.drawPile.size();
            if (draw == 0) {
                bonus += 2.8;
                note(r, "抽牌堆已经空了，【华丽收场】现在可以打：50/60 伤害打全体；"
                        + "它吃易伤/力量/袖剑，清场前先确认手牌里还有没有要留的牌。");
            } else {
                bonus -= 2.0;
                note(r, "抽牌堆还有 " + draw + " 张，【华丽收场】现在打不出去；"
                        + "先用抽牌/弃牌把抽牌堆清空，再把它留到那一回合。");
            }
        } else if ("corpseexplosion".equals(id)) {
            if (poison > 0 && alive.size() >= 2) {
                bonus += 1.8;
                note(r, "目标已有 " + poison + " 层毒，场上有 " + alive.size()
                        + " 个敌人；【尸爆】在目标毒死时会炸全体，多目标战很赚。");
            } else if (poison > 0) {
                bonus += 0.8;
                note(r, "【尸爆】先上毒，目标毒死时炸全体；单目标收益一般，"
                        + "但可以补一层毒并等它自然死亡。");
            } else {
                bonus -= 0.5;
                note(r, "目标现在没毒；【尸爆】要先有毒源才容易触发爆炸，别裸打。");
            }
        } else if ("terror".equals(id)) {
            if (target != null && !target.hasPower("Vulnerable")) {
                bonus += 1.0;
                note(r, "【恐惧】给 99 层易伤，之后所有攻击都吃 1.5 倍；"
                        + "先上易伤再打输出，别把它当伤害牌。");
            } else {
                bonus -= 0.4;
                note(r, "目标已经有易伤，【恐惧】的收益会打折；先打输出或换目标。");
            }
        } else if ("piercingwail".equals(id)) {
            if (alive.size() >= 2 || incoming >= 12) {
                bonus += 0.9;
                note(r, "【尖啸】减全体力量，直接削多段/多目标伤害；"
                        + "对面这回合 " + incoming + " 伤，先用它再决定要不要硬挡。");
            } else {
                bonus += 0.2;
                note(r, "【尖啸】是减力量，不是格挡；单目标小伤害时收益一般，"
                        + "但长战/多段敌人很值。");
            }
        } else if ("malaise".equals(id)) {
            if (target != null && (target.getIntentBaseDmg() > 0
                    || target.hasPower("Strength"))) {
                bonus += 0.8;
                note(r, "【萎靡】是 X 费：每点能量给虚弱并减力量；"
                        + "目标有攻击意图/力量时，先削它的输出，再考虑输出牌。");
            } else {
                bonus += 0.2;
            }
        } else if ("toolsofthetrade".equals(id)) {
            if (discard >= 2 || p.hand == null || p.hand.size() >= 4) {
                bonus += 0.7;
                note(r, "【工具】每回合多抽 1 弃 1，能把弃牌收益/弃牌遗物变成每回合资源；"
                        + "手牌多、弃牌牌多时更值。");
            }
        } else if ("sneakystrike".equals(id)) {
            if (discardedThisTurn() > 0) {
                bonus += 0.6;
                note(r, "本回合已经弃过 " + discardedThisTurn() + " 张牌，【诡计打击】现在打会回 2 能量；"
                        + "先弃牌再打它，不要先交。");
            } else {
                bonus -= 0.5;
                note(r, "本回合还没弃过牌，【诡计打击】现在打不回能量；先弃牌再打它。");
            }
        } else if ("eviscerate".equals(id)) {
            if (discardedThisTurn() > 0) {
                bonus += 0.3;
                note(r, "本回合已弃 " + discardedThisTurn() + " 张牌，【剜割】的费用已经降了；"
                        + "按当前显示费用打，不要按原费估。");
            }
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
        if (countId(p, "corpseexplosion") > 0) {
            out.add("【尸爆】先给目标上毒，毒死时炸全体；多目标战先算毒什么时候结算，"
                    + "不要把它当普通单体伤害。");
        }
        if (countId(p, "grandfinale") > 0) {
            out.add("【华丽收场】只有抽牌堆为空才能打；它打全体 50/60，"
                    + "要先用抽牌/弃牌把抽牌堆清空，再把它留到那一回合。");
        }
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

    private static int discardedThisTurn() {
        return com.megacrit.cardcrawl.actions.GameActionManager.totalDiscardedThisTurn;
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

    private static int strength(AbstractPlayer p) {
        AbstractPower power = p == null ? null : p.getPower("Strength");
        return power == null ? 0 : power.amount;
    }

    private static int accuracyBonus(AbstractPlayer p) {
        int bonus = 0;
        if (p == null || p.masterDeck == null) {
            return 0;
        }
        for (AbstractCard c : p.masterDeck.group) {
            if (c != null && CardDatabase.norm(c.cardID).contains("accuracy")) {
                bonus += c.upgraded ? 6 : 4;
            }
        }
        return bonus;
    }

    private static int shivsGenerated(AbstractCard c, AbstractPlayer p) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("bladedance")) {
            return c.upgraded ? 4 : 3;
        }
        if (id.contains("cloakanddagger")) {
            return c.upgraded ? 2 : 1;
        }
        if (id.contains("stormofsteel")) {
            return p == null || p.hand == null ? 0 : Math.max(0, p.hand.size() - 1);
        }
        if (id.contains("infiniteblades")) {
            return 1;
        }
        return 0;
    }

    private static int skillsInHand(AbstractPlayer p, AbstractCard exclude) {
        if (p == null || p.hand == null) {
            return 0;
        }
        int n = 0;
        for (AbstractCard c : p.hand.group) {
            if (c != null && c != exclude && c.type == AbstractCard.CardType.SKILL) {
                n++;
            }
        }
        return n;
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

    private static int handJunk(AbstractPlayer p) {
        int n = 0;
        if (p == null || p.hand == null) {
            return 0;
        }
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
                n++;
                continue;
            }
            String id = CardDatabase.norm(c.cardID);
            if (id.startsWith("strike") || id.startsWith("defend")) {
                n++;
                continue;
            }
            CardDatabase.Entry e = CardDatabase.get(c);
            if (e != null && e.score < 4.5) {
                n++;
            }
        }
        return n;
    }

    private static int handDiscardPayoff(AbstractPlayer p) {
        int n = 0;
        if (p == null || p.hand == null) {
            return 0;
        }
        for (AbstractCard c : p.hand.group) {
            String id = CardDatabase.norm(c.cardID);
            if (id.contains("tactician") || id.contains("reflex")
                    || id.contains("eviscerate") || id.contains("sneakystrike")) {
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
