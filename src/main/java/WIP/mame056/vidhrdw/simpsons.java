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

public class simpsons
{
	
	static int bg_colorbase,sprite_colorbase;
        static int[] layer_colorbase = new int[3];
	public static UBytePtr simpsons_xtraram=new UBytePtr();
	static int[] layerpri = new int[3];
	
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
                
		code[0] |= ((color[0] & 0x3f) << 8) | (bank << 14);
		color[0] = layer_colorbase[layer] + ((color[0] & 0xc0) >> 6);
            }
        };
	
	/***************************************************************************
	
	  Callbacks for the K053247
	
	***************************************************************************/
	
	public static K053247_callbackProcPtr sprite_callback = new K053247_callbackProcPtr() { 
            public void handler(int[] code,int[] color,int[] priority_mask){
            
		priority_mask[0] = (color[0] & 0x0f80) >> 6;	/* ??????? */
		color[0] = sprite_colorbase + (color[0] & 0x001f);
            }
        };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr simpsons_vh_start = new VhStartPtr() { public int handler() 
	{
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
			return 1;
		if (K053247_vh_start(REGION_GFX2,53,23,0, 1, 2, 3,sprite_callback) != 0)
		{
			K052109_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr simpsons_vh_stop = new VhStopPtr() { public void handler() 
	{
		K052109_vh_stop.handler();
		K053247_vh_stop();
	} };
	
	/***************************************************************************
	
	  Extra video banking
	
	***************************************************************************/
	
	public static ReadHandlerPtr simpsons_K052109_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return K052109_r.handler(offset + 0x2000);
	} };
	
	public static WriteHandlerPtr simpsons_K052109_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		K052109_w.handler(offset + 0x2000,data);
	} };
	
	public static ReadHandlerPtr simpsons_K053247_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (offset < 0x1000) return K053247_r.handler(offset);
		else return simpsons_xtraram.read(offset - 0x1000);
	} };
	
	public static WriteHandlerPtr simpsons_K053247_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset < 0x1000) K053247_w.handler(offset,data);
		else simpsons_xtraram.write(offset - 0x1000, data);
	} };
	
	public static void simpsons_video_banking(int bank)
	{
		if ((bank & 1) != 0)
		{
			memory_set_bankhandler_r(3,0,paletteram_r);
			memory_set_bankhandler_w(3,0,paletteram_xBBBBBGGGGGRRRRR_swap_w);
		}
		else
		{
			memory_set_bankhandler_r(3,0,K052109_r);
			memory_set_bankhandler_w(3,0,K052109_w);
		}
	
		if ((bank & 2) != 0)
		{
			memory_set_bankhandler_r(4,0,simpsons_K053247_r);
			memory_set_bankhandler_w(4,0,simpsons_K053247_w);
		}
		else
		{
			memory_set_bankhandler_r(4,0,simpsons_K052109_r);
			memory_set_bankhandler_w(4,0,simpsons_K052109_w);
		}
	}
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	
	public static VhUpdatePtr simpsons_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
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
	
/*TODO*///		sortlayers(layer,layerpri);
/*                if (pri[a] < pri[b])
		{
			int t;
			t = pri[a]; pri[a] = pri[b]; pri[b] = t;
			t = layer[a]; layer[a] = layer[b]; layer[b] = t;
		}
	
		SWAP(0,1)
		SWAP(0,2)
		SWAP(1,2)
*/
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
		fillbitmap(bitmap,Machine.pens[16 * bg_colorbase],Machine.visible_area);
		K052109_tilemap_draw(bitmap,layer[0],0,1);
		K052109_tilemap_draw(bitmap,layer[1],0,2);
		K052109_tilemap_draw(bitmap,layer[2],0,4);
	
		K053247_sprites_draw(bitmap);
	} };
}
