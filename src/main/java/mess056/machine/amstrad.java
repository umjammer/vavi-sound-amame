/***************************************************************************

  machine.c

  Functions to emulate general aspects of the machine (RAM, ROM, interrupts,
  I/O ports)

Amstrad hardware consists of:

- General Instruments AY-3-8912 (audio and keyboard scanning)
- Intel 8255PPI (keyboard, access to AY-3-8912, cassette etc)
- Z80A CPU
- 765 FDC (disc drive interface)
- "Gate Array" (custom chip by Amstrad controlling colour, mode,
rom/ram selection


***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static common.ptr.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.*;
import static arcadeflex056.fileio.*;
import static arcadeflex056.osdepend.*;
import static consoleflex056.funcPtr.*;
import static mame056.cpu.z80.z80H.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mess056.device.*;
import static mess056.deviceH.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mame056.memory.*;

import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mess056.cassette.*;
import static mess056.cassetteH.*;

import static mess056.systems.amstrad.*;
import static mess056.vidhrdw.m6845.*;
import static mess056.vidhrdw.m6845H.*;
import static mame056.machine._8255ppi.*;
import static mame056.machine._8255ppiH.*;
import static mame056.osdependH.*;
import static mess056.machine.dsk.*;

public class amstrad
{
	public static UBytePtr snapshot = null;
	
	public static int snapshot_loaded = 0;
	
	public static io_initPtr amstrad_floppy_init = new io_initPtr() {
            public int handler(int id) {
                if (device_filename(IO_FLOPPY, id)==null)
			return INIT_PASS;
	
		return dsk_floppy_load.handler(id);
               
            }
        };
	
	
	/* used to setup computer if a snapshot was specified */
	public static opbase_handlerPtr amstrad_opbaseoverride = new opbase_handlerPtr() {
            public int handler(int address) {
                /* clear op base override */
		memory_set_opbase_handler(0,null);
	
		if (snapshot_loaded != 0)
		{
			/* its a snapshot file - setup hardware state */
			amstrad_handle_snapshot(snapshot);
	
			/* free memory */
			snapshot = null;
	
			snapshot_loaded = 0;
	
		}
	
		return (cpunum_get_pc(0) & 0x0ffff);
            }
        };
	
	public static void amstrad_setup_machine()
	{
		/* allocate ram - I control how it is accessed so I must
		allocate it somewhere - here will do */
		Amstrad_Memory = new UBytePtr(128*1024);
		if (Amstrad_Memory == null) return;
	
		if (snapshot_loaded != 0)
		{
			/* setup for snapshot */
			memory_set_opbase_handler(0,amstrad_opbaseoverride);
		}
	
	
		if (snapshot_loaded == 0)
		{
			Amstrad_Reset();
		}
	}
	
	
	
	public static io_initPtr amstrad_cassette_init = new io_initPtr() {
            public int handler(int id) {
                cassette_args args;
		//memset(args, 0, sizeof(args));
                args = new cassette_args();
		args.create_smpfreq = 22050;	/* maybe 11025 Hz would be sufficient? */
		return cassette_init(id, args);
            }
        };
	
	public static io_exitPtr amstrad_cassette_exit = new io_exitPtr() {
            public int handler(int id) {
                device_close(IO_CASSETTE, id);
                
                return INIT_PASS;
            }
        };
	
	/* load CPCEMU style snapshots */
	public static void amstrad_handle_snapshot(UBytePtr pSnapshot)
	{
		int RegData;
		int i;
	
	
		/* init Z80 */
		RegData = (pSnapshot.read(0x011) & 0x0ff) | ((pSnapshot.read(0x012) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_AF, RegData);
	
		RegData = (pSnapshot.read(0x013) & 0x0ff) | ((pSnapshot.read(0x014) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_BC, RegData);
	
		RegData = (pSnapshot.read(0x015) & 0x0ff) | ((pSnapshot.read(0x016) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_DE, RegData);
	
		RegData = (pSnapshot.read(0x017) & 0x0ff) | ((pSnapshot.read(0x018) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_HL, RegData);
	
		RegData = (pSnapshot.read(0x019) & 0x0ff) ;
		cpunum_set_reg(0,Z80_R, RegData);
	
		RegData = (pSnapshot.read(0x01a) & 0x0ff);
		cpunum_set_reg(0,Z80_I, RegData);
	
		if ((pSnapshot.read(0x01b) & 1)==1)
		{
			cpunum_set_reg(0,Z80_IFF1, 1);
		}
		else
		{
			cpunum_set_reg(0,Z80_IFF1, 0);
		}
	
		if ((pSnapshot.read(0x01c) & 1)==1)
		{
			cpunum_set_reg(0,Z80_IFF2, 1);
		}
		else
		{
			cpunum_set_reg(0,Z80_IFF2, 0);
		}
	
		RegData = (pSnapshot.read(0x01d) & 0x0ff) | ((pSnapshot.read(0x01e) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_IX, RegData);
	
		RegData = (pSnapshot.read(0x01f) & 0x0ff) | ((pSnapshot.read(0x020) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_IY, RegData);
	
		RegData = (pSnapshot.read(0x021) & 0x0ff) | ((pSnapshot.read(0x022) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_SP, RegData);
		cpunum_set_sp(0,RegData);
	
		RegData = (pSnapshot.read(0x023) & 0x0ff) | ((pSnapshot.read(0x024) & 0x0ff)<<8);
	
		cpunum_set_reg(0,Z80_PC, RegData);
	//	cpu_set_pc(RegData);
	
		RegData = (pSnapshot.read(0x025) & 0x0ff);
		cpunum_set_reg(0,Z80_IM, RegData);
	
		RegData = (pSnapshot.read(0x026) & 0x0ff) | ((pSnapshot.read(0x027) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_AF2, RegData);
	
		RegData = (pSnapshot.read(0x028) & 0x0ff) | ((pSnapshot.read(0x029) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_BC2, RegData);
	
		RegData = (pSnapshot.read(0x02a) & 0x0ff) | ((pSnapshot.read(0x02b) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_DE2, RegData);
	
		RegData = (pSnapshot.read(0x02c) & 0x0ff) | ((pSnapshot.read(0x02d) & 0x0ff)<<8);
		cpunum_set_reg(0,Z80_HL2, RegData);
	
		/* init GA */
		for (i=0; i<17; i++)
		{
			AmstradCPC_GA_Write(i);
	
			AmstradCPC_GA_Write(((pSnapshot.read(0x02f + i) & 0x01f) | 0x040));
		}
	
		AmstradCPC_GA_Write(pSnapshot.read(0x02e) & 0x01f);
	
		AmstradCPC_GA_Write(((pSnapshot.read(0x040) & 0x03f) | 0x080));
	
		AmstradCPC_GA_Write(((pSnapshot.read(0x041) & 0x03f) | 0x0c0));
	
		/* init CRTC */
		for (i=0; i<18; i++)
		{
	                crtc6845_address_w(0,i);
	                crtc6845_register_w(0, pSnapshot.read(0x043+i) & 0x0ff);
		}
	
                crtc6845_address_w(0,i);
	
		/* upper rom selection */
		AmstradCPC_SetUpperRom(pSnapshot.read(0x055));
	
		/* PPI */
		ppi8255_w(0,3,pSnapshot.read(0x059) & 0x0ff);
	
		ppi8255_w(0,0,pSnapshot.read(0x056) & 0x0ff);
		ppi8255_w(0,1,pSnapshot.read(0x057) & 0x0ff);
		ppi8255_w(0,2,pSnapshot.read(0x058) & 0x0ff);
	
		/* PSG */
		for (i=0; i<16; i++)
		{
			AY8910_control_port_0_w.handler(0,i);
	
			AY8910_write_port_0_w.handler(0,pSnapshot.read(0x05b + i) & 0x0ff);
		}
	
		AY8910_control_port_0_w.handler(0,pSnapshot.read(0x05a));
	
		{
			int MemSize;
			int MemorySize;
	
			MemSize = (pSnapshot.read(0x06b) & 0x0ff) | ((pSnapshot.read(0x06c) & 0x0ff)<<8);
	
			if (MemSize==128)
			{
				MemorySize = 128*1024;
			}
			else
			{
				MemorySize = 64*1024;
			}
	
			memcpy(Amstrad_Memory, new UBytePtr(pSnapshot, 0x0100), MemorySize);
	
		}
	
		Amstrad_RethinkMemory();
	
	}
	
	/* load image */
	public static UBytePtr amstrad_load(int type, int id, UBytePtr ptr)
	{
		Object file;
	
		file = image_fopen(type, id, OSD_FILETYPE_IMAGE_R, OSD_FOPEN_READ);
	
		if (file != null)
		{
			int datasize;
			//UBytePtr data;
	
			/* get file size */
			datasize = osd_fsize(file);
	
			if (datasize!=0)
			{
				/* malloc memory for this data */
				ptr = new UBytePtr(datasize);
	
				if (ptr!=null)
				{
					/* read whole file */
					osd_fread(file, ptr, datasize);
	
					System.out.println("amstrad_load");
                                        //System.out.println(ptr);
                                        //System.out.println(ptr.memory);
                                        //ptr = new UBytePtr(data);
	
					/* close file */
					osd_fclose(file);
	
					logerror("File loaded!\r\n");
	
					/* ok! */
					return ptr;
				}
				osd_fclose(file);
	
			}
		}
	
		return null;
	}
	
	/* load snapshot */
	public static io_initPtr amstrad_snapshot_load = new io_initPtr() {
            public int handler(int id) {
                /* machine can be started without a snapshot */
		/* if filename not specified, then init is ok */
		if (device_filename(IO_SNAPSHOT, id)==null)
			return INIT_PASS;
	
		/* filename specified */
		snapshot_loaded = 0;
                snapshot = new UBytePtr();
	
		/* load and verify image */
		if ((snapshot=amstrad_load(IO_SNAPSHOT,id,snapshot)) != null)
		{
			snapshot_loaded = 1;
                        //System.out.println(snapshot);
                        //System.out.println(snapshot.memory);
			if (memcmp(snapshot.memory, 0, "MV - SNA", 8)==0)
				return INIT_PASS;
			else
				return INIT_FAIL;
		}
	
		return INIT_FAIL;
            }
        };
	
	public static io_exitPtr amstrad_snapshot_exit = new io_exitPtr() {
            public int handler(int id) {
                if (snapshot!=null)
			snapshot = null;
	
		snapshot_loaded = 0;
                
                return INIT_PASS;
            }
        };
	
	public static io_initPtr amstrad_plus_cartridge_init = new io_initPtr() {
            public int handler(int id) {
                /* cpc+ requires a cartridge to be inserted to run */
		if (device_filename(IO_CARTSLOT, id)==null)
			return INIT_FAIL;
	
		return INIT_PASS;
            }
        };
	
	public static io_exitPtr amstrad_plus_cartridge_exit = new io_exitPtr() {
            public int handler(int id) {
                return INIT_PASS;
            }
        };
	
}
