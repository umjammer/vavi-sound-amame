
package mame056.cpu.hd6309;

import static mame056.cpu.hd6309.hd6309.CC_Z;
import static mame056.cpu.hd6309.hd6309.CC_N;
import static mame056.cpu.hd6309.hd6309.CC_V;

public class _6309tbl {
    
    public static int flags8i[]=	 /* increment */
    {
    CC_Z,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    CC_N|CC_V,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N
    };
    
    public static int flags8d[]= /* decrement */
    {
    CC_Z,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
    0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,CC_V,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,
    CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N,CC_N
    };

    public static int index_cycle_em[] = {        /* Index Loopup cycle counts, emulated 6809 */
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */

    /* 0x0X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x1X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x2X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x3X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x4X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x5X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x6X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x7X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x8X */      2,    3,    2,    3,    0,    1,    1,    1,    1,    4,    1,    4,    1,    5,    4,    0,
    /* 0x9X */      3,    6,    5,    6,    3,    4,    4,    4,    4,    7,    4,    7,    4,    8,    7,    5,
    /* 0xAX */      2,    3,    2,    3,    0,    1,    1,    1,    1,    4,    1,    4,    1,    5,    4,    5,
    /* 0xBX */      5,    6,    5,    6,    3,    4,    4,    4,    4,    7,    4,    7,    4,    8,    7,    8,
    /* 0xCX */      2,    3,    2,    3,    0,    1,    1,    1,    1,    4,    1,    4,    1,    5,    4,    3,
    /* 0xDX */      4,    6,    5,    6,    3,    4,    4,    4,    4,    7,    4,    7,    4,    8,    7,    8,
    /* 0xEX */      2,    3,    2,    3,    0,    1,    1,    1,    1,    4,    1,    4,    1,    5,    4,    3,
    /* 0xFX */      4,    6,    5,    6,    3,    4,    4,    4,    4,    7,    4,    7,    4,    8,    7,    8
    };

    public static int index_cycle_na[] = {         /* Index Loopup cycle counts, native 6309 */
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */

    /* 0x0X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x1X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x2X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x3X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x4X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x5X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x6X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x7X */      1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    /* 0x8X */      1,    2,    1,    3,    0,    1,    1,    1,    1,    3,    1,    2,    1,    3,    1,    0,
    /* 0x9X */      3,    5,    4,    5,    3,    4,    4,    4,    4,    5,    4,    7,    4,    6,    5,    5,
    /* 0xAX */      1,    2,    1,    2,    0,    1,    1,    1,    1,    3,    1,    2,    1,    3,    1,    2,
    /* 0xBX */      5,    5,    4,    5,    3,    4,    4,    4,    4,    7,    4,    5,    4,    6,    4,    7,
    /* 0xCX */      1,    2,    1,    2,    0,    1,    1,    1,    1,    3,    1,    2,    1,    3,    1,    1,
    /* 0xDX */      4,    5,    4,    5,    3,    4,    4,    4,    4,    7,    4,    5,    4,    6,    4,    7,
    /* 0xEX */      1,    2,    1,    2,    0,    1,    1,    1,    1,    3,    1,    2,    1,    3,    1,    1,
    /* 0xFX */      4,    5,    4,    5,    3,    4,    4,    4,    4,    7,    4,    5,    4,    6,    4,    7
    };

    public static int IIP0	= 19;			/* Illegal instruction cycle count page 0 */
    public static int IIP1	= 20;			/* Illegal instruction cycle count page 01 & 11 */

