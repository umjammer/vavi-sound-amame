/***************************************************************************

	TExt Display 7360
	PeT mess@utanet.at

***************************************************************************/
/*
Metal Oxid Semicontuctor MOS
Commodore Business Machine CBM
Textured Display Ted7360
------------------------

7360/8360 (NTSC-M, PAL-B by same chip ?)
8365 PAL-N
8366 PAL-M

Ted7360 used in Commodore C16
Ted8360 used in Commodore C116/Plus4
 should be 100% software kompatible with Ted7360

video unit (without sprites), dram controller seams to be much like vic6567

PAL/NTSC variants

functions of this chip:
 memory controller
 dram controller
 cpu clock generation with single and double clock switching
 3 timer
 1 8bit latched input
 2 channel sound/noise generator
 video controller
 interrupt controller for timer and video

Reasons for this document:
 document state in mess c16/plus4 emulator
 base to discuss ted7360 with others (i have no c16 computer anymore)

pal:
 clock 17734470
 divided by 5 for base clock (pixel clock changing edges?)
 312 lines
 50 hertz vertical refresh

ntsc:
 clock 14318180
 divided by 4 for base clock (pixel clock changing edges?)
 262 lines
 60 hertz vertical refresh

registers:
 0xff00 timer 1 low byte
 0xff01 timer 1 high byte
 0xff02 timer 2 low byte
 0xff03 timer 2 high byte
 0xff04 timer 3 low byte
 0xff05 timer 3 high byte
 0xff06
  bit 7 test?
  bit 6 ecm on
  bit 5 hires on
  bit 4 screen on
  bit 3 25 raws (0 24 raws)
  bit 2-0 vertical position
 0xff07
  bit 7 reverse off (0 on)
  bit 6 read only NTSC (0 PAL)
  bit 5 freeze horicontal position?
  bit 4 multicolor on
  bit 3 40 columns (0 38 columns)
  bit 2-0 horicontal pos
 0xff08 input latch (0 input low), write reloads latch
 0xff09 interrupt request
  7: interrupt
  6: timer 3
  5: ?
  4: timer 2
  3: timer 1
  2: lightpen
  1: rasterline
  0: 1 (asl quitting)
 0xff0a interrupt enable
  7: ?
  6: timer 3
  5: ?
  4: timer 2
  3: timer 1
  2: lightpen
  1: rasterline
  0: irq rasterline bit 8
 0xff0b
  7-0: irq rasterline 7-0
 0xff0c
  7-2: 1?
  1,0: cursorpos bit 9,8
 0xff0d cursorpos bit 7-0
 0xff0e tone channel 1: frequency 7-0
 0xff0f tone channel 2: frequency 7-0
 0xff10
  7-2: ?
  1,0: tone channel 2 bit 9,8
 0xff11
  7: sound reload
  6: tone 2 noise on (tone 2 must be off for noise)
  5: tone 2 tone on
  4: tone 1 on
  3-0: volume 8?-0
 0xff12
  7,6: ?
  5-3: bitmap address bit 15-13
  2: bitmapmemory?, charactermemory in rom (0 ram)
  1,0: tone 1 frequency bit 9,8
 0xff13
  7-2: chargen address bit 15-11
  2: chargen address bit 10 (only used when reverse is off)
  1: single clock in overscan area (0 double clock)
  0: ram (0)/rom status
 0xff14
  7-2: video address bit 15-10
  1,0: ?
 0xff15
  7: ?
  6-0: backgroundcolor
 0xff16
  7: ?
  6-0: color1
 0xff17
  7: ?
  6-0: color2
 0xff18
  7: ?
  6-0: color3
 0xff19
  7: ?
  6-0: framecolor
 0xff1a
  7-2: ?
  1,0: bitmap reload(cursorpos2) bit 9-8
 0xff1b bitmap reload bit 7-0
 0xff1c
  7-1: 1? (matrix expects this)
  0: current rasterline bit 8
 0xff1d current rasterline bit 7-0
 0xff1e
  7-0: current rastercolumn /2 (0 begin visible area?)
 0xff1f cursorblink?
  7: ?
  6-3: blink counter
  2-0: vsub
 0xff3e write switches to rom
 0xff3f write switches to dram

memory controller
 0x8000-0xfcff, 0xff20-0xffff
  read selection between dram and rom
 0xff3e-0xff3f write nothing (would change main memory in 16k ram c16)

clock generator
 in displayed area ted need 3 memory cycles to get data for video chip
 memory system runs at approximately 4 megahertz
 so only 1 megahertz is available for the cpu
 in the other area the clock can be doubled for the cpu

dram controller
 generator of refresh cycles
 when are these occuring?
 vic6567 in each rasterline reads 5 refresh addresses

sound generator
 2 channels
 channel 2 can be switched to generate noise
 volume control (0-8?)
 tone reload bit?
 samples ton/noise?
 frequency=(base clock/32)/(1024-value)

timer
 timer count down
 when reaching 0
  they restart from 0xffff
  set the interrupt request flag
 writing to low byte stops timer
 writing to high byte restarts timer with current value

interrupt controller
 interrupt sources must be enabled in the enable register to generate
 an interrupt
 request flag are set also when not enabled
 interrupt is generated when rising edge on request and enable
 quitting of interrupt is done via writing 1 to the request bit
 what happens with request interrupts when the are enabled?

Video part
 colors
  bit 3-0 color
   black, white, red, cyan
   purple, green, blue, yellow
   orange, light orange, pink, light cyan,
   light violett, light green, light blue, light yellow
  bit 6-4 luminance (0 dark)
  r,g,b values from 0 to 127
   taken from digitized tv screen shot
   0x06,0x01,0x03, 0x2b,0x2b,0x2b, 0x67,0x0e,0x0f, 0x00,0x3f,0x42,
   0x57,0x00,0x6d, 0x00,0x4e,0x00, 0x19,0x1c,0x94, 0x38,0x38,0x00,
   0x56,0x20,0x00, 0x4b,0x28,0x00, 0x16,0x48,0x00, 0x69,0x07,0x2f,
   0x00,0x46,0x26, 0x06,0x2a,0x80, 0x2a,0x14,0x9b, 0x0b,0x49,0x00,

   0x00,0x03,0x02, 0x3d,0x3d,0x3d, 0x75,0x1e,0x20, 0x00,0x50,0x4f,
   0x6a,0x10,0x78, 0x04,0x5c,0x00, 0x2a,0x2a,0xa3, 0x4c,0x47,0x00,
   0x69,0x2f,0x00, 0x59,0x38,0x00, 0x26,0x56,0x00, 0x75,0x15,0x41,
   0x00,0x58,0x3d, 0x15,0x3d,0x8f, 0x39,0x22,0xae, 0x19,0x59,0x00,

   0x00,0x03,0x04, 0x42,0x42,0x42, 0x7b,0x28,0x20, 0x02,0x56,0x59,
   0x6f,0x1a,0x82, 0x0a,0x65,0x09, 0x30,0x34,0xa7, 0x50,0x51,0x00,
   0x6e,0x36,0x00, 0x65,0x40,0x00, 0x2c,0x5c,0x00, 0x7d,0x1e,0x45,
   0x01,0x61,0x45, 0x1c,0x45,0x99, 0x42,0x2d,0xad, 0x1d,0x62,0x00,

   0x05,0x00,0x02, 0x56,0x55,0x5a, 0x90,0x3c,0x3b, 0x17,0x6d,0x72,
   0x87,0x2d,0x99, 0x1f,0x7b,0x15, 0x46,0x49,0xc1, 0x66,0x63,0x00,
   0x84,0x4c,0x0d, 0x73,0x55,0x00, 0x40,0x72,0x00, 0x91,0x33,0x5e,
   0x19,0x74,0x5c, 0x32,0x59,0xae, 0x59,0x3f,0xc3, 0x32,0x76,0x00,

   0x02,0x01,0x06, 0x84,0x7e,0x85, 0xbb,0x67,0x68, 0x45,0x96,0x96,
   0xaf,0x58,0xc3, 0x4a,0xa7,0x3e, 0x73,0x73,0xec, 0x92,0x8d,0x11,
   0xaf,0x78,0x32, 0xa1,0x80,0x20, 0x6c,0x9e,0x12, 0xba,0x5f,0x89,
   0x46,0x9f,0x83, 0x61,0x85,0xdd, 0x84,0x6c,0xef, 0x5d,0xa3,0x29,

   0x02,0x00,0x0a, 0xb2,0xac,0xb3, 0xe9,0x92,0x92, 0x6c,0xc3,0xc1,
   0xd9,0x86,0xf0, 0x79,0xd1,0x76, 0x9d,0xa1,0xff, 0xbd,0xbe,0x40,
   0xdc,0xa2,0x61, 0xd1,0xa9,0x4c, 0x93,0xc8,0x3d, 0xe9,0x8a,0xb1,
   0x6f,0xcd,0xab, 0x8a,0xb4,0xff, 0xb2,0x9a,0xff, 0x88,0xcb,0x59,

   0x02,0x00,0x0a, 0xc7,0xca,0xc9, 0xff,0xac,0xac, 0x85,0xd8,0xe0,
   0xf3,0x9c,0xff, 0x92,0xea,0x8a, 0xb7,0xba,0xff, 0xd6,0xd3,0x5b,
   0xf3,0xbe,0x79, 0xe6,0xc5,0x65, 0xb0,0xe0,0x57, 0xff,0xa4,0xcf,
   0x89,0xe5,0xc8, 0xa4,0xca,0xff, 0xca,0xb3,0xff, 0xa2,0xe5,0x7a,

   0x01,0x01,0x01, 0xff,0xff,0xff, 0xff,0xf6,0xf2, 0xd1,0xff,0xff,
   0xff,0xe9,0xff, 0xdb,0xff,0xd3, 0xfd,0xff,0xff, 0xff,0xff,0xa3,
   0xff,0xff,0xc1, 0xff,0xff,0xb2, 0xfc,0xff,0xa2, 0xff,0xee,0xff,
   0xd1,0xff,0xff, 0xeb,0xff,0xff, 0xff,0xf8,0xff, 0xed,0xff,0xbc

 video generation
  column counter from 0 to 39
  line counter from 0 to 199

  character pointer position/hires: color 0-15 position
   videoaddr+0x400+(line/8)*40+column
  color position/hires: color luminance position
   videoaddr+(line/8)*40+column
  character bitmap position
   chargenaddr+(character pointer)+(line&7)
  bitmap position
   bitmapaddr+(line/8)*40+column+(line&7)

 normal text mode (reverse off, ecm off, multicolor off, hires off)
  character pointer based
   fetch character pointer, fetch character bitmap, fetch color
  pixel on color (color)&0x7f
  pixel off color backgroundcolor
  blinking (synchron to cursor) (color&0x80)
 reverse text mode (reverse on, ecm off, multicolor off, hires off)
  character pointer &0x7f
  character pointer bit 7
   set
    pixel off color attribut &0x7f
    pixel on color backgroundcolor
   clear
    like normal text mode
  blinking (synchron to cursor) (color&0x80)
 multicolor text mode (ecm off, multicolor on, hires off)
  reverse ?
  attribut bit 3 character in multicolor (else monocolor character)
  pixel on color attribut 0x77
  0 backgroundcolor
  1 color1
  2 color2
  3 attribut &0x77
  (color&0x80) ?
 ecm text mode (ecm on)
  reverse ?
  multicolor ? (c64/vic6567 black display?)
  hires ? (c64/vic6567 black display?)
  character pointer &0x3f
  character pointer bit 7,6 select pixel off color
   0 backgroundcolor ?
   1 color1?
   2 color2?
   3 color3?
  (color&0x80)?
 hires (ecm off, multicolor off, hires on)
  bitmap based
   fetch bitmap, fetch color, fetch color luminance
  reverse ?
  pixel on color
   6-4: (color luminance) bit 2-0
   3-0: (color) bit 7-4
  pixel off color
   6-4: (color luminance) bit 6-4
   3-0: (color) bit 3-0
  (color) bit 7?
  (color luminance) bit 7?
 hires multicolor (ecm off, multicolor on, hires on)
  reverse ?
  0 backgroundcolor
  1
   6-4: (color luminance) bit 2-0
   3-0: (color) bit 7-4
  2
   6-4: (color luminance) bit 6-4
   3-0: (color) bit 3-0
  3 color1
  (color) bit 7?
  (color luminance) bit 7?
 scrolling support
  visible part of the display in fixed position
  columns 38 mode left 7 and right 9 pixel less displayed
   but line generation start at the same position as in columns 40 mode
  lines25 flag top and bottom 4 pixel less displayed ?
  horicontal scrolling support
   in columns 38 mode
    horicontal position 1-7: pixel later started line generation
    horicontal position 0: 8 pixels later started line generation
   in columns 40 mode ?
  vertical scrolling support
   in lines 24 mode
    vertical position 1-7: lines later started line generation
    vertical position 0: 8 lines later started line generation
   in lines 25 mode?
    vertial position 3: for correct display

 hardware cursor
  full 8x8 in (color) color
  flashing when in multicolor, hires ?
  flashes with about 1 hertz
  bitmap reload?

 rasterline/rastercolumn
  values in registers begin visible area 0
  vic 6567
   pal line 0 beginning of vertical refresh
   ntsc line 0 in bottom frame
    beginning of 25 lines screen 0x33 (24 lines screen 0x37)
    beginning of 40 columns line 0x18 (38 columns 0x1f)

 lightpen
  (where to store values?)
  i found a lightpen hardware description with a demo and drawing program
  lightpen must be connected to joy0 (button)
  demo program:
   disables interrupt, polles joy0 button (0xfd0d? plus 4 related???)
   and reads the rasterline value!
   so i no exact column value reachable!
*/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.video.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.sndintrfH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.includes.c16H.*;
import static mess056.sndhrdw.ted7360.*;
import static mess056.includes.ted7360H.*;
import static mess056.machine.c16.*;

