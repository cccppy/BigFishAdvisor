package bigfish.launcher;

import com.evacipated.cardcrawl.modthespire.Loader;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches ModTheSpire directly from the normal SlayTheSpire.exe config.
 * It intentionally does not show the ModTheSpire mod selector; it loads every
 * jar in mods/ and then starts the game.
 */
public final class BigFishLauncher {
    private static final String MTS_VERSION = "3.6.3";
    private static final String APP_ID = "646570";

    private BigFishLauncher() {
    }

    public static void main(String[] args) {
        try {
            writeSteamAppId();
            setMtsVersion();
            File modDir = new File("mods");
            File[] jars = modDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jars == null || jars.length == 0) {
                throw new IllegalStateException("No mods found in " + modDir.getAbsolutePath());
            }
            List<File> list = new ArrayList<File>(Arrays.asList(jars));
            list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            System.out.println("[BigFishLauncher] Loading " + list.size() + " mod(s) without the selector.");
            Loader.runMods(list.toArray(new File[0]));
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                JOptionPane.showMessageDialog(null,
                        "大肥鱼助手启动失败：\n" + t + "\n\n请改用游戏目录 MTS-debug.cmd 查看完整错误。",
                        "Big Fish Advisor", JOptionPane.ERROR_MESSAGE);
            } catch (Throwable ignored) {
            }
            System.exit(1);
        }
    }

    private static void setMtsVersion() throws Exception {
        Class<?> semver = Class.forName("com.vdurmont.semver4j.Semver");
        Object version = semver.getConstructor(String.class).newInstance(MTS_VERSION);
        Field field = Loader.class.getDeclaredField("MTS_VERSION");
        field.setAccessible(true);
        field.set(null, version);
    }

    private static void writeSteamAppId() {
        File f = new File("steam_appid.txt");
        if (f.exists()) {
            return;
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(APP_ID);
        } catch (Exception ignored) {
        }
    }
}
