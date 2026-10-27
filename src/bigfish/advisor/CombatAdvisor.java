package bigfish.advisor;

import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A transparent heuristic combat advisor. It scores each legal card using
 * incoming damage, lethal range, block efficiency, tempo, and deck synergy.
 * The numbers are estimates; the game state is always the source of truth.
 */
public final class CombatAdvisor {
    public static final class Scored {
        public AbstractCard card;
        public AbstractMonster target;
        public double score;
        public int damage;
        public int block;
        public int energyCost;
        public boolean lethal;
        public boolean aoe;
        public boolean setupVulnerable;
        public boolean enablesLethal;
        public String lethalAttack;
        public int energyGain;
        public int selfHpCost;
        public String reason;
    }

    public static final class Result {
        public Scored best;
        public Scored second;
        public List<Scored> ranking = new ArrayList<Scored>();
        public boolean endTurn;
        public int incoming;
        public int energy;
        public int turn;
        public List<String> enemyLines = new ArrayList<String>();
        public String title;
        public String context;
        public List<String> lines = new ArrayList<String>();
        public String plan;
    }

    private static Field multiAmountField;
    private static boolean multiFieldTried;

    public Result analyze(AbstractPlayer player, MonsterGroup group) {
        Result r = new Result();
        if (player == null || group == null) {
            return null;
        }
        r.turn = Math.max(1, GameActionManager.turn);
        r.energy = EnergyPanel.totalCount;
        r.incoming = incomingDamage(player, group);
        List<AbstractMonster> alive = aliveMonsters(group);
        if (alive.isEmpty()) {
            return null;
        }
        r.enemyLines = EnemyAdvisor.lines(alive);

        for (AbstractCard c : new ArrayList<AbstractCard>(player.hand.group)) {
            if (c == null || c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
                continue;
            }
            if (c.costForTurn > r.energy && !c.freeToPlay()) {
                continue;
            }
            Scored s = scoreCard(player, c, alive, r);
            if (s != null) {
                r.ranking.add(s);
            }
        }
        Collections.sort(r.ranking, new Comparator<Scored>() {
            @Override
            public int compare(Scored a, Scored b) {
                int byScore = Double.compare(b.score, a.score);
                if (byScore != 0) {
                    return byScore;
                }
                return Integer.compare(a.energyCost, b.energyCost);
            }
        });

        if (r.ranking.isEmpty() || r.ranking.get(0).score < 0.8) {
            r.endTurn = true;
        } else {
            r.best = r.ranking.get(0);
            if (r.ranking.size() > 1) {
                r.second = r.ranking.get(1);
            }
        }
        r.title = r.endTurn ? "这回合先收手" : "打【" + CardDatabase.shortName(r.best.card) + "】";
        r.context = String.format("第 %d 回合 · 敌人意图 %d 伤 · 你 %d 格挡 · 能量 %d",
                r.turn, r.incoming, player.currentBlock, r.energy);
        buildLines(player, r);
        return r;
    }

