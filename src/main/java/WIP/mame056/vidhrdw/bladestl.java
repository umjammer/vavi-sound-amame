/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.tilemapH.*;
import static mame056.tilemapC.*;
import static WIP.mame056.drivers.bladestl.bladestl_spritebank;
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

public class bladestl
{
	
	static int[] layer_colorbase = new int[2];
        
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
	
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs] = (char) value;
        }
	
	public static VhConvertColorPromPtr bladestl_vh_convert_color_prom = new VhConvertColorPromPtr() { 
            public void handler(char []palette, char []colortable, UBytePtr color_prom) 
            {
		int i;
		
		/* build the lookup table for sprites. Palette is dynamic. */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable, 1,i, 0x20 + ((color_prom.readinc()) & 0x0f));
	} };
	
	/***************************************************************************
	
	  Callback for the K007342
	
	***************************************************************************/
	
	static K007342_callbackProcPtr tile_callback = new K007342_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
            
		code[0] |= ((color[0] & 0x0f) << 8) | ((color[0] & 0x40) << 6);
		color[0] = layer_colorbase[layer];
            }
        };
	
	/***************************************************************************
	
	  Callback for the K007420
	
	***************************************************************************/
	
	static K007420_callbackProcPtr sprite_callback = new K007420_callbackProcPtr() {
            public void handler(int[] code, int[] color) {
                
		code[0]=( code[0] | ((color[0] & 0xc0) << 2) + bladestl_spritebank);
		code[0]=( (code[0] << 2) | ((color[0] & 0x30) >> 4));
		color[0]=( 0 + (color[0] & 0x0f));
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr bladestl_vh_start = new VhStartPtr() { public int handler() 
	{
		layer_colorbase[0] = 0;
		layer_colorbase[1] = 1;
	
		if (K007342_vh_start(0,tile_callback) != 0)
		{
			return 1;
		}
	
		if (K007420_vh_start(1,sprite_callback) != 0)
		{
			K007420_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr bladestl_vh_stop = new VhStopPtr() { public void handler() 
	{
		K007342_vh_stop.handler();
		K007420_vh_stop.handler();
	} };
	
	/***************************************************************************
	
	  Screen Refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr bladestl_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		K007342_tilemap_update();
	
		K007342_tilemap_draw( bitmap, 1, TILEMAP_IGNORE_TRANSPARENCY, 0);
		K007420_sprites_draw( bitmap );
		K007342_tilemap_draw( bitmap, 1, 1 | TILEMAP_IGNORE_TRANSPARENCY, 0);
		K007342_tilemap_draw( bitmap, 0, 0, 0);
		K007342_tilemap_draw( bitmap, 0, 1, 0);
	} };
}
