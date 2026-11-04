package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;
import java.util.List;

/**
 * Defect orb logic.  The important part is not the "orb" tag but the actual
 * slots: a channel when every slot is full pushes the rightmost orb out, so
 * the advisor has to look at the queue before recommending another channel.
 */
public final class OrbAdvisor {
    private OrbAdvisor() {
    }

    public static CharacterAdvisor.Read readCard(AbstractPlayer p, AbstractCard c,
                                                 List<AbstractMonster> alive, AbstractMonster target,
                                                 boolean aoe, int energy, int incoming) {
        CharacterAdvisor.Read r = new CharacterAdvisor.Read();
        if (p == null || c == null) {
            return r;
        }
        String id = CardDatabase.norm(c.cardID);
        int slots = Math.max(0, p.maxOrbs);
        int used = p.orbs == null ? 0 : p.orbs.size();
        int free = Math.max(0, slots - used);
        int focus = focus(p);
        int add = orbsAdded(c, energy, alive);
        int evoke = evokes(c, energy, used);
        double bonus = 0.0;

        if (add > 0) {
            if (free <= 0) {
                bonus -= 2.2;
                note(r, "球槽已经满了（" + used + "/" + slots + "）。再充能会挤掉最右边的【"
                        + rightName(p) + "】；如果那是黑暗/等离子，先激发或扩容，别直接把它挤掉。");
                if (isValuable(rightOrb(p))) {
                    bonus -= 1.0;
                }
            } else {
                bonus += 1.0 + Math.min(2.0, add * 0.35);
                note(r, "这次会生成 " + add + " 个球，槽位还有 " + free + " 个；"
                        + "先充能，再用激发把球的价值打出来。");
            }
            if (focus > 0) {
                bonus += 0.35 + Math.min(1.0, focus * 0.12);
            }
        }

        if (evoke > 0) {
            if (used <= 0) {
                bonus -= 2.0;
                note(r, "现在没有球，激发类牌打出去是空的；先充能再激发。");
            } else {
                int value = rightValue(p);
                if ("dualcast".equals(id)) {
                    // Dualcast evokes the rightmost orb twice even when it is
                    // the only orb in the slots.
                    value *= 2;
                }
                bonus += 1.0 + Math.min(2.8, value * 0.12);
                if ("dualcast".equals(id)) {
                    note(r, "【双重施法】会把最右侧的【" + rightName(p) + "】激发 2 次；"
                            + "哪怕只有 1 个球也会结算两次，先确认它的激发伤害/格挡值得交。");
                } else {
                    note(r, "这张会激发" + (evoke >= used ? "全部 " + used : " " + evoke)
                            + " 个球；最右边是【" + rightName(p) + "】，"
                            + orbValueText(p) + "。先看它值不值得现在交。");
                }
                if ("dark".equals(rightId(p)) && focus >= 3) {
                    bonus += 0.6;
                }
            }
        }

        if ("capacitor".equals(id)) {
            bonus += slots <= 3 ? 1.5 : 0.5;
            note(r, "扩容增加球槽；你现在 " + slots + " 槽，"
                    + (slots <= 3 ? "槽位偏少，扩容能让球体系转起来。" : "槽位已经够用，先补集中/输出更赚。"));
        } else if ("consume".equals(id)) {
            if (slots <= 3) {
                bonus -= 1.2;
                note(r, "吞噬会消耗 1 个球槽换集中；你现在只有 " + slots
                        + " 槽，槽位太紧，先扩容再考虑它。");
            } else {
                bonus += 1.1;
                note(r, "吞噬用 1 个球槽换集中；你有 " + slots + " 槽，"
                        + "集中会同时放大冰球格挡和闪电伤害，槽位够时很值。");
            }
        } else if ("defragment".equals(id)) {
            bonus += 2.4 + Math.min(1.5, focus * 0.1);
        } else if ("biasedcognition".equals(id)) {
            bonus += 2.8;
            if (focus >= 2) {
                bonus += 0.5;
            }
            note(r, "偏执认知给大量集中，但每回合掉 1 点；"
                    + "它是短战爆发牌，不是长战成长牌，别在需要拖的 Boss 战里裸开。");
        } else if ("hyperbeam".equals(id)) {
            if (focus > 0 || used >= 2) {
                bonus -= 2.2;
                note(r, "光束会失去 3 点集中；你现在" + (focus > 0 ? "有 " + focus + " 集中" : "有球")
                        + "，这一下会把冰球/闪电的加成一并打掉，除非这回合能清场，否则别打。");
            } else {
                bonus += 0.4;
            }
        } else if ("reprogram".equals(id)) {
            if (focus > 0 || used >= 2) {
                bonus -= 1.8;
                note(r, "重构会失去集中；你有 " + focus + " 集中/" + used
                        + " 个球，这和球体系是反方向，别在球流里拿它。");
            }
        } else if ("fission".equals(id)) {
            if (used >= 3) {
                bonus += 1.8;
                note(r, "裂变移除所有球，按数量给能量/抽牌；你现在 " + used
                        + " 个球，是爆发/换资源的好时机。");
            } else {
                bonus -= 1.3;
                note(r, "裂变要球多才赚；现在只有 " + used + " 个球，先充能再裂变。");
            }
        } else if ("barrage".equals(id)) {
            bonus += Math.min(1.4, used * 0.25);
            if (!aoe) {
                AbstractMonster t = target;
                if (t == null && alive != null && !alive.isEmpty()) {
                    t = alive.get(0);
                }
                int per = CombatAdvisor.estimateDamage(c, t, energy);
                r.damage = Math.max(0, per) * used;
            }
            note(r, "弹幕每有一个球就打一段；你现在 " + used + " 个球，"
                    + "先充能再打它，伤害会按球数翻。");
        } else if ("blizzard".equals(id)) {
            int frost = countOrb(p, "Frost");
            bonus += Math.min(1.5, frost * 0.25);
            note(r, "暴风雪按本场累计充能的冰球数增伤；场上现在还有 " + frost
                    + " 个冰球，实际通常比这个数更高，冰球越多它越强。");
        } else if ("thunderstrike".equals(id)) {
            int lightning = countOrb(p, "Lightning");
            bonus += Math.min(1.2, lightning * 0.2);
            note(r, "雷击按本场累计充能的闪电球数增伤；场上现在还有 " + lightning
                    + " 个闪电球，实际通常比这个数更高，闪电越多它越强。");
        } else if ("loop".equals(id)) {
            if (used > 0) {
                bonus += 0.9;
                note(r, "循环每回合额外触发最右侧的球；你现在最右边是【"
                        + rightName(p) + "】，有了它每回合都能多赚一次。");
            } else {
                bonus -= 0.3;
            }
        } else if ("electrodynamics".equals(id)) {
            bonus += 1.3;
            note(r, "电动力学让闪电球打全体，并充能 2 个闪电；群战/多目标时这是球流的核心。");
        } else if ("staticdischarge".equals(id)) {
            if (incoming > 0) {
                bonus += 0.9;
                note(r, "静电释放受伤时充能闪电；这回合对面要打 " + incoming
                        + "，能白赚球，但也别为了它故意挨打。");
            }
        } else if ("storm".equals(id)) {
            if (countType(p, AbstractCard.CardType.POWER) >= 2) {
                bonus += 1.0;
                note(r, "雷暴每打一张能力牌充能闪电；你牌组有 "
                        + countType(p, AbstractCard.CardType.POWER) + " 张能力，适合能力/球混合。");
            }
        } else if ("creativeai".equals(id) || "echoform".equals(id)) {
            bonus += 1.0;
            note(r, "这张是长战资源引擎；越早开越赚，但要先确认这回合不会被打穿。");
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
        int slots = Math.max(0, p.maxOrbs);
        int used = p.orbs == null ? 0 : p.orbs.size();
        int focus = focus(p);
        int frost = countOrb(p, "Frost");
        int lightning = countOrb(p, "Lightning");
        int dark = countOrb(p, "Dark");
        int plasma = countOrb(p, "Plasma");
        out.add("充能球：槽位 " + used + "/" + slots + "，集中 " + focus + "。"
                + (used >= slots ? "槽位已满，下一次充能会挤掉最右边的球；先激发/扩容。"
                : "还有 " + (slots - used) + " 个空槽，可以继续充能。"));
        if (used > 0) {
            out.add("从右边数：" + rightName(p) + "。" + orbValueText(p)
                    + " 冰球 " + frost + " / 闪电 " + lightning + " / 黑暗 " + dark
                    + " / 等离子 " + plasma + "。");
        } else {
            out.add("现在场上没有球。球体系先充能，再靠激发/集中把伤害和格挡滚起来；"
                    + "裸打激发牌是空的，别把能量浪费掉。");
        }
        out.add("集中的作用：每点集中让冰球被动/激发格挡 +1、闪电伤害 +1、黑暗爆发 +1；"
                + "所以【碎片整理】/【偏执认知】这类牌不是直接伤害，"
                + "而是把整条球体系一起放大。");
        out.add("激发的顺序：先看最右边的球。黑暗球适合攒大再激发，等离子球激发给能量，"
                + "冰球激发给格挡，闪电球激发打伤害；球槽满时，先激发再充能，别让新球把关键球挤掉。");
        if (focus > 0) {
            out.add("你现在有 " + focus + " 集中；冰球格挡和闪电伤害都按这个数加成。"
                    + "如果要打光束/重构，先想清楚会不会把集中清掉。");
        }
        if (used >= slots && slots > 0) {
            out.add("槽满时最忌讳无脑充能：新球会把最右边的球挤掉，"
                    + "如果那是黑暗/等离子，等于白丢资源；先 Dualcast/Multi-Cast/Recursion 激发它。");
        }
        return out;
    }

    public static String plan(AbstractPlayer p, int energy, int incoming) {
        if (p == null) {
            return "";
        }
        int used = p.orbs == null ? 0 : p.orbs.size();
        int slots = Math.max(0, p.maxOrbs);
        if (used >= slots && slots > 0) {
            return "槽满：先激发最右边的球，再充能/扩容；别让新球把黑暗/等离子挤掉。";
        }
        if (used == 0) {
            return "先充能，再激发；没有球时不要先打激发牌。";
        }
        return "先看最右侧球的类型：黑暗攒大再激发，冰球要格挡，等离子换能量；再决定充能还是激发。";
    }

    public static double pickBonus(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return 0.0;
        }
        String id = CardDatabase.norm(c.cardID);
        int orb = d.tag("orb");
        int focus = d.tag("focus");
        double b = 0.0;
        if (id.contains("defragment") || id.contains("biasedcognition")) {
            b += focus < 2 ? 0.5 : 0.25;
        }
        if (id.contains("capacitor")) {
            b += d.tag("orb") < 3 ? 0.35 : 0.1;
        }
        if (id.contains("hyperbeam") || id.contains("reprogram")) {
            b += focus >= 2 ? -0.45 : 0.05;
        }
        if (id.contains("blizzard") || id.contains("barrage")
                || id.contains("thunderstrike") || id.contains("fission")) {
            b += orb >= 3 ? 0.3 : 0.0;
        }
        if (id.contains("electrodynamics")) {
            b += d.aoeCards < 2 ? 0.3 : 0.1;
        }
        if (id.contains("loop")) {
            b += orb >= 2 ? 0.3 : 0.0;
        }
        return b;
    }

