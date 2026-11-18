package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Relic-aware combat, reward and boss-relic advice. */
public final class RelicAdvisor {
    public static final class BossChoice {
        public AbstractRelic best;
        public double score;
        public List<String> lines = new ArrayList<String>();
        public List<Scored> choices = new ArrayList<Scored>();
    }

    public static final class Scored {
        public AbstractRelic relic;
        public double score;
        public String fit;
        public String downside;
    }

    private RelicAdvisor() {
    }

    // ---------------------------------------------------------------- combat

    public static List<String> combatLines(AbstractPlayer p) {
        List<String> out = new ArrayList<String>();
        if (p == null) {
            return out;
        }
        List<String> energy = new ArrayList<String>();
        addIf(p, energy, "Coffee Dripper", "每回合 +1 能量");
        addIf(p, energy, "Cursed Key", "每回合 +1 能量（开箱吃诅咒）");
        addIf(p, energy, "Fusion Hammer", "每回合 +1 能量（不能锻造）");
        addIf(p, energy, "Slaver's Collar", "精英/Boss 战 +1 能量");
        addIf(p, energy, "Sozu", "每回合 +1 能量（不能再拿药水）");
        addIf(p, energy, "Ectoplasm", "每回合 +1 能量（不能再拿金币）");
        addIf(p, energy, "Philosopher's Stone", "每回合 +1 能量（敌人 +1 力量）");
        addIf(p, energy, "Runic Dome", "每回合 +1 能量（看不到意图）");
        addIf(p, energy, "Velvet Choker", "每回合 +1 能量（最多 6 张牌）");
        addIf(p, energy, "Busted Crown", "每回合 +1 能量（奖励少 2 张）");
        addIf(p, energy, "Mark of Pain", "每回合 +1 能量（开局 2 伤口）");
        addIf(p, energy, "Holy Water", "开局 3 张奇迹");
        addIf(p, energy, "Violet Lotus", "退出平静额外 +1 能量");
        if (!energy.isEmpty()) {
            out.add("遗物能量：" + join(energy));
        }

        List<String> stats = new ArrayList<String>();
        addIf(p, stats, "Vajra", "力量 +1");
        addIf(p, stats, "Oddly Smooth Stone", "敏捷 +1");
        addIf(p, stats, "Data Disk", "集中 +1");
        addIf(p, stats, "Girya", "举铁层数可换力量");
        if (!stats.isEmpty()) {
            out.add("遗物属性：" + join(stats));
        }

        List<String> triggers = new ArrayList<String>();
        counter(p, "Pen Nib", "笔尖", "下一张攻击翻倍", triggers);
        counter(p, "Nunchaku", "不倒翁", "再打几次攻击 +1 能量", triggers);
        counter(p, "Kunai", "苦无", "再打几次攻击 +1 敏捷", triggers);
        counter(p, "Shuriken", "手里剑", "再打几次攻击 +1 力量", triggers);
        counter(p, "Letter Opener", "开信刀", "再打几张技能造成 AOE", triggers);
        if (!triggers.isEmpty()) {
            out.add("遗物计数：" + join(triggers));
        }
        String potionLine = RelicGuide.potionLine(p);
        if (potionLine.length() > 0) {
            out.add(potionLine);
        }
        String healthLine = RelicGuide.healthLine(p);
        if (healthLine.length() > 0) {
            out.add(healthLine);
        }
        if (p.hasPower("Confusion")) {
            out.add("费用提醒：混乱已生效，我是按手牌当前显示的费用算，不看原费。");
        }
        return out;
    }

