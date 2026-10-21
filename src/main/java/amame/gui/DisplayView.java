/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

import amame.emu.Panel;


/**
 * Shows a {@link Panel.Display}: a dot matrix lcd when the display hands out glyphs, a vfd in
 * text when it does not. It reads the display on every paint, a timer of the panel repaints.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class DisplayView extends JComponent {

    /** the lcd of the FB-01, the colors of mame's palette */
    private static final Color LCD_BACK = new Color(30, 0, 0);
    private static final Color LCD_ON = new Color(230, 30, 20);
    private static final Color LCD_OFF = new Color(55, 8, 6);

    /** a blue green vfd */
    private static final Color VFD_BACK = new Color(8, 14, 16);
    private static final Color VFD_ON = new Color(110, 255, 230);

    private static final int DOT = 4;
    private static final int GAP = 1;

    private Panel.Display display;
    private final Font font = new Font(Font.MONOSPACED, Font.BOLD, 18);

    public void setDisplay(Panel.Display display) {
        this.display = display;
        revalidate();
        repaint();
    }

    private boolean isDotMatrix() {
        return display != null && display.getGlyph(0, 0) != null;
    }

    @Override
    public Dimension getPreferredSize() {
        if (display == null) return new Dimension(400, 60);
        if (isDotMatrix()) {
            int w = display.getGlyphWidth();
            return new Dimension(display.getColumns() * (w + 1) * (DOT + GAP) + 24, display.getRows() * 9 * (DOT + GAP) + 24);
        }
        FontMetrics fm = getFontMetrics(font);
        return new Dimension(display.getColumns() * fm.charWidth('M') + 32, display.getRows() * (fm.getHeight() + 4) + 24);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Panel.Display d = display;
        boolean dots = isDotMatrix();
        g2.setColor(dots ? LCD_BACK : VFD_BACK);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        if (d == null) return;
        if (dots) {
            paintDots(g2, d);
        } else {
            paintText(g2, d);
        }
    }

    private void paintDots(Graphics2D g, Panel.Display d) {
        int w = d.getGlyphWidth();
        int cellW = (w + 1) * (DOT + GAP);
        int cellH = 9 * (DOT + GAP);
        int x0 = (getWidth() - d.getColumns() * cellW) / 2;
        int y0 = (getHeight() - d.getRows() * cellH) / 2;
        for (int r = 0; r < d.getRows(); r++) {
            for (int c = 0; c < d.getColumns(); c++) {
                int[] glyph = d.getGlyph(r, c);
                for (int y = 0; y < 8; y++) {
                    int line = glyph != null ? glyph[y] : 0;
                    for (int x = 0; x < w; x++) {
                        boolean on = (line & (1 << (w - 1 - x))) != 0;
                        g.setColor(on ? LCD_ON : LCD_OFF);
                        g.fillRect(x0 + c * cellW + x * (DOT + GAP), y0 + r * cellH + y * (DOT + GAP), DOT, DOT);
                    }
                }
            }
        }
    }

    private void paintText(Graphics2D g, Panel.Display d) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int cw = fm.charWidth('M');
        int lh = fm.getHeight() + 4;
        int x0 = (getWidth() - d.getColumns() * cw) / 2;
        int y0 = (getHeight() - d.getRows() * lh) / 2;
        int cursor = d.getCursor();
        for (int r = 0; r < d.getRows(); r++) {
            for (int c = 0; c < d.getColumns(); c++) {
                int x = x0 + c * cw;
                int y = y0 + r * lh;
                int attr = d.getAttributes(r, c);
                boolean reverse = (attr & Panel.Display.REVERSE) != 0 || cursor == r * d.getColumns() + c;
                if (reverse) {
                    g.setColor(VFD_ON);
                    g.fillRect(x, y, cw, lh - 2);
                }
                g.setColor(reverse ? VFD_BACK : VFD_ON);
                g.drawString(String.valueOf(d.getChar(r, c)), x, y + fm.getAscent());
                if ((attr & Panel.Display.UNDERLINE) != 0) {
                    g.fillRect(x, y + lh - 4, cw - 1, 2);
                }
            }
        }
    }
}
