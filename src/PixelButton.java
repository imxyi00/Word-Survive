package wordsurvive;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

final class PixelButton extends JButton {
    private BufferedImage img;
    private String label;
    private boolean hover, pressed;

    PixelButton(String label) { this(Assets.blankButton(), label); }
    PixelButton(BufferedImage img) { this(img, null); }

    PixelButton(BufferedImage img, String label) {
        this.img = img; this.label = label;
        setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addActionListener(e -> Sound.sfx("click"));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    void setCaption(String label) { this.label = label; repaint(); }
    void setImage(BufferedImage img) { this.img = img; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UI.nearest(g);
        if (!isEnabled()) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        int grow = hover && isEnabled() ? 4 : 0, dy = pressed ? 3 : 0;
        Rectangle r = new Rectangle(-grow, -grow + dy, getWidth() + 2 * grow, getHeight() + 2 * grow);
        g.drawImage(img, r.x, r.y, r.width, r.height, null);
        if (label != null) {
            g.setFont(UI.fit(g, label, UI.font(46), (int) (getWidth() * 0.78)));
            UI.centered(g, label, r, Color.BLACK);
        }
        g.dispose();
    }
}