public class ted7360
{
	
	public static int VERBOSE_DBG = 0;
	
	
	/*#define GFX */
	
	public static int VREFRESHINLINES = 28;
	
	public static int TIMER1HELPER(){ return (ted7360[0]|(ted7360[1]<<8)); }
	public static int TIMER2HELPER(){ return (ted7360[2]|(ted7360[3]<<8)); }
	public static int TIMER3HELPER(){ return (ted7360[4]|(ted7360[5]<<8)); }
	public static int TIMER1(){ return (TIMER1HELPER()!=0?TIMER1HELPER():0x10000); }
	public static int TIMER2(){ return (TIMER2HELPER()!=0?TIMER2HELPER():0x10000); }
	public static int TIMER3(){ return (TIMER3HELPER()!=0?TIMER3HELPER():0x10000); }
	public static double TEDTIME_IN_CYCLES(int a){ return ((double)(a)/TED7360_CLOCK());}
	public static int TEDTIME_TO_CYCLES(int a) { return(int)((a)*TED7360_CLOCK());}
	
	public static int TED7360_YPOS = 40;
	public static int RASTERLINE_2_C16(int a){ return ((a+lines-TED7360_YPOS-5)%lines);}
	public static int C16_2_RASTERLINE(int a){ return ((a+TED7360_YPOS+5)%lines); }
	public static int XPOS = 8;
	public static int YPOS = 8;
	
