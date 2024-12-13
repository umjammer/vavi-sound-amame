/******************************************************************************

	Video Hardware for Nichibutsu Mahjong series.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 1999/11/05 -
	Special thanks to Tatsuyuki Satoh

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.palette.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.usrintrf.usrintf_showmessage;

import static arcadeflex056.osdepend.logerror;
import common.subArrays.IntArray;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.inptport.*;
import static mame056.inputH.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class sailorws
{
	
	
	public static final int VRAM_MAX	= 2;
	
	public static final int RASTER_SCROLL	= 1;
	
	public static final int SCANLINE_MIN	= 0;
	public static final int SCANLINE_MAX	= 512;
	
	
	static int[] sailorws_scrollx=new int[VRAM_MAX], sailorws_scrolly=new int[VRAM_MAX];
	static int[][] sailorws_scrollx_tmp=new int[VRAM_MAX][2], sailorws_scrolly_tmp=new int[VRAM_MAX][2];
	
	static int[][] sailorws_scrollx_raster=new int[VRAM_MAX][SCANLINE_MAX];
	static int[] sailorws_scanline=new int[VRAM_MAX];
	
	static int[] sailorws_drawx=new int[VRAM_MAX], sailorws_drawy=new int[VRAM_MAX];
	static int[][] sailorws_drawx_tmp=new int[VRAM_MAX][2], sailorws_drawy_tmp=new int[VRAM_MAX][2];
	static int[] sailorws_sizex=new int[VRAM_MAX], sailorws_sizey=new int[VRAM_MAX];
	static int[] sailorws_radr=new int[VRAM_MAX];
	static int[][] sailorws_radr_tmp=new int[VRAM_MAX][3];
	static int[] sailorws_gfxflag=new int[VRAM_MAX];
	static int[] sailorws_dispflag=new int[VRAM_MAX];
	static int[] sailorws_flipscreen=new int[VRAM_MAX];
	static int[] sailorws_highcolor=new int[VRAM_MAX];
	static int[] sailorws_transparency=new int[VRAM_MAX];
	static int[] sailorws_flipx=new int[VRAM_MAX], sailorws_flipy=new int[VRAM_MAX];
	static int sailorws_paltblnum;
	static int sailorws_screen_refresh;
	static int sailorws_gfxflag2;
	static int gfxdraw_mode;
	
	static mame_bitmap sailorws_tmpbitmap0, sailorws_tmpbitmap1;
	static UBytePtr sailorws_videoram0=new UBytePtr(), sailorws_videoram1=new UBytePtr();
	static UBytePtr sailorws_videoworkram0=new UBytePtr(), sailorws_videoworkram1=new UBytePtr();
	static UBytePtr sailorws_palette=new UBytePtr(), mscoutm_palette=new UBytePtr();
	static UBytePtr sailorws_paltbl0=new UBytePtr(), sailorws_paltbl1=new UBytePtr();
	
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static ReadHandlerPtr sailorws_palette_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return sailorws_palette.read(offset);
	} };
	
	public static WriteHandlerPtr sailorws_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		sailorws_palette.write(offset, data);
	
		if ((offset & 1) != 0)
		{
			offset &= 0x1fe;
	
			r = ((sailorws_palette.read(offset + 0) & 0x0f) << 4);
			g = ((sailorws_palette.read(offset + 0) & 0xf0) << 0);
			b = ((sailorws_palette.read(offset + 1) & 0x0f) << 4);
	
			r = (r | (r >> 4));
			g = (g | (g >> 4));
			b = (b | (b >> 4));
	
			palette_set_color((offset >> 1), r, g, b);
		}
	} };
	
	public static ReadHandlerPtr mscoutm_palette_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return mscoutm_palette.read(offset);
	} };
	
	public static WriteHandlerPtr mscoutm_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
		int offs_h, offs_l;
	
		mscoutm_palette.write(offset, data);
	
		offs_h = (offset / 0x0300);	// 0x000, 0x300, 0x600, 0x900
		offs_l = (offset & 0x00ff);	// 0x000 - 0x0ff
	
		r = mscoutm_palette.read((0x000 + (offs_h * 0x300) + offs_l));
		g = mscoutm_palette.read((0x100 + (offs_h * 0x300) + offs_l));
		b = mscoutm_palette.read((0x200 + (offs_h * 0x300) + offs_l));
	
		palette_set_color(((offs_h * 0x100) + offs_l), r, g, b);
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	static int sailorws_gfxbusy_r(int vram, int offset)
	{
		return 0xfe;
	}
	
	static void sailorws_scrollx_w(int vram, int offset, int data)
	{

		int new_line;
	
		sailorws_scrollx_tmp[vram][offset] = data;
	
		if (offset != 0)
		{
			sailorws_scrollx[vram] = -((((sailorws_scrollx_tmp[vram][0] + (sailorws_scrollx_tmp[vram][1] << 8)) & 0x1ff) + 0x4e) << 1);
	
			if (gfxdraw_mode != 2)
			{
				/* update line scroll position */
	
				new_line = cpu_getscanline();
				if (new_line > SCANLINE_MAX) new_line = SCANLINE_MAX;
	
				if (sailorws_flipscreen[vram] != 0)
				{
					for ( ; sailorws_scanline[vram] < new_line; sailorws_scanline[vram]++)
					{
						sailorws_scrollx_raster[vram][sailorws_scanline[vram]] = sailorws_scrollx[vram];
					}
				}
				else
				{
					for ( ; sailorws_scanline[vram] < new_line; sailorws_scanline[vram]++)
					{
						sailorws_scrollx_raster[vram][(sailorws_scanline[vram] ^ 0x1ff)] = sailorws_scrollx[vram];
					}
				}
			}
		}
