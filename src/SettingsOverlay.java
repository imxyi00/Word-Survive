package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

final class SettingsOverlay extends JPanel {
    private static final double PX = 452, PY = 38, PW = 535, PH = 748;
    private static final double VIEW_TOP = 205, VIEW_BOTTOM = 677, MAX_SCROLL = 84, WHEEL_STEP = 30;
    private static final double MUSIC_Y = 395, RES_Y = 511, FIELD_H = 68;

    private static final double TITLE_SIZE = 52;
    private static final double HEADING_SIZE = 30;
    private static final double LABEL_SIZE = 30;
    private static final double TRACK_SIZE = 22.5;
    private static final double BUTTON_SIZE = 28;
    private static final double TAB_SIZE = 30;
    private static final double X_SIZE = 40;
    private static final double CONFIRM_SIZE = 60;
    private final Main app;
    private boolean themeMode;
    private int bg, sfx, music, theme, res;
    private double scroll;
    private Runnable onClose;

    private final JPanel view = new JPanel(null), body = new Body(), confirm = new Confirm();
    private final Slider bgSlider = new Slider(), sfxSlider = new Slider();
    private final Hot prev = new Hot(""), next = new Hot(""), rprev = new Hot(""), rnext = new Hot(""), themes, logout, reset = new Hot("Reset"), save = new Hot("Save");
    private final Hot th0 = new Hot(""), th1 = new Hot(""), close = new Hot("");

    SettingsOverlay(Main app) {
        this.app = app;
        setLayout(null); setOpaque(false); setVisible(false);
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseAdapter() { });
        addMouseWheelListener(e -> { if (!themeMode && !confirm.isVisible()) scrollTo(scroll + e.getWheelRotation() * WHEEL_STEP); });

        confirm.setBounds(0, 0, UI.W, UI.H); add(confirm);

        bodyPlace(bgSlider, 755, 255, 195, 34); bodyPlace(sfxSlider, 755, 308, 195, 34);
        bgSlider.onChange = v -> { bg = v; Sound.setBg(v); };
        sfxSlider.onChange = v -> { sfx = v; Sound.sfxVol = v; };
        sfxSlider.onRelease = () -> Sound.sfx("correct");

        bodyPlace(prev, 485, MUSIC_Y, 45, FIELD_H); bodyPlace(next, 905, MUSIC_Y, 45, FIELD_H);
        prev.onClick = () -> changeTrack(-1); next.onClick = () -> changeTrack(1);
        bodyPlace(rprev, 485, RES_Y, 45, FIELD_H); bodyPlace(rnext, 905, RES_Y, 45, FIELD_H);
        rprev.onClick = () -> changeRes(-1); rnext.onClick = () -> changeRes(1);

        themes = new Hot("Themes"); logout = new Hot("Log Out");
        themes.image = Assets.get("green_button.png"); logout.image = Assets.get("red_button.png");
        themes.fontSize = logout.fontSize = BUTTON_SIZE;
        bodyPlace(themes, 618, 597, 199, 69); bodyPlace(logout, 617, 682, 202, 70);
        themes.onClick = () -> { themeMode = true; layoutMode(); };
        logout.onClick = () -> confirm.setVisible(true);

        place(save, 508, 684, 195, 100); place(reset, 742, 684, 188, 100);
        reset.fontSize = save.fontSize = TAB_SIZE; reset.fit = save.fit = 0.55;
        reset.onClick = this::resetOrBack;
        save.onClick = this::saveOrOk;
        place(close, 900, 110, 105, 105);
        close.onClick = () -> { if (themeMode) { themeMode = false; layoutMode(); } else finish(false); };
        place(th0, 585, 250, 270, 152); place(th1, 585, 450, 270, 152);
        th0.onClick = () -> { theme = 0; app.theme = 0; app.root.repaint(); };
        th1.onClick = () -> { theme = 1; app.theme = 1; app.root.repaint(); };

