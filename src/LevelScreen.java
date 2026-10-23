package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

final class LevelScreen extends Screen {
    private static final double[] Y = {620, 527, 405, 530};
    private static final double STEP = 245, X0 = 560, R = 75;
    private double scroll, target;
    private int pressX = -1, dragFrom;
    private boolean dragged;
    private final javax.swing.Timer anim;

    LevelScreen(Main app) {
        super(app);
        PixelButton back = new PixelButton(Assets.trimmed("BackBlue.png"));
        back.addActionListener(e -> app.show("mode"));
        place(back, 40, 22, 129, 90);
        PixelButton settings = new PixelButton(Assets.get("settings_icon.png"));
        settings.addActionListener(e -> app.openSettings(null));
        place(settings, 1335, 10, 100, 100);
        PixelButton left = new PixelButton("<"), right = new PixelButton(">");
        left.addActionListener(e -> target = clamp(target - 600));
        right.addActionListener(e -> target = clamp(target + 600));
        place(left, 20, 330, 90, 85);
        place(right, 1346, 330, 90, 85);

        anim = new javax.swing.Timer(16, e -> {
            if (Math.abs(target - scroll) > 0.5) { scroll += (target - scroll) * 0.18; repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { pressX = e.getX(); dragFrom = e.getX(); dragged = false; }
            @Override public void mouseReleased(MouseEvent e) {
                if (!dragged) {
                    int hit = hitLevel(e.getPoint());
                    if (hit > 0 && hit <= app.user.cleared() + 1) { Sound.sfx("click"); app.startStage(hit); }
                    else if (hit > 0) Sound.sfx("wrong");
                }
                pressX = -1;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (pressX < 0) return;
                if (Math.abs(e.getX() - pressX) > 6) dragged = true;
                if (dragged) { scroll = target = clamp(scroll - (e.getX() - dragFrom) / UI.S); dragFrom = e.getX(); repaint(); }
            }
        });
        addMouseWheelListener(e -> target = clamp(target + e.getWheelRotation() * 250));
    }

    private static double clamp(double v) { return Math.max(0, Math.min(maxScroll(), v)); }
    private static double maxScroll() { return X0 + (SentenceBank.LEVELS - 1) * STEP + 300 - 1456; }
    private double nx(int level) { return X0 + (level - 1) * STEP - scroll; }
    private static double ny(int level) { return Y[(level - 1) % 4]; }

    private int hitLevel(Point p) {
        for (int lv = 1; lv <= SentenceBank.LEVELS; lv++) {
            double dx = p.x - UI.s(nx(lv)), dy = p.y - UI.s(ny(lv));
            if (Math.hypot(dx, dy) <= UI.s(R)) return lv;
        }
        return 0;
    }

    @Override void onShow() {
        header = false;
        int cur = Math.min(SentenceBank.LEVELS, app.user.cleared() + 1);
        scroll = target = clamp(X0 + (cur - 1) * STEP - 728);
        anim.start();
    }
    @Override void onHide() { anim.stop(); }

    @Override protected void paintExtra(Graphics2D g) {
        int cleared = app.user.cleared();
        g.setColor(new Color(255, 255, 255, 170));
        for (int lv = 1; lv < SentenceBank.LEVELS; lv++)
            for (int k = 1; k <= 6; k++) {
                double t = k / 7.0;
                int x = UI.s(nx(lv) + (nx(lv + 1) - nx(lv)) * t), y = UI.s(ny(lv) + (ny(lv + 1) - ny(lv)) * t);
                g.fillRect(x - 4, y - 4, 8, 8);
            }
        for (int lv = 1; lv <= SentenceBank.LEVELS; lv++) {
            double x = nx(lv), y = ny(lv);
            if (x < -150 || x > 1600) continue;
            boolean open = lv <= cleared + 1;
            if (!open) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            if (lv == cleared + 1) {
                int pulse = (int) (Math.sin(tick * 0.2) * 5 + 8);
                g.setColor(new Color(255, 240, 120, 160));
                g.fillOval(UI.s(x - R) - pulse, UI.s(y - R) - pulse, UI.s(2 * R) + 2 * pulse, UI.s(2 * R) + 2 * pulse);
            }
            UI.draw(g, Assets.trimmed("Level_Selection_Button.png"), x - R, y - R, 2 * R, 2 * R);
            g.setFont(UI.font(lv < 10 ? 78 : 62));
            UI.centered(g, String.valueOf(lv), new Rectangle(UI.s(x - R), UI.s(y - R), UI.s(2 * R), UI.s(2 * R)), Color.BLACK);
            g.setComposite(AlphaComposite.SrcOver);
            if (lv <= cleared) {
                g.setColor(new Color(255, 215, 0));
                int c = UI.s(11), sx = UI.s(x + R - 30), sy = UI.s(y - R);
                g.fillRect(sx + c, sy, c, c * 3); g.fillRect(sx, sy + c, c * 3, c);
            }
        }
        UI.banner(g, 235, 0, 1020, 115);
        g.setFont(UI.font(60));
        UI.centered(g, "Stage", new Rectangle(UI.s(300), 0, UI.s(330), UI.s(105)), Color.BLACK);
        UI.centered(g, "Mode", new Rectangle(UI.s(860), 0, UI.s(300), UI.s(105)), Color.BLACK);
        Avatars.drawCircle(g, app.user.avatar, 660, 10, 160);
    }
}
