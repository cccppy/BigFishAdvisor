package bigfish.advisor;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic-programming route planner. It evaluates every legal continuation to
 * the top of the act and recommends the route with the best risk/reward for the
 * player's current HP, gold, deck, and act.
 */
public final class MapAdvisor {
    public static final class Result {
        public List<MapRoomNode> path = new ArrayList<MapRoomNode>();
        public MapRoomNode alternativeStart;
        public double score;
        public double alternativeScore;
        public String title;
        public String kicker;
        public List<String> lines = new ArrayList<String>();
        public int rest;
        public int shop;
        public int elite;
        public int event;
        public int monster;
        public int treasure;
    }

    private MapRoomNode[][] grid;
    private double[][] best;
    private MapRoomNode[][] next;
    private int rows;
    private int cols;

    public Result analyze(AbstractPlayer p) {
        if (p == null || AbstractDungeon.map == null || AbstractDungeon.map.isEmpty()) {
            return null;
        }
        buildGrid();
        if (rows == 0) {
            return null;
        }
        best = new double[rows][cols];
        next = new MapRoomNode[rows][cols];
        for (double[] row : best) {
            java.util.Arrays.fill(row, Double.NEGATIVE_INFINITY);
        }
        for (int y = rows - 1; y >= 0; y--) {
            for (int x = 0; x < cols; x++) {
                MapRoomNode node = grid[y][x];
                if (node == null) {
                    continue;
                }
                double base = nodeScore(node, y, p);
                double bestChild = Double.NEGATIVE_INFINITY;
                MapRoomNode bestChildNode = null;
                boolean hasChild = false;
                for (MapEdge e : node.getEdges()) {
                    if (e.dstY < 0 || e.dstY >= rows || e.dstX < 0 || e.dstX >= cols) {
                        continue;
                    }
                    MapRoomNode child = grid[e.dstY][e.dstX];
                    if (child == null || Double.isInfinite(best[e.dstY][e.dstX])) {
                        continue;
                    }
                    hasChild = true;
                    if (best[e.dstY][e.dstX] > bestChild) {
                        bestChild = best[e.dstY][e.dstX];
                        bestChildNode = child;
                    }
                }
                best[y][x] = base + (hasChild && bestChildNode != null ? bestChild : 0.0);
                next[y][x] = bestChildNode;
            }
        }

        Result r = new Result();
        MapRoomNode start = null;
        if (AbstractDungeon.firstRoomChosen && AbstractDungeon.currMapNode != null) {
            start = AbstractDungeon.currMapNode;
            if (start.y >= 0 && start.y < rows && start.x >= 0 && start.x < cols) {
                r.score = best[start.y][start.x];
            }
            MapRoomNode child = next[start.y][start.x];
            while (child != null) {
                r.path.add(child);
                child = next[child.y][child.x];
            }
        } else {
            double bestScore = Double.NEGATIVE_INFINITY;
            MapRoomNode second = null;
            double secondScore = Double.NEGATIVE_INFINITY;
            for (int x = 0; x < cols; x++) {
                MapRoomNode n = grid[0][x];
                if (n == null || Double.isInfinite(best[0][x])) {
                    continue;
                }
                double s = best[0][x];
                if (s > bestScore) {
                    secondScore = bestScore;
                    second = start;
                    bestScore = s;
                    start = n;
                } else if (s > secondScore) {
                    secondScore = s;
                    second = n;
                }
            }
            if (start != null) {
                r.score = bestScore;
                r.alternativeStart = second;
                r.alternativeScore = secondScore;
                r.path.add(start);
                MapRoomNode child = next[start.y][start.x];
                while (child != null) {
                    r.path.add(child);
                    child = next[child.y][child.x];
                }
            }
        }

        if (start == null) {
            return null;
        }
        count(r);
        r.kicker = String.format("路线建议 · 第 %d 层 · 第 %d 幕", AbstractDungeon.floorNum,
                Math.max(1, AbstractDungeon.actNum));
        r.title = AbstractDungeon.actNum == 1
                ? (r.monster < 4 ? "这条路战斗偏少，建议换一条"
                : "第一层先打弱怪，再冲营火/精英")
                : "走这条线";
        buildText(p, r);
        return r;
    }

    private void buildGrid() {
        rows = AbstractDungeon.map.size();
        cols = 7;
        grid = new MapRoomNode[rows][cols];
        for (int y = 0; y < rows; y++) {
            ArrayList<MapRoomNode> row = AbstractDungeon.map.get(y);
            for (MapRoomNode n : row) {
                if (n != null && n.x >= 0 && n.x < cols && n.y >= 0 && n.y < rows) {
                    grid[n.y][n.x] = n;
                }
            }
        }
    }

