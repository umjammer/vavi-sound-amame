/***************************************************************************

							-=  SunA 8 Bit Games =-

					driver by	Luca Elia (l.elia@tin.it)

	These games have only sprites, of a peculiar type:

	there is a region of memory where 4 pages of 32x32 tile codes can
	be written like a tilemap made of 4 pages of 256x256 pixels. Each
	tile uses 2 bytes. Later games may use more pages through RAM
	banking.

	Sprites are rectangular regions of *tiles* fetched from there and
	sent to the screen. Each sprite uses 4 bytes, held within the last
	page of tiles.

	* Note: later games use a more complex format than the following,
	        which is yet to be completely understood.

							[ Sprites Format ]


	Offset:			Bits:				Value:

		0.b								Y (Bottom up)

		1.b			7--- ----			Sprite Size (1 = 2x32 tiles; 0 = 2x2)

					2x2 Sprites:
					-65- ----			Tiles Row (height = 8 tiles)
					---4 ----			Page

					2x32 Sprites:
					-6-- ----			Ignore X (Multisprite)
					--54 ----			Page

					---- 3210			Tiles Column (width = 2 tiles)

		2.b								X

		3.b			7--- ----
					-6-- ----			X (Sign Bit)
					--54 3---
					---- -210			Tiles Bank


						[ Sprite's Tiles Format ]


	Offset: 		Bits:					Value:

		0.b								Code (Low Bits)

		1.b			7--- ----			Flip Y
					-6-- ----			Flip X
					--54 32--			Color
					---- --10			Code (High Bits)


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
import static mame056.inputH.KEYCODE_X;
import static mame056.inputH.keyboard_pressed;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class suna8
{
	
	public static int suna8_text_dim; /* specifies format of text layer */
	
	public static int suna8_rombank, suna8_spritebank, suna8_palettebank;
	public static int suna8_unknown;
	
	/* Functions defined in vidhrdw: */
	
	
	
	
	/***************************************************************************
		For Debug: there's no tilemap, just sprites.
	***************************************************************************/
	
	static struct_tilemap tilemap;
	static int tiles, rombank;
	
	static GetTileInfoPtr get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, attr;
		if (keyboard_pressed(KEYCODE_X) != 0)
		{	UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1), 0x10000 + 0x4000*rombank);
			code = rom.read( 2 * tile_index + 0 );
			attr = rom.read( 2 * tile_index + 1 );	}
		else
		{	code = spriteram.read( 2 * tile_index + 0 );
			attr = spriteram.read( 2 * tile_index + 1 );	}
		SET_TILE_INFO(
				0,
				( (attr & 0x03) << 8 ) + code + tiles*0x400,
				(attr >> 2) & 0xf,
				TILE_FLIPYX( (attr >> 6) & 3 ));
            }
        };
	
	
	public static ReadHandlerPtr suna8_banked_paletteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		offset += suna8_palettebank * 0x200;
		return paletteram.read(offset);
	} };
	
	public static ReadHandlerPtr suna8_banked_spriteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		offset += suna8_spritebank * 0x2000;
		return spriteram.read(offset);
	} };
	
	public static WriteHandlerPtr suna8_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (spriteram.read(offset)!= data)
		{
			spriteram.write(offset,data);
			tilemap_mark_tile_dirty(tilemap,offset/2);
		}
	} };
	
	public static WriteHandlerPtr suna8_banked_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		offset += suna8_spritebank * 0x2000;
		if (spriteram.read(offset)!= data)
		{
			spriteram.write(offset,data);
			tilemap_mark_tile_dirty(tilemap,offset/2);
		}
	} };
	
	/*
		Banked Palette RAM. The data is scrambled
	*/
	public static WriteHandlerPtr brickzn_banked_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r,g,b;
		int rgb;
		offset += suna8_palettebank * 0x200;
		paletteram.write(offset,data);
		rgb = (paletteram.read(offset&~1)<< 8) + paletteram.read(offset|1);
		r	=	(((rgb & (1<<0xc))!=0?1:0)<<0) |
				(((rgb & (1<<0xb))!=0?1:0)<<1) |
				(((rgb & (1<<0xe))!=0?1:0)<<2) |
				(((rgb & (1<<0xf))!=0?1:0)<<3);
		g	=	(((rgb & (1<<0x8))!=0?1:0)<<0) |
				(((rgb & (1<<0x9))!=0?1:0)<<1) |
				(((rgb & (1<<0xa))!=0?1:0)<<2) |
				(((rgb & (1<<0xd))!=0?1:0)<<3);
		b	=	(((rgb & (1<<0x4))!=0?1:0)<<0) |
				(((rgb & (1<<0x3))!=0?1:0)<<1) |
				(((rgb & (1<<0x6))!=0?1:0)<<2) |
				(((rgb & (1<<0x7))!=0?1:0)<<3);
	
		r = (r << 4) | r;
		g = (g << 4) | g;
		b = (b << 4) | b;
		palette_set_color(offset/2,r,g,b);
	} };
	
	
	
	static int suna8_vh_start_common(int dim)
	{
		suna8_text_dim = dim;
		tilemap = tilemap_create(	get_tile_info, tilemap_scan_cols,
									TILEMAP_TRANSPARENT,
									8,8,0x20*((suna8_text_dim > 0)?4:8),0x20);
	
		if ( tilemap == null )	return 1;
	
		if (!(suna8_text_dim > 0))
		{
			paletteram	=	memory_region(REGION_USER1);
			spriteram	=	memory_region(REGION_USER2);
			suna8_spritebank  = 0;
			suna8_palettebank = 0;
		}
	
		tilemap_set_transparent_pen(tilemap,15);
		return 0;
	}
	
	public static VhStartPtr suna8_vh_start_textdim0 = new VhStartPtr() { public int handler() 	{ return suna8_vh_start_common(0);  } };
	public static VhStartPtr suna8_vh_start_textdim8 = new VhStartPtr() { public int handler() 	{ return suna8_vh_start_common(8);  } };
	public static VhStartPtr suna8_vh_start_textdim12 = new VhStartPtr() { public int handler() 	{ return suna8_vh_start_common(12); } };
	
	/***************************************************************************
	
	
									Sprites Drawing
	
	
	***************************************************************************/
	
	static void suna8_draw_normal_sprites(mame_bitmap bitmap)
	{
		int i;
		int mx = 0;	// multisprite x counter
	
		int max_x	=	Machine.drv.screen_width	- 8;
		int max_y	=	Machine.drv.screen_height - 8;
	
		for (i = 0x1d00; i < 0x2000; i += 4)
		{
			int srcpg, srcx,srcy, dimx,dimy, tx, ty;
			int gfxbank, flipx,flipy, multisprite;
	
			int y		=	spriteram.read(i + 0);
			int code	=	spriteram.read(i + 1);
			int x		=	spriteram.read(i + 2);
			int bank	=	spriteram.read(i + 3);
	
			if (suna8_text_dim > 0)
			{
				/* Older, simpler hardware */
				flipx = 0;
				flipy = 0;
				gfxbank = bank & 0x3f;
				switch( code & 0x80 )
				{
				case 0x80:
					dimx = 2;					dimy =	32;
					srcx  = (code & 0xf) * 2;	srcy = 0;
					srcpg = (code >> 4) & 3;
					break;
				case 0x00:
				default:
					dimx = 2;					dimy =	2;
					srcx  = (code & 0xf) * 2;	srcy = ((code >> 5) & 0x3) * 8 + 6;
					srcpg = (code >> 4) & 1;
					break;
				}
				multisprite = ((code & 0x80)!=0 && (code & 0x40)!=0)?1:0;
			}
			else
			{
				/* Newer, more complex hardware (not finished yet!) */
				switch( code & 0xc0 )
				{
				case 0xc0:
					dimx = 4;					dimy = 32;
					srcx  = (code & 0xe) * 2;	srcy = 0;
					flipx = (code & 0x1);
					flipy = 0;
					gfxbank = bank & 0x1f;
					srcpg = (code >> 4) & 3;
					break;
				case 0x80:
					dimx = 2;					dimy = 32;
					srcx  = (code & 0xf) * 2;	srcy = 0;
					flipx = 0;
					flipy = 0;
					gfxbank = bank & 0x1f;
					srcpg = (code >> 4) & 3;
					break;
				case 0x40:
					dimx = 4;					dimy = 4;
					srcx  = (code & 0xf) * 2;
					flipx = 0;
					flipy = bank & 0x10;
					srcy  = (((bank & 0x80)>>4) + (bank & 0x04) + ((~bank >> 4)&2)) * 2;
					gfxbank = bank & 0x3;	// ??? brickzn: 06,a6,a2,b2.6. starfigh: 01.01,4.0
					srcpg = (code >> 4) & 7;
					break;
				case 0x00:
				default:
					dimx = 2;					dimy = 2;
					srcx  = (code & 0xf) * 2;
					flipx = 0;
					flipy = 0;
					gfxbank = bank & 0x03;
					srcy  = (((bank & 0x80)>>4) + (bank & 0x04) + ((~bank >> 4)&3)) * 2;
					srcpg = (code >> 4) & 3;
					break;
				}
				multisprite = ((code & 0x80)!=0 && (bank & 0x80)!=0)?1:0;
			}
	
			x = x - ((bank & 0x40)!=0 ? 0x100 : 0);
			y = (0x100 - y - dimy*8 ) & 0xff;
	
			/* Multi Sprite */
			if ( multisprite !=0 )	{	mx += dimx*8;	x = mx;	}
			else					mx = x;
	
			gfxbank	*= 0x400;
	
			for (ty = 0; ty < dimy; ty ++)
			{
				for (tx = 0; tx < dimx; tx ++)
				{
					int addr	=	(srcpg * 0x20 * 0x20) +
									((srcx + (flipx!=0?dimx-tx-1:tx)) & 0x1f) * 0x20 +
									((srcy + (flipy!=0?dimy-ty-1:ty)) & 0x1f);
	
					int tile	=	spriteram.read(addr*2 + 0);
					int attr	=	spriteram.read(addr*2 + 1);
	
					int tile_flipx	=	attr & 0x40;
					int tile_flipy	=	attr & 0x80;
	
					int sx		=	 x + tx * 8;
					int sy		=	(y + ty * 8) & 0xff;
	
					if (flipx!=0)	tile_flipx = tile_flipx!=0?0:1;
					if (flipy!=0)	tile_flipy = tile_flipy!=0?0:1;
	
					if (flip_screen()!=0)
					{	sx = max_x - sx;	tile_flipx = tile_flipx!=0?0:1;
						sy = max_y - sy;	tile_flipy = tile_flipy!=0?0:1;	}
	
					drawgfx(	bitmap,Machine.gfx[0],
								tile + (attr & 0x3)*0x100 + gfxbank,
								(attr >> 2) & 0xf,
								tile_flipx, tile_flipy,
								sx, sy,
								Machine.visible_area,TRANSPARENCY_PEN,15);
				}
			}
	
		}
	}
	
	static void suna8_draw_text_sprites(mame_bitmap bitmap)
	{
		int i;
	
		int max_x	=	Machine.drv.screen_width	- 8;
		int max_y	=	Machine.drv.screen_height - 8;
	
		/* Earlier games only */
		if (!(suna8_text_dim > 0))	return;
	
		for (i = 0x1900; i < 0x19ff; i += 4)
		{
			int srcpg, srcx,srcy, dimx,dimy, tx, ty;
	
			int y		=	spriteram.read(i + 0);
			int code	=	spriteram.read(i + 1);
			int x		=	spriteram.read(i + 2);
			int bank	=	spriteram.read(i + 3);
	
			if ((~code & 0x80)!=0)	continue;
	
			dimx = 2;					dimy = suna8_text_dim;
			srcx  = (code & 0xf) * 2;	srcy = (y & 0xf0) / 8;
			srcpg = (code >> 4) & 3;
	
			x = x - ((bank & 0x40)!=0 ? 0x100 : 0);
			y = 0;
	
			bank	=	(bank & 0x3f) * 0x400;
	
			for (ty = 0; ty < dimy; ty ++)
			{
				for (tx = 0; tx < dimx; tx ++)
				{
					int real_ty	=	(ty < (dimy/2)) ? ty : (ty + 0x20 - dimy);
	
					int addr	=	(srcpg * 0x20 * 0x20) +
									((srcx + tx) & 0x1f) * 0x20 +
									((srcy + real_ty) & 0x1f);
	
					int tile	=	spriteram.read(addr*2 + 0);
					int attr	=	spriteram.read(addr*2 + 1);
	
					int flipx	=	attr & 0x40;
					int flipy	=	attr & 0x80;
	
					int sx		=	 x + tx * 8;
					int sy		=	(y + real_ty * 8) & 0xff;
	
					if (flip_screen()!=0)
					{	sx = max_x - sx;	flipx = flipx!=0?0:1;
						sy = max_y - sy;	flipy = flipy!=0?0:1;	}
	
					drawgfx(	bitmap,Machine.gfx[0],
								tile + (attr & 0x3)*0x100 + bank,
								(attr >> 2) & 0xf,
								flipx, flipy,
								sx, sy,
								Machine.visible_area,TRANSPARENCY_PEN,15);
				}
			}
	
		}
	}
	
	/***************************************************************************
	
	
									Screen Drawing
	
	
	***************************************************************************/
	
	/*
		Set TILEMAPS to 1 to debug.
		Press Z (you see the "tilemaps" in RAM) or
		Press X (you see the "tilemaps" in ROM) then
	
		- use Q&W to cycle through the pages.
		- Use R&T to cycle through the tiles banks.
		- Use A&S to cycle through the ROM banks (only with X pressed, of course).
	*/
	public static int TILEMAPS = 0;
	
	public static VhUpdatePtr suna8_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	#if TILEMAPS
