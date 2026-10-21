/*
 * This Z80 emulator is a modified version of Ben Ryves 'Brazil' emulator.
 * It is MIT licensed.
 */

package amame.cpu.z80;


import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Scanner;
import java.util.function.Consumer;


import static java.lang.System.getLogger;
import static amame.cpu.z80.Tables.initialiseTables;
import static amame.cpu.z80.Tables.isS;
import static amame.cpu.z80.Tables.isX;
import static amame.cpu.z80.Tables.isY;
import static amame.cpu.z80.Tables.tableALU;
import static amame.cpu.z80.Tables.TableDaa;
import static amame.cpu.z80.Tables.tableDec;
import static amame.cpu.z80.Tables.tableHalfBorrow;
import static amame.cpu.z80.Tables.tableInc;
import static amame.cpu.z80.Tables.tableNeg;
import static amame.cpu.z80.Tables.tableParity;
import static amame.cpu.z80.Tables.tableRotShift;
import static amame.cpu.z80.Tables.cc_ex;
import static amame.cpu.z80.Tables.cc_op;


/**
 * ZiLOG Z80A CPU Emulator
 */
public class Z80A {

    private static final Logger logger = getLogger(Z80A.class.getName());

    private boolean interruptTable;
    private long totalExecutedCycles;
    private int pendingCycles;

    /** before instruction listener */
    private Consumer<Z80A> listener;

    /** add before instruction listener */
    public void addListener(Consumer<Z80A> listener) {
        this.listener = listener;
    }

    /** exec instruction listener */
    private void fireListener(Z80A arg) {
        if (listener != null)
            listener.accept(arg);
    }

    public long getTotalExecutedCycles() {
        return totalExecutedCycles;
    }

    public void setTotalExecutedCycles(long value) {
        totalExecutedCycles = value;
    }

    public int getPendingCycles() {
        return pendingCycles;
    }

    public void setPendingCycles(int value) {
        pendingCycles = value;
    }

    /**
     * Creates an instance of the {@link Z80A} emulator class.
     */
    public Z80A() {
        initialiseTables();
        // Clear main registers
        ppc = 0;
        regAF = new RegisterPair((short) 0x0040);
        regBC = new RegisterPair((short) 0);
        regDE = new RegisterPair((short) 0);
        regHL = new RegisterPair((short) 0);
        // Clear alternate registers
        regAltAF = new RegisterPair((short) 0);
        regAltBC = new RegisterPair((short) 0);
        regAltDE = new RegisterPair((short) 0);
        regAltHL = new RegisterPair((short) 0);
        // Clear special purpose registers
        regI = 0;
        regR = 0;
        regR2 = 0;
        setRegisterIX(0xffff);
        setRegisterIY(0xffff);
        setRegisterSP(0);
        setRegisterPC(0);
        setRegisterWZ(0);
        iff1 = iff2 = false;
        halted = false;
        interruptMode = 0;
    }

    /**
     * Reset the Z80 to its initial state
     */
    public void reset() {
        ResetRegisters();
        ResetInterrupts();
    }

    /** the line number of the non maskable interrupt, any other number is the maskable one */
    public static final int INPUT_LINE_NMI = 32;

    public void set_irq_line(int irqline, boolean state) {
        if (irqline == INPUT_LINE_NMI) {
            if (!nonMaskableInterrupt && state)
                nonMaskableInterruptPending = true;
            nonMaskableInterrupt = state;
        } else {
            interrupt = state;
        }
    }

    // Memory Access

    public Read8 readOp, readOpArg;
    public Read8 readMemory;
    public Write8 writeMemory;

    /** a byte read from the memory or the i/o space */
    @FunctionalInterface
    public interface Read8 {
        byte read(short address);
    }

    /** a byte write to the memory or the i/o space */
    @FunctionalInterface
    public interface Write8 {
        void write(short address, byte value);
    }

    public interface DebugDelegate extends Runnable {

    }

    public DebugDelegate debugger_start_cpu_hook_callback = () -> {}, debugger_stop_cpu_hook_callback = () -> {};

    public void unregisterMemoryMapper() {
        readMemory = null;
        writeMemory = null;
    }

    // Hardware I/O Port Access

    public Read8 readHardware;
    public Write8 writeHardware;

    // State Save/Load

    public void saveStateText(PrintWriter writer) {
        writer.printf("[Z80]");
        writer.printf("AF %4X", regAF.word);
        writer.printf("BC %4X", regBC.word);
        writer.printf("DE %4X", regDE.word);
        writer.printf("HL %4X", regHL.word);
        writer.printf("ShadowAF %4X", regAltAF.word);
        writer.printf("ShadowBC %4X", regAltBC.word);
        writer.printf("ShadowDE %4X", regAltDE.word);
        writer.printf("ShadowHL %4X", regAltHL.word);
        writer.printf("I %2X", regI);
        writer.printf("R %2X", regR);
        writer.printf("IX %4X", regIX.word);
        writer.printf("IY %4X", regIY.word);
        writer.printf("SP %4X", regSP.word);
        writer.printf("PC %4X", regPC.word);
        writer.printf("IRQ %s", interrupt);
        writer.printf("NMI %s", nonMaskableInterrupt);
        writer.printf("NMIPending %s", nonMaskableInterruptPending);
        writer.printf("IM %d", interruptMode);
        writer.printf("IFF1 %s", iff1);
        writer.printf("IFF2 %s", iff2);
        writer.printf("Halted %s", halted);
        writer.printf("ExecutedCycles %d", totalExecutedCycles);
        writer.printf("PendingCycles %d", pendingCycles);
        writer.printf("[/Z80]");
        writer.println();
    }

    public void loadStateText(Scanner reader) {
        while (reader.hasNextLine()) {
            String[] args = reader.nextLine().split(" ");
            if (args[0].trim().isEmpty()) continue;
            if (args[0].equals("[/Z80]")) break;
            if (args[0].equals("AF"))
                setRegisterAF(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("BC"))
                setRegisterBC(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("DE"))
                setRegisterDE(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("HL"))
                setRegisterHL(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("ShadowAF"))
                setRegisterShadowAF(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("ShadowBC"))
                setRegisterShadowBC(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("ShadowDE"))
                setRegisterShadowDE(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("ShadowHL"))
                setRegisterShadowHL(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("I"))
                regI = Byte.parseByte(args[1], 16) & 0xff;
            else if (args[0].equals("R"))
                regR = Byte.parseByte(args[1], 16) & 0xff;
            else if (args[0].equals("IX"))
                setRegisterIX(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("IY"))
                setRegisterIY(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("SP"))
                setRegisterSP(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("PC"))
                setRegisterPC(Short.parseShort(args[1], 16) & 0xffff);
            else if (args[0].equals("IRQ"))
                interrupt = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("NMI"))
                nonMaskableInterrupt = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("NMIPending"))
                nonMaskableInterruptPending = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("IM"))
                interruptMode = Integer.parseInt(args[1]);
            else if (args[0].equals("IFF1"))
                iff1 = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("IFF2"))
                iff2 = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("Halted"))
                halted = Boolean.parseBoolean(args[1]);
            else if (args[0].equals("ExecutedCycles"))
                totalExecutedCycles = Long.parseLong(args[1]);
            else if (args[0].equals("PendingCycles"))
                pendingCycles = Integer.parseInt(args[1]);

            else
                logger.log(Level.DEBUG, "Skipping unrecognized identifier " + args[0]);
        }
    }

//#region Registers

    //@StructLayout(LayoutKind.Explicit)
    public static class RegisterPair implements Serializable {

        //@FieldOffset(0)
        public int word;

        public void setWord(int value) {
            try {
                word = value & 0xffff;
            } finally {
                down();
            }
        }

        public int addWord(int value) {
            try {
                return word = add16(word, value);
            } finally {
                down();
            }
        }

        public int subWord(int value) {
            try {
                return word = sub16(word, value);
            } finally {
                down();
            }
        }

        public int incAndGetWord() {
            try {
                word = inc16(word);
                return word;
            } finally {
                down();
            }
        }

        public int getAndIncWord() {
            try {
                return word;
            } finally {
                word = inc16(word);
                down();
            }
        }

        public int decAndGetWord() {
            try {
                word = dec16(word);
                return word;
            } finally {
                down();
            }
        }

        public int getAndDecWord() {
            try {
                return word;
            } finally {
                word = dec16(word);
                down();
            }
        }

        //@FieldOffset(0)
        public int low;

        public void setLow(int value) {
            try {
                low = value & 0xff;
            } finally {
                up();
            }
        }

        public int add8Low(int value) {
            try {
                return low = Z80A.add8(low, value);
            } finally {
                up();
            }
        }

        public int sub8Low(int value) {
            try {
                return low = Z80A.sub8(low, value);
            } finally {
                up();
            }
        }

        public void setLowAnd(int value) {
            try {
                low &= value & 0xff;
            } finally {
                up();
            }
        }

        public void setLowOr(int value) {
            try {
                low |= value & 0xff;
            } finally {
                up();
            }
        }

        public int incAndGetLow() {
            try {
                low = inc8(low);
                return low;
            } finally {
                up();
            }
        }

        public int decAndGetLow() {
            try {
                low = dec8(low);
                return low;
            } finally {
                up();
            }
        }

        //@FieldOffset(1)
        public int high;

        public void setHigh(int value) {
            try {
                high = value & 0xff;
            } finally {
                up();
            }
        }

        public int sub8High(int value) {
            try {
                return high = Z80A.sub8(high, value);
            } finally {
                up();
            }
        }

        public void setHighAnd(int value) {
            try {
                high &= value & 0xff;
            } finally {
                up();
            }
        }

        public void setHighOr(int value) {
            try {
                high |= value & 0xff;
            } finally {
                up();
            }
        }

        public void setHighXor(int value) {
            try {
                high ^= value & 0xff;
            } finally {
                up();
            }
        }

        public int incAndGetHigh() {
            try {
                high = inc8(high);
                return high;
            } finally {
                up();
            }
        }

        public int decAndGetHigh() {
            try {
                high = dec8(high);
                return high;
            } finally {
                up();
            }
        }

        public RegisterPair(short value) {
            word = value & 0xffff;
            down();
        }

        private void down() {
            low = word & 0xff;
            high = (word >> 8) & 0xff;
        }

        private void up() {
            word = (high << 8) | low;
        }
    }

    public int getPC() {
        return regPC.word;
    }

    public boolean getRegFlagC() {
        return (regAF.low & 0x01) != 0;
    }

    public void setRegFlagC(boolean value) {
        setRegisterF((regAF.low & ~0x01) | (value ? 0x01 : 0x00));
    }

    public boolean getRegFlagN() {
        return (regAF.low & 0x02) != 0;
    }

    public void setRegFlagN(boolean value) {
        setRegisterF((regAF.low & ~0x02) | (value ? 0x02 : 0x00));
    }

    public boolean getRegFlagP() {
        return (regAF.low & 0x04) != 0;
    }

    public void setRegFlagP(boolean value) {
        setRegisterF((regAF.low & ~0x04) | (value ? 0x04 : 0x00));
    }

    public boolean getRegFlag3() {
        return (regAF.low & 0x08) != 0;
    }

    public void setRegFlag3(boolean value) {
        setRegisterF((regAF.low & ~0x08) | (value ? 0x08 : 0x00));
    }

    public boolean getRegFlagH() {
        return (regAF.low & 0x10) != 0;
    }

    public void setRegFlagH(boolean value) {
        setRegisterF((regAF.low & ~0x10) | (value ? 0x10 : 0x00));
    }

    public boolean getRegFlag5() {
        return (regAF.low & 0x20) != 0;
    }

    public void setRegFlag5(boolean value) {
        setRegisterF((regAF.low & ~0x20) | (value ? 0x20 : 0x00));
    }

    public boolean getRegFlagZ() {
        return (regAF.low & 0x40) != 0;
    }

    public void setRegFlagZ(boolean value) {
        setRegisterF((regAF.low & ~0x40) | (value ? 0x40 : 0x00));
    }

    public boolean getRegFlagS() {
        return (regAF.low & 0x80) != 0;
    }

    public void setRegFlagS(boolean value) {
        setRegisterF((regAF.low & ~0x80) | (value ? 0x80 : 0x00));
    }

    private final RegisterPair regAF;
    private final RegisterPair regBC;
    private final RegisterPair regDE;
    private final RegisterPair regHL;

    private final RegisterPair regAltAF; // Shadow for A and F
    private final RegisterPair regAltBC; // Shadow for B and C
    private final RegisterPair regAltDE; // Shadow for D and E
    private final RegisterPair regAltHL; // Shadow for H and L

    private int regI; // I (interrupt vector)
    private int regR; // R (memory refresh)

    private int regR2;

    private final RegisterPair regIX = new RegisterPair((short) 0); // IX (index register x)
    private final RegisterPair regIY = new RegisterPair((short) 0); // IY (index register y)
    private final RegisterPair regWZ = new RegisterPair((short) 0); // WZ

    private final RegisterPair regSP = new RegisterPair((short) 0); // SP (stack pointer)
    private final RegisterPair regPC = new RegisterPair((short) 0); // PC (program counter)

    private void ResetRegisters() {
        regI = 0;
        regR = 0;
        regR2 = 0;
        setRegisterPC(0);
        setRegisterWZ(0);
    }

    public int getRegisterA() {
        return regAF.high;
    }

    public void setRegisterA(int value) {
        regAF.setHigh(value);
    }

    public int getRegisterF() {
        return regAF.low;
    }

    public void setRegisterF(int value) {
        regAF.setLow(value);
    }

    public int getRegisterAF() {
        return regAF.word;
    }

    public void setRegisterAF(int value) {
        regAF.setWord(value);
    }

    public int getRegisterB() {
        return regBC.high;
    }

    public void setRegisterB(int value) {
        regBC.setHigh(value);
    }

    public int getRegisterC() {
        return regBC.low;
    }

    public void setRegisterC(int value) {
        regBC.setLow(value);
    }

    public int getRegisterBC() {
        return regBC.word;
    }

    public void setRegisterBC(int value) {
        regBC.setWord(value);
    }

    public int getRegisterD() {
        return regDE.high;
    }

    public void setRegisterD(int value) {
        regDE.setHigh(value);
    }

    public int getRegisterE() {
        return regDE.low;
    }

    public void setRegisterE(int value) {
        regDE.setLow(value);
    }

    public int getRegisterDE() {
        return regDE.word;
    }

    public void setRegisterDE(int value) {
        regDE.setWord(value);
    }

    public int getRegisterH() {
        return regHL.high;
    }

    public void setRegisterH(int value) {
        regHL.setHigh(value);
    }

    public int getRegisterL() {
        return regHL.low;
    }

    public void setRegisterL(int value) {
        regHL.setLow(value);
    }

    public int getRegisterHL() {
        return regHL.word;
    }

    public void setRegisterHL(int value) {
        regHL.setWord(value);
    }

    public int getRegisterPC() {
        return regPC.word;
    }

    public void setRegisterPC(int value) {
        regPC.setWord(value);
    }

    public int getRegisterSP() {
        return regSP.word;
    }

    public void setRegisterSP(int value) {
        regSP.setWord(value);
    }

    public int getRegisterIX() {
        return regIX.word;
    }

    public int getRegisterIXH() {
        return regIX.high;
    }

    public int getRegisterIXL() {
        return regIX.low;
    }

    public void setRegisterIX(int value) {
        regIX.setWord(value);
    }

    public void setRegisterIXH(int value) {
        regIX.setHigh(value);
    }

    public void setRegisterIXL(int value) {
        regIX.setLow(value);
    }

    public int getRegisterIY() {
        return regIY.word;
    }

    public void setRegisterIY(int value) {
        regIY.setWord(value);
    }

    public int getRegisterIYH() {
        return regIY.high;
    }

    public int getRegisterIYL() {
        return regIY.low;
    }

    public void setRegisterIYH(int value) {
        regIY.setHigh(value);
    }

    public void setRegisterIYL(int value) {
        regIY.setLow(value);
    }

    public int getRegisterWZ() {
        return regWZ.word;
    }

    public void setRegisterWZ(int value) {
        regWZ.setWord(value);
    }

    public int getRegisterI() {
        return regI;
    }

    public void setRegisterI(int value) {
        regI = value;
    }

    public int getRegisterR() {
        return regR;
    }

    public void setRegisterR(int value) {
        regR = value;
    }

    public int getRegisterR2() {
        return regR2;
    }

    public void setRegisterR2(int value) {
        regR2 = value & 0xff;
    }

    public int getRegisterShadowAF() {
        return regAltAF.word;
    }

    public void setRegisterShadowAF(int value) {
        regAltAF.setWord(value);
    }

    public int getRegisterShadowBC() {
        return regAltBC.word;
    }

    public void setRegisterShadowBC(int value) {
        regAltBC.setWord(value);
    }

    public int getRegisterShadowDE() {
        return regAltDE.word;
    }

    public void setRegisterShadowDE(int value) {
        regAltDE.setWord(value);
    }

    public int getRegisterShadowHL() {
        return regAltHL.word;
    }

    public void setRegisterShadowHL(int value) {
        regAltHL.setWord(value);
    }

//#endregion

