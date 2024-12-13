/***************************************************************************
		Spectrum/Inves/TK90X etc. memory map:

	CPU:
		0000-3fff ROM
		4000-ffff RAM

		Spectrum 128/+2/+2a/+3 memory map:

		CPU:
				0000-3fff Banked ROM/RAM (banked rom only on 128/+2)
				4000-7fff Banked RAM
				8000-bfff Banked RAM
				c000-ffff Banked RAM

		TS2068 memory map: (Can't have both EXROM and DOCK active)
		The 8K EXROM can be loaded into multiple pages.

	CPU:
				0000-1fff	  ROM / EXROM / DOCK (Cartridge)
				2000-3fff	  ROM / EXROM / DOCK
				4000-5fff \
				6000-7fff  \
				8000-9fff  |- RAM / EXROM / DOCK
				a000-bfff  |
				c000-dfff  /
				e000-ffff /


Interrupts:

Changes:

29/1/2000	KT -	Implemented initial +3 emulation.
30/1/2000	KT -	Improved input port decoding for reading and therefore
			correct keyboard handling for Spectrum and +3.
31/1/2000	KT -	Implemented buzzer sound for Spectrum and +3.
			Implementation copied from Paul Daniel's Jupiter driver.
			Fixed screen display problems with dirty chars.
			Added support to load .Z80 snapshots. 48k support so far.
13/2/2000	KT -	Added Interface II, Kempston, Fuller and Mikrogen
			joystick support.
17/2/2000	DJR -	Added full key descriptions and Spectrum+ keys.
			Fixed Spectrum +3 keyboard problems.
17/2/2000	KT -	Added tape loading from WAV/Changed from DAC to generic
			speaker code.
18/2/2000	KT -	Added tape saving to WAV.
27/2/2000	KT -	Took DJR's changes and added my changes.
27/2/2000	KT -	Added disk image support to Spectrum +3 driver.
27/2/2000	KT -	Added joystick I/O code to the Spectrum +3 I/O handler.
14/3/2000	DJR -	Tape handling dipswitch.
26/3/2000	DJR -	Snapshot files are now classifed as snapshots not
			cartridges.
04/4/2000	DJR -	Spectrum 128 / +2 Support.
13/4/2000	DJR -	+4 Support (unofficial 48K hack).
13/4/2000	DJR -	+2a Support (rom also used in +3 models).
13/4/2000	DJR -	TK90X, TK95 and Inves support (48K clones).
21/4/2000	DJR -	TS2068 and TC2048 support (TC2048 Supports extra video
			modes but doesn't have bank switching or sound chip).
09/5/2000	DJR -	Spectrum +2 (France, Spain), +3 (Spain).
17/5/2000	DJR -	Dipswitch to enable/disable disk drives on +3 and clones.
27/6/2000	DJR -	Changed 128K/+3 port decoding (sound now works in Zub 128K).
06/8/2000	DJR -	Fixed +3 Floppy support
10/2/2001	KT  -	Re-arranged code and split into each model emulated.
			Code is split into 48k, 128k, +3, tc2048 and ts2048
			segments. 128k uses some of the functions in 48k, +3
			uses some functions in 128, and tc2048/ts2048 use some
			of the functions in 48k. The code has been arranged so
			these functions come in some kind of "override" order,
			read functions changed to use READ_HANDLER and write
			functions changed to use WRITE_HANDLER.
			Added Scorpion256 preliminary.
18/6/2001	DJR -	Added support for Interface 2 cartridges.
xx/xx/2001	KS -	TS-2068 sound fixed.
			Added support for DOCK cartridges for TS-2068.
			Added Spectrum 48k Psycho modified rom driver.
			Added UK-2086 driver.
23/12/2001	KS -	48k machines are now able to run code in screen memory.
				Programs which keep their code in screen memory
				like monitors, tape copiers, decrunchers, etc.
				works now.
		     	Fixed problem with interrupt vector set to 0xffff (much
			more 128k games works now).
				A useful used trick on the Spectrum is to set
				interrupt vector to 0xffff (using the table 
				which contain 0xff's) and put a byte 0x18 hex,
				the opcode for JR, at this address. The first
				byte of the ROM is a 0xf3 (DI), so the JR will
				jump to 0xfff4, where a long JP to the actual
				interrupt routine is put. Due to unideal
				bankswitching in MAME this JP were to 0001 what
				causes Spectrum to reset. Fixing this problem
				made much more software runing (i.e. Paperboy).
			Corrected frames per second value for 48k and 128k
			Sincalir machines.
				There are 50.08 frames per second for Spectrum
				48k what gives 69888 cycles for each frame and
				50.021 for Spectrum 128/+2/+2A/+3 what gives
				70908 cycles for each frame. 
			Remaped some Spectrum+ keys.
				Presing F3 to reset was seting 0xf7 on keyboard
				input port. Problem occured for snapshots of
				some programms where it was readed as pressing
				key 4 (which is exit in Tapecopy by R. Dannhoefer
				for example).
			Added support to load .SP snapshots.
			Added .BLK tape images support.
				.BLK files are identical to .TAP ones, extension
				is an only difference.
08/03/2002	KS -	#FF port emulation added.
				Arkanoid works now, but is not playable due to
				completly messed timings.

Initialisation values used when determining which model is being emulated:
 48K		Spectrum doesn't use either port.
 128K/+2	Bank switches with port 7ffd only.
 +3/+2a		Bank switches with both ports.

Notes:
 1. No contented memory.
 2. No hi-res colour effects (need contended memory first for accurate timing).
 3. Multiface 1 and Interface 1 not supported.
 4. Horace and the Spiders cartridge doesn't run properly.
 5. Tape images not supported:
    .TZX, .SPC, .ITM, .PAN, .TAP(Warajevo), .VOC, .ZXS.
 6. Snapshot images not supported:
    .ACH, .PRG, .RAW, .SEM, .SIT, .SNX, .ZX, .ZXS, .ZX82.
 7. 128K emulation is not perfect - the 128K machines crash and hang while
    running quite a lot of games.
 8. Disk errors occur on some +3 games.
 9. Video hardware of all machines is timed incorrectly.
10. EXROM and HOME cartridges are not emulated.
11. The TK90X and TK95 roms output 0 to port #df on start up.
12. The purpose of this port is unknown (probably display mode as TS2068) and
    thus is not emulated.

Very detailed infos about the ZX Spectrum +3e can be found at

http://www.z88forever.org.uk/zxplus3e/

*******************************************************************************/

/*
 * ported to v0.56.1
 * using automatic conversion tool v0.01
 */ 
package mess056.systems;

import static mame056.sound.ay8910H.*;
import static mame056.sound.ay8910.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import common.subArrays.IntArray;
import consoleflex056.funcPtr.StopMachinePtr;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mess056.deviceH.*;
import static mess056.device.*;
import static mess056.eventlst.*;
import static mess056.eventlstH.*;
import static mess056.includes.flopdrvH.floppy_type.FLOPPY_DRIVE_SS_40;
import static mess056.sound.speaker.*;
import static mess056.sound.speakerH.*;
import static mess056.vidhrdw.spectrum.*;
import static mess056.machine.spectrum.*;
import static mess056.machine.flopdrv.*;
import static mess056.includes.spectrumH.*;
import static mess056.includes.spectrumH.TIMEX_CART_TYPE.TIMEX_CART_DOCK;
import static mess056.messH.*;
import static mess056.sound.waveH.*;
import static mess056.sound.wave.*;
import static mess056.includes.nec765H.*;
import static mess056.machine.dsk.*;
import static mess056.machine.nec765.*;
import static mess056.machine.wd179x.*;

public class spectrum
{
    static AY8910interface spectrum_ay_interface = new AY8910interface
    (
            1,
            1773400,
            new int[] {25,25},
            new ReadHandlerPtr[] {null},
            new ReadHandlerPtr[] {null},
            new WriteHandlerPtr[] {null},
            new WriteHandlerPtr[] {null}
    );

    /****************************************************************************************************/
    /* Spectrum 48k functions */

    /*
     bit 7-5: not used
     bit 4: Ear output/Speaker
     bit 3: MIC/Tape Output
     bit 2-0: border colour
    */

    public static int PreviousFE = 0;

    public static WriteHandlerPtr spectrum_port_fe_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        
            int Changed;

            Changed = PreviousFE^data;

            /* border colour changed? */
            if ((Changed & 0x07)!=0)
            {
                    /* yes - send event */
                    EventList_AddItemOffset(0x0fe, data & 0x07, TIME_TO_CYCLES(0,cpu_getscanline()*cpu_getscanlineperiod()));
            }

            if ((Changed & (1<<4))!=0)
            {
                    /* DAC output state */
                    speaker_level_w(0,(data>>4) & 0x01);
            }

            
            if ((Changed & (1<<3))!=0)
            {
                    // Sounds while saving (added by ChusoGar@gmail.com)
                    speaker_level_w(0,(data>>3) & 0x01);
                    
                    /* write cassette data */
                    device_output(IO_CASSETTE, 0, (data & (1<<3))!=0 ? -32768: 32767);
            }

