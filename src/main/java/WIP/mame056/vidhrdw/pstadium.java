/******************************************************************************

	Video Hardware for Nichibutsu Mahjong series.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 1999/11/05 -

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.machine.nb1413m3.*;
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

public class pstadium
{
	
	
	static int pstadium_scrollx, pstadium_scrollx1, pstadium_scrollx2;
	static int pstadium_scrolly, pstadium_scrolly1, pstadium_scrolly2;
	static int pstadium_drawx, pstadium_drawx1, pstadium_drawx2;
	static int pstadium_drawy, pstadium_drawy1, pstadium_drawy2;
	static int pstadium_sizex, pstadium_sizey;
	static int pstadium_radrx, pstadium_radry;
	static int pstadium_gfxrom;
	static int pstadium_dispflag;
	static int pstadium_flipscreen;
	static int pstadium_flipx, pstadium_flipy;
	static int pstadium_paltblnum;
	static int pstadium_screen_refresh;
	
	static mame_bitmap pstadium_tmpbitmap;
	public static UBytePtr pstadium_videoram=new UBytePtr();
	public static UBytePtr pstadium_palette=new UBytePtr();
	public static UBytePtr pstadium_paltbl=new UBytePtr();
	
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static ReadHandlerPtr pstadium_palette_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return pstadium_palette.read(offset);
	} };
	
	public static WriteHandlerPtr pstadium_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		pstadium_palette.write(offset, data);
	
		if ((offset & 1)==0) return;
	
		offset &= 0x1fe;
	
		r = ((pstadium_palette.read(offset + 1) & 0x0f) << 4);
		g = ((pstadium_palette.read(offset + 0) & 0xf0) << 0);
		b = ((pstadium_palette.read(offset + 0) & 0x0f) << 4);
	
		r = (r | (r >> 4));
		g = (g | (g >> 4));
		b = (b | (b >> 4));
	
		palette_set_color((offset >> 1), r, g, b);
	} };
	
	public static WriteHandlerPtr galkoku_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		pstadium_palette.write(offset, data);
	
		if ((offset & 1)==0) return;
	
		offset &= 0x1fe;
	
		r = ((pstadium_palette.read(offset + 0) & 0x0f) << 4);
		g = ((pstadium_palette.read(offset + 1) & 0xf0) << 0);
		b = ((pstadium_palette.read(offset + 1) & 0x0f) << 4);
	
		r = (r | (r >> 4));
		g = (g | (g >> 4));
		b = (b | (b >> 4));
	
		palette_set_color((offset >> 1), r, g, b);
	} };
	
	public static WriteHandlerPtr galkaika_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		pstadium_palette.write(offset, data);
	
		if ((offset & 1)==0) return;
	
		offset &= 0x1fe;
	
		r = ((pstadium_palette.read(offset + 0) & 0x7c) >> 2);
		g = (((pstadium_palette.read(offset + 0) & 0x03) << 3) | ((pstadium_palette.read(offset + 1) & 0xe0) >> 5));
		b = ((pstadium_palette.read(offset + 1) & 0x1f) >> 0);
	
		r = ((r << 3) | (r >> 2));
		g = ((g << 3) | (g >> 2));
		b = ((b << 3) | (b >> 2));
	
		palette_set_color((offset / 2), r, g, b);
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static void pstadium_calc_scrollx()
	{
		pstadium_scrollx = ((((pstadium_scrollx2 + pstadium_scrollx1) ^ 0x1ff) & 0x1ff) << 1);
	}
	
	public static void pstadium_calc_scrolly()
	{
		if (pstadium_flipscreen != 0) pstadium_scrolly = (((pstadium_scrolly2 + pstadium_scrolly1 - 0xf0) ^ 0x1ff) & 0x1ff);
		else pstadium_scrolly = (((pstadium_scrolly2 + pstadium_scrolly1 + 1) - 0x10) & 0x1ff);
	}
	
	public static void pstadium_calc_drawx()
	{
		pstadium_drawx = ((pstadium_drawx2 + pstadium_drawx1) ^ 0x1ff) & 0x1ff;
	}
	
	public static void pstadium_calc_drawy()
	{
		pstadium_drawy = ((pstadium_drawy2 + pstadium_drawy1) ^ 0x1ff) & 0x1ff;
	}
	
	public static void pstadium_radrx_w(int data)
	{
		pstadium_radrx = data;
	}
	
	public static void pstadium_radry_w(int data)
	{
		pstadium_radry = data;
	}
	
	public static void pstadium_sizex_w(int data)
	{
		pstadium_sizex = data;
	}
	
	public static void pstadium_sizey_w(int data)
	{
		pstadium_sizey = data;
	
		pstadium_gfxdraw();
	}
        
        static int pstadium_flipscreen_old = -1;
	
	public static void pstadium_gfxflag_w(int data)
	{
		
		pstadium_flipx = (data & 0x01)!=0 ? 1 : 0;
		pstadium_flipy = (data & 0x02)!=0 ? 1 : 0;
		pstadium_flipscreen = (data & 0x04)!=0 ? 0 : 1;
		pstadium_dispflag = (data & 0x10)!=0 ? 0 : 1;
	
		if (pstadium_flipscreen != pstadium_flipscreen_old)
		{
			pstadium_vramflip();
			pstadium_screen_refresh = 1;
			pstadium_flipscreen_old = pstadium_flipscreen;
		}
	}
	
	public static void pstadium_gfxflag2_w(int data)
	{
		pstadium_drawx2 = (((data & 0x01) >> 0) << 8);
		pstadium_drawy2 = (((data & 0x02) >> 1) << 8);
		pstadium_scrollx2 = (((data & 0x04) >> 2) << 8);
		pstadium_scrolly2 = (((data & 0x08) >> 3) << 8);
	}
	
	public static void pstadium_drawx_w(int data)
	{
		pstadium_drawx1 = data;
	}
	
	public static void pstadium_drawy_w(int data)
	{
		pstadium_drawy1 = data;
	}
	
	public static void pstadium_scrollx_w(int data)
	{
		pstadium_scrollx1 = data;
	}
	
	public static void pstadium_scrolly_w(int data)
	{
		pstadium_scrolly1 = data;
	}
	
	public static void pstadium_romsel_w(int data)
	{
		pstadium_gfxrom = data;
	
		if ((0x20000 * pstadium_gfxrom) > (memory_region_length(REGION_GFX1) - 1))
		{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///			usrintf_showmessage("GFXROM BANK OVER!!");
/*TODO*///	#endif
			pstadium_gfxrom = 0;
		}
	}
	
	public static void pstadium_paltblnum_w(int data)
	{
		pstadium_paltblnum = data;
	}
	
	public static ReadHandlerPtr pstadium_paltbl_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return pstadium_paltbl.read(offset);
	} };
	
	public static WriteHandlerPtr pstadium_paltbl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		pstadium_paltbl.write(((pstadium_paltblnum & 0x7f) * 0x10) + (offset & 0x0f), data);
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static void pstadium_vramflip()
	{
		int x, y;
		int color1, color2;
	
		for (y = 0; y < (Machine.drv.screen_height / 2); y++)
		{
			for (x = 0; x < Machine.drv.screen_width; x++)
			{
				color1 = pstadium_videoram.read((y * Machine.drv.screen_width) + x);
				color2 = pstadium_videoram.read(((y ^ 0x1ff) * Machine.drv.screen_width) + (x ^ 0x3ff));
	
				pstadium_videoram.write((y * Machine.drv.screen_width) + x, color2);
				pstadium_videoram.write(((y ^ 0x1ff) * Machine.drv.screen_width) + (x ^ 0x3ff), color1);
			}
		}
	}
	
	static void pstadium_gfxdraw()
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
	
		pstadium_calc_drawx();
		pstadium_calc_drawy();
	
		if (pstadium_flipx != 0)
		{
			pstadium_drawx -= pstadium_sizex;
			startx = pstadium_sizex;
			sizex = ((pstadium_sizex ^ 0xff) + 1);
			skipx = -1;
		}
		else
		{
			pstadium_drawx = (pstadium_drawx - pstadium_sizex);
			startx = 0;
			sizex = (pstadium_sizex + 1);
			skipx = 1;
		}
	
		if (pstadium_flipy != 0)
		{
			pstadium_drawy -= (pstadium_sizey + 1);
			starty = pstadium_sizey;
			sizey = ((pstadium_sizey ^ 0xff) + 1);
			skipy = -1;
		}
		else
		{
			pstadium_drawy = (pstadium_drawy - pstadium_sizey - 1);
			starty = 0;
			sizey = (pstadium_sizey + 1);
			skipy = 1;
		}
	
		gfxaddr = ((pstadium_gfxrom << 17) + (pstadium_radry << 9) + (pstadium_radrx << 1));
	
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
	
				if (pstadium_flipscreen != 0)
				{
					dx1 = (((((pstadium_drawx + x) * 2) + 0) ^ 0x3ff) & 0x3ff);
					dx2 = (((((pstadium_drawx + x) * 2) + 1) ^ 0x3ff) & 0x3ff);
					dy = (((pstadium_drawy + y) ^ 0x1ff) & 0x1ff);
				}
				else
				{
					dx1 = ((((pstadium_drawx + x) * 2) + 0) & 0x3ff);
					dx2 = ((((pstadium_drawx + x) * 2) + 1) & 0x3ff);
					dy = ((pstadium_drawy + y) & 0x1ff);
				}
	
				if (pstadium_flipx != 0)
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
	
				drawcolor1 = pstadium_paltbl.read(((pstadium_paltblnum & 0x7f) * 0x10) + color1);
				drawcolor2 = pstadium_paltbl.read(((pstadium_paltblnum & 0x7f) * 0x10) + color2);
	
				tflag1 = (drawcolor1 != 0xff) ? 1 : 0;
				tflag2 = (drawcolor2 != 0xff) ? 1 : 0;
	
				nb1413m3_busyctr++;
	
				if (tflag1 != 0)
				{
					pstadium_videoram.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
					plot_pixel.handler(pstadium_tmpbitmap, dx1, dy, Machine.pens[drawcolor1]);
				}
				if (tflag2 != 0)
				{
					pstadium_videoram.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
					plot_pixel.handler(pstadium_tmpbitmap, dx2, dy, Machine.pens[drawcolor2]);
				}
			}
		}
	
		nb1413m3_busyflag = (nb1413m3_busyctr > 7500) ? 0 : 1;
	
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhStartPtr pstadium_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((pstadium_tmpbitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((pstadium_videoram = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height )) == null) return 1;
		if ((pstadium_palette = new UBytePtr(0x200)) == null) return 1;
		if ((pstadium_paltbl = new UBytePtr(0x800)) == null) return 1;
		memset(pstadium_videoram, 0x00, (Machine.drv.screen_width * Machine.drv.screen_height));
		return 0;
	} };
	
	public static VhStopPtr pstadium_vh_stop = new VhStopPtr() { public void handler() 
	{
		pstadium_paltbl=null;
		pstadium_palette=null;
		pstadium_videoram=null;
		bitmap_free(pstadium_tmpbitmap);
		pstadium_paltbl = null;
		pstadium_palette = null;
		pstadium_videoram = null;
		pstadium_tmpbitmap = null;
	} };
	
	public static VhUpdatePtr pstadium_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		int color;
	
		if (full_refresh!=0 || pstadium_screen_refresh!=0)
		{
			pstadium_screen_refresh = 0;
			for (y = 0; y < Machine.drv.screen_height; y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color = pstadium_videoram.read((y * Machine.drv.screen_width) + x);
					plot_pixel.handler(pstadium_tmpbitmap, x, y, Machine.pens[color]);
				}
			}
		}
	
		pstadium_calc_scrollx();
		pstadium_calc_scrolly();
	
		if ((nb1413m3_inputport & 0x20) != 0)
		{
			copyscrollbitmap(bitmap, pstadium_tmpbitmap, 1, new int[]{pstadium_scrollx}, 1, new int[]{pstadium_scrolly}, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
		else
		{
			fillbitmap(bitmap, Machine.pens[0x00], null);
		}
	} };
	
	public static VhUpdatePtr galkoku_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		int color;
	
		if (full_refresh!=0 || pstadium_screen_refresh!=0)
		{
			pstadium_screen_refresh = 0;
			for (y = 0; y < Machine.drv.screen_height; y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color = pstadium_videoram.read((y * Machine.drv.screen_width) + x);
					plot_pixel.handler(pstadium_tmpbitmap, x, y, Machine.pens[color]);
				}
			}
		}
	
		pstadium_calc_scrollx();
		pstadium_calc_scrolly();
	
		if (pstadium_dispflag != 0)
		{
			copyscrollbitmap(bitmap, pstadium_tmpbitmap, 1, new int[]{pstadium_scrollx}, 1, new int[]{pstadium_scrolly}, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
		else
		{
			fillbitmap(bitmap, Machine.pens[0x00], null);
		}
	} };
}
