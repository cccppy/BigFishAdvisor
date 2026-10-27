package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One compact line telling the player whether to drink or save each potion. */
public final class PotionAdvisor {
    private PotionAdvisor() {
    }

    /**
     * One short instruction for the collapsed bubble, e.g.
     * "先喝【能量药水】充能". Empty when no potion should be used now.
     */
    public static String quickUse(AbstractPlayer p, boolean combat, int incoming, boolean eliteOrBoss) {
        return quickUse(p, combat, incoming, eliteOrBoss, null);
    }

    public static String quickUse(AbstractPlayer p, boolean combat, int incoming, boolean eliteOrBoss,
                                  AbstractCard recommended) {
        if (p == null || !combat) {
            return "";
        }
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        boolean emergency = incoming > 0 && incoming >= p.currentHealth + p.currentBlock;
        if (emergency) {
            for (AbstractPotion po : potions(p)) {
                if (isDefensive(po)) {
                    return "先喝【" + po.name + "】保命";
                }
            }
        }
        if (EnergyPanel.totalCount <= 0) {
            for (AbstractPotion po : potions(p)) {
                if (is(po, "energypotion")) {
                    AbstractCard after = recommendedAttack(recommended, 2);
                    return after == null ? "先喝【" + po.name + "】充能"
                            : "先喝【" + po.name + "】，再打【" + after.name + "】";
                }
            }
        }
        if (hpPct < 45) {
            for (AbstractPotion po : potions(p)) {
                if (isHealing(po)) {
                    return "先喝【" + po.name + "】回血";
                }
            }
        }
        if (eliteOrBoss) {
            for (AbstractPotion po : potions(p)) {
                if (isTemporaryStrength(po) || isStrengthPotion(po)) {
                    AbstractCard attack = recommendedAttack(recommended, 0);
                    if (attack != null) {
                        return "先喝【" + po.name + "】，再打【" + attack.name + "】";
                    }
                } else if (isDuplication(po)) {
                    AbstractCard attack = recommendedAttack(recommended, 0);
                    if (attack != null) {
                        return "先喝【" + po.name + "】，再打【" + attack.name + "】";
                    }
                } else if (isTemporaryDex(po)) {
                    AbstractCard block = bestBlock(p);
                    if (block != null) {
                        return "先喝【" + po.name + "】，再打【" + block.name + "】";
                    }
                } else if (isOffensive(po)) {
                    return "可以先喝【" + po.name + "】抢节奏";
                }
            }
        }
        return "";
    }

    public static List<String> lines(AbstractPlayer p, boolean combat, int incoming, boolean eliteOrBoss) {
        List<String> out = new ArrayList<String>();
        String summary = line(p, combat, incoming, eliteOrBoss);
        if (summary.length() == 0) {
            return out;
        }
        out.add(summary);
        for (AbstractPotion po : potions(p)) {
            out.add("【" + po.name + "】：" + detail(po, p, combat, incoming, eliteOrBoss));
        }
        return out;
    }

