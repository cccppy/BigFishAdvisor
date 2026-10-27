package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;

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
        return out;
    }

    public static double combatBonus(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return 0.0;
        }
        double b = 0.0;
        if (has(p, "Necronomicon") && c.type == AbstractCard.CardType.ATTACK && c.cost >= 2) {
            b += 2.0;
        }
        if (has(p, "Chemical X") && c.costForTurn < 0) {
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
        if (has(p, "Snecko Eye") && c.cost >= 2) {
            b += 0.25;
        }
        return b;
    }

    public static String combatNote(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return "";
        }
        if (has(p, "Necronomicon") && c.type == AbstractCard.CardType.ATTACK && c.cost >= 2) {
            return "死灵书：这张 2 费以上攻击会打两次";
        }
        if (has(p, "Chemical X") && c.costForTurn < 0) {
            return "化学 X：X 费牌额外 +2";
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
        if (has(p, "Necronomicon") && c.type == AbstractCard.CardType.ATTACK && c.cost >= 2) {
            b += 0.8;
        }
        if (has(p, "Chemical X") && c.costForTurn < 0) {
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
        if (has(p, "Wrist Blade") && c.costForTurn == 0 && c.type == AbstractCard.CardType.ATTACK) {
            b += 0.7;
        }
        if (has(p, "Hovering Kite") && CardDatabase.hasTag(c, "discard")) {
            b += 0.55;
        }
        if (has(p, "Tough Bandages") && CardDatabase.hasTag(c, "discard")) {
            b += 0.5;
        }
        if (has(p, "Shuriken") || has(p, "Kunai")) {
            if (c.type == AbstractCard.CardType.ATTACK && c.costForTurn <= 1) {
                b += 0.25;
            }
        }
        if (has(p, "Letter Opener") && c.type == AbstractCard.CardType.SKILL && c.costForTurn <= 1) {
            b += 0.2;
        }
        if (has(p, "Tungsten Rod") && CardDatabase.hasTag(c, "selfdmg")) {
            b += 0.35;
        }
        return b;
    }

    public static String cardNote(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return "";
        }
        if (has(p, "Necronomicon") && c.type == AbstractCard.CardType.ATTACK && c.cost >= 2) {
            return "死灵书能把它变成双发";
        }
        if (has(p, "Chemical X") && c.costForTurn < 0) {
            return "化学 X 直接加强 X 费";
        }
        if (has(p, "Dead Branch") && CardDatabase.hasTag(c, "exhaust")) {
            return "腐化树枝会把消耗变成资源";
        }
        if (has(p, "Mummified Hand") && c.type == AbstractCard.CardType.POWER) {
            return "干瘪之手让能力牌更赚";
        }
        if (has(p, "Wrist Blade") && c.costForTurn == 0 && c.type == AbstractCard.CardType.ATTACK) {
            return "袖剑给 0 费攻击加伤害";
        }
        if (has(p, "Hovering Kite") && CardDatabase.hasTag(c, "discard")) {
            return "风筝把弃牌变成能量";
        }
        return "";
    }

    // --------------------------------------------------------------- shop fit

    public static double fitScore(AbstractPlayer p, AbstractRelic r) {
        String id = norm(r.relicId);
        DeckSummary d = DeckSummary.of(p);
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
            if (id.contains("bustedcrown") && d.size < 20) {
                s -= 1.2;
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
        if (id.contains("hoveringkite") && d.tag("discard") >= 3) {
            s += 0.6;
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
            return "敌人全体 +1 力量，多段怪会很疼";
        }
        if (id.contains("runicdome")) {
            return "看不到敌人意图，新手很难防";
        }
        if (id.contains("velvetchoker")) {
            return "每回合最多 6 张牌，0 费连击流会被卡";
        }
        if (id.contains("bustedcrown")) {
            return "以后奖励少 2 张，补关键牌更难";
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
            if (c.costForTurn >= 2 || c.cost >= 2) {
                n++;
            }
        }
        return n;
    }

    private static int highCostAttacks(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c.type == AbstractCard.CardType.ATTACK && (c.costForTurn >= 2 || c.cost >= 2)) {
                n++;
            }
        }
        return n;
    }

    private static int xCosts(AbstractPlayer p) {
        int n = 0;
        for (AbstractCard c : p.masterDeck.group) {
            if (c.costForTurn < 0 || c.cost < 0) {
                n++;
            }
        }
        return n;
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
