package bigfish.advisor;

import com.megacrit.cardcrawl.monsters.AbstractMonster;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Short, practical enemy notes: current intent, known passive/pattern, and what
 * to answer it with. Names/nicknames are intentionally community-style.
 */
public final class EnemyAdvisor {
    private static Field multiField;

    private EnemyAdvisor() {
    }

    public static List<String> lines(List<AbstractMonster> alive) {
        List<String> out = new ArrayList<String>();
        for (AbstractMonster m : alive) {
            Profile p = profile(m);
            out.add("【" + m.name + (p.nick.length() == 0 ? "" : "｜" + p.nick) + "】剩 "
                    + m.currentHealth + " 血。当前：" + intent(m) + "。"
                    + (p.passive.length() == 0 ? "" : "被动：" + p.passive + "。"));
            if (p.pattern.length() > 0) {
                out.add("接下来大概会：" + p.pattern);
            }
            out.add("怎么应对：" + (p.counter.length() == 0 ? "看当前意图决定攻防；优先处理威胁最高的。" : p.counter));
        }
        return out;
    }

    private static final class Profile {
        String nick = "";
        String passive = "";
        String pattern = "";
        String counter = "";
    }

    private static Profile profile(AbstractMonster m) {
        String key = norm(m.id + " " + m.name);
        Profile p = new Profile();
        if (key.contains("jawworm")) {
            p.nick = "大颚虫 / 咬咬虫";
            p.passive = "会咆哮叠力量";
            p.pattern = "啃咬、甩尾、咆哮轮着来；咆哮后攻击会越来越疼";
            p.counter = "别让它舒服地咆哮；用高单次伤害破格挡，别拿多段小攻击硬喂";
        } else if (key.contains("cultist")) {
            p.nick = "邪教徒 / 咔咔";
            p.passive = "仪式：每回合叠力量";
            p.pattern = "第一回合先上仪式，之后一直打；拖越久越疼";
            p.counter = "优先秒掉；秒不掉就每回合留格挡，别跟它比成长";
        } else if (key.contains("louse")) {
            p.nick = "虱虫";
            p.passive = "绿虱会蜷缩加格挡，红虱会带荆棘";
            p.pattern = "啃咬和蜷缩/荆棘交替；红虱会惩罚多段攻击";
            p.counter = "绿虱用高单次伤害破格挡；红虱别用多段小攻击，先杀威胁大的";
        } else if (key.contains("fungibeast")) {
            p.nick = "真菌怪";
            p.passive = "会生长叠力量，死亡时可能给你易伤";
            p.pattern = "啃咬 + 生长；越拖攻击越高";
            p.counter = "别让它白生长；要杀就集中杀，注意死亡时身上的孢子";
        } else if (key.contains("gremlinnob")) {
            p.nick = "地精大块头 / 乐嘉";
            p.passive = "激怒：你每打一张技能牌，它加力量";
            p.pattern = "重击/猛冲，血越少打越狠";
            p.counter = "尽量用攻击牌打，少打技能；必须防御时只打高价值格挡，争取几回合内秒掉";
        } else if (key.contains("lagavulin")) {
            p.nick = "乐嘉 / 睡美人";
            p.passive = "睡着时免伤；被攻击会醒，醒后叠金属化/力量";
            p.pattern = "睡 3 回合，醒来后重击；拖久会越来越硬";
            p.counter = "要么前几回合安心铺垫、等它醒；要么确认能爆发秒掉再提前叫醒，别用小伤害挠醒它";
        } else if (key.contains("sentry")) {
            p.nick = "哨卫";
            p.passive = "有人工制品，会往你手里塞眩晕";
            p.pattern = "攻击和塞眩晕交替；死一个后其余会加力量";
            p.counter = "优先集中杀一个，别平均摊伤害；有 AOE/多段更好，眩晕多了先过牌";
        } else if (key.contains("hexaghost")) {
            p.nick = "六火 / 火鬼";
            p.passive = "会按血量切换大招，往牌组塞燃烧";
            p.pattern = "六连击、灼烧、分隔重击；半血左右大招很疼";
            p.counter = "大招前把格挡留足；燃烧牌别乱打，血线低时优先保命";
        } else if (key.contains("slimeboss")) {
            p.nick = "史莱姆王";
            p.passive = "半血会分裂成两只";
            p.pattern = "史莱姆撞击、塞黏液；分裂后压力更大";
            p.counter = "留 AOE 处理分裂；别在分裂前把能量全交光";
        } else if (key.contains("guardian")) {
            p.nick = "守护者";
            p.passive = "会切防御形态，形态下很硬";
            p.pattern = "攻击/防御形态切换，防御形态会反伤";
            p.counter = "防御形态别硬砸，先叠格挡/过牌；等它切回攻击形态再爆发";
        } else if (key.contains("champ")) {
            p.nick = "冠军";
            p.passive = "会嘲讽、叠力量，半血后处决";
            p.pattern = "重击、防御、嘲讽；半血后伤害飙升";
            p.counter = "半血前把格挡/削弱准备好；别在它处决回合硬吃";
        } else if (key.contains("bookofstabbing")) {
            p.nick = "刺书";
            p.passive = "多段攻击会越来越多";
            p.pattern = "连续多段刺击，回合越久段数越高";
            p.counter = "每回合都要有格挡；有虚弱/力量削弱/无实体就交，别省";
        } else if (key.contains("gremlin") && !key.contains("gremlinleader")
                && !key.contains("gremlinnob")) {
            p.nick = "小地精";
            p.passive = "愤怒地精受伤会加力量；狡猾地精会跑；法师会塞状态";
            p.pattern = "小地精各自攻击/加 buff，法师在后排捣乱";
            p.counter = "优先杀法师和愤怒地精，别让它们叠起来；有 AOE 就一起压血";
        } else if (key.contains("slime")) {
            p.nick = "史莱姆";
            p.passive = "酸液/尖刺史莱姆会塞黏液、给虚弱；部分会分裂";
            p.pattern = "撞击、吐酸、分裂；拖久手牌会被黏液卡住";
            p.counter = "有 AOE 就清场；没有就集中秒一只，别让分裂后越打越多";
        } else if (key.contains("thief")) {
            p.nick = "盗贼";
            p.passive = "会偷金币，偷够就跑";
            p.pattern = "偷钱、逃跑、攻击";
            p.counter = "优先杀，别让它带着你的金币跑掉；跑掉的金币不会回来";
        } else if (key.contains("gremlinleader")) {
            p.nick = "地精头子";
            p.passive = "会给小地精加 buff，小地精死了它会变强";
            p.pattern = "召唤/强化小地精，然后一起打";
            p.counter = "优先 AOE 清小地精或直接压头子血；别让小地精白打太多轮";
        } else if (key.contains("automaton")) {
            p.nick = "铜人 / 自动机";
            p.passive = "会召球，充能后打激光";
            p.pattern = "召唤球、充能、激光；激光回合特别疼";
            p.counter = "激光前留大格挡；能清球就清，别让充能叠太高";
        } else if (key.contains("collector")) {
            p.nick = "收藏家";
            p.passive = "会召火炬头，自己叠力量";
            p.pattern = "召唤、强化、重击；小怪会一起输出";
            p.counter = "AOE 清小怪，或者抢在力量叠高前压死本体";
        } else if (key.contains("timeeater")) {
            p.nick = "时间老头 / 蜗牛";
            p.passive = "每打出 12 张牌就结束你的回合";
            p.pattern = "叠力量、打重击、卡牌数限制";
            p.counter = "别无脑刷小牌；把牌留到关键回合，算好 12 张上限再爆发";
        } else if (key.contains("awakenedone")) {
            p.nick = "觉醒者";
            p.passive = "会复活，第二阶段更强；会惩罚能力牌";
            p.pattern = "第一阶段倒下后复活，第二阶段多段/重击";
            p.counter = "第一条命别把关键药水全交；第二条命再爆发，能力牌别开太多";
        } else if (key.contains("donu") || key.contains("deca")) {
            p.nick = "甜甜圈 / 方片";
            p.passive = "两只互相强化，会给彼此加力量/格挡";
            p.pattern = "一只强化另一只，拖久两只都难打";
            p.counter = "集中杀一只，别平均打；优先解决正在叠力量的那只";
        } else if (key.contains("corruptheart") || key.contains("heart")) {
            p.nick = "心脏";
            p.passive = "每打一张牌掉血；有伤害上限/无敌层";
            p.pattern = "多段连击、塞状态、强制你控牌数";
            p.counter = "少打低价值牌，算好每张牌的血量代价；多段攻击前留够格挡";
        } else if (key.contains("byrd")) {
            p.nick = "鸟";
            p.passive = "会飞，偶尔减伤";
            p.pattern = "多段啄击/飞行，群怪一起打很疼";
            p.counter = "AOE 或先杀一只；每回合留格挡防多段";
        } else if (key.contains("chosen")) {
            p.nick = "被选者";
            p.passive = "会塞诅咒/虚无牌";
            p.pattern = "诅咒、攻击、强化交替";
            p.counter = "优先杀；手里被塞诅咒时先算好抽牌，别让关键牌被卡掉";
        } else if (key.contains("taskmaster")) {
            p.nick = "工头 / 监工";
            p.passive = "会上易伤，让后续攻击更疼";
            p.pattern = "鞭打/上易伤，和另外两只奴隶一起压";
            p.counter = "三奴隶里先杀工头，别让易伤叠起来；再处理红/蓝奴隶";
        } else if (key.contains("looter") || key.contains("mugger")) {
            p.nick = key.contains("mugger") ? "强盗" : "盗贼";
            p.passive = "会偷金币，偷够就跑";
            p.pattern = "偷钱、攻击、准备逃跑";
            p.counter = "优先杀血少/准备逃跑的那只；跑掉的金币不会回来";
        } else if (key.contains("banditleader")) {
            p.nick = "强盗头目 / Romeo";
            p.passive = "会给全体加力量/强化";
            p.pattern = "先强化，再配合 Pointy/Bear 输出";
            p.counter = "先杀 Romeo，别让它把两只强盗养起来；Pointy 输出高，Bear 比较肉";
        } else if (key.contains("banditpointy")) {
            p.nick = "尖头强盗 / Pointy";
            p.passive = "高单体输出";
            p.pattern = "连续刺击，血少但很疼";
            p.counter = "Romeo 死后优先处理 Pointy；留够格挡防连续攻击";
        } else if (key.contains("banditbear")) {
            p.nick = "熊 / Bear";
            p.passive = "血厚、会保护同伴";
            p.pattern = "重击/格挡，拖久很烦";
            p.counter = "先杀 Romeo/Pointy，最后处理熊；别把爆发浪费在它身上";
        } else if (key.contains("exploder")) {
            p.nick = "爆炸怪";
            p.passive = "几回合后会自爆，伤害很高";
            p.pattern = "充能后爆炸；不打它也会爆";
            p.counter = "先杀爆炸怪，别让它白爆；有 AOE 就一起清";
        } else if (key.contains("repulsor")) {
            p.nick = "排斥怪";
            p.passive = "会上虚弱/易伤，干扰输出";
            p.pattern = "debuff + 攻击";
            p.counter = "先处理排斥怪，别让虚弱把你的爆发拖没；爆炸怪优先级更高";
        } else if (key.contains("spiker")) {
            p.nick = "尖刺怪";
            p.passive = "有荆棘，打它会反伤";
            p.pattern = "缩起来反伤/攻击";
            p.counter = "别用多段小攻击喂荆棘；用高单次伤害或等它不缩时打";
        } else if (key.contains("sphericguardian")) {
            p.nick = "球体守卫";
            p.passive = "开局有屏障/人工制品，硬打会吃减伤";
            p.pattern = "屏障、攻击、上负面交替";
            p.counter = "先破屏障，别把低伤害分散；破盾后集中输出，注意人工制品挡 debuff";
        } else if (key.contains("shelledparasite")) {
            p.nick = "甲壳寄生体";
            p.passive = "有覆甲，每回合加格挡；会吸血/上负面";
            p.pattern = "覆甲 + 攻击 + 吸血";
            p.counter = "先用多段攻击剥覆甲，再用高单次伤害打本体；别让它靠覆甲拖回合";
        } else if (key.contains("snecko")) {
            p.nick = "蛇眼";
            p.passive = "混乱：每回合手牌费用随机";
            p.pattern = "咬击/上混乱，费用不再按原牌面";
            p.counter = "别按原费规划；优先打当前费用低/能立刻形成格挡或输出的牌";
        } else if (key.contains("reptomancer")) {
            p.nick = "蛇女 / 爬行者";
            p.passive = "会召唤匕首，匕首会爆炸";
            p.pattern = "召唤、双咬、重咬；匕首越多越危险";
            p.counter = "先用 AOE/多段清匕首，别让匕首堆到 4 把；清完再打本体";
        } else if (key.contains("nemesis")) {
            p.nick = "复仇女神";
            p.passive = "每隔一回合进入无实体，硬打很亏";
            p.pattern = "无实体/攻击交替，还会塞幽灵";
            p.counter = "无实体回合别硬砸，叠格挡/过牌；等实体回合再爆发";
        } else if (key.contains("gianthead")) {
            p.nick = "大头";
            p.passive = "有 Slow：你每打一张牌它就少受一点伤害";
            p.pattern = "前期慢，后期重击；拖久很疼";
            p.counter = "别用一堆小牌刷它；攒好资源，在它重击前留大格挡/爆发";
        } else if (key.contains("writhingmass")) {
            p.nick = "蠕动之物";
            p.passive = "被打会加格挡/塞寄生虫，越打越硬";
            p.pattern = "攻击、塞诅咒、叠格挡";
            p.counter = "别用多段小攻击喂它；算好斩杀线，用高单次伤害一次压死";
        } else if (key.contains("spireshield")) {
            p.nick = "尖塔盾";
            p.passive = "会给全体加格挡，自己很肉";
            p.pattern = "重击/加盾，和长矛一起打";
            p.counter = "先杀长矛，再处理盾；别把爆发浪费在它的格挡上";
        } else if (key.contains("spirespear")) {
            p.nick = "尖塔矛";
            p.passive = "会给全体加力量、塞燃烧";
            p.pattern = "燃烧攻击、穿刺、强化";
            p.counter = "第四幕精英先杀长矛，拆掉全体力量/燃烧；再打盾";
        } else if (key.contains("bronzeorb")) {
            p.nick = "青铜球";
            p.passive = "会偷牌/干扰，和铜人一起出现";
            p.pattern = "光束/偷牌，拖久会打乱牌组";
            p.counter = "先清球再打铜人；球清掉后留大格挡防 Hyper Beam";
        } else if (key.contains("torchhead")) {
            p.nick = "火炬头";
            p.passive = "被收藏家强化，每回合输出";
            p.pattern = "撞击，收藏家会给它加力量";
            p.counter = "先清火炬头，别让收藏家白强化；AOE 可同时压本体";
        } else if (key.contains("dagger")) {
            p.nick = "匕首";
            p.passive = "会爆炸，数量越多越危险";
            p.pattern = "攻击/爆炸，蛇女会不断召唤";
            p.counter = "优先用 AOE/多段清匕首，别让它们堆到 4 把";
        } else if (key.contains("centurion") || key.contains("mystic")) {
            p.nick = key.contains("mystic") ? "祭司" : "百夫长";
            p.passive = "祭司会给百夫长回血/加 buff；百夫长负责输出";
            p.pattern = "祭司治疗/强化，百夫长攻击；拖久很难打死";
            p.counter = "优先杀祭司，别让治疗循环起来；有 AOE 就一起压";
        } else if (key.contains("snakeplant")) {
            p.nick = "蛇花";
            p.passive = "会削敏捷/塞状态";
            p.pattern = "多段攻击 + 削弱";
            p.counter = "先保格挡，别让敏捷被削后硬吃多段";
        } else if (key.contains("mugger") || key.contains("slaver")) {
            p.nick = key.contains("mugger") ? "强盗" : "奴隶主";
            p.passive = "强盗会偷钱逃跑，奴隶主会缠绕";
            p.pattern = "攻击、偷钱/缠绕、准备逃跑";
            p.counter = "优先杀，别让强盗跑掉；被缠绕时先打非攻击牌/防御";
        } else if (key.contains("transient")) {
            p.nick = "瞬变";
            p.passive = "会很快消失，但伤害极高";
            p.pattern = "每回合高伤，几回合后自己消失";
            p.counter = "不用杀，活着拖过去就行；格挡/虚弱/无实体优先";
        } else if (key.contains("maw")) {
            p.nick = "大嘴";
            p.passive = "会咆哮叠力量";
            p.pattern = "咆哮 + 重击，越拖越疼";
            p.counter = "咆哮后立刻准备大格挡；有爆发就早点压血";
        } else if (key.contains("darkling")) {
            p.nick = "暗灵";
            p.passive = "会复活，三只一起打";
            p.pattern = "多段攻击、复活";
            p.counter = "AOE 压三只血线，尽量同一回合收掉，别让它们轮流复活";
        } else if (key.contains("orbwalker")) {
            p.nick = "球行者";
            p.passive = "会充能打激光";
            p.pattern = "充能后高伤激光";
            p.counter = "激光前留格挡；能打断/秒掉就优先";
        } else if (key.contains("spiregrowth")) {
            p.nick = "尖塔增生";
            p.passive = "会缠绕，持续掉血";
            p.pattern = "缠绕 + 重击";
            p.counter = "被缠绕后尽快杀；每回合留格挡，别拖";
        }

        // Visible powers are useful even when the name/profile is unfamiliar.
        String powers = powers(m);
        if (powers.length() > 0) {
            p.passive = p.passive.length() == 0 ? powers : p.passive + "；" + powers;
        }
        if (p.pattern.length() == 0) {
            p.pattern = "看当前意图；同类怪的行为一般有固定循环，别只看这一回合";
        }
        if (p.counter.length() == 0) {
            p.counter = "按当前意图应对：攻击就挡/杀，增益就抢节奏，塞状态就先过牌";
        }
        return p;
    }