    private Scored scoreCard(AbstractPlayer player, AbstractCard card, List<AbstractMonster> alive, Result r) {
        Scored s = new Scored();
        s.card = card;
        s.aoe = isAoe(card);
        s.energyCost = card.costForTurn < 0 ? r.energy : Math.max(0, card.costForTurn);
        s.energyGain = energyGain(card);
        s.selfHpCost = selfHpCost(card);
        int bestDamage = 0;
        AbstractMonster bestTarget = null;
        boolean lethal = false;
        if (s.aoe) {
            for (AbstractMonster m : alive) {
                int d = estimateDamage(card, m, r.energy);
                bestDamage += d;
                if (d >= m.currentHealth + m.currentBlock) {
                    lethal = true;
                }
            }
            bestTarget = mostThreatening(alive);
        } else {
            double bestValue = -9999.0;
            for (AbstractMonster m : alive) {
                int d = estimateDamage(card, m, r.energy);
                boolean kill = d >= m.currentHealth + m.currentBlock;
                double v = d - Math.max(0, d - (m.currentHealth + m.currentBlock)) * 0.25
                        + (kill ? 40.0 : 0.0) + threat(m) * 0.02;
                if (v > bestValue) {
                    bestValue = v;
                    bestDamage = d;
                    bestTarget = m;
                    lethal = kill;
                }
            }
        }
        s.damage = bestDamage;
        s.target = s.aoe ? null : bestTarget;
        s.lethal = lethal && s.damage > 0;
        if (bestTarget == null) {
            bestTarget = mostThreatening(alive);
        }
        if (!card.canUse(player, card.target == AbstractCard.CardTarget.ALL
                || card.target == AbstractCard.CardTarget.ALL_ENEMY ? null : bestTarget)) {
            return null;
        }

        Set<String> tags = CardDatabase.tags(card);
        int neededBlock = Math.max(0, r.incoming - player.currentBlock);
        s.block = estimateBlock(card);
        double score = 0.0;
        CardDatabase.Entry quality = CardDatabase.get(card);
        if (quality != null) {
            score += (quality.score - 6.0) * 0.16;
        }
        score += RelicAdvisor.combatBonus(player, card);

        if (s.damage > 0) {
            int effective = s.damage;
            if (bestTarget != null && !s.aoe) {
                effective = Math.min(s.damage, bestTarget.currentHealth + bestTarget.currentBlock);
                if (s.damage > effective) {
                    score -= (s.damage - effective) * 0.18;
                }
            }
            score += effective * 0.95;
            if (s.lethal) {
                score += 55.0;
            }
            if (bestTarget != null) {
                score += threat(bestTarget) * 0.012;
            }
        }
        if (s.block > 0) {
            int useful = Math.min(s.block, neededBlock);
            score += useful * 1.65;
            score += Math.max(0, s.block - neededBlock) * 0.10;
            if (neededBlock > 0 && s.block >= neededBlock) {
                score += 1.7;
            }
        }

        int otherAttackDamage = otherAttackDamage(player, card, bestTarget, r.energy);
        boolean targetVulnerable = bestTarget != null && bestTarget.hasPower("Vulnerable");
        boolean targetWeak = bestTarget != null && bestTarget.hasPower("Weak");

        if (tags.contains("vuln") && !targetVulnerable) {
            score += 2.4 + Math.min(12.0, otherAttackDamage * 0.5);
            if (s.damage + otherAttackDamage * 1.5 >= (bestTarget == null ? 0
                    : bestTarget.currentHealth + bestTarget.currentBlock)) {
                s.setupVulnerable = true;
                score += 16.0;
            }
        }
        if (tags.contains("weak") && !targetWeak) {
            score += 1.7 + Math.min(8.0, r.incoming * 0.22);
        }
        if (tags.contains("str")) {
            score += 2.6 + r.turn * 0.65;
            if (CardDatabase.norm(card.cardID).contains("flex")) {
                score -= 1.0;
            }
        }
        if (tags.contains("focus")) {
            score += 3.0 + r.turn * 0.55;
        }
        if (tags.contains("orb")) {
            score += 1.1;
        }
        if (tags.contains("poison")) {
            score += 1.4 + r.turn * 0.30;
        }
        if (tags.contains("draw")) {
            score += Math.min(2.2, 1.5 + Math.max(0, card.draw) * 0.5);
        }
        if (tags.contains("energy")) {
            score += 2.2;
        }
        if (tags.contains("zero")) {
            score += 0.55;
        }
        if (tags.contains("retain")) {
            score += 0.45;
        }
        if (tags.contains("scry")) {
            score += 0.45;
        }
        if (tags.contains("upgrade")) {
            score += 0.55;
        }
        if (tags.contains("heal")) {
            score += 1.8;
        }
        if (tags.contains("xcost")) {
            score += r.energy >= 3 ? 0.8 : -0.7;
        }
        if (tags.contains("exhaust")) {
            score -= 0.35;
            if (player.hasPower("Feel No Pain") || player.hasPower("Dark Embrace")
                    || player.hasPower("Corruption")) {
                score += 1.5;
            }
        }
        if (tags.contains("selfdmg")) {
            score -= player.currentHealth * 100 / Math.max(1, player.maxHealth) < 45 ? 1.8 : 0.25;
        }
        if (tags.contains("status")) {
            score -= 0.8;
        }
        if (tags.contains("curse")) {
            return null;
        }

        if (card.type == AbstractCard.CardType.POWER) {
            score += 3.4 + r.turn * 0.45;
            if (r.incoming > 16 && neededBlock > 0) {
                score -= 2.7;
            }
            if (r.turn >= 8) {
                score -= 1.0;
            }
        }
        if (card.type == AbstractCard.CardType.ATTACK) {
            score += 0.3;
        }
        if (card.type == AbstractCard.CardType.SKILL && s.block == 0 && !tags.contains("draw")
                && !tags.contains("energy")) {
            score -= 0.7;
        }

        if (s.energyGain > 0) {
            if (s.selfHpCost > 0 && player.currentHealth <= s.selfHpCost) {
                score -= 20.0;
            } else {
                Lookahead l = findLethalWithEnergy(player, alive, card, r.energy, r.energy + s.energyGain);
                if (l != null) {
                    s.enablesLethal = true;
                    s.lethalAttack = CardDatabase.shortName(l.attack);
                    score += 42.0 + Math.min(24.0, r.incoming * 0.8);
                } else if (hasAttackAfterEnergy(player, card, r.energy, r.energy + s.energyGain)) {
                    score += 2.4;
                }
            }
        }

        if (neededBlock > 0 && s.block == 0 && !s.lethal && !s.enablesLethal) {
            score -= Math.min(9.0, neededBlock * 0.10);
        }
        if (card.costForTurn >= 2) {
            score -= 0.15 * card.costForTurn;
        }
        if (card.exhaust) {
            score -= 0.2;
        }
        if (card.isEthereal) {
            score -= 0.2;
        }
        if (card.freeToPlayOnce) {
            score += 1.0;
        }

        // Small synergy bonuses: don't overwhelm the baseline, just break ties.
        int deckSynergy = 0;
        String[] keys = {"str", "block", "exhaust", "poison", "shiv", "discard", "orb", "focus", "stance"};
        for (String key : keys) {
            if (tags.contains(key) && countTag(player, key) >= 3) {
                deckSynergy++;
            }
        }
        score += Math.min(1.5, deckSynergy * 0.35);

        s.score = score;
        s.reason = buildReason(player, s, r, targetVulnerable, otherAttackDamage);
        return s;
    }

