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

public class battlnts
{
	
	static int spritebank;
	
	static int[] layer_colorbase = new int[2];
	
	/***************************************************************************
	
	  Callback for the K007342
	
	***************************************************************************/
	
	static K007342_callbackProcPtr tile_callback = new K007342_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {                
		code[0] |= ((color[0] & 0x0f) << 9) | ((color[0] & 0x40) << 2);
		color[0] = layer_colorbase[layer];
            }
        };
	
	/***************************************************************************
	
	  Callback for the K007420
	
	***************************************************************************/
	
	static K007420_callbackProcPtr sprite_callback = new K007420_callbackProcPtr() {
            public void handler(int[] code, int[] color) {
            	code[0]=( code[0] | ((color[0] & 0xc0) << 2) | spritebank );
		code[0]=((code[0] << 2) | ((color[0] & 0x30) >> 4));
		color[0]=( 0 );
            }
        };
	
	public static WriteHandlerPtr battlnts_spritebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		spritebank = 1024 * (data & 1);
	} };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr battlnts_vh_start = new VhStartPtr() { public int handler() 
	{
		layer_colorbase[0] = 0;
		layer_colorbase[1] = 0;
	
		if (K007342_vh_start(0,tile_callback) != 0)
		{
			/* Battlantis use this as Work RAM */
			//K007342_tilemap_set_enable(1, 0);
                        tilemap_set_enable(K007342_tilemap[1], 0);
			return 1;
		}
	
		if (K007420_vh_start(1,sprite_callback) != 0)
		{
			K007420_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr battlnts_vh_stop = new VhStopPtr() { public void handler() 
	{
		K007342_vh_stop.handler();
		K007420_vh_stop.handler();
	} };
	
	/***************************************************************************
	
	  Screen Refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr battlnts_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) {
	
		K007342_tilemap_update();
	
		K007342_tilemap_draw( bitmap, 0, TILEMAP_IGNORE_TRANSPARENCY, 0);
		K007420_sprites_draw( bitmap );
		K007342_tilemap_draw( bitmap, 0, 1 | TILEMAP_IGNORE_TRANSPARENCY, 0);
	} };
}
