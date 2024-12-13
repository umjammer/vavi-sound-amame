/*
 * ported to v0.37b6
 * using automatic conversion tool v0.01
 */
package mess056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static mame056.osdependH.*;
import static mess056.vidhrdw.tms9928a.*;
import static mess056.vidhrdw.tms9928aH.*;

import mame056.commonH.mame_bitmap;

public class coleco {

    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr coleco_vh_start = new VhStartPtr() {
        public int handler() {
            return TMS9928A_start(TMS99x8A, 0x4000);
        }
    };

    /**
     * *************************************************************************
     *
     * Stop the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStopPtr coleco_vh_stop = new VhStopPtr() {
        public void handler() {
            TMS9928A_stop.handler();
        }
    };

    /**
     * *************************************************************************
     *
     * Refresh the video screen
     *
     **************************************************************************
     */
    /* This routine is called at the start of vblank to refresh the screen */
    public static VhUpdatePtr coleco_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            TMS9928A_refresh.handler(bitmap, full_refresh);
        }

    };
}
