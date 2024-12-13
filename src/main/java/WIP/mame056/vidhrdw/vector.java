/******************************************************************************
 *
 * vector.c
 *
 *
 * Copyright 1997,1998 by the M.A.M.E. Project
 *
 *        anti-alias code by Andrew Caldwell
 *        (still more to add)
 *
 * 980611 use translucent vectors. Thanks to Peter Hirschberg
 *        and Neil Bradley for the inspiration. BW
 * 980307 added cleverer dirty handling. BW, ASG
 *        fixed antialias table .ac
 * 980221 rewrote anti-alias line draw routine
 *        added inline assembly multiply fuction for 8086 based machines
 *        beam diameter added to draw routine
 *        beam diameter is accurate in anti-alias line draw (Tcosin)
 *        flicker added .ac
 * 980203 moved LBO's routines for drawing into a buffer of vertices
 *        from avgdvg.c to this location. Scaling is now initialized
 *        by calling vector_init(...). BW
 * 980202 moved out of msdos.c ASG
 * 980124 added anti-alias line draw routine
 *        modified avgdvg.c and sega.c to support new line draw routine
 *        added two new tables Tinten and Tmerge (for 256 color support)
 *        added find_color routine to build above tables .ac
 * 010903 added support for direct RGB modes MLR
 *
 **************************************************************************** */

/* GLmame and FXmame provide their own vector implementations */
/*TODO*///#if !(defined xgl) && !(defined xfx) && !(defined svgafx)

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.dirtyH.*;

import static common.libc.cstdlib.rand;

import static java.lang.Math.*;
import static mame056.driverH.*;
import static WIP.mame056.vidhrdw.vectorH.*;

public class vector
{
	
	public static int VCLEAN = 0;
	public static int VDIRTY = 1;
	public static int VCLIP  = 2;
	
	public static UBytePtr vectorram = new UBytePtr();
	public static int[] vectorram_size = new int[1];
	
	static int vector_orientation;
	
	static int antialias;                            /* flag for anti-aliasing */
	static int beam;                                 /* size of vector beam    */
	static int flicker;                              /* beam flicker value     */
	public static int translucency;
	
	static int beam_diameter_is_one;		  /* flag that beam is one pixel wide */
	
	static int vector_scale_x;                /* scaling to screen */
	static int vector_scale_y;                /* scaling to screen */
	
	static float gamma_correction = 1.2f;
	static float flicker_correction = 0.0f;
	static float intensity_correction = 1.5f;
	
	/* The vectices are buffered here */
	public static class point
	{
		public int x; public int y;
		public int col;
		public int intensity;
		public int arg1; public int arg2; /* start/end in pixel array or clipping info */
		public int status;         /* for dirty and clipping handling */
	};
	
	static point[] new_list=new point[1024];
	static point[] old_list=new point[1024];
	static int new_index;
	static int old_index;
	
	/* coordinates of pixels are stored here for faster removal */
	static int[] pixel;
	static int p_index=0;
	
	static int[] pTcosin;            /* adjust line width */
	
	public static int Tcosin(int x){
            return pTcosin[(x)];          /* adjust line width */
        }
	
	public static int ANTIALIAS_GUNBIT = 6;             /* 6 bits per gun in vga (1-8 valid) */
	public static int ANTIALIAS_GUNNUM = (1<<ANTIALIAS_GUNBIT);
	
	static int[] Tgamma=new int[256];         /* quick gamma anti-alias table  */
	static int[] Tgammar=new int[256];        /* same as above, reversed order */
	
	static mame_bitmap vecbitmap;
	static int vecwidth, vecheight;
	static int vecshift;
	static int xmin, ymin, xmax, ymax; /* clipping area */
	
	static int vector_runs;	/* vector runs per refresh */
        
        static int vector_draw_aa_pixel_method;
        public static final int vector_draw_aa_pixel_15_method = 0;
        public static final int vector_draw_aa_pixel_32_method = 1;
	
	/*TODO*///static void (*vector_draw_aa_pixel)(int x, int y, int col, int dirty);
	
	/*TODO*///static void vector_draw_aa_pixel_15 (int x, int y, int col, int dirty);
	/*TODO*///static void vector_draw_aa_pixel_32 (int x, int y, int col, int dirty);
	
