/***************************************************************************

Functions to emulate the video hardware of the machine.

This hardware of the machine is similar with that of Mutant Night.
The difference between the two machines is that there are three BG
layers as against just one BG layer in Mutant Night.


Foreground RAM format ( Foreground RAM format is same as Mutant Night )
--------------------------------------------------------------
 +0         +1
 xxxx xxxx  ---- ----		= bottom 8 bits of tile number
 ---- ----  xx-- ----		= top 2 bits of tile number
 ---- ----  --x- ----		= flip X
 ---- ----  ---x ----		= flip Y
 ---- ----  ---- xxxx		= color ( 00h - 0fh )


Background RAM format
--------------------------------------------------------------
 +0         +1
 xxxx xxxx  ---- ----		= bottom 8 bits of tile number
 ---- ----  x--- ----		= bit 9 of tile number
 ---- ----  -x-- ----		= bit 8 of tile number
 ---- ----  --x- ----		= bit 10 of tile number
 ---- ----  ---x ----		= bit 11 of tile number (the most top bit)
 ---- ----  ---- xxxx		= color ( 00h - 1fh )


Sprite RAM format	( Sprite format is same as Mutant Night )
--------------------------------------------------------------
 +0         +1         +2         +3         +4
 xxxx xxxx  ---- ----  ---- ----  ---- ----  ---- ----	= sprite Y position
 ---- ----  xxxx xxxx  ---- ----  ---- ----  ---- ----  = bottom 8 bits of sprite X position
 ---- ----  ---- ----  xx-- ----  ---- ----  ---- ----	= middle 2 bits of sprite number
 ---- ----  ---- ----  --x- ----  ---- ----  ---- ----	= flip X
 ---- ----  ---- ----  ---x ----  ---- ----  ---- ----  = flip Y
 ---- ----  ---- ----  ---- x---  ---- ----  ---- ----	= top bit of sprite number
 ---- ----  ---- ----  ---- -x--  ---- ----  ---- ----	= 0:normal size (16x16)  1:big size (32x32)
 ---- ----  ---- ----  ---- --x-  ---- ----  ---- ----	= sprite on / off
 ---- ----  ---- ----  ---- ---x  ---- ----  ---- ----	= top bit of sprite X position
 ---- ----  ---- ----  ---- ----  xxxx xxxx  ---- ----  = bottom 8 bits of sprite number
 ---- ----  ---- ----  ---- ----  ---- ----  xxxx xxxx	= color


Scroll RAM format (Omega Fighter)
--------------------------------------------------------------
    +0         +1
 X  ???? -xxx  xxxx xxxx		= scroll X (0 - 0x3ff)
 Y  ???? ---x  xxxx xxxx        = scroll Y (0 - 0x1ff)

Scroll RAM format (Atomic Robokid)
--------------------------------------------------------------
    +0         +1
 X  ???? ---x  xxxx xxxx		= scroll X (0 - 0x1ff)
 Y  ???? ---x  xxxx xxxx        = scroll Y (0 - 0x1ff)


Real screen resolution and virtual one
--------------------------------------------------------------
                Real        Virtual
Omega Fighter   256x192(H)  2048x512
Atomic Robokid  256x192(H)  512x512


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


public class omegaf
{
	
	
	
	/**************************************************************************
	  Variables
	**************************************************************************/
	
	public static UBytePtr omegaf_fg_videoram=new UBytePtr();
	public static UBytePtr omegaf_bg0_videoram=new UBytePtr();
	public static UBytePtr omegaf_bg1_videoram=new UBytePtr();
	public static UBytePtr omegaf_bg2_videoram=new UBytePtr();
	public static int[] omegaf_fgvideoram_size=new int[2];
	
	static int omegaf_bg0_bank = 0;
	static int omegaf_bg1_bank = 0;
	static int omegaf_bg2_bank = 0;
	
	public static UBytePtr omegaf_bg0_scroll_x=new UBytePtr();
	public static UBytePtr omegaf_bg1_scroll_x=new UBytePtr();
	public static UBytePtr omegaf_bg2_scroll_x=new UBytePtr();
	public static UBytePtr omegaf_bg0_scroll_y=new UBytePtr();
	public static UBytePtr omegaf_bg1_scroll_y=new UBytePtr();
	public static UBytePtr omegaf_bg2_scroll_y=new UBytePtr();
	
	static struct_tilemap fg_tilemap;
	static struct_tilemap bg0_tilemap;
	static struct_tilemap bg1_tilemap;
	static struct_tilemap bg2_tilemap;
	
	static int bg0_enabled = 1;
	static int bg1_enabled = 1;
	static int bg2_enabled = 1;
	
	static mame_bitmap bitmap_sp;	/* for sprite overdraw */
	static int sprite_overdraw_enabled = 0;
	
	static int scrollx_mask = 0x07ff;
	static int bank_mask = 1;
	
	
	/***************************************************************************
	  Callbacks for the tilemap code
	***************************************************************************/
	
	static GetTileInfoPtr get_bg0_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
            
		int color, tile, hi, lo;
	
		int attrib = ( (tile_index & 0x00f) | ( (tile_index & 0x070) << 5 ) |
		                                      ( (tile_index & 0xf80) >> 3 ) ) << 1;
		lo  = omegaf_bg0_videoram.read( attrib );
		hi  = omegaf_bg0_videoram.read( attrib | 1 );
		color = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				0,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	static GetTileInfoPtr get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int color, tile, hi, lo;
	
		int attrib = ( (tile_index & 0x00f) | ( (tile_index & 0x070) << 5 ) |
		                                      ( (tile_index & 0xf80) >> 3 ) ) << 1;
		lo  = omegaf_bg1_videoram.read( attrib );
		hi  = omegaf_bg1_videoram.read( attrib | 1 );
		color = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				1,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	static GetTileInfoPtr get_bg2_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int color, tile, hi, lo;
	
		int attrib = ( (tile_index & 0x00f) | ( (tile_index & 0x070) << 5 ) |
		                                      ( (tile_index & 0xf80) >> 3 ) ) << 1;
		lo  = omegaf_bg2_videoram.read( attrib );
		hi  = omegaf_bg2_videoram.read( attrib | 1 );
		color = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				2,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	static GetTileInfoPtr robokid_get_bg0_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int color, tile, hi, lo;
	
		int attrib = ( (tile_index & 0x00f) | ( (tile_index & 0x010) << 5 ) |
		                                      ( (tile_index & 0x3e0) >> 1 ) ) << 1;
		lo  = omegaf_bg0_videoram.read( attrib );
		hi  = omegaf_bg0_videoram.read( attrib | 1 );
		color = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				0,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	static GetTileInfoPtr robokid_get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int color, tile, hi, lo;
	
		int attrib = ( (tile_index & 0x00f) | ( (tile_index & 0x010) << 5 ) |
		                                      ( (tile_index & 0x3e0) >> 1 ) ) << 1;
		lo  = omegaf_bg1_videoram.read( attrib );
		hi  = omegaf_bg1_videoram.read( attrib | 1 );
		color = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				1,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	static GetTileInfoPtr robokid_get_bg2_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int color, tile, hi, lo;
	
		int attrib = ( (tile_index & 0x00f) | ( (tile_index & 0x010) << 5 ) |
		                                      ( (tile_index & 0x3e0) >> 1 ) ) << 1;
		lo  = omegaf_bg2_videoram.read( attrib );
		hi  = omegaf_bg2_videoram.read( attrib | 1 );
		color = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				2,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int color, tile, hi, lo;
	
		lo  = omegaf_fg_videoram.read( tile_index << 1 );
		hi = omegaf_fg_videoram.read( (tile_index << 1) + 1 );
		color  = hi & 0x0f;
		tile = ( ((hi & 0x80) << 2) | ((hi & 0x40) << 2) |
		         ((hi & 0x20) << 5) | ((hi & 0x10) << 7) ) | lo;
		SET_TILE_INFO(
				5,
				tile,
				color,
				0);
		tile_info.priority = 0;
            }
        };
	
	/***************************************************************************
	  Initialize and destroy video hardware emulation
	***************************************************************************/
	
	static int videoram_alloc(int size)
	{
		/* create video ram */
		if ( (omegaf_bg0_videoram = new UBytePtr(size)) == null )
		{
			return 1;
		}
		memset( omegaf_bg0_videoram, 0x00, size );
	
		if ( (omegaf_bg1_videoram = new UBytePtr(size)) == null )
		{
			omegaf_bg0_videoram = null;
			return 1;
		}
		memset( omegaf_bg1_videoram, 0x00, size );
	
		if ( (omegaf_bg2_videoram = new UBytePtr(size)) == null )
		{
			omegaf_bg0_videoram = null;
			omegaf_bg1_videoram = null;
			return 1;
		}
		memset( omegaf_bg2_videoram, 0x00, size );
	
		if ( (bitmap_sp =
		      bitmap_alloc (Machine . drv . screen_width, Machine . drv . screen_height
		   ) ) == null )
		{
			omegaf_bg0_videoram = null;
			omegaf_bg1_videoram = null;
			omegaf_bg2_videoram = null;
			return 1;
		}
	
		return 0;
	}
	
	public static VhStartPtr omegaf_vh_start = new VhStartPtr() { public int handler() 
	{
		scrollx_mask = 0x07ff;
		bank_mask = 7;
	
		if ( videoram_alloc(0x2000) != 0 )
			return 1;
	
		/*                           Info               Offset             Type                 w   h  col  row */
		fg_tilemap  = tilemap_create(get_fg_tile_info,  tilemap_scan_rows, TILEMAP_TRANSPARENT, 8,  8,  32, 32);
		bg0_tilemap = tilemap_create(get_bg0_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 16, 16, 128, 32);
		bg1_tilemap = tilemap_create(get_bg1_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 16, 16, 128, 32);
		bg2_tilemap = tilemap_create(get_bg2_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 16, 16, 128, 32);
	
		if ( fg_tilemap==null || bg0_tilemap==null || bg1_tilemap==null || bg2_tilemap==null )
			return 1;
	
		tilemap_set_transparent_pen( fg_tilemap,  15 );
		tilemap_set_transparent_pen( bg0_tilemap, 15 );
		tilemap_set_transparent_pen( bg1_tilemap, 15 );
		tilemap_set_transparent_pen( bg2_tilemap, 15 );
	
		return 0;
	} };
	
	public static VhStartPtr robokid_vh_start = new VhStartPtr() { public int handler() 
	{
		scrollx_mask = 0x01ff;
		bank_mask = 1;
	
		if ( videoram_alloc(0x0800) != 0 )
			return 1;
	
		/*                           Info               Offset             Type                         w   h  col  row */
		fg_tilemap  = tilemap_create(        get_fg_tile_info,  tilemap_scan_rows, TILEMAP_TRANSPARENT, 8,  8,  32, 32);
		bg0_tilemap = tilemap_create(robokid_get_bg0_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 16, 16, 32, 32);
		bg1_tilemap = tilemap_create(robokid_get_bg1_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 16, 16, 32, 32);
		bg2_tilemap = tilemap_create(robokid_get_bg2_tile_info, tilemap_scan_rows, TILEMAP_TRANSPARENT, 16, 16, 32, 32);
	
		if ( fg_tilemap==null || bg0_tilemap==null || bg1_tilemap==null || bg2_tilemap==null )
			return 1;
	
		tilemap_set_transparent_pen( fg_tilemap,  15 );
		tilemap_set_transparent_pen( bg0_tilemap, 15 );
		tilemap_set_transparent_pen( bg1_tilemap, 15 );
		tilemap_set_transparent_pen( bg2_tilemap, 15 );
	
		return 0;
	} };
	
	public static VhStopPtr omegaf_vh_stop = new VhStopPtr() { public void handler() 
	{
		omegaf_bg0_videoram = null;
		omegaf_bg1_videoram = null;
		omegaf_bg2_videoram = null;
		bitmap_free(bitmap_sp);
	} };
	
	
	/***************************************************************************
	  Memory handler
	***************************************************************************/
	
	public static WriteHandlerPtr omegaf_bg0_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		omegaf_bg0_bank = data & bank_mask;
	} };
	
	public static WriteHandlerPtr omegaf_bg1_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		omegaf_bg1_bank = data & bank_mask;
	} };
	
	public static WriteHandlerPtr omegaf_bg2_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		omegaf_bg2_bank = data & bank_mask;
	} };
	
	public static ReadHandlerPtr omegaf_bg0_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return omegaf_bg0_videoram.read( (omegaf_bg0_bank << 10) | offset );
	} };
	
	public static ReadHandlerPtr omegaf_bg1_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return omegaf_bg1_videoram.read( (omegaf_bg1_bank << 10) | offset );
	} };
	
	public static ReadHandlerPtr omegaf_bg2_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return omegaf_bg2_videoram.read( (omegaf_bg2_bank << 10) | offset );
	} };
	
	public static WriteHandlerPtr omegaf_bg0_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int address;
		int tile_index;
	
		address = (omegaf_bg0_bank << 10 ) | offset;
		if ( omegaf_bg0_videoram.read(address) != data )
		{
			omegaf_bg0_videoram.write( address, data );
			tile_index = ( (address & 0x001e) >> 1 ) | ( (address & 0x1c00) >> 6 ) |
			             ( (address & 0x03e0) << 2 );
			tilemap_mark_tile_dirty( bg0_tilemap, tile_index );
		}
	} };
	
	public static WriteHandlerPtr omegaf_bg1_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int address;
		int tile_index;
	
		address = (omegaf_bg1_bank << 10 ) | offset;
		if ( omegaf_bg1_videoram.read(address) != data )
		{
			omegaf_bg1_videoram.write( address, data );
			tile_index = ( (address & 0x001e) >> 1 ) | ( (address & 0x1c00) >> 6 ) |
			             ( (address & 0x03e0) << 2 );
			tilemap_mark_tile_dirty( bg1_tilemap, tile_index );
		}
	} };
	
	public static WriteHandlerPtr omegaf_bg2_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int address;
		int tile_index;
	
		address = (omegaf_bg2_bank << 10 ) | offset;
		if ( omegaf_bg2_videoram.read(address) != data )
		{
			omegaf_bg2_videoram.write( address, data );
			tile_index = ( (address & 0x001e) >> 1 ) | ( (address & 0x1c00) >> 6 ) |
			             ( (address & 0x03e0) << 2 );
			tilemap_mark_tile_dirty( bg2_tilemap, tile_index );
		}
	} };
	
	public static WriteHandlerPtr robokid_bg0_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int address;
		int tile_index;
	
		address = (omegaf_bg0_bank << 10 ) | offset;
		if ( omegaf_bg0_videoram.read(address) != data )
		{
			omegaf_bg0_videoram.write( address, data);
			tile_index = ( (address & 0x001e) >> 1 ) | ( (address & 0x0400) >> 6 ) |
			               (address & 0x03e0);
			tilemap_mark_tile_dirty( bg0_tilemap, tile_index );
		}
	} };
	
	public static WriteHandlerPtr robokid_bg1_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int address;
		int tile_index;
	
		address = (omegaf_bg1_bank << 10 ) | offset;
		if ( omegaf_bg1_videoram.read(address) != data )
		{
			omegaf_bg1_videoram.write( address, data );
			tile_index = ( (address & 0x001e) >> 1 ) | ( (address & 0x0400) >> 6 ) |
			               (address & 0x03e0);
			tilemap_mark_tile_dirty( bg1_tilemap, tile_index );
		}
	} };
	
	public static WriteHandlerPtr robokid_bg2_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int address;
		int tile_index;
	
		address = (omegaf_bg2_bank << 10 ) | offset;
		if ( omegaf_bg2_videoram.read(address) != data )
		{
			omegaf_bg2_videoram.write( address, data );
			tile_index = ( (address & 0x001e) >> 1 ) | ( (address & 0x0400) >> 6 ) |
			               (address & 0x03e0);
			tilemap_mark_tile_dirty( bg2_tilemap, tile_index );
		}
	} };
	
	public static WriteHandlerPtr omegaf_bg0_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrollx;
	
		omegaf_bg0_scroll_x.write(offset, data);
	
		scrollx = (omegaf_bg0_scroll_x.read(1) << 8) | omegaf_bg0_scroll_x.read(0);
		scrollx &= scrollx_mask;
		tilemap_set_scrollx( bg0_tilemap, 0, scrollx );
	} };
	
	public static WriteHandlerPtr omegaf_bg0_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrolly;
	
		omegaf_bg0_scroll_y.write(offset, data);
	
		scrolly = (omegaf_bg0_scroll_y.read(1) << 8) | omegaf_bg0_scroll_y.read(0);
		scrolly &= 0x01ff;
		tilemap_set_scrolly( bg0_tilemap, 0, scrolly );
	} };
	
	public static WriteHandlerPtr omegaf_bg1_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrollx;
	
		omegaf_bg1_scroll_x.write(offset, data);
	
		scrollx = (omegaf_bg1_scroll_x.read(1) << 8) | omegaf_bg1_scroll_x.read(0);
		scrollx &= scrollx_mask;
		tilemap_set_scrollx( bg1_tilemap, 0, scrollx );
	} };
	
	public static WriteHandlerPtr omegaf_bg1_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrolly;
	
		omegaf_bg1_scroll_y.write(offset, data);
	
		scrolly = (omegaf_bg1_scroll_y.read(1) << 8) | omegaf_bg1_scroll_y.read(0);
		scrolly &= 0x01ff;
		tilemap_set_scrolly( bg1_tilemap, 0, scrolly );
	} };
	
	public static WriteHandlerPtr omegaf_bg2_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrollx;
	
		omegaf_bg2_scroll_x.write(offset, data);
	
		scrollx = (omegaf_bg2_scroll_x.read(1) << 8) | omegaf_bg2_scroll_x.read(0);
		scrollx &= scrollx_mask;
		tilemap_set_scrollx( bg2_tilemap, 0, scrollx );
	} };
	
	public static WriteHandlerPtr omegaf_bg2_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrolly;
	
		omegaf_bg2_scroll_y.write(offset, data);
	
		scrolly = (omegaf_bg2_scroll_y.read(1) << 8) | omegaf_bg2_scroll_y.read(0);
		scrolly &= 0x01ff;
		tilemap_set_scrolly( bg2_tilemap, 0, scrolly );
	} };
	
	public static WriteHandlerPtr omegaf_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (omegaf_fg_videoram.read(offset) != data)
		{
			omegaf_fg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap, offset >> 1);
		}
	} };
	
	public static WriteHandlerPtr omegaf_bg0_enabled_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (bg0_enabled != data)
			bg0_enabled = data;
	} };
	
	public static WriteHandlerPtr omegaf_bg1_enabled_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (bg1_enabled != data)
			bg1_enabled = data;
	} };
	
	public static WriteHandlerPtr omegaf_bg2_enabled_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (bg2_enabled != data)
			bg2_enabled = data;
	} };
	
	public static WriteHandlerPtr omegaf_sprite_overdraw_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///		logerror( "sprite overdraw flag : %02x\n", data );
		if (sprite_overdraw_enabled != (data & 1))
		{
			sprite_overdraw_enabled = data & 1;
			fillbitmap(bitmap_sp, 15, Machine . visible_area);
		}
	} };
	
	
	/***************************************************************************
	  Screen refresh
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		/* Draw the sprites */
		for (offs = 11 ;offs < spriteram_size[0]; offs += 16)
		{
			int sx, sy, tile, color, flipx, flipy, big;
	
			if ((spriteram.read(offs + 2)& 2) != 0)
			{
				sx = spriteram.read(offs + 1);
				sy = spriteram.read(offs);
				if ((spriteram.read(offs + 2)& 1) != 0)
					sx -= 256;
				tile = spriteram.read(offs + 3)|
						((spriteram.read(offs + 2)& 0xc0) << 2) |
						((spriteram.read(offs + 2)& 0x08) << 7);
	
				big  = spriteram.read(offs + 2)& 4;
				if (big != 0)
					tile >>= 2;
				flipx = spriteram.read(offs + 2)& 0x10;
				flipy = spriteram.read(offs + 2)& 0x20;
				color = spriteram.read(offs + 4)& 0x0f;
				drawgfx(bitmap,Machine.gfx[(big!=0) ? 4 : 3],
						tile,
						color,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,
						TRANSPARENCY_PEN, 15);
			}
		}
	}
	
	public static VhUpdatePtr omegaf_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(bitmap,Machine.pens[15],Machine.visible_area);	// ??
	
		if (bg0_enabled!=0)	tilemap_draw(bitmap, bg0_tilemap, 0, 0);
		if (bg1_enabled!=0)	tilemap_draw(bitmap, bg1_tilemap, 0, 0);
		if (bg2_enabled!=0)	tilemap_draw(bitmap, bg2_tilemap, 0, 0);
		if ( sprite_overdraw_enabled != 0)				/* overdraw sprite mode */
		{
			draw_sprites(bitmap_sp);
			copybitmap(bitmap, bitmap_sp, 0, 0, 0, 0,
			           Machine.visible_area, TRANSPARENCY_PEN, 15);
		}
		else										/* normal sprite mode */
			draw_sprites(bitmap);
		tilemap_draw(bitmap, fg_tilemap, 0, 0);
	} };
}
