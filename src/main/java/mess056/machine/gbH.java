/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static common.ptr.*;
import static mess056.machine.gb.*;


public class gbH
{
	
	public static final int VBL_IFLAG = 0x01;
	public static final int LCD_IFLAG = 0x02;
        public static final int TIM_IFLAG = 0x04;
        public static final int SIO_IFLAG = 0x08;
        public static final int EXT_IFLAG = 0x10;

        public static final int VBL_INT = 0;
        public static final int LCD_INT = 1;
        public static final int TIM_INT = 2;
        public static final int SIO_INT = 3;
        public static final int EXT_INT = 4;

        public static final int NONE  = 0;
        public static final int MBC1  = 1;
        public static final int MBC2  = 2;
        public static final int MBC3  = 3;
        public static final int MBC5  = 4;
        public static final int TAMA5 = 5;
        public static final int HUC1  = 6;
        public static final int HUC3  = 7;

        public static final int RAM     = 0x01;  /* Cartridge has RAM */
        public static final int BATTERY = 0x02;  /* Cartridge has a battery to save RAM */
        public static final int TIMER   = 0x04;  /* Cartridge has a real-time-clock (MBC3 only) */
        public static final int RUMBLE  = 0x08;  /* Cartridge has a rumble motor */
        public static final int SRAM    = 0x10;  /* Cartridge has SRAM */
        public static final int UNKNOWN = 0x80;  /* Unknown cartridge type */

        public static int JOYPAD(){ return  gb_ram.memory[0xFF00]; } /* Joystick: 1.1.P15.P14.P13.P12.P11.P10      */
        public static void JOYPAD(int val){ gb_ram.memory[0xFF00] = (char) (val & 0xff); }
        public static int SIODATA(){ return gb_ram.memory[0xFF01]; } /* Serial IO data buffer 					 */
        public static void SIODATA(int val){ gb_ram.memory[0xFF01] = (char) (val & 0xff); }
        public static int SIOCONT(){ return  gb_ram.memory[0xFF02]; } /* Serial IO control register				 */
        public static void SIOCONT(int val){ gb_ram.memory[0xFF02] = (char) (val & 0xff); }
        public static int DIVREG(){ return  gb_ram.memory[0xFF04]; } /* Divider register (???)					 */
        public static int TIMECNT(){ return  gb_ram.memory[0xFF05]; } /* Timer counter. Gen. int. when it overflows */
        public static int TIMEMOD(){ return  gb_ram.memory[0xFF06]; } /* New value of TimeCount after it overflows  */
        public static int TIMEFRQ(){ return  gb_ram.memory[0xFF07]; } /* Timer frequency and start/stop switch 	 */
        public static int IFLAGS(){ return  gb_ram.memory[0xFF0F]; } /* Interrupt flags: 0.0.0.JST.SIO.TIM.LCD.VBL */
        public static void IFLAGS(int val){ gb_ram.memory[0xFF0F] = (char) (val & 0xff); }
        public static int ISWITCH(){ return  gb_ram.memory[0xFFFF]; } /* Switches to enable/disable interrupts 	 */
        public static void ISWITCH(int val){ gb_ram.memory[0xFFFF] = (char) (val & 0xff); }
        public static int LCDCONT(){ return  gb_ram.read(0xFF40); } /* LCD control register						 */
        public static int LCDSTAT(){ return  gb_ram.memory[0xFF41]; } /* LCD status register						 */
        public static void LCDSTAT(int val){ gb_ram.memory[0xFF41] = (char) (val & 0xff); } 
        public static int SCROLLY(){ return  gb_ram.memory[0xFF42] & 0xF; } /* Starting Y position of the background 	 */
        public static void SCROLLY(int val){ gb_ram.memory[0xFF42] = (char) (val & 0xff); }
        public static int SCROLLX(){ return  gb_ram.memory[0xFF43] & 0xF; } /* Starting X position of the background 	 */
        public static void SCROLLX(int val){ gb_ram.memory[0xFF43] = (char) (val & 0xff); }
        public static int CURLINE(){ return  gb_ram.read(0xFF44); } /* Current screen line being scanned 		 */
        public static void CURLINE(int val){ gb_ram.write(0xFF44, (char) (val & 0xff)); }
        public static int CMPLINE(){ return  gb_ram.memory[0xFF45]; } /* Gen. int. when scan reaches this line 	 */
        public static void CMPLINE(int val){ gb_ram.memory[0xFF45] = (char) (val & 0xff); }
        public static int BGRDPAL(){ return  gb_ram.memory[0xFF47]; } /* Background palette						 */
        public static int SPR0PAL(){ return  gb_ram.memory[0xFF48]; } /* Sprite palette #0 						 */
        public static int SPR1PAL(){ return  gb_ram.memory[0xFF49]; } /* Sprite palette #1 						 */
        public static int WNDPOSY(){ return  gb_ram.read(0xFF4A); } /* Window Y position 						 */
        public static void WNDPOSY(int val){ gb_ram.write(0xFF4A, (char) (val & 0xff)); }
        public static int WNDPOSX(){ return  gb_ram.read(0xFF4B); } /* Window X position 						 */
        public static void WNDPOSX(int val){ gb_ram.write(0xFF4B, (char) (val & 0xff)); }

