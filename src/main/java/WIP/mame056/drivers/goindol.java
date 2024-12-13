/***************************************************************************
  GOINDOL

  Driver provided by Jarek Parchanski (jpdev@friko6.onet.pl)
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
import static mame056.timer.*;
import static mame056.timerH.*;
import static WIP.mame056.vidhrdw.goindol.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.machine._6812pia.*;
import static mame056.machine._6812pia.*;
import static mame056.machine._6812piaH.*;
import static mame056.machine._8255ppiH.*;
import static mame056.machine._8255ppi.*;

import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.sound._3526intf.*;
import static mame056.sound._3812intfH.*;
import static WIP.mame056.sound._5220intf.*;
import static WIP.mame056.sound._5220intfH.*;
import static mame056.mame.Machine;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;

import static mame056.sound.MSM5205.*;
import static mame056.sound.MSM5205H.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

import static mame056.vidhrdw.generic.*;

public class goindol
{
	
	public static WriteHandlerPtr goindol_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bankaddress;
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		bankaddress = 0x10000 + ((data & 3) * 0x4000);
		cpu_setbank(1, new UBytePtr(RAM, bankaddress));
	
		if (goindol_char_bank != ((data & 0x10) >> 4))
		{
			goindol_char_bank = (data & 0x10) >> 4;
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc000, 0xc7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xc800, 0xc800, MRA_NOP ),
		new Memory_ReadAddress( 0xd000, 0xefff, MRA_RAM ),
		new Memory_ReadAddress( 0xf000, 0xf000, input_port_3_r ),
		new Memory_ReadAddress( 0xf800, 0xf800, input_port_4_r ),
		new Memory_ReadAddress( 0xc834, 0xc834, input_port_1_r ),
		new Memory_ReadAddress( 0xc820, 0xc820, input_port_2_r ),
		new Memory_ReadAddress( 0xc830, 0xc830, input_port_0_r ),
		new Memory_ReadAddress( 0xe000, 0xefff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xc810, 0xc810, goindol_bankswitch_w ),
		new Memory_WriteAddress( 0xc820, 0xd820, MWA_RAM, goindol_fg_scrolly ),
		new Memory_WriteAddress( 0xc830, 0xd830, MWA_RAM, goindol_fg_scrollx ),
		new Memory_WriteAddress( 0xc800, 0xc800, soundlatch_w ),
		new Memory_WriteAddress( 0xd000, 0xd03f, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xd040, 0xd7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xd800, 0xdfff, goindol_bg_videoram_w, goindol_bg_videoram, goindol_bg_videoram_size ),
		new Memory_WriteAddress( 0xe000, 0xe03f, MWA_RAM, spriteram_2 ),
		new Memory_WriteAddress( 0xe040, 0xe7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xe800, 0xefff, goindol_fg_videoram_w, goindol_fg_videoram, goindol_fg_videoram_size ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xc7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xd800, 0xd800, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xa000, 0xa000, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0xa001, 0xa001, YM2203_write_port_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_goindol = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1, 1 );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN2, 1 );
	
		PORT_START();       /* IN2 - spinner */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL , 40, 10, 0, 0);
	
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x1c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x1c, "Easiest" );
		PORT_DIPSETTING(    0x18, "Very Very Easy" );
		PORT_DIPSETTING(    0x14, "Very Easy" );
		PORT_DIPSETTING(    0x10, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x08, "Difficult" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPSETTING(    0x00, "Very Hard" );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BITX(    0x40, 0x40, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invulnerability", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START();       /* DSW1 */
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x04, "30k and every 50k" );
		PORT_DIPSETTING(    0x05, "50k and every 100k" );
		PORT_DIPSETTING(    0x06, "50k and every 200k" );
		PORT_DIPSETTING(    0x07, "100k and every 200k" );
		PORT_DIPSETTING(    0x01, "10000 only" );
		PORT_DIPSETTING(    0x02, "30000 only" );
		PORT_DIPSETTING(    0x03, "50000 only" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0x38, 0x00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x28, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x38, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_homo = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1, 1 );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN2, 1 );
	
		PORT_START();       /* IN2 - spinner */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL , 40, 10, 0, 0);
	
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x1c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x1c, "Easiest" );
		PORT_DIPSETTING(    0x18, "Very Very Easy" );
		PORT_DIPSETTING(    0x14, "Very Easy" );
		PORT_DIPSETTING(    0x10, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x08, "Difficult" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPSETTING(    0x00, "Very Hard" );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START();       /* DSW1 */
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x04, "30k and every 50k" );
		PORT_DIPSETTING(    0x05, "50k and every 100k" );
		PORT_DIPSETTING(    0x06, "50k and every 200k" );
		PORT_DIPSETTING(    0x07, "100k and every 200k" );
		PORT_DIPSETTING(    0x01, "10000 only" );
		PORT_DIPSETTING(    0x02, "30000 only" );
		PORT_DIPSETTING(    0x03, "50000 only" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0x38, 0x00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x28, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x38, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,3),
		3,
		new int[] {  RGN_FRAC(0,3), RGN_FRAC(1,3), RGN_FRAC(2,3) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 0, 32 ),
		new GfxDecodeInfo( REGION_GFX2, 0, charlayout, 0, 32 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static YM2203interface ym2203_interface = new YM2203interface
	(
		1,		/* 1 chip */
		2000000,	/* 2 MHz (?) */
		new int[]{ YM2203_VOL(25,25) },
		new ReadHandlerPtr[]{ null },
		new ReadHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ null }
	);
	
	
	
	static MachineDriver machine_driver_goindol = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				6000000,        /* 6 MHz (?) */
				readmem,writemem,null,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				4000000,	/* 4 MHz (?) */
				sound_readmem,sound_writemem,null,null,
				interrupt,4
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		256, 0,
		palette_RRRR_GGGG_BBBB_convert_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		goindol_vh_start,
		null,
		goindol_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2203,
				ym2203_interface
			)
		}
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_goindol = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );    /* 2*64k for code */
		ROM_LOAD( "r1", 0x00000, 0x8000, 0x3111c61b );/* Code 0000-7fff */
		ROM_LOAD( "r2", 0x10000, 0x8000, 0x1ff6e3a2 );/* Paged data */
		ROM_LOAD( "r3", 0x18000, 0x8000, 0xe9eec24a );/* Paged data */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "r10", 0x00000, 0x8000, 0x72e1add1 );
	
		ROM_REGION( 0x18000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "r4", 0x00000, 0x8000, 0x1ab84225 );/* Characters */
		ROM_LOAD( "r5", 0x08000, 0x8000, 0x4997d469 );
		ROM_LOAD( "r6", 0x10000, 0x8000, 0x752904b0 );
	
		ROM_REGION( 0x18000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "r7", 0x00000, 0x8000, 0x362f2a27 );
		ROM_LOAD( "r8", 0x08000, 0x8000, 0x9fc7946e );
		ROM_LOAD( "r9", 0x10000, 0x8000, 0xe6212fe4 );
	
		ROM_REGION( 0x0300, REGION_PROMS, 0 );
		ROM_LOAD( "am27s21.pr1", 0x0000, 0x0100, 0x361f0868 );/* palette red bits   */
		ROM_LOAD( "am27s21.pr2", 0x0100, 0x0100, 0xe355da4d );/* palette green bits */
		ROM_LOAD( "am27s21.pr3", 0x0200, 0x0100, 0x8534cfb5 );/* palette blue bits  */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_homo = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );    /* 2*64k for code */
		ROM_LOAD( "homo.01", 0x00000, 0x8000, 0x28c539ad );/* Code 0000-7fff */
		ROM_LOAD( "r2", 0x10000, 0x8000, 0x1ff6e3a2 );/* Paged data */
		ROM_LOAD( "r3", 0x18000, 0x8000, 0xe9eec24a );/* Paged data */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "r10", 0x00000, 0x8000, 0x72e1add1 );
	
		ROM_REGION( 0x18000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "r4", 0x00000, 0x8000, 0x1ab84225 );/* Characters */
		ROM_LOAD( "r5", 0x08000, 0x8000, 0x4997d469 );
		ROM_LOAD( "r6", 0x10000, 0x8000, 0x752904b0 );
	
		ROM_REGION( 0x18000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "r7", 0x00000, 0x8000, 0x362f2a27 );
		ROM_LOAD( "r8", 0x08000, 0x8000, 0x9fc7946e );
		ROM_LOAD( "r9", 0x10000, 0x8000, 0xe6212fe4 );
	
		ROM_REGION( 0x0300, REGION_PROMS, 0 );
		ROM_LOAD( "am27s21.pr1", 0x0000, 0x0100, 0x361f0868 );/* palette red bits   */
		ROM_LOAD( "am27s21.pr2", 0x0100, 0x0100, 0xe355da4d );/* palette green bits */
		ROM_LOAD( "am27s21.pr3", 0x0200, 0x0100, 0x8534cfb5 );/* palette blue bits  */
	ROM_END(); }}; 
	
	
	
	public static InitDriverPtr init_goindol = new InitDriverPtr() { public void handler() 
	{
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
	
	
		/* I hope that's all patches to avoid protection */
	
		rom.write(0x04a7, 0xc9);
		rom.write(0x0641, 0xc9);
		rom.write(0x0831, 0xc9);
		rom.write(0x0b30, 0x00);
		rom.write(0x0c13, 0xc9);
		rom.write(0x134e, 0xc9);
		rom.write(0x172e, 0xc9);
		rom.write(0x1785, 0xc9);
		rom.write(0x17cc, 0xc9);
		rom.write(0x1aa5, 0x7b);
		rom.write(0x1aa6, 0x17);
		rom.write(0x1bee, 0xc9);
		rom.write(0x218c, 0x00);
		rom.write(0x218d, 0x00);
		rom.write(0x218e, 0x00);
		rom.write(0x333d, 0xc9);
		rom.write(0x3365, 0x00);
	} };
	
	public static InitDriverPtr init_homo = new InitDriverPtr() { public void handler() 
	{
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
	
	
		rom.write(0x218c, 0x00);
		rom.write(0x218d, 0x00);
		rom.write(0x218e, 0x00);
	} };
	
	
	
	public static GameDriver driver_goindol	   = new GameDriver("1987"	,"goindol"	,"goindol.java"	,rom_goindol,null	,machine_driver_goindol	,input_ports_goindol	,init_goindol	,ROT90	,	"Sun a Electronics", "Goindol", GAME_NO_COCKTAIL );
	public static GameDriver driver_homo	   = new GameDriver("1987"	,"homo"	,"goindol.java"	,rom_homo,driver_goindol	,machine_driver_goindol	,input_ports_homo	,init_homo	,ROT90	,	"bootleg", "Homo", GAME_NO_COCKTAIL );
}
