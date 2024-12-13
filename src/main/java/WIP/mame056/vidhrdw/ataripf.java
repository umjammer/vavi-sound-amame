/*##########################################################################

	ataripf.c

	Common playfield management functions for Atari raster games.

##########################################################################*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.ataripfH.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.drawgfxH.*;
import static mame056.mame.Machine;
import static mame056.mameH.*;
import static mame056.memoryH.*;
import static mame056.timer.*;
import static mame056.timerH.*;

public class ataripf
{
	
	
	
	/*##########################################################################
		TYPES & STRUCTURES
	##########################################################################*/
	
	/* internal state structure containing values that can change scanline-by-scanline */
	public static class ataripf_state
	{
		int					scanline;			/* scanline where we are valid */
		int					xscroll;			/* xscroll value */
		int					yscroll;			/* yscroll value */
		int					bankbits;			/* bank bits */
	};
	
	
	/* internal variant of the gfxelement that contains extra data */
	public static class ataripf_gfxelement
	{
		GfxElement		element;
		int						initialized;
		int						colorshift;
	};
	
        public static abstract interface pf_render {
            public abstract mame_bitmap handler();
        }
	
	/* internal structure containing the state of a playfield */
	public static class ataripf_data
	{
		int					initialized;		/* true if we're initialized */
		int					timerallocated;		/* true if we've allocated the timer */
		int					gfxchanged;			/* true if the gfx info has changed */

/*TODO*///		int					colshift;			/* bits to shift X coordinate when looking up in VRAM */
/*TODO*///		int 				rowshift;			/* bits to shift Y coordinate when looking up in VRAM */
		int					colmask;			/* mask to use when wrapping X coordinate in VRAM */
		int                                     rowmask;			/* mask to use when wrapping Y coordinate in VRAM */
/*TODO*///		int					vrammask;			/* combined mask when accessing VRAM with raw addresses */
/*TODO*///		int					vramsize;			/* total size of VRAM, in entries */

		int					tilexshift;			/* bits to shift X coordinate when drawing */
		int					tileyshift;			/* bits to shift Y coordinate when drawing */
		int					tilewidth;			/* width of a single tile */
		int					tileheight;			/* height of a single tile */
/*TODO*///		int					bitmapwidth;		/* width of the full playfield bitmap */
/*TODO*///		int					bitmapheight;		/* height of the full playfield bitmap */
/*TODO*///		int					bitmapxmask;		/* x coordinate mask for the playfield bitmap */
/*TODO*///		int					bitmapymask;		/* y coordinate mask for the playfield bitmap */
	
		int					palettebase;		/* base palette entry */
		int					maxcolors;			/* maximum number of colors */
/*TODO*///		int					shadowxor;			/* color XOR for shadow effect (if any) */
/*TODO*///		int				transpens;			/* transparent pen */
/*TODO*///		int					transpen;			/* transparent pen */
/*TODO*///	
		int					lookupmask;			/* mask for the lookup table */
/*TODO*///	
/*TODO*///		int					latchval;			/* value for latching */
/*TODO*///		int					latchdata;			/* shifted value for latching */
/*TODO*///		int					latchmask;			/* mask for latching */

		mame_bitmap	bitmap;				/* backing bitmap */
		IntArray			vram;				/* pointer to VRAM */
		IntArray			dirtymap;			/* dirty bitmap */
		UBytePtr				visitmap;			/* visiting bitmap */
		IntArray			lookup;				/* pointer to lookup table */

		ataripf_state curstate;			/* current state */
		ataripf_state[] statelist;		/* list of changed states */
		int					stateindex;			/* index of the next state */

		rectangle	process_clip;		/* (during processing) the clip rectangle */
		rectangle	process_tiles;		/* (during processing) the tiles rectangle */
		mame_bitmap				process_param;		/* (during processing) the callback parameter */

		ataripf_gfxelement[] gfxelement = new ataripf_gfxelement[MAX_GFX_ELEMENTS]; /* graphics element copies */
	};
	
	
	/* callback function for the internal playfield processing mechanism */
        public static abstract interface pf_callback {
            public abstract void handler(ataripf_data pf, ataripf_state state);
        }
	
	
/*TODO*///	/*##########################################################################
/*TODO*///		MACROS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/* verification macro for void functions */
/*TODO*///	#define VERIFY(cond, msg) if (!(cond)) { logerror(msg); return; }
/*TODO*///	
/*TODO*///	/* verification macro for non-void functions */
/*TODO*///	#define VERIFYRETFREE(cond, msg, ret) if (!(cond)) { logerror(msg); ataripf_free(); return (ret); }
/*TODO*///	
/*TODO*///	
/*TODO*///	/* accessors for upper/lower halves of a 32-bit value */
/*TODO*///	#if LSB_FIRST
/*TODO*///	#define LOWER_HALF(x) ((data16_t *)&(x))[0]
/*TODO*///	#define UPPER_HALF(x) ((data16_t *)&(x))[1]
/*TODO*///	#else
        public static int LOWER_HALF(int x){
            //((data16_t *)&(x))[1];
            return (x&0x0000f0)>>1;
        }
/*TODO*///	#define UPPER_HALF(x) ((data16_t *)&(x))[0]
/*TODO*///	#endif
	
	
	
	/*##########################################################################
		GLOBAL VARIABLES
	##########################################################################*/
	
	public static IntArray ataripf_0_base;
/*TODO*///	data16_t *ataripf_0_upper;
/*TODO*///	
/*TODO*///	data16_t *ataripf_1_base;
/*TODO*///	
/*TODO*///	data32_t *ataripf_0_base32;
	
	
	
	/*##########################################################################
		STATIC VARIABLES
	##########################################################################*/
	
	static ataripf_data[] ataripf = new ataripf_data[ATARIPF_MAX];

