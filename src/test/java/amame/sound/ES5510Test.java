/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * ES5510 through its host interface, the way the VFX's self test talks to it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
class ES5510Test {

    private static void writeGpr(ES5510 esp, int reg, int value) {
        esp.hostW(0x00, value >> 16);
        esp.hostW(0x01, value >> 8);
        esp.hostW(0x02, value);
        esp.hostW(0xa0, reg);
    }

    private static int readGpr(ES5510 esp, int reg) {
        esp.hostW(0x80, reg);
        return (esp.hostR(0x00) << 16) | (esp.hostR(0x01) << 8) | esp.hostR(0x02);
    }

    @Test
    void gpr() {
        ES5510 esp = new ES5510();
        esp.reset();
        writeGpr(esp, 0x10, 0x123456);
        assertEquals(0x123456, readGpr(esp, 0x10));
    }

    /** mame keeps the whole value written to sigreg, ccr and cmr, the VFX's self test reads it back */
    @Test
    void specialRegistersReadBack() {
        ES5510 esp = new ES5510();
        esp.reset();
        writeGpr(esp, 0xf9, 0xffffff); // sigreg
        assertEquals(0xff, readGpr(esp, 0xf9) >> 16);
        writeGpr(esp, 0xfa, 0x0000ff); // ccr, an int8_t
        assertEquals(0xffffff, readGpr(esp, 0xfa));
    }

    /** a program that copies serial in 0 left to serial out 3 left, one sample per run */
    @Test
    void program() {
        ES5510 esp = new ES5510();
        esp.reset();
        // MOV SER0L > SER3L; mac c, d unused (ZERO), alu b = SER0L (235), a = SER3L (241)
        long mov = (0xffL << 40) | (0xffL << 32) | (235L << 24) | (241L << 16) | (0x9L << 12);
        long end = (0xffL << 40) | (0xffL << 32) | (0xffL << 24) | (0xffL << 16) | (0xfL << 12);
        long nop = (0xffL << 40) | (0xffL << 32) | (0xffL << 24) | (0xffL << 16) | (0x9L << 12);
        long[] program = {mov, nop, nop, end};
        for (int i = 0; i < program.length; i++) {
            for (int b = 0; b < 6; b++) esp.hostW(0x03 + b, (int) (program[i] >> (40 - b * 8)));
            esp.hostW(0xc0, i);
        }
        esp.serW(0, (short) 0x1234);
        esp.runOnce();
        esp.runOnce();
        assertEquals(0x1234, esp.serR(6));
    }
}
