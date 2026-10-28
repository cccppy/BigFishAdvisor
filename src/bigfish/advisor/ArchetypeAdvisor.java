package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deck-building stage model.
 *
 * Early Slay the Spire is not "pick an archetype from floor 1"; it is
 * foundation -> direction -> refine -> finish. The community guides repeatedly
 * make the same point: take enough front-loaded damage, block and draw to
 * survive, then commit to the synergy the run is actually offering.
 */
public final class ArchetypeAdvisor {
    public static final class Read {
        public String stage = "";
        public String directionKey = "";
        public String directionName = "";
        public String plan = "";
        public String warning = "";
        public int coreCount;
        public int directionCount;
        public boolean foundationWeak;
    }

    private ArchetypeAdvisor() {
    }

    public static Read read(AbstractPlayer p, DeckSummary d, int act, int floor) {
        Read r = new Read();
        if (d == null) {
            return r;
        }
        r.foundationWeak = d.attacks < 4 || d.blockCards < 4
                || (act >= 2 && d.aoeCards < 1)
                || (d.size >= 12 && d.drawCards < 1);
        r.coreCount = coreCount(p);
        String[] dir = bestDirection(p, d);
        r.directionKey = dir[0];
        r.directionName = dir[1];
        r.directionCount = dir[0].length() == 0 ? 0 : d.tag(dir[0]);

        if (act == 1 && floor <= 7) {
            r.stage = "打基础";
        } else if (r.coreCount < 2) {
            r.stage = "找方向";
        } else if (r.coreCount < 4) {
            r.stage = "定核心";
        } else if (act >= 3) {
            r.stage = "收尾";
        } else {
            r.stage = "补强";
        }

        List<String> gaps = new ArrayList<String>();
        if (d.attacks < 4) {
            gaps.add("攻击");
        }
        if (d.blockCards < 4) {
            gaps.add("格挡");
        }
        if (act >= 2 && d.aoeCards < 1) {
            gaps.add("AOE");
        }
        if (d.size >= 12 && d.drawCards < 1) {
            gaps.add("抽牌");
        }
        if (!gaps.isEmpty()) {
            r.warning = "基础缺口：" + join(gaps) + "。";
        }

        if (r.stage.equals("打基础")) {
            r.plan = "现在先补基础：能立即用的伤害、格挡、AOE、抽牌优先；"
                    + "不要为了一个需要整套配合的核心牌，把前期战斗力让掉。";
        } else if (r.stage.equals("找方向")) {
            r.plan = r.directionKey.length() > 0
                    ? "你已经有一点「" + r.directionName + "」的影子；先找 1 张能和现有牌配合的核心，不要同时开两条线。"
                    : "还没有明确方向；先看现有牌/遗物，哪条线的配合最多，再拿第一张核心。";
        } else if (r.stage.equals("定核心")) {
            r.plan = "方向是「" + r.directionName + "」；现在补 1-2 张核心/发动机，"
                    + "普通过渡牌开始少拿，避免牌组变厚。";
        } else if (r.stage.equals("收尾")) {
            r.plan = "牌组基本定型：只补短板和关键升级，非核心牌很容易稀释牌组。";
        } else {
            r.plan = "进入补强期：优先能和「" + r.directionName + "」联动的牌，"
                    + "以及抽牌/能量/防御短板；不要为了新鲜感换流派。";
        }
        return r;
    }