        view.setOpaque(false);
        view.setBounds(0, UI.s(VIEW_TOP), UI.W, UI.s(VIEW_BOTTOM - VIEW_TOP));
        view.add(body);
        body.setOpaque(false);
        body.setSize(UI.W, UI.H);
        add(view);
    }

    private void place(Component c, double x, double y, double w, double h) { UI.place(this, c, x, y, w, h); }
    private void bodyPlace(Component c, double x, double y, double w, double h) { UI.place(body, c, x, y, w, h); }

    private void scrollTo(double v) {
        scroll = Math.max(0, Math.min(MAX_SCROLL, v));
        body.setLocation(0, -UI.s(VIEW_TOP + scroll));
        repaint();
    }

    void open(Runnable onClose) {
        this.onClose = onClose;
        User u = app.user;
        bg = u == null ? Sound.bgVol : u.bgVol; sfx = u == null ? Sound.sfxVol : u.sfxVol;
        music = u == null ? Sound.track : u.music; theme = app.theme; res = app.resolution;
        bgSlider.value = bg; sfxSlider.value = sfx;
        themeMode = false;
        confirm.setVisible(false);
        scrollTo(0);
        layoutMode();
        setVisible(true);
    }

    private void layoutMode() {
        boolean s = !themeMode;
        view.setVisible(s);
        th0.setVisible(themeMode); th1.setVisible(themeMode);
        reset.label = s ? "Reset" : "Back"; save.label = s ? "Save" : "Ok";
        repaint();
    }

    private void changeTrack(int d) {
        music = Math.floorMod(music + d, Sound.TRACKS.length);
        Sound.track = music; Sound.startMusic(); repaint();
    }

    private void changeRes(int d) { res = Math.floorMod(res + d, Main.RES_LABELS.length); repaint(); }

    private void resetOrBack() {
        if (themeMode) { themeMode = false; layoutMode(); return; }
        bg = 70; sfx = 80; music = 0; theme = 0; app.theme = 0;
        bgSlider.value = bg; sfxSlider.value = sfx;
        Sound.sfxVol = sfx; Sound.setBg(bg); Sound.track = 0; Sound.startMusic();
        app.root.repaint(); repaint();
    }

    private void saveOrOk() {
        if (themeMode) { themeMode = false; layoutMode(); return; }
        if (app.user != null) {
            app.user.bgVol = bg; app.user.sfxVol = sfx; app.user.music = music; app.user.theme = theme;
            app.users.save();
        }
        finish(true);
        if (res != app.resolution) app.changeResolution(res);
    }

    private void finish(boolean saved) {
        if (!saved && app.user != null) {
            Sound.sfxVol = app.user.sfxVol; Sound.setBg(app.user.bgVol);
            if (Sound.track != app.user.music) { Sound.track = app.user.music; Sound.startMusic(); }
            app.theme = app.user.theme;
        }
        setVisible(false);
        app.root.repaint();
        if (onClose != null) onClose.run();
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        UI.nearest(g);
        g.setColor(new Color(0, 0, 0, 190)); g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(196, 206, 86));
        g.fillRect(UI.s(PX + 22), UI.s(PY + 70), UI.s(PW - 44), UI.s(PH - 150));
        UI.draw(g, Assets.trimmed("Settings.png"), PX, PY, PW, PH);
        String head = themeMode ? "Theme" : "Settings";
        g.setFont(UI.fitFont(g, head, TITLE_SIZE, 290));
        UI.centered(g, head, new Rectangle(UI.s(PX + 60), UI.s(PY + 40), UI.s(PW - 120), UI.s(70)), Color.BLACK);
        UI.draw(g, Assets.trimmed("Level_Selection_Button.png"), 905, 112, 100, 100);
        g.setFont(UI.font(X_SIZE)); UI.centered(g, "X", new Rectangle(UI.s(905), UI.s(112), UI.s(100), UI.s(100)), Color.BLACK);
        if (themeMode) {
            for (int i = 0; i < 2; i++) {
                double y = i == 0 ? 250 : 450;
                g.drawImage(Assets.get(i == 0 ? "background.png" : "Another_Background.png"), UI.s(585), UI.s(y), UI.s(270), UI.s(152), null);
                g.setColor(Color.BLACK); g.setStroke(new BasicStroke(UI.s(3))); g.drawRect(UI.s(585), UI.s(y), UI.s(270), UI.s(152));
                if (theme == i) sparkles(g, 585, y, 270, 152);
            }
        }
    }

    private final class Body extends JPanel {
        Body() { super(null); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            heading(g, "Sound", 219);
            label(g, "Background:", 603, 271); label(g, "SFX:", 577, 324);
            heading(g, "BG Music", 374);
            field(g, MUSIC_Y, Sound.TRACKS[music]);
            heading(g, "Resolution", 490);
            field(g, RES_Y, Main.RES_LABELS[res]);
        }
    }

    private final class Confirm extends JPanel {
        Confirm() {
            super(null); setOpaque(false); setVisible(false);
            addMouseListener(new MouseAdapter() { });
            PixelButton yes = new PixelButton("Yes"), no = new PixelButton("No");
            yes.addActionListener(e -> { setVisible(false); finish(false); app.logout(); });
            no.addActionListener(e -> { setVisible(false); SettingsOverlay.this.repaint(); });
            UI.place(this, yes, 293, 409, 351, 123);
            UI.place(this, no, 824, 409, 351, 123);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            g.setColor(new Color(0, 0, 0, 200)); g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(UI.font(CONFIRM_SIZE)); g.setColor(Color.WHITE);
            UI.text(g, "Log Out?", 728, 226, 0, 700);
        }
    }

    private void heading(Graphics2D g, String s, double cy) {
        g.setFont(UI.font(HEADING_SIZE)); g.setColor(Color.BLACK);
        UI.text(g, s, PX + PW / 2, cy, 0, PW - 60);
    }

    private void field(Graphics2D g, double y, String... lines) {
        UI.draw(g, Assets.get("textfield.png"), 552, y, 330, FIELD_H);
        double cx = 552 + 330 / 2.0, cy = y + FIELD_H / 2, gap = 23;
        g.setColor(Color.BLACK);
        for (int i = 0; i < lines.length; i++) {
            g.setFont(UI.fitFont(g, lines[i], TRACK_SIZE, 250));
            UI.text(g, lines[i], cx, cy + (i - (lines.length - 1) / 2.0) * gap, 0, 250);
        }
        arrow(g, 494, y + 11, false); arrow(g, 914, y + 11, true);
    }

    private void label(Graphics2D g, String s, double cx, double cy) {
        g.setFont(UI.fitFont(g, s, LABEL_SIZE, 260));
        g.setColor(Color.BLACK);
        UI.text(g, s, cx, cy, 0, 260);
    }

    private void arrow(Graphics2D g, double x, double y, boolean right) {
        BufferedImage img = Assets.trimmed("arrow.png");
        double w = 26, h = 47;
        if (right) g.drawImage(img, UI.s(x + w), UI.s(y), -UI.s(w), UI.s(h), null);
        else g.drawImage(img, UI.s(x), UI.s(y), UI.s(w), UI.s(h), null);
    }

    private void sparkles(Graphics2D g, double x, double y, double w, double h) {
        double[][] pts = {{x, y + h * 0.2}, {x + w * 0.45, y - 6}, {x + w, y + h * 0.3}, {x + w * 0.4, y + h + 6}, {x - 4, y + h * 0.75}};
        g.setColor(new Color(250, 235, 60));
        for (double[] p : pts) { int c = UI.s(5); g.fillRect(UI.s(p[0]) - c, UI.s(p[1]) - c * 3, c * 2, c * 6); g.fillRect(UI.s(p[0]) - c * 3, UI.s(p[1]) - c, c * 6, c * 2); }
    }

    private static final class Hot extends JButton {
        String label; Runnable onClick; BufferedImage image; double fontSize = 28, fit = 0.72;
        private boolean hover;
        Hot(String label) {
            this.label = label;
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> { Sound.sfx("click"); if (onClick != null) onClick.run(); });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UI.nearest(g);
            if (image != null) g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            if (hover) { g.setColor(new Color(255, 255, 255, 45)); g.fillRect(0, 0, getWidth(), getHeight()); }
            if (!label.isEmpty()) {
                g.setFont(UI.fit(g, label, UI.font(fontSize), (int) (getWidth() * fit)));
                UI.centered(g, label, new Rectangle(0, 0, getWidth(), getHeight()), Color.BLACK);
            }
            g.dispose();
        }
    }

    private static final class Slider extends JComponent {
        int value = 50; java.util.function.IntConsumer onChange; Runnable onRelease;
        Slider() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { set(e.getX()); }
                @Override public void mouseDragged(MouseEvent e) { set(e.getX()); }
                @Override public void mouseReleased(MouseEvent e) { if (onRelease != null) onRelease.run(); }
            };
            addMouseListener(m); addMouseMotionListener(m);
        }
        private void set(int x) {
            int pad = getHeight() / 2;
            value = Math.max(0, Math.min(100, (int) Math.round((x - pad) * 100.0 / Math.max(1, getWidth() - 2 * pad))));
            if (onChange != null) onChange.accept(value);
            repaint();
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = getHeight() / 2, ty = getHeight() / 2 - UI.s(5), th = UI.s(10);
            g.setColor(new Color(165, 165, 165)); g.fillRoundRect(pad, ty, getWidth() - 2 * pad, th, th, th);
            int kx = pad + (int) ((getWidth() - 2 * pad) * value / 100.0);
            g.setColor(new Color(110, 110, 110)); g.fillOval(kx - pad, 0, getHeight(), getHeight());
        }
    }
}