	/*
	 * multiply and divide routines for drawing lines
	 * can be be replaced by an assembly routine in osinline.h
	 */
	
	public static int vec_mult(int parm1, int parm2)
	{
		int temp,result;
	
		temp     = abs(parm1);
		result   = (temp&0x0000ffff) * (parm2&0x0000ffff);
		result >>= 16;
		result  += (temp&0x0000ffff) * (parm2>>16       );
		result  += (temp>>16       ) * (parm2&0x0000ffff);
		result >>= 16;
		result  += (temp>>16       ) * (parm2>>16       );
	
		if( parm1 < 0 )
			return(-result);
		else
			return( result);
	}
	
	
	/* can be be replaced by an assembly routine in osinline.h */
	
	public static int vec_div(int parm1, int parm2)
	{
		if( (parm2>>12) != 0 )
		{
			parm1 = (parm1<<4) / (parm2>>12);
			if( parm1 > 0x00010000 )
				return( 0x00010000 );
			if( parm1 < -0x00010000 )
				return( -0x00010000 );
			return( parm1 );
		}
		return( 0x00010000 );
	}
	
	
	public static int Tinten(int intensity, int col){
            return ((((col) & 0xff) * (intensity)) >> 8)
		| (((((col) >> 8) & 0xff) * (intensity)) & 0xff00)
		| (((((col) >> 16) * (intensity)) >> 8) << 16);
        }
	
	/* MLR 990316 new gamma handling added */
	public static void vector_set_gamma(float _gamma)
	{
		int i, h;
	
		gamma_correction = _gamma;
	
		for (i = 0; i < 256; i++)
		{
			h = (int) (255.0*pow(i/255, 1/gamma_correction));
			if( h > 255) h = 255;
			Tgamma[i] = Tgammar[255-i] = h;
		}
	}
	
	public static float vector_get_gamma()
	{
		return gamma_correction;
	}
	
	public static void vector_set_flicker(float _flicker)
	{
		flicker_correction = _flicker;
		flicker = (int)(flicker_correction * 2.55);
	}
	
	public static float vector_get_flicker()
	{
		return flicker_correction;
	}
	
	public static void vector_set_intensity(float _intensity)
	{
		intensity_correction = _intensity;
	}
	
	public static float vector_get_intensity()
	{
		return intensity_correction;
	}
	
	/*
	 * Initializes vector game video emulation
	 */
	
	public static VhStartPtr vector_vh_start = new VhStartPtr() { public int handler() 
	{
		int i;
	
		/* Grab the settings for this session */
		antialias = options.antialias;
		translucency = options.translucency;
		vector_set_flicker(options.vector_flicker);
		beam = options.beam;
	
	
		if (beam == 0x00010000)
			beam_diameter_is_one = 1;
		else
			beam_diameter_is_one = 0;
	
		p_index = 0;
	
		new_index = 0;
		old_index = 0;
		vector_runs = 0;
	
		switch(Machine.color_depth)
		{
		case 15:
			vector_draw_aa_pixel_method = vector_draw_aa_pixel_15_method;
			break;
		case 32:
			vector_draw_aa_pixel_method = vector_draw_aa_pixel_32_method;
			break;
		default:
			logerror ("Vector games have to use direct RGB modes!\n");
			return 1;
			//break;
		}
	
		/* allocate memory for tables */
		pTcosin = new int[2048+1];   /* yes! 2049 is correct */
		pixel = new int[MAX_PIXELS];
		old_list = new point[MAX_POINTS];
		new_list = new point[MAX_POINTS];
	
		/* did we get the requested memory? */
		if (!((pTcosin!=null) && (pixel!=null) && (old_list!=null) && (new_list!=null)))
		{
			/* vector_vh_stop should better be called by the main engine */
			/* if vector_vh_start fails */
			vector_vh_stop.handler();
			return 1;
		}
	
		/* build cosine table for fixing line width in antialias */
		for (i=0; i<=2048; i++)
		{
			pTcosin[i] = (int)((double)(1.0/cos(atan((double)(i)/2048.0)))*0x10000000 + 0.5);
		}
	
		vector_set_flip_x(0);
		vector_set_flip_y(0);
		vector_set_swap_xy(0);
	
		/* build gamma correction table */
		vector_set_gamma (gamma_correction);
	
		return 0;
	} };
	
	
	
