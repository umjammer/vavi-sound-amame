/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.drivers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static amame.AmameTestSupport.run;
import static amame.AmameTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Ensoniq VFX, against what mame 0.289 does.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
@EnabledIf("amame.AmameTestSupport#vfxRoms")
class Esq5505Test {

    @Test
    void boot() throws Exception {
        Esq5505 m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        run(m, 1);
        assertEquals("              ENSONIQ  VFX              |   DYNAMIC COMPONENT PROCESSING SYSTEM  ",
                text(m.getPanel().getDisplay()));
        run(m, 2);
        // the first preset of the internal bank, as mame shows it
        assertTrue(text(m.getPanel().getDisplay()).startsWith("INT0 PLAY-1-NOTE TRANS-TINE"), text(m.getPanel().getDisplay()));
    }

    /**
     * middle c from the panel's keyboard, velocity 100, at 3 seconds of a cold start: mame's
     * output has an rms of 2087 (of 32767) between 3.6 and 3.7 seconds
     */
    @Test
    void level() throws Exception {
        Esq5505 m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        run(m, 3);
        m.getPanel().key(60, 100);
        run(m, 0.6);
        double rms = run(m, 0.1) * 32767;
        assertEquals(2087, rms, 2087 * 0.1, "rms " + rms);
    }

    @Test
    void bootVfxsd() throws Exception {
        Esq5505 m = new Esq5505(Esq5505.Model.VFXSD);
        m.start();
        run(m, 4);
        // the first preset of the internal bank of the VFX-SD, as mame shows it
        assertTrue(text(m.getPanel().getDisplay()).startsWith("INT0 ITS-A-SYNTH  ZIRCONIUM"), text(m.getPanel().getDisplay()));
    }

    @Test
    void midi() throws Exception {
        Esq5505 m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        run(m, 3);
        m.midiIn(new byte[] {(byte) 0x90, 60, 100});
        assertTrue(run(m, 0.5) > 0.01);
    }

    @Test
    void button() throws Exception {
        Esq5505 m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        run(m, 3);
        var master = m.getPanel().getControls().stream().filter(c -> c.getName().equals("Master")).findFirst().get();
        master.set(1);
        run(m, 0.1);
        master.set(0);
        run(m, 0.3);
        assertTrue(text(m.getPanel().getDisplay()).contains("TUNE"), text(m.getPanel().getDisplay()));
    }
}
