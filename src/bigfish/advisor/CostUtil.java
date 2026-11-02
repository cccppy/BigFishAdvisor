package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * One place for "what does this card cost right now?".
 *
 * <p>Combat must never use the printed/base cost.  Confusion (Snecko Eye,
 * Snecko Oil, the Snecko enemy) randomizes {@code cost} and
 * {@code costForTurn}; Corruption, Mummified Hand and Madness can change only
 * {@code costForTurn}.  The game itself checks {@code costForTurn} when it
 * decides whether a card can be played and when Necronomicon triggers, so the
 * advisor has to use the same number.</p>
 */
public final class CostUtil {
    private CostUtil() {
    }

    /** True for X-cost cards; they are not randomized by Confusion. */
    public static boolean isX(AbstractCard c) {
        return c != null && (c.cost == -1 || c.costForTurn == -1);
    }

    /**
     * Actual cost for the current turn.  Returns -1 for X-cost cards, and 0
     * for cards that are free to play.  Never falls back to {@code cost} when
     * {@code costForTurn} is a valid number: temporary discounts matter.
     */
    public static int current(AbstractCard c) {
        if (c == null) {
            return 0;
        }
        if (c.freeToPlay()) {
            return 0;
        }
        if (c.costForTurn == -1) {
            return -1;
        }
        if (c.costForTurn < 0) {
            return 0;
        }
        return Math.max(0, c.costForTurn);
    }

    public static boolean canPay(AbstractCard c, int energy) {
        if (c == null) {
            return false;
        }
        int cost = current(c);
        return cost < 0 || cost <= energy;
    }

    /**
     * Printed/base cost for deck-building screens (rewards, shop, campfire).
     * This is deliberately not {@link #current}: outside combat the transient
     * costForTurn is meaningless, and the user is choosing a card for future
     * fights, not playing it this turn.
     */
    public static int printed(AbstractCard c) {
        if (c == null) {
            return 0;
        }
        if (c.cost == -1) {
            return -1;
        }
        if (c.cost < 0) {
            return 0;
        }
        return Math.max(0, c.cost);
    }

    /** Snecko Eye or any source of Confusion. */
    public static boolean randomCost(AbstractPlayer p) {
        return p != null && (p.hasRelic("Snecko Eye") || p.hasPower("Confusion"));
    }

    /**
     * Expected-value adjustment for a card reward while Confusion is active.
     * A printed cost of 0/1/2/3 becomes uniformly 0/1/2/3 on draw, so high
     * cost cards get better and cheap cards get less reliable.  X-cost cards
     * are unaffected.
     */
    public static double sneckoPickBonus(AbstractCard c) {
        if (c == null || isX(c) || c.freeToPlay()) {
            return 0.0;
        }
        int base = printed(c);
        if (base < 0) {
            return 0.0;
        }
        double bonus = (base - 1.5) * 0.22;
        if (base == 0) {
            bonus -= 0.12;
        }
        return Math.max(-0.55, Math.min(0.55, bonus));
    }

    public static String sneckoPickNote(AbstractCard c) {
        if (c == null || isX(c)) {
            return "";
        }
        int base = printed(c);
        if (base == 0) {
            return "异蛇之眼会把这张 0 费牌随机成 0–3，0 费连招不再稳定";
        }
        if (base >= 2) {
            return "异蛇之眼会把费用随机成 0–3，高费核心反而更容易赚，不能按原费死算";
        }
        return "异蛇之眼会随机改费，这张低费牌也可能突然变贵";
    }
}
