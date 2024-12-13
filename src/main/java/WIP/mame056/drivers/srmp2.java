/***************************************************************************

Super Real Mahjong P2
-------------------------------------
driver by Yochizo and Takahiro Nogi

  Yochizo took charge of video and I/O part.
  Takahiro Nogi took charge of sound, I/O and NVRAM part.

  ... and this is based on "seta.c" driver written by Luca Elia.

  Thanks for your reference, Takahiro Nogi and Luca Elia.


Supported games :
==================
 Super Real Mahjong Part2     (C) 1987 Seta
 Super Real Mahjong Part3     (C) 1988 Seta
 Mahjong Yuugi (set 1)        (C) 1990 Visco
 Mahjong Yuugi (set 2)        (C) 1990 Visco


Not supported game :
=====================
 Super Real Mahjong Part1 (not dumped)


System specs :
===============
   CPU       : 68000 (8MHz)
   Sound     : AY8910 + MSM5205
   Chips     : X1-001, X1-002A, X1-003, X1-004x2, X0-005 x2
           X1-001, X1-002A  : Sprites
           X1-003           : Video output
           X1-004           : ???
           X1-005           : ???


Known issues :
===============
 - I/O port isn't fully analized. Currently avoid I/O error message with hack.
 - AY-3-8910 sound may be wrong.
 - CPU clock of srmp3 does not match the real machine.
 - MSM5205 clock frequency in srmp3 is wrong.


Note:
======
 - In mjyuugi and mjyuugia, DSW3 (Debug switch) is available if you
   turn on the cheat switch.


****************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fileio.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
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
import static WIP.mame056.vidhrdw.srmp2.*;
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

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

import static mame056.vidhrdw.generic.*;

public class srmp2
{
	
	
	/***************************************************************************
	
	  Variables
	
	***************************************************************************/
	
	
	
	
	static int srmp2_adpcm_bank;
	static int srmp2_adpcm_data;
	static int srmp2_adpcm_sptr;
	static int srmp2_adpcm_eptr;
	
	static int srmp2_port_select;
	
	static UBytePtr	srmp2_nvram;
	static int[]			srmp2_nvram_size = new int[1];
	static UBytePtr	srmp3_nvram;
	static int[]			srmp3_nvram_size = new int[1];
	
	
	/***************************************************************************
	
	  Interrupt(s)
	
	***************************************************************************/
	
	public static InterruptPtr srmp2_interrupt = new InterruptPtr() { public int handler() 
	{
		switch (cpu_getiloops())
		{
			case 0:		return 4;	/* vblank */
			default:	return 2;	/* sound */
		}
	} };
	
	public static InterruptPtr srmp3_interrupt = new InterruptPtr() { public int handler() 
	{
		return interrupt.handler();
	} };
	
	
	public static InitDriverPtr init_srmp2 = new InitDriverPtr() { public void handler()
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		/* Fix "ERROR BACK UP" and "ERROR IOX" */
		RAM.write(0x20c80 / 2, 0x4e75);								// RTS
	} };
	
	public static InitDriverPtr init_srmp3 = new InitDriverPtr() { public void handler()
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		/* BANK ROM (0x08000 - 0x1ffff) Check skip [MAIN ROM side] */
		RAM.write(0x00000 + 0x7b69, 0x00);							// NOP
		RAM.write(0x00000 + 0x7b6a, 0x00);							// NOP
	
		/* MAIN ROM (0x00000 - 0x07fff) Check skip .write(BANK ROM side] */
		RAM.write(0x08000 + 0xc10b, 0x00);							// NOP
		RAM.write(0x08000 + 0xc10c, 0x00);							// NOP
		RAM.write(0x08000 + 0xc10d, 0x00);							// NOP
		RAM.write(0x08000 + 0xc10e, 0x00);							// NOP
		RAM.write(0x08000 + 0xc10f, 0x00);							// NOP
		RAM.write(0x08000 + 0xc110, 0x00);							// NOP
		RAM.write(0x08000 + 0xc111, 0x00);							// NOP
	
		/* "ERR IOX" Check skip .write(MAIN ROM side] */
		RAM.write(0x00000 + 0x784e, 0x00);							// NOP
		RAM.write(0x00000 + 0x784f, 0x00);							// NOP
		RAM.write(0x00000 + 0x7850, 0x00);							// NOP
	} };
	
	public static InitDriverPtr init_mjyuugi = new InitDriverPtr() { public void handler()
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		/* Sprite RAM check skip */
		RAM.write(0x0276e / 2, 0x4e75);								// RTS
	} };
	
	
	public static InitMachinePtr srmp2_init_machine = new InitMachinePtr() { public void handler()
	{
		srmp2_port_select = 0;
	} };
	
	public static InitMachinePtr srmp3_init_machine = new InitMachinePtr() { public void handler()
	{
		srmp2_port_select = 0;
	} };
	
	
	/***************************************************************************
	
	  Memory Handler(s)
	
	***************************************************************************/
	
	static nvramPtr srmp2_nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
                if (read_or_write != 0)
		{
			osd_fwrite(file, srmp2_nvram, srmp2_nvram_size[0]);
		}
		else
		{
			if (file != null)
			{
				osd_fread(file, srmp2_nvram, srmp2_nvram_size[0]);
			}
			else
			{
				memset(srmp2_nvram, 0, srmp2_nvram_size[0]);
			}
		}
            }
        };
	
	static nvramPtr srmp3_nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
		if (read_or_write != 0)
		{
			osd_fwrite(file, srmp3_nvram, srmp3_nvram_size[0]);
		}
		else
		{
			if (file != null)
			{
				osd_fread(file, srmp3_nvram, srmp3_nvram_size[0]);
			}
			else
			{
				//memset(srmp3_nvram, 0, srmp3_nvram_size[0]);
                            srmp3_nvram = new UBytePtr(srmp3_nvram_size[0]);
			}
		}
            }
        };
	
	
	static WriteHandlerPtr srmp2_flags_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*
		---- ---x : Coin Counter
		---x ---- : Coin Lock Out
		--x- ---- : ADPCM Bank
		x--- ---- : Palette Bank
	*/
	
		coin_counter_w.handler( 0, ((data & 0x01) >> 0) );
		coin_lockout_w( 0, (((~data) & 0x10) >> 4) );
		srmp2_adpcm_bank = ( (data & 0x20) >> 5 );
		srmp2_color_bank = ( (data & 0x80) >> 7 );
            }
        };
	
	
	static WriteHandlerPtr mjyuugi_flags_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
	/*
		---- ---x : Coin Counter
		---x ---- : Coin Lock Out
	*/
	
		coin_counter_w.handler( 0, ((data & 0x01) >> 0) );
		coin_lockout_w( 0, (((~data) & 0x10) >> 4) );
            }
        };
	
	static WriteHandlerPtr mjyuugi_adpcm_bank_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
	/*
		---- xxxx : ADPCM Bank
		--xx ---- : GFX Bank
	*/
		srmp2_adpcm_bank = (data & 0x0f);
		mjyuugi_gfx_bank = ((data >> 4) & 0x03);
            }
        };
	
	static WriteHandlerPtr srmp2_adpcm_code_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
	/*
		- Received data may be playing ADPCM number.
		- 0x000000 - 0x0000ff and 0x010000 - 0x0100ff are offset table.
		- When the hardware receives the ADPCM number, it refers the offset
		  table and plays the ADPCM for itself.
	*/
	
		UBytePtr ROM = new UBytePtr(memory_region(REGION_SOUND1));
	
		srmp2_adpcm_sptr = (ROM.read(((srmp2_adpcm_bank * 0x10000) + (data << 2) + 0)) << 8);
		srmp2_adpcm_eptr = (ROM.read(((srmp2_adpcm_bank * 0x10000) + (data << 2) + 1)) << 8);
		srmp2_adpcm_eptr  = (srmp2_adpcm_eptr - 1) & 0x0ffff;
	
		srmp2_adpcm_sptr += (srmp2_adpcm_bank * 0x10000);
		srmp2_adpcm_eptr += (srmp2_adpcm_bank * 0x10000);
	
		MSM5205_reset_w.handler(0, 0);
		srmp2_adpcm_data = -1;
            }
        };
	
	public static WriteHandlerPtr srmp3_adpcm_code_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	/*
		- Received data may be playing ADPCM number.
		- 0x000000 - 0x0000ff and 0x010000 - 0x0100ff are offset table.
		- When the hardware receives the ADPCM number, it refers the offset
		  table and plays the ADPCM for itself.
	*/
	
		UBytePtr ROM = new UBytePtr(memory_region(REGION_SOUND1));
	
		srmp2_adpcm_sptr = (ROM.read(((srmp2_adpcm_bank * 0x10000) + (data << 2) + 0)) << 8);
		srmp2_adpcm_eptr = (ROM.read(((srmp2_adpcm_bank * 0x10000) + (data << 2) + 1)) << 8);
		srmp2_adpcm_eptr  = (srmp2_adpcm_eptr - 1) & 0x0ffff;
	
		srmp2_adpcm_sptr += (srmp2_adpcm_bank * 0x10000);
		srmp2_adpcm_eptr += (srmp2_adpcm_bank * 0x10000);
	
		MSM5205_reset_w.handler(0, 0);
		srmp2_adpcm_data = -1;
	} };
	
	
	static vclk_interruptPtr srmp2_adpcm_int = new vclk_interruptPtr() {
            public void handler(int num) {
                UBytePtr ROM = new UBytePtr(memory_region(REGION_SOUND1));
	
		if (srmp2_adpcm_sptr != 0)
		{
			if (srmp2_adpcm_data == -1)
			{
				srmp2_adpcm_data = ROM.read(srmp2_adpcm_sptr);
	
				if (srmp2_adpcm_sptr >= srmp2_adpcm_eptr)
				{
					MSM5205_reset_w.handler(0, 1);
					srmp2_adpcm_data = 0;
					srmp2_adpcm_sptr = 0;
				}
				else
				{
					MSM5205_data_w.handler(0, ((srmp2_adpcm_data >> 4) & 0x0f));
				}
			}
			else
			{
				MSM5205_data_w.handler(0, ((srmp2_adpcm_data >> 0) & 0x0f));
				srmp2_adpcm_sptr++;
				srmp2_adpcm_data = -1;
			}
		}
		else
		{
			MSM5205_reset_w.handler(0, 1);
		}
            }
        };
	
	
	static ReadHandlerPtr srmp2_cchip_status_0_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		return 0x01;
            }
        };
	
	static ReadHandlerPtr srmp2_cchip_status_1_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		return 0x01;
            }
        };
	
	static ReadHandlerPtr srmp2_input_1_r = new ReadHandlerPtr() {
            public int handler(int offset) {
	/*
		---x xxxx : Key code
		--x- ---- : Player 1 and 2 side flag
	*/
	
/*TODO*///		if (ACCESSING_LSB == 0)
/*TODO*///		{
/*TODO*///			return 0xffff;
/*TODO*///		}
	
		if (srmp2_port_select != 2)			/* Panel keys */
		{
			int i, j, t;
	
			for (i = 0x00 ; i < 0x20 ; i += 8)
			{
				j = (i / 0x08) + 3;
	
				for (t = 0 ; t < 8 ; t ++)
				{
					if ((readinputport(j) & ( 1 << t ))==0)
					{
						return (i + t);
					}
				}
			}
		}
		else								/* Analizer and memory reset keys */
		{
			return readinputport(7);
		}
	
		return 0xffff;
            }
        };
	
	static ReadHandlerPtr srmp2_input_2_r = new ReadHandlerPtr() {
            public int handler(int offset) {
/*TODO*///                if (ACCESSING_LSB == 0)
/*TODO*///		{
/*TODO*///			return 0x0001;
/*TODO*///		}
	
		/* Always return 1, otherwise freeze. Maybe read I/O status */
		return 0x0001;
            }
        };
	
	
	
	static WriteHandlerPtr srmp2_input_1_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                if (data != 0x0000)
		{
			srmp2_port_select = 1;
		}
		else
		{
			srmp2_port_select = 0;
		}
            }
        };
	
	static WriteHandlerPtr srmp2_input_2_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		if (data == 0x0000)
		{
			srmp2_port_select = 2;
		}
		else
		{
			srmp2_port_select = 0;
		}
            }
        };
	
	public static WriteHandlerPtr srmp3_rombank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	/*
		---x xxxx : MAIN ROM bank
		xxx- ---- : ADPCM ROM bank
	*/
	
		UBytePtr ROM = new UBytePtr(memory_region(REGION_CPU1));
		int addr;
	
		srmp2_adpcm_bank = ((data & 0xe0) >> 5);
	
		if ((data & 0x1f)!=0) addr = ((0x10000 + (0x2000 * (data & 0x0f))) - 0x8000);
		else addr = 0x10000;
	
		cpu_setbank(1, new UBytePtr(ROM, addr));
	} };
	
	/**************************************************************************
	
	  Memory Map(s)
	
	**************************************************************************/
	
	
	public static Memory_ReadAddress srmp2_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x000000, 0x03ffff, MRA_ROM ),
		new Memory_ReadAddress( 0x0c0000, 0x0c3fff, MRA_RAM ),
		new Memory_ReadAddress( 0x140000, 0x143fff, MRA_RAM ),				/* Sprites Code + X + Attr */
		new Memory_ReadAddress( 0x180000, 0x180607, MRA_RAM ),				/* Sprites Y */
		new Memory_ReadAddress( 0x900000, 0x900001, input_port_0_word_r ),	/* Coinage */
		new Memory_ReadAddress( 0xa00000, 0xa00001, srmp2_input_1_r ),		/* I/O port 1 */
		new Memory_ReadAddress( 0xa00002, 0xa00003, srmp2_input_2_r ),		/* I/O port 2 */
		new Memory_ReadAddress( 0xb00000, 0xb00001, srmp2_cchip_status_0_r ),	/* Custom chip status ??? */
		new Memory_ReadAddress( 0xb00002, 0xb00003, srmp2_cchip_status_1_r ),	/* Custom chip status ??? */
		new Memory_ReadAddress( 0xf00000, 0xf00001, AY8910_read_port_0_lsb_r ),
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress srmp2_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x000000, 0x03ffff, MWA_ROM ),
		new Memory_WriteAddress( 0x0c0000, 0x0c3fff, MWA_RAM, srmp2_nvram, srmp2_nvram_size ),
		new Memory_WriteAddress( 0x140000, 0x143fff, MWA_RAM, spriteram16_2 ),	/* Sprites Code + X + Attr */
		new Memory_WriteAddress( 0x180000, 0x180609, MWA_RAM, spriteram16 ),	/* Sprites Y */
		new Memory_WriteAddress( 0x1c0000, 0x1c0001, MWA_NOP ),					/* ??? */
		new Memory_WriteAddress( 0x800000, 0x800001, srmp2_flags_w ),				/* ADPCM bank, Color bank, etc. */
		new Memory_WriteAddress( 0x900000, 0x900001, MWA_NOP ),					/* ??? */
		new Memory_WriteAddress( 0xa00000, 0xa00001, srmp2_input_1_w ),			/* I/O ??? */
		new Memory_WriteAddress( 0xa00002, 0xa00003, srmp2_input_2_w ),			/* I/O ??? */
		new Memory_WriteAddress( 0xb00000, 0xb00001, srmp2_adpcm_code_w ),			/* ADPCM number */
		new Memory_WriteAddress( 0xc00000, 0xc00001, MWA_NOP ),					/* ??? */
		new Memory_WriteAddress( 0xd00000, 0xd00001, MWA_NOP ),					/* ??? */
		new Memory_WriteAddress( 0xe00000, 0xe00001, MWA_NOP ),					/* ??? */
		new Memory_WriteAddress( 0xf00000, 0xf00001, AY8910_control_port_0_lsb_w ),
		new Memory_WriteAddress( 0xf00002, 0xf00003, AY8910_write_port_0_lsb_w ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_ReadAddress mjyuugi_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x000000, 0x07ffff, MRA_ROM ),
		new Memory_ReadAddress( 0x100000, 0x100001, input_port_0_word_r ),	/* Coinage */
		new Memory_ReadAddress( 0x100010, 0x100011, MRA_NOP ),				/* ??? */
		new Memory_ReadAddress( 0x200000, 0x200001, MRA_NOP ),				/* ??? */
		new Memory_ReadAddress( 0x300000, 0x300001, MRA_NOP ),				/* ??? */
		new Memory_ReadAddress( 0x500000, 0x500001, input_port_8_word_r ),	/* DSW 3-1 */
		new Memory_ReadAddress( 0x500010, 0x500011, input_port_9_word_r ),	/* DSW 3-2 */
		new Memory_ReadAddress( 0x700000, 0x7003ff, paletteram16_word_r ),
		new Memory_ReadAddress( 0x800000, 0x800001, MRA_NOP ),				/* ??? */
		new Memory_ReadAddress( 0x900000, 0x900001, srmp2_input_1_r ),		/* I/O port 1 */
		new Memory_ReadAddress( 0x900002, 0x900003, srmp2_input_2_r ),		/* I/O port 2 */
		new Memory_ReadAddress( 0xa00000, 0xa00001, srmp2_cchip_status_0_r ),	/* custom chip status ??? */
		new Memory_ReadAddress( 0xa00002, 0xa00003, srmp2_cchip_status_1_r ),	/* custom chip status ??? */
		new Memory_ReadAddress( 0xb00000, 0xb00001, AY8910_read_port_0_lsb_r ),
		new Memory_ReadAddress( 0xd00000, 0xd00609, MRA_RAM ),				/* Sprites Y */
		new Memory_ReadAddress( 0xd02000, 0xd023ff, MRA_RAM ),				/* ??? */
		new Memory_ReadAddress( 0xe00000, 0xe03fff, MRA_RAM ),				/* Sprites Code + X + Attr */
		new Memory_ReadAddress( 0xffc000, 0xffffff, MRA_RAM ),
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress mjyuugi_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x000000, 0x07ffff, MWA_ROM ),
		new Memory_WriteAddress( 0x100000, 0x100001, mjyuugi_flags_w ),			/* Coin Counter */
		new Memory_WriteAddress( 0x100010, 0x100011, mjyuugi_adpcm_bank_w ),		/* ADPCM bank, GFX bank */
		new Memory_WriteAddress( 0x700000, 0x7003ff, paletteram16_xRRRRRGGGGGBBBBB_word_w, paletteram16 ),
		new Memory_WriteAddress( 0x900000, 0x900001, srmp2_input_1_w ),			/* I/O ??? */
		new Memory_WriteAddress( 0x900002, 0x900003, srmp2_input_2_w ),			/* I/O ??? */
		new Memory_WriteAddress( 0xa00000, 0xa00001, srmp2_adpcm_code_w ),			/* ADPCM number */
		new Memory_WriteAddress( 0xb00000, 0xb00001, AY8910_control_port_0_lsb_w ),
		new Memory_WriteAddress( 0xb00002, 0xb00003, AY8910_write_port_0_lsb_w ),
		new Memory_WriteAddress( 0xc00000, 0xc00001, MWA_NOP ),					/* ??? */
		new Memory_WriteAddress( 0xd00000, 0xd00609, MWA_RAM, spriteram16 ),	/* Sprites Y */
		new Memory_WriteAddress( 0xd02000, 0xd023ff, MWA_RAM ),					/* ??? only writes $00fa */
		new Memory_WriteAddress( 0xe00000, 0xe03fff, MWA_RAM, spriteram16_2 ),	/* Sprites Code + X + Attr */
		new Memory_WriteAddress( 0xffc000, 0xffffff, MWA_RAM, srmp2_nvram, srmp2_nvram_size ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static ReadHandlerPtr srmp3_cchip_status_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return 0x01;
	} };
	
	public static ReadHandlerPtr srmp3_cchip_status_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return 0x01;
	} };
        
        static int qqq01 = 0;
        static int qqq02 = 0;
        static int qqq49 = 0;
        static int qqqzz = 0;
	
	public static WriteHandlerPtr srmp3_input_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	/*
		---- --x- : Player 1 side flag ?
		---- -x-- : Player 2 side flag ?
	*/
	
		logerror("PC:%04X DATA:%02X  srmp3_input_1_wn", cpu_get_pc(), data);
	
		srmp2_port_select = 0;
	
		{
			
	
			if (data == 0x01) qqq01++;
			else if (data == 0x02) qqq02++;
			else if (data == 0x49) qqq49++;
			else qqqzz++;
	
	//		usrintf_showmessage("%04X %04X %04X %04X", qqq01, qqq02, qqq49, qqqzz);
		}
	} };
	
	public static WriteHandlerPtr srmp3_input_2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	
		/* Key matrix reading related ? */
	
		logerror("PC:%04X DATA:%02X  srmp3_input_2_wn", cpu_get_pc(), data);
	
		srmp2_port_select = 1;
	
	} };
	
	public static ReadHandlerPtr srmp3_input_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	/*
		---x xxxx : Key code
		--x- ---- : Player 1 and 2 side flag
	*/
	
		/* Currently I/O port of srmp3 is fully understood. */
	
		int keydata = 0xff;
	
		logerror("PC:%04X          srmp3_input_rn", cpu_get_pc());
	
		// PC:0x8903	ROM:0xC903
		// PC:0x7805	ROM:0x7805
	
		if ((cpu_get_pc() == 0x8903) || (cpu_get_pc() == 0x7805))	/* Panel keys */
		{
			int i, j, t;
	
			for (i = 0x00 ; i < 0x20 ; i += 8)
			{
				j = (i / 0x08) + 3;
	
				for (t = 0 ; t < 8 ; t ++)
				{
					if ((readinputport(j) & ( 1 << t ))==0)
					{
						keydata = (i + t);
					}
				}
			}
		}
	
		// PC:0x8926	ROM:0xC926
		// PC:0x7822	ROM:0x7822
	
		if ((cpu_get_pc() == 0x8926) || (cpu_get_pc() == 0x7822))	/* Analizer and memory reset keys */
		{
			keydata = readinputport(7);
		}
	
		return keydata;
	} };
	
	public static WriteHandlerPtr srmp3_flags_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	/*
		---- ---x : Coin Counter
		---x ---- : Coin Lock Out
		xx-- ---- : GFX Bank
	*/
	
		coin_counter_w.handler( 0, ((data & 0x01) >> 0) );
		coin_lockout_w( 0, (((~data) & 0x10) >> 4) );
		srmp3_gfx_bank = (data >> 6) & 0x03;
	} };
	
	
	public static Memory_ReadAddress srmp3_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x9fff, MRA_BANK1 ),						/* rom bank */
		new Memory_ReadAddress( 0xa000, 0xa7ff, MRA_RAM ),						/* work ram */
		new Memory_ReadAddress( 0xb000, 0xb303, MRA_RAM ),						/* Sprites Y */
		new Memory_ReadAddress( 0xc000, 0xdfff, MRA_RAM ),						/* Sprites Code + X + Attr */
		new Memory_ReadAddress( 0xe000, 0xffff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress srmp3_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x9fff, MWA_ROM ),						/* rom bank */
		new Memory_WriteAddress( 0xa000, 0xa7ff, MWA_RAM, srmp3_nvram, srmp3_nvram_size ),	/* work ram */
		new Memory_WriteAddress( 0xa800, 0xa800, MWA_NOP ),						/* flag ? */
		new Memory_WriteAddress( 0xb000, 0xb303, MWA_RAM, spriteram ),			/* Sprites Y */
		new Memory_WriteAddress( 0xb800, 0xb800, MWA_NOP ),						/* flag ? */
		new Memory_WriteAddress( 0xc000, 0xdfff, MWA_RAM, spriteram_2 ),			/* Sprites Code + X + Attr */
		new Memory_WriteAddress( 0xe000, 0xffff, MWA_RAM, spriteram_3 ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort srmp3_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x40, 0x40, input_port_0_r ),						/* coin, service */
		new IO_ReadPort( 0xa1, 0xa1, srmp3_cchip_status_0_r ),				/* custom chip status ??? */
		new IO_ReadPort( 0xc0, 0xc0, srmp3_input_r ),						/* key matrix */
		new IO_ReadPort( 0xc1, 0xc1, srmp3_cchip_status_1_r ),				/* custom chip status ??? */
		new IO_ReadPort( 0xe2, 0xe2, AY8910_read_port_0_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort srmp3_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x20, 0x20, IOWP_NOP ),							/* elapsed interrupt signal */
		new IO_WritePort( 0x40, 0x40, srmp3_flags_w ),						/* GFX bank, counter, lockout */
		new IO_WritePort( 0x60, 0x60, srmp3_rombank_w ),					/* ROM bank select */
		new IO_WritePort( 0xa0, 0xa0, srmp3_adpcm_code_w ),					/* ADPCM number */
		new IO_WritePort( 0xc0, 0xc0, srmp3_input_1_w ),					/* I/O ??? */
		new IO_WritePort( 0xc1, 0xc1, srmp3_input_2_w ),					/* I/O ??? */
		new IO_WritePort( 0xe0, 0xe0, AY8910_control_port_0_w ),
		new IO_WritePort( 0xe1, 0xe1, AY8910_write_port_0_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	/***************************************************************************
	
	  Input Port(s)
	
	***************************************************************************/
	
	public static void SETAMJCTRL_PORT3() {
		PORT_START(); 	/* KEY MATRIX INPUT (3) */ 
		PORT_BIT ( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BITX( 0x0002, IP_ACTIVE_LOW, 0, "P1 Small",       KEYCODE_BACKSPACE, IP_JOY_NONE );
		PORT_BITX( 0x0004, IP_ACTIVE_LOW, 0, "P1 Double Up",   KEYCODE_RSHIFT,    IP_JOY_NONE );
		PORT_BITX( 0x0008, IP_ACTIVE_LOW, 0, "P1 Big",         KEYCODE_ENTER,     IP_JOY_NONE );
		PORT_BITX( 0x0010, IP_ACTIVE_LOW, 0, "P1 Take Score",  KEYCODE_RCONTROL,  IP_JOY_NONE );
		PORT_BITX( 0x0020, IP_ACTIVE_LOW, 0, "P1 Flip",        KEYCODE_X,         IP_JOY_NONE );
		PORT_BITX( 0x0040, IP_ACTIVE_LOW, 0, "P1 Last Chance", KEYCODE_RALT,      IP_JOY_NONE );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void SETAMJCTRL_PORT4() {
		PORT_START(); 	/* KEY MATRIX INPUT (4) */ 
		PORT_BITX( 0x0001, IP_ACTIVE_LOW, 0, "P1 K",   KEYCODE_K,     IP_JOY_NONE );
		PORT_BITX( 0x0002, IP_ACTIVE_LOW, 0, "P1 Ron", KEYCODE_Z,     IP_JOY_NONE );
		PORT_BITX( 0x0004, IP_ACTIVE_LOW, 0, "P1 G",   KEYCODE_G,     IP_JOY_NONE );
		PORT_BITX( 0x0008, IP_ACTIVE_LOW, 0, "P1 Chi", KEYCODE_SPACE, IP_JOY_NONE );
		PORT_BITX( 0x0010, IP_ACTIVE_LOW, 0, "P1 C",   KEYCODE_C,     IP_JOY_NONE );
		PORT_BIT ( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BITX( 0x0040, IP_ACTIVE_LOW, 0, "P1 L",   KEYCODE_L,     IP_JOY_NONE );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void SETAMJCTRL_PORT5() {
		PORT_START(); 	/* KEY MATRIX INPUT (5) */ 
		PORT_BITX( 0x0001, IP_ACTIVE_LOW, 0, "P1 H",     KEYCODE_H,        IP_JOY_NONE );
		PORT_BITX( 0x0002, IP_ACTIVE_LOW, 0, "P1 Pon",   KEYCODE_LALT,     IP_JOY_NONE );
		PORT_BITX( 0x0004, IP_ACTIVE_LOW, 0, "P1 D",     KEYCODE_D,        IP_JOY_NONE );
		PORT_BITX( 0x0008, IP_ACTIVE_LOW, 0, "P1 Start", KEYCODE_1,        IP_JOY_NONE );
		PORT_BITX( 0x0010, IP_ACTIVE_LOW, 0, "P1 I",     KEYCODE_I,        IP_JOY_NONE );
		PORT_BITX( 0x0020, IP_ACTIVE_LOW, 0, "P1 Kan",   KEYCODE_LCONTROL, IP_JOY_NONE );
		PORT_BITX( 0x0040, IP_ACTIVE_LOW, 0, "P1 E",     KEYCODE_E,        IP_JOY_NONE );
		PORT_BITX( 0x0080, IP_ACTIVE_LOW, 0, "P1 M",     KEYCODE_M,        IP_JOY_NONE );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	public static void SETAMJCTRL_PORT6() {
		PORT_START(); 	/* KEY MATRIX INPUT (6) */ 
		PORT_BITX( 0x0001, IP_ACTIVE_LOW, 0, "P1 A",     KEYCODE_A,      IP_JOY_NONE );
		PORT_BITX( 0x0002, IP_ACTIVE_LOW, 0, "P1 Bet",   KEYCODE_2,      IP_JOY_NONE );
		PORT_BITX( 0x0004, IP_ACTIVE_LOW, 0, "P1 J",     KEYCODE_J,      IP_JOY_NONE );
		PORT_BITX( 0x0008, IP_ACTIVE_LOW, 0, "P1 Reach", KEYCODE_LSHIFT, IP_JOY_NONE );
		PORT_BITX( 0x0010, IP_ACTIVE_LOW, 0, "P1 F",     KEYCODE_F,      IP_JOY_NONE );
		PORT_BITX( 0x0020, IP_ACTIVE_LOW, 0, "P1 N",     KEYCODE_N,      IP_JOY_NONE );
		PORT_BITX( 0x0040, IP_ACTIVE_LOW, 0, "P1 B",     KEYCODE_B,      IP_JOY_NONE );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }
	
	static InputPortPtr input_ports_srmp2 = new InputPortPtr(){ public void handler() { 
		PORT_START(); 			/* Coinnage (0) */
		PORT_BIT ( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0002, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0004, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0010, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT ( 0x0020, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT ( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 			/* DSW (1) */
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
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0xe0, 0xe0, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0xe0, "1 (Easy)" );
		PORT_DIPSETTING(    0xc0, "2" );
		PORT_DIPSETTING(    0xa0, "3" );
		PORT_DIPSETTING(    0x80, "4" );
		PORT_DIPSETTING(    0x60, "5" );
		PORT_DIPSETTING(    0x40, "6" );
		PORT_DIPSETTING(    0x20, "7" );
		PORT_DIPSETTING(    0x00, "8 (Hard)" );
	
		PORT_START(); 			/* DSW (2) */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Free_Play") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x02, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_3C") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		SETAMJCTRL_PORT3();	/* INPUT1 (3) */
		SETAMJCTRL_PORT4();	/* INPUT1 (4) */
		SETAMJCTRL_PORT5();	/* INPUT1 (5) */
		SETAMJCTRL_PORT6();	/* INPUT1 (6) */
	
		PORT_START(); 			/* INPUT1 (7) */
		PORT_BIT ( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0002, IP_ACTIVE_LOW, IPT_SERVICE3 );
		PORT_BIT ( 0x0004, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0008, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT ( 0x0010, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_srmp3 = new InputPortPtr(){ public void handler() { 
		PORT_START(); 			/* Coinnage (0) */
		PORT_BIT ( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT ( 0x02, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT ( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 			/* DSW (1) */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BITX   ( 0x04, 0x04, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug Mode", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, "Open Reach of CPU" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0xe0, 0xe0, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0xe0, "1 (Easy)" );
		PORT_DIPSETTING(    0xc0, "2" );
		PORT_DIPSETTING(    0xa0, "3" );
		PORT_DIPSETTING(    0x80, "4" );
		PORT_DIPSETTING(    0x60, "5" );
		PORT_DIPSETTING(    0x40, "6" );
		PORT_DIPSETTING(    0x20, "7" );
		PORT_DIPSETTING(    0x00, "8 (Hard)" );
	
		PORT_START(); 			/* DSW (2) */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Free_Play") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x02, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_3C") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		SETAMJCTRL_PORT3();	/* INPUT1 (3) */
		SETAMJCTRL_PORT4();	/* INPUT1 (4) */
		SETAMJCTRL_PORT5();	/* INPUT1 (5) */
		SETAMJCTRL_PORT6();	/* INPUT1 (6) */
	
		PORT_START(); 			/* INPUT1 (7) */
		PORT_BIT ( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x02, IP_ACTIVE_LOW, IPT_SERVICE3 );
		PORT_BIT ( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x08, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT ( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_mjyuugi = new InputPortPtr(){ public void handler() { 
		PORT_START(); 			/* Coinnage (0) */
		PORT_BIT ( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0002, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0004, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0010, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT ( 0x0020, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT ( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 			/* DSW (1) */
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x07, "1 (Easy)" );
		PORT_DIPSETTING(    0x06, "2" );
		PORT_DIPSETTING(    0x05, "3" );
		PORT_DIPSETTING(    0x04, "4" );
		PORT_DIPSETTING(    0x03, "5" );
		PORT_DIPSETTING(    0x02, "6" );
		PORT_DIPSETTING(    0x01, "7" );
		PORT_DIPSETTING(    0x00, "8 (Hard)" );
		PORT_DIPNAME( 0x08, 0x08, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x10, 0x10, "Gal Score" );
		PORT_DIPSETTING(    0x10, "+0" );
		PORT_DIPSETTING(    0x00, "+1000" );
		PORT_DIPNAME( 0x20, 0x20, "Player Score" );
		PORT_DIPSETTING(    0x20, "+0" );
		PORT_DIPSETTING(    0x00, "+1000" );
		PORT_DIPNAME( 0x40, 0x40, "Item price initialize ?" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 			/* DSW (2) */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_3C") );
		PORT_SERVICE( 0x40, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Free_Play") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		SETAMJCTRL_PORT3();	/* INPUT1 (3) */
		SETAMJCTRL_PORT4();	/* INPUT1 (4) */
		SETAMJCTRL_PORT5();	/* INPUT1 (5) */
		SETAMJCTRL_PORT6();	/* INPUT1 (6) */
	
		PORT_START(); 			/* INPUT1 (7) */
		PORT_BIT ( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0002, IP_ACTIVE_LOW, IPT_SERVICE3 );
		PORT_BITX( 0x0004, 0x0004, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug Mode", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0004, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BIT ( 0x0008, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT ( 0x0010, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 			/* DSW (3-1) [Debug switch] */
		PORT_BITX( 0x0001, 0x0001, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  0", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0001, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0002, 0x0002, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  1", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0002, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0004, 0x0004, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  2", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0004, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0008, 0x0008, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  3", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0008, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0010, 0x0010, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  4", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0010, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0020, 0x0020, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  5", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0020, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0040, 0x0040, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  6", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0040, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0080, 0x0080, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  7", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0080, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 			/* DSW (3-2) [Debug switch] */
		PORT_BITX( 0x0001, 0x0001, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  8", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0001, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0002, 0x0002, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug  9", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0002, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0004, 0x0004, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug 10", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0004, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0008, 0x0008, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug 11", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0008, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0010, 0x0010, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug 12", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0010, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0020, 0x0020, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug 13", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0020, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0040, 0x0040, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug 14", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0040, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BITX( 0x0080, 0x0080, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Debug 15", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(   0x0080, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x0000, DEF_STR( "On") );
		PORT_BIT ( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	
	/***************************************************************************
	
	  Machine Driver(s)
	
	***************************************************************************/
	
	static AY8910interface srmp2_ay8910_interface = new AY8910interface
	(
		1,
		20000000/16,					/* 1.25 MHz */
		new int[] { 40 },
		new ReadHandlerPtr[] { input_port_2_r },				/* Input A: DSW 2 */
		new ReadHandlerPtr[] { input_port_1_r },				/* Input B: DSW 1 */
		new WriteHandlerPtr[] { null },
		new WriteHandlerPtr[] { null }
	);
	
	static AY8910interface srmp3_ay8910_interface = new AY8910interface
	(
		1,
		16000000/16,					/* 1.00 MHz */
		new int[] { 20 },
		new ReadHandlerPtr[] { input_port_2_r },				/* Input A: DSW 2 */
		new ReadHandlerPtr[] { input_port_1_r },				/* Input B: DSW 1 */
		new WriteHandlerPtr[] { null },
		new WriteHandlerPtr[] { null }
	);
	
	static AY8910interface mjyuugi_ay8910_interface = new AY8910interface
	(
		1,
		16000000/16,					/* 1.00 MHz */
		new int[] { 20 },
		new ReadHandlerPtr[] { input_port_2_r },				/* Input A: DSW 2 */
		new ReadHandlerPtr[] { input_port_1_r },				/* Input B: DSW 1 */
		new WriteHandlerPtr[] { null },
		new WriteHandlerPtr[] { null }
	);
	
	
	static MSM5205interface srmp2_msm5205_interface = new MSM5205interface
	(
		1,
		384000,
		new vclk_interruptPtr[]{ srmp2_adpcm_int },			/* IRQ handler */
		new int[]{ MSM5205_S48_4B },				/* 8 KHz, 4 Bits  */
		new int[]{ 45 }
	);
	
	static MSM5205interface srmp3_msm5205_interface = new MSM5205interface
	(
		1,
	//	455000,							/* 455 KHz */
		384000,							/* 384 KHz */
		new vclk_interruptPtr[]{ srmp2_adpcm_int },			/* IRQ handler */
		new int[]{ MSM5205_S48_4B },				/* 8 KHz, 4 Bits  */
		new int[]{ 45 }
	);
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		16, 16,
		RGN_FRAC(1, 2),
		4,
		new int[] { RGN_FRAC(1, 2) + 8, RGN_FRAC(1, 2) + 0, 8, 0 },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 128, 129, 130, 131, 132, 133, 134, 135 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
		  16*16, 17*16, 18*16, 19*16, 20*16, 21*16, 22*16, 23*16 },
		16*16*2
	);
	
	static GfxDecodeInfo srmp2_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 0, 64 ),
		new GfxDecodeInfo( -1 )
	};
	
	static GfxDecodeInfo srmp3_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 0, 32 ),
		new GfxDecodeInfo( -1 )
	};
	
	
	static MachineDriver machine_driver_srmp2 = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				16000000/2,				/* 8.00 MHz */
				srmp2_readmem, srmp2_writemem, 0, 0,
				srmp2_interrupt, 16		/* Interrupt times is not understood */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		srmp2_init_machine,
	
		/* video hardware */
		464, 256-16, new rectangle( 16, 464-1, 8, 256-1-24 ),
		srmp2_gfxdecodeinfo,
		1024, 1024,						/* sprites only */
		srmp2_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		null,								/* no need for a vh_start: no tilemaps */
		null,
		srmp2_vh_screenrefresh,			/* just draw the sprites */
	
		/* sound hardware */
		0, 0, 0, 0,
		new MachineSound[] {
			new MachineSound(
                            SOUND_AY8910,
                            srmp2_ay8910_interface ),
			new MachineSound( 
                                SOUND_MSM5205, 
                                srmp2_msm5205_interface )
		},
		srmp2_nvram_handler
	);
	
	
	static MachineDriver machine_driver_srmp3 = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				3500000,				/* 3.50 MHz ? */
		//		4000000,				/* 4.00 MHz ? */
				srmp3_readmem, srmp3_writemem, srmp3_readport, srmp3_writeport,
		//		interrupt, 1
				srmp3_interrupt, 1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		srmp3_init_machine,
	
		/* video hardware */
		400, 256-16, new rectangle( 16, 400-1, 8, 256-1-24 ),
		srmp3_gfxdecodeinfo,
		512, 0,	/* sprites only */
		srmp3_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		null,								/* no need for a vh_start: no tilemaps */
		null,
		srmp3_vh_screenrefresh,			/* just draw the sprites */
	
		/* sound hardware */
		0, 0, 0, 0,
		new MachineSound[] {
			new MachineSound(
                                SOUND_AY8910, 
                                srmp3_ay8910_interface ),
			new MachineSound( 
                                SOUND_MSM5205, 
                                srmp3_msm5205_interface )
		},
		srmp3_nvram_handler
	);
	
	
	static MachineDriver machine_driver_mjyuugi = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				16000000/2,				/* 8.00 MHz */
				mjyuugi_readmem, mjyuugi_writemem, 0, 0,
				srmp2_interrupt, 16		/* Interrupt times is not understood */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		srmp2_init_machine,
	
		/* video hardware */
		400, 256-16, new rectangle( 16, 400-1, 0, 256-1-16 ),
		srmp3_gfxdecodeinfo,
		512, 0,						/* sprites only */
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		null,								/* no need for a vh_start: no tilemaps */
		null,
		mjyuugi_vh_screenrefresh,		/* just draw the sprites */
	
		/* sound hardware */
		0, 0, 0, 0,
		new MachineSound[] {
			new MachineSound(
                                SOUND_AY8910, 
                                mjyuugi_ay8910_interface ),
			new MachineSound(
                                SOUND_MSM5205, 
                                srmp2_msm5205_interface )
		},
		srmp2_nvram_handler
	);
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_srmp2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x040000, REGION_CPU1, 0 );				/* 68000 Code */
		ROM_LOAD16_BYTE( "uco-2.17", 0x000000, 0x020000, 0x0d6c131f );
		ROM_LOAD16_BYTE( "uco-3.18", 0x000001, 0x020000, 0xe9fdf5f8 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD       ( "ubo-4.60",  0x000000, 0x040000, 0xcb6f7cce );
		ROM_LOAD       ( "ubo-5.61",  0x040000, 0x040000, 0x7b48c540 );
		ROM_LOAD16_BYTE( "uco-8.64",  0x080000, 0x040000, 0x1ca1c7c9 );
		ROM_LOAD16_BYTE( "uco-9.65",  0x080001, 0x040000, 0xef75471b );
		ROM_LOAD       ( "ubo-6.62",  0x100000, 0x040000, 0x6c891ac5 );
		ROM_LOAD       ( "ubo-7.63",  0x140000, 0x040000, 0x60a45755 );
		ROM_LOAD16_BYTE( "uco-10.66", 0x180000, 0x040000, 0xcb6bd857 );
		ROM_LOAD16_BYTE( "uco-11.67", 0x180001, 0x040000, 0x199f79c0 );
	
		ROM_REGION( 0x020000, REGION_SOUND1, 0 );			/* Samples */
		ROM_LOAD( "uco-1.19", 0x000000, 0x020000, 0xf284af8e );
	
		ROM_REGION( 0x000800, REGION_PROMS, 0 );				/* Color PROMs */
		ROM_LOAD( "uc-1o.12", 0x000000, 0x000400, 0xfa59b5cb );
		ROM_LOAD( "uc-2o.13", 0x000400, 0x000400, 0x50a33b96 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_srmp3 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x028000, REGION_CPU1, 0 );				/* 68000 Code */
		ROM_LOAD( "za0-10.bin", 0x000000, 0x008000, 0x939d126f );
		ROM_CONTINUE(           0x010000, 0x018000 );
	
		ROM_REGION( 0x400000, REGION_GFX1, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD16_BYTE( "za0-02.bin", 0x000000, 0x080000, 0x85691946 );
		ROM_LOAD16_BYTE( "za0-04.bin", 0x000001, 0x080000, 0xc06e7a96 );
		ROM_LOAD16_BYTE( "za0-01.bin", 0x100000, 0x080000, 0x95e0d87c );
		ROM_LOAD16_BYTE( "za0-03.bin", 0x100001, 0x080000, 0x7c98570e );
		ROM_LOAD16_BYTE( "za0-06.bin", 0x200000, 0x080000, 0x8b874b0a );
		ROM_LOAD16_BYTE( "za0-08.bin", 0x200001, 0x080000, 0x3de89d88 );
		ROM_LOAD16_BYTE( "za0-05.bin", 0x300000, 0x080000, 0x80d3b4e6 );
		ROM_LOAD16_BYTE( "za0-07.bin", 0x300001, 0x080000, 0x39d15129 );
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );			/* Samples */
		ROM_LOAD( "za0-11.bin", 0x000000, 0x080000, 0x2248c23f );
	
		ROM_REGION( 0x000400, REGION_PROMS, 0 );				/* Color PROMs */
		ROM_LOAD( "za0-12.prm", 0x000000, 0x000200, 0x1ac5387c );
		ROM_LOAD( "za0-13.prm", 0x000200, 0x000200, 0x4ea3d2fe );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mjyuugi = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x080000, REGION_CPU1, 0 );				/* 68000 Code */
		ROM_LOAD16_BYTE( "um001.001", 0x000000, 0x020000, 0x28d5340f );
		ROM_LOAD16_BYTE( "um001.003", 0x000001, 0x020000, 0x275197de );
		ROM_LOAD16_BYTE( "um001.002", 0x040000, 0x020000, 0xd5dd4710 );
		ROM_LOAD16_BYTE( "um001.004", 0x040001, 0x020000, 0xc5ddb567 );
	
		ROM_REGION( 0x400000, REGION_GFX1, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD16_BYTE( "maj-001.10",  0x000000, 0x080000, 0x3c08942a );
		ROM_LOAD16_BYTE( "maj-001.08",  0x000001, 0x080000, 0xe2444311 );
		ROM_LOAD16_BYTE( "maj-001.09",  0x100000, 0x080000, 0xa1974860 );
		ROM_LOAD16_BYTE( "maj-001.07",  0x100001, 0x080000, 0xb1f1d118 );
		ROM_LOAD16_BYTE( "maj-001.06",  0x200000, 0x080000, 0x4c60acdd );
		ROM_LOAD16_BYTE( "maj-001.04",  0x200001, 0x080000, 0x0a4b2de1 );
		ROM_LOAD16_BYTE( "maj-001.05",  0x300000, 0x080000, 0x6be7047a );
		ROM_LOAD16_BYTE( "maj-001.03",  0x300001, 0x080000, 0xc4fb6ea0 );
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );			/* Samples */
		ROM_LOAD( "maj-001.01", 0x000000, 0x080000, 0x029a0b60 );
		ROM_LOAD( "maj-001.02", 0x080000, 0x080000, 0xeb28e641 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mjyuugia = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x080000, REGION_CPU1, 0 );				/* 68000 Code */
		ROM_LOAD16_BYTE( "um_001.001", 0x000000, 0x020000, 0x76dc0594 );
		ROM_LOAD16_BYTE( "um001.003",  0x000001, 0x020000, 0x275197de );
		ROM_LOAD16_BYTE( "um001.002",  0x040000, 0x020000, 0xd5dd4710 );
		ROM_LOAD16_BYTE( "um001.004",  0x040001, 0x020000, 0xc5ddb567 );
	
		ROM_REGION( 0x400000, REGION_GFX1, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD16_BYTE( "maj-001.10", 0x000000, 0x080000, 0x3c08942a );
		ROM_LOAD16_BYTE( "maj-001.08", 0x000001, 0x080000, 0xe2444311 );
		ROM_LOAD16_BYTE( "maj-001.09", 0x100000, 0x080000, 0xa1974860 );
		ROM_LOAD16_BYTE( "maj-001.07", 0x100001, 0x080000, 0xb1f1d118 );
		ROM_LOAD16_BYTE( "maj-001.06", 0x200000, 0x080000, 0x4c60acdd );
		ROM_LOAD16_BYTE( "maj-001.04", 0x200001, 0x080000, 0x0a4b2de1 );
		ROM_LOAD16_BYTE( "maj-001.05", 0x300000, 0x080000, 0x6be7047a );
		ROM_LOAD16_BYTE( "maj-001.03", 0x300001, 0x080000, 0xc4fb6ea0 );
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );			/* Samples */
		ROM_LOAD( "maj-001.01", 0x000000, 0x080000, 0x029a0b60 );
		ROM_LOAD( "maj-001.02", 0x080000, 0x080000, 0xeb28e641 );
	ROM_END(); }}; 
	
	
	public static GameDriver driver_srmp2	   = new GameDriver("1987"	,"srmp2"	,"srmp2.java"	,rom_srmp2,null	,machine_driver_srmp2	,input_ports_srmp2	,init_srmp2	,ROT0	,	"Seta", "Super Real Mahjong Part 2 (Japan)" );
	public static GameDriver driver_srmp3	   = new GameDriver("1988"	,"srmp3"	,"srmp2.java"	,rom_srmp3,null	,machine_driver_srmp3	,input_ports_srmp3	,init_srmp3	,ROT0	,	"Seta", "Super Real Mahjong Part 3 (Japan)" );
	public static GameDriver driver_mjyuugi	   = new GameDriver("1990"	,"mjyuugi"	,"srmp2.java"	,rom_mjyuugi,null	,machine_driver_mjyuugi	,input_ports_mjyuugi	,init_mjyuugi	,ROT0	,	"Visco", "Mahjong Yuugi (Japan set 1)" );
	public static GameDriver driver_mjyuugia	   = new GameDriver("1990"	,"mjyuugia"	,"srmp2.java"	,rom_mjyuugia,driver_mjyuugi	,machine_driver_mjyuugi	,input_ports_mjyuugi	,init_mjyuugi	,ROT0	,	"Visco", "Mahjong Yuugi (Japan set 2)" );
}
