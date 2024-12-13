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
import static mame056.palette.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static WIP.mame056.vidhrdw.konamiic.*;

public class spy
{
	
	
	static int[] layer_colorbase = new int[3];
        static int sprite_colorbase;
	
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
            
		tile_info.flags = (color[0] & 0x20)!=0 ? TILE_FLIPX : 0;
		code[0]=( code[0] | ((color[0] & 0x03) << 8) | ((color[0] & 0x10) << 6) | ((color[0] & 0x0c) << 9)
				| (bank << 13));
		color[0]=(  layer_colorbase[layer] + ((color[0] & 0xc0) >> 6));
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K051960
	
	***************************************************************************/
	
	static K051960_callbackProcPtr sprite_callback = new K051960_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority, int[] shadow) {                
		/* bit 4 = priority over layer A (0 = have priority) */
		/* bit 5 = priority over layer B (1 = have priority) */
		priority[0]=( (color[0] & 0x30) >> 4 );
		color[0]=( sprite_colorbase + (color[0] & 0x0f) );
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr spy_vh_start = new VhStartPtr() { public int handler() 
	{
		layer_colorbase[0] = 48;
		layer_colorbase[1] = 0;
		layer_colorbase[2] = 16;
		sprite_colorbase = 32;
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
		{
			return 1;
		}
		if (K051960_vh_start(REGION_GFX2,0, 1, 2, 3,sprite_callback) != 0)
		{
			K052109_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr spy_vh_stop = new VhStopPtr() { public void handler() 
	{
		K052109_vh_stop.handler();
/*TODO*///		K051960_vh_stop.handler();
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr spy_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		K052109_tilemap_update();
	
		fillbitmap(bitmap,Machine.pens[16 * layer_colorbase[0]],new rectangle(Machine.visible_area));
		K051960_sprites_draw(bitmap,1,1);	/* are these used? */
		K052109_tilemap_draw(bitmap,1,0,0);
		K051960_sprites_draw(bitmap,0,0);
		K052109_tilemap_draw(bitmap,2,0,0);
		K051960_sprites_draw(bitmap,3,3);	/* are these used? They are supposed to have */
											/* priority over layer B but not layer A. */
		K051960_sprites_draw(bitmap,2,2);
		K052109_tilemap_draw(bitmap,0,0,0);
	} };
}
