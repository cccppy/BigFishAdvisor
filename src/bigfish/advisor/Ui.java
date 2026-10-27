package bigfish.advisor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;

import java.util.ArrayList;
import java.util.List;

/** All drawing primitives for the fish bubble. Coordinates are game pixels. */
public final class Ui {
    public static final Color INK = new Color(0.12f, 0.17f, 0.27f, 1.0f);
    public static final Color CREAM = new Color(1.00f, 0.975f, 0.90f, 0.97f);
    public static final Color GOLD = new Color(0.98f, 0.77f, 0.30f, 1.0f);
    public static final Color ORANGE = new Color(0.80f, 0.43f, 0.14f, 1.0f);
    public static final Color GREEN = new Color(0.15f, 0.45f, 0.31f, 1.0f);
    public static final Color RED = new Color(0.68f, 0.22f, 0.18f, 1.0f);
    public static final Color BLUE = new Color(0.17f, 0.34f, 0.58f, 1.0f);
    public static final Color MUTED = new Color(0.36f, 0.41f, 0.48f, 1.0f);

    public final float s;
    public final BitmapFont titleFont;
    public final BitmapFont bodyFont;
    public final BitmapFont smallFont;
    private final Texture avatar;
    private final Texture avatarSmall;
    private final Texture panelTex;
    private final Texture borderTex;
    private final Texture badgeTex;
    private final Texture ringTex;
    private final Texture glowTex;
    private final Texture whiteTex;
    private final NinePatch panel;
    private final NinePatch borderPanel;
    private final NinePatch badgePanel;
    private final FreeTypeFontGenerator generator;

    public Ui() {
        s = Math.max(0.72f, Math.min(Settings.WIDTH / 1920.0f, Settings.HEIGHT / 1080.0f));
        avatar = texture("bigfish/avatar.png");
        avatarSmall = texture("bigfish/avatar_small.png");
        panelTex = texture("bigfish/panel.png");
        borderTex = texture("bigfish/border.png");
        badgeTex = texture("bigfish/badge.png");
        ringTex = texture("bigfish/ring.png");
        glowTex = texture("bigfish/glow.png");
        panel = new NinePatch(panelTex, 40, 40, 40, 40);
        borderPanel = new NinePatch(borderTex, 40, 40, 40, 40);
        badgePanel = new NinePatch(badgeTex, 14, 14, 14, 14);
        com.badlogic.gdx.graphics.Pixmap pix = new com.badlogic.gdx.graphics.Pixmap(1, 1,
                com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        whiteTex = new Texture(pix);
        pix.dispose();

        generator = new FreeTypeFontGenerator(Gdx.files.internal("bigfish/font.ttf"));
        titleFont = makeFont(Math.round(30 * s));
        bodyFont = makeFont(Math.round(21 * s));
        smallFont = makeFont(Math.round(16 * s));
        generator.dispose();
    }

    private Texture texture(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private BitmapFont makeFont(int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.color = Color.WHITE;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        String chars = CardDatabase.fontChars();
        p.characters = chars;
        BitmapFont f = generator.generateFont(p);
        f.setUseIntegerPositions(false);
        f.getData().markupEnabled = false;
        return f;
    }

    public void dispose() {
        if (avatar != null) {
            avatar.dispose();
        }
        if (avatarSmall != null) {
            avatarSmall.dispose();
        }
        if (panelTex != null) {
            panelTex.dispose();
        }
        if (borderTex != null) {
            borderTex.dispose();
        }
        if (badgeTex != null) {
            badgeTex.dispose();
        }
        if (ringTex != null) {
            ringTex.dispose();
        }
        if (glowTex != null) {
            glowTex.dispose();
        }
        if (whiteTex != null) {
            whiteTex.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (bodyFont != null) {
            bodyFont.dispose();
        }
        if (smallFont != null) {
            smallFont.dispose();
        }
    }

    public float pad() {
        return 16.0f * s;
    }

    public void drawPanel(SpriteBatch sb, float x, float y, float w, float h) {
        // soft drop shadow, no horizontal "sticker" line
        sb.setColor(0.04f, 0.06f, 0.10f, 0.20f);
        panel.draw(sb, x + 8 * s, y - 10 * s, w, h);
        sb.setColor(0.04f, 0.06f, 0.10f, 0.22f);
        panel.draw(sb, x + 4 * s, y - 6 * s, w, h);
        sb.setColor(INK);
        panel.draw(sb, x - 3 * s, y - 3 * s, w + 6 * s, h + 6 * s);
        sb.setColor(CREAM);
        panel.draw(sb, x, y, w, h);
        // No inner outline/accent: avoid the thin line at the top/left.
        drawDragGrip(sb, x + w - 62 * s, y + h - 17 * s);
    }

    public void drawDragGrip(SpriteBatch sb, float x, float y) {
        sb.setColor(INK.r, INK.g, INK.b, 0.30f);
        for (int i = 0; i < 3; i++) {
            sb.draw(whiteTex, x + i * 6 * s, y, 3 * s, 3 * s);
        }
    }

    public void drawAvatar(SpriteBatch sb, float x, float y, float size) {
        sb.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        sb.draw(avatar, x, y, size, size);
    }

    public void drawHiddenTab(SpriteBatch sb, float x, float y, float size, boolean hovered) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 400.0);
        sb.setColor(1.0f, 0.85f, 0.45f, 0.18f + 0.10f * pulse);
        sb.draw(glowTex, x - 24 * s, y - 24 * s, size + 48 * s, size + 48 * s);
        sb.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        sb.draw(avatarSmall, x, y, size, size);
        drawBadge(sb, x + size - 18 * s, y - 10 * s, 52 * s, 24 * s, INK, GOLD,
                "F8", smallFont, CREAM);
    }

    public void drawTitle(SpriteBatch sb, String kicker, String title, float x, float y, float maxWidth) {
        smallFont.setColor(GOLD);
        smallFont.draw(sb, kicker, x, y);
        float yy = y - 26 * s;
        titleFont.setColor(INK);
        List<String> lines = wrap(titleFont, title, maxWidth);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            titleFont.draw(sb, lines.get(i), x, yy);
            yy -= titleFont.getLineHeight();
        }
    }

