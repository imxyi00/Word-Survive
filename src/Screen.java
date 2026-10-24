package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

abstract class Screen extends JPanel {
    protected final Main app;
    protected boolean clouds = true, dim = false, header = false;
    static int tick = 0;
    private BufferedImage bgCache;
    private String bgKey = "";

    Screen(Main app) {
        this.app = app;
        setLayout(null);
        setPreferredSize(new Dimension(UI.W, UI.H));
    }

    void onShow() { }
    void onHide() { }
    protected void paintExtra(Graphics2D g) { }

    protected <T extends Component> T place(T c, double x, double y, double w, double h) { return UI.place(this, c, x, y, w, h); }

    protected PixelButton addBack(String target, double x, double y) {
        PixelButton b = new PixelButton(Assets.trimmed("BackBlue.png"));
        b.addActionListener(e -> app.show(target));
        return place(b, x, y, 129, 90);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        UI.nearest(g);
        String key = app.themeBg() + getWidth() + "x" + getHeight();
        if (!key.equals(bgKey)) {
            bgCache = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
            Graphics2D b = bgCache.createGraphics();
            UI.nearest(b);
            b.drawImage(Assets.get(app.themeBg()), 0, 0, bgCache.getWidth(), bgCache.getHeight(), null);
            b.dispose();
            bgKey = key;
        }
        g.drawImage(bgCache, 0, 0, null);
        if (clouds && app.theme == 0) UI.draw(g, Assets.get("cloud.png"), 200 + Math.sin(tick * 0.03) * 22, 64, 1105, 303);
        if (dim) { g.setColor(new Color(25, 25, 55, 150)); g.fillRect(0, 0, getWidth(), getHeight()); }
        if (header && app.user != null) {
            Avatars.drawCircle(g, app.user.avatar, 538, 15, 175);
            g.setFont(UI.font(38));
            g.setColor(Color.BLACK);
            UI.text(g, app.user.name, 737, 100, -1, 310);
        }
        paintExtra(g);
    }
}
