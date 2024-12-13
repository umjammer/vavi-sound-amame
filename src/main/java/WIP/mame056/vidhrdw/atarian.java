/*##########################################################################

	atarian.c

	Common alphanumerics management functions for Atari raster games.

##########################################################################*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.atarianH.*;
import static arcadeflex056.fucPtr.*;
import static mame056.memoryH.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.Machine;

public class atarian
{
	
	
	/*##########################################################################
		TYPES & STRUCTURES
	##########################################################################*/
	
	/* internal structure containing the state of the alphanumerics */
	public static class atarian_data
	{
		int					colshift;			/* bits to shift X coordinate when looking up in VRAM */
/*TODO*///		int 				rowshift;			/* bits to shift Y coordinate when looking up in VRAM */
/*TODO*///		int					colmask;			/* mask to use when wrapping X coordinate in VRAM */
/*TODO*///		int 				rowmask;			/* mask to use when wrapping Y coordinate in VRAM */
/*TODO*///		int					vrammask;			/* combined mask when accessing VRAM with raw addresses */
/*TODO*///		int					vramsize;			/* total size of VRAM, in entries */

		int					tilexshift;			/* bits to shift X coordinate when drawing */
		int					tileyshift;			/* bits to shift Y coordinate when drawing */
/*TODO*///		int					tilewidth;			/* width of a single tile */
/*TODO*///		int					tileheight;			/* height of a single tile */

		int					xtiles;				/* number of visible X tiles */
		int					ytiles;				/* number of visible Y tiles */
/*TODO*///	
/*TODO*///		int					palettebase;		/* base palette entry */
/*TODO*///		int					maxcolors;			/* maximum number of colors */

		int					lookupmask;			/* mask for the lookup table */

		IntArray			lookup;				/* pointer to lookup table */
		IntArray			vram;				/* pointer to the VRAM pointer */

		int					bankbits;			/* current extra banking bits */

		GfxElement 	gfxelement;			/* copy of the GfxElement we're using */
	};
	
	
	
/*TODO*///	/*##########################################################################
/*TODO*///		MACROS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/* verification macro for void functions */
/*TODO*///	#define VERIFY(cond, msg) if (!(cond)) { logerror(msg); return; }
/*TODO*///	
/*TODO*///	/* verification macro for non-void functions */
/*TODO*///	#define VERIFYRETFREE(cond, msg, ret) if (!(cond)) { logerror(msg); atarian_free(); return (ret); }
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		GLOBAL VARIABLES
/*TODO*///	##########################################################################*/
/*TODO*///	
	public static IntArray atarian_0_base;
