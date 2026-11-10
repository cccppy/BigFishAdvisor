package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-turn ("setup") and X-cost valuation.
 *
 * <p>The normal combat scorer is intentionally myopic: it values what a card
 * does this turn.  That misses the most important beginner mistake on Silent
 * and Watcher: spending a safe turn to play Phantasmal Killer, Doppelganger,
 * Echo Form or a long-term power, then cashing it in next turn.  This class
 * adds a separate, explainable bonus for those lines and a real per-energy
 * model for X-cost cards (including Chemical X at 0 energy).</p>
 */
public final class SetupAdvisor {
    public static final class Advice {
        public double bonus;
        public String note = "";
        public boolean setup;
        public boolean safe;
        public int nextTurnDamage;
        public int block;
    }

    private SetupAdvisor() {
    }

    /**
     * X is the number of energy actually spent, plus Chemical X's +2 effect.
     * Chemical X increases the *effect*, not the energy spent, which is why
     * Necronomicon still checks the real energy (handled in RelicAdvisor).
     */
    public static int xValue(AbstractPlayer p, int energy) {
        int x = Math.max(0, energy);
        if (p != null && p.hasRelic("Chemical X")) {
            x += 2;
        }
        return x;
    }

    public static boolean isSafeTurn(AbstractPlayer p, int incoming) {
        if (p == null) {
            return false;
        }
        if (incoming <= 0) {
            return true;
        }
        // A few points of chip damage are not a reason to skip a game-winning
        // setup.  A real attack that can take a large chunk of HP is.
        return incoming <= p.currentBlock + 5
                && p.currentHealth > Math.max(10, incoming);
    }

    private static boolean isDangerous(AbstractPlayer p, int incoming) {
        if (p == null || incoming <= 0) {
            return false;
        }
        return incoming > p.currentBlock + 10
                || incoming >= p.currentHealth;
    }

