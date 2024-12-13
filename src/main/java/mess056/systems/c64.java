/***************************************************************************
	commodore c64 home computer

	PeT mess@utanet.at

    documentation
     www.funet.fi
***************************************************************************/

/*
------------------------------------
max     commodore max (vic10/ultimax/vickie prototype)
c64		commodore c64 (ntsc version)
c64pal	commodore c64 (pal version)
c64gs   commodore c64 game system (ntsc version)
sx64    commodore sx64 (pal version)
------------------------------------
(preliminary version)

if the game runs to fast with the ntsc version, try the pal version!

c64
 design like the vic20
 better videochip with sprites
 famous sid6581 sound chip
 64 kbyte ram
 2nd gameport
Educator 64-1
 standard c64
 bios color bios (as in pet64 series) when delivered with green monitor
max  (vic10,ultimax,vickey prototype)
 delivered in japan only?
 (all modules should work with c64)
 cartridges neccessary
 low cost c64
 flat design
 only 4 kbyte sram
 simplier banking chip
  no portlines from cpu
 only 1 cia6526 chip
  restore key connection?
  no serial bus
  no userport
 keyboard
 tape port
 2 gameports
  lightpen (port a only) and joystick mentioned in advertisement
  paddles
 cartridge/expansion port (some signals different to c64)
 no rom on board (minibasic with kernel delivered as cartridge?)
c64gs
 game console without keyboard
 standard c64 mainboard!
 modified kernal
 basic rom
 2. cia yes
 no userport
 no cbm serial port
 no keyboard connector
 no tapeport
cbm4064/pet64/educator64-2
 build in green monitor
 other case
 differences, versions???
(sx100 sx64 like prototype with build in black/white monitor)
sx64
 movable compact (and heavy) all in one comp
 build in vc1541
 build in small color monitor
 no tape connector
dx64 prototype
 two build in vc1541 (or 2 drives driven by one vc1541 circuit)

state
-----
rasterline based video system
 no cpu holding
 imperfect scrolling support (when 40 columns or 25 lines)
 lightpen support not finished
 rasterline not finished
no sound
cia6526's look in machine/cia6526.c
keyboard
gameport a
 paddles 1,2
 joystick 1
 2 button joystick/mouse joystick emulation
 no mouse
 lightpen (not finished)
gameport b
 paddles 3,4
 joystick 2
 2 button joystick/mouse joystick emulation
 no mouse
simple tape support
 (not working, cia timing?)
serial bus
 simple disk drives
 no printer or other devices
expansion modules c64
 rom cartridges (exrom)
 ultimax rom cartridges (game)
 no other rom cartridges (bankswitching logic in it, switching exrom, game)
 no ieee488 support
 no cpm cartridge
 no speech cartridge (no circuit diagram found)
 no fm sound cartridge
 no other expansion modules
expansion modules ultimax
 ultimax rom cartridges
 no other expansion modules
no userport
 no rs232/v.24 interface
no super cpu modification
no second sid modification
quickloader

Keys
----
Some PC-Keyboards does not behave well when special two or more keys are
pressed at the same time
(with my keyboard printscreen clears the pressed pause key!)

shift-cbm switches between upper-only and normal character set
(when wrong characters on screen this can help)
run (shift-stop) loads pogram from type and starts it

Lightpen
--------
Paddle 5 x-axe
Paddle 6 y-axe

Tape
----
(DAC 1 volume in noise volume)
loading of wav, prg and prg files in zip archiv
commandline -cassette image
wav:
 8 or 16(not tested) bit, mono, 125000 Hz minimum
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
 loads file from rom directory (*.prg,*.p00) (must NOT be specified on commandline)
 or file from d64 image (here also directory LOAD"$",8 supported)
use LOAD"filename",8
or LOAD"filename",8,1 (for loading machine language programs at their address)
for loading
type RUN or the appropriate sys call to start them

several programs rely on more features
(loading other file types, writing, ...)

most games rely on starting own programs in the floppy drive
(and therefor cpu level emulation is needed)

Roms
----
.prg
.crt
.80 .90 .a0 .b0 .e0 .f0
files with boot-sign in it
  recogniced as roms

.prg files loaded at address in its first two bytes
.?0 files to address specified in extension
.crt roms to addresses in crt file

Quickloader
-----------
.prg and .p00 files supported
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
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.memoryH.*;
import static mame056.sndintrfH.*;
import static mame056.sound.mixerH.*;
import static mess056.deviceH.*;
import static mess056.messH.*;
import static mess056.includes.c64H.*;
import static mess056.includes.cbmH.*;
import static mess056.includes.vc20tapeH.*;
import static mess056.includes.cbmserbH.*;
import static mess056.includes.sid6581H.*;
import static mess056.includes.vic6567H.*;
import static mess056.machine.c64.*;
import static mess056.machine.cia6526.*;
import static mess056.machine.vc20tape.*;
import static mess056.sndhrdw.sid6581.*;
import static mess056.vidhrdw.vic6567.*;

public class c64
{
	
	public static int VERBOSE_DBG = 0;
	
	
	public static Memory_ReadAddress ultimax_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress(0x0000, 0x0001, c64_m6510_port_r),
		new Memory_ReadAddress(0x0002, 0x0fff, MRA_RAM),
		new Memory_ReadAddress(0x8000, 0x9fff, MRA_ROM),
		new Memory_ReadAddress(0xd000, 0xd3ff, vic2_port_r),
		new Memory_ReadAddress(0xd400, 0xd7ff, sid6581_0_port_r),
		new Memory_ReadAddress(0xd800, 0xdbff, MRA_RAM),		   /* colorram  */
		new Memory_ReadAddress(0xdc00, 0xdcff, cia6526_0_port_r),
		new Memory_ReadAddress(0xe000, 0xffff, MRA_ROM),		   /* ram or kernel rom */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress ultimax_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress(0x0000, 0x0001, c64_m6510_port_w, c64_memory),
		new Memory_WriteAddress(0x0002, 0x0fff, MWA_RAM),
		new Memory_WriteAddress(0x8000, 0x9fff, MWA_ROM, c64_roml),
		new Memory_WriteAddress(0xd000, 0xd3ff, vic2_port_w),
		new Memory_WriteAddress(0xd400, 0xd7ff, sid6581_0_port_w),
		new Memory_WriteAddress(0xd800, 0xdbff, c64_colorram_write, c64_colorram),
		new Memory_WriteAddress(0xdc00, 0xdcff, cia6526_0_port_w),
		new Memory_WriteAddress(0xe000, 0xffff, MWA_ROM, c64_romh),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress c64_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress(0x0000, 0x0001, c64_m6510_port_r),
                new Memory_ReadAddress(0x0002, 0x7fff, MRA_RAM),
                new Memory_ReadAddress(0x8000, 0x9fff, MRA_BANK1),	   /* ram or external roml */
                new Memory_ReadAddress(0xa000, 0xbfff, MRA_BANK3),	   /* ram or basic rom or external romh */
                new Memory_ReadAddress(0xc000, 0xcfff, MRA_RAM),
