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
import static mame056.cpuintrfH.*;
import static mame056.palette.*;
import static mame056.memory.*;
import static mame056.memoryH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class lwings
{
	
	public static UBytePtr lwings_fgvideoram = new UBytePtr();
	public static UBytePtr lwings_bg1videoram = new UBytePtr();
	
	static int bAvengersHardware, bg2_image;
	static struct_tilemap fg_tilemap, bg1_tilemap, bg2_tilemap;
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr get_bg2_memory_offset = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {                
		return (row * 0x800) | (col * 2);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		int code, color;
	
		code = lwings_fgvideoram.read(tile_index);
		color = lwings_fgvideoram.read(tile_index + 0x400);
		SET_TILE_INFO(
				0,
				code + ((color & 0xc0) << 2),
				color & 0x0f,
				TILE_FLIPYX((color & 0x30) >> 4));
            }
        };
	
	static GetTileInfoPtr lwings_get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, color;
	
		code = lwings_bg1videoram.read(tile_index);
		color = lwings_bg1videoram.read(tile_index + 0x400);
		SET_TILE_INFO(
				1,
				code + ((color & 0xe0) << 3),
				color & 0x07,
				TILE_FLIPYX((color & 0x18) >> 3));
            }
        };
	
	static GetTileInfoPtr trojan_get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int avengers_color_remap[]	= {6,7,4,5,2,3,0,1};
		int code, color;
	
		code = lwings_bg1videoram.read(tile_index);
		color = lwings_bg1videoram.read(tile_index + 0x400);
		code += (color & 0xe0)<<3;
		SET_TILE_INFO(
				1,
				code,
				bAvengersHardware!=0 ? avengers_color_remap[color & 7] : (color & 7),
				TILE_SPLIT((color & 0x08) >> 3) | ((color & 0x10)!=0 ? TILE_FLIPX : 0));
            }
        };
	
	static GetTileInfoPtr get_bg2_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, color;
	
		code = new UBytePtr(memory_region(REGION_GFX5)).read(bg2_image * 0x20 + tile_index);
		color = new UBytePtr(memory_region(REGION_GFX5)).read(bg2_image * 0x20 + tile_index + 1);
		SET_TILE_INFO(
				3,
				code + ((color & 0x80) << 1),
				color & 0x07,
				TILE_FLIPYX((color & 0x30) >> 4));
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr lwings_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap  = tilemap_create(get_fg_tile_info,        tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
		bg1_tilemap = tilemap_create(lwings_get_bg1_tile_info,tilemap_scan_cols,TILEMAP_OPAQUE,     16,16,32,32);
	
		if (fg_tilemap==null || bg1_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,3);
	
		return 0;
	} };
	
	public static VhStartPtr trojan_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap  = tilemap_create(get_fg_tile_info,        tilemap_scan_rows,    TILEMAP_TRANSPARENT,8, 8,32,32);
		bg1_tilemap = tilemap_create(trojan_get_bg1_tile_info,tilemap_scan_cols,    TILEMAP_SPLIT,     16,16,32,32);
		bg2_tilemap = tilemap_create(get_bg2_tile_info,       get_bg2_memory_offset,TILEMAP_OPAQUE,    16,16,32,16);
	
		if( fg_tilemap!=null && bg1_tilemap!=null && bg2_tilemap!=null )
		{
			tilemap_set_transparent_pen(fg_tilemap,3);
			tilemap_set_transmask(bg1_tilemap,0,0xffff,0x0001); /* split type 0 is totally transparent in front half */
			tilemap_set_transmask(bg1_tilemap,1,0xf07f,0x0f81); /* split type 1 has pens 7-11 opaque in front half */
	
			bAvengersHardware = 0;
			return 0;
		}
		return 1; /* error */
	} };
	
	public static VhStartPtr avengers_vh_start = new VhStartPtr() { public int handler() 
	{
		int result = trojan_vh_start.handler();
		bAvengersHardware = 1;
		return result;
	} };
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr lwings_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		lwings_fgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
	} };
	
	public static WriteHandlerPtr lwings_bg1videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		lwings_bg1videoram.write(offset, data);
		tilemap_mark_tile_dirty(bg1_tilemap,offset & 0x3ff);
	} };
	
	static int[] scroll = new int[2];
        
	public static WriteHandlerPtr lwings_bg1_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
		tilemap_set_scrollx(bg1_tilemap,0,scroll[0] | (scroll[1] << 8));
	} };
	
	public static WriteHandlerPtr lwings_bg1_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
		tilemap_set_scrolly(bg1_tilemap,0,scroll[0] | (scroll[1] << 8));
	} };
	
	public static WriteHandlerPtr lwings_bg2_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrollx(bg2_tilemap,0,data);
	} };
	
	public static WriteHandlerPtr lwings_bg2_image_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (bg2_image != data)
		{
			bg2_image = data;
			tilemap_mark_all_tiles_dirty(bg2_tilemap);
		}
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static int is_sprite_on(int offs)
	{
		int sx,sy;
	
	
		sx = buffered_spriteram.read(offs + 3) - 0x100 * (buffered_spriteram.read(offs + 1) & 0x01);
		sy = buffered_spriteram.read(offs + 2);
	
		return sx!=0 && sy!=0 ? 1 : 0;
	}
	
	static void lwings_draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
	
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if (is_sprite_on(offs) != 0)
			{
				int code,color,sx,sy,flipx,flipy;
	
	
				sx = buffered_spriteram.read(offs + 3) - 0x100 * (buffered_spriteram.read(offs + 1) & 0x01);
				sy = buffered_spriteram.read(offs + 2);
				code = buffered_spriteram.read(offs) | (buffered_spriteram.read(offs + 1) & 0xc0) << 2;
				color = (buffered_spriteram.read(offs + 1) & 0x38) >> 3;
				flipx = buffered_spriteram.read(offs + 1) & 0x02;
				flipy = buffered_spriteram.read(offs + 1) & 0x04;
	
				if (flip_screen() != 0)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				drawgfx(bitmap,Machine.gfx[2],
						code,color,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,15);
			}
		}
	}
	
	static void trojan_draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
	
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if (is_sprite_on(offs) != 0)
			{
				int code,color,sx,sy,flipx,flipy;
	
	
				sx = buffered_spriteram.read(offs + 3) - 0x100 * (buffered_spriteram.read(offs + 1) & 0x01);
				sy = buffered_spriteram.read(offs + 2);
				code = buffered_spriteram.read(offs) |
					   ((buffered_spriteram.read(offs + 1) & 0x20) << 4) |
					   ((buffered_spriteram.read(offs + 1) & 0x40) << 2) |
					   ((buffered_spriteram.read(offs + 1) & 0x80) << 3);
				color = (buffered_spriteram.read(offs + 1) & 0x0e) >> 1;
	
				if( bAvengersHardware != 0 )
				{
					flipx = 0;										/* Avengers */
					flipy = ~buffered_spriteram.read(offs + 1) & 0x10;
				}
				else
				{
					flipx = buffered_spriteram.read(offs + 1) & 0x10;	/* Trojan */
					flipy = 1;
				}
	
				if (flip_screen() != 0)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				drawgfx(bitmap,Machine.gfx[2],
						code,color,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,15);
			}
		}
	}
	
	public static VhUpdatePtr lwings_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg1_tilemap,0,0);
		lwings_draw_sprites(bitmap);
		tilemap_draw(bitmap,fg_tilemap,0,0);
	} };
	
	public static VhUpdatePtr trojan_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg2_tilemap,0,0);
		tilemap_draw(bitmap,bg1_tilemap,TILEMAP_BACK,0);
		trojan_draw_sprites(bitmap);
		tilemap_draw(bitmap,bg1_tilemap,TILEMAP_FRONT,0);
		tilemap_draw(bitmap,fg_tilemap,0,0);
	} };
	
	public static VhEofCallbackPtr lwings_eof_callback = new VhEofCallbackPtr() {
            public void handler() {            
		buffer_spriteram_w.handler(0,0);
            }
        };
}
