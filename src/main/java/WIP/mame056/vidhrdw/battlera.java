/*******************************************************************************

	Battle Rangers - Bryan McPhail, mish@tendril.co.uk

	This file only implements necessary features - not all PC-Engine video
	features are used in this game (no DMA for one).

*******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static mame056.cpu.h6280.h6280H.H6280_INT_IRQ1;
import static mame056.tilemapC.*;
import static mame056.tilemapH.*;

public class battlera
{
	
	static int[] HuC6270_registers = new int[20];
	static int VDC_register,vram_ptr;
	static UBytePtr HuC6270_vram=new UBytePtr(), tile_dirty=new UBytePtr(), sprite_dirty=new UBytePtr(), vram_dirty=new UBytePtr();
	static mame_bitmap tile_bitmap, front_bitmap;
	
	static int current_scanline,next_update_first_line,inc_value;
	static int irq_enable,rcr_enable,sb_enable,bb_enable,bldwolf_vblank;
	
	
	
	/******************************************************************************/
	
	public static VhStopPtr battlera_vh_stop = new VhStopPtr() { public void handler() 
	{
		tile_dirty = null;
		HuC6270_vram = null;
		sprite_dirty = null;
		vram_dirty = null;
	
		bitmap_free (tile_bitmap);
		bitmap_free (front_bitmap);
	} };
	
	public static VhStartPtr battlera_vh_start = new VhStartPtr() { public int handler() 
	{
		HuC6270_vram=new UBytePtr(0x20000);
		tile_dirty=new UBytePtr(0x1000);
		sprite_dirty=new UBytePtr(0x400);
		vram_dirty=new UBytePtr(0x1000);
	
		memset(HuC6270_vram,0,0x20000);
		memset(tile_dirty,1,0x1000);
		memset(sprite_dirty,1,0x400);
		memset(vram_dirty,1,0x1000);
	
		tile_bitmap=bitmap_alloc(512,512);
		front_bitmap=bitmap_alloc(512,512);
	
		if (tile_bitmap==null || front_bitmap==null || tile_dirty==null || HuC6270_vram==null || sprite_dirty==null || vram_dirty==null)
			return 1;
	
		vram_ptr=0;
		inc_value=1;
		current_scanline=0;
		irq_enable=rcr_enable=sb_enable=bb_enable=0;
	
		return 0;
	} };
	
	/******************************************************************************/
	
	public static WriteHandlerPtr battlera_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r,g,b,pal_word;
	
		paletteram.write(offset,data);
		if ((offset%2)!=0) offset-=1;
	
		pal_word=paletteram.read(offset)| (paletteram.read(offset+1)<<8);
	
		r = ((pal_word >> 3) & 7) << 5;
		g = ((pal_word >> 6) & 7) << 5;
		b = ((pal_word >> 0) & 7) << 5;
		palette_set_color(offset/2, r, g, b);
	} };
	
	/******************************************************************************/
	
	public static ReadHandlerPtr HuC6270_debug_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return HuC6270_vram.read(offset);
	} };
	
	public static WriteHandlerPtr HuC6270_debug_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		HuC6270_vram.write(offset, data);
	} };
	public static ReadHandlerPtr HuC6270_register_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int rr;
	
		if ((current_scanline+56)==HuC6270_registers[6]) rr=1; else rr=0;
	
		return 0		/* CR flag */
			| (0 << 1)	/* OR flag */
			| (rr << 2)	/* RR flag */
			| (0 << 3)	/* DS flag */
			| (0 << 4)	/* DV flag */
			| (bldwolf_vblank << 5)	/* VD flag (1 when vblank else 0) */
			| (0 << 6)	/* BSY flag (1 when dma active, else 0) */
			| (0 << 7);	/* Always zero */
	} };
	
	public static WriteHandlerPtr HuC6270_register_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset) {
		case 0: /* Select data region */
			VDC_register=data;
			break;
		case 1: /* Unused */
			break;
		}
	} };
	
	/******************************************************************************/
	
	public static ReadHandlerPtr HuC6270_data_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result;
	
		switch (offset) {
			case 0: /* LSB */
				return HuC6270_vram.read((HuC6270_registers[1]<<1)|1);
	
			case 1:/* MSB */
				result=HuC6270_vram.read((HuC6270_registers[1]<<1)|0);
				HuC6270_registers[1]=(HuC6270_registers[1]+inc_value)&0xffff;
				return result;
		}
	
		return 0;
	} };
	
	public static WriteHandlerPtr HuC6270_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset) {
			case 0: /* LSB */
				switch (VDC_register) {
	
				case 0: /* MAWR */
					HuC6270_registers[0]=(HuC6270_registers[0]&0xff00) | (data);
					return;
	
				case 1: /* MARR */
					HuC6270_registers[0]=(HuC6270_registers[1]&0xff00) | (data);
					return;
	
				case 2: /* VRAM */
					if (HuC6270_vram.read((HuC6270_registers[0]<<1)|1)!=data) {
						HuC6270_vram.write((HuC6270_registers[0]<<1)|1, data);
						tile_dirty.write(HuC6270_registers[0]>>4, 1);
						sprite_dirty.write(HuC6270_registers[0]>>6, 1);
					}
					if (HuC6270_registers[0]<0x1000) vram_dirty.write(HuC6270_registers[0], 1);
					return;
	
				case 3: break; /* Unused */
				case 4: break; /* Unused */
	
				case 5: /* CR - Control register */
					/* Bits 0,1 unknown */
					rcr_enable=data&0x4; /* Raster interrupt enable */
					irq_enable=data&0x8; /* VBL interrupt enable */
					/* Bits 4,5 unknown (EX) */
					sb_enable=data&0x40; /* Sprites enable */
					bb_enable=data&0x80; /* Background enable */
					return;
	
				case 6: /* RCR - Raster counter register */
					HuC6270_registers[6]=(HuC6270_registers[6]&0xff00) | (data);
					return;
	
				case 7: /* BXR - X scroll */
					HuC6270_registers[7]=(HuC6270_registers[7]&0xff00) | (data);
					return;
	
				case 8: /* BYR - Y scroll */
					HuC6270_registers[8]=(HuC6270_registers[8]&0xff00) | (data);
					return;
	
				case 15: /* DMA */
				case 16:
				case 17:
				case 18:
/*TODO*///					logerror("%04x: dma 2 %02x\n",cpu_get_pc(),data);
					break;
	
				case 19: /* SATB */
					HuC6270_registers[19]=(HuC6270_registers[19]&0xff00) | (data);
					return;
	
				}
				break;
	
			/*********************************************/
	
			case 1: /* MSB (Autoincrement on this write) */
				switch (VDC_register) {
	
				case 0: /* MAWR - Memory Address Write Register */
					HuC6270_registers[0]=(HuC6270_registers[0]&0xff) | (data<<8);
					return;
	
				case 1: /* MARR */
					HuC6270_registers[1]=(HuC6270_registers[1]&0xff) | (data<<8);
					return;
	
				case 2: /* VWR - VRAM */
					if (HuC6270_vram.read((HuC6270_registers[0]<<1)|0)!=data) {
						HuC6270_vram.write((HuC6270_registers[0]<<1)|0, data);
						tile_dirty.write(HuC6270_registers[0]>>4, 1);
						sprite_dirty.write(HuC6270_registers[0]>>6, 1);
						if (HuC6270_registers[0]<0x1000) vram_dirty.write(HuC6270_registers[0], 1);
					}
					HuC6270_registers[0]+=inc_value;
					HuC6270_registers[0]=HuC6270_registers[0]&0xffff;
					return;
	
				case 5: /* CR */
					/* IW - Auto-increment values */
					switch ((data>>3)&3) {
						case 0: inc_value=1; break;
						case 1: inc_value=32;break;
						case 2: inc_value=64; break;
						case 3: inc_value=128; break;
					}
	
					/* DR, TE unknown */
					return;
	
				case 6: /* RCR - Raster counter register */
					HuC6270_registers[6]=(HuC6270_registers[6]&0xff) | (data<<8);
					return;
	
				case 7: /* BXR - X scroll */
					HuC6270_registers[7]=(HuC6270_registers[7]&0xff) | (data<<8);
							return;
	
				case 8: /* BYR - Y scroll */
					HuC6270_registers[8]=(HuC6270_registers[8]&0xff) | (data<<8);
					return;
	
				case 15: /* DMA */
				case 16:
				case 17:
				case 18:
/*TODO*///					logerror("%04x: dma 2 %02x\n",cpu_get_pc(),data);
					break;
	
				case 19: /* SATB - Sprites */
					HuC6270_registers[19]=(HuC6270_registers[19]&0xff) | (data<<8);
					return;
				}
				break;
		}
