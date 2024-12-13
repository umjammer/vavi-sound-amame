package mame056.cpu.i8x41;

import static arcadeflex056.fucPtr.*;
import common.libc.cstdio.FILE;
import static common.libc.cstdio.fprintf;
import common.ptr.UBytePtr;
import static mame056.common.memory_region;
import static mame056.commonH.REGION_CPU1;
import static mame056.cpu.h6280.h6280H.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static arcadeflex056.osdepend.logerror;
import static mame056.cpu.i8x41.i8x41H.*;

/**
 *
 * @author chusogar
 */
public class i8x41  extends cpu_interface {
    
    public i8x41(){
        //	CPU0(I8X41,    i8x41,	 1,  0,1.00,I8X41_INT_IBF,  8, 16,	  0,16,LE,1, 2	),
        cpu_num = CPU_I8X41;
        num_irqs = 1;
        default_vector = 0;
        icount = i8x41_ICount;
        overclock = 1.00;
        irq_int = I8X41_INT_IBF;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 2;
    }

    @Override
    public void init() {
        i8x41_init();
    }

    @Override
    public void reset(Object param) {
        i8x41_reset(param);
    }

    @Override
    public void exit() {
        i8x41_exit();
    }

    @Override
    public int execute(int cycles) {
        return i8x41_execute(cycles);
    }

    @Override
    public Object init_context() {
        Object reg = new I8X41_Regs();
        return reg;
    }

    @Override
    public Object get_context() {
        return i8x41_get_context();
    }

    @Override
    public void set_context(Object reg) {
        i8x41_set_context(reg);
    }

