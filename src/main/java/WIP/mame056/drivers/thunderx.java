/***************************************************************************

Super Contra / Thunder Cross

driver by Bryan McPhail, Manuel Abadia

K052591 emulation by Eddie Edwards

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
import static mame056.sound.k007232.*;
import static mame056.sound.k007232H.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static WIP.mame056.vidhrdw.thunderx.*;
import static WIP.mame056.vidhrdw.konamiic.*;
import static mame056.sound.MSM5205.*;
import static mame056.vidhrdw.generic.*;
import static mame056.cpu.konami.konamiH.*;
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
import static mame056.sound.k053260.*;
import static mame056.sound.k053260H.*;
import mame056.timer;
import static mame056.cpu.konami.konami.konami_cpu_setlines_callback;
import static mame056.cpu.konami.konami.konami_cpu_setlines_callbackPtr;

public class thunderx
{
	
	static int unknown_enable = 0;
	
	/***************************************************************************/
	
	public static InterruptPtr scontra_interrupt = new InterruptPtr() { public int handler() 
	{
		if (K052109_is_IRQ_enabled() != 0)
			return KONAMI_INT_IRQ;
		else
			return ignore_interrupt.handler();
	} };
	
	public static InterruptPtr thunderx_interrupt = new InterruptPtr() { public int handler() 
	{
		if (K052109_is_IRQ_enabled() != 0)
		{
			if (cpu_getiloops() == 0) return KONAMI_INT_IRQ;
			else if ((cpu_getiloops() & 1) != 0) return KONAMI_INT_FIRQ;	/* ??? */
		}
		return ignore_interrupt.handler();
	} };
	
	
	static int palette_selected;
	static int bank;
	static UBytePtr ram = new UBytePtr(), cdram = new UBytePtr();
	
	public static ReadHandlerPtr scontra_bankedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (palette_selected != 0)
			return paletteram_r.handler(offset);
		else
			return ram.read(offset);
	} };
	
	public static WriteHandlerPtr scontra_bankedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (palette_selected != 0)
			paletteram_xBBBBBGGGGGRRRRR_swap_w.handler(offset,data);
		else
			ram.write(offset, data);
	} };
	
	public static ReadHandlerPtr thunderx_bankedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if ((bank & 0x01) == 0)
		{
			if ((bank & 0x10) != 0)
			{
	//			debug_signal_breakpoint(0);
				return cdram.read(offset);
			}
			else
				return paletteram_r.handler(offset);
		}
		else
			return ram.read(offset);
	} };
	
	public static WriteHandlerPtr thunderx_bankedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((bank & 0x01) == 0)
		{
			if ((bank & 0x10) != 0)
			{
	//			if (offset == 0x200)	debug_signal_breakpoint(1);
				cdram.write(offset, data);
			}
			else
				paletteram_xBBBBBGGGGGRRRRR_swap_w.handler(offset,data);
		}
		else
			ram.write(offset, data);
	} };
	
	// run_collisions
	//
	// collide objects from s0 to e0 against
	// objects from s1 to e1
	//
	// only compare objects with the specified bits (cm) set in their flags
	// only set object 0's hit bit if (hm & 0x40) is true
	//
	// the data format is:
	//
	// +0 : flags
	// +1 : width (4 pixel units)
	// +2 : height (4 pixel units)
	// +3 : x (2 pixel units) of center of object
	// +4 : y (2 pixel units) of center of object
	
	static void run_collisions(int s0, int e0, int s1, int e1, int cm, int hm)
	{
		UBytePtr	p0;
		UBytePtr	p1;
		int				ii,jj;
	
		p0 = new UBytePtr(cdram, 16 + 5*s0);
		for (ii = s0; ii < e0; ii++, p0.inc(5) )
		{
			int	l0,r0,b0,t0;
	
			// check valid
			if ((p0.read(0) & cm)==0)			continue;
	
			// get area
			l0 = p0.read(3) - p0.read(1);
			r0 = p0.read(3) + p0.read(1);
			t0 = p0.read(4) - p0.read(2);
			b0 = p0.read(4) + p0.read(2);
	
			p1 = new UBytePtr(cdram, 16 + 5*s1);
			for (jj = s1; jj < e1; jj++,p1.inc(5))
			{
				int	l1,r1,b1,t1;
	
				// check valid
				if ((p1.read(0) & cm)==0)		continue;
	
				// get area
				l1 = p1.read(3) - p1.read(1);
				r1 = p1.read(3) + p1.read(1);
				t1 = p1.read(4) - p1.read(2);
				b1 = p1.read(4) + p1.read(2);
	
				// overlap check
				if (l1 >= r0)	continue;
				if (l0 >= r1)	continue;
				if (t1 >= b0)	continue;
				if (t0 >= b1)	continue;
	
				// set flags
				if ((hm & 0x40) != 0)		p0.write(0, p0.read(0) | 0x10 );
				p1.write(0, p1.read(0) | 0x10 );
			}
		}
	}
	
	// calculate_collisions
	//
	// emulates K052591 collision detection
	
	static void calculate_collisions()
	{
		int	X0,Y0;
		int	X1,Y1;
		int	CM,HM;
	
		// the data at 0x00 to 0x06 defines the operation
		//
		// 0x00 : word : last byte of set 0
		// 0x02 : byte : last byte of set 1
		// 0x03 : byte : collide mask
		// 0x04 : byte : hit mask
		// 0x05 : byte : first byte of set 0
		// 0x06 : byte : first byte of set 1
		//
		// the USA version is slightly different:
		//
		// 0x05 : word : first byte of set 0
		// 0x07 : byte : first byte of set 1
		//
		// the operation is to intersect set 0 with set 1
		// collide mask specifies objects to ignore
		// hit mask is 40 to set bit on object 0 and object 1
		// hit mask is 20 to set bit on object 1 only
	
		Y0 = cdram.read(0);
		Y0 = (Y0 << 8) + cdram.read(1);
		Y0 = (Y0 - 15) / 5;
		Y1 = (cdram.read(2) - 15) / 5;
	
		if (cdram.read(5) < 16)
		{
			// US Thunder Cross uses this form
			X0 = cdram.read(5);
			X0 = (X0 << 8) + cdram.read(6);
			X0 = (X0 - 16) / 5;
			X1 = (cdram.read(7) - 16) / 5;
		}
		else
		{
			// Japan Thunder Cross uses this form
			X0 = (cdram.read(5) - 16) / 5;
			X1 = (cdram.read(6) - 16) / 5;
		}
	
		CM = cdram.read(3);
		HM = cdram.read(4);
	
		run_collisions(X0,Y0,X1,Y1,CM,HM);
	}
	
	public static WriteHandlerPtr thunderx_1f98_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 = enable char ROM reading through the video RAM */
		K052109_set_RMRD_line((data & 0x01)!=0 ? ASSERT_LINE : CLEAR_LINE);
	
		/* bit 1 = reset for collision MCU??? */
		/* we don't need it, anyway */
	
		/* bit 2 = do collision detection when 0->1 */
		if ((data & 4)!=0 && (unknown_enable & 4)==0)
		{
			calculate_collisions();
		}
	
		unknown_enable = data;
	} };
	
	public static WriteHandlerPtr scontra_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
		int offs;
	
	//logerror("%04x: bank switch %02x\n",cpu_get_pc(),data);
	
		/* bits 0-3 ROM bank */
		offs = 0x10000 + (data & 0x0f)*0x2000;
		cpu_setbank( 1, new UBytePtr(RAM, offs) );
	
		/* bit 4 select work RAM or palette RAM at 5800-5fff */
		palette_selected = ~data & 0x10;
	
		/* bits 5/6 coin counters */
		coin_counter_w.handler(0,data & 0x20);
		coin_counter_w.handler(1,data & 0x40);
	
		/* bit 7 controls layer priority */
		scontra_priority = data & 0x80;
	} };
	
	public static WriteHandlerPtr thunderx_videobank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror("%04x: select video ram bank %02x\n",cpu_get_pc(),data);
		/* 0x01 = work RAM at 4000-5fff */
		/* 0x00 = palette at 5800-5fff */
		/* 0x10 = unknown RAM at 5800-5fff */
		bank = data;
	
		/* bits 1/2 coin counters */
		coin_counter_w.handler(0,data & 0x02);
		coin_counter_w.handler(1,data & 0x04);
	
		/* bit 3 controls layer priority (seems to be always 1) */
		scontra_priority = data & 0x08;
	} };
	
	public static WriteHandlerPtr thunderx_sh_irqtrigger_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_cause_interrupt(1,0xff);
	} };
	
	public static WriteHandlerPtr scontra_snd_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_SOUND1));
		/* b3-b2: bank for chanel B */
		/* b1-b0: bank for chanel A */
	
		int bank_A = 0x20000*(data & 0x03);
		int bank_B = 0x20000*((data >> 2) & 0x03);
	
		K007232_bankswitch(0,new UBytePtr(RAM, bank_A), new UBytePtr(RAM, bank_B));
	} };
	
	/***************************************************************************/
	
	public static Memory_ReadAddress scontra_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x1f90, 0x1f90, input_port_0_r ), /* coin */
		new Memory_ReadAddress( 0x1f91, 0x1f91, input_port_1_r ), /* p1 */
		new Memory_ReadAddress( 0x1f92, 0x1f92, input_port_2_r ), /* p2 */
		new Memory_ReadAddress( 0x1f93, 0x1f93, input_port_5_r ), /* Dip 3 */
		new Memory_ReadAddress( 0x1f94, 0x1f94, input_port_3_r ), /* Dip 1 */
		new Memory_ReadAddress( 0x1f95, 0x1f95, input_port_4_r ), /* Dip 2 */
	
		new Memory_ReadAddress( 0x0000, 0x3fff, K052109_051960_r ),
		new Memory_ReadAddress( 0x4000, 0x57ff, MRA_RAM ),
		new Memory_ReadAddress( 0x5800, 0x5fff, scontra_bankedram_r ),			/* palette + work RAM */
		new Memory_ReadAddress( 0x6000, 0x7fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress thunderx_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x1f90, 0x1f90, input_port_0_r ), /* coin */
		new Memory_ReadAddress( 0x1f91, 0x1f91, input_port_1_r ), /* p1 */
		new Memory_ReadAddress( 0x1f92, 0x1f92, input_port_2_r ), /* p2 */
		new Memory_ReadAddress( 0x1f93, 0x1f93, input_port_5_r ), /* Dip 3 */
		new Memory_ReadAddress( 0x1f94, 0x1f94, input_port_3_r ), /* Dip 1 */
		new Memory_ReadAddress( 0x1f95, 0x1f95, input_port_4_r ), /* Dip 2 */
	
		new Memory_ReadAddress( 0x0000, 0x3fff, K052109_051960_r ),
		new Memory_ReadAddress( 0x4000, 0x57ff, MRA_RAM ),
		new Memory_ReadAddress( 0x5800, 0x5fff, thunderx_bankedram_r ),			/* palette + work RAM + unknown RAM */
		new Memory_ReadAddress( 0x6000, 0x7fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress scontra_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x1f80, 0x1f80, scontra_bankswitch_w ),	/* bankswitch control + coin counters */
		new Memory_WriteAddress( 0x1f84, 0x1f84, soundlatch_w ),
		new Memory_WriteAddress( 0x1f88, 0x1f88, thunderx_sh_irqtrigger_w ),		/* cause interrupt on audio CPU */
		new Memory_WriteAddress( 0x1f8c, 0x1f8c, watchdog_reset_w ),
		new Memory_WriteAddress( 0x1f98, 0x1f98, thunderx_1f98_w ),
	
		new Memory_WriteAddress( 0x0000, 0x3fff, K052109_051960_w ),		/* video RAM + sprite RAM */
		new Memory_WriteAddress( 0x4000, 0x57ff, MWA_RAM ),
		new Memory_WriteAddress( 0x5800, 0x5fff, scontra_bankedram_w, ram ),			/* palette + work RAM */
		new Memory_WriteAddress( 0x6000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress thunderx_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x1f80, 0x1f80, thunderx_videobank_w ),
		new Memory_WriteAddress( 0x1f84, 0x1f84, soundlatch_w ),
		new Memory_WriteAddress( 0x1f88, 0x1f88, thunderx_sh_irqtrigger_w ),		/* cause interrupt on audio CPU */
		new Memory_WriteAddress( 0x1f8c, 0x1f8c, watchdog_reset_w ),
		new Memory_WriteAddress( 0x1f98, 0x1f98, thunderx_1f98_w ),
	
		new Memory_WriteAddress( 0x0000, 0x3fff, K052109_051960_w ),
		new Memory_WriteAddress( 0x4000, 0x57ff, MWA_RAM ),
		new Memory_WriteAddress( 0x5800, 0x5fff, thunderx_bankedram_w, ram ),			/* palette + work RAM + unknown RAM */
		new Memory_WriteAddress( 0x6000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress scontra_readmem_sound[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),				/* ROM */
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),				/* RAM */
		new Memory_ReadAddress( 0xa000, 0xa000, soundlatch_r ),			/* soundlatch_r */
		new Memory_ReadAddress( 0xb000, 0xb00d, K007232_read_port_0_r ),	/* 007232 registers */
		new Memory_ReadAddress( 0xc001, 0xc001, YM2151_status_port_0_r ),	/* YM2151 */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress scontra_writemem_sound[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),					/* ROM */
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),					/* RAM */
		new Memory_WriteAddress( 0xb000, 0xb00d, K007232_write_port_0_w ),		/* 007232 registers */
		new Memory_WriteAddress( 0xc000, 0xc000, YM2151_register_port_0_w ),	/* YM2151 */
		new Memory_WriteAddress( 0xc001, 0xc001, YM2151_data_port_0_w ),		/* YM2151 */
		new Memory_WriteAddress( 0xf000, 0xf000, scontra_snd_bankswitch_w ),	/* 007232 bank select */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress thunderx_readmem_sound[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0xa000, 0xa000, soundlatch_r ),
		new Memory_ReadAddress( 0xc001, 0xc001, YM2151_status_port_0_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress thunderx_writemem_sound[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0xc000, 0xc000, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0xc001, 0xc001, YM2151_data_port_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	/***************************************************************************
	
		Input Ports
	
	***************************************************************************/
	
	static InputPortPtr input_ports_scontra = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* COINSW */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* PLAYER 1 INPUTS */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* PLAYER 2 INPUTS */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* DSW #1 */
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
	//	PORT_DIPSETTING(    0x00, "Invalid" );
	
		PORT_START(); 	/* DSW #2 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(	0x03, "2" );
		PORT_DIPSETTING(	0x02, "3" );
		PORT_DIPSETTING(	0x01, "5" );
		PORT_DIPSETTING(	0x00, "7" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") );	/* test mode calls it cabinet type, */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );		/* but this is a 2 players game */
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x18, 0x10, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(	0x18, "30000 200000" );
		PORT_DIPSETTING(	0x10, "50000 300000" );
		PORT_DIPSETTING(	0x08, "30000" );
		PORT_DIPSETTING(	0x00, "50000" );
		PORT_DIPNAME( 0x60, 0x40, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(	0x60, "Easy" );
		PORT_DIPSETTING(	0x40, "Normal" );
		PORT_DIPSETTING(	0x20, "Difficult" );
		PORT_DIPSETTING(	0x00, "Very Difficult" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(	0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* DSW #3 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(	0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, "Continue Limit" );
		PORT_DIPSETTING(    0x08, "3" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_thunderx = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* PLAYER 1 INPUTS */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* PLAYER 2 INPUTS */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
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
	//	PORT_DIPSETTING(    0x00, "Invalid" );
	
	 	PORT_START(); 
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPSETTING(    0x00, "7" );
		PORT_DIPNAME( 0x04, 0x00, "Award Bonus Life" );
		PORT_DIPSETTING(    0x04, DEF_STR( "No") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x18, 0x18, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x18, "30000 200000" );
		PORT_DIPSETTING(    0x10, "50000 300000" );
		PORT_DIPSETTING(    0x08, "30000" );
		PORT_DIPSETTING(    0x00, "50000" );
		PORT_DIPNAME( 0x60, 0x60, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x60, "Easy" );
		PORT_DIPSETTING(    0x40, "Normal" );
		PORT_DIPSETTING(    0x20, "Difficult" );
		PORT_DIPSETTING(    0x00, "Very Difficult" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	
	/***************************************************************************
	
		Machine Driver
	
	***************************************************************************/
	
	static YM2151interface ym2151_interface = new YM2151interface
	(
		1,			/* 1 chip */
		3579545,	/* 3.579545 MHz */
		new int[]{ YM3012_VOL(100,MIXER_PAN_LEFT,100,MIXER_PAN_RIGHT) },
		new WriteYmHandlerPtr[]{ null }
	);
	
	static WriteYmHandlerPtr volume_callback = new WriteYmHandlerPtr() {
            public void handler(int v) {
                K007232_set_volume(0,0,(v >> 4) * 0x11,0);
		K007232_set_volume(0,1,0,(v & 0x0f) * 0x11);
            }
        };
        
	static K007232_interface k007232_interface = new K007232_interface
	(
		1,		/* number of chips */
		new int[]{ REGION_SOUND1 },	/* memory regions */
		new int[]{ K007232_VOL(20,MIXER_PAN_CENTER,20,MIXER_PAN_CENTER) },	/* volume */
		new WriteYmHandlerPtr[]{ volume_callback }	/* external port callback */
	);
	
        public static InitMachinePtr scontra_init_machine = new InitMachinePtr() { public void handler()
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		paletteram = new UBytePtr(RAM, 0x30000);
	} };
	
	public static InitMachinePtr thunderx_init_machine = new InitMachinePtr() { public void handler()
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		konami_cpu_setlines_callback = thunderx_banking;
		cpu_setbank( 1, new UBytePtr(RAM, 0x10000) ); /* init the default bank */
	
		paletteram = new UBytePtr(RAM, 0x28000);
		cdram = new UBytePtr(RAM, 0x28800);
	} };
	
	public static InitDriverPtr init_scontra = new InitDriverPtr() { public void handler()
	{
		konami_rom_deinterleave_2(REGION_GFX1);
		konami_rom_deinterleave_2(REGION_GFX2);
	} };
	
	
	
	static MachineDriver machine_driver_scontra = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_KONAMI,	/* 052001 */
				3000000,	/* ? */
				scontra_readmem,scontra_writemem,null,null,
				scontra_interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3579545,		/* ? */
				scontra_readmem_sound,scontra_writemem_sound,null,null,
				ignore_interrupt,0	/* interrupts are triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		scontra_init_machine,
	
		/* video hardware */
		64*8, 32*8, new rectangle( 14*8, (64-14)*8-1, 2*8, 30*8-1 ),
		null, /* gfx decoded by konamiic.c */
		1024, 0,
		null,
	
		VIDEO_TYPE_RASTER | VIDEO_HAS_SHADOWS,
		null,
		scontra_vh_start,
		scontra_vh_stop,
		scontra_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
			),
			new MachineSound(
				SOUND_K007232,
				k007232_interface
			)
		}
	);
	
	static MachineDriver machine_driver_thunderx = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_KONAMI,
				3000000,		/* ? */
				thunderx_readmem,thunderx_writemem,null,null,
				thunderx_interrupt,16	/* ???? */
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3579545,		/* ? */
				thunderx_readmem_sound,thunderx_writemem_sound,null,null,
				ignore_interrupt,0	/* interrupts are triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		thunderx_init_machine,
	
		/* video hardware */
		64*8, 32*8, new rectangle( 14*8, (64)*8-1, 2*8, 30*8-1 ),
		null, /* gfx decoded by konamiic.c */
		1024, 0,
		null,
	
		VIDEO_TYPE_RASTER | VIDEO_HAS_SHADOWS,
		null,
		scontra_vh_start,
		scontra_vh_stop,
		scontra_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
			)
		}
	);
	
	
	/***************************************************************************
	
	  Game ROMs
	
	***************************************************************************/
	
	static RomLoadPtr rom_scontra = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x30800, REGION_CPU1, 0 );/* ROMs + banked RAM */
		ROM_LOAD( "e02.k11",     0x10000, 0x08000, 0xa61c0ead );/* banked ROM */
		ROM_CONTINUE(            0x08000, 0x08000 );			/* fixed ROM */
		ROM_LOAD( "e03.k13",     0x20000, 0x10000, 0x00b02622 );/* banked ROM */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the SOUND CPU */
		ROM_LOAD( "775-c01.bin", 0x00000, 0x08000, 0x0ced785a );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* tiles */
		ROM_LOAD16_BYTE( "775-a07a.bin", 0x00000, 0x20000, 0xe716bdf3 );/* tiles */
		ROM_LOAD16_BYTE( "775-a07e.bin", 0x00001, 0x20000, 0x0986e3a5 );
		ROM_LOAD16_BYTE( "775-f07c.bin", 0x40000, 0x10000, 0xb0b30915 );
		ROM_LOAD16_BYTE( "775-f07g.bin", 0x40001, 0x10000, 0xfbed827d );
		ROM_LOAD16_BYTE( "775-f07d.bin", 0x60000, 0x10000, 0xf184be8e );
		ROM_LOAD16_BYTE( "775-f07h.bin", 0x60001, 0x10000, 0x7b56c348 );
		ROM_LOAD16_BYTE( "775-a08a.bin", 0x80000, 0x20000, 0x3ddd11a4 );
		ROM_LOAD16_BYTE( "775-a08e.bin", 0x80001, 0x20000, 0x1007d963 );
		ROM_LOAD16_BYTE( "775-f08c.bin", 0xc0000, 0x10000, 0x53abdaec );
		ROM_LOAD16_BYTE( "775-f08g.bin", 0xc0001, 0x10000, 0x3df85a6e );
		ROM_LOAD16_BYTE( "775-f08d.bin", 0xe0000, 0x10000, 0x102dcace );
		ROM_LOAD16_BYTE( "775-f08h.bin", 0xe0001, 0x10000, 0xad9d7016 );
	
		ROM_REGION( 0x100000, REGION_GFX2, 0 );/* sprites */
		ROM_LOAD16_BYTE( "775-a05a.bin", 0x00000, 0x10000, 0xa0767045 );/* sprites */
		ROM_LOAD16_BYTE( "775-a05e.bin", 0x00001, 0x10000, 0x2f656f08 );
		ROM_LOAD16_BYTE( "775-a05b.bin", 0x20000, 0x10000, 0xab8ad4fd );
		ROM_LOAD16_BYTE( "775-a05f.bin", 0x20001, 0x10000, 0x1c0eb1b6 );
		ROM_LOAD16_BYTE( "775-f05c.bin", 0x40000, 0x10000, 0x5647761e );
		ROM_LOAD16_BYTE( "775-f05g.bin", 0x40001, 0x10000, 0xa1692cca );
		ROM_LOAD16_BYTE( "775-f05d.bin", 0x60000, 0x10000, 0xad676a6f );
		ROM_LOAD16_BYTE( "775-f05h.bin", 0x60001, 0x10000, 0x3f925bcf );
		ROM_LOAD16_BYTE( "775-a06a.bin", 0x80000, 0x10000, 0x77a34ad0 );
		ROM_LOAD16_BYTE( "775-a06e.bin", 0x80001, 0x10000, 0x8a910c94 );
		ROM_LOAD16_BYTE( "775-a06b.bin", 0xa0000, 0x10000, 0x563fb565 );
		ROM_LOAD16_BYTE( "775-a06f.bin", 0xa0001, 0x10000, 0xe14995c0 );
		ROM_LOAD16_BYTE( "775-f06c.bin", 0xc0000, 0x10000, 0x5ee6f3c1 );
		ROM_LOAD16_BYTE( "775-f06g.bin", 0xc0001, 0x10000, 0x2645274d );
		ROM_LOAD16_BYTE( "775-f06d.bin", 0xe0000, 0x10000, 0xc8b764fa );
		ROM_LOAD16_BYTE( "775-f06h.bin", 0xe0001, 0x10000, 0xd6595f59 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* k007232 data */
		ROM_LOAD( "775-a04a.bin", 0x00000, 0x10000, 0x7efb2e0f );
		ROM_LOAD( "775-a04b.bin", 0x10000, 0x10000, 0xf41a2b33 );
		ROM_LOAD( "775-a04c.bin", 0x20000, 0x10000, 0xe4e58f14 );
		ROM_LOAD( "775-a04d.bin", 0x30000, 0x10000, 0xd46736f6 );
		ROM_LOAD( "775-f04e.bin", 0x40000, 0x10000, 0xfbf7e363 );
		ROM_LOAD( "775-f04f.bin", 0x50000, 0x10000, 0xb031ef2d );
		ROM_LOAD( "775-f04g.bin", 0x60000, 0x10000, 0xee107bbb );
		ROM_LOAD( "775-f04h.bin", 0x70000, 0x10000, 0xfb0fab46 );
	
		ROM_REGION( 0x0100, REGION_PROMS, 0 );
		ROM_LOAD( "775a09.b19",   0x0000, 0x0100, 0x46d1e0df );/* priority encoder (not used) */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_scontraj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x30800, REGION_CPU1, 0 );/* ROMs + banked RAM */
		ROM_LOAD( "775-f02.bin", 0x10000, 0x08000, 0x8d5933a7 );/* banked ROM */
		ROM_CONTINUE(            0x08000, 0x08000 );			/* fixed ROM */
		ROM_LOAD( "775-f03.bin", 0x20000, 0x10000, 0x1ef63d80 );/* banked ROM */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the SOUND CPU */
		ROM_LOAD( "775-c01.bin", 0x00000, 0x08000, 0x0ced785a );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* tiles */
		ROM_LOAD16_BYTE( "775-a07a.bin", 0x00000, 0x20000, 0xe716bdf3 );/* tiles */
		ROM_LOAD16_BYTE( "775-a07e.bin", 0x00001, 0x20000, 0x0986e3a5 );
		ROM_LOAD16_BYTE( "775-f07c.bin", 0x40000, 0x10000, 0xb0b30915 );
		ROM_LOAD16_BYTE( "775-f07g.bin", 0x40001, 0x10000, 0xfbed827d );
		ROM_LOAD16_BYTE( "775-f07d.bin", 0x60000, 0x10000, 0xf184be8e );
		ROM_LOAD16_BYTE( "775-f07h.bin", 0x60001, 0x10000, 0x7b56c348 );
		ROM_LOAD16_BYTE( "775-a08a.bin", 0x80000, 0x20000, 0x3ddd11a4 );
		ROM_LOAD16_BYTE( "775-a08e.bin", 0x80001, 0x20000, 0x1007d963 );
		ROM_LOAD16_BYTE( "775-f08c.bin", 0xc0000, 0x10000, 0x53abdaec );
		ROM_LOAD16_BYTE( "775-f08g.bin", 0xc0001, 0x10000, 0x3df85a6e );
		ROM_LOAD16_BYTE( "775-f08d.bin", 0xe0000, 0x10000, 0x102dcace );
		ROM_LOAD16_BYTE( "775-f08h.bin", 0xe0001, 0x10000, 0xad9d7016 );
	
		ROM_REGION( 0x100000, REGION_GFX2, 0 );/* sprites */
		ROM_LOAD16_BYTE( "775-a05a.bin", 0x00000, 0x10000, 0xa0767045 );/* sprites */
		ROM_LOAD16_BYTE( "775-a05e.bin", 0x00001, 0x10000, 0x2f656f08 );
		ROM_LOAD16_BYTE( "775-a05b.bin", 0x20000, 0x10000, 0xab8ad4fd );
		ROM_LOAD16_BYTE( "775-a05f.bin", 0x20001, 0x10000, 0x1c0eb1b6 );
		ROM_LOAD16_BYTE( "775-f05c.bin", 0x40000, 0x10000, 0x5647761e );
		ROM_LOAD16_BYTE( "775-f05g.bin", 0x40001, 0x10000, 0xa1692cca );
		ROM_LOAD16_BYTE( "775-f05d.bin", 0x60000, 0x10000, 0xad676a6f );
		ROM_LOAD16_BYTE( "775-f05h.bin", 0x60001, 0x10000, 0x3f925bcf );
		ROM_LOAD16_BYTE( "775-a06a.bin", 0x80000, 0x10000, 0x77a34ad0 );
		ROM_LOAD16_BYTE( "775-a06e.bin", 0x80001, 0x10000, 0x8a910c94 );
		ROM_LOAD16_BYTE( "775-a06b.bin", 0xa0000, 0x10000, 0x563fb565 );
		ROM_LOAD16_BYTE( "775-a06f.bin", 0xa0001, 0x10000, 0xe14995c0 );
		ROM_LOAD16_BYTE( "775-f06c.bin", 0xc0000, 0x10000, 0x5ee6f3c1 );
		ROM_LOAD16_BYTE( "775-f06g.bin", 0xc0001, 0x10000, 0x2645274d );
		ROM_LOAD16_BYTE( "775-f06d.bin", 0xe0000, 0x10000, 0xc8b764fa );
		ROM_LOAD16_BYTE( "775-f06h.bin", 0xe0001, 0x10000, 0xd6595f59 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* k007232 data */
		ROM_LOAD( "775-a04a.bin", 0x00000, 0x10000, 0x7efb2e0f );
		ROM_LOAD( "775-a04b.bin", 0x10000, 0x10000, 0xf41a2b33 );
		ROM_LOAD( "775-a04c.bin", 0x20000, 0x10000, 0xe4e58f14 );
		ROM_LOAD( "775-a04d.bin", 0x30000, 0x10000, 0xd46736f6 );
		ROM_LOAD( "775-f04e.bin", 0x40000, 0x10000, 0xfbf7e363 );
		ROM_LOAD( "775-f04f.bin", 0x50000, 0x10000, 0xb031ef2d );
		ROM_LOAD( "775-f04g.bin", 0x60000, 0x10000, 0xee107bbb );
		ROM_LOAD( "775-f04h.bin", 0x70000, 0x10000, 0xfb0fab46 );
	
		ROM_REGION( 0x0100, REGION_PROMS, 0 );
		ROM_LOAD( "775a09.b19",   0x0000, 0x0100, 0x46d1e0df );/* priority encoder (not used) */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thunderx = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x29000, REGION_CPU1, 0 );/* ROMs + banked RAM */
		ROM_LOAD( "873k03.k15", 0x10000, 0x10000, 0x276817ad );
		ROM_LOAD( "873k02.k13", 0x20000, 0x08000, 0x80cc1c45 );
		ROM_CONTINUE(           0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "873h01.f8",    0x0000, 0x8000, 0x990b7a7c );
	
		ROM_REGION( 0x80000, REGION_GFX1, 0 );/* temporary space for graphics (disposed after conversion) */
		ROM_LOAD16_BYTE( "873c06a.f6",   0x00000, 0x10000, 0x0e340b67 );/* Chars */
		ROM_LOAD16_BYTE( "873c06c.f5",   0x00001, 0x10000, 0xef0e72cd );
		ROM_LOAD16_BYTE( "873c06b.e6",   0x20000, 0x10000, 0x97ad202e );
		ROM_LOAD16_BYTE( "873c06d.e5",   0x20001, 0x10000, 0x8393d42e );
		ROM_LOAD16_BYTE( "873c07a.f4",   0x40000, 0x10000, 0xa8aab84f );
		ROM_LOAD16_BYTE( "873c07c.f3",   0x40001, 0x10000, 0x2521009a );
		ROM_LOAD16_BYTE( "873c07b.e4",   0x60000, 0x10000, 0x12a2b8ba );
		ROM_LOAD16_BYTE( "873c07d.e3",   0x60001, 0x10000, 0xfae9f965 );
	
		ROM_REGION( 0x80000, REGION_GFX2, 0 );
		ROM_LOAD16_BYTE( "873c04a.f11",  0x00000, 0x10000, 0xf7740bf3 );/* Sprites */
		ROM_LOAD16_BYTE( "873c04c.f10",  0x00001, 0x10000, 0x5dacbd2b );
		ROM_LOAD16_BYTE( "873c04b.e11",  0x20000, 0x10000, 0x9ac581da );
		ROM_LOAD16_BYTE( "873c04d.e10",  0x20001, 0x10000, 0x44a4668c );
		ROM_LOAD16_BYTE( "873c05a.f9",   0x40000, 0x10000, 0xd73e107d );
		ROM_LOAD16_BYTE( "873c05c.f8",   0x40001, 0x10000, 0x59903200 );
		ROM_LOAD16_BYTE( "873c05b.e9",   0x60000, 0x10000, 0x81059b99 );
		ROM_LOAD16_BYTE( "873c05d.e8",   0x60001, 0x10000, 0x7fa3d7df );
	
		ROM_REGION( 0x0100, REGION_PROMS, 0 );
		ROM_LOAD( "873a08.f20",   0x0000, 0x0100, 0xe2d09a1b );/* priority encoder (not used) */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thnderxj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x29000, REGION_CPU1, 0 );/* ROMs + banked RAM */
		ROM_LOAD( "873-n03.k15", 0x10000, 0x10000, 0xa01e2e3e );
		ROM_LOAD( "873-n02.k13", 0x20000, 0x08000, 0x55afa2cc );
		ROM_CONTINUE(            0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "873-f01.f8",   0x0000, 0x8000, 0xea35ffa3 );
	
		ROM_REGION( 0x80000, REGION_GFX1, 0 );/* temporary space for graphics (disposed after conversion) */
		ROM_LOAD16_BYTE( "873c06a.f6",   0x00000, 0x10000, 0x0e340b67 );/* Chars */
		ROM_LOAD16_BYTE( "873c06c.f5",   0x00001, 0x10000, 0xef0e72cd );
		ROM_LOAD16_BYTE( "873c06b.e6",   0x20000, 0x10000, 0x97ad202e );
		ROM_LOAD16_BYTE( "873c06d.e5",   0x20001, 0x10000, 0x8393d42e );
		ROM_LOAD16_BYTE( "873c07a.f4",   0x40000, 0x10000, 0xa8aab84f );
		ROM_LOAD16_BYTE( "873c07c.f3",   0x40001, 0x10000, 0x2521009a );
		ROM_LOAD16_BYTE( "873c07b.e4",   0x60000, 0x10000, 0x12a2b8ba );
		ROM_LOAD16_BYTE( "873c07d.e3",   0x60001, 0x10000, 0xfae9f965 );
	
		ROM_REGION( 0x80000, REGION_GFX2, 0 );
		ROM_LOAD16_BYTE( "873c04a.f11",  0x00000, 0x10000, 0xf7740bf3 );/* Sprites */
		ROM_LOAD16_BYTE( "873c04c.f10",  0x00001, 0x10000, 0x5dacbd2b );
		ROM_LOAD16_BYTE( "873c04b.e11",  0x20000, 0x10000, 0x9ac581da );
		ROM_LOAD16_BYTE( "873c04d.e10",  0x20001, 0x10000, 0x44a4668c );
		ROM_LOAD16_BYTE( "873c05a.f9",   0x40000, 0x10000, 0xd73e107d );
		ROM_LOAD16_BYTE( "873c05c.f8",   0x40001, 0x10000, 0x59903200 );
		ROM_LOAD16_BYTE( "873c05b.e9",   0x60000, 0x10000, 0x81059b99 );
		ROM_LOAD16_BYTE( "873c05d.e8",   0x60001, 0x10000, 0x7fa3d7df );
	
		ROM_REGION( 0x0100, REGION_PROMS, 0 );
		ROM_LOAD( "873a08.f20",   0x0000, 0x0100, 0xe2d09a1b );/* priority encoder (not used) */
	ROM_END(); }}; 
	
	/***************************************************************************/
	
	static konami_cpu_setlines_callbackPtr thunderx_banking = new konami_cpu_setlines_callbackPtr() {
            public void handler(int lines) {
                UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
		int offs;
	
	//	logerror("thunderx %04x: bank select %02x\n", cpu_get_pc(), lines );
	
		offs = 0x10000 + (((lines & 0x0f) ^ 0x08) * 0x2000);
		if (offs >= 0x28000) offs -= 0x20000;
		cpu_setbank( 1, new UBytePtr(RAM, offs) );
            }
        };
        
	public static GameDriver driver_scontra	   = new GameDriver("1988"	,"scontra"	,"thunderx.java"	,rom_scontra,null	,machine_driver_scontra	,input_ports_scontra	,init_scontra	,ROT90	,	"Konami", "Super Contra" );
	public static GameDriver driver_scontraj	   = new GameDriver("1988"	,"scontraj"	,"thunderx.java"	,rom_scontraj,driver_scontra	,machine_driver_scontra	,input_ports_scontra	,init_scontra	,ROT90	,	"Konami", "Super Contra (Japan)" );
	public static GameDriver driver_thunderx	   = new GameDriver("1988"	,"thunderx"	,"thunderx.java"	,rom_thunderx,null	,machine_driver_thunderx	,input_ports_thunderx	,init_scontra	,ROT0	,	"Konami", "Thunder Cross" );
	public static GameDriver driver_thnderxj	   = new GameDriver("1988"	,"thnderxj"	,"thunderx.java"	,rom_thnderxj,driver_thunderx	,machine_driver_thunderx	,input_ports_thunderx	,init_scontra	,ROT0	,	"Konami", "Thunder Cross (Japan)" );
}