    /** Extra score for the card reward / shop card. */
    public static double cardBonus(AbstractPlayer p, AbstractCard c, DeckSummary d, int act, int floor) {
        if (p == null || c == null || d == null) {
            return 0.0;
        }
        Read r = read(p, d, act, floor);
        boolean core = isCore(c);
        boolean foundation = isFoundation(c);
        int enablers = enablersFor(c, d);
        double b = 0.0;

        if (r.stage.equals("打基础")) {
            if (foundation) {
                b += 0.55;
                if (CardDatabase.hasTag(c, "aoe")) {
                    b += 0.25;
                }
            }
            if (core) {
                if (enablers >= 2) {
                    b += 0.55;
                } else if (isStandaloneCore(c)) {
                    b += 0.10;
                } else {
                    b -= 0.85;
                }
            }
        } else if (r.stage.equals("找方向")) {
            if (core) {
                if (enablers >= 2) {
                    b += 1.10;
                } else if (isStandaloneCore(c)) {
                    b += 0.15;
                } else {
                    b -= 0.35;
                }
            }
            if (matchesDirection(c, r.directionKey)) {
                b += 0.25;
            }
            if (core && isDifferentDirection(c, r.directionKey) && r.directionCount >= 3) {
                b -= 0.45;
            }
        } else if (r.stage.equals("定核心")) {
            if (core) {
                b += enablers >= 2 ? 1.20 : 0.55;
            }
            if (matchesDirection(c, r.directionKey)) {
                b += 0.55;
            } else if (core && isDifferentDirection(c, r.directionKey)) {
                b -= 0.65;
            }
        } else {
            if (matchesDirection(c, r.directionKey)) {
                b += 0.55;
            } else if (isDifferentDirection(c, r.directionKey)) {
                b -= 0.85;
            }
            if (core && enablers >= 2) {
                b += 0.65;
            }
            if (!core && !foundation && d.size >= 22) {
                b -= 0.45;
            }
        }

        // A card that can start a new plan is allowed to pivot only when the
        // deck already has two real enablers; otherwise it is a trap.
        if (!core && isTransitional(c) && r.stage.equals("打基础")) {
            b += 0.25;
        }
        return b;
    }

    public static String cardNote(AbstractPlayer p, AbstractCard c, DeckSummary d, int act, int floor) {
        if (p == null || c == null || d == null) {
            return "";
        }
        Read r = read(p, d, act, floor);
        int enablers = enablersFor(c, d);
        if (isCore(c)) {
            if (enablers >= 2) {
                return "现有牌里已经有 " + enablers + " 个配合点，这张核心可以开始定型";
            }
            if (isStandaloneCore(c)) {
                return "这张本身够强，配合少也能拿；但别为了它硬塞一整套不合适的牌";
            }
            if (r.stage.equals("打基础")) {
                return "这张是核心，但现在配合点太少；先拿能打的过渡牌，等有 2 个配合点再围绕它";
            }
            return "这张核心目前配合点不足，拿了容易变成“裸核心”";
        }
        if (r.stage.equals("打基础") && isFoundation(c)) {
            return "现在属于打基础阶段，这种能立即用的牌比裸核心更稳";
        }
        if (r.stage.equals("打基础") && isTransitional(c)) {
            return "过渡牌可以拿，但别超过前期需要的数量";
        }
        if (matchesDirection(c, r.directionKey)) {
            return "和你现在的「" + r.directionName + "」方向吻合";
        }
        if (isDifferentDirection(c, r.directionKey) && r.directionCount >= 3) {
            return "会偏离当前的「" + r.directionName + "」方向，除非有明确转型理由";
        }
        return "";
    }

    public static String stageLine(AbstractPlayer p, DeckSummary d, int act, int floor) {
        Read r = read(p, d, act, floor);
        return "牌组阶段：" + r.stage
                + (r.directionName.length() == 0 ? "" : " · 方向：" + r.directionName)
                + "。";
    }

    public static String planLine(AbstractPlayer p, DeckSummary d, int act, int floor) {
        Read r = read(p, d, act, floor);
        return r.plan + (r.warning.length() == 0 ? "" : r.warning);
    }

    // ------------------------------------------------------------- core data

    private static boolean isCore(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        return id.contains("corruption") || id.contains("darkembrace") || id.contains("feelnopain")
                || id.contains("barricade") || id.contains("limitbreak") || id.contains("demonform")
                || id.contains("juggernaut") || id.contains("rupture") || id.contains("combust")
                || id.contains("catalyst") || id.contains("noxiousfumes") || id.contains("accuracy")
                || id.contains("wraithform") || id.contains("toolsofthetrade")
                || id.contains("athousandcuts") || id.contains("envenom")
                || id.contains("corpseexplosion") || id.contains("nightmare")
                || id.contains("electrodynamics") || id.contains("biasedcognition")
                || id.contains("echoform") || id.contains("creativeai") || id.contains("consume")
                || id.contains("capacitor") || id.contains("defragment") || id.contains("loop")
                || id.contains("allforone") || id.contains("machinelearning")
                || id.contains("rushdown") || id.contains("mentalfortress")
                || id.contains("talktothehand") || id.contains("tantrum")
                || id.contains("devaform") || id.contains("blasphemy")
                || id.contains("scrawl") || id.contains("vault");
    }