	public static int SCREENON(){ return (ted7360[6]&0x10); }
	public static int TEST(){ return (ted7360[6]&0x80); }
	public static int VERTICALPOS(){ return (ted7360[6]&7); }
	public static int HORICONTALPOS(){ return (ted7360[7]&7); }
	public static int ECMON(){ return (ted7360[6]&0x40); }
	public static int HIRESON(){ return (ted7360[6]&0x20); }
	public static int MULTICOLORON(){ return (ted7360[7]&0x10); }
	public static int REVERSEON(){ return ((ted7360[7]&0x80))!=0?0:1; }
	/* hardware inverts character when bit 7 set (character taken &0x7f) */
	/* instead of fetching character with higher number! */
	public static int LINES25(){ return (ted7360[6]&8); }		   /* else 24 Lines */
	public static int LINES(){ return (LINES25()!=0?25:24); }
	public static int YSIZE(){ return (LINES()*8); }
	public static int COLUMNS40(){ return (ted7360[7]&8); }	   /* else 38 Columns */
	public static int COLUMNS(){ return (COLUMNS40()!=0?40:38); }
	public static int XSIZE(){ return (COLUMNS()*8); }
	
	public static int INROM(){ return (ted7360[0x12]&4); }
	public static int CHARGENADDR(){ return (REVERSEON()!=0&&HIRESON()==0&&MULTICOLORON()==0?
			     ((ted7360[0x13]&0xfc)<<8):
			     ((ted7360[0x13]&0xf8)<<8) ); }
	public static int BITMAPADDR(){ return ((ted7360[0x12]&0x38)<<10); }
	public static int VIDEOADDR(){ return ((ted7360[0x14]&0xf8)<<8); }
	
	public static int RASTERLINE(){ return ( ((ted7360[0xa]&1)<<8)|ted7360[0xb]); }
	public static int CURSOR1POS(){ return ( ted7360[0xd]|((ted7360[0xc]&3)<<8) ); }
	public static int CURSOR2POS(){ return ( ted7360[0x1b]|((ted7360[0x1a]&3)<<8) ); }
	public static int CURSORRATE(){ return ( (ted7360[0x1f]&0x7c)>>2 ); }
	
	public static int BACKGROUNDCOLOR(){ return (ted7360[0x15]&0x7f); }
	public static int FOREGROUNDCOLOR(){ return (ted7360[0x16]&0x7f); }
	public static int MULTICOLOR1(){ return (ted7360[0x17]&0x7f); }
	public static int MULTICOLOR2(){ return (ted7360[0x18]&0x7f); }
	public static int FRAMECOLOR(){ return (ted7360[0x19]&0x7f); }
	
