package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Recommends buy / remove / skip for the merchant. */
public final class ShopAdvisor {
    public static final class Item {
        public String name;
        public int price;
        public double priority;
        public String note;
        public AbstractCard card;
        public boolean purge;
    }

    public static final class Result {
        public String title;
        public String kicker = "商店 · 买、删还是走";
        public String compact;
        public List<String> lines = new ArrayList<String>();
        public List<Item> items = new ArrayList<Item>();
        public Item best;
        public boolean skip;
        public AbstractCard bestCard;
        public String purgeTarget;
    }

    private final CardRewardAdvisor cardAdvisor = new CardRewardAdvisor();
    private static Field relicsField;
    private static Field potionsField;

    public Result analyze(AbstractPlayer p, ShopScreen shop) {
        if (p == null || shop == null || !shop.isActive) {
            return null;
        }
        Result r = new Result();
        int gold = p.gold;
        addCards(r, p, shop, gold);
        addRelics(r, p, shop, gold);
        addPurge(r, p, shop, gold);

        Collections.sort(r.items, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                return Double.compare(b.priority, a.priority);
            }
        });
        if (!r.items.isEmpty()) {
            r.best = r.items.get(0);
            if (r.best.card != null) {
                r.bestCard = r.best.card;
            }
        }
        // 55 = roughly "worth spending gold now"; below that, keep the money.
        r.skip = r.best == null || r.best.priority < 55.0 || r.best.price > gold;
        if (r.purgeTarget != null && !r.skip && r.best != null && r.best.purge) {
            r.best.name = "删牌";
        }

        if (r.skip) {
            r.title = "这间先跳过，攒钱";
            r.compact = "跳过商店";
            r.lines.add(String.format("金币 %d：现在没有价格合适、又能补短板的货。", gold));
            r.lines.add("跳过直接前进，把钱留给后面的商店/删牌；不要为了花完而买普通牌。");
            if (!r.purgeTarget.isEmpty()) {
                r.lines.add("以后能删牌时优先删【" + r.purgeTarget + "】。");
            }
            if (!r.items.isEmpty()) {
                r.lines.add("这些我为什么没让你买：");
                for (int i = 0; i < r.items.size() && i < 4; i++) {
                    Item it = r.items.get(i);
                    r.lines.add(String.format("【%s】%d 金 · 推荐度 %.0f", it.name, it.price, it.priority));
                }
            }
        } else {
            if (r.best.purge) {
                r.title = "买删牌服务";
                r.compact = "删 " + r.purgeTarget;
                r.lines.add(String.format("大肥鱼建议：花 %d 金删【%s】（剩 %d 金）。",
                        r.best.price, r.purgeTarget, gold - r.best.price));
                r.lines.add("为什么呢：" + r.best.note);
            } else {
                r.title = "买【" + r.best.name + "】";
                r.compact = "买 " + r.best.name;
                r.lines.add(String.format("大肥鱼建议：买【%s】，%d 金（剩 %d 金）。",
                        r.best.name, r.best.price, gold - r.best.price));
                r.lines.add("为什么呢：" + r.best.note);
            }
            r.lines.add("店里这些东西，我的排序是：");
            for (int i = 0; i < r.items.size() && i < 5; i++) {
                Item it = r.items.get(i);
                r.lines.add(String.format("%s【%s】%d 金 · 推荐度 %.0f%s",
                        i == 0 ? "首选" : "候选", it.name, it.price, it.priority,
                        it.price <= gold - r.best.price ? "（买完首选后仍买得起）" : ""));
            }
            r.lines.add("商店大肥鱼的想法：优先核心牌/关键遗物，其次删诅咒或基础牌；药水最后考虑，"
                    + "别为了清空金币买泛用牌。买完首选后剩下的钱，留给后续商店或删牌更稳。");
        }
        return r;
    }

    private void addCards(Result r, AbstractPlayer p, ShopScreen shop, int gold) {
        addCardList(r, p, shop.coloredCards, gold);
        addCardList(r, p, shop.colorlessCards, gold);
    }

    private void addCardList(Result r, AbstractPlayer p, List<AbstractCard> cards, int gold) {
        if (cards == null) {
            return;
        }
        for (AbstractCard c : cards) {
            if (c == null || c.price <= 0 || c.price > gold) {
                continue;
            }
            double cardScore = cardAdvisor.scoreForShop(p, c);
            if (cardScore <= 0.5) {
                continue;
            }
            Item i = new Item();
            i.card = c;
            i.name = CardDatabase.shortName(c);
            i.price = c.price;
            i.priority = cardScore * 10.0 - c.price * 0.12;
            i.note = CardDatabase.note(c);
            r.items.add(i);
        }
    }

    private void addRelics(Result r, AbstractPlayer p, ShopScreen shop, int gold) {
        for (StoreRelic sr : relics(shop)) {
            if (sr == null || sr.isPurchased || sr.relic == null || sr.price <= 0 || sr.price > gold) {
                continue;
            }
            Item i = new Item();
            double community = RelicDatabase.score(sr.relic);
            String communityTier = RelicDatabase.tier(sr.relic);
            i.name = sr.relic.name;
            i.price = sr.price;
            double fit = RelicAdvisor.fitScore(p, sr.relic);
            String fitNote = RelicAdvisor.fitNote(p, sr.relic);
            i.priority = (community > 0 ? community : relicValue(sr.relic)) * 10.0
                    + fit * 5.0 - sr.price * 0.12;
            i.note = "遗物是永久提升；" + tierText(sr.relic.tier)
                    + (communityTier.length() > 0 ? "；玩家 Tier " + communityTier : "")
                    + (fitNote.length() > 0 ? "；" + fitNote : "") + "。";
            r.items.add(i);
        }
    }

    private void addPurge(Result r, AbstractPlayer p, ShopScreen shop, int gold) {
        r.purgeTarget = purgeTarget(p);
        if (!shop.purgeAvailable || r.purgeTarget.length() == 0) {
            return;
        }
        int cost = Math.max(1, ShopScreen.actualPurgeCost);
        if (cost > gold) {
            return;
        }
        Item i = new Item();
        i.purge = true;
        i.name = "删牌";
        i.price = cost;
        i.priority = 90.0 - cost * 0.12;
        i.note = "删掉【" + r.purgeTarget + "】，提高关键牌上手率；优先诅咒/状态/基础牌。";
        r.items.add(i);
    }

    private String purgeTarget(AbstractPlayer p) {
        AbstractCard worst = null;
        double worstScore = -999;
        for (AbstractCard c : p.masterDeck.group) {
            if (c == null) {
                continue;
            }
            double s;
            if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
                s = 100;
            } else if (CardDatabase.norm(c.cardID).contains("strike")
                    || CardDatabase.norm(c.cardID).contains("defend")) {
                s = 80;
            } else {
                CardDatabase.Entry e = CardDatabase.get(c);
                s = 30 - (e == null ? 5 : e.score);
            }
            if (s > worstScore) {
                worstScore = s;
                worst = c;
            }
        }
        return worst == null ? "" : CardDatabase.shortName(worst);
    }

    private double relicValue(AbstractRelic relic) {
        if (relic.tier == null) {
            return 5.0;
        }
        switch (relic.tier) {
            case BOSS:
                return 9.4;
            case SHOP:
                return 8.2;
            case RARE:
                return 7.5;
            case UNCOMMON:
                return 6.8;
            case COMMON:
                return 6.0;
            case STARTER:
                return 4.0;
            default:
                return 5.0;
        }
    }

    private String tierText(AbstractRelic.RelicTier tier) {
        if (tier == null) {
            return "遗物";
        }
        switch (tier) {
            case BOSS:
                return "Boss 遗物";
            case SHOP:
                return "商店遗物";
            case RARE:
                return "稀有遗物";
            case UNCOMMON:
                return "罕见遗物";
            case COMMON:
                return "普通遗物";
            default:
                return "遗物";
        }
    }

    @SuppressWarnings("unchecked")
    private List<StoreRelic> relics(ShopScreen shop) {
        try {
            if (relicsField == null) {
                relicsField = ShopScreen.class.getDeclaredField("relics");
                relicsField.setAccessible(true);
            }
            Object v = relicsField.get(shop);
            if (v instanceof List) {
                return (List<StoreRelic>) v;
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<StoreRelic>();
    }

    @SuppressWarnings("unchecked")
    private List<StorePotion> potions(ShopScreen shop) {
        try {
            if (potionsField == null) {
                potionsField = ShopScreen.class.getDeclaredField("potions");
                potionsField.setAccessible(true);
            }
            Object v = potionsField.get(shop);
            if (v instanceof List) {
                return (List<StorePotion>) v;
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<StorePotion>();
    }
}
