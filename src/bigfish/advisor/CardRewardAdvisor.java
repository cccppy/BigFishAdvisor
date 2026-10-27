package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Scores a card reward against the current deck, act, and rarity. */
public final class CardRewardAdvisor {
    public static final class Scored {
        public AbstractCard card;
        public double score;
        public String note;
        public String tier;
        public int rank;
        public boolean skipRecommended;
        public boolean offPackage;
    }

    public static final class Result {
        public List<Scored> cards = new ArrayList<Scored>();
        public Scored top;
        public boolean skip;
        public String title;
        public String kicker;
        public List<String> lines = new ArrayList<String>();
        public DeckSummary deck;
        public Map<UUID, Scored> byId = new HashMap<UUID, Scored>();
    }

    public Result analyze(AbstractPlayer player, List<AbstractCard> offered, RewardItem item) {
        Result r = new Result();
        if (player == null || offered == null || offered.isEmpty()) {
            return null;
        }
        r.deck = DeckSummary.of(player);
        int act = Math.max(1, AbstractDungeon.actNum);
        int floor = AbstractDungeon.floorNum;

        for (AbstractCard card : offered) {
            if (card == null) {
                continue;
            }
            Scored s = new Scored();
            s.card = card;
            s.score = score(player, card, r.deck, act, floor);
            s.offPackage = isOffPackage(card, r.deck);
            s.note = CardDatabase.note(card);
            s.tier = CardDatabase.tier(card);
            r.cards.add(s);
            r.byId.put(card.uuid, s);
        }
        Collections.sort(r.cards, new Comparator<Scored>() {
            @Override
            public int compare(Scored a, Scored b) {
                return Double.compare(b.score, a.score);
            }
        });
        for (int i = 0; i < r.cards.size(); i++) {
            r.cards.get(i).rank = i + 1;
        }
        r.top = r.cards.isEmpty() ? null : r.cards.get(0);
        if (r.top == null) {
            return null;
        }

        boolean bowl = player.hasRelic("Singing Bowl");
        double skipThreshold = bowl ? 6.1 : 5.6;
        r.skip = item != null && r.top != null && r.top.score < skipThreshold
                && r.deck.size >= (bowl ? 16 : 14);
        if (r.top != null && r.top.offPackage && r.deck.size >= 18 && r.top.score < 7.0) {
            r.skip = true;
        }
        if (r.top != null) {
            r.top.skipRecommended = r.skip;
        }
        buildText(player, r, act, bowl);
        return r;
    }

    /** Used by the shop advisor so cards are judged with the same deck logic. */
    public double scoreForShop(AbstractPlayer p, AbstractCard c) {
        return score(p, c, DeckSummary.of(p), Math.max(1, AbstractDungeon.actNum),
                AbstractDungeon.floorNum);
    }

    private double score(AbstractPlayer p, AbstractCard c, DeckSummary deck, int act, int floor) {
        CardDatabase.Entry entry = CardDatabase.get(c);
        double score = entry == null ? genericScore(c) : entry.score;
        score += RelicAdvisor.cardBonus(p, c);
        score += ActAdvisor.cardBonus(p, c);
        score += keySynergy(p, c, deck, act);
        if (c.upgraded || c.timesUpgraded > 0) {
            score += 0.15;
        }
        if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
            return 0.0;
        }
        if (c.rarity == AbstractCard.CardRarity.UNCOMMON) {
            score += 0.12;
        } else if (c.rarity == AbstractCard.CardRarity.RARE) {
            score += 0.18;
        }

        if (deck.attacks < 4 && c.type == AbstractCard.CardType.ATTACK) {
            score += 0.55;
        }
        if (deck.blockCards < 4 && (c.block > 0 || c.baseBlock > 0 || CardDatabase.hasTag(c, "block"))) {
            score += 0.40;
        }
        if (act >= 2 && deck.aoeCards < 1 && CardDatabase.hasTag(c, "aoe")) {
            score += 0.65;
        }
        if (deck.drawCards < 1 && deck.size >= 12 && CardDatabase.hasTag(c, "draw")) {
            score += 0.35;
        }
        if (deck.size >= 20) {
            score -= 0.30;
        }
        if (deck.size >= 26) {
            score -= 0.45;
        }
        if (deck.size < 12 && (c.rarity == AbstractCard.CardRarity.COMMON
                || c.rarity == AbstractCard.CardRarity.UNCOMMON)) {
            score += 0.15;
        }

