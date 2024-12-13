/***************************************************************************
	commodore c16 home computer

	PeT mess@utanet.at

	documentation
	 www.funet.fi

***************************************************************************/

/*
------------------------------------
c16 commodore c16/c116/c232/c264 (pal version)
plus4	commodore plus4 (ntsc version)
c364 commodore c364/v364 prototype (ntsc version)
------------------------------------
(beta version)

if the game runs to fast with the ntsc version, try the pal version!
flickering affects in one video version, try the other video version

c16(pal version, ntsc version?):
 design like the vc20/c64
 sequel to c64
 other keyboardlayout,
 worse soundchip,
 more colors, but no sprites,
 other tape and gameports plugs
 16 kbyte ram
 newer basic and kernal (ieee floppy support)

c116(pal version, ntsc version?):
 100% softwarecompatible to c16
 small and flat
 gummi keys

232:
 video system versions (?)
 plus4 case
 32 kbyte ram
 userport?, acia6551 chip?

264:
 video system versions (?)
 plus4 case
 64 kbyte ram
 userport?, acia6551 chip?

plus4(pal version, ntsc version?):
 in emu ntsc version!
 case like c116, but with normal keys
 64 kbyte ram
 userport
 build in additional rom with programs

c364 prototype:
 video system versions (?)
 like plus4, but with additional numeric keyblock
 slightly modified kernel rom
 build in speech hardware/rom

state
-----
rasterline based video system
 imperfect scrolling support (when 40 columns or 25 lines)
 lightpen support missing?
 should be enough for 95% of the games and programs
imperfect sound
keyboard, joystick 1 and 2
no speech hardware (c364)
simple tape support
serial bus
 simple disk drives
 no printer or other devices
expansion modules
 rom cartridges
 simple ieee488 floppy support (c1551 floppy disk drive)
 no other expansion modules
no userport (plus4)
 no rs232/v.24 interface
quickloader

some unsolved problems
 memory check by c16 kernel will not recognize more memory without restart of mess
 cpu clock switching/changing (overclocking should it be)

Keys
----
Some PC-Keyboards does not behave well when special two or more keys are
pressed at the same time
(with my keyboard printscreen clears the pressed pause key!)

shift-cbm switches between upper-only and normal character set
(when wrong characters on screen this can help)
run (shift-stop) loads first program from device 8 (dload"*) and starts it
stop-reset activates monitor (use x to leave it)

Tape
----
(DAC 1 volume in noise volume)
loading of wav, prg and prg files in zip archiv
commandline -cassette image
wav:
 8 or 16(not tested) bit, mono, 5000 Hz minimum
 has the same problems like an original tape drive (tone head must
 be adjusted to get working(no load error,...) wav-files)
zip:
 must be placed in current directory
 prg's are played in the order of the files in zip file

use LOAD or LOAD"" or LOAD"",1 for loading of normal programs
use LOAD"",1,1 for loading programs to their special address

several programs relies on more features
(loading other file types, writing, ...)

Discs
-----
only file load from drive 8 and 9 implemented
 loads file from rom directory (*.prg,*p00) (must NOT be specified on commandline)
 or file from d64 image (here also directory LOAD"$",8 supported)
use DLOAD"filename"
or LOAD"filename",8
or LOAD"filename",8,1 (for loading machine language programs at their address)
for loading
type RUN or the appropriate sys call to start them

several programs rely on more features
(loading other file types, writing, ...)

most games rely on starting own programs in the floppy drive
(and therefor cpu level emulation is needed)

Roms
----
.bin .rom .lo .hi .prg
files with boot-sign in it
  recogniced as roms

.prg files loaded at address in its first two bytes
.bin, .rom, .lo , .hi roms loaded to cs1 low, cs1 high, cs2 low, cs2 high
 address accordingly to order in command line

Quickloader
-----------
.prg files supported
loads program into memory and sets program end pointer
(works with most programs)
program ready to get started with RUN
loads first rom when you press quickload key (f8)

when problems start with -log and look into error.log file
 */


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.systems;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.commonH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mess056.deviceH.*;
import static mess056.includes.ted7360H.*;
import static mess056.machine.c16.*;
import static mess056.messH.*;
import static mess056.vidhrdw.ted7360.*;

public class c16
{
	
	public static int VERBOSE_DBG = 0;
	
	/*
	 * commodore c16/c116/plus 4
	 * 16 KByte (C16/C116) or 32 KByte or 64 KByte (plus4) RAM
	 * 32 KByte Rom (C16/C116) 64 KByte Rom (plus4)
	 * availability to append additional 64 KByte Rom
	 *
	 * ports 0xfd00 till 0xff3f are always read/writeable for the cpu
	 * for the video interface chip it seams to read from
	 * ram or from rom in this	area
	 *
	 * writes go always to ram
	 * only 16 KByte Ram mapped to 0x4000,0x8000,0xc000
	 * only 32 KByte Ram mapped to 0x8000
	 *
	 * rom bank at 0x8000: 16K Byte(low bank)
	 * first: basic
	 * second(plus 4 only): plus4 rom low
	 * third: expansion slot
	 * fourth: expansion slot
	 * rom bank at 0xc000: 16K Byte(high bank)
	 * first: kernal
	 * second(plus 4 only): plus4 rom high
	 * third: expansion slot
	 * fourth: expansion slot
	 * writes to 0xfddx select rom banks:
	 * address line 0 and 1: rom bank low
	 * address line 2 and 3: rom bank high
	 *
	 * writes to 0xff3e switches to roms (0x8000 till 0xfd00, 0xff40 till 0xffff)
	 * writes to 0xff3f switches to rams
	 *
	 * at 0xfc00 till 0xfcff is ram or rom kernal readable
	 */
	
