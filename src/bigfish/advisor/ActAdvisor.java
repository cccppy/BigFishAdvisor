package bigfish.advisor;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Boss/elite scouting and pre-building advice for the current act. */
public final class ActAdvisor {
    private ActAdvisor() {
    }

    public static String bossName() {
        return AbstractDungeon.bossKey == null ? "未知 Boss" : AbstractDungeon.bossKey;
    }

    public static String bossLine(AbstractPlayer p) {
        String key = norm(bossName());
        String nick = "";
        String mechanic = "";
        String prepare = "";
        if (key.contains("slimeboss")) {
            nick = "史莱姆王";
            mechanic = "半血分裂成两只，会塞黏液、撞击越来越疼";
            prepare = "留 AOE 处理分裂，别在分裂前把能量全交光；单体爆发也要够";
        } else if (key.contains("guardian")) {
            nick = "守护者";
            mechanic = "会在攻击/防御形态间切换，防御形态很硬";
            prepare = "带持续格挡/成长，防御形态别硬砸；等它切回攻击形态再爆发";
        } else if (key.contains("hexaghost")) {
            nick = "六火";
            mechanic = "六连击、灼烧、半血左右的分隔重击";
            prepare = "准备好大格挡，燃烧牌别乱打；血线低时优先保命";
        } else if (key.contains("automaton")) {
            nick = "铜人 / 自动机";
            mechanic = "召球、充能、激光，激光回合特别疼";
            prepare = "激光前留大格挡；有 AOE 清球更好";
        } else if (key.contains("collector")) {
            nick = "收藏家";
            mechanic = "召火炬头、叠力量、重击";
            prepare = "准备 AOE 清小怪，或者抢在力量叠高前压死本体";
        } else if (key.contains("champ")) {
            nick = "冠军";
            mechanic = "嘲讽、防御、半血后处决";
            prepare = "半血前留好格挡，别在处决回合硬吃；有爆发就压血";
        } else if (key.contains("timeeater")) {
            nick = "时间老头 / 蜗牛";
            mechanic = "每打出 12 张牌就结束你的回合";
            prepare = "少打低价值小牌，把高影响力牌留到关键回合；算好 12 张上限";
        } else if (key.contains("awakenedone")) {
            nick = "觉醒者";
            mechanic = "会复活，第二阶段更强；会惩罚能力牌";
            prepare = "第一条命别交光药水；能力牌别开太多，第二条命再爆发";
        } else if (key.contains("donu") || key.contains("deca")) {
            nick = "甜甜圈 / 方片";
            mechanic = "两只互相强化，拖久两只都难打";
            prepare = "集中杀一只，别平均打；优先解决正在叠力量的那只";
        } else if (key.contains("heart")) {
            nick = "心脏";
            mechanic = "每打一张牌掉血，多段连击，有伤害上限";
            prepare = "少打低价值牌，多段攻击前留够格挡；有虚弱/无实体/格挡引擎更好";
        }
        if (nick.length() == 0) {
            return "本幕 Boss：" + bossName() + "。先按当前意图和牌组短板准备，别等到 Boss 前才补防御。";
        }
        return "本幕 Boss：" + bossName() + "（" + nick + "）——" + mechanic + "。准备：" + prepare;
    }

    public static String eliteLine(AbstractPlayer p) {
        List<String> names = new ArrayList<String>();
        if (AbstractDungeon.eliteMonsterList != null) {
            for (String s : AbstractDungeon.eliteMonsterList) {
                if (s != null && s.length() > 0) {
                    names.add(nick(s));
                }
            }
        }
        if (names.isEmpty()) {
            return "本幕精英：未知；按当前牌组短板决定要不要绕。";
        }
        double readiness = eliteReadiness(p);
        String verdict = readiness > 0.35 ? "当前牌组可以打"
                : readiness < -0.25 ? "当前牌组偏危险" : "看血量和药水决定";
        return "本幕可能遇到精英：" + join(names) + "；" + verdict + "。";
    }

    public static double eliteReadiness(AbstractPlayer p) {
        if (p == null || AbstractDungeon.eliteMonsterList == null) {
            return 0.0;
        }
        DeckSummary d = DeckSummary.of(p);
        double best = d.blockCards >= 6 ? 0.0 : -0.3;
        for (String raw : AbstractDungeon.eliteMonsterList) {
            String key = norm(raw);
            double s = 0.0;
            if (key.contains("gremlinnob")) {
                s = d.attacks >= 6 ? 0.4 : -0.4;
                if (d.skills >= 7) {
                    s -= 0.3;
                }
            } else if (key.contains("lagavulin")) {
                s = (d.tag("poison") >= 2 || d.attacks >= 7) ? 0.35 : -0.2;
            } else if (key.contains("sentry")) {
                s = d.aoeCards > 0 ? 0.5 : -0.3;
                if (d.drawCards >= 2) {
                    s += 0.15;
                }
            } else if (key.contains("gremlinleader") || key.contains("reptomancer")
                    || key.contains("slavers")) {
                s = d.aoeCards > 0 ? 0.5 : -0.35;
            } else if (key.contains("bookofstabbing")) {
                s = d.blockCards >= 6 ? 0.4 : -0.2;
            } else if (key.contains("gianthead")) {
                s = (d.tag("str") >= 2 || d.tag("poison") >= 2 || d.powers >= 3) ? 0.4 : -0.2;
            } else if (key.contains("nemesis")) {
                s = (d.blockCards >= 6 || d.tag("poison") >= 3) ? 0.3 : -0.2;
            }
            if (s > best) {
                best = s;
            }
        }
        return best;
    }