    private void buildLines(AbstractPlayer player, Result r) {
        r.lines.clear();
        r.lines.add("先看对面，主人：");
        r.lines.addAll(r.enemyLines);
        List<String> relicLines = RelicAdvisor.combatLines(player);
        if (!relicLines.isEmpty()) {
            r.lines.add("遗物这边也在帮您：");
            r.lines.addAll(relicLines);
        }
        if (r.endTurn) {
            r.lines.add(String.format("它这回合总共要打 %d 伤，你现在的格挡是 %d，硬吃大概掉 %d 血；"
                            + "你手里有 %d 张牌、%d 点能量。", r.incoming, player.currentBlock,
                    Math.max(0, r.incoming - player.currentBlock), player.hand.size(), r.energy));
            r.lines.add("我建议这回合先收手，不是让您白让一回合：手里这些牌要么打不出足够价值，要么打完也挡不住。"
                    + "0 费、充能、抽牌我都看过了，真能救命/斩杀会直接推荐。");
            r.lines.add("结束回合别乱打；能量不会留，但乱出牌会把自己的防御/斩杀节奏打乱。");
            r.plan = "如果你还想挣扎一下，先打 0 费抽牌/充能，看看能不能找到防御或斩杀，再决定。";
            return;
        }
        r.lines.add(String.format("这回合对面总共 %d 伤，你有 %d 格挡，硬吃会掉 %d 血；"
                        + "你现在 %d 能量、%d 张手牌。", r.incoming, player.currentBlock,
                Math.max(0, r.incoming - player.currentBlock), r.energy, player.hand.size()));
        r.lines.add(r.best.reason);
        r.lines.add(String.format("我为什么更想打它：它大约能打 %d、挡 %d，只花 %d 能量。"
                        + "这不是只看伤害数字，而是把“能不能挡住这回合、能不能收掉威胁、费用会不会卡住”一起算了。",
                r.best.damage, r.best.block, r.best.energyCost));
        String relicNote = RelicAdvisor.combatNote(player, r.best.card);
        if (relicNote.length() > 0) {
            r.lines.add("遗物配合：" + relicNote + "。");
        }
        if (r.best.enablesLethal) {
            r.lines.add("顺序：先打【" + CardDatabase.shortName(r.best.card) + "】充能，再打【"
                    + r.best.lethalAttack + "】斩杀；不要现在结束回合。");
        } else if (r.best.setupVulnerable) {
            r.lines.add("顺序：先上易伤，再用攻击收尾；易伤只持续到敌人回合结束。");
        } else if (r.best.lethal) {
            r.lines.add("这是斩杀线：先收掉它，能少挨一轮攻击/减少后续变数。");
        } else if (r.best.block > 0 && r.incoming > 0) {
            r.lines.add("这回合优先保血；如果输出足够，也可以先打攻击再补防御。");
        } else if (r.best.card.type == AbstractCard.CardType.POWER) {
            r.lines.add("能力牌越早开越赚；但先确认这回合不会因此被打穿。");
        } else {
            r.lines.add("先处理威胁最高的敌人，别只看单张伤害数字。");
        }
        if (r.second != null) {
            r.lines.add(String.format("另一个选择是【%s】：它大约 %d 伤 / %d 格挡。"
                            + "两张差不多时，我更偏向能保命、能过牌、费用更灵活的那张。",
                    CardDatabase.shortName(r.second.card), r.second.damage, r.second.block));
            r.plan = "备选是【" + CardDatabase.shortName(r.second.card) + "】"
                    + (r.second.target != null ? "，目标 " + r.second.target.name : "")
                    + "；如果首选打不死/挡不住，就换它。";
        } else {
            r.plan = "目标：" + (r.best.target == null ? "全体敌人" : r.best.target.name);
        }
        if (r.best.block == 0 && r.incoming > player.currentBlock) {
            r.lines.add("但要注意：这张不给你格挡，打完预计还要吃 "
                    + Math.max(0, r.incoming - player.currentBlock)
                    + " 伤。如果这点血会要命，就别贪输出，先换防御牌或喝药。");
        } else if (r.best.energyCost > 0 && r.energy - r.best.energyCost == 0) {
            r.lines.add("费用上要留个心眼：打完它能量正好归零，后面只能打 0 费牌。"
                    + "手里还有 0 费过牌的话，先过牌再打它可能更顺。");
        } else {
            r.lines.add("打牌顺序上，先处理最危险的那个敌人；如果这张是能力/易伤，"
                    + "先铺垫再打攻击，通常比反过来更赚。");
        }
        if (player.hasRelic("Runic Pyramid")) {
            r.lines.add("留牌提醒：您有金字塔，别为了把能量打空而把手牌全丢出去。"
                    + "关键防御/成长牌可以留到下一回合，但要留意 10 张手牌上限。");
        } else if (CardDatabase.hasTag(r.best.card, "retain")) {
            r.lines.add("留牌提醒：这张有保留，能留到下一回合配合；别在不需要的回合随便交掉。");
        }
    }

