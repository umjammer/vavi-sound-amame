/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.util.Arrays;

import amame.emu.EmuTimer;
import amame.emu.Machine;
import amame.emu.Panel;


/**
 * Hitachi HD44780 LCD controller, after mame's hd44780.cpp.
 * <p>
 * Instead of drawing into a screen bitmap it is a {@link Panel.Display}: the gui asks it for
 * the dots of each cell, from the character generator rom when there is one and from the
 * character ram for the eight user characters. amame forwards every data write to its web gui
 * through mame's outputs, which this does away with.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class HD44780 implements Panel.Display {

    private static final int DDRAM = 0, CGRAM = 1;

    private final double clock;
    private final int lines;
    private final int chars;
    /** shows all lines side by side in one row, the FB-01's 2x8 is seen as 1x16 */
    private final boolean oneRow;

    /** the character generator, 16 bytes per character, may be null */
    private byte[] cgrom;

    private final EmuTimer busyTimer;
    private final EmuTimer blinkTimer;

    private boolean busyFlag;
    private final byte[] ddram = new byte[0x80];
    private final byte[] cgram = new byte[0x40];
    private int ac;
    private int dr;
    private int ir;
    private int activeRam;
    private boolean displayOn;
    private boolean cursorOn;
    private boolean blinkOn;
    private boolean shiftOn;
    private int dispShift;
    private int direction;
    private int dataLen;
    private int numLine;
    private int charSize;
    private boolean blink;
    private boolean firstCmd;
    private boolean nibble;
    private int rsState;
    private int rwState;

    /**
     * @param lines the lines of the glass
     * @param chars the characters of each line
     */
    public HD44780(Machine machine, double clock, int lines, int chars, boolean oneRow) {
        this.clock = clock;
        this.lines = lines;
        this.chars = chars;
        this.oneRow = oneRow;
        this.busyTimer = machine.timer(() -> busyFlag = false);
        this.blinkTimer = machine.timer(() -> blink = !blink);
    }

    /** the character generator rom, hd44780_a00.bin */
    public void setCgrom(byte[] cgrom) {
        this.cgrom = cgrom;
    }

    public void reset() {
        Arrays.fill(ddram, (byte) 0x20);
        Arrays.fill(cgram, (byte) 0);
        ac = 0;
        dr = 0;
        ir = 0;
        activeRam = DDRAM;
        displayOn = false;
        cursorOn = false;
        blinkOn = false;
        shiftOn = false;
        direction = 1;
        dataLen = 8;
        numLine = 1;
        charSize = 8;
        dispShift = 0;
        blink = false;
        nibble = false;
        firstCmd = true;
        rsState = 0;
        rwState = 0;
        double period = 102400 / clock;
        blinkTimer.adjust(period, period);
        setBusyFlag(410);
    }

    private void setBusyFlag(int cycles) {
        busyFlag = true;
        busyTimer.adjust(cycles / clock);
    }

    private void correctAc() {
        if (activeRam == DDRAM) {
            int maxAc = (numLine == 1) ? 0x4f : 0x67;
            if (ac > maxAc)
                ac -= maxAc + 1;
            else if (ac < 0)
                ac = maxAc;
            else if (numLine == 2 && ac > 0x27 && ac < 0x40)
                ac = 0x40 + (ac - 0x28);
        } else {
            ac &= 0x3f;
        }
    }

    private void updateAc(int direction) {
        if (activeRam == DDRAM && numLine == 2 && direction == -1 && ac == 0x40)
            ac = 0x27;
        else
            ac += direction;
        correctAc();
    }

    private void shiftDisplay(int direction) {
        dispShift += direction;
        if (dispShift == 0x50)
            dispShift = 0;
        else if (dispShift == -1)
            dispShift = 0x4f;
    }

    private void updateNibble(int rs, int rw) {
        if (rsState != rs || rwState != rw) {
            rsState = rs;
            rwState = rw;
            nibble = false;
        }
        nibble = !nibble;
    }

    public int read(int offset) {
        if (dataLen == 4) updateNibble(offset & 1, 1);
        return (offset & 1) == 0 ? controlRead() : dataRead();
    }

    public void write(int offset, int data) {
        if (dataLen == 4) updateNibble(offset & 1, 0);
        if ((offset & 1) == 0) {
            controlWrite(data & 0xff);
        } else {
            dataWrite(data & 0xff);
        }
    }

    private void controlWrite(int data) {
        if (dataLen == 4) {
            if (nibble) {
                ir = data & 0xf0;
                return;
            } else {
                ir |= (data >> 4) & 0x0f;
            }
        } else {
            ir = data;
        }

        if ((ir & 0x80) != 0) {
            // set DDRAM address
            activeRam = DDRAM;
            ac = ir & 0x7f;
            correctAc();
            setBusyFlag(10);
            return;
        } else if ((ir & 0x40) != 0) {
            // set CGRAM address
            activeRam = CGRAM;
            ac = ir & 0x3f;
            setBusyFlag(10);
            return;
        } else if ((ir & 0x20) != 0) {
            // function set
            int newDataLen = (ir & 0x10) != 0 ? 8 : 4;
            int newCharSize = (ir & 0x04) != 0 ? 10 : 8;
            int newNumLine = ((ir >> 3) & 1) + 1;
            if (!firstCmd && dataLen == newDataLen && (charSize != newCharSize || numLine != newNumLine)) {
                // function set cannot be executed after other instructions unless the interface data length is changed
                return;
            }
            firstCmd = true;
            charSize = newCharSize;
            dataLen = newDataLen;
            numLine = newNumLine;
            correctAc();
            setBusyFlag(10);
            return;
        } else if ((ir & 0x10) != 0) {
            // cursor or display shift
            int direction = (ir & 0x04) != 0 ? +1 : -1;
            if ((ir & 0x08) != 0)
                shiftDisplay(direction);
            else
                updateAc(direction);
            setBusyFlag(10);
        } else if ((ir & 0x08) != 0) {
            // display on/off control
            displayOn = (ir & 0x04) != 0;
            cursorOn = (ir & 0x02) != 0;
            blinkOn = (ir & 0x01) != 0;
            setBusyFlag(10);
        } else if ((ir & 0x04) != 0) {
            // entry mode set
            direction = (ir & 0x02) != 0 ? +1 : -1;
            shiftOn = (ir & 0x01) != 0;
            setBusyFlag(10);
        } else if ((ir & 0x02) != 0) {
            // return home
            ac = 0;
            activeRam = DDRAM;
            direction = 1;
            dispShift = 0;
            setBusyFlag(410);
        } else if ((ir & 0x01) != 0) {
            // clear display
            ac = 0;
            activeRam = DDRAM;
            direction = 1;
            dispShift = 0;
            Arrays.fill(ddram, (byte) 0x20);
            setBusyFlag(410);
            // some machines clear the display first, even though function set must come first
            return;
        }

        firstCmd = false;
    }

    private int controlRead() {
        if (dataLen == 4) {
            if (nibble)
                return (busyFlag ? 0x80 : 0) | (ac & 0x70);
            else
                return (ac << 4) & 0xf0;
        } else {
            return (busyFlag ? 0x80 : 0) | (ac & 0x7f);
        }
    }

    private void dataWrite(int data) {
        if (dataLen == 4) {
            if (nibble) {
                dr = data & 0xf0;
                return;
            } else {
                dr |= (data >> 4) & 0x0f;
            }
        } else {
            dr = data;
        }

        if (activeRam == DDRAM)
            ddram[ac] = (byte) dr;
        else
            cgram[ac] = (byte) dr;

        setBusyFlag(10);
        updateAc(direction);
        if (shiftOn)
            shiftDisplay(direction);
    }

    private int dataRead() {
        int data = (activeRam == DDRAM ? ddram[ac] : cgram[ac]) & 0xff;

        if (dataLen == 4) {
            if (nibble)
                return data & 0xf0;
            else
                data = (data << 4) & 0xf0;
        }

        setBusyFlag(10);
        updateAc(direction);
        return data;
    }

    // Panel.Display

    @Override
    public int getRows() {
        return oneRow ? 1 : lines;
    }

    @Override
    public int getColumns() {
        return oneRow ? lines * chars : chars;
    }

    /** the ddram address shown at a cell */
    private int charPos(int row, int column) {
        int line = oneRow ? column / chars : row;
        int pos = oneRow ? column % chars : column;
        int lineSize = 80 / numLine;
        if (line >= numLine) return -1;
        return line * 0x40 + ((pos + dispShift) % lineSize);
    }

    @Override
    public char getChar(int row, int column) {
        if (!displayOn) return ' ';
        int pos = charPos(row, column);
        if (pos < 0) return ' ';
        return toUnicode(ddram[pos] & 0xff);
    }

    /** the a00 rom is ascii with a yen sign and arrows, and half width katakana up high */
    static char toUnicode(int code) {
        if (code < 0x10) return '█';
        if (code == 0x5c) return '¥';
        if (code == 0x7e) return '→';
        if (code == 0x7f) return '←';
        if (code >= 0x20 && code < 0x7e) return (char) code;
        if (code >= 0xa1 && code <= 0xdf) return (char) (0xff61 + code - 0xa1);
        return ' ';
    }

    @Override
    public int getCursor() {
        if (!displayOn || activeRam != DDRAM || !(cursorOn || blinkOn)) return -1;
        for (int r = 0; r < getRows(); r++) {
            for (int c = 0; c < getColumns(); c++) {
                if (charPos(r, c) == ac) return r * getColumns() + c;
            }
        }
        return -1;
    }

    @Override
    public int[] getGlyph(int row, int column) {
        int code;
        int pos = charPos(row, column);
        if (!displayOn || pos < 0) {
            code = 0x20;
        } else {
            code = ddram[pos] & 0xff;
        }
        int[] glyph = new int[8];
        if (displayOn && pos >= 0 && code < 0x10) {
            int base = charSize == 8 ? (code & 0x07) * 8 : ((code >> 1) & 0x03) * 16;
            for (int y = 0; y < 8; y++) glyph[y] = cgram[(base + y) & 0x3f] & 0x1f;
        } else if (cgrom != null) {
            for (int y = 0; y < 8; y++) glyph[y] = cgrom[code * 16 + y] & 0x1f;
        } else {
            return null;
        }
        if (displayOn && pos == ac && activeRam == DDRAM) {
            if (cursorOn) glyph[7] = 0x1f;
            if (!blink && blinkOn) Arrays.fill(glyph, 0x1f);
        }
        return glyph;
    }
}
