/***************************************************************************

 SAM Coupe Driver - Written By Lee Hammerton

  Functions to emulate general aspects of the machine (RAM, ROM, interrupts,
  I/O ports)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.cpu_cause_interrupt;
import static mame056.cpuexecH.Z80_NMI_INT;
import static mame056.memory.*;
import static mame056.memoryH.*;

import static mess056.includes.coupeH.*;
import static mess056.includes.basicdskH.*;
import static mess056.machine.wd179x.*;
import static mess056.includes.wd179xH.*;
import static mess056.vidhrdw.coupe.*;
import static mess056.messH.*;
import static mess056.machine.basicdsk.*;

/**
 *
 * @author chusogar
 */
public class coupe {
	
	public static int LMPR,HMPR,VMPR;	/* Bank Select Registers (Low Page p250, Hi Page p251, Video Page p252) */
	public static int[] CLUT = new int[16]; 		/* 16 entries in a palette (no line affects supported yet!) */
	public static int SOUND_ADDR;		/* Current Address in sound registers */
	public static int[] SOUND_REG = new int[32];	/* 32 sound registers */
	public static int LINE_INT; 		/* Line interrupt */
	public static int LPEN,HPEN;		/* ??? */
	public static int CURLINE;			/* Current scanline */
	public static int STAT; 			/* returned when port 249 read */
	public static int RAM_SIZE;		/* RAM size (256K or 512K) */
	public static int PAGE_MASK;		/* 256K = 0x0f, 512K = 0x1f */
	
	public static io_initPtr coupe_floppy_init = new io_initPtr() {
            public int handler(int id) {
                if (basicdsk_floppy_init(id)==INIT_PASS)
		{
	
			basicdsk_set_geometry(id, 80, 2, 10, 512, 1, 0);
	
			return INIT_PASS;
		}
	
		return INIT_FAIL;
            }
        };
	
	public static void coupe_update_memory()
	{
            UBytePtr mem = new UBytePtr(memory_region(REGION_CPU1));
	
	    if ((LMPR & LMPR_RAM0) != 0)   /* Is ram paged in at bank 1 */
		{
			if ((LMPR & 0x1F) <= PAGE_MASK)
			{
				cpu_setbank(1,new UBytePtr(mem, (LMPR & PAGE_MASK) * 0x4000));
				memory_set_bankhandler_r(1, 0, MRA_BANK1);
				memory_set_bankhandler_w(1, 0, MWA_BANK1);
			}
			else
			{
				memory_set_bankhandler_r(1, 0, MRA_NOP);	/* Attempt to page in non existant ram region */
				memory_set_bankhandler_w(1, 0, MWA_NOP);
			}
		}
		else
		{
			cpu_setbank(1,new UBytePtr(memory_region(REGION_CPU1), RAM_SIZE));	/* Rom0 paged in */
			cpu_setbank(1,new UBytePtr(memory_region(REGION_CPU1), RAM_SIZE));
			memory_set_bankhandler_r(1, 0, MRA_BANK1);
			memory_set_bankhandler_w(1, 0, MWA_ROM);
		}
	
		if (( (LMPR+1) & 0x1F) <= PAGE_MASK)
		{
			cpu_setbank(2,new UBytePtr(mem, ((LMPR+1) & PAGE_MASK) * 0x4000));
			memory_set_bankhandler_r(2, 0, MRA_BANK2);
			memory_set_bankhandler_w(2, 0, MWA_BANK2);
		}
		else
		{
			memory_set_bankhandler_r(2, 0, MRA_NOP);	/* Attempt to page in non existant ram region */
			memory_set_bankhandler_w(2, 0, MWA_NOP);
		}
	
		if ( (HMPR & 0x1F) <= PAGE_MASK )
		{
			cpu_setbank(3,new UBytePtr(mem, (HMPR & PAGE_MASK)*0x4000));
			memory_set_bankhandler_r(3, 0, MRA_BANK3);
			memory_set_bankhandler_w(3, 0, MWA_BANK3);
		}
		else
		{
			memory_set_bankhandler_r(3, 0, MRA_NOP);	/* Attempt to page in non existant ram region */
			memory_set_bankhandler_w(3, 0, MWA_NOP);
		}
	
		if ((LMPR & LMPR_ROM1) != 0)	/* Is Rom1 paged in at bank 4 */
		{
			cpu_setbank(4,new UBytePtr(mem, RAM_SIZE + 0x4000));
			memory_set_bankhandler_r(4, 0, MRA_BANK4);
			memory_set_bankhandler_w(4, 0, MWA_ROM);
		}
		else
		{
			if (( (HMPR+1) & 0x1F) <= PAGE_MASK)
			{
				cpu_setbank(4,new UBytePtr(mem, ((HMPR+1) & PAGE_MASK) * 0x4000));
				memory_set_bankhandler_r(4, 0, MRA_BANK4);
				memory_set_bankhandler_w(4, 0, MWA_BANK4);
			}
			else
			{
				memory_set_bankhandler_r(4, 0, MRA_NOP);	/* Attempt to page in non existant ram region */
				memory_set_bankhandler_w(4, 0, MWA_NOP);
			}
		}
	
		if ((VMPR & 0x40) != 0)	/* if bit set in 2 bank screen mode */
			sam_screen = new UBytePtr(mem, ((VMPR&0x1E) & PAGE_MASK) * 0x4000);
		else
			sam_screen = new UBytePtr(mem, ((VMPR&0x1F) & PAGE_MASK) * 0x4000);
	}
	
	public static void coupe_init_machine_common()
	{
            memory_set_bankhandler_r(1, 0, MRA_BANK1);
            memory_set_bankhandler_w(1, 0, MWA_BANK1);
            memory_set_bankhandler_r(2, 0, MRA_BANK2);
            memory_set_bankhandler_w(2, 0, MWA_BANK2);
            memory_set_bankhandler_r(3, 0, MRA_BANK3);
            memory_set_bankhandler_w(3, 0, MWA_BANK3);
            memory_set_bankhandler_r(4, 0, MRA_BANK4);
            memory_set_bankhandler_w(4, 0, MWA_BANK4);

	
	    LMPR = 0x0F;            /* ROM0 paged in, ROM1 paged out RAM Banks */
	    HMPR = 0x01;
	    VMPR = 0x81;
	
	    LINE_INT = 0xFF;
	    LPEN = 0x00;
	    HPEN = 0x00;
	
	    STAT = 0x1F;
	
	    CURLINE = 0x00;
	
	    coupe_update_memory();
	
	    wd179x_init(WD_TYPE_177X,null);
	}
	
	public static InitMachinePtr coupe_init_machine_256 = new InitMachinePtr() {
            public void handler() {
                PAGE_MASK = 0x0f;
		RAM_SIZE = 0x40000;
		coupe_init_machine_common();
            }
        };
	
	public static InitMachinePtr coupe_init_machine_512 = new InitMachinePtr() {
            public void handler() {
		PAGE_MASK = 0x1f;
		RAM_SIZE = 0x80000;
		coupe_init_machine_common();
            }
        };
	
	public static StopMachinePtr coupe_shutdown_machine = new StopMachinePtr() {
            public void handler() {
                wd179x_exit();
            }
        };
	
	/*************************************
	 *
	 *      Interrupt handlers.
	 *
	 *************************************/
	
	void coupe_nmi_generate(int param)
	{
		cpu_cause_interrupt(0, Z80_NMI_INT);
	}
	
    
}
