
package WIP.mame056.vidhrdw;


public class atarianH {
/*TODO*////*##########################################################################
/*TODO*///
/*TODO*///	atarian.h
/*TODO*///
/*TODO*///	Common alphanumerics management functions for Atari raster games.
/*TODO*///
/*TODO*///##########################################################################*/
/*TODO*///
/*TODO*///#ifndef __ATARIAN__
/*TODO*///#define __ATARIAN__


    /*##########################################################################
            CONSTANTS
    ##########################################################################*/

    public static final int ATARIAN_MAX		= 2;


    /*##########################################################################
            TYPES & STRUCTURES
    ##########################################################################*/

    /* description of the alphanumerics */
    public static class atarian_desc
    {
            int				gfxindex;			/* index to which gfx system */
            int				cols, rows;			/* size of the alpha RAM in tiles (x,y) */

            int				palettebase;		/* index of palette base */
            int				maxcolors;			/* maximum number of colors */
            int				palettesplit;		/* mask of the palette split */

            int				tilemask;			/* tile data index mask */
            int				colormask;			/* tile data color mask */
            int				hflipmask;			/* tile data hflip mask */
            int				opaquemask;			/* tile data opacity mask */
            
            public atarian_desc(int gfxindex, int cols, int rows, int palettebase, int maxcolors, int palettesplit, int tilemask, int colormask, int hflipmask, int opaquemask){
                this.gfxindex = gfxindex;
                this.cols = cols;
                this.rows = rows;
                this.palettebase = palettebase;
                this.maxcolors = maxcolors;
                this.palettesplit = palettesplit;
                this.tilemask = tilemask;
                this.colormask = colormask;
                this.hflipmask = hflipmask;
                this.opaquemask = opaquemask;
            }
    };



/*TODO*////*##########################################################################
/*TODO*///	MACROS
/*TODO*///##########################################################################*/
/*TODO*///
/*TODO*////* accessors for the lookup table */
    public static final int ATARIAN_LOOKUP_DATABITS				= 9;
    
    public static int ATARIAN_LOOKUP_DATAMASK(){
        return ((1 << ATARIAN_LOOKUP_DATABITS) - 1);
    }
    
    public static int ATARIAN_LOOKUP_CODEMASK(){
        return (0xffff ^ ATARIAN_LOOKUP_DATAMASK());
    }

    public static int ATARIAN_LOOKUP_CODE(int lookup, int data){
        return (((lookup) & ATARIAN_LOOKUP_CODEMASK()) | ((data) & ATARIAN_LOOKUP_DATAMASK()));
    }
    public static int ATARIAN_LOOKUP_COLOR(int lookup){
        return (((lookup) >> 16) & 0x7ff);
    }
    public static int ATARIAN_LOOKUP_HFLIP(int lookup){
        return (((lookup) >> 27) & 1);
    }
    public static int ATARIAN_LOOKUP_OPAQUE(int lookup){
        return (((lookup) >> 28) & 1);
    }
/*TODO*///#define ATARIAN_LOOKUP_GFX(lookup)			(((lookup) >> 29) & 7)
/*TODO*///
/*TODO*///#define ATARIAN_LOOKUP_ENTRY(gfxindex, code, color, hflip, opaque)		\
/*TODO*///			(((gfxindex) & 7) << 29) |									\
/*TODO*///			(((opaque) & 1) << 28) |									\
/*TODO*///			(((hflip) & 1) << 27) |										\
/*TODO*///			(((color) & 0x7ff) << 16) |									\
/*TODO*///			(((code) % Machine->gfx[gfxindex]->total_elements) & ATARIAN_LOOKUP_CODEMASK)
/*TODO*///
/*TODO*///#define ATARIAN_LOOKUP_SET_CODE(data,code)		((data) = ((data) & ~ATARIAN_LOOKUP_CODEMASK) | ((code) & ATARIAN_LOOKUP_CODEMASK))
/*TODO*///#define ATARIAN_LOOKUP_SET_COLOR(data,color)	((data) = ((data) & ~0x07ff0000) | (((color) << 16) & 0x07ff0000))
/*TODO*///#define ATARIAN_LOOKUP_SET_HFLIP(data,hflip)	((data) = ((data) & ~0x08000000) | (((hflip) << 27) & 0x08000000))
/*TODO*///#define ATARIAN_LOOKUP_SET_OPAQUE(data,opq)		((data) = ((data) & ~0x10000000) | (((opq) << 28) & 0x10000000))
/*TODO*///#define ATARIAN_LOOKUP_SET_GFX(data,gfx)		((data) = ((data) & ~0xe0000000) | (((gfx) << 29) & 0xe0000000))
/*TODO*///
/*TODO*///
/*TODO*////*##########################################################################
/*TODO*///	FUNCTION PROTOTYPES
/*TODO*///##########################################################################*/
/*TODO*///
/*TODO*////* setup/shutdown */
/*TODO*///int atarian_init(int map, const struct atarian_desc *desc);
/*TODO*///UINT32 *atarian_get_lookup(int map, int *size);
/*TODO*///
/*TODO*////* core processing */
/*TODO*///void atarian_render(int map, struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*////* atrribute setters */
/*TODO*///void atarian_set_bankbits(int map, int bankbits);
/*TODO*///
/*TODO*////* atrribute getters */
/*TODO*///int atarian_get_bankbits(int map);
/*TODO*///data16_t *atarian_get_vram(int map);
/*TODO*///
/*TODO*////* write handlers */
/*TODO*///WRITE16_HANDLER( atarian_0_vram_w );
/*TODO*///WRITE16_HANDLER( atarian_1_vram_w );
/*TODO*///
/*TODO*///WRITE32_HANDLER( atarian_0_vram32_w );
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*##########################################################################
/*TODO*///	GLOBAL VARIABLES
/*TODO*///##########################################################################*/
/*TODO*///
/*TODO*///extern data16_t *atarian_0_base;
/*TODO*///extern data16_t *atarian_1_base;
/*TODO*///
/*TODO*///extern data32_t *atarian_0_base32;
/*TODO*///
/*TODO*///
/*TODO*///#endif
    
}
