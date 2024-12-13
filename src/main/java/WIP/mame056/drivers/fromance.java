/******************************************************************************

	Game Driver for Video System Mahjong series.

	Idol-Mahjong Final Romance
	(c)1991 Video System Co.,Ltd.

	Nekketsu Mahjong Sengen! AFTER 5
	(c)1991 Video System Co.,Ltd.

	Mahjong Daiyogen
	(c)1990 Video System Co.,Ltd.

	Mahjong Fun Club - Idol Saizensen
	(c)1989 Video System Co.,Ltd.

	Mahjong Natsu Monogatari (Mahjong Summer Story)
	(c)1989 Video System Co.,Ltd.

	Idol-Mahjong Housoukyoku
	(c)1988 System Service Co.,Ltd.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 2001/02/04 -
	and Nicola Salmoria, Aaron Giles

******************************************************************************/
/******************************************************************************
Memo:

- 2player's input is not supported.

- Communication between MAIN CPU and SUB CPU can be wrong.

******************************************************************************/

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
import static WIP.mame056.vidhrdw.fromance.*;
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
import static mame056.inputH.*;
import static mame056.mame.Machine;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;

import static mame056.sound.MSM5205.*;
import static mame056.sound.MSM5205H.*;
import static mame056.sound._2413intf.*;
import static mame056.sound._2413intfH.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

import static mame056.vidhrdw.generic.*;

public class fromance
{
	
	
	/* Local variables */
	static int fromance_directionflag;
	static int fromance_commanddata;
	static int fromance_portselect;
	
	static int fromance_adpcm_reset;
	static int fromance_adpcm_data;
	static int fromance_vclk_left;
	
	
	
	/*************************************
	 *
	 *	Machine init
	 *
	 *************************************/
	
