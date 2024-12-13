/***************************************************************************

	Atari System 2 hardware

****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.atarian.*;
import static WIP.mame056.vidhrdw.atarianH.*;
import static WIP.mame056.vidhrdw.atarimo.*;
import static WIP.mame056.vidhrdw.atarimoH.*;
import static WIP.mame056.vidhrdw.ataripf.*;
import static WIP.mame056.vidhrdw.ataripfH.*;
import static arcadeflex056.fucPtr.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.machine.slapstic.*;
import static mame056.cpuexec.*;
import static mame056.memoryH.*;

public class atarisy2
{
	
	
	
	/*************************************
	 *
	 *	Globals we own
	 *
	 *************************************/
	
	static int[] atarisys2_slapstic;
	
	
	
	/*************************************
	 *
	 *	Statics
	 *
	 *************************************/
	
	static int latched_vscroll;
	static int bankbits;
	static int videobank;
	static IntArray vram;
	
	
	
	/*************************************
	 *
	 *	Video system start
	 *
	 *************************************/
	
	public static VhStartPtr atarisys2_vh_start = new VhStartPtr() { public int handler() 
	{
		ataripf_desc pfdesc = new ataripf_desc
		(
			0,			/* index to which gfx system */
			128,64,		/* size of the playfield in tiles (x,y) */
			1,128,		/* tile_index = x * xmult + y * ymult (xmult,ymult) */
	
			0x80,		/* index of palette base */
			0x80,		/* maximum number of colors */
			0,			/* color XOR for shadow effect (if any) */
			0,			/* latch mask */
			0,			/* transparent pen mask */
	
			0xff07ff,	/* tile data index mask */
			0x003800,	/* tile data color mask */
			0,			/* tile data hflip mask */
			0,			/* tile data vflip mask */
			0x00c000	/* tile data priority mask */
		);
	
		atarimo_desc modesc = new atarimo_desc
		(
			1,					/* index to which gfx system */
			1,					/* number of motion object banks */
			1,					/* are the entries linked? */
			0,					/* are the entries split? */
			0,					/* render in reverse order? */
			0,					/* render in swapped X/Y order? */
			0,					/* does the neighbor bit affect the next object? */
			0,					/* pixels per SLIP entry (0 for no-slip) */
			8,					/* number of scanlines between MO updates */
	
			0x00,				/* base palette entry */
			0x40,				/* maximum number of colors */
			15,					/* transparent pen index */
	
			new atarimo_entry( 0,0,0,0x07f8 ),	/* mask for the link */
			new atarimo_entry( 0, 0, 0, 0 ),			/* mask for the graphics bank */
			new atarimo_entry( 0,0x07ff,0,0 ),	/* mask for the code index */
			new atarimo_entry( 0x0007,0,0,0 ),	/* mask for the upper code index */
			new atarimo_entry( 0,0,0,0x3000 ),	/* mask for the color */
			new atarimo_entry( 0,0,0xffc0,0 ),	/* mask for the X position */
			new atarimo_entry( 0x7fc0,0,0,0 ),	/* mask for the Y position */
			new atarimo_entry( 0, 0, 0, 0 ),			/* mask for the width, in tiles*/
			new atarimo_entry( 0,0x3800,0,0 ),	/* mask for the height, in tiles */
			new atarimo_entry( 0,0x4000,0,0 ),	/* mask for the horizontal flip */
			new atarimo_entry( 0, 0, 0, 0 ),			/* mask for the vertical flip */
			new atarimo_entry( 0,0,0,0xc000 ),	/* mask for the priority */
			new atarimo_entry( 0,0x8000,0,0 ),	/* mask for the neighbor */
			new atarimo_entry( 0, 0, 0, 0 ),			/* mask for absolute coordinates */
	
			new atarimo_entry( 0, 0, 0, 0 ),			/* mask for the ignore value */
			0,					/* resulting value to indicate "ignore" */
			null					/* callback routine for ignored entries */
		);
	
		atarian_desc andesc = new atarian_desc
		(
			2,			/* index to which gfx system */
			64,64,		/* size of the alpha RAM in tiles (x,y) */
	
			0x40,		/* index of palette base */
			0x40,		/* maximum number of colors */
			0,			/* mask of the palette split */
	
			0x03ff,		/* tile data index mask */
			0xe000,		/* tile data color mask */
			0,			/* tile data hflip mask */
			0			/* tile data opacity mask */
		);
	
		IntArray pflookup;
		int i;
                int [] size = new int[1];
	
		/* allocate banked memory */
		vram = new IntArray(0x8000);
		if (vram == null)
			return 1;
		atarian_0_base = new IntArray(vram, 0x0000);
		atarimo_0_spriteram = new IntArray(vram, 0x0c00);
		ataripf_0_base = new IntArray(vram, 0x2000);
	
		/* initialize the playfield */
		if (ataripf_init(0, pfdesc) == 0)
                {
			atarian_free();
                        vram = null;
                        return 1;
                }        
	
		/* initialize the motion objects */
		if (atarimo_init(0, modesc) == 0){
			ataripf_free();
                        atarian_free();
                        vram = null;
                        
                        return 1;
                }
	
		/* initialize the alphanumerics */
		if (atarian_init(0, andesc) == 0){
			vram = null;
                        
                        return 1;
                }
	
		/* modify the playfield lookup table to support our odd banking system */
		pflookup = ataripf_get_lookup(0, size);
		for (i = 0; i < size[0]; i++)
		{
			int code = i << ATARIPF_LOOKUP_DATABITS;
			int bankselect = (code >> 10) & 1;
			int bank = (code >> (16 + 4 * bankselect)) & 15;
	
			code = (code & 0x3ff) | (bank << 10);
                        int[] _data = new int[]{ pflookup.read(i) };
			ATARIPF_LOOKUP_SET_CODE(_data, code);
                        pflookup.write(i, _data[0]);
		}
	
		/* reset the statics */
		bankbits = 0;
		videobank = 0;
		return 0;

	} };
	
	
	
	/*************************************
	 *
	 *	Video system shutdown
	 *
	 *************************************/
	
	public static VhStopPtr atarisys2_vh_stop = new VhStopPtr() { public void handler() 
	{
		vram = null;
		atarian_free();
		atarimo_free();
		ataripf_free();
	} };
	
	
	
	/*************************************
	 *
	 *	Scroll/playfield bank write
	 *
	 *************************************/
	//16bits
	public static WriteHandlerPtr atarisys2_hscroll_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int scanline = cpu_getscanline() + 1;
		int newscroll = (ataripf_get_xscroll(0) << 6) | ((bankbits >> 16) & 0xf);
		COMBINE_DATA(newscroll, data);
	
		/* update the playfield parameters - hscroll is clocked on the following scanline */
		ataripf_set_xscroll(0, (newscroll >> 6) & 0x03ff, scanline);
		bankbits = (bankbits & 0xf00000) | ((newscroll & 0xf) << 16);
		ataripf_set_bankbits(0, bankbits, scanline);
            }
        };
	
	//16bits
	public static WriteHandlerPtr atarisys2_vscroll_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		int scanline = cpu_getscanline() + 1;
		int newscroll = (latched_vscroll << 6) | ((bankbits >> 20) & 0xf);
		COMBINE_DATA(newscroll, data);
	
		/* if bit 4 is zero, the scroll value is clocked in right away */
		latched_vscroll = (newscroll >> 6) & 0x01ff;
		if ((newscroll & 0x10)==0) ataripf_set_yscroll(0, latched_vscroll, scanline);
	
		/* update the playfield parameters */
		bankbits = (bankbits & 0x0f0000) | ((newscroll & 0xf) << 20);
		ataripf_set_bankbits(0, bankbits, scanline);
            }
        };
	
	
	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Palette RAM write handler
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarisys2_paletteram_w )
/*TODO*///	{
/*TODO*///		static const int intensity_table[16] =
/*TODO*///		{
/*TODO*///			#define ZB 115
/*TODO*///			#define Z3 78
/*TODO*///			#define Z2 37
/*TODO*///			#define Z1 17
/*TODO*///			#define Z0 9
/*TODO*///			0, ZB+Z0, ZB+Z1, ZB+Z1+Z0, ZB+Z2, ZB+Z2+Z0, ZB+Z2+Z1, ZB+Z2+Z1+Z0,
/*TODO*///			ZB+Z3, ZB+Z3+Z0, ZB+Z3+Z1, ZB+Z3+Z1+Z0,ZB+ Z3+Z2, ZB+Z3+Z2+Z0, ZB+Z3+Z2+Z1, ZB+Z3+Z2+Z1+Z0
/*TODO*///		};
/*TODO*///		static const int color_table[16] =
/*TODO*///			{ 0x0, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xe, 0xf, 0xf };
/*TODO*///	
/*TODO*///		int newword, inten, red, green, blue;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&paletteram16[offset]);
/*TODO*///		newword = paletteram16[offset];
/*TODO*///	
/*TODO*///		inten = intensity_table[newword & 15];
/*TODO*///		red = (color_table[(newword >> 12) & 15] * inten) >> 4;
/*TODO*///		green = (color_table[(newword >> 8) & 15] * inten) >> 4;
/*TODO*///		blue = (color_table[(newword >> 4) & 15] * inten) >> 4;
/*TODO*///		palette_set_color(offset, red, green, blue);
/*TODO*///	}
	
	
	
	/*************************************
	 *
	 *	Video RAM bank read/write handlers
	 *
	 *************************************/
	
	public static ReadHandlerPtr atarisys2_slapstic_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int result = atarisys2_slapstic[offset];
		slapstic_tweak(offset);
	
		/* an extra tweak for the next opcode fetch */
		videobank = slapstic_tweak(0x1234) * 0x1000;
		return result;
            }
        };
	
	
