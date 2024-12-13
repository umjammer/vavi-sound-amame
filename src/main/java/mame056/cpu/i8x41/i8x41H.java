package mame056.cpu.i8x41;

/**
 *
 * @author chusogar
 */
public class i8x41H {
    
    /* Note:
     * I8X41_DATA is A0 = 0 and R/W
     * I8X41_CMND is A0 = 1 and W only
     * I8X41_STAT is A0 = 1 and R only
     */
    public static final int I8X41_PC    = 1;
    public static final int I8X41_SP    = 2;
    public static final int I8X41_PSW   = 3;
    public static final int I8X41_A     = 4;
    public static final int I8X41_T     = 5;
    public static final int I8X41_DATA  = 6;
    public static final int I8X41_CMND  = 7;
    public static final int I8X41_STAT  = 8;
    public static final int I8X41_R0    = 9;
    public static final int I8X41_R1    = 10;
    public static final int I8X41_R2    = 11;
    public static final int I8X41_R3    = 12;
    public static final int I8X41_R4    = 13;
    public static final int I8X41_R5    = 14;
    public static final int I8X41_R6    = 15;
    public static final int I8X41_R7    = 16;

    public static final int I8X41_INT_IBF   = 0;	/* input buffer full interrupt */
    public static final int I8X41_INT_TEST0 = 1;	/* test0 line */
    public static final int I8X41_INT_TEST1 = 2;	/* test1 line (also counter interrupt; taken on cntr overflow)	*/

    
}
