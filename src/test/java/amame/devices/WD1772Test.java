/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import amame.AmameTestSupport.Bench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * The floppy controller driven as the VFX-SD drives it, polling drq.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
class WD1772Test {

    private static final int BUSY = 0x01, DRQ = 0x02, RNF = 0x10, WP = 0x40;

    private Bench m;
    private WD1772 fdc;

    @BeforeEach
    void setup() {
        m = new Bench();
        fdc = new WD1772(m);
        fdc.reset();
    }

    /** waits for drq or the end of the command */
    private int waitDrq() {
        for (int i = 0; i < 2_000_000; i++) {
            int status = fdc.read(0);
            if ((status & DRQ) != 0 || (status & BUSY) == 0) return status;
            m.step(4e-6);
        }
        throw new AssertionError("timeout");
    }

    private int waitEnd() {
        for (int i = 0; i < 2_000_000; i++) {
            int status = fdc.read(0);
            if ((status & BUSY) == 0) return status;
            m.step(4e-6);
        }
        throw new AssertionError("timeout");
    }

    private void seek(int track) {
        fdc.write(3, track);
        fdc.write(0, 0x10); // seek, spin up
        assertEquals(0, waitEnd() & RNF);
        assertEquals(track, fdc.read(1));
    }

    /** a track as the VFX-SD formats it, sectors 0 to 9 of 512 bytes filled with a pattern */
    private void writeTrack(int track, int head) {
        List<Integer> bytes = new ArrayList<>();
        for (int i = 0; i < 60; i++) bytes.add(0x4e);
        for (int s = 0; s < 10; s++) {
            for (int i = 0; i < 12; i++) bytes.add(0x00);
            for (int i = 0; i < 3; i++) bytes.add(0xf5);
            bytes.addAll(List.of(0xfe, track, head, s, 2, 0xf7));
            for (int i = 0; i < 22; i++) bytes.add(0x4e);
            for (int i = 0; i < 12; i++) bytes.add(0x00);
            for (int i = 0; i < 3; i++) bytes.add(0xf5);
            bytes.add(0xfb);
            for (int i = 0; i < 512; i++) bytes.add(i % 2 == 0 ? 0x6d : 0xb6);
            bytes.add(0xf7);
            for (int i = 0; i < 40; i++) bytes.add(0x4e);
        }
        fdc.write(0, 0xf8); // write track, no spin up
        int i = 0;
        while (true) {
            int status = waitDrq();
            if ((status & BUSY) == 0) break;
            fdc.write(3, i < bytes.size() ? bytes.get(i) : 0x4e);
            i++;
        }
    }

    private byte[] readSector(int sector) {
        fdc.write(2, sector);
        fdc.write(0, 0x88);
        byte[] data = new byte[512];
        int n = 0;
        while (true) {
            int status = waitDrq();
            if ((status & BUSY) == 0) {
                assertEquals(0, status & RNF, "sector " + sector + " not found");
                break;
            }
            data[n++] = (byte) fdc.read(3);
        }
        assertEquals(512, n);
        return data;
    }

    private void writeSector(int sector, byte[] data) {
        fdc.write(2, sector);
        fdc.write(0, 0xa8);
        int n = 0;
        while (true) {
            int status = waitDrq();
            if ((status & BUSY) == 0) break;
            fdc.write(3, data[n++]);
        }
        assertEquals(512, n);
    }

    @Test
    void formatWriteRead(@TempDir Path dir) throws Exception {
        FloppyDisk disk = new FloppyDisk();
        fdc.setDisk(disk);
        seek(3);
        fdc.setSide(1);
        writeTrack(3, 1);
        List<FloppyDisk.Sector> sectors = disk.getTrack(3, 1);
        assertEquals(10, sectors.size());
        assertEquals(9, sectors.get(9).sector);
        assertEquals((byte) 0x6d, readSector(4)[0]);

        byte[] data = new byte[512];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 7);
        writeSector(7, data);
        assertArrayEquals(data, readSector(7));
        assertTrue(disk.isModified());

        // the image has the sector at its place
        Path image = dir.resolve("disk.img");
        disk.save(image);
        FloppyDisk loaded = FloppyDisk.load(image);
        assertArrayEquals(data, loaded.getTrack(3, 1).get(7).data);
        byte[] bytes = java.nio.file.Files.readAllBytes(image);
        assertEquals(FloppyDisk.IMAGE_SIZE, bytes.length);
        assertEquals((byte) 7, bytes[((3 * 2 + 1) * 10 + 7) * 512 + 1]);
    }

    @Test
    void readAddress() {
        fdc.setDisk(FloppyDisk.of(new byte[FloppyDisk.IMAGE_SIZE]));
        seek(5);
        fdc.write(0, 0xc0);
        int[] id = new int[6];
        int n = 0;
        while (true) {
            int status = waitDrq();
            if ((status & BUSY) == 0) break;
            id[n++] = fdc.read(3);
        }
        assertEquals(6, n);
        assertEquals(5, id[0]);
        assertEquals(2, id[3]);
        int crc = WD1772.crc(new int[] {0xa1, 0xa1, 0xa1, 0xfe, id[0], id[1], id[2], id[3]});
        assertEquals(crc, (id[4] << 8) | id[5]);
        // the track of the id goes to the sector register
        assertEquals(5, fdc.read(2));
    }

    @Test
    void sectorNotFound() {
        fdc.setDisk(FloppyDisk.of(new byte[FloppyDisk.IMAGE_SIZE]));
        seek(1);
        fdc.write(2, 10); // there are 0 to 9
        fdc.write(0, 0x88);
        double start = m.time();
        int status = waitEnd();
        assertEquals(RNF, status & RNF);
        // five turns
        assertEquals(1.0, m.time() - start, 0.21);
    }

    @Test
    void writeProtected() {
        FloppyDisk disk = FloppyDisk.of(new byte[FloppyDisk.IMAGE_SIZE]);
        disk.setWriteProtected(true);
        fdc.setDisk(disk);
        fdc.write(0, 0xa8);
        assertEquals(WP, waitEnd() & WP);
        assertTrue(!disk.isModified());
    }

    @Test
    void noDiskNoIndex() {
        fdc.write(0, 0x00); // restore with spin up
        m.step(3);
        // no index pulses come, the spin up never ends
        assertEquals(BUSY, fdc.read(0) & BUSY);
        fdc.write(0, 0xd0);
        assertEquals(0, fdc.read(0) & BUSY);
    }
}
