package wordsurvive;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

final class Assets {
    private static final Map<String, BufferedImage> cache = new HashMap<>();
    private static Map<String, BufferedImage> keys;

    private Assets() { }

    private static File jarAssets() {
        try {
            File loc = new File(Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = loc.isFile() ? loc.getParentFile() : loc;
            return new File(dir, "assets");
        } catch (Exception e) { return new File("assets"); }
    }

    static InputStream open(String name) {
        for (File base : new File[]{new File("assets"), jarAssets()}) {
            File f = new File(base, name);
            if (f.isFile()) {
                try { return new FileInputStream(f); } catch (IOException ignored) { }
            }
        }
        return Assets.class.getResourceAsStream("/assets/" + name);
    }

    static BufferedImage get(String name) {
        BufferedImage img = cache.get(name);
        if (img == null) {
            img = load(name);
            cache.put(name, img);
        }
        return img;
    }

    private static BufferedImage load(String name) {
        try (InputStream in = open(name)) {
            if (in != null) {
                BufferedImage raw = ImageIO.read(in);
                BufferedImage argb = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = argb.createGraphics(); g.drawImage(raw, 0, 0, null); g.dispose();
                return argb;
            }
        } catch (Exception ignored) { }
        BufferedImage ph = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ph.createGraphics(); g.setColor(Color.MAGENTA); g.fillRect(0, 0, 64, 64); g.dispose();
        return ph;
    }

    static BufferedImage gray(String name) {
        String key = "gray:" + name;
        BufferedImage img = cache.get(key);
        if (img != null) return img;
        BufferedImage src = get(name);
        img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y), a = p >>> 24;
                if (a == 0) continue;
                double lum = 0.3 * ((p >> 16) & 255) + 0.59 * ((p >> 8) & 255) + 0.11 * (p & 255);
                int v = lum < 30 ? 0 : (int) Math.min(255, 130 + lum * 0.45);
                img.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
            }
        cache.put(key, img);
        return img;
    }

    static BufferedImage trimmed(String name) {
        String key = "trim:" + name;
        BufferedImage img = cache.get(key);
        if (img != null) return img;
        BufferedImage src = get(name);
        int x0 = src.getWidth(), y0 = src.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++)
                if ((src.getRGB(x, y) >>> 24) > 10) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
        img = x1 < 0 ? src : src.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
        cache.put(key, img);
        return img;
    }

    static BufferedImage blankButton() {
        BufferedImage img = cache.get("blank");
        if (img != null) return img;
        BufferedImage src = get("Play.png");
        BufferedImage full = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = full.createGraphics(); g.drawImage(src, 0, 0, null); g.dispose();
        for (int y = 45; y < Math.min(126, full.getHeight()); y++) {
            int body = full.getRGB(70, y);
            for (int x = 80; x < Math.min(440, full.getWidth()); x++) full.setRGB(x, y, body);
        }
        img = full.getWidth() > 24 ? full.getSubimage(12, 0, full.getWidth() - 24, full.getHeight() - 1) : full;
        cache.put("blank", img);
        return img;
    }

    static BufferedImage tinted(String which) {
        BufferedImage img = cache.get("tint:" + which);
        if (img != null) return img;
        BufferedImage src = blankButton();
        img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        boolean green = which.equals("green");
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24, r = (p >> 16) & 255, gg = (p >> 8) & 255, b = p & 255;
                if (a == 0 || Math.max(r, Math.max(gg, b)) < 40) { img.setRGB(x, y, p); continue; }
                float[] hsb = Color.RGBtoHSB(r, gg, b, null);
                int rgb = green ? Color.HSBtoRGB(0.36f, hsb[1], hsb[2] * 0.85f) : Color.HSBtoRGB(0.0f, hsb[1] * 0.75f, hsb[2] * 0.95f);
                img.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
            }
        cache.put("tint:" + which, img);
        return img;
    }

    static BufferedImage key(String token) {
        if (keys == null) {
            keys = new HashMap<>();
            BufferedImage kb = get("FullKeyBoard.png");
            String[][] rows = {{"Q","W","E","R","T","Y","U","I","O","P"}, {"A","S","D","F","G","H","J","K","L"}, {"DEL","Z","X","C","V","B","N","M","GO"}};
            int[] x0 = {19, 53, 54}, y0 = {6, 110, 216};
            double[] pitch = {74.6, 74.9, 74.9};
            for (int r = 0; r < rows.length; r++)
                for (int i = 0; i < rows[r].length; i++) {
                    int x = (int) Math.round(x0[r] + i * pitch[r]);
                    if (x + 66 <= kb.getWidth() && y0[r] + 86 <= kb.getHeight()) keys.put(rows[r][i], kb.getSubimage(x, y0[r], 66, 86));
                }
        }
        return keys.get(token);
    }
}
