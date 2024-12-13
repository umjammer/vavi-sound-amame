/***************************************************************************

  Functions to emulate the video hardware of the machine.

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

public class flkatck
{
	
	static struct_tilemap[] k007121_tilemap = new struct_tilemap[2];
	
	public static UBytePtr k007121_ram = new UBytePtr();
	
	public static int flkatck_irq_enabled;
	
	public static int k007121_flip_screen = 0;
	
	/***************************************************************************
	
	  Callbacks for the K007121
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info_A = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int attr = k007121_ram.read(tile_index);
		int code = k007121_ram.read(tile_index+0x400);
		int bit0 = (K007121_ctrlram[0][0x05] >> 0) & 0x03;
		int bit1 = (K007121_ctrlram[0][0x05] >> 2) & 0x03;
		int bit2 = (K007121_ctrlram[0][0x05] >> 4) & 0x03;
		int bit3 = (K007121_ctrlram[0][0x05] >> 6) & 0x03;
		int bank = ((attr & 0x80) >> 7) |
				((attr >> (bit0+2)) & 0x02) |
				((attr >> (bit1+1)) & 0x04) |
				((attr >> (bit2  )) & 0x08) |
				((attr >> (bit3-1)) & 0x10) |
				((K007121_ctrlram[0][0x03] & 0x01) << 5);
		int mask = (K007121_ctrlram[0][0x04] & 0xf0) >> 4;
	
		bank = (bank & ~(mask << 1)) | ((K007121_ctrlram[0][0x04] & mask) << 1);
	
		if ((attr == 0x0d) && ((K007121_ctrlram[0][0]==0)) && ((K007121_ctrlram[0][2]==0)))
			bank = 0;	/*	this allows the game to print text
						in all banks selected by the k007121 */
	
		SET_TILE_INFO(
				0,
				code + 256*bank,
				(attr & 0x0f) + 16,
				(attr & 0x20)!=0 ? TILE_FLIPY : 0);
            }
        };
	
	static GetTileInfoPtr get_tile_info_B = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = k007121_ram.read(tile_index+0x800);
		int code = k007121_ram.read(tile_index+0xc00);
	
		SET_TILE_INFO(
				0,
				code,
				(attr & 0x0f) + 16,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr flkatck_vh_start = new VhStartPtr() { public int handler() 
	{
		k007121_tilemap[0] = tilemap_create(get_tile_info_A,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
		k007121_tilemap[1] = tilemap_create(get_tile_info_B,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
	
		if (k007121_tilemap[0]==null || k007121_tilemap[1]==null)
			return 1;
	
		{
			rectangle clip = new rectangle(Machine.visible_area);
			clip.min_x += 40;
			tilemap_set_clip(k007121_tilemap[0],clip);
	
			clip.max_x = 39;
			clip.min_x = 0;
			tilemap_set_clip(k007121_tilemap[1],clip);
	
			return 0;
		}
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr flkatck_k007121_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset < 0x1000){	/* tiles */
			if (k007121_ram.read(offset) != data)
			{
				k007121_ram.write(offset, data);
				if ((offset & 0x800) != 0)	/* score */
					tilemap_mark_tile_dirty(k007121_tilemap[1],offset & 0x3ff);
				else
					tilemap_mark_tile_dirty(k007121_tilemap[0],offset & 0x3ff);
			}
		}
		else	/* sprites */
			k007121_ram.write(offset, data);
	} };
	
	public static WriteHandlerPtr flkatck_k007121_regs_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset)
		{
			case 0x04:	/* ROM bank select */
				if (data != K007121_ctrlram[0][0x04])
					tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
				break;
	
			case 0x07:	/* flip screen + IRQ control */
				k007121_flip_screen = data & 0x08;
				tilemap_set_flip(ALL_TILEMAPS, k007121_flip_screen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
				flkatck_irq_enabled = data & 0x02;
				break;
		}
	
		K007121_ctrl_0_w.handler(offset,data);
	} };
	
	
	/***************************************************************************
	
		Display Refresh
	
	***************************************************************************/
	
	/***************************************************************************
	
		Flack Attack sprites. Each sprite has 16 bytes!:
	
	
	***************************************************************************/
	
	public static VhUpdatePtr flkatck_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
/*TODO*///	#if 0
/*TODO*///	usrintf_showmessage("%02x-%02x-%02x-%02x-%02x-%02x-%02x-%02x  %02x-%02x-%02x-%02x-%02x-%02x-%02x-%02x",
/*TODO*///		K007121_ctrlram[0][0x00],K007121_ctrlram[0][0x01],K007121_ctrlram[0][0x02],K007121_ctrlram[0][0x03],K007121_ctrlram[0][0x04],K007121_ctrlram[0][0x05],K007121_ctrlram[0][0x06],K007121_ctrlram[0][0x07],
/*TODO*///		K007121_ctrlram[1][0x00],K007121_ctrlram[1][0x01],K007121_ctrlram[1][0x02],K007121_ctrlram[1][0x03],K007121_ctrlram[1][0x04],K007121_ctrlram[1][0x05],K007121_ctrlram[1][0x06],K007121_ctrlram[1][0x07]);
/*TODO*///	#endif
	
		/* set scroll registers */
		tilemap_set_scrollx(k007121_tilemap[0],0,K007121_ctrlram[0][0x00] - 40);
		tilemap_set_scrolly(k007121_tilemap[0],0,K007121_ctrlram[0][0x02]);
	
		/* draw the graphics */
		tilemap_draw(bitmap,k007121_tilemap[0],0,0);
		K007121_sprites_draw(0,bitmap,new UBytePtr(k007121_ram, 0x1000),0,40,0,-1);
		tilemap_draw(bitmap,k007121_tilemap[1],0,0);
	} };
}
