package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.rewards.RewardItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Advice for the moment when every potion slot is full and a combat reward
 * offers another potion.  The base game has no replacement dialog: claiming
 * the reward with a full belt simply discards the new potion, so the useful
 * advice is which old potion to drop (or drink) before claiming.
 */
public final class PotionRewardAdvisor {
    private PotionRewardAdvisor() {
    }

    public static final class Result {
        public final AbstractPotion incoming;
        public final RewardItem reward;
        public final List<AbstractPotion> current;
        public final AbstractPotion drop;
        public final boolean drinkInstead;
        public final boolean skip;
        public final String title;
        public final String compact;
        public final List<String> lines;

        private Result(AbstractPotion incoming, RewardItem reward, List<AbstractPotion> current,
                       AbstractPotion drop, boolean drinkInstead, boolean skip,
                       String title, String compact, List<String> lines) {
            this.incoming = incoming;
            this.reward = reward;
            this.current = current;
            this.drop = drop;
            this.drinkInstead = drinkInstead;
            this.skip = skip;
            this.title = title;
            this.compact = compact;
            this.lines = lines;
        }
    }

    /** Null unless we are on a combat-reward screen, full, and a potion is offered. */
    public static Result find(AbstractPlayer p) {
        if (p == null || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.COMBAT_REWARD
                || AbstractDungeon.combatRewardScreen == null
                || AbstractDungeon.combatRewardScreen.rewards == null) {
            return null;
        }
        if (freeSlots(p) > 0) {
            return null;
        }
        RewardItem reward = null;
        for (RewardItem r : AbstractDungeon.combatRewardScreen.rewards) {
            if (r != null && !r.isDone && r.type == RewardItem.RewardType.POTION && r.potion != null) {
                reward = r;
                break;
            }
        }
        if (reward == null) {
            return null;
        }
        List<AbstractPotion> current = potions(p);
        if (current.isEmpty()) {
            return null;
        }
        return analyze(p, reward, current);
    }

