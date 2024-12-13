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

public class holeland
{
	
	static int palette_offset;
	static struct_tilemap bg_tilemap;
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr holeland_get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = colorram.read(tile_index);
		int tile_number = videoram.read(tile_index)| ((attr & 0x03) << 8);
	
	/*if (keyboard_pressed(KEYCODE_Q) && (attr & 0x10)) tile_number = rand(); */
	/*if (keyboard_pressed(KEYCODE_W) && (attr & 0x20)) tile_number = rand(); */
	/*if (keyboard_pressed(KEYCODE_E) && (attr & 0x40)) tile_number = rand(); */
	/*if (keyboard_pressed(KEYCODE_R) && (attr & 0x80)) tile_number = rand(); */
		SET_TILE_INFO(
				0,
				tile_number,
				palette_offset + ((attr >> 4) & 0x0f),
				TILE_FLIPYX((attr >> 2) & 0x03) | TILE_SPLIT((attr >> 4) & 1));
            }
        };
	
	static GetTileInfoPtr crzrally_get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = colorram.read(tile_index);
		int tile_number = videoram.read(tile_index)| ((attr & 0x03) << 8);
	
		SET_TILE_INFO(
				0,
				tile_number,
				palette_offset + ((attr >> 4) & 0x0f),
				TILE_FLIPYX((attr >> 2) & 0x03) | TILE_SPLIT((attr >> 4) & 1));
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr holeland_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(holeland_get_tile_info,tilemap_scan_rows,TILEMAP_SPLIT,16,16,32,32);
	
		if (bg_tilemap == null)
			return 1;
	
		tilemap_set_transmask(bg_tilemap,0,0xff,0x00); /* split type 0 is totally transparent in front half */
		tilemap_set_transmask(bg_tilemap,1,0x01,0xfe); /* split type 1 has pen 0? transparent in front half */
		return 0;
	} };
	
	public static VhStartPtr crzrally_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(crzrally_get_tile_info,tilemap_scan_cols,TILEMAP_SPLIT,8,8,32,32);
	
		if (bg_tilemap == null)
			return 1;
	
		return 0;
	} };
	
	public static WriteHandlerPtr holeland_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( videoram.read(offset)!=data )
		{
			videoram.write(offset,data);
			tilemap_mark_tile_dirty( bg_tilemap, offset );
		}
	} };
	
	public static WriteHandlerPtr holeland_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( colorram.read(offset)!=data )
		{
			colorram.write(offset,data);
			tilemap_mark_tile_dirty( bg_tilemap, offset );
		}
	} };
        
        static int[] po = new int[2];
	
	public static WriteHandlerPtr holeland_pal_offs_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		if ((data & 1) != po[offset])
		{
			po[offset] = data & 1;
			palette_offset = (po[0] + (po[1] << 1)) << 4;
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	} };
	
	public static WriteHandlerPtr holeland_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrollx(bg_tilemap, 0, data);
	} };
	
	public static WriteHandlerPtr holeland_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset!=0) flip_screen_y_set(data);
		else        flip_screen_x_set(data);
	} };
	
	
	static void holeland_draw_sprites(mame_bitmap bitmap)
	{
		int offs,code,sx,sy,color,flipx, flipy;
	
		/* Weird, sprites entries don't start on DWORD boundary */
		for (offs = 3;offs < spriteram_size[0] - 1;offs += 4)
		{
			sy = 236 - spriteram.read(offs);
			sx = spriteram.read(offs+2);
	
			/* Bit 7 unknown */
			code = spriteram.read(offs+1)& 0x7f;
			color = palette_offset + (spriteram.read(offs+3)>> 4);
	
			/* Bit 0, 1 unknown */
			flipx = spriteram.read(offs+3)& 0x04;
			flipy = spriteram.read(offs+3)& 0x08;
	
			if (flip_screen_x[0] != 0)
			{
				flipx = flipx!=0?0:1;
				sx = 240 - sx;
			}
	
			if (flip_screen_y[0] != 0)
			{
				flipy = flipy!=0?0:1;
				sy = 240 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[1],
					code,
					color,
					flipx,flipy,
					2*sx,2*sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	static void crzrally_draw_sprites(mame_bitmap bitmap)
	{
		int offs,code,sx,sy,color,flipx, flipy;
	
		/* Weird, sprites entries don't start on DWORD boundary */
		for (offs = 3;offs < spriteram_size[0] - 1;offs += 4)
		{
			sy = 236 - spriteram.read(offs);
			sx = spriteram.read(offs+2);
	
			code = spriteram.read(offs+1)+ ((spriteram.read(offs+3)& 0x01) << 8);
			color = (spriteram.read(offs+3)>> 4) + ((spriteram.read(offs+3)& 0x01) << 4);
	
			/* Bit 1 unknown */
			flipx = spriteram.read(offs+3)& 0x04;
			flipy = spriteram.read(offs+3)& 0x08;
	
			if (flip_screen_x[0] != 0)
			{
				flipx = flipx!=0?0:1;
				sx = 240 - sx;
			}
	
			if (flip_screen_y[0] != 0)
			{
				flipy = flipy!=0?0:1;
				sy = 240 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[1],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	public static VhUpdatePtr holeland_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	/*tilemap_mark_all_tiles_dirty(bg_tilemap); */
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_BACK,0);
		holeland_draw_sprites(bitmap);
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_FRONT,0);
	} };
	
	public static VhUpdatePtr crzrally_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,0,0);
		crzrally_draw_sprites(bitmap);
	} };
}