	public static void vector_set_flip_x (int flip)
	{
		if (flip != 0)
			vector_orientation |=  ORIENTATION_FLIP_X;
		else
			vector_orientation &= ~ORIENTATION_FLIP_X;
	}
	
	public static void vector_set_flip_y (int flip)
	{
		if (flip != 0)
			vector_orientation |=  ORIENTATION_FLIP_Y;
		else
			vector_orientation &= ~ORIENTATION_FLIP_Y;
	}
	
	public static void vector_set_swap_xy (int swap)
	{
		if (swap != 0)
			vector_orientation |=  ORIENTATION_SWAP_XY;
		else
			vector_orientation &= ~ORIENTATION_SWAP_XY;
	}
	
	
	/*
	 * Setup scaling. Currently the Sega games are stuck at VECSHIFT 15
	 * and the the AVG games at VECSHIFT 16
	 */
	public static void vector_set_shift (int shift)
	{
		vecshift = shift;
	}
	
	/*
	 * Clear the old bitmap. Delete pixel for pixel, this is faster than memset.
	 */
	static void vector_clear_pixels ()
	{
		int i;
		int coords;
	
		if (Machine.color_depth == 32)
		{
			for (i=p_index-1; i>=0; i--)
			{
				coords = pixel[i];
				//((UINT32 *)vecbitmap.line[coords & 0xffff])[coords >> 16] = 0;
                                (vecbitmap.line[coords & 0xffff]).write(coords >> 16, 0);
			}
		}
		else
		{
			for (i=p_index-1; i>=0; i--)
			{
				coords = pixel[i];
				//((UINT16 *)vecbitmap.line[coords & 0xffff])[coords >> 16] = 0;
                                (new UShortPtr(vecbitmap.line[coords & 0xffff])).write(coords >> 16, (char)0);
			}
		}
		p_index=0;
	}
	
	/*
	 * Stop the vector video hardware emulation. Free memory.
	 */
	public static VhStopPtr vector_vh_stop = new VhStopPtr() { public void handler() 
	{
		if (pTcosin != null)
			pTcosin = null;
		
		if (pixel != null)
			pixel = null;
                
		if (old_list != null)
			old_list = null;
                
		if (new_list != null)
			new_list = null;
	} };
	
	/*
	 * draws an anti-aliased pixel (blends pixel with background)
	 */
	public static int LIMIT5(int x){
            return ((x < 0x1f)? x : 0x1f);
        }
        
	public static int LIMIT8(int x){
            return ((x < 0xff)? x : 0xff);
        }
        
        public static void vector_draw_aa_pixel(int x, int y, int col, int dirty){
            switch (vector_draw_aa_pixel_method){
                
                case vector_draw_aa_pixel_15_method:
                    vector_draw_aa_pixel_15(x, y, col, dirty);
                break;
                
                case vector_draw_aa_pixel_32_method:
                    vector_draw_aa_pixel_32(x, y, col, dirty);
                break;
            }
        }
        
        
	static void vector_draw_aa_pixel_15 (int x, int y, int col, int dirty)
	{
            if ((col == 0)&&(dirty!=0)){
                    col = 0x00FF00;
            }
		int dst;
	
		if (x < xmin || x >= xmax)
			return;
		if (y < ymin || y >= ymax)
			return;
	
		//dst = ((UINT16 *)vecbitmap.line[y])[x];
                dst = (new UShortPtr(vecbitmap.line[y])).read(x);
		//((UINT16 *)vecbitmap.line[y])[x] = LIMIT5(((col>>3) & 0x1f) + (dst & 0x1f))
                if (dirty!=0)
                (new UShortPtr(vecbitmap.line[y])).write(x, (char) 0x00FF00);
	
		if (p_index<MAX_PIXELS)
		{
			pixel[p_index] = y | (x << 16);
			p_index++;
		}
	
		/* Mark this pixel as dirty */
		if (dirty != 0)
			osd_mark_vector_dirty (x, y);
	}
	
