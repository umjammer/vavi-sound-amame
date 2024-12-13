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

public class vendetta
{
	
	
	static int[] layer_colorbase=new int[3];
        static int bg_colorbase,sprite_colorbase;
	static int[] layerpri=new int[3];
	
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
                code[0] |= ((color[0] & 0x03) << 8) | ((color[0] & 0x30) << 6) |
				((color[0] & 0x0c) << 10) | (bank << 14);
		color[0] = layer_colorbase[layer] + ((color[0] & 0xc0) >> 6);
            }
        };
	
	
	/***************************************************************************
	
	  Callbacks for the K053247
	
	***************************************************************************/
	
	static K053247_callbackProcPtr sprite_callback = new K053247_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority_mask) {
                int pri = (color[0] & 0x03e0) >> 4;	/* ??????? */
		if (pri <= layerpri[2]) priority_mask[0] = 0;
		else if (pri > layerpri[2] && pri <= layerpri[1])	priority_mask[0] = 0xf0;
		else if (pri > layerpri[1] && pri <= layerpri[0])	priority_mask[0] = 0xf0|0xcc;
		else 							priority_mask[0] = 0xf0|0xcc|0xaa;
	
		color[0] = sprite_colorbase + (color[0] & 0x001f);
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr vendetta_vh_start = new VhStartPtr() { public int handler() 
	{
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
			return 1;
		if (K053247_vh_start(REGION_GFX2,53,6,0, 1, 2, 3,sprite_callback) != 0)
		{
			K052109_vh_stop.handler();
			return 1;
		}
		return 0;
	} };
	
	public static VhStopPtr vendetta_vh_stop = new VhStopPtr() { public void handler() 
	{
		K052109_vh_stop.handler();
		K053247_vh_stop();
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr vendetta_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int[] layer = new int[3];
	
	
		bg_colorbase       = K053251_get_palette_index(K053251_CI0);
		sprite_colorbase   = K053251_get_palette_index(K053251_CI1);
		layer_colorbase[0] = K053251_get_palette_index(K053251_CI2);
		layer_colorbase[1] = K053251_get_palette_index(K053251_CI3);
		layer_colorbase[2] = K053251_get_palette_index(K053251_CI4);
	
		K052109_tilemap_update();
	
		layer[0] = 0;
		layerpri[0] = K053251_get_priority(K053251_CI2);
		layer[1] = 1;
		layerpri[1] = K053251_get_priority(K053251_CI3);
		layer[2] = 2;
		layerpri[2] = K053251_get_priority(K053251_CI4);
	
		if (layerpri[0] < layerpri[1]) 
		{ 
			int t; 
			t = layerpri[0]; layerpri[0] = layerpri[1]; layerpri[1] = t; 
			t = layer[0]; layer[0] = layer[1]; layer[1] = t; 
		}
                if (layerpri[0] < layerpri[2]) 
		{ 
			int t; 
			t = layerpri[0]; layerpri[0] = layerpri[2]; layerpri[2] = t; 
			t = layer[0]; layer[0] = layer[2]; layer[2] = t; 
		}
                if (layerpri[1] < layerpri[2]) 
		{ 
			int t; 
			t = layerpri[1]; layerpri[1] = layerpri[2]; layerpri[2] = t; 
			t = layer[1]; layer[1] = layer[2]; layer[2] = t; 
		}
	
		fillbitmap(priority_bitmap,0,null);
		K052109_tilemap_draw(bitmap,layer[0],TILEMAP_IGNORE_TRANSPARENCY,1);
		K052109_tilemap_draw(bitmap,layer[1],0,2);
		K052109_tilemap_draw(bitmap,layer[2],0,4);
	
		K053247_sprites_draw(bitmap);
	} };
}
