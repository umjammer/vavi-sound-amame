/***************************************************************************

  vidhrdw/nes.c

  Routines to control the unique NES video hardware/PPU.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.logerror;
import static arcadeflex056.video.osd_skip_this_frame;
import static common.libc.cstring.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.mame.*;
import static mame056.usrintrf.*;
import static mame056.vidhrdw.generic.*;
import static mess056.machine.nes.*;
import static mess056.machine.nes_mmcH.*;
import static mess056.includes.nesH.*;

public class nes
{
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	//#define BACKGROUND_OFF
/*TODO*///	//#define LOG_VIDEO
/*TODO*///	//#define LOG_COLOR
/*TODO*///	//#define DIRTY_BUFFERS
/*TODO*///	
    public static final int VIDEORAM_SIZE = 0x4000;
    public static final int SPRITERAM_SIZE = 0x100;
    public static final int VRAM_SIZE = 0x3c0;

    public static int[] nes_vram = new int[8];
    /* Keep track of 8 .5k vram pages to speed things up */
    static int[] nes_vram_sprite = new int[8];
    /* Used only by mmc5 for now */
    public static int[] dirtychar = new int[0x200];

    static int gfx_bank;

    static char[] nes_palette = new char[3 * 64];

    static char[] line_priority = new char[0x100];
    /*TODO*///	
    /* Changed at runtime */
    static char nes_colortable[]
            = {
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,};

    public static int colortable_mono[]
            = {
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,};

    public static VhConvertColorPromPtr nes_init_palette = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            /* This routine builds a palette using a transformation from */
 /* the YUV (Y, B-Y, R-Y) to the RGB color space */

 /* The NES has a 64 color palette                        */
 /* 16 colors, with 4 luminance levels for each color     */
 /* The 16 colors circle around the YUV color space,      */
 /* It also returns a fake colortable, for the menus */
            int i, j;

            double R, G, B;

            double tint = .5;
            double hue = 332.0;
            double bright_adjust = 1.0;

            double brightness[][]
                    = {
                        {0.50, 0.75, 1.0, 1.0},
                        {0.29, 0.45, 0.73, 0.9},
                        {0, 0.24, 0.47, 0.77}
                    };

            double angle[] = {0, 240, 210, 180, 150, 120, 90, 60, 30, 0, 330, 300, 270, 0, 0, 0};
            int p_ptr = 0;

            /* loop through the 4 intensities */
            for (i = 0; i < 4; i++) {
                /* loop through the 16 colors */
                for (j = 0; j < 16; j++) {
                    double sat;
                    double y;
                    double rad;

                    switch (j) {
                        case 0:
                            sat = 0;
                            y = brightness[0][i];
                            break;
                        case 13:
                            sat = 0;
                            y = brightness[2][i];
                            break;
                        case 14:
                        case 15:
                            sat = 0;
                            y = 0;
                            break;
                        default:
                            sat = tint;
                            y = brightness[1][i];
                            break;
                    }

                    rad = Math.PI * ((angle[j] + hue) / 180.0);

                    y *= bright_adjust;

                    /* Transform to RGB */
                    R = (y + sat * Math.sin(rad)) * 255.0;
                    G = (y - (27 / 53) * sat * Math.sin(rad) + (10 / 53) * sat * Math.cos(rad)) * 255.0;
                    B = (y - sat * Math.cos(rad)) * 255.0;

                    /* Clipping, in case of saturation */
                    if (R < 0) {
                        R = 0;
                    }
                    if (R > 255) {
                        R = 255;
                    }
                    if (G < 0) {
                        G = 0;
                    }
                    if (G > 255) {
                        G = 255;
                    }
                    if (B < 0) {
                        B = 0;
                    }
                    if (B > 255) {
                        B = 255;
                    }

                    /* Round, and set the value */
                    palette[p_ptr] = (char) Math.floor(R + .5);
                    nes_palette[(i * 16 + j) * 3] = (char) (palette[p_ptr] & 0xFF);
                    p_ptr++;
                    palette[p_ptr] = (char) Math.floor(G + .5);
                    nes_palette[(i * 16 + j) * 3 + 1] = (char) (palette[p_ptr] & 0xFF);
                    p_ptr++;
                    palette[p_ptr] = (char) Math.floor(B + .5);
                    nes_palette[(i * 16 + j) * 3 + 2] = (char) (palette[p_ptr] & 0xFF);
                    p_ptr++;
                }
            }

            memcpy(colortable, nes_colortable, nes_colortable.length);
        }
    };

    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr nes_vh_start = new VhStartPtr() {
        public int handler() {
            int i;

            /* We must clear the videoram on startup */
            if ((videoram = new UBytePtr(VIDEORAM_SIZE)) == null) {
                return 1;
            }

            /* We use an offscreen bitmap that's 4 times as large as the visible one */
            if ((tmpbitmap = bitmap_alloc(2 * 32 * 8, 2 * 30 * 8)) == null) {
                videoram = null;
                bitmap_free(tmpbitmap);
                return 1;
            }

            /* sprite RAM must be clear on startup */
            if ((spriteram = new UBytePtr(SPRITERAM_SIZE)) == null) {
                videoram = null;
                bitmap_free(tmpbitmap);
                return 1;
            }
            /* Mark all chars as 'clean' */
            for (i = 0; i < 0x200; i++) {
                dirtychar[i] = 0;
            }

            if (_nes.chr_chunks[0] == 0) {
                gfx_bank = 1;
            } else {
                gfx_bank = 0;
            }

            return 0;
        }
    };

    /**
     * *************************************************************************
     *
     * Stop the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStopPtr nes_vh_stop = new VhStopPtr() {
        public void handler() {
            videoram = null;
            spriteram = null;
            bitmap_free(tmpbitmap);

        }
    };

    /**
     * *************************************************************************
     *
     * Draw the current scanline
     *
     **************************************************************************
     */
    public static void nes_vh_renderscanline(int scanline) {
        //throw new UnsupportedOperationException("Not supported yet.");
        int x;
        int i;
        int start_x;
        int tile_index;
        int scroll_x_coarse;
        int scroll_y_coarse;
        int scroll_y_fine;
        int color_mask;
        int total_elements;
        IntArray paldata_1;
        //	const UBytePtr sd_1;

        if (osd_skip_this_frame() != 0) {
            /* Increment the fine y-scroll */
            u16_PPU_refresh_data = (u16_PPU_refresh_data + 0x1000) & 0xFFFF;

            /* If it's rolled, increment the coarse y-scroll */
            if ((u16_PPU_refresh_data & 0x8000) != 0) {
                int tmp;
                tmp = ((u16_PPU_refresh_data & 0x03e0) + 0x20) & 0xFFFF;
                u16_PPU_refresh_data = (u16_PPU_refresh_data & 0x7c1f) & 0xFFFF;
                /* Handle bizarro scrolling rollover at the 30th (not 32nd) vertical tile */
                if (tmp == 0x03c0) {
                    u16_PPU_refresh_data = (u16_PPU_refresh_data ^ 0x0800) & 0xFFFF;
                } else {
                    u16_PPU_refresh_data = (u16_PPU_refresh_data | (tmp & 0x03e0)) & 0xFFFF;
                }
            }
            return;
        }

        if ((PPU_Control1 & 0x01) != 0) {
            color_mask = 0xf0;
        } else {
            color_mask = 0xff;
        }

        /* Set the background color */
        memset(Machine.scrbitmap.line[scanline], Machine.pens[PPU_background_color & color_mask], 0x100);

        for (i = 0; i < 0x100; i++) {
            /* Clear the priority buffer for this line */
            line_priority[i] = 0;

            /* Set the background color */
            //		plot_pixel (Machine.scrbitmap, i, scanline, Machine.pens[PPU_background_color]);
        }

        /* Clear sprite count for this line */
        PPU_Status &= ~PPU_status_8sprites;

        /* If the background is off, don't draw it */
        if ((PPU_Control1 & PPU_c1_background) == 0) {
            //	draw_sprites:
            /* If sprites are hidden in the leftmost column, fake a priority flag to mask them */
            if ((PPU_Control1 & PPU_c1_sprites_L8) == 0) {
                for (i = 0; i < 8; i++) {
                    line_priority[i] |= 0x01;
                }
            }

            /* If sprites are on, draw them */
            if ((PPU_Control1 & PPU_c1_sprites) != 0) {
                render_sprites(scanline);
            }

            /* Does the user not want to see the top/bottom 8 lines? */
            if ((readinputport(7) & 0x01) != 0 && ((scanline < 8) || (scanline > 231))) {
                /* Clear this line if we're not drawing it */
                memset(Machine.scrbitmap.line[scanline], Machine.pens[0x3f & color_mask], 0x100);
            }

            //draw_nothing:
            /* Increment the fine y-scroll */
            u16_PPU_refresh_data = (u16_PPU_refresh_data + 0x1000) & 0xFFFF;

            /* If it's rolled, increment the coarse y-scroll */
            if ((u16_PPU_refresh_data & 0x8000) != 0) {
                int tmp;
                tmp = ((u16_PPU_refresh_data & 0x03e0) + 0x20) & 0xFFFF;
                u16_PPU_refresh_data = (u16_PPU_refresh_data & 0x7c1f) & 0xFFFF;
                /* Handle bizarro scrolling rollover at the 30th (not 32nd) vertical tile */
                if (tmp == 0x03c0) {
                    u16_PPU_refresh_data = (u16_PPU_refresh_data ^ 0x0800) & 0xFFFF;
                } else {
                    u16_PPU_refresh_data = (u16_PPU_refresh_data | (tmp & 0x03e0)) & 0xFFFF;
                }
            }
            return;
        }

        /* Determine where in the nametable to start drawing from, based on the current scanline and scroll regs */
        scroll_x_coarse = u16_PPU_refresh_data & 0x1f;
        scroll_y_coarse = ((u16_PPU_refresh_data & 0x3e0) >> 5) & 0xFF;
        scroll_y_fine = ((u16_PPU_refresh_data & 0x7000) >> 12) & 0xFF;

        start_x = (PPU_X_fine ^ 0x07) - 7;

        x = scroll_x_coarse;
        tile_index = ((u16_PPU_refresh_data & 0xc00) | 0x2000) + scroll_y_coarse * 32;

        total_elements = Machine.gfx[gfx_bank].total_elements;

        //	sd_1 = Machine.gfx[gfx_bank].gfxdata;
        if ((PPU_Control1 & 0x01) != 0) {
            paldata_1 = new IntArray(colortable_mono);
        } else {
            paldata_1 = new IntArray(Machine.gfx[0].colortable);
        }

        /* Draw the 32 or 33 tiles that make up a line */
        while (start_x < 256) {
            int color_byte;
            int color_bits;
            int pos;
            int index1;
            int page, address;
            int index2;

            index1 = tile_index + x;

            /* Figure out which byte in the color table to use */
            pos = ((index1 & 0x380) >> 4) | ((index1 & 0x1f) >> 2);

            /* TODO: this only needs calculating every 2nd tile - link it to "x" */
            page = (index1 & 0x0c00) >> 10;
            address = 0x3c0 + pos;
            color_byte = ppu_page[page].read(address);

            /* Figure out which bits in the color table to use */
            color_bits = ((index1 & 0x40) >> 4) + (index1 & 0x02);

            address = index1 & 0x3ff;
            index2 = nes_vram[(ppu_page[page].read(address) >> 6) | PPU_tile_page] + (ppu_page[page].read(address) & 0x3f);
            /*TODO*///	
/*TODO*///	#ifdef MMC5_VRAM
/*TODO*///			/* Use the extended bits if necessary */
/*TODO*///			if ((MMC5_vram_control & 0x01) != 0)
/*TODO*///			{
/*TODO*///				index2 |= (MMC5_vram[address] & 0x3f) << 8;
/*TODO*///			}
/*TODO*///	#endif
/*TODO*///	
            {
                IntArray paldata;
                UBytePtr sd;
                UBytePtr bm;
                int start;

                //			paldata = &Machine.gfx[gfx_bank].colortable[4 * (((color_byte >> color_bits) & 0x03)/* % 8*/)];
                paldata = new IntArray(paldata_1, 4 * (((color_byte >> color_bits) & 0x03)));
                bm = new UBytePtr(Machine.scrbitmap.line[scanline], start_x);
                //			sd = &Machine.gfx[gfx_bank].gfxdata[start * Machine.gfx[gfx_bank].width];
                start = (index2 % total_elements) * 8 + scroll_y_fine;
                sd = new UBytePtr(Machine.gfx[gfx_bank].gfxdata, start * 8);
                //			sd = &sd_1[start * 8];

                for (i = 0; i < 8; i++) {
                    if (start_x + i >= 0) {
                        if (sd.read(i) != 0) {
                            plot_pixel.handler(Machine.scrbitmap, start_x + i, scanline, paldata.read(sd.read(i)));
                            line_priority[start_x + i] |= 0x02;
                        }
                    }
                }
                /*TODO*///	
/*TODO*///				if (*ppu_latch)
/*TODO*///				{
/*TODO*///					(*ppu_latch)((PPU_tile_page << 10) | (ppu_page[page][address] << 4));
/*TODO*///				}
/*TODO*///	
            }

            start_x += 8;

            /* Move to next tile over and toggle the horizontal name table if necessary */
            x++;
            if (x > 31) {
                x = 0;
                tile_index ^= 0x400;
            }
        }

        /* If the left 8 pixels for the background are off, blank 'em */
 /* TODO: handle this properly, along with sprite clipping */
        if ((PPU_Control1 & PPU_c1_background_L8) == 0) {
            memset(Machine.scrbitmap.line[scanline], Machine.pens[PPU_background_color & color_mask], 0x08);
        }
        //	draw_sprites:
        /* If sprites are hidden in the leftmost column, fake a priority flag to mask them */
        if ((PPU_Control1 & PPU_c1_sprites_L8) == 0) {
            for (i = 0; i < 8; i++) {
                line_priority[i] |= 0x01;
            }
        }

        /* If sprites are on, draw them */
        if ((PPU_Control1 & PPU_c1_sprites) != 0) {
            render_sprites(scanline);
        }

        /* Does the user not want to see the top/bottom 8 lines? */
        if ((readinputport(7) & 0x01) != 0 && ((scanline < 8) || (scanline > 231))) {
            /* Clear this line if we're not drawing it */
            memset(Machine.scrbitmap.line[scanline], Machine.pens[0x3f & color_mask], 0x100);
        }

        //draw_nothing:
        /* Increment the fine y-scroll */
        u16_PPU_refresh_data = (u16_PPU_refresh_data + 0x1000) & 0xFFFF;

        /* If it's rolled, increment the coarse y-scroll */
        if ((u16_PPU_refresh_data & 0x8000) != 0) {
            int tmp;
            tmp = ((u16_PPU_refresh_data & 0x03e0) + 0x20) & 0xFFFF;
            u16_PPU_refresh_data = (u16_PPU_refresh_data & 0x7c1f) & 0xFFFF;
            /* Handle bizarro scrolling rollover at the 30th (not 32nd) vertical tile */
            if (tmp == 0x03c0) {
                u16_PPU_refresh_data = (u16_PPU_refresh_data ^ 0x0800) & 0xFFFF;
            } else {
                u16_PPU_refresh_data = (u16_PPU_refresh_data | (tmp & 0x03e0)) & 0xFFFF;
            }
        }

    }

    static void render_sprites(int scanline) {
        int x, y;
        int tile, i, index1, page;
        int pri;

        int flipx, flipy, color;
        int size;
        int spriteCount;

        /* Determine if the sprites are 8x8 or 8x16 */
        size = (PPU_Control0 & PPU_c0_sprite_size) != 0 ? 16 : 8;

        spriteCount = 0;

        for (i = 0; i < 0x100; i += 4) //	for (i = 0xfc; i >= 0; i -= 4)
        {
            y = spriteram.read(i) + 1;

            /* If the sprite isn't visible, skip it */
            if ((y + size <= scanline) || (y > scanline)) {
                continue;
            }

            x = spriteram.read(i + 3);
            tile = spriteram.read(i + 1);
            color = (spriteram.read(i + 2) & 0x03) + 4;
            pri = spriteram.read(i + 2) & 0x20;
            flipx = spriteram.read(i + 2) & 0x40;
            flipy = spriteram.read(i + 2) & 0x80;

            if (size == 16) {
                /* If it's 8x16 and odd-numbered, draw the other half instead */
                if ((tile & 0x01) != 0) {
                    tile &= ~0x01;
                    tile |= 0x100;
                }
                /* Note that the sprite page value has no effect on 8x16 sprites */
                page = tile >> 6;
            } else {
                page = (tile >> 6) | PPU_sprite_page;
            }

            //		if (Mapper == 5)
            //			index1 = nes_vram_sprite[page] + (tile & 0x3f);
            //		else
            index1 = nes_vram[page] + (tile & 0x3f);

            //if (priority == 0)
/*TODO*///	{
/*TODO*///			if (*ppu_latch)
/*TODO*///			{
/*TODO*///	//			if ((tile == 0x1fd) || (tile == 0x1fe)) Debugger ();
/*TODO*///				(*ppu_latch)((PPU_sprite_page << 10) | ((tile & 0xff) << 4));
/*TODO*///			}
/*TODO*///	//		continue;
/*TODO*///	}
            {
                int sprite_line;
                int drawn = 0;
                IntArray paldata;
                UBytePtr sd;
                UBytePtr bm;
                int start;

                sprite_line = scanline - y;
                if (flipy != 0) {
                    sprite_line = (size - 1) - sprite_line;
                }

                if ((i == 0) /*&& (spriteram.read(i+2)& 0x20)*/) {
                    //	if (y2 == 0)
                    //		logerror ("sprite 0 (%02x/%02x) tile: %04x, bank: %d, color: %02x, flags: %02x\n", x, y, index1, bank, color, spriteram.read(i+2));
                    //	color = rand() & 0xff;
                    //	if (y == 0xc0)
                    //		Debugger ();
                }

                paldata = new IntArray(Machine.gfx[gfx_bank].colortable, 4 * color);
                start = (index1 % Machine.gfx[gfx_bank].total_elements) * 8 + sprite_line;
                bm = new UBytePtr(Machine.scrbitmap.line[scanline], x);
                sd = new UBytePtr(Machine.gfx[gfx_bank].gfxdata, start * Machine.gfx[gfx_bank].width);

                if (pri != 0) {
                    /* Draw the low-priority sprites */
                    int j;

                    if (flipx != 0) {
                        for (j = 0; j < 8; j++) {
                            /* Is this pixel non-transparent? */
                            if (sd.read(7 - j) != 0) {
                                /* Has the background (or another sprite) already been drawn here? */
                                if (line_priority[x + j] == 0) {
                                    /* No, draw */
                                    plot_pixel.handler(Machine.scrbitmap, x + j, scanline, paldata.read(sd.read(7 - j)));
                                    drawn = 1;
                                }
                                /* Indicate that a sprite was drawn at this location, even if it's not seen */
                                line_priority[x + j] |= 0x01;

                                /* Set the "sprite 0 hit" flag if appropriate */
                                if (i == 0) {
                                    PPU_Status |= PPU_status_sprite0_hit;
                                }
                            }
                        }
                    } else {
                        for (j = 0; j < 8; j++) {
                            /* Is this pixel non-transparent? */
                            if (sd.read(j) != 0) {
                                /* Has the background (or another sprite) already been drawn here? */
                                if (line_priority[x + j] == 0) {
                                    plot_pixel.handler(Machine.scrbitmap, x + j, scanline, paldata.read(sd.read(j)));
                                    drawn = 1;
                                }
                                /* Indicate that a sprite was drawn at this location, even if it's not seen */
                                line_priority[x + j] |= 0x01;

                                /* Set the "sprite 0 hit" flag if appropriate */
                                if (i == 0) {
                                    PPU_Status |= PPU_status_sprite0_hit;
                                }
                            }
                        }
                    }
                } else {
                    /* Draw the high-priority sprites */
                    int j;

                    if (flipx != 0) {
                        for (j = 0; j < 8; j++) {
                            /* Is this pixel non-transparent? */
                            if (sd.read(7 - j) != 0) {
                                /* Has another sprite been drawn here? */
                                if ((line_priority[x + j] & 0x01) == 0) {
                                    /* No, draw */
                                    plot_pixel.handler(Machine.scrbitmap, x + j, scanline, paldata.read(sd.read(7 - j)));
                                    line_priority[x + j] |= 0x01;
                                    drawn = 1;
                                }

                                /* Set the "sprite 0 hit" flag if appropriate */
                                if ((i == 0) && (line_priority[x + j] & 0x02) != 0) {
                                    PPU_Status |= PPU_status_sprite0_hit;
                                }
                            }
                        }
                    } else {
                        for (j = 0; j < 8; j++) {
                            /* Is this pixel non-transparent? */
                            if (sd.read(j) != 0) {
                                /* Has another sprite been drawn here? */
                                if ((line_priority[x + j] & 0x01) == 0) {
                                    /* No, draw */
                                    plot_pixel.handler(Machine.scrbitmap, x + j, scanline, paldata.read(sd.read(j)));
                                    line_priority[x + j] |= 0x01;
                                    drawn = 1;
                                }

                                /* Set the "sprite 0 hit" flag if appropriate */
                                if ((i == 0) && (line_priority[x + j] & 0x02) != 0) {
                                    PPU_Status |= PPU_status_sprite0_hit;
                                }
                            }
                        }
                    }
                }

                if (drawn != 0) {
                    /* If there are more than 8 sprites on this line, set the flag */
                    spriteCount++;
                    if (spriteCount == 8) {
                        PPU_Status |= PPU_status_8sprites;
                        logerror("> 8 sprites (%d), scanline: %d\n", spriteCount, scanline);

                        /* The real NES only draws up to 8 sprites - the rest should be invisible */
                        if ((readinputport(7) & 0x02) == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public static WriteHandlerPtr nes_vh_sprite_dma_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            
            UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
            memcpy (spriteram, new UBytePtr(RAM, data * 0x100), 0x100);
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	#ifdef macintosh
/*TODO*///		if (data >= 0x40) SysBeep (0);
/*TODO*///	#endif
/*TODO*///	#endif
        }
    };

    public static void draw_sight(int playerNum, int x_center, int y_center) {
        System.out.println("plot");
        int x, y;
        char color;

        if (playerNum == 2) {
            color = (char) Machine.pens[0];
            /* grey */
        } else {
            color = (char) Machine.pens[0x30];
            /* white */
        }

        if (x_center < 2) {
            x_center = 2;
        }
        if (x_center > 253) {
            x_center = 253;
        }

        if (y_center < 2) {
            y_center = 2;
        }
        if (y_center > 253) {
            y_center = 253;
        }

        for (y = y_center - 5; y < y_center + 6; y++) {
            if ((y >= 0) && (y < 256)) {
                plot_pixel.handler(Machine.scrbitmap, x_center, y, color);
            }
        }

        for (x = x_center - 5; x < x_center + 6; x++) {
            if ((x >= 0) && (x < 256)) {
                plot_pixel.handler(Machine.scrbitmap, x, y_center, color);
            }
        }
    }

    /* This routine is called at the start of vblank to refresh the screen */
    public static VhUpdatePtr nes_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            //System.out.println("nes_vh_screenrefresh "+readinputport(2));
            if (readinputport(2) == 0x01) /* zapper on port 1 */ {
                draw_sight(1, readinputport(3), readinputport(4));
            } else if (readinputport(2) == 0x10) /* zapper on port 2 */ {
                draw_sight(1, readinputport(3), readinputport(4));
            } else if (readinputport(2) == 0x11) /* zapper on both ports */ {
                draw_sight(1, readinputport(3), readinputport(4));
                draw_sight(2, readinputport(5), readinputport(6));
            }

            /* if this is a disk system game, check for the flip-disk key */
            if (_nes.mapper == 20) {
                throw new UnsupportedOperationException("Not supported yet.");
                /*TODO*///			if (readinputport (11) & 0x01)
/*TODO*///			{
/*TODO*///				while (readinputport (11) & 0x01) { update_input_ports (); };
/*TODO*///	
/*TODO*///				nes_fds.current_side ++;
/*TODO*///				if (nes_fds.current_side > nes_fds.sides)
/*TODO*///					nes_fds.current_side = 0;
/*TODO*///	
/*TODO*///				if (nes_fds.current_side == 0)
/*TODO*///				{
/*TODO*///					usrintf_showmessage ("No disk inserted.");
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					usrintf_showmessage ("Disk set to side %d", nes_fds.current_side);
/*TODO*///				}
/*TODO*///			}
            }
        }
    };	
}
