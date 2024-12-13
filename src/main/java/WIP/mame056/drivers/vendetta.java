/***************************************************************************

Vendetta (GX081) (c) 1991 Konami

Preliminary driver by:
Ernesto Corvi
someone@secureshell.com

Notes:
- collision detection is handled by a protection chip. Its emulation might
  not be 100% accurate.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstdio.*;
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
import static WIP.mame056.vidhrdw.vendetta.*;
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

public class vendetta
{
	
	
	/***************************************************************************
	
	  EEPROM
	
	***************************************************************************/
	
	static int init_eeprom_count;
	
	
	static EEPROM_interface eeprom_interface = new EEPROM_interface
	(
		7,				/* address bits */
		8,				/* data bits */
		"011000",		/*  read command */
		"011100",		/* write command */
		null,				/* erase command */
		"0100000000000",/* lock command */
		"0100110000000" /* unlock command */
        );
	
	static nvramPtr nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
		if (read_or_write != 0)
			EEPROM_save(file);
		else
		{
			EEPROM_init(eeprom_interface);
	
			if (file != null)
			{
				init_eeprom_count = 0;
				EEPROM_load(file);
			}
			else
				init_eeprom_count = 1000;
		}
            }
        };
	
	public static ReadHandlerPtr vendetta_eeprom_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int res;
	
		res = EEPROM_read_bit();
	
		res |= 0x02;//konami_eeprom_ack() << 5; /* add the ack */
	
		res |= readinputport( 3 ) & 0x0c; /* test switch */
	
		if (init_eeprom_count != 0)
		{
			init_eeprom_count--;
			res &= 0xfb;
		}
		return res;
	} };
	
	static int irq_enabled;
	
	public static WriteHandlerPtr vendetta_eeprom_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 - VOC0 - Video banking related */
		/* bit 1 - VOC1 - Video banking related */
		/* bit 2 - MSCHNG - Mono Sound select (Amp) */
		/* bit 3 - EEPCS - Eeprom CS */
		/* bit 4 - EEPCLK - Eeprom CLK */
		/* bit 5 - EEPDI - Eeprom data */
		/* bit 6 - IRQ enable */
		/* bit 7 - Unused */
	
		if ( data == 0xff ) /* this is a bug in the eeprom write code */
			return;
	
		/* EEPROM */
		EEPROM_write_bit(data & 0x20);
		EEPROM_set_clock_line((data & 0x10)!=0 ? ASSERT_LINE : CLEAR_LINE);
		EEPROM_set_cs_line((data & 0x08)!=0 ? CLEAR_LINE : ASSERT_LINE);
	
		irq_enabled = ( data >> 6 ) & 1;
	
		vendetta_video_banking( data & 1 );
	} };
	
	/********************************************/
	
	public static ReadHandlerPtr vendetta_K052109_r  = new ReadHandlerPtr() { public int handler(int offset) { return K052109_r.handler(offset + 0x2000 ); } };
	public static WriteHandlerPtr vendetta_K052109_w = new WriteHandlerPtr() {public void handler(int offset, int data) { K052109_w.handler(offset + 0x2000, data ); } };
	
	static void vendetta_video_banking( int select )
	{
		if (( select & 1 ) != 0)
		{
			memory_set_bankhandler_r( 2, 0, paletteram_r );
			memory_set_bankhandler_w( 2, 0, paletteram_xBBBBBGGGGGRRRRR_swap_w );
			memory_set_bankhandler_r( 3, 0, K053247_r );
			memory_set_bankhandler_w( 3, 0, K053247_w );
		}
		else
		{
			memory_set_bankhandler_r( 2, 0, vendetta_K052109_r );
			memory_set_bankhandler_w( 2, 0, vendetta_K052109_w );
			memory_set_bankhandler_r( 3, 0, K052109_r );
			memory_set_bankhandler_w( 3, 0, K052109_w );
		}
	}
	
	public static WriteHandlerPtr vendetta_5fe0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//char baf[40];
	//sprintf(baf,"5fe0 = %02x",data);
	//usrintf_showmessage(baf);
	
		/* bit 0,1 coin counters */
		coin_counter_w.handler(0,data & 0x01);
		coin_counter_w.handler(1,data & 0x02);
	
		/* bit 2 = BRAMBK ?? */
	
		/* bit 3 = enable char ROM reading through the video RAM */
		K052109_set_RMRD_line((data & 0x08)!=0 ? ASSERT_LINE : CLEAR_LINE);
	
		/* bit 4 = INIT ?? */
	
		/* bit 5 = enable sprite ROM reading */
		K053246_set_OBJCHA_line((data & 0x20)!=0 ? ASSERT_LINE : CLEAR_LINE);
	} };
	
	public static ReadHandlerPtr speedup_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		int data = ( RAM.read(0x28d2) << 8 ) | RAM.read(0x28d3);
	
		if ( data < memory_region_length(REGION_CPU1) )
		{
			data = ( RAM.read(data) << 8 ) | RAM.read(data + 1);
	
			if ( data == 0xffff )
				cpu_spinuntil_int();
		}
	
		return RAM.read(0x28d2);
	} };
	
	static timer_callback z80_nmi_callback = new timer_callback() {
            public void handler(int i) {
                cpu_set_nmi_line( 1, ASSERT_LINE );
            }
        };
	
	public static WriteHandlerPtr z80_arm_nmi_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_nmi_line( 1, CLEAR_LINE );
	
		timer_set( TIME_IN_USEC( 50 ), 0, z80_nmi_callback );
	} };
	
	public static WriteHandlerPtr z80_irq_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_cause_interrupt( 1, 0xff );
	} };
	
	public static ReadHandlerPtr vendetta_sound_interrupt_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		cpu_cause_interrupt( 1, 0xff );
		return 0x00;
	} };
        
        static int res = 0x00;
	
	public static ReadHandlerPtr vendetta_sound_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* If the sound CPU is running, read the status, otherwise
		   just make it pass the test */
		if (Machine.sample_rate != 0) 	return K053260_0_r.handler(2 + offset);
		else
		{
			
			res = ((res + 1) & 0x07);
			return offset!=0 ? res : 0x00;
		}
	} };
	
	/********************************************/
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x1fff, MRA_BANK1	),
		new Memory_ReadAddress( 0x28d2, 0x28d2, speedup_r ),
		new Memory_ReadAddress( 0x2000, 0x3fff, MRA_RAM ),
		new Memory_ReadAddress( 0x5f80, 0x5f9f, K054000_r ),
		new Memory_ReadAddress( 0x5fc0, 0x5fc0, input_port_0_r ),
		new Memory_ReadAddress( 0x5fc1, 0x5fc1, input_port_1_r ),
		new Memory_ReadAddress( 0x5fc2, 0x5fc2, input_port_4_r ),
		new Memory_ReadAddress( 0x5fc3, 0x5fc3, input_port_5_r ),
		new Memory_ReadAddress( 0x5fd0, 0x5fd0, vendetta_eeprom_r ), /* vblank, service */
		new Memory_ReadAddress( 0x5fd1, 0x5fd1, input_port_2_r ),
		new Memory_ReadAddress( 0x5fe4, 0x5fe4, vendetta_sound_interrupt_r ),
		new Memory_ReadAddress( 0x5fe6, 0x5fe7, vendetta_sound_r ),
		new Memory_ReadAddress( 0x5fe8, 0x5fe9, K053246_r ),
		new Memory_ReadAddress( 0x5fea, 0x5fea, watchdog_reset_r ),
		new Memory_ReadAddress( 0x4000, 0x4fff, MRA_BANK3 ),
		new Memory_ReadAddress( 0x6000, 0x6fff, MRA_BANK2 ),
		new Memory_ReadAddress( 0x4000, 0x7fff, K052109_r ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x1fff, MWA_ROM ),
		new Memory_WriteAddress( 0x2000, 0x3fff, MWA_RAM ),
		new Memory_WriteAddress( 0x5f80, 0x5f9f, K054000_w ),
		new Memory_WriteAddress( 0x5fa0, 0x5faf, K053251_w ),
		new Memory_WriteAddress( 0x5fb0, 0x5fb7, K053246_w ),
		new Memory_WriteAddress( 0x5fe0, 0x5fe0, vendetta_5fe0_w ),
		new Memory_WriteAddress( 0x5fe2, 0x5fe2, vendetta_eeprom_w ),
		new Memory_WriteAddress( 0x5fe4, 0x5fe4, z80_irq_w ),
		new Memory_WriteAddress( 0x5fe6, 0x5fe7, K053260_0_w ),
		new Memory_WriteAddress( 0x4000, 0x4fff, MWA_BANK3 ),
		new Memory_WriteAddress( 0x6000, 0x6fff, MWA_BANK2 ),
		new Memory_WriteAddress( 0x4000, 0x7fff, K052109_w ),
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress readmem_sound[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xefff, MRA_ROM ),
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf801, 0xf801, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0xfc00, 0xfc2f, K053260_0_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem_sound[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xefff, MWA_ROM ),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xf800, 0xf800, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0xf801, 0xf801, YM2151_data_port_0_w ),
		new Memory_WriteAddress( 0xfa00, 0xfa00, z80_arm_nmi_w ),
		new Memory_WriteAddress( 0xfc00, 0xfc2f, K053260_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/***************************************************************************
	
		Input Ports
	
	***************************************************************************/
	
	static InputPortPtr input_ports_vendet4p = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* EEPROM data */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* EEPROM ready */
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_VBLANK );/* not really vblank, object related. Its timed, otherwise sprites flicker */
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN3 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_vendetta = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* EEPROM data */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* EEPROM ready */
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_VBLANK );/* not really vblank, object related. Its timed, otherwise sprites flicker */
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	
	/***************************************************************************
	
		Machine Driver
	
	***************************************************************************/
	
	static YM2151interface ym2151_interface = new YM2151interface
	(
		1,			/* 1 chip */
		3579545,	/* 3.579545 MHz */
		new int[]{ YM3012_VOL(35,MIXER_PAN_LEFT,35,MIXER_PAN_RIGHT) },
		new WriteYmHandlerPtr[]{ null }
	);
	
	static K053260_interface k053260_interface = new K053260_interface
	(
		1,
		new int[]{ 3579545 },
		new int[]{ REGION_SOUND1 }, /* memory region */
		new int[][]{ { MIXER(75,MIXER_PAN_LEFT), MIXER(75,MIXER_PAN_RIGHT) } },
		new timer_callback[]{ null }
	);
	
	public static InterruptPtr vendetta_irq = new InterruptPtr() { public int handler() 
	{
		if (irq_enabled != 0)
			return KONAMI_INT_IRQ;
		else
			return ignore_interrupt.handler();
	} };
        
        static konami_cpu_setlines_callbackPtr vendetta_banking = new konami_cpu_setlines_callbackPtr() {
            public void handler(int lines) {
                UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		if ( lines >= 0x1c )
		{
			logerror("PC = %04x : Unknown bank selected %02x\n", cpu_get_pc(), lines );
		}
		else
			cpu_setbank( 1, new UBytePtr(RAM, 0x10000 + ( lines * 0x2000 ) ) );
            }
        };
	
	public static InitMachinePtr vendetta_init_machine = new InitMachinePtr() { public void handler()
	{
		konami_cpu_setlines_callback = vendetta_banking;
	
		paletteram = new UBytePtr(memory_region(REGION_CPU1), 0x48000);
		irq_enabled = 0;
	
		/* init banks */
		cpu_setbank( 1, new UBytePtr(memory_region(REGION_CPU1), 0x10000) );
		vendetta_video_banking( 0 );
	} };
	
	public static InitDriverPtr init_vendetta = new InitDriverPtr() { public void handler()
	{
		konami_rom_deinterleave_2(REGION_GFX1);
		konami_rom_deinterleave_4(REGION_GFX2);
	} };
	
	static MachineDriver machine_driver_vendetta = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_KONAMI,
				3000000,		/* ? */
				readmem,writemem,null,null,
				vendetta_irq,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3579545,
				readmem_sound, writemem_sound,null,null,
				ignore_interrupt,0	/* interrupts are triggered by the main CPU */
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		vendetta_init_machine,
	
		/* video hardware */
		64*8, 32*8, new rectangle( 13*8, (64)*8-1, 2*8, 30*8-1 ),
		null,	/* gfx decoded by konamiic.c */
		2048, 0,
		null,
	
		VIDEO_TYPE_RASTER | VIDEO_HAS_SHADOWS,
		null,
		vendetta_vh_start,
		vendetta_vh_stop,
		vendetta_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
			),
			new MachineSound(
				SOUND_K053260,
				k053260_interface
			)
		},
	
		nvram_handler
	);
	
	/***************************************************************************
	
	  Game ROMs
	
	***************************************************************************/
	
	static RomLoadPtr rom_vendetta = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x49000, REGION_CPU1, 0 );/* code + banked roms + banked ram */
		ROM_LOAD( "081t01", 0x10000, 0x38000, 0xe76267f5 );
		ROM_CONTINUE(		0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the sound CPU */
		ROM_LOAD( "081b02", 0x000000, 0x10000, 0x4c604d9b );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a09", 0x000000, 0x080000, 0xb4c777a9 );/* characters */
		ROM_LOAD( "081a08", 0x080000, 0x080000, 0x272ac8d9 );/* characters */
	
		ROM_REGION( 0x400000, REGION_GFX2, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a04", 0x000000, 0x100000, 0x464b9aa4 );/* sprites */
		ROM_LOAD( "081a05", 0x100000, 0x100000, 0x4e173759 );/* sprites */
		ROM_LOAD( "081a06", 0x200000, 0x100000, 0xe9fe6d80 );/* sprites */
		ROM_LOAD( "081a07", 0x300000, 0x100000, 0x8a22b29a );/* sprites */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* 053260 samples */
		ROM_LOAD( "081a03", 0x000000, 0x100000, 0x14b6baea );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vendetar = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x49000, REGION_CPU1, 0 );/* code + banked roms + banked ram */
		ROM_LOAD( "081r01", 0x10000, 0x38000, 0x84796281 );
		ROM_CONTINUE(		0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the sound CPU */
		ROM_LOAD( "081b02", 0x000000, 0x10000, 0x4c604d9b );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a09", 0x000000, 0x080000, 0xb4c777a9 );/* characters */
		ROM_LOAD( "081a08", 0x080000, 0x080000, 0x272ac8d9 );/* characters */
	
		ROM_REGION( 0x400000, REGION_GFX2, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a04", 0x000000, 0x100000, 0x464b9aa4 );/* sprites */
		ROM_LOAD( "081a05", 0x100000, 0x100000, 0x4e173759 );/* sprites */
		ROM_LOAD( "081a06", 0x200000, 0x100000, 0xe9fe6d80 );/* sprites */
		ROM_LOAD( "081a07", 0x300000, 0x100000, 0x8a22b29a );/* sprites */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* 053260 samples */
		ROM_LOAD( "081a03", 0x000000, 0x100000, 0x14b6baea );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vendetas = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x49000, REGION_CPU1, 0 );/* code + banked roms + banked ram */
		ROM_LOAD( "081u01", 0x10000, 0x38000, 0xb4d9ade5 );
		ROM_CONTINUE(		0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the sound CPU */
		ROM_LOAD( "081b02", 0x000000, 0x10000, 0x4c604d9b );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a09", 0x000000, 0x080000, 0xb4c777a9 );/* characters */
		ROM_LOAD( "081a08", 0x080000, 0x080000, 0x272ac8d9 );/* characters */
	
		ROM_REGION( 0x400000, REGION_GFX2, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a04", 0x000000, 0x100000, 0x464b9aa4 );/* sprites */
		ROM_LOAD( "081a05", 0x100000, 0x100000, 0x4e173759 );/* sprites */
		ROM_LOAD( "081a06", 0x200000, 0x100000, 0xe9fe6d80 );/* sprites */
		ROM_LOAD( "081a07", 0x300000, 0x100000, 0x8a22b29a );/* sprites */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* 053260 samples */
		ROM_LOAD( "081a03", 0x000000, 0x100000, 0x14b6baea );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vendeta2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x49000, REGION_CPU1, 0 );/* code + banked roms + banked ram */
		ROM_LOAD( "081d01", 0x10000, 0x38000, 0x335da495 );
		ROM_CONTINUE(		0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the sound CPU */
		ROM_LOAD( "081b02", 0x000000, 0x10000, 0x4c604d9b );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a09", 0x000000, 0x080000, 0xb4c777a9 );/* characters */
		ROM_LOAD( "081a08", 0x080000, 0x080000, 0x272ac8d9 );/* characters */
	
		ROM_REGION( 0x400000, REGION_GFX2, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a04", 0x000000, 0x100000, 0x464b9aa4 );/* sprites */
		ROM_LOAD( "081a05", 0x100000, 0x100000, 0x4e173759 );/* sprites */
		ROM_LOAD( "081a06", 0x200000, 0x100000, 0xe9fe6d80 );/* sprites */
		ROM_LOAD( "081a07", 0x300000, 0x100000, 0x8a22b29a );/* sprites */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* 053260 samples */
		ROM_LOAD( "081a03", 0x000000, 0x100000, 0x14b6baea );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vendettj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x49000, REGION_CPU1, 0 );/* code + banked roms + banked ram */
		ROM_LOAD( "081p01", 0x10000, 0x38000, 0x5fe30242 );
		ROM_CONTINUE(		0x08000, 0x08000 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the sound CPU */
		ROM_LOAD( "081b02", 0x000000, 0x10000, 0x4c604d9b );
	
		ROM_REGION( 0x100000, REGION_GFX1, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a09", 0x000000, 0x080000, 0xb4c777a9 );/* characters */
		ROM_LOAD( "081a08", 0x080000, 0x080000, 0x272ac8d9 );/* characters */
	
		ROM_REGION( 0x400000, REGION_GFX2, 0 );/* graphics ( don't dispose as the program can read them ) */
		ROM_LOAD( "081a04", 0x000000, 0x100000, 0x464b9aa4 );/* sprites */
		ROM_LOAD( "081a05", 0x100000, 0x100000, 0x4e173759 );/* sprites */
		ROM_LOAD( "081a06", 0x200000, 0x100000, 0xe9fe6d80 );/* sprites */
		ROM_LOAD( "081a07", 0x300000, 0x100000, 0x8a22b29a );/* sprites */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* 053260 samples */
		ROM_LOAD( "081a03", 0x000000, 0x100000, 0x14b6baea );
	ROM_END(); }}; 
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	
	public static GameDriver driver_vendetta	   = new GameDriver("1991"	,"vendetta"	,"vendetta.java"	,rom_vendetta,null	,machine_driver_vendetta	,input_ports_vendet4p	,init_vendetta	,ROT0	,	"Konami", "Vendetta (US ver. T)" );
	public static GameDriver driver_vendetar	   = new GameDriver("1991"	,"vendetar"	,"vendetta.java"	,rom_vendetar,driver_vendetta	,machine_driver_vendetta	,input_ports_vendet4p	,init_vendetta	,ROT0	,	"Konami", "Vendetta (US ver. R)" );
	public static GameDriver driver_vendetas	   = new GameDriver("1991"	,"vendetas"	,"vendetta.java"	,rom_vendetas,driver_vendetta	,machine_driver_vendetta	,input_ports_vendetta	,init_vendetta	,ROT0	,	"Konami", "Vendetta (Asia ver. U)" );
	public static GameDriver driver_vendeta2	   = new GameDriver("1991"	,"vendeta2"	,"vendetta.java"	,rom_vendeta2,driver_vendetta	,machine_driver_vendetta	,input_ports_vendetta	,init_vendetta	,ROT0	,	"Konami", "Vendetta (Asia ver. D)" );
	public static GameDriver driver_vendettj	   = new GameDriver("1991"	,"vendettj"	,"vendetta.java"	,rom_vendettj,driver_vendetta	,machine_driver_vendetta	,input_ports_vendetta	,init_vendetta	,ROT0	,	"Konami", "Crime Fighters 2 (Japan ver. P)" );
}
