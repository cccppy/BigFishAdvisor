package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;
import java.util.List;

/**
 * Watcher stance logic.  The key question is not "is this card tagged stance",
 * but "what does entering/leaving this stance do to this turn's damage and
 * incoming damage?"  Wrath doubles both sides; Calm pays two energy when you
 * leave it; Divinity triples damage for one turn.
 */
public final class StanceAdvisor {
    private static final int NONE = 0;
    private static final int CALM = 1;
    private static final int WRATH = 2;
    private static final int DIVINITY = 3;
    private static final int EXIT = 4;

    private StanceAdvisor() {
    }

    public static CharacterAdvisor.Read readCard(AbstractPlayer p, AbstractCard c,
                                                 List<AbstractMonster> alive, AbstractMonster target,
                                                 boolean aoe, int energy, int incoming) {
        CharacterAdvisor.Read r = new CharacterAdvisor.Read();
        if (p == null || c == null) {
            return r;
        }
        int cur = stance(p);
        AbstractMonster t = target == null ? mostThreatening(alive) : target;
        // Indignation only enters Wrath from Calm; Fear No Evil only enters
        // Calm when the target actually intends to attack; Inner Peace draws
        // instead of re-entering Calm when already calm.
        int move = effectiveMove(cur, c, t);
        int next = nextStance(cur, move);
        if (!aoe && c.type == AbstractCard.CardType.ATTACK && move != NONE && next != cur) {
            int base = CombatAdvisor.estimateDamage(c, t, energy);
            // Every current Watcher attack that changes stance queues its
            // DamageAction before ChangeStanceAction (Eruption, Tantrum,
            // Empty Fist, Fear No Evil).  So this card's own hit uses the
            // OLD stance multiplier; only later attacks get the new one.
            r.damage = base;
            r.note = "这张是先结算伤害、再切到" + stanceName(next) + "；"
                    + "所以本张不吃" + stanceName(next) + "倍率，后面的攻击才吃。";
        } else if (c.type == AbstractCard.CardType.ATTACK && aoe && move != NONE && next != cur) {
            r.note = "这是群体牌，本身不改姿态；伤害按当前姿态倍率结算。";
        }

        double bonus = 0.0;
        int left = Math.max(0, energy - (CostUtil.isX(c) ? energy : CostUtil.current(c)));
        if (move == WRATH) {
            int potential = attackPotential(p, c, alive, left);
            if (cur != WRATH) {
                potential *= 2; // subsequent attacks will be in Wrath
            }
            bonus += 1.25 + Math.min(2.2, potential * 0.10);
            if (cur == WRATH) {
                bonus -= 0.9;
            }
            if (p.hasPower("Rushdown")) {
                bonus += 0.8;
            }
            if (p.hasPower("Mental Fortress")) {
                bonus += 0.4;
            }
            if (incoming * 2 > p.currentBlock + p.currentHealth * 0.30) {
                bonus -= 1.35;
            }
            if (potential == 0 && cur != WRATH) {
                bonus -= 1.1;
            }
        } else if (move == CALM) {
            bonus += 0.85;
            if (cur == WRATH) {
                bonus += 1.25;
            }
            if (p.hasPower("Like Water")) {
                bonus += 0.55;
            }
            if (p.hasPower("Mental Fortress")) {
                bonus += 0.45;
            }
        } else if (move == DIVINITY) {
            int potential = attackPotential(p, c, alive, left);
            if (cur != DIVINITY) {
                potential *= 3; // subsequent attacks will be in Divinity
            }
            bonus += 3.0 + Math.min(3.4, potential * 0.15);
            if (cur == DIVINITY) {
                bonus -= 1.4;
            }
            if (incoming > p.currentHealth * 0.45) {
                bonus -= 0.7;
            }
        } else if (move == EXIT) {
            if (cur == WRATH) {
                bonus += 1.7 + Math.min(2.0, incoming * 0.09);
            } else if (cur == CALM) {
                bonus += 1.0;
            } else {
                bonus += 0.2;
            }
        }

        String id = CardDatabase.norm(c.cardID);
        if (id.contains("indignation") && move == NONE) {
            bonus += 0.35;
            r.note = "你现在不在平静，【义愤填膺】不会进愤怒，只会给目标易伤；"
                    + "要进愤怒得先进平静，或改用【暴怒】/【发泄】这类直接进愤怒的牌。";
        } else if (id.contains("innerpeace") && move == NONE) {
            bonus += 0.5;
            r.note = "你已经在平静，【内心宁静】这时只抽牌、不会再次进平静；"
                    + "把它当抽牌用，别指望它再触发一次退出平静的 2 能量。";
        } else if (id.contains("fearnoevil") && move == NONE) {
            bonus += 0.1;
            r.note = "目标这回合没有攻击意图，【不惧妖邪】打出去不会进平静；"
                    + "它现在只是一张普通攻击，想进平静要换有攻击意图的目标或别的平静牌。";
        } else if (id.contains("meditate") && cur == CALM) {
            bonus -= 1.4;
            r.note = "你已经在平静，【冥想】这时主要是结束回合、保留一张牌；"
                    + "除非你确实要结束回合，不然它会白白浪费这回合的输出机会。";
        } else if ("miracle".equals(id)) {
            int miracle = handCount(p, "miracle");
            if (energy <= 0) {
                bonus = 3.0;
                r.note = "手里这张【奇迹】是 0 费、保留、给 1 能量。现在能量是 0，"
                        + "先打它就能多打一张关键牌；如果这 1 点能量能让你进愤怒/补防御，就很值。";
            } else if (energy < 3) {
                bonus = 0.8;
                r.note = "手里有【奇迹】。现在还有 " + energy + " 能量，先打它会把能量留到后面；"
                        + "如果你正缺 1 点能量进愤怒/神性，就打；否则可以留着下回合再打。";
            } else {
                bonus = -0.15;
                r.note = "现在能量比较充足，【奇迹】可以留到下一回合进愤怒/神性前再打；"
                        + "它是保留牌，不会消失。手里共有 " + miracle + " 张。";
            }
        } else if ("devotion".equals(id)) {
            int mantra = mantra(p);
            bonus += mantra >= 8 ? 1.6 : 0.45;
            r.note = "【虔诚】是每回合开始给 2 点真言，不是打出去立刻加；"
                    + "你现在 " + mantra + "/10，最早也要下回合才可能进神性，"
                    + "所以别把它当这回合的爆发，先开能力再拖回合。";
        } else if (mantraGain(c) > 0) {
            int mantra = mantra(p);
            int gain = mantraGain(c);
            bonus += mantra + gain >= 10 ? 2.2 : 0.2 + gain * 0.10;
            r.note = "这张给 " + gain + " 点真言；你现在 " + mantra + "/10。"
                    + (mantra + gain >= 10
                    ? "打完会进入神性，本回合输出 ×3，优先把攻击打出去。"
                    : "真言不会直接变成伤害，要到 10 点才进神性；先算离 10 还差多少。");
        } else if ("blasphemy".equals(id)) {
            bonus += cur == DIVINITY ? -4.0 : 2.0;
            r.note = cur == DIVINITY
                    ? "你已经在神性，【渎神】不会再给你新的神性回合，只会让你下回合死亡；"
                    + "除非你确定这回合能结束战斗，否则绝对不要打。"
                    : "【渎神】直接进神性，但下回合开始会死亡；"
                    + "只有这回合能斩杀、或有免死/足够格挡时才拿它收尾，不能当普通姿态牌乱开。";
        } else if ("rushdown".equals(id)) {
            int entries = countId(p, "eruption") + countId(p, "crescendo")
                    + countId(p, "indignation") + countId(p, "tantrum");
            if (entries >= 2) {
                bonus += 0.9;
                r.note = "你牌组有 " + entries + " 张进愤怒牌；【猛虎下山】每次进愤怒抽 2，"
                        + "是姿态循环的发动机，越早开越赚。";
            } else {
                bonus -= 0.3;
                r.note = "你目前只有 " + entries + " 张进愤怒牌；【猛虎下山】需要频繁进愤怒才值，"
                        + "配合不够时先别急着裸拿。";
            }
        } else if ("mentalfortress".equals(id)) {
            int moves = stanceMoves(p);
            if (moves >= 3) {
                bonus += 0.8;
                r.note = "你牌组有 " + moves + " 张姿态切换牌；每次切换【心灵堡垒】都给格挡，"
                        + "是姿态循环的防御核心。";
            }
        } else if ("likewater".equals(id)) {
            if (countId(p, "vigilance") + countId(p, "tranquility")
                    + countId(p, "innerpeace") + countId(p, "fearnoevil")
                    + countId(p, "meditate") >= 2) {
                bonus += 0.55;
                r.note = "你有稳定的进平静牌；【如水】在平静回合结束给格挡，适合先平静再爆发的节奏。";
            }
        } else if ("talktothehand".equals(id)) {
            if (countType(p, AbstractCard.CardType.ATTACK) >= 8) {
                bonus += 0.45;
                r.note = "你攻击牌多；【他山之石】每次攻击都给格挡，能把多段输出转成防御。";
            }
        } else if ("collect".equals(id)) {
            if (!p.hasRelic("Pure Water")) {
                bonus += 0.35;
                r.note = "你没有【纯水】，【收集】是额外的奇迹来源；但它把 2 张奇迹洗进抽牌堆，"
                        + "不是直接上手，打完还要过牌才看得到。";
            } else {
                r.note = "【收集】把 2 张奇迹洗进抽牌堆，不是直接上手；纯水已经给了一张起手奇迹，"
                        + "这张更适合长战/过牌多的牌组。";
            }
        } else if ("halt".equals(id) && cur == WRATH) {
            bonus += 0.35;
            r.note = "现在在愤怒，【停顿】的格挡会翻倍；但打完别忘了愤怒也会让敌人打你更疼。";
        } else if ("flurryofblows".equals(id) && stanceMoves(p) >= 3) {
            bonus += 0.3;
            r.note = "你姿态切换多，【连击】会反复回手，是姿态循环的稳定输出。";
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
        int cur = stance(p);
        int mantra = mantra(p);
        int miracle = handCount(p, "miracle");
        out.add("观者姿态：现在是" + stanceName(cur) + "。"
                + stanceExplain(cur));
        out.add("真言：" + mantra + "/10。"
                + (mantra >= 8 ? "已经接近神性，下一张真言牌很可能直接触发；先把攻击/能量准备好。"
                : "真言到 10 才进神性；它本身不增伤，别为了叠真言把关键攻击拖没。"));
        if (miracle > 0) {
            out.add("奇迹：手里有 " + miracle + " 张。它是 0 费、保留、给 1 能量；"
                    + "现在 " + energy + " 能量，" + (energy <= 0
                    ? "先打一张再决定下一张，别让能量空着。"
                    : "如果打完能多打一张进愤怒/防御牌，就打；否则留着下回合进愤怒/神性前再打。"));
        } else {
            String source = miracleSource(p);
            out.add("奇迹：现在手里没有。" + source);
        }
        int calm = countId(p, "vigilance") + countId(p, "tranquility")
                + countId(p, "innerpeace") + countId(p, "fearnoevil")
                + countId(p, "meditate");
        int wrath = countId(p, "eruption") + countId(p, "crescendo")
                + countId(p, "indignation") + countId(p, "tantrum");
        int exit = countId(p, "emptyfist") + countId(p, "emptybody")
                + countId(p, "emptymind");
        out.add("这回合手里的姿态入口：进平静 " + calm + " 张、进愤怒 " + wrath
                + " 张、退出 " + exit + " 张。");
        if (cur == CALM) {
            out.add("为什么现在适合先平静：平静本身不增伤，但退出平静会给 2 能量；"
                    + "先用平静牌/格挡稳住，再退出拿能量进愤怒，才能把 2 能量变成两张攻击。"
                    + "如果你这回合直接进愤怒，等于放弃这笔能量，还要白吃双倍反击。");
        } else if (cur == WRATH) {
            out.add("为什么现在要小心愤怒：你造成和受到的伤害都 ×2。"
                    + "如果这回合不能斩杀/挡住，先把愤怒退掉（Empty 系列）再打；"
                    + "否则对面 " + incoming + " 伤会变成约 " + (incoming * 2)
                    + "，很容易把血线打穿。");
        } else if (cur == DIVINITY) {
            out.add("为什么现在要全力输出：神性只持续到回合结束，攻击 ×3；"
                    + "不要再进平静/愤怒浪费这回合，先把高伤害攻击全部打出去。");
        } else {
            out.add("为什么现在先找入口：中立姿态没有倍率。"
                    + "如果手里有平静，先平静攒 2 能量；有愤怒且这回合能输出，再进愤怒。"
                    + "没有姿态牌就别硬等，按普通攻防打。");
        }
        int potential = attackPotential(p, null, alive, energy);
        if (potential > 0) {
            if (cur == WRATH) {
                out.add("按你手里的攻击、以当前愤怒姿态粗算，这回合还能打出约 " + potential
                        + " 伤；这个数已经含 ×2，不要再乘第二次。"
                        + "但敌人打你也会翻倍，先看能不能在这回合收掉。");
            } else {
                out.add("按你手里的攻击、以当前姿态粗算，这回合还能打出约 " + potential + " 伤；"
                        + "如果进愤怒，这部分约变成 " + (potential * 2) + "，"
                        + "但敌人打你也会翻倍。先看能不能在这回合收掉，再决定要不要贪。");
            }
        }
        return out;
    }

    public static String plan(AbstractPlayer p, int energy, int incoming) {
        if (p == null) {
            return "";
        }
        int cur = stance(p);
        if (cur == CALM) {
            return "先平静稳住、退出平静拿 2 能量、进愤怒输出；奇迹留到进愤怒/神性前再打。";
        }
        if (cur == WRATH) {
            return "先算这回合能否斩杀/挡住；不能就先退出愤怒，别硬吃双倍伤害。";
        }
        if (cur == DIVINITY) {
            return "这回合全力输出，不要再进平静/愤怒；优先高伤害攻击。";
        }
        return "先找平静/愤怒入口；没有姿态牌就按普通攻防打，别为了进姿态空过。";
    }

    public static double pickBonus(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return 0.0;
        }
        String id = CardDatabase.norm(c.cardID);
        double b = 0.0;
        int mantra = d.tag("mantra");
        int stance = d.tag("stance");
        int retain = d.tag("retain");
        if (id.contains("worship") || id.contains("pray") || id.contains("prostrate")
                || id.contains("devotion")) {
            b += mantra < 3 ? 0.45 : 0.25;
            if (mantra >= 2) {
                b += 0.2;
            }
        }
        if (id.contains("rushdown")) {
            b += stance >= 2 ? 0.8 : -0.25;
        }
        if (id.contains("mentalfortress")) {
            b += stance >= 3 ? 0.7 : 0.15;
        }
        if (id.contains("talktothehand")) {
            b += d.attacks >= 6 ? 0.35 : 0.0;
        }
        if (id.contains("blasphemy")) {
            b += mantra >= 5 ? 0.45 : -0.2;
        }
        if (id.contains("collect")) {
            b += p.hasRelic("Pure Water") ? 0.1 : 0.3;
        }
        if (id.contains("establishment")) {
            b += retain >= 3 ? 0.4 : 0.0;
        }
        if (id.contains("likeWater") || id.contains("likewater")) {
            b += stance >= 2 ? 0.3 : 0.0;
        }
        if (id.contains("devaform") || id.contains("deva")) {
            b += d.size >= 18 ? 0.25 : 0.0;
        }
        return b;
    }

