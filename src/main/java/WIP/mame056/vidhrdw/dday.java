/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

  Convention: "sl" stands for "searchlight"

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
import static mame056.inputH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.mixer.mixer_sound_enable_global_w;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class dday
{
	
	
	public static UBytePtr dday_bgvideoram=new UBytePtr();
	public static UBytePtr dday_fgvideoram=new UBytePtr();
	public static UBytePtr dday_textvideoram=new UBytePtr();
	public static UBytePtr dday_colorram=new UBytePtr();
	
	static struct_tilemap fg_tilemap, bg_tilemap, text_tilemap, sl_tilemap;
	static mame_bitmap main_bitmap;
	static int control;
	static int sl_image;
	static int sl_enable;
	static timer_entry countdown_timer;
	static int timer_value;
	
	
	
	/* Note: There seems to be no way to reset this timer via hardware.
	         The game uses a difference method to reset it to 99.
	
	  Thanks Zwaxy for the timer info. */
	
	public static ReadHandlerPtr dday_countdown_timer_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return ((timer_value / 10) << 4) | (timer_value % 10);
	} };
	
	static timer_callback countdown_timer_callback = new timer_callback() {
            public void handler(int i) {
                timer_value--;
	
		if (timer_value < 0)
		{
			timer_value = 99;
		}
            }
        };
	
	static void start_countdown_timer()
	{
		timer_value = 0;
	
		countdown_timer = timer_pulse(TIME_IN_SEC(1), 0, countdown_timer_callback);
	}
	
	static void stop_countdown_timer()
	{
		if (countdown_timer!=null)  timer_remove(countdown_timer);
		countdown_timer = null;
	}
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr dday_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] obsolete, char[] colortable, UBytePtr color_prom) {
                int i;
	
	
		palette_set_shadow_factor(1.0/8);	/* this matches the previos version of the driver (>>3) */
	
		for (i = 0; i < Machine.drv.total_colors; i++)
		{
			int bit0,bit1,bit2,bit3,r,g,b;
	
	
			/* red component */
			bit0 = (color_prom.read(i)>> 0) & 0x01;
			bit1 = (color_prom.read(i)>> 1) & 0x01;
			bit2 = (color_prom.read(i)>> 2) & 0x01;
			bit3 = (color_prom.read(i)>> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* green component */
			bit0 = (color_prom.read(i + Machine.drv.total_colors)>> 0) & 0x01;
			bit1 = (color_prom.read(i + Machine.drv.total_colors)>> 1) & 0x01;
			bit2 = (color_prom.read(i + Machine.drv.total_colors)>> 2) & 0x01;
			bit3 = (color_prom.read(i + Machine.drv.total_colors)>> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* blue component */
			bit0 = (color_prom.read(i + 2*Machine.drv.total_colors)>> 0) & 0x01;
			bit1 = (color_prom.read(i + 2*Machine.drv.total_colors)>> 1) & 0x01;
			bit2 = (color_prom.read(i + 2*Machine.drv.total_colors)>> 2) & 0x01;
			bit3 = (color_prom.read(i + 2*Machine.drv.total_colors)>> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			palette_set_color(i,r,g,b);
		}
	
	
		/* HACK!!! This table is handgenerated, but it matches the screenshot.
		   I have no clue how it really works */
	
		colortable[0*8+0] = 0;
		colortable[0*8+1] = 1;
		colortable[0*8+2] = 21;
		colortable[0*8+3] = 2;
		colortable[0*8+4+0] = 0;
		colortable[0*8+4+1] = 1;
		colortable[0*8+4+2] = 21;
		colortable[0*8+4+3] = 2;
	
		colortable[1*8+0] = 4;
		colortable[1*8+1] = 5;
		colortable[1*8+2] = 3;
		colortable[1*8+3] = 7;
		colortable[1*8+4+0] = 4;
		colortable[1*8+4+1] = 5;
		colortable[1*8+4+2] = 3;
		colortable[1*8+4+3] = 7;
	
		colortable[2*8+0] = 8;
		colortable[2*8+1] = 21;
		colortable[2*8+2] = 10;
		colortable[2*8+3] = 3;
		colortable[2*8+4+0] = 8;
		colortable[2*8+4+1] = 21;
		colortable[2*8+4+2] = 10;
		colortable[2*8+4+3] = 3;
	
		colortable[3*8+0] = 8;
		colortable[3*8+1] = 21;
		colortable[3*8+2] = 10;
		colortable[3*8+3] = 3;
		colortable[3*8+4+0] = 8;
		colortable[3*8+4+1] = 21;
		colortable[3*8+4+2] = 10;
		colortable[3*8+4+3] = 3;
	
		colortable[4*8+0] = 16;
		colortable[4*8+1] = 17;
		colortable[4*8+2] = 18;
		colortable[4*8+3] = 7;
		colortable[4*8+4+0] = 16;
		colortable[4*8+4+1] = 17;
		colortable[4*8+4+2] = 18;
		colortable[4*8+4+3] = 7;
	
		colortable[5*8+0] = 29;
		colortable[5*8+1] = 21;
		colortable[5*8+2] = 22;
		colortable[5*8+3] = 27;
		colortable[5*8+4+0] = 29;
		colortable[5*8+4+1] = 21;
		colortable[5*8+4+2] = 22;
		colortable[5*8+4+3] = 27;
	
		colortable[6*8+0] = 29;
		colortable[6*8+1] = 21;
		colortable[6*8+2] = 26;
		colortable[6*8+3] = 27;
		colortable[6*8+4+0] = 29;
		colortable[6*8+4+1] = 21;
		colortable[6*8+4+2] = 26;
		colortable[6*8+4+3] = 27;
	
		colortable[7*8+0] = 29;
		colortable[7*8+1] = 2;
		colortable[7*8+2] = 4;
		colortable[7*8+3] = 27;
		colortable[7*8+4+0] = 29;
		colortable[7*8+4+1] = 2;
		colortable[7*8+4+2] = 4;
		colortable[7*8+4+3] = 27;
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code;
	
		code = dday_bgvideoram.read(tile_index);
		SET_TILE_INFO(0, code, code >> 5, 0);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, flipx;
	
		flipx = dday_colorram.read(tile_index & 0x03e0) & 0x01;
		code = dday_fgvideoram.read(flipx!=0 ? tile_index ^ 0x1f : tile_index);
		SET_TILE_INFO(2, code, code >> 5, flipx!=0 ? TILE_FLIPX : 0);
            }
        };
	
	static GetTileInfoPtr get_text_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code;
	
		code = dday_textvideoram.read(tile_index);
		SET_TILE_INFO(1, code, code >> 5, 0);
            }
        };
	
	static GetTileInfoPtr get_sl_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code, sl_flipx, flipx;
		UBytePtr sl_map;
	
		sl_map = new UBytePtr(memory_region(REGION_USER1), (sl_image & 0x07) * 0x0200);
	
		flipx = (tile_index >> 4) & 0x01;
		sl_flipx = (sl_image >> 3) & 0x01;
	
		/* bit 4 is really a flip indicator.  Need to shift bits 5-9 to the right by 1 */
		tile_index = ((tile_index & 0x03e0) >> 1) | (tile_index & 0x0f);
	
		code = sl_map.read(flipx!=0 ? tile_index ^ 0x0f : tile_index);
	
		if (sl_flipx != flipx)
		{
			if ((code & 0x80) != 0)
			{
				/* no mirroring, draw dark spot */
				code = 1;
			}
		}
	
		SET_TILE_INFO(3, code & 0x3f, 0, flipx!=0 ? TILE_FLIPX : 0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr dday_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap   = tilemap_create(get_bg_tile_info,  tilemap_scan_rows,TILEMAP_SPLIT,8,8,32,32);
		fg_tilemap   = tilemap_create(get_fg_tile_info,  tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		text_tilemap = tilemap_create(get_text_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		sl_tilemap   = tilemap_create(get_sl_tile_info,  tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
	
		main_bitmap = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height);
	
		if (bg_tilemap==null || fg_tilemap==null || text_tilemap==null || sl_tilemap==null || main_bitmap==null)
		{
			dday_vh_stop.handler();
			return 1;
		}
	
		tilemap_set_transmask(bg_tilemap,0,0x00f0,0xff0f); /* pens 0-3 have priority over the foreground layer */
	
		tilemap_set_transparent_pen(fg_tilemap, 0);
	
		tilemap_set_transparent_pen(text_tilemap, 0);
	
		control = 0;
		sl_enable = 0;
		sl_image = 0;
	
		start_countdown_timer();
	
		return 0;
	} };
	
	public static VhStopPtr dday_vh_stop = new VhStopPtr() { public void handler() 
	{
		if (main_bitmap != null)  bitmap_free(main_bitmap);
	
		stop_countdown_timer();
	} };
	
	
	public static WriteHandlerPtr dday_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dday_bgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(bg_tilemap, offset);
	} };
	
	public static WriteHandlerPtr dday_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dday_fgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(fg_tilemap, offset);
		tilemap_mark_tile_dirty(fg_tilemap, offset ^ 0x1f);  /* for flipx case */
	} };
	
	public static WriteHandlerPtr dday_textvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dday_textvideoram.write(offset, data);
		tilemap_mark_tile_dirty(text_tilemap, offset);
	} };
	
	public static WriteHandlerPtr dday_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i;
	
	
		offset &= 0x03e0;
	
	    dday_colorram.write(offset & 0x3e0, data);
	
	    for (i = 0; i < 0x20; i++)
	    {
			tilemap_mark_tile_dirty(fg_tilemap, offset + i);
		}
	} };
	
	public static ReadHandlerPtr dday_colorram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return dday_colorram.read(offset & 0x03e0);
	} };
	
	
	public static WriteHandlerPtr dday_sl_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (sl_image != data)
		{
			sl_image = data;
	
			tilemap_mark_all_tiles_dirty(sl_tilemap);
		}
	} };
	
	
	public static WriteHandlerPtr dday_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//if (data & 0xac)  logerror("Control = %02X\n", data & 0xac);
	
		/* bit 0 is coin counter 1 */
		coin_counter_w.handler(0, data & 0x01);
	
		/* bit 1 is coin counter 2 */
		coin_counter_w.handler(1, data & 0x02);
	
		/* bit 4 is sound enable */
		if ((data & 0x10)==0 && (control & 0x10)!=0)
		{
			AY8910_reset(0);
			AY8910_reset(1);
		}
	
		mixer_sound_enable_global_w(data & 0x10);
	
		/* bit 6 is search light enable */
		sl_enable = data & 0x40;
	
		control = data;
	} };
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr dday_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(main_bitmap,bg_tilemap,TILEMAP_BACK,0);
		tilemap_draw(main_bitmap,fg_tilemap,0,0);
		tilemap_draw(main_bitmap,bg_tilemap,TILEMAP_FRONT,0);
		tilemap_draw(main_bitmap,text_tilemap,0,0);
	
		if (sl_enable != 0)
		{
			/* apply shadow */
	
			mame_bitmap sl_bitmap;
			int x, y;
	
	
			sl_bitmap = tilemap_get_pixmap(sl_tilemap);
	
			for (x = Machine.visible_area.min_x; x <= Machine.visible_area.max_x; x++)
			{
				for (y = Machine.visible_area.min_y; y <= Machine.visible_area.max_y; y++)
				{
					int src_pixel;
	
	
					src_pixel = read_pixel.handler(main_bitmap, x, y);
	
					if (read_pixel.handler(sl_bitmap, x, y) == 255)
					{
						src_pixel += Machine.drv.total_colors;
					}
	
					plot_pixel.handler(bitmap, x, y, src_pixel);
				}
			}
		}
		else
		{
			copybitmap(bitmap,main_bitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	} };
}
