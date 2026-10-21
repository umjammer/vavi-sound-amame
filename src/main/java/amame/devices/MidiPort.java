/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.devices;

import java.util.ArrayDeque;
import java.util.function.IntConsumer;

import amame.emu.EmuTimer;
import amame.emu.Machine;


/**
 * A midi in socket fed from the host, amame's virtual midi.
 * <p>
 * mame shifts midi in bit by bit at 31250 baud. This hands the uart whole characters at the
 * pace the wire would bring them, one every ten bit times, which is all a uart can tell apart.
 * Bytes sent faster than that queue up as they would in a midi interface.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class MidiPort {

    /** start bit, eight data bits and a stop bit at 31250 baud */
    public static final double BYTE_TIME = 10 / 31250.0;

    private final ArrayDeque<Integer> queue = new ArrayDeque<>();
    private final EmuTimer timer;
    private final IntConsumer receiver;

    /** the uart that takes the characters */
    public MidiPort(Machine machine, IntConsumer receiver) {
        this.receiver = receiver;
        this.timer = machine.timer(this::deliver);
    }

    /** a midi message, sysex included */
    public void write(byte[] message) {
        for (byte b : message) {
            queue.add(b & 0xff);
        }
        if (!timer.isEnabled()) timer.adjust(BYTE_TIME);
    }

    private void deliver() {
        Integer data = queue.poll();
        if (data != null) receiver.accept(data);
        if (!queue.isEmpty()) timer.adjust(BYTE_TIME);
    }

    public void clear() {
        queue.clear();
        timer.enable(false);
    }
}
