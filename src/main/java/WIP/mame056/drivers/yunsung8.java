/***************************************************************************

						  -= Yun Sung 8 Bit Games =-

					driver by	Luca Elia (l.elia@tin.it)


Main  CPU    :  Z80B
Sound CPU    :  Z80A
Video Chips  :	?
Sound Chips  :	OKI M5205 + YM3812

---------------------------------------------------------------------------
Year + Game         Board#
---------------------------------------------------------------------------
95 Cannon Ball      ?
95 Magix / Rock     ?
---------------------------------------------------------------------------

Notes:

- "Magix" can change title to "Rock" through a DSW
- In service mode press Service Coin (e.g. '9')

To Do:

- Better Sound

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
import static mame056.sound.mixerH.*;
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
import static WIP.mame056.vidhrdw.yunsung8.*;
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


public class yunsung8
{
	
	
	public static InitMachinePtr yunsung8_init_machine = new InitMachinePtr() { public void handler() 
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1), 0x24000);
	
		yunsung8_videoram_0 = new UBytePtr(RAM, 0x0000);	// Ram is banked
		yunsung8_videoram_1 = new UBytePtr(RAM, 0x2000);
		yunsung8_videobank_w.handler(0,0);
	} };
	
	
	/***************************************************************************
	
	
								Memory Maps - Main CPU
	
	
	***************************************************************************/
	
	
	public static WriteHandlerPtr yunsung8_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		int bank			=	data & 7;		// ROM bank
		yunsung8_layers_ctrl	=	data & 0x30;	// Layers enable
	
		if ((data & ~0x37)!=0)	logerror("CPU #0 - PC %04X: Bank %02X\n",cpu_get_pc(),data);
	
		if (bank < 3)	RAM = new UBytePtr(RAM, 0x4000 * bank);
		else			RAM = new UBytePtr(RAM, 0x4000 * (bank-3) + 0x10000);
	
		cpu_setbank(1, RAM);
	} };
	
	/*
		Banked Video RAM:
	
		c000-c7ff	Palette	(bit 1 of port 0 switches between 2 banks)
	
		c800-cfff	Color	(bit 0 of port 0 switches between 2 banks)
		d000-dfff	Tiles	""
	*/
	
	public static Memory_ReadAddress yunsung8_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM				),	// ROM
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK1				),	// Banked ROM
		new Memory_ReadAddress( 0xc000, 0xdfff, yunsung8_videoram_r	),	// Video RAM (Banked)
		new Memory_ReadAddress( 0xe000, 0xffff, MRA_RAM				),	// RAM
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress yunsung8_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0000, MWA_ROM				),	// ROM
		new Memory_WriteAddress( 0x0001, 0x0001, yunsung8_bankswitch_w	),	// ROM Bank (again?)
		new Memory_WriteAddress( 0x0002, 0xbfff, MWA_ROM				),	// ROM
		new Memory_WriteAddress( 0xc000, 0xdfff, yunsung8_videoram_w	),	// Video RAM (Banked)
		new Memory_WriteAddress( 0xe000, 0xffff, MWA_RAM				),	// RAM
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort yunsung8_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x00, input_port_0_r		),	// Coins
		new IO_ReadPort( 0x01, 0x01, input_port_1_r		),	// P1
		new IO_ReadPort( 0x02, 0x02, input_port_2_r		),	// P2
		new IO_ReadPort( 0x03, 0x03, input_port_3_r		),	// DSW 1
		new IO_ReadPort( 0x04, 0x04, input_port_4_r		),	// DSW 2
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort yunsung8_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x00, 0x00, yunsung8_videobank_w	),	// Video RAM Bank
		new IO_WritePort( 0x01, 0x01, yunsung8_bankswitch_w	),	// ROM Bank + Layers Enable
		new IO_WritePort( 0x02, 0x02, soundlatch_w			),	// To Sound CPU
		new IO_WritePort( 0x06, 0x06, yunsung8_flipscreen_w	),	// Flip Screen
		new IO_WritePort( 0x07, 0x07, IOWP_NOP				),	// ? (end of IRQ, random value)
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/***************************************************************************
	
	
								Memory Maps - Sound CPU
	
	
	***************************************************************************/
	
	
	static int adpcm;
	
	public static WriteHandlerPtr yunsung8_sound_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU2));
		int bank = data & 7;
	
		if ( bank != (data&(~0x20)) ) 	logerror("CPU #1 - PC %04X: Bank %02X\n",cpu_get_pc(),data);
	
		if (bank < 3)	RAM = new UBytePtr(RAM, 0x4000 * bank);
		else			RAM = new UBytePtr(RAM, 0x4000 * (bank-3) + 0x10000);
	
		cpu_setbank(2, RAM);
	
		MSM5205_reset_w.handler(0,data & 0x20);
	} };
	
	public static WriteHandlerPtr yunsung8_adpcm_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Swap the nibbles */
		adpcm = ((data&0xf)<<4) | ((data >>4)&0xf);
	} };
	
	
	
	public static Memory_ReadAddress yunsung8_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM						),	// ROM
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK2						),	// Banked ROM
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_RAM						),	// RAM
		new Memory_ReadAddress( 0xf800, 0xf800, soundlatch_r					),	// From Main CPU
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress yunsung8_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM						),	// ROM
		new Memory_WriteAddress( 0x8000, 0xbfff, MWA_ROM						),	// Banked ROM
		new Memory_WriteAddress( 0xe000, 0xe000, yunsung8_sound_bankswitch_w	),	// ROM Bank
		new Memory_WriteAddress( 0xe400, 0xe400, yunsung8_adpcm_w				),
		new Memory_WriteAddress( 0xec00, 0xec00, YM3812_control_port_0_w		),	// YM3812
		new Memory_WriteAddress( 0xec01, 0xec01, YM3812_write_port_0_w			),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_RAM						),	// RAM
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	
	/***************************************************************************
	
	
									Input Ports
	
	
	***************************************************************************/
	
	/***************************************************************************
										Magix
	***************************************************************************/
	
	static InputPortPtr input_ports_magix = new InputPortPtr(){ public void handler() { 
	
		PORT_START(); 	// IN0 - Coins
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_START2   );
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_START1   );
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_COIN1    );
	
		PORT_START(); 	// IN1 - Player 1
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER1 );// same as button1 !?
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER1 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
	
		PORT_START(); 	// IN2 - Player 2
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER2 );// same as button1 !?
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER2 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 );
	
		PORT_START(); 	// IN3 - DSW 1
		PORT_SERVICE( 0x01, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x06, 0x06, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Easy" );
		PORT_DIPSETTING(    0x06, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPSETTING(    0x02, "Hardest" );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0xe0, 0xe0, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_4C") );
	
		PORT_START(); 	// IN4 - DSW 2
		PORT_DIPNAME( 0x01, 0x01, "Title" );
		PORT_DIPSETTING(    0x01, "Magix" );
		PORT_DIPSETTING(    0x00, "Rock" );
		PORT_DIPNAME( 0x02, 0x02, "Unknown 2-1" );// the rest seems unused
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, "Unknown 2-2" );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, "Unknown 2-3" );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, "Unknown 2-4" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Unknown 2-5" );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, "Unknown 2-6" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Unknown 2-7" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
	INPUT_PORTS_END(); }}; 
	
	
	/***************************************************************************
									Cannon Ball
	***************************************************************************/
	
	static InputPortPtr input_ports_cannball = new InputPortPtr(){ public void handler() { 
	
		PORT_START(); 	// IN0 - Coins
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_START2   );
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_START1   );
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_COIN1    );
	
		PORT_START(); 	// IN1 - Player 1
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER1 );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER1 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER1 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
	
		PORT_START(); 	// IN2 - Player 2
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER2 );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER2 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER2 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 );
	
		PORT_START(); 	// IN3 - DSW 1
		PORT_SERVICE( 0x01, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x06, 0x06, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Easy" );
		PORT_DIPSETTING(    0x06, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPSETTING(    0x02, "Hardest" );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0xe0, 0xe0, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_4C") );
	
		PORT_START(); 	// IN4 - DSW 2
		PORT_DIPNAME( 0x01, 0x01, "Unknown 2-0" );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, "Unknown 2-1" );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, "Unknown 2-2" );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, "Unknown 2-3" );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, "Unknown 2-4" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Unknown 2-5" );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, "Unknown 2-6" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Unknown 2-7" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
	INPUT_PORTS_END(); }}; 
	
	
	
	/***************************************************************************
	
	
									Graphics Layouts
	
	
	***************************************************************************/
	
	/* 8x8x4 tiles in 2 roms */
	static GfxLayout layout_8x8x4 = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		4,
		new int[] { 
                    //STEP4(0,1) 
                    0, 1, 2, 3
                },
		new int[] { RGN_FRAC(1,2)+1*4,RGN_FRAC(1,2)+0*4,1*4,0*4, RGN_FRAC(1,2)+3*4,RGN_FRAC(1,2)+2*4,3*4,2*4},
		new int[] { 
                    //STEP8(0,16) 
                    0+16*0, 0+16*1, 0+16*2, 0+16*3, 0+16*4, 0+16*5, 0+16*6, 0+16*7
                },
		8*8*4/2
	);
	
	/* 8x8x8 tiles in 4 roms */
	static GfxLayout layout_8x8x8 = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,4),
		8,
		new int[] { 
                    //STEP8(0,1) 
                    0, 1, 2, 3, 4, 5, 6, 7
                },
		new int[] { RGN_FRAC(0,4) + 0*8, RGN_FRAC(1,4) + 0*8, RGN_FRAC(2,4) + 0*8, RGN_FRAC(3,4) + 0*8,
		  RGN_FRAC(0,4) + 1*8, RGN_FRAC(1,4) + 1*8, RGN_FRAC(2,4) + 1*8, RGN_FRAC(3,4) + 1*8 },
		new int[] { 
                    //STEP8(0,16)
                    0+16*0, 0+16*1, 0+16*2, 0+16*3, 0+16*4, 0+16*5, 0+16*6, 0+16*7
                },
		8*8*8/4
	);
	
	static GfxDecodeInfo yunsung8_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, layout_8x8x8, 0, 0x08 ), // [0] Tiles (Background)
		new GfxDecodeInfo( REGION_GFX2, 0, layout_8x8x4, 0,	0x40 ), // [1] Tiles (Text)
		new GfxDecodeInfo( -1 )
	};
	
	
	
	/***************************************************************************
	
	
									Machine Drivers
	
	
	***************************************************************************/
	
        static int toggle=0;
	
	static vclk_interruptPtr yunsung8_adpcm_int = new vclk_interruptPtr() {
            public void handler(int irq) {
                MSM5205_data_w.handler(0,adpcm>>4);
		adpcm<<=4;
	
		toggle ^= 1;
		if (toggle!=0)
			cpu_set_nmi_line(1,PULSE_LINE);
            }
        };
	
	static YM3812interface yunsung8_ym3812_interface = new YM3812interface
	(
		1,
		4000000,	/* ? */
		new int[]{ 50 },
		new WriteYmHandlerPtr[]{ null }
	);
	
	static MSM5205interface yunsung8_msm5205_interface = new MSM5205interface
	(
		1,
		384000,
		new vclk_interruptPtr[]{ yunsung8_adpcm_int },	/* interrupt function */
		new int[]{ MSM5205_S96_4B },		/* 4KHz, 4 Bits */
		new int[]{ 80 }
	);
	
	
	static MachineDriver machine_driver_yunsung8 = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,					/* Z80B */
				8000000,					/* ? */
				yunsung8_readmem,  yunsung8_writemem,
				yunsung8_readport, yunsung8_writeport,
				interrupt, 1	/* No nmi routine */
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,	/* Z80A */
				4000000,					/* ? */
				yunsung8_sound_readmem, yunsung8_sound_writemem,
				null,null,
				interrupt, 1	/* NMI caused by the MSM5205? */
			)
		},
		60,DEFAULT_60HZ_VBLANK_DURATION,
		1,
		yunsung8_init_machine,
	
		/* video hardware */
		512, 256, new rectangle( 0+64, 512-1, 0+8, 256-8-1 ),
		yunsung8_gfxdecodeinfo,
		2048, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		yunsung8_vh_start,
		null,
		yunsung8_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_YM3812,  yunsung8_ym3812_interface  ),
			new MachineSound( SOUND_MSM5205, yunsung8_msm5205_interface )
		}
	);
	
	
	/***************************************************************************
	
	
									ROMs Loading
	
	
	***************************************************************************/
	
	/***************************************************************************
	
										Magix
	
	Yun Sung, 1995.
	CPU : Z80B
	SND : Z80A + YM3812 + Oki M5205
	OSC : 16.000
	
	***************************************************************************/
	
	static RomLoadPtr rom_magix = new RomLoadPtr(){ public void handler(){ 
	
		ROM_REGION( 0x24000+0x4000, REGION_CPU1, 0 );	/* Main Z80 Code */
		ROM_LOAD( "yunsung8.07", 0x00000, 0x0c000, 0xd4d0b68b );
		ROM_CONTINUE(         0x10000, 0x14000             );
		/* $2000 bytes for bank 0 of video ram (text) */
		/* $2000 bytes for bank 1 of video ram (background) */
	
		ROM_REGION( 0x24000, REGION_CPU2, 0 );	/* Sound Z80 Code */
		ROM_LOAD( "yunsung8.08", 0x00000, 0x0c000, 0x6fd60be9 );
		ROM_CONTINUE(         0x10000, 0x14000             );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Background */
		ROM_LOAD( "yunsung8.04",  0x000000, 0x80000, 0x0a100d2b );
		ROM_LOAD( "yunsung8.03",  0x080000, 0x80000, 0xc8cb0373 );
		ROM_LOAD( "yunsung8.02",  0x100000, 0x80000, 0x09efb8e5 );
		ROM_LOAD( "yunsung8.01",  0x180000, 0x80000, 0x4590d782 );
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );/* Text */
		ROM_LOAD( "yunsung8.05", 0x00000, 0x20000, 0x862d378c );// only first $8000 bytes != 0
		ROM_LOAD( "yunsung8.06", 0x20000, 0x20000, 0x8b2ab901 );// only first $8000 bytes != 0
	
	ROM_END(); }}; 
	
	
	/***************************************************************************
	
									Cannon Ball
	
	01, 02, 03, 04  are 27c020
	05, 06, 07, 08  are 27c010
	2 pals used
	
	Z80b PROGRAM, Z80b SOUND
	
	Cy7c384A
	16mhz
	
	***************************************************************************/
	
	static RomLoadPtr rom_cannball = new RomLoadPtr(){ public void handler(){ 
	
		ROM_REGION( 0x24000+0x4000, REGION_CPU1, 0 );	/* Main Z80 Code */
		ROM_LOAD( "cannball.07", 0x00000, 0x0c000, 0x17db56b4 );
		ROM_CONTINUE(            0x10000, 0x14000             );
		/* $2000 bytes for bank 0 of video ram (text) */
		/* $2000 bytes for bank 1 of video ram (background) */
	
		ROM_REGION( 0x24000, REGION_CPU2, 0 );	/* Sound Z80 Code */
		ROM_LOAD( "cannball.08", 0x00000, 0x0c000, 0x11403875 );
		ROM_CONTINUE(            0x10000, 0x14000             );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Background */
		ROM_LOAD( "cannball.01",  0x000000, 0x40000, 0x2d7785e4 );
		ROM_LOAD( "cannball.02",  0x040000, 0x40000, 0x24df387e );
		ROM_LOAD( "cannball.03",  0x080000, 0x40000, 0x4d62f192 );
		ROM_LOAD( "cannball.04",  0x0c0000, 0x40000, 0x37cf8b12 );
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );/* Text */
		ROM_LOAD( "cannball.05", 0x00000, 0x20000, 0x87c1f1fa );
		ROM_LOAD( "cannball.06", 0x20000, 0x20000, 0xe722bee8 );
	
	ROM_END(); }}; 
	
	
	/***************************************************************************
	
	
									Game Drivers
	
	
	***************************************************************************/
	
	public static GameDriver driver_cannball	   = new GameDriver("1995"	,"cannball"	,"yunsung8.java"	,rom_cannball,null	,machine_driver_yunsung8	,input_ports_cannball	,null	,ROT0	,	"Yun Sung / Soft Vision", "Cannon Ball",  GAME_IMPERFECT_SOUND );
	public static GameDriver driver_magix	   = new GameDriver("1995"	,"magix"	,"yunsung8.java"	,rom_magix,null	,machine_driver_yunsung8	,input_ports_magix	,null	,ROT0	,	"Yun Sung",               "Magix / Rock", GAME_IMPERFECT_SOUND ); // Title: DSW
}