    private String buildReason(AbstractPlayer player, Scored s, Result r, boolean targetVulnerable,
                               int otherAttackDamage) {
        String name = CardDatabase.shortName(s.card);
        String target = s.target == null ? "全体敌人" : s.target.name;
        if (s.enablesLethal) {
            String cost = s.selfHpCost > 0 ? "卖 " + s.selfHpCost + " 血" : "打出";
            return String.format("【%s】%s换 %d 能量，下一张【%s】正好斩杀；不打会白挨 %d 伤。",
                    name, cost, s.energyGain, s.lethalAttack, r.incoming);
        }
        if (s.lethal) {
            if (s.target == null) {
                return String.format("【%s】群体约 %d 伤，能收掉残血目标/完成清场。", name, s.damage);
            }
            return String.format("【%s】约 %d 伤，%s只有 %d 血：正好斩杀，先收掉它。",
                    name, s.damage, target, s.target.currentHealth);
        }
        if (s.setupVulnerable) {
            return String.format("【%s】先上易伤：后续攻击伤害 ×1.5，手里攻击还能多打约 %d。",
                    name, Math.max(0, (int) (otherAttackDamage * 0.5)));
        }
        if (s.damage > 0 && s.block > 0) {
            return String.format("【%s】约 %d 伤 + %d 格挡，攻防一体；对面这回合有 %d 伤。",
                    name, s.damage, s.block, r.incoming);
        }
        if (s.damage > 0) {
            return String.format("【%s】约 %d 伤，花 %d 能量；%s的威胁最高（本回合 %d 伤）。",
                    name, s.damage, s.energyCost, target, threat(s.target));
        }
        if (s.block > 0) {
            int need = Math.max(0, r.incoming - player.currentBlock);
            if (need > 0) {
                return String.format("【%s】+%d 格挡，本回合要挡 %d；能少掉约 %d 血。",
                        name, s.block, need, Math.min(need, s.block));
            }
            return String.format("【%s】+%d 格挡；现在敌人没有攻击意图，格挡收益不高。",
                    name, s.block);
        }
        if (r.best != null && r.best.card == s.card && s.card.type == AbstractCard.CardType.POWER) {
            return String.format("【%s】是成长牌：第 %d 回合开出，后面每回合都能赚；代价是这回合少 %d 能量。",
                    name, r.turn, s.energyCost);
        }
        if (CardDatabase.hasTag(s.card, "draw")) {
            return String.format("【%s】先抽牌找防御/斩杀；低费过牌不会太亏节奏。", name);
        }
        if (CardDatabase.hasTag(s.card, "energy")) {
            return String.format("【%s】给能量，让你这回合能多打一张关键牌。", name);
        }
        if (s.card.type == AbstractCard.CardType.POWER) {
            return String.format("【%s】是能力牌，长战收益高；确认这回合不会被打穿再开。", name);
        }
        return String.format("【%s】是当前手牌里综合收益最高的一张；先稳住节奏，再看下一张。", name);
    }

