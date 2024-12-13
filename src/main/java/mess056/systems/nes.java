/***************************************************************************

  nes.c

  Driver file to handle emulation of the Nintendo Entertainment System (Famicom).

  MESS driver by Brad Oliver (bradman@pobox.com), NES sound code by Matt Conte.
  Based in part on the old xNes code, by Nicolas Hamel, Chuck Mason, Brad Oliver,
  Richard Bannister and Jeff Mitchell.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.systems;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.sndintrfH.*;
import static mame056.sound.nes_apu.NESPSG_0_w;

import static mess056.deviceH.*;
import static mess056.messH.*;
import static mess056.includes.nesH.*;
import static mess056.machine.nes.*;
import static mess056.machine.nes_mmc.*;
import static mess056.vidhrdw.nes.*;

import static mame056.sound.nes_apuH.*;

public class nes
{
	
	public static UBytePtr battery_ram = new UBytePtr();
	public static UBytePtr main_ram = new UBytePtr();
	
	public static ReadHandlerPtr nes_mirrorram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return main_ram.read(offset & 0x7ff);
            }
        };
	
	public static WriteHandlerPtr nes_mirrorram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                main_ram.write(offset & 0x7ff, data);
            }
        };
	
        static int val = 0xff;
	
	public static ReadHandlerPtr nes_bogus_r = new ReadHandlerPtr() {
            public int handler(int offset) {
	    
                val ^= 0xff;
                return val;
            }
        };
	
	static Memory_ReadAddress readmem_nes[]= {
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress( 0x0000, 0x07ff, MRA_RAM ),                /* RAM */
	    new Memory_ReadAddress( 0x0800, 0x1fff, nes_mirrorram_r ),        /* mirrors of RAM */
	    new Memory_ReadAddress( 0x2000, 0x3fff, nes_ppu_r ),              /* PPU registers */
	    new Memory_ReadAddress( 0x4016, 0x4016, nes_IN0_r ),              /* IN0 - input port 1 */
	    new Memory_ReadAddress( 0x4017, 0x4017, nes_IN1_r ),              /* IN1 - input port 2 */
	    new Memory_ReadAddress( 0x4015, 0x4015, nes_bogus_r ),            /* ?? sound status ?? */
	    new Memory_ReadAddress( 0x4100, 0x5fff, nes_low_mapper_r ),       /* Perform unholy acts on the machine */
            //  { 0x6000, 0x7fff, MRA_BANK5 },              /* RAM (also trainer ROM) */
            //  { 0x8000, 0x9fff, MRA_BANK1 },              /* 4 16k NES_ROM banks */
            //  { 0xa000, 0xbfff, MRA_BANK2 },
            //  { 0xc000, 0xdfff, MRA_BANK3 },
            //  { 0xe000, 0xffff, MRA_BANK4 },
            new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */
        };
	
	static Memory_WriteAddress writemem_nes []= {
            new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_WriteAddress( 0x0000, 0x07ff, MWA_RAM, main_ram ),
	    new Memory_WriteAddress( 0x0800, 0x1fff, nes_mirrorram_w ),        /* mirrors of RAM */
	    new Memory_WriteAddress( 0x2000, 0x3fff, nes_ppu_w ),              /* PPU registers */
	    new Memory_WriteAddress( 0x4000, 0x4015, NESPSG_0_w ),
	    new Memory_WriteAddress( 0x4016, 0x4016, nes_IN0_w ),              /* IN0 - input port 1 */
	    new Memory_WriteAddress( 0x4017, 0x4017, nes_IN1_w ),              /* IN1 - input port 2 */
	    new Memory_WriteAddress( 0x4100, 0x5fff, nes_low_mapper_w ),       /* Perform unholy acts on the machine */
            //  { 0x6000, 0x7fff, nes_mid_mapper_w },       /* RAM (sometimes battery-backed) */
            //  { 0x8000, 0xffff, nes_mapper_w },           /* Perform unholy acts on the machine */
            new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */
        };
	
	
	static InputPortPtr input_ports_nes = new InputPortPtr(){ public void handler() { 
	    PORT_START();   /* IN0 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT1 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START1 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT );
	
	    PORT_START();   /* IN1 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT2 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START2 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER2 );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER2 );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2 );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
	
	    PORT_START();   /* IN2 - fake */
	    PORT_DIPNAME( 0x0f, 0x00, "P1 Controller");
	    PORT_DIPSETTING(    0x00, "Joypad" );
	    PORT_DIPSETTING(    0x01, "Zapper" );
	    PORT_DIPSETTING(    0x02, "P1/P3 multi-adapter" );
	    PORT_DIPNAME( 0xf0, 0x00, "P2 Controller");
	    PORT_DIPSETTING(    0x00, "Joypad" );
	    PORT_DIPSETTING(    0x10, "Zapper" );
	    PORT_DIPSETTING(    0x20, "P2/P4 multi-adapter" );
	    PORT_DIPSETTING(    0x30, "Arkanoid paddle" );
	
	    PORT_START();   /* IN3 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X, 70, 30, 0, 255 );
	
	    PORT_START();   /* IN4 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y, 50, 30, 0, 255 );
	
	    PORT_START();   /* IN5 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X | IPF_PLAYER2, 70, 30, 0, 255 );
	
	    PORT_START();   /* IN6 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y | IPF_PLAYER2, 50, 30, 0, 255 );
	
	    PORT_START();   /* IN7 - fake dips */
	    PORT_DIPNAME( 0x01, 0x00, "Draw Top/Bottom 8 Lines");
	    PORT_DIPSETTING(    0x01, DEF_STR("No"));
	    PORT_DIPSETTING(    0x00, DEF_STR("Yes"));
	    PORT_DIPNAME( 0x02, 0x00, "Enforce 8 Sprites/line");
	    PORT_DIPSETTING(    0x02, DEF_STR("No"));
	    PORT_DIPSETTING(    0x00, DEF_STR("Yes"));
	
	    PORT_START();   /* IN8 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER3 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER3 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT3 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START3 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER3 );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER3 );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER3 );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER3 );
	
	    PORT_START();   /* IN9 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER4 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER4 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT4 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START4 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER4 );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER4 );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER4 );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER4 );
	
	    PORT_START();   /* IN10 - arkanoid paddle */
	    PORT_ANALOG( 0xff, 0x7f, IPT_PADDLE, 25, 3, 0x62, 0xf2 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_famicom = new InputPortPtr(){ public void handler() { 
	    PORT_START();   /* IN0 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT1 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START1 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT );
	
	    PORT_START();   /* IN1 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT2 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START2 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER2 );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER2 );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2 );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
	
	    PORT_START();   /* IN2 - fake */
	    PORT_DIPNAME( 0x0f, 0x00, "P1 Controller");
	    PORT_DIPSETTING(    0x00, "Joypad" );
	    PORT_DIPSETTING(    0x01, "Zapper" );
	    PORT_DIPSETTING(    0x02, "P1/P3 multi-adapter" );
	    PORT_DIPNAME( 0xf0, 0x00, "P2 Controller");
	    PORT_DIPSETTING(    0x00, "Joypad" );
	    PORT_DIPSETTING(    0x10, "Zapper" );
	    PORT_DIPSETTING(    0x20, "P2/P4 multi-adapter" );
	    PORT_DIPSETTING(    0x30, "Arkanoid paddle" );
	
	    PORT_START();   /* IN3 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X, 70, 30, 0, 255 );
	
	    PORT_START();   /* IN4 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y, 50, 30, 0, 255 );
	
	    PORT_START();   /* IN5 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X | IPF_PLAYER2, 70, 30, 0, 255 );
	
	    PORT_START();   /* IN6 - generic analog */
	    PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y | IPF_PLAYER2, 50, 30, 0, 255 );
	
	    PORT_START();   /* IN7 - fake dips */
	    PORT_DIPNAME( 0x01, 0x00, "Draw Top/Bottom 8 Lines");
	    PORT_DIPSETTING(    0x01, DEF_STR("No"));
	    PORT_DIPSETTING(    0x00, DEF_STR("Yes"));
	    PORT_DIPNAME( 0x02, 0x00, "Enforce 8 Sprites/line");
	    PORT_DIPSETTING(    0x02, DEF_STR("No"));
	    PORT_DIPSETTING(    0x00, DEF_STR("Yes"));
	
	    PORT_START();   /* IN8 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER3 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER3 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT3 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START3 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER3 );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER3 );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER3 );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER3 );
	
	    PORT_START();   /* IN9 */
	    PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER4 );
	    PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER4 );
	    PORT_BIT ( 0x04, IP_ACTIVE_HIGH, IPT_SELECT4 );
	    PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_START4 );
	    PORT_BIT ( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER4 );
	    PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER4 );
	    PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER4 );
	    PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER4 );
	
	    PORT_START();   /* IN10 - arkanoid paddle */
	    PORT_ANALOG( 0xff, 0x7f, IPT_PADDLE, 25, 3, 0x62, 0xf2 );
	
	    PORT_START();  /* IN11 - fake keys */
	//  PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON3 );
	    PORT_BITX ( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON3, "Change Disk Side", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
	
	INPUT_PORTS_END(); }}; 
	
	/* !! Warning: the charlayout is changed by nes_init_cart !! */
	public static GfxLayout nes_charlayout = new GfxLayout
	(
	    8,8,    /* 8*8 characters */
	    512,    /* 512 characters - changed at runtime */
	    2,  /* 2 bits per pixel */
	    new int[] { 8*8, 0 }, /* the two bitplanes are separated */
	    new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
	    new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	    16*8    /* every char takes 16 consecutive bytes */
	);
	
	/* This layout is not changed at runtime */
	static GfxLayout nes_vram_charlayout = new GfxLayout
	(
	    8,8,    /* 8*8 characters */
	    512,    /* 512 characters */
	    2,  /* 2 bits per pixel */
	    new int[] { 8*8, 0 }, /* the two bitplanes are separated */
	    new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
	    new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	    16*8    /* every char takes 16 consecutive bytes */
	);
	
	
	static GfxDecodeInfo nes_gfxdecodeinfo[] ={
	    new GfxDecodeInfo( REGION_GFX1, 0x0000, nes_charlayout,        0, 8 ),
	    new GfxDecodeInfo( REGION_GFX2, 0x0000, nes_vram_charlayout,   0, 8 ),
            new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static NESinterface nes_interface = new NESinterface
	(
	    1,
            new int[]{REGION_CPU1},
            new int[]{100},
            (int) N2A03_DEFAULTCLOCK,
            new WriteHandlerPtr[]{nes_vh_sprite_dma_w},
            new ReadHandlerPtr[]{null}
	);
	
	/*TODO*///static NESinterface nespal_interface = new NESinterface
	/*TODO*///(
	/*TODO*///    1,
	/*TODO*///    26601712/15,
	/*TODO*///    new int[] { 100 },
	/*TODO*///    { 0 },
	/*TODO*///    { nes_vh_sprite_dma_w },
	/*TODO*///    { null }
	/*TODO*///);
	
	static RomLoadPtr rom_nes = new RomLoadPtr(){ public void handler(){ 
	    ROM_REGION( 0x10000, REGION_CPU1,0 ); /* Main RAM + program banks */
	    ROM_REGION( 0x2000,  REGION_GFX1,0 ); /* VROM */
	    ROM_REGION( 0x2000,  REGION_GFX2,0 ); /* VRAM */
	    ROM_REGION( 0x10000, REGION_USER1,0 );/* WRAM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_nespal = new RomLoadPtr(){ public void handler(){ 
	    ROM_REGION( 0x10000, REGION_CPU1,0 ); /* Main RAM + program banks */
	    ROM_REGION( 0x2000,  REGION_GFX1,0 ); /* VROM */
	    ROM_REGION( 0x2000,  REGION_GFX2,0 ); /* VRAM */
	    ROM_REGION( 0x10000, REGION_USER1,0 );/* WRAM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_famicom = new RomLoadPtr(){ public void handler(){ 
	    ROM_REGION( 0x10000, REGION_CPU1,0 ); /* Main RAM + program banks */
	    ROM_LOAD_OPTIONAL ("disksys.rom", 0xe000, 0x2000, 0x5e607dcf);
	
	    ROM_REGION( 0x2000,  REGION_GFX1,0 ); /* VROM */
	
	    ROM_REGION( 0x2000,  REGION_GFX2,0 ); /* VRAM */
	
	    ROM_REGION( 0x10000, REGION_USER1,0 );/* WRAM */
	ROM_END(); }}; 
	
	
	
	static MachineDriver machine_driver_nes = new MachineDriver
	(
	    /* basic machine hardware */
	    new MachineCPU[] {
	        new MachineCPU(
	            CPU_N2A03, 
                    (int) N2A03_DEFAULTCLOCK, /* 1.79 Mhz - good timing test is Bayou Billy startup wave */
	            readmem_nes,writemem_nes,null,null,
	            nes_interrupt,NTSC_SCANLINES_PER_FRAME /* one for each scanline */
	        )
	    },
	    60, 114*(NTSC_SCANLINES_PER_FRAME-BOTTOM_VISIBLE_SCANLINE), /* frames per second, vblank duration */
	    1,
	    nes_init_machine,
	    nes_stop_machine,
	
	    /* video hardware */
	/*TODO*///#ifdef BIG_SCREEN
	/*TODO*///    32*8*2, 30*8*2, new rectangle( 0*8, 32*8*2-1, 0*8, 30*8*2-1 ),
	/*TODO*///#else
	    32*8, 30*8, new rectangle( 0*8, 32*8-1, 0*8, 30*8-1 ),
	/*TODO*///#endif
	    nes_gfxdecodeinfo,
	/*TODO*///#ifdef COLOR_INTENSITY
	/*TODO*///    4*16*8, 4*8,
	/*TODO*///#else
	    4*16, 4*8,
	/*TODO*///#endif
	    nes_init_palette,
	
	    VIDEO_TYPE_RASTER,
	    null,
	    nes_vh_start,
	    nes_vh_stop,
	    nes_vh_screenrefresh,
	
	    /* sound hardware */
	    0,0,0,0,
	    new MachineSound[]{
                new MachineSound(
                        SOUND_NES,
                        nes_interface
                )
            }
	);
	
	/*TODO*///static MachineDriver machine_driver_nespal = new MachineDriver
	/*TODO*///(
	/*TODO*///    /* basic machine hardware */
	/*TODO*///    new MachineCPU[] {
	/*TODO*///        new MachineCPU(
	/*TODO*///            CPU_N2A03,
	/*TODO*///            26601712/15,    /* 1.773 Mhz - good timing test is Bayou Billy startup wave */
	/*TODO*///            readmem_nes,writemem_nes,null,null,
	/*TODO*///            nes_interrupt,PAL_SCANLINES_PER_FRAME /* one for each scanline */
	/*TODO*///        )
	/*TODO*///    },
	/*TODO*///    50, 114*(PAL_SCANLINES_PER_FRAME-BOTTOM_VISIBLE_SCANLINE),  /* frames per second, vblank duration */
	/*TODO*///    1,
	/*TODO*///    nes_init_machine,
	/*TODO*///    nes_stop_machine,
	
	/*TODO*///    /* video hardware */
	/*TODO*///#ifdef BIG_SCREEN
	/*TODO*///    32*8*2, 30*8*2, new rectangle( 0*8, 32*8*2-1, 0*8, 30*8*2-1 ),
	/*TODO*///#else
	/*TODO*///    32*8, 30*8, { 0*8, 32*8-1, 0*8, 30*8-1 },
	/*TODO*///#endif
	/*TODO*///    nes_gfxdecodeinfo,
	/*TODO*///#ifdef COLOR_INTENSITY
	/*TODO*///    4*16*8, 4*8,
	/*TODO*///#else
	/*TODO*///    4*16, 4*8,
	/*TODO*///#endif
	/*TODO*///    nes_init_palette,
	/*TODO*///
	/*TODO*///    VIDEO_TYPE_RASTER,
	/*TODO*///    0,
	/*TODO*///    nes_vh_start,
	/*TODO*///    nes_vh_stop,
	/*TODO*///    nes_vh_screenrefresh,
	/*TODO*///
	/*TODO*///    /* sound hardware */
	/*TODO*///    0,0,0,0,
	/*TODO*///    {
	/*TODO*///        {
	/*TODO*///            SOUND_NES,
	/*TODO*///            nespal_interface
	/*TODO*///        }
	/*TODO*///    }
	/*TODO*///);
	
	
	static IODevice io_famicom[] = {
	    new IODevice(
	        IO_CARTSLOT,        /* type */
	        1,                  /* count */
	        "nes\0",            /* file extensions */
	        0, /* private */
                nes_id_rom, /* id */
                nes_load_rom, /* init */
	        null,               /* exit */
	        null,               /* info */
	        null,               /* open */
	        null,               /* close */
	        null,               /* status */
	        null,               /* seek */
	        null,               /* tell */
	        null,               /* input */
	        null,               /* output */
	        null,               /* input_chunk */
	        null,               /* output_chunk */
		nes_partialcrc
	    ),
	    /*TODO*///new IODevice(
	    /*TODO*///    IO_FLOPPY,          /* type */
	    /*TODO*///    1,                  /* count */
	    /*TODO*///    "dsk\0fds\0",       /* file extensions */
	    /*TODO*///    IO_RESET_NONE,      /* reset if file changed */
	    /*TODO*///    null,               /* id */
	    /*TODO*///    nes_load_disk,      /* init */
	    /*TODO*///    nes_exit_disk,      /* exit */
	    /*TODO*///    null,               /* info */
	    /*TODO*///    null,               /* open */
	    /*TODO*///    null,               /* close */
	    /*TODO*///    null,               /* status */
	    /*TODO*///    null,               /* seek */
	    /*TODO*///    null,               /* tell */
	    /*TODO*///    null,               /* input */
	    /*TODO*///    null,               /* output */
	    /*TODO*///    null,               /* input_chunk */
	    /*TODO*///    null,                /* output_chunk */
	    /*TODO*///    nes_partialcrc      /* correct CRC */
	    /*TODO*///),
	    new IODevice( IO_END )
	};
	
	static IODevice io_nes[] = {
	    new IODevice(
	        IO_CARTSLOT, /* type */
                1, /* count */
                "nes\0", /* file extensions */
                0, /* private */
                nes_id_rom, /* id */
                nes_load_rom, /* init */
                null, /* exit */
                null, /* info */
                null, /* open */
                null, /* close */
                null, /* status */
                null, /* seek */
                null, /* tell */
                null, /* input */
                null, /* output */
                null, /* input_chunk */
                null, /* output_chunk */
                nes_partialcrc /* correct CRC */
	    ),
	    new IODevice( IO_END )
	};
	
	static IODevice io_nespal[] = {
	    new IODevice(
	        IO_CARTSLOT,        /* type */
	        1,                  /* count */
	        "nes\0",            /* file extensions */
	        0, /* private */
                nes_id_rom, /* id */
                nes_load_rom, /* init */
	        null,               /* exit */
	        null,               /* info */
	        null,               /* open */
	        null,               /* close */
	        null,               /* status */
	        null,               /* seek */
	        null,               /* tell */
	        null,               /* input */
	        null,               /* output */
	        null,               /* input_chunk */
	        null,                /* output_chunk */
	        nes_partialcrc      /* correct CRC */
	    ),
	    new IODevice( IO_END )
	};
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	/*     YEAR  NAME      PARENT    MACHINE   INPUT     INIT      COMPANY   FULLNAME */
	//CONS( 1983, famicom,   0,        nes,      famicom,  nes,      "Nintendo", "Famicom" )
        public static GameDriver driver_famicom = new GameDriver("1983", "famicom", "nes.java", rom_famicom, null, machine_driver_nes, input_ports_famicom, null, io_famicom, "Nintendo", "ZX Famicom");
	//CONS( 1985, nes,       0,        nes,      nes,      nes,      "Nintendo", "Nintendo Entertainment System (NTSC)" )
        public static GameDriver driver_nes = new GameDriver("1985", "nes", "nes.java", rom_nes, null, machine_driver_nes, input_ports_nes, null, io_nes, "Nintendo", "Nintendo Entertainment System (NTSC)");        
	//CONS( 1987, nespal,    nes,      nespal,   nes,      nespal,   "Nintendo", "Nintendo Entertainment System (PAL)" )
        /*TODO*///public static GameDriver driver_spectrum = new GameDriver("1987", "nespal", "nes.java", rom_nespal, null, machine_driver_nespal, input_ports_nes, null, io_nespal, "Nintendo", "Nintendo Entertainment System (PAL)");
	
}