    public static double combatBonus(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return 0.0;
        }
        double b = 0.0;
        if (necronomiconNow(p, c)) {
            b += 2.0;
        }
        if (has(p, "Chemical X") && CostUtil.isX(c)) {
            b += 1.5;
        }
        if (has(p, "Pen Nib") && c.type == AbstractCard.CardType.ATTACK && counter(p, "Pen Nib") >= 9) {
            b += 1.5;
        }
        if (has(p, "Nunchaku") && c.type == AbstractCard.CardType.ATTACK && counter(p, "Nunchaku") >= 9) {
            b += 0.8;
        }
        if ((has(p, "Kunai") || has(p, "Shuriken")) && c.type == AbstractCard.CardType.ATTACK
                && (counter(p, "Kunai") >= 2 || counter(p, "Shuriken") >= 2)) {
            b += 0.5;
        }
        if (has(p, "Letter Opener") && c.type == AbstractCard.CardType.SKILL && counter(p, "Letter Opener") >= 2) {
            b += 0.4;
        }
        if (has(p, "Dead Branch") && CardDatabase.hasTag(c, "exhaust")) {
            b += 0.6;
        }
        if (has(p, "Mummified Hand") && c.type == AbstractCard.CardType.POWER) {
            b += 0.5;
        }
        if (has(p, "Bird-Faced Urn") && c.type == AbstractCard.CardType.POWER) {
            b += 0.4;
        }
        if (has(p, "Tungsten Rod") && CardDatabase.hasTag(c, "selfdmg")) {
            b += 0.6;
        }
        if (has(p, "Snecko Eye") && CostUtil.current(c) >= 2) {
            b += 0.25;
        }
        return b;
    }

    public static String combatNote(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return "";
        }
        if (necronomiconNow(p, c)) {
            return "死灵书：这张当前费用 2 费以上，会打两次";
        }
        if (has(p, "Chemical X") && CostUtil.isX(c)) {
            return "化学 X：这张 X 费牌额外 +2";
        }
        if (has(p, "Pen Nib") && c.type == AbstractCard.CardType.ATTACK && counter(p, "Pen Nib") >= 9) {
            return "笔尖已满：这张攻击翻倍";
        }
        if (has(p, "Dead Branch") && CardDatabase.hasTag(c, "exhaust")) {
            return "腐化树枝：消耗后可能补一张牌";
        }
        if (has(p, "Mummified Hand") && c.type == AbstractCard.CardType.POWER) {
            return "干瘪之手：开能力后随机一张牌变 0 费";
        }
        return "";
    }

    // ------------------------------------------------------------- card pick

    public static double cardBonus(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return 0.0;
        }
        double b = 0.0;
        if (necronomiconPick(p, c)) {
            b += 0.8;
        }
        if (has(p, "Chemical X") && CostUtil.isX(c)) {
            b += 0.7;
        }
        if (has(p, "Dead Branch") && CardDatabase.hasTag(c, "exhaust")) {
            b += 0.55;
        }
        if (has(p, "Mummified Hand") && c.type == AbstractCard.CardType.POWER) {
            b += 0.45;
        }
        if (has(p, "Bird-Faced Urn") && c.type == AbstractCard.CardType.POWER) {
            b += 0.35;
        }
        if (has(p, "Wrist Blade") && CostUtil.printed(c) == 0 && c.type == AbstractCard.CardType.ATTACK) {
            b += 0.7;
        }
        if (has(p, "Hovering Kite") && CardDatabase.hasTag(c, "discard")) {
            b += 0.55;
        }
        if (has(p, "Tough Bandages") && CardDatabase.hasTag(c, "discard")) {
            b += 0.5;
        }
        if (has(p, "Tingsha") && CardDatabase.hasTag(c, "discard")) {
            b += 0.45;
        }
        if (has(p, "Snecko Skull") && CardDatabase.hasTag(c, "poison")) {
            b += 0.5;
        }
        if (has(p, "The Specimen") && CardDatabase.hasTag(c, "poison")) {
            b += 0.35;
        }
        if (has(p, "Wrist Blade") && CardDatabase.hasTag(c, "shiv")) {
            b += 0.5;
        }
        if (has(p, "Runic Pyramid") && CardDatabase.hasTag(c, "retain")) {
            b += 0.3;
        }
        if (has(p, "Violet Lotus") && CardDatabase.hasTag(c, "stance")) {
            b += 0.3;
        }
        if (has(p, "Shuriken") || has(p, "Kunai")) {
            int printed = CostUtil.printed(c);
            if (c.type == AbstractCard.CardType.ATTACK && printed >= 0 && printed <= 1) {
                b += 0.25;
            }
        }
        if (has(p, "Letter Opener") && c.type == AbstractCard.CardType.SKILL
                && CostUtil.printed(c) >= 0 && CostUtil.printed(c) <= 1) {
            b += 0.2;
        }
        if (has(p, "Tungsten Rod") && CardDatabase.hasTag(c, "selfdmg")) {
            b += 0.35;
        }
        if (RelicGuide.sustain(p) > 0 && CardDatabase.hasTag(c, "selfdmg")) {
            b += 0.25;
        }
        if (has(p, "Sozu") && CardDatabase.hasTag(c, "potion")) {
            b -= 0.35;
        }
        if (has(p, "Ornamental Fan") && c.type == AbstractCard.CardType.ATTACK) {
            b += 0.2;
        }
        if (has(p, "Molten Egg 2") && c.type == AbstractCard.CardType.ATTACK) {
            b += 0.35;
        }
        if (has(p, "Toxic Egg 2") && c.type == AbstractCard.CardType.SKILL) {
            b += 0.30;
        }
        if (has(p, "Frozen Egg 2") && c.type == AbstractCard.CardType.POWER) {
            b += 0.30;
        }
        if (has(p, "Ice Cream") && CostUtil.isX(c)) {
            b += 0.25;
        }
        if (has(p, "Snecko Eye") && CostUtil.printed(c) >= 2) {
            b += 0.20;
        }
        return b;
    }

    public static String cardNote(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return "";
        }
        List<String> notes = new ArrayList<String>();
        if (necronomiconPick(p, c)) {
            notes.add("死灵书能把它变成双发");
        }
        if (has(p, "Chemical X") && CostUtil.isX(c)) {
            notes.add("化学 X 直接加强 X 费");
        }
        if (has(p, "Dead Branch") && CardDatabase.hasTag(c, "exhaust")) {
            notes.add("腐化树枝会把消耗变成资源");
        }
        if (has(p, "Mummified Hand") && c.type == AbstractCard.CardType.POWER) {
            notes.add("干瘪之手让能力牌更赚");
        }
        if (has(p, "Bird-Faced Urn") && c.type == AbstractCard.CardType.POWER) {
            notes.add("鸟面瓮让开能力回血");
        }
        if (has(p, "Wrist Blade") && CostUtil.printed(c) == 0 && c.type == AbstractCard.CardType.ATTACK) {
            notes.add("袖剑给 0 费攻击加伤害");
        }
        if (has(p, "Hovering Kite") && CardDatabase.hasTag(c, "discard")) {
            notes.add("风筝把弃牌变成能量");
        }
        if (has(p, "Tough Bandages") && CardDatabase.hasTag(c, "discard")) {
            notes.add("绷带让弃牌变格挡");
        }
        if (has(p, "Tingsha") && CardDatabase.hasTag(c, "discard")) {
            notes.add("铜钹让弃牌补伤害");
        }
        if (has(p, "Snecko Skull") && CardDatabase.hasTag(c, "poison")) {
            notes.add("蛇骷髅让每次上毒多 1 层");
        }
        if (has(p, "The Specimen") && CardDatabase.hasTag(c, "poison")) {
            notes.add("标本让毒转移到别的怪");
        }
        if (has(p, "Wrist Blade") && CardDatabase.hasTag(c, "shiv")) {
            notes.add("袖剑专门加强小刀");
        }
        if (has(p, "Runic Pyramid") && CardDatabase.hasTag(c, "retain")) {
            notes.add("金字塔让你能留住关键牌");
        }
        if (has(p, "Violet Lotus") && CardDatabase.hasTag(c, "stance")) {
            notes.add("紫莲让退出平静多 1 能量");
        }
        if ((has(p, "Shuriken") || has(p, "Kunai")) && c.type == AbstractCard.CardType.ATTACK
                && CostUtil.printed(c) >= 0 && CostUtil.printed(c) <= 1) {
            notes.add("手里剑、苦无吃低费攻击次数");
        }
        if (has(p, "Letter Opener") && c.type == AbstractCard.CardType.SKILL
                && CostUtil.printed(c) >= 0 && CostUtil.printed(c) <= 1) {
            notes.add("开信刀吃低费技能次数");
        }
        if (has(p, "Tungsten Rod") && CardDatabase.hasTag(c, "selfdmg")) {
            notes.add("钨钢棒减轻自伤");
        }
        if (RelicGuide.sustain(p) > 0 && CardDatabase.hasTag(c, "selfdmg")) {
            notes.add("有回血遗物，卖血牌更敢用");
        }
        if (has(p, "Sozu") && CardDatabase.hasTag(c, "potion")) {
            notes.add("添水让你不能拿药水，药水牌会变差");
        }
        if (has(p, "Ornamental Fan") && c.type == AbstractCard.CardType.ATTACK) {
            notes.add("装饰扇吃攻击次数，多段攻击更容易触发格挡");
        }
        if (has(p, "Molten Egg 2") && c.type == AbstractCard.CardType.ATTACK) {
            notes.add("熔火蛋让攻击牌直接升级");
        }
        if (has(p, "Toxic Egg 2") && c.type == AbstractCard.CardType.SKILL) {
            notes.add("毒蛋让技能牌直接升级");
        }
        if (has(p, "Frozen Egg 2") && c.type == AbstractCard.CardType.POWER) {
            notes.add("冻蛋让能力牌直接升级");
        }
        if (has(p, "Ice Cream") && CostUtil.isX(c)) {
            notes.add("冰淇淋把剩余能量留到下一回合，X 费更赚");
        }
        if (has(p, "Snecko Eye") && CostUtil.printed(c) >= 2) {
            notes.add("异蛇之眼会把高费牌随机成 0–3 费");
        }
        StringBuilder sb = new StringBuilder();
        for (String n : notes) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(n);
        }
        return sb.toString();
    }

    // --------------------------------------------------------------- shop fit

    public static double fitScore(AbstractPlayer p, AbstractRelic r) {
        String id = norm(r.relicId);
        DeckSummary d = DeckSummary.of(p);
        if (id.contains("whitebeast")) {
            if (has(p, "Sozu")) {
                return -0.8;
            }
            return p.potions != null && p.potions.size() < p.potionSlots ? 0.65 : 0.4;
        }
        if (id.contains("potionbelt")) {
            return p.potions != null && p.potions.size() >= 2 ? 0.45 : 0.3;
        }
        if (id.contains("sacredbark")) {
            return p.potions != null && p.potions.size() >= 2 ? 0.55 : 0.3;
        }
        if (id.contains("burningblood") || id.contains("blackblood") || id.contains("bloodvial")
                || id.contains("meatonthebone") || id.contains("magicflower")
                || id.contains("birdfacedurn") || id.contains("regalpillow")) {
            return p.currentHealth * 100 / Math.max(1, p.maxHealth) < 70 ? 0.5 : 0.2;
        }
        if (id.contains("ornamentalfan")) {
            return d.attacks >= 8 ? 0.5 : 0.2;
        }
        if (id.contains("pennib") || id.contains("nunchaku") || id.contains("kunai")
                || id.contains("shuriken")) {
            return d.attacks >= 8 ? 0.35 : 0.15;
        }
        if (id.contains("membershipcard") || id.contains("courier") || id.contains("mawbank")
                || id.contains("smilingmask")) {
            return p.gold >= 150 ? 0.35 : 0.15;
        }
        if (id.contains("threadandneedle") || id.contains("orichalcum")) {
            return d.blockCards < 8 ? 0.4 : 0.15;
        }
        if (id.contains("omamori") || id.contains("bluecandle") || id.contains("duvudoll")
                || id.contains("darkstoneperiapt")) {
            return curseCount(p) > 0 ? 0.4 : 0.1;
        }
        if (id.contains("deadbranch") && d.tag("exhaust") >= 3) {
            return 0.8;
        }
        if (id.contains("sneckoeye") && highCost(p) >= 5) {
            return 0.7;
        }
        if (id.contains("runicpyramid") && d.drawCards >= 2) {
            return 0.6;
        }
        if (id.contains("icecream") && d.tag("energy") >= 2) {
            return 0.6;
        }
        if (id.contains("necronomicon") && highCostAttacks(p) >= 2) {
            return 0.8;
        }
        if (id.contains("chemicalx") && xCosts(p) >= 1) {
            return 0.7;
        }
        if (id.contains("mummifiedhand") && d.powers >= 3) {
            return 0.5;
        }
        if (id.contains("birdfacedurn") && d.powers >= 3) {
            return 0.45;
        }
        if (id.contains("wristblade") && d.tag("shiv") >= 3) {
            return 0.8;
        }
        if (id.contains("hoveringkite") && d.tag("discard") >= 3) {
            return 0.7;
        }
        if (id.contains("toughbandages") && d.tag("discard") >= 3) {
            return 0.6;
        }
        if (id.contains("sacredbark") && (d.tag("potion") >= 1 || p.hasRelic("White Beast Statue"))) {
            return 0.45;
        }
        if (id.contains("tungstenrod") && d.tag("selfdmg") >= 2) {
            return 0.55;
        }
        return 0.0;
    }

    public static String fitNote(AbstractPlayer p, AbstractRelic r) {
        String id = norm(r.relicId);
        DeckSummary d = DeckSummary.of(p);
        if (id.contains("whitebeast")) {
            return has(p, "Sozu")
                    ? "添水会让药水掉落失效，别拿"
                    : "药水会稳定掉落，普通战斗可以更积极喝药，Boss 前留高价值药水";
        }
        if (id.contains("potionbelt")) {
            return "多两个药水格，可以多带一瓶针对 Boss 的药水";
        }
        if (id.contains("sacredbark")) {
            return "药水效果翻倍，药水越强越值得留";
        }
        if (id.contains("burningblood") || id.contains("blackblood") || id.contains("bloodvial")
                || id.contains("meatonthebone") || id.contains("magicflower")
                || id.contains("birdfacedurn") || id.contains("regalpillow")) {
            return "有续航，卖血换资源更安全；但 Boss 前仍要留安全垫";
        }
        if (id.contains("ornamentalfan")) {
            return d.attacks >= 8 ? "攻击牌多，每 3 次攻击给格挡很赚" : "攻击频率不高时收益一般";
        }
        if (id.contains("membershipcard") || id.contains("courier") || id.contains("mawbank")
                || id.contains("smilingmask")) {
            return "商店/金币更自由，可以多删牌或买关键遗物";
        }
        if (id.contains("threadandneedle") || id.contains("orichalcum")) {
            return d.blockCards < 8 ? "格挡牌偏少，这种遗物正好补防御" : "格挡牌已经够多";
        }
        if (id.contains("omamori") || id.contains("bluecandle") || id.contains("duvudoll")
                || id.contains("darkstoneperiapt")) {
            return curseCount(p) > 0 ? "牌组已有诅咒，能把它变成资源" : "没有诅咒时只是保险，别主动吃";
        }
        if (id.contains("deadbranch") && d.tag("exhaust") >= 3) {
            return "你有消耗体系，树枝能把消耗变成牌";
        }
        if (id.contains("sneckoeye") && highCost(p) >= 5) {
            return "你高费牌多，蛇眼能靠额外抽牌和高费牌赚";
        }
        if (id.contains("runicpyramid") && d.drawCards >= 2) {
            return "你有过牌，金字塔能把关键牌留到正确回合";
        }
        if (id.contains("necronomicon") && highCostAttacks(p) >= 2) {
            return "你有高费攻击，死灵书能双发";
        }
        if (id.contains("wristblade") && d.tag("shiv") >= 3) {
            return "小刀体系，袖剑直接加伤害";
        }
        if (id.contains("hoveringkite") && d.tag("discard") >= 3) {
            return "弃牌体系，风筝把弃牌变能量";
        }
        return "";
    }

    // ----------------------------------------------------------- boss relics

    public static BossChoice chooseBoss(AbstractPlayer p, List<AbstractRelic> relics) {
        BossChoice out = new BossChoice();
        if (p == null || relics == null || relics.isEmpty()) {
            return null;
        }
        DeckSummary deck = DeckSummary.of(p);
        for (AbstractRelic r : relics) {
            if (r == null) {
                continue;
            }
            Scored s = new Scored();
            s.relic = r;
            s.score = bossScore(p, r, deck);
            s.fit = bossFit(p, r, deck);
            s.downside = bossDownside(r);
            out.choices.add(s);
        }
        Collections.sort(out.choices, new Comparator<Scored>() {
            @Override
            public int compare(Scored a, Scored b) {
                return Double.compare(b.score, a.score);
            }
        });
        if (out.choices.isEmpty()) {
            return null;
        }
        out.best = out.choices.get(0).relic;
        out.score = out.choices.get(0).score;
        out.lines.add("Boss 遗物三选一，我帮您逐个看：");
        for (Scored s : out.choices) {
            String tier = RelicDatabase.tier(s.relic);
            out.lines.add(String.format("【%s】%s · 本局 %.1f 分",
                    s.relic.name, tier.length() == 0 ? "" : "Tier " + tier, s.score));
            out.lines.add("作用：" + effect(s.relic));
            if (s.downside.length() > 0) {
                out.lines.add("代价：" + s.downside);
            }
            if (s.fit.length() > 0) {
                out.lines.add("和您牌组：" + s.fit);
            }
            RelicGuide.Profile profile = RelicGuide.profile(p, s.relic);
            if (profile.plan.length() > 0) {
                out.lines.add("拿到后怎么用：" + profile.plan);
            }
        }
        Scored best = out.choices.get(0);
        out.lines.add("我建议选【" + best.relic.name + "】："
                + (best.fit.length() > 0 ? best.fit + "。" : "它最贴合现在的牌组和血量。"));
        return out;
    }

    private static double bossScore(AbstractPlayer p, AbstractRelic r, DeckSummary d) {
        double base = RelicDatabase.score(r);
        if (base < 0) {
            base = 6.0;
        }
        double s = base;
        String id = norm(r.relicId);
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        int basics = basics(p);
        int high = highCost(p);
        if (isEnergyRelic(id)) {
            s += high >= 5 ? 0.5 : 0.2;
            if (hpPct < 55 && id.contains("coffe")) {
                s -= 0.8;
            }
            if (id.contains("bustedcrown") && (d.size < 24 || highValueCards(p) < 6)) {
                s -= 1.0;
            }
            if (id.contains("sozu") && p.potions != null && !p.potions.isEmpty()) {
                s -= 0.2;
            }
            if (id.contains("ectoplasm") && p.gold > 200) {
                s -= 0.5;
            }
            if (id.contains("runicdome")) {
                s -= 0.5;
            }
        }
        if (id.contains("runicpyramid") && d.drawCards >= 2) {
            s += 0.6;
        }
        if (id.contains("sneckoeye")) {
            s += high >= 5 ? 0.8 : -0.4;
            if (d.zeroCost >= 5) {
                s -= 0.5;
            }
        }
        if (id.contains("pandorasbox") && basics >= 6) {
            s += 0.7;
        }
        if (id.contains("astrolabe") && basics >= 4) {
            s += 0.5;
        }
        if (id.contains("emptycage") && basics >= 4) {
            s += 0.4;
        }
        if (id.contains("blackblood") && hpPct < 65) {
            s += 0.5;
        }
        if (id.contains("blackstar") && hpPct > 65) {
            s += 0.35;
        }
        if (id.contains("sacredbark") && p.potions != null && p.potions.size() >= 2) {
            s += 0.35;
        }
        if (id.contains("wristblade") && d.tag("shiv") >= 3) {
            s += 0.7;
        }
        if (id.contains("hoveringkite")) {
            if (d.tag("discard") >= 3) {
                s += 0.6;
            } else {
                s -= 0.9;
            }
        }
        if (id.contains("velvetchoker") && (d.zeroCost >= 5 || d.drawCards >= 4)) {
            s -= 0.8;
        }
        if (id.contains("philosophersstone") && (d.blockCards < 8 || hpPct < 60)) {
            s -= 0.8;
        }
        if (id.contains("frozenco") && d.tag("focus") >= 3) {
            s += 0.5;
        }
        if (id.contains("holywater") && d.character.contains("WATCHER")) {
            s += 0.4;
        }
        if (id.contains("violetlotus") && d.tag("stance") >= 3) {
            s += 0.6;
        }
        if (id.contains("nuclearbattery") && d.character.contains("DEFECT")) {
            s += 0.4;
        }
        return s;
    }

    private static String bossFit(AbstractPlayer p, AbstractRelic r, DeckSummary d) {
        String id = norm(r.relicId);
        if (id.contains("runicpyramid") && d.drawCards >= 2) {
            return "你有过牌，能把关键牌留到正确回合";
        }
        if (id.contains("sneckoeye") && highCost(p) >= 5) {
            return "高费牌多，额外抽两张很赚";
        }
        if (id.contains("pandorasbox") && basics(p) >= 6) {
            return "基础打击防御多，变身提升最大";
        }
        if (id.contains("astrolabe") && basics(p) >= 4) {
            return "能把基础牌变成升级牌，补质量";
        }
        if (id.contains("emptycage") && basics(p) >= 4) {
            return "删两张基础牌，提高关键牌上手率";
        }
        if (id.contains("blackblood") && p.currentHealth * 100 / Math.max(1, p.maxHealth) < 65) {
            return "血量偏低，打赢后回 12 血很稳";
        }
        if (id.contains("wristblade") && d.tag("shiv") >= 3) {
            return "小刀体系直接加伤害";
        }
        if (id.contains("hoveringkite") && d.tag("discard") >= 3) {
            return "弃牌体系每回合多能量";
        }
        if (id.contains("violetlotus") && d.tag("stance") >= 3) {
            return "姿态循环能反复触发";
        }
        return "";
    }

    private static String bossDownside(AbstractRelic r) {
        String id = norm(r.relicId);
        if (id.contains("coffe")) {
            return "不能休息，只能靠战斗后回血/药水/治疗";
        }
        if (id.contains("cursedkey")) {
            return "开箱会吃诅咒，别见箱就开";
        }
        if (id.contains("fusionhammer")) {
            return "不能锻造，核心牌还没升级时要谨慎";
        }
        if (id.contains("sozu")) {
            return "不能再拿药水，药水是打精英/Boss 的保险";
        }
        if (id.contains("ectoplasm")) {
            return "不能再拿金币，商店/删牌会受影响";
        }
        if (id.contains("philosophersstone")) {
            return "敌人全体 +1 力量，多段攻击会越打越痛；格挡不足时很危险";
        }
        if (id.contains("runicdome")) {
            return "看不到敌人意图，新手很难防";
        }
        if (id.contains("velvetchoker")) {
            return "每回合最多出 6 张牌，低费/抽牌循环会被卡住";
        }
        if (id.contains("bustedcrown")) {
            return "未来卡奖励少 2 张；牌组没成型时更难补核心";
        }
        if (id.contains("markofpain")) {
            return "开局多 2 张伤口，没消耗手段会卡手";
        }
        if (id.contains("sneckoeye")) {
            return "费用随机，稳定 0 费连招会被打乱";
        }
        if (id.contains("runicpyramid")) {
            return "手牌上限只有 10，状态/诅咒会卡住";
        }
        if (id.contains("pandorasbox") || id.contains("astrolabe")) {
            return "变化结果随机，可能补不到防御";
        }
        if (id.contains("hoveringkite")) {
            return "需要每回合弃牌才有能量；没有稳定弃牌出口时接近白板";
        }
        return "";
    }

    private static String effect(AbstractRelic r) {
        if (r.description == null || r.description.length() == 0) {
            return r.name;
        }
        return r.description;
    }

    // --------------------------------------------------------------- helpers

    private static boolean has(AbstractPlayer p, String idPart) {
        if (p == null || p.relics == null) {
            return false;
        }
        for (AbstractRelic r : p.relics) {
            if (r != null && norm(r.relicId).contains(norm(idPart))) {
                return true;
            }
        }
        return false;
    }

    private static int counter(AbstractPlayer p, String idPart) {
        if (p == null || p.relics == null) {
            return 0;
        }
        for (AbstractRelic r : p.relics) {
            if (r != null && norm(r.relicId).contains(norm(idPart))) {
                return r.counter;
            }
        }
        return 0;
    }

    private static void counter(AbstractPlayer p, String id, String label, String text,
                                List<String> out) {
        int n = counter(p, id);
        if (n > 0 || has(p, id)) {
            out.add("【" + label + "】" + n + " 层，" + text);
        }
    }

    private static void addIf(AbstractPlayer p, List<String> out, String id, String text) {
        if (has(p, id)) {
            out.add("【" + id + "】" + text);
        }
    }

    private static boolean isEnergyRelic(String id) {
        return id.contains("coffe") || id.contains("cursedkey") || id.contains("fusionhammer")
                || id.contains("slaverscollar") || id.contains("sozu") || id.contains("ectoplasm")
                || id.contains("philosophersstone") || id.contains("runicdome")
                || id.contains("velvetchoker") || id.contains("bustedcrown")
                || id.contains("markofpain");
    }

    private static int curseCount(AbstractPlayer p) {
        int n = 0;
        if (p == null || p.masterDeck == null) {
            return n;
        }
        for (AbstractCard c : p.masterDeck.group) {
            if (c != null && c.type == AbstractCard.CardType.CURSE) {
                n++;
            }
        }
        return n;
    }

    private static int highValueCards(AbstractPlayer p) {        int n = 0;
        if (p == null || p.masterDeck == null) {
            return n;
        }
        for (AbstractCard c : p.masterDeck.group) {
            CardDatabase.Entry e = CardDatabase.get(c);
            if (e != null && e.score >= 8.0) {
                n++;
            }
        }
        return n;
    }

    private static int basics(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            String id = norm(c.cardID);
            if (id.contains("strike") || id.contains("defend")) {
                n++;
            }
        }
        return n;
    }

    private static int highCost(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (CostUtil.printed(c) >= 2) {
                n++;
            }
        }
        return n;
    }

    private static int highCostAttacks(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c.type == AbstractCard.CardType.ATTACK && CostUtil.printed(c) >= 2) {
                n++;
            }
        }
        return n;
    }

    private static int xCosts(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (CostUtil.isX(c)) {
                n++;
            }
        }
        return n;
    }

    private static boolean necronomiconNow(AbstractPlayer p, AbstractCard c) {
        if (!has(p, "Necronomicon") || c == null || c.type != AbstractCard.CardType.ATTACK) {
            return false;
        }
        // Necronomicon checks costForTurn >= 2, or X-cost with at least 2
        // energy spent.  Do not use the printed cost: Corruption and
        // Mummified Hand can make a 2-cost attack cost 0 right now.
        if (CostUtil.isX(c)) {
            return EnergyPanel.totalCount >= 2;
        }
        return CostUtil.current(c) >= 2;
    }

    private static boolean necronomiconPick(AbstractPlayer p, AbstractCard c) {
        return has(p, "Necronomicon") && c != null
                && c.type == AbstractCard.CardType.ATTACK && CostUtil.printed(c) >= 2;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size() && i < 3; i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static String norm(String s) {
        return CardDatabase.norm(s == null ? "" : s.toLowerCase(Locale.ROOT));
    }
}