    private void count(Result r) {
        for (MapRoomNode n : r.path) {
            String t = type(n);
            if (t.equals("营火")) {
                r.rest++;
            } else if (t.equals("商店")) {
                r.shop++;
            } else if (t.equals("精英")) {
                r.elite++;
            } else if (t.equals("事件")) {
                r.event++;
            } else if (t.equals("宝箱")) {
                r.treasure++;
            } else if (t.equals("普通怪")) {
                r.monster++;
            }
        }
    }

    private double nodeScore(MapRoomNode node, int y, AbstractPlayer p) {
        String t = type(node);
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        boolean act1 = AbstractDungeon.actNum == 1;
        boolean earlyAct1 = act1 && y <= 5;
        double score;
        if (t.equals("营火")) {
            score = earlyAct1 ? 3.8 : 4.6;
            if (hpPct < 65) {
                score += 1.7;
            }
            if (y >= rows - 2) {
                score += 3.0;
            }
        } else if (t.equals("商店")) {
            score = 2.1;
            if (p.gold >= 120) {
                score += 1.2;
            } else if (p.gold < 80) {
                score -= 0.8;
            }
            if (y >= rows - 2) {
                score += 0.8;
            }
        } else if (t.equals("宝箱")) {
            score = 4.2;
        } else if (t.equals("事件")) {
            score = earlyAct1 ? 1.4 : 1.9;
        } else if (t.equals("精英")) {
            // Community route guides: Act 1 is the greedy resource act, but
            // early elites are only good after the first weak fights and a fire.
            score = 0.5;
            if (y < 4) {
                score -= 3.2;
            }
            if (hpPct < 60) {
                score -= 3.2;
            } else if (hpPct > 72) {
                score += 0.9;
            }
            DeckSummary deck = DeckSummary.of(p);
            if (deck.size >= 15 && deck.attacks >= 6 && deck.blockCards >= 5) {
                score += 1.3;
            } else {
                score -= 1.2;
            }
            if (AbstractDungeon.actNum >= 2) {
                score += 0.6;
            }
            if (deck.aoeCards > 0) {
                score += 0.4;
            }
            score += ActAdvisor.eliteReadiness(p) * 1.1;
            if (AbstractDungeon.ascensionLevel >= 10) {
                score -= 0.8;
            }
        } else if (t.equals("普通怪")) {
            // Act 1's first three fights draw from the weak pool: they are the
            // cheapest deck-building resource before elites/boss.
            score = earlyAct1 ? 2.2 : 0.75;
            if (earlyAct1 && p.masterDeck.size() < 12) {
                score += 0.6;
            }
            if (hpPct < 45) {
                score -= earlyAct1 ? 0.4 : 0.7;
            }
        } else {
            score = 0.0;
        }
        return score;
    }

    private void buildText(AbstractPlayer p, Result r) {
        int hpPct = p.currentHealth * 100 / Math.max(1, p.maxHealth);
        StringBuilder route = new StringBuilder();
        int shown = 0;
        for (MapRoomNode n : r.path) {
            if (shown > 0) {
                route.append(" -> ");
            }
            route.append(type(n));
            shown++;
        }
        r.lines.add("大肥鱼建议：" + (route.length() == 0 ? "已到 Boss 前" : route.toString()));
        r.lines.add(String.format("现在：生命 %d/%d（%d%%）· 金币 %d · 牌组 %d 张 · %s",
                p.currentHealth, p.maxHealth, hpPct, p.gold, p.masterDeck.size(), DeckSummary.of(p).archetype()));
        r.lines.add("为什么呢：" + routeReason(p, r, hpPct));
        r.lines.add(ActAdvisor.bossLine(p));
        r.lines.add(ActAdvisor.eliteLine(p));
        if (AbstractDungeon.actNum == 1) {
            r.lines.add("第一层节奏：前 3 场是弱怪，先打它们抓过渡牌；然后优先营火升级，再考虑精英/商店。");
            if (r.monster < 4) {
                r.lines.add("警告：这条路线普通怪只有 " + r.monster
                        + " 场，第一层发育会不足；如果另一条线多 1-2 场战斗，优先换。");
            }
            if (r.elite == 0) {
                r.lines.add("警告：这幕路线没有精英，会少遗物/金币；牌组够硬时可以换带精英的线。");
            }
        }
        r.lines.add("你的牌组现在：" + DeckSummary.of(p).needs(AbstractDungeon.actNum).get(0));
        if (!r.path.isEmpty()) {
            StringBuilder plan = new StringBuilder("这条路我会这么走：");
            for (int i = 0; i < r.path.size() && i < 5; i++) {
                if (i > 0) {
                    plan.append("；");
                }
                String t = type(r.path.get(i));
                plan.append("第 ").append(i + 1).append(" 步 ").append(t).append("：").append(nodeHint(t, i));
            }
            r.lines.add(plan.toString());
        }
        r.lines.add(String.format("要小心的是：生命 %d%%，%s；金币 %d，%s。",
                hpPct,
                hpPct < 55 ? "再撞精英容易暴毙" : "血线允许吃一场精英",
                p.gold,
                p.gold >= 120 ? "商店可以买关键牌/删牌" : "金币偏少，商店优先删牌而不是乱买"));
        String stats = String.format("路线统计：%d 战 / %d 营火 / %d 商店 / %d 事件 / %d 精英",
                r.monster, r.rest, r.shop, r.event, r.elite);
        if (r.treasure > 0) {
            stats += " / " + r.treasure + " 宝箱";
        }
        r.lines.add(stats);
        if (r.alternativeStart != null && !Double.isInfinite(r.alternativeScore)
                && r.score - r.alternativeScore < 9.0) {
            r.lines.add(String.format("备选：从第 %d 列起步（差 %.1f 分）；两条都能走，优先保血。",
                    r.alternativeStart.x + 1, r.score - r.alternativeScore));
        }
    }

