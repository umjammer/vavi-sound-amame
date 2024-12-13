/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mess056.machine.lynx.*;

/**
 *
 * @author chusogar
 */
public class lynxH {
    
    public static int PAD_UP    = 0x80;
    public static int PAD_DOWN  = 0x40;
    public static int PAD_LEFT  = 0x20;
    public static int PAD_RIGHT = 0x10;

    public static class MIKEY {
            public int[] data = new int[0x100];
    };

    public static void INCLUDE_LYNX_LINE_FUNCTION(int y, int xdir)
    {
            int j, xi, wi, i;
            int b, p, color;

            i=blitter.mem.read(blitter.bitmap);
            blitter.memory_accesses++;
            for (xi=blitter.x, p=0, b=0, j=1, wi=0; (j<i);) {
                if (p<bits) {
                    b=(b<<8)|blitter.mem.read(blitter.bitmap+j);
                    j++;
                    p+=8;
                    blitter.memory_accesses++;
                }
                for (;(p>=bits);) {
                    color=blitter.color[(b>>(p-bits))&mask]; p-=bits;
                    for (;(wi<blitter.width);wi+=0x100, xi+=xdir) {
                        if ((xi>=0)&&(xi<160)) {
                            lynx_plot_pixel(blitter.mode, xi, y, color);
                        }
                    }
                    wi-=blitter.width;
                }
            }
    }

    public static void INCLUDE_LYNX_LINE_RLE_FUNCTION(int y, int xdir)
    {

            int wi, xi;
            int b, p, j;
            int t, count, color;

            for( p=0, j=0, b=0, xi=blitter.x, wi=0; ; ) { // through the rle entries
                if (p<5+bits) { // under 7 bits no complete entry
                    j++;
                    if (j>=blitter.mem.read(blitter.bitmap)) return;
                    p+=8;
                    b=(b<<8)|blitter.mem.read(blitter.bitmap+j);
                    blitter.memory_accesses++;
                }
                t=(b>>(p-1))&1;p--;
                count=((b>>(p-4))&0xf)+1;p-=4;
                if (t != 0) { // count of different pixels
                    for (;count!=0; count--) {
                        if (p<bits) {
                            j++;
                            if (j>=blitter.mem.read(blitter.bitmap)) return;
                            p+=8;
                            b=(b<<8)|blitter.mem.read(blitter.bitmap+j);
                            blitter.memory_accesses++;
                        }
                        color=blitter.color[(b>>(p-bits))&mask];p-=bits;
                        for (;(wi<blitter.width);wi+=0x100, xi+=xdir) {
                            if ((xi>=0)&&(xi<160)) {
                                lynx_plot_pixel(blitter.mode, xi, y, color);
                            }
                        }
                        wi-=blitter.width;
                    }
                } else { // count of same pixels
                    if (count==0) return;
                    if (p<bits) {
                        j++;
                        if (j>=blitter.mem.read(blitter.bitmap)) return;
                        p+=8;
                        b=(b<<8)|blitter.mem.read(blitter.bitmap+j);
                        blitter.memory_accesses++;
                    }
                    color=blitter.color[(b>>(p-bits))&mask];p-=bits;
                    for (;count!=0; count--) {
                        for (;(wi<blitter.width);wi+=0x100, xi+=xdir) {
                            if ((xi>=0)&&(xi<160)) {
                                lynx_plot_pixel(blitter.mode, xi, y, color);
                            }
                        }
                        wi-=blitter.width;
                    }
                }
            }

    }
    
}

