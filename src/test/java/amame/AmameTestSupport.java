/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame;

import amame.emu.Machine;
import amame.emu.Panel;
import amame.emu.RomSet;


/**
 * What the machine tests share. The tests that need roms run only when {@code amame.rompath}
 * points at them, set it in {@code local.properties}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class AmameTestSupport {

    private AmameTestSupport() {
    }

    public static final double RATE = 48000;
    public static final int BLOCK = 480;

    public static boolean fb01Roms() {
        return new RomSet("fb01").exists();
    }

    public static boolean vfxRoms() {
        return new RomSet("vfx").exists() && new RomSet("vfxsd").exists();
    }

    /** the display as text, the rows separated by '|' */
    public static String text(Panel.Display d) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < d.getRows(); r++) {
            if (r > 0) sb.append('|');
            for (int c = 0; c < d.getColumns(); c++) sb.append(d.getChar(r, c));
        }
        return sb.toString();
    }

    /** a machine of no cpus, for the chips alone */
    public static final class Bench extends Machine {

        @Override
        public String getName() {
            return "bench";
        }

        @Override
        public String getDescription() {
            return "bench";
        }

        @Override
        public void start() {
        }

        @Override
        public Panel getPanel() {
            return null;
        }

        @Override
        public void midiIn(byte[] message) {
        }

        /** runs on a little */
        public void step(double seconds) {
            run(time() + seconds);
        }
    }

    /** runs a machine for a while, the rms of the left channel comes back */
    public static double run(Machine m, double seconds) {
        float[][] out = new float[2][BLOCK];
        double sum = 0;
        long n = 0;
        int blocks = (int) Math.round(seconds * RATE / BLOCK);
        for (int b = 0; b < blocks; b++) {
            m.render(out, BLOCK, RATE, null);
            for (float v : out[0]) {
                sum += v * v;
                n++;
            }
        }
        return Math.sqrt(sum / Math.max(1, n));
    }
}
