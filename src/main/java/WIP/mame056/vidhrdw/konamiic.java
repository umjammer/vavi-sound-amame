/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static mame056.usrintrf.usrintf_showmessage;
import static WIP.mame056.vidhrdw.konamiicH.*;
import static common.ptr.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.*;
import static common.subArrays.*;
import static java.lang.System.exit;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuintrfH.*;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.palette.*;
import static mame056.paletteH.*;
import static mame056.mame.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.VIDEO_HAS_SHADOWS;
import static mame056.mameH.MAX_GFX_ELEMENTS;
import static arcadeflex056.osdepend.logerror;
import java.awt.Transparency;
import static mame056.cpuexec.cpu_getcurrentframe;
import static mame056.cpuintrfH.ASSERT_LINE;
import static mame056.driverH.ORIENTATION_FLIP_X;
import static mame056.driverH.ORIENTATION_FLIP_Y;
import static mame056.driverH.ORIENTATION_SWAP_XY;

public class konamiic
{
	//K051960_callback interface
        public static abstract interface K051960_callbackProcPtr {

            public abstract void handler(int[] code, int[] color, int[] priority, int[] shadow);
        }
        
        //K051316_callback interface
        public static abstract interface K051316_callbackProcPtr {
            public abstract void handler(int[] code, int[] color);
        }
        
        //K053247_callback interface
        public static abstract interface K053247_callbackProcPtr {
            public abstract void handler(int[] code, int[] color, int[] priority);
        }
        //K052109_callback interface
        public static abstract interface K052109_callbackProcPtr {
            public abstract void handler(int layer, int bank, int[] code, int[] color);
        }
        
        	
        	
        	/*
        		This recursive function doesn't use additional memory
        		(it could be easily converted into an iterative one).
        		It's called shuffle because it mimics the shuffling of a deck of cards.
        	*/
        	static void shuffle(UBytePtr buf,int len)
        	{
        		int i;
                        char t;

                        if (len == 2) return;
	
		if ((len % 4)!=0) exit(1);   /* must not happen */
	
		len /= 2;
	
		for (i = 0;i < len/2;i++)
		{
			t = buf.read(len/2 + i);
			buf.write(len/2 + i, buf.read(len + i));
			buf.write(len + i, t);
		}

                        shuffle(new UBytePtr(buf), len);
                        shuffle(new UBytePtr(buf, len), len);//len*2 ??
        	}
        	
        	
        	/* helper function to join two 16-bit ROMs and form a 32-bit data stream */
        	public static void konami_rom_deinterleave_2(int mem_region)
        	{
        		shuffle(new UBytePtr(memory_region(mem_region)),memory_region_length(mem_region));
        	}
        	
        	/* helper function to join four 16-bit ROMs and form a 64-bit data stream */
        	public static void konami_rom_deinterleave_4(int mem_region)
        	{
        		konami_rom_deinterleave_2(mem_region);
        		konami_rom_deinterleave_2(mem_region);
        	}
        	
        	
        	
        	
        	
        	
        	
        	
        	public static int[][] K007121_ctrlram = new int[MAX_K007121][8];
        	static int[] K007121_flipscreen = new int[MAX_K007121];
        	
        	
        	public static void K007121_ctrl_w(int chip,int offset,int data)
        	{
        		switch (offset)
        		{
        			case 6:
        	/* palette bank change */
        	if ((K007121_ctrlram[chip][offset] & 0x30) != (data & 0x30))
        		tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
        				break;
        			case 7:
        				K007121_flipscreen[chip] = data & 0x08;
        				break;
        		}
        	
        		K007121_ctrlram[chip][offset] = data;
        	}
        	