	public static char ted7360_palette[] =
	{
	/* black, white, red, cyan */
	/* purple, green, blue, yellow */
	/* orange, light orange, pink, light cyan, */
	/* light violett, light green, light blue, light yellow */
	/* these 16 colors are 8 times here in different luminance (dark..light) */
	/* taken from digitized tv screenshot */
		0x06, 0x01, 0x03, 0x2b, 0x2b, 0x2b, 0x67, 0x0e, 0x0f, 0x00, 0x3f, 0x42,
		0x57, 0x00, 0x6d, 0x00, 0x4e, 0x00, 0x19, 0x1c, 0x94, 0x38, 0x38, 0x00,
		0x56, 0x20, 0x00, 0x4b, 0x28, 0x00, 0x16, 0x48, 0x00, 0x69, 0x07, 0x2f,
		0x00, 0x46, 0x26, 0x06, 0x2a, 0x80, 0x2a, 0x14, 0x9b, 0x0b, 0x49, 0x00,
	
		0x00, 0x03, 0x02, 0x3d, 0x3d, 0x3d, 0x75, 0x1e, 0x20, 0x00, 0x50, 0x4f,
		0x6a, 0x10, 0x78, 0x04, 0x5c, 0x00, 0x2a, 0x2a, 0xa3, 0x4c, 0x47, 0x00,
		0x69, 0x2f, 0x00, 0x59, 0x38, 0x00, 0x26, 0x56, 0x00, 0x75, 0x15, 0x41,
		0x00, 0x58, 0x3d, 0x15, 0x3d, 0x8f, 0x39, 0x22, 0xae, 0x19, 0x59, 0x00,
	
		0x00, 0x03, 0x04, 0x42, 0x42, 0x42, 0x7b, 0x28, 0x20, 0x02, 0x56, 0x59,
		0x6f, 0x1a, 0x82, 0x0a, 0x65, 0x09, 0x30, 0x34, 0xa7, 0x50, 0x51, 0x00,
		0x6e, 0x36, 0x00, 0x65, 0x40, 0x00, 0x2c, 0x5c, 0x00, 0x7d, 0x1e, 0x45,
		0x01, 0x61, 0x45, 0x1c, 0x45, 0x99, 0x42, 0x2d, 0xad, 0x1d, 0x62, 0x00,
	
		0x05, 0x00, 0x02, 0x56, 0x55, 0x5a, 0x90, 0x3c, 0x3b, 0x17, 0x6d, 0x72,
		0x87, 0x2d, 0x99, 0x1f, 0x7b, 0x15, 0x46, 0x49, 0xc1, 0x66, 0x63, 0x00,
		0x84, 0x4c, 0x0d, 0x73, 0x55, 0x00, 0x40, 0x72, 0x00, 0x91, 0x33, 0x5e,
		0x19, 0x74, 0x5c, 0x32, 0x59, 0xae, 0x59, 0x3f, 0xc3, 0x32, 0x76, 0x00,
	
		0x02, 0x01, 0x06, 0x84, 0x7e, 0x85, 0xbb, 0x67, 0x68, 0x45, 0x96, 0x96,
		0xaf, 0x58, 0xc3, 0x4a, 0xa7, 0x3e, 0x73, 0x73, 0xec, 0x92, 0x8d, 0x11,
		0xaf, 0x78, 0x32, 0xa1, 0x80, 0x20, 0x6c, 0x9e, 0x12, 0xba, 0x5f, 0x89,
		0x46, 0x9f, 0x83, 0x61, 0x85, 0xdd, 0x84, 0x6c, 0xef, 0x5d, 0xa3, 0x29,
	
		0x02, 0x00, 0x0a, 0xb2, 0xac, 0xb3, 0xe9, 0x92, 0x92, 0x6c, 0xc3, 0xc1,
		0xd9, 0x86, 0xf0, 0x79, 0xd1, 0x76, 0x9d, 0xa1, 0xff, 0xbd, 0xbe, 0x40,
		0xdc, 0xa2, 0x61, 0xd1, 0xa9, 0x4c, 0x93, 0xc8, 0x3d, 0xe9, 0x8a, 0xb1,
		0x6f, 0xcd, 0xab, 0x8a, 0xb4, 0xff, 0xb2, 0x9a, 0xff, 0x88, 0xcb, 0x59,
	
		0x02, 0x00, 0x0a, 0xc7, 0xca, 0xc9, 0xff, 0xac, 0xac, 0x85, 0xd8, 0xe0,
		0xf3, 0x9c, 0xff, 0x92, 0xea, 0x8a, 0xb7, 0xba, 0xff, 0xd6, 0xd3, 0x5b,
		0xf3, 0xbe, 0x79, 0xe6, 0xc5, 0x65, 0xb0, 0xe0, 0x57, 0xff, 0xa4, 0xcf,
		0x89, 0xe5, 0xc8, 0xa4, 0xca, 0xff, 0xca, 0xb3, 0xff, 0xa2, 0xe5, 0x7a,
	
		0x01, 0x01, 0x01, 0xff, 0xff, 0xff, 0xff, 0xf6, 0xf2, 0xd1, 0xff, 0xff,
		0xff, 0xe9, 0xff, 0xdb, 0xff, 0xd3, 0xfd, 0xff, 0xff, 0xff, 0xff, 0xa3,
		0xff, 0xff, 0xc1, 0xff, 0xff, 0xb2, 0xfc, 0xff, 0xa2, 0xff, 0xee, 0xff,
		0xd1, 0xff, 0xff, 0xeb, 0xff, 0xff, 0xff, 0xf8, 0xff, 0xed, 0xff, 0xbc
	};
	
	public static CustomSound_interface ted7360_sound_interface = new CustomSound_interface
        (
		ted7360_custom_start,
		ted7360_custom_stop,
		ted7360_custom_update
	);
	
	public static int[] ted7360 = new int[0x20];
	
	public static boolean ted7360_pal;
	public static boolean ted7360_rom;
	
	static int lines=312;
	static timer_entry timer1 = null, timer2 = null, timer3 = null;
	static boolean cursor1 = false;
	static ReadHandlerPtr vic_dma_read;
	static ReadHandlerPtr vic_dma_read_rom;
	static int chargenaddr, bitmapaddr, videoaddr;
	
	static int x_begin, x_end;
	static int y_begin, y_end;
	
	static int[] c16_bitmap=new int[2], bitmapmulti=new int[4], mono=new int[2], monoinversed=new int[2], multi=new int[4], ecmcolor=new int[2], colors=new int[5];
	
	//static mame_bitmap ted7360_bitmap;	/* Machine.scrbitmap for speedup */
	static int rasterline = 0, lastline = 0;
	static double rastertime;
	
	static int[] cursorcolortable = new int[2];
        
	static GfxLayout cursorlayout = new GfxLayout
	(
		8, 8,
		1,
		1,
		new int[] {0},
		new int[] {0, 1, 2, 3, 4, 5, 6, 7},
		new int[] {0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8},
		8 * 8
	);
	
	static char cursormask[] =
	{0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff};
        
	static GfxElement cursorelement;
	
	public static void ted7360_init (int pal)
	{
		ted7360_pal = pal!=0 ? true:false;
		lines = TED7360_LINES;
		chargenaddr = bitmapaddr = videoaddr = 0;
	}
	
	public static void ted7360_set_dma (ReadHandlerPtr dma_read,
						  ReadHandlerPtr dma_read_rom)
	{
		vic_dma_read = dma_read;
		vic_dma_read_rom = dma_read_rom;
	}
	
	static void ted7360_set_interrupt (int mask)
	{
		/* kernel itself polls for timer 2 shot (interrupt disabled!)
		 * when cassette loading */
		ted7360[9] |= mask;
		if ((ted7360[0xa] & ted7360[9] & 0x5e)!=0)
		{
			if ((ted7360[9] & 0x80)==0)
			{
				/*TODO*///DBG_LOG (1, "ted7360", (errorlog, "irq start %.2x\n", mask));
				ted7360[9] |= 0x80;
				c16_interrupt (1);
			}
		}
		ted7360[9] |= mask;
	}
	
	static void ted7360_clear_interrupt (int mask)
	{
		ted7360[9] &= ~mask;
		if ((ted7360[9] & 0x80)!=0 && (ted7360[9] & ted7360[0xa] & 0x5e)==0)
		{
			/*TODO*///DBG_LOG (1, "ted7360", (errorlog, "irq end %.2x\n", mask));
			ted7360[9] &= ~0x80;
			c16_interrupt (0);
		}
	}
	
	static int ted7360_rastercolumn ()
	{
		return (int) ((timer_get_time () - rastertime)
					  * TED7360_VRETRACERATE * lines * 57 * 8 + 0.5);
	}
	
	public static timer_callback ted7360_timer_timeout = new timer_callback() {
            public void handler(int which) {
                /*TODO*///DBG_LOG (3, "ted7360 ", (errorlog, "timer %d timeout\n", which));
		switch (which)
		{
		case 1:
		    // prooved by digisound of several intros like eoroidpro
			timer1 = timer_set (TEDTIME_IN_CYCLES (TIMER1()), 1, ted7360_timer_timeout);
			ted7360_set_interrupt (8);
			break;
		case 2:
			timer2 = timer_set (TEDTIME_IN_CYCLES (0x10000), 2, ted7360_timer_timeout);
			ted7360_set_interrupt (0x10);
			break;
		case 3:
			timer3 = timer_set (TEDTIME_IN_CYCLES (0x10000), 3, ted7360_timer_timeout);
			ted7360_set_interrupt (0x40);
			break;
		}
            }
        };
	