	public static Memory_ReadAddress c16_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress(0x0000, 0x0001, c16_m7501_port_r),
		new Memory_ReadAddress(0x0002, 0x3fff, MRA_RAM),
		new Memory_ReadAddress(0x4000, 0x7fff, MRA_BANK1),	   /* only ram memory configuration */
		new Memory_ReadAddress(0x8000, 0xbfff, MRA_BANK2),
		new Memory_ReadAddress(0xc000, 0xfbff, MRA_BANK3),
		new Memory_ReadAddress(0xfc00, 0xfcff, MRA_BANK4),
		new Memory_ReadAddress(0xfd10, 0xfd1f, c16_fd1x_r),
		new Memory_ReadAddress(0xfd30, 0xfd3f, c16_6529_port_r), /* 6529 keyboard matrix */
	/*TODO*///#if 0
	/*TODO*///	new Memory_ReadAddress( 0xfd40, 0xfd5f, sid6581_0_port_r ), /* sidcard, eoroidpro ... */
	/*TODO*///	new Memory_ReadAddress(0xfec0, 0xfedf, c16_iec9_port_r), /* configured in c16_common_init */
	/*TODO*///	new Memory_ReadAddress(0xfee0, 0xfeff, c16_iec8_port_r), /* configured in c16_common_init */
	/*TODO*///#endif
		new Memory_ReadAddress(0xff00, 0xff1f, ted7360_port_r),
		new Memory_ReadAddress(0xff20, 0xffff, MRA_BANK8),
	/*	new Memory_ReadAddress( 0x10000, 0x3ffff, MRA_ROM ), */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress c16_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress(0x0000, 0x0001, c16_m7501_port_w, c16_memory),
		new Memory_WriteAddress(0x0002, 0x3fff, MWA_RAM),
	/*TODO*///#ifndef NEW_BANKHANDLER
		new Memory_WriteAddress(0x4000, 0x7fff, MWA_BANK5),
		new Memory_WriteAddress(0x8000, 0xbfff, MWA_BANK6),
		new Memory_WriteAddress(0xc000, 0xfcff, MWA_BANK7),
	/*TODO*///#endif
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0x4000, 0x7fff, c16_write_4000),  /*configured in c16_common_init */
	/*TODO*///	new Memory_WriteAddress(0x8000, 0xbfff, c16_write_8000),  /*configured in c16_common_init */
	/*TODO*///	new Memory_WriteAddress(0xc000, 0xfcff, c16_write_c000),  /*configured in c16_common_init */
	/*TODO*///#endif
		new Memory_WriteAddress(0xfd30, 0xfd3f, c16_6529_port_w), /* 6529 keyboard matrix */
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xfd40, 0xfd5f, sid6581_0_port_w),
	/*TODO*///#endif
		new Memory_WriteAddress(0xfdd0, 0xfddf, c16_select_roms), /* rom chips selection */
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xfec0, 0xfedf, c16_iec9_port_w), /*configured in c16_common_init */
	/*TODO*///	new Memory_WriteAddress(0xfee0, 0xfeff, c16_iec8_port_w), /*configured in c16_common_init */
	/*TODO*///#endif
		new Memory_WriteAddress(0xff00, 0xff1f, ted7360_port_w),
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xff20, 0xff3d, c16_write_ff20),  /*configure in c16_common_init */
	/*TODO*///#endif
		new Memory_WriteAddress(0xff3e, 0xff3e, c16_switch_to_rom),
		new Memory_WriteAddress(0xff3f, 0xff3f, c16_switch_to_ram),
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xff40, 0xffff, c16_write_ff40),  /*configure in c16_common_init */
	/*TODO*/////	new Memory_WriteAddress(0x10000, 0x3ffff, MWA_ROM),
	/*TODO*///#endif
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress plus4_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress(0x0000, 0x0001, c16_m7501_port_r),
		new Memory_ReadAddress(0x0002, 0x7fff, MRA_RAM),
		new Memory_ReadAddress(0x8000, 0xbfff, MRA_BANK2),
		new Memory_ReadAddress(0xc000, 0xfbff, MRA_BANK3),
		new Memory_ReadAddress(0xfc00, 0xfcff, MRA_BANK4),
		new Memory_ReadAddress(0xfd00, 0xfd0f, c16_6551_port_r),
		new Memory_ReadAddress(0xfd10, 0xfd1f, plus4_6529_port_r),
		new Memory_ReadAddress(0xfd30, 0xfd3f, c16_6529_port_r), /* 6529 keyboard matrix */
	/*TODO*///#if 0
	/*TODO*///	new Memory_ReadAddress( 0xfd40, 0xfd5f, sid6581_0_port_r ), /* sidcard, eoroidpro ... */
	/*TODO*///	new Memory_ReadAddress(0xfec0, 0xfedf, c16_iec9_port_r), /* configured in c16_common_init */
	/*TODO*///	new Memory_ReadAddress(0xfee0, 0xfeff, c16_iec8_port_r), /* configured in c16_common_init */
	/*TODO*///#endif
		new Memory_ReadAddress(0xff00, 0xff1f, ted7360_port_r),
		new Memory_ReadAddress(0xff20, 0xffff, MRA_BANK8),
	/*	new Memory_ReadAddress( 0x10000, 0x3ffff, MRA_ROM ), */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress plus4_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress(0x0000, 0x0001, c16_m7501_port_w, c16_memory),
		new Memory_WriteAddress(0x0002, 0xfcff, MWA_RAM),
		new Memory_WriteAddress(0xfd00, 0xfd0f, c16_6551_port_w),
		new Memory_WriteAddress(0xfd10, 0xfd1f, plus4_6529_port_w),
		new Memory_WriteAddress(0xfd30, 0xfd3f, c16_6529_port_w), /* 6529 keyboard matrix */
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xfd40, 0xfd5f, sid6581_0_port_w),
	/*TODO*///#endif
		new Memory_WriteAddress(0xfdd0, 0xfddf, c16_select_roms), /* rom chips selection */
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xfec0, 0xfedf, c16_iec9_port_w), /*configured in c16_common_init */
	/*TODO*///	new Memory_WriteAddress(0xfee0, 0xfeff, c16_iec8_port_w), /*configured in c16_common_init */
	/*TODO*///#endif
		new Memory_WriteAddress(0xff00, 0xff1f, ted7360_port_w),
		new Memory_WriteAddress(0xff20, 0xff3d, MWA_RAM),
		new Memory_WriteAddress(0xff3e, 0xff3e, c16_switch_to_rom),
		new Memory_WriteAddress(0xff3f, 0xff3f, plus4_switch_to_ram),
		new Memory_WriteAddress(0xff40, 0xffff, MWA_RAM),
	//	new Memory_WriteAddress(0x10000, 0x3ffff, MWA_ROM),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress c364_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress(0x0000, 0x0001, c16_m7501_port_r),
		new Memory_ReadAddress(0x0002, 0x7fff, MRA_RAM),
		new Memory_ReadAddress(0x8000, 0xbfff, MRA_BANK2),
		new Memory_ReadAddress(0xc000, 0xfbff, MRA_BANK3),
		new Memory_ReadAddress(0xfc00, 0xfcff, MRA_BANK4),
		new Memory_ReadAddress(0xfd00, 0xfd0f, c16_6551_port_r),
		new Memory_ReadAddress(0xfd10, 0xfd1f, plus4_6529_port_r),
	/*TODO*///	new Memory_ReadAddress(0xfd20, 0xfd2f, c364_speech_r ),
		new Memory_ReadAddress(0xfd30, 0xfd3f, c16_6529_port_r), /* 6529 keyboard matrix */
	/*TODO*///#if 0
	/*TODO*///	new Memory_ReadAddress( 0xfd40, 0xfd5f, sid6581_0_port_r ), /* sidcard, eoroidpro ... */
	/*TODO*///	new Memory_ReadAddress(0xfec0, 0xfedf, c16_iec9_port_r), /* configured in c16_common_init */
	/*TODO*///	new Memory_ReadAddress(0xfee0, 0xfeff, c16_iec8_port_r), /* configured in c16_common_init */
	/*TODO*///#endif
		new Memory_ReadAddress(0xff00, 0xff1f, ted7360_port_r),
		new Memory_ReadAddress(0xff20, 0xffff, MRA_BANK8),
	/*	new Memory_ReadAddress( 0x10000, 0x3ffff, MRA_ROM ), */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress c364_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress(0x0000, 0x0001, c16_m7501_port_w, c16_memory),
		new Memory_WriteAddress(0x0002, 0xfcff, MWA_RAM),
		new Memory_WriteAddress(0xfd00, 0xfd0f, c16_6551_port_w),
		new Memory_WriteAddress(0xfd10, 0xfd1f, plus4_6529_port_w),
	/*TODO*///	new Memory_WriteAddress(0xfd20, 0xfd2f, c364_speech_w ),
		new Memory_WriteAddress(0xfd30, 0xfd3f, c16_6529_port_w), /* 6529 keyboard matrix */
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xfd40, 0xfd5f, sid6581_0_port_w),
	/*TODO*///#endif
		new Memory_WriteAddress(0xfdd0, 0xfddf, c16_select_roms), /* rom chips selection */
	/*TODO*///#if 0
	/*TODO*///	new Memory_WriteAddress(0xfec0, 0xfedf, c16_iec9_port_w), /*configured in c16_common_init */
	/*TODO*///	new Memory_WriteAddress(0xfee0, 0xfeff, c16_iec8_port_w), /*configured in c16_common_init */
	/*TODO*///#endif
		new Memory_WriteAddress(0xff00, 0xff1f, ted7360_port_w),
		new Memory_WriteAddress(0xff20, 0xff3d, MWA_RAM),
		new Memory_WriteAddress(0xff3e, 0xff3e, c16_switch_to_rom),
		new Memory_WriteAddress(0xff3f, 0xff3f, plus4_switch_to_ram),
		new Memory_WriteAddress(0xff40, 0xffff, MWA_RAM),
	//	new Memory_WriteAddress(0x10000, 0x3ffff, MWA_ROM),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static void DIPS_HELPER(int bit, String name, int keycode){ 
	   PORT_BITX(bit, IP_ACTIVE_HIGH, IPT_KEYBOARD, name, keycode, IP_JOY_NONE);
        }
	
	public static void DIPS_BOTH(){
		PORT_START();
		PORT_BIT( 8, IP_ACTIVE_HIGH, IPT_BUTTON1);
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT ( 0x7, 0x0,	 IPT_UNUSED );
		PORT_START();
		PORT_BITX( 8, IP_ACTIVE_HIGH, IPT_BUTTON1|IPF_PLAYER2, 
			   "P2 Button", KEYCODE_LALT,JOYCODE_2_BUTTON1 );
		PORT_BITX( 0x80, IP_ACTIVE_HIGH, 
			   IPT_JOYSTICK_LEFT|IPF_PLAYER2 | IPF_8WAY,
			   "P2 Left", KEYCODE_DEL,JOYCODE_2_LEFT );
		PORT_BITX( 0x40, IP_ACTIVE_HIGH, 
			   IPT_JOYSTICK_RIGHT|IPF_PLAYER2 | IPF_8WAY,
			   "P2 Right",KEYCODE_PGDN,JOYCODE_2_RIGHT );
		PORT_BITX( 0x20, IP_ACTIVE_HIGH, 
			   IPT_JOYSTICK_UP|IPF_PLAYER2 | IPF_8WAY,"P2 Up", 
			   KEYCODE_HOME, JOYCODE_2_UP);
		PORT_BITX( 0x10, IP_ACTIVE_HIGH, 
			   IPT_JOYSTICK_DOWN|IPF_PLAYER2 | IPF_8WAY,
			   "P2 Down", KEYCODE_END, JOYCODE_2_DOWN);
		PORT_BIT ( 0x7, 0x0,	 IPT_UNUSED );
		PORT_START();
		DIPS_HELPER( 0x8000, "ESC", KEYCODE_TILDE);
		DIPS_HELPER( 0x4000, "1 !   BLK   ORNG", KEYCODE_1);
		DIPS_HELPER( 0x2000, "2 \"   WHT   BRN", KEYCODE_2);
		DIPS_HELPER( 0x1000, "3 #   RED   L GRN", KEYCODE_3);
		DIPS_HELPER( 0x0800, "4 $   CYN   PINK", KEYCODE_4);
		DIPS_HELPER( 0x0400, "5 %   PUR   BL GRN", KEYCODE_5);
		DIPS_HELPER( 0x0200, "6 &   GRN   L BLU", KEYCODE_6);
		DIPS_HELPER( 0x0100, "7 '   BLU   D BLU", KEYCODE_7);
		DIPS_HELPER( 0x0080, "8 (   YEL   L GRN", KEYCODE_8);
		DIPS_HELPER( 0x0040, "9 )   RVS-ON", KEYCODE_9);
		DIPS_HELPER( 0x0020, "0 ^   RVS-OFF", KEYCODE_0);
		DIPS_HELPER( 0x0010, "Cursor Left", KEYCODE_4_PAD);
		DIPS_HELPER( 0x0008, "Cursor Right", KEYCODE_6_PAD);
		DIPS_HELPER( 0x0004, "Cursor Up", KEYCODE_8_PAD);
		DIPS_HELPER( 0x0002, "Cursor Down", KEYCODE_2_PAD);
		DIPS_HELPER( 0x0001, "DEL INST", KEYCODE_BACKSPACE);
		PORT_START();
		DIPS_HELPER( 0x8000, "CTRL", KEYCODE_RCONTROL);
		DIPS_HELPER( 0x4000, "Q", KEYCODE_Q);
		DIPS_HELPER( 0x2000, "W", KEYCODE_W);
		DIPS_HELPER( 0x1000, "E", KEYCODE_E);
		DIPS_HELPER( 0x0800, "R", KEYCODE_R);
		DIPS_HELPER( 0x0400, "T", KEYCODE_T);
		DIPS_HELPER( 0x0200, "Y", KEYCODE_Y);
		DIPS_HELPER( 0x0100, "U", KEYCODE_U);
		DIPS_HELPER( 0x0080, "I", KEYCODE_I);
		DIPS_HELPER( 0x0040, "O", KEYCODE_O);
		DIPS_HELPER( 0x0020, "P", KEYCODE_P);
		DIPS_HELPER( 0x0010, "At", KEYCODE_OPENBRACE);
		DIPS_HELPER( 0x0008, "+", KEYCODE_CLOSEBRACE);
		DIPS_HELPER( 0x0004, "-",KEYCODE_MINUS);
		DIPS_HELPER( 0x0002, "HOME CLEAR", KEYCODE_EQUALS);
		DIPS_HELPER( 0x0001, "STOP RUN", KEYCODE_TAB);
		PORT_START();
		PORT_BITX ( 0x8000, 0, IPT_DIPSWITCH_NAME | IPF_TOGGLE, 
					"SHIFT-Lock (switch)", KEYCODE_CAPSLOCK, CODE_NONE);
		PORT_DIPSETTING(  0, DEF_STR("Off"));
		PORT_DIPSETTING( 0x8000, DEF_STR("On"));
		/*PORT_BITX( 0x8000, IP_ACTIVE_HIGH, IPF_TOGGLE,*/
				 /*"SHIFT-LOCK (switch);, KEYCODE_CAPSLOCK, IP_JOY_NONE)*/
		DIPS_HELPER( 0x4000, "A", KEYCODE_A);
		DIPS_HELPER( 0x2000, "S", KEYCODE_S);
		DIPS_HELPER( 0x1000, "D", KEYCODE_D);
		DIPS_HELPER( 0x0800, "F", KEYCODE_F);
		DIPS_HELPER( 0x0400, "G", KEYCODE_G);
		DIPS_HELPER( 0x0200, "H", KEYCODE_H);
		DIPS_HELPER( 0x0100, "J", KEYCODE_J);
		DIPS_HELPER( 0x0080, "K", KEYCODE_K);
		DIPS_HELPER( 0x0040, "L", KEYCODE_L);
		DIPS_HELPER( 0x0020, ": [", KEYCODE_COLON);
		DIPS_HELPER( 0x0010, "; ]", KEYCODE_QUOTE);
		DIPS_HELPER( 0x0008, "*", KEYCODE_BACKSLASH);
		DIPS_HELPER( 0x0004, "RETURN",KEYCODE_ENTER);
		DIPS_HELPER( 0x0002, "CBM", KEYCODE_RALT);
		DIPS_HELPER( 0x0001, "Left-Shift", KEYCODE_LSHIFT);
		PORT_START();
		DIPS_HELPER( 0x8000, "Z", KEYCODE_Z);
		DIPS_HELPER( 0x4000, "X", KEYCODE_X);
		DIPS_HELPER( 0x2000, "C", KEYCODE_C);
		DIPS_HELPER( 0x1000, "V", KEYCODE_V);
		DIPS_HELPER( 0x0800, "B", KEYCODE_B);
		DIPS_HELPER( 0x0400, "N", KEYCODE_N);
		DIPS_HELPER( 0x0200, "M", KEYCODE_M);
		DIPS_HELPER( 0x0100, ", <   FLASH-ON", KEYCODE_COMMA);
		DIPS_HELPER( 0x0080, ". >   FLASH-OFF", KEYCODE_STOP);
		DIPS_HELPER( 0x0040, "/ ?", KEYCODE_SLASH);
		DIPS_HELPER( 0x0020, "Right-Shift", KEYCODE_RSHIFT);
		DIPS_HELPER( 0x0010, "Pound", KEYCODE_INSERT);
		DIPS_HELPER( 0x0008, "= Pi Arrow-Left", KEYCODE_PGUP);
		DIPS_HELPER( 0x0004, "Space", KEYCODE_SPACE);
		DIPS_HELPER( 0x0002, "f1 f4", KEYCODE_F1);
		DIPS_HELPER( 0x0001, "f2 f5", KEYCODE_F2);
		PORT_START();
		DIPS_HELPER( 0x8000, "f3 f6", KEYCODE_F3);
		DIPS_HELPER( 0x4000, "HELP f7", KEYCODE_F4);
		PORT_BITX ( 0x2000, 0, IPT_DIPSWITCH_NAME|IPF_TOGGLE,
					"Swap Gameport 1 and 2", KEYCODE_NUMLOCK, CODE_NONE);
		PORT_DIPSETTING(  0, "No");
		PORT_DIPSETTING( 0x2000, "Yes");
	/*PORT_BITX( 0x2000, IP_ACTIVE_HIGH, IPF_TOGGLE,*/
	/*"Swap Gameport 1 and 2", KEYCODE_NUMLOCK, IP_JOY_NONE);*/
		DIPS_HELPER( 0x08, "Quickload", KEYCODE_F8);
		DIPS_HELPER( 0x04, "Tape Drive Play",       KEYCODE_F5);
		DIPS_HELPER( 0x02, "Tape Drive Record",     KEYCODE_F6);
		DIPS_HELPER( 0x01, "Tape Drive Stop",       KEYCODE_F7);
		PORT_START();
		PORT_DIPNAME ( 0x80, 0x80, "Joystick 1");
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x80, DEF_STR( "On") );
		PORT_DIPNAME ( 0x40, 0x40, "Joystick 2");
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(0x40, DEF_STR( "On") );
		PORT_DIPNAME   ( 0x20, 0x20, "Tape Drive/Device 1");
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(0x20, DEF_STR( "On") );
		PORT_DIPNAME   ( 0x10, 0x00, " Tape Sound");
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(0x10, DEF_STR( "On") );
        }
	
	static InputPortPtr input_ports_c16  = new InputPortPtr(){ public void handler() {
		DIPS_BOTH();
		PORT_START();
		PORT_BIT (0xc0, 0x0, IPT_UNUSED);	   /* no real floppy */
		PORT_DIPNAME ( 0x38, 8, "Device 8");
		PORT_DIPSETTING(  0, "None" );
		PORT_DIPSETTING( 8, "C1551 Floppy Drive Simulation" );
		PORT_DIPSETTING( 0x18, "Serial Bus/VC1541 Floppy Drive Simulation" );
		PORT_DIPNAME ( 0x07, 0x01, "Device 9");
		PORT_DIPSETTING(  0, "None" );
		PORT_DIPSETTING(  1, "C1551 Floppy Drive Simulation" );
		PORT_DIPSETTING(  3, "Serial Bus/VC1541 Floppy Drive Simulation" );
		PORT_START();
		PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
		PORT_DIPSETTING(  0, DEF_STR("Off"));
		PORT_DIPSETTING( 0x80, DEF_STR("On"));
		PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
		PORT_DIPSETTING(  0, DEF_STR("Off"));
		PORT_DIPSETTING( 0x40, DEF_STR("On"));
		PORT_BIT (0x10, 0x0, IPT_UNUSED);	   /* pal */
		PORT_BIT (0xc, 0x0, IPT_UNUSED);	   /* c16 */
		PORT_DIPNAME (3, 3, "Memory");
		PORT_DIPSETTING (0, "16 KByte");
		PORT_DIPSETTING (2, "32 KByte");
		PORT_DIPSETTING (3, "64 KByte");
	INPUT_PORTS_END(); }}; 
	
	/*TODO*///static InputPortPtr input_ports_c16c  = new InputPortPtr(){ public void handler() {
	/*TODO*///	DIPS_BOTH();
	/*TODO*///	PORT_START();
	/*TODO*///	PORT_BIT (0xc0, 0x40, IPT_UNUSED);	   /* c1551 floppy */
	/*TODO*///	PORT_BIT (0x38, 0x10, IPT_UNUSED);
	/*TODO*///	PORT_BIT (0x7, 0x0, IPT_UNUSED);
	/*TODO*///	PORT_START
	/*TODO*///	PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x80, DEF_STR(On);
	/*TODO*///	PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x40, DEF_STR(On);
	/*TODO*///	PORT_BIT (0x10, 0x0, IPT_UNUSED);	   /* pal */
	/*TODO*///	PORT_BIT (0xc, 0x0, IPT_UNUSED);	   /* c16 */
	/*TODO*///	PORT_DIPNAME (3, 3, "Memory");
	/*TODO*///	PORT_DIPSETTING (0, "16 KByte");
	/*TODO*///	PORT_DIPSETTING (2, "32 KByte");
	/*TODO*///	PORT_DIPSETTING (3, "64 KByte");
	/*TODO*///INPUT_PORTS_END(); }}; 
	
	/*TODO*///INPUT_PORTS_START (c16v)
	/*TODO*///	DIPS_BOTH
	/*TODO*///	PORT_START
	/*TODO*///	PORT_BIT (0xc0, 0x80, IPT_UNUSED);	   /* vc1541 floppy */
	/*TODO*///	PORT_BIT (0x38, 0x20, IPT_UNUSED);
	/*TODO*///	PORT_BIT (0x7, 0x0, IPT_UNUSED);
	/*TODO*///	PORT_START
	/*TODO*///	PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x80, DEF_STR(On);
	/*TODO*///	PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x40, DEF_STR(On);
	/*TODO*///	PORT_BIT (0x10, 0x0, IPT_UNUSED);	   /* pal */
	/*TODO*///	PORT_BIT (0xc, 0x0, IPT_UNUSED);	   /* c16 */
	/*TODO*///	PORT_DIPNAME (3, 3, "Memory");
	/*TODO*///	PORT_DIPSETTING (0, "16 KByte");
	/*TODO*///	PORT_DIPSETTING (2, "32 KByte");
	/*TODO*///	PORT_DIPSETTING (3, "64 KByte");
	/*TODO*///INPUT_PORTS_END(); }}; 
	
	/*TODO*///INPUT_PORTS_START (plus4)
	/*TODO*///	DIPS_BOTH
	/*TODO*///	PORT_START
	/*TODO*///	PORT_BIT (0xc0, 0x00, IPT_UNUSED);	   /* no real floppy */
	/*TODO*///	PORT_DIPNAME ( 0x38, 0x8, "Device 8");
	/*TODO*///	PORT_DIPSETTING(  0, "None" );
	/*TODO*///	PORT_DIPSETTING(  8, "C1551 Floppy Drive Simulation" );
	/*TODO*///	PORT_DIPSETTING(  0x18, "Serial Bus/VC1541 Floppy Drive Simulation" );
	/*TODO*///	PORT_DIPNAME ( 0x07, 0x01, "Device 9");
	/*TODO*///	PORT_DIPSETTING(  0, "None" );
	/*TODO*///	PORT_DIPSETTING(  1, "C1551 Floppy Drive Simulation" );
	/*TODO*///	PORT_DIPSETTING(  3, "Serial Bus/VC1541 Floppy Drive Simulation" );
	/*TODO*///	PORT_START
	/*TODO*///	PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x80, DEF_STR(On);
	/*TODO*///	PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x40, DEF_STR(On);
	/*TODO*///	PORT_BIT (0x10, 0x10, IPT_UNUSED);	   /* ntsc */
	/*TODO*///	PORT_BIT (0xc, 0x4, IPT_UNUSED);	   /* plus4 */
	/*TODO*///	PORT_BIT (0x3, 0x3, IPT_UNUSED);	   /* 64K Memory */
	/*TODO*///INPUT_PORTS_END(); }}; 
	
	/*TODO*///INPUT_PORTS_START (plus4c)
	/*TODO*///	DIPS_BOTH
	/*TODO*///	PORT_START
	/*TODO*///	PORT_BIT (0xc0, 0x40, IPT_UNUSED);	   /* c1551 floppy */
	/*TODO*///	PORT_BIT (0x38, 0x10, IPT_UNUSED);
	/*TODO*///	PORT_BIT (0x7, 0x0, IPT_UNUSED);
	/*TODO*///	PORT_START
	/*TODO*///	PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x80, DEF_STR(On);
	/*TODO*///	PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x40, DEF_STR(On);
	/*TODO*///	PORT_BIT (0x10, 0x10, IPT_UNUSED);	   /* ntsc */
	/*TODO*///	PORT_BIT (0xc, 0x4, IPT_UNUSED);	   /* plus4 */
	/*TODO*///	PORT_BIT (0x3, 0x3, IPT_UNUSED);	   /* 64K Memory */
	/*TODO*///INPUT_PORTS_END(); }}; 
	
	/*TODO*///INPUT_PORTS_START (plus4v)
	/*TODO*///	DIPS_BOTH
	/*TODO*///	PORT_START
	/*TODO*///	PORT_BIT (0xc0, 0x80, IPT_UNUSED);	   /* vc1541 floppy */
	/*TODO*///	PORT_BIT (0x38, 0x20, IPT_UNUSED);
	/*TODO*///	PORT_BIT (0x7, 0x0, IPT_UNUSED);
	/*TODO*///	PORT_START
	/*TODO*///	PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x80, DEF_STR(On);
	/*TODO*///	PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x40, DEF_STR(On);
	/*TODO*///	PORT_BIT (0x10, 0x10, IPT_UNUSED);	   /* ntsc */
	/*TODO*///	PORT_BIT (0xc, 0x4, IPT_UNUSED);	   /* plus4 */
	/*TODO*///	PORT_BIT (0x3, 0x3, IPT_UNUSED);	   /* 64K Memory */
	/*TODO*///INPUT_PORTS_END(); }}; 
	
	/*TODO*///#if 0
	/*TODO*///INPUT_PORTS_START (c364)
	/*TODO*///	DIPS_BOTH
	/*TODO*///	PORT_START
	/*TODO*///	PORT_BIT (0xc0, 0x00, IPT_UNUSED);	   /* no real floppy */
	/*TODO*///	PORT_DIPNAME ( 0x38, 0x10, "Device 8");
	/*TODO*///	PORT_DIPSETTING(  0, "None" );
	/*TODO*///	PORT_DIPSETTING(  8, "C1551 Floppy Drive Simulation" );
	/*TODO*///	PORT_DIPSETTING(  0x18, "Serial Bus/VC1541 Floppy Drive Simulation" );
	/*TODO*///	PORT_DIPNAME ( 0x03, 0x01, "Device 9");
	/*TODO*///	PORT_DIPSETTING(  0, "None" );
	/*TODO*///	PORT_DIPSETTING(  1, "C1551 Floppy Drive Simulation" );
	/*TODO*///	PORT_DIPSETTING(  3, "Serial Bus/VC1541 Floppy Drive Simulation" );
	/*TODO*///	PORT_START
	/*TODO*///	PORT_DIPNAME ( 0x80, 0x80, "Sidcard");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x80, DEF_STR(On);
	/*TODO*///	PORT_DIPNAME ( 0x40, 0, " SID 0xd400 hack");
	/*TODO*///	PORT_DIPSETTING(  0, DEF_STR(Off);
	/*TODO*///	PORT_DIPSETTING( 0x40, DEF_STR(On);
	/*TODO*///	PORT_BIT (0x10, 0x10, IPT_UNUSED);	   /* ntsc */
	/*TODO*///	PORT_BIT (0xc, 0x8, IPT_UNUSED);	   /* 364 */
	/*TODO*///	PORT_BIT (0x3, 0x3, IPT_UNUSED);	   /* 64K Memory */
	/*TODO*///	 /* numeric block
	/*TODO*///		hardware wired to other keys?
	/*TODO*///		@ + - =
	/*TODO*///		7 8 9 *
	/*TODO*///		4 5 6 /
	/*TODO*///		1 2 3
	/*TODO*///		? ? ? ?
	/*TODO*///		( 0 , . Return ???) */
	/*TODO*///INPUT_PORTS_END(); }}; 
	/*TODO*///#endif
	
	/* Initialise the c16 palette */
	static VhConvertColorPromPtr c16_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
		memcpy (sys_palette, ted7360_palette, ted7360_palette.length);
            }
        };
	
	/*TODO*///#if 0
	/*TODO*////* cbm version in kernel at 0xff80 (offset 0x3f80)
	/*TODO*///   0x80 means pal version */
	/*TODO*///
	/*TODO*///	 /* basic */
	/*TODO*///	 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
	/*TODO*///
	/*TODO*///	 /* kernal pal */
	/*TODO*///	 ROM_LOAD("318004.05",    0x14000, 0x4000, 0x71c07bd4);
	/*TODO*///	 ROM_LOAD ("318004.03", 0x14000, 0x4000, 0x77bab934);
	/*TODO*///
	/*TODO*///	 /* kernal ntsc */
	/*TODO*///	 ROM_LOAD ("318005.05", 0x14000, 0x4000, 0x70295038);
	/*TODO*///	 ROM_LOAD ("318005.04", 0x14000, 0x4000, 0x799a633d);
	/*TODO*///
	/*TODO*///	 /* 3plus1 program */
	/*TODO*///	 ROM_LOAD ("317053.01", 0x18000, 0x4000, 0x4fd1d8cb);
	/*TODO*///	 ROM_LOAD ("317054.01", 0x1c000, 0x4000, 0x109de2fc);
	/*TODO*///
	/*TODO*///	 /* same as 109de2fc, but saved from running machine, so
	/*TODO*///		io area different ! */
	/*TODO*///	 ROM_LOAD ("3plus1hi.rom", 0x1c000, 0x4000, 0xaab61387);
	/*TODO*///	 /* same but lo and hi in one rom */
	/*TODO*///	 ROM_LOAD ("3plus1.rom", 0x18000, 0x8000, 0x7d464449);
	/*TODO*///#endif
	
	static RomLoadPtr rom_c16 = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD("318004.05",    0x14000, 0x4000, 0x71c07bd4);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_c16hun = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD("hungary.bin",    0x14000, 0x4000, 0x775f60c5);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_c16c = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD("318004.05",    0x14000, 0x4000, 0x71c07bd4);
		 /*TODO*///C1551_ROM (REGION_CPU2)
	ROM_END(); }}; 
	
	static RomLoadPtr rom_c16v = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD("318004.05",    0x14000, 0x4000, 0x71c07bd4);
		 /*TODO*///VC1541_ROM (REGION_CPU2)
	ROM_END(); }}; 
	
	static RomLoadPtr rom_plus4 = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD ("318005.05", 0x14000, 0x4000, 0x70295038);
		 ROM_LOAD ("317053.01", 0x18000, 0x4000, 0x4fd1d8cb);
		 ROM_LOAD ("317054.01", 0x1c000, 0x4000, 0x109de2fc);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_plus4c = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD ("318005.05", 0x14000, 0x4000, 0x70295038);
		 ROM_LOAD ("317053.01", 0x18000, 0x4000, 0x4fd1d8cb);
		 ROM_LOAD ("317054.01", 0x1c000, 0x4000, 0x109de2fc);
		 /*TODO*///C1551_ROM (REGION_CPU2)
	ROM_END(); }}; 
	
	static RomLoadPtr rom_plus4v = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD ("318005.05", 0x14000, 0x4000, 0x70295038);
		 ROM_LOAD ("317053.01", 0x18000, 0x4000, 0x4fd1d8cb);
		 ROM_LOAD ("317054.01", 0x1c000, 0x4000, 0x109de2fc);
		 /*TODO*///VC1541_ROM (REGION_CPU2)
	ROM_END(); }}; 
	
	static RomLoadPtr rom_c364 = new RomLoadPtr(){ public void handler(){
		 ROM_REGION (0x40000, REGION_CPU1, 0);
		 ROM_LOAD ("318006.01", 0x10000, 0x4000, 0x74eaae87);
		 ROM_LOAD ("kern364p.bin", 0x14000, 0x4000, 0x84fd4f7a);
		 ROM_LOAD ("317053.01", 0x18000, 0x4000, 0x4fd1d8cb);
		 ROM_LOAD ("317054.01", 0x1c000, 0x4000, 0x109de2fc);
		 /* at address 0x20000 not so good */
		 ROM_LOAD ("spk3cc4.bin", 0x28000, 0x4000, 0x5227c2ee);
	ROM_END(); }}; 
	
	/*TODO*///static SID6581_interface sidc16_sound_interface =
	/*TODO*///{
	/*TODO*///	{
	/*TODO*///		sid6581_custom_start,
	/*TODO*///		sid6581_custom_stop,
	/*TODO*///		sid6581_custom_update
	/*TODO*///	},
	/*TODO*///	1,
	/*TODO*///	{
	/*TODO*///		{
	/*TODO*///			MIXER(50, MIXER_PAN_CENTER),
	/*TODO*///			MOS8580,
	/*TODO*///			TED7360PAL_CLOCK/4,
	/*TODO*///			NULL
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///};
	
	/*TODO*///static SID6581_interface sidplus4_sound_interface =
	/*TODO*///{
	/*TODO*///	{
	/*TODO*///		sid6581_custom_start,
	/*TODO*///		sid6581_custom_stop,
	/*TODO*///		sid6581_custom_update
	/*TODO*///	},
	/*TODO*///	1,
	/*TODO*///	{
	/*TODO*///		{
	/*TODO*///			MIXER(50, MIXER_PAN_CENTER),
	/*TODO*///			MOS8580,
	/*TODO*///			TED7360NTSC_CLOCK/4,
	/*TODO*///			NULL
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///};
	
	static MachineDriver machine_driver_c16 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				//CPU_M7501,				   /* MOS7501 has no nmi line */
                                CPU_M6502,
				1400000,				   /*TED7360PAL_CLOCK/2, */
				c16_readmem, c16_writemem,
				null, null,
				c16_frame_interrupt, 1,
				ted7360_raster_interrupt, TED7360_HRETRACERATE
			)
		},
		TED7360PAL_VRETRACERATE, 0, 	/* frames per second, vblank duration */
		0,
		c16_init_machine,
		c16_shutdown_machine,
	
	  /* video hardware */
		336,							   /* screen width */
		216,							   /* screen height */
		new rectangle(0, 336 - 1, 0, 216 - 1),                     /* visible_area */
		null,                                                      /* graphics decode info */
		ted7360_palette.length / 3,
		0,
		c16_init_palette,				   /* convert color prom */
		VIDEO_TYPE_RASTER,
		null,
		ted7360_vh_start,
		ted7360_vh_stop,
		ted7360_vh_screenrefresh,
	
	  /* sound hardware */
		0, 0, 0, 0,
		/*TODO*///{
		/*TODO*///	{SOUND_CUSTOM, &ted7360_sound_interface},
		/*TODO*///	{SOUND_CUSTOM, &sidc16_sound_interface},
		/*TODO*///	{SOUND_DAC, &vc20tape_sound_interface}
		/*TODO*///}
                
                null
	);
	
	/*TODO*///static struct MachineDriver machine_driver_c16c =
	/*TODO*///{
	/*TODO*///  /* basic machine hardware */
	/*TODO*///	{
	/*TODO*///		{
	/*TODO*///			CPU_M7501,
	/*TODO*///			1400000,				   /*TED7360PAL_CLOCK/2, */
	/*TODO*///			c16_readmem, c16_writemem,
	/*TODO*///			0, 0,
	/*TODO*///			c16_frame_interrupt, 1,
	/*TODO*///			ted7360_raster_interrupt, TED7360_HRETRACERATE,
	/*TODO*///		},
	/*TODO*///		C1551_CPU
	/*TODO*///	},
	/*TODO*///	TED7360PAL_VRETRACERATE, 0, 	/* frames per second, vblank duration */
	/*TODO*///#ifdef CPU_SYNC
	/*TODO*///	1,
	/*TODO*///#else
	/*TODO*///	100,
	/*TODO*///#endif
	/*TODO*///	c16_init_machine,
	/*TODO*///	c16_shutdown_machine,
	/*TODO*///
	/*TODO*///  /* video hardware */
	/*TODO*///	336,							   /* screen width */
	/*TODO*///	216,							   /* screen height */
	/*TODO*///	{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
	/*TODO*///	0,								   /* graphics decode info */
	/*TODO*///	sizeof (ted7360_palette) / sizeof (ted7360_palette[0]) / 3,
	/*TODO*///	0,
	/*TODO*///	c16_init_palette,				   /* convert color prom */
	/*TODO*///	VIDEO_TYPE_RASTER,
	/*TODO*///	0,
	/*TODO*///	ted7360_vh_start,
	/*TODO*///	ted7360_vh_stop,
	/*TODO*///	ted7360_vh_screenrefresh,
	/*TODO*///
	/*TODO*///  /* sound hardware */
	/*TODO*///	0, 0, 0, 0,
	/*TODO*///	{
	/*TODO*///		{SOUND_CUSTOM, &ted7360_sound_interface},
	/*TODO*///		{SOUND_CUSTOM, &sidc16_sound_interface},
	/*TODO*///		{SOUND_DAC, &vc20tape_sound_interface}
	/*TODO*///	}
	/*TODO*///};
	
	/*TODO*///static struct MachineDriver machine_driver_c16v =
