/***************************************************************************
	zx.c

    machine driver
	Juergen Buchmueller <pullmoll@t-online.de>, Dec 1999

	TODO:
	Find a clean solution for putting the tape image into memory.
	Right now only loading through the IO emulation works (and takes time :)

****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.*;
import static common.ptr.*;
import static arcadeflex056.osdepend.*;
import static consoleflex056.funcPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpu.z80.z80H.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.osdependH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.deviceH.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.vidhrdw.zx.*;

public class zx
{
	
	/* statics */
	public static int TAPE_PULSE		= 348;
	public static int TAPE_DELAY		= 1648;
	
	static int tape_size = 0;
	static UBytePtr tape_image = null;
	
	static Object tape_file = null;
	static int tape_data = 0x00;
	static int tape_mask = 0x00;
	static timer_entry tape_bit_timer = null;
	static int tape_header = 0;			   /* silence counter */
	static int tape_trailer = 0;
	static int tape_data_offs = 0;
	static int[] tape_dump = new int[16];
	static char[] tape_name = new char[16+1];
	static int tape_name_offs = 0;
	static int tape_name_size = 0;
	
	void init_zx()
	{
		UBytePtr gfx = new UBytePtr(memory_region(REGION_GFX1));
		int i;
	
		for (i = 0; i < 256; i++)
			gfx.write(i, i);
	}
	
	public static opbase_handlerPtr zx_setopbase = new opbase_handlerPtr() {
            public int handler(int address) {
                if ((address & 0x8000) != 0)
			return zx_ula_r(address, REGION_CPU1);
		else
		if (address == 0x0066 && tape_size > 0)
		{
			UBytePtr ram = new UBytePtr(memory_region(REGION_CPU1));
			int border = ram.read(0x4028);
	
			memcpy(new UBytePtr(ram, 0x4009), tape_image, tape_size);
			ram.write(0x4028, border);
			tape_size = 0;
			ram.write(0x03a7, 0xff);
			address = 0x03a6;
		}
		return address;
            }
        };
	{
		
	}
	
	public static opbase_handlerPtr pc8300_setopbase = new opbase_handlerPtr() {
            public int handler(int address) {
		if ((address & 0x8000) != 0)
			return zx_ula_r(address, REGION_GFX2);
		else if (address == 0x0066 && tape_size > 0)
		{
			UBytePtr ram = new UBytePtr(memory_region(REGION_CPU1));
			int border = ram.read(0x4028);
	
			memcpy(new UBytePtr(ram, 0x4009), tape_image, tape_size);
			ram.write(0x4028, border);
			tape_size = 0;
			ram.write(0x03a7, 0xff);
			address = 0x03a6;
		}
		return address;
            }
        };
	
	public static opbase_handlerPtr pow3000_setopbase = new opbase_handlerPtr() {
            public int handler(int address) {
		if ((address & 0x8000) != 0)
			return zx_ula_r(address, REGION_GFX2);
		return address;
            }
        };
	
	static void common_init_machine()
	{
		memory_set_opbase_handler(0, zx_setopbase);
	}
	
	public static InitMachinePtr zx80_init_machine = new InitMachinePtr() {
            public void handler() {
                if ((readinputport(0) & 0x80) != 0)
		{
			install_mem_read_handler(0, 0x4400, 0x7fff, MRA_RAM);
			install_mem_write_handler(0, 0x4400, 0x7fff, MWA_RAM);
		}
		else
		{
			install_mem_read_handler(0, 0x4400, 0x7fff, MRA_NOP);
			install_mem_write_handler(0, 0x4400, 0x7fff, MWA_NOP);
		}
		common_init_machine();
            }
        };
	
	public static InitMachinePtr zx81_init_machine = new InitMachinePtr() {
            public void handler() {
		if ((readinputport(0) & 0x80) != 0)
		{
			install_mem_read_handler(0, 0x4400, 0x7fff, MRA_RAM);
			install_mem_write_handler(0, 0x4400, 0x7fff, MWA_RAM);
		}
		else
		{
			install_mem_read_handler(0, 0x4400, 0x7fff, MRA_NOP);
			install_mem_write_handler(0, 0x4400, 0x7fff, MWA_NOP);
		}
		common_init_machine();
            }
        };
	
	public static InitMachinePtr pc8300_init_machine = new InitMachinePtr() {
            public void handler() {
		memory_set_opbase_handler(0, pc8300_setopbase);
            }
        };
	
	public static InitMachinePtr pow3000_init_machine = new InitMachinePtr() {
            public void handler() {
		memory_set_opbase_handler(0, pow3000_setopbase);
            }
        };
	
	public static StopMachinePtr zx_shutdown_machine = new StopMachinePtr() {
            public void handler() {
            
            }
        };
	
	public static io_initPtr zx_cassette_init = new io_initPtr() {
            public int handler(int id) {
                Object file;
	
		file = image_fopen(IO_CASSETTE, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_READ);
		if (file != null)
		{
			tape_size = osd_fsize(file);
			tape_image = new UBytePtr(tape_size);
			if (tape_image != null)
			{
				if (osd_fread(file, tape_image, tape_size) != tape_size)
				{
					osd_fclose(file);
					return 1;
				}
			}
			else
			{
				tape_size = 0;
			}
			osd_fclose(file);
		}
		return 0;
            }
        };
	
	
	public static io_exitPtr zx_cassette_exit = new io_exitPtr()
	{
            public int handler(int id) {
		if (tape_image != null)
		{
			tape_image = null;
		}
                
                return 1;
            }
        };
	
	static timer_callback tape_bit_shift = new timer_callback() {
            public void handler(int param) {
                int tape_wave = param & 15;
		int tape_bits = param >> 4;
	
		tape_bit_timer = null;
	
		if (tape_header > 0)
		{
			tape_bit_timer = timer_set(TIME_IN_USEC(TAPE_PULSE), 0, tape_bit_shift);
			tape_header--;
			zx_frame_time = 15;
			sprintf(new String(zx_frame_message), "Tape header %5d", tape_header);
			return;
		}
	
		if (tape_trailer > 0)
		{
			tape_bit_timer = timer_set(TIME_IN_USEC(TAPE_PULSE), 0, tape_bit_shift);
			tape_trailer--;
			zx_frame_time = 15;
			sprintf(new String(zx_frame_message), "Tape trailer %5d", tape_trailer);
			return;
		}
	
		if (tape_wave != 0)
		{
			tape_mask ^= 0x80;
			zx_ula_bkgnd(tape_mask!=0 ? 1 : 0);
			if (tape_wave == 1)
			{
				logerror("TAPE wave #%d done (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", param, tape_mask, cpu_get_reg
						(Z80_AF), cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
				tape_bit_timer = timer_set(TIME_IN_USEC(TAPE_DELAY),
										   (tape_bits << 4), tape_bit_shift);
			}
			else
			{
				logerror("TAPE wave #%d      (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", param, tape_mask, cpu_get_reg
						(Z80_AF), cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
				tape_bit_timer = timer_set(TIME_IN_USEC(tape_mask!=0 ? TAPE_PULSE : TAPE_PULSE * 6 / 7),
										   (tape_bits << 4) | (tape_wave - 1), tape_bit_shift);
			}
			return;
		}
	
		if (tape_bits == 0)
		{
			if (tape_name_offs < tape_name_size)
			{
				tape_data = tape_dump[tape_name_offs];
				logerror("TAPE name @$%04X: $%02X (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", tape_name_offs, tape_data,
							tape_mask, cpu_get_reg(Z80_AF), cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
				tape_bits = 8;
				tape_name_offs++;
				zx_frame_time = 15;
				sprintf(new String(zx_frame_message), "Tape name %04X:%02X", tape_name_offs, tape_data);
			}
			else if (osd_fread(tape_file, new UBytePtr(tape_data), 1) == 1)
			{
				logerror("TAPE data @$%04X: $%02X (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", tape_data_offs, tape_data,
							tape_mask, cpu_get_reg(Z80_AF), cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
				tape_bits = 8;
				tape_data_offs++;
				zx_frame_time = 15;
				sprintf(new String(zx_frame_message), "Tape data %04X:%02X", tape_data_offs, tape_data);
			}
			else if (tape_file != null)
			{
				osd_fclose(tape_file);
				tape_file = null;
				tape_trailer = 256 * 8;
				tape_bit_timer = timer_set(TIME_IN_USEC(TAPE_PULSE), 0, tape_bit_shift);
				logerror("TAPE trailer %d\n", tape_trailer);
			}
		}
	
		if (tape_bits != 0)
		{
			tape_bits--;
			if (((tape_data >> tape_bits) & 1) != 0)
			{
				logerror("TAPE get bit #%d:1 (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", tape_bits, tape_mask,
							cpu_get_reg(Z80_AF), cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
				tape_wave = 9;
			}
			else
			{
				logerror("TAPE get bit #%d:0 (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", tape_bits, tape_mask,
							cpu_get_reg(Z80_AF), cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
				tape_mask ^= 0x80;
				zx_ula_bkgnd(tape_mask!=0 ? 1 : 0);
				tape_wave = 4;
			}
			tape_bit_timer = timer_set(TIME_IN_USEC(TAPE_PULSE),
									   (tape_bits << 4) | (tape_wave - 1), tape_bit_shift);
		}
            }
        };
        
        static char zx2pc[] =
        {
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', '"', '$', '$', ':', '?',
                '(', ')', '>', '<', '=', '+', '-', '*',
                '/', ';', ',', '.', '0', '1', '2', '3',
                '4', '5', '6', '7', '8', '9', 'A', 'B',
                'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
                'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
	
	static void extract_name()
	{
		
		int a, de = cpu_get_reg(Z80_DE), i;
		UBytePtr ram = new UBytePtr(memory_region(REGION_CPU1)), name;
	
		tape_name[0] = '\0';
		/* find a LOAD token starting at (DE) */
		/*TODO*///name = memchr(&ram[de], 0xef, 32);
                name = new UBytePtr(ram, de);
		if (name!=null && name.read(1) == 0x0b)	   /* load followed by doublequote? */
		{
			name.inc( 2 );
			for (i = 0; i < 16; i++)
			{
				a = name.read(i);
				tape_dump[i] = a;
				tape_name[i] = zx2pc[a & 0x3f];
				if ((a & 0x80) != 0)
					break;
			}
			tape_name_size = i + 1;
			tape_name[tape_name_size] = '\0';
			tape_name_offs = 0;
			logerror("extracted tape name '%s'\n", tape_name);
		}
		else
		{
			logerror("no tape name found");
		}
	}
        
        static int cycles_last_bit = 0, fast_read_count = 0;
	
	static int zx_tape_get_bit()
	{
		if (tape_file!=null || tape_header!=0 || tape_trailer!=0)
		{
			logerror("      read status (%02X AF:%04X BC:%04X DE:%04X HL:%04X)\n", tape_mask, cpu_get_reg(Z80_AF),
					cpu_get_reg(Z80_BC), cpu_get_reg(Z80_DE), cpu_get_reg(Z80_HL));
		}
		else
		{
			
			int cycles_this_bit;
	
			cycles_this_bit = cpu_gettotalcycles();
			/* check if there's a tight loop reading the tape input */
			if (cycles_this_bit - cycles_last_bit < 64)
			{
				fast_read_count++;
				logerror("TAPE time between reads %d cycles %d times\n", cycles_this_bit - cycles_last_bit, fast_read_count);
				if (fast_read_count > 64)
				{
					extract_name();
					if (tape_name[0] != 0)
					{
						String ext = new String(tape_name).substring(tape_name.length);
	
						ext = ".P";
						tape_file = osd_fopen(Machine.gamedrv.name, new String(tape_name), OSD_FILETYPE_ROM, 0);
						if (tape_file == null)
						{
							ext = ".81";
							tape_file = osd_fopen(Machine.gamedrv.name, new String(tape_name), OSD_FILETYPE_ROM, 0);
						}
						if (tape_file==null && Machine.gamedrv.clone_of!=null)
						{
							ext = ".P";
							tape_file = osd_fopen(Machine.gamedrv.clone_of.name, new String(tape_name), OSD_FILETYPE_ROM, 0);
						}
						if (tape_file==null && Machine.gamedrv.clone_of!=null)
						{
							ext = ".81";
							tape_file = osd_fopen(Machine.gamedrv.clone_of.name, new String(tape_name), OSD_FILETYPE_ROM, 0);
						}
						if (tape_file != null)
						{
							tape_bit_timer = timer_set(TIME_IN_USEC(TAPE_PULSE), 0, tape_bit_shift);
							tape_header = 1024 * 8;
							tape_data_offs = 0;
							tape_mask = 0x80;
							logerror("TAPE header %d\n", tape_header);
						}
					}
				}
			}
			else
			{
				fast_read_count = 0;
			}
			cycles_last_bit = cycles_this_bit;
		}
	
		return tape_mask;
	}
	
	public static WriteHandlerPtr zx_io_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                logerror("IOW %3d $%04X", cpu_getscanline(), offset);
		if ((offset & 2) == 0)
		{
			logerror(" ULA NMIs off\n");
			if (ula_nmi != null)
				timer_remove(ula_nmi);
			ula_nmi = null;
		}
		else if ((offset & 1) == 0)
		{
			logerror(" ULA NMIs on\n");
			ula_nmi = timer_pulse(TIME_IN_CYCLES(207, 0), 0, zx_ula_nmi);
			/* remove the IRQ */
			if (ula_irq != null)
			{
				timer_remove(ula_irq);
				ula_irq = null;
			}
		}
		else
		{
			logerror(" ULA IRQs on\n");
			zx_ula_bkgnd(1);
			if (ula_frame_vsync == 2)
			{
				cpu_spinuntil_time(cpu_getscanlinetime(Machine.drv.screen_height - 1));
				ula_scanline_count = Machine.drv.screen_height - 1;
			}
		}
            }
        };
	
	public static ReadHandlerPtr zx_io_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int data = 0xff;
	
		if ((offset & 1) == 0)
		{
			int extra1 = readinputport(9);
			int extra2 = readinputport(10);
	
			ula_scancode_count = 0;
			if ((offset & 0x0100) == 0)
			{
				data &= readinputport(1);
				/* SHIFT for extra keys */
				if (extra1 != 0xff || extra2 != 0xff)
					data &= ~0x01;
			}
			if ((offset & 0x0200) == 0)
				data &= readinputport(2);
			if ((offset & 0x0400) == 0)
				data &= readinputport(3);
			if ((offset & 0x0800) == 0)
				data &= readinputport(4) & extra1;
			if ((offset & 0x1000) == 0)
				data &= readinputport(5) & extra2;
			if ((offset & 0x2000) == 0)
				data &= readinputport(6);
			if ((offset & 0x4000) == 0)
				data &= readinputport(7);
			if ((offset & 0x8000) == 0)
				data &= readinputport(8);
			if (Machine.drv.frames_per_second > 55)
				data &= ~0x40;
	
			if (ula_irq != null)
			{
				logerror("IOR %3d $%04X data $%02X (ULA IRQs off)\n", cpu_getscanline(), offset, data);
				zx_ula_bkgnd(0);
				timer_remove(ula_irq);
				ula_irq = null;
			}
			else
			{
				data &= ~zx_tape_get_bit();
				logerror("IOR %3d $%04X data $%02X (tape)\n", cpu_getscanline(), offset, data);
			}
			if (ula_frame_vsync == 3)
			{
				ula_frame_vsync = 2;
				logerror("vsync starts in scanline %3d\n", cpu_getscanline());
			}
		}
		else
		{
			logerror("IOR %3d $%04X data $%02X\n", cpu_getscanline(), offset, data);
		}
		return data;
            }
        };
	
	
	public static ReadHandlerPtr pow3000_io_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		int data = 0xff;
	
		if ((offset & 1) == 0)
		{
			int extra1 = readinputport(9);
			int extra2 = readinputport(10);
	
			ula_scancode_count = 0;
			if ((offset & 0x0100) == 0)
			{
				data &= readinputport(1) & extra1;
				/* SHIFT for extra keys */
				if (extra1 != 0xff || extra2 != 0xff)
					data &= ~0x01;
			}
			if ((offset & 0x0200) == 0)
				data &= readinputport(2) & extra2;
			if ((offset & 0x0400) == 0)
				data &= readinputport(3);
			if ((offset & 0x0800) == 0)
				data &= readinputport(4);
			if ((offset & 0x1000) == 0)
				data &= readinputport(5);
			if ((offset & 0x2000) == 0)
				data &= readinputport(6);
			if ((offset & 0x4000) == 0)
				data &= readinputport(7);
			if ((offset & 0x8000) == 0)
				data &= readinputport(8);
			if (Machine.drv.frames_per_second > 55)
				data &= ~0x40;
	
			if (ula_irq != null)
			{
				logerror("IOR %3d $%04X data $%02X (ULA IRQs off)\n", cpu_getscanline(), offset, data);
				zx_ula_bkgnd(0);
				timer_remove(ula_irq);
				ula_irq = null;
			}
			else
			{
				data &= ~zx_tape_get_bit();
				logerror("IOR %3d $%04X data $%02X (tape)\n", cpu_getscanline(), offset, data);
			}
			if (ula_frame_vsync == 3)
			{
				ula_frame_vsync = 2;
				logerror("vsync starts in scanline %3d\n", cpu_getscanline());
			}
		}
		else
		{
			logerror("IOR %3d $%04X data $%02X\n", cpu_getscanline(), offset, data);
		}
		return data;
            }
        };
}