        int copies = 0;
        for (AbstractCard d : p.masterDeck.group) {
            if (d.cardID != null && d.cardID.equals(c.cardID)) {
                copies++;
            }
        }
        if (copies >= 1) {
            score -= Math.min(0.9, 0.45 + copies * 0.15);
        }

        if (c.type == AbstractCard.CardType.POWER && act == 1 && floor < 9) {
            score -= 0.35;
        }
        if (act == 3 && c.type == AbstractCard.CardType.ATTACK
                && !CardDatabase.hasTag(c, "str") && !CardDatabase.hasTag(c, "block")
                && c.rarity != AbstractCard.CardRarity.RARE) {
            score -= 0.25;
        }
        if (c.costForTurn >= 3 && act == 1) {
            score -= 0.20;
        }
        if (c.costForTurn <= 1) {
            score += 0.08;
        }
        if (CardDatabase.hasTag(c, "selfdmg") && p.currentHealth * 100 / Math.max(1, p.maxHealth) < 45) {
            score -= 0.7;
        }
        if (CardDatabase.hasTag(c, "status")) {
            score -= 0.8;
        }

        int synergy = 0;
        for (String tag : CardDatabase.tags(c)) {
            if (tag.equals("aoe") || tag.equals("draw") || tag.equals("block")) {
                continue;
            }
            if (deck.tag(tag) >= 3) {
                synergy++;
            }
        }
        score += Math.min(1.0, synergy * 0.25);

