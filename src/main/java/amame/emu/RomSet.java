/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.System.getLogger;


/**
 * Finds the roms of a machine, mame style: {@code <rompath>/<name>.zip} or a directory
 * {@code <rompath>/<name>/}.
 * <p>
 * The rom path is the system property {@code amame.rompath}, several directories separated by
 * the path separator, and {@code roms} when it is not set. The names are those of mame 0.289.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class RomSet {

    private static final Logger logger = getLogger(RomSet.class.getName());

    private final String name;

    public RomSet(String name) {
        this.name = name;
    }

    /** the directories searched, in order */
    public static List<Path> getRomPath() {
        String property = System.getProperty("amame.rompath", "roms");
        List<Path> paths = new ArrayList<>();
        for (String p : property.split(File.pathSeparator)) {
            if (!p.isBlank()) paths.add(Path.of(p));
        }
        return paths;
    }

    /** whether the set can be found at all */
    public boolean exists() {
        for (Path dir : getRomPath()) {
            if (Files.isRegularFile(dir.resolve(name + ".zip")) || Files.isDirectory(dir.resolve(name))) return true;
        }
        return false;
    }

    /**
     * loads one rom of the set
     *
     * @param crc the crc32 mame lists, a mismatch is only logged, 0 to not check
     */
    public byte[] load(String file, int length, int crc) throws IOException {
        for (Path dir : getRomPath()) {
            byte[] data = null;
            Path zip = dir.resolve(name + ".zip");
            if (Files.isRegularFile(zip)) {
                try (ZipFile zf = new ZipFile(zip.toFile())) {
                    ZipEntry entry = zf.getEntry(file);
                    if (entry == null && crc != 0) entry = findByCrc(zf, crc);
                    if (entry != null) {
                        try (InputStream is = zf.getInputStream(entry)) {
                            data = is.readAllBytes();
                        }
                    }
                }
            }
            Path plain = dir.resolve(name).resolve(file);
            if (data == null && Files.isRegularFile(plain)) {
                data = Files.readAllBytes(plain);
            }
            if (data != null) {
                if (data.length != length) {
                    throw new IOException("%s/%s: length %d, expected %d".formatted(name, file, data.length, length));
                }
                if (crc != 0) {
                    CRC32 crc32 = new CRC32();
                    crc32.update(data);
                    if ((int) crc32.getValue() != crc) {
                        logger.log(Level.WARNING, "%s/%s: crc %08x, expected %08x".formatted(name, file, (int) crc32.getValue(), crc));
                    }
                }
                return data;
            }
        }
        throw new FileNotFoundException("rom " + name + "/" + file + " not found in " + getRomPath());
    }

    private static ZipEntry findByCrc(ZipFile zf, int crc) {
        return zf.stream().filter(e -> (int) e.getCrc() == crc).findFirst().orElse(null);
    }

    /** loads interleaved roms, e.g. the even and odd bytes of a 16 bit bus */
    public static void interleave(byte[] dest, int offset, int step, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            dest[offset + i * step] = src[i];
        }
    }
}
