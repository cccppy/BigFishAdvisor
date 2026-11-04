package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;
import java.util.List;

/**
 * Ironclad combat identity: strength scales every hit, exhaust turns cards
 * into block/draw, block can become damage, and self-damage needs a payoff.
 */
public final class IroncladAdvisor {
    private IroncladAdvisor() {
    }

    public static CharacterAdvisor.Read readCard(AbstractPlayer p, AbstractCard c,
                                                 List<AbstractMonster> alive, AbstractMonster target,
                                                 boolean aoe, int energy, int incoming) {
        CharacterAdvisor.Read r = new CharacterAdvisor.Read();
        if (p == null || c == null) {
            return r;
        }
        String id = CardDatabase.norm(c.cardID);
        int strength = strength(p);
        int block = p.currentBlock;
        int exhaustDeck = countTag(p, "exhaust");
        int selfDmg = countTag(p, "selfdmg");
        double bonus = 0.0;

        if ("bodyslam".equals(id)) {
            int bodyDamage = Math.max(0, block) + Math.max(0, strength);
            if (target != null && target.hasPower("Vulnerable")) {
                bodyDamage = (int) Math.round(bodyDamage * 1.5);
            }
            r.damage = bodyDamage;
            bonus += 1.2 + Math.min(3.0, block * 0.05);
            note(r, "【全身撞击】的伤害等于你当前格挡 " + block
                    + (strength > 0 ? "，再加上 " + strength + " 点力量" : "")
                    + "；所以先叠格挡再打它，顺序反过来伤害会小很多。");
        } else if ("entrench".equals(id)) {
            if (block >= 8) {
                bonus += 1.4;
                note(r, "你现在 " + block + " 格挡，【巩固】会把它翻倍；"
                        + "先打巩固再打全身撞击，能把防御直接变成伤害。");
            } else {
                bonus -= 0.4;
                note(r, "现在只有 " + block + " 格挡，巩固翻倍收益不高；先叠格挡再用。");
            }
        } else if ("barricade".equals(id)) {
            if (countTag(p, "block") >= 5 || countId(p, "bodyslam") > 0) {
                bonus += 2.4;
                note(r, "你有 " + countTag(p, "block") + " 张格挡牌"
                        + (countId(p, "bodyslam") > 0 ? "/全身撞击" : "")
                        + "；【壁垒】让格挡不消失，是把防御转成后续伤害的核心。");
            } else {
                bonus += 0.5;
                note(r, "【壁垒】让格挡保留；但你现在格挡牌不多，先确认牌组能持续叠甲再拿。");
            }
        } else if ("limitbreak".equals(id)) {
            if (strength >= 3) {
                bonus += 3.0 + Math.min(2.5, strength * 0.18);
                note(r, "你现在 " + strength + " 力量，【极限突破】直接翻倍；"
                        + "先开力量再翻倍，别在 0 力量时打它。");
            } else {
                bonus -= 2.2;
                note(r, "现在只有 " + strength + " 力量，翻倍收益太低；先叠力量再打极限突破。");
            }
        } else if ("inflame".equals(id) || "demondform".equals(id)
                || "demonform".equals(id)) {
            bonus += 1.6 + Math.min(1.8, strength * 0.08);
            note(r, "力量每 +1，每一段攻击都 +1；多段牌/旋风斩/剑柄打击会把力量放大很多。"
                    + (strength > 0 ? "你现在已有 " + strength + " 力量，继续叠很赚。" : "先开第一层力量。"));
        } else if ("flex".equals(id)) {
            int attacks = handType(p, AbstractCard.CardType.ATTACK);
            bonus += attacks > 0 ? 1.0 : -0.5;
            note(r, "【灵活】只给本回合 +" + (c.upgraded ? 4 : 2) + " 力量，回合结束就没了；"
                    + (attacks > 0 ? "先打它，再打手里这 " + attacks + " 张攻击。" : "现在手里没攻击，先别急着打。"));
        } else if ("spotweakness".equals(id)) {
            // Spot Weakness checks the enemy it is played on, not "any enemy
            // in the room"; do not recommend it just because another monster
            // happens to be attacking.
            boolean attacking = target != null && target.getIntentBaseDmg() >= 0;
            bonus += attacking ? 1.4 : -0.7;
            note(r, attacking
                    ? "【观察弱点】只有敌人本回合确实要攻击时才给力量；现在意图是攻击，先打它再加输出。"
                    : "【观察弱点】现在敌人没有攻击意图，打了也不给力量；等它露出攻击意图再打。");
        } else if ("heavyblade".equals(id)) {
            bonus += strength >= 3 ? 1.4 : 0.4;
            note(r, "【重刃】把力量按倍率放大；你现在 " + strength
                    + " 力量，力量越高越值得先打它。");
        } else if ("reaper".equals(id)) {
            if (strength >= 3) {
                bonus += 0.8;
            }
            note(r, "【收割】按伤害回血；力量/易伤越高，回得越多，长战很稳。");
        } else if ("feed".equals(id)) {
            if (c.upgraded) {
                bonus += 0.3;
            }
            note(r, "【进食】斩杀加最大生命；用它收尾，不要拿来打普通伤害。");
        }

        if ("corruption".equals(id)) {
            int skills = countType(p, AbstractCard.CardType.SKILL);
            if (skills >= 6) {
                bonus += 2.6;
                note(r, "你牌组有 " + skills + " 张技能牌；【腐化】让技能 0 费并消耗，"
                        + "技能越多越像发动机，但会把技能烧掉。");
            } else {
                bonus += 0.4;
            }
        } else if ("feelnopain".equals(id) || "darkembrace".equals(id)) {
            if (exhaustDeck >= 3) {
                bonus += 1.3;
                note(r, "你牌组有 " + exhaustDeck + " 张消耗相关牌；"
                        + "【无痛】把消耗转格挡，【黑暗之拥】把消耗转抽牌，先开它们再烧牌。");
            }
        } else if ("secondwind".equals(id)) {
            int nonAttack = handNonAttack(p);
            bonus += Math.min(2.0, nonAttack * 0.25);
            note(r, "手里有 " + nonAttack + " 张非攻击牌（技能/能力/状态/诅咒）；"
                    + "【重振精神】只消耗非攻击牌，每张给格挡，先打要留的攻击，再重振。");
        } else if ("fiendfire".equals(id)) {
            int hand = p.hand == null ? 0 : p.hand.size();
            bonus += Math.min(2.4, hand * 0.18);
            note(r, "【恶魔之火】消耗整手牌，每张打一段；手牌 " + hand
                    + " 张时伤害很高，但会把防御/关键牌一起烧掉，确认这回合能收尾再打。");
        } else if ("juggernaut".equals(id)) {
            if (countTag(p, "block") >= 4) {
                bonus += 1.1;
                note(r, "你有 " + countTag(p, "block") + " 张格挡牌；"
                        + "【势不可挡】每次获得格挡都造成伤害，格挡越多越赚。");
            }
        } else if ("rupture".equals(id)) {
            if (selfDmg >= 2) {
                bonus += 1.5;
                note(r, "你牌组有 " + selfDmg + " 张自伤牌；"
                        + "【撕裂】把自伤转力量，先开它再卖血。");
            } else {
                bonus -= 0.2;
            }
        } else if ("bloodletting".equals(id) || "offering".equals(id)
                || "hemokinesis".equals(id) || "brutality".equals(id)
                || "combust".equals(id)) {
            if (p.currentHealth <= Math.max(12, p.maxHealth / 3)) {
                bonus -= 2.0;
                note(r, "你现在只有 " + p.currentHealth + " 血，自伤牌会把血线打穿；"
                        + "除非这回合能斩杀/有回血，否则先别卖。");
            } else if (countId(p, "rupture") > 0) {
                bonus += 0.8;
                note(r, "你有【撕裂】，自伤会转成力量；但先开撕裂，再卖血。");
            }
        }

        if ("doubletap".equals(id)) {
            AbstractCard best = bestAttackInHand(p, energy);
            if (best != null) {
                bonus += 1.0;
                note(r, "【双发】让下一张攻击打两次；手里【" + CardDatabase.shortName(best)
                        + "】伤害高，先双发再打它。");
            }
        } else if ("berserk".equals(id)) {
            bonus += incoming > 0 ? 0.8 : 0.2;
            note(r, "【狂暴】给能量，但每回合开始获得 1 层易伤；长战赚，但血线/格挡不够时会把风险放大。");
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
        int strength = strength(p);
        int block = p.currentBlock;
        int exhaust = countTag(p, "exhaust");
        int selfDmg = countTag(p, "selfdmg");
        out.add("力量：现在 " + strength + "。每点力量让每一段攻击 +1；"
                + "所以多段牌（旋风斩/剑柄打击/双发）比单段牌吃力量更狠。"
                + "先开力量再打攻击，顺序反了会少一整轮加成。");
        out.add("格挡：现在 " + block + "。"
                + (countId(p, "barricade") > 0
                ? "你有壁垒，格挡会保留；先叠甲，再用全身撞击/巩固把甲变成伤害。"
                : countId(p, "bodyslam") > 0
                ? "你有全身撞击；它按当前格挡算伤害，先叠甲再撞。"
                : "没有壁垒时格挡回合结束会清空，别为了全身撞击硬留甲。"));
        out.add("消耗：牌组 " + exhaust + " 张消耗相关牌。"
                + (countId(p, "corruption") > 0
                ? "腐化让技能 0 费并消耗；先把无痛/黑暗之拥开好，再烧技能。"
                : "没有腐化时，消耗是资源交换；先确认无痛/黑暗之拥能把消耗转成收益。"));
        if (selfDmg > 0) {
            out.add("自伤：牌组 " + selfDmg + " 张自伤牌，当前 " + p.currentHealth + "/"
                    + p.maxHealth + " 血。"
                    + (p.currentHealth <= Math.max(12, p.maxHealth / 3)
                    ? "血线偏低，先别卖血；除非这回合能斩杀。"
                    : "有撕裂时自伤会转力量，但先开撕裂再卖。"));
        }
        out.add("这回合对面 " + incoming + " 伤，你有 " + block + " 格挡；"
                + "如果挡不住，优先补防御/虚弱，别为了力量/消耗把血线打穿。");
        return out;
    }

    public static String plan(AbstractPlayer p, int energy, int incoming) {
        if (p == null) {
            return "";
        }
        if (countId(p, "barricade") > 0 || countId(p, "bodyslam") > 0) {
            return "先叠格挡、巩固翻倍、全身撞击收尾；有壁垒就把甲留到下回合。";
        }
        if (countId(p, "corruption") > 0) {
            return "先开无痛/黑暗之拥，再打腐化；之后技能 0 费，用消耗换格挡/抽牌。";
        }
        if (strength(p) >= 3) {
            return "先开力量/极限突破，再打多段攻击；力量越高，多段越赚。";
        }
        return "先看这回合能不能挡住；能挡再叠力量/开能力，挡不住先补防御。";
    }

    public static double pickBonus(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return 0.0;
        }
        String id = CardDatabase.norm(c.cardID);
        double b = 0.0;
        if (id.contains("inflame") || id.contains("demonform")
                || id.contains("spotweakness") || id.contains("limitbreak")) {
            b += d.tag("str") < 3 ? 0.45 : 0.2;
        }
        if (id.contains("bodyslam") || id.contains("entrench")
                || id.contains("barricade") || id.contains("juggernaut")) {
            b += d.blockCards >= 5 ? 0.4 : 0.1;
        }
        if (id.contains("corruption") || id.contains("feelnopain")
                || id.contains("darkembrace") || id.contains("secondwind")
                || id.contains("fiendfire")) {
            b += d.tag("exhaust") >= 3 ? 0.5 : 0.15;
        }
        if (id.contains("rupture") || id.contains("bloodletting")
                || id.contains("offering") || id.contains("hemokinesis")) {
            b += d.tag("selfdmg") >= 2 ? 0.4 : 0.05;
        }
        if (id.contains("heavyblade")) {
            b += d.tag("str") >= 3 ? 0.35 : 0.0;
        }
        if (id.contains("reaper") || id.contains("feed")) {
            b += 0.15;
        }
        return b;
    }

