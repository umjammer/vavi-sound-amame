/***************************************************************************

	Atari Tetris hardware

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
//import static mame037b11.mame.tilemapC.*;
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

public class atetris
{
	
	
	static struct_tilemap tilemap;
	
	
	/*************************************
	 *
	 *	Tilemap callback
	 *
	 *************************************/
	
	static GetTileInfoPtr get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = videoram.read(tile_index * 2)| ((videoram.read(tile_index * 2 + 1)& 7) << 8);
		int color = (videoram.read(tile_index * 2 + 1)& 0xf0) >> 4;
	
		SET_TILE_INFO(0, code, color, 0);
            }
        };
	
	
	
	/*************************************
	 *
	 *	Video RAM write
	 *
	 *************************************/
	
	public static WriteHandlerPtr atetris_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram.write(offset,data);
		tilemap_mark_tile_dirty(tilemap, offset / 2);
	} };
	
	
	
	/*************************************
	 *
	 *	Video system start
	 *
	 *************************************/
	
	public static VhStartPtr atetris_vh_start = new VhStartPtr() { public int handler() 
	{
		tilemap = tilemap_create(get_tile_info, tilemap_scan_rows, TILEMAP_OPAQUE, 8,8, 64,32);
		if (tilemap == null)
			return 1;
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Main refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr atetris_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap, tilemap, 0,0);
                //tilemap_update(ALL_TILEMAPS);
                //tilemap_render(ALL_TILEMAPS);
	} };
}
