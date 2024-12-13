/***************************************************************************

							-= American Speedway =-

					driver by	Luca Elia (l.elia@tin.it)


- 8x8 4 Color Tiles (with 8 palettes) used for both:

	- 1 256x256 non scrolling layer
	- 64 (32?) Sprites

***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
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

public class amspdwy
{
	
	/* Variables only used here: */
	
	static struct_tilemap tilemap;
	
	
	public static WriteHandlerPtr amspdwy_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		data ^= 0xff;
		paletteram_BBGGGRRR_w.handler(offset,data);
	//	paletteram_RRRGGGBB_w(offset,data);
	} };
        
        static int flip = 0;
	
	public static WriteHandlerPtr amspdwy_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip ^= 1;
		flip_screen_set( flip );
	} };
	
	/***************************************************************************
	
							Callbacks for the TileMap code
	
								  [ Tiles Format ]
	
		Videoram:	76543210	Code Low Bits
		Colorram:	765-----
					---43---	Code High Bits
					-----210	Color
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		int code	=	videoram.read( tile_index );
		int color	=	colorram.read( tile_index );
		SET_TILE_INFO(
				0,
				code + ((color & 0x18)<<5),
				color & 0x07,
				0);
            }
        };
	
	public static WriteHandlerPtr amspdwy_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (videoram.read(offset)!= data)
		{
			videoram.write(offset,data);
			tilemap_mark_tile_dirty(tilemap, offset);
		}
	} };
	
	public static WriteHandlerPtr amspdwy_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (colorram.read(offset)!= data)
		{
			colorram.write(offset,data);
			tilemap_mark_tile_dirty(tilemap, offset);
		}
	} };
	
	
	/* logical (col,row) . memory offset */
	public static GetMemoryOffsetPtr tilemap_scan_cols_back = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
            
		return col*num_rows + (num_rows - row - 1);
            }
        };
	
	
	public static VhStartPtr amspdwy_vh_start = new VhStartPtr() { public int handler() 
	{
		tilemap	= tilemap_create(get_tile_info,	tilemap_scan_cols_back, TILEMAP_OPAQUE,	8,8,	0x20, 0x20 );
	
		if (tilemap != null)	return 0;
		else			return 1;
	} };
	
	
	
	/***************************************************************************
	
									Sprites Drawing
	
	Offset:		Format:		Value:
	
	0						Y
	1						X
	2						Code Low Bits
	3			7-------	Flip X
				-6------	Flip Y
				--5-----
				---4----	?
				----3---	Code High Bit?
				-----210	Color
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int i;
		int max_x = Machine.drv.screen_width  - 1;
		int max_y = Machine.drv.screen_height - 1;
	
		for (i = 0; i < spriteram_size[0] ; i += 4)
		{
			int y		=	spriteram.read(i+0);
			int x		=	spriteram.read(i+1);
			int code	=	spriteram.read(i+2);
			int attr	=	spriteram.read(i+3);
			int flipx	=	attr & 0x80;
			int flipy	=	attr & 0x40;
	
			if (flip_screen() != 0)
			{
				x = max_x - x - 8;	y = max_y - y - 8;
				flipx = flipx!=0?0:1;	flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
	//				code + ((attr & 0x18)<<5),
					code + ((attr & 0x08)<<5),
					attr,
					flipx, flipy,
					x,y,
					Machine.visible_area,TRANSPARENCY_PEN,0 );
		}
	}
	
	
	
	
	/***************************************************************************
	
									Screen Drawing
	
	***************************************************************************/
	
	public static VhUpdatePtr amspdwy_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,tilemap,0,0);
		draw_sprites(bitmap);
	} };
}
