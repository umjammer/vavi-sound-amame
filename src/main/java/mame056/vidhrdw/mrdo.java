/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.drawgfx.*;
import static mame056.drawgfx.drawgfx;
import static mame056.drawgfx.fillbitmap;
import static mame056.drawgfxH.*;
import static mame056.drawgfxH.TRANSPARENCY_PEN;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.mame.Machine;
import static mame056.palette.*;

import static mame056.tilemapC.tilemap_create;
import static mame056.tilemapC.tilemap_draw;
import static mame056.tilemapC.tilemap_mark_tile_dirty;
import static mame056.tilemapC.tilemap_scan_rows;
import static mame056.tilemapC.tilemap_set_flip;
import static mame056.tilemapC.tilemap_set_scrollx;
import static mame056.tilemapC.tilemap_set_scrolly;
import static mame056.tilemapC.tilemap_set_transparent_pen;
import static mame056.tilemapH.ALL_TILEMAPS;
import static mame056.tilemapH.SET_TILE_INFO;
import static mame056.tilemapH.TILEMAP_FLIPX;
import static mame056.tilemapH.TILEMAP_FLIPY;
import static mame056.tilemapH.TILEMAP_TRANSPARENT;
import static mame056.tilemapH.TILE_IGNORE_TRANSPARENCY;
import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static mame056.tilemapC.*;
//import static mame037b11.mame.tilemapC.*;
import static mame056.tilemapH.*;
import static mame056.vidhrdw.generic.spriteram;
import static mame056.vidhrdw.generic.spriteram_size;


public class mrdo
{
	
	
	public static UBytePtr mrdo_bgvideoram=new UBytePtr(), mrdo_fgvideoram=new UBytePtr();
	static struct_tilemap bg_tilemap, fg_tilemap;
	static int flipscreen;
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Mr. Do! has two 32 bytes palette PROM and a 32 bytes sprite color lookup
	  table PROM.
	  The palette PROMs are connected to the RGB output this way:
	
	  U2:
	  bit 7 -- unused
	        -- unused
	        -- 100 ohm resistor  -diode- BLUE
	        --  75 ohm resistor  -diode- BLUE
	        -- 100 ohm resistor  -diode- GREEN
	        --  75 ohm resistor  -diode- GREEN
	        -- 100 ohm resistor  -diode- RED
	  bit 0 --  75 ohm resistor  -diode- RED
	
	  T2:
	  bit 7 -- unused
	        -- unused
	        -- 150 ohm resistor  -diode- BLUE
	        -- 120 ohm resistor  -diode- BLUE
	        -- 150 ohm resistor  -diode- GREEN
	        -- 120 ohm resistor  -diode- GREEN
	        -- 150 ohm resistor  -diode- RED
	  bit 0 -- 120 ohm resistor  -diode- RED
	
	  200 ohm pulldown on all three components
	
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }

        
	public static VhConvertColorPromPtr mrdo_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		
		int R1 = 150;
		int R2 = 120;
		int R3 = 100;
		int R4 = 75;
		int pull = 200;
		float[] pot = new float[16];
		int[] weight = new int[16];
		float potadjust = 0.2f;	/* diode voltage drop */
	
		for (i = 15;i >= 0;i--)
		{
			float par = 0;
	
			if ((i & 1)!=0) par += 1.0/R1;
			if ((i & 2)!=0) par += 1.0/R2;
			if ((i & 4)!=0) par += 1.0/R3;
			if ((i & 8)!=0) par += 1.0/R4;
			if (par!=0)
			{
				par = 1/par;
				pot[i] = pull/(pull+par) - potadjust;
			}
			else pot[i] = 0;
	
			weight[i] = (int) (255 * pot[i] / pot[15]);
		}
                
                int _palette = 0;
	
		for (i = 0;i < 256;i++)
		{
			int a1,a2;
			int bits0,bits2;
	
			a1 = ((i >> 3) & 0x1c) + (i & 0x03) + 32;
			a2 = ((i >> 0) & 0x1c) + (i & 0x03);
	
			bits0 = (color_prom.read(a1)>> 0) & 0x03;
			bits2 = (color_prom.read(a2)>> 0) & 0x03;
			palette[_palette++] = (char) weight[bits0 + (bits2 << 2)];
			bits0 = (color_prom.read(a1)>> 2) & 0x03;
			bits2 = (color_prom.read(a2)>> 2) & 0x03;
			palette[_palette++] = (char) weight[bits0 + (bits2 << 2)];
			bits0 = (color_prom.read(a1)>> 4) & 0x03;
			bits2 = (color_prom.read(a2)>> 4) & 0x03;
			palette[_palette++] = (char) weight[bits0 + (bits2 << 2)];
		}
	
		color_prom.inc( 64 );
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(2);i++)
		{
			int bits;
	
			if (i < 32)
				bits = color_prom.read(i)& 0x0f;		/* low 4 bits are for sprite color n */
			else
				bits = color_prom.read(i & 0x1f)>> 4;	/* high 4 bits are for sprite color n + 8 */
	
			COLOR(colortable,2,i, bits + ((bits & 0x0c) << 3));
		}
	} };
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = mrdo_bgvideoram.read(tile_index);
		SET_TILE_INFO(
				1,
				mrdo_bgvideoram.read(tile_index+0x400) + ((attr & 0x80) << 1),
				attr & 0x3f,
				((attr & 0x40)!=0) ? TILE_IGNORE_TRANSPARENCY : 0);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int attr = mrdo_fgvideoram.read(tile_index);
		SET_TILE_INFO(
				0,
				mrdo_fgvideoram.read(tile_index+0x400) + ((attr & 0x80) << 1),
				attr & 0x3f,
				(attr & 0x40)!=0 ? TILE_IGNORE_TRANSPARENCY : 0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr mrdo_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (bg_tilemap==null || fg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(bg_tilemap,0);
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr mrdo_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (mrdo_bgvideoram.read(offset) != data)
		{
			mrdo_bgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset & 0x3ff);
		}
	} };
	
	public static WriteHandlerPtr mrdo_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (mrdo_fgvideoram.read(offset) != data)
		{
			mrdo_fgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
		}
	} };
	
	
	public static WriteHandlerPtr mrdo_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrollx(bg_tilemap,0,data);
	} };
	
	public static WriteHandlerPtr mrdo_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrolly(bg_tilemap,0,data);
	} };
	
	
	public static WriteHandlerPtr mrdo_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 1-3 control the playfield priority, but they are not used by */
		/* Mr. Do! so we don't emulate them */
	
		flipscreen = data & 0x01;
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
	
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if (spriteram.read(offs + 1)!= 0)
			{
				drawgfx(bitmap,Machine.gfx[2],
						spriteram.read(offs),spriteram.read(offs + 2)& 0x0f,
						spriteram.read(offs + 2)& 0x10,spriteram.read(offs + 2)& 0x20,
						spriteram.read(offs + 3),256 - spriteram.read(offs + 1),
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	public static VhUpdatePtr mrdo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
		tilemap_draw(bitmap,bg_tilemap,0,0);
		tilemap_draw(bitmap,fg_tilemap,0,0);
		draw_sprites(bitmap);
	} };
}
