/*##########################################################################

	atarimo.h

	Common motion object management functions for Atari raster games.

##########################################################################*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static mame056.commonH.*;
import static mame056.drawgfxH.*;

public class atarimoH
{
	
	
	/*##########################################################################
		CONSTANTS
	##########################################################################*/
	
	/* maximum number of motion object processors */
	public static final int ATARIMO_MAX			= 2;
	
	/* maximum objects per bank */
	public static final int ATARIMO_MAXPERBANK              = 1024;
	
	
	
	/*##########################################################################
		TYPES & STRUCTURES
	##########################################################################*/
	
	/* callback for special processing */
        public static abstract interface atarimo_special_cb {

            public abstract int handler(mame_bitmap bitmap, rectangle clip, int code, int color, int xpos, int ypos);
        }
	
	/* description for a four-word mask */
	public static class atarimo_entry
	{
		int[]			data = new int[4];
                
                public atarimo_entry(int _a, int _b, int _c, int _d){
                    data[0]=_a;
                    data[1]=_b;
                    data[2]=_c;
                    data[3]=_d;
                }

                public atarimo_entry() {
                        data[0]=0;
                        data[1]= 0;
                        data[2]= 0;
                        data[3]= 0;
                }
	};
	
	/* description of the motion objects */
	public static class atarimo_desc
	{
		int				gfxindex;			/* index to which gfx system */
		int				banks;				/* number of motion object banks */
		int				linked;				/* are the entries linked? */
		int				split;				/* are the entries split? */
		int				reverse;			/* render in reverse order? */
		int				swapxy;				/* render in swapped X/Y order? */
		int				nextneighbor;		/* does the neighbor bit affect the next object? */
		int				slipheight;			/* pixels per SLIP entry (0 for no-slip) */
		int				updatescans;		/* number of scanlines between MO updates */
	
		int				palettebase;		/* base palette entry */
		int				maxcolors;			/* maximum number of colors */
		int				transpen;			/* transparent pen index */
	
		atarimo_entry linkmask = new atarimo_entry();			/* mask for the link */
		atarimo_entry gfxmask = new atarimo_entry();			/* mask for the graphics bank */
		atarimo_entry codemask = new atarimo_entry();			/* mask for the code index */
		atarimo_entry codehighmask = new atarimo_entry();		/* mask for the upper code index */
		atarimo_entry colormask = new atarimo_entry();			/* mask for the color */
		atarimo_entry xposmask = new atarimo_entry();			/* mask for the X position */
		atarimo_entry yposmask = new atarimo_entry();			/* mask for the Y position */
		atarimo_entry widthmask = new atarimo_entry();			/* mask for the width, in tiles*/
		atarimo_entry heightmask = new atarimo_entry();		/* mask for the height, in tiles */
		atarimo_entry hflipmask = new atarimo_entry();			/* mask for the horizontal flip */
		atarimo_entry vflipmask = new atarimo_entry();			/* mask for the vertical flip */
		atarimo_entry prioritymask = new atarimo_entry();		/* mask for the priority */
		atarimo_entry neighbormask = new atarimo_entry();		/* mask for the neighbor */
		atarimo_entry absolutemask = new atarimo_entry();		/* mask for absolute coordinates */
	
		atarimo_entry ignoremask = new atarimo_entry();		/* mask for the ignore value */
		int			ignorevalue;		/* resulting value to indicate "ignore" */
		atarimo_special_cb	ignorecb;			/* callback routine for ignored entries */
                
                public atarimo_desc(int gfxindex, int banks, int linked, int split, int reverse, int swapxy, int nextneighbor, int slipheight, int updatescans, int palettebase, int maxcolors, int transpen, atarimo_entry linkmask, atarimo_entry gfxmask, atarimo_entry codemask, atarimo_entry codehighmask, atarimo_entry colormask, atarimo_entry xposmask, atarimo_entry yposmask, atarimo_entry widthmask, atarimo_entry heightmask, atarimo_entry hflipmask, atarimo_entry vflipmask, atarimo_entry prioritymask, atarimo_entry neighbormask, atarimo_entry absolutemask, atarimo_entry ignoremask, int ignorevalue, atarimo_special_cb ignorecb) {
                    this.gfxindex = gfxindex;
                    this.banks = banks;
                    this.linked = linked;
                    this.split = split;
                    this.reverse = reverse;
                    this.swapxy = swapxy;
                    this.nextneighbor = nextneighbor;
                    this.slipheight = slipheight;
                    this.updatescans = updatescans;
                    this.palettebase = palettebase;
                    this.maxcolors = maxcolors;
                    this.transpen = transpen;
                    this.linkmask = linkmask;
                    this.gfxmask = gfxmask;
                    this.codemask = codemask;
                    this.codehighmask = codehighmask;
                    this.colormask = colormask;
                    this.xposmask = xposmask;
                    this.yposmask = yposmask;
                    this.widthmask = widthmask;
                    this.heightmask = heightmask;
                    this.hflipmask = hflipmask;
                    this.vflipmask = vflipmask;
                    this.prioritymask = prioritymask;
                    this.neighbormask = neighbormask;
                    this.absolutemask = absolutemask;
                    this.ignoremask = ignoremask;
                    this.ignorevalue = ignorevalue;
                    this.ignorecb = ignorecb;
                }
	};
	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		FUNCTION PROTOTYPES
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	/* setup/shutdown */
/*TODO*///	int atarimo_init(int map, const struct atarimo_desc *desc);
/*TODO*///	UINT16 *atarimo_get_code_lookup(int map, int *size);
/*TODO*///	UINT8 *atarimo_get_color_lookup(int map, int *size);
/*TODO*///	UINT8 *atarimo_get_gfx_lookup(int map, int *size);
/*TODO*///	
/*TODO*///	/* core processing */
/*TODO*///	void atarimo_render(int map, struct mame_bitmap *bitmap, ataripf_overrender_cb callback1, ataripf_overrender_cb callback2);
/*TODO*///	void atarimo_force_update(int map, int scanline);
/*TODO*///	
/*TODO*///	/* atrribute setters */
/*TODO*///	void atarimo_set_bank(int map, int bank, int scanline);
/*TODO*///	void atarimo_set_palettebase(int map, int base, int scanline);
/*TODO*///	void atarimo_set_xscroll(int map, int xscroll, int scanline);
/*TODO*///	void atarimo_set_yscroll(int map, int yscroll, int scanline);
/*TODO*///	
/*TODO*///	/* atrribute getters */
/*TODO*///	int atarimo_get_bank(int map);
/*TODO*///	int atarimo_get_palettebase(int map);
/*TODO*///	int atarimo_get_xscroll(int map);
/*TODO*///	int atarimo_get_yscroll(int map);
/*TODO*///	
/*TODO*///	/* write handlers */
/*TODO*///	WRITE16_HANDLER( atarimo_0_spriteram_w );
/*TODO*///	WRITE16_HANDLER( atarimo_0_spriteram_expanded_w );
/*TODO*///	WRITE16_HANDLER( atarimo_0_slipram_w );
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( atarimo_1_spriteram_w );
/*TODO*///	WRITE16_HANDLER( atarimo_1_slipram_w );
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*##########################################################################
/*TODO*///		GLOBAL VARIABLES
/*TODO*///	##########################################################################*/
/*TODO*///	
/*TODO*///	extern data16_t *atarimo_0_spriteram;
/*TODO*///	extern data16_t *atarimo_0_slipram;
/*TODO*///	
/*TODO*///	extern data16_t *atarimo_1_spriteram;
/*TODO*///	extern data16_t *atarimo_1_slipram;
/*TODO*///	
/*TODO*///	
/*TODO*///	#endif
}