/*TODO*///	{
/*TODO*///	  /* basic machine hardware */
/*TODO*///		{
/*TODO*///			{
/*TODO*///				CPU_M7501,
/*TODO*///				1400000,				   /*TED7360PAL_CLOCK/2, */
/*TODO*///				c16_readmem, c16_writemem,
/*TODO*///				0, 0,
/*TODO*///				c16_frame_interrupt, 1,
/*TODO*///				ted7360_raster_interrupt, TED7360_HRETRACERATE,
/*TODO*///			},
/*TODO*///			VC1541_CPU
/*TODO*///		},
/*TODO*///		TED7360PAL_VRETRACERATE, 0, 	/* frames per second, vblank duration */
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		1,
/*TODO*///	#else
/*TODO*///		5000,
/*TODO*///	#endif
/*TODO*///		c16_init_machine,
/*TODO*///		c16_shutdown_machine,
/*TODO*///	
/*TODO*///	  /* video hardware */
/*TODO*///		336,							   /* screen width */
/*TODO*///		216,							   /* screen height */
/*TODO*///		{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
/*TODO*///		0,								   /* graphics decode info */
/*TODO*///		sizeof (ted7360_palette) / sizeof (ted7360_palette[0]) / 3,
/*TODO*///		0,
/*TODO*///		c16_init_palette,				   /* convert color prom */
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		0,
/*TODO*///		ted7360_vh_start,
/*TODO*///		ted7360_vh_stop,
/*TODO*///		ted7360_vh_screenrefresh,
/*TODO*///	
/*TODO*///	  /* sound hardware */
/*TODO*///		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{SOUND_CUSTOM, &ted7360_sound_interface},
/*TODO*///			{SOUND_CUSTOM, &sidc16_sound_interface},
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
/*TODO*///	};
/*TODO*///	
/*TODO*///	static struct MachineDriver machine_driver_plus4 =
/*TODO*///	{
/*TODO*///	  /* basic machine hardware */
/*TODO*///		{
/*TODO*///			{
/*TODO*///				CPU_M7501,
/*TODO*///				1200000,				   /*TED7360NTSC_CLOCK/2, */
/*TODO*///				plus4_readmem, plus4_writemem,
/*TODO*///				0, 0,
/*TODO*///				c16_frame_interrupt, 1,
/*TODO*///				ted7360_raster_interrupt, TED7360_HRETRACERATE,
/*TODO*///			},
/*TODO*///		},
/*TODO*///		TED7360NTSC_VRETRACERATE,0, /* frames per second, vblank duration */
/*TODO*///		0,
/*TODO*///		c16_init_machine,
/*TODO*///		c16_shutdown_machine,
/*TODO*///	
/*TODO*///	  /* video hardware */
/*TODO*///		336,							   /* screen width */
/*TODO*///		216,							   /* screen height */
/*TODO*///		{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
/*TODO*///		0,								   /* graphics decode info */
/*TODO*///		sizeof (ted7360_palette) / sizeof (ted7360_palette[0]) / 3,
/*TODO*///		0,
/*TODO*///		c16_init_palette,				   /* convert color prom */
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		0,
/*TODO*///		ted7360_vh_start,
/*TODO*///		ted7360_vh_stop,
/*TODO*///		ted7360_vh_screenrefresh,
/*TODO*///	
/*TODO*///	  /* sound hardware */
/*TODO*///		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{SOUND_CUSTOM, &ted7360_sound_interface},
/*TODO*///			{SOUND_CUSTOM, &sidplus4_sound_interface},
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
/*TODO*///	};
/*TODO*///	
/*TODO*///	static struct MachineDriver machine_driver_plus4c =
/*TODO*///	{
/*TODO*///	  /* basic machine hardware */
/*TODO*///		{
/*TODO*///			{
/*TODO*///				CPU_M7501,
/*TODO*///				1200000,				   /*TED7360NTSC_CLOCK/2, */
/*TODO*///				plus4_readmem, plus4_writemem,
/*TODO*///				0, 0,
/*TODO*///				c16_frame_interrupt, 1,
/*TODO*///				ted7360_raster_interrupt, TED7360_HRETRACERATE,
/*TODO*///			},
/*TODO*///			C1551_CPU
/*TODO*///		},
/*TODO*///		TED7360NTSC_VRETRACERATE,
/*TODO*///		DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		1,
/*TODO*///	#else
/*TODO*///		1000,
/*TODO*///	#endif
/*TODO*///		c16_init_machine,
/*TODO*///		c16_shutdown_machine,
/*TODO*///	
/*TODO*///	  /* video hardware */
/*TODO*///		336,							   /* screen width */
/*TODO*///		216,							   /* screen height */
/*TODO*///		{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
/*TODO*///		0,								   /* graphics decode info */
/*TODO*///		sizeof (ted7360_palette) / sizeof (ted7360_palette[0]) / 3,
/*TODO*///		0,
/*TODO*///		c16_init_palette,				   /* convert color prom */
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		0,
/*TODO*///		ted7360_vh_start,
/*TODO*///		ted7360_vh_stop,
/*TODO*///		ted7360_vh_screenrefresh,
/*TODO*///	
/*TODO*///	  /* sound hardware */
/*TODO*///		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{SOUND_CUSTOM, &ted7360_sound_interface},
/*TODO*///			{SOUND_CUSTOM, &sidplus4_sound_interface},
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
/*TODO*///	};
/*TODO*///	
/*TODO*///	static struct MachineDriver machine_driver_plus4v =
/*TODO*///	{
/*TODO*///	  /* basic machine hardware */
/*TODO*///		{
/*TODO*///			{
/*TODO*///				CPU_M7501,
/*TODO*///				1200000,				   /*TED7360NTSC_CLOCK/2, */
/*TODO*///				plus4_readmem, plus4_writemem,
/*TODO*///				0, 0,
/*TODO*///				c16_frame_interrupt, 1,
/*TODO*///				ted7360_raster_interrupt, TED7360_HRETRACERATE,
/*TODO*///			},
/*TODO*///			VC1541_CPU
/*TODO*///		},
/*TODO*///		TED7360NTSC_VRETRACERATE,
/*TODO*///		DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		1,
/*TODO*///	#else
/*TODO*///		5000,
/*TODO*///	#endif
/*TODO*///		c16_init_machine,
/*TODO*///		c16_shutdown_machine,
/*TODO*///	
/*TODO*///	  /* video hardware */
/*TODO*///		336,							   /* screen width */
/*TODO*///		216,							   /* screen height */
/*TODO*///		{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
/*TODO*///		0,								   /* graphics decode info */
/*TODO*///		sizeof (ted7360_palette) / sizeof (ted7360_palette[0]) / 3,
/*TODO*///		0,
/*TODO*///		c16_init_palette,				   /* convert color prom */
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		0,
/*TODO*///		ted7360_vh_start,
/*TODO*///		ted7360_vh_stop,
/*TODO*///		ted7360_vh_screenrefresh,
/*TODO*///	
/*TODO*///	  /* sound hardware */
/*TODO*///		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{SOUND_CUSTOM, &ted7360_sound_interface},
/*TODO*///			{SOUND_CUSTOM, &sidplus4_sound_interface},
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
/*TODO*///	};
/*TODO*///	
/*TODO*///	static struct MachineDriver machine_driver_c364 =
/*TODO*///	{
/*TODO*///	  /* basic machine hardware */
/*TODO*///		{
/*TODO*///			{
/*TODO*///				CPU_M7501,
/*TODO*///				1200000,				   /*TED7360NTSC_CLOCK/2, */
/*TODO*///				c364_readmem, c364_writemem,
/*TODO*///				0, 0,
/*TODO*///				c16_frame_interrupt, 1,
/*TODO*///				ted7360_raster_interrupt, TED7360_HRETRACERATE,
/*TODO*///			},
/*TODO*///		},
/*TODO*///		TED7360NTSC_VRETRACERATE,
/*TODO*///		DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
/*TODO*///		0,
/*TODO*///		c16_init_machine,
/*TODO*///		c16_shutdown_machine,
/*TODO*///	
/*TODO*///	  /* video hardware */
/*TODO*///		336,							   /* screen width */
/*TODO*///		216,							   /* screen height */
/*TODO*///		{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
/*TODO*///		0,								   /* graphics decode info */
/*TODO*///		sizeof (ted7360_palette) / sizeof (ted7360_palette[0]) / 3,
/*TODO*///		0,
/*TODO*///		c16_init_palette,				   /* convert color prom */
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		0,
/*TODO*///		ted7360_vh_start,
/*TODO*///		ted7360_vh_stop,
/*TODO*///		ted7360_vh_screenrefresh,
/*TODO*///	
/*TODO*///	  /* sound hardware */
/*TODO*///		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{SOUND_CUSTOM, &ted7360_sound_interface},
/*TODO*///			{SOUND_CUSTOM, &sidplus4_sound_interface},
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
/*TODO*///	};
	
	static IODevice io_c16[] =
	{
		/*TODO*///IODEVICE_CBM_QUICK,
		new IODevice(
			IO_CARTSLOT,				   /* type */
			2,				/* normal 1 *//* count */
			"bin\0rom\0",                     /* file extensions */
			IO_RESET_ALL,			/* reset if file changed */
			null,
			c16_rom_init,                   /* init */
			null,				/* exit */
			null,				/* info */
			null,				/* open */
			null,				/* close */
			null,				/* status */
			null,				/* seek */
                        null,                           /* tell */
			null,				/* input */
			null,				/* output */
			null,				/* input_chunk */
			null				/* output_chunk */
		),
		/*TODO*///IODEVICE_VC20TAPE,
		/*TODO*///IODEVICE_CBM_DRIVE,
		new IODevice(IO_END)
	};
	
	/*TODO*///static const struct IODevice io_c16c[] =
