/***************************************************************************

	Video Interface Chip 6567 (6566 6569 and some other)
	PeT mess@utanet.at

***************************************************************************/
/* mos videochips
  vic (6560 NTSC, 6561 PAL)
  used in commodore vic20

  vic II
   6566 NTSC
    no dram refresh?
   6567 NTSC
   6569 PAL-B
   6572 PAL-N
   6573 PAL-M
   8562 NTSC
   8565 PAL
  used in commodore c64
  complete different to vic

  ted
   7360/8360 (NTSC-M, PAL-B by same chip ?)
   8365 PAL-N
   8366 PAL-M
  used in c16 c116 c232 c264 plus4 c364
  based on the vic II
  but no sprites and not register compatible
  includes timers, input port, sound generators
  memory interface, dram refresh, clock generation

  vic IIe
   8564 NTSC-M
   8566 PAL-B
   8569 PAL-N
  used in commodore c128
  vic II with some additional features
   3 programmable output pins k0 k1 k2

  vic III
   4567
  used in commodore c65 prototype
  vic II compatible (mode only?)
  several additional features
   different resolutions, more colors, ...
   (maybe like in the amiga graphic chip docu)

  vdc
   8563
   8568 (composite video and composite sync)
  second graphic chip in c128
  complete different to above chips
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.video.osd_skip_this_frame;
import static common.libc.cstring.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.includes.c64H.*;
import static mess056.includes.vic6567H.*;

public class vic6567
{
/*TODO*///	#define VERBOSE_DBG 1
/*TODO*///	
/*TODO*///	/* lightpen values */
/*TODO*///	
/*TODO*///	
/*TODO*///	/*#define GFX */
/*TODO*///	
/*TODO*///	#define VREFRESHINLINES 28
/*TODO*///	
	public static int VIC2_YPOS = 50;
	public static int RASTERLINE_2_C64(int a){ return (a); }
	public static int C64_2_RASTERLINE(int a){ return (a); }
	public static int XPOS = 8;
	public static int YPOS = 8;

	/* lightpen delivers values from internal counters
	 * they do not start with the visual area or frame area */
	public static int VIC2_MAME_XPOS = 0;
	public static int VIC2_MAME_YPOS = 0;
	public static int VIC6567_X_BEGIN = 38;
	public static int VIC6567_Y_BEGIN = -6;			   /* first 6 lines after retrace not for lightpen! */
	public static int VIC6569_X_BEGIN = 38;
	public static int VIC6569_Y_BEGIN = -6;
	public static int VIC2_X_BEGIN(){ return (vic2.pal?VIC6569_X_BEGIN:VIC6567_X_BEGIN); }
	public static int VIC2_Y_BEGIN(){ return (vic2.pal?VIC6569_Y_BEGIN:VIC6567_Y_BEGIN); }
	public static int VIC2_X_VALUE(){ return ((LIGHTPEN_X_VALUE()+VIC2_X_BEGIN()+VIC2_MAME_XPOS)/2); }
	public static int VIC2_Y_VALUE(){ return LIGHTPEN_Y_VALUE()+VIC2_Y_BEGIN()+VIC2_MAME_YPOS; }
	
/*TODO*///	#define VIC2E_K0_LEVEL (vic2.reg[0x2f]&1)
/*TODO*///	#define VIC2E_K1_LEVEL (vic2.reg[0x2f]&2)
/*TODO*///	#define VIC2E_K2_LEVEL (vic2.reg[0x2f]&4)
/*TODO*///	
/*TODO*///	/*#define VIC3_P5_LEVEL (vic2.reg[0x30]&0x20) */
	public static int VIC3_BITPLANES(){ return (vic2.reg[0x31]&0x10); }
/*TODO*///	#define VIC3_80COLUMNS (vic2.reg[0x31]&0x80)
/*TODO*///	#define VIC3_LINES	   ((vic2.reg[0x31]&0x19)==0x19?400:200)
/*TODO*///	#define VIC3_BITPLANES_WIDTH (vic2.reg[0x31]&0x80?640:320)
/*TODO*///	
/*TODO*///		 /*#define VIC2E_TEST (vic2[0x30]&2) */
/*TODO*///	#define DOUBLE_CLOCK (vic2.reg[0x30]&1)
/*TODO*///	
/*TODO*///	/* sprites 0 .. 7 */
	public static int SPRITE_BASE_X_SIZE = 24;
/*TODO*///	#define SPRITE_BASE_Y_SIZE 21
	public static int SPRITEON(int nr){ return (vic2.reg[0x15]&(1<<nr)); }
	public static int SPRITE_Y_EXPAND(int nr){ return (vic2.reg[0x17]&(1<<nr)); }
/*TODO*///	#define SPRITE_Y_SIZE(nr) (SPRITE_Y_EXPAND(nr)?2*21:21)
	public static int SPRITE_X_EXPAND(int nr){ return (vic2.reg[0x1d]&(1<<nr)); }
	public static int SPRITE_X_SIZE(int nr){ return (SPRITE_X_EXPAND(nr)!=0?2*24:24); }
	public static int SPRITE_X_POS(int nr){ 
            int par1=vic2.reg[(nr)*2];
            int par2=((vic2.reg[0x10]&(1<<nr))!=0?0x100:0);
            return ( (par1|par2)-24+XPOS ); 
        }
	public static int SPRITE_Y_POS(int nr){ return (vic2.reg[1+2*(nr)]-50+YPOS); }
	public static int SPRITE_MULTICOLOR(int nr){ return (vic2.reg[0x1c]&(1<<nr)); }
	public static int SPRITE_PRIORITY(int nr){ return (vic2.reg[0x1b]&(1<<nr)); }
	public static int SPRITE_MULTICOLOR1(){ return (vic2.reg[0x25]&0xf); }
	public static int SPRITE_MULTICOLOR2(){ return (vic2.reg[0x26]&0xf); }
	public static int SPRITE_COLOR(int nr){ return (vic2.reg[0x27+nr]&0xf); }
	public static int SPRITE_ADDR(int nr){ return (vic2.videoaddr+0x3f8+nr); }
	public static int SPRITE_BG_COLLISION(int nr){ return (vic2.reg[0x1f]&(1<<nr)); }
	public static int SPRITE_COLLISION(int nr){ return (vic2.reg[0x1e]&(1<<nr)); }
	public static void SPRITE_SET_BG_COLLISION(int nr){ vic2.reg[0x1f]|=(1<<nr); }
	public static void SPRITE_SET_COLLISION(int nr){ vic2.reg[0x1e]|=(1<<nr); }

	public static int SCREENON(){ return (vic2.reg[0x11]&0x10); }
	public static int VERTICALPOS(){ return (vic2.reg[0x11]&7); }
	public static int HORICONTALPOS(){ return (vic2.reg[0x16]&7); }
	public static int ECMON(){ return (vic2.reg[0x11]&0x40); }
	public static int HIRESON(){ return (vic2.reg[0x11]&0x20); }
	public static int MULTICOLORON(){ return (vic2.reg[0x16]&0x10); }
	public static int LINES25(){ return (vic2.reg[0x11]&8); }		   /* else 24 Lines */
