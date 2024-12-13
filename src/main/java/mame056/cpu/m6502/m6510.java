/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.t6510.*;
import static mame056.cpuintrfH.*;
import static mame056.memoryH.*;

public class m6510 extends m6502 {
    
    public m6510(){
        cpu_num = CPU_M6510;
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
                return "M6510";
            case CPU_INFO_VERSION:
                return "1.2";
        }
        return super.cpu_info(context, regnum);
    }
    
    @Override
    public void init() {
        //m6502.subtype = SUBTYPE_6510;
	//m6502.insn = insn6510;
	//m6502_state_register("m6510");
    }
    
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
            insn6510[op].handler();

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
    
    /*@Override
    public void reset(Object param) {
        m6502_reset(param);
    }
	void m6510_exit  (void) { m6502_exit(); }
	int  m6510_execute(int cycles) { return m6502_execute(cycles); }
	unsigned m6510_get_context (void *dst) { return m6502_get_context(dst); }
	void m6510_set_context (void *src) { m6502_set_context(src); }
	unsigned m6510_get_reg (int regnum) { return m6502_get_reg(regnum); }
	void m6510_set_reg (int regnum, unsigned val) { m6502_set_reg(regnum,val); }
	void m6510_set_irq_line(int irqline, int state) { m6502_set_irq_line(irqline,state); }
	void m6510_set_irq_callback(int (*callback)(int irqline)) { m6502_set_irq_callback(callback); }
*/
}
