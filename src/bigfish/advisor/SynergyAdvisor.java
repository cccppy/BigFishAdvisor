package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.ArrayList;
import java.util.List;

/**
 * Package-level deck building, rather than a flat card-quality list.
 *
 * <p>Slay the Spire decks are built from a small number of engines: discard,
 * poison, shivs, exhaust, strength, block, orbs, stance, retain, scry and
 * mantra.  A card can be a premium standalone card and still be wrong for the
 * current deck.  This class counts the actual enablers and payoffs already in
 * the deck, then judges whether a new card completes that engine or creates a
 * second, half-finished package.</p>
 *
 * <p>The card IDs are normalized through {@link CardDatabase#norm(String)} so
 * this also works for upgraded cards and localization-independent IDs.</p>
 */
public final class SynergyAdvisor {
    private static final int NONE = 0;
    private static final int OUTLET = 1;
    private static final int PAYOFF = 2;
    private static final int CORE = 3;
    private static final int SUPPORT = 4;

    public static final class Fit {
        public double bonus;
        public String note = "";
        public String key = "";
        public int count;
    }

    public static final class Pack {
        public String key = "";
        public String name = "";
        public int total;
        public int outlets;
        public int payoffs;
        public int cores;
        public int support;

        public double rank() {
            return cores * 2.4 + payoffs * 1.5 + outlets * 1.2 + support * 0.6 + total * 0.25;
        }
    }

    public static final class DeckRead {
        public String key = "";
        public String name = "";
        public double rank;
        public final List<Pack> packs = new ArrayList<Pack>();
    }

    private SynergyAdvisor() {
    }

    public static Fit fit(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        Fit out = new Fit();
        if (c == null || d == null) {
            return out;
        }
        DeckRead read = read(d);
        String id = CardDatabase.norm(c.cardID);
        Pack best = null;
        int bestRole = NONE;
        double bestBonus = -999.0;
        for (Pack pack : read.packs) {
            int role = role(id, pack.key);
            if (role == NONE) {
                continue;
            }
            double b = base(role, pack, read, p, id);
            if (b > bestBonus) {
                bestBonus = b;
                best = pack;
                bestRole = role;
            }
        }
        if (best == null) {
            return out;
        }
        double pair = pairBonus(id, best, d, c);
        out.bonus = bestBonus + pair;
        out.key = best.key;
        out.count = best.total;
        out.note = note(best, bestRole, read, pair);
        return out;
    }

    public static double cardBonus(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        double b = fit(p, c, d).bonus;
        String id = CardDatabase.norm(c.cardID);
        if (poisonPayoffMissing(d)
                && (id.contains("catalyst") || id.contains("corpseexplosion"))) {
            b += 1.15;
        }
        return b;
    }

    public static String note(AbstractPlayer p, AbstractCard c, DeckSummary d) {
        String note = fit(p, c, d).note;
        String id = CardDatabase.norm(c.cardID);
        if (poisonPayoffMissing(d)
                && (id.contains("catalyst") || id.contains("corpseexplosion"))) {
            note = note + " 你已经有 " + d.tag("poison")
                    + " 张毒牌，但缺爆发；这张能把毒层兑现成伤害，别再只叠毒。";
        }
        return note;
    }

    private static boolean poisonPayoffMissing(DeckSummary d) {
        return d.tag("poison") >= 3
                && !d.has("catalyst")
                && !d.has("corpseexplosion");
    }