/*TODO*///	WRITE16_HANDLER( atarisys2_slapstic_w )
/*TODO*///	{
/*TODO*///		slapstic_tweak(offset);
/*TODO*///	
/*TODO*///		/* an extra tweak for the next opcode fetch */
/*TODO*///		videobank = slapstic_tweak(0x1234) * 0x1000;
/*TODO*///	}
	
	
	
	/*************************************
	 *
	 *	Video RAM read/write handlers
	 *
	 *************************************/
	
	public static ReadHandlerPtr atarisys2_videoram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		return vram.read(offset | videobank);
            }
        };
	
	//16bits
	public static WriteHandlerPtr atarisys2_videoram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                
                //hack
                int mem_mask = 0;
            
		int offs = offset | videobank;
	
		/* alpharam? */
		if (offs < 0x0c00)
			atarian_0_vram_w.handler(offs, data, mem_mask);
	
		/* spriteram? */
		else if (offs < 0x1000)
		{
			atarimo_0_spriteram_w.handler(offs - 0x0c00, data, mem_mask);
	
			/* force an update if the link of object 0 changes */
			if (offs == 0x0c03)
				atarimo_force_update(0, cpu_getscanline() + 1);
		}
	
		/* playfieldram? */
		else if (offs >= 0x2000)
			ataripf_0_simple_w.handler(offs - 0x2000, data, mem_mask);
	
		/* generic case */
		else
		{
                        int _res = vram.read(offs);
			COMBINE_DATA(_res, data);
                        vram.write(offs, _res);
		}
            }
        };
	
	
	
	/*************************************
	 *
	 *	Periodic scanline updater
	 *
	 *************************************/
	
	public static void atarisys2_scanline_update(int scanline)
	{
		/* latch the Y scroll value */
		if (scanline == 0)
			ataripf_set_yscroll(0, latched_vscroll, 0);
	}
	
	
	
	/*************************************
	 *
	 *	Overrender callback
	 *
	 *************************************/
	
	static ataripf_overrender_cb overrender_callback = new ataripf_overrender_cb() {
            public int handler(ataripf_overrender_data data, int state) {
                /* we need to check tile-by-tile, so always return OVERRENDER_SOME */
		if (state == OVERRENDER_BEGIN)
		{
			/* by default, draw anywhere the MO pen was 15 */
			data.drawmode = TRANSPARENCY_PENS;
			data.drawpens = 0x00ff;
			data.maskpens = 0x8000;
			return OVERRENDER_SOME;
		}
	
		/* handle a query */
		else if (state == OVERRENDER_QUERY)
		{
			int mopriority = data.mopriority << 1;
			int pfpriority = ((~data.pfpriority & 3) << 1) | 1;
	
			/* this equation comes from the schematics */
			return ((mopriority + pfpriority) & 4)!=0 ? OVERRENDER_YES : OVERRENDER_NO;
		}
		return 0;
            }
        };
        
	
	
	/*************************************
	 *
	 *	Main refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr atarisys2_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* draw the layers */
		ataripf_render(0, bitmap);
		atarimo_render(0, bitmap, overrender_callback, null);
		atarian_render(0, bitmap);
	} };
}
