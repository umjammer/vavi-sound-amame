/***************************************************************************

	vidhrdw.c

	Functions to emulate the video hardware of the machine.

	There are only a few differences between the video hardware of Mysterious
	Stones and Mat Mania. The tile bank select bit is different and the sprite
	selection seems to be different as well. Additionally, the palette is stored
	differently. I'm also not sure that the 2nd tile page is really used in
	Mysterious Stones.

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

public class mystston
{
	
	public static UBytePtr mystston_fgvideoram=new UBytePtr();
	public static UBytePtr mystston_bgvideoram=new UBytePtr();
	public static UBytePtr mystston_scroll=new UBytePtr();
	
	static int textcolor;
	static struct_tilemap fg_tilemap, bg_tilemap;
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Mysterious Stones has both palette RAM and a PROM. The PROM is used for
	  text.
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr mystston_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
	
                int _palette=0;
		_palette += 3*24;	/* first 24 colors are RAM */
	
		for (i = 0;i < 32;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
			bit0 = (color_prom.read() >> 0) & 0x01;
			bit1 = (color_prom.read() >> 1) & 0x01;
			bit2 = (color_prom.read() >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
			bit0 = (color_prom.read() >> 3) & 0x01;
			bit1 = (color_prom.read() >> 4) & 0x01;
			bit2 = (color_prom.read() >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read() >> 6) & 0x01;
			bit2 = (color_prom.read() >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	} };
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr get_memory_offset = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
                return (num_cols - 1 - col) * num_rows + row;
            }
        };
        
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code;
	
		code = mystston_fgvideoram.read(tile_index) + ((mystston_fgvideoram.read(tile_index + 0x400) & 0x07) << 8);
		SET_TILE_INFO(
				0,
				code,
				textcolor,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code;
	
		code = mystston_bgvideoram.read(tile_index) + ((mystston_bgvideoram.read(tile_index + 0x200) & 0x01) << 8);
		SET_TILE_INFO(
				1,
				code,
				0,
				(tile_index & 0x10)!=0 ? TILE_FLIPY : 0);	/* the right (lower) side of the screen is flipped */
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr mystston_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap = tilemap_create(get_fg_tile_info,get_memory_offset,TILEMAP_TRANSPARENT, 8, 8,32,32);
		bg_tilemap = tilemap_create(get_bg_tile_info,get_memory_offset,TILEMAP_OPAQUE,     16,16,16,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr mystston_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		mystston_fgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
	} };
	
	public static WriteHandlerPtr mystston_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		mystston_bgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(bg_tilemap,offset & 0x1ff);
	} };
	
	
	public static WriteHandlerPtr mystston_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrolly(bg_tilemap,0,data);
	} };
	
	
	public static WriteHandlerPtr mystston_2000_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int new_textcolor;
	
	
		/* bits 0 and 1 are text color */
		new_textcolor = ((data & 0x01) << 1) | ((data & 0x02) >> 1);
		if (textcolor != new_textcolor)
		{
			tilemap_mark_all_tiles_dirty(fg_tilemap);
			textcolor = new_textcolor;
		}
	
		/* bits 4 and 5 are coin counters */
		coin_counter_w.handler(0,data & 0x10);
		coin_counter_w.handler(1,data & 0x20);
	
		/* bit 7 is screen flip */
		flip_screen_set(data & 0x80);
	
		/* other bits unused? */
/*TODO*///		logerror("PC %04x: 2000 = %02x\n",cpu_get_pc(),data);
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
	
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			if ((spriteram.read(offs)& 0x01) != 0)
			{
				int sx,sy,flipx,flipy;
	
	
				sx = 240 - spriteram.read(offs+3);
				sy = (240 - spriteram.read(offs+2)) & 0xff;
				flipx = spriteram.read(offs)& 0x04;
				flipy = spriteram.read(offs)& 0x02;
				if (flip_screen() != 0)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				drawgfx(bitmap,Machine.gfx[2],
						spriteram.read(offs+1)+ ((spriteram.read(offs)& 0x10) << 4),
						(spriteram.read(offs)& 0x08) >> 3,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	public static VhUpdatePtr mystston_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,fg_tilemap,0,0);
	} };
}
