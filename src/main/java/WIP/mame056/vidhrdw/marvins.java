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

import static WIP.mame056.drivers.marvins.madcrash_vreg;

public class marvins
{
	
	static int flipscreen, sprite_flip_adjust;
	static struct_tilemap fg_tilemap, bg_tilemap, tx_tilemap;
	static int bg_color, fg_color, old_bg_color, old_fg_color;
	
	/***************************************************************************
	**
	**  Palette Handling:
	**
	**  Each color entry is encoded by 12 bits in color proms
	**
	**  There are sixteen 8-color sprite palettes
	**  sprite palette is selected by a nibble of spriteram
	**
	**  There are eight 16-color text layer character palettes
	**  character palette is determined by character_number
	**
	**  Background and Foreground tilemap layers each have eight 16-color
	**  palettes.  A palette bank select register is associated with the whole
	**  layer.
	**
	***************************************************************************/
	
	public static WriteHandlerPtr marvins_palette_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		bg_color = data>>4;
		fg_color = data&0xf;
	} };
	
	static void stuff_palette( int source_index, int dest_index, int num_colors )
	{
		UBytePtr color_prom = new UBytePtr(memory_region(REGION_PROMS), source_index);
		int i;
		for( i=0; i<num_colors; i++ )
		{
			int bit0=0,bit1,bit2,bit3;
			int red, green, blue;
	
			bit0 = (color_prom.read(0x800)>> 2) & 0x01; // ?
			bit1 = (color_prom.read(0x000)>> 1) & 0x01;
			bit2 = (color_prom.read(0x000)>> 2) & 0x01;
			bit3 = (color_prom.read(0x000)>> 3) & 0x01;
			red = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			bit0 = (color_prom.read(0x800)>> 1) & 0x01; // ?
			bit1 = (color_prom.read(0x400)>> 2) & 0x01;
			bit2 = (color_prom.read(0x400)>> 3) & 0x01;
			bit3 = (color_prom.read(0x000)>> 0) & 0x01;
			green = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			bit0 = (color_prom.read(0x800)>> 0) & 0x01; // ?
			bit1 = (color_prom.read(0x800)>> 3) & 0x01; // ?
			bit2 = (color_prom.read(0x400)>> 0) & 0x01;
			bit3 = (color_prom.read(0x400)>> 1) & 0x01;
			blue = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			palette_set_color( dest_index++, red, green, blue );
			color_prom.inc();
		}
	}
	
	static void update_palette( int type )
	{
		if( bg_color!=old_bg_color )
		{
			stuff_palette( 256+16*(bg_color&0x7), (0x11-type)*16, 16 );
			old_bg_color = bg_color;
		}
	
		if( fg_color!=old_fg_color )
		{
			stuff_palette( 128+16*(fg_color&0x7), (0x10+type)*16, 16 );
			old_fg_color = fg_color;
		}
	}
	
	/***************************************************************************
	**
	**  Memory Handlers
	**
	***************************************************************************/
	
	public static WriteHandlerPtr marvins_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		spriteram.write(offset,data);
	} };
	public static ReadHandlerPtr marvins_spriteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return spriteram.read(offset);
	} };
	
	public static ReadHandlerPtr marvins_foreground_ram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return videoram.read(offset+0x1000);
	} };
	public static WriteHandlerPtr marvins_foreground_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( offset<0x800 )
		{
			if( videoram.read(offset+0x1000)==data ) return;
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
		videoram.write(offset+0x1000,data);
	} };
	
	public static ReadHandlerPtr marvins_background_ram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return videoram.read(offset);
	} };
	public static WriteHandlerPtr marvins_background_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( offset<0x800 )
		{
			if( videoram.read(offset)==data ) return;
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
		videoram.write(offset,data);
	} };
	
	public static ReadHandlerPtr marvins_text_ram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return videoram.read(offset+0x2000);
	} };
	public static WriteHandlerPtr marvins_text_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( offset<0x400 )
		{
			if( videoram.read(offset+0x2000)==data ) return;
			tilemap_mark_tile_dirty(tx_tilemap,offset);
		}
		videoram.write(offset+0x2000,data);
	} };
	
	/***************************************************************************
	**
	**  Callbacks for Tilemap Manager
	**
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tilemap_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		SET_TILE_INFO(
				2,
				videoram.read(tile_index),
				0,
				0);
            }
        };
	
	static GetTileInfoPtr get_fg_tilemap_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		SET_TILE_INFO(
				1,
				videoram.read(tile_index+0x1000),
				0,
				0);
            }
        };
	
	static GetTileInfoPtr get_tx_tilemap_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int tile_number = videoram.read(tile_index+0x2000);
		SET_TILE_INFO(
				0,
				tile_number,
				(tile_number>>5),
				0);
            }
        };
	
	/***************************************************************************
	**
	**  Video Initialization
	**
	***************************************************************************/
	
	public static VhStartPtr marvins_vh_start = new VhStartPtr() { public int handler() 
	{
		flipscreen = -1; old_bg_color = old_fg_color = -1;
	
		stuff_palette( 0, 0, 16*8 ); /* load sprite colors */
		stuff_palette( 16*8*3, 16*8, 16*8 ); /* load text colors */
	
		fg_tilemap = tilemap_create(get_fg_tilemap_info,tilemap_scan_cols,TILEMAP_TRANSPARENT,8,8,64,32);
		bg_tilemap = tilemap_create(get_bg_tilemap_info,tilemap_scan_cols,TILEMAP_TRANSPARENT,8,8,64,32);
		tx_tilemap = tilemap_create(get_tx_tilemap_info,tilemap_scan_cols,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (fg_tilemap==null || bg_tilemap==null || tx_tilemap==null)
			return 1;
	
		{
			rectangle clip = new rectangle(Machine.visible_area);
			clip.max_x-=16;
			clip.min_x+=16;
			tilemap_set_clip( fg_tilemap, clip );
			tilemap_set_clip( bg_tilemap, clip );
			tilemap_set_clip( tx_tilemap, clip );
	
			tilemap_set_transparent_pen(fg_tilemap,0xf);
			tilemap_set_transparent_pen(bg_tilemap,0xf);
			tilemap_set_transparent_pen(tx_tilemap,0xf);
	
			if( strcmp(Machine.gamedrv.name,"marvins")==0 )
			{
				tilemap_set_scrolldx( bg_tilemap,   271, 287 );
				tilemap_set_scrolldx( fg_tilemap,   15, 13+18 );
				sprite_flip_adjust = 256+182+1;
			}
			else
			{
				tilemap_set_scrolldx( bg_tilemap,   -16, -10 );
				tilemap_set_scrolldx( fg_tilemap,   16, 22 );
				sprite_flip_adjust = 256+182;
			}
	
			tilemap_set_scrolldx( tx_tilemap, 16, 16 );
			tilemap_set_scrolldy( bg_tilemap, 0, -40 );
			tilemap_set_scrolldy( fg_tilemap, 0, -40 );
			tilemap_set_scrolldy( tx_tilemap, 0, 0 );
	
			return 0;
		}
	} };
	
	/***************************************************************************
	**
	**  Screen Refresh
	**
	***************************************************************************/
	
	static void draw_status( mame_bitmap bitmap )
	{
		UBytePtr base = new UBytePtr(videoram, 0x2400);
		rectangle clip = new rectangle(Machine.visible_area);
		GfxElement gfx = Machine.gfx[0];
		int row;
		for( row=0; row<4; row++ )
		{
			int sy,sx = (row&1)*8;
			UBytePtr source = new UBytePtr(base, (row&1)*32);
			if( row>1 )
			{
				sx+=256+16;
			}
			else
			{
				source.inc(30*32);
			}
	
			for( sy=0; sy<256; sy+=8 )
			{
				int tile_number = source.readinc();
				drawgfx( bitmap, gfx,
				    tile_number, tile_number>>5,
				    0,0, /* no flip */
				    sx,sy,
				    clip,
				    TRANSPARENCY_NONE, 0xf );
			}
		}
	}
	
	static void draw_sprites( mame_bitmap bitmap, int scrollx, int scrolly,
			int priority, int sprite_partition )
	{
		GfxElement gfx = Machine.gfx[3];
		rectangle clip = new rectangle(Machine.visible_area);
		UBytePtr source, finish;
	
		if( sprite_partition>0x64 ) sprite_partition = 0x64;
	
		if( priority != 0 )
		{
			source = new UBytePtr(spriteram, sprite_partition);
			finish = new UBytePtr(spriteram, 0x64);
		}
		else
		{
			source = new UBytePtr(spriteram);
			finish = new UBytePtr(spriteram, sprite_partition);
		}
	
		while( source.offset<finish.offset )
		{
			int attributes = source.read(3); /* Y?F? CCCC */
			int tile_number = source.read(1);
			int sy = (-16+source.read(0) - scrolly)&0xff;
			int sx = source.read(2) - scrollx + ((attributes&0x80)!=0?256:0);
			int color = attributes&0xf;
			int flipy = (attributes&0x20);
			int flipx = 0;
	
			if( flipscreen != 0 )
			{
				if( flipy != 0 )
				{
				    flipx = 1; flipy = 0;
				}
				else
				{
				    flipx = flipy = 1;
				}
				sx = sprite_flip_adjust-sx;
				sy = 246-sy;
			}
	
			if( sy>240 ) sy -= 256;
	
			drawgfx( bitmap,gfx,
				tile_number,
				color,
				flipx, flipy,
				(256-sx)&0x1ff,sy,
				clip,TRANSPARENCY_PEN,7);
	
			source.inc(4);
		}
	}
	
	public static VhUpdatePtr marvins_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		UBytePtr mem = new UBytePtr(memory_region(REGION_CPU1));
	
		int sprite_partition = mem.read(0xfe00);
	
		int attributes = mem.read(0x8600); /* 0x20: normal, 0xa0: video flipped */
		int scroll_attributes = mem.read(0xff00);
		int sprite_scrolly = mem.read(0xf800);
		int sprite_scrollx = mem.read(0xf900);
	
		int bg_scrolly = mem.read(0xfa00);
		int bg_scrollx = mem.read(0xfb00);
		int fg_scrolly = mem.read(0xfc00);
		int fg_scrollx = mem.read(0xfd00);
	
		if( (scroll_attributes & 4)==0 ) bg_scrollx += 256;
		if(( scroll_attributes & 1 )!=0) sprite_scrollx += 256;
		if(( scroll_attributes & 2 )!=0) fg_scrollx += 256;
	
		/* palette bank for background/foreground is set by a memory-write handler */
		update_palette(0);
	
		if( flipscreen != (attributes&0x80) )
		{
			flipscreen = attributes&0x80;
			tilemap_set_flip( ALL_TILEMAPS, flipscreen!=0?TILEMAP_FLIPY|TILEMAP_FLIPX:0);
		}
	
		tilemap_set_scrollx( bg_tilemap,    0, bg_scrollx );
		tilemap_set_scrolly( bg_tilemap,    0, bg_scrolly );
		tilemap_set_scrollx( fg_tilemap,    0, fg_scrollx );
		tilemap_set_scrolly( fg_tilemap,    0, fg_scrolly );
		tilemap_set_scrollx( tx_tilemap,  0, 0 );
		tilemap_set_scrolly( tx_tilemap,  0, 0 );
	
		tilemap_draw( bitmap,fg_tilemap,TILEMAP_IGNORE_TRANSPARENCY ,0);
		draw_sprites( bitmap, sprite_scrollx+29+1, sprite_scrolly, 0, sprite_partition );
		tilemap_draw( bitmap,bg_tilemap,0 ,0);
		draw_sprites( bitmap, sprite_scrollx+29+1, sprite_scrolly, 1, sprite_partition );
		tilemap_draw( bitmap,tx_tilemap,0 ,0);
		draw_status( bitmap );
	} };
	
	public static VhUpdatePtr madcrash_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		UBytePtr mem = new UBytePtr(memory_region(REGION_CPU1), madcrash_vreg);
	
		int attributes = mem.read(0x8600); /* 0x20: normal, 0xa0: video flipped */
		int bg_scrolly = mem.read(0xf800);
		int bg_scrollx = mem.read(0xf900);
		int scroll_attributes = mem.read(0xfb00);
		int sprite_scrolly = mem.read(0xfc00);
		int sprite_scrollx = mem.read(0xfd00);
		int fg_scrolly = mem.read(0xfe00);
		int fg_scrollx = mem.read(0xff00);
		if( (scroll_attributes & 4)==0 ) bg_scrollx += 256;
		if(( scroll_attributes & 1 )!=0) sprite_scrollx += 256;
		if(( scroll_attributes & 2 )!=0) fg_scrollx += 256;
	
		marvins_palette_bank_w.handler( 0, mem.read(0xc800) );
		update_palette(1);
	
		if( flipscreen != (attributes&0x80) )
		{
			flipscreen = attributes&0x80;
			tilemap_set_flip( ALL_TILEMAPS, flipscreen!=0?TILEMAP_FLIPY|TILEMAP_FLIPX:0);
		}
	
		tilemap_set_scrollx( bg_tilemap, 0, bg_scrollx );
		tilemap_set_scrolly( bg_tilemap, 0, bg_scrolly );
		tilemap_set_scrollx( fg_tilemap, 0, fg_scrollx );
		tilemap_set_scrolly( fg_tilemap, 0, fg_scrolly );
		tilemap_set_scrollx( tx_tilemap,  0, 0 );
		tilemap_set_scrolly( tx_tilemap,  0, 0 );
	
		tilemap_draw( bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY ,0);
		tilemap_draw( bitmap,fg_tilemap,0 ,0);
		draw_sprites( bitmap, sprite_scrollx+29, sprite_scrolly+1, 1, 0 );
	
		tilemap_draw( bitmap,tx_tilemap,0 ,0);
		draw_status( bitmap );
	} };
}
