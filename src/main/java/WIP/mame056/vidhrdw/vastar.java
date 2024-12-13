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
import static arcadeflex056.osdepend.logerror;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.Machine;
import static mame056.palette.*;
import static mame056.tilemapH.*;
import static mame056.tilemapC.*;
import static mame056.vidhrdw.generic.*;

public class vastar
{
	
	
	
	public static UBytePtr vastar_bg1videoram=new UBytePtr(), vastar_bg2videoram=new UBytePtr(), vastar_fgvideoram=new UBytePtr();
	public static UBytePtr vastar_bg1_scroll=new UBytePtr(), vastar_bg2_scroll=new UBytePtr();
	public static UBytePtr vastar_sprite_priority=new UBytePtr();
	
	static struct_tilemap fg_tilemap, bg1_tilemap, bg2_tilemap;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int code, color;
	
		code = vastar_fgvideoram.read(tile_index + 0x800) | (vastar_fgvideoram.read(tile_index + 0x400) << 8);
		color = vastar_fgvideoram.read(tile_index);
		SET_TILE_INFO(
				0,
				code,
				color & 0x3f,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, color;
	
		code = vastar_bg1videoram.read(tile_index + 0x800) | (vastar_bg1videoram.read(tile_index) << 8);
		color = vastar_bg1videoram.read(tile_index + 0xc00);
		SET_TILE_INFO(
				4,
				code,
				color & 0x3f,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg2_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, color;
	
		code = vastar_bg2videoram.read(tile_index + 0x800) | (vastar_bg2videoram.read(tile_index) << 8);
		color = vastar_bg2videoram.read(tile_index + 0xc00);
		SET_TILE_INFO(
				3,
				code,
				color & 0x3f,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr vastar_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap  = tilemap_create(get_fg_tile_info, tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		bg1_tilemap = tilemap_create(get_bg1_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		bg2_tilemap = tilemap_create(get_bg2_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (fg_tilemap==null || bg1_tilemap==null || bg2_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
		tilemap_set_transparent_pen(bg1_tilemap,0);
		tilemap_set_transparent_pen(bg2_tilemap,0);
	
		tilemap_set_scroll_cols(bg1_tilemap, 32);
		tilemap_set_scroll_cols(bg2_tilemap, 32);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr vastar_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		vastar_fgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
	} };
	
	public static WriteHandlerPtr vastar_bg1videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		vastar_bg1videoram.write(offset, data);
		tilemap_mark_tile_dirty(bg1_tilemap,offset & 0x3ff);
	} };
	
	public static WriteHandlerPtr vastar_bg2videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		vastar_bg2videoram.write(offset, data);
		tilemap_mark_tile_dirty(bg2_tilemap,offset & 0x3ff);
	} };
	
	
	public static ReadHandlerPtr vastar_bg1videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vastar_bg1videoram.read(offset);
	} };
	
	public static ReadHandlerPtr vastar_bg2videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vastar_bg2videoram.read(offset);
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
	
		for (offs = 0; offs < spriteram_size[0]; offs += 2)
		{
			int code, sx, sy, color, flipx, flipy;
	
	
			code = ((spriteram_3.read(offs)& 0xfc) >> 2) + ((spriteram_2.read(offs)& 0x01) << 6)
					+ ((offs & 0x20) << 2);
	
			sx = spriteram_3.read(offs + 1);
			sy = spriteram.read(offs);
			color = spriteram.read(offs + 1)& 0x3f;
			flipx = spriteram_3.read(offs)& 0x02;
			flipy = spriteram_3.read(offs)& 0x01;
	
			if (flip_screen() != 0)
			{
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			if ((spriteram_2.read(offs)& 0x08) != 0)	/* double width */
			{
				if (flip_screen() == 0)
					sy = 224 - sy;
	
				drawgfx(bitmap,Machine.gfx[2],
						code/2,
						color,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
				/* redraw with wraparound */
				drawgfx(bitmap,Machine.gfx[2],
						code/2,
						color,
						flipx,flipy,
						sx,sy+256,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
			else
			{
				if (flip_screen() == 0)
					sy = 240 - sy;
	
				drawgfx(bitmap,Machine.gfx[1],
						code,
						color,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	public static VhUpdatePtr vastar_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int i;
	
	
		for (i = 0;i < 32;i++)
		{
			tilemap_set_scrolly(bg1_tilemap,i,vastar_bg1_scroll.read(i));
			tilemap_set_scrolly(bg2_tilemap,i,vastar_bg2_scroll.read(i));
		}
	
		switch (vastar_sprite_priority.read())
		{
		case 0:
			tilemap_draw(bitmap, bg1_tilemap, TILEMAP_IGNORE_TRANSPARENCY,0);
			draw_sprites(bitmap);
			tilemap_draw(bitmap, bg2_tilemap, 0,0);
			tilemap_draw(bitmap, fg_tilemap, 0,0);
			break;
	
		case 2:
			tilemap_draw(bitmap, bg1_tilemap, TILEMAP_IGNORE_TRANSPARENCY,0);
			draw_sprites(bitmap);
			tilemap_draw(bitmap, bg1_tilemap, 0,0);
			tilemap_draw(bitmap, bg2_tilemap, 0,0);
			tilemap_draw(bitmap, fg_tilemap, 0,0);
			break;
	
		case 3:
			tilemap_draw(bitmap, bg1_tilemap, TILEMAP_IGNORE_TRANSPARENCY,0);
			tilemap_draw(bitmap, bg2_tilemap, 0,0);
			tilemap_draw(bitmap, fg_tilemap, 0,0);
			draw_sprites(bitmap);
			break;
	
		default:
			logerror("Unimplemented priority %X\n", vastar_sprite_priority.read());
			break;
		}
	} };
}
