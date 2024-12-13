/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpu.m6502.ops02H.RDOP;
//import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.t65sc02.*;
import static mame056.cpuintrfH.*;
import static mame056.memoryH.*;

public class m65sc02 extends m6502 {

        public m65sc02() {
            cpu_num = CPU_M65SC02;
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
        
        /****************************************************************************
	 * 65SC02 section
	 ****************************************************************************/
	/*TODO*///#if (HAS_M65SC02)
	/* Layout of the registers in the debugger */
	/*TODO*///static UINT8 m65sc02_reg_layout[] = {
	/*TODO*///	M65SC02_A,M65SC02_X,M65SC02_Y,M65SC02_S,M65SC02_PC,M65SC02_P, -1,
	/*TODO*///	M65SC02_EA,M65SC02_ZP,M65SC02_NMI_STATE,M65SC02_IRQ_STATE, 0
	/*TODO*///};
	
	/* Layout of the debugger windows x,y,w,h */
	/*TODO*///static UINT8 m65sc02_win_layout[] = {
	/*TODO*///	25, 0,55, 2,	/* register window (top, right rows) */
	/*TODO*///	 0, 0,24,22,	/* disassembler window (left colums) */
	/*TODO*///	25, 3,55, 9,	/* memory #1 window (right, upper middle) */
	/*TODO*///	25,13,55, 9,	/* memory #2 window (right, lower middle) */
	/*TODO*///	 0,23,80, 1,	/* command line window (bottom rows) */
	/*TODO*///};
	
	
	@Override
        public void init()
	{
		//m6502.subtype = SUBTYPE_65SC02;
		//m6502.insn = insn65sc02;
		//m6502_state_register("m65sc02");
	}
        
	/*TODO*///void m65sc02_reset(void *param) { m6502_reset(param); }
	/*TODO*///void m65sc02_exit  (void) { m6502_exit(); }
	/*TODO*///int  m65sc02_execute(int cycles) { return m65c02_execute(cycles); }
        @Override
        public int execute(int cycles) {
            m6502_ICount[0] = cycles;

            change_pc16(m6502.pc.D);

            do {
                int/*UINT8*/ op;
                m6502.ppc.SetD(m6502.pc.D);

                /* if an irq is pending, take it now */
                if (m6502.u8_pending_irq != 0) {
                    m6502_take_irq();
                }

                op = RDOP();
                insn65sc02[op].handler();

                /* check if the I flag was just reset (interrupts enabled) */
                if (m6502.u8_after_cli != 0) {
                    //LOG(("M6502#%d after_cli was >0", cpu_getactivecpu()));
                    m6502.u8_after_cli = 0;
                    if (m6502.u8_irq_state != CLEAR_LINE) {
                        //LOG((": irq line is asserted: set pending IRQ\n"));
                        m6502.u8_pending_irq = 1;
                    } else {
                        //LOG((": irq line is clear\n"));
                    }
                } else if (m6502.u8_pending_irq != 0) {
                    m6502_take_irq();
                }

            } while (m6502_ICount[0] > 0);

            return cycles - m6502_ICount[0];
        }
	/*TODO*///unsigned m65sc02_get_context (void *dst) { return m6502_get_context(dst); }
	/*TODO*///void m65sc02_set_context (void *src) { m6502_set_context(src); }
	/*TODO*///unsigned m65sc02_get_reg (int regnum) { return m6502_get_reg(regnum); }
	/*TODO*///void m65sc02_set_reg (int regnum, unsigned val) { m6502_set_reg(regnum,val); }
	/*TODO*///void m65sc02_set_irq_line(int irqline, int state) { m6502_set_irq_line(irqline,state); }
	/*TODO*///void m65sc02_set_irq_callback(int (*callback)(int irqline)) { m6502_set_irq_callback(callback); }
        
        public String cpu_info(Object context, int regnum) {
            switch (regnum) {
                case CPU_INFO_NAME:
                    return "M65SC02";
                //case CPU_INFO_FAMILY: return "Metal Oxid Semiconductor MOS 6502";
                case CPU_INFO_VERSION:
                    return "1.0beta";
                //case CPU_INFO_CREDITS:
		//		return "Copyright (c) 1998 Juergen Buchmueller\n"
		//			"Copyright (c) 2000 Peter Trauner\n"
		//			"all rights reserved.";
		//	case CPU_INFO_REG_LAYOUT: return (const char*)m65sc02_reg_layout;
		//	case CPU_INFO_WIN_LAYOUT: return (const char*)m65sc02_win_layout;
            }
            return super.cpu_info(context, regnum);
        }
         
}