/*TODO*///	data16_t *atarian_1_base;
/*TODO*///	
/*TODO*///	data32_t *atarian_0_base32;
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		STATIC VARIABLES
/*TODO*///	##########################################################################*/
/*TODO*///	
	static atarian_data[] atarian = new atarian_data[ATARIAN_MAX];
	static int address_xor;
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		INLINE FUNCTIONS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		compute_log: Computes the number of bits necessary to
/*TODO*///		hold a given value. The input must be an even power of
/*TODO*///		two.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	INLINE int compute_log(int value)
/*TODO*///	{
/*TODO*///		int log = 0;
/*TODO*///	
/*TODO*///		if (value == 0)
/*TODO*///			return -1;
/*TODO*///		while (!(value & 1))
/*TODO*///			log++, value >>= 1;
/*TODO*///		if (value != 1)
/*TODO*///			return -1;
/*TODO*///		return log;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		round_to_powerof2: Rounds a number up to the nearest
/*TODO*///		power of 2. Even powers of 2 are rounded up to the
/*TODO*///		next greatest power (e.g., 4 returns 8).
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	INLINE int round_to_powerof2(int value)
/*TODO*///	{
/*TODO*///		int log = 0;
/*TODO*///	
/*TODO*///		if (value == 0)
/*TODO*///			return 1;
/*TODO*///		while ((value >>= 1) != 0)
/*TODO*///			log++;
/*TODO*///		return 1 << (log + 1);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		collapse_bits: Moving right-to-left, for each 1 bit in
/*TODO*///		the mask, copy the corresponding bit from the input
/*TODO*///		value into the result, packing the bits along the way.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	INLINE int collapse_bits(int value, int mask)
/*TODO*///	{
/*TODO*///		int testmask, ormask;
/*TODO*///		int result = 0;
/*TODO*///	
/*TODO*///		for (testmask = ormask = 1; testmask != 0; testmask <<= 1)
/*TODO*///			if (mask & testmask)
/*TODO*///			{
/*TODO*///				if (value & testmask)
/*TODO*///					result |= ormask;
/*TODO*///				ormask <<= 1;
/*TODO*///			}
/*TODO*///		return result;
/*TODO*///	}
	
	
	
	/*##########################################################################
		GLOBAL FUNCTIONS
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarian_init: Configures the alphanumerics using the input
		description. Allocates all memory necessary.
	---------------------------------------------------------------*/
	
	public static int atarian_init(int map, atarian_desc desc)
	{
/*TODO*///		int lookupcount = round_to_powerof2(desc->tilemask | desc->colormask | desc->hflipmask | desc->opaquemask) >> ATARIAN_LOOKUP_DATABITS;
/*TODO*///		struct GfxElement *gfx = Machine->gfx[desc->gfxindex];
/*TODO*///		struct atarian_data *an = &atarian[map];
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* sanity checks */
/*TODO*///		VERIFYRETFREE(map >= 0 && map < ATARIAN_MAX, "atarian_init: map out of range", 0)
/*TODO*///		VERIFYRETFREE(compute_log(desc->cols) != -1, "atarian_init: cols must be power of 2", 0)
/*TODO*///		VERIFYRETFREE(compute_log(desc->rows) != -1, "atarian_init: rows must be power of 2", 0)
/*TODO*///	
/*TODO*///		/* copy in the basic data */
/*TODO*///		an->colshift     = compute_log(desc->cols);
/*TODO*///		an->rowshift     = compute_log(desc->rows);
/*TODO*///		an->colmask      = desc->cols - 1;
/*TODO*///		an->rowmask      = desc->rows - 1;
/*TODO*///		an->vrammask     = (an->colmask << an->colshift) | (an->rowmask << an->rowshift);
/*TODO*///		an->vramsize     = round_to_powerof2(an->vrammask);
/*TODO*///	
/*TODO*///		an->tilexshift   = compute_log(gfx->width);
/*TODO*///		an->tileyshift   = compute_log(gfx->height);
/*TODO*///		an->tilewidth    = gfx->width;
/*TODO*///		an->tileheight   = gfx->height;
/*TODO*///	
/*TODO*///		an->xtiles       = Machine->drv->screen_width >> an->tilexshift;
/*TODO*///		an->ytiles       = Machine->drv->screen_height >> an->tileyshift;
/*TODO*///	
/*TODO*///		an->palettebase  = desc->palettebase;
/*TODO*///		an->maxcolors    = desc->maxcolors / gfx->color_granularity;
/*TODO*///	
/*TODO*///		an->lookupmask   = lookupcount - 1;
/*TODO*///		an->vram         = (map == 0) ? &atarian_0_base : &atarian_1_base;
/*TODO*///	
/*TODO*///		/* allocate the attribute lookup */
/*TODO*///		an->lookup = malloc(lookupcount * sizeof(an->lookup[0]));
/*TODO*///		VERIFYRETFREE(an->lookup, "atarian_init: out of memory for lookup", 0)
/*TODO*///	
/*TODO*///		/* fill in the attribute lookup */
/*TODO*///		for (i = 0; i < lookupcount; i++)
/*TODO*///		{
/*TODO*///			int value    = (i << ATARIAN_LOOKUP_DATABITS);
/*TODO*///			int tile     = collapse_bits(value, desc->tilemask);
/*TODO*///			int color    = collapse_bits(value, desc->colormask);
/*TODO*///			int flip     = collapse_bits(value, desc->hflipmask);
/*TODO*///			int opaque   = collapse_bits(value, desc->opaquemask);
/*TODO*///	
/*TODO*///			if (desc->palettesplit)
/*TODO*///				color = (color & desc->palettesplit) | ((color & ~desc->palettesplit) << 1);
/*TODO*///			an->lookup[i] = ATARIAN_LOOKUP_ENTRY(desc->gfxindex, tile, color, flip, opaque);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* make a copy of the original GfxElement structure */
/*TODO*///		an->gfxelement = *Machine->gfx[desc->gfxindex];
/*TODO*///	
/*TODO*///		/* adjust the color base */
/*TODO*///		an->gfxelement.colortable = &Machine->remapped_colortable[an->palettebase];
/*TODO*///	
/*TODO*///		/* by default we don't need to swap */
/*TODO*///		address_xor = 0;
/*TODO*///	
/*TODO*///		/* copy the 32-bit base */
/*TODO*///		if (cpunum_databus_width(0) == 32)
/*TODO*///		{
/*TODO*///			address_xor = 1;
/*TODO*///			atarian_0_base = (data16_t *)atarian_0_base32;
/*TODO*///		}
/*TODO*///	
/*TODO*///		logerror("atarian_init:\n");
/*TODO*///		logerror("  width=%d (shift=%d),  height=%d (shift=%d)\n", gfx->width, an->tilexshift, gfx->height, an->tileyshift);
/*TODO*///		logerror("  cols=%d  (mask=%X),   rows=%d   (mask=%X)\n", desc->cols, an->colmask, desc->rows, an->rowmask);
/*TODO*///		logerror("  VRAM mask=%X\n", an->vrammask);
	
		return 1;
	}
	
	
	/*---------------------------------------------------------------
		atarian_free: Frees any memory allocated for the alphanumerics.
	---------------------------------------------------------------*/
	
	public static void atarian_free()
	{
		int i;
	
		for (i = 0; i < ATARIAN_MAX; i++)
		{
			//atarian_data *an = &atarian[i];
	
			/* free the lookup */
			if (atarian[i].lookup != null)
				atarian[i].lookup = null;
			
		}
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarian_get_lookup: Fetches the lookup table so it can
/*TODO*///		be modified.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	UINT32 *atarian_get_lookup(int map, int *size)
/*TODO*///	{
/*TODO*///		struct atarian_data *an = &atarian[map];
/*TODO*///	
/*TODO*///		if (size)
/*TODO*///			*size = round_to_powerof2(an->lookupmask);
/*TODO*///		return an->lookup;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarian_render: Render the alphanumerics to the
		destination bitmap.
	---------------------------------------------------------------*/
	
	public static void atarian_render(int map, mame_bitmap bitmap)
	{
		rectangle clip = new rectangle(Machine.visible_area);
		atarian_data an = atarian[map];
		IntArray base = an.vram;
		int x, y;
	
		/* loop over rows */
		for (y = 0; y < an.ytiles; y++)
		{
			int offs = y << an.colshift;
	
			/* loop over columns */
			for (x = 0; x < an.xtiles; x++, offs++)
			{
				int data = base.read(offs ^ address_xor) | an.bankbits;
				int lookup = an.lookup.read((data >> ATARIAN_LOOKUP_DATABITS) & an.lookupmask);
				int code = ATARIAN_LOOKUP_CODE(lookup, data);
				int opaque = ATARIAN_LOOKUP_OPAQUE(lookup);
	
				/* only process opaque tiles or non-zero tiles */
				if (code!=0 || opaque!=0)
				{
					int color = ATARIAN_LOOKUP_COLOR(lookup);
					int hflip = ATARIAN_LOOKUP_HFLIP(lookup);
	
					drawgfx(bitmap, an.gfxelement, code, color, hflip, 0,
							x << an.tilexshift, y << an.tileyshift, clip,
							opaque!=0 ? TRANSPARENCY_NONE : TRANSPARENCY_PEN, 0);
				}
			}
		}
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarian_set_bankbits: Set the extra banking bits for the
/*TODO*///		alphanumerics.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarian_set_bankbits(int map, int bankbits)
/*TODO*///	{
/*TODO*///		struct atarian_data *an = &atarian[map];
/*TODO*///		an->bankbits = bankbits;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarian_get_bankbits: Returns the extra banking bits for
/*TODO*///		the alphanumerics.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int atarian_get_bankbits(int map)
/*TODO*///	{
/*TODO*///		return atarian[map].bankbits;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarian_get_vram: Returns a pointer to video RAM.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	data16_t *atarian_get_vram(int map)
/*TODO*///	{
/*TODO*///		return (map == 0) ? atarian_0_base : atarian_1_base;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarian_vram_w: Write handler for the alphanumerics RAM.
	---------------------------------------------------------------*/
	//16bits
	public static WriteHandlerPtr16 atarian_0_vram_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                int _res = atarian_0_base.read(offset);
                COMBINE_DATA(_res, data);
                atarian_0_base.write(offset, _res);
            }
        };
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarian_vram_w: Write handler for the alphanumerics RAM.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarian_1_vram_w )
/*TODO*///	{
/*TODO*///		COMBINE_DATA(&atarian_1_base[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarian_vram_w: Write handler for the alphanumerics RAM.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( atarian_0_vram32_w )
/*TODO*///	{
/*TODO*///		COMBINE_DATA(&atarian_0_base32[offset]);
/*TODO*///	}
}
