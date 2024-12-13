/***************************************************************************

  spectrum.c

  Functions to emulate the video hardware of the ZX Spectrum.

  Changes:

  DJR 08/02/00 - Added support for FLASH 1.
  DJR 16/05/00 - Support for TS2068/TC2048 hires and 64 column modes.
  DJR 19/05/00 - Speeded up Spectrum 128 screen refresh.
  DJR 23/05/00 - Preliminary support for border colour emulation.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.osdepend.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.commonH.*;
import static mess056.eventlst.*;
import static mess056.eventlstH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mess056.includes.spectrumH.*;
import static mess056.systems.spectrum.spectrum_128_screen_location;
import static mess056.systems.spectrum.ts2068_port_ff_data;
import static mess056.systems.spectrum.ts2068_ram;
import static mess056.vidhrdw.border.*;

public class spectrum
{
	
        public static UBytePtr spectrum_characterram;
	public static UBytePtr spectrum_colorram;
	static UBytePtr charsdirty;
	static int frame_number;    /* Used for handling FLASH 1 */
	static int flash_invert=0;
	
	/***************************************************************************
	  Start the video hardware emulation.
	***************************************************************************/
	public static VhStartPtr spectrum_vh_start = new VhStartPtr() { public int handler() 
	{
	        frame_number = 0;
	        flash_invert = 0;
		spectrum_characterram = new UBytePtr(0x1800);
	        if (spectrum_characterram == null)
			return 1;
	
		spectrum_colorram = new UBytePtr(0x300);
	        if (spectrum_colorram == null)
	        {
			//free(spectrum_characterram);
                        spectrum_characterram = null;
			return 1;
		}
	
	        charsdirty = new UBytePtr(0x300);
	        if (charsdirty == null)
	        {
			//free(spectrum_colorram);
                        spectrum_colorram = null;
			//free(spectrum_characterram);
                        spectrum_characterram = null;
			return 1;
		}
		memset(charsdirty,1,0x300);
	
		EventList_Initialise(30000);
	
		return 0;
	} };
	
	public static VhStopPtr spectrum_vh_stop = new VhStopPtr() { public void handler() 
	{
		EventList_Finish();
	
		//free(spectrum_characterram);
                spectrum_characterram = null;
		//free(spectrum_colorram);
                spectrum_colorram = null;
		//free(charsdirty);
                charsdirty = null;
	} };
	
	/* screen is stored as:
	32 chars wide. first 0x100 bytes are top scan of lines 0 to 7 */
	
	public static WriteHandlerPtr spectrum_characterram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                spectrum_characterram.write(offset, data);
	
	        charsdirty.write(((offset & 0x0f800)>>3) + (offset & 0x0ff), 1);
            }
            
        };
	
	public static ReadHandlerPtr spectrum_characterram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return(spectrum_characterram.read(offset));
            }
            
        };
	
	public static WriteHandlerPtr spectrum_colorram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /* Will eventually be used to emulate hi-res colour effects. No point
	           doing it now as contented memory is not emulated so timings will
	           be way off. (eg Zynaps taking 212 cycles not 224 per scanline)
	        */
	/*        EventList_AddItemOffset(offset+0x5800, data, cpu_getcurrentcycles()); */
	
		spectrum_colorram.write(offset, data);
		charsdirty.write(offset, 1);
            }
        };
	
	public static ReadHandlerPtr spectrum_colorram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return(spectrum_colorram.read(offset));
            }
        };
	
	/* return the color to be used inverting FLASHing colors if necessary */
	public static int get_display_color (int color, int invert)
	{
	        if ((invert != 0) && ((color & 0x80) != 0))
	                return (color & 0xc0) + ((color & 0x38) >> 3) + ((color & 0x07) << 3);
	        else
	                return color;
	}
	
	/* Code to change the FLASH status every 25 frames. Note this must be
	   independent of frame skip etc. */
	public static VhEofCallbackPtr spectrum_eof_callback = new VhEofCallbackPtr() { public void handler() 
	{
	        EVENT_LIST_ITEM pItem;
	        int NumItems;
	
	        frame_number++;
	        if (frame_number >= 25)
	        {
	                frame_number = 0;
	                flash_invert = (flash_invert==0) ? 1 : 0;
	        }
	
	        /* Empty event buffer for undisplayed frames noting the last border
	           colour (in case colours are not changed in the next frame). */
	        NumItems = EventList_NumEvents();
	        if (NumItems != 0)
	        {
	                pItem = EventList_GetFirstItem();
	                set_last_border_color ( pEventListBuffer[NumItems-1].Event_Data );
	                EventList_Reset();
	                EventList_SetOffsetStartTime ( TIME_TO_CYCLES(0,cpu_getscanline()*cpu_getscanlineperiod()) );
	                logerror ("Event log reset in callback fn.\n");
	        }
	} };
	
	
	/* Update FLASH status for ts2068. Assumes flash update every 1/2s. */
	public static VhEofCallbackPtr ts2068_eof_callback = new VhEofCallbackPtr() { public void handler() 
	{
	        EVENT_LIST_ITEM pItem;
	        int NumItems;
	
	        frame_number++;
	        if (frame_number >= 30)
	        {
	                frame_number = 0;
	                flash_invert = (flash_invert==0) ? 1 : 0;
	        }
	
	        /* Empty event buffer for undisplayed frames noting the last border
	           colour (in case colours are not changed in the next frame). */
	        NumItems = EventList_NumEvents();
	        if (NumItems != 0)
	        {
	                pItem = EventList_GetFirstItem();
	                set_last_border_color ( pEventListBuffer[NumItems-1].Event_Data );
	                EventList_Reset();
	                EventList_SetOffsetStartTime ( TIME_TO_CYCLES(0,cpu_getscanline()*cpu_getscanlineperiod()) );
	                logerror ("Event log reset in callback fn.\n");
	        }
	} };
	
	/***************************************************************************
	  Update the spectrum screen display.
	
	  The screen consists of 312 scanlines as follows:
	  64  border lines (the last 48 are actual border lines; the others may be
	                    border lines or vertical retrace)
	  192 screen lines
	  56  border lines
	
	  Each screen line has 48 left border pixels, 256 screen pixels and 48 right
	  border pixels.
	
	  Each scanline takes 224 T-states divided as follows:
	  128 Screen (reads a screen and ATTR byte [8 pixels] every 4 T states)
	  24  Right border
	  48  Horizontal retrace
	  24  Left border
	
	  The 128K Spectrums have only 63 scanlines before the TV picture (311 total)
	  and take 228 T-states per scanline.
	
	***************************************************************************/
	static int last_invert = 0;
        
	public static VhUpdatePtr spectrum_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int count;
	        
	
	        if (full_refresh != 0)
	        {
			memset(charsdirty,1,0x300);
	                last_invert = flash_invert;
	        }
	        else
	        {
	                /* Update all flashing characters when necessary */
	                if (last_invert != flash_invert) {
	                        for (count=0;count<0x300;count++)
	                                if ((spectrum_colorram.read(count) & 0x80) != 0)
	                                        charsdirty.write(count, 1);
	                        last_invert = flash_invert;
	                }
	        }
	
	        for (count=0;count<32*8;count++)
	        {
			if ((charsdirty.read(count)) != 0) {
				decodechar( Machine.gfx[0],count,spectrum_characterram,
					    Machine.drv.gfxdecodeinfo[0].gfxlayout );
			}
	
			if ((charsdirty.read(count+256)) != 0) {
				decodechar( Machine.gfx[1],count,new UBytePtr(spectrum_characterram,0x800),
					    Machine.drv.gfxdecodeinfo[0].gfxlayout );
			}
	
			if ((charsdirty.read(count+512)) != 0) {
				decodechar( Machine.gfx[2],count,new UBytePtr(spectrum_characterram,0x1000),
					    Machine.drv.gfxdecodeinfo[0].gfxlayout );
			}
		}
	
	        for (count=0;count<32*8;count++)
	        {
			int sx=count%32;
			int sy=count/32;
			int color;
	
	                if ((charsdirty.read(count)) != 0) {
	                        color=get_display_color(spectrum_colorram.read(count),
	                                                flash_invert);
				drawgfx(bitmap,Machine.gfx[0],
					count,
					color,
					0,0,
	                                (sx*8)+SPEC_LEFT_BORDER,(sy*8)+SPEC_TOP_BORDER,
					null,TRANSPARENCY_NONE,0);
				charsdirty.write(count, 0);
			}
	
			if ((charsdirty.read(count+256)) != 0) {
	                        color=get_display_color(spectrum_colorram.read(count+0x100),
	                                                flash_invert);
				drawgfx(bitmap,Machine.gfx[1],
					count,
					color,
					0,0,
	                                (sx*8)+SPEC_LEFT_BORDER,((sy+8)*8)+SPEC_TOP_BORDER,
					null,TRANSPARENCY_NONE,0);
				charsdirty.write(count+256, 0);
			}
	
			if ((charsdirty.read(count+512)) != 0) {
	                        color=get_display_color(spectrum_colorram.read(count+0x200),
	                                                flash_invert);
				drawgfx(bitmap,Machine.gfx[2],
					count,
					color,
					0,0,
	                                (sx*8)+SPEC_LEFT_BORDER,((sy+16)*8)+SPEC_TOP_BORDER,
					null,TRANSPARENCY_NONE,0);
				charsdirty.write(count+512, 0);
			}
		}
	
	        /* When screen refresh is called there is only one blank line
	           (synchronised with start of screen data) before the border lines.
	           There should be 16 blank lines after an interrupt is called.
	        */
	        draw_border(bitmap, full_refresh,
	                SPEC_TOP_BORDER, SPEC_DISPLAY_YSIZE, SPEC_BOTTOM_BORDER,
	                SPEC_LEFT_BORDER, SPEC_DISPLAY_XSIZE, SPEC_RIGHT_BORDER,
	                SPEC_LEFT_BORDER_CYCLES, SPEC_DISPLAY_XSIZE_CYCLES,
	                SPEC_RIGHT_BORDER_CYCLES, SPEC_RETRACE_CYCLES, 200, 0xfe);
	}};
	
	
	public static VhStartPtr spectrum_128_vh_start = new VhStartPtr() { public int handler() 
	{
	        frame_number = 0;
	        flash_invert = 0;
	
		EventList_Initialise(30000);
	
	        return 0;
	} };
	
	public static VhStopPtr spectrum_128_vh_stop = new VhStopPtr() { public void handler() 
	{
	        EventList_Finish();
	} };
	
	/* Refresh the spectrum 128 screen (code modified from COUPE.C) */
        public static VhUpdatePtr spectrum_128_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                /* for now do a full-refresh */
	        int x, y, b, scrx, scry;
	        int ink, pap;
	        UBytePtr attr, scr;
	
	        scr=new UBytePtr(spectrum_128_screen_location, 0);
                //scr=spectrum_128_screen_location;
                //scr.offset=0;
	
	        for (y=0; y<192; y++)
	        {
	                scrx=SPEC_LEFT_BORDER;
	                scry=((y&7) * 8) + ((y&0x38)>>3) + (y&0xC0);
	                attr=new UBytePtr(spectrum_128_screen_location, ((scry>>3)*32) + 0x1800);
	
	                for (x=0;x<32;x++)
	                {
	                        /* Get ink and paper colour with bright */
	                        if ((flash_invert==1) & ((attr.read() & 0x80)!=0))
	                        {
	                                ink=((attr.read())>>3) & 0x0f;
	                                pap=((attr.read()) & 0x07) + (((attr.read())>>3) & 0x08);
	                        }
	                        else
	                        {
	                                ink=((attr.read()) & 0x07) + (((attr.read())>>3) & 0x08);
	                                pap=((attr.read())>>3) & 0x0f;
	                        }
	
	                        for (b=0x80;b!=0;b>>=1)
	                        {
	                                if ((scr.read()&b)!=0)
	                                        plot_pixel.handler(bitmap,scrx++,SPEC_TOP_BORDER+scry,Machine.pens[ink]);
	                                else
	                                        plot_pixel.handler(bitmap,scrx++,SPEC_TOP_BORDER+scry,Machine.pens[pap]);
				}
	                scr.inc();
	                attr.inc();
	                }
		}
	
	        draw_border(bitmap, full_refresh,
	                SPEC_TOP_BORDER, SPEC_DISPLAY_YSIZE, SPEC_BOTTOM_BORDER,
	                SPEC_LEFT_BORDER, SPEC_DISPLAY_XSIZE, SPEC_RIGHT_BORDER,
	                SPEC_LEFT_BORDER_CYCLES, SPEC_DISPLAY_XSIZE_CYCLES,
	                SPEC_RIGHT_BORDER_CYCLES, SPEC128_RETRACE_CYCLES, 200, 0xfe);
                
                //spectrum_128_screen_location.offset=0;
	} };

	/*******************************************************************
	 *
	 *      Update the TS2068 display.
	 *
	 *      Port ff is used to set the display mode.
	 *
	 *      bits 2..0  Video Mode Select
	 *      000 = Primary DFILE active   (at 0x4000-0x5aff)
	 *      001 = Secondary DFILE active (at 0x6000-0x7aff)
	 *      010 = Extended Colour Mode   (chars at 0x4000-0x57ff, colors 0x6000-0x7aff)
	 *      110 = 64 column mode         (columns 0,2,4,...62 from DFILE 1
	 *                                    columns 1,3,5,...63 from DFILE 2)
	 *      other = unpredictable results
	 *
	 *      bits 5..3  64 column mode ink/paper selection (attribute value in brackets)
	 *      000 = Black/White   (56)        100 = Green/Magenta (28)
	 *      001 = Blue/Yellow   (49)        101 = Cyan/Red      (21)
	 *      010 = Red/Cyan      (42)        110 = Yellow/Blue   (14)
	 *      011 = Magenta/Green (35)        111 = White/Black   (7)
	 *
	 *******************************************************************/
	
	/* Draw a scanline in TS2068/TC2048 hires mode (code modified from COUPE.C) */
	static void ts2068_hires_scanline(mame_bitmap bitmap, int y, int borderlines)
	{
		int x,b,scrx,scry;
		int ink,pap;
	        UBytePtr attr, scr;
	
	        scrx=TS2068_LEFT_BORDER;
		scry=((y&7) * 8) + ((y&0x38)>>3) + (y&0xC0);
	
	        scr=new UBytePtr(ts2068_ram, y*32);
	        attr=new UBytePtr(scr, 0x2000);
	
	        for (x=0;x<32;x++)
		{
	                /* Get ink and paper colour with bright */
	                if ((flash_invert != 0) && ((attr.read() & 0x80) != 0))
	                {
	                        ink=((attr.read())>>3) & 0x0f;
	                        pap=((attr.read()) & 0x07) + (((attr.read())>>3) & 0x08);
	                }
	                else
	                {
	                        ink=((attr.read()) & 0x07) + (((attr.read())>>3) & 0x08);
	                        pap=((attr.read())>>3) & 0x0f;
	                }
	
			for (b=0x80;b!=0;b>>=1)
			{
	                        if ((scr.read()&b) != 0)
				{
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[ink]);
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[ink]);
				}
				else
				{
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[pap]);
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[pap]);
				}
			}
	                scr.inc();
	                attr.inc();
		}
	}
	
	/* Draw a scanline in TS2068/TC2048 64-column mode */
	static void ts2068_64col_scanline(mame_bitmap bitmap, int y, int borderlines, int inkcolor)
	{
		int x,b,scrx,scry;
	        UBytePtr scr1, scr2;
	
	        scrx=TS2068_LEFT_BORDER;
		scry=((y&7) * 8) + ((y&0x38)>>3) + (y&0xC0);
	
	        scr1=new UBytePtr(ts2068_ram, y*32);
	        scr2=new UBytePtr(scr1, 0x2000);
	
	        for (x=0;x<32;x++)
		{
			for (b=0x80;b!=0;b>>=1)
			{
	                        if ((scr1.read()&b) != 0)
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[inkcolor]);
				else
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[7-inkcolor]);
			}
	                scr1.inc();
	
			for (b=0x80;b!=0;b>>=1)
			{
	                        if ((scr2.read()&b) != 0)
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[inkcolor]);
				else
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[7-inkcolor]);
			}
	                scr2.inc();
		}
	}
	
	/* Draw a scanline in TS2068/TC2048 lores (normal Spectrum) mode */
	static void ts2068_lores_scanline(mame_bitmap bitmap, int y, int borderlines, int screen)
	{
		int x,b,scrx,scry;
		int ink,pap;
	        UBytePtr attr, scr;
	
	        scrx=TS2068_LEFT_BORDER;
		scry=((y&7) * 8) + ((y&0x38)>>3) + (y&0xC0);
	
	        scr=new UBytePtr(ts2068_ram, y*32 + screen*0x2000);
	        attr=new UBytePtr(ts2068_ram, ((scry>>3)*32) + screen*0x2000 + 0x1800);
	
	        for (x=0;x<32;x++)
		{
	                /* Get ink and paper colour with bright */
	                if ((flash_invert!=0) && ((attr.read() & 0x80) != 0))
	                {
	                        ink=((attr.read())>>3) & 0x0f;
	                        pap=((attr.read()) & 0x07) + (((attr.read())>>3) & 0x08);
	                }
	                else
	                {
	                        ink=((attr.read()) & 0x07) + (((attr.read())>>3) & 0x08);
	                        pap=((attr.read())>>3) & 0x0f;
	                }
	
			for (b=0x80;b!=0;b>>=1)
			{
	                        if ((scr.read()&b) != 0)
				{
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[ink]);
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[ink]);
				}
				else
				{
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[pap]);
	                                plot_pixel.handler(bitmap,scrx++,scry+borderlines,Machine.pens[pap]);
				}
			}
	                scr.inc();
	                attr.inc();
		}
	}
	
	public static VhUpdatePtr ts2068_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                /* for now TS2068 will do a full-refresh */
		int count;
	
	        if ((ts2068_port_ff_data & 7) == 6)
	        {
	                /* 64 Column mode */
	                int inkcolor = (ts2068_port_ff_data & 0x38) >> 3;
	                for (count = 0; count < 192; count++)
	                        ts2068_64col_scanline(bitmap, count, TS2068_TOP_BORDER, inkcolor);
		}
	        else if ((ts2068_port_ff_data & 7) == 2)
	        {
	                /* Extended Color mode */
	                for (count = 0; count < 192; count++)
	                        ts2068_hires_scanline(bitmap, count, TS2068_TOP_BORDER);
	        }
	        else if ((ts2068_port_ff_data & 7) == 1)
	        {
	                /* Screen 6000-7aff */
	                for (count = 0; count < 192; count++)
	                        ts2068_lores_scanline(bitmap, count, TS2068_TOP_BORDER, 1);
	        }
	        else
	        {
	                /* Screen 4000-5aff */
	                for (count = 0; count < 192; count++)
	                        ts2068_lores_scanline(bitmap, count, TS2068_TOP_BORDER, 0);
	        }
	
	        draw_border(bitmap, full_refresh,
	                TS2068_TOP_BORDER, SPEC_DISPLAY_YSIZE, TS2068_BOTTOM_BORDER,
	                TS2068_LEFT_BORDER, TS2068_DISPLAY_XSIZE, TS2068_RIGHT_BORDER,
	                SPEC_LEFT_BORDER_CYCLES, SPEC_DISPLAY_XSIZE_CYCLES,
	                SPEC_RIGHT_BORDER_CYCLES, SPEC_RETRACE_CYCLES, 200, 0xfe);
            }
        };

	public static VhUpdatePtr tc2048_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                /* for now TS2068 will do a full-refresh */
		int count;
	
	        if ((ts2068_port_ff_data & 7) == 6)
	        {
	                /* 64 Column mode */
	                int inkcolor = (ts2068_port_ff_data & 0x38) >> 3;
	                for (count = 0; count < 192; count++)
	                        ts2068_64col_scanline(bitmap, count, SPEC_TOP_BORDER, inkcolor);
		}
	        else if ((ts2068_port_ff_data & 7) == 2)
	        {
	                /* Extended Color mode */
	                for (count = 0; count < 192; count++)
	                        ts2068_hires_scanline(bitmap, count, SPEC_TOP_BORDER);
	        }
	        else if ((ts2068_port_ff_data & 7) == 1)
	        {
	                /* Screen 6000-7aff */
	                for (count = 0; count < 192; count++)
	                        ts2068_lores_scanline(bitmap, count, SPEC_TOP_BORDER, 1);
	        }
	        else
	        {
	                /* Screen 4000-5aff */
	                for (count = 0; count < 192; count++)
	                        ts2068_lores_scanline(bitmap, count, SPEC_TOP_BORDER, 0);
	        }
	
	        draw_border(bitmap, full_refresh,
	                SPEC_TOP_BORDER, SPEC_DISPLAY_YSIZE, SPEC_BOTTOM_BORDER,
	                TS2068_LEFT_BORDER, TS2068_DISPLAY_XSIZE, TS2068_RIGHT_BORDER,
	                SPEC_LEFT_BORDER_CYCLES, SPEC_DISPLAY_XSIZE_CYCLES,
	                SPEC_RIGHT_BORDER_CYCLES, SPEC_RETRACE_CYCLES, 200, 0xfe);
            }
        };
	
}