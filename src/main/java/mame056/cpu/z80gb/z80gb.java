package mame056.cpu.z80gb;

import static mame056.cpu.z80gb.opc_mainH.*;
import static mame056.cpu.z80gb.z80gbH.*;
import mame056.cpuintrfH;
import static mame056.cpuintrfH.*;
import mame056.cpuintrfH.irqcallbacksPtr;
import static mame056.memory.cpu_readmem16;
import static mame056.memory.cpu_setOPbase16;
import static mame056.memory.cpu_writemem16;
import static mame056.memoryH.change_pc16;
import static mess056.machine.gbH.IFLAGS;
import static mess056.machine.gbH.ISWITCH;
import static mess056.machine.gbH.TIMEFRQ;
import static mess056.machine.gbH.TIMEMOD;
import static mess056.machine.gbH.TIM_IFLAG;
import static mess056.machine.gbH.gb_divcount;
import static mess056.machine.gbH.gb_timer_count;
import static mess056.machine.gbH.gb_timer_shift;

public class z80gb extends cpuintrfH.cpu_interface {

    public static int[] z80gb_ICount = new int[1];

    public z80gb() {
        cpu_num = CPU_Z80GB;
        num_irqs = 5;
        default_vector = 255;
        icount = z80gb_ICount;
        overclock = 1.00;
        irq_int = 0;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 4;

    }

    public static class Z80GB_Regs {

        public int PC, SP, A, F, B, C, D, E, H, L;
        public int irq_state;/* irq line state */
        public int enable;
        public irqcallbacksPtr irq_callback;
    }

    public static int FLAG_Z = 0x80;
    public static int FLAG_N = 0x40;
    public static int FLAG_H = 0x20;
    public static int FLAG_C = 0x10;

    static Z80GB_Regs Regs = new Z80GB_Regs();
    public static int /*UINT8*/ ICycles;
    public static int/*UINT8*/ CheckInterrupts;
    public static int IME = 0x01;
    public static int HALTED = 0x02;

    private static int AF() {
        return ((Regs.A << 8) | Regs.F) & 0xFFFF;
    }

    public static int BC() {
        return ((Regs.B << 8) | Regs.C) & 0xFFFF;
    }

    public static int DE() {
        return ((Regs.D << 8) | Regs.E) & 0xFFFF;
    }

    public static int HL() {
        return ((Regs.H << 8) | Regs.L) & 0xFFFF;
    }

    private static void AF(int nn) {
        Regs.A = (nn >> 8) & 0xff;
        Regs.F = nn & 0xff;
    }

    private static void BC(int nn) {
        Regs.B = (nn >> 8) & 0xff;
        Regs.C = nn & 0xff;
    }

    private static void DE(int nn) {
        Regs.D = (nn >> 8) & 0xff;
        Regs.E = nn & 0xff;
    }

    private static void HL(int nn) {
        Regs.H = (nn >> 8) & 0xff;
        Regs.L = nn & 0xff;
    }
    static int Cycles[]
            = {
                4, 12, 8, 8, 4, 4, 8, 4, 20, 8, 8, 8, 4, 4, 8, 4,
                4, 12, 8, 8, 4, 4, 8, 4, 8, 8, 8, 8, 4, 4, 8, 4,
                8, 12, 8, 8, 4, 4, 8, 4, 8, 8, 8, 8, 4, 4, 8, 4,
                8, 12, 8, 8, 12, 12, 12, 4, 8, 8, 8, 8, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                8, 8, 8, 8, 8, 8, 4, 8, 4, 4, 4, 4, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                4, 4, 4, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 8, 4,
                8, 12, 12, 12, 12, 16, 8, 16, 8, 8, 12, 0, 12, 24, 8, 16,
                8, 12, 12, 4, 12, 16, 8, 16, 8, 16, 12, 4, 12, 4, 8, 16,
                12, 12, 8, 4, 4, 16, 8, 16, 16, 4, 16, 4, 4, 4, 8, 16,
                12, 12, 8, 4, 4, 16, 8, 16, 12, 8, 16, 4, 4, 4, 8, 16
            };

    static int CyclesCB[]
            = {
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8,
                8, 8, 8, 8, 8, 8, 16, 8, 8, 8, 8, 8, 8, 8, 16, 8
            };

    @Override
    public void init() {
        //not needed
    }

    @Override
    public void reset(Object param) {
        Regs.PC = 0;
        Regs.SP = 0;
        Regs.A = 0;
        Regs.F = 0;
        Regs.B = 0;
        Regs.C = 0;
        Regs.D = 0;
        Regs.E = 0;
        Regs.H = 0;
        Regs.L = 0;
        Regs.irq_callback = null;
        Regs.enable = 0;
        Regs.irq_state = 0;
        //gb intialaze
        AF(0x01B0);
        BC(0x0013);
        DE(0x00D8);
        HL(0x014D);
        Regs.SP = 0xFFFE;
        Regs.PC = 0x0100;
        Regs.enable &= ~IME;

        CheckInterrupts = 0;
    }

    public static void z80gb_ProcessInterrupts() {

        if (CheckInterrupts != 0 && ((Regs.enable & IME)) != 0) {
            int /*UINT8*/ irq;

            CheckInterrupts = 0;

            irq = (ISWITCH() & IFLAGS()) & 0xFF;

            /*
                            logerror("Attempting to process Z80GB Interrupt IRQ $%02X\n", irq);
                            logerror("Attempting to process Z80GB Interrupt ISWITCH $%02X\n", ISWITCH);
                            logerror("Attempting to process Z80GB Interrupt IFLAGS $%02X\n", IFLAGS);
             */
            if (irq != 0) {
                int irqline = 0;
                /*
                            logerror("Z80GB Interrupt IRQ $%02X\n", irq);
                 */

                while (irqline < 5) {
                    if ((irq & (1 << irqline)) != 0) {
                        if (Regs.irq_callback != null) {
                            (Regs.irq_callback).handler(irqline);
                        }
                        if ((Regs.enable & HALTED) != 0) {
                            Regs.enable &= ~HALTED;
                            Regs.PC = (Regs.PC + 1) & 0xFFFF;
                        }
                        Regs.enable &= ~IME;
                        IFLAGS(IFLAGS() & ~(1 << irqline));
                        ICycles += 20;
                        Regs.SP = (Regs.SP - 2) & 0xFFFF;
                        mem_WriteWord(Regs.SP, Regs.PC);
                        Regs.PC = (0x40 + irqline * 8) & 0xFFFF;
                        /*logerror("Z80GB Interrupt PC $%04X\n", Regs.w.PC );*/
                        return;
                    }
                    irqline++;
                }
            }
        }
    }

    @Override
    public void exit() {
        //no functionality expected
    }

    /**
     * *
     *
     * arcadeflex functions
     */
    @Override
    public Object init_context() {
        Object reg = new Z80GB_Regs();
        return reg;
    }

    @Override
    public void set_op_base(int pc) {
        cpu_setOPbase16.handler(pc);
    }

    @Override
    public int memory_read(int offset) {
        return cpu_readmem16(offset);
    }

    @Override
    public void memory_write(int offset, int data) {
        cpu_writemem16(offset, data);
    }

    @Override
    public int internal_read(int offset) {
        return 0; //doesn't exist in z80 cpu
    }

    @Override
    public void internal_write(int offset, int data) {
        //doesesn't exist in z80 cpu
    }

    @Override
    public int mem_address_bits_of_cpu() {
        return 16;
    }

    @Override
    public Object get_context() {
        Z80GB_Regs r = new Z80GB_Regs();
        r.PC = Regs.PC;
        r.SP = Regs.SP;
        r.A = Regs.A;
        r.F = Regs.F;
        r.B = Regs.B;
        r.C = Regs.C;
        r.D = Regs.D;
        r.E = Regs.E;
        r.H = Regs.H;
        r.L = Regs.L;
        r.irq_state = Regs.irq_state;
        r.enable = Regs.enable;
        r.irq_callback = Regs.irq_callback;
        return r;
    }

    @Override
    public void set_context(Object reg) {
        Z80GB_Regs r = (Z80GB_Regs) reg;
        Regs.PC = r.PC;
        Regs.SP = r.SP;
        Regs.A = r.A;
        Regs.F = r.F;
        Regs.B = r.B;
        Regs.C = r.C;
        Regs.D = r.D;
        Regs.E = r.E;
        Regs.H = r.H;
        Regs.L = r.L;
        Regs.irq_state = r.irq_state;
        Regs.enable = r.enable;
        Regs.irq_callback = r.irq_callback;
        change_pc16(Regs.PC);
    }

    @Override
    public int[] get_cycle_table(int which) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int get_reg(int regnum) {
        switch (regnum) {
            case Z80GB_PC:
                return Regs.PC & 0xFFFF;
            case Z80GB_SP:
                return Regs.SP & 0xFFFF;
            case Z80GB_AF:
                return AF();
            case Z80GB_BC:
                return BC();
            case Z80GB_DE:
                return DE();
            case Z80GB_HL:
                return HL();
        }
        return 0;
    }

    @Override
    public void set_reg(int regnum, int val) {
        switch (regnum) {
            case Z80GB_PC:
                Regs.PC = val & 0xFFFF;
            case Z80GB_SP:
                Regs.SP = val & 0xFFFF;
            case Z80GB_AF:
                AF(val);
            case Z80GB_BC:
                BC(val);
            case Z80GB_DE:
                DE(val);
            case Z80GB_HL:
                HL(val);
        }
    }

    @Override
    public void set_irq_line(int irqline, int state) {
        /*logerror("setting irq line 0x%02x state 0x%08x\n", irqline, state);*/
        //if( Regs.w.irq_state == state )
        //	return;

        Regs.irq_state = state;
        if (state == ASSERT_LINE) {

            IFLAGS(IFLAGS() | (0x01 << irqline));
            CheckInterrupts = 1;
            /*logerror("Z80GB assert irq line %d ($%02X)\n", irqline, IFLAGS);*/

        } else {

            IFLAGS(IFLAGS() & ~(0x01 << irqline));
            if (IFLAGS() == 0) {
                CheckInterrupts = 0;
            }
            /*logerror("Z80GB clear irq line %d ($%02X)\n", irqline, IFLAGS);*/
        }
    }

