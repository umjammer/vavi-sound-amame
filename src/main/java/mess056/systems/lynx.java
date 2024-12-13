/******************************************************************************
 PeT mess@utanet.at 2000,2001

 info found in bastian schick's bll
 and in cc65 for lynx

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.systems;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.video.*;
import static common.ptr.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static consoleflex056.funcPtr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.mame.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mess056.deviceH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.osdependH.*;
import static mame056.usrintrf.ui_text;

import static mess056.machine.lynx.*;
import static mess056.mess.*;
import static mess056.messH.*;
import static mess056.sndhrdw.lynx.*;

public class lynx
{
	
	
	public static int rotate=0;
	public static int lynx_rotate;
	public static int lynx_line_y;
	public static int[] lynx_palette = new int[0x10];
	
	public static Memory_ReadAddress lynx_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xfbff, MRA_RAM ),
		new Memory_ReadAddress( 0xfc00, 0xfcff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xfd00, 0xfdff, MRA_BANK2 ),
		new Memory_ReadAddress( 0xfe00, 0xfff7, MRA_BANK3 ),
		new Memory_ReadAddress( 0xfff8, 0xfff9, MRA_RAM ),
	    new Memory_ReadAddress( 0xfffa, 0xffff, MRA_BANK4 ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress lynx_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xfbff, MWA_RAM ),
		new Memory_WriteAddress( 0xfc00, 0xfcff, MWA_BANK1 ),
		new Memory_WriteAddress( 0xfd00, 0xfdff, MWA_BANK2 ),
		new Memory_WriteAddress( 0xfe00, 0xfff8, MWA_RAM ),
                new Memory_WriteAddress( 0xfff9, 0xfff9, lynx_memory_config ),
                new Memory_WriteAddress( 0xfffa, 0xffff, MWA_RAM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	static InputPortPtr input_ports_lynx = new InputPortPtr(){ public void handler() { 
		PORT_START();
		PORT_BITX( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1, "A", CODE_DEFAULT, CODE_DEFAULT );
		PORT_BITX( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2, "B", CODE_DEFAULT, CODE_DEFAULT );
		PORT_BITX( 0x04, IP_ACTIVE_HIGH, IPT_KEYBOARD, "Opt 2", KEYCODE_2, IP_JOY_DEFAULT );
		PORT_BITX( 0x08, IP_ACTIVE_HIGH, IPT_KEYBOARD, "Opt 1",  KEYCODE_1, IP_JOY_DEFAULT );
	    PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT);
	    PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT );
	    PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN );
	    PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP   );
		PORT_START();
		PORT_BITX( 0x01, IP_ACTIVE_HIGH, IPT_KEYBOARD, "Pause",  KEYCODE_3, IP_JOY_DEFAULT );
		// power on and power off buttons
		PORT_START();
		PORT_DIPNAME ( 0x03, 3, "90 Degree Rotation");
		PORT_DIPSETTING(	2, "counterclockwise" );
		PORT_DIPSETTING(	1, "clockwise" );
		PORT_DIPSETTING(	0, "None" );
		PORT_DIPSETTING(	3, "crcfile" );
	INPUT_PORTS_END(); }}; 
	
	static InterruptPtr lynx_frame_int = new InterruptPtr() {
            public int handler() {
                lynx_rotate=rotate;
	    if ((readinputport(2)&3)!=3) lynx_rotate=readinputport(2)&3;
	    return ignore_interrupt.handler();
            }
        };
	
	public static VhStartPtr lynx_vh_start = new VhStartPtr() {
            public int handler() {
                return 0;
            }
        };
	
	public static VhStopPtr lynx_vh_stop = new VhStopPtr() {
            public void handler() {
            
            }
        };
	
	static String[] debug_strings = new String[16];
        static {
            for (int _i=0 ; _i<16 ; _i++)
                debug_strings[_i] = "";
        }
	static int debug_pos=0;
	/*
	DISPCTL EQU $FD92       ; set to $D by INITMIKEY
	
	; B7..B4        0
	; B3    1 EQU color
	; B2    1 EQU 4 bit mode
	; B1    1 EQU flip screen
	; B0    1 EQU video DMA enabled
	*/
        static int height=-1, width=-1;
	    
	public static void lynx_draw_lines(int newline)
	{
            //System.out.println("lynx_draw_lines");
	    int h,w;
	    int x, yend;
	    int j; // clipping needed!
	    UBytePtr mem=new UBytePtr(memory_region(REGION_CPU1));
	
	    if (osd_skip_this_frame() != 0) newline=-1;
	
	    if (newline==-1) yend=102;
	    else yend=newline;
	
	    if (yend>102) yend=102;
	    if (yend==lynx_line_y) {
		if (newline==-1) lynx_line_y=0;
		return;
	    }
	
	    j=(mikey.data[0x94]|(mikey.data[0x95]<<8))+lynx_line_y*160/2;
	    if ((mikey.data[0x92]&2) != 0) {
		j-=160*102/2-1;
	    }
	
	    if ((lynx_rotate&3) != 0) { // rotation
		h=160; w=102;
		if ( ((lynx_rotate==1)&&((mikey.data[0x92]&2) != 0))
		     ||( (lynx_rotate==2)&&(mikey.data[0x92]&2)==0) ) {
		    for (;lynx_line_y<yend;lynx_line_y++) {
			for (x=160-2;x>=0;j++,x-=2) {
			    plot_pixel.handler(Machine.scrbitmap, lynx_line_y, x+1, lynx_palette[mem.read(j)>>4]);
			    plot_pixel.handler(Machine.scrbitmap, lynx_line_y, x, lynx_palette[mem.read(j)&0xf]);
			}
		    }
		} else {
		    for (;lynx_line_y<yend;lynx_line_y++) {
			for (x=0;x<160;j++,x+=2) {
			    plot_pixel.handler(Machine.scrbitmap, 102-1-lynx_line_y, x, lynx_palette[mem.read(j)>>4]);
			    plot_pixel.handler(Machine.scrbitmap, 102-1-lynx_line_y, x+1, lynx_palette[mem.read(j)&0xf]);
			}
		    }
		}
	    } else {
		w=160; h=102;
		if (( mikey.data[0x92]&2) != 0) {
		    for (;lynx_line_y<yend;lynx_line_y++) {
			for (x=160-2;x>=0;j++,x-=2) {
			    plot_pixel.handler(Machine.scrbitmap, x+1, 102-1-lynx_line_y, lynx_palette[mem.read(j)>>4]);
			    plot_pixel.handler(Machine.scrbitmap, x, 102-1-lynx_line_y, lynx_palette[mem.read(j)&0xf]);
			}
		    }
		} else {
		    for (;lynx_line_y<yend;lynx_line_y++) {
			for (x=0;x<160;j++,x+=2) {
			    plot_pixel.handler(Machine.scrbitmap, x, lynx_line_y, lynx_palette[mem.read(j)>>4]);
			    plot_pixel.handler(Machine.scrbitmap, x+1, lynx_line_y, lynx_palette[mem.read(j)&0xf]);
			}
		    }
		}
	    }
	    if (newline==-1) {
		lynx_line_y=0;
		if ((w!=width)||(h!=height)) {
		    width=w;
		    height=h;
		    osd_set_visible_area(0,width-1,0, height-1);
		}
	    }
	}
	
	public static VhUpdatePtr lynx_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                int j;
	
	    lynx_audio_debug(bitmap);
	
	    for (j=0; j<debug_pos; j++) {
		ui_text(bitmap, debug_strings[j], 0, j*8);
	    }
	    debug_pos=0;
            }
        };
	
	static VhConvertColorPromPtr lynx_init_colors = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
                int i;
                char[] palette = new char[0x1000 * 3];

                for (i=0; i<0x1000; i++) {
                    palette[(i*3) + 0]=(char) ((i&0xf)*16);
                    palette[(i*3) + 1]=(char) (((i&0xf0)>>4)*16);
                    palette[(i*3) + 2]=(char) (((i&0xf00)>>8)*16);
                }

                memcpy (sys_palette, palette, palette.length);
            //	memcpy(sys_colortable,lynx_colortable,sizeof(lynx_colortable));
            }
        };
	
	public static CustomSound_interface lynx_sound_interface = new CustomSound_interface
        (
		lynx_custom_start,
		lynx_custom_stop,
		lynx_custom_update
	);
	
	public static CustomSound_interface lynx2_sound_interface = new CustomSound_interface
        (
		lynx2_custom_start,
		lynx_custom_stop,
		lynx_custom_update
	);
	
	
	static MachineDriver machine_driver_lynx = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M65SC02, // vti core, integrated in vlsi, stz, but not bbr bbs
                                //CPU_M6502, // vti core, integrated in vlsi, stz, but not bbr bbs
				4000000,
				lynx_readmem,lynx_writemem,null,null,
				lynx_frame_int, 1
                        )
		},
		/* frames per second, VBL duration */
		30, DEFAULT_60HZ_VBLANK_DURATION, // lcd!, varies
		1,				/* single CPU */
		lynx_machine_init,
		null,//pc1401_machine_stop,
	
		// 160 x 102
	//	160, 102, { 0, 160 - 1, 0, 102 - 1},
		160, 160, new rectangle( 0, 160 - 1, 0, 102 - 1),
		null, //lynx_gfxdecodeinfo,			   /* graphics decode info */
		// 16 out of 4096
		0x1000,
		0, //sizeof (lynx_colortable) / sizeof(lynx_colortable[0][0]),
		lynx_init_colors,		/* convert color prom */
	
		VIDEO_TYPE_RASTER,	/* video flags */
		null,						/* obsolete */
                lynx_vh_start,
		lynx_vh_stop,
		lynx_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {			
			new MachineSound(
                            SOUND_CUSTOM, 
                            lynx_sound_interface
                        )
                }
                
	);
	
	static MachineDriver machine_driver_lynx2 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M65SC02, // vti core, integrated in vlsi, stz, but not bbr bbs
                                //CPU_M6502, // vti core, integrated in vlsi, stz, but not bbr bbs
				4000000,
				lynx_readmem,lynx_writemem,null,null,
				lynx_frame_int, 1
                        )
		},
		/* frames per second, VBL duration */
		30, DEFAULT_60HZ_VBLANK_DURATION, // lcd!
		1,				/* single CPU */
		lynx_machine_init,
		null,//pc1401_machine_stop,
	
		// 160 x 102
	//	160, 102, { 0, 160 - 1, 0, 102 - 1},
		160, 160, new rectangle( 0, 160 - 1, 0, 102 - 1),
		null, //lynx_gfxdecodeinfo,			   /* graphics decode info */
		// 16 out of 4096
		0x1000,
		0, //sizeof (lynx_colortable) / sizeof(lynx_colortable[0][0]),
		lynx_init_colors,		/* convert color prom */
	
		VIDEO_TYPE_RASTER,	/* video flags */
		null,						/* obsolete */
                lynx_vh_start,
		lynx_vh_stop,
		lynx_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {			
			new MachineSound(
                            SOUND_CUSTOM, 
                            lynx2_sound_interface
                        )
                }
	);
	
	/* these 2 dumps are saved from an running machine,
	   and therefor the rom byte at 0xff09 is not readable!
	   (memory configuration)
	   these 2 dumps differ only in this byte!
	*/
	static RomLoadPtr rom_lynx = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10200,REGION_CPU1, 0);
		ROM_LOAD("lynx.bin", 0x10000, 0x200, 0xe1ffecb6);
		ROM_REGION(0x100,REGION_GFX1, 0);
		ROM_REGION(0x100000, REGION_USER1, 0);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_lynxa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10200,REGION_CPU1, 0);
		ROM_LOAD("lynxa.bin", 0x10000, 0x200, 0x0d973c9d);
		ROM_REGION(0x100,REGION_GFX1, 0);
		ROM_REGION(0x100000, REGION_USER1, 0);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_lynx2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10200,REGION_CPU1, 0);
		ROM_LOAD("lynx2.bin", 0x10000, 0x200, 0x0);
		ROM_REGION(0x100,REGION_GFX1, 0);
		ROM_REGION(0x100000, REGION_USER1, 0);
	ROM_END(); }}; 
	
	/*TODO*///public static int lynx_partialcrc(const unsigned char *buf, int size)
	/*TODO*///{
	/*TODO*///	int crc;
	/*TODO*///
	/*TODO*///	if (size < 65) return 0;
	/*TODO*///	crc = (UINT32) crc32(0L,&buf[64],size-64);
	/*TODO*///	logerror("Lynx Partial CRC: %08lx %ld\n",crc,size);
	/*TODO*///	/* printf("Lynx Partial CRC: %08x %d\n",crc,size); */
	/*TODO*///	return (UINT32)crc;
	/*TODO*///}
	
	public static int lynx_verify_cart (UBytePtr header)
	{
	
		logerror("Trying Header Compare\n");
	
		/*TODO*///if (strncmp("LYNX",&header[0],4) && strncmp("BS9",&header[6],3)) {
		/*TODO*///	logerror("Not an valid Lynx image\n");
		/*TODO*///	return IMAGE_VERIFY_FAIL;
		/*TODO*///}
		logerror("returning ID_OK\n");
		return IMAGE_VERIFY_PASS;
	}
	
	static void lynx_crc_keyword(int io_device, int id)
	{
	    String info;
	    info=device_extrainfo(io_device, id);
	    rotate=0;
	    if (info!=null) {
		if (strcmp(info, "ROTATE90DEGREE")==0) rotate=1;
		else if (strcmp(info, "ROTATE270DEGREE")==0) rotate=2;
	    }
	}
	
	
	static io_initPtr lynx_init_cart = new io_initPtr() {
            public int handler(int id) {
                Object cartfile;
		UBytePtr rom = new UBytePtr(memory_region(REGION_USER1));
		int size;
		UBytePtr header = new UBytePtr(0x40);
	/* 64 byte header
	   LYNX
	   intelword lower counter size
	   0 0 1 0
	   32 chars name
	   22 chars manufacturer
	*/
	
		if (device_filename(IO_CARTSLOT, id) == null)
		{
			return 0;
		}
	
		if ((cartfile = image_fopen(IO_CARTSLOT, id, OSD_FILETYPE_IMAGE_R, 0))==null)
		{
			logerror("%s not found\n",device_filename(IO_CARTSLOT,id));
			return 1;
		}
		size=osd_fsize(cartfile);
		if (osd_fread(cartfile, header, 0x40)!=0x40) {
			logerror("%s load error\n",device_filename(IO_CARTSLOT,id));
			osd_fclose(cartfile);
			return 1;
		}
	
		/* Check the image */
		if (lynx_verify_cart(header) == IMAGE_VERIFY_FAIL)
		{
			osd_fclose(cartfile);
			return INIT_FAIL;
		}
	
		size-=0x40;
		lynx_granularity=header.read(4)|(header.read(5)<<8);
	
		/*TODO*///logerror ("%s %dkb cartridge with %dbyte granularity from %s\n",
		/*TODO*///		  header+10,size/1024,lynx_granularity, header+42);
	
		if (osd_fread(cartfile, rom, size)!=size) {
			logerror("%s load error\n",device_filename(IO_CARTSLOT,id));
			osd_fclose(cartfile);
			return 1;
		}
		osd_fclose(cartfile);
	
		lynx_crc_keyword(IO_CARTSLOT, id);
	
		return 0;
            }
        };
	
	static io_initPtr lynx_quickload = new io_initPtr() {
            public int handler(int id) {
		Object cartfile;
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
		int size;
		UBytePtr header = new UBytePtr(10); // 80 08 dw Start dw Len B S 9 3
		// maybe the first 2 bytes must be used to identify the endianess of the file
		int start;
	
		if (device_filename(IO_QUICKLOAD, id) == null)
		{
			return 0;
		}
	
		if ((cartfile = image_fopen(IO_QUICKLOAD, id, OSD_FILETYPE_IMAGE_R, 0))==null)
		{
			logerror("%s not found\n",device_filename(IO_QUICKLOAD,id));
			return 1;
		}
		size=osd_fsize(cartfile);
	
		if (osd_fread(cartfile, header, 10)!=10) {
			logerror("%s load error\n",device_filename(IO_QUICKLOAD,id));
			osd_fclose(cartfile);
			return 1;
		}
		size-=10;
		start=header.read(3)|(header.read(2)<<8); //! big endian format in file format for little endian cpu
	
		if (osd_fread(cartfile, new UBytePtr(rom, start), size)!=size) {
			logerror("%s load error\n",device_filename(IO_QUICKLOAD,id));
			osd_fclose(cartfile);
			return 1;
		}
		osd_fclose(cartfile);
	
		rom.write(0xfffc+0x200, start&0xff);
		rom.write(0xfffd+0x200, start>>8);
	
		lynx_crc_keyword(IO_QUICKLOAD, id);
	
		return 0;
            }
        };
	
	static IODevice io_lynx[] = {
		new IODevice(
			IO_CARTSLOT,					/* type */
			1,								/* count */
			"lnx\0",                        /* file extensions */
			IO_RESET_ALL,					/* reset if file changed */
			null,
			lynx_init_cart, 				/* init */
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
			null,							/* output_chunk */
			null /*TODO*///lynx_partialcrc                                         /* partial crc */
		),
		new IODevice(
			IO_QUICKLOAD,					/* type */
			1,								/* count */
			"o\0",                        /* file extensions */
			IO_RESET_ALL,					/* reset if file changed */
			null,
			lynx_quickload, 				/* init */
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
	
	public static void init_lynx()
	{
		int i;
		UBytePtr gfx=new UBytePtr(memory_region(REGION_GFX1));
	
		for (i=0; i<256; i++) gfx.write(i, i);
	
		lynx_quickload.handler(0);
	
	
	}
	
	//#define io_lynxa io_lynx
	//#define io_lynx2 io_lynx
	
	/*    YEAR  NAME      PARENT    MACHINE   INPUT     INIT      MONITOR	COMPANY   FULLNAME */
	//CONSX( 1989, lynx,	  0, 		lynx,  lynx, 	lynx,	  "Atari",  "Lynx", GAME_NOT_WORKING|GAME_IMPERFECT_SOUND)
        public static GameDriver driver_lynx = new GameDriver("1989", "lynx", "lynx.java", rom_lynx, null, machine_driver_lynx, input_ports_lynx, null, io_lynx, "Atari", "Lynx");
	//CONSX( 1989, lynxa,	  lynx, 	lynx,  lynx, 	lynx,	  "Atari",  "Lynx (alternate rom save!)", GAME_NOT_WORKING|GAME_IMPERFECT_SOUND)
        public static GameDriver driver_lynxa = new GameDriver("1989", "lynxa", "lynx.java", rom_lynxa, null, machine_driver_lynx, input_ports_lynx, null, io_lynx, "Atari", "Lynx (alternate rom save!)");
	//CONSX( 1991, lynx2,	  lynx, 	lynx2,  lynx, 	lynx,	  "Atari",  "Lynx II", GAME_NOT_WORKING|GAME_IMPERFECT_SOUND)
        public static GameDriver driver_lynx2 = new GameDriver("1991", "lynx2", "lynx.java", rom_lynx2, null, machine_driver_lynx2, input_ports_lynx, null, io_lynx, "Atari", "Lynx II");
	
	/*TODO*///#ifdef RUNTIME_LOADER
	/*TODO*///extern void lynx_runtime_loader_init(void)
	/*TODO*///{
	/*TODO*///	int i;
	/*TODO*///	for (i=0; drivers[i]; i++) {
	/*TODO*///		if ( strcmp(drivers[i].name,"lynx")==0) drivers[i]=&driver_lynx;
	/*TODO*///		if ( strcmp(drivers[i].name,"lynxa")==0) drivers[i]=&driver_lynxa;
	/*TODO*///		if ( strcmp(drivers[i].name,"lynx2")==0) drivers[i]=&driver_lynx2;
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///#endif
	
}
