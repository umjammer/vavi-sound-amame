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
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.usrintrf.usrintf_showmessage;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.inptport.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class sidepckt
{
	
	
	static struct_tilemap bg_tilemap;
	static int flipscreen;
	
	public static VhConvertColorPromPtr sidepckt_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] obsolete, char[] colortable, UBytePtr color_prom) {
		int i;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2,bit3,r,g,b;
	
			/* red component */
			bit0 = (color_prom.read(i)>> 4) & 0x01;
			bit1 = (color_prom.read(i)>> 5) & 0x01;
			bit2 = (color_prom.read(i)>> 6) & 0x01;
			bit3 = (color_prom.read(i)>> 7) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* green component */
			bit0 = (color_prom.read(i)>> 0) & 0x01;
			bit1 = (color_prom.read(i)>> 1) & 0x01;
			bit2 = (color_prom.read(i)>> 2) & 0x01;
			bit3 = (color_prom.read(i)>> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* blue component */
			bit0 = (color_prom.read(i + Machine.drv.total_colors)>> 0) & 0x01;
			bit1 = (color_prom.read(i + Machine.drv.total_colors)>> 1) & 0x01;
			bit2 = (color_prom.read(i + Machine.drv.total_colors)>> 2) & 0x01;
			bit3 = (color_prom.read(i + Machine.drv.total_colors)>> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			palette_set_color(i,r,g,b);
		}
            }
        };
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info = new GetTileInfoPtr() {
                public void handler(int tile_index) {
                    int attr = colorram.read(tile_index);
                    SET_TILE_INFO(
                                    0,
                                    videoram.read(tile_index)+ ((attr & 0x07) << 8),
                                    ((attr & 0x10) >> 3) | ((attr & 0x20) >> 5),
                                    TILE_FLIPX | TILE_SPLIT((attr & 0x80) >> 7));
                }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr sidepckt_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_SPLIT,8,8,32,32);
	
		if (bg_tilemap == null)
			return 1;
	
		tilemap_set_transmask(bg_tilemap,0,0xff,0x00); /* split type 0 is totally transparent in front half */
		tilemap_set_transmask(bg_tilemap,1,0x01,0xfe); /* split type 1 has pen 0 transparent in front half */
	
		tilemap_set_flip(ALL_TILEMAPS,TILEMAP_FLIPX);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr sidepckt_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (videoram.read(offset)!= data)
		{
			videoram.write(offset,data);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr sidepckt_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (colorram.read(offset)!= data)
		{
			colorram.write(offset,data);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr sidepckt_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flipscreen = data;
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? TILEMAP_FLIPY : TILEMAP_FLIPX);
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = 0;offs < spriteram_size[0]; offs += 4)
		{
			int sx,sy,code,color,flipx,flipy;
	
			code = spriteram.read(offs+3)+ ((spriteram.read(offs+1)& 0x03) << 8);
			color = (spriteram.read(offs+1)& 0xf0) >> 4;
	
			sx = spriteram.read(offs+2)-2;
			sy = spriteram.read(offs);
	
			flipx = spriteram.read(offs+1)& 0x08;
			flipy = spriteram.read(offs+1)& 0x04;
	
			drawgfx(bitmap,Machine.gfx[1],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
			/* wraparound */
			drawgfx(bitmap,Machine.gfx[1],
					code,
					color,
					flipx,flipy,
					sx-256,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	
	public static VhUpdatePtr sidepckt_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_BACK,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_FRONT,0);
	} };
}