        static int count;
	
	public static int ted7360_frame_interrupt ()
	{
		
	
		if ((ted7360[0x1f] & 0xf) >= 0x0f)
		{
	/*  if (count>=CURSORRATE) { */
			cursor1 ^= true;
			ted7360[0x1f] &= ~0xf;
			count = 0;
		}
		else
			ted7360[0x1f]++;
		return ignore_interrupt.handler();
	}
	
	public static WriteHandlerPtr ted7360_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int old;
	
		if ((offset != 8) && ((offset < 0x15) || (offset > 0x19)))
		{
			/*TODO*///DBG_LOG (1, "ted7360_port_w", (errorlog, "%.2x:%.2x\n", offset, data));
		}
		switch (offset)
		{
		case 0xe:
		case 0xf:
		case 0x10:
		case 0x11:
		case 0x12:
			ted7360_soundport_w(offset, data);
			break;
		}
		switch (offset)
		{
		case 0:						   /* stop timer 1 */
			ted7360[offset] = data;
			if (timer1 != null)
			{
				ted7360[1] = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer1)) >> 8;
				timer_remove (timer1);
				timer1 = null;
			}
			break;
		case 1:						   /* start timer 1 */
			ted7360[offset] = data;
			if (timer1 == null)
				timer1 = timer_set (TEDTIME_IN_CYCLES (TIMER1()), 1, ted7360_timer_timeout);
			else
				timer_reset (timer1, TEDTIME_IN_CYCLES (TIMER1()));
			break;
		case 2:						   /* stop timer 2 */
			ted7360[offset] = data;
			if (timer2 != null)
			{
				ted7360[3] = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer2)) >> 8;
				timer_remove (timer2);
				timer2 = null;
			}
			break;
		case 3:						   /* start timer 2 */
			ted7360[offset] = data;
			if (timer2 == null)
				timer2 = timer_set (TEDTIME_IN_CYCLES (TIMER2()), 2, ted7360_timer_timeout);
			else
				timer_reset (timer2, TEDTIME_IN_CYCLES (TIMER2()));
			break;
		case 4:						   /* stop timer 3 */
			ted7360[offset] = data;
			if (timer3 != null)
			{
				ted7360[5] = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer3)) >> 8;
				timer_remove (timer3);
				timer3 = null;
			}
			break;
		case 5:						   /* start timer 3 */
			ted7360[offset] = data;
			if (timer3 == null)
				timer3 = timer_set (TEDTIME_IN_CYCLES (TIMER3()), 3, ted7360_timer_timeout);
			else
				timer_reset (timer3, TEDTIME_IN_CYCLES (TIMER3()));
			break;
		case 6:
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				if (LINES25() != 0)
				{
					y_begin = 0;
					y_end = y_begin + 200;
				}
				else
				{
					y_begin = 4;
					y_end = y_begin + 192;
				}
				chargenaddr = CHARGENADDR();
			}
			break;
		case 7:
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				if (COLUMNS40() != 0)
				{
					x_begin = 0;
					x_end = x_begin + 320;
				}
				else
				{
					x_begin = HORICONTALPOS();
					x_end = x_begin + 320;
				}
				/*TODO*///DBG_LOG (3, "ted7360_port_w", (errorlog, "%s %s\n", data & 0x40 ? "ntsc" : "pal",
				/*TODO*///							   data & 0x20 ? "hori freeze" : ""));
				chargenaddr = CHARGENADDR();
			}
			break;
		case 8:
			ted7360[offset] = c16_read_keyboard (data);
			break;
		case 9:
			if ((data & 8) != 0)
				ted7360_clear_interrupt (8);
			if ((data & 0x10) != 0)
				ted7360_clear_interrupt (0x10);
			if ((data & 0x40) != 0)
				ted7360_clear_interrupt (0x40);
			if ((data & 2) != 0)
				ted7360_clear_interrupt (2);
			break;
		case 0xa:
			old = data;
			ted7360[offset] = data | 0xa0;
	/*TODO*///#if 0
	/*TODO*///		ted7360[9] = (ted7360[9] & 0xa1) | (ted7360[9] & data & 0x5e);
	/*TODO*///		if (ted7360[9] & 0x80)
	/*TODO*///			ted7360_clear_interrupt (0);
	/*TODO*///#endif
			if (((data ^ old) & 1) != 0)
			{
	/*    /*TODO*///DBG_LOG(1,"set rasterline hi",(errorlog,"soll:%d\n",RASTERLINE)); */
			}
			break;
		case 0xb:
			if (data != ted7360[offset])
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				/*  /*TODO*///DBG_LOG(1,"set rasterline lo",(errorlog,"soll:%d\n",RASTERLINE)); */
			}
			break;
		case 0xc:
		case 0xd:
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
			}
			break;
		case 0x12:
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				bitmapaddr = BITMAPADDR();
				chargenaddr = CHARGENADDR();
				/*TODO*///DBG_LOG (3, "ted7360_port_w", (errorlog, "bitmap %.4x %s\n",
				/*TODO*///							   BITMAPADDR, INROM ? "rom" : "ram"));
			}
			break;
		case 0x13:
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				chargenaddr = CHARGENADDR();
				/*TODO*///DBG_LOG (3, "ted7360_port_w", (errorlog, "chargen %.4x %s %d\n",
				/*TODO*///							   CHARGENADDR, data & 2 ? "" : "doubleclock",
				/*TODO*///							   data & 1));
			}
			break;
		case 0x14:
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				videoaddr = VIDEOADDR();
				/*TODO*///DBG_LOG (3, "ted7360_port_w", (errorlog, "videoram %.4x\n",
				/*TODO*///							   VIDEOADDR));
			}
			break;
		case 0x15:						   /* backgroundcolor */
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				monoinversed[1] = mono[0] = bitmapmulti[0] = multi[0] = colors[0] =
					Machine.pens[BACKGROUNDCOLOR()];
			}
			break;
		case 0x16:						   /* foregroundcolor */
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				bitmapmulti[3] = multi[1] = colors[1] = Machine.pens[FOREGROUNDCOLOR()];
			}
			break;
		case 0x17:						   /* multicolor 1 */
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				multi[2] = colors[2] = Machine.pens[MULTICOLOR1()];
			}
			break;
		case 0x18:						   /* multicolor 2 */
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				colors[3] = Machine.pens[MULTICOLOR2()];
			}
			break;
		case 0x19:						   /* framecolor */
			if (ted7360[offset] != data)
			{
				ted7360_drawlines (lastline, rasterline);
				ted7360[offset] = data;
				colors[4] = Machine.pens[FRAMECOLOR()];
			}
			break;
		case 0x1c:
			ted7360[offset] = data;		   /*? */
			/*TODO*///DBG_LOG (1, "ted7360_port_w", (errorlog, "write to rasterline high %.2x\n",
			/*TODO*///							   data));
			break;
		case 0x1f:
			ted7360[offset] = data;
			/*TODO*///DBG_LOG (1, "ted7360_port_w", (errorlog, "write to cursorblink %.2x\n", data));
			break;
		default:
			ted7360[offset] = data;
			break;
		}
            }
        };
	
	public static ReadHandlerPtr ted7360_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int val = 0;
	
		switch (offset)
		{
		case 0:
			if (timer1 != null)
				val = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer1)) & 0xff;
			else
				val = ted7360[offset];
			break;
		case 1:
			if (timer1 != null)
				val = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer1)) >> 8;
			else
				val = ted7360[offset];
			break;
		case 2:
			if (timer2 != null)
				val = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer2)) & 0xff;
			else
				val = ted7360[offset];
			break;
		case 3:
			if (timer2 != null)
				val = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer2)) >> 8;
			else
				val = ted7360[offset];
			break;
		case 4:
			if (timer3 != null)
				val = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer3)) & 0xff;
			else
				val = ted7360[offset];
			break;
		case 5:
			if (timer3 != null)
				val = TEDTIME_TO_CYCLES ((int) timer_timeleft (timer3)) >> 8;
			else
				val = ted7360[offset];
			break;
		case 7:
			val = (ted7360[offset] & ~0x40);
			if (ted7360_pal == false)
				val |= 0x40;
			break;
		case 9:
			val = ted7360[offset] | 1;
			break;
		case 0xa:
			val = ted7360[offset];
			break;
		case 0xb:
			val = ted7360[offset];
			break;
		case 0x0c:
			val = ted7360[offset] |= 0xfc;
			break;
		case 0x13:
			val = ted7360[offset] & ~1;
			if (ted7360_rom)
				val |= 1;
			break;
		case 0x1c:						   /*rasterline */
			ted7360_drawlines (lastline, rasterline);
			val = ((RASTERLINE_2_C16 (rasterline) & 0x100) >> 8) | 0xfe;	/* expected by matrix */
			break;
		case 0x1d:						   /*rasterline */
			ted7360_drawlines (lastline, rasterline);
			val = RASTERLINE_2_C16 (rasterline) & 0xff;
			break;
		case 0x1e:						   /*rastercolumn */
			val = ted7360_rastercolumn () / 2;	/* pengo >=0x99 */
			break;
		case 0x1f:
			val = ((rasterline & 7) << 4) | (ted7360[offset] & 0x0f);
			/*TODO*///DBG_LOG (1, "ted7360_port_w", (errorlog, "read from cursorblink %.2x\n", val));
			break;
		default:
			val = ted7360[offset];
			break;
		}
		if ((offset != 8) && (offset >= 6) && (offset != 0x1c) && (offset != 0x1d) && (offset != 9)
			&& ((offset < 0x15) || (offset > 0x19)))
		{
			/*TODO*///DBG_LOG (1, "ted7360_port_r", (errorlog, "%.2x:%.2x\n", offset, val));
		}
		return val;
            }
        };
	
	public static VhStartPtr ted7360_vh_start = new VhStartPtr() {
            public int handler() {
                cursorelement = decodegfx (new UBytePtr(cursormask), cursorlayout);
		cursorelement.colortable = new IntArray(cursorcolortable);
		cursorcolortable[1] = Machine.pens[1];
		cursorelement.total_colors = 2;
		//ted7360_bitmap = Machine.scrbitmap;
		return 0;
            }
        };
	
	public static VhStopPtr ted7360_vh_stop = new VhStopPtr() {
            public void handler() {
                freegfx (cursorelement);
            }
        };
	
	static void ted7360_draw_character (int ybegin, int yend, int ch, int yoff, int xoff, int[] color)
	{
		int y, code=0xFF;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				if (INROM)
					code = vic_dma_read_rom (chargenaddr + ch * 8 + y);
				else
					code = vic_dma_read (chargenaddr + ch * 8 + y);
				ted7360_bitmap.line[y + yoff][xoff] = color[code >> 7];
				ted7360_bitmap.line[y + yoff][1 + xoff] = color[(code >> 6) & 1];
				ted7360_bitmap.line[y + yoff][2 + xoff] = color[(code >> 5) & 1];
				ted7360_bitmap.line[y + yoff][3 + xoff] = color[(code >> 4) & 1];
				ted7360_bitmap.line[y + yoff][4 + xoff] = color[(code >> 3) & 1];
				ted7360_bitmap.line[y + yoff][5 + xoff] = color[(code >> 2) & 1];
				ted7360_bitmap.line[y + yoff][6 + xoff] = color[(code >> 1) & 1];
				ted7360_bitmap.line[y + yoff][7 + xoff] = color[code & 1];
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				if (INROM() != 0)
					code = vic_dma_read_rom.handler(chargenaddr + ch * 8 + y);
				else
					code = vic_dma_read.handler(chargenaddr + ch * 8 + y);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff, (char) color[code >> 7]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(1 + xoff, (char) color[(code >> 6) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(2 + xoff, (char) color[(code >> 5) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(3 + xoff, (char) color[(code >> 4) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(4 + xoff, (char) color[(code >> 3) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(5 + xoff, (char) color[(code >> 2) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(6 + xoff, (char) color[(code >> 1) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(7 + xoff, (char) color[code & 1]);
			}
		}
	}
	
	static void ted7360_draw_character_multi (int ybegin, int yend, int ch, int yoff, int xoff)
	{
		int y, code=0xFF;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				if (INROM)
					code = vic_dma_read_rom (chargenaddr + ch * 8 + y);
				else
					code = vic_dma_read (chargenaddr + ch * 8 + y);
				ted7360_bitmap.line[y + yoff][xoff] =
					ted7360_bitmap.line[y + yoff][xoff + 1] = multi[code >> 6];
				ted7360_bitmap.line[y + yoff][xoff + 2] =
					ted7360_bitmap.line[y + yoff][xoff + 3] = multi[(code >> 4) & 3];
				ted7360_bitmap.line[y + yoff][xoff + 4] =
					ted7360_bitmap.line[y + yoff][xoff + 5] = multi[(code >> 2) & 3];
				ted7360_bitmap.line[y + yoff][xoff + 6] =
					ted7360_bitmap.line[y + yoff][xoff + 7] = multi[code & 3];
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				if (INROM() != 0)
					code = vic_dma_read_rom.handler(chargenaddr + ch * 8 + y);
				else
					code = vic_dma_read.handler(chargenaddr + ch * 8 + y);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff, (char) multi[code >> 6]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 1, (char) multi[code >> 6]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 2, (char) multi[(code >> 4) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 3, (char) multi[(code >> 4) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 4, (char) multi[(code >> 2) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 5, (char) multi[(code >> 2) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 6, (char) multi[code & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 7, (char) multi[code & 3]);
			}
		}
	}
	
	static void ted7360_draw_bitmap (int ybegin, int yend, int ch, int yoff, int xoff)
	{
		int y, code=0xFF;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic_dma_read (bitmapaddr + ch * 8 + y);
				ted7360_bitmap.line[y + yoff][xoff] = c16_bitmap[code >> 7];
				ted7360_bitmap.line[y + yoff][1 + xoff] = c16_bitmap[(code >> 6) & 1];
				ted7360_bitmap.line[y + yoff][2 + xoff] = c16_bitmap[(code >> 5) & 1];
				ted7360_bitmap.line[y + yoff][3 + xoff] = c16_bitmap[(code >> 4) & 1];
				ted7360_bitmap.line[y + yoff][4 + xoff] = c16_bitmap[(code >> 3) & 1];
				ted7360_bitmap.line[y + yoff][5 + xoff] = c16_bitmap[(code >> 2) & 1];
				ted7360_bitmap.line[y + yoff][6 + xoff] = c16_bitmap[(code >> 1) & 1];
				ted7360_bitmap.line[y + yoff][7 + xoff] = c16_bitmap[code & 1];
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic_dma_read.handler(bitmapaddr + ch * 8 + y);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff, (char) c16_bitmap[code >> 7]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(1 + xoff, (char) c16_bitmap[(code >> 6) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(2 + xoff, (char) c16_bitmap[(code >> 5) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(3 + xoff, (char) c16_bitmap[(code >> 4) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(4 + xoff, (char) c16_bitmap[(code >> 3) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(5 + xoff, (char) c16_bitmap[(code >> 2) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(6 + xoff, (char) c16_bitmap[(code >> 1) & 1]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(7 + xoff, (char) c16_bitmap[code & 1]);
			}
		}
	}
	
	static void ted7360_draw_bitmap_multi (int ybegin, int yend, int ch, int yoff, int xoff)
	{
		int y, code=0xFF;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic_dma_read (bitmapaddr + ch * 8 + y);
				ted7360_bitmap.line[y + yoff][xoff] =
					ted7360_bitmap.line[y + yoff][xoff + 1] = bitmapmulti[code >> 6];
				ted7360_bitmap.line[y + yoff][xoff + 2] =
					ted7360_bitmap.line[y + yoff][xoff + 3] = bitmapmulti[(code >> 4) & 3];
				ted7360_bitmap.line[y + yoff][xoff + 4] =
					ted7360_bitmap.line[y + yoff][xoff + 5] = bitmapmulti[(code >> 2) & 3];
				ted7360_bitmap.line[y + yoff][xoff + 6] =
					ted7360_bitmap.line[y + yoff][xoff + 7] = bitmapmulti[code & 3];
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic_dma_read.handler(bitmapaddr + ch * 8 + y);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff, (char) bitmapmulti[code >> 6]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 1, (char) bitmapmulti[code >> 6]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 2, (char) bitmapmulti[(code >> 4) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 3, (char) bitmapmulti[(code >> 4) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 4, (char) bitmapmulti[(code >> 2) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 5, (char) bitmapmulti[(code >> 2) & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 6, (char) bitmapmulti[code & 3]);
				new UShortPtr(Machine.scrbitmap.line[y + yoff]).write(xoff + 7, (char) bitmapmulti[code & 3]);
			}
		}
	}
	
	static void ted7360_draw_cursor (int ybegin, int yend, int yoff, int xoff, int color)
	{
		int y;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				memset (ted7360_bitmap.line[yoff + y] + xoff, color, 8);
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				/*TODO*///memset16 (new UShortPtr(ted7360_bitmap.line[yoff + y], xoff), color, 8);
                                memset(new UShortPtr(Machine.scrbitmap.line[yoff + y], xoff), color, 8);
			}
		}
	}
	
	static void ted7360_drawlines (int first, int last)
	{
	    int line, vline, end;
	    int attr=0xFF, ch=0xFF, c1, c2, ecm;
	    int offs, yoff, xoff, ybegin, yend, xbegin=0, xend;
	    int i;
	    
	    lastline = last;
	    
	    if (osd_skip_this_frame () != 0)
		return;
	    
	    /* top part of display not rastered */
	    first -= TED7360_YPOS;
	    last -= TED7360_YPOS;
	    if ((first >= last) || (last <= 0))
		return;
	    if (first < 0)
		first = 0;
	    
	    if (SCREENON() == 0)
	    {
	/*	if (Machine.color_depth == 8)
		{
		    for (line = first; (line < last) && (line < ted7360_bitmap.height); line++)
			memset (ted7360_bitmap.line[line], Machine.pens[0], ted7360_bitmap.width);
		}
		else
	*/	{
		    for (line = first; (line < last) && (line < Machine.scrbitmap.height); line++){
			/*TODO*///memset16 (ted7360_bitmap.line[line], Machine.pens[0], ted7360_bitmap.width);
                        memset(Machine.scrbitmap.line[line], Machine.pens[0], Machine.scrbitmap.width);
                    }
		}
		return;
	    }
	    
	    if (COLUMNS40() != 0){
		xbegin = XPOS; xend = xbegin + 320;
            } else {
		xbegin = XPOS + 7; xend = xbegin + 304;
            }
	    
	    if (last < y_begin)
		end = last;
	    else
		end = y_begin + YPOS;
	/*    if (Machine.color_depth == 8)
	    {
		for (line = first; line < end; line++)
		    memset (ted7360_bitmap.line[line], Machine.pens[FRAMECOLOR],
			    ted7360_bitmap.width);
	    }
	    else
	*/   {
		for (line = first; line < end; line++){
		    /*TODO*///memset16 (ted7360_bitmap.line[line], Machine.pens[FRAMECOLOR()], ted7360_bitmap.width);
                    memset(Machine.scrbitmap.line[line], Machine.pens[FRAMECOLOR()], Machine.scrbitmap.width);
                }
	    }
	    if (LINES25() != 0)
	    {
		vline = line - y_begin - YPOS;
	    }
	    else
	    {
		vline = line - y_begin - YPOS + 8 - VERTICALPOS();
	    }
	    if (last < y_end + YPOS)
		end = last;
	    else
		end = y_end + YPOS;
	    for (; line < end; vline = (vline + 8) & ~7, line = line + 1 + yend - ybegin)
	    {
		offs = (vline >> 3) * 40;
		ybegin = vline & 7;
		yoff = line - ybegin;
		yend = (yoff + 7 < end) ? 7 : (end - yoff - 1);
			/* rendering 39 characters */
		/* left and right borders are overwritten later */
		
		for (xoff = x_begin + XPOS; xoff < x_end + XPOS; xoff += 8, offs++)
		{
		    if (HIRESON() != 0)
		    {
			ch = vic_dma_read.handler((videoaddr | 0x400) + offs);
			attr = vic_dma_read.handler(((videoaddr) + offs));
			c1 = ((ch >> 4) & 0xf) | (attr << 4);
			c2 = (ch & 0xf) | (attr & 0x70);
			bitmapmulti[1] = c16_bitmap[1] = Machine.pens[c1 & 0x7f];
			bitmapmulti[2] = c16_bitmap[0] = Machine.pens[c2 & 0x7f];
			if (MULTICOLORON() != 0)
			{
			    ted7360_draw_bitmap_multi (ybegin, yend, offs, yoff, xoff);
			}
			else
			{
			    ted7360_draw_bitmap (ybegin, yend, offs, yoff, xoff);
			}
		    }
		    else
		    {
			ch = vic_dma_read.handler((videoaddr | 0x400) + offs);
			attr = vic_dma_read.handler(((videoaddr) + offs));
			// levente harsfalvi's docu says cursor off in ecm and multicolor
			if (ECMON() != 0) {
			    // hardware reverse off
			    ecm = ch >> 6;
			    ecmcolor[0] = colors[ecm];
			    ecmcolor[1] = Machine.pens[attr & 0x7f];
			    ted7360_draw_character (ybegin, yend, ch & ~0xC0, yoff, xoff, ecmcolor);
			} else if (MULTICOLORON() != 0) {
			    // hardware reverse off
			    if ((attr & 8) != 0) {
				multi[3] = Machine.pens[attr & 0x77];
				ted7360_draw_character_multi (ybegin, yend, ch, yoff, xoff);
			    } else {
				mono[1] = Machine.pens[attr & 0x7f];
				ted7360_draw_character (ybegin, yend, ch, yoff, xoff,
							mono);
			    }
			} else if (cursor1 && (offs == CURSOR1POS())) {
	/*TODO*///#ifndef GFX
			    ted7360_draw_cursor (ybegin, yend, yoff, xoff,
						 Machine.pens[attr & 0x7f]);
	/*TODO*///#else
			    drawgfx (Machine.scrbitmap, cursorelement, 0, Machine.pens[attr & 0x7f], 0, 0,
				     xoff, yoff, null, TRANSPARENCY_NONE, 0);
	/*TODO*///#endif
			} else if (REVERSEON()!=0 && (ch & 0x80)!=0) {
			    monoinversed[0] = Machine.pens[attr & 0x7f];
			    if (cursor1 && (attr & 0x80)!=0)
				ted7360_draw_cursor (ybegin, yend, yoff, xoff, monoinversed[0]);
			    else
				ted7360_draw_character (ybegin, yend, ch & ~0x80, yoff, xoff,
							monoinversed);
			}
			else
			{
			    mono[1] = Machine.pens[attr & 0x7f];
			    if (cursor1 && (attr & 0x80)!=0)
				ted7360_draw_cursor (ybegin, yend, yoff, xoff, mono[0]);
			    else
				ted7360_draw_character (ybegin, yend, ch, yoff, xoff, mono);
			}
		    }
		}
	/*	if (Machine.color_depth == 8)
		{
		    for (i = ybegin; i <= yend; i++)
		    {
			memset (ted7360_bitmap.line[yoff + i], Machine.pens[FRAMECOLOR], xbegin);
			memset (ted7360_bitmap.line[yoff + i] + xend, Machine.pens[FRAMECOLOR],
				ted7360_bitmap.width - xend);
		    }
		}
		else
	*/	{
		    for (i = ybegin; i <= yend; i++)
		    {
			/*TODO*///memset16 (ted7360_bitmap.line[yoff + i], Machine.pens[FRAMECOLOR()], xbegin);
                        memset(Machine.scrbitmap.line[yoff + i], Machine.pens[FRAMECOLOR()], xbegin);
			/*TODO*///memset16 (new UShortPtr(ted7360_bitmap.line[yoff + i], xend), Machine.pens[FRAMECOLOR()], ted7360_bitmap.width - xend);
                        memset(new UShortPtr(Machine.scrbitmap.line[yoff + i], xend), Machine.pens[FRAMECOLOR()], Machine.scrbitmap.width - xend);
		    }
		}
	    }
	    if (last < Machine.scrbitmap.height)
		end = last;
	    else
		end = Machine.scrbitmap.height;
	/*    if (Machine.color_depth == 8)
	    {
		for (; line < end; line++)
		    memset (ted7360_bitmap.line[line], Machine.pens[FRAMECOLOR],
			    ted7360_bitmap.width);
	    }
	    else
	*/    {
		for (; line < end; line++){
		    /*TODO*///memset16 (ted7360_bitmap.line[line], Machine.pens[FRAMECOLOR()], ted7360_bitmap.width);
                    memset(Machine.scrbitmap.line[line], Machine.pens[FRAMECOLOR()], Machine.scrbitmap.width);
                }
	    }
	}
	
	static void ted7360_draw_text (mame_bitmap bitmap, String text, int[] y)
	{
            //System.out.println("ted7360_draw_text "+text);
		int x, x0, y1t, width = (Machine.visible_area.max_x -
								 Machine.visible_area.min_x) / Machine.uifont.width;
	
		if (text != null && text.length() != 0)
		{
			x = text.length();
			y[0] -= Machine.uifont.height * ((x + width - 1) / width);
			y1t = y[0] + Machine.uifont.height;
			x = 0;
			while ( x < text.length() )
			{
				for (x0 = Machine.visible_area.min_x;
					/*TODO*/// text[x] && 
                                        (x0 < Machine.visible_area.max_x -
								 Machine.uifont.width);
					 x++, x0 += Machine.uifont.width)
				{
					drawgfx (bitmap, Machine.uifont, text.charAt(x), 0, 0, 0, x0, y1t, null,
							 TRANSPARENCY_NONE, 0);
				}
				y1t += Machine.uifont.height;
			}
		}
	}
	
	public static InterruptPtr ted7360_raster_interrupt = new InterruptPtr() {
            public int handler() {
                int y;
		String text = "";
	
		rasterline++;
		rastertime = timer_get_time ();
		if (rasterline >= lines)
		{
			rasterline = 0;
			ted7360_drawlines (lastline, TED7360_LINES);
			lastline = 0;
	
			y = Machine.visible_area.max_y + 1 - Machine.uifont.height;
	
		/*TODO*///	vc20_tape_status (text, sizeof (text));
			ted7360_draw_text (Machine.scrbitmap, text, new int[]{y});
	
			if (REAL_C1551() != 0) {
		/*TODO*///		vc1541_drive_status (text, sizeof (text));
				ted7360_draw_text (Machine.scrbitmap, text, new int[]{y});
			}
	
		/*TODO*///	cbm_drive_0_status (text, text.length);
			ted7360_draw_text (Machine.scrbitmap, text, new int[]{y});
	
		/*TODO*///	cbm_drive_1_status (text, text.length);
			ted7360_draw_text (Machine.scrbitmap, text, new int[]{y});
		}
		if (rasterline == C16_2_RASTERLINE (RASTERLINE()))
		{
			ted7360_drawlines (lastline, rasterline);
			ted7360_set_interrupt (2);
		}
		return ignore_interrupt.handler();
            }
        };
	
	
	public static VhUpdatePtr ted7360_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                // nothing to do
            }
        };
	
}
