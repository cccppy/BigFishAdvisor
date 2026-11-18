package bigfish.advisor;

import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

import java.util.Locale;

/**
 * Plain-language relic analysis: what it does, how to build around it and
 * which part of the run it changes.  RelicAdvisor keeps the scoring; this
 * class keeps the explanation so every pickup path (shop, boss, event) can
 * show the same advice instead of a bare tier letter.
 */
public final class RelicGuide {
    public static final class Profile {
        public String category;
        public String effect;
        public String plan;
        public String caution;

        public String text() {
            StringBuilder sb = new StringBuilder();
            sb.append("作用：").append(effect);
            if (plan.length() > 0) {
                sb.append("。拿法：").append(plan);
            }
            if (caution.length() > 0) {
                sb.append("。注意：").append(caution);
            }
            return sb.toString();
        }
    }

    private RelicGuide() {
    }

    public static Profile profile(AbstractPlayer p, AbstractRelic r) {
        Profile out = new Profile();
        out.effect = clean(r == null ? "" : r.description);
        String id = norm(r == null ? "" : r.relicId);
        out.category = category(id);
        out.plan = plan(p, id);
        out.caution = caution(id);
        if (out.effect.length() == 0 && r != null) {
            out.effect = r.name;
        }
        return out;
    }

    public static String note(AbstractPlayer p, AbstractRelic r) {
        return profile(p, r).text();
    }

    public static boolean whiteBeast(AbstractPlayer p) {
        return has(p, "White Beast Statue");
    }

    /** White Beast makes potion drops reliable unless Sozu blocks them. */
    public static boolean abundantPotions(AbstractPlayer p) {
        return whiteBeast(p) && !has(p, "Sozu");
    }

    public static int sustain(AbstractPlayer p) {
        int n = 0;
        n += has(p, "Burning Blood") ? 1 : 0;
        n += has(p, "Black Blood") ? 1 : 0;
        n += has(p, "Blood Vial") ? 1 : 0;
        n += has(p, "Meat on the Bone") ? 1 : 0;
        n += has(p, "Magic Flower") ? 1 : 0;
        n += has(p, "Toy Ornithopter") ? 1 : 0;
        n += has(p, "Bird-Faced Urn") ? 1 : 0;
        n += has(p, "Regal Pillow") ? 1 : 0;
        return n;
    }

    public static String potionLine(AbstractPlayer p) {
        if (abundantPotions(p)) {
            return "百兽雕像：战斗后药水掉落有保障，普通战斗可以更积极喝普通药水；"
                    + "但只有三格，Boss/精英前留复活、果汁和神圣树皮加成的高价值药水，别为了掉药水把好药喝光。";
        }
        if (has(p, "Sozu")) {
            return "添水：不能再拿药水，现有药水要留给精英/Boss；别买药水，也别选送药水的事件。";
        }
        if (has(p, "Potion Belt")) {
            return "药水腰带：多两个格子，可以多带一瓶针对 Boss 的药水；但仍然优先留复活/高价值药水。";
        }
        if (has(p, "Sacred Bark")) {
            return "神圣树皮：药水效果翻倍，普通药水也值得留到精英/Boss，不要随手喝掉。";
        }
        return "";
    }

    public static String healthLine(AbstractPlayer p) {
        int n = sustain(p);
        if (n <= 0) {
            return "";
        }
        return "续航遗物：你有 " + n + " 件回血/续航遗物，可以比平时更敢用血换资源；"
                + "但 Boss 前仍要留安全垫，别卖到一击线以下。";
    }

    // -------------------------------------------------------------- categories

    private static String category(String id) {
        if (id.contains("whitebeast") || id.contains("potionbelt") || id.contains("sacredbark")
                || id.contains("sozu") || id.contains("toyornithopter")) {
            return "药水";
        }
        if (id.contains("burningblood") || id.contains("blackblood") || id.contains("bloodvial")
                || id.contains("meatonthebone") || id.contains("magicflower")
                || id.contains("birdfacedurn") || id.contains("regalpillow")) {
            return "续航";
        }
        if (isEnergy(id)) {
            return "能量";
        }
        if (id.contains("pennib") || id.contains("nunchaku") || id.contains("kunai")
                || id.contains("shuriken") || id.contains("ornamentalfan")
                || id.contains("letteropener")) {
            return "攻击触发";
        }
        if (id.contains("anchor") || id.contains("horncleat") || id.contains("captainswheel")
                || id.contains("threadandneedle") || id.contains("oddlysmoothstone")
                || id.contains("orichalcum") || id.contains("calipers")) {
            return "防御";
        }
        if (id.contains("bagofpreparation") || id.contains("ringofthesnake")
                || id.contains("pocketwatch") || id.contains("sneckoeye")) {
            return "抽牌";
        }
        if (id.contains("mawbank") || id.contains("membershipcard") || id.contains("courier")
                || id.contains("smilingmask") || id.contains("ectoplasm")) {
            return "金币/商店";
        }
        if (id.contains("omamori") || id.contains("bluecandle") || id.contains("duvudoll")
                || id.contains("darkstoneperiapt") || id.contains("necronomicurse")) {
            return "诅咒/状态";
        }
        if (id.contains("nuclearbattery") || id.contains("datadisk") || id.contains("symbioticvirus")
                || id.contains("frozenco") || id.contains("crackedcore")) {
            return "充能球";
        }
        if (id.contains("violetlotus") || id.contains("damaru") || id.contains("teardroplocket")
                || id.contains("holywater") || id.contains("melange")) {
            return "姿态";
        }
        if (id.contains("wristblade")) {
            return "小刀";
        }
        if (id.contains("sneckoskull") || id.contains("thespecimen")) {
            return "毒";
        }
        if (id.contains("hoveringkite") || id.contains("toughbandages") || id.contains("tingsha")) {
            return "弃牌";
        }
        if (id.contains("deadbranch") || id.contains("charonsashes")) {
            return "消耗";
        }
        if (id.contains("runicpyramid") || id.contains("welllaidplans")) {
            return "保留";
        }
        if (id.contains("scry")) {
            return "占卜";
        }
        if (id.contains("map") || id.contains("compass") || id.contains("astrolabe")) {
            return "路线";
        }
        return "通用";
    }

