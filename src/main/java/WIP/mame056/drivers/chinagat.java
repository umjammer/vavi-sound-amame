/*
China Gate.
By Paul Hampson from First Principles
(IE: Roms + a description of their contents and a list of CPUs on board.)

Based on ddragon.c:
"Double Dragon, Double Dragon (bootleg) & Double Dragon II"
"By Carlos A. Lozano & Rob Rosenbrock et. al."

NOTES:
A couple of things unaccounted for:

No backgrounds ROMs from the original board...
- This may be related to the SubCPU. I don't think it's contributing
  much right now, but I could be wrong. And it would explain that vast
  expanse of bankswitch ROM on a slave CPU....
- Just had a look at the sprites, and they seem like kosher sprites all
  the way up.... So it must be hidden in the sub-cpu somewhere?
- Got two bootleg sets with background gfx roms. Using those on the
  original games for now.

OBVIOUS SPEED PROBLEMS...
- Timers are too fast and/or too slow, and the whole thing's moving too fast

Port 0x2800 on the Sub CPU.
- All those I/O looking ports on the main CPU (0x3exx and 0x3fxx)
- One's scroll control. Prolly other vidhrdw control as well.
- Location 0x1a2ec in cgate51.bin (The main CPU's ROM) is 88. This is
  copied to videoram, and causes that minor visual discrepancy on
  the title screen. But the CPU tests that part of the ROM and passes
  it OK. Since it's just a simple summing of words, another word
  somewhere (or others in total) has lost 0x8000. Or the original
  game had this problem. (Not on the screenshot I got)
- The Japanese ones have a different title screen so I can't check.

ADPCM in the bootlegs is not quite right.... Misusing the data?
- They're nibble-swapped versions of the original roms...
- There's an Intel i8748 CPU on the bootlegs (bootleg 1 lists D8749 but
  the microcode dump's the same). This in conjunction with the different
  ADPCM chip (msm5205) are used to 'fake' a M6295.
- Bootleg 1 ADPCM is now wired up, but still not working :-(
  Definantly sync problems between the i8049 and the m5205 which need
  further looking at.


There's also a few small dumps from the boards.


MAJOR DIFFERENCES FROM DOUBLE DRAGON:
Sound system is like Double Dragon II (In fact for MAME's
purposes it's identical. I think DD3 and one or two others
also use this. Was it an addon on the original?
The dual-CPU setup looked similar to DD at first, but
the second CPU doesn't talk to the sprite RAM at all, but
just through the shared memory (which DD1 doesn't have,
except for the sprite RAM.)
Also the 2nd CPU in China Gate has just as much code as
the first CPU, and bankswitches similarly, where DD1 and DD2 have
different Sprite CPUs but only a small bank of code each.
More characters and colours of characters than DD1 or 2.
More sprites than DD1, less than DD2.
But the formats are the same (allowing for extra chars and colours)
Video hardware's like DD1 (thank god)
Input is unique but has a few similarities to DD2 (the coin inputs)


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
import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.sound.streams.*;
import static mame056.sound.k007232.*;
import static mame056.sound.k007232H.*;
import static WIP.mame056.vidhrdw.ddragon.*;
import static mame056.vidhrdw.generic.*;
import static mame056.cpu.m6809.m6809H.*;
import static mame056.inputH.*;
import static common.libc.cstring.*;
import static mame056.mame.Machine;
import static mame056.sound.mixerH.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.cpu.i8039.i8039H.*;
import static mame056.sound.MSM5205.*;
import static mame056.sound.MSM5205H.*;
import static mame056.sound._2151intf.*;
import static mame056.sound._2151intfH.*;
import static mame056.sound.oki6295.*;
import static mame056.sound.oki6295H.*;
import static mame056.drawgfxH.*;

public class chinagat
{
	
	/**************** Machine stuff ******************/
	static int sprite_irq, sound_irq, adpcm_sound_irq;
	static int saiyugb1_adpcm_addr;
	static int saiyugb1_i8748_P1;
	static int saiyugb1_i8748_P2;
	static int saiyugb1_pcm_shift;
	static int saiyugb1_pcm_nibble;
	static int saiyugb1_mcu_command;
/*TODO*///	#if 0
/*TODO*///	static int saiyugb1_m5205_clk;
/*TODO*///	#endif
	
	
	
	public static InitMachinePtr chinagat_init_machine = new InitMachinePtr() { public void handler()
	{
		technos_video_hw = 1;
		sprite_irq = M6809_INT_IRQ;
		sound_irq = Z80_NMI_INT;
	} };
	
	public static WriteHandlerPtr chinagat_video_ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/***************************
		---- ---x   X Scroll MSB
		---- --x-   Y Scroll MSB
		---- -x--   Flip screen
		--x- ----   Enable video ???
		****************************/
	
		ddragon_scrolly_hi = ( ( data & 0x02 ) << 7 );
		ddragon_scrollx_hi = ( ( data & 0x01 ) << 8 );
	
		flip_screen_set(~data & 0x04);
	} };
	
	public static WriteHandlerPtr chinagat_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
		cpu_setbank( 1, new UBytePtr(RAM,  0x10000 + (0x4000 * (data & 7)) ) );
	} };
	
	public static WriteHandlerPtr chinagat_sub_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region( REGION_CPU2 ));
		cpu_setbank( 4,new UBytePtr(RAM,  0x10000 + (0x4000 * (data & 7)) ) );
	} };
	
	public static WriteHandlerPtr chinagat_sub_IRQ_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_cause_interrupt( 1, sprite_irq );
	} };
	
	public static WriteHandlerPtr chinagat_cpu_sound_cmd_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		soundlatch_w.handler(offset, data );
		cpu_cause_interrupt( 2, sound_irq );
	} };
	
	public static ReadHandlerPtr saiyugb1_mcu_command_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
