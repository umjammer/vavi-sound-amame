/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.cpu;

import amame.cpu.z80.Z80A;
import amame.emu.Cpu;


/**
 * The m1 Z80A core on the scheduler.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class Z80Cpu extends Cpu {

    /** the memory and the i/o space of the cpu */
    public interface Bus {
        int read(int address);

        void write(int address, int data);

        int in(int port);

        void out(int port, int data);
    }

    private final Z80A core = new Z80A();

    public Z80Cpu(double clock, Bus bus) {
        super(clock);
        core.readOp = a -> (byte) bus.read(a & 0xffff);
        core.readOpArg = core.readOp;
        core.readMemory = core.readOp;
        core.writeMemory = (a, v) -> bus.write(a & 0xffff, v & 0xff);
        core.readHardware = a -> (byte) bus.in(a & 0xffff);
        core.writeHardware = (a, v) -> bus.out(a & 0xffff, v & 0xff);
    }

    public Z80A getCore() {
        return core;
    }

    /** the maskable interrupt line */
    public void setIrq(boolean state) {
        core.set_irq_line(0, state);
    }

    public void setNmi(boolean state) {
        core.set_irq_line(Z80A.INPUT_LINE_NMI, state);
    }

    @Override
    protected int execute(int cycles) {
        return core.executeCycles(cycles);
    }

    @Override
    protected int remainingCycles() {
        return core.getPendingCycles();
    }

    @Override
    public void reset() {
        core.reset();
    }
}