/*TODO*///
                new Memory_ReadAddress(0xd000, 0xdfff, MRA_BANK5),
	/*TODO*///#else
	/* dram */
/* or character rom */
/*TODO*///	{0xd000, 0xd3ff, vic2_port_r},
/*TODO*///	{0xd400, 0xd7ff, sid6581_0_port_r},
/*TODO*///	{0xd800, 0xdbff, MRA_RAM},		   /* colorram  */
/*TODO*///	{0xdc00, 0xdcff, cia6526_0_port_r},
/*TODO*///	{0xdd00, 0xddff, cia6526_1_port_r},
/*TODO*///	{0xde00, 0xdeff, MRA_NOP},		   /* csline expansion port */
/*TODO*///	{0xdf00, 0xdfff, MRA_NOP},		   /* csline expansion port */
/*TODO*///#endif
                new Memory_ReadAddress(0xe000, 0xffff, MRA_BANK7),	   /* ram or kernel rom or external romh */
/*TODO*///                new Memory_ReadAddress(0x10000, 0x11fff, MRA_ROM),	   /* basic at 0xa000 */
/*TODO*///                new Memory_ReadAddress(0x12000, 0x13fff, MRA_ROM),	   /* kernal at 0xe000 */
/*TODO*///                new Memory_ReadAddress(0x14000, 0x14fff, MRA_ROM),	   /* charrom at 0xd000 */
/*TODO*///                new Memory_ReadAddress(0x15000, 0x153ff, MRA_RAM),	   /* colorram at 0xd800 */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress c64_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress(0x0000, 0x0001, c64_m6510_port_w, c64_memory),
                new Memory_WriteAddress(0x0002, 0x7fff, MWA_RAM),
                new Memory_WriteAddress(0x8000, 0x9fff, MWA_BANK2),
                new Memory_WriteAddress(0xa000, 0xcfff, MWA_RAM),
/*TODO*///        #if 1
                new Memory_WriteAddress(0xd000, 0xdfff, MWA_BANK6),
/*TODO*///        #else
/*TODO*///                /* or dram memory */
/*TODO*///                {0xd000, 0xd3ff, vic2_port_w},
/*TODO*///                {0xd400, 0xd7ff, sid6581_0_port_w},
/*TODO*///                {0xd800, 0xdbff, c64_colorram_write},
/*TODO*///                {0xdc00, 0xdcff, cia6526_0_port_w},
/*TODO*///                {0xdd00, 0xddff, cia6526_1_port_w},
/*TODO*///                {0xde00, 0xdeff, MWA_NOP},		   /* csline expansion port */
/*TODO*///                {0xdf00, 0xdfff, MWA_NOP},		   /* csline expansion port */
/*TODO*///        #endif
                new Memory_WriteAddress(0xe000, 0xffff, MWA_BANK8),
