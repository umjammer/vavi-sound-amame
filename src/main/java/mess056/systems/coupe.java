/***************************************************************************

 SAM Coupe Driver - Written By Lee Hammerton


	Sam Coupe Memory Map - Based around the current spectrum.c (for obvious reasons!!)

	CPU:
		0000-7fff Banked rom/ram
		8000-ffff Banked rom/ram


Interrupts:

Changes:

 V0.2	- Added FDC support. - Based on 1771 document. Coupe had a 1772... (any difference?)
		  	floppy supports only read sector single mode at present will add write sector
			in next version.
		  Fixed up palette - had red & green wrong way round.


 KT 26-Aug-2000 - Changed to use wd179x code. This is the same as the 1772.
                - Coupe supports the basic disk image format, but can be changed in
                  the future to support others
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.systems;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound.saa1099.*;
import static mame056.sound.saa1099H.*;

import static mess056.includes.coupeH.*;
import static mess056.machine.coupe.*;
import static mess056.vidhrdw.coupe.*;

import static mess056.sound.speaker.*;
import static mess056.sound.speakerH.*;
import static mess056.device.*;
import static mess056.deviceH.*;
import static mess056.machine.basicdsk.*;
import static mess056.machine.flopdrv.*;
import static mess056.machine.wd179x.*;
import static mess056.messH.*;

/**
 *
 * @author chusogar
 */
public class coupe {
	
	static Memory_ReadAddress coupe_readmem[]
            = {
                new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
                new Memory_ReadAddress( 0x0000, 0x3FFF, MRA_BANK1 ),
		new Memory_ReadAddress( 0x4000, 0x7FFF, MRA_BANK2 ),
		new Memory_ReadAddress( 0x8000, 0xBFFF, MRA_BANK3 ),
		new Memory_ReadAddress( 0xC000, 0xFFFF, MRA_BANK4 ),
                new Memory_ReadAddress(MEMPORT_MARKER, 0) /* end of table */};
	
	static Memory_WriteAddress coupe_writemem[]
            = {
                new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
                new Memory_WriteAddress( 0x0000, 0x3FFF, MWA_BANK1 ),
		new Memory_WriteAddress( 0x4000, 0x7FFF, MWA_BANK2 ),
		new Memory_WriteAddress( 0x8000, 0xBFFF, MWA_BANK3 ),
		new Memory_WriteAddress( 0xC000, 0xFFFF, MWA_BANK4 ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0) /* end of table */};
	
	public static InterruptPtr coupe_line_interrupt = new InterruptPtr() {
            public int handler() {
                mame_bitmap bitmap = Machine.scrbitmap;
		int interrupted=0;	/* This is used to allow me to clear the STAT flag (easiest way I can do it!) */
	
		HPEN = CURLINE;
	
		if (LINE_INT<192)
		{
			if (CURLINE == LINE_INT)
			{
				/* No other interrupts can occur - NOT CORRECT!!! */
                                STAT=0x1E;
				cpu_cause_interrupt(0, Z80_IRQ_INT);
				interrupted=1;
			}
		}
	
		/* scan line on screen so draw last scan line (may need to alter this slightly!!) */
                if (CURLINE!=0 && (CURLINE-1) < 192)
		{
			switch ((VMPR & 0x60)>>5)
			{
			case 0: /* mode 1 */
				drawMode1_line(bitmap,(CURLINE-1));
				break;
			case 1: /* mode 2 */
				drawMode2_line(bitmap,(CURLINE-1));
				break;
			case 2: /* mode 3 */
				drawMode3_line(bitmap,(CURLINE-1));
				break;
			case 3: /* mode 4 */
				drawMode4_line(bitmap,(CURLINE-1));
				break;
			}
		}
	
		CURLINE = (CURLINE + 1) % (192+10);
	
		if (CURLINE == 193)
		{
			if (interrupted != 0)
				STAT&=~0x08;
			else
				STAT=0x17;
	
			cpu_cause_interrupt(0, Z80_IRQ_INT);
			interrupted=1;
		}
	
		if (interrupted == 0)
			STAT=0x1F;
	
		return ignore_interrupt.handler();
            }
        };
		