	static void vector_draw_aa_pixel_32 (int x, int y, int col, int dirty)
	{
            System.out.println("vector_draw_aa_pixel_32");
		int dst;
	
		if (x < xmin || x >= xmax)
			return;
		if (y < ymin || y >= ymax)
			return;
	
		//dst = ((UINT32 *)vecbitmap.line[y])[x];
                dst = ((vecbitmap.line[y])).read(x);
		//((UINT32 *)vecbitmap.line[y])[x] = LIMIT8((col & 0xff) + (dst & 0xff))
                ((vecbitmap.line[y])).write(x, (char) (LIMIT8((col & 0xff) + (dst & 0xff))
                        | (LIMIT8(((col >> 8) & 0xff) + ((dst >> 8) & 0xff)) << 8)
                        | (LIMIT8((col >> 16) + (dst >> 16)) << 16)));
	
		if (p_index<MAX_PIXELS)
		{
			pixel[p_index] = y | (x << 16);
			p_index++;
		}
	
		/* Mark this pixel as dirty */
		if (dirty != 0)
			osd_mark_vector_dirty (x, y);
	}
	
	
	/*
	 * draws a line
	 *
	 * input:   x2  16.16 fixed point
	 *          y2  16.16 fixed point
	 *         col  0-255 indexed color (8 bit)
	 *   intensity  0-255 intensity
	 *       dirty  bool  mark the pixels as dirty while plotting them
	 *
	 * written by Andrew Caldwell
	 */
        
        static int x1=0,yy1=0;
        
        public static class struct_draw_to {
            public int x2;
            public int y2;
            public int col;
            public int intensity;
            public int dirty;
        }
	
	public static struct_draw_to vector_draw_to (struct_draw_to _param)
	{
                //x1=0;yy1=0;
		int a1=0;
		int dx=0,dy=0,sx=0,sy=0,cx=0,cy=0,width=0;
		
		int xx=0,yy=0;
		int xy_swap=0;
                //antialias = 1;
	
		/* [1] scale coordinates to display */
	
		_param.x2 = vec_mult(_param.x2<<4,vector_scale_x);
		_param.y2 = vec_mult(_param.y2<<4,vector_scale_y);
	
		/* [2] fix display orientation */
	
		if (((Machine.orientation ^ vector_orientation) & ORIENTATION_SWAP_XY) != 0)
			xy_swap = 1;
		else
			xy_swap = 0;
	
		if ((vector_orientation & ORIENTATION_FLIP_X) != 0)
		{
			if (xy_swap != 0)
				_param.x2 = ((vecheight-1)<<16)-_param.x2;
			else
				_param.x2 = ((vecwidth-1)<<16)-_param.x2;
		}
		if ((vector_orientation & ORIENTATION_FLIP_Y) != 0)
		{
			if (xy_swap != 0)
				_param.y2 = ((vecwidth-1)<<16)-_param.y2;
			else
				_param.y2 = ((vecheight-1)<<16)-_param.y2;
		}
	
		if (((Machine.orientation ^ vector_orientation) & ORIENTATION_SWAP_XY) != 0)
		{
			int temp;
			temp = _param.x2;
			_param.x2 = _param.y2;
			_param.y2 = temp;
		}
		if ((Machine.orientation & ORIENTATION_FLIP_X) != 0)
			_param.x2 = ((vecwidth-1)<<16)-_param.x2;
		if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0)
			_param.y2 = ((vecheight-1)<<16)-_param.y2;
	
		/* [3] adjust cords if needed */
	
		if (antialias != 0)
		{
			if(beam_diameter_is_one != 0)
			{
				_param.x2 = (_param.x2+0x8000)&0xffff0000;
				_param.y2 = (_param.y2+0x8000)&0xffff0000;
			}
		}
		else /* noantialiasing */
		{
			_param.x2 >>= 16;
			_param.y2 >>= 16;
		}
	
		/* [4] handle color and intensity */
	
		//if (intensity == 0) goto end_draw;
                /*if (_param.intensity == 0){
                    x1=_param.x2;
                    yy1=_param.y2;
                    return _param;
                }*/
	