    private static Result analyze(AbstractPlayer p, RewardItem reward, List<AbstractPotion> current) {
        AbstractPotion incoming = reward.potion;
        Context c = new Context(p, current);
        List<String> lines = new ArrayList<String>();
        String newName = name(incoming);
        String newKind = kind(incoming);
        String newEffect = effect(incoming, c);

        lines.add("奖励里是【" + newName + "】：" + newEffect + "。");
        lines.add("你的药水格已经满了；原版在满格时点领取，新药水会直接消失，不会问你要不要替换。");
        lines.add("现在带着的是：");
        for (AbstractPotion po : current) {
            lines.add("【" + name(po) + "】：" + kind(po) + "，" + effect(po, c) + "。");
        }

        if (p.hasRelic("Sozu")) {
            lines.add("你有【添水】：药水奖励本来就不能收进来，别为了它丢任何旧药水，直接继续就行。");
            return new Result(incoming, reward, current, null, false, true,
                    "药水满了，但你有添水", "别丢旧药水，添水收不了新药水", lines);
        }

        double newScore = score(incoming, c) - 0.45 * sameKindCount(current, newKind);
        if (samePotionCount(current, incoming) > 0) {
            newScore -= 0.7;
        }

        AbstractPotion worst = null;
        double worstScore = Double.MAX_VALUE;
        for (AbstractPotion po : current) {
            if (isFairy(po)) {
                continue;
            }
            double s = scoreCurrent(po, c);
            if (s < worstScore) {
                worstScore = s;
                worst = po;
            }
        }
        if (worst == null) {
            // Three copies of Fairy is almost impossible, but never tell the
            // player to throw away the only safety net.
            lines.add("三瓶都是复活药水，这种组合太珍贵了；这瓶新药水建议放弃，别拆掉保命底牌。");
            return new Result(incoming, reward, current, null, false, true,
                    "药水满了，建议放弃【" + newName + "】", "不拿【" + newName + "】，保住复活药水", lines);
        }

        boolean newIsFairy = isFairy(incoming);
        boolean hasFairy = hasFairy(current);
        boolean clearlyBetter = newScore > worstScore + 0.55;
        boolean clearlyWorse = newScore < worstScore - 0.35;
        boolean fillsGap = sameKindCount(current, newKind) == 0 && newScore >= worstScore - 0.1;

        if (newIsFairy && !hasFairy) {
            clearlyBetter = true;
            clearlyWorse = false;
        }
        if (newIsFairy && hasFairy) {
            // One Fairy is a safety net; a second one is usually not worth
            // throwing away a real combat potion.
            clearlyBetter = false;
            clearlyWorse = true;
        }
        if (hasFairy && !newIsFairy && sameKindCount(current, "复活") > 0) {
            // Still allowed to replace a non-fairy potion when the new one is
            // much stronger; otherwise keep the existing safety net.
            if (!clearlyBetter) {
                clearlyWorse = true;
            }
        }

        if (clearlyWorse) {
            lines.add("我的建议：这瓶【" + newName + "】先放弃。它和【" + name(worst)
                    + "】比没有明显更值得占一个格子；别为了它拆掉现有组合。");
            lines.add("操作：直接点继续/跳过这个奖励，不要点领取；不领取不会损失原来的药水。");
            return new Result(incoming, reward, current, null, false, true,
                    "药水满了，建议放弃【" + newName + "】",
                    "不拿【" + newName + "】，留【" + name(worst) + "】", lines);
        }

        if (clearlyBetter || fillsGap) {
            boolean drink = isFruitJuice(worst);
            if (drink) {
                lines.add("我的建议：拿【" + newName + "】。最弱的是【" + name(worst)
                        + "】，但它是果汁，别丢；先把它喝掉（永久加最大生命），空出格子后再领新药水。");
                lines.add("操作：点开【" + name(worst) + "】喝掉，再点奖励领取【" + newName + "】。");
            } else {
                lines.add("我的建议：拿【" + newName + "】，丢【" + name(worst) + "】。"
                        + whyDrop(worst, c) + " 新药水更补你现在的短板。");
                lines.add("操作：先点开【" + name(worst) + "】选择丢弃，再点奖励领取【" + newName
                        + "】；顺序反了，新药水会直接消失。");
            }
            return new Result(incoming, reward, current, worst, drink, false,
                    "药水满了：拿【" + newName + "】",
                    (drink ? "先喝【" + name(worst) + "】腾格，再拿【" + newName + "】"
                            : "拿【" + newName + "】，丢【" + name(worst) + "】"), lines);
        }

        lines.add("我的建议：看情况。新药水和【" + name(worst) + "】各有用途，差距不大。");
        lines.add("如果马上要打精英/Boss，或你现在缺【" + newKind + "】，就拿【" + newName
                + "】、丢【" + name(worst) + "】；如果接下来只是普通战斗，保留现在的三瓶更稳。");
        lines.add("操作：先点开【" + name(worst) + "】丢弃，再点领取；不准备换就跳过奖励。");
        return new Result(incoming, reward, current, worst, false, false,
                "药水满了：看情况换不换", "看情况：拿【" + newName + "】就丢【" + name(worst) + "】", lines);
    }

    private static final class Context {
        final AbstractPlayer p;
        final DeckSummary deck;
        final int hpPct;
        final int act;
        final int highCost;
        final List<AbstractPotion> current;

        Context(AbstractPlayer p, List<AbstractPotion> current) {
            this.p = p;
            this.current = current;
            this.deck = DeckSummary.of(p);
            this.hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
            this.act = AbstractDungeon.actNum;
            int high = 0;
            if (p.masterDeck != null) {
                for (AbstractCard card : p.masterDeck.group) {
                    if (card != null && CostUtil.printed(card) >= 2) {
                        high++;
                    }
                }
            }
            this.highCost = high;
        }
    }

