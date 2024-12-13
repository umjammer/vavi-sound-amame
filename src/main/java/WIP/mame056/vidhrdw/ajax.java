/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.konamiic.*;
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

public class ajax
{
	
	
	public static int ajax_priority;
	static int[] layer_colorbase = new int[3];
        static int sprite_colorbase,zoom_colorbase;
	
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {            
		code[0] |= ((color[0] & 0x0f) << 8) | (bank << 12);
		color[0] = layer_colorbase[layer] + ((color[0] & 0xf0) >> 4);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051960
	
	***************************************************************************/
	
	static K051960_callbackProcPtr sprite_callback = new K051960_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority, int[] shadow) {
            
		/* priority bits:
		   4 over zoom (0 = have priority)
		   5 over B    (0 = have priority)
		   6 over A    (1 = have priority)
		   never over F
		*/
		priority[0] = 0xff00;							/* F = 8 */
		if (( color[0] & 0x10)!=0) priority[0] |= 0xf0f0;	/* Z = 4 */
		if ((~color[0] & 0x40)!=0) priority[0] |= 0xcccc;	/* A = 2 */
		if (( color[0] & 0x20)!=0) priority[0] |= 0xaaaa;	/* B = 1 */
		color[0] = sprite_colorbase + (color[0] & 0x0f);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051316
	
	***************************************************************************/
	
	static K051316_callbackProcPtr zoom_callback = new K051316_callbackProcPtr() {
            public void handler(int[] code, int[] color) {            
		code[0] |= ((color[0] & 0x07) << 8);
		color[0] = zoom_colorbase + ((color[0] & 0x08) >> 3);
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr ajax_vh_start = new VhStartPtr() { public int handler() 
	{
		layer_colorbase[0] = 64;
		layer_colorbase[1] = 0;
		layer_colorbase[2] = 32;
		sprite_colorbase = 16;
		zoom_colorbase = 6;	/* == 48 since it's 7-bit graphics */
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
			return 1;
		if (K051960_vh_start(REGION_GFX2,0, 1, 2, 3,sprite_callback) != 0)
		{
			K052109_vh_stop.handler();
			return 1;
		}
		if (K051316_vh_start_0(REGION_GFX3,7,TILEMAP_TRANSPARENT,0,zoom_callback) != 0)
		{
			K052109_vh_stop.handler();
			K051960_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr ajax_vh_stop = new VhStopPtr() { public void handler() 
	{
		K052109_vh_stop.handler();
		K051960_vh_stop.handler();
		K051316_vh_stop_0.handler();
	} };
	
	
	
	/***************************************************************************
	
		Display Refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr ajax_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		K052109_tilemap_update();
	
		fillbitmap(priority_bitmap,0,null);
	
		fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
		K052109_tilemap_draw(bitmap,2,0,1);
		if (ajax_priority != 0)
		{
			/* basic layer order is B, zoom, A, F */
			K051316_zoom_draw_0(bitmap,0,4);
			K052109_tilemap_draw(bitmap,1,0,2);
		}
		else
		{
			/* basic layer order is B, A, zoom, F */
			K052109_tilemap_draw(bitmap,1,0,2);
			K051316_zoom_draw_0(bitmap,0,4);
		}
		K052109_tilemap_draw(bitmap,0,0,8);
	
		K051960_sprites_draw(bitmap,-1,-1);
	} };
}
