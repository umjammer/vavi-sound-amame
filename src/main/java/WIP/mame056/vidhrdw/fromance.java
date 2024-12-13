/******************************************************************************

	Video Hardware for Video System Mahjong series and Pipe Dream.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 2001/02/04 -
	and Bryan McPhail, Nicola Salmoria, Aaron Giles

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

public class fromance
{
	
	
	static int selected_videoram;
	public static UBytePtr[] local_videoram = new UBytePtr[2];
	
	static int selected_paletteram;
	public static UBytePtr local_paletteram = new UBytePtr();
	
	static int[] scrollx=new int[2], scrolly=new int[2];
	static int gfxreg;
	static int flipscreen;
	
	static int crtc_register;
	static timer_entry crtc_timer;
	
	static struct_tilemap bg_tilemap, fg_tilemap;
	
	
	
	/*************************************
	 *
	 *	Tilemap callbacks
	 *
	 *************************************/
	
	public static void get_tile_info(int tile_index,int layer)
	{
		int tile = ((local_videoram[layer].read(0x0000 + tile_index) & 0x80) << 9) |
					(local_videoram[layer].read(0x1000 + tile_index) << 8) |
					local_videoram[layer].read(0x2000 + tile_index);
		int color = local_videoram[layer].read(tile_index) & 0x7f;
	
		SET_TILE_INFO(layer, tile, color, 0);
	}
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() { public void handler(int tile_index) { get_tile_info(tile_index, 0); } };
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() { public void handler(int tile_index) { get_tile_info(tile_index, 1); } };
	
	
	
	/*************************************
	 *
	 *	Video system start
	 *
	 *************************************/
	
	public static VhStartPtr fromance_vh_start = new VhStartPtr() { public int handler() 
	{
		/* allocate tilemaps */
		bg_tilemap = tilemap_create(get_bg_tile_info, tilemap_scan_rows, TILEMAP_OPAQUE,      8,4, 64,64);
		fg_tilemap = tilemap_create(get_fg_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 8,4, 64,64);
	
		/* allocate local videoram */
		local_videoram[0] = new UBytePtr(0x1000 * 3);
		local_videoram[1] = new UBytePtr(0x1000 * 3);
	
		/* allocate local palette RAM */
		local_paletteram = new UBytePtr(0x800 * 2);
	
		/* handle failure */
		if (bg_tilemap==null || fg_tilemap==null || local_videoram[0]==null || local_videoram[1]==null || local_paletteram==null)
		{
			fromance_vh_stop.handler();
			return 1;
		}
	
		/* configure tilemaps */
		tilemap_set_transparent_pen(fg_tilemap,15);
	
		/* reset the timer */
		crtc_timer = null;
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video system stop
	 *
	 *************************************/
	
	public static VhStopPtr fromance_vh_stop = new VhStopPtr() { public void handler() 
	{
		/* free all RAM */
		local_paletteram = null;
		local_videoram[1] = null;
		local_videoram[0] = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Graphics control register
	 *
	 *************************************/
        
        static int flipscreen_old = -1;
	
	public static WriteHandlerPtr fromance_gfxreg_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		gfxreg = data;
		flipscreen = (data & 0x01);
		selected_videoram = (~data >> 1) & 1;
		selected_paletteram = (data >> 6) & 1;
	
		if (flipscreen != flipscreen_old)
		{
			flipscreen_old = flipscreen;
			tilemap_set_flip(ALL_TILEMAPS, flipscreen != 0 ? (TILEMAP_FLIPX | TILEMAP_FLIPY) : 0);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Banked palette RAM
	 *
	 *************************************/
	
	public static ReadHandlerPtr fromance_paletteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* adjust for banking and read */
		offset |= selected_paletteram << 11;
		return local_paletteram.read(offset);
	} };
	
	
	public static WriteHandlerPtr fromance_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int palword;
		int r, g, b;
	
		/* adjust for banking and modify */
		offset |= selected_paletteram << 11;
		local_paletteram.write(offset, data);
	
		/* compute R,G,B */
		palword = (local_paletteram.read(offset | 1) << 8) | local_paletteram.read(offset & ~1);
		r = (palword >> 10) & 0x1f;
		g = (palword >>  5) & 0x1f;
		b = (palword >>  0) & 0x1f;
	
		/* up to 8 bits */
		r = (r << 3) | (r >> 2);
		g = (g << 3) | (g >> 2);
		b = (b << 3) | (b >> 2);
		palette_set_color(offset / 2, r, g, b);
	} };
	
	
	
	/*************************************
	 *
	 *	Video RAM read/write
	 *
	 *************************************/
	
	public static ReadHandlerPtr fromance_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return local_videoram[selected_videoram].read(offset);
	} };
	
	
	public static WriteHandlerPtr fromance_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		local_videoram[selected_videoram].write(offset, data);
		tilemap_mark_tile_dirty(selected_videoram != 0 ? fg_tilemap : bg_tilemap, offset & 0x0fff);
	} };
	
	
	
	/*************************************
	 *
	 *	Scroll registers
	 *
	 *************************************/
	
	public static WriteHandlerPtr fromance_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (flipscreen != 0)
		{
			switch (offset)
			{
				case 0:
					scrollx[1] = (data + (((gfxreg & 0x08) >> 3) * 0x100) - 0x159);
					break;
				case 1:
					scrolly[1] = (data + (((gfxreg & 0x04) >> 2) * 0x100) - 0x10);
					break;
				case 2:
					scrollx[0] = (data + (((gfxreg & 0x20) >> 5) * 0x100) - 0x159);
					break;
				case 3:
					scrolly[0] = (data + (((gfxreg & 0x10) >> 4) * 0x100) - 0x10);
					break;
			}
		}
		else
		{
			switch (offset)
			{
				case 0:
					scrollx[1] = (data + (((gfxreg & 0x08) >> 3) * 0x100) - 0x1f7);
					break;
				case 1:
					scrolly[1] = (data + (((gfxreg & 0x04) >> 2) * 0x100) - 0xfa);
					break;
				case 2:
					scrollx[0] = (data + (((gfxreg & 0x20) >> 5) * 0x100) - 0x1f7);
					break;
				case 3:
					scrolly[0] = (data + (((gfxreg & 0x10) >> 4) * 0x100) - 0xfa);
					break;
			}
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Fake video controller
	 *
	 *************************************/
	
	static timer_callback crtc_interrupt_gen = new timer_callback() {
            public void handler(int param) {
                cpu_cause_interrupt(1, 1);
		if (param != 0)
			crtc_timer = timer_pulse(TIME_IN_HZ(Machine.drv.frames_per_second * param), 0, crtc_interrupt_gen);
            }
        };
	
	public static WriteHandlerPtr fromance_crtc_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (crtc_register)
		{
			/* only register we know about.... */
			case 0x0b:
				if (crtc_timer != null)
					timer_remove(crtc_timer);
				crtc_timer = timer_set(cpu_getscanlinetime(Machine.visible_area.max_y + 1), (data > 0x80) ? 2 : 1, crtc_interrupt_gen);
				break;
	
			default:
				logerror("CRTC register %02X = %02X\n", crtc_register, data & 0xff);
				break;
		}
	} };
	
	
	public static WriteHandlerPtr fromance_crtc_register_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		crtc_register = data;
	} };
	
	
	
	/*************************************
	 *
	 *	Sprite routines (Pipe Dream)
	 *
	 *************************************/
	
	static void draw_sprites(mame_bitmap bitmap, int draw_priority)
	{
		int zoomtable[] = { 0,7,14,20,25,30,34,38,42,46,49,52,54,57,59,61 };
		int offs;
	
		/* draw the sprites */
		for (offs = 0; offs < spriteram_size[0]; offs += 8)
		{
			int data2 = spriteram.read(offs + 4)| (spriteram.read(offs + 5)<< 8);
			int priority = (data2 >> 4) & 1;
	
			/* turns out the sprites are the same as in aerofgt.c */
			if ((data2 & 0x80)!=0 && priority == draw_priority)
			{
				int data0 = spriteram.read(offs + 0)| (spriteram.read(offs + 1)<< 8);
				int data1 = spriteram.read(offs + 2)| (spriteram.read(offs + 3)<< 8);
				int data3 = spriteram.read(offs + 6)| (spriteram.read(offs + 7)<< 8);
				int code = data3 & 0xfff;
				int color = data2 & 0x0f;
				int y = (data0 & 0x1ff) - 6;
				int x = (data1 & 0x1ff) - 13;
				int yzoom = (data0 >> 12) & 15;
				int xzoom = (data1 >> 12) & 15;
				int zoomed = (xzoom | yzoom);
				int ytiles = ((data2 >> 12) & 7) + 1;
				int xtiles = ((data2 >> 8) & 7) + 1;
				int yflip = (data2 >> 15) & 1;
				int xflip = (data2 >> 11) & 1;
				int xt, yt;
	
				/* compute the zoom factor -- stolen from aerofgt.c */
				xzoom = 16 - zoomtable[xzoom] / 8;
				yzoom = 16 - zoomtable[yzoom] / 8;
	
				/* wrap around */
				if (x > Machine.visible_area.max_x) x -= 0x200;
				if (y > Machine.visible_area.max_y) y -= 0x200;
	
				/* normal case */
				if (xflip==0 && yflip==0)
				{
					for (yt = 0; yt < ytiles; yt++)
						for (xt = 0; xt < xtiles; xt++, code++)
							if (zoomed == 0)
								drawgfx(bitmap, Machine.gfx[2], code, color, 0, 0,
										x + xt * 16, y + yt * 16, null, TRANSPARENCY_PEN, 15);
							else
								drawgfxzoom(bitmap, Machine.gfx[2], code, color, 0, 0,
										x + xt * xzoom, y + yt * yzoom, null, TRANSPARENCY_PEN, 15,
										0x1000 * xzoom, 0x1000 * yzoom);
				}
	
				/* xflipped case */
				else if (xflip!=0 && yflip==0)
				{
					for (yt = 0; yt < ytiles; yt++)
						for (xt = 0; xt < xtiles; xt++, code++)
							if (zoomed == 0)
								drawgfx(bitmap, Machine.gfx[2], code, color, 1, 0,
										x + (xtiles - 1 - xt) * 16, y + yt * 16, null, TRANSPARENCY_PEN, 15);
							else
								drawgfxzoom(bitmap, Machine.gfx[2], code, color, 1, 0,
										x + (xtiles - 1 - xt) * xzoom, y + yt * yzoom, null, TRANSPARENCY_PEN, 15,
										0x1000 * xzoom, 0x1000 * yzoom);
				}
	
				/* yflipped case */
				else if (xflip==0 && yflip!=0)
				{
					for (yt = 0; yt < ytiles; yt++)
						for (xt = 0; xt < xtiles; xt++, code++)
							if (zoomed == 0)
								drawgfx(bitmap, Machine.gfx[2], code, color, 0, 1,
										x + xt * 16, y + (ytiles - 1 - yt) * 16, null, TRANSPARENCY_PEN, 15);
							else
								drawgfxzoom(bitmap, Machine.gfx[2], code, color, 0, 1,
										x + xt * xzoom, y + (ytiles - 1 - yt) * yzoom, null, TRANSPARENCY_PEN, 15,
										0x1000 * xzoom, 0x1000 * yzoom);
				}
	
				/* x & yflipped case */
				else
				{
					for (yt = 0; yt < ytiles; yt++)
						for (xt = 0; xt < xtiles; xt++, code++)
							if (zoomed == 0)
								drawgfx(bitmap, Machine.gfx[2], code, color, 1, 1,
										x + (xtiles - 1 - xt) * 16, y + (ytiles - 1 - yt) * 16, null, TRANSPARENCY_PEN, 15);
							else
								drawgfxzoom(bitmap, Machine.gfx[2], code, color, 1, 1,
										x + (xtiles - 1 - xt) * xzoom, y + (ytiles - 1 - yt) * yzoom, null, TRANSPARENCY_PEN, 15,
										0x1000 * xzoom, 0x1000 * yzoom);
				}
			}
		}
	}
	
	
	
	/*************************************
	 *
	 *	Main screen refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr fromance_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_set_scrollx(bg_tilemap, 0, scrollx[0]);
		tilemap_set_scrolly(bg_tilemap, 0, scrolly[0]);
		tilemap_set_scrollx(fg_tilemap, 0, scrollx[1]);
		tilemap_set_scrolly(fg_tilemap, 0, scrolly[1]);
	
		tilemap_draw(bitmap, bg_tilemap, 0, 0);
		tilemap_draw(bitmap, fg_tilemap, 0, 0);
	} };
	
	
	public static VhUpdatePtr pipedrm_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* there seems to be no logical mapping for the X scroll register -- maybe it's gone */
		tilemap_set_scrolly(bg_tilemap, 0, scrolly[1]);
		tilemap_set_scrolly(fg_tilemap, 0, scrolly[0]);
	
		tilemap_draw(bitmap, bg_tilemap, 0, 0);
		tilemap_draw(bitmap, fg_tilemap, 0, 0);
	
		draw_sprites(bitmap, 0);
		draw_sprites(bitmap, 1);
	} };
}