/*TODO*///	#if 0
/*TODO*///		if (saiyugb1_mcu_command == 0x78)
/*TODO*///		{
/*TODO*///			timer_suspendcpu(3, 1, SUSPEND_REASON_HALT);	/* Suspend (speed up) */
/*TODO*///		}
/*TODO*///	#endif
		return saiyugb1_mcu_command;
	} };
	
	public static WriteHandlerPtr saiyugb1_mcu_command_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		saiyugb1_mcu_command = data;
/*TODO*///	#if 0
/*TODO*///		if (data != 0x78)
/*TODO*///		{
/*TODO*///			timer_suspendcpu(3, 0, SUSPEND_REASON_HALT);	/* Wake up */
/*TODO*///		}
/*TODO*///	#endif
	} };
	
	public static WriteHandlerPtr saiyugb1_adpcm_rom_addr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* i8748 Port 1 write */
		saiyugb1_i8748_P1 = data;
	} };
	
	public static WriteHandlerPtr saiyugb1_adpcm_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* i8748 Port 2 write */
	
		UBytePtr saiyugb1_adpcm_rom = new UBytePtr(memory_region(REGION_SOUND1));
	
		if ((data & 0x80) != 0)	/* Reset m5205 and disable ADPCM ROM outputs */
		{
			logerror("ADPCM output disabled\n");
			saiyugb1_pcm_nibble = 0x0f;
			MSM5205_reset_w.handler(0,1);
		}
		else
		{
			if ( (saiyugb1_i8748_P2 & 0xc) != (data & 0xc) )
			{
				if ((saiyugb1_i8748_P2 & 0xc) == 0)	/* Latch MSB Address */
				{
	///				logerror("Latching MSB\n");
					saiyugb1_adpcm_addr = (saiyugb1_adpcm_addr & 0x3807f) | (saiyugb1_i8748_P1 << 7);
				}
				if ((saiyugb1_i8748_P2 & 0xc) == 4)	/* Latch LSB Address */
				{
	///				logerror("Latching LSB\n");
					saiyugb1_adpcm_addr = (saiyugb1_adpcm_addr & 0x3ff80) | (saiyugb1_i8748_P1 >> 1);
					saiyugb1_pcm_shift = (saiyugb1_i8748_P1 & 1) * 4;
				}
			}
	
			saiyugb1_adpcm_addr = ((saiyugb1_adpcm_addr & 0x07fff) | (data & 0x70 << 11));
	
			saiyugb1_pcm_nibble = saiyugb1_adpcm_rom.read(saiyugb1_adpcm_addr & 0x3ffff);
	
			saiyugb1_pcm_nibble = (saiyugb1_pcm_nibble >> saiyugb1_pcm_shift) & 0x0f;
	
	///		logerror("Writing %02x to m5205. $ROM=%08x  P1=%02x  P2=%02x  Prev_P2=%02x  Nibble=%08x\n",saiyugb1_pcm_nibble,saiyugb1_adpcm_addr,saiyugb1_i8748_P1,data,saiyugb1_i8748_P2,saiyugb1_pcm_shift);
	
			if ( ((saiyugb1_i8748_P2 & 0xc) >= 8) && ((data & 0xc) == 4) )
			{
				MSM5205_data_w.handler(0, saiyugb1_pcm_nibble);
				logerror("Writing %02x to m5205\n",saiyugb1_pcm_nibble);
			}
			logerror("$ROM=%08x  P1=%02x  P2=%02x  Prev_P2=%02x  Nibble=%1x  PCM_data=%02x\n",saiyugb1_adpcm_addr,saiyugb1_i8748_P1,data,saiyugb1_i8748_P2,saiyugb1_pcm_shift,saiyugb1_pcm_nibble);
		}
		saiyugb1_i8748_P2 = data;
	} };
	
	public static WriteHandlerPtr saiyugb1_m5205_clk_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* i8748 T0 output clk mode */
		/* This signal goes through a divide by 8 counter */
		/* to the xtal pins of the MSM5205 */
	
		/* Actually, T0 output clk mode is not supported by the i8048 core */
	
