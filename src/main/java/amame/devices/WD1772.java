/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import amame.emu.EmuTimer;
import amame.emu.Machine;

import static java.lang.System.getLogger;


/**
 * A Western Digital WD1772 floppy controller and its drive, after mame's wd_fdc.cpp (O. Galibert)
 * at the level of the sectors.
 * <p>
 * The disk turns at 300 rpm while the controller's motor or the drive's own is on, the index
 * comes once a turn and the sectors pass the head where their id fields are. The data goes a
 * byte at a time through the data register with drq, which the VFX-SD polls: the next byte
 * comes a byte time after the cpu took the last one, so no data is ever lost. Write track
 * takes the format the controller is given, the ids and the data of the sectors in it.
 * <p>
 * The step rates, the spin up of 6 index pulses, the 5 turns that a sector is looked for and
 * the motor that stops 9 turns after the last command are those of the chip, so is the empty
 * drive, where no index ever comes: a command waits for the spin up until a force interrupt.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 * @version 0.01 2026-10-21 nsano the disks <br>
 */
public final class WD1772 {

    private static final Logger logger = getLogger(WD1772.class.getName());

    private static final int S_BUSY = 0x01;
    private static final int S_DRQ = 0x02, S_IP = 0x02;
    private static final int S_TR00 = 0x04;
    private static final int S_RNF = 0x10;
    private static final int S_SPIN = 0x20;
    private static final int S_WP = 0x40;
    private static final int S_MON = 0x80;

    private static final double REVOLUTION = 0.2;
    private static final double BYTE_TIME = REVOLUTION / FloppyDisk.TRACK_BYTES;
    private static final double INDEX_PULSE = 0.002;
    /** 6, 12, 2, 3 ms */
    private static final double[] STEP_TIMES = {0.006, 0.012, 0.002, 0.003};
    private static final double SETTLE_TIME = 0.030;
    private static final double VERIFY_TIME = 0.015;

    private enum State {
        IDLE,
        SPINUP,
        STEP,
        VERIFY,
        SETTLE,
        SEARCH,
        READ_DATA,
        WRITE_DATA,
        WRITE_DATA_END,
        READ_ADDRESS,
        WRITE_TRACK_START,
        WRITE_TRACK,
        READ_TRACK,
    }

    private final Machine machine;
    private final EmuTimer timer;
    private final EmuTimer motorTimer;

    private int status;
    private boolean statusType1 = true;
    private int track;
    private int sector;
    private int data;
    private int command;
    private boolean drq;

    private State state = State.IDLE;
    /** where the state goes on after the spin up */
    private State afterSpinup;
    private int counter;
    private boolean stepOut;
    /** when the search began, it gives up after 5 turns */
    private double searchStart;

    // the transfer
    private byte[] buffer;
    private int bufferIndex;
    /** when the next byte passes the head, a byte the cpu is late for does not slow the disk */
    private double nextByteAt;
    private FloppyDisk.Sector current;
    private final List<Integer> trackBytes = new ArrayList<>();
    /** the bytes written to the track, a crc is two */
    private int trackLength;

    // the drive
    private FloppyDisk disk;
    private int cylinder;
    private int side;
    private boolean driveMotor;
    private boolean motor;

    public WD1772(Machine machine) {
        this.machine = machine;
        timer = machine.timer(this::next);
        motorTimer = machine.timer(this::motorOff);
    }

    public void reset() {
        timer.enable(false);
        motorTimer.enable(false);
        state = State.IDLE;
        status = 0;
        statusType1 = true;
        command = 0x03;
        track = 0;
        sector = 1;
        data = 0;
        drq = false;
        motor = false;
        // a restore at reset
        cylinder = 0;
    }

    // the drive

    /** puts a disk in, null for none */
    public void setDisk(FloppyDisk disk) {
        this.disk = disk;
    }

    public FloppyDisk getDisk() {
        return disk;
    }

    /** the side select of the drive */
    public void setSide(int side) {
        this.side = side & 1;
    }

    /** the motor on of the drive, the controller's own goes along with it */
    public void setDriveMotor(boolean on) {
        driveMotor = on;
    }

    private boolean spinning() {
        return disk != null && (motor || driveMotor);
    }

    /** where the disk is in its turn, 0 at the index */
    private double angle() {
        double t = machine.time();
        return t / REVOLUTION - Math.floor(t / REVOLUTION);
    }

    private boolean index() {
        return spinning() && angle() * REVOLUTION < INDEX_PULSE;
    }

    /** the time until the next index */
    private double untilIndex() {
        return (1 - angle()) * REVOLUTION;
    }

    /** the time until a byte of the track passes the head */
    private double until(int position) {
        double target = (double) position / FloppyDisk.TRACK_BYTES;
        double d = target - angle();
        if (d <= 1e-9) d += 1;
        return d * REVOLUTION;
    }

