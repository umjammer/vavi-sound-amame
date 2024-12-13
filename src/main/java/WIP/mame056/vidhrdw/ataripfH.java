/*##########################################################################

	ataripf.c

	Common playfield management functions for Atari raster games.

##########################################################################*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

/**
 *
 * @author chusogar
 */
public class ataripfH {
    
    /*##########################################################################
            CONSTANTS
    ##########################################################################*/

    /* maximum number of playfields */
    public static final int ATARIPF_MAX			= 2;

    /* overrendering constants */
    public static final int OVERRENDER_BEGIN	= 0;
    public static final int OVERRENDER_QUERY	= 1;
    public static final int OVERRENDER_FINISH	= 2;

    /* return results for OVERRENDER_BEGIN case */
    public static final int OVERRENDER_NONE		= 0;
    public static final int OVERRENDER_SOME		= 1;
    public static final int OVERRENDER_ALL		= 2;

    /* return results for OVERRENDER_QUERY case */
    public static final int OVERRENDER_NO		= 0;
    public static final int OVERRENDER_YES		= 1;

/*TODO*////* latch masks */
/*TODO*///#define LATCHMASK_NONE		0x0000
/*TODO*///#define LATCHMASK_MSB		0xff00
/*TODO*///#define LATCHMASK_LSB		0x00ff

    /* base granularity for all playfield gfx */
    public static final int ATARIPF_BASE_GRANULARITY_SHIFT	= 3;
    public static int ATARIPF_BASE_GRANULARITY(){ return (1 << ATARIPF_BASE_GRANULARITY_SHIFT); }



    /*##########################################################################
            TYPES & STRUCTURES
    ##########################################################################*/

    /* description of the playfield */
    public static class ataripf_desc
    {
            int				gfxindex;			/* index to which gfx system */
            int				cols, rows;			/* size of the playfield in tiles (x,y) */
            int				xmult, ymult;		/* tile_index = y * ymult + x * xmult (xmult,ymult) */

            int				palettebase;		/* index of palette base */
            int				maxcolors;			/* maximum number of colors */
            int				shadowxor;			/* color XOR for shadow effect (if any) */
            int				latchmask;			/* latch mask */
            int				transpens;			/* transparent pen mask */

            int				tilemask;			/* tile data index mask */
            int				colormask;			/* tile data color mask */
            int				hflipmask;			/* tile data hflip mask */
            int				vflipmask;			/* tile data hflip mask */
            int				prioritymask;		/* tile data priority mask */
            
            public ataripf_desc(int gfxindex, int cols, int rows, int xmult, int ymult, int palettebase, int maxcolors, int shadowxor, int latchmask, int transpens, int tilemask, int colormask, int hflipmask, int vflipmask, int prioritymask){
                this.gfxindex = gfxindex;
                this.cols = cols;
                this.rows = rows;
                this.xmult = xmult;
                this.ymult = ymult;
                this.palettebase = palettebase;
                this.maxcolors = maxcolors;
                this.shadowxor = shadowxor;
                this.latchmask = latchmask;
                this.transpens = transpens;
                this.tilemask = tilemask;
                this.colormask = colormask;
                this.hflipmask = hflipmask;
                this.vflipmask = vflipmask;
                this.prioritymask = prioritymask;
            }
    };


    /* data used for overrendering */
    public static class ataripf_overrender_data
    {
/*TODO*///	/* these are passed in to ataripf_overrender */
/*TODO*///	struct mame_bitmap *	bitmap;				/* bitmap we're drawing to */
/*TODO*///	struct rectangle 	clip;				/* clip region to overrender with */
/*TODO*///	UINT32				mousage;			/* motion object pen usage */
/*TODO*///	UINT32				mocolor;			/* motion object color */
	int				mopriority;			/* motion object priority */

/*TODO*///	/* these are filled in for the callback's usage */
/*TODO*///	UINT32				pfcolor;			/* playfield tile color */
	int				pfpriority;			/* playfield tile priority */

	/* these can be modified by the callback, along with pfcolor, above */
	int					drawmode;			/* how should the tile be drawn */
	int					drawpens;			/* which pens? */
	int					maskpens;			/* mask pens */
    };


    /* overrendering callback function */
    public static abstract interface ataripf_overrender_cb {
        public abstract int handler(ataripf_overrender_data data, int stage);
    }


    /*##########################################################################
            MACROS
    ##########################################################################*/

    /* accessors for the lookup table */
    public static int ATARIPF_LOOKUP_DATABITS				= 8;
    
    public static int ATARIPF_LOOKUP_DATAMASK(){
        return ((1 << ATARIPF_LOOKUP_DATABITS) - 1);
    }
    
