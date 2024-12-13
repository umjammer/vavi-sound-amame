/*
World Cup 90 ( Tecmo ) driver
-----------------------------

Ernesto Corvi
(ernesto@imagina.com)

TODO:
- Dip switches mapping is not complete. ( Anyone has the manual handy? )
- Missing drums, they might be internal to the YM2608.
- wc90t has wrong graphics. Different hardware? It is also missing the color
  bars on startup.


CPU #1 : Handles background & foreground tiles, controllers, dipswitches.
CPU #2 : Handles sprites and palette
CPU #3 : Audio.

Memory Layout:

CPU #1
0000-8000 ROM
8000-9000 RAM
a000-a800 Color Ram for background #1 tiles
a800-b000 Video Ram for background #1 tiles
c000-c800 Color Ram for background #2 tiles
c800-c000 Video Ram for background #2 tiles
e000-e800 Color Ram for foreground tiles
e800-f000 Video Ram for foreground tiles
f800-fc00 Common Ram with CPU #2
fc00-fc00 Stick 1 input port
fc02-fc02 Stick 2 input port
fc05-fc05 Start buttons and Coins input port
fc06-fc06 Dip Switch A
fc07-fc07 Dip Switch B

CPU #2
0000-c000 ROM
c000-d000 RAM
d000-d800 RAM Sprite Ram
e000-e800 RAM Palette Ram
f800-fc00 Common Ram with CPU #1

CPU #3
0000-0xc000 ROM
???????????

*/

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
import static WIP.mame056.vidhrdw.wc90.*;
import static mame056.sound.ay8910.*;
import mame056.sound.ay8910H.AY8910interface;

import static mame056.sound._2608intf.*;
import static mame056.sound._2608intfH.*;
import static mame056.sound._2151intf.*;
import static mame056.sound._2151intfH.*;
import static mame056.sound.mixerH.*;

import static mame056.vidhrdw.generic.*;


public class wc90
{
	
	static UBytePtr wc90_shared = new UBytePtr();
	
