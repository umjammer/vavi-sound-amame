/*##########################################################################

	atarimo.c

	Common motion object management functions for Atari raster games.

##########################################################################*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.osdepend.logerror;
import static common.subArrays.*;
import static WIP.mame056.vidhrdw.atarimoH.*;
import static WIP.mame056.vidhrdw.ataripf.*;
import static WIP.mame056.vidhrdw.ataripfH.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.drawgfxH.*;
import static mame056.mame.Machine;
import static mame056.mameH.*;
import static mame056.memoryH.*;
import static mame056.timer.*;

public class atarimo
{
	
	
	/*##########################################################################
		TYPES & STRUCTURES
	##########################################################################*/
	
	/* internal structure containing a word index, shift and mask */
	public static class atarimo_mask
	{
		int					word;				/* word index */
		int					shift;				/* shift amount */
		int					mask;				/* final mask */
	};
	
	/* internal cache entry */
	public static class atarimo_cache
	{
		int				scanline;			/* effective scanline */
		atarimo_entry entry;				/* entry data */
	};
	
	/* internal structure containing the state of the motion objects */
	public static class atarimo_data
	{
		int					timerallocated;		/* true if we've allocated the timer */
		int					gfxchanged;			/* true if the gfx info has changed */
		GfxElement[]	gfxelement = new GfxElement[MAX_GFX_ELEMENTS]; /* local copy of graphics elements */

		int					linked;				/* are the entries linked? */
		int					split;				/* are entries split or together? */
		int					reverse;			/* render in reverse order? */
/*TODO*///		int					swapxy;				/* render in swapped X/Y order? */
/*TODO*///		UINT8				nextneighbor;		/* does the neighbor bit affect the next object? */
		int					slipshift;			/* log2(pixels_per_SLIP) */
		int					updatescans;		/* number of scanlines per update */

		int					entrycount;			/* number of entries per bank */
		int					entrybits;			/* number of bits needed to represent entrycount */
/*TODO*///		int					bankcount;			/* number of banks */

/*TODO*///		int					tilexshift;			/* bits to shift X coordinate when drawing */
/*TODO*///		int					tileyshift;			/* bits to shift Y coordinate when drawing */
/*TODO*///		int					bitmapwidth;		/* width of the full playfield bitmap */
/*TODO*///		int					bitmapheight;		/* height of the full playfield bitmap */
/*TODO*///		int					bitmapxmask;		/* x coordinate mask for the playfield bitmap */
		int					bitmapymask;		/* y coordinate mask for the playfield bitmap */

/*TODO*///		int					spriterammask;		/* combined mask when accessing sprite RAM with raw addresses */
/*TODO*///		int					spriteramsize;		/* total size of sprite RAM, in entries */
/*TODO*///		int					sliprammask;		/* combined mask when accessing SLIP RAM with raw addresses */
/*TODO*///		int					slipramsize;		/* total size of SLIP RAM, in entries */

		int					palettebase;		/* base palette entry */
/*TODO*///		int					maxcolors;			/* maximum number of colors */
/*TODO*///		int					transpen;			/* transparent pen index */

		int					bank;				/* current bank number */
		int					xscroll;			/* current x scroll offset */
		int					yscroll;			/* current y scroll offset */

		atarimo_mask	linkmask;			/* mask for the link */
		atarimo_mask gfxmask;			/* mask for the graphics bank */
/*TODO*///		struct atarimo_mask	codemask;			/* mask for the code index */
/*TODO*///		struct atarimo_mask codehighmask;		/* mask for the upper code index */
/*TODO*///		struct atarimo_mask	colormask;			/* mask for the color */
/*TODO*///		struct atarimo_mask	xposmask;			/* mask for the X position */
/*TODO*///		struct atarimo_mask	yposmask;			/* mask for the Y position */
/*TODO*///		struct atarimo_mask	widthmask;			/* mask for the width, in tiles*/
/*TODO*///		struct atarimo_mask	heightmask;			/* mask for the height, in tiles */
/*TODO*///		struct atarimo_mask	hflipmask;			/* mask for the horizontal flip */
/*TODO*///		struct atarimo_mask	vflipmask;			/* mask for the vertical flip */
/*TODO*///		struct atarimo_mask	prioritymask;		/* mask for the priority */
/*TODO*///		struct atarimo_mask	neighbormask;		/* mask for the neighbor */
/*TODO*///		struct atarimo_mask absolutemask;		/* mask for absolute coordinates */

/*TODO*///		struct atarimo_mask ignoremask;			/* mask for the ignore value */
/*TODO*///		int					ignorevalue;		/* resulting value to indicate "ignore" */
/*TODO*///		atarimo_special_cb	ignorecb;			/* callback routine for ignored entries */
/*TODO*///		int					codehighshift;		/* shift count for the upper code */

		atarimo_entry[] spriteram;		/* pointer to sprite RAM */
		int[]			slipram;			/* pointer to the SLIP RAM pointer */
/*TODO*///		UINT16 *			codelookup;			/* lookup table for codes */
/*TODO*///		UINT8 *				colorlookup;		/* lookup table for colors */
		UBytePtr				gfxlookup;			/* lookup table for graphics */

		atarimo_cache cache;			/* pointer to the cache data */
		atarimo_cache endcache;			/* end of the cache */
		atarimo_cache curcache;			/* current cache entry */
		atarimo_cache prevcache;		/* previous cache entry */

		ataripf_overrender_cb overrender0;		/* overrender callback for PF 0 */
		ataripf_overrender_cb overrender1;		/* overrender callback for PF 1 */
		rectangle	process_clip;		/* (during processing) the clip rectangle */
		mame_bitmap				process_param;		/* (during processing) the callback parameter */
/*TODO*///		int					last_xpos;			/* (during processing) the previous X position */
		int					next_xpos;			/* (during processing) the next X position */
		int					process_xscroll;	/* (during processing) the X scroll position */
		int					process_yscroll;	/* (during processing) the Y scroll position */
	};
	
	
	/* callback function for the internal playfield processing mechanism */
	public static abstract interface mo_callback {
            public abstract void handler(atarimo_data pf, atarimo_entry entry);
        }
	
	