		_param.col = Tinten(_param.intensity,_param.col);
	
		/* [5] draw line */
	
		if (antialias != 0)
		{
			/* draw an anti-aliased line */
			dx=abs(x1-_param.x2);
			dy=abs(yy1-_param.y2);
			if (dx>=dy)
			{
				sx = ((x1 <= _param.x2) ? 1:-1);
				sy = vec_div(_param.y2-yy1,dx);
				if (sy<0)
					dy--;
				x1 >>= 16;
				xx = _param.x2>>16;
				width = vec_mult(beam<<4,Tcosin(abs(sy)>>5));
				if (beam_diameter_is_one == 0)
					yy1-= width>>1; /* start back half the diameter */
                                System.out.println("Draw line-> "+_param.col);
				for (;;)
				{
					dx = width;    /* init diameter of beam */
					dy = yy1>>16;
					vector_draw_aa_pixel(x1,dy++,Tinten(Tgammar[0xff&(yy1>>8)],_param.col), _param.dirty);
					dx -= 0x10000-(0xffff & yy1); /* take off amount plotted */
					a1 = Tgamma[(dx>>8)&0xff];   /* calc remainder pixel */
					dx >>= 16;                   /* adjust to pixel (solid) count */
					while (dx-- != 0)                 /* plot rest of pixels */
						vector_draw_aa_pixel(x1,dy++,_param.col, _param.dirty);
					vector_draw_aa_pixel(x1,dy,Tinten(a1,_param.col), _param.dirty);
					if (x1 == xx) break;
					x1+=sx;
					yy1+=sy;
				}
			}
			else
			{
				sy = ((yy1 <= _param.y2) ? 1:-1);
				sx = vec_div(_param.x2-x1,dy);
				if (sx<0)
					dx--;
				yy1 >>= 16;
				yy = _param.y2>>16;
				width = vec_mult(beam<<4,Tcosin(abs(sx)>>5));
				if (beam_diameter_is_one == 0)
					x1-= width>>1; /* start back half the width */
				for (;;)
				{
					dy = width;    /* calc diameter of beam */
					dx = x1>>16;
					vector_draw_aa_pixel(dx++,yy1,Tinten(Tgammar[0xff&(x1>>8)],_param.col), _param.dirty);
					dy -= 0x10000-(0xffff & x1); /* take off amount plotted */
					a1 = Tgamma[(dy>>8)&0xff];   /* remainder pixel */
					dy >>= 16;                   /* adjust to pixel (solid) count */
					while (dy-- != 0)                 /* plot rest of pixels */
						vector_draw_aa_pixel(dx++,yy1,_param.col, _param.dirty);
					vector_draw_aa_pixel(dx,yy1,Tinten(a1,_param.col), _param.dirty);
					if (yy1 == yy) break;
					yy1+=sy;
					x1+=sx;
				}
			}
		}
		else /* use good old Bresenham for non-antialiasing 980317 BW */
		{
			dx = abs(x1-_param.x2);
			dy = abs(yy1-_param.y2);
			sx = (x1 <= _param.x2) ? 1: -1;
			sy = (yy1 <= _param.y2) ? 1: -1;
			cx = dx/2;
			cy = dy/2;
                        
                        //System.out.println("Draw line2-> "+col);
                        //col = 0;
	
			if (dx>=dy)
			{
				for (;;)
				{
					vector_draw_aa_pixel (x1, yy1, _param.col, _param.dirty);
					if (x1 == _param.x2) break;
					x1 += sx;
					cx -= dy;
					if (cx < 0)
					{
						yy1 += sy;
						cx += dx;
					}
				}
			}
			else
			{
				for (;;)
				{
					vector_draw_aa_pixel (x1, yy1, _param.col, _param.dirty);
					if (yy1 == _param.y2) break;
					yy1 += sy;
					cy -= dx;
					if (cy < 0)
					{
						x1 += sx;
						cy += dy;
					}
				}
			}
		}
	
	//end_draw:
	if (_param.intensity == 0){
		x1=_param.x2;
		yy1=_param.y2;
        }
                
