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

public class rockrage
{
	
	static int[] layer_colorbase=new int[2];
	static int rockrage_vreg;
        
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char []colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
	
	public static VhConvertColorPromPtr rockrage_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		
		/* build the lookup table for sprites. Palette is dynamic. */
		for (i = 0;i < TOTAL_COLORS(0)/2; i++){
			COLOR(colortable,0,i, 0x00 + (color_prom.read(i)& 0x0f));
			COLOR(colortable,0,(TOTAL_COLORS(0)/2)+i, 0x10 + (color_prom.read(0x100+i)& 0x0f));
		}
	} };
	
	/***************************************************************************
	
	  Callback for the K007342
	
	***************************************************************************/
	
	static K007342_callbackProcPtr tile_callback = new K007342_callbackProcPtr() {
            public void handler(int layer, int bank, int[] code, int[] color) {
            
		if (layer == 1)
			code[0]=( code[0] | ((color[0] & 0x40) << 2) | ((bank & 0x01) << 9));
		else
			code[0]=( code[0] | ((color[0] & 0x40) << 2) | ((bank & 0x03) << 10) | ((rockrage_vreg & 0x04) << 7) | ((rockrage_vreg & 0x08) << 9));
		color[0]=( layer_colorbase[layer] + (color[0] & 0x0f));
            }
        };
	
	/***************************************************************************
	
	  Callback for the K007420
	
	***************************************************************************/
	
	static K007420_callbackProcPtr _sprite_callback = new K007420_callbackProcPtr() {
                public void handler(int[] code, int[] color) {
                
		code[0]=( code[0] | ((color[0] & 0x40) << 2) | ((color[0] & 0x80) << 1)*(rockrage_vreg << 1));
		code[0]=( (code[0] << 2) | ((color[0] & 0x30) >> 4) );
		color[0]=( 0 );
            }
        };
	
	
	public static WriteHandlerPtr rockrage_vreg_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		/* bits 4-7: unused */
		/* bit 3: bit 4 of bank # (layer 0) */
		/* bit 2: bit 1 of bank # (layer 0) */
		/* bits 0-1: sprite bank select */
	
		if ((data & 0x0c) != (rockrage_vreg & 0x0c))
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
	
		rockrage_vreg = data;
	} };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr rockrage_vh_start = new VhStartPtr() { public int handler() 
	{
		layer_colorbase[0] = 0x00;
		layer_colorbase[1] = 0x10;
	
		if (K007342_vh_start(0,tile_callback) != 0)
		{
			return 1;
		}
	
		if (K007420_vh_start(1,_sprite_callback) != 0)
		{
			K007420_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr rockrage_vh_stop = new VhStopPtr() { public void handler() 
	{
		K007342_vh_stop.handler();
		K007420_vh_stop.handler();
	} };
	
	/***************************************************************************
	
	  Screen Refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr rockrage_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		K007342_tilemap_update();
	
		K007342_tilemap_draw( bitmap, 0, TILEMAP_IGNORE_TRANSPARENCY, 0);
		K007420_sprites_draw( bitmap );
		K007342_tilemap_draw( bitmap, 0, 1 | TILEMAP_IGNORE_TRANSPARENCY, 0);
		K007342_tilemap_draw( bitmap, 1, 0, 0);
		K007342_tilemap_draw( bitmap, 1, 1, 0);
	} };
}
