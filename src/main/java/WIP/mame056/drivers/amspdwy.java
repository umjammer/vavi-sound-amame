/***************************************************************************

							-= American Speedway =-

					driver by	Luca Elia (l.elia@tin.it)


CPU  :	Z80A x 2
Sound:	YM2151


(c)1987 Enerdyne Technologies, Inc. / PGD

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.memoryH.*;
import static mame056.palette.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.sound.vlm5030.*;
import static mame056.sound.vlm5030H.*;
import static mame056.sound.streams.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static WIP.mame056.vidhrdw.amspdwy.*;
import static mame056.vidhrdw.generic.*;
import static mame056.inputH.*;
import static mame056.sound.MSM5205.*;
import static mame056.sound.MSM5205H.*;
import static mame056.sound._3526intf.*;
import static mame056.sound._3812intf.*;
import static mame056.sound._3812intfH.*;
import static common.libc.cstring.*;
import static mame056.mame.Machine;
import static mame056.sound._2151intf.*;
import static mame056.sound._2151intfH.*;
import static mame056.sound.mixerH.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.machine.eeprom.*;
import static mame056.machine.eepromH.*;
import mame056.timer;

public class amspdwy
{
	
	/* Variables & functions defined in vidhrdw: */
	
	
	
	
	/***************************************************************************
	
	
										Main CPU
	
	
	***************************************************************************/
	
	/*
		765-----	Buttons
		---4----	Sgn(Wheel Delta)
		----3210	Abs(Wheel Delta)
	
		Or last value when wheel delta = 0
	*/
	//#define AMSPDWY_WHEEL_R( _n_ ) 
	//AMSPDWY_WHEEL_R( 0 )
        static int wheel_old, ret;
        
        public static ReadHandlerPtr amspdwy_wheel_0_r  = new ReadHandlerPtr() { public int handler(int offset) 
	{ 
		 
		int wheel = readinputport(5 + 0); 
		if (wheel != wheel_old) 
		{ 
			wheel = (wheel & 0x7fff) - (wheel & 0x8000); 
			if (wheel > wheel_old)	ret = ((+wheel) & 0xf) | 0x00; 
			else					ret = ((-wheel) & 0xf) | 0x10; 
			wheel_old = wheel; 
		} 
		return ret | readinputport(2 + 0); 
	} };
    
	//AMSPDWY_WHEEL_R( 1 )
        public static ReadHandlerPtr amspdwy_wheel_1_r  = new ReadHandlerPtr() { public int handler(int offset) 
	{ 
		int wheel = readinputport(5 + 1); 
		if (wheel != wheel_old) 
		{ 
			wheel = (wheel & 0x7fff) - (wheel & 0x8000); 
			if (wheel > wheel_old)	ret = ((+wheel) & 0xf) | 0x00; 
			else					ret = ((-wheel) & 0xf) | 0x10; 
			wheel_old = wheel; 
		} 
		return ret | readinputport(2 + 1); 
	} };
	
	
	public static ReadHandlerPtr amspdwy_sound_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (YM2151_status_port_0_r.handler(0) & ~ 0x30) | readinputport(4);
	} };
	
	public static WriteHandlerPtr amspdwy_sound_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		soundlatch_w.handler(0,data);
		cpu_set_nmi_line(1,PULSE_LINE);
	} };
	
	public static Memory_ReadAddress amspdwy_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM				),	// ROM
	//	new Memory_ReadAddress( 0x8000, 0x801f, MRA_RAM				),	// Palette
		new Memory_ReadAddress( 0x9000, 0x93ff, videoram_r			),	// Layer
		new Memory_ReadAddress( 0x9400, 0x97ff, videoram_r			),	// Mirror?
		new Memory_ReadAddress( 0x9800, 0x9bff, colorram_r			),	// Layer
		new Memory_ReadAddress( 0x9c00, 0x9fff, MRA_RAM				),	// Unused?
		new Memory_ReadAddress( 0xa000, 0xa000, input_port_0_r		),	// DSW 1
		new Memory_ReadAddress( 0xa400, 0xa400, input_port_1_r		),	// DSW 2
		new Memory_ReadAddress( 0xa800, 0xa800, amspdwy_wheel_0_r		),	// Player 1
		new Memory_ReadAddress( 0xac00, 0xac00, amspdwy_wheel_1_r		),	// Player 2
		new Memory_ReadAddress( 0xb400, 0xb400, amspdwy_sound_r		),	// YM2151 Status + Buttons
		new Memory_ReadAddress( 0xc000, 0xc0ff, MRA_RAM				),	// Sprites
		new Memory_ReadAddress( 0xe000, 0xe7ff, MRA_RAM				),	// Work RAM
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress amspdwy_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM							),	// ROM
		new Memory_WriteAddress( 0x8000, 0x801f, amspdwy_paletteram_w, paletteram	),	// Palette
		new Memory_WriteAddress( 0x9000, 0x93ff, amspdwy_videoram_w, videoram		),	// Layer
		new Memory_WriteAddress( 0x9400, 0x97ff, amspdwy_videoram_w				),	// Mirror?
		new Memory_WriteAddress( 0x9800, 0x9bff, amspdwy_colorram_w, colorram		),	// Layer
		new Memory_WriteAddress( 0x9c00, 0x9fff, MWA_RAM							),	// Unused?
	//	new Memory_WriteAddress( 0xa000, 0xa000, MWA_NOP							),	// ?
		new Memory_WriteAddress( 0xa400, 0xa400, amspdwy_flipscreen_w				),	// Toggle Flip Screen?
		new Memory_WriteAddress( 0xb000, 0xb000, MWA_NOP							),	// ? Exiting IRQ
		new Memory_WriteAddress( 0xb400, 0xb400, amspdwy_sound_w					),	// To Sound CPU
		new Memory_WriteAddress( 0xc000, 0xc0ff, MWA_RAM, spriteram, spriteram_size	),	// Sprites
		new Memory_WriteAddress( 0xe000, 0xe7ff, MWA_RAM							),	// Work RAM
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static ReadHandlerPtr amspdwy_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		UBytePtr Tracks = new UBytePtr(memory_region(REGION_CPU1), 0x10000);
		return Tracks.read(offset);
	} };
	
	public static IO_ReadPort amspdwy_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x0000, 0x7fff, amspdwy_port_r	),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	
	/***************************************************************************
	
	
									Sound CPU
	
	
	***************************************************************************/
	
	public static Memory_ReadAddress amspdwy_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM					),	// ROM
		new Memory_ReadAddress( 0x9000, 0x9000, soundlatch_r				),	// From Main CPU
		new Memory_ReadAddress( 0xc000, 0xdfff, MRA_RAM					),	// Work RAM
		new Memory_ReadAddress( 0xffff, 0xffff, MRA_NOP					),	// ??? IY = FFFF at the start ?
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress amspdwy_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM					),	// ROM
	//	new Memory_WriteAddress( 0x8000, 0x8000, MWA_NOP					),	// ? Written with 0 at the start
		new Memory_WriteAddress( 0xa000, 0xa000, YM2151_register_port_0_w	),	// YM2151
		new Memory_WriteAddress( 0xa001, 0xa001, YM2151_data_port_0_w		),	//
		new Memory_WriteAddress( 0xc000, 0xdfff, MWA_RAM					),	// Work RAM
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	
	/***************************************************************************
	
	
									Input Ports
	
	
	***************************************************************************/
	
	static InputPortPtr input_ports_amspdwy = new InputPortPtr(){ public void handler() { 
	
		PORT_START(); 	// IN0 - DSW 1
		PORT_DIPNAME( 0x01, 0x00, "Character Test" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "Show Arrows" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_SERVICE( 0x08, IP_ACTIVE_HIGH );
		PORT_DIPNAME( 0x10, 0x00, "Steering Test" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_BIT(     0xe0, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	
		PORT_START(); 	// IN1 - DSW 2
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x03, DEF_STR( "2C_1C") );
	//	PORT_DIPSETTING(    0x02, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x0c, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Easy" );
		PORT_DIPSETTING(    0x04, "Normal" );
		PORT_DIPSETTING(    0x08, "Hard" );
		PORT_DIPSETTING(    0x0c, "Hardest" );
		PORT_DIPNAME( 0x10, 0x00, "Time" );
		PORT_DIPSETTING(    0x10, "45 sec" );
		PORT_DIPSETTING(    0x00, "60 sec" );
		PORT_BIT(     0xe0, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	
		PORT_START(); 	// IN2 - Player 1 Wheel + Coins
		PORT_BIT( 0x1f, IP_ACTIVE_HIGH, IPT_SPECIAL );// wheel
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN1, 2 );// 2-3f
	
		PORT_START(); 	// IN3 - Player 2 Wheel + Coins
		PORT_BIT( 0x1f, IP_ACTIVE_HIGH, IPT_SPECIAL );
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN2, 2 );
	
		PORT_START(); 	// IN4 - Player 1&2 Pedals + YM2151 Sound Status
		PORT_BIT( 0x0f, IP_ACTIVE_HIGH, IPT_SPECIAL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0xc0, IP_ACTIVE_HIGH, IPT_SPECIAL );
	
		PORT_START(); 	// IN5 - Player 1 Analog Fake Port
		PORT_ANALOGX( 0xffff, 0x0000, IPT_DIAL | IPF_PLAYER1, 15, 20, 0, 0, KEYCODE_LEFT, KEYCODE_RIGHT, 0, 0 );
	
		PORT_START(); 	// IN6 - Player 2 Analog Fake Port
		PORT_ANALOGX( 0xffff, 0x0000, IPT_DIAL | IPF_PLAYER2, 15, 20, 0, 0, KEYCODE_D, KEYCODE_G, 0, 0 );
	
	INPUT_PORTS_END(); }}; 
	
	
	
	
	/***************************************************************************
	
	
									Graphics Layouts
	
	
	***************************************************************************/
	
	static GfxLayout layout_8x8x2 = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		2,
		new int[] { RGN_FRAC(0,2), RGN_FRAC(1,2) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8
	);
	
	static GfxDecodeInfo amspdwy_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, layout_8x8x2,   0, 8 ), // [0] Layer  Sprites
		new GfxDecodeInfo( -1 )
	};
	
	
	
	/***************************************************************************
	
	
									Machine Drivers
	
	
	***************************************************************************/
	
	
	static WriteYmHandlerPtr irq_handler = new WriteYmHandlerPtr() {
            public void handler(int irq) {                
		cpu_set_irq_line(1,0,irq!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	static YM2151interface amspdwy_ym2151_interface = new YM2151interface
	(
		1,
		3000000,	/* ? */
		new int[]{ YM3012_VOL(100,MIXER_PAN_LEFT,100,MIXER_PAN_RIGHT) },
		new WriteYmHandlerPtr[]{ irq_handler }
	);
	
	
	static MachineDriver machine_driver_amspdwy = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80 | CPU_16BIT_PORT,
				3000000,	/* ? */
				amspdwy_readmem,  amspdwy_writemem,
				amspdwy_readport, null,
				interrupt, 1	/* IRQ: 60Hz, NMI: retn */
			),
			new MachineCPU(
				CPU_Z80,	/* Can't be disabled: the YM2151 timers must work */
				3000000,	/* ? */
				amspdwy_sound_readmem, amspdwy_sound_writemem,
				null, null,
				ignore_interrupt, 1		/* IRQ: YM2151, NMI: main CPU */
			)
		},
		60,DEFAULT_60HZ_VBLANK_DURATION,
		1,
		null,
	
		/* video hardware */
		256, 256, new rectangle( 0, 256-1, 0+16, 256-16-1 ),
		amspdwy_gfxdecodeinfo,
		32, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		amspdwy_vh_start,
		null,
		amspdwy_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound(	SOUND_YM2151,	amspdwy_ym2151_interface	)
		}
	);
	
	
	
	
	/***************************************************************************
	
	
									ROMs Loading
	
	
	***************************************************************************/
	
	
	
	/***************************************************************************
	
								American Speedway
	
	USES TWO Z80 CPU'S W/YM2151 SOUND
	THE NUMBERS WITH THE NAMES ARE PROBABLY CHECKSUMS
	
	NAME    LOCATION    TYPE
	------------------------
	AUDI9363 U2         27256   CONN BD
	GAME5807 U33         "       "
	TRKS6092 U34         "       "
	HIHIE12A 4A         2732    REAR BD
	HILO9B3C 5A          "       "
	LOHI4644 2A          "       "
	LOLO1D51 1A          "       "
	
							American Speedway (Set 2)
	
	1987 Enerdyne Technologies, Inc. Has Rev 4 PGD written on the top board.
	
	Processors
	------------------
	Dual Z80As
	YM2151     (sound)
	
	RAM
	------------------
	12 2114
	5  82S16N
	
	Eproms
	==================
	
	Name        Loc   TYpe   Checksum
	----------  ----  -----  --------
	Game.u22    U33   27256  A222
	Tracks.u34  U34   27256  6092
	Audio.U02   U2    27256  9363
	LOLO1.1A    1A    2732   1D51
	LOHI.2A     2A    2732   4644
	HIHI.4A     3/4A  2732   E12A
	HILO.5A     5A    2732   9B3C
	
	***************************************************************************/
	
	static RomLoadPtr rom_amspdwy = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );	/* Main Z80 Code */
		ROM_LOAD( "game5807.u33", 0x00000, 0x8000, 0x88233b59 );
		ROM_LOAD( "trks6092.u34", 0x10000, 0x8000, 0x74a4e7b7 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* Sound Z80 Code */
		ROM_LOAD( "audi9463.u2", 0x00000, 0x8000, 0x61b0467e );
	
		ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );/* Layer + Sprites */
		ROM_LOAD( "hilo9b3c.5a", 0x0000, 0x1000, 0xf50f864c );
		ROM_LOAD( "hihie12a.4a", 0x1000, 0x1000, 0x3d7497f3 );
		ROM_LOAD( "lolo1d51.1a", 0x2000, 0x1000, 0x58701c1c );
		ROM_LOAD( "lohi4644.2a", 0x3000, 0x1000, 0xa1d802b1 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_amspdwya = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );	/* Main Z80 Code */
		ROM_LOAD( "game.u33",     0x00000, 0x8000, 0xfacab102 );
		ROM_LOAD( "trks6092.u34", 0x10000, 0x8000, 0x74a4e7b7 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* Sound Z80 Code */
		ROM_LOAD( "audi9463.u2", 0x00000, 0x8000, 0x61b0467e );
	
		ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );/* Layer + Sprites */
		ROM_LOAD( "hilo9b3c.5a", 0x0000, 0x1000, 0xf50f864c );
		ROM_LOAD( "hihie12a.4a", 0x1000, 0x1000, 0x3d7497f3 );
		ROM_LOAD( "lolo1d51.1a", 0x2000, 0x1000, 0x58701c1c );
		ROM_LOAD( "lohi4644.2a", 0x3000, 0x1000, 0xa1d802b1 );
	ROM_END(); }}; 
	
	
	/* (C) 1987 ETI 8402 MAGNOLIA ST. #C SANTEE, CA 92071 */
	
	public static GameDriver driver_amspdwy	   = new GameDriver("1987"	,"amspdwy"	,"amspdwy.java"	,rom_amspdwy,null	,machine_driver_amspdwy	,input_ports_amspdwy	,null	,ROT0	,	"Enerdyne Technologies, Inc.", "American Speedway (set 1)" );
	public static GameDriver driver_amspdwya	   = new GameDriver("1987"	,"amspdwya"	,"amspdwy.java"	,rom_amspdwya,driver_amspdwy	,machine_driver_amspdwy	,input_ports_amspdwy	,null	,ROT0	,	"Enerdyne Technologies, Inc.", "American Speedway (set 2)" );
}
