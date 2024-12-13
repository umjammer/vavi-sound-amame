/***************************************************************************

Jack Rabbit memory map (preliminary)

driver by Nicola Salmoria
thanks to Andrea Babich for the manual.

TODO:
- correctly hook up TMS5200 (there's a kludge in zaccaria_ca2_r to make it work)

- there seems to be a strange kind of DAC connected to 8910 #0 port A, but it sounds
  horrible so I'm leaving its volume at 0.

- The 8910 outputs go through some analog circuitry to make them sound more like
  real intruments.
  #0 Ch. A = "rullante"/"cassa" (drum roll/bass drum) (selected by bits 3&4 of port A)
  #0 Ch. B = "basso" (bass)
  #0 Ch. C = straight out through an optional filter
  #1 Ch. A = "piano"
  #1 Ch. B = "tromba" (trumpet) (level selected by bit 0 of port A)
  #1 Ch. C = disabled (there's an open jumper, otherwise would go out through a filter)

- some minor color issues (see vidhrdw)


Notes:
- There is a protection device which I haven't located on the schematics. It
  sits on bits 4-7 of the data bus, and is read from locations where only bits
  0-3 are connected to regular devices (6400-6407 has 4-bit RAM, while 6c00-6c07
  has a 4-bit input port).

- The 6802 driving the TMS5220 has a push button connected to the NMI line. Test?

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
import static WIP.mame056.vidhrdw.zaccaria.*;
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

import static mame056.vidhrdw.generic.*;


public class zaccaria
{
	
	
	static int dsw;
	
	public static WriteHandlerPtr zaccaria_dsw_sel_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (data & 0xf0)
		{
			case 0xe0:
				dsw = 0;
				break;
	
			case 0xd0:
				dsw = 1;
				break;
	
			case 0xb0:
				dsw = 2;
				break;
	
			default:
	logerror("PC %04x: portsel = %02x\n",cpu_get_pc(),data);
				break;
		}
	} };
	
	public static ReadHandlerPtr zaccaria_dsw_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(dsw);
	} };
	
	
	
	public static WriteHandlerPtr ay8910_port0a_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		// bits 0-2 go to a weird kind of DAC ??
		// bits 3-4 control the analog drum emulation on 8910 #0 ch. A
	
		if ((data & 1)!=0)	/* DAC enable */
		{
			/* TODO: is this right? it sound awful */
			int table[] = { 0x05, 0x1b, 0x0b, 0x55 };
			DAC_signed_data_w(0,table[(data & 0x06) >> 1]);
		}
		else
			DAC_signed_data_w(0,0x80);
	} };
	
	
	static irqfuncPtr zaccaria_irq0a = new irqfuncPtr() {
            public void handler(int state) {
                cpu_set_nmi_line(1,  state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
        
	static irqfuncPtr zaccaria_irq0b = new irqfuncPtr() {
            public void handler(int state) { 
                cpu_set_irq_line(1,0,state!=0 ? ASSERT_LINE : CLEAR_LINE);
            } 
        };
	
	static int active_8910,port0a,acs;
	
	public static ReadHandlerPtr zaccaria_port0a_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (active_8910 == 0)
			return AY8910_read_port_0_r.handler(0);
		else
			return AY8910_read_port_1_r.handler(0);
	} };
	
	public static WriteHandlerPtr zaccaria_port0a_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		port0a = data;
	} };
        
        static int last;
	
	public static WriteHandlerPtr zaccaria_port0b_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		/* bit 1 goes to 8910 #0 BDIR pin  */
		if ((last & 0x02) == 0x02 && (data & 0x02) == 0x00)
		{
			/* bit 0 goes to the 8910 #0 BC1 pin */
			if ((last & 0x01) != 0)
				AY8910_control_port_0_w.handler(0,port0a);
			else
				AY8910_write_port_0_w.handler(0,port0a);
		}
		else if ((last & 0x02) == 0x00 && (data & 0x02) == 0x02)
		{
			/* bit 0 goes to the 8910 #0 BC1 pin */
			if ((last & 0x01) != 0)
				active_8910 = 0;
		}
		/* bit 3 goes to 8910 #1 BDIR pin  */
		if ((last & 0x08) == 0x08 && (data & 0x08) == 0x00)
		{
			/* bit 2 goes to the 8910 #1 BC1 pin */
			if ((last & 0x04) != 0)
				AY8910_control_port_1_w.handler(0,port0a);
			else
				AY8910_write_port_1_w.handler(0,port0a);
		}
		else if ((last & 0x08) == 0x00 && (data & 0x08) == 0x08)
		{
			/* bit 2 goes to the 8910 #1 BC1 pin */
			if ((last & 0x04) != 0)
				active_8910 = 1;
		}
	
		last = data;
	} };
        
        static int toggle;
	
	static InterruptPtr zaccaria_cb1_toggle = new InterruptPtr() {
            public int handler() {
                pia_0_cb1_w.handler(0,toggle & 1);
		toggle ^= 1;
	
		return ignore_interrupt.handler();
            }
        };
	
	
	static int port1a,port1b;
	
	public static ReadHandlerPtr zaccaria_port1a_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if ((~port1b & 1)!=0) return tms5220_status_r.handler(0);
		else return port1a;
	} };
	
	public static WriteHandlerPtr zaccaria_port1a_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		port1a = data;
	} };
	
	public static WriteHandlerPtr zaccaria_port1b_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		port1b = data;
	
		// bit 0 = /RS
	
		// bit 1 = /WS
		if ((~data & 2)!=0) tms5220_data_w.handler(0,port1a);
	
		// bit 3 = "ACS" (goes, inverted, to input port 6 bit 3)
		acs = ~data & 0x08;
	
		// bit 4 = led (for testing?)
		/*TODO*///set_led_status(0,~data & 0x10);
	} };
        
        static int counter;
	
	public static ReadHandlerPtr zaccaria_ca2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	// TODO: this doesn't work, why?
	//	return !tms5220_ready_r();
	
	counter = (counter+1) & 0x0f;
	
	return counter;
	
	} };
	
	static IrqPtr tms5220_irq_handler = new IrqPtr() {
            public void handler(int state) {
                pia_1_cb1_w.handler(0,state!=0 ? 0 : 1);
            }
        };
	
	static pia6821_interface pia_0_intf = new pia6821_interface
	(
		/*inputs : A/B,CA/B1,CA/B2 */ zaccaria_port0a_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ zaccaria_port0a_w, zaccaria_port0b_w, null, null,
		/*irqs   : A/B             */ zaccaria_irq0a, zaccaria_irq0b
	);
	
	static pia6821_interface pia_1_intf = new pia6821_interface
	(
		/*inputs : A/B,CA/B1,CA/B2 */ zaccaria_port1a_r, null, null, null, zaccaria_ca2_r, null,
		/*outputs: A/B,CA/B2       */ zaccaria_port1a_w, zaccaria_port1b_w, null, null,
		/*irqs   : A/B             */ null, null
	);
	
	
	static ppi8255_interface ppi8255_intf = new ppi8255_interface
	(
		1, 								/* 1 chip */
		new ReadHandlerPtr[]{input_port_3_r},				/* Port A read */
		new ReadHandlerPtr[]{input_port_4_r},				/* Port B read */
		new ReadHandlerPtr[]{input_port_5_r},				/* Port C read */
		new WriteHandlerPtr[]{null},							/* Port A write */
		new WriteHandlerPtr[]{null},							/* Port B write */
		new WriteHandlerPtr[]{zaccaria_dsw_sel_w} 			/* Port C write */
	);
	
	
	public static InitMachinePtr zaccaria_init_machine = new InitMachinePtr() { public void handler()
	{
		ppi8255_init(ppi8255_intf);
	
		pia_unconfig();
		pia_config(0, PIA_STANDARD_ORDERING, pia_0_intf);
		pia_config(1, PIA_STANDARD_ORDERING, pia_1_intf);
		pia_reset();
	} };
	
	
	public static WriteHandlerPtr sound_command_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		soundlatch_w.handler(0,data);
		cpu_set_irq_line(2,0,(data & 0x80)!=0 ? CLEAR_LINE : ASSERT_LINE);
	} };
	
	public static WriteHandlerPtr sound1_command_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		pia_0_ca1_w.handler(0,data & 0x80);
		soundlatch2_w.handler(0,data);
	} };
	
	public static WriteHandlerPtr mc1408_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		DAC_data_w(1,data);
	} };
	
	
	static GameDriver monymony_driver;
	
	public static ReadHandlerPtr zaccaria_prot1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch (offset)
		{
			case 0:
				return 0x50;    /* Money Money */
	
			case 4:
				return 0x40;    /* Jack Rabbit */
	
			case 6:
				if (Machine.gamedrv == monymony_driver)
					return 0x70;    /* Money Money */
				return 0xa0;    /* Jack Rabbit */
	
			default:
				return 0;
		}
	} };
	
	public static ReadHandlerPtr zaccaria_prot2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch (offset)
		{
			case 0:
				return (input_port_6_r.handler(0) & 0x07) | (acs & 0x08);   /* bits 4 and 5 must be 0 in Jack Rabbit */
	
			case 2:
				return 0x10;    /* Jack Rabbit */
	
			case 4:
				return 0x80;    /* Money Money */
	
			case 6:
				return 0x00;    /* Money Money */
	
			default:
				return 0;
		}
	} };
	
	
	public static WriteHandlerPtr coin_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_counter_w.handler(0,data & 1);
	} };
	
	public static WriteHandlerPtr nmienable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_w.handler(0,data & 1);
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x5fff, MRA_ROM ),
		new Memory_ReadAddress( 0x6000, 0x63ff, MRA_RAM ),
		new Memory_ReadAddress( 0x6400, 0x6407, zaccaria_prot1_r ),
		new Memory_ReadAddress( 0x6c00, 0x6c07, zaccaria_prot2_r ),
		new Memory_ReadAddress( 0x6e00, 0x6e00, zaccaria_dsw_r ),
		new Memory_ReadAddress( 0x7000, 0x77ff, MRA_RAM ),
		new Memory_ReadAddress( 0x7800, 0x7803, ppi8255_0_r ),
		new Memory_ReadAddress( 0x7c00, 0x7c00, watchdog_reset_r ),
		new Memory_ReadAddress( 0x8000, 0xdfff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x5fff, MWA_ROM ),
		new Memory_WriteAddress( 0x6000, 0x67ff, zaccaria_videoram_w, zaccaria_videoram ),	/* 6400-67ff is 4 bits wide */
		new Memory_WriteAddress( 0x6800, 0x683f, zaccaria_attributes_w, zaccaria_attributesram ),
		new Memory_WriteAddress( 0x6840, 0x685f, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x6881, 0x68bc, MWA_RAM, spriteram_2, spriteram_2_size ),
		new Memory_WriteAddress( 0x6c00, 0x6c00, zaccaria_flip_screen_x_w ),
		new Memory_WriteAddress( 0x6c01, 0x6c01, zaccaria_flip_screen_y_w ),
		new Memory_WriteAddress( 0x6c02, 0x6c02, MWA_NOP ),    /* sound reset */
		new Memory_WriteAddress( 0x6e00, 0x6e00, sound_command_w ),
		new Memory_WriteAddress( 0x6c06, 0x6c06, coin_w ),
		new Memory_WriteAddress( 0x6c07, 0x6c07, nmienable_w ),
		new Memory_WriteAddress( 0x7000, 0x77ff, MWA_RAM ),
		new Memory_WriteAddress( 0x7800, 0x7803, ppi8255_0_w ),
		new Memory_WriteAddress( 0x8000, 0xdfff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem1[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x007f, MRA_RAM ),
		new Memory_ReadAddress( 0x500c, 0x500f, pia_0_r ),
		new Memory_ReadAddress( 0xa000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xe000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem1[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x007f, MWA_RAM ),
		new Memory_WriteAddress( 0x500c, 0x500f, pia_0_w ),
		new Memory_WriteAddress( 0xa000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xe000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem2[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x007f, MRA_RAM ),
		new Memory_ReadAddress( 0x0090, 0x0093, pia_1_r ),
		new Memory_ReadAddress( 0x1800, 0x1800, soundlatch_r ),
		new Memory_ReadAddress( 0xa000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xe000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem2[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x007f, MWA_RAM ),
		new Memory_WriteAddress( 0x0090, 0x0093, pia_1_w ),
		new Memory_WriteAddress( 0x1000, 0x1000, mc1408_data_w ),	/* MC1408 */
		new Memory_WriteAddress( 0x1400, 0x1400, sound1_command_w ),
		new Memory_WriteAddress( 0xa000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xe000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_monymony = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0x01, "3" );
		PORT_DIPSETTING(    0x02, "4" );
		PORT_DIPSETTING(    0x03, "5" );
		PORT_BITX(    0x04, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Infinite Lives", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Easy" );
		PORT_DIPSETTING(    0x08, "Hard" );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x20, 0x00, "Freeze" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "Cross Hatch Pattern" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );  /* random high scores? */
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x01, "200000" );
		PORT_DIPSETTING(    0x02, "300000" );
		PORT_DIPSETTING(    0x03, "400000" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0x04, 0x00, "Table Title" );
		PORT_DIPSETTING(    0x00, "Todays High Scores" );
		PORT_DIPSETTING(    0x04, "High Scores" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x03, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_3C") );
		PORT_DIPNAME( 0x8c, 0x84, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x8c, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x88, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x84, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x70, 0x50, "Coin C" );
		PORT_DIPSETTING(    0x70, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_7C") );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		/* other bits are outputs */
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_SPECIAL );/* "ACS" - from pin 13 of a PIA on the sound board */
		/* other bits come from a protection device */
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_jackrabt = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0x01, "3" );
		PORT_DIPSETTING(    0x02, "4" );
		PORT_DIPSETTING(    0x03, "5" );
		PORT_BITX(    0x04, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Infinite Lives", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x20, 0x00, "Freeze" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "Cross Hatch Pattern" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "Table Title" );
		PORT_DIPSETTING(    0x00, "Todays High Scores" );
		PORT_DIPSETTING(    0x04, "High Scores" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x03, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_3C") );
		PORT_DIPNAME( 0x8c, 0x84, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x8c, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x88, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x84, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x70, 0x50, "Coin C" );
		PORT_DIPSETTING(    0x70, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_7C") );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		/* other bits are outputs */
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_SPECIAL );/* "ACS" - from pin 13 of a PIA on the sound board */
		/* other bits come from a protection device */
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,3),
		3,
		new int[] { RGN_FRAC(2,3), RGN_FRAC(1,3), RGN_FRAC(0,3) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,3),
		3,
		new int[] { RGN_FRAC(2,3), RGN_FRAC(1,3), RGN_FRAC(0,3) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7,
				8*8+0, 8*8+1, 8*8+2, 8*8+3, 8*8+4, 8*8+5, 8*8+6, 8*8+7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
				16*8, 17*8, 18*8, 19*8, 20*8, 21*8, 22*8, 23*8 },
		32*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,      0, 32 ),
		new GfxDecodeInfo( REGION_GFX1, 0, spritelayout, 32*8, 32 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		2,	/* 2 chips */
		3580000/2,
		new int[] { 15, 15 },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { soundlatch2_r, null },
		new WriteHandlerPtr[] { ay8910_port0a_w, null },
		new WriteHandlerPtr[] { null, null }
	);
	
	static DACinterface dac_interface = new DACinterface
	(
		2,
		new int[] { 0,80 }	/* I'm leaving the first DAC(?) off because it sounds awful */
	);
	
	static TMS5220interface tms5220_interface = new TMS5220interface
        (
		640000,				/* clock speed (80*samplerate) */
		80,					/* volume */
		tms5220_irq_handler	/* IRQ handler */
	);
	
	
	
	static MachineDriver machine_driver_zaccaria = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				18432000/6,	/* 3.072 MHz */
				readmem,writemem,null,null,
				nmi_interrupt,1
			),
			new MachineCPU(
				CPU_M6802 | CPU_AUDIO_CPU,
				3580000/4,	/* 895 kHz */
				sound_readmem1,sound_writemem1,null,null,
				ignore_interrupt,0,	/* IRQ and NMI triggered by the PIA */
				zaccaria_cb1_toggle,3580000/4096
			),
			new MachineCPU(
				CPU_M6802 | CPU_AUDIO_CPU,
				3580000/4,	/* 895 kHz */
				sound_readmem2,sound_writemem2,null,null,
				ignore_interrupt,0	/* IRQ triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,   /* frames per second, vblank duration */
		1,  /* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		zaccaria_init_machine,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		512, 32*8+32*8,
		zaccaria_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		zaccaria_vh_start,
		null,
		zaccaria_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			),
			new MachineSound(
				SOUND_DAC,
				dac_interface
			),
			new MachineSound(
				SOUND_TMS5220,
				tms5220_interface
			)
		}
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_monymony = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "1a",           0x0000, 0x1000, 0x13c227ca );
		ROM_CONTINUE(             0x8000, 0x1000 );
		ROM_LOAD( "1b",           0x1000, 0x1000, 0x87372545 );
		ROM_CONTINUE(             0x9000, 0x1000 );
		ROM_LOAD( "1c",           0x2000, 0x1000, 0x6aea9c01 );
		ROM_CONTINUE(             0xa000, 0x1000 );
		ROM_LOAD( "1d",           0x3000, 0x1000, 0x5fdec451 );
		ROM_CONTINUE(             0xb000, 0x1000 );
		ROM_LOAD( "2a",           0x4000, 0x1000, 0xaf830e3c );
		ROM_CONTINUE(             0xc000, 0x1000 );
		ROM_LOAD( "2c",           0x5000, 0x1000, 0x31da62b1 );
		ROM_CONTINUE(             0xd000, 0x1000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for first 6802 */
		ROM_LOAD( "2g",           0xa000, 0x2000, 0x78b01b98 );
		ROM_LOAD( "1i",           0xe000, 0x2000, 0x94e3858b );
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for second 6802 */
		ROM_LOAD( "1h",           0xa000, 0x1000, 0xaad76193 );
		ROM_CONTINUE(             0xe000, 0x1000 );
		ROM_LOAD( "1g",           0xb000, 0x1000, 0x1e8ffe3e );
		ROM_CONTINUE(             0xf000, 0x1000 );
	
		ROM_REGION( 0x6000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "2d",           0x0000, 0x2000, 0x82ab4d1a );
		ROM_LOAD( "1f",           0x2000, 0x2000, 0x40d4e4d1 );
		ROM_LOAD( "1e",           0x4000, 0x2000, 0x36980455 );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );
		ROM_LOAD( "monymony.9g",  0x0000, 0x0200, 0xfc9a0f21 );
		ROM_LOAD( "monymony.9f",  0x0200, 0x0200, 0x93106704 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_jackrabt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "cpu-01.1a",    0x0000, 0x1000, 0x499efe97 );
		ROM_CONTINUE(             0x8000, 0x1000 );
		ROM_LOAD( "cpu-01.2l",    0x1000, 0x1000, 0x4772e557 );
		ROM_LOAD( "cpu-01.3l",    0x2000, 0x1000, 0x1e844228 );
		ROM_LOAD( "cpu-01.4l",    0x3000, 0x1000, 0xebffcc38 );
		ROM_LOAD( "cpu-01.5l",    0x4000, 0x1000, 0x275e0ed6 );
		ROM_LOAD( "cpu-01.6l",    0x5000, 0x1000, 0x8a20977a );
		ROM_LOAD( "cpu-01.2h",    0x9000, 0x1000, 0x21f2be2a );
		ROM_LOAD( "cpu-01.3h",    0xa000, 0x1000, 0x59077027 );
		ROM_LOAD( "cpu-01.4h",    0xb000, 0x1000, 0x0b9db007 );
		ROM_LOAD( "cpu-01.5h",    0xc000, 0x1000, 0x785e1a01 );
		ROM_LOAD( "cpu-01.6h",    0xd000, 0x1000, 0xdd5979cf );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for first 6802 */
		ROM_LOAD( "13snd.2g",     0xa000, 0x2000, 0xfc05654e );
		ROM_LOAD( "9snd.1i",      0xe000, 0x2000, 0x3dab977f );
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for second 6802 */
		ROM_LOAD( "8snd.1h",      0xa000, 0x1000, 0xf4507111 );
		ROM_CONTINUE(             0xe000, 0x1000 );
		ROM_LOAD( "7snd.1g",      0xb000, 0x1000, 0xc722eff8 );
		ROM_CONTINUE(             0xf000, 0x1000 );
	
		ROM_REGION( 0x6000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1bg.2d",       0x0000, 0x2000, 0x9f880ef5 );
		ROM_LOAD( "2bg.1f",       0x2000, 0x2000, 0xafc04cd7 );
		ROM_LOAD( "3bg.1e",       0x4000, 0x2000, 0x14f23cdd );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );
		ROM_LOAD( "jr-ic9g",      0x0000, 0x0200, 0x85577107 );
		ROM_LOAD( "jr-ic9f",      0x0200, 0x0200, 0x085914d1 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_jackrab2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "1cpu2.1a",     0x0000, 0x1000, 0xf9374113 );
		ROM_CONTINUE(             0x8000, 0x1000 );
		ROM_LOAD( "2cpu2.1b",     0x1000, 0x1000, 0x0a0eea4a );
		ROM_CONTINUE(             0x9000, 0x1000 );
		ROM_LOAD( "3cpu2.1c",     0x2000, 0x1000, 0x291f5772 );
		ROM_CONTINUE(             0xa000, 0x1000 );
		ROM_LOAD( "4cpu2.1d",     0x3000, 0x1000, 0x10972cfb );
		ROM_CONTINUE(             0xb000, 0x1000 );
		ROM_LOAD( "5cpu2.2a",     0x4000, 0x1000, 0xaa95d06d );
		ROM_CONTINUE(             0xc000, 0x1000 );
		ROM_LOAD( "6cpu2.2c",     0x5000, 0x1000, 0x404496eb );
		ROM_CONTINUE(             0xd000, 0x1000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for first 6802 */
		ROM_LOAD( "13snd.2g",     0xa000, 0x2000, 0xfc05654e );
		ROM_LOAD( "9snd.1i",      0xe000, 0x2000, 0x3dab977f );
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for second 6802 */
		ROM_LOAD( "8snd.1h",      0xa000, 0x1000, 0xf4507111 );
		ROM_CONTINUE(             0xe000, 0x1000 );
		ROM_LOAD( "7snd.1g",      0xb000, 0x1000, 0xc722eff8 );
		ROM_CONTINUE(             0xf000, 0x1000 );
	
		ROM_REGION( 0x6000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1bg.2d",       0x0000, 0x2000, 0x9f880ef5 );
		ROM_LOAD( "2bg.1f",       0x2000, 0x2000, 0xafc04cd7 );
		ROM_LOAD( "3bg.1e",       0x4000, 0x2000, 0x14f23cdd );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );
		ROM_LOAD( "jr-ic9g",      0x0000, 0x0200, 0x85577107 );
		ROM_LOAD( "jr-ic9f",      0x0200, 0x0200, 0x085914d1 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_jackrabs = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "1cpu.1a",      0x0000, 0x1000, 0x6698dc65 );
		ROM_CONTINUE(             0x8000, 0x1000 );
		ROM_LOAD( "2cpu.1b",      0x1000, 0x1000, 0x42b32929 );
		ROM_CONTINUE(             0x9000, 0x1000 );
		ROM_LOAD( "3cpu.1c",      0x2000, 0x1000, 0x89b50c9a );
		ROM_CONTINUE(             0xa000, 0x1000 );
		ROM_LOAD( "4cpu.1d",      0x3000, 0x1000, 0xd5520665 );
		ROM_CONTINUE(             0xb000, 0x1000 );
		ROM_LOAD( "5cpu.2a",      0x4000, 0x1000, 0x0f9a093c );
		ROM_CONTINUE(             0xc000, 0x1000 );
		ROM_LOAD( "6cpu.2c",      0x5000, 0x1000, 0xf53d6356 );
		ROM_CONTINUE(             0xd000, 0x1000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for first 6802 */
		ROM_LOAD( "13snd.2g",     0xa000, 0x2000, 0xfc05654e );
		ROM_LOAD( "9snd.1i",      0xe000, 0x2000, 0x3dab977f );
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for second 6802 */
		ROM_LOAD( "8snd.1h",      0xa000, 0x1000, 0xf4507111 );
		ROM_CONTINUE(             0xe000, 0x1000 );
		ROM_LOAD( "7snd.1g",      0xb000, 0x1000, 0xc722eff8 );
		ROM_CONTINUE(             0xf000, 0x1000 );
	
		ROM_REGION( 0x6000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1bg.2d",       0x0000, 0x2000, 0x9f880ef5 );
		ROM_LOAD( "2bg.1f",       0x2000, 0x2000, 0xafc04cd7 );
		ROM_LOAD( "3bg.1e",       0x4000, 0x2000, 0x14f23cdd );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );
		ROM_LOAD( "jr-ic9g",      0x0000, 0x0200, 0x85577107 );
		ROM_LOAD( "jr-ic9f",      0x0200, 0x0200, 0x085914d1 );
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_monymony	   = new GameDriver("1983"	,"monymony"	,"zaccaria.java"	,rom_monymony,null	,machine_driver_zaccaria	,input_ports_monymony	,null	,ROT90	,	"Zaccaria", "Money Money", GAME_IMPERFECT_SOUND );
	public static GameDriver driver_jackrabt	   = new GameDriver("1984"	,"jackrabt"	,"zaccaria.java"	,rom_jackrabt,null	,machine_driver_zaccaria	,input_ports_jackrabt	,null	,ROT90	,	"Zaccaria", "Jack Rabbit (set 1)", GAME_IMPERFECT_SOUND );
	public static GameDriver driver_jackrab2	   = new GameDriver("1984"	,"jackrab2"	,"zaccaria.java"	,rom_jackrab2,driver_jackrabt	,machine_driver_zaccaria	,input_ports_jackrabt	,null	,ROT90	,	"Zaccaria", "Jack Rabbit (set 2)", GAME_IMPERFECT_SOUND );
	public static GameDriver driver_jackrabs	   = new GameDriver("1984"	,"jackrabs"	,"zaccaria.java"	,rom_jackrabs,driver_jackrabt	,machine_driver_zaccaria	,input_ports_jackrabt	,null	,ROT90	,	"Zaccaria", "Jack Rabbit (special)", GAME_IMPERFECT_SOUND );
}
