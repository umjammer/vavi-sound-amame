/*** T-11: Portable DEC T-11 emulator ******************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.cpu.t11;


public class t11H {
/*TODO*///	enum {
    public static int T11_R0            = 1;
    public static int T11_R1            = 2;
    public static int T11_R2            = 3;
    public static int T11_R3            = 4;
    public static int T11_R4            = 5;
    public static int T11_R5            = 6;
    public static int T11_SP            = 7;
    public static int T11_PC            = 8;
    public static int T11_PSW           = 9;
    public static int T11_IRQ0_STATE    = 10;
    public static int T11_IRQ1_STATE    = 11;
    public static int T11_IRQ2_STATE    = 12;
    public static int T11_IRQ3_STATE    = 13;
    public static int T11_BANK0         = 14;
    public static int T11_BANK1         = 15;
    public static int T11_BANK2         = 16;
    public static int T11_BANK3         = 17;
    public static int T11_BANK4         = 18;
    public static int T11_BANK5         = 19;
    public static int T11_BANK6         = 20;
    public static int T11_BANK7         = 21;
	
    public static int T11_IRQ0          = 0;      /* IRQ0 */
    public static int T11_IRQ1		= 1;	   /* IRQ1 */
    public static int T11_IRQ2		= 2;	   /* IRQ2 */
    public static int T11_IRQ3		= 3;	   /* IRQ3 */

    public static int T11_RESERVED      = 0x000;   /* Reserved vector */
    public static int T11_TIMEOUT       = 0x004;   /* Time-out/system error vector */
    public static int T11_ILLINST       = 0x008;   /* Illegal and reserved instruction vector */
    public static int T11_BPT           = 0x00C;   /* BPT instruction vector */
    public static int T11_IOT           = 0x010;   /* IOT instruction vector */
    public static int T11_PWRFAIL       = 0x014;   /* Power fail vector */
    public static int T11_EMT           = 0x018;   /* EMT instruction vector */
    public static int T11_TRAP          = 0x01C;   /* TRAP instruction vector */

    
    public static class t11_setup
    {
            public int	mode;			/* initial processor mode */
            
            public t11_setup(int mode){
                this.mode = mode;
            }
    };
	
/*TODO*///	
/*TODO*///	/* PUBLIC GLOBALS */
/*TODO*///	
/*TODO*///	
/*TODO*///	/* PUBLIC FUNCTIONS */
/*TODO*///	extern unsigned t11_get_context(void *dst);
/*TODO*///	extern unsigned t11_get_reg(int regnum);
/*TODO*///	extern const char *t11_info(void *context, int regnum);
/*TODO*///	extern unsigned t11_dasm(char *buffer, unsigned pc);
/*TODO*///	
/*TODO*///	/****************************************************************************/
/*TODO*///	/* Read a byte from given memory location                                   */
/*TODO*///	/****************************************************************************/
/*TODO*///	#define T11_RDMEM(A) ((unsigned)cpu_readmem16lew(A))
/*TODO*///	#define T11_RDMEM_WORD(A) ((unsigned)cpu_readmem16lew_word(A))
/*TODO*///	
/*TODO*///	/****************************************************************************/
/*TODO*///	/* Write a byte to given memory location                                    */
/*TODO*///	/****************************************************************************/
/*TODO*///	#define T11_WRMEM(A,V) (cpu_writemem16lew(A,V))
/*TODO*///	#define T11_WRMEM_WORD(A,V) (cpu_writemem16lew_word(A,V))

}