            PreviousFE = data;
        }
    };

    static Memory_ReadAddress spectrum_readmem[] = {
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress(0x0000, 0x3fff, MRA_ROM ),
            new Memory_ReadAddress(0x4000, 0x57ff, spectrum_characterram_r ),
            new Memory_ReadAddress(0x5800, 0x5aff, spectrum_colorram_r ),
            new Memory_ReadAddress(0x5b00, 0xffff, MRA_RAM ),
            new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */
        };

        static Memory_WriteAddress spectrum_writemem[] = {
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress(0x0000, 0x3fff, MWA_ROM ),
            new Memory_WriteAddress(0x4000, 0x57ff, spectrum_characterram_w ),
            new Memory_WriteAddress(0x5800, 0x5aff, spectrum_colorram_w ),
            new Memory_WriteAddress(0x5b00, 0xffff, MWA_RAM ),
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */
        };

        /* KT: more accurate keyboard reading */
        /* DJR: Spectrum+ keys added */
        public static ReadHandlerPtr spectrum_port_fe_r = new ReadHandlerPtr() {
            public int handler(int offset) {
            
                int lines = offset>>8;
                int data = 0xff;

                int cs_extra1 = readinputport(8)  & 0x1f;
                int cs_extra2 = readinputport(9)  & 0x1f;
                int cs_extra3 = readinputport(10) & 0x1f;
                int ss_extra1 = readinputport(11) & 0x1f;
                int ss_extra2 = readinputport(12) & 0x1f;

                /* Caps - V */
                if ((lines & 1)==0)
                {
                             data &= readinputport(0);
                             /* CAPS for extra keys */
                             if (cs_extra1 != 0x1f || cs_extra2 != 0x1f || cs_extra3 != 0x1f)
                                     data &= ~0x01;
                }

                /* A - G */
                if ((lines & 2)==0)
                             data &= readinputport(1);

                /* Q - T */
                if ((lines & 4)==0)
                             data &= readinputport(2);

                /* 1 - 5 */
                if ((lines & 8)==0)
                             data &= readinputport(3) & cs_extra1;

                /* 6 - 0 */
                if ((lines & 16)==0)
                             data &= readinputport(4) & cs_extra2;

                /* Y - P */
                if ((lines & 32)==0)
                             data &= readinputport(5) & ss_extra1;

                /* H - Enter */
                if ((lines & 64)==0)
                             data &= readinputport(6);

                     /* B - Space */
                     if ((lines & 128)==0)
                     {
                             data &= readinputport(7) & cs_extra3 & ss_extra2;
                             /* SYMBOL SHIFT for extra keys */
                             if (ss_extra1 != 0x1f || ss_extra2 != 0x1f)
                                     data &= ~0x02;
                     }

                     data |= (0xe0); /* Set bits 5-7 - as reset above */

                     /* cassette input from wav */
                     if (device_input(IO_CASSETTE, 0)>255 )
                     {
                             data &= ~0x40;
                     }

                     /* Issue 2 Spectrums default to having bits 5, 6 & 7 set.
                     Issue 3 Spectrums default to having bits 5 & 7 set and bit 6 reset. */
                     if ((readinputport(16) & 0x80) != 0)
                             data ^= (0x40);
                     return data;
             }
        };

        /* kempston joystick interface */
        public static ReadHandlerPtr spectrum_port_1f_r = new ReadHandlerPtr() {
            public int handler(int offset) {            
                return readinputport(13) & 0x1f;
            }
        };

        /* fuller joystick interface */
        public static ReadHandlerPtr spectrum_port_7f_r = new ReadHandlerPtr() {
            public int handler(int offset) {        
                return readinputport(14) | (0xff^0x8f);
            }
        };

        /* mikrogen joystick interface */
        public static ReadHandlerPtr spectrum_port_df_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return readinputport(15) | (0xff^0x1f);
            }
        };

        public static ReadHandlerPtr spectrum_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
            
                        if ((offset & 1)==0)
                                return spectrum_port_fe_r.handler(offset);

                        if ((offset & 0xff)==0x1f)
                                return spectrum_port_1f_r.handler(offset);

                        if ((offset & 0xff)==0x7f)
                                return spectrum_port_7f_r.handler(offset);

                        if ((offset & 0xff)==0xdf)
                                return spectrum_port_df_r.handler(offset);

                        return cpu_getscanline()<193 ? spectrum_colorram.read((cpu_getscanline()&0xf8)<<2):0xff;
            }
        };

        public static WriteHandlerPtr spectrum_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                        if ((offset & 1)==0)
                                spectrum_port_fe_w.handler(offset,data);
                        else
                        {
                                logerror("Write %02x to Port: %04x\n", data, offset);
                        }
            }
        };

        /* ports are not decoded full.
        The function decodes the ports appropriately */
        public static IO_ReadPort spectrum_readport[] = {
            new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_ReadPort(0x0000, 0xffff, spectrum_port_r),
            new IO_ReadPort(MEMPORT_MARKER, 0) /* end of table */
        };

        /* ports are not decoded full.
        The function decodes the ports appropriately */
        public static IO_WritePort spectrum_writeport[] = {
            new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_WritePort(0x0000, 0xffff, spectrum_port_w),
            new IO_WritePort(MEMPORT_MARKER, 0) /* end of table */
        };

        /****************************************************************************************************/
        /* functions and data used by spectrum 128, spectrum +2, spectrum +3 and scorpion */
        public static UBytePtr spectrum_ram = null;

        static int spectrum_alloc_ram(int ram_size_in_k)
        {
                spectrum_ram = new UBytePtr(ram_size_in_k*1024);
                if (spectrum_ram != null)
                {
                        memset(spectrum_ram, 0, ram_size_in_k*1024);
                        return 1;
                }

                return 0;
        }

        static void spectrum_free_ram()
        {
                if (spectrum_ram != null)
                {
                        spectrum_ram = null;
                }
        }


        /****************************************************************************************************/
        /* Spectrum 128 specific functions */

        public static int spectrum_128_port_7ffd_data = -1;
        public static UBytePtr spectrum_128_screen_location = null;

        public static WriteHandlerPtr spectrum_128_port_7ffd_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                /* D0-D2: RAM page located at 0x0c000-0x0ffff */
                /* D3 - Screen select (screen 0 in ram page 5, screen 1 in ram page 7 */
                /* D4 - ROM select - which rom paged into 0x0000-0x03fff */
                /* D5 - Disable paging */

                /* disable paging? */
                if ((spectrum_128_port_7ffd_data & 0x20) != 0)
                                return;

                /* store new state */
                spectrum_128_port_7ffd_data = data;

                /* update memory */
                spectrum_128_update_memory();
            }
        };

        public static void spectrum_128_update_memory()
        {
            UBytePtr ChosenROM;
            int ROMSelection;

            if ((spectrum_128_port_7ffd_data & 8) != 0)
            {
                            logerror("SCREEN 1: BLOCK 7\n");
                            spectrum_128_screen_location = new UBytePtr(spectrum_ram, (7<<14));
            }
            else
            {
                            logerror("SCREEN 0: BLOCK 5\n");
                            spectrum_128_screen_location = new UBytePtr(spectrum_ram, (5<<14));
            }

            /* select ram at 0x0c000-0x0ffff */
            {
                int ram_page;
                UBytePtr ram_data;

                ram_page = spectrum_128_port_7ffd_data & 0x07;
                ram_data = new UBytePtr(spectrum_ram, (ram_page<<14));

                cpu_setbank(4, ram_data);
                cpu_setbank(8, ram_data);

                logerror("RAM at 0xc000: %02x\n",ram_page);
            }

            /* ROM switching */
            ROMSelection = ((spectrum_128_port_7ffd_data>>4) & 0x01);

            /* rom 0 is 128K rom, rom 1 is 48 BASIC */

            ChosenROM = new UBytePtr(memory_region(REGION_CPU1), 0x010000 + (ROMSelection<<14));

            cpu_setbank(1, ChosenROM);

            logerror("rom switch: %02x\n", ROMSelection);
        }

        public static WriteHandlerPtr spectrum_128_port_bffd_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                AY8910_write_port_0_w.handler(0, data);
            }
        };

        public static WriteHandlerPtr spectrum_128_port_fffd_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                AY8910_control_port_0_w.handler(0, data);
            }
        };

        public static ReadHandlerPtr spectrum_128_port_fffd_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return AY8910_read_port_0_r.handler(0);
            }
        };

        public static ReadHandlerPtr spectrum_128_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                 if ((offset & 1)==0)
                 {
                         return spectrum_port_fe_r.handler(offset);
                 }

                 if ((offset & 2)==0)
                 {
                        switch ((offset>>14) & 0x03)
                        {
                                default:
                                        break;

                                case 3:
                                        return spectrum_128_port_fffd_r.handler(offset);
                        }
                 }

                 /* don't think these are correct! */
                 if ((offset & 0xff)==0x1f)
                         return spectrum_port_1f_r.handler(offset);

                 if ((offset & 0xff)==0x7f)
                         return spectrum_port_7f_r.handler(offset);

                 if ((offset & 0xff)==0xdf)
                         return spectrum_port_df_r.handler(offset);

                 return cpu_getscanline()<193 ? spectrum_128_screen_location.read(0x1800|(cpu_getscanline()&0xf8)<<2):0xff;
            }
        };

        public static WriteHandlerPtr spectrum_128_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                        if ((offset & 1)==0)
                                        spectrum_port_fe_w.handler(offset,data);

                        /* Only decodes on A15, A14 & A1 */
                        else if ((offset & 2)==0)
                        {
                                        switch ((offset>>8) & 0xc0)
                                        {
                                                        case 0x40:
                                                                        spectrum_128_port_7ffd_w.handler(offset, data);
                                                                        break;
                                                        case 0x80:
                                                                        spectrum_128_port_bffd_w.handler(offset, data);
                                                                        break;
                                                        case 0xc0:
                                                                        spectrum_128_port_fffd_w.handler(offset, data);
                                                                        break;
                                                        default:
                                                                        logerror("Write %02x to 128 port: %04x\n", data, offset);
                                        }
                        }
                        else
                        {
                                logerror("Write %02x to 128 port: %04x\n", data, offset);
                        }
            }
        };

        /* ports are not decoded full.
        The function decodes the ports appropriately */
        public static IO_ReadPort spectrum_128_readport[] = {
            new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_ReadPort(0x0000, 0xffff, spectrum_128_port_r),
            new IO_ReadPort(MEMPORT_MARKER, 0) /* end of table */
        };

        /* ports are not decoded full.
        The function decodes the ports appropriately */
        public static IO_WritePort spectrum_128_writeport[] = {
            new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_WritePort(0x0000, 0xffff, spectrum_128_port_w),
            new IO_WritePort(MEMPORT_MARKER, 0) /* end of table */
        };

        static Memory_ReadAddress spectrum_128_readmem[] = {
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress(0x0000, 0x3fff, MRA_BANK1 ),
            new Memory_ReadAddress(0x4000, 0x7fff, MRA_BANK2 ),
            new Memory_ReadAddress(0x8000, 0xbfff, MRA_BANK3 ),
            new Memory_ReadAddress(0xc000, 0xffff, MRA_BANK4 ),
            new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */
        };

        static Memory_WriteAddress spectrum_128_writemem[] = {
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress(0x0000, 0x3fff, MWA_BANK5 ),
            new Memory_WriteAddress(0x4000, 0x7fff, MWA_BANK6 ),
            new Memory_WriteAddress(0x8000, 0xbfff, MWA_BANK7 ),
            new Memory_WriteAddress(0xc000, 0xffff, MWA_BANK8 ),
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */
        };

        public static InitMachinePtr spectrum_128_init_machine = new InitMachinePtr() {
            public void handler() {
            
                if (spectrum_alloc_ram(128)!=0)
                {
                        memory_set_bankhandler_r(1, 0, MRA_BANK1);
                        memory_set_bankhandler_r(2, 0, MRA_BANK2);
                        memory_set_bankhandler_r(3, 0, MRA_BANK3);
                        memory_set_bankhandler_r(4, 0, MRA_BANK4);

                        /* 0x0000-0x3fff always holds ROM */
                        memory_set_bankhandler_w(5, 0, MWA_ROM);
                        memory_set_bankhandler_w(6, 0, MWA_BANK6);
                        memory_set_bankhandler_w(7, 0, MWA_BANK7);
                        memory_set_bankhandler_w(8, 0, MWA_BANK8);


                        /* Bank 5 is always in 0x4000 - 0x7fff */
                        cpu_setbank(2, new UBytePtr(spectrum_ram, (5<<14)));
                        cpu_setbank(6, new UBytePtr(spectrum_ram, (5<<14)));

                        /* Bank 2 is always in 0x8000 - 0xbfff */
                        cpu_setbank(3, new UBytePtr(spectrum_ram, (2<<14)));
                        cpu_setbank(7, new UBytePtr(spectrum_ram, (2<<14)));

                        /* set initial ram config */
                        spectrum_128_port_7ffd_data = 0;
                        spectrum_128_update_memory();

                        spectrum_init_machine.handler();
                }
            }
        };

        public static StopMachinePtr spectrum_128_exit_machine = new StopMachinePtr() {
            public void handler() {            
                spectrum_free_ram();
            }
        };


        /****************************************************************************************************/
        /* Spectrum + 3 specific functions */
        /* This driver uses some of the spectrum_128 functions. The +3 is similar to a spectrum 128
        but with a disc drive */

        public static int spectrum_plus3_port_1ffd_data = -1;


        static nec765_interface spectrum_plus3_nec765_interface = new nec765_interface
        (
			null,
			null
	);

        static int spectrum_plus3_memory_selections[]=
        {
                        0,1,2,3,
                        4,5,6,7,
                        4,5,6,3,
                        4,7,6,3
        };

        public static WriteHandlerPtr spectrum_plus3_port_3ffd_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                        if ((~readinputport(16) & 0x20) != 0)
                                        nec765_data_w.handler(0,data);
            }
        };

        public static ReadHandlerPtr spectrum_plus3_port_3ffd_r = new ReadHandlerPtr() {
            public int handler(int offset) {            
                        if ((readinputport(16) & 0x20) != 0)
                                        return 0xff;
                        else
                                        return nec765_data_r.handler(0);
            }
        };

        public static ReadHandlerPtr spectrum_plus3_port_2ffd_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                        if ((readinputport(16) & 0x20) != 0)
                                        return 0xff;
                        else
                                        return nec765_status_r.handler(0);
            }
        };

        public static void spectrum_plus3_update_memory()
        {
                        if ((spectrum_128_port_7ffd_data & 8) != 0)
                        {
                                        logerror("+3 SCREEN 1: BLOCK 7\n");
                                        spectrum_128_screen_location = new UBytePtr(spectrum_ram, (7<<14));
                        }
                        else
                        {
                                        logerror("+3 SCREEN 0: BLOCK 5\n");
                                        spectrum_128_screen_location = new UBytePtr(spectrum_ram, (5<<14));
                        }

                        if ((spectrum_plus3_port_1ffd_data & 0x01)==0)
                        {
                                        int ram_page;
                                        UBytePtr ram_data;

                                        /* ROM switching */
                                        UBytePtr ChosenROM;
                                        int ROMSelection;

                                        /* select ram at 0x0c000-0x0ffff */
                                        ram_page = spectrum_128_port_7ffd_data & 0x07;
                                        ram_data = new UBytePtr(spectrum_ram, (ram_page<<14));

                                        cpu_setbank(4, ram_data);
                                        cpu_setbank(8, ram_data);

                                        logerror("RAM at 0xc000: %02x\n",ram_page);

                                        /* Reset memory between 0x4000 - 0xbfff in case extended paging was being used */
                                        /* Bank 5 in 0x4000 - 0x7fff */
                                        cpu_setbank(2, new UBytePtr(spectrum_ram, (5<<14)));
                                        cpu_setbank(6, new UBytePtr(spectrum_ram, (5<<14)));

                                        /* Bank 2 in 0x8000 - 0xbfff */
                                        cpu_setbank(3, new UBytePtr(spectrum_ram, (2<<14)));
                                        cpu_setbank(7, new UBytePtr(spectrum_ram, (2<<14)));


                                        ROMSelection = ((spectrum_128_port_7ffd_data>>4) & 0x01) |
                                                ((spectrum_plus3_port_1ffd_data>>1) & 0x02);

                                        /* rom 0 is editor, rom 1 is syntax, rom 2 is DOS, rom 3 is 48 BASIC */

                                        ChosenROM = new UBytePtr(memory_region(REGION_CPU1), 0x010000 + (ROMSelection<<14));

                                        cpu_setbank(1, ChosenROM);
                                        memory_set_bankhandler_w(5, 0, MWA_ROM);

                                        logerror("rom switch: %02x\n", ROMSelection);
                        }
                        else
                        {
                                        /* Extended memory paging */

                                        IntArray memory_selection;
                                        int MemorySelection;
                                        UBytePtr ram_data;

                                        MemorySelection = (spectrum_plus3_port_1ffd_data>>1) & 0x03;

                                        memory_selection = new IntArray(spectrum_plus3_memory_selections, (MemorySelection<<2));

                                        ram_data = new UBytePtr(spectrum_ram, (memory_selection.read(0)<<14));
                                        cpu_setbank(1, ram_data);
                                        cpu_setbank(5, ram_data);
                                        /* allow writes to 0x0000-0x03fff */
                                        memory_set_bankhandler_w(5, 0, MWA_BANK5);

                                        ram_data = new UBytePtr(spectrum_ram, (memory_selection.read(1)<<14));
                                        cpu_setbank(2, ram_data);
                                        cpu_setbank(6, ram_data);

                                        ram_data = new UBytePtr(spectrum_ram, (memory_selection.read(2)<<14));
                                        cpu_setbank(3, ram_data);
                                        cpu_setbank(7, ram_data);

                                        ram_data = new UBytePtr(spectrum_ram, (memory_selection.read(3)<<14));
                                        cpu_setbank(4, ram_data);
                                        cpu_setbank(8, ram_data);

                                        logerror("extended memory paging: %02x\n",MemorySelection);
                         }
        }

        public static WriteHandlerPtr spectrum_plus3_port_7ffd_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                   /* D0-D2: RAM page located at 0x0c000-0x0ffff */
                   /* D3 - Screen select (screen 0 in ram page 5, screen 1 in ram page 7 */
                   /* D4 - ROM select - which rom paged into 0x0000-0x03fff */
                   /* D5 - Disable paging */

                        /* disable paging? */
                        if ((spectrum_128_port_7ffd_data & 0x20) != 0)
                                        return;

                        /* store new state */
                        spectrum_128_port_7ffd_data = data;

                        /* update memory */
                        spectrum_plus3_update_memory();
            }
        };

        public static WriteHandlerPtr spectrum_plus3_port_1ffd_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                        /* D0-D1: ROM/RAM paging */
                        /* D2: Affects if d0-d1 work on ram/rom */
                        /* D3 - Disk motor on/off */
                        /* D4 - parallel port strobe */

                        floppy_drive_set_motor_state(0, data & (1<<3));
                        floppy_drive_set_motor_state(1, data & (1<<3));
                        floppy_drive_set_ready_state(0, 1, 1);
                        floppy_drive_set_ready_state(1, 1, 1);

                        spectrum_plus3_port_1ffd_data = data;

                        /* disable paging? */
                        if ((spectrum_128_port_7ffd_data & 0x20)==0)
                        {
                                        /* no */
                                        spectrum_plus3_update_memory();
                        }
            }
        };

        /* decoding as per spectrum FAQ on www.worldofspectrum.org */
        public static ReadHandlerPtr spectrum_plus3_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
            
                 if ((offset & 1)==0)
                 {
                         return spectrum_port_fe_r.handler(offset);
                 }

                 if ((offset & 2)==0)
                 {
                         switch ((offset>>14) & 0x03)
                         {
                                /* +3 fdc,memory,centronics */
                                case 0:
                                {
                                        switch ((offset>>12) & 0x03)
                                        {
                                                /* +3 centronics */
                                                case 0:
                                                        break;

                                                /* +3 fdc status */
                                                case 2:
                                                        return spectrum_plus3_port_2ffd_r.handler(offset);
                                                /* +3 fdc data */
                                                case 3:
                                                        return spectrum_plus3_port_3ffd_r.handler(offset);

                                                default:
                                                        break;
                                        }
                                }
                                break;

                                /* 128k AY data */
                                case 3:
                                        return spectrum_128_port_fffd_r.handler(offset);

                                default:
                                        break;
                         }
                 }

                 return cpu_getscanline()<193 ? spectrum_128_screen_location.read(0x1800|(cpu_getscanline()&0xf8)<<2):0xff;
            }
        };

        public static WriteHandlerPtr spectrum_plus3_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                        if ((offset & 1)==0)
                                        spectrum_port_fe_w.handler(offset,data);

                        /* the following is not decoded exactly, need to check
                        what is correct! */

                        if ((offset & 2)==0)
                        {
                                switch ((offset>>14) & 0x03)
                                {
                                        /* +3 fdc,memory,centronics */
                                        case 0:
                                        {
                                                switch ((offset>>12) & 0x03)
                                                {
                                                        /* +3 centronics */
                                                        case 0:
                                                        {


                                                        }
                                                        break;

                                                        /* +3 memory */
                                                        case 1:
                                                                spectrum_plus3_port_1ffd_w.handler(offset, data);
                                                                break;

                                                        /* +3 fdc data */
                                                        case 3:
                                                                spectrum_plus3_port_3ffd_w.handler(offset,data);
                                                                break;

                                                        default:
                                                                break;
                                                }
                                        }
                                        break;

                                        /* 128k memory */
                                        case 1:
                                                spectrum_plus3_port_7ffd_w.handler(offset, data);
                                                break;

                                        /* 128k AY data */
                                        case 2:
                                                spectrum_128_port_bffd_w.handler(offset, data);
                                                break;

                                        /* 128K AY register */
                                        case 3:
                                                spectrum_128_port_fffd_w.handler(offset, data);

                                        default:
                                                break;
                                }
                        }

            /*logerror("Write %02x to +3 port: %04x\n", data, offset); */
            }
        };

        public static IO_ReadPort spectrum_plus3_readport[] = {
            new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_ReadPort(0x0000, 0xffff, spectrum_plus3_port_r),
            new IO_ReadPort(MEMPORT_MARKER, 0) /* end of table */
        };
	
	/* ports are not decoded full.
	The function decodes the ports appropriately */
	static IO_WritePort spectrum_plus3_writeport[] = {
            new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_WritePort(0x0000, 0xffff, spectrum_plus3_port_w),
            new IO_WritePort(MEMPORT_MARKER, 0) /* end of table */
        };

        public static InitMachinePtr spectrum_plus3_init_machine = new InitMachinePtr() {
            public void handler() {
            
                if (spectrum_alloc_ram(128) != 0)
                {

                        memory_set_bankhandler_r(1, 0, MRA_BANK1);
                        memory_set_bankhandler_r(2, 0, MRA_BANK2);
                        memory_set_bankhandler_r(3, 0, MRA_BANK3);
                        memory_set_bankhandler_r(4, 0, MRA_BANK4);

                        memory_set_bankhandler_w(5, 0, MWA_BANK5);
                        memory_set_bankhandler_w(6, 0, MWA_BANK6);
                        memory_set_bankhandler_w(7, 0, MWA_BANK7);
                        memory_set_bankhandler_w(8, 0, MWA_BANK8);

                        nec765_init(spectrum_plus3_nec765_interface, NEC765A);

                        floppy_drive_set_geometry(0, FLOPPY_DRIVE_SS_40);
                        floppy_drive_set_geometry(1, FLOPPY_DRIVE_SS_40);

                        /* Initial configuration */
                        spectrum_128_port_7ffd_data = 0;
                        spectrum_plus3_port_1ffd_data = 0;
                        spectrum_plus3_update_memory();

                        spectrum_init_machine.handler();
                }
            }
        };

        public static StopMachinePtr spectrum_plus3_exit_machine = new StopMachinePtr() {
            public void handler() {            
                nec765_stop();
                spectrum_free_ram();
            }
        };


        /****************************************************************************************************/
        /* TS2048 specific functions */
        public static int ts2068_port_ff_data = -1; /* Display enhancement control */
        public static int ts2068_port_f4_data = -1; /* Horizontal Select Register */
        public static UBytePtr ts2068_ram = null;

        public static ReadHandlerPtr ts2068_port_f4_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return ts2068_port_f4_data;
            }
        };

        public static WriteHandlerPtr ts2068_port_f4_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                ts2068_port_f4_data = data;
                ts2068_update_memory();
            }
        };

        public static WriteHandlerPtr ts2068_port_f5_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                AY8910_control_port_0_w.handler(0, data);
            }
        };

        public static ReadHandlerPtr ts2068_port_f6_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                
                        /* TODO - Reading from register 14 reads the joystick ports
                           set bit 8 of address to read joystick #1
                           set bit 9 of address to read joystick #2
                           if both bits are set then OR values
                           Bit 0 up, 1 down, 2 left, 3 right, 7 fire active low. Other bits 1
                        */
                        return AY8910_read_port_0_r.handler(0);
            }
        };

        public static WriteHandlerPtr ts2068_port_f6_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                AY8910_write_port_0_w.handler(0, data);
            }
        };

        public static ReadHandlerPtr ts2068_port_ff_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return ts2068_port_ff_data;
            }
        };

        public static WriteHandlerPtr ts2068_port_ff_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                
                        /* Bits 0-2 Video Mode Select
                           Bits 3-5 64 column mode ink/paper selection
                                                (See ts2068_vh_screenrefresh for more info)
                           Bit	6	17ms Interrupt Inhibit
                           Bit	7	Cartridge (0) / EXROM (1) select
                        */
                        ts2068_port_ff_data = data;
                        ts2068_update_memory();
                        logerror("Port %04x write %02x\n", offset, data);
            }
        };


        public static ReadHandlerPtr ts2068_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                
                        switch (offset & 0xff)
                        {
                                        /* Note: keys only decoded on port #fe not all even ports so
                                           ports #f4 & #f6 correctly read */
                                        case 0xf4: return ts2068_port_f4_r.handler(offset);
                                        case 0xf6: return ts2068_port_f6_r.handler(offset);
                                        case 0xff: return ts2068_port_ff_r.handler(offset);

                                        case 0xfe: return spectrum_port_fe_r.handler(offset);
                                        case 0x1f: return spectrum_port_1f_r.handler(offset);
                                        case 0x7f: return spectrum_port_7f_r.handler(offset);
                                        case 0xdf: return spectrum_port_df_r.handler(offset);
                        }
                        logerror("Read from port: %04x\n", offset);

                        return 0xff;
            }
        };

        public static WriteHandlerPtr ts2068_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
                        /* Ports #fd & #fc were reserved by Timex for bankswitching and are not used
                           by either the hardware or system software.
                           Port #fb is the Thermal printer port and works exactly as the Sinclair
                           Printer - ie not yet emulated.
                        */
                        switch (offset & 0xff)
                        {
                                        case 0xfe: spectrum_port_fe_w.handler(offset,data); break;
                                        case 0xf4: ts2068_port_f4_w.handler(offset,data); break;
                                        case 0xf5: ts2068_port_f5_w.handler(offset,data); break;
                                        case 0xf6: ts2068_port_f6_w.handler(offset,data); break;
                                        case 0xff: ts2068_port_ff_w.handler(offset,data); break;
                                        default:
                                                        logerror("Write %02x to Port: %04x\n", data, offset);
                        }
            }
        };

        /*******************************************************************
         *
         *		Bank switch between the 3 internal memory banks HOME, EXROM
         *		and DOCK (Cartridges). The HOME bank contains 16K ROM in the
         *		0-16K area and 48K RAM fills the rest. The EXROM contains 8K
         *		ROM and can appear in every 8K segment (ie 0-8K, 8-16K etc).
         *		The DOCK is empty and is meant to be occupied by cartridges
         *		you can plug into the cartridge dock of the 2068.
         *
         *		The address space is divided into 8 8K chunks. Bit 0 of port
         *		#f4 corresponds to the 0-8K chunk, bit 1 to the 8-16K chunk
         *		etc. If the bit is 0 then the chunk is controlled by the HOME
         *		bank. If the bit is 1 then the chunk is controlled by either
         *		the DOCK or EXROM depending on bit 7 of port #ff. Note this
         *		means that that the Z80 can't see chunks of the EXROM and DOCK
         *		at the same time.
         *
         *******************************************************************/
        public static void ts2068_update_memory()
        {
                        UBytePtr ChosenROM, ExROM, DOCK;

                        DOCK = new UBytePtr(timex_cart_data);

                        ExROM = new UBytePtr(memory_region(REGION_CPU1), 0x014000);

                        if ((ts2068_port_f4_data & 0x01) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(1, ExROM);
                                                        memory_set_bankhandler_r(1, 0, MRA_BANK1);
                                                        memory_set_bankhandler_w(9, 0, MWA_ROM);
                                                        logerror("0000-1fff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(1, DOCK);
                                                                memory_set_bankhandler_r(1, 0, MRA_BANK1);
                                                                if ((timex_cart_chunks&0x01) != 0)
                                                                        memory_set_bankhandler_w(9, 0, MWA_BANK9);
                                                                else
                                                                        memory_set_bankhandler_w(9, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(1, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(9, 0, MWA_ROM);
                                                        }
                                                        logerror("0000-1fff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        ChosenROM = new UBytePtr(memory_region(REGION_CPU1), 0x010000);
                                        cpu_setbank(1, ChosenROM);
                                        memory_set_bankhandler_r(1, 0, MRA_BANK1);
                                        memory_set_bankhandler_w(9, 0, MWA_ROM);
                                        logerror("0000-1fff HOME\n");
                        }

                        if ((ts2068_port_f4_data & 0x02) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(2, ExROM);
                                                        memory_set_bankhandler_r(2, 0, MRA_BANK2);
                                                        memory_set_bankhandler_w(10, 0, MWA_ROM);
                                                        logerror("2000-3fff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(2, new UBytePtr(DOCK, 0x2000));
                                                                memory_set_bankhandler_r(2, 0, MRA_BANK2);
                                                                if ((timex_cart_chunks&0x02) != 0)
                                                                        memory_set_bankhandler_w(10, 0, MWA_BANK10);
                                                                else
                                                                        memory_set_bankhandler_w(10, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(2, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(10, 0, MWA_ROM);
                                                        }
                                                        logerror("2000-3fff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        ChosenROM = new UBytePtr(memory_region(REGION_CPU1), 0x012000);
                                        cpu_setbank(2, ChosenROM);
                                        memory_set_bankhandler_r(2, 0, MRA_BANK2);
                                        memory_set_bankhandler_w(10, 0, MWA_ROM);
                                        logerror("2000-3fff HOME\n");
                        }

                        if ((ts2068_port_f4_data & 0x04) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(3, ExROM);
                                                        memory_set_bankhandler_r(3, 0, MRA_BANK3);
                                                        memory_set_bankhandler_w(11, 0, MWA_ROM);
                                                        logerror("4000-5fff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(3, new UBytePtr(DOCK, 0x4000));
                                                                memory_set_bankhandler_r(3, 0, MRA_BANK3);
                                                                if ((timex_cart_chunks&0x04) != 0)
                                                                        memory_set_bankhandler_w(11, 0, MWA_BANK11);
                                                                else
                                                                        memory_set_bankhandler_w(11, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(3, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(11, 0, MWA_ROM);
                                                        }
                                                        logerror("4000-5fff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        cpu_setbank(3, ts2068_ram);
                                        cpu_setbank(11, ts2068_ram);
                                        memory_set_bankhandler_r(3, 0, MRA_BANK3);
                                        memory_set_bankhandler_w(11, 0, MWA_BANK11);
                                        logerror("4000-5fff RAM\n");
                        }

                        if ((ts2068_port_f4_data & 0x08) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(4, ExROM);
                                                        memory_set_bankhandler_r(4, 0, MRA_BANK4);
                                                        memory_set_bankhandler_w(12, 0, MWA_ROM);
                                                        logerror("6000-7fff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(4, new UBytePtr(DOCK, 0x6000));
                                                                memory_set_bankhandler_r(4, 0, MRA_BANK4);
                                                                if ((timex_cart_chunks&0x08) != 0)
                                                                        memory_set_bankhandler_w(12, 0, MWA_BANK12);
                                                                else
                                                                        memory_set_bankhandler_w(12, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(4, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(12, 0, MWA_ROM);
                                                        }
                                                        logerror("6000-7fff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        cpu_setbank(4, new UBytePtr(ts2068_ram, 0x2000));
                                        cpu_setbank(12, new UBytePtr(ts2068_ram, 0x2000));
                                        memory_set_bankhandler_r(4, 0, MRA_BANK4);
                                        memory_set_bankhandler_w(12, 0, MWA_BANK12);
                                        logerror("6000-7fff RAM\n");
                        }

                        if ((ts2068_port_f4_data & 0x10) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(5, ExROM);
                                                        memory_set_bankhandler_r(5, 0, MRA_BANK5);
                                                        memory_set_bankhandler_w(13, 0, MWA_ROM);
                                                        logerror("8000-9fff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(5, new UBytePtr(DOCK, 0x8000));
                                                                memory_set_bankhandler_r(5, 0, MRA_BANK5);
                                                                if ((timex_cart_chunks&0x10) != 0)
                                                                        memory_set_bankhandler_w(13, 0, MWA_BANK13);
                                                                else
                                                                        memory_set_bankhandler_w(13, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(5, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(13, 0, MWA_ROM);
                                                        }
                                                        logerror("8000-9fff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        cpu_setbank(5, new UBytePtr(ts2068_ram, 0x4000));
                                        cpu_setbank(13, new UBytePtr(ts2068_ram, 0x4000));
                                        memory_set_bankhandler_r(5, 0, MRA_BANK5);
                                        memory_set_bankhandler_w(13, 0, MWA_BANK13);
                                        logerror("8000-9fff RAM\n");
                        }

                        if ((ts2068_port_f4_data & 0x20) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(6, ExROM);
                                                        memory_set_bankhandler_r(6, 0, MRA_BANK6);
                                                        memory_set_bankhandler_w(14, 0, MWA_ROM);
                                                        logerror("a000-bfff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(6, new UBytePtr(DOCK, 0xa000));
                                                                memory_set_bankhandler_r(6, 0, MRA_BANK6);
                                                                if ((timex_cart_chunks&0x20) != 0)
                                                                        memory_set_bankhandler_w(14, 0, MWA_BANK14);
                                                                else
                                                                        memory_set_bankhandler_w(14, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(6, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(14, 0, MWA_ROM);
                                                        }
                                                        logerror("a000-bfff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        cpu_setbank(6, new UBytePtr(ts2068_ram, 0x6000));
                                        cpu_setbank(14, new UBytePtr(ts2068_ram, 0x6000));
                                        memory_set_bankhandler_r(6, 0, MRA_BANK6);
                                        memory_set_bankhandler_w(14, 0, MWA_BANK14);
                                        logerror("a000-bfff RAM\n");
                        }

                        if ((ts2068_port_f4_data & 0x40) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(7, ExROM);
                                                        memory_set_bankhandler_r(7, 0, MRA_BANK7);
                                                        memory_set_bankhandler_w(15, 0, MWA_ROM);
                                                        logerror("c000-dfff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(7, new UBytePtr(DOCK, 0xc000));
                                                                memory_set_bankhandler_r(7, 0, MRA_BANK7);
                                                                if ((timex_cart_chunks&0x40) != 0)
                                                                        memory_set_bankhandler_w(15, 0, MWA_BANK15);
                                                                else
                                                                        memory_set_bankhandler_w(15, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(7, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(15, 0, MWA_ROM);
                                                        }
                                                        logerror("c000-dfff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        cpu_setbank(7, new UBytePtr(ts2068_ram, 0x8000));
                                        cpu_setbank(15, new UBytePtr(ts2068_ram, 0x8000));
                                        memory_set_bankhandler_r(7, 0, MRA_BANK7);
                                        memory_set_bankhandler_w(15, 0, MWA_BANK15);
                                        logerror("c000-dfff RAM\n");
                        }

                        if ((ts2068_port_f4_data & 0x80) != 0)
                        {
                                        if ((ts2068_port_ff_data & 0x80) != 0)
                                        {
                                                        cpu_setbank(8, ExROM);
                                                        memory_set_bankhandler_r(8, 0, MRA_BANK8);
                                                        memory_set_bankhandler_w(16, 0, MWA_ROM);
                                                        logerror("e000-ffff EXROM\n");
                                        }
                                        else
                                        {
                                                        if (timex_cart_type == TIMEX_CART_DOCK)
                                                        {
                                                                cpu_setbank(8, new UBytePtr(DOCK, 0xe000));
                                                                memory_set_bankhandler_r(8, 0, MRA_BANK8);
                                                                if ((timex_cart_chunks&0x80) != 0)
                                                                        memory_set_bankhandler_w(16, 0, MWA_BANK16);
                                                                else
                                                                        memory_set_bankhandler_w(16, 0, MWA_ROM);
                                                        }
                                                        else
                                                        {
                                                                memory_set_bankhandler_r(8, 0, MRA_NOP);
                                                                memory_set_bankhandler_w(16, 0, MWA_ROM);
                                                        }
                                                        logerror("e000-ffff Cartridge\n");
                                        }
                        }
                        else
                        {
                                        cpu_setbank(8, new UBytePtr(ts2068_ram, 0xa000));
                                        cpu_setbank(16, new UBytePtr(ts2068_ram, 0xa000));
                                        memory_set_bankhandler_r(8, 0, MRA_BANK8);
                                        memory_set_bankhandler_w(16, 0, MWA_BANK16);
                                        logerror("e000-ffff RAM\n");
                        }
        }


        public static IO_ReadPort ts2068_readport[] = {
            new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_ReadPort(0x0000, 0x0ffff, ts2068_port_r),
            new IO_ReadPort(MEMPORT_MARKER, 0) /* end of table */
        };
	
	static IO_WritePort ts2068_writeport[] = {
            new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_WritePort(0x0000, 0x0ffff, ts2068_port_w),
            new IO_WritePort(MEMPORT_MARKER, 0) /* end of table */
        };
        
        static Memory_ReadAddress ts2068_readmem[] = {
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress(0x0000, 0x1fff, MRA_BANK1 ),
            new Memory_ReadAddress(0x2000, 0x3fff, MRA_BANK2 ),
            new Memory_ReadAddress(0x4000, 0x5fff, MRA_BANK3 ),
            new Memory_ReadAddress(0x6000, 0x7fff, MRA_BANK4 ),
            new Memory_ReadAddress(0x8000, 0x9fff, MRA_BANK5 ),
            new Memory_ReadAddress(0xa000, 0xbfff, MRA_BANK6 ),
            new Memory_ReadAddress(0xc000, 0xdfff, MRA_BANK7 ),
            new Memory_ReadAddress(0xe000, 0xffff, MRA_BANK8 ),
            new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */
        };

        static Memory_WriteAddress ts2068_writemem[] = {
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress(0x0000, 0x1fff, MWA_BANK9 ),
            new Memory_WriteAddress(0x2000, 0x3fff, MWA_BANK10 ),
            new Memory_WriteAddress(0x4000, 0x5fff, MWA_BANK11 ),
            new Memory_WriteAddress(0x6000, 0x7fff, MWA_BANK12 ),
            new Memory_WriteAddress(0x8000, 0x9fff, MWA_BANK13 ),
            new Memory_WriteAddress(0xa000, 0xbfff, MWA_BANK14 ),
            new Memory_WriteAddress(0xc000, 0xdfff, MWA_BANK15 ),
            new Memory_WriteAddress(0xe000, 0xffff, MWA_BANK16 ),
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */
        };

        public static InitMachinePtr ts2068_init_machine = new InitMachinePtr() {
            public void handler() {
            
		ts2068_ram = new UBytePtr(48*1024);
		if(ts2068_ram==null) return;
		memset(ts2068_ram, 0, 48*1024);

		memory_set_bankhandler_r(1, 0, MRA_BANK1);
		memory_set_bankhandler_r(2, 0, MRA_BANK2);
		memory_set_bankhandler_r(3, 0, MRA_BANK3);
		memory_set_bankhandler_r(4, 0, MRA_BANK4);
		memory_set_bankhandler_r(5, 0, MRA_BANK5);
		memory_set_bankhandler_r(6, 0, MRA_BANK6);
		memory_set_bankhandler_r(7, 0, MRA_BANK7);
		memory_set_bankhandler_r(8, 0, MRA_BANK8);

		/* 0x0000-0x3fff always holds ROM */
		memory_set_bankhandler_w(9, 0, MWA_BANK9);
		memory_set_bankhandler_w(10, 0, MWA_BANK10);
		memory_set_bankhandler_w(11, 0, MWA_BANK11);
		memory_set_bankhandler_w(12, 0, MWA_BANK12);
		memory_set_bankhandler_w(13, 0, MWA_BANK13);
		memory_set_bankhandler_w(14, 0, MWA_BANK14);
		memory_set_bankhandler_w(15, 0, MWA_BANK15);
		memory_set_bankhandler_w(16, 0, MWA_BANK16);

		ts2068_port_ff_data = 0;
		ts2068_port_f4_data = 0;
		ts2068_update_memory();

		spectrum_init_machine.handler();
            }
        };

        public static StopMachinePtr ts2068_exit_machine = new StopMachinePtr() {
            public void handler() {            
		if (ts2068_ram!=null)
                    ts2068_ram = null;
            }
        };


/*TODO*////****************************************************************************************************/
/*TODO*////* TC2048 specific functions */
/*TODO*///
/*TODO*///
/*TODO*///static void tc2048_port_ff_w(int offset, int data)
/*TODO*///{
/*TODO*///		ts2068_port_ff_data = data;
/*TODO*///		logerror("Port %04x write %02x\n", offset, data);
/*TODO*///}
/*TODO*///
/*TODO*///READ_HANDLER ( tc2048_port_r )
/*TODO*///{
/*TODO*///		if ((offset & 1)==0)
/*TODO*///				return spectrum_port_fe_r(offset);
/*TODO*///		switch (offset & 0xff)
/*TODO*///		{
/*TODO*///				case 0xff: return ts2068_port_ff_r(offset);
/*TODO*///				case 0x1f: return spectrum_port_1f_r(offset);
/*TODO*///				case 0x7f: return spectrum_port_7f_r(offset);
/*TODO*///				case 0xdf: return spectrum_port_df_r(offset);
/*TODO*///		}
/*TODO*///
/*TODO*///		logerror("Read from port: %04x\n", offset);
/*TODO*///		return 0xff;
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER ( tc2048_port_w )
/*TODO*///{
/*TODO*///		if ((offset & 1)==0)
/*TODO*///				spectrum_port_fe_w(offset,data);
/*TODO*///		else if ((offset & 0xff)==0xff)
/*TODO*///				tc2048_port_ff_w(offset,data);
/*TODO*///		else
/*TODO*///		{
/*TODO*///				logerror("Write %02x to Port: %04x\n", data, offset);
/*TODO*///		}
/*TODO*///}
/*TODO*///
/*TODO*////* ports are not decoded full.
/*TODO*///The function decodes the ports appropriately */
/*TODO*///static PORT_READ_START (tc2048_readport)
/*TODO*///	{0x0000, 0x0ffff, tc2048_port_r},
/*TODO*///PORT_END
/*TODO*///
/*TODO*////* ports are not decoded full.
/*TODO*///The function decodes the ports appropriately */
/*TODO*///static PORT_WRITE_START (tc2048_writeport)
/*TODO*///	{0x0000, 0x0ffff, tc2048_port_w},
/*TODO*///PORT_END
/*TODO*///
/*TODO*///
/*TODO*///static MEMORY_READ_START (tc2048_readmem)
/*TODO*///	{ 0x0000, 0x3fff, MRA_ROM },
/*TODO*///	{ 0x4000, 0xffff, MRA_BANK1 },
/*TODO*///MEMORY_END
/*TODO*///
/*TODO*///static MEMORY_WRITE_START (tc2048_writemem)
/*TODO*///	{ 0x0000, 0x3fff, MWA_ROM },
/*TODO*///	{ 0x4000, 0xffff, MWA_BANK2 },
/*TODO*///MEMORY_END
/*TODO*///
/*TODO*///
/*TODO*///void tc2048_init_machine(void)
/*TODO*///{
/*TODO*///		ts2068_ram = (unsigned char *)malloc(48*1024);
/*TODO*///		if(!ts2068_ram) return;
/*TODO*///		memset(ts2068_ram, 0, 48*1024);
/*TODO*///
/*TODO*///		memory_set_bankhandler_r(1, 0, MRA_BANK1);
/*TODO*///		memory_set_bankhandler_w(2, 0, MWA_BANK2);
/*TODO*///		cpu_setbank(1, ts2068_ram);
/*TODO*///		cpu_setbank(2, ts2068_ram);
/*TODO*///		ts2068_port_ff_data = 0;
/*TODO*///
/*TODO*///		spectrum_init_machine();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************************************/
/*TODO*////* BETADISK/TR-DOS disc controller emulation */
/*TODO*////* microcontroller KR1818VG93 is a russian wd179x clone */
/*TODO*///#include "includes/wd179x.h"
/*TODO*///
/*TODO*////*
/*TODO*///DRQ (D6) and INTRQ (D7).
/*TODO*///DRQ - signal showing request of data by microcontroller
/*TODO*///INTRQ - signal of completion of execution of command.
/*TODO*///*/
/*TODO*///
/*TODO*///static int betadisk_status;
/*TODO*///static int betadisk_active;
/*TODO*///static void (*betadisk_memory_update)(void);
/*TODO*///
/*TODO*///static OPBASE_HANDLER(betadisk_opbase_handler)
/*TODO*///{
/*TODO*///
/*TODO*///	int pc;
/*TODO*///
/*TODO*///	pc = cpu_get_pc();
/*TODO*///
/*TODO*///	if ((pc & 0xc000)!=0x0000)
/*TODO*///	{
/*TODO*///		/* outside rom area */
/*TODO*///		betadisk_active = 0;
/*TODO*///
/*TODO*///		betadisk_memory_update();
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		/* inside rom area, switch on betadisk */
/*TODO*///	//	betadisk_active = 1;
/*TODO*///
/*TODO*///	//	betadisk_memory_update();
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	return pc & 0x0ffff;
/*TODO*///}
/*TODO*///
/*TODO*///static void betadisk_wd179x_callback(int state)
/*TODO*///{
/*TODO*///	switch (state)
/*TODO*///	{
/*TODO*///		case WD179X_DRQ_SET:
/*TODO*///		{
/*TODO*///			betadisk_status |= (1<<6);
/*TODO*///		}
/*TODO*///		break;
/*TODO*///
/*TODO*///		case WD179X_DRQ_CLR:
/*TODO*///		{
/*TODO*///			betadisk_status &=~(1<<6);
/*TODO*///		}
/*TODO*///		break;
/*TODO*///
/*TODO*///		case WD179X_IRQ_SET:
/*TODO*///		{
/*TODO*///			betadisk_status |= (1<<7);
/*TODO*///		}
/*TODO*///		break;
/*TODO*///
/*TODO*///		case WD179X_IRQ_CLR:
/*TODO*///		{
/*TODO*///			betadisk_status &=~(1<<7);
/*TODO*///		}
/*TODO*///		break;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////* these are active only when betadisk is enabled */
/*TODO*///WRITE_HANDLER(betadisk_w)
/*TODO*///{
/*TODO*///
/*TODO*///	if (betadisk_active)
/*TODO*///	{
/*TODO*///
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////* these are active only when betadisk is enabled */
/*TODO*///READ_HANDLER(betadisk_r)
/*TODO*///{
/*TODO*///	if (betadisk_active)
/*TODO*///	{
/*TODO*///		/* decoding of these ports might be wrong - to be checked! */
/*TODO*///		if ((offset & 0x01f)==0x01f)
/*TODO*///		{
/*TODO*///			switch (offset & 0x0ff)
/*TODO*///			{
/*TODO*///
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///	}
/*TODO*///
/*TODO*///	return 0x0ff;
/*TODO*///}
/*TODO*///
/*TODO*///void	 betadisk_init(void)
/*TODO*///{
/*TODO*///	betadisk_active = 0;
/*TODO*///	betadisk_status = 0x03f;
/*TODO*///	wd179x_init(WD_TYPE_179X,&betadisk_wd179x_callback);
/*TODO*///}
/*TODO*///
/*TODO*///void	betadisk_exit(void)
/*TODO*///{
/*TODO*///	wd179x_exit();
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************************************/
/*TODO*////* Zs Scorpion 256 */
/*TODO*///
/*TODO*////*
/*TODO*///port 7ffd. full compatibility with Zx spectrum 128. digits are:
/*TODO*///
/*TODO*///D0-D2 - number of RAM page to put in C000-FFFF
/*TODO*///D3    - switch of address for RAM of screen. 0 - 4000, 1 - c000
/*TODO*///D4    - switch of ROM : 0-zx128, 1-zx48
/*TODO*///D5    - 1 in this bit will block further output in port 7FFD, until reset.
/*TODO*///*/
/*TODO*///
/*TODO*////*
/*TODO*///port 1ffd - additional port for resources of computer.
/*TODO*///
/*TODO*///D0    - block of ROM in 0-3fff. when set to 1 - allows read/write page 0 of RAM
/*TODO*///D1    - selects ROM expansion. this rom contains main part of service monitor.
/*TODO*///D2    - not used
/*TODO*///D3    - used for output in RS-232C
/*TODO*///D4    - extended RAM. set to 1 - connects RAM page with number 8-15 in
/*TODO*///	C000-FFFF. number of page is given in gidits D0-D2 of port 7FFD
/*TODO*///D5    - signal of strobe for interface centronics. to form the strobe has to be
/*TODO*///	set to 1.
/*TODO*///D6-D7 - not used. ( yet ? )
/*TODO*///*/
/*TODO*///
/*TODO*////* rom 0=zx128, 1=zx48, 2 = service monitor, 3=tr-dos */
/*TODO*///
/*TODO*///static int scorpion_256_port_1ffd_data = 0;
/*TODO*///
/*TODO*///extern void scorpion_update_memory(void)
/*TODO*///{
/*TODO*///		unsigned char *ChosenROM;
/*TODO*///		int ROMSelection;
/*TODO*///
/*TODO*///		if (spectrum_128_port_7ffd_data & 8)
/*TODO*///		{
/*TODO*///				logerror("SCREEN 1: BLOCK 7\n");
/*TODO*///				spectrum_128_screen_location = spectrum_ram + (7<<14);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///				logerror("SCREEN 0: BLOCK 5\n");
/*TODO*///				spectrum_128_screen_location = spectrum_ram + (5<<14);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* select ram at 0x0c000-0x0ffff */
/*TODO*///		{
/*TODO*///				int ram_page;
/*TODO*///				unsigned char *ram_data;
/*TODO*///
/*TODO*///				ram_page = (spectrum_128_port_7ffd_data & 0x07) | ((scorpion_256_port_1ffd_data & (1<<4))>>1);
/*TODO*///				ram_data = spectrum_ram + (ram_page<<14);
/*TODO*///
/*TODO*///				cpu_setbank(4, ram_data);
/*TODO*///				cpu_setbank(8, ram_data);
/*TODO*///
/*TODO*///				logerror("RAM at 0xc000: %02x\n",ram_page);
/*TODO*///		}
/*TODO*///
/*TODO*///		if (scorpion_256_port_1ffd_data & (1<<0))
/*TODO*///		{
/*TODO*///			/* ram at 0x0000 */
/*TODO*///			logerror("RAM at 0x0000\n");
/*TODO*///
/*TODO*///			/* connect page 0 of ram to 0x0000 */
/*TODO*///			memory_set_bankhandler_r(1, 0, MRA_BANK1);
/*TODO*///			memory_set_bankhandler_w(5, 0, MWA_BANK5);
/*TODO*///			cpu_setbank(1, spectrum_ram+(8<<14));
/*TODO*///			cpu_setbank(5, spectrum_ram+(8<<14));
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* rom at 0x0000 */
/*TODO*///			logerror("ROM at 0x0000\n");
/*TODO*///
/*TODO*///			/* connect page 0 of rom to 0x0000 */
/*TODO*///			memory_set_bankhandler_r(1, 0, MRA_BANK1);
/*TODO*///			memory_set_bankhandler_w(5, 0, MWA_NOP);
/*TODO*///
/*TODO*///			if (scorpion_256_port_1ffd_data & (1<<1))
/*TODO*///			{
/*TODO*///				ROMSelection = 2;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///
/*TODO*///				/* ROM switching */
/*TODO*///				ROMSelection = ((spectrum_128_port_7ffd_data>>4) & 0x01);
/*TODO*///			}
/*TODO*///
/*TODO*///			/* rom 0 is 128K rom, rom 1 is 48 BASIC */
/*TODO*///			ChosenROM = memory_region(REGION_CPU1) + 0x010000 + (ROMSelection<<14);
/*TODO*///
/*TODO*///			cpu_setbank(1, ChosenROM);
/*TODO*///
/*TODO*///			logerror("rom switch: %02x\n", ROMSelection);
/*TODO*///		}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static WRITE_HANDLER(scorpion_port_7ffd_w)
/*TODO*///{
/*TODO*///	logerror("scorpion 7ffd w: %02x\n", data);
/*TODO*///
/*TODO*///	/* disable paging? */
/*TODO*///	if (spectrum_128_port_7ffd_data & 0x20)
/*TODO*///		return;
/*TODO*///
/*TODO*///	/* store new state */
/*TODO*///	spectrum_128_port_7ffd_data = data;
/*TODO*///
/*TODO*///	/* update memory */
/*TODO*///	scorpion_update_memory();
/*TODO*///}
/*TODO*///
/*TODO*///static WRITE_HANDLER(scorpion_port_1ffd_w)
/*TODO*///{
/*TODO*///	logerror("scorpion 1ffd w: %02x\n", data);
/*TODO*///
/*TODO*///	scorpion_256_port_1ffd_data = data;
/*TODO*///
/*TODO*///	/* disable paging? */
/*TODO*///	if ((spectrum_128_port_7ffd_data & 0x20)==0)
/*TODO*///	{
/*TODO*///		scorpion_update_memory();
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////* not sure if decoding is full or partial on scorpion */
/*TODO*////* TO BE CHECKED! */
/*TODO*///static READ_HANDLER(scorpion_port_r)
/*TODO*///{
/*TODO*///	 if ((offset & 1)==0)
/*TODO*///	 {
/*TODO*///		 return spectrum_port_fe_r(offset);
/*TODO*///	 }
/*TODO*///
/*TODO*///	 /* KT: the following is not decoded exactly, need to check what
/*TODO*///	 is correct */
/*TODO*///	 if ((offset & 2)==0)
/*TODO*///	 {
/*TODO*///		 switch ((offset>>8) & 0xff)
/*TODO*///		 {
/*TODO*///				case 0xff: return spectrum_128_port_fffd_r(offset);
/*TODO*///				case 0x1f: return spectrum_port_1f_r(offset);
/*TODO*///				case 0x7f: return spectrum_port_7f_r(offset);
/*TODO*///				case 0xdf: return spectrum_port_df_r(offset);
/*TODO*///		 }
/*TODO*///	 }
/*TODO*///#if 0
/*TODO*///	 switch (offset & 0x0ff)
/*TODO*///	 {
/*TODO*///		case 0x01f:
/*TODO*///			return wd179x_status_r(offset);
/*TODO*///		case 0x03f:
/*TODO*///			return wd179x_track_r(offset);
/*TODO*///		case 0x05f:
/*TODO*///			return wd179x_sector_r(offset);
/*TODO*///		case 0x07f:
/*TODO*///			return wd179x_data_r(offset);
/*TODO*///		case 0x0ff:
/*TODO*///			return betadisk_status;
/*TODO*///	 }
/*TODO*///#endif
/*TODO*///	 logerror("Read from scorpion port: %04x\n", offset);
/*TODO*///
/*TODO*///	 return 0xff;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////* not sure if decoding is full or partial on scorpion */
/*TODO*////* TO BE CHECKED! */
/*TODO*///static WRITE_HANDLER(scorpion_port_w)
/*TODO*///{
/*TODO*///		if ((offset & 1)==0)
/*TODO*///			spectrum_port_fe_w(offset,data);
/*TODO*///
/*TODO*///		else if ((offset & 2)==0)
/*TODO*///		{
/*TODO*///				switch ((offset>>8) & 0xf0)
/*TODO*///				{
/*TODO*///					case 0x70:
/*TODO*///							scorpion_port_7ffd_w(offset, data);
/*TODO*///							break;
/*TODO*///					case 0xb0:
/*TODO*///							spectrum_128_port_bffd_w(offset, data);
/*TODO*///							break;
/*TODO*///					case 0xf0:
/*TODO*///							spectrum_128_port_fffd_w(offset, data);
/*TODO*///							break;
/*TODO*///					case 0x10:
/*TODO*///							scorpion_port_1ffd_w(offset, data);
/*TODO*///							break;
/*TODO*///					default:
/*TODO*///							logerror("Write %02x to scorpion port: %04x\n", data, offset);
/*TODO*///				}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			logerror("Write %02x to scorpion port: %04x\n", data, offset);
/*TODO*///		}
/*TODO*///#if 0
/*TODO*///		/* decoding of these ports might be wrong - to be checked! */
/*TODO*///			switch (offset & 0x0ff)
/*TODO*///			{
/*TODO*///				case 0x01f:
/*TODO*///				{
/*TODO*///					wd179x_command_w(offset,data);
/*TODO*///				}
/*TODO*///				break;
/*TODO*///
/*TODO*///				case 0x03f:
/*TODO*///				{
/*TODO*///					wd179x_track_w(offset,data);
/*TODO*///				}
/*TODO*///				break;
/*TODO*///
/*TODO*///				case 0x05f:
/*TODO*///				{
/*TODO*///					wd179x_sector_w(offset,data);
/*TODO*///				}
/*TODO*///				break;
/*TODO*///
/*TODO*///				case 0x07f:
/*TODO*///				{
/*TODO*///					wd179x_data_w(offset,data);
/*TODO*///				}
/*TODO*///				break;
/*TODO*///
/*TODO*///				case 0x0ff:
/*TODO*///				{
/*TODO*///					int density;
/*TODO*///
/*TODO*///					/*
/*TODO*///						D0, D1 - diskdrive select. 00 for drive A, 01 for drive B
/*TODO*///									   10 for drive C, 11 for drive D
/*TODO*///						D2     - hardware microcontroller reset. by resetting and then setting this bit
/*TODO*///							 again, we can form impulse of microcontroller reset. usually this reset
/*TODO*///							 happenes in very begin of TR-DOS session.
/*TODO*///
/*TODO*///						D3     - this digit blocks signal HLT of microcontroller. For normal work must
/*TODO*///							 contain '1'.
/*TODO*///
/*TODO*///						D4     - Diskdrive head select. contents of this digit translates directly to
/*TODO*///							 diskdrive. 0 means first head or 'bottom' side of disk, 1 - second
/*TODO*///							 head/'top' side of disk.
/*TODO*///
/*TODO*///						D5     - Density select. reset of this digit makes microcontroller works in FM
/*TODO*///							 mode, seted digit - MFM.
/*TODO*///					*/
/*TODO*///
/*TODO*///					wd179x_set_drive(data & 0x03);
/*TODO*///
/*TODO*///			//		if (data & (1<<2))
/*TODO*///			//		{
/*TODO*///			//			wd179x_reset();
/*TODO*///			//		}
/*TODO*///
/*TODO*///					wd179x_set_side((data>>4) & 0x01);
/*TODO*///
/*TODO*///					if (data & (1<<5))
/*TODO*///					{
/*TODO*///						density = DEN_FM_HI;
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						density = DEN_MFM_LO;
/*TODO*///					}
/*TODO*///
/*TODO*///
/*TODO*///					wd179x_set_density(data>>5);
/*TODO*///				}
/*TODO*///			}
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////* ports are not decoded full.
/*TODO*///The function decodes the ports appropriately */
/*TODO*///static PORT_READ_START (scorpion_readport)
/*TODO*///		{0x0000, 0xffff, scorpion_port_r},
/*TODO*///PORT_END
/*TODO*///
/*TODO*////* KT: Changed it to this because the ports are not decoded fully.
/*TODO*///The function decodes the ports appropriately */
/*TODO*///static PORT_WRITE_START (scorpion_writeport)
/*TODO*///		{0x0000, 0xffff, scorpion_port_w},
/*TODO*///PORT_END
/*TODO*///
/*TODO*///
/*TODO*///void scorpion_init_machine(void)
/*TODO*///{
/*TODO*///	if (spectrum_alloc_ram(256))
/*TODO*///	{
/*TODO*///		memory_set_bankhandler_r(4, 0, MRA_BANK4);
/*TODO*///		memory_set_bankhandler_w(8, 0, MWA_BANK8);
/*TODO*///
/*TODO*///		/* Bank 5 is always in 0x4000 - 0x7fff */
/*TODO*///		memory_set_bankhandler_r(2, 0, MRA_BANK2);
/*TODO*///		memory_set_bankhandler_w(6, 0, MWA_BANK6);
/*TODO*///		cpu_setbank(2, spectrum_ram + (5<<14));
/*TODO*///		cpu_setbank(6, spectrum_ram + (5<<14));
/*TODO*///
/*TODO*///		/* Bank 2 is always in 0x8000 - 0xbfff */
/*TODO*///		memory_set_bankhandler_r(3, 0, MRA_BANK3);
/*TODO*///		memory_set_bankhandler_w(7, 0, MWA_BANK7);
/*TODO*///		cpu_setbank(3, spectrum_ram + (2<<14));
/*TODO*///		cpu_setbank(7, spectrum_ram + (2<<14));
/*TODO*///
/*TODO*///
/*TODO*///		spectrum_128_port_7ffd_data = 0;
/*TODO*///		scorpion_256_port_1ffd_data = 0;
/*TODO*///
/*TODO*///		scorpion_update_memory();
/*TODO*///
/*TODO*///		betadisk_init();
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///void	scorpion_exit_machine(void)
/*TODO*///{
/*TODO*///	betadisk_exit();
/*TODO*///	spectrum_free_ram();
/*TODO*///}

        /****************************************************************************************************/
        /* pentagon */

        public static ReadHandlerPtr pentagon_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {            
                return 0x0ff;
            }
        };

        public static WriteHandlerPtr pentagon_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
            }
        };
        
        /* ports are not decoded full.
        The function decodes the ports appropriately */
        static IO_ReadPort pentagon_readport[] = {
            new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_ReadPort(0x0000, 0xffff, pentagon_port_r),
            new IO_ReadPort(MEMPORT_MARKER, 0) /* end of table */
        };

        /* KT: Changed it to this because the ports are not decoded fully.
        The function decodes the ports appropriately */
        static IO_WritePort pentagon_writeport[] = {
            new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
            new IO_WritePort(0x0000, 0xffff, pentagon_port_w),
            new IO_WritePort(MEMPORT_MARKER, 0) /* end of table */
        };


        public static InitMachinePtr pentagon_init_machine = new InitMachinePtr() {
            public void handler() {
            
                if (spectrum_alloc_ram(128) != 0)
                {
                        memory_set_bankhandler_r(4, 0, MRA_BANK4);

                        memory_set_bankhandler_w(8, 0, MWA_BANK8);

                        /* Bank 5 is always in 0x4000 - 0x7fff */
                        memory_set_bankhandler_r(2, 0, MRA_BANK2);
                        memory_set_bankhandler_w(6, 0, MWA_BANK6);
                        cpu_setbank(2, new UBytePtr(spectrum_ram, (5<<14)));
                        cpu_setbank(6, new UBytePtr(spectrum_ram, (5<<14)));

                        /* Bank 2 is always in 0x8000 - 0xbfff */
                        memory_set_bankhandler_r(3, 0, MRA_BANK3);
                        memory_set_bankhandler_w(7, 0, MWA_BANK7);
                        cpu_setbank(3, new UBytePtr(spectrum_ram, (2<<14)));
                        cpu_setbank(7, new UBytePtr(spectrum_ram, (2<<14)));

/*TODO*///                        betadisk_init();
                }
            }
        };

        public static StopMachinePtr pentagon_exit_machine = new StopMachinePtr() {
            public void handler() {
/*TODO*///                betadisk_exit();
                spectrum_free_ram();
            }
        };


        /****************************************************************************************************/
        static GfxLayout spectrum_charlayout = new GfxLayout(
                8,8,
                256,
                1,						/* 1 bits per pixel */

                new int[] { 0 },					/* no bitplanes; 1 bit per pixel */

                new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
                new int[] { 0, 8*256, 16*256, 24*256, 32*256, 40*256, 48*256, 56*256 },

                8				/* every char takes 1 consecutive byte */
        );

        static GfxDecodeInfo spectrum_gfxdecodeinfo[] ={
		new GfxDecodeInfo( 0, 0x0, spectrum_charlayout, 0, 0x80 ),
                new GfxDecodeInfo( 0, 0x0, spectrum_charlayout, 0, 0x80 ),
                new GfxDecodeInfo( 0, 0x0, spectrum_charlayout, 0, 0x80 ),
                new GfxDecodeInfo( -1 ) /* end of array */
	};

        static InputPortPtr input_ports_spectrum = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* 0xFEFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "CAPS SHIFT",                       KEYCODE_Z,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "Z  COPY    :      LN       BEEP",  KEYCODE_Z,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "X  CLEAR   Pound  EXP      INK",   KEYCODE_X,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "C  CONT    ?      LPRINT   PAPER", KEYCODE_C,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "V  CLS     /      LLIST    FLASH", KEYCODE_V,  IP_JOY_NONE );
	
		PORT_START();  /* 0xFDFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "A  NEW     STOP   READ     ~",  KEYCODE_A,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "S  SAVE    NOT    RESTORE  |",  KEYCODE_S,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "D  DIM     STEP   DATA     \\", KEYCODE_D,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "F  FOR     TO     SGN      {",  KEYCODE_F,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "G  GOTO    THEN   ABS      }",  KEYCODE_G,  IP_JOY_NONE );
	
		PORT_START();  /* 0xFBFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "Q  PLOT    <=     SIN      ASN",    KEYCODE_Q,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "W  DRAW    <>     COS      ACS",    KEYCODE_W,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "E  REM     >=     TAN      ATN",    KEYCODE_E,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "R  RUN     <      INT      VERIFY", KEYCODE_R,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "T  RAND    >      RND      MERGE",  KEYCODE_T,  IP_JOY_NONE );
	
			/* interface II uses this port for joystick */
		PORT_START();  /* 0xF7FE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "1          !      BLUE     DEF FN", KEYCODE_1,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "2          @      RED      FN",     KEYCODE_2,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "3          #      MAGENTA  LINE",   KEYCODE_3,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "4          $      GREEN    OPEN#",  KEYCODE_4,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "5          %      CYAN     CLOSE#", KEYCODE_5,  IP_JOY_NONE );
	
			/* protek clashes with interface II! uses 5 = left, 6 = down, 7 = up, 8 = right, 0 = fire */
		PORT_START();  /* 0xEFFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "0          _      BLACK    FORMAT", KEYCODE_0,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "9          );              POINT",  KEYCODE_9,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "8          (               CAT",    KEYCODE_8,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "7          '      WHITE    ERASE",  KEYCODE_7,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "6          &      YELLOW   MOVE",   KEYCODE_6,  IP_JOY_NONE );
	
		PORT_START();  /* 0xDFFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "P  PRINT   \"      TAB      (c)", KEYCODE_P,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "O  POKE    ;      PEEK     OUT", KEYCODE_O,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "I  INPUT   AT     CODE     IN",  KEYCODE_I,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "U  IF      OR     CHR$     ]",   KEYCODE_U,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "Y  RETURN  AND    STR$     [",   KEYCODE_Y,  IP_JOY_NONE );
	
		PORT_START();  /* 0xBFFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "ENTER",                              KEYCODE_ENTER,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "L  LET     =      USR      ATTR",    KEYCODE_L,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "K  LIST    +      LEN      SCREEN$", KEYCODE_K,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "J  LOAD    -      VAL      VAL$",    KEYCODE_J,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "H  GOSUB   ^      SQR      CIRCLE",  KEYCODE_H,  IP_JOY_NONE );
	
		PORT_START();  /* 0x7FFE */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "SPACE",                              KEYCODE_SPACE,   IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "SYMBOL SHIFT",                       KEYCODE_LSHIFT,  IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "M  PAUSE   .      PI       INVERSE", KEYCODE_M,  IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "N  NEXT    ,      INKEY$   OVER",    KEYCODE_N,  IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "B  BORDER  *      BIN      BRIGHT",  KEYCODE_B,  IP_JOY_NONE );
	
			PORT_START();  /* Spectrum+ Keys (set CAPS + 1-5) */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "EDIT          (CAPS + 1)",  KEYCODE_F1,         IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "CAPS LOCK     (CAPS + 2)",  KEYCODE_CAPSLOCK,   IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "TRUE VID      (CAPS + 3)",  KEYCODE_F2,         IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "INV VID       (CAPS + 4)",  KEYCODE_F3,         IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "Cursor left   (CAPS + 5)",  KEYCODE_LEFT,       IP_JOY_NONE );
			PORT_BIT(0xe0, IP_ACTIVE_LOW, IPT_UNUSED);
	
			PORT_START();  /* Spectrum+ Keys (set CAPS + 6-0) */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "DEL           (CAPS + 0)",  KEYCODE_BACKSPACE,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "GRAPH         (CAPS + 9)",  KEYCODE_LALT,       IP_JOY_NONE );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "Cursor right  (CAPS + 8)",  KEYCODE_RIGHT,      IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "Cursor up     (CAPS + 7)",  KEYCODE_UP,         IP_JOY_NONE );
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "Cursor down   (CAPS + 6)",  KEYCODE_DOWN,       IP_JOY_NONE );
			PORT_BIT(0xe0, IP_ACTIVE_LOW, IPT_UNUSED);
	
			PORT_START();  /* Spectrum+ Keys (set CAPS + SPACE and CAPS + SYMBOL */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "BREAK",                     KEYCODE_PAUSE,      IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "EXT MODE",                  KEYCODE_RCONTROL,   IP_JOY_NONE );
			PORT_BIT(0xfc, IP_ACTIVE_LOW, IPT_UNUSED);
	
			PORT_START();  /* Spectrum+ Keys (set SYMBOL SHIFT + O/P */
	//		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "\"", KEYCODE_F4,  IP_JOY_NONE );
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "\"", KEYCODE_MINUS_PAD,  IP_JOY_NONE );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, ";", KEYCODE_COLON,  IP_JOY_NONE );
			PORT_BIT(0xfc, IP_ACTIVE_LOW, IPT_UNUSED);
	
			PORT_START();  /* Spectrum+ Keys (set SYMBOL SHIFT + N/M */
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, ".", KEYCODE_STOP,   IP_JOY_NONE );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, ",", KEYCODE_COMMA,  IP_JOY_NONE );
			PORT_BIT(0xf3, IP_ACTIVE_LOW, IPT_UNUSED);
	
			PORT_START();  /* Kempston joystick interface */
			PORT_BITX(0x01, IP_ACTIVE_HIGH, IPT_KEYBOARD, "KEMPSTON JOYSTICK RIGHT",     IP_KEY_NONE,    JOYCODE_1_RIGHT );
			PORT_BITX(0x02, IP_ACTIVE_HIGH, IPT_KEYBOARD, "KEMPSTON JOYSTICK LEFT",      IP_KEY_NONE,   JOYCODE_1_LEFT );
			PORT_BITX(0x04, IP_ACTIVE_HIGH, IPT_KEYBOARD, "KEMPSTON JOYSTICK DOWN",         IP_KEY_NONE,        JOYCODE_1_DOWN );
			PORT_BITX(0x08, IP_ACTIVE_HIGH, IPT_KEYBOARD, "KEMPSTON JOYSTICK UP",         IP_KEY_NONE,        JOYCODE_1_UP);
			PORT_BITX(0x10, IP_ACTIVE_HIGH, IPT_KEYBOARD, "KEMPSTON JOYSTICK FIRE",         IP_KEY_NONE,        JOYCODE_1_BUTTON1 );
	
			PORT_START();  /* Fuller joystick interface */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "FULLER JOYSTICK UP",     IP_KEY_NONE,    JOYCODE_1_UP );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "FULLER JOYSTICK DOWN",      IP_KEY_NONE,   JOYCODE_1_DOWN );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "FULLER JOYSTICK LEFT",         IP_KEY_NONE,        JOYCODE_1_LEFT );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "FULLER JOYSTICK RIGHT",         IP_KEY_NONE,        JOYCODE_1_RIGHT);
			PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "FULLER JOYSTICK FIRE",         IP_KEY_NONE,        JOYCODE_1_BUTTON1);
	
			PORT_START();  /* Mikrogen joystick interface */
			PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "MIKROGEN JOYSTICK UP",     IP_KEY_NONE,    JOYCODE_1_UP );
			PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "MIKROGEN JOYSTICK DOWN",      IP_KEY_NONE,   JOYCODE_1_DOWN );
			PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "MIKROGEN JOYSTICK RIGHT",         IP_KEY_NONE,        JOYCODE_1_RIGHT );
			PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "MIKROGEN JOYSTICK LEFT",         IP_KEY_NONE,        JOYCODE_1_LEFT);
			PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "MIKROGEN JOYSTICK FIRE",         IP_KEY_NONE,        JOYCODE_1_BUTTON1);
	
	
			PORT_START(); 
			PORT_BITX(0x8000, IP_ACTIVE_HIGH, IPT_KEYBOARD, "Quickload", KEYCODE_F8, IP_JOY_NONE);
			PORT_DIPNAME(0x80, 0x00, "Hardware Version");
			PORT_DIPSETTING(0x00, "Issue 2" );
			PORT_DIPSETTING(0x80, "Issue 3" );
			PORT_DIPNAME(0x40, 0x00, "End of .TAP action");
			PORT_DIPSETTING(0x00, "Disable .TAP support" );
			PORT_DIPSETTING(0x40, "Rewind tape to start (to reload earlier levels)" );
			PORT_DIPNAME(0x20, 0x00, "+3/+2a etc. Disk Drive");
			PORT_DIPSETTING(0x00, "Enabled" );
			PORT_DIPSETTING(0x20, "Disabled" );
			PORT_BIT(0x1f, IP_ACTIVE_LOW, IPT_UNUSED);
	
	INPUT_PORTS_END(); }}; 
        
        static char spectrum_palette[] = {
                0x00, 0x00, 0x00, 0x00, 0x00, 0xbf,
                0xbf, 0x00, 0x00, 0xbf, 0x00, 0xbf,
                0x00, 0xbf, 0x00, 0x00, 0xbf, 0xbf,
                0xbf, 0xbf, 0x00, 0xbf, 0xbf, 0xbf,

                0x00, 0x00, 0x00, 0x00, 0x00, 0xff,
                0xff, 0x00, 0x00, 0xff, 0x00, 0xff,
                0x00, 0xff, 0x00, 0x00, 0xff, 0xff,
                0xff, 0xff, 0x00, 0xff, 0xff, 0xff,
        };

        static char spectrum_colortable[] = {
                0,0, 0,1, 0,2, 0,3, 0,4, 0,5, 0,6, 0,7,
                1,0, 1,1, 1,2, 1,3, 1,4, 1,5, 1,6, 1,7,
                2,0, 2,1, 2,2, 2,3, 2,4, 2,5, 2,6, 2,7,
                3,0, 3,1, 3,2, 3,3, 3,4, 3,5, 3,6, 3,7,
                4,0, 4,1, 4,2, 4,3, 4,4, 4,5, 4,6, 4,7,
                5,0, 5,1, 5,2, 5,3, 5,4, 5,5, 5,6, 5,7,
                6,0, 6,1, 6,2, 6,3, 6,4, 6,5, 6,6, 6,7,
                7,0, 7,1, 7,2, 7,3, 7,4, 7,5, 7,6, 7,7,

                 8,8,  8,9,  8,10,	8,11,  8,12,  8,13,  8,14,	8,15,
                 9,8,  9,9,  9,10,	9,11,  9,12,  9,13,  9,14,	9,15,
                10,8, 10,9, 10,10, 10,11, 10,12, 10,13, 10,14, 10,15,
                11,8, 11,9, 11,10, 11,11, 11,12, 11,13, 11,14, 11,15,
                12,8, 12,9, 12,10, 12,11, 12,12, 12,13, 12,14, 12,15,
                13,8, 13,9, 13,10, 13,11, 13,12, 13,13, 13,14, 13,15,
                14,8, 14,9, 14,10, 14,11, 14,12, 14,13, 14,14, 14,15,
                15,8, 15,9, 15,10, 15,11, 15,12, 15,13, 15,14, 15,15
        };
        
        static VhConvertColorPromPtr spectrum_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
                memcpy(sys_palette,spectrum_palette,spectrum_palette.length);
		memcpy(sys_colortable,spectrum_colortable,spectrum_colortable.length);
            }
        };
        
        static int quickload = 0;

        public static InterruptPtr spec_interrupt = new InterruptPtr() {
            public int handler() {
                
                if (quickload==0 && (readinputport(16) & 0x8000)!=0)
                {
                                spec_quick_open.handler(0, 0, null);
                                quickload = 1;
                }
                else
                                quickload = 0;

                return interrupt.handler();
            }
        };

        static Speaker_interface spectrum_speaker_interface=new Speaker_interface
	(
	 1,
	 new int[]{50}
	);
        
        static Wave_interface spectrum_wave_interface=new Wave_interface
	(
		1,	  /* number of cassette drives = number of waves to mix */
		new int[]{25}	/* default mixing level */
	
	);

        static MachineDriver machine_driver_spectrum = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80|CPU_16BIT_PORT,
				3500000,		/* 3.5 Mhz */
				spectrum_readmem,spectrum_writemem,
				spectrum_readport,spectrum_writeport,
				spec_interrupt,1
			)
		},
		50, 2500,		/* frames per second, vblank duration */
		1,
		spectrum_init_machine,
		spectrum_shutdown_machine,
	
		/* video hardware */
		SPEC_SCREEN_WIDTH,				/* screen width */
		SPEC_SCREEN_HEIGHT, 			/* screen height */
		new rectangle( 0, SPEC_SCREEN_WIDTH-1, 0, SPEC_SCREEN_HEIGHT-1),  /* visible_area */
		spectrum_gfxdecodeinfo, 			 /* graphics decode info */
		16, 256,							 /* colors used for the characters */
		spectrum_init_palette,				 /* initialise palette */
	
		VIDEO_TYPE_RASTER,
		spectrum_eof_callback,
		spectrum_vh_start,
		spectrum_vh_stop,
		spectrum_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			/* standard spectrum sound */
			new MachineSound(
					SOUND_SPEAKER,
					spectrum_speaker_interface
			),
			/* cassette wave sound */
			new MachineSound(
					SOUND_WAVE,
					spectrum_wave_interface
			)
		}
	);

        static MachineDriver machine_driver_spectrum_128 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80|CPU_16BIT_PORT,
				3546900,		/* 3.54690 Mhz */
				spectrum_128_readmem,spectrum_128_writemem,
				spectrum_128_readport,spectrum_128_writeport,
				spec_interrupt,1
			)
		},
		50, 2500,		/* frames per second, vblank duration */
		1,
		spectrum_128_init_machine,
		spectrum_128_exit_machine,
	
		/* video hardware */
		SPEC_SCREEN_WIDTH,				/* screen width */
		SPEC_SCREEN_HEIGHT, 			/* screen height */
		new rectangle( 0, SPEC_SCREEN_WIDTH-1, 0, SPEC_SCREEN_HEIGHT-1),  /* visible_area */
		spectrum_gfxdecodeinfo, 			 /* graphics decode info */
		16, 256,							 /* colors used for the characters */
		spectrum_init_palette,				 /* initialise palette */
	
		VIDEO_TYPE_RASTER,
		spectrum_eof_callback,
		spectrum_128_vh_start,
		spectrum_128_vh_stop,
		spectrum_128_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			/* Ay-3-8912 sound */
			new MachineSound(
					SOUND_AY8910,
					spectrum_ay_interface
			),
			/* standard spectrum buzzer sound */
			new MachineSound(
					SOUND_SPEAKER,
					spectrum_speaker_interface
			),
			/* cassette wave sound */
			new MachineSound(
					SOUND_WAVE,
					spectrum_wave_interface
			)
		}
	);

        static MachineDriver machine_driver_spectrum_plus3 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80|CPU_16BIT_PORT,
				3546900,		/* 3.54690 Mhz */
				spectrum_128_readmem,spectrum_128_writemem,
				spectrum_plus3_readport,spectrum_plus3_writeport,
				spec_interrupt,1
			)
		},
		50, 2500,		/* frames per second, vblank duration */
		1,
		spectrum_plus3_init_machine,
		spectrum_plus3_exit_machine,
	
		/* video hardware */
		SPEC_SCREEN_WIDTH,				/* screen width */
		SPEC_SCREEN_HEIGHT, 			/* screen height */
		new rectangle( 0, SPEC_SCREEN_WIDTH-1, 0, SPEC_SCREEN_HEIGHT-1),  /* visible_area */
		spectrum_gfxdecodeinfo, 			 /* graphics decode info */
		16, 256,							 /* colors used for the characters */
		spectrum_init_palette,				 /* initialise palette */
	
		VIDEO_TYPE_RASTER,
		spectrum_eof_callback,
		spectrum_128_vh_start,
		spectrum_128_vh_stop,
		spectrum_128_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			/*  Ay-3-8912 sound */
			new MachineSound(
					SOUND_AY8910,
					spectrum_ay_interface
			),
			/* standard spectrum buzzer sound */
			new MachineSound(
					SOUND_SPEAKER,
					spectrum_speaker_interface
			),
			/* cassette wave sound */
			new MachineSound(
					SOUND_WAVE,
					spectrum_wave_interface
			)
		}
	);