/*TODO*///                new Memory_WriteAddress(0x10000, 0x11fff, MWA_ROM, c64_basic),	/* basic at 0xa000 */
/*TODO*///                new Memory_WriteAddress(0x12000, 0x13fff, MWA_ROM, c64_kernal),	/* kernal at 0xe000 */
/*TODO*///                new Memory_WriteAddress(0x14000, 0x14fff, MWA_ROM, c64_chargen),	/* charrom at 0xd000 */
/*TODO*///                new Memory_WriteAddress(0x15000, 0x153ff, MWA_RAM, c64_colorram),		/* colorram at 0xd800 */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static void DIPS_HELPER(int bit, String name, int keycode) {
	   PORT_BITX(bit, IP_ACTIVE_HIGH, IPT_KEYBOARD, name, keycode, IP_JOY_NONE);
        }
	
	public static void C64_KEYBOARD() {
		PORT_START();
		DIPS_HELPER( 0x8000, "Arrow-Left", KEYCODE_TILDE);
		DIPS_HELPER( 0x4000, "1 !   BLK   ORNG", KEYCODE_1);
		DIPS_HELPER( 0x2000, "2 \"   WHT   BRN", KEYCODE_2);
		DIPS_HELPER( 0x1000, "3 #   RED   L RED", KEYCODE_3);
		DIPS_HELPER( 0x0800, "4 $   CYN   D GREY", KEYCODE_4);
		DIPS_HELPER( 0x0400, "5 %   PUR   GREY", KEYCODE_5);
		DIPS_HELPER( 0x0200, "6 &   GRN   L GRN", KEYCODE_6);
		DIPS_HELPER( 0x0100, "7 '   BLU   L BLU", KEYCODE_7);
		DIPS_HELPER( 0x0080, "8 (   YEL   L GREY", KEYCODE_8);
		DIPS_HELPER( 0x0040, "9 )   RVS-ON", KEYCODE_9);
		DIPS_HELPER( 0x0020, "0     RVS-OFF", KEYCODE_0);
		DIPS_HELPER( 0x0010, "+", KEYCODE_PLUS_PAD);
		DIPS_HELPER( 0x0008, "-", KEYCODE_MINUS_PAD);
		DIPS_HELPER( 0x0004, "Pound", KEYCODE_MINUS);
		DIPS_HELPER( 0x0002, "HOME CLR", KEYCODE_EQUALS);
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
	    /*TODO*///DIPS_HELPER( 0x0008, "*", KEYCODE_ASTERISK);
                DIPS_HELPER( 0x0008, "*", KEYCODE_M);
		DIPS_HELPER( 0x0004, "Arrow-Up Pi",KEYCODE_CLOSEBRACE);
	    DIPS_HELPER( 0x0002, "RESTORE", KEYCODE_PRTSCR);
		DIPS_HELPER( 0x0001, "STOP RUN", KEYCODE_TAB);
		PORT_START();
		PORT_BITX( 0x8000, IP_ACTIVE_HIGH, IPT_DIPSWITCH_NAME|IPF_TOGGLE,"SHIFT-LOCK (switch)", KEYCODE_CAPSLOCK, IP_JOY_NONE);
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(  0x8000, DEF_STR( "On") );
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
		DIPS_HELPER( 0x0008, "=", KEYCODE_BACKSLASH);
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
		DIPS_HELPER( 0x0100, ", <", KEYCODE_COMMA);
		DIPS_HELPER( 0x0080, ". >", KEYCODE_STOP);
		DIPS_HELPER( 0x0040, "/ ?", KEYCODE_SLASH);
		DIPS_HELPER( 0x0020, "Right-Shift", KEYCODE_RSHIFT);
		DIPS_HELPER( 0x0010, "CRSR-DOWN UP", KEYCODE_2_PAD);
		DIPS_HELPER( 0x0008, "CRSR-RIGHT LEFT", KEYCODE_6_PAD);
		DIPS_HELPER( 0x0004, "Space", KEYCODE_SPACE);
		DIPS_HELPER( 0x0002, "f1 f2", KEYCODE_F1);
		DIPS_HELPER( 0x0001, "f3 f4", KEYCODE_F2);
		PORT_START();
		DIPS_HELPER( 0x8000, "f5 f6", KEYCODE_F3);
		DIPS_HELPER( 0x4000, "f7 f8", KEYCODE_F4);
		DIPS_HELPER( 0x2000, "(Right-Shift Cursor-Down)Special CRSR Up", 
					 KEYCODE_8_PAD);
		DIPS_HELPER( 0x1000, "(Right-Shift Cursor-Right)Special CRSR Left", 
					 KEYCODE_4_PAD);
        }
	
	public static void VIC64S_KEYBOARD() {
		PORT_START();
		DIPS_HELPER( 0x8000, "Arrow-Left", KEYCODE_TILDE);
		DIPS_HELPER( 0x4000, "1 !   BLK   ORNG", KEYCODE_1);
		DIPS_HELPER( 0x2000, "2 \"   WHT   BRN", KEYCODE_2);
		DIPS_HELPER( 0x1000, "3 #   RED   L RED", KEYCODE_3);
		DIPS_HELPER( 0x0800, "4 $   CYN   D GREY", KEYCODE_4);
		DIPS_HELPER( 0x0400, "5 %   PUR   GREY", KEYCODE_5);
		DIPS_HELPER( 0x0200, "6 &   GRN   L GRN", KEYCODE_6);
		DIPS_HELPER( 0x0100, "7 '   BLU   L BLU", KEYCODE_7);
		DIPS_HELPER( 0x0080, "8 (   YEL   L GREY", KEYCODE_8);
		DIPS_HELPER( 0x0040, "9 )   RVS-ON", KEYCODE_9);
		DIPS_HELPER( 0x0020, "0     RVS-OFF", KEYCODE_0);
		DIPS_HELPER( 0x0010, "-", KEYCODE_PLUS_PAD);
		DIPS_HELPER( 0x0008, "=", KEYCODE_MINUS_PAD);
		DIPS_HELPER( 0x0004, ": *", KEYCODE_MINUS);
		DIPS_HELPER( 0x0002, "HOME CLR", KEYCODE_EQUALS);
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
		DIPS_HELPER( 0x0010, "Overcircle-A", KEYCODE_OPENBRACE);
	    DIPS_HELPER( 0x0008, "At", KEYCODE_ASTERISK);
		DIPS_HELPER( 0x0004, "Arrow-Up Pi",KEYCODE_CLOSEBRACE);
	    DIPS_HELPER( 0x0002, "RESTORE", KEYCODE_PRTSCR);
		DIPS_HELPER( 0x0001, "STOP RUN", KEYCODE_TAB);
		PORT_START();
		PORT_BITX( 0x8000, IP_ACTIVE_HIGH, IPT_DIPSWITCH_NAME|IPF_TOGGLE,
			     "SHIFT-LOCK (switch)", KEYCODE_CAPSLOCK, IP_JOY_NONE);
		PORT_DIPSETTING(  0, "Off" );
		PORT_DIPSETTING(  0x8000, "On" );
		DIPS_HELPER( 0x4000, "A", KEYCODE_A);
		DIPS_HELPER( 0x2000, "S", KEYCODE_S);
		DIPS_HELPER( 0x1000, "D", KEYCODE_D);
		DIPS_HELPER( 0x0800, "F", KEYCODE_F);
		DIPS_HELPER( 0x0400, "G", KEYCODE_G);
		DIPS_HELPER( 0x0200, "H", KEYCODE_H);
		DIPS_HELPER( 0x0100, "J", KEYCODE_J);
		DIPS_HELPER( 0x0080, "K", KEYCODE_K);
		DIPS_HELPER( 0x0040, "L", KEYCODE_L);
		DIPS_HELPER( 0x0020, "Diaresis-O", KEYCODE_COLON);
		DIPS_HELPER( 0x0010, "Diaresis-A", KEYCODE_QUOTE);
		DIPS_HELPER( 0x0008, "; +", KEYCODE_BACKSLASH);
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
		DIPS_HELPER( 0x0100, ", <", KEYCODE_COMMA);
		DIPS_HELPER( 0x0080, ". >", KEYCODE_STOP);
		DIPS_HELPER( 0x0040, "/ ?", KEYCODE_SLASH);
		DIPS_HELPER( 0x0020, "Right-Shift", KEYCODE_RSHIFT);
		DIPS_HELPER( 0x0010, "CRSR-DOWN UP", KEYCODE_2_PAD);
		DIPS_HELPER( 0x0008, "CRSR-RIGHT LEFT", KEYCODE_6_PAD);
		DIPS_HELPER( 0x0004, "Space", KEYCODE_SPACE);
		DIPS_HELPER( 0x0002, "f1 f2", KEYCODE_F1);
		DIPS_HELPER( 0x0001, "f3 f4", KEYCODE_F2);
		PORT_START();
		DIPS_HELPER( 0x8000, "f5 f6", KEYCODE_F3);
		DIPS_HELPER( 0x4000, "f7 f8", KEYCODE_F4);
		DIPS_HELPER( 0x2000, "(Right-Shift Cursor-Down)Special CRSR Up",
					 KEYCODE_8_PAD);
		DIPS_HELPER( 0x1000, "(Right-Shift Cursor-Right)Special CRSR Left",
					 KEYCODE_4_PAD);
        };
	
	static InputPortPtr input_ports_ultimax = new InputPortPtr(){ public void handler() {
		C64_DIPS();
		PORT_START();
		DIPS_HELPER( 0x8000, "Quickload", KEYCODE_F8);
		PORT_DIPNAME   ( 0x4000, 0x4000, "Tape Drive/Device 1");
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(0x4000, DEF_STR( "On") );
		PORT_DIPNAME   ( 0x2000, 0x00, " Tape Sound");
		PORT_DIPSETTING(  0, DEF_STR( "Off") );
		PORT_DIPSETTING(0x2000, DEF_STR( "On") );
		DIPS_HELPER( 0x1000, "Tape Drive Play",       KEYCODE_F5);
		DIPS_HELPER( 0x0800, "Tape Drive Record",     KEYCODE_F6);
		DIPS_HELPER( 0x0400, "Tape Drive Stop",       KEYCODE_F7);
		PORT_DIPNAME   ( 0x80, 0x00, "Sid Chip Type");
		PORT_DIPSETTING(  0, "MOS6581" );
		PORT_DIPSETTING(0x80, "MOS8580" );
		 PORT_BIT (0x1c, 0x4, IPT_UNUSED);   /* only ultimax cartridges */;
		 PORT_BIT (0x2, 0x0, IPT_UNUSED);	   /* no serial bus */;
		 PORT_BIT (0x1, 0x0, IPT_UNUSED);
		 C64_KEYBOARD();
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_c64gs = new InputPortPtr(){ public void handler() {
		 C64_DIPS();
		 PORT_START();
		 PORT_BIT (0xff00, 0x0, IPT_UNUSED);
		PORT_DIPNAME   ( 0x80, 0x00, "Sid Chip Type");
		PORT_DIPSETTING(  0, "MOS6581" );
		PORT_DIPSETTING(0x80, "MOS8580" );
		 PORT_DIPNAME (0x1c, 0x00, "Cartridge Type");
		 PORT_DIPSETTING (0, "Automatic");
		 PORT_DIPSETTING (4, "Ultimax (GAME)");
		 PORT_DIPSETTING (8, "C64 (EXROM)");
	/*TODO*///#ifdef PET_TEST_CODE
	/*TODO*///	 PORT_DIPSETTING (0x10, "CBM Supergames");
	/*TODO*///	 PORT_DIPSETTING (0x14, "Ocean Robocop2");
	/*TODO*///#endif
		 PORT_BIT (0x2, 0x0, IPT_UNUSED);	   /* no serial bus */
		 PORT_BIT (0x1, 0x0, IPT_UNUSED);
		 PORT_START(); /* no keyboard */
		 PORT_BIT (0xffff, 0x0, IPT_UNUSED);
		 PORT_START();
		 PORT_BIT (0xffff, 0x0, IPT_UNUSED);
		 PORT_START();
		 PORT_BIT (0xffff, 0x0, IPT_UNUSED);
		 PORT_START();
		 PORT_BIT (0xffff, 0x0, IPT_UNUSED);
		 PORT_START();
		 PORT_BIT (0xf000, 0x0, IPT_UNUSED);
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_c64 = new InputPortPtr(){ public void handler() {
		 C64_DIPS();
		 PORT_START();
		 DIPS_HELPER( 0x8000, "Quickload", KEYCODE_F8);
		 PORT_DIPNAME   ( 0x4000, 0x4000, "Tape Drive/Device 1");
		 PORT_DIPSETTING(  0, DEF_STR( "Off") );
		 PORT_DIPSETTING(0x4000, DEF_STR( "On") );
		 PORT_DIPNAME   ( 0x2000, 0x00, " Tape Sound");
		 PORT_DIPSETTING(  0, DEF_STR( "Off") );
		 PORT_DIPSETTING(0x2000, DEF_STR( "On") );
		 DIPS_HELPER( 0x1000, "Tape Drive Play",       KEYCODE_F5);
		 DIPS_HELPER( 0x0800, "Tape Drive Record",     KEYCODE_F6);
		 DIPS_HELPER( 0x0400, "Tape Drive Stop",       KEYCODE_F7);
		PORT_DIPNAME   ( 0x80, 0x00, "Sid Chip Type");
		PORT_DIPSETTING(  0, "MOS6581" );
		PORT_DIPSETTING(0x80, "MOS8580" );
		 PORT_DIPNAME (0x1c, 0x00, "Cartridge Type");
		 PORT_DIPSETTING (0, "Automatic");
		 PORT_DIPSETTING (4, "Ultimax (GAME)");
		 PORT_DIPSETTING (8, "C64 (EXROM)");
	/*TODO*///#ifdef PET_TEST_CODE
	/*TODO*///	 PORT_DIPSETTING (0x10, "CBM Supergames");
	/*TODO*///	 PORT_DIPSETTING (0x14, "Ocean Robocop2");
	/*TODO*///#endif
		 PORT_DIPNAME (0x02, 0x02, "Serial Bus/Device 8");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (2, "VC1541 Floppy Drive");
		 PORT_DIPNAME (0x01, 0x01, "Serial Bus/Device 9");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (1, "VC1541 Floppy Drive");
	     C64_KEYBOARD();
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_vic64s = new InputPortPtr(){ public void handler() {
		 C64_DIPS();
		 PORT_START();
		 DIPS_HELPER( 0x8000, "Quickload", KEYCODE_F8);
		 PORT_DIPNAME   ( 0x4000, 0x4000, "Tape Drive/Device 1");
		 PORT_DIPSETTING(  0, DEF_STR( "Off") );
		 PORT_DIPSETTING(0x4000, DEF_STR( "On") );
		 PORT_DIPNAME   ( 0x2000, 0x00, " Tape Sound");
		 PORT_DIPSETTING(  0, DEF_STR( "Off") );
		 PORT_DIPSETTING(0x2000, DEF_STR( "On") );
		 DIPS_HELPER( 0x1000, "Tape Drive Play",       KEYCODE_F5);
		 DIPS_HELPER( 0x0800, "Tape Drive Record",     KEYCODE_F6);
		 DIPS_HELPER( 0x0400, "Tape Drive Stop",       KEYCODE_F7);
		PORT_DIPNAME   ( 0x80, 0x00, "Sid Chip Type");
		PORT_DIPSETTING(  0, "MOS6581" );
		PORT_DIPSETTING(0x80, "MOS8580" );
		 PORT_DIPNAME (0x1c, 0x00, "Cartridge Type");
		 PORT_DIPSETTING (0, "Automatic");
		 PORT_DIPSETTING (4, "Ultimax (GAME)");
		 PORT_DIPSETTING (8, "C64 (EXROM)");
	/*TODO*///#ifdef PET_TEST_CODE
	/*TODO*///	 PORT_DIPSETTING (0x10, "CBM Supergames");
	/*TODO*///	 PORT_DIPSETTING (0x14, "Ocean Robocop2");
	/*TODO*///#endif
		 PORT_DIPNAME (0x02, 0x02, "Serial Bus/Device 8");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (2, "VC1541 Floppy Drive");
		 PORT_DIPNAME (0x01, 0x01, "Serial Bus/Device 9");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (1, "VC1541 Floppy Drive");
	     VIC64S_KEYBOARD();
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_sx64 = new InputPortPtr(){ public void handler() {
	     C64_DIPS();
		 PORT_START();
		 DIPS_HELPER( 0x8000, "Quickload", KEYCODE_F8);
		 PORT_BIT (0x7f00, 0x0, IPT_UNUSED);/* no tape */
		PORT_DIPNAME   ( 0x80, 0x00, "Sid Chip Type");
		PORT_DIPSETTING(  0, "MOS6581" );
		PORT_DIPSETTING(0x80, "MOS8580" );
		 PORT_DIPNAME (0x1c, 0x00, "Cartridge Type");
		 PORT_DIPSETTING (0, "Automatic");
		 PORT_DIPSETTING (4, "Ultimax (GAME)");
		 PORT_DIPSETTING (8, "C64 (EXROM)");
	/*TODO*///#ifdef PET_TEST_CODE
	/*TODO*///	 PORT_DIPSETTING (0x10, "CBM Supergames");
	/*TODO*///	 PORT_DIPSETTING (0x14, "Ocean Robocop2");
	/*TODO*///#endif
		 /* 1 vc1541 build in, device number selectable 8,9,10,11 */
		 PORT_DIPNAME (0x02, 0x02, "Serial Bus/Device 8");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (2, "VC1541 Floppy Drive");
		 PORT_DIPNAME (0x01, 0x01, "Serial Bus/Device 9");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (1, "VC1541 Floppy Drive");
	     C64_KEYBOARD();
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_vip64 = new InputPortPtr(){ public void handler() {
		 C64_DIPS();
		 PORT_START();
		 DIPS_HELPER( 0x8000, "Quickload", KEYCODE_F8);
		 PORT_BIT (0x7f00, 0x0, IPT_UNUSED);/* no tape */
		PORT_DIPNAME   ( 0x80, 0x00, "Sid Chip Type");
		PORT_DIPSETTING(  0, "MOS6581" );
		PORT_DIPSETTING(0x80, "MOS8580" );
		 PORT_DIPNAME (0x1c, 0x00, "Cartridge Type");
		 PORT_DIPSETTING (0, "Automatic");
		 PORT_DIPSETTING (4, "Ultimax (GAME)");
		 PORT_DIPSETTING (8, "C64 (EXROM)");
	/*TODO*///#ifdef PET_TEST_CODE
	/*TODO*///	 PORT_DIPSETTING (0x10, "CBM Supergames");
	/*TODO*///	 PORT_DIPSETTING (0x14, "Ocean Robocop2");
	/*TODO*///#endif
		 /* 1 vc1541 build in, device number selectable 8,9,10,11 */
		 PORT_DIPNAME (0x02, 0x02, "Serial Bus/Device 8");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (2, "VC1541 Floppy Drive");
		 PORT_DIPNAME (0x01, 0x01, "Serial Bus/Device 9");
		 PORT_DIPSETTING (0, "None");
		 PORT_DIPSETTING (1, "VC1541 Floppy Drive");
		 VIC64S_KEYBOARD();
	INPUT_PORTS_END(); }}; 
	
	public static VhConvertColorPromPtr c64_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
                memcpy (sys_palette, vic2_palette, vic2_palette.length);
            }
        };
	
	public static VhConvertColorPromPtr pet64_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
		int i;
		memcpy (sys_palette, vic2_palette, vic2_palette.length);
		for (i=0; i<16; i++){
			sys_palette[i*3]=sys_palette[i*3+2]=0;
                }
            }
        };
	
	static RomLoadPtr rom_ultimax = new RomLoadPtr() { public void handler() {
                ROM_REGION (0x10000, REGION_CPU1, 0);
        ROM_END(); }};	 
	
	static RomLoadPtr rom_c64gs = new RomLoadPtr() { public void handler() {
		ROM_REGION (0x19400, REGION_CPU1, 0);
		/* standard basic, modified kernel */
		ROM_LOAD ("390852.01", 0x10000, 0x4000, 0xb0a9c2da);
		ROM_LOAD ("901225-01.u5", 0x14000, 0x1000, 0xec4272ee);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_c64 = new RomLoadPtr() { public void handler() {
		ROM_REGION (0x19400, REGION_CPU1, 0);
	ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
	ROM_LOAD( "901227.03",   0x12000, 0x2000, 0xdbe3e7c7 );
	ROM_LOAD ("901225.01", 0x14000, 0x1000, 0xec4272ee);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_c64pal = new RomLoadPtr() { public void handler() {
		ROM_REGION (0x15400, REGION_CPU1, 0);
                //ROM_LOAD ("390852.01", 0x10000, 0x4000, 0xb0a9c2da);
		//ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
                ROM_LOAD ("basic.rom", 0x10000, 0x2000, 0xf833d117);
		//ROM_LOAD( "901227.03",   0x12000, 0x2000, 0xdbe3e7c7 );
		ROM_LOAD( "kernal_1.rom",   0x12000, 0x2000, 0xdbe3e7c7 );
		//ROM_LOAD ("901225-01.u5", 0x14000, 0x1000, 0xec4272ee);
                ROM_LOAD ("char.rom", 0x14000, 0x1000, 0xec4272ee);
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vic64s = new RomLoadPtr() { public void handler() {
		ROM_REGION (0x19400, REGION_CPU1, 0);
		ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
		ROM_LOAD( "kernel.swe",   0x12000, 0x2000, 0xf10c2c25 );
		ROM_LOAD ("charswe.bin", 0x14000, 0x1000, 0xbee9b3fd);
	ROM_END(); }}; 
	
/*TODO*///	static RomLoadPtr rom_sx64 = new RomLoadPtr() { public void handler() {
/*TODO*///		ROM_REGION (0x19400, REGION_CPU1, 0);
/*TODO*///		ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
/*TODO*///		ROM_LOAD( "251104.04",     0x12000, 0x2000, 0x2c5965d4 );
/*TODO*///		ROM_LOAD ("901225.01", 0x14000, 0x1000, 0xec4272ee);
/*TODO*///		VC1541_ROM (REGION_CPU2)
/*TODO*///	ROM_END(); }}; 
	
/*TODO*///	static RomLoadPtr rom_dx64 = new RomLoadPtr() { public void handler() {
/*TODO*///		ROM_REGION (0x19400, REGION_CPU1, 0);
/*TODO*///	    ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
/*TODO*///	    ROM_LOAD( "dx64kern.bin",     0x12000, 0x2000, 0x58065128 );
/*TODO*///	    // vc1541 roms were not included in submission
/*TODO*///	    VC1541_ROM (REGION_CPU2)
/*TODO*///	//    VC1541_ROM (REGION_CPU3)
/*TODO*///	ROM_END(); }}; 
	
/*TODO*///	static RomLoadPtr rom_vip64 = new RomLoadPtr() { public void handler() {
/*TODO*///		ROM_REGION (0x19400, REGION_CPU1, 0);
/*TODO*///		ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
/*TODO*///		ROM_LOAD( "kernelsx.swe",   0x12000, 0x2000, 0x7858d3d7 );
/*TODO*///		ROM_LOAD ("charswe.bin", 0x14000, 0x1000, 0xbee9b3fd);
/*TODO*///		VC1541_ROM (REGION_CPU2);
/*TODO*///	ROM_END(); }}; 
	
	static RomLoadPtr rom_pet64 = new RomLoadPtr() { public void handler() {
		ROM_REGION (0x19400, REGION_CPU1, 0);
		ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
		ROM_LOAD( "901246.01", 0x12000, 0x2000, 0x789c8cc5);
		ROM_LOAD ("901225.01", 0x14000, 0x1000, 0xec4272ee);
	ROM_END(); }}; 
	
	/*TODO*///#if 0
	/*TODO*///ROM_START (flash8)
	/*TODO*///	ROM_REGION (0x1009400, REGION_CPU1, 0);
	/*TODO*///#if 1
	/*TODO*///    ROM_LOAD ("flash8", 0x010000, 0x002000, 0x3c4fb703);// basic
	/*TODO*///    ROM_CONTINUE( 0x014000, 0x001000);// empty
	/*TODO*///    ROM_CONTINUE( 0x014000, 0x001000);// characterset
	/*TODO*///    ROM_CONTINUE( 0x012000, 0x002000);// c64 mode kernel
	/*TODO*///    ROM_CONTINUE( 0x015000, 0x002000);// kernel
	/*TODO*///#else
	/*TODO*///	ROM_LOAD ("flash8", 0x012000-0x6000, 0x008000, 0x3c4fb703);
	/*TODO*///#endif
	/*TODO*///ROM_END(); }}; 
	/*TODO*///#endif
	
	/*TODO*///#if 0
	/*TODO*///     /* character rom */
	/*TODO*///	 ROM_LOAD ("901225.01", 0x14000, 0x1000, 0xec4272ee);
	/*TODO*///	 ROM_LOAD ("charswe.bin", 0x14000, 0x1000, 0xbee9b3fd);
	/*TODO*///
	/*TODO*///	/* basic */
	/*TODO*///	 ROM_LOAD ("901226.01", 0x10000, 0x2000, 0xf833d117);
	/*TODO*///
	/*TODO*////* in c16 and some other commodore machines:
	/*TODO*///   cbm version in kernel at 0xff80 (offset 0x3f80)
	/*TODO*///   0x80 means pal version */
	/*TODO*///
	/*TODO*///	 /* scrap */
	/*TODO*///     /* modified for alec 64, not booting */
	/*TODO*///	 ROM_LOAD( "alec64.e0",   0x12000, 0x2000, 0x2b1b7381 );
	/*TODO*///     /* unique copyright, else speeddos? */
	/*TODO*///	 ROM_LOAD( "a.e0", 0x12000, 0x2000, 0xb8f49365 );
	/*TODO*///	 /* ? */
	/*TODO*///	 ROM_LOAD( "kernelx.e0",  0x12000, 0x2000, 0xbeed6d49 );
	/*TODO*///	 ROM_LOAD( "kernelx2.e0",  0x12000, 0x2000, 0xcfb58230 );
	/*TODO*///	 /* basic x 2 */
	/*TODO*///	 ROM_LOAD( "frodo.e0",    0x12000, 0x2000, 0x6ec94629 );
	/*TODO*///
	/*TODO*///     /* commodore versions */
	/*TODO*///	 /* 901227-01 */
	/*TODO*///	 ROM_LOAD( "901227.01",  0x12000, 0x2000, 0xdce782fa );
	/*TODO*///     /* 901227-02 */
	/*TODO*///	 ROM_LOAD( "901227.02", 0x12000, 0x2000, 0xa5c687b3 );
	/*TODO*///     /* 901227-03 */
	/*TODO*///	 ROM_LOAD( "901227.03",   0x12000, 0x2000, 0xdbe3e7c7 );
	/*TODO*///	 /* 901227-03? swedish  */
	/*TODO*///	 ROM_LOAD( "kernel.swe",   0x12000, 0x2000, 0xf10c2c25 );
	/*TODO*///	 /* c64c 901225-01 + 901227-03 */
	/*TODO*///	 ROM_LOAD ("251913.01", 0x10000, 0x4000, 0x0010ec31);
	/*TODO*///     /* c64gs 901225-01 with other fillbyte, modified kernel */
	/*TODO*///	 ROM_LOAD ("390852.01", 0x10000, 0x4000, 0xb0a9c2da);
	/*TODO*///	 /* sx64 */
	/*TODO*///	 ROM_LOAD( "251104.04",     0x12000, 0x2000, 0x2c5965d4 );
	/*TODO*///     /* 251104.04? swedish */
	/*TODO*///	 ROM_LOAD( "kernel.swe",   0x12000, 0x2000, 0x7858d3d7 );
	/*TODO*///	 /* 4064, Pet64, Educator 64 */
	/*TODO*///	 ROM_LOAD( "901246.01",     0x12000, 0x2000, 0x789c8cc5 );
	/*TODO*///
	/*TODO*///	 /* few differences to above versions */
	/*TODO*///	 ROM_LOAD( "901227.02b",  0x12000, 0x2000, 0xf80eb87b );
	/*TODO*///	 ROM_LOAD( "901227.03b",  0x12000, 0x2000, 0x8e5c500d );
	/*TODO*///	 ROM_LOAD( "901227.03c",  0x12000, 0x2000, 0xc13310c2 );
	/*TODO*///
	/*TODO*///     /* 64er system v1
	/*TODO*///        ieee interface extension for c64 and vc1541!? */
	/*TODO*///     ROM_LOAD( "64ersys1.e0", 0x12000, 0x2000, 0x97d9a4df );
	/*TODO*///	 /* 64er system v3 */
	/*TODO*///	 ROM_LOAD( "64ersys3.e0", 0x12000, 0x2000, 0x5096b3bd );
	/*TODO*///
	/*TODO*///	 /* exos v3 */
	/*TODO*///	 ROM_LOAD( "exosv3.e0",   0x12000, 0x2000, 0x4e54d020 );
	/*TODO*///     /* 2 bytes different */
	/*TODO*///	 ROM_LOAD( "exosv3.e0",   0x12000, 0x2000, 0x26f3339e );
	/*TODO*///
	/*TODO*///	 /* jiffydos v6.01 by cmd */
	/*TODO*///	 ROM_LOAD( "jiffy.e0",    0x12000, 0x2000, 0x2f79984c );
	/*TODO*///
	/*TODO*///	 /* dolphin with dolphin vc1541 */
	/*TODO*///	 ROM_LOAD( "mager.e0",    0x12000, 0x2000, 0xc9bb21bc );
	/*TODO*///	 ROM_LOAD( "dos20.e0",    0x12000, 0x2000, 0xffaeb9bc );
	/*TODO*///
	/*TODO*///	 /* speeddos plus
	/*TODO*///		parallel interface on userport to modified vc1541 !? */
	/*TODO*///	 ROM_LOAD( "speeddos.e0", 0x12000, 0x2000, 0x8438e77b );
	/*TODO*///	 /* speeddos plus + */
	/*TODO*///	 ROM_LOAD( "speeddos.e0", 0x12000, 0x2000, 0x10aee0ae );
	/*TODO*///	 /* speeddos plus and 80 column text */
	/*TODO*///	 ROM_LOAD( "rom80.e0",    0x12000, 0x2000, 0xe801dadc );
	/*TODO*///#endif
	
	/*TODO*///static SID6581_interface ultimax_sound_interface =
/*TODO*///	{
/*TODO*///		{
/*TODO*///			sid6581_custom_start,
/*TODO*///			sid6581_custom_stop,
/*TODO*///			sid6581_custom_update
/*TODO*///		},
/*TODO*///		1,
/*TODO*///		{
/*TODO*///			{
/*TODO*///				MIXER(50, MIXER_PAN_CENTER),
/*TODO*///				MOS6581,
/*TODO*///				1000000,
/*TODO*///				c64_paddle_read
/*TODO*///			}
/*TODO*///		}
/*TODO*///	};
/*TODO*///	
	static SID6581_interface pal_sound_interface = new SID6581_interface(
                //new CustomSound_interface(
			sid6581_custom_start,
			sid6581_custom_stop,
			sid6581_custom_update,
		//),
		1,
                new _chips[]
                {
                    new _chips(MIXER(50, MIXER_PAN_CENTER),
				MOS6581,
				VIC6569_CLOCK,
				c64_paddle_read
                    )
		}
                
	);
/*TODO*///	
        static SID6581_interface ntsc_sound_interface = new SID6581_interface(
                //new CustomSound_interface(
			sid6581_custom_start,
			sid6581_custom_stop,
			sid6581_custom_update,
		//),
		1,
                new _chips[]
                {
                    new _chips(MIXER(50, MIXER_PAN_CENTER),
				MOS6581,
				VIC6567_CLOCK,
				c64_paddle_read
                    )
		}
                
	);
	
	static MachineDriver machine_driver_ultimax = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6510,
                                1000000, /*! */
				ultimax_readmem, ultimax_writemem,
				null, null,
				c64_frame_interrupt, 1,
				vic2_raster_irq, VIC2_HRETRACERATE
                            )
		},
		
		VIC6567_VRETRACERATE, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		0,
		c64_init_machine,
		c64_shutdown_machine,
	
	  /* video hardware */
		336,							   /* screen width */
		216,							   /* screen height */
		new rectangle(0, 336 - 1, 0, 216 - 1),		   /* visible_area */
		null,								   /* graphics decode info */
		vic2_palette.length / 3,
		0,
		c64_init_palette,				   /* convert color prom */
		VIDEO_TYPE_RASTER,
		null,
		vic2_vh_start,
		vic2_vh_stop,
		vic2_vh_screenrefresh,
	
	  /* sound hardware */
		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{ SOUND_CUSTOM, &ultimax_sound_interface },
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
                
                null
	);
	
	
	static MachineDriver machine_driver_c64 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6510,
                                VIC6567_CLOCK,
				c64_readmem, c64_writemem,
				null, null,
				c64_frame_interrupt, 1,
				vic2_raster_irq, VIC2_HRETRACERATE
                            )
		},
		VIC6567_VRETRACERATE, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		0,
		c64_init_machine,
		c64_shutdown_machine,

	  /* video hardware */
		336,							   /* screen width */
		216,							   /* screen height */
		new rectangle(0, 336 - 1, 0, 216 - 1),                      /* visible_area */
		null,								   /* graphics decode info */
		vic2_palette.length / 3,
		0,
		c64_init_palette,				   /* convert color prom */
		VIDEO_TYPE_RASTER,
		null,
		vic2_vh_start,
		vic2_vh_stop,
		vic2_vh_screenrefresh,
	
	  /* sound hardware */
		0, 0, 0, 0,
                new MachineSound[]{
                    new MachineSound( SOUND_CUSTOM, ntsc_sound_interface ),
                    new MachineSound(SOUND_DAC, vc20tape_sound_interface)
		}
                
	);
	
	static MachineDriver machine_driver_pet64 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6510,
				VIC6567_CLOCK,
				c64_readmem, c64_writemem,
				null, null,
				c64_frame_interrupt, 1,
				vic2_raster_irq, VIC2_HRETRACERATE
                        )
		},
		VIC6567_VRETRACERATE, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		0,
		c64_init_machine,
		c64_shutdown_machine,
	
	  /* video hardware */
		336,							   /* screen width */
		216,							   /* screen height */
		new rectangle(0, 336 - 1, 0, 216 - 1),		   /* visible_area */
		null,								   /* graphics decode info */
		vic2_palette.length / 3,
		0,
		pet64_init_palette,				   /* convert color prom */
		VIDEO_TYPE_RASTER,
		null,
		vic2_vh_start,
		vic2_vh_stop,
		vic2_vh_screenrefresh,
	
	  /* sound hardware */
		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{ SOUND_CUSTOM, &ntsc_sound_interface },
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
                
                null
	);
	
	static MachineDriver machine_driver_c64pal = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6510,
                                VIC6569_CLOCK,
				c64_readmem, c64_writemem,
				null, null,
				c64_frame_interrupt, 1,
				vic2_raster_irq, VIC2_HRETRACERATE
			)
		},
		VIC6569_VRETRACERATE,
		DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		0,
		c64_init_machine,
		c64_shutdown_machine,
	
	  /* video hardware */
		336,							   /* screen width */
		216,							   /* screen height */
		new rectangle(0, 336 - 1, 0, 216 - 1),                      /* visible_area */
		null,								   /* graphics decode info */
		vic2_palette.length / 3,
		0,
		c64_init_palette,				   /* convert color prom */
		VIDEO_TYPE_RASTER,
		null,
		vic2_vh_start,
		vic2_vh_stop,
		vic2_vh_screenrefresh,
	
	  /* sound hardware */
		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{ SOUND_CUSTOM, &pal_sound_interface },
/*TODO*///			{SOUND_DAC, &vc20tape_sound_interface}
/*TODO*///		}
                   null
	);
	
	static MachineDriver machine_driver_c64gs = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6510,
                                //CPU_M6502,
				VIC6569_CLOCK,
				c64_readmem, c64_writemem,
				null, null,
				c64_frame_interrupt, 1,
				vic2_raster_irq, VIC2_HRETRACERATE
                            )
		},
		VIC6569_VRETRACERATE, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		0,
		c64_init_machine,
		c64_shutdown_machine,

	  /* video hardware */
		336,							   /* screen width */
		216,							   /* screen height */
		new rectangle(0, 336 - 1, 0, 216 - 1),                      /* visible_area */
		null,								   /* graphics decode info */
		vic2_palette.length / 3,
		0,
		c64_init_palette,				   /* convert color prom */
		VIDEO_TYPE_RASTER,
		null,
		vic2_vh_start,
		vic2_vh_stop,
		vic2_vh_screenrefresh,
	
	  /* sound hardware */
		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{ SOUND_CUSTOM, &pal_sound_interface },