    public static int incomingDamage(AbstractPlayer player, MonsterGroup group) {
        int total = 0;
        if (group == null) {
            return 0;
        }
        for (AbstractMonster m : group.monsters) {
            if (m == null || m.isDeadOrEscaped() || m.intent == null) {
                continue;
            }
            String intent = m.intent.name();
            if (!intent.contains("ATTACK")) {
                continue;
            }
            int dmg = Math.max(0, m.getIntentDmg());
            int hits = multiAmount(m);
            total += dmg * hits;
        }
        if (player != null && player.hasPower("Intangible") && total > 0) {
            // Intangible reduces each hit to 1; this is deliberately conservative.
            total = Math.min(total, 1);
        }
        return total;
    }

    private static int multiAmount(AbstractMonster m) {
        try {
            if (!multiFieldTried) {
                multiFieldTried = true;
                multiAmountField = AbstractMonster.class.getDeclaredField("intentMultiAmt");
                multiAmountField.setAccessible(true);
            }
            if (multiAmountField != null) {
                int n = multiAmountField.getInt(m);
                return n > 0 ? n : 1;
            }
        } catch (Throwable ignored) {
        }
        return 1;
    }

    private static List<AbstractMonster> aliveMonsters(MonsterGroup group) {
        List<AbstractMonster> out = new ArrayList<AbstractMonster>();
        for (AbstractMonster m : group.monsters) {
            if (m != null && !m.isDeadOrEscaped()) {
                out.add(m);
            }
        }
        return out;
    }

    private static AbstractMonster mostThreatening(List<AbstractMonster> monsters) {
        AbstractMonster best = null;
        double bestScore = -1;
        for (AbstractMonster m : monsters) {
            double v = threat(m) * 10.0 - m.currentHealth * 0.05;
            if (v > bestScore) {
                bestScore = v;
                best = m;
            }
        }
        return best;
    }

    private static int threat(AbstractMonster m) {
        if (m == null || m.intent == null) {
            return 0;
        }
        if (m.intent.name().contains("ATTACK")) {
            return Math.max(0, m.getIntentDmg()) * multiAmount(m);
        }
        if (m.intent.name().contains("BUFF") || m.intent.name().contains("DEFEND")) {
            return 3;
        }
        return 1;
    }

    private static final class Lookahead {
        AbstractCard attack;
        AbstractMonster target;

        Lookahead(AbstractCard attack, AbstractMonster target) {
            this.attack = attack;
            this.target = target;
        }
    }