        // Community archetype logic: reward cards that fit the deck's actual plan,
        // and avoid mixing mutually exclusive Silent/Defect packages.
        String arch = deck.archetypeKey();
        String secondary = deck.secondaryKey();
        int primaryCount = arch.length() == 0 ? 0 : deck.tag(arch);
        boolean matchesPackage = arch.length() == 0
                || CardDatabase.hasTag(c, arch)
                || (secondary.length() > 0 && CardDatabase.hasTag(c, secondary));
        if (arch.length() > 0 && matchesPackage) {
            score += 0.45;
        }
        if (primaryCount >= 5 && !matchesPackage) {
            double quality = entry == null ? 5.0 : entry.score;
            // A truly premium card can be worth splashing; a mediocre one just
            // dilutes the package. This is the "别什么都拿" rule.
            score -= quality >= 8.8 ? 0.35 : 1.05;
        }
        if (primaryCount >= 6 && !matchesPackage) {
            score -= 0.30;
        }
        if (deck.character.contains("SILENT")) {
            if (deck.tag("poison") >= 3 && CardDatabase.hasTag(c, "shiv")) {
                score -= 0.45;
            }
            if (deck.tag("shiv") >= 3 && CardDatabase.hasTag(c, "poison")) {
                score -= 0.35;
            }
        }
        if (deck.character.contains("DEFECT")) {
            if (deck.tag("focus") >= 3 && CardDatabase.hasTag(c, "zero")) {
                score -= 0.25;
            }
        }
        if (act == 1 && floor <= 6 && c.type == AbstractCard.CardType.ATTACK) {
            score += 0.30;
        }
        if (act == 1 && floor <= 8 && CardDatabase.norm(c.cardID).contains("bash")) {
            score += 0.40;
        }
        return Math.max(0.0, Math.min(10.0, score));
    }

    /**
     * Context-defining cards can be much better than their global tier in the
     * right shell: Barricade + Body Slam, Corruption + Dark Embrace, etc.
     */
    private double keySynergy(AbstractPlayer p, AbstractCard c, DeckSummary deck, int act) {
        String id = CardDatabase.norm(c.cardID);
        double b = 0.0;
        if (id.contains("barricade")) {
            if (deck.blockCards >= 5) {
                b += 2.6;
            }
            if (hasCard(p, "Body Slam") || hasCard(p, "Entrench")) {
                b += 0.8;
            }
            if (act >= 2 && deck.blockCards < 7) {
                b += 0.7;
            }
        } else if (id.contains("corruption")) {
            if (deck.skills >= 8) {
                b += 1.6;
            }
            if (deck.tag("exhaust") >= 2 || hasCard(p, "Dark Embrace")) {
                b += 1.2;
            }
        } else if (id.contains("darkembrace")) {
            if (deck.tag("exhaust") >= 3) {
                b += 1.8;
            }
            if (hasCard(p, "Corruption")) {
                b += 1.0;
            }
        } else if (id.contains("feelnopain")) {
            if (deck.tag("exhaust") >= 3) {
                b += 2.0;
            }
            if (hasCard(p, "Corruption")) {
                b += 1.0;
            }
        } else if (id.contains("limitbreak")) {
            if (deck.tag("str") >= 2 || hasCard(p, "Inflame")) {
                b += 1.6;
            }
        } else if (id.contains("powerthrough")) {
            if (deck.blockCards < 7) {
                b += 1.1;
            }
            if (hasCard(p, "Feel No Pain") || hasCard(p, "Evolve")) {
                b += 0.8;
            }
        } else if (id.contains("impervious") || id.contains("flamebarrier")) {
            if (deck.blockCards < 7) {
                b += 0.7;
            }
        } else if (id.contains("bodyslam")) {
            if (deck.blockCards >= 6) {
                b += 1.0;
            }
        } else if (id.contains("entrench")) {
            if (hasCard(p, "Barricade")) {
                b += 1.5;
            }
        }
        if (c.exhaust && countCopies(p, c) >= 1) {
            b -= 0.8;
        }
        return b;
    }

    private String keyNote(AbstractPlayer p, AbstractCard c, DeckSummary deck) {
        String id = CardDatabase.norm(c.cardID);
        if (id.contains("barricade") && deck.blockCards >= 5) {
            return "你有格挡和 Body Slam，Barricade 能把防御变成持续输出";
        }
        if (id.contains("corruption") && deck.skills >= 8) {
            return "你技能牌多，Corruption 能把整回合费用压下来";
        }
        if (id.contains("darkembrace") && deck.tag("exhaust") >= 3) {
            return "你消耗牌多，Dark Embrace 能把消耗变成抽牌";
        }
        if (id.contains("feelnopain") && deck.tag("exhaust") >= 3) {
            return "你消耗牌多，Feel No Pain 能把消耗变成格挡";
        }
        if (id.contains("limitbreak") && (deck.tag("str") >= 2 || hasCard(p, "Inflame"))) {
            return "你已经有力量来源，Limit Break 能翻倍爆发";
        }
        if (id.contains("powerthrough") && deck.blockCards < 7) {
            return "第二幕多段伤害多，你需要更高格挡";
        }
        return "";
    }

    private boolean hasCard(AbstractPlayer p, String name) {
        for (AbstractCard c : p.masterDeck.group) {
            if (c.cardID != null && CardDatabase.norm(c.cardID).contains(CardDatabase.norm(name))) {
                return true;
            }
        }
        return false;
    }

    private int countCopies(AbstractPlayer p, AbstractCard card) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c.cardID != null && c.cardID.equals(card.cardID)) {
                n++;
            }
        }
        return n;
    }

    private boolean isOffPackage(AbstractCard c, DeckSummary deck) {
        String primary = deck.archetypeKey();
        if (primary.length() == 0 || deck.tag(primary) < 5) {
            return false;
        }
        String secondary = deck.secondaryKey();
        if (CardDatabase.hasTag(c, primary)
                || (secondary.length() > 0 && CardDatabase.hasTag(c, secondary))) {
            return false;
        }
        if (CardDatabase.hasTag(c, "block") && deck.blockCards < 5) {
            return false;
        }
        if (CardDatabase.hasTag(c, "draw") && deck.drawCards < 2) {
            return false;
        }
        return true;
    }

    private double genericScore(AbstractCard c) {
        if (c.type == AbstractCard.CardType.ATTACK) {
            double d = Math.max(0, c.baseDamage > 0 ? c.baseDamage : c.damage);
            return Math.min(7.4, 4.7 + d * 0.07 + (c.baseBlock > 0 ? 0.4 : 0));
        }
        if (c.type == AbstractCard.CardType.SKILL) {
            return Math.min(7.2, 4.8 + Math.max(0, c.baseBlock) * 0.05
                    + Math.max(0, c.baseDraw) * 0.35 + (c.cost <= 1 ? 0.2 : 0));
        }
        if (c.type == AbstractCard.CardType.POWER) {
            return 5.4;
        }
        return 3.0;
    }

    private void buildText(AbstractPlayer p, Result r, int act, boolean bowl) {
        r.kicker = String.format("选牌建议 · 第 %d 层 · 第 %d 幕", AbstractDungeon.floorNum, act);
        if (r.top == null) {
            r.title = "这组牌不用拿";
            r.lines.add("没有可评估的牌。");
            return;
        }
        if (r.skip) {
            r.title = "跳过，保持牌组精简";
        } else {
            r.title = "拿【" + CardDatabase.shortName(r.top.card) + "】";
        }
        r.lines.add(r.deck.countsLine());
        r.lines.add("当前需要：" + r.deck.needLine(act));
        r.lines.add(ActAdvisor.bossLine(p));
        r.lines.add(ActAdvisor.eliteLine(p));
        r.lines.add("你的牌组现在：" + (r.deck.archetypeKey().length() == 0
                ? "还没有明显主流派，先补通用攻防/过牌，别过早锁死流派。"
                : "已经偏【" + r.deck.archetype() + "】，优先拿同体系关键牌；"
                + "跨体系牌只有在质量明显更高时才考虑。"));
        String relicFit = RelicAdvisor.cardNote(p, r.top.card);
        if (relicFit.length() > 0) {
            r.lines.add("遗物配合：" + relicFit + "。");
        }
        String actFit = ActAdvisor.cardNote(p, r.top.card);
        if (actFit.length() > 0) {
            r.lines.add("针对本幕：" + actFit + "。");
        }
        String keyFit = keyNote(p, r.top.card, r.deck);
        if (keyFit.length() > 0) {
            r.lines.add("这套牌的关键：" + keyFit + "。");
        }
        r.lines.add("这三张牌，我一张张说：");
        for (Scored s : r.cards) {
            r.lines.add(String.format("【%s】%s评分 %.1f%s%s：%s",
                    CardDatabase.shortName(s.card),
                    s.tier == null || s.tier.length() == 0 ? "" : "Tier " + s.tier + " · ",
                    s.score,
                    s.card.upgraded || s.card.timesUpgraded > 0 ? " · 已升级" : "",
                    s == r.top ? " · 首选" : "",
                    s.note));
        }
        if (r.top.offPackage) {
            r.lines.add("这张和当前主流派不太搭：拿它会把牌组拉成杂派。"
                    + "除非它能立刻补命/补斩杀，或者质量明显高出一截，否则我建议跳过。");
        }
        if (r.skip) {
            r.lines.add(String.format("三张里最高只有 %.1f 分，都不解决当前问题；跳过能少一张废牌。", r.top.score));
            r.lines.add("最高分牌：" + r.top.note);
            if (bowl) {
                r.lines.add("你有【歌唱碗】：跳过还能 +2 最大生命，这次更值得跳。");
            } else {
                r.lines.add("记住：不是每次奖励都必须拿；牌组越精简，关键牌越容易抽到。");
            }
        } else {
            r.lines.add("我最想拿它的原因是：" + r.top.note);
            if (r.top.tier != null && r.top.tier.length() > 0) {
                r.lines.add("玩家评价：Tier " + r.top.tier + "（SpireSpy 玩家排行，已按当前牌组修正）");
            }
            r.lines.add("跳过判断：这张能补当前短板，不建议跳；除非牌组已经 25 张以上。");
            r.lines.add("大肥鱼的想法：" + strategyLine(p, r, act));
        }
        if (r.cards.size() >= 2) {
            Scored second = r.cards.get(1);
            r.lines.add(String.format("备选：%s（%.1f）· %s", CardDatabase.shortName(second.card),
                    second.score, second.note));
        }
    }

    private String strategyLine(AbstractPlayer p, Result r, int act) {
        if (r.top != null && r.top.score >= 8.2) {
            return "这是高质量核心，优先拿；之后围绕它补配合牌，而不是继续抓泛用牌。";
        }
        if (p.masterDeck.size() >= 24) {
            return "牌组已经偏厚，只拿能直接提升胜率的关键牌；普通牌开始拖累抽牌质量。";
        }
        if (act == 1) {
            return "第 1 层先保证低费攻击和防御的数量，不要过早堆高费能力；先能稳定打过精英/Boss。";
        }
        if (act == 2) {
            return "第 2 层开始补群体伤害、抽牌和成长；单张数值牌逐渐让位给体系配合。";
        }
        return "第 3 层只拿能补短板或直接配合核心的牌；不要为了稀有度破坏牌组结构。";
    }
}
