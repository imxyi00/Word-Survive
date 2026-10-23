package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

final class TitleScreen extends Screen {
    private final JPanel quitConfirm = new JPanel(null) {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            g.setColor(new Color(15, 20, 35, 210));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(UI.font(64));
            g.setColor(Color.WHITE);
            UI.text(g, "Quit?", 728, 232, 0, 700);
        }
    };

    TitleScreen(Main app) {
        super(app);
        quitConfirm.setOpaque(false);
        quitConfirm.setVisible(false);
        quitConfirm.addMouseListener(new MouseAdapter() { });
        PixelButton yes = new PixelButton("Yes"), no = new PixelButton("No");
        yes.addActionListener(e -> System.exit(0));
        no.addActionListener(e -> { quitConfirm.setVisible(false); repaint(); });
        UI.place(quitConfirm, yes, 282, 408, 372, 128);
        UI.place(quitConfirm, no, 813, 408, 372, 128);
        quitConfirm.setBounds(0, 0, UI.W, UI.H);
        add(quitConfirm);

        PixelButton quit = new PixelButton(Assets.trimmed("Off.png"));
        quit.addActionListener(e -> { quitConfirm.setVisible(true); repaint(); });
        place(quit, 63, 51, 86, 94);

        PixelButton start = new PixelButton("Start");
        start.addActionListener(e -> app.show("choice"));
        place(start, 542, 547, 372, 128);
        setComponentZOrder(quitConfirm, 0);
    }
    @Override void onShow() { quitConfirm.setVisible(false); }
    @Override protected void paintExtra(Graphics2D g) { UI.drawTrimmed(g, "LBL.png", 712, 141, 0.757); }
}

final class AuthChoiceScreen extends Screen {
    AuthChoiceScreen(Main app) {
        super(app);
        PixelButton login = new PixelButton("Log In");
        PixelButton signup = new PixelButton("Sign Up");
        login.addActionListener(e -> app.show("login"));
        signup.addActionListener(e -> app.show("signup"));
        place(login, 214, 553, 372, 128);
        place(signup, 843, 553, 372, 128);
        addBack("title", 40, 45);
    }
    @Override protected void paintExtra(Graphics2D g) { UI.drawTrimmed(g, "LBL.png", 712, 141, 0.757); }
}

final class AuthScreen extends Screen {
    private final boolean signup;
    private final JTextField user = PixelField.text();
    private final JPasswordField pass = PixelField.password();
    private final JLabel msg = new JLabel("", SwingConstants.CENTER);

    AuthScreen(Main app, boolean signup) {
        super(app);
        this.signup = signup;
        JLabel l1 = new JLabel("Email/Username", SwingConstants.CENTER), l2 = new JLabel("Password", SwingConstants.CENTER);
        for (JLabel l : new JLabel[]{l1, l2}) { l.setFont(UI.font(38)); l.setForeground(Color.BLACK); }
        place(l1, 82, 317, 632, 60);
        place(l2, 82, 504, 632, 60);
        place(user, 734, 284, 582, 125);
        place(pass, 734, 471, 582, 125);
        msg.setFont(UI.font(26)); msg.setForeground(new Color(190, 25, 25));
        place(msg, 300, 598, 860, 40);
        PixelButton play = new PixelButton("Play");
        play.addActionListener(e -> submit());
        user.addActionListener(e -> pass.requestFocusInWindow());
        pass.addActionListener(e -> submit());
        place(play, 542, 641, 372, 128);
        addBack("choice", 40, 45);
    }

    private void submit() {
        String name = user.getText().trim(), pw = new String(pass.getPassword());
        if (signup) {
            String err = app.users.register(name, pw);
            if (err != null) { msg.setText(err); return; }
            app.user = app.users.login(name, pw);
            app.theme = 0;
            app.show("avatar");
        } else {
            app.user = app.users.login(name, pw);
            if (app.user == null) { msg.setText("Wrong username or password."); return; }
            app.afterLogin();
        }
    }

    @Override void onShow() { user.setText(""); pass.setText(""); msg.setText(""); user.requestFocusInWindow(); }
    @Override protected void paintExtra(Graphics2D g) { UI.drawTrimmed(g, signup ? "SIGNUPLBL.png" : "LOGINLBL.png", 728, 75, 0.757); }
}

final class AvatarScreen extends Screen {
    private static final int[][] TILES = {{425, 222}, {650, 230}, {875, 222}, {518, 468}, {782, 468}};
    private int sel = 0;

