/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mess056.vidhrdw.vic6567.vic2;

public class vic6567H {
    /*
     * if you need this chip in another mame/mess emulation than let it me know
     * I will split this from the c64 driver
     * peter.trauner@jk.uni-linz.ac.at
     * 1. 1. 2000
     * look at mess/systems/c64.c and mess/machine/c64.c
     * on how to use it
     */

    public static int VIC6567_VRETRACERATE = 60;
    public static int VIC6569_VRETRACERATE = 50;
/*TODO*///    public static int VIC2_VRETRACERATE(){ return (vic2.pal?VIC6569_VRETRACERATE:VIC6567_VRETRACERATE); }
    public static int VIC2_HRETRACERATE = 15625;

    /* to be inserted in MachineDriver-Structure */
    /* the following values depend on the VIC clock,
     * but to achieve TV-frequency the clock must have a fix frequency */
    public static int VIC2_HSIZE	= 320;
    public static int VIC2_VSIZE	= 200;
    /* of course you clock select an other clock, but for accurate */
    /* video timing */
    public static int VIC6567_CLOCK	= (8180000/8);
    public static int VIC6569_CLOCK	= (7880000/8);
    /* pixel clock 8 mhz */
    /* accesses to memory with 2 megahertz */
    /* needs 2 memory accesses for 8 pixel */
    /* + sprite + */
    /* but system clock 1 megahertz */
    /* cpu driven with one (visible screen area) */
/*TODO*///    public static int VIC2_CLOCK(){ return ((vic2.pal?VIC6569_CLOCK:VIC6567_CLOCK)); }

    /* pal 50 Hz vertical screen refresh, screen consists of 312 lines
     * ntsc 60 Hz vertical screen refresh, screen consists of 262 lines */
    public static int VIC6567_LINES = 261;
    public static int VIC6569_LINES = 312;
    public static int VIC2_LINES(){ return (vic2.pal?VIC6569_LINES:VIC6567_LINES); }

}