	public static ReadHandlerPtr wc90_shared_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return wc90_shared.read(offset);
	} };
	
	public static WriteHandlerPtr wc90_shared_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		wc90_shared.write(offset, data);
	} };
	
	public static WriteHandlerPtr wc90_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bankaddress;
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
	
		bankaddress = 0x10000 + ( ( data & 0xf8 ) << 8 );
		cpu_setbank( 1,new UBytePtr(RAM, bankaddress) );
	} };
	
	public static WriteHandlerPtr wc90_bankswitch1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bankaddress;
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU2));
	
	
		bankaddress = 0x10000 + ( ( data & 0xf8 ) << 8 );
		cpu_setbank( 2,new UBytePtr(RAM, bankaddress) );
	} };
	
	public static WriteHandlerPtr wc90_sound_command_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		soundlatch_w.handler(offset,data);
		cpu_cause_interrupt(2,Z80_NMI_INT);
	} };
	
	
	
	public static Memory_ReadAddress wc90_readmem1[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x9fff, MRA_RAM ), /* Main RAM */
		new Memory_ReadAddress( 0xa000, 0xafff, MRA_RAM ), /* fg video ram */
		new Memory_ReadAddress( 0xb000, 0xbfff, MRA_RAM ),
		new Memory_ReadAddress( 0xc000, 0xcfff, MRA_RAM ), /* bg video ram */
		new Memory_ReadAddress( 0xd000, 0xdfff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xefff, MRA_RAM ), /* tx video ram */
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xf800, 0xfbff, wc90_shared_r ),
		new Memory_ReadAddress( 0xfc00, 0xfc00, input_port_0_r ), /* Stick 1 */
		new Memory_ReadAddress( 0xfc02, 0xfc02, input_port_1_r ), /* Stick 2 */
		new Memory_ReadAddress( 0xfc05, 0xfc05, input_port_4_r ), /* Start  Coin */
		new Memory_ReadAddress( 0xfc06, 0xfc06, input_port_2_r ), /* DIP Switch A */
		new Memory_ReadAddress( 0xfc07, 0xfc07, input_port_3_r ), /* DIP Switch B */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress wc90_readmem2[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xcfff, MRA_RAM ),
		new Memory_ReadAddress( 0xd000, 0xd7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xd800, 0xdfff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xe7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_BANK2 ),
		new Memory_ReadAddress( 0xf800, 0xfbff, wc90_shared_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress wc90_writemem1[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x9fff, MWA_RAM ),
		new Memory_WriteAddress( 0xa000, 0xafff, wc90_fgvideoram_w, wc90_fgvideoram ),
		new Memory_WriteAddress( 0xb000, 0xbfff, MWA_RAM ),
		new Memory_WriteAddress( 0xc000, 0xcfff, wc90_bgvideoram_w, wc90_bgvideoram ),
		new Memory_WriteAddress( 0xd000, 0xdfff, MWA_RAM ),
		new Memory_WriteAddress( 0xe000, 0xefff, wc90_txvideoram_w, wc90_txvideoram ),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_ROM ),
		new Memory_WriteAddress( 0xf800, 0xfbff, wc90_shared_w, wc90_shared ),
		new Memory_WriteAddress( 0xfc02, 0xfc02, MWA_RAM, wc90_scroll0ylo ),
		new Memory_WriteAddress( 0xfc03, 0xfc03, MWA_RAM, wc90_scroll0yhi ),
		new Memory_WriteAddress( 0xfc06, 0xfc06, MWA_RAM, wc90_scroll0xlo ),
		new Memory_WriteAddress( 0xfc07, 0xfc07, MWA_RAM, wc90_scroll0xhi ),
		new Memory_WriteAddress( 0xfc22, 0xfc22, MWA_RAM, wc90_scroll1ylo ),
		new Memory_WriteAddress( 0xfc23, 0xfc23, MWA_RAM, wc90_scroll1yhi ),
		new Memory_WriteAddress( 0xfc26, 0xfc26, MWA_RAM, wc90_scroll1xlo ),
		new Memory_WriteAddress( 0xfc27, 0xfc27, MWA_RAM, wc90_scroll1xhi ),
		new Memory_WriteAddress( 0xfc42, 0xfc42, MWA_RAM, wc90_scroll2ylo ),
		new Memory_WriteAddress( 0xfc43, 0xfc43, MWA_RAM, wc90_scroll2yhi ),
		new Memory_WriteAddress( 0xfc46, 0xfc46, MWA_RAM, wc90_scroll2xlo ),
		new Memory_WriteAddress( 0xfc47, 0xfc47, MWA_RAM, wc90_scroll2xhi ),
		new Memory_WriteAddress( 0xfcc0, 0xfcc0, wc90_sound_command_w ),
		new Memory_WriteAddress( 0xfcd0, 0xfcd0, watchdog_reset_w ),
		new Memory_WriteAddress( 0xfce0, 0xfce0, wc90_bankswitch_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress wc90_writemem2[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xcfff, MWA_RAM ),
		new Memory_WriteAddress( 0xd000, 0xd7ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xd800, 0xdfff, MWA_RAM ),
		new Memory_WriteAddress( 0xe000, 0xe7ff, paletteram_xxxxBBBBRRRRGGGG_swap_w, paletteram ),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_ROM ),
		new Memory_WriteAddress( 0xf800, 0xfbff, wc90_shared_w ),
		new Memory_WriteAddress( 0xfc00, 0xfc00, wc90_bankswitch1_w ),
		new Memory_WriteAddress( 0xfc01, 0xfc01, watchdog_reset_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf800, 0xf800, YM2608_status_port_0_A_r ),
		new Memory_ReadAddress( 0xf802, 0xf802, YM2608_status_port_0_B_r ),
		new Memory_ReadAddress( 0xfc00, 0xfc00, MRA_NOP ), /* ??? adpcm ??? */
		new Memory_ReadAddress( 0xfc10, 0xfc10, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xf800, 0xf800, YM2608_control_port_0_A_w ),
		new Memory_WriteAddress( 0xf801, 0xf801, YM2608_data_port_0_A_w ),
		new Memory_WriteAddress( 0xf802, 0xf802, YM2608_control_port_0_B_w ),
		new Memory_WriteAddress( 0xf803, 0xf803, YM2608_data_port_0_B_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_wc90 = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 bit 0-5 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN1 bit 0-5 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* DSWA */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, "10 Coins/1 Credit" );
		PORT_DIPSETTING(    0x08, DEF_STR( "9C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "8C_1C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "7C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "6C_1C") );
		PORT_DIPSETTING(    0x0a, DEF_STR( "5C_1C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x0e, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x09, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x0b, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0d, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x30, "Easy" );
		PORT_DIPSETTING(    0x10, "Normal" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x40, 0x40, "Countdown Speed" );
		PORT_DIPSETTING(    0x40, "Normal" );
		PORT_DIPSETTING(    0x00, "Fast" );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START(); 	/* DSWB */
		PORT_DIPNAME( 0x03, 0x03, "1 Player Game Time" );
		PORT_DIPSETTING(    0x01, "1:00" );
		PORT_DIPSETTING(    0x02, "1:30" );
		PORT_DIPSETTING(    0x03, "2:00" );
		PORT_DIPSETTING(    0x00, "2:30" );
		PORT_DIPNAME( 0x1c, 0x1c, "2 Players Game Time" );
		PORT_DIPSETTING(    0x0c, "1:00" );
		PORT_DIPSETTING(    0x14, "1:30" );
		PORT_DIPSETTING(    0x04, "2:00" );
		PORT_DIPSETTING(    0x18, "2:30" );
		PORT_DIPSETTING(    0x1c, "3:00" );
		PORT_DIPSETTING(    0x08, "3:30" );
		PORT_DIPSETTING(    0x10, "4:00" );
		PORT_DIPSETTING(    0x00, "5:00" );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Language" );
		PORT_DIPSETTING(    0x00, "English" );
		PORT_DIPSETTING(    0x80, "Japanese" );
	
		PORT_START(); 	/* IN2 bit 0-3 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START2 );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8
	);
	
	static GfxLayout tilelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,1),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
				32*8+0*4, 32*8+1*4, 32*8+2*4, 32*8+3*4, 32*8+4*4, 32*8+5*4, 32*8+6*4, 32*8+7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
				16*32, 17*32, 18*32, 19*32, 20*32, 21*32, 22*32, 23*32 },
		128*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,2),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { 0*4, 1*4, RGN_FRAC(1,2)+0*4, RGN_FRAC(1,2)+1*4, 2*4, 3*4, RGN_FRAC(1,2)+2*4, RGN_FRAC(1,2)+3*4,
				16*8+0*4, 16*8+1*4, RGN_FRAC(1,2)+16*8+0*4, RGN_FRAC(1,2)+16*8+1*4, 16*8+2*4, 16*8+3*4, RGN_FRAC(1,2)+16*8+2*4, RGN_FRAC(1,2)+16*8+3*4 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
				16*16, 17*16, 18*16, 19*16, 20*16, 21*16, 22*16, 23*16 },
		64*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x00000, charlayout,      	1*16*16, 16*16 ),
		new GfxDecodeInfo( REGION_GFX2, 0x00000, tilelayout,		2*16*16, 16*16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x00000, tilelayout,		3*16*16, 16*16 ),
		new GfxDecodeInfo( REGION_GFX4, 0x00000, spritelayout,		0*16*16, 16*16 ), // sprites
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	/* handler called by the 2608 emulator when the internal timers cause an IRQ */
	static WriteYmHandlerPtr irqhandler = new WriteYmHandlerPtr() {
            public void handler(int irq) {
		cpu_set_irq_line(2,0,irq!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	static YM2608interface ym2608_interface = new YM2608interface
	(
		1,
		8000000,
		new int[]{ 50 },
		new ReadHandlerPtr[]{ null },
		new ReadHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ null },
		new WriteYmHandlerPtr[] { irqhandler },
		new int[]{ REGION_SOUND1 },
		new int[]{ YM3012_VOL(50,MIXER_PAN_LEFT,50,MIXER_PAN_RIGHT) }
	);
	
	static MachineDriver machine_driver_wc90 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				6000000,	/* 6.0 MHz ??? */
				wc90_readmem1, wc90_writemem1,null,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				6000000,	/* 6.0 MHz ??? */
				wc90_readmem2, wc90_writemem2,null,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				4000000,	/* 4 MHz ???? */
				sound_readmem,sound_writemem,null,null,
				ignore_interrupt,0	/* IRQs are triggered by the YM2203 */
									/* NMIs are triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		1024, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		wc90_vh_start,
		null,
		wc90_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2608,
				ym2608_interface
			)
		}
                
	);
	
	
	
	static RomLoadPtr rom_wc90 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* 128k for code */
		ROM_LOAD( "ic87_01.bin",  0x00000, 0x08000, 0x4a1affbc );/* c000-ffff is not used */
		ROM_LOAD( "ic95_02.bin",  0x10000, 0x10000, 0x847d439c );/* banked at f000-f7ff */
	
		ROM_REGION( 0x20000, REGION_CPU2, 0 );/* 96k for code */  /* Second CPU */
		ROM_LOAD( "ic67_04.bin",  0x00000, 0x10000, 0xdc6eaf00 );/* c000-ffff is not used */
		ROM_LOAD( "ic56_03.bin",  0x10000, 0x10000, 0x1ac02b3b );/* banked at f000-f7ff */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "ic54_05.bin",  0x00000, 0x10000, 0x27c348b3 );
	
		ROM_REGION( 0x010000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "ic85_07v.bin", 0x00000, 0x10000, 0xc5219426 );/* characters */
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "ic86_08v.bin", 0x00000, 0x20000, 0x8fa1a1ff );/* tiles #1 */
		ROM_LOAD( "ic90_09v.bin", 0x20000, 0x20000, 0x99f8841c );/* tiles #2 */
	
		ROM_REGION( 0x040000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "ic87_10v.bin", 0x00000, 0x20000, 0x8232093d );/* tiles #3 */
		ROM_LOAD( "ic91_11v.bin", 0x20000, 0x20000, 0x188d3789 );/* tiles #4 */
	
		ROM_REGION( 0x080000, REGION_GFX4, ROMREGION_DISPOSE );
		ROM_LOAD( "ic50_12v.bin", 0x00000, 0x20000, 0xda1fe922 );/* sprites  */
		ROM_LOAD( "ic54_13v.bin", 0x20000, 0x20000, 0x9ad03c2c );/* sprites  */
		ROM_LOAD( "ic60_14v.bin", 0x40000, 0x20000, 0x499dfb1b );/* sprites  */
		ROM_LOAD( "ic65_15v.bin", 0x60000, 0x20000, 0xd8ea5c81 );/* sprites  */
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* 64k for ADPCM samples */
		ROM_LOAD( "ic82_06.bin",  0x00000, 0x20000, 0x2fd692ed );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_wc90a = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* 128k for code */
		ROM_LOAD( "wc90-1.bin",   0x00000, 0x08000, 0xd1804e1a );/* c000-ffff is not used */
		ROM_LOAD( "ic95_02.bin",  0x10000, 0x10000, 0x847d439c );/* banked at f000-f7ff */
	
		ROM_REGION( 0x20000, REGION_CPU2, 0 );/* 96k for code */  /* Second CPU */
		ROM_LOAD( "ic67_04.bin",  0x00000, 0x10000, 0xdc6eaf00 );/* c000-ffff is not used */
		ROM_LOAD( "ic56_03.bin",  0x10000, 0x10000, 0x1ac02b3b );/* banked at f000-f7ff */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "ic54_05.bin",  0x00000, 0x10000, 0x27c348b3 );
	
		ROM_REGION( 0x010000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "ic85_07v.bin", 0x00000, 0x10000, 0xc5219426 );/* characters */
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "ic86_08v.bin", 0x00000, 0x20000, 0x8fa1a1ff );/* tiles #1 */
		ROM_LOAD( "ic90_09v.bin", 0x20000, 0x20000, 0x99f8841c );/* tiles #2 */
	
		ROM_REGION( 0x040000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "ic87_10v.bin", 0x00000, 0x20000, 0x8232093d );/* tiles #3 */
		ROM_LOAD( "ic91_11v.bin", 0x20000, 0x20000, 0x188d3789 );/* tiles #4 */
	
		ROM_REGION( 0x080000, REGION_GFX4, ROMREGION_DISPOSE );
		ROM_LOAD( "ic50_12v.bin", 0x00000, 0x20000, 0xda1fe922 );/* sprites  */
		ROM_LOAD( "ic54_13v.bin", 0x20000, 0x20000, 0x9ad03c2c );/* sprites  */
		ROM_LOAD( "ic60_14v.bin", 0x40000, 0x20000, 0x499dfb1b );/* sprites  */
		ROM_LOAD( "ic65_15v.bin", 0x60000, 0x20000, 0xd8ea5c81 );/* sprites  */
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* 64k for ADPCM samples */
		ROM_LOAD( "ic82_06.bin",  0x00000, 0x20000, 0x2fd692ed );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_wc90t = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* 128k for code */
		ROM_LOAD( "wc90a-1.bin",  0x00000, 0x08000, 0xb6f51a68 );/* c000-ffff is not used */
		ROM_LOAD( "wc90a-2.bin",  0x10000, 0x10000, 0xc50f2a98 );/* banked at f000-f7ff */
	
		ROM_REGION( 0x20000, REGION_CPU2, 0 );/* 96k for code */  /* Second CPU */
		ROM_LOAD( "ic67_04.bin",  0x00000, 0x10000, 0xdc6eaf00 );/* c000-ffff is not used */
		ROM_LOAD( "wc90a-3.bin",  0x10000, 0x10000, 0x8c7a9542 );/* banked at f000-f7ff */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "ic54_05.bin",  0x00000, 0x10000, 0x27c348b3 );
	
		ROM_REGION( 0x010000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "ic85_07v.bin", 0x00000, 0x10000, 0xc5219426 );/* characters */
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "ic86_08v.bin", 0x00000, 0x20000, 0x8fa1a1ff );/* tiles #1 */
		ROM_LOAD( "ic90_09v.bin", 0x20000, 0x20000, 0x99f8841c );/* tiles #2 */
	
		ROM_REGION( 0x040000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "ic87_10v.bin", 0x00000, 0x20000, 0x8232093d );/* tiles #3 */
		ROM_LOAD( "ic91_11v.bin", 0x20000, 0x20000, 0x188d3789 );/* tiles #4 */
	
		ROM_REGION( 0x080000, REGION_GFX4, ROMREGION_DISPOSE );
		ROM_LOAD( "ic50_12v.bin", 0x00000, 0x20000, 0xda1fe922 );/* sprites  */
		ROM_LOAD( "ic54_13v.bin", 0x20000, 0x20000, 0x9ad03c2c );/* sprites  */
		ROM_LOAD( "ic60_14v.bin", 0x40000, 0x20000, 0x499dfb1b );/* sprites  */
		ROM_LOAD( "ic65_15v.bin", 0x60000, 0x20000, 0xd8ea5c81 );/* sprites  */
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* 64k for ADPCM samples */
		ROM_LOAD( "ic82_06.bin",  0x00000, 0x20000, 0x2fd692ed );
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_wc90	   = new GameDriver("1989"	,"wc90"	,"wc90.java"	,rom_wc90,null	,machine_driver_wc90	,input_ports_wc90	,null	,ROT0	,	"Tecmo", "World Cup '90 (set 1)", GAME_IMPERFECT_SOUND | GAME_NO_COCKTAIL );
	public static GameDriver driver_wc90a	   = new GameDriver("1989"	,"wc90a"	,"wc90.java"	,rom_wc90a,driver_wc90	,machine_driver_wc90	,input_ports_wc90	,null	,ROT0	,	"Tecmo", "World Cup '90 (set 2)", GAME_IMPERFECT_SOUND | GAME_NO_COCKTAIL );
	public static GameDriver driver_wc90t	   = new GameDriver("1989"	,"wc90t"	,"wc90.java"	,rom_wc90t,driver_wc90	,machine_driver_wc90	,input_ports_wc90	,null	,ROT0	,	"Tecmo", "World Cup '90 (trackball)", GAME_IMPERFECT_SOUND | GAME_NO_COCKTAIL );
}