    private static String plan(AbstractPlayer p, String id) {
        if (id.contains("whitebeast")) {
            return "药水掉落稳定，普通战斗该喝就喝，用血换输出/格挡；"
                    + "但只有三格，Boss/精英前留复活、果汁和树皮配合的高价值药水。";
        }
        if (id.contains("sozu")) {
            return "能量更足，但不能拿药水；现在这瓶高价值药水要留到精英/Boss，别在普通战斗空喝。";
        }
        if (id.contains("potionbelt")) {
            return "多两个药水格，可以专门留一瓶 Boss 药；普通战斗仍优先用低价值药水。";
        }
        if (id.contains("sacredbark")) {
            return "药水效果翻倍，普通药水也值得留到精英/Boss；别在满血小怪战浪费。";
        }
        if (isHeal(id)) {
            return "可以比平时更敢卖血换遗物/删牌/事件收益；但 Boss 前要留安全垫，营火仍按 Boss 伤害判断。";
        }
        if (id.contains("icecream")) {
            return "剩余能量会留到下回合，适合攒能量打 X 费/爆发；算费时别再额外加一次。";
        }
        if (id.contains("chemicalx")) {
            return "X 费牌效果 +2，0 能量也能吃到；选牌时优先 X 费核心，别只看当前能量。";
        }
        if (id.contains("mummifiedhand")) {
            return "开能力后随机一张牌变 0 费；能力牌越多越赚，战斗里先开能力再打被减费的牌。";
        }
        if (id.contains("deadbranch")) {
            return "消耗牌会补随机牌，消耗体系越厚越强；但随机牌可能卡手，别把关键牌乱烧。";
        }
        if (id.contains("necronomicon")) {
            return "当前费用 2 费以上的攻击会打两次；围绕高费攻击建牌，注意异蛇/腐化后的当前费用。";
        }
        if (id.contains("pennib")) {
            return "每 10 次攻击下一张攻击翻倍；把计数留给高伤攻击，别用小刀/低伤攻击浪费。";
        }
        if (id.contains("nunchaku") || id.contains("kunai") || id.contains("shuriken")) {
            return "每 3 次攻击给能量/敏捷/力量；多段和低费攻击优先，把计数打满。";
        }
        if (id.contains("ornamentalfan")) {
            return "每 3 次攻击给 4 格挡；攻击频率就是防御，适合多段/小刀/低费攻击。";
        }
        if (id.contains("letteropener")) {
            return "每 3 次技能造成 AOE；技能多的牌组更赚，但别为了触发塞废技能。";
        }
        if (id.contains("tungstenrod")) {
            return "自伤 -1，卖血/自伤牌更安全；配合 Rupture/Bloodletting 时更敢开。";
        }
        if (id.contains("omamori")) {
            return "挡两次诅咒；有它才考虑吃诅咒换资源，挡完就别再主动拿。";
        }
        if (id.contains("bluecandle")) {
            return "诅咒可以打出并消耗；有它才适合拿诅咒当资源，没配合别硬吃。";
        }
        if (id.contains("duvudoll")) {
            return "每张诅咒 +1 力量；诅咒多才强，但先确保有移除/消耗手段。";
        }
        if (id.contains("darkstoneperiapt")) {
            return "每张诅咒 +6 最大生命；和蓝蜡烛/杜沃娃娃一起才值得主动吃诅咒。";
        }
        if (id.contains("membershipcard") || id.contains("courier") || id.contains("mawbank")
                || id.contains("smilingmask")) {
            return "商店/金币更自由，可以多删牌/买关键遗物；但别为了清空金币买普通牌。";
        }
        if (id.contains("runicpyramid")) {
            return "手牌不再丢弃，关键牌能留到爆发回合；但状态/诅咒会卡手，需要消耗/弃牌出口。";
        }
        if (id.contains("violetlotus")) {
            return "退出平静额外 +1 能量；姿态循环越频繁越赚，优先多进平静再退出。";
        }
        if (id.contains("sneckoskull")) {
            return "每次上毒多 1 层；毒牌越多越强，先补上毒源再考虑倍率。";
        }
        if (id.contains("thespecimen")) {
            return "毒会传染给别的怪；多目标战强，但单体 Boss 不如直接倍率/尸爆。";
        }
        if (id.contains("wristblade")) {
            return "0 费攻击 +4 伤害；小刀/0 费攻击体系核心，优先多拿生成小刀的牌。";
        }
        if (id.contains("hoveringkite")) {
            return "每回合第一次弃牌 +1 能量；必须有稳定弃牌出口，否则接近白板。";
        }
        if (id.contains("toughbandages")) {
            return "每次弃牌 +3 格挡；弃牌越多越赚，配合计算下注/杂技。";
        }
        if (id.contains("tingsha")) {
            return "每次弃牌造成 3 点 AOE；弃牌体系补伤害，但别为了它硬塞弃牌。";
        }
        if (id.contains("calipers")) {
            return "格挡不再每回合全掉，只掉 15；高格挡/壁垒体系更稳。";
        }
        if (id.contains("threadandneedle")) {
            return "每回合 +4 格挡；低格挡牌组更需要，能显著减少小怪战损。";
        }
        if (id.contains("orichalcum")) {
            return "回合结束没格挡时给 6 格挡；适合不靠格挡牌的进攻牌组。";
        }
        if (isEnergy(id)) {
            return "高费核心更可用；算费时它已经算进每回合能量，别重复加。";
        }
        return "按它的被动调整选牌与战斗；不要只当白板，先看它改的是费用、防御、抽牌还是资源。";
    }