/*TODO*///static struct MachineDriver machine_driver_ts2068 =
/*TODO*///{
/*TODO*///	/* basic machine hardware */
/*TODO*///	{
/*TODO*///		{
/*TODO*///			CPU_Z80|CPU_16BIT_PORT,
/*TODO*///			3580000,		/* 3.58 Mhz */
/*TODO*///			ts2068_readmem,ts2068_writemem,
/*TODO*///			ts2068_readport,ts2068_writeport,
/*TODO*///			spec_interrupt,1,
/*TODO*///		},
/*TODO*///	},
/*TODO*///		60, 2500,		/* frames per second, vblank duration */
/*TODO*///	1,
/*TODO*///	ts2068_init_machine,
/*TODO*///	ts2068_exit_machine,
/*TODO*///
/*TODO*///	/* video hardware */
/*TODO*///	TS2068_SCREEN_WIDTH,			/* screen width */
/*TODO*///	TS2068_SCREEN_HEIGHT,			/* screen height */
/*TODO*///	{ 0, TS2068_SCREEN_WIDTH-1, 0, TS2068_SCREEN_HEIGHT-1},  /* visible_area */
/*TODO*///	spectrum_gfxdecodeinfo, 			 /* graphics decode info */
/*TODO*///	16, 256,							 /* colors used for the characters */
/*TODO*///	spectrum_init_palette,				 /* initialise palette */
/*TODO*///
/*TODO*///	VIDEO_TYPE_RASTER | VIDEO_PIXEL_ASPECT_RATIO_1_2,
/*TODO*///	ts2068_eof_callback,
/*TODO*///	spectrum_128_vh_start,
/*TODO*///	spectrum_128_vh_stop,
/*TODO*///	ts2068_vh_screenrefresh,
/*TODO*///
/*TODO*///	/* sound hardware */
/*TODO*///	0,0,0,0,
/*TODO*///	{
/*TODO*///		/*  Ay-3-8912 sound */
/*TODO*///		{
/*TODO*///				SOUND_AY8910,
/*TODO*///				&spectrum_ay_interface,
/*TODO*///		},
/*TODO*///		/* standard spectrum sound */
/*TODO*///		{
/*TODO*///				SOUND_SPEAKER,
/*TODO*///				&spectrum_speaker_interface
/*TODO*///		},
/*TODO*///		/* cassette wave sound */
/*TODO*///		{
/*TODO*///				SOUND_WAVE,
/*TODO*///				&spectrum_wave_interface,
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
/*TODO*///static struct MachineDriver machine_driver_uk2086 =
/*TODO*///{
/*TODO*///	/* basic machine hardware */
/*TODO*///	{
/*TODO*///		{
/*TODO*///			CPU_Z80|CPU_16BIT_PORT,
/*TODO*///			3580000,		/* 3.58 Mhz */
/*TODO*///			ts2068_readmem,ts2068_writemem,
/*TODO*///			ts2068_readport,ts2068_writeport,
/*TODO*///			spec_interrupt,1,
/*TODO*///		},
/*TODO*///	},
/*TODO*///		50, 2500,		/* frames per second, vblank duration */
/*TODO*///	1,
/*TODO*///	ts2068_init_machine,
/*TODO*///	ts2068_exit_machine,
/*TODO*///
/*TODO*///	/* video hardware */
/*TODO*///	TS2068_SCREEN_WIDTH,			/* screen width */
/*TODO*///	TS2068_SCREEN_HEIGHT,			/* screen height */
/*TODO*///	{ 0, TS2068_SCREEN_WIDTH-1, 0, TS2068_SCREEN_HEIGHT-1},  /* visible_area */
/*TODO*///	spectrum_gfxdecodeinfo, 			 /* graphics decode info */
/*TODO*///	16, 256,							 /* colors used for the characters */
/*TODO*///	spectrum_init_palette,				 /* initialise palette */
/*TODO*///
/*TODO*///	VIDEO_TYPE_RASTER | VIDEO_PIXEL_ASPECT_RATIO_1_2,
/*TODO*///	ts2068_eof_callback,
/*TODO*///	spectrum_128_vh_start,
/*TODO*///	spectrum_128_vh_stop,
/*TODO*///	ts2068_vh_screenrefresh,
/*TODO*///
/*TODO*///	/* sound hardware */
/*TODO*///	0,0,0,0,
/*TODO*///	{
/*TODO*///		/*  Ay-3-8912 sound */
/*TODO*///		{
/*TODO*///				SOUND_AY8910,
/*TODO*///				&spectrum_ay_interface,
/*TODO*///		},
/*TODO*///		/* standard spectrum sound */
/*TODO*///		{
/*TODO*///				SOUND_SPEAKER,
/*TODO*///				&spectrum_speaker_interface
/*TODO*///		},
/*TODO*///		/* cassette wave sound */
/*TODO*///		{
/*TODO*///				SOUND_WAVE,
/*TODO*///				&spectrum_wave_interface,
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
/*TODO*///static struct MachineDriver machine_driver_tc2048 =
/*TODO*///{
/*TODO*///	/* basic machine hardware */
/*TODO*///	{
/*TODO*///		{
/*TODO*///			CPU_Z80|CPU_16BIT_PORT,
/*TODO*///			3500000,		/* 3.5 Mhz */
/*TODO*///			tc2048_readmem,tc2048_writemem,
/*TODO*///			tc2048_readport,tc2048_writeport,
/*TODO*///			spec_interrupt,1,
/*TODO*///		},
/*TODO*///	},
/*TODO*///	50, 2500,		/* frames per second, vblank duration */
/*TODO*///	1,
/*TODO*///	tc2048_init_machine,
/*TODO*///	ts2068_exit_machine,
/*TODO*///
/*TODO*///	/* video hardware */
/*TODO*///	TS2068_SCREEN_WIDTH,			/* screen width */
/*TODO*///	SPEC_SCREEN_HEIGHT, 			/* screen height */
/*TODO*///	{ 0, TS2068_SCREEN_WIDTH-1, 0, SPEC_SCREEN_HEIGHT-1},  /* visible_area */
/*TODO*///	spectrum_gfxdecodeinfo, 			 /* graphics decode info */
/*TODO*///	16, 256,							 /* colors used for the characters */
/*TODO*///	spectrum_init_palette,				 /* initialise palette */
/*TODO*///
/*TODO*///	VIDEO_TYPE_RASTER | VIDEO_PIXEL_ASPECT_RATIO_1_2,
/*TODO*///	spectrum_eof_callback,
/*TODO*///	spectrum_128_vh_start,
/*TODO*///	spectrum_128_vh_stop,
/*TODO*///	tc2048_vh_screenrefresh,
/*TODO*///
/*TODO*///	/* sound hardware */
/*TODO*///	0,0,0,0,
/*TODO*///	{
/*TODO*///		/* standard spectrum sound */
/*TODO*///		{
/*TODO*///				SOUND_SPEAKER,
/*TODO*///				&spectrum_speaker_interface
/*TODO*///		},
/*TODO*///		/* cassette wave sound */
/*TODO*///		{
/*TODO*///				SOUND_WAVE,
/*TODO*///				&spectrum_wave_interface,
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*///static struct MachineDriver machine_driver_scorpion =
/*TODO*///{
/*TODO*///	/* basic machine hardware */
/*TODO*///	{
/*TODO*///		{
/*TODO*///			CPU_Z80|CPU_16BIT_PORT,
/*TODO*///			3546900,		/* 3.54690 Mhz */
/*TODO*///			spectrum_128_readmem,spectrum_128_writemem,
/*TODO*///			scorpion_readport,scorpion_writeport,
/*TODO*///			spec_interrupt,1,
/*TODO*///		},
/*TODO*///	},
/*TODO*///	50, 2500,		/* frames per second, vblank duration */
/*TODO*///	1,
/*TODO*///	scorpion_init_machine,
/*TODO*///	scorpion_exit_machine,
/*TODO*///
/*TODO*///	/* video hardware */
/*TODO*///	SPEC_SCREEN_WIDTH,				/* screen width */
/*TODO*///	SPEC_SCREEN_HEIGHT, 			/* screen height */
/*TODO*///	{ 0, SPEC_SCREEN_WIDTH-1, 0, SPEC_SCREEN_HEIGHT-1},  /* visible_area */
/*TODO*///	spectrum_gfxdecodeinfo, 			 /* graphics decode info */
/*TODO*///	16, 256,							 /* colors used for the characters */
/*TODO*///	spectrum_init_palette,				 /* initialise palette */
/*TODO*///
/*TODO*///	VIDEO_TYPE_RASTER,
/*TODO*///	spectrum_eof_callback,
/*TODO*///	spectrum_128_vh_start,
/*TODO*///	spectrum_128_vh_stop,
/*TODO*///	spectrum_128_vh_screenrefresh,
/*TODO*///
/*TODO*///	/* sound hardware */
/*TODO*///	0,0,0,0,
/*TODO*///	{
/*TODO*///		/* Ay-3-8912 sound */
/*TODO*///		{
/*TODO*///				SOUND_AY8910,
/*TODO*///				&spectrum_ay_interface,
/*TODO*///		},
/*TODO*///		/* standard spectrum buzzer sound */
/*TODO*///		{
/*TODO*///				SOUND_SPEAKER,
/*TODO*///				&spectrum_speaker_interface,
/*TODO*///		},
/*TODO*///		/* cassette wave sound */
/*TODO*///		{
/*TODO*///				SOUND_WAVE,
/*TODO*///				&spectrum_wave_interface,
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
        static MachineDriver machine_driver_pentagon = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
                    new MachineCPU(
			CPU_Z80|CPU_16BIT_PORT,
			3546900,		/* 3.54690 Mhz */
			spectrum_128_readmem,spectrum_128_writemem,
			pentagon_readport,pentagon_writeport,
			spec_interrupt,1
                    )
                },
                50, 2500,		/* frames per second, vblank duration */
                1,
                pentagon_init_machine,
                pentagon_exit_machine,

                /* video hardware */
                SPEC_SCREEN_WIDTH,				/* screen width */
                SPEC_SCREEN_HEIGHT, 			/* screen height */
                new rectangle( 0, SPEC_SCREEN_WIDTH-1, 0, SPEC_SCREEN_HEIGHT-1),  /* visible_area */
                spectrum_gfxdecodeinfo, 			 /* graphics decode info */
                16, 256,							 /* colors used for the characters */
                spectrum_init_palette,				 /* initialise palette */

                VIDEO_TYPE_RASTER,
                spectrum_eof_callback,
                spectrum_128_vh_start,
                spectrum_128_vh_stop,
                spectrum_128_vh_screenrefresh,

                /* sound hardware */
                0,0,0,0,
                new MachineSound[] {
			/* Ay-3-8912 sound */
			new MachineSound(
					SOUND_AY8910,
					spectrum_ay_interface
			),
			/* standard spectrum buzzer sound */
			new MachineSound(
					SOUND_SPEAKER,
					spectrum_speaker_interface
			),
			/* cassette wave sound */
			new MachineSound(
					SOUND_WAVE,
					spectrum_wave_interface
			)
		}
	);


        /***************************************************************************

          Game driver(s)

        ***************************************************************************/

        static RomLoadPtr rom_spectrum = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10000,REGION_CPU1,0);
		ROM_LOAD("spectrum.rom", 0x0000, 0x4000, 0xddee531f);
	ROM_END(); }}; 

