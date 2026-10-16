package wordsurvive;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Main {
    final UserManager users = new UserManager();
    final SentenceBank bank = new SentenceBank();
    final ScoreBoard scores = new ScoreBoard();

    User user;
    int theme = 0;
    String current, previous;

    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    final JPanel root = new JPanel(cards);
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final GameScreen game;
    final SettingsOverlay settings;

    Main() {
        game = new GameScreen(this);
        settings = new SettingsOverlay(this);
        add("title", new TitleScreen(this));
        add("choice", new AuthChoiceScreen(this));
        add("login", new AuthScreen(this, false));
        add("signup", new AuthScreen(this, true));
        add("avatar", new AvatarScreen(this));
        add("menu", new MenuScreen(this));
        add("mode", new StackScreen(this, "menu", new String[]{"Stage", "Speed Rush", "Leaderboard"},
                new Runnable[]{() -> show("levels"), () -> show("rush"), () -> show("leaderboard")}));
        add("rush", new StackScreen(this, "mode", new String[]{"1 minute", "2 minutes", "3 minutes"},
                new Runnable[]{() -> startRush(60), () -> startRush(120), () -> startRush(180)}));
        add("levels", new LevelScreen(this));
        add("leaderboard", new LeaderboardScreen(this));
        add("profile", new ProfileScreen(this));
        add("game", game);
        root.setPreferredSize(new java.awt.Dimension(UI.W, UI.H));
    }

    private void add(String name, Screen s) { screens.put(name, s); root.add(s, name); }

    String themeBg() { return theme == 1 ? "Another_Background.png" : "background.png"; }

    void show(String name) {
        Screen old = screens.get(current);
        boolean sameScreen = name.equals(current);

        if (old != null && !sameScreen) old.onHide();
        if (!sameScreen) { previous = current; current = name; }
        cards.show(root, name);
        screens.get(name).onShow();
        root.repaint();
    }

    void startStage(int level) { show("game"); game.beginStage(level); }
    void startRush(int seconds) { show("game"); game.beginRush(seconds); }
    void openSettings(Runnable onClose) { settings.open(onClose); }

    void afterLogin() {
        theme = user.theme;
        Sound.bgVol = user.bgVol; Sound.sfxVol = user.sfxVol; Sound.track = user.music;
        Sound.startMusic();
        show("menu");
    }

    void logout() { user = null; theme = 0; show("title"); }

    static final String[][] RES_LABELS = {{"1920 x 1080", "Borderless"}, {"1920 x 1080", "Bordered"}, {"1280 x 720"}};
    private static final int[][] RES = {{1920, 1080, 1}, {1920, 1080, 0}, {1280, 720, 0}};
    private static final java.nio.file.Path SETTINGS = java.nio.file.Paths.get("data", "settings.properties");
    int resolution;
    private JFrame frame;
    private Timer ticker;

    private static int loadResolution() {
        java.util.Properties p = new java.util.Properties();
        try (java.io.Reader r = java.nio.file.Files.newBufferedReader(SETTINGS)) {
            p.load(r);
            int v = Integer.parseInt(p.getProperty("resolution"));
            if (v >= 0 && v < RES.length) return v;
        } catch (Exception ignored) { }
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        return screen.width >= 1920 && screen.height >= 1080 ? 0 : 2;
    }

    private static void saveResolution(int v) {
        java.util.Properties p = new java.util.Properties();
        p.setProperty("resolution", String.valueOf(v));
        try {
            java.nio.file.Files.createDirectories(SETTINGS.getParent());
            try (java.io.Writer w = java.nio.file.Files.newBufferedWriter(SETTINGS)) { p.store(w, "Word Survive settings"); }
        } catch (java.io.IOException e) { System.err.println("Could not save settings: " + e.getMessage()); }
    }

    /** Rebuilds every screen at the new scale (layout is computed in constructors) and keeps the session. */
    void changeResolution(int res) {
        saveResolution(res);
        Main m = start(res);
        if (user != null) { m.user = m.users.get(user.name); m.theme = user.theme; }
        if (m.user != null) m.show(current.equals("game") ? "menu" : current);
        screens.get(current).onHide();
        ticker.stop();
        frame.dispose();
    }

    private static Main start(int res) {
        UI.setSize(RES[res][0], RES[res][1]);
        Main m = new Main();
        m.resolution = res;
        m.launch(RES[res][2] == 1);
        return m;
    }

    void launch(boolean borderless) {
        JFrame f = frame = new JFrame("Word Survive");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (borderless) f.setUndecorated(true);
        f.setContentPane(root);
        f.setGlassPane(settings);
        f.setResizable(false);
        f.pack();
        java.awt.Rectangle b = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        f.setLocation(b.x + Math.max(0, (b.width - f.getWidth()) / 2), b.y + Math.max(0, (b.height - f.getHeight()) / 2));
        show("title");
        ticker = new Timer(50, e -> {
            Screen.tick++;
            Screen s = screens.get(current);
            if (s != null && s.clouds && theme == 0) s.repaint();
        });
        ticker.start();
        f.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
                if (e.getID() == java.awt.event.KeyEvent.KEY_PRESSED && e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_Q) System.exit(0);
                return false;
            });
            start(loadResolution());
        });
    }
}
