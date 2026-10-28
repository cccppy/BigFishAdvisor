package bigfish.advisor;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * Entry point for ModTheSpire. The mod is deliberately dependency-free apart
 * from ModTheSpire itself, so a fresh Slay the Spire install only needs
 * ModTheSpire + this jar.
 */
@SpireInitializer
public final class BigFishAdvisor {
    public static final String MOD_ID = "bigfishadvisor";
    public static final String VERSION = "1.1.0";
    public static Config config;
    public static Overlay overlay;

    private BigFishAdvisor() {
    }

    public static void initialize() {
        try {
            CardDatabase.load();
            config = Config.load();
            overlay = new Overlay();
            log("loaded v" + VERSION + " (F8 toggles the assistant)");
        } catch (Throwable t) {
            log("initialization failed: " + t);
            t.printStackTrace();
        }
    }

    public static void preUpdate() {
        if (overlay != null) {
            overlay.preUpdate();
        }
    }

    public static void update() {
        if (overlay != null) {
            overlay.update();
        }
    }

    public static void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
        if (overlay != null) {
            overlay.render(sb);
        }
    }

    public static void renderWelcome(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
        if (overlay != null) {
            overlay.renderWelcome(sb);
        }
    }

    public static void log(String message) {
        System.out.println("[BigFishAdvisor] " + message);
    }
}
