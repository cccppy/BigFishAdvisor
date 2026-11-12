package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One place for "is this curse actually a problem?".
 *
 * The game has a few curses that shops/campfires must never offer to remove:
 * Ascender's Bane, Necronomicurse, and Curse of the Bell.  More importantly, a
 * curse is not worth taking just because an event gives +max HP or a relic; it
 * is only reasonable when the deck can remove/exhaust it or actively profits
 * from it.
 */
public final class CurseAdvisor {
    private CurseAdvisor() {
    }

    public static boolean isUnremovable(AbstractCard c) {
        if (c == null) {
            return false;
        }
        // CardGroup.getPurgeableCards() excludes exactly these three IDs in
        // the game build.  Checking only Ascender's Bane still let the shop
        // point at Necronomicurse / Curse of the Bell, which cannot be
        // removed either.  Keep this list in sync with the game's filter.
        String id = CardDatabase.norm(c.cardID);
        return id.equals("ascendersbane")
                || id.equals("necronomicurse")
                || id.equals("curseofthebell");
    }

    /** Cards the game will actually let the player purge, in deck order. */
    public static List<AbstractCard> purgeable(AbstractPlayer p) {
        List<AbstractCard> out = new ArrayList<AbstractCard>();
        if (p == null || p.masterDeck == null) {
            return out;
        }
        for (AbstractCard c : p.masterDeck.group) {
            if (c != null && !isUnremovable(c)) {
                out.add(c);
            }
        }
        return out;
    }

    public static boolean hasCurseText(String text) {
        if (text == null) {
            return false;
        }
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("诅咒") || t.contains("curse");
    }

    /**
     * True only when an option ADDS a curse.  A blanket "contains 诅咒" check
     * also matched "移除一张诅咒" and made a beneficial removal look like a
     * curse bargain.  Match the add verb near the curse instead of excluding
     * any option that happens to contain "移除" (some events both remove a
     * card and add a curse).
     */
    public static boolean givesCurse(String text) {
        if (text == null) {
            return false;
        }
        String t = text.toLowerCase(Locale.ROOT);
        if (!(t.contains("诅咒") || t.contains("curse"))) {
            return false;
        }
        boolean add = t.matches("(?s).*(获得|加入|放入|塞入|被|受到|变成|替换为)\\s*"
                + "(\\d+\\s*张|一张|两张|三张|所有)?\\s*(诅咒|curse).*")
                || t.matches("(?s).*将[^。；;]{0,8}诅咒[^。；;]{0,8}(加入|放入|塞入).*")
                || (t.contains("curse") && (t.contains("add") || t.contains("shuffle")
                || t.contains("obtain")));
        return add;
    }

    public static boolean hasOmamori(AbstractPlayer p) {
        AbstractRelic r = p == null ? null : p.getRelic("Omamori");
        return r != null && r.counter > 0;
    }

    public static boolean hasRemoval(AbstractPlayer p) {
        if (p == null) {
            return false;
        }
        // Peace Pipe: guaranteed campfire removal.  Blue Candle: exhaust a
        // curse in combat, which is often enough to survive the fight.
        if (p.hasRelic("Peace Pipe") || p.hasRelic("Blue Candle")) {
            return true;
        }
        // A few cards can exhaust a curse (or any non-attack) in combat.
        String[] ids = {"True Grit", "Burning Pact", "Second Wind", "Fiend Fire", "Purity"};
        for (String id : ids) {
            if (hasCard(p, id)) {
                return true;
            }
        }
        return DeckSummary.of(p).tag("exhaust") > 0;
    }

    public static boolean hasPayoff(AbstractPlayer p) {
        if (p == null) {
            return false;
        }
        return p.hasRelic("Du-Vu Doll")
                || p.hasRelic("Darkstone Periapt")
                || p.hasRelic("Blue Candle");
    }

    public static boolean canHandle(AbstractPlayer p) {
        return hasOmamori(p) || hasRemoval(p) || hasPayoff(p);
    }

    /**
     * Event cost of accepting one more curse.  A curse is a real deck slot,
     * not a small "-2 HP" line item; when the deck has no answer it should be
     * close to a hard no.
     */
    public static double eventPenalty(AbstractPlayer p) {
        if (hasOmamori(p)) {
            return 0.4;
        }
        int existing = p == null || p.masterDeck == null ? 0 : DeckSummary.of(p).curses;
        double base = canHandle(p) ? 1.4 : 3.8;
        if (existing >= 2) {
            base += 0.6;
        }
        return base;
    }

    /** Short, player-facing explanation used by event notes. */
    public static String advice(AbstractPlayer p) {
        if (hasOmamori(p)) {
            return "御守还能挡一次诅咒，这次代价小一些；但别把护符当成无限解咒。";
        }
        if (hasPayoff(p)) {
            return "你有诅咒收益/消耗手段，拿一张还能处理；但诅咒会卡手，不要多拿。";
        }
        if (hasRemoval(p)) {
            return "你有和平烟斗/蓝蜡烛或消耗牌，之后能处理；但没处理前它仍会卡手。";
        }
        return "你目前没有稳定解咒手段：别为了回血、最大生命或一个遗物硬吃诅咒；"
                + "多一张诅咒会直接降低关键牌上手率，Boss 前尤其致命。";
    }

    public static String deckLine(AbstractPlayer p) {
        if (p == null || p.masterDeck == null) {
            return "";
        }
        int curses = 0;
        int ascender = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c != null && c.type == AbstractCard.CardType.CURSE) {
                curses++;
                if (isUnremovable(c)) {
                    ascender++;
                }
            }
        }
        if (curses == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder("牌组里已有 ").append(curses).append(" 张诅咒");
        if (ascender > 0) {
            b.append("（其中进阶之灾不能删，别在商店浪费钱）");
        }
        b.append("；").append(advice(p));
        return b.toString();
    }

    private static boolean hasCard(AbstractPlayer p, String idPart) {
        if (p == null || p.masterDeck == null) {
            return false;
        }
        String needle = CardDatabase.norm(idPart);
        for (AbstractCard c : p.masterDeck.group) {
            if (c != null && CardDatabase.norm(c.cardID).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
