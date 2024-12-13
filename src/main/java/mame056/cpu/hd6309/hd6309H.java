/*** hd6309: Portable 6309 emulator ******************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.cpu.hd6309;

import static mame056.memory.*;
import static mame056.memoryH.*;

public class hd6309H
{
	
	public static final int HD6309_PC           = 1;
        public static final int HD6309_S            = 2;
        public static final int HD6309_CC           = 3;
        public static final int HD6309_A            = 4;
        public static final int HD6309_B            = 5;
        public static final int HD6309_U            = 6;
        public static final int HD6309_X            = 7;
        public static final int HD6309_Y            = 8;
        public static final int HD6309_DP           = 9;
        public static final int HD6309_NMI_STATE    = 10;
	public static final int HD6309_IRQ_STATE    = 11;
        public static final int HD6309_FIRQ_STATE   = 12;
        public static final int HD6309_E            = 13;
        public static final int HD6309_F            = 14;
        public static final int HD6309_V            = 15;
        public static final int HD6309_MD           = 16;
	
	public static final int HD6309_IRQ_LINE     = 0;	/* IRQ line number */
	public static final int HD6309_FIRQ_LINE    = 1;	 /* FIRQ line number */
	
	
	/****************************************************************************/
	/* Read a byte from given memory location									*/
	/****************************************************************************/
	/* ASG 971005 -- changed to cpu_readmem16/cpu_writemem16 */
	public static int HD6309_RDMEM(int Addr){
            return (cpu_readmem16(Addr));
        }
	
	/****************************************************************************/
	/* Write a byte to given memory location									*/
	/****************************************************************************/
	public static void HD6309_WRMEM(int Addr, int Value){
            cpu_writemem16(Addr,Value);
        }
	
	/****************************************************************************/
	/* Z80_RDOP() is identical to Z80_RDMEM() except it is used for reading 	*/
	/* opcodes. In case of system with memory mapped I/O, this function can be	*/
	/* used to greatly speed up emulation										*/
	/****************************************************************************/
	public static int HD6309_RDOP(int Addr){
            return (cpu_readop(Addr));
        }
	
	/****************************************************************************/
	/* Z80_RDOP_ARG() is identical to Z80_RDOP() except it is used for reading	*/
	/* opcode arguments. This difference can be used to support systems that	*/
	/* use different encoding mechanisms for opcodes and opcode arguments		*/
	/****************************************************************************/
	public static int HD6309_RDOP_ARG(int Addr){
            return (cpu_readop_arg(Addr));
        }
	
}
