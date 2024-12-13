/***************************************************************************

Hexion (GX122) (c) 1992 Konami

driver by Nicola Salmoria

Notes:
- There are probably palette PROMs missing. Palette data doesn't seem to be
  written anywhere in RAM.
- The board has a 052591, which is used for protection in Thunder Cross and
  S.P.Y. IN this game, however, it doesn't seem to do much, except maybe
  clear the screen.
- during startup, some garbage is written to video RAM. This is probably
  supposed to go somewhere else, maybe the 052591. It doesn't look like
  palette data.

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
import static WIP.mame056.vidhrdw.hexion.*;
import static mame056.sound.ay8910.*;
import mame056.sound.ay8910H.AY8910interface;
import static arcadeflex056.osdepend.logerror;

import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.sound._3526intf.*;
import static mame056.sound._3812intfH.*;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;
import static mame056.sound.k051649.*;
import mame056.sound.k051649H.k051649_interface;
import static mame056.sound.oki6295.*;
import mame056.sound.oki6295H.OKIM6295interface;

import static mame056.vidhrdw.generic.*;

public class hexion
{
	
	public static WriteHandlerPtr coincntr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror("%04x: coincntr_w %02x\n",cpu_get_pc(),data);
	
		/* bits 0/1 = coin counters */
		coin_counter_w.handler(0,data & 0x01);
		coin_counter_w.handler(1,data & 0x02);
	
		/* bit 5 = flip screen */
		flip_screen_set(data & 0x20);
	
		/* other bit unknown */
