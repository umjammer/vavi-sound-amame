/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.drivers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import amame.devices.VfxCartridge;
import amame.emu.MediaSlot;
import amame.emu.Panel;

import static amame.AmameTestSupport.run;
import static amame.AmameTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    private static void press(Esq5505 m, String name) {
        Panel.Control button = m.getPanel().getControls().stream().filter(c -> c.getName().equals(name)).findFirst().get();
        button.set(1);
        run(m, 0.1);
        button.set(0);
        run(m, 0.4);
    }

    private static MediaSlot slot(Esq5505 m, String name) {
        return m.getMediaSlots().stream().filter(s -> s.getName().equals(name)).findFirst().get();
    }

    private static String lit(Esq5505 m) {
        return m.getPanel().getControls().stream().filter(Panel.Control::isLit).map(Panel.Control::getName)
                .collect(Collectors.joining(","));
    }

    /** the sounds bank and the sound selected are lit, as on the real panel */
    @Test
    void lights() throws Exception {
        Esq5505 m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        run(m, 3);
        assertEquals("Sounds,0", lit(m));
        press(m, "5");
        assertEquals("Sounds,5", lit(m));
    }

    /** the internal programs copied to a new eeprom cartridge come back as the cartridge bank */
    @Test
    void cartridge(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cart.eeprom");
        Esq5505 m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        run(m, 3);
        slot(m, "Cartridge").create(file);
        press(m, "Storage");
        press(m, "Soft Bottom Right"); // cartridge
        press(m, "Soft Bottom Left"); // programs
        press(m, "Soft Bottom Left"); // int-to-cart
        assertTrue(text(m.getPanel().getDisplay()).contains("FROM INTERNAL TO CARTRIDGE"), text(m.getPanel().getDisplay()));
        press(m, "Soft Top Right"); // yes
        run(m, 120);
        assertTrue(text(m.getPanel().getDisplay()).startsWith("STORAGE"), text(m.getPanel().getDisplay()));
        slot(m, "Cartridge").unload();
        assertEquals(VfxCartridge.SIZE, Files.size(file));

        m = new Esq5505(Esq5505.Model.VFX);
        m.start();
        slot(m, "Cartridge").load(file);
        m.reset();
        run(m, 3);
        press(m, "Cart");
        assertTrue(text(m.getPanel().getDisplay()).startsWith("CRT0 PLAY-1-NOTE TRANS-TINE"), text(m.getPanel().getDisplay()));
    }

    /**
     * a new disk formatted and a program saved on it by the VFX-SD, then loaded back. mame reads
     * the disk this makes, the ensoniq file system is on it.
     */
    @Test
    void disk(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("disk.img");
        Esq5505 m = new Esq5505(Esq5505.Model.VFXSD);
        m.start();
        run(m, 4);
        slot(m, "Floppy").create(file);
        run(m, 1);
        press(m, "Storage");
        press(m, "Soft Bottom Center"); // disk
        press(m, "Soft Top Left"); // format
        press(m, "Soft Top Right"); // yes
        run(m, 100);
        assertTrue(text(m.getPanel().getDisplay()).startsWith("DISK   FORMAT"), text(m.getPanel().getDisplay()));
        press(m, "Soft Bottom Left"); // save
        run(m, 3);
        assertTrue(text(m.getPanel().getDisplay()).contains("FREE=1577"), text(m.getPanel().getDisplay()));
        press(m, "Soft Top Right"); // yes, a program
        run(m, 1);
        press(m, "Soft Top Right"); // yes, its name
        run(m, 5);
        slot(m, "Floppy").unload();

        byte[] image = Files.readAllBytes(file);
        // block 1 is the device id, block 2 the os block with the directory entry of the file
        assertEquals("ID", new String(image, 512 + 38, 2));
        assertEquals("OS", new String(image, 2 * 512 + 28, 2));
        assertEquals("ITS-A-SYNTH", new String(image, 2 * 512 + 32, 11));

        m = new Esq5505(Esq5505.Model.VFXSD);
        m.start();
        run(m, 4);
        slot(m, "Floppy").load(file);
        run(m, 1);
        press(m, "Storage");
        press(m, "Soft Bottom Center"); // disk
        press(m, "Soft Bottom Center"); // load
        run(m, 3);
        assertTrue(text(m.getPanel().getDisplay()).startsWith("LOAD FILE=ITS-A-SYNTH"), text(m.getPanel().getDisplay()));
        // nothing was written, the file is left alone
        slot(m, "Floppy").unload();
        assertArrayEquals(image, Files.readAllBytes(file));
    }
}
