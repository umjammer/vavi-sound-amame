/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


/**
 * A cartridge of the Ensoniq VFX family, after mame's vfxcart.cpp (Christian Brunschen).
 * <p>
 * 32 KiB, a rom or an eeprom such as the StorCart-32. The VFX writes an eeprom a byte at a time,
 * each after the software write protection sequence, and reads the byte back until it is
 * written: here it is written at once. A file named {@code .eeprom} or {@code .sc32} is an
 * eeprom when it can be written, {@code .rom} and {@code .cart} are roms.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class VfxCartridge {

    public static final int SIZE = 32 * 1024;
    private static final int MASK = SIZE - 1;

    private enum State {
        IDLE,
        CMD1,
        CMD2,
        WR
    }

    private State state = State.IDLE;
    private final byte[] storage = new byte[SIZE];
    private boolean writeable;

    /** a new eeprom, empty but for the marks the VFX looks for */
    public static VfxCartridge blank() {
        VfxCartridge cart = new VfxCartridge();
        cart.storage[0x7ffe] = 0x05;
        cart.storage[0x7fff] = 0x01;
        cart.writeable = true;
        return cart;
    }

    public static VfxCartridge of(byte[] data, boolean writeable) {
        if (data.length != SIZE) throw new IllegalArgumentException("Invalid size, must be 32kiB");
        VfxCartridge cart = new VfxCartridge();
        System.arraycopy(data, 0, cart.storage, 0, SIZE);
        cart.writeable = writeable;
        return cart;
    }

    public static VfxCartridge load(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase();
        boolean eeprom = name.endsWith(".eeprom") || name.endsWith(".sc32");
        return of(Files.readAllBytes(path), eeprom && Files.isWritable(path));
    }

    public void save(Path path) throws IOException {
        Files.write(path, storage);
    }

    public boolean isWriteable() {
        return writeable;
    }

    public byte[] getData() {
        return Arrays.copyOf(storage, SIZE);
    }

    public int read(int offset) {
        state = State.IDLE;
        return storage[offset & MASK] & 0xff;
    }

    public void write(int offset, int data) {
        if (!writeable) return;
        // the common eeprom software write protection, done for each byte
        state = switch (state) {
            case IDLE -> offset == 0x5555 && data == 0xaa ? State.CMD1 : State.IDLE;
            case CMD1 -> offset == 0x2aaa && data == 0x55 ? State.CMD2 : State.IDLE;
            case CMD2 -> offset == 0x5555 && data == 0xa0 ? State.WR : State.IDLE;
            case WR -> {
                storage[offset & MASK] = (byte) data;
                yield State.WR;
            }
        };
    }
}