    private static String caution(String id) {
        if (id.contains("coffe")) {
            return "不能休息，只能靠战斗后回血/药水/治疗；低血量时很危险。";
        }
        if (id.contains("sozu")) {
            return "不能再拿药水；现有药水是打精英/Boss 的保险。";
        }
        if (id.contains("ectoplasm")) {
            return "不能再拿金币，商店/删牌要提前做。";
        }
        if (id.contains("bustedcrown")) {
            return "未来卡奖励少 2 张；牌组没成型时更难补核心。";
        }
        if (id.contains("velvetchoker")) {
            return "每回合最多出 6 张牌；低费/抽牌循环会被卡住。";
        }
        if (id.contains("philosophersstone")) {
            return "敌人全体 +1 力量，多段战越打越痛；格挡不足时危险。";
        }
        if (id.contains("runicdome")) {
            return "看不到敌人意图，新手很难防。";
        }
        if (id.contains("cursedkey")) {
            return "开箱会吃诅咒，别见箱就开。";
        }
        if (id.contains("fusionhammer")) {
            return "不能锻造，核心牌没升级时要谨慎。";
        }
        if (id.contains("markofpain")) {
            return "开局多 2 张伤口，没消耗/进化会卡手。";
        }
        if (id.contains("sneckoeye")) {
            return "费用随机，稳定 0 费连招会被打乱。";
        }
        if (id.contains("runicpyramid")) {
            return "手牌上限只有 10，状态/诅咒会卡住。";
        }
        if (id.contains("hoveringkite")) {
            return "需要每回合弃牌才有能量；没有稳定出口时接近白板。";
        }
        if (id.contains("whitebeast")) {
            return "药水只有三格，掉落稳定不等于可以无限喝；Boss 前仍要留高价值药水。";
        }
        return "";
    }

    private static boolean isHeal(String id) {
        return id.contains("burningblood") || id.contains("blackblood") || id.contains("bloodvial")
                || id.contains("meatonthebone") || id.contains("magicflower")
                || id.contains("birdfacedurn") || id.contains("regalpillow");
    }

    private static boolean isEnergy(String id) {
        return id.contains("coffe") || id.contains("cursedkey") || id.contains("fusionhammer")
                || id.contains("slaverscollar") || id.contains("sozu") || id.contains("ectoplasm")
                || id.contains("philosophersstone") || id.contains("runicdome")
                || id.contains("velvetchoker") || id.contains("bustedcrown")
                || id.contains("markofpain");
    }

    private static boolean has(AbstractPlayer p, String part) {
        if (p == null || p.relics == null) {
            return false;
        }
        for (AbstractRelic r : p.relics) {
            if (r != null && norm(r.relicId).contains(norm(part))) {
                return true;
            }
        }
        return false;
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        String out = s.replaceAll("\\[#[0-9a-fA-F]{6,8}\\]", "")
                .replaceAll("#(?:gold|[bgryp9])", "")
                .replaceAll("\\s+", " ")
                .trim();
        return out;
    }

    private static String norm(String s) {
        return CardDatabase.norm(s == null ? "" : s.toLowerCase(Locale.ROOT));
    }
}