/*TODO*///ROM_START(specbusy)
/*TODO*///	ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///	ROM_LOAD("48-busy.rom", 0x0000, 0x4000, 0x1511cddb)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specpsch)
/*TODO*///	ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///	ROM_LOAD("48-psych.rom", 0x0000, 0x4000, 0xcd60b589)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specgrot)
/*TODO*///	ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///	ROM_LOAD("48-groot.rom", 0x0000, 0x4000, 0xabf18c45)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specimc)
/*TODO*///	ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///	ROM_LOAD("48-imc.rom", 0x0000, 0x4000, 0xd1be99ee)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(speclec)
/*TODO*///	ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///	ROM_LOAD("80-lec.rom", 0x0000, 0x4000, 0x5b5c92b1)
/*TODO*///ROM_END
        static RomLoadPtr rom_spec128 = new RomLoadPtr(){ 
            public void handler(){ 
		ROM_REGION(0x18000,REGION_CPU1,0);
		ROM_LOAD("zx128_0.rom",0x10000,0x4000, 0xe76799d2);
		ROM_LOAD("zx128_1.rom",0x14000,0x4000, 0xb96a36be);
                ROM_END(); 
            }
        };
        
/*TODO*///ROM_START(spec128s)
/*TODO*///		ROM_REGION(0x18000,REGION_CPU1,0)
/*TODO*///	ROM_LOAD("zx128s0.rom",0x10000,0x4000, 0x453d86b2)
/*TODO*///	ROM_LOAD("zx128s1.rom",0x14000,0x4000, 0x6010e796)
/*TODO*///ROM_END
/*TODO*///
         
        static RomLoadPtr rom_specpls2 = new RomLoadPtr(){ public void handler(){ 
                ROM_REGION(0x18000,REGION_CPU1,0);
                ROM_LOAD("zxp2_0.rom",0x10000,0x4000, 0x5d2e8c66);
                ROM_LOAD("zxp2_1.rom",0x14000,0x4000, 0x98b1320b);
        ROM_END(); }};

