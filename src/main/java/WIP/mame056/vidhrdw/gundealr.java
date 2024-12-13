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
import static common.ptr.*;
import static mame056.tilemapH.*;
import static mame056.tilemapC.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static common.libc.cstring.*;

public class gundealr
{
	
	public static UBytePtr gundealr_bg_videoram=new UBytePtr(), gundealr_fg_videoram=new UBytePtr();
	
	static struct_tilemap bg_tilemap, fg_tilemap;
	static int flipscreen;
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		int attr = gundealr_bg_videoram.read(2*tile_index+1);
		SET_TILE_INFO(
				0,
				gundealr_bg_videoram.read(2*tile_index) + ((attr & 0x07) << 8),
				(attr & 0xf0) >> 4,
				0);
            }
        };
	
	static GetMemoryOffsetPtr gundealr_scan = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
                
		/* logical (col,row) -> memory offset */
		return (row & 0x0f) + ((col & 0x3f) << 4) + ((row & 0x10) << 6);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		
                int attr = gundealr_fg_videoram.read(2*tile_index+1);
		SET_TILE_INFO(
				1,
				gundealr_fg_videoram.read(2*tile_index) + ((attr & 0x03) << 8),
				(attr & 0xf0) >> 4,
				0);
            }
        };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr gundealr_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_cols,TILEMAP_OPAQUE,      8, 8,32,32);
		fg_tilemap = tilemap_create(get_fg_tile_info,gundealr_scan,    TILEMAP_TRANSPARENT,16,16,64,32);
	
		if (bg_tilemap==null || fg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,15);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr gundealr_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (gundealr_bg_videoram.read(offset) != data)
		{
			gundealr_bg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset/2);
		}
	} };
	
	public static WriteHandlerPtr gundealr_fg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (gundealr_fg_videoram.read(offset) != data)
		{
			gundealr_fg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset/2);
		}
	} };
	
	public static WriteHandlerPtr gundealr_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r,g,b,val;
	
	
		paletteram.write(offset,data);
	
		val = paletteram.read(offset & ~1);
		r = (val >> 4) & 0x0f;
		g = (val >> 0) & 0x0f;
	
		val = paletteram.read(offset | 1);
		b = (val >> 4) & 0x0f;
		/* TODO: the bottom 4 bits are used as well, but I'm not sure about the meaning */
	
		r = 0x11 * r;
		g = 0x11 * g;
		b = 0x11 * b;
	
		palette_set_color(offset / 2,r,g,b);
	} };
        
        static int[] scroll = new int[4];
	
	public static WriteHandlerPtr gundealr_fg_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		scroll[offset] = data;
		tilemap_set_scrollx(fg_tilemap,0,scroll[1] | ((scroll[0] & 0x03) << 8));
		tilemap_set_scrolly(fg_tilemap,0,scroll[3] | ((scroll[2] & 0x03) << 8));
	} };
	
	public static WriteHandlerPtr yamyam_fg_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
		tilemap_set_scrollx(fg_tilemap,0,scroll[0] | ((scroll[1] & 0x03) << 8));
		tilemap_set_scrolly(fg_tilemap,0,scroll[2] | ((scroll[3] & 0x03) << 8));
	} };
	
	public static WriteHandlerPtr gundealr_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flipscreen = data;
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr gundealr_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,0,0);
		tilemap_draw(bitmap,fg_tilemap,0,0);
	} };
}
