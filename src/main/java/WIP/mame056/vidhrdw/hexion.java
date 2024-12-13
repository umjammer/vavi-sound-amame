/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.palette.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.usrintrf.usrintf_showmessage;

import static arcadeflex056.osdepend.logerror;
import common.subArrays.IntArray;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.cpuintrfH.CLEAR_LINE;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.inptport.*;

import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class hexion
{
	
	
	static UBytePtr[] vram=new UBytePtr[2];
        static UBytePtr unkram=new UBytePtr();
	static int bankctrl,rambank,gfxrom_select;
	static struct_tilemap[] tilemap = new struct_tilemap[2];
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	public static void get_tile_info(int tile_index, UBytePtr ram)
	{
		tile_index *= 4;
		SET_TILE_INFO(
				0,
				ram.read(tile_index) + ((ram.read(tile_index+1) & 0x3f) << 8),
				ram.read(tile_index+2) & 0x0f,
				0);
	}
	
	static GetTileInfoPtr get_tile_info0 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		get_tile_info(tile_index,vram[0]);
            }
        };
	
	static GetTileInfoPtr get_tile_info1 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		get_tile_info(tile_index,vram[1]);
            }
        };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr hexion_vh_start = new VhStartPtr() { public int handler() 
	{
		tilemap[0] = tilemap_create(get_tile_info0,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		tilemap[1] = tilemap_create(get_tile_info1,tilemap_scan_rows,TILEMAP_OPAQUE,     8,8,64,32);
	
		if (tilemap[0]==null || tilemap[1]==null)
			return 1;
	
		tilemap_set_transparent_pen(tilemap[0],0);
		tilemap_set_scrollx(tilemap[1],0,-4);
		tilemap_set_scrolly(tilemap[1],0,4);
	
		vram[0] = new UBytePtr(memory_region(REGION_CPU1), 0x30000);
		vram[1] = new UBytePtr(vram[0], 0x2000);
		unkram = new UBytePtr(vram[1], 0x2000);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr hexion_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1), 0x10000);
	
		/* bits 0-3 select ROM bank */
		cpu_setbank(1, new UBytePtr(rom, 0x2000 * (data & 0x0f)));
	
		/* does bit 6 trigger the 052591? */
		if ((data & 0x40) != 0)
		{
			int bank = unkram.read(0)&1;
			memset(vram[bank],unkram.read(1),0x2000);
			tilemap_mark_all_tiles_dirty(tilemap[bank]);
		}
	
		/* other bits unknown */
	if ((data & 0x30) != 0)
		usrintf_showmessage("bankswitch %02x",data&0xf0);
	
	//logerror("%04x: bankswitch_w %02x\n",cpu_get_pc(),data);
	} };
	
	public static ReadHandlerPtr hexion_bankedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (gfxrom_select!=0 && offset < 0x1000)
		{
			return new UBytePtr(memory_region(REGION_GFX1)).read(((gfxrom_select & 0x7f) << 12) + offset);
		}
		else if (bankctrl == 0)
		{
			return vram[rambank].read(offset);
		}
		else if (bankctrl == 2 && offset < 0x800)
		{
			return unkram.read(offset);
		}
		else
		{
	//logerror("%04x: bankedram_r offset %04x, bankctrl = %02x\n",cpu_get_pc(),offset,bankctrl);
			return 0;
		}
	} };
	
	public static WriteHandlerPtr hexion_bankedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (bankctrl == 3 && offset == 0 && (data & 0xfe) == 0)
		{
	//logerror("%04x: bankedram_w offset %04x, data %02x, bankctrl = %02x\n",cpu_get_pc(),offset,data,bankctrl);
			rambank = data & 1;
		}
		else if (bankctrl == 0)
		{
	//logerror("%04x: bankedram_w offset %04x, data %02x, bankctrl = %02x\n",cpu_get_pc(),offset,data,bankctrl);
			if (vram[rambank].read(offset) != data)
			{
				vram[rambank].write(offset, data);
				tilemap_mark_tile_dirty(tilemap[rambank],offset/4);
			}
		}
		else if (bankctrl == 2 && offset < 0x800)
		{
	//logerror("%04x: unkram_w offset %04x, data %02x, bankctrl = %02x\n",cpu_get_pc(),offset,data,bankctrl);
			unkram.write(offset, data);
		}
/*TODO*///		else
/*TODO*///	logerror("%04x: bankedram_w offset %04x, data %02x, bankctrl = %02x\n",cpu_get_pc(),offset,data,bankctrl);
	} };
	
	public static WriteHandlerPtr hexion_bankctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror("%04x: bankctrl_w %02x\n",cpu_get_pc(),data);
		bankctrl = data;
	} };
	
	public static WriteHandlerPtr hexion_gfxrom_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//logerror("%04x: gfxrom_select_w %02x\n",cpu_get_pc(),data);
		gfxrom_select = data;
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr hexion_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,tilemap[1],0,0);
		tilemap_draw(bitmap,tilemap[0],0,0);
	} };
}
