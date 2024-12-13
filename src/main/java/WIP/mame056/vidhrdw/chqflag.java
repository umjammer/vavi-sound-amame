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
import static WIP.mame056.vidhrdw.konamiic.*;
import static WIP.mame056.vidhrdw.konamiicH.*;
import static mame056.palette.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static common.libc.cstring.*;

public class chqflag
{
	
	public static int SPRITEROM_MEM_REGION = REGION_GFX1;
	public static int ZOOMROM0_MEM_REGION  = REGION_GFX2;
	public static int ZOOMROM1_MEM_REGION  = REGION_GFX3;
	
	static int sprite_colorbase;
        static int[] zoom_colorbase = new int[2];
	
	/***************************************************************************
	
	  Callbacks for the K051960
	
	***************************************************************************/
	
	static K051960_callbackProcPtr sprite_callback = new K051960_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority, int[] shadow) {
                priority[0] = (color[0] & 0x10) >> 4;
		color[0] = sprite_colorbase + (color[0] & 0x0f);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051316
	
	***************************************************************************/
	
	static K051316_callbackProcPtr zoom_callback_0 = new K051316_callbackProcPtr() {
            public void handler(int[] code, int[] color) {
                code[0] |= ((color[0] & 0x03) << 8);
		color[0] = zoom_colorbase[0] + ((color[0] & 0x3c) >> 2);
            }
        };
	
	static K051316_callbackProcPtr zoom_callback_1 = new K051316_callbackProcPtr() {
            public void handler(int[] code, int[] color) {
		tile_info.flags = TILE_FLIPYX((color[0] & 0xc0) >> 6);
		code[0] |= ((color[0] & 0x0f) << 8);
		color[0] = zoom_colorbase[1] + ((color[0] & 0x10) >> 4);
            }
        };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr chqflag_vh_start = new VhStartPtr() { public int handler() 
	{
		sprite_colorbase = 0;
		zoom_colorbase[0] = 0x10;
		zoom_colorbase[1] = 0x02;
	
		if (K051960_vh_start(SPRITEROM_MEM_REGION,0, 1, 2, 3,sprite_callback) != 0)
		{
			return 1;
		}
	
		if (K051316_vh_start_0(ZOOMROM0_MEM_REGION,4,TILEMAP_TRANSPARENT,0,zoom_callback_0) != 0)
		{
			K051960_vh_stop.handler();
			return 1;
		}
	
		if (K051316_vh_start_1(ZOOMROM1_MEM_REGION,8,TILEMAP_SPLIT_PENBIT,0xc0,zoom_callback_1) != 0)
		{
			K051960_vh_stop.handler();
			K051316_vh_stop_0.handler();
			return 1;
		}
	
		K051316_set_offset(0,7,0);
		K051316_wraparound_enable(1,1);
	
		return 0;
	} };
	
	public static VhStopPtr chqflag_vh_stop = new VhStopPtr() { public void handler() 
	{
		K051960_vh_stop.handler();
		K051316_vh_stop_0.handler();
		K051316_vh_stop_1.handler();
	} };
	
	/***************************************************************************
	
		Display Refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr chqflag_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		fillbitmap(bitmap,Machine.pens[0],new rectangle(Machine.visible_area));
	
		K051316_zoom_draw_1(bitmap,TILEMAP_BACK,0);
		K051960_sprites_draw(bitmap,0,0);
		K051316_zoom_draw_1(bitmap,TILEMAP_FRONT,0);
		K051960_sprites_draw(bitmap,1,1);
		K051316_zoom_draw_0(bitmap,0,0);
	} };
}