/*TODO*///			{ 0 }
/*TODO*///		}
                null
	);
	
/*TODO*///	static struct MachineDriver machine_driver_sx64 =
/*TODO*///	{
/*TODO*///	  /* basic machine hardware */
/*TODO*///		{
/*TODO*///			{
/*TODO*///				CPU_M6510,
/*TODO*///				VIC6569_CLOCK,
/*TODO*///				c64_readmem, c64_writemem,
/*TODO*///				0, 0,
/*TODO*///				c64_frame_interrupt, 1,
/*TODO*///				vic2_raster_irq, VIC2_HRETRACERATE,
/*TODO*///			},
/*TODO*///			VC1541_CPU
/*TODO*///		},
/*TODO*///		VIC6569_VRETRACERATE,
/*TODO*///		DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
/*TODO*///	#ifdef CPU_SYNC
/*TODO*///		1,
/*TODO*///	#else
/*TODO*///		3000,
/*TODO*///	#endif
/*TODO*///		c64_init_machine,
/*TODO*///		c64_shutdown_machine,
/*TODO*///	
/*TODO*///	  /* video hardware */
/*TODO*///		336,							   /* screen width */
/*TODO*///		216,							   /* screen height */
/*TODO*///		{0, 336 - 1, 0, 216 - 1},		   /* visible_area */
/*TODO*///		0,								   /* graphics decode info */
/*TODO*///		sizeof (vic2_palette) / sizeof (vic2_palette[0]) / 3,
/*TODO*///		0,
/*TODO*///		c64_init_palette,				   /* convert color prom */
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		0,
/*TODO*///		vic2_vh_start,
/*TODO*///		vic2_vh_stop,
/*TODO*///		vic2_vh_screenrefresh,
/*TODO*///	
/*TODO*///	  /* sound hardware */
/*TODO*///		0, 0, 0, 0,
/*TODO*///		{
/*TODO*///			{ SOUND_CUSTOM, &pal_sound_interface },
/*TODO*///			{ 0 }
/*TODO*///		}
/*TODO*///	};
	
	static IODevice io_c64[] =
	{
		IODEVICE_CBM_QUICK,
		IODEVICE_CBM_ROM("crt\080\0"),
		IODEVICE_VC20TAPE,
		IODEVICE_CBM_DRIVE,
		new IODevice(IO_END)
	};
	
	static IODevice  io_sx64[] =
	{
/*TODO*///		IODEVICE_CBM_QUICK,
/*TODO*///		IODEVICE_CBM_ROM("crt\080\0"),
/*TODO*///		IODEVICE_VC1541,
                new IODevice(IO_END)
	};
	
        static IODevice io_ultimax[] =
        {
		IODEVICE_CBM_QUICK,
		IODEVICE_CBM_ROM("crt\0e0\0f0\0"),
		IODEVICE_VC20TAPE,
                new IODevice(IO_END)
	};

        static IODevice io_c64gs[] =
        {
		IODEVICE_CBM_ROM("crt\080\0"),
                new IODevice(IO_END)
        };