/*TODO*///	/*##########################################################################
/*TODO*///		MACROS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/* verification macro for void functions */
/*TODO*///	#define VERIFY(cond, msg) if (!(cond)) { logerror(msg); return; }
/*TODO*///	
/*TODO*///	/* verification macro for non-void functions */
/*TODO*///	#define VERIFYRETFREE(cond, msg, ret) if (!(cond)) { logerror(msg); atarimo_free(); return (ret); }
	
	
	/* data extraction */
	public static int EXTRACT_DATA(atarimo_entry _input, atarimo_mask _mask){
            return (((_input).data[(_mask).word] >> (_mask).shift) & (_mask).mask);
        }
	
	
	
	/*##########################################################################
		GLOBAL VARIABLES
	##########################################################################*/
	
	public static IntArray atarimo_0_spriteram;
/*TODO*///	data16_t *atarimo_0_slipram;
/*TODO*///	
/*TODO*///	data16_t *atarimo_1_spriteram;
/*TODO*///	data16_t *atarimo_1_slipram;
	
	
	
	/*##########################################################################
		STATIC VARIABLES
	##########################################################################*/
	
	public static atarimo_data[] atarimo = new atarimo_data[ATARIMO_MAX];
	
	
	
/*TODO*///	/*##########################################################################
/*TODO*///		STATIC FUNCTION DECLARATIONS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	static void mo_process(struct atarimo_data *mo, mo_callback callback, void *param, const struct rectangle *clip);
/*TODO*///	static void mo_update(struct atarimo_data *mo, int scanline);
/*TODO*///	static void mo_render_callback(struct atarimo_data *mo, const struct atarimo_entry *entry);
/*TODO*///	static void mo_scanline_callback(int scanline);
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
/*TODO*///		convert_mask: Converts a 4-word mask into a word index,
/*TODO*///		shift, and adjusted mask. Returns 0 if invalid.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	INLINE int convert_mask(const struct atarimo_entry *input, struct atarimo_mask *result)
/*TODO*///	{
/*TODO*///		int i, temp;
/*TODO*///	
/*TODO*///		/* determine the word and make sure it's only 1 */
/*TODO*///		result->word = -1;
/*TODO*///		for (i = 0; i < 4; i++)
/*TODO*///			if (input->data[i])
/*TODO*///			{
/*TODO*///				if (result->word == -1)
/*TODO*///					result->word = i;
/*TODO*///				else
/*TODO*///					return 0;
/*TODO*///			}
/*TODO*///	
/*TODO*///		/* if all-zero, it's valid */
/*TODO*///		if (result->word == -1)
/*TODO*///		{
/*TODO*///			result->word = result->shift = result->mask = 0;
/*TODO*///			return 1;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* determine the shift and final mask */
/*TODO*///		result->shift = 0;
/*TODO*///		temp = input->data[result->word];
/*TODO*///		while (!(temp & 1))
/*TODO*///		{
/*TODO*///			result->shift++;
/*TODO*///			temp >>= 1;
/*TODO*///		}
/*TODO*///		result->mask = temp;
/*TODO*///		return 1;
/*TODO*///	}
	
	
	
	/*##########################################################################
		GLOBAL FUNCTIONS
	##########################################################################*/
	
	/*---------------------------------------------------------------
		atarimo_init: Configures the motion objects using the input
		description. Allocates all memory necessary and generates
		the attribute lookup table.
	---------------------------------------------------------------*/
	
	public static int atarimo_init(int map, atarimo_desc desc)
	{
/*TODO*///		struct GfxElement *gfx = Machine->gfx[desc->gfxindex];
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		VERIFYRETFREE(map >= 0 && map < ATARIMO_MAX, "atarimo_init: map out of range", 0)
/*TODO*///	
/*TODO*///		/* determine the masks first */
/*TODO*///		convert_mask(&desc->linkmask,     &mo->linkmask);
/*TODO*///		convert_mask(&desc->gfxmask,      &mo->gfxmask);
/*TODO*///		convert_mask(&desc->codemask,     &mo->codemask);
/*TODO*///		convert_mask(&desc->codehighmask, &mo->codehighmask);
/*TODO*///		convert_mask(&desc->colormask,    &mo->colormask);
/*TODO*///		convert_mask(&desc->xposmask,     &mo->xposmask);
/*TODO*///		convert_mask(&desc->yposmask,     &mo->yposmask);
/*TODO*///		convert_mask(&desc->widthmask,    &mo->widthmask);
/*TODO*///		convert_mask(&desc->heightmask,   &mo->heightmask);
/*TODO*///		convert_mask(&desc->hflipmask,    &mo->hflipmask);
/*TODO*///		convert_mask(&desc->vflipmask,    &mo->vflipmask);
/*TODO*///		convert_mask(&desc->prioritymask, &mo->prioritymask);
/*TODO*///		convert_mask(&desc->neighbormask, &mo->neighbormask);
/*TODO*///		convert_mask(&desc->absolutemask, &mo->absolutemask);
/*TODO*///	
/*TODO*///		/* copy in the basic data */
/*TODO*///		mo->timerallocated = 0;
/*TODO*///		mo->gfxchanged    = 0;
/*TODO*///	
/*TODO*///		mo->linked        = desc->linked;
/*TODO*///		mo->split         = desc->split;
/*TODO*///		mo->reverse       = desc->reverse;
/*TODO*///		mo->swapxy        = desc->swapxy;
/*TODO*///		mo->nextneighbor  = desc->nextneighbor;
/*TODO*///		mo->slipshift     = desc->slipheight ? compute_log(desc->slipheight) : 0;
/*TODO*///		mo->updatescans   = desc->updatescans;
/*TODO*///	
/*TODO*///		mo->entrycount    = round_to_powerof2(mo->linkmask.mask);
/*TODO*///		mo->entrybits     = compute_log(mo->entrycount);
/*TODO*///		mo->bankcount     = desc->banks;
/*TODO*///	
/*TODO*///		mo->tilexshift    = compute_log(gfx->width);
/*TODO*///		mo->tileyshift    = compute_log(gfx->height);
/*TODO*///		mo->bitmapwidth   = round_to_powerof2(mo->xposmask.mask);
/*TODO*///		mo->bitmapheight  = round_to_powerof2(mo->yposmask.mask);
/*TODO*///		mo->bitmapxmask   = mo->bitmapwidth - 1;
/*TODO*///		mo->bitmapymask   = mo->bitmapheight - 1;
/*TODO*///	
/*TODO*///		mo->spriteramsize = mo->bankcount * mo->entrycount;
/*TODO*///		mo->spriterammask = mo->spriteramsize - 1;
/*TODO*///		mo->slipramsize   = mo->bitmapheight >> mo->tileyshift;
/*TODO*///		mo->sliprammask   = mo->slipramsize - 1;
/*TODO*///	
/*TODO*///		mo->palettebase   = desc->palettebase;
/*TODO*///		mo->maxcolors     = desc->maxcolors / gfx->color_granularity;
/*TODO*///		mo->transpen      = desc->transpen;
/*TODO*///	
/*TODO*///		mo->bank          = 0;
/*TODO*///		mo->xscroll       = 0;
/*TODO*///		mo->yscroll       = 0;
/*TODO*///	
/*TODO*///		convert_mask(&desc->ignoremask, &mo->ignoremask);
/*TODO*///		mo->ignorevalue   = desc->ignorevalue;
/*TODO*///		mo->ignorecb      = desc->ignorecb;
/*TODO*///		mo->codehighshift = compute_log(round_to_powerof2(mo->codemask.mask));
/*TODO*///	
/*TODO*///		mo->slipram       = (map == 0) ? &atarimo_0_slipram : &atarimo_1_slipram;
/*TODO*///	
/*TODO*///		/* allocate the priority bitmap */
/*TODO*///		priority_bitmap = bitmap_alloc_depth(Machine->drv->screen_width, Machine->drv->screen_height, 8);
/*TODO*///		VERIFYRETFREE(priority_bitmap, "atarimo_init: out of memory for priority bitmap", 0)
/*TODO*///	
/*TODO*///		/* allocate the spriteram */
/*TODO*///		mo->spriteram = malloc(sizeof(mo->spriteram.read(0)) * mo->spriteramsize);
/*TODO*///		VERIFYRETFREE(mo->spriteram, "atarimo_init: out of memory for spriteram", 0)
/*TODO*///	
/*TODO*///		/* clear it to zero */
/*TODO*///		memset(mo->spriteram, 0, sizeof(mo->spriteram.read(0)) * mo->spriteramsize);
/*TODO*///	
/*TODO*///		/* allocate the code lookup */
/*TODO*///		mo->codelookup = malloc(sizeof(mo->codelookup[0]) * round_to_powerof2(mo->codemask.mask));
/*TODO*///		VERIFYRETFREE(mo->codelookup, "atarimo_init: out of memory for code lookup", 0)
/*TODO*///	
/*TODO*///		/* initialize it 1:1 */
/*TODO*///		for (i = 0; i < round_to_powerof2(mo->codemask.mask); i++)
/*TODO*///			mo->codelookup[i] = i;
/*TODO*///	
/*TODO*///		/* allocate the color lookup */
/*TODO*///		mo->colorlookup = malloc(sizeof(mo->colorlookup[0]) * round_to_powerof2(mo->colormask.mask));
/*TODO*///		VERIFYRETFREE(mo->colorlookup, "atarimo_init: out of memory for color lookup", 0)
/*TODO*///	
/*TODO*///		/* initialize it 1:1 */
/*TODO*///		for (i = 0; i < round_to_powerof2(mo->colormask.mask); i++)
/*TODO*///			mo->colorlookup[i] = i;
/*TODO*///	
/*TODO*///		/* allocate the gfx lookup */
/*TODO*///		mo->gfxlookup = malloc(sizeof(mo->gfxlookup[0]) * round_to_powerof2(mo->gfxmask.mask));
/*TODO*///		VERIFYRETFREE(mo->gfxlookup, "atarimo_init: out of memory for gfx lookup", 0)
/*TODO*///	
/*TODO*///		/* initialize it with the gfxindex we were passed in */
/*TODO*///		for (i = 0; i < round_to_powerof2(mo->gfxmask.mask); i++)
/*TODO*///			mo->gfxlookup[i] = desc->gfxindex;
/*TODO*///	
/*TODO*///		/* allocate the cache */
/*TODO*///		mo->cache = malloc(mo->entrycount * Machine->drv->screen_height * sizeof(mo->cache[0]));
/*TODO*///		VERIFYRETFREE(mo->cache, "atarimo_init: out of memory for cache", 0)
/*TODO*///		mo->endcache = mo->cache + mo->entrycount * Machine->drv->screen_height;
/*TODO*///	
/*TODO*///		/* initialize the end/last pointers */
/*TODO*///		mo->curcache = mo->cache;
/*TODO*///		mo->prevcache = NULL;
/*TODO*///	
/*TODO*///		/* initialize the gfx elements */
/*TODO*///		mo->gfxelement[desc->gfxindex] = *Machine->gfx[desc->gfxindex];
/*TODO*///		mo->gfxelement[desc->gfxindex].colortable = &Machine->remapped_colortable[mo->palettebase];
/*TODO*///	
/*TODO*///		logerror("atarimo_init:\n");
/*TODO*///		logerror("  width=%d (shift=%d),  height=%d (shift=%d)\n", gfx->width, mo->tilexshift, gfx->height, mo->tileyshift);
/*TODO*///		logerror("  spriteram mask=%X, size=%d\n", mo->spriterammask, mo->spriteramsize);
/*TODO*///		logerror("  slipram mask=%X, size=%d\n", mo->sliprammask, mo->slipramsize);
/*TODO*///		logerror("  bitmap size=%dx%d\n", mo->bitmapwidth, mo->bitmapheight);
	
		return 1;
	}
	
	
	/*---------------------------------------------------------------
		atarimo_free: Frees any memory allocated for motion objects.
	---------------------------------------------------------------*/
	
	public static void atarimo_free()
	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* free the motion object data */
