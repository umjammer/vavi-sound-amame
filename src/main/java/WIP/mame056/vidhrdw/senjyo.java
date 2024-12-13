/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

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
import static mame056.palette.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.usrintrf.usrintf_showmessage;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.inptport.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

import static WIP.mame056.machine.segacrpt.*;

public class senjyo
{
	
	public static UBytePtr senjyo_fgscroll = new UBytePtr();
	public static UBytePtr senjyo_scrollx1 = new UBytePtr(), senjyo_scrolly1 = new UBytePtr();
	public static UBytePtr senjyo_scrollx2 = new UBytePtr(), senjyo_scrolly2 = new UBytePtr();
	public static UBytePtr senjyo_scrollx3 = new UBytePtr(), senjyo_scrolly3 = new UBytePtr();
	public static UBytePtr senjyo_fgvideoram = new UBytePtr(), senjyo_fgcolorram = new UBytePtr();
	public static UBytePtr senjyo_bg1videoram = new UBytePtr(), senjyo_bg2videoram = new UBytePtr(), senjyo_bg3videoram = new UBytePtr();
	public static UBytePtr senjyo_radarram = new UBytePtr();
	public static UBytePtr senjyo_bgstripesram = new UBytePtr();
	
	static struct_tilemap fg_tilemap, bg1_tilemap, bg2_tilemap, bg3_tilemap;
	
	static int senjyo, scrollhack;
	static int senjyo_bgstripes;
	
