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

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class wc90
{
	
	
	public static UBytePtr wc90_fgvideoram=new UBytePtr(), wc90_bgvideoram=new UBytePtr(), wc90_txvideoram=new UBytePtr();
	
	
	public static UBytePtr wc90_scroll0xlo=new UBytePtr(), wc90_scroll0xhi=new UBytePtr();
	public static UBytePtr wc90_scroll1xlo=new UBytePtr(), wc90_scroll1xhi=new UBytePtr();
	public static UBytePtr wc90_scroll2xlo=new UBytePtr(), wc90_scroll2xhi=new UBytePtr();
	
	public static UBytePtr wc90_scroll0ylo=new UBytePtr(), wc90_scroll0yhi=new UBytePtr();
	public static UBytePtr wc90_scroll1ylo=new UBytePtr(), wc90_scroll1yhi=new UBytePtr();
	public static UBytePtr wc90_scroll2ylo=new UBytePtr(), wc90_scroll2yhi=new UBytePtr();
	
	
	static struct_tilemap tx_tilemap,fg_tilemap,bg_tilemap;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		int attr = wc90_bgvideoram.read(tile_index);
		int tile = wc90_bgvideoram.read(tile_index + 0x800) +
						256 * ((attr & 3) + ((attr >> 1) & 4));
		SET_TILE_INFO(
				2,
				tile,
				attr >> 4,
				0);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = wc90_fgvideoram.read(tile_index);
		int tile = wc90_fgvideoram.read(tile_index + 0x800) +
						256 * ((attr & 3) + ((attr >> 1) & 4));
		SET_TILE_INFO(
				1,
				tile,
				attr >> 4,
				0);
            }
        };
	
	static GetTileInfoPtr get_tx_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		SET_TILE_INFO(
				0,
				wc90_txvideoram.read(tile_index + 0x800) + ((wc90_txvideoram.read(tile_index) & 0x07) << 8),
				wc90_txvideoram.read(tile_index) >> 4,
				0);
            }
        };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr wc90_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,     16,16,64,32);
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,64,32);
		tx_tilemap = tilemap_create(get_tx_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,64,32);
	
		if ((bg_tilemap==null) || (fg_tilemap==null) || (tx_tilemap==null))
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
		tilemap_set_transparent_pen(tx_tilemap,0);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr wc90_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (wc90_bgvideoram.read(offset) != data)
		{
			wc90_bgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset & 0x7ff);
		}
	} };
	
	public static WriteHandlerPtr wc90_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (wc90_fgvideoram.read(offset) != data)
		{
			wc90_fgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset & 0x7ff);
		}
	} };
	
	public static WriteHandlerPtr wc90_txvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (wc90_txvideoram.read(offset) != data)
		{
			wc90_txvideoram.write(offset, data);
			tilemap_mark_tile_dirty(tx_tilemap,offset & 0x7ff);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static void WC90_DRAW_SPRITE( mame_bitmap bitmap, int flags, int bank, int code, int sx, int sy ){ 
            drawgfx( bitmap, Machine.gfx[3], code, flags >> 4, 
            bank&1, bank&2, sx, sy, Machine.visible_area, TRANSPARENCY_PEN, 0 );
        }
	
	static char pos32x32[] = { 0, 1, 2, 3 };
	static char pos32x32x[] = { 1, 0, 3, 2 };
	static char pos32x32y[] = { 2, 3, 0, 1 };
	static char pos32x32xy[] = { 3, 2, 1, 0 };
	
	static char pos32x64[] = { 0, 1, 2, 3, 4, 5, 6, 7 };
	static char pos32x64x[] = { 5, 4, 7, 6, 1, 0, 3, 2 };
	static char pos32x64y[] = { 2, 3, 0, 1,	6, 7, 4, 5 };
	static char pos32x64xy[] = { 7, 6, 5, 4, 3, 2, 1, 0 };
	
	static char pos64x32[] = { 0, 1, 2, 3, 4, 5, 6, 7 };
	static char pos64x32x[] = { 1, 0, 3, 2, 5, 4, 7, 6 };
	static char pos64x32y[] = { 6, 7, 4, 5, 2, 3, 0, 1 };
	static char pos64x32xy[] = { 7, 6, 5, 4, 3, 2, 1, 0 };
	
	static char pos64x64[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	static char pos64x64x[] = { 5, 4, 7, 6, 1, 0, 3, 2, 13, 12, 15, 14, 9, 8, 11, 10 };
	static char pos64x64y[] = { 10, 11, 8, 9, 14, 15, 12, 13, 2, 3, 0, 1, 6, 7,	4, 5 };
	static char pos64x64xy[] = { 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
	
	static char p32x32[][] = {
		pos32x32,
		pos32x32x,
		pos32x32y,
		pos32x32xy
	};
	
	static char p32x64[][] = {
		pos32x64,
		pos32x64x,
		pos32x64y,
		pos32x64xy
	};
	
	static char p64x32[][] = {
		pos64x32,
		pos64x32x,
		pos64x32y,
		pos64x32xy
	};
	
	static char p64x64[][] = {
		pos64x64,
		pos64x64x,
		pos64x64y,
		pos64x64xy
	};
        
        public static abstract interface drawsprites_procdef {
            public abstract void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags);
        };
	
	static drawsprites_procdef drawsprite_16x16 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
                WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
            }
        };
            
	static drawsprites_procdef drawsprite_16x32 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
		if (( bank & 2 ) != 0) {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx, sy+16 );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
		} else {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx, sy+16 );
		}
            }
        };
	
	static drawsprites_procdef drawsprite_16x64 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
		if (( bank & 2 ) != 0) {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+3, sx, sy+48 );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+2, sx, sy+32 );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx, sy+16 );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
		} else {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx, sy+16 );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+2, sx, sy+32 );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+3, sx, sy+48 );
		}
            }
        };
	
	static drawsprites_procdef drawsprite_32x16 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
		if (( bank & 1 ) != 0) {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx+16, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
		} else {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx+16, sy );
		}
            }
        };
	
	static drawsprites_procdef drawsprite_32x32 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
	
		char[] p = p32x32[ bank&3 ];
	
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[0], sx, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[1], sx+16, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[2], sx, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[3], sx+16, sy+16 );
            }
        };
	
	static drawsprites_procdef drawsprite_32x64 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
	
		char[] p = p32x64[ bank&3 ];
	
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[0], sx, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[1], sx+16, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[2], sx, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[3], sx+16, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[4], sx, sy+32 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[5], sx+16, sy+32 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[6], sx, sy+48 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[7], sx+16, sy+48 );
            }
        };
	
	static drawsprites_procdef drawsprite_64x16 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
		if (( bank & 1 ) != 0) {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+3, sx+48, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+2, sx+32, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx+16, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
		} else {
			WC90_DRAW_SPRITE( bitmap, flags, bank, code, sx, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+1, sx+16, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+2, sx+32, sy );
			WC90_DRAW_SPRITE( bitmap, flags, bank, code+3, sx+48, sy );
		}
            }
        };
	
	static drawsprites_procdef drawsprite_64x32 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
	
		char[] p = p64x32[ bank&3 ];
	
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[0], sx, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[1], sx+16, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[2], sx, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[3], sx+16, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[4], sx+32, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[5], sx+48, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[6], sx+32, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[7], sx+48, sy+16 );
            }
        };
	
	static drawsprites_procdef drawsprite_64x64 = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
	
		char[] p = p64x64[ bank&3 ];
	
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[0], sx, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[1], sx+16, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[2], sx, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[3], sx+16, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[4], sx+32, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[5], sx+48, sy );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[6], sx+32, sy+16 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[7], sx+48, sy+16 );
	
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[8], sx, sy+32 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[9], sx+16, sy+32 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[10], sx, sy+48 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[11], sx+16, sy+48 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[12], sx+32, sy+32 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[13], sx+48, sy+32 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[14], sx+32, sy+48 );
		WC90_DRAW_SPRITE( bitmap, flags, bank, code+p[15], sx+48, sy+48 );
            }
        };
	
	static drawsprites_procdef drawsprite_invalid = new drawsprites_procdef() {
            public void handler(mame_bitmap bitmap, int code, int sx, int sy, int bank, int flags) {
                logerror("8 pixel sprite size not supportedn" );
            }
        };
		
        
	
	static drawsprites_procdef drawsprites_proc[] = {
		drawsprite_invalid,		/* 0000 = 08x08 */
		drawsprite_invalid,		/* 0001 = 16x08 */
		drawsprite_invalid,		/* 0010 = 32x08 */
		drawsprite_invalid,		/* 0011 = 64x08 */
		drawsprite_invalid,		/* 0100 = 08x16 */
		drawsprite_16x16,		/* 0101 = 16x16 */
		drawsprite_32x16,		/* 0110 = 32x16 */
		drawsprite_64x16,		/* 0111 = 64x16 */
		drawsprite_invalid,		/* 1000 = 08x32 */
		drawsprite_16x32,		/* 1001 = 16x32 */
		drawsprite_32x32,		/* 1010 = 32x32 */
		drawsprite_64x32,		/* 1011 = 64x32 */
		drawsprite_invalid,		/* 1100 = 08x64 */
		drawsprite_16x64,		/* 1101 = 16x64 */
		drawsprite_32x64,		/* 1110 = 32x64 */
		drawsprite_64x64		/* 1111 = 64x64 */
	};
	
	static void draw_sprites( mame_bitmap bitmap, int priority )
	{
		int offs, sx,sy, flags, which;
	
		/* draw all visible sprites of specified priority */
		for (offs = 0;offs < spriteram_size[0];offs += 16){
			int bank = spriteram.read(offs+0);
	
			if ( ( bank >> 4 ) == priority ) {
	
				if (( bank & 4 ) != 0) { /* visible */
					which = ( spriteram.read(offs+2)>> 2 ) + ( spriteram.read(offs+3)<< 6 );
	
					sx = spriteram.read(offs + 8)+ ( (spriteram.read(offs + 9)& 1 ) << 8 );
					sy = spriteram.read(offs + 6)+ ( (spriteram.read(offs + 7)& 1 ) << 8 );
	
					flags = spriteram.read(offs+4);
					drawsprites_proc[ flags & 0x0f ].handler(bitmap, which, sx, sy, bank, flags );
				}
			}
		}
	}
	
