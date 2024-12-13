/***************************************************************************

							-= Clash Road =-

					driver by	Luca Elia (l.elia@tin.it)

	[ 2 Horizontally Scrolling Layers ]

		Size :	512 x 256
		Tiles:	16 x 16 x 4.

		These 2 layers share the same graphics and X scroll value.
		The tile codes are stuffed together in memory too: first one
		layer's row, then the other's (and so on for all the rows).

	[ 1 Fixed Layer ]

		Size :	(256 + 32) x 256
		Tiles:	8 x 8 x 4.

		This is like a 32x32 tilemap, but the top and bottom rows (that
		fall outside the visible area) are used to widen the tilemap
		horizontally, adding 2 vertical columns both sides.

		The result is a 36x28 visible tilemap.

	[ 64? sprites ]

		Sprites are 16 x 16 x 4.

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

public class clshroad
{
	
	/* Variables only used here: */
	
	static struct_tilemap tilemap_0a, tilemap_0b, tilemap_1;
	
	/* Variables & functions needed by drivers: */
	
	public static UBytePtr clshroad_vram_0 = new UBytePtr(), clshroad_vram_1=new UBytePtr();
	public static UBytePtr clshroad_vregs = new UBytePtr(2);
	
	
	
	public static WriteHandlerPtr clshroad_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set( data & 1 );
	} };
	
	public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
        
	public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
        
	public static VhConvertColorPromPtr firebatl_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] obsolete, char[] colortable, UBytePtr color_prom) {
                
		int i;
		
/*TODO*///	#if 1
		for (i = 0;i < 256;i++)
		{
			int bit0,bit1,bit2,bit3,r,g,b;
	
	
			/* red component */
			bit0 = (color_prom.read(i)>> 0) & 0x01;
			bit1 = (color_prom.read(i)>> 1) & 0x01;
			bit2 = (color_prom.read(i)>> 2) & 0x01;
			bit3 = (color_prom.read(i)>> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* green component */
			bit0 = (color_prom.read(i + 256)>> 0) & 0x01;
			bit1 = (color_prom.read(i + 256)>> 1) & 0x01;
			bit2 = (color_prom.read(i + 256)>> 2) & 0x01;
			bit3 = (color_prom.read(i + 256)>> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* blue component */
			bit0 = (color_prom.read(i + 2*256)>> 0) & 0x01;
			bit1 = (color_prom.read(i + 2*256)>> 1) & 0x01;
			bit2 = (color_prom.read(i + 2*256)>> 2) & 0x01;
			bit3 = (color_prom.read(i + 2*256)>> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			palette_set_color(i,r,g,b);
		}
/*TODO*///	#endif
	
		color_prom.inc( 3*256 );
		/* color_prom now points to the beginning of the lookup table */
	
	
		for (i = 0;i < TOTAL_COLORS(2);i++)
			COLOR(colortable,2,i, ((color_prom.read(i)& 0x0f) << 4) + (color_prom.read(i+256)& 0x0f));
            }
        };
	
	
	/***************************************************************************
	
							Callbacks for the TileMap code
	
	***************************************************************************/
	
	/***************************************************************************
	
							  Layers 0 Tiles Format
	
	Offset:
	
		00-3f:	Even bytes: Codes	Odd bytes: Colors	<- Layer B First Row
		40-7f:	Even bytes: Codes	Odd bytes: Colors	<- Layer A First Row
		..										<- 2nd Row
		..										<- 3rd Row
		etc.
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info_0a = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int code, color;
		tile_index = (tile_index & 0x1f) + (tile_index & ~0x1f)*2;
		code	=	clshroad_vram_0.read( tile_index * 2 + 0x40 );
		color	=	clshroad_vram_0.read( tile_index * 2 + 0x41 );
		SET_TILE_INFO(
				1,
				code,
				color & 0x0f,
				0);
            }
        };
	
	static GetTileInfoPtr get_tile_info_0b = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, color;
		tile_index = (tile_index & 0x1f) + (tile_index & ~0x1f)*2;
		code	=	clshroad_vram_0.read( tile_index * 2 + 0x00 );
		color	=	clshroad_vram_0.read( tile_index * 2 + 0x01 );
		SET_TILE_INFO(
				1,
				code,
				color & 0x0f,
				0);
            }
        };
	
	public static WriteHandlerPtr clshroad_vram_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (clshroad_vram_0.read(offset) != data)
		{
			int tile_index = offset / 2;
			int tile = (tile_index & 0x1f) + (tile_index & ~0x3f)/2;
			clshroad_vram_0.write(offset, data);
			if ((tile_index & 0x20)!=0)	tilemap_mark_tile_dirty(tilemap_0a, tile);
			else					tilemap_mark_tile_dirty(tilemap_0b, tile);
		}
	} };
	
	/***************************************************************************
	
							  Layer 1 Tiles Format
	
	Offset:
	
		000-3ff		Code
		400-7ff		7654----	Code (High bits)
					----3210	Color
	
		This is like a 32x32 tilemap, but the top and bottom rows (that
		fall outside the visible area) are used to widen the tilemap
		horizontally, adding 2 vertical columns both sides.
	
		The result is a 36x28 visible tilemap.
	
	***************************************************************************/
	
	/* logical (col,row) . memory offset */
	static GetMemoryOffsetPtr tilemap_scan_rows_extra = new GetMemoryOffsetPtr() {
            @Override
            public int handler(int col, int row, int num_cols, int num_rows) {
                
		// The leftmost columns come from the bottom rows
		if (col <= 0x01)	return row + (col + 0x1e) * 0x20;
		// The rightmost columns come from the top rows
		if (col >= 0x22)	return row + (col - 0x22) * 0x20;
	
		// These are not visible, but *must* be mapped to other tiles than
		// those used by the leftmost and rightmost columns (tilemap "bug"?)
		if (row <= 0x01)	return 0;
		if (row >= 0x1e)	return 0;
	
		// "normal" layout for the rest.
		return (col-2) + row * 0x20;
            }
        };
	
	static GetTileInfoPtr get_tile_info_fb1 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code	=	clshroad_vram_1.read( tile_index + 0x000 );
		int color	=	clshroad_vram_1.read( tile_index + 0x400 );
		SET_TILE_INFO(
				2,
				code,
				color & 0x3f,
				0);
            }
        };
	
	static GetTileInfoPtr get_tile_info_1 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code	=	clshroad_vram_1.read( tile_index + 0x000 );
		int color	=	clshroad_vram_1.read( tile_index + 0x400 );
		SET_TILE_INFO(
				2,
				code + ((color & 0xf0)<<4),
				color & 0x0f,
				0);
            }
        };
	
	public static WriteHandlerPtr clshroad_vram_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (clshroad_vram_1.read(offset) != data)
		{
			clshroad_vram_1.write(offset, data);
			tilemap_mark_tile_dirty(tilemap_1, offset % 0x400);
		}
	} };
	
	
	public static VhStartPtr firebatl_vh_start = new VhStartPtr() { public int handler() 
	{
		/* These 2 use the graphics and scroll value */
		tilemap_0a = tilemap_create(get_tile_info_0a,tilemap_scan_rows,TILEMAP_OPAQUE,     16,16,0x20,0x10);
		tilemap_0b = tilemap_create(get_tile_info_0b,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,0x20,0x10);
		/* Text (No scrolling) */
		tilemap_1  = tilemap_create(get_tile_info_fb1,tilemap_scan_rows_extra,TILEMAP_TRANSPARENT_COLOR,8,8,0x24,0x20);
	
		if (tilemap_0a==null || tilemap_0b==null || tilemap_1==null)
			return 1;
	
		tilemap_set_scroll_rows( tilemap_0a, 1);
		tilemap_set_scroll_rows( tilemap_0b, 1);
		tilemap_set_scroll_rows( tilemap_1,  1);
	
		tilemap_set_scroll_cols( tilemap_0a, 1);
		tilemap_set_scroll_cols( tilemap_0b, 1);
		tilemap_set_scroll_cols( tilemap_1,  1);
	
		tilemap_set_scrolldx( tilemap_0a, -0x30, -0xb5);
		tilemap_set_scrolldx( tilemap_0b, -0x30, -0xb5);
	
		tilemap_set_transparent_pen( tilemap_0a, 0 );
		tilemap_set_transparent_pen( tilemap_0b, 0 );
		tilemap_set_transparent_pen( tilemap_1,  0x0f );
	
		return 0;
	} };
	
	public static VhStartPtr clshroad_vh_start = new VhStartPtr() { public int handler() 
	{
		/* These 2 use the graphics and scroll value */
		tilemap_0a = tilemap_create(get_tile_info_0a,tilemap_scan_rows,TILEMAP_OPAQUE,     16,16,0x20,0x10);
		tilemap_0b = tilemap_create(get_tile_info_0b,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,0x20,0x10);
		/* Text (No scrolling) */
		tilemap_1  = tilemap_create(get_tile_info_1,tilemap_scan_rows_extra,TILEMAP_TRANSPARENT,8,8,0x24,0x20);
	
		if (tilemap_0a==null || tilemap_0b==null || tilemap_1==null)
			return 1;
	
		tilemap_set_scroll_rows( tilemap_0a, 1);
		tilemap_set_scroll_rows( tilemap_0b, 1);
		tilemap_set_scroll_rows( tilemap_1,  1);
	
		tilemap_set_scroll_cols( tilemap_0a, 1);
		tilemap_set_scroll_cols( tilemap_0b, 1);
		tilemap_set_scroll_cols( tilemap_1,  1);
	
		tilemap_set_scrolldx( tilemap_0a, -0x30, -0xb5);
		tilemap_set_scrolldx( tilemap_0b, -0x30, -0xb5);
	
		tilemap_set_transparent_pen( tilemap_0a, 0 );
		tilemap_set_transparent_pen( tilemap_0b, 0 );
		tilemap_set_transparent_pen( tilemap_1,  0 );
	
		return 0;
	} };
	
	
	/***************************************************************************
	
									Sprites Drawing
	
	Offset:		Format:		Value:
	
		0
	
		1					Y (Bottom-up)
	
		2		765432--
				------10	Code (high bits)
	
		3		76------
				--543210	Code (low bits)
	
		4
	
		5					X (low bits)
	
		6					X (High bits)
	
		7					Color?
	
	- Sprite flipping ?
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int i;
	
		for (i = 0; i < spriteram_size[0] ; i += 8)
		{
			int y		=	 240 - spriteram.read(i+1);
			int code	=	(spriteram.read(i+3)& 0x3f) + (spriteram.read(i+2)<< 6);
			int x		=	 spriteram.read(i+5)+ (spriteram.read(i+6)<< 8);
			int attr	=	 spriteram.read(i+7);
	
			int flipx	=	0;
			int flipy	=	0;
	
			x -= 0x4a/2;
			if (flip_screen() != 0)
			{
				y = 240 - y;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					code,
					attr & 0x0f,
					flipx,flipy,
					x,y,
					Machine.visible_area,TRANSPARENCY_PEN,0 );
		}
	}
	
	
	/***************************************************************************
	
	
									Screen Drawing
	
	
	***************************************************************************/
	
	public static VhUpdatePtr clshroad_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int scrollx  = clshroad_vregs.read( 0 ) + (clshroad_vregs.read( 1 ) << 8);
	//	int priority = clshroad_vregs[ 2 ];
	
		/* Only horizontal scrolling (these 2 layers use the same value) */
		tilemap_set_scrollx(tilemap_0a, 0, scrollx);
		tilemap_set_scrollx(tilemap_0b, 0, scrollx);
	
		tilemap_draw(bitmap,tilemap_0a,0,0);	// Opaque
		tilemap_draw(bitmap,tilemap_0b,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,tilemap_1,0,0);
	} };
}