	public static InitMachinePtr fromance_init_machine = new InitMachinePtr() { public void handler()
	{
		fromance_directionflag = 0;
		fromance_commanddata = 0;
		fromance_portselect = 0;
	
		fromance_adpcm_reset = 0;
		fromance_adpcm_data = 0;
		fromance_vclk_left = 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Master/slave communication
	 *
	 *************************************/
	
	public static ReadHandlerPtr fromance_commanddata_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return fromance_commanddata;
	} };
	
	
	static timer_callback deferred_commanddata_w = new timer_callback() {
            public void handler(int data) {
                fromance_commanddata = data;
		fromance_directionflag = 1;
            }
        };
	
	
	public static WriteHandlerPtr fromance_commanddata_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* do this on a timer to let the slave CPU synchronize */
		timer_set(TIME_NOW, data, deferred_commanddata_w);
	} };
	
	
	public static ReadHandlerPtr fromance_busycheck_main_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* set a timer to force synchronization after the read */
		timer_set(TIME_NOW, 0, null);
	
		if (fromance_directionflag == 0) return 0x00;		// standby
		else return 0xff;								// busy
	} };
	
	
	public static ReadHandlerPtr fromance_busycheck_sub_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (fromance_directionflag != 0) return 0xff;		// standby
		else return 0x00;								// busy
	} };
	
	
	public static WriteHandlerPtr fromance_busycheck_sub_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		fromance_directionflag = 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Slave CPU ROM banking
	 *
	 *************************************/
	
	public static WriteHandlerPtr fromance_rombank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr ROM = new UBytePtr(memory_region(REGION_CPU2));
	
		cpu_setbank(1, new UBytePtr(ROM, 0x010000 + (0x4000 * data)));
	} };
	
	
	
	/*************************************
	 *
	 *	ADPCM interface
	 *
	 *************************************/
	
	public static WriteHandlerPtr fromance_adpcm_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		fromance_adpcm_reset = data;
		fromance_vclk_left = 0;
	
		MSM5205_reset_w.handler(0, (data & 0x01)!=0?0:1);
	} };
	
	
	public static WriteHandlerPtr fromance_adpcm_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		fromance_adpcm_data = data;
		fromance_vclk_left = 2;
	} };
	
	
	static vclk_interruptPtr fromance_adpcm_int = new vclk_interruptPtr() {
            public void handler(int irq) {
                /* skip if we're reset */
		if (fromance_adpcm_reset == 0)
			return;
	
		/* clock the data through */
		if (fromance_vclk_left != 0)
		{
			MSM5205_data_w.handler(0, (fromance_adpcm_data >> 4));
			fromance_adpcm_data <<= 4;
			fromance_vclk_left--;
		}
	
		/* generate an NMI if we're out of data */
		if (fromance_vclk_left == 0)
			cpu_set_nmi_line(1, PULSE_LINE);
            }
        };
	
	
	/*************************************
	 *
	 *	Input handlers
	 *
	 *************************************/
	
	public static WriteHandlerPtr fromance_portselect_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		fromance_portselect = data;
	} };
	
	
	public static ReadHandlerPtr fromance_keymatrix_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = 0xff;
	
		if ((fromance_portselect & 0x01) != 0)
			ret &= readinputport(4);
		if ((fromance_portselect & 0x02) != 0)
			ret &= readinputport(5);
		if ((fromance_portselect & 0x04) != 0)
			ret &= readinputport(6);
		if ((fromance_portselect & 0x08) != 0)
			ret &= readinputport(7);
		if ((fromance_portselect & 0x10) != 0)
			ret &= readinputport(8);
	
		return ret;
	} };
	
	
	
	/*************************************
	 *
	 *	Coin counters
	 *
	 *************************************/
	
	public static WriteHandlerPtr fromance_coinctr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//
	} };
	
	
	
	/*************************************
	 *
	 *	Master CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress fromance_readmem_main[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xdfff, MRA_RAM ),
		new Memory_ReadAddress( 0x9e89, 0x9e89, MRA_NOP ),			// unknown (idolmj)
		new Memory_ReadAddress( 0xe000, 0xe000, input_port_0_r ),
		new Memory_ReadAddress( 0xe001, 0xe001, fromance_keymatrix_r ),
		new Memory_ReadAddress( 0xe002, 0xe002, input_port_1_r ),
		new Memory_ReadAddress( 0xe003, 0xe003, fromance_busycheck_main_r ),
		new Memory_ReadAddress( 0xe004, 0xe004, input_port_3_r ),
		new Memory_ReadAddress( 0xe005, 0xe005, input_port_2_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress fromance_writemem_main[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xdfff, MWA_RAM ),
		new Memory_WriteAddress( 0xe000, 0xe000, fromance_portselect_w ),
		new Memory_WriteAddress( 0xe002, 0xe002, fromance_coinctr_w ),
		new Memory_WriteAddress( 0xe003, 0xe003, fromance_commanddata_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Slave CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress fromance_readmem_sub[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc000, 0xc7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xc800, 0xcfff, fromance_paletteram_r ),
		new Memory_ReadAddress( 0xd000, 0xffff, fromance_videoram_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress fromance_writemem_sub[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xc800, 0xcfff, fromance_paletteram_w ),
		new Memory_WriteAddress( 0xd000, 0xffff, fromance_videoram_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Slave CPU port handlers
	 *
	 *************************************/
	
	public static IO_ReadPort fromance_readport_sub[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x12, 0x12, IORP_NOP ),				// unknown
		new IO_ReadPort( 0x21, 0x21, fromance_busycheck_sub_r ),
		new IO_ReadPort( 0x26, 0x26, fromance_commanddata_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort idolmj_writeport_sub[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x10, 0x10, fromance_crtc_data_w ),
		new IO_WritePort( 0x11, 0x11, fromance_crtc_register_w ),
		new IO_WritePort( 0x20, 0x20, fromance_rombank_w ),
		new IO_WritePort( 0x21, 0x21, fromance_gfxreg_w ),
		new IO_WritePort( 0x22, 0x25, fromance_scroll_w ),
		new IO_WritePort( 0x26, 0x26, fromance_busycheck_sub_w ),
		new IO_WritePort( 0x27, 0x27, fromance_adpcm_reset_w ),
		new IO_WritePort( 0x28, 0x28, fromance_adpcm_w ),
		new IO_WritePort( 0x29, 0x29, AY8910_write_port_0_w ),
		new IO_WritePort( 0x2a, 0x2a, AY8910_control_port_0_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort fromance_writeport_sub[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x10, 0x10, fromance_crtc_data_w ),
		new IO_WritePort( 0x11, 0x11, fromance_crtc_register_w ),
		new IO_WritePort( 0x20, 0x20, fromance_rombank_w ),
		new IO_WritePort( 0x21, 0x21, fromance_gfxreg_w ),
		new IO_WritePort( 0x22, 0x25, fromance_scroll_w ),
		new IO_WritePort( 0x26, 0x26, fromance_busycheck_sub_w ),
		new IO_WritePort( 0x27, 0x27, fromance_adpcm_reset_w ),
		new IO_WritePort( 0x28, 0x28, fromance_adpcm_w ),
		new IO_WritePort( 0x2a, 0x2a, YM2413_register_port_0_w ),
		new IO_WritePort( 0x2b, 0x2b, YM2413_data_port_0_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	public static void FROMANCE_KEYMATRIX1() {
		PORT_START();  
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 A", KEYCODE_A, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 E", KEYCODE_E, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 I", KEYCODE_I, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 M", KEYCODE_M, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Kan", KEYCODE_LCONTROL, IP_JOY_NONE );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void FROMANCE_KEYMATRIX2() {
		PORT_START();  
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 B", KEYCODE_B, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 F", KEYCODE_F, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 J", KEYCODE_J, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 N", KEYCODE_N, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Reach", KEYCODE_LSHIFT, IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 Bet", KEYCODE_2, IP_JOY_NONE );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void FROMANCE_KEYMATRIX3() {
		PORT_START();  
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 C", KEYCODE_C, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 G", KEYCODE_G, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 K", KEYCODE_K, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Chi", KEYCODE_SPACE, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Ron", KEYCODE_Z, IP_JOY_NONE );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void FROMANCE_KEYMATRIX4() {
		PORT_START();  
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 D", KEYCODE_D, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 H", KEYCODE_H, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 L", KEYCODE_L, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Pon", KEYCODE_LALT, IP_JOY_NONE );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void FROMANCE_KEYMATRIX5() {
		PORT_START();  
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 Last Chance", KEYCODE_RALT, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 Take Score", KEYCODE_RCONTROL, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 Double Up", KEYCODE_RSHIFT, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Flip", KEYCODE_X, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Big", KEYCODE_ENTER, IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 Small", KEYCODE_BACKSPACE, IP_JOY_NONE );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	
	static InputPortPtr input_ports_idolmj = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) TEST SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BITX( 0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );	// TEST
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) COIN SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-1 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 1-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 1-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 1-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "Voices" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 1-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 1-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "DIPSW 1-8" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START(); 	/* (3) DIPSW-2 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 2-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 2-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 2-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		FROMANCE_KEYMATRIX1();	/* (4) PORT 1-0 */
		FROMANCE_KEYMATRIX2();	/* (5) PORT 1-1 */
		FROMANCE_KEYMATRIX3();	/* (6) PORT 1-2 */
		FROMANCE_KEYMATRIX4();	/* (7) PORT 1-3 */
		FROMANCE_KEYMATRIX5();	/* (8) PORT 1-4 */
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_fromance = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) TEST SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BITX( 0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );	// TEST
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) COIN SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-1 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 1-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 1-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 1-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 1-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 1-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START(); 	/* (3) DIPSW-2 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 2-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 2-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 2-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		FROMANCE_KEYMATRIX1();	/* (4) PORT 1-0 */
		FROMANCE_KEYMATRIX2();	/* (5) PORT 1-1 */
		FROMANCE_KEYMATRIX3();	/* (6) PORT 1-2 */
		FROMANCE_KEYMATRIX4();	/* (7) PORT 1-3 */
		FROMANCE_KEYMATRIX5();	/* (8) PORT 1-4 */
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_nmsengen = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) TEST SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BITX( 0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );	// TEST
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) COIN SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-1 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 1-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 1-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 1-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 1-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 1-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START(); 	/* (3) DIPSW-2 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 2-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 2-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 2-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		FROMANCE_KEYMATRIX1();	/* (4) PORT 1-0 */
		FROMANCE_KEYMATRIX2();	/* (5) PORT 1-1 */
		FROMANCE_KEYMATRIX3();	/* (6) PORT 1-2 */
		FROMANCE_KEYMATRIX4();	/* (7) PORT 1-3 */
		FROMANCE_KEYMATRIX5();	/* (8) PORT 1-4 */
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_daiyogen = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) TEST SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BITX( 0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );	// TEST
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) COIN SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-1 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 1-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 1-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 1-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 1-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 1-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "DIPSW 1-8" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START(); 	/* (3) DIPSW-2 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 2-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 2-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 2-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		FROMANCE_KEYMATRIX1();	/* (4) PORT 1-0 */
		FROMANCE_KEYMATRIX2();	/* (5) PORT 1-1 */
		FROMANCE_KEYMATRIX3();	/* (6) PORT 1-2 */
		FROMANCE_KEYMATRIX4();	/* (7) PORT 1-3 */
		FROMANCE_KEYMATRIX5();	/* (8) PORT 1-4 */
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_mfunclub = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) TEST SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BITX( 0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );	// TEST
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) COIN SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-1 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 1-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 1-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 1-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "Voices" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 1-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 1-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "DIPSW 1-8" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START(); 	/* (3) DIPSW-2 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 2-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 2-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 2-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		FROMANCE_KEYMATRIX1();	/* (4) PORT 1-0 */
		FROMANCE_KEYMATRIX2();	/* (5) PORT 1-1 */
		FROMANCE_KEYMATRIX3();	/* (6) PORT 1-2 */
		FROMANCE_KEYMATRIX4();	/* (7) PORT 1-3 */
		FROMANCE_KEYMATRIX5();	/* (8) PORT 1-4 */
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_mjnatsu = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) TEST SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BITX( 0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );	// TEST
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) COIN SW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-1 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 1-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 1-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 1-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "Voices" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 1-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 1-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "DIPSW 1-8" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START(); 	/* (3) DIPSW-2 */
		PORT_DIPNAME( 0x01, 0x00, "DIPSW 2-1" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIPSW 2-2" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIPSW 2-3" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIPSW 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIPSW 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIPSW 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIPSW 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		FROMANCE_KEYMATRIX1();	/* (4) PORT 1-0 */
		FROMANCE_KEYMATRIX2();	/* (5) PORT 1-1 */
		FROMANCE_KEYMATRIX3();	/* (6) PORT 1-2 */
		FROMANCE_KEYMATRIX4();	/* (7) PORT 1-3 */
		FROMANCE_KEYMATRIX5();	/* (8) PORT 1-4 */
	INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Graphics definitions
	 *
	 *************************************/
	
	static GfxLayout bglayout = new GfxLayout
	(
		8,4,
		RGN_FRAC(1,1),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { 4, 0, 12, 8, 20, 16, 28, 24 },
		new int[] { 0*32, 1*32, 2*32, 3*32 },
		16*8
	);
	
	
	static GfxDecodeInfo fromance_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, bglayout,   0, 128 ),
		new GfxDecodeInfo( REGION_GFX2, 0, bglayout,   0, 128 ),
		new GfxDecodeInfo( -1 )
	};
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static YM2413interface ym2413_interface = new YM2413interface
	(
		1,						/* 1 chip */
		3579545*2,				/* 3.579545 MHz ? */
		new int[]{ 100 }				/* Volume */
	);
	
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		1,						/* 1 chip */
		12000000/6,				/* 1.5 MHz ? */
		new int[] { 15 },					/* volume */
		new ReadHandlerPtr[] { null },					/* read port #0 */
		new ReadHandlerPtr[] { null },					/* read port #1 */
		new WriteHandlerPtr[] { null },					/* write port #0 */
		new WriteHandlerPtr[] { null }					/* write port #1 */
	);
	
	
	static MSM5205interface msm5205_interface = new MSM5205interface
	(
		1,						/* 1 chip */
		384000,					/* 384 KHz */
		new vclk_interruptPtr[]{ fromance_adpcm_int },	/* IRQ handler */
		new int[]{ MSM5205_S48_4B },		/* 8 KHz */
		new int[]{ 80 }					/* volume */
	);
	
	static MSM5205interface fromance_msm5205_interface = new MSM5205interface
	(
		1,						/* 1 chip */
		384000,					/* 384 KHz */
		new vclk_interruptPtr[]{ fromance_adpcm_int },	/* IRQ handler */
		new int[]{ MSM5205_S48_4B },		/* 8 KHz */
		new int[]{ 20 }					/* volume */
	);
	
	
	/*************************************
	 *
	 *	Machine drivers
	 *
	 *************************************/
	
	static MachineDriver machine_driver_idolmj = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				12000000/2,		/* 6.00 Mhz ? */
				fromance_readmem_main,fromance_writemem_main, null, null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				12000000/2,		/* 6.00 Mhz ? */
				fromance_readmem_sub,fromance_writemem_sub,fromance_readport_sub,idolmj_writeport_sub,
				ignore_interrupt,1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		fromance_init_machine,
	
		/* video hardware */
		512, 256, new rectangle( 0, 352-1, 0, 240-1 ),
		fromance_gfxdecodeinfo,
		2048, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		fromance_vh_start,
		fromance_vh_stop,
		fromance_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
                                SOUND_AY8910, 
                                ay8910_interface 
                        ),
			new MachineSound(
                                SOUND_MSM5205, 
                                msm5205_interface 
                        )
		}
	);
	
	
	static MachineDriver machine_driver_fromance = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				12000000/2,		/* 6.00 Mhz ? */
				fromance_readmem_main,fromance_writemem_main,null,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				12000000/2,		/* 6.00 Mhz ? */
				fromance_readmem_sub,fromance_writemem_sub,fromance_readport_sub,fromance_writeport_sub,
				ignore_interrupt,1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		fromance_init_machine,
	
		/* video hardware */
		512, 256, new rectangle( 0, 352-1, 0, 240-1 ),
		fromance_gfxdecodeinfo,
		2048, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		fromance_vh_start,
		fromance_vh_stop,
		fromance_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
                                SOUND_YM2413, 
                                ym2413_interface 
                        ),
			new MachineSound(
                                SOUND_MSM5205, 
                                fromance_msm5205_interface
                        )
		}
	);
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_idolmj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x010000, REGION_CPU1, 0 );
		ROM_LOAD( "3-13g.bin", 0x000000, 0x008000, 0x910e9e7a );
	
		ROM_REGION( 0x410000, REGION_CPU2, 0 );
		ROM_LOAD( "5-13e.bin", 0x000000, 0x008000, 0xcda33264 );
		ROM_LOAD( "18e.bin",   0x010000, 0x080000, 0x7ee5aaf3 );
		ROM_LOAD( "17e.bin",   0x090000, 0x080000, 0x38055f94 );
		ROM_LOAD( "4-14e.bin", 0x190000, 0x010000, 0x84d80b43 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "6e.bin",    0x000000, 0x080000, 0x51dadedd );
		ROM_LOAD( "2-8e.bin",  0x080000, 0x008000, 0xa1a62c4c );
		ROM_FILL(              0x088000, 0x008000, 0xff );
		ROM_FILL(              0x090000, 0x170000, 0xff );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "3e.bin",    0x000000, 0x080000, 0xeff9b562 );
		ROM_LOAD( "1-1e.bin",  0x080000, 0x008000, 0xabf03c62 );
		ROM_FILL(              0x088000, 0x008000, 0xff );
		ROM_FILL(              0x090000, 0x170000, 0xff );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_mjnatsu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x010000, REGION_CPU1, 0 );
		ROM_LOAD( "3-ic70.bin", 0x000000, 0x008000, 0x543eb9e1 );
	
		ROM_REGION( 0x410000, REGION_CPU2, 0 );
		ROM_LOAD( "4-ic47.bin", 0x000000, 0x008000, 0x27a61dc7 );
		ROM_LOAD( "ic87.bin",   0x010000, 0x080000, 0xcaec9310 );
		ROM_LOAD( "ic78.bin",   0x090000, 0x080000, 0x2b291006 );
		ROM_LOAD( "ic72.bin",   0x110000, 0x020000, 0x42464fba );
		ROM_LOAD( "5-ic48.bin", 0x210000, 0x010000, 0xd3c06cd9 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "ic58.bin",   0x000000, 0x080000, 0x257a8075 );
		ROM_LOAD( "ic63.bin",   0x080000, 0x020000, 0xb54c7d3a );
		ROM_LOAD( "1-ic74.bin", 0x0a0000, 0x008000, 0xfbafa46b );
		ROM_FILL(               0x0a8000, 0x008000, 0xff );
		ROM_FILL(               0x0b0000, 0x150000, 0xff );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "ic59.bin",   0x000000, 0x080000, 0x03983ac7 );
		ROM_LOAD( "ic64.bin",   0x080000, 0x040000, 0x9bd8e855 );
		ROM_FILL(               0x0c0000, 0x140000, 0xff );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_mfunclub = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x010000, REGION_CPU1, 0 );
		ROM_LOAD( "3.70",        0x000000, 0x008000, 0xe6f76ca3 );
	
		ROM_REGION( 0x410000, REGION_CPU2, 0 );
		ROM_LOAD( "4.47",        0x000000, 0x008000, 0xd71ee0e3 );
		ROM_LOAD( "586.87",      0x010000, 0x080000, 0xe197af4a );
		ROM_LOAD( "587.78",      0x090000, 0x080000, 0x08ff39c3 );
		ROM_LOAD( "5.57",        0x290000, 0x010000, 0xbf988bde );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "584.58",      0x000000, 0x080000, 0xd65af431 );
		ROM_LOAD( "lh634a14.63", 0x080000, 0x080000, 0xcdda9b9e );
		ROM_FILL(                0x100000, 0x080000, 0xff );
		ROM_LOAD( "1.74",        0x180000, 0x008000, 0x5b0b2efc );
		ROM_FILL(                0x188000, 0x078000, 0xff );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "585.59",      0x000000, 0x080000, 0x58ce0937 );
		ROM_FILL(                0x080000, 0x100000, 0xff );
		ROM_LOAD( "2.75",        0x180000, 0x010000, 0x4dd4f786 );
		ROM_FILL(                0x190000, 0x070000, 0xff );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_daiyogen = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x010000, REGION_CPU1, 0 );
		ROM_LOAD( "n1-ic70.bin", 0x000000, 0x008000, 0x29af632b );
	
		ROM_REGION( 0x130000, REGION_CPU2, 0 );
		ROM_LOAD( "n2-ic47.bin", 0x000000, 0x008000, 0x8896604c );
		ROM_LOAD( "ic87.bin",    0x010000, 0x080000, 0x4f86ffe2 );
		ROM_LOAD( "ic78.bin",    0x090000, 0x080000, 0xae52bccd );
		ROM_LOAD( "7-ic72.bin",  0x110000, 0x020000, 0x30279296 );
	
		ROM_REGION( 0x0c0000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "ic58.bin",    0x000000, 0x080000, 0x8cf3d5f5 );
		ROM_LOAD( "ic63.bin",    0x080000, 0x040000, 0x64611070 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "ic59.bin",    0x000000, 0x080000, 0x715f2f8c );
		ROM_LOAD( "ic64.bin",    0x080000, 0x080000, 0xe5a41864 );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_nmsengen = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x010000, REGION_CPU1, 0 );
		ROM_LOAD( "3-ic70.bin",   0x000000, 0x008000, 0x4e6edbbb );
	
		ROM_REGION( 0x410000, REGION_CPU2, 0 );
		ROM_LOAD( "4-ic47.bin",   0x000000, 0x008000, 0xd31c596e );
		ROM_LOAD( "vsj-ic87.bin", 0x010000, 0x100000, 0xd3e8bd73 );
		ROM_LOAD( "j5-ic72.bin",  0x210000, 0x020000, 0xdb937253 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "vsk-ic63.bin", 0x000000, 0x100000, 0xf95f9c67 );
		ROM_LOAD( "ic58.bin",     0x100000, 0x040000, 0xc66dcf18 );
		ROM_FILL(                 0x140000, 0x080000, 0xff );
		ROM_LOAD( "1-ic68.bin",   0x1c0000, 0x020000, 0xa944a8d6 );
		ROM_FILL(                 0x1e0000, 0x020000, 0xff );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "vsh-ic64.bin", 0x000000, 0x100000, 0xf546ffaf );
		ROM_LOAD( "vsg-ic59.bin", 0x100000, 0x080000, 0x25bae018 );
		ROM_LOAD( "ic69.bin",     0x180000, 0x040000, 0xdc867ccd );
		ROM_LOAD( "2-ic75.bin",   0x1c0000, 0x020000, 0xe2fad82e );
		ROM_FILL(                 0x1e0000, 0x020000, 0xff );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_fromance = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x010000, REGION_CPU1, 0 );
		ROM_LOAD( "2-ic70.bin", 0x000000, 0x008000, 0xa0866e26 );
	
		ROM_REGION( 0x410000, REGION_CPU2, 0 );
		ROM_LOAD( "1-ic47.bin", 0x000000, 0x008000, 0xac859917 );
		ROM_LOAD( "ic87.bin",   0x010000, 0x100000, 0xbb0d224e );
		ROM_LOAD( "ic78.bin",   0x110000, 0x040000, 0xba2dba83 );
		ROM_LOAD( "5-ic72.bin", 0x210000, 0x020000, 0x377cd57c );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "ic63.bin",   0x000000, 0x100000, 0xfaa9cdf3 );
		ROM_FILL(               0x100000, 0x0c0000, 0xff );
		ROM_LOAD( "4-ic68.bin", 0x1c0000, 0x020000, 0x9b35cea3 );
		ROM_FILL(               0x1e0000, 0x020000, 0xff );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "ic64.bin",   0x000000, 0x100000, 0x23b9a484 );
		ROM_FILL(               0x100000, 0x080000, 0xff );
		ROM_LOAD( "ic69.bin",   0x180000, 0x040000, 0xd06a0fc0 );
		ROM_LOAD( "3-ic75.bin", 0x1c0000, 0x020000, 0xbb314e78 );
		ROM_FILL(               0x1e0000, 0x020000, 0xff );
	ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_idolmj	   = new GameDriver("1988"	,"idolmj"	,"fromance.java"	,rom_idolmj,null	,machine_driver_idolmj	,input_ports_idolmj	,null	,ROT0	,	"System Service", "Idol-Mahjong Housoukyoku (Japan)" );
	public static GameDriver driver_mjnatsu	   = new GameDriver("1989"	,"mjnatsu"	,"fromance.java"	,rom_mjnatsu,null	,machine_driver_fromance	,input_ports_mjnatsu	,null	,ROT0	,	"Video System", "Mahjong Natsu Monogatari (Japan)" );
	public static GameDriver driver_mfunclub	   = new GameDriver("1989"	,"mfunclub"	,"fromance.java"	,rom_mfunclub,null	,machine_driver_fromance	,input_ports_mfunclub	,null	,ROT0	,	"Video System", "Mahjong Fun Club - Idol Saizensen (Japan)" );
	public static GameDriver driver_daiyogen	   = new GameDriver("1990"	,"daiyogen"	,"fromance.java"	,rom_daiyogen,null	,machine_driver_fromance	,input_ports_daiyogen	,null	,ROT0	,	"Video System", "Mahjong Daiyogen (Japan)" );
	public static GameDriver driver_nmsengen	   = new GameDriver("1991"	,"nmsengen"	,"fromance.java"	,rom_nmsengen,null	,machine_driver_fromance	,input_ports_nmsengen	,null	,ROT0	,	"Video System", "Nekketsu Mahjong Sengen! AFTER 5 (Japan)" );
	public static GameDriver driver_fromance	   = new GameDriver("1991"	,"fromance"	,"fromance.java"	,rom_fromance,null	,machine_driver_fromance	,input_ports_fromance	,null	,ROT0	,	"Video System", "Idol-Mahjong Final Romance (Japan)" );
}