	public static int getSamKey1(int hi)
	{
		int result;
	
		hi=~hi;
		result=0xFF;
	
		if (hi==0x00)
			result &=readinputport(8) & 0x1F;
		else
		{
			if ((hi&0x80) != 0) result &= readinputport(7) & 0x1F;
			if ((hi&0x40) != 0) result &= readinputport(6) & 0x1F;
			if ((hi&0x20) != 0) result &= readinputport(5) & 0x1F;
			if ((hi&0x10) != 0) result &= readinputport(4) & 0x1F;
			if ((hi&0x08) != 0) result &= readinputport(3) & 0x1F;
			if ((hi&0x04) != 0) result &= readinputport(2) & 0x1F;
			if ((hi&0x02) != 0) result &= readinputport(1) & 0x1F;
			if ((hi&0x01) != 0) result &= readinputport(0) & 0x1F;
		}
	
		return result;
	}
	
	public static int getSamKey2(int hi)
	{
		int result;
	
		hi=~hi;
		result=0xFF;
	
		if (hi==0x00)
		{
			/* does not map to any keys? */
		}
		else
		{
			if ((hi&0x80) != 0) result &= readinputport(7) & 0xE0;
			if ((hi&0x40) != 0) result &= readinputport(6) & 0xE0;
			if ((hi&0x20) != 0) result &= readinputport(5) & 0xE0;
			if ((hi&0x10) != 0) result &= readinputport(4) & 0xE0;
			if ((hi&0x08) != 0) result &= readinputport(3) & 0xE0;
			if ((hi&0x04) != 0) result &= readinputport(2) & 0xE0;
			if ((hi&0x02) != 0) result &= readinputport(1) & 0xE0;
			if ((hi&0x01) != 0) result &= readinputport(0) & 0xE0;
		}
	
		return result;
	}
	
	
	public static ReadHandlerPtr coupe_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    if (offset==SSND_ADDR)  /* Sound address request */
			return SOUND_ADDR;
	
		if (offset==HPEN_PORT)
			return HPEN;
	
		switch (offset & 0xFF)
		{
		case DSK1_PORT+0:	/* This covers the total range of ports for 1 floppy controller */
	    case DSK1_PORT+4:
			wd179x_set_side((offset >> 2) & 1);
			return wd179x_status_r.handler(0);
                        
		case DSK1_PORT+1:
	    case DSK1_PORT+5:
			wd179x_set_side((offset >> 2) & 1);
                        return wd179x_track_r.handler(0);
                        
		case DSK1_PORT+2:
	    case DSK1_PORT+6:
			wd179x_set_side((offset >> 2) & 1);
                        return wd179x_sector_r.handler(0);
                        
		case DSK1_PORT+3:
		case DSK1_PORT+7:
			wd179x_set_side((offset >> 2) & 1);
                        return wd179x_data_r.handler(0);
                        
		case LPEN_PORT:
			return LPEN;
		case STAT_PORT:
			return ((getSamKey2((offset >> 8)&0xFF))&0xE0) | STAT;
		case LMPR_PORT:
			return LMPR;
		case HMPR_PORT:
			return HMPR;
		case VMPR_PORT:
			return VMPR;
		case KEYB_PORT:
			return (getSamKey1((offset >> 8)&0xFF)&0x1F) | 0xE0;
		case SSND_DATA:
			return SOUND_REG[SOUND_ADDR];
		default:
			logerror("Read Unsupported Port: %04x\n", offset);
			break;
		}
	
