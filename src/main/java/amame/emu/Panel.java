/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

import java.util.List;


/**
 * The front panel of a machine: its display and its controls.
 * <p>
 * This takes the place of the layout files, input ports and outputs that amame's web gui is
 * built on. A gui shows {@link #getDisplay()} and turns its widgets into calls on the
 * {@link Control}s, it needs to know nothing else of the machine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public interface Panel {

    /** the character display */
    Display getDisplay();

    /** the buttons and sliders, in the order to lay them out */
    List<Control> getControls();

    /**
     * a key of the machine's own keyboard, for the machines that are keyboards
     *
     * @param note the midi note number
     * @param velocity 0 for a key up
     * @return false when the machine has no keyboard or not this key, the note is to be sent as midi then
     */
    default boolean key(int note, int velocity) {
        return false;
    }

    /**
     * a grid of characters. it is written by the emulation thread and read by the gui, a read
     * sees a consistent enough picture to paint, if not always the newest one.
     */
    interface Display {

        int getRows();

        int getColumns();

        /** the character at a cell, unicode, a private character is shown as a block */
        char getChar(int row, int column);

        /** {@link #UNDERLINE} and friends */
        default int getAttributes(int row, int column) {
            return 0;
        }

        /** the cursor cell as row * columns + column, -1 for none */
        default int getCursor() {
            return -1;
        }

        /**
         * the dots of a cell for a dot matrix display, one int per line with the leftmost dot in
         * the highest of {@link #getGlyphWidth()} bits, null to show {@link #getChar} instead
         */
        default int[] getGlyph(int row, int column) {
            return null;
        }

        default int getGlyphWidth() {
            return 5;
        }

        int UNDERLINE = 1;
        int BLINK = 2;
        int REVERSE = 4;
        int BOLD = 8;
    }

    /** a button or a slider */
    interface Control {

        enum Type {
            BUTTON,
            SLIDER
        }

        String getName();

        Type getType();

        /** where to put it, buttons sharing a group are shown together */
        default String getGroup() {
            return "";
        }

        /** a button is 1 while held down and 0 otherwise, a slider goes from 0 to 1 */
        void set(float value);
    }

    /** a control that does something on the emulation thread */
    record SimpleControl(String getName, Type getType, String getGroup, java.util.function.Consumer<Float> action)
            implements Control {

        @Override
        public void set(float value) {
            action.accept(value);
        }
    }
}
