package wordsurvive;

import javax.swing.*;
import java.awt.*;

final class PixelField {
    private PixelField() { }

    private static void style(JTextField f) {
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(0, UI.s(45), 0, UI.s(45)));
        f.setFont(UI.font(30));
        f.setForeground(Color.BLACK);
        f.setCaretColor(Color.BLACK);
    }

    private static void skin(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0;
        UI.nearest(g);
        g.drawImage(Assets.get("textfield.png"), 0, 0, c.getWidth(), c.getHeight(), null);
    }

    static JTextField text() {
        JTextField f = new JTextField() { @Override protected void paintComponent(Graphics g) { skin(g, this); super.paintComponent(g); } };
        style(f);
        return f;
    }

    static JPasswordField password() {
        JPasswordField f = new JPasswordField() { @Override protected void paintComponent(Graphics g) { skin(g, this); super.paintComponent(g); } };
        style(f);
        f.setEchoChar('*');
        return f;
    }
}