    /** A compact deck-level line for the card reward / shop panel. */
    public static String deckLine(DeckSummary d) {
        DeckRead r = read(d);
        if (r.key.length() == 0) {
            return "流派判断：目前还没有成型的配合链；先拿能立即用的攻防/过牌，"
                    + "不要为了裸核心牌硬凑一套。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("流派判断：当前偏「").append(r.name).append("」");
        Pack first = pack(r, r.key);
        if (first != null) {
            sb.append("（配合 ").append(first.total)
                    .append("：启动/出口 ").append(first.outlets)
                    .append("，收益 ").append(first.payoffs).append("）");
        }
        Pack second = secondPack(r);
        if (second != null && second.rank() >= 2.0) {
            sb.append("；第二方向「").append(second.name).append("」")
                    .append(second.total).append(" 张");
        }
        sb.append("。");
        Pack primary = pack(r, r.key);
        if (primary != null) {
            if (primary.outlets == 0 && !r.key.equals("power") && !r.key.equals("str")
                    && !r.key.equals("block") && !r.key.equals("selfdmg")) {
                sb.append("现在缺主动启动牌；先补能做事的牌，别只拿收益牌。");
            } else if (primary.outlets > 0 && primary.payoffs == 0) {
                sb.append("有启动但缺收益牌；下一步优先找能把资源兑现的核心。");
            } else if (primary.outlets > 0 && primary.payoffs > 0) {
                sb.append("启动和收益都有了，开始补稳定性/防御，不要继续塞普通牌。");
            }
        }
        if (poisonPayoffMissing(d)) {
            sb.append(" 毒牌已有 ").append(d.tag("poison"))
                    .append(" 张，但没有【催化剂】/【尸爆术】：毒层叠得再高也只是慢慢掉血，")
                    .append("优先补爆发，或者别再继续加毒。");
        }
        return sb.toString();
    }

    public static String directionName(String key) {
        if ("discard".equals(key)) {
            return "弃牌流";
        }
        if ("poison".equals(key)) {
            return "毒流";
        }
        if ("shiv".equals(key)) {
            return "小刀流";
        }
        if ("str".equals(key)) {
            return "力量流";
        }
        if ("exhaust".equals(key)) {
            return "消耗流";
        }
        if ("block".equals(key)) {
            return "防战/格挡";
        }
        if ("selfdmg".equals(key)) {
            return "自伤流";
        }
        if ("orb".equals(key) || "focus".equals(key)) {
            return "充能球/集中";
        }
        if ("zero".equals(key)) {
            return "0 费/爪击";
        }
        if ("power".equals(key)) {
            return "能力流";
        }
        if ("stance".equals(key)) {
            return "姿态流";
        }
        if ("retain".equals(key)) {
            return "保留流";
        }
        if ("scry".equals(key)) {
            return "占卜流";
        }
        if ("mantra".equals(key)) {
            return "神性/真言";
        }
        return key;
    }

    public static boolean fitsPackage(AbstractCard c, String key) {
        if (c == null || key == null || key.length() == 0) {
            return false;
        }
        String id = CardDatabase.norm(c.cardID);
        String k = "focus".equals(key) ? "orb" : key;
        return role(id, k) != NONE;
    }

    public static boolean isCoreCard(AbstractCard c) {
        if (c == null) {
            return false;
        }
        String id = CardDatabase.norm(c.cardID);
        for (String[] dir : allDirections()) {
            if (role(id, dir[0]) == CORE) {
                return true;
            }
        }
        return false;
    }

    /** True when the candidate belongs to the deck's committed package. */
    public static boolean isOffPackage(AbstractCard c, DeckSummary d) {
        DeckRead r = read(d);
        if (r.key.length() == 0 || r.rank < 1.2) {
            return false;
        }
        return !fitsPackage(c, r.key);
    }

    /** Used by ArchetypeAdvisor to prefer a direction with real enablers. */
    public static double directionWeight(DeckSummary d, String key) {
        DeckRead r = read(d);
        Pack p = pack(r, key);
        return p == null ? 0.0 : p.rank();
    }

    public static int packageCount(DeckSummary d, String key) {
        DeckRead r = read(d);
        Pack p = pack(r, key);
        return p == null ? 0 : p.total;
    }

    public static boolean matchesDirection(AbstractCard c, String key) {
        if (c == null || key == null || key.length() == 0) {
            return false;
        }
        String k = "focus".equals(key) ? "orb" : key;
        if (role(CardDatabase.norm(c.cardID), k) != NONE) {
            return true;
        }
        return CardDatabase.hasTag(c, key);
    }

    // ------------------------------------------------------------- deck state

    public static DeckRead read(DeckSummary d) {
        DeckRead r = new DeckRead();
        if (d == null) {
            return r;
        }
        for (String[] dir : directions(d.character)) {
            Pack p = new Pack();
            p.key = dir[0];
            p.name = dir[1];
            for (String cardId : d.cardIds) {
                int role = role(CardDatabase.norm(cardId), p.key);
                if (role == OUTLET) {
                    p.outlets++;
                } else if (role == PAYOFF) {
                    p.payoffs++;
                } else if (role == CORE) {
                    p.cores++;
                } else if (role == SUPPORT) {
                    p.support++;
                }
                if (role != NONE) {
                    p.total++;
                }
            }
            r.packs.add(p);
        }
        for (Pack p : r.packs) {
            if (p.total >= 2 && p.rank() > r.rank
                    || (p.total >= 2 && Math.abs(p.rank() - r.rank) < 0.001
                    && p.total > (pack(r, r.key) == null ? 0 : pack(r, r.key).total))) {
                r.rank = p.rank();
                r.key = p.key;
                r.name = p.name;
            }
        }
        return r;
    }

    private static Pack pack(DeckRead r, String key) {
        if (r == null || key == null) {
            return null;
        }
        String k = "focus".equals(key) ? "orb" : key;
        for (Pack p : r.packs) {
            if (p.key.equals(k)) {
                return p;
            }
        }
        return null;
    }

    private static Pack secondPack(DeckRead r) {
        Pack best = null;
        for (Pack p : r.packs) {
            if (p.key.equals(r.key) || p.total < 2) {
                continue;
            }
            if (best == null || p.rank() > best.rank()) {
                best = p;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ card roles

    private static int role(String id, String key) {
        if (id.length() == 0 || key == null) {
            return NONE;
        }
        if ("discard".equals(key)) {
            if (id.contains("toolsofthetrade")) {
                return CORE;
            }
            if (any(id, "eviscerate", "sneakystrike", "tactician", "reflex",
                    "grandfinale", "expertise")) {
                return PAYOFF;
            }
            if (any(id, "survivor", "prepared", "acrobatics", "daggerthrow",
                    "calculatedgamble", "concentrate", "stormofsteel", "alloutattack",
                    "unload")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("poison".equals(key)) {
            if (any(id, "noxiousfumes", "envenom")) {
                return CORE;
            }
            if (any(id, "catalyst", "bane", "corpseexplosion")) {
                return PAYOFF;
            }
            if (any(id, "deadlypoison", "poisonedstab", "bouncingflask",
                    "cripplingpoison", "cripplingcloud")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("shiv".equals(key)) {
            if (any(id, "accuracy", "afterimage", "infiniteblades")) {
                return CORE;
            }
            if (any(id, "accuracy", "afterimage", "envenom", "wristblade",
                    "finisher", "athousandcuts", "panache")) {
                return PAYOFF;
            }
            if (any(id, "bladedance", "cloakanddagger", "infiniteblades",
                    "stormofsteel")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("str".equals(key)) {
            if (any(id, "inflame", "spotweakness", "demonform")) {
                return CORE;
            }
            if (any(id, "limitbreak", "heavyblade")) {
                return PAYOFF;
            }
            if (any(id, "flex", "rupture", "wish", "fasting2")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("exhaust".equals(key)) {
            if (any(id, "corruption", "darkembrace", "feelnopain")) {
                return CORE;
            }
            if (any(id, "truegrit", "burningpact", "secondwind", "fiendfire",
                    "seversoul", "exhume", "sentinel", "havoc")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("block".equals(key)) {
            if (any(id, "barricade", "juggernaut")) {
                return CORE;
            }
            if (any(id, "bodyslam", "entrench")) {
                return PAYOFF;
            }
            if (any(id, "shrugitoff", "ghostlyarmor", "flamebarrier", "metallicize",
                    "impervious", "powerthrough", "truegrit", "secondwind")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("selfdmg".equals(key)) {
            if (any(id, "rupture", "combust", "brutality")) {
                return CORE;
            }
            if (any(id, "bloodletting", "offering", "hemokinesis", "berserk")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("orb".equals(key)) {
            if (any(id, "defragment", "biasedcognition", "consume", "capacitor",
                    "loop", "electrodynamics", "echoform", "creativeai",
                    "staticdischarge", "storm")) {
                return CORE;
            }
            if (any(id, "multicast", "thunderstrike", "blizzard", "barrage")) {
                return PAYOFF;
            }
            if (any(id, "zap", "balllightning", "coldsnap", "glacier", "doomandgloom",
                    "darkness", "fusion", "dualcast", "rainbow", "tempest", "recursion",
                    "compiledriver", "fission")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("zero".equals(key)) {
            if (any(id, "allforone", "claw", "scrape")) {
                return PAYOFF;
            }
            if (any(id, "turbo", "rebound", "steam", "hologram")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("power".equals(key)) {
            if (any(id, "creativeai", "echoform", "machinelearning", "electrodynamics")) {
                return CORE;
            }
            return NONE;
        }
        if ("stance".equals(key)) {
            if (any(id, "rushdown", "mentalfortress", "talktothehand", "likewater")) {
                return CORE;
            }
            if (any(id, "flurryofblows", "tantrum")) {
                return PAYOFF;
            }
            if (any(id, "eruption", "vigilance", "emptyfist", "emptybody",
                    "emptymind", "crescendo", "indignation", "fearnoevil",
                    "innerpeace", "meditate")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("retain".equals(key)) {
            if (any(id, "welllaidplans", "establishment")) {
                return CORE;
            }
            if (any(id, "windmillstrike", "sandsoftime", "perseverance",
                    "protect", "flyingleeves", "worship")) {
                return PAYOFF;
            }
            if (any(id, "meditate", "crescendo", "miracle")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("scry".equals(key)) {
            if (any(id, "nirvana", "masterreality")) {
                return CORE;
            }
            if (any(id, "weave")) {
                return PAYOFF;
            }
            if (any(id, "thirdeye", "cutthroughfate", "justlucky")) {
                return OUTLET;
            }
            return NONE;
        }
        if ("mantra".equals(key)) {
            if (any(id, "devotion")) {
                return CORE;
            }
            if (any(id, "blasphemy")) {
                return PAYOFF;
            }
            if (any(id, "worship", "pray", "prostrate")) {
                return OUTLET;
            }
            return NONE;
        }
        return NONE;
    }

    private static double base(int role, Pack pack, DeckRead read, AbstractPlayer p, String id) {
        double b;
        if (role == CORE) {
            b = pack.outlets >= 2 ? 1.25 : pack.outlets >= 1 ? 0.55 : -0.30;
            if (isStandaloneCore(id)) {
                b = Math.max(b, 0.45);
            }
        } else if (role == PAYOFF) {
            b = pack.outlets >= 2 ? 1.15 : pack.outlets >= 1 ? 0.50 : -0.65;
        } else if (role == OUTLET) {
            b = pack.payoffs >= 1 ? 0.90 : pack.cores >= 1 ? 0.50
                    : pack.total >= 1 ? 0.25 : 0.05;
        } else {
            b = pack.total >= 2 ? 0.35 : 0.08;
        }
        b += Math.min(0.60, pack.total * 0.08);
        if (read.key.length() > 0 && pack.key.equals(read.key) && read.rank >= 2.5) {
            b += 0.15;
        } else if (read.key.length() > 0 && !pack.key.equals(read.key)
                && read.rank >= 3.5 && role >= PAYOFF) {
            b -= 0.45;
        }
        b += relicBonus(p, pack.key, id);
        return b;
    }

    private static boolean isStandaloneCore(String id) {
        return any(id, "wraithform", "biasedcognition", "echoform", "demonform",
                "electrodynamics", "blasphemy", "barricade", "corruption");
    }

    private static double pairBonus(String id, Pack pack, DeckSummary d, AbstractCard c) {
        double b = 0.0;
        if ("discard".equals(pack.key)) {
            if (any(id, "tactician", "reflex")) {
                b += pack.outlets == 0 ? -0.85 : 0.25 + pack.outlets * 0.12;
            } else if (any(id, "eviscerate", "sneakystrike")) {
                b += pack.outlets == 0 ? -0.55 : 0.22;
            } else if (id.contains("grandfinale")) {
                b += d.drawCards >= 3 ? 0.55 : -0.75;
            } else if (id.contains("expertise")) {
                b += d.drawCards >= 2 ? 0.25 : -0.15;
            } else if (id.contains("calculatedgamble")) {
                b += pack.payoffs > 0 ? 0.40 : 0.10;
            }
        } else if ("poison".equals(pack.key)) {
            if (id.contains("catalyst")) {
                b += pack.outlets == 0 ? -1.0 : pack.outlets >= 2 ? 0.90 : 0.40;
            } else if (id.contains("bane")) {
                b += pack.outlets == 0 ? -0.40 : 0.35;
            } else if (id.contains("corpseexplosion")) {
                b += pack.outlets == 0 ? -0.20 : 0.35;
            }
        } else if ("str".equals(pack.key)) {
            if (id.contains("limitbreak")) {
                b += pack.cores == 0 ? -0.60 : 0.50;
            } else if (id.contains("heavyblade")) {
                b += pack.cores == 0 ? -0.20 : 0.35;
            }
        } else if ("block".equals(pack.key)) {
            if (id.contains("bodyslam")) {
                b += d.blockCards < 5 ? -0.30 : 0.45;
            } else if (id.contains("entrench")) {
                b += d.has("barricade") ? 0.75 : d.blockCards >= 6 ? 0.30 : -0.15;
            }
        } else if ("exhaust".equals(pack.key)) {
            if (id.contains("corruption")) {
                b += d.skills < 7 ? -0.35 : 0.45;
            } else if (any(id, "darkembrace", "feelnopain")) {
                b += d.tag("exhaust") < 2 ? -0.35 : 0.45;
            }
        } else if ("orb".equals(pack.key)) {
            if (id.contains("electrodynamics")) {
                b += pack.outlets < 2 ? -0.25 : 0.45;
            } else if (id.contains("capacitor")) {
                b += pack.cores == 0 && d.tag("focus") == 0 ? -0.30 : 0.35;
            } else if (id.contains("multicast")) {
                b += pack.outlets < 2 ? -0.40 : 0.35;
            } else if (any(id, "blizzard", "thunderstrike", "barrage")) {
                b += pack.outlets < 3 ? -0.25 : 0.30;
            }
        } else if ("zero".equals(pack.key)) {
            if (any(id, "allforone", "claw")) {
                b += d.tag("zero") < 3 ? -0.35 : 0.45;
            }
        } else if ("stance".equals(pack.key)) {
            if (any(id, "rushdown", "mentalfortress")) {
                b += d.tag("stance") < 2 ? -0.35 : 0.45;
            }
        } else if ("retain".equals(pack.key)) {
            if (id.contains("welllaidplans")) {
                b += d.tag("retain") < 2 ? -0.15 : 0.40;
            } else if (id.contains("establishment")) {
                b += d.tag("retain") < 3 ? -0.25 : 0.45;
            }
        } else if ("scry".equals(pack.key)) {
            if (any(id, "nirvana", "masterreality")) {
                b += d.tag("scry") < 2 ? -0.30 : 0.40;
            }
        } else if ("mantra".equals(pack.key)) {
            if (id.contains("blasphemy")) {
                b += d.tag("mantra") < 3 ? -0.35 : 0.45;
            }
        }
        return b;
    }

    private static String note(Pack pack, int role, DeckRead read, double pair) {
        StringBuilder sb = new StringBuilder();
        sb.append("「").append(pack.name).append("」");
        if (pack.total == 0) {
            sb.append("：牌组还没有这张牌的配合点");
        } else {
            sb.append("：已有 ").append(pack.total).append(" 张配合牌（启动 ")
                    .append(pack.outlets).append("、收益 ").append(pack.payoffs).append("）");
        }
        if (role == PAYOFF && pack.outlets == 0) {
            sb.append("；现在没有主动启动/弃牌/上毒出口，裸拿容易卡手");
        } else if (role == CORE && pack.outlets == 0) {
            sb.append("；先补能触发它的低费牌，不要只拿核心");
        } else if (role == OUTLET && pack.payoffs == 0) {
            sb.append("；现在只有启动没有收益，下一步要找能兑现资源的牌");
        }
        if (pair <= -0.25) {
            sb.append("；这张和当前配合不成立，别为了它硬转");
        }
        if (read.key.length() > 0 && !pack.key.equals(read.key) && read.rank >= 3.5) {
            sb.append("；你已经有更完整的「").append(read.name).append("」，这张会稀释主线");
        }
        return sb.toString();
    }

    private static double relicBonus(AbstractPlayer p, String key, String id) {
        if (p == null) {
            return 0.0;
        }
        double b = 0.0;
        if ("discard".equals(key)) {
            if (hasRelic(p, "Hovering Kite")) {
                b += 0.40;
            }
            if (hasRelic(p, "Tough Bandages")) {
                b += 0.35;
            }
            if (hasRelic(p, "Tingsha")) {
                b += 0.35;
            }
        } else if ("poison".equals(key)) {
            if (hasRelic(p, "Snecko Skull")) {
                b += 0.50;
            }
            if (hasRelic(p, "The Specimen")) {
                b += 0.30;
            }
        } else if ("shiv".equals(key)) {
            if (hasRelic(p, "Wrist Blade")) {
                b += 0.45;
            }
        } else if ("exhaust".equals(key)) {
            if (hasRelic(p, "Dead Branch")) {
                b += 0.45;
            }
        } else if ("power".equals(key)) {
            if (hasRelic(p, "Mummified Hand")) {
                b += 0.25;
            }
        } else if ("stance".equals(key)) {
            if (hasRelic(p, "Violet Lotus")) {
                b += 0.30;
            }
        } else if ("orb".equals(key)) {
            if (hasRelic(p, "Nuclear Battery")) {
                b += 0.25;
            }
        } else if ("selfdmg".equals(key)) {
            if (hasRelic(p, "Tungsten Rod")) {
                b += 0.30;
            }
        }
        return b;
    }

    private static boolean hasRelic(AbstractPlayer p, String idPart) {
        if (p == null || p.relics == null) {
            return false;
        }
        String needle = CardDatabase.norm(idPart);
        for (AbstractRelic r : p.relics) {
            if (r != null && CardDatabase.norm(r.relicId).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean any(String id, String... parts) {
        for (String part : parts) {
            if (id.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static String[][] allDirections() {
        return new String[][]{
                {"discard", "弃牌流"}, {"poison", "毒流"}, {"shiv", "小刀流"},
                {"str", "力量流"}, {"exhaust", "消耗流"}, {"block", "防战/格挡"},
                {"selfdmg", "自伤流"}, {"orb", "充能球/集中"}, {"zero", "0 费/爪击"},
                {"power", "能力流"}, {"stance", "姿态流"}, {"retain", "保留流"},
                {"scry", "占卜流"}, {"mantra", "神性/真言"}
        };
    }

    private static String[][] directions(String character) {
        if (character != null && character.contains("SILENT")) {
            return new String[][]{
                    {"discard", "弃牌流"}, {"poison", "毒流"}, {"shiv", "小刀流"}
            };
        }
        if (character != null && character.contains("DEFECT")) {
            return new String[][]{
                    {"orb", "充能球/集中"}, {"zero", "0 费/爪击"}, {"power", "能力流"}
            };
        }
        if (character != null && character.contains("WATCHER")) {
            return new String[][]{
                    {"stance", "姿态流"}, {"retain", "保留流"},
                    {"scry", "占卜流"}, {"mantra", "神性/真言"}
            };
        }
        return new String[][]{
                {"str", "力量流"}, {"exhaust", "消耗流"},
                {"block", "防战/格挡"}, {"selfdmg", "自伤流"}
        };
    }
}
