package wordsurvive;

import java.awt.*;
import java.awt.image.BufferedImage;

final class Avatars {
    private static final String[] FILES = {"Mikyu.png", "avatar2.png", "Snor.png", "avatar4.png", "avatar5.png"};
    static final int COUNT = FILES.length;

    private Avatars() { }

    static BufferedImage get(int i) { return Assets.trimmed(FILES[Math.floorMod(i, COUNT)]); }

    static void drawCircle(Graphics2D g, int avatar, double x, double y, double size) {
        UI.draw(g, Assets.trimmed("avatar_frame.png"), x, y, size, size);
        double inner = size * 0.58;
        drawIn(g, avatar, x + (size - inner) / 2, y + (size - inner) / 2, inner, inner);
    }

    static void drawIn(Graphics2D g, int avatar, double x, double y, double w, double h) {
        BufferedImage img = get(avatar);
        double k = Math.min(w / img.getWidth(), h / img.getHeight());
        double dw = img.getWidth() * k, dh = img.getHeight() * k;
        UI.draw(g, img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh);
    }
}
