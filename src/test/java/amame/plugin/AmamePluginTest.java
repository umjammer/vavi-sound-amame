/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.plugin;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.urish.jnavst.VstHost;
import org.urish.jnavst.VstPlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * amame as a VST plug-in, hosted by jnavst in this process: the AEffect and every call go
 * through jna the way they would for a native host.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
@EnabledIf("amame.AmameTestSupport#fb01Roms")
class AmamePluginTest {

    @Test
    void host() {
        List<byte[]> midiOut = new ArrayList<>();
        VstPlugin plugin = new VstPlugin(new AmamePlugin(), new VstHost() {
            @Override
            public float getSampleRate() {
                return 48000;
            }

            @Override
            public void midiOut(VstPlugin plugin, byte[] message, int deltaFrames) {
                midiOut.add(message);
            }
        });
        try {
            plugin.open();
            assertEquals("AMAME", plugin.getName());
            assertTrue(plugin.isSynth());
            assertTrue(plugin.usesChunks());
            assertEquals(2, plugin.getNumOutputs());
            assertEquals(1, plugin.canDo("receiveVstMidiEvent"));
            plugin.setSampleRate(48000);
            plugin.setBlockSize(512);
            plugin.resume();

            float[][] out = new float[2][512];
            double peak = 0;
            for (int blk = 0; blk < 48000 * 6 / 512; blk++) {
                if (blk == 48000 * 4 / 512) plugin.getEventQueue().add(new byte[] {(byte) 0x90, 60, 100}, 100);
                plugin.processReplacing(new float[0][], out, 512);
                if (blk > 48000 * 4 / 512) for (float v : out[0]) peak = Math.max(peak, Math.abs(v));
            }
            assertTrue(peak > 0.01, "peak " + peak);

            byte[] chunk = plugin.getChunk(false);
            assertNotNull(chunk);
            assertTrue(chunk.length > 0x4000);
            plugin.setChunk(chunk, false);
            assertEquals("fb01", plugin.getProgramName());
            assertNotNull(plugin.getEditorComponent());
        } finally {
            plugin.close();
        }
    }
}
