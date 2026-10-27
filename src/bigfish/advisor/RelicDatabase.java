package bigfish.advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Community relic quality from the SpireSpy relic tier list. */
public final class RelicDatabase {
    private static final Map<String, Entry> ENTRIES = new HashMap<String, Entry>();
    private static boolean loaded;

    public static final class Entry {
        public final double score;
        public final String tier;
        public final String name;

        Entry(double score, String tier, String name) {
            this.score = score;
            this.tier = tier;
            this.name = name;
        }
    }

    private RelicDatabase() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            JsonObject root = new JsonParser().parse(readResource("/bigfish/relic_tiers.json")).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                ENTRIES.put(e.getKey(), new Entry(o.get("score").getAsDouble(),
                        o.get("tier").getAsString(), o.get("name").getAsString()));
            }
        } catch (Throwable t) {
            BigFishAdvisor.log("relic_tiers.json load failed: " + t);
        }
    }

    public static Entry get(AbstractRelic relic) {
        if (relic == null) {
            return null;
        }
        load();
        Entry e = ENTRIES.get(norm(relic.relicId));
        if (e == null) {
            e = ENTRIES.get(norm(relic.name));
        }
        return e;
    }

    public static double score(AbstractRelic relic) {
        Entry e = get(relic);
        return e == null ? -1.0 : e.score;
    }

    public static String tier(AbstractRelic relic) {
        Entry e = get(relic);
        return e == null ? "" : e.tier;
    }

    private static String norm(String s) {
        return CardDatabase.norm(s);
    }

    private static String readResource(String path) throws Exception {
        InputStream in = RelicDatabase.class.getResourceAsStream(path);
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
}
