/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.cpu;

import java.util.function.IntUnaryOperator;

import amame.cpu.m68000.MC68000;
import amame.emu.Cpu;


/**
 * The m1 MC68000 core on the scheduler, a 24 bit bus.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class M68000Cpu extends Cpu {

    /** the 16 bit bus, addresses are 24 bit */
    public interface Bus {
        int read8(int address);

        int read16(int address);

        void write8(int address, int data);

        void write16(int address, int data);

        /**
         * an opcode or pc relative fetch, which is program space: some boards decode it apart
         *
         * @param supervisor function code 6 rather than 2
         */
        default int readProgram16(int address, boolean supervisor) {
            return read16(address);
        }
    }

    private final MC68000 core = new MC68000();

    public M68000Cpu(double clock, Bus bus) {
        super(clock);
        core.ReadByte = a -> (byte) bus.read8(a & 0xff_ffff);
        core.ReadOpByte = a -> (byte) bus.readProgram16(a & 0xff_fffe, core.getS());
        core.ReadWord = a -> (short) bus.read16(a & 0xff_ffff);
        core.ReadOpWord = a -> (short) bus.readProgram16(a & 0xff_ffff, core.getS());
        core.ReadPcrelWord = core.ReadOpWord;
        core.ReadLong = a -> (bus.read16(a & 0xff_ffff) << 16) | (bus.read16((a + 2) & 0xff_ffff) & 0xffff);
        core.ReadOpLong = a -> (bus.readProgram16(a & 0xff_ffff, core.getS()) << 16) | (bus.readProgram16((a + 2) & 0xff_ffff, core.getS()) & 0xffff);
        core.ReadPcrelLong = core.ReadOpLong;
        core.WriteByte = (a, v) -> bus.write8(a & 0xff_ffff, v & 0xff);
        core.WriteWord = (a, v) -> bus.write16(a & 0xff_ffff, v & 0xffff);
        core.WriteLong = (a, v) -> {
            bus.write16(a & 0xff_ffff, (v >>> 16) & 0xffff);
            bus.write16((a + 2) & 0xff_ffff, v & 0xffff);
        };
    }

    public MC68000 getCore() {
        return core;
    }

    /** the level on the ipl pins, 0 for none */
    public void setIrqLevel(int level) {
        core.set_irq_line(level);
    }

    /** answers the interrupt acknowledge cycle with a vector number */
    public void setIrqAcknowledge(IntUnaryOperator irqAcknowledge) {
        core.irqAcknowledge = irqAcknowledge;
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
    protected void setRemainingCycles(int cycles) {
        core.setPendingCycles(cycles);
    }

    @Override
    public void reset() {
        core.reset();
    }
}
