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

public class bottom9
{
	
	
	
	public static int bottom9_video_enable;
	
	static int[] layer_colorbase=new int[3];
        static int sprite_colorbase,zoom_colorbase;
	
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
                code[0] |= (color[0] & 0x3f) << 8;
		color[0] = layer_colorbase[layer] + ((color[0] & 0xc0) >> 6);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051960
	
	***************************************************************************/
	
	static K051960_callbackProcPtr sprite_callback = new K051960_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority, int[] shadow) {
                /* bit 4 = priority over zoom (0 = have priority) */
		/* bit 5 = priority over B (1 = have priority) */
		priority[0] = (color[0] & 0x30) >> 4;
		color[0] = sprite_colorbase + (color[0] & 0x0f);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051316
	
	***************************************************************************/
	
	static K051316_callbackProcPtr zoom_callback = new K051316_callbackProcPtr() {
            public void handler(int[] code, int[] color) {
                tile_info.flags = (color[0] & 0x40)!=0 ? TILE_FLIPX : 0;
		code[0] |= ((color[0] & 0x03) << 8);
		color[0] = zoom_colorbase + ((color[0] & 0x3c) >> 2);
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr bottom9_vh_start = new VhStartPtr() { public int handler() 
	{
		layer_colorbase[0] = 0;	/* not used */
		layer_colorbase[1] = 0;
		layer_colorbase[2] = 16;
		sprite_colorbase = 32;
		zoom_colorbase = 48;
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
		{
			return 1;
		}
		if (K051960_vh_start(REGION_GFX2,0, 1, 2, 3,sprite_callback) != 0)
		{
			K052109_vh_stop.handler();
			return 1;
		}
		if (K051316_vh_start_0(REGION_GFX3,4,TILEMAP_TRANSPARENT,0,zoom_callback) != 0)
		{
			K052109_vh_stop.handler();
			K051960_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr bottom9_vh_stop = new VhStopPtr() { public void handler() 
	{
		K052109_vh_stop.handler();
		K051960_vh_stop.handler();
		K051316_vh_stop_0.handler();
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr bottom9_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		K052109_tilemap_update();
	
		/* note: FIX layer is not used */
		fillbitmap(bitmap,Machine.pens[layer_colorbase[1]],Machine.visible_area);
	//	if (bottom9_video_enable)
		{
			K051960_sprites_draw(bitmap,1,1);
			K051316_zoom_draw_0(bitmap,0,0);
			K051960_sprites_draw(bitmap,0,0);
			K052109_tilemap_draw(bitmap,2,0,0);
			/* note that priority 3 is opposite to the basic layer priority! */
			/* (it IS used, but hopefully has no effect) */
			K051960_sprites_draw(bitmap,2,3);
			K052109_tilemap_draw(bitmap,1,0,0);
		}
	} };
}