    public static int ccounts_page0_em[] =    /* Cycle Counts Page zero, Emulated 6809 */
    {
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */
    /* 0x0X */     6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    3,    6,
    /* 0x1X */     0,    0,    2,    2,    4, IIP0,    5,    9, IIP0,    2,    3, IIP0,    3,    2,    8,    6,
    /* 0x2X */     3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,
    /* 0x3X */     4,    4,    4,    4,    5,    5,    6,    5, IIP0,    5,    3,    6,   22,   11, IIP0,   19,
    /* 0x4X */     2, IIP0, IIP0,    2,    2, IIP0,    2,    2,    2,    2,    2, IIP0,    2,    2, IIP0,    2,
    /* 0x5X */     2, IIP0, IIP0,    2,    2, IIP0,    2,    2,    2,    2,    2, IIP0,    2,    2, IIP0,    2,
    /* 0x6X */     6,    7,    7,    6,    6,    7,    6,    6,    6,    6,    6,    7,    6,    6,    3,    6,
    /* 0x7X */     7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    4,    7,
    /* 0x8X */     2,    2,    2,    4,    2,    2,    2, IIP0,    2,    2,    2,    2,    4,    7,    3, IIP0,
    /* 0x9X */     4,    4,    4,    6,    4,    4,    4,    4,    4,    4,    4,    4,    6,    7,    5,    5,
    /* 0xAX */     4,    4,    4,    6,    4,    4,    4,    4,    4,    4,    4,    4,    6,    7,    5,    5,
    /* 0xBX */     5,    5,    5,    7,    5,    5,    5,    5,    5,    5,    5,    5,    7,    8,    6,    6,
    /* 0xCX */     2,    2,    2,    4,    2,    2,    2, IIP0,    2,    2,    2,    2,    3,    5,    3, IIP0,
    /* 0xDX */     4,    4,    4,    6,    4,    4,    4,    4,    4,    4,    4,    4,    5,    5,    5,    5,
    /* 0xEX */     4,    4,    4,    6,    4,    4,    4,    4,    4,    4,    4,    4,    5,    5,    5,    5,
    /* 0xFX */     5,    5,    5,    7,    5,    5,    5,    5,    5,    5,    5,    5,    6,    6,    6,    6
    };

    public static int ccounts_page0_na[] =   /* Cycle Counts Page zero, Native 6309 */
    {
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */
    /* 0x0X */     5,    6,    6,    5,    5,    6,    5,    5,    5,    5,    5,    6,    5,    4,    2,    5,
    /* 0x1X */     0,    0,    1,    1,    4, IIP0,    4,    7, IIP0,    1,    2, IIP0,    3,    1,    5,    4,
    /* 0x2X */     3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,
    /* 0x3X */     4,    4,    4,    4,    4,    4,    4,    4, IIP0,    4,    1,    6,   20,   10, IIP0,   21,
    /* 0x4X */     1, IIP0, IIP0,    1,    1, IIP0,    1,    1,    1,    1,    1, IIP0,    1,    1, IIP0,    1,
    /* 0x5X */     1, IIP0, IIP0,    1,    1, IIP0,    1,    1,    1,    1,    1, IIP0,    1,    1, IIP0,    1,
    /* 0x6X */     6,    7,    7,    6,    6,    7,    6,    6,    6,    6,    6,    7,    6,    5,    3,    6,
    /* 0x7X */     6,    7,    7,    6,    6,    7,    6,    6,    6,    6,    6,    7,    6,    5,    3,    6,
    /* 0x8X */     2,    2,    2,    3,    2,    2,    2, IIP0,    2,    2,    2,    2,    3,    6,    3, IIP0,
    /* 0x9X */     3,    3,    3,    4,    3,    3,    3,    3,    3,    3,    3,    3,    4,    6,    4,    4,
    /* 0xAX */     4,    4,    4,    5,    4,    4,    4,    4,    4,    4,    4,    4,    5,    6,    5,    5,
    /* 0xBX */     4,    4,    4,    5,    4,    4,    4,    4,    4,    4,    4,    4,    5,    7,    5,    5,
    /* 0xCX */     2,    2,    2,    3,    2,    2,    2, IIP0,    2,    2,    2,    2,    3,    5,    3, IIP0,
    /* 0xDX */     3,    3,    3,    4,    3,    3,    3,    3,    3,    3,    3,    3,    4,    4,    4,    4,
    /* 0xEX */     4,    4,    4,    5,    4,    4,    4,    4,    4,    4,    4,    4,    5,    5,    5,    5,
    /* 0xFX */     4,    4,    4,    5,    4,    4,    4,    4,    4,    4,    4,    4,    5,    5,    5,    5
    };