    private String nodeHint(String type, int index) {
        if (type.equals("普通怪")) {
            return index < 3 ? "弱怪池，低战损补牌/金币" : "强怪池，检验当前输出和防御";
        }
        if (type.equals("营火")) {
            return "优先升级核心牌，血低才休息";
        }
        if (type.equals("精英")) {
            return "给遗物，但先确认血量和牌组能打";
        }
        if (type.equals("商店")) {
            return "删废牌/补关键牌，别为了花钱而买";
        }
        if (type.equals("事件")) {
            return "可能给资源，也可能出怪；别连续赌太多";
        }
        if (type.equals("宝箱")) {
            return "免费遗物，顺路拿";
        }
        return "按当前资源决定";
    }

    private String routeReason(AbstractPlayer p, Result r, int hpPct) {
        List<String> reasons = new ArrayList<String>();
        if (AbstractDungeon.actNum == 1 && r.monster >= 3) {
            reasons.add("第一层前半多打普通怪，先补牌/金币/药水，牌组才不会一路裸奔");
        }
        if (r.elite == 0) {
            reasons.add("这条线不碰精英，适合当前 " + (hpPct < 65 ? "血量偏低" : "求稳") + " 的状态");
        } else if (hpPct >= 75) {
            reasons.add("有 " + r.elite + " 个精英，但血量健康，可以换遗物");
        } else {
            reasons.add("有 " + r.elite + " 个精英，血量不健康时会很危险");
        }
        if (r.rest > 0) {
            reasons.add(r.rest + " 个营火可回血/升级");
        }
        if (r.shop > 0 && p.gold >= 100) {
            reasons.add("商店能花掉 " + p.gold + " 金币补关键牌");
        }
        if (AbstractDungeon.actNum == 1 && r.monster < 4) {
            reasons.add("第一层普通怪只有 " + r.monster + " 场，牌组/金币成长会不够");
        } else if (r.monster <= 2) {
            reasons.add("战斗偏少，牌组成长可能不够");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size() && i < 2; i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }

    public static String type(MapRoomNode n) {
        if (n == null || n.getRoom() == null) {
            return "未知";
        }
        String cls = n.getRoom().getClass().getSimpleName();
        if (cls.equals("RestRoom")) {
            return "营火";
        }
        if (cls.equals("ShopRoom")) {
            return "商店";
        }
        if (cls.equals("MonsterRoomElite")) {
            return "精英";
        }
        if (cls.equals("MonsterRoomBoss")) {
            return "Boss";
        }
        if (cls.equals("MonsterRoom")) {
            return "普通怪";
        }
        if (cls.equals("EventRoom")) {
            return "事件";
        }
        if (cls.equals("TreasureRoom")) {
            return "宝箱";
        }
        if (cls.equals("TreasureRoomBoss")) {
            return "Boss 宝箱";
        }
        if (cls.equals("TrueVictoryRoom") || cls.equals("VictoryRoom")) {
            return "胜利";
        }
        return "未知";
    }
}
