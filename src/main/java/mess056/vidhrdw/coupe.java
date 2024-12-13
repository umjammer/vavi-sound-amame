/***************************************************************************

 SAM Coupe Driver - Written By Lee Hammerton

  Functions to emulate the video hardware of the coupe.

 At present these are not done using the mame driver standard, they are
 simply plot pixelled.. I will fix this in a future version.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.mame.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.vidhrdw.generic.*;
import static mess056.machine.coupe.*;

/**
 *
 * @author chusogar
 */
public class coupe {
	
	
	public static UBytePtr sam_screen = new UBytePtr();
        public static boolean MONO = false;
	
	/***************************************************************************
	  Start the video hardware emulation.
	***************************************************************************/
	public static VhStartPtr coupe_vh_start = new VhStartPtr() {
            public int handler() {
                if( generic_bitmapped_vh_start.handler()!= 0 )
			return 1;
		return 0;
            }
        };
	
	public static VhStopPtr coupe_vh_stop = new VhStopPtr() {
            public void handler() {
                generic_vh_stop.handler();
            }
        };
	
	//#define MONO
	
	public static void drawMode4_line(mame_bitmap bitmap,int y)
	{
            //System.out.println("drawMode4_line");
		int x;
		int tmp=0;
	
		for (x=0;x<256;)
		{
			tmp=(sam_screen).read((x/2) + (y*128));
                        if (MONO){
                            if ((tmp>>4) != 0)
                            {
                                    plot_pixel.handler(bitmap, x*2, y, Machine.pens[127]);
                                    plot_pixel.handler(bitmap, x*2+1, y, Machine.pens[127]);
                            }
                            else
                            {
                                    plot_pixel.handler(bitmap, x*2, y, Machine.pens[0]);
                                    plot_pixel.handler(bitmap, x*2+1, y, Machine.pens[0]);
                            }
                            x++;
                            if ((tmp&0x0F) != 0)
                            {
                                    plot_pixel.handler(bitmap, x*2, y, Machine.pens[127]);
                                    plot_pixel.handler(bitmap, x*2+1, y, Machine.pens[127]);
                            }
                            else
                            {
                                    plot_pixel.handler(bitmap, x*2, y, Machine.pens[0]);
                                    plot_pixel.handler(bitmap, x*2+1, y, Machine.pens[0]);
                            }
                            x++;
                        } else {
                            plot_pixel.handler(bitmap, x*2, y, Machine.pens[CLUT[tmp>>4]]);
                            plot_pixel.handler(bitmap, x*2+1, y, Machine.pens[CLUT[tmp>>4]]);
                            x++;
                            plot_pixel.handler(bitmap, x*2, y, Machine.pens[CLUT[tmp&0x0F]]);
                            plot_pixel.handler(bitmap, x*2+1, y, Machine.pens[CLUT[tmp&0x0F]]);				
                            x++;
                        }
		}
	}
	
	public static void drawMode3_line(mame_bitmap bitmap,int y)
	{
            System.out.println("drawMode3_line");
		int x;
		int tmp=0;
	
		for (x=0;x<512;)
		{
			tmp=(sam_screen).read((x/4) + (y*128));
                        if (MONO){
                            if ((tmp>>6) != 0)
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[127]);
                            else
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[0]);
                            x++;
                            if (((tmp>>4)&0x03) != 0)
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[127]);
                            else
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[0]);
                            x++;
                            if (((tmp>>2)&0x03) != 0)
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[127]);
                            else
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[0]);
                            x++;
                            if ((tmp&0x03) != 0)
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[127]);
                            else
                                    plot_pixel.handler(bitmap,x,y,Machine.pens[0]);
                            x++;
                        } else {
                            plot_pixel.handler(bitmap,x,y,Machine.pens[CLUT[tmp>>6]]);
                            x++;
                            plot_pixel.handler(bitmap,x,y,Machine.pens[CLUT[(tmp>>4)&0x03]]);
                            x++;
                            plot_pixel.handler(bitmap,x,y,Machine.pens[CLUT[(tmp>>2)&0x03]]);
                            x++;
                            plot_pixel.handler(bitmap,x,y,Machine.pens[CLUT[tmp&0x03]]);
                            x++;
                        }
		}
	}
	
	public static void drawMode2_line(mame_bitmap bitmap,int y)
	{
            System.out.println("drawMode2_line");
		int x,b,scrx;
		int tmp=0;
		int ink,pap;
		UBytePtr attr = new UBytePtr();
	
		attr=new UBytePtr(sam_screen, 32*192 + y*32);
	
		scrx=0;
		for (x=0;x<256/8;x++)
		{
			tmp=(sam_screen).read(x + (y*32));
                        if (MONO) {
                            ink=127;
                            pap=0;
                        } else {
                            ink=CLUT[(attr.read()) & 0x07];
                            pap=CLUT[((attr.read())>>3) & 0x07];
                        }
			attr.inc();
	
			for (b=0x80;b!=0;b>>=1)
			{
				if ((tmp&b) != 0)
				{
					plot_pixel.handler(bitmap,scrx++,y,Machine.pens[ink]);
					plot_pixel.handler(bitmap,scrx++,y,Machine.pens[ink]);
				}
				else
				{
					plot_pixel.handler(bitmap,scrx++,y,Machine.pens[pap]);
					plot_pixel.handler(bitmap,scrx++,y,Machine.pens[pap]);
				}
			}
		}
	}
	
	public static void drawMode1_line(mame_bitmap bitmap,int y)
	{
            //System.out.println("drawMode1_line");
		int x,b,scrx,scry;
		int tmp=0;
		int ink,pap;
		UBytePtr attr = new UBytePtr();
	
		attr=new UBytePtr(sam_screen, 32*192 + (y/8)*32);
	
		scrx=0;
		scry=((y&7) * 8) + ((y&0x38)>>3) + (y&0xC0);
		for (x=0;x<256/8;x++)
		{
			tmp=sam_screen.read(x + (y*32));
                        if (MONO) {
                            ink=127;
                            pap=0;
                        } else {
                            ink=CLUT[(attr.read()) & 0x07];
                            pap=CLUT[((attr.read())>>3) & 0x07];
                        }
			attr.inc();
			for (b=0x80;b!=0;b>>=1)
			{
				if ((tmp&b) != 0)
				{
					plot_pixel.handler(bitmap,scrx++,scry,Machine.pens[ink]);
					plot_pixel.handler(bitmap,scrx++,scry,Machine.pens[ink]);
				}
				else
				{
					plot_pixel.handler(bitmap,scrx++,scry,Machine.pens[pap]);
					plot_pixel.handler(bitmap,scrx++,scry,Machine.pens[pap]);
				}
			}
		}
	}
	
	/***************************************************************************
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function,
	  it will be called by the main emulation engine.
	***************************************************************************/
	public static VhUpdatePtr coupe_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                
            }
        };
    
}