    public static Advice read(AbstractPlayer p, AbstractCard c, List<AbstractMonster> alive,
                              int energy, int incoming, int turn) {
        Advice a = new Advice();
        if (p == null || c == null) {
            return a;
        }
        String id = CardDatabase.norm(c.cardID);
        a.safe = isSafeTurn(p, incoming);
        boolean danger = isDangerous(p, incoming);
        double base = 0.0;

        if (CostUtil.isX(c)) {
            base += xBonus(p, c, id, alive, energy, incoming, a);
        }

        if ("phantasmalkiller".equals(id)) {
            int future = nextTurnAttackEstimate(p, energy);
            int handAttacks = countAttacksInHand(p);
            a.setup = true;
            a.nextTurnDamage = future;
            base += 3.0 + Math.min(12.0, future * 0.42);
            if (handAttacks >= 2) {
                base += 1.0;
            }
            a.note = "【幻影杀手】是跨回合启动：下一回合所有攻击伤害翻倍，"
                    + "只翻攻击，不翻毒/遗物伤害；和易伤、笔尖相乘。"
                    + "这回合先看对面会不会把您打穿；安全就把攻击留到下周目爆发。";
        } else if ("nightmare".equals(id)) {
            a.setup = true;
            base += 2.6 + bestCopyValue(p) * 0.55;
            a.note = "【噩梦】是下一回合复制 3 张牌：要选手里最值钱的一张（核心能力/高伤攻击/关键防御），"
                    + "不是随便复制一张基础打击。它需要先活过这回合，危险回合别硬开。";
        } else if ("predator".equals(id)) {
            base += 1.6;
            a.note = "【猎杀者】打完还有下回合抽 2；它是“现在打伤害、下回合接上”的桥，"
                    + "安全回合可以早打，别把它只当一张普通攻击。";
        } else if ("burst".equals(id)) {
            int skills = countTypeInHand(p, AbstractCard.CardType.SKILL);
            base += 0.8 + Math.min(3.2, skills * 0.75);
            a.note = "【爆发】让下一张技能打两次：先看手里有没有值得复制的技能"
                    + "（格挡/抽牌/上毒/能力），没有就先别交。";
        } else if ("welllaidplans".equals(id)) {
            base += 2.0;
            a.note = "【计划妥当】每回合保留一张牌：把关键攻击/能力留到爆发回合，"
                    + "或把防御留到敌人攻击回合；它是长期节奏牌，不是即时战力。";
        } else if ("wraithform".equals(id)) {
            base += incoming <= 0 ? -1.0 : Math.min(4.0, 1.5 + incoming * 0.12);
            a.note = "【幽灵形态】给无形，几乎免疫接下来两回合伤害，但会持续掉敏捷；"
                    + "它是保命/拖回合的启动，不是输出牌。安全回合开它价值低，危险回合能救命。";
        } else if ("echoform".equals(id)) {
            a.setup = true;
            base += a.safe ? 4.0 : 1.0;
            a.note = "【回响形态】每回合第一张牌打两次：越早开越赚，但 3 费会占掉这回合；"
                    + "安全回合先开，危险回合先把格挡/血线处理掉。";
        } else if ("biasedcognition".equals(id)) {
            base += 2.6;
            a.note = "【偏执认知】立即给集中，之后每回合掉集中；它是“现在就兑现”的爆发启动，"
                    + "不是慢慢养的能力；有球/冰球时更值，但别在长战里无条件早开。";
        } else if ("electrodynamics".equals(id)) {
            base += alive.size() >= 2 ? 3.2 : 1.2;
            a.note = "【电动力学】让闪电球打全体：多目标时是质变，单体时只是一般能力；"
                    + "先看这回合/下回合的敌人数量再决定。";
        } else if ("loop".equals(id)) {
            base += countOrbs(p) > 0 ? 2.3 : 0.8;
            a.note = "【循环】让最右边球的被动多触发一次：有球才值，"
                    + "尤其冰球/黑暗球；空槽时先充能再开它。";
        } else if ("demonform".equals(id)) {
            a.setup = true;
            base += a.safe ? 3.4 : 0.8;
            a.note = "【恶魔形态】每回合加力量，越早开越赚；3 费会让这回合很紧，"
                    + "安全回合先开，危险回合先保命。";
        } else if ("limitbreak".equals(id)) {
            int str = strength(p);
            base += str >= 2 ? 3.0 + Math.min(4.0, str * 0.25) : -3.0;
            a.note = str >= 2
                    ? "【极限突破】把当前力量翻倍：现在 " + str + " 力量，翻完就是 "
                    + (str * 2) + "；先打力量牌再打它。"
                    : "【极限突破】现在只有 " + str + " 力量，翻倍也几乎没收益；先叠力量。";
        } else if ("barricade".equals(id)) {
            int blockCards = countTag(p, "block");
            base += blockCards >= 3 ? (a.safe ? 2.8 : 0.8) : -1.0;
            a.note = blockCards >= 3
                    ? "【壁垒】让格挡保留：牌组有 " + blockCards + " 张格挡牌，"
                    + "开了以后可以跨回合叠甲；但它 3 费，先确认这回合不会被打穿。"
                    : "【壁垒】需要牌组有足够格挡牌才值；现在格挡牌少，开了也叠不起来。";
        } else if ("corruption".equals(id)) {
            int skills = countType(p, AbstractCard.CardType.SKILL);
            base += skills >= 5 ? 3.0 : -0.8;
            a.note = skills >= 5
                    ? "【腐化】让技能 0 费但消耗：牌组技能多，开了以后每回合能多打很多张；"
                    + "配【黑暗之拥】/【无痛】/【树枝】才是完整引擎。"
                    : "【腐化】需要大量技能牌；现在技能不够，开了反而烧牌。";
        } else if ("feelnopain".equals(id) || "darkembrace".equals(id)) {
            int exhaust = countTag(p, "exhaust");
            base += exhaust >= 3 ? 2.4 : 0.2;
            a.note = exhaust >= 3
                    ? "【" + CardDatabase.shortName(c) + "】是消耗流的引擎：牌组有 "
                    + exhaust + " 张消耗牌，越早开越赚。"
                    : "【" + CardDatabase.shortName(c) + "】需要消耗牌配合；现在消耗牌少，别急着开。";
        } else if ("devaform".equals(id)) {
            a.setup = true;
            base += a.safe ? 3.0 : 0.4;
            a.note = "【天人形态】是长期能量启动：越早开，后面每回合多能量；"
                    + "但这回合会少一个大牌的费用，安全回合才值得。";
        } else if ("battlehymn".equals(id)) {
            base += 2.2;
            a.note = "【战歌】每回合给一张 Smite（0 费攻击）；它是长期资源，"
                    + "越早开越赚，但会占一张手牌/一个能力位。";
        } else if ("fasting".equals(id)) {
            base += 2.0;
            a.note = "【斋戒】给力量和敏捷，但每回合少 1 能量；"
                    + "牌组低费/多段时更值，能量本来就紧时别硬开。";
        } else if ("study".equals(id)) {
            base += 1.7;
            a.note = "【学习】每回合把洞见洗进抽牌堆：它不是即时抽牌，"
                    + "是长期过牌引擎，越早开越赚。";
        } else if ("establishment".equals(id)) {
            int retain = countTag(p, "retain");
            base += retain >= 2 ? 2.3 : 0.5;
            a.note = "【确立基础】让保留牌降费：牌组有 " + retain + " 张保留/保留收益牌，"
                    + "配合【计划妥当】/金字塔才完整。";
        } else if ("alpha".equals(id)) {
            base += a.safe && turn <= 6 ? 2.0 : 0.0;
            a.note = "【阿尔法】要经过贝塔、欧米伽，三回合后才变成每回合 50 伤；"
                    + "只有安全/长战才值得提前投资，危险回合别开。";
        } else if ("blasphemy".equals(id)) {
            boolean canLethal = hasLethalInHand(p, alive, energy);
            base += canLethal ? 4.0 : -5.0;
            a.note = canLethal
                    ? "【亵渎】进神性，但这回合必须斩杀/结束战斗，否则下回合会死；"
                    + "现在手里有斩杀线，可以作为最后一击的启动。"
                    : "【亵渎】进神性后下回合会死；现在没有把握斩杀，别拿命换一回合。";
        } else if ("doubletap".equals(id)) {
            int big = bestAttackInHand(p, energy);
            base += big >= 12 ? 2.2 : 0.2;
            a.note = big >= 12
                    ? "【双发】让下一张攻击打两次：手里有约 " + big + " 伤的大攻击，"
                    + "先双发再打它，别让双发落在小刀上。"
                    : "【双发】需要一张值得复制的大攻击；现在手里攻击偏小，先别急着交。";
        } else if ("conjureblade".equals(id)) {
            a.setup = true;
            base += a.safe ? 2.6 : 0.7;
            a.note = "【聚能成刃】洗一张 X 费【灭除之刃】进抽牌堆：它不是这回合的伤害，"
                    + "而是给之后有能量/化学 X 的回合兑现；安全时先做，危险时先保命。";
        } else if (c.type == AbstractCard.CardType.POWER) {
            base += a.safe ? 1.2 : -0.3;
            if (a.safe) {
                a.note = "能力牌是长期投资：这回合安全，先开它，后面每回合都能赚；"
                        + "如果对面这回合能打穿，就先保命。";
            }
        }

        if (a.setup && danger) {
            base *= 0.45;
        } else if (a.setup && a.safe) {
            base *= 1.15;
        }
        if (turn >= 8 && a.setup) {
            base *= 0.85;
        }
        a.bonus = base;
        return a;
    }

