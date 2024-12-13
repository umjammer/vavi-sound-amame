/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.includes;

import static mess056.vidhrdw.ted7360.ted7360_pal;

public class ted7360H {
    
    /*
     * if you need this chip in another mame/mess emulation than let it me know
     * I will split this from the c16 driver
     * peter.trauner@jk.uni-linz.ac.at
     * 16. november 1999
     * look at mess/systems/c16.c and mess/machine/c16.c
     * on how to use it
     */

    /* call to init videodriver */
    /* pal version */
    /* dma_read: videochip fetched 1 byte data from system bus */
    
    public static int TED7360NTSC_VRETRACERATE  = 60;
    public static int TED7360PAL_VRETRACERATE   = 50;
    public static int TED7360_VRETRACERATE      = (ted7360_pal?TED7360PAL_VRETRACERATE:TED7360NTSC_VRETRACERATE);
    public static int TED7360_HRETRACERATE      = 15625;

    /* to be inserted in MachineDriver-Structure */
    /* the following values depend on the VIC clock,
     * but to achieve TV-frequency the clock must have a fix frequency */
    public static int TED7360_HSIZE             = 320;
    public static int TED7360_VSIZE             = 200;
    /* of course you clock select an other clock, but for accurate */
    /* video timing (these are used in c16/c116/plus4) */
    public static int TED7360NTSC_CLOCK         = (14318180/4);
    public static int TED7360PAL_CLOCK          = (17734470/5);
    /* pixel clock 8 mhz */
    /* accesses to memory with 4 megahertz */
    /* needs 3 memory accesses for 8 pixel */
    /* but system clock 1 megahertz */
    /* cpu driven with one (visible screen area) or two cycles (when configured) */
    public static int TED7360_CLOCK(){ return ((ted7360_pal?TED7360PAL_CLOCK:TED7360NTSC_CLOCK)/4); }

    /* pal 50 Hz vertical screen refresh, screen consists of 312 lines
     * ntsc 60 Hz vertical screen refresh, screen consists of 262 lines */
    public static int TED7360NTSC_LINES         = 261;
    public static int TED7360PAL_LINES          = 312;
    public static int TED7360_LINES             = (ted7360_pal?TED7360PAL_LINES:TED7360NTSC_LINES);

}
