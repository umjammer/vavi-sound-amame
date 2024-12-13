/***************************************************************************
  Goindol

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

public class goindol
{
	
	public static UBytePtr goindol_bg_videoram=new UBytePtr();
	public static UBytePtr goindol_fg_videoram=new UBytePtr();
	public static UBytePtr goindol_fg_scrollx=new UBytePtr();
	public static UBytePtr goindol_fg_scrolly=new UBytePtr();
	
	public static int[] goindol_fg_videoram_size=new int[1];
	public static int[] goindol_bg_videoram_size=new int[1];
	public static int goindol_char_bank;
	
	static struct_tilemap bg_tilemap, fg_tilemap;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = goindol_fg_videoram.read(2*tile_index+1);
		int attr = goindol_fg_videoram.read(2*tile_index);
		SET_TILE_INFO(
				0,
				code | ((attr & 0x7) << 8) | (goindol_char_bank << 11),
				(attr & 0xf8) >> 3,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = goindol_bg_videoram.read(2*tile_index+1);
		int attr = goindol_bg_videoram.read(2*tile_index);
		SET_TILE_INFO(
				1,
				code | ((attr & 0x7) << 8) | (goindol_char_bank << 11),
				(attr & 0xf8) >> 3,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr goindol_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_SPLIT,      8,8,32,32);
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr goindol_fg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (goindol_fg_videoram.read(offset) != data)
		{
			goindol_fg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset / 2);
		}
	} };
	
	public static WriteHandlerPtr goindol_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (goindol_bg_videoram.read(offset) != data)
		{
			goindol_bg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset / 2);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap, int gfxbank, UBytePtr sprite_ram)
	{
		int offs,sx,sy,tile,palette;
	
		for (offs = 0 ;offs < spriteram_size[0]; offs+=4)
		{
			sx = sprite_ram.read(offs);
			sy = 240-sprite_ram.read(offs+1);
	
			if ((sprite_ram.read(offs+1) >> 3)!=0 && (sx < 248))
			{
				tile	 = ((sprite_ram.read(offs+3))+((sprite_ram.read(offs+2) & 7) << 8));
				tile	+= tile;
				palette	 = sprite_ram.read(offs+2) >> 3;
	
				drawgfx(bitmap,Machine.gfx[gfxbank],
							tile,
							palette,
							0,0,
							sx,sy,
							Machine.visible_area,
							TRANSPARENCY_PEN, 0);
				drawgfx(bitmap,Machine.gfx[gfxbank],
							tile+1,
							palette,
							0,0,
							sx,sy+8,
							Machine.visible_area,
							TRANSPARENCY_PEN, 0);
			}
		}
	}
	
	public static VhUpdatePtr goindol_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_set_scrollx(fg_tilemap,0,goindol_fg_scrollx.read());
		tilemap_set_scrolly(fg_tilemap,0,goindol_fg_scrolly.read());
	
		tilemap_draw(bitmap,bg_tilemap,0,0);
		tilemap_draw(bitmap,fg_tilemap,0,0);
		draw_sprites(bitmap,1,spriteram);
		draw_sprites(bitmap,0,spriteram_2);
	} };
}