        	public static WriteHandlerPtr K007121_ctrl_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K007121_ctrl_w(0,offset,data);
        	} };
        	
        	public static WriteHandlerPtr K007121_ctrl_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K007121_ctrl_w(1,offset,data);
        	} };
        	
        	
        	/*
        	 * Sprite Format
        	 * ------------------
        	 *
        	 * There are 0x40 sprites, each one using 5 bytes. However the number of
        	 * sprites can be increased to 0x80 with a control register (Combat School
        	 * sets it on and off during the game).
        	 *
        	 * Byte | Bit(s)   | Use
        	 * -----+-76543210-+----------------
        	 *   0  | xxxxxxxx | sprite code
        	 *   1  | xxxx---- | color
        	 *   1  | ----xx-- | sprite code low 2 bits for 16x8/8x8 sprites
        	 *   1  | ------xx | sprite code bank bits 1/0
        	 *   2  | xxxxxxxx | y position
        	 *   3  | xxxxxxxx | x position (low 8 bits)
        	 *   4  | xx------ | sprite code bank bits 3/2
        	 *   4  | --x----- | flip y
        	 *   4  | ---x---- | flip x
        	 *   4  | ----xxx- | sprite size 000=16x16 001=16x8 010=8x16 011=8x8 100=32x32
        	 *   4  | -------x | x position (high bit)
        	 *
        	 * Flack Attack uses a different, "wider" layout with 32 bytes per sprite,
        	 * mapped as follows, and the priority order is reversed. Maybe it is a
        	 * compatibility mode with an older custom IC. It is not known how this
        	 * alternate layout is selected.
        	 *
        	 * 0 . e
        	 * 1 . f
        	 * 2 . 6
        	 * 3 . 4
        	 * 4 . 8
        	 *
        	 */
        	
        	public static void K007121_sprites_draw(int chip, mame_bitmap bitmap,
        			UBytePtr source,int base_color,int global_x_offset,int bank_base,
        			int pri_mask)
        	{
        		GfxElement gfx = Machine.gfx[chip];
        		int flipscreen = K007121_flipscreen[chip];
        		int i,num,inc,trans;
        		int[] offs = new int[5];
        		int is_flakatck = K007121_ctrlram[chip][0x06] & 0x04;	/* WRONG!!!! */
        	
        /*TODO*///	#if 0
        /*TODO*///	usrintf_showmessage("%02x-%02x-%02x-%02x-%02x-%02x-%02x-%02x  %02x-%02x-%02x-%02x-%02x-%02x-%02x-%02x",
        /*TODO*///		K007121_ctrlram[0][0x00],K007121_ctrlram[0][0x01],K007121_ctrlram[0][0x02],K007121_ctrlram[0][0x03],K007121_ctrlram[0][0x04],K007121_ctrlram[0][0x05],K007121_ctrlram[0][0x06],K007121_ctrlram[0][0x07],
        /*TODO*///		K007121_ctrlram[1][0x00],K007121_ctrlram[1][0x01],K007121_ctrlram[1][0x02],K007121_ctrlram[1][0x03],K007121_ctrlram[1][0x04],K007121_ctrlram[1][0x05],K007121_ctrlram[1][0x06],K007121_ctrlram[1][0x07]);
        /*TODO*///	#endif
        /*TODO*///	#if 0
        /*TODO*///	if (keyboard_pressed(KEYCODE_D))
        /*TODO*///	{
        /*TODO*///		FILE *fp;
        /*TODO*///		fp=fopen(chip?"SPRITE1.DMP":"SPRITE0.DMP", "w+b");
        /*TODO*///		if (fp)
        /*TODO*///		{
        /*TODO*///			fwrite(source, 0x800, 1, fp);
        /*TODO*///			usrintf_showmessage("saved");
        /*TODO*///			fclose(fp);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	#endif
        	
        		if (is_flakatck != 0)
        		{
        			num = 0x40;
        			inc = -0x20;
        			source.inc( 0x3f*0x20 );
        			offs[0] = 0x0e;
        			offs[1] = 0x0f;
        			offs[2] = 0x06;
        			offs[3] = 0x04;
        			offs[4] = 0x08;
        			/* Flak Attack doesn't use a lookup PROM, it maps the color code directly */
        			/* to a palette entry */
        			trans = TRANSPARENCY_PEN;
        		}
        		else	/* all others */
        		{
        			num = (K007121_ctrlram[chip][0x03] & 0x40)!=0 ? 0x80 : 0x40;	/* WRONG!!! (needed by combasc)  */
        			inc = 5;
        			offs[0] = 0x00;
        			offs[1] = 0x01;
        			offs[2] = 0x02;
        			offs[3] = 0x03;
        			offs[4] = 0x04;
        			trans = TRANSPARENCY_COLOR;
        			/* when using priority buffer, draw front to back */
        			if (pri_mask != -1)
        			{
        				source.inc( (num-1)*inc );
        				inc = -inc;
        			}
        		}
        	
        		for (i = 0;i < num;i++)
        		{
        			int number = source.read(offs[0]);				/* sprite number */
        			int sprite_bank = source.read(offs[1]) & 0x0f;	/* sprite bank */
        			int sx = source.read(offs[3]);					/* vertical position */
        			int sy = source.read(offs[2]);					/* horizontal position */
        			int attr = source.read(offs[4]);				/* attributes */
        			int xflip = source.read(offs[4]) & 0x10;		/* flip x */
        			int yflip = source.read(offs[4]) & 0x20;		/* flip y */
        			int color = base_color + ((source.read(offs[1]) & 0xf0) >> 4);
        			int width,height;
        			int x_offset[] = {0x0,0x1,0x4,0x5};
        			int y_offset[] = {0x0,0x2,0x8,0xa};
        			int x,y, ex, ey;
        	
        			if ((attr & 0x01) != 0) sx -= 256;
        			if (sy >= 240) sy -= 256;
        	
        			number += ((sprite_bank & 0x3) << 8) + ((attr & 0xc0) << 4);
        			number = number << 2;
        			number += (sprite_bank >> 2) & 3;
        	
        			if (is_flakatck==0 || source.read(0x00)!=0)	/* Flak Attack needs this */
        			{
        				number += bank_base;
        	
        				switch( attr&0xe )
        				{
        					case 0x06: width = height = 1; break;
        					case 0x04: width = 1; height = 2; number &= (~2); break;
        					case 0x02: width = 2; height = 1; number &= (~1); break;
        					case 0x00: width = height = 2; number &= (~3); break;
        					case 0x08: width = height = 4; number &= (~3); break;
        					default: width = 1; height = 1;
        	//					logerror("Unknown sprite size %02x\n",attr&0xe);
        	//					usrintf_showmessage("Unknown sprite size %02x\n",attr&0xe);
        				}
        	
        				for (y = 0;y < height;y++)
        				{
        					for (x = 0;x < width;x++)
        					{
        						ex = xflip!=0 ? (width-1-x) : x;
        						ey = yflip!=0 ? (height-1-y) : y;
        	
        						if (flipscreen!=0)
        						{
        							if (pri_mask != -1)
        								pdrawgfx(bitmap,gfx,
        									number + x_offset[ex] + y_offset[ey],
        									color,
        									xflip!=0?0:1,yflip!=0?0:1,
        									248-(sx+x*8),248-(sy+y*8),
        									Machine.visible_area,trans,0,
        									pri_mask);
        							else
        								drawgfx(bitmap,gfx,
        									number + x_offset[ex] + y_offset[ey],
        									color,
        									xflip!=0?0:1,yflip!=0?0:1,
        									248-(sx+x*8),248-(sy+y*8),
        									Machine.visible_area,trans,0);
        						}
        						else
        						{
        							if (pri_mask != -1)
        								pdrawgfx(bitmap,gfx,
        									number + x_offset[ex] + y_offset[ey],
        									color,
        									xflip,yflip,
        									global_x_offset+sx+x*8,sy+y*8,
        									Machine.visible_area,trans,0,
        									pri_mask);
        							else
        								drawgfx(bitmap,gfx,
        									number + x_offset[ex] + y_offset[ey],
        									color,
        									xflip,yflip,
        									global_x_offset+sx+x*8,sy+y*8,
        									Machine.visible_area,trans,0);
        						}
        					}
        				}
        			}
        	
        			source.inc( inc );
        		}
        	}
        
        
        
        
        
        
	        public static abstract interface K007342_callbackProcPtr {	
	            public abstract void handler(int tilemap, int bank, int[] code, int[] color);
	        }
	        
        	static UBytePtr K007342_ram, K007342_scroll_ram;
        	static int K007342_gfxnum;
        	static int K007342_int_enabled;
        	static int K007342_flipscreen;
        	static int[] K007342_scrollx = new int[2];
        	static int[] K007342_scrolly = new int[2];
        	static UBytePtr K007342_videoram_0, K007342_colorram_0;
        	static UBytePtr K007342_videoram_1, K007342_colorram_1;
        	static int[] K007342_regs = new int[8];
        	static K007342_callbackProcPtr K007342_callback; //(int tilemap, int bank, int *code, int *color);
        	static struct_tilemap[] K007342_tilemap = new struct_tilemap[2];

        /***************************************************************************
        
          Callbacks for the TileMap code
        
        ***************************************************************************/
        
        /*
          data format:
          video RAM     xxxxxxxx    tile number (bits 0-7)
          color RAM     x-------    tiles with priority over the sprites
          color RAM     -x------    depends on external conections
          color RAM     --x-----    flip Y
          color RAM     ---x----    flip X
          color RAM     ----xxxx    depends on external connections (usually color and banking)
        */
        
        static GetMemoryOffsetPtr K007342_scan = new GetMemoryOffsetPtr() {
        	public int handler(int col,int row,int num_cols,int num_rows)
        	{
	        	/* logical (col,row) . memory offset */
	        	return (col & 0x1f) + ((row & 0x1f) << 5) + ((col & 0x20) << 5);
	        }
        };
        
        public static void K007342_get_tile_info(int tile_index,int layer,UBytePtr cram,UBytePtr vram)
        {
        	int[] color=new int[1], code=new int[1];
        
        	color[0] = cram.read(tile_index);
        	code[0] = vram.read(tile_index);
        
        	tile_info.flags = TILE_FLIPYX((color[0] & 0x30) >> 4);
        	tile_info.priority = (color[0] & 0x80) >> 7;
        
        	(K007342_callback).handler(layer, K007342_regs[1], code, color);
        
        	SET_TILE_INFO(
        			K007342_gfxnum,
        			code[0],
        			color[0],
        			tile_info.flags);
        }
        
        	static GetTileInfoPtr K007342_get_tile_info0 = new GetTileInfoPtr() {
        		public void handler(int tile_index) {
        			K007342_get_tile_info(tile_index,0,K007342_colorram_0,K007342_videoram_0);
        		}
        	};
        	
        	static GetTileInfoPtr K007342_get_tile_info1 = new GetTileInfoPtr() {
        		public void handler(int tile_index) {
        			K007342_get_tile_info(tile_index,1,K007342_colorram_1,K007342_videoram_1); 
    			}
    		};
        
        
        
        public static int K007342_vh_start(int gfx_index, K007342_callbackProcPtr callback)
        {
        	K007342_gfxnum = gfx_index;
        	K007342_callback = callback;
        
        	K007342_tilemap[0] = tilemap_create(K007342_get_tile_info0,K007342_scan,TILEMAP_TRANSPARENT,8,8,64,32);
        	K007342_tilemap[1] = tilemap_create(K007342_get_tile_info1,K007342_scan,TILEMAP_TRANSPARENT,8,8,64,32);
        
        	K007342_ram = new UBytePtr(0x2000);
        	K007342_scroll_ram = new UBytePtr(0x0200);
        
        	if (K007342_ram==null || K007342_scroll_ram==null || K007342_tilemap[0]==null || K007342_tilemap[1]==null)
        	{
        		K007342_vh_stop.handler();
        		return 1;
        	}
        
        	memset(K007342_ram,0,0x2000);
        
        	K007342_colorram_0 = new UBytePtr(K007342_ram, 0x0000);
        	K007342_colorram_1 = new UBytePtr(K007342_ram, 0x1000);
        	K007342_videoram_0 = new UBytePtr(K007342_ram, 0x0800);
        	K007342_videoram_1 = new UBytePtr(K007342_ram, 0x1800);
        
        	tilemap_set_transparent_pen(K007342_tilemap[0],0);
        	tilemap_set_transparent_pen(K007342_tilemap[1],0);
        
        	return 0;
        }
        
        public static VhStopPtr K007342_vh_stop = new VhStopPtr() { public void handler() 
        {
        	K007342_ram = null;
        	K007342_scroll_ram = null;
        } };
        
        public static ReadHandlerPtr K007342_r  = new ReadHandlerPtr() { public int handler(int offset)
        {
        	return K007342_ram.read(offset);
        } };
        
        public static WriteHandlerPtr K007342_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        {
        	if (offset < 0x1000)
        	{		/* layer 0 */
        		if (K007342_ram.read(offset) != data)
        		{
        			K007342_ram.write(offset, data);
        			tilemap_mark_tile_dirty(K007342_tilemap[0],offset & 0x7ff);
        		}
        	}
        	else
        	{						/* layer 1 */
        		if (K007342_ram.read(offset) != data)
        		{
        			K007342_ram.write(offset, data);
        			tilemap_mark_tile_dirty(K007342_tilemap[1],offset & 0x7ff);
        		}
        	}
        } };
        
        public static ReadHandlerPtr K007342_scroll_r  = new ReadHandlerPtr() { public int handler(int offset)
        {
        	return K007342_scroll_ram.read(offset);
        } };
        
        public static WriteHandlerPtr K007342_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        {
        	K007342_scroll_ram.write(offset, data);
        } };
        
        public static WriteHandlerPtr K007342_vreg_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        {
        	switch(offset)
        	{
        		case 0x00:
        			/* bit 1: INT control */
        			K007342_int_enabled = data & 0x02;
        			K007342_flipscreen = data & 0x10;
        			tilemap_set_flip(K007342_tilemap[0],K007342_flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
        			tilemap_set_flip(K007342_tilemap[1],K007342_flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
        			break;
        		case 0x01:  /* used for banking in Rock'n'Rage */
        			if (data != K007342_regs[1])
        				tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
        		case 0x02:
        			K007342_scrollx[0] = (K007342_scrollx[0] & 0xff) | ((data & 0x01) << 8);
        			K007342_scrollx[1] = (K007342_scrollx[1] & 0xff) | ((data & 0x02) << 7);
        			break;
        		case 0x03:  /* scroll x (register 0) */
        			K007342_scrollx[0] = (K007342_scrollx[0] & 0x100) | data;
        			break;
        		case 0x04:  /* scroll y (register 0) */
        			K007342_scrolly[0] = data;
        			break;
        		case 0x05:  /* scroll x (register 1) */
        			K007342_scrollx[1] = (K007342_scrollx[1] & 0x100) | data;
        			break;
        		case 0x06:  /* scroll y (register 1) */
        			K007342_scrolly[1] = data;
        		case 0x07:  /* unused */
        			break;
        	}
        	K007342_regs[offset] = data;
        } };
        
        	public static void K007342_tilemap_update()
        	{
        		int offs;
        	
        	
        		/* update scroll */
        		switch (K007342_regs[2] & 0x1c)
        		{
        			case 0x00:
        			case 0x08:	/* unknown, blades of steel shootout between periods */
        				tilemap_set_scroll_rows(K007342_tilemap[0],1);
        				tilemap_set_scroll_cols(K007342_tilemap[0],1);
        				tilemap_set_scrollx(K007342_tilemap[0],0,K007342_scrollx[0]);
        				tilemap_set_scrolly(K007342_tilemap[0],0,K007342_scrolly[0]);
        				break;
        	
        			case 0x0c:	/* 32 columns */
        				tilemap_set_scroll_rows(K007342_tilemap[0],1);
        				tilemap_set_scroll_cols(K007342_tilemap[0],512);
        				tilemap_set_scrollx(K007342_tilemap[0],0,K007342_scrollx[0]);
        				for (offs = 0;offs < 256;offs++)
        					tilemap_set_scrolly(K007342_tilemap[0],(offs + K007342_scrollx[0]) & 0x1ff,
        							K007342_scroll_ram.read(2*(offs/8)) + 256 * K007342_scroll_ram.read(2*(offs/8)+1));
        				break;
        	
        			case 0x14:	/* 256 rows */
        				tilemap_set_scroll_rows(K007342_tilemap[0],256);
        				tilemap_set_scroll_cols(K007342_tilemap[0],1);
        				tilemap_set_scrolly(K007342_tilemap[0],0,K007342_scrolly[0]);
        				for (offs = 0;offs < 256;offs++)
        					tilemap_set_scrollx(K007342_tilemap[0],(offs + K007342_scrolly[0]) & 0xff,
        							K007342_scroll_ram.read(2*offs) + 256 * K007342_scroll_ram.read(2*offs+1));
        				break;
        	
        			default:
        				usrintf_showmessage("unknown scroll ctrl %02x",K007342_regs[2] & 0x1c);
        				break;
        		}
        	
        		tilemap_set_scrollx(K007342_tilemap[1],0,K007342_scrollx[1]);
        		tilemap_set_scrolly(K007342_tilemap[1],0,K007342_scrolly[1]);
        	
        /*TODO*///	#if 0
        /*TODO*///		{
        /*TODO*///			static int current_layer = 0;
        /*TODO*///	
        /*TODO*///			if (keyboard_pressed_memory(KEYCODE_Z)) current_layer = !current_layer;
        /*TODO*///			tilemap_set_enable(K007342_tilemap[current_layer], 1);
        /*TODO*///			tilemap_set_enable(K007342_tilemap[!current_layer], 0);
        /*TODO*///	
        /*TODO*///			usrintf_showmessage("regs:%02x %02x %02x %02x-%02x %02x %02x %02x:%02x",
        /*TODO*///				K007342_regs[0], K007342_regs[1], K007342_regs[2], K007342_regs[3],
        /*TODO*///				K007342_regs[4], K007342_regs[5], K007342_regs[6], K007342_regs[7],
        /*TODO*///				current_layer);
        /*TODO*///		}
        /*TODO*///	#endif
        	}
        /*TODO*///	
        /*TODO*///	void K007342_tilemap_set_enable(int tilemap, int enable)
        /*TODO*///	{
        /*TODO*///		tilemap_set_enable(K007342_tilemap[tilemap], enable);
        /*TODO*///	}
        	
    	public static void K007342_tilemap_draw(mame_bitmap bitmap,int num,int flags,int priority)
    	{
    		tilemap_draw(bitmap,K007342_tilemap[num],flags,priority);
    	}
        	
        public static int K007342_is_INT_enabled()
        {
        	return K007342_int_enabled;
        }
        
        // K007420 INI 
        public static abstract interface K007420_callbackProcPtr {
            public abstract void handler(int[] code, int[] color);
        }
        
        static GfxElement K007420_gfx;
        static K007420_callbackProcPtr K007420_callback; //(int *code,int *color);
        static UBytePtr K007420_ram;
        
        public static int K007420_vh_start(int gfxnum, K007420_callbackProcPtr callback)
        {
        	K007420_gfx = Machine.gfx[gfxnum];
        	K007420_callback = callback;
        	K007420_ram = new UBytePtr(0x200);
        	if (K007420_ram == null) return 1;
        
        	memset(K007420_ram,0,0x200);
        
        	return 0;
        }
        
        public static VhStopPtr K007420_vh_stop = new VhStopPtr() { public void handler() 
        {
        	K007420_ram = null;
        } };
        
        public static ReadHandlerPtr K007420_r  = new ReadHandlerPtr() { public int handler(int offset)
        {
        	return K007420_ram.read(offset);
        } };
        
        public static WriteHandlerPtr K007420_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        {
        	K007420_ram.write(offset, data);
        } };
        
        /*
         * Sprite Format
         * ------------------
         *
         * Byte | Bit(s)   | Use
         * -----+-76543210-+----------------
         *   0  | xxxxxxxx | y position
         *   1  | xxxxxxxx | sprite code (low 8 bits)
         *   2  | xxxxxxxx | depends on external conections. Usually banking
         *   3  | xxxxxxxx | x position (low 8 bits)
         *   4  | x------- | x position (high bit)
         *   4  | -xxx---- | sprite size 000=16x16 001=8x16 010=16x8 011=8x8 100=32x32
         *   4  | ----x--- | flip y
         *   4  | -----x-- | flip x
         *   4  | ------xx | zoom (bits 8 & 9)
         *   5  | xxxxxxxx | zoom (low 8 bits)  0x080 = normal, < 0x80 enlarge, > 0x80 reduce
         *   6  | xxxxxxxx | unused
         *   7  | xxxxxxxx | unused
         */
        
        public static void K007420_sprites_draw(mame_bitmap bitmap)
        {
        	int K007420_SPRITERAM_SIZE = 0x200;
        	int offs;
        
        	for (offs = K007420_SPRITERAM_SIZE - 8; offs >= 0; offs -= 8)
        	{
        		int ox,oy,flipx,flipy,zoom,w,h,x,y;
        		int[] code=new int[1];
        		int[] color=new int[1];
        		int xoffset[] = { 0, 1, 4, 5 };
        		int yoffset[] = { 0, 2, 8, 10 };
        
        		code[0] = K007420_ram.read(offs+1);
        		color[0] = K007420_ram.read(offs+2);
        		ox = K007420_ram.read(offs+3) - ((K007420_ram.read(offs+4) & 0x80) << 1);
        		oy = 256 - K007420_ram.read(offs+0);
        		flipx = K007420_ram.read(offs+4) & 0x04;
        		flipy = K007420_ram.read(offs+4) & 0x08;
        
        		(K007420_callback).handler(code, color);
        
        		/* kludge for rock'n'rage */
        		if ((K007420_ram.read(offs+4) == 0x40) && (K007420_ram.read(offs+1) == 0xff) &&
        			(K007420_ram.read(offs+2) == 0x00) && (K007420_ram.read(offs+5) == 0xf0)) continue;
        
        		/* 0x080 = normal scale, 0x040 = double size, 0x100 half size */
        		zoom = K007420_ram.read(offs+5) | ((K007420_ram.read(offs+4) & 0x03) << 8);
        		if (zoom == 0) continue;
        		zoom = 0x10000 * 128 / zoom;
        
        		switch (K007420_ram.read(offs+4) & 0x70)
        		{
        			case 0x30: w = h = 1; break;
        			case 0x20: w = 2; h = 1; code[0] &= (~1); break;
        			case 0x10: w = 1; h = 2; code[0] &= (~2); break;
        			case 0x00: w = h = 2; code[0] &= (~3); break;
        			case 0x40: w = h = 4; code[0] &= (~3); break;
        			default: w = 1; h = 1;
        //logerror("Unknown sprite size %02x\n",(K007420_ram[offs+4] & 0x70)>>4);
        		}
        
        		if (K007342_flipscreen != 0)
        		{
        			ox = 256 - ox - ((zoom * w + (1<<12)) >> 13);
        			oy = 256 - oy - ((zoom * h + (1<<12)) >> 13);
        			flipx = flipx!=0?0:1;
        			flipy = flipy!=0?0:1;
        		}
        
        		if (zoom == 0x10000)
        		{
        			int sx,sy;
        
        			for (y = 0;y < h;y++)
        			{
        				sy = oy + 8 * y;
        
        				for (x = 0;x < w;x++)
        				{
        					int c = code[0];
        
        					sx = ox + 8 * x;
        					if (flipx!=0) c += xoffset[(w-1-x)];
        					else c += xoffset[x];
        					if (flipy!=0) c += yoffset[(h-1-y)];
        					else c += yoffset[y];
        
        					drawgfx(bitmap,K007420_gfx,
        						c,
        						color[0],
        						flipx,flipy,
        						sx,sy,
        						Machine.visible_area,TRANSPARENCY_PEN,0);
        
        					if ((K007342_regs[2] & 0x80) != 0)
        						drawgfx(bitmap,K007420_gfx,
        							c,
        							color[0],
        							flipx,flipy,
        							sx,sy-256,
        							Machine.visible_area,TRANSPARENCY_PEN,0);
        				}
        			}
        		}
        		else
        		{
        			int sx,sy,zw,zh;
        			for (y = 0;y < h;y++)
        			{
        				sy = oy + ((zoom * y + (1<<12)) >> 13);
        				zh = (oy + ((zoom * (y+1) + (1<<12)) >> 13)) - sy;
        
        				for (x = 0;x < w;x++)
        				{
        					int c = code[0];
        
        					sx = ox + ((zoom * x + (1<<12)) >> 13);
        					zw = (ox + ((zoom * (x+1) + (1<<12)) >> 13)) - sx;
        					if (flipx!=0) c += xoffset[(w-1-x)];
        					else c += xoffset[x];
        					if (flipy!=0) c += yoffset[(h-1-y)];
        					else c += yoffset[y];
        
        					drawgfxzoom(bitmap,K007420_gfx,
        						c,
        						color[0],
        						flipx,flipy,
        						sx,sy,
        						Machine.visible_area,TRANSPARENCY_PEN,0,
        						(zw << 16) / 8,(zh << 16) / 8);
        
        					if ((K007342_regs[2] & 0x80) != 0)
        						drawgfxzoom(bitmap,K007420_gfx,
        							c,
        							color[0],
        							flipx,flipy,
        							sx,sy-256,
        							Machine.visible_area,TRANSPARENCY_PEN,0,
        							(zw << 16) / 8,(zh << 16) / 8);
        				}
        			}
        		}
        	}
        /*TODO*///	#if 0
        /*TODO*///		{
        /*TODO*///			static int current_sprite = 0;
        /*TODO*///	
        /*TODO*///			if (keyboard_pressed_memory(KEYCODE_Z)) current_sprite = (current_sprite+1) & ((K007420_SPRITERAM_SIZE/8)-1);
        /*TODO*///			if (keyboard_pressed_memory(KEYCODE_X)) current_sprite = (current_sprite-1) & ((K007420_SPRITERAM_SIZE/8)-1);
        /*TODO*///	
        /*TODO*///			usrintf_showmessage("%02x:%02x %02x %02x %02x %02x %02x %02x %02x", current_sprite,
        /*TODO*///				K007420_ram[(current_sprite*8)+0], K007420_ram[(current_sprite*8)+1],
        /*TODO*///				K007420_ram[(current_sprite*8)+2], K007420_ram[(current_sprite*8)+3],
        /*TODO*///				K007420_ram[(current_sprite*8)+4], K007420_ram[(current_sprite*8)+5],
        /*TODO*///				K007420_ram[(current_sprite*8)+6], K007420_ram[(current_sprite*8)+7]);
        /*TODO*///		}
        /*TODO*///	#endif
        }
        
        
        
        
        	static int K052109_memory_region;
        	static int K052109_gfxnum;
        	static K052109_callbackProcPtr K052109_callback;
        	static UBytePtr K052109_ram;
        	static UBytePtr K052109_videoram_F, K052109_videoram2_F, K052109_colorram_F;
        	static UBytePtr K052109_videoram_A, K052109_videoram2_A, K052109_colorram_A;
        	static UBytePtr K052109_videoram_B, K052109_videoram2_B, K052109_colorram_B;
        	static int[] K052109_charrombank = new int[4];
        	static int has_extra_video_ram;
        	static int K052109_RMRD_line;
        	static int K052109_tileflip_enable;
        	static int K052109_irq_enabled;
        	static int K052109_romsubbank,K052109_scrollctrl;
        	static struct_tilemap[] K052109_tilemap = new struct_tilemap[3];
        	
        	
        	
        	/***************************************************************************
        	
        	  Callbacks for the TileMap code
        	
        	***************************************************************************/
        	
        	/*
        	  data format:
        	  video RAM    xxxxxxxx  tile number (low 8 bits)
        	  color RAM    xxxx----  depends on external connections (usually color and banking)
        	  color RAM    ----xx--  bank select (0-3): these bits are replaced with the 2
        	                         bottom bits of the bank register before being placed on
        	                         the output pins. The other two bits of the bank register are
        	                         placed on the CAB1 and CAB2 output pins.
        	  color RAM    ------xx  depends on external connections (usually banking, flip)
        	*/
        	
        	public static void K052109_get_tile_info(int tile_index,int layer,UBytePtr cram,UBytePtr vram1,UBytePtr vram2)
        	{
        		int flipy = 0;
                        int[] code=new int[1], color=new int[1];
                        code[0] = vram1.read(tile_index) + 256 * vram2.read(tile_index);
                        color[0] = READ_WORD(cram,tile_index);
                        int bank = K052109_charrombank[(color[0] & 0x0c) >> 2];
                        if (has_extra_video_ram!=0) bank = (color[0] & 0x0c) >> 2;	/* kludge for X-Men */
                        color[0] = (color[0] & 0xf3) | ((bank & 0x03) << 2);
                        bank >>= 2;

                        flipy = color[0] & 0x02;

                        tile_info.flags = 0;

                        K052109_callback.handler(layer,bank,code,color);

                        SET_TILE_INFO(
                                        K052109_gfxnum,
                                        code[0],
                                        color[0],
                                        tile_info.flags);

                        /* if the callback set flip X but it is not enabled, turn it off */
                        if ((K052109_tileflip_enable & 1)==0) tile_info.flags &= ~TILE_FLIPX;

                        /* if flip Y is enabled and the attribute but is set, turn it on */
                        if (flipy!=0 && (K052109_tileflip_enable & 2)!=0) tile_info.flags |= TILE_FLIPY;
        	}
        	
        	static GetTileInfoPtr K052109_get_tile_info0 = new GetTileInfoPtr() { public void handler(int tile_index) {K052109_get_tile_info(tile_index,0,K052109_colorram_F,K052109_videoram_F,K052109_videoram2_F); }};
                static GetTileInfoPtr K052109_get_tile_info1 = new GetTileInfoPtr() { public void handler(int tile_index) {K052109_get_tile_info(tile_index,1,K052109_colorram_A,K052109_videoram_A,K052109_videoram2_A); }};
                static GetTileInfoPtr K052109_get_tile_info2 = new GetTileInfoPtr() { public void handler(int tile_index) {K052109_get_tile_info(tile_index,2,K052109_colorram_B,K052109_videoram_B,K052109_videoram2_B); }};
        /*TODO*///	
        /*TODO*///	052109_colorram_F = new UBytePtr(K052109_ram, 0x0000);
        		/*
                        K052109_colorram_A = new UBytePtr(K052109_ram, 0x0800);
        		K052109_colorram_B = new UBytePtr(K052109_ram, 0x1000);
        		K052109_videoram_F = new UBytePtr(K052109_ram, 0x2000);
        		K052109_videoram_A = new UBytePtr(K052109_ram, 0x2800);
        		K052109_videoram_B = new UBytePtr(K052109_ram, 0x3000);
        		K052109_videoram2_F = new UBytePtr(K052109_ram, 0x4000);
        		K052109_videoram2_A = new UBytePtr(K052109_ram, 0x4800);
        		K052109_videoram2_B = new UBytePtr(K052109_ram, 0x5000);
                */
        /*TODO*///	static void K052109_tileflip_reset(void)
        /*TODO*///	{
        /*TODO*///		int data = K052109_ram[0x1e80];
        /*TODO*///		tilemap_set_flip(K052109_tilemap[0],(data & 1) ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
        /*TODO*///		tilemap_set_flip(K052109_tilemap[1],(data & 1) ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
        /*TODO*///		tilemap_set_flip(K052109_tilemap[2],(data & 1) ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
        /*TODO*///		K052109_tileflip_enable = ((data & 0x06) >> 1);
        /*TODO*///	}
        	
        	
        	public static int K052109_vh_start(int gfx_memory_region,int plane0,int plane1,int plane2,int plane3,
        			K052109_callbackProcPtr callback)
        	{
        		int gfx_index;
                        GfxLayout charlayout = new GfxLayout
                        (
                                8,8,
                                0,				/* filled in later */
                                4,
                                new int[] { 0, 0, 0, 0 },	/* filled in later */
                                new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
                                new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
                                32*8
                        );


                        /* find first empty slot to decode gfx */
                        for (gfx_index = 0; gfx_index < MAX_GFX_ELEMENTS; gfx_index++)
                                if (Machine.gfx[gfx_index] == null)
                                        break;
                        if (gfx_index == MAX_GFX_ELEMENTS)
                                return 1;

                        /* tweak the structure for the number of tiles we have */
                        charlayout.total = memory_region_length(gfx_memory_region) / 32;
                        charlayout.planeoffset[0] = plane3 * 8;
                        charlayout.planeoffset[1] = plane2 * 8;
                        charlayout.planeoffset[2] = plane1 * 8;
                        charlayout.planeoffset[3] = plane0 * 8;

                        /* decode the graphics */
                        Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region),charlayout);
                        if (Machine.gfx[gfx_index]==null)
                                return 1;

                        /* set the color information */
                        if (Machine.drv.color_table_len != 0)
                        {
                                Machine.gfx[gfx_index].colortable = new IntArray(Machine.remapped_colortable);
                                Machine.gfx[gfx_index].total_colors = Machine.drv.color_table_len / 16;
                        }
                        else
                        {
                                Machine.gfx[gfx_index].colortable = new IntArray(Machine.pens);
                                Machine.gfx[gfx_index].total_colors = Machine.drv.total_colors / 16;
                        }

                        K052109_memory_region = gfx_memory_region;
                        K052109_gfxnum = gfx_index;
                        K052109_callback = callback;
                        K052109_RMRD_line = CLEAR_LINE;

                        has_extra_video_ram = 0;

                        K052109_tilemap[0] = tilemap_create(K052109_get_tile_info0,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
                        K052109_tilemap[1] = tilemap_create(K052109_get_tile_info1,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
                        K052109_tilemap[2] = tilemap_create(K052109_get_tile_info2,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);

                        K052109_ram = new UBytePtr(0x6000);

                        if (K052109_ram==null || K052109_tilemap[0]==null || K052109_tilemap[1]==null || K052109_tilemap[2]==null)
                        {
                                K052109_vh_stop.handler();
                                return 1;
                        }

                        memset(K052109_ram,0,0x6000);

                        K052109_colorram_F = new UBytePtr(K052109_ram, 0x0000);
                        K052109_colorram_A = new UBytePtr(K052109_ram, 0x0800);
                        K052109_colorram_B = new UBytePtr(K052109_ram, 0x1000);
                        K052109_videoram_F = new UBytePtr(K052109_ram, 0x2000);
                        K052109_videoram_A = new UBytePtr(K052109_ram, 0x2800);
                        K052109_videoram_B = new UBytePtr(K052109_ram, 0x3000);
                        K052109_videoram2_F = new UBytePtr(K052109_ram, 0x4000);
                        K052109_videoram2_A = new UBytePtr(K052109_ram, 0x4800);
                        K052109_videoram2_B = new UBytePtr(K052109_ram, 0x5000);

                        tilemap_set_transparent_pen(K052109_tilemap[0],0);
                        tilemap_set_transparent_pen(K052109_tilemap[1],0);
                        tilemap_set_transparent_pen(K052109_tilemap[2],0);
	

        
        	
        /*TODO*///		state_save_register_UINT8("k052109", 0, "ram",        K052109_ram, 0x6000);
        /*TODO*///		state_save_register_int  ("k052109", 0, "rmrd",       &K052109_RMRD_line);
        /*TODO*///		state_save_register_UINT8("k052109", 0, "romsubbank", &K052109_romsubbank, 1);
        /*TODO*///		state_save_register_UINT8("k052109", 0, "scrollctrl", &K052109_scrollctrl, 1);
        /*TODO*///		state_save_register_int  ("k052109", 0, "irqen",      &K052109_irq_enabled);
        /*TODO*///		state_save_register_UINT8("k052109", 0, "charbank",   K052109_charrombank, 4);
        /*TODO*///		state_save_register_int  ("k052109", 0, "extra",      &has_extra_video_ram);
        /*TODO*///	
        /*TODO*///		state_save_register_func_postload(K052109_tileflip_reset);
        		return 0;
        	}
        	
        	public static VhStopPtr K052109_vh_stop = new VhStopPtr() { public void handler() 
        	{
        		K052109_ram = null;
        	} };
        	
        	
        	
        	public static ReadHandlerPtr K052109_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (K052109_RMRD_line == CLEAR_LINE)
		{
			if ((offset & 0x1fff) >= 0x1800)
			{
				if (offset >= 0x180c && offset < 0x1834)
				{	/* A y scroll */	}
				else if (offset >= 0x1a00 && offset < 0x1c00)
				{	/* A x scroll */	}
				else if (offset == 0x1d00)
				{	/* read for bitwise operations before writing */	}
				else if (offset >= 0x380c && offset < 0x3834)
				{	/* B y scroll */	}
				else if (offset >= 0x3a00 && offset < 0x3c00)
				{	/* B x scroll */	}
				else
	logerror("%04x: read from unknown 052109 address %04x\n",cpu_get_pc(),offset);
			}
	
			return K052109_ram.read(offset);
		}
		else	/* Punk Shot and TMNT read from 0000-1fff, Aliens from 2000-3fff */
		{
			int[] code=new int[1], color=new int[1];
                        code[0] = (offset & 0x1fff) >> 5;
			color[0] = K052109_romsubbank;
			int bank = K052109_charrombank[(color[0] & 0x0c) >> 2] >> 2;   /* discard low bits (TMNT) */
			int addr;
	
	if (has_extra_video_ram != 0) code[0] |= color[0] << 8;	/* kludge for X-Men */
	else
			K052109_callback.handler(0,bank,code,color);
	
			addr = (code[0] << 5) + (offset & 0x1f);
			addr &= memory_region_length(K052109_memory_region)-1;
	
	
			return memory_region(K052109_memory_region).read(addr);
		}
	} };
	
	public static WriteHandlerPtr K052109_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((offset & 0x1fff) < 0x1800) /* tilemap RAM */
		{
			if (K052109_ram.read(offset) != data)
			{
				if (offset >= 0x4000) has_extra_video_ram = 1;  /* kludge for X-Men */
				K052109_ram.write(offset, data);
				tilemap_mark_tile_dirty(K052109_tilemap[(offset & 0x1800) >> 11],offset & 0x7ff);
			}
		}
		else	/* control registers */
		{
			K052109_ram.write(offset, data);
	
			if (offset >= 0x180c && offset < 0x1834)
			{	/* A y scroll */	}
			else if (offset >= 0x1a00 && offset < 0x1c00)
			{	/* A x scroll */	}
			else if (offset == 0x1c80)
			{
	if (K052109_scrollctrl != data)
	{
	
	logerror("%04x: rowscrollcontrol = %02x\n",cpu_get_pc(),data);
				K052109_scrollctrl = data;
	}
			}
			else if (offset == 0x1d00)
			{
	
				/* bit 2 = irq enable */
				/* the custom chip can also generate NMI and FIRQ, for use with a 6809 */
				K052109_irq_enabled = data & 0x04;
			}
			else if (offset == 0x1d80)
			{
				int dirty = 0;
	
				if (K052109_charrombank[0] != (data & 0x0f)) dirty |= 1;
				if (K052109_charrombank[1] != ((data >> 4) & 0x0f)) dirty |= 2;
				if (dirty != 0)
				{
					int i;
	
					K052109_charrombank[0] = data & 0x0f;
					K052109_charrombank[1] = (data >> 4) & 0x0f;
	
					for (i = 0;i < 0x1800;i++)
					{
						int bank = (K052109_ram.read(i)&0x0c) >> 2;
						if ((bank == 0 && (dirty & 1)!=0) || (bank == 1 && (dirty & 2)!=0))
						{
							tilemap_mark_tile_dirty(K052109_tilemap[(i & 0x1800) >> 11],i & 0x7ff);
						}
					}
				}
			}
			else if (offset == 0x1e00)
			{
	logerror("%04x: 052109 register 1e00 = %02x\n",cpu_get_pc(),data);
				K052109_romsubbank = data;
			}
			else if (offset == 0x1e80)
			{
	if ((data & 0xfe)!=0) logerror("%04x: 052109 register 1e80 = %02x\n",cpu_get_pc(),data);
				tilemap_set_flip(K052109_tilemap[0],(data & 1)!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
				tilemap_set_flip(K052109_tilemap[1],(data & 1)!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
				tilemap_set_flip(K052109_tilemap[2],(data & 1)!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
				if (K052109_tileflip_enable != ((data & 0x06) >> 1))
				{
					K052109_tileflip_enable = ((data & 0x06) >> 1);
	
					tilemap_mark_all_tiles_dirty(K052109_tilemap[0]);
					tilemap_mark_all_tiles_dirty(K052109_tilemap[1]);
					tilemap_mark_all_tiles_dirty(K052109_tilemap[2]);
				}
			}
			else if (offset == 0x1f00)
			{
				int dirty = 0;
	
				if (K052109_charrombank[2] != (data & 0x0f)) dirty |= 1;
				if (K052109_charrombank[3] != ((data >> 4) & 0x0f)) dirty |= 2;
				if (dirty != 0)
				{
					int i;
	
					K052109_charrombank[2] = data & 0x0f;
					K052109_charrombank[3] = (data >> 4) & 0x0f;
	
					for (i = 0;i < 0x1800;i++)
					{
						int bank = (K052109_ram.read(i) & 0x0c) >> 2;
						if ((bank == 2 && (dirty & 1)!=0) || (bank == 3 && (dirty & 2)!=0))
							tilemap_mark_tile_dirty(K052109_tilemap[(i & 0x1800) >> 11],i & 0x7ff);
					}
				}
			}
			else if (offset >= 0x380c && offset < 0x3834)
			{	/* B y scroll */	}
			else if (offset >= 0x3a00 && offset < 0x3c00)
			{	/* B x scroll */	}
			else
	logerror("%04x: write %02x to unknown 052109 address %04x\n",cpu_get_pc(),data,offset);
		}
	} };
        	
        /*TODO*///	READ16_HANDLER( K052109_word_r )
        /*TODO*///	{
        /*TODO*///		return K052109_r(offset + 0x2000) | (K052109_r(offset) << 8);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K052109_word_w )
        /*TODO*///	{
        /*TODO*///		if (ACCESSING_MSB)
        /*TODO*///			K052109_w(offset,(data >> 8) & 0xff);
        /*TODO*///		if (ACCESSING_LSB)
        /*TODO*///			K052109_w(offset + 0x2000,data & 0xff);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	READ16_HANDLER(K052109_lsb_r)
        /*TODO*///	{
        /*TODO*///		return K052109_r(offset);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER(K052109_lsb_w)
        /*TODO*///	{
        /*TODO*///		if(ACCESSING_LSB)
        /*TODO*///			K052109_w(offset, data & 0xff);
        /*TODO*///	}
        	
        	public static void K052109_set_RMRD_line(int state)
        	{
        		K052109_RMRD_line = state;
        	}
                
                //public static UBytePtr colorram, videoram1, videoram2;
                //public static int layer;
        	/*static void tilemap0_preupdate() {
                    colorram = K052109_colorram_F;
                    videoram1 = K052109_videoram_F;
                    videoram2 = K052109_videoram2_F;
                    layer = 0;
                }

                static void tilemap1_preupdate() {
                    colorram = K052109_colorram_A;
                    videoram1 = K052109_videoram_A;
                    videoram2 = K052109_videoram2_A;
                    layer = 1;
                }

                static void tilemap2_preupdate() {
                    colorram = K052109_colorram_B;
                    videoram1 = K052109_videoram_B;
                    videoram2 = K052109_videoram2_B;
                    layer = 2;
                }*/
        	
        	public static void K052109_tilemap_update()
        	{
        if ((K052109_scrollctrl & 0x03) == 0x02) {
            int xscroll, yscroll, offs;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x1a00);

            tilemap_set_scroll_rows(K052109_tilemap[1], 256);
            tilemap_set_scroll_cols(K052109_tilemap[1], 1);
            yscroll = K052109_ram.read(0x180c);
            tilemap_set_scrolly(K052109_tilemap[1], 0, yscroll);
            for (offs = 0; offs < 256; offs++) {
                xscroll = scrollram.read(2 * (offs & 0xfff8) + 0) + 256 * scrollram.read(2 * (offs & 0xfff8) + 1);
                xscroll -= 6;
                tilemap_set_scrollx(K052109_tilemap[1], (offs + yscroll) & 0xff, xscroll);
            }
        } else if ((K052109_scrollctrl & 0x03) == 0x03) {
            int xscroll, yscroll, offs;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x1a00);

            tilemap_set_scroll_rows(K052109_tilemap[1], 256);
            tilemap_set_scroll_cols(K052109_tilemap[1], 1);
            yscroll = K052109_ram.read(0x180c);
            tilemap_set_scrolly(K052109_tilemap[1], 0, yscroll);
            for (offs = 0; offs < 256; offs++) {
                xscroll = scrollram.read(2 * offs + 0) + 256 * scrollram.read(2 * offs + 1);
                xscroll -= 6;
                tilemap_set_scrollx(K052109_tilemap[1], (offs + yscroll) & 0xff, xscroll);
            }
        } else if ((K052109_scrollctrl & 0x04) == 0x04) {
            int xscroll, yscroll, offs;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x1800);

            tilemap_set_scroll_rows(K052109_tilemap[1], 1);
            tilemap_set_scroll_cols(K052109_tilemap[1], 512);
            xscroll = K052109_ram.read(0x1a00) + 256 * K052109_ram.read(0x1a01);
            xscroll -= 6;
            tilemap_set_scrollx(K052109_tilemap[1], 0, xscroll);
            for (offs = 0; offs < 512; offs++) {
                yscroll = scrollram.read(offs / 8);
                tilemap_set_scrolly(K052109_tilemap[1], (offs + xscroll) & 0x1ff, yscroll);
            }
        } else {
            int xscroll, yscroll;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x1a00);

            tilemap_set_scroll_rows(K052109_tilemap[1], 1);
            tilemap_set_scroll_cols(K052109_tilemap[1], 1);
            xscroll = scrollram.read(0) + 256 * scrollram.read(1);
            xscroll -= 6;
            yscroll = K052109_ram.read(0x180c);
            tilemap_set_scrollx(K052109_tilemap[1], 0, xscroll);
            tilemap_set_scrolly(K052109_tilemap[1], 0, yscroll);
        }

        if ((K052109_scrollctrl & 0x18) == 0x10) {
            int xscroll, yscroll, offs;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x3a00);

            tilemap_set_scroll_rows(K052109_tilemap[2], 256);
            tilemap_set_scroll_cols(K052109_tilemap[2], 1);
            yscroll = K052109_ram.read(0x380c);
            tilemap_set_scrolly(K052109_tilemap[2], 0, yscroll);
            for (offs = 0; offs < 256; offs++) {
                xscroll = scrollram.read(2 * (offs & 0xfff8) + 0) + 256 * scrollram.read(2 * (offs & 0xfff8) + 1);
                xscroll -= 6;
                tilemap_set_scrollx(K052109_tilemap[2], (offs + yscroll) & 0xff, xscroll);
            }
        } else if ((K052109_scrollctrl & 0x18) == 0x18) {
            int xscroll, yscroll, offs;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x3a00);

            tilemap_set_scroll_rows(K052109_tilemap[2], 256);
            tilemap_set_scroll_cols(K052109_tilemap[2], 1);
            yscroll = K052109_ram.read(0x380c);
            tilemap_set_scrolly(K052109_tilemap[2], 0, yscroll);
            for (offs = 0; offs < 256; offs++) {
                xscroll = scrollram.read(2 * offs + 0) + 256 * scrollram.read(2 * offs + 1);
                xscroll -= 6;
                tilemap_set_scrollx(K052109_tilemap[2], (offs + yscroll) & 0xff, xscroll);
            }
        } else if ((K052109_scrollctrl & 0x20) == 0x20) {
            int xscroll, yscroll, offs;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x3800);

            tilemap_set_scroll_rows(K052109_tilemap[2], 1);
            tilemap_set_scroll_cols(K052109_tilemap[2], 512);
            xscroll = K052109_ram.read(0x3a00) + 256 * K052109_ram.read(0x3a01);
            xscroll -= 6;
            tilemap_set_scrollx(K052109_tilemap[2], 0, xscroll);
            for (offs = 0; offs < 512; offs++) {
                yscroll = scrollram.read(offs / 8);
                tilemap_set_scrolly(K052109_tilemap[2], (offs + xscroll) & 0x1ff, yscroll);
            }
        } else {
            int xscroll, yscroll;
            UBytePtr scrollram = new UBytePtr(K052109_ram, 0x3a00);

            tilemap_set_scroll_rows(K052109_tilemap[2], 1);
            tilemap_set_scroll_cols(K052109_tilemap[2], 1);
            xscroll = scrollram.read(0) + 256 * scrollram.read(1);
            xscroll -= 6;
            yscroll = K052109_ram.read(0x380c);
            tilemap_set_scrollx(K052109_tilemap[2], 0, xscroll);
            tilemap_set_scrolly(K052109_tilemap[2], 0, yscroll);
        }
        	}
        	
        	public static void K052109_tilemap_draw(mame_bitmap bitmap,int num,int flags,int priority)
        	{
        		tilemap_draw(bitmap,K052109_tilemap[num],flags,priority);
        	}
        	
        	public static int K052109_is_IRQ_enabled()
        	{
        		return K052109_irq_enabled;
        	}
        	
        	
        	
        	
        	
        	
        	
        	static int K051960_memory_region;
        	static GfxElement K051960_gfx;
        	static K051960_callbackProcPtr K051960_callback;
        	static int K051960_romoffset;
        	static int K051960_spriteflip,K051960_readroms;
        	static int[] K051960_spriterombank = new int[3];
        	static UBytePtr K051960_ram;
        	static int K051960_irq_enabled, K051960_nmi_enabled;
        	
        	
        	public static int K051960_vh_start(int gfx_memory_region,int plane0,int plane1,int plane2,int plane3,
        			K051960_callbackProcPtr callback)
        	{
        		int gfx_index,i;
        		GfxLayout spritelayout = new GfxLayout
        		(
        			16,16,
        			0,				/* filled in later */
        			4,
        			new int[] { 0, 0, 0, 0 },	/* filled in later */
        			new int[] { 0, 1, 2, 3, 4, 5, 6, 7,
        					8*32+0, 8*32+1, 8*32+2, 8*32+3, 8*32+4, 8*32+5, 8*32+6, 8*32+7 },
        			new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
        					16*32, 17*32, 18*32, 19*32, 20*32, 21*32, 22*32, 23*32 },
        			128*8
        		);
        	
        	
        		/* find first empty slot to decode gfx */
        		for (gfx_index = 0; gfx_index < MAX_GFX_ELEMENTS; gfx_index++)
        			if (Machine.gfx[gfx_index] == null)
        				break;
        		if (gfx_index == MAX_GFX_ELEMENTS)
        			return 1;
        	
        		/* tweak the structure for the number of tiles we have */
        		spritelayout.total = memory_region_length(gfx_memory_region) / 128;
        		spritelayout.planeoffset[0] = plane0 * 8;
        		spritelayout.planeoffset[1] = plane1 * 8;
        		spritelayout.planeoffset[2] = plane2 * 8;
        		spritelayout.planeoffset[3] = plane3 * 8;
        	
        		/* decode the graphics */
        		Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region),spritelayout);
        		if (Machine.gfx[gfx_index]==null)
        			return 1;
        	
        		/* set the color information */
        		if (Machine.drv.color_table_len != 0)
        		{
        			Machine.gfx[gfx_index].colortable = new IntArray(Machine.remapped_colortable);
        			Machine.gfx[gfx_index].total_colors = Machine.drv.color_table_len / 16;
        		}
        		else
        		{
        			Machine.gfx[gfx_index].colortable = new IntArray(Machine.pens);
        			Machine.gfx[gfx_index].total_colors = Machine.drv.total_colors / 16;
        		}
        	
        	if ((Machine.drv.video_attributes & VIDEO_HAS_SHADOWS)==0)
        		usrintf_showmessage("driver should use VIDEO_HAS_SHADOWS");
        	
        		/* prepare shadow draw table */
        		gfx_drawmode_table[0] = DRAWMODE_NONE;
        		for (i = 1;i < 15;i++)
        			gfx_drawmode_table[i] = DRAWMODE_SOURCE;
        		gfx_drawmode_table[15] = DRAWMODE_SHADOW;
        	
        		K051960_memory_region = gfx_memory_region;
        		K051960_gfx = Machine.gfx[gfx_index];
        		K051960_callback = callback;
        		K051960_ram = new UBytePtr(0x400);
        		if (K051960_ram == null) return 1;
        		memset(K051960_ram,0,0x400);
        	
        		return 0;
        	}
        	
        	public static VhStopPtr K051960_vh_stop = new VhStopPtr() { public void handler() 
        	{
        		K051960_ram = null;
        	} };
        	
        	
        	static int K051960_fetchromdata(int _byte)
        	{
        		int[] code=new int[1],color=new int[1],pri=new int[1],shadow=new int[1];
                        int off1,addr;
        	
        	
        		addr = K051960_romoffset + (K051960_spriterombank[0] << 8) +
        				((K051960_spriterombank[1] & 0x03) << 16);
        		code[0] = (addr & 0x3ffe0) >> 5;
        		off1 = addr & 0x1f;
        		color[0] = ((K051960_spriterombank[1] & 0xfc) >> 2) + ((K051960_spriterombank[2] & 0x03) << 6);
        		pri[0] = 0;
        		shadow[0] = color[0] & 0x80;
        		(K051960_callback).handler(code,color,pri,shadow);
        	
        		addr = (code[0] << 7) | (off1 << 2) | _byte;
        		addr &= memory_region_length(K051960_memory_region)-1;
        	
        /*TODO*///	#if 0
        /*TODO*///		usrintf_showmessage("%04x: addr %06x",cpu_get_pc(),addr);
        /*TODO*///	#endif
        /*TODO*///	
        		return memory_region(K051960_memory_region).read(addr);
        	}
        	
        	public static ReadHandlerPtr K051960_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		if (K051960_readroms != 0)
        		{
        			/* the 051960 remembers the last address read and uses it when reading the sprite ROMs */
        			K051960_romoffset = (offset & 0x3fc) >> 2;
        			return K051960_fetchromdata(offset & 3);	/* only 88 Games reads the ROMs from here */
        		}
        		else
        			return K051960_ram.read(offset);
        	} };
        	
        	public static WriteHandlerPtr K051960_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K051960_ram.write(offset, data);
        	} };
        	
        /*TODO*///	READ16_HANDLER( K051960_word_r )
        /*TODO*///	{
        /*TODO*///		return K051960_r(offset*2 + 1) | (K051960_r(offset*2) << 8);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K051960_word_w )
        /*TODO*///	{
        /*TODO*///		if (ACCESSING_MSB)
        /*TODO*///			K051960_w(offset*2,(data >> 8) & 0xff);
        /*TODO*///		if (ACCESSING_LSB)
        /*TODO*///			K051960_w(offset*2 + 1,data & 0xff);
        /*TODO*///	}
                
                static int counter;
        	
        	public static ReadHandlerPtr K051937_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		if (K051960_readroms!=0 && offset >= 4 && offset < 8)
        		{
        			return K051960_fetchromdata(offset & 3);
        		}
        		else
        		{
        			if (offset == 0)
        			{
        				
        	
        				/* some games need bit 0 to pulse */
        				return (counter++) & 1;
        			}
                                logerror("%04x: read unknown 051937 address %x\n",cpu_get_pc(),offset);
        			return 0;
        		}
        	} };
        	
        	public static WriteHandlerPtr K051937_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		if (offset == 0)
        		{
        /*TODO*///	#ifdef MAME_DEBUG
        /*TODO*///	if (data & 0xc2)
        /*TODO*///		usrintf_showmessage("051937 reg 00 = %02x",data);
        /*TODO*///	#endif
        			/* bit 0 is IRQ enable */
        			K051960_irq_enabled = (data & 0x01);
        	
        			/* bit 1: probably FIRQ enable */
        	
        			/* bit 2 is NMI enable */
        			K051960_nmi_enabled = (data & 0x04);
        	
        			/* bit 3 = flip screen */
        			K051960_spriteflip = data & 0x08;
        	
        			/* bit 4 used by Devastators and TMNT, unknown */
        	
        			/* bit 5 = enable gfx ROM reading */
        			K051960_readroms = data & 0x20;
        /*TODO*///	#if VERBOSE
        /*TODO*///	logerror("%04x: write %02x to 051937 address %x\n",cpu_get_pc(),data,offset);
        /*TODO*///	#endif
        		}
        		else if (offset == 1)
        		{
        /*TODO*///	#if 0
        /*TODO*///		usrintf_showmessage("%04x: write %02x to 051937 address %x",cpu_get_pc(),data,offset);
        /*TODO*///	#endif
        	logerror("%04x: write %02x to unknown 051937 address %x\n",cpu_get_pc(),data,offset);
        		}
        		else if (offset >= 2 && offset < 5)
        		{
        			K051960_spriterombank[offset - 2] = data;
        		}
        		else
        		{
        /*TODO*///	#if 0
        /*TODO*///		usrintf_showmessage("%04x: write %02x to 051937 address %x",cpu_get_pc(),data,offset);
        /*TODO*///	#endif
        	logerror("%04x: write %02x to unknown 051937 address %x\n",cpu_get_pc(),data,offset);
        		}
        	} };
        	
        /*TODO*///	READ16_HANDLER( K051937_word_r )
        /*TODO*///	{
        /*TODO*///		return K051937_r(offset*2 + 1) | (K051937_r(offset*2) << 8);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K051937_word_w )
        /*TODO*///	{
        /*TODO*///		if (ACCESSING_MSB)
        /*TODO*///			K051937_w(offset*2,(data >> 8) & 0xff);
        /*TODO*///		if (ACCESSING_LSB)
        /*TODO*///			K051937_w(offset*2 + 1,data & 0xff);
        /*TODO*///	}
        	
        	
        	/*
        	 * Sprite Format
        	 * ------------------
        	 *
        	 * Byte | Bit(s)   | Use
        	 * -----+-76543210-+----------------
        	 *   0  | x------- | active (show this sprite)
        	 *   0  | -xxxxxxx | priority order
        	 *   1  | xxx----- | sprite size (see below)
        	 *   1  | ---xxxxx | sprite code (high 5 bits)
        	 *   2  | xxxxxxxx | sprite code (low 8 bits)
        	 *   3  | xxxxxxxx | "color", but depends on external connections (see below)
        	 *   4  | xxxxxx-- | zoom y (0 = normal, >0 = shrink)
        	 *   4  | ------x- | flip y
        	 *   4  | -------x | y position (high bit)
        	 *   5  | xxxxxxxx | y position (low 8 bits)
        	 *   6  | xxxxxx-- | zoom x (0 = normal, >0 = shrink)
        	 *   6  | ------x- | flip x
        	 *   6  | -------x | x position (high bit)
        	 *   7  | xxxxxxxx | x position (low 8 bits)
        	 *
        	 * Example of "color" field for Punk Shot:
        	 *   3  | x------- | shadow
        	 *   3  | -xx----- | priority
        	 *   3  | ---x---- | use second gfx ROM bank
        	 *   3  | ----xxxx | color code
        	 *
        	 * shadow enables transparent shadows. Note that it applies to pen 0x0f ONLY.
        	 * The rest of the sprite remains normal.
        	 * Note that Aliens also uses the shadow bit to select the second sprite bank.
        	 */
        	
        	public static void K051960_sprites_draw(mame_bitmap bitmap,int min_priority,int max_priority)
        	{
                        int NUM_SPRITES = 128;
        		int offs,pri_code;
        		int[] sortedlist = new int[NUM_SPRITES];
                        //K051960_ram.offset=0;
        	
        		for (offs = 0;offs < NUM_SPRITES;offs++)
        			sortedlist[offs] = -1;
        	
        		/* prebuild a sorted table */
        		for (offs = 0;offs < 0x400;offs += 8)
        		{
        			if ((K051960_ram.read(offs) & 0x80) != 0)
        			{
        				if (max_priority == -1)	/* draw front to back when using priority buffer */
        					sortedlist[(K051960_ram.read(offs) & 0x7f) ^ 0x7f] = offs;
        				else
        					sortedlist[K051960_ram.read(offs) & 0x7f] = offs;
        			}
        		}
        	
        		for (pri_code = 0;pri_code < NUM_SPRITES;pri_code++)
        		{
        			int ox,oy,size,w,h,x,y,flipx,flipy,zoomx,zoomy;
                                int[] code=new int[1],color=new int[1],pri=new int[1],shadow=new int[1];
        			/* sprites can be grouped up to 8x8. The draw order is
        				 0  1  4  5 16 17 20 21
        				 2  3  6  7 18 19 22 23
        				 8  9 12 13 24 25 28 29
        				10 11 14 15 26 27 30 31
        				32 33 36 37 48 49 52 53
        				34 35 38 39 50 51 54 55
        				40 41 44 45 56 57 60 61
        				42 43 46 47 58 59 62 63
        			*/
        			int xoffset[] = { 0, 1, 4, 5, 16, 17, 20, 21 };
        			int yoffset[] = { 0, 2, 8, 10, 32, 34, 40, 42 };
        			int width[] =  { 1, 2, 1, 2, 4, 2, 4, 8 };
        			int height[] = { 1, 1, 2, 2, 2, 4, 4, 8 };
        	
        	
        			offs = sortedlist[pri_code];
        			if (offs == -1) continue;
        	
        			code[0] = K051960_ram.read(offs+2) + ((K051960_ram.read(offs+1) & 0x1f) << 8);
        			color[0] = K051960_ram.read(offs+3) & 0xff;
        			pri[0] = 0;
        			shadow[0] = color[0] & 0x80;
                                //shadow[0]=0;
        			K051960_callback.handler(code,color,pri,shadow);
        	
        			if (max_priority != -1)
        				if (pri[0] < min_priority || pri[0] > max_priority) continue;
        	
        			size = (K051960_ram.read(offs+1) & 0xe0) >> 5;
        			w = width[size];
        			h = height[size];
        	
        			if (w >= 2) code[0] &= ~0x01;
        			if (h >= 2) code[0] &= ~0x02;
        			if (w >= 4) code[0] &= ~0x04;
        			if (h >= 4) code[0] &= ~0x08;
        			if (w >= 8) code[0] &= ~0x10;
        			if (h >= 8) code[0] &= ~0x20;
        	
        			ox = (256 * K051960_ram.read(offs+6) + K051960_ram.read(offs+7)) & 0x01ff;
        			oy = 256 - ((256 * K051960_ram.read(offs+4) + K051960_ram.read(offs+5)) & 0x01ff);
        			flipx = K051960_ram.read(offs+6) & 0x02;
        			flipy = K051960_ram.read(offs+4) & 0x02;
        			zoomx = (K051960_ram.read(offs+6) & 0xfc) >> 2;
        			zoomy = (K051960_ram.read(offs+4) & 0xfc) >> 2;
        			zoomx = 0x10000 / 128 * (128 - zoomx);
        			zoomy = 0x10000 / 128 * (128 - zoomy);
        	
        			if (K051960_spriteflip != 0)
        			{
        				ox = 512 - (zoomx * w >> 12) - ox;
        				oy = 256 - (zoomy * h >> 12) - oy;
        				flipx = flipx!=0?0:1;
        				flipy = flipy!=0?0:1;
        			}
                                                                        	
        			if (zoomx == 0x10000 && zoomy == 0x10000)
        			{
        				int sx,sy;
        	
        				for (y = 0;y < h;y++)
        				{
        					sy = oy + 16 * y;
        	
        					for (x = 0;x < w;x++)
        					{
        						int c = code[0];
        	
        						sx = ox + 16 * x;
        						if (flipx!=0) c += xoffset[(w-1-x)];
        						else c += xoffset[x];
        						if (flipy!=0) c += yoffset[(h-1-y)];
        						else c += yoffset[y];
        	
        						if (max_priority == -1)
        							pdrawgfx(bitmap,K051960_gfx,
        									c,
        									color[0],
        									flipx,flipy,
        									sx & 0x1ff,sy,
        									Machine.visible_area,shadow[0]!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,pri[0]);
        						else
        							drawgfx(bitmap,K051960_gfx,
        									c,
        									color[0],
        									flipx,flipy,
        									sx & 0x1ff,sy,
        									Machine.visible_area,shadow[0]!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0);
        					}
        				}
        			}
        			else
        			{
        				int sx,sy,zw,zh;
        	
        				for (y = 0;y < h;y++)
        				{
        					sy = oy + ((zoomy * y + (1<<11)) >> 12);
        					zh = (oy + ((zoomy * (y+1) + (1<<11)) >> 12)) - sy;
        	
        					for (x = 0;x < w;x++)
        					{
        						int c = code[0];
        	
        						sx = ox + ((zoomx * x + (1<<11)) >> 12);
        						zw = (ox + ((zoomx * (x+1) + (1<<11)) >> 12)) - sx;
        						if (flipx!=0) c += xoffset[(w-1-x)];
        						else c += xoffset[x];
        						if (flipy!=0) c += yoffset[(h-1-y)];
        						else c += yoffset[y];
        	
        						if (max_priority == -1)
        							pdrawgfxzoom(bitmap,K051960_gfx,
        									c,
        									color[0],
        									flipx,flipy,
        									sx & 0x1ff,sy,
        									Machine.visible_area,shadow[0]!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,
        									(zw << 16) / 16,(zh << 16) / 16,pri[0]);
        						else
        							drawgfxzoom(bitmap,K051960_gfx,
        									c,
        									color[0],
        									flipx,flipy,
        									sx & 0x1ff,sy,
        									Machine.visible_area,shadow[0]!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,
        									(zw << 16) / 16,(zh << 16) / 16);
        					}
        				}
        			}
        		}
        /*TODO*///	#if 0
        /*TODO*///	if (keyboard_pressed(KEYCODE_D))
        /*TODO*///	{
        /*TODO*///		FILE *fp;
        /*TODO*///		fp=fopen("SPRITE.DMP", "w+b");
        /*TODO*///		if (fp)
        /*TODO*///		{
        /*TODO*///			fwrite(K051960_ram, 0x400, 1, fp);
        /*TODO*///			usrintf_showmessage("saved");
        /*TODO*///			fclose(fp);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	#endif
        /*TODO*///	#undef NUM_SPRITES
        	}
        	
        	public static int K051960_is_IRQ_enabled()
        	{
        		return K051960_irq_enabled;
        	}
        	
        	public static int K051960_is_NMI_enabled()
        	{
        		return K051960_nmi_enabled;
        	}
        	
        	
        	
        	
        	public static ReadHandlerPtr K052109_051960_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		if (K052109_RMRD_line == CLEAR_LINE)
        		{
        			if (offset >= 0x3800 && offset < 0x3808)
        				return K051937_r.handler(offset - 0x3800);
        			else if (offset < 0x3c00)
        				return K052109_r.handler(offset);
        			else
        				return K051960_r.handler(offset - 0x3c00);
        		}
        		else return K052109_r.handler(offset);
        	} };
        	
        	public static WriteHandlerPtr K052109_051960_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		if (offset >= 0x3800 && offset < 0x3808)
        			K051937_w.handler(offset - 0x3800,data);
        		else if (offset < 0x3c00)
        			K052109_w.handler(offset,data);
        		else
        			K051960_w.handler(offset - 0x3c00,data);
        	} };
        	
        	
        	
        	public static abstract interface K053245_callbackProcPtr {
                    public abstract void handler(int[] code, int[] color, int[] priority);
                }
        	
        	static int K053245_memory_region = 2;
    static GfxElement K053245_gfx;
    static K053245_callbackProcPtr K053245_callback;//static void (*K053245_callback)(int *code,int *color,int *priority);
    static int K053244_romoffset, K053244_rombank;
    static int K053244_readroms;
    static int K053245_flipscreenX, K053245_flipscreenY;
    static int K053245_spriteoffsX, K053245_spriteoffsY;
    static UBytePtr K053245_ram;

    public static int K053245_vh_start(int gfx_memory_region, int plane0, int plane1, int plane2, int plane3,
            K053245_callbackProcPtr callback) {
        int gfx_index;
        GfxLayout spritelayout = new GfxLayout(
                16, 16,
                0, /* filled in later */
                4,
                new int[]{0, 0, 0, 0}, /* filled in later */
                new int[]{0, 1, 2, 3, 4, 5, 6, 7,
                    8 * 32 + 0, 8 * 32 + 1, 8 * 32 + 2, 8 * 32 + 3, 8 * 32 + 4, 8 * 32 + 5, 8 * 32 + 6, 8 * 32 + 7},
                new int[]{0 * 32, 1 * 32, 2 * 32, 3 * 32, 4 * 32, 5 * 32, 6 * 32, 7 * 32,
                    16 * 32, 17 * 32, 18 * 32, 19 * 32, 20 * 32, 21 * 32, 22 * 32, 23 * 32},
                128 * 8
        );

        /* find first empty slot to decode gfx */
        for (gfx_index = 0; gfx_index < MAX_GFX_ELEMENTS; gfx_index++) {
            if (Machine.gfx[gfx_index] == null) {
                break;
            }
        }
        if (gfx_index == MAX_GFX_ELEMENTS) {
            return 1;
        }

        /* tweak the structure for the number of tiles we have */
        spritelayout.total = memory_region_length(gfx_memory_region) / 128;
        spritelayout.planeoffset[0] = plane3 * 8;
        spritelayout.planeoffset[1] = plane2 * 8;
        spritelayout.planeoffset[2] = plane1 * 8;
        spritelayout.planeoffset[3] = plane0 * 8;

        /* decode the graphics */
        Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region), spritelayout);
        if (Machine.gfx[gfx_index] == null) {
            return 1;
        }

        /* set the color information */
        Machine.gfx[gfx_index].colortable = new IntArray(Machine.remapped_colortable);
        Machine.gfx[gfx_index].total_colors = Machine.drv.color_table_len / 16;

        K053245_memory_region = gfx_memory_region;
        K053245_gfx = Machine.gfx[gfx_index];
        K053245_callback = callback;
        K053244_rombank = 0;
        K053245_ram = new UBytePtr(0x800);
        if (K053245_ram == null) {
            return 1;
        }

        for (int i = 0; i < 0x800; i++) {
            K053245_ram.write(i, 0);//memset(K053245_ram,0,0x800);
        }
        return 0;
    }

    public static void K053245_vh_stop() {
        K053245_ram = null;
    }
    public static ReadHandlerPtr K053245_word_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return READ_WORD(K053245_ram, offset);
        }
    };
    public static WriteHandlerPtr K053245_word_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            COMBINE_WORD_MEM(K053245_ram, offset, data);
        }
    };

    public static ReadHandlerPtr K053245_r = new ReadHandlerPtr() {
        public int handler(int offset) {

            int shift = ((offset & 1) ^ 1) << 3;
            int d = (READ_WORD(K053245_ram, offset & ~1) >>> shift) & 0xff;
            
            //System.out.println(d);
            return d;

            //return (READ_WORD(&K053245_ram[offset & ~1]) >> shift) & 0xff;
        }
    };

    public static WriteHandlerPtr K053245_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int shift = ((offset & 1) ^ 1) << 3;
            offset &= ~1;
            //K053245_ram.COMBINE_WORD_MEM(offset,(char)((0xff000000 >> shift) | ((data & 0xff) << shift)));
            COMBINE_WORD_MEM(K053245_ram, offset, (0xff000000 >>> shift) | ((data & 0xff) << shift));
            
        }
    };

    public static ReadHandlerPtr K053244_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            if (K053244_readroms != 0 && offset >= 0x0c && offset < 0x10) {
                int addr;

                addr = 0x200000 * K053244_rombank + 4 * (K053244_romoffset & 0x7ffff) + ((offset & 3) ^ 1);
                addr &= memory_region_length(K053245_memory_region) - 1;

                /*#if 0
                 usrintf_showmessage("%04x: offset %02x addr %06x",cpu_get_pc(),offset&3,addr);
                 #endif*/
                return memory_region(K053245_memory_region).read(addr);
            } else {
                
                return 0;
            }
        }
    };

    public static WriteHandlerPtr K053244_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (offset == 0x00) {
                K053245_spriteoffsX = (K053245_spriteoffsX & 0x00ff) | (data << 8);
            } else if (offset == 0x01) {
                K053245_spriteoffsX = (K053245_spriteoffsX & 0xff00) | data;
            } else if (offset == 0x02) {
                K053245_spriteoffsY = (K053245_spriteoffsY & 0x00ff) | (data << 8);
            } else if (offset == 0x03) {
                K053245_spriteoffsY = (K053245_spriteoffsY & 0xff00) | data;
            } else if (offset == 0x05) {
                /*#ifdef MAME_DEBUG
                 if (data & 0xc8)
                 usrintf_showmessage("053244 reg 05 = %02x",data);
                 #endif*/
                /* bit 0/1 = flip screen */
                K053245_flipscreenX = data & 0x01;
                K053245_flipscreenY = data & 0x02;

                /* bit 2 = unknown, Parodius uses it */

                /* bit 4 = enable gfx ROM reading */
                K053244_readroms = data & 0x10;

                /* bit 5 = unknown, Rollergames uses it */
                /*#if VERBOSE
                 if (errorlog) fprintf(errorlog,"%04x: write %02x to 053244 address 5\n",cpu_get_pc(),data);
                 #endif*/
            } else if (offset >= 0x08 && offset < 0x0c) {
                offset = 8 * ((offset & 0x03) ^ 0x01);
                K053244_romoffset = (K053244_romoffset & ~(0xff << offset)) | (data << offset);
                return;
            } 
        }
    };

    public static void K053244_bankselect(int bank) /* used by TMNT2 for ROM testing */ {
        K053244_rombank = bank;
    }
        	
        	/*
        	 * Sprite Format
        	 * ------------------
        	 *
        	 * Word | Bit(s)           | Use
        	 * -----+-fedcba9876543210-+----------------
        	 *   0  | x--------------- | active (show this sprite)
        	 *   0  | -x-------------- | maintain aspect ratio (when set, zoom y acts on both axis)
        	 *   0  | --x------------- | flip y
        	 *   0  | ---x------------ | flip x
        	 *   0  | ----xxxx-------- | sprite size (see below)
        	 *   0  | ---------xxxxxxx | priority order
        	 *   1  | --xxxxxxxxxxxxxx | sprite code. We use an additional bit in TMNT2, but this is
        	 *                           probably not accurate (protection related so we can't verify)
        	 *   2  | ------xxxxxxxxxx | y position
        	 *   3  | ------xxxxxxxxxx | x position
        	 *   4  | xxxxxxxxxxxxxxxx | zoom y (0x40 = normal, <0x40 = enlarge, >0x40 = reduce)
        	 *   5  | xxxxxxxxxxxxxxxx | zoom x (0x40 = normal, <0x40 = enlarge, >0x40 = reduce)
        	 *   6  | ------x--------- | mirror y (top half is drawn as mirror image of the bottom)
        	 *   6  | -------x-------- | mirror x (right half is drawn as mirror image of the left)
        	 *   6  | --------x------- | shadow
        	 *   6  | ---------xxxxxxx | "color", but depends on external connections
        	 *   7  | ---------------- |
        	 *
        	 * shadow enables transparent shadows. Note that it applies to pen 0x0f ONLY.
        	 * The rest of the sprite remains normal.
        	 */
        	
        	public static void K053245_sprites_draw(mame_bitmap bitmap)
        	{
                        int NUM_SPRITES = 128;
        int offs, pri_code;
        int[] sortedlist = new int[NUM_SPRITES];

        for (offs = 0; offs < NUM_SPRITES; offs++) {
            sortedlist[offs] = -1;
        }

        /* prebuild a sorted table */
        for (offs = 0; offs < 0x800; offs += 16) {
            if ((READ_WORD(K053245_ram, offs) & 0x8000) != 0) {
                sortedlist[READ_WORD(K053245_ram, offs) & 0x007f] = offs;
            }
        }

        for (pri_code = 0; pri_code < NUM_SPRITES; pri_code++) {
            int ox, oy, size, w, h, x, y, flipx, flipy, mirrorx, mirrory, zoomx, zoomy;
            int[] color = new int[1];
            int[] code = new int[1];
            int[] pri = new int[1];

            offs = sortedlist[pri_code];
            if (offs == -1) {
                continue;
            }

            /* the following changes the sprite draw order from
             0  1  4  5 16 17 20 21
             2  3  6  7 18 19 22 23
             8  9 12 13 24 25 28 29
             10 11 14 15 26 27 30 31
             32 33 36 37 48 49 52 53
             34 35 38 39 50 51 54 55
             40 41 44 45 56 57 60 61
             42 43 46 47 58 59 62 63

             to

             0  1  2  3  4  5  6  7
             8  9 10 11 12 13 14 15
             16 17 18 19 20 21 22 23
             24 25 26 27 28 29 30 31
             32 33 34 35 36 37 38 39
             40 41 42 43 44 45 46 47
             48 49 50 51 52 53 54 55
             56 57 58 59 60 61 62 63
             */

            /* NOTE: from the schematics, it looks like the top 2 bits should be ignored */
            /* (there are not output pins for them), and probably taken from the "color" */
            /* field to do bank switching. However this applies only to TMNT2, with its */
            /* protection mcu creating the sprite table, so we don't know where to fetch */
            /* the bits from. */
            code[0] = READ_WORD(K053245_ram, offs + 0x02);
            code[0] = ((code[0] & 0xffe1) + ((code[0] & 0x0010) >> 2) + ((code[0] & 0x0008) << 1)
                    + ((code[0] & 0x0004) >> 1) + ((code[0] & 0x0002) << 2));
            color[0] = READ_WORD(K053245_ram, offs + 0x0c) & 0x00ff;
            pri[0] = 0;

            (K053245_callback).handler(code, color, pri);

            /*if (pri[0] < min_priority || pri[0] > max_priority) {
                continue;
            }*/

            size = (READ_WORD(K053245_ram, offs) & 0x0f00) >> 8;

            w = 1 << (size & 0x03);
            h = 1 << ((size >> 2) & 0x03);

            /* zoom control:
             0x40 = normal scale
             <0x40 enlarge (0x20 = double size)
             >0x40 reduce (0x80 = half size)
             */
            zoomy = READ_WORD(K053245_ram, offs + 0x08);
            if (zoomy > 0x2000) {
                continue;
            }
            if (zoomy != 0) {
                zoomy = (0x400000 + zoomy / 2) / zoomy;
            } else {
                zoomy = 2 * 0x400000;
            }
            if ((READ_WORD(K053245_ram, offs) & 0x4000) == 0) {
                zoomx = READ_WORD(K053245_ram, offs + 0x0a);
                if (zoomx > 0x2000) {
                    continue;
                }
                if (zoomx != 0) {
                    zoomx = (0x400000 + zoomx / 2) / zoomx;
                } //			else zoomx = 2 * 0x400000;
                else {
                    zoomx = zoomy; /* workaround for TMNT2 */

                }
            } else {
                zoomx = zoomy;
            }

            ox = READ_WORD(K053245_ram, offs + 0x06) + K053245_spriteoffsX;
            oy = READ_WORD(K053245_ram, offs + 0x04);

            flipx = READ_WORD(K053245_ram, offs) & 0x1000;
            flipy = READ_WORD(K053245_ram, offs) & 0x2000;
            mirrorx = READ_WORD(K053245_ram, offs + 0x0c) & 0x0100;
            mirrory = READ_WORD(K053245_ram, offs + 0x0c) & 0x0200;

            if (K053245_flipscreenX != 0) {
                ox = 512 - ox;
                if (mirrorx == 0) {
                    flipx = flipx!=0?0:1;
                }
            }
            if (K053245_flipscreenY != 0) {
                oy = -oy;
                if (mirrory == 0) {
                    flipy = flipy!=0?0:1;
                }
            }

            ox = (ox + 0x5d) & 0x3ff;
            if (ox >= 768) {
                ox -= 1024;
            }
            oy = (-(oy + K053245_spriteoffsY + 0x07)) & 0x3ff;
            if (oy >= 640) {
                oy -= 1024;
            }

            /* the coordinates given are for the *center* of the sprite */
            ox -= (zoomx * w) >> 13;
            oy -= (zoomy * h) >> 13;

            for (y = 0; y < h; y++) {
                int sx, sy, zw, zh;

                sy = oy + ((zoomy * y + (1 << 11)) >> 12);
                zh = (oy + ((zoomy * (y + 1) + (1 << 11)) >> 12)) - sy;

                for (x = 0; x < w; x++) {
                    int c, fx, fy;

                    sx = ox + ((zoomx * x + (1 << 11)) >> 12);
                    zw = (ox + ((zoomx * (x + 1) + (1 << 11)) >> 12)) - sx;
                    c = code[0];
                    if (mirrorx != 0) {
                        if ((flipx == 0) ^ (2 * x < w)) {
                            /* mirror left/right */
                            c += (w - x - 1);
                            fx = 1;
                        } else {
                            c += x;
                            fx = 0;
                        }
                    } else {
                        if (flipx != 0) {
                            c += w - 1 - x;
                        } else {
                            c += x;
                        }
                        fx = flipx;
                    }
                    if (mirrory != 0) {
                        if ((flipy == 0) ^ (2 * y >= h)) {
                            /* mirror top/bottom */
                            c += 8 * (h - y - 1);
                            fy = 1;
                        } else {
                            c += 8 * y;
                            fy = 0;
                        }
                    } else {
                        if (flipy != 0) {
                            c += 8 * (h - 1 - y);
                        } else {
                            c += 8 * y;
                        }
                        fy = flipy;
                    }

                    /* the sprite can start at any point in the 8x8 grid, but it must stay */
                    /* in a 64 entries window, wrapping around at the edges. The animation */
                    /* at the end of the saloon level in SUnset Riders breaks otherwise. */
                    c = (c & 0x3f) | (code[0] & ~0x3f);

                    if (zoomx == 0x10000 && zoomy == 0x10000) {
                        /* hack to simulate shadow */
                        if ((READ_WORD(K053245_ram, offs + 0x0c) & 0x0080) != 0) {
                            int o = K053245_gfx.colortable.read(16 * color[0] + 15);
                            K053245_gfx.colortable.write(16 * color[0] + 15, TRANSPARENCY_PEN);
                            drawgfx(bitmap, K053245_gfx, c, color[0],
                                    fx, fy,
                                    sx, sy,
                                    Machine.visible_area, TRANSPARENCY_PENS, (cpu_getcurrentframe() & 1) != 0 ? 0x8001 : 0x0001);
                            K053245_gfx.colortable.write(16 * color[0] + 15, o);
                        } else {
                            drawgfx(bitmap, K053245_gfx, c, color[0],
                                    fx, fy,
                                    sx, sy,
                                    Machine.visible_area, TRANSPARENCY_PEN, 0);
                        }
                    } else {
                        drawgfxzoom(bitmap, K053245_gfx, c, color[0],
                                fx, fy,
                                sx, sy,
                                Machine.visible_area, TRANSPARENCY_PEN, 0,
                                (zw << 16) / 16, (zh << 16) / 16);
                    }
                }
            }
        }
    }
        	
        	
        	
        	
        	static int K053247_memory_region, K053247_dx, K053247_dy;
	static int[] K053246_regs=new int[8];
	static UBytePtr K053247_ram;
	static GfxElement K053247_gfx;
	static K053247_callbackProcPtr K053247_callback;
	static int K053246_OBJCHA_line;
	
	
	public static int K053247_vh_start(int gfx_memory_region, int dx, int dy, int plane0,int plane1,int plane2,int plane3,
						 K053247_callbackProcPtr callback)
	{
		int gfx_index,i;
		GfxLayout spritelayout = new GfxLayout(
                        16, 16,
                        0, /* filled in later */
                        4,
                        new int[]{0, 0, 0, 0}, /* filled in later */
                        new int[]{2 * 4, 3 * 4, 0 * 4, 1 * 4, 6 * 4, 7 * 4, 4 * 4, 5 * 4,
                            10 * 4, 11 * 4, 8 * 4, 9 * 4, 14 * 4, 15 * 4, 12 * 4, 13 * 4},
                        new int[]{0 * 64, 1 * 64, 2 * 64, 3 * 64, 4 * 64, 5 * 64, 6 * 64, 7 * 64,
                            8 * 64, 9 * 64, 10 * 64, 11 * 64, 12 * 64, 13 * 64, 14 * 64, 15 * 64},
                        128 * 8
                );
	
	
		/* find first empty slot to decode gfx */
		for (gfx_index = 0; gfx_index < MAX_GFX_ELEMENTS; gfx_index++)
			if (Machine.gfx[gfx_index] == null)
				break;
		if (gfx_index == MAX_GFX_ELEMENTS)
			return 1;
	
		/* tweak the structure for the number of tiles we have */
		spritelayout.total = memory_region_length(gfx_memory_region) / 128;
		spritelayout.planeoffset[0] = plane0;
		spritelayout.planeoffset[1] = plane1;
		spritelayout.planeoffset[2] = plane2;
		spritelayout.planeoffset[3] = plane3;
	
		/* decode the graphics */
		Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region),spritelayout);
		if (Machine.gfx[gfx_index]==null)
			return 1;
	
		/* set the color information */
		if (Machine.drv.color_table_len != 0)
		{
			Machine.gfx[gfx_index].colortable = new IntArray(Machine.remapped_colortable);
			Machine.gfx[gfx_index].total_colors = Machine.drv.color_table_len / 16;
		}
		else
		{
			Machine.gfx[gfx_index].colortable = new IntArray(Machine.pens);
			Machine.gfx[gfx_index].total_colors = Machine.drv.total_colors / 16;
		}
	
	if ((Machine.drv.video_attributes & VIDEO_HAS_SHADOWS)==0)
		usrintf_showmessage("driver should use VIDEO_HAS_SHADOWS");
	
		/* prepare shadow draw table */
		gfx_drawmode_table[0] = DRAWMODE_NONE;
		for (i = 1;i < 15;i++)
			gfx_drawmode_table[i] = DRAWMODE_SOURCE;
		gfx_drawmode_table[15] = DRAWMODE_SHADOW;
	
		K053247_dx = dx;
		K053247_dy = dy;
		K053247_memory_region = gfx_memory_region;
		K053247_gfx = Machine.gfx[gfx_index];
		K053247_callback = callback;
		K053246_OBJCHA_line = CLEAR_LINE;
		K053247_ram = new UBytePtr(0x1000);
		if (K053247_ram == null) return 1;
	
		memset(K053247_ram,  0, 0x1000);
		memset(K053246_regs, 0, 8);
	
		return 0;
	}

    public static void K053247_vh_stop() {
        K053247_ram = null;
    }
    public static ReadHandlerPtr K053247_word_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int read = K053247_ram.read(offset + 1) << 8 | K053247_ram.read(offset);            
            
            return read;
        }
    };
    public static WriteHandlerPtr K053247_word_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