/*TODO*///ROM_START(specpl2a)
/*TODO*///		ROM_REGION(0x20000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("p2a41_0.rom",0x10000,0x4000, 0x30c9f490)
/*TODO*///		ROM_LOAD("p2a41_1.rom",0x14000,0x4000, 0xa7916b3f)
/*TODO*///		ROM_LOAD("p2a41_2.rom",0x18000,0x4000, 0xc9a0b748)
/*TODO*///		ROM_LOAD("p2a41_3.rom",0x1c000,0x4000, 0xb88fd6e3)
/*TODO*///ROM_END

        static RomLoadPtr rom_specpls3 = new RomLoadPtr(){ public void handler(){ 
                ROM_REGION(0x20000,REGION_CPU1,0);
                ROM_LOAD("pl3-0.rom",0x10000,0x4000, 0x17373da2);
                ROM_LOAD("pl3-1.rom",0x14000,0x4000, 0xf1d1d99e);
                ROM_LOAD("pl3-2.rom",0x18000,0x4000, 0x3dbf351d);
                ROM_LOAD("pl3-3.rom",0x1c000,0x4000, 0x04448eaa);
        ROM_END(); }};

/*TODO*///ROM_START(specpls4)
/*TODO*///		ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("plus4.rom",0x0000,0x4000, 0x7e0f47cb)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(tk90x)
/*TODO*///		ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("tk90x.rom",0x0000,0x4000, 0x3e785f6f)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(tk95)
/*TODO*///		ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("tk95.rom",0x0000,0x4000, 0x17368e07)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(inves)
/*TODO*///		ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("inves.rom",0x0000,0x4000, 0x8ff7a4d1)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(tc2048)
/*TODO*///		ROM_REGION(0x10000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("tc2048.rom",0x0000,0x4000, 0xf1b5fa67)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(ts2068)
/*TODO*///		ROM_REGION(0x16000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("ts2068_h.rom",0x10000,0x4000, 0xbf44ec3f)
/*TODO*///		ROM_LOAD("ts2068_x.rom",0x14000,0x2000, 0xae16233a)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(uk2086)
/*TODO*///		ROM_REGION(0x16000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("uk2086_h.rom",0x10000,0x4000, 0x5ddc0ca2)
/*TODO*///		ROM_LOAD("ts2068_x.rom",0x14000,0x2000, 0xae16233a)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specp2fr)
/*TODO*///		ROM_REGION(0x18000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("plus2fr0.rom",0x10000,0x4000, 0xc684c535)
/*TODO*///		ROM_LOAD("plus2fr1.rom",0x14000,0x4000, 0xf5e509c5)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specp2sp)
/*TODO*///		ROM_REGION(0x18000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("plus2sp0.rom",0x10000,0x4000, 0xe807d06e)
/*TODO*///		ROM_LOAD("plus2sp1.rom",0x14000,0x4000, 0x41981d4b)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specp3sp)
/*TODO*///		ROM_REGION(0x20000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("plus3sp0.rom",0x10000,0x4000, 0x1f86147a)
/*TODO*///		ROM_LOAD("plus3sp1.rom",0x14000,0x4000, 0xa8ac4966)
/*TODO*///		ROM_LOAD("plus3sp2.rom",0x18000,0x4000, 0xf6bb0296)
/*TODO*///		ROM_LOAD("plus3sp3.rom",0x1c000,0x4000, 0xf6d25389)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specpl3e)
/*TODO*///		ROM_REGION(0x20000,REGION_CPU1,0)
/*TODO*///		ROM_LOAD("roma-en.rom",0x10000,0x8000, 0x14fddc04)
/*TODO*///		ROM_LOAD("romb-en.rom",0x18000,0x8000, 0xba488ccd)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(specp3es)
/*TODO*///	        ROM_REGION(0x20000,REGION_CPU1,0)
/*TODO*///	        ROM_LOAD("roma-es.rom",0x10000,0x8000, 0x932f1801)
/*TODO*///	        ROM_LOAD("romb-es.rom",0x18000,0x8000, 0xf0a12485)
/*TODO*///ROM_END
/*TODO*///
/*TODO*///ROM_START(scorpion)
/*TODO*///		ROM_REGION(0x020000, REGION_CPU1, 0)
/*TODO*///		ROM_LOAD("scorp0.rom",0x010000, 0x4000, 0x0eb40a09)
/*TODO*///		ROM_LOAD("scorp1.rom",0x014000, 0x4000, 0x9d513013)
/*TODO*///		ROM_LOAD("scorp2.rom",0x018000, 0x4000, 0xfd0d3ce1)
/*TODO*///		ROM_LOAD("scorp3.rom",0x01c000, 0x4000, 0x1fe1d003)
/*TODO*///ROM_END

        static RomLoadPtr rom_pentagon = new RomLoadPtr(){ public void handler(){ 
                ROM_REGION(0x020000, REGION_CPU1, 0);                
        ROM_END(); }};

        static IODevice IODEVICE_SPEC_QUICK =
	   new IODevice(
                IO_QUICKLOAD,	   /* type */
                1,				   /* count */
                "scr\0",            /* file extensions */
                IO_RESET_ALL,	   /* reset if file changed */
                null,			   /* id */
                spec_quick_init,    /* init */
                spec_quick_exit,    /* exit */
                null,			   /* info */
                spec_quick_open,    /* open */
                null,				/* close */
                null,				/* status */
                null,				/* seek */
                null,                           /* tell */
                null,				/* input */
                null,				/* output */
                null,				/* input_chunk */
                null				/* output_chunk */
            );

        static IODevice io_spectrum[] = {
            new IODevice(
		IO_SNAPSHOT,		/* type */
		1,					/* count */
		"sna\0z80\0sp\0",       /* file extensions */
		IO_RESET_ALL,		/* reset if file changed */
		null, //spectrum_rom_id,
                spectrum_snap_load,	/* init */
                spectrum_snap_exit,	/* exit */
                null,				/* info */
                null,				/* open */
                null,				/* close */
                null,				/* status */
                null,				/* seek */
                null,                           /* tell */
                null,				/* input */
                null,				/* output */
                null,				/* input_chunk */
                null				/* output_chunk */
            ),
            IODEVICE_SPEC_QUICK,
            new IODevice(
                                IO_CASSETTE,
                        1,
                        "wav\0tap\0",
                        IO_RESET_NONE,
                        null,
                        spectrum_cassette_init, 
                        spectrum_cassette_exit,
                        wave_info,			/* info */						
                        wave_open,			/* open */						
                        null, //wave_close, 		/* close */ 					
                        wave_status,		/* status */					
                        wave_seek,			/* seek */						
                        wave_tell,			/* tell */						
                        wave_input, 		/* input */ 					
                        null,//wave_output,		/* output */					
                        wave_input_chunk,	/* input_chunk */				
                        wave_output_chunk),
	new IODevice(
			IO_CARTSLOT,		/* type */
			1,					/* count */
			"rom\0",			/* file extensions */
			IO_RESET_ALL,		/* reset if file changed */
			null,
			spectrum_cart_load,	/* init */
			null,				/* exit */
			null,				/* info */
			null,				/* open */
			null,				/* close */
			null,				/* status */
			null,				/* seek */
                        null,                           /* tell */
			null,				/* input */
			null,				/* output */
			null,				/* input_chunk */
			null				/* output_chunk */
                ),
		new IODevice(IO_END)
        };

        static IODevice io_specpls3[] = {
            new IODevice(
		IO_SNAPSHOT,		/* type */
		1,					/* count */
		"sna\0z80\0sp\0",       /* file extensions */
		IO_RESET_ALL,		/* reset if file changed */
		null,
		spectrum_snap_load,	/* init */
		spectrum_snap_exit,	/* exit */
		null,				/* info */
                null,				/* open */
                null,				/* close */
                null,				/* status */
                null,				/* seek */
                null,                           /* tell */
                null,				/* input */
                null,				/* output */
                null,				/* input_chunk */
                null				/* output_chunk */
            ),
		IODEVICE_SPEC_QUICK,
		IO_CASSETTE_WAVE(1,"wav\0tap\0blk\0", null,spectrum_cassette_init, spectrum_cassette_exit),
            new IODevice(
		IO_FLOPPY,			/* type */
		2,					/* count */
		"dsk\0",            /* file extensions */
		IO_RESET_NONE,		/* reset if file changed */
		null,
		dsk_floppy_load,                /* init */
		dsk_floppy_exit,                /* exit */
		null,				/* info */
		null,				/* open */
		null,				/* close */
                floppy_status,                  /* status */
		null,				/* seek */
		null,                           /* tell */
                null,				/* input */
		null,				/* output */
		null,				/* input_chunk */
		null				/* output chunk */
            ),
            new IODevice(IO_END)
        };

