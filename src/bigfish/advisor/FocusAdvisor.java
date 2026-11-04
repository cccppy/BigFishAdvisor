package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Multi-enemy focus.  "Highest HP" is not always the right target: Sentries
 * should be killed one at a time, Byrd flight has to be stripped first, and a
 * healing Mystic is more urgent than the Centurion it heals.  The advisor
 * therefore returns both a primary focus and an explicit kill order.
 */
public final class FocusAdvisor {
    public static final class Focus {
        public AbstractMonster target;
        public String reason = "";
        public List<AbstractMonster> order = new ArrayList<AbstractMonster>();
    }

    private FocusAdvisor() {
    }

    public static Focus choose(AbstractPlayer p, List<AbstractMonster> alive,
                               AbstractCard card, boolean aoe) {
        Focus f = new Focus();
        if (alive == null || alive.isEmpty()) {
            return f;
        }
        List<AbstractMonster> list = new ArrayList<AbstractMonster>();
        for (AbstractMonster m : alive) {
            if (m != null && !m.isDeadOrEscaped()) {
                list.add(m);
            }
        }
        if (list.isEmpty()) {
            return f;
        }
        if (list.size() == 1) {
            f.target = list.get(0);
            f.order.addAll(list);
            return f;
        }
        if (all(list, "sentry")) {
            // The two side Sentries share their beam pattern; the middle one
            // is on the opposite beat.  Focus first or third (whichever is
            // lower) so you remove one half of the 18/20 burst instead of
            // spreading damage and eating both beams.
            AbstractMonster first = list.get(0);
            AbstractMonster last = list.get(list.size() - 1);
            f.target = (first.currentHealth + first.currentBlock)
                    <= (last.currentHealth + last.currentBlock) ? first : last;
            f.order.add(f.target);
            // If the chosen side is the first, kill the last next; if it is the
            // last, kill the first next.  That removes both synchronized beams
            // before spending damage on the middle Sentry.
            AbstractMonster otherSide = f.target == first ? last : first;
            if (otherSide != f.target) {
                f.order.add(otherSide);
            }
            for (AbstractMonster m : list) {
                if (!f.order.contains(m)) {
                    f.order.add(m);
                }
            }
            f.reason = "三柱（哨卫）不要平均摊伤害：两侧哨卫的激光是同步的，"
                    + "先集中杀第一只或第三只，拆掉一半的 18/20 爆发；"
                    + "中间那只和两侧错开行动。";
            return f;
        }
        if (all(list, "byrd")) {
            return byrd(f, list);
        }

        // ---- Special group fights ----------------------------------------
        // These are not "pick the highest HP" fights.  Each group below has a
        // concrete kill order that changes the amount of damage you take.
        if (any(list, "gremlinleader")) {
            return gremlinLeader(f, list, aoe);
        }
        if (any(list, "gremlinfat") || any(list, "gremlinwizard")
                || any(list, "gremlinwarrior") || any(list, "gremlinthief")
                || any(list, "gremlintsundere")) {
            return ordered(f, list, new String[]{
                    "gremlinfat", "gremlinwizard", "gremlinwarrior",
                    "gremlinthief", "gremlintsundere"},
                    "小地精不是随便点：先杀胖地精（它给全体加格挡，不拆会让其他地精多活一轮），"
                            + "再杀法师（塞状态/充能）；战士/发怒地精受伤会加力量，别用多段小攻击喂它。"
                            + "有 AOE 就一起压，但优先目标仍是胖地精/法师。");
        }
        if (any(list, "taskmaster") || any(list, "slaverblue")
                || any(list, "slaverred")) {
            return ordered(f, list, new String[]{"taskmaster", "slaverred", "slaverblue"},
                    "三奴隶先杀工头（Taskmaster）：它会上易伤，让你后面每一刀都更疼；"
                            + "再杀红奴隶（缠绕/限制出牌），最后蓝奴隶。别平均打。");
        }
        if (any(list, "bronzeorb")) {
            return ordered(f, list, new String[]{"bronzeorb", "automaton"},
                    "铜人的两个青铜球会偷牌/干扰，先清球再打本体；"
                            + "球清掉后铜人只剩 Hyper Beam 的节奏，留好大格挡。");
        }
        if (any(list, "torchhead")) {
            return ordered(f, list, new String[]{"torchhead", "collector"},
                    "收藏家会强化火炬头，火炬头每回合都输出；先清火炬头，"
                            + "别让收藏家白加力量。AOE 可以同时压本体。");
        }
        if (any(list, "awakenedone") && any(list, "cultist")) {
            return ordered(f, list, new String[]{"cultist", "awakenedone"},
                    "觉醒者带两个邪教徒：先杀邪教徒，它们会给本体叠力量/持续输出；"
                            + "再处理觉醒者，注意第一条命倒下后会复活。");
        }
        if (any(list, "cultist")) {
            sortByHpThenThreat(list);
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "多只邪教徒会一起叠仪式，越拖越疼；集中杀一只，"
                    + "别平均打，否则它们的仪式层数会一起长起来。";
            return f;
        }
        if (any(list, "looter") || any(list, "mugger")) {
            sortByHpThenThreat(list);
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "盗贼会偷钱并逃跑，优先杀血少的那只，别让它带着金币跑掉；"
                    + "两只都活着时，先解决准备逃跑/血线低的。";
            return f;
        }
        if (any(list, "acidslime") || any(list, "spikeslime")) {
            return ordered(f, list, new String[]{"acidslime", "spikeslime"},
                    "史莱姆分裂后先杀酸液史莱姆（会给你虚弱/塞黏液），"
                            + "再处理尖刺史莱姆（有荆棘，别用多段小攻击）；"
                            + "能 AOE 就一起压，别让它们拖回合。");
        }
        if (any(list, "dagger") && any(list, "reptomancer")) {
            return ordered(f, list, new String[]{"dagger", "reptomancer"},
                    "蛇女召唤的匕首会越积越多、还会爆炸；先用 AOE/多段清匕首，"
                            + "匕首数量压住后再打蛇女。不要让匕首堆到 4 把以上。");
        }
        if (any(list, "spirespear") && any(list, "spireshield")) {
            return ordered(f, list, new String[]{"spirespear", "spireshield"},
                    "第四幕精英先杀长矛：它会给全体加力量、往牌堆塞燃烧；"
                            + "盾的格挡/护盾可以后处理。先拆长矛的增益，再解决盾。");
        }
        if (any(list, "chosen")) {
            return ordered(f, list, new String[]{"chosen"},
                    "被选者会塞诅咒/虚无并上易伤，先处理它；"
                            + "它活着时你的手牌会越来越脏，越拖越难打。");
        }
        if (any(list, "banditleader") || any(list, "banditpointy")
                || any(list, "banditbear")) {
            return ordered(f, list, new String[]{"banditleader", "banditpointy", "banditbear"},
                    "蒙面强盗先杀头目（Romeo）：它会加力量/强化；再杀 Pointy（输出），"
                            + "熊（Bear）比较肉，最后处理。");
        }
        if (any(list, "exploder") || any(list, "repulsor") || any(list, "spiker")) {
            return ordered(f, list, new String[]{"exploder", "repulsor", "spiker"},
                    "三只形状怪：先杀爆炸怪（它会在几回合后自爆），"
                            + "再杀排斥怪（上虚弱/易伤），最后处理尖刺怪（有荆棘，别用多段小攻击）。");
        }
        if (any(list, "louse")) {
            return ordered(f, list, new String[]{"fuzzylousenormal", "redlouse",
                    "fuzzylousedeffensive", "greenlouse"},
                    "两只虱虫先杀红虱（会叠力量，越拖越疼）；绿虱会蜷缩加格挡，"
                            + "用高单次伤害破壳，别用多段小攻击。");
        }
        if (any(list, "fungibeast")) {
            sortByHpThenThreat(list);
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "真菌怪会生长叠力量，死亡还会给你易伤；集中杀一只，"
                    + "别两只都打残，否则孢子/力量一起爆。";
            return f;
        }
        if (any(list, "mystic")) {
            sortPriority(list, "mystic");
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "先杀祭司：它会治疗/强化百夫长，不先处理会把战斗拖成回血循环。";
            return f;
        }
        if (any(list, "donu")) {
            sortPriority(list, "donu");
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "先杀甜甜圈（Donu）：它会给两只加力量；方片（Deca）的格挡可以后面处理。";
            return f;
        }
        if (all(list, "darkling")) {
            sortByHpThenThreat(list);
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "暗灵会复活：尽量同一回合收掉，先压血最少的一只；"
                    + "AOE 压三只血线，别让它们轮流复活。";
            return f;
        }

        // Generic group: threat first, then a low-health target that can be
        // finished.  For AoE, pressure the dangerous/high-health target rather
        // than chasing a almost-dead minion.
        Collections.sort(list, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                double va = genericValue(a, aoe);
                double vb = genericValue(b, aoe);
                return Double.compare(vb, va);
            }
        });
        f.target = list.get(0);
        f.order.addAll(list);
        f.reason = aoe
                ? "这是群体牌，会打到所有敌人；先关注高威胁/高血量目标，不要只为了收残血把伤害摊平。"
                : "先处理威胁最高、又最容易被收掉的目标；不要平均摊伤害。";
        return f;
    }

    private static Focus byrd(Focus f, List<AbstractMonster> list) {
        // Byrd: Flight halves incoming attack damage and loses one stack per
        // attack hit; it lands only when every Flight stack is stripped.
        // Prefer the bird with the least Flight/lowest HP, and use multi-hit/AoE.
        Collections.sort(list, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                int fa = flight(a);
                int fb = flight(b);
                if (fa != fb) {
                    return Integer.compare(fa, fb);
                }
                int ha = a.currentHealth + a.currentBlock;
                int hb = b.currentHealth + b.currentBlock;
                if (ha != hb) {
                    return Integer.compare(ha, hb);
                }
                return Integer.compare(threat(b), threat(a));
            }
        });
        f.target = list.get(0);
        f.order.addAll(list);
        f.reason = "鸟群有【飞行】：攻击伤害会减半，打满飞行层数才落地；"
                + "先用多段/AOE 把飞行剥掉，再集中一只杀。不要三只平均打，"
                + "否则每只都剩一点血继续打你。";
        return f;
    }

    private static Focus gremlinLeader(Focus f, List<AbstractMonster> list, boolean aoe) {
        AbstractMonster leader = find(list, "gremlinleader");
        if (leader == null) {
            return ordered(f, list, new String[]{"gremlinfat", "gremlinwizard"},
                    "先清小地精，再处理地精头子。");
        }
        // Low leader HP: decapitate before it can Rally/Encourage again.
        // Otherwise remove the minions that are actually dealing damage.
        boolean burst = leader.currentHealth <= Math.max(35, leader.maxHealth / 2);
        if (burst || aoe) {
            f.target = leader;
            f.order.add(leader);
            for (AbstractMonster m : list) {
                if (m != leader) {
                    f.order.add(m);
                }
            }
            f.reason = "地精头子血线已经低了，直接斩首：它一死，小地精的强化/召唤就断了。"
                    + (aoe ? "AOE 会顺手压小地精。" : "别再把伤害摊给小地精。");
            return f;
        }
        List<AbstractMonster> minions = new ArrayList<AbstractMonster>();
        for (AbstractMonster m : list) {
            if (m != leader) {
                minions.add(m);
            }
        }
        Collections.sort(minions, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                int ra = rank(a, new String[]{"gremlinfat", "gremlinwizard", "gremlinwarrior"});
                int rb = rank(b, new String[]{"gremlinfat", "gremlinwizard", "gremlinwarrior"});
                if (ra != rb) {
                    return Integer.compare(ra, rb);
                }
                return Integer.compare(a.currentHealth, b.currentHealth);
            }
        });
        f.target = minions.isEmpty() ? leader : minions.get(0);
        f.order.addAll(minions);
        if (f.target != leader) {
            f.order.add(leader);
        }
        f.reason = "先清小地精：头子会 Rally/Encourage，小地精活着每轮都在加伤害；"
                + "等头子血线低（约一半以下）再直接斩首。AOE 可以同时压。";
        return f;
    }

    /** Pick the first matching monster and put it first, then the rest by rank. */
    private static Focus ordered(Focus f, List<AbstractMonster> list, String[] priority, String reason) {
        Collections.sort(list, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                int ra = rank(a, priority);
                int rb = rank(b, priority);
                if (ra != rb) {
                    return Integer.compare(ra, rb);
                }
                int ta = threat(a);
                int tb = threat(b);
                if (ta != tb) {
                    return Integer.compare(tb, ta);
                }
                return Integer.compare(a.currentHealth, b.currentHealth);
            }
        });
        f.target = list.get(0);
        f.order.addAll(list);
        f.reason = reason;
        return f;
    }

    private static AbstractMonster find(List<AbstractMonster> list, String needle) {
        for (AbstractMonster m : list) {
            if (key(m).contains(needle)) {
                return m;
            }
        }
        return null;
    }

    private static int rank(AbstractMonster m, String[] priority) {
        String k = key(m);
        for (int i = 0; i < priority.length; i++) {
            if (k.contains(priority[i])) {
                return i;
            }
        }
        return priority.length + 1;
    }

    /** Preserve the special order (Sentries/Byrds) chosen above instead of
     *  re-sorting by generic threat in the display line. */
    public static String orderLine(Focus focus, boolean aoe) {
        if (focus == null || focus.order == null || focus.order.size() < 2) {
            return "";
        }
        StringBuilder sb = new StringBuilder("先打谁：");
        int n = Math.min(3, focus.order.size());
        for (int i = 0; i < n; i++) {
            AbstractMonster m = focus.order.get(i);
            if (m == null) {
                continue;
            }
            if (i > 0) {
                sb.append("、");
            }
            sb.append("【").append(m.name).append("】");
            sb.append("（").append(targetReason(m)).append("）");
        }
        if (focus.order.size() > 3) {
            sb.append("、其余");
        }
        if (aoe) {
            sb.append("。AOE 会打到全体，但别平均摊；先把高威胁目标压到斩杀线。");
        } else {
            sb.append("。不要平均打，先把一只打死的压力变成少挨一轮。");
        }
        return sb.toString();
    }

    public static String orderLine(List<AbstractMonster> alive, AbstractMonster primary,
                                   boolean aoe) {
        if (alive == null || alive.size() < 2) {
            return "";
        }
        List<AbstractMonster> order = new ArrayList<AbstractMonster>();
        if (primary != null && !primary.isDeadOrEscaped()) {
            order.add(primary);
        }
        List<AbstractMonster> rest = new ArrayList<AbstractMonster>();
        for (AbstractMonster m : alive) {
            if (m != null && !m.isDeadOrEscaped() && m != primary) {
                rest.add(m);
            }
        }
        Collections.sort(rest, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                return Double.compare(genericValue(b, false), genericValue(a, false));
            }
        });
        order.addAll(rest);
        StringBuilder sb = new StringBuilder("先打谁：");
        int n = Math.min(3, order.size());
        for (int i = 0; i < n; i++) {
            AbstractMonster m = order.get(i);
            if (i > 0) {
                sb.append("、");
            }
            sb.append("【").append(m.name).append("】");
            sb.append("（").append(targetReason(m)).append("）");
        }
        if (order.size() > 3) {
            sb.append("、其余");
        }
        if (aoe) {
            sb.append("。AOE 会打到全体，但别平均摊；先把高威胁目标压到斩杀线。");
        } else {
            sb.append("。不要三只平均打，先把一只打死的压力变成少挨一轮。");
        }
        return sb.toString();
    }

    public static String focusLine(AbstractMonster target, boolean aoe) {
        if (target == null) {
            return "";
        }
        return (aoe ? "AOE 优先关注【" : "优先目标【") + target.name + "】："
                + targetReason(target) + "。";
    }

    private static String targetReason(AbstractMonster m) {
        int t = threat(m);
        if (flight(m) > 0) {
            return "还有 " + flight(m) + " 层飞行，攻击减半，先打落地";
        }
        if (t >= 15) {
            return "这回合要打 " + t + "，威胁最高";
        }
        if (m.currentHealth <= 20) {
            return "血少，能收掉";
        }
        if (t > 0) {
            return "这回合要打 " + t;
        }
        return "血厚/会成长，先压它";
    }

    private static double genericValue(AbstractMonster m, boolean aoe) {
        int hp = m.currentHealth + m.currentBlock;
        double v = threat(m) * 2.0 + m.currentHealth * (aoe ? 0.03 : -0.02);
        if (!aoe) {
            v += hp <= 24 ? 4.0 : 0.0;
            v += m.currentHealth <= 12 ? 3.0 : 0.0;
        }
        if (flight(m) > 0) {
            v -= flight(m) * 0.8;
        }
        return v;
    }

    private static void sortByHpThenThreat(List<AbstractMonster> list) {
        Collections.sort(list, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                int ha = a.currentHealth + a.currentBlock;
                int hb = b.currentHealth + b.currentBlock;
                if (ha != hb) {
                    return Integer.compare(ha, hb);
                }
                return Integer.compare(threat(b), threat(a));
            }
        });
    }

    private static void sortPriority(List<AbstractMonster> list, final String needle) {
        Collections.sort(list, new Comparator<AbstractMonster>() {
            @Override
            public int compare(AbstractMonster a, AbstractMonster b) {
                boolean wa = key(a).contains(needle);
                boolean wb = key(b).contains(needle);
                if (wa != wb) {
                    return wa ? -1 : 1;
                }
                int ta = threat(a);
                int tb = threat(b);
                if (ta != tb) {
                    return Integer.compare(tb, ta);
                }
                return Integer.compare(a.currentHealth, b.currentHealth);
            }
        });
    }

    private static boolean all(List<AbstractMonster> list, String part) {
        for (AbstractMonster m : list) {
            if (!key(m).contains(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean any(List<AbstractMonster> list, String part) {
        for (AbstractMonster m : list) {
            if (key(m).contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static String key(AbstractMonster m) {
        String raw = (m == null ? "" : m.id + " " + m.name).toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int flight(AbstractMonster m) {
        AbstractPower power = m == null ? null : m.getPower("Flight");
        return power == null ? 0 : Math.max(0, power.amount);
    }

    private static int threat(AbstractMonster m) {
        if (m == null || m.intent == null) {
            return 0;
        }
        String intent = m.intent.name();
        if (intent.contains("ATTACK")) {
            int hits = 1;
            try {
                java.lang.reflect.Field f = AbstractMonster.class.getDeclaredField("intentMultiAmt");
                f.setAccessible(true);
                int n = f.getInt(m);
                if (n > 0) {
                    hits = n;
                }
            } catch (Throwable ignored) {
            }
            return Math.max(0, m.getIntentDmg()) * hits;
        }
        if (intent.contains("BUFF") || intent.contains("DEFEND")) {
            return 4;
        }
        return 1;
    }
}