    private static boolean isStandaloneCore(AbstractCard c) {
        String id = CardDatabase.norm(c.cardID);
        return id.contains("wraithform") || id.contains("biasedcognition")
                || id.contains("echoform") || id.contains("demonform")
                || id.contains("electrodynamics") || id.contains("blasphemy");
    }

    private static boolean isFoundation(AbstractCard c) {
        if (c.type == AbstractCard.CardType.ATTACK) {
            return c.baseDamage >= 7 || c.damage >= 7 || CardDatabase.hasTag(c, "aoe");
        }
        if (c.type == AbstractCard.CardType.SKILL) {
            return c.baseBlock > 0 || c.block > 0 || CardDatabase.hasTag(c, "block")
                    || c.draw > 0 || CardDatabase.hasTag(c, "draw");
        }
        return false;
    }

    private static boolean isTransitional(AbstractCard c) {
        if (c.type == AbstractCard.CardType.ATTACK) {
            return c.costForTurn <= 2 && !isCore(c);
        }
        if (c.type == AbstractCard.CardType.SKILL) {
            return c.costForTurn <= 2 && (c.block > 0 || c.baseBlock > 0 || c.draw > 0);
        }
        return false;
    }

    private static int enablersFor(AbstractCard c, DeckSummary d) {
        String id = CardDatabase.norm(c.cardID);
        int n = 0;
        if (id.contains("corruption") || id.contains("darkembrace") || id.contains("feelnopain")) {
            n += Math.min(3, d.tag("exhaust"));
            if (hasCore(d, "darkembrace") || hasCore(d, "feelnopain") || hasCore(d, "corruption")) {
                n += 2;
            }
        }
        if (id.contains("barricade") || id.contains("juggernaut")) {
            n += Math.min(3, d.blockCards / 2);
            if (hasCard(d, "Body Slam") || hasCard(d, "Entrench")) {
                n += 2;
            }
        }
        if (id.contains("rupture") || id.contains("combust")) {
            n += Math.min(3, d.tag("selfdmg"));
        }
        if (id.contains("limitbreak") || id.contains("demonform")) {
            n += Math.min(3, d.tag("str"));
            if (hasCore(d, "inflame") || hasCore(d, "spotweakness")) {
                n += 2;
            }
        }
        if (id.contains("catalyst") || id.contains("noxiousfumes") || id.contains("envenom")) {
            n += Math.min(3, d.tag("poison"));
        }
        if (id.contains("accuracy") || id.contains("wraithform") || id.contains("toolsofthetrade")) {
            n += Math.min(3, d.tag("shiv"));
        }
        if (id.contains("wraithform") || id.contains("toolsofthetrade")) {
            n += Math.min(2, d.tag("discard"));
        }
        if (id.contains("electrodynamics") || id.contains("biasedcognition")
                || id.contains("echoform") || id.contains("creativeai") || id.contains("consume")
                || id.contains("capacitor") || id.contains("defragment") || id.contains("loop")) {
            n += Math.min(3, d.tag("focus") + d.tag("orb"));
        }
        if (id.contains("rushdown") || id.contains("mentalfortress") || id.contains("tantrum")
                || id.contains("talktothehand") || id.contains("devaform")) {
            n += Math.min(3, d.tag("stance"));
        }
        if (id.contains("blasphemy")) {
            n += Math.min(2, d.tag("mantra") + d.tag("stance"));
        }
        if (id.contains("scrawl") || id.contains("vault")) {
            n += Math.min(2, d.tag("draw") + d.tag("retain"));
        }
        return n;
    }

