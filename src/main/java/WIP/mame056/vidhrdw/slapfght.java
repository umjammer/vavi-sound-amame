/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of early Toaplan hardware.

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
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class slapfght
{
	
	public static UBytePtr slapfight_videoram = new UBytePtr();
	public static UBytePtr slapfight_colorram = new UBytePtr();
	public static int[] slapfight_videoram_size = new int[2];
	public static UBytePtr slapfight_scrollx_lo = new UBytePtr(), slapfight_scrollx_hi = new UBytePtr(), slapfight_scrolly = new UBytePtr();
	static int flipscreen;
	
	static struct_tilemap pf1_tilemap, fix_tilemap;
	
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_pf_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int tile,color;
	
		tile=videoram.read(tile_index)+ ((colorram.read(tile_index)& 0x03) << 8);
		color=(colorram.read(tile_index)>> 3) & 0x0f;
		SET_TILE_INFO(
				0,
				tile,
				color,
				0);
            }
        };
	
	static GetTileInfoPtr get_pf1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int tile,color;
	
		tile=videoram.read(tile_index)+ ((colorram.read(tile_index)& 0x0f) << 8);
		color=(colorram.read(tile_index)& 0xf0) >> 4;
	
		SET_TILE_INFO(
				1,
				tile,
				color,
				0);
            }
        };
	
	static GetTileInfoPtr get_fix_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int tile,color;
	
		tile=slapfight_videoram.read(tile_index) + ((slapfight_colorram.read(tile_index) & 0x03) << 8);
		color=(slapfight_colorram.read(tile_index) & 0xfc) >> 2;
	
		SET_TILE_INFO(
				0,
				tile,
				color,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr perfrman_vh_start = new VhStartPtr() { public int handler() 
	{
		pf1_tilemap = tilemap_create(get_pf_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
	
		if (pf1_tilemap == null)
			return 1;
	
		tilemap_set_transparent_pen(pf1_tilemap,0);
	
		return 0;
	} };
	
	public static VhStartPtr slapfight_vh_start = new VhStartPtr() { public int handler() 
	{
		pf1_tilemap = tilemap_create(get_pf1_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,64,32);
		fix_tilemap = tilemap_create(get_fix_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
	
		if (pf1_tilemap==null || fix_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fix_tilemap,0);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr slapfight_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram.write(offset,data);
		tilemap_mark_tile_dirty(pf1_tilemap,offset);
	} };
	
	public static WriteHandlerPtr slapfight_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		colorram.write(offset,data);
		tilemap_mark_tile_dirty(pf1_tilemap,offset);
	} };
	
	public static WriteHandlerPtr slapfight_fixram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		slapfight_videoram.write(offset, data);
		tilemap_mark_tile_dirty(fix_tilemap,offset);
	} };
	
	public static WriteHandlerPtr slapfight_fixcol_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		slapfight_colorram.write(offset, data);
		tilemap_mark_tile_dirty(fix_tilemap,offset);
	} };
	
	public static WriteHandlerPtr slapfight_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("Writing %02x to flipscreen\n",offset);
		if (offset==0) flipscreen=1; /* Port 0x2 is flipscreen */
		else flipscreen=0; /* Port 0x3 is normal */
	} };
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	void slapfght_log_vram(void)
/*TODO*///	{
/*TODO*///		if ( keyboard_pressed_memory(KEYCODE_B) )
/*TODO*///		{
/*TODO*///			int i;
/*TODO*///			for (i=0; i<0x800; i++)
/*TODO*///			{
/*TODO*///				logerror("Offset:%03x   TileRAM:%02x   AttribRAM:%02x   SpriteRAM:%02x\n",i, videoram.read(i),colorram.read(i),spriteram.read(i));
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	#endif
	
	/***************************************************************************
	
	  Render the Sprites
	
	***************************************************************************/
	static void perfrman_draw_sprites( mame_bitmap bitmap, int priority_to_display )
	{
		int offs;
	
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			int sx, sy;
	
			if ((buffered_spriteram.read(offs+2) & 0x80) == priority_to_display)
			{
				if (flipscreen != 0)
				{
					sx = 265 - buffered_spriteram.read(offs+1);
					sy = 239 - buffered_spriteram.read(offs+3);
					sy &= 0xff;
				}
				else
				{
					sx = buffered_spriteram.read(offs+1) + 3;
					sy = buffered_spriteram.read(offs+3) - 1;
				}
				drawgfx(bitmap,Machine.gfx[1],
					buffered_spriteram.read(offs),
					((buffered_spriteram.read(offs+2) >> 1) & 3)
						+ ((buffered_spriteram.read(offs+2) << 2) & 4)
	//					+ ((buffered_spriteram[offs+2] >> 2) & 8)
					,
					flipscreen, flipscreen,
					sx, sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static VhUpdatePtr perfrman_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_set_flip( pf1_tilemap, flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
		tilemap_set_scrolly( pf1_tilemap ,0 , 0 );
		if (flipscreen != 0) {
			tilemap_set_scrollx( pf1_tilemap ,0 , 264 );
		}
		else {
			tilemap_set_scrollx( pf1_tilemap ,0 , -16 );
		}
	
		fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
	
		perfrman_draw_sprites(bitmap,0);
		tilemap_draw(bitmap,pf1_tilemap,0,0);
		perfrman_draw_sprites(bitmap,0x80);
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///		slapfght_log_vram();
/*TODO*///	#endif
	} };
	
	
	public static VhUpdatePtr slapfight_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
		if (flipscreen != 0) {
			tilemap_set_scrollx( fix_tilemap,0,296);
			tilemap_set_scrollx( pf1_tilemap,0,(slapfight_scrollx_lo.read() + 256 * slapfight_scrollx_hi.read())+296 );
			tilemap_set_scrolly( pf1_tilemap,0, (slapfight_scrolly.read())+15 );
			tilemap_set_scrolly( fix_tilemap,0, -1 ); /* Glitch in Tiger Heli otherwise */
		}
		else {
			tilemap_set_scrollx( fix_tilemap,0,0);
			tilemap_set_scrollx( pf1_tilemap,0,(slapfight_scrollx_lo.read() + 256 * slapfight_scrollx_hi.read()) );
			tilemap_set_scrolly( pf1_tilemap,0, (slapfight_scrolly.read())-1 );
			tilemap_set_scrolly( fix_tilemap,0, -1 ); /* Glitch in Tiger Heli otherwise */
		}
	
		tilemap_draw(bitmap,pf1_tilemap,0,0);
	
		/* Draw the sprites */
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			if (flipscreen != 0)
				drawgfx(bitmap,Machine.gfx[2],
					buffered_spriteram.read(offs) + ((buffered_spriteram.read(offs+2) & 0xc0) << 2),
					(buffered_spriteram.read(offs+2) & 0x1e) >> 1,
					1,1,
					288-(buffered_spriteram.read(offs+1) + ((buffered_spriteram.read(offs+2) & 0x01) << 8)) +18,240-buffered_spriteram.read(offs+3),
					Machine.visible_area,TRANSPARENCY_PEN,0);
			else
				drawgfx(bitmap,Machine.gfx[2],
					buffered_spriteram.read(offs) + ((buffered_spriteram.read(offs+2) & 0xc0) << 2),
					(buffered_spriteram.read(offs+2) & 0x1e) >> 1,
					0,0,
					(buffered_spriteram.read(offs+1) + ((buffered_spriteram.read(offs+2) & 0x01) << 8)) - 13,buffered_spriteram.read(offs+3),
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
		tilemap_draw(bitmap,fix_tilemap,0,0);
	
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///		slapfght_log_vram();
/*TODO*///	#endif
	} };
}