/*TODO*///	if ((data & 0xdc) != 0x10) usrintf_showmessage("coincntr %02x",data);
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x9fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xa000, 0xbfff, MRA_RAM ),
		new Memory_ReadAddress( 0xc000, 0xdffe, hexion_bankedram_r ),
		new Memory_ReadAddress( 0xf400, 0xf400, input_port_0_r ),
		new Memory_ReadAddress( 0xf401, 0xf401, input_port_1_r ),
		new Memory_ReadAddress( 0xf402, 0xf402, input_port_3_r ),
		new Memory_ReadAddress( 0xf403, 0xf403, input_port_4_r ),
		new Memory_ReadAddress( 0xf440, 0xf440, input_port_2_r ),
		new Memory_ReadAddress( 0xf441, 0xf441, input_port_5_r ),
		new Memory_ReadAddress( 0xf540, 0xf540, watchdog_reset_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0xa000, 0xbfff, MWA_RAM ),
		new Memory_WriteAddress( 0xc000, 0xdffe, hexion_bankedram_w ),
		new Memory_WriteAddress( 0xdfff, 0xdfff, hexion_bankctrl_w ),
		new Memory_WriteAddress( 0xe800, 0xe87f, K051649_waveform_w ),
		new Memory_WriteAddress( 0xe880, 0xe889, K051649_frequency_w ),
		new Memory_WriteAddress( 0xe88a, 0xe88e, K051649_volume_w ),
		new Memory_WriteAddress( 0xe88f, 0xe88f, K051649_keyonoff_w ),
		new Memory_WriteAddress( 0xf000, 0xf00f, MWA_NOP ),	/* 053252? f00e = IRQ ack, f00f = NMI ack */
		new Memory_WriteAddress( 0xf200, 0xf200, OKIM6295_data_0_w ),
		new Memory_WriteAddress( 0xf480, 0xf480, hexion_bankswitch_w ),
		new Memory_WriteAddress( 0xf4c0, 0xf4c0, coincntr_w ),
		new Memory_WriteAddress( 0xf500, 0xf500, hexion_gfxrom_select_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_hexion = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x02, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x0e, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0x0d, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0b, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x0a, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x09, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0xf0, 0xf0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x20, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0xf0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x70, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0xd0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0xb0, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x90, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x70, 0x70, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x70, "Easiest" );
		PORT_DIPSETTING(    0x60, "Very Easy" );
		PORT_DIPSETTING(    0x50, "Easy" );
		PORT_DIPSETTING(    0x40, "Medium" );
		PORT_DIPSETTING(    0x30, "Medium Hard" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x10, "Very Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(	0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_SPECIAL );/* 052591? game waits for it to be 0 */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { RGN_FRAC(1,2)+0*4, RGN_FRAC(1,2)+1*4, 0*4, 1*4, RGN_FRAC(1,2)+2*4, RGN_FRAC(1,2)+3*4, 2*4, 3*4 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
		16*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 0, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static OKIM6295interface okim6295_interface = new OKIM6295interface
	(
		1,                  /* 1 chip */
		new int[]{ 8000 },           /* 8000Hz frequency */
		new int[]{ REGION_SOUND1 },	/* memory region */
		new int[]{ 100 }
	);
	
	static k051649_interface k051649_interface = new k051649_interface
	(
		24000000/16,	/* Clock */
		100			/* Volume */
	);
	
	
	
	public static InterruptPtr hexion_interrupt = new InterruptPtr() { public int handler() 
	{
		/* NMI handles start and coin inputs, origin unknown */
		if (cpu_getiloops() != 0)
			return nmi_interrupt.handler();
		else
			return interrupt.handler();
	} };
	
	static MachineDriver machine_driver_hexion = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				24000000/4,	/* Z80B 6 MHz */
				readmem,writemem,null,null,
				hexion_interrupt,3	/* both IRQ and NMI are used */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		null,
	
		/* video hardware */
		64*8, 32*8, new rectangle( 0*8, 64*8-1, 0*8, 32*8-1 ),
		gfxdecodeinfo,
		256, 0,
		palette_RRRR_GGGG_BBBB_convert_prom,
	
		VIDEO_TYPE_RASTER | VIDEO_PIXEL_ASPECT_RATIO_1_2,
		null,
		hexion_vh_start,
		null,
		hexion_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_OKIM6295,
				okim6295_interface
			),
			new MachineSound(
				SOUND_K051649,
				k051649_interface
			)
		}
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_hexion = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x34800, REGION_CPU1, 0 );/* ROMs + space for additional RAM */
		ROM_LOAD( "122jab01.bin", 0x00000, 0x20000, 0xeabc6dd1 );
		ROM_RELOAD(               0x10000, 0x20000 );/* banked at 8000-9fff */
	
		ROM_REGION( 0x80000, REGION_GFX1, 0 );/* addressable by the main CPU */
		ROM_LOAD( "122a07.bin",   0x00000, 0x40000, 0x22ae55e3 );
		ROM_LOAD( "122a06.bin",   0x40000, 0x40000, 0x438f4388 );
	
		ROM_REGION( 0x40000, REGION_SOUND1, 0 );/* OKIM6295 samples */
		ROM_LOAD( "122a05.bin",   0x0000, 0x40000, 0xbcc831bf );
	
		ROM_REGION( 0x0300, REGION_PROMS, 0 );
		ROM_LOAD( "proms",        0x0000, 0x0300, 0x00000000 );
	ROM_END(); }}; 
	
	
	public static InitDriverPtr init_hexion = new InitDriverPtr() { public void handler()
	{
		int col,i;
		UBytePtr prom = new UBytePtr(memory_region(REGION_PROMS));
	
		prom.write(1+0x000,  17/16);
		prom.write(1+0x100,  37/16);
		prom.write(1+0x200,  170/16);
		prom.write(4+0x000,  100/16);
		prom.write(4+0x100,  100/16);
		prom.write(4+0x200,  100/16);
		prom.write(5+0x000,  206/16);
		prom.write(5+0x100,  16/16);
		prom.write(5+0x200,  16/16);
		prom.write(6+0x000,  160/16);
		prom.write(6+0x100,  16/16);
		prom.write(6+0x200,  16/16);
		prom.write(7+0x000,  0/16);
		prom.write(7+0x100,  216/16);
		prom.write(7+0x200,  254/16);
		prom.write(8+0x000,  132/16);
		prom.write(8+0x100,  237/16);
		prom.write(8+0x200,  243/16);
		prom.write(9+0x000,  157/16);
		prom.write(9+0x100,  255/16);
		prom.write(9+0x200,  255/16);
		prom.write(10+0x000,  200/16);
		prom.write(10+0x100,  30/16);
		prom.write(10+0x200,  110/16);
		prom.write(11+0x000,  230/16);
		prom.write(11+0x100,  80/16);
		prom.write(11+0x200,  120/16);
		prom.write(12+0x000,  230/16);
		prom.write(12+0x100,  104/16);
		prom.write(12+0x200,  140/16);
		prom.write(13+0x000,  90/16);
		prom.write(13+0x100,  104/16);
		prom.write(13+0x200,  190/16);
		prom.write(14+0x000,  192/16);
		prom.write(14+0x100,  222/16);
		prom.write(14+0x200,  255/16);
		prom.write(15+0x000,  255/16);
		prom.write(15+0x100,  255/16);
		prom.write(15+0x200,  255/16);
		prom.write(1*16+1+0x000,  17/16);
		prom.write(1*16+1+0x100,  37/16);
		prom.write(1*16+1+0x200,  170/16);
		prom.write(1*16+8+0x000,  216/16);
		prom.write(1*16+8+0x100,  221/16);
		prom.write(1*16+8+0x200,  167/16);
		prom.write(1*16+14+0x000,  183/16);
		prom.write(1*16+14+0x100,  162/16);
		prom.write(1*16+14+0x200,  238/16);
		prom.write(2*16+1+0x000,  17/16);
		prom.write(2*16+1+0x100,  37/16);
		prom.write(2*16+1+0x200,  170/16);
		prom.write(2*16+14+0x000,  117/16);
		prom.write(2*16+14+0x100,  212/16);
		prom.write(2*16+14+0x200,  255/16);
	
		col = 0x05;
		for (i = 1;i < 16;i++)
		{
			prom.write(col*16+i+0x000,  (80+i*(255-80)/15)/16);
			prom.write(col*16+i+0x100,  (150+i*(255-150)/15)/16);
			prom.write(col*16+i+0x200,  (60+i*(255-60)/15)/16);
		}
		col = 0x0c;
		for (i = 0;i < 16;i++)
		{
			prom.write(col*16+i+0x000,  i);
			prom.write(col*16+i+0x100,  i);
			prom.write(col*16+i+0x200,  i);
		}
		col = 0x0d;
		for (i = 0;i < 16;i++)
		{
			prom.write(col*16+i+0x000,  i);
			prom.write(col*16+i+0x100,  0);
			prom.write(col*16+i+0x200,  0);
		}
		col = 0x0e;
		for (i = 0;i < 16;i++)
		{
			prom.write(col*16+i+0x000,  0);
			prom.write(col*16+i+0x100,  i);
			prom.write(col*16+i+0x200,  0);
		}
		col = 0x0f;
		for (i = 0;i < 16;i++)
		{
			prom.write(col*16+i+0x000,  0);
			prom.write(col*16+i+0x100,  0);
			prom.write(col*16+i+0x200,  i);
		}
	
		for (col = 0;col < 16;col++)
		{
			prom.write(col*16+0x000,  0);
			prom.write(col*16+0x100,  0);
			prom.write(col*16+0x200,  0);
		}
	} };
	
	
	public static GameDriver driver_hexion	   = new GameDriver("1992"	,"hexion"	,"hexion.java"	,rom_hexion,null	,machine_driver_hexion	,input_ports_hexion	,init_hexion	,ROT0	,	"Konami", "Hexion (Japan)", GAME_WRONG_COLORS );
}
