/******************************************************************************
 watara supervision handheld

 PeT mess@utanet.at in december 2000
******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.systems;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.libc.cstdio.*;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static common.libc.cstring.*;
import static consoleflex056.funcPtr.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.input.*;
import static mame056.inputH.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.osdependH.*;
import static mame056.sndintrfH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.deviceH.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.sndhrdw.svision.*;

public class svision {
	
	
	/*
	supervision
	watara
	
	cartridge code is m65c02 or something more (65ce02?)
	
	
	
	4 mhz quartz
	
	right dil28 ram? 8kb?
	left dil28 ???
	
	integrated speaker
	stereo phone jack
	40 pin connector for cartridges
	com port (9 pol dsub) pc at rs232?
	looked at
	5 4 3 2 1
	 9 8 7 6
	2 black -.vlsi
	3 brown -.vlsi
	4 yellow -.vlsi
	5 red vlsi
	7 violett
	9 white
	
	port for 6V power supply
	
	on/off switch
	volume control analog
	contrast control analog
	
	
	
	
	cartridge connector (look at the cartridge)
	 /oe or /ce	1  40 +5v (picture side)
			a0  2  39 nc
			a1  3  38 nc
			a2  4  37 nc
			a3  5  36 nc
			a4  6  35 nc in crystball
			a5  7  34 d0
			a6  8  33 d1
			a7  9  32 d2
			a8  10 31 d3
			a9  11 30 d4
			a10 12 29 d5
			a11 13 28 d6
			a12 14 27 d7
	        a13 15 26 nc
	        a14 16 25 nc
	        a15 17 24 nc
	        a16?18 23 nc
	        a17?19 22 gnd connected with 21 in crystalball
	        a18?20 21 (shorter pin in crystalball)
	
	adapter for dumping as 27c4001
	
	cryst ball:
	a16,a17,a18 not connected
	
	delta hero:
	a16,a17,a18 not connected
	
	
	ordering of pins in the cartridge!
	21,22 connected
	idea: it is a 27512, and pin are in this ordering
	
	+5V 40
	a15 17
	a12 14
	a7   9
	a6   8
	a5   7
	a4   6
	a3   5
	a2   4
	a1   3
	a0   2
	d0  34
	d1  33
	d2  32
	gnd 21!
	d3  31
	d4  30
	d5  29
	d6  28
	d7  27
	ce  21 (gnd)
	a10 12
	oe  1
	a11 13
	a9  11
	a8  10
	a13 15
	a14 16
	*/
	
	// in pixel
	public static int XPOS(){ 
            return svision_reg.read(2);
        }
	
	static UBytePtr svision_reg=new UBytePtr();
	/*
	  0x2000 0xa0 something to do with video dma?
	  0x2001 0xa0 something to do with video dma?
	  0x2010,11,12 audio channel
	   offset 0,1 frequency; offset 1 always zero?
	   offset 2:
	    0, 0x60-0x6f
	    bit 0..3: volume??
	    bit 5: on left??
	    bit 6: on right??
	  0x2014,15,16 audio channel
	  0x2020 buttons and pad
	  0x2022 0x0f ?
	  0x2023 timer?
	   next interrupt at 256*value?
	   writing sets timer and clear interrupt request?
	   fast irq in crystball needed for timing
	   slower irq in deltahero with music?
	  0x2026 bank switching
	  0x2027
	   bit 0: 0x2023 timer interrupt occured
	
	  0x2041-0x2053
	  0x3041-
	 */
	
	public static class _svision {
	    public timer_entry timer1;
	    public boolean timer1_shot = false;
	};

        public static _svision svision = new _svision();
        
	static timer_callback svision_timer = new timer_callback() {
            public void handler(int param) {
                svision.timer1_shot=true;
	    svision.timer1=null;
	    cpu_set_irq_line(0, M6502_IRQ_LINE, ASSERT_LINE);
            }
        };
	
	public static ReadHandlerPtr svision_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int data=svision_reg.read(offset);
	    switch (offset) {
	    case 0x20:
		data=readinputport(0);
		break;
	    case 0x27:
		if (svision.timer1_shot) data|=1; //crystball irq routine
		break;
	    case 0x24: case 0x25://deltahero irq routine read
		break;
	    default:
		logerror("%.6f svision read %04x %02x\n",timer_get_time(),offset,data);
		break;
	    }
	
	    return data;
            }
        };
	
	
	public static WriteHandlerPtr svision_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                svision_reg.write(offset, data);
                switch (offset) {
                case 0x26: // bits 5,6 memory management for a000?
                    cpu_setbank(1, new UBytePtr(memory_region(REGION_CPU1), 0x10000+((data&0x60)<<9) ));
                    break;
                case 0x23: //delta hero irq routine write
                    cpu_set_irq_line(0, M65C02_IRQ_LINE, CLEAR_LINE);
                    svision.timer1_shot=false;
                    if (svision.timer1 != null)
                        timer_reset(svision.timer1, TIME_IN_CYCLES(data*256, 0));
                    else
                        svision.timer1=timer_set(TIME_IN_CYCLES(data*256, 0),0,svision_timer);
                    break;
                case 0x10: case 0x11: case 0x12:
                    svision_soundport_w(svision_channel[0], offset&3, data);
                    break;
                case 0x14: case 0x15: case 0x16:
                    svision_soundport_w(svision_channel[1], offset&3, data);
                    break;
                default:
                    logerror("%.6f svision write %04x %02x\n",timer_get_time(),offset,data);
                }
            }
        };


	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
                new Memory_ReadAddress( 0x0000, 0x1fff, MRA_RAM ),
                new Memory_ReadAddress( 0x2000, 0x3fff, svision_r ),
                new Memory_ReadAddress( 0x4000, 0x5fff, MRA_RAM ), //?
		new Memory_ReadAddress( 0x6000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc000, 0xffff, MRA_BANK2 ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
        
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
                new Memory_WriteAddress( 0x0000, 0x1fff, MWA_RAM ),
                new Memory_WriteAddress( 0x2000, 0x3fff, svision_w, svision_reg ),
		new Memory_WriteAddress( 0x4000, 0x5fff, MWA_RAM ),
		new Memory_WriteAddress( 0x6000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
        
	static InputPortPtr input_ports_svision = new InputPortPtr(){ public void handler() { 
            PORT_START();
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT);
            PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
            PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );
            PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP   );
            PORT_BITX( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2, "B", CODE_DEFAULT, CODE_DEFAULT );
            PORT_BITX( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1, "A", CODE_DEFAULT, CODE_DEFAULT );
            PORT_BITX( 0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "select", KEYCODE_5, IP_JOY_DEFAULT );
            PORT_BITX( 0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "start/pause",  KEYCODE_1, IP_JOY_DEFAULT );
            INPUT_PORTS_END(); }}; 
	
	/* most games contain their graphics in roms, and have hardware to
	   draw complete rectangular objects */
	/* look into src/drawgfx.h for more info */
	/* this is for a console with monochrom hires graphics in ram
	   1 byte/ 8 pixels are enlarged */
	static GfxLayout svision_charlayout = new GfxLayout
	(
		8,	/* width of object */
		1,	/* height of object */
		256,/* 256 characters */
		2,	/* bits per pixel */
		new int[] { 0,1 }, /* no bitplanes */
		/* x offsets */
		new int[] { 6,4,2,0,0,0,0,0 },
		/* y offsets */
		new int[] { 0,0,0,0,0,0,0,0 },
		8*1 /* size of 1 object in bits */
	);
	
	static GfxDecodeInfo svision_gfxdecodeinfo[] ={
		new GfxDecodeInfo(
			REGION_GFX1, /* memory region */
			0x0000, /* offset in memory region */
			svision_charlayout,
			0, /* index in the color lookup table where color codes start */
			1  /* total number of color codes */
		),
	    new GfxDecodeInfo( -1 ) /* end of array */
	};
        
	/* palette in red, green, blue tribles */
	static char svision_palette[] =
	{
            0x00, 0x00, 0x00,
            0xff, 0xff, 0xff,
/*TODO*///	#if 0
/*TODO*///	    // greens grabbed from a scan of a handheld
/*TODO*///	    // in its best adjustment for contrast
/* 53, 73, 42 ,
 42, 64, 47 ,
 22, 42, 51 ,
 22, 25, 32 */
/*TODO*///	#else
		// grabbed from chris covell's black white pics
		 0xe0, 0xe0, 0xe0 ,
		 0xb9, 0xb9, 0xb9 ,
		 0x54, 0x54, 0x54 ,
		 0x12, 0x12, 0x12 

/*TODO*///	#endif
	};
	
	/* color table for 1 2 color objects */
	static char svision_colortable[] = {
		 0, 1, 2, 3 
	};
	
	static VhConvertColorPromPtr svision_init_colors = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
		memcpy(sys_palette, svision_palette, svision_palette.length);
		memcpy(sys_colortable, svision_colortable, svision_colortable.length);
            }
        };

	static VhUpdatePtr svision_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                int x, y;
                UBytePtr vram = new UBytePtr(memory_region(REGION_CPU1), 0x4000 + (svision_reg.read(2) / 4));
                UBytePtr vram_line;
                UShortPtr line;
                int b;

                for (y = 0; y < 160; y++)
                {
                        line = new UShortPtr(bitmap.line[y]);
                        vram_line = new UBytePtr(vram, y * 0x30);

                        for (x = 0; x < 160; x += 4)
                        {
                                b = vram_line.readinc();
                                //System.out.println(b);
                                line.write(3, (char) (((b >> 6) & 0x03) + 2));
                                line.write(2, (char) (((b >> 4) & 0x03) + 2));
                                line.write(1, (char) (((b >> 2) & 0x03) + 2));
                                line.write(0, (char) (((b >> 0) & 0x03) + 2));
                                line.inc(4);
                        }
                }
            }
        };

	static InterruptPtr svision_frame_int = new InterruptPtr() {
            public int handler() {
                cpu_set_nmi_line(0, PULSE_LINE);
		return ignore_interrupt.handler();
            }
        };
        
        
	
	static void init_svision()
	{
		UBytePtr gfx=new UBytePtr(memory_region(REGION_GFX1));
		int i;
	
		for (i=0; i<256;i++) gfx.write(i, i);
	}
	
	static InitMachinePtr svision_reset = new InitMachinePtr() {
            public void handler() {
                svision.timer1_shot = false;
                //svision_update_banks();
            }
        };
	
	
	static CustomSound_interface svision_sound_interface = new CustomSound_interface
        (
		svision_custom_start,
		null, //svision_custom_stop,
		svision_custom_update
	);
        
        static MachineDriver machine_driver_svision = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M65C02, //? stz used!
                                //CPU_M6502, //? stz used!
				4000000, //?
				readmem, writemem,
				null,null,
				svision_frame_int,1
                        )
		},
		/* frames per second, VBL duration */
		60, DEFAULT_60HZ_VBLANK_DURATION, // based on crystball sound speed!
		1, /* single CPU */
		svision_reset,//stub_machine_init,
		null,//stub_machine_stop,
		160, 160, /* width and height of screen and allocated sizes */
		new rectangle( 0, 160 - 1, 0, 160 - 1), /* left, right, top, bottom of visible area */
		svision_gfxdecodeinfo,			   /* graphics decode info */
		svision_palette.length/3,
		svision_colortable.length,
		svision_init_colors,		/* convert color prom */
	
		VIDEO_TYPE_RASTER,	/* lcd */
		null,						/* obsolete */
		null,// generic_vh_start,
		null,//generic_vh_stop,
		svision_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
                new MachineSound[] {			
			new MachineSound(
                            SOUND_CUSTOM, 
                            svision_sound_interface
                        )
                }
                
            );
	
	static RomLoadPtr rom_svision = new RomLoadPtr(){ 
            public void handler(){ 
		ROM_REGION(0x20000,REGION_CPU1, 0);
                ROM_REGION(0x100,REGION_GFX1, 0);
                ROM_END(); 
            }
        }; 
	
	/* deltahero
	 c000
	  dd6a clear 0x2000 at ($57/58) (0x4000)
	  deb6 clear hardware regs
	   e35d clear hardware reg
	   e361 clear hardware reg
	  e3a4
	 c200
	
	 nmi c053 ?
	 irq c109
	      e3f7
	      def4
	 routines:
	 dd6a clear 0x2000 at ($57/58) (0x4000)
	 */
	
	static io_initPtr svision_load_rom = new io_initPtr() {
            public int handler(int id) {
                
                Object cartfile;
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
		int size;
	
		if (device_filename(IO_CARTSLOT, id) == null)
		{
			printf("%s requires Cartridge!\n", Machine.gamedrv.name);
			return 0;
		}
	
		if ((cartfile = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0))==null)
		{
			logerror("%s not found\n",device_filename(IO_CARTSLOT,id));
			return 1;
		}
		size=osd_fsize(cartfile);
		if (size>0x10000) {
		    logerror("%s: size %d not yet supported\n",device_filename(IO_CARTSLOT,id), size);
		    return 1;
		}
	
		if (osd_fread(cartfile, new UBytePtr(rom, 0x20000-size), size)!=size) {
			logerror("%s load error\n",device_filename(IO_CARTSLOT,id));
			osd_fclose(cartfile);
			return 1;
		}
		if (size==0x8000) {
		    memcpy(new UBytePtr(rom, 0x10000), new UBytePtr(rom, 0x20000-size), size);
		}
		memcpy(new UBytePtr(rom, 0xc000), new UBytePtr(rom, 0x1c000), 0x10000-0xc000);
		osd_fclose(cartfile);
		return 0;
            }
        };

	
	static  IODevice io_svision[] = {
		new IODevice(
			IO_CARTSLOT,					/* type */
			1,								/* count */
			"bin\0",                        /* file extensions */
			IO_RESET_ALL,					/* reset if file changed */
			null,
			svision_load_rom,                                       /* init */
			null,							/* exit */
			null,							/* info */
			null,							/* open */
			null,							/* close */
			null,							/* status */
			null,							/* seek */
			null,							/* tell */
			null,							/* input */
			null,							/* output */
			null,							/* input_chunk */
			null							/* output_chunk */
		),
                new IODevice( IO_END )
	};
	
	/*    YEAR      NAME            PARENT  MACHINE   INPUT     INIT
		  COMPANY                 FULLNAME */
//	CONSX( 1992, svision,       0,          svision,  svision,    svision,   "Watara", "Super Vision", GAME_IMPERFECT_SOUND)
        public static GameDriver driver_svision = new GameDriver("1992", "svision", "svision.java", rom_svision, null, machine_driver_svision, input_ports_svision, null, io_svision, "Watara", "Super Vision");
/*TODO*///	// marketed under a ton of firms and names
/*TODO*///	
/*TODO*///	#ifdef RUNTIME_LOADER
/*TODO*///	extern void svision_runtime_loader_init(void)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		for (i=0; drivers[i]; i++) {
/*TODO*///			if ( strcmp(drivers[i].name,"svision")==0) drivers[i]=&driver_svision;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	#endif    
}
