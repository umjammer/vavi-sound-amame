/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

import amame.emu.EmuTimer;
import amame.emu.Machine;
import amame.emu.Panel;


/**
 * The front panel of the Ensoniq VFX family and its 2x40 vfd, after mame's esqpanel.cpp and
 * esqvfd.cpp (R. Belmont, Christian Brunschen).
 * <p>
 * The panel is a microcontroller on a 62500 baud serial line: the main board sends it what to
 * show and which lights to light, the panel sends back the buttons, the keys and the
 * calibration answer. amame drives the same line from its web gui with button codes, here
 * {@link #setButton} does that for the swing panel.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class EsqPanel implements Panel.Display {

    /** 62500 baud, 8N2 */
    private static final double BYTE_TIME = 11 / 62500.0;

    private static final int AT_UNDERLINE = 0x01;
    private static final int AT_BLINK = 0x02;

    private static final int ROWS = 2, COLS = 40;

    private IntConsumer writeTx = data -> {};
    private AnalogWriter writeAnalog = (offset, value) -> {};

    /** an analog value of the panel to the adc of the otis */
    @FunctionalInterface
    public interface AnalogWriter {
        void write(int offset, int value);
    }

    private final ArrayDeque<Integer> xmit = new ArrayDeque<>();
    private final EmuTimer txTimer;
    private final EmuTimer blinkTimer;
    private final Set<Integer> pressedButtons = new HashSet<>();

    private boolean expectCalibrationSecondByte;
    private boolean expectLightSecondByte;
    private final int[] lightStates = new int[16];
    private int blinkPhase;

    // the vfd
    private boolean blinkOn;
    private int cursx, cursy;
    private int savedx, savedy;
    private int curattr;
    private int lastchar;
    private final byte[][] chars = new byte[ROWS][COLS];
    private final byte[][] attrs = new byte[ROWS][COLS];

    public EsqPanel(Machine machine) {
        txTimer = machine.timer(this::traComplete);
        blinkTimer = machine.timer(this::updateBlink);
    }

    public void setWriteTx(IntConsumer writeTx) {
        this.writeTx = writeTx;
    }

    public void setWriteAnalog(AnalogWriter writeAnalog) {
        this.writeAnalog = writeAnalog;
    }

    public void reset() {
        txTimer.enable(false);
        xmit.clear();
        expectCalibrationSecondByte = false;
        expectLightSecondByte = false;
        blinkTimer.adjust(0.25, 0.25);
        // the vfd
        cursx = cursy = 0;
        savedx = savedy = 0;
        curattr = 0;
        lastchar = 0;
        blinkOn = false;
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(chars[r], (byte) 0);
            Arrays.fill(attrs[r], (byte) 0);
        }
    }

    /** a byte from the main board */
    public void rx(int data) {
        writeChar(data);

        if (expectCalibrationSecondByte) {
            if (data == 0xfd) { // calibration request
                xmitChar(0xff); // this is the correct response for "calibration OK"
            }
            expectCalibrationSecondByte = false;
        } else if (expectLightSecondByte) {
            // Light states: 0 = Off, 2 = On, 3 = Blinking
            if ((data & 0x3f) < lightStates.length) lightStates[data & 0x3f] = (data & 0xc0) >> 6;
            expectLightSecondByte = false;
        } else if (data == 0xfb) { // request calibration
            expectCalibrationSecondByte = true;
        } else if (data == 0xff) { // button light state command
            expectLightSecondByte = true;
        }
    }

    private void traComplete() {
        Integer data = xmit.poll();
        if (data != null) writeTx.accept(data);
        if (!xmit.isEmpty()) txTimer.adjust(BYTE_TIME);
    }

    public void xmitChar(int data) {
        xmit.add(data & 0xff);
        if (!txTimer.isEnabled()) txTimer.adjust(BYTE_TIME);
    }

    public void setAnalogValue(int offset, int value) {
        writeAnalog.write(offset, value);
    }

    /** a button of the panel, the codes are listed in the {@code esq5505} driver */
    public void setButton(int button, boolean pressed) {
        boolean current = pressedButtons.contains(button);
        if (pressed == current) return;
        xmitChar((pressed ? 0x80 : 0) | (button & 0xff));
        xmitChar(0x00);
        if (pressed) pressedButtons.add(button); else pressedButtons.remove(button);
    }

    /** a key of the keyboard, 0 to 60 */
    public void keyDown(int key, int velocity) {
        velocity = Math.max(1, Math.min(127, velocity));
        xmitChar(0x80 | (key & 0x3f));
        xmitChar(velocity);
    }

    public void keyUp(int key) {
        xmitChar(key & 0x3f);
        xmitChar(0x40);
    }

    private void updateBlink() {
        blinkPhase = (blinkPhase + 1) & 3;
        blinkOn = (blinkPhase & 2) != 0;
    }

    /** a light, 0 to 15: off, on or blinking */
    public boolean isLightOn(int light) {
        int state = lightStates[light];
        return state == 2 || (state == 3 && (blinkPhase & 1) == 0);
    }

    // the vfd

    private void cursorLeft() {
        cursx--;
        if (cursx < 0) {
            cursx += COLS;
            cursy--;
            if (cursy < 0) cursy += ROWS;
        }
    }

    private void cursorRight() {
        cursx++;
        if (cursx >= COLS) {
            cursx -= COLS;
            cursy++;
            if (cursy >= ROWS) cursy -= ROWS;
        }
    }

    private void clear() {
        cursx = cursy = curattr = 0;
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(chars[r], (byte) 0);
            Arrays.fill(attrs[r], (byte) 0);
        }
    }

    private void writeChar(int data) {
        if (lastchar == 0xfa) {
            // (cursor move) 0xfa 0xYY marks YY characters as underlined at the current cursor location
            for (int j = 0; j < ROWS; j++) {
                for (int i = 0; i < COLS; i++) {
                    if (cursy == j && i >= cursx && i < cursx + data)
                        attrs[j][i] |= AT_UNDERLINE;
                    else
                        attrs[j][i] &= ~AT_UNDERLINE;
                }
            }
            lastchar = 0;
            return;
        } else if (lastchar == 0xff) {
            // 0xff light commands are followed by a byte indicating the light and its requested status
            lastchar = 0;
            return;
        }

        lastchar = data;

        if (data >= 0x80 && data < 0xd0) {
            cursy = ((data & 0x7f) >= 40) ? 1 : 0;
            cursx = (data & 0x7f) % 40;
        } else if (data >= 0xd0) {
            switch (data) {
                case 0xd0 -> curattr |= AT_BLINK; // blink start
                case 0xd1 -> curattr = 0; // blink stop (cancel all attribs on VFX+)
                case 0xd2 -> curattr |= AT_BLINK | AT_UNDERLINE; // blinking underline on VFX
                case 0xd3 -> curattr |= AT_UNDERLINE; // start underline
                case 0xd4 -> cursorRight();
                case 0xd5 -> cursorLeft();
                case 0xd6, 0xfd -> clear();
                case 0xe8 -> curattr = 0; // also cancel attributes
                case 0xf5 -> { // save cursor position
                    savedx = cursx;
                    savedy = cursy;
                }
                case 0xf6 -> { // restore cursor position
                    cursx = savedx;
                    cursy = savedy;
                    curattr = attrs[cursy][cursx];
                }
                default -> {}
            }
        } else if (data >= 0x20 && data <= 0x5f) {
            chars[cursy][cursx] = (byte) (data - ' ');
            attrs[cursy][cursx] = (byte) curattr;
            cursorRight();
        }
    }

    // Panel.Display

    @Override
    public int getRows() {
        return ROWS;
    }

    @Override
    public int getColumns() {
        return COLS;
    }

    @Override
    public char getChar(int row, int column) {
        int attr = attrs[row][column];
        if ((attr & AT_BLINK) != 0 && !blinkOn && (attr & AT_UNDERLINE) == 0) {
            // there is no underline, blink the entire character
            return ' ';
        }
        return (char) (chars[row][column] + ' ');
    }

    @Override
    public int getAttributes(int row, int column) {
        int attr = attrs[row][column];
        if ((attr & AT_UNDERLINE) == 0) return 0;
        // blink the underline off
        if ((attr & AT_BLINK) != 0 && !blinkOn) return 0;
        return UNDERLINE;
    }
}