/*TODO*///            COMBINE_WORD_MEM(K053247_ram, offset, data);
        }
    };
    public static ReadHandlerPtr K053247_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int shift = ((offset & 1) ^ 1) << 3;
            int read = ((K053247_ram.read((offset & ~1) + 1) << 8 | K053247_ram.read(offset & ~1)) >>> shift) & 0xff;  //unsigned shift?
            
            return read;
        }
    };
    
    public static int READ_WORD(UBytePtr a, int index) {
            //return (char)(((memory[offset + 1 + index] << 8)& 0xFF) | (memory[offset + index]& 0xFF));
            /*return (char)((( memory[offset+index]&0xFF) << 0)
             | (( memory[offset+index + 1]&0xFF) << 8));*/
            return a.memory[a.offset + index + 1] << 8 | a.memory[a.offset + index];

        }
    
    public static void WRITE_WORD(UBytePtr a, int index, int value) {
            a.memory[a.offset + index + 1] = (char) (value >> 8 & 0xFF);
            a.memory[a.offset + index] = (char) (value & 0xFF);
            //memory[offset + index] = (char)(value & 0xFF);
            //memory[offset + index+ 1] = (char)((value >> 8)&0xFF);
        }
    
    public static void COMBINE_WORD_MEM(UBytePtr a, int offset,int d)
        {
            WRITE_WORD(a, offset, (READ_WORD(a, offset) & ((d) >> 16) | d));
            
        }
    
    public static WriteHandlerPtr K053247_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int shift = ((offset & 1) ^ 1) << 3;
            offset &= ~1;
            COMBINE_WORD_MEM(K053247_ram, offset, (0xff000000 >>> shift) | ((data & 0xff) << shift));//unsigned shift
        }
    };

    public static ReadHandlerPtr K053246_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            if (K053246_OBJCHA_line == ASSERT_LINE)
		{
			int addr;
	
	
			addr = (K053246_regs[6] << 17) | (K053246_regs[7] << 9) | (K053246_regs[4] << 1) | ((offset & 1) ^ 1);
			addr &= memory_region_length(K053247_memory_region)-1;
	
			return memory_region(K053247_memory_region).read(addr);
		}
		else
		{
                        logerror("%04x: read from unknown 053246 address %x\n",cpu_get_pc(),offset);
			return 0;
		}
        }
    };

    public static WriteHandlerPtr K053246_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            K053246_regs[offset] = data;
        }
    };
    public static ReadHandlerPtr K053246_word_r = new ReadHandlerPtr() {
        public int handler(int offset) {

            return K053246_r.handler(offset + 1) | (K053246_r.handler(offset) << 8);
        }
    };
    public static WriteHandlerPtr K053246_word_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {

            if ((data & 0xff000000) == 0) {
                K053246_w.handler(offset, (data >>> 8) & 0xff);
            }
            if ((data & 0x00ff0000) == 0) {
                K053246_w.handler(offset + 1, data & 0xff);
            }
        }
    };

    public static void K053246_set_OBJCHA_line(int state) {
        K053246_OBJCHA_line = state;
    }

    /*
     * Sprite Format
     * ------------------
     *
     * Word | Bit(s)           | Use
     * -----+-fedcba9876543210-+----------------
     *   0  | x--------------- | active (show this sprite)
     *   0  | -x-------------- | maintain aspect ratio (when set, zoom y acts on both axis)
     *   0  | --x------------- | flip y
     *   0  | ---x------------ | flip x
     *   0  | ----xxxx-------- | sprite size (see below)
     *   0  | ---------xxxxxxx | priority order
     *   1  | xxxxxxxxxxxxxxxx | sprite code
     *   2  | ------xxxxxxxxxx | y position
     *   3  | ------xxxxxxxxxx | x position
     *   4  | xxxxxxxxxxxxxxxx | zoom y (0x40 = normal, <0x40 = enlarge, >0x40 = reduce)
     *   5  | xxxxxxxxxxxxxxxx | zoom x (0x40 = normal, <0x40 = enlarge, >0x40 = reduce)
     *   6  | x--------------- | mirror y (top half is drawn as mirror image of the bottom)
     *   6  | -x-------------- | mirror x (right half is drawn as mirror image of the left)
     *   6  | -----x---------- | shadow
     *   6  | xxxxxxxxxxxxxxxx | "color", but depends on external connections
     *   7  | ---------------- |
     *
     * shadow enables transparent shadows. Note that it applies to pen 0x0f ONLY.
     * The rest of the sprite remains normal.
     */
    static int offsetkludge;

    public static void K053247_sprites_draw(mame_bitmap bitmap) {
        //System.out.println("K053247_sprites_draw");
        //K053247_ram.offset=0;
        int NUM_SPRITES = 256;
        int offs,pri_code;
		int[] sortedlist = new int[NUM_SPRITES];
	
		int flipscreenx = K053246_regs[5] & 0x01;
		int flipscreeny = K053246_regs[5] & 0x02;
		int offx = (K053246_regs[0] << 8) | K053246_regs[1];
		int offy = (K053246_regs[2] << 8) | K053246_regs[3];
	
		for (offs = 0;offs < NUM_SPRITES;offs++)
			sortedlist[offs] = -1;
	
	
		/* prebuild a sorted table */
		for (offs = 0;offs < 0x1000;offs += 16)
		{
	//		if (K053247_ram[offs] & 0x8000)
			sortedlist[READ_WORD(K053247_ram, offs) & 0x00ff] = offs;
		}
                
                //System.out.println("A");
	
		for (pri_code = 0;pri_code < NUM_SPRITES;pri_code++)
		{
			//System.out.println(pri_code);
                        int ox,oy,size,w,h,x,y,xa,ya,flipx,flipy,mirrorx,mirrory,zoomx,zoomy, shadow;
                        int[] color=new int[1],code=new int[1],pri=new int[1]; 
			/* sprites can be grouped up to 8x8. The draw order is
				 0  1  4  5 16 17 20 21
				 2  3  6  7 18 19 22 23
				 8  9 12 13 24 25 28 29
				10 11 14 15 26 27 30 31
				32 33 36 37 48 49 52 53
				34 35 38 39 50 51 54 55
				40 41 44 45 56 57 60 61
				42 43 46 47 58 59 62 63
			*/
			int xoffset[] = { 0, 1, 4, 5, 16, 17, 20, 21 };
			int yoffset[] = { 0, 2, 8, 10, 32, 34, 40, 42 };
                        
	
	//System.out.println("B");
			offs = sortedlist[pri_code];
			if (offs == -1) continue;
                        
                        //System.out.println("C");
	
			if ((READ_WORD(K053247_ram, offs) & 0x8000) == 0) continue;
                        
                        //System.out.println("D");
	
			code[0] = READ_WORD(K053247_ram, offs + 0x02);
			color[0] = READ_WORD(K053247_ram, offs + 0x0c);
			pri[0] = 0;
	
			K053247_callback.handler(code,color,pri);
	
			size = (READ_WORD(K053247_ram, offs) & 0x0f00) >> 8;
	
			w = 1 << (size & 0x03);
			h = 1 << ((size >> 2) & 0x03);
                        
                        //System.out.println("w="+w);
                        //System.out.println("h="+h);
	
			/* the sprite can start at any point in the 8x8 grid. We have to */
			/* adjust the offsets to draw it correctly. Simpsons does this all the time. */
			xa = 0;
			ya = 0;
			if ((code[0] & 0x01)!=0) xa += 1;
			if ((code[0] & 0x02)!=0) ya += 1;
			if ((code[0] & 0x04)!=0) xa += 2;
			if ((code[0] & 0x08)!=0) ya += 2;
			if ((code[0] & 0x10)!=0) xa += 4;
			if ((code[0] & 0x20)!=0) ya += 4;
			code[0] &= ~0x3f;
	
	
			/* zoom control:
			   0x40 = normal scale
			  <0x40 enlarge (0x20 = double size)
			  >0x40 reduce (0x80 = half size)
			*/
			zoomy = READ_WORD(K053247_ram, offs + 0x08);
			if (zoomy > 0x2000) continue;
			if (zoomy!=0) zoomy = (0x400000+zoomy/2) / zoomy;
			else zoomy = 2 * 0x400000;
			if ((READ_WORD(K053247_ram, offs) & 0x4000) == 0) {
                                zoomx = READ_WORD(K053247_ram, offs + 0x0a);
				if (zoomx > 0x2000) continue;
				if (zoomx!=0) zoomx = (0x400000+zoomx/2) / zoomx;
				else zoomx = 2 * 0x400000;
			}
			else zoomx = zoomy;
	
			ox = READ_WORD(K053247_ram, offs + 0x06);
                        oy = READ_WORD(K053247_ram, offs + 0x04);
	
			flipx = READ_WORD(K053247_ram, offs) & 0x1000;
			flipy = READ_WORD(K053247_ram, offs) & 0x2000;
			mirrorx = READ_WORD(K053247_ram, offs+6) & 0x4000;
			mirrory = READ_WORD(K053247_ram, offs+6) & 0x8000;
			shadow = READ_WORD(K053247_ram, offs+6) & 0x0400;
	
			if (flipscreenx!=0)
			{
				ox = -ox;
				if (mirrorx == 0) flipx = flipx!=0?0:1;
			}
			if (flipscreeny!=0)
			{
				oy = -oy;
				if (mirrory == 0) flipy = flipy!=0?0:1;
			}
	
	
			ox = (K053247_dx + ox - offx) & 0x3ff;
			if (ox >= 0x300) ox -= 0x400;
			oy = (-(K053247_dy + oy + offy)) & 0x3ff;
			if (oy >= 0x280) oy -= 0x400;
	
			/* the coordinates given are for the *center* of the sprite */
			ox -= (zoomx * w) >> 13;
			oy -= (zoomy * h) >> 13;
                        
                        //System.out.println("h="+h);
	
			for (y = 0;y < h;y++)
			{
				int sx,sy,zw,zh;
	
				sy = oy + ((zoomy * y + (1<<11)) >> 12);
				zh = (oy + ((zoomy * (y+1) + (1<<11)) >> 12)) - sy;
	
				for (x = 0;x < w;x++)
				{
					int c,fx,fy;
	
					sx = ox + ((zoomx * x + (1<<11)) >> 12);
                                        //System.out.println("x="+sx);
					zw = (ox + ((zoomx * (x+1) + (1<<11)) >> 12)) - sx;
					c = code[0];
					if (mirrorx!=0)
					{
						if ((flipx == 0) ^ (2*x < w))
						{
							/* mirror left/right */
							c += xoffset[(w-1-x+xa)&7];
							fx = 1;
						}
						else
						{
							c += xoffset[(x+xa)&7];
							fx = 0;
						}
					}
					else
					{
						if (flipx!=0) c += xoffset[(w-1-x+xa)&7];
						else c += xoffset[(x+xa)&7];
						fx = flipx;
					}
					if (mirrory!=0)
					{
						if ((flipy == 0) ^ (2*y >= h))
						{
							/* mirror top/bottom */
							c += yoffset[(h-1-y+ya)&7];
							fy = 1;
						}
						else
						{
							c += yoffset[(y+ya)&7];
							fy = 0;
						}
					}
					else
					{
						if (flipy!=0) c += yoffset[(h-1-y+ya)&7];
						else c += yoffset[(y+ya)&7];
						fy = flipy;
					}
                                        
                                        //System.out.println("zoomx="+zoomx);
	
					if (zoomx == 0x10000 && zoomy == 0x10000)
					{
                                            //System.out.println("pdrawgfx");
						pdrawgfx(bitmap,K053247_gfx,
								c,
								color[0],
								fx,fy,
								sx,sy,
								new rectangle(Machine.visible_area),shadow!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,pri[0]);
					}
					else
					{
						//System.out.println("pdrawgfxzoom");
                                                pdrawgfxzoom(bitmap,K053247_gfx,
								c,
								color[0],
								fx,fy,
								sx,sy,
								new rectangle(Machine.visible_area),shadow!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,
								(zw << 16) / 16,(zh << 16) / 16,pri[0]);
					}
	
					if (mirrory!=0 && h == 1)  /* Simpsons shadows */
					{
						if (zoomx == 0x10000 && zoomy == 0x10000)
						{
                                                    //System.out.println("pdrawgfx2");
							pdrawgfx(bitmap,K053247_gfx,
									c,
									color[0],
									fx,fy!=0?0:1,
									sx,sy,
									new rectangle(Machine.visible_area),shadow!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,pri[0]);
						}
						else
						{
                                                    //System.out.println("pdrawgfxzoom2");
							pdrawgfxzoom(bitmap,K053247_gfx,
									c,
									color[0],
									fx,fy!=0?0:1,
									sx,sy,
									new rectangle(Machine.visible_area),shadow!=0 ? TRANSPARENCY_PEN_TABLE : TRANSPARENCY_PEN,0,
									(zw << 16) / 16,(zh << 16) / 16,pri[0]);
						}
					}
				}
			}
		}
    }

        	public static int K053246_is_IRQ_enabled() {
                    return K053246_regs[5] & 0x10;
                }
        	
        	public static int MAX_K051316 = 3;
        
        	static int[] K051316_memory_region = new int[MAX_K051316];
        	static int[] K051316_gfxnum = new int[MAX_K051316];
        	static int[] K051316_wraparound = new int[MAX_K051316];
        	static int[][] K051316_offset = new int[MAX_K051316][2];
        	static int[] K051316_bpp = new int[MAX_K051316];
        	static K051316_callbackProcPtr[] K051316_callback = new K051316_callbackProcPtr[MAX_K051316]; //(int *code,int *color);
        	static UBytePtr[] K051316_ram = new UBytePtr[MAX_K051316];
        	static int[][] K051316_ctrlram = new int[MAX_K051316][16];
        	static struct_tilemap[] K051316_tilemap = new struct_tilemap[MAX_K051316];
        /*TODO*///	
        /*TODO*///	void K051316_vh_stop(int chip);
        	
        	/***************************************************************************
        	
        	  Callbacks for the TileMap code
        	
        	***************************************************************************/
        	
        	public static void K051316_get_tile_info(int tile_index,int chip)
        	{
                        int[] code = new int[1];
                        int[] color = new int[1];
                        
        		code[0] = K051316_ram[chip].read(tile_index);
        		color[0] = K051316_ram[chip].read(tile_index + 0x400);
        	
        		tile_info.flags = 0;
        	
        		(K051316_callback[chip]).handler(code,color);
        	
        		SET_TILE_INFO(
        				K051316_gfxnum[chip],
        				code[0],
        				color[0],
        				tile_info.flags);
        	}
        	
        	static GetTileInfoPtr K051316_get_tile_info0 = new GetTileInfoPtr() { public void handler(int tile_index) { K051316_get_tile_info(tile_index,0); }};
                static GetTileInfoPtr K051316_get_tile_info1 = new GetTileInfoPtr() { public void handler(int tile_index) { K051316_get_tile_info(tile_index,1); }};
        	static GetTileInfoPtr K051316_get_tile_info2 = new GetTileInfoPtr() { public void handler(int tile_index) { K051316_get_tile_info(tile_index,2); }};
        	
        	
        	public static int K051316_vh_start(int chip, int gfx_memory_region,int bpp,
        			int tilemap_type,int transparent_pen,
        			K051316_callbackProcPtr callback)
        	{
        		int gfx_index;
        		GetTileInfoPtr[] get_tile_info = { K051316_get_tile_info0,K051316_get_tile_info1,K051316_get_tile_info2 };
        	
        		/* find first empty slot to decode gfx */
        		for (gfx_index = 0; gfx_index < MAX_GFX_ELEMENTS; gfx_index++)
        			if (Machine.gfx[gfx_index] == null)
        				break;
        		if (gfx_index == MAX_GFX_ELEMENTS)
        			return 1;
        	
        		if (bpp == 4)
        		{
        			GfxLayout charlayout = new GfxLayout
        			(
        				16,16,
        				0,				/* filled in later */
        				4,
        				new int[] { 0, 1, 2, 3 },
        				new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
        						8*4, 9*4, 10*4, 11*4, 12*4, 13*4, 14*4, 15*4 },
        				new int[] { 0*64, 1*64, 2*64, 3*64, 4*64, 5*64, 6*64, 7*64,
        						8*64, 9*64, 10*64, 11*64, 12*64, 13*64, 14*64, 15*64 },
        				128*8
        			);
        	
        	
        			/* tweak the structure for the number of tiles we have */
        			charlayout.total = memory_region_length(gfx_memory_region) / 128;
        	
        			/* decode the graphics */
        			Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region),charlayout);
        		}
        		else if (bpp == 7 || bpp == 8)
        		{
        			GfxLayout charlayout = new GfxLayout(
                    16, 16,
                    0, /* filled in later */
                    7,
                    new int[]{1, 2, 3, 4, 5, 6, 7},
                    new int[]{0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8,
                        8 * 8, 9 * 8, 10 * 8, 11 * 8, 12 * 8, 13 * 8, 14 * 8, 15 * 8},
                    new int[]{0 * 128, 1 * 128, 2 * 128, 3 * 128, 4 * 128, 5 * 128, 6 * 128, 7 * 128,
                        8 * 128, 9 * 128, 10 * 128, 11 * 128, 12 * 128, 13 * 128, 14 * 128, 15 * 128},
                    256 * 8
            );


            /* tweak the structure for the number of tiles we have */
            charlayout.total = memory_region_length(gfx_memory_region) / 256;

            /* decode the graphics */
            Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region), charlayout);
        		}
        		else
        		{
                                logerror("K051316_vh_start supports only 4, 7 and 8 bpp\n");
        			return 1;
        		}
        	
        		if (Machine.gfx[gfx_index]==null)
        			return 1;
        	
        		/* set the color information */
        		if (Machine.drv.color_table_len != 0)
        		{
        			Machine.gfx[gfx_index].colortable = new IntArray(Machine.remapped_colortable);
        			Machine.gfx[gfx_index].total_colors = Machine.drv.color_table_len / (1 << bpp);
        		}
        		else
        		{
        			Machine.gfx[gfx_index].colortable = new IntArray(Machine.pens);
        			Machine.gfx[gfx_index].total_colors = Machine.drv.total_colors / (1 << bpp);
        		}
        	
        		K051316_memory_region[chip] = gfx_memory_region;
        		K051316_gfxnum[chip] = gfx_index;
        		K051316_bpp[chip] = bpp;
        		K051316_callback[chip] = callback;
        	
        		K051316_tilemap[chip] = tilemap_create(get_tile_info[chip],tilemap_scan_rows,tilemap_type,16,16,32,32);
        	
        		K051316_ram[chip] = new UBytePtr(0x800);
        	
        		if (K051316_ram[chip]==null || K051316_tilemap[chip]==null)
        		{
        			K051316_vh_stop(chip);
        			return 1;
        		}
        	
        		tilemap_set_transparent_pen(K051316_tilemap[chip],transparent_pen);
        	
        		K051316_wraparound[chip] = 0;	/* default = no wraparound */
        		K051316_offset[chip][0] = K051316_offset[chip][1] = 0;
        	
        		return 0;
        	}
        	
        	public static int K051316_vh_start_0(int gfx_memory_region,int bpp,
        			int tilemap_type,int transparent_pen,
        			K051316_callbackProcPtr callback)
        	{
        		return K051316_vh_start(0,gfx_memory_region,bpp,tilemap_type,transparent_pen,callback);
        	}
        	
        	public static int K051316_vh_start_1(int gfx_memory_region,int bpp,
        			int tilemap_type,int transparent_pen,
        			K051316_callbackProcPtr callback)
        	{
        		return K051316_vh_start(1,gfx_memory_region,bpp,tilemap_type,transparent_pen,callback);
        	}
        	
        /*TODO*///	int K051316_vh_start_2(int gfx_memory_region,int bpp,
        /*TODO*///			int tilemap_type,int transparent_pen,
        /*TODO*///			void (*callback)(int *code,int *color))
        /*TODO*///	{
        /*TODO*///		return K051316_vh_start(2,gfx_memory_region,bpp,tilemap_type,transparent_pen,callback);
        /*TODO*///	}
        /*TODO*///	
        	
        	public static void K051316_vh_stop(int chip)
        	{
        		K051316_ram[chip] = null;
        	}
        	
        	public static VhStopPtr K051316_vh_stop_0 = new VhStopPtr() { public void handler() 
        	{
        		K051316_vh_stop(0);
        	} };
        	
        	public static VhStopPtr K051316_vh_stop_1 = new VhStopPtr() { public void handler() 
        	{
        		K051316_vh_stop(1);
        	} };
        	
        /*TODO*///	public static VhStopPtr K051316_vh_stop_2 = new VhStopPtr() { public void handler() 
        /*TODO*///	{
        /*TODO*///		K051316_vh_stop(2);
        /*TODO*///	} };
        	
        	public static int K051316_r(int chip, int offset)
        	{
        		return K051316_ram[chip].read(offset);
        	}
        	
        	public static ReadHandlerPtr K051316_0_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		return K051316_r(0, offset);
        	} };
        	
        	public static ReadHandlerPtr K051316_1_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		return K051316_r(1, offset);
        	} };
        	
        /*TODO*///	public static ReadHandlerPtr K051316_2_r  = new ReadHandlerPtr() { public int handler(int offset)
        /*TODO*///	{
        /*TODO*///		return K051316_r(2, offset);
        /*TODO*///	} };
        	
        	
        	public static void K051316_w(int chip,int offset,int data)
        	{
        		if (K051316_ram[chip].read(offset) != data)
        		{
        			K051316_ram[chip].write(offset, data);
        			tilemap_mark_tile_dirty(K051316_tilemap[chip],offset & 0x3ff);
        		}
        	}
        	
        	public static WriteHandlerPtr K051316_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K051316_w(0,offset,data);
        	} };
        	
        	public static WriteHandlerPtr K051316_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K051316_w(1,offset,data);
        	} };
        /*TODO*///	
        /*TODO*///	public static WriteHandlerPtr K051316_2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        /*TODO*///	{
        /*TODO*///		K051316_w(2,offset,data);
        /*TODO*///	} };
        	
        	
        	static int K051316_rom_r(int chip, int offset)
        	{
        		if ((K051316_ctrlram[chip][0x0e] & 0x01) == 0)
        		{
        			int addr;
        	
        			addr = offset + (K051316_ctrlram[chip][0x0c] << 11) + (K051316_ctrlram[chip][0x0d] << 19);
        			if (K051316_bpp[chip] <= 4) addr /= 2;
        			addr &= memory_region_length(K051316_memory_region[chip])-1;
        	
        /*TODO*///	#if 0
        /*TODO*///		usrintf_showmessage("%04x: offset %04x addr %04x",cpu_get_pc(),offset,addr);
        /*TODO*///	#endif
        	
        			return memory_region(K051316_memory_region[chip]).read(addr);
        		}
        		else
        		{
                            logerror("%04x: read 051316 ROM offset %04x but reg 0x0c bit 0 not clear\n",cpu_get_pc(),offset);
        			return 0;
        		}
        	}
        	
        	public static ReadHandlerPtr K051316_rom_0_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		return K051316_rom_r(0,offset);
        	} };
        	
        	public static ReadHandlerPtr K051316_rom_1_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		return K051316_rom_r(1,offset);
        	} };
        	
        /*TODO*///	public static ReadHandlerPtr K051316_rom_2_r  = new ReadHandlerPtr() { public int handler(int offset)
        /*TODO*///	{
        /*TODO*///		return K051316_rom_r(2,offset);
        /*TODO*///	} };
        	
        	
        	
        	static void K051316_ctrl_w(int chip,int offset,int data)
        	{
        		K051316_ctrlram[chip][offset] = data;
        	//if (offset >= 0x0c) logerror("%04x: write %02x to 051316 reg %x\n",cpu_get_pc(),data,offset);
        	}
        	
        	public static WriteHandlerPtr K051316_ctrl_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K051316_ctrl_w(0,offset,data);
        	} };
        	
        	public static WriteHandlerPtr K051316_ctrl_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        		K051316_ctrl_w(1,offset,data);
        	} };
        	
        /*TODO*///	public static WriteHandlerPtr K051316_ctrl_2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        /*TODO*///	{
        /*TODO*///		K051316_ctrl_w(2,offset,data);
        /*TODO*///	} };
        	
        	public static void K051316_wraparound_enable(int chip, int status)
        	{
        		K051316_wraparound[chip] = status;
        	}
        	
        	public static void K051316_set_offset(int chip, int xoffs, int yoffs)
        	{
        		K051316_offset[chip][0] = xoffs;
        		K051316_offset[chip][1] = yoffs;
        	}
        	
        	
        	public static void K051316_zoom_draw(int chip, mame_bitmap bitmap,int flags,int priority)
        	{
        		int startx,starty;
		int incxx,incxy,incyx,incyy;
	
		startx = 256 * ((256 * K051316_ctrlram[chip][0x00] + K051316_ctrlram[chip][0x01]));
		incxx  =        (256 * K051316_ctrlram[chip][0x02] + K051316_ctrlram[chip][0x03]);
		incyx  =        (256 * K051316_ctrlram[chip][0x04] + K051316_ctrlram[chip][0x05]);
		starty = 256 * ((256 * K051316_ctrlram[chip][0x06] + K051316_ctrlram[chip][0x07]));
		incxy  =        (256 * K051316_ctrlram[chip][0x08] + K051316_ctrlram[chip][0x09]);
		incyy  =        (256 * K051316_ctrlram[chip][0x0a] + K051316_ctrlram[chip][0x0b]);
	
		startx -= (16 + K051316_offset[chip][1]) * incyx;
		starty -= (16 + K051316_offset[chip][1]) * incyy;
	
		startx -= (89 + K051316_offset[chip][0]) * incxx;
		starty -= (89 + K051316_offset[chip][0]) * incxy;
	
		tilemap_draw_roz(bitmap,K051316_tilemap[chip],startx << 5,starty << 5,
				incxx << 5,incxy << 5,incyx << 5,incyy << 5,
				K051316_wraparound[chip],
				flags,priority);
                }
        	
        	public static void K051316_zoom_draw_0(mame_bitmap bitmap,int flags,int priority)
        	{
        		K051316_zoom_draw(0,bitmap,flags,priority);
        	}
        	
        	public static void K051316_zoom_draw_1(mame_bitmap bitmap,int flags,int priority)
        	{
        		K051316_zoom_draw(1,bitmap,flags,priority);
        	}
        	
        /*TODO*///	void K051316_zoom_draw_2(struct mame_bitmap *bitmap,int flags,UINT32 priority)
        /*TODO*///	{
        /*TODO*///		K051316_zoom_draw(2,bitmap,flags,priority);
        /*TODO*///	}
        
                
                
                
        	static /*unsigned*/ char[] K053251_ram = new char[16];
    static int[] K053251_palette_index = new int[5];

    public static WriteHandlerPtr K053251_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            data &= 0x3f;

            if (K053251_ram[offset] != data) {
                K053251_ram[offset] = (char) (data & 0xFF);
                if (offset == 9) {
                    /* palette base index */
                    K053251_palette_index[0] = 32 * ((data >> 0) & 0x03);
                    K053251_palette_index[1] = 32 * ((data >> 2) & 0x03);
                    K053251_palette_index[2] = 32 * ((data >> 4) & 0x03);
                    tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
                } else if (offset == 10) {
                    /* palette base index */
                    K053251_palette_index[3] = 16 * ((data >> 0) & 0x07);
                    K053251_palette_index[4] = 16 * ((data >> 3) & 0x07);
                    tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
                }
                /*#if 0
                 else
                 {
                 if (errorlog)
                 fprintf(errorlog,"%04x: write %02x to K053251 register %04x\n",cpu_get_pc(),data&0xff,offset);
                 usrintf_showmessage("pri = %02x%02x%02x%02x %02x%02x%02x%02x %02x%02x%02x%02x %02x%02x%02x%02x",
                 K053251_ram[0],K053251_ram[1],K053251_ram[2],K053251_ram[3],
                 K053251_ram[4],K053251_ram[5],K053251_ram[6],K053251_ram[7],
                 K053251_ram[8],K053251_ram[9],K053251_ram[10],K053251_ram[11],
                 K053251_ram[12],K053251_ram[13],K053251_ram[14],K053251_ram[15]
                 );
                 }
                 #endif*/
            }
        }
    };

    static int K053251_get_priority(int ci) {
        return K053251_ram[ci];
    }

    static int K053251_get_palette_index(int ci) {
        return K053251_palette_index[ci];
    }
        	
        	
        	
        	static char[] K054000_ram = new char[0x20];
        /*TODO*///	
        /*TODO*///	public static WriteHandlerPtr collision_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        /*TODO*///	{
        /*TODO*///	} };
        	
        	public static WriteHandlerPtr K054000_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        /*TODO*///	#if VERBOSE
        /*TODO*///	logerror("%04x: write %02x to 054000 address %02x\n",cpu_get_pc(),data,offset);
        /*TODO*///	#endif
        	
        		K054000_ram[offset] = (char) data;
        	} };
        	
        	public static ReadHandlerPtr K054000_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		int Acx,Acy,Aax,Aay;
        		int Bcx,Bcy,Bax,Bay;
        	
        	
        /*TODO*///	#if VERBOSE
        /*TODO*///	logerror("%04x: read 054000 address %02x\n",cpu_get_pc(),offset);
        /*TODO*///	#endif
        	
        		if (offset != 0x18) return 0;
        	
        	
        		Acx = (K054000_ram[0x01] << 16) | (K054000_ram[0x02] << 8) | K054000_ram[0x03];
        		Acy = (K054000_ram[0x09] << 16) | (K054000_ram[0x0a] << 8) | K054000_ram[0x0b];
        	/* TODO: this is a hack to make thndrx2 pass the startup check. It is certainly wrong. */
        	if (K054000_ram[0x04] == 0xff) Acx+=3;
        	if (K054000_ram[0x0c] == 0xff) Acy+=3;
        		Aax = K054000_ram[0x06] + 1;
        		Aay = K054000_ram[0x07] + 1;
        	
        		Bcx = (K054000_ram[0x15] << 16) | (K054000_ram[0x16] << 8) | K054000_ram[0x17];
        		Bcy = (K054000_ram[0x11] << 16) | (K054000_ram[0x12] << 8) | K054000_ram[0x13];
        		Bax = K054000_ram[0x0e] + 1;
        		Bay = K054000_ram[0x0f] + 1;
        	
        		if (Acx + Aax < Bcx - Bax)
        			return 1;
        	
        		if (Bcx + Bax < Acx - Aax)
        			return 1;
        	
        		if (Acy + Aay < Bcy - Bay)
        			return 1;
        	
        		if (Bcy + Bay < Acy - Aay)
        			return 1;
        	
        		return 0;
        	} };
        	
        /*TODO*///	READ16_HANDLER( K054000_lsb_r )
        /*TODO*///	{
        /*TODO*///		return K054000_r(offset);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K054000_lsb_w )
        /*TODO*///	{
        /*TODO*///		if (ACCESSING_LSB)
        /*TODO*///			K054000_w(offset, data & 0xff);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	
        /*TODO*///	
        /*TODO*///	
        	static int[] K051733_ram = new int[0x20];
        	
        	public static WriteHandlerPtr K051733_w = new WriteHandlerPtr() {public void handler(int offset, int data)
        	{
        /*TODO*///	#if VERBOSE
        /*TODO*///	logerror("%04x: write %02x to 051733 address %02x\n",cpu_get_pc(),data,offset);
        /*TODO*///	#endif
        
	        	K051733_ram[offset] = data;
	        } };
        
        
        	static int int_sqrt(int op)
        	{
        		int i,step;
        	
        		i = 0x8000;
        		step = 0x4000;
        		while (step!=0)
        		{
        			if (i*i == op) return i;
        			else if (i*i > op) i -= step;
        			else i += step;
        			step >>= 1;
        		}
        		return i;
        	}
        	
        	public static ReadHandlerPtr K051733_r  = new ReadHandlerPtr() { public int handler(int offset)
        	{
        		int op1 = (K051733_ram[0x00] << 8) | K051733_ram[0x01];
        		int op2 = (K051733_ram[0x02] << 8) | K051733_ram[0x03];
        		int op3 = (K051733_ram[0x04] << 8) | K051733_ram[0x05];
        	
        		int rad = (K051733_ram[0x06] << 8) | K051733_ram[0x07];
        		int yobj1c = (K051733_ram[0x08] << 8) | K051733_ram[0x09];
        		int xobj1c = (K051733_ram[0x0a] << 8) | K051733_ram[0x0b];
        		int yobj2c = (K051733_ram[0x0c] << 8) | K051733_ram[0x0d];
        		int xobj2c = (K051733_ram[0x0e] << 8) | K051733_ram[0x0f];
        	
        /*TODO*///	#if VERBOSE
        /*TODO*///	logerror("%04x: read 051733 address %02x\n",cpu_get_pc(),offset);
        /*TODO*///	#endif
        	
        		switch(offset){
        			case 0x00:
        				if (op2!=0) return	(op1 / op2) >> 8;
        				else return 0xff;
        			case 0x01:
        				if (op2!=0) return	(op1 / op2) & 0xff;
        				else return 0xff;
        	
        			/* this is completely unverified */
        			case 0x02:
        				if (op2!=0) return	(op1 % op2) >> 8;
        				else return 0xff;
        			case 0x03:
        				if (op2!=0) return	(op1 % op2) & 0xff;
        				else return 0xff;
        	
        			case 0x04:
        				return int_sqrt(op3<<16) >> 8;
        	
        			case 0x05:
        				return int_sqrt(op3<<16) & 0xff;
        	
        			case 0x07:{
        				if (xobj1c + rad < xobj2c - rad)
        					return 0x80;
        	
        				if (xobj2c + rad < xobj1c - rad)
        					return 0x80;
        	
        				if (yobj1c + rad < yobj2c - rad)
        					return 0x80;
        	
        				if (yobj2c + rad < yobj1c - rad)
        					return 0x80;
        	
        				return 0;
        			}
        			default:
        				return K051733_ram[offset];
        		}
        	} };
        	
        	
        	
        /*TODO*///	static struct tilemap *K054157_tilemap[4], *K054157_cur_tilemap;
        /*TODO*///	static struct tilemap *K054157_tilemapb[4], *K054157_tilemaps[4];
        /*TODO*///	
        /*TODO*///	static data16_t K054157_regs[0x20], K054157_regsb[4];
        /*TODO*///	static void (*K054157_linescroll_updater[4])(int layer);
        /*TODO*///	
        /*TODO*///	static int K054157_cur_rombank, K054157_romnbbanks;
        /*TODO*///	static int K054157_uses_tile_banks, K054157_cur_tile_bank;
        /*TODO*///	static int K054157_gfxnum, K054157_memory_region;
        /*TODO*///	static int K054157_cur_offset;
        /*TODO*///	static data16_t *K054157_rambase, *K054157_cur_spbase, *K054157_cur_rambase;
        /*TODO*///	static data8_t *K054157_rombase;
        /*TODO*///	static data16_t *K054157_rambasel[8];
        /*TODO*///	static int K054157_tilemapl[8], K054157_offsetl[8];
        /*TODO*///	
        /*TODO*///	static void (*K054157_callback)(int, int *, int *);
        /*TODO*///	
        /*TODO*///	INLINE void K054157_get_tile_info(int tile_index,int layer)
        /*TODO*///	{
        /*TODO*///		data16_t *addr;
        /*TODO*///		int attr, code;
        /*TODO*///		data16_t *lbase = K054157_rambase + 0x2000*layer;
        /*TODO*///		if(tile_index < 64*32)
        /*TODO*///			addr = lbase + (tile_index<<1);
        /*TODO*///		else
        /*TODO*///			addr = lbase + (tile_index<<1) + 0x1000 - 64*32*2;
        /*TODO*///	
        /*TODO*///		attr = addr[0];
        /*TODO*///		code = addr[1];
        /*TODO*///		tile_info.flags = 0;
        /*TODO*///	
        /*TODO*///		(*K054157_callback)(layer, &code, &attr);
        /*TODO*///		SET_TILE_INFO(K054157_gfxnum,
        /*TODO*///				code,
        /*TODO*///				attr,
        /*TODO*///				tile_info.flags)
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_get_tile_info0(int tile_index) { K054157_get_tile_info(tile_index,0); }
        /*TODO*///	static void K054157_get_tile_info1(int tile_index) { K054157_get_tile_info(tile_index,1); }
        /*TODO*///	static void K054157_get_tile_info2(int tile_index) { K054157_get_tile_info(tile_index,2); }
        /*TODO*///	static void K054157_get_tile_info3(int tile_index) { K054157_get_tile_info(tile_index,3); }
        /*TODO*///	
        /*TODO*///	
        /*TODO*///	public static VhStopPtr K054157_vh_stop = new VhStopPtr() { public void handler() 
        /*TODO*///	{
        /*TODO*///		if(K054157_rambase) {
        /*TODO*///			free(K054157_rambase);
        /*TODO*///			K054157_rambase = 0;
        /*TODO*///		}
        /*TODO*///	} };
        /*TODO*///	
        /*TODO*///	static void K054157_lsu_1_256(int layer)
        /*TODO*///	{
        /*TODO*///		int y;
        /*TODO*///		int basey = K054157_regs[0x10|layer];
        /*TODO*///	
        /*TODO*///		data16_t *baseram = K054157_cur_spbase + layer*0x400;
        /*TODO*///		for(y=0; y<256; y++) {
        /*TODO*///			int offset = (((basey + y) & 0x1ff) << 1) | 1;
        /*TODO*///			tilemap_set_scrollx(K054157_tilemap[layer], y, baseram[offset]);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_lsu_1_512(int layer)
        /*TODO*///	{
        /*TODO*///		int y;
        /*TODO*///		int basey = K054157_regs[0x10|layer];
        /*TODO*///	
        /*TODO*///		data16_t *baseram = K054157_cur_spbase + layer*0x400;
        /*TODO*///		for(y=0; y<512; y++) {
        /*TODO*///			int offset = (((basey + y) & 0x1ff) << 1) | 1;
        /*TODO*///			tilemap_set_scrollx(K054157_tilemap[layer], y, baseram[offset]);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_lsu_8_256(int layer)
        /*TODO*///	{
        /*TODO*///		int y;
        /*TODO*///		int basey = K054157_regs[0x10|layer];
        /*TODO*///	
        /*TODO*///		data16_t *baseram = K054157_cur_spbase + layer*0x400;
        /*TODO*///		for(y=0; y<256; y++) {
        /*TODO*///			int offset = (((basey + y) & 0x1f8) << 1) | 1;
        /*TODO*///			tilemap_set_scrollx(K054157_tilemap[layer], y, baseram[offset]);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_lsu_8_512(int layer)
        /*TODO*///	{
        /*TODO*///		int y;
        /*TODO*///		int basey = K054157_regs[0x10|layer];
        /*TODO*///	
        /*TODO*///		data16_t *baseram = K054157_cur_spbase + layer*0x400;
        /*TODO*///		for(y=0; y<512; y++) {
        /*TODO*///			int offset = (((basey + y) & 0x1f8) << 1) | 1;
        /*TODO*///			tilemap_set_scrollx(K054157_tilemap[layer], y, baseram[offset]);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_lsu_full(int layer)
        /*TODO*///	{
        /*TODO*///		tilemap_set_scrollx(K054157_tilemap[layer], 0, K054157_regs[0x14|layer]);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_reset_linescroll(void)
        /*TODO*///	{
        /*TODO*///		int layer;
        /*TODO*///		int mode = K054157_regs[5];
        /*TODO*///		for(layer=0; layer < 4; layer++) {
        /*TODO*///			int lmode = (mode >> (layer << 1)) & 3;
        /*TODO*///			switch(lmode) {
        /*TODO*///			case 0:
        /*TODO*///				if(K054157_tilemap[layer] == K054157_tilemaps[layer]) {
        /*TODO*///					K054157_linescroll_updater[layer] = K054157_lsu_1_256;
        /*TODO*///					tilemap_set_scroll_rows(K054157_tilemap[layer], 256);
        /*TODO*///				} else {
        /*TODO*///					K054157_linescroll_updater[layer] = K054157_lsu_1_512;
        /*TODO*///					tilemap_set_scroll_rows(K054157_tilemap[layer], 512);
        /*TODO*///				}
        /*TODO*///				break;
        /*TODO*///			case 1:
        /*TODO*///				logerror("LS mode layer %d unknown (%d)\n", layer, lmode);
        /*TODO*///				goto rhaaa_lovely;
        /*TODO*///			case 2:
        /*TODO*///				if(K054157_tilemap[layer] == K054157_tilemaps[layer]) {
        /*TODO*///					K054157_linescroll_updater[layer] = K054157_lsu_8_256;
        /*TODO*///					tilemap_set_scroll_rows(K054157_tilemap[layer], 256);
        /*TODO*///				} else {
        /*TODO*///					K054157_linescroll_updater[layer] = K054157_lsu_8_512;
        /*TODO*///					tilemap_set_scroll_rows(K054157_tilemap[layer], 512);
        /*TODO*///				}
        /*TODO*///				break;
        /*TODO*///			case 3:
        /*TODO*///			rhaaa_lovely:
        /*TODO*///				K054157_linescroll_updater[layer] = K054157_lsu_full;
        /*TODO*///				tilemap_set_scroll_rows(K054157_tilemap[layer], 1);
        /*TODO*///				break;
        /*TODO*///			}
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_change_tilemap(int layer)
        /*TODO*///	{
        /*TODO*///		int flip = 0;
        /*TODO*///		if(K054157_regs[8|layer] & 1) {
        /*TODO*///			tilemap_set_enable(K054157_tilemapb[layer], 1);
        /*TODO*///			tilemap_set_enable(K054157_tilemaps[layer], 0);
        /*TODO*///			K054157_tilemap[layer] = K054157_tilemapb[layer];
        /*TODO*///		} else {
        /*TODO*///			tilemap_set_enable(K054157_tilemapb[layer], 0);
        /*TODO*///			tilemap_set_enable(K054157_tilemaps[layer], 1);
        /*TODO*///			K054157_tilemap[layer] = K054157_tilemaps[layer];
        /*TODO*///		}
        /*TODO*///		tilemap_mark_all_tiles_dirty(K054157_tilemap[layer]);
        /*TODO*///	
        /*TODO*///		if(K054157_regs[0] & 0x20)
        /*TODO*///			flip |= TILEMAP_FLIPY;
        /*TODO*///		if(K054157_regs[0] & 0x10)
        /*TODO*///			flip |= TILEMAP_FLIPX;
        /*TODO*///	
        /*TODO*///		tilemap_set_flip(K054157_tilemap[layer], flip);
        /*TODO*///	
        /*TODO*///		K054157_reset_linescroll();
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_reset_tilemaps(void)
        /*TODO*///	{
        /*TODO*///		int i;
        /*TODO*///		// To avoid crashes in K054157_reset_linescroll()
        /*TODO*///		for(i=0; i<4; i++)
        /*TODO*///			K054157_tilemap[i] = K054157_tilemaps[i];
        /*TODO*///		for(i=0; i<4; i++)
        /*TODO*///			K054157_change_tilemap(i);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_change_flip(void)
        /*TODO*///	{
        /*TODO*///		int flip = 0;
        /*TODO*///	
        /*TODO*///		if(K054157_regs[0] & 0x20)
        /*TODO*///			flip |= TILEMAP_FLIPY;
        /*TODO*///		if(K054157_regs[0] & 0x10)
        /*TODO*///			flip |= TILEMAP_FLIPX;
        /*TODO*///		tilemap_set_flip(K054157_tilemap[0], flip);
        /*TODO*///		tilemap_set_flip(K054157_tilemap[1], flip);
        /*TODO*///		tilemap_set_flip(K054157_tilemap[2], flip);
        /*TODO*///		tilemap_set_flip(K054157_tilemap[3], flip);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_change_rambank(void)
        /*TODO*///	{
        /*TODO*///		int bank = ((K054157_regs[0x19]>>2) & 6) | (K054157_regs[0x19] & 1);
        /*TODO*///	
        /*TODO*///		K054157_cur_rambase = K054157_rambasel[bank];
        /*TODO*///		K054157_cur_tilemap = K054157_tilemap[K054157_tilemapl[bank]];
        /*TODO*///		K054157_cur_offset  = K054157_offsetl[bank];
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_change_splayer(void)
        /*TODO*///	{
        /*TODO*///		int bank = ((K054157_regs[0x18]>>2) & 6) | (K054157_regs[0x18] & 1);
        /*TODO*///		K054157_cur_spbase = K054157_rambasel[bank];
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	static void K054157_change_rombank(void)
        /*TODO*///	{
        /*TODO*///		int bank;
        /*TODO*///	
        /*TODO*///		if (K054157_uses_tile_banks)	/* asterix */
        /*TODO*///			bank = (K054157_regs[0x1a] >> 8) | (K054157_regs[0x1b] << 4) | (K054157_cur_tile_bank << 6);
        /*TODO*///		else	/* everything else */
        /*TODO*///			bank = K054157_regs[0x1a] | (K054157_regs[0x1b] << 16);
        /*TODO*///	
        /*TODO*///		K054157_cur_rombank = bank % K054157_romnbbanks;
        /*TODO*///	//usrintf_showmessage("%04x: %04x %04x %04x",cpu_get_pc(),K054157_regs[0x1a],K054157_regs[0x1b],K054157_cur_rombank);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	int K054157_vh_start(int gfx_memory_region, int big, int (*scrolld)[4][2], int plane0,int plane1,int plane2,int plane3, void (*callback)(int, int *, int *))
        /*TODO*///	{
        /*TODO*///		int gfx_index;
        /*TODO*///		int i;
        /*TODO*///		static GfxLayout charlayout = new GfxLayout
        /*TODO*///		(
        /*TODO*///			8, 8,
        /*TODO*///			0,				/* filled in later */
        /*TODO*///			4,
        /*TODO*///			new int[] { 0, 0, 0, 0 },	/* filled in later */
        /*TODO*///			new int[] { 2*4, 3*4, 0*4, 1*4, 6*4, 7*4, 4*4, 5*4 },
        /*TODO*///			new int[] { 0*8*4, 1*8*4, 2*8*4, 3*8*4, 4*8*4, 5*8*4, 6*8*4, 7*8*4 },
        /*TODO*///			8*8*4
        /*TODO*///		);
        /*TODO*///	
        /*TODO*///		/* find first empty slot to decode gfx */
        /*TODO*///		for (gfx_index = 0; gfx_index < MAX_GFX_ELEMENTS; gfx_index++)
        /*TODO*///			if (Machine.gfx[gfx_index] == 0)
        /*TODO*///				break;
        /*TODO*///		if (gfx_index == MAX_GFX_ELEMENTS)
        /*TODO*///			return 1;
        /*TODO*///	
        /*TODO*///		/* tweak the structure for the number of tiles we have */
        /*TODO*///		charlayout.total = memory_region_length(gfx_memory_region) / (8*4);
        /*TODO*///		charlayout.planeoffset[0] = plane0;
        /*TODO*///		charlayout.planeoffset[1] = plane1;
        /*TODO*///		charlayout.planeoffset[2] = plane2;
        /*TODO*///		charlayout.planeoffset[3] = plane3;
        /*TODO*///	
        /*TODO*///		/* decode the graphics */
        /*TODO*///		Machine.gfx[gfx_index] = decodegfx(memory_region(gfx_memory_region), &charlayout);
        /*TODO*///		if (!Machine.gfx[gfx_index])
        /*TODO*///			return 1;
        /*TODO*///	
        /*TODO*///		/* set the color information */
        /*TODO*///		if (Machine.drv.color_table_len)
        /*TODO*///		{
        /*TODO*///			Machine.gfx[gfx_index].colortable = Machine.remapped_colortable;
        /*TODO*///			Machine.gfx[gfx_index].total_colors = Machine.drv.color_table_len / 16;
        /*TODO*///		}
        /*TODO*///		else
        /*TODO*///		{
        /*TODO*///			Machine.gfx[gfx_index].colortable = Machine.pens;
        /*TODO*///			Machine.gfx[gfx_index].total_colors = Machine.drv.total_colors / 16;
        /*TODO*///		}
        /*TODO*///	
        /*TODO*///		K054157_memory_region = gfx_memory_region;
        /*TODO*///		K054157_gfxnum = gfx_index;
        /*TODO*///		K054157_callback = callback;
        /*TODO*///	
        /*TODO*///		K054157_rombase = memory_region(gfx_memory_region);
        /*TODO*///		K054157_romnbbanks = memory_region_length(gfx_memory_region)/0x2000;
        /*TODO*///		K054157_cur_rombank = 0;
        /*TODO*///		K054157_uses_tile_banks = 0;
        /*TODO*///	
        /*TODO*///		K054157_tilemapb[0] = tilemap_create(K054157_get_tile_info0, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 64);
        /*TODO*///		K054157_tilemapb[1] = tilemap_create(K054157_get_tile_info1, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 64);
        /*TODO*///		K054157_tilemapb[2] = tilemap_create(K054157_get_tile_info2, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 64);
        /*TODO*///		K054157_tilemapb[3] = tilemap_create(K054157_get_tile_info3, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 64);
        /*TODO*///		K054157_tilemaps[0] = tilemap_create(K054157_get_tile_info0, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 32);
        /*TODO*///		K054157_tilemaps[1] = tilemap_create(K054157_get_tile_info1, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 32);
        /*TODO*///		K054157_tilemaps[2] = tilemap_create(K054157_get_tile_info2, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 32);
        /*TODO*///		K054157_tilemaps[3] = tilemap_create(K054157_get_tile_info3, tilemap_scan_rows,
        /*TODO*///											 TILEMAP_TRANSPARENT, 8, 8, 64, 32);
        /*TODO*///	
        /*TODO*///		K054157_rambase = malloc(0x14000);
        /*TODO*///	
        /*TODO*///		if(!K054157_rambase
        /*TODO*///		   || !K054157_tilemapb[0] || !K054157_tilemapb[1] || !K054157_tilemapb[2] || !K054157_tilemapb[3]
        /*TODO*///		   || !K054157_tilemaps[0] || !K054157_tilemaps[1] || !K054157_tilemaps[2] || !K054157_tilemaps[3]) {
        /*TODO*///			K054157_vh_stop();
        /*TODO*///			return 1;
        /*TODO*///		}
        /*TODO*///	
        /*TODO*///		if(big) {
        /*TODO*///			K054157_rambasel[0] = K054157_rambase + 0x2000;
        /*TODO*///			K054157_rambasel[1] = K054157_rambase + 0x6000;
        /*TODO*///			K054157_rambasel[2] = K054157_rambase + 0x3000;
        /*TODO*///			K054157_rambasel[3] = K054157_rambase + 0x7000;
        /*TODO*///			K054157_rambasel[4] = K054157_rambase + 0x4000;
        /*TODO*///			K054157_rambasel[5] = K054157_rambase + 0x0000;
        /*TODO*///			K054157_rambasel[6] = K054157_rambase + 0x5000;
        /*TODO*///			K054157_rambasel[7] = K054157_rambase + 0x1000;
        /*TODO*///		} else {
        /*TODO*///			K054157_rambasel[0] = K054157_rambase + 0x0000;
        /*TODO*///			K054157_rambasel[1] = K054157_rambase + 0x2000;
        /*TODO*///			K054157_rambasel[2] = K054157_rambase + 0x4000;
        /*TODO*///			K054157_rambasel[3] = K054157_rambase + 0x6000;
        /*TODO*///			K054157_rambasel[4] = 0;
        /*TODO*///			K054157_rambasel[5] = 0;
        /*TODO*///			K054157_rambasel[6] = 0;
        /*TODO*///			K054157_rambasel[7] = 0;
        /*TODO*///		}
        /*TODO*///	
        /*TODO*///		for(i=0; i<8; i++) {
        /*TODO*///			if(K054157_rambasel[i]) {
        /*TODO*///				int delta = K054157_rambasel[i] - K054157_rambase;
        /*TODO*///				K054157_tilemapl[i] = delta >> 13;
        /*TODO*///				K054157_offsetl [i] = (delta & 0x1000) ? 64*32 : 0;
        /*TODO*///			} else {
        /*TODO*///				K054157_tilemapl[i] = 0;
        /*TODO*///				K054157_offsetl [i] = 0;
        /*TODO*///			}
        /*TODO*///		}
        /*TODO*///	
        /*TODO*///		memset(K054157_rambase, 0, 0x10000);
        /*TODO*///		memset(K054157_regs, 0, 0x40);
        /*TODO*///		memset(K054157_regsb, 0, 8);
        /*TODO*///	
        /*TODO*///		for(i=0; i<4; i++) {
        /*TODO*///			tilemap_set_transparent_pen(K054157_tilemapb[i],0);
        /*TODO*///			tilemap_set_scrolldx(K054157_tilemapb[i], -scrolld[0][i][0], -scrolld[1][i][0]);
        /*TODO*///			tilemap_set_scrolldy(K054157_tilemapb[i], -scrolld[0][i][1], -scrolld[1][i][1]);
        /*TODO*///	
        /*TODO*///			tilemap_set_transparent_pen(K054157_tilemaps[i],0);
        /*TODO*///			tilemap_set_scrolldx(K054157_tilemaps[i], -scrolld[0][i][0], -scrolld[1][i][0]);
        /*TODO*///			tilemap_set_scrolldy(K054157_tilemaps[i], -scrolld[0][i][1], -scrolld[1][i][1]);
        /*TODO*///		}
        /*TODO*///	
        /*TODO*///		K054157_reset_tilemaps();
        /*TODO*///		K054157_change_rambank();
        /*TODO*///		K054157_change_rombank();
        /*TODO*///		K054157_change_splayer();
        /*TODO*///	
        /*TODO*///		state_save_register_UINT16("K054157", 0, "memory",      K054157_rambase, 0x8000);
        /*TODO*///		state_save_register_UINT16("K054157", 0, "registers",   K054157_regs,    0x20);
        /*TODO*///		state_save_register_UINT16("K054157", 0, "registers b", K054157_regsb,   0x4);
        /*TODO*///	
        /*TODO*///		state_save_register_func_postload(K054157_reset_tilemaps);
        /*TODO*///		state_save_register_func_postload(K054157_change_rambank);
        /*TODO*///		state_save_register_func_postload(K054157_change_rombank);
        /*TODO*///	
        /*TODO*///		return 0;
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	
        /*TODO*///	READ16_HANDLER( K054157_ram_word_r )
        /*TODO*///	{
        /*TODO*///		return K054157_cur_rambase[offset];
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	READ16_HANDLER( K054157_ram_half_word_r )
        /*TODO*///	{
        /*TODO*///		return K054157_cur_rambase[((offset << 1) & 0xffe) | ((offset >> 11) ^ 1)];
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	READ16_HANDLER( K054157_rom_word_r )
        /*TODO*///	{
        /*TODO*///		int addr = 0x2000*K054157_cur_rombank + 2*offset;
        /*TODO*///	
        /*TODO*///	#if 0
        /*TODO*///		usrintf_showmessage("%04x: addr %06x",cpu_get_pc(),addr);
        /*TODO*///	#endif
        /*TODO*///	
        /*TODO*///		return K054157_rombase[addr+1] | (K054157_rombase[addr] << 8);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K054157_ram_word_w )
        /*TODO*///	{
        /*TODO*///		data16_t *adr = K054157_cur_rambase + offset;
        /*TODO*///		data16_t old = *adr;
        /*TODO*///	
        /*TODO*///		COMBINE_DATA(adr);
        /*TODO*///		if(*adr != old && K054157_cur_tilemap)
        /*TODO*///			tilemap_mark_tile_dirty(K054157_cur_tilemap, offset/2 + K054157_cur_offset);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K054157_ram_half_word_w )
        /*TODO*///	{
        /*TODO*///		data16_t *adr = K054157_cur_rambase + (((offset << 1) & 0xffe) | 1);
        /*TODO*///		data16_t old = *adr;
        /*TODO*///	
        /*TODO*///		COMBINE_DATA(adr);
        /*TODO*///		if(*adr != old)
        /*TODO*///			tilemap_mark_tile_dirty(K054157_cur_tilemap, (offset & 0x7ff) + K054157_cur_offset);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K054157_word_w )
        /*TODO*///	{
        /*TODO*///		UINT16 old = K054157_regs[offset];
        /*TODO*///		COMBINE_DATA (K054157_regs + offset);
        /*TODO*///	
        /*TODO*///		if(K054157_regs[offset] != old)
        /*TODO*///		{
        /*TODO*///			switch(offset) {
        /*TODO*///			case 0x00:
        /*TODO*///				if((K054157_regs[0] & 0x30) != (old & 0x30))
        /*TODO*///					K054157_change_flip();
        /*TODO*///				break;
        /*TODO*///			case 0x05:
        /*TODO*///				K054157_reset_linescroll();
        /*TODO*///				break;
        /*TODO*///			case 0x08:
        /*TODO*///			case 0x09:
        /*TODO*///			case 0x0a:
        /*TODO*///			case 0x0b:
        /*TODO*///				if((K054157_regs[offset] & 1) ^ (K054157_tilemap[offset & 3] == K054157_tilemapb[offset & 3]))
        /*TODO*///					K054157_change_tilemap(offset & 3);
        /*TODO*///				break;
        /*TODO*///			case 0x18:
        /*TODO*///				K054157_change_splayer();
        /*TODO*///				break;
        /*TODO*///			case 0x19:
        /*TODO*///				K054157_change_rambank();
        /*TODO*///				break;
        /*TODO*///			case 0x1a:
        /*TODO*///			case 0x1b:
        /*TODO*///				K054157_change_rombank();
        /*TODO*///				break;
        /*TODO*///			case 0x1c:
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemapb[0]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemapb[1]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemapb[2]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemapb[3]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemaps[0]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemaps[1]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemaps[2]);
        /*TODO*///				tilemap_mark_all_tiles_dirty(K054157_tilemaps[3]);
        /*TODO*///				break;
        /*TODO*///			}
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	WRITE16_HANDLER( K054157_b_word_w )
        /*TODO*///	{
        /*TODO*///		COMBINE_DATA (K054157_regsb + offset);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	void K054157_tilemap_update(void)
        /*TODO*///	{
        /*TODO*///		int layer;
        /*TODO*///	
        /*TODO*///		for(layer=0; layer<4; layer++)
        /*TODO*///		{
        /*TODO*///			K054157_linescroll_updater[layer](layer);
        /*TODO*///			tilemap_set_scrolly(K054157_tilemap[layer], 0, K054157_regs[0x10|layer]);
        /*TODO*///		}
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	void K054157_tilemap_draw(struct mame_bitmap *bitmap, int num, int flags, UINT32 priority)
        /*TODO*///	{
        /*TODO*///		tilemap_draw(bitmap, K054157_tilemap[num], flags, priority);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	void K054157_mark_plane_dirty(int num)
        /*TODO*///	{
        /*TODO*///		tilemap_mark_all_tiles_dirty(K054157_tilemap[num]);
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	int K054157_is_IRQ_enabled(void)
        /*TODO*///	{
        /*TODO*///		return K054157_regs[3] & 1;
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	int K054157_get_lookup(int bits)
        /*TODO*///	{
        /*TODO*///		int res;
        /*TODO*///	
        /*TODO*///		res = (K054157_regs[0x1c] >> (bits << 2)) & 0x0f;
        /*TODO*///	
        /*TODO*///		if (K054157_uses_tile_banks)	/* Asterix */
        /*TODO*///			res |= K054157_cur_tile_bank << 4;
        /*TODO*///	
        /*TODO*///		return res;
        /*TODO*///	}
        /*TODO*///	
        /*TODO*///	void K054157_set_tile_bank(int bank)
        /*TODO*///	{
        /*TODO*///		K054157_uses_tile_banks = 1;
        /*TODO*///	
        /*TODO*///		if (K054157_cur_tile_bank != bank)
        /*TODO*///		{
        /*TODO*///			K054157_cur_tile_bank = bank;
        /*TODO*///	
        /*TODO*///			K054157_mark_plane_dirty(0);
        /*TODO*///			K054157_mark_plane_dirty(1);
        /*TODO*///			K054157_mark_plane_dirty(2);
        /*TODO*///			K054157_mark_plane_dirty(3);
        /*TODO*///		}
        /*TODO*///	
        /*TODO*///		K054157_change_rombank();
        /*TODO*///	}
}

