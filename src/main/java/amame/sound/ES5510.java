/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.sound;

import java.util.Arrays;


/**
 * Ensoniq ES5510 ESP, the effects dsp, after mame's es5510.cpp (Christian Brunschen).
 * <p>
 * mame 0.289 does not schedule it as a cpu on the VFX, the pump runs its program once per
 * sample with {@link #runOnce()}, which is the only way it runs here.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class ES5510 {

    public static final int DRAM_SIZE = 1 << 20;
    public static final int DRAM_MASK = DRAM_SIZE - 1;

    private static final int FLAG_N = 1 << 7;
    private static final int FLAG_C = 1 << 6;
    private static final int FLAG_V = 1 << 5;
    private static final int FLAG_LT = 1 << 4;
    private static final int FLAG_Z = 1 << 3;
    private static final int FLAG_NOT = 1 << 2;
    private static final int FLAG_MASK = FLAG_N | FLAG_C | FLAG_V | FLAG_LT | FLAG_Z;

    private static final int STATE_RUNNING = 0;
    private static final int STATE_HALTED = 1;

    private static final int SRC_DST_REG = 1, SRC_DST_DELAY = 2, SRC_DST_BOTH = 3;

    /** alu src, alu dst, mac src, mac dst */
    private static final int[][] OPERAND_SELECT = {
            {SRC_DST_REG, SRC_DST_REG, SRC_DST_REG, SRC_DST_REG},
            {SRC_DST_REG, SRC_DST_REG, SRC_DST_REG, SRC_DST_DELAY},
            {SRC_DST_REG, SRC_DST_REG, SRC_DST_REG, SRC_DST_BOTH},
            {SRC_DST_REG, SRC_DST_REG, SRC_DST_DELAY, SRC_DST_REG},
            {SRC_DST_REG, SRC_DST_REG, SRC_DST_DELAY, SRC_DST_BOTH},
            {SRC_DST_REG, SRC_DST_DELAY, SRC_DST_REG, SRC_DST_REG},
            {SRC_DST_REG, SRC_DST_DELAY, SRC_DST_DELAY, SRC_DST_REG},
            {SRC_DST_REG, SRC_DST_BOTH, SRC_DST_REG, SRC_DST_REG},
            {SRC_DST_REG, SRC_DST_BOTH, SRC_DST_DELAY, SRC_DST_REG},
            {SRC_DST_DELAY, SRC_DST_REG, SRC_DST_REG, SRC_DST_REG},
            {SRC_DST_DELAY, SRC_DST_REG, SRC_DST_REG, SRC_DST_DELAY},
            {SRC_DST_DELAY, SRC_DST_REG, SRC_DST_REG, SRC_DST_BOTH},
            {SRC_DST_DELAY, SRC_DST_REG, SRC_DST_DELAY, SRC_DST_REG},
            {SRC_DST_DELAY, SRC_DST_REG, SRC_DST_DELAY, SRC_DST_BOTH},
            {SRC_DST_DELAY, SRC_DST_BOTH, SRC_DST_REG, SRC_DST_REG},
            {SRC_DST_DELAY, SRC_DST_BOTH, SRC_DST_DELAY, SRC_DST_REG},
    };

    /** the operand count of each alu op */
    private static final int[] ALU_OPERANDS = {2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 0};

    /** the CMP operation is not affected by being skippable */
    private static final int OP_CMP = 4;

    private static final int RAM_CYCLE_READ = 0, RAM_CYCLE_WRITE = 1, RAM_CYCLE_DUMP_FIFO = 2;
    private static final int RAM_CONTROL_DELAY = 0, RAM_CONTROL_TABLE_A = 1, RAM_CONTROL_TABLE_B = 2, RAM_CONTROL_IO = 3;
    /** cycle, access */
    private static final int[][] RAM_CONTROL = {
            {RAM_CYCLE_READ, RAM_CONTROL_DELAY},
            {RAM_CYCLE_WRITE, RAM_CONTROL_DELAY},
            {RAM_CYCLE_READ, RAM_CONTROL_TABLE_A},
            {RAM_CYCLE_WRITE, RAM_CONTROL_TABLE_A},
            {RAM_CYCLE_READ, RAM_CONTROL_TABLE_B},
            {RAM_CYCLE_DUMP_FIFO, RAM_CONTROL_DELAY},
            {RAM_CYCLE_READ, RAM_CONTROL_IO},
            {RAM_CYCLE_WRITE, RAM_CONTROL_IO},
    };

    private int icount;
    private boolean haltAsserted;
    private int pc;
    private int state = STATE_HALTED;
    private final int[] gpr = new int[0xc0];
    private short ser0r, ser0l, ser1r, ser1l, ser2r, ser2l, ser3r, ser3l;
    private long machl;
    private boolean macOverflow;
    private int dil;
    private int memsiz = 0x00ffffff;
    private int memmask = 0x00000000;
    private int memincrement = 0x01000000;
    private int memshift = 24;
    private int dlength;
    private int abase;
    private int bbase;
    private int dbase;
    private int sigreg = 1;
    private final int mulshift = 1;
    private int ccr;
    private int cmr;
    private final int[] dol = new int[2];
    private int dolCount;
    private final long[] instr = new long[160];
    private final short[] dram = new short[DRAM_SIZE];

    private int dolLatch;
    private int dilLatch;
    private int dadrLatch;
    private int gprLatch;
    private long instrLatch;
    private int ramSel;
    private int hostControl;
    private int hostSerial;

    // alu
    private int aluAReg, aluBReg, aluSrc, aluDst, aluOp;
    private int aluAValue, aluBValue;
    private boolean aluUpdateCcr, aluWriteResult;

    // mulacc
    private int macCReg, macDReg, macSrc, macDst;
    private boolean macAccumulate;
    private int macCValue, macDValue;
    private boolean macWriteResult;

    // ram operations for cycles N, N-1 and N-2
    private int ramAddress, ramPAddress, ramPPAddress;
    private boolean ramIo, ramPIo, ramPPIo;
    private int ramCycle, ramPCycle, ramPPCycle;

    private static int setFlagTo(int ccr, int flag, boolean set) {
        return set ? ccr | flag : ccr & ~flag;
    }

    private static boolean isFlagSet(int ccr, int flag) {
        return (ccr & flag) != 0;
    }

    private static int sext24(int v) {
        return (v << 8) >> 8;
    }

    private static long sext48(long v) {
        return (v << 16) >> 16;
    }

    /** the flags come back in flags[0] */
    private static int add(int a, int b, int[] flags) {
        int aSign = a & 0x00800000;
        int bSign = b & 0x00800000;
        int result = a + b;
        int resultSign = result & 0x00800000;
        boolean overflow = (aSign == bSign) && (aSign != resultSign);
        boolean carry = (result & 0x01000000) != 0;
        boolean negative = resultSign != 0;
        boolean lessThan = (overflow && !negative) || (!overflow && negative);
        int f = flags[0];
        f = setFlagTo(f, FLAG_C, carry);
        f = setFlagTo(f, FLAG_N, negative);
        f = setFlagTo(f, FLAG_Z, (result & 0x00ffffff) == 0);
        f = setFlagTo(f, FLAG_V, overflow);
        f = setFlagTo(f, FLAG_LT, lessThan);
        flags[0] = f;
        return result & 0x00ffffff;
    }

    private static int saturate(int value, int[] flags, boolean negative) {
        if (isFlagSet(flags[0], FLAG_V)) {
            flags[0] = setFlagTo(flags[0], FLAG_N, negative);
            flags[0] &= ~FLAG_Z;
            return negative ? 0x00800000 : 0x007fffff;
        } else {
            return value;
        }
    }

    private static int negate(int value) {
        return ((value ^ 0x00ffffff) + 1) & 0x00ffffff;
    }

    private static int asl(int value, int shift, int[] flags) {
        int signBefore = value & 0x00800000;
        int result = value << shift;
        int signAfter = result & 0x00800000;
        boolean overflow = signBefore != signAfter;
        flags[0] = setFlagTo(flags[0], FLAG_Z, (result & 0x00ffffff) == 0);
        flags[0] = setFlagTo(flags[0], FLAG_V, overflow);
        return saturate(result, flags, signBefore != 0);
    }

    private short dramR(int addr) {
        return dram[addr & DRAM_MASK];
    }

    private void dramW(int addr, short data) {
        dram[addr & DRAM_MASK] = data;
    }

    public void setHalt(boolean halt) {
        haltAsserted = halt;
    }

    public int hostR(int offset) {
        return switch (offset) {
            case 0x00 -> (gprLatch >> 16) & 0xff;
            case 0x01 -> (gprLatch >> 8) & 0xff;
            case 0x02 -> gprLatch & 0xff;
            case 0x03 -> (int) ((instrLatch >> 40) & 0xff);
            case 0x04 -> (int) ((instrLatch >> 32) & 0xff);
            case 0x05 -> (int) ((instrLatch >> 24) & 0xff);
            case 0x06 -> (int) ((instrLatch >> 16) & 0xff);
            case 0x07 -> (int) ((instrLatch >> 8) & 0xff);
            case 0x08 -> (int) (instrLatch & 0xff);
            case 0x09 -> (dilLatch >> 16) & 0xff;
            case 0x0a -> (dilLatch >> 8) & 0xff;
            case 0x0b -> 0;
            case 0x0c -> (dolLatch >> 16) & 0xff;
            case 0x0d -> (dolLatch >> 8) & 0xff;
            case 0x0e -> 0xff;
            case 0x0f -> (dadrLatch >> 16) & 0xff;
            case 0x10 -> (dadrLatch >> 8) & 0xff;
            case 0x11 -> dadrLatch & 0xff;
            case 0x12 -> 0; // Host Control
            case 0x16 -> 0x27; // Program Counter, for test purposes only
            default -> 0;
        };
    }

    public void hostW(int offset, int data) {
        data &= 0xff;
        switch (offset) {
            case 0x00 -> gprLatch = (gprLatch & 0x00ffff) | (data << 16);
            case 0x01 -> gprLatch = (gprLatch & 0xff00ff) | (data << 8);
            case 0x02 -> gprLatch = (gprLatch & 0xffff00) | data;
            // 0x03 to 0x08 INSTR Register
            case 0x03 -> instrLatch = (instrLatch & 0x00ffffffffffL) | ((long) data << 40);
            case 0x04 -> instrLatch = (instrLatch & 0xff00ffffffffL) | ((long) data << 32);
            case 0x05 -> instrLatch = (instrLatch & 0xffff00ffffffL) | ((long) data << 24);
            case 0x06 -> instrLatch = (instrLatch & 0xffffff00ffffL) | ((long) data << 16);
            case 0x07 -> instrLatch = (instrLatch & 0xffffffff00ffL) | ((long) data << 8);
            case 0x08 -> instrLatch = (instrLatch & 0xffffffffff00L) | data;
            // 0x09 to 0x0b DIL Register (r/o)
            case 0x0c -> dolLatch = (dolLatch & 0x00ffff) | (data << 16);
            case 0x0d -> dolLatch = (dolLatch & 0xff00ff) | (data << 8);
            case 0x0e -> dolLatch = (dolLatch & 0xffff00) | data;
            case 0x0f -> {
                dadrLatch = (dadrLatch & 0x00ffff) | (data << 16);
                if (ramSel != 0) {
                    dilLatch = dramR(dadrLatch) << 8;
                } else {
                    dramW(dadrLatch, (short) (dolLatch >> 8));
                }
            }
            case 0x10 -> dadrLatch = (dadrLatch & 0xff00ff) | (data << 8);
            case 0x11 -> dadrLatch = (dadrLatch & 0xffff00) | data;
            // 0x12 Host Control
            case 0x12 -> {
                hostControl = (hostControl & 0x4) | (data & 0x3);
                if ((hostControl & 2) != 0) { // RAM clear
                    if (state == STATE_HALTED) {
                        Arrays.fill(dram, (short) 0);
                    }
                    hostControl &= ~0x2;
                }
            }
            case 0x14 -> ramSel = data & 0x80;
            case 0x18 -> hostSerial = data;
            // 0x1f Halt enable (w) / Frame Counter (r)
            case 0x1f -> {
                if (haltAsserted) {
                    state = STATE_HALTED;
                }
            }
            case 0x80 -> { // Read select - GPR + INSTR
                if (data < 0xa0) {
                    instrLatch = instr[data];
                }
                if (data < 0xc0) {
                    gprLatch = gpr[data] & 0xffffff;
                } else if (data >= 0xea) {
                    gprLatch = readReg(data);
                }
            }
            case 0xa0 -> writeReg(data, gprLatch); // Write select - GPR
            case 0xc0 -> { // Write select - INSTR
                if (data < 0xa0) {
                    instr[data] = instrLatch & 0xffffffffffffL;
                }
            }
            case 0xe0 -> { // Write select - GPR + INSTR
                if (data < 0xa0) {
                    instr[data] = instrLatch;
                }
                writeReg(data, gprLatch);
            }
            default -> {}
        }
    }

    public short serR(int offset) {
        return switch (offset) {
            case 0 -> ser0l;
            case 1 -> ser0r;
            case 2 -> ser1l;
            case 3 -> ser1r;
            case 4 -> ser2l;
            case 5 -> ser2r;
            case 6 -> ser3l;
            case 7 -> ser3r;
            default -> 0;
        };
    }

    public void serW(int offset, short data) {
        switch (offset) {
            case 0 -> ser0l = data;
            case 1 -> ser0r = data;
            case 2 -> ser1l = data;
            case 3 -> ser1r = data;
            case 4 -> ser2l = data;
            case 5 -> ser2r = data;
            case 6 -> ser3l = data;
            case 7 -> ser3r = data;
            default -> {}
        }
    }

    public void reset() {
        pc = 0x00;
        Arrays.fill(gpr, 0);
        Arrays.fill(instr, 0);
        Arrays.fill(dram, (short) 0);
        state = STATE_RUNNING;
        dilLatch = dolLatch = dadrLatch = gprLatch = 0;
        instrLatch = 0;
        ramSel = 0;
        hostControl = 0x04; // Signal Host Access not OK
        hostSerial = 0;
        ramAddress = ramPAddress = ramPPAddress = 0;
        ramIo = ramPIo = ramPPIo = false;
        ramCycle = ramPCycle = ramPPCycle = 0;
    }

    private final int[] flagsBox = new int[1];

    private void executeRun() {
        while (icount > 0) {
            if (state == STATE_HALTED) {
                // Currently halted, sample the HALT line
                if (haltAsserted) {
                    hostControl &= ~0x04; // Signal Host Access OK
                } else {
                    // start from the beginning at PC 0
                    state = STATE_RUNNING;
                    hostControl |= 0x04; // Signal Host Access not OK
                    pc = 0;
                }
            } else {
                // currently running, execute one instruction.
                ramPPAddress = ramPAddress; ramPPIo = ramPIo; ramPPCycle = ramPCycle;
                ramPAddress = ramAddress; ramPIo = ramIo; ramPCycle = ramCycle;

                // --- Read instruction N
                // a program without an END runs off the 160 instructions, which reads as one
                long instr = pc < 160 ? this.instr[pc] : 0xffff_ffff_ffffL;

                // --- RAM cycle N-2 (if a Read cycle): data read from bus is stored in DIL
                if (ramPPCycle != RAM_CYCLE_WRITE) {
                    if (ramPPIo) {
                        dil = 0; // read_io(ram_pp.address);
                    } else {
                        dil = dramR(ramPPAddress) << 8;
                    }
                }

                // --- start of RAM cycle N
                int[] ramControl = RAM_CONTROL[(int) ((instr >> 3) & 0x07)];
                ramCycle = ramControl[0];
                ramIo = ramControl[1] == RAM_CONTROL_IO;

                // --- RAM cycle N: read offset N
                int offset = pc < 0xc0 ? gpr[pc] : 0;
                switch (ramControl[1]) {
                    case RAM_CONTROL_DELAY ->
                            ramAddress = (Integer.remainderUnsigned(dbase + offset, dlength + memincrement) & memmask) >> memshift;
                    case RAM_CONTROL_TABLE_A -> ramAddress = ((abase + offset) & memmask) >> memshift;
                    case RAM_CONTROL_TABLE_B -> ramAddress = ((bbase + offset) & memmask) >> memshift;
                    default -> ramAddress = offset & 0x00fffff0; // mask off the low 4 bits
                }

                // --- Decode instruction N
                int operandSelect = (int) ((instr >> 8) & 0x0f);
                int[] opSelect = OPERAND_SELECT[operandSelect];
                boolean skip;
                boolean skippable = (instr & (0x01 << 7)) != 0;
                if (skippable) {
                    boolean skipConditionSatisfied = (ccr & cmr & FLAG_MASK) != 0;
                    if (isFlagSet(cmr, FLAG_NOT)) {
                        skipConditionSatisfied = !skipConditionSatisfied;
                    }
                    skip = skipConditionSatisfied;
                } else {
                    skip = false;
                }

                // --- Write Multiplier result N-1
                if (macWriteResult) {
                    long product = ((long) sext24(macCValue) * (long) sext24(macDValue)) << mulshift;
                    long result = macAccumulate ? product + machl : product;
                    macOverflow = result < -(1L << 47) || result >= (1L << 47);
                    machl = result;
                    int tmp = macOverflow ? (machl < 0 ? 0x00800000 : 0x007fffff) : (int) ((result & 0x0000ffffff000000L) >> 24);
                    if ((macDst & SRC_DST_REG) != 0) {
                        writeReg(macCReg, tmp);
                    }
                    if ((macDst & SRC_DST_DELAY) != 0) {
                        writeToDol(tmp);
                    }
                }

                // --- Start of multiplier cycle N
                macCReg = (int) ((instr >> 32) & 0xff);
                macDReg = (int) ((instr >> 40) & 0xff);
                macSrc = opSelect[2];
                macDst = opSelect[3];
                macAccumulate = ((instr >> 6) & 0x01) != 0;
                macWriteResult = !skip;

                // --- Read Multiplier Operands N
                if (macSrc == SRC_DST_REG) {
                    macCValue = readReg(macCReg);
                } else { // must be SRC_DST_DELAY
                    macCValue = dil;
                }
                macDValue = readReg(macDReg);

                // --- Write ALU Result N-1
                if (aluWriteResult) {
                    flagsBox[0] = ccr;
                    int result = aluOperation(aluOp, aluAValue, aluBValue, flagsBox);
                    if ((aluDst & SRC_DST_REG) != 0) {
                        writeReg(aluAReg, result);
                    }
                    if ((aluDst & SRC_DST_DELAY) != 0) {
                        writeToDol(result);
                    }
                    if (aluUpdateCcr) {
                        ccr = flagsBox[0];
                    }
                }

                // --- Start of ALU cycle N
                aluAReg = (int) ((instr >> 16) & 0xff);
                aluBReg = (int) ((instr >> 24) & 0xff);
                aluOp = (int) ((instr >> 12) & 0x0f);
                aluSrc = opSelect[0];
                aluDst = opSelect[1];
                aluWriteResult = !skip;
                aluUpdateCcr = !skippable || (aluOp == OP_CMP);

                if (aluOp == 0xf) {
                    aluOperationEnd();
                } else {
                    // --- Read ALU Operands N
                    if (ALU_OPERANDS[aluOp] == 1) {
                        if (aluSrc == SRC_DST_REG) {
                            aluBValue = readReg(aluBReg);
                        } else { // must be SRC_DST_DELAY
                            aluBValue = dil;
                        }
                    } else {
                        if (aluSrc == SRC_DST_REG) {
                            aluAValue = readReg(aluAReg);
                        } else { // must be SRC_DST_DELAY
                            aluAValue = dil;
                        }
                        aluBValue = readReg(aluBReg);
                    }
                }

                // --- RAM cycle N-1
                if (ramPCycle != RAM_CYCLE_READ) {
                    if (ramPCycle == RAM_CYCLE_WRITE) {
                        // If this is a write cycle, write the frontmost DOL value to RAM or I/O
                        if (!ramPIo) {
                            dramW(ramPAddress, (short) (dol[0] >> 8));
                        }
                    }
                    // If this is a Write or Dump cycle, eject the frontmost DL value.
                    dol[0] = dol[1];
                    if (dolCount > 0) {
                        --dolCount;
                    }
                }

                ++pc;
            }
            --icount;
        }
    }

    private int readReg(int reg) {
        if (reg < 0xc0) {
            return gpr[reg];
        }
        return switch (reg) {
            case 234 -> ser0r << 8;
            case 235 -> ser0l << 8;
            case 236 -> ser1r << 8;
            case 237 -> ser1l << 8;
            case 238 -> ser2r << 8;
            case 239 -> ser2l << 8;
            case 240 -> ser3r << 8;
            case 241 -> ser3l << 8;
            case 242 -> macOverflow ? (machl < 0 ? 0x00000000 : 0x00ffffff) : (int) (machl & 0x00ffffff);
            case 243 -> macOverflow ? (machl < 0 ? 0x00800000 : 0x007fffff) : (int) ((machl >> 24) & 0x00ffffff);
            case 244 -> dil; // DIL when reading
            case 245 -> dlength;
            case 246 -> abase;
            case 247 -> bbase;
            case 248 -> dbase;
            case 249 -> sigreg;
            case 250 -> ccr;
            case 251 -> cmr;
            case 252 -> 0x00ffffff; // minus one
            case 253 -> 0x00800000; // min
            case 254 -> 0x007fffff; // max
            default -> 0; // zero, unknown
        };
    }

    /** runs the program once, which is one sample */
    public void runOnce() {
        // turn HALT off
        haltAsserted = false;

        // run for one instruction
        icount = 1;
        executeRun();

        // turn HALT on again
        haltAsserted = true;

        // run ESP to the end of its program, a few instructions at a time
        int guard = 0;
        while (state != STATE_HALTED && guard++ < 1024) {
            icount = 1;
            executeRun();
        }
    }

    private static int countLowOnes(int x) {
        int n = 0;
        while ((x & 1) == 1) {
            ++n;
            x >>= 1;
        }
        return n;
    }

    private void writeReg(int reg, int value) {
        value &= 0x00ffffff;
        if (reg < 0xc0) {
            gpr[reg] = value;
            return;
        }
        switch (reg) {
            case 234 -> ser0r = (short) (value >> 8);
            case 235 -> ser0l = (short) (value >> 8);
            case 236 -> ser1r = (short) (value >> 8);
            case 237 -> ser1l = (short) (value >> 8);
            case 238 -> ser2r = (short) (value >> 8);
            case 239 -> ser2l = (short) (value >> 8);
            case 240 -> ser3r = (short) (value >> 8);
            case 241 -> ser3l = (short) (value >> 8);
            case 242 -> { // macl
                long masked = machl & (0x00ffffffL << 24);
                long shifted = value & 0x00ffffffL;
                machl = sext48(masked | shifted);
            }
            case 243 -> { // mach
                long masked = machl & 0x00ffffffL;
                long shifted = (long) (value & 0x00ffffff) << 24;
                machl = sext48(masked | shifted);
                macOverflow = false;
            }
            case 244 -> { // MEMSIZ when writing
                memshift = countLowOnes(value);
                memsiz = 0x00ffffff >> (24 - memshift);
                memmask = 0x00ffffff & ~memsiz;
                memincrement = 1 << memshift;
            }
            case 245 -> dlength = value;
            case 246 -> abase = value;
            case 247 -> bbase = value;
            case 248 -> dbase = value;
            // mame's WRITE_REG(r, x) stores the value and not x, into an int8_t for ccr and cmr,
            // and the self test of the VFX reads that back
            case 249 -> sigreg = value;
            case 250 -> ccr = (byte) value;
            case 251 -> cmr = (byte) value;
            default -> {} // the constants and unknown registers
        }
    }

    private void writeToDol(int value) {
        if (dolCount >= 2) {
            dol[0] = dol[1];
            dol[1] = value;
        } else {
            dol[dolCount++] = value;
        }
    }

    private void aluOperationEnd() {
        // sample the HALT line
        if (haltAsserted) {
            state = STATE_HALTED;
            hostControl &= ~0x04; // Signal Host Access OK
        }
        // update the delay line base pointer
        dbase -= memincrement;
        if (dbase < 0) {
            dbase = dlength;
        }
        // Possibly reset the PC
        if (state == STATE_RUNNING) {
            pc = 0;
        }
    }

    private static int aluOperation(int op, int a, int b, int[] flags) {
        int tmp;
        switch (op) {
            case 0x0: // ADD
                tmp = add(a, b, flags);
                return saturate(tmp, flags, (a & 0x00800000) != 0);
            case 0x1: // SUB
                tmp = add(a, negate(b), flags);
                return saturate(tmp, flags, (a & 0x00800000) != 0);
            case 0x2: // ADDU
                return add(a, b, flags);
            case 0x3: // SUBU
                return add(a, negate(b), flags);
            case 0x4: // CMP
                add(a, negate(b), flags);
                return a;
            case 0x5: // AND
                a &= b;
                flags[0] = setFlagTo(flags[0], FLAG_N, (a & 0x00800000) != 0);
                flags[0] = setFlagTo(flags[0], FLAG_Z, a == 0);
                return a;
            case 0x6: // OR
                a |= b;
                flags[0] = setFlagTo(flags[0], FLAG_N, (a & 0x00800000) != 0);
                flags[0] = setFlagTo(flags[0], FLAG_Z, a == 0);
                return a;
            case 0x7: // XOR
                a ^= b;
                flags[0] = setFlagTo(flags[0], FLAG_N, (a & 0x00800000) != 0);
                flags[0] = setFlagTo(flags[0], FLAG_Z, a == 0);
                return a;
            case 0x8: { // ABS
                flags[0] &= ~FLAG_N;
                boolean isNegative = (a & 0x00800000) != 0;
                flags[0] = setFlagTo(flags[0], FLAG_C, isNegative);
                // Note: the absolute value is calculated by one's complement!
                return isNegative ? (0x00ffffff ^ a) : a;
            }
            case 0x9: // MOV
                return b;
            case 0xa: // ASL2
                return asl(b, 2, flags);
            case 0xb: // ASL8
                return asl(b, 8, flags);
            case 0xc: // LS15
                flags[0] &= ~FLAG_N;
                flags[0] = setFlagTo(flags[0], FLAG_C, (b & 0x00800000) != 0);
                return (b << 15) & 0x007fffff;
            case 0xd: // DIFF
                return add(0x007fffff, negate(b), flags);
            case 0xe: // ASR
                flags[0] = setFlagTo(flags[0], FLAG_N, (b & 0x00800000) != 0);
                flags[0] = setFlagTo(flags[0], FLAG_C, (b & 1) != 0);
                return (b >> 1) | (b & 0x00800000);
            default: // END - handled separately in alu_operation_end()
                return 0;
        }
    }
}
