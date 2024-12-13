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

public class suprloco
{
	
	public static UBytePtr suprloco_videoram = new UBytePtr();
	
	static struct_tilemap bg_tilemap;
	static int control;
	
	public static int SPR_Y_TOP     = 0;
	public static int SPR_Y_BOTTOM	= 1;
	public static int SPR_X         = 2;
	public static int SPR_COL       = 3;
	public static int SPR_SKIP_LO   = 4;
	public static int SPR_SKIP_HI   = 5;
	public static int SPR_GFXOFS_LO	= 6;
	public static int SPR_GFXOFS_HI	= 7;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  I'm not sure about the resistor values, I'm using the Galaxian ones.
	
	***************************************************************************/
	public static VhConvertColorPromPtr suprloco_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] obsolete, char[] colortable, UBytePtr color_prom) {
		int i;
	
	
		for (i = 0;i < 512;i++)
		{
			int bit0,bit1,bit2,r,g,b;
	
			/* red component */
			bit0 = (color_prom.read(i)>> 0) & 0x01;
			bit1 = (color_prom.read(i)>> 1) & 0x01;
			bit2 = (color_prom.read(i)>> 2) & 0x01;
			r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (color_prom.read(i)>> 3) & 0x01;
			bit1 = (color_prom.read(i)>> 4) & 0x01;
			bit2 = (color_prom.read(i)>> 5) & 0x01;
			g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read(i)>> 6) & 0x01;
			bit2 = (color_prom.read(i)>> 7) & 0x01;
			b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			palette_set_color(i,r,g,b);
	
			/* hack: generate a second bank of sprite palette with red changed to purple */
			if (i >= 256)
			{
				if ((i & 0x0f) == 0x09)
					palette_set_color(i+256,r,g,0xff);
				else
					palette_set_color(i+256,r,g,b);
			}
		}
            }
        };
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = suprloco_videoram.read(2*tile_index+1);
		SET_TILE_INFO(
				0,
				suprloco_videoram.read(2*tile_index) | ((attr & 0x03) << 8),
				(attr & 0x1c) >> 2,
				0);
		tile_info.priority = (attr & 0x20) >> 5;
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr suprloco_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
	
		if (bg_tilemap == null)
			return 1;
	
		tilemap_set_scroll_rows(bg_tilemap,32);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr suprloco_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (suprloco_videoram.read(offset) != data)
		{
			suprloco_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset/2);
		}
	} };
	
	static int[] suprloco_scrollram=new int[32];
	
	public static WriteHandlerPtr suprloco_scrollram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int adj = flip_screen()!=0 ? -8 : 8;
	
		suprloco_scrollram[offset] = data;
		tilemap_set_scrollx(bg_tilemap,offset, data - adj);
	} };
	
	public static ReadHandlerPtr suprloco_scrollram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return suprloco_scrollram[offset];
	} };
	
	public static WriteHandlerPtr suprloco_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* There is probably a palette select in here */
	
	   	/* Bit 0   - coin counter A */
		/* Bit 1   - coin counter B (only used if coinage differs from A) */
		/* Bit 2-3 - probably unused */
		/* Bit 4   - ??? */
		/* Bit 5   - pulsated when loco turns "super" */
		/* Bit 6   - probably unused */
		/* Bit 7   - flip screen */
	
		if ((control & 0x10) != (data & 0x10))
		{
			/*logerror("Bit 4 = %d\n", (data >> 4) & 1); */
		}
	
		coin_counter_w.handler(0, data & 0x01);
		coin_counter_w.handler(1, data & 0x02);
	
		flip_screen_set(data & 0x80);
		tilemap_set_scrolly(bg_tilemap,0,flip_screen()!=0 ? -32 : 0);
	
		control = data;
	} };
	
	
	public static ReadHandlerPtr suprloco_control_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return control;
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static void draw_pixel(mame_bitmap bitmap,int x,int y,int color)
	{
		if (flip_screen()!=0)
		{
			x = bitmap.width - x - 1;
			y = bitmap.height - y - 1;
		}
	
		if (x < Machine.visible_area.min_x ||
			x > Machine.visible_area.max_x ||
			y < Machine.visible_area.min_y ||
			y > Machine.visible_area.max_y)
			return;
	
		plot_pixel.handler(bitmap, x, y, color);
	}
	
	
	static void render_sprite(mame_bitmap bitmap,int spr_number)
	{
		int sx,sy,col,row,height,src,adjy,dy;
		UBytePtr spr_reg=new UBytePtr();
		IntArray spr_palette;
		short skip;	/* bytes to skip before drawing each row (can be negative) */
	
	
		spr_reg	= new UBytePtr(spriteram, 0x10 * spr_number);
	
		src = spr_reg.read(SPR_GFXOFS_LO) + (spr_reg.read(SPR_GFXOFS_HI) << 8);
		skip = (short) (spr_reg.read(SPR_SKIP_LO) + (spr_reg.read(SPR_SKIP_HI) << 8));
	
		height		= spr_reg.read(SPR_Y_BOTTOM) - spr_reg.read(SPR_Y_TOP);
		spr_palette	= new IntArray(Machine.remapped_colortable, 0x100 + 0x10 * spr_reg.read(SPR_COL) + ((control & 0x20)!=0?0x100:0));
		sx = spr_reg.read(SPR_X);
		sy = spr_reg.read(SPR_Y_TOP) + 1;
	
		if (flip_screen() == 0)
		{
			adjy = sy;
			dy = 1;
		}
		else
		{
			adjy = sy + height + 30;  /* some of the sprites are still off by a pixel */
			dy = -1;
		}
	
		for (row = 0;row < height;row++,adjy+=dy)
		{
			int color1,color2,flipx;
			int data;
			UBytePtr gfx=new UBytePtr();
	
			src += skip;
	
			col = 0;
	
			/* get pointer to packed sprite data */
			gfx = new UBytePtr(memory_region(REGION_GFX2), src & 0x7fff);
			flipx = src & 0x8000;   /* flip x */
	
			while (true)
			{
				if (flipx!=0)	/* flip x */
				{
					data = gfx.read();
                                        gfx.dec();
					color1 = data & 0x0f;
					color2 = data >> 4;
				}
				else
				{
					data = gfx.readinc();
					color1 = data >> 4;
					color2 = data & 0x0f;
				}
	
				if (color1 == 15) break;
				if (color1!=0)
					draw_pixel(bitmap,sx+col,  adjy,spr_palette.read(color1));
	
				if (color2 == 15) break;
				if (color2!=0)
					draw_pixel(bitmap,sx+col+1,adjy,spr_palette.read(color2));
	
				col += 2;
			}
		}
	}
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int spr_number;
		UBytePtr spr_reg=new UBytePtr();
	
	
		for (spr_number = 0;spr_number < (spriteram_size[0] >> 4);spr_number++)
		{
			spr_reg = new UBytePtr(spriteram, 0x10 * spr_number);
			if (spr_reg.read(SPR_X) != 0xff)
				render_sprite(bitmap,spr_number);
		}
	}
	
	public static VhUpdatePtr suprloco_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,bg_tilemap,1,0);
	} };
}