    @Override
    public int[] get_cycle_table(int which) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String cpu_info(Object context, int regnum) {
        return i8x41_info(context, regnum);
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int get_reg(int regnum) {
        return i8x41_get_reg(regnum);
    }

    @Override
    public void set_reg(int regnum, int val) {
        i8x41_set_reg(regnum, val);
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        i8x41_set_irq_line(irqline, linestate);
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        i8x41_set_irq_callback(callback);
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int memory_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void memory_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int internal_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void internal_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_op_base(int pc) {
        cpu_setOPbase16.handler(pc);
    }

    @Override
    public int mem_address_bits_of_cpu() {
        return 16;
    }
    
    public static class I8X41_Regs {
	int	ppc;
	int	pc;
	int	timer;
	int	subtype;
	int	a;
	int	psw;
	int	state;
	int	tovf;
	int	enable;
	int	dbbi;
	int	dbbo;
	UBytePtr	ram;
        public irqcallbacksPtr irq_callback;
    };

    public static int[] i8x41_ICount = new int[1];
    
    static I8X41_Regs i8x41 = new I8X41_Regs();

/*TODO*////* Layout of the registers in the debugger */
/*TODO*///static UINT8 i8x41_reg_layout[] = {
/*TODO*///	I8X41_PC, I8X41_SP, I8X41_PSW, I8X41_A, I8X41_T, I8X41_DATA, I8X41_CMND, -1,
/*TODO*///	I8X41_R0, I8X41_R1, I8X41_R2, I8X41_R3, I8X41_R4, I8X41_R5, I8X41_R6, I8X41_R7, 0
/*TODO*///};
/*TODO*///
/*TODO*////* Layout of the debugger windows x,y,w,h */
/*TODO*///static UINT8 i8x41_win_layout[] = {
/*TODO*///	 0, 0,80, 2,	/* register window (top rows) */
/*TODO*///	 0, 3,24,19,	/* disassembler window (left colums) */
/*TODO*///	25, 3,55, 9,	/* memory #1 window (right, upper middle) */
/*TODO*///	25,13,55, 9,	/* memory #2 window (right, lower middle) */
/*TODO*///	 0,23,80, 1,	/* command line window (bottom rows) */
/*TODO*///};

    public static int RM(int a){ return	cpu_readmem16(a); }
    public static void WM(int a, int v){ cpu_writemem16(a,v); }
    public static int ROP(int pc){ return cpu_readop(pc); }
    public static int ROP_ARG(int pc){ return cpu_readop_arg(pc); }

    /* PC vectors */
    public static final int V_RESET = 0x000;	/* power on address */
    public static final int V_IBF   = 0x003;	/* input buffer full interrupt vector */
    public static final int V_TIMER = 0x007;	/* timer/counter interrupt vector */

    /*
     * Memory locations
     * Note:
     * 000-3ff		internal ROM for 8x41 (1K)
     * 400-7ff		(more) internal for 8x42 type (2K)
     * 800-8ff		internal RAM
     */
    public static final int M_IRAM  = 0x800;	/* internal RAM is mapped here */
    public static final int M_BANK0 = 0x800;	/* register bank 0 (8 times 8 bits) */
    public static final int M_STACK = 0x808;	/* stack (8 times 16 bits) */
    public static final int M_BANK1 = 0x818;	/* register bank 1 (8 times 8 bits) */
    public static final int M_USER  = 0x820;	/* user memory (224 times 8 bits) */

    /* PSW flag bits */
    public static final int FC		= 0x80;	/* carry flag */
    public static final int FA		= 0x40;	/* auxiliary carry flag */
    public static final int F0		= 0x20;	/* flag 0 */
    public static final int F1		= 0x10;	/* flag 1 (also used as bank select!?) */
    public static final int F3		= 0x08;	/* unused */
    public static final int SP		= 0x07;	/* lower three bits are used as stack pointer */

    /* STATE flag bits */
    public static final int OBF 	= 0x01;	/* output buffer full */
    public static final int IBF 	= 0x02;	/* input buffer full */
    public static final int TEST0	= 0x04;	/* test0 line */
    public static final int TEST1	= 0x08;	/* test1 line */

    /* ENABLE flag bits */
    public static int IBFI	= 0x01;	/* input buffer full interrupt */
    public static int TCNTI	= 0x02;	/* timer/counter interrupt */
    public static int DMA 	= 0x04;	/* DMA mode */
    public static int FLAGS	= 0x08;	/* FLAGS mode */
    public static int T		= 0x10;	/* timer */
    public static int CNT 	= 0x20;	/* counter */

/*TODO*////* shorter names for the I8x41 structure elements */
/*TODO*///#define PPC 	i8x41.ppc
/*TODO*///#define PC		i8x41.pc
/*TODO*///#define A		i8x41.a
/*TODO*///#define PSW 	i8x41.psw
/*TODO*///#define DBBI	i8x41.dbbi
/*TODO*///#define DBBO	i8x41.dbbo
    public static int R(int n){ return i8x41.ram.read(((i8x41.psw & F1)!=0 ? M_BANK1:M_BANK0)+(n)); }    
    public static void set_R(int n, int newval){ i8x41.ram.write(((i8x41.psw & F1)!=0 ? M_BANK1:M_BANK0)+(n), (newval&0xff)); }    
/*TODO*///#define STATE	i8x41.state
/*TODO*///#define ENABLE	i8x41.enable
/*TODO*///#define TIMER	i8x41.timer
/*TODO*///#define TOVF	i8x41.tovf

    static int i8x41_cycles[] = {
            1,1,1,2,2,1,1,1,2,2,2,2,2,2,2,2,
            1,1,2,2,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,1,2,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,2,1,2,1,2,1,2,2,2,2,2,2,2,2,
            1,1,1,2,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,2,2,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,
            1,1,2,1,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,1,2,2,1,2,1,2,2,2,2,2,2,2,2,
            1,1,2,2,1,1,2,1,2,2,2,2,2,2,2,2,
            1,1,1,2,2,1,1,1,1,1,1,1,1,1,1,1,
            2,2,2,2,2,1,2,1,2,2,2,2,2,2,2,2,
            1,1,1,1,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,2,1,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,1,2,2,1,2,1,1,1,1,1,1,1,1,1,
            1,1,2,1,2,1,2,1,2,2,2,2,2,2,2,2
    };

    /***********************************
     *	illegal opcodes
     ***********************************/
    public static void illegal()
    {
            logerror("i8x41 #%d: illegal opcode at 0x%03x: %02x\n", cpu_getactivecpu(), i8x41.pc, ROP(i8x41.pc));
    }

    /***********************************
     *	0110 1rrr *  ADD	 A,Rr
     ***********************************/
    public static void add_r(int r)
    {
    	int res = i8x41.a + R(r);
    	if( res < i8x41.a ) i8x41.psw |= FC;
    	if( (res & 0x0f) < (i8x41.a & 0x0f) ) i8x41.psw |= FA;
    	i8x41.a = res;
    }
    
    /***********************************
     *	0110 000r
     *	ADD 	A,@Rr
     ***********************************/
    public static void add_rm(int r)
    {
    	int res = i8x41.a + RM( M_IRAM + (R(r) & 0x3f) );
    	if( res < i8x41.a ) i8x41.psw |= FC;
    	if( (res & 0x0f) < (i8x41.a & 0x0f) ) i8x41.psw |= FA;
    	i8x41.a = res;
    }

    /***********************************
     *	0000 0011 7654 3210
     *	ADD 	A,#n
     ***********************************/
    public static void add_i()
    {
            int res = i8x41.a + ROP_ARG(i8x41.pc);
            i8x41.pc++;
            if( res < i8x41.a ) i8x41.psw |= FC;
            if( (res & 0x0f) < (i8x41.a & 0x0f) ) i8x41.psw |= FA;
            i8x41.a = res;
    }

    /***********************************
     *	0111 1rrr
     *	ADDC	A,Rr
     ***********************************/
    public static void addc_r(int r)
    {
    	int res = i8x41.a + R(r) + (i8x41.psw >> 7);
    	if( res <= i8x41.a ) i8x41.psw |= FC;
    	if( (res & 0x0f) <= (i8x41.a & 0x0f) ) i8x41.psw |= FA;
    	i8x41.a = res;
    }
    
    /***********************************
     *	0111 000r
     *	ADDC	A,@Rr
     ***********************************/
    public static void addc_rm(int r)
    {
    	int res = i8x41.a + RM( M_IRAM+ (R(r) & 0x3f) ) + (i8x41.psw >> 7);
    	if( res <= i8x41.a ) i8x41.psw |= FC;
    	if( (res & 0x0f) <= (i8x41.a & 0x0f) ) i8x41.psw |= FA;
    	i8x41.a = res;
    }

    /***********************************
     *	0001 0011 7654 3210
     *	ADDC	A,#n
     ***********************************/
    public static void addc_i()
    {
            int res = i8x41.a + ROP_ARG(i8x41.pc);
            i8x41.pc++;
            if( res < i8x41.a ) i8x41.psw |= FC;
            if( (res & 0x0f) < (i8x41.a & 0x0f) ) i8x41.psw |= FA;
            i8x41.a = res;
    }

    /***********************************
     *	0101 1rrr
     *	ANL 	A,Rr
     ***********************************/
    public static void anl_r(int r)
    {
    	i8x41.a = i8x41.a & R(r);
    }

    /***********************************
     *	0101 000r
     *	ANL 	A,@Rr
     ***********************************/
    public static void anl_rm(int r)
    {
            i8x41.a = i8x41.a & RM( M_IRAM + (R(r) & 0x3f) );
    }

    /***********************************
     *	0101 0011 7654 3210
     *	ANL 	A,#n
     ***********************************/
    public static void anl_i()
    {
    	i8x41.a = i8x41.a & ROP_ARG(i8x41.pc);
    	i8x41.pc++;
    }
    
    /***********************************
     *	1001 10pp 7654 3210
     *	ANL 	Pp,#n
     ***********************************/
    public static void anl_p_i(int p)
    {
    	int val = ROP_ARG(i8x41.pc);
    	i8x41.pc++;
    	val = val & cpu_readport16(p);
    	cpu_writeport16(p, val);
    }
    
    /***********************************
     *	1001 11pp 7654 3210
     *	ANLD	Pp,A
     ***********************************/
    public static void anld_p_a(int p)
    {
    	int val = i8x41.a & 0x0f;
    	val = (val | 0xf0) & cpu_readport16(p);
    	cpu_writeport16(p, val);
    }

    /***********************************
     *	aaa1 0100 7654 3210
     *	CALL	addr
     ***********************************/
    public static void call_i(int page)
    {
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc++;
            WM( M_STACK + (i8x41.psw&SP) * 2 + 0, i8x41.pc & 0xff);
            WM( M_STACK + (i8x41.psw&SP) * 2 + 1, ((i8x41.pc >> 8) & 0x0f) | (i8x41.psw & 0xf0) );
            i8x41.psw = (i8x41.psw & ~SP) | ((i8x41.psw + 1) & SP);
            i8x41.pc = page | adr;
    }

    /***********************************
     *	0010 0111
     *	CLR 	A
     ***********************************/
    public static void clr_a()
    {
            i8x41.a = 0;
    }

    /***********************************
     *	1001 0111
     *	CLR 	C
     ***********************************/
    public static void clr_c()
    {
    	i8x41.psw &= ~FC;
    }
    
    /***********************************
     *	1000 0101
     *	CLR 	F0
     ***********************************/
    public static void clr_f0()
    {
    	i8x41.psw &= ~F0;
    }
    
    /***********************************
     *	1010 0101
     *	CLR 	F1
     ***********************************/
    public static void clr_f1()
    {
    	i8x41.psw &= ~F1;
    }

    /***********************************
     *	0011 0111
     *	CPL 	A
     ***********************************/
    public static void cpl_a()
    {
            i8x41.a = ~i8x41.a;
    }   

    /***********************************
     *	1010 0111
     *	CPL 	C
     ***********************************/
    public static void cpl_c()
    {
    	i8x41.psw ^= FC;
    }
    
    /***********************************
     *	1001 0101
     *	CPL 	F0
     ***********************************/
    public static void cpl_f0()
    {
    	i8x41.psw ^= F0;
    }
    
    /***********************************
     *	1011 0101
     *	CPL 	F1
     ***********************************/
    public static void cpl_f1()
    {
    	i8x41.psw ^= F1;
    }
    
    /***********************************
     *	0101 0111
     *	DA		A
     ***********************************/
    public static void da_a()
    {
    	int res = i8x41.a + (((i8x41.psw & FA)!=0 || ((i8x41.a & 0x0f) > 0x09)) ? 0x06 : 0x00);
    	if( (i8x41.psw & FC)!=0 || ((res & 0xf0) > 0x90) )
    		res += 0x60;
    	if( res < i8x41.a )
    		i8x41.psw |= FC;
    	else
    		i8x41.psw &= ~FC;
    	i8x41.a = res;
    }

    /***********************************
     *	0000 0111
     *	DEC 	A
     ***********************************/
    public static void dec_a()
    {
            i8x41.a -= 1;
    }

    /***********************************
     *	1100 1rrr
     *	DEC 	Rr
     ***********************************/
    public static void dec_r(int r)
    {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        set_R(r, R(r)-1);
    }

    /***********************************
     *	0001 0101
     *	DIS 	I
     ***********************************/
    public static void dis_i()
    {
            i8x41.enable &= ~IBFI;	/* disable input buffer full interrupt */
    }

    /***********************************
     *	0011 0101
     *	DIS 	TCNTI
     ***********************************/
    public static void dis_tcnti()
    {
            i8x41.enable &= ~TCNTI;	/* disable timer/counter interrupt */
    }

    /***********************************
     *	0111 1rrr 7654 3210
     *	DJNZ	Rr,addr
     ***********************************/
    public static void djnz_r_i(int r)
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc++;
    	//R(r) -= 1;
        set_R(r, R(r)-1);
        
    	if( R(r) != 0 )
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /***********************************
     *	1110 0101
     *	EN		DMA
     ***********************************/
    public static void en_dma()
    {
    	i8x41.enable |= DMA;		/* enable DMA handshake lines */
    }
    
    /***********************************
     *	1111 0101
     *	EN		FLAGS
     ***********************************/
    public static void en_flags()
    {
    	i8x41.enable |= FLAGS;	/* enable flags handshake lines P24 & P25 */
    }

    /***********************************
     *	0000 0101
     *	EN		I
     ***********************************/
    public static void en_i()
    {
            if (0 == (i8x41.enable & IBFI))
            {
                    i8x41.enable |= IBFI; 	/* enable input buffer full interrupt */
                    if ((i8x41.state & IBF) != 0)	/* already got data in the buffer? */
                            i8x41_set_irq_line(I8X41_INT_IBF, HOLD_LINE);
            }
    }

    /***********************************
     *	0010 0101
     *	EN		TCNTI
     ***********************************/
    public static void en_tcnti()
    {
            i8x41.enable |= TCNTI;	/* enable timer/counter interrupt */
    }

    /***********************************
     *	0010 0010
     *	IN		A,DBB
     ***********************************/
    public static void in_a_dbb()
    {
        //System.out.println("in_a_dbb");
            if (i8x41.irq_callback != null){
                
                    (i8x41.irq_callback).handler(I8X41_INT_IBF);	/* clear input buffer full flag */
            }
            i8x41.state &= ~IBF;
            i8x41.a = i8x41.dbbi;				/* DBB input buffer */
    }

    /***********************************
     *	0000 10pp
     *	IN		A,Pp
     ***********************************/
    public static void in_a_p(int p)
    {
            i8x41.a = cpu_readport16(p);	/* should read port 0/3 be prevented? */
    }

    /***********************************
     *	0001 0111
     *	INC 	A
     ***********************************/
    public static void inc_a()
    {
            i8x41.a += 1;
    }

    /***********************************
     *	0001 1rrr
     *	DEC 	Rr
     ***********************************/
    public static void inc_r(int r)
    {
        //R(r) += 1;
        set_R(r, R(r)+1);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /***********************************
     *	0001 000r
     *	INC  @	Rr
     ***********************************/
    public static void inc_rm(int r)
    {
            int addr = M_IRAM + (R(r) & 0x3f);
            WM( addr, RM(addr) + 1 );
    }

    /***********************************
     *	bbb1 0010
     *	JBb 	addr
     ***********************************/
    public static void jbb_i(int bit)
    {
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc += 1;
            if(( i8x41.a & (1 << bit) ) != 0)
                    i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	1111 0110
     *	JC		addr
     ***********************************/
    public static void jc_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if(( i8x41.psw & FC ) != 0)
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }
    
    /***********************************
     *	1011 0110
     *	JF0 	addr
     ***********************************/
    public static void jf0_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if(( i8x41.psw & F0 ) != 0)
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }
    
    /***********************************
     *	0111 0110
     *	JF1 	addr
     ***********************************/
    public static void jf1_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if(( i8x41.psw & F1 ) != 0)
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	aaa0 0100
     *	JMP 	addr
     ***********************************/
    public static void jmp_i(int page)
    {
            /* err.. do we have 10 or 11 PC bits?
             * CALL is said to use 0aa1 (4 pages)
             * JMP is said to use aaa0 (8 pages)
             */
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc += 1;
            i8x41.pc = page | adr;
    }

    /***********************************
     *	1011 0011
     *	JMP  @	A
     ***********************************/
    public static void jmpp_a()
    {
    	int adr = (i8x41.pc & 0x700) | i8x41.a;
    	i8x41.pc = (i8x41.pc & 0x700) | RM(adr);
    }
    
    /***********************************
     *	1110 0110
     *	JNC 	addr
     ***********************************/
    public static void jnc_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if( (i8x41.psw & FC) == 0 )
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }
    
    /***********************************
     *	1101 0110
     *	JNIBF	addr
     ***********************************/
    public static void jnibf_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if( 0 == (i8x41.state & IBF) )
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	0010 0110
     *	JNT0	addr
     ***********************************/
    public static void jnt0_i()
    {
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc += 1;
            if( (i8x41.state & TEST0) == 0 )
                    i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	0100 0110
     *	JNT1	addr
     ***********************************/
    public static void jnt1_i()
    {
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc += 1;
            if( (i8x41.state & TEST1) == 0 )
                    i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	1001 0110
     *	JNZ 	addr
     ***********************************/
    public static void jnz_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if( i8x41.a != 0 )
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }
    
    /***********************************
     *	1000 0110
     *	JOBF	addr
     ***********************************/
    public static void jobf_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if(( i8x41.state & OBF ) != 0)
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	0001 0110
     *	JTF 	addr
     ***********************************/
    public static void jtf_i()
    {
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc += 1;
            if( i8x41.tovf != 0 )
                    i8x41.pc = (i8x41.pc & 0x700) | adr;
            i8x41.tovf = 0;
    }

    /***********************************
     *	0011 0110
     *	JT0 	addr
     ***********************************/
    public static void jt0_i()
    {
            int adr = ROP_ARG(i8x41.pc);
            i8x41.pc += 1;
            if(( i8x41.enable & TEST0 ) != 0)
                    i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	0101 0110
     *	JT1 	addr
     ***********************************/
    public static void jt1_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if(( i8x41.state & TEST1 ) != 0)
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }
    
    /***********************************
     *	1100 0110
     *	JZ		addr
     ***********************************/
    public static void jz_i()
    {
    	int adr = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	if( i8x41.a == 0 )
    		i8x41.pc = (i8x41.pc & 0x700) | adr;
    }

    /***********************************
     *	0010 0011
     *	MOV 	A,#n
     ***********************************/
    public static void mov_a_i()
    {
            i8x41.a = ROP(i8x41.pc);
            i8x41.pc += 1;
    }

    /***********************************
     *	1100 0111
     *	MOV 	A,PSW
     ***********************************/
    public static void mov_a_psw()
    {
    	i8x41.a = i8x41.psw;
    }
    
    /***********************************
     *	1111 1rrr
     *	MOV 	A,Rr
     ***********************************/
    public static void mov_a_r(int r)
    {
            i8x41.a = R(r);
    }
    
    /***********************************
     *	1111 000r
     *	MOV 	A,Rr
     ***********************************/
    public static void mov_a_rm(int r)
    {
    	i8x41.a = RM( M_IRAM + (R(r) & 0x3f) );
    }

    /***********************************
     *	0100 0010
     *	MOV 	A,T
     ***********************************/
    public static void mov_a_t()
    {
            i8x41.a = (i8x41.timer / 32);
    }

    /***********************************
     *	1101 0111
     *	MOV 	PSW,A
     ***********************************/
    public static void mov_psw_a()
    {
    	i8x41.psw = i8x41.a;
    }
    
    /***********************************
     *	1010 1rrr
     *	MOV 	Rr,A
     ***********************************/
    public static void mov_r_a(int r)
    {
    	//R(r) = A;
        set_R(r, i8x41.a);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /***********************************
     *	1011 1rrr
     *	MOV 	Rr,#n
     ***********************************/
    public static void mov_r_i(int r)
    {
    	int val = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	//R(r) = val;
        set_R(r, val);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /***********************************
     *	1010 000r
     *	MOV 	@Rr,A
     ***********************************/
    public static void mov_rm_a(int r)
    {
    	WM( M_IRAM + (R(r) & 0x3f), i8x41.a );
    }
    
    /***********************************
     *	1011 000r
     *	MOV 	@Rr,#n
     ***********************************/
    public static void mov_rm_i(int r)
    {
    	int val = ROP_ARG(i8x41.pc);
    	i8x41.pc += 1;
    	WM( M_IRAM + (R(r) & 0x3f), val );
    }
    
    /***********************************
     *	1001 0000
     *	MOV 	STS,A
     ***********************************/
    public static void mov_sts_a()
    {
    	i8x41.state = (i8x41.state & 0x0f) | (i8x41.a & 0xf0);
    }
    
    /***********************************
     *	0110 0010
     *	MOV 	T,A
     ***********************************/
    public static void mov_t_a()
    {
    	i8x41.timer = i8x41.a * 32;
    }

    /***********************************
     *	0000 11pp
     *	MOVD	A,Pp
     ***********************************/
    public static void movd_a_p(int p)
    {
            int val = cpu_readport16(p);
            i8x41.a = val & 0x0f;
    }

    /***********************************
     *	0011 11pp
     *	MOVD	Pp,A
     ***********************************/
    public static void movd_p_a(int p)
    {
            cpu_writeport16(p, i8x41.a & 0x0f);
    }

    /***********************************
     *	1010 0011
     *	MOVP	A,@A
     ***********************************/
    public static void movp_a_am()
    {
    	int addr = (i8x41.pc & 0x700) | i8x41.a;
    	i8x41.a = RM(addr);
    }
    
    /***********************************
     *	1110 0011
     *	MOVP3	A,@A
     ***********************************/
    public static void movp3_a_am()
    {
    	int addr = 0x300 | i8x41.a;
    	i8x41.a = RM(addr);
    }

    /***********************************
     *	0000 0000
     *	NOP
     ***********************************/
    public static void nop()
    {
    }

    /***********************************
     *	0100 1rrr
     *	ORL 	A,Rr
     ***********************************/
    public static void orl_r(int r)
    {
            i8x41.a = i8x41.a | R(r);
    }

    /***********************************
     *	0100 000r
     *	ORL 	A,@Rr
     ***********************************/
    public static void orl_rm(int r)
    {
            i8x41.a = i8x41.a | RM( M_IRAM + (R(r) & 0x3f) );
    }

    /***********************************
     *	0100 0011 7654 3210
     *	ORL 	A,#n
     ***********************************/
    public static void orl_i()
    {
            int val = ROP_ARG(i8x41.pc);
            i8x41.pc++;
            i8x41.a = i8x41.a | val;
    }

    /***********************************
     *	1000 10pp 7654 3210
     *	ORL 	Pp,#n
     ***********************************/
    public static void orl_p_i(int p)
    {
    	int val = ROP_ARG(i8x41.pc);
    	i8x41.pc++;
    	val = val | cpu_readport16(p);
    	cpu_writeport16(p, val);
    }
    
    /***********************************
     *	1000 11pp 7654 3210
     *	ORLD	Pp,A
     ***********************************/
    public static void orld_p_a(int p)
    {
    	int val = i8x41.a & 0x0f;
    	val = val | cpu_readport16(p);
    	cpu_writeport16(p, val);
    }

    /***********************************
     *	0000 0010
     *	OUT 	DBB,A
     ***********************************/
    public static void out_dbb_a()
    {
            i8x41.dbbo = i8x41.a;			/* DBB output buffer */
            i8x41.state |= OBF;		/* assert the output buffer full flag */
    }

    /***********************************
     *	0011 10pp
     *	OUT 	Pp,A
     ***********************************/
    public static void out_p_a(int p)
    {
            cpu_writeport16(p, i8x41.a);
    }

    /***********************************
     *	1000 0011
     *	RET
     ***********************************/
    public static void ret()
    {
    	int msb;
    	i8x41.psw = (i8x41.psw & ~SP) | ((i8x41.psw - 1) & SP);
    	msb = RM(M_STACK + (i8x41.psw&SP) * 2 + 1);
    	i8x41.pc = RM(M_STACK + (i8x41.psw&SP) * 2 + 0);
    	i8x41.pc |= (msb << 8) & 0x700;
    }
    
    /***********************************
     *	1001 0011
     *	RETR
     ***********************************/
    public static void retr()
    {
    	int msb;
    	i8x41.psw = (i8x41.psw & ~SP) | ((i8x41.psw - 1) & SP);
    	msb = RM(M_STACK + (i8x41.psw&SP) * 2 + 1);
    	i8x41.pc = RM(M_STACK + (i8x41.psw&SP) * 2 + 0);
    	i8x41.pc |= (msb << 8) & 0x700;
    	i8x41.psw = (i8x41.psw & 0x0f) | (msb & 0xf0);
    }
    
    /***********************************
     *	1110 0111
     *	RL		A
     ***********************************/
    public static void rl_a()
    {
    	i8x41.a = (i8x41.a << 1) | (i8x41.a >> 7);
    }
    
    /***********************************
     *	1111 0111
     *	RLC 	A
     ***********************************/
    public static void rlc_a()
    {
    	int c = i8x41.psw >> 7;
    	i8x41.psw = (i8x41.psw & ~FC) | (i8x41.a >> 7);
    	i8x41.a = (i8x41.a << 1) | c;
    }
    
    /***********************************
     *	0111 0111
     *	RR		A
     ***********************************/
    public static void rr_a()
    {
    	i8x41.a = (i8x41.a >> 1) | (i8x41.a << 7);
    }
    
    /***********************************
     *	0110 0111
     *	RRC 	A
     ***********************************/
    public static void rrc_a()
    {
    	int c = i8x41.psw & 0x80;
    	i8x41.psw = (i8x41.psw & ~FC) | (i8x41.a << 7);
    	i8x41.a = (i8x41.a >> 1) | c;
    }
    
    /***********************************
     *	1100 0101
     *	SEL 	RB0
     ***********************************/
    public static void sel_rb0()
    {
    	i8x41.psw &= ~F1;
    }
    
    /***********************************
     *	1101 0101
     *	SEL 	RB1
     ***********************************/
    public static void sel_rb1()
    {
    	i8x41.psw |= F1;
    }
    
    /***********************************
     *	0110 0101
     *	STOP	TCNT
     ***********************************/
    public static void stop_tcnt()
    {
    	i8x41.enable &= ~(T|CNT);
    }

    /***********************************
     *	0100 0101
     *	STRT	CNT
     ***********************************/
    public static void strt_cnt()
    {
            i8x41.enable |= CNT;
    }

    /***********************************
     *	0101 0101
     *	STRT	T
     ***********************************/
    public static void strt_t()
    {
    	i8x41.enable |= T;
    }

    /***********************************
     *	0100 0111
     *	SWAP	A
     ***********************************/
    public static void swap_a()
    {
            i8x41.a = (i8x41.a << 4) | (i8x41.a >> 4);
    }

    /***********************************
     *	0010 1rrr
     *	XCH 	A,Rr
     ***********************************/
    public static void xch_a_r(int r)
    {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            int tmp = R(r);
            //R(r) = A;
            set_R(r, i8x41.a);
            i8x41.a = tmp;
    }

    /***********************************
     *	0010 000r
     *	XCH 	A,@Rr
     ***********************************/
    public static void xch_a_rm(int r)
    {
            int addr = M_IRAM + (R(r) & 0x3f);
            int tmp = RM(addr);
            WM( addr, i8x41.a );
            i8x41.a = tmp;
    }

    /***********************************
     *	0011 000r
     *	XCHD	A,@Rr
     ***********************************/
    public static void xchd_a_rm(int r)
    {
            int addr = M_IRAM + (R(r) & 0x3f);
            int tmp = RM(addr);
            WM( addr, (tmp & 0xf0) | (i8x41.a & 0x0f) );
            i8x41.a = (i8x41.a & 0xf0) | (tmp & 0x0f);
    }

    /***********************************
     *	1101 1rrr
     *	XRL 	A,Rr
     ***********************************/
    public static void xrl_r(int r)
    {
    	i8x41.a = i8x41.a ^ R(r);
    }
    
    /***********************************
     *	1101 000r
     *	XRL 	A,@Rr
     ***********************************/
    public static void xrl_rm(int r)
    {
    	i8x41.a = i8x41.a ^ RM( M_IRAM + (R(r) & 0x3f) );
    }
    
    /***********************************
     *	1101 0011 7654 3210
     *	XRL 	A,#n
     ***********************************/
    public static void xrl_i()
    {
    	int val = ROP_ARG(i8x41.pc);
    	i8x41.pc++;
    	i8x41.a = i8x41.a ^ val;
    }

    public void i8x41_init()
    {
/*TODO*///	int cpu = cpu_getactivecpu();
/*TODO*///	state_save_register_UINT16("i8x41", cpu, "PPC",       &i8x41.ppc,    1);
/*TODO*///	state_save_register_UINT16("i8x41", cpu, "PC",        &i8x41.pc,     1);
/*TODO*///	state_save_register_UINT16("i8x41", cpu, "TIMER",     &i8x41.timer,  1);
/*TODO*///	state_save_register_UINT16("i8x41", cpu, "SUBTYPE",   &i8x41.subtype,1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "A",         &i8x41.a,      1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "PSW",       &i8x41.psw,    1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "STATE",     &i8x41.state,  1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "TOVF",      &i8x41.tovf,   1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "ENABLE",    &i8x41.enable, 1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "DBBI",      &i8x41.dbbi,   1);
/*TODO*///	state_save_register_UINT8 ("i8x41", cpu, "DBBO",      &i8x41.dbbo,   1);
    }

    /* Reset registers to the initial values */
    public void i8x41_reset(Object param)
    {
            //memset(&i8x41, 0, sizeof(I8X41));
            i8x41 = new I8X41_Regs();
            /* default to 8041 behaviour for DBBI/DBBO and extended commands */
            i8x41.subtype = 8041;
            /* ugly hack.. excuse my lazyness */
            i8x41.ram = new UBytePtr(memory_region(REGION_CPU1 + cpu_getactivecpu()));
            i8x41.enable = IBFI | TCNTI;
            i8x41.dbbi = 0xff;
            i8x41.dbbo = 0xff;
    }

    /* Shut down CPU core */
    public void i8x41_exit()
    {
            /* nothing to do */
    }

    /* Execute cycles - returns number of cycles actually run */
    public int i8x41_execute(int cycles)
    {
            i8x41_ICount[0] = cycles;

            do
            {
                    int op = cpu_readop(i8x41.pc);
                    //System.out.println(op);

                    i8x41.ppc = i8x41.pc;

    /*TODO*///		CALL_MAME_DEBUG;

                    i8x41.pc += 1;
                    i8x41_ICount[0] -= i8x41_cycles[op];

                    if(( i8x41.enable & T ) != 0)
                            i8x41.timer += i8x41_cycles[op];

                    if( i8x41.timer > 0x1fff )
                    {
                            i8x41.timer &= 0x1fff;
                            i8x41.tovf = 1;
                            if(( i8x41.enable & TCNTI ) != 0)
                            {
                                    WM( M_STACK + (i8x41.psw&SP) * 2 + 1, ((i8x41.pc >> 8) & 0x0f) | (i8x41.psw & 0xf0) );
                                    i8x41.psw = (i8x41.psw & ~SP) | ((i8x41.psw + 1) & SP);
                                    i8x41.pc = V_TIMER;
                            }
                    }

                    switch( op )
                    {
                    /* opcode cycles bitmask */
                    case 0x00: /* 1: 0000 0000 */
                            nop();
                            break;
                    case 0x01: /* 1: 0000 0001 */
                            illegal();
                            break;
                    case 0x02: /* 1: 0000 0010 */
                            out_dbb_a();
                            break;
                    case 0x03: /* 2: 0000 0011 */
                            add_i();
                            break;
                    case 0x04: /* 2: aaa0 0100 */
                            jmp_i(0x000);
                            break;
                    case 0x05: /* 1: 0000 0101 */
                            en_i();
                            break;
                    case 0x06: /* 1: 0000 0110 */
                            illegal();
                            break;
                    case 0x07: /* 1: 0000 0111 */
                            dec_a();
                            break;
                    case 0x08: /* 2: 0000 10pp */
                    case 0x09: /* 2: 0000 10pp */
                    case 0x0a: /* 2: 0000 10pp */
                    case 0x0b: /* 2: 0000 10pp */
                            in_a_p(op & 3);
                            break;
                    case 0x0c: /* 2: 0000 11pp */
                    case 0x0d: /* 2: 0000 11pp */
                    case 0x0e: /* 2: 0000 11pp */
                    case 0x0f: /* 2: 0000 11pp */
                            movd_a_p(op & 3);
                            break;
                    case 0x10: /* 1: 0001 000r */
                            inc_rm(0);
                            break;
                    case 0x11: /* 1: 0001 000r */
                            inc_rm(1);
                            break;
                    case 0x12: /* 2: bbb1 0010 */
                            jbb_i(0);
                            break;
                    case 0x13: /* 2: 0001 0011 */
                            addc_i();
                            break;
                    case 0x14: /* 2: aaa1 0100 */
                            call_i(0x000);
                            break;
                    case 0x15: /* 1: 0001 0101 */
                            dis_i();
                            break;
                    case 0x16: /* 2: 0001 0110 */
                            jtf_i();
                            break;
                    case 0x17: /* 1: 0001 0111 */
                            inc_a();
                            break;
                    case 0x18: /* 1: 0001 1rrr */
                    case 0x19: /* 1: 0001 1rrr */
                    case 0x1a: /* 1: 0001 1rrr */
                    case 0x1b: /* 1: 0001 1rrr */
                    case 0x1c: /* 1: 0001 1rrr */
                    case 0x1d: /* 1: 0001 1rrr */
                    case 0x1e: /* 1: 0001 1rrr */
                    case 0x1f: /* 1: 0001 1rrr */
                            inc_r(op & 7);
                            break;
                    case 0x20: /* 1: 0010 000r */
                            xch_a_rm(0);
                            break;
                    case 0x21: /* 1: 0010 000r */
                            xch_a_rm(1);
                            break;
                    case 0x22: /* 1: 0010 0010 */
                            in_a_dbb();
                            break;
                    case 0x23: /* 2: 0010 0011 */
                            mov_a_i();
                            break;
                    case 0x24: /* 2: aaa0 0100 */
                            jmp_i(0x100);
                            break;
                    case 0x25: /* 1: 0010 0101 */
                            en_tcnti();
                            break;
                    case 0x26: /* 2: 0010 0110 */
                            jnt0_i();
                            break;
                    case 0x27: /* 1: 0010 0111 */
                            clr_a();
                            break;
                    case 0x28: /* 1: 0010 1rrr */
                    case 0x29: /* 1: 0010 1rrr */
                    case 0x2a: /* 1: 0010 1rrr */
                    case 0x2b: /* 1: 0010 1rrr */
                    case 0x2c: /* 1: 0010 1rrr */
                    case 0x2d: /* 1: 0010 1rrr */
                    case 0x2e: /* 1: 0010 1rrr */
                    case 0x2f: /* 1: 0010 1rrr */
                            xch_a_r(op & 7);
                            break;
                    case 0x30: /* 1: 0011 000r */
                            xchd_a_rm(0);
                            break;
                    case 0x31: /* 1: 0011 000r */
                            xchd_a_rm(1);
                            break;
                    case 0x32: /* 2: bbb1 0010 */
                            jbb_i(1);
                            break;
                    case 0x33: /* 1: 0011 0101 */
                            illegal();
                            break;
                    case 0x34: /* 2: aaa1 0100 */
                            call_i(0x100);
                            break;
                    case 0x35: /* 1: 0000 0101 */
                            dis_tcnti();
                            break;
                    case 0x36: /* 2: 0011 0110 */
                            jt0_i();
                            break;
                    case 0x37: /* 1: 0011 0111 */
                            cpl_a();
                            break;
                    case 0x38: /* 2: 0011 10pp */
                    case 0x39: /* 2: 0011 10pp */
                    case 0x3a: /* 2: 0011 10pp */
                    case 0x3b: /* 2: 0011 10pp */
                            out_p_a(op & 3);
                            break;
                    case 0x3c: /* 2: 0011 11pp */
                    case 0x3d: /* 2: 0011 11pp */
                    case 0x3e: /* 2: 0011 11pp */
                    case 0x3f: /* 2: 0011 11pp */
                            movd_p_a(op & 3);
                            break;
                    case 0x40: /* 1: 0100 000r */
                            orl_rm(0);
                            break;
                    case 0x41: /* 1: 0100 000r */
                            orl_rm(1);
                            break;
                    case 0x42: /* 1: 0100 0010 */
                            mov_a_t();
                            break;
                    case 0x43: /* 2: 0100 0011 */
                            orl_i();
                            break;
                    case 0x44: /* 2: aaa0 0100 */
                            jmp_i(0x200);
                            break;
                    case 0x45: /* 1: 0100 0101 */
                            strt_cnt();
                            break;
                    case 0x46: /* 2: 0100 0110 */
                            jnt1_i();
                            break;
                    case 0x47: /* 1: 0100 0111 */
                            swap_a();
                            break;
                    case 0x48: /* 1: 0100 1rrr */
                    case 0x49: /* 1: 0100 1rrr */
                    case 0x4a: /* 1: 0100 1rrr */
                    case 0x4b: /* 1: 0100 1rrr */
                    case 0x4c: /* 1: 0100 1rrr */
                    case 0x4d: /* 1: 0100 1rrr */
                    case 0x4e: /* 1: 0100 1rrr */
                    case 0x4f: /* 1: 0100 1rrr */
                            orl_r(op & 7);
                            break;
                    case 0x50: /* 1: 0101 000r */
                            anl_rm(0);
                            break;
                    case 0x51: /* 1: 0101 000r */
                            anl_rm(1);
                            break;
                    case 0x52: /* 2: bbb1 0010 */
                            jbb_i(2);
                            break;
                    case 0x53: /* 2: 0101 0011 */
                            anl_i();
                            break;
                    case 0x54: /* 2: aaa1 0100 */
                            call_i(0x200);
                            break;
                    case 0x55: /* 1: 0101 0101 */
                            strt_t();
                            break;
                    case 0x56: /* 2: 0101 0110 */
                            jt1_i();
                            break;
                    case 0x57: /* 1: 0101 0111 */
                            da_a();
                            break;
                    case 0x58: /* 1: 0101 1rrr */
                    case 0x59: /* 1: 0101 1rrr */
                    case 0x5a: /* 1: 0101 1rrr */
                    case 0x5b: /* 1: 0101 1rrr */
                    case 0x5c: /* 1: 0101 1rrr */
                    case 0x5d: /* 1: 0101 1rrr */
                    case 0x5e: /* 1: 0101 1rrr */
                    case 0x5f: /* 1: 0101 1rrr */
                            anl_r(op & 7);
                            break;
                    case 0x60: /* 1: 0110 000r */
                            add_rm(0);
                            break;
                    case 0x61: /* 1: 0110 000r */
                            add_rm(1);
                            break;
                    case 0x62: /* 1: 0110 0010 */
                            mov_t_a();
                            break;
                    case 0x63: /* 1: 0110 0011 */
                            illegal();
                            break;
                    case 0x64: /* 2: aaa0 0100 */
                            jmp_i(0x300);
                            break;
                    case 0x65: /* 1: 0110 0101 */
                            stop_tcnt();
                            break;
                    case 0x66: /* 1: 0110 0110 */
                            illegal();
                            break;
                    case 0x67: /* 1: 0110 0111 */
                            rrc_a();
                            break;
                    case 0x68: /* 1: 0110 1rrr */
                    case 0x69: /* 1: 0110 1rrr */
                    case 0x6a: /* 1: 0110 1rrr */
                    case 0x6b: /* 1: 0110 1rrr */
                    case 0x6c: /* 1: 0110 1rrr */
                    case 0x6d: /* 1: 0110 1rrr */
                    case 0x6e: /* 1: 0110 1rrr */
                    case 0x6f: /* 1: 0110 1rrr */
                            add_r(op & 7);
                            break;
                    case 0x70: /* 1: 0111 000r */
                            addc_rm(0);
                            break;
                    case 0x71: /* 1: 0111 000r */
                            addc_rm(1);
                            break;
                    case 0x72: /* 2: bbb1 0010 */
                            jbb_i(3);
                            break;
                    case 0x73: /* 1: 0111 0011 */
                            illegal();
                            break;
                    case 0x74: /* 2: aaa1 0100 */
                            call_i(0x300);
                            break;
                    case 0x75: /* 1: 0111 0101 */
                            illegal();
                            break;
                    case 0x76: /* 2: 0111 0110 */
                            jf1_i();
                            break;
                    case 0x77: /* 1: 0111 0111 */
                            rr_a();
                            break;
                    case 0x78: /* 1: 0111 1rrr */
                    case 0x79: /* 1: 0111 1rrr */
                    case 0x7a: /* 1: 0111 1rrr */
                    case 0x7b: /* 1: 0111 1rrr */
                    case 0x7c: /* 1: 0111 1rrr */
                    case 0x7d: /* 1: 0111 1rrr */
                    case 0x7e: /* 1: 0111 1rrr */
                    case 0x7f: /* 1: 0111 1rrr */
                            addc_r(op & 7);
                            break;
                    case 0x80: /* 1: 1000 0000 */
                            illegal();
                            break;
                    case 0x81: /* 1: 1000 0001 */
                            illegal();
                            break;
                    case 0x82: /* 1: 1000 0010 */
                            illegal();
                            break;
                    case 0x83: /* 2: 1000 0011 */
                            ret();
                            break;
                    case 0x84: /* 2: aaa0 0100 */
                            jmp_i(0x400);
                            break;
                    case 0x85: /* 1: 1000 0101 */
                            clr_f0();
                            break;
                    case 0x86: /* 2: 1000 0110 */
                            jobf_i();
                            break;
                    case 0x87: /* 1: 1000 0111 */
                            illegal();
                            break;
                    case 0x88: /* 2: 1000 10pp */
                    case 0x89: /* 2: 1000 10pp */
                    case 0x8a: /* 2: 1000 10pp */
                    case 0x8b: /* 2: 1000 10pp */
                            orl_p_i(op & 3);
                            break;
                    case 0x8c: /* 2: 1000 11pp */
                    case 0x8d: /* 2: 1000 11pp */
                    case 0x8e: /* 2: 1000 11pp */
                    case 0x8f: /* 2: 1000 11pp */
                            orld_p_a(op & 7);
                            break;
                    case 0x90: /* 1: 1001 0000 */
                            mov_sts_a();
                            break;
                    case 0x91: /* 1: 1001 0001 */
                            illegal();
                            break;
                    case 0x92: /* 2: bbb1 0010 */
                            jbb_i(4);
                            break;
                    case 0x93: /* 2: 1001 0011 */
                            retr();
                            break;
                    case 0x94: /* 1: aaa1 0100 */
                            call_i(0x400);
                            break;
                    case 0x95: /* 1: 1001 0101 */
                            cpl_f0();
                            break;
                    case 0x96: /* 2: 1001 0110 */
                            jnz_i();
                            break;
                    case 0x97: /* 1: 1001 0111 */
                            clr_c();
                            break;
                    case 0x98: /* 2: 1001 10pp */
                    case 0x99: /* 2: 1001 10pp */
                    case 0x9a: /* 2: 1001 10pp */
                    case 0x9b: /* 2: 1001 10pp */
                            anl_p_i(op & 3);
                            break;
                    case 0x9c: /* 2: 1001 11pp */
                    case 0x9d: /* 2: 1001 11pp */
                    case 0x9e: /* 2: 1001 11pp */
                    case 0x9f: /* 2: 1001 11pp */
                            anld_p_a(op & 7);
                            break;
                    case 0xa0: /* 1: 1010 000r */
                            mov_rm_a(0);
                            break;
                    case 0xa1: /* 1: 1010 000r */
                            mov_rm_a(1);
                            break;
                    case 0xa2: /* 1: 1010 0010 */
                            illegal();
                            break;
                    case 0xa3: /* 2: 1010 0011 */
                            movp_a_am();
                            break;
                    case 0xa4: /* 2: aaa0 0100 */
                            jmp_i(0x500);
                            break;
                    case 0xa5: /* 1: 1010 0101 */
                            clr_f1();
                            break;
                    case 0xa6: /* 1: 1010 0110 */
                            illegal();
                            break;
                    case 0xa7: /* 1: 1010 0111 */
                            cpl_c();
                            break;
                    case 0xa8: /* 1: 1010 1rrr */
                    case 0xa9: /* 1: 1010 1rrr */
                    case 0xaa: /* 1: 1010 1rrr */
                    case 0xab: /* 1: 1010 1rrr */
                    case 0xac: /* 1: 1010 1rrr */
                    case 0xad: /* 1: 1010 1rrr */
                    case 0xae: /* 1: 1010 1rrr */
                    case 0xaf: /* 1: 1010 1rrr */
                            mov_r_a(op & 7);
                            break;
                    case 0xb0: /* 2: 1011 000r */
                            mov_rm_i(0);
                            break;
                    case 0xb1: /* 2: 1011 000r */
                            mov_rm_i(1);
                            break;
                    case 0xb2: /* 2: bbb1 0010 */
                            jbb_i(5);
                            break;
                    case 0xb3: /* 2: 1011 0011 */
                            jmpp_a();
                            break;
                    case 0xb4: /* 2: aaa1 0100 */
                            call_i(0x500);
                            break;
                    case 0xb5: /* 1: 1011 0101 */
                            cpl_f1();
                            break;
                    case 0xb6: /* 2: 1011 0110 */
                            jf0_i();
                            break;
                    case 0xb7: /* 1: 1011 0111 */
                            illegal();
                            break;
                    case 0xb8: /* 2: 1011 1rrr */
                    case 0xb9: /* 2: 1011 1rrr */
                    case 0xba: /* 2: 1011 1rrr */
                    case 0xbb: /* 2: 1011 1rrr */
                    case 0xbc: /* 2: 1011 1rrr */
                    case 0xbd: /* 2: 1011 1rrr */
                    case 0xbe: /* 2: 1011 1rrr */
                    case 0xbf: /* 2: 1011 1rrr */
                            mov_r_i(op & 7);
                            break;
                    case 0xc0: /* 1: 1100 0000 */
                            illegal();
                            break;
                    case 0xc1: /* 1: 1100 0001 */
                            illegal();
                            break;
                    case 0xc2: /* 1: 1100 0010 */
                            illegal();
                            break;
                    case 0xc3: /* 1: 1100 0011 */
                            illegal();
                            break;
                    case 0xc4: /* 2: aaa0 0100 */
                            jmp_i(0x600);
                            break;
                    case 0xc5: /* 1: 1100 0101 */
                            sel_rb0();
                            break;
                    case 0xc6: /* 2: 1100 0110 */
                            jz_i();
                            break;
                    case 0xc7: /* 1: 1100 0111 */
                            mov_a_psw();
                            break;
                    case 0xc8: /* 1: 1100 1rrr */
                    case 0xc9: /* 1: 1100 1rrr */
                    case 0xca: /* 1: 1100 1rrr */
                    case 0xcb: /* 1: 1100 1rrr */
                    case 0xcc: /* 1: 1100 1rrr */
                    case 0xcd: /* 1: 1100 1rrr */
                    case 0xcf: /* 1: 1100 1rrr */
                            dec_r(op & 7);
                            break;
                    case 0xd0: /* 1: 1101 000r */
                            xrl_rm(0);
                            break;
                    case 0xd1: /* 1: 1101 000r */
                            xrl_rm(1);
                            break;
                    case 0xd2: /* 2: bbb1 0010 */
                            jbb_i(6);
                            break;
                    case 0xd3: /* 1: 1101 0011 */
                            xrl_i();
                            break;
                    case 0xd4: /* 2: aaa1 0100 */
                            call_i(0x600);
                            break;
                    case 0xd5: /* 1: 1101 0101 */
                            sel_rb1();
                            break;
                    case 0xd6: /* 2: 1101 0110 */
                            jnibf_i();
                            break;
                    case 0xd7: /* 1: 1101 0111 */
                            mov_psw_a();
                            break;
                    case 0xd8: /* 1: 1101 1rrr */
                    case 0xd9: /* 1: 1101 1rrr */
                    case 0xda: /* 1: 1101 1rrr */
                    case 0xdb: /* 1: 1101 1rrr */
                    case 0xdc: /* 1: 1101 1rrr */
                    case 0xdd: /* 1: 1101 1rrr */
                    case 0xde: /* 1: 1101 1rrr */
                    case 0xdf: /* 1: 1101 1rrr */
                            xrl_r(op & 7);
                            break;
                    case 0xe0: /* 1: 1110 0000 */
                            illegal();
                            break;
                    case 0xe1: /* 1: 1110 0001 */
                            illegal();
                            break;
                    case 0xe2: /* 1: 1110 0010 */
                            illegal();
                            break;
                    case 0xe3: /* 2: 1110 0011 */
                            movp3_a_am();
                            break;
                    case 0xe4: /* 2: aaa0 0100 */
                            jmp_i(0x700);
                            break;
                    case 0xe5: /* 1: 1110 0101 */
                            en_dma();
                            break;
                    case 0xe6: /* 2: 1110 0110 */
                            jnc_i();
                            break;
                    case 0xe7: /* 1: 1110 0111 */
                            rl_a();
                            break;
                    case 0xe8: /* 2: 1111 1rrr */
                    case 0xe9: /* 2: 1111 1rrr */
                    case 0xea: /* 2: 1111 1rrr */
                    case 0xeb: /* 2: 1111 1rrr */
                    case 0xec: /* 2: 1111 1rrr */
                    case 0xed: /* 2: 1111 1rrr */
                    case 0xee: /* 2: 1111 1rrr */
                    case 0xef: /* 2: 1111 1rrr */
                            djnz_r_i(op & 7);
                            break;
                    case 0xf0: /* 1: 1111 000r */
                            mov_a_rm(0);
                            break;
                    case 0xf1: /* 1: 1111 000r */
                            mov_a_rm(1);
                            break;
                    case 0xf2: /* 2: bbb1 0010 */
                            jbb_i(7);
                            break;
                    case 0xf3: /* 1: 1111 0011 */
                            illegal();
                            break;
                    case 0xf4: /* 2: aaa1 0100 */
                            call_i(0x700);
                            break;
                    case 0xf5: /* 1: 1111 0101 */
                            en_flags();
                            break;
                    case 0xf6: /* 2: 1111 0110 */
                            jc_i();
                            break;
                    case 0xf7: /* 1: 1111 0111 */
                            rlc_a();
                            break;
                    case 0xf8: /* 1: 1110 1rrr */
                    case 0xf9: /* 1: 1110 1rrr */
                    case 0xfa: /* 1: 1110 1rrr */
                    case 0xfb: /* 1: 1110 1rrr */
                    case 0xfc: /* 1: 1110 1rrr */
                    case 0xfd: /* 1: 1110 1rrr */
                    case 0xfe: /* 1: 1110 1rrr */
                    case 0xff: /* 1: 1110 1rrr */
                            mov_a_r(op & 7);
                            break;
                    }
            } while( i8x41_ICount[0] > 0 );

            return cycles - i8x41_ICount[0];
    }

    /* Get registers, return context size */
    public static Object i8x41_get_context()
    {
            return i8x41;
    }

    /* Set registers */
    public static void i8x41_set_context(Object src)
    {
            if( src != null )
                i8x41 = (I8X41_Regs) src;
                    //memcpy(&i8x41, src, sizeof(I8X41));
    }

    public static int i8x41_get_reg(int regnum)
    {
            switch( regnum )
            {
            case REG_PC:
            case I8X41_PC:	return i8x41.pc;
            case REG_SP:
            case I8X41_SP:	return i8x41.psw & SP;
            case I8X41_PSW:     return i8x41.psw;
            case I8X41_A:	return i8x41.a;
            case I8X41_T:	return i8x41.timer;
            case I8X41_R0:	return R(0);
            case I8X41_R1:	return R(1);
            case I8X41_R2:	return R(2);
            case I8X41_R3:	return R(3);
            case I8X41_R4:	return R(4);
            case I8X41_R5:	return R(5);
            case I8X41_R6:	return R(6);
            case I8X41_R7:	return R(7);
            case I8X41_DATA:
                            i8x41.state &= ~OBF;	/* reset the output buffer full flag */
                            return i8x41.dbbo;
            case I8X41_STAT:
                            return i8x41.state;
            case REG_PREVIOUSPC: return i8x41.ppc;
            default:
                    if( regnum <= REG_SP_CONTENTS )
                    {
                            int offset = (i8x41.psw & SP) + (REG_SP_CONTENTS - regnum);
                            if( offset < 8 )
                                    return RM( M_STACK + offset ) | ( RM( M_STACK + offset + 1 ) << 8 );
                    }
            }
            return 0;
    }

    public static void i8x41_set_reg (int regnum, int val)
    {
            switch( regnum )
            {
            case REG_PC:
            case I8X41_PC:	i8x41.pc = val & 0x7ff;
            case REG_SP:
            case I8X41_SP:	i8x41.psw = (i8x41.psw & ~SP) | (val & SP);
            case I8X41_PSW:     i8x41.psw = val;
            case I8X41_A:	i8x41.a = val;
            case I8X41_T:	i8x41.timer = val & 0x1fff;
            case I8X41_R0:	set_R(0, val); break;
            case I8X41_R1:	set_R(1, val); break;
            case I8X41_R2:	set_R(2, val); break;
            case I8X41_R3:	set_R(3, val); break;
            case I8X41_R4:	set_R(4, val); break;
            case I8X41_R5:	set_R(5, val); break;
            case I8X41_R6:	set_R(6, val); break;
            case I8X41_R7:	set_R(7, val); break;
            case I8X41_DATA:
                            i8x41.psw &= ~F1;
                            i8x41.dbbi = val;
                            if (i8x41.subtype == 8041) /* plain 8041 had no split input/output DBB buffers */
                                    i8x41.dbbo = val;
                            if ((i8x41.enable & IBFI) != 0)
                                    i8x41_set_irq_line(I8X41_INT_IBF, HOLD_LINE);
                            else
                                    i8x41.state |= IBF;
                            break;
            case I8X41_CMND:
                            i8x41.psw |= F1;
                            i8x41.dbbi = val;
                            if (i8x41.subtype == 8041) /* plain 8041 had no split input/output DBB buffers */
                                    i8x41.dbbo = val;
                            if ((i8x41.enable & IBFI) != 0)
                                    i8x41_set_irq_line(I8X41_INT_IBF, HOLD_LINE);
                            else
                                    i8x41.state |= IBF;
                            break;
            case I8X41_STAT:
                            /* writing status.. hmm, should we issue interrupts here too? */
                            i8x41.state = val;
                            break;
            default:
                    if( regnum <= REG_SP_CONTENTS )
                    {
                            int offset = (i8x41.psw & SP) + (REG_SP_CONTENTS - regnum);
                            if( offset < 8 )
                            {
                                    WM( M_STACK + offset, val & 0xff );
                                    WM( M_STACK + offset + 1, (val >> 8) & 0xff );
                            }
                    }
            }
    }

    public static void i8x41_set_irq_line(int irqline, int state)
    {
        System.out.println("i8x41_set_irq_line");
            switch( irqline )
            {
            case I8X41_INT_IBF:
                    if (state != CLEAR_LINE)
                    {
                            i8x41.state |= IBF;
                            if ((i8x41.enable & IBFI) != 0)
                            {
                                    WM( M_STACK + (i8x41.psw&SP) * 2 + 0, i8x41.pc & 0xff);
                                    WM( M_STACK + (i8x41.psw&SP) * 2 + 1, ((i8x41.pc >> 8) & 0x0f) | (i8x41.psw & 0xf0) );
                                    i8x41.psw = (i8x41.psw & ~SP) | ((i8x41.psw + 1) & SP);
                                    i8x41.pc = V_IBF;
                            }
                    }
                    else
                    {
                            i8x41.state &= ~IBF;
                    }
                    break;

            case I8X41_INT_TEST0:
                    if (state != CLEAR_LINE)
                            i8x41.state |= TEST0;
                    else
                            i8x41.state &= ~TEST0;
                    break;

            case I8X41_INT_TEST1:
                    if (state != CLEAR_LINE)
                    {
                            i8x41.state |= TEST1;
                    }
                    else
                    {
                            /* high to low transition? */
                            if ((i8x41.state & TEST1) != 0)
                            {
                                    /* counting enabled? */
                                    if ((i8x41.enable & CNT) != 0)
                                    {
                                            if (++i8x41.timer > 0x1fff)
                                            {
                                                    i8x41.tovf = 1;
                                                    if ((i8x41.enable & TCNTI) != 0)
                                                    {
                                                            WM( M_STACK + (i8x41.psw&SP) * 2 + 0, i8x41.pc & 0xff);
                                                            WM( M_STACK + (i8x41.psw&SP) * 2 + 1, ((i8x41.pc >> 8) & 0x0f) | (i8x41.psw & 0xf0) );
                                                            i8x41.psw = (i8x41.psw & ~SP) | ((i8x41.psw + 1) & SP);
                                                            i8x41.pc = V_TIMER;
                                                    }
                                            }
                                    }
                            }
                            i8x41.state &= ~TEST1;
                    }
                    break;
            }
    }

    public static void i8x41_set_irq_callback(irqcallbacksPtr callback)
    {
        System.out.println("i8x41_set_irq_callback");
            i8x41.irq_callback = callback;
    }

/*TODO*///void i8x41_state_save(void *file)
/*TODO*///{
/*TODO*///}
/*TODO*///
/*TODO*///void i8x41_state_load(void *file)
/*TODO*///{
/*TODO*///}
/*TODO*///
    
    public String i8x41_info(Object context, int regnum)
    {
/*TODO*///	static char buffer[8][15+1];
/*TODO*///	static int which = 0;
/*TODO*///	I8X41 *r = context;
/*TODO*///
/*TODO*///	which = (which+1) % 8;
/*TODO*///	buffer[which][0] = '\0';
/*TODO*///	if( !context )
/*TODO*///		r = &i8x41;
/*TODO*///
	switch( regnum )
	{
/*TODO*///		case CPU_INFO_REG+I8X41_PC: sprintf(buffer[which], "PC:%04X", r->pc); break;
/*TODO*///		case CPU_INFO_REG+I8X41_SP: sprintf(buffer[which], "S:%X", r->psw & SP); break;
/*TODO*///		case CPU_INFO_REG+I8X41_PSW:sprintf(buffer[which], "PSW:%02X", r->psw); break;
/*TODO*///		case CPU_INFO_REG+I8X41_A:	sprintf(buffer[which], "A:%02X", r->a); break;
/*TODO*///		case CPU_INFO_REG+I8X41_T:	sprintf(buffer[which], "T:%04X", r->timer); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R0: sprintf(buffer[which], "R0:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 0]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R1: sprintf(buffer[which], "R1:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 1]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R2: sprintf(buffer[which], "R2:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 2]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R3: sprintf(buffer[which], "R3:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 3]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R4: sprintf(buffer[which], "R4:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 4]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R5: sprintf(buffer[which], "R5:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 5]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R6: sprintf(buffer[which], "R6:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 6]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_R7: sprintf(buffer[which], "R7:%02X", i8x41.ram[((r->psw & F1) ? M_BANK1 : M_BANK0) + 7]); break;
/*TODO*///		case CPU_INFO_REG+I8X41_DATA:sprintf(buffer[which], "DBBI:%02X", i8x41.dbbi); break;
/*TODO*///		case CPU_INFO_REG+I8X41_CMND:sprintf(buffer[which], "DBBO:%02X", i8x41.dbbo); break;
/*TODO*///		case CPU_INFO_REG+I8X41_STAT:sprintf(buffer[which], "STAT:%02X", i8x41.state); break;
/*TODO*///		case CPU_INFO_FLAGS:
/*TODO*///			sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
/*TODO*///				r->psw & 0x80 ? 'C':'.',
/*TODO*///				r->psw & 0x40 ? 'A':'.',
/*TODO*///				r->psw & 0x20 ? '0':'.',
/*TODO*///				r->psw & 0x10 ? 'B':'.',
/*TODO*///				r->psw & 0x08 ? '?':'.',
/*TODO*///				r->psw & 0x04 ? 's':'.',
/*TODO*///				r->psw & 0x02 ? 's':'.',
/*TODO*///				r->psw & 0x01 ? 's':'.');
/*TODO*///			break;
		case CPU_INFO_NAME: return "I8X41";
		case CPU_INFO_FAMILY: return "Intel 8x41";
		case CPU_INFO_VERSION: return "0.1";
		case CPU_INFO_FILE: return "i8X41.java";
		case CPU_INFO_CREDITS: return "Copyright (c) 1999 Juergen Buchmueller, all rights reserved.";
/*TODO*///		case CPU_INFO_REG_LAYOUT: return (const char*)i8x41_reg_layout;
/*TODO*///		case CPU_INFO_WIN_LAYOUT: return (const char*)i8x41_win_layout;
	}
/*TODO*///	return buffer[which];
        throw new UnsupportedOperationException("Not supported yet.");
    }

/*TODO*///unsigned i8x41_dasm(char *buffer, unsigned pc)
/*TODO*///{
/*TODO*///#ifdef MAME_DEBUG
/*TODO*///	return Dasm8x41( buffer, pc );
/*TODO*///#else
/*TODO*///	sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///	return 1;
/*TODO*///#endif
/*TODO*///}
/*TODO*///
    
}