    private static double xBonus(AbstractPlayer p, AbstractCard c, String id,
                                 List<AbstractMonster> alive, int energy, int incoming,
                                 Advice a) {
        int x = xValue(p, energy);
        int per;
        if (x <= 0) {
            a.note = "这张是 X 费：现在 0 能量、也没有化学 X，打出去几乎不产生效果；"
                    + "先攒能量/等化学 X，别为了触发“打一张牌”白交。";
            return -4.0;
        }
        if ("whirlwind".equals(id)) {
            per = c.upgraded ? 8 : 5;
            int total = per * x * Math.max(1, alive.size());
            a.nextTurnDamage = total;
            a.note = "【旋风斩】X=" + x + "：每点能量对全体 " + per + " 伤，约 " + total
                    + " 总伤；能量越多越强，化学 X 会额外 +2 次。";
            return Math.min(24.0, total * 0.36);
        }
        if ("skewer".equals(id)) {
            per = c.upgraded ? 10 : 7;
            int total = per * x;
            a.nextTurnDamage = total;
            a.note = "【串刺】X=" + x + "：约 " + total + " 单体伤害；"
                    + "和力量/易伤/笔尖配合，化学 X 让 X 再 +2。";
            return Math.min(22.0, total * 0.42);
        }
        if ("malaise".equals(id)) {
            a.note = "【不适】X=" + x + "：让目标失去 " + x + " 力量并叠 " + x
                    + " 虚弱；它是防御性 X 费，敌人这回合伤害越高越值。";
            return 2.0 + x * 1.25 + Math.min(3.0, incoming * 0.08);
        }
        if ("reinforcedbody".equals(id)) {
            per = c.upgraded ? 9 : 7;
            a.block = per * x;
            a.note = "【强化身体】X=" + x + "：获得约 " + a.block + " 格挡；"
                    + "先算这回合要挡多少，别把能量全砸进溢出格挡。";
            return Math.min(18.0, Math.min(a.block, Math.max(0, incoming)) * 1.15);
        }
        if ("tempest".equals(id)) {
            a.note = "【暴风雨】X=" + x + "：充能 " + x + " 个闪电球；"
                    + "槽位不够会挤掉最右球，先看槽位/电动力学。";
            return 1.5 + x * 1.1;
        }
        if ("multicast".equals(id)) {
            int orbs = countOrbs(p);
            a.note = "【多重释放】X=" + x + "：激发最右球 " + x + " 次；场上有 "
                    + orbs + " 个球，黑暗/等离子球时爆发很高。";
            return 1.2 + x * 1.4 + Math.min(3.0, orbs * 0.6);
        }
        if ("doppelganger".equals(id)) {
            a.setup = true;
            a.note = "【双重存在】X=" + x + "：下回合多抽 " + x + " 张、多 " + x
                    + " 能量；它是跨回合启动，不是这回合的输出。";
            return 1.5 + x * 1.8;
        }
        if ("collect".equals(id)) {
            a.setup = true;
            a.note = "【收集】X=" + x + "：把奇迹洗进抽牌堆/后续回合上手；"
                    + "奇迹是 0 费给 1 能量，等于把能量延后兑现。";
            return 1.2 + x * 1.5;
        }
        if ("transmutation".equals(id)) {
            a.note = "【转化】X=" + x + "：随机拿 " + x + " 张无色牌，本回合 0 费；"
                    + "上限看随机质量，别把它当稳定斩杀。";
            return 0.5 + x * 0.75;
        }
        return 0.5 + x * 0.6;
    }