/*TODO*///	public static int LINES(){ return (LINES25()!=0?25:24); }
/*TODO*///	#define YSIZE (LINES*8)
	public static int COLUMNS40(){ return (vic2.reg[0x16]&8); }	   /* else 38 Columns */
/*TODO*///	#define COLUMNS (COLUMNS40?40:38)
/*TODO*///	#define XSIZE (COLUMNS*8)

	public static int VIDEOADDR(){ return ( (vic2.reg[0x18]&0xf0)<<(10-4) ); }
	public static int CHARGENADDR(){ return ((vic2.reg[0x18]&0xe)<<10); }

	public static int RASTERLINE(){ return ( ((vic2.reg[0x11]&0x80)<<1)|vic2.reg[0x12]); }

	public static int BACKGROUNDCOLOR(){ return (vic2.reg[0x21]&0xf); }
	public static int MULTICOLOR1(){ return (vic2.reg[0x22]&0xf); }
	public static int MULTICOLOR2(){ return (vic2.reg[0x23]&0xf); }
	public static int FOREGROUNDCOLOR(){ return (vic2.reg[0x24]&0xf); }
	public static int FRAMECOLOR(){ return (vic2.reg[0x20]&0xf); }

	
	public static char vic2_palette[] =
	{
	/* black, white, red, cyan */
	/* purple, green, blue, yellow */
	/* orange, brown, light red, dark gray, */
	/* medium gray, light green, light blue, light gray */
	/* taken from the vice emulator */
		0x00, 0x00, 0x00,  0xfd, 0xfe, 0xfc,  0xbe, 0x1a, 0x24,  0x30, 0xe6, 0xc6,
		0xb4, 0x1a, 0xe2,  0x1f, 0xd2, 0x1e,  0x21, 0x1b, 0xae,  0xdf, 0xf6, 0x0a,
		0xb8, 0x41, 0x04,  0x6a, 0x33, 0x04,  0xfe, 0x4a, 0x57,  0x42, 0x45, 0x40,
		0x70, 0x74, 0x6f,  0x59, 0xfe, 0x59,  0x5f, 0x53, 0xfe,  0xa4, 0xa7, 0xa2
	};
        
        public static class _sprites 
		{
            public int on, x, y, xexpand, yexpand;
	
            public int repeat;						   /* y expand, line once drawn */
            public int line;						   /* 0 not painting, else painting */
	
            /* buffer for currently painted line */
            public int[] paintedline = new int[8];
            public char[][] bitmap = new char[8][SPRITE_BASE_X_SIZE * 2 / 8 + 1/*for simplier sprite collision detection*/];

        }
	
	public static class _vic2 {
		public int[] reg = new int[0x80];
		public boolean pal;
		public boolean vic2e;		     /* version with some port lines */
		public boolean vic3;
		public boolean on; /* rastering of the screen */

		public ReadHandlerPtr dma_read;
                public ReadHandlerPtr dma_read_color;
                public ReadHandlerPtr interrupt;
/*TODO*///		void (*port_changed)(int);

		public int lines;
		public timer_entry lightpentimer;

		public int chargenaddr, videoaddr;

		public mame_bitmap bitmap;		   /* Machine.scrbitmap for speedup */
		public int x_begin, x_end;
		public int y_begin, y_end;

		public int[] c64_bitmap=new int[2], bitmapmulti=new int[4], mono=new int[2],
			multi=new int[4], ecmcolor=new int[2], colors=new int[4], spritemulti=new int[4];

		public int lastline, rasterline;

		/* background/foreground for sprite collision */
		public UBytePtr[] screen=new UBytePtr[216];
                public int[] shift=new int[216];
	
		/* convert multicolor byte to background/foreground for sprite collision */
		public int[] foreground = new int[256];
		public int[] expandx = new int[256];
		public int[] expandx_multi = new int[256];

		/* converts sprite multicolor info to info for background collision checking */
		public int[] multi_collision = new int[256];
	

		public _sprites[] sprites = new _sprites[8];
                
                public _vic2(){
                    for (int _i=0 ; _i<8 ; _i++)
                        sprites[_i] = new _sprites();
                }
	};
        
        public static _vic2 vic2 = new _vic2();
        
	
	public static int vic2_getforeground (int y, int x)
	{
	    return ((vic2.screen[y].read(x >> 3) << 8)
		    | (vic2.screen[y].read((x >> 3) + 1))) >> (8 - (x & 7));
	}
	
	public static int vic2_getforeground16 (int y, int x)
	{
		return ((vic2.screen[y].read(x >> 3) << 16)
				| (vic2.screen[y].read((x >> 3) + 1) << 8)
				| (vic2.screen[y].read((x >> 3) + 2))) >> (8 - (x & 7));
	}
	
/*TODO*///	#if 0
/*TODO*///	static void vic2_setforeground (int y, int x, int value)
/*TODO*///	{
/*TODO*///		vic2.screen[y][x >> 3] = value;
/*TODO*///	}
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	static void vic2_drawlines (int first, int last);
	
	public static void vic6567_init (int chip_vic2e, int pal,
					   ReadHandlerPtr dma_read, ReadHandlerPtr dma_read_color,
					   ReadHandlerPtr irq)
	{
		//memset(&vic2, 0, sizeof(vic2));
                vic2 = new _vic2();
	
		vic2.lines = VIC2_LINES();
	
		vic2.dma_read = dma_read;
		vic2.dma_read_color = dma_read_color;
		vic2.interrupt = irq;
		vic2.vic2e = chip_vic2e!=0?true:false;
		vic2.pal = pal!=0?true:false;
		vic2.on = true;
	}
	
/*TODO*///	void vic2_set_rastering(int onoff)
/*TODO*///	{
/*TODO*///		vic2.on=onoff;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vic2e_k0_r (void)
/*TODO*///	{
/*TODO*///		return VIC2E_K0_LEVEL;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vic2e_k1_r (void)
/*TODO*///	{
/*TODO*///		return VIC2E_K1_LEVEL;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int vic2e_k2_r (void)
/*TODO*///	{
/*TODO*///		return VIC2E_K2_LEVEL;
/*TODO*///	}
/*TODO*///	
/*TODO*///	#if 0
/*TODO*///	int vic3_p5_r (void)
/*TODO*///	{
/*TODO*///		return VIC3_P5_LEVEL;
/*TODO*///	}
/*TODO*///	#endif
	
	public static void vic2_set_interrupt (int mask)
	{
		if ((((vic2.reg[0x19] ^ mask) & vic2.reg[0x1a] & 0xf)) != 0)
		{
			if ((vic2.reg[0x19] & 0x80)==0)
			{
/*TODO*///				DBG_LOG (2, "vic2", ("irq start %.2x\n", mask));
				vic2.reg[0x19] |= 0x80;
				vic2.interrupt.handler(1);
			}
		}
		vic2.reg[0x19] |= mask;
	}
	
	public static void vic2_clear_interrupt (int mask)
	{
		vic2.reg[0x19] &= ~mask;
		if ((vic2.reg[0x19] & 0x80)!=0 && (vic2.reg[0x19] & vic2.reg[0x1a] & 0xf)==0)
		{
/*TODO*///			DBG_LOG (2, "vic2", ("irq end %.2x\n", mask));
			vic2.reg[0x19] &= ~0x80;
			vic2.interrupt.handler(0);
		}
	}
	
	public static void vic2_lightpen_write (int level)
	{
		/* calculate current position, write it and raise interrupt */
	}
	
	public static timer_callback vic2_timer_timeout = new timer_callback() {
            public void handler(int which) {
        
/*TODO*///		DBG_LOG (3, "vic2 ", ("timer %d timeout\n", which));
		switch (which)
		{
		case 1:						   /* light pen */
			/* and diode must recognize light */
			if (true)
			{
				vic2.reg[0x13] = VIC2_X_VALUE();
				vic2.reg[0x14] = VIC2_Y_VALUE();
			}
			vic2_set_interrupt (8);
			break;
		}
            }
	};
	
	public static int vic2_frame_interrupt ()
	{
		return ignore_interrupt.handler();
            //return 0;
	}
	
	public static WriteHandlerPtr vic2_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
            
