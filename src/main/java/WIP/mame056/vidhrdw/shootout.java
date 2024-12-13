/*
	Video Hardware for Shoot Out
	prom GB09.K6 may be related to background tile-sprite priority
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.drivers.shootout.shootout_textram;
import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.palette.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.usrintrf.usrintf_showmessage;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.inptport.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class shootout
{
	
	static struct_tilemap background, foreground;
	
	public static VhConvertColorPromPtr shootout_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] obsolete, char[] colortable, UBytePtr color_prom) {
                int i;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2,r,g,b;
	
			/* red component */
			bit0 = (color_prom.read(i)>> 0) & 0x01;
			bit1 = (color_prom.read(i)>> 1) & 0x01;
			bit2 = (color_prom.read(i)>> 2) & 0x01;
			r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (color_prom.read(i)>> 3) & 0x01;
			bit1 = (color_prom.read(i)>> 4) & 0x01;
			bit2 = (color_prom.read(i)>> 5) & 0x01;
			g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read(i)>> 6) & 0x01;
			bit2 = (color_prom.read(i)>> 7) & 0x01;
			b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			palette_set_color(i,r,g,b);
                }
            }
        };
        
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
                public void handler(int tile_index) {
                    int attributes = videoram.read(tile_index+0x400); /* CCCC -TTT */
                    int tile_number = videoram.read(tile_index)+ 256*(attributes&7);
                    int color = attributes>>4;
                    SET_TILE_INFO(
                                    2,
                                    tile_number,
                                    color,
                                    0);
                }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
                public void handler(int tile_index) {
                    int attributes = shootout_textram.read(tile_index+0x400); /* CCCC --TT */
                    int tile_number = shootout_textram.read(tile_index) + 256*(attributes&0x3);
                    int color = attributes>>4;
                    SET_TILE_INFO(
                                    0,
                                    tile_number,
                                    color,
                                    0);
            }
        };
	
	public static WriteHandlerPtr shootout_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		if( videoram.read(offset)!=data ){
			videoram.write(offset,data);
			tilemap_mark_tile_dirty( background, offset&0x3ff );
		}
	} };
	public static WriteHandlerPtr shootout_textram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		if( shootout_textram.read(offset)!=data ){
			shootout_textram.write(offset, data);
			tilemap_mark_tile_dirty( foreground, offset&0x3ff );
		}
	} };
	
	public static VhStartPtr shootout_vh_start = new VhStartPtr() { public int handler() {
		background = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
		foreground = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		if( background!=null && foreground!=null ){
			tilemap_set_transparent_pen( foreground, 0 );
			return 0;
		}
		return 1; /* error */
	} };
        
        static int bFlicker;
	
	static void draw_sprites( mame_bitmap bitmap, int bank_bits ){
		
		GfxElement gfx = Machine.gfx[1];
		rectangle clip = new rectangle(Machine.visible_area);
		UBytePtr source = new UBytePtr(spriteram, 127*4);
		int count;
	
		bFlicker = bFlicker!=0?0:1;
	
		for( count=0; count<128; count++ ){
			int attributes = source.read(1);
			/*
			    76543210
				xxx-----	bank
				---x----	vertical size
				----x---	priority
				-----x--	horizontal flip
				------x-	flicker
				-------x	enable
			*/
			if (( attributes & 0x01 ) != 0){ /* visible */
				if( bFlicker!=0 || (attributes&0x02)==0 ){
					int priority_mask = (attributes&0x08)!=0?0xaa:0;
					int sx = (240 - source.read(2))&0xff;
					int sy = (240 - source.read(0))&0xff;
					int number = source.read(3) | ((attributes<<bank_bits)&0x700);
					int flipx = (attributes & 0x04);
	
					if(( attributes & 0x10 ) != 0){ /* double height */
						number = number&(~1);
						sy -= 16;
						pdrawgfx(bitmap,gfx,
							number,
							0 /*color*/,
							flipx,0 /*flipy*/,
							sx,sy,
							clip,TRANSPARENCY_PEN,0,
							priority_mask);
	
						number++;
						sy += 16;
					}
	
					pdrawgfx(bitmap,gfx,
							number,
							0 /*color*/,
							flipx,0 /*flipy*/,
							sx,sy,
							clip,TRANSPARENCY_PEN,0,
							priority_mask);
					}
			}
			source.dec( 4 );
		}
	}
	
	public static VhUpdatePtr shootout_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(priority_bitmap,0,null);
	
		tilemap_draw(bitmap,background,0,0);
		tilemap_draw(bitmap,foreground,0,1);
		draw_sprites( bitmap,3/*bank bits */ );
	} };
	
	public static VhUpdatePtr shootouj_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(priority_bitmap,0,null);
	
		tilemap_draw(bitmap,background,0,1);
		tilemap_draw(bitmap,foreground,0,2);
		draw_sprites( bitmap,2/*bank bits*/ );
	} };
}