/*TODO*///	#define init_c64 c64_driver_init
/*TODO*///	#define init_c64pal c64pal_driver_init
/*TODO*///	#define init_ultimax ultimax_driver_init
/*TODO*///	#define init_sx64 sx64_driver_init
/*TODO*///	#define init_c64gs c64gs_driver_init
/*TODO*///	
/*TODO*///	#define io_c64pal io_c64
/*TODO*///	#define io_vic64s io_c64
/*TODO*///	#define io_max io_ultimax
/*TODO*///	#define io_cbm4064 io_c64
/*TODO*///	#define io_vip64 io_sx64
/*TODO*///	#define io_dx64 io_sx64
/*TODO*///	
/*TODO*///	#define rom_max rom_ultimax
/*TODO*///	#define rom_cbm4064 rom_pet64
/*TODO*///	
/*TODO*///	/*	  YEAR	NAME		PARENT	MACHINE 		INPUT	INIT	COMPANY 						   FULLNAME */
    //	COMP(1982, max,		0,		ultimax,		ultimax,ultimax,"Commodore Business Machines Co.", "Commodore Max (Ultimax/VC10)")
        public static GameDriver driver_max = new GameDriver("1982", "max", "c64.java", rom_ultimax, null, machine_driver_ultimax, input_ports_ultimax, ultimax_driver_init, io_ultimax, "Commodore Business Machines Co.", "Commodore Max (Ultimax/VC10)");
    //	COMP(1982, c64,		0,		c64,			c64,	c64,	"Commodore Business Machines Co.", "Commodore 64 (NTSC)")
        public static GameDriver driver_c64 = new GameDriver("1982", "c64", "c64.java", rom_c64, null, machine_driver_c64, input_ports_c64, c64_driver_init, io_c64, "Commodore Business Machines Co.", "Commodore 64 (NTSC)");
    //	COMP(1982, cbm4064,	c64,	pet64,			c64,	c64,	"Commodore Business Machines Co.", "CBM4064/PET64/Educator64 (NTSC)")
        public static GameDriver driver_cbm4064 = new GameDriver("1982", "cbm4064", "c64.java", rom_pet64, null, machine_driver_pet64, input_ports_c64, c64_driver_init, io_c64, "Commodore Business Machines Co.", "CBM4064/PET64/Educator64 (NTSC)");
    //	COMP(1982, c64pal, 	c64,	c64pal, 		c64,	c64pal, "Commodore Business Machines Co.", "Commodore 64/VC64/VIC64 (PAL)")
        public static GameDriver driver_c64pal = new GameDriver("1982", "c64pal", "c64.java", rom_c64pal, null, machine_driver_c64pal, input_ports_c64, c64pal_driver_init, io_c64, "Commodore Business Machines Co.", "Commodore 64/VC64/VIC64 (PAL)");