/*TODO*///		DBG_LOG (2, "vic write", ("%.2x:%.2x\n", offset, data));
		offset &= 0x3f;
                //if (offset==0x18)
                //    System.out.println("0x18!!!!");
                switch (offset)
                {
                case 1:
                case 3:
                case 5:
                case 7:
                case 9:
                case 0xb:
                case 0xd:
                case 0xf:
                        /* sprite y positions */
                        if (vic2.reg[offset] != data) {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.sprites[offset/2].y=SPRITE_Y_POS(offset/2);
                        }
                        break;
                case 0:
                case 2:
                case 4:
                case 6:
                case 8:
                case 0xa:
                case 0xc:
                case 0xe:
                        /* sprite x positions */
                        if (vic2.reg[offset] != data) {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.sprites[offset/2].x=SPRITE_X_POS(offset/2);
                        }
                        break;
                case 0x10:
                        /* sprite x positions */
                        if (vic2.reg[offset] != data) {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.sprites[0].x=SPRITE_X_POS(0);
                                vic2.sprites[1].x=SPRITE_X_POS(1);
                                vic2.sprites[2].x=SPRITE_X_POS(2);
                                vic2.sprites[3].x=SPRITE_X_POS(3);
                                vic2.sprites[4].x=SPRITE_X_POS(4);
                                vic2.sprites[5].x=SPRITE_X_POS(5);
                                vic2.sprites[6].x=SPRITE_X_POS(6);
                                vic2.sprites[7].x=SPRITE_X_POS(7);
                        }
                        break;
                case 0x17:						   /* sprite y size */
                case 0x1d:						   /* sprite x size */
                case 0x1b:						   /* sprite background priority */
                case 0x1c:						   /* sprite multicolor mode select */
                case 0x27:
                case 0x28:						   /* sprite colors */
                case 0x29:
                case 0x2a:
                case 0x2b:
                case 0x2c:
                case 0x2d:
                case 0x2e:
                        if (vic2.reg[offset] != data)
                        {
                                vic2.reg[offset] = data;
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                        }
                        break;
                case 0x25:
                        if (vic2.reg[offset] != data)
                        {
                                vic2.reg[offset] = data;
                                vic2.spritemulti[1] = Machine.pens[SPRITE_MULTICOLOR1()];
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                        }
                        break;
                case 0x26:
                        if (vic2.reg[offset] != data)
                        {
                                vic2.reg[offset] = data;
                                vic2.spritemulti[3] = Machine.pens[SPRITE_MULTICOLOR2()];
                                if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                        }
                        break;
                case 0x19:
                        vic2_clear_interrupt (data & 0xf);
                        break;
                case 0x1a:						   /* irq mask */
                        vic2.reg[offset] = data;
                        vic2_set_interrupt(0); //beamrider needs this
                        break;
                case 0x11:
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                if (LINES25()!=0)
                                {
                                        vic2.y_begin = 0;
                                        vic2.y_end = vic2.y_begin + 200;
                                }
                                else
                                {
                                        vic2.y_begin = 4;
                                        vic2.y_end = vic2.y_begin + 192;
                                }
                        }
                        break;
                case 0x12:
                        if (data != vic2.reg[offset])
                        {
                                vic2.reg[offset] = data;
                        }
                        break;
                case 0x16:
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                if (COLUMNS40()!=0)
                                {
                                        vic2.x_begin = 0;
                                        vic2.x_end = vic2.x_begin + 320;
                                }
                                else
                                {
                                        vic2.x_begin = HORICONTALPOS();
                                        vic2.x_end = vic2.x_begin + 320;
                                }
                        }
                        break;
                case 0x18:
                    //System.out.println("Dentro!");
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.videoaddr = VIDEOADDR();
                                vic2.chargenaddr = CHARGENADDR();
                        }
                        break;
                case 0x21:						   /* backgroundcolor */
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.mono[0] = vic2.bitmapmulti[0] = vic2.multi[0] =
                                        vic2.colors[0] = Machine.pens[BACKGROUNDCOLOR()];
                        }
                        break;
                case 0x22:						   /* background color 1 */
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.multi[1] = vic2.colors[1] = Machine.pens[MULTICOLOR1()];
                        }
                        break;
                case 0x23:						   /* background color 2 */
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.multi[2] = vic2.colors[2] = Machine.pens[MULTICOLOR2()];
                        }
                        break;
                case 0x24:						   /* background color 3 */
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                                vic2.colors[3] = Machine.pens[FOREGROUNDCOLOR()];
                        }
                        break;
                case 0x20:						   /* framecolor */
                        if (vic2.reg[offset] != data)
                        {
                                if (vic2.on)
                                        vic2_drawlines (vic2.lastline, vic2.rasterline);
                                vic2.reg[offset] = data;
                        }
                        break;
                case 0x2f:
                        if (vic2.vic2e)
                        {
/*TODO*///                                DBG_LOG (2, "vic write", ("%.2x:%.2x\n", offset, data));
                                vic2.reg[offset] = data;
                        }
                        break;
                case 0x30:
                        if (vic2.vic2e)
                        {
                                vic2.reg[offset] = data;
                        }
                        break;
                case 0x31:
                case 0x32:
                case 0x33:
                case 0x34:
                case 0x35:
                case 0x36:
                case 0x37:
                case 0x38:
                case 0x39:
                case 0x3a:
                case 0x3b:
                case 0x3c:
                case 0x3d:
                case 0x3e:
                case 0x3f:
                        vic2.reg[offset] = data;
