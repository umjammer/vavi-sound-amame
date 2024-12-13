/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.machine;

/**
 *
 * @author chusogar
 */
public class tiaH {
    /* TIA *Write* Addresses (6 bit) */

    public static final int VSYNC	= 0x00;	/* Vertical Sync Set/Clear              */
    public static final int VBLANK	= 0x01;    /* Vertical Blank Set/Clear	            */
    public static final int WSYNC	= 0x02;    /* Wait for Horizontal Blank            */
    public static final int RSYNC	= 0x03;    /* Reset Horizontal Sync Counter        */
    public static final int NUSIZ0	= 0x04;    /* Number-Size player/missle 0	        */
    public static final int NUSIZ1	= 0x05;    /* Number-Size player/missle 1		    */


    public static final int COLUP0	= 0x06;    /* Color-Luminance Player 0			    */
    public static final int COLUP1	= 0x07;    /* Color-Luminance Player 1				*/
    public static final int COLUPF	= 0x08;    /* Color-Luminance Playfield			*/
    public static final int COLUBK	= 0x09;    /* Color-Luminance BackGround			*/
    /*
    COLUP0, COLUP1, COLUPF, COLUBK:
    These addresses write data into the player, playfield
    and background color-luminance registers:

    COLOR           D7  D6  D5  D4 | D3  D2  D1  LUM
    grey/gold       0   0   0   0  | 0   0   0   black
                    0   0   0   1  | 0   0   1   dark grey
    orange/brt org  0   0   1   0  | 0   1   0
                    0   0   1   1  | 0   1   1   grey
    pink/purple     0   1   0   0  | 1   0   0
                    0   1   0   1  | 1   0   1
    purp/blue/blue  0   1   1   0  | 1   1   0   light grey
                    0   1   1   1  | 1   1   1   white
    blue/lt blue    1   0   0   0
                    1   0   0   1
    torq/grn blue   1   0   1   0
                    1   0   1   1
    grn/yel grn     1   1   0   0
                    1   1   0   1
    org.grn/lt org  1   1   1   0
                    1   1   1   1
    */

    public static final int 	CTRLPF	= 0x0A;    /* Control Playfield, Ball, Collisions	*/
    public static final int 	REFP0	= 0x0B;    /* Reflection Player 0					*/
    public static final int 	REFP1	= 0x0C;    /* Reflection Player 1					*/
    public static final int   PF0	= 0x0D;    /* Playfield Register Byte 0			*/
    public static final int 	PF1	= 0x0E;    /* Playfield Register Byte 1			*/
    public static final int 	PF2	= 0x0F;    /* Playfield Register Byte 2			*/
    public static final int 	RESP0	= 0x10;    /* Reset Player 0						*/
    public static final int 	RESP1	= 0x11;    /* Reset Player 1						*/
    public static final int 	RESM0	= 0x12;    /* Reset Missle 0						*/
    public static final int 	RESM1	= 0x13;    /* Reset Missle 1						*/
    public static final int 	RESBL	= 0x14;    /* Reset Ball							*/

    public static final int 	AUDC0	= 0x15;    /* Audio Control 0						*/
    public static final int 	AUDC1	= 0x16;    /* Audio Control 1						*/
    public static final int 	AUDF0	= 0x17;    /* Audio Frequency 0					*/
    public static final int 	AUDF1	= 0x18;    /* Audio Frequency 1					*/
    public static final int 	AUDV0	= 0x19;    /* Audio Volume 0						*/
    public static final int 	AUDV1	= 0x1A;    /* Audio Volume 1						*/

    /* The next 5 registers are flash registers */
    public static final int 	GRP0	= 0x1B;    /* Graphics Register Player 0			*/
    public static final int 	GRP1	= 0x1C;    /* Graphics Register Player 0			*/
    public static final int 	ENAM0	= 0x1D;    /* Graphics Enable Missle 0				*/
    public static final int 	ENAM1	= 0x1E;    /* Graphics Enable Missle 1				*/
    public static final int 	ENABL	= 0x1F;    /* Graphics Enable Ball					*/


    public static final int   HMP0	= 0x20;    /* Horizontal Motion Player 0			*/
    public static final int 	HMP1	= 0x21;    /* Horizontal Motion Player 0			*/
    public static final int 	HMM0	= 0x22;    /* Horizontal Motion Missle 0			*/
    public static final int 	HMM1	= 0x23;    /* Horizontal Motion Missle 1			*/
    public static final int 	HMBL	= 0x24;    /* Horizontal Motion Ball				*/
    public static final int 	VDELP0	= 0x25;    /* Vertical Delay Player 0				*/
    public static final int 	VDELP1	= 0x26;    /* Vertical Delay Player 1				*/
    public static final int 	VDELBL	= 0x27;    /* Vertical Delay Ball					*/
    public static final int 	RESMP0	= 0x28;    /* Reset Missle 0 to Player 0			*/
    public static final int 	RESMP1	= 0x29;    /* Reset Missle 1 to Player 1			*/
    public static final int 	HMOVE	= 0x2A;    /* Apply Horizontal Motion				*/
    public static final int 	HMCLR	= 0x2B;    /* Clear Horizontal Move Registers		*/
    public static final int 	CXCLR	= 0x2C;    /* Clear Collision Latches				*/

    /* TIA *Read* Addresses */
                            /*          bit 6  bit 7				*/
    public static final int 	CXM0P	= 0x00;	/* Read Collision M0-P1  M0-P0			*/
    public static final int 	CXM1P	= 0x01;    /*                M1-P0  M1-P1			*/
    public static final int 	CXP0FB	= 0x02;	/*                P0-PF  P0-BL			*/
    public static final int 	CXP1FB	= 0x03;    /*                P1-PF  P1-BL			*/
    public static final int 	CXM0FB	= 0x04;    /*                M0-PF  M0-BL			*/
    public static final int 	CXM1FB	= 0x05;    /*                M1-PF  M1-BL			*/
    public static final int 	CXBLPF	= 0x06;    /*                BL-PF  -----			*/
    public static final int 	CXPPMM	= 0x07;    /*                P0-P1  M0-M1			*/
    public static final int 	INPT0	= 0x08;    /* Read Pot Port 0						*/
    public static final int 	INPT1	= 0x09;    /* Read Pot Port 1						*/
    public static final int 	INPT2	= 0x0A;    /* Read Pot Port 2						*/
    public static final int 	INPT3	= 0x0B;    /* Read Pot Port 3						*/
    public static final int 	INPT4	= 0x0C;    /* Read Input (Trigger) 0				*/
    public static final int 	INPT5	= 0x0D;    /* Read Input (Trigger) 1				*/




    /* keep a record of the colors here */
    public static class COLREG {
            public int P0; /* player 0   */
            public int M0;	/* missile 0  */
            public int P1;	/* player 1   */
            public int M1;	/* missile 1  */
            public int PF;	/* playfield  */
            public int BL;	/* ball       */
            public int BK;	/* background */
    };


    /* keep a record of the playfield registers here */
    public static class PFREG {
            public int B0; /* 8 bits, only left 4 bits used */
            public int B1; /* 8 bits  */
            public int B2;	/* 8 bits  */
    };

    public static int[] scanline_registers = new int[80]; /* array to hold info on who gets displayed */



    public static class TIA
    {
        public COLREG colreg    = new COLREG();			/* keep a record of the color registers written to */
        public PFREG  pfreg     = new PFREG();			/* keep a record of the playfield registers written to */
    };
    
}