/*TODO*///	{
/*TODO*///		IODEVICE_CBM_QUICK,
/*TODO*///		{
/*TODO*///			IO_CARTSLOT,				   /* type */
/*TODO*///			2,							   /* normal 1 *//* count */
/*TODO*///			"bin\0rom\0",                  /* file extensions */
/*TODO*///			IO_RESET_ALL,				   /* reset if file changed */
/*TODO*///			0,
/*TODO*///			c16_rom_init,				   /* init */
/*TODO*///			NULL,						   /* exit */
/*TODO*///			NULL,						   /* info */
/*TODO*///			NULL,						   /* open */
/*TODO*///			NULL,						   /* close */
/*TODO*///			NULL,						   /* status */
/*TODO*///			NULL,						   /* seek */
/*TODO*///			NULL,						   /* input */
/*TODO*///			NULL,						   /* output */
/*TODO*///			NULL,						   /* input_chunk */
/*TODO*///			NULL						   /* output_chunk */
/*TODO*///		},
/*TODO*///		IODEVICE_VC20TAPE,
/*TODO*///		IODEVICE_C1551,
/*TODO*///		{IO_END}
/*TODO*///	};
/*TODO*///	
/*TODO*///	static const struct IODevice io_c16v[] =
/*TODO*///	{
/*TODO*///		IODEVICE_CBM_QUICK,
/*TODO*///		{
/*TODO*///			IO_CARTSLOT,				   /* type */
/*TODO*///			2,							   /* normal 1 *//* count */
/*TODO*///			"bin\0rom\0",                  /* file extensions */
/*TODO*///			IO_RESET_ALL,				   /* reset if file changed */
/*TODO*///			0,
/*TODO*///			c16_rom_init,				   /* init */
/*TODO*///			NULL,						   /* exit */
/*TODO*///			NULL,						   /* info */
/*TODO*///			NULL,						   /* open */
/*TODO*///			NULL,						   /* close */
/*TODO*///			NULL,						   /* status */
/*TODO*///			NULL,						   /* seek */
/*TODO*///			NULL,						   /* input */
/*TODO*///			NULL,						   /* output */
/*TODO*///			NULL,						   /* input_chunk */
/*TODO*///			NULL						   /* output_chunk */
/*TODO*///		},
/*TODO*///		IODEVICE_VC20TAPE,
/*TODO*///		IODEVICE_VC1541,
/*TODO*///		{IO_END}
/*TODO*///	};
	