/*TODO*///	#if 0
/*TODO*///		saiyugb1_m5205_clk++;
/*TODO*///		if (saiyugb1_m5205_clk == 8)
/*TODO*///		} };
/*TODO*///			MSM5205_vclk_w (0, 1);		/* ??? */
/*TODO*///			saiyugb1_m5205_clk = 0;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		}
/*TODO*///			MSM5205_vclk_w (0, 0);		/* ??? */
/*TODO*///		}
/*TODO*///	#endif
            }
        };
	
	public static ReadHandlerPtr saiyugb1_m5205_irq_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (adpcm_sound_irq != 0)
		{
			adpcm_sound_irq = 0;
			return 1;
		}
		return 0;
	} };
	static vclk_interruptPtr saiyugb1_m5205_irq_w = new vclk_interruptPtr() {
            public void handler(int num) {
            
		adpcm_sound_irq = 1;
            }
        };
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x1fff, MRA_BANK2 ),
		new Memory_ReadAddress( 0x3f00, 0x3f00, input_port_0_r ),
		new Memory_ReadAddress( 0x3f01, 0x3f01, input_port_1_r ),
		new Memory_ReadAddress( 0x3f02, 0x3f02, input_port_2_r ),
		new Memory_ReadAddress( 0x3f03, 0x3f03, input_port_3_r ),
		new Memory_ReadAddress( 0x3f04, 0x3f04, input_port_4_r ),
		new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x1fff, MWA_BANK2 ),
		new Memory_WriteAddress( 0x2000, 0x27ff, ddragon_fgvideoram_w, ddragon_fgvideoram ),
		new Memory_WriteAddress( 0x2800, 0x2fff, ddragon_bgvideoram_w, ddragon_bgvideoram ),
		new Memory_WriteAddress( 0x3000, 0x317f, paletteram_xxxxBBBBGGGGRRRR_split1_w, paletteram ),
		new Memory_WriteAddress( 0x3400, 0x357f, paletteram_xxxxBBBBGGGGRRRR_split2_w, paletteram_2 ),
		new Memory_WriteAddress( 0x3800, 0x397f, MWA_BANK3, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x3e00, 0x3e00, chinagat_cpu_sound_cmd_w ),
	//	new Memory_WriteAddress( 0x3e01, 0x3e01, MWA_NOP ),
	//	new Memory_WriteAddress( 0x3e02, 0x3e02, MWA_NOP ),
	//	new Memory_WriteAddress( 0x3e03, 0x3e03, MWA_NOP ),
		new Memory_WriteAddress( 0x3e04, 0x3e04, chinagat_sub_IRQ_w ),
		new Memory_WriteAddress( 0x3e06, 0x3e06, MWA_RAM, ddragon_scrolly_lo ),
		new Memory_WriteAddress( 0x3e07, 0x3e07, MWA_RAM, ddragon_scrollx_lo ),
		new Memory_WriteAddress( 0x3f00, 0x3f00, chinagat_video_ctrl_w ),
		new Memory_WriteAddress( 0x3f01, 0x3f01, chinagat_bankswitch_w ),
		new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sub_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x1fff, MRA_BANK2 ),
	//	new Memory_ReadAddress( 0x2a2b, 0x2a2b, MRA_NOP ), /* What lives here? */
	//	new Memory_ReadAddress( 0x2a30, 0x2a30, MRA_NOP ), /* What lives here? */
		new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK4 ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sub_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x1fff, MWA_BANK2 ),
		new Memory_WriteAddress( 0x2000, 0x2000, chinagat_sub_bankswitch_w ),
		new Memory_WriteAddress( 0x2800, 0x2800, MWA_RAM ), /* Called on CPU start and after return from jump table */
		new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8801, 0x8801, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0x9800, 0x9800, OKIM6295_status_0_r ),
		new Memory_ReadAddress( 0xA000, 0xA000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x8800, 0x8800, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0x8801, 0x8801, YM2151_data_port_0_w ),
		new Memory_WriteAddress( 0x9800, 0x9800, OKIM6295_data_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress ym2203c_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8800, 0x8800, YM2203_status_port_0_r ),
	//	new Memory_ReadAddress( 0x8802, 0x8802, OKIM6295_status_0_r ),
		new Memory_ReadAddress( 0x8804, 0x8804, YM2203_status_port_1_r ),
	//	new Memory_ReadAddress( 0x8801, 0x8801, YM2151_status_port_0_r ),
	//	new Memory_ReadAddress( 0x9800, 0x9800, OKIM6295_status_0_r ),
		new Memory_ReadAddress( 0xA000, 0xA000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress ym2203c_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
	// 8804 and/or 8805 make a gong sound when the coin goes in
	// but only on the title screen....
	
		new Memory_WriteAddress( 0x8800, 0x8800, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0x8801, 0x8801, YM2203_write_port_0_w ),
	//	new Memory_WriteAddress( 0x8802, 0x8802, OKIM6295_data_0_w ),
	//	new Memory_WriteAddress( 0x8803, 0x8803, OKIM6295_data_0_w ),
		new Memory_WriteAddress( 0x8804, 0x8804, YM2203_control_port_1_w ),
		new Memory_WriteAddress( 0x8805, 0x8805, YM2203_write_port_1_w ),
	//	new Memory_WriteAddress( 0x8804, 0x8804, MWA_RAM ),
	//	new Memory_WriteAddress( 0x8805, 0x8805, MWA_RAM ),
	
	//	new Memory_WriteAddress( 0x8800, 0x8800, YM2151_register_port_0_w ),
	//	new Memory_WriteAddress( 0x8801, 0x8801, YM2151_data_port_0_w ),
	//	new Memory_WriteAddress( 0x9800, 0x9800, OKIM6295_data_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress saiyugb1_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8801, 0x8801, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0xA000, 0xA000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress saiyugb1_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x8800, 0x8800, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0x8801, 0x8801, YM2151_data_port_0_w ),
		new Memory_WriteAddress( 0x9800, 0x9800, saiyugb1_mcu_command_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress i8748_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x03ff, MRA_ROM ),
		new Memory_ReadAddress( 0x0400, 0x07ff, MRA_ROM ),	/* i8749 version */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress i8748_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x03ff, MWA_ROM ),
		new Memory_WriteAddress( 0x0400, 0x07ff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort i8748_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( I8039_bus, I8039_bus, saiyugb1_mcu_command_r ),
		new IO_ReadPort( I8039_t1,  I8039_t1,  saiyugb1_m5205_irq_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort i8748_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( I8039_t0,  I8039_t0,  saiyugb1_m5205_clk_w ), 		/* Drives the clock on the m5205 at 1/8 of this frequency */
		new IO_WritePort( I8039_p1,  I8039_p1,  saiyugb1_adpcm_rom_addr_w ),
		new IO_WritePort( I8039_p2,  I8039_p2,  saiyugb1_adpcm_control_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_chinagat = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_VBLANK );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0xf0, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_5C") );
		PORT_DIPNAME( 0x38, 0x38, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x38, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x28, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_5C") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(	0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(	0x40, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(	0x01, "Easy" );
		PORT_DIPSETTING(	0x03, "Normal" );
		PORT_DIPSETTING(	0x02, "Hard" );
		PORT_DIPSETTING(	0x00, "Hardest" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, "Timer" );
		PORT_DIPSETTING(    0x00, "50" );
		PORT_DIPSETTING(    0x20, "55" );
		PORT_DIPSETTING(    0x30, "60" );
		PORT_DIPSETTING(    0x10, "70" );
		PORT_DIPNAME( 0xc0, 0x80, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0xc0, "2" );
		PORT_DIPSETTING(    0x80, "3" );
		PORT_DIPSETTING(    0x40, "4" );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	INPUT_PORTS_END(); }}; 
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,			/* 8*8 chars */
		RGN_FRAC(1,1),	/* num of characters */
		4,				/* 4 bits per pixel */
		new int[] { 0, 2, 4, 6 },		/* plane offset */
		new int[] { 1, 0, 65, 64, 129, 128, 193, 192 },
		new int[] { STEP8(0,0,8) },			/* { 0*8, 1*8 ... 6*8, 7*8 }, */
		32*8 /* every char takes 32 consecutive bytes */
	);
	
	static GfxLayout tilelayout = new GfxLayout
	(
		16,16,			/* 16x16 chars */
		RGN_FRAC(1,2),	/* num of Tiles/Sprites */
		4,				/* 4 bits per pixel */
		new int[] { RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+4, 0,4 }, /* plane offset */
		new int[] { 3, 2, 1, 0, 16*8+3, 16*8+2, 16*8+1, 16*8+0,
			32*8+3,32*8+2 ,32*8+1 ,32*8+0 ,48*8+3 ,48*8+2 ,48*8+1 ,48*8+0 },
		new int[] { STEP16(0,0,8) },		/* { 0*8, 1*8 ... 14*8, 15*8 }, */
		64*8 /* every char takes 64 consecutive bytes */
	);
	