/*TODO*///		for (i = 0; i < ATARIMO_MAX; i++)
/*TODO*///		{
/*TODO*///			struct atarimo_data *mo = &atarimo[i];
/*TODO*///	
/*TODO*///			/* free the priority bitmap */
/*TODO*///			if (priority_bitmap)
/*TODO*///				free(priority_bitmap);
/*TODO*///			priority_bitmap = NULL;
/*TODO*///	
/*TODO*///			/* free the spriteram */
/*TODO*///			if (mo->spriteram)
/*TODO*///				free(mo->spriteram);
/*TODO*///			mo->spriteram = NULL;
/*TODO*///	
/*TODO*///			/* free the codelookup */
/*TODO*///			if (mo->codelookup)
/*TODO*///				free(mo->codelookup);
/*TODO*///			mo->codelookup = NULL;
/*TODO*///	
/*TODO*///			/* free the codelookup */
/*TODO*///			if (mo->codelookup)
/*TODO*///				free(mo->codelookup);
/*TODO*///			mo->codelookup = NULL;
/*TODO*///	
/*TODO*///			/* free the colorlookup */
/*TODO*///			if (mo->colorlookup)
/*TODO*///				free(mo->colorlookup);
/*TODO*///			mo->colorlookup = NULL;
/*TODO*///	
/*TODO*///			/* free the gfxlookup */
/*TODO*///			if (mo->gfxlookup)
/*TODO*///				free(mo->gfxlookup);
/*TODO*///			mo->gfxlookup = NULL;
/*TODO*///	
/*TODO*///			/* free the cache */
/*TODO*///			if (mo->cache)
/*TODO*///				free(mo->cache);
/*TODO*///			mo->cache = NULL;
/*TODO*///		}
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_code_lookup: Returns a pointer to the code
/*TODO*///		lookup table.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	UINT16 *atarimo_get_code_lookup(int map, int *size)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///	
/*TODO*///		if (size)
/*TODO*///			*size = round_to_powerof2(mo->codemask.mask);
/*TODO*///		return mo->codelookup;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_code_lookup: Returns a pointer to the code
/*TODO*///		lookup table.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	UINT8 *atarimo_get_color_lookup(int map, int *size)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///	
/*TODO*///		if (size)
/*TODO*///			*size = round_to_powerof2(mo->colormask.mask);
/*TODO*///		return mo->colorlookup;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_code_lookup: Returns a pointer to the code
/*TODO*///		lookup table.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	UINT8 *atarimo_get_gfx_lookup(int map, int *size)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///	
/*TODO*///		mo->gfxchanged = 1;
/*TODO*///		if (size)
/*TODO*///			*size = round_to_powerof2(mo->gfxmask.mask);
/*TODO*///		return mo->gfxlookup;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarimo_render: Render the motion objects to the
		destination bitmap.
	---------------------------------------------------------------*/
	
	public static void atarimo_render(int map, mame_bitmap bitmap, ataripf_overrender_cb callback1, ataripf_overrender_cb callback2)
	{
		atarimo_data mo = atarimo[map];
	
		/* render via the standard render callback */
		mo.overrender0 = callback1;
		mo.overrender1 = callback2;
		mo_process(mo, mo_render_callback, bitmap, null);
	
		/* set a timer to call the eof function on scanline 0 */
		if (mo.timerallocated == 0)
		{
			timer_set(cpu_getscanlinetime(0), 0 | (map << 16), mo_scanline_callback);
			mo.timerallocated = 1;
		}
	}
	
	
	/*---------------------------------------------------------------
		atarimo_force_update: Force an update for the given
		scanline.
	---------------------------------------------------------------*/
	
	public static void atarimo_force_update(int map, int scanline)
	{
                atarimo_data _var=atarimo[map];
		mo_update(_var, scanline);
                atarimo[map] = _var;
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_set_bank: Set the banking value for
/*TODO*///		the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarimo_set_bank(int map, int bank, int scanline)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///	
/*TODO*///		if (mo->bank != bank)
/*TODO*///		{
/*TODO*///			mo->bank = bank;
/*TODO*///			mo_update(mo, scanline);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_set_palettebase: Set the palette base for
/*TODO*///		the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarimo_set_palettebase(int map, int base, int scanline)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		mo->palettebase = base;
/*TODO*///		for (i = 0; i < MAX_GFX_ELEMENTS; i++)
/*TODO*///			mo->gfxelement[i].colortable = &Machine->remapped_colortable[base];
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_set_xscroll: Set the horizontal scroll value for
/*TODO*///		the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarimo_set_xscroll(int map, int xscroll, int scanline)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///	
/*TODO*///		if (mo->xscroll != xscroll)
/*TODO*///		{
/*TODO*///			mo->xscroll = xscroll;
/*TODO*///			mo_update(mo, scanline);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_set_yscroll: Set the vertical scroll value for
/*TODO*///		the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void atarimo_set_yscroll(int map, int yscroll, int scanline)
/*TODO*///	{
/*TODO*///		struct atarimo_data *mo = &atarimo[map];
/*TODO*///	
/*TODO*///		if (mo->yscroll != yscroll)
/*TODO*///		{
/*TODO*///			mo->yscroll = yscroll;
/*TODO*///			mo_update(mo, scanline);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_bank: Returns the banking value
/*TODO*///		for the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int atarimo_get_bank(int map)
/*TODO*///	{
/*TODO*///		return atarimo[map].bank;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_palettebase: Returns the palette base
/*TODO*///		for the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int atarimo_get_palettebase(int map)
/*TODO*///	{
/*TODO*///		return atarimo[map].palettebase;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_xscroll: Returns the horizontal scroll value
/*TODO*///		for the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int atarimo_get_xscroll(int map)
/*TODO*///	{
/*TODO*///		return atarimo[map].xscroll;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_get_yscroll: Returns the vertical scroll value for
/*TODO*///		the motion objects.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int atarimo_get_yscroll(int map)
/*TODO*///	{
/*TODO*///		return atarimo[map].yscroll;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		atarimo_0_spriteram_w: Write handler for the spriteram.
	---------------------------------------------------------------*/
	
	public static WriteHandlerPtr16 atarimo_0_spriteram_w = new WriteHandlerPtr16() {
            @Override
            public void handler(int offset, int data, int d2) {
                int entry, idx, bank;
                
                int _res = atarimo_0_spriteram.read(offset);
	
		COMBINE_DATA(_res, data);
                atarimo_0_spriteram.write(offset, _res);
                
		if (atarimo[0].split != 0)
		{
			entry = offset & atarimo[0].linkmask.mask;
			idx = (offset >> atarimo[0].entrybits) & 3;
		}
		else
		{
			entry = (offset >> 2) & atarimo[0].linkmask.mask;
			idx = offset & 3;
		}
		bank = offset >> (2 + atarimo[0].entrybits);
		
                _res=atarimo[0].spriteram[(bank << atarimo[0].entrybits) + entry].data[idx];
                COMBINE_DATA(_res, data);
                atarimo[0].spriteram[(bank << atarimo[0].entrybits) + entry].data[idx]=_res;
            }
        };
	
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_1_spriteram_w: Write handler for the spriteram.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarimo_1_spriteram_w )
/*TODO*///	{
/*TODO*///		int entry, idx, bank;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&atarimo_1_spriteram[offset]);
/*TODO*///		if (atarimo[1].split)
/*TODO*///		{
/*TODO*///			entry = offset & atarimo[1].linkmask.mask;
/*TODO*///			idx = (offset >> atarimo[1].entrybits) & 3;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			entry = (offset >> 2) & atarimo[1].linkmask.mask;
/*TODO*///			idx = offset & 3;
/*TODO*///		}
/*TODO*///		bank = offset >> (2 + atarimo[1].entrybits);
/*TODO*///		COMBINE_DATA(&atarimo[1].spriteram.read((bank << atarimo[1).entrybits) + entry].data[idx]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_0_spriteram_expanded_w: Write handler for the
/*TODO*///		expanded form of spriteram.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarimo_0_spriteram_expanded_w )
/*TODO*///	{
/*TODO*///		int entry, idx, bank;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&atarimo_0_spriteram[offset]);
/*TODO*///		if (!(offset & 1))
/*TODO*///		{
/*TODO*///			offset >>= 1;
/*TODO*///			if (atarimo[0].split)
/*TODO*///			{
/*TODO*///				entry = offset & atarimo[0].linkmask.mask;
/*TODO*///				idx = (offset >> atarimo[0].entrybits) & 3;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				entry = (offset >> 2) & atarimo[0].linkmask.mask;
/*TODO*///				idx = offset & 3;
/*TODO*///			}
/*TODO*///			bank = offset >> (2 + atarimo[0].entrybits);
/*TODO*///			COMBINE_DATA(&atarimo[0].spriteram.read((bank << atarimo[0).entrybits) + entry].data[idx]);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_0_slipram_w: Write handler for the slipram.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarimo_0_slipram_w )
/*TODO*///	{
/*TODO*///		COMBINE_DATA(&atarimo_0_slipram[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		atarimo_1_slipram_w: Write handler for the slipram.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarimo_1_slipram_w )
/*TODO*///	{
/*TODO*///		COMBINE_DATA(&atarimo_1_slipram[offset]);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		mo_process: Internal routine that loops over chunks of
		the playfield with common parameters and processes them
		via a callback.
	---------------------------------------------------------------*/
	
	static void mo_process(atarimo_data mo, mo_callback callback, mame_bitmap param, rectangle clip)
	{
		rectangle finalclip;
		atarimo_cache base = mo.cache;
	
		if (clip != null)
			finalclip = new rectangle(clip);
		else
			finalclip = new rectangle(Machine.visible_area);
	
		/* if the graphics info has changed, recompute */
		if (mo.gfxchanged != 0)
		{
			int i;
	
			mo.gfxchanged = 0;
			for (i = 0; i < round_to_powerof2(mo.gfxmask.mask); i++)
			{
				int idx = mo.gfxlookup.read(i);
				mo.gfxelement[idx] = Machine.gfx[idx];
				mo.gfxelement[idx].colortable = new IntArray(Machine.remapped_colortable, mo.palettebase);
			}
		}
	
		/* create a clipping rectangle so that only partial sections are updated at a time */
		mo.process_clip.min_x = finalclip.min_x;
		mo.process_clip.max_x = finalclip.max_x;
		mo.process_param = param;
		mo.next_xpos = 123456;
	
		/* loop over the list until the end */
                //while (base < mo.curcache)
		while (base.scanline < mo.curcache.scanline)
		{
			atarimo_cache current, first=new atarimo_cache(), last=new atarimo_cache();
			int step;
	
			/* set the upper clip bound and a maximum lower bound */
			mo.process_clip.min_y = base.scanline;
			mo.process_clip.max_y = 100000;
	
			/* import the X and Y scroll values */
			mo.process_xscroll = base.entry.data[0];
			mo.process_yscroll = base.entry.data[1];
			base.scanline++;
	
			/* look for an entry whose scanline start is different from ours; that's our bottom */
			for (current = base; current.scanline < mo.curcache.scanline; current.scanline++)
				if (current.scanline != mo.process_clip.min_y)
				{
					mo.process_clip.max_y = current.scanline;
					break;
				}
	
			/* clip the clipper */
			if (mo.process_clip.min_y < finalclip.min_y)
				mo.process_clip.min_y = finalclip.min_y;
			if (mo.process_clip.max_y > finalclip.max_y)
				mo.process_clip.max_y = finalclip.max_y;
	
			/* set the start and end points */
			if (mo.reverse != 0)
			{
				first.scanline = current.scanline - 1;
				last.scanline = base.scanline - 1;
				step = -1;
			}
			else
			{
				first = base;
				last = current;
				step = 1;
			}
	
			/* update the base */
			base = current;
	
			/* render the mos */
			for (current.scanline = first.scanline; current.scanline != last.scanline; current.scanline += step)
				callback.handler(mo, current.entry);
		}
                
                mo.cache = base;
	}
	
	
	/*---------------------------------------------------------------
		mo_update: Parses the current motion object list, caching
		all entries.
	---------------------------------------------------------------*/
	
	static void mo_update(atarimo_data mo, int scanline)
	{
		atarimo_cache current = mo.curcache;
		atarimo_cache previous = mo.prevcache;
		atarimo_cache new_previous = current;
		int[] spritevisit = new int[ATARIMO_MAXPERBANK];
		int match = 0, link;
	
		/* skip if the scanline is past the bottom of the screen */
		if (scanline > Machine.visible_area.max_y)
			return;
	
		/* if we don't use SLIPs, just recapture from 0 */
		if (mo.slipshift==0)
			link = 0;
	
		/* otherwise, grab the SLIP */
		else
		{
			int slipentry = ((scanline + mo.yscroll) & mo.bitmapymask) >> mo.slipshift;
			link = ((mo.slipram)[slipentry] >> mo.linkmask.shift) & mo.linkmask.mask;
		}
	
		/* if the last list entries were on the same scanline, overwrite them */
		if (previous != null)
		{
			if (previous.scanline == scanline)
				current = new_previous = previous;
			else
				match = 1;
		}
	
		/* set up the first entry with scroll and banking information */
		current.scanline = scanline;
		current.entry.data[0] = mo.xscroll;
		current.entry.data[1] = mo.yscroll;
	
		/* look for a match with the previous entry */
		if (match != 0)
		{
			if (previous.entry.data[0] != current.entry.data[0] ||
				previous.entry.data[1] != current.entry.data[1])
				match = 0;
			previous.scanline++;
		}
		current.scanline++;
	
		/* visit all the sprites and copy their data into the display list */
		memset(spritevisit, 0, mo.entrycount);
		while (spritevisit[link] == 0)
		{
			atarimo_entry modata = mo.spriteram[link + (mo.bank << mo.entrybits)];
	
			/* bounds checking */
			if (current.scanline >= mo.endcache.scanline)
			{
				logerror("Motion object list exceeded maximum\n");
				break;
			}
	
			/* start with the scanline */
			current.scanline = scanline;
			current.entry = modata;
	
			/* update our match status */
			if (match != 0)
			{
				if (previous.entry.data[0] != current.entry.data[0] ||
					previous.entry.data[1] != current.entry.data[1] ||
					previous.entry.data[2] != current.entry.data[2] ||
					previous.entry.data[3] != current.entry.data[3])
					match = 0;
				previous.scanline++;
			}
			current.scanline++;
	
			/* link to the next object */
			spritevisit[link] = 1;
			if (mo.linked != 0)
				link = EXTRACT_DATA(modata, mo.linkmask);
			else
				link = (link + 1) & mo.linkmask.mask;
		}
	
		/* if we didn't match the last set of entries, update the counters */
		if (match == 0)
		{
			mo.prevcache = new_previous;
			mo.curcache = current;
		}
	}
	
	
	/*---------------------------------------------------------------
		mo_render_callback: Internal processing callback that
		renders to the backing bitmap and then copies the result
		to the destination.
	---------------------------------------------------------------*/
	
	static mo_callback mo_render_callback = new mo_callback() {
            public void handler(atarimo_data mo, atarimo_entry entry) {            
/*TODO*///		int gfxindex = mo.gfxlookup[EXTRACT_DATA(entry, mo.gfxmask)];
/*TODO*///		const struct GfxElement *gfx = &mo.gfxelement[gfxindex];
/*TODO*///		const unsigned int *usage = gfx.pen_usage;
/*TODO*///		struct mame_bitmap *bitmap = mo.process_param;
/*TODO*///		struct ataripf_overrender_data overrender_data;
/*TODO*///		UINT32 total_usage = 0;
/*TODO*///		int x, y, sx, sy;
/*TODO*///	
/*TODO*///		/* extract data from the various words */
/*TODO*///		int code = mo.codelookup[EXTRACT_DATA(entry, mo.codemask)] | (EXTRACT_DATA(entry, mo.codehighmask) << mo.codehighshift);
/*TODO*///		int color = mo.colorlookup[EXTRACT_DATA(entry, mo.colormask)];
/*TODO*///		int xpos = EXTRACT_DATA(entry, mo.xposmask);
/*TODO*///		int ypos = -EXTRACT_DATA(entry, mo.yposmask);
/*TODO*///		int hflip = EXTRACT_DATA(entry, mo.hflipmask);
/*TODO*///		int vflip = EXTRACT_DATA(entry, mo.vflipmask);
/*TODO*///		int width = EXTRACT_DATA(entry, mo.widthmask) + 1;
/*TODO*///		int height = EXTRACT_DATA(entry, mo.heightmask) + 1;
/*TODO*///		int xadv, yadv;
/*TODO*///	
/*TODO*///		/* is this one to ignore? */
/*TODO*///		if (mo.ignoremask.mask != 0 && EXTRACT_DATA(entry, mo.ignoremask) == mo.ignorevalue)
/*TODO*///		{
/*TODO*///			if (mo.ignorecb)
/*TODO*///				(*mo.ignorecb)(bitmap, &mo.process_clip, code, color, xpos, ypos);
/*TODO*///			return;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* add in the scroll positions if we're not in absolute coordinates */
/*TODO*///		if (!EXTRACT_DATA(entry, mo.absolutemask))
/*TODO*///		{
/*TODO*///			xpos -= mo.process_xscroll;
/*TODO*///			ypos -= mo.process_yscroll;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* adjust for height */
/*TODO*///		ypos -= height << mo.tileyshift;
/*TODO*///	
/*TODO*///		/* handle previous hold bits */
/*TODO*///		if (mo.next_xpos != 123456)
/*TODO*///			xpos = mo.next_xpos;
/*TODO*///		mo.next_xpos = 123456;
/*TODO*///	
/*TODO*///		/* check for the hold bit */
/*TODO*///		if (EXTRACT_DATA(entry, mo.neighbormask))
/*TODO*///		{
/*TODO*///			if (!mo.nextneighbor)
/*TODO*///				xpos = mo.last_xpos + gfx.width;
/*TODO*///			else
/*TODO*///				mo.next_xpos = xpos + gfx.width;
/*TODO*///		}
/*TODO*///		mo.last_xpos = xpos;
/*TODO*///	
/*TODO*///		/* adjust the final coordinates */
/*TODO*///		xpos &= mo.bitmapxmask;
/*TODO*///		ypos &= mo.bitmapymask;
/*TODO*///		if (xpos > Machine.visible_area.max_x) xpos -= mo.bitmapwidth;
/*TODO*///		if (ypos > Machine.visible_area.max_y) ypos -= mo.bitmapheight;
/*TODO*///	
/*TODO*///		/* compute the overrendering clip rect */
/*TODO*///		overrender_data.clip.min_x = xpos;
/*TODO*///		overrender_data.clip.min_y = ypos;
/*TODO*///		overrender_data.clip.max_x = xpos + width * gfx.width - 1;
/*TODO*///		overrender_data.clip.max_y = ypos + height * gfx.height - 1;
/*TODO*///	
/*TODO*///		/* adjust for h flip */
/*TODO*///		xadv = gfx.width;
/*TODO*///		if (hflip)
/*TODO*///		{
/*TODO*///			xpos += (width - 1) << mo.tilexshift;
/*TODO*///			xadv = -xadv;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* adjust for v flip */
/*TODO*///		yadv = gfx.height;
/*TODO*///		if (vflip)
/*TODO*///		{
/*TODO*///			ypos += (height - 1) << mo.tileyshift;
/*TODO*///			yadv = -yadv;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* standard order is: loop over Y first, then X */
/*TODO*///		if (!mo.swapxy)
/*TODO*///		{
/*TODO*///			/* loop over the height */
/*TODO*///			for (y = 0, sy = ypos; y < height; y++, sy += yadv)
/*TODO*///			{
/*TODO*///				/* clip the Y coordinate */
/*TODO*///				if (sy <= mo.process_clip.min_y - gfx.height)
/*TODO*///				{
/*TODO*///					code += width;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else if (sy > mo.process_clip.max_y)
/*TODO*///					break;
/*TODO*///	
/*TODO*///				/* loop over the width */
/*TODO*///				for (x = 0, sx = xpos; x < width; x++, sx += xadv, code++)
/*TODO*///				{
/*TODO*///					/* clip the X coordinate */
/*TODO*///					if (sx <= -mo.process_clip.min_x - gfx.width || sx > mo.process_clip.max_x)
/*TODO*///						continue;
/*TODO*///	
/*TODO*///					/* draw the sprite */
/*TODO*///					drawgfx(bitmap, gfx, code, color, hflip, vflip, sx, sy, &mo.process_clip, TRANSPARENCY_PEN, mo.transpen);
/*TODO*///	
/*TODO*///					/* also draw the raw version to the priority bitmap */
/*TODO*///					if (mo.overrender0 || mo.overrender1)
/*TODO*///						drawgfx(priority_bitmap, gfx, code, 0, hflip, vflip, sx, sy, &mo.process_clip, TRANSPARENCY_NONE_RAW, mo.transpen);
/*TODO*///	
/*TODO*///					/* track the total usage */
/*TODO*///					total_usage |= usage[code];
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* alternative order is swapped */
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* loop over the width */
/*TODO*///			for (x = 0, sx = xpos; x < width; x++, sx += xadv)
/*TODO*///			{
/*TODO*///				/* clip the X coordinate */
/*TODO*///				if (sx <= mo.process_clip.min_x - gfx.width)
/*TODO*///				{
/*TODO*///					code += height;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else if (sx > mo.process_clip.max_x)
/*TODO*///					break;
/*TODO*///	
/*TODO*///				/* loop over the height */
/*TODO*///				for (y = 0, sy = ypos; y < height; y++, sy += yadv, code++)
/*TODO*///				{
/*TODO*///					/* clip the X coordinate */
/*TODO*///					if (sy <= -mo.process_clip.min_y - gfx.height || sy > mo.process_clip.max_y)
/*TODO*///						continue;
/*TODO*///	
/*TODO*///					/* draw the sprite */
/*TODO*///					drawgfx(bitmap, gfx, code, color, hflip, vflip, sx, sy, &mo.process_clip, TRANSPARENCY_PEN, mo.transpen);
/*TODO*///	
/*TODO*///					/* also draw the raw version to the priority bitmap */
/*TODO*///					if (mo.overrender0 || mo.overrender1)
/*TODO*///						drawgfx(priority_bitmap, gfx, code, 0, hflip, vflip, sx, sy, &mo.process_clip, TRANSPARENCY_NONE_RAW, mo.transpen);
/*TODO*///	
/*TODO*///					/* track the total usage */
/*TODO*///					total_usage |= usage[code];
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* handle overrendering */
/*TODO*///		if (mo.overrender0 || mo.overrender1)
/*TODO*///		{
/*TODO*///			/* clip to the display */
/*TODO*///			if (overrender_data.clip.min_x < mo.process_clip.min_x)
/*TODO*///				overrender_data.clip.min_x = mo.process_clip.min_x;
/*TODO*///			else if (overrender_data.clip.min_x > mo.process_clip.max_x)
/*TODO*///				overrender_data.clip.min_x = mo.process_clip.max_x;
/*TODO*///			if (overrender_data.clip.max_x < mo.process_clip.min_x)
/*TODO*///				overrender_data.clip.max_x = mo.process_clip.min_x;
/*TODO*///			else if (overrender_data.clip.max_x > mo.process_clip.max_x)
/*TODO*///				overrender_data.clip.max_x = mo.process_clip.max_x;
/*TODO*///			if (overrender_data.clip.min_y < mo.process_clip.min_y)
/*TODO*///				overrender_data.clip.min_y = mo.process_clip.min_y;
/*TODO*///			else if (overrender_data.clip.min_y > mo.process_clip.max_y)
/*TODO*///				overrender_data.clip.min_y = mo.process_clip.max_y;
/*TODO*///			if (overrender_data.clip.max_y < mo.process_clip.min_y)
/*TODO*///				overrender_data.clip.max_y = mo.process_clip.min_y;
/*TODO*///			else if (overrender_data.clip.max_y > mo.process_clip.max_y)
/*TODO*///				overrender_data.clip.max_y = mo.process_clip.max_y;
/*TODO*///	
/*TODO*///			/* overrender the playfield */
/*TODO*///			overrender_data.bitmap = bitmap;
/*TODO*///			overrender_data.mousage = total_usage;
/*TODO*///			overrender_data.mocolor = color;
/*TODO*///			overrender_data.mopriority = EXTRACT_DATA(entry, mo.prioritymask);
/*TODO*///			if (mo.overrender0)
/*TODO*///				ataripf_overrender(0, mo.overrender0, &overrender_data);
/*TODO*///			if (mo.overrender1)
/*TODO*///				ataripf_overrender(1, mo.overrender1, &overrender_data);
/*TODO*///		}
            }
    };
	
	
	/*---------------------------------------------------------------
		mo_scanline_callback: This callback is called on SLIP
		boundaries to update the current set of motion objects.
	---------------------------------------------------------------*/
	
	static timer_callback mo_scanline_callback = new timer_callback() {
            public void handler(int param) {
                atarimo_data mo = atarimo[param >> 16];
		int scanline = param & 0xffff;
		int nextscanline = scanline + mo.updatescans;
	
		/* if this is scanline 0, reset things */
		/* also, adjust where we will next break */
		if (scanline == 0)
		{
			mo.curcache = mo.cache;
			mo.prevcache = null;
		}
	
		/* do the update */
		mo_update(mo, scanline);
	
		/* don't bother updating in the VBLANK area, just start back at 0 */
		if (nextscanline > Machine.visible_area.max_y)
			nextscanline = 0;
		timer_set(cpu_getscanlinetime(nextscanline), nextscanline | (param & ~0xffff), mo_scanline_callback);
                
                atarimo[param >> 16] = mo;
            }
        };
	
}
