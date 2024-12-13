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

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class xain
{
	
	public static UBytePtr xain_charram=new UBytePtr(), xain_bgram0=new UBytePtr(), xain_bgram1=new UBytePtr();
	
	static struct_tilemap char_tilemap, bgram0_tilemap, bgram1_tilemap;
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr back_scan = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
		/* logical (col,row) . memory offset */
		return (col & 0x0f) + ((row & 0x0f) << 4) + ((col & 0x10) << 4) + ((row & 0x10) << 5);
            }
        };
	
	static GetTileInfoPtr get_bgram0_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = xain_bgram0.read(tile_index | 0x400);
		SET_TILE_INFO(
				2,
				xain_bgram0.read(tile_index) | ((attr & 7) << 8),
				(attr & 0x70) >> 4,
				(attr & 0x80)!=0 ? TILE_FLIPX : 0);
            }
        };
	
	static GetTileInfoPtr get_bgram1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = xain_bgram1.read(tile_index | 0x400);
		SET_TILE_INFO(
				1,
				xain_bgram1.read(tile_index) | ((attr & 7) << 8),
				(attr & 0x70) >> 4,
				(attr & 0x80)!=0 ? TILE_FLIPX : 0);
            }
        };
	
	static GetTileInfoPtr get_char_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = xain_charram.read(tile_index | 0x400);
		SET_TILE_INFO(
				0,
				xain_charram.read(tile_index) | ((attr & 3) << 8),
				(attr & 0xe0) >> 5,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr xain_vh_start = new VhStartPtr() { public int handler() 
	{
		bgram0_tilemap = tilemap_create(get_bgram0_tile_info,back_scan,    TILEMAP_OPAQUE,     16,16,32,32);
		bgram1_tilemap = tilemap_create(get_bgram1_tile_info,back_scan,    TILEMAP_TRANSPARENT,16,16,32,32);
		char_tilemap = tilemap_create(get_char_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
	
		if (bgram0_tilemap==null || bgram1_tilemap==null || char_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(bgram1_tilemap,0);
		tilemap_set_transparent_pen(char_tilemap,0);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr xain_bgram0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xain_bgram0.read(offset) != data)
		{
			xain_bgram0.write(offset, data);
			tilemap_mark_tile_dirty(bgram0_tilemap,offset & 0x3ff);
		}
	} };
	
	public static WriteHandlerPtr xain_bgram1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xain_bgram1.read(offset) != data)
		{
			xain_bgram1.write(offset, data);
			tilemap_mark_tile_dirty(bgram1_tilemap,offset & 0x3ff);
		}
	} };
	
	public static WriteHandlerPtr xain_charram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xain_charram.read(offset) != data)
		{
			xain_charram.write(offset, data);
			tilemap_mark_tile_dirty(char_tilemap,offset & 0x3ff);
		}
	} };
        
        static int[] xain_scrollxP0=new int[2];
        static int[] xain_scrollyP0=new int[2];
        static int[] xain_scrollxP1=new int[2];
        static int[] xain_scrollyP1=new int[2];
	
	public static WriteHandlerPtr xain_scrollxP0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
	
		xain_scrollxP0[offset] = data;
		tilemap_set_scrollx(bgram0_tilemap, 0, xain_scrollxP0[0]|(xain_scrollxP0[1]<<8));
	} };
	
	public static WriteHandlerPtr xain_scrollyP0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		xain_scrollyP0[offset] = data;
		tilemap_set_scrolly(bgram0_tilemap, 0, xain_scrollyP0[0]|(xain_scrollyP0[1]<<8));
	} };
	
	public static WriteHandlerPtr xain_scrollxP1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		xain_scrollxP1[offset] = data;
		tilemap_set_scrollx(bgram1_tilemap, 0, xain_scrollxP1[0]|(xain_scrollxP1[1]<<8));
	} };
	
	public static WriteHandlerPtr xain_scrollyP1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		xain_scrollyP1[offset] = data;
		tilemap_set_scrolly(bgram1_tilemap, 0, xain_scrollyP1[0]|(xain_scrollyP1[1]<<8));
	} };
	
	
	public static WriteHandlerPtr xain_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(data & 1);
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = 0; offs < spriteram_size[0];offs += 4)
		{
			int sx,sy,flipx;
			int attr = spriteram.read(offs+1);
			int numtile = spriteram.read(offs+2)| ((attr & 7) << 8);
			int color = (attr & 0x38) >> 3;
	
			sx = 239 - spriteram.read(offs+3);
			if (sx <= -7) sx += 256;
			sy = 240 - spriteram.read(offs);
			if (sy <= -7) sy += 256;
			flipx = attr & 0x40;
			if (flip_screen() != 0)
			{
				sx = 239 - sx;
				sy = 240 - sy;
				flipx = flipx!=0?0:1;
			}
	
			if ((attr & 0x80) !=0)	/* double height */
			{
				drawgfx(bitmap,Machine.gfx[3],
						numtile,
						color,
						flipx,flip_screen(),
						sx-1,flip_screen()!=0?sy+16:sy-16,
						Machine.visible_area,TRANSPARENCY_PEN,0);
				drawgfx(bitmap,Machine.gfx[3],
						numtile+1,
						color,
						flipx,flip_screen(),
						sx-1,sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
			else
			{
				drawgfx(bitmap,Machine.gfx[3],
						numtile,
						color,
						flipx,flip_screen(),
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	public static VhUpdatePtr xain_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bgram0_tilemap,0,0);
		tilemap_draw(bitmap,bgram1_tilemap,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,char_tilemap,0,0);
	} };
}