    private static boolean matchesDirection(AbstractCard c, String direction) {
        if (direction.length() == 0) {
            return false;
        }
        if (CardDatabase.hasTag(c, direction)) {
            return true;
        }
        String id = CardDatabase.norm(c.cardID);
        if (direction.equals("str")) {
            return id.contains("inflame") || id.contains("spotweakness") || id.contains("heavyblade");
        }
        if (direction.equals("exhaust")) {
            return c.exhaust || id.contains("corruption") || id.contains("darkembrace")
                    || id.contains("feelnopain");
        }
        if (direction.equals("block")) {
            return c.block > 0 || c.baseBlock > 0 || id.contains("barricade")
                    || id.contains("bodyslam") || id.contains("entrench");
        }
        if (direction.equals("poison")) {
            return id.contains("poison") || id.contains("catalyst") || id.contains("noxious");
        }
        if (direction.equals("shiv")) {
            return id.contains("shiv") || id.contains("blade") || id.contains("accuracy");
        }
        if (direction.equals("discard")) {
            return id.contains("discard") || id.contains("prepared") || id.contains("acrobatics");
        }
        if (direction.equals("focus") || direction.equals("orb")) {
            return id.contains("focus") || id.contains("orb") || id.contains("defragment")
                    || id.contains("capacitor") || id.contains("balllightning")
                    || id.contains("coldsnap") || id.contains("glacier");
        }
        if (direction.equals("stance")) {
            return id.contains("stance") || id.contains("rushdown") || id.contains("tantrum")
                    || id.contains("eruption") || id.contains("vigilance");
        }
        return false;
    }

    private static boolean isDifferentDirection(AbstractCard c, String direction) {
        if (direction.length() == 0 || matchesDirection(c, direction)) {
            return false;
        }
        return isCore(c) && !isStandaloneCore(c);
    }

    private static boolean hasCore(DeckSummary d, String idPart) {
        return d != null && d.has(idPart);
    }

    private static boolean hasCard(DeckSummary d, String name) {
        return d != null && d.has(name);
    }

    private static int coreCount(AbstractPlayer p) {
        int n = 0;
        if (p == null || p.masterDeck == null) {
            return 0;
        }
        for (AbstractCard c : p.masterDeck.group) {
            if (isCore(c)) {
                n++;
            }
        }
        return n;
    }

    private static String[] bestDirection(AbstractPlayer p, DeckSummary d) {
        String best = "";
        String bestName = "";
        int bestScore = 2;
        for (String[] m : directions(d)) {
            int score = d.tag(m[0]) + relicDirection(p, m[0]);
            if (score > bestScore) {
                bestScore = score;
                best = m[0];
                bestName = m[1];
            }
        }
        return new String[]{best, bestName};
    }

    private static String[][] directions(DeckSummary d) {
        if (d.character.contains("SILENT")) {
            return new String[][]{
                    {"poison", "毒流"}, {"shiv", "小刀流"},
                    {"discard", "弃牌流"}, {"block", "防御流"}
            };
        }
        if (d.character.contains("DEFECT")) {
            return new String[][]{
                    {"focus", "集中/冰球"}, {"orb", "充能球"},
                    {"zero", "0 费/爪击"}, {"power", "能力流"}
            };
        }
        if (d.character.contains("WATCHER")) {
            return new String[][]{
                    {"stance", "姿态流"}, {"retain", "保留流"},
                    {"scry", "占卜流"}, {"mantra", "神性流"}
            };
        }
        return new String[][]{
                {"str", "力量流"}, {"exhaust", "消耗流"},
                {"block", "防战/格挡"}, {"selfdmg", "自伤流"}
        };
    }

    private static int relicDirection(AbstractPlayer p, String key) {
        if (p == null || p.relics == null) {
            return 0;
        }
        int n = 0;
        for (AbstractRelic r : p.relics) {
            if (r == null) {
                continue;
            }
            String id = CardDatabase.norm(r.relicId);
            if (key.equals("exhaust") && id.contains("deadbranch")) {
                n += 2;
            }
            if (key.equals("shiv") && id.contains("wristblade")) {
                n += 2;
            }
            if (key.equals("discard") && id.contains("hoveringkite")) {
                n += 2;
            }
            if ((key.equals("focus") || key.equals("orb")) && id.contains("nuclearbattery")) {
                n += 1;
            }
            if (key.equals("str") && id.contains("redskull")) {
                n += 1;
            }
        }
        return n;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size() && i < 4; i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
