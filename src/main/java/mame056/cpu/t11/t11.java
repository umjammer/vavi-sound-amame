/*** t11: Portable DEC T-11 emulator ******************************************

	Copyright (C) Aaron Giles 1998-2001

	System dependencies:	long must be at least 32 bits
	                        word must be 16 bit unsigned int
							byte must be 8 bit unsigned int
							long must be more than 16 bits
							arrays up to 65536 bytes must be supported
							machine must be twos complement

*****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.cpu.t11;

import static mame056.cpu.t11.t11H.*;
import static mame056.cpu.t11.t11ops.*;
import static mame056.cpu.t11.t11table.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;

/**
 *
 * @author chusogar
 */
public class t11  extends cpu_interface {

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset(Object param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void exit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int execute(int cycles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object init_context() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object get_context() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_context(Object reg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_reg(int regnum, int val) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        return t11_info(context, regnum);
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int memory_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void memory_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int internal_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void internal_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_op_base(int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int mem_address_bits_of_cpu() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Debugger layouts
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	static UINT8 t11_reg_layout[] =
/*TODO*///	{
/*TODO*///		T11_PC, T11_SP, T11_PSW, T11_IRQ0_STATE, T11_IRQ1_STATE, T11_IRQ2_STATE, T11_IRQ3_STATE, -1,
/*TODO*///		T11_R0,T11_R1,T11_R2,T11_R3,T11_R4,T11_R5, -1,
/*TODO*///		T11_BANK0,T11_BANK1,T11_BANK2,T11_BANK3, T11_BANK4,T11_BANK5,T11_BANK6,T11_BANK7, 0
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	static UINT8 t11_win_layout[] =
/*TODO*///	{
/*TODO*///		 0, 0,80, 4,	/* register window (top rows) */
/*TODO*///		 0, 5,31,17,	/* disassembler window (left colums) */
/*TODO*///		32, 5,48, 8,	/* memory #1 window (right, upper middle) */
/*TODO*///		32,14,48, 8,	/* memory #2 window (right, lower middle) */
/*TODO*///		 0,23,80, 1,	/* command line window (bottom rows) */
/*TODO*///	};
	
	
	
	/*************************************
	 *
	 *	Internal state representation
	 *
	 *************************************/
    
        public static class PAIR {
            //L = low 8 bits
            //H = high 8 bits
            //D = whole 16 bits

            public int H, L, D;

            public void SetH(int val) {
                H = val & 0xFF;
                D = ((H << 8) | L) & 0xFFFF;
            }

            public void SetL(int val) {
                L = val & 0xFF;
                D = ((H << 8) | L) & 0xFFFF;
            }

            public void SetD(int val) {
                D = val & 0xFFFF;
                H = D >> 8 & 0xFF;
                L = D & 0xFF;
            }

            public void AddH(int val) {
                H = (H + val) & 0xFF;
                D = ((H << 8) | L) & 0xFFFF;
            }

            public void AddL(int val) {
                L = (L + val) & 0xFF;
                D = ((H << 8) | L) & 0xFFFF;
            }

            public void AddD(int val) {
                D = (D + val) & 0xFFFF;
                H = D >> 8 & 0xFF;
                L = D & 0xFF;
            }
        };

	
	public static class t11_Regs
	{
            PAIR	ppc = new PAIR();	/* previous program counter */
	    PAIR[]	reg = new PAIR[8];
	    PAIR	psw = new PAIR();
	    int         op;
	    int         wait_state;
	    int[]	bank = new int[8];
	    int         irq_state;
	    int		interrupt_cycles;
	    
            public irqcallbacksPtr irq_callback;
	} ;

	public static t11_Regs t11 = new t11_Regs();
	
	
	/*************************************
	 *
	 *	Global variables
	 *
	 *************************************/
	
        public static int[] t11_ICount = new int[1];    
	
        //CPU0(M6502,    m6502,	 1,  0,1.00,M6502_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
        //CPU0(T11,	   t11,  4,  0,1.00,-1,	            16,16lew,     0,16,LE,2, 6	),
        
        public t11() {
            cpu_num = CPU_T11;
            num_irqs = 4;
            default_vector = 0;
            icount = t11_ICount;
            overclock = 1.00;
            irq_int = -1;
            databus_width = 16;
            pgm_memory_base = 0;
            address_shift = 0;
            address_bits = 16;
            endianess = CPU_IS_LE;
            align_unit = 3;
            max_inst_len = 6;
        }
	
	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Macro shortcuts
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/* registers of various sizes */
/*TODO*///	#define REGD(x) t11.reg[x].d
/*TODO*///	#define REGW(x) t11.reg[x].w.l
/*TODO*///	#define REGB(x) t11.reg[x].b.l
/*TODO*///	
/*TODO*///	/* PC, SP, and PSW definitions */
/*TODO*///	#define SP REGW(6)
/*TODO*///	#define PC REGW(7)
/*TODO*///	#define SPD REGD(6)
/*TODO*///	#define PCD REGD(7)
/*TODO*///	#define PSW t11.psw.b.l
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Low-level memory operations
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	INLINE int ROPCODE(void)
/*TODO*///	{
/*TODO*///		int pc = PCD;
/*TODO*///		PC += 2;
/*TODO*///		return READ_WORD(&t11.bank[pc >> 13][pc & 0x1fff]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE int RBYTE(int addr)
/*TODO*///	{
/*TODO*///		return T11_RDMEM(addr);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE void WBYTE(int addr, int data)
/*TODO*///	{
/*TODO*///		T11_WRMEM(addr, data);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE int RWORD(int addr)
/*TODO*///	{
/*TODO*///		return T11_RDMEM_WORD(addr & 0xfffe);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE void WWORD(int addr, int data)
/*TODO*///	{
/*TODO*///		T11_WRMEM_WORD(addr & 0xfffe, data);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Low-level stack operations
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	INLINE void PUSH(int val)
/*TODO*///	{
/*TODO*///		SP -= 2;
/*TODO*///		WWORD(SPD, val);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE int POP(void)
/*TODO*///	{
/*TODO*///		int result = RWORD(SPD);
/*TODO*///		SP += 2;
/*TODO*///		return result;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Flag definitions and operations
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/* flag definitions */
/*TODO*///	#define CFLAG 1
/*TODO*///	#define VFLAG 2
/*TODO*///	#define ZFLAG 4
/*TODO*///	#define NFLAG 8
/*TODO*///	
/*TODO*///	/* extracts flags */
/*TODO*///	#define GET_C (PSW & CFLAG)
/*TODO*///	#define GET_V (PSW & VFLAG)
/*TODO*///	#define GET_Z (PSW & ZFLAG)
/*TODO*///	#define GET_N (PSW & NFLAG)
/*TODO*///	
/*TODO*///	/* clears flags */
/*TODO*///	#define CLR_C (PSW &= ~CFLAG)
/*TODO*///	#define CLR_V (PSW &= ~VFLAG)
/*TODO*///	#define CLR_Z (PSW &= ~ZFLAG)
/*TODO*///	#define CLR_N (PSW &= ~NFLAG)
/*TODO*///	
/*TODO*///	/* sets flags */
/*TODO*///	#define SET_C (PSW |= CFLAG)
/*TODO*///	#define SET_V (PSW |= VFLAG)
/*TODO*///	#define SET_Z (PSW |= ZFLAG)
/*TODO*///	#define SET_N (PSW |= NFLAG)
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Interrupt handling
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	struct irq_table_entry
/*TODO*///	{
/*TODO*///		UINT8	priority;
/*TODO*///		UINT8	vector;
/*TODO*///	};
/*TODO*///	
/*TODO*///	static const struct irq_table_entry irq_table[] =
/*TODO*///	{
/*TODO*///		{ 0<<5, 0x00 },
/*TODO*///		{ 4<<5, 0x38 },
/*TODO*///		{ 4<<5, 0x34 },
/*TODO*///		{ 4<<5, 0x30 },
/*TODO*///		{ 5<<5, 0x5c },
/*TODO*///		{ 5<<5, 0x58 },
/*TODO*///		{ 5<<5, 0x54 },
/*TODO*///		{ 5<<5, 0x50 },
/*TODO*///		{ 6<<5, 0x4c },
/*TODO*///		{ 6<<5, 0x48 },
/*TODO*///		{ 6<<5, 0x44 },
/*TODO*///		{ 6<<5, 0x40 },
/*TODO*///		{ 7<<5, 0x6c },
/*TODO*///		{ 7<<5, 0x68 },
/*TODO*///		{ 7<<5, 0x64 },
/*TODO*///		{ 7<<5, 0x60 }
/*TODO*///	};
/*TODO*///	
/*TODO*///	static void t11_check_irqs(void)
/*TODO*///	{
/*TODO*///		const struct irq_table_entry *irq = &irq_table[t11.irq_state & 15];
/*TODO*///		int priority = PSW & 0xe0;
/*TODO*///	
/*TODO*///		/* compare the priority of the interrupt to the PSW */
/*TODO*///		if (irq->priority > priority)
/*TODO*///		{
/*TODO*///			/* get the priority of this interrupt */
/*TODO*///			int new_pc = RWORD(irq->vector);
/*TODO*///			int new_psw = RWORD(irq->vector + 2);
/*TODO*///			
/*TODO*///			/* call the callback */
/*TODO*///			if (t11.irq_callback)
/*TODO*///			{
/*TODO*///				int vector = 0;
/*TODO*///				
/*TODO*///				if (t11.irq_state & 8) vector = 3;
/*TODO*///				else if (t11.irq_state & 4) vector = 2;
/*TODO*///				else if (t11.irq_state & 2) vector = 1;
/*TODO*///				(*t11.irq_callback)(vector);
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* kludge for 720 - fix me! */
/*TODO*///			if (new_pc == 0)
/*TODO*///				return;
/*TODO*///	
/*TODO*///			/* push the old state, set the new one */
/*TODO*///			PUSH(PSW);
/*TODO*///			PUSH(PC);
/*TODO*///			PCD = new_pc;
/*TODO*///			PSW = new_psw;
/*TODO*///	
/*TODO*///			/* count cycles and clear the WAIT flag */
/*TODO*///			t11.interrupt_cycles += 114;
/*TODO*///			t11.wait_state = 0;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Core opcodes
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/* includes the static function prototypes and the master opcode table */
/*TODO*///	
/*TODO*///	/* includes the actual opcode implementations */
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Fetch current context into buffer
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	unsigned t11_get_context(void *dst)
/*TODO*///	{
/*TODO*///		if (dst)
/*TODO*///			*(t11_Regs *)dst = t11;
/*TODO*///		return sizeof(t11_Regs);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Retrieve context from buffer
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void t11_set_context(void *src)
/*TODO*///	{
/*TODO*///		if (src)
/*TODO*///			t11 = *(t11_Regs *)src;
/*TODO*///		t11_check_irqs();
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	External register getting
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	unsigned t11_get_reg(int regnum)
/*TODO*///	{
/*TODO*///		switch (regnum)
/*TODO*///		{
/*TODO*///			case REG_PC:
/*TODO*///			case T11_PC:			return PCD;
/*TODO*///			case REG_SP:
/*TODO*///			case T11_SP:			return SPD;
/*TODO*///			case T11_PSW:			return PSW;
/*TODO*///			case T11_R0:			return REGD(0);
/*TODO*///			case T11_R1:			return REGD(1);
/*TODO*///			case T11_R2:			return REGD(2);
/*TODO*///			case T11_R3:			return REGD(3);
/*TODO*///			case T11_R4:			return REGD(4);
/*TODO*///			case T11_R5:			return REGD(5);
/*TODO*///			case T11_IRQ0_STATE:	return (t11.irq_state & 1) ? ASSERT_LINE : CLEAR_LINE;
/*TODO*///			case T11_IRQ1_STATE:	return (t11.irq_state & 2) ? ASSERT_LINE : CLEAR_LINE;
/*TODO*///			case T11_IRQ2_STATE:	return (t11.irq_state & 4) ? ASSERT_LINE : CLEAR_LINE;
/*TODO*///			case T11_IRQ3_STATE:	return (t11.irq_state & 8) ? ASSERT_LINE : CLEAR_LINE;
/*TODO*///			case T11_BANK0:			return (unsigned)(t11.bank[0] - OP_RAM);
/*TODO*///			case T11_BANK1:			return (unsigned)(t11.bank[1] - OP_RAM);
/*TODO*///			case T11_BANK2:			return (unsigned)(t11.bank[2] - OP_RAM);
/*TODO*///			case T11_BANK3:			return (unsigned)(t11.bank[3] - OP_RAM);
/*TODO*///			case T11_BANK4:			return (unsigned)(t11.bank[4] - OP_RAM);
/*TODO*///			case T11_BANK5:			return (unsigned)(t11.bank[5] - OP_RAM);
/*TODO*///			case T11_BANK6:			return (unsigned)(t11.bank[6] - OP_RAM);
/*TODO*///			case T11_BANK7:			return (unsigned)(t11.bank[7] - OP_RAM);
/*TODO*///			case REG_PREVIOUSPC:	return t11.ppc.w.l;
/*TODO*///			default:
/*TODO*///				if (regnum <= REG_SP_CONTENTS)
/*TODO*///				{
/*TODO*///					unsigned offset = SPD + 2 * (REG_SP_CONTENTS - regnum);
/*TODO*///					if (offset < 0xffff)
/*TODO*///						return RWORD(offset);
/*TODO*///				}
/*TODO*///		}
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	External register setting
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void t11_set_reg(int regnum, unsigned val)
/*TODO*///	{
/*TODO*///		switch (regnum)
/*TODO*///		{
/*TODO*///			case REG_PC:
/*TODO*///			case T11_PC:			PC = val; /* change_pc16 not needed */ break;
/*TODO*///			case REG_SP:
/*TODO*///			case T11_SP:			SP = val; break;
/*TODO*///			case T11_PSW:			PSW = val; break;
/*TODO*///			case T11_R0:			REGW(0) = val; break;
/*TODO*///			case T11_R1:			REGW(1) = val; break;
/*TODO*///			case T11_R2:			REGW(2) = val; break;
/*TODO*///			case T11_R3:			REGW(3) = val; break;
/*TODO*///			case T11_R4:			REGW(4) = val; break;
/*TODO*///			case T11_R5:			REGW(5) = val; break;
/*TODO*///			case T11_IRQ0_STATE:	t11_set_irq_line(T11_IRQ0, val); break;
/*TODO*///			case T11_IRQ1_STATE:	t11_set_irq_line(T11_IRQ1, val); break;
/*TODO*///			case T11_IRQ2_STATE:	t11_set_irq_line(T11_IRQ2, val); break;
/*TODO*///			case T11_IRQ3_STATE:	t11_set_irq_line(T11_IRQ3, val); break;
/*TODO*///			case T11_BANK0:			t11.bank[0] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK1:			t11.bank[1] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK2:			t11.bank[2] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK3:			t11.bank[3] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK4:			t11.bank[4] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK5:			t11.bank[5] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK6:			t11.bank[6] = &OP_RAM[val]; break;
/*TODO*///			case T11_BANK7:			t11.bank[7] = &OP_RAM[val]; break;
/*TODO*///			default:
/*TODO*///				if (regnum < REG_SP_CONTENTS)
/*TODO*///				{
/*TODO*///					unsigned offset = SPD + 2 * (REG_SP_CONTENTS - regnum);
/*TODO*///					if (offset < 0xffff)
/*TODO*///						WWORD(offset, val & 0xffff);
/*TODO*///				}
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Low-level initialization/cleanup
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void t11_init(void)
/*TODO*///	{
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	void t11_exit(void)
/*TODO*///	{
/*TODO*///		/* nothing to do */
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	CPU reset
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void t11_reset(void *param)
/*TODO*///	{
/*TODO*///		static const UINT16 initial_pc[] =
/*TODO*///		{
/*TODO*///			0xc000, 0x8000, 0x4000, 0x2000,
/*TODO*///			0x1000, 0x0000, 0xf600, 0xf400
/*TODO*///		};
/*TODO*///		struct t11_setup *setup = param;
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* reset the state */
/*TODO*///		memset(&t11, 0, sizeof(t11));
/*TODO*///		
/*TODO*///		/* initial SP is 376 octal, or 0xfe */
/*TODO*///		SP = 0x00fe;
/*TODO*///		
/*TODO*///		/* initial PC comes from the setup word */
/*TODO*///		PC = initial_pc[setup->mode >> 13];
/*TODO*///	
/*TODO*///		/* PSW starts off at highest priority */
/*TODO*///		PSW = 0xe0;
/*TODO*///	
/*TODO*///		/* initialize the banking */
/*TODO*///		for (i = 0; i < 8; i++)
/*TODO*///			t11.bank[i] = &OP_RAM[i * 0x2000];
/*TODO*///		
/*TODO*///		/* initialize the IRQ state */
/*TODO*///		t11.irq_state = 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Interrupt handling
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void t11_set_irq_line(int irqline, int state)
/*TODO*///	{
/*TODO*///		/* set the appropriate bit */
/*TODO*///		if (state == CLEAR_LINE)
/*TODO*///			t11.irq_state &= ~(1 << irqline);
/*TODO*///		else
/*TODO*///			t11.irq_state |= 1 << irqline;
/*TODO*///	
/*TODO*///		/* recheck for interrupts */
/*TODO*///	   	t11_check_irqs();
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	void t11_set_irq_callback(int (*callback)(int irqline))
/*TODO*///	{
/*TODO*///		t11.irq_callback = callback;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Core execution
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	int t11_execute(int cycles)
/*TODO*///	{
/*TODO*///		t11_ICount = cycles;
/*TODO*///		t11_ICount -= t11.interrupt_cycles;
/*TODO*///		t11.interrupt_cycles = 0;
/*TODO*///	
/*TODO*///		if (t11.wait_state)
/*TODO*///		{
/*TODO*///			t11_ICount = 0;
/*TODO*///			goto getout;
/*TODO*///		}
/*TODO*///	
/*TODO*///		do
/*TODO*///		{
/*TODO*///			t11.ppc = t11.reg[7];	/* copy PC to previous PC */
/*TODO*///	
/*TODO*///			CALL_MAME_DEBUG;
/*TODO*///	
/*TODO*///			t11.op = ROPCODE();
/*TODO*///			(*opcode_table[t11.op >> 3])();
/*TODO*///	
/*TODO*///		} while (t11_ICount > 0);
/*TODO*///	
/*TODO*///	getout:
/*TODO*///	
/*TODO*///		t11_ICount -= t11.interrupt_cycles;
/*TODO*///		t11.interrupt_cycles = 0;
/*TODO*///	
/*TODO*///		return cycles - t11_ICount;
/*TODO*///	}
	
	
	
	/*************************************
	 *
	 *	Return formatted string
	 *
	 *************************************/
	
	public String t11_info( Object context, int regnum )
	{
/*TODO*///		static char buffer[16][47+1];
/*TODO*///		static int which = 0;
/*TODO*///		t11_Regs *r = context;
/*TODO*///	
/*TODO*///		which = (which+1) % 16;
/*TODO*///	    buffer[which][0] = '\0';
/*TODO*///	
/*TODO*///		if (context == 0)
/*TODO*///			r = &t11;
/*TODO*///	
	    switch( regnum )
            {
/*TODO*///			case CPU_INFO_REG+T11_PC: sprintf(buffer[which], "PC:%04X", r->reg[7].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_SP: sprintf(buffer[which], "SP:%04X", r->reg[6].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_PSW: sprintf(buffer[which], "PSW:%02X", r->psw.b.l); break;
/*TODO*///			case CPU_INFO_REG+T11_R0: sprintf(buffer[which], "R0:%04X", r->reg[0].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_R1: sprintf(buffer[which], "R1:%04X", r->reg[1].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_R2: sprintf(buffer[which], "R2:%04X", r->reg[2].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_R3: sprintf(buffer[which], "R3:%04X", r->reg[3].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_R4: sprintf(buffer[which], "R4:%04X", r->reg[4].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_R5: sprintf(buffer[which], "R5:%04X", r->reg[5].w.l); break;
/*TODO*///			case CPU_INFO_REG+T11_IRQ0_STATE: sprintf(buffer[which], "IRQ0:%X", (r->irq_state & 1) ? ASSERT_LINE : CLEAR_LINE); break;
/*TODO*///			case CPU_INFO_REG+T11_IRQ1_STATE: sprintf(buffer[which], "IRQ1:%X", (r->irq_state & 2) ? ASSERT_LINE : CLEAR_LINE); break;
/*TODO*///			case CPU_INFO_REG+T11_IRQ2_STATE: sprintf(buffer[which], "IRQ2:%X", (r->irq_state & 4) ? ASSERT_LINE : CLEAR_LINE); break;
/*TODO*///			case CPU_INFO_REG+T11_IRQ3_STATE: sprintf(buffer[which], "IRQ3:%X", (r->irq_state & 8) ? ASSERT_LINE : CLEAR_LINE); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK0: sprintf(buffer[which], "B0:%06X", (unsigned)(r->bank[0] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK1: sprintf(buffer[which], "B1:%06X", (unsigned)(r->bank[1] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK2: sprintf(buffer[which], "B2:%06X", (unsigned)(r->bank[2] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK3: sprintf(buffer[which], "B3:%06X", (unsigned)(r->bank[3] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK4: sprintf(buffer[which], "B4:%06X", (unsigned)(r->bank[4] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK5: sprintf(buffer[which], "B5:%06X", (unsigned)(r->bank[5] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK6: sprintf(buffer[which], "B6:%06X", (unsigned)(r->bank[6] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_REG+T11_BANK7: sprintf(buffer[which], "B7:%06X", (unsigned)(r->bank[7] - OP_RAM)); break;
/*TODO*///			case CPU_INFO_FLAGS:
/*TODO*///				sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
/*TODO*///					r->psw.b.l & 0x80 ? '?':'.',
/*TODO*///					r->psw.b.l & 0x40 ? 'I':'.',
/*TODO*///					r->psw.b.l & 0x20 ? 'I':'.',
/*TODO*///					r->psw.b.l & 0x10 ? 'T':'.',
/*TODO*///					r->psw.b.l & 0x08 ? 'N':'.',
/*TODO*///					r->psw.b.l & 0x04 ? 'Z':'.',
/*TODO*///					r->psw.b.l & 0x02 ? 'V':'.',
/*TODO*///					r->psw.b.l & 0x01 ? 'C':'.');
/*TODO*///				break;
			case CPU_INFO_NAME: return "T11";
			case CPU_INFO_FAMILY: return "DEC T-11";
			case CPU_INFO_VERSION: return "1.0";
			case CPU_INFO_FILE: return "t11.java";
			case CPU_INFO_CREDITS: return "Copyright (C) Aaron Giles 1998";
/*TODO*///			case CPU_INFO_REG_LAYOUT: return (const char*)t11_reg_layout;
/*TODO*///			case CPU_INFO_WIN_LAYOUT: return (const char*)t11_win_layout;
	    }
/*TODO*///		return buffer[which];
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	
	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Disassembly hook
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	unsigned t11_dasm(char *buffer, unsigned pc)
/*TODO*///	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	    return DasmT11(buffer,pc);
/*TODO*///	#else
/*TODO*///		sprintf( buffer, "$%04X", cpu_readmem16lew_word(pc) );
/*TODO*///		return 2;
/*TODO*///	#endif
/*TODO*///	}
    
}
