/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.cpu.m68000;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * The bugs of the m1 port fixed here, found against mame running the Ensoniq VFX.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
class MC68000Test {

    private static final int PROGRAM = 0x1000;
    private static final int IRQ_HANDLER = 0x2000;
    private static final int LINE_A_HANDLER = 0x2100;
    /** a write here raises irq 1, from inside the instruction that writes */
    private static final int IRQ_PORT = 0xff0000;

    private final byte[] memory = new byte[0x10000];
    private MC68000 cpu;
    private int pc;

    private int word(int a) {
        return ((memory[a & 0xffff] & 0xff) << 8) | (memory[(a + 1) & 0xffff] & 0xff);
    }

    private void putWord(int a, int v) {
        memory[a & 0xffff] = (byte) (v >> 8);
        memory[(a + 1) & 0xffff] = (byte) v;
    }

    private void putLong(int a, int v) {
        putWord(a, v >>> 16);
        putWord(a + 2, v);
    }

    @BeforeEach
    void setup() {
        cpu = new MC68000();
        cpu.ReadByte = a -> memory[a & 0xffff];
        cpu.ReadOpByte = cpu.ReadByte;
        cpu.ReadWord = a -> (short) word(a);
        cpu.ReadOpWord = cpu.ReadWord;
        cpu.ReadPcrelWord = cpu.ReadWord;
        cpu.ReadLong = a -> (word(a) << 16) | word(a + 2);
        cpu.ReadOpLong = cpu.ReadLong;
        cpu.ReadPcrelLong = cpu.ReadLong;
        cpu.WriteByte = (a, v) -> {
            if ((a & 0xff_ffff) == IRQ_PORT) cpu.set_irq_line(1); else memory[a & 0xffff] = v;
        };
        cpu.WriteWord = (a, v) -> putWord(a, v);
        cpu.WriteLong = (a, v) -> putLong(a, v);

        putLong(0, 0x8000); // ssp
        putLong(4, PROGRAM); // pc
        putLong(10 * 4, LINE_A_HANDLER);
        putLong(25 * 4, IRQ_HANDLER); // autovector 1
        putWord(IRQ_HANDLER, 0x4e71); // nop
        putWord(LINE_A_HANDLER, 0x4e71);
        pc = PROGRAM;
    }

    private void code(int... words) {
        for (int w : words) {
            putWord(pc, w);
            pc += 2;
        }
    }

    /** resets and runs that many instructions */
    private void run(int instructions) {
        cpu.reset();
        for (int i = 0; i < instructions; i++) {
            cpu.executeCycles(1);
        }
    }

    @Test
    void divuOverflow() {
        code(0x7080, // moveq #-128, d0
             0x80fc, 0x0001); // divu.w #1, d0
        run(2);
        // the quotient does not fit: d0 is left alone, v is set
        assertEquals(0xffff_ff80, cpu.D[0].s32);
        assertTrue(cpu.V);
        assertFalse(cpu.C);
    }

    @Test
    void divuUnsigned() {
        code(0x203c, 0x0001, 0x86a0, // move.l #100000, d0
             0x80fc, 0x0007); // divu.w #7, d0
        run(2);
        assertEquals((5 << 16) | 14285, cpu.D[0].s32);
        assertFalse(cpu.V);
    }

    @Test
    void muluUnsigned() {
        code(0x323c, 0xffff, // move.w #$ffff, d1
             0xc2fc, 0xffff); // mulu.w #$ffff, d1
        run(2);
        assertEquals(0xfffe_0001, cpu.D[1].s32);
    }

    @Test
    void swapIsLogical() {
        code(0x203c, 0xff00, 0x0000, // move.l #$ff000000, d0
             0x4840); // swap d0
        run(2);
        assertEquals(0x0000_ff00, cpu.D[0].s32);
    }

    @Test
    void lsrLongIsLogical() {
        code(0x203c, 0x8000, 0x0000, // move.l #$80000000, d0
             0xe288); // lsr.l #1, d0
        run(2);
        assertEquals(0x4000_0000, cpu.D[0].s32);
    }

    @Test
    void addWordCarry() {
        code(0x303c, 0x7fff, // move.w #$7fff, d0
             0x323c, 0xffff, // move.w #$ffff, d1
             0xd041); // add.w d1, d0
        run(3);
        assertEquals(0x7ffe, cpu.D[0].s32 & 0xffff);
        assertTrue(cpu.C);
        assertTrue(cpu.X);
    }

    @Test
    void addiLongCarry() {
        code(0x203c, 0x8000, 0x0000, // move.l #$80000000, d0
             0x0680, 0x8000, 0x0000); // addi.l #$80000000, d0
        run(2);
        assertEquals(0, cpu.D[0].s32);
        assertTrue(cpu.C);
        assertTrue(cpu.Z);
    }

    @Test
    void movepReadsUnsignedBytes() {
        putWord(0x3000, 0x0000);
        putWord(0x3002, 0x0000);
        putWord(0x3004, 0x0000);
        putWord(0x3006, 0x00ff);
        code(0x207c, 0x0000, 0x3001, // movea.l #$3001, a0
             0x0548, 0x0000); // movep.l 0(a0), d2
        run(2);
        assertEquals(0x0000_00ff, cpu.D[2].s32);
    }

    @Test
    void lineA() {
        code(0xa000); // the VFX's system call
        run(1);
        assertEquals(LINE_A_HANDLER, cpu.PC);
        // the frame holds the address of the line a instruction itself
        assertEquals(PROGRAM, (word(cpu.A[7].s32 + 2) << 16) | word(cpu.A[7].s32 + 4));
    }

    @Test
    void interruptBetweenInstructions() {
        code(0x46fc, 0x2000, // move #$2000, sr
             0x13c0, 0x00ff, 0x0000, // move.b d0, $ff0000.l, raises irq 1
             0x4e71); // nop
        run(2);
        // the move completes before the interrupt is taken
        assertEquals(IRQ_HANDLER, cpu.PC);
        int stacked = (word(cpu.A[7].s32 + 2) << 16) | word(cpu.A[7].s32 + 4);
        assertEquals(PROGRAM + 10, stacked);
    }
}