    // the registers

    public int read(int offset) {
        return switch (offset & 3) {
            case 0 -> readStatus();
            case 1 -> track;
            case 2 -> sector;
            default -> readData();
        };
    }

    public void write(int offset, int value) {
        value &= 0xff;
        switch (offset & 3) {
            case 0 -> command(value);
            case 1 -> track = value;
            case 2 -> sector = value;
            default -> writeData(value);
        }
    }

    private int readStatus() {
        int s = status;
        if (statusType1) {
            s &= ~(S_IP | S_TR00 | S_WP);
            if (index()) s |= S_IP;
            if (cylinder == 0) s |= S_TR00;
            if (disk != null && disk.isWriteProtected()) s |= S_WP;
        } else {
            s = drq ? s | S_DRQ : s & ~S_DRQ;
        }
        return s;
    }

    private int readData() {
        if (drq && (state == State.READ_DATA || state == State.READ_ADDRESS || state == State.READ_TRACK)) {
            drq = false;
            nextByte();
        }
        return data;
    }

    private void writeData(int value) {
        data = value;
        if (!drq) return;
        switch (state) {
            case WRITE_DATA -> {
                drq = false;
                buffer[bufferIndex++] = (byte) value;
                nextByte();
            }
            case WRITE_TRACK_START -> {
                // the track is written from the next index on
                drq = false;
                state = State.WRITE_TRACK;
                trackBytes.clear();
                trackLength = 0;
                addTrackByte(value);
                if (spinning()) {
                    nextByteAt = machine.time() + untilIndex();
                    nextByte();
                }
            }
            case WRITE_TRACK -> {
                drq = false;
                addTrackByte(value);
                nextByte();
            }
            default -> {}
        }
    }

    /** the byte after this one, a byte time after this one was due or now when the cpu was late */
    private void nextByte() {
        nextByteAt += BYTE_TIME;
        timer.adjust(Math.max(0, nextByteAt - machine.time()));
    }

    private void addTrackByte(int value) {
        trackBytes.add(value);
        trackLength += value == 0xf7 ? 2 : 1;
    }

    // the commands

    private void command(int value) {
        if ((value & 0xf0) == 0xd0) {
            forceInterrupt(value);
            return;
        }
        if ((status & S_BUSY) != 0) return;
        command = value;
        motorTimer.enable(false);
        drq = false;
        if ((value & 0x80) == 0) {
            statusType1 = true;
            status = (status & (S_MON | S_SPIN)) | S_BUSY;
        } else {
            statusType1 = false;
            status = (status & S_MON) | S_BUSY;
            if ((value & 0xe0) == 0xa0 || (value & 0xf0) == 0xf0) {
                // write sector and write track check the tab first
                if (disk != null && disk.isWriteProtected()) {
                    status |= S_WP;
                    end();
                    return;
                }
            }
        }
        afterSpinup = (value & 0x80) == 0 ? State.STEP : (value & 0x04) != 0 ? State.SETTLE : State.SEARCH;
        logger.log(Level.TRACE, "command %02x track %d sector %d data %d cylinder %d side %d".formatted(value, track, sector, data, cylinder, side));
        counter = 0;
        if ((status & S_MON) == 0 && (value & 0x08) == 0) {
            // spin up, 6 index pulses
            status |= S_MON;
            motor = true;
            state = State.SPINUP;
            counter = 0;
            if (spinning()) timer.adjust(untilIndex());
            return;
        }
        status |= S_MON;
        motor = true;
        if ((value & 0x80) == 0 && (value & 0x08) == 0) status |= S_SPIN;
        start(afterSpinup);
    }

    private void forceInterrupt(int value) {
        timer.enable(false);
        if ((status & S_BUSY) != 0) {
            status &= ~S_BUSY;
        } else {
            statusType1 = true;
        }
        drq = false;
        state = State.IDLE;
        // the WD1772 spins up on a force interrupt
        status |= S_MON | S_SPIN;
        motor = true;
        motorTimer.adjust(9 * REVOLUTION);
    }

    private void start(State next) {
        state = next;
        switch (next) {
            case STEP -> step();
            case SETTLE -> timer.adjust(SETTLE_TIME);
            case SEARCH -> search();
            default -> throw new IllegalStateException(next.name());
        }
    }

    /** the timer, the state goes on */
    private void next() {
        switch (state) {
            case SPINUP -> {
                if (++counter < 6) {
                    timer.adjust(REVOLUTION);
                } else {
                    if (statusType1 && (command & 0x08) == 0) status |= S_SPIN;
                    start(afterSpinup);
                }
            }
            case STEP -> step();
            case VERIFY -> verify();
            case SETTLE -> {
                state = State.SEARCH;
                search();
            }
            case SEARCH -> search();
            case READ_DATA -> readByte();
            case WRITE_DATA -> writeByte();
            case WRITE_DATA_END -> end();
            case READ_ADDRESS -> readAddressByte();
            case WRITE_TRACK -> writeTrackByte();
            case READ_TRACK -> readTrackByte();
            default -> {}
        }
    }