	static mame_bitmap bgbitmap;
	
	
	public static InitDriverPtr init_starforc = new InitDriverPtr() { public void handler() 
	{
		senjyo = 0;
		scrollhack = 1;
	} };
	public static InitDriverPtr init_starfore = new InitDriverPtr() { public void handler() 
	{
		/* encrypted CPU */
		suprloco_decode();
	
		senjyo = 0;
		scrollhack = 0;
	} };
	public static InitDriverPtr init_senjyo = new InitDriverPtr() { public void handler() 
	{
		senjyo = 1;
		scrollhack = 0;
	} };
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = senjyo_fgcolorram.read(tile_index);
		SET_TILE_INFO(
				0,
				senjyo_fgvideoram.read(tile_index) + ((attr & 0x10) << 4),
				attr & 0x07,
				(attr & 0x80)!=0 ? TILE_FLIPY : 0);
		if (senjyo!=0 && (tile_index & 0x1f) >= 32-8)
			tile_info.flags |= TILE_IGNORE_TRANSPARENCY;
            }
        };
	
	static GetTileInfoPtr senjyo_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = senjyo_bg1videoram.read(tile_index);
		SET_TILE_INFO(
				1,
				code,
				(code & 0x70) >> 4,
				0);
            }
        };
	
	static GetTileInfoPtr starforc_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		/* Star Force has more tiles in bg1, so to get a uniform color code spread */
		/* they wired bit 7 of the tile code in place of bit 4 to get the color code */
		int colormap[] = { 0,2,4,6,1,3,5,7 };
		int code = senjyo_bg1videoram.read(tile_index);
		SET_TILE_INFO(
				1,
				code,
				colormap[(code & 0xe0) >> 5],
				0);
            }
        };
	
	static GetTileInfoPtr get_bg2_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = senjyo_bg2videoram.read(tile_index);
		SET_TILE_INFO(
				2,
				code,
				(code & 0xe0) >> 5,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg3_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code = senjyo_bg3videoram.read(tile_index);
		SET_TILE_INFO(
				3,
				code,
				(code & 0xe0) >> 5,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStopPtr senjyo_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(bgbitmap);
		bgbitmap = null;
	} };
	
	public static VhStartPtr senjyo_vh_start = new VhStartPtr() { public int handler() 
	{
		bgbitmap = bitmap_alloc(256,256);
		if (bgbitmap == null)
			return 1;
	
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		if (senjyo != 0)
		{
			bg1_tilemap = tilemap_create(senjyo_bg1_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,16,32);
			bg2_tilemap = tilemap_create(get_bg2_tile_info,   tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,16,48);	/* only 16x32 used by Star Force */
			bg3_tilemap = tilemap_create(get_bg3_tile_info,   tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,16,56);	/* only 16x32 used by Star Force */
		}
		else
		{
			bg1_tilemap = tilemap_create(starforc_bg1_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,16,32);
			bg2_tilemap = tilemap_create(get_bg2_tile_info,     tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,16,32);	/* only 16x32 used by Star Force */
			bg3_tilemap = tilemap_create(get_bg3_tile_info,     tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,16,32);	/* only 16x32 used by Star Force */
		}
	
	
		if (fg_tilemap==null || bg1_tilemap==null || bg2_tilemap==null || bg3_tilemap==null)
		{
			senjyo_vh_stop.handler();
	
			return 1;
		}
	
		tilemap_set_transparent_pen(fg_tilemap,0);
		tilemap_set_transparent_pen(bg1_tilemap,0);
		tilemap_set_transparent_pen(bg2_tilemap,0);
		tilemap_set_transparent_pen(bg3_tilemap,0);
		tilemap_set_scroll_cols(fg_tilemap,32);
	
		schedule_full_refresh();
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr senjyo_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (senjyo_fgvideoram.read(offset) != data)
		{
			senjyo_fgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
	} };
	public static WriteHandlerPtr senjyo_fgcolorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (senjyo_fgcolorram.read(offset) != data)
		{
			senjyo_fgcolorram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
	} };
	public static WriteHandlerPtr senjyo_bg1videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (senjyo_bg1videoram.read(offset) != data)
		{
			senjyo_bg1videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg1_tilemap,offset);
		}
	} };
	public static WriteHandlerPtr senjyo_bg2videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (senjyo_bg2videoram.read(offset) != data)
		{
			senjyo_bg2videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg2_tilemap,offset);
		}
	} };
	public static WriteHandlerPtr senjyo_bg3videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (senjyo_bg3videoram.read(offset) != data)
		{
			senjyo_bg3videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg3_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr senjyo_bgstripes_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		senjyo_bgstripesram.write( data );
		//set_vh_global_attribute(senjyo_bgstripes, data);
                senjyo_bgstripes = data;
	} };
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_bgbitmap(mame_bitmap bitmap, int full_refresh)
	{
		int x,y,pen,strwid,count;
	
	
		if (senjyo_bgstripes == 0xff)	/* off */
		{
			fillbitmap(bitmap,Machine.pens[0],null);
			return;
		}
	
		if (full_refresh != 0)
		{
			pen = 0;
			count = 0;
			strwid = senjyo_bgstripes;
			if (strwid == 0) strwid = 0x100;
			if (flip_screen() != 0) strwid ^= 0xff;
	
			for (x = 0;x < 256;x++)
			{
				if (flip_screen() != 0)
				{
					for (y = 0;y < 256;y++)
					{
						plot_pixel.handler(bgbitmap, 255 - x, y, Machine.pens[384 + pen]);
					}
				}
				else
				{
					for (y = 0;y < 256;y++)
					{
						plot_pixel.handler(bgbitmap, x, y, Machine.pens[384 + pen]);
					}
				}
	
				count += 0x10;
				if (count >= strwid)
				{
					pen = (pen + 1) & 0x0f;
					count -= strwid;
				}
			}
		}
	
		copybitmap(bitmap,bgbitmap,0,0,0,0,new rectangle(Machine.visible_area),TRANSPARENCY_NONE,0);
	}
	
	static void draw_radar(mame_bitmap bitmap)
	{
		int offs,x;
	
		for (offs = 0;offs < 0x400;offs++)
		{
			if (senjyo_radarram.read(offs) != 0)
			{
				for (x = 0;x < 8;x++)
				{
					if ((senjyo_radarram.read(offs) & (1 << x)) != 0)
					{
						int sx, sy;
	
						sx = (8 * (offs % 8) + x) + 256-64;
						sy = ((offs & 0x1ff) / 8) + 96;
	
						if (flip_screen() != 0)
						{
							sx = 255 - sx;
							sy = 255 - sy;
						}
	
						plot_pixel.handler(bitmap,
								   sx, sy,
								   Machine.pens[offs < 0x200 ? 400 : 401]);
					}
				}
			}
		}
	}
	
	static void draw_sprites(mame_bitmap bitmap,int priority)
	{
		rectangle clip = new rectangle(Machine.visible_area);
		int offs;
	
	
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int big,sx,sy,flipx,flipy;
	
			if (((spriteram.read(offs+1)& 0x30) >> 4) == priority)
			{
				if (senjyo != 0)	/* Senjyo */
					big = (spriteram.read(offs)& 0x80);
				else	/* Star Force */
					big = ((spriteram.read(offs)& 0xc0) == 0xc0)?1:0;
				sx = spriteram.read(offs+3);
				if (big != 0)
					sy = 224-spriteram.read(offs+2);
				else
					sy = 240-spriteram.read(offs+2);
				flipx = spriteram.read(offs+1)& 0x40;
				flipy = spriteram.read(offs+1)& 0x80;
	
				if (flip_screen() != 0)
				{
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
	
					if (big != 0)
					{
						sx = 224 - sx;
						sy = 226 - sy;
					}
					else
					{
						sx = 240 - sx;
						sy = 242 - sy;
					}
				}
	
	
				drawgfx(bitmap,Machine.gfx[big!=0 ? 5 : 4],
						spriteram.read(offs),
						spriteram.read(offs + 1)& 0x07,
						flipx,flipy,
						sx,sy,
						clip,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	public static VhUpdatePtr senjyo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int i;
	
	
		/* two colors for the radar dots (verified on the real board) */
		palette_set_color(400,0xff,0x00,0x00);	/* red for enemies */
		palette_set_color(401,0xff,0xff,0x00);	/* yellow for player */
	
		{
			int scrollx,scrolly;
	
			for (i = 0;i < 32;i++)
				tilemap_set_scrolly(fg_tilemap,i,senjyo_fgscroll.read(i));
	
			scrollx = senjyo_scrollx1.read(0);
			scrolly = senjyo_scrolly1.read(0) + 256 * senjyo_scrolly1.read(1);
			if (flip_screen() != 0)
				scrollx = -scrollx;
			tilemap_set_scrollx(bg1_tilemap,0,scrollx);
			tilemap_set_scrolly(bg1_tilemap,0,scrolly);
	
			scrollx = senjyo_scrollx2.read(0);
			scrolly = senjyo_scrolly2.read(0) + 256 * senjyo_scrolly2.read(1);
			if (scrollhack != 0)	/* Star Force, but NOT the encrypted version */
			{
				scrollx = senjyo_scrollx1.read(0);
				scrolly = senjyo_scrolly1.read(0) + 256 * senjyo_scrolly1.read(1);
			}
			if (flip_screen() != 0)
				scrollx = -scrollx;
			tilemap_set_scrollx(bg2_tilemap,0,scrollx);
			tilemap_set_scrolly(bg2_tilemap,0,scrolly);
	
			scrollx = senjyo_scrollx3.read(0);
			scrolly = senjyo_scrolly3.read(0) + 256 * senjyo_scrolly3.read(1);
			if (flip_screen() != 0)
				scrollx = -scrollx;
			tilemap_set_scrollx(bg3_tilemap,0,scrollx);
			tilemap_set_scrolly(bg3_tilemap,0,scrolly);
		}
	
		draw_bgbitmap(bitmap, full_refresh);
		draw_sprites(bitmap,0);
		tilemap_draw(bitmap,bg3_tilemap,0,0);
		draw_sprites(bitmap,1);
		tilemap_draw(bitmap,bg2_tilemap,0,0);
		draw_sprites(bitmap,2);
		tilemap_draw(bitmap,bg1_tilemap,0,0);
		draw_sprites(bitmap,3);
		tilemap_draw(bitmap,fg_tilemap,0,0);
		draw_radar(bitmap);
	
/*TODO*///	#if 0
/*TODO*///	{
/*TODO*///		char baf[80];
/*TODO*///	
/*TODO*///		sprintf(baf,"%02x %02x %02x %02x %02x %02x %02x %02x",
/*TODO*///			senjyo_scrolly3[0x00],
/*TODO*///			senjyo_scrolly3[0x01],
/*TODO*///			senjyo_scrolly3[0x02],
/*TODO*///			senjyo_scrolly3[0x03],
/*TODO*///			senjyo_scrolly3[0x04],
/*TODO*///			senjyo_scrolly3[0x05],
/*TODO*///			senjyo_scrolly3[0x06],
/*TODO*///			senjyo_scrolly3[0x07]);
/*TODO*///		ui_text(baf,0,0);
/*TODO*///		sprintf(baf,"%02x %02x %02x %02x %02x %02x %02x %02x",
/*TODO*///			senjyo_scrolly3[0x08],
/*TODO*///			senjyo_scrolly3[0x09],
/*TODO*///			senjyo_scrolly3[0x0a],
/*TODO*///			senjyo_scrolly3[0x0b],
/*TODO*///			senjyo_scrolly3[0x0c],
/*TODO*///			senjyo_scrolly3[0x0d],
/*TODO*///			senjyo_scrolly3[0x0e],
/*TODO*///			senjyo_scrolly3[0x0f]);
/*TODO*///		ui_text(baf,0,10);
/*TODO*///		sprintf(baf,"%02x %02x %02x %02x %02x %02x %02x %02x",
/*TODO*///			senjyo_scrolly3[0x10],
/*TODO*///			senjyo_scrolly3[0x11],
/*TODO*///			senjyo_scrolly3[0x12],
/*TODO*///			senjyo_scrolly3[0x13],
/*TODO*///			senjyo_scrolly3[0x14],
/*TODO*///			senjyo_scrolly3[0x15],
/*TODO*///			senjyo_scrolly3[0x16],
/*TODO*///			senjyo_scrolly3[0x17]);
/*TODO*///		ui_text(baf,0,20);
/*TODO*///		sprintf(baf,"%02x %02x %02x %02x %02x %02x %02x %02x",
/*TODO*///			senjyo_scrolly3[0x18],
/*TODO*///			senjyo_scrolly3[0x19],
/*TODO*///			senjyo_scrolly3[0x1a],
/*TODO*///			senjyo_scrolly3[0x1b],
/*TODO*///			senjyo_scrolly3[0x1c],
/*TODO*///			senjyo_scrolly3[0x1d],
/*TODO*///			senjyo_scrolly3[0x1e],
/*TODO*///			senjyo_scrolly3[0x1f]);
/*TODO*///		ui_text(baf,0,30);
/*TODO*///	}
/*TODO*///	#endif
	} };
}