        public static int OAM  = 0xFE00;
        public static int VRAM = 0x8000;

        public static char[] gb_bpal = new char[4];			/* Background palette */
        public static char[] gb_wpal = new char[4];
	public static int[] gb_spal0 = new int[4];			/* Sprite 0 palette */
	public static int[] gb_spal1 = new int[4];			/* Sprite 1 palette */
	public static UBytePtr gb_chrgen;		/* Character generator */
	public static UBytePtr gb_bgdtab;		/* Background character table */
	public static UBytePtr gb_wndtab;		/* Window character table */
        public static int gb_divcount;
	public static int gb_timer_count;
	public static int gb_timer_shift;
	public static int gb_tile_no_mod;
        
        /* -- Super GameBoy specific -- */
        public static int SGB_BORDER_PAL_OFFSET = 64;	/* Border colours stored from pal 4-7	*/
        public static int SGB_XOFFSET = 48;				/* GB screen starts at column 48		*/
        public static int SGB_YOFFSET = 40;				/* GB screen starts at row 40			*/

        public static int[] sgb_pal_data = new int[4096];	/* 512 palettes of 4 colours			*/
        public static int[][] sgb_pal_map = new int[20][18];	/* Palette tile map						*/
        public static UBytePtr sgb_tile_data;		/* 256 tiles of 32 bytes each			*/
        public static UBytePtr sgb_tile_map = new UBytePtr(2048);	/* 32x32 tile map data (0-tile,1-attribute)	*/
        public static int sgb_window_mask;		/* Current GB screen mask				*/
        public static int sgb_hack;				/* Flag set if we're using a hack		*/
        
        /* -- GameBoy Color specific -- */
        public static int HDMA1(){ return  gb_ram.memory[0xFF51]; }		/* HDMA source high byte				*/
        public static void HDMA1(int val){ gb_ram.memory[0xFF51] = (char) val; }
        public static int HDMA2(){ return  gb_ram.memory[0xFF52]; }		/* HDMA source low byte					*/
        public static void HDMA2(int val){ gb_ram.memory[0xFF52] = (char) val; }
        public static int HDMA3(){ return  gb_ram.memory[0xFF53]; }		/* HDMA destination high byte			*/
        public static void HDMA3(int val){ gb_ram.memory[0xFF53] = (char) val; }
        public static int HDMA4(){ return  gb_ram.memory[0xFF54]; }		/* HDMA destination low byte			*/
        public static void HDMA4(int val){ gb_ram.memory[0xFF54] = (char) val; }
        public static int HDMA5(){ return  gb_ram.memory[0xFF55]; }		/* HDMA length/mode/start				*/
        public static void HDMA5(int val){ gb_ram.memory[0xFF55] = (char) val; }

        public static int GBCBCPS(){ return  gb_ram.memory[0xFF68]; }		/* Backgound palette spec				*/
        public static void GBCBCPS(int val){ gb_ram.memory[0xFF68] = (char) val; }
        public static int GBCBCPD(){ return  gb_ram.memory[0xFF69]; }		/* Backgound palette data				*/
        public static int GBCOCPS(){ return  gb_ram.memory[0xFF6A]; }		/* Object palette spec					*/
        public static void GBCOCPS(int val){ gb_ram.memory[0xFF6A] = (char) val; }
        public static int GBCOCPD(){ return  gb_ram.memory[0xFF6B]; }		/* Object palette data					*/
        
        public static final int GBC_MODE_GBC       = 1;			/* GBC is in colour mode				*/
        public static final int GBC_MODE_MONO      = 2;			/* GBC is in mono mode					*/
        public static final int GBC_PAL_OBJ_OFFSET = 32;		/* Object palette offset				*/

        public static UBytePtr gbc_chrgen;		/* Character generator					*/
        public static UBytePtr gbc_bgdtab;		/* Background character table			*/
        public static UBytePtr gbc_wndtab;		/* Window character table				*/
        public static int gbc_mode;			/* is the GBC in mono/colour mode?		*/
        public static int gbc_hdma_enabled;		/* is HDMA enabled?						*/
        
}