    private static double score(AbstractPotion po, Context c) {
        String k = kind(po);
        double s = rarity(po);
        if ("复活".equals(k)) {
            return 6.0 + (c.act >= 3 ? 1.0 : 0.0);
        }
        if ("永久生命".equals(k)) {
            return 1.7 + (c.hpPct < 60 ? 0.5 : 0.0);
        }
        if ("治疗".equals(k)) {
            return 1.4 + (c.hpPct < 55 ? 1.3 : 0.0);
        }
        if ("防御".equals(k)) {
            return 1.9 + (c.deck.blockCards < 6 ? 0.8 : 0.0) + (c.hpPct < 50 ? 0.3 : 0.0);
        }
        if ("能量".equals(k)) {
            return 2.1 + (c.highCost >= 6 ? 0.4 : 0.0);
        }
        if ("力量".equals(k)) {
            return 1.8 + (c.deck.attacks >= 12 ? 0.6 : 0.0);
        }
        if ("敏捷".equals(k)) {
            return 1.8 + (c.deck.blockCards < 6 ? 0.5 : 0.0);
        }
        if ("复制".equals(k)) {
            return 2.0 + (c.deck.attacks >= 10 ? 0.4 : 0.0);
        }
        if ("抽牌".equals(k)) {
            return 1.7 + (c.deck.size < 18 ? 0.2 : 0.0);
        }
        if ("群体攻击".equals(k)) {
            return 1.8 + (c.deck.aoeCards < 2 ? 0.5 : 0.0);
        }
        if ("攻击".equals(k)) {
            return 1.6 + (c.deck.attacks < 9 ? 0.4 : 0.0);
        }
        if ("球".equals(k)) {
            return isDefect(c.p) ? 2.3 : 0.7;
        }
        if ("姿态".equals(k)) {
            return isWatcher(c.p) ? 2.1 : 0.7;
        }
        if ("奇迹".equals(k)) {
            return isWatcher(c.p) ? 2.0 : 0.7;
        }
        if ("毒".equals(k)) {
            return isSilent(c.p) ? 2.0 : 1.0;
        }
        if ("小刀".equals(k)) {
            return isSilent(c.p) ? 1.9 : 0.9;
        }
        if ("逃跑".equals(k)) {
            return c.hpPct < 30 ? 2.1 : 1.0;
        }
        if ("回收".equals(k)) {
            return 1.5 + (c.highCost >= 6 ? 0.4 : 0.0);
        }
        if ("补药".equals(k)) {
            return 0.7;
        }
        return 1.0;
    }

    private static double scoreCurrent(AbstractPotion po, Context c) {
        double s = score(po, c);
        int same = sameKindCount(c.current, kind(po));
        if (same >= 2) {
            s -= 0.45;
        }
        if (samePotionCount(c.current, po) > 1) {
            s -= 0.8;
        }
        return s;
    }

    private static String whyDrop(AbstractPotion po, Context c) {
        String k = kind(po);
        if ("补药".equals(k)) {
            return "满格时它没有空位可补，等于白占一格。";
        }
        if ("逃跑".equals(k)) {
            return "你现在不是残血逃命，烟雾弹可以以后再找。";
        }
        if ("攻击".equals(k) || "群体攻击".equals(k)) {
            return "你的输出牌已经够用，少这一瓶爆发不至于打不动。";
        }
        if ("防御".equals(k)) {
            return "你的格挡牌不算少，这瓶防御不是唯一保命手段。";
        }
        if ("抽牌".equals(k)) {
            return "抽牌药是顺风牌，逆风时不如保命/能量稳定。";
        }
        return "它在你现在的牌组里替代品最多。";
    }