    public static String pickNote(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return "";
        }
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("bodyslam") || id.contains("entrench")
                || id.contains("barricade") || id.contains("juggernaut")) {
            return "你牌组有 " + d.blockCards + " 张格挡牌；壁垒/巩固/全身撞击"
                    + "把格挡转成伤害，格挡牌越多越成体系。";
        }
        if (id.contains("corruption") || id.contains("feelnopain")
                || id.contains("darkembrace")) {
            return "你牌组有 " + d.tag("exhaust") + " 张消耗相关牌；"
                    + "腐化+无痛+黑暗之拥是消耗流三件套，先开收益牌再烧牌。";
        }
        if (id.contains("rupture") || id.contains("bloodletting")
                || id.contains("offering") || id.contains("hemokinesis")) {
            return "你牌组有 " + d.tag("selfdmg") + " 张自伤牌；"
                    + "撕裂把自伤转力量，但血线低时卖血会要命。";
        }
        if (id.contains("inflame") || id.contains("demonform")
                || id.contains("spotweakness") || id.contains("limitbreak")) {
            return "你牌组有 " + d.tag("str") + " 张力量相关牌；"
                    + "力量按每一段攻击加成，多段牌越多越值。";
        }
        if (id.contains("heavyblade")) {
            return "你牌组力量牌 " + d.tag("str") + " 张；重刃把力量按倍率放大，"
                    + "力量越高越值得拿。";
        }
        return "";
    }

    private static int strength(AbstractPlayer p) {
        AbstractPower power = p == null ? null : p.getPower("Strength");
        return power == null ? 0 : power.amount;
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

    private static int handType(AbstractPlayer p, AbstractCard.CardType type) {
        int n = 0;
        if (p.hand == null) {
            return 0;
        }
        for (AbstractCard c : p.hand.group) {
            if (c.type == type) {
                n++;
            }
        }
        return n;
    }

    private static int handNonAttack(AbstractPlayer p) {
        int n = 0;
        if (p.hand == null) {
            return 0;
        }
        for (AbstractCard c : p.hand.group) {
            if (c.type != AbstractCard.CardType.ATTACK) {
                n++;
            }
        }
        return n;
    }

    private static AbstractCard bestAttackInHand(AbstractPlayer p, int energy) {
        AbstractCard best = null;
        int bestDamage = 0;
        for (AbstractCard c : p.hand.group) {
            if (c.type != AbstractCard.CardType.ATTACK || !CostUtil.canPay(c, energy)) {
                continue;
            }
            int d = 0;
            for (AbstractMonster m : AbstractDungeonHelper.monsters()) {
                d = Math.max(d, CombatAdvisor.estimateDamage(c, m, energy));
            }
            if (d > bestDamage) {
                bestDamage = d;
                best = c;
            }
        }
        return best;
    }

    private static void note(CharacterAdvisor.Read r, String text) {
        if (r.note == null || r.note.length() == 0) {
            r.note = text;
        } else {
            r.note += " " + text;
        }
    }

    /** Tiny indirection so this class stays testable and does not depend on a
     *  particular dungeon singleton in its public API. */
    private static final class AbstractDungeonHelper {
        static List<AbstractMonster> monsters() {
            List<AbstractMonster> out = new ArrayList<AbstractMonster>();
            if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.getMonsters() == null) {
                return out;
            }
            for (AbstractMonster m : com.megacrit.cardcrawl.dungeons.AbstractDungeon.getMonsters().monsters) {
                if (m != null && !m.isDeadOrEscaped()) {
                    out.add(m);
                }
            }
            return out;
        }
    }
}