    public float drawBody(SpriteBatch sb, List<String> lines, float x, float y, float maxWidth,
                          int maxLines, Color color) {
        return drawBody(sb, lines, x, y, maxWidth, maxLines, color, bodyFont);
    }

    public float drawBody(SpriteBatch sb, List<String> lines, float x, float y, float maxWidth,
                          int maxLines, Color color, BitmapFont font) {
        return drawBody(sb, lines, x, y, maxWidth, maxLines, color, font, 0);
    }

    /** Draw one page of body lines. startLine is zero-based; no ellipsis is added. */
    public float drawBody(SpriteBatch sb, List<String> lines, float x, float y, float maxWidth,
                          int maxLines, Color color, BitmapFont font, int startLine) {
        List<String> flat = new ArrayList<String>();
        List<Boolean> firstLine = new ArrayList<Boolean>();
        for (String raw : lines) {
            if (raw == null || raw.length() == 0) {
                continue;
            }
            List<String> wrapped = wrap(font, raw, maxWidth);
            for (int i = 0; i < wrapped.size(); i++) {
                flat.add(wrapped.get(i));
                firstLine.add(i == 0);
            }
        }
        int total = flat.size();
        int start = Math.max(0, Math.min(startLine, total));
        int end = Math.min(total, start + Math.max(0, maxLines));
        font.setColor(color);
        for (int i = start; i < end; i++) {
            if (firstLine.get(i)) {
                sb.setColor(GOLD);
                sb.draw(whiteTex, x - 8 * s, y - 10 * s, 3 * s, 13 * s);
                font.setColor(color);
            }
            font.draw(sb, flat.get(i), x, y);
            y -= font.getLineHeight();
        }
        return y;
    }

    public float bodyHeight(List<String> lines, float maxWidth, int maxLines) {
        int drawn = 0;
        float h = 0;
        for (String raw : lines) {
            if (raw == null || raw.length() == 0) {
                continue;
            }
            List<String> wrapped = wrap(bodyFont, raw, maxWidth);
            for (int i = 0; i < wrapped.size() && drawn < maxLines; i++) {
                h += bodyFont.getLineHeight();
                drawn++;
            }
        }
        return h;
    }

    public void drawFooter(SpriteBatch sb, String text, float x, float y, float maxWidth) {
        smallFont.setColor(MUTED);
        smallFont.draw(sb, text, x, y);
    }

    public void drawBadge(SpriteBatch sb, float x, float y, float w, float h, Color fill,
                          Color border, String text, BitmapFont font, Color textColor) {
        sb.setColor(border);
        badgePanel.draw(sb, x - 2 * s, y - 2 * s, w + 4 * s, h + 4 * s);
        sb.setColor(fill);
        badgePanel.draw(sb, x, y, w, h);
        GlyphLayout layout = new GlyphLayout(font, text);
        font.setColor(textColor);
        font.draw(sb, text, x + (w - layout.width) / 2.0f, y + (h + layout.height) / 2.0f);
    }

    public void drawPill(SpriteBatch sb, float x, float y, float w, float h, Color fill, String text,
                         BitmapFont font, Color textColor) {
        drawBadge(sb, x, y, w, h, fill, INK, text, font, textColor);
    }