    private static String effect(AbstractPotion po, Context c) {
        String k = kind(po);
        String s = simple(po);
        if ("FairyPotion".equals(s)) return "死亡时自动复活，并回复部分生命";
        if ("FruitJuice".equals(s)) return "永久增加最大生命，越早喝越好";
        if ("BloodPotion".equals(s)) return "回复大量生命";
        if ("RegenPotion".equals(s)) return "获得持续数回合的回血";
        if ("EnergyPotion".equals(s)) return "立刻获得 2 点能量";
        if ("BlockPotion".equals(s)) return "获得 12 点格挡";
        if ("GhostInAJar".equals(s)) return "获得 1 层无实体，几乎免疫一回合";
        if ("HeartOfIron".equals(s)) return "获得多层金属化，持续整场战斗";
        if ("LiquidBronze".equals(s)) return "获得多层荆棘，反弹攻击伤害";
        if ("SpeedPotion".equals(s)) return "本回合获得大量敏捷，适合先喝再叠甲";
        if ("DexterityPotion".equals(s)) return "获得永久敏捷，格挡牌越打越厚";
        if ("WeakenPotion".equals(s) || "FearPotion".equals(s)) return "给敌人上虚弱，降低它这回合的输出";
        if ("StrengthPotion".equals(s)) return "获得永久力量，多段攻击收益最高";
        if ("SteroidPotion".equals(s)) return "本回合获得临时力量，先喝再打攻击";
        if ("DuplicationPotion".equals(s)) return "下一张牌打出两次，留给关键攻击或能力";
        if ("FirePotion".equals(s)) return "造成 20 点单体伤害";
        if ("ExplosivePotion".equals(s)) return "对所有敌人造成 10 点伤害";
        if ("PoisonPotion".equals(s)) return "给目标上 6 层毒，毒伤无视格挡";
        if ("CunningPotion".equals(s)) return "生成 3 张小刀，适合小刀/连击体系";
        if ("AttackPotion".equals(s)) return "随机生成一张攻击牌，补伤害";
        if ("PowerPotion".equals(s)) return "随机生成一张能力牌，补成长";
        if ("SkillPotion".equals(s)) return "随机生成一张技能牌，补防御/功能";
        if ("ColorlessPotion".equals(s)) return "随机生成一张无色牌，功能不稳定";
        if ("DistilledChaosPotion".equals(s)) return "自动打出抽牌堆顶 3 张牌，适合已铺好场";
        if ("EssenceOfSteel".equals(s)) return "获得 4 点覆甲，先挡多段攻击";
        if ("CultistPotion".equals(s)) return "获得仪式，长战斗越早喝越赚";
        if ("Ambrosia".equals(s)) return "直接进入神性，观者的爆发开关";
        if ("BottledMiracle".equals(s)) return "生成 2 张奇迹，补能量/连招";
        if ("StancePotion".equals(s)) return "进入平静，配合退出姿态打能量";
        if ("FocusPotion".equals(s)) return "获得集中，闪电/冰霜球都会变强";
        if ("PotionOfCapacity".equals(s)) return "增加充能球槽，球体系的关键空间";
        if ("EssenceOfDarkness".equals(s)) return "生成黑暗球，适合爆发/激发";
        if ("SwiftPotion".equals(s)) return "抽 3 张牌，找关键牌/连招";
        if ("GamblersBrew".equals(s)) return "弃掉不要的牌再抽同样数量，过滤手牌";
        if ("SneckoOil".equals(s)) return "随机化手牌费用并抽牌，高费牌组可赌一把";
        if ("LiquidMemories".equals(s)) return "从弃牌堆回收一张牌，拿回关键牌";
        if ("SmokeBomb".equals(s)) return "立刻逃离普通战斗；Boss 战无效";
        if ("Elixir".equals(s)) return "消耗手牌，消耗体系里能触发配合";
        if ("BlessingOfTheForge".equals(s)) return "本场战斗升级所有牌，长战斗很强";
        if ("AncientPotion".equals(s)) return "获得 1 层人工制品，挡一次负面状态";
        if ("EntropicBrew".equals(s)) return "补满空药水格；满格时几乎没用";
        String d = clean(po.description);
        return d.length() == 0 ? "按药水说明使用" : d;
    }

