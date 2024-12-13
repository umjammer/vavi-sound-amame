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
import static common.libc.cstring.*;

public class blktiger
{
	
	public static UBytePtr blktiger_txvideoram = new UBytePtr();
	
	public static int BGRAM_BANK_SIZE = 0x1000;
	public static int BGRAM_BANKS = 4;
	
	static int blktiger_scroll_bank;
	public static UBytePtr scroll_ram = new UBytePtr();
	static int screen_layout;
	static int chon,objon,bgon;
	
	static struct_tilemap tx_tilemap, bg_tilemap8x4, bg_tilemap4x8;
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr bg8x4_scan = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
                
		/* logical (col,row) . memory offset */
		return (col & 0x0f) + ((row & 0x0f) << 4) + ((col & 0x70) << 4) + ((row & 0x30) << 7);
            }
        };
	
	static GetMemoryOffsetPtr bg4x8_scan = new GetMemoryOffsetPtr() {
            public int handler(int col, int row, int num_cols, int num_rows) {
		/* logical (col,row) . memory offset */
		return (col & 0x0f) + ((row & 0x0f) << 4) + ((col & 0x30) << 4) + ((row & 0x70) << 6);
            }
        };
        
        /* the tile priority table is a guess compiled by looking at the game. It
        was not derived from a PROM so it could be wrong. */
        static int split_table[] =
        {
                3,0,2,2,	/* the fourth could be 1 instead of 2 */
                0,1,0,0,
                0,0,0,0,
                0,0,0,0
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                
		int attr = scroll_ram.read(2*tile_index + 1);
		int color = (attr & 0x78) >> 3;
		SET_TILE_INFO(
				1,
				scroll_ram.read(2*tile_index) + ((attr & 0x07) << 8),
				color,
				TILE_SPLIT(split_table[color]) | ((attr & 0x80) !=0 ? TILE_FLIPX : 0));
            }
        };
	
	static GetTileInfoPtr get_tx_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = blktiger_txvideoram.read(tile_index + 0x400);
		SET_TILE_INFO(
				0,
				blktiger_txvideoram.read(tile_index) + ((attr & 0xe0) << 3),
				attr & 0x1f,
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStopPtr blktiger_vh_stop = new VhStopPtr() { public void handler() 
	{
		scroll_ram = null;
	} };
	
	public static VhStartPtr blktiger_vh_start = new VhStartPtr() { public int handler() 
	{
		scroll_ram = new UBytePtr(BGRAM_BANK_SIZE * BGRAM_BANKS);
	
		tx_tilemap =    tilemap_create(get_tx_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		bg_tilemap8x4 = tilemap_create(get_bg_tile_info,bg8x4_scan,       TILEMAP_SPLIT,   16,16,128,64);
		bg_tilemap4x8 = tilemap_create(get_bg_tile_info,bg4x8_scan,       TILEMAP_SPLIT,   16,16,64,128);
	
		if (scroll_ram==null || tx_tilemap==null || bg_tilemap8x4==null || bg_tilemap4x8==null)
		{
			blktiger_vh_stop.handler();
			return 1;
		}
	
		tilemap_set_transparent_pen(tx_tilemap,3);
	
		tilemap_set_transmask(bg_tilemap8x4,0,0xffff,0x8000);	/* split type 0 is totally transparent in front half */
		tilemap_set_transmask(bg_tilemap8x4,1,0xfff0,0x800f);	/* split type 1 has pens 4-15 transparent in front half */
		tilemap_set_transmask(bg_tilemap8x4,2,0xff00,0x80ff);	/* split type 1 has pens 8-15 transparent in front half */
		tilemap_set_transmask(bg_tilemap8x4,3,0xf000,0x8fff);	/* split type 1 has pens 12-15 transparent in front half */
		tilemap_set_transmask(bg_tilemap4x8,0,0xffff,0x8000);
		tilemap_set_transmask(bg_tilemap4x8,1,0xfff0,0x800f);
		tilemap_set_transmask(bg_tilemap4x8,2,0xff00,0x80ff);
		tilemap_set_transmask(bg_tilemap4x8,3,0xf000,0x8fff);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr blktiger_txvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (blktiger_txvideoram.read(offset) != data)
		{
			blktiger_txvideoram.write(offset, data);
			tilemap_mark_tile_dirty(tx_tilemap,offset & 0x3ff);
		}
	} };
	
	public static ReadHandlerPtr blktiger_bgvideoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return scroll_ram.read(offset + blktiger_scroll_bank);
	} };
	
	public static WriteHandlerPtr blktiger_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		offset += blktiger_scroll_bank;
	
		if (scroll_ram.read(offset) != data)
		{
			scroll_ram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap8x4,offset/2);
			tilemap_mark_tile_dirty(bg_tilemap4x8,offset/2);
		}
	} };
	
	public static WriteHandlerPtr blktiger_bgvideoram_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		blktiger_scroll_bank = (data % BGRAM_BANKS) * BGRAM_BANK_SIZE;
	} };
	
	static int[] scroll = new int[2];
        
	public static WriteHandlerPtr blktiger_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		int scrolly;
	
		scroll[offset] = data;
		scrolly = scroll[0] | (scroll[1] << 8);
		tilemap_set_scrolly(bg_tilemap8x4,0,scrolly);
		tilemap_set_scrolly(bg_tilemap4x8,0,scrolly);
	} };
	
	public static WriteHandlerPtr blktiger_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scrollx;
	
		scroll[offset] = data;
		scrollx = scroll[0] | (scroll[1] << 8);
		tilemap_set_scrollx(bg_tilemap8x4,0,scrollx);
		tilemap_set_scrollx(bg_tilemap4x8,0,scrollx);
	} };
	
	
	public static WriteHandlerPtr blktiger_video_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0 and 1 are coin counters */
		coin_counter_w.handler(0,data & 1);
		coin_counter_w.handler(1,data & 2);
	
		/* bit 5 resets the sound CPU */
		cpu_set_reset_line(1,(data & 0x20)!=0 ? ASSERT_LINE : CLEAR_LINE);
	
		/* bit 6 flips screen */
		flip_screen_set(data & 0x40);
	
		/* bit 7 enables characters? Just a guess */
		chon = ~data & 0x80;
	} };
	
	public static WriteHandlerPtr blktiger_video_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* not sure which is which, but I think that bit 1 and 2 enable background and sprites */
		/* bit 1 enables bg ? */
		bgon = ~data & 0x02;
	
		/* bit 2 enables sprites ? */
		objon = ~data & 0x04;
	} };
	
	public static WriteHandlerPtr blktiger_screen_layout_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		screen_layout = data;
		tilemap_set_enable(bg_tilemap8x4, screen_layout);
		tilemap_set_enable(bg_tilemap4x8,screen_layout!=0?0:1);
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		/* Draw the sprites. */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int attr = buffered_spriteram.read(offs+1);
			int sx = buffered_spriteram.read(offs + 3) - ((attr & 0x10) << 4);
			int sy = buffered_spriteram.read(offs + 2);
			int code = buffered_spriteram.read(offs) | ((attr & 0xe0) << 3);
			int color = attr & 0x07;
			int flipx = attr & 0x08;
	
			if (flip_screen() != 0)
			{
				sx = 240 - sx;
				sy = 240 - sy;
				flipx = flipx!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[2],
					code,
					color,
					flipx,flip_screen(),
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,15);
		}
	}
	
	public static VhUpdatePtr blktiger_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(bitmap,Machine.pens[1023],Machine.visible_area);
	
		if (bgon != 0)
			tilemap_draw(bitmap,screen_layout!=0 ? bg_tilemap8x4 : bg_tilemap4x8,TILEMAP_BACK,0);
	
		if (objon != 0)
			draw_sprites(bitmap);
	
		if (bgon != 0)
			tilemap_draw(bitmap,screen_layout!=0 ? bg_tilemap8x4 : bg_tilemap4x8,TILEMAP_FRONT,0);
	
		if (chon != 0)
			tilemap_draw(bitmap,tx_tilemap,0,0);
	} };
	
	public static VhEofCallbackPtr blktiger_eof_callback = new VhEofCallbackPtr() {
            public void handler() {
            
		buffer_spriteram_w.handler(0,0);
            }
        };
}