    public static String pickNote(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        if (p == null || c == null || d == null) {
            return "";
        }
        String id = CardDatabase.norm(c.cardID);
        int orb = d.tag("orb");
        int focus = d.tag("focus");
        if (id.contains("defragment") || id.contains("biasedcognition")) {
            return "你牌组有 " + orb + " 张球牌 / " + focus + " 张集中牌；"
                    + "集中会同时放大冰球格挡和闪电伤害，是球体系的核心乘数。";
        }
        if (id.contains("hyperbeam")) {
            return "光束会失去 3 点集中；你有 " + focus + " 张集中牌，"
                    + "除非走非集中清场，否则它会把球体系的加成打掉。";
        }
        if (id.contains("reprogram")) {
            return "重构失去集中换力量/敏捷，和球体系反方向；球牌越多越不该拿。";
        }
        if (id.contains("capacitor")) {
            return "你牌组有 " + orb + " 张球牌；扩容增加槽位，球越多越需要槽位，"
                    + "但槽位已经够时先补集中/输出。";
        }
        if (id.contains("blizzard")) {
            return "暴风雪按本场已充能的冰球数增伤；冰球牌越多越强。";
        }
        if (id.contains("barrage")) {
            return "弹幕每有一个球打一段；你球牌多，充能后它的伤害会按球数翻。";
        }
        if (id.contains("fission")) {
            return "裂变按当前球数换能量/抽牌；球多时是爆发，球少时很亏。";
        }
        return "";
    }

