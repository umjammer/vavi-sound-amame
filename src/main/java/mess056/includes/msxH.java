/*
** msx.h : part of MSX1 emulation.
**
** By Sean Young 1999
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static common.ptr.*;

/**
 *
 * @author chusogar
 */
public class msxH {
    
    public static int MSX_MAX_CARTS   = (2);

    public static class MSX_CART {
        public int type,bank_mask;
        public int[] banks = new int[4];
        public UBytePtr mem = null;
        public String sramfile;
        public int pacsram;
    };

    public static class MSX {
        public int run; /* set after init_msx () */
        /* PSG */
        public int psg_b,opll_active;
        /* memory */
        public UBytePtr empty=null, ram=null, ramp=new UBytePtr(4);
        /* memory status */
        public MSX_CART[] cart = new MSX_CART[MSX_MAX_CARTS];
        
        /* printer */
        public int prn_data, prn_strobe;
        /* mouse */
        public int[] mouse = new int[2];
        public int[] mouse_stat = new int[2];
        /* rtc */
        public int rtc_latch;
        /* disk */
        public int dsk_stat;
        public UBytePtr disk = null;
        
        public MSX() {
            for (int _i = 0; _i<MSX_MAX_CARTS ; _i++)
                cart[_i] = new MSX_CART();        
        }
        
    };

    
}
