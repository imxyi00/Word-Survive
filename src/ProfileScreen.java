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
    private final JTextField nameField = new JTextField();
    private final JTextArea bioArea = new JTextArea();
    private final JLabel msg = new JLabel("", SwingConstants.CENTER);
    private final JComponent picker = new JComponent() { };

    ProfileScreen(Main app) {
        super(app);
        dim = true;
        addBack("menu", 40, 45);
        edit.addActionListener(e -> setEditing(true));
        place(edit, 522, 497, 212, 73);

        nameField.setOpaque(false);
        nameField.setForeground(INK);
        nameField.setCaretColor(INK);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);
        nameField.setFont(UI.font(21));
        nameField.setBorder(BorderFactory.createEmptyBorder(0, UI.s(16), 0, UI.s(64)));
        place(nameField, 860, 214, 392, 60);
        pencil = new PixelButton(Assets.trimmed("change_name_icon.png"));
        pencil.addActionListener(e -> { nameField.requestFocusInWindow(); nameField.selectAll(); });
        place(pencil, 1201, 226, 33, 33);
        setComponentZOrder(pencil, 0);

        bioArea.setOpaque(false);
        bioArea.setForeground(INK);
        bioArea.setCaretColor(INK);
        bioArea.setLineWrap(true); bioArea.setWrapStyleWord(true); bioArea.setFont(UI.font(18));
        bioArea.setBorder(BorderFactory.createEmptyBorder(UI.s(10), UI.s(14), UI.s(8), UI.s(14)));
        place(bioArea, 863, 496, 392, 72);
        msg.setFont(UI.font(18)); msg.setForeground(new Color(170, 20, 20));
        place(msg, 855, 648, 410, 24);

        cancel = new PixelButton(Assets.get("red_button.png"), "Cancel");
        save = new PixelButton(Assets.get("green_button.png"), "Save");
        cancel.addActionListener(e -> setEditing(false));
        save.addActionListener(e -> saveEdits());
        place(cancel, 871, 581, 178, 62);
        place(save, 1080, 581, 181, 62);

        place(picker, 824, 326, 460, 124);
        picker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        picker.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                double dx = e.getX() / UI.S;
                if (dx < 38) offset += Avatars.COUNT - 1;
                else if (dx > 420) offset += 1;
                else {
                    int k = (int) ((dx - 39) / 134);
                    if (k >= 0 && k < 3 && (dx - 39) % 134 < 118) picked = (offset + k) % Avatars.COUNT;
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
        double w = 30, h = 55;
        if (right) g.drawImage(img, UI.s(x + w), UI.s(y), -UI.s(w), UI.s(h), null);
        else g.drawImage(img, UI.s(x), UI.s(y), UI.s(w), UI.s(h), null);
    }

    @Override protected void paintExtra(Graphics2D g) {
        User u = app.user;
        UI.draw(g, Assets.get("profile_ui1.png"), 146, 283, 582, 259);
        UI.draw(g, Assets.get("profile_ui2.png"), 738, 78, editing ? 613 : 586, editing ? 689 : 659);
        Avatars.drawCircle(g, editing ? picked : u.avatar, 181, 203, 184);

        put(g, u.name, 485, 333, 40, 260);
        put(g, "Account Created:", 444, 408, 25, 440);
        String when;
        try { when = LocalDate.parse(u.created).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)); }
        catch (RuntimeException e) { when = u.created; }
        put(g, when, 444, 442, 25, 440);

        if (!editing) {
            put(g, "STATS", 1040, 128, 40, 330);
            put(g, "Stage Completed", 1040, 207, 25, 380);
            put(g, u.cleared() + "/" + SentenceBank.LEVELS, 1040, 252, 26, 380);
            putLeft(g, "Bio", 863, 508, 26, 200);
            g.setColor(new Color(252, 238, 190));
            g.fillRoundRect(UI.s(866), UI.s(534), UI.s(363), UI.s(77), UI.s(14), UI.s(14));
            g.setColor(INK); g.setStroke(new BasicStroke(UI.s(3)));
            g.drawRoundRect(UI.s(866), UI.s(534), UI.s(363), UI.s(77), UI.s(14), UI.s(14));
            wrap(g, u.bio.isEmpty() ? "-" : u.bio, UI.s(885), UI.s(562), UI.s(330), 20);
        } else {
            put(g, "EDIT PROFILE", 1052, 150, 38, 360);
            putLeft(g, "Username:", 868, 200, 20, 330);
            g.setColor(new Color(252, 238, 190));
            g.fillRoundRect(UI.s(860), UI.s(214), UI.s(392), UI.s(60), UI.s(14), UI.s(14));
            g.setColor(INK); g.setStroke(new BasicStroke(UI.s(3)));
            g.drawRoundRect(UI.s(860), UI.s(214), UI.s(392), UI.s(60), UI.s(14), UI.s(14));
            g.drawRoundRect(UI.s(1194), UI.s(219), UI.s(48), UI.s(47), UI.s(10), UI.s(10));

            putLeft(g, "Select Avatar:", 863, 306, 20, 330);
            putLeft(g, "Edit Bio:", 868, 480, 20, 330);
            g.setColor(new Color(252, 238, 190));
            g.fillRoundRect(UI.s(863), UI.s(496), UI.s(392), UI.s(72), UI.s(14), UI.s(14));
            g.setColor(INK); g.setStroke(new BasicStroke(UI.s(3)));
            g.drawRoundRect(UI.s(863), UI.s(496), UI.s(392), UI.s(72), UI.s(14), UI.s(14));

            for (int k = 0; k < 3; k++) {
                int avatar = (offset + k) % Avatars.COUNT;
                double x = 863 + k * 134, y = 328;
                g.setColor(avatar == picked ? new Color(95, 230, 240) : new Color(250, 238, 195));
                g.fillRoundRect(UI.s(x), UI.s(y), UI.s(118), UI.s(120), UI.s(14), UI.s(14));
                g.setColor(avatar == picked ? new Color(30, 30, 190) : INK); g.setStroke(new BasicStroke(UI.s(3)));
                g.drawRoundRect(UI.s(x), UI.s(y), UI.s(118), UI.s(120), UI.s(14), UI.s(14));
                Avatars.drawIn(g, avatar, x + 12, y + 12, 94, 96);
            }
            arrow(g, 828, 363, false);
            arrow(g, 1250, 361, true);
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
