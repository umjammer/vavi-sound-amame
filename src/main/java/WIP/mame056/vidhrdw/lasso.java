/***************************************************************************

		driver by Phil Stroffolino, Nicola Salmoria, Luca Elia


Note:	if MAME_DEBUG is defined, pressing Z with:

		Q			shows the background
		W			shows the foreground (if present)
		A			shows the sprites

		Keys can be used together!


	Every game has 1 256 x 256 tilemap (non scrollable) made of 8 x 8 x 2
	tiles, and 16 x 16 x 2 sprites (some games use 32, some more).

	The graphics for tiles and sprites are held inside the same ROMs,
	but	aren't shared between the two:

	the first $100 tiles are for the tilemap, the following	$100 are
	for sprites. This constitutes the first graphics bank. There can
	be several.

	Lasso has an additional pixel layer (256 x 256 x 1) and a third
	CPU devoted to drawing into it (the lasso!)

	Wwjgtin has an additional $800 x $400 scrolling tilemap in ROM
	and $100 more 16 x 16 x 4 tiles for it.

	The colors are static ($40 colors, 2 PROMs) but the background
	color can be changed at runtime. Wwjgtin can change the last
	4 colors (= last palette) too.

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

public class lasso
{
	
	/* variables only used here: */
	static struct_tilemap background, background1;
	static int gfxbank, wwjgtin_bg1_enable;
	
	
	/* variables needed externally: */
	public static UBytePtr lasso_vram=new UBytePtr(); 	/* 0x2000 bytes for a 256 x 256 x 1 bitmap */
	public static UBytePtr wwjgtin_scroll=new UBytePtr();
	
	
	/***************************************************************************
	
	
									Memory Handlers
	
	
	***************************************************************************/
	
	public static WriteHandlerPtr lasso_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( videoram.read(offset)!=data )
		{
			videoram.write(offset,data);
			tilemap_mark_tile_dirty( background, offset&0x3ff );
		}
	} };
	
	public static WriteHandlerPtr lasso_gfxbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bank = (data & 0x04) >> 2;
	
		flip_screen_set( data & 0x01 );
	
		if (gfxbank != bank)
		{
			gfxbank = bank;
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	} };
	
	public static WriteHandlerPtr wwjgtin_gfxbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bank = ((data & 0x04)!=0 ? 0 : 1) + ((data & 0x10)!=0 ? 2 : 0);
		wwjgtin_bg1_enable = data & 0x08;
	
		flip_screen_set( data & 0x01 );
	
		if (gfxbank != bank)
		{
			gfxbank = bank;
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	} };
	
	/***************************************************************************
	
	
								Colors (BBGGGRRR)
	
	
	***************************************************************************/
	
	static void lasso_set_color(int i, int data)
	{
		int bit0,bit1,bit2,r,g,b;
	
		/* red component */
		bit0 = (data >> 0) & 0x01;
		bit1 = (data >> 1) & 0x01;
		bit2 = (data >> 2) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* green component */
		bit0 = (data >> 3) & 0x01;
		bit1 = (data >> 4) & 0x01;
		bit2 = (data >> 5) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* blue component */
		bit0 = (data >> 6) & 0x01;
		bit1 = (data >> 7) & 0x01;
		b = 0x4f * bit0 + 0xa8 * bit1;
	
		palette_set_color( i,r,g,b );
	}
	
	public static VhConvertColorPromPtr lasso_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
	
		for (i = 0;i < 0x40;i++)
		{
			lasso_set_color(i,color_prom.read());
			color_prom.inc();
		}
            }
        };
	
	/* 16 color tiles with a 4 color step for the palettes */
	public static VhConvertColorPromPtr wwjgtin_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int color, pen;
	
		lasso_vh_convert_color_prom.handler(palette,colortable,color_prom);
	
		for( color = 0; color < 0x10; color++ )
			for( pen = 0; pen < 16; pen++ )
				colortable[color * 16 + pen + 4*16] = (char) ((color * 4 + pen) % 0x40);
            }
        };
	
	/* The background color can be changed */
	public static WriteHandlerPtr lasso_backcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i;
		for( i=0; i<0x40; i+=4 ) /* stuff into color#0 of each palette */
			lasso_set_color(i,data);
	} };
	
	/* The last 4 color (= last palette) entries can be changed */
	public static WriteHandlerPtr wwjgtin_lastcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		lasso_set_color(0x3f - offset,data);
	} };
	
	/***************************************************************************
	
	
									Tilemaps
	
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int tile_number	=	videoram.read(tile_index);
		int attributes	=	videoram.read(tile_index + 0x400);
		SET_TILE_INFO(		0,
							tile_number + (gfxbank << 8),
							attributes & 0x0f,
							0	);
            }
        };
	
	/* wwjgtin has an additional scrollable tilemap stored in ROM */
	static GetTileInfoPtr get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		UBytePtr ROM = new UBytePtr(memory_region(REGION_GFX3));
		int tile_number	=	ROM.read(tile_index);
		int attributes	=	ROM.read(tile_index + 0x2000);
		SET_TILE_INFO(		2,
							tile_number,
							attributes & 0x0f,
							0	);
            }
        };
	
	/***************************************************************************
	
	
							  	Video Hardware Init
	
	
	***************************************************************************/
	
	public static VhStartPtr lasso_vh_start = new VhStartPtr() { public int handler() 
	{
		background = tilemap_create(	get_bg_tile_info, tilemap_scan_rows,
										TILEMAP_OPAQUE,		8,8,	32,32);
	
		if (background == null)
			return 1;
	
		return 0;
	} };
	
	public static VhStartPtr wwjgtin_vh_start = new VhStartPtr() { public int handler() 
	{
		background = tilemap_create(	get_bg_tile_info, tilemap_scan_rows,
										TILEMAP_TRANSPARENT,	8,8,	32,32);
	
		background1 = tilemap_create(	get_bg1_tile_info, tilemap_scan_rows,
										TILEMAP_OPAQUE,			16,16,	0x80,0x40);
	
		if (background==null || background1==null)
			return 1;
	
		tilemap_set_transparent_pen(background,0);
		return 0;
	} };
	
	
	
	
	/***************************************************************************
	
									Sprites Drawing
	
	
			Offset:		Format:			Value:
	
				0						Y (Bottom-up)
	
				1		7--- ----		Flip Y
						-6-- ----		Flip X
						--54 3210		Code
	
				2		7654 ----
						---- 3210		Color
	
				3						X
	
	***************************************************************************/
	
	static void draw_sprites( mame_bitmap bitmap, int reverse )
	{
                rectangle clip = new rectangle(Machine.visible_area);
		UBytePtr finish=new UBytePtr(), source=new UBytePtr();
		int inc;
	
		if (reverse != 0)
		{
			source	=	new UBytePtr(spriteram);
			finish	=	new UBytePtr(spriteram, spriteram_size[0]);
			inc		=	4;
		}
		else
		{
			source	=	new UBytePtr(spriteram, spriteram_size[0] - 4);
			finish	=	new UBytePtr(spriteram, - 4);
			inc		=	-4;
		}
	
		while( source.offset != finish.offset)
		{
			int sy			=	source.read(0);
			int tile_number         =	source.read(1);
			int color		=	source.read(2);
			int sx			=	source.read(3);
	
			int flipx		=	(tile_number & 0x40);
			int flipy		=	(tile_number & 0x80);
	
			if( flip_screen() != 0 )
			{
				sx = 240-sx;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
			else
			{
				sy = 240-sy;
			}
	        drawgfx(	bitmap, Machine.gfx[1],
						(tile_number & 0x3f) + (gfxbank << 6),
						color,
						flipx, flipy,
						sx,sy,
						clip, TRANSPARENCY_PEN,0	);
	
		//if (inc>0)	
                    source.inc(inc);
                //else
                //    source.dec(inc*-1);
	    }
	}
	
	/***************************************************************************
	
	
									Pixmap Drawing
	
	
	***************************************************************************/
	
	static void draw_lasso( mame_bitmap bitmap )
	{
		UBytePtr source = new UBytePtr(lasso_vram);
		int x,y;
		int pen = Machine.pens[0x3f];
		for( y=0; y<256; y++ )
		{
			for( x=0; x<256; x+=8 )
			{
				int data = source.readinc();
				if( data != 0 )
				{
					int bit;
					if( flip_screen() != 0 )
					{
						for( bit=0; bit<8; bit++ )
						{
							if(( (data<<bit)&0x80 ) != 0)
								plot_pixel.handler( bitmap, 255-(x+bit), 255-y, pen );
						}
					}
					else
					{
						for( bit=0; bit<8; bit++ )
						{
							if(( (data<<bit)&0x80 ) != 0)
								plot_pixel.handler( bitmap, x+bit, y, pen );
						}
					}
				}
			}
		}
	}
	
	/***************************************************************************
	
	
									Screen Drawing
	
	
	***************************************************************************/
	
	public static VhUpdatePtr lasso_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		int layers_ctrl = -1;
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	if (keyboard_pressed(KEYCODE_Z))
/*TODO*///	{	int msk = 0;
/*TODO*///		if (keyboard_pressed(KEYCODE_Q))	msk |= 1;
/*TODO*///		if (keyboard_pressed(KEYCODE_W))	msk |= 2;
/*TODO*///		if (keyboard_pressed(KEYCODE_A))	msk |= 4;
/*TODO*///		if (msk != 0) layers_ctrl &= msk;		}
/*TODO*///	#endif
	
		if ((layers_ctrl & 1)!=0)	tilemap_draw(bitmap, background,  0,0);
		else					fillbitmap(bitmap,Machine.pens[0],null);
		if ((layers_ctrl & 2)!=0)	draw_lasso(bitmap);
		if ((layers_ctrl & 4)!=0)	draw_sprites(bitmap, 0);
	} };
	
	public static VhUpdatePtr chameleo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		int layers_ctrl = -1;
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	if (keyboard_pressed(KEYCODE_Z))
/*TODO*///	{	int msk = 0;
/*TODO*///		if (keyboard_pressed(KEYCODE_Q))	msk |= 1;
/*TODO*///		if (keyboard_pressed(KEYCODE_W))	msk |= 2;
/*TODO*///		if (keyboard_pressed(KEYCODE_A))	msk |= 4;
/*TODO*///		if (msk != 0) layers_ctrl &= msk;		}
/*TODO*///	#endif
	
		if ((layers_ctrl & 1)!=0)	tilemap_draw(bitmap, background,  0,0);
		else					fillbitmap(bitmap,Machine.pens[0],null);
	//	if (layers_ctrl & 2)	draw_lasso(bitmap);
		if ((layers_ctrl & 4)!=0)	draw_sprites(bitmap, 0);
	} };
	
	
	public static VhUpdatePtr wwjgtin_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		int layers_ctrl = -1;
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	if (keyboard_pressed(KEYCODE_Z))
/*TODO*///	{	int msk = 0;
/*TODO*///		if (keyboard_pressed(KEYCODE_Q))	msk |= 1;
/*TODO*///		if (keyboard_pressed(KEYCODE_W))	msk |= 2;
/*TODO*///		if (keyboard_pressed(KEYCODE_A))	msk |= 4;
/*TODO*///		if (msk != 0) layers_ctrl &= msk;		}
/*TODO*///	#endif
	
		tilemap_set_scrollx(background1,0,wwjgtin_scroll.read(0) + wwjgtin_scroll.read(1)*256);
		tilemap_set_scrolly(background1,0,wwjgtin_scroll.read(2) + wwjgtin_scroll.read(3)*256);
	
		if((layers_ctrl & 1)!=0 && wwjgtin_bg1_enable!=0)
			tilemap_draw(bitmap, background1, 0,0);
		else
			fillbitmap(bitmap,Machine.pens[0x40],null);	// BLACK
	
		if ((layers_ctrl & 4)!=0)	draw_sprites(bitmap, 1);	// reverse order
		if ((layers_ctrl & 2)!=0)	tilemap_draw(bitmap, background,  0,0);
	} };
}