    AvatarScreen(Main app) {
        super(app);
        PixelButton pick = new PixelButton("Pick");
        pick.addActionListener(e -> {
            app.user.avatar = sel;
            app.users.save();
            app.afterLogin();
        });
        place(pick, 620, 700, 230, 82);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                for (int i = 0; i < TILES.length; i++) {
                    Rectangle r = new Rectangle(UI.s(TILES[i][0]), UI.s(TILES[i][1]), UI.s(168), UI.s(218));
                    if (r.contains(e.getPoint())) { sel = i; Sound.sfx("select"); repaint(); return; }
                }
            }
        });
    }

    @Override void onShow() { sel = 0; }

    @Override protected void paintExtra(Graphics2D g) {
        UI.draw(g, Assets.blankButton(), 510, 40, 450, 155);
        g.setFont(UI.font(46));
        g.setColor(Color.BLACK);
        UI.centered(g, "Select Your", new Rectangle(UI.s(510), UI.s(62), UI.s(450), UI.s(50)), Color.BLACK);
        UI.centered(g, "Avatar", new Rectangle(UI.s(510), UI.s(108), UI.s(450), UI.s(50)), Color.BLACK);
        for (int i = 0; i < TILES.length; i++) {
            double x = TILES[i][0], y = TILES[i][1];
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i == sel ? 0.95f : 0.6f));
            UI.draw(g, Assets.trimmed("tileforselectingavatar.png"), x, y, 168, 218);
            g.setComposite(AlphaComposite.SrcOver);
            if (i == sel) { g.setColor(new Color(90, 225, 240, 150)); g.fillRect(UI.s(x + 12), UI.s(y + 14), UI.s(144), UI.s(190)); }
            Avatars.drawIn(g, i, x + 14, y + 40, 140, 140);
        }
    }
}

final class MenuScreen extends Screen {
    MenuScreen(Main app) {
        super(app);
        header = true;
        String[] labels = {"Play", "Profile", "Options"};
        Runnable[] acts = {() -> app.show("mode"), () -> app.show("profile"), () -> app.openSettings(null)};
        for (int i = 0; i < 3; i++) {
            PixelButton b = new PixelButton(labels[i]);
            Runnable a = acts[i];
            b.addActionListener(e -> a.run());
            place(b, 542, 287 + i * 157, 372, 128);
        }
    }
    @Override protected void paintExtra(Graphics2D g) { UI.drawTrimmed(g, "LBL.png", 1236, 35, 0.349); }
}

final class StackScreen extends Screen {
    StackScreen(Main app, String back, String[] labels, Runnable[] actions) {
        super(app);
        header = true;
        for (int i = 0; i < labels.length; i++) {
            PixelButton b = new PixelButton(labels[i]);
            Runnable a = actions[i];
            b.addActionListener(e -> a.run());
            place(b, 542, 287 + i * 157, 372, 128);
        }
        addBack(back, 45, 685);
    }
    @Override protected void paintExtra(Graphics2D g) { UI.drawTrimmed(g, "LBL.png", 1236, 35, 0.349); }
}

final class LeaderboardScreen extends Screen {
    private static final String[] MODES = {"STAGE", "RUSH60", "RUSH120", "RUSH180"};
    private static final String[] NAMES = {"Stage", "1 min", "2 min", "3 min"};
    private final PixelButton[] tabs = new PixelButton[4];
    private int selected = 0;

    LeaderboardScreen(Main app) {
        super(app);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            tabs[i] = new PixelButton(NAMES[i]);
            tabs[i].addActionListener(e -> { selected = idx; refreshTabs(); repaint(); });
            place(tabs[i], 262 + i * 235, 40, 220, 90);
        }
        addBack("mode", 45, 685);
    }

    private void refreshTabs() {
        for (int i = 0; i < 4; i++) tabs[i].setImage(i == selected ? Assets.tinted("green") : Assets.blankButton());
    }

    @Override void onShow() {
        if (app.user != null) app.scores.submit("STAGE", app.user.name, app.user.stageScore());
        refreshTabs();
    }

    @Override protected void paintExtra(Graphics2D g) {
        g.setColor(new Color(255, 245, 120));
        g.fillRect(UI.s(537), UI.s(252), UI.s(386), UI.s(490));
        UI.draw(g, Assets.trimmed("Leaderboard.png"), 458, 150, 540, 608);
        g.setFont(UI.fit(g, "LEADERBOARD", UI.font(46), UI.s(380)));
        UI.centered(g, "LEADERBOARD", new Rectangle(UI.s(458), UI.s(168), UI.s(540), UI.s(100)), new Color(90, 65, 0));
        List<ScoreBoard.Entry> top = app.scores.top(MODES[selected], 8);
        g.setFont(UI.font(32));
        if (top.isEmpty()) {
            UI.centered(g, "No scores yet", new Rectangle(UI.s(458), UI.s(380), UI.s(540), UI.s(80)), new Color(60, 45, 0));
            return;
        }
        for (int i = 0; i < top.size(); i++) {
            ScoreBoard.Entry e = top.get(i);
            double cy = 322 + i * 53;
            if (app.user != null && e.name.equalsIgnoreCase(app.user.name)) {
                g.setColor(new Color(255, 160, 0, 90)); g.fillRect(UI.s(545), UI.s(cy - 24), UI.s(370), UI.s(48));
            }
            g.setColor(new Color(60, 45, 0));
            g.setFont(UI.font(26));
            UI.text(g, (i + 1) + ".", 556, cy, -1, 40);
            UI.text(g, e.name, 600, cy, -1, 205);
            UI.text(g, String.valueOf(e.score), 905, cy, 1, 100);
        }
    }
}
