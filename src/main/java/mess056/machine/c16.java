/***************************************************************************

	commodore c16 home computer

	peter.trauner@jk.uni-linz.ac.at
    documentation
 	 www.funet.fi

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static consoleflex056.funcPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpu.m6502.m6502H.M6510_IRQ_LINE;
import static mame056.cpuexec.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.osdependH.*;
import static mess056.deviceH.*;
import static mess056.includes.c16H.*;
import static mess056.includes.cbmserbH.*;
import static mess056.includes.sid6581H.*;
import static mess056.includes.ted7360H.*;
import static mess056.mess.*;
import static mess056.vidhrdw.ted7360.*;
import static mess056.machine.cbmserb.*;
import static mess056.machine.tpi6525.*;
import static mess056.messH.*;

public class c16
{
	
	public static int VERBOSE_DBG = 1;
	
	
	static int keyline[] =
	{
		0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
	};
	
	static int[] rom_specified=new int[2];
	
	/*
	 * tia6523
	 *
	 * connector to floppy c1551 (delivered with c1551 as c16 expansion)
	 * port a for data read/write
	 * port b
	 * 0 status 0
	 * 1 status 1
	 * port c
	 * 6 dav output edge data on port a available
	 * 7 ack input edge ready for next datum
	 */
	
	static int port6529, port7501, ddr7501;
	
	static int lowrom = 0, highrom = 0;
	
	public static UBytePtr c16_memory = new UBytePtr(0xfd00);
	public static UBytePtr c16_memory_10000;
	public static UBytePtr c16_memory_14000;
	public static UBytePtr c16_memory_18000;
	public static UBytePtr c16_memory_1c000;
	public static UBytePtr c16_memory_20000;
	public static UBytePtr c16_memory_24000;
	public static UBytePtr c16_memory_28000;
	public static UBytePtr c16_memory_2c000;
	
	/**
	  ddr bit 1 port line is output
	  port bit 1 port line is high
	
	  serial bus
	  1 serial srq in (ignored)
	  2 gnd
	  3 atn out (pull up)
	  4 clock in/out (pull up)
	  5 data in/out (pull up)
	  6 /reset (pull up) hardware
	
	
	  p0 negated serial bus pin 5 /data out
	  p1 negated serial bus pin 4 /clock out, cassette write
	  p2 negated serial bus pin 3 /atn out
	  p3 cassette motor out
	
	  p4 cassette read
	  p5 not connected (or not available on MOS7501?)
	  p6 serial clock in
	  p7 serial data in, serial bus 5
	 */
	public static WriteHandlerPtr c16_m7501_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int dat, atn, clk;
	
		if (offset != 0)
		{
			if (port7501 != data)
				port7501 = data;
		}
		else
		{
			if (ddr7501 != data)
				ddr7501 = data;
		}
		data = (port7501 & ddr7501) | (ddr7501 ^ 0xff);
		/* bit zero then output 0 */
		cbm_serial_atn_write (atn = (data & 4)!=0?0:1);
		cbm_serial_clock_write (clk = (data & 2)!=0?0:1);
		cbm_serial_data_write (dat = (data & 1)!=0?0:1);
		/*TODO*///vc20_tape_write ((data & 2)!=0?0:1);
		/*TODO*///vc20_tape_motor (data & 8);
            }
        };
	
	public static ReadHandlerPtr c16_m7501_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                if (offset != 0)
		{
			int data = (ddr7501 & port7501) | (ddr7501 ^ 0xff);
	
			if ((ddr7501 & 0x80)==0
				&& (((ddr7501 & 1)!=0 && (port7501 & 1)!=0) || cbm_serial_data_read ()==0))
				data &= ~0x80;
			if ((ddr7501 & 0x40)==0
				&& (((ddr7501 & 2)!=0 && (port7501 & 2)!=0) || cbm_serial_clock_read ()==0))
				data &= ~0x40;
			/*TODO*///if ((ddr7501 & 0x10)==0 && vc20_tape_read ()==0)
			/*TODO*///	data &= ~0x10;
	/*      data&=~0x20; //port bit not in pinout */
			return data;
		}
		else
		{
			return ddr7501;
		}
            }
        };
	
	static void c16_bankswitch ()
	{
		switch (lowrom)
		{
		case 0:
			cpu_setbank (2, new UBytePtr(c16_memory, 0x10000));
			break;
		case 1:
			cpu_setbank (2, new UBytePtr(c16_memory, 0x18000));
			break;
		case 2:
			cpu_setbank (2, new UBytePtr(c16_memory, 0x20000));
			break;
		case 3:
			cpu_setbank (2, new UBytePtr(c16_memory, 0x28000));
			break;
		}
		switch (highrom)
		{
		case 0:
			cpu_setbank (3, new UBytePtr(c16_memory, 0x14000));
			cpu_setbank (8, new UBytePtr(c16_memory, 0x17f20));
			break;
		case 1:
			cpu_setbank (3, new UBytePtr(c16_memory, 0x1c000));
			cpu_setbank (8, new UBytePtr(c16_memory, 0x1ff20));
			break;
		case 2:
			cpu_setbank (3, new UBytePtr(c16_memory, 0x24000));
			cpu_setbank (8, new UBytePtr(c16_memory, 0x27f20));
			break;
		case 3:
			cpu_setbank (3, new UBytePtr(c16_memory, 0x2c000));
			cpu_setbank (8, new UBytePtr(c16_memory, 0x2ff20));
			break;
		}
		cpu_setbank (4, new UBytePtr(c16_memory, 0x17c00));
	}
	
	public static WriteHandlerPtr c16_switch_to_rom = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                ted7360_rom = true;
		c16_bankswitch ();
            }
        };
	
	/* write access to fddX load data flipflop
	 * and selects roms
	 * a0 a1
	 * 0  0  basic
	 * 0  1  plus4 low
	 * 1  0  c1 low
	 * 1  1  c2 low
	 *
	 * a2 a3
	 * 0  0  kernal
	 * 0  1  plus4 hi
	 * 1  0  c1 high
	 * 1  1  c2 high */
	public static WriteHandlerPtr c16_select_roms = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		lowrom = offset & 3;
		highrom = (offset & 0xc) >> 2;
		if (ted7360_rom != false)
			c16_bankswitch ();
            }
        };
	
	public static WriteHandlerPtr c16_switch_to_ram = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		ted7360_rom = false;
		switch (DIPMEMORY())
		{
		case MEMORY64K:
			cpu_setbank (2, new UBytePtr(c16_memory, 0x8000));
			cpu_setbank (3, new UBytePtr(c16_memory, 0xc000));
			cpu_setbank (4, new UBytePtr(c16_memory, 0xfc00));
			cpu_setbank (8, new UBytePtr(c16_memory, 0xff20));
			break;
		case MEMORY32K:
			cpu_setbank (2, new UBytePtr(c16_memory));
			cpu_setbank (3, new UBytePtr(c16_memory, 0x4000));
			cpu_setbank (4, new UBytePtr(c16_memory, 0x7c00));
			cpu_setbank (8, new UBytePtr(c16_memory, 0x7f20));
			break;
		case MEMORY16K:
			cpu_setbank (2, new UBytePtr(c16_memory));
			cpu_setbank (3, new UBytePtr(c16_memory));
			cpu_setbank (4, new UBytePtr(c16_memory, 0x3c00));
			cpu_setbank (8, new UBytePtr(c16_memory, 0x3f20));
			break;
		}
            }
        };
	
	public static WriteHandlerPtr plus4_switch_to_ram = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		ted7360_rom = false;
		cpu_setbank (2, new UBytePtr(c16_memory, 0x8000));
		cpu_setbank (3, new UBytePtr(c16_memory, 0xc000));
		cpu_setbank (4, new UBytePtr(c16_memory, 0xfc00));
		cpu_setbank (8, new UBytePtr(c16_memory, 0xff20));
            }
        };
	
	public static int c16_read_keyboard (int databus)
	{
		int value = 0xff;
	
		if ((port6529 & 1)==0)
			value &= keyline[0];
		if ((port6529 & 2)==0)
			value &= keyline[1];
		if ((port6529 & 4)==0)
			value &= keyline[2];
		if ((port6529 & 8)==0)
			value &= keyline[3];
		if ((port6529 & 0x10)==0)
			value &= keyline[4];
		if ((port6529 & 0x20)==0)
			value &= keyline[5];
		if ((port6529 & 0x40)==0)
			value &= keyline[6];
		if ((port6529 & 0x80)==0)
			value &= keyline[7];
	
		/* looks like joy 0 needs dataline2 low
		 * and joy 1 needs dataline1 low
		 * write to 0xff08 (value on databus) reloads latches */
		if ((databus & 4)==0) {
			value &= keyline[8];
		}
		if ((databus & 2)==0) {
			value &= keyline[9];
		}
		return value;
	}
	
	/*
	 * mos 6529
	 * simple 1 port 8bit input output
	 * output with pull up resistors, 0 means low
	 * input, 0 means low
	 */
	/*
	 * ic used as output,
	 * output low means keyboard line selected
	 * keyboard line is then read into the ted7360 latch
	 */
	public static WriteHandlerPtr c16_6529_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                port6529 = data;
            }
        };
	
	public static ReadHandlerPtr c16_6529_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return port6529 & (c16_read_keyboard (0xff /*databus */ ) | (port6529 ^ 0xff));
            }
        };
	
	/*
	 * p0 Userport b
	 * p1 Userport k
	 * p2 Userport 4, cassette sense
	 * p3 Userport 5
	 * p4 Userport 6
	 * p5 Userport 7
	 * p6 Userport j
	 * p7 Userport f
	 */
	public static WriteHandlerPtr plus4_6529_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                // nothing to do
            }
        };
	
	public static ReadHandlerPtr plus4_6529_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		int data = 0;
	
		/*TODO*///if (vc20_tape_switch () != 0)
		/*TODO*///	data |= 4;
		return data;
            }
        };
	
	public static ReadHandlerPtr c16_fd1x_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		int data = 0;
	
		/*TODO*///if (vc20_tape_switch () != 0)
		/*TODO*///	data |= 4;
		return data;
            }
        };
	
	/**
	 0 write: transmit data
	 0 read: receiver data
	 1 write: programmed rest (data is dont care)
	 1 read: status register
	 2 command register
	 3 control register
	 control register (offset 3)
	  cleared by hardware reset, not changed by programmed reset
	  7: 2 stop bits (0 1 stop bit)
	  6,5: data word length
	   00 8 bits
	   01 7
	   10 6
	   11 5
	  4: ?? clock source
	   0 external receiver clock
	   1 baud rate generator
	  3-0: baud rate generator
	   0000 use external clock
	   0001 60
	   0010 75
	   0011
	   0100
	   0101
	   0110 300
	   0111 600
	   1000 1200
	   1001
	   1010 2400
	   1011 3600
	   1100 4800
	   1101 7200
	   1110 9600
	   1111 19200
	 control register
	  */
	public static WriteHandlerPtr c16_6551_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		offset &= 3;
		/*TODO*///DBG_LOG (3, "6551", ("port write %.2x %.2x\n", offset, data));
		port6529 = data;
            }
        };
	
	public static ReadHandlerPtr c16_6551_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		int data = 0;
	
		offset &= 3;
		/*TODO*///DBG_LOG (3, "6551", ("port read %.2x %.2x\n", offset, data));
		return data;
            }
        };
	
	public static WriteHandlerPtr c16_write_3f20 = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		c16_memory.write(0x3f20 + offset, data);
            }
        };
	
	public static WriteHandlerPtr c16_write_3f40 = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		c16_memory.write(0x3f40 + offset, data);
            }
        };
	
	public static WriteHandlerPtr c16_write_7f20 = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		c16_memory.write(0x7f20 + offset, data);
            }
        };
	
	public static WriteHandlerPtr c16_write_7f40 = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		c16_memory.write(0x7f40 + offset, data);
            }
        };
	
	public static ReadHandlerPtr ted7360_dma_read_16k = new ReadHandlerPtr() {
            public int handler(int offset) {
		return c16_memory.read(offset & 0x3fff);
            }
        };
	
	public static ReadHandlerPtr ted7360_dma_read_32k = new ReadHandlerPtr() {
            public int handler(int offset) {
		return c16_memory.read(offset & 0x7fff);
            }
        };
	
	public static ReadHandlerPtr ted7360_dma_read = new ReadHandlerPtr() {
            public int handler(int offset) {
		return c16_memory.read(offset);
            }
        };
	
	public static ReadHandlerPtr ted7360_dma_read_rom = new ReadHandlerPtr() {
            public int handler(int offset) {
		/* should read real c16 system bus from 0xfd00 -ff1f */
		if (offset >= 0xc000)
		{								   /* rom address in rom */
			if ((offset >= 0xfc00) && (offset < 0xfd00))
				return c16_memory_10000.read(offset);
			switch (highrom)
			{
			case 0:
				return c16_memory_10000.read(offset & 0x7fff);
			case 1:
				return c16_memory_18000.read(offset & 0x7fff);
			case 2:
				return c16_memory_20000.read(offset & 0x7fff);
			case 3:
				return c16_memory_28000.read(offset & 0x7fff);
			}
		}
		if (offset >= 0x8000)
		{								   /* rom address in rom */
			switch (lowrom)
			{
			case 0:
				return c16_memory_10000.read(offset & 0x7fff);
			case 1:
				return c16_memory_18000.read(offset & 0x7fff);
			case 2:
				return c16_memory_20000.read(offset & 0x7fff);
			case 3:
				return c16_memory_28000.read(offset & 0x7fff);
			}
		}
		switch (DIPMEMORY())
		{
		case MEMORY16K:
			return c16_memory.read(offset & 0x3fff);
		case MEMORY32K:
			return c16_memory.read(offset & 0x7fff);
		case MEMORY64K:
			return c16_memory.read(offset);
		}
		/*TODO*///exit (0);
                return 0;
            }
        };
        
        static int old_level;
	
	public static void c16_interrupt (int level)
	{
		
	
		if (level != old_level)
		{
			/*TODO*///DBG_LOG (3, "mos7501", ("irq %s\n", level ? "start" : "end"));
			cpu_set_irq_line (0, M6510_IRQ_LINE, level);
			old_level = level;
		}
	}
	
	static void c16_common_driver_init ()
	{
	/*TODO*///#ifdef VC1541
	/*TODO*///	VC1541_CONFIG vc1541= { 1, 8 };
	/*TODO*///#endif
	/*TODO*///	C1551_CONFIG config= { 1 };
	
		c16_select_roms.handler(0, 0);
		c16_switch_to_rom.handler(0, 0);
	
		/*TODO*///if (REAL_C1551() != 0) {
		/*TODO*///	tpi6525[2].a.read=c1551x_0_read_data;
		/*TODO*///	tpi6525[2].a.output=c1551x_0_write_data;
		/*TODO*///	tpi6525[2].b.read=c1551x_0_read_status;
		/*TODO*///	tpi6525[2].c.read=c1551x_0_read_handshake;
		/*TODO*///	tpi6525[2].c.output=c1551x_0_write_handshake;
		/*TODO*///} else {
		/*TODO*///	tpi6525[2].a.read=c1551_0_read_data;
		/*TODO*///	tpi6525[2].a.output=c1551_0_write_data;
		/*TODO*///	tpi6525[2].b.read=c1551_0_read_status;
		/*TODO*///	tpi6525[2].c.read=c1551_0_read_handshake;
		/*TODO*///	tpi6525[2].c.output=c1551_0_write_handshake;
		/*TODO*///}
	
		/*TODO*///tpi6525[3].a.read=c1551_1_read_data;
		/*TODO*///tpi6525[3].a.output=c1551_1_write_data;
		/*TODO*///tpi6525[3].b.read=c1551_1_read_status;
		/*TODO*///tpi6525[3].c.read=c1551_1_read_handshake;
		/*TODO*///tpi6525[3].c.output=c1551_1_write_handshake;
	
		c16_memory_10000 = new UBytePtr(c16_memory, 0x10000);
		c16_memory_14000 = new UBytePtr(c16_memory, 0x14000);
		c16_memory_18000 = new UBytePtr(c16_memory, 0x18000);
		c16_memory_1c000 = new UBytePtr(c16_memory, 0x1c000);
		c16_memory_20000 = new UBytePtr(c16_memory, 0x20000);
		c16_memory_24000 = new UBytePtr(c16_memory, 0x24000);
		c16_memory_28000 = new UBytePtr(c16_memory, 0x28000);
		c16_memory_2c000 = new UBytePtr(c16_memory, 0x2c000);
	
		/*    memset(c16_memory, 0, 0xfd00); */
		/* need to recognice non available tia6523's (iec8/9) */
		memset (new UBytePtr(c16_memory, 0xfdc0), 0xff, 0x40);
	
	
		memset (new UBytePtr(c16_memory, 0xfd40), 0xff, 0x20);
	
		/*TODO*///c16_tape_open ();
	
		cbm_drive_open ();
		cbm_drive_attach_fs (0);
		cbm_drive_attach_fs (1);
	
		/*TODO*///if (REAL_C1551() != 0)
		/*TODO*///	c1551_config (0, 0, config);
	
	/*TODO*///#ifdef VC1541
		/*TODO*///if (REAL_VC1541() != 0)
		/*TODO*///	vc1541_config (0, 0, vc1541);
	/*TODO*///#endif
	}
	
	public static void c16_driver_init ()
	{
		c16_common_driver_init ();
		ted7360_init (C16_PAL());
		ted7360_set_dma (ted7360_dma_read, ted7360_dma_read_rom);
	}
	
	public static void c16_driver_shutdown ()
	{
		/*TODO*///vc20_tape_close ();
		cbm_drive_close ();
	}
	
	public static WriteHandlerPtr c16_sidcart_16k = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                c16_memory.write(0x1400+offset, data);
		c16_memory.write(0x5400+offset, data);
		c16_memory.write(0x9400+offset, data);
		c16_memory.write(0xd400+offset, data);
		/*TODO*///sid6581_0_port_w(offset,data);
            }
        };
	
	public static WriteHandlerPtr c16_sidcart_32k = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		c16_memory.write(0x5400+offset, data);
		c16_memory.write(0xd400+offset, data);
		/*TODO*///sid6581_0_port_w(offset,data);
            }
        };
	
	public static WriteHandlerPtr c16_sidcart_64k = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		c16_memory.write(0xd400+offset, data);
		/*TODO*///sid6581_0_port_w(offset,data);
            }
        };
	
	public static InitMachinePtr c16_init_machine = new InitMachinePtr() {
            public void handler() {
                int i;
                
                c16_memory = new UBytePtr(memory_region(REGION_CPU1));
	
		tpi6525_2_reset();
		tpi6525_3_reset();
	
		/*TODO*///sid6581_reset(0);
		/*TODO*///if (SIDCARD() != 0) {
		/*TODO*///	sid6581_set_type(0, MOS8580);
		/*TODO*///	install_mem_read_handler (0, 0xfd40, 0xfd5f, sid6581_0_port_r);
		/*TODO*///	install_mem_write_handler (0, 0xfd40, 0xfd5f, sid6581_0_port_w);
		/*TODO*///	install_mem_read_handler (0, 0xfe80, 0xfe9f, sid6581_0_port_r);
		/*TODO*///	install_mem_write_handler (0, 0xfe80, 0xfe9f, sid6581_0_port_w);
		/*TODO*///} else {
			install_mem_read_handler (0, 0xfd40, 0xfd5f, MRA_NOP);
			install_mem_write_handler (0, 0xfd40, 0xfd5f, MWA_NOP);
			install_mem_read_handler (0, 0xfe80, 0xfe9f, MRA_NOP);
			install_mem_write_handler (0, 0xfe80, 0xfe9f, MWA_NOP);
		/*TODO*///}
	
	/*TODO*///#if 0
	/*TODO*///	c16_switch_to_rom.handler(0, 0);
	/*TODO*///	c16_select_roms.handler(0, 0);
	/*TODO*///#endif
		if (TYPE_C16() != 0)
		{
			cpu_setbank (1, (DIPMEMORY() == MEMORY16K) ? new UBytePtr(c16_memory) : new UBytePtr(c16_memory, 0x4000));
			switch (DIPMEMORY())
			{
			case MEMORY16K:
				cpu_setbank (5, c16_memory);
				cpu_setbank (6, c16_memory);
				cpu_setbank (7, c16_memory);
	/*TODO*///#ifdef NEW_BANKHANDLER
	/*TODO*///			/* causes problems to do this */
	/*TODO*///			/* seeable with c16 and 32k ram extension */
	/*TODO*///			install_mem_write_handler (0, 0x8000, 0xbfff, MWA_BANK6);
	/*TODO*///			install_mem_write_handler (0, 0x4000, 0x7fff, MWA_BANK5);
	/*TODO*///			install_mem_write_handler (0, 0xc000, 0xfcff, MWA_BANK7);
	/*TODO*///#endif
				install_mem_write_handler (0, 0xff20, 0xff3d, c16_write_3f20);
				install_mem_write_handler (0, 0xff40, 0xffff, c16_write_3f40);
				if (SIDCARD_HACK() != 0) {
					install_mem_write_handler (0, 0xd400, 0xd41f, c16_sidcart_16k);
				}
				ted7360_set_dma (ted7360_dma_read_16k, ted7360_dma_read_rom);
				break;
			case MEMORY32K:
	/*TODO*///#ifdef NEW_BANKHANDLER
	/*TODO*///			install_mem_write_handler (0, 0x4000, 0x7fff, MWA_RAM);
	/*TODO*///			cpu_setbank (5, c16_memory);
	/*TODO*///			install_mem_write_handler (0, 0x8000, 0xfcff, MWA_BANK5);
	/*TODO*///			cpu_setbank (6, (c16_memory + 0x7f20));
	/*TODO*///			install_mem_write_handler (0, 0xff20, 0xff3d, MWA_BANK6);
	/*TODO*///			cpu_setbank (7, (c16_memory + 0x7f40));
	/*TODO*///			install_mem_write_handler (0, 0xff40, 0xffff, MWA_BANK7);
	/*TODO*///#else
				cpu_setbank (5, new UBytePtr(c16_memory, 0x4000));
				cpu_setbank (6, new UBytePtr(c16_memory));
				cpu_setbank (7, new UBytePtr(c16_memory, 0x4000));
				install_mem_write_handler (0, 0xff20, 0xff3d, c16_write_7f20);
				install_mem_write_handler (0, 0xff40, 0xffff, c16_write_7f40);
	/*TODO*///#endif
				ted7360_set_dma (ted7360_dma_read_32k, ted7360_dma_read_rom);
				if (SIDCARD_HACK() != 0) {
					install_mem_write_handler (0, 0xd400, 0xd41f, c16_sidcart_32k);
				}
				break;
			case MEMORY64K:
				install_mem_write_handler (0, 0x4000, 0xfcff, MWA_RAM);
				if (SIDCARD_HACK() != 0) {
					install_mem_write_handler (0, 0xd400, 0xd41f, c16_sidcart_64k);
				}
				install_mem_write_handler (0, 0xff20, 0xff3d, MWA_RAM);
				install_mem_write_handler (0, 0xff40, 0xffff, MWA_RAM);
				ted7360_set_dma (ted7360_dma_read, ted7360_dma_read_rom);
				break;
			}
		}
		else
		{
			install_mem_write_handler (0, 0x4000, 0xfcff, MWA_RAM);
			if (SIDCARD_HACK() != 0) {
				install_mem_write_handler (0, 0xd400, 0xd41f, c16_sidcart_64k);
			}
			ted7360_set_dma (ted7360_dma_read, ted7360_dma_read_rom);
		}
		if (IEC8ON()!=0||REAL_C1551()!=0)
		{
			install_mem_write_handler (0, 0xfee0, 0xfeff, tpi6525_2_port_w);
			install_mem_read_handler (0, 0xfee0, 0xfeff, tpi6525_2_port_r);
		}
		else
		{
			install_mem_write_handler (0, 0xfee0, 0xfeff, MWA_NOP);
			install_mem_read_handler (0, 0xfee0, 0xfeff, MRA_NOP);
		}
		if (IEC9ON() != 0)
		{
			install_mem_write_handler (0, 0xfec0, 0xfedf, tpi6525_3_port_w);
			install_mem_read_handler (0, 0xfec0, 0xfedf, tpi6525_3_port_r);
		}
		else
		{
			install_mem_write_handler (0, 0xfec0, 0xfedf, MWA_NOP);
			install_mem_read_handler (0, 0xfec0, 0xfedf, MRA_NOP);
		}
	
		if (SERIAL8ON() != 0)
			i = SERIAL;
		else if (IEC8ON() != 0)
			i = IEC;
		else
			i = 0;
		cbm_drive_0_config (i, 8);
		if (SERIAL9ON() != 0)
			i = SERIAL;
		else if (IEC9ON() !=0)
			i = IEC;
		else
			i = 0;
		cbm_drive_1_config (i, 9);
	
		/*TODO*///if (REAL_C1551() != 0)
		/*TODO*///	c1551_reset ();
	
	/*TODO*///#ifdef VC1541
		/*TODO*///if (REAL_VC1541() != 0)
		/*TODO*///	vc1541_reset ();
	/*TODO*///#endif
	
		cbm_serial_reset_write (0);
	
		for (i = 0; rom_specified[i]!=0 && (i < rom_specified.length); i++)
			c16_rom_load (i);
            }
        };
	
	public static StopMachinePtr c16_shutdown_machine = new StopMachinePtr() {
            public void handler() {
            
            }
        };
	
	public static int c16_rom_id (int id)
	{
	    /* magic lowrom at offset 7: $43 $42 $4d */
		/* if at offset 6 stands 1 it will immediatly jumped to offset 0 (0x8000) */
		int retval = 0;
		char magic[] = {0x43, 0x42, 0x4d}; 
                /*TODO*///buffer = new [magic.length];
		String name = device_filename(IO_CARTSLOT,id);
		Object romfile;
		/*TODO*///char *cp;
	
		logerror("c16_rom_id %s\n", name);
		retval = 0;
		if ((romfile = image_fopen (IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0))==null)
		{
			logerror("rom %s not found\n", name);
			return 0;
		}
	
		osd_fseek (romfile, 7, SEEK_SET);
		/*TODO*///osd_fread (romfile, buffer, magic.length);
		osd_fclose (romfile);
	
		/*TODO*///if (memcmp (magic, buffer, magic.length) == 0)
		/*TODO*///{
		/*TODO*///	retval = 1;
		/*TODO*///}
		/*TODO*///else if ((cp = strrchr (name, '.')) != null)
		/*TODO*///{
		/*TODO*///	if ((stricmp (cp + 1, "rom") == 0) || (stricmp (cp + 1, "prg") == 0)
		/*TODO*///		|| (stricmp (cp + 1, "bin") == 0)
		/*TODO*///		|| (stricmp (cp + 1, "lo") == 0) || (stricmp (cp + 1, "hi") == 0))
		/*TODO*///		retval = 1;
		/*TODO*///}
	
			if (retval != 0)
				logerror("rom %s recognized\n", name);
			else
				logerror("rom %s not recognized\n", name);
		return retval;
	}
	
	public static io_initPtr c16_rom_init = new io_initPtr() {
            public int handler(int id) {
                rom_specified[id] = device_filename(IO_CARTSLOT,id) != null ? 1:0;
		return rom_specified[id]!=0 && c16_rom_id(id)==0 ? INIT_FAIL: INIT_PASS;
            }
        };
	
        static int addr = 0;
	
	public static int c16_rom_load (int id)
	{
		String name = device_filename(IO_CARTSLOT,id);
                UBytePtr mem = new UBytePtr(memory_region (REGION_CPU1));
		Object fp;
		int size, read;
		/*TODO*///char *cp;
		
               
		if (name==null) return 1;
		if (c16_rom_id (id)==0)
			return 1;
		fp = image_fopen (IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0);
		if (fp == null)
		{
			logerror("%s file not found\n", name);
			return 1;
		}
	
		size = osd_fsize (fp);
	
		/*TODO*///if ((cp = strrchr (name, '.')) != null)
		/*TODO*///{
		/*TODO*///	if (stricmp (cp, ".prg") == 0)
		/*TODO*///	{
		/*TODO*///		int in;
	
		/*TODO*///		osd_fread_lsbfirst (fp, in, 2);
		/*TODO*///		logerror("rom prg %.4x\n", in);
		/*TODO*///		addr = in+0x20000;
		/*TODO*///		size -= 2;
		/*TODO*///	}
		/*TODO*///}
		if (addr == 0)
		{
			addr = 0x20000;
		}
		logerror("loading rom %s at %.5x size:%.4x\n", name, addr, size);
		read = osd_fread (fp, new UBytePtr(mem, addr), size);
		addr += size;
		osd_fclose (fp);
		if (read != size)
			return 1;
		return 0;
	}
	
        static int quickload = 0;
	
	public static InterruptPtr c16_frame_interrupt = new InterruptPtr() {
            public int handler() {
                int value;
	
		/*TODO*///sid6581_update();
	
		/*TODO*///if (quickload==0 && QUICKLOAD()!=0)
		/*TODO*///	cbm_quick_open (0, 0, c16_memory);
		quickload = QUICKLOAD();
	
		value = 0xff;
		if (KEY_ATSIGN() != 0)
			value &= ~0x80;
		if (KEY_F3() != 0)
			value &= ~0x40;
		if (KEY_F2() != 0)
			value &= ~0x20;
		if (KEY_F1() != 0)
			value &= ~0x10;
		if (KEY_HELP() != 0)
			value &= ~8;
		if (KEY_POUND() != 0)
			value &= ~4;
		if (KEY_RETURN() != 0)
			value &= ~2;
		if (KEY_DEL() != 0)
			value &= ~1;
		keyline[0] = value;
	
		value = 0xff;
		if (KEY_SHIFT() != 0)
			value &= ~0x80;
		if (KEY_E() != 0)
			value &= ~0x40;
		if (KEY_S() != 0)
			value &= ~0x20;
		if (KEY_Z() != 0)
			value &= ~0x10;
		if (KEY_4() != 0)
			value &= ~8;
		if (KEY_A() != 0)
			value &= ~4;
		if (KEY_W() != 0)
			value &= ~2;
		if (KEY_3() != 0)
			value &= ~1;
		keyline[1] = value;
	
		value = 0xff;
		if (KEY_X() != 0)
			value &= ~0x80;
		if (KEY_T() != 0)
			value &= ~0x40;
		if (KEY_F() != 0)
			value &= ~0x20;
		if (KEY_C() != 0)
			value &= ~0x10;
		if (KEY_6() != 0)
			value &= ~8;
		if (KEY_D() != 0)
			value &= ~4;
		if (KEY_R() != 0)
			value &= ~2;
		if (KEY_5() != 0)
			value &= ~1;
		keyline[2] = value;
	
		value = 0xff;
		if (KEY_V() != 0)
			value &= ~0x80;
		if (KEY_U() != 0)
			value &= ~0x40;
		if (KEY_H() != 0)
			value &= ~0x20;
		if (KEY_B() != 0)
			value &= ~0x10;
		if (KEY_8() != 0)
			value &= ~8;
		if (KEY_G() != 0)
			value &= ~4;
		if (KEY_Y() != 0)
			value &= ~2;
		if (KEY_7() != 0)
			value &= ~1;
		keyline[3] = value;
	
		value = 0xff;
		if (KEY_N() != 0)
			value &= ~0x80;
		if (KEY_O() != 0)
			value &= ~0x40;
		if (KEY_K() != 0)
			value &= ~0x20;
		if (KEY_M() != 0)
			value &= ~0x10;
		if (KEY_0() != 0)
			value &= ~8;
		if (KEY_J() != 0)
			value &= ~4;
		if (KEY_I() != 0)
			value &= ~2;
		if (KEY_9() != 0)
			value &= ~1;
		keyline[4] = value;
	
		value = 0xff;
		if (KEY_COMMA() != 0)
			value &= ~0x80;
		if (KEY_MINUS() != 0)
			value &= ~0x40;
		if (KEY_SEMICOLON() != 0)
			value &= ~0x20;
		if (KEY_POINT() != 0)
			value &= ~0x10;
		if (KEY_UP() != 0)
			value &= ~8;
		if (KEY_L() != 0)
			value &= ~4;
		if (KEY_P() != 0)
			value &= ~2;
		if (KEY_DOWN() != 0)
			value &= ~1;
		keyline[5] = value;
	
		value = 0xff;
		if (KEY_SLASH() != 0)
			value &= ~0x80;
		if (KEY_PLUS() != 0)
			value &= ~0x40;
		if (KEY_EQUALS() != 0)
			value &= ~0x20;
		if (KEY_ESC() != 0)
			value &= ~0x10;
		if (KEY_RIGHT() != 0)
			value &= ~8;
		if (KEY_COLON() != 0)
			value &= ~4;
		if (KEY_ASTERIX() != 0)
			value &= ~2;
		if (KEY_LEFT() != 0)
			value &= ~1;
		keyline[6] = value;
	
		value = 0xff;
		if (KEY_STOP() != 0)
			value &= ~0x80;
		if (KEY_Q() != 0)
			value &= ~0x40;
		if (KEY_CBM() != 0)
			value &= ~0x20;
		if (KEY_SPACE() != 0)
			value &= ~0x10;
		if (KEY_2() != 0)
			value &= ~8;
		if (KEY_CTRL() != 0)
			value &= ~4;
		if (KEY_HOME() != 0)
			value &= ~2;
		if (KEY_1() != 0)
			value &= ~1;
		keyline[7] = value;
	
		if (JOYSTICK1_PORT() != 0) {
			value = 0xff;
			if (JOYSTICK_1_BUTTON() != 0)
				{
					if (JOYSTICK_SWAP() != 0)
						value &= ~0x80;
					else
						value &= ~0x40;
				}
			if (JOYSTICK_1_RIGHT() != 0)
				value &= ~8;
			if (JOYSTICK_1_LEFT() != 0)
				value &= ~4;
			if (JOYSTICK_1_DOWN() != 0)
				value &= ~2;
			if (JOYSTICK_1_UP() != 0)
				value &= ~1;
			if (JOYSTICK_SWAP() != 0)
				keyline[9] = value;
			else
				keyline[8] = value;
		}
	
		if (JOYSTICK2_PORT() != 0) {
			value = 0xff;
			if (JOYSTICK_2_BUTTON() != 0)
				{
					if (JOYSTICK_SWAP() != 0)
						value &= ~0x40;
					else
						value &= ~0x80;
				}
			if (JOYSTICK_2_RIGHT() != 0)
				value &= ~8;
			if (JOYSTICK_2_LEFT() != 0)
				value &= ~4;
			if (JOYSTICK_2_DOWN() != 0)
				value &= ~2;
			if (JOYSTICK_2_UP() != 0)
				value &= ~1;
			if (JOYSTICK_SWAP() != 0)
				keyline[8] = value;
			else
				keyline[9] = value;
		}
	
		ted7360_frame_interrupt ();
	
		/*TODO*///vc20_tape_config (DATASSETTE, DATASSETTE_TONE);
		/*TODO*///vc20_tape_buttons (DATASSETTE_PLAY, DATASSETTE_RECORD, DATASSETTE_STOP);
		/*TODO*///set_led_status (1 /*KB_CAPSLOCK_FLAG */ , KEY_SHIFTLOCK ? 1 : 0);
		/*TODO*///set_led_status (0 /*KB_NUMLOCK_FLAG */ , JOYSTICK_SWAP ? 1 : 0);
	
		return ignore_interrupt.handler();
            }
        };
	
}