/*TODO*///static const struct IODevice io_ts2068[] = {
/*TODO*///	{
/*TODO*///		IO_SNAPSHOT,		/* type */
/*TODO*///		1,					/* count */
/*TODO*///		"sna\0z80\0sp\0",       /* file extensions */
/*TODO*///		IO_RESET_ALL,		/* reset if file changed */
/*TODO*///		0,
/*TODO*///		spectrum_snap_load,	/* init */
/*TODO*///		spectrum_snap_exit,	/* exit */
/*TODO*///		null,				/* info */
/*TODO*///		null,				/* open */
/*TODO*///		null,				/* close */
/*TODO*///		null,				/* status */
/*TODO*///		null,				/* seek */
/*TODO*///		null,				/* input */
/*TODO*///		null,				/* output */
/*TODO*///		null,				/* input_chunk */
/*TODO*///		null				/* output_chunk */
/*TODO*///	},
/*TODO*///		IODEVICE_SPEC_QUICK,
/*TODO*///		IO_CASSETTE_WAVE(1,"wav\0tap\0blk\0", null,spectrum_cassette_init, spectrum_cassette_exit),
/*TODO*///	{
/*TODO*///		IO_CARTSLOT,			/* type */
/*TODO*///		1,				/* count */
/*TODO*///		"dck\0",			/* file extensions */
/*TODO*///		IO_RESET_ALL,			/* reset if file changed */
/*TODO*///		0,
/*TODO*///		timex_cart_load,		/* init */
/*TODO*///		timex_cart_exit,		/* exit */
/*TODO*///		null,				/* info */
/*TODO*///		null,				/* open */
/*TODO*///		null,				/* close */
/*TODO*///		null,				/* status */
/*TODO*///		null,				/* seek */
/*TODO*///		null,				/* input */
/*TODO*///		null,				/* output */
/*TODO*///		null,				/* input_chunk */
/*TODO*///		null				/* output_chunk */
/*TODO*///	},
/*TODO*///		{ IO_END }
/*TODO*///};
/*TODO*///
/*TODO*///#define io_spec128	io_spectrum
/*TODO*///#define io_spec128s	io_spectrum
/*TODO*///#define io_specpls2	io_spectrum
/*TODO*///#define io_specbusy	io_spectrum
/*TODO*///#define io_specpsch	io_spectrum
/*TODO*///#define io_specgrot	io_spectrum
/*TODO*///#define io_specimc	io_spectrum
/*TODO*///#define io_speclec	io_spectrum
/*TODO*///#define io_specpls4	io_spectrum
/*TODO*///#define io_inves	io_spectrum
/*TODO*///#define io_tk90x	io_spectrum
/*TODO*///#define io_tk95 	io_spectrum
/*TODO*///#define io_tc2048	io_spectrum
/*TODO*///#define io_uk2086	io_ts2068
/*TODO*///#define io_specpl2a	io_specpls3
/*TODO*///#define io_specp2fr	io_spectrum
/*TODO*///#define io_specp2sp	io_spectrum
/*TODO*///#define io_specp3sp	io_specpls3
/*TODO*///#define io_specpl3e	io_specpls3
/*TODO*///#define io_specp3es	io_specpls3
/*TODO*///#define io_scorpion	io_specpls3
/*TODO*///#define io_pentagon	io_specpls3

        /*     YEAR  NAME      PARENT    MACHINE   INPUT     INIT  COMPANY		FULLNAME */
        //COMP ( 1982, spectrum, 0,	 spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum" )
        public static GameDriver driver_spectrum = new GameDriver("1982", "spectrum", "spectrum.java", rom_spectrum, null, machine_driver_spectrum, input_ports_spectrum, null, io_spectrum, "Sinclair Research", "ZX Spectrum");