    public static int ccounts_page01_em[] =    /* Cycle Counts Page 01, Emulated 6809 */
    {
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */
    /* 0x0X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x1X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x2X */   IIP1,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,
    /* 0x3X */      4,    4,    4,    4,    4,    4,    4,    4,    6,    6,    6,    6, IIP1, IIP1, IIP1,   20,
    /* 0x4X */      3,  IIP1,IIP1,    3,    3, IIP1,    3,    3,    3,    3,    3, IIP1,    3,    3, IIP1,    3,
    /* 0x5X */   IIP1, IIP1, IIP1,    3,    3, IIP1,    3, IIP1, IIP1,    3,    3, IIP1,    3,    3, IIP1,    3,
    /* 0x6X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x7X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x8X */      5,    5,    5,    5,    5,    5,    4, IIP1,    5,    5,    5,    5,    5, IIP1,    5, IIP1,
    /* 0x9X */      7,    7,    7,    7,    7,    7,    6,    6,    7,    7,    7,    7,    7, IIP1,    6,    6,
    /* 0xAX */      7,    7,    7,    7,    7,    7,    6,    6,    7,    7,    7,    7,    7, IIP1,    6,    6,
    /* 0xBX */      8,    8,    8,    8,    8,    8,    7,    7,    8,    8,    8,    8,    8, IIP1,    7,    7,
    /* 0xCX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    4, IIP1,
    /* 0xDX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    8,    8,    6,    6,
    /* 0xEX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    8,    8,    6,    6,
    /* 0xFX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    9,    9,    7,    7
    };

    public static int ccounts_page01_na[] =   /* Cycle Counts Page 01, Native 6309 */
    {
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */
    /* 0x0X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x1X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x2X */   IIP1,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,
    /* 0x3X */      4,    4,    4,    4,    4,    4,    4,    4,    6,    6,    6,    6, IIP1, IIP1, IIP1,   20,
    /* 0x4X */      2, IIP1, IIP1,    2,    2, IIP1,    2,    2,    2,    2,    2, IIP1,    2,    2, IIP1,    2,
    /* 0x5X */   IIP1, IIP1, IIP1,    2,    2, IIP1,    2, IIP1, IIP1,    2,    2, IIP1,    2,    2, IIP1,    2,
    /* 0x6X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x7X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x8X */      4,    4,    4,    4,    4,    4,    4, IIP1,    4,    4,    4,    4,    4, IIP1,    4, IIP1,
    /* 0x9X */      5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5,    5, IIP1,    5,    5,
    /* 0xAX */      6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6, IIP1,    6,    6,
    /* 0xBX */      6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6,    6, IIP1,    6,    6,
    /* 0xCX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    4, IIP1,
    /* 0xDX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    7,    7,    5,    5,
    /* 0xEX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    8,    8,    6,    6,
    /* 0xFX */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    8,    8,    6,    6
    };

    public static int ccounts_page11_em[] =    /* Cycle Counts Page 11, Emulated 6809 */
    {
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */
    /* 0x0X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x1X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x2X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x3X */      7,    7,    7,    7,    7,    7,    7,    8,    3,    3,    3,    3,    4,    5, IIP1,   20,
    /* 0x4X */   IIP1, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    3, IIP1,    3,    3, IIP1,    3,
    /* 0x5X */   IIP1, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    3, IIP1,    3,    3, IIP1,    3,
    /* 0x6X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x7X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x8X */      3,    3, IIP1,    5, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1,    3,    5,   25,   34,   28,
    /* 0x9X */      5,    5, IIP1,    7, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5,    7,   27,   36,   30,
    /* 0xAX */      5,    5, IIP1,    7, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5,    7,   27,   36,   30,
    /* 0xBX */      6,    6, IIP1,    8, IIP1, IIP1,    6,    6, IIP1, IIP1, IIP1,    6,    8,   28,   37,   31,
    /* 0xCX */      3,    3, IIP1, IIP1, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1,
    /* 0xDX */      5,    5, IIP1, IIP1, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5, IIP1, IIP1, IIP1, IIP1,
    /* 0xEX */      5,    5, IIP1, IIP1, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5, IIP1, IIP1, IIP1, IIP1,
    /* 0xFX */      6,    6, IIP1, IIP1, IIP1, IIP1,    6,    6, IIP1, IIP1, IIP1,    6, IIP1, IIP1, IIP1, IIP1
    };

    public static int ccounts_page11_na[] =    /* Cycle Counts Page 11, Native 6309 */
    {
    /*	         0xX0, 0xX1, 0xX2, 0xX3, 0xX4, 0xX5, 0xX6, 0xX7, 0xX8, 0xX9, 0xXA, 0xXB, 0xXC, 0xXD, 0xXE, 0xXF */
    /* 0x0X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x1X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x2X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x3X */      6,    6,    6,    6,    6,    6,    6,    7,    3,    3,    3,    3,    4,    5, IIP1,   20,
    /* 0x4X */   IIP1, IIP1, IIP1,    2, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    2, IIP1,    2,    2, IIP1,    2,
    /* 0x5X */   IIP1, IIP1, IIP1,    2, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,    2, IIP1,    2,    2, IIP1,    2,
    /* 0x6X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x7X */   IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1, IIP1,
    /* 0x8X */      3,    3, IIP1,    4, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1,    3,    4,   25,   34,   28,
    /* 0x9X */      4,    4, IIP1,    5, IIP1, IIP1,    4,    4, IIP1, IIP1, IIP1,    4,    5,   26,   35,   29,
    /* 0xAX */      5,    5, IIP1,    6, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5,    6,   27,   36,   30,
    /* 0xBX */      5,    5, IIP1,    6, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5,    6,   27,   36,   30,
    /* 0xCX */      3,    3, IIP1, IIP1, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1,    3, IIP1, IIP1, IIP1, IIP1,
    /* 0xDX */      4,    4, IIP1, IIP1, IIP1, IIP1,    4,    4, IIP1, IIP1, IIP1,    4, IIP1, IIP1, IIP1, IIP1,
    /* 0xEX */      5,    5, IIP1, IIP1, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5, IIP1, IIP1, IIP1, IIP1,
    /* 0xFX */      5,    5, IIP1, IIP1, IIP1, IIP1,    5,    5, IIP1, IIP1, IIP1,    5, IIP1, IIP1, IIP1, IIP1
    };

/*TODO*///    #ifndef BIG_SWITCH
/*TODO*///
/*TODO*///    static void (*hd6309_main[0x100])(void) = {
/*TODO*///    /*	        0xX0,   0xX1,     0xX2,    0xX3,    0xX4,    0xX5,    0xX6,    0xX7,
/*TODO*///                            0xX8,   0xX9,     0xXA,    0xXB,    0xXC,    0xXD,    0xXE,    0xXF   */
/*TODO*///
/*TODO*///    /* 0x0X */  neg_di,  oim_di,  aim_di,  com_di,  lsr_di,  eim_di,  ror_di,  asr_di,
/*TODO*///                asl_di,  rol_di,  dec_di,  tim_di,  inc_di,  tst_di,  jmp_di,  clr_di,
/*TODO*///
/*TODO*///    /* 0x1X */  pref10,  pref11,  nop,     sync,    sexw,    IIError, lbra,    lbsr,
/*TODO*///                IIError, daa,     orcc,    IIError, andcc,   sex,     exg,     tfr,
/*TODO*///
/*TODO*///    /* 0x2X */  bra,     brn,     bhi,     bls,     bcc,     bcs,     bne,     beq,
/*TODO*///                bvc,     bvs,     bpl,     bmi,     bge,     blt,     bgt,     ble,
/*TODO*///
/*TODO*///    /* 0x3X */  leax,    leay,    leas,    leau,    pshs,    puls,    pshu,    pulu,
/*TODO*///                IIError, rts,     abx,     rti,     cwai,    mul,     IIError, swi,
/*TODO*///
/*TODO*///    /* 0x4X */  nega,    IIError, IIError, coma,    lsra,    IIError, rora,    asra,
/*TODO*///                asla,    rola,    deca,    IIError, inca,    tsta,    IIError, clra,
/*TODO*///
/*TODO*///    /* 0x5X */  negb,    IIError, IIError, comb,    lsrb,    IIError, rorb,    asrb,
/*TODO*///                aslb,    rolb,    decb,    IIError, incb,    tstb,    IIError, clrb,
/*TODO*///
/*TODO*///    /* 0x6X */  neg_ix,  oim_ix,  aim_ix,  com_ix,  lsr_ix,  eim_ix,  ror_ix,  asr_ix,
/*TODO*///                asl_ix,  rol_ix,  dec_ix,  tim_ix,  inc_ix,  tst_ix,  jmp_ix,  clr_ix,
/*TODO*///
/*TODO*///    /* 0x7X */  neg_ex,  oim_ex,  aim_ex,  com_ex,  lsr_ex,  eim_ex,  ror_ex,  asr_ex,
/*TODO*///                asl_ex,  rol_ex,  dec_ex,  tim_ex,  inc_ex,  tst_ex,  jmp_ex,  clr_ex,
/*TODO*///
/*TODO*///    /* 0x8X */  suba_im, cmpa_im, sbca_im, subd_im, anda_im, bita_im, lda_im,  IIError,
/*TODO*///                eora_im, adca_im, ora_im,  adda_im, cmpx_im, bsr,     ldx_im,  IIError,
/*TODO*///
/*TODO*///    /* 0x9X */  suba_di, cmpa_di, sbca_di, subd_di, anda_di, bita_di, lda_di,  sta_di,
/*TODO*///                eora_di, adca_di, ora_di,  adda_di, cmpx_di, jsr_di,  ldx_di,  stx_di,
/*TODO*///
/*TODO*///    /* 0xAX */  suba_ix, cmpa_ix, sbca_ix, subd_ix, anda_ix, bita_ix, lda_ix,  sta_ix,
/*TODO*///                eora_ix, adca_ix, ora_ix,  adda_ix, cmpx_ix, jsr_ix,  ldx_ix,  stx_ix,
/*TODO*///
/*TODO*///    /* 0xBX */  suba_ex, cmpa_ex, sbca_ex, subd_ex, anda_ex, bita_ex, lda_ex,  sta_ex,
/*TODO*///                eora_ex, adca_ex, ora_ex,  adda_ex, cmpx_ex, jsr_ex,  ldx_ex,  stx_ex,
/*TODO*///
/*TODO*///    /* 0xCX */  subb_im, cmpb_im, sbcb_im, addd_im, andb_im, bitb_im, ldb_im,  IIError,
/*TODO*///                eorb_im, adcb_im, orb_im,  addb_im, ldd_im,  ldq_im,  ldu_im,  IIError,
/*TODO*///
/*TODO*///    /* 0xDX */  subb_di, cmpb_di, sbcb_di, addd_di, andb_di, bitb_di, ldb_di,  stb_di,
/*TODO*///                eorb_di, adcb_di, orb_di,  addb_di, ldd_di,  std_di,  ldu_di,  stu_di,
/*TODO*///
/*TODO*///    /* 0xEX */  subb_ix, cmpb_ix, sbcb_ix, addd_ix, andb_ix, bitb_ix, ldb_ix,  stb_ix,
/*TODO*///                eorb_ix, adcb_ix, orb_ix,  addb_ix, ldd_ix,  std_ix,  ldu_ix,  stu_ix,
/*TODO*///
/*TODO*///    /* 0xFX */  subb_ex, cmpb_ex, sbcb_ex, addd_ex, andb_ex, bitb_ex, ldb_ex,  stb_ex,
/*TODO*///                eorb_ex, adcb_ex, orb_ex,  addb_ex, ldd_ex,  std_ex,  ldu_ex,  stu_ex
/*TODO*///    };
/*TODO*///
/*TODO*///    static void (*hd6309_page01[0x100])(void) = {
/*TODO*///    /*	        0xX0,   0xX1,     0xX2,    0xX3,    0xX4,    0xX5,    0xX6,    0xX7,
/*TODO*///                            0xX8,   0xX9,     0xXA,    0xXB,    0xXC,    0xXD,    0xXE,    0xXF   */
/*TODO*///
/*TODO*///    /* 0x0X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x1X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x2X */  IIError, lbrn,    lbhi,    lbls,    lbcc,    lbcs,    lbne,    lbeq,
/*TODO*///                            lbvc,    lbvs,    lbpl,    lbmi,    lbge,    lblt,    lbgt,    lble,
/*TODO*///
/*TODO*///    /* 0x3X */  addr_r,  adcr,    subr,    sbcr,    andr,    orr,     eorr,    cmpr,
/*TODO*///                            pshsw,   pulsw,   pshuw,   puluw,   IIError, IIError, IIError, swi2,
/*TODO*///
/*TODO*///    /* 0x4X */  negd,    IIError, IIError, comd,    lsrd,    IIError, rord,    asrd,
/*TODO*///                            asld,    rold,    decd,    IIError, incd,    tstd,    IIError, clrd,
/*TODO*///
/*TODO*///    /* 0x5X */  IIError, IIError, IIError, comw,    lsrw,    IIError, rorw,    IIError,
/*TODO*///                            IIError, rolw,    decw,    IIError, incw,    tstw,    IIError, clrw,
/*TODO*///
/*TODO*///    /* 0x6X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x7X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x8X */  subw_im, cmpw_im, sbcd_im, cmpd_im, andd_im, bitd_im, ldw_im,  IIError,
/*TODO*///                            eord_im, adcd_im, ord_im,  addw_im, cmpy_im, IIError, ldy_im,  IIError,
/*TODO*///
/*TODO*///    /* 0x9X */  subw_di, cmpw_di, sbcd_di, cmpd_di, andd_di, bitd_di, ldw_di,  stw_di,
/*TODO*///                            eord_di, adcd_di, ord_di,  addw_di, cmpy_di, IIError, ldy_di,  sty_di,
/*TODO*///
/*TODO*///    /* 0xAX */  subw_ix, cmpw_ix, sbcd_ix, cmpd_ix, andd_ix, bitd_ix, ldw_ix,  stw_ix,
/*TODO*///                            eord_ix, adcd_ix, ord_ix,  addw_ix, cmpy_ix, IIError, ldy_ix,  sty_ix,
/*TODO*///
/*TODO*///    /* 0xBX */  subw_ex, cmpw_ex, sbcd_ex, cmpd_ex, andd_ex, bitd_ex, ldw_ex,  stw_ex,
/*TODO*///                            eord_ex, adcd_ex, ord_ex,  addw_ex, cmpy_ex, IIError, ldy_ex,  sty_ex,
/*TODO*///
/*TODO*///    /* 0xCX */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, lds_im,  IIError,
/*TODO*///
/*TODO*///    /* 0xDX */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, ldq_di,  stq_di,  lds_di,  sts_di,
/*TODO*///
/*TODO*///    /* 0xEX */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, ldq_ix,  stq_ix,  lds_ix,  sts_ix,
/*TODO*///
/*TODO*///    /* 0xFX */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, ldq_ex,  stq_ex,  lds_ex,  sts_ex
/*TODO*///    };
/*TODO*///    static void (*hd6309_page11[0x100])(void) = {
/*TODO*///    /*	        0xX0,   0xX1,     0xX2,    0xX3,    0xX4,    0xX5,    0xX6,    0xX7,
/*TODO*///                            0xX8,   0xX9,     0xXA,    0xXB,    0xXC,    0xXD,    0xXE,    0xXF   */
/*TODO*///
/*TODO*///    /* 0x0X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x1X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x2X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x3X */  band,    biand,   bor,     bior,    beor,    bieor,   ldbt,    stbt,
/*TODO*///                            tfmpp,   tfmmm,   tfmpc,   tfmcp,   bitmd_im,ldmd_im, IIError, swi3,
/*TODO*///
/*TODO*///    /* 0x4X */  IIError, IIError, IIError, come,    IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, dece,    IIError, ince,    tste,    IIError, clre,
/*TODO*///
/*TODO*///    /* 0x5X */  IIError, IIError, IIError, comf,    IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, decf,    IIError, incf,    tstf,    IIError, clrf,
/*TODO*///
/*TODO*///    /* 0x6X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x7X */  IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///                            IIError, IIError, IIError, IIError, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0x8X */  sube_im, cmpe_im, IIError, cmpu_im, IIError, IIError, lde_im,  IIError,
/*TODO*///                            IIError, IIError, IIError, adde_im, cmps_im, divd_im, divq_im, muld_im,
/*TODO*///
/*TODO*///    /* 0x9X */  sube_di, cmpe_di, IIError, cmpu_di, IIError, IIError, lde_di,  ste_di,
/*TODO*///                            IIError, IIError, IIError, adde_di, cmps_di, divd_di, divq_di, muld_di,
/*TODO*///
/*TODO*///    /* 0xAX */  sube_ix, cmpe_ix, IIError, cmpu_ix, IIError, IIError, lde_ix,  ste_ix,
/*TODO*///                            IIError, IIError, IIError, adde_ix, cmps_ix, divd_ix, divq_ix, muld_ix,
/*TODO*///
/*TODO*///    /* 0xBX */  sube_ex, cmpe_ex, IIError, cmpu_ex, IIError, IIError, lde_ex,  ste_ex,
/*TODO*///                            IIError, IIError, IIError, adde_ex, cmps_ex, divd_ex, divq_ex, muld_ex,
/*TODO*///
/*TODO*///    /* 0xCX */  subf_im, cmpf_im, IIError, IIError, IIError, IIError, ldf_im,  IIError,
/*TODO*///                            IIError, IIError, IIError, addf_im, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0xDX */  subf_di, cmpf_di, IIError, IIError, IIError, IIError, ldf_di,  stf_di,
/*TODO*///                            IIError, IIError, IIError, addf_di, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0xEX */  subf_ix, cmpf_ix, IIError, IIError, IIError, IIError, ldf_ix,  stf_ix,
/*TODO*///                            IIError, IIError, IIError, addf_ix, IIError, IIError, IIError, IIError,
/*TODO*///
/*TODO*///    /* 0xFX */  subf_ex, cmpf_ex, IIError, IIError, IIError, IIError, ldf_ex,  stf_ex,
/*TODO*///                            IIError, IIError, IIError, addf_ex, IIError, IIError, IIError, IIError
/*TODO*///
/*TODO*///    };
/*TODO*///
/*TODO*///    #endif /* BIG_SWITCH */
    
}
