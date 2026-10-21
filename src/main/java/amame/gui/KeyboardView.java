/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import javax.swing.JComponent;


/**
 * The virtual keyboard of amame's frontend, a piano to play with the mouse.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class KeyboardView extends JComponent {

    private static final boolean[] BLACK = {false, true, false, true, false, false, true, false, true, false, true, false};

    private static final int WHITE_WIDTH = 18;
    private static final int HEIGHT = 90;

    /** C2 .. C7, the 61 keys of the VFX */
    private final int lowest = 36;
    private final int highest = 96;

    private final BiConsumer<Integer, Integer> keyListener;
    private int pressed = -1;

    /**
     * @param keyListener called with the note and the velocity, 0 for a key up
     */
    public KeyboardView(BiConsumer<Integer, Integer> keyListener) {
        this.keyListener = keyListener;
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                press(noteAt(e.getX(), e.getY()), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int note = noteAt(e.getX(), e.getY());
                if (note != pressed) {
                    release();
                    press(note, e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                release();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void press(int note, int y) {
        if (note < 0) return;
        pressed = note;
        // further down the key is played harder
        int velocity = Math.max(20, Math.min(127, 40 + y * 87 / HEIGHT));
        keyListener.accept(note, velocity);
        repaint();
    }

    private void release() {
        if (pressed < 0) return;
        keyListener.accept(pressed, 0);
        pressed = -1;
        repaint();
    }

    private int whiteKeys() {
        int n = 0;
        for (int note = lowest; note <= highest; note++) if (!BLACK[note % 12]) n++;
        return n;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(whiteKeys() * WHITE_WIDTH + 1, HEIGHT + 1);
    }

    private Rectangle whiteRect(int index) {
        return new Rectangle(index * WHITE_WIDTH, 0, WHITE_WIDTH, HEIGHT);
    }

    private Rectangle blackRect(int whiteIndex) {
        return new Rectangle(whiteIndex * WHITE_WIDTH - WHITE_WIDTH / 3, 0, WHITE_WIDTH * 2 / 3, HEIGHT * 3 / 5);
    }

    private int noteAt(int x, int y) {
        int white = 0;
        for (int note = lowest; note <= highest; note++) {
            if (BLACK[note % 12]) {
                if (blackRect(white).contains(x, y)) return note;
            } else {
                white++;
            }
        }
        white = 0;
        for (int note = lowest; note <= highest; note++) {
            if (!BLACK[note % 12]) {
                if (whiteRect(white).contains(x, y)) return note;
                white++;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int white = 0;
        for (int note = lowest; note <= highest; note++) {
            if (BLACK[note % 12]) continue;
            Rectangle r = whiteRect(white++);
            g.setColor(note == pressed ? new Color(180, 210, 255) : Color.WHITE);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(Color.GRAY);
            g.drawRect(r.x, r.y, r.width, r.height);
            if (note % 12 == 0) {
                g.drawString("C" + (note / 12 - 1), r.x + 2, r.y + r.height - 4);
            }
        }
        white = 0;
        for (int note = lowest; note <= highest; note++) {
            if (!BLACK[note % 12]) {
                white++;
                continue;
            }
            Rectangle r = blackRect(white);
            g.setColor(note == pressed ? new Color(60, 90, 160) : Color.BLACK);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}