    public static String line(AbstractPlayer p, boolean combat, int incoming, boolean eliteOrBoss) {
        if (p == null || p.potions == null) {
            return "";
        }
        List<AbstractPotion> potions = new ArrayList<AbstractPotion>();
        for (AbstractPotion po : p.potions) {
            if (po != null && po.ID != null && !po.ID.toLowerCase(Locale.ROOT).contains("potion slot")) {
                potions.add(po);
            }
        }
        if (potions.isEmpty()) {
            return "";
        }
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        int energy = EnergyPanel.totalCount;

        if (!combat) {
            for (AbstractPotion po : potions) {
                if (is(po, "fruitjuice")) {
                    return "药水：现在喝【" + po.name + "】永久加最大生命，不用留。";
                }
            }
            if (p.potions.size() < p.potionSlots) {
                for (AbstractPotion po : potions) {
                    if (is(po, "entropicbrew")) {
                        return "药水：现在喝【" + po.name + "】补满空药水格，别等到格子满了再用。";
                    }
                }
            }
            for (AbstractPotion po : potions) {
                if (is(po, "fairy")) {
                    return "药水：【" + po.name + "】留着自动复活，不要手动喝；其它药水留给精英/Boss。";
                }
            }
            if (hpPct < 55) {
                for (AbstractPotion po : potions) {
                    if (isHealing(po)) {
                        return "药水：血量 " + hpPct + "%，可以喝【" + po.name + "】回血；否则留给精英/Boss。";
                    }
                }
            }
            String names = names(potions, 3);
            return "药水：留着" + names + "给精英/Boss；格子满时优先喝普通药水。";
        }

        // Combat: survival first, then lethal/tempo.
        boolean emergency = incoming > 0 && incoming >= p.currentHealth + p.currentBlock;
        if (emergency) {
            for (AbstractPotion po : potions) {
                if (isDefensive(po)) {
                    return "药水：这回合 " + incoming + " 伤会打穿，立刻喝【" + po.name + "】保命。";
                }
            }
        }
        if (energy <= 0) {
            for (AbstractPotion po : potions) {
                if (is(po, "energypotion") || is(po, "liquidmemories")) {
                    return "药水：能量 0，先喝【" + po.name + "】充能，再打手里的攻击。";
                }
            }
        }
        if (hpPct < 45) {
            for (AbstractPotion po : potions) {
                if (isHealing(po)) {
                    return "药水：血量 " + hpPct + "%，喝【" + po.name + "】回血，别省到死。";
                }
            }
        }
        if (eliteOrBoss) {
            for (AbstractPotion po : potions) {
                if (isTemporaryStrength(po) || isStrengthPotion(po) || isDuplication(po)) {
                    AbstractCard attack = bestAttack(p, 0);
                    if (attack != null) {
                        return "药水：先喝【" + po.name + "】，再打多段/关键攻击；顺序反了会少很多伤害。";
                    }
                } else if (isOffensive(po)) {
                    return "药水：这是精英/Boss，可以喝【" + po.name + "】补伤害/抢节奏。";
                }
            }
        }
        for (AbstractPotion po : potions) {
            if (is(po, "fairy") || is(po, "fruitjuice")) {
                return "药水：优先留着【" + po.name + "】；普通战斗别浪费，Boss/危险战再用。";
            }
        }
        String names = names(potions, 3);
        return "药水：" + names + "先留着；等精英/Boss 或血量危险时再用，普通战斗不要空喝。";
    }

    private static List<AbstractPotion> potions(AbstractPlayer p) {
        List<AbstractPotion> out = new ArrayList<AbstractPotion>();
        if (p == null || p.potions == null) {
            return out;
        }
        for (AbstractPotion po : p.potions) {
            if (po != null && po.ID != null && !po.ID.toLowerCase(Locale.ROOT).contains("potion slot")) {
                out.add(po);
            }
        }
        return out;
    }

    private static String detail(AbstractPotion po, AbstractPlayer p, boolean combat,
                                 int incoming, boolean eliteOrBoss) {
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        if (is(po, "fairy")) {
            return "留着自动复活；不要手动喝，Boss 战尤其要保留。";
        }
        if (is(po, "smokebomb")) {
            return "留作紧急逃跑；Boss 战不能逃跑，别把它当 Boss 保命。";
        }
        if (is(po, "fruitjuice")) {
            return "现在/空闲时喝，永久 +最大生命；越早喝越早受益。";
        }
        if (is(po, "entropicbrew")) {
            return p.potions.size() < p.potionSlots
                    ? "有空药水格，现在喝补满；格子满时先别喝。"
                    : "药水格满了，先用掉一瓶再喝。";
        }
        AbstractCard affordable = bestAttack(p, 0);
        if (isTemporaryStrength(po)) {
            if (!combat) {
                return "临时力量只持续到回合结束，战斗里能打多段攻击时再喝；现在先留着。";
            }
            return affordable == null
                    ? "这是临时力量，只持续到回合结束。这回合没有能打出的攻击，先留着，别空喝。"
                    : "临时力量，先喝再打【" + affordable.name + "】；先打攻击再喝就浪费了。";
        }
        if (isStrengthPotion(po)) {
            return affordable == null
                    ? "永久力量，早喝早赚；但现在手里没有能打出的攻击，可以等关键攻击前再喝。"
                    : "永久力量，先喝再打【" + affordable.name + "】更赚；多段攻击前喝最好。";
        }
        if (isDuplication(po)) {
            return affordable == null
                    ? "下一张牌打两次；先想好要复制哪张关键牌，别先出小牌。"
                    : "下一张牌打两次；先喝再打【" + affordable.name + "】，别把复制交给小牌。";
        }
        if (isTemporaryDex(po)) {
            AbstractCard block = bestBlock(p);
            return block == null
                    ? "临时敏捷，只到回合结束；这回合没有能打出的格挡牌，先留着。"
                    : "临时敏捷，先喝再打【" + block.name + "】；先打格挡再喝会少很多格挡。";
        }
        if (isDefensive(po)) {
            if (combat && incoming >= p.currentHealth + p.currentBlock) {
                return "这回合会打穿，立刻喝保命。";
            }
            return eliteOrBoss ? "精英/Boss 战可以喝，挡关键回合。" : "留着挡精英/Boss 的大伤害。";
        }
        if (isOffensive(po)) {
            return eliteOrBoss ? "精英/Boss 现在喝，抢节奏/补斩杀。"
                    : "普通战斗先留；等精英/Boss 或确认能斩杀再用。";
        }
        if (is(po, "energypotion") || is(po, "liquidmemories")) {
            return combat ? "能量不足/关键回合喝，让你多打一张。"
                    : "留到战斗关键回合，别在走路时喝。";
        }
        if (isHealing(po)) {
            return hpPct < 50 ? "血量偏低，可以现在回血。" : "血量还稳，留着战斗危险时喝。";
        }
        return eliteOrBoss ? "精英/Boss 可以用，普通战斗先省。"
                : "普通战斗先留，等关键战再用。";
    }