    private static int nextTurnAttackEstimate(AbstractPlayer p, int energy) {
        int hand = 0;
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.ATTACK) {
                hand += CombatAdvisor.estimateDamage(c, null, energy);
            }
        }
        int count = 0;
        int total = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c.type == AbstractCard.CardType.ATTACK) {
                count++;
                total += CombatAdvisor.estimateDamage(c, null, energy);
            }
        }
        int deckFuture = 0;
        if (count > 0) {
            int avg = total / count;
            int deckSize = Math.max(1, p.masterDeck.size());
            int expectedHits = Math.max(1, (int) Math.round(3.0 * count / deckSize));
            deckFuture = avg * expectedHits;
        }
        return Math.max(hand, deckFuture);
    }

    private static int countAttacksInHand(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.ATTACK) {
                n++;
            }
        }
        return n;
    }

    private static int countTypeInHand(AbstractPlayer p, AbstractCard.CardType type) {
        int n = 0;
        for (AbstractCard c : p.hand.group) {
            if (c.type == type) {
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

    private static int countTag(AbstractPlayer p, String tag) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (CardDatabase.hasTag(c, tag)) {
                n++;
            }
        }
        return n;
    }

    private static int countOrbs(AbstractPlayer p) {
        int n = 0;
        for (AbstractOrb o : p.orbs) {
            if (o != null) {
                n++;
            }
        }
        return n;
    }

    private static int strength(AbstractPlayer p) {
        return p.getPower("Strength") == null ? 0 : p.getPower("Strength").amount;
    }

    private static int bestCopyValue(AbstractPlayer p) {
        int best = 0;
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.POWER) {
                best = Math.max(best, 12);
            } else if (c.type == AbstractCard.CardType.ATTACK) {
                best = Math.max(best, CombatAdvisor.estimateDamage(c, null, 3));
            }
        }
        return best;
    }

    private static int bestAttackInHand(AbstractPlayer p, int energy) {
        int best = 0;
        for (AbstractCard c : p.hand.group) {
            if (c.type == AbstractCard.CardType.ATTACK) {
                best = Math.max(best, CombatAdvisor.estimateDamage(c, null, energy));
            }
        }
        return best;
    }

    private static boolean hasLethalInHand(AbstractPlayer p, List<AbstractMonster> alive, int energy) {
        for (AbstractCard c : p.hand.group) {
            if (c.type != AbstractCard.CardType.ATTACK) {
                continue;
            }
            for (AbstractMonster m : alive) {
                if (CombatAdvisor.estimateDamage(c, m, energy) >= m.currentHealth + m.currentBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int xBlock(AbstractPlayer p, AbstractCard c, int energy) {
        if (c == null || !CostUtil.isX(c)) {
            return 0;
        }
        String id = CardDatabase.norm(c.cardID);
        if ("reinforcedbody".equals(id)) {
            return (c.upgraded ? 9 : 7) * xValue(p, energy);
        }
        return 0;
    }

    public static List<String> lines(AbstractPlayer p, List<AbstractMonster> alive,
                                     int energy, int incoming) {
        List<String> out = new ArrayList<String>();
        if (p == null) {
            return out;
        }
        boolean hasSetup = false;
        boolean hasX = false;
        for (AbstractCard c : p.hand.group) {
            String id = CardDatabase.norm(c.cardID);
            if (CostUtil.isX(c)) {
                hasX = true;
            }
            if ("phantasmalkiller".equals(id) || "doppelganger".equals(id)
                    || "nightmare".equals(id) || "echoform".equals(id)
                    || "demonform".equals(id) || "barricade".equals(id)
                    || "corruption".equals(id) || "devaform".equals(id)
                    || "welllaidplans".equals(id) || "wraithform".equals(id)
                    || "conjureblade".equals(id)) {
                hasSetup = true;
            }
        }
        if (hasSetup) {
            out.add("启动：这回合如果对面打不穿您，先把“下回合才兑现”的牌开出来；"
                    + "如果这回合有斩杀/会被打穿，就先处理眼前，不要为了启动硬吃。");
        }
        if (hasX) {
            out.add("X 费：实际效果按“当前能量 + 化学X的2”算；0 能量时只有化学 X 才能让 X 费有 2 点效果，"
                    + "别把 0 能量 X 费当免费输出。");
        }
        return out;
    }
}
