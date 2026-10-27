package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A fast, intentionally simple deck diagnosis used by the reward advisor. */
public final class DeckSummary {
    public int size;
    public int attacks;
    public int skills;
    public int powers;
    public int blockCards;
    public int aoeCards;
    public int drawCards;
    public int curses;
    public int zeroCost;
    public String character = "";
    public final Map<String, Integer> tagCounts = new HashMap<String, Integer>();

    private DeckSummary() {
    }

    public static DeckSummary of(AbstractPlayer player) {
        DeckSummary d = new DeckSummary();
        if (player == null || player.masterDeck == null) {
            return d;
        }
        d.character = player.chosenClass == null ? "" : player.chosenClass.name();
        List<AbstractCard> cards = new ArrayList<AbstractCard>(player.masterDeck.group);
        d.size = cards.size();
        for (AbstractCard c : cards) {
            if (c.type == AbstractCard.CardType.ATTACK) {
                d.attacks++;
            } else if (c.type == AbstractCard.CardType.SKILL) {
                d.skills++;
            } else if (c.type == AbstractCard.CardType.POWER) {
                d.powers++;
            } else if (c.type == AbstractCard.CardType.CURSE) {
                d.curses++;
            }
            if (c.cost == 0 || c.costForTurn == 0) {
                d.zeroCost++;
            }
            if (c.block > 0 || c.baseBlock > 0 || CardDatabase.hasTag(c, "block")) {
                d.blockCards++;
            }
            if (CardDatabase.hasTag(c, "aoe")) {
                d.aoeCards++;
            }
            if (c.draw > 0 || CardDatabase.hasTag(c, "draw")) {
                d.drawCards++;
            }
            Set<String> tags = CardDatabase.tags(c);
            for (String tag : tags) {
                Integer old = d.tagCounts.get(tag);
                d.tagCounts.put(tag, old == null ? 1 : old + 1);
            }
        }
        return d;
    }

    public int tag(String tag) {
        Integer n = tagCounts.get(tag);
        return n == null ? 0 : n;
    }

    public String archetypeKey() {
        String best = null;
        int bestCount = 2;
        for (String[] m : archetypeMap()) {
            int n = tag(m[0]);
            if (n > bestCount) {
                bestCount = n;
                best = m[0];
            }
        }
        return best == null ? "" : best;
    }

    private String[][] archetypeMap() {
        if (character.contains("SILENT")) {
            return new String[][]{
                    {"poison", "毒流"}, {"shiv", "小刀流"},
                    {"discard", "弃牌流"}, {"block", "敏捷/防御"}
            };
        }
        if (character.contains("DEFECT")) {
            return new String[][]{
                    {"focus", "集中/冰球"}, {"orb", "充能球"},
                    {"zero", "0 费/爪击"}, {"power", "能力流"}
            };
        }
        if (character.contains("WATCHER")) {
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

    public String secondaryKey() {
        String primary = archetypeKey();
        String best = null;
        int bestCount = 2;
        for (String[] m : archetypeMap()) {
            if (m[0].equals(primary)) {
                continue;
            }
            int n = tag(m[0]);
            if (n > bestCount) {
                bestCount = n;
                best = m[0];
            }
        }
        return best == null ? "" : best;
    }

    public String archetype() {
        String key = archetypeKey();
        if (key.length() == 0) {
            return "基础/混合";
        }
        for (String[] m : archetypeMap()) {
            if (m[0].equals(key)) {
                return m[1];
            }
        }
        return "基础/混合";
    }

    public String countsLine() {
        return String.format("牌组 %d 张（攻击 %d / 技能 %d / 能力 %d）· %s",
                size, attacks, skills, powers, archetype());
    }

    public List<String> needs(int act) {
        List<String> needs = new ArrayList<String>();
        if (attacks < 4) {
            needs.add("攻击牌偏少，前期需要补输出");
        }
        if (act >= 2 && blockCards < 7) {
            needs.add("第二幕多段/精英伤害高，格挡牌偏少，优先补防御");
        } else if (blockCards < 4) {
            needs.add("防御牌偏少，容易掉血");
        }
        if (aoeCards < 1 && act >= 2) {
            needs.add("缺群体伤害，群怪战会难受");
        }
        if (drawCards < 1 && size >= 12) {
            needs.add("缺抽牌，关键牌不容易上手");
        }
        if (size > 24) {
            needs.add("牌组偏厚，普通牌要更挑剔");
        }
        if (curses > 0) {
            needs.add("牌组里有诅咒，尽快在商店移除");
        }
        if (needs.isEmpty()) {
            needs.add("结构比较均衡，按当前流派补强");
        }
        return needs;
    }

    public String needLine(int act) {
        List<String> n = needs(act);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.size() && i < 2; i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(n.get(i));
        }
        return sb.toString();
    }
}