    private static int orbsAdded(AbstractCard c, int energy, List<AbstractMonster> alive) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("rainbow")) {
            return 3;
        }
        if (id.contains("glacier") || id.contains("electrodynamics")) {
            return 2;
        }
        if (id.contains("chill")) {
            return Math.max(1, alive == null ? 1 : alive.size());
        }
        if (id.contains("tempest")) {
            return Math.max(0, energy);
        }
        if (id.contains("zap") || id.contains("balllightning") || id.contains("coldsnap")
                || id.contains("coolheaded") || id.contains("fusion") || id.contains("chaos")
                || id.contains("darkness")) {
            return 1;
        }
        return 0;
    }

    private static int evokes(AbstractCard c, int energy, int used) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("dualcast")) {
            return used > 0 ? 2 : 0;
        }
        if (id.contains("multicast")) {
            return Math.max(0, energy);
        }
        if (id.contains("fission")) {
            // Fission removes the orbs for energy/cards; it does not evoke them.
            return 0;
        }
        if (id.contains("recursion")) {
            return Math.min(1, used);
        }
        return 0;
    }

    private static int focus(AbstractPlayer p) {
        AbstractPower power = p == null ? null : p.getPower("Focus");
        return power == null ? 0 : power.amount;
    }

    private static int countOrb(AbstractPlayer p, String type) {
        int n = 0;
        if (p == null || p.orbs == null) {
            return 0;
        }
        for (AbstractOrb orb : p.orbs) {
            if (orb != null && type.equals(orb.ID)) {
                n++;
            }
        }
        return n;
    }

    private static AbstractOrb rightOrb(AbstractPlayer p) {
        if (p == null || p.orbs == null || p.orbs.isEmpty()) {
            return null;
        }
        return p.orbs.get(p.orbs.size() - 1);
    }

    private static String rightId(AbstractPlayer p) {
        AbstractOrb orb = rightOrb(p);
        return orb == null || orb.ID == null ? "" : orb.ID.toLowerCase();
    }

    private static String rightName(AbstractPlayer p) {
        AbstractOrb orb = rightOrb(p);
        if (orb == null) {
            return "没有球";
        }
        if ("Dark".equals(orb.ID)) {
            return "黑暗球";
        }
        if ("Plasma".equals(orb.ID)) {
            return "等离子球";
        }
        if ("Frost".equals(orb.ID)) {
            return "冰霜球";
        }
        if ("Lightning".equals(orb.ID)) {
            return "闪电球";
        }
        return orb.name == null ? orb.ID : orb.name;
    }

    private static String orbValueText(AbstractPlayer p) {
        AbstractOrb orb = rightOrb(p);
        if (orb == null) {
            return "没有球。";
        }
        if ("Dark".equals(orb.ID)) {
            return "黑暗球被动攒伤害，激发时对单体爆发；现在激发约 "
                    + Math.max(0, orb.evokeAmount) + " 伤。";
        }
        if ("Plasma".equals(orb.ID)) {
            return "等离子球被动给能量，激发给更多能量；适合先激发换费用。";
        }
        if ("Frost".equals(orb.ID)) {
            return "冰霜球被动给格挡，激发给更多格挡；现在激发约 "
                    + Math.max(0, orb.evokeAmount) + " 格挡。";
        }
        if ("Lightning".equals(orb.ID)) {
            return "闪电球被动打伤害，激发打更高伤害；现在激发约 "
                    + Math.max(0, orb.evokeAmount) + " 伤。";
        }
        return "这个球当前激发值约 " + Math.max(0, orb.evokeAmount) + "。";
    }

    private static int rightValue(AbstractPlayer p) {
        AbstractOrb orb = rightOrb(p);
        if (orb == null) {
            return 0;
        }
        if ("Dark".equals(orb.ID)) {
            return Math.max(0, orb.evokeAmount) * 2;
        }
        if ("Plasma".equals(orb.ID)) {
            return 14;
        }
        if ("Frost".equals(orb.ID)) {
            return Math.max(0, orb.evokeAmount);
        }
        if ("Lightning".equals(orb.ID)) {
            return Math.max(0, orb.evokeAmount);
        }
        return Math.max(0, orb.evokeAmount);
    }

    private static boolean isValuable(AbstractOrb orb) {
        return orb != null && ("Dark".equals(orb.ID) || "Plasma".equals(orb.ID));
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