/*TODO*///	COMP(1982, vic64s, 	c64,	c64pal, 		vic64s,	c64pal, "Commodore Business Machines Co.", "Commodore 64 Swedish (PAL)")
    //	CONS(1987, c64gs,		c64,	c64gs,			c64gs,	c64gs,	"Commodore Business Machines Co.", "C64GS (PAL)")
        public static GameDriver driver_c64gs = new GameDriver("1987", "c64gs", "c64.java", rom_c64gs, null, machine_driver_c64gs, input_ports_c64gs, c64gs_driver_init, io_c64gs, "Commodore Business Machines Co.", "C64GS (PAL)");
/*TODO*///	/* please leave the following as testdriver, */
/*TODO*///	/* or better don't include them in system.c */
/*TODO*///	COMPX(1983, sx64,		c64,	sx64,			sx64,	sx64,	"Commodore Business Machines Co.", "SX64 (PAL)",                      GAME_NOT_WORKING)
/*TODO*///	COMPX(1983, vip64,		c64,	sx64,			vip64,	sx64,	"Commodore Business Machines Co.", "VIP64 (SX64 PAL), Swedish Expansion Kit", GAME_NOT_WORKING)
/*TODO*///	// sx64 with second disk drive
/*TODO*///	COMPX(198?, dx64,		c64,	sx64,			sx64,	sx64,	"Commodore Business Machines Co.", "DX64 (Prototype, PAL)",                      GAME_NOT_WORKING)
/*TODO*///	/*c64 II (cbm named it still c64) */
/*TODO*///	/*c64c (bios in 1 chip) */
/*TODO*///	/*c64g late 8500/8580 based c64, sold at aldi/germany */
/*TODO*///	/*c64cgs late c64, sold in ireland, gs bios?, but with keyboard */
/*TODO*///	
/*TODO*///	#ifdef RUNTIME_LOADER
/*TODO*///	extern void c64_runtime_loader_init(void)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		for (i=0; drivers[i]; i++) {
/*TODO*///			if ( strcmp(drivers[i]->name,"max")==0) drivers[i]=&driver_max;
/*TODO*///			if ( strcmp(drivers[i]->name,"c64")==0) drivers[i]=&driver_c64;
/*TODO*///			if ( strcmp(drivers[i]->name,"cbm4064")==0) drivers[i]=&driver_cbm4064;
/*TODO*///			if ( strcmp(drivers[i]->name,"c64pal")==0) drivers[i]=&driver_c64pal;
/*TODO*///			if ( strcmp(drivers[i]->name,"vic64s")==0) drivers[i]=&driver_vic64s;
/*TODO*///			if ( strcmp(drivers[i]->name,"c64gs")==0) drivers[i]=&driver_c64gs;
/*TODO*///			if ( strcmp(drivers[i]->name,"sx64")==0) drivers[i]=&driver_sx64;
/*TODO*///			if ( strcmp(drivers[i]->name,"vip64")==0) drivers[i]=&driver_vip64;
/*TODO*///			if ( strcmp(drivers[i]->name,"dx64")==0) drivers[i]=&driver_sx64;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	#endif
}