/*TODO*///                        DBG_LOG (2, "vic write", ("%.2x:%.2x\n", offset, data));
                        break;
                default:
                        vic2.reg[offset] = data;
                        break;
                }
            }
        };
	
	public static ReadHandlerPtr vic2_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {

		int val = 0;
                offset &= 0x3f;
                switch (offset)
                {
                case 0x11:
                        val = (vic2.reg[offset] & ~0x80) | ((vic2.rasterline & 0x100) >> 1);
                        if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                        break;
                case 0x12:
                        val = vic2.rasterline & 0xff;
                        if (vic2.on) vic2_drawlines (vic2.lastline, vic2.rasterline);
                        break;
                case 0x16:
                        val = vic2.reg[offset] | 0xc0;
                        break;
                case 0x18:
                        val = vic2.reg[offset] | 1;
                        break;
                case 0x19:						   /* interrupt flag register */
                        /*    vic2_clear_interrupt(0xf); */
                        val = vic2.reg[offset] | 0x70;
                        break;
                case 0x1a:
                        val = vic2.reg[offset] | 0xf0;
                        break;
                case 0x1e:						   /* sprite to sprite collision detect */
                        val = vic2.reg[offset];
                        vic2.reg[offset] = 0;
                        vic2_clear_interrupt (4);
                        break;
                case 0x1f:						   /* sprite to background collision detect */
                        val = vic2.reg[offset];
                        vic2.reg[offset] = 0;
                        vic2_clear_interrupt (2);
                        break;
                case 0x20:
                case 0x21:
                case 0x22:
                case 0x23:
                case 0x24:
                case 0x25:
                case 0x26:
                case 0x27:
                case 0x28:
                case 0x29:
                case 0x2a:
                case 0x2b:
                case 0x2c:
                case 0x2d:
                case 0x2e:
                        val = vic2.reg[offset];
                        break;
                case 0x2f:
                case 0x30:
                        if (vic2.vic2e) {
                                val = vic2.reg[offset];
/*TODO*///                                DBG_LOG (2, "vic read", ("%.2x:%.2x\n", offset, val));
                        } else {
                                val = 0xff;
                        }
                        break;
                case 0x31:
                case 0x32:
                case 0x33:
                case 0x34:
                case 0x35:
                case 0x36:
                case 0x37:
                case 0x38:
                case 0x39:
                case 0x3a:
                case 0x3b:
                case 0x3c:
                case 0x3d:
                case 0x3e:
                case 0x3f:						   /* not used */
                        val = vic2.reg[offset];
/*TODO*///                        DBG_LOG (2, "vic read", ("%.2x:%.2x\n", offset, val));
                        break;
                default:
                        val = vic2.reg[offset];
                }
/*TODO*///                if ((offset != 0x11) && (offset != 0x12))
/*TODO*///                        DBG_LOG (2, "vic read", ("%.2x:%.2x\n", offset, val));
                return val;
            }
                        
        };
	
	public static VhStartPtr vic2_vh_start = new VhStartPtr() {
            public int handler() {
            
		int i;
	
		vic2.bitmap = Machine.scrbitmap;
	
		if (vic2.vic3) {
			vic2.screen[0] = new UBytePtr(216 * 656 / 8);
	
			if (vic2.screen[0]==null)
				return 1;
			for (i = 1; i < 216; i++)
				vic2.screen[i] = new UBytePtr(vic2.screen[i - 1], 656 / 8);
		} else {
			vic2.screen[0] = new UBytePtr(216 * 336 / 8);
	
			if (vic2.screen[0]==null)
				return 1;
			for (i = 1; i < 216; i++)
				vic2.screen[i] = new UBytePtr(vic2.screen[i - 1], 336 / 8);
                        /*for (i = 0; i < 216; i++)
				vic2.screen[i] = new UBytePtr(216 * 656 / 8);*/
		}
	
		for (i = 0; i < 256; i++)
		{
			vic2.foreground[i] = 0;
			if ((i & 3) > 1)
				vic2.foreground[i] |= 0x3;
			if ((i & 0xc) > 0x4)
				vic2.foreground[i] |= 0xc;
			if ((i & 0x30) > 0x10)
				vic2.foreground[i] |= 0x30;
			if ((i & 0xc0) > 0x40)
				vic2.foreground[i] |= 0xc0;
		}
		for (i = 0; i < 256; i++)
		{
			vic2.expandx[i] = 0;
			if ((i & 1) != 0)
				vic2.expandx[i] |= 3;
			if ((i & 2) != 0)
				vic2.expandx[i] |= 0xc;
			if ((i & 4) != 0)
				vic2.expandx[i] |= 0x30;
			if ((i & 8) != 0)
				vic2.expandx[i] |= 0xc0;
			if ((i & 0x10) != 0)
				vic2.expandx[i] |= 0x300;
			if ((i & 0x20) != 0)
				vic2.expandx[i] |= 0xc00;
			if ((i & 0x40) != 0)
				vic2.expandx[i] |= 0x3000;
			if ((i & 0x80) != 0)
				vic2.expandx[i] |= 0xc000;
		}
		for (i = 0; i < 256; i++)
		{
			vic2.expandx_multi[i] = 0;
			if ((i & 1) != 0)
				vic2.expandx_multi[i] |= 5;
			if ((i & 2) != 0)
				vic2.expandx_multi[i] |= 0xa;
			if ((i & 4) != 0)
				vic2.expandx_multi[i] |= 0x50;
			if ((i & 8) != 0)
				vic2.expandx_multi[i] |= 0xa0;
			if ((i & 0x10) != 0)
				vic2.expandx_multi[i] |= 0x500;
			if ((i & 0x20) != 0)
				vic2.expandx_multi[i] |= 0xa00;
			if ((i & 0x40) != 0)
				vic2.expandx_multi[i] |= 0x5000;
			if ((i & 0x80) != 0)
				vic2.expandx_multi[i] |= 0xa000;
		}
		for (i = 0; i < 256; i++)
		{
			vic2.multi_collision[i] = 0;
			if ((i & 3) != 0)
				vic2.multi_collision[i] |= 3;
			if ((i & 0xc) != 0)
				vic2.multi_collision[i] |= 0xc;
			if ((i & 0x30) != 0)
				vic2.multi_collision[i] |= 0x30;
			if ((i & 0xc0) != 0)
				vic2.multi_collision[i] |= 0xc0;
		}
		return 0;
            }
        };
	
	public static VhStopPtr vic2_vh_stop = new VhStopPtr() {
            public void handler() {
                vic2.screen[0] = null;
            }
        };
	
	public static void vic2_draw_character (int ybegin, int yend, int ch, int yoff, int xoff, int[] color)
	{
		int y, code;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read (vic2.chargenaddr + ch * 8 + y);
				vic2.screen[y + yoff][xoff >> 3] = code;
				vic2.bitmap.line[y + yoff][xoff] = color[code >> 7];
				vic2.bitmap.line[y + yoff][1 + xoff] = color[(code >> 6) & 1];
				vic2.bitmap.line[y + yoff][2 + xoff] = color[(code >> 5) & 1];
				vic2.bitmap.line[y + yoff][3 + xoff] = color[(code >> 4) & 1];
				vic2.bitmap.line[y + yoff][4 + xoff] = color[(code >> 3) & 1];
				vic2.bitmap.line[y + yoff][5 + xoff] = color[(code >> 2) & 1];
				vic2.bitmap.line[y + yoff][6 + xoff] = color[(code >> 1) & 1];
				vic2.bitmap.line[y + yoff][7 + xoff] = color[code & 1];
			}
		}
		else
	*/	{
            //System.out.println("vic2.chargenaddrNR="+vic2.chargenaddr);
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read.handler(vic2.chargenaddr + ch * 8 + y);
				(vic2.screen[y + yoff]).write(xoff >> 3, (char) code);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(xoff, (char) color[code >> 7]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(1 + xoff, (char) color[(code >> 6) & 1]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(2 + xoff, (char) color[(code >> 5) & 1]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(3 + xoff, (char) color[(code >> 4) & 1]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(4 + xoff, (char) color[(code >> 3) & 1]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(5 + xoff, (char) color[(code >> 2) & 1]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(6 + xoff, (char) color[(code >> 1) & 1]);
				new UShortPtr(vic2.bitmap.line[y + yoff]).write(7 + xoff, (char) color[code & 1]);
			}
		}
	}
	
	public static void vic2_draw_character_multi (int ybegin, int yend, int ch, int yoff, int xoff)
	{
		int y, code;
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read (vic2.chargenaddr + ch * 8 + y);
				vic2.screen[y + yoff][xoff >> 3] = vic2.foreground[code];
				vic2.bitmap.line[y + yoff][xoff] =
					vic2.bitmap.line[y + yoff][xoff + 1] = vic2.multi[code >> 6];
				vic2.bitmap.line[y + yoff][xoff + 2] =
					vic2.bitmap.line[y + yoff][xoff + 3] = vic2.multi[(code >> 4) & 3];
				vic2.bitmap.line[y + yoff][xoff + 4] =
					vic2.bitmap.line[y + yoff][xoff + 5] = vic2.multi[(code >> 2) & 3];
				vic2.bitmap.line[y + yoff][xoff + 6] =
					vic2.bitmap.line[y + yoff][xoff + 7] = vic2.multi[code & 3];
			}
		}
		else
	*/	{
                        //System.out.println("vic2.chargenaddr="+vic2.chargenaddr);
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read.handler(vic2.chargenaddr + ch * 8 + y);
				(vic2.screen[y + yoff]).write(xoff >> 3, (char) vic2.foreground[code]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff, (char) vic2.multi[code >> 6]); 
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 1, (char) vic2.multi[code >> 6]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 2, (char) vic2.multi[(code >> 4) & 3]); 
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 3, (char) vic2.multi[(code >> 4) & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 4, (char) vic2.multi[(code >> 2) & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 5, (char) vic2.multi[(code >> 2) & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 6, (char) vic2.multi[code & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 7, (char) vic2.multi[code & 3]);
			}
		}
	}
	
	public static void vic2_draw_bitmap (int ybegin, int yend, int ch, int yoff, int xoff)
	{
		int y, code;
                //System.out.println("vic2_draw_bitmap");
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read ((vic2.chargenaddr&0x2000) + ch * 8 + y);
				vic2.screen[y + yoff][xoff >> 3] = code;
				vic2.bitmap.line[y + yoff][xoff] = vic2.c64_bitmap[code >> 7];
				vic2.bitmap.line[y + yoff][1 + xoff] = vic2.c64_bitmap[(code >> 6) & 1];
				vic2.bitmap.line[y + yoff][2 + xoff] = vic2.c64_bitmap[(code >> 5) & 1];
				vic2.bitmap.line[y + yoff][3 + xoff] = vic2.c64_bitmap[(code >> 4) & 1];
				vic2.bitmap.line[y + yoff][4 + xoff] = vic2.c64_bitmap[(code >> 3) & 1];
				vic2.bitmap.line[y + yoff][5 + xoff] = vic2.c64_bitmap[(code >> 2) & 1];
				vic2.bitmap.line[y + yoff][6 + xoff] = vic2.c64_bitmap[(code >> 1) & 1];
				vic2.bitmap.line[y + yoff][7 + xoff] = vic2.c64_bitmap[code & 1];
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read.handler((vic2.chargenaddr&0x2000) + ch * 8 + y);
				(vic2.screen[y + yoff]).write(xoff >> 3, (char) code);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff, (char) vic2.c64_bitmap[code >> 7]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(1 + xoff, (char) vic2.c64_bitmap[(code >> 6) & 1]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(2 + xoff, (char) vic2.c64_bitmap[(code >> 5) & 1]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(3 + xoff, (char) vic2.c64_bitmap[(code >> 4) & 1]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(4 + xoff, (char) vic2.c64_bitmap[(code >> 3) & 1]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(5 + xoff, (char) vic2.c64_bitmap[(code >> 2) & 1]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(6 + xoff, (char) vic2.c64_bitmap[(code >> 1) & 1]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(7 + xoff, (char) vic2.c64_bitmap[code & 1]);
			}
		}
	}
	
	public static void vic2_draw_bitmap_multi (int ybegin, int yend, int ch, int yoff, int xoff)
	{
		int y, code;
                //System.out.println("vic2_draw_bitmap_multi "+vic2.chargenaddr);
	
	/*	if (Machine.color_depth == 8)
		{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read ((vic2.chargenaddr&0x2000)+ ch * 8 + y);
				vic2.screen[y + yoff][xoff >> 3] = vic2.foreground[code];
				vic2.bitmap.line[y + yoff][xoff] =
					vic2.bitmap.line[y + yoff][xoff + 1] = vic2.bitmapmulti[code >> 6];
				vic2.bitmap.line[y + yoff][xoff + 2] =
					vic2.bitmap.line[y + yoff][xoff + 3] = vic2.bitmapmulti[(code >> 4) & 3];
				vic2.bitmap.line[y + yoff][xoff + 4] =
					vic2.bitmap.line[y + yoff][xoff + 5] = vic2.bitmapmulti[(code >> 2) & 3];
				vic2.bitmap.line[y + yoff][xoff + 6] =
					vic2.bitmap.line[y + yoff][xoff + 7] = vic2.bitmapmulti[code & 3];
			}
		}
		else
	*/	{
			for (y = ybegin; y <= yend; y++)
			{
				code = vic2.dma_read.handler((vic2.chargenaddr&0x2000) + ch * 8 + y);
                            //code = vic2.dma_read.handler((8192&0x2000) + ch * 8 + y);
				(vic2.screen[y + yoff]).write(xoff >> 3, (char) vic2.foreground[code]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff, (char) vic2.bitmapmulti[code >> 6]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 1, (char) vic2.bitmapmulti[code >> 6]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 2, (char) vic2.bitmapmulti[(code >> 4) & 3]); 
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 3, (char) vic2.bitmapmulti[(code >> 4) & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 4, (char) vic2.bitmapmulti[(code >> 2) & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 5, (char) vic2.bitmapmulti[(code >> 2) & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 6, (char) vic2.bitmapmulti[code & 3]);
				new UShortPtr( vic2.bitmap.line[y + yoff]).write(xoff + 7, (char) vic2.bitmapmulti[code & 3]);
			}
		}
	}
	
	public static void vic2_draw_sprite_code_multi (int y, int xbegin, int code, int prior)
	{
		int x, mask, shift;
	
		if ((y < YPOS) || (y >= 208) || (xbegin <= 1) || (xbegin >= 328))
			return;
	/*	if (Machine.color_depth == 8)
		{
			for (x = 0, mask = 0xc0, shift = 6; x < 8; x += 2, mask >>= 2, shift -= 2)
			{
				if (code & mask)
				{
					switch ((prior & mask) >> shift)
					{
					case 1:
						vic2.bitmap.line[y][xbegin + x + 1] =
							vic2.spritemulti[(code >> shift) & 3];
						break;
					case 2:
						vic2.bitmap.line[y][xbegin + x] =
							vic2.spritemulti[(code >> shift) & 3];
						break;
					case 3:
						vic2.bitmap.line[y][xbegin + x] =
							vic2.bitmap.line[y][xbegin + x + 1] =
							vic2.spritemulti[(code >> shift) & 3];
						break;
					}
				}
			}
		}
		else
	*/	{
			for (x = 0, mask = 0xc0, shift = 6; x < 8; x += 2, mask >>= 2, shift -= 2)
			{
				if ((code & mask) != 0)
				{
					switch ((prior & mask) >> shift)
					{
					case 1:
						new UShortPtr( vic2.bitmap.line[y]).write(xbegin + x + 1, (char) vic2.spritemulti[(code >> shift) & 3]);
						break;
					case 2:
						new UShortPtr( vic2.bitmap.line[y]).write(xbegin + x, (char) vic2.spritemulti[(code >> shift) & 3]);
						break;
					case 3:
						new UShortPtr( vic2.bitmap.line[y]).write(xbegin + x, (char) vic2.spritemulti[(code >> shift) & 3]);
						new UShortPtr( vic2.bitmap.line[y]).write(xbegin + x + 1, (char) vic2.spritemulti[(code >> shift) & 3]);
							
						break;
					}
				}
			}
		}
	}
	
	public static void vic2_draw_sprite_code (int y, int xbegin, int code, int color)
	{
		int mask, x;
	
		if ((y < YPOS) || (y >= 208) || (xbegin <= 1) || (xbegin >= 328))
			return;
	/*	if (Machine.color_depth == 8)
		{
			for (x = 0, mask = 0x80; x < 8; x++, mask >>= 1)
			{
				if (code & mask)
				{
					vic2.bitmap.line[y][xbegin + x] = color;
				}
			}
		}
		else
	*/	{
			for (x = 0, mask = 0x80; x < 8; x++, mask >>= 1)
			{
				if ((code & mask) != 0)
				{
					new UShortPtr( vic2.bitmap.line[y]).write(xbegin + x, (char) color);
				}
			}
		}
	}
	
	public static void vic2_sprite_collision (int nr, int y, int x, int mask)
	{
		int i, value, xdiff;
	
		for (i = 7; i > nr; i--)
		{
			if (SPRITEON (i)==0
				|| vic2.sprites[i].paintedline[y]==0
				|| (SPRITE_COLLISION (i)!=0 && SPRITE_COLLISION (nr)!=0))
				continue;
			if ((x + 7 < SPRITE_X_POS (i))
				|| (x >= SPRITE_X_POS (i) + SPRITE_X_SIZE (i)))
				continue;
			xdiff = x - SPRITE_X_POS (i);
			if ((x & 7) == (SPRITE_X_POS (i) & 7))
				value = vic2.sprites[i].bitmap[y][xdiff >> 3];
			else if (xdiff < 0)
				value = vic2.sprites[i].bitmap[y][0] >> (-xdiff);
			else {
				UBytePtr vp = new UBytePtr(vic2.sprites[i].bitmap[y], (xdiff>>3));
				value = ((vp.read(1) | (vp.read() << 8)) >> (8 - (xdiff&7) )) & 0xff;
			}
			if ((value & mask) != 0)
			{
				SPRITE_SET_COLLISION (i);
				SPRITE_SET_COLLISION (nr);
				vic2_set_interrupt (4);
			}
		}
	}
	
	public static void vic2_draw_sprite_multi (int nr, int yoff, int ybegin, int yend)
	{
		int y, i, prior, addr, xbegin, collision;
		int value, value2, value3 = 0, bg;
                int[] color = new int[2];
	
		xbegin = SPRITE_X_POS (nr);
		addr = vic2.dma_read.handler(SPRITE_ADDR (nr)) << 6;
		vic2.spritemulti[2] = Machine.pens[SPRITE_COLOR (nr)];
		prior = SPRITE_PRIORITY (nr);
		collision = SPRITE_BG_COLLISION (nr);
		color[0] = Machine.pens[0];
		color[1] = Machine.pens[1];
	
		if (SPRITE_X_EXPAND (nr) != 0)
		{
			for (y = ybegin; y <= yend; y++)
			{
				vic2.sprites[nr].paintedline[y] = 1;
				for (i = 0; i < 3; i++)
				{
					value = vic2.expandx_multi[bg = vic2.dma_read.handler(addr + vic2.sprites[nr].line * 3 + i)];
					value2 = vic2.expandx[vic2.multi_collision[bg]];
					vic2.sprites[nr].bitmap[y][i*2] = (char) (value2>>8);
					vic2.sprites[nr].bitmap[y][i*2+1] = (char) (value2&0xff);
					vic2_sprite_collision (nr, y, xbegin + i * 16, value2 >> 8);
					vic2_sprite_collision (nr, y, xbegin + i * 16 + 8, value2 & 0xff);
					if (prior!=0 || collision==0)
					{
						value3 = vic2_getforeground16 (yoff + y, xbegin + i * 16);
					}
					if (collision==0 && (value2 & value3)!=0)
					{
						collision = 1;
						SPRITE_SET_BG_COLLISION (nr);
						vic2_set_interrupt (2);
					}
					if (prior!=0)
					{
						vic2_draw_sprite_code_multi (yoff + y, xbegin + i * 16, value >> 8,
													 (value3 >> 8) ^ 0xff);
						vic2_draw_sprite_code_multi (yoff + y, xbegin + i * 16 + 8, value & 0xff,
													 (value3 & 0xff) ^ 0xff);
					}
					else
					{
						vic2_draw_sprite_code_multi (yoff + y, xbegin + i * 16, value >> 8, 0xff);
						vic2_draw_sprite_code_multi (yoff + y, xbegin + i * 16 + 8, value & 0xff, 0xff);
					}
				}
				vic2.sprites[nr].bitmap[y][i*2]=0; //easier sprite collision detection
				if (SPRITE_Y_EXPAND (nr) != 0)
				{
					if (vic2.sprites[nr].repeat != 0)
					{
						vic2.sprites[nr].line++;
						vic2.sprites[nr].repeat = 0;
					}
					else
						vic2.sprites[nr].repeat = 1;
				}
				else
				{
					vic2.sprites[nr].line++;
				}
			}
		}
		else
		{
			for (y = ybegin; y <= yend; y++)
			{
				vic2.sprites[nr].paintedline[y] = 1;
				for (i = 0; i < 3; i++)
				{
					value = vic2.dma_read.handler(addr + vic2.sprites[nr].line * 3 + i);
                                        value2 = vic2.multi_collision[value];
					vic2.sprites[nr].bitmap[y][i] = (char) value2;
					vic2_sprite_collision (nr, y, xbegin + i * 8, value2);
					if (prior!=0 || collision==0)
					{
						value3 = vic2_getforeground (yoff + y, xbegin + i * 8);
					}
					if (collision==0 && (value2 & value3)!=0)
					{
						collision = 1;
						SPRITE_SET_BG_COLLISION (nr);
						vic2_set_interrupt (2);
					}
					if (prior!=0)
					{
						vic2_draw_sprite_code_multi (yoff + y, xbegin + i * 8, value, value3 ^ 0xff);
					}
					else
					{
						vic2_draw_sprite_code_multi (yoff + y, xbegin + i * 8, value, 0xff);
					}
				}
				vic2.sprites[nr].bitmap[y][i]=0; //easier sprite collision detection
				if (SPRITE_Y_EXPAND (nr) != 0)
				{
					if (vic2.sprites[nr].repeat != 0)
					{
						vic2.sprites[nr].line++;
						vic2.sprites[nr].repeat = 0;
					}
					else
						vic2.sprites[nr].repeat = 1;
				}
				else
				{
					vic2.sprites[nr].line++;
				}
			}
		}
	}
	
	public static void vic2_draw_sprite (int nr, int yoff, int ybegin, int yend)
	{
		int y, i, addr, xbegin, color, prior, collision;
		int value, value3 = 0;
	
		xbegin = SPRITE_X_POS (nr);
		addr = vic2.dma_read.handler(SPRITE_ADDR (nr)) << 6;
		color = Machine.pens[SPRITE_COLOR (nr)];
		prior = SPRITE_PRIORITY (nr);
		collision = SPRITE_BG_COLLISION (nr);
	
		if (SPRITE_X_EXPAND (nr) != 0)
		{
			for (y = ybegin; y <= yend; y++)
			{
				vic2.sprites[nr].paintedline[y] = 1;
				for (i = 0; i < 3; i++)
				{
					value = vic2.expandx[vic2.dma_read.handler(addr + vic2.sprites[nr].line * 3 + i)];
					vic2.sprites[nr].bitmap[y][i*2] = (char) (value>>8);
					vic2.sprites[nr].bitmap[y][i*2+1] = (char) (value&0xff);
					vic2_sprite_collision (nr, y, xbegin + i * 16, value >> 8);
					vic2_sprite_collision (nr, y, xbegin + i * 16 + 8, value & 0xff);
					if (prior!=0 || collision==0)
						value3 = vic2_getforeground16 (yoff + y, xbegin + i * 16);
					if (collision==0 && (value & value3)!=0)
					{
						collision = 1;
						SPRITE_SET_BG_COLLISION (nr);
						vic2_set_interrupt (2);
					}
					if (prior != 0)
						value &= ~value3;
					vic2_draw_sprite_code (yoff + y, xbegin + i * 16, value >> 8, color);
					vic2_draw_sprite_code (yoff + y, xbegin + i * 16 + 8, value & 0xff, color);
				}
				vic2.sprites[nr].bitmap[y][i*2]=0; //easier sprite collision detection
				if (SPRITE_Y_EXPAND (nr) != 0)
				{
					if (vic2.sprites[nr].repeat != 0)
					{
						vic2.sprites[nr].line++;
						vic2.sprites[nr].repeat = 0;
					}
					else
						vic2.sprites[nr].repeat = 1;
				}
				else
				{
					vic2.sprites[nr].line++;
				}
			}
		}
		else
		{
			for (y = ybegin; y <= yend; y++)
			{
				vic2.sprites[nr].paintedline[y] = 1;
				for (i = 0; i < 3; i++)
				{
					value = vic2.dma_read.handler(addr + vic2.sprites[nr].line * 3 + i);
					vic2.sprites[nr].bitmap[y][i] = (char) value;
					vic2_sprite_collision (nr, y, xbegin + i * 8, value);
					if (prior!=0 || collision==0)
						value3 = vic2_getforeground (yoff + y, xbegin + i * 8);
					if (collision==0 && (value & value3)!=0)
					{
						collision = 1;
						SPRITE_SET_BG_COLLISION (nr);
						vic2_set_interrupt (2);
					}
					if (prior != 0)
						value &= ~value3;
					vic2_draw_sprite_code (yoff + y, xbegin + i * 8, value, color);
				}
				vic2.sprites[nr].bitmap[y][i]=0; //easier sprite collision detection
				if (SPRITE_Y_EXPAND (nr) != 0)
				{
					if (vic2.sprites[nr].repeat != 0)
					{
						vic2.sprites[nr].line++;
						vic2.sprites[nr].repeat = 0;
					}
					else
						vic2.sprites[nr].repeat = 1;
				}
				else
				{
					vic2.sprites[nr].line++;
				}
			}
		}
	}
	
/*TODO*///	static void vic3_drawlines (int first, int last);
	
	public static void vic2_drawlines (int first, int last)
	{
            /*System.out.println("ECMON():"+ECMON());
            System.out.println("HIRESON():"+HIRESON());
            System.out.println("MULTICOLORON():"+MULTICOLORON());
            System.out.println("LINES25():"+LINES25());
            System.out.println("COLUMNS40():"+COLUMNS40());*/
		int line, vline, end;
		int attr, ch, ecm;
		int syend;
		int offs, yoff, xoff, ybegin, yend, xbegin, xend;
		int i, j;
	
		if (vic2.vic3 && VIC3_BITPLANES()!=0) return ;
		/* temporary allowing vic3 displaying 80 columns */
		if (vic2.vic3&&(vic2.reg[0x31]&0x80)!=0) {
/*TODO*///			vic3_drawlines(first,last);
			return;
		}
	
		if (first == last)
			return;
		vic2.lastline = last;
		if (osd_skip_this_frame () != 0)
			return;
	
	
		/* top part of display not rastered */
		first -= VIC2_YPOS - YPOS;
		last -= VIC2_YPOS - YPOS;
		if ((first >= last) || (last <= 0))
		{
			for (i = 0; i < 8; i++)
				vic2.sprites[i].repeat = vic2.sprites[i].line = 0;
			return;
		}
		if (first < 0)
			first = 0;
	
		if (SCREENON() == 0)
		{
	/*		if (Machine.color_depth == 8)
			{
				for (line = first; (line < last) && (line < vic2.bitmap.height); line++)
					memset (vic2.bitmap.line[line], Machine.pens[0], vic2.bitmap.width);
			}
			else
	*/		{
				for (line = first; (line < last) && (line < vic2.bitmap.height); line++){
					//memset16 (vic2.bitmap.line[line], Machine.pens[0], vic2.bitmap.width);
                                        memset(new UShortPtr(vic2.bitmap.line[line]), Machine.pens[0], vic2.bitmap.width);
                                }
			}
			return;
		}
	
		if (COLUMNS40() != 0){
			xbegin = XPOS; xend = xbegin + 320;
                } else {
			xbegin = XPOS + 7; xend = xbegin + 304;
                }
	
		if (last < vic2.y_begin)
			end = last;
		else
			end = vic2.y_begin + YPOS;
	
		line=first;
		if (line<end) {
			plot_box.handler(vic2.bitmap, 0, line, vic2.bitmap.width, end-line,
					 Machine.pens[FRAMECOLOR()]);
			line=end;
		}
	
		if (LINES25() != 0)
		{
			vline = line - vic2.y_begin - YPOS;
		}
		else
		{
			vline = line - vic2.y_begin - YPOS + 8 - VERTICALPOS();
		}
		if (last < vic2.y_end + YPOS)
			end = last;
		else
			end = vic2.y_end + YPOS;
	
		for (; line < end; vline = (vline + 8) & ~7, line = line + 1 + yend - ybegin)
		{
			offs = (vline >> 3) * 40;
			ybegin = vline & 7;
			yoff = line - ybegin;
			yend = (yoff + 7 < end) ? 7 : (end - yoff - 1);
			/* rendering 39 characters */
			/* left and right borders are overwritten later */
			vic2.shift[line] = HORICONTALPOS();
	
			for (xoff = vic2.x_begin + XPOS; xoff < vic2.x_end + XPOS; xoff += 8, offs++)
			{
				ch = vic2.dma_read.handler(vic2.videoaddr + offs);
/*TODO*///	#if 0
/*TODO*///				attr = vic2.dma_read_color (vic2.videoaddr + offs);
/*TODO*///	#else
				/* temporaery until vic3 finished */
				attr = vic2.dma_read_color.handler((vic2.videoaddr + offs)&0x3ff)&0x0f;
/*TODO*///	#endif
				if (HIRESON() != 0)
				{
					vic2.bitmapmulti[1] = vic2.c64_bitmap[1] = Machine.pens[ch >> 4];
					vic2.bitmapmulti[2] = vic2.c64_bitmap[0] = Machine.pens[ch & 0xf];
					if (MULTICOLORON() != 0)
					{
					    vic2.bitmapmulti[3] = Machine.pens[attr];
						vic2_draw_bitmap_multi (ybegin, yend, offs, yoff, xoff);
					}
					else
					{
						vic2_draw_bitmap (ybegin, yend, offs, yoff, xoff);
					}
				}
				else if (ECMON() != 0)
				{
					ecm = ch >> 6;
					vic2.ecmcolor[0] = vic2.colors[ecm];
					vic2.ecmcolor[1] = Machine.pens[attr];
					vic2_draw_character (ybegin, yend, ch & ~0xC0, yoff, xoff, vic2.ecmcolor);
				}
				else if (MULTICOLORON()!=0 && (attr & 8)!=0)
				{
					vic2.multi[3] = Machine.pens[attr & 7];
					vic2_draw_character_multi (ybegin, yend, ch, yoff, xoff);
				}
				else
				{
					vic2.mono[1] = Machine.pens[attr];
					vic2_draw_character (ybegin, yend, ch, yoff, xoff, vic2.mono);
				}
			}
			/* sprite priority, sprite overwrites lowerprior pixels */
			for (i = 7; i >= 0; i--)
			{
				if (vic2.sprites[i].line!=0 || vic2.sprites[i].repeat!=0)
				{
					syend = yend;
					if (SPRITE_Y_EXPAND(i) != 0)
					{
						if ((21 - vic2.sprites[i].line) * 2 - vic2.sprites[i].repeat < yend - ybegin + 1)
							syend = ybegin + (21 - vic2.sprites[i].line) * 2 - vic2.sprites[i].repeat - 1;
					}
					else
					{
						if (vic2.sprites[i].line + yend - ybegin + 1 > 20)
							syend = ybegin + 20 - vic2.sprites[i].line;
					}
					if (yoff + syend > YPOS + 200)
						syend = YPOS + 200 - yoff - 1;
					if (SPRITE_MULTICOLOR(i) != 0)
						vic2_draw_sprite_multi (i, yoff, ybegin, syend);
					else
						vic2_draw_sprite (i, yoff, ybegin, syend);
					if ((syend != yend) || (vic2.sprites[i].line > 20))
					{
						vic2.sprites[i].line = vic2.sprites[i].repeat = 0;
						for (j = syend; j <= yend; j++)
							vic2.sprites[i].paintedline[j] = 0;
					}
				}
				else if (SPRITEON(i)!=0 && (yoff + ybegin <= SPRITE_Y_POS (i))
						 && (yoff + yend >= SPRITE_Y_POS (i)))
				{
					syend = yend;
					if (SPRITE_Y_EXPAND(i) != 0)
					{
						if (21 * 2 < yend - ybegin + 1)
							syend = ybegin + 21 * 2 - 1;
					}
					else
					{
						if (yend - ybegin + 1 > 21)
							syend = ybegin + 21 - 1;
					}
					if (yoff + syend >= YPOS + 200)
						syend = YPOS + 200 - yoff - 1;
					for (j = 0; j < SPRITE_Y_POS (i) - yoff; j++)
						vic2.sprites[i].paintedline[j] = 0;
					if (SPRITE_MULTICOLOR(i) != 0)
						vic2_draw_sprite_multi (i, yoff, SPRITE_Y_POS (i) - yoff, syend);
					else
						vic2_draw_sprite (i, yoff, SPRITE_Y_POS (i) - yoff, syend);
					if ((syend != yend) || (vic2.sprites[i].line > 20))
					{
						for (j = syend; j <= yend; j++)
							vic2.sprites[i].paintedline[j] = 0;
						vic2.sprites[i].line = vic2.sprites[i].repeat = 0;
					}
				}
				else
				{
					memset (vic2.sprites[i].paintedline, 0, (vic2.sprites[i].paintedline.length));
				}
			}
			plot_box.handler(vic2.bitmap, 0, yoff+ybegin, xbegin, yend-ybegin+1,
					 Machine.pens[FRAMECOLOR()]);
			plot_box.handler(vic2.bitmap, xend, yoff+ybegin, vic2.bitmap.width - xend, yend-ybegin+1,
					 Machine.pens[FRAMECOLOR()]);
		}
		if (last < vic2.bitmap.height)
			end = last;
		else
			end = vic2.bitmap.height;
		if (line<end) {
			plot_box.handler(vic2.bitmap, 0, line, vic2.bitmap.width, end-line,
					 Machine.pens[FRAMECOLOR()]);
			line=end;
		}
	}
	
	
	public static InterruptPtr vic2_raster_irq = new InterruptPtr() {
            public int handler() {

		int i;

                vic2.rasterline++;
                if (vic2.rasterline >= vic2.lines)
                {
                        vic2.rasterline = 0;
                        if (vic2.on) vic2_drawlines (vic2.lastline, vic2.lines);

                        for (i = 0; i < 8; i++)
                                vic2.sprites[i].repeat = vic2.sprites[i].line = 0;
                        vic2.lastline = 0;
                        if (LIGHTPEN_BUTTON()!=0)
                        {
                                double tme = 0.0;

                                /* lightpen timer starten */
                                vic2.lightpentimer = timer_set (tme, 1, vic2_timer_timeout);
                        }
                        //state_display(vic2.bitmap);
                }
                if (vic2.rasterline == C64_2_RASTERLINE (RASTERLINE()))
                {
                        if (vic2.on)
                                vic2_drawlines (vic2.lastline, vic2.rasterline);
                        vic2_set_interrupt (1);
                }
                return ignore_interrupt.handler();
            }
        };
	
	public static VhUpdatePtr vic2_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
/*TODO*///	#if 0
/*TODO*///	    char text[40];
/*TODO*///	    int i, y;
/*TODO*///	    for (y=0, i=0; i<8; i++) {
/*TODO*///		if (SPRITEON(i)) {
/*TODO*///		    sprintf(text,"%d x:%d y:%d",i,
/*TODO*///			    SPRITE_X_POS(i), SPRITE_Y_POS(i) );
/*TODO*///	
/*TODO*///		    ui_text(bitmap,text,0,y);
/*TODO*///		    y+=8;
/*TODO*///		}
/*TODO*///	    }
/*TODO*///	#endif
/*TODO*///		state_display(bitmap);
/*TODO*///	}
/*TODO*///	
            }
        };
	
}