package com.megacrit.mtslauncher;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Replacement for the official Workshop-only mts-launcher.jar.
 *
 * Steam's "Play with Mods" option runs this jar. The official version refuses to
 * start unless ModTheSpire is installed from the Steam Workshop. This version
 * launches the local ModTheSpire.jar (and therefore the normal ModTheSpire mod
 * selector) instead.
 */
public final class Main {
    private static final String APP_ID = "646570";

    private Main() {
    }

    public static void main(String[] args) {
        boolean testOnly = args != null && args.length > 0 && "--test".equals(args[0]);
        try {
            File game = new File(".").getAbsoluteFile();
            if (!new File(game, "ModTheSpire.jar").exists()) {
                try {
                    File self = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    if (self.isFile()) {
                        game = self.getParentFile();
                    }
                } catch (Throwable ignored) {
                }
            }
            File mts = new File(game, "ModTheSpire.jar");
            File java = new File(game, "jre/bin/javaw.exe");
            if (!java.exists()) {
                java = new File(game, "jre/bin/java.exe");
            }
            if (!mts.exists()) {
                throw new IllegalStateException("找不到本地 ModTheSpire.jar：\n" + mts.getAbsolutePath()
                        + "\n\n请重新运行 BigFishAdvisor 的 install.ps1。");
            }
            if (!java.exists()) {
                throw new IllegalStateException("找不到 Java 8 运行时：\n" + java.getAbsolutePath());
            }
            writeSteamAppId(game);

            if (testOnly) {
                System.out.println("MTS_LOCAL_LAUNCHER_OK");
                System.out.println("game=" + game);
                System.out.println("java=" + java);
                System.out.println("mts=" + mts);
                return;
            }

            File logDir = new File(game, "sendToDevs/logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            File log = new File(logDir, "mts_process_launch.log");
            ProcessBuilder pb = new ProcessBuilder(java.getAbsolutePath(), "-Xmx1g", "-jar", mts.getAbsolutePath());
            pb.directory(game);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
            pb.start();
            System.out.println("[MtsLocalLauncher] Started local ModTheSpire.");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            if (!testOnly) {
                try {
                    JOptionPane.showMessageDialog(null,
                            "大肥鱼助手无法启动 ModTheSpire：\n" + t.getMessage()
                                    + "\n\n请改用游戏目录的 MTS-debug.cmd 查看错误。",
                            "Big Fish Advisor", JOptionPane.ERROR_MESSAGE);
                } catch (Throwable ignored) {
                }
            }
            System.exit(1);
        }
    }

    private static void writeSteamAppId(File game) {
        File f = new File(game, "steam_appid.txt");
        if (f.exists()) {
            return;
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(APP_ID);
        } catch (Exception ignored) {
        }
    }
}
