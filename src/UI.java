package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

final class UI {
    static int W = 1280, H = 720;
    static double S = 1280.0 / 1456.0;

    static void setSize(int w, int h) { W = w; H = h; S = w / 1456.0; }
    private static Font base;

    private UI() { }

    static int s(double v) { return (int) Math.round(v * S); }

    static final double FONT_SCALE = 1.0;

    static final double WIDE_FONT_FACTOR = 0.75;

    private static boolean custom;
    private static double norm = 1;

    private static void loadFont() {
        for (String name : new String[]{"font.ttf", "PressStart2P-Regular.ttf", "PressStart2P.ttf"}) {
            try (java.io.InputStream in = Assets.open(name)) {
                if (in == null) continue;
                useFont(Font.createFont(Font.TRUETYPE_FONT, in), true);
                System.out.println("[Word Survive] font loaded: " + name);
                return;
            } catch (Exception e) { System.err.println("[Word Survive] could not read font " + name + ": " + e.getMessage()); }
        }
        useFont(new Font(Font.MONOSPACED, Font.BOLD, 12), false);
        System.out.println("[Word Survive] no font file found in assets/, using the fallback font");
    }

    static void useFont(Font f, boolean isCustom) {
        base = f; custom = isCustom;
        double adv = f.deriveFont(100f).getStringBounds("MMMMMMMMMM", new java.awt.font.FontRenderContext(null, true, true)).getWidth() / 1000.0;
        norm = adv > 0.65 ? WIDE_FONT_FACTOR / adv : 1;
    }

    static Font font(double designSize) {
        if (base == null) loadFont();

        return base.deriveFont(custom ? Font.PLAIN : Font.BOLD, (float) (designSize * S * FONT_SCALE * norm));
    }

    static void text(Graphics2D g, String text, double x, double cy, int align, double maxW) {
        Font original = g.getFont();
        g.setFont(fit(g, text, original, s(maxW)));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        int px = align < 0 ? s(x) : align == 0 ? s(x) - w / 2 : s(x) - w;
        g.drawString(text, px, s(cy) + (fm.getAscent() - fm.getDescent()) / 2);
        g.setFont(original);
    }

    static void nearest(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

    static void draw(Graphics2D g, BufferedImage img, double x, double y, double w, double h) {
        g.drawImage(img, s(x), s(y), s(w), s(h), null);
    }

    static void drawTrimmed(Graphics2D g, String name, double cx, double y, double scale) {
        BufferedImage img = Assets.trimmed(name);
        double w = img.getWidth() * scale, h = img.getHeight() * scale;
        draw(g, img, cx - w / 2, y, w, h);
    }

    static void banner(Graphics2D g, double x, double y, double w, double h) {
        draw(g, Assets.trimmed("top_navigation.png"), x, y, w, h);
    }

    static Font fit(Graphics2D g, String text, Font start, int maxWidth) {
        Font f = start;
        while (f.getSize2D() > 9 && g.getFontMetrics(f).stringWidth(text) > maxWidth) f = f.deriveFont(f.getSize2D() - 1f);
        return f;
    }

    static Font fitFont(Graphics2D g, String text, double maxSize, double maxWidth) {
        return fit(g, text, font(maxSize), s(maxWidth));
    }

    static void centered(Graphics2D g, String text, Rectangle r, Color c) {
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent());
    }

    static <T extends Component> T place(Container parent, T c, double x, double y, double w, double h) {
        c.setBounds(s(x), s(y), s(w), s(h));
        parent.add(c);
        return c;
    }
}
