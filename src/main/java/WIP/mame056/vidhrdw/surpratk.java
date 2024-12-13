/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.konamiic.*;
import static WIP.mame056.vidhrdw.konamiicH.*;
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

public class surpratk
{
	
	static int sprite_colorbase,bg_colorbase;
	static int[] layer_colorbase=new int[3], layerpri=new int[3];
	
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {            
		tile_info.flags = (color[0] & 0x80)!=0 ? TILE_FLIPX : 0;
		code[0] |= ((color[0] & 0x03) << 8) | ((color[0] & 0x10) << 6) | ((color[0] & 0x0c) << 9) | (bank << 13);
		color[0] = layer_colorbase[layer] + ((color[0] & 0x60) >> 5);
            }
        };
        
	/***************************************************************************
	
	  Callbacks for the K053245
	
	***************************************************************************/
	
	static K053245_callbackProcPtr sprite_callback = new K053245_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority_mask) {                
		int pri = 0x20 | ((color[0] & 0x60) >> 2);
		if (pri <= layerpri[2]) priority_mask[0] = 0;
		else if (pri > layerpri[2] && pri <= layerpri[1])	priority_mask[0] = 0xf0;
		else if (pri > layerpri[1] && pri <= layerpri[0])	priority_mask[0] = 0xf0|0xcc;
                else priority_mask[0] = 0xf0|0xcc|0xaa;
	
		color[0] = sprite_colorbase + (color[0] & 0x1f);
            }
        };
        
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr surpratk_vh_start = new VhStartPtr() { public int handler() 
	{
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
		{
			return 1;
		}
		if (K053245_vh_start(REGION_GFX2,0, 1, 2, 3,sprite_callback) != 0)
		{
			K052109_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr surpratk_vh_stop = new VhStopPtr() { public void handler() 
	{
		K052109_vh_stop.handler();
		K053245_vh_stop();
	} };
	
	/* useful function to sort the three tile layers by priority order */
/*TODO*///	static void sortlayers(int *layer,int *pri)
/*TODO*///	{
/*TODO*///	#define SWAP(a,b) \
/*TODO*///		if (pri[a] < pri[b]) \
/*TODO*///		{ \
/*TODO*///			int t; \
/*TODO*///			t = pri[a]; pri[a] = pri[b]; pri[b] = t; \
/*TODO*///			t = layer[a]; layer[a] = layer[b]; layer[b] = t; \
/*TODO*///		}
/*TODO*///	
/*TODO*///		SWAP(0,1)
/*TODO*///		SWAP(0,2)
/*TODO*///		SWAP(1,2)
	/*TODO*///}
	
	public static VhUpdatePtr surpratk_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int[] layer = new int[3];
	
	
		bg_colorbase       = K053251_get_palette_index(K053251_CI0);
		sprite_colorbase   = K053251_get_palette_index(K053251_CI1);
		layer_colorbase[0] = K053251_get_palette_index(K053251_CI2);
		layer_colorbase[1] = K053251_get_palette_index(K053251_CI4);
		layer_colorbase[2] = K053251_get_palette_index(K053251_CI3);
	
		K052109_tilemap_update();
	
		layer[0] = 0;
		layerpri[0] = K053251_get_priority(K053251_CI2);
		layer[1] = 1;
		layerpri[1] = K053251_get_priority(K053251_CI4);
		layer[2] = 2;
		layerpri[2] = K053251_get_priority(K053251_CI3);
	
/*TODO*///		sortlayers(layer,layerpri);
	
		fillbitmap(priority_bitmap,0,null);
		fillbitmap(bitmap,Machine.pens[16 * bg_colorbase],Machine.visible_area);
		K052109_tilemap_draw(bitmap,layer[0],0,1);
		K052109_tilemap_draw(bitmap,layer[1],0,2);
		K052109_tilemap_draw(bitmap,layer[2],0,4);
	
		K053245_sprites_draw(bitmap);
	} };
}