    /** Card bonus for the current act's boss/elite roster. */
    public static double cardBonus(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return 0.0;
        }
        double b = 0.0;
        String boss = norm(bossName());
        if (boss.contains("slimeboss")) {
            if (CardDatabase.hasTag(c, "aoe")) {
                b += 0.45;
            }
            if (CardDatabase.hasTag(c, "block")) {
                b += 0.15;
            }
        } else if (boss.contains("guardian")) {
            if (CardDatabase.hasTag(c, "block")) {
                b += 0.35;
            }
            if (c.type == AbstractCard.CardType.POWER) {
                b += 0.15;
            }
        } else if (boss.contains("hexaghost")) {
            if (CardDatabase.hasTag(c, "block")) {
                b += 0.3;
            }
            if (c.type == AbstractCard.CardType.ATTACK) {
                b += 0.15;
            }
        } else if (boss.contains("automaton")) {
            if (CardDatabase.hasTag(c, "block") || CardDatabase.hasTag(c, "aoe")) {
                b += 0.3;
            }
        } else if (boss.contains("collector")) {
            if (CardDatabase.hasTag(c, "aoe")) {
                b += 0.45;
            }
        } else if (boss.contains("champ")) {
            if (CardDatabase.hasTag(c, "block")) {
                b += 0.3;
            }
            if (c.type == AbstractCard.CardType.ATTACK) {
                b += 0.15;
            }
        } else if (boss.contains("timeeater")) {
            if (c.costForTurn >= 2 || c.cost >= 2) {
                b += 0.3;
            }
            if (c.costForTurn == 0) {
                b -= 0.25;
            }
        } else if (boss.contains("awakenedone")) {
            if (c.type == AbstractCard.CardType.POWER) {
                b -= 0.2;
            }
            if (CardDatabase.hasTag(c, "block")) {
                b += 0.15;
            }
        } else if (boss.contains("donu") || boss.contains("deca")) {
            if (c.type == AbstractCard.CardType.ATTACK) {
                b += 0.2;
            }
        } else if (boss.contains("heart")) {
            if (CardDatabase.hasTag(c, "block")) {
                b += 0.35;
            }
            if (CardDatabase.hasTag(c, "weak")) {
                b += 0.2;
            }
        }

        if (AbstractDungeon.eliteMonsterList != null) {
            for (String raw : AbstractDungeon.eliteMonsterList) {
                String key = norm(raw);
                if (key.contains("gremlinnob")) {
                    if (c.type == AbstractCard.CardType.ATTACK) {
                        b += 0.25;
                    } else if (c.type == AbstractCard.CardType.SKILL) {
                        b -= 0.25;
                    }
                } else if (key.contains("sentry") && CardDatabase.hasTag(c, "aoe")) {
                    b += 0.3;
                } else if (key.contains("bookofstabbing") && CardDatabase.hasTag(c, "block")) {
                    b += 0.25;
                } else if ((key.contains("gremlinleader") || key.contains("reptomancer")
                        || key.contains("slavers")) && CardDatabase.hasTag(c, "aoe")) {
                    b += 0.3;
                }
            }
        }
        return b;
    }

    public static String cardNote(AbstractPlayer p, AbstractCard c) {
        if (p == null || c == null) {
            return "";
        }
        String boss = norm(bossName());
        if (boss.contains("slimeboss") && CardDatabase.hasTag(c, "aoe")) {
            return "打史莱姆王需要 AOE 处理分裂";
        }
        if (boss.contains("guardian") && CardDatabase.hasTag(c, "block")) {
            return "守护者防御形态需要持续格挡";
        }
        if (boss.contains("hexaghost") && CardDatabase.hasTag(c, "block")) {
            return "六火的大招需要格挡";
        }
        if (boss.contains("collector") && CardDatabase.hasTag(c, "aoe")) {
            return "收藏家召小怪，需要 AOE";
        }
        if (boss.contains("timeeater") && (c.costForTurn >= 2 || c.cost >= 2)) {
            return "时间老头有 12 张牌限制，高影响力牌更值";
        }
        if (boss.contains("heart") && CardDatabase.hasTag(c, "block")) {
            return "心脏多段连击，格挡很重要";
        }
        if (AbstractDungeon.eliteMonsterList != null) {
            for (String raw : AbstractDungeon.eliteMonsterList) {
                String key = norm(raw);
                if (key.contains("gremlinnob") && c.type == AbstractCard.CardType.ATTACK) {
                    return "乐嘉惩罚技能，攻击牌更稳";
                }
                if (key.contains("sentry") && CardDatabase.hasTag(c, "aoe")) {
                    return "哨卫需要集中/群体伤害";
                }
                if (key.contains("bookofstabbing") && CardDatabase.hasTag(c, "block")) {
                    return "刺书多段攻击，格挡很关键";
                }
            }
        }
        return "";
    }

    private static String nick(String raw) {
        String key = norm(raw);
        if (key.contains("gremlinnob")) {
            return "乐嘉";
        }
        if (key.contains("lagavulin")) {
            return "睡美人";
        }
        if (key.contains("sentry")) {
            return "三哨卫";
        }
        if (key.contains("gremlinleader")) {
            return "地精头子";
        }
        if (key.contains("bookofstabbing")) {
            return "刺书";
        }
        if (key.contains("slavers")) {
            return "三奴隶";
        }
        if (key.contains("gianthead")) {
            return "大头";
        }
        if (key.contains("nemesis")) {
            return "复仇女神";
        }
        if (key.contains("reptomancer")) {
            return "蛇女";
        }
        return raw;
    }

    private static String join(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size() && i < 4; i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private static String norm(String s) {
        return CardDatabase.norm(s == null ? "" : s.toLowerCase(Locale.ROOT));
    }
}