    // type I

    private void step() {
        int type = command & 0xf0;
        if (counter == 0) {
            switch (type) {
                case 0x00 -> { // restore
                    track = 0xff;
                    data = 0;
                }
                case 0x20, 0x30 -> {} // step, the last direction
                case 0x40, 0x50 -> stepOut = false;
                case 0x60, 0x70 -> stepOut = true;
                default -> {}
            }
        }
        boolean done;
        if (type < 0x20) {
            // restore and seek
            if (type == 0x00 && cylinder == 0) {
                track = 0;
                done = true;
            } else if (track == data) {
                done = true;
            } else {
                stepOut = data < track;
                done = false;
            }
            if (!done && counter >= 255) {
                done = true;
                if (type == 0x00) status |= S_RNF;
            }
            if (!done) {
                track = (track + (stepOut ? -1 : 1)) & 0xff;
                move();
                return;
            }
        } else {
            if (counter == 0) {
                if ((command & 0x10) != 0) track = (track + (stepOut ? -1 : 1)) & 0xff;
                move();
                return;
            }
            done = true;
        }
        if ((command & 0x04) != 0) {
            state = State.VERIFY;
            timer.adjust(VERIFY_TIME);
        } else {
            end();
        }
    }

    private void move() {
        if (stepOut) {
            if (cylinder > 0) cylinder--;
        } else {
            if (cylinder < FloppyDisk.TRACKS + 3) cylinder++;
        }
        counter++;
        state = State.STEP;
        timer.adjust(STEP_TIMES[command & 3]);
    }

    /** a type I verify, an id of the track register on the track */
    private void verify() {
        if (spinning()) {
            for (FloppyDisk.Sector s : disk.getTrack(cylinder, side)) {
                if (s.track == track) {
                    end();
                    return;
                }
            }
        }
        // not in 5 turns
        if (counter >= 0) {
            counter = -1;
            timer.adjust(5 * REVOLUTION);
            return;
        }
        status |= S_RNF;
        end();
    }

    // type II and III

    private void search() {
        int type = command & 0xf0;
        if (type == 0xf0) {
            // write track: drq for the first byte, the track starts at the index after it
            state = State.WRITE_TRACK_START;
            drq = true;
            return;
        }
        if (type == 0xe0) {
            if (!spinning()) return;
            buffer = trackImage();
            bufferIndex = 0;
            state = State.READ_TRACK;
            nextByteAt = machine.time() + untilIndex();
            timer.adjust(untilIndex());
            return;
        }
        if (state != State.SEARCH || counter == 0) {
            searchStart = machine.time();
            counter = 1;
        }
        state = State.SEARCH;
        if (!spinning()) {
            // no index, the controller waits forever
            return;
        }
        // the next matching id field that passes the head within 5 turns
        FloppyDisk.Sector found = null;
        double best = Double.MAX_VALUE;
        for (FloppyDisk.Sector s : disk.getTrack(cylinder, side)) {
            if (type == 0xc0 || (s.track == track && s.sector == sector)) {
                double t = until(s.position);
                if (t < best) {
                    best = t;
                    found = s;
                }
            }
        }
        double left = searchStart + 5 * REVOLUTION - machine.time();
        if (found == null || best > left) {
            // the end comes with the timer
            status |= S_RNF;
            state = State.WRITE_DATA_END;
            timer.adjust(Math.max(0, left));
            return;
        }
        current = found;
        bufferIndex = 0;
        switch (type) {
            case 0xc0 -> {
                int crc = crc(new int[] {0xa1, 0xa1, 0xa1, 0xfe, found.track, found.head, found.sector, found.size});
                buffer = new byte[] {(byte) found.track, (byte) found.head, (byte) found.sector, (byte) found.size,
                        (byte) (crc >> 8), (byte) crc};
                state = State.READ_ADDRESS;
                nextByteAt = machine.time() + best;
                timer.adjust(best);
            }
            case 0x80, 0x90 -> {
                buffer = found.data;
                state = State.READ_DATA;
                // the gap and the data mark
                nextByteAt = machine.time() + best + 43 * BYTE_TIME;
                timer.adjust(best + 43 * BYTE_TIME);
            }
            default -> {
                buffer = found.data;
                state = State.WRITE_DATA;
                // drq after 2 bytes of the gap, the data from the gap's end on
                nextByteAt = machine.time() + best + 43 * BYTE_TIME;
                timer.adjust(best + 2 * BYTE_TIME);
            }
        }
    }

