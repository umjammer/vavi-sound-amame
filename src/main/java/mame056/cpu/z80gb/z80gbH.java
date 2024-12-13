/**
 * ported to v0.56
 */
package mame056.cpu.z80gb;

import static mame056.memory.cpu_readmem16;
import static mame056.memory.cpu_writemem16;

public class z80gbH {

    public static final int Z80GB_PC = 1;
    public static final int Z80GB_SP = 2;
    public static final int Z80GB_AF = 3;
    public static final int Z80GB_BC = 4;
    public static final int Z80GB_DE = 5;
    public static final int Z80GB_HL = 6;
    public static final int Z80GB_IRQ_STATE = 7;

    /**
     * *************************************************************************
     */
    /* Memory functions                                                         */
    /**
     * *************************************************************************
     */
    public static int mem_ReadByte(int addr) {
        return cpu_readmem16(addr) & 0xFF;
    }

    public static void mem_WriteByte(int addr, int value) {
        cpu_writemem16(addr, value & 0xFF);
    }

    public static int mem_ReadWord(int addr) {
        return (mem_ReadByte(addr) | (mem_ReadByte((addr + 1) & 0xffff) << 8)) & 0xFFFF;
    }

    public static void mem_WriteWord(int address, int data) {
        mem_WriteByte(address, data & 0xFF);
        mem_WriteByte((address + 1) & 0xffff, data >> 8);
    }

}