/*TODO*///	{
/*TODO*///		char buf[80];
/*TODO*///		int max_tiles = memory_region_length(REGION_GFX1) / (0x400 * 0x20);
/*TODO*///	
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_Q))	{ page--;	tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);	}
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_W))	{ page++;	tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);	}
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_R))	{ tiles--;	tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);	}
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_T))	{ tiles++;	tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);	}
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_A))	{ rombank--;	tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);	}
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_S))	{ rombank++;	tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);	}
/*TODO*///	
/*TODO*///		rombank  &= 0xf;
/*TODO*///		page  &= (suna8_text_dim > 0)?3:7;
/*TODO*///		tiles %= max_tiles;
/*TODO*///		if (tiles < 0)	tiles += max_tiles;
/*TODO*///	
/*TODO*///		tilemap_set_scrollx( tilemap, 0, 0x100 * page);
/*TODO*///		tilemap_set_scrolly( tilemap, 0, 0);
/*TODO*///	
/*TODO*///	#if 1
/*TODO*///		sprintf(buf,	"%02X %02X %02X %02X - p%2X g%02X r%02X",
/*TODO*///						suna8_rombank, suna8_palettebank, suna8_spritebank, suna8_unknown,
/*TODO*///						page,tiles,rombank	);
/*TODO*///		usrintf_showmessage(buf);
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	}
/*TODO*///	#endif
/*TODO*///	#endif
	
		/* see hardhead, hardhea2 test mode (press button 2 for both players) */
		fillbitmap(bitmap,Machine.pens[0xff],Machine.visible_area);
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	#if TILEMAPS
/*TODO*///		if (keyboard_pressed(KEYCODE_Z) || keyboard_pressed(KEYCODE_X))
/*TODO*///			tilemap_draw(bitmap, tilemap, 0, 0);
/*TODO*///		else
/*TODO*///	#endif
/*TODO*///	#endif
		{
			suna8_draw_normal_sprites(bitmap);
			suna8_draw_text_sprites(bitmap);
		}
	} };
}