    private void readByte() {
        if (bufferIndex < buffer.length) {
            data = buffer[bufferIndex++] & 0xff;
            drq = true;
            return;
        }
        // the crc, then the next sector for a multiple one
        if ((command & 0x10) != 0) {
            sector = (sector + 1) & 0xff;
            counter = 0;
            state = State.SEARCH;
            search();
        } else {
            end();
        }
    }

    private void writeByte() {
        if (bufferIndex < buffer.length) {
            drq = true;
            return;
        }
        disk.setModified();
        if ((command & 0x10) != 0) {
            sector = (sector + 1) & 0xff;
            counter = 0;
            state = State.SEARCH;
            search();
        } else {
            // the crc and the gap byte
            state = State.WRITE_DATA_END;
            timer.adjust(3 * BYTE_TIME);
        }
    }

    private void readAddressByte() {
        if (bufferIndex < buffer.length) {
            data = buffer[bufferIndex++] & 0xff;
            drq = true;
            return;
        }
        // the track of the id goes to the sector register
        sector = buffer[0] & 0xff;
        end();
    }

    private void writeTrackByte() {
        if (trackLength >= FloppyDisk.TRACK_BYTES) {
            disk.setTrack(cylinder, side, format(trackBytes));
            end();
            return;
        }
        drq = true;
    }

    private void readTrackByte() {
        if (bufferIndex < buffer.length) {
            data = buffer[bufferIndex++] & 0xff;
            drq = true;
            return;
        }
        end();
    }

    private void end() {
        timer.enable(false);
        state = State.IDLE;
        status &= ~S_BUSY;
        drq = false;
        // the motor stops 9 turns after the last command
        motorTimer.adjust(9 * REVOLUTION);
    }

    private void motorOff() {
        if (state != State.IDLE) return;
        status &= ~S_MON;
        motor = false;
    }

    // the format

    /**
     * the sectors of a track as write track has written it: f5 is a sync a1, f6 a c2, f7 the
     * crc, an id field after a1 a1 a1 fe and its data after the next a1 a1 a1 fb
     */
    static List<FloppyDisk.Sector> format(List<Integer> bytes) {
        List<FloppyDisk.Sector> sectors = new ArrayList<>();
        int position = 0;
        FloppyDisk.Sector pending = null;
        for (int i = 0; i < bytes.size(); i++) {
            int b = bytes.get(i);
            position += b == 0xf7 ? 2 : 1;
            if (b != 0xf5 || i + 1 >= bytes.size() || bytes.get(i + 1) == 0xf5) continue;
            // the last of the syncs, the address mark follows
            int mark = bytes.get(i + 1);
            if (mark == 0xfe && i + 5 < bytes.size()) {
                pending = new FloppyDisk.Sector(bytes.get(i + 2), bytes.get(i + 3), bytes.get(i + 4), bytes.get(i + 5),
                        position + 7);
                i += 5;
                position += 5;
            } else if ((mark == 0xfb || mark == 0xf8) && pending != null) {
                int n = Math.min(pending.data.length, bytes.size() - (i + 2));
                for (int k = 0; k < n; k++) pending.data[k] = (byte) (int) bytes.get(i + 2 + k);
                sectors.add(pending);
                pending = null;
                i += 1 + n;
                position += 1 + n;
            }
        }
        return sectors;
    }

    /** the raw bytes of the track, gaps, marks and crcs, for read track */
    private byte[] trackImage() {
        byte[] image = new byte[FloppyDisk.TRACK_BYTES];
        java.util.Arrays.fill(image, (byte) 0x4e);
        for (FloppyDisk.Sector s : disk.getTrack(cylinder, side)) {
            int p = s.position - 10;
            int[] id = {0xa1, 0xa1, 0xa1, 0xfe, s.track, s.head, s.sector, s.size};
            int crc = crc(id);
            for (int v : id) put(image, p++, v);
            put(image, p++, crc >> 8);
            put(image, p++, crc);
            p += 22;
            int[] mark = {0xa1, 0xa1, 0xa1, 0xfb};
            int[] all = new int[4 + s.data.length];
            System.arraycopy(mark, 0, all, 0, 4);
            for (int k = 0; k < s.data.length; k++) all[4 + k] = s.data[k] & 0xff;
            crc = crc(all);
            for (int v : all) put(image, p++, v);
            put(image, p++, crc >> 8);
            put(image, p, crc);
        }
        return image;
    }

    private static void put(byte[] image, int p, int v) {
        if (p >= 0 && p < image.length) image[p] = (byte) v;
    }

    /** crc-ccitt from ffff */
    static int crc(int[] bytes) {
        int crc = 0xffff;
        for (int b : bytes) {
            crc ^= (b & 0xff) << 8;
            for (int i = 0; i < 8; i++) crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
        }
        return crc & 0xffff;
    }
}