    public void drawRing(SpriteBatch sb, float cx, float cy, float radius, Color color, float alpha) {
        sb.setColor(color.r, color.g, color.b, alpha);
        sb.draw(ringTex, cx - radius, cy - radius, radius * 2, radius * 2);
    }

    public void drawGlow(SpriteBatch sb, float cx, float cy, float radius, Color color, float alpha) {
        sb.setColor(color.r, color.g, color.b, alpha);
        sb.draw(glowTex, cx - radius, cy - radius, radius * 2, radius * 2);
    }

    public void drawLine(SpriteBatch sb, float x1, float y1, float x2, float y2, float width, Color color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) {
            return;
        }
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        sb.setColor(color);
        sb.draw(whiteTex, x1, y1 - width / 2, 0, width / 2, len, width, 1, 1, angle,
                0, 0, 1, 1, false, false);
    }

    public void drawOptionHighlight(SpriteBatch sb, com.megacrit.cardcrawl.helpers.Hitbox hb, Color color) {
        if (hb == null) {
            return;
        }
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 280.0);
        sb.setColor(color.r, color.g, color.b, 0.55f + 0.35f * pulse);
        float t = 4 * s;
        sb.draw(whiteTex, hb.x, hb.y, hb.width, t);
        sb.draw(whiteTex, hb.x, hb.y + hb.height - t, hb.width, t);
        sb.draw(whiteTex, hb.x, hb.y, t, hb.height);
        sb.draw(whiteTex, hb.x + hb.width - t, hb.y, t, hb.height);
    }

    public void drawCardCorners(SpriteBatch sb, AbstractCard c, Color color) {
        if (c == null) {
            return;
        }
        float w = AbstractCard.IMG_WIDTH * c.drawScale;
        float h = AbstractCard.IMG_HEIGHT * c.drawScale;
        float left = c.current_x - w / 2;
        float bottom = c.current_y - h / 2;
        float len = 22 * s;
        float thick = 3 * s;
        sb.setColor(color);
        sb.draw(whiteTex, left, bottom, len, thick);
        sb.draw(whiteTex, left, bottom, thick, len);
        sb.draw(whiteTex, left + w - len, bottom, len, thick);
        sb.draw(whiteTex, left + w - thick, bottom, thick, len);
        sb.draw(whiteTex, left, bottom + h - thick, len, thick);
        sb.draw(whiteTex, left, bottom + h - len, thick, len);
        sb.draw(whiteTex, left + w - len, bottom + h - thick, len, thick);
        sb.draw(whiteTex, left + w - thick, bottom + h - len, thick, len);
    }

    public void drawCardTag(SpriteBatch sb, AbstractCard c, String text, Color fill, Color border,
                            Color textColor) {
        if (c == null) {
            return;
        }
        float w = AbstractCard.IMG_WIDTH * c.drawScale;
        float h = AbstractCard.IMG_HEIGHT * c.drawScale;
        float top = c.current_y + h / 2 + 8 * s;
        GlyphLayout layout = new GlyphLayout(smallFont, text);
        float bw = Math.max(74 * s, layout.width + 26 * s);
        drawBadge(sb, c.current_x - bw / 2, top, bw, 27 * s, fill, border, text, smallFont, textColor);
    }

    public void drawTarget(SpriteBatch sb, com.megacrit.cardcrawl.monsters.AbstractMonster m) {
        if (m == null || m.hb == null) {
            return;
        }
        float r = Math.max(m.hb.width, m.hb.height) * 0.63f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);
        drawGlow(sb, m.hb.cX, m.hb.cY, r * 1.45f, GOLD, 0.13f + 0.08f * pulse);
        drawRing(sb, m.hb.cX, m.hb.cY, r, GOLD, 0.92f);
        drawBadge(sb, m.hb.cX - 42 * s, m.hb.cY - r - 34 * s, 84 * s, 25 * s, INK, GOLD,
                "推荐目标", smallFont, GOLD);
    }

    public String ellipsize(BitmapFont font, String text, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (measure(font, text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (measure(font, sb.toString() + suffix) > maxWidth) {
                sb.setLength(Math.max(0, sb.length() - 1));
                break;
            }
        }
        return sb.toString() + suffix;
    }

    public List<String> wrap(BitmapFont font, String text, float maxWidth) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        String[] paragraphs = text.split("\\n", -1);
        for (String p : paragraphs) {
            if (p.length() == 0) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < p.length(); i++) {
                char ch = p.charAt(i);
                line.append(ch);
                if (measure(font, line.toString()) > maxWidth && line.length() > 1) {
                    line.setLength(line.length() - 1);
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(ch);
                }
            }
            if (line.length() > 0) {
                out.add(line.toString());
            }
        }
        return out;
    }

    public float measure(BitmapFont font, String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        GlyphLayout layout = new GlyphLayout(font, text);
        return layout.width;
    }

    public float scale() {
        return s;
    }
}