/*TODO*///	#define io_c16hun		io_c16
/*TODO*///	#define io_plus4		io_c16
/*TODO*///	#define io_plus4c		io_c16c
/*TODO*///	#define io_plus4v		io_c16v
/*TODO*///	#define io_c364 		io_c16
	
/*TODO*///	#define init_c16		c16_driver_init
/*TODO*///	#define init_c16hun 	c16_driver_init
/*TODO*///	#define init_c16c		c16_driver_init
/*TODO*///	#define init_c16v		c16_driver_init
/*TODO*///	#define init_plus4		c16_driver_init
/*TODO*///	#define init_plus4c 	c16_driver_init
/*TODO*///	#define init_plus4v 	c16_driver_init
/*TODO*///	#define init_c364		c16_driver_init
	
	/*		YEAR	NAME	PARENT	MACHINE INPUT	INIT	COMPANY 								FULLNAME */
	//COMP ( 1984,	c16,	0,		c16,	c16,	c16,	"Commodore Business Machines Co.",      "Commodore 16/116/232/264 (PAL)")
        public static GameDriver driver_c16 = new GameDriver("1984", "c16", "c16.java", rom_c16, null, machine_driver_c16, input_ports_c16, null, io_c16, "Commodore Business Machines Co.", "Commodore 16/116/232/264 (PAL)");