                return _param;
	}
	
	
	/*
	 * Adds a line end point to the vertices list. The vector processor emulation
	 * needs to call this.
	 */
        static class struct_vector_add_point {
            public int x;
            public int y;
            public int color;
            public int intensity;
        }
                
	public static struct_vector_add_point vector_add_point (struct_vector_add_point _param)
	{
		//point _new;
	
		_param.intensity *= intensity_correction;
		if (_param.intensity > 0xff)
			_param.intensity = 0xff;
	
		if (flicker!=0 && (_param.intensity > 0))
		{
			_param.intensity += (_param.intensity * (0x80-(rand()&0xff)) * flicker)>>16;
			if (_param.intensity < 0)
				_param.intensity = 0;
			if (_param.intensity > 0xff)
				_param.intensity = 0xff;
		}
		//new = &new_list[new_index];
                if (new_list[new_index] == null)
                    new_list[new_index] = new point();
                
		new_list[new_index].x = _param.x;
		new_list[new_index].y = _param.y;
		new_list[new_index].col = _param.color;
		new_list[new_index].intensity = _param.intensity;
		new_list[new_index].status = VDIRTY; /* mark identical lines as clean later */
	
		new_index++;
		if (new_index >= MAX_POINTS)
		{
			new_index--;
			logerror("*** Warning! Vector list overflow!\n");
		}
                
                return _param;
	}
	
	/*
	 * Add new clipping info to the list
	 */
	public static void vector_add_clip (int x1, int yy1, int x2, int y2)
	{
		//point _new;
	
		//_new = new_list[new_index];
		new_list[new_index].x = x1;
		new_list[new_index].y = yy1;
		new_list[new_index].arg1 = x2;
		new_list[new_index].arg2 = y2;
		new_list[new_index].status = VCLIP;
	
		new_index++;
		if (new_index >= MAX_POINTS)
		{
			new_index--;
			logerror("*** Warning! Vector list overflow!\n");
		}
	}
	
	
	/*
	 * Set the clipping area
	 */
        public static class struct_vector_set_clip {
            public int x1;
            public int yy1;
            public int x2;
            public int y2;
        }
        
	public static struct_vector_set_clip vector_set_clip (struct_vector_set_clip _param)
	{
		int tmp;
	
		/* failsafe */
		if ((_param.x1 >= _param.x2) || (_param.yy1 >= _param.y2))
		{
			logerror("Error in clipping parameters.\n");
			xmin = 0;
			ymin = 0;
			xmax = vecwidth;
			ymax = vecheight;
			return _param;
		}
	
		/* scale coordinates to display */
		_param.x1 = vec_mult(_param.x1<<4,vector_scale_x);
		_param.yy1 = vec_mult(_param.yy1<<4,vector_scale_y);
		_param.x2 = vec_mult(_param.x2<<4,vector_scale_x);
		_param.y2 = vec_mult(_param.y2<<4,vector_scale_y);
	
		/* fix orientation */
	
		/* don't forget to swap x1,x2, since x2 becomes the minimum */
		if ((vector_orientation & ORIENTATION_FLIP_X) != 0)
		{
			_param.x1 = ((vecwidth-1)<<16)-_param.x1;
			_param.x2 = ((vecwidth-1)<<16)-_param.x2;
			tmp = _param.x1; _param.x1 = _param.x2; _param.x2 = tmp;
		}
		/* don't forget to swap yy1,y2, since y2 becomes the minimum */
		if ((vector_orientation & ORIENTATION_FLIP_Y) != 0)
		{
			_param.yy1 = ((vecheight-1)<<16)-_param.yy1;
			_param.y2 = ((vecheight-1)<<16)-_param.y2;
			tmp = _param.yy1; _param.yy1 = _param.y2; _param.y2 = tmp;
		}
		/* swapping x/y coordinates will still have the minima in x1,yy1 */
		if (((Machine.orientation ^ vector_orientation) & ORIENTATION_SWAP_XY) != 0)
		{
			tmp = _param.x1; _param.x1 = _param.yy1; _param.yy1 = tmp;
			tmp = _param.x2; _param.x2 = _param.y2; _param.y2 = tmp;
		}
		/* don't forget to swap x1,x2, since x2 becomes the minimum */
		if ((Machine.orientation & ORIENTATION_FLIP_X) != 0)
		{
			_param.x1 = ((vecwidth-1)<<16)-_param.x1;
			_param.x2 = ((vecwidth-1)<<16)-_param.x2;
			tmp = _param.x1; _param.x1 = _param.x2; _param.x2 = tmp;
		}
		/* don't forget to swap yy1,y2, since y2 becomes the minimum */
		if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0)
		{
			_param.yy1 = ((vecheight-1)<<16)-_param.yy1;
			_param.y2 = ((vecheight-1)<<16)-_param.y2;
			tmp = _param.yy1; _param.yy1 = _param.y2; _param.y2 = tmp;
		}
	
		xmin = _param.x1 >> 16;
		ymin = _param.yy1 >> 16;
		xmax = _param.x2 >> 16;
		ymax = _param.y2 >> 16;
	
		/* Make it foolproof by trapping rounding errors */
		if (xmin < 0) xmin = 0;
		if (ymin < 0) ymin = 0;
		if (xmax > vecwidth) xmax = vecwidth;
		if (ymax > vecheight) ymax = vecheight;
                
                return _param;
	}
	
	
	/*
	 * The vector CPU creates a new display list. We save the old display list,
	 * but only once per refresh.
	 */
	public static void vector_clear_list ()
	{
		point[] tmp;
	
		if (vector_runs == 0)
		{
			old_index = new_index;
			tmp = old_list; old_list = new_list; new_list = tmp;
		}
	
		new_index = 0;
		vector_runs++;
	}
	
	
	/*
	 * By comparing with the last drawn list, we can prevent that identical
	 * vectors are marked dirty which appeared at the same list index in the
	 * previous frame. BW 19980307
	 */
	static void clever_mark_dirty ()
	{
		int i, j, min_index, last_match = 0;
		int coords;
		//point[] _new, old;
		point newclip, oldclip;
		int clips_match = 1;
                
                int oldPos = 0;
                int newPos = 0;
	
		if (old_index < new_index)
			min_index = old_index;
		else
			min_index = new_index;
	
		/* Reset the active clips to invalid values */
		//memset (&newclip, 0, sizeof (newclip));
                newclip = new point();
		//memset (&oldclip, 0, sizeof (oldclip));
                oldclip = new point();
	
		/* Mark vectors which are not the same in both lists as dirty */
		//_new = new_list;
		//old = old_list;
	
		for (i = min_index; i > 0; i--, oldPos++, newPos++)
		{
			/* If this is a clip, we need to determine if the clip regions still match */
			if (old_list[oldPos].status == VCLIP || new_list[newPos].status == VCLIP)
			{
				if (old_list[oldPos].status == VCLIP)
					oldclip = old_list[oldPos];
				if (new_list[newPos].status == VCLIP)
					newclip = new_list[newPos];
				clips_match = (newclip.x == oldclip.x) && (newclip.y == oldclip.y) && (newclip.arg1 == oldclip.arg1) && (newclip.arg2 == oldclip.arg2)?1:0;
				if (clips_match == 0)
					last_match = 0;
	
				/* fall through to erase the old line if this is not a clip */
				if (old_list[oldPos].status == VCLIP)
					continue;
			}
	
			/* If the clips match and the vectors match, update */
			else if (
                                (clips_match!=0) && 
                                (new_list[newPos].x == old_list[oldPos].x) && 
                                (new_list[newPos].y == old_list[oldPos].y) &&
				(new_list[newPos].col == old_list[oldPos].col) && (new_list[newPos].intensity == old_list[oldPos].intensity))
			{
				if (last_match != 0)
				{
					new_list[newPos].status = VCLEAN;
					continue;
				}
				last_match = 1;
			}
	
			/* If nothing matches, remember it */
			else
				last_match = 0;
	
			/* mark the pixels of the old vector dirty */
			coords = pixel[old_list[oldPos].arg1];
			for (j = (old_list[oldPos].arg2 - old_list[oldPos].arg1); j > 0; j--)
			{
				osd_mark_vector_dirty (coords >> 16, coords & 0x0000ffff);
				coords++;
			}
		}
	
		/* all old vector with index greater new_index are dirty */
		/* old = &old_list[min_index] here! */
		for (i = (old_index-min_index); i > 0; i--, oldPos++)
		{
			/* skip old clips */
			if (old_list[oldPos].status == VCLIP)
				continue;
	
			/* mark the pixels of the old vector dirty */
			coords = pixel[old_list[oldPos].arg1];
			for (j = (old_list[oldPos].arg2 - old_list[oldPos].arg1); j > 0; j--)
			{
				osd_mark_vector_dirty (coords >> 16, coords & 0x0000ffff);
				coords++;
			}
		}
	}
	
	public static VhUpdatePtr vector_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int i;
		int temp_x, temp_y;
		//point[] _new;
                int newPos = 0;
	
	
		if (full_refresh != 0)
			fillbitmap(bitmap,0,null);
	
	
		/* copy parameters */
		vecbitmap = bitmap;
		vecwidth  = bitmap.width;
		vecheight = bitmap.height;
	
		/* setup scaling */
		temp_x = (1<<(44-vecshift)) / (Machine.visible_area.max_x - Machine.visible_area.min_x);
		temp_y = (1<<(44-vecshift)) / (Machine.visible_area.max_y - Machine.visible_area.min_y);
	
		if (((Machine.orientation ^ vector_orientation) & ORIENTATION_SWAP_XY) != 0)
		{
			vector_scale_x = temp_x * vecheight;
			vector_scale_y = temp_y * vecwidth;
		}
		else
		{
			vector_scale_x = temp_x * vecwidth;
			vector_scale_y = temp_y * vecheight;
		}
		/* reset clipping area */
		xmin = 0; xmax = vecwidth; ymin = 0; ymax = vecheight;
	
		/* next call to vector_clear_list() is allowed to swap the lists */
		vector_runs = 0;
	
		/* mark pixels which are not idential in newlist and oldlist dirty */
		/* the old pixels which get removed are marked dirty immediately,  */
		/* new pixels are recognized by setting new.dirty                 */
		clever_mark_dirty();
	
		/* clear ALL pixels in the hidden map */
		vector_clear_pixels();
	
		/* Draw ALL lines into the hidden map. Mark only those lines with */
		/* new.dirty = 1 as dirty. Remember the pixel start/end indices  */
		//_new = new_list;
		for (i = 0; i < new_index; i++)
		{
			if (new_list[newPos].status == VCLIP){
				struct_vector_set_clip _param = new struct_vector_set_clip();
                                _param.x1 = new_list[newPos].x;
                                _param.yy1 = new_list[newPos].y;
                                _param.x2 = new_list[newPos].arg1;
                                _param.y2 = new_list[newPos].arg2;
                                
                                _param = vector_set_clip ( _param );
                                
                                /*new_list[newPos].x = _param.x1;
                                new_list[newPos].y = _param.yy1;
                                new_list[newPos].arg1 = _param.x2;
                                new_list[newPos].arg2 = _param.y2;*/
			
                        } 
                        else
			{
				new_list[newPos].arg1 = p_index;
                                
                                struct_draw_to _param = new struct_draw_to();
                                _param.x2 = new_list[newPos].x;
                                _param.y2 = new_list[newPos].y;
                                _param.col = new_list[newPos].col;
                                _param.intensity = Tgamma[new_list[newPos].intensity];
                                _param.dirty = new_list[newPos].status;
                                
				_param = vector_draw_to ( _param );
                                
                                //if ((_param.intensity!=0)){
                                new_list[newPos].x = _param.x2;
                                new_list[newPos].y = _param.y2;
                                new_list[newPos].col = _param.col;
                                Tgamma[new_list[newPos].intensity] = _param.intensity;
                                new_list[newPos].status = _param.dirty;
	
				new_list[newPos].arg2 = p_index;
                                
			}
			newPos++;
		}
	} };
	
	//#endif /* if !(defined xgl) && !(defined xfx) && !(defined svgafx) */
        
        public static void osd_mark_vector_dirty(int x, int y){
            dirty_new[(y)/16 * DIRTY_H + (x)/16] = 1;
        };
}
