/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

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
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.palette.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;


public class citycon
{
	
	public static UBytePtr citycon_videoram = new UBytePtr();
	public static UBytePtr citycon_linecolor = new UBytePtr();
	public static UBytePtr citycon_scroll = new UBytePtr();
	
	static int bg_image;
	static struct_tilemap bg_tilemap, fg_tilemap;
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	public static GetMemoryOffsetPtr citycon_scan = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
            	/* logical (col,row) . memory offset */
		return (col & 0x1f) + ((row & 0x1f) << 5) + ((col & 0x60) << 5);
            }
        };
	
	public static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		SET_TILE_INFO(
				0,
				citycon_videoram.read(tile_index),
				(tile_index & 0x03e0) >> 5,	/* color depends on scanline only */
				0);
            }
        };
	
	public static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		UBytePtr rom = new UBytePtr(memory_region(REGION_GFX4));
		int code = rom.read(0x1000 * bg_image + tile_index);
		SET_TILE_INFO(
				3 + bg_image,
				code,
				rom.read(0xc000 + 0x100 * bg_image + code),
				0);
            }
        };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr citycon_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap = tilemap_create(get_fg_tile_info,citycon_scan,TILEMAP_TRANSPARENT,8,8,128,32);
		bg_tilemap = tilemap_create(get_bg_tile_info,citycon_scan,TILEMAP_OPAQUE,     8,8,128,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
		tilemap_set_scroll_rows(fg_tilemap,32);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr citycon_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (citycon_videoram.read(offset) != data)
		{
			citycon_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
	} };
	
	
	public static WriteHandlerPtr citycon_linecolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		citycon_linecolor.write(offset, data);
	} };
	
	
	public static WriteHandlerPtr citycon_background_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 4-7 control the background image */
		if (bg_image != (data >> 4))
		{
			bg_image = (data >> 4);
			tilemap_mark_all_tiles_dirty(bg_tilemap);
		}
	
		/* bit 0 flips screen */
		/* it is also used to multiplex player 1 and player 2 controls */
		flip_screen_set(data & 0x01);
	
		/* bits 1-3 are unknown */
	//	if ((data & 0x0e) != 0) logerror("background register = %02x\n",data);
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = spriteram_size[0]-4;offs >= 0;offs -= 4)
		{
			int sx,sy,flipx;
	
	
			sx = spriteram.read(offs + 3);
			sy = 239 - spriteram.read(offs);
			flipx = ~spriteram.read(offs + 2)& 0x10;
			if (flip_screen() != 0)
			{
				sx = 240 - sx;
				sy = 238 - sy;
				flipx=flipx!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[(spriteram.read(offs + 1)& 0x80) != 0 ? 2 : 1],
					spriteram.read(offs + 1)& 0x7f,
					spriteram.read(offs + 2)& 0x0f,
					flipx,flip_screen(),
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	
	static void changecolor_RRRRGGGGBBBBxxxx(int color,int indx)
	{
		int r,g,b;
		int data = paletteram.read(2*indx | 1)| (paletteram.read(2*indx)<< 8);
	
		r = (data >> 12) & 0x0f;
		g = (data >>  8) & 0x0f;
		b = (data >>  4) & 0x0f;
	
		r = (r << 4) | r;
		g = (g << 4) | g;
		b = (b << 4) | b;
	
		palette_set_color(color,r,g,b);
	}
	
	public static VhUpdatePtr citycon_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,scroll;
	
	
		/* Update the virtual palette to support text color code changing on every scanline. */
		for (offs = 0;offs < 256;offs++)
		{
			int indx = citycon_linecolor.read(offs);
			int i;
	
			for (i = 0;i < 4;i++)
				changecolor_RRRRGGGGBBBBxxxx(640 + 4*offs + i,512 + 4*indx + i);
		}
	
	
		scroll = citycon_scroll.read(0)*256 + citycon_scroll.read(1);
		tilemap_set_scrollx(bg_tilemap,0,scroll >> 1);
		for (offs = 6;offs < 32;offs++)
			tilemap_set_scrollx(fg_tilemap,offs,scroll);
	
		tilemap_draw(bitmap,bg_tilemap,0,0);
		tilemap_draw(bitmap,fg_tilemap,0,0);
		draw_sprites(bitmap);
	} };
}