    @Override
    public void set_irq_callback(cpuintrfH.irqcallbacksPtr callback) {
        Regs.irq_callback = callback;
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        /*TODO*///	static char buffer[8][47+1];
/*TODO*///	static int which = 0;
/*TODO*///	z80gb_regs *r = context;
/*TODO*///
/*TODO*///	which = (which + 1) % 8;
/*TODO*///    buffer[which][0] = '\0';
/*TODO*///	if( !context )
/*TODO*///		r = &Regs;
/*TODO*///
        switch (regnum) {
            /*TODO*///		case CPU_INFO_REG+Z80GB_PC: sprintf(buffer[which], "PC:%04X", r->w.PC); break;
/*TODO*///		case CPU_INFO_REG+Z80GB_SP: sprintf(buffer[which], "SP:%04X", r->w.SP); break;
/*TODO*///		case CPU_INFO_REG+Z80GB_AF: sprintf(buffer[which], "AF:%04X", r->w.AF); break;
/*TODO*///		case CPU_INFO_REG+Z80GB_BC: sprintf(buffer[which], "BC:%04X", r->w.BC); break;
/*TODO*///		case CPU_INFO_REG+Z80GB_DE: sprintf(buffer[which], "DE:%04X", r->w.DE); break;
/*TODO*///		case CPU_INFO_REG+Z80GB_HL: sprintf(buffer[which], "HL:%04X", r->w.HL); break;
/*TODO*///		case CPU_INFO_REG+Z80GB_IRQ_STATE: sprintf(buffer[which], "IRQ:%X", r->w.irq_state); break;
/*TODO*///        case CPU_INFO_FLAGS:
/*TODO*///			sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
/*TODO*///				r->b.F & 0x80 ? 'Z':'.',
/*TODO*///				r->b.F & 0x40 ? 'N':'.',
/*TODO*///				r->b.F & 0x20 ? 'H':'.',
/*TODO*///				r->b.F & 0x10 ? 'C':'.',
/*TODO*///				r->b.F & 0x08 ? '3':'.',
/*TODO*///				r->b.F & 0x04 ? '2':'.',
/*TODO*///				r->b.F & 0x02 ? '1':'.',
/*TODO*///				r->b.F & 0x01 ? '0':'.');
/*TODO*///			break;
            case CPU_INFO_NAME:
                return "Z80GB";
            case CPU_INFO_FAMILY:
                return "Nintendo Z80";
            case CPU_INFO_VERSION:
                return "1.0";
            case CPU_INFO_FILE:
                return "z80gb.java";
            case CPU_INFO_CREDITS:
                return "Copyright (C) 2000 by The MESS Team.";
            /*TODO*///		case CPU_INFO_REG_LAYOUT: return (const char *)z80gb_reg_layout;
/*TODO*///		case CPU_INFO_WIN_LAYOUT: return (const char *)z80gb_win_layout;
        }
        throw new UnsupportedOperationException("Not supported yet.");
        /*TODO*///	return buffer[which];
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        return null;//no functionality
    }

    public static int INC_8BIT(int r) {
        r = (r + 1) & 0xFF;
        int f = (Regs.F & FLAG_C) & 0xFF;
        if (r == 0) {
            f |= FLAG_Z;
        }
        if ((r & 0xF) == 0) {
            f |= FLAG_H;
        }
        Regs.F = f & 0xFF;
        return r;
    }

    public static int DEC_8BIT(int x) {
        //uint8_t r,f; \
        x = (x - 1) & 0xFF;
        int r = (x);
        int f = ((Regs.F & FLAG_C) | FLAG_N) & 0xFF;
        if (r == 0) {
            f |= FLAG_Z;
        }
        if ((r & 0xF) == 0xF) {
            f |= FLAG_H;
        }
        Regs.F = f & 0xFF;
        return x;
    }

    public static void OR_A_X(int value) {
        Regs.A = (Regs.A | value) & 0xff;
        if (Regs.A == 0) {
            Regs.F = FLAG_Z;
        } else {
            Regs.F = 0;
        }
    }

    public static void XOR_A_X(int value) {
        Regs.A = (Regs.A ^ value) & 0xff;
        if (Regs.A == 0) {
            Regs.F = FLAG_Z;
        } else {
            Regs.F = 0;
        }
    }

    public static void AND_A_X(int value) {
        Regs.A = (Regs.A & value) & 0xff;
        if (Regs.A == 0) {
            Regs.F = FLAG_H | FLAG_Z;
        } else {
            Regs.F = FLAG_H;
        }
    }

    public static void CP_A_X(int x) {
        int /*UINT16*/ r1, r2;
        int /*UINT8*/ f;
        r1 = ((Regs.A & 0xF) - ((x) & 0xF)) & 0xFFFF;
        r2 = (Regs.A - (x)) & 0xFFFF;
        if ((r2 & 0xFF) == 0) {
            f = FLAG_N | FLAG_Z;
        } else {
            f = FLAG_N;
        }
        if (r2 > 0xFF) {
            f |= FLAG_C;
        }
        if (r1 > 0xF) {
            f |= FLAG_H;
        }
        Regs.F = f & 0xFF;
    }

    public static void ADD_A_X(int x) {
        int /*UINT16*/ r1, r2;
        int /*UINT8*/ f;
        r1 = ((Regs.A & 0xF) + ((x) & 0xF)) & 0xFFFF;
        r2 = (Regs.A + (x)) & 0xFFFF;
        Regs.A = r2 & 0xFF;
        if ((r2 & 0xFF) == 0) {
            f = FLAG_Z;
        } else {
            f = 0;
        }
        if (r2 > 0xFF) {
            f |= FLAG_C;
        }
        if (r1 > 0xF) {
            f |= FLAG_H;
        }
        Regs.F = f & 0xFF;
    }

    public static void ADC_A_X(int x) {
        int /*UINT16*/ r1, r2;
        int /*UINT8*/ f;
        r1 = ((Regs.A & 0xF) + ((x) & 0xF) + ((Regs.F & FLAG_C) != 0 ? 1 : 0)) & 0xFFFF;
        r2 = (Regs.A + (x) + ((Regs.F & FLAG_C) != 0 ? 1 : 0)) & 0xFFFF;
        if ((Regs.A = (r2 & 0xFF)) == 0) {
            f = FLAG_Z;
        } else {
            f = 0;
        }
        if (r2 > 0xFF) {
            f |= FLAG_C;
        }
        if (r1 > 0xF) {
            f |= FLAG_H;
        }
        Regs.F = f & 0xFF;
    }

    public static void ADD_HL_RR(int x) {
        int /*UINT32*/ r1, r2;
        int /*UINT8*/ f;
        r1 = HL() + (x);
        r2 = (HL() & 0xFFF) + ((x) & 0xFFF);
        f = (Regs.F & FLAG_Z) & 0xFF;
        if (r1 > 0xFFFF) {
            f |= FLAG_C;
        }
        if (r2 > 0x0FFF) {
            f |= FLAG_H;
        }
        HL(r1 & 0xFFFF);
        Regs.F = f & 0xFF;
    }

    public static void SUB_A_X(int x) {
        int /*UINT16*/ r1, r2;
        int /*UINT8*/ f;
        r1 = ((Regs.A & 0xF) - ((x) & 0xF)) & 0xFFFF;
        r2 = (Regs.A - (x)) & 0xFFFF;
        Regs.A = r2 & 0xFF;
        if ((r2 & 0xFF) == 0) {
            f = FLAG_N | FLAG_Z;
        } else {
            f = FLAG_N;
        }
        if (r2 > 0xFF) {
            f |= FLAG_C;
        }
        if (r1 > 0xF) {
            f |= FLAG_H;
        }
        Regs.F = f & 0xFF;
    }

    public static int SRL_8BIT(int x) {
        int f;
        if (((x) & 1) != 0) {
            f = FLAG_C;
        } else {
            f = 0;
        }
        x = x >> 1 & 0xFF;
        if ((x) == 0) {
            f |= FLAG_Z;
        }
        Regs.F = f & 0xFF;
        return x;
    }

    public static int RL_8BIT(int x) {
        int r;
        r = ((x) & 0x80) != 0 ? FLAG_C : 0;
        (x) = (((x) << 1) | ((Regs.F & FLAG_C) != 0 ? 1 : 0)) & 0xFF;
        if ((x) == 0) {
            r |= FLAG_Z;
        }
        Regs.F = r & 0xFF;
        return x;
    }

    public static int RLC_8BIT(int x) {
        int /*uint8_t*/ f;
        (x) = (((x) << 1) | ((x) >> 7)) & 0xFF;
        if (((x) & 1) != 0) {
            f = FLAG_C;
        } else {
            f = 0;
        }
        if ((x) == 0) {
            f |= FLAG_Z;
        }
        Regs.F = f & 0xFF;
        return x;
    }

    public static int RR_8BIT(int x) {
        int /*UINT8*/ r;
        r = ((x) & 1) != 0 ? FLAG_C : 0;
        (x) = (((x) >> 1) | ((Regs.F & FLAG_C) != 0 ? 0x80 : 0)) & 0xFF;
        if ((x) == 0) {
            r |= FLAG_Z;
        }
        Regs.F = r & 0xFF;
        return x;
    }

    public static int RRC_8BIT(int x) {
        int f;
        (x) = (((x) >> 1) | ((x) << 7)) & 0xff;
        if (((x) & 0x80) != 0) {
            f = FLAG_C;
        } else {
            f = 0;
        }
        if ((x) == 0) {
            f |= FLAG_Z;
        }
        Regs.F = f;
        return x;
    }

    public static int SWAP_8BIT(int x) {
        (x) = (((x) >> 4) | ((x) << 4)) & 0xFF;
        if ((x) == 0) {
            Regs.F = FLAG_Z;
        } else {
            Regs.F = 0;
        }
        return x;
    }
    private static final int[] bitSet = {1, 2, 4, 8, 16, 32, 64, 128};           // lookup table for setting a bit of an 8-bit value using OR
    private static final int[] bitRes = {254, 253, 251, 247, 239, 223, 191, 127}; // lookup table for resetting a bit of an 8-bit value using AND

    public static int RES_8BIT(int bitNumber, int value) {
        value = value & bitRes[bitNumber];
        return value;
    }

    public static int SET_8BIT(int bitNumber, int value) {
        value = value | bitSet[bitNumber];
        return value;
    }

    public static int SLA_8BIT(int x) {
        int f;
        if (((x) & 0x80) != 0) {
            f = FLAG_C;
        } else {
            f = 0;
        }
        x = x << 1 & 0xFF;
        if ((x) == 0) {
            f |= FLAG_Z;
        }
        Regs.F = f & 0xFF;
        return x;
    }

    public static int SRA_8BIT(int x) {
        int f;
        if (((x) & 1) != 0) {
            f = FLAG_C;
        } else {
            f = 0;
        }
        (x) = (((byte) (x)) >> 1) & 0xFF;
        if ((x) == 0) {
            f |= FLAG_Z;
        }
        Regs.F = f & 0xFF;
        return x;
    }

    public static void BIT_8BIT(int n, int x) {
        if (((x) & (1 << (n))) != 0) {
            Regs.F = (FLAG_H | (Regs.F & FLAG_C)) & 0xFF;
        } else {
            Regs.F = (FLAG_Z | FLAG_H | (Regs.F & FLAG_C)) & 0xFF;
        }
    }

