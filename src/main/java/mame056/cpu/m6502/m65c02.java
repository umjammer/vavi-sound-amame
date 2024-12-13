/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.opsc02H.*;
import static mame056.cpu.m6502.t6502.insn6502;
import static mame056.cpu.m6502.t65c02.*;
import static mame056.cpuintrfH.*;
import static mame056.memoryH.*;

public class m65c02 extends m6502 {
    
    public m65c02(){
        cpu_num = CPU_M65C02;
        num_irqs = 1;
        default_vector = 0;
        icount = m6502_ICount;
        overclock = 1.00;
        irq_int = M6502_IRQ_LINE;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 3;
    }
    
    public String cpu_info(Object context, int regnum) {
        switch (regnum) {
            case CPU_INFO_NAME:
                return "M65C02";
            case CPU_INFO_VERSION:
                return "1.2";
        }
        return super.cpu_info(context, regnum);
    } 
    
/*TODO*////* Layout of the registers in the debugger */
/*TODO*///	static UINT8 m65c02_reg_layout[] = {
/*TODO*///		M65C02_A,M65C02_X,M65C02_Y,M65C02_S,M65C02_PC,M65C02_P, -1,
/*TODO*///		M65C02_EA,M65C02_ZP,M65C02_NMI_STATE,M65C02_IRQ_STATE, 0
/*TODO*///	};
/*TODO*///	
/*TODO*///	/* Layout of the debugger windows x,y,w,h */
/*TODO*///	static UINT8 m65c02_win_layout[] = {
/*TODO*///		25, 0,55, 2,	/* register window (top, right rows) */
/*TODO*///		 0, 0,24,22,	/* disassembler window (left colums) */
/*TODO*///		25, 3,55, 9,	/* memory #1 window (right, upper middle) */
/*TODO*///		25,13,55, 9,	/* memory #2 window (right, lower middle) */
/*TODO*///		 0,23,80, 1,	/* command line window (bottom rows) */
/*TODO*///	};
    
    @Override
    public void init() {
/*TODO*///		m6502.subtype = SUBTYPE_65C02;
/*TODO*///		m6502.insn = insn65c02;
/*TODO*///		m6502_state_register("m65c02");
    }

    @Override
    public void reset(Object param)
    {
            super.reset(param);
            m6502.u8_p &=~F_D;
    }
	
/*TODO*///	void m65c02_exit  (void) { m6502_exit(); }
	
	public static void m65c02_take_irq()
	{
            //System.out.println("m65c02_take_irq!");
		if( (m6502.u8_p & F_I) == 0 )
		{
                    m6502.ea.SetD(M6502_IRQ_VEC);
                    m6502_ICount[0] -= 7;
                    PUSH(m6502.pc.H);
                    PUSH(m6502.pc.L);
                    PUSH(m6502.u8_p & ~F_B);
                    m6502.u8_p = (m6502.u8_p & ~F_D) | F_I;		/* knock out D and set I flag */
                    m6502.pc.SetL(RDMEM(m6502.ea.D));
                    m6502.ea.AddD(1);
                    m6502.pc.SetH(RDMEM(m6502.ea.D));
/*TODO*///			LOG(("M65c02#%d takes IRQ ($%04x)\n", cpu_getactivecpu(), PCD));
                    /* call back the cpuintrf to let it clear the line */
                    if (m6502.irq_callback != null) m6502.irq_callback.handler(0);
                    change_pc16(m6502.pc.D);
		}
		m6502.u8_pending_irq = 0;
	}
	
        @Override
        public int execute(int cycles)
        {
		m6502_ICount[0] = cycles;

                change_pc16(m6502.pc.D);

		do
		{
                    int/*UINT8*/ op;
                    m6502.ppc.SetD(m6502.pc.D);

/*TODO*///			CALL_MAME_DEBUG;
	
			op = RDOP();
                        //System.out.println(op);
			insn65c02[op].handler();
	
			/* if an irq is pending, take it now */
			if( m6502.u8_pending_irq != 0 )
				m65c02_take_irq();
	
	
			/* check if the I flag was just reset (interrupts enabled) */
			if( m6502.u8_after_cli != 0 )
			{
/*TODO*///				LOG(("M6502#%d after_cli was >0", cpu_getactivecpu()));
				m6502.u8_after_cli = 0;
				if (m6502.u8_irq_state != CLEAR_LINE)
				{
/*TODO*///					LOG((": irq line is asserted: set pending IRQ\n"));
					m6502.u8_pending_irq = 1;
				}
				else
				{
/*TODO*///					LOG((": irq line is clear\n"));
				}
			}
			else
			if( m6502.u8_pending_irq != 0 )
				m65c02_take_irq();
	
		} while (m6502_ICount[0] > 0);
	
		return cycles - m6502_ICount[0];
	}

/*TODO*///	unsigned m65c02_get_context (void *dst) { return m6502_get_context(dst); }
/*TODO*///	void m65c02_set_context (void *src) { m6502_set_context(src); }
/*TODO*///	unsigned m65c02_get_reg (int regnum) { return m6502_get_reg(regnum); }
/*TODO*///	void m65c02_set_reg (int regnum, unsigned val) { m6502_set_reg(regnum,val); }
/*TODO*///	
        @Override
        public void set_irq_line(int irqline, int state)
	{
            //System.out.println("set_irq_line "+state);
		if (irqline == IRQ_LINE_NMI)
		{
			if (m6502.u8_nmi_state == state) return;
			m6502.u8_nmi_state = state;
			if( state != CLEAR_LINE )
			{
                            
/*TODO*///				LOG(( "M6502#%d set_nmi_line(ASSERT)\n", cpu_getactivecpu()));
				m6502.ea.SetD(M6502_NMI_VEC);
				m6502_ICount[0] -= 7;
				PUSH(m6502.pc.H);
                                PUSH(m6502.pc.L);
				PUSH(m6502.u8_p & ~F_B);
				m6502.u8_p = (m6502.u8_p & ~F_D) | F_I;		/* knock out D and set I flag */
				m6502.pc.SetL(RDMEM(m6502.ea.D));
                                m6502.ea.AddD(1);
                                m6502.pc.SetH(RDMEM(m6502.ea.D));
/*TODO*///				LOG(("M6502#%d takes NMI ($%04x)\n", cpu_getactivecpu(), PCD));
				change_pc16(m6502.pc.D);
			}
		}
                else{
                    //System.out.println("kk");
			super.set_irq_line(irqline,state);
                }
	}

/*TODO*///	void m65c02_set_irq_callback(int (*callback)(int irqline)) { m6502_set_irq_callback(callback); }
/*TODO*///	const char *m65c02_info(void *context, int regnum)
/*TODO*///	{
/*TODO*///		switch( regnum )
/*TODO*///		{
/*TODO*///			case CPU_INFO_NAME: return "M65C02";
/*TODO*///			case CPU_INFO_VERSION: return "1.2";
/*TODO*///			case CPU_INFO_REG_LAYOUT: return (const char*)m65c02_reg_layout;
/*TODO*///			case CPU_INFO_WIN_LAYOUT: return (const char*)m65c02_win_layout;
/*TODO*///		}
/*TODO*///		return m6502_info(context,regnum);
/*TODO*///	}
/*TODO*///	unsigned m65c02_dasm(char *buffer, unsigned pc)
/*TODO*///	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///		return Dasm6502( buffer, pc );
/*TODO*///	#else
/*TODO*///		sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///		return 1;
/*TODO*///	#endif
/*TODO*///	}
/*TODO*///	
/*TODO*///	#endif
     
}
