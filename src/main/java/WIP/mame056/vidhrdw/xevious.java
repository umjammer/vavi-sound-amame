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

public class xevious
{
	
	
	public static UBytePtr xevious_fg_videoram=new UBytePtr(), xevious_fg_colorram=new UBytePtr();
	public static UBytePtr xevious_bg_videoram=new UBytePtr(), xevious_bg_colorram=new UBytePtr();
	
	static struct_tilemap fg_tilemap, bg_tilemap;
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Xevious has three 256x4 palette PROMs (one per gun) and four 512x4 lookup
	  table PROMs (two for sprites, two for background tiles; foreground
	  characters map directly to a palette color without using a PROM).
	  The palette PROMs are connected to the RGB output this way:
	
	  bit 3 -- 220 ohm resistor  -- RED/GREEN/BLUE
			-- 470 ohm resistor  -- RED/GREEN/BLUE
			-- 1  kohm resistor  -- RED/GREEN/BLUE
	  bit 0 -- 2.2kohm resistor  -- RED/GREEN/BLUE
	
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char []colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
        
	public static VhConvertColorPromPtr xevious_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
                
		for (i = 0;i < 128;i++)
		{
			int bit0,bit1,bit2,bit3;
	
	
			/* red component */
			bit0 = (color_prom.read(0)>> 0) & 0x01;
			bit1 = (color_prom.read(0)>> 1) & 0x01;
			bit2 = (color_prom.read(0)>> 2) & 0x01;
			bit3 = (color_prom.read(0)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* green component */
			bit0 = (color_prom.read(256)>> 0) & 0x01;
			bit1 = (color_prom.read(256)>> 1) & 0x01;
			bit2 = (color_prom.read(256)>> 2) & 0x01;
			bit3 = (color_prom.read(256)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* blue component */
			bit0 = (color_prom.read(2*256)>> 0) & 0x01;
			bit1 = (color_prom.read(2*256)>> 1) & 0x01;
			bit2 = (color_prom.read(2*256)>> 2) & 0x01;
			bit3 = (color_prom.read(2*256)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			color_prom.inc();
		}
	
		/* color 0x80 is used by sprites to mark transparency */
		palette[_palette++] = 0;
		palette[_palette++] = 0;
		palette[_palette++] = 0;
	
		color_prom.inc( 128 );  /* the bottom part of the PROM is unused */
		color_prom.inc( 2*256 );
		/* color_prom now points to the beginning of the lookup table */
	
		/* background tiles */
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			COLOR(colortable, 1,i, (color_prom.read(0)& 0x0f) | ((color_prom.read(TOTAL_COLORS(1))& 0x0f) << 4));
	
			color_prom.inc();
		}
		color_prom.inc( TOTAL_COLORS(1) );
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(2);i++)
		{
			int c = (color_prom.read(0)& 0x0f) | ((color_prom.read(TOTAL_COLORS(2))& 0x0f) << 4);
	
			if ((c & 0x80)!=0) COLOR(colortable,2,i, c & 0x7f);
			else COLOR(colortable,2,i, 0x80); /* transparent */
	
			color_prom.inc();
		}
		color_prom.inc( TOTAL_COLORS(2) );
	
		/* foreground characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
		{
			if (i % 2 == 0) COLOR(colortable,0,i, 0x80);  /* transparent */
			else COLOR(colortable,0,i, i / 2);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int attr = xevious_fg_colorram.read(tile_index);
		SET_TILE_INFO(
				0,
				xevious_fg_videoram.read(tile_index),
				((attr & 0x03) << 4) | ((attr & 0x3c) >> 2),
				TILE_FLIPYX((attr & 0xc0) >> 6));
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = xevious_bg_videoram.read(tile_index);
		int attr = xevious_bg_colorram.read(tile_index);
		SET_TILE_INFO(
				1,
				code + ((attr & 0x01) << 8),
				((attr & 0x3c) >> 2) | ((code & 0x80) >> 3) | ((attr & 0x03) << 5),
				TILE_FLIPYX((attr & 0xc0) >> 6));
            }
        };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr xevious_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,     8,8,64,32);
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
	
		if (bg_tilemap==null || fg_tilemap==null)
			return 1;
	
		tilemap_set_scrolldx(fg_tilemap,0,-160);
		tilemap_set_scrolldy(fg_tilemap,0,8);
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr xevious_fg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xevious_fg_videoram.read(offset) != data)
		{
			xevious_fg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr xevious_fg_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xevious_fg_colorram.read(offset) != data)
		{
			xevious_fg_colorram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr xevious_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xevious_bg_videoram.read(offset) != data)
		{
			xevious_bg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr xevious_bg_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (xevious_bg_colorram.read(offset) != data)
		{
			xevious_bg_colorram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr xevious_vh_latch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int reg;
		int scroll = data + ((offset&0x01)<<8);   /* A0 . D8 */
	
		reg = (offset&0xf0)>>4;
	
		switch (reg)
		{
		case 0:
			if (flip_screen() != 0)
				tilemap_set_scrollx(bg_tilemap,0,scroll-312);
			else
				tilemap_set_scrollx(bg_tilemap,0,scroll+20);
			break;
		case 1:
			tilemap_set_scrollx(fg_tilemap,0,scroll+32);
			break;
		case 2:
			tilemap_set_scrolly(bg_tilemap,0,scroll+16);
			break;
		case 3:
			tilemap_set_scrolly(fg_tilemap,0,scroll+18);
			break;
		case 7:
			flip_screen_set(scroll & 1);
			break;
	   default:
/*TODO*///			   logerror("CRTC WRITE REG: %x  Data: %03x\n",reg, scroll);
			   break;
		}
	} };
	
	
	
	
	/*
	background pattern data
	
	colorram mapping
	b000-bfff background attribute
			  bit 0-1 COL:palette set select
			  bit 2-5 AN :color select
			  bit 6   AFF:Y flip
			  bit 7   PFF:X flip
	c000-cfff background pattern name
			  bit 0-7 PP0-7
	
	seet 8A
											2	  +-------+
	COL0,1 --------------------------------------.|backg. |
											1	  |color  |
	PP7------------------------------------------.|replace|
											4	  | ROM   |  6
	AN0-3 ---------------------------------------.|  4H   |----. color code 6 bit
			1  +-----------+	  +--------+	   |  4F   |
	COL0  ---.|B8   ROM 3C| 16   |custom  |  2	|	   |
			8  |		   |----.|shifter |-----.|	   |
	PP0-7 ---.|B0-7 ROM 3D|	  |16.2*8 |	   |	   |
			   +-----------+	  +--------+	   +-------+
	
	font rom controller
		   1  +--------+	 +--------+
	ANF   --.| ROM	|  8  |shift   |  1
		   8  | 3B	 |---.|reg	 |----. font data
	PP0-7 --.|		|	 |8.1*8  |
			  +--------+	 +--------+
	
	font color ( not use color map )
			2  |
	COL0-1 --.|  color code 6 bit
			4  |
	AN0-3  --.|
	
	sprite
	
	ROM 3M,3L color reprace table for sprite
	
	
	
	*/
	
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs,sx,sy;
	
	
		for (offs = 0;offs < spriteram_size[0];offs += 2)
		{
			if ((spriteram.read(offs + 1)& 0x40) == 0)  /* I'm not sure about this one */
			{
				int bank,code,color,flipx,flipy;
	
	
				if ((spriteram_3.read(offs)& 0x80) != 0)
				{
					bank = 4;
					code = spriteram.read(offs)& 0x3f;
				}
				else
				{
					bank = 2 + ((spriteram.read(offs)& 0x80) >> 7);
					code = spriteram.read(offs)& 0x7f;
				}
	
				color = spriteram.read(offs + 1)& 0x7f;
				flipx = spriteram_3.read(offs)& 4;
				flipy = spriteram_3.read(offs)& 8;
				if (flip_screen() != 0)
				{
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
				sx = spriteram_2.read(offs + 1)- 40 + 0x100*(spriteram_3.read(offs + 1)& 1);
				sy = 28*8-spriteram_2.read(offs)-1;
				if ((spriteram_3.read(offs)& 2) != 0)  /* double height (?) */
				{
					if ((spriteram_3.read(offs)& 1) != 0)  /* double width, double height */
					{
						code &= 0x7c;
						drawgfx(bitmap,Machine.gfx[bank],
								code+3,color,flipx,flipy,
								flipx!=0 ? sx : sx+16,flipy!=0 ? sy-16 : sy,
								Machine.visible_area,TRANSPARENCY_COLOR,0x80);
						drawgfx(bitmap,Machine.gfx[bank],
								code+1,color,flipx,flipy,
								flipx!=0 ? sx : sx+16,flipy!=0 ? sy : sy-16,
								Machine.visible_area,TRANSPARENCY_COLOR,0x80);
					}
					code &= 0x7d;
					drawgfx(bitmap,Machine.gfx[bank],
							code+2,color,flipx,flipy,
							flipx!=0 ? sx+16 : sx,flipy!=0 ? sy-16 : sy,
							Machine.visible_area,TRANSPARENCY_COLOR,0x80);
					drawgfx(bitmap,Machine.gfx[bank],
							code,color,flipx,flipy,
							flipx!=0 ? sx+16 : sx,flipy!=0 ? sy : sy-16,
							Machine.visible_area,TRANSPARENCY_COLOR,0x80);
				}
				else if ((spriteram_3.read(offs)& 1) != 0) /* double width */
				{
					code &= 0x7e;
					drawgfx(bitmap,Machine.gfx[bank],
							code,color,flipx,flipy,
							flipx!=0 ? sx+16 : sx,flipy!=0 ? sy-16 : sy,
							Machine.visible_area,TRANSPARENCY_COLOR,0x80);
					drawgfx(bitmap,Machine.gfx[bank],
							code+1,color,flipx,flipy,
							flipx!=0 ? sx : sx+16,flipy!=0 ? sy-16 : sy,
							Machine.visible_area,TRANSPARENCY_COLOR,0x80);
				}
				else	/* normal */
				{
					drawgfx(bitmap,Machine.gfx[bank],
							code,color,flipx,flipy,sx,sy,
							Machine.visible_area,TRANSPARENCY_COLOR,0x80);
				}
			}
		}
	}
	
	
	public static VhUpdatePtr xevious_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,fg_tilemap,0,0);
	} };
}
