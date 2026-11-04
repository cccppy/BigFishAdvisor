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
        if (any(list, "gremlinwizard") || any(list, "wizard")) {
            sortPriority(list, "wizard");
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "先杀地精法师：它会塞状态/充能，拖越久越难打；"
                    + "其他小地精可以后处理，有 AOE 就一起压。";
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
        if (any(list, "torchhead")) {
            sortPriority(list, "torchhead");
            f.target = list.get(0);
            f.order.addAll(list);
            f.reason = "先清火炬头：它们会一起输出，别让收藏家白召；AOE 压小怪后再打本体。";
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
