/***************************************************************************

  Video Hardware for some Technos games:
    Double Dragon, Double Dragon bootleg, Double Dragon II and China Gate

  Two Tile layers.
	Background layer is 512x512 , tiles are 16x16
	Top        layer is 256x256 , tiles are 8x8

  Sprites are 16x16, 16x32, 32x16, or 32x32 (attribute bits set dimension)


BG Tile Layout
  0          1
  ---- -xxx  xxxx xxxx  = Tile number
  --xx x---  ---- ----  = Color
  -x-- ----  ---- ----  = X Flip
  x--- ----  ---- ----  = Y Flip


Top Tile layout.
  0          1
  ---- xxxx  xxxx xxxx  = Tile number
  xxxx ----  ---- ----  = Color (China Gate)
  xxx- ----  ---- ----  = Color (Double Dragon)


Sprite layout.
  0          1          2          3          4
  ---- ----  ---- ----  ---- xxxx  xxxx xxxx  ---- ----  = Sprite number
  ---- ----  ---- ----  -xxx ----  ---- ----  ---- ----  = Color
  xxxx xxxx  ---- ----  ---- ----  ---- ----  ---- ----  = Y position
  ---- ----  ---- ---x  ---- ----  ---- ----  ---- ----  = Y MSb position ???
  ---- ----  ---- ----  ---- ----  ---- ----  xxxx xxxx  = X position
  ---- ----  ---- --x-  ---- ----  ---- ----  ---- ----  = X MSb position ???
  ---- ----  ---- -x--  ---- ----  ---- ----  ---- ----  = Y Flip
  ---- ----  ---- x---  ---- ----  ---- ----  ---- ----  = X Flip
  ---- ----  --xx ----  ---- ----  ---- ----  ---- ----  = Sprite Dimension
  ---- ----  x--- ----  ---- ----  ---- ----  ---- ----  = Visible

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
import static mame056.cpuintrfH.*;
import static mame056.palette.*;
import static mame056.memory.*;
import static mame056.memoryH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.*;
import static mame056.cpuexec.*;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class ddragon
{
	
	
	public static UBytePtr ddragon_bgvideoram=new UBytePtr(), ddragon_fgvideoram=new UBytePtr();
	public static int ddragon_scrollx_hi, ddragon_scrolly_hi;
	public static UBytePtr ddragon_scrollx_lo=new UBytePtr();
	public static UBytePtr ddragon_scrolly_lo=new UBytePtr();
	public static UBytePtr ddragon_spriteram=new UBytePtr();
	public static int technos_video_hw;
	
	static struct_tilemap fg_tilemap, bg_tilemap;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr background_scan = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
            	/* logical (col,row) . memory offset */
		return (col & 0x0f) + ((row & 0x0f) << 4) + ((col & 0x10) << 4) + ((row & 0x10) << 5);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		int attr = ddragon_bgvideoram.read(2*tile_index);
		SET_TILE_INFO(
				2,
				ddragon_bgvideoram.read(2*tile_index+1) + ((attr & 0x07) << 8),
				(attr >> 3) & 0x07,
				TILE_FLIPYX((attr & 0xc0) >> 6));
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = ddragon_fgvideoram.read(2*tile_index);
		SET_TILE_INFO(
				0,
				ddragon_fgvideoram.read(2*tile_index+1) + ((attr & 0x07) << 8),
				attr >> 5,
				0);
            }
        };
	
	static GetTileInfoPtr get_fg_16color_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = ddragon_fgvideoram.read(2*tile_index);
		SET_TILE_INFO(
				0,
				ddragon_fgvideoram.read(2*tile_index+1) + ((attr & 0x0f) << 8),
				attr >> 4,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr ddragon_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,background_scan,  TILEMAP_OPAQUE,     16,16,32,32);
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
	
		if (bg_tilemap==null || fg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	public static VhStartPtr chinagat_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,background_scan,  TILEMAP_OPAQUE,     16,16,32,32);
		fg_tilemap = tilemap_create(get_fg_16color_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
	
		if (bg_tilemap==null || fg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr ddragon_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (ddragon_bgvideoram.read(offset) != data)
		{
			ddragon_bgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset/2);
		}
	} };
	
	public static WriteHandlerPtr ddragon_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (ddragon_fgvideoram.read(offset) != data)
		{
			ddragon_fgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset/2);
		}
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static void DRAW_SPRITE( mame_bitmap bitmap, GfxElement gfx, int which, int color, int flipx, int flipy, rectangle clip, int order, int sx, int sy ) {
            drawgfx( bitmap, gfx, (which+order),color,flipx,flipy,sx,sy, clip,TRANSPARENCY_PEN,0);
        }
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		rectangle clip = new rectangle(Machine.visible_area);
                GfxElement gfx = Machine.gfx[1];
	
		UBytePtr src;
		int i;
	
		if ( technos_video_hw == 1 ) {		/* China Gate Sprite RAM */
			src = new UBytePtr(spriteram);
		} else {
			src = new UBytePtr( ddragon_spriteram, 0x800 );
		}
	
		for( i = 0; i < ( 64 * 5 ); i += 5 ) {
			int attr = src.read(i+1);
			if (( attr & 0x80 ) != 0) { /* visible */
				int sx = 240 - src.read(i+4) + ( ( attr & 2 ) << 7 );
				int sy = 240 - src.read(i+0) + ( ( attr & 1 ) << 8 );
				int size = ( attr & 0x30 ) >> 4;
				int flipx = ( attr & 8 );
				int flipy = ( attr & 4 );
				int dx = -16,dy = -16;
	
				int which;
				int color;
	
				if ( technos_video_hw == 2 )		/* Double Dragon 2 */
				{
					color = ( src.read(i+2) >> 5 );
					which = src.read(i+3) + ( ( src.read(i+2) & 0x1f ) << 8 );
				}
				else
				{
					if ( technos_video_hw == 1 )		/* China Gate */
					{
						if ((sx < -7) && (sx > -16)) sx += 256; /* fix sprite clip */
						if ((sy < -7) && (sy > -16)) sy += 256; /* fix sprite clip */
					}
					color = ( src.read(i+2) >> 4 ) & 0x07;
					which = src.read(i+3) + ( ( src.read(i+2) & 0x0f ) << 8 );
				}
	
				if (flip_screen() != 0)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
					dx = -dx;
					dy = -dy;
				}
	
				switch ( size ) {
					case 0: /* normal */
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 0, sx, sy );
					break;
	
					case 1: /* double y */
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 0, sx, sy + dy );
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 1, sx, sy );
					break;
	
					case 2: /* double x */
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 0, sx + dx, sy );
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 2, sx, sy );
					break;
	
					case 3:
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 0, sx + dx, sy + dy );
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 1, sx + dx, sy );
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 2, sx, sy + dy );
					DRAW_SPRITE( bitmap, gfx, which, color, flipx, flipy, clip, 3, sx, sy );
					break;
				}
			}
		}
	}
	
	
	
	public static VhUpdatePtr ddragon_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int scrollx = ddragon_scrollx_hi + ddragon_scrollx_lo.read();
		int scrolly = ddragon_scrolly_hi + ddragon_scrolly_lo.read();
	
		tilemap_set_scrollx(bg_tilemap,0,scrollx);
		tilemap_set_scrolly(bg_tilemap,0,scrolly);
	
		tilemap_draw(bitmap,bg_tilemap,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,fg_tilemap,0,0);
	} };
}