/*TODO*///	#else
/*TODO*///		sailorws_scrollx_tmp[vram][offset] = data;
/*TODO*///	
/*TODO*///		if (offset)
/*TODO*///		{
/*TODO*///			sailorws_scrollx[vram] = -((((sailorws_scrollx_tmp[vram][0] + (sailorws_scrollx_tmp[vram][1] << 8)) & 0x1ff) + 0x4e) << 1);
/*TODO*///		}
/*TODO*///	#endif
	}
	
	static void sailorws_scrolly_w(int vram, int offset, int data)
	{
		sailorws_scrolly_tmp[vram][offset] = data;
	
		if (offset != 0)
		{
			if (sailorws_flipscreen[vram] != 0) sailorws_scrolly[vram] = ((sailorws_scrolly_tmp[vram][0] + (sailorws_scrolly_tmp[vram][1] << 8)) ^ 0x1ff) & 0x1ff;
			else sailorws_scrolly[vram] = (sailorws_scrolly_tmp[vram][0] + (sailorws_scrolly_tmp[vram][1] << 8) + 1) & 0x1ff;
		}
	}
	
	static void sailorws_radr_w(int vram, int offset, int data)
	{
		sailorws_radr_tmp[vram][offset] = data;
	
		if (offset == 0)
		{
			sailorws_radr[vram] = (sailorws_radr_tmp[vram][0] + (sailorws_radr_tmp[vram][1] << 8) + (sailorws_radr_tmp[vram][2] << 16));
		}
	}
        
        static int sailorws_flipscreen_old[] = { -1, -1 };
	
	static void sailorws_gfxflag_w(int vram, int offset, int data)
	{
		
	
		sailorws_gfxflag[vram] = data;
	
		sailorws_flipx[vram] = (data & 0x01)!=0 ? 1 : 0;
		sailorws_flipy[vram] = (data & 0x02)!=0 ? 1 : 0;
		sailorws_highcolor[vram] = (data & 0x04)!=0 ? 1 : 0;
	//	if (data & 0x08) usrintf_showmessage("Unknown GFX Flag!! (0x08)");
		sailorws_transparency[vram] = (data & 0x10)!=0 ? 1 : 0;
	//	if (data & 0x20) usrintf_showmessage("Unknown GFX Flag!! (0x20)");
		sailorws_flipscreen[vram] = (data & 0x40)!=0 ? 0 : 1;
		sailorws_dispflag[vram] = (data & 0x80)!=0 ? 1 : 0;
	
		if (sailorws_flipscreen[vram] != sailorws_flipscreen_old[vram])
		{
			sailorws_screen_refresh = 1;
			sailorws_flipscreen_old[vram] = sailorws_flipscreen[vram];
			sailorws_vramflip(vram);
		}
	}
	
	static void sailorws_sizex_w(int vram, int offset, int data)
	{
		sailorws_sizex[vram] = data;
	}
	
	static void sailorws_sizey_w(int vram, int offset, int data)
	{
		sailorws_sizey[vram] = data;
	}
	
	static void sailorws_drawx_w(int vram, int offset, int data)
	{
		sailorws_drawx_tmp[vram][offset] = data;
	
		if (offset!=0)
		{
			sailorws_drawx[vram] = ((sailorws_drawx_tmp[vram][0] + (sailorws_drawx_tmp[vram][1] << 8)) ^ 0x3ff) & 0x3ff;
		}
	}
	
	static void sailorws_drawy_w(int vram, int offset, int data)
	{
		sailorws_drawy_tmp[vram][offset] = data;
	
		if (offset!=0)
		{
			sailorws_drawy[vram] = ((sailorws_drawy_tmp[vram][0] + (sailorws_drawy_tmp[vram][1] << 8)) ^ 0x1ff) & 0x1ff;
	
			if (gfxdraw_mode == 2) mscoutm_gfxdraw(vram);
			else sailorws_gfxdraw(vram);
		}
	}
	
	public static void sailorws_paltblnum_w(int data)
	{
		sailorws_paltblnum = data;
	}
	
	public static WriteHandlerPtr sailorws_paltbl_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		sailorws_paltbl0.write(((sailorws_paltblnum & 0xff) * 0x10) + (offset & 0x0f), data);
	} };
	
	public static WriteHandlerPtr sailorws_paltbl_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		sailorws_paltbl1.write(((sailorws_paltblnum & 0xff) * 0x10) + (offset & 0x0f), data);
	} };
	
	public static void sailorws_gfxflag2_w(int data)
	{
		sailorws_gfxflag2 = data;
	}
	
	static int sailorws_gfxrom_r(int vram, int offset)
	{
		UBytePtr GFXROM = new UBytePtr(memory_region(REGION_GFX1));
	
		return GFXROM.read(sailorws_radr[vram]);
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	static void sailorws_vramflip(int vram)
	{
		int x, y;
		int color1, color2;
		UBytePtr vidram;
	
		vidram = vram!=0 ? sailorws_videoram1 : sailorws_videoram0;
	
		for (y = 0; y < (Machine.drv.screen_height / 2); y++)
		{
			for (x = 0; x < Machine.drv.screen_width; x++)
			{
				color1 = vidram.read((y * Machine.drv.screen_width) + x);
				color2 = vidram.read(((y ^ 0x1ff) * Machine.drv.screen_width) + (x ^ 0x3ff));
				vidram.write((y * Machine.drv.screen_width) + x, color2);
				vidram.write(((y ^ 0x1ff) * Machine.drv.screen_width) + (x ^ 0x3ff), color1);
			}
		}
	
		if (gfxdraw_mode == 2)
		{
			vidram = vram!=0 ? sailorws_videoworkram1 : sailorws_videoworkram0;
	
			for (y = 0; y < (Machine.drv.screen_height / 2); y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color1 = vidram.read((y * Machine.drv.screen_width) + x);
					color2 = vidram.read(((y ^ 0x1ff) * Machine.drv.screen_width) + (x ^ 0x3ff));
					vidram.write((y * Machine.drv.screen_width) + x, color2);
					vidram.write(((y ^ 0x1ff) * Machine.drv.screen_width) + (x ^ 0x3ff), color1);
				}
			}
		}
	}
	
	static void sailorws_gfxdraw(int vram)
	{
		UBytePtr GFX = new UBytePtr(memory_region(REGION_GFX1));
	
		int x, y;
		int dx1, dx2, dy;
		int startx, starty;
		int sizex, sizey;
		int skipx, skipy;
		int ctrx, ctry;
		int tflag1, tflag2;
		int color, color1, color2;
		int drawcolor1, drawcolor2;
		int gfxaddr;
	
		if (sailorws_flipx[vram] != 0)
		{
			sailorws_drawx[vram] -= sailorws_sizex[vram];
			startx = sailorws_sizex[vram];
			sizex = (sailorws_sizex[vram] + 1);
			skipx = -1;
		}
		else
		{
			sailorws_drawx[vram] = (sailorws_drawx[vram] - sailorws_sizex[vram]);
			startx = 0;
			sizex = (sailorws_sizex[vram] + 1);
			skipx = 1;
		}
	
		if (sailorws_flipy[vram] != 0)
		{
			sailorws_drawy[vram] -= (sailorws_sizey[vram] + 1);
			starty = sailorws_sizey[vram];
			sizey = (sailorws_sizey[vram] + 1);
			skipy = -1;
		}
		else
		{
			sailorws_drawy[vram] = (sailorws_drawy[vram] - sailorws_sizey[vram] - 1);
			starty = 0;
			sizey = (sailorws_sizey[vram] + 1);
			skipy = 1;
		}
	
		gfxaddr = ((sailorws_radr[vram] + 2) & 0x00ffffff);
	
		for (y = starty, ctry = sizey; ctry > 0; y += skipy, ctry--)
		{
			for (x = startx, ctrx = sizex; ctrx > 0; x += skipx, ctrx--)
			{
				if ((gfxaddr > (memory_region_length(REGION_GFX1) - 1)))
				{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///					usrintf_showmessage("GFXROM ADDRESS OVER!!");
/*TODO*///	#endif
					gfxaddr = 0;
				}
	
				color = GFX.read(gfxaddr++);
	
				if (sailorws_flipscreen[vram] != 0)
				{
					dx1 = (((((sailorws_drawx[vram] + x) * 2) + 0) ^ 0x3ff) & 0x3ff);
					dx2 = (((((sailorws_drawx[vram] + x) * 2) + 1) ^ 0x3ff) & 0x3ff);
					dy = (((sailorws_drawy[vram] + y) ^ 0x1ff) & 0x1ff);
				}
				else
				{
					dx1 = ((((sailorws_drawx[vram] + x) * 2) + 0) & 0x3ff);
					dx2 = ((((sailorws_drawx[vram] + x) * 2) + 1) & 0x3ff);
					dy = ((sailorws_drawy[vram] + y) & 0x1ff);
				}
	
				if (sailorws_flipx[vram] != 0)
				{
					// flip
					color1 = (color & 0xf0) >> 4;
					color2 = (color & 0x0f) >> 0;
				}
				else
				{
					// normal
					color1 = (color & 0x0f) >> 0;
					color2 = (color & 0xf0) >> 4;
				}
	
				if (vram == 0)
				{
					drawcolor1 = sailorws_paltbl0.read((sailorws_paltblnum * 0x10) + color1);
					drawcolor2 = sailorws_paltbl0.read((sailorws_paltblnum * 0x10) + color2);
				}
				else
				{
					drawcolor1 = sailorws_paltbl1.read((sailorws_paltblnum * 0x10) + color1);
					drawcolor2 = sailorws_paltbl1.read((sailorws_paltblnum * 0x10) + color2);
				}
	
				if (sailorws_transparency[vram] != 0)
				{
					tflag1 = (drawcolor1 != 0xff) ? 1 : 0;
					tflag2 = (drawcolor2 != 0xff) ? 1 : 0;
				}
				else
				{
					tflag1 = 1;
					tflag2 = 1;
				}
	
				if (vram == 0)
				{
					if (tflag1 != 0)
					{
						sailorws_videoram0.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
						plot_pixel.handler(sailorws_tmpbitmap0, dx1, dy, Machine.pens[drawcolor1]);
					}
					if (tflag2 != 0)
					{
						sailorws_videoram0.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
						plot_pixel.handler(sailorws_tmpbitmap0, dx2, dy, Machine.pens[drawcolor2]);
					}
				}
				else
				{
					if (tflag1 != 0)
					{
						sailorws_videoram1.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
						plot_pixel.handler(sailorws_tmpbitmap1, dx1, dy, Machine.pens[drawcolor1]);
					}
					if (tflag2 != 0)
					{
						sailorws_videoram1.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
						plot_pixel.handler(sailorws_tmpbitmap1, dx2, dy, Machine.pens[drawcolor2]);
					}
				}
			}
		}
	}
	
	static void mscoutm_gfxdraw(int vram)
	{
		UBytePtr GFX = new UBytePtr(memory_region(REGION_GFX1));
	
		int x, y;
		int dx1, dx2, dy;
		int startx, starty;
		int sizex, sizey;
		int skipx, skipy;
		int ctrx, ctry;
		int tflag1, tflag2;
		int color, color1, color2;
		int drawcolor1, drawcolor2;
		int gfxaddr;
	
		if (sailorws_highcolor[vram] != 0)
		{
			// NB22090 high color mode
			sailorws_sizex[vram] = (GFX.read(((sailorws_radr[vram] + 0) & 0x00ffffff)) & 0xff);
			sailorws_sizey[vram] = (GFX.read(((sailorws_radr[vram] + 1) & 0x00ffffff)) & 0xff);
		}
	
		if (sailorws_flipx[vram] != 0)
		{
			sailorws_drawx[vram] -= sailorws_sizex[vram];
			startx = sailorws_sizex[vram];
			sizex = (sailorws_sizex[vram] + 1);
			skipx = -1;
		}
		else
		{
			sailorws_drawx[vram] = (sailorws_drawx[vram] - sailorws_sizex[vram]);
			startx = 0;
			sizex = (sailorws_sizex[vram] + 1);
			skipx = 1;
		}
	
		if (sailorws_flipy[vram] != 0)
		{
			sailorws_drawy[vram] -= (sailorws_sizey[vram] + 1);
			starty = sailorws_sizey[vram];
			sizey = (sailorws_sizey[vram] + 1);
			skipy = -1;
		}
		else
		{
			sailorws_drawy[vram] = (sailorws_drawy[vram] - sailorws_sizey[vram] - 1);
			starty = 0;
			sizey = (sailorws_sizey[vram] + 1);
			skipy = 1;
		}
	
		gfxaddr = ((sailorws_radr[vram] + 2) & 0x00ffffff);
	
		for (y = starty, ctry = sizey; ctry > 0; y += skipy, ctry--)
		{
			for (x = startx, ctrx = sizex; ctrx > 0; x += skipx, ctrx--)
			{
				if ((gfxaddr > (memory_region_length(REGION_GFX1) - 1)))
				{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///					usrintf_showmessage("GFXROM ADDRESS OVER!!");
/*TODO*///	#endif
					gfxaddr = 0;
				}
	
				color = GFX.read(gfxaddr++);
	
				if (sailorws_flipscreen[vram] != 0)
				{
					dx1 = (((((sailorws_drawx[vram] + x) * 2) + 0) ^ 0x3ff) & 0x3ff);
					dx2 = (((((sailorws_drawx[vram] + x) * 2) + 1) ^ 0x3ff) & 0x3ff);
					dy = (((sailorws_drawy[vram] + y) ^ 0x1ff) & 0x1ff);
				}
				else
				{
					dx1 = ((((sailorws_drawx[vram] + x) * 2) + 0) & 0x3ff);
					dx2 = ((((sailorws_drawx[vram] + x) * 2) + 1) & 0x3ff);
					dy = ((sailorws_drawy[vram] + y) & 0x1ff);
				}
	
				if (sailorws_flipx[vram] != 0)
				{
					// flip
					color1 = (color & 0xf0) >> 4;
					color2 = (color & 0x0f) >> 0;
				}
				else
				{
					// normal
					color1 = (color & 0x0f) >> 0;
					color2 = (color & 0xf0) >> 4;
				}
	
				if (sailorws_highcolor[vram] != 0)
				{
					// high color mode
	
					if ((sailorws_gfxflag2 & 0xc0) != 0)
					{
						// high color mode 1st draw
	
						drawcolor1 = ((color1 & 0x0f) << 0);
						drawcolor2 = ((color2 & 0x0f) << 0);
	
						if (vram == 0)
						{
							sailorws_videoworkram0.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
							sailorws_videoworkram0.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
						}
						else
						{
							sailorws_videoworkram1.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
							sailorws_videoworkram1.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
						}
						continue;
					}
					else
					{
						// high color mode 2nd draw
	
						drawcolor1 = ((color1 & 0x0f) << 4);
						drawcolor2 = ((color2 & 0x0f) << 4);
	
						if (vram == 0)
						{
							drawcolor1 |= sailorws_videoworkram0.read((dy * Machine.drv.screen_width) + dx1);
							drawcolor2 |= sailorws_videoworkram0.read((dy * Machine.drv.screen_width) + dx2);
	
							drawcolor1 += sailorws_paltbl0.read((sailorws_paltblnum * 0x10));
							drawcolor2 += sailorws_paltbl0.read((sailorws_paltblnum * 0x10));
						}
						else
						{
							drawcolor1 |= sailorws_videoworkram1.read((dy * Machine.drv.screen_width) + dx1);
							drawcolor2 |= sailorws_videoworkram1.read((dy * Machine.drv.screen_width) + dx2);
	
							drawcolor1 += sailorws_paltbl1.read((sailorws_paltblnum * 0x10));
							drawcolor2 += sailorws_paltbl1.read((sailorws_paltblnum * 0x10));
						}
					}
				}
				else
				{
					// normal color mode
	
					if (vram == 0)
					{
						drawcolor1 = sailorws_paltbl0.read((sailorws_paltblnum * 0x10) + color1);
						drawcolor2 = sailorws_paltbl0.read((sailorws_paltblnum * 0x10) + color2);
					}
					else
					{
						drawcolor1 = sailorws_paltbl1.read((sailorws_paltblnum * 0x10) + color1);
						drawcolor2 = sailorws_paltbl1.read((sailorws_paltblnum * 0x10) + color2);
					}
				}
	
				if (sailorws_transparency[vram] != 0)
				{
					tflag1 = (drawcolor1 != 0xff) ? 1 : 0;
					tflag2 = (drawcolor2 != 0xff) ? 1 : 0;
				}
				else
				{
					tflag1 = 1;
					tflag2 = 1;
				}
	
				drawcolor1 |= (0x100 * vram);
				drawcolor2 |= (0x100 * vram);
	
				if (vram == 0)
				{
					if (tflag1 != 0)
					{
						sailorws_videoram0.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
						plot_pixel.handler(sailorws_tmpbitmap0, dx1, dy, Machine.pens[drawcolor1]);
					}
					if (tflag2 != 0)
					{
						sailorws_videoram0.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
						plot_pixel.handler(sailorws_tmpbitmap0, dx2, dy, Machine.pens[drawcolor2]);
					}
				}
				else
				{
					if (tflag1 != 0)
					{
						sailorws_videoram1.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
						plot_pixel.handler(sailorws_tmpbitmap1, dx1, dy, Machine.pens[drawcolor1]);
					}
					if (tflag2 != 0)
					{
						sailorws_videoram1.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
						plot_pixel.handler(sailorws_tmpbitmap1, dx2, dy, Machine.pens[drawcolor2]);
					}
				}
			}
		}
	
		if (sailorws_highcolor[vram] != 0)
		{
			// NB22090 high color mode
			sailorws_radr[vram] = gfxaddr;
		}
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static WriteHandlerPtr sailorws_gfxflag_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_gfxflag_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_scrollx_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_scrollx_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_scrolly_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_scrolly_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_radr_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_radr_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_sizex_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_sizex_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_sizey_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_sizey_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_drawx_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_drawx_w(0, offset, data); } };
	public static WriteHandlerPtr sailorws_drawy_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_drawy_w(0, offset, data); } };
	
	public static WriteHandlerPtr sailorws_gfxflag_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_gfxflag_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_scrollx_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_scrollx_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_scrolly_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_scrolly_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_radr_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_radr_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_sizex_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_sizex_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_sizey_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_sizey_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_drawx_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_drawx_w(1, offset, data); } };
	public static WriteHandlerPtr sailorws_drawy_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { sailorws_drawy_w(1, offset, data); } };
	
	public static ReadHandlerPtr sailorws_gfxbusy_0_r  = new ReadHandlerPtr() { public int handler(int offset) { return sailorws_gfxbusy_r(0, offset); } };
	public static ReadHandlerPtr sailorws_gfxbusy_1_r  = new ReadHandlerPtr() { public int handler(int offset) { return sailorws_gfxbusy_r(1, offset); } };
	public static ReadHandlerPtr sailorws_gfxrom_0_r  = new ReadHandlerPtr() { public int handler(int offset) { return sailorws_gfxrom_r(0, offset); } };
	public static ReadHandlerPtr sailorws_gfxrom_1_r  = new ReadHandlerPtr() { public int handler(int offset) { return sailorws_gfxrom_r(1, offset); } };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhStartPtr sailorws_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((sailorws_tmpbitmap0 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_tmpbitmap1 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoram0 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoram1 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_palette = new UBytePtr(0x200)) == null) return 1;
		if ((sailorws_paltbl0 = new UBytePtr(0x1000)) == null) return 1;
		if ((sailorws_paltbl1 = new UBytePtr(0x1000)) == null) return 1;
		memset(sailorws_videoram0, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
		memset(sailorws_videoram1, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
/*TODO*///	#if RASTER_SCROLL
		sailorws_scanline[0] = sailorws_scanline[1] = SCANLINE_MIN;
/*TODO*///	#endif
		gfxdraw_mode = 1;
		return 0;
	} };
	
	public static VhStopPtr sailorws_vh_stop = new VhStopPtr() { public void handler() 
	{
		
		bitmap_free(sailorws_tmpbitmap1);
		bitmap_free(sailorws_tmpbitmap0);
		sailorws_paltbl1 = null;
		sailorws_paltbl0 = null;
		sailorws_palette = null;
		sailorws_videoram1 = null;
		sailorws_videoram0 = null;
		sailorws_tmpbitmap1 = null;
		sailorws_tmpbitmap0 = null;
	} };
	
	public static VhStartPtr mjkoiura_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((sailorws_tmpbitmap0 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoram0 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_palette = new UBytePtr(0x200)) == null) return 1;
		if ((sailorws_paltbl0 = new UBytePtr(0x1000)) == null) return 1;
		memset(sailorws_videoram0, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
/*TODO*///	#if RASTER_SCROLL
		sailorws_scanline[0] = sailorws_scanline[1] = SCANLINE_MIN;
/*TODO*///	#endif
		gfxdraw_mode = 0;
		return 0;
	} };
	
	public static VhStopPtr mjkoiura_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(sailorws_tmpbitmap0);
		sailorws_paltbl0 = null;
		sailorws_palette = null;
		sailorws_videoram0 = null;
		sailorws_tmpbitmap0 = null;
	} };
	
	public static VhStartPtr mscoutm_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((sailorws_tmpbitmap0 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_tmpbitmap1 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoram0 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoram1 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoworkram0 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((sailorws_videoworkram1 = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((mscoutm_palette = new UBytePtr(0xc00)) == null) return 1;
		if ((sailorws_paltbl0 = new UBytePtr(0x1000)) == null) return 1;
		if ((sailorws_paltbl1 = new UBytePtr(0x1000)) == null) return 1;
		memset(sailorws_videoram0, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
		memset(sailorws_videoram1, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
		memset(sailorws_videoworkram0, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
		memset(sailorws_videoworkram1, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
		gfxdraw_mode = 2;
		return 0;
	} };
	
	public static VhStopPtr mscoutm_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(sailorws_tmpbitmap1);
		bitmap_free(sailorws_tmpbitmap0);
		sailorws_paltbl1 = null;
		sailorws_paltbl0 = null;
		mscoutm_palette = null;
		sailorws_videoworkram1 = null;
		sailorws_videoworkram0 = null;
		sailorws_videoram1 = null;
		sailorws_videoram0 = null;
		sailorws_tmpbitmap1 = null;
		sailorws_tmpbitmap0 = null;
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhUpdatePtr sailorws_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		int color;
	
		if (full_refresh!=0 || sailorws_screen_refresh!=0)
		{
			sailorws_screen_refresh = 0;
			for (y = 0; y < Machine.drv.screen_height; y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color = sailorws_videoram0.read((y * Machine.drv.screen_width) + x);
					plot_pixel.handler(sailorws_tmpbitmap0, x, y, Machine.pens[color]);
				}
			}
			if (gfxdraw_mode != 0)
			{
				for (y = 0; y < Machine.drv.screen_height; y++)
				{
					for (x = 0; x < Machine.drv.screen_width; x++)
					{
						color = sailorws_videoram1.read((y * Machine.drv.screen_width) + x);
						plot_pixel.handler(sailorws_tmpbitmap1, x, y, Machine.pens[color]);
					}
				}
			}
		}
	
/*TODO*///	#if RASTER_SCROLL
		{
			int i;
	
			for (i = 0; i < 2; i++)
			{
				if (sailorws_flipscreen[i] != 0)
				{
					for ( ; sailorws_scanline[i] < SCANLINE_MAX; sailorws_scanline[i]++)
					{
						sailorws_scrollx_raster[i][sailorws_scanline[i]] = sailorws_scrollx[i];
					}
				}
				else
				{
					for ( ; sailorws_scanline[i] < SCANLINE_MAX; sailorws_scanline[i]++)
					{
						sailorws_scrollx_raster[i][(sailorws_scanline[i] ^ 0x1ff)] = sailorws_scrollx[i];
					}
				}
	
				sailorws_scanline[i] = SCANLINE_MIN;
			}
		}
/*TODO*///	#endif
	
		if (sailorws_dispflag[0] != 0)
		{
/*TODO*///	#if RASTER_SCROLL
			copyscrollbitmap(bitmap, sailorws_tmpbitmap0, SCANLINE_MAX, sailorws_scrollx_raster[0], 1, new int[]{sailorws_scrolly[0]}, Machine.visible_area, TRANSPARENCY_NONE, 0);
/*TODO*///	#else
/*TODO*///			copyscrollbitmap(bitmap, sailorws_tmpbitmap0, 1, &sailorws_scrollx[0], 1, &sailorws_scrolly[0], &Machine.visible_area, TRANSPARENCY_NONE, 0);
/*TODO*///	#endif
		}
		else
		{
			fillbitmap(bitmap, Machine.pens[0x0ff], null);
		}
	
		if (gfxdraw_mode != 0)
		{
			if (sailorws_dispflag[1] != 0)
			{
/*TODO*///	#if RASTER_SCROLL
				copyscrollbitmap(bitmap, sailorws_tmpbitmap1, SCANLINE_MAX, sailorws_scrollx_raster[1], 1, new int[]{sailorws_scrolly[1]}, Machine.visible_area, TRANSPARENCY_PEN, Machine.pens[0x0ff]);
/*TODO*///	#else
/*TODO*///				copyscrollbitmap(bitmap, sailorws_tmpbitmap1, 1, &sailorws_scrollx[1], 1, &sailorws_scrolly[1], &Machine.visible_area, TRANSPARENCY_PEN, Machine.pens[0x0ff]);
/*TODO*///	#endif
			}
		}
	} };
	
	public static VhUpdatePtr mscoutm_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		int color;
	
		if (full_refresh!=0 || sailorws_screen_refresh!=0)
		{
			sailorws_screen_refresh = 0;
			for (y = 0; y < Machine.drv.screen_height; y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color = sailorws_videoram0.read((y * Machine.drv.screen_width) + x);
					plot_pixel.handler(sailorws_tmpbitmap0, x, y, Machine.pens[color]);
				}
			}
			if (gfxdraw_mode != 0)
			{
				for (y = 0; y < Machine.drv.screen_height; y++)
				{
					for (x = 0; x < Machine.drv.screen_width; x++)
					{
						color = sailorws_videoram1.read((y * Machine.drv.screen_width) + x);
						plot_pixel.handler(sailorws_tmpbitmap1, x, y, Machine.pens[color]);
					}
				}
			}
		}
	
		if (sailorws_dispflag[0] != 0)
		{
			copyscrollbitmap(bitmap, sailorws_tmpbitmap0, 1, new int[]{sailorws_scrollx[0]}, 1, new int[]{sailorws_scrolly[0]}, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
		else
		{
			fillbitmap(bitmap, Machine.pens[0x0ff], null);
		}
	
		if (gfxdraw_mode != 0)
		{
			if (sailorws_dispflag[1] != 0)
			{
				copyscrollbitmap(bitmap, sailorws_tmpbitmap1, 1, new int[]{sailorws_scrollx[1]}, 1, new int[]{sailorws_scrolly[1]}, Machine.visible_area, TRANSPARENCY_PEN, Machine.pens[0x1ff]);
			}
		}
	} };
}
