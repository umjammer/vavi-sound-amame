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


public class crimfght
{
	
	
	static int[] layer_colorbase=new int[3];
        static int sprite_colorbase;
	
	/***************************************************************************
	
	  Callbacks for the K052109
	
	***************************************************************************/
	
	static K052109_callbackProcPtr tile_callback = new K052109_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
                tile_info.flags = (color[0] & 0x20)!=0 ? TILE_FLIPX : 0;
		code[0] |= ((color[0] & 0x1f) << 8) | (bank << 13);
		color[0] = layer_colorbase[layer] + ((color[0] & 0xc0) >> 6);
            }
        };
        
	/***************************************************************************
	
	  Callbacks for the K051960
	
	***************************************************************************/
	
	static K051960_callbackProcPtr sprite_callback = new K051960_callbackProcPtr() {
            public void handler(int[] code, int[] color, int[] priority, int[] shadow) {
                /* Weird priority scheme. Why use three bits when two would suffice? */
		/* The PROM allows for mixed priorities, where sprites would have */
		/* priority over text but not on one or both of the other two planes. */
		/* Luckily, this isn't used by the game. */
		switch (color[0] & 0x70)
		{
			case 0x10: priority[0] = 0; break;
			case 0x00: priority[0] = 1; break;
			case 0x40: priority[0] = 2; break;
			case 0x20: priority[0] = 3; break;
			/*   0x60 == 0x20 */
			/*   0x50 priority over F and A, but not over B */
			/*   0x30 priority over F, but not over A and B */
			/*   0x70 == 0x30 */
		}
		/* bit 7 is on in the "Game Over" sprites, meaning unknown */
		/* in Aliens it is the top bit of the code, but that's not needed here */
		color[0] = sprite_colorbase + (color[0] & 0x0f);
            }
        };
        
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStopPtr crimfght_vh_stop = new VhStopPtr() { public void handler() 
	{
		paletteram = null;
		K052109_vh_stop.handler();
		K051960_vh_stop.handler();
	} };
	
	public static VhStartPtr crimfght_vh_start = new VhStartPtr() { public int handler() 
	{
		paletteram = new UBytePtr(0x400);
		if (paletteram == null) return 1;
	
		layer_colorbase[0] = 0;
		layer_colorbase[1] = 4;
		layer_colorbase[2] = 8;
		sprite_colorbase = 16;
		if (K052109_vh_start(REGION_GFX1,0, 1, 2, 3,tile_callback) != 0)
		{
			paletteram = null;
			return 1;
		}
		if (K051960_vh_start(REGION_GFX2,0, 1, 2, 3,sprite_callback) != 0)
		{
			paletteram = null;
			K052109_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr crimfght_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		K052109_tilemap_update();
	
		K052109_tilemap_draw(bitmap,1,TILEMAP_IGNORE_TRANSPARENCY,0);
		K051960_sprites_draw(bitmap,2,2);
		K052109_tilemap_draw(bitmap,2,0,0);
		K051960_sprites_draw(bitmap,1,1);
		K052109_tilemap_draw(bitmap,0,0,0);
		K051960_sprites_draw(bitmap,0,0);
	} };
}