    public static int ATARIPF_LOOKUP_CODEMASK(){ 
        return (0xffff ^ ATARIPF_LOOKUP_DATAMASK());
    }

/*TODO*///#define ATARIPF_LOOKUP_CODE(lookup,data)	(((lookup) & ATARIPF_LOOKUP_CODEMASK) | ((data) & ATARIPF_LOOKUP_DATAMASK))
/*TODO*///#define ATARIPF_LOOKUP_COLOR(lookup)		(((lookup) >> 16) & 0xff)
/*TODO*///#define ATARIPF_LOOKUP_HFLIP(lookup)		(((lookup) >> 24) & 1)
/*TODO*///#define ATARIPF_LOOKUP_VFLIP(lookup)		(((lookup) >> 25) & 1)
/*TODO*///#define ATARIPF_LOOKUP_PRIORITY(lookup)		(((lookup) >> 26) & 7)
    public static int ATARIPF_LOOKUP_GFX(int lookup){ return (((lookup) >> 29) & 7); }

/*TODO*///#define ATARIPF_LOOKUP_ENTRY(gfxindex, code, color, hflip, vflip, priority)	\
/*TODO*///			(((gfxindex) & 7) << 29) |										\
/*TODO*///			(((priority) & 7) << 26) |										\
/*TODO*///			(((vflip) & 1) << 25) |											\
/*TODO*///			(((hflip) & 1) << 24) |											\
/*TODO*///			(((color) & 0xff) << 16) |										\
/*TODO*///			(((code) % Machine->gfx[gfxindex]->total_elements) & ATARIPF_LOOKUP_CODEMASK)

    public static void ATARIPF_LOOKUP_SET_CODE(int[] data, int code){
        data[0] = (data[0] & ~ATARIPF_LOOKUP_CODEMASK()) | ((code) & ATARIPF_LOOKUP_CODEMASK());
    }
/*TODO*///#define ATARIPF_LOOKUP_SET_COLOR(data,color)	((data) = ((data) & ~0x00ff0000) | (((color) << 16) & 0x00ff0000))
/*TODO*///#define ATARIPF_LOOKUP_SET_HFLIP(data,hflip)	((data) = ((data) & ~0x01000000) | (((hflip) << 24) & 0x01000000))
/*TODO*///#define ATARIPF_LOOKUP_SET_VFLIP(data,vflip)	((data) = ((data) & ~0x02000000) | (((vflip) << 25) & 0x02000000))
/*TODO*///#define ATARIPF_LOOKUP_SET_PRIORITY(data,pri)	((data) = ((data) & ~0x1c000000) | (((pri) << 26) & 0x1c000000))
/*TODO*///#define ATARIPF_LOOKUP_SET_GFX(data,gfx)		((data) = ((data) & ~0xe0000000) | (((gfx) << 29) & 0xe0000000))
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*##########################################################################
/*TODO*///	FUNCTION PROTOTYPES
/*TODO*///##########################################################################*/
/*TODO*///
/*TODO*////* preinitialization */
/*TODO*///void ataripf_blend_gfx(int gfx0, int gfx1, int mask0, int mask1);
/*TODO*///
/*TODO*////* setup/shutdown */
/*TODO*///int ataripf_init(int map, const struct ataripf_desc *desc);
/*TODO*///UINT32 *ataripf_get_lookup(int map, int *size);
/*TODO*///
/*TODO*////* core processing */
/*TODO*///void ataripf_render(int map, struct mame_bitmap *bitmap);
/*TODO*///void ataripf_overrender(int map, ataripf_overrender_cb callback, struct ataripf_overrender_data *data);
/*TODO*///
/*TODO*////* atrribute setters */
/*TODO*///void ataripf_set_bankbits(int map, int bankbits, int scanline);
/*TODO*///void ataripf_set_xscroll(int map, int xscroll, int scanline);
/*TODO*///void ataripf_set_yscroll(int map, int yscroll, int scanline);
/*TODO*///void ataripf_set_latch(int map, int latch);
/*TODO*///void ataripf_set_latch_lo(int latch);
/*TODO*///void ataripf_set_latch_hi(int latch);
/*TODO*///void ataripf_set_transparent_pens(int map, int pens);
/*TODO*///
/*TODO*////* atrribute getters */
/*TODO*///int ataripf_get_bankbits(int map);
/*TODO*///int ataripf_get_xscroll(int map);
/*TODO*///int ataripf_get_yscroll(int map);
/*TODO*///UINT32 *ataripf_get_vram(int map);
/*TODO*///
/*TODO*////* write handlers */
/*TODO*///WRITE16_HANDLER( ataripf_0_simple_w );
/*TODO*///WRITE16_HANDLER( ataripf_0_latched_w );
/*TODO*///WRITE16_HANDLER( ataripf_0_upper_msb_w );
/*TODO*///WRITE16_HANDLER( ataripf_0_upper_lsb_w );
/*TODO*///WRITE16_HANDLER( ataripf_0_large_w );
/*TODO*///WRITE16_HANDLER( ataripf_0_split_w );
/*TODO*///
/*TODO*///WRITE16_HANDLER( ataripf_1_simple_w );
/*TODO*///WRITE16_HANDLER( ataripf_1_latched_w );
/*TODO*///
/*TODO*///WRITE16_HANDLER( ataripf_01_upper_lsb_msb_w );
/*TODO*///
/*TODO*///WRITE32_HANDLER( ataripf_0_split32_w );
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*##########################################################################
/*TODO*///	GLOBAL VARIABLES
/*TODO*///##########################################################################*/
/*TODO*///
/*TODO*///extern data16_t *ataripf_0_base;
/*TODO*///extern data16_t *ataripf_0_upper;
/*TODO*///extern data16_t *ataripf_1_base;
/*TODO*///extern data16_t *ataripf_1_upper;
/*TODO*///
/*TODO*///extern data32_t *ataripf_0_base32;
/*TODO*///
/*TODO*///
/*TODO*///#endif
    
}
