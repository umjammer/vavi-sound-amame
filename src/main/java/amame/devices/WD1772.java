/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import amame.emu.EmuTimer;
import amame.emu.Machine;


/**
 * A Western Digital WD1772 floppy controller with an empty drive.
 * <p>
 * Just enough for the VFX-SD to find its controller and no disk in it: the registers read back,
 * a seek moves the head and reports track 0, a read or a write gives up with record not found,
 * as the chip does when no index pulses come. Disks are not there.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class WD1772 {

    private static final int S_BUSY = 0x01;
    private static final int S_TR00 = 0x04;
    private static final int S_RNF = 0x10;
    private static final int S_SPINUP = 0x20;
    private static final int S_MON = 0x80;

    private int status;
    private int track;
    private int sector;
    private int data;
    private int command;

    private final EmuTimer timer;

    public WD1772(Machine machine) {
        timer = machine.timer(this::complete);
    }

    public void reset() {
        timer.enable(false);
        status = S_TR00;
        track = 0;
        sector = 1;
        data = 0;
    }

    private void complete() {
        status &= ~S_BUSY;
        if ((command & 0x80) == 0) {
            // type I, the head is where it was told to go, spin up is reported unless h disabled it
            status = S_MON | (track == 0 ? S_TR00 : 0) | ((command & 0x08) != 0 ? 0 : S_SPINUP);
        } else {
            // type II and III, no index pulses ever come
            status |= S_RNF;
        }
    }

    public int read(int offset) {
        return switch (offset & 3) {
            case 0 -> status;
            case 1 -> track;
            case 2 -> sector;
            default -> data;
        };
    }

    public void write(int offset, int value) {
        value &= 0xff;
        switch (offset & 3) {
            case 0 -> command(value);
            case 1 -> track = value;
            case 2 -> sector = value;
            default -> data = value;
        }
    }

    private void command(int value) {
        if ((value & 0xf0) == 0xd0) {
            // force interrupt, the motor is on and the status is a type I one
            timer.enable(false);
            status = S_MON | S_SPINUP | (track == 0 ? S_TR00 : 0);
            command = value & 0x7f;
            return;
        }
        if ((status & S_BUSY) != 0) return;
        command = value;
        status = S_BUSY | S_MON | ((value & 0x80) == 0 && track == 0 ? S_TR00 : 0);
        double delay;
        if ((value & 0x80) == 0) {
            // restore, seek, step
            switch (value & 0xf0) {
                case 0x00 -> track = 0;
                case 0x10 -> track = data;
                default -> {}
            }
            delay = 0.006;
        } else {
            // five revolutions at 300 rpm
            delay = 1.0;
        }
        timer.adjust(delay);
    }
}
