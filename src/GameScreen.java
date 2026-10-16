package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

final class GameScreen extends Screen {

    private GameSession s;
    private int rushSeconds;
    private javax.swing.Timer clock, pending;
    private int feedback;
    private String flash = "";
    private boolean busy, running;

    private final SentenceView sentence = new SentenceView();
    private final TextBox meaning = new TextBox();
    private final WheelView wheel = new WheelView();
    private final AnswerPill pill = new AnswerPill();
    private final PixelButton shuffle = new PixelButton(Assets.get("shuffle_button.png"));
    private final HintButton hint = new HintButton();
    private final Overlay overlay = new Overlay();

    GameScreen(Main app) {
        super(app);
        dim = true;
        JButton pause = new JButton() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.BLACK); g.setStroke(new BasicStroke(UI.s(7)));
                int m = UI.s(5); g.drawOval(m, m, getWidth() - 2 * m, getHeight() - 2 * m);
                int bw = UI.s(9), bh = UI.s(32), cx = getWidth() / 2, cy = getHeight() / 2;
                g.fillRect(cx - bw - UI.s(3), cy - bh / 2, bw, bh); g.fillRect(cx + UI.s(3), cy - bh / 2, bw, bh);
            }
        };
        pause.setOpaque(false); pause.setContentAreaFilled(false); pause.setBorderPainted(false); pause.setFocusable(false);
        pause.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pause.addActionListener(e -> { Sound.sfx("click"); pauseGame(); });
        place(pause, 70, 25, 90, 90);

        place(sentence, 448, 198, 545, 172);
        place(meaning, 70, 524, 338, 165);
        place(wheel, 535, 428, 375, 367);
        place(pill, 965, 468, 430, 147);
        place(shuffle, 928, 665, 112, 113);
        place(hint, 1080, 672, 295, 98);
        shuffle.addActionListener(e -> doShuffle());
        hint.addActionListener(e -> useHint());

        wheel.onPath = () -> pill.repaint();
        wheel.onSubmit = this::submit;
        sentence.onPick = i -> { if (!busy && running) { s.select(i); Sound.sfx("select"); refresh(); } };

        overlay.setBounds(0, 0, UI.W, UI.H);
        add(overlay);
        setComponentZOrder(overlay, 0);

        setFocusable(true);
        addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); } });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k >= KeyEvent.VK_A && k <= KeyEvent.VK_Z) wheel.typeLetter((char) k);
                else if (k == KeyEvent.VK_BACK_SPACE) wheel.backspace();
                else if (k == KeyEvent.VK_ENTER) wheel.release();
                else if (k == KeyEvent.VK_SPACE) doShuffle();
                else if (k == KeyEvent.VK_ESCAPE) pauseGame();
            }
        });
        clock = new javax.swing.Timer(100, e -> tick());
    }

    void beginStage(int level) { s = GameSession.stage(app.bank, level); start(); }
    void beginRush(int seconds) { rushSeconds = seconds; s = GameSession.rush(app.bank, seconds); start(); }

    private void start() {
        stopTimers();
        overlay.setVisible(false);
        feedback = 0; busy = false; running = true;
        refresh();
        clock.start();
    }

    private void refresh() {
        Puzzle.Blank b = s.current();
        meaning.setText(b == null ? "" : b.meaning);
        wheel.setLetters(b == null ? new char[0] : b.wheel);
        sentence.setPuzzle(s.puzzle, s.selected);
        repaint();
    }

    @Override void onShow() { requestFocusInWindow(); }
    @Override void onHide() { stopTimers(); running = false; }

    private void stopTimers() {
        clock.stop();
        if (pending != null) pending.stop();
    }

    private void later(int ms, Runnable r) {
        if (pending != null) pending.stop();
        pending = new javax.swing.Timer(ms, e -> r.run());
        pending.setRepeats(false);
        pending.start();
    }

    private void tick() {
        if (!running) return;
        s.tick(100);
        if (s.timeUp() && !overlay.isVisible()) endRush();
        repaint(0, 0, getWidth(), UI.s(130));
    }

    private void doShuffle() {
        if (!running || busy || s.current() == null) return;
        s.shuffle(); wheel.setLetters(s.current().wheel); Sound.sfx("shuffle");
    }

    private void useHint() {
        if (!running || busy) return;
        if (s.useHint(app.user)) { app.users.save(); Sound.sfx("hint"); pill.repaint(); hint.repaint(); repaint(); }
        else Sound.sfx("wrong");
        requestFocusInWindow();
    }

    private void submit(String attempt) {
        if (!running || busy || s.current() == null) return;
        GameSession.Result r = s.guess(attempt);
        switch (r) {
            case INCOMPLETE -> { }
            case CORRECT -> {
                feedback = 1; flash = attempt; busy = true; Sound.sfx("correct"); sentence.repaint();
                later(750, this::afterCorrect);
            }
            case REPEATED -> { feedback = 2; flash = attempt; busy = true; Sound.sfx("wrong"); later(500, this::resetFeedback); }
            case WRONG -> {
                feedback = 2; flash = attempt; busy = true; Sound.sfx("wrong");
                later(800, () -> {
                    if (s.outOfLives()) levelFailed();
                    else if (s.timeUp()) endRush();
                    else resetFeedback();
                });
            }
        }
        repaint();
    }

    private void resetFeedback() { feedback = 0; busy = false; pill.repaint(); repaint(); }

    private void afterCorrect() {
        feedback = 0; busy = false;
        if (s.puzzleDone()) {
            s.points += s.clearBonus();
            if (s.mode == GameSession.Mode.STAGE) { levelComplete(); return; }
            if (s.timeUp()) { endRush(); return; }
            s.puzzlesDone++;
            s.loadPuzzle();
        } else s.selectNextUnsolved();
        refresh();
    }

    private void levelComplete() {
        running = false; clock.stop();
        User u = app.user;
        boolean first = u.best[s.level - 1] == 0;
        int coins = GameSession.coinsFor(s.points, first);
        u.coins += coins;
        u.best[s.level - 1] = Math.max(u.best[s.level - 1], s.points);
        app.users.save();
        app.scores.submit("STAGE", u.name, u.stageScore());
        Sound.sfx("win");
        final int lvl = s.level;
        boolean last = lvl >= SentenceBank.LEVELS;
        overlay.open("LEVEL CLEAR!", "Points " + s.points + "    Coins +" + coins + (first ? "" : " (replay)"),
                new String[]{last ? "Replay" : "Next Level", "Levels"},
                new Runnable[]{() -> app.startStage(last ? lvl : lvl + 1), () -> app.show("levels")});
    }

    private void levelFailed() {
        running = false; clock.stop();
        int coins = GameSession.coinsFor(s.points, false);
        app.user.coins += coins; app.users.save();
        Sound.sfx("lose");
        final int lvl = s.level;
        overlay.open("OUT OF LIVES", "Points " + s.points + "    Coins +" + coins,
                new String[]{"Retry", "Levels"}, new Runnable[]{() -> app.startStage(lvl), () -> app.show("levels")});
    }

    private void endRush() {
        running = false; clock.stop();
        if (pending != null) pending.stop();
        int coins = GameSession.coinsFor(s.points, true);
        if (s.points > 0) { app.user.coins += coins; app.users.save(); app.scores.submit("RUSH" + rushSeconds, app.user.name, s.points); }
        Sound.sfx(s.points > 0 ? "win" : "lose");
        final int secs = rushSeconds;
        overlay.open("TIME UP!", "Points " + s.points + "    Coins +" + (s.points > 0 ? coins : 0),
                new String[]{"Play Again", "Menu"}, new Runnable[]{() -> app.startRush(secs), () -> app.show("rush")});
    }

    private void pauseGame() {
        if (s == null || !running || overlay.isVisible()) return;
        running = false;
        overlay.open("Paused", "", new String[]{"Resume", "Settings", "Quit"}, new Runnable[]{
                this::resume,
                () -> { overlay.setVisible(false); app.openSettings(() -> overlay.setVisible(true)); },
                () -> app.show(s.mode == GameSession.Mode.STAGE ? "levels" : "rush")});
    }

    private void resume() { overlay.setVisible(false); running = true; refresh(); requestFocusInWindow(); }

    @Override protected void paintExtra(Graphics2D g) {
        if (s == null) return;
        UI.draw(g, Assets.get("sentence_box.png"), 340, 135, 775, 300);
        UI.draw(g, Assets.get("Word_meaning_box.png"), 22, 441, 433, 331);
        UI.draw(g, Assets.trimmed("top_navigation.png"), 216, 0, 1024, 122);
        g.setFont(UI.font(40)); g.setColor(Color.BLACK);
        boolean stage = s.mode == GameSession.Mode.STAGE;
        if (stage) {
            UI.text(g, "LVL " + s.level, 330, 58, -1, 250);
            for (int i = 0; i < GameSession.MAX_LIVES; i++) drawHeart(g, 612 + i * 85, 30, i < s.lives);
        } else {
            long sec = (s.timeLeftMs + 999) / 1000;
            String t = String.format("%d:%02d", sec / 60, sec % 60);
            g.setFont(UI.font(60)); g.setColor(sec <= 10 ? new Color(190, 20, 20) : Color.BLACK);
            UI.text(g, t, 573, 57, 0, 260);
        }
        g.drawImage(Assets.get("coin.png"), UI.s(881), UI.s(27), UI.s(52), UI.s(56), null);
        g.setFont(UI.font(40)); g.setColor(Color.BLACK);
        UI.text(g, String.valueOf(app.user.coins), 947, 56, -1, 175);
        g.setFont(UI.font(22)); g.setColor(new Color(200, 235, 255));
        UI.text(g, "Points " + s.points, 30, 150, -1, 200);
    }

    private void drawHeart(Graphics2D g, double dx, double dy, boolean full) {
        UI.draw(g, full ? Assets.get("heart.png") : Assets.gray("heart.png"), dx, dy, 68, 59);
    }

    private static final class SentenceView extends JComponent {
        java.util.function.IntConsumer onPick;
        private Puzzle pz; private int selected;
        private final List<Object[]> hits = new ArrayList<>();

        SentenceView() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    for (Object[] h : hits) if (((Rectangle) h[0]).contains(e.getPoint()) && onPick != null) onPick.accept((Integer) h[1]);
                }
            });
        }
        void setPuzzle(Puzzle p, int sel) { pz = p; selected = sel; repaint(); }

        private String shown(Puzzle.Part p) {
            if (p.blank < 0) return p.text;
            Puzzle.Blank b = pz.blanks.get(p.blank);
            return b.solved ? b.word : "[" + "_".repeat(b.word.length()) + "]";
        }

        @Override protected void paintComponent(Graphics g0) {
            if (pz == null) return;
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            Font font = UI.font(30);
            List<List<List<Puzzle.Part>>> lines;
            int lh;
            while (true) {
                FontMetrics fm = g.getFontMetrics(font);
                lines = layout(fm, getWidth());
                lh = (int) (fm.getHeight() * 1.5);
                if (lines.size() * lh <= getHeight() || font.getSize2D() <= 11) break;
                font = font.deriveFont(font.getSize2D() - 1f);
            }
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            hits.clear();
            int y = (getHeight() - lines.size() * lh) / 2 + fm.getAscent();
            for (List<List<Puzzle.Part>> line : lines) {
                int total = 0;
                for (List<Puzzle.Part> chunk : line) total += width(fm, chunk) + fm.charWidth(' ');
                int x = (getWidth() - total + fm.charWidth(' ')) / 2;
                for (List<Puzzle.Part> chunk : line) {
                    for (Puzzle.Part p : chunk) {
                        String t = shown(p);
                        int w = fm.stringWidth(t);
                        if (p.blank >= 0) {
                            Puzzle.Blank b = pz.blanks.get(p.blank);
                            g.setColor(b.solved ? new Color(0, 150, 70) : p.blank == selected ? new Color(100, 40, 220) : new Color(30, 30, 30));
                            if (!b.solved) hits.add(new Object[]{new Rectangle(x - 4, y - fm.getAscent(), w + 8, lh), p.blank});
                        } else g.setColor(new Color(30, 30, 30));
                        g.drawString(t, x, y);
                        x += w;
                    }
                    x += fm.charWidth(' ');
                }
                y += lh;
            }
        }

        private int width(FontMetrics fm, List<Puzzle.Part> chunk) { int w = 0; for (Puzzle.Part p : chunk) w += fm.stringWidth(shown(p)); return w; }

        private List<List<List<Puzzle.Part>>> layout(FontMetrics fm, int maxW) {
            List<List<List<Puzzle.Part>>> lines = new ArrayList<>();
            List<List<Puzzle.Part>> cur = new ArrayList<>();
            int w = 0, sp = fm.charWidth(' ');
            for (List<Puzzle.Part> chunk : pz.chunks) {
                int cw = width(fm, chunk);
                if (!cur.isEmpty() && w + sp + cw > maxW) { lines.add(cur); cur = new ArrayList<>(); w = 0; }
                w += (cur.isEmpty() ? 0 : sp) + cw;
                cur.add(chunk);
            }
            if (!cur.isEmpty()) lines.add(cur);
            return lines;
        }
    }

    private static final class TextBox extends JComponent {
        private String text = "";
        void setText(String t) { text = t; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            Font font = UI.font(28);
            List<String> lines = wrap(g, font);
            while ((lines.size() * (int) (g.getFontMetrics(font).getHeight() * 1.5) > getHeight() || widest(g, font, lines) > getWidth()) && font.getSize2D() > 11) {
                font = font.deriveFont(font.getSize2D() - 1f);
                lines = wrap(g, font);
            }
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int lh = (int) (fm.getHeight() * 1.5);
            int y = (getHeight() - lines.size() * lh) / 2 + fm.getAscent();
            g.setColor(new Color(30, 30, 30));
            for (String l : lines) { g.drawString(l, (getWidth() - fm.stringWidth(l)) / 2, y); y += lh; }
        }
        private List<String> wrap(Graphics2D g, Font f) {
            FontMetrics fm = g.getFontMetrics(f);
            List<String> out = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String w : text.split("\\s+")) {
                String test = line.length() == 0 ? w : line + " " + w;
                if (line.length() > 0 && fm.stringWidth(test) > getWidth()) { out.add(line.toString()); line = new StringBuilder(w); }
                else line = new StringBuilder(test);
            }
            if (line.length() > 0) out.add(line.toString());
            return out;
        }
        private int widest(Graphics2D g, Font f, List<String> lines) { int m = 0; FontMetrics fm = g.getFontMetrics(f); for (String l : lines) m = Math.max(m, fm.stringWidth(l)); return m; }
    }

    private final class AnswerPill extends JComponent {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            BufferedImage im = feedback == 1 ? Assets.tinted("green") : feedback == 2 ? Assets.tinted("red") : Assets.blankButton();
            g.drawImage(im, 0, 0, getWidth(), getHeight(), null);
            if (s == null || s.current() == null) return;
            String t = feedback != 0 ? flash.toUpperCase() : wheel.word().isEmpty() ? s.pattern().replace(" ", "") .isEmpty() ? "" : spaced(s.pattern()) : wheel.word();
            g.setFont(UI.fit(g, t, UI.font(62), (int) (getWidth() * 0.80)));
            UI.centered(g, t, new Rectangle(0, 0, getWidth(), getHeight()), Color.BLACK);
        }
        private String spaced(String p) { return p; }
    }

    private final class HintButton extends JButton {
        HintButton() {
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            boolean ok = s != null && running && !busy && s.canHint(app.user);
            if (!ok) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.drawImage(Assets.get("yellow_button.png"), 0, 0, getWidth(), getHeight(), null);
            g.drawImage(Assets.get("hint_logo.png"), UI.s(14), UI.s(8), UI.s(48), UI.s(54), null);
            g.setFont(UI.font(32)); g.setColor(Color.BLACK);
            UI.text(g, "Hint", 100, 30, -1, 170);
            g.drawImage(Assets.get("coin.png"), UI.s(98), UI.s(52), UI.s(30), UI.s(32), null);
            g.setFont(UI.font(30));
            UI.text(g, String.valueOf(GameSession.HINT_COST), 138, 68, -1, 110);
        }
    }

    private static final class WheelView extends JComponent {
        Runnable onPath; java.util.function.Consumer<String> onSubmit;
        private char[] letters = new char[0];
        private final ArrayDeque<Integer> path = new ArrayDeque<>();
        private Point mouse;

        WheelView() {
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int i = letterAt(e.getPoint());
                    path.clear();
                    if (i >= 0) { path.push(i); Sound.sfx("select"); }
                    mouse = e.getPoint(); changed();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    mouse = e.getPoint();
                    int i = letterAt(e.getPoint());
                    if (i >= 0 && !path.isEmpty() && i != path.peek()) {
                        Integer top = path.pop();
                        Integer below = path.peek();
                        if (below != null && below == i) {  }
                        else { path.push(top); if (!path.contains(i)) { path.push(i); Sound.sfx("select"); } }
                    }
                    changed();
                }
                @Override public void mouseReleased(MouseEvent e) { mouse = null; release(); }
            };
            addMouseListener(m); addMouseMotionListener(m);
        }

        void setLetters(char[] l) { letters = l; path.clear(); changed(); }
        private void changed() { if (onPath != null) onPath.run(); repaint(); }

        String word() {
            StringBuilder sb = new StringBuilder();
            Object[] idx = path.toArray();
            for (int k = idx.length - 1; k >= 0; k--) sb.append(letters[(Integer) idx[k]]);
            return sb.toString();
        }

        void release() {
            String w = word();
            path.clear(); changed();
            if (!w.isEmpty() && onSubmit != null) onSubmit.accept(w);
        }

        void typeLetter(char c) {
            for (int i = 0; i < letters.length; i++) if (letters[i] == c && !path.contains(i)) { path.push(i); Sound.sfx("select"); changed(); return; }
        }
        void backspace() { if (!path.isEmpty()) { path.pop(); changed(); } }

        private Point center(int i) {
            double r = Math.min(getWidth(), getHeight()) * 0.34, a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, letters.length);
            return new Point((int) (getWidth() / 2 + r * Math.cos(a)), (int) (getHeight() / 2 + r * Math.sin(a)));
        }
        private int letterAt(Point p) {
            int hit = Math.min(getWidth(), getHeight()) / 9;
            for (int i = 0; i < letters.length; i++) if (center(i).distance(p) <= hit) return i;
            return -1;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            UI.nearest(g);
            g.drawImage(Assets.get("wordscape_circle.png"), 0, 0, getWidth(), getHeight(), null);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int rad = Math.min(getWidth(), getHeight()) / 11;
            Object[] idx = path.toArray();
            if (idx.length > 0) {
                g.setColor(new Color(255, 255, 255, 200)); g.setStroke(new BasicStroke(UI.s(12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = idx.length - 1; k > 0; k--) { Point a = center((Integer) idx[k]), b = center((Integer) idx[k - 1]); g.drawLine(a.x, a.y, b.x, b.y); }
                if (mouse != null) { Point a = center((Integer) idx[0]); g.drawLine(a.x, a.y, mouse.x, mouse.y); }
            }
            g.setFont(UI.font(letters.length > 9 ? 46 : 60));
            for (int i = 0; i < letters.length; i++) {
                Point c = center(i);
                if (path.contains(i)) { g.setColor(new Color(10, 70, 150, 190)); g.fillOval(c.x - rad, c.y - rad, rad * 2, rad * 2); }
                UI.centered(g, String.valueOf(letters[i]), new Rectangle(c.x - rad, c.y - rad, rad * 2, rad * 2), Color.WHITE);
            }
        }
    }

    private final class Overlay extends JPanel {
        private final JLabel title = new JLabel("", SwingConstants.CENTER), msg = new JLabel("", SwingConstants.CENTER);
        private final PixelButton[] btn = {new PixelButton(""), new PixelButton(""), new PixelButton("")};

        Overlay() {
            setLayout(null); setOpaque(false); setVisible(false);
            addMouseListener(new MouseAdapter() { });
            title.setFont(UI.font(78)); title.setForeground(Color.WHITE);
            msg.setFont(UI.font(36)); msg.setForeground(new Color(255, 230, 120));
            UI.place(this, title, 100, 170, 1256, 110);
            UI.place(this, msg, 100, 290, 1256, 70);
            for (PixelButton b : btn) UI.place(this, b, 0, 0, 10, 10);
        }

        void open(String t, String m, String[] labels, Runnable[] actions) {
            title.setText(t); msg.setText(m);
            double w = 370, gap = 60, total = labels.length * w + (labels.length - 1) * gap, x = (1456 - total) / 2;
            for (int i = 0; i < btn.length; i++) {
                PixelButton b = btn[i];
                for (ActionListener al : b.getActionListeners()) if (!(al.getClass().getName().contains("PixelButton"))) b.removeActionListener(al);
                b.setVisible(i < labels.length);
                if (i < labels.length) {
                    b.setCaption(labels[i]);
                    final Runnable a = actions[i];
                    b.addActionListener(e -> a.run());
                    b.setBounds(UI.s(x + i * (w + gap)), UI.s(430), UI.s(w), UI.s(125));
                }
            }
            setVisible(true);
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(0, 0, 0, 225));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
