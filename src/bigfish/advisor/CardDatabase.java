package bigfish.advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.cards.AbstractCard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Community-inspired card notes and a small amount of per-card metadata.
 * Scores are not a tier list carved in stone: they are the starting point which
 * CardRewardAdvisor adjusts for the current deck, act, and duplicates.
 */
public final class CardDatabase {
    public static final class Entry {
        public final double score;
        public final String note;
        public final Set<String> tags;
        public final String tier;

        Entry(double score, String note, Set<String> tags, String tier) {
            this.score = score;
            this.note = note;
            this.tags = tags;
            this.tier = tier;
        }
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<String, Entry>();
    private static final Map<String, String> ZH_NAMES = new HashMap<String, String>();
    private static String fontChars = "";
    private static boolean loaded = false;

    private CardDatabase() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            JsonObject root = new JsonParser().parse(readResource("/bigfish/cards.json")).getAsJsonObject();
            JsonObject cards = root.getAsJsonObject("cards");
            for (Map.Entry<String, JsonElement> e : cards.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                Set<String> tags = new HashSet<String>();
                if (o.has("t")) {
                    for (JsonElement tag : o.getAsJsonArray("t")) {
                        tags.add(tag.getAsString());
                    }
                }
                String tier = o.has("tier") ? o.get("tier").getAsString() : "";
                Entry entry = new Entry(o.get("s").getAsDouble(), o.get("n").getAsString(), tags, tier);
                putEntry(e.getKey(), entry);
            }
        } catch (Throwable t) {
            BigFishAdvisor.log("cards.json load failed: " + t);
        }
        ZH_NAMES.put("strike", "打击");
        ZH_NAMES.put("defend", "防御");
        ZH_NAMES.put("rushdown", "猛虎下山");
        try {
            JsonObject names = new JsonParser().parse(readResource("/bigfish/names_zh.json")).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : names.entrySet()) {
                String n = norm(e.getKey());
                ZH_NAMES.put(n, e.getValue().getAsString());
                if (n.endsWith("v2")) {
                    ZH_NAMES.put(n.substring(0, n.length() - 2), e.getValue().getAsString());
                }
            }
        } catch (Throwable t) {
            BigFishAdvisor.log("names_zh.json load failed: " + t);
        }
        try {
            fontChars = readResource("/bigfish/charset.txt");
        } catch (Throwable t) {
            BigFishAdvisor.log("charset.txt load failed: " + t);
        }
    }

    private static void putEntry(String key, Entry entry) {
        String n = norm(key);
        ENTRIES.put(n, entry);
        if (n.endsWith("v2")) {
            ENTRIES.put(n.substring(0, n.length() - 2), entry);
        }
    }

    private static String readResource(String path) throws Exception {
        InputStream in = CardDatabase.class.getResourceAsStream(path);
        if (in == null) {
            throw new java.io.FileNotFoundException(path);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }

    public static Entry get(AbstractCard card) {
        if (card == null) {
            return null;
        }
        Entry e = ENTRIES.get(norm(card.cardID));
        if (e == null) {
            e = ENTRIES.get(norm(card.name));
        }
        return e;
    }

    public static String displayName(AbstractCard card) {
        if (card == null) {
            return "?";
        }
        String name = card.name == null ? String.valueOf(card.cardID) : card.name;
        if (containsCjk(name)) {
            return name;
        }
        String zh = ZH_NAMES.get(norm(name));
        if (zh == null) {
            zh = ZH_NAMES.get(norm(card.cardID));
        }
        if (zh == null || zh.equals(name)) {
            return name;
        }
        return zh + " " + name;
    }

    public static String shortName(AbstractCard card) {
        if (card == null) {
            return "?";
        }
        String name = card.name == null ? String.valueOf(card.cardID) : card.name;
        if (containsCjk(name)) {
            return name;
        }
        String zh = ZH_NAMES.get(norm(name));
        if (zh == null) {
            zh = ZH_NAMES.get(norm(card.cardID));
        }
        return zh == null ? name : zh;
    }

    public static String note(AbstractCard card) {
        Entry e = get(card);
        if (e != null && e.note != null && e.note.length() > 0) {
            return upgradedNote(card, e.note);
        }
        if (card == null) {
            return "";
        }
        if (card.type == AbstractCard.CardType.ATTACK) {
            return "一张普通攻击牌；补伤害/斩杀，前期可以拿，但别让牌组全是基础攻击。";
        }
        if (card.type == AbstractCard.CardType.SKILL) {
            return "一张技能牌；提供格挡或过牌，优先选不亏牌、费用低的。";
        }
        if (card.type == AbstractCard.CardType.POWER) {
            return "一张能力牌；长战成长高，但会占用当回合能量，前期别拿太多。";
        }
        return "基础牌/状态牌，通常不主动拿。";
    }

    private static String upgradedNote(AbstractCard card, String note) {
        if (card == null || (!card.upgraded && card.timesUpgraded <= 0)) {
            return note;
        }
        String n = note.replace("优先升级。", "已经升级过了。")
                .replace("升级后才", "已经升级，")
                .replace("升级后", "已经升级，");
        if (!n.contains("已升级")) {
            n += "（已升级）";
        }
        return n;
    }

    public static String tier(AbstractCard card) {
        Entry e = get(card);
        return e == null || e.tier == null ? "" : e.tier;
    }

    public static Set<String> tags(AbstractCard card) {
        Entry e = get(card);
        if (e == null) {
            return new HashSet<String>();
        }
        return e.tags;
    }

    public static boolean hasTag(AbstractCard card, String tag) {
        return tags(card).contains(tag);
    }

    public static String fontChars() {
        load();
        StringBuilder sb = new StringBuilder(fontChars);
        for (int i = 32; i < 127; i++) {
            if (sb.indexOf(String.valueOf((char) i)) < 0) {
                sb.append((char) i);
            }
        }
        if (sb.length() == 0) {
            sb.append("大肥鱼助手推荐路线选牌出牌最优解斩杀格挡易伤虚弱力量敏捷能力牌");
        }
        return sb.toString();
    }

    public static String norm(String s) {
        if (s == null) {
            return "";
        }
        String lower = s.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean containsCjk(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 0x4E00 && s.charAt(i) <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }
}