    private static String intent(AbstractMonster m) {
        if (m.intent == null) {
            return "意图不明";
        }
        String intent = m.intent.name();
        if (intent.contains("ATTACK")) {
            int hits = multi(m);
            return "要打你 " + Math.max(0, m.getIntentDmg()) * hits + " 伤"
                    + (hits > 1 ? "（" + hits + " 段）" : "");
        }
        if (intent.contains("DEFEND")) {
            return "在防御/加格挡";
        }
        if (intent.contains("BUFF")) {
            return "在给自己加增益";
        }
        if (intent.contains("DEBUFF")) {
            return "要给你上负面状态";
        }
        if (intent.contains("MAGIC")) {
            return "在蓄力，下一波可能很疼";
        }
        if (intent.contains("SLEEP")) {
            return "还在睡";
        }
        if (intent.contains("STUN")) {
            return "这回合晕着";
        }
        if (intent.contains("ESCAPE")) {
            return "准备逃跑";
        }
        return "意图不太明确";
    }

    private static String powers(AbstractMonster m) {
        StringBuilder sb = new StringBuilder();
        append(sb, m, "Ritual", "每回合叠力量");
        append(sb, m, "Enrage", "你打技能它会加力量");
        append(sb, m, "Artifact", "有人工制品，会挡负面");
        append(sb, m, "Curl Up", "会蜷缩加格挡");
        append(sb, m, "Thorns", "有荆棘，打它会反伤");
        append(sb, m, "Metallicize", "每回合加格挡");
        append(sb, m, "Plated Armor", "有覆甲，每回合加格挡");
        append(sb, m, "Angry", "受伤会加力量");
        append(sb, m, "Split", "半血会分裂");
        append(sb, m, "Intangible", "有无实体，硬打很亏");
        append(sb, m, "Beat of Death", "你每打一张牌会掉血");
        append(sb, m, "Invincible", "单回合伤害有上限");
        return sb.toString();
    }

    private static void append(StringBuilder sb, AbstractMonster m, String power, String text) {
        if (m.hasPower(power)) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(text);
        }
    }

    private static int multi(AbstractMonster m) {
        try {
            if (multiField == null) {
                multiField = AbstractMonster.class.getDeclaredField("intentMultiAmt");
                multiField.setAccessible(true);
            }
            int n = multiField.getInt(m);
            return n > 0 ? n : 1;
        } catch (Throwable ignored) {
            return 1;
        }
    }

    private static String norm(String s) {
        return CardDatabase.norm(s == null ? "" : s.toLowerCase(Locale.ROOT));
    }
}