/*TODO*///	COMP ( 1984,	c16hun, c16,	c16,	c16,	c16,	"Commodore Business Machines Co.",      "Commodore 16 Novotrade (PAL, Hungarian Character Set)")
/*TODO*///	COMPX ( 1984,	c16c,	c16,	c16c,	c16c,	c16,	"Commodore Business Machines Co.",      "Commodore 16/116/232/264 (PAL), 1551", GAME_NOT_WORKING)
/*TODO*///	COMP ( 1984,	plus4,	c16,	plus4,	plus4,	plus4,	"Commodore Business Machines Co.",      "Commodore +4 (NTSC)")
/*TODO*///	COMPX ( 1984,	plus4c, c16,	plus4c, plus4c, plus4,	"Commodore Business Machines Co.",      "Commodore +4 (NTSC), 1551", GAME_NOT_WORKING)
/*TODO*///	COMPX ( 198?,	c364,	c16,	c364,	plus4,	plus4,	"Commodore Business Machines Co.",      "Commodore 364 (Prototype)", GAME_IMPERFECT_SOUND)
/*TODO*///	// please leave the following as testdriver only
/*TODO*///	COMPX ( 1984,	c16v,	c16,	c16v,	c16v,	c16,	"Commodore Business Machines Co.",      "Commodore 16/116/232/264 (PAL), VC1541", GAME_NOT_WORKING)
/*TODO*///	COMPX ( 1984,	plus4v, c16,	plus4v, plus4v, plus4,	"Commodore Business Machines Co.",      "Commodore +4 (NTSC), VC1541", GAME_NOT_WORKING)
	
	/*TODO*///#ifdef RUNTIME_LOADER
	/*TODO*///extern void c16_runtime_loader_init(void)
	/*TODO*///{
	/*TODO*///	int i;
	/*TODO*///	for (i=0; drivers[i]; i++) {
	/*TODO*///		if ( strcmp(drivers[i]->name,"c16")==0) drivers[i]=&driver_c16;
	/*TODO*///		if ( strcmp(drivers[i]->name,"c16hun")==0) drivers[i]=&driver_c16hun;
	/*TODO*///		if ( strcmp(drivers[i]->name,"c16c")==0) drivers[i]=&driver_c16c;
	/*TODO*///		if ( strcmp(drivers[i]->name,"c16v")==0) drivers[i]=&driver_c16v;
	/*TODO*///		if ( strcmp(drivers[i]->name,"plus4")==0) drivers[i]=&driver_plus4;
	/*TODO*///		if ( strcmp(drivers[i]->name,"plus4c")==0) drivers[i]=&driver_plus4c;
	/*TODO*///		if ( strcmp(drivers[i]->name,"plus4v")==0) drivers[i]=&driver_plus4v;
	/*TODO*///		if ( strcmp(drivers[i]->name,"c364")==0) drivers[i]=&driver_c364;
	/*TODO*///	}
	/*TODO*///}
	/*TODO*///#endif
}
