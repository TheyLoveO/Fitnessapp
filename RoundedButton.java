import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;

public class RoundedButton extends JButton {
    private boolean hover = false, pressed = false;
    private Color c1 = Theme.RED, c2 = Theme.YELLOW;
    RoundedButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(Theme.BUTTON_FONT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 16, 10, 16));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { hover = false; repaint(); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e){ pressed = false; repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        Color start = c1, end = c2;
        if (pressed) { start = start.darker(); end = end.darker(); }
        else if (hover) { start = start.brighter(); end = end.brighter(); }
        g2.setPaint(new GradientPaint(0, 0, start, w, h, end));
        g2.fillRoundRect(0, 0, w, h, Theme.RADIUS * 2, Theme.RADIUS * 2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g2.setPaint(Color.white);
        g2.fillRoundRect(2, 2, w - 4, h / 2, Theme.RADIUS, Theme.RADIUS);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(new Color(0,0,0,40));
        g2.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS * 2, Theme.RADIUS * 2);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(getText()), th = fm.getAscent();
        g2.setColor(getForeground());
        g2.drawString(getText(), (w - tw) / 2, (h + th) / 2 - 2);
        g2.dispose();
    }
}