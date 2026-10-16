package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ProfileScreen extends Screen {
    private static final Color INK = new Color(25, 25, 25);
    private static final int BIO_MAX = 70;
    private boolean editing;
    private int offset, picked;
    private final PixelButton edit = new PixelButton("Edit"), cancel, save, pencil;
    private final JTextField nameField = PixelField.text();
    private final JTextArea bioArea = new JTextArea();
    private final JLabel msg = new JLabel("", SwingConstants.CENTER);
    private final JComponent picker = new JComponent() { };

    ProfileScreen(Main app) {
        super(app);
        dim = true;
        addBack("menu", 60, 30);
        edit.addActionListener(e -> setEditing(true));
        place(edit, 527, 500, 206, 68);

        nameField.setFont(UI.font(24));
        nameField.setBorder(BorderFactory.createEmptyBorder(0, UI.s(22), 0, UI.s(62)));
        place(nameField, 858, 202, 378, 56);
        pencil = new PixelButton(Assets.get("change_name_icon.png"));
        pencil.addActionListener(e -> { nameField.requestFocusInWindow(); nameField.selectAll(); });
        place(pencil, 1186, 207, 46, 45);
        setComponentZOrder(pencil, 0);

        bioArea.setLineWrap(true); bioArea.setWrapStyleWord(true); bioArea.setFont(UI.font(18));
        bioArea.setBackground(new Color(252, 238, 190));
        bioArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(INK, 3, true), BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        place(bioArea, 858, 468, 378, 62);
        msg.setFont(UI.font(18)); msg.setForeground(new Color(170, 20, 20));
        place(msg, 850, 610, 400, 24);

        cancel = new PixelButton(Assets.tinted("red"), "Cancel");
        save = new PixelButton(Assets.tinted("green"), "Save");
        cancel.addActionListener(e -> setEditing(false));
        save.addActionListener(e -> saveEdits());
        place(cancel, 860, 548, 180, 58);
        place(save, 1058, 548, 180, 58);

        place(picker, 822, 306, 454, 120);
        picker.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                double dx = e.getX() / UI.S;
                if (dx < 44) offset += Avatars.COUNT - 1;
                else if (dx > 410) offset += 1;
                else {
                    int k = (int) ((dx - 46) / 120);
                    if (k >= 0 && k < 3 && (dx - 46) % 120 < 108) picked = (offset + k) % Avatars.COUNT;
                }
                Sound.sfx("select");
                repaint();
            }
        });
        setEditing(false);
    }

    private void setEditing(boolean on) {
        editing = on;
        edit.setVisible(!on);
        for (JComponent c : new JComponent[]{nameField, pencil, bioArea, msg, cancel, save, picker}) c.setVisible(on);
        if (on) {
            nameField.setText(app.user.name); bioArea.setText(app.user.bio);
            picked = app.user.avatar; offset = picked; msg.setText("");
        }
        repaint();
    }

    private void saveEdits() {
        String err = app.users.rename(app.user, nameField.getText());
        if (err != null) { msg.setText(err); return; }
        app.user.avatar = picked;
        String bio = bioArea.getText().replaceAll("\\s+", " ").trim();
        app.user.bio = bio.length() > BIO_MAX ? bio.substring(0, BIO_MAX) : bio;
        app.users.save();
        setEditing(false);
    }

    @Override void onShow() { setEditing(false); }

    private void put(Graphics2D g, String s, double cx, double cy, double size, double maxW) {
        g.setFont(UI.font(size));
        g.setColor(INK);
        UI.text(g, s, cx, cy, 0, maxW);
    }

    private void putLeft(Graphics2D g, String s, double x, double cy, double size, double maxW) {
        g.setFont(UI.font(size));
        g.setColor(INK);
        UI.text(g, s, x, cy, -1, maxW);
    }

    private void arrow(Graphics2D g, double x, double y, boolean right) {
        java.awt.image.BufferedImage img = Assets.trimmed("arrow.png");
        double w = 22, h = 40;
        if (right) g.drawImage(img, UI.s(x + w), UI.s(y), -UI.s(w), UI.s(h), null);
        else g.drawImage(img, UI.s(x), UI.s(y), UI.s(w), UI.s(h), null);
    }

    @Override protected void paintExtra(Graphics2D g) {
        User u = app.user;
        UI.draw(g, Assets.get("profile_ui1.png"), 150, 284, 580, 258);
        UI.draw(g, Assets.get("profile_ui2.png"), 739, 78, 586, 658);
        Avatars.drawCircle(g, editing ? picked : u.avatar, 186, 168, 184);

        put(g, u.name, 520, 330, 42, 250);
        put(g, "Account Created:", 450, 406, 26, 440);
        String when;
        try { when = LocalDate.parse(u.created).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)); }
        catch (RuntimeException e) { when = u.created; }
        put(g, when, 450, 442, 26, 440);

        if (!editing) {
            put(g, "STATS", 1046, 128, 40, 330);
            put(g, "Stage Completed", 1046, 207, 26, 380);
            put(g, u.cleared() + "/" + SentenceBank.LEVELS, 1046, 252, 30, 380);
            put(g, "Points: " + u.stageScore(), 1046, 325, 24, 380);
            g.drawImage(Assets.get("coin.png"), UI.s(962), UI.s(352), UI.s(42), UI.s(45), null);
            put(g, String.valueOf(u.coins), 1100, 375, 30, 150);
            putLeft(g, "Bio", 866, 506, 28, 200);
            g.setColor(new Color(252, 238, 190));
            g.fillRoundRect(UI.s(866), UI.s(530), UI.s(356), UI.s(72), UI.s(14), UI.s(14));
            g.setColor(INK); g.setStroke(new BasicStroke(UI.s(3)));
            g.drawRoundRect(UI.s(866), UI.s(530), UI.s(356), UI.s(72), UI.s(14), UI.s(14));
            wrap(g, u.bio.isEmpty() ? "-" : u.bio, UI.s(880), UI.s(554), UI.s(330), 17);
        } else {
            put(g, "EDIT PROFILE", 1046, 140, 34, 330);
            putLeft(g, "Username:", 860, 186, 20, 330);
            putLeft(g, "Select Avatar:", 860, 288, 20, 330);
            putLeft(g, "Edit Bio:", 860, 448, 20, 330);
            for (int k = 0; k < 3; k++) {
                int avatar = (offset + k) % Avatars.COUNT;
                double x = 868 + k * 120, y = 312;
                g.setColor(avatar == picked ? new Color(95, 230, 240) : new Color(250, 238, 195));
                g.fillRoundRect(UI.s(x), UI.s(y), UI.s(108), UI.s(108), UI.s(14), UI.s(14));
                g.setColor(avatar == picked ? new Color(30, 30, 190) : INK); g.setStroke(new BasicStroke(UI.s(3)));
                g.drawRoundRect(UI.s(x), UI.s(y), UI.s(108), UI.s(108), UI.s(14), UI.s(14));
                Avatars.drawIn(g, avatar, x + 10, y + 10, 88, 88);
            }
            arrow(g, 824, 346, false);
            arrow(g, 1252, 346, true);
        }
    }

    private void wrap(Graphics2D g, String s, int x, int y, int width, double size) {
        g.setFont(UI.font(size));
        g.setColor(INK);
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int yy = y;
        for (String w : s.split(" ")) {
            if (line.length() > 0 && fm.stringWidth(line + " " + w) > width) { g.drawString(line.toString(), x, yy); yy += fm.getHeight() + UI.s(2); line = new StringBuilder(w); }
            else line.append(line.length() > 0 ? " " : "").append(w);
        }
        g.drawString(line.toString(), x, yy);
    }
}