    public static String pickNote(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return "";
        }
        String id = CardDatabase.norm(c.cardID);
        int mantra = d.tag("mantra");
        int stance = d.tag("stance");
        if (id.contains("worship") || id.contains("pray") || id.contains("prostrate")
                || id.contains("devotion")) {
            return "你现在有 " + mantra + " 张真言牌；真言要到 10 才进神性，"
                    + "再拿这张会让神性更稳定，但别为了叠真言放弃眼前的输出/防御。";
        }
        if (id.contains("rushdown")) {
            return "你有 " + stance + " 张姿态牌；猛虎下山每次进愤怒抽 2，"
                    + "是姿态循环的抽牌发动机，进愤怒越频繁越值。";
        }
        if (id.contains("mentalfortress")) {
            return "你有 " + stance + " 张姿态牌；心灵堡垒每次切换都给格挡，"
                    + "配合平静/愤怒来回切，能把输出回合也变成防御回合。";
        }
        if (id.contains("talktothehand")) {
            return "你牌组有 " + d.attacks + " 张攻击牌；他山之石让每次攻击都留格挡，"
                    + "多段/低费攻击越多越强。";
        }
        if (id.contains("blasphemy")) {
            return "你有 " + mantra + " 张真言牌；渎神直接进神性，但下回合开始死亡，"
                    + "只适合能在这回合斩杀、或有免死/格挡收尾的牌组。";
        }
        if (id.contains("collect")) {
            return p.hasRelic("Pure Water")
                    ? "纯水已经给起手奇迹；收集把 2 张奇迹洗进抽牌堆，不是直接上手，适合长战/过牌。"
                    : "你没有纯水，收集是额外奇迹来源；但它是洗进抽牌堆，打完还要过牌才看得到。";
        }
        if (id.contains("establishment")) {
            return "你有 " + d.tag("retain") + " 张保留牌；建立让保留牌每回合降费，"
                    + "保留越多越赚，但要有足够的保留牌才拿。";
        }
        if (id.contains("likewater")) {
            return "你有 " + stance + " 张姿态牌；如水在平静回合结束给格挡，"
                    + "适合先平静再爆发的循环。";
        }
        return "";
    }

    private static int stance(AbstractPlayer p) {
        if (p == null || p.stance == null || p.stance.ID == null) {
            return NONE;
        }
        String id = p.stance.ID;
        if ("Calm".equals(id)) {
            return CALM;
        }
        if ("Wrath".equals(id)) {
            return WRATH;
        }
        if ("Divinity".equals(id)) {
            return DIVINITY;
        }
        return NONE;
    }

    private static int effectiveMove(int cur, AbstractCard c, AbstractMonster target) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("indignation") && cur != CALM) {
            return NONE;
        }
        if (id.contains("fearnoevil")
                && (target == null || target.getIntentBaseDmg() < 0)) {
            return NONE;
        }
        if (id.contains("innerpeace") && cur == CALM) {
            return NONE;
        }
        return move(c);
    }

    private static int move(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("blasphemy")) {
            return DIVINITY;
        }
        if (id.contains("eruption") || id.contains("crescendo")
                || id.contains("indignation") || id.contains("tantrum")) {
            return WRATH;
        }
        if (id.contains("vigilance") || id.contains("tranquility")
                || id.contains("innerpeace") || id.contains("fearnoevil")
                || id.contains("meditate")) {
            return CALM;
        }
        if (id.contains("emptyfist") || id.contains("emptybody")
                || id.contains("emptymind")) {
            return EXIT;
        }
        return NONE;
    }

    private static int nextStance(int cur, int move) {
        if (move == CALM) {
            return CALM;
        }
        if (move == WRATH) {
            return WRATH;
        }
        if (move == DIVINITY) {
            return DIVINITY;
        }
        if (move == EXIT) {
            return NONE;
        }
        return cur;
    }

    private static int factor(int stance) {
        if (stance == WRATH) {
            return 2;
        }
        if (stance == DIVINITY) {
            return 3;
        }
        return 1;
    }

    private static String stanceName(int stance) {
        if (stance == CALM) {
            return "平静";
        }
        if (stance == WRATH) {
            return "愤怒";
        }
        if (stance == DIVINITY) {
            return "神性";
        }
        return "中立";
    }

    private static String stanceExplain(int stance) {
        if (stance == CALM) {
            return "平静不增伤，但退出平静会给 2 能量，是观者爆发的启动资金。";
        }
        if (stance == WRATH) {
            return "愤怒造成伤害 ×2，但受到伤害也 ×2；这是高风险高回报的姿态。";
        }
        if (stance == DIVINITY) {
            return "神性造成伤害 ×3，只持续到回合结束；这是斩杀窗口。";
        }
        return "中立没有倍率；先找平静/愤怒入口。";
    }

    private static int mantra(AbstractPlayer p) {
        AbstractPower power = p == null ? null : p.getPower("Mantra");
        return power == null ? 0 : Math.max(0, power.amount);
    }

    private static int mantraGain(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("worship")) {
            return 5;
        }
        if (id.contains("pray")) {
            return 3;
        }
        if (id.contains("prostrate")) {
            return 2;
        }
        if (id.contains("devotion")) {
            return 2;
        }
        return 0;
    }

    private static int stanceMoves(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (move(c) != NONE) {
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

    private static int handCount(AbstractPlayer p, String part) {
        int n = 0;
        for (AbstractCard c : p.hand.group) {
            if (CardDatabase.norm(c.cardID).contains(part)) {
                n++;
            }
        }
        return n;
    }

    private static String miracleSource(AbstractPlayer p) {
        if (p.hasRelic("Pure Water")) {
            return "你有纯水：每场战斗开局给 1 张奇迹；如果已经打掉，它不会再来。";
        }
        if (countId(p, "collect") > 0) {
            return "你没有纯水，但有【收集】：它把 2 张奇迹洗进抽牌堆，"
                    + "不是直接上手，必须再抽到；没抽到就不会出现。";
        }
        if (countId(p, "deceivereality") > 0) {
            return "你没有纯水，但有【欺骗现实】：它直接生成 1 张奇迹到手牌，"
                    + "不是洗进抽牌堆；打完这张牌就能看到那张奇迹。";
        }
        return "这套没有纯水、收集或欺骗现实，奇迹不会凭空出现；"
                + "想稳定见到它，需要先拿这些来源。";
    }

    private static int attackPotential(AbstractPlayer p, AbstractCard self,
                                       List<AbstractMonster> alive, int energy) {
        if (p == null || p.hand == null) {
            return 0;
        }
        // estimateDamage already applies the current stance's atDamageGive.
        // Do not multiply by the stance factor again, or Wrath would show 4x.
        int total = 0;
        int left = Math.max(0, energy);
        for (AbstractCard c : p.hand.group) {
            if (c == self || c.type != AbstractCard.CardType.ATTACK
                    || (c.damage <= 0 && c.baseDamage <= 0)) {
                continue;
            }
            int cost = CostUtil.isX(c) ? left : CostUtil.current(c);
            if (cost > left) {
                continue;
            }
            int d = 0;
            if (CombatAdvisor.isAoe(c)) {
                for (AbstractMonster m : alive) {
                    d += CombatAdvisor.estimateDamage(c, m, left);
                }
            } else {
                AbstractMonster t = mostThreatening(alive);
                d = CombatAdvisor.estimateDamage(c, t, left);
            }
            total += d;
            left = Math.max(0, left - Math.max(0, cost));
        }
        return total;
    }

    private static AbstractMonster mostThreatening(List<AbstractMonster> alive) {
        AbstractMonster best = null;
        int bestThreat = -1;
        for (AbstractMonster m : alive) {
            if (m == null || m.isDeadOrEscaped()) {
                continue;
            }
            int t = 0;
            if (m.intent != null && m.intent.name().contains("ATTACK")) {
                t = Math.max(0, m.getIntentDmg());
            }
            if (t > bestThreat) {
                bestThreat = t;
                best = m;
            }
        }
        return best;
    }
}