/*TODO*///COMPX( 2000, specpls4, spectrum, spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum +4", GAME_COMPUTER_MODIFIED )
/*TODO*///COMPX( 1994, specbusy, spectrum, spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum (BusySoft Upgrade v1.18)", GAME_COMPUTER_MODIFIED )
/*TODO*///COMPX( ????, specpsch, spectrum, spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum (Maly's Psycho Upgrade)", GAME_COMPUTER_MODIFIED )
/*TODO*///COMPX( ????, specgrot, spectrum, spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum (De Groot's Upgrade)", GAME_COMPUTER_MODIFIED )
/*TODO*///COMPX( 1985, specimc,  spectrum, spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum (Collier's Upgrade)", GAME_COMPUTER_MODIFIED )
/*TODO*///COMPX( 1987, speclec,  spectrum, spectrum, spectrum,	0, "Sinclair Research",	"ZX Spectrum (LEC Upgrade)", GAME_COMPUTER_MODIFIED )
/*TODO*///COMP ( 1986, inves,    spectrum, spectrum, spectrum,	0, "Investronica",	"Inves Spectrum 48K+" )
/*TODO*///COMP ( 1985, tk90x,    spectrum, spectrum, spectrum,	0, "Micro Digital",	"TK-90x Color Computer" )
/*TODO*///COMP ( 1986, tk95,     spectrum, spectrum, spectrum,	0, "Micro Digital",	"TK-95 Color Computer" )
/*TODO*///COMP ( 198?, tc2048,   spectrum, tc2048,   spectrum,	0, "Timex of Portugal",	"TC-2048" )
/*TODO*///COMP ( 1983, ts2068,   spectrum, ts2068,   spectrum,	0, "Timex Sinclair",	"TS-2068" )
/*TODO*///COMP ( 1986, uk2086,   spectrum, uk2086,   spectrum,	0, "Unipolbrit",	"UK-2086 ver. 1.2" )

        //COMPX( 1986, spec128,  0,		 spectrum_128,	 spectrum, 0,			 "Sinclair Research",    "ZX Spectrum 128" ,GAME_NOT_WORKING)
        public static GameDriver driver_spec128 = new GameDriver("1986", "spec128", "spectrum.java", rom_spec128, null, machine_driver_spectrum_128, input_ports_spectrum, null, io_spectrum, "Sinclair Research", "ZX Spectrum 128");
