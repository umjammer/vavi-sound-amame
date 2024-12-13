/*****************************************************************************

	h6280.h Portable Hu6280 emulator interface

	Copyright (c) 1999 Bryan McPhail, mish@tendril.co.uk

	This source code is based (with permission!) on the 6502 emulator by
	Juergen Buchmueller.  It is released as part of the Mame emulator project.
	Let me know if you intend to use this code in any other project.

******************************************************************************/
package mame056.cpu.h6280;

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
public class h6280H {
/*TODO*///enum {
    public static final int H6280_PC=1;
    public static final int H6280_S=2;
    public static final int H6280_P=3;
    public static final int H6280_A=4;
    public static final int H6280_X=5;
    public static final int H6280_Y=6;
    public static final int H6280_IRQ_MASK=7;
    public static final int H6280_TIMER_STATE=8;
    public static final int H6280_NMI_STATE=9;
    public static final int H6280_IRQ1_STATE=10;
    public static final int H6280_IRQ2_STATE=11;
    public static final int H6280_IRQT_STATE=12;
/*TODO*///#ifdef MAME_DEBUG
/*TODO*///    ,
/*TODO*///	H6280_M1, H6280_M2, H6280_M3, H6280_M4,
/*TODO*///	H6280_M5, H6280_M6, H6280_M7, H6280_M8
/*TODO*///#endif
/*TODO*///};
/*TODO*///
/*TODO*/////#define LAZY_FLAGS  1
/*TODO*///
    public static final int H6280_INT_NONE = 0;
    public static final int H6280_INT_NMI = 1;
    public static final int H6280_INT_TIMER = 2;
    public static final int H6280_INT_IRQ1 = 3;
    public static final int H6280_INT_IRQ2 = 4;

    public static final int H6280_RESET_VEC = 0xfffe;
    public static final int H6280_NMI_VEC = 0xfffc;
    public static final int H6280_TIMER_VEC = 0xfffa;
    public static final int H6280_IRQ1_VEC = 0xfff8;
    public static final int H6280_IRQ2_VEC = 0xfff6;
    /* Aka BRK vector */    
}
