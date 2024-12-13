/*
**	Video Driver for Taito Samurai (1985)
*/

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
import static mame056.inputH.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class tsamurai
{
	
	
	/*
	** variables
	*/
	public static UBytePtr tsamurai_videoram = new UBytePtr();
	static int bgcolor;
	static int textbank1, textbank2;
	
	static struct_tilemap background, foreground;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attributes = tsamurai_videoram.read(2*tile_index+1);
		int tile_number = tsamurai_videoram.read(2*tile_index);
		tile_number += (( attributes & 0xc0 ) >> 6 ) * 256;	 /* legacy */
		tile_number += (( attributes & 0x20 ) >> 5 ) * 1024; /* Mission 660 add-on*/
		SET_TILE_INFO(
				0,
				tile_number,
				attributes & 0x1f,
				0);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int tile_number = videoram.read(tile_index);
		if ((textbank1 & 0x01)!=0) tile_number += 256; /* legacy */
		if ((textbank2 & 0x01)!=0) tile_number += 512; /* Mission 660 add-on */
		SET_TILE_INFO(
				1,
				tile_number,
				colorram.read(((tile_index&0x1f)*2)+1)& 0x1f,
				0);
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr tsamurai_vh_start = new VhStartPtr() { public int handler() 
	{
		background = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		foreground = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (background==null || foreground==null)
			return 1;
	
		tilemap_set_transparent_pen(background,0);
		tilemap_set_transparent_pen(foreground,0);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr tsamurai_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrolly( background, 0, data );
	} };
	
	public static WriteHandlerPtr tsamurai_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrollx( background, 0, data );
	} };
	
	public static WriteHandlerPtr tsamurai_bgcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		bgcolor = data;
	} };
	
	public static WriteHandlerPtr tsamurai_textbank1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( textbank1!=data )
		{
			textbank1 = data;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	} };
	
	public static WriteHandlerPtr tsamurai_textbank2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( textbank2!=data )
		{
			textbank2 = data;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	} };
	
	public static WriteHandlerPtr tsamurai_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( tsamurai_videoram.read(offset)!=data )
		{
			tsamurai_videoram.write(offset, data);
			offset = offset/2;
			tilemap_mark_tile_dirty(background,offset);
		}
	} };
	public static WriteHandlerPtr tsamurai_fg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( videoram.read(offset)!=data )
		{
			videoram.write(offset,data);
			tilemap_mark_tile_dirty(foreground,offset);
		}
	} };
	public static WriteHandlerPtr tsamurai_fg_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( colorram.read(offset)!=data )
		{
			colorram.write(offset,data);
			if ((offset & 1) != 0)
			{
				int col = offset/2;
				int row;
				for (row = 0;row < 32;row++)
					tilemap_mark_tile_dirty(foreground,32*row+col);
			}
		}
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	static int flicker;
        
	static void draw_sprites( mame_bitmap bitmap )
	{
		GfxElement gfx = Machine.gfx[2];
		rectangle clip = new rectangle(Machine.visible_area);
		UBytePtr source = new UBytePtr(spriteram, 32*4-4);
		UBytePtr finish = new UBytePtr(spriteram); /* ? */
		
		flicker = 1-flicker;
	
		while( source.offset>=finish.offset )
		{
			int attributes = source.read(2); /* bit 0x10 is usually, but not always set */
	
			int sx = source.read(3) - 16;
			int sy = 240-source.read(0);
			int sprite_number = source.read(1);
			int color = attributes&0x1f;
			//color = 0x2d - color; nunchakun fix?
			if( sy<-16 ) sy += 256;
	
			if( flip_screen() != 0 )
			{
				drawgfx( bitmap,gfx,
					sprite_number&0x7f,
					color,
					1,(sprite_number&0x80)!=0?0:1,
					256-32-sx,256-32-sy,
					clip,TRANSPARENCY_PEN,0 );
			}
			else
			{
				drawgfx( bitmap,gfx,
					sprite_number&0x7f,
					color,
					0,sprite_number&0x80,
					sx,sy,
					clip,TRANSPARENCY_PEN,0 );
			}
	
			source.dec(4);
		}
	}
	
	public static VhUpdatePtr tsamurai_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		int i;
	
	/* Do the column scroll used for the "660" logo on the title screen */
		tilemap_set_scroll_cols(foreground, 32);
		for (i = 0 ; i < 32 ; i++)
		{
			tilemap_set_scrolly(foreground, i, colorram.read(i*2));
		}
	/* end of column scroll code */
	
		/*
			This following isn't particularly efficient.  We'd be better off to
			dynamically change every 8th palette to the background color, so we
			could draw the background as an opaque tilemap.
	
			Note that the background color register isn't well understood
			(screenshots would be helpful)
		*/
		fillbitmap(bitmap,Machine.pens[bgcolor],Machine.visible_area);
		tilemap_draw(bitmap,background,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,foreground,0,0);
	} };
	
	/***************************************************************************
	
	VS Gong Fight runs on older hardware
	
	***************************************************************************/
	
	static int vsgongf_color;
	
	public static WriteHandlerPtr vsgongf_color_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( vsgongf_color != data )
		{
			vsgongf_color = data;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	} };
	
	static GetTileInfoPtr get_vsgongf_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int tile_number = videoram.read(tile_index);
		int color = vsgongf_color&0x1f;
		if( textbank1 != 0 ) tile_number += 0x100;
		SET_TILE_INFO(
				1,
				tile_number,
				color,
				0);
            }
        };
	
	public static VhStartPtr vsgongf_vh_start = new VhStartPtr() { public int handler() 
	{
		foreground = tilemap_create(get_vsgongf_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
		if (foreground == null) return 1;
		return 0;
	} };
        
        static int k;
	
	public static VhUpdatePtr vsgongf_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		
/*TODO*///		if( keyboard_pressed( KEYCODE_Q ) ){
/*TODO*///			while( keyboard_pressed( KEYCODE_Q ) ){}
/*TODO*///			k++;
/*TODO*///			vsgongf_color = k;
/*TODO*///			tilemap_mark_all_tiles_dirty( foreground );
/*TODO*///		}
	
		tilemap_draw(bitmap,foreground,0,0);
		draw_sprites(bitmap);
	} };
}