/*TODO*///		logerror("%04x: unknown write to  VDC_register %02x (%02x) at %02x\n",cpu_get_pc(),VDC_register,data,offset);
	} };
	
	/******************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap, rectangle clip, int pri)
	{
		int offs,my,mx,code,code2,fx,fy,cgy=0,cgx,colour,i;
	
		/* Draw sprites, starting at SATB, draw in _reverse_ order */
		for (offs=(HuC6270_registers[19]<<1)+0x200-8; offs>=(HuC6270_registers[19]<<1); offs-=8)
		{
			if ((HuC6270_vram.read(offs+7)&0x80)!=0 && pri==0) continue;
			if ((HuC6270_vram.read(offs+7)&0x80)==0 && pri!=0) continue;
	
			code=HuC6270_vram.read(offs+5) + (HuC6270_vram.read(offs+4)<<8);
			code=code>>1;
	
			my=HuC6270_vram.read(offs+1) + (HuC6270_vram.read(offs+0)<<8);
			mx=HuC6270_vram.read(offs+3) + (HuC6270_vram.read(offs+2)<<8);
	
			mx-=32;
			my-=57;
	
			fx=HuC6270_vram.read(offs+6)&0x8;
			fy=HuC6270_vram.read(offs+6)&0x80;
			cgx=HuC6270_vram.read(offs+6)&1;
			colour=HuC6270_vram.read(offs+7)&0xf;
	
			switch ((HuC6270_vram.read(offs+6)>>4)&3) {
			case 0: cgy=1; break;
			case 1: cgy=2; break;
			case 2: cgy=0; break; /* Illegal */
			case 3: cgy=4; break;
			}
	
			if (cgx!=0 && cgy==2) code=code&0x3fc; /* Title screen */
	
			if (fx!=0 && cgx!=0) {code2=code; code++;} /* Swap tile order on X flips */
			else code2=code+1;
	
			for (i=0; i<cgy; i++) {
				drawgfx(bitmap,Machine.gfx[1],
					code,
					colour,
					fx,fy,
					mx,my,
					clip,TRANSPARENCY_PEN,0);
	
				if (cgx != 0)
					drawgfx(bitmap,Machine.gfx[1],
							code2,
							colour,
							fx,fy,
							mx+16,my,
							clip,TRANSPARENCY_PEN,0);
				my+=16;
				/* if (cgx) */ /* Different from console? */
				code+=2;
				code2+=2;
				/*else code+=1; */ /* Different from console? */
			}
		}
	
	}
	
	static void screenrefresh(mame_bitmap bitmap, rectangle clip)
	{
		int offs,code,scrollx,scrolly,mx,my;
	
		/* Dynamically decode chars if dirty */
		for (code = 0x0000;code < 0x1000;code++)
			if (tile_dirty.read(code) != 0)
				decodechar(Machine.gfx[0],code,HuC6270_vram,Machine.drv.gfxdecodeinfo[0].gfxlayout);
	
		/* Dynamically decode sprites if dirty */
		for (code = 0x0000;code < 0x400;code++)
			if (sprite_dirty.read(code) != 0)
				decodechar(Machine.gfx[1],code,HuC6270_vram,Machine.drv.gfxdecodeinfo[1].gfxlayout);
	
		/* NB: If first 0x1000 byte is always tilemap, no need to decode the first batch of tiles/sprites */
	
		mx=-1;
		my=0;
		for (offs = 0x0000;offs < 0x2000;offs += 2)
		{
			mx++;
			if (mx==64) {mx=0; my++;}
			code=HuC6270_vram.read(offs+1) + ((HuC6270_vram.read(offs) & 0x0f) << 8);
	
			/* If this tile was changed OR tilemap was changed, redraw */
			if (tile_dirty.read(code)!=0 || vram_dirty.read(offs/2)!=0) {
				vram_dirty.write(offs/2, 0);
		        drawgfx(tile_bitmap,Machine.gfx[0],
						code,
						HuC6270_vram.read(offs) >> 4,
						0,0,
						8*mx,8*my,
						null,TRANSPARENCY_NONE,0);
			drawgfx(front_bitmap,Machine.gfx[2],
						0,
						0,	/* fill the spot with pen 256 */
						0,0,
						8*mx,8*my,
						null,TRANSPARENCY_NONE,0);
		        drawgfx(front_bitmap,Machine.gfx[0],
						code,
						HuC6270_vram.read(offs) >> 4,
						0,0,
						8*mx,8*my,
						null,TRANSPARENCY_PENS,0x1);
				}
		}
	
		/* Nothing dirty after a screen refresh */
		for (code = 0x0000;code < 0x1000;code++)
			tile_dirty.write(code, 0);
		for (code = 0x0000;code < 0x400;code++)
			sprite_dirty.write(code, 0);
	
		/* Render bitmap */
		scrollx=-HuC6270_registers[7];
		scrolly=-HuC6270_registers[8]+clip.min_y-1;
	
		copyscrollbitmap(bitmap,tile_bitmap,1,new int[]{scrollx},1,new int[]{scrolly},clip,TRANSPARENCY_NONE,0);
	
		/* Todo:  Background enable (not used anyway) */
	
		/* Render low priority sprites, if enabled */
		if (sb_enable != 0) draw_sprites(bitmap,clip,0);
	
		/* Render background over sprites */
		copyscrollbitmap(bitmap,front_bitmap,1,new int[]{scrollx},1,new int[]{scrolly},clip,TRANSPARENCY_COLOR,256);
	
		/* Render high priority sprites, if enabled */
		if (sb_enable != 0) draw_sprites(bitmap,clip,1);
	}
	
	/******************************************************************************/
	
	public static VhUpdatePtr battlera_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* Nothing */
	} };
	
	static void partial_refresh(mame_bitmap bitmap,int current_line)
	{
		rectangle clip = new rectangle();
	
		clip.min_x = Machine.visible_area.min_x;
		clip.max_x = Machine.visible_area.max_x;
		clip.min_y = next_update_first_line;
		clip.max_y = current_line;
		if (clip.min_y < Machine.visible_area.min_y)
			clip.min_y = Machine.visible_area.min_y;
		if (clip.max_y > Machine.visible_area.max_y)
			clip.max_y = Machine.visible_area.max_y;
	
		if (clip.max_y >= clip.min_y)
		{
			screenrefresh(bitmap,clip);
		}
	
		next_update_first_line = current_line + 1;
	}
	
	static void battlera_vh_raster_partial_refresh(mame_bitmap bitmap,int start_line,int end_line)
	{
		rectangle clip = new rectangle();
	
		clip.min_x = Machine.visible_area.min_x;
		clip.max_x = Machine.visible_area.max_x;
		clip.min_y = start_line;
		clip.max_y = end_line;
		if (clip.min_y < Machine.visible_area.min_y)
			clip.min_y = Machine.visible_area.min_y;
		if (clip.max_y > Machine.visible_area.max_y)
			clip.max_y = Machine.visible_area.max_y;
	
		if (clip.max_y > clip.min_y)
		{
			screenrefresh(bitmap, clip);
		}
	}
	
	/******************************************************************************/
        
        static int last_line=0;
	
	public static InterruptPtr battlera_interrupt = new InterruptPtr() { public int handler() 
	{
		
	
		current_scanline=255-cpu_getiloops(); /* 8 lines clipped at top */
	
		/* If raster interrupt occurs, refresh screen _up_ to this point */
		if (rcr_enable!=0 && (current_scanline+56)==HuC6270_registers[6]) {
			battlera_vh_raster_partial_refresh(Machine.scrbitmap,last_line,current_scanline);
			last_line=current_scanline;
			return H6280_INT_IRQ1; /* RCR interrupt */
		}
	
		/* Start of vblank */
		if (current_scanline==240) {
			bldwolf_vblank=1;
			battlera_vh_raster_partial_refresh(Machine.scrbitmap,last_line,240);
			if (irq_enable != 0)
				return H6280_INT_IRQ1; /* VBL */
		}
	
		/* End of vblank */
		if (current_scanline==254) {
			bldwolf_vblank=0;
			last_line=0;
		}
	
		return ignore_interrupt.handler();
	} };
}