    /**
     * Bloodletting/Seeing Red/Offering can turn an unaffordable lethal attack
     * into a kill. Find that line before deciding to end the turn.
     */
    private Lookahead findLethalWithEnergy(AbstractPlayer p, List<AbstractMonster> alive,
                                           AbstractCard energyCard, int oldEnergy, int newEnergy) {
        for (AbstractCard c : p.hand.group) {
            if (c == energyCard || c.type != AbstractCard.CardType.ATTACK
                    || (c.damage <= 0 && c.baseDamage <= 0)
                    || (c.costForTurn > newEnergy && !c.freeToPlay())
                    || (c.costForTurn <= oldEnergy && !c.freeToPlay())) {
                continue;
            }
            for (AbstractMonster m : alive) {
                int d = estimateDamage(c, m, newEnergy);
                if (d >= m.currentHealth + m.currentBlock) {
                    return new Lookahead(c, m);
                }
            }
        }
        return null;
    }

    private boolean hasAttackAfterEnergy(AbstractPlayer p, AbstractCard energyCard,
                                         int oldEnergy, int newEnergy) {
        for (AbstractCard c : p.hand.group) {
            if (c != energyCard && c.type == AbstractCard.CardType.ATTACK
                    && (c.damage > 0 || c.baseDamage > 0)
                    && (c.costForTurn <= newEnergy || c.freeToPlay())
                    && (c.costForTurn > oldEnergy || c.freeToPlay())) {
                return true;
            }
        }
        return false;
    }

    private int energyGain(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("bloodletting")) {
            return c.upgraded ? 3 : 2;
        }
        if (id.contains("seeingred")) {
            return c.upgraded ? 3 : 2;
        }
        if (id.contains("offering")) {
            return 2;
        }
        if (id.contains("adrenaline")) {
            return c.upgraded ? 2 : 1;
        }
        if (id.contains("turbo")) {
            return c.upgraded ? 3 : 2;
        }
        return 0;
    }

    private int selfHpCost(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("bloodletting")) {
            return 3;
        }
        if (id.contains("offering")) {
            return 6;
        }
        if (id.contains("hemokinesis")) {
            return 2;
        }
        return 0;
    }

    public static int estimateDamage(AbstractCard card, AbstractMonster target, int energy) {
        if (card == null) {
            return 0;
        }
        AbstractCard copy = card.makeStatEquivalentCopy();
        if (target != null) {
            copy.calculateCardDamage(target);
        } else {
            copy.applyPowers();
        }
        int per = Math.max(0, copy.damage);
        int hits = hitCount(copy, energy);
        return per * hits;
    }

    public static int estimateBlock(AbstractCard card) {
        if (card == null) {
            return 0;
        }
        AbstractCard copy = card.makeStatEquivalentCopy();
        copy.applyPowers();
        return Math.max(0, Math.max(copy.block, card.block));
    }

    public static int hitCount(AbstractCard c, int energy) {
        String id = CardDatabase.norm(c.cardID);
        int hits = 1;
        if (id.contains("twinstrike")) {
            hits = 2;
        } else if (id.contains("swordboomerang")) {
            hits = 3;
        } else if (id.contains("pummel")) {
            hits = c.upgraded ? 5 : 4;
        } else if (id.contains("riddlewithholes")) {
            hits = c.upgraded ? 6 : 5;
        } else if (id.contains("tantrum")) {
            hits = 3;
        } else if (id.contains("daggerspray")) {
            hits = 2;
        } else if (id.contains("ripandtear")) {
            hits = 4;
        } else if (id.contains("reachheaven")) {
            hits = 2;
        } else if (id.contains("whirlwind") || id.contains("skewer") || id.contains("fiendfire")) {
            hits = Math.max(1, energy);
        }
        if (c.cost == -1 || c.costForTurn == -1) {
            hits = Math.max(1, energy);
        }
        return Math.max(1, hits);
    }

    private static int otherAttackDamage(AbstractPlayer p, AbstractCard self, AbstractMonster target, int energy) {
        int total = 0;
        int n = 0;
        for (AbstractCard c : p.hand.group) {
            if (c == self || c.type != AbstractCard.CardType.ATTACK || c.damage <= 0) {
                continue;
            }
            total += estimateDamage(c, target, energy);
            if (++n >= 2) {
                break;
            }
        }
        return total;
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

    public static boolean isAoe(AbstractCard c) {
        return c.target == AbstractCard.CardTarget.ALL_ENEMY
                || c.target == AbstractCard.CardTarget.ALL
                || CardDatabase.hasTag(c, "aoe");
    }
}