/*TODO*///	static GfxDecodeInfo gfxdecodeinfo[] =
/*TODO*///	{
/*TODO*///		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   0,16 ),	/*  8x8  chars */
/*TODO*///		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout, 128, 8 ),	/* 16x16 sprites */
/*TODO*///		new GfxDecodeInfo( REGION_GFX3, 0, tilelayout, 256, 8 ),	/* 16x16 background tiles */
/*TODO*///		new GfxDecodeInfo( -1 ) /* end of array */
/*TODO*///	};
        
        static GfxLayout char_layout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		4,
		new int[] { 0, 2, 4, 6 },
		new int[] { 1, 0, 8*8+1, 8*8+0, 16*8+1, 16*8+0, 24*8+1, 24*8+0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		32*8
	);
	
	static GfxLayout tile_layout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,2),
		4,
		new int[] { RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+4, 0, 4 },
		new int[] { 3, 2, 1, 0, 16*8+3, 16*8+2, 16*8+1, 16*8+0,
			  32*8+3, 32*8+2, 32*8+1, 32*8+0, 48*8+3, 48*8+2, 48*8+1, 48*8+0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			  8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
		64*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, char_layout,   0, 8 ),	/* colors   0-127 */
		new GfxDecodeInfo( REGION_GFX2, 0, tile_layout, 128, 8 ),	/* colors 128-255 */
		new GfxDecodeInfo( REGION_GFX3, 0, tile_layout, 256, 8 ),	/* colors 256-383 */
		new GfxDecodeInfo( -1 )
	};
	
	static WriteYmHandlerPtr chinagat_irq_handler = new WriteYmHandlerPtr() {
            public void handler(int irq) {
                
		cpu_set_irq_line( 2, 0, irq!=0 ? ASSERT_LINE : CLEAR_LINE );
            }
        };
	
	static YM2151interface ym2151_interface = new YM2151interface
	(
		1,			/* 1 chip */
		3579545,	/* 3.579545 oscillator */
		new int[]{ YM3012_VOL(80,MIXER_PAN_LEFT,80,MIXER_PAN_RIGHT) },	/* only right channel is connected */
		new WriteYmHandlerPtr[]{ chinagat_irq_handler }
	);
	
	
	static OKIM6295interface okim6295_interface = new OKIM6295interface
	(
		1,					/* 1 chip */
		new int[]{ 11000 },			/* ??? frequency (Hz) */
		new int[]{ REGION_SOUND1 },	/* memory region */
		new int[]{ 45 }
	);
	
	/* This on the bootleg board, instead of the m6295 */
	static MSM5205interface msm5205_interface = new MSM5205interface
	(
		1,							/* 1 chip */
		9263750 / 24,				/* 385989.6 Hz from the 9.26375MHz oscillator */
		new vclk_interruptPtr[]{ saiyugb1_m5205_irq_w },	/* Interrupt function */
		new int[]{ MSM5205_S64_4B },			/* vclk input mode (6030Hz, 4-bit) */
		new int[]{ 60 }
	);
	
	public static InterruptPtr chinagat_interrupt = new InterruptPtr() { public int handler() 
	{
		cpu_set_irq_line(0, 1, HOLD_LINE);	/* hold the FIRQ line */
		cpu_set_nmi_line(0, PULSE_LINE);	/* pulse the NMI line */
		return ignore_interrupt.handler();
	} };
	
	/* This is only on the second bootleg board */
	static YM2203interface ym2203_interface = new YM2203interface
	(
		2,			/* 2 chips */
		3579545,	/* 3.579545 oscillator */
		new int[]{ YM2203_VOL(80,50), YM2203_VOL(80,50) },
		new ReadHandlerPtr[]{ null, null },
		new ReadHandlerPtr[]{ null, null },
		new WriteHandlerPtr[]{ null, null },
		new WriteHandlerPtr[]{ null, null },
		new WriteYmHandlerPtr[]{ chinagat_irq_handler }
	);
	
	static MachineDriver machine_driver_chinagat = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_HD6309,
				12000000/8,		/* 1.5 MHz (12MHz oscillator ???) */
				readmem,writemem,null,null,
				chinagat_interrupt,1
			),
			new MachineCPU(
				CPU_HD6309,
				12000000/8,		/* 1.5 MHz (12MHz oscillator ???) */
				sub_readmem,sub_writemem,null,null,
				ignore_interrupt,0
			),
			new MachineCPU(
				CPU_Z80,
				3579545,	/* 3.579545 MHz */
				sound_readmem,sound_writemem,null,null,
				ignore_interrupt,0
			)
		},
		56, DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		100, /* heavy interleaving to sync up sprite<->main cpu's */
		chinagat_init_machine,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		384, 0,
		null,
		VIDEO_TYPE_RASTER,
		null,
		chinagat_vh_start,
		null,
		ddragon_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
			),
			new MachineSound(
				SOUND_OKIM6295,
				okim6295_interface
			)
		}
                
	);
	
	static MachineDriver machine_driver_saiyugb1 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6809,		/* 68B09EP */
				12000000/8,		/* 1.5 MHz (12MHz oscillator) */
				readmem,writemem,null,null,
				chinagat_interrupt,1
			),
			new MachineCPU(
				CPU_M6809,		/* 68B09EP */
				12000000/8,		/* 1.5 MHz (12MHz oscillator) */
				sub_readmem,sub_writemem,null,null,
				ignore_interrupt,0
			),
			new MachineCPU(
				CPU_Z80,
				3579545,		/* 3.579545 MHz oscillator */
				saiyugb1_sound_readmem,saiyugb1_sound_writemem,null,null,
				ignore_interrupt,0
			),
			new MachineCPU(
				CPU_I8048,
				9263750/3,		/* 3.087916 MHz (9.263750 MHz oscillator) */
				i8748_readmem,i8748_writemem,i8748_readport,i8748_writeport,
				ignore_interrupt,0
                        )
		},
		56, DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		100, /* heavy interleaving to sync up sprite<->main cpu's */
		chinagat_init_machine,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		384, 0,
		null,
		VIDEO_TYPE_RASTER,
		null,
		chinagat_vh_start,
		null,
		ddragon_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
			),
			new MachineSound(
				SOUND_MSM5205,
				msm5205_interface
			)
		}
                
	);
	
	static MachineDriver machine_driver_saiyugb2 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6809,
				12000000/8,		/* 1.5 MHz (12MHz oscillator) */
				readmem,writemem,null,null,
				chinagat_interrupt,1
			),
			new MachineCPU(
				CPU_M6809,
				12000000/8,		/* 1.5 MHz (12MHz oscillator) */
				sub_readmem,sub_writemem,null,null,
				ignore_interrupt,0
			),
			new MachineCPU(
				CPU_Z80,
				3579545,		/* 3.579545 MHz oscillator */
				ym2203c_sound_readmem,ym2203c_sound_writemem,null,null,
				ignore_interrupt,0
			)
		},
		56, DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		100, /* heavy interleaving to sync up sprite<->main cpu's */
		chinagat_init_machine,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		384, 0,
		null,
		VIDEO_TYPE_RASTER,
		null,
		chinagat_vh_start,
		null,
		ddragon_vh_screenrefresh,
	
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
	
	static RomLoadPtr rom_chinagat = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );/* Main CPU: 128KB for code (bankswitched using $3F01) */
		ROM_LOAD( "cgate51.bin", 0x10000, 0x18000, 0x439a3b19 );/* Banks 0x4000 long @ 0x4000 */
		ROM_CONTINUE(            0x08000, 0x08000 );			/* Static code */
	
		ROM_REGION( 0x28000, REGION_CPU2, 0 );/* Slave CPU: 128KB for code (bankswitched using $2000) */
		ROM_LOAD( "23j4-0.48",   0x10000, 0x18000, 0x2914af38 );/* Banks 0x4000 long @ 0x4000 */
		ROM_CONTINUE(            0x08000, 0x08000 );			/* Static code */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* Music CPU, 64KB */
		ROM_LOAD( "23j0-0.40",   0x00000, 0x08000, 0x9ffcadb6 );
	
		ROM_REGION(0x20000, REGION_GFX1, ROMREGION_DISPOSE );/* Text */
		ROM_LOAD( "cgate18.bin", 0x00000, 0x20000, 0x8d88d64d );/* 0,1,2,3 */
	
		ROM_REGION(0x80000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "23j7-0.103",  0x00000, 0x20000, 0x2f445030 );/* 2,3 */
		ROM_LOAD( "23j8-0.102",  0x20000, 0x20000, 0x237f725a );/* 2,3 */
		ROM_LOAD( "23j9-0.101",  0x40000, 0x20000, 0x8caf6097 );/* 0,1 */
		ROM_LOAD( "23ja-0.100",  0x60000, 0x20000, 0xf678594f );/* 0,1 */
	
		ROM_REGION(0x40000, REGION_GFX3, ROMREGION_DISPOSE );/* Background */
		ROM_LOAD( "chinagat_a-13", 0x00000, 0x10000, 0x00000000 );	/* Where are    */
		ROM_LOAD( "chinagat_a-12", 0x10000, 0x10000, 0x00000000 );	/* these on the */
		ROM_LOAD( "chinagat_a-15", 0x20000, 0x10000, 0x00000000 );	/* real board ? */
		ROM_LOAD( "chinagat_a-14", 0x30000, 0x10000, 0x00000000 );
	
		ROM_REGION(0x40000, REGION_SOUND1, 0 );/* ADPCM */
		ROM_LOAD( "23j1-0.53", 0x00000, 0x20000, 0xf91f1001 );
		ROM_LOAD( "23j2-0.52", 0x20000, 0x20000, 0x8b6f26e9 );
	
		ROM_REGION(0x300, REGION_USER1, 0 );/* Unknown Bipolar PROMs */
		ROM_LOAD( "23jb-0.16", 0x000, 0x200, 0x46339529 );/* 82S131 on video board */
		ROM_LOAD( "23j5-0.45", 0x200, 0x100, 0xfdb130a9 );/* 82S129 on main board */
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_saiyugou = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );/* Main CPU: 128KB for code (bankswitched using $3F01) */
		ROM_LOAD( "23j3-0.51",  0x10000, 0x18000, 0xaa8132a2 );/* Banks 0x4000 long @ 0x4000 */
		ROM_CONTINUE(           0x08000, 0x08000);			/* Static code */
	
		ROM_REGION( 0x28000, REGION_CPU2, 0 );/* Slave CPU: 128KB for code (bankswitched using $2000) */
		ROM_LOAD( "23j4-0.48",  0x10000, 0x18000, 0x2914af38 );/* Banks 0x4000 long @ 0x4000 */
		ROM_CONTINUE(           0x08000, 0x08000);			/* Static code */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* Music CPU, 64KB */
		ROM_LOAD( "23j0-0.40",  0x00000, 0x8000, 0x9ffcadb6 );
	
		ROM_REGION(0x20000, REGION_GFX1, ROMREGION_DISPOSE );/* Text */
		ROM_LOAD( "23j6-0.18",  0x00000, 0x20000, 0x86d33df0 );/* 0,1,2,3 */
	
		ROM_REGION(0x80000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "23j7-0.103", 0x00000, 0x20000, 0x2f445030 );/* 2,3 */
		ROM_LOAD( "23j8-0.102", 0x20000, 0x20000, 0x237f725a );/* 2,3 */
		ROM_LOAD( "23j9-0.101", 0x40000, 0x20000, 0x8caf6097 );/* 0,1 */
		ROM_LOAD( "23ja-0.100", 0x60000, 0x20000, 0xf678594f );/* 0,1 */
	
		ROM_REGION(0x40000, REGION_GFX3, ROMREGION_DISPOSE );/* Background */
		ROM_LOAD( "a-13", 0x00000, 0x10000, 0x00000000 );
		ROM_LOAD( "a-12", 0x10000, 0x10000, 0x00000000 );
		ROM_LOAD( "a-15", 0x20000, 0x10000, 0x00000000 );
		ROM_LOAD( "a-14", 0x30000, 0x10000, 0x00000000 );
	
		ROM_REGION(0x40000, REGION_SOUND1, 0 );/* ADPCM */
		ROM_LOAD( "23j1-0.53", 0x00000, 0x20000, 0xf91f1001 );
		ROM_LOAD( "23j2-0.52", 0x20000, 0x20000, 0x8b6f26e9 );
	
		ROM_REGION(0x300, REGION_USER1, 0 );/* Unknown Bipolar PROMs */
		ROM_LOAD( "23jb-0.16", 0x000, 0x200, 0x46339529 );/* 82S131 on video board */
		ROM_LOAD( "23j5-0.45", 0x200, 0x100, 0xfdb130a9 );/* 82S129 on main board */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_saiyugb1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );/* Main CPU: 128KB for code (bankswitched using $3F01) */
		ROM_LOAD( "23j3-0.51",  0x10000, 0x18000, 0xaa8132a2 );/* Banks 0x4000 long @ 0x4000 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same.
		   ROM_LOAD( "a-5.bin", 0x10000, 0x10000, 0x39795aa5 );   Banks 0x4000 long @ 0x4000
		   ROM_LOAD( "a-9.bin", 0x20000, 0x08000, 0x051ebe92 );   Banks 0x4000 long @ 0x4000
		*/
		ROM_CONTINUE(           0x08000, 0x08000 );			/* Static code */
	
		ROM_REGION( 0x28000, REGION_CPU2, 0 );/* Slave CPU: 128KB for code (bankswitched using $2000) */
		ROM_LOAD( "23j4-0.48",  0x10000, 0x18000, 0x2914af38 );/* Banks 0x4000 long @ 0x4000 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same.
		   ROM_LOAD( "a-4.bin", 0x10000, 0x10000, 0x9effddc1 );   Banks 0x4000 long @ 0x4000
		   ROM_LOAD( "a-8.bin", 0x20000, 0x08000, 0xa436edb8 );   Banks 0x4000 long @ 0x4000
		*/
		ROM_CONTINUE(           0x08000, 0x08000 );			/* Static code */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* Music CPU, 64KB */
		ROM_LOAD( "a-1.bin",  0x00000, 0x8000,  0x46e5a6d4 );
	
		ROM_REGION( 0x800, REGION_CPU4, 0 );	/* ADPCM CPU, 1KB */
		ROM_LOAD( "mcu8748.bin", 0x000, 0x400, 0x6d28d6c5 );
	
		ROM_REGION(0x20000, REGION_GFX1, ROMREGION_DISPOSE );/* Text */
		ROM_LOAD( "23j6-0.18",  0x00000, 0x20000, 0x86d33df0 );/* 0,1,2,3 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same.
		   ROM_LOAD( "a-2.bin", 0x00000, 0x10000, 0xbaa5a3b9 );   0,1
		   ROM_LOAD( "a-3.bin", 0x10000, 0x10000, 0x532d59be );   2,3
		*/
	
		ROM_REGION(0x80000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "23j7-0.103",  0x00000, 0x20000, 0x2f445030 );/* 2,3 */
		ROM_LOAD( "23j8-0.102",  0x20000, 0x20000, 0x237f725a );/* 2,3 */
		ROM_LOAD( "23j9-0.101",  0x40000, 0x20000, 0x8caf6097 );/* 0,1 */
		ROM_LOAD( "23ja-0.100",  0x60000, 0x20000, 0xf678594f );/* 0,1 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same
		   ROM_LOAD( "a-23.bin", 0x00000, 0x10000, 0x12b56225 );   2,3
		   ROM_LOAD( "a-22.bin", 0x10000, 0x10000, 0xb592aa9b );   2,3
		   ROM_LOAD( "a-21.bin", 0x20000, 0x10000, 0xa331ba3d );   2,3
		   ROM_LOAD( "a-20.bin", 0x30000, 0x10000, 0x2515d742 );   2,3
		   ROM_LOAD( "a-19.bin", 0x40000, 0x10000, 0xd796f2e4 );   0,1
		   ROM_LOAD( "a-18.bin", 0x50000, 0x10000, 0xc9e1c2f9 );   0,1
		   ROM_LOAD( "a-17.bin", 0x60000, 0x10000, 0x00b6db0a );   0,1
		   ROM_LOAD( "a-16.bin", 0x70000, 0x10000, 0xf196818b );   0,1
		*/
	
		ROM_REGION(0x40000, REGION_GFX3, ROMREGION_DISPOSE );/* Background */
		ROM_LOAD( "a-13", 0x00000, 0x10000, 0xb745cac4 );
		ROM_LOAD( "a-12", 0x10000, 0x10000, 0x3c864299 );
		ROM_LOAD( "a-15", 0x20000, 0x10000, 0x2f268f37 );
		ROM_LOAD( "a-14", 0x30000, 0x10000, 0xaef814c8 );
	
		/* Some bootlegs have incorrectly halved the ADPCM data ! */
		/* These are same as the 128k sample except nibble-swapped */
		ROM_REGION(0x40000, REGION_SOUND1, 0 );/* ADPCM */		/* Bootleggers wrong data */
		ROM_LOAD ( "a-6.bin",   0x00000, 0x10000, 0x4da4e935 );/* 0x8000, 0x7cd47f01 */
		ROM_LOAD ( "a-7.bin",   0x10000, 0x10000, 0x6284c254 );/* 0x8000, 0x7091959c */
		ROM_LOAD ( "a-10.bin",  0x20000, 0x10000, 0xb728ec6e );/* 0x8000, 0x78349cb6 */
		ROM_LOAD ( "a-11.bin",  0x30000, 0x10000, 0xa50d1895 );/* 0x8000, 0xaa5b6834 */
	
		ROM_REGION(0x300, REGION_USER1, 0 );/* Unknown Bipolar PROMs */
		ROM_LOAD( "23jb-0.16", 0x000, 0x200, 0x46339529 );/* 82S131 on video board */
		ROM_LOAD( "23j5-0.45", 0x200, 0x100, 0xfdb130a9 );/* 82S129 on main board */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_saiyugb2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );/* Main CPU: 128KB for code (bankswitched using $3F01) */
		ROM_LOAD( "23j3-0.51",   0x10000, 0x18000, 0xaa8132a2 );/* Banks 0x4000 long @ 0x4000 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same.
		   ROM_LOAD( "sai5.bin", 0x10000, 0x10000, 0x39795aa5 );   Banks 0x4000 long @ 0x4000
		   ROM_LOAD( "sai9.bin", 0x20000, 0x08000, 0x051ebe92 );   Banks 0x4000 long @ 0x4000
		*/
		ROM_CONTINUE(            0x08000, 0x08000 );			/* Static code */
	
		ROM_REGION( 0x28000, REGION_CPU2, 0 );/* Slave CPU: 128KB for code (bankswitched using $2000) */
		ROM_LOAD( "23j4-0.48", 0x10000, 0x18000, 0x2914af38 );/* Banks 0x4000 long @ 0x4000 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same.
		   ROM_LOAD( "sai4.bin", 0x10000, 0x10000, 0x9effddc1 );   Banks 0x4000 long @ 0x4000
		   ROM_LOAD( "sai8.bin", 0x20000, 0x08000, 0xa436edb8 );   Banks 0x4000 long @ 0x4000
		*/
		ROM_CONTINUE(         0x08000, 0x08000 );			/* Static code */
	
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* Music CPU, 64KB */
		ROM_LOAD( "sai-alt1.bin", 0x00000, 0x8000, 0x8d397a8d );
	
	//	ROM_REGION( 0x800, REGION_CPU4, 0 );	/* ADPCM CPU, 1KB */
	//	ROM_LOAD( "sgr-8749.bin", 0x000, 0x800, 0x9237e8c5 );/* same as above but padded with 00 for different mcu */
	
		ROM_REGION(0x20000, REGION_GFX1, ROMREGION_DISPOSE );/* Text */
		ROM_LOAD( "23j6-0.18", 0x00000, 0x20000, 0x86d33df0 );/* 0,1,2,3 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same.
		   ROM_LOAD( "sai2.bin", 0x00000, 0x10000, 0xbaa5a3b9 );   0,1
		   ROM_LOAD( "sai3.bin", 0x10000, 0x10000, 0x532d59be );   2,3
		*/
	
		ROM_REGION(0x80000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "23j7-0.103",   0x00000, 0x20000, 0x2f445030 );/* 2,3 */
		ROM_LOAD( "23j8-0.102",   0x20000, 0x20000, 0x237f725a );/* 2,3 */
		ROM_LOAD( "23j9-0.101",   0x40000, 0x20000, 0x8caf6097 );/* 0,1 */
		ROM_LOAD( "23ja-0.100",   0x60000, 0x20000, 0xf678594f );/* 0,1 */
		/* Orientation of bootleg ROMs which are split, but otherwise the same
		   ROM_LOAD( "sai23.bin", 0x00000, 0x10000, 0x12b56225 );   2,3
		   ROM_LOAD( "sai22.bin", 0x10000, 0x10000, 0xb592aa9b );   2,3
		   ROM_LOAD( "sai21.bin", 0x20000, 0x10000, 0xa331ba3d );   2,3
		   ROM_LOAD( "sai20.bin", 0x30000, 0x10000, 0x2515d742 );   2,3
		   ROM_LOAD( "sai19.bin", 0x40000, 0x10000, 0xd796f2e4 );   0,1
		   ROM_LOAD( "sai18.bin", 0x50000, 0x10000, 0xc9e1c2f9 );   0,1
		   ROM_LOAD( "roku17.bin",0x60000, 0x10000, 0x00b6db0a );   0,1
		   ROM_LOAD( "sai16.bin", 0x70000, 0x10000, 0xf196818b );   0,1
		*/
	
		ROM_REGION(0x40000, REGION_GFX3, ROMREGION_DISPOSE );/* Background */
		ROM_LOAD( "a-13", 0x00000, 0x10000, 0xb745cac4 );
		ROM_LOAD( "a-12", 0x10000, 0x10000, 0x3c864299 );
		ROM_LOAD( "a-15", 0x20000, 0x10000, 0x2f268f37 );
		ROM_LOAD( "a-14", 0x30000, 0x10000, 0xaef814c8 );
	
		ROM_REGION(0x40000, REGION_SOUND1, 0 );/* ADPCM */
		/* These are same as the 128k sample except nibble-swapped */
		/* Some bootlegs have incorrectly halved the ADPCM data !  Bootleggers wrong data */
		ROM_LOAD ( "a-6.bin",   0x00000, 0x10000, 0x4da4e935 );/* 0x8000, 0x7cd47f01 */
		ROM_LOAD ( "a-7.bin",   0x10000, 0x10000, 0x6284c254 );/* 0x8000, 0x7091959c */
		ROM_LOAD ( "a-10.bin",  0x20000, 0x10000, 0xb728ec6e );/* 0x8000, 0x78349cb6 */
		ROM_LOAD ( "a-11.bin",  0x30000, 0x10000, 0xa50d1895 );/* 0x8000, 0xaa5b6834 */
	
		ROM_REGION(0x300, REGION_USER1, 0 );/* Unknown Bipolar PROMs */
		ROM_LOAD( "23jb-0.16", 0x000, 0x200, 0x46339529 );/* 82S131 on video board */
		ROM_LOAD( "23j5-0.45", 0x200, 0x100, 0xfdb130a9 );/* 82S129 on main board */
	ROM_END(); }}; 
	
	
	
	/*   ( YEAR  NAME      PARENT    MACHINE   INPUT     INIT    MONITOR COMPANY    FULLNAME     FLAGS ) */
	public static GameDriver driver_chinagat	   = new GameDriver("1988"	,"chinagat"	,"chinagat.java"	,rom_chinagat,null	,machine_driver_chinagat	,input_ports_chinagat	,null	,	ROT0, "[Technos] (Taito Romstar license)", "China Gate (US)" );
	public static GameDriver driver_saiyugou	   = new GameDriver("1988"	,"saiyugou"	,"chinagat.java"	,rom_saiyugou,driver_chinagat	,machine_driver_chinagat	,input_ports_chinagat	,null	,	ROT0, "Technos", "Sai Yu Gou Ma Roku (Japan)" );
	public static GameDriver driver_saiyugb1	   = new GameDriver("1988"	,"saiyugb1"	,"chinagat.java"	,rom_saiyugb1,driver_chinagat	,machine_driver_saiyugb1	,input_ports_chinagat	,null	,	ROT0, "bootleg", "Sai Yu Gou Ma Roku (bootleg 1)", GAME_IMPERFECT_SOUND );
	public static GameDriver driver_saiyugb2	   = new GameDriver("1988"	,"saiyugb2"	,"chinagat.java"	,rom_saiyugb2,driver_chinagat	,machine_driver_saiyugb2	,input_ports_chinagat	,null	,	ROT0, "bootleg", "Sai Yu Gou Ma Roku (bootleg 2)" );
}
