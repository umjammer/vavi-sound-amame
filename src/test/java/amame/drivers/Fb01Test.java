/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.drivers;

import java.util.ArrayList;
import java.util.List;

import amame.emu.Panel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static amame.AmameTestSupport.run;
import static amame.AmameTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Yamaha FB-01.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
@EnabledIf("amame.AmameTestSupport#fb01Roms")
class Fb01Test {

    @Test
    void boot() throws Exception {
        Fb01 m = new Fb01();
        m.start();
        run(m, 1.5);
        assertEquals("FB-01 ready  !!!", text(m.getPanel().getDisplay()));
    }

    @Test
    void note() throws Exception {
        Fb01 m = new Fb01();
        m.start();
        // the firmware takes no midi until it has settled, at about 3.4 seconds
        assertEquals(0, run(m, 4), 1e-9);
        m.midiIn(new byte[] {(byte) 0x90, 69, 100});
        double rms = run(m, 0.5);
        assertTrue(rms > 0.01, "rms " + rms);
        m.midiIn(new byte[] {(byte) 0x80, 69, 0});
        run(m, 2);
        assertTrue(run(m, 0.5) < rms / 10);
    }

    @Test
    void button() throws Exception {
        Fb01 m = new Fb01();
        m.start();
        run(m, 4);
        String before = text(m.getPanel().getDisplay());
        Panel.Control select = m.getPanel().getControls().stream().filter(c -> c.getName().equals("Voice Select")).findFirst().get();
        select.set(1);
        run(m, 0.2);
        select.set(0);
        run(m, 0.2);
        assertNotEquals(before, text(m.getPanel().getDisplay()));
    }

    @Test
    void sysexOut() throws Exception {
        Fb01 m = new Fb01();
        List<Integer> out = new ArrayList<>();
        m.setMidiOut(out::add);
        m.start();
        run(m, 4);
        // a voice bank 0 dump request, system channel 1
        m.midiIn(new byte[] {(byte) 0xf0, 0x43, 0x75, 0x00, 0x20, 0x00, 0x00, (byte) 0xf7});
        // the firmware paces the dump, it takes some five seconds
        run(m, 6);
        assertEquals(6363, out.size());
        assertEquals(0xf0, out.get(0));
        assertEquals(0xf7, out.get(out.size() - 1));
    }
}
