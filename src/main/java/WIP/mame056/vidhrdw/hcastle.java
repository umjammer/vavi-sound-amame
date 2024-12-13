/***************************************************************************

	Haunted Castle video emulation

***************************************************************************/

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


public class hcastle
{
	
	public static UBytePtr hcastle_pf1_videoram = new UBytePtr(), hcastle_pf2_videoram=new UBytePtr();
	static int gfx_bank;
	
	static struct_tilemap fg_tilemap, bg_tilemap;
	static int pf2_bankbase,pf1_bankbase;
	
	
	public static VhConvertColorPromPtr hcastle_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i,chip,pal,clut;
                
                int _colortable = 0;
	
		for (chip = 0;chip < 2;chip++)
		{
			for (pal = 0;pal < 8;pal++)
			{
				clut = (pal & 1) + 2 * chip;
				for (i = 0;i < 256;i++)
				{
					if ((pal & 1) == 0)	/* sprites */
					{
						if (color_prom.read(256 * clut + i) == 0){
							colortable[_colortable++] = 0;
                                                } else {                                                
							colortable[_colortable++] = (char) (16 * pal + color_prom.read(256 * clut + i));
                                                }
					}
					else
						colortable[_colortable++] = (char) (16 * pal + color_prom.read(256 * clut + i));
				}
			}
		}
	} };
	
	
	
	
	/***************************************************************************
	
		Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr tilemap_scan = new GetMemoryOffsetPtr() {
            public int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows) {            
		/* logical (col,row) -> memory offset */
		return (u32_col & 0x1f) + ((u32_row & 0x1f) << 5) + ((u32_col & 0x20) << 6);	/* skip 0x400 */
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            
            public void handler(int tile_index) {
            
		int bit0 = (K007121_ctrlram[0][0x05] >> 0) & 0x03;
		int bit1 = (K007121_ctrlram[0][0x05] >> 2) & 0x03;
		int bit2 = (K007121_ctrlram[0][0x05] >> 4) & 0x03;
		int bit3 = (K007121_ctrlram[0][0x05] >> 6) & 0x03;
		int attr = hcastle_pf1_videoram.read(tile_index);
		int tile = hcastle_pf1_videoram.read(tile_index + 0x400);
		int color = attr & 0x7;
		int bank =  ((attr & 0x80) >> 7) |
					((attr >> (bit0+2)) & 0x02) |
					((attr >> (bit1+1)) & 0x04) |
					((attr >> (bit2  )) & 0x08) |
					((attr >> (bit3-1)) & 0x10);
	
		SET_TILE_INFO(
				0,
				tile + bank*0x100 + pf1_bankbase,
				((K007121_ctrlram[0][6]&0x30)*2+16) + color,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            
            public void handler(int tile_index) {
		int bit0 = (K007121_ctrlram[1][0x05] >> 0) & 0x03;
		int bit1 = (K007121_ctrlram[1][0x05] >> 2) & 0x03;
		int bit2 = (K007121_ctrlram[1][0x05] >> 4) & 0x03;
		int bit3 = (K007121_ctrlram[1][0x05] >> 6) & 0x03;
		int attr = hcastle_pf2_videoram.read(tile_index);
		int tile = hcastle_pf2_videoram.read(tile_index + 0x400);
		int color = attr & 0x7;
		int bank =  ((attr & 0x80) >> 7) |
					((attr >> (bit0+2)) & 0x02) |
					((attr >> (bit1+1)) & 0x04) |
					((attr >> (bit2  )) & 0x08) |
					((attr >> (bit3-1)) & 0x10);
	
		SET_TILE_INFO(
				1,
				tile + bank*0x100 + pf2_bankbase,
				((K007121_ctrlram[1][6]&0x30)*2+16) + color,
				0);
            }
        };
	
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr hcastle_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan,TILEMAP_TRANSPARENT,8,8,64,32);
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan,TILEMAP_OPAQUE,     8,8,64,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		tilemap_set_transparent_pen(fg_tilemap,0);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
		Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr hcastle_pf1_video_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (hcastle_pf1_videoram.read(offset) != data)
		{
			hcastle_pf1_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset & 0xbff);
		}
	} };
	
	public static WriteHandlerPtr hcastle_pf2_video_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (hcastle_pf2_videoram.read(offset) != data)
		{
			hcastle_pf2_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset & 0xbff);
		}
	} };
	
	public static WriteHandlerPtr hcastle_gfxbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		gfx_bank = data;
	} };
	
	public static ReadHandlerPtr hcastle_gfxbank_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return gfx_bank;
	} };
	
	public static WriteHandlerPtr hcastle_pf1_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset==3)
		{
			if ((data&0x8)==0)
				buffer_spriteram(new UBytePtr(spriteram, 0x800),0x800);
			else
				buffer_spriteram(new UBytePtr(spriteram),0x800);
		}
		K007121_ctrl_0_w.handler(offset,data);
	} };
	
	public static WriteHandlerPtr hcastle_pf2_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset==3)
		{
			if ((data&0x8)==0)
				buffer_spriteram_2(new UBytePtr(spriteram_2, 0x800),0x800);
			else
				buffer_spriteram_2(new UBytePtr(spriteram_2),0x800);
		}
		K007121_ctrl_1_w.handler(offset,data);
	} };
	
	/*****************************************************************************/
	
	static void draw_sprites( mame_bitmap bitmap, UBytePtr sbank, int bank )
	{
		int bank_base = (bank == 0) ? 0x4000 * (gfx_bank & 1) : 0;
		K007121_sprites_draw(bank,bitmap,new UBytePtr(sbank),(K007121_ctrlram[bank][6]&0x30)*2,0,bank_base,-1);
	}
	
	/*****************************************************************************/
        
        static int old_pf1,old_pf2;
	
	public static VhUpdatePtr hcastle_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		
		pf1_bankbase = 0x0000;
		pf2_bankbase = 0x4000 * ((gfx_bank & 2) >> 1);
	
		if ((K007121_ctrlram[0][3] & 0x01)!=0) pf1_bankbase += 0x2000;
		if ((K007121_ctrlram[1][3] & 0x01)!=0) pf2_bankbase += 0x2000;
	
		if (pf1_bankbase != old_pf1)
			tilemap_mark_all_tiles_dirty(fg_tilemap);
	
		if (pf2_bankbase != old_pf2)
			tilemap_mark_all_tiles_dirty(bg_tilemap);
	
		old_pf1 = pf1_bankbase;
		old_pf2 = pf2_bankbase;
	
		tilemap_set_scrolly(bg_tilemap,0,K007121_ctrlram[1][2]);
		tilemap_set_scrollx(bg_tilemap,0,((K007121_ctrlram[1][1]<<8)+K007121_ctrlram[1][0]));
		tilemap_set_scrolly(fg_tilemap,0,K007121_ctrlram[0][2]);
		tilemap_set_scrollx(fg_tilemap,0,((K007121_ctrlram[0][1]<<8)+K007121_ctrlram[0][0]));
	
	//	/* Sprite priority */
	//	if (K007121_ctrlram[0][3]&0x20)
		if ((gfx_bank & 0x04) == 0)
		{
			tilemap_draw(bitmap,bg_tilemap,0,0);
			draw_sprites( bitmap, buffered_spriteram, 0 );
			draw_sprites( bitmap, buffered_spriteram_2, 1 );
			tilemap_draw(bitmap,fg_tilemap,0,0);
		}
		else
		{
			tilemap_draw(bitmap,bg_tilemap,0,0);
			tilemap_draw(bitmap,fg_tilemap,0,0);
			draw_sprites( bitmap, buffered_spriteram, 0 );
			draw_sprites( bitmap, buffered_spriteram_2, 1 );
		}
	} };
}
