/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.frontend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import static amame.AmameTestSupport.BLOCK;
import static amame.AmameTestSupport.RATE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * The frontend: machines, nvram, sysex.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
class FrontendTest {

    @Test
    void splitSysex() {
        byte[] data = {(byte) 0xf0, 1, 2, (byte) 0xf7, 0x33, (byte) 0xf0, 3, (byte) 0xf7};
        List<byte[]> messages = Frontend.splitSysex(data);
        assertEquals(2, messages.size());
        assertArrayEquals(new byte[] {(byte) 0xf0, 1, 2, (byte) 0xf7}, messages.get(0));
        assertArrayEquals(new byte[] {(byte) 0xf0, 3, (byte) 0xf7}, messages.get(1));
    }

    private static void render(Frontend frontend, double seconds) {
        float[][] out = new float[2][BLOCK];
        for (int i = 0; i < seconds * RATE / BLOCK; i++) frontend.render(out, BLOCK, RATE, null);
    }

    @Test
    @EnabledIf("amame.AmameTestSupport#fb01Roms")
    void swapAndNvram(@TempDir Path dir) throws Exception {
        Frontend frontend = new Frontend();
        frontend.setNvramDirectory(dir);
        frontend.setMachine("fb01");
        render(frontend, 2);
        assertEquals("fb01", frontend.getMachine().getName());
        if (amame.AmameTestSupport.vfxRoms()) {
            frontend.setMachine("vfx");
            assertEquals("vfx", frontend.getMachine().getName());
            // the fb01's memory was kept when it was swapped out
            assertTrue(Files.size(dir.resolve("fb01.nv")) == 0x4000);
        }
        frontend.close();
        assertTrue(Files.exists(dir.resolve(frontend.getMachine().getName() + ".nv")));
    }

    @Test
    @EnabledIf("amame.AmameTestSupport#fb01Roms")
    void sysexCapture() throws Exception {
        Frontend frontend = new Frontend();
        frontend.setMachine("fb01");
        render(frontend, 4);
        // a voice bank dump request
        frontend.midiIn(new byte[] {(byte) 0xf0, 0x43, 0x75, 0x00, 0x20, 0x00, 0x00, (byte) 0xf7});
        render(frontend, 6);
        byte[] sysex = frontend.getSysex();
        assertEquals(6363, sysex.length);
        assertEquals(1, Frontend.splitSysex(sysex).size());
        frontend.clearSysex();
        assertEquals(0, frontend.getSysex().length);
    }
}