/*TODO*///	#undef WC90_DRAW_SPRITE
	
	
	public static VhUpdatePtr wc90_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_set_scrollx(bg_tilemap,0,wc90_scroll2xlo.read(0) + 256 * wc90_scroll2xhi.read(0));
		tilemap_set_scrolly(bg_tilemap,0,wc90_scroll2ylo.read(0) + 256 * wc90_scroll2yhi.read(0));
		tilemap_set_scrollx(fg_tilemap,0,wc90_scroll1xlo.read(0) + 256 * wc90_scroll1xhi.read(0));
		tilemap_set_scrolly(fg_tilemap,0,wc90_scroll1ylo.read(0) + 256 * wc90_scroll1yhi.read(0));
		tilemap_set_scrollx(tx_tilemap,0,wc90_scroll0xlo.read(0) + 256 * wc90_scroll0xhi.read(0));
		tilemap_set_scrolly(tx_tilemap,0,wc90_scroll0ylo.read(0) + 256 * wc90_scroll0yhi.read(0));
	
	//	draw_sprites( bitmap, 3 );
		tilemap_draw(bitmap,bg_tilemap,0,0);
		draw_sprites( bitmap, 2 );
		tilemap_draw(bitmap,fg_tilemap,0,0);
		draw_sprites( bitmap, 1 );
		tilemap_draw(bitmap,tx_tilemap,0,0);
		draw_sprites( bitmap, 0 );
	} };
}