		return 0x0ff;
	} };
	
	
	public static WriteHandlerPtr coupe_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset==SSND_ADDR)						// Set sound address
		{
			SOUND_ADDR=data&0x1F;					// 32 registers max
			saa1099_control_port_0_w.handler(0, SOUND_ADDR);
	        return;
		}
	
		switch (offset & 0xFF)
		{
		case DSK1_PORT+0:							// This covers the total range of ports for 1 floppy controller
	    case DSK1_PORT+4:
			wd179x_set_side((offset >> 2) & 1);
                        wd179x_command_w.handler(0, data);
			break;
	    case DSK1_PORT+1:
	    case DSK1_PORT+5:
			/* Track byte requested on address line */
			wd179x_set_side((offset >> 2) & 1);
                        wd179x_track_w.handler(0, data);
			break;
	    case DSK1_PORT+2:
	    case DSK1_PORT+6:
			/* Sector byte requested on address line */
			wd179x_set_side((offset >> 2) & 1);
                        wd179x_sector_w.handler(0, data);
	        break;
	    case DSK1_PORT+3:
		case DSK1_PORT+7:
			/* Data byte requested on address line */
			wd179x_set_side((offset >> 2) & 1);
                        wd179x_data_w.handler(0, data);
			break;
		case CLUT_PORT:
			CLUT[(offset >> 8)&0x0F]=data&0x7F;		// set CLUT data
			break;
		case LINE_PORT:
			LINE_INT=data;						// Line to generate interrupt on
			break;
	    case LMPR_PORT:
			LMPR=data;
			coupe_update_memory();
			break;
	    case HMPR_PORT:
			HMPR=data;
			coupe_update_memory();
			break;
	    case VMPR_PORT:
			VMPR=data;
			coupe_update_memory();
			break;
	    case BORD_PORT:
			/* DAC output state */
			speaker_level_w(0,(data>>4) & 0x01);
			break;
	    case SSND_DATA:
			saa1099_write_port_0_w.handler(0, data);
			SOUND_REG[SOUND_ADDR] = data;
			break;
	    default:
			logerror("Write Unsupported Port: %04x,%02x\n", offset,data);
			break;
		}
	} };
	
	static IO_ReadPort coupe_readport[]
            = {
                new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
                new IO_ReadPort(0x0000, 0x0ffff, coupe_port_r),
                new IO_ReadPort(MEMPORT_MARKER, 0) /* end of table */};
	
	static IO_WritePort coupe_writeport[]
            = {
                new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
                new IO_WritePort(0x0000, 0x0ffff, coupe_port_w),
                new IO_WritePort(MEMPORT_MARKER, 0) /* end of table */};
	
	public static GfxDecodeInfo coupe_gfxdecodeinfo[] ={
            new GfxDecodeInfo( -1 )	 /* end of array */
        };
	
	static InputPortPtr input_ports_coupe = new InputPortPtr(){ public void handler() { 
		PORT_START(); // FE  0
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "SHIFT", KEYCODE_LSHIFT,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "Z", KEYCODE_Z,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "X", KEYCODE_X,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "C", KEYCODE_C,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "V", KEYCODE_V,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, "F1",KEYCODE_F1, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "F2",KEYCODE_F2, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "F3",KEYCODE_F3, IP_JOY_NONE );
	
		PORT_START(); // FD  1
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "A", KEYCODE_A,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "S", KEYCODE_S,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "D", KEYCODE_D,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "F", KEYCODE_F,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "G", KEYCODE_G,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, "F4",KEYCODE_F4, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "F5",KEYCODE_F5, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "F6",KEYCODE_F6, IP_JOY_NONE );
	
		PORT_START(); // FB  2
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "Q", KEYCODE_Q,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "W", KEYCODE_W,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "E", KEYCODE_E,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "R", KEYCODE_R,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "T", KEYCODE_T,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, "F7",KEYCODE_F7, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "F8",KEYCODE_F8, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "F9",KEYCODE_F9, IP_JOY_NONE );
	
		PORT_START(); // F7  3
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "1", KEYCODE_1,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "2", KEYCODE_2,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "3", KEYCODE_3,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "4", KEYCODE_4,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "5", KEYCODE_5,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, "ESC",KEYCODE_ESC, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "TAB",KEYCODE_TAB, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "CAPS LOCK",KEYCODE_CAPSLOCK, IP_JOY_NONE );
	
		PORT_START(); // EF  4
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "0", KEYCODE_0,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "9", KEYCODE_9,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "8", KEYCODE_8,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "7", KEYCODE_7,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "6", KEYCODE_6,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, "-", KEYCODE_MINUS, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "+", KEYCODE_EQUALS, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "BACKSPACE",KEYCODE_BACKSPACE, IP_JOY_NONE );
	
		PORT_START(); // DF  5
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "P", KEYCODE_P,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "O", KEYCODE_O,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "I", KEYCODE_I,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "U", KEYCODE_U,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "Y", KEYCODE_Y,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, "=", KEYCODE_OPENBRACE, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, "\"", KEYCODE_CLOSEBRACE, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "F0", KEYCODE_F10, IP_JOY_NONE );
	
		PORT_START(); // BF  6
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "RETURN", KEYCODE_ENTER,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "L", KEYCODE_L,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "K", KEYCODE_K,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "J", KEYCODE_J,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "H", KEYCODE_H,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, ";", KEYCODE_COLON, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, ":", KEYCODE_QUOTE, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "EDIT", KEYCODE_RALT, IP_JOY_NONE );
	
		PORT_START(); // 7F  7
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "SPACE", KEYCODE_SPACE,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "SYMBOL", KEYCODE_LCONTROL,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "M", KEYCODE_M,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "N", KEYCODE_N,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "B", KEYCODE_B,  IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_KEYBOARD, ",", KEYCODE_COMMA, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_KEYBOARD, ".", KEYCODE_STOP, IP_JOY_NONE );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_KEYBOARD, "INV", KEYCODE_SLASH, IP_JOY_NONE );
	
		PORT_START(); // FF  8
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_KEYBOARD, "CTRL", KEYCODE_LALT,  IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, IPT_KEYBOARD, "UP", KEYCODE_UP,  IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_KEYBOARD, "DOWN", KEYCODE_DOWN,  IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_KEYBOARD, "LEFT", KEYCODE_LEFT,  IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_KEYBOARD, "RIGHT", KEYCODE_RIGHT,  IP_JOY_NONE );
	
	INPUT_PORTS_END(); }}; 
	
	/* Initialise the palette */
	static VhConvertColorPromPtr coupe_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int red,green,blue;
		int a;
		char[] coupe_palette = new char[128*3];
		char[] coupe_colortable = new char[128];		// 1-1 relationship to palette!
	
		for (a=0;a<128;a++)
		{
			/* decode colours for palette as follows :
			 * bit number		7		6		5		4		3		2		1		0
			 *						|		|		|		|		|		|		|
			 *				 nothing   G+4	   R+4	   B+4	  ALL+1    G+2	   R+2	   B+2
			 *
			 * these values scaled up to 0-255 range would give modifiers of :	+4 = +(4*36), +2 = +(2*36), +1 = *(1*36)
			 * not quite max of 255 but close enough for me!
			 */
			red=green=blue=0;
			if ((a&0x01) != 0)
				blue+=2*36;
			if ((a&0x02) != 0)
				red+=2*36;
			if ((a&0x04) != 0)
				green+=2*36;
			if ((a&0x08) != 0)
			{
				red+=1*36;
				green+=1*36;
				blue+=1*36;
			}
			if ((a&0x10) != 0)
				blue+=4*36;
			if ((a&0x20) != 0)
				red+=4*36;
			if ((a&0x40) != 0)
				green+=4*36;
	
			coupe_palette[a*3+0]=(char) red;
			coupe_palette[a*3+1]=(char) green;
			coupe_palette[a*3+2]=(char) blue;
                        
                        //System.out.println("("+red+", "+green+", "+blue+")");
                        
                        
	
			coupe_colortable[a]=(char) a;
		}
                
		memcpy(palette,coupe_palette,coupe_palette.length);
		memcpy(colortable,coupe_colortable,coupe_colortable.length);
            }
        };
	
	static Speaker_interface coupe_speaker_interface=new Speaker_interface(
		1,
		new int[]{50}
	);
	
	static SAA1099_interface coupe_saa1099_interface=new SAA1099_interface(
                1,
		new int[][]{{50,50}}
	);
	
	static MachineDriver machine_driver_coupe256 = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
				CPU_Z80|CPU_16BIT_PORT,
				6000000,        /* 6 Mhz */
                                coupe_readmem,coupe_writemem,
				coupe_readport,coupe_writeport,
				coupe_line_interrupt,192 + 10			/* 192 scanlines + 10 lines of vblank (approx).. */
                    )
		},
		50, 0,								/* frames per second, vblank duration */
		1,
		coupe_init_machine_256,
		coupe_shutdown_machine,
	
		/* video hardware */
		64*8,                               /* screen width */
		24*8,                               /* screen height */
		new rectangle( 0, 64*8-1, 0, 24*8-1 ),           /* visible_area */
		coupe_gfxdecodeinfo,				/* graphics decode info */
		128, 128,							/* colors used for the characters */
		coupe_init_palette,					/* initialise palette */
	
		VIDEO_TYPE_RASTER,
		null,
		coupe_vh_start,
		coupe_vh_stop,
		coupe_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			/* standard spectrum sound */
			new MachineSound(
				SOUND_SPEAKER,
				coupe_speaker_interface
			),
			new MachineSound(
				SOUND_SAA1099,
				coupe_saa1099_interface
			)
	    }
	
	);
	
	static MachineDriver machine_driver_coupe512 = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
				CPU_Z80|CPU_16BIT_PORT,
				6000000,        /* 6 Mhz */
                                coupe_readmem,coupe_writemem,
				coupe_readport,coupe_writeport,
				coupe_line_interrupt,192 + 10	/* 192 scanlines + 10 lines of vblank (approx).. */
	
			)
		},
		50, 0,	/* frames per second, vblank duration */
		1,
		coupe_init_machine_512,
		coupe_shutdown_machine,
	
		/* video hardware */
		64*8,                               /* screen width */
		24*8,                               /* screen height */
		new rectangle( 0, 64*8-1, 0, 24*8-1 ),           /* visible_area */
		coupe_gfxdecodeinfo,				/* graphics decode info */
		128, 128,							/* colors used for the characters */
		coupe_init_palette,					/* initialise palette */
	
		VIDEO_TYPE_RASTER,
		null,
		coupe_vh_start,
		coupe_vh_stop,
		coupe_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			/* standard spectrum sound */
			new MachineSound(
				SOUND_SPEAKER,
				coupe_speaker_interface
			),
			new MachineSound(
				SOUND_SAA1099,
				coupe_saa1099_interface
                        )
	        
	    }
	
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_coupe = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x48000,REGION_CPU1,0);
		ROM_LOAD("sam_rom0.rom", 0x40000, 0x4000, 0x9954CF1A);
		ROM_LOAD("sam_rom1.rom", 0x44000, 0x4000, 0xF031AED4);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_coupe512 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x88000,REGION_CPU1,0);
		ROM_LOAD("sam_rom0.rom", 0x80000, 0x4000, 0x9954CF1A);
		ROM_LOAD("sam_rom1.rom", 0x84000, 0x4000, 0xF031AED4);
	ROM_END(); }}; 
	
	static IODevice io_coupe[] = {
            new IODevice(
			IO_FLOPPY,				/* type */
			2,						/* count */
                        /* Only .DSK (raw dump images) are supported at present */
                        "dsk\0",                /* file extensions */
			IO_RESET_NONE,			/* reset if file changed */
			null,					/* id */
			coupe_floppy_init,		/* init */
			basicdsk_floppy_exit,	/* exit */
			null,					/* info */
			null,					/* open */
			null,					/* close */
			floppy_status,			/* status */
			null,					/* seek */
			null,					/* tell */
			null,					/* input */
			null,					/* output */
			null,					/* input_chunk */
			null					/* output_chunk */
	    ),
		new IODevice(IO_END)
	};
	
        //#define io_coupe256 io_coupe
	//#define io_coupe512 io_coupe
	
	/*    YEAR  NAME      PARENT    MACHINE         INPUT     INIT          COMPANY                 		  FULLNAME */
	//COMP( 1989, coupe,	  0,		coupe256,		coupe,	  0,			"Miles Gordon Technology plc",    "Sam Coupe 256K RAM" )
        public static GameDriver driver_coupe = new GameDriver("1989", "coupe", "coupe.java", rom_coupe, null, machine_driver_coupe256, input_ports_coupe, null, io_coupe, "Miles Gordon Technology plc", "Sam Coupe 256K RAM");
	//COMP( 1989, coupe512, coupe,	coupe512,		coupe,	  0,			"Miles Gordon Technology plc",    "Sam Coupe 512K RAM" )    
        public static GameDriver driver_coupe512 = new GameDriver("1989", "coupe512", "coupe.java", rom_coupe512, null, machine_driver_coupe512, input_ports_coupe, null, io_coupe, "Miles Gordon Technology plc", "Sam Coupe 512K RAM");
}