//#region Execute

    public int ppc;
    public int op;

    private static class Context {

        int displacement;

        boolean tBool;
        int tb; // byte
        int tbh; // byte
        int tbl; // byte
        int tb1; // byte
        int tb2; // byte
        int tsb; // byte
        int tus; // short
        int ti1; // int
        int ti2; // int
        int tir; // int

        int incAndGetTB() {
            tb = inc8(tb);
            return tb;
        }

        int decAndGetTB() {
            tb = dec8(tb);
            return tb;
        }

        int getAndIncTUS() {
            try {
                return tus;
            } finally {
                tus = inc16(tus);
            }
        }

        int getAndDecTUS() {
            try {
                return tus;
            } finally {
                tus = dec16(tus);
            }
        }
    }

    /**
     * Runs the CPU for a particular number of clock cycles.
     *
     * @param cycles The number of cycles to run the CPU emulator for. Specify -1 to run for a single instruction.
     */
    public int executeCycles(int cycles) {
        fireListener(this);

        pendingCycles = cycles;

        Context c = new Context();

        // Process interrupt requests.
        if (nonMaskableInterruptPending) {
            if (halted) {
                halted = false;
                regPC.incAndGetWord();
            }
            totalExecutedCycles += 11;
            pendingCycles -= 11;
            nonMaskableInterruptPending = false;
//            iff2 = iff1;
            iff1 = false;
            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
            setRegisterPC(0x66);
            setRegisterWZ(regPC.word);
            NMICallback.run();
        }
        do {
            if (interrupt && iff1 && interruptTable) { //Z80.irq_state iff1 !Z80.after_ei
                if (halted) {
                    halted = false;
                    regPC.incAndGetWord();
                }
                iff1 = iff2 = false;
                int irq_vector = IRQCallback.getAsInt();
                switch (interruptMode) {
                    case 0:
                        switch (irq_vector & 0xff_0000) {
                            case 0xcd_0000:
                                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                                setRegisterPC(irq_vector & 0xffff);
                                totalExecutedCycles += 19;
                                pendingCycles -= 19;
                                break;
                            case 0xc3_0000:
                                setRegisterPC(irq_vector & 0xffff);
                                totalExecutedCycles += 12;
                                pendingCycles -= 12;
                                break;
                            default:
                                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                                setRegisterPC(irq_vector & 0x0038);
                                totalExecutedCycles += cc_op[regPC.low] + cc_ex[regPC.low];
                                pendingCycles -= cc_op[regPC.low] + cc_ex[regPC.low];
                                break;
                        }
                        break;
                    case 1:
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x38);
                        totalExecutedCycles += 13;
                        pendingCycles -= 13;
                        break;
                    case 2:
                        c.tus = add16((regI & 0xff) * 256, irq_vector);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        regPC.setLow(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        regPC.setHigh(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 17;
                        pendingCycles -= 17;
                        break;
                }
                setRegisterWZ(regPC.word);
            }

            {
                interruptTable = true;

                regR = inc7(regR);

                ppc = regPC.word;
                op = readOp.read((short) ppc) & 0xff;
                regPC.incAndGetWord();

                debugger_start_cpu_hook_callback.run();
                switch (op) { //readMemory(regPC.getAndIncWord()))
                    case 0x00: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x01: // LD BC, nn
                        setRegisterBC((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x02: // LD (BC), A
                        writeMemory.write((short) regBC.word, (byte) regAF.high);
                        regWZ.setLow(add16(regBC.word, 1) & 0xFF);
                        regWZ.setHigh(regAF.high);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x03: // INC BC
                        regBC.incAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x04: // INC B
                        setRegisterF(tableInc[regBC.incAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x05: // DEC B
                        setRegisterF(tableDec[regBC.decAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x06: // LD B, n
                        setRegisterB(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x07: // RLCA
                        setRegisterAF(tableRotShift[0][0][regAF.word]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x08: // EX AF, AF'
                        c.tus = regAF.word;
                        setRegisterAF(regAltAF.word);
                        setRegisterShadowAF(c.tus);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x09: // ADD HL, BC
                        c.ti1 = regHL.word;
                        c.ti2 = regBC.word;
                        c.tir = c.ti1 + c.ti2;
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(c.tir > 0xffff);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x0A: // LD A, (BC)
                        setRegisterA(readMemory.read((short) regBC.word) & 0xff);
                        setRegisterWZ(add16(regBC.word, 1));
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x0B: // DEC BC
                        regBC.decAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x0C: // INC C
                        setRegisterF(tableInc[regBC.incAndGetLow()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0D: // DEC C
                        setRegisterF(tableDec[regBC.decAndGetLow()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0E: // LD C, n
                        setRegisterC(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x0F: // RRCA
                        setRegisterAF(tableRotShift[0][1][regAF.word]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x10: // DJNZ d
                        c.tsb = readOpArg.read((short) regPC.getAndIncWord());
                        if ((regBC.decAndGetHigh()) != 0) {
                            setRegisterPC(regPC.addWord(c.tsb) & 0xffff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 13;
                            pendingCycles -= 13;
                        } else {
                            totalExecutedCycles += 8;
                            pendingCycles -= 8;
                        }
                        break;
                    case 0x11: // LD DE, nn
                        setRegisterDE((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x12: // LD (DE), A
                        writeMemory.write((short) regDE.word, (byte) regAF.high);
                        regWZ.setLow(add16(regDE.word, 1) & 0xff);
                        regWZ.setHigh(regAF.high);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x13: // INC DE
                        regDE.incAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x14: // INC D
                        setRegisterF(tableInc[regDE.incAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x15: // DEC D
                        setRegisterF(tableDec[regDE.decAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x16: // LD D, n
                        setRegisterD(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x17: // RLA
                        setRegisterAF(tableRotShift[0][2][regAF.word]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x18: // JR d
                        c.tsb = readOpArg.read((short) regPC.getAndIncWord());
                        setRegisterPC(regPC.addWord(c.tsb));
                        setRegisterWZ(regPC.word);
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        break;
                    case 0x19: // ADD HL, DE
                        c.ti1 = regHL.word;
                        c.ti2 = regDE.word;
                        c.tir = c.ti1 + c.ti2;
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(c.tir > 0xffff);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x1A: // LD A, (DE)
                        setRegisterA(readMemory.read((short) regDE.word) & 0xff);
                        setRegisterWZ(add16(regDE.word, 1));
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x1B: // DEC DE
                        regDE.decAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x1C: // INC E
                        setRegisterF(tableInc[regDE.incAndGetLow()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1D: // DEC E
                        setRegisterF(tableDec[regDE.decAndGetLow()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1E: // LD E, n
                        setRegisterE(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x1F: // RRA
                        setRegisterAF(tableRotShift[0][3][regAF.word]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x20: // JR NZ, d
                        c.tsb = readOpArg.read((short) regPC.getAndIncWord());
                        if (!getRegFlagZ()) {
                            setRegisterPC(regPC.addWord(c.tsb));
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x21: // LD HL, nn
                        setRegisterHL((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x22: // LD (nn), HL
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regHL.low);
                        writeMemory.write((short) c.tus, (byte) regHL.high);
                        setRegisterWZ(c.tus);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x23: // INC HL
                        regHL.incAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x24: // INC H
                        setRegisterF(tableInc[regHL.incAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x25: // DEC H
                        setRegisterF(tableDec[regHL.decAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x26: // LD H, n
                        setRegisterH(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x27: // DAA
                        setRegisterAF(TableDaa[regAF.word]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x28: // JR Z, d
                        c.tsb = readOpArg.read((short) regPC.getAndIncWord());
                        if (getRegFlagZ()) {
                            setRegisterPC(regPC.addWord(c.tsb));
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x29: // ADD HL, HL
                        c.ti1 = regHL.word;
                        c.ti2 = regHL.word;
                        c.tir = c.ti1 + c.ti2;
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(c.tir > 0xffff);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x2A: // LD HL, (nn)
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterL(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterH(readMemory.read((short) c.tus) & 0xff);
                        setRegisterWZ(c.tus);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x2B: // DEC HL
                        regHL.decAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x2C: // INC L
                        setRegisterF(tableInc[regHL.incAndGetLow()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2D: // DEC L
                        setRegisterF(tableDec[regHL.decAndGetLow()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2E: // LD L, n
                        setRegisterL(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x2F: // CPL
                        regAF.setHighXor(0xff);
                        setRegFlagH(true);
                        setRegFlagN(true);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x30: // JR NC, d
                        c.tsb = readOpArg.read((short) regPC.getAndIncWord());
                        if (!getRegFlagC()) {
                            setRegisterPC(regPC.addWord(c.tsb));
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x31: // LD SP, nn
                        setRegisterSP((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x32: // LD (nn), A
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.tus, (byte) regAF.high);
                        regWZ.setLow(add16(c.tus, 1) & 0xFF);
                        regWZ.setHigh(regAF.high);
                        totalExecutedCycles += 13;
                        pendingCycles -= 13;
                        break;
                    case 0x33: // INC SP
                        regSP.incAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x34: // INC (HL)
                        c.tb = readMemory.read((short) regHL.word) & 0xff;
                        setRegisterF(tableInc[c.incAndGetTB()] | (regAF.low & 1));
                        writeMemory.write((short) regHL.word, (byte) c.tb);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x35: // DEC (HL)
                        c.tb = readMemory.read((short) regHL.word) & 0xff;
                        setRegisterF(tableDec[c.decAndGetTB()] | (regAF.low & 1));
                        writeMemory.write((short) regHL.word, (byte) c.tb);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x36: // LD (HL), n
                        writeMemory.write((short) regHL.word, readOpArg.read((short) regPC.getAndIncWord()));
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0x37: // SCF
                        setRegFlagH(false);
                        setRegFlagN(false);
                        setRegFlagC(true);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x38: // JR C, d
                        c.tsb = readOpArg.read((short) regPC.getAndIncWord());
                        if (getRegFlagC()) {
                            setRegisterPC(regPC.addWord(c.tsb));
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 12;
                            pendingCycles -= 12;
                        } else {
                            totalExecutedCycles += 7;
                            pendingCycles -= 7;
                        }
                        break;
                    case 0x39: // ADD HL, SP
                        c.ti1 = regHL.word;
                        c.ti2 = regSP.word;
                        c.tir = c.ti1 + c.ti2;
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0x3A: // LD A, (nn)
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterA(readMemory.read((short) c.tus) & 0xff);
                        setRegisterWZ(add16(c.tus, 1));
                        totalExecutedCycles += 13;
                        pendingCycles -= 13;//
                        break;
                    case 0x3B: // DEC SP
                        regSP.decAndGetWord();
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0x3C: // INC A
                        setRegisterF(tableInc[regAF.incAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3D: // DEC A
                        setRegisterF(tableDec[regAF.decAndGetHigh()] | (regAF.low & 1));
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3E: // LD A, n
                        setRegisterA(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x3F: // CCF
                        setRegFlagH(getRegFlagC());
                        setRegFlagN(false);
                        setRegFlagC(getRegFlagC() ^ true);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x40: // LD B, B
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x41: // LD B, C
                        setRegisterB(regBC.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x42: // LD B, D
                        setRegisterB(regDE.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x43: // LD B, E
                        setRegisterB(regDE.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x44: // LD B, H
                        setRegisterB(regHL.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x45: // LD B, L
                        setRegisterB(regHL.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x46: // LD B, (HL)
                        setRegisterB(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x47: // LD B, A
                        setRegisterB(regAF.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x48: // LD C, B
                        setRegisterC(regBC.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x49: // LD C, C
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4A: // LD C, D
                        setRegisterC(regDE.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4B: // LD C, E
                        setRegisterC(regDE.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4C: // LD C, H
                        setRegisterC(regHL.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4D: // LD C, L
                        setRegisterC(regHL.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x4E: // LD C, (HL)
                        setRegisterC(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x4F: // LD C, A
                        setRegisterC(regAF.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x50: // LD D, B
                        setRegisterD(regBC.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x51: // LD D, C
                        setRegisterD(regBC.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x52: // LD D, D
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x53: // LD D, E
                        setRegisterD(regDE.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x54: // LD D, H
                        setRegisterD(regHL.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x55: // LD D, L
                        setRegisterD(regHL.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x56: // LD D, (HL)
                        setRegisterD(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x57: // LD D, A
                        setRegisterD(regAF.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x58: // LD E, B
                        setRegisterE(regBC.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x59: // LD E, C
                        setRegisterE(regBC.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5A: // LD E, D
                        setRegisterE(regDE.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5B: // LD E, E
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5C: // LD E, H
                        setRegisterE(regHL.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5D: // LD E, L
                        setRegisterE(regHL.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x5E: // LD E, (HL)
                        setRegisterE(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x5F: // LD E, A
                        setRegisterE(regAF.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x60: // LD H, B
                        setRegisterH(regBC.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x61: // LD H, C
                        setRegisterH(regBC.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x62: // LD H, D
                        setRegisterH(regDE.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x63: // LD H, E
                        setRegisterH(regDE.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x64: // LD H, H
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x65: // LD H, L
                        setRegisterH(regHL.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x66: // LD H, (HL)
                        setRegisterH(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x67: // LD H, A
                        setRegisterH(regAF.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x68: // LD L, B
                        setRegisterL(regBC.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x69: // LD L, C
                        setRegisterL(regBC.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6A: // LD L, D
                        setRegisterL(regDE.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6B: // LD L, E
                        setRegisterL(regDE.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6C: // LD L, H
                        setRegisterL(regHL.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6D: // LD L, L
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x6E: // LD L, (HL)
                        setRegisterL(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x6F: // LD L, A
                        setRegisterL(regAF.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x70: // LD (HL), B
                        writeMemory.write((short) regHL.word, (byte) regBC.high);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x71: // LD (HL), C
                        writeMemory.write((short) regHL.word, (byte) regBC.low);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x72: // LD (HL), D
                        writeMemory.write((short) regHL.word, (byte) regDE.high);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x73: // LD (HL), E
                        writeMemory.write((short) regHL.word, (byte) regDE.low);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x74: // LD (HL), H
                        writeMemory.write((short) regHL.word, (byte) regHL.high);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x75: // LD (HL), L
                        writeMemory.write((short) regHL.word, (byte) regHL.low);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x76: // HALT
                        Halt();
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x77: // LD (HL), A
                        writeMemory.write((short) regHL.word, (byte) regAF.high);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x78: // LD A, B
                        setRegisterA(regBC.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x79: // LD A, C
                        setRegisterA(regBC.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7A: // LD A, D
                        setRegisterA(regDE.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7B: // LD A, E
                        setRegisterA(regDE.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7C: // LD A, H
                        setRegisterA(regHL.high);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7D: // LD A, L
                        setRegisterA(regHL.low);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x7E: // LD A, (HL)
                        setRegisterA(readMemory.read((short) regHL.word) & 0xff);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x7F: // LD A, A
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x80: // ADD A, B
                        setRegisterAF(tableALU[0][regAF.high][regBC.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x81: // ADD A, C
                        setRegisterAF(tableALU[0][regAF.high][regBC.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x82: // ADD A, D
                        setRegisterAF(tableALU[0][regAF.high][regDE.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x83: // ADD A, E
                        setRegisterAF(tableALU[0][regAF.high][regDE.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x84: // ADD A, H
                        setRegisterAF(tableALU[0][regAF.high][regHL.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x85: // ADD A, L
                        setRegisterAF(tableALU[0][regAF.high][regHL.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x86: // ADD A, (HL)
                        setRegisterAF(tableALU[0][regAF.high][readMemory.read((short) regHL.word) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x87: // ADD A, A
                        setRegisterAF(tableALU[0][regAF.high][regAF.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x88: // ADC A, B
                        setRegisterAF(tableALU[1][regAF.high][regBC.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x89: // ADC A, C
                        setRegisterAF(tableALU[1][regAF.high][regBC.low][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8A: // ADC A, D
                        setRegisterAF(tableALU[1][regAF.high][regDE.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8B: // ADC A, E
                        setRegisterAF(tableALU[1][regAF.high][regDE.low][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8C: // ADC A, H
                        setRegisterAF(tableALU[1][regAF.high][regHL.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8D: // ADC A, L
                        setRegisterAF(tableALU[1][regAF.high][regHL.low][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8E: // ADC A, (HL)
                        setRegisterAF(tableALU[1][regAF.high][readMemory.read((short) regHL.word) & 0xff][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x8F: // ADC A, A
                        setRegisterAF(tableALU[1][regAF.high][regAF.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x90: // SUB B
                        setRegisterAF(tableALU[2][regAF.high][regBC.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x91: // SUB C
                        setRegisterAF(tableALU[2][regAF.high][regBC.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x92: // SUB D
                        setRegisterAF(tableALU[2][regAF.high][regDE.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x93: // SUB E
                        setRegisterAF(tableALU[2][regAF.high][regDE.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x94: // SUB H
                        setRegisterAF(tableALU[2][regAF.high][regHL.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x95: // SUB L
                        setRegisterAF(tableALU[2][regAF.high][regHL.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x96: // SUB (HL)
                        setRegisterAF(tableALU[2][regAF.high][readMemory.read((short) regHL.word) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x97: // SUB A, A
                        setRegisterAF(tableALU[2][regAF.high][regAF.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x98: // SBC A, B
                        setRegisterAF(tableALU[3][regAF.high][regBC.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x99: // SBC A, C
                        setRegisterAF(tableALU[3][regAF.high][regBC.low][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9A: // SBC A, D
                        setRegisterAF(tableALU[3][regAF.high][regDE.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9B: // SBC A, E
                        setRegisterAF(tableALU[3][regAF.high][regDE.low][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9C: // SBC A, H
                        setRegisterAF(tableALU[3][regAF.high][regHL.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9D: // SBC A, L
                        setRegisterAF(tableALU[3][regAF.high][regHL.low][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9E: // SBC A, (HL)
                        setRegisterAF(tableALU[3][regAF.high][readMemory.read((short) regHL.word) & 0xff][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0x9F: // SBC A, A
                        setRegisterAF(tableALU[3][regAF.high][regAF.high][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA0: // AND B
                        setRegisterAF(tableALU[4][regAF.high][regBC.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA1: // AND C
                        setRegisterAF(tableALU[4][regAF.high][regBC.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA2: // AND D
                        setRegisterAF(tableALU[4][regAF.high][regDE.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA3: // AND E
                        setRegisterAF(tableALU[4][regAF.high][regDE.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA4: // AND H
                        setRegisterAF(tableALU[4][regAF.high][regHL.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA5: // AND L
                        setRegisterAF(tableALU[4][regAF.high][regHL.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA6: // AND (HL)
                        setRegisterAF(tableALU[4][regAF.high][readMemory.read((short) regHL.word) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xA7: // AND A
                        setRegisterAF(tableALU[4][regAF.high][regAF.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA8: // XOR B
                        setRegisterAF(tableALU[5][regAF.high][regBC.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA9: // XOR C
                        setRegisterAF(tableALU[5][regAF.high][regBC.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAA: // XOR D
                        setRegisterAF(tableALU[5][regAF.high][regDE.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAB: // XOR E
                        setRegisterAF(tableALU[5][regAF.high][regDE.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAC: // XOR H
                        setRegisterAF(tableALU[5][regAF.high][regHL.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAD: // XOR L
                        setRegisterAF(tableALU[5][regAF.high][regHL.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAE: // XOR (HL)
                        setRegisterAF(tableALU[5][regAF.high][readMemory.read((short) regHL.word) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xAF: // XOR A
                        setRegisterAF(tableALU[5][regAF.high][regAF.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB0: // OR B
                        setRegisterAF(tableALU[6][regAF.high][regBC.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB1: // OR C
                        setRegisterAF(tableALU[6][regAF.high][regBC.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB2: // OR D
                        setRegisterAF(tableALU[6][regAF.high][regDE.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB3: // OR E
                        setRegisterAF(tableALU[6][regAF.high][regDE.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB4: // OR H
                        setRegisterAF(tableALU[6][regAF.high][regHL.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB5: // OR L
                        setRegisterAF(tableALU[6][regAF.high][regHL.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB6: // OR (HL)
                        setRegisterAF(tableALU[6][regAF.high][readMemory.read((short) regHL.word) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xB7: // OR A
                        setRegisterAF(tableALU[6][regAF.high][regAF.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB8: // CP B
                        setRegisterAF(tableALU[7][regAF.high][regBC.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB9: // CP C
                        setRegisterAF(tableALU[7][regAF.high][regBC.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBA: // CP D
                        setRegisterAF(tableALU[7][regAF.high][regDE.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBB: // CP E
                        setRegisterAF(tableALU[7][regAF.high][regDE.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBC: // CP H
                        setRegisterAF(tableALU[7][regAF.high][regHL.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBD: // CP L
                        setRegisterAF(tableALU[7][regAF.high][regHL.low][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBE: // CP (HL)
                        setRegisterAF(tableALU[7][regAF.high][readMemory.read((short) regHL.word) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xBF: // CP A
                        setRegisterAF(tableALU[7][regAF.high][regAF.high][0]);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC0: // RET NZ
                        if (!getRegFlagZ()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xC1: // POP BC
                        setRegisterC(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterB(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xC2: // JP NZ, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagZ()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xC3: // JP nn
                        setRegisterPC((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        setRegisterWZ(regPC.word);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xC4: // CALL NZ, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagZ()) {
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xC5: // PUSH BC
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regBC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regBC.low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xC6: // ADD A, n
                        setRegisterAF(tableALU[0][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xC7: // RST $00
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x00);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xC8: // RET Z
                        if (getRegFlagZ()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xC9: // RET
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterWZ(regPC.word);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xCA: // JP Z, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagZ()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xCB: // (Prefix)
                        regR = inc7(regR);
                        processCB(c);
                        break;
                    case 0xCC: // CALL Z, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagZ()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xCD: // CALL nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(regWZ.word);
                        totalExecutedCycles += 17;
                        pendingCycles -= 17;
                        break;
                    case 0xCE: // ADC A, n
                        setRegisterAF(tableALU[1][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xCF: // RST $08
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x08);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD0: // RET NC
                        if (!getRegFlagC()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xD1: // POP DE
                        setRegisterE(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterD(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xD2: // JP NC, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagC()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xD3: // OUT n, A
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) | ((regAF.high & 0xff) << 8);
                        writeHardware.write((short) c.tus, (byte) regAF.high);
                        regWZ.setLow(inc8(c.tus));
                        regWZ.setHigh(regAF.high);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD4: // CALL NC, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagC()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xD5: // PUSH DE
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regDE.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regDE.low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD6: // SUB n
                        setRegisterAF(tableALU[2][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xD7: // RST $10
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x10);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xD8: // RET C
                        if (getRegFlagC()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xD9: // EXX
                        c.tus = regBC.word;
                        setRegisterBC(regAltBC.word);
                        setRegisterShadowBC(c.tus);
                        c.tus = regDE.word;
                        setRegisterDE(regAltDE.word);
                        setRegisterShadowDE(c.tus);
                        c.tus = regHL.word;
                        setRegisterHL(regAltHL.word);
                        setRegisterShadowHL(c.tus);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDA: // JP C, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagC()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xDB: // IN A, n
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) | ((regAF.high & 0xff) << 8);
                        setRegisterA(readHardware.read((short) c.tus) & 0xff);
                        setRegisterWZ(add16(c.tus, 1));
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xDC: // CALL C, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagC()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xDD: // (Prefix)
                        regR = inc7(regR);
                        processDD(c);
                        break;
                    case 0xDE: // SBC A, n
                        setRegisterAF(tableALU[3][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][getRegFlagC() ? 1 : 0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xDF: // RST $18
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x18);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xE0: // RET PO
                        if (!getRegFlagP()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xE1: // POP HL
                        setRegisterL(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterH(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xE2: // JP PO, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagP()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xE3: // EX (SP), HL
                        c.tus = regSP.word;
                        c.tbl = readMemory.read((short) c.getAndIncTUS()) & 0xff;
                        c.tbh = readMemory.read((short) c.getAndDecTUS()) & 0xff;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regHL.low);
                        writeMemory.write((short) c.tus, (byte) regHL.high);
                        setRegisterL(c.tbl);
                        setRegisterH(c.tbh);
                        setRegisterWZ(regHL.word);
                        totalExecutedCycles += 19;
                        pendingCycles -= 19;
                        break;
                    case 0xE4: // CALL C, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagC()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xE5: // PUSH HL
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regHL.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regHL.low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xE6: // AND n
                        setRegisterAF(tableALU[4][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xE7: // RST $20
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x20);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xE8: // RET PE
                        if (getRegFlagP()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xE9: // JP HL
                        setRegisterPC(regHL.word);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEA: // JP PE, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagP()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xEB: // EX DE, HL
                        c.tus = regDE.word;
                        setRegisterDE(regHL.word);
                        setRegisterHL(c.tus);
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEC: // CALL PE, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagP()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xED: // (Prefix)
                        regR = inc7(regR);
                        processED(c);
                        break;
                    case 0xEE: // XOR n
                        setRegisterAF(tableALU[5][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xEF: // RST $28
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x28);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xF0: // RET P
                        if (!getRegFlagS()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xF1: // POP AF
                        setRegisterF(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterA(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xF2: // JP P, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagS()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xF3: // DI
                        iff1 = iff2 = false;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF4: // CALL P, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (!getRegFlagS()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xF5: // PUSH AF
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regAF.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regAF.low);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xF6: // OR n
                        setRegisterAF(tableALU[6][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xF7: // RST $30
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x30);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                    case 0xF8: // RET M
                        if (getRegFlagS()) {
                            regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                            setRegisterWZ(regPC.word);
                            totalExecutedCycles += 11;
                            pendingCycles -= 11;
                        } else {
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xF9: // LD SP, HL
                        setRegisterSP(regHL.word);
                        totalExecutedCycles += 6;
                        pendingCycles -= 6;
                        break;
                    case 0xFA: // JP M, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagS()) {
                            setRegisterPC(regWZ.word);
                        }
                        totalExecutedCycles += 10;
                        pendingCycles -= 10;
                        break;
                    case 0xFB: // EI
                        iff1 = iff2 = true;
                        interruptTable = false;
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFC: // CALL M, nn
                        setRegisterWZ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                        if (getRegFlagS()) {
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                            writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                            setRegisterPC(regWZ.word);
                            totalExecutedCycles += 17;
                            pendingCycles -= 17;
                        } else {
                            totalExecutedCycles += 10;
                            pendingCycles -= 10;
                        }
                        break;
                    case 0xFD: // (Prefix)
                        regR = inc7(regR);
                        processFD(c);
                        break;
                    case 0xFE: // CP n
                        setRegisterAF(tableALU[7][regAF.high][readOpArg.read((short) regPC.getAndIncWord()) & 0xff][0]);
                        totalExecutedCycles += 7;
                        pendingCycles -= 7;
                        break;
                    case 0xFF: // RST $38
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                        writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                        setRegisterPC(0x38);
                        totalExecutedCycles += 11;
                        pendingCycles -= 11;
                        break;
                }
                debugger_stop_cpu_hook_callback.run();
            }
        }
        while (pendingCycles > 0);
        return cycles - pendingCycles;
    }

    private void processCB(Context c) {
        switch (readOp.read((short) regPC.getAndIncWord()) & 0xff) {
            case 0x00: // RLC B
                c.tus = tableRotShift[1][0][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x01: // RLC C
                c.tus = tableRotShift[1][0][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x02: // RLC D
                c.tus = tableRotShift[1][0][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x03: // RLC E
                c.tus = tableRotShift[1][0][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x04: // RLC H
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                c.tus = tableRotShift[1][0][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                break;
            case 0x05: // RLC L
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                c.tus = tableRotShift[1][0][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                break;
            case 0x06: // RLC (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x07: // RLC A
                c.tus = tableRotShift[1][0][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x08: // RRC B
                c.tus = tableRotShift[1][1][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x09: // RRC C
                c.tus = tableRotShift[1][1][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0A: // RRC D
                c.tus = tableRotShift[1][1][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0B: // RRC E
                c.tus = tableRotShift[1][1][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0C: // RRC H
                c.tus = tableRotShift[1][1][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0D: // RRC L
                c.tus = tableRotShift[1][1][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x0E: // RRC (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0F: // RRC A
                c.tus = tableRotShift[1][1][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x10: // RL B
                c.tus = tableRotShift[1][2][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x11: // RL C
                c.tus = tableRotShift[1][2][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x12: // RL D
                c.tus = tableRotShift[1][2][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x13: // RL E
                c.tus = tableRotShift[1][2][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x14: // RL H
                c.tus = tableRotShift[1][2][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x15: // RL L
                c.tus = tableRotShift[1][2][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x16: // RL (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x17: // RL A
                c.tus = tableRotShift[1][2][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x18: // RR B
                c.tus = tableRotShift[1][3][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x19: // RR C
                c.tus = tableRotShift[1][3][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1A: // RR D
                c.tus = tableRotShift[1][3][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1B: // RR E
                c.tus = tableRotShift[1][3][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1C: // RR H
                c.tus = tableRotShift[1][3][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1D: // RR L
                c.tus = tableRotShift[1][3][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x1E: // RR (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1F: // RR A
                c.tus = tableRotShift[1][3][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x20: // SLA B
                c.tus = tableRotShift[1][4][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x21: // SLA C
                c.tus = tableRotShift[1][4][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x22: // SLA D
                c.tus = tableRotShift[1][4][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x23: // SLA E
                c.tus = tableRotShift[1][4][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x24: // SLA H
                c.tus = tableRotShift[1][4][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x25: // SLA L
                c.tus = tableRotShift[1][4][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x26: // SLA (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x27: // SLA A
                c.tus = tableRotShift[1][4][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x28: // SRA B
                c.tus = tableRotShift[1][5][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x29: // SRA C
                c.tus = tableRotShift[1][5][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2A: // SRA D
                c.tus = tableRotShift[1][5][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2B: // SRA E
                c.tus = tableRotShift[1][5][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2C: // SRA H
                c.tus = tableRotShift[1][5][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2D: // SRA L
                c.tus = tableRotShift[1][5][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x2E: // SRA (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2F: // SRA A
                c.tus = tableRotShift[1][5][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x30: // SL1 B
                c.tus = tableRotShift[1][6][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x31: // SL1 C
                c.tus = tableRotShift[1][6][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x32: // SL1 D
                c.tus = tableRotShift[1][6][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x33: // SL1 E
                c.tus = tableRotShift[1][6][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x34: // SL1 H
                c.tus = tableRotShift[1][6][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x35: // SL1 L
                c.tus = tableRotShift[1][6][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x36: // SL1 (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x37: // SL1 A
                c.tus = tableRotShift[1][6][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x38: // SRL B
                c.tus = tableRotShift[1][7][regAF.low + 256 * regBC.high];
                setRegisterB((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x39: // SRL C
                c.tus = tableRotShift[1][7][regAF.low + 256 * regBC.low];
                setRegisterC((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3A: // SRL D
                c.tus = tableRotShift[1][7][regAF.low + 256 * regDE.high];
                setRegisterD((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3B: // SRL E
                c.tus = tableRotShift[1][7][regAF.low + 256 * regDE.low];
                setRegisterE((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3C: // SRL H
                c.tus = tableRotShift[1][7][regAF.low + 256 * regHL.high];
                setRegisterH((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3D: // SRL L
                c.tus = tableRotShift[1][7][regAF.low + 256 * regHL.low];
                setRegisterL((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x3E: // SRL (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regHL.word) & 0xff)];
                writeMemory.write((short) regHL.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3F: // SRL A
                c.tus = tableRotShift[1][7][regAF.low + 256 * regAF.high];
                setRegisterA((c.tus >>> 8) & 0xff);
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x40: // BIT 0, B
                setRegFlagZ((regBC.high & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x41: // BIT 0, C
                setRegFlagZ((regBC.low & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x42: // BIT 0, D
                setRegFlagZ((regDE.high & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x43: // BIT 0, E
                setRegFlagZ((regDE.low & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x44: // BIT 0, H
                setRegFlagZ((regHL.high & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x45: // BIT 0, L
                setRegFlagZ((regHL.low & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x46: // BIT 0, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x47: // BIT 0, A
                setRegFlagZ((regAF.high & 0x01) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x48: // BIT 1, B
                setRegFlagZ((regBC.high & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x49: // BIT 1, C
                setRegFlagZ((regBC.low & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4A: // BIT 1, D
                setRegFlagZ((regDE.high & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4B: // BIT 1, E
                setRegFlagZ((regDE.low & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4C: // BIT 1, H
                setRegFlagZ((regHL.high & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4D: // BIT 1, L
                setRegFlagZ((regHL.low & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4E: // BIT 1, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x4F: // BIT 1, A
                setRegFlagZ((regAF.high & 0x02) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x50: // BIT 2, B
                setRegFlagZ((regBC.high & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x51: // BIT 2, C
                setRegFlagZ((regBC.low & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x52: // BIT 2, D
                setRegFlagZ((regDE.high & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x53: // BIT 2, E
                setRegFlagZ((regDE.low & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x54: // BIT 2, H
                setRegFlagZ((regHL.high & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x55: // BIT 2, L
                setRegFlagZ((regHL.low & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x56: // BIT 2, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x57: // BIT 2, A
                setRegFlagZ((regAF.high & 0x04) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x58: // BIT 3, B
                setRegFlagZ((regBC.high & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x59: // BIT 3, C
                setRegFlagZ((regBC.low & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5A: // BIT 3, D
                setRegFlagZ((regDE.high & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5B: // BIT 3, E
                setRegFlagZ((regDE.low & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5C: // BIT 3, H
                setRegFlagZ((regHL.high & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5D: // BIT 3, L
                setRegFlagZ((regHL.low & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5E: // BIT 3, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x5F: // BIT 3, A
                setRegFlagZ((regAF.high & 0x08) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(!getRegFlagZ());
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x60: // BIT 4, B
                setRegFlagZ((regBC.high & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x61: // BIT 4, C
                setRegFlagZ((regBC.low & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x62: // BIT 4, D
                setRegFlagZ((regDE.high & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x63: // BIT 4, E
                setRegFlagZ((regDE.low & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x64: // BIT 4, H
                setRegFlagZ((regHL.high & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x65: // BIT 4, L
                setRegFlagZ((regHL.low & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x66: // BIT 4, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x67: // BIT 4, A
                setRegFlagZ((regAF.high & 0x10) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x68: // BIT 5, B
                setRegFlagZ((regBC.high & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x69: // BIT 5, C
                setRegFlagZ((regBC.low & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6A: // BIT 5, D
                setRegFlagZ((regDE.high & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6B: // BIT 5, E
                setRegFlagZ((regDE.low & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6C: // BIT 5, H
                setRegFlagZ((regHL.high & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6D: // BIT 5, L
                setRegFlagZ((regHL.low & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6E: // BIT 5, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x6F: // BIT 5, A
                setRegFlagZ((regAF.high & 0x20) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(!getRegFlagZ());
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x70: // BIT 6, B
                setRegFlagZ((regBC.high & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x71: // BIT 6, C
                setRegFlagZ((regBC.low & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x72: // BIT 6, D
                setRegFlagZ((regDE.high & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x73: // BIT 6, E
                setRegFlagZ((regDE.low & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x74: // BIT 6, H
                setRegFlagZ((regHL.high & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x75: // BIT 6, L
                setRegFlagZ((regHL.low & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x76: // BIT 6, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x77: // BIT 6, A
                setRegFlagZ((regAF.high & 0x40) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(false);
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x78: // BIT 7, B
                setRegFlagZ((regBC.high & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x79: // BIT 7, C
                setRegFlagZ((regBC.low & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7A: // BIT 7, D
                setRegFlagZ((regDE.high & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7B: // BIT 7, E
                setRegFlagZ((regDE.low & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7C: // BIT 7, H
                setRegFlagZ((regHL.high & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7D: // BIT 7, L
                setRegFlagZ((regHL.low & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7E: // BIT 7, (HL)
                setRegFlagZ((readMemory.read((short) regHL.word) & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3((regWZ.high & 0x08) != 0);
                setRegFlag5((regWZ.high & 0x20) != 0);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x7F: // BIT 7, A
                setRegFlagZ((regAF.high & 0x80) == 0);
                setRegFlagP(getRegFlagZ());
                setRegFlagS(!getRegFlagZ());
                setRegFlag3(false);
                setRegFlag5(false);
                setRegFlagH(true);
                setRegFlagN(false);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x80: // RES 0, B
                regBC.setHighAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x81: // RES 0, C
                regBC.setLowAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x82: // RES 0, D
                regDE.setHighAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x83: // RES 0, E
                regDE.setLowAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x84: // RES 0, H
                regHL.setHighAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x85: // RES 0, L
                regHL.setLowAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x86: // RES 0, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x01));
                break;
            case 0x87: // RES 0, A
                regAF.setHighAnd(~0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x88: // RES 1, B
                regBC.setHighAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x89: // RES 1, C
                regBC.setLowAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8A: // RES 1, D
                regDE.setHighAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8B: // RES 1, E
                regDE.setLowAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8C: // RES 1, H
                regHL.setHighAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8D: // RES 1, L
                regHL.setLowAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8E: // RES 1, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x02));
                break;
            case 0x8F: // RES 1, A
                regAF.setHighAnd(~0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x90: // RES 2, B
                regBC.setHighAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x91: // RES 2, C
                regBC.setLowAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x92: // RES 2, D
                regDE.setHighAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x93: // RES 2, E
                regDE.setLowAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x94: // RES 2, H
                regHL.setHighAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x95: // RES 2, L
                regHL.setLowAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x96: // RES 2, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x04));
                break;
            case 0x97: // RES 2, A
                regAF.setHighAnd(~0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x98: // RES 3, B
                regBC.setHighAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x99: // RES 3, C
                regBC.setLowAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9A: // RES 3, D
                regDE.setHighAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9B: // RES 3, E
                regDE.setLowAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9C: // RES 3, H
                regHL.setHighAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9D: // RES 3, L
                regHL.setLowAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9E: // RES 3, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x08));
                break;
            case 0x9F: // RES 3, A
                regAF.setHighAnd(~0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA0: // RES 4, B
                regBC.setHighAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA1: // RES 4, C
                regBC.setLowAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA2: // RES 4, D
                regDE.setHighAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA3: // RES 4, E
                regDE.setLowAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA4: // RES 4, H
                regHL.setHighAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA5: // RES 4, L
                regHL.setLowAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA6: // RES 4, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x10));
                break;
            case 0xA7: // RES 4, A
                regAF.setHighAnd(~0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA8: // RES 5, B
                regBC.setHighAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA9: // RES 5, C
                regBC.setLowAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAA: // RES 5, D
                regDE.setHighAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAB: // RES 5, E
                regDE.setLowAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAC: // RES 5, H
                regHL.setHighAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAD: // RES 5, L
                regHL.setLowAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAE: // RES 5, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x20));
                break;
            case 0xAF: // RES 5, A
                regAF.setHighAnd(~0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB0: // RES 6, B
                regBC.setHighAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB1: // RES 6, C
                regBC.setLowAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB2: // RES 6, D
                regDE.setHighAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB3: // RES 6, E
                regDE.setLowAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB4: // RES 6, H
                regHL.setHighAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB5: // RES 6, L
                regHL.setLowAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB6: // RES 6, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x40));
                break;
            case 0xB7: // RES 6, A
                regAF.setHighAnd(~0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB8: // RES 7, B
                regBC.setHighAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB9: // RES 7, C
                regBC.setLowAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBA: // RES 7, D
                regDE.setHighAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBB: // RES 7, E
                regDE.setLowAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBC: // RES 7, H
                regHL.setHighAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBD: // RES 7, L
                regHL.setLowAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBE: // RES 7, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) & (byte) ~0x80));
                break;
            case 0xBF: // RES 7, A
                regAF.setHighAnd(~0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC0: // SET 0, B
                regBC.setHighOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC1: // SET 0, C
                regBC.setLowOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC2: // SET 0, D
                regDE.setHighOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC3: // SET 0, E
                regDE.setLowOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC4: // SET 0, H
                regHL.setHighOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC5: // SET 0, L
                regHL.setLowOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC6: // SET 0, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x01));
                break;
            case 0xC7: // SET 0, A
                regAF.setHighOr(0x01);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC8: // SET 1, B
                regBC.setHighOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xC9: // SET 1, C
                regBC.setLowOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCA: // SET 1, D
                regDE.setHighOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCB: // SET 1, E
                regDE.setLowOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCC: // SET 1, H
                regHL.setHighOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCD: // SET 1, L
                regHL.setLowOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xCE: // SET 1, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x02));
                break;
            case 0xCF: // SET 1, A
                regAF.setHighOr(0x02);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD0: // SET 2, B
                regBC.setHighOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD1: // SET 2, C
                regBC.setLowOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD2: // SET 2, D
                regDE.setHighOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD3: // SET 2, E
                regDE.setLowOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD4: // SET 2, H
                regHL.setHighOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD5: // SET 2, L
                regHL.setLowOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD6: // SET 2, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x04));
                break;
            case 0xD7: // SET 2, A
                regAF.setHighOr(0x04);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD8: // SET 3, B
                regBC.setHighOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xD9: // SET 3, C
                regBC.setLowOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDA: // SET 3, D
                regDE.setHighOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDB: // SET 3, E
                regDE.setLowOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDC: // SET 3, H
                regHL.setHighOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDD: // SET 3, L
                regHL.setLowOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xDE: // SET 3, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x08));
                break;
            case 0xDF: // SET 3, A
                regAF.setHighOr(0x08);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE0: // SET 4, B
                regBC.setHighOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE1: // SET 4, C
                regBC.setLowOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE2: // SET 4, D
                regDE.setHighOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE3: // SET 4, E
                regDE.setLowOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE4: // SET 4, H
                regHL.setHighOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE5: // SET 4, L
                regHL.setLowOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE6: // SET 4, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x10));
                break;
            case 0xE7: // SET 4, A
                regAF.setHighOr(0x10);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE8: // SET 5, B
                regBC.setHighOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xE9: // SET 5, C
                regBC.setLowOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEA: // SET 5, D
                regDE.setHighOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEB: // SET 5, E
                regDE.setLowOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEC: // SET 5, H
                regHL.setHighOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xED: // SET 5, L
                regHL.setLowOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEE: // SET 5, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x20));
                break;
            case 0xEF: // SET 5, A
                regAF.setHighOr(0x20);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF0: // SET 6, B
                regBC.setHighOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF1: // SET 6, C
                regBC.setLowOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF2: // SET 6, D
                regDE.setHighOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF3: // SET 6, E
                regDE.setLowOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF4: // SET 6, H
                regHL.setHighOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF5: // SET 6, L
                regHL.setLowOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF6: // SET 6, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x40));
                break;
            case 0xF7: // SET 6, A
                regAF.setHighOr(0x40);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF8: // SET 7, B
                regBC.setHighOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xF9: // SET 7, C
                regBC.setLowOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFA: // SET 7, D
                regDE.setHighOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFB: // SET 7, E
                regDE.setLowOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFC: // SET 7, H
                regHL.setHighOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFD: // SET 7, L
                regHL.setLowOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xFE: // SET 7, (HL)
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regHL.word, (byte) (readMemory.read((short) regHL.word) | (byte) 0x80));
                break;
            case 0xFF: // SET 7, A
                regAF.setHighOr(0x80);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
        }
    }

    private void processDD(Context c) {
        switch (readOp.read((short) regPC.getAndIncWord()) & 0xff) {
            case 0x00: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x01: // LD BC, nn
                setRegisterBC((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x02: // LD (BC), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                writeMemory.write((short) regBC.word, (byte) regAF.high);
                break;
            case 0x03: // INC BC
                regBC.incAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x04: // INC B
                setRegisterF(tableInc[regBC.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x05: // DEC B
                setRegisterF(tableDec[regBC.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x06: // LD B, n
                setRegisterB(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x07: // RLCA
                setRegisterAF(tableRotShift[0][0][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x08: // EX AF, AF'
                c.tus = regAF.word;
                setRegisterAF(regAltAF.word);
                setRegisterShadowAF(c.tus);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x09: // ADD IX, BC
                c.ti1 = regIX.word;
                c.ti2 = regBC.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIX.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIX(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x0A: // LD A, (BC)
                setRegisterA(readMemory.read((short) regBC.word) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x0B: // DEC BC
                regBC.decAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x0C: // INC C
                setRegisterF(tableInc[regBC.incAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0D: // DEC C
                setRegisterF(tableDec[regBC.decAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0E: // LD C, n
                setRegisterC(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x0F: // RRCA
                setRegisterAF(tableRotShift[0][1][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x10: // DJNZ d
                c.tsb = readMemory.read((short) regPC.getAndIncWord()) & 0xff;
                if ((regBC.decAndGetHigh()) != 0) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 13;
                    pendingCycles -= 13;
                } else {
                    totalExecutedCycles += 8;
                    pendingCycles -= 8;
                }
                break;
            case 0x11: // LD DE, nn
                setRegisterDE((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x12: // LD (DE), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                writeMemory.write((short) regDE.word, (byte) regAF.high);
                break;
            case 0x13: // INC DE
                regDE.incAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x14: // INC D
                setRegisterF(tableInc[regDE.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x15: // DEC D
                setRegisterF(tableDec[regDE.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x16: // LD D, n
                setRegisterD(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x17: // RLA
                setRegisterAF(tableRotShift[0][2][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x18: // JR d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                setRegisterPC(regPC.addWord(c.tsb));
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x19: // ADD IX, DE
                c.ti1 = regIX.word;
                c.ti2 = regDE.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIX.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIX(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x1A: // LD A, (DE)
                setRegisterA(readMemory.read((short) regDE.word) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1B: // DEC DE
                regDE.decAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x1C: // INC E
                setRegisterF(tableInc[regDE.incAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1D: // DEC E
                setRegisterF(tableDec[regDE.decAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1E: // LD E, n
                setRegisterE(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1F: // RRA
                setRegisterAF(tableRotShift[0][3][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x20: // JR NZ, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (!getRegFlagZ()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x21: // LD IX, nn
                setRegisterIX((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0x22: // LD (nn), IX
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regIX.low);
                writeMemory.write((short) c.tus, (byte) regIX.high);
                setRegisterWZ(c.tus);
                break;
            case 0x23: // INC IX
                regIX.incAndGetWord();
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x24: // INC IXH
                setRegisterF(tableInc[regIX.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x25: // DEC IXH
                setRegisterF(tableDec[regIX.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x26: // LD IXH, n
                regIX.setHigh(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                break;
            case 0x27: // DAA
                setRegisterAF(TableDaa[regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x28: // JR Z, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (getRegFlagZ()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x29: // ADD IX, IX
                c.ti1 = regIX.word;
                c.ti2 = regIX.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIX.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIX(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x2A: // LD IX, (nn)
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                regIX.setLow(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                regIX.setHigh(readMemory.read((short) c.tus) & 0xff);
                setRegisterWZ(c.tus);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x2B: // DEC IX
                regIX.decAndGetWord();
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x2C: // INC IXL
                setRegisterF(tableInc[regIX.incAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2D: // DEC IXL
                setRegisterF(tableDec[regIX.decAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2E: // LD IXL, n
                regIX.setLow(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                break;
            case 0x2F: // CPL
                regAF.setHighXor(0xFF);
                setRegFlagH(true);
                setRegFlagN(true);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x30: // JR NC, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (!getRegFlagC()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x31: // LD SP, nn
                setRegisterSP((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x32: // LD (nn), A
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                writeMemory.write((short) ((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256), (byte) regAF.high);
                break;
            case 0x33: // INC SP
                regSP.incAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x34: // INC (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                c.tb = readMemory.read((short) regWZ.word) & 0xff;
                setRegisterF(tableInc[c.incAndGetTB()] | (regAF.low & 1));
                writeMemory.write((short) regWZ.word, (byte) c.tb);
                break;
            case 0x35: // DEC (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                c.tb = readMemory.read((short) regWZ.word) & 0xff;
                setRegisterF(tableDec[c.decAndGetTB()] | (regAF.low & 1));
                writeMemory.write((short) regWZ.word, (byte) c.tb);
                break;
            case 0x36: // LD (IX+d), n
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, readOpArg.read((short) regPC.getAndIncWord()));
                break;
            case 0x37: // SCF
                setRegFlagH(false);
                setRegFlagN(false);
                setRegFlagC(true);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x38: // JR C, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (getRegFlagC()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x39: // ADD IX, SP
                c.ti1 = regIX.word;
                c.ti2 = regSP.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIX.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIX(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x3A: // LD A, (nn)
                setRegisterA(readMemory.read((short) ((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256)) & 0xff);
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                break;
            case 0x3B: // DEC SP
                regSP.decAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x3C: // INC A
                setRegisterF(tableInc[regAF.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3D: // DEC A
                setRegisterF(tableDec[regAF.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3E: // LD A, n
                setRegisterA(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x3F: // CCF
                setRegFlagH(getRegFlagC());
                setRegFlagN(false);
                setRegFlagC(getRegFlagC() ^ true);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x40: // LD B, B
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x41: // LD B, C
                setRegisterB(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x42: // LD B, D
                setRegisterB(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x43: // LD B, E
                setRegisterB(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x44: // LD B, IXH
                setRegisterB(regIX.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x45: // LD B, IXL
                setRegisterB(regIX.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x46: // LD B, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterB(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x47: // LD B, A
                setRegisterB(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x48: // LD C, B
                setRegisterC(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x49: // LD C, C
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4A: // LD C, D
                setRegisterC(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4B: // LD C, E
                setRegisterC(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4C: // LD C, IXH
                setRegisterC(regIX.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4D: // LD C, IXL
                setRegisterC(regIX.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4E: // LD C, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterC(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x4F: // LD C, A
                setRegisterC(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x50: // LD D, B
                setRegisterD(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x51: // LD D, C
                setRegisterD(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x52: // LD D, D
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x53: // LD D, E
                setRegisterD(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x54: // LD D, IXH
                setRegisterD(regIX.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x55: // LD D, IXL
                setRegisterD(regIX.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x56: // LD D, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterD(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x57: // LD D, A
                setRegisterD(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x58: // LD E, B
                setRegisterE(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x59: // LD E, C
                setRegisterE(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5A: // LD E, D
                setRegisterE(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5B: // LD E, E
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5C: // LD E, IXH
                setRegisterE(regIX.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5D: // LD E, IXL
                setRegisterE(regIX.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5E: // LD E, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterE(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x5F: // LD E, A
                setRegisterE(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x60: // LD IXH, B
                regIX.setHigh(regBC.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x61: // LD IXH, C
                regIX.setHigh(regBC.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x62: // LD IXH, D
                regIX.setHigh(regDE.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x63: // LD IXH, E
                regIX.setHigh(regDE.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x64: // LD IXH, IXH
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x65: // LD IXH, IXL
                regIX.setHigh(regIX.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x66: // LD H, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterH(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x67: // LD IXH, A
                regIX.setHigh(regAF.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x68: // LD IXL, B
                regIX.setLow(regBC.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x69: // LD IXL, C
                regIX.setLow(regBC.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6A: // LD IXL, D
                regIX.setLow(regDE.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6B: // LD IXL, E
                regIX.setLow(regDE.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6C: // LD IXL, IXH
                regIX.setLow(regIX.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6D: // LD IXL, IXL
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6E: // LD L, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterL(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x6F: // LD IXL, A
                regIX.setLow(regAF.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x70: // LD (IX+d), B
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0x71: // LD (IX+d), C
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0x72: // LD (IX+d), D
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0x73: // LD (IX+d), E
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0x74: // LD (IX+d), H
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0x75: // LD (IX+d), L
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0x76: // HALT
                Halt();
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x77: // LD (IX+d), A
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0x78: // LD A, B
                setRegisterA(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x79: // LD A, C
                setRegisterA(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7A: // LD A, D
                setRegisterA(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7B: // LD A, E
                setRegisterA(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7C: // LD A, IXH
                setRegisterA(regIX.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7D: // LD A, IXL
                setRegisterA(regIX.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7E: // LD A, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterA(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x7F: // LD A, A
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x80: // ADD A, B
                setRegisterAF(tableALU[0][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x81: // ADD A, C
                setRegisterAF(tableALU[0][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x82: // ADD A, D
                setRegisterAF(tableALU[0][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x83: // ADD A, E
                setRegisterAF(tableALU[0][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x84: // ADD A, IXH
                setRegisterAF(tableALU[0][regAF.high][regIX.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x85: // ADD A, IXL
                setRegisterAF(tableALU[0][regAF.high][regIX.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x86: // ADD A, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[0][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19; // modify 16 to 19
                break;
            case 0x87: // ADD A, A
                setRegisterAF(tableALU[0][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x88: // ADC A, B
                setRegisterAF(tableALU[1][regAF.high][regBC.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x89: // ADC A, C
                setRegisterAF(tableALU[1][regAF.high][regBC.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8A: // ADC A, D
                setRegisterAF(tableALU[1][regAF.high][regDE.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8B: // ADC A, E
                setRegisterAF(tableALU[1][regAF.high][regDE.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8C: // ADC A, IXH
                setRegisterAF(tableALU[1][regAF.high][regIX.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8D: // ADC A, IXL
                setRegisterAF(tableALU[1][regAF.high][regIX.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8E: // ADC A, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[1][regAF.high][readMemory.read((short) regWZ.word) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x8F: // ADC A, A
                setRegisterAF(tableALU[1][regAF.high][regAF.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x90: // SUB B
                setRegisterAF(tableALU[2][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x91: // SUB C
                setRegisterAF(tableALU[2][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x92: // SUB D
                setRegisterAF(tableALU[2][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x93: // SUB E
                setRegisterAF(tableALU[2][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x94: // SUB IXH
                setRegisterAF(tableALU[2][regAF.high][regIX.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x95: // SUB IXL
                setRegisterAF(tableALU[2][regAF.high][regIX.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x96: // SUB (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[2][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x97: // SUB A, A
                setRegisterAF(tableALU[2][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x98: // SBC A, B
                setRegisterAF(tableALU[3][regAF.high][regBC.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x99: // SBC A, C
                setRegisterAF(tableALU[3][regAF.high][regBC.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9A: // SBC A, D
                setRegisterAF(tableALU[3][regAF.high][regDE.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9B: // SBC A, E
                setRegisterAF(tableALU[3][regAF.high][regDE.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9C: // SBC A, IXH
                setRegisterAF(tableALU[3][regAF.high][regIX.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9D: // SBC A, IXL
                setRegisterAF(tableALU[3][regAF.high][regIX.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9E: // SBC A, (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[3][regAF.high][readMemory.read((short) regWZ.word) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x9F: // SBC A, A
                setRegisterAF(tableALU[3][regAF.high][regAF.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA0: // AND B
                setRegisterAF(tableALU[4][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA1: // AND C
                setRegisterAF(tableALU[4][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA2: // AND D
                setRegisterAF(tableALU[4][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA3: // AND E
                setRegisterAF(tableALU[4][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA4: // AND IXH
                setRegisterAF(tableALU[4][regAF.high][regIX.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA5: // AND IXL
                setRegisterAF(tableALU[4][regAF.high][regIX.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA6: // AND (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[4][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xA7: // AND A
                setRegisterAF(tableALU[4][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA8: // XOR B
                setRegisterAF(tableALU[5][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA9: // XOR C
                setRegisterAF(tableALU[5][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAA: // XOR D
                setRegisterAF(tableALU[5][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAB: // XOR E
                setRegisterAF(tableALU[5][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAC: // XOR IXH
                setRegisterAF(tableALU[5][regAF.high][regIX.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAD: // XOR IXL
                setRegisterAF(tableALU[5][regAF.high][regIX.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAE: // XOR (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[5][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xAF: // XOR A
                setRegisterAF(tableALU[5][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB0: // OR B
                setRegisterAF(tableALU[6][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB1: // OR C
                setRegisterAF(tableALU[6][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB2: // OR D
                setRegisterAF(tableALU[6][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB3: // OR E
                setRegisterAF(tableALU[6][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB4: // OR IXH
                setRegisterAF(tableALU[6][regAF.high][regIX.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB5: // OR IXL
                setRegisterAF(tableALU[6][regAF.high][regIX.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB6: // OR (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[6][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xB7: // OR A
                setRegisterAF(tableALU[6][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB8: // CP B
                setRegisterAF(tableALU[7][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB9: // CP C
                setRegisterAF(tableALU[7][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBA: // CP D
                setRegisterAF(tableALU[7][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBB: // CP E
                setRegisterAF(tableALU[7][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBC: // CP IXH
                setRegisterAF(tableALU[7][regAF.high][regIX.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBD: // CP IXL
                setRegisterAF(tableALU[7][regAF.high][regIX.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBE: // CP (IX+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIX.word, c.displacement));
                setRegisterAF(tableALU[7][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xBF: // CP A
                setRegisterAF(tableALU[7][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC0: // RET NZ
                if (!getRegFlagZ()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC1: // POP BC
                setRegisterC(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterB(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC2: // JP NZ, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagZ()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC3: // JP nn
                setRegisterPC((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC4: // CALL NZ, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xC5: // PUSH BC
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regBC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regBC.low);
                break;
            case 0xC6: // ADD A, n
                setRegisterAF(tableALU[0][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xC7: // RST $00
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x00);
                break;
            case 0xC8: // RET Z
                if (getRegFlagZ()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC9: // RET
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCA: // JP Z, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagZ()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCB: // (Prefix)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                //regR = inc7(regR);
                setRegisterWZ(add16(regIX.word, c.displacement));
                processDDCB(c);
                break;
            case 0xCC: // CALL Z, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xCD: // CALL nn
                totalExecutedCycles += 17;
                pendingCycles -= 17;
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(c.tus);
                break;
            case 0xCE: // ADC A, n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                setRegisterAF(tableALU[1][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][getRegFlagC() ? 1 : 0]);
                break;
            case 0xCF: // RST $08
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x08);
                break;
            case 0xD0: // RET NC
                if (!getRegFlagC()) {
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD1: // POP DE
                setRegisterE(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterD(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD2: // JP NC, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagC()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD3: // OUT n, A
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeHardware.write((short) (byte) readMemory.read((short) regPC.getAndIncWord()), (byte) regAF.high);
                break;
            case 0xD4: // CALL NC, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xD5: // PUSH DE
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regDE.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regDE.low);
                break;
            case 0xD6: // SUB n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                setRegisterAF(tableALU[2][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                break;
            case 0xD7: // RST $10
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x10);
                break;
            case 0xD8: // RET C
                if (getRegFlagC()) {
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD9: // EXX
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                c.tus = regBC.word;
                setRegisterBC(regAltBC.word);
                setRegisterShadowBC(c.tus);
                c.tus = regDE.word;
                setRegisterDE(regAltDE.word);
                setRegisterShadowDE(c.tus);
                c.tus = regHL.word;
                setRegisterHL(regAltHL.word);
                setRegisterShadowHL(c.tus);
                break;
            case 0xDA: // JP C, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagC()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xDB: // IN A, n
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) | ((regAF.high & 0xff) << 8);
                setRegisterA(readHardware.read((short) c.tus) & 0xff);
                setRegisterWZ(add16(c.tus, 1));
                break;
            case 0xDC: // CALL C, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xDD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xDE: // SBC A, n
                setRegisterAF(tableALU[3][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xDF: // RST $18
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x18);
                break;
            case 0xE0: // RET PO
                if (!getRegFlagP()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE1: // POP IX
                regIX.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regIX.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0xE2: // JP PO, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagP()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xE3: // EX (SP), IX
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = regSP.word;
                c.tbl = readMemory.read((short) c.getAndIncTUS()) & 0xff;
                c.tbh = readMemory.read((short) c.getAndDecTUS()) & 0xff;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regIX.low);
                writeMemory.write((short) c.tus, (byte) regIX.high);
                regIX.setLow(c.tbl);
                regIX.setHigh(c.tbh);
                setRegisterWZ(regIX.word);
                break;
            case 0xE4: // CALL C, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xE5: // PUSH IX
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regIX.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regIX.low);
                break;
            case 0xE6: // AND n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                setRegisterAF(tableALU[4][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                break;
            case 0xE7: // RST $20
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x20);
                break;
            case 0xE8: // RET PE
                if (getRegFlagP()) {
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE9: // JP IX
                setRegisterPC(regIX.word);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEA: // JP PE, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagP()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xEB: // EX DE, HL
                c.tus = regDE.word;
                setRegisterDE(regHL.word);
                setRegisterHL(c.tus);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEC: // CALL PE, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagP()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xED: // (Prefix)
                regR = inc7(regR);
                switch (readOp.read((short) regPC.getAndIncWord()) & 0xff) {
                    case 0x00: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x01: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x02: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x03: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x04: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x05: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x06: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x07: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x08: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x09: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x10: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x11: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x12: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x13: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x14: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x15: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x16: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x17: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x18: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x19: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x20: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x21: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x22: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x23: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x24: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x25: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x26: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x27: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x28: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x29: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x30: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x31: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x32: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x33: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x34: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x35: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x36: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x37: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x38: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x39: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x40: // IN B, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterB(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regBC.high > 127);
                        setRegFlagZ(regBC.high == 0);
                        setRegFlag5((regBC.high & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regBC.high & 0x08) != 0);
                        setRegFlagP(tableParity[regBC.high]);
                        setRegFlagN(false);
                        break;
                    case 0x41: // OUT C, B
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regBC.high);
                        break;
                    case 0x42: // SBC HL, BC
                        c.ti1 = regHL.word;
                        c.ti2 = regBC.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((regHL.word ^ regBC.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC(((regHL.word - regBC.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x43: // LD (nn), BC
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regBC.low);
                        writeMemory.write((short) c.tus, (byte) regBC.high);
                        break;
                    case 0x44: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x45: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x46: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x47: // LD I, A
                        regI = regAF.high;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x48: // IN C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterC(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regBC.low > 127);
                        setRegFlagZ(regBC.low == 0);
                        setRegFlag5((regBC.low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regBC.low & 0x08) != 0);
                        setRegFlagP(tableParity[regBC.low]);
                        setRegFlagN(false);
                        break;
                    case 0x49: // OUT C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regBC.low);
                        break;
                    case 0x4A: // ADC HL, BC
                        c.ti1 = (short) regHL.word;
                        c.ti2 = (short) regBC.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            ++c.tir;
                            ++c.ti2;
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(((c.ti1 & 0xffff) + (c.ti2 & 0xffff)) > 0xffff);
                        setRegFlagP(c.tir > 0x7fff || c.tir < -0x8000);
                        setRegFlagS(c.tus > 0x7fff);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x4B: // LD BC, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterC(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterB(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x4C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x4E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4F: // LD R, A
                        regR = regAF.high;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x50: // IN D, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterD(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regDE.high > 127);
                        setRegFlagZ(regDE.high == 0);
                        setRegFlag5((regDE.high & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regDE.high & 0x08) != 0);
                        setRegFlagP(tableParity[regDE.high]);
                        setRegFlagN(false);
                        break;
                    case 0x51: // OUT C, D
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regDE.high);
                        break;
                    case 0x52: // SBC HL, DE
                        c.ti1 = regHL.word;
                        c.ti2 = regDE.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((regHL.word ^ regDE.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC(((regHL.word - regDE.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x53: // LD (nn), DE
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regDE.low);
                        writeMemory.write((short) c.tus, (byte) regDE.high);
                        break;
                    case 0x54: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x55: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x56: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x57: // LD A, I
                        setRegisterA(regI);
                        setRegFlagS((regI & 0xff) > 127);
                        setRegFlagZ(regI == 0);
                        setRegFlag5(((regI & 0x20) != 0));
                        setRegFlagH(false);
                        setRegFlag3(((regI & 0x08) != 0));
                        setRegFlagN(false);
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x58: // IN E, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterE(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regDE.low > 127);
                        setRegFlagZ(regDE.low == 0);
                        setRegFlag5((regDE.low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regDE.low & 0x08) != 0);
                        setRegFlagP(tableParity[regDE.low]);
                        setRegFlagN(false);
                        break;
                    case 0x59: // OUT C, E
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regDE.low);
                        break;
                    case 0x5A: // ADC HL, DE
                        c.ti1 = (short) regHL.word;
                        c.ti2 = (short) regDE.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            ++c.tir;
                            ++c.ti2;
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(((c.ti1 & 0xffff) + (c.ti2 & 0xffff)) > 0xffff);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x5B: // LD DE, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterE(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterD(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x5C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x5E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5F: // LD A, R
                        setRegisterA((regR & 0x7F) | (regR2 & 0xff));
                        setRegFlagS((regR2 == (byte) 0x80));
                        setRegFlagZ(((regR & 0x7F) | (regR2 & 0xff)) == 0);
                        setRegFlagH(false);
                        setRegFlag5(((regR & 0x20) != 0));
                        setRegFlagN(false);
                        setRegFlag3(((regR & 0x08) != 0));
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x60: // IN H, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterH(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regHL.high > 127);
                        setRegFlagZ(regHL.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regHL.high]);
                        setRegFlagN(false);
                        setRegFlag3((regHL.high & 0x08) != 0);
                        setRegFlag5((regHL.high & 0x20) != 0);
                        break;
                    case 0x61: // OUT C, H
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regHL.high);
                        break;
                    case 0x62: // SBC HL, HL
                        c.ti1 = regHL.word;
                        c.ti2 = regHL.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((regHL.word ^ regHL.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) regHL.word - (int) regHL.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x63: // LD (nn), HL
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regHL.low);
                        writeMemory.write((short) c.tus, (byte) regHL.high);
                        break;
                    case 0x64: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x65: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x66: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x67: // RRD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.tb1 = regAF.high;
                        c.tb2 = readMemory.read((short) regHL.word) & 0xff;
                        writeMemory.write((short) regHL.word, (byte) ((c.tb2 >> 4) + (c.tb1 << 4)));
                        setRegisterA((c.tb1 & 0xF0) + (c.tb2 & 0x0F));
                        setRegFlagS(regAF.high > 127);
                        setRegFlagZ(regAF.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regAF.high]);
                        setRegFlagN(false);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        break;
                    case 0x68: // IN L, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterL(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regHL.low > 127);
                        setRegFlagZ(regHL.low == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regHL.low]);
                        setRegFlagN(false);
                        setRegFlag3((regHL.low & 0x08) != 0);
                        setRegFlag5((regHL.low & 0x20) != 0);
                        break;
                    case 0x69: // OUT C, L
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regHL.low);
                        break;
                    case 0x6A: // ADC HL, HL
                        c.ti1 = (short) regHL.word;
                        c.ti2 = (short) regHL.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            ++c.tir;
                            ++c.ti2;
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(((c.ti1 & 0xffff) + (c.ti2 & 0xffff)) > 0xffff);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x6B: // LD HL, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterL(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterH(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x6C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x6E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6F: // RLD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.tb1 = regAF.high;
                        c.tb2 = readMemory.read((short) regHL.word) & 0xff;
                        writeMemory.write((short) regHL.word, (byte) ((c.tb1 & 0x0F) + (c.tb2 << 4)));
                        setRegisterA((c.tb1 & 0xF0) + ((c.tb2 >>> 4) & 0x0f));
                        setRegFlagS(regAF.high > 127);
                        setRegFlagZ(regAF.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regAF.high]);
                        setRegFlagN(false);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        break;
                    case 0x70: // IN 0, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegFlagS((c.tb & 0xff) > 127);
                        setRegFlagZ(c.tb == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[c.tb]);
                        setRegFlagN(false);
                        setRegFlag3((c.tb & 0x08) != 0);
                        setRegFlag5((c.tb & 0x20) != 0);
                        break;
                    case 0x71: // OUT C, 0
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) 0);
                        break;
                    case 0x72: // SBC HL, SP
                        c.ti1 = regHL.word;
                        c.ti2 = regSP.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((regHL.word ^ regSP.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC(((regHL.word - regSP.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x73: // LD (nn), SP
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regSP.low);
                        writeMemory.write((short) c.tus, (byte) regSP.high);
                        break;
                    case 0x74: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x75: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x76: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x77: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x78: // IN A, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterA(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regAF.high > 127);
                        setRegFlagZ(regAF.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regAF.high]);
                        setRegFlagN(false);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        setRegisterWZ(add16(regBC.word, 1));
                        break;
                    case 0x79: // OUT C, A
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regAF.high);
                        break;
                    case 0x7A: // ADC HL, SP
                        c.ti1 = regHL.word;
                        c.ti2 = regSP.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            c.tir = inc16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x7B: // LD SP, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        regSP.setLow(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        regSP.setHigh(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x7C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x7E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x80: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x81: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x82: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x83: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x84: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x85: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x86: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x87: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x88: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x89: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x90: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x91: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x92: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x93: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x94: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x95: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x96: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x97: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x98: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x99: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA0: // LDI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        writeMemory.write((short) regDE.getAndIncWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA1: // CPI
                        c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS((c.tb2 & 0xff) > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xA2: // INI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(add16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndIncWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(add8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xA3: // OUTI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        regBC.decAndGetHigh();
                        setRegisterWZ(add16(regBC.word, 1));
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(isX(regBC.high));
                        setRegFlagH(false);
                        setRegFlag5(isY(regBC.high));
                        setRegFlagZ(regBC.high == 0);
                        setRegFlagS(isS(regBC.high));
                        c.tus = add16(regHL.low, c.tb);
                        if (isS(c.tb)) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xA4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA8: // LDD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        writeMemory.write((short) regDE.getAndDecWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA9: // CPD
                        c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        regWZ.decAndGetWord();
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS(c.tb2 > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xAA: // IND
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(sub16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndDecWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(sub8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xAB: // OUTD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        regBC.decAndGetHigh();
                        setRegisterWZ(sub16(regBC.word, 1));
                        c.tus = add16(regHL.low, c.tb);
                        setRegFlagZ(regBC.high == 0);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xAC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB0: // LDIR
                        writeMemory.write((short) regDE.getAndIncWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (regBC.word != 0) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB1: // CPIR
                        c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS(c.tb2 > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        if (regBC.word != 0 && !getRegFlagZ()) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB2: // INIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(add16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndIncWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(add8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB3: // OTIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        regBC.decAndGetHigh();
                        setRegisterWZ(add16(regBC.word, 1));
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(isX(regBC.high));
                        setRegFlagH(false);
                        setRegFlag5(isY(regBC.high));
                        setRegFlagZ(regBC.high == 0);
                        setRegFlagS(isS(regBC.high));
                        c.tus = add16(regHL.low, c.tb);
                        if (isS(c.tb)) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB8: // LDDR
                        writeMemory.write((short) regDE.getAndDecWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (regBC.word != 0) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB9: // CPDR
                        c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS(c.tb2 > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        if (regBC.word != 0 && !getRegFlagZ()) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xBA: // INDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(sub16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndDecWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(sub8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBB: // OTDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        regBC.decAndGetHigh();
                        setRegisterWZ(sub16(regBC.word, 1));
                        c.tus = add16(regHL.low, c.tb);
                        setRegFlagZ(regBC.high == 0);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xED: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                }
                break;
            case 0xEE: // XOR n
                setRegisterAF(tableALU[5][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xEF: // RST $28
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x28);
                break;
            case 0xF0: // RET P
                if (!getRegFlagS()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF1: // POP AF
                setRegisterF(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterA(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF2: // JP P, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagS()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF3: // DI
                iff1 = iff2 = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF4: // CALL P, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xF5: // PUSH AF
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regAF.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regAF.low);
                break;
            case 0xF6: // OR n
                setRegisterAF(tableALU[6][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xF7: // RST $30
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x30);
                break;
            case 0xF8: // RET M
                if (getRegFlagS()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF9: // LD SP, IX
                setRegisterSP(regIX.word);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFA: // JP M, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagS()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFB: // EI
                iff1 = iff2 = true;
                interruptTable = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFC: // CALL M, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xFD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xFE: // CP n
                setRegisterAF(tableALU[7][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xFF: // RST $38
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x38);
                break;
        }
    }

    private void processDDCB(Context c) {
        switch (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) {
            case 0x00: // RLC (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x01: // RLC (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x02: // RLC (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x03: // RLC (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x04: // RLC (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x05: // RLC (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x06: // RLC (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x07: // RLC (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x08: // RRC (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x09: // RRC (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x0A: // RRC (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x0B: // RRC (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x0C: // RRC (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x0D: // RRC (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x0E: // RRC (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0F: // RRC (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x10: // RL (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x11: // RL (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x12: // RL (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x13: // RL (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x14: // RL (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x15: // RL (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x16: // RL (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x17: // RL (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x18: // RR (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x19: // RR (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x1A: // RR (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x1B: // RR (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x1C: // RR (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x1D: // RR (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x1E: // RR (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1F: // RR (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x20: // SLA (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x21: // SLA (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x22: // SLA (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x23: // SLA (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x24: // SLA (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x25: // SLA (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x26: // SLA (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x27: // SLA (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x28: // SRA (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x29: // SRA (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x2A: // SRA (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x2B: // SRA (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x2C: // SRA (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x2D: // SRA (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x2E: // SRA (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2F: // SRA (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x30: // SLL (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x31: // SLL (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x32: // SLL (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x33: // SLL (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x34: // SLL (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x35: // SLL (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x36: // SLL (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x37: // SLL (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x38: // SRL (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterB((c.tus >>> 8) & 0xff);
                break;
            case 0x39: // SRL (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterC((c.tus >>> 8) & 0xff);
                break;
            case 0x3A: // SRL (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterD((c.tus >>> 8) & 0xff);
                break;
            case 0x3B: // SRL (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterE((c.tus >>> 8) & 0xff);
                break;
            case 0x3C: // SRL (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterH((c.tus >>> 8) & 0xff);
                break;
            case 0x3D: // SRL (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterL((c.tus >>> 8) & 0xff);
                break;
            case 0x3E: // SRL (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3F: // SRL (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                setRegisterA((c.tus >>> 8) & 0xff);
                break;
            case 0x40: // BIT 0, (IX+d)
            case 0x41: // BIT 0, (IX+d)
            case 0x42: // BIT 0, (IX+d)
            case 0x43: // BIT 0, (IX+d)
            case 0x44: // BIT 0, (IX+d)
            case 0x45: // BIT 0, (IX+d)
            case 0x46: // BIT 0, (IX+d)
            case 0x47: // BIT 0, (IX+d)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tBool = (readMemory.read((short) regWZ.word) & 0x01) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                break;
            case 0x48: // BIT 1, (IX+d)
            case 0x49: // BIT 1, (IX+d)
            case 0x4A: // BIT 1, (IX+d)
            case 0x4B: // BIT 1, (IX+d)
            case 0x4C: // BIT 1, (IX+d)
            case 0x4D: // BIT 1, (IX+d)
            case 0x4E: // BIT 1, (IX+d)
            case 0x4F: // BIT 1, (IX+d)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tBool = (readMemory.read((short) regWZ.word) & 0x02) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                break;
            case 0x50: // BIT 2, (IX+d)
            case 0x51: // BIT 2, (IX+d)
            case 0x52: // BIT 2, (IX+d)
            case 0x53: // BIT 2, (IX+d)
            case 0x54: // BIT 2, (IX+d)
            case 0x55: // BIT 2, (IX+d)
            case 0x56: // BIT 2, (IX+d)
            case 0x57: // BIT 2, (IX+d)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tBool = (readMemory.read((short) regWZ.word) & 0x04) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                break;
            case 0x58: // BIT 3, (IX+d)
            case 0x59: // BIT 3, (IX+d)
            case 0x5A: // BIT 3, (IX+d)
            case 0x5B: // BIT 3, (IX+d)
            case 0x5C: // BIT 3, (IX+d)
            case 0x5D: // BIT 3, (IX+d)
            case 0x5E: // BIT 3, (IX+d)
            case 0x5F: // BIT 3, (IX+d)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tBool = (readMemory.read((short) regWZ.word) & 0x08) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                break;
            case 0x60: // BIT 4, (IX+d)
            case 0x61: // BIT 4, (IX+d)
            case 0x62: // BIT 4, (IX+d)
            case 0x63: // BIT 4, (IX+d)
            case 0x64: // BIT 4, (IX+d)
            case 0x65: // BIT 4, (IX+d)
            case 0x66: // BIT 4, (IX+d)
            case 0x67: // BIT 4, (IX+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x10) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x68: // BIT 5, (IX+d)
            case 0x69: // BIT 5, (IX+d)
            case 0x6A: // BIT 5, (IX+d)
            case 0x6B: // BIT 5, (IX+d)
            case 0x6C: // BIT 5, (IX+d)
            case 0x6D: // BIT 5, (IX+d)
            case 0x6E: // BIT 5, (IX+d)
            case 0x6F: // BIT 5, (IX+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x20) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x70: // BIT 6, (IX+d)
            case 0x71: // BIT 6, (IX+d)
            case 0x72: // BIT 6, (IX+d)
            case 0x73: // BIT 6, (IX+d)
            case 0x74: // BIT 6, (IX+d)
            case 0x75: // BIT 6, (IX+d)
            case 0x76: // BIT 6, (IX+d)
            case 0x77: // BIT 6, (IX+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x40) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x78: // BIT 7, (IX+d)
            case 0x79: // BIT 7, (IX+d)
            case 0x7A: // BIT 7, (IX+d)
            case 0x7B: // BIT 7, (IX+d)
            case 0x7C: // BIT 7, (IX+d)
            case 0x7D: // BIT 7, (IX+d)
            case 0x7E: // BIT 7, (IX+d)
            case 0x7F: // BIT 7, (IX+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x80) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(!c.tBool);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x80: // RES 0, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0x81: // RES 0, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0x82: // RES 0, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0x83: // RES 0, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0x84: // RES 0, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0x85: // RES 0, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0x86: // RES 0, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfe));
                break;
            case 0x87: // RES 0, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xfe);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0x88: // RES 1, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0x89: // RES 1, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0x8A: // RES 1, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0x8B: // RES 1, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0x8C: // RES 1, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0x8D: // RES 1, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0x8E: // RES 1, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8F: // RES 1, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xfd);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0x90: // RES 2, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0x91: // RES 2, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0x92: // RES 2, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0x93: // RES 2, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0x94: // RES 2, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0x95: // RES 2, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0x96: // RES 2, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x04));
                break;
            case 0x97: // RES 2, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xfb);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0x98: // RES 3, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0x99: // RES 3, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0x9A: // RES 3, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0x9B: // RES 3, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0x9C: // RES 3, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0x9D: // RES 3, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0x9E: // RES 3, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x08));
                break;
            case 0x9F: // RES 3, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xf7);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xA0: // RES 4, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xA1: // RES 4, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xA2: // RES 4, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xA3: // RES 4, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xA4: // RES 4, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xA5: // RES 4, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xA6: // RES 4, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x10));
                break;
            case 0xA7: // RES 4, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xef);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xA8: // RES 5, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xA9: // RES 5, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xAA: // RES 5, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xAB: // RES 5, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xAC: // RES 5, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xAD: // RES 5, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xAE: // RES 5, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x20));
                break;
            case 0xAF: // RES 5, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xdf);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xB0: // RES 6, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xB1: // RES 6, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xB2: // RES 6, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xB3: // RES 6, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xB4: // RES 6, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xB5: // RES 6, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xB6: // RES 6, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x40));
                break;
            case 0xB7: // RES 6, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0xbf);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xB8: // RES 7, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xB9: // RES 7, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xBA: // RES 7, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xBB: // RES 7, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xBC: // RES 7, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xBD: // RES 7, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xBE: // RES 7, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x80));
                break;
            case 0xBF: // RES 7, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA(readMemory.read((short) regWZ.word) & 0x7f);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xC0: // SET 0, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xC1: // SET 0, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xC2: // SET 0, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xC3: // SET 0, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xC4: // SET 0, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xC5: // SET 0, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xC6: // SET 0, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x01));
                break;
            case 0xC7: // SET 0, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x01);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xC8: // SET 1, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xC9: // SET 1, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xCA: // SET 1, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xCB: // SET 1, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xCC: // SET 1, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xCD: // SET 1, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xCE: // SET 1, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x02));
                break;
            case 0xCF: // SET 1, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x02);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xD0: // SET 2, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xD1: // SET 2, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xD2: // SET 2, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xD3: // SET 2, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xD4: // SET 2, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xD5: // SET 2, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xD6: // SET 2, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x04));
                break;
            case 0xD7: // SET 2, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x04);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xD8: // SET 3, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xD9: // SET 3, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xDA: // SET 3, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xDB: // SET 3, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xDC: // SET 3, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xDD: // SET 3, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xDE: // SET 3, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x08));
                break;
            case 0xDF: // SET 3, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x08);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xE0: // SET 4, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xE1: // SET 4, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xE2: // SET 4, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xE3: // SET 4, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xE4: // SET 4, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xE5: // SET 4, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xE6: // SET 4, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x10));
                break;
            case 0xE7: // SET 4, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x10);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xE8: // SET 5, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xE9: // SET 5, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xEA: // SET 5, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xEB: // SET 5, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xEC: // SET 5, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xED: // SET 5, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xEE: // SET 5, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x20));
                break;
            case 0xEF: // SET 5, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x20);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xF0: // SET 6, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xF1: // SET 6, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xF2: // SET 6, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xF3: // SET 6, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xF4: // SET 6, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xF5: // SET 6, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xF6: // SET 6, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x40));
                break;
            case 0xF7: // SET 6, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x40);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0xF8: // SET 7, (IX+d)→B
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterB((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0xF9: // SET 7, (IX+d)→C
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterC((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0xFA: // SET 7, (IX+d)→D
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterD((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0xFB: // SET 7, (IX+d)→E
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterE((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0xFC: // SET 7, (IX+d)→H
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterH((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0xFD: // SET 7, (IX+d)→L
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterL((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0xFE: // SET 7, (IX+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) | 0x80));
                break;
            case 0xFF: // SET 7, (IX+d)→A
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                setRegisterA((readMemory.read((short) regWZ.word) & 0xff) | 0x80);
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
        }
    }

    private void processED(Context c) {
        switch (readOp.read((short) regPC.getAndIncWord()) & 0xff) {
            case 0x00: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x01: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x02: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x03: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x04: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x05: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x06: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x07: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x08: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x09: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x10: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x11: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x12: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x13: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x14: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x15: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x16: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x17: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x18: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x19: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x20: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x21: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x22: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x23: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x24: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x25: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x26: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x27: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x28: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x29: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x2F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x30: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x31: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x32: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x33: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x34: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x35: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x36: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x37: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x38: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x39: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x40: // IN B, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterB(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regBC.high > 127);
                setRegFlagZ(regBC.high == 0);
                setRegFlag5((regBC.high & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((regBC.high & 0x08) != 0);
                setRegFlagP(tableParity[regBC.high]);
                setRegFlagN(false);
                break;
            case 0x41: // OUT C, B
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regBC.high);
                break;
            case 0x42: // SBC HL, BC
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regBC.word;
                c.tir = c.ti1 - c.ti2;
                if (getRegFlagC()) {
                    c.tir = dec16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((regHL.word ^ regBC.word ^ c.tus) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC(((regHL.word - regBC.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) != 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x43: // LD (nn), BC
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regBC.low);
                writeMemory.write((short) c.tus, (byte) regBC.high);
                setRegisterWZ(c.tus);
                break;
            case 0x44: // NEG
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                setRegisterAF(tableNeg[regAF.word]);
                break;
            case 0x45: // RETN
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x46: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x47: // LD I, A
                regI = regAF.high;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x48: // IN C, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterC(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regBC.low > 127);
                setRegFlagZ(regBC.low == 0);
                setRegFlag5((regBC.low & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((regBC.low & 0x08) != 0);
                setRegFlagP(tableParity[regBC.low]);
                setRegFlagN(false);
                break;
            case 0x49: // OUT C, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regBC.low);
                break;
            case 0x4A: // ADC HL, BC
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regBC.word;
                c.tir = c.ti1 + c.ti2;
                if (getRegFlagC()) {
                    c.tir = inc16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((c.ti1 & 0xfff) + (c.ti2 & 0xfff)) > 0xfff);
                setRegFlagN(false);
                setRegFlagC(c.tir > 0xffff);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) == 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 0x7fff);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x4B: // LD BC, (nn)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                setRegisterC(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                setRegisterB(readMemory.read((short) c.tus) & 0xff);
                setRegisterWZ(c.tus);
                break;
            case 0x4C: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x4E: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x4F: // LD R, A
                regR = regAF.high;
                regR2 = regAF.high & 0x80;
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x50: // IN D, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterD(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regDE.high > 127);
                setRegFlagZ(regDE.high == 0);
                setRegFlag5((regDE.high & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((regDE.high & 0x08) != 0);
                setRegFlagP(tableParity[regDE.high]);
                setRegFlagN(false);
                break;
            case 0x51: // OUT C, D
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regDE.high);
                break;
            case 0x52: // SBC HL, DE
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regDE.word;
                c.tir = c.ti1 - c.ti2;
                if (getRegFlagC()) {
                    c.tir = dec16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((regHL.word ^ regDE.word ^ c.tus) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) regHL.word - (int) regDE.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) != 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x53: // LD (nn), DE
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regDE.low);
                writeMemory.write((short) c.tus, (byte) regDE.high);
                setRegisterWZ(c.tus);
                break;
            case 0x54: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x55: // RETN
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0x56: // IM $1
                interruptMode = 1;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x57: // LD A, I
                setRegisterA(regI);
                setRegFlagS((regI & 0xff) > 127);
                setRegFlagZ(regI == 0);
                setRegFlag5(((regI & 0x20) != 0));
                setRegFlagH(false);
                setRegFlag3(((regI & 0x08) != 0));
                setRegFlagN(false);
                setRegFlagP(iff2);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x58: // IN E, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterE(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regDE.low > 127);
                setRegFlagZ(regDE.low == 0);
                setRegFlag5((regDE.low & 0x20) != 0);
                setRegFlagH(false);
                setRegFlag3((regDE.low & 0x08) != 0);
                setRegFlagP(tableParity[regDE.low]);
                setRegFlagN(false);
                break;
            case 0x59: // OUT C, E
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regDE.low);
                break;
            case 0x5A: // ADC HL, DE
                c.ti1 = regHL.word;
                c.ti2 = regDE.word;
                c.tir = c.ti1 + c.ti2;
                if (getRegFlagC()) {
                    c.tir = inc16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) == 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x5B: // LD DE, (nn)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                setRegisterE(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                setRegisterD(readMemory.read((short) c.tus) & 0xff);
                setRegisterWZ(c.tus);
                break;
            case 0x5C: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x5E: // IM $2
                interruptMode = 2;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x5F: // LD A, R
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                setRegisterA((regR & 0x7F) | (regR2 & 0xff));
                setRegFlagS((regR2 == (byte) 0x80));
                setRegFlagZ(((regR & 0x7F) | (regR2 & 0xff)) == 0);
                setRegFlagH(false);
                setRegFlag5(((regR & 0x20) != 0));
                setRegFlagN(false);
                setRegFlag3(((regR & 0x08) != 0));
                setRegFlagP(iff2);
                break;
            case 0x60: // IN H, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterH(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regHL.high > 127);
                setRegFlagZ(regHL.high == 0);
                setRegFlagH(false);
                setRegFlagP(tableParity[regHL.high]);
                setRegFlagN(false);
                setRegFlag3((regHL.high & 0x08) != 0);
                setRegFlag5((regHL.high & 0x20) != 0);
                break;
            case 0x61: // OUT C, H
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regHL.high);
                break;
            case 0x62: // SBC HL, HL
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regHL.word;
                c.tir = c.ti1 - c.ti2;
                if (getRegFlagC()) {
                    c.tir = dec16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((regHL.word ^ regHL.word ^ c.tus) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) regHL.word - (int) regHL.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) != 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x63: // LD (nn), HL
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regHL.low);
                writeMemory.write((short) c.tus, (byte) regHL.high);
                setRegisterWZ(c.tus);
                break;
            case 0x64: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x65: // RETN
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x66: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x67: // RRD
                totalExecutedCycles += 18;
                pendingCycles -= 18;
                c.tb1 = regAF.high;
                c.tb2 = readMemory.read((short) regHL.word) & 0xff;
                setRegisterWZ(add16(regHL.word, 1));
                writeMemory.write((short) regHL.word, (byte) ((c.tb2 >> 4) + (c.tb1 << 4)));
                setRegisterA((c.tb1 & 0xF0) + (c.tb2 & 0x0F));
                setRegFlagS(regAF.high > 127);
                setRegFlagZ(regAF.high == 0);
                setRegFlagH(false);
                setRegFlagP(tableParity[regAF.high]);
                setRegFlagN(false);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                break;
            case 0x68: // IN L, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterL(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regHL.low > 127);
                setRegFlagZ(regHL.low == 0);
                setRegFlagH(false);
                setRegFlagP(tableParity[regHL.low]);
                setRegFlagN(false);
                setRegFlag3((regHL.low & 0x08) != 0);
                setRegFlag5((regHL.low & 0x20) != 0);
                break;
            case 0x69: // OUT C, L
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regHL.low);
                break;
            case 0x6A: // ADC HL, HL
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regHL.word;
                c.tir = c.ti1 + c.ti2;
                if (getRegFlagC()) {
                    c.tir = inc16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) == 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x6B: // LD HL, (nn)
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                setRegisterL(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                setRegisterH(readMemory.read((short) c.tus) & 0xff);
                setRegisterWZ(c.tus);
                break;
            case 0x6C: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x6E: // IM $0
                interruptMode = 0;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x6F: // RLD
                totalExecutedCycles += 18;
                pendingCycles -= 18;
                c.tb1 = regAF.high;
                c.tb2 = readMemory.read((short) regHL.word) & 0xff;
                setRegisterWZ(add16(regHL.word, 1));
                writeMemory.write((short) regHL.word, (byte) ((c.tb1 & 0x0F) + (c.tb2 << 4)));
                setRegisterA((c.tb1 & 0xF0) + ((c.tb2 >>> 4) & 0x0f));
                setRegFlagS(regAF.high > 127);
                setRegFlagZ(regAF.high == 0);
                setRegFlagH(false);
                setRegFlagP(tableParity[regAF.high]);
                setRegFlagN(false);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                break;
            case 0x70: // IN 0, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                c.tb = readHardware.read((short) regBC.word) & 0xff;
                setRegFlagS((c.tb & 0xff) > 127);
                setRegFlagZ(c.tb == 0);
                setRegFlagH(false);
                setRegFlagP(tableParity[c.tb]);
                setRegFlagN(false);
                setRegFlag3((c.tb & 0x08) != 0);
                setRegFlag5((c.tb & 0x20) != 0);
                break;
            case 0x71: // OUT C, 0
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) 0);
                break;
            case 0x72: // SBC HL, SP
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regSP.word;
                c.tir = c.ti1 - c.ti2;
                if (getRegFlagC()) {
                    c.tir = dec16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((regHL.word ^ regSP.word ^ c.tus) & 0x1000) != 0);
                setRegFlagN(true);
                setRegFlagC((((int) regHL.word - (int) regSP.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) != 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x73: // LD (nn), SP
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regSP.low);
                writeMemory.write((short) c.tus, (byte) regSP.high);
                setRegisterWZ(c.tus);
                break;
            case 0x74: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x75: // RETN
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x76: // IM $1
                interruptMode = 1;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x77: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x78: // IN A, C
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                setRegisterA(readHardware.read((short) regBC.word) & 0xff);
                setRegFlagS(regAF.high > 127);
                setRegFlagZ(regAF.high == 0);
                setRegFlagH(false);
                setRegFlagP(tableParity[regAF.high]);
                setRegFlagN(false);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                setRegisterWZ(add16(regBC.word, 1));
                break;
            case 0x79: // OUT C, A
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                writeHardware.write((short) regBC.word, (byte) regAF.high);
                setRegisterWZ(add16(regBC.word, 1));
                break;
            case 0x7A: // ADC HL, SP
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                c.ti1 = regHL.word;
                c.ti2 = regSP.word;
                c.tir = c.ti1 + c.ti2;
                if (getRegFlagC()) {
                    c.tir = inc16(c.tir);
                    c.ti2 = inc16(c.ti2);
                }
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regHL.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegFlagP(((regHL.word ^ c.ti2) & 0x8000) == 0 && ((regHL.word ^ c.tus) & 0x8000) != 0);
                setRegFlagS(c.tus > 32767);
                setRegFlagZ(c.tus == 0);
                setRegisterHL(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                break;
            case 0x7B: // LD SP, (nn)
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                regSP.setLow(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                regSP.setHigh(readMemory.read((short) c.tus) & 0xff);
                setRegisterWZ(c.tus);
                break;
            case 0x7C: // NEG
                setRegisterAF(tableNeg[regAF.word]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7D: // RETI
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterWZ(regPC.word);
                iff1 = iff2;
                break;
            case 0x7E: // IM $2
                interruptMode = 2;
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x7F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x80: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x81: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x82: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x83: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x84: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x85: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x86: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x87: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x88: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x89: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x90: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x91: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x92: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x93: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x94: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x95: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x96: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x97: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x98: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x99: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9A: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9B: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9C: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9D: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9E: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9F: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA0: // LDI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                writeMemory.write((short) regDE.getAndIncWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff));
                c.tb1 += regAF.high;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                break;
            case 0xA1: // CPI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                c.tb2 = sub8(regAF.high, c.tb1);;
                regWZ.incAndGetWord();
                setRegFlagN(true);
                setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                setRegFlagZ(c.tb2 == 0);
                setRegFlagS((c.tb2 & 0xff) > 127);
                c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                break;
            case 0xA2: // INI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readHardware.read((short) regBC.word) & 0xff;
                setRegisterWZ(add16(regBC.word, 1));
                regBC.decAndGetHigh();
                writeMemory.write((short) regHL.getAndIncWord(), (byte) c.tb);
                setRegFlagZ(regBC.high == 0);
                c.tus = add16(add8(regBC.low, 1), c.tb);
                if ((c.tb & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                break;
            case 0xA3: // OUTI
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                regBC.decAndGetHigh();
                setRegisterWZ(add16(regBC.word, 1));
                writeHardware.write((short) regBC.word, (byte) c.tb);
                setRegFlagC(false);
                setRegFlagN(false);
                setRegFlag3(isX(regBC.high));
                setRegFlagH(false);
                setRegFlag5(isY(regBC.high));
                setRegFlagZ(regBC.high == 0);
                setRegFlagS(isS(regBC.high));
                c.tus = add16(regHL.low, c.tb);
                if (isS(c.tb)) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagC(true);
                    setRegFlagH(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                break;
            case 0xA4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA8: // LDD
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                writeMemory.write((short) regDE.getAndDecWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff));
                c.tb1 += regAF.high;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                break;
            case 0xA9: // CPD
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                c.tb2 = sub8(regAF.high, c.tb1);;
                regWZ.decAndGetWord();
                setRegFlagN(true);
                setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                setRegFlagZ(c.tb2 == 0);
                setRegFlagS(c.tb2 > 127);
                c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                break;
            case 0xAA: // IND
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readHardware.read((short) regBC.word) & 0xff;
                setRegisterWZ(sub16(regBC.word, 1));
                regBC.decAndGetHigh();
                writeMemory.write((short) regHL.getAndDecWord(), (byte) c.tb);
                setRegFlagZ(regBC.high == 0);
                c.tus = add16(sub8(regBC.low, 1), c.tb);
                if ((c.tb & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                break;
            case 0xAB: // OUTD
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                writeHardware.write((short) regBC.word, (byte) c.tb);
                regBC.decAndGetHigh();
                setRegisterWZ(sub16(regBC.word, 1));
                c.tus = add16(regHL.low, c.tb);
                setRegFlagZ(regBC.high == 0);
                if ((c.tb & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                break;
            case 0xAC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB0: // LDIR
                writeMemory.write((short) regDE.getAndIncWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff));
                c.tb1 += regAF.high;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                if (regBC.word != 0) {
                    regPC.subWord(2);
                    setRegisterWZ(add16(regPC.word, 1));
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xB1: // CPIR
                c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                c.tb2 = sub8(regAF.high, c.tb1);;
                regWZ.incAndGetWord();
                setRegFlagN(true);
                setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                setRegFlagZ(c.tb2 == 0);
                setRegFlagS(c.tb2 > 127);
                c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                if (regBC.word != 0 && !getRegFlagZ()) {
                    regPC.subWord(2);
                    setRegisterWZ(add16(regPC.word, 1));
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xB2: // INIR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readHardware.read((short) regBC.word) & 0xff;
                setRegisterWZ(add16(regBC.word, 1));
                regBC.decAndGetHigh();
                writeMemory.write((short) regHL.getAndIncWord(), (byte) c.tb);
                setRegFlagZ(regBC.high == 0);
                c.tus = add16(add8(regBC.low,1), c.tb);
                if ((c.tb & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                if (regBC.high != 0) {
                    regPC.subWord(2);
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xB3: // OTIR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                regBC.decAndGetHigh();
                setRegisterWZ(add16(regBC.word, 1));
                writeHardware.write((short) regBC.word, (byte) c.tb);
                setRegFlagC(false);
                setRegFlagN(false);
                setRegFlag3(isX(regBC.high));
                setRegFlagH(false);
                setRegFlag5(isY(regBC.high));
                setRegFlagZ(regBC.high == 0);
                setRegFlagS(isS(regBC.high));
                c.tus = add8(regHL.low, c.tb);
                if (isS(c.tb)) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagC(true);
                    setRegFlagH(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                if (regBC.high != 0) {
                    regPC.subWord(2);
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xB4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB8: // LDDR
                writeMemory.write((short) regDE.getAndDecWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff));
                c.tb1 += regAF.high;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                setRegFlagH(false);
                setRegFlagN(false);
                if (regBC.word != 0) {
                    regPC.subWord(2);
                    setRegisterWZ(add16(regPC.word, 1));
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xB9: // CPDR
                c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                c.tb2 = sub8(regAF.high, c.tb1);;
                regWZ.decAndGetWord();
                setRegFlagN(true);
                setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                setRegFlagZ(c.tb2 == 0);
                setRegFlagS(c.tb2 > 127);
                c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                setRegFlag5((c.tb1 & 0x02) != 0);
                setRegFlag3((c.tb1 & 0x08) != 0);
                regBC.decAndGetWord();
                setRegFlagP(regBC.word != 0);
                if (regBC.word != 0 && !getRegFlagZ()) {
                    regPC.subWord(2);
                    setRegisterWZ(add16(regPC.word, 1));
                    totalExecutedCycles += 21;
                    pendingCycles -= 21;
                } else {
                    totalExecutedCycles += 16;
                    pendingCycles -= 16;
                }
                break;
            case 0xBA: // INDR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readHardware.read((short) regBC.word) & 0xff;
                setRegisterWZ(sub16(regBC.word, 1));
                regBC.decAndGetHigh();
                writeMemory.write((short) regHL.getAndDecWord(), (byte) c.tb);
                setRegFlagZ(regBC.high == 0);
                c.tus = add16(sub8(regBC.low, 1), c.tb);
                if ((c.tb & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                if (regBC.high != 0) {
                    regPC.subWord(2);
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xBB: // OTDR
                totalExecutedCycles += 16;
                pendingCycles -= 16;
                c.tb = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                writeHardware.write((short) regBC.word, (byte) c.tb);
                regBC.decAndGetHigh();
                setRegisterWZ(sub16(regBC.word, 1));
                c.tus = add8(regHL.low, c.tb);
                setRegFlagZ(regBC.high == 0);
                if ((c.tb & 0x80) != 0) {
                    setRegFlagN(true);
                }
                if ((c.tus & 0x100) != 0) {
                    setRegFlagH(true);
                    setRegFlagC(true);
                }
                setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                if (regBC.high != 0) {
                    regPC.subWord(2);
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xBC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xCF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xD9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xE9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xED: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF0: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF1: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF2: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF3: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF4: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF5: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF6: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF7: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF8: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF9: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFA: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFB: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFC: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFD: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFE: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFF: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
        }
    }

    private void processFDCB(Context c) {
        switch (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) {
            case 0x00: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x01: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x02: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x03: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x04: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x05: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x06: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x07: // RLC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][0][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x08: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x09: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0A: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0B: // RRC (IY+d)
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                break;
            case 0x0C: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0D: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0E: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x0F: // RRC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][1][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x10: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x11: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x12: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x13: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x14: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x15: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x16: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x17: // RL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][2][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x18: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x19: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1A: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1B: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1C: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1D: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1E: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x1F: // RR (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][3][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x20: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x21: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x22: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x23: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x24: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x25: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x26: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x27: // SLA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][4][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x28: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x29: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2A: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2B: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2C: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2D: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2E: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x2F: // SRA (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][5][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x30: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x31: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x32: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x33: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x34: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x35: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x36: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x37: // SLL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][6][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x38: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x39: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3A: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3B: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3C: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3D: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3E: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x3F: // SRL (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = tableRotShift[1][7][regAF.low + 256 * (readMemory.read((short) regWZ.word) & 0xff)];
                writeMemory.write((short) regWZ.word, (byte) (c.tus >> 8));
                setRegisterF(c.tus & 0xff);
                break;
            case 0x40: // BIT 0, (IY+d)
            case 0x41: // BIT 0, (IY+d)
            case 0x42: // BIT 0, (IY+d)
            case 0x43: // BIT 0, (IY+d)
            case 0x44: // BIT 0, (IY+d)
            case 0x45: // BIT 0, (IY+d)
            case 0x46: // BIT 0, (IY+d)
            case 0x47: // BIT 0, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x01) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x48: // BIT 1, (IY+d)
            case 0x49: // BIT 1, (IY+d)
            case 0x4A: // BIT 1, (IY+d)
            case 0x4B: // BIT 1, (IY+d)
            case 0x4C: // BIT 1, (IY+d)
            case 0x4D: // BIT 1, (IY+d)
            case 0x4E: // BIT 1, (IY+d)
            case 0x4F: // BIT 1, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x02) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x50: // BIT 2, (IY+d)
            case 0x51: // BIT 2, (IY+d)
            case 0x52: // BIT 2, (IY+d)
            case 0x53: // BIT 2, (IY+d)
            case 0x54: // BIT 2, (IY+d)
            case 0x55: // BIT 2, (IY+d)
            case 0x56: // BIT 2, (IY+d)
            case 0x57: // BIT 2, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x04) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x58: // BIT 3, (IY+d)
            case 0x59: // BIT 3, (IY+d)
            case 0x5a: // BIT 3, (IY+d)
            case 0x5b: // BIT 3, (IY+d)
            case 0x5c: // BIT 3, (IY+d)
            case 0x5d: // BIT 3, (IY+d)
            case 0x5e: // BIT 3, (IY+d)
            case 0x5f: // BIT 3, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x08) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x60: // BIT 4, (IY+d)
            case 0x61: // BIT 4, (IY+d)
            case 0x62: // BIT 4, (IY+d)
            case 0x63: // BIT 4, (IY+d)
            case 0x64: // BIT 4, (IY+d)
            case 0x65: // BIT 4, (IY+d)
            case 0x66: // BIT 4, (IY+d)
            case 0x67: // BIT 4, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x10) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x68: // BIT 5, (IY+d)
            case 0x69: // BIT 5, (IY+d)
            case 0x6A: // BIT 5, (IY+d)
            case 0x6B: // BIT 5, (IY+d)
            case 0x6C: // BIT 5, (IY+d)
            case 0x6D: // BIT 5, (IY+d)
            case 0x6E: // BIT 5, (IY+d)
            case 0x6F: // BIT 5, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x20) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x70: // BIT 6, (IY+d)
            case 0x71: // BIT 6, (IY+d)
            case 0x72: // BIT 6, (IY+d)
            case 0x73: // BIT 6, (IY+d)
            case 0x74: // BIT 6, (IY+d)
            case 0x75: // BIT 6, (IY+d)
            case 0x76: // BIT 6, (IY+d)
            case 0x77: // BIT 6, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x40) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(false);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x78: // BIT 7, (IY+d)
            case 0x79: // BIT 7, (IY+d)
            case 0x7A: // BIT 7, (IY+d)
            case 0x7B: // BIT 7, (IY+d)
            case 0x7C: // BIT 7, (IY+d)
            case 0x7D: // BIT 7, (IY+d)
            case 0x7E: // BIT 7, (IY+d)
            case 0x7F: // BIT 7, (IY+d)
                c.tBool = (readMemory.read((short) regWZ.word) & 0x80) == 0;
                setRegFlagN(false);
                setRegFlagP(c.tBool);
                setRegFlag3(((regWZ.word >> 8) & 0x08) != 0);
                setRegFlagH(true);
                setRegFlag5(((regWZ.word >> 8) & 0x20) != 0);
                setRegFlagZ(c.tBool);
                setRegFlagS(!c.tBool);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x80: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x81: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x82: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x83: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x84: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x85: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x86: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x87: // RES 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & (byte) ~0x01));
                break;
            case 0x88: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x89: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8A: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8B: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8C: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8D: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8E: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x8F: // RES 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfd));
                break;
            case 0x90: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x91: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x92: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x93: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x94: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x95: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x96: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x97: // RES 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xfb));
                break;
            case 0x98: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x99: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x9A: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x9B: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x9C: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x9D: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x9E: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0x9F: // RES 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xf7));
                break;
            case 0xA0: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA1: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA2: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA3: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA4: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA5: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA6: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA7: // RES 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xef));
                break;
            case 0xA8: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xA9: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xAA: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xAB: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xAC: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xAD: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xAE: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xAF: // RES 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xdf));
                break;
            case 0xB0: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB1: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB2: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB3: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB4: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB5: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB6: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB7: // RES 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0xbf));
                break;
            case 0xB8: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xB9: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xBA: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xBB: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xBC: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xBD: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xBE: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xBF: // RES 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) (readMemory.read((short) regWZ.word) & 0x7f));
                break;
            case 0xC0: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC1: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC2: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC3: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC4: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC5: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC6: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC7: // SET 0, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x01));
                break;
            case 0xC8: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xC9: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xCA: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xCB: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xCC: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xCD: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xCE: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xCF: // SET 1, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x02));
                break;
            case 0xD0: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD1: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD2: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD3: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD4: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD5: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD6: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD7: // SET 2, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x04));
                break;
            case 0xD8: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xD9: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xDA: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xDB: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xDC: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xDD: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xDE: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xDF: // SET 3, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x08));
                break;
            case 0xE0: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE1: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE2: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE3: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE4: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE5: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE6: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE7: // SET 4, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x10));
                break;
            case 0xE8: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xE9: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xEA: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xEB: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xEC: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xED: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xEE: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xEF: // SET 5, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x20));
                break;
            case 0xF0: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF1: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF2: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF3: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF4: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF5: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF6: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF7: // SET 6, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x40));
                break;
            case 0xF8: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xF9: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xFA: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xFB: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xFC: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xFD: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xFE: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
            case 0xFF: // SET 7, (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                writeMemory.write((short) regWZ.word, (byte) ((readMemory.read((short) regWZ.word) & 0xff) |  0x80));
                break;
        }
    }

    void processFD(Context c) {
        switch (readOp.read((short) regPC.getAndIncWord()) & 0xff) {
            case 0x00: // NOP
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x01: // LD BC, nn
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                setRegisterBC((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                break;
            case 0x02: // LD (BC), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                writeMemory.write((short) regBC.word, (byte) regAF.high);
                break;
            case 0x03: // INC BC
                regBC.incAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x04: // INC B
                setRegisterF(tableInc[regBC.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x05: // DEC B
                setRegisterF(tableDec[regBC.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x06: // LD B, n
                setRegisterB(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x07: // RLCA
                setRegisterAF(tableRotShift[0][0][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x08: // EX AF, AF'
                c.tus = regAF.word;
                setRegisterAF(regAltAF.word);
                setRegisterShadowAF(c.tus);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x09: // ADD IY, BC
                c.ti1 = regIY.word;
                c.ti2 = regBC.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIY.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIY(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x0A: // LD A, (BC)
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                setRegisterA(readMemory.read((short) regBC.word) & 0xff);
                break;
            case 0x0B: // DEC BC
                regBC.decAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x0C: // INC C
                setRegisterF(tableInc[regBC.incAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0D: // DEC C
                setRegisterF(tableDec[regBC.decAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x0E: // LD C, n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                setRegisterC(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                break;
            case 0x0F: // RRCA
                setRegisterAF(tableRotShift[0][1][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x10: // DJNZ d
                c.tsb = readMemory.read((short) regPC.getAndIncWord()) & 0xff;
                if ((regBC.decAndGetHigh()) != 0) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 13;
                    pendingCycles -= 13;
                } else {
                    totalExecutedCycles += 8;
                    pendingCycles -= 8;
                }
                break;
            case 0x11: // LD DE, nn
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                setRegisterDE((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                break;
            case 0x12: // LD (DE), A
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                writeMemory.write((short) regDE.word, (byte) regAF.high);
                break;
            case 0x13: // INC DE
                regDE.incAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x14: // INC D
                setRegisterF(tableInc[regDE.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x15: // DEC D
                setRegisterF(tableDec[regDE.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x16: // LD D, n
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                setRegisterD(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                break;
            case 0x17: // RLA
                setRegisterAF(tableRotShift[0][2][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x18: // JR d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                setRegisterPC(regPC.addWord(c.tsb));
                totalExecutedCycles += 12;
                pendingCycles -= 12;
                break;
            case 0x19: // ADD IY, DE
                c.ti1 = regIY.word;
                c.ti2 = regDE.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIY.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIY(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x1A: // LD A, (DE)
                setRegisterA(readMemory.read((short) regDE.word) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1B: // DEC DE
                regDE.decAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x1C: // INC E
                setRegisterF(tableInc[regDE.incAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1D: // DEC E
                setRegisterF(tableDec[regDE.decAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x1E: // LD E, n
                setRegisterE(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x1F: // RRA
                setRegisterAF(tableRotShift[0][3][regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x20: // JR NZ, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (!getRegFlagZ()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x21: // LD IY, nn
                setRegisterIY((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0x22: // LD (nn), IY
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regIY.low);
                writeMemory.write((short) c.tus, (byte) regIY.high);
                setRegisterWZ(c.tus);
                break;
            case 0x23: // INC IY
                regIY.incAndGetWord();
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x24: // INC IYH
                setRegisterF(tableInc[regIY.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x25: // DEC IYH
                setRegisterF(tableDec[regIY.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x26: // LD IYH, n
                regIY.setHigh(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                break;
            case 0x27: // DAA
                setRegisterAF(TableDaa[regAF.word]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x28: // JR Z, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (getRegFlagZ()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x29: // ADD IY, IY
                c.ti1 = regIY.word;
                c.ti2 = regIY.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIY.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIY(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x2A: // LD IY, (nn)
                c.tus = (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) + (readOpArg.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                regIY.setLow(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                regIY.setHigh(readMemory.read((short) c.tus) & 0xff);
                setRegisterWZ(c.tus);
                totalExecutedCycles += 20;
                pendingCycles -= 20;
                break;
            case 0x2B: // DEC IY
                regIY.decAndGetWord();
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x2C: // INC IYL
                setRegisterF(tableInc[regIY.incAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2D: // DEC IYL
                setRegisterF(tableDec[regIY.decAndGetLow()] | (regAF.low & 1));
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x2E: // LD IYL, n
                regIY.setLow(readOpArg.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                break;
            case 0x2F: // CPL
                regAF.setHighXor(0xFF);
                setRegFlagH(true);
                setRegFlagN(true);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x30: // JR NC, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (!getRegFlagC()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x31: // LD SP, nn
                setRegisterSP((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0x32: // LD (nn), A
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                writeMemory.write((short) ((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256), (byte) regAF.high);
                break;
            case 0x33: // INC SP
                regSP.incAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x34: // INC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                c.tb = readMemory.read((short) regWZ.word) & 0xff;
                setRegisterF(tableInc[c.incAndGetTB()] | (regAF.low & 1));
                writeMemory.write((short) regWZ.word, (byte) c.tb);
                break;
            case 0x35: // DEC (IY+d)
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                c.tb = readMemory.read((short) regWZ.word) & 0xff;
                setRegisterF(tableDec[c.decAndGetTB()] | (regAF.low & 1));
                writeMemory.write((short) regWZ.word, (byte) c.tb);
                break;
            case 0x36: // LD (IY+d), n
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, readOpArg.read((short) regPC.getAndIncWord()));
                break;
            case 0x37: // SCF
                setRegFlagH(false);
                setRegFlagN(false);
                setRegFlagC(true);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x38: // JR C, d
                c.tsb = readMemory.read((short) regPC.getAndIncWord());
                if (getRegFlagC()) {
                    setRegisterPC(regPC.addWord(c.tsb));
                    totalExecutedCycles += 12;
                    pendingCycles -= 12;
                } else {
                    totalExecutedCycles += 7;
                    pendingCycles -= 7;
                }
                break;
            case 0x39: // ADD IY, SP
                c.ti1 = regIY.word;
                c.ti2 = regSP.word;
                c.tir = c.ti1 + c.ti2;
                c.tus = c.tir & 0xffff;
                setRegisterWZ(add16(regIY.word, 1));
                setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                setRegFlagN(false);
                setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                setRegisterIY(c.tus);
                setRegFlag3((c.tus & 0x0800) != 0);
                setRegFlag5((c.tus & 0x2000) != 0);
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                break;
            case 0x3A: // LD A, (nn)
                setRegisterA(readMemory.read((short) ((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256)) & 0xff);
                totalExecutedCycles += 13;
                pendingCycles -= 13;
                break;
            case 0x3B: // DEC SP
                regSP.decAndGetWord();
                totalExecutedCycles += 6;
                pendingCycles -= 6;
                break;
            case 0x3C: // INC A
                setRegisterF(tableInc[regAF.incAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3D: // DEC A
                setRegisterF(tableDec[regAF.decAndGetHigh()] | (regAF.low & 1));
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x3E: // LD A, n
                setRegisterA(readMemory.read((short) regPC.getAndIncWord()) & 0xff);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0x3F: // CCF
                setRegFlagH(getRegFlagC());
                setRegFlagN(false);
                setRegFlagC(getRegFlagC() ^ true);
                setRegFlag3((regAF.high & 0x08) != 0);
                setRegFlag5((regAF.high & 0x20) != 0);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x40: // LD B, B
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x41: // LD B, C
                setRegisterB(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x42: // LD B, D
                setRegisterB(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x43: // LD B, E
                setRegisterB(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x44: // LD B, IYH
                setRegisterB(regIY.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x45: // LD B, IYL
                setRegisterB(regIY.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x46: // LD B, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterB(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x47: // LD B, A
                setRegisterB(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x48: // LD C, B
                setRegisterC(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x49: // LD C, C
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4A: // LD C, D
                setRegisterC(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4B: // LD C, E
                setRegisterC(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x4C: // LD C, IYH
                setRegisterC(regIY.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4D: // LD C, IYL
                setRegisterC(regIY.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x4E: // LD C, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterC(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x4F: // LD C, A
                setRegisterC(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x50: // LD D, B
                setRegisterD(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x51: // LD D, C
                setRegisterD(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x52: // LD D, D
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x53: // LD D, E
                setRegisterD(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x54: // LD D, IYH
                setRegisterD(regIY.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x55: // LD D, IYL
                setRegisterD(regIY.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x56: // LD D, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterD(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x57: // LD D, A
                setRegisterD(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x58: // LD E, B
                setRegisterE(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x59: // LD E, C
                setRegisterE(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5A: // LD E, D
                setRegisterE(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5B: // LD E, E
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x5C: // LD E, IYH
                setRegisterE(regIY.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5D: // LD E, IYL
                setRegisterE(regIY.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x5E: // LD E, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterE(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x5F: // LD E, A
                setRegisterE(regAF.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x60: // LD IYH, B
                regIY.setHigh(regBC.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x61: // LD IYH, C
                regIY.setHigh(regBC.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x62: // LD IYH, D
                regIY.setHigh(regDE.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x63: // LD IYH, E
                regIY.setHigh(regDE.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x64: // LD IYH, IYH
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x65: // LD IYH, IYL
                regIY.setHigh(regIY.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x66: // LD H, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterH(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x67: // LD IYH, A
                regIY.setHigh(regAF.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x68: // LD IYL, B
                regIY.setLow(regBC.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x69: // LD IYL, C
                regIY.setLow(regBC.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6A: // LD IYL, D
                regIY.setLow(regDE.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6B: // LD IYL, E
                regIY.setLow(regDE.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6C: // LD IYL, IYH
                regIY.setLow(regIY.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6D: // LD IYL, IYL
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x6E: // LD L, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterL(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x6F: // LD IYL, A
                regIY.setLow(regAF.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x70: // LD (IY+d), B
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regBC.high);
                break;
            case 0x71: // LD (IY+d), C
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regBC.low);
                break;
            case 0x72: // LD (IY+d), D
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regDE.high);
                break;
            case 0x73: // LD (IY+d), E
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regDE.low);
                break;
            case 0x74: // LD (IY+d), H
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regHL.high);
                break;
            case 0x75: // LD (IY+d), L
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regHL.low);
                break;
            case 0x76: // HALT
                Halt();
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x77: // LD (IY+d), A
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                writeMemory.write((short) regWZ.word, (byte) regAF.high);
                break;
            case 0x78: // LD A, B
                setRegisterA(regBC.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x79: // LD A, C
                setRegisterA(regBC.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7A: // LD A, D
                setRegisterA(regDE.high);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7B: // LD A, E
                setRegisterA(regDE.low);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x7C: // LD A, IYH
                setRegisterA(regIY.high);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7D: // LD A, IYL
                setRegisterA(regIY.low);
                totalExecutedCycles += 9;
                pendingCycles -= 9;
                break;
            case 0x7E: // LD A, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterA(readMemory.read((short) regWZ.word) & 0xff);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x7F: // LD A, A
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x80: // ADD A, B
                setRegisterAF(tableALU[0][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x81: // ADD A, C
                setRegisterAF(tableALU[0][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x82: // ADD A, D
                setRegisterAF(tableALU[0][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x83: // ADD A, E
                setRegisterAF(tableALU[0][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x84: // ADD A, IYH
                setRegisterAF(tableALU[0][regAF.high][regIY.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x85: // ADD A, IYL
                setRegisterAF(tableALU[0][regAF.high][regIY.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x86: // ADD A, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[0][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19; // 16
                break;
            case 0x87: // ADD A, A
                setRegisterAF(tableALU[0][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x88: // ADC A, B
                setRegisterAF(tableALU[1][regAF.high][regBC.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x89: // ADC A, C
                setRegisterAF(tableALU[1][regAF.high][regBC.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8A: // ADC A, D
                setRegisterAF(tableALU[1][regAF.high][regDE.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8B: // ADC A, E
                setRegisterAF(tableALU[1][regAF.high][regDE.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x8C: // ADC A, IYH
                setRegisterAF(tableALU[1][regAF.high][regIY.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8D: // ADC A, IYL
                setRegisterAF(tableALU[1][regAF.high][regIY.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x8E: // ADC A, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[1][regAF.high][readMemory.read((short) regWZ.word) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x8F: // ADC A, A
                setRegisterAF(tableALU[1][regAF.high][regAF.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x90: // SUB B
                setRegisterAF(tableALU[2][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x91: // SUB C
                setRegisterAF(tableALU[2][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x92: // SUB D
                setRegisterAF(tableALU[2][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x93: // SUB E
                setRegisterAF(tableALU[2][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x94: // SUB IYH
                setRegisterAF(tableALU[2][regAF.high][regIY.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x95: // SUB IYL
                setRegisterAF(tableALU[2][regAF.high][regIY.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x96: // SUB (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[2][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x97: // SUB A, A
                setRegisterAF(tableALU[2][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x98: // SBC A, B
                setRegisterAF(tableALU[3][regAF.high][regBC.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x99: // SBC A, C
                setRegisterAF(tableALU[3][regAF.high][regBC.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9A: // SBC A, D
                setRegisterAF(tableALU[3][regAF.high][regDE.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9B: // SBC A, E
                setRegisterAF(tableALU[3][regAF.high][regDE.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0x9C: // SBC A, IYH
                setRegisterAF(tableALU[3][regAF.high][regIY.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9D: // SBC A, IYL
                setRegisterAF(tableALU[3][regAF.high][regIY.low][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0x9E: // SBC A, (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[3][regAF.high][readMemory.read((short) regWZ.word) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0x9F: // SBC A, A
                setRegisterAF(tableALU[3][regAF.high][regAF.high][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA0: // AND B
                setRegisterAF(tableALU[4][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA1: // AND C
                setRegisterAF(tableALU[4][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA2: // AND D
                setRegisterAF(tableALU[4][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA3: // AND E
                setRegisterAF(tableALU[4][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA4: // AND IYH
                setRegisterAF(tableALU[4][regAF.high][regIY.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA5: // AND IYL
                setRegisterAF(tableALU[4][regAF.high][regIY.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xA6: // AND (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[4][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xA7: // AND A
                setRegisterAF(tableALU[4][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA8: // XOR B
                setRegisterAF(tableALU[5][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xA9: // XOR C
                setRegisterAF(tableALU[5][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAA: // XOR D
                setRegisterAF(tableALU[5][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAB: // XOR E
                setRegisterAF(tableALU[5][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xAC: // XOR IYH
                setRegisterAF(tableALU[5][regAF.high][regIY.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAD: // XOR IYL
                setRegisterAF(tableALU[5][regAF.high][regIY.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xAE: // XOR (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[5][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xAF: // XOR A
                setRegisterAF(tableALU[5][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB0: // OR B
                setRegisterAF(tableALU[6][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB1: // OR C
                setRegisterAF(tableALU[6][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB2: // OR D
                setRegisterAF(tableALU[6][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB3: // OR E
                setRegisterAF(tableALU[6][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB4: // OR IYH
                setRegisterAF(tableALU[6][regAF.high][regIY.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB5: // OR IYL
                setRegisterAF(tableALU[6][regAF.high][regIY.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xB6: // OR (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[6][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xB7: // OR A
                setRegisterAF(tableALU[6][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB8: // CP B
                setRegisterAF(tableALU[7][regAF.high][regBC.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xB9: // CP C
                setRegisterAF(tableALU[7][regAF.high][regBC.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBA: // CP D
                setRegisterAF(tableALU[7][regAF.high][regDE.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBB: // CP E
                setRegisterAF(tableALU[7][regAF.high][regDE.low][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xBC: // CP IYH
                setRegisterAF(tableALU[7][regAF.high][regIY.high][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBD: // CP IYL
                setRegisterAF(tableALU[7][regAF.high][regIY.low][0]);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xBE: // CP (IY+d)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                setRegisterWZ(add16(regIY.word, c.displacement));
                setRegisterAF(tableALU[7][regAF.high][readMemory.read((short) regWZ.word) & 0xff][0]);
                totalExecutedCycles += 19;
                pendingCycles -= 19;
                break;
            case 0xBF: // CP A
                setRegisterAF(tableALU[7][regAF.high][regAF.high][0]);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xC0: // RET NZ
                if (!getRegFlagZ()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC1: // POP BC
                setRegisterC(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterB(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC2: // JP NZ, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagZ()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC3: // JP nn
                setRegisterPC((readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xC4: // CALL NZ, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xC5: // PUSH BC
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regBC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regBC.low);
                break;
            case 0xC6: // ADD A, n
                setRegisterAF(tableALU[0][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xC7: // RST $00
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x00);
                break;
            case 0xC8: // RET Z
                if (getRegFlagZ()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xC9: // RET
                regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCA: // JP Z, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagZ()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xCB: // (Prefix)
                c.displacement = readOpArg.read((short) regPC.getAndIncWord());
                //regR = inc7(regR);
                setRegisterWZ(add16(regIY.word, c.displacement));
                processFDCB(c);
                break;
            case 0xCC: // CALL Z, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagZ()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xCD: // CALL nn
                totalExecutedCycles += 17;
                pendingCycles -= 17;
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(c.tus);
                break;
            case 0xCE: // ADC A, n
                setRegisterAF(tableALU[1][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xCF: // RST $08
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x08);
                break;
            case 0xD0: // RET NC
                if (!getRegFlagC()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD1: // POP DE
                setRegisterE(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterD(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD2: // JP NC, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagC()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xD3: // OUT n, A
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeHardware.write((short) (byte) readMemory.read((short) regPC.getAndIncWord()), (byte) regAF.high);
                break;
            case 0xD4: // CALL NC, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xD5: // PUSH DE
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regDE.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regDE.low);
                break;
            case 0xD6: // SUB n
                setRegisterAF(tableALU[2][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xD7: // RST $10
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x10);
                break;
            case 0xD8: // RET C
                if (getRegFlagC()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xD9: // EXX
                c.tus = regBC.word;
                setRegisterBC(regAltBC.word);
                setRegisterShadowBC(c.tus);
                c.tus = regDE.word;
                setRegisterDE(regAltDE.word);
                setRegisterShadowDE(c.tus);
                c.tus = regHL.word;
                setRegisterHL(regAltHL.word);
                setRegisterShadowHL(c.tus);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xDA: // JP C, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagC()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xDB: // IN A, n
                c.tus = ((readOpArg.read((short) regPC.getAndIncWord()) & 0xff) | ((regAF.high & 0xff) << 8));
                setRegisterA(readHardware.read((short) c.tus) & 0xff);
                setRegisterWZ(add16(c.tus, 1));
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                break;
            case 0xDC: // CALL C, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xDD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xDE: // SBC A, n
                setRegisterAF(tableALU[3][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][getRegFlagC() ? 1 : 0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xDF: // RST $18
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x18);
                break;
            case 0xE0: // RET PO
                if (!getRegFlagP()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE1: // POP IY
                regIY.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                regIY.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 14;
                pendingCycles -= 14;
                break;
            case 0xE2: // JP PO, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagP()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xE3: // EX (SP), IY
                totalExecutedCycles += 23;
                pendingCycles -= 23;
                c.tus = regSP.word;
                c.tbl = readMemory.read((short) c.getAndIncTUS()) & 0xff;
                c.tbh = readMemory.read((short) c.getAndDecTUS()) & 0xff;
                writeMemory.write((short) c.getAndIncTUS(), (byte) regIY.low);
                writeMemory.write((short) c.tus, (byte) regIY.high);
                regIY.setLow(c.tbl);
                regIY.setHigh(c.tbh);
                setRegisterWZ(regIY.word);
                break;
            case 0xE4: // CALL C, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagC()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xE5: // PUSH IY
                totalExecutedCycles += 15;
                pendingCycles -= 15;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regIY.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regIY.low);
                break;
            case 0xE6: // AND n
                setRegisterAF(tableALU[4][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xE7: // RST $20
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x20);
                break;
            case 0xE8: // RET PE
                if (getRegFlagP()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xE9: // JP IY
                setRegisterPC(regIY.word);
                totalExecutedCycles += 8;
                pendingCycles -= 8;
                break;
            case 0xEA: // JP PE, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagP()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xEB: // EX DE, HL
                c.tus = regDE.word;
                setRegisterDE(regHL.word);
                setRegisterHL(c.tus);
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xEC: // CALL PE, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagP()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xED: // (Prefix)
                regR = inc7(regR);
                switch (readOp.read((short) regPC.getAndIncWord()) & 0xff) {
                    case 0x00: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x01: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x02: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x03: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x04: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x05: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x06: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x07: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x08: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x09: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x0F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x10: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x11: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x12: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x13: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x14: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x15: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x16: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x17: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x18: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x19: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x1F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x20: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x21: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x22: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x23: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x24: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x25: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x26: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x27: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x28: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x29: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x2F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x30: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x31: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x32: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x33: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x34: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x35: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x36: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x37: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x38: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x39: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x3F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x40: // IN B, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterB(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regBC.high > 127);
                        setRegFlagZ(regBC.high == 0);
                        setRegFlag5((regBC.high & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regBC.high & 0x08) != 0);
                        setRegFlagP(tableParity[regBC.high]);
                        setRegFlagN(false);
                        break;
                    case 0x41: // OUT C, B
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regBC.high);
                        break;
                    case 0x42: // SBC HL, BC
                        c.ti1 = regHL.word;
                        c.ti2 = regBC.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((regHL.word ^ regBC.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC(((regHL.word - regBC.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x43: // LD (nn), BC
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regBC.low);
                        writeMemory.write((short) c.tus, (byte) regBC.high);
                        setRegisterWZ(c.tus);
                        break;
                    case 0x44: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x45: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterWZ(regPC.word);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x46: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x47: // LD I, A
                        regI = regAF.high;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x48: // IN C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterC(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regBC.low > 127);
                        setRegFlagZ(regBC.low == 0);
                        setRegFlag5((regBC.low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regBC.low & 0x08) != 0);
                        setRegFlagP(tableParity[regBC.low]);
                        setRegFlagN(false);
                        break;
                    case 0x49: // OUT C, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regBC.low);
                        break;
                    case 0x4A: // ADC HL, BC
                        c.ti1 = regHL.word;
                        c.ti2 = regBC.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            c.tir = inc16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x4B: // LD BC, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterC(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterB(readMemory.read((short) c.tus) & 0xff);
                        setRegisterWZ(c.tus);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x4C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x4E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x4F: // LD R, A
                        regR = regAF.high;
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x50: // IN D, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterD(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regDE.high > 127);
                        setRegFlagZ(regDE.high == 0);
                        setRegFlag5((regDE.high & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regDE.high & 0x08) != 0);
                        setRegFlagP(tableParity[regDE.high]);
                        setRegFlagN(false);
                        break;
                    case 0x51: // OUT C, D
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regDE.high);
                        break;
                    case 0x52: // SBC HL, DE
                        c.ti1 = regHL.word;
                        c.ti2 = regDE.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((regHL.word ^ regDE.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC(((regHL.word - regDE.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x53: // LD (nn), DE
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regDE.low);
                        writeMemory.write((short) c.tus, (byte) regDE.high);
                        setRegisterWZ(c.tus);
                        break;
                    case 0x54: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x55: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x56: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x57: // LD A, I
                        setRegisterA(regI);
                        setRegFlagS((regI & 0xff) > 127);
                        setRegFlagZ(regI == 0);
                        setRegFlag5(((regI & 0x20) != 0));
                        setRegFlagH(false);
                        setRegFlag3(((regI & 0x08) != 0));
                        setRegFlagN(false);
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x58: // IN E, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterE(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regDE.low > 127);
                        setRegFlagZ(regDE.low == 0);
                        setRegFlag5((regDE.low & 0x20) != 0);
                        setRegFlagH(false);
                        setRegFlag3((regDE.low & 0x08) != 0);
                        setRegFlagP(tableParity[regDE.low]);
                        setRegFlagN(false);
                        break;
                    case 0x59: // OUT C, E
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regDE.low);
                        break;
                    case 0x5A: // ADC HL, DE
                        c.ti1 = regHL.word;
                        c.ti2 = regDE.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            c.tir = inc16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xfff)) > 0xfff);
                        setRegFlagN(false);
                        setRegFlagC(c.tir > 0xffff);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x5B: // LD DE, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterE(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterD(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x5C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x5E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x5F: // LD A, R
                        setRegisterA((regR & 0x7F) | (regR2 & 0xff));
                        setRegFlagS((regR2 == (byte) 0x80));
                        setRegFlagZ(((regR & 0x7F) | (regR2 & 0xff)) == 0);
                        setRegFlagH(false);
                        setRegFlag5(((regR & 0x20) != 0));
                        setRegFlagN(false);
                        setRegFlag3(((regR & 0x08) != 0));
                        setRegFlagP(iff2);
                        totalExecutedCycles += 9;
                        pendingCycles -= 9;
                        break;
                    case 0x60: // IN H, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterH(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regHL.high > 127);
                        setRegFlagZ(regHL.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regHL.high]);
                        setRegFlagN(false);
                        setRegFlag3((regHL.high & 0x08) != 0);
                        setRegFlag5((regHL.high & 0x20) != 0);
                        break;
                    case 0x61: // OUT C, H
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regHL.high);
                        break;
                    case 0x62: // SBC HL, HL
                        c.ti1 = regHL.word;
                        c.ti2 = regHL.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((regHL.word ^ regHL.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) regHL.word - (int) regHL.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x63: // LD (nn), HL
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regHL.low);
                        writeMemory.write((short) c.tus, (byte) regHL.high);
                        break;
                    case 0x64: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x65: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x66: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x67: // RRD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.tb1 = regAF.high;
                        c.tb2 = readMemory.read((short) regHL.word) & 0xff;
                        writeMemory.write((short) regHL.word, (byte) ((c.tb2 >> 4) + (c.tb1 << 4)));
                        setRegisterA((c.tb1 & 0xF0) + (c.tb2 & 0x0F));
                        setRegFlagS(regAF.high > 127);
                        setRegFlagZ(regAF.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regAF.high]);
                        setRegFlagN(false);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        break;
                    case 0x68: // IN L, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterL(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regHL.low > 127);
                        setRegFlagZ(regHL.low == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regHL.low]);
                        setRegFlagN(false);
                        setRegFlag3((regHL.low & 0x08) != 0);
                        setRegFlag5((regHL.low & 0x20) != 0);
                        break;
                    case 0x69: // OUT C, L
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regHL.low);
                        break;
                    case 0x6A: // ADC HL, HL
                        c.ti1 = regHL.word;
                        c.ti2 = regHL.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            c.tir = inc16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x6B: // LD HL, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        setRegisterL(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        setRegisterH(readMemory.read((short) c.tus) & 0xff);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0x6C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x6E: // IM $0
                        interruptMode = 0;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x6F: // RLD
                        totalExecutedCycles += 18;
                        pendingCycles -= 18;
                        c.tb1 = regAF.high;
                        c.tb2 = readMemory.read((short) regHL.word) & 0xff;
                        writeMemory.write((short) regHL.word, (byte) ((c.tb1 & 0x0F) + (c.tb2 << 4)));
                        setRegisterA((c.tb1 & 0xF0) + ((c.tb2 >>> 4) & 0x0f));
                        setRegFlagS(regAF.high > 127);
                        setRegFlagZ(regAF.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regAF.high]);
                        setRegFlagN(false);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        break;
                    case 0x70: // IN 0, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegFlagS((c.tb & 0xff) > 127);
                        setRegFlagZ(c.tb == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[c.tb]);
                        setRegFlagN(false);
                        setRegFlag3((c.tb & 0x08) != 0);
                        setRegFlag5((c.tb & 0x20) != 0);
                        break;
                    case 0x71: // OUT C, 0
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) 0);
                        break;
                    case 0x72: // SBC HL, SP
                        c.ti1 = regHL.word;
                        c.ti2 = regSP.word;
                        c.tir = c.ti1 - c.ti2;
                        if (getRegFlagC()) {
                            c.tir = dec16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((regHL.word ^ regSP.word ^ c.tus) & 0x1000) != 0);
                        setRegFlagN(true);
                        setRegFlagC((((int) regHL.word - (int) regSP.word - (getRegFlagC() ? 1 : 0)) & 0x1_0000) != 0);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x73: // LD (nn), SP
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        writeMemory.write((short) c.getAndIncTUS(), (byte) regSP.low);
                        writeMemory.write((short) c.tus, (byte) regSP.high);
                        setRegisterWZ(c.tus);
                        break;
                    case 0x74: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x75: // RETN
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterWZ(regPC.word);
                        iff1 = iff2;
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x76: // IM $1
                        interruptMode = 1;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x77: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x78: // IN A, C
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        setRegisterA(readHardware.read((short) regBC.word) & 0xff);
                        setRegFlagS(regAF.high > 127);
                        setRegFlagZ(regAF.high == 0);
                        setRegFlagH(false);
                        setRegFlagP(tableParity[regAF.high]);
                        setRegFlagN(false);
                        setRegFlag3((regAF.high & 0x08) != 0);
                        setRegFlag5((regAF.high & 0x20) != 0);
                        setRegisterWZ(add16(regBC.word, 1));
                        break;
                    case 0x79: // OUT C, A
                        totalExecutedCycles += 12;
                        pendingCycles -= 12;
                        writeHardware.write((short) regBC.low, (byte) regAF.high);
                        setRegisterWZ(add16(regBC.word, 1));
                        break;
                    case 0x7A: // ADC HL, SP
                        c.ti1 = regHL.word;
                        c.ti2 = regSP.word;
                        c.tir = c.ti1 + c.ti2;
                        if (getRegFlagC()) {
                            c.tir = inc16(c.tir);
                            c.ti2 = inc16(c.ti2);
                        }
                        c.tus = c.tir & 0xffff;
                        setRegisterWZ(add16(regHL.word, 1));
                        setRegFlagH(((c.ti1 & 0xFFF) + (c.ti2 & 0xFFF)) > 0xFFF);
                        setRegFlagN(false);
                        setRegFlagC((c.ti1 + c.ti2) > 0xFFFF);
                        setRegFlagP(c.tir > 32767 || c.tir < -32768);
                        setRegFlagS(c.tus > 32767);
                        setRegFlagZ(c.tus == 0);
                        setRegisterHL(c.tus);
                        setRegFlag3((c.tus & 0x0800) != 0);
                        setRegFlag5((c.tus & 0x2000) != 0);
                        totalExecutedCycles += 15;
                        pendingCycles -= 15;
                        break;
                    case 0x7B: // LD SP, (nn)
                        c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                        regSP.setLow(readMemory.read((short) c.getAndIncTUS()) & 0xff);
                        regSP.setHigh(readMemory.read((short) c.tus) & 0xff);
                        setRegisterWZ(c.tus);
                        totalExecutedCycles += 20;
                        pendingCycles -= 20;
                        break;
                    case 0x7C: // NEG
                        setRegisterAF(tableNeg[regAF.word]);
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7D: // RETI
                        regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                        setRegisterWZ(regPC.word);
                        totalExecutedCycles += 14;
                        pendingCycles -= 14;
                        break;
                    case 0x7E: // IM $2
                        interruptMode = 2;
                        totalExecutedCycles += 8;
                        pendingCycles -= 8;
                        break;
                    case 0x7F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x80: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x81: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x82: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x83: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x84: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x85: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x86: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x87: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x88: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x89: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x8F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x90: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x91: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x92: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x93: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x94: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x95: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x96: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x97: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x98: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x99: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9A: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9B: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9C: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9D: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9E: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0x9F: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA0: // LDI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        writeMemory.write((short) regDE.getAndIncWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA1: // CPI
                        c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS((c.tb2 & 0xff) > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xA2: // INI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(add16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndIncWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(add8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xA3: // OUTI
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        regBC.decAndGetHigh();
                        setRegisterWZ(add16(regBC.word, 1));
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(isX(regBC.high));
                        setRegFlagH(false);
                        setRegFlag5(isY(regBC.high));
                        setRegFlagZ(regBC.high == 0);
                        setRegFlagS(isS(regBC.high));
                        c.tus = add8(regHL.low, c.tb);
                        if (isS(c.tb)) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xA4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xA8: // LDD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        writeMemory.write((short) regDE.getAndDecWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        break;
                    case 0xA9: // CPD
                        c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS(c.tb2 > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        break;
                    case 0xAA: // IND
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(sub16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndDecWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(sub8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xAB: // OUTD
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        regBC.decAndGetHigh();
                        setRegisterWZ(sub16(regBC.word, 1));
                        c.tus = add8(regHL.low, c.tb);
                        setRegFlagZ(regBC.high == 0);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        break;
                    case 0xAC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xAF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB0: // LDIR
                        writeMemory.write((short) regDE.getAndIncWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (regBC.word != 0) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB1: // CPIR
                        c.tb1 = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        regWZ.incAndGetWord();
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS(c.tb2 > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        if (regBC.word != 0 && !getRegFlagZ()) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB2: // INIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(add16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndIncWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(add8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB3: // OTIR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndIncWord()) & 0xff;
                        regBC.decAndGetHigh();
                        setRegisterWZ(add16(regBC.word, 1));
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        setRegFlagC(false);
                        setRegFlagN(false);
                        setRegFlag3(isX(regBC.high));
                        setRegFlagH(false);
                        setRegFlag5(isY(regBC.high));
                        setRegFlagZ(regBC.high == 0);
                        setRegFlagS(isS(regBC.high));
                        c.tus = add8(regHL.low, c.tb);
                        if (isS(c.tb)) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagC(true);
                            setRegFlagH(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xB4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xB8: // LDDR
                        writeMemory.write((short) regDE.getAndDecWord(), (byte) (c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff));
                        c.tb1 += regAF.high;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        setRegFlagH(false);
                        setRegFlagN(false);
                        if (regBC.word != 0) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xB9: // CPDR
                        c.tb1 = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        c.tb2 = sub8(regAF.high, c.tb1);;
                        regWZ.decAndGetWord();
                        setRegFlagN(true);
                        setRegFlagH(tableHalfBorrow[regAF.high][c.tb1]);
                        setRegFlagZ(c.tb2 == 0);
                        setRegFlagS(c.tb2 > 127);
                        c.tb1 = sub8(regAF.high, c.tb1 + (getRegFlagH() ? 1 : 0));;
                        setRegFlag5((c.tb1 & 0x02) != 0);
                        setRegFlag3((c.tb1 & 0x08) != 0);
                        regBC.decAndGetWord();
                        setRegFlagP(regBC.word != 0);
                        if (regBC.word != 0 && !getRegFlagZ()) {
                            regPC.subWord(2);
                            setRegisterWZ(add16(regPC.word, 1));
                            totalExecutedCycles += 21;
                            pendingCycles -= 21;
                        } else {
                            totalExecutedCycles += 16;
                            pendingCycles -= 16;
                        }
                        break;
                    case 0xBA: // INDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readHardware.read((short) regBC.word) & 0xff;
                        setRegisterWZ(sub16(regBC.word, 1));
                        regBC.decAndGetHigh();
                        writeMemory.write((short) regHL.getAndDecWord(), (byte) c.tb);
                        setRegFlagZ(regBC.high == 0);
                        c.tus = add16(sub8(regBC.low, 1), c.tb);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBB: // OTDR
                        totalExecutedCycles += 16;
                        pendingCycles -= 16;
                        c.tb = readMemory.read((short) regHL.getAndDecWord()) & 0xff;
                        writeHardware.write((short) regBC.word, (byte) c.tb);
                        regBC.decAndGetHigh();
                        setRegisterWZ(sub16(regBC.word, 1));
                        c.tus = add8(regHL.low, c.tb);
                        setRegFlagZ(regBC.high == 0);
                        if ((c.tb & 0x80) != 0) {
                            setRegFlagN(true);
                        }
                        if ((c.tus & 0x100) != 0) {
                            setRegFlagH(true);
                            setRegFlagC(true);
                        }
                        setRegFlagP(tableParity[(c.tus & 0x07) ^ regBC.high]);
                        if (regBC.high != 0) {
                            regPC.subWord(2);
                            totalExecutedCycles += 5;
                            pendingCycles -= 5;
                        }
                        break;
                    case 0xBC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xBF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xC9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xCF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xD9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xDF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xE9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xED: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xEF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF0: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF1: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF2: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF3: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF4: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF5: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF6: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF7: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF8: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xF9: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFA: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFB: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFC: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFD: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFE: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                    case 0xFF: // NOP
                        totalExecutedCycles += 4;
                        pendingCycles -= 4;
                        break;
                }
                break;
            case 0xEE: // XOR n
                setRegisterAF(tableALU[5][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xEF: // RST $28
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x28);
                break;
            case 0xF0: // RET P
                if (!getRegFlagS()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF1: // POP AF
                setRegisterF(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                setRegisterA(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF2: // JP P, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagS()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xF3: // DI
                iff1 = iff2 = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xF4: // CALL P, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (!getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xF5: // PUSH AF
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regAF.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regAF.low);
                break;
            case 0xF6: // OR n
                setRegisterAF(tableALU[6][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xF7: // RST $30
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x30);
                break;
            case 0xF8: // RET M
                if (getRegFlagS()) {
                    regPC.setLow(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    regPC.setHigh(readMemory.read((short) regSP.getAndIncWord()) & 0xff);
                    totalExecutedCycles += 11;
                    pendingCycles -= 11;
                } else {
                    totalExecutedCycles += 5;
                    pendingCycles -= 5;
                }
                break;
            case 0xF9: // LD SP, IY
                setRegisterSP(regIY.word);
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFA: // JP M, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagS()) {
                    setRegisterPC(c.tus);
                }
                totalExecutedCycles += 10;
                pendingCycles -= 10;
                break;
            case 0xFB: // EI
                iff1 = iff2 = true;
                interruptTable = false;
                totalExecutedCycles += 4;
                pendingCycles -= 4;
                break;
            case 0xFC: // CALL M, nn
                c.tus = (readMemory.read((short) regPC.getAndIncWord()) & 0xff) + (readMemory.read((short) regPC.getAndIncWord()) & 0xff) * 256;
                if (getRegFlagS()) {
                    totalExecutedCycles += 17;
                    pendingCycles -= 17;
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                    writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                    setRegisterPC(c.tus);
                } else {
                    totalExecutedCycles += 10;
                    pendingCycles -= 10;
                }
                break;
            case 0xFD: // <-
                // Invalid sequence.
                totalExecutedCycles += 1337;
                pendingCycles -= 1337;
                break;
            case 0xFE: // CP n
                setRegisterAF(tableALU[7][regAF.high][readMemory.read((short) regPC.getAndIncWord()) & 0xff][0]);
                totalExecutedCycles += 7;
                pendingCycles -= 7;
                break;
            case 0xFF: // RST $38
                totalExecutedCycles += 11;
                pendingCycles -= 11;
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.high);
                writeMemory.write((short) regSP.decAndGetWord(), (byte) regPC.low);
                setRegisterPC(0x38);
                break;
        }
    }

//#endregion

//#region Interrupts

    private boolean iff1;

    public boolean getIFF1() {
        return iff1;
    }

    public void setIFF1(boolean value) {
        iff1 = value;
    }

    private boolean iff2;

    public boolean getIFF2() {
        return iff2;
    }

    public void setIFF2(boolean value) {
        iff2 = value;
    }

    private boolean interrupt;

    public boolean getInterrupt() {
        return interrupt;
    }

    public void setInterrupt(boolean value) {
        interrupt = value;
    }

    private boolean nonMaskableInterrupt;

    public boolean getNonMaskableInterrupt() {
        return nonMaskableInterrupt;
    }

    public void setNonMaskableInterrupt(boolean value) {
        if (value && !nonMaskableInterrupt) nonMaskableInterruptPending = true;
        nonMaskableInterrupt = value;
    }

    private boolean nonMaskableInterruptPending;

    public boolean getNonMaskableInterruptPending() {
        return nonMaskableInterruptPending;
    }

    public void setNonMaskableInterruptPending(boolean value) {
        nonMaskableInterruptPending = value;
    }

    private int interruptMode;

    public int getInterruptMode() {
        return interruptMode;
    }

    public void setInterruptMode(int value) {
        if (value < 0 || value > 2) throw new IndexOutOfBoundsException();
        interruptMode = value;
    }

    private boolean halted;

    public boolean getHalted() {
        return halted;
    }

    public void setHalted(boolean value) {
        halted = value;
    }

    /** answers the interrupt acknowledge cycle, a floating bus reads 0xff (rst 38h in mode 0) */
    public java.util.function.IntSupplier IRQCallback = () -> 0xff;

    public Runnable NMICallback = () -> {
    };

    private void ResetInterrupts() {
        interrupt = false;
        nonMaskableInterrupt = false;
        nonMaskableInterruptPending = false;
        interruptTable = true;
        iff1 = iff2 = false;
    }

    private void Halt() {
        regPC.decAndGetWord();
        halted = true;
    }

//#endregion

    public static int add16(int v, int o) {
        return (v + o) & 0xffff;
    }

    public static int inc16(int v) {
        return add16(v, 1);
    }

    public static int sub16(int v, int o) {
        return (v - o) & 0xffff;
    }

    public static int dec16(int v) {
        return sub16(v, 1);
    }

    public static int add8(int v, int o) {
        return (v + o) & 0xff;
    }

    public static int inc8(int v) {
        return add8(v, 1);
    }

    private int inc7(int v) {
        return (v & 0x80) | ((v + 1) & 0x7F);
    }

    public static int sub8(int v, int o) {
        return (v - o) & 0xff;
    }

    public static int dec8(int v) {
        return sub8(v, 1);
    }
}
