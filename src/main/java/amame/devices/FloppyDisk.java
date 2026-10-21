/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * A 3.5" double density floppy disk as its sectors, the tracks the floppy controller sees.
 * <p>
 * The file is an Ensoniq image, mame's esq16 format: 80 tracks of 2 sides of 10 sectors of 512
 * bytes, numbered from 0, one after another, 819200 bytes. A track may be formatted otherwise
 * or not at all, such a track is not in the file, it is written there as zeros.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class FloppyDisk {

    public static final int TRACKS = 80;
    public static final int HEADS = 2;
    public static final int SECTORS = 10;
    public static final int SECTOR_SIZE = 512;
    public static final int IMAGE_SIZE = TRACKS * HEADS * SECTORS * SECTOR_SIZE;

    /** the bytes of a track at 250 kbps and 300 rpm */
    public static final int TRACK_BYTES = 6250;

    /** a sector, its id field and its data */
    public static final class Sector {
        public final int track, head, sector, size;
        /** where its id field ends, bytes from the index */
        public final int position;
        public final byte[] data;

        public Sector(int track, int head, int sector, int size, int position) {
            this.track = track;
            this.head = head;
            this.sector = sector;
            this.size = size;
            this.position = position;
            this.data = new byte[128 << (size & 3)];
        }
    }

    /** [cylinder][head], an empty list for an unformatted track */
    private final List<Sector>[][] tracks;
    private boolean writeProtected;
    private boolean modified;

    @SuppressWarnings("unchecked")
    public FloppyDisk() {
        tracks = new List[TRACKS + 4][HEADS];
        for (List<Sector>[] cylinder : tracks) {
            for (int h = 0; h < HEADS; h++) cylinder[h] = new ArrayList<>();
        }
    }

    /** the layout of mame's esq_10_desc, scaled down to the track */
    private static int position(int sector) {
        return 146 + sector * 600 + 16;
    }

    /** an image in memory */
    public static FloppyDisk of(byte[] image) {
        if (image.length != IMAGE_SIZE) throw new IllegalArgumentException("an ensoniq disk image is " + IMAGE_SIZE + " bytes, not " + image.length);
        FloppyDisk disk = new FloppyDisk();
        int offset = 0;
        for (int t = 0; t < TRACKS; t++) {
            for (int h = 0; h < HEADS; h++) {
                for (int s = 0; s < SECTORS; s++) {
                    Sector sector = new Sector(t, h, s, 2, position(s));
                    System.arraycopy(image, offset, sector.data, 0, SECTOR_SIZE);
                    disk.tracks[t][h].add(sector);
                    offset += SECTOR_SIZE;
                }
            }
        }
        return disk;
    }

    public static FloppyDisk load(Path path) throws IOException {
        FloppyDisk disk = of(Files.readAllBytes(path));
        disk.writeProtected = !Files.isWritable(path);
        return disk;
    }

    /** the image, the sectors that are not where an ensoniq disk has them are left out */
    public byte[] toImage() {
        byte[] image = new byte[IMAGE_SIZE];
        for (int t = 0; t < TRACKS; t++) {
            for (int h = 0; h < HEADS; h++) {
                for (Sector sector : tracks[t][h]) {
                    if (sector.track == t && sector.sector < SECTORS && sector.data.length == SECTOR_SIZE) {
                        System.arraycopy(sector.data, 0, image, ((t * HEADS + h) * SECTORS + sector.sector) * SECTOR_SIZE, SECTOR_SIZE);
                    }
                }
            }
        }
        return image;
    }

    public void save(Path path) throws IOException {
        Files.write(path, toImage());
        modified = false;
    }

    /** the sectors of a track in the order they pass the head */
    public List<Sector> getTrack(int cylinder, int head) {
        if (cylinder < 0 || cylinder >= tracks.length) return List.of();
        return tracks[cylinder][head & 1];
    }

    /** a track formatted anew, the sectors with their data */
    public void setTrack(int cylinder, int head, List<Sector> sectors) {
        if (cylinder < 0 || cylinder >= tracks.length) return;
        tracks[cylinder][head & 1] = new ArrayList<>(sectors);
        modified = true;
    }

    public boolean isWriteProtected() {
        return writeProtected;
    }

    public void setWriteProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    /** a sector was written or a track formatted */
    public boolean isModified() {
        return modified;
    }

    void setModified() {
        modified = true;
    }
}
