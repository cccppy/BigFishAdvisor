package bigfish.advisor;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;

import java.io.IOException;

/** Small persisted settings file under the normal ModTheSpire config directory. */
public final class Config {
    public boolean enabled = true;
    public boolean combat = true;
    public boolean cardReward = true;
    public boolean map = true;
    public boolean events = true;
    public boolean campfire = true;
    public boolean shop = true;
    public boolean potions = true;
    public boolean cardSelect = true;
    public boolean relics = true;
    public boolean expanded = false;
    public float panelX = -1.0f;
    public float panelY = -1.0f;
    public float fishX = -1.0f;
    public float fishY = -1.0f;
    private transient SpireConfig config;

    public static Config load() {
        Config c = new Config();
        try {
            java.util.Properties defaults = new java.util.Properties();
            defaults.setProperty("enabled", "true");
            defaults.setProperty("combat", "true");
            defaults.setProperty("cardReward", "true");
            defaults.setProperty("map", "true");
            defaults.setProperty("events", "true");
            defaults.setProperty("campfire", "true");
            defaults.setProperty("shop", "true");
            defaults.setProperty("potions", "true");
            defaults.setProperty("cardSelect", "true");
            defaults.setProperty("relics", "true");
            defaults.setProperty("expanded", "false");
            defaults.setProperty("panelX", "-1");
            defaults.setProperty("panelY", "-1");
            defaults.setProperty("fishX", "-1");
            defaults.setProperty("fishY", "-1");
            c.config = new SpireConfig(BigFishAdvisor.MOD_ID, "config", defaults);
            c.enabled = c.config.getBool("enabled");
            c.combat = c.config.getBool("combat");
            c.cardReward = c.config.getBool("cardReward");
            c.map = c.config.getBool("map");
            c.events = c.config.getBool("events");
            c.campfire = c.config.getBool("campfire");
            c.shop = c.config.getBool("shop");
            c.potions = c.config.getBool("potions");
            c.cardSelect = c.config.getBool("cardSelect");
            c.relics = c.config.getBool("relics");
            c.expanded = c.config.getBool("expanded");
            c.panelX = c.config.getFloat("panelX");
            c.panelY = c.config.getFloat("panelY");
            c.fishX = c.config.getFloat("fishX");
            c.fishY = c.config.getFloat("fishY");
        } catch (Throwable t) {
            BigFishAdvisor.log("config load failed, using defaults: " + t);
        }
        return c;
    }

    public void save() {
        if (config == null) {
            return;
        }
        try {
            config.setBool("enabled", enabled);
            config.setBool("combat", combat);
            config.setBool("cardReward", cardReward);
            config.setBool("map", map);
            config.setBool("events", events);
            config.setBool("campfire", campfire);
            config.setBool("shop", shop);
            config.setBool("potions", potions);
            config.setBool("cardSelect", cardSelect);
            config.setBool("relics", relics);
            config.setBool("expanded", expanded);
            config.setFloat("panelX", panelX);
            config.setFloat("panelY", panelY);
            config.setFloat("fishX", fishX);
            config.setFloat("fishY", fishY);
            config.save();
        } catch (IOException e) {
            BigFishAdvisor.log("config save failed: " + e);
        }
    }
}
