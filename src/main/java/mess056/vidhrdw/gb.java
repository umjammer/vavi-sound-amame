/***************************************************************************

  gb.c

  Video file to handle emulation of the Nintendo GameBoy.

  Original code                               Carsten Sorensen   1998
  Mess modifications, bug fixes and speedups  Hans de Goede      1998

***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mess056.machine.gb.*;
import static mess056.machine.gbH.*;

public class gb
{
    static char[] /*UINT8*/ bg_zbuf = new char[160];

    public static void gb_update_sprites ()
    {
                mame_bitmap bitmap = Machine.scrbitmap;
		int height, tilemask, line;
                UBytePtr oam = new UBytePtr();
		int i, yindex;
	
		if ((LCDCONT() & 0x04) != 0)
		{
			height = 16;
			tilemask = 0xFE;
		}
		else
		{
			height = 8;
			tilemask = 0xFF;
		}
	
		yindex = CURLINE();
		line = CURLINE() + 16;
	
		oam = new UBytePtr(gb_ram, OAM + 39 * 4);
		for (i = 39; i >= 0; i--)
		{
			/* if sprite is on current line && x-coordinate && x-coordinate is < 168 */
			if (line >= oam.read(0) && line < (oam.read(0) + height) && oam.read(1)!=0 && oam.read(1) < 168)
			{
				int data;
                                char[] spal;
				int bit;
				int xindex;
	
	//			spal = (oam[3] & 0x10) ? gb_spal1 : gb_spal0;
				spal = gb_bpal;
				xindex = oam.read(1) - 8;
				if ((oam.read(3) & 0x40) != 0)		   /* flip y ? */
				{
					data = gb_ram.read(VRAM + (oam.read(2) & tilemask) * 16 + (height - 1 - line + oam.read(0)) * 2);
				}
				else
				{
					data = gb_ram.read(VRAM + (oam.read(2) & tilemask) * 16 + (line - oam.read(0)) * 2);
				}
/*TODO*///	#ifndef LSB_FIRST
				data = (data << 8) | (data >> 8);
/*TODO*///	#endif
	
				switch (oam.read(3) & 0xA0)
				{
				case 0xA0:
					for (bit = 0; bit < 8; bit++, xindex++)
					{
						int colour = ((data & 0x0100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
						if (colour!=0 && bg_zbuf[xindex]==0)
							plot_pixel.handler(bitmap, xindex, yindex, spal[colour]);
						data >>= 1;
					}
					break;
				case 0x20:				   /* priority is not set (overlaps bgnd & wnd, flip x) */
					for (bit = 0; bit < 8; bit++, xindex++)
					{
						int colour = ((data & 0x0100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
	                    if (colour != 0)
							plot_pixel.handler(bitmap, xindex, yindex, spal[colour]);
						data >>= 1;
					}
					break;
				case 0x80:				   /* priority is set (behind bgnd & wnd, don't flip x) */
					for (bit = 0; bit < 8; bit++, xindex++)
					{
						int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
						if (colour!=0 && bg_zbuf[xindex]==0)
							plot_pixel.handler(bitmap, xindex, yindex, spal[colour]);
						data <<= 1;
					}
					break;
				case 0x00:				   /* priority is not set (overlaps bgnd & wnd, don't flip x) */
					for (bit = 0; bit < 8; bit++, xindex++)
					{
						int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
						if (colour != 0)
							plot_pixel.handler(bitmap, xindex, yindex, spal[colour]);
						data <<= 1;
					}
					break;
				}
			}
			oam.dec( 4 );
		}
    }

    public static class layer_struct
    {
            public int  enabled;
            public UShortPtr bg_tiles;
            public UBytePtr  bg_map;
            public int  xindex;
            public int  xshift;
            public int  xstart;
            public int  xend;
            /* GBC specific */
            public UShortPtr[] gbc_tiles = new UShortPtr[2];
            public UBytePtr  gbc_map;
            public int  bgline;
    };
    
    public static abstract interface _refresh_scanline {

        public abstract void handler();
    }
    
    public static _refresh_scanline refresh_scanline;

    public static _refresh_scanline gb_refresh_scanline = new _refresh_scanline() {
        public void handler() {
        
                mame_bitmap bitmap = Machine.scrbitmap;
                int _zbuff=0;
		char[] zbuf = bg_zbuf;
		int l = 0, yindex = CURLINE();
	
		/* layer info layer[0]=background, layer[1]=window */
		layer_struct[] layer = new layer_struct[2];
                for (int _i=0 ; _i<2 ; _i++)
                    layer[_i] = new layer_struct();
	
		/* if background or screen disabled clear line */
		if ((LCDCONT() & 0x81) != 0x81)
		{
			rectangle r = Machine.visible_area;
			r.min_y = r.max_y = yindex;
			fillbitmap(bitmap, Machine.pens[0], r);
		}
	
		/* if lcd disabled return */
		if ((LCDCONT() & 0x80)==0)
			return;
	
		/* Window is enabled if the hardware says so AND the current scanline is
		 * within the window AND the window X coordinate is <=166 */
		layer[1].enabled = ((LCDCONT() & 0x20)!=0 && CURLINE() >= WNDPOSY() && WNDPOSX() <= 166) ? 1 : 0;
	
		/* BG is enabled if the hardware says so AND (window_off OR (window_on
		 * AND window's X position is >=7 ) ) */
		layer[0].enabled = ((LCDCONT() & 0x01)!=0 && ((layer[1].enabled==0) || (layer[1].enabled!=0 && WNDPOSX() >= 7))) ? 1 : 0;
	
		if (layer[0].enabled != 0)
		{
			int bgline;
	
			bgline = (SCROLLY() + CURLINE()) & 0xFF;
	
			layer[0].bg_map = new UBytePtr(gb_bgdtab);
			layer[0].bg_map.inc( (bgline << 2) & 0x3E0 );
			layer[0].bg_tiles = new UShortPtr(gb_chrgen, (bgline & 7));
			layer[0].xindex = SCROLLX() >> 3;
			layer[0].xshift = SCROLLX() & 7;
			layer[0].xend = 160;
		}
	
		if (layer[1].enabled != 0)
		{
			int bgline, xpos;
	
			bgline = (CURLINE() - WNDPOSY()) & 0xFF;
			xpos = WNDPOSX() - 7;
	
			layer[1].bg_map = new UBytePtr(gb_wndtab);
			layer[1].bg_map.inc( (bgline << 2) & 0x3E0 );
			layer[1].bg_tiles = new UShortPtr(gb_chrgen, (bgline & 7));
			layer[1].xindex = 0;
			layer[1].xshift = 0;
			if (xpos < 0)
				xpos = 0;
			layer[0].xend = xpos;
			layer[1].xend = 160 - xpos;
		}
	
		while (l < 2)
		{
			/*
			 * BG display on
			 */
			UBytePtr map;
                        int xidx, bit, i;
			UShortPtr tiles;
                        int data;
			int xindex;
	
			if (layer[l].enabled==0)
			{
				l++;
				continue;
			}
	
			map = new UBytePtr(layer[l].bg_map);
			tiles = new UShortPtr(layer[l].bg_tiles);
			xidx = layer[l].xindex;
			bit = layer[l].xshift;
			i = layer[l].xend;
	
			data =  (tiles.read((map.read(xidx) ^ gb_tile_no_mod) * 8) << bit);
/*TODO*///	#ifndef LSB_FIRST
			data = (data << 8) | (data >> 8);
/*TODO*///	#endif
			xindex = 0;
			while (i != 0)
			{
				while ((bit < 8) && i!=0)
				{
					int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
					plot_pixel.handler(bitmap, xindex, yindex, gb_bpal[colour]);
					xindex++;
					zbuf[_zbuff++] = (char) colour;
					data <<= 1;
					bit++;
					i--;
				}
				xidx = (xidx + 1) & 31;
				bit = 0;
				data = tiles.read((map.read(xidx) ^ gb_tile_no_mod) * 8);
			}
			l++;
		}
	
		if ((LCDCONT() & 0x02) != 0)
			gb_update_sprites();
        }
    };

    /* --- Super Gameboy Specific --- */

    public static void sgb_update_sprites ()
    {
            mame_bitmap bitmap = Machine.scrbitmap;
            int height, tilemask, line, pal;
            UBytePtr oam;
            int i, yindex;

            if ((LCDCONT() & 0x04) != 0)
            {
                    height = 16;
                    tilemask = 0xFE;
            }
            else
            {
                    height = 8;
                    tilemask = 0xFF;
            }

            /* Offset to center of screen */
            yindex = CURLINE() + SGB_YOFFSET;
            line = CURLINE() + 16;

            oam = new UBytePtr(gb_ram, OAM + 39 * 4);
            for (i = 39; i >= 0; i--)
            {
                    /* if sprite is on current line && x-coordinate && x-coordinate is < 168 */
                    if (line >= oam.read(0) && line < (oam.read(0) + height) && oam.read(1)!=0 && oam.read(1) < 168)
                    {
                            int data;
                            int bit;
                            int[] spal;
                            int xindex;

                            spal = (oam.read(3) & 0x10)!=0 ? gb_spal1 : gb_spal0;
                            xindex = oam.read(1) - 8;
                            if ((oam.read(3) & 0x40) != 0)		   /* flip y ? */
                            {
                                    data = gb_ram.read(VRAM + (oam.read(2) & tilemask) * 16 + (height - 1 - line + oam.read(0)) ) & 0xffff;
                            }
                            else
                            {
                                    data = gb_ram.read(VRAM + (oam.read(2) & tilemask) * 16 + (line - oam.read(0)) ) & 0xffff;
                            }
/*TODO*///    #ifndef LSB_FIRST
                            data = (data << 8) | (data >> 8);
/*TODO*///    #endif

                            /* Find the palette to use */
                            pal = sgb_pal_map[(xindex >> 3)][((yindex - SGB_YOFFSET) >> 3)] << 2;

                            /* Offset to center of screen */
                            xindex += SGB_XOFFSET;

                            switch (oam.read(3) & 0xA0)
                            {
                            case 0xA0:
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x0100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
                                            if ((xindex >= SGB_XOFFSET && xindex <= SGB_XOFFSET + 160) && colour!=0 && bg_zbuf[xindex - SGB_XOFFSET]==0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + spal[colour]));
                                            data >>= 1;
                                    }
                                    break;
                            case 0x20:				   /* priority is not set (overlaps bgnd & wnd, flip x) */
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x0100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
                                            if ((xindex >= SGB_XOFFSET && xindex <= SGB_XOFFSET + 160) && colour!=0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + spal[colour]));
                                            data >>= 1;
                                    }
                                    break;
                            case 0x80:				   /* priority is set (behind bgnd & wnd, don't flip x) */
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
                                            if ((xindex >= SGB_XOFFSET && xindex <= SGB_XOFFSET + 160) && colour!=0 && bg_zbuf[xindex - SGB_XOFFSET]==0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + spal[colour]));
                                            data <<= 1;
                                    }
                                    break;
                            case 0x00:				   /* priority is not set (overlaps bgnd & wnd, don't flip x) */
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
                                            if ((xindex >= SGB_XOFFSET && xindex <= SGB_XOFFSET + 160) && colour!=0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + spal[colour]));
                                            data <<= 1;
                                    }
                                    break;
                            }
                    }
                    oam.dec( 4 );
            }
    }

    public static _refresh_scanline sgb_refresh_scanline = new _refresh_scanline() {
        public void handler() {
        
            mame_bitmap bitmap = Machine.scrbitmap;
            UBytePtr zbuf = new UBytePtr(bg_zbuf);
            int l = 0, yindex = CURLINE();

            /* layer info layer[0]=background, layer[1]=window */
            layer_struct[] layer = new layer_struct[2];
            for (int _i=0 ; _i<2 ; _i++)
                layer[_i] = new layer_struct();

            /* Handle SGB mask */
            switch( sgb_window_mask )
            {
                    case 1:	/* Freeze screen */
                            return;
                    case 2:	/* Blank screen (black) */
                            {
                                    rectangle r = new rectangle(Machine.visible_area);
                                    r.min_x = SGB_XOFFSET;
                                    r.max_x -= SGB_XOFFSET;
                                    r.min_y = SGB_YOFFSET;
                                    r.max_y -= SGB_YOFFSET;
                                    fillbitmap( bitmap, Machine.pens[0], r );
                            } return;
                    case 3:	/* Blank screen (white - or should it be color 0?) */
                            {
                                    rectangle r = new rectangle(Machine.visible_area);
                                    r.min_x = SGB_XOFFSET;
                                    r.max_x -= SGB_XOFFSET;
                                    r.min_y = SGB_YOFFSET;
                                    r.max_y -= SGB_YOFFSET;
                                    fillbitmap( bitmap, Machine.pens[32767], r );
                            } return;
            }

            /* Draw the "border" if we're on the first line */
            if( CURLINE() == 0 )
            {
                    sgb_refresh_border();
            }

            /* if background or screen disabled clear line */
            if ((LCDCONT() & 0x81) != 0x81)
            {
                    rectangle r = new rectangle(Machine.visible_area);
                    r.min_x = SGB_XOFFSET;
                    r.max_x -= SGB_XOFFSET;
                    r.min_y = r.max_y = yindex + SGB_YOFFSET;
                    fillbitmap(bitmap, Machine.pens[0], r);
            }

            /* if lcd disabled return */
            if ((LCDCONT() & 0x80) == 0)
                    return;

            /* Window is enabled if the hardware says so AND the current scanline is
             * within the window AND the window X coordinate is <=166 */
            layer[1].enabled = ((LCDCONT() & 0x20)!=0 && CURLINE() >= WNDPOSY() && WNDPOSX() <= 166) ? 1 : 0;

            /* BG is enabled if the hardware says so AND (window_off OR (window_on
             * AND window's X position is >=7 ) ) */
            layer[0].enabled = ((LCDCONT() & 0x01)!=0 && ((layer[1].enabled)==0 || (layer[1].enabled!=0 && WNDPOSX() >= 7))) ? 1 : 0;

            if (layer[0].enabled != 0)
            {
                    int bgline;

                    bgline = (SCROLLY() + CURLINE()) & 0xFF;

                    layer[0].bg_map = new UBytePtr(gb_bgdtab);
                    layer[0].bg_map.inc( (bgline << 2) & 0x3E0 );
                    layer[0].bg_tiles = new UShortPtr(gb_chrgen, (bgline & 7));
                    layer[0].xindex = SCROLLX() >> 3;
                    layer[0].xshift = SCROLLX() & 7;
                    layer[0].xstart = 0;
                    layer[0].xend = 160;
            }

            if (layer[1].enabled != 0)
            {
                    int bgline, xpos;

                    bgline = (CURLINE() - WNDPOSY()) & 0xFF;
                    /* Window X position is offset by 7 so we'll need to adust */
                    xpos = WNDPOSX() - 7;
                    if (xpos < 0)
                            xpos = 0;

                    layer[1].bg_map = new UBytePtr(gb_wndtab);
                    layer[1].bg_map.inc( (bgline << 2) & 0x3E0 );
                    layer[1].bg_tiles = new UShortPtr(gb_chrgen, (bgline & 7));
                    layer[1].xindex = 0;
                    layer[1].xshift = 0;
                    layer[1].xstart = xpos;
                    layer[1].xend = 160 - xpos;
                    layer[0].xend = xpos;
            }

            while (l < 2)
            {
                    /*
                     * BG display on
                     */
                    UBytePtr map;
                    int xidx, bit, i, pal;
                    UShortPtr tiles;
                    int data;
                    int xindex;

                    if (layer[l].enabled == 0)
                    {
                            l++;
                            continue;
                    }

                    map = layer[l].bg_map;
                    tiles = layer[l].bg_tiles;
                    xidx = layer[l].xindex;
                    bit = layer[l].xshift;
                    i = layer[l].xend;

                    data = (tiles.read((map.read(xidx) ^ gb_tile_no_mod) * 8) << bit);
/*TODO*///    #ifndef LSB_FIRST
                    data = (data << 8) | (data >> 8);
/*TODO*///    #endif
                    xindex = layer[l].xstart;

                    /* Figure out which palette we're using */
                    pal = sgb_pal_map[(xindex >> 3)][(yindex >> 3)] << 2;

                    while (i != 0)
                    {
                            while ((bit < 8) && i!=0)
                            {
                                    int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
                                    plot_pixel.handler(bitmap, xindex + SGB_XOFFSET, yindex + SGB_YOFFSET, Machine.remapped_colortable.read(pal + gb_bpal[colour]));
                                    xindex++;
                                    zbuf.writeinc(colour);
                                    data <<= 1;
                                    bit++;
                                    i--;
                            }
                            xidx = (xidx + 1) & 31;
                            pal = sgb_pal_map[(xindex >> 3)][(yindex >> 3)] << 2;
                            bit = 0;
                            data = tiles.read((map.read(xidx) ^ gb_tile_no_mod) * 8);
                    }
                    l++;
            }

            if ((LCDCONT() & 0x02) != 0)
                    sgb_update_sprites();
        }
    };

    public static void sgb_refresh_border()
    {
            UShortPtr tiles, tiles2;
            int data, data2;
            UShortPtr map;
            int yidx, xidx, xindex;
            int pal, i;
            mame_bitmap bitmap = Machine.scrbitmap;

            map = new UShortPtr(sgb_tile_map, - 32);

            for( yidx = 0; yidx < 224; yidx++ )
            {
                    xindex = 0;
                    map.inc( (yidx % 8)!=0 ? 0 : 32 );
                    for( xidx = 0; xidx < 32; xidx++ )
                    {
                            if(( map.read(xidx) & 0x8000 ) != 0) /* Vertical flip */
                                    tiles = new UShortPtr(sgb_tile_data, (7 - (yidx % 8)));
                            else /* No vertical flip */
                                    tiles = new UShortPtr(sgb_tile_data, (yidx % 8));
                            /*TODO*///tiles2 = tiles + 8;
                            tiles2 = new UShortPtr(tiles, 8);

                            pal = ((map.read(xidx) & 0x1C00) >> 10) * 16;
                            if( sgb_hack != 0 ) /* A few games do weird stuff */
                            {
                                    int tileno = map.read(xidx) & 0xFF;
                                    if( tileno >= 128 ) tileno = ((64 + tileno) % 128) + 128;
                                    else tileno = (64 + tileno) % 128;
                                    data = tiles.read(tileno * 16);
                                    data2 = tiles2.read(tileno * 16);
                            }
                            else
                            {
                                    data = tiles.read((map.read(xidx) & 0xFF) * 16);
                                    data2 = tiles2.read((map.read(xidx) & 0xFF) * 16);
                            }

                            for( i = 0; i < 8; i++ )
                            {
                                    int colour;
                                    if( (map.read(xidx) & 0x4000) != 0 ) /* Horizontal flip */
                                    {
                                            colour = ((data  & 0x0001)!=0 ? 1 : 0) |
                                                             ((data  & 0x0100)!=0 ? 2 : 0) |
                                                             ((data2 & 0x0001)!=0 ? 4 : 0) |
                                                             ((data2 & 0x0100)!=0 ? 8 : 0);
                                            data >>= 1;
                                            data2 >>= 1;
                                    }
                                    else /* No horizontal flip */
                                    {
                                            colour = ((data  & 0x0080)!= 0 ? 1 : 0) |
                                                             ((data  & 0x8000)!= 0 ? 2 : 0) |
                                                             ((data2 & 0x0080)!= 0 ? 4 : 0) |
                                                             ((data2 & 0x8000)!= 0 ? 8 : 0);
                                            data <<= 1;
                                            data2 <<= 1;
                                    }
                                    if( colour != 0 ) /* Colour 0 is transparent */
                                            plot_pixel.handler(bitmap, xindex, yidx, Machine.remapped_colortable.read(pal + colour));
                                    xindex++;
                            }
                    }
            }
    }

    /* --- Gameboy Color Specific --- */

    public static void gbc_update_sprites ()
    {
            mame_bitmap bitmap = Machine.scrbitmap;
            int height, tilemask, line;
            UBytePtr oam;
            int i, xindex, yindex;

            if ((LCDCONT() & 0x04) != 0)
            {
                    height = 16;
                    tilemask = 0xFE;
            }
            else
            {
                    height = 8;
                    tilemask = 0xFF;
            }

            yindex = CURLINE();
            line = CURLINE() + 16;

            oam = new UBytePtr(gb_ram, OAM + 39 * 4);
            for (i = 39; i >= 0; i--)
            {
                    /* if sprite is on current line && x-coordinate && x-coordinate is < 168 */
                    if (line >= oam.read(0) && line < (oam.read(0) + height) && oam.read(1)!=0 && oam.read(1) < 168)
                    {
                            int data;
                            int bit, pal;

                            /* Handle mono mode for GB games */
                            if( gbc_mode == GBC_MODE_MONO )
                                    pal = (oam.read(3) & 0x10)!=0 ? 8 : 4;
                            else
                                    pal = GBC_PAL_OBJ_OFFSET + (oam.read(3) & 0x7) * 4;

                            xindex = oam.read(1) - 8;
                            if ((oam.read(3) & 0x40) != 0)		   /* flip y ? */
                            {
                                    data = GBC_VRAMMap[(oam.read(3) & 0x8)>>3].read((oam.read(2) & tilemask) * 16 + (height - 1 - line + oam.read(0) )) & 0xffff;
                            }
                            else
                            {
                                    data = GBC_VRAMMap[(oam.read(3) & 0x8)>>3].read((oam.read(2) & tilemask) * 16 + (line - oam.read(0) )) & 0xffff;
                            }
/*TODO*///    #ifndef LSB_FIRST
                            data = (data << 8) | (data >> 8);
/*TODO*///    #endif

                            switch (oam.read(3) & 0xA0)
                            {
                            case 0xA0:
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x0100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
                                            if (colour!=0 && bg_zbuf[xindex]==0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + colour));
                                            data >>= 1;
                                    }
                                    break;
                            case 0x20:				   /* priority is not set (overlaps bgnd & wnd, flip x) */
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x0100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
                                            if (colour != 0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + colour));
                                            data >>= 1;
                                    }
                                    break;
                            case 0x80:				   /* priority is set (behind bgnd & wnd, don't flip x) */
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
                                            if (colour!=0 && bg_zbuf[xindex]==0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + colour));
                                            data <<= 1;
                                    }
                                    break;
                            case 0x00:				   /* priority is not set (overlaps bgnd & wnd, don't flip x) */
                                    for (bit = 0; bit < 8; bit++, xindex++)
                                    {
                                            int colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
                                            if (colour != 0)
                                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read(pal + colour));
                                            data <<= 1;
                                    }
                                    break;
                            }
                    }
                    oam.dec( 4 );
            }
    }

    public static _refresh_scanline gbc_refresh_scanline = new _refresh_scanline() {
        public void handler() {
        
            mame_bitmap bitmap = Machine.scrbitmap;
            UBytePtr zbuf = new UBytePtr(bg_zbuf);
            int l = 0, yindex = CURLINE();

            /* layer info layer[0]=background, layer[1]=window */
            layer_struct[] layer = new layer_struct[2];
            for (int _i=0 ; _i<2 ; _i++)
                layer[_i] = new layer_struct();

            /* if background or screen disabled clear line */
            if ((LCDCONT() & 0x81) != 0x81)
            {
                    rectangle r = new rectangle(Machine.visible_area);
                    r.min_y = r.max_y = yindex;
                    fillbitmap(bitmap, Machine.pens[0], r);
            }

            /* if lcd disabled return */
            if ((LCDCONT() & 0x80) == 0)
                    return;

            /* Window is enabled if the hardware says so AND the current scanline is
             * within the window AND the window X coordinate is <=166 */
            layer[1].enabled = ((LCDCONT() & 0x20)!=0 && CURLINE() >= WNDPOSY() && WNDPOSX() <= 166) ? 1 : 0;

            /* BG is enabled if the hardware says so AND (window_off OR (window_on
             * AND window's X position is >=7 ) ) */
            layer[0].enabled = ((LCDCONT() & 0x01)!=0 && ((layer[1].enabled==0) || (layer[1].enabled!=0 && WNDPOSX() >= 7))) ? 1 : 0;

            if (layer[0].enabled != 0)
            {
                    int bgline;

                    bgline = (SCROLLY() + CURLINE()) & 0xFF;

                    layer[0].bgline = bgline;
                    layer[0].bg_map = new UBytePtr(gb_bgdtab);
                    layer[0].bg_map.inc( (bgline << 2) & 0x3E0 );
                    layer[0].gbc_map = new UBytePtr(gbc_bgdtab);
                    layer[0].gbc_map.inc( (bgline << 2) & 0x3E0 );
                    layer[0].gbc_tiles[0] = new UShortPtr(gb_chrgen, (bgline & 7));
                    layer[0].gbc_tiles[1] = new UShortPtr(gbc_chrgen, (bgline & 7));
                    layer[0].xindex = SCROLLX() >> 3;
                    layer[0].xshift = SCROLLX() & 7;
                    layer[0].xstart = 0;
                    layer[0].xend = 160;
            }

            if (layer[1].enabled != 0)
            {
                    int bgline, xpos;

                    bgline = (CURLINE() - WNDPOSY()) & 0xFF;
                    /* Window X position is offset by 7 so we'll need to adust */
                    xpos = WNDPOSX() - 7;
                    if (xpos < 0)
                            xpos = 0;

                    layer[1].bgline = bgline;
                    layer[1].bg_map = new UBytePtr(gb_wndtab);
                    layer[1].bg_map.inc((bgline << 2) & 0x3E0);
                    layer[1].gbc_map = new UBytePtr(gbc_wndtab);
                    layer[1].gbc_map.inc( (bgline << 2) & 0x3E0 );
                    layer[1].gbc_tiles[0] = new UShortPtr(gb_chrgen, (bgline & 7));
                    layer[1].gbc_tiles[1] = new UShortPtr(gbc_chrgen, (bgline & 7));
                    layer[1].xindex = 0;
                    layer[1].xshift = 0;
                    layer[1].xstart = xpos;
                    layer[1].xend = 160 - xpos;
                    layer[0].xend = xpos;
            }

            while (l < 2)
            {
                    /*
                     * BG display on
                     */
                    UBytePtr map, gbcmap;
                    int xidx, bit, i;
                    UShortPtr tiles;
                    int data;
                    int xindex;

                    if (layer[l].enabled == 0)
                    {
                            l++;
                            continue;
                    }

                    map = layer[l].bg_map;
                    gbcmap = layer[l].gbc_map;
                    xidx = layer[l].xindex;
                    bit = layer[l].xshift;
                    i = layer[l].xend;

                    tiles = layer[l].gbc_tiles[(gbcmap.read(xidx) & 0x8) >> 3];
                    if(( (gbcmap.read(xidx) & 0x40) >> 6 ) != 0)/* vertical flip */
                            tiles.dec( ((layer[l].bgline & 7) << 1) - 7 );
                    data = (tiles.read((map.read(xidx) ^ gb_tile_no_mod) * 8) << bit) & 0xffff;
/*TODO*///    #ifndef LSB_FIRST
                    data = (data << 8) | (data >> 8);
/*TODO*///    #endif

                    xindex = layer[l].xstart;
                    while (i != 0)
                    {
                            while ((bit < 8) && i!=0)
                            {
                                    int colour;
                                    if( ((gbcmap.read(xidx) & 0x20) >> 5) != 0 ) /* horizontal flip */
                                    {
                                            colour = ((data & 0x100)!=0 ? 2 : 0) | ((data & 0x0001)!=0 ? 1 : 0);
                                            data >>= 1;
                                    }
                                    else /* no horizontal flip */
                                    {
                                            colour = ((data & 0x8000)!=0 ? 2 : 0) | ((data & 0x0080)!=0 ? 1 : 0);
                                            data <<= 1;
                                    }
                                    plot_pixel.handler(bitmap, xindex, yindex, Machine.remapped_colortable.read((((gbcmap.read(xidx) & 0x7) * 4) + colour)));
                                    xindex++;
                                    zbuf.writeinc(colour);
                                    bit++;
                                    i--;
                            }
                            xidx = (xidx + 1) & 31;
                            bit = 0;
                            tiles = new UShortPtr(layer[l].gbc_tiles[(gbcmap.read(xidx) & 0x8) >> 3]);
                            if(( (gbcmap.read(xidx) & 0x40) >> 6 ) != 0) /* vertical flip */
                                    tiles.dec( ((layer[l].bgline & 7) << 1) - 7 );
                            data = (tiles.read((map.read(xidx) ^ gb_tile_no_mod) * 8));
                    }
                    l++;
            }

            if ((LCDCONT() & 0x02) != 0)
                    gbc_update_sprites();
        }
    };
	
}