/*TODO*///COMPX( 1985, spec128s, spec128,  spectrum_128,	 spectrum, 0,			 "Sinclair Research",    "ZX Spectrum 128 (Spain)" ,GAME_NOT_WORKING)
        //COMPX( 1986, specpls2, spec128,  spectrum_128,	 spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +2" ,GAME_NOT_WORKING)
        public static GameDriver driver_specpls2 = new GameDriver("1986", "specpls2", "spectrum.java", rom_specpls2, null, machine_driver_spectrum_128, input_ports_spectrum, null, io_spectrum, "Amstrad plc", "ZX Spectrum +2");
/*TODO*///COMPX( 1987, specpl2a, spec128,  spectrum_plus3, spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +2a" ,GAME_NOT_WORKING)
        //COMPX( 1987, specpls3, spec128,  spectrum_plus3, spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +3" ,GAME_NOT_WORKING)
        public static GameDriver driver_specpls3 = new GameDriver("1987", "specpls3", "spectrum.java", rom_specpls3, null, machine_driver_spectrum_plus3, input_ports_spectrum, null, io_specpls3, "Amstrad plc", "ZX Spectrum +3");
/*TODO*///
/*TODO*///COMPX( 1986, specp2fr, spec128,  spectrum_128,	 spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +2 (France)" ,GAME_NOT_WORKING)
/*TODO*///COMPX( 1986, specp2sp, spec128,  spectrum_128,	 spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +2 (Spain)" ,GAME_NOT_WORKING)
/*TODO*///COMPX( 1987, specp3sp, spec128,  spectrum_plus3, spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +3 (Spain)" ,GAME_NOT_WORKING)
/*TODO*///COMPX( 2000, specpl3e, spec128,  spectrum_plus3, spectrum, 0,			 "Amstrad plc",          "ZX Spectrum +3e" , GAME_NOT_WORKING|GAME_COMPUTER_MODIFIED )
/*TODO*///COMPX( 2000, specp3es, spec128,  spectrum_plus3, spectrum, 0,                    "Amstrad plc",          "ZX Spectrum +3e (Spain)" , GAME_NOT_WORKING|GAME_COMPUTER_MODIFIED )
/*TODO*///
/*TODO*///COMPX( ????, scorpion, 0, scorpion,	spectrum, 0,			"Zonov and Co.",		"Zs Scorpion 256", GAME_NOT_WORKING)
        //COMPX( ????, pentagon, spectrum, pentagon,	spectrum, 0,			"???",		"Pentagon", GAME_NOT_WORKING)
        public static GameDriver driver_pentagon = new GameDriver("????", "pentagon", "spectrum.java", rom_pentagon, null, machine_driver_pentagon, input_ports_spectrum, null, io_specpls3, "????", "Pentagon");
}

