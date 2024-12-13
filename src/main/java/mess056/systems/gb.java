/***************************************************************************

  gb.c

  Driver file to handle emulation of the Nintendo Gameboy.
  By:

  Hans de Goede               1998

  Todo list:
  Done entries kept for historical reasons, besides that it's nice to see
  what is already done instead of what has to be done.

Priority:  Todo:                                                  Done:
  2        Replace Marat's  vidhrdw/gb.c  by Playboy code           *
  2        Clean & speed up vidhrdw/gb.c                            *
  2        Replace Marat's  Z80gb/Z80gb.c by Playboy code           *
  2        Transform Playboys Z80gb.c to big case method            *
  2        Clean up Z80gb.c                                         *
  2        Fix / optimise halt instruction                          *
  2        Do correct lcd stat timing
  2        Generate lcd stat interrupts
  2        Replace Marat's code in machine/gb.c by Playboy code
  1        Check, and fix if needed flags bug which troubles ffa
  1        Save/restore battery backed ram
           (urgent needed to play zelda ;)
  1        Add sound
  0        Add supergb support
  0        Add palette editting, save & restore
  0        Add somekind of backdrop support
  0        Speedups if remotly possible

  2 = has to be done before first public release
  1 = should be added later on
  0 = bells and whistles

***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.systems;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.commonH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.memoryH.*;
import static mame056.palette.*;
import static mame056.sndintrfH.*;
import static mame056.vidhrdw.generic.*;
import static mess056.deviceH.*;
import static mess056.machine.gb.*;
import static mess056.messH.*;
import static mess056.sndhrdw.gb.*;
import static mess056.vidhrdw.gb.*;

public class gb
{
	
	static Memory_ReadAddress readmem[] = {
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress(0x0000, 0x3fff, MRA_ROM ),			/* 16k fixed ROM BANK #0*/
            new Memory_ReadAddress(0x4000, 0x7fff, MRA_BANK1 ),			/* 16k switched ROM bank */
            new Memory_ReadAddress(0x8000, 0x9fff, MRA_RAM ),			/* 8k video ram */
            new Memory_ReadAddress(0xa000, 0xbfff, MRA_BANK2 ),			/* 8k switched RAM bank (on cartridge) */
            new Memory_ReadAddress(0xc000, 0xfe9f, MRA_RAM ),			/* internal ram + echo + sprite Ram & IO */
            new Memory_ReadAddress(0xfea0, 0xfeff, MRA_NOP ),			/* Unusable */
            new Memory_ReadAddress(0xff00, 0xff7f, gb_r_io ),			/* gb io */
            new Memory_ReadAddress(0xff80, 0xffff, MRA_RAM ),			/* plain ram (high) */            
            new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */
        };
	
	static Memory_WriteAddress writemem[] = { 
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress( 0x0000, 0x1fff, MWA_ROM ),            /* plain rom */
            new Memory_WriteAddress( 0x2000, 0x3fff, gb_rom_bank_select ), /* rom bank select */
            new Memory_WriteAddress( 0x4000, 0x5fff, gb_ram_bank_select ), /* ram bank select */
            new Memory_WriteAddress( 0x6000, 0x7fff, gb_mem_mode_select ),            /* plain rom */
            new Memory_WriteAddress( 0x8000, 0x9fff, MWA_RAM ),            /* plain ram */
            new Memory_WriteAddress( 0xa000, 0xbfff, MWA_BANK2 ),          /* banked (cartridge) ram */
            new Memory_WriteAddress( 0xc000, 0xfe9f, MWA_RAM, videoram, videoram_size ), /* video & sprite ram */
            new Memory_WriteAddress( 0xfea0, 0xfeff, MWA_NOP ),
            new Memory_WriteAddress( 0xff00, 0xff7f, gb_w_io ),			/* gb io */
            new Memory_WriteAddress( 0xff80, 0xfffe, MWA_RAM ),			/* plain ram (high) */
            new Memory_WriteAddress( 0xffff, 0xffff, gb_w_ie ),			/* gb io (interrupt enable) */
            
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */
            
        };
        
        static Memory_WriteAddress sgb_writemem[] = { 
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress( 0x0000, 0x1fff, MWA_ROM ),			/* 8k ROM (should really be RAM enable */
            new Memory_WriteAddress( 0x2000, 0x3fff, gb_rom_bank_select ),	/* ROM bank select */
            new Memory_WriteAddress( 0x4000, 0x5fff, gb_ram_bank_select ),	/* RAM bank select */
            new Memory_WriteAddress( 0x6000, 0x7fff, gb_mem_mode_select ),	/* RAM/ROM mode select */
            new Memory_WriteAddress( 0x8000, 0x9fff, MWA_RAM ),			/* 8k VRAM */
            new Memory_WriteAddress( 0xa000, 0xbfff, MWA_BANK2 ),			/* 8k switched RAM bank (on cartridge) */
            new Memory_WriteAddress( 0xc000, 0xfe9f, MWA_RAM ),			/* 8k low RAM, echo RAM, OAM RAM */
            new Memory_WriteAddress( 0xfea0, 0xfeff, MWA_NOP ),			/* unusable */
            new Memory_WriteAddress( 0xff00, 0xff7f, sgb_w_io ),			/* sgb io */
            new Memory_WriteAddress( 0xff80, 0xfffe, MWA_RAM ),			/* 127b high RAM */
            new Memory_WriteAddress( 0xffff, 0xffff, gb_w_ie ),			/* gb io (interrupt enable) */
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */            
        };

    static Memory_ReadAddress gbc_readmem[] = {
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),			/* 16k fixed ROM bank */
            new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK1 ),			/* 16k switched ROM bank */
            new Memory_ReadAddress( 0x8000, 0x9fff, MRA_BANK4 ),			/* 8k switched VRAM bank */
            new Memory_ReadAddress( 0xa000, 0xbfff, MRA_BANK2 ),			/* 8k switched RAM bank (on cartridge) */
            new Memory_ReadAddress( 0xc000, 0xcfff, MRA_RAM ),			/* 4k fixed RAM bank */
            new Memory_ReadAddress( 0xd000, 0xdfff, MRA_BANK3 ),			/* 4k switched RAM bank */
            new Memory_ReadAddress( 0xe000, 0xfe9f, MRA_RAM ),			/* echo RAM, OAM RAM */
            new Memory_ReadAddress( 0xfea0, 0xfeff, MRA_NOP ),			/* unusable */
            new Memory_ReadAddress( 0xff00, 0xff7f, gb_r_io ),			/* gb io */
            new Memory_ReadAddress( 0xff80, 0xffff, MRA_RAM ),			/* 127 bytes high RAM, interrupt enable io */
            new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */
        };

    static Memory_WriteAddress gbc_writemem[] = { 
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress( 0x0000, 0x1fff, MWA_ROM ),			/* 8k ROM (should really be RAM enable */
            new Memory_WriteAddress( 0x2000, 0x3fff, gb_rom_bank_select ),	/* ROM bank select */
            new Memory_WriteAddress( 0x4000, 0x5fff, gb_ram_bank_select ),	/* RAM bank select */
            new Memory_WriteAddress( 0x6000, 0x7fff, gb_mem_mode_select ),	/* RAM/ROM mode select */
            new Memory_WriteAddress( 0x8000, 0x9fff, MWA_BANK4 ),			/* 8k switched VRAM bank */
            new Memory_WriteAddress( 0xa000, 0xbfff, MWA_BANK2 ),			/* 8k switched RAM bank (on cartridge) */
            new Memory_WriteAddress( 0xc000, 0xcfff, MWA_RAM ),			/* 4k fixed RAM bank */
            new Memory_WriteAddress( 0xd000, 0xdfff, MWA_BANK3 ),			/* 4k switched RAM bank */
            new Memory_WriteAddress( 0xe000, 0xfeff, MWA_RAM ),			/* echo RAM, OAM RAM */
    //{ 0xe000, 0xfeff, MWA_RAM/*, &videoram, &videoram_size*/ }, /* video & sprite ram */
            new Memory_WriteAddress( 0xff00, 0xff7f, gbc_w_io ),			/* gbc io */
            new Memory_WriteAddress( 0xff80, 0xfffe, MWA_RAM ),			/* 127b high RAM */
            new Memory_WriteAddress( 0xffff, 0xffff, gb_w_ie ),			/* gb io (interrupt enable) */
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */            
        };

        
        static GfxLayout gb_charlayout = new GfxLayout(
		8,8,
		256,
		1,						/* 1 bits per pixel */
	
		new int[] { 0 },					/* no bitplanes; 1 bit per pixel */
	
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0, 8*256, 16*256, 24*256, 32*256, 40*256, 48*256, 56*256 },
	
		8				/* every char takes 1 consecutive byte */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
            /*new GfxDecodeInfo( 0, 0x0, gb_charlayout, 0, 0x80 ),
		new GfxDecodeInfo( 0, 0x0, gb_charlayout, 0, 0x80 ),
		new GfxDecodeInfo( 0, 0x0, gb_charlayout, 0, 0x80 ),*/
		new GfxDecodeInfo( -1 )
	};
	
	static InputPortPtr input_ports_gameboy = new InputPortPtr(){ public void handler() { 
		PORT_START();	/* IN0 */
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT);
                PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
                PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP   );
                PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );
                PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1       );
                PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2       );
		/*PORT_BITX( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN, "Select", KEYCODE_LSHIFT, IP_JOY_DEFAULT );*/
		/*PORT_BITX( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN, "Start",  KEYCODE_Z,      IP_JOY_DEFAULT );*/
		PORT_BITX( 0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "Select", KEYCODE_5, IP_JOY_DEFAULT );
		PORT_BITX( 0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "Start",  KEYCODE_1, IP_JOY_DEFAULT );
	INPUT_PORTS_END(); }}; 
	
	static char palette[] =
	{
            /*0xFF,0xFF,0xFF,
            0x00,0x00,0x00,*/
            /*0xFF,0xFB,0x87,
            0xB1,0xAE,0x4E,
            0x84,0x80,0x4E,
            0x4E,0x4E,0x4E*/
            /*248, 224, 136,
            216, 176, 88,
            152, 120, 56,
            72, 56, 24 */
            0xFF,0xFB,0x87,
            0xB1,0xAE,0x4E,
            0x84,0x80,0x4E,
            0x4E,0x4E,0x4E
	};
	
	static char colortable[] = {
            0,1,2,3,
            0,1,2,3,
	    0,1,2,3,    /* Background colours */
	    0,1,2,3,    /* Sprite 0 colours */
	    0,1,2,3,    /* Sprite 1 colours */
	    0,1,2,3,    /* Window colours */
	};
	
	/* Initialise the palette */
	static VhConvertColorPromPtr gb_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
                memcpy(sys_palette,palette,palette.length);
		memcpy(sys_colortable,colortable,colortable.length);
            }
        };
        
        static VhConvertColorPromPtr sgb_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i, r, g, b;

                for( i = 0; i < 32768; i++ )
                {
                        r = (i & 0x1F) << 3;
                        g = ((i >> 5) & 0x1F) << 3;
                        b = ((i >> 10) & 0x1F) << 3;
                        palette_set_color( i, r, g, b );
                }

                /* Some default colours for non-SGB games */
                colortable[0] = 32767;
                colortable[1] = 21140;
                colortable[2] = 10570;
                colortable[3] = 0;
                /* The rest of the colortable can be black */
                for( i = 4; i < 8*16; i++ )
                        colortable[i] = 0;
            }
        };
        
        static VhConvertColorPromPtr gbc_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i, r, g, b;

                for( i = 0; i < 32768; i++ )
                {
                        r = (i & 0x1F) << 3;
                        g = ((i >> 5) & 0x1F) << 3;
                        b = ((i >> 10) & 0x1F) << 3;
                        palette_set_color( i, r, g, b );
                }

                /* Background is initialised as white */
                for( i = 0; i < 8*4; i++ )
                        colortable[i] = 32767;
                /* Sprites are supposed to be uninitialized, but we'll make them black */
                for( i = 8*4; i < 16*4; i++ )
                        colortable[i] = 0;
            }
        };

	
	static CustomSound_interface gameboy_sound_interface = new CustomSound_interface
        (
		gameboy_sh_start,
		null,
		null
	);
	
	static MachineDriver machine_driver_gameboy = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80GB,
				4194304,	  /* 4.194304 Mhz */
				readmem,writemem,null,null,
				gb_scanline_interrupt, 154 *3 /* 1 int each scanline ! */
			)
		},
		60, 0,	/* frames per second, vblank duration */
		1,
		machine_init_gb,
		gb_shutdown_machine,	/* shutdown machine */
	
		/* video hardware (double size) */
		160, 144,
		new rectangle( 0, 160-1, 0, 144-1 ),
		gfxdecodeinfo,
		palette.length/3,
		16,
		gb_init_palette,				/* init palette */
	
		VIDEO_TYPE_RASTER,
		null,
		generic_bitmapped_vh_start,					/* vh_start */
                generic_bitmapped_vh_stop,                     /* vh_stop */
		generic_bitmapped_vh_screenrefresh, //gb_vh_screen_refresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_CUSTOM, gameboy_sound_interface )
		}
	);
        
        static MachineDriver machine_driver_supergb = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80GB,
				4295454,	  /* 4.194304 Mhz */
				readmem,sgb_writemem,null,null,
				gb_scanline_interrupt, 154 *3 /* 1 int each scanline ! */
			)
		},
		60, 0,	/* frames per second, vblank duration */
		1,
		machine_init_sgb,
		gb_shutdown_machine,	/* shutdown machine */
	
		/* video hardware (double size) */
		32*8, 28*8,
		new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),
		gfxdecodeinfo,
		32768,
		8*16, /* 8 palettes of 16 colours */
		sgb_init_palette,				/* init palette */
	
		VIDEO_TYPE_RASTER,
		null,
		generic_bitmapped_vh_start,					/* vh_start */
                generic_bitmapped_vh_stop,                     /* vh_stop */
		generic_bitmapped_vh_screenrefresh, //gb_vh_screen_refresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_CUSTOM, gameboy_sound_interface )
		}
	);

        static MachineDriver machine_driver_gbcolor = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80GB,
				4194304,	  /* 4.194304 Mhz */
				gbc_readmem, gbc_writemem,null,null,
				gb_scanline_interrupt, 154 *3 /* 1 int each scanline ! */
			)
		},
		60, 0,	/* frames per second, vblank duration */
		1,
		machine_init_gbc,
		gb_shutdown_machine,	/* shutdown machine */
                /* MDRV_CPU_CONFIG(gbc_cpu_af_reset) */
		/* video hardware (double size) */
		160, 144,
		new rectangle( 0, 160-1, 0, 144-1 ),
		gfxdecodeinfo,
		32768,
		16*4, /* 16 palettes of 4 colours */
		gbc_init_palette,				/* init palette */
	
		VIDEO_TYPE_RASTER,
		null,
		generic_bitmapped_vh_start,					/* vh_start */
                generic_bitmapped_vh_stop,                     /* vh_stop */
		generic_bitmapped_vh_screenrefresh, //gb_vh_screen_refresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_CUSTOM, gameboy_sound_interface )
		}
	);
	
	static IODevice io_gameboy[] = {
            new IODevice(
			IO_CARTSLOT,		/* type */
			1,					/* count */
			"gb\0gmb\0cgb\0gbc\0sgb\0",		/* file extensions */
			IO_RESET_ALL,		/* reset if file changed */
                        null,
			gb_load_rom,		/* init */
			null,				/* exit */
			null,				/* info */
			null,               /* open */
			null,               /* close */
			null,               /* status */
			null,               /* seek */
			null,				/* tell */
                        null,               /* input */
			null,               /* output */
			null,               /* input_chunk */
			null                /* output_chunk */
                ),
		new IODevice(IO_END)
            };
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_gameboy = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10000,REGION_CPU1,0);
	ROM_END(); }}; 
        
        static RomLoadPtr rom_supergb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10000,REGION_CPU1,0);
                ROM_REGION( 0x2000,  REGION_GFX1, 0 );	/* SGB border */
        ROM_END(); }}; 

        static RomLoadPtr rom_gbcolor = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10000,REGION_CPU1,0);
	ROM_END(); }};
	
	/*     YEAR  NAME      PARENT    MACHINE   INPUT     INIT      COMPANY   FULLNAME */
	//CONSX( 1990, gameboy,  0,		 gameboy,  gameboy,  0,		   "Nintendo", "GameBoy", GAME_NO_SOUND )
	public static GameDriver driver_gameboy = new GameDriver("1990", "gameboy", "gameboy.java", rom_gameboy, null, machine_driver_gameboy, input_ports_gameboy, null, io_gameboy, "Nintendo", "GameBoy");
        //CONSX( 1994, supergb, gameboy, supergb, gameboy, 0,    "Nintendo", "Super GameBoy", GAME_IMPERFECT_SOUND )
        public static GameDriver driver_supergb = new GameDriver("1994", "supergb", "gameboy.java", rom_supergb, null, machine_driver_supergb, input_ports_gameboy, null, io_gameboy, "Nintendo", "Super GameBoy");
        //CONSX( 1998, gbcolor, gameboy, gbcolor, gameboy, 0,    "Nintendo", "GameBoy Color", GAME_IMPERFECT_SOUND )
        public static GameDriver driver_gbcolor = new GameDriver("1998", "gbcolor", "gameboy.java", rom_gbcolor, null, machine_driver_gbcolor, input_ports_gameboy, null, io_gameboy, "Nintendo", "GameBoy Color");
}