    private static String kind(AbstractPotion po) {
        String s = simple(po);
        if ("FairyPotion".equals(s)) return "复活";
        if ("FruitJuice".equals(s)) return "永久生命";
        if ("BloodPotion".equals(s) || "RegenPotion".equals(s)) return "治疗";
        if ("BlockPotion".equals(s) || "GhostInAJar".equals(s) || "HeartOfIron".equals(s)
                || "LiquidBronze".equals(s) || "SpeedPotion".equals(s)
                || "DexterityPotion".equals(s) || "WeakenPotion".equals(s)
                || "FearPotion".equals(s) || "EssenceOfSteel".equals(s)
                || "AncientPotion".equals(s)) return "防御";
        if ("EnergyPotion".equals(s)) return "能量";
        if ("StrengthPotion".equals(s) || "SteroidPotion".equals(s)
                || "CultistPotion".equals(s)) return "力量";
        if ("DuplicationPotion".equals(s)) return "复制";
        if ("SwiftPotion".equals(s) || "GamblersBrew".equals(s)
                || "SneckoOil".equals(s)) return "抽牌";
        if ("FirePotion".equals(s) || "AttackPotion".equals(s)
                || "DistilledChaosPotion".equals(s)) return "攻击";
        if ("ExplosivePotion".equals(s)) return "群体攻击";
        if ("PoisonPotion".equals(s)) return "毒";
        if ("CunningPotion".equals(s)) return "小刀";
        if ("FocusPotion".equals(s) || "PotionOfCapacity".equals(s)
                || "EssenceOfDarkness".equals(s)) return "球";
        if ("StancePotion".equals(s) || "Ambrosia".equals(s)) return "姿态";
        if ("BottledMiracle".equals(s)) return "奇迹";
        if ("LiquidMemories".equals(s)) return "回收";
        if ("SmokeBomb".equals(s)) return "逃跑";
        if ("EntropicBrew".equals(s)) return "补药";
        return "功能";
    }

    private static boolean isDefect(AbstractPlayer p) {
        return p != null && p.chosenClass == com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass.DEFECT;
    }

    private static boolean isWatcher(AbstractPlayer p) {
        return p != null && p.chosenClass == com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass.WATCHER;
    }

    private static boolean isSilent(AbstractPlayer p) {
        return p != null && p.chosenClass == com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass.THE_SILENT;
    }

    private static boolean isFairy(AbstractPotion po) {
        return "FairyPotion".equals(simple(po));
    }

    private static boolean isFruitJuice(AbstractPotion po) {
        return "FruitJuice".equals(simple(po));
    }

    private static boolean hasFairy(List<AbstractPotion> potions) {
        for (AbstractPotion po : potions) {
            if (isFairy(po)) {
                return true;
            }
        }
        return false;
    }

    private static int sameKindCount(List<AbstractPotion> potions, String kind) {
        int n = 0;
        for (AbstractPotion po : potions) {
            if (kind.equals(kind(po))) {
                n++;
            }
        }
        return n;
    }

    private static int samePotionCount(List<AbstractPotion> potions, AbstractPotion other) {
        String id = simple(other);
        int n = 0;
        for (AbstractPotion po : potions) {
            if (id.equals(simple(po))) {
                n++;
            }
        }
        return n;
    }

    private static int freeSlots(AbstractPlayer p) {
        int free = 0;
        if (p.potions != null) {
            for (AbstractPotion po : p.potions) {
                if (isEmpty(po)) {
                    free++;
                }
            }
        }
        return free;
    }

    private static List<AbstractPotion> potions(AbstractPlayer p) {
        List<AbstractPotion> out = new ArrayList<AbstractPotion>();
        if (p != null && p.potions != null) {
            for (AbstractPotion po : p.potions) {
                if (!isEmpty(po)) {
                    out.add(po);
                }
            }
        }
        return out;
    }

    private static boolean isEmpty(AbstractPotion po) {
        if (po == null || po instanceof PotionSlot) {
            return true;
        }
        return po.ID != null && po.ID.toLowerCase(Locale.ROOT).contains("potion slot");
    }

    private static String simple(AbstractPotion po) {
        return po == null ? "" : po.getClass().getSimpleName();
    }

    private static String name(AbstractPotion po) {
        if (po == null) {
            return "?";
        }
        return po.name == null || po.name.length() == 0 ? simple(po) : po.name;
    }

    private static double rarity(AbstractPotion po) {
        if (po == null || po.rarity == null) {
            return 0.4;
        }
        String r = po.rarity.name();
        if ("RARE".equals(r)) {
            return 1.2;
        }
        if ("UNCOMMON".equals(r)) {
            return 0.8;
        }
        return 0.4;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        String s = text.replace("NL", "；");
        s = s.replaceAll("\\[[^\\]]*\\]", "");
        s = s.replace("#b", "").replace("#g", "").replace("#r", "")
                .replace("#y", "").replace("#p", "").replace("#9", "")
                .replace("＃9", "").replace("*", "").replace("%", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