    @Override
    public int execute(int cycles) {
        int/*UINT8*/ x;

        z80gb_ICount[0] = cycles;

        do {
            ICycles = 0;
            z80gb_ProcessInterrupts();
            x = mem_ReadByte(Regs.PC);
            Regs.PC = (Regs.PC + 1) & 0xFFFF;
            ICycles += Cycles[x];
            switch (x) {
                case 0x00:
                    break;

                case 0x01:
                    BC(mem_ReadWord(Regs.PC));/*	   LD BC,n16 */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0x02:
                    mem_WriteByte(BC(), Regs.A);/*	   LD (BC),A */
                    break;
                case 0x03:
                    BC((BC() + 1) & 0xFFFF);/*	   INC BC */
                    break;
                case 0x04:
                    Regs.B = INC_8BIT(Regs.B);/*	   INC B */
                    break;
                case 0x05:
                    Regs.B = DEC_8BIT(Regs.B);/*	   DEC B */
                    break;
                case 0x06:
                    Regs.B = mem_ReadByte(Regs.PC);/*	   LD B,n8 */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x07:
                    /*	   RLCA */
                    Regs.A = ((Regs.A << 1) | (Regs.A >> 7)) & 0xFF;
                    if ((Regs.A & 1) != 0) {
                        Regs.F = FLAG_C;
                    } else {
                        Regs.F = 0;
                    }
                    break;
                case 0x08:
                    mem_WriteWord(mem_ReadWord(Regs.PC), Regs.SP);/*	   LD (n16),SP */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0x09:
                    /*	   ADD HL,BC */
                    ADD_HL_RR(BC());
                    break;
                case 0x0A:
                    Regs.A = mem_ReadByte(BC());/*	   LD A,(BC) */
                    break;
                case 0x0B:
                    BC((BC() - 1) & 0xFFFF);/*	   DEC BC */
                    break;
                case 0x0C:
                    Regs.C = INC_8BIT(Regs.C);/*	   INC C */
                    break;
                case 0x0D:
                    Regs.C = DEC_8BIT(Regs.C);/*	   DEC C */
                    break;
                case 0x0E:
                    Regs.C = mem_ReadByte(Regs.PC);/*	   LD C,n8 */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x0F: /*	   RRCA */ {
                    Regs.A = ((Regs.A >> 1) | (Regs.A << 7)) & 0xFF;
                    Regs.F = 0;
                    if ((Regs.A & 0x80) != 0) {
                        Regs.F |= FLAG_C;
                    }
                }
                break;
                case 0x10:
                    /*	   STOP */
                    break;
                case 0x11:
                    DE(mem_ReadWord(Regs.PC));/*	   LD DE,n16 */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0x12:
                    mem_WriteByte(DE(), Regs.A);/*	   LD (DE),A */
                    break;
                case 0x13:
                    DE((DE() + 1) & 0xFFFF);/*	   INC DE */
                    break;
                case 0x14:
                    Regs.D = INC_8BIT(Regs.D);/*	   INC D */
                    break;
                case 0x15:
                    /*	   DEC D */
                    Regs.D = DEC_8BIT(Regs.D);
                    break;
                case 0x16:
                    /*	   LD D,n8 */
                    Regs.D = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x17:
                    /*	   RLA */

                    x = (Regs.A & 0x80) != 0 ? FLAG_C : 0;

                    Regs.A = ((Regs.A << 1) | ((Regs.F & FLAG_C) != 0 ? 1 : 0)) & 0xff;
                    Regs.F = x;
                    break;
                case 0x18: /*	   JR	   n8 */ {
                    byte offset;
                    offset = (byte) mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    Regs.PC = (Regs.PC + offset) & 0xFFFF;
                }
                break;
                case 0x19:
                    /*	   ADD HL,DE */
                    ADD_HL_RR(DE());
                    break;
                case 0x1A:
                    Regs.A = mem_ReadByte(DE());/*	   LD A,(DE) */
                    break;
                case 0x1B:
                    /*	   DEC DE */
                    DE((DE() - 1) & 0xFFFF);
                    break;
                case 0x1C:
                    Regs.E = INC_8BIT(Regs.E);/*	   INC E */
                    break;
                case 0x1D:
                    Regs.E = DEC_8BIT(Regs.E);/*	   DEC E */
                    break;
                case 0x1E:
                    /*	   LD E,n8 */
                    Regs.E = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x1F: /*	   RRA */ {
                    int x1 = (Regs.A & 1) != 0 ? FLAG_C : 0;
                    Regs.A = ((Regs.A >> 1) | ((Regs.F & FLAG_C) != 0 ? 0x80 : 0)) & 0xFF;
                    Regs.F = x1;
                }
                break;
                case 0x20:
                    if ((Regs.F & FLAG_Z) != 0) {/*	   JR NZ,n8 */
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    } else {
                        byte offset = (byte) mem_ReadByte(Regs.PC);
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                        Regs.PC = (Regs.PC + offset) & 0xFFFF;
                        ICycles += 4;
                    }
                    break;
                case 0x21:
                    HL(mem_ReadWord(Regs.PC));/*	   LD HL,n16 */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0x22:
                    mem_WriteByte(HL(), Regs.A);/*	   LD (HL+),A */
                    HL((HL() + 1) & 0xFFFF);
                    break;
                case 0x23:
                    HL((HL() + 1) & 0xFFFF);/*	   INC HL */
                    break;
                case 0x24:
                    Regs.H = INC_8BIT(Regs.H);/*	   INC H */
                    break;
                case 0x25:
                    Regs.H = DEC_8BIT(Regs.H);/*	   DEC H */
                    break;
                case 0x26:
                    Regs.H = mem_ReadByte(Regs.PC);/*	   LD H,n8 */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x27: /*	   DAA */ {
                    int tmp = Regs.A;

                    if ((Regs.F & FLAG_N) == 0) {
                        if ((Regs.F & FLAG_H) != 0 || (tmp & 0x0F) > 9) {
                            tmp += 6;
                        }
                        if ((Regs.F & FLAG_C) != 0 || tmp > 0x9F) {
                            tmp += 0x60;
                        }
                    } else {
                        if ((Regs.F & FLAG_H) != 0) {
                            tmp -= 6;
                            if ((Regs.F & FLAG_C) == 0) {
                                tmp &= 0xFF;
                            }
                        }
                        if ((Regs.F & FLAG_C) != 0) {
                            tmp -= 0x60;
                        }
                    }
                    Regs.F = (Regs.F & ~(FLAG_H | FLAG_Z)) & 0xFF;
                    if ((tmp & 0x100) != 0) {
                        Regs.F |= FLAG_C;
                    }
                    Regs.A = tmp & 0xFF;
                    if (Regs.A == 0) {
                        Regs.F |= FLAG_Z;
                    }
                }
                break;
                case 0x28:
                    if ((Regs.F & FLAG_Z) != 0) /*	   JR Z,n8 */ {
                        byte offset;

                        offset = (byte) mem_ReadByte(Regs.PC);
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                        Regs.PC = (Regs.PC + offset) & 0xFFFF;

                        ICycles += 4;
                    } else {
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    }
                    break;
                case 0x29:
                    ADD_HL_RR(HL());/*	   ADD HL,HL */
                    break;
                case 0x2A:
                    Regs.A = mem_ReadByte(HL());/*	   LD A,(HL+) */
                    HL((HL() + 1) & 0xFFFF);
                    break;
                case 0x2B:
                    /*	   DEC HL */
                    HL((HL() - 1) & 0xFFFF);
                    break;
                case 0x2C:
                    Regs.L = INC_8BIT(Regs.L);/*	   INC L */
                    break;
                case 0x2D:
                    Regs.L = DEC_8BIT(Regs.L);/*	   DEC L */
                    break;
                case 0x2E:
                    /*	   LD L,n8 */
                    Regs.L = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x2F:
                    /*	   CPL */
                    Regs.A = (~Regs.A) & 0xFF;
                    Regs.F |= FLAG_N | FLAG_H;
                    //return 4;
                    break;
                case 0x30:
                    if ((Regs.F & FLAG_C) != 0)/*	   JR NC,n8 */ {
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    } else {
                        byte offset;
                        offset = (byte) mem_ReadByte(Regs.PC);
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                        Regs.PC = (Regs.PC + offset) & 0xFFFF;
                        ICycles += 4;
                    }
                    break;
                case 0x31:
                    Regs.SP = mem_ReadWord(Regs.PC);/*	   LD SP,n16 */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0x32:
                    mem_WriteByte(HL(), Regs.A);/*	   LD (HL-),A */
                    HL((HL() - 1) & 0xFFFF);
                    break;
                case 0x33:
                    /*	   INC SP */
                    Regs.SP = (Regs.SP + 1) & 0xFFFF;
                    break;
                case 0x34: /*	   INC (HL) */ {
                    int r, f;
                    f = (Regs.F & FLAG_C) & 0xFF;
                    r = (mem_ReadByte(HL()) + 1) & 0xFF;
                    mem_WriteByte(HL(), r);

                    if (r == 0) {
                        f |= FLAG_Z;
                    }

                    if ((r & 0xF) == 0) {
                        f |= FLAG_H;
                    }

                    Regs.F = f & 0xFF;
                }
                break;
                case 0x35: /*	   DEC (HL) */ {
                    int addr = HL();
                    int r, f;

                    f = ((Regs.F & FLAG_C) | FLAG_N) & 0xFF;
                    r = mem_ReadByte(addr);
                    r = (r - 1) & 0xFF;
                    mem_WriteByte(addr, r);

                    if (r == 0) {
                        f |= FLAG_Z;
                    }

                    if ((r & 0xF) == 0xF) {
                        f |= FLAG_H;
                    }

                    Regs.F = f & 0xFF;
                }
                break;
                case 0x36:
                    /*	   LD (HL),n8 */
 /* FIXED / broken ? */
                    mem_WriteByte(HL(), mem_ReadByte(Regs.PC));
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x37:
                    /*	   SCF */

                    Regs.F = ((Regs.F & FLAG_Z) | FLAG_C) & 0xFF;
                    break;
                case 0x38:
                    /*	   JR C,n8 */

                    if ((Regs.F & FLAG_C) != 0) {
                        byte offset;

                        offset = (byte) mem_ReadByte(Regs.PC);
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                        Regs.PC = (Regs.PC + offset) & 0xFFFF;

                        ICycles += 4;
                    } else {
                        Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    }
                    break;
                case 0x39:
                    /*	   ADD HL,SP */
                    ADD_HL_RR(Regs.SP);
                    break;
                case 0x3A:
                    /*	   LD A,(HL-) */
                    Regs.A = mem_ReadByte(HL());
                    HL((HL() - 1) & 0xFFFF);
                    break;
                case 0x3B:
                    /*	   DEC SP */
                    Regs.SP = (Regs.SP - 1) & 0xFFFF;
                    break;
                case 0x3C:
                    Regs.A = INC_8BIT(Regs.A);/*	   INC	   A */
                    break;
                case 0x3D:
                    Regs.A = DEC_8BIT(Regs.A);/*	   DEC	   A */
                    break;
                case 0x3E:
                    Regs.A = mem_ReadByte(Regs.PC);/*	   LD A,n8 */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0x3F:
                    /*	   CCF */

                    Regs.F = ((Regs.F & FLAG_Z) | ((Regs.F & FLAG_C) != 0 ? 0 : FLAG_C)) & 0xff;
                    break;
                case 0x40:
                    /*	   LD B,B */
                    break;
                case 0x41:
                    /*	   LD B,C */
                    Regs.B = Regs.C;
                    break;
                case 0x42:
                    /*	   LD B,D */
                    Regs.B = Regs.D;
                    break;
                case 0x43:
                    /*	   LD B,E */
                    Regs.B = Regs.E;
                    break;
                case 0x44:
                    /*	   LD B,H */
                    Regs.B = Regs.H;
                    break;
                case 0x45:
                    /*	   LD B,L */
                    Regs.B = Regs.L;
                    break;
                case 0x46:
                    Regs.B = mem_ReadByte(HL());/*	   LD B,(HL) */
                    break;
                case 0x47:
                    Regs.B = Regs.A;/*	   LD B,A */
                    break;
                case 0x48:
                    /*	   LD C,B */
                    Regs.C = Regs.B;
                    break;
                case 0x49:
                    /*	   LD C,C */
                    break;
                case 0x4A:
                    /*	   LD C,D */
                    Regs.C = Regs.D;
                    break;
                case 0x4B:
                    /*	   LD C,E */
                    Regs.C = Regs.E;
                    break;
                case 0x4C:
                    /*	   LD C,H */
                    Regs.C = Regs.H;
                    break;
                case 0x4D:
                    /*	   LD C,L */
                    Regs.C = Regs.L;
                    break;
                case 0x4E:
                    Regs.C = mem_ReadByte(HL());/*	   LD C,(HL) */
                    break;
                case 0x4F:
                    /*	   LD C,A */
                    Regs.C = Regs.A;
                    break;
                case 0x50:
                    /*	   LD D,B */
                    Regs.D = Regs.B;
                    break;
                case 0x51:
                    /*	   LD D,C */
                    Regs.D = Regs.C;
                    break;
                case 0x52:
                    /*	   LD D,D */
                    break;
                case 0x53:
                    /*	   LD D,E */
                    Regs.D = Regs.E;
                    break;
                case 0x54:
                    /*	   LD D,H */
                    Regs.D = Regs.H;
                    break;
                case 0x55:
                    /*	   LD D,L */
                    Regs.D = Regs.L;
                    break;
                case 0x56:
                    Regs.D = mem_ReadByte(HL());/*	   LD D,(HL) */
                    break;
                case 0x57:
                    /*	   LD D,A */
                    Regs.D = Regs.A;
                    break;
                case 0x58:
                    /*	   LD E,B */
                    Regs.E = Regs.B;
                    break;
                case 0x59:
                    /*	   LD E,C */
                    Regs.E = Regs.C;
                    break;
                case 0x5A:
                    /*	   LD E,D */
                    Regs.E = Regs.D;
                    break;
                case 0x5B:
                    /*	   LD E,E */
                    break;
                case 0x5C:
                    /*	   LD E,H */
                    Regs.E = Regs.H;
                    break;
                case 0x5D:
                    /*	   LD E,L */
                    Regs.E = Regs.L;
                    break;
                case 0x5E:
                    Regs.E = mem_ReadByte(HL());/*	   LD E,(HL) */
                    break;
                case 0x5F:
                    /*	   LD E,A */
                    Regs.E = Regs.A;
                    break;
                case 0x60:
                    /*	   LD H,B */
                    Regs.H = Regs.B;
                    break;
                case 0x61:
                    /*	   LD H,C */
                    Regs.H = Regs.C;
                    break;
                case 0x62:
                    /*	   LD H,D */
                    Regs.H = Regs.D;
                    break;
                case 0x63:
                    /*	   LD H,E */
                    Regs.H = Regs.E;
                    break;
                case 0x64:
                    /*	   LD H,H */
                    break;
                case 0x65:
                    /*	   LD H,L */
                    Regs.H = Regs.L;
                    break;
                case 0x66:
                    Regs.H = mem_ReadByte(HL());/*	   LD H,(HL) */
                    break;
                case 0x67:
                    /*	   LD H,A */
                    Regs.H = Regs.A;
                    break;
                case 0x68:
                    /*	   LD L,B */
                    Regs.L = Regs.B;
                    break;
                case 0x69:
                    /*	   LD L,C */
                    Regs.L = Regs.C;
                    break;
                case 0x6A:
                    /*	   LD L,D */
                    Regs.L = Regs.D;
                    break;
                case 0x6B:
                    /*	   LD L,E */
                    Regs.L = Regs.E;
                    break;
                case 0x6C:
                    /*	   LD L,H */
                    Regs.L = Regs.H;
                    break;
                case 0x6D:
                    /*	   LD L,L */
                    break;
                case 0x6E:
                    /*	   LD L,(HL) */
                    Regs.L = mem_ReadByte(HL());
                    break;
                case 0x6F:
                    /*	   LD L,A */
                    Regs.L = Regs.A;
                    break;
                case 0x70:
                    /*	   LD (HL),B */
                    mem_WriteByte(HL(), Regs.B);
                    break;
                case 0x71:
                    /*	   LD (HL),C */
                    mem_WriteByte(HL(), Regs.C);
                    break;
                case 0x72:
                    /*	   LD (HL),D */
                    mem_WriteByte(HL(), Regs.D);
                    break;
                case 0x73:
                    /*	   LD (HL),E */
                    mem_WriteByte(HL(), Regs.E);
                    break;
                case 0x74:
                    /*	   LD (HL),H */
                    mem_WriteByte(HL(), Regs.H);
                    break;
                case 0x75:
                    /*	   LD (HL),L */
                    mem_WriteByte(HL(), Regs.L);
                    break;
                case 0x76: /*	   HALT */ {
                    int skip_cycles;
                    Regs.enable |= HALTED;
                    CheckInterrupts = 1;
                    Regs.PC--;

                    /* Calculate nr of cycles which can be skipped */
                    skip_cycles = (0x100 << gb_timer_shift) - gb_timer_count;
                    if (skip_cycles > z80gb_ICount[0]) {
                        skip_cycles = z80gb_ICount[0];
                    }

                    /* round cycles to multiple of 4 always round upwards */
                    skip_cycles = (skip_cycles + 3) & ~3;
                    if (skip_cycles > ICycles) {
                        ICycles += skip_cycles - ICycles;
                    }
                }
                break;
                case 0x77:
                    mem_WriteByte(HL(), Regs.A);/*	   LD (HL),A */
                    break;
                case 0x78:
                    Regs.A = Regs.B;/*	   LD A,B */
                    break;
                case 0x79:
                    Regs.A = Regs.C;/*	   LD A,C */
                    break;
                case 0x7A:
                    Regs.A = Regs.D;/*	   LD A,D */
                    break;
                case 0x7B:
                    Regs.A = Regs.E;/*	   LD A,E */
                    break;
                case 0x7C:
                    Regs.A = Regs.H;/*	   LD A,H */
                    break;
                case 0x7D:
                    Regs.A = Regs.L;/*	   LD A,L */
                    break;
                case 0x7E:
                    /*	   LD A,(HL) */
                    Regs.A = mem_ReadByte(HL());
                    break;
                case 0x7F:
                    /*	   LD A,A */
                    break;
                case 0x80:
                    /*	   ADD A,B */
                    ADD_A_X(Regs.B);
                    break;
                case 0x81:
                    /*	   ADD A,C */
                    ADD_A_X(Regs.C);
                    break;
                case 0x82:
                    /*	   ADD A,D */
                    ADD_A_X(Regs.D);
                    break;
                case 0x83:
                    /*	   ADD A,E */
                    ADD_A_X(Regs.E);
                    break;
                case 0x84:
                    /*	   ADD A,H */
                    ADD_A_X(Regs.H);
                    break;
                case 0x85:
                    /*	   ADD A,L */
                    ADD_A_X(Regs.L);
                    break;
                case 0x86: /*	   ADD A,(HL) */ {
                    int x1 = mem_ReadByte(HL());
                    ADD_A_X(x1);
                }
                break;
                case 0x87:
                    /*	   ADD A,A */
                    ADD_A_X(Regs.A);
                    break;
                case 0x88:
                    /*	   ADC A,B */
                    ADC_A_X(Regs.B);
                    break;
                case 0x89:
                    /*	   ADC A,C */
                    ADC_A_X(Regs.C);
                    break;
                case 0x8A:
                    /*	   ADC A,D */
                    ADC_A_X(Regs.D);
                    break;
                case 0x8B:
                    /*	   ADC A,E */
                    ADC_A_X(Regs.E);
                    break;
                case 0x8C:
                    /*	   ADC A,H */
                    ADC_A_X(Regs.H);
                    break;
                case 0x8D:
                    /*	   ADC A,L */
                    ADC_A_X(Regs.L);
                    break;
                case 0x8E: /*	   ADC A,(HL) */ {
                    int x1 = mem_ReadByte(HL());
                    ADC_A_X(x1);
                }
                break;
                case 0x8F:
                    /*	   ADC A,A */
                    ADC_A_X(Regs.A);
                    break;
                case 0x90:
                    /*	   SUB A,B */
                    SUB_A_X(Regs.B);
                    break;
                case 0x91:
                    /*	   SUB A,C */
                    SUB_A_X(Regs.C);
                    break;
                case 0x92:
                    /*	   SUB A,D */
                    SUB_A_X(Regs.D);
                    break;
                case 0x93:
                    /*	   SUB A,E */
                    SUB_A_X(Regs.E);
                    break;
                case 0x94:
                    /*	   SUB A,H */
                    SUB_A_X(Regs.H);
                    break;
                case 0x95:
                    /*	   SUB A,L */
                    SUB_A_X(Regs.L);
                    break;
                case 0x96: /*	   SUB A,(HL) */ {
                    int x1 = mem_ReadByte(HL());
                    SUB_A_X(x1);
                }
                break;
                case 0x97:
                    /*	   SUB A,A */
                    SUB_A_X(Regs.A);
                    break;
                case 0x98:
                    /*	   SBC A,B */

                    SBC_A_X(Regs.B);
                    break;
                case 0x99:
                    /*	   SBC A,C */

                    SBC_A_X(Regs.C);
                    break;
                case 0x9A:
                    /*	   SBC A,D */

                    SBC_A_X(Regs.D);
                    break;
                case 0x9B:
                    /*	   SBC A,E */

                    SBC_A_X(Regs.E);
                    break;
                case 0x9C:
                    /*	   SBC A,H */

                    SBC_A_X(Regs.H);
                    break;
                case 0x9D:
                    /*	   SBC A,L */

                    SBC_A_X(Regs.L);
                    break;
                case 0x9E:
                    /*	   SBC A,(HL) */
                    x = mem_ReadByte(HL());
                    SBC_A_X(x);
                    break;
                case 0x9F:
                    /*	   SBC A,A */

                    SBC_A_X(Regs.A);
                    break;
                case 0xA0:
                    /*	   AND A,B */
                    AND_A_X(Regs.B);
                    break;
                case 0xA1:
                    /*	   AND A,C */
                    AND_A_X(Regs.C);
                    break;
                case 0xA2:
                    /*	   AND A,D */
                    AND_A_X(Regs.D);
                    break;
                case 0xA3:
                    /*	   AND A,E */
                    AND_A_X(Regs.E);
                    break;
                case 0xA4:
                    /*	   AND A,H */
                    AND_A_X(Regs.H);
                    break;
                case 0xA5:
                    /*	   AND A,L */
                    AND_A_X(Regs.L);
                    break;
                case 0xA6: /*	   AND A,(HL) */ {
                    int x1 = mem_ReadByte(HL());

                    AND_A_X(x1);
                }
                break;
                case 0xA7:
                    /*	   AND A,A */
                    Regs.F = (Regs.A == 0) ? (FLAG_H | FLAG_Z) : FLAG_H;
                    break;
                case 0xA8:
                    XOR_A_X(Regs.B);/*	   XOR A,B */
                    break;
                case 0xA9:
                    XOR_A_X(Regs.C);/*	   XOR A,C */
                    break;
                case 0xAA:
                    XOR_A_X(Regs.D);/*	   XOR A,D */
                    break;
                case 0xAB:
                    XOR_A_X(Regs.E);/*	   XOR A,E */
                    break;
                case 0xAC:
                    XOR_A_X(Regs.H);/*	   XOR A,H */
                    break;
                case 0xAD:
                    XOR_A_X(Regs.L);/*	   XOR A,L */
                    break;
                case 0xAE: /*	   XOR A,(HL) */ {
                    int x1 = mem_ReadByte(HL());
                    XOR_A_X(x1);
                }
                break;
                case 0xAF:
                    XOR_A_X(Regs.A);/*	   XOR A,A */
                    break;
                case 0xB0:
                    OR_A_X(Regs.B);/*	   OR A,B */
                    break;
                case 0xB1:
                    OR_A_X(Regs.C);/*	   OR A,C */
                    break;
                case 0xB2:
                    OR_A_X(Regs.D);/*	   OR A,D */
                    break;
                case 0xB3:
                    OR_A_X(Regs.E);/*	   OR A,E */
                    break;
                case 0xB4:
                    OR_A_X(Regs.H);/*	   OR A,H */
                    break;
                case 0xB5:
                    OR_A_X(Regs.L);/*	   OR A,L */
                    break;
                case 0xB6: /*	   OR A,(HL) */ {
                    int x1 = mem_ReadByte(HL());
                    OR_A_X(x1);
                }
                break;
                case 0xB7:
                    OR_A_X(Regs.A);/*	   OR A,A */
                    break;
                case 0xB8:
                    /*	   CP A,B */
                    CP_A_X(Regs.B);
                    break;
                case 0xB9:
                    /*	   CP A,C */
                    CP_A_X(Regs.C);
                    break;
                case 0xBA:
                    /*	   CP A,D */
                    CP_A_X(Regs.D);
                    break;
                case 0xBB:
                    /*	   CP A,E */
                    CP_A_X(Regs.E);
                    break;
                case 0xBC:
                    /*	   CP A,H */
                    CP_A_X(Regs.H);
                    break;
                case 0xBD:
                    /*	   CP A,L */
                    CP_A_X(Regs.L);
                    break;
                case 0xBE: /*	   CP A,(HL) */ {
                    int x1 = mem_ReadByte(HL());
                    CP_A_X(x1);
                }
                break;
                case 0xBF:
                    /*	   CP A,A */
                    CP_A_X(Regs.A);
                    break;
                case 0xC0:
                    /*	   RET NZ */
                    if ((Regs.F & FLAG_Z) == 0) {
                        Regs.PC = mem_ReadWord(Regs.SP);
                        Regs.SP = (Regs.SP + 2) & 0xFFFF;
                        ICycles += 12;
                    }
                    break;
                case 0xC1:
                    BC(mem_ReadWord(Regs.SP));/*	   POP BC */
                    Regs.SP = (Regs.SP + 2) & 0xFFFF;
                    break;
                case 0xC2:
                    /*	   JP NZ,n16 */
                    if ((Regs.F & FLAG_Z) != 0) {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    } else {
                        Regs.PC = mem_ReadWord(Regs.PC);
                        ICycles += 4;
                    }
                    break;
                case 0xC3:
                    Regs.PC = mem_ReadWord(Regs.PC);/*	   JP n16 */
                    break;
                case 0xC4:
                    if ((Regs.F & FLAG_Z) != 0)/*	   CALL NZ,n16 */ {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    } else {
                        int PC;
                        PC = mem_ReadWord(Regs.PC);
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;

                        Regs.SP = (Regs.SP - 2) & 0xFFFF;
                        mem_WriteWord(Regs.SP, Regs.PC);
                        Regs.PC = PC & 0xFFFF;
                        ICycles += 12;
                    }
                    break;
                case 0xC5:
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;/*	   PUSH BC */
                    mem_WriteWord(Regs.SP, BC());
                    break;
                case 0xC6: /*	   ADD A,n8 */ {
                    int x1 = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    ADD_A_X(x1);
                }
                break;
                case 0xC7: /*	   RST 0 */ {
                    int PC;
                    PC = Regs.PC;
                    Regs.PC = 0;
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, PC);
                }
                break;
                case 0xC8:
                    /*	   RET Z */
                    if ((Regs.F & FLAG_Z) != 0) {
                        Regs.PC = mem_ReadWord(Regs.SP);
                        Regs.SP = (Regs.SP + 2) & 0xFFFF;
                        ICycles += 12;
                    }
                    break;
                case 0xC9:
                    Regs.PC = mem_ReadWord(Regs.SP);/*	   RET */
                    Regs.SP = (Regs.SP + 2) & 0xFFFF;
                    break;
                case 0xCA:
                    /*	   JP Z,n16 */
                    if ((Regs.F & FLAG_Z) != 0) {
                        Regs.PC = mem_ReadWord(Regs.PC);
                        ICycles += 4;
                    } else {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    }
                    break;
                case 0xCB:
                    x = mem_ReadByte(Regs.PC);/*	   PREFIX! */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    ICycles += CyclesCB[x];
                    switch (x) {
                        case 0x00:
                            /*      RLC B */
                            Regs.B = RLC_8BIT(Regs.B);
                            break;
                        case 0x01:
                            /*      RLC C */
                            Regs.C = RLC_8BIT(Regs.C);
                            break;
                        case 0x02:
                            /*      RLC D */
                            Regs.D = RLC_8BIT(Regs.D);
                            break;
                        case 0x03:
                            /*      RLC E */
                            Regs.E = RLC_8BIT(Regs.E);
                            break;
                        case 0x04:
                            /*      RLC H */
                            Regs.H = RLC_8BIT(Regs.H);
                            break;
                        case 0x05:
                            /*      RLC L */
                            Regs.L = RLC_8BIT(Regs.L);
                            break;
                        case 0x06:
                            /*      RLC (HL) */

                            x = mem_ReadByte(HL());
                            x = RLC_8BIT(x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x07:
                            /*      RLC A */

                            Regs.A = RLC_8BIT(Regs.A);
                            break;
                        case 0x08:
                            /*      RRC B */

                            Regs.B = RRC_8BIT(Regs.B);
                            break;
                        case 0x09:
                            /*      RRC C */

                            Regs.C = RRC_8BIT(Regs.C);
                            break;
                        case 0x0A:
                            /*      RRC D */

                            Regs.D = RRC_8BIT(Regs.D);
                            break;
                        case 0x0B:
                            /*      RRC E */

                            Regs.E = RRC_8BIT(Regs.E);
                            break;
                        case 0x0C:
                            /*      RRC H */

                            Regs.H = RRC_8BIT(Regs.H);
                            break;
                        case 0x0D:
                            /*      RRC L */

                            Regs.L = RRC_8BIT(Regs.L);
                            break;
                        case 0x0E:
                            /*      RRC (HL) */

                            x = mem_ReadByte(HL());
                            x = RRC_8BIT(x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x0F:
                            /*      RRC A */

                            Regs.A = RRC_8BIT(Regs.A);
                            break;
                        case 0x10:
                            /*      RL B */

                            Regs.B = RL_8BIT(Regs.B);
                            break;
                        case 0x11:
                            /*      RL C */

                            Regs.C = RL_8BIT(Regs.C);
                            break;
                        case 0x12:
                            /*      RL D */

                            Regs.D = RL_8BIT(Regs.D);
                            break;
                        case 0x13:
                            /*      RL E */

                            Regs.E = RL_8BIT(Regs.E);
                            break;
                        case 0x14:
                            /*      RL H */

                            Regs.H = RL_8BIT(Regs.H);
                            break;
                        case 0x15:
                            /*      RL L */

                            Regs.L = RL_8BIT(Regs.L);
                            break;
                        case 0x16:
                            /*      RL (HL) */

                            x = mem_ReadByte(HL());
                            x = RL_8BIT(x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x17:
                            /*      RL A */

                            Regs.A = RL_8BIT(Regs.A);
                            break;
                        case 0x18:
                            /*      RR B */
                            Regs.B = RR_8BIT(Regs.B);
                            break;
                        case 0x19:
                            Regs.C = RR_8BIT(Regs.C);/*      RR C */
                            break;
                        case 0x1A:
                            Regs.D = RR_8BIT(Regs.D);/*      RR D */
                            break;
                        case 0x1B:
                            Regs.E = RR_8BIT(Regs.E);/*      RR E */
                            break;
                        case 0x1C:
                            /*      RR H */
                            Regs.H = RR_8BIT(Regs.H);
                            break;
                        case 0x1D:
                            /*      RR L */
                            Regs.L = RR_8BIT(Regs.L);
                            break;
                        case 0x1E: /*      RR (HL) */ {
                            int x1 = mem_ReadByte(HL());
                            x1 = RR_8BIT(x1);
                            mem_WriteByte(HL(), x1);
                        }
                        break;
                        case 0x1F:
                            /*      RR A */
                            Regs.A = RR_8BIT(Regs.A);
                            break;
                        case 0x20:
                            /*      SLA B */

                            Regs.B = SLA_8BIT(Regs.B);
                            break;
                        case 0x21:
                            /*      SLA C */

                            Regs.C = SLA_8BIT(Regs.C);
                            break;
                        case 0x22:
                            /*      SLA D */

                            Regs.D = SLA_8BIT(Regs.D);
                            break;
                        case 0x23:
                            /*      SLA E */

                            Regs.E = SLA_8BIT(Regs.E);
                            break;
                        case 0x24:
                            /*      SLA H */

                            Regs.H = SLA_8BIT(Regs.H);
                            break;
                        case 0x25:
                            /*      SLA L */

                            Regs.L = SLA_8BIT(Regs.L);
                            break;
                        case 0x26:
                            /*      SLA (HL) */

                            x = mem_ReadByte(HL());
                            x = SLA_8BIT(x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x27:
                            /*      SLA A */
                            Regs.A = SLA_8BIT(Regs.A);
                            break;
                        case 0x28:
                            /*      SRA B */

                            Regs.B = SRA_8BIT(Regs.B);
                            break;
                        case 0x29:
                            /*      SRA C */

                            Regs.C = SRA_8BIT(Regs.C);
                            break;
                        case 0x2A:
                            /*      SRA D */

                            Regs.D = SRA_8BIT(Regs.D);
                            break;
                        case 0x2B:
                            /*      SRA E */

                            Regs.E = SRA_8BIT(Regs.E);
                            break;
                        case 0x2C:
                            /*      SRA H */

                            Regs.H = SRA_8BIT(Regs.H);
                            break;
                        case 0x2D:
                            /*      SRA L */

                            Regs.L = SRA_8BIT(Regs.L);
                            break;
                        case 0x2E:
                            /*      SRA (HL) */

                            x = mem_ReadByte(HL());
                            x = SRA_8BIT(x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x2F:
                            /*      SRA A */

                            Regs.A = SRA_8BIT(Regs.A);
                            break;
                        case 0x30:
                            /*      SWAP B */

                            Regs.B = SWAP_8BIT(Regs.B);
                            break;
                        case 0x31:
                            /*      SWAP C */

                            Regs.C = SWAP_8BIT(Regs.C);
                            break;
                        case 0x32:
                            /*      SWAP D */

                            Regs.D = SWAP_8BIT(Regs.D);
                            break;
                        case 0x33:
                            /*      SWAP E */

                            Regs.E = SWAP_8BIT(Regs.E);
                            break;
                        case 0x34:
                            /*      SWAP H */

                            Regs.H = SWAP_8BIT(Regs.H);
                            break;
                        case 0x35:
                            /*      SWAP L */
                            Regs.L = SWAP_8BIT(Regs.L);
                            break;
                        case 0x36: /*      SWAP (HL) */ {
                            int x1 = mem_ReadByte(HL());
                            x1 = SWAP_8BIT(x1);
                            mem_WriteByte(HL(), x1);
                        }
                        break;
                        case 0x37:
                            /*      SWAP A */
                            Regs.A = SWAP_8BIT(Regs.A);
                            break;
                        case 0x38:
                            Regs.B = SRL_8BIT(Regs.B);/*      SRL B */
                            break;
                        case 0x39:
                            /*      SRL C */

                            Regs.C = SRL_8BIT(Regs.C);
                            break;
                        case 0x3A:
                            /*      SRL D */

                            Regs.D = SRL_8BIT(Regs.D);
                            break;
                        case 0x3B:
                            /*      SRL E */

                            Regs.E = SRL_8BIT(Regs.E);
                            break;
                        case 0x3C:
                            /*      SRL H */

                            Regs.H = SRL_8BIT(Regs.H);
                            break;
                        case 0x3D:
                            /*      SRL L */

                            Regs.L = SRL_8BIT(Regs.L);
                            break;
                        case 0x3E: /*      SRL (HL) */ {

                            int x1 = mem_ReadByte(HL());
                            x1 = SRL_8BIT(x1);
                            mem_WriteByte(HL(), x1);
                        }
                        break;
                        case 0x3F:
                            /*      SRL A */

                            Regs.A = SRL_8BIT(Regs.A);
                            break;
                        case 0x40:
                            /*      BIT 0,B */

                            BIT_8BIT(0, Regs.B);
                            break;
                        case 0x41:
                            /*      BIT 0,C */

                            BIT_8BIT(0, Regs.C);
                            break;
                        case 0x42:
                            /*      BIT 0,D */

                            BIT_8BIT(0, Regs.D);
                            break;
                        case 0x43:
                            /*      BIT 0,E */

                            BIT_8BIT(0, Regs.E);
                            break;
                        case 0x44:
                            /*      BIT 0,H */

                            BIT_8BIT(0, Regs.H);
                            break;
                        case 0x45:
                            /*      BIT 0,L */

                            BIT_8BIT(0, Regs.L);
                            break;
                        case 0x46: /*      BIT 0,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(0, x1);
                        }
                        break;
                        case 0x47:
                            /*      BIT 0,A */

                            BIT_8BIT(0, Regs.A);
                            break;
                        case 0x48:
                            /*      BIT 1,B */

                            BIT_8BIT(1, Regs.B);
                            break;
                        case 0x49:
                            /*      BIT 1,C */

                            BIT_8BIT(1, Regs.C);
                            break;
                        case 0x4A:
                            /*      BIT 1,D */

                            BIT_8BIT(1, Regs.D);
                            break;
                        case 0x4B:
                            /*      BIT 1,E */

                            BIT_8BIT(1, Regs.E);
                            break;
                        case 0x4C:
                            /*      BIT 1,H */

                            BIT_8BIT(1, Regs.H);
                            break;
                        case 0x4D:
                            /*      BIT 1,L */

                            BIT_8BIT(1, Regs.L);
                            break;
                        case 0x4E: /*      BIT 1,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(1, x1);
                        }
                        break;
                        case 0x4F:
                            /*      BIT 1,A */

                            BIT_8BIT(1, Regs.A);
                            break;
                        case 0x50:
                            /*      BIT 2,B */

                            BIT_8BIT(2, Regs.B);
                            break;
                        case 0x51:
                            /*      BIT 2,C */

                            BIT_8BIT(2, Regs.C);
                            break;
                        case 0x52:
                            /*      BIT 2,D */

                            BIT_8BIT(2, Regs.D);
                            break;
                        case 0x53:
                            /*      BIT 2,E */

                            BIT_8BIT(2, Regs.E);
                            break;
                        case 0x54:
                            /*      BIT 2,H */

                            BIT_8BIT(2, Regs.H);
                            break;
                        case 0x55:
                            /*      BIT 2,L */

                            BIT_8BIT(2, Regs.L);
                            break;
                        case 0x56: {
                            /*      BIT 2,(HL) */

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(2, x1);
                        }
                        break;
                        case 0x57:
                            /*      BIT 2,A */

                            BIT_8BIT(2, Regs.A);
                            break;
                        case 0x58:
                            /*      BIT 3,B */

                            BIT_8BIT(3, Regs.B);
                            break;
                        case 0x59:
                            /*      BIT 3,C */

                            BIT_8BIT(3, Regs.C);
                            break;
                        case 0x5A:
                            /*      BIT 3,D */

                            BIT_8BIT(3, Regs.D);
                            break;
                        case 0x5B:
                            /*      BIT 3,E */

                            BIT_8BIT(3, Regs.E);
                            break;
                        case 0x5C:
                            /*      BIT 3,H */

                            BIT_8BIT(3, Regs.H);
                            break;
                        case 0x5D:
                            /*      BIT 3,L */

                            BIT_8BIT(3, Regs.L);
                            break;
                        case 0x5E: /*      BIT 3,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(3, x1);
                        }
                        break;
                        case 0x5F:
                            /*      BIT 3,A */

                            BIT_8BIT(3, Regs.A);
                            break;
                        case 0x60:
                            /*      BIT 4,B */

                            BIT_8BIT(4, Regs.B);
                            break;
                        case 0x61:
                            /*      BIT 4,C */

                            BIT_8BIT(4, Regs.C);
                            break;
                        case 0x62:
                            /*      BIT 4,D */

                            BIT_8BIT(4, Regs.D);
                            break;
                        case 0x63:
                            /*      BIT 4,E */

                            BIT_8BIT(4, Regs.E);
                            break;
                        case 0x64:
                            /*      BIT 4,H */

                            BIT_8BIT(4, Regs.H);
                            break;
                        case 0x65:
                            /*      BIT 4,L */

                            BIT_8BIT(4, Regs.L);
                            break;
                        case 0x66: /*      BIT 4,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(4, x1);
                        }
                        break;
                        case 0x67:
                            /*      BIT 4,A */

                            BIT_8BIT(4, Regs.A);
                            break;
                        case 0x68:
                            /*      BIT 5,B */

                            BIT_8BIT(5, Regs.B);
                            break;
                        case 0x69:
                            /*      BIT 5,C */

                            BIT_8BIT(5, Regs.C);
                            break;
                        case 0x6A:
                            /*      BIT 5,D */

                            BIT_8BIT(5, Regs.D);
                            break;
                        case 0x6B:
                            /*      BIT 5,E */

                            BIT_8BIT(5, Regs.E);
                            break;
                        case 0x6C:
                            /*      BIT 5,H */

                            BIT_8BIT(5, Regs.H);
                            break;
                        case 0x6D:
                            /*      BIT 5,L */

                            BIT_8BIT(5, Regs.L);
                            break;
                        case 0x6E: /*      BIT 5,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(5, x1);
                        }
                        break;
                        case 0x6F:
                            /*      BIT 5,A */

                            BIT_8BIT(5, Regs.A);
                            break;
                        case 0x70:
                            /*      BIT 6,B */

                            BIT_8BIT(6, Regs.B);
                            break;
                        case 0x71:
                            /*      BIT 6,C */

                            BIT_8BIT(6, Regs.C);
                            break;
                        case 0x72:
                            /*      BIT 6,D */

                            BIT_8BIT(6, Regs.D);
                            break;
                        case 0x73:
                            /*      BIT 6,E */

                            BIT_8BIT(6, Regs.E);
                            break;
                        case 0x74:
                            /*      BIT 6,H */

                            BIT_8BIT(6, Regs.H);
                            break;
                        case 0x75:
                            /*      BIT 6,L */

                            BIT_8BIT(6, Regs.L);
                            break;
                        case 0x76: /*      BIT 6,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(6, x1);
                        }
                        break;
                        case 0x77:
                            /*      BIT 6,A */

                            BIT_8BIT(6, Regs.A);
                            break;
                        case 0x78:
                            /*      BIT 7,B */

                            BIT_8BIT(7, Regs.B);
                            break;
                        case 0x79:
                            /*      BIT 7,C */

                            BIT_8BIT(7, Regs.C);
                            break;
                        case 0x7A:
                            /*      BIT 7,D */

                            BIT_8BIT(7, Regs.D);
                            break;
                        case 0x7B:
                            /*      BIT 7,E */

                            BIT_8BIT(7, Regs.E);
                            break;
                        case 0x7C:
                            /*      BIT 7,H */

                            BIT_8BIT(7, Regs.H);
                            break;
                        case 0x7D:
                            /*      BIT 7,L */

                            BIT_8BIT(7, Regs.L);
                            break;
                        case 0x7E: /*      BIT 7,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            BIT_8BIT(7, x1);
                        }
                        break;
                        case 0x7F:
                            /*      BIT 7,A */

                            BIT_8BIT(7, Regs.A);
                            break;
                        case 0x80:
                            /*      RES 0,B */

                            Regs.B = RES_8BIT(0, Regs.B);
                            break;
                        case 0x81:
                            /*      RES 0,C */

                            Regs.C = RES_8BIT(0, Regs.C);
                            break;
                        case 0x82:
                            /*      RES 0,D */

                            Regs.D = RES_8BIT(0, Regs.D);
                            break;
                        case 0x83:
                            /*      RES 0,E */

                            Regs.E = RES_8BIT(0, Regs.E);
                            break;
                        case 0x84:
                            /*      RES 0,H */

                            Regs.H = RES_8BIT(0, Regs.H);
                            break;
                        case 0x85:
                            /*      RES 0,L */

                            Regs.L = RES_8BIT(0, Regs.L);
                            break;
                        case 0x86: /*      RES 0,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            x1 = RES_8BIT(0, x1);
                            mem_WriteByte(HL(), x1);
                        }
                        break;
                        case 0x87:
                            /*      RES 0,A */
                            Regs.A = RES_8BIT(0, Regs.A);
                            break;
                        case 0x88:
                            /*      RES 1,B */
                            Regs.B = RES_8BIT(1, Regs.B);
                            break;
                        case 0x89:
                            /*      RES 1,C */
                            Regs.C = RES_8BIT(1, Regs.C);
                            break;
                        case 0x8A:
                            /*      RES 1,D */
                            Regs.D = RES_8BIT(1, Regs.D);
                            break;
                        case 0x8B:
                            /*      RES 1,E */
                            Regs.E = RES_8BIT(1, Regs.E);
                            break;
                        case 0x8C:
                            /*      RES 1,H */
                            Regs.H = RES_8BIT(1, Regs.H);
                            break;
                        case 0x8D:
                            /*      RES 1,L */
                            Regs.L = RES_8BIT(1, Regs.L);
                            break;
                        case 0x8E:
                            /*      RES 1,(HL) */

                            x = mem_ReadByte(HL());
                            x = RES_8BIT(1, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x8F:
                            /*      RES 1,A */

                            Regs.A = RES_8BIT(1, Regs.A);
                            break;
                        case 0x90:
                            /*      RES 2,B */

                            Regs.B = RES_8BIT(2, Regs.B);
                            break;
                        case 0x91:
                            /*      RES 2,C */

                            Regs.C = RES_8BIT(2, Regs.C);
                            break;
                        case 0x92:
                            /*      RES 2,D */

                            Regs.D = RES_8BIT(2, Regs.D);
                            break;
                        case 0x93:
                            /*      RES 2,E */

                            Regs.E = RES_8BIT(2, Regs.E);
                            break;
                        case 0x94:
                            /*      RES 2,H */

                            Regs.H = RES_8BIT(2, Regs.H);
                            break;
                        case 0x95:
                            /*      RES 2,L */

                            Regs.L = RES_8BIT(2, Regs.L);
                            break;
                        case 0x96:
                            /*      RES 2,(HL) */

                            x = mem_ReadByte(HL());
                            x = RES_8BIT(2, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x97:
                            /*      RES 2,A */

                            Regs.A = RES_8BIT(2, Regs.A);
                            break;
                        case 0x98:
                            /*      RES 3,B */

                            Regs.B = RES_8BIT(3, Regs.B);
                            break;
                        case 0x99:
                            /*      RES 3,C */

                            Regs.C = RES_8BIT(3, Regs.C);
                            break;
                        case 0x9A:
                            /*      RES 3,D */

                            Regs.D = RES_8BIT(3, Regs.D);
                            break;
                        case 0x9B:
                            /*      RES 3,E */

                            Regs.E = RES_8BIT(3, Regs.E);
                            break;
                        case 0x9C:
                            /*      RES 3,H */

                            Regs.H = RES_8BIT(3, Regs.H);
                            break;
                        case 0x9D:
                            /*      RES 3,L */

                            Regs.L = RES_8BIT(3, Regs.L);
                            break;
                        case 0x9E:
                            /*      RES 3,(HL) */

                            x = mem_ReadByte(HL());
                            x = RES_8BIT(3, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0x9F:
                            /*      RES 3,A */

                            Regs.A = RES_8BIT(3, Regs.A);
                            break;
                        case 0xA0:
                            /*      RES 4,B */

                            Regs.B = RES_8BIT(4, Regs.B);
                            break;
                        case 0xA1:
                            /*      RES 4,C */

                            Regs.C = RES_8BIT(4, Regs.C);
                            break;
                        case 0xA2:
                            /*      RES 4,D */

                            Regs.D = RES_8BIT(4, Regs.D);
                            break;
                        case 0xA3:
                            /*      RES 4,E */

                            Regs.E = RES_8BIT(4, Regs.E);
                            break;
                        case 0xA4:
                            /*      RES 4,H */

                            Regs.H = RES_8BIT(4, Regs.H);
                            break;
                        case 0xA5:
                            /*      RES 4,L */

                            Regs.L = RES_8BIT(4, Regs.L);
                            break;
                        case 0xA6:
                            /*      RES 4,(HL) */

                            x = mem_ReadByte(HL());
                            x = RES_8BIT(4, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xA7:
                            /*      RES 4,A */

                            Regs.A = RES_8BIT(4, Regs.A);
                            break;
                        case 0xA8:
                            /*      RES 5,B */

                            Regs.B = RES_8BIT(5, Regs.B);
                            break;
                        case 0xA9:
                            /*      RES 5,C */

                            Regs.C = RES_8BIT(5, Regs.C);
                            break;
                        case 0xAA:
                            /*      RES 5,D */

                            Regs.D = RES_8BIT(5, Regs.D);
                            break;
                        case 0xAB:
                            /*      RES 5,E */

                            Regs.E = RES_8BIT(5, Regs.E);
                            break;
                        case 0xAC:
                            /*      RES 5,H */

                            Regs.H = RES_8BIT(5, Regs.H);
                            break;
                        case 0xAD:
                            /*      RES 5,L */

                            Regs.L = RES_8BIT(5, Regs.L);
                            break;
                        case 0xAE:
                            /*      RES 5,(HL) */

                            x = mem_ReadByte(HL());
                            x = RES_8BIT(5, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xAF:
                            /*      RES 5,A */

                            Regs.A = RES_8BIT(5, Regs.A);
                            break;
                        case 0xB0:
                            /*      RES 6,B */

                            Regs.B = RES_8BIT(6, Regs.B);
                            break;
                        case 0xB1:
                            /*      RES 6,C */

                            Regs.C = RES_8BIT(6, Regs.C);
                            break;
                        case 0xB2:
                            /*      RES 6,D */

                            Regs.D = RES_8BIT(6, Regs.D);
                            break;
                        case 0xB3:
                            /*      RES 6,E */

                            Regs.E = RES_8BIT(6, Regs.E);
                            break;
                        case 0xB4:
                            /*      RES 6,H */

                            Regs.H = RES_8BIT(6, Regs.H);
                            break;
                        case 0xB5:
                            /*      RES 6,L */

                            Regs.L = RES_8BIT(6, Regs.L);
                            break;
                        case 0xB6:
                            /*      RES 6,(HL) */

                            x = mem_ReadByte(HL());
                            x = RES_8BIT(6, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xB7:
                            /*      RES 6,A */
                            Regs.A = RES_8BIT(6, Regs.A);
                            break;
                        case 0xB8:
                            /*      RES 7,B */
                            Regs.B = RES_8BIT(7, Regs.B);
                            break;
                        case 0xB9:
                            /*      RES 7,C */
                            Regs.C = RES_8BIT(7, Regs.C);
                            break;
                        case 0xBA:
                            /*      RES 7,D */
                            Regs.D = RES_8BIT(7, Regs.D);
                            break;
                        case 0xBB:
                            /*      RES 7,E */
                            Regs.E = RES_8BIT(7, Regs.E);
                            break;
                        case 0xBC:
                            /*      RES 7,H */
                            Regs.H = RES_8BIT(7, Regs.H);
                            break;
                        case 0xBD:
                            /*      RES 7,L */
                            Regs.L = RES_8BIT(7, Regs.L);
                            break;
                        case 0xBE: /*      RES 7,(HL) */ {
                            int x1 = mem_ReadByte(HL());
                            x1 = RES_8BIT(7, x1);
                            mem_WriteByte(HL(), x1);
                        }
                        break;
                        case 0xBF:
                            /*      RES 7,A */
                            Regs.A = RES_8BIT(7, Regs.A);
                            break;
                        case 0xC0:
                            /*      SET 0,B */
                            Regs.B = SET_8BIT(0, Regs.B);
                            break;
                        case 0xC1:
                            /*      SET 0,C */

                            Regs.C = SET_8BIT(0, Regs.C);
                            break;
                        case 0xC2:
                            /*      SET 0,D */

                            Regs.D = SET_8BIT(0, Regs.D);
                            break;
                        case 0xC3:
                            /*      SET 0,E */

                            Regs.E = SET_8BIT(0, Regs.E);
                            break;
                        case 0xC4:
                            /*      SET 0,H */

                            Regs.H = SET_8BIT(0, Regs.H);
                            break;
                        case 0xC5:
                            /*      SET 0,L */

                            Regs.L = SET_8BIT(0, Regs.L);
                            break;
                        case 0xC6:
                            /*      SET 0,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(0, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xC7:
                            /*      SET 0,A */

                            Regs.A = SET_8BIT(0, Regs.A);
                            break;
                        case 0xC8:
                            /*      SET 1,B */

                            Regs.B = SET_8BIT(1, Regs.B);
                            break;
                        case 0xC9:
                            /*      SET 1,C */

                            Regs.C = SET_8BIT(1, Regs.C);
                            break;
                        case 0xCA:
                            /*      SET 1,D */

                            Regs.D = SET_8BIT(1, Regs.D);
                            break;
                        case 0xCB:
                            /*      SET 1,E */

                            Regs.E = SET_8BIT(1, Regs.E);
                            break;
                        case 0xCC:
                            /*      SET 1,H */

                            Regs.H = SET_8BIT(1, Regs.H);
                            break;
                        case 0xCD:
                            /*      SET 1,L */

                            Regs.L = SET_8BIT(1, Regs.L);
                            break;
                        case 0xCE:
                            /*      SET 1,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(1, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xCF:
                            /*      SET 1,A */

                            Regs.A = SET_8BIT(1, Regs.A);
                            break;
                        case 0xD0:
                            /*      SET 2,B */

                            Regs.B = SET_8BIT(2, Regs.B);
                            break;
                        case 0xD1:
                            /*      SET 2,C */

                            Regs.C = SET_8BIT(2, Regs.C);
                            break;
                        case 0xD2:
                            /*      SET 2,D */

                            Regs.D = SET_8BIT(2, Regs.D);
                            break;
                        case 0xD3:
                            /*      SET 2,E */

                            Regs.E = SET_8BIT(2, Regs.E);
                            break;
                        case 0xD4:
                            /*      SET 2,H */

                            Regs.H = SET_8BIT(2, Regs.H);
                            break;
                        case 0xD5:
                            /*      SET 2,L */

                            Regs.L = SET_8BIT(2, Regs.L);
                            break;
                        case 0xD6:
                            /*      SET 2,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(2, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xD7:
                            /*      SET 2,A */

                            Regs.A = SET_8BIT(2, Regs.A);
                            break;
                        case 0xD8:
                            /*      SET 3,B */

                            Regs.B = SET_8BIT(3, Regs.B);
                            break;
                        case 0xD9:
                            /*      SET 3,C */

                            Regs.C = SET_8BIT(3, Regs.C);
                            break;
                        case 0xDA:
                            /*      SET 3,D */

                            Regs.D = SET_8BIT(3, Regs.D);
                            break;
                        case 0xDB:
                            /*      SET 3,E */

                            Regs.E = SET_8BIT(3, Regs.E);
                            break;
                        case 0xDC:
                            /*      SET 3,H */

                            Regs.H = SET_8BIT(3, Regs.H);
                            break;
                        case 0xDD:
                            /*      SET 3,L */

                            Regs.L = SET_8BIT(3, Regs.L);
                            break;
                        case 0xDE:
                            /*      SET 3,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(3, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xDF:
                            /*      SET 3,A */

                            Regs.A = SET_8BIT(3, Regs.A);
                            break;
                        case 0xE0:
                            /*      SET 4,B */

                            Regs.B = SET_8BIT(4, Regs.B);
                            break;
                        case 0xE1:
                            /*      SET 4,C */

                            Regs.C = SET_8BIT(4, Regs.C);
                            break;
                        case 0xE2:
                            /*      SET 4,D */

                            Regs.D = SET_8BIT(4, Regs.D);
                            break;
                        case 0xE3:
                            /*      SET 4,E */

                            Regs.E = SET_8BIT(4, Regs.E);
                            break;
                        case 0xE4:
                            /*      SET 4,H */

                            Regs.H = SET_8BIT(4, Regs.H);
                            break;
                        case 0xE5:
                            /*      SET 4,L */

                            Regs.L = SET_8BIT(4, Regs.L);
                            break;
                        case 0xE6:
                            /*      SET 4,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(4, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xE7:
                            /*      SET 4,A */

                            Regs.A = SET_8BIT(4, Regs.A);
                            break;
                        case 0xE8:
                            /*      SET 5,B */

                            Regs.B = SET_8BIT(5, Regs.B);
                            break;
                        case 0xE9:
                            /*      SET 5,C */

                            Regs.C = SET_8BIT(5, Regs.C);
                            break;
                        case 0xEA:
                            /*      SET 5,D */

                            Regs.D = SET_8BIT(5, Regs.D);
                            break;
                        case 0xEB:
                            /*      SET 5,E */

                            Regs.E = SET_8BIT(5, Regs.E);
                            break;
                        case 0xEC:
                            /*      SET 5,H */

                            Regs.H = SET_8BIT(5, Regs.H);
                            break;
                        case 0xED:
                            /*      SET 5,L */

                            Regs.L = SET_8BIT(5, Regs.L);
                            break;
                        case 0xEE:
                            /*      SET 5,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(5, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xEF:
                            /*      SET 5,A */

                            Regs.A = SET_8BIT(5, Regs.A);
                            break;
                        case 0xF0:
                            /*      SET 6,B */

                            Regs.B = SET_8BIT(6, Regs.B);
                            break;
                        case 0xF1:
                            /*      SET 6,C */

                            Regs.C = SET_8BIT(6, Regs.C);
                            break;
                        case 0xF2:
                            /*      SET 6,D */

                            Regs.D = SET_8BIT(6, Regs.D);
                            break;
                        case 0xF3:
                            /*      SET 6,E */

                            Regs.E = SET_8BIT(6, Regs.E);
                            break;
                        case 0xF4:
                            /*      SET 6,H */

                            Regs.H = SET_8BIT(6, Regs.H);
                            break;
                        case 0xF5:
                            /*      SET 6,L */

                            Regs.L = SET_8BIT(6, Regs.L);
                            break;
                        case 0xF6:
                            /*      SET 6,(HL) */

                            x = mem_ReadByte(HL());
                            x = SET_8BIT(6, x);
                            mem_WriteByte(HL(), x);
                            break;
                        case 0xF7:
                            /*      SET 6,A */

                            Regs.A = SET_8BIT(6, Regs.A);
                            break;
                        case 0xF8:
                            /*      SET 7,B */

                            Regs.B = SET_8BIT(7, Regs.B);
                            break;
                        case 0xF9:
                            /*      SET 7,C */

                            Regs.C = SET_8BIT(7, Regs.C);
                            break;
                        case 0xFA:
                            /*      SET 7,D */

                            Regs.D = SET_8BIT(7, Regs.D);
                            break;
                        case 0xFB:
                            /*      SET 7,E */

                            Regs.E = SET_8BIT(7, Regs.E);
                            break;
                        case 0xFC:
                            /*      SET 7,H */

                            Regs.H = SET_8BIT(7, Regs.H);
                            break;
                        case 0xFD:
                            /*      SET 7,L */

                            Regs.L = SET_8BIT(7, Regs.L);
                            break;
                        case 0xFE: /*      SET 7,(HL) */ {

                            int x1 = mem_ReadByte(HL());
                            x1 = SET_8BIT(7, x1);
                            mem_WriteByte(HL(), x1);
                        }
                        break;
                        case 0xFF:
                            /*      SET 7,A */
                            Regs.A = SET_8BIT(7, Regs.A);
                            break;
                        default:
                            System.out.println("Unsupported CB 0x" + Integer.toHexString(x));
                            throw new UnsupportedOperationException("Unsupported");
                    }
                    break;
                case 0xCC:
                    /*	   CALL Z,n16 */

                    if ((Regs.F & FLAG_Z) != 0) {
                        int PC;
                        PC = mem_ReadWord(Regs.PC);
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;

                        Regs.SP = (Regs.SP - 2) & 0xFFFF;
                        mem_WriteWord(Regs.SP, Regs.PC);
                        Regs.PC = PC;
                        ICycles += 12;
                    } else {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    }
                    break;
                case 0xCD: /*	   CALL n16 */ {
                    int PC;
                    PC = mem_ReadWord(Regs.PC);
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = PC & 0xFFFF;
                }
                break;
                case 0xCE: /*	   ADC A,n8 */ {
                    int x1 = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    ADC_A_X(x1);
                }
                break;
                case 0xCF:
                    /*	   RST 8 */
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 8;
                    break;
                case 0xD0:
                    /*	   RET NC */
                    if ((Regs.F & FLAG_C) == 0) {
                        Regs.PC = mem_ReadWord(Regs.SP);
                        Regs.SP = (Regs.SP + 2) & 0xFFFF;
                        ICycles += 12;
                    }
                    break;
                case 0xD1:
                    /*	   POP DE */
                    DE(mem_ReadWord(Regs.SP));
                    Regs.SP = (Regs.SP + 2) & 0xFFFF;
                    break;
                case 0xD2:
                    /*	   JP NC,n16 */

                    if ((Regs.F & FLAG_C) != 0) {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    } else {
                        Regs.PC = mem_ReadWord(Regs.PC);
                        ICycles += 4;
                    }
                    break;
                case 0xD3:
                    /*	   EH? */
                    break;
                case 0xD4:
                    /*	   CALL NC,n16 */

                    if ((Regs.F & FLAG_C) != 0) {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    } else {
                        int PC;
                        PC = mem_ReadWord(Regs.PC) & 0xffff;
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;

                        Regs.SP = (Regs.SP - 2) & 0xFFFF;
                        mem_WriteWord(Regs.SP, Regs.PC);
                        Regs.PC = PC;
                        ICycles += 12;
                    }
                    break;
                case 0xD5:
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;/*	   PUSH DE */
                    mem_WriteWord(Regs.SP, DE());
                    break;
                case 0xD6: {
                    int x1 = mem_ReadByte(Regs.PC);/*	   SUB A,n8 */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    SUB_A_X(x1);
                }
                break;
                case 0xD7:
                    /*	   RST	   $10 */
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 0x10;
                    break;
                case 0xD8:
                    /*	   RET C */
                    if ((Regs.F & FLAG_C) != 0) {
                        Regs.PC = mem_ReadWord(Regs.SP);
                        Regs.SP = (Regs.SP + 2) & 0xFFFF;
                        ICycles += 12;
                    }
                    break;
                case 0xD9:
                    /*	   RETI */
                    Regs.PC = mem_ReadWord(Regs.SP);
                    Regs.SP = (Regs.SP + 2) & 0xFFFF;
                    Regs.enable |= IME;
                    CheckInterrupts = 1;
                    break;
                case 0xDA:
                    /*	   JP C,n16 */

                    if ((Regs.F & FLAG_C) != 0) {
                        Regs.PC = mem_ReadWord(Regs.PC) & 0xFFFF;
                        ICycles += 4;
                    } else {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    }
                    break;
                case 0xDB:
                    /*	   EH? */
                    break;
                case 0xDC:
                    /*	   CALL C,n16 */

                    if ((Regs.F & FLAG_C) != 0) {
                        int PC;
                        PC = mem_ReadWord(Regs.PC) & 0xffff;
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;

                        Regs.SP = (Regs.SP - 2) & 0xFFFF;
                        mem_WriteWord(Regs.SP, Regs.PC);
                        Regs.PC = PC;
                        ICycles += 12;
                    } else {
                        Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    }
                    break;
                case 0xDD:
                    /*	   EH? */
                    break;
                case 0xDE:
                    /*	   SBC A,n8 */

                    x = mem_ReadByte(Regs.PC++);
                    SBC_A_X(x);
                    break;
                case 0xDF:
                    /*	   RST	   $18 */
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 0x18;
                    break;
                case 0xE0:
                    mem_WriteByte(mem_ReadByte(Regs.PC) + 0xFF00, Regs.A);/*	   LD	   ($FF00+n8),A */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0xE1:
                    HL(mem_ReadWord(Regs.SP));/*	   POP HL */
                    Regs.SP = (Regs.SP + 2) & 0xFFFF;
                    break;
                case 0xE2:
                    /*	   LD ($FF00+C),A */

                    mem_WriteByte((0xFF00 + Regs.C) & 0xFFFF, Regs.A);
                    break;
                case 0xE3:
                    /*	   EH? */
                    break;
                case 0xE4:
                    /*	   EH? */
                    break;
                case 0xE5:
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;/*	   PUSH HL */
                    mem_WriteWord(Regs.SP, HL());
                    break;
                case 0xE6: /*	   AND A,n8 */ {
                    int x1 = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    AND_A_X(x1);
                }
                break;
                case 0xE7:
                    /*	   RST $20 */
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 0x20;
                    break;
                case 0xE8: /*	   ADD SP,n8 */ {

                    int n;
                    int f;
                    n = (byte) mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;

                    if ((Regs.SP & 0xFF) + (n & 0xFF) > 0xFF) {
                        f = FLAG_C;
                    } else {
                        f = 0;
                    }

                    if ((Regs.SP & 0x0F) + (n & 0x0F) > 0x0F) {
                        f |= FLAG_H;
                    }

                    Regs.SP = (Regs.SP + n) & 0xFFFF;
                    Regs.F = f & 0xFF;
                }
                break;
                case 0xE9:
                    Regs.PC = HL();/*	   JP (HL) */
                    break;
                case 0xEA:
                    mem_WriteByte(mem_ReadWord(Regs.PC), Regs.A);/*	   LD (n16),A */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0xEB:
                    /*	   EH? */
                    break;
                case 0xEC:
                    /*	   EH? */
                    break;
                case 0xED:
                    /*	   EH? */
                    break;
                case 0xEE: /*	   XOR A,n8 */ {
                    int x1 = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    XOR_A_X(x1);
                }
                break;
                case 0xEF:
                    /*	   RST $28 */
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 0x28;
                    break;
                case 0xF0:
                    Regs.A = mem_ReadByte(0xFF00 + mem_ReadByte(Regs.PC));/*	   LD A,($FF00+n8) */
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    break;
                case 0xF1:
                    AF((mem_ReadWord(Regs.SP) & 0xFFF0));/*	   POP AF */
                    Regs.SP = (Regs.SP + 2) & 0xFFFF;
                    break;
                case 0xF2:
                    /*	   LD A,($FF00+C) */
                    Regs.A = mem_ReadByte((0xFF00 + Regs.C) & 0xFFFF);
                    break;
                case 0xF3:
                    Regs.enable &= ~IME;/*	   DI */
                    break;
                case 0xF4:
                    /*	   EH? */
                    break;
                case 0xF5:
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;/*	   PUSH AF */
                    mem_WriteWord(Regs.SP, (AF() & 0xFFF0) & 0xFFFF);
                    break;
                case 0xF6: /*	   OR A,n8 */ {
                    x = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    OR_A_X(x);
                }
                break;
                case 0xF7:
                    /*	   RST $30 */
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 0x30;
                    break;
                case 0xF8: /*	   LD HL,SP+n8 */ {
                    int n;
                    int f;
                    n = (byte) mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;

                    if ((Regs.SP & 0xFF) + (n & 0xFF) > 0xFF) {
                        f = FLAG_C;
                    } else {
                        f = 0;
                    }

                    if ((Regs.SP & 0x0F) + (n & 0x0F) > 0x0F) {
                        f |= FLAG_H;
                    }

                    int res = (Regs.SP + n) & 0xFFFF;

                    Regs.L = res & 0xFF;
                    Regs.H = (res >> 8) & 0xFF;
                    Regs.F = f & 0xFF;
                }
                break;
                case 0xF9:
                    /*	   LD SP,HL */
                    Regs.SP = HL();
                    break;
                case 0xFA:
                    Regs.A = mem_ReadByte(mem_ReadWord(Regs.PC));/*	   LD A,(n16) */
                    Regs.PC = (Regs.PC + 2) & 0xFFFF;
                    break;
                case 0xFB:
                    /*	   EI */
                    Regs.enable |= IME;
                    CheckInterrupts = 1;
                    break;
                case 0xFC:
                    /*	   EH? */
                    break;
                case 0xFD:
                    /*	   EH? */
                    break;
                case 0xFE: /*	   CP A,n8 */ {
                    int x1 = mem_ReadByte(Regs.PC);
                    Regs.PC = (Regs.PC + 1) & 0xFFFF;
                    CP_A_X(x1);
                }
                break;
                case 0xFF:
                    Regs.SP = (Regs.SP - 2) & 0xFFFF;/*	   RST $38 */
                    mem_WriteWord(Regs.SP, Regs.PC);
                    Regs.PC = 0x38;
                    break;
                default: {
                    System.out.println("Unsupported 0x" + Integer.toHexString(x));
                    throw new UnsupportedOperationException("Unsupported");
                }
            }
            z80gb_ICount[0] -= ICycles;
            gb_divcount += ICycles;
            if ((TIMEFRQ() & 0x04) != 0) {
                gb_timer_count += ICycles;
                if ((gb_timer_count & (0xFF00 << gb_timer_shift)) != 0) {
                    gb_timer_count = TIMEMOD() << gb_timer_shift;
                    IFLAGS(IFLAGS() | TIM_IFLAG);
                    CheckInterrupts = 1;
                }
            }
        } while (z80gb_ICount[0] > 0);

        return cycles - z80gb_ICount[0];
    }
}
