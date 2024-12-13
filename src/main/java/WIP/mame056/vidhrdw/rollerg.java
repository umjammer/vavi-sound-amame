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

public class rollerg
{
	
	
	static int bg_colorbase,sprite_colorbase,zoom_colorbase;
	
	
	
	/***************************************************************************
	
	  Callbacks for the K053245
	
	***************************************************************************/
	
	static K053245_callbackProcPtr sprite_callback = new K053245_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority_mask) {            
		priority_mask[0] = (color[0] & 0x10)!=0 ? 0 : 0x02;
		color[0] = sprite_colorbase + (color[0] & 0x0f);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051316
	
	***************************************************************************/
	
	static K051316_callbackProcPtr zoom_callback = new K051316_callbackProcPtr() {
            public void handler(int[] code, int[] color) {            
		tile_info.flags = TILE_FLIPYX((color[0] & 0xc0) >> 6);
		code[0] |= ((color[0] & 0x0f) << 8);
		color[0] = zoom_colorbase + ((color[0] & 0x30) >> 4);
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr rollerg_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_colorbase = 16;
		sprite_colorbase = 16;
		zoom_colorbase = 0;
	
		if (K053245_vh_start(REGION_GFX1,0, 1, 2, 3, sprite_callback) != 0)
			return 1;
		if (K051316_vh_start_0(REGION_GFX2,4,TILEMAP_TRANSPARENT,0,zoom_callback) != 0)
		{
			K053245_vh_stop();
			return 1;
		}
	
		K051316_set_offset(0, 22, 1);
		return 0;
	} };
	
	public static VhStopPtr rollerg_vh_stop = new VhStopPtr() { public void handler() 
	{
		K053245_vh_stop();
		K051316_vh_stop_0.handler();
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr rollerg_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(priority_bitmap,0,null);
		fillbitmap(bitmap,Machine.pens[16 * bg_colorbase],Machine.visible_area);
		K051316_zoom_draw_0(bitmap,0,1);
		K053245_sprites_draw(bitmap);
	} };
}