    private static boolean isTemporaryStrength(AbstractPotion p) {
        return is(p, "steroid");
    }

    private static boolean isStrengthPotion(AbstractPotion p) {
        return is(p, "strengthpotion") || is(p, "strength potion");
    }

    private static boolean isDuplication(AbstractPotion p) {
        return is(p, "duplication");
    }

    private static boolean isTemporaryDex(AbstractPotion p) {
        return is(p, "speed");
    }

    private static AbstractCard bestBlock(AbstractPlayer p) {
        if (p == null || p.hand == null) {
            return null;
        }
        int energy = EnergyPanel.totalCount;
        AbstractCard best = null;
        int bestBlock = -1;
        for (AbstractCard c : p.hand.group) {
            if (c == null || c.type != AbstractCard.CardType.SKILL) {
                continue;
            }
            int block = Math.max(c.block, c.baseBlock);
            if (block <= 0 || (!c.freeToPlay() && c.costForTurn > energy)) {
                continue;
            }
            if (block > bestBlock) {
                bestBlock = block;
                best = c;
            }
        }
        return best;
    }

    /** Only use the card the combat advisor is actually recommending. */
    private static AbstractCard recommendedAttack(AbstractCard recommended, int extraEnergy) {
        if (recommended == null || recommended.type != AbstractCard.CardType.ATTACK) {
            return null;
        }
        int energy = EnergyPanel.totalCount + Math.max(0, extraEnergy);
        if (!recommended.freeToPlay() && recommended.costForTurn > energy) {
            return null;
        }
        return recommended;
    }

    /** Best affordable attack in hand, optionally assuming +extraEnergy. */
    private static AbstractCard bestAttack(AbstractPlayer p, int extraEnergy) {
        if (p == null || p.hand == null) {
            return null;
        }
        int energy = EnergyPanel.totalCount + Math.max(0, extraEnergy);
        AbstractCard best = null;
        double bestDamage = -1;
        for (AbstractCard c : p.hand.group) {
            if (c == null || c.type != AbstractCard.CardType.ATTACK) {
                continue;
            }
            if (!c.freeToPlay() && c.costForTurn > energy) {
                continue;
            }
            double d = c.damage > 0 ? c.damage : c.baseDamage;
            if (d > bestDamage) {
                bestDamage = d;
                best = c;
            }
        }
        return best;
    }

    private static String names(List<AbstractPotion> potions, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < potions.size() && i < max; i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append("【").append(potions.get(i).name).append("】");
        }
        return sb.toString();
    }

    private static boolean is(AbstractPotion p, String idPart) {
        return p.ID != null && p.ID.toLowerCase(Locale.ROOT).replace(" ", "").contains(idPart);
    }

    private static boolean isDefensive(AbstractPotion p) {
        return is(p, "block") || is(p, "ghostinajar") || is(p, "weak") || is(p, "fear")
                || is(p, "speed") || is(p, "dexterity") || is(p, "liquidbronze")
                || is(p, "heartofiron");
    }

    private static boolean isOffensive(AbstractPotion p) {
        return is(p, "fire") || is(p, "explosive") || is(p, "poison") || is(p, "cunning")
                || is(p, "attack") || is(p, "distilled") || is(p, "essenceofsteel")
                || is(p, "steroid");
    }

    private static boolean isHealing(AbstractPotion p) {
        return is(p, "blood") || is(p, "regen") || is(p, "fruitjuice");
    }
}