/*TODO*///	static ataripf_overrender_cb overrender_callback;
/*TODO*///	static struct ataripf_overrender_data overrender_data;
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		STATIC FUNCTION DECLARATIONS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	static void pf_process(struct ataripf_data *pf, pf_callback callback, void *param, const struct rectangle *clip);
/*TODO*///	static void pf_render_callback(struct ataripf_data *pf, const struct ataripf_state *state);
/*TODO*///	static void pf_overrender_callback(struct ataripf_data *pf, const struct ataripf_state *state);
/*TODO*///	static void pf_eof_callback(int map);
/*TODO*///	static void pf_init_gfx(struct ataripf_data *pf, int gfxindex);
	
	
	
	/*##########################################################################
		INLINE FUNCTIONS
	##########################################################################*/
	
	/*---------------------------------------------------------------
		compute_log: Computes the number of bits necessary to
		hold a given value. The input must be an even power of
		two.
	---------------------------------------------------------------*/
	
	public static int compute_log(int value)
	{
		int log = 0;
	
		if (value == 0)
			return -1;
		while ((value & 1)==0){
			log++; value >>= 1;
                }
		if (value != 1)
			return -1;
		return log;
	}
	
	
	/*---------------------------------------------------------------
		round_to_powerof2: Rounds a number up to the nearest
		power of 2. Even powers of 2 are rounded up to the
		next greatest power (e.g., 4 returns 8).
	---------------------------------------------------------------*/
	
	public static int round_to_powerof2(int value)
	{
		int log = 0;
	
		if (value == 0)
			return 1;
		while ((value >>= 1) != 0)
			log++;
		return 1 << (log + 1);
	}
	
	
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
	
	
	/*---------------------------------------------------------------
		pf_update_state: Internal routine that updates the
		state list of the playfield with the current parameters.
	---------------------------------------------------------------*/
	
	public static void pf_update_state(ataripf_data pf, int scanline)
	{
		ataripf_state state = pf.statelist[pf.stateindex];
	
		/* ignore anything after the bottom of the visible screen */
		if (scanline > Machine.visible_area.max_y)
			return;
	
		/* ignore anything earlier than the last scanline we entered */
		if (pf.statelist[pf.stateindex-1].scanline > scanline)
		{
			//logerror("pf_update_state: Attempted state update on prior scanline (%d vs. %d)\n", scanline, state[-1]);
			return;
		}
	
		/* if this is the same scanline as last time, overwrite it */
		else if (pf.statelist[pf.stateindex-1].scanline == scanline)
		{
			//logerror("pf_update_state: scanlines equal, overwriting\n");
			//state--;
                        pf.stateindex--;
		}
	
		/* otherwise, move forward one entry */
		else
		{
			//logerror("pf_update_state: new entry\n");
			pf.stateindex++;
		}
	
		/* fill in the data */
		state = pf.curstate;
		state.scanline = scanline;
                
                pf.statelist[pf.stateindex] = state;
	}
	
	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		GLOBAL FUNCTIONS
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_blend_gfx: Takes two GFXElements and blends their
/*TODO*///		data together to form one. Then frees the second.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void ataripf_blend_gfx(int gfx0, int gfx1, int mask0, int mask1)
/*TODO*///	{
/*TODO*///		struct GfxElement *gx0 = Machine->gfx[gfx0];
/*TODO*///		struct GfxElement *gx1 = Machine->gfx[gfx1];
/*TODO*///		int c, x, y;
/*TODO*///	
/*TODO*///		/* loop over elements */
/*TODO*///		for (c = 0; c < gx0->total_elements; c++)
/*TODO*///		{
/*TODO*///			UINT8 *c0base = gx0->gfxdata + gx0->char_modulo * c;
/*TODO*///			UINT8 *c1base = gx1->gfxdata + gx1->char_modulo * c;
/*TODO*///			UINT32 usage = 0;
/*TODO*///	
/*TODO*///			/* loop over height */
/*TODO*///			for (y = 0; y < gx0->height; y++)
/*TODO*///			{
/*TODO*///				UINT8 *c0 = c0base, *c1 = c1base;
/*TODO*///	
/*TODO*///				for (x = 0; x < gx0->width; x++, c0++, c1++)
/*TODO*///				{
/*TODO*///					*c0 = (*c0 & mask0) | (*c1 & mask1);
/*TODO*///					usage |= 1 << *c0;
/*TODO*///				}
/*TODO*///				c0base += gx0->line_modulo;
/*TODO*///				c1base += gx1->line_modulo;
/*TODO*///				if (gx0->pen_usage)
/*TODO*///					gx0->pen_usage[c] = usage;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* free the second graphics element */
/*TODO*///		freegfx(gx1);
/*TODO*///		Machine->gfx[gfx1] = NULL;
/*TODO*///	}
/*TODO*///	
	
	/*---------------------------------------------------------------
		ataripf_init: Configures the playfield using the input
		description. Allocates all memory necessary and generates
		the attribute lookup table. If custom_lookup is provided,
		it is used in place of the generated attribute table.
	---------------------------------------------------------------*/
	
	public static int ataripf_init(int map, ataripf_desc desc)
	{
/*TODO*///		int lookupcount = round_to_powerof2(desc->tilemask | desc->colormask | desc->hflipmask | desc->vflipmask | desc->prioritymask) >> ATARIPF_LOOKUP_DATABITS;
/*TODO*///		struct GfxElement *gfx = Machine->gfx[desc->gfxindex];
/*TODO*///		struct ataripf_data *pf = &ataripf[map];
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* sanity checks */
/*TODO*///		VERIFYRETFREE(map >= 0 && map < ATARIPF_MAX, "ataripf_init: map out of range", 0)
/*TODO*///		VERIFYRETFREE(compute_log(desc->cols) != -1, "ataripf_init: cols must be power of 2", 0)
/*TODO*///		VERIFYRETFREE(compute_log(desc->rows) != -1, "ataripf_init: rows must be power of 2", 0)
/*TODO*///		VERIFYRETFREE(compute_log(desc->xmult) != -1, "ataripf_init: xmult must be power of 2", 0)
/*TODO*///		VERIFYRETFREE(compute_log(desc->ymult) != -1, "ataripf_init: ymult must be power of 2", 0)
/*TODO*///		VERIFYRETFREE((desc->tilemask & ATARIPF_LOOKUP_DATAMASK) == ATARIPF_LOOKUP_DATAMASK, "ataripf_init: low bits of tilemask must be 0xff", 0)
/*TODO*///	
/*TODO*///		/* copy in the basic data */
/*TODO*///		pf->initialized  = 0;
/*TODO*///		pf->timerallocated = 0;
/*TODO*///		pf->gfxchanged   = 0;
/*TODO*///	
/*TODO*///		pf->colshift     = compute_log(desc->xmult);
/*TODO*///		pf->rowshift     = compute_log(desc->ymult);
/*TODO*///		pf->colmask      = desc->cols - 1;
/*TODO*///		pf->rowmask      = desc->rows - 1;
/*TODO*///		pf->vrammask     = (pf->colmask << pf->colshift) | (pf->rowmask << pf->rowshift);
/*TODO*///		pf->vramsize     = round_to_powerof2(pf->vrammask);
/*TODO*///	
/*TODO*///		pf->tilexshift   = compute_log(gfx->width);
/*TODO*///		pf->tileyshift   = compute_log(gfx->height);
/*TODO*///		pf->tilewidth    = gfx->width;
/*TODO*///		pf->tileheight   = gfx->height;
/*TODO*///		pf->bitmapwidth  = desc->cols * gfx->width;
/*TODO*///		pf->bitmapheight = desc->rows * gfx->height;
/*TODO*///		pf->bitmapxmask  = pf->bitmapwidth - 1;
/*TODO*///		pf->bitmapymask  = pf->bitmapheight - 1;
/*TODO*///	
/*TODO*///		pf->palettebase  = desc->palettebase;
/*TODO*///		pf->maxcolors    = desc->maxcolors / ATARIPF_BASE_GRANULARITY;
/*TODO*///		pf->shadowxor    = desc->shadowxor;
/*TODO*///		pf->transpens    = desc->transpens;
/*TODO*///		pf->transpen     = desc->transpens ? compute_log(desc->transpens) : -1;
/*TODO*///	
/*TODO*///		pf->lookupmask   = lookupcount - 1;
/*TODO*///	
/*TODO*///		pf->latchval     = 0;
/*TODO*///		pf->latchdata    = -1;
/*TODO*///		pf->latchmask    = desc->latchmask;
/*TODO*///	
/*TODO*///		/* allocate the backing bitmap */
/*TODO*///		pf->bitmap = bitmap_alloc(pf->bitmapwidth, pf->bitmapheight);
/*TODO*///		VERIFYRETFREE(pf->bitmap, "ataripf_init: out of memory for bitmap", 0)
/*TODO*///	
/*TODO*///		/* allocate the vram */
/*TODO*///		pf->vram = malloc(sizeof(pf->vram[0]) * pf->vramsize);
/*TODO*///		VERIFYRETFREE(pf->vram, "ataripf_init: out of memory for vram", 0)
/*TODO*///	
/*TODO*///		/* clear it to zero */
/*TODO*///		memset(pf->vram, 0, sizeof(pf->vram[0]) * pf->vramsize);
/*TODO*///	
/*TODO*///		/* allocate the dirty map */
/*TODO*///		pf->dirtymap = malloc(sizeof(pf->dirtymap[0]) * pf->vramsize);
/*TODO*///		VERIFYRETFREE(pf->dirtymap, "ataripf_init: out of memory for dirtymap", 0)
/*TODO*///	
/*TODO*///		/* mark everything dirty */
/*TODO*///		memset(pf->dirtymap, -1, sizeof(pf->dirtymap[0]) * pf->vramsize);
/*TODO*///	
/*TODO*///		/* allocate the visitation map */
/*TODO*///		pf->visitmap = malloc(sizeof(pf->visitmap[0]) * pf->vramsize);
/*TODO*///		VERIFYRETFREE(pf->visitmap, "ataripf_init: out of memory for visitmap", 0)
/*TODO*///	
/*TODO*///		/* mark everything non-visited */
/*TODO*///		memset(pf->visitmap, 0, sizeof(pf->visitmap[0]) * pf->vramsize);
/*TODO*///	
/*TODO*///		/* allocate the attribute lookup */
/*TODO*///		pf->lookup = malloc(lookupcount * sizeof(pf->lookup[0]));
/*TODO*///		VERIFYRETFREE(pf->lookup, "ataripf_init: out of memory for lookup", 0)
/*TODO*///	
/*TODO*///		/* fill in the attribute lookup */
/*TODO*///		for (i = 0; i < lookupcount; i++)
/*TODO*///		{
/*TODO*///			int value    = (i << ATARIPF_LOOKUP_DATABITS);
/*TODO*///			int tile     = collapse_bits(value, desc->tilemask);
/*TODO*///			int color    = collapse_bits(value, desc->colormask);
/*TODO*///			int hflip    = collapse_bits(value, desc->hflipmask);
/*TODO*///			int vflip    = collapse_bits(value, desc->vflipmask);
/*TODO*///			int priority = collapse_bits(value, desc->prioritymask);
/*TODO*///	
/*TODO*///			pf->lookup[i] = ATARIPF_LOOKUP_ENTRY(desc->gfxindex, tile, color, hflip, vflip, priority);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* compute the extended usage map */
/*TODO*///		pf_init_gfx(pf, desc->gfxindex);
/*TODO*///	
/*TODO*///		/* allocate the state list */
/*TODO*///		pf->statelist = malloc(pf->bitmapheight * sizeof(pf->statelist[0]));
/*TODO*///		VERIFYRETFREE(pf->statelist, "ataripf_init: out of memory for extra state list", 0)
/*TODO*///	
/*TODO*///		/* reset the state list */
/*TODO*///		memset(&pf->curstate, 0, sizeof(pf->curstate));
/*TODO*///		pf->statelist[0] = pf->curstate;
/*TODO*///		pf->stateindex = 1;
/*TODO*///	
/*TODO*///		pf->initialized = 1;
/*TODO*///	
/*TODO*///		logerror("ataripf_init:\n");
/*TODO*///		logerror("  width=%d (shift=%d),  height=%d (shift=%d)\n", gfx->width, pf->tilexshift, gfx->height, pf->tileyshift);
/*TODO*///		logerror("  cols=%d  (mask=%X),   rows=%d   (mask=%X)\n", desc->cols, pf->colmask, desc->rows, pf->rowmask);
/*TODO*///		logerror("  xmult=%d (shift=%d),  ymult=%d  (shift=%d)\n", desc->xmult, pf->colshift, desc->ymult, pf->rowshift);
/*TODO*///		logerror("  VRAM mask=%X,  dirtymap size=%d\n", pf->vrammask, pf->vramsize);
/*TODO*///		logerror("  bitmap size=%dx%d\n", pf->bitmapwidth, pf->bitmapheight);
	
		return 1;
	}
	
	
	/*---------------------------------------------------------------
		ataripf_free: Frees any memory allocated for any playfield.
	---------------------------------------------------------------*/
	
	public static void ataripf_free()
	{
		int i;
	
		/* free the playfield data */
		for (i = 0; i < ATARIPF_MAX; i++)
		{
			//struct ataripf_data *pf = &ataripf[i];
	
			/* free the backing bitmap */
			if (ataripf[i].bitmap != null)
                            ataripf[i].bitmap = null;
	
			/* free the vram */
			if (ataripf[i].vram != null)
                            ataripf[i].vram = null;
	
			/* free the dirty map */
			if (ataripf[i].dirtymap != null)
                            ataripf[i].dirtymap = null;
	
			/* free the visitation map */
			if (ataripf[i].visitmap != null)
                            ataripf[i].visitmap = null;
	
			/* free the attribute lookup */
			if (ataripf[i].lookup != null)
                            ataripf[i].lookup = null;
	
			/* free the state list */
			if (ataripf[i].statelist != null)
                            ataripf[i].statelist = null;
	
			/* free the extended usage maps */
			for (i = 0; i < MAX_GFX_ELEMENTS; i++)
                            ataripf[i].gfxelement[i].initialized = 0;
	
			ataripf[i].initialized = 0;
		}
	}
	
	
	/*---------------------------------------------------------------
		ataripf_get_lookup: Fetches the lookup table so it can
		be modified.
	---------------------------------------------------------------*/
	
	public static IntArray ataripf_get_lookup(int map, int[] size)
	{
		ataripf[map].gfxchanged = 1;
		if (size[0] != 0)
			size[0] = round_to_powerof2(ataripf[map].lookupmask);
		return ataripf[map].lookup;
	}
	
	
	/*---------------------------------------------------------------
		ataripf_render: Render the playfield, updating any dirty
		blocks, and copy it to the destination bitmap.
	---------------------------------------------------------------*/
	
	public static void ataripf_render(int map, mame_bitmap bitmap)
	{
		//struct ataripf_data *pf = &ataripf[map];
	
		if (ataripf[map].initialized != 0)
		{
			/* render via the standard render callback */
			pf_process(ataripf[map], pf_render_callback, bitmap, null);
	
			/* set a timer to call the eof function just before scanline 0 */
			if (ataripf[map].timerallocated == 0)
			{
				timer_set(cpu_getscanlinetime(0), map, pf_eof_callback);
				ataripf[map].timerallocated = 1;
			}
		}
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_overrender: Overrender the playfield, calling
/*TODO*///		the callback for each tile before proceeding.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void ataripf_overrender(int map, ataripf_overrender_cb callback, struct ataripf_overrender_data *data)
/*TODO*///	{
/*TODO*///		struct ataripf_data *pf = &ataripf[map];
/*TODO*///	
/*TODO*///		if (pf->initialized)
/*TODO*///		{
/*TODO*///			/* set the globals before processing */
/*TODO*///			overrender_callback = callback;
/*TODO*///			overrender_data = *data;
/*TODO*///	
/*TODO*///			/* render via the standard render callback */
/*TODO*///			pf_process(pf, pf_overrender_callback, data->bitmap, &data->clip);
/*TODO*///		}
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		ataripf_set_bankbits: Set the extra banking bits for a
		playfield.
	---------------------------------------------------------------*/
	
	public static void ataripf_set_bankbits(int map, int bankbits, int scanline)
	{
		//ataripf_data pf = ataripf[map];
	
		if (ataripf[map].initialized!=0 && ataripf[map].curstate.bankbits != bankbits)
		{
			ataripf[map].curstate.bankbits = bankbits;
			pf_update_state(ataripf[map], scanline);
		}
	}
	
	
	/*---------------------------------------------------------------
		ataripf_set_xscroll: Set the horizontal scroll value for
		a playfield.
	---------------------------------------------------------------*/
	
	public static void ataripf_set_xscroll(int map, int xscroll, int scanline)
	{
		ataripf_data pf = ataripf[map];
		if (ataripf[map].initialized!=0 && ataripf[map].curstate.xscroll != xscroll)
		{
			ataripf[map].curstate.xscroll = xscroll;
			pf_update_state(pf, scanline);
		}
	}
	
	
	/*---------------------------------------------------------------
		ataripf_set_yscroll: Set the vertical scroll value for
		a playfield.
	---------------------------------------------------------------*/
	
	public static void ataripf_set_yscroll(int map, int yscroll, int scanline)
	{
		//struct ataripf_data *pf = &ataripf[map];
		if (ataripf[map].initialized!=0 && ataripf[map].curstate.yscroll != yscroll)
		{
			ataripf[map].curstate.yscroll = yscroll;
			pf_update_state(ataripf[map], scanline);
		}
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_set_latch: Set the upper word latch value and mask
/*TODO*///		a playfield.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void ataripf_set_latch(int map, int latch)
/*TODO*///	{
/*TODO*///		struct ataripf_data *pf = &ataripf[map];
/*TODO*///		int mask;
/*TODO*///	
/*TODO*///		if (pf->initialized)
/*TODO*///		{
/*TODO*///			/* -1 means disable the latching */
/*TODO*///			if (latch == -1)
/*TODO*///				pf->latchdata = -1;
/*TODO*///			else
/*TODO*///				pf->latchdata = latch & pf->latchmask;
/*TODO*///	
/*TODO*///			/* compute the shifted value */
/*TODO*///			pf->latchval = latch & pf->latchmask;
/*TODO*///			mask = pf->latchmask;
/*TODO*///			if (mask)
/*TODO*///				for ( ; !(mask & 1); mask >>= 1)
/*TODO*///					pf->latchval >>= 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_set_latch_lo: Set the latch for any playfield with
/*TODO*///		a latchmask in the low byte.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void ataripf_set_latch_lo(int latch)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		for (i = 0; i < ATARIPF_MAX; i++)
/*TODO*///			if (ataripf[i].latchmask & 0x00ff)
/*TODO*///				ataripf_set_latch(i, latch);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_set_latch_hi: Set the latch for any playfield with
/*TODO*///		a latchmask in the high byte.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	void ataripf_set_latch_hi(int latch)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		for (i = 0; i < ATARIPF_MAX; i++)
/*TODO*///			if (ataripf[i].latchmask & 0xff00)
/*TODO*///				ataripf_set_latch(i, latch);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_get_bankbits: Returns the extra banking bits for a
/*TODO*///		playfield.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int ataripf_get_bankbits(int map)
/*TODO*///	{
/*TODO*///		return ataripf[map].curstate.bankbits;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		ataripf_get_xscroll: Returns the horizontal scroll value
		for a playfield.
	---------------------------------------------------------------*/
	
	public static int ataripf_get_xscroll(int map)
	{
		return ataripf[map].curstate.xscroll;
	}
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_get_yscroll: Returns the vertical scroll value for
/*TODO*///		a playfield.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	int ataripf_get_yscroll(int map)
/*TODO*///	{
/*TODO*///		return ataripf[map].curstate.yscroll;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_get_vram: Returns a pointer to video RAM.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	UINT32 *ataripf_get_vram(int map)
/*TODO*///	{
/*TODO*///		return ataripf[map].vram;
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		ataripf_0_simple_w: Simple write handler for single-word
		playfields.
	---------------------------------------------------------------*/
	
	public static WriteHandlerPtr16 ataripf_0_simple_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                int oldword = LOWER_HALF(ataripf[0].vram.read(offset));
		int newword = oldword;
	
		COMBINE_DATA(newword, data);
	
		if (oldword != newword)
		{
			//LOWER_HALF(ataripf[0].vram[offset]) = newword;
                        int _part = (newword&0x0000f0);
                        int _var =ataripf[0].vram.read(offset) & 0xff0f;
                        ataripf[0].vram.write(offset, _var | _part);
			ataripf[0].dirtymap.write(offset, -1);
		}
                int _res = ataripf_0_base.read(offset);
		COMBINE_DATA(_res, data);
                ataripf_0_base.write(offset, _res);
            }
        };
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_1_simple_w: Simple write handler for single-word
/*TODO*///		playfields.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_1_simple_w )
/*TODO*///	{
/*TODO*///		int oldword = LOWER_HALF(ataripf[1].vram[offset]);
/*TODO*///		int newword = oldword;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&newword);
/*TODO*///	
/*TODO*///		if (oldword != newword)
/*TODO*///		{
/*TODO*///			LOWER_HALF(ataripf[1].vram[offset]) = newword;
/*TODO*///			ataripf[1].dirtymap[offset] = -1;
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_1_base[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_0_latched_w: Simple write handler for single-word
/*TODO*///		playfields that latches additional bits in the upper word.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_0_latched_w )
/*TODO*///	{
/*TODO*///		int oldword = LOWER_HALF(ataripf[0].vram[offset]);
/*TODO*///		int newword = oldword;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&newword);
/*TODO*///	
/*TODO*///		if (oldword != newword)
/*TODO*///		{
/*TODO*///			LOWER_HALF(ataripf[0].vram[offset]) = newword;
/*TODO*///			ataripf[0].dirtymap[offset] = -1;
/*TODO*///		}
/*TODO*///		if (ataripf[0].latchdata != -1)
/*TODO*///		{
/*TODO*///			UPPER_HALF(ataripf[0].vram[offset]) = ataripf[0].latchval;
/*TODO*///			ataripf_0_upper[offset] = (ataripf_0_upper[offset] & ~ataripf[0].latchmask) | ataripf[0].latchdata;
/*TODO*///			ataripf[0].dirtymap[offset] = -1;
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_0_base[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_1_latched_w: Simple write handler for single-word
/*TODO*///		playfields that latches additional bits in the upper word.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_1_latched_w )
/*TODO*///	{
/*TODO*///		int oldword = LOWER_HALF(ataripf[1].vram[offset]);
/*TODO*///		int newword = oldword;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&newword);
/*TODO*///	
/*TODO*///		if (oldword != newword)
/*TODO*///		{
/*TODO*///			LOWER_HALF(ataripf[1].vram[offset]) = newword;
/*TODO*///			ataripf[1].dirtymap[offset] = -1;
/*TODO*///		}
/*TODO*///		if (ataripf[1].latchdata != -1)
/*TODO*///		{
/*TODO*///			UPPER_HALF(ataripf[1].vram[offset]) = ataripf[1].latchval;
/*TODO*///			ataripf_0_upper[offset] = (ataripf_0_upper[offset] & ~ataripf[1].latchmask) | ataripf[1].latchdata;
/*TODO*///			ataripf[1].dirtymap[offset] = -1;
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_1_base[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_0_upper_msb_w: Simple write handler for the upper
/*TODO*///		word of split two-word playfields, where the MSB contains
/*TODO*///		the significant data.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_0_upper_msb_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_MSB)
/*TODO*///		{
/*TODO*///			int oldword = UPPER_HALF(ataripf[0].vram[offset]);
/*TODO*///			int newword = oldword << 8;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///			newword >>= 8;
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				UPPER_HALF(ataripf[0].vram[offset]) = newword;
/*TODO*///				ataripf[0].dirtymap[offset] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_0_upper[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_0_upper_lsb_w: Simple write handler for the upper
/*TODO*///		word of split two-word playfields, where the LSB contains
/*TODO*///		the significant data.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_0_upper_lsb_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			int oldword = UPPER_HALF(ataripf[0].vram[offset]);
/*TODO*///			int newword = oldword;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///			newword &= 0xff;
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				UPPER_HALF(ataripf[0].vram[offset]) = newword;
/*TODO*///				ataripf[0].dirtymap[offset] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_0_upper[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_0_large_w: Simple write handler for double-word
/*TODO*///		playfields.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_0_large_w )
/*TODO*///	{
/*TODO*///		if (!(offset & 1))
/*TODO*///		{
/*TODO*///			int offs = offset / 2;
/*TODO*///			int oldword = UPPER_HALF(ataripf[0].vram[offs]);
/*TODO*///			int newword = oldword;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				UPPER_HALF(ataripf[0].vram[offs]) = newword;
/*TODO*///				ataripf[0].dirtymap[offs] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			int offs = offset / 2;
/*TODO*///			int oldword = LOWER_HALF(ataripf[0].vram[offs]);
/*TODO*///			int newword = oldword;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				LOWER_HALF(ataripf[0].vram[offs]) = newword;
/*TODO*///				ataripf[0].dirtymap[offs] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_0_base[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_0_split_w: Simple write handler for split playfields.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_0_split_w )
/*TODO*///	{
/*TODO*///		int adjusted = (offset & 0x003f) | ((~offset & 0x1000) >> 6) | ((offset & 0x0fc0) << 1);
/*TODO*///		int oldword = LOWER_HALF(ataripf[0].vram[adjusted]);
/*TODO*///		int newword = oldword;
/*TODO*///	
/*TODO*///		COMBINE_DATA(&newword);
/*TODO*///	
/*TODO*///		if (oldword != newword)
/*TODO*///		{
/*TODO*///			LOWER_HALF(ataripf[0].vram[adjusted]) = newword;
/*TODO*///			ataripf[0].dirtymap[adjusted] = -1;
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_0_base[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_01_upper_lsb_msb_w: Simple write handler for the
/*TODO*///		upper word of dual split two-word playfields, where the LSB
/*TODO*///		contains the significant data for playfield 0 and the MSB
/*TODO*///		contains the significant data for playfield 1.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( ataripf_01_upper_lsb_msb_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			int oldword = UPPER_HALF(ataripf[0].vram[offset]);
/*TODO*///			int newword = oldword;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///			newword &= 0xff;
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				UPPER_HALF(ataripf[0].vram[offset]) = newword;
/*TODO*///				ataripf[0].dirtymap[offset] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		if (ACCESSING_MSB)
/*TODO*///		{
/*TODO*///			int oldword = UPPER_HALF(ataripf[1].vram[offset]);
/*TODO*///			int newword = oldword << 8;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///			newword >>= 8;
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				UPPER_HALF(ataripf[1].vram[offset]) = newword;
/*TODO*///				ataripf[1].dirtymap[offset] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		COMBINE_DATA(&ataripf_0_upper[offset]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		ataripf_0_split32_w: Simple write handler for split playfields.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( ataripf_0_split32_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_MSW32)
/*TODO*///		{
/*TODO*///			int adjusted = ((offset & 0x001f) | ((~offset & 0x0800) >> 6) | ((offset & 0x07e0) << 1)) * 2;
/*TODO*///			int oldword = LOWER_HALF(ataripf[0].vram[adjusted]);
/*TODO*///			int newword = oldword << 16;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///			newword >>= 16;
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				LOWER_HALF(ataripf[0].vram[adjusted]) = newword;
/*TODO*///				ataripf[0].dirtymap[adjusted] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (ACCESSING_LSW32)
/*TODO*///		{
/*TODO*///			int adjusted = ((offset & 0x001f) | ((~offset & 0x0800) >> 6) | ((offset & 0x07e0) << 1)) * 2 + 1;
/*TODO*///			int oldword = LOWER_HALF(ataripf[0].vram[adjusted]);
/*TODO*///			int newword = oldword;
/*TODO*///	
/*TODO*///			COMBINE_DATA(&newword);
/*TODO*///			newword &= 0xffff;
/*TODO*///	
/*TODO*///			if (oldword != newword)
/*TODO*///			{
/*TODO*///				LOWER_HALF(ataripf[0].vram[adjusted]) = newword;
/*TODO*///				ataripf[0].dirtymap[adjusted] = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		COMBINE_DATA(&ataripf_0_base32[offset]);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		pf_process: Internal routine that loops over chunks of
		the playfield with common parameters and processes them
		via a callback.
	---------------------------------------------------------------*/
	
	static void pf_process(ataripf_data pf, pf_callback callback, mame_bitmap param, rectangle clip)
	{
		//struct ataripf_state *state = pf.statelist;
                int state = 0;
		rectangle finalclip;
		int i;
	
		if (clip != null)
			finalclip = new rectangle(clip);
		else
			finalclip = new rectangle(Machine.visible_area);
	
		/* if the gfx has changed, make sure we have extended usage maps for everyone */
		if (pf.gfxchanged != 0)
		{
			pf.gfxchanged = 0;
			for (i = 0; i < pf.lookupmask + 1; i++)
			{
				int gfxindex = ATARIPF_LOOKUP_GFX(pf.lookup.read(i));
				if (pf.gfxelement[gfxindex].initialized == 0)
				{
					pf_init_gfx(pf, gfxindex);
				}
			}
		}
	
		/* preinitialization */
		pf.process_clip.min_x = finalclip.min_x;
		pf.process_clip.max_x = finalclip.max_x;
	
		/* mark the n+1'th entry with a large scanline */
		pf.statelist[pf.stateindex].scanline = 100000;
		pf.process_param = param;
	
		/* loop over all entries */
		for (i = 0; i < pf.stateindex; i++, state++)
		{
			/* determine the clip rect */
			pf.process_clip.min_y = pf.statelist[0 + state].scanline;
			pf.process_clip.max_y = pf.statelist[1 + state].scanline - 1;
	
			/* skip if we're clipped out */
			if (pf.process_clip.min_y > finalclip.max_y || pf.process_clip.max_y < finalclip.min_y)
				continue;
	
			/* clip the clipper */
			if (pf.process_clip.min_y < finalclip.min_y)
				pf.process_clip.min_y = finalclip.min_y;
			if (pf.process_clip.max_y > finalclip.max_y)
				pf.process_clip.max_y = finalclip.max_y;
	
			/* determine the tile rect */
			pf.process_tiles.min_x = ((pf.statelist[state].xscroll + pf.process_clip.min_x) >> pf.tilexshift) & pf.colmask;
			pf.process_tiles.max_x = ((pf.statelist[state].xscroll + pf.process_clip.max_x + pf.tilewidth) >> pf.tilexshift) & pf.colmask;
			pf.process_tiles.min_y = ((pf.statelist[state].yscroll + pf.process_clip.min_y) >> pf.tileyshift) & pf.rowmask;
			pf.process_tiles.max_y = ((pf.statelist[state].yscroll + pf.process_clip.max_y + pf.tileheight) >> pf.tileyshift) & pf.rowmask;
	
			/* call the callback */
			callback.handler(pf, pf.statelist[state]);
		}
	}
	
	
	/*---------------------------------------------------------------
		pf_render_callback: Internal processing callback that
		renders to the backing bitmap and then copies the result
		to the destination.
	---------------------------------------------------------------*/
	
	static pf_callback pf_render_callback = new pf_callback() {
            public void handler(ataripf_data pf, ataripf_state state) {
		mame_bitmap bitmap = pf.process_param;
		int x, y, bankbits = state.bankbits;
	
/*TODO*///		/* standard loop over tiles */
/*TODO*///		for (y = pf->process_tiles.min_y; y != pf->process_tiles.max_y; y = (y + 1) & pf->rowmask)
/*TODO*///			for (x = pf->process_tiles.min_x; x != pf->process_tiles.max_x; x = (x + 1) & pf->colmask)
/*TODO*///			{
/*TODO*///				int offs = (y << pf->rowshift) + (x << pf->colshift);
/*TODO*///				UINT32 data = pf->vram[offs] | bankbits;
/*TODO*///	
/*TODO*///				/* update only if dirty */
/*TODO*///				if (pf->dirtymap[offs] != data)
/*TODO*///				{
/*TODO*///					int lookup = pf->lookup[(data >> ATARIPF_LOOKUP_DATABITS) & pf->lookupmask];
/*TODO*///					const struct ataripf_gfxelement *gfx = &pf->gfxelement[ATARIPF_LOOKUP_GFX(lookup)];
/*TODO*///					int code = ATARIPF_LOOKUP_CODE(lookup, data);
/*TODO*///					int color = ATARIPF_LOOKUP_COLOR(lookup);
/*TODO*///					int hflip = ATARIPF_LOOKUP_HFLIP(lookup);
/*TODO*///					int vflip = ATARIPF_LOOKUP_VFLIP(lookup);
/*TODO*///					int saved_color_index = 0;
/*TODO*///					int saved_color = 0;
/*TODO*///	
/*TODO*///					/* kludge alert: until we convert to tilemaps, we use pen 0xffff to indicate the transparent */
/*TODO*///					/* color; temporarily change the colortable entry so that the bitmap gets updated appropriately */
/*TODO*///					if (pf->transpens)
/*TODO*///					{
/*TODO*///						saved_color_index = pf->transpen + (color << (gfx->colorshift + ATARIPF_BASE_GRANULARITY_SHIFT));
/*TODO*///						saved_color = gfx->element.colortable[saved_color_index];
/*TODO*///						gfx->element.colortable[saved_color_index] = 0xffff;
/*TODO*///					}
/*TODO*///	
/*TODO*///					/* draw and reset the dirty value */
/*TODO*///					drawgfx(pf->bitmap, &gfx->element, code, color << gfx->colorshift, hflip, vflip,
/*TODO*///							x << pf->tilexshift, y << pf->tileyshift,
/*TODO*///							0, TRANSPARENCY_NONE, 0);
/*TODO*///					pf->dirtymap[offs] = data;
/*TODO*///	
/*TODO*///					/* restore the temporarily changed color */
/*TODO*///					if (pf->transpens)
/*TODO*///						gfx->element.colortable[saved_color_index] = saved_color;
/*TODO*///				}
/*TODO*///	
/*TODO*///				/* track the tiles we've visited */
/*TODO*///				pf->visitmap[offs] = 1;
/*TODO*///			}
/*TODO*///	
/*TODO*///		/* then blast the result */
/*TODO*///		x = -state->xscroll;
/*TODO*///		y = -state->yscroll;
/*TODO*///		if (!pf->transpens)
/*TODO*///			copyscrollbitmap(bitmap, pf->bitmap, 1, &x, 1, &y, &pf->process_clip, TRANSPARENCY_NONE, 0);
/*TODO*///		else
/*TODO*///			copyscrollbitmap(bitmap, pf->bitmap, 1, &x, 1, &y, &pf->process_clip, TRANSPARENCY_PEN, 0xffff);
            }
        };
	
	
	
/*TODO*///	/*---------------------------------------------------------------
/*TODO*///		pf_overrender_callback: Internal processing callback that
/*TODO*///		calls an external function to determine if a tile should
/*TODO*///		be drawn again, and if so, how it should be drawn.
/*TODO*///	---------------------------------------------------------------*/
/*TODO*///	
/*TODO*///	static void pf_overrender_callback(struct ataripf_data *pf, const struct ataripf_state *state)
/*TODO*///	{
/*TODO*///		int x, y, bankbits = state->bankbits;
/*TODO*///		int first_result;
/*TODO*///	
/*TODO*///		/* make the first overrender call */
/*TODO*///		first_result = (*overrender_callback)(&overrender_data, OVERRENDER_BEGIN);
/*TODO*///		if (first_result == OVERRENDER_NONE)
/*TODO*///			return;
/*TODO*///	
/*TODO*///		/* standard loop over tiles */
/*TODO*///		for (y = pf->process_tiles.min_y; y != pf->process_tiles.max_y; y = (y + 1) & pf->rowmask)
/*TODO*///		{
/*TODO*///			int sy = ((y << pf->tileyshift) - state->yscroll) & pf->bitmapymask;
/*TODO*///			if (sy > Machine->visible_area.max_y) sy -= pf->bitmapheight;
/*TODO*///	
/*TODO*///			for (x = pf->process_tiles.min_x; x != pf->process_tiles.max_x; x = (x + 1) & pf->colmask)
/*TODO*///			{
/*TODO*///				int offs = (y << pf->rowshift) + (x << pf->colshift);
/*TODO*///				UINT32 data = pf->vram[offs] | bankbits;
/*TODO*///				int lookup = pf->lookup[(data >> ATARIPF_LOOKUP_DATABITS) & pf->lookupmask];
/*TODO*///				const struct ataripf_gfxelement *gfx = &pf->gfxelement[ATARIPF_LOOKUP_GFX(lookup)];
/*TODO*///				int code = ATARIPF_LOOKUP_CODE(lookup, data);
/*TODO*///	
/*TODO*///				/* fill in the overrender data that might be needed */
/*TODO*///				overrender_data.pfcolor = ATARIPF_LOOKUP_COLOR(lookup);
/*TODO*///				overrender_data.pfpriority = ATARIPF_LOOKUP_PRIORITY(lookup);
/*TODO*///	
/*TODO*///				/* check with the callback to see if we should overrender */
/*TODO*///				if (first_result == OVERRENDER_ALL || (*overrender_callback)(&overrender_data, OVERRENDER_QUERY))
/*TODO*///				{
/*TODO*///					int hflip = ATARIPF_LOOKUP_HFLIP(lookup);
/*TODO*///					int vflip = ATARIPF_LOOKUP_VFLIP(lookup);
/*TODO*///					int sx = ((x << pf->tilexshift) - state->xscroll) & pf->bitmapxmask;
/*TODO*///					if (sx > Machine->visible_area.max_x) sx -= pf->bitmapwidth;
/*TODO*///	
/*TODO*///					/* use either mdrawgfx or drawgfx depending on the mask pens */
/*TODO*///					if (overrender_data.maskpens != 0)
/*TODO*///						mdrawgfx(overrender_data.bitmap, &gfx->element, code, overrender_data.pfcolor << gfx->colorshift, hflip, vflip,
/*TODO*///								sx, sy, &pf->process_clip, overrender_data.drawmode, overrender_data.drawpens,
/*TODO*///								overrender_data.maskpens);
/*TODO*///					else
/*TODO*///						drawgfx(overrender_data.bitmap, &gfx->element, code, overrender_data.pfcolor << gfx->colorshift, hflip, vflip,
/*TODO*///								sx, sy, &pf->process_clip, overrender_data.drawmode, overrender_data.drawpens);
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* make the final call */
/*TODO*///		(*overrender_callback)(&overrender_data, OVERRENDER_FINISH);
/*TODO*///	}
	
	
	/*---------------------------------------------------------------
		pf_eof_callback: This callback is called on scanline 0 to
		reset the playfields.
	---------------------------------------------------------------*/
	
	static timer_callback pf_eof_callback = new timer_callback() {
            public void handler(int map) {
                //struct ataripf_data *pf = &ataripf[map];
	
		/* copy the current state to entry 0 and reset the index */
		ataripf[map].statelist[0] = ataripf[map].curstate;
		ataripf[map].statelist[0].scanline = 0;
		ataripf[map].stateindex = 1;
	
		/* go off again same time next frame */
		timer_set(cpu_getscanlinetime(0), map, pf_eof_callback);
            }
        };
	
	
	/*---------------------------------------------------------------
		pf_init_gfx: Initializes our own internal graphics
		representation.
	---------------------------------------------------------------*/
	
	static void pf_init_gfx(ataripf_data pf, int gfxindex)
	{
		ataripf_gfxelement gfx = pf.gfxelement[gfxindex];
	
		/* make a copy of the original GfxElement structure */
		gfx.element = Machine.gfx[gfxindex];
	
		/* adjust the granularity */
		gfx.colorshift = compute_log(gfx.element.color_granularity / ATARIPF_BASE_GRANULARITY());
		gfx.element.color_granularity = ATARIPF_BASE_GRANULARITY();
		gfx.element.total_colors = pf.maxcolors;
		gfx.element.colortable = new IntArray(Machine.remapped_colortable, pf.palettebase);
	
		gfx.initialized = 1;
                
                pf.gfxelement[gfxindex] = gfx;
	}
}
