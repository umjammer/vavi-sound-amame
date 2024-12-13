
package mame056.cpu.hd6309;

/*TODO*///import static mame056.cpu.hd6309.hd6309.*;
import static mame056.cpu.m6809.m6809.*;

public class _6309ops {

    /*

    HNZVC

    ? = undefined
    * = affected
    - = unaffected
    0 = cleared
    1 = set
    # = CCr directly affected by instruction
    @ = special - carry set if bit 7 is set

    */

/*TODO*///    public static void illegal()
/*TODO*///    {
/*TODO*////*TODO*///    	LOG(("HD6309: illegal opcode at %04x\nVectoring to [$fff0]\n",PC));
/*TODO*///    
/*TODO*///    	CC( CC() | CC_E); 				/* save entire state */
/*TODO*///    	PUSHWORD(pPC());
/*TODO*///    	PUSHWORD(pU());
/*TODO*///    	PUSHWORD(pY());
/*TODO*///    	PUSHWORD(pX());
/*TODO*///    	PUSHBYTE(DP());
/*TODO*///    
/*TODO*///    	if (( MD() & MD_EM ) != 0)
/*TODO*///    	{
/*TODO*///    		PUSHBYTE(F());
/*TODO*///    		PUSHBYTE(E());
/*TODO*///    		hd6309_ICount[0] -= 2;
/*TODO*///    	}
/*TODO*///    
/*TODO*///    	PUSHBYTE(B());
/*TODO*///    	PUSHBYTE(A());
/*TODO*///    	PUSHBYTE(CC());
/*TODO*///    
/*TODO*///    	PCD( RM16(0xfff0) );
/*TODO*///    	CHANGE_PC();
/*TODO*///    }
/*TODO*///    
/*TODO*///    public static void IIError()
/*TODO*///    {
/*TODO*///            SEII();		// Set illegal Instruction Flag
/*TODO*///            illegal();		// Vector to Trap handler
/*TODO*///    }
/*TODO*///
/*TODO*///    public static void DZError()
/*TODO*///    {
/*TODO*///            SEDZ();		// Set Division by Zero Flag
/*TODO*///            illegal();		// Vector to Trap handler
/*TODO*///    }
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///    /* $00 NEG direct ?**** */
/*TODO*///    public static void neg_di()
/*TODO*///    {
/*TODO*///            int r,t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = -t;
/*TODO*///            CLR_NZVC();
/*TODO*///            SET_FLAGS8(0,t,r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $01 OIM direct ?**** */
/*TODO*///    public static void oim_di()
/*TODO*///    {
/*TODO*///            int	r,t=0,im=0;
/*TODO*///            im = IMMBYTE(im);
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = im | t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_NZ8(r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $02 AIM direct */
/*TODO*///    public static void aim_di()
/*TODO*///    {
/*TODO*///            int	r,t=0,im=0;
/*TODO*///            im = IMMBYTE(im);
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = im & t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_NZ8(r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $03 COM direct -**01 */
/*TODO*///    public static void com_di()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            t = ~t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_NZ8(t);
/*TODO*///            SEC();
/*TODO*///            WM(EAD(),t);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $04 LSR direct -0*-* */
/*TODO*///    public static void lsr_di()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            CLR_NZC();
/*TODO*///            CC( CC() | (t & CC_C) );
/*TODO*///            t >>= 1;
/*TODO*///            SET_Z8(t);
/*TODO*///            WM(EAD(),t);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $05 EIM direct */
/*TODO*///    public static void eim_di()
/*TODO*///    {
/*TODO*///            int	r,t=0,im=0;
/*TODO*///            im = IMMBYTE(im);
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = im ^ t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_NZ8(r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $06 ROR direct -**-* */
/*TODO*///    public static void ror_di()
/*TODO*///    {
/*TODO*///            int t=0,r;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = (CC() & CC_C) << 7;
/*TODO*///            CLR_NZC();
/*TODO*///            CC( CC() | (t & CC_C) );
/*TODO*///            r |= t>>1;
/*TODO*///            SET_NZ8(r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $07 ASR direct ?**-* */
/*TODO*///    public static void asr_di()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            CLR_NZC();
/*TODO*///            CC ( CC() | (t & CC_C) );
/*TODO*///            t = (t & 0x80) | (t >> 1);
/*TODO*///            SET_NZ8(t);
/*TODO*///            WM(EAD(),t);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $08 ASL direct ?**** */
/*TODO*///    public static void asl_di()
/*TODO*///    {
/*TODO*///            int t=0,r;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = t << 1;
/*TODO*///            CLR_NZVC();
/*TODO*///            SET_FLAGS8(t,t,r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $09 ROL direct -**** */
/*TODO*///    public static void rol_di()
/*TODO*///    {
/*TODO*///            int t=0,r;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = (CC() & CC_C) | (t << 1);
/*TODO*///            CLR_NZVC();
/*TODO*///            SET_FLAGS8(t,t,r);
/*TODO*///            WM(EAD(),r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $0A DEC direct -***- */
/*TODO*///    public static void dec_di()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            --t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_FLAGS8D(t);
/*TODO*///            WM(EAD(),t);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $0B TIM direct */
/*TODO*///    public static void tim_di()
/*TODO*///    {
/*TODO*///            int	r,t=0,im=0;
/*TODO*///            im = IMMBYTE(im);
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            r = im & t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_NZ8(r);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $OC INC direct -***- */
/*TODO*///    public static void inc_di()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            ++t;
/*TODO*///            CLR_NZV();
/*TODO*///            SET_FLAGS8I(t);
/*TODO*///            WM(EAD(),t);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $OD TST direct -**0- */
/*TODO*///    public static void tst_di()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = DIRBYTE(t);
/*TODO*///            CLR_NZV();
/*TODO*///            SET_NZ8(t);
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $0E JMP direct ----- */
/*TODO*///    public static void jmp_di()
/*TODO*///    {
/*TODO*///            DIRECT();
/*TODO*///            PCD( EAD() );
/*TODO*///            CHANGE_PC();
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $0F CLR direct -0100 */
/*TODO*///    public static void clr_di()
/*TODO*///    {
/*TODO*///    	DIRECT();
/*TODO*///    	WM(EAD(),0);
/*TODO*///    	CLR_NZVC();
/*TODO*///    	SEZ();
/*TODO*///    }
/*TODO*///
/*TODO*///
/*TODO*///    /* $10 FLAG */
/*TODO*///
/*TODO*///    /* $11 FLAG */
/*TODO*///
/*TODO*///    /* $12 NOP inherent ----- */
/*TODO*///    public static void nop()
/*TODO*///    {
/*TODO*///            ;
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $13 SYNC inherent ----- */
/*TODO*///    public static void sync()
/*TODO*///    {
/*TODO*///            /* SYNC stops processing instructions until an interrupt request happens. */
/*TODO*///            /* This doesn't require the corresponding interrupt to be enabled: if it */
/*TODO*///            /* is disabled, execution continues with the next instruction. */
/*TODO*///            hd6309.int_state |= HD6309_SYNC;	 /* HJB 990227 */
/*TODO*///            CHECK_IRQ_LINES();
/*TODO*///            /* if HD6309_SYNC has not been cleared by CHECK_IRQ_LINES(),
/*TODO*///             * stop execution until the interrupt lines change. */
/*TODO*///            if(( hd6309.int_state & HD6309_SYNC ) != 0)
/*TODO*///                    if (hd6309_ICount[0] > 0) hd6309_ICount[0] = 0;
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $14 sexw inherent */
/*TODO*///    public static void sexw()
/*TODO*///    {
/*TODO*///    	int t;
/*TODO*///    	t = SIGNED_16( W() );
/*TODO*///    	D( t );
/*TODO*///    	CLR_NZV();
/*TODO*///    	SET_N8(A());
/*TODO*///    	if ( D() == 0 && W() == 0 ) SEZ();
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $15 ILLEGAL */
/*TODO*///    
/*TODO*///    /* $16 LBRA relative ----- */
/*TODO*///    public static void lbra()
/*TODO*///    {
/*TODO*///    	ea = IMMWORD(ea);
/*TODO*///    	PC( PC() + EA() );
/*TODO*///    	CHANGE_PC();
/*TODO*///    
/*TODO*///    	if ( EA() == 0xfffd )  /* EHC 980508 speed up busy loop */
/*TODO*///    		if ( hd6309_ICount[0] > 0)
/*TODO*///    			hd6309_ICount[0] = 0;
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $17 LBSR relative ----- */
/*TODO*///    public static void lbsr()
/*TODO*///    {
/*TODO*///    	ea = IMMWORD(ea);
/*TODO*///    	PUSHWORD(pPC());
/*TODO*///    	PC( PC() + EA() );
/*TODO*///    	CHANGE_PC();
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $18 ILLEGAL */
/*TODO*///    
/*TODO*///    /* $19 DAA inherent (A) -**0* */
/*TODO*///    public static void daa()
/*TODO*///    {
/*TODO*///    	int msn, lsn;
/*TODO*///    	int t, cf = 0;
/*TODO*///    	msn = A() & 0xf0; lsn = A() & 0x0f;
/*TODO*///    	if( lsn>0x09 || (CC() & CC_H)!=0) cf |= 0x06;
/*TODO*///    	if( msn>0x80 && lsn>0x09 ) cf |= 0x60;
/*TODO*///    	if( msn>0x90 || (CC() & CC_C)!=0) cf |= 0x60;
/*TODO*///    	t = cf + A();
/*TODO*///    	CLR_NZV(); /* keep carry from previous operation */
/*TODO*///    	SET_NZ8(t); SET_C8(t);
/*TODO*///    	A( t );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1A ORCC immediate ##### */
/*TODO*///    public static void orcc()
/*TODO*///    {
/*TODO*///            int t=0;
/*TODO*///            t = IMMBYTE(t);
/*TODO*///            CC( CC() | t );
/*TODO*///            CHECK_IRQ_LINES();	/* HJB 990116 */
/*TODO*///    }
/*TODO*///
/*TODO*///    /* $1B ILLEGAL */
/*TODO*///    
/*TODO*///    /* $1C ANDCC immediate ##### */
/*TODO*///    public static void andcc()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	CC( CC() & t );
/*TODO*///    	CHECK_IRQ_LINES();	/* HJB 990116 */
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1D SEX inherent -**0- */
/*TODO*///    public static void sex()
/*TODO*///    {
/*TODO*///    	int t;
/*TODO*///    	t = SIGNED(B());
/*TODO*///    	D( t );
/*TODO*///    	CLR_NZV();
/*TODO*///    	SET_NZ16(t);
/*TODO*///    }
/*TODO*///    
/*TODO*///    public static void exg()
/*TODO*///    {
/*TODO*///    	int t1,t2;
/*TODO*///    	int tb=0;
/*TODO*///    	int 	promote = 0;
/*TODO*///    
/*TODO*///    	tb = IMMBYTE(tb);
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)	/* HJB 990225: mixed 8/16 bit case? */
/*TODO*///    	{
/*TODO*///    		promote = 1;
/*TODO*///    	}
/*TODO*///    
/*TODO*///    	switch(tb>>4) {
/*TODO*///    		case  0: t1 = D();  break;
/*TODO*///    		case  1: t1 = X();  break;
/*TODO*///    		case  2: t1 = Y();  break;
/*TODO*///    		case  3: t1 = U();  break;
/*TODO*///    		case  4: t1 = S();  break;
/*TODO*///    		case  5: t1 = PC(); break;
/*TODO*///    		case  6: t1 = W();  break;
/*TODO*///    		case  7: t1 = V();  break;
/*TODO*///    		case  8: t1 = (promote!=0 ? D() : A());  break;
/*TODO*///    		case  9: t1 = (promote!=0 ? D() : B());  break;
/*TODO*///    		case 10: t1 = CC(); break;
/*TODO*///    		case 11: t1 = DP(); break;
/*TODO*///    		case 12: t1 = 0;  break;
/*TODO*///    		case 13: t1 = 0;  break;
/*TODO*///    		case 14: t1 = (promote!=0 ? W() : E() ); break;
/*TODO*///    		default: t1 = (promote!=0 ? W() : F() ); break;
/*TODO*///    	}
/*TODO*///    	switch(tb&15) {
/*TODO*///    		case  0: t2 = D();  break;
/*TODO*///    		case  1: t2 = X();  break;
/*TODO*///    		case  2: t2 = Y();  break;
/*TODO*///    		case  3: t2 = U();  break;
/*TODO*///    		case  4: t2 = S();  break;
/*TODO*///    		case  5: t2 = PC(); break;
/*TODO*///    		case  6: t2 = W();  break;
/*TODO*///    		case  7: t2 = V();  break;
/*TODO*///    		case  8: t2 = (promote!=0 ? D() : A());  break;
/*TODO*///    		case  9: t2 = (promote!=0 ? D() : B());  break;
/*TODO*///    		case 10: t2 = CC(); break;
/*TODO*///    		case 11: t2 = DP(); break;
/*TODO*///    		case 12: t2 = 0;  break;
/*TODO*///    		case 13: t2 = 0;  break;
/*TODO*///    		case 14: t2 = (promote!=0 ? W() : E()); break;
/*TODO*///    		default: t2 = (promote!=0 ? W() : F()); break;
/*TODO*///    	}
/*TODO*///    
/*TODO*///    	switch(tb>>4) {
/*TODO*///    		case  0: D( t2 );  break;
/*TODO*///    		case  1: X( t2 );  break;
/*TODO*///    		case  2: Y( t2 );  break;
/*TODO*///    		case  3: U( t2 );  break;
/*TODO*///    		case  4: S( t2 );  break;
/*TODO*///    		case  5: PC( t2 ); CHANGE_PC(); break;
/*TODO*///    		case  6: W( t2 );  break;
/*TODO*///    		case  7: V( t2 );  break;
/*TODO*///    		case  8: if (promote!=0) D( t2 ); else A( t2 ); break;
/*TODO*///    		case  9: if (promote!=0) D( t2 ); else B( t2 ); break;
/*TODO*///    		case 10: CC( t2 ); break;
/*TODO*///    		case 11: DP( t2 ); break;
/*TODO*///    		case 12: /* 0 = t2 */ break;
/*TODO*///    		case 13: /* 0 = t2 */ break;
/*TODO*///    		case 14: if (promote!=0) W( t2 ); else E( t2 ); break;
/*TODO*///    		case 15: if (promote!=0) W( t2 ); else F( t2 ); break;
/*TODO*///    	}
/*TODO*///    	switch(tb&15) {
/*TODO*///    		case  0: D( t1 );  break;
/*TODO*///    		case  1: X( t1 );  break;
/*TODO*///    		case  2: Y( t1 );  break;
/*TODO*///    		case  3: U( t1 );  break;
/*TODO*///    		case  4: S( t1 );  break;
/*TODO*///    		case  5: PC( t1 ); CHANGE_PC(); break;
/*TODO*///    		case  6: W( t1 );  break;
/*TODO*///    		case  7: V( t1 );  break;
/*TODO*///    		case  8: if (promote!=0) D( t1 ); else A( t1 ); break;
/*TODO*///    		case  9: if (promote!=0) D( t1 ); else B( t1 ); break;
/*TODO*///    		case 10: CC( t1 ); break;
/*TODO*///    		case 11: DP( t1 ); break;
/*TODO*///    		case 12: /* 0 = t1 */ break;
/*TODO*///    		case 13: /* 0 = t1 */ break;
/*TODO*///    		case 14: if (promote!=0) W( t1 ); else E( t1 ); break;
/*TODO*///    		case 15: if (promote!=0) W( t1 ); else F( t1 ); break;
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1F TFR inherent ----- */
/*TODO*///    public static void tfr()
/*TODO*///    {
/*TODO*///    	int tb=0;
/*TODO*///    	int t;
/*TODO*///    	int 	promote = 0;
/*TODO*///    
/*TODO*///    	tb = IMMBYTE(tb);
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    	{
/*TODO*///    		promote = 1;
/*TODO*///    	}
/*TODO*///    
/*TODO*///    	switch(tb>>4) {
/*TODO*///    		case  0: t = D();  break;
/*TODO*///    		case  1: t = X();  break;
/*TODO*///    		case  2: t = Y();  break;
/*TODO*///    		case  3: t = U();  break;
/*TODO*///    		case  4: t = S();  break;
/*TODO*///    		case  5: t = PC(); break;
/*TODO*///    		case  6: t = W();  break;
/*TODO*///    		case  7: t = V();  break;
/*TODO*///    		case  8: t = (promote!=0 ? D() : A() );  break;
/*TODO*///    		case  9: t = (promote!=0 ? D() : B() );  break;
/*TODO*///    		case 10: t = CC(); break;
/*TODO*///    		case 11: t = DP(); break;
/*TODO*///    		case 12: t = 0;  break;
/*TODO*///    		case 13: t = 0;  break;
/*TODO*///    		case 14: t = (promote!=0 ? W() : E() ); break;
/*TODO*///    		default: t = (promote!=0 ? W() : F() ); break;
/*TODO*///    	}
/*TODO*///    
/*TODO*///    	switch(tb&15) {
/*TODO*///    		case  0: D( t );  break;
/*TODO*///    		case  1: X( t );  break;
/*TODO*///    		case  2: Y( t );  break;
/*TODO*///    		case  3: U( t );  break;
/*TODO*///    		case  4: S( t );  break;
/*TODO*///    		case  5: PC( t ); CHANGE_PC(); break;
/*TODO*///    		case  6: W( t );  break;
/*TODO*///    		case  7: V( t );  break;
/*TODO*///    		case  8: if (promote!=0) D( t ); else A( t ); break;
/*TODO*///    		case  9: if (promote!=0) D( t ); else B( t ); break;
/*TODO*///    		case 10: CC( t ); break;
/*TODO*///    		case 11: DP( t ); break;
/*TODO*///    		case 12: /* 0 = t1 */ break;
/*TODO*///    		case 13: /* 0 = t1 */ break;
/*TODO*///    		case 14: if (promote!=0) W( t ); else E( t ); break;
/*TODO*///    		case 15: if (promote!=0) W( t ); else F( t ); break;
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $20 BRA relative ----- */
/*TODO*///    public static void bra()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	PC( PC() + SIGNED(t) );
/*TODO*///    	CHANGE_PC();
/*TODO*///    	/* JB 970823 - speed up busy loops */
/*TODO*///    	if( t == 0xfe )
/*TODO*///    		if( hd6309_ICount[0] > 0 ) hd6309_ICount[0] = 0;
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $21 BRN relative ----- */
/*TODO*///    public static void brn()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1021 LBRN relative ----- */
/*TODO*///    public static void lbrn()
/*TODO*///    {
/*TODO*///    	ea = IMMWORD(ea);
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $22 BHI relative ----- */
/*TODO*///    public static void bhi()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC() & (CC_Z|CC_C))==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1022 LBHI relative ----- */
/*TODO*///    public static void lbhi()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC() & (CC_Z|CC_C))==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $23 BLS relative ----- */
/*TODO*///    public static void bls()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC() & (CC_Z|CC_C)) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1023 LBLS relative ----- */
/*TODO*///    public static void lbls()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&(CC_Z|CC_C)) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $24 BCC relative ----- */
/*TODO*///    public static void bcc()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_C)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1024 LBCC relative ----- */
/*TODO*///    public static void lbcc()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_C)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $25 BCS relative ----- */
/*TODO*///    public static void bcs()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_C) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1025 LBCS relative ----- */
/*TODO*///    public static void lbcs()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_C) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $26 BNE relative ----- */
/*TODO*///    public static void bne()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_Z)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1026 LBNE relative ----- */
/*TODO*///    public static void lbne()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_Z)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $27 BEQ relative ----- */
/*TODO*///    public static void beq()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_Z) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1027 LBEQ relative ----- */
/*TODO*///    public static void lbeq()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_Z) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $28 BVC relative ----- */
/*TODO*///    public static void bvc()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_V)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1028 LBVC relative ----- */
/*TODO*///    public static void lbvc()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_V)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $29 BVS relative ----- */
/*TODO*///    public static void bvs()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_V) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1029 LBVS relative ----- */
/*TODO*///    public static void lbvs()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_V) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $2A BPL relative ----- */
/*TODO*///    public static void bpl()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_N)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $102A LBPL relative ----- */
/*TODO*///    public static void lbpl()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_N)==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $2B BMI relative ----- */
/*TODO*///    public static void bmi()
/*TODO*///    {
/*TODO*///    	BRANCH( (CC()&CC_N) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $102B LBMI relative ----- */
/*TODO*///    public static void lbmi()
/*TODO*///    {
/*TODO*///    	LBRANCH( (CC()&CC_N) );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $2C BGE relative ----- */
/*TODO*///    public static void bge()
/*TODO*///    {
/*TODO*///    	BRANCH( NXORV()!=0?0:1 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $102C LBGE relative ----- */
/*TODO*///    public static void lbge()
/*TODO*///    {
/*TODO*///    	LBRANCH( NXORV()==0?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $2D BLT relative ----- */
/*TODO*///    public static void blt()
/*TODO*///    {
/*TODO*///    	BRANCH( NXORV() );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $102D LBLT relative ----- */
/*TODO*///    public static void lblt()
/*TODO*///    {
/*TODO*///    	LBRANCH( NXORV() );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $2E BGT relative ----- */
/*TODO*///    public static void bgt()
/*TODO*///    {
/*TODO*///    	BRANCH( (NXORV()!=0 || (CC()&CC_Z)!=0)?0:1 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $102E LBGT relative ----- */
/*TODO*///    public static void lbgt()
/*TODO*///    {
/*TODO*///    	LBRANCH( (NXORV()!=0 || (CC()&CC_Z)!=0)?0:1 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $2F BLE relative ----- */
/*TODO*///    public static void ble()
/*TODO*///    {
/*TODO*///    	BRANCH( (NXORV()!=0 || (CC()&CC_Z)!=0)?1:0 );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $102F LBLE relative ----- */
/*TODO*///    public static void lble()
/*TODO*///    {
/*TODO*///    	LBRANCH( (NXORV()!=0 || (CC()&CC_Z)!=0)?1:0 );
/*TODO*///    }
/*TODO*///    /*
/*TODO*///    public static void REGREG_PREAMBLE()
/*TODO*///    {
/*TODO*///    	
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = &D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = &X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = &Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = &U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = &S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = &PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = &W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = &V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = &D; else src8Reg = &A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = &D; else src8Reg = &B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = &z16; else src8Reg = &CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = &z16; else src8Reg = &DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = &z16; else src8Reg = &z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = &z16; else src8Reg = &z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = &W; else src8Reg = &E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = &W; else src8Reg = &F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = &D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = &X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = &Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = &U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = &S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = &PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = &W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = &V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = &D; else dst8Reg = &A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = &D; else dst8Reg = &B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = &z16; else dst8Reg = &CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = &z16; else dst8Reg = &DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = &z16; else dst8Reg = &z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = &z16; else dst8Reg = &z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = &W; else dst8Reg = &E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = &W; else dst8Reg = &F; break;		
/*TODO*///    	}																		
/*TODO*///    }
/*TODO*///    */
/*TODO*///    
/*TODO*///    
/*TODO*///    // method added by Chuso
/*TODO*///    static enum RegNum {_D, _E, _X, _Y, _U, _S, _PC, _W, _V, _A, _B, _z8, _z16, _CC, _DP, _F};
/*TODO*///    public static void setDstReg(RegNum reg, int value)
/*TODO*///    {
/*TODO*///        switch (reg){
/*TODO*///            case _D:
/*TODO*///                D(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _E:
/*TODO*///                E(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _X:
/*TODO*///                X(value);
/*TODO*///                break;
/*TODO*///                    
/*TODO*///            case _Y:
/*TODO*///                Y(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _U:
/*TODO*///                U(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _S:
/*TODO*///                S(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _PC:
/*TODO*///                PC(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _W:
/*TODO*///                W(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _V:
/*TODO*///                V(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _A:
/*TODO*///                A(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _B:
/*TODO*///                B(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _z8:
/*TODO*///                SET_Z8(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _z16:
/*TODO*///                SET_Z16(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _CC:
/*TODO*///                CC(value);
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _DP:
/*TODO*///                DP(value);
/*TODO*///                break;
/*TODO*///            case _F:
/*TODO*///                F(value);
/*TODO*///                break;
/*TODO*///        }
/*TODO*///    }
/*TODO*///    
/*TODO*///    public static int getDstReg(RegNum reg)
/*TODO*///    {
/*TODO*///        int value=0;
/*TODO*///        
/*TODO*///        switch (reg){
/*TODO*///            case _D:
/*TODO*///                value=D();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _E:
/*TODO*///                value=E();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _X:
/*TODO*///                value=X();
/*TODO*///                break;
/*TODO*///                    
/*TODO*///            case _Y:
/*TODO*///                value=Y();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _U:
/*TODO*///                value=U();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _S:
/*TODO*///                value=S();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _PC:
/*TODO*///                value=PC();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _W:
/*TODO*///                value=W();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _V:
/*TODO*///                value=V();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _A:
/*TODO*///                value=A();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _B:
/*TODO*///                value=B();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _z8:
/*TODO*///                value=GET_Z();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _z16:
/*TODO*///                value=GET_Z();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _CC:
/*TODO*///                value=CC();
/*TODO*///                break;
/*TODO*///                
/*TODO*///            case _DP:
/*TODO*///                value=DP();
/*TODO*///                break;
/*TODO*///            case _F:
/*TODO*///                value=F();
/*TODO*///                break;
/*TODO*///        }
/*TODO*///        
/*TODO*///        
/*TODO*///        return value;
/*TODO*///    }
/*TODO*///    // end method added by Chuso
/*TODO*///    
/*TODO*///    /* $1030 addr_r r1 + r2 -> r2 */
/*TODO*///    public static void addr_r()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(src16Reg) + getDstReg(dst16Reg);
/*TODO*///    		CLR_HNZVC();
/*TODO*///    		SET_FLAGS16( getDstReg(src16Reg), getDstReg(dst16Reg), r16);
/*TODO*///    		setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(src8Reg) + getDstReg(dst8Reg);
/*TODO*///    		CLR_HNZVC();
/*TODO*///    		SET_FLAGS8(getDstReg(src8Reg), getDstReg(dst8Reg), r8);
/*TODO*///    		/* SET_H(*src8Reg,*src8Reg,r8);*/ /*Experimentation prooved this not to be the case */
/*TODO*///    		setDstReg( dst8Reg, r8 );
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    
/*TODO*///    public static void adcr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(src16Reg) + getDstReg(dst16Reg) + (CC() & CC_C);
/*TODO*///    		CLR_HNZVC();
/*TODO*///    		SET_FLAGS16(getDstReg(src16Reg), getDstReg(dst16Reg), r16);
/*TODO*///    		      setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(src8Reg) + getDstReg(dst8Reg) + (CC() & CC_C);
/*TODO*///    		CLR_HNZVC();
/*TODO*///    		SET_FLAGS8(getDstReg(src8Reg), getDstReg(dst8Reg), r8);
/*TODO*///    		/* SET_H(*src8Reg,*src8Reg,r8);*/ /*Experimentation prooved this not to be the case */
/*TODO*///    		      setDstReg(dst8Reg, r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1032 SUBR r1 - r2 -> r2 */
/*TODO*///    public static void subr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(dst16Reg) - getDstReg(src16Reg);
/*TODO*///    		CLR_NZVC();
/*TODO*///    		SET_FLAGS16(getDstReg(dst16Reg), getDstReg(src16Reg), r16);
/*TODO*///    		      setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(dst8Reg) - getDstReg(src8Reg);
/*TODO*///    		CLR_NZVC();
/*TODO*///    		SET_FLAGS8(getDstReg(dst8Reg), getDstReg(src8Reg),r8);
/*TODO*///    		      setDstReg(dst8Reg, r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1033 SBCR r1 - r2 - C -> r2 */
/*TODO*///    public static void sbcr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(dst16Reg) - getDstReg(src16Reg) - (CC() & CC_C);
/*TODO*///    		CLR_NZVC();
/*TODO*///    		SET_FLAGS16(getDstReg(dst16Reg), getDstReg(src16Reg), r16);
/*TODO*///    		      setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(dst8Reg) - getDstReg(src8Reg) - (CC() & CC_C);
/*TODO*///    		CLR_NZVC();
/*TODO*///    		SET_FLAGS8(getDstReg(dst8Reg), getDstReg(src8Reg), r8);
/*TODO*///    		      setDstReg(dst8Reg, r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1034 ANDR r1 & r2 -> r2 */
/*TODO*///    public static void andr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(src16Reg) & getDstReg(dst16Reg);
/*TODO*///    		CLR_NZV();
/*TODO*///    		SET_NZ16(r16);
/*TODO*///    		      setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(src8Reg) & getDstReg(dst8Reg);
/*TODO*///    		CLR_NZV();
/*TODO*///    		SET_NZ8(r8);
/*TODO*///    		      setDstReg(dst8Reg, r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1035 ORR r1 | r2 -> r2 */
/*TODO*///    public static void orr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(src16Reg) | getDstReg(dst16Reg);
/*TODO*///    		CLR_NZV();
/*TODO*///    		SET_NZ16(r16);
/*TODO*///    		      setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(src8Reg) | getDstReg(dst8Reg);
/*TODO*///    		CLR_NZV();
/*TODO*///    		SET_NZ8(r8);
/*TODO*///    		      setDstReg(dst8Reg, r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1036 EORR r1 ^ r2 -> r2 */
/*TODO*///    public static void eorr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(src16Reg) ^ getDstReg(dst16Reg);
/*TODO*///    		CLR_NZV();
/*TODO*///    		SET_NZ16(r16);
/*TODO*///    		      setDstReg(dst16Reg, r16);
/*TODO*///    
/*TODO*///    		if ( (tb&15) == 5 )
/*TODO*///    		{
/*TODO*///    			CHANGE_PC();
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(src8Reg) ^ getDstReg(dst8Reg);
/*TODO*///    		CLR_NZV();
/*TODO*///    		SET_NZ8(r8);
/*TODO*///    		      setDstReg(dst8Reg, r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1037 CMPR r1 - r2 */
/*TODO*///    public static void cmpr()
/*TODO*///    {
/*TODO*///    	int	tb=0, z8 = 0;
/*TODO*///    	int	z16 = 0, r8;
/*TODO*///    	int	r16;
/*TODO*///    	RegNum	src8Reg = null, dst8Reg = null;
/*TODO*///    	RegNum	src16Reg = null, dst16Reg = null;
/*TODO*///    	int 	promote = 0, large = 0;
/*TODO*///    
/*TODO*///        //	REGREG_PREAMBLE;
/*TODO*///        tb = IMMBYTE(tb);															
/*TODO*///    	if(( (tb^(tb>>4)) & 0x08 ) != 0)
/*TODO*///    		{promote = 1;}
/*TODO*///    	switch(tb>>4) {															
/*TODO*///    		case  0: src16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: src16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: src16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: src16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: src16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: src16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: src16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: src16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) src16Reg = RegNum._D; else src8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) src16Reg = RegNum._z16; else src8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) src16Reg = RegNum._W; else src8Reg = RegNum._F; break;		
/*TODO*///    	}																		
/*TODO*///    	switch(tb&15) {															
/*TODO*///    		case  0: dst16Reg = RegNum._D; large = 1;  break;						
/*TODO*///    		case  1: dst16Reg = RegNum._X; large = 1;  break;						
/*TODO*///    		case  2: dst16Reg = RegNum._Y; large = 1;  break;						
/*TODO*///    		case  3: dst16Reg = RegNum._U; large = 1;  break;						
/*TODO*///    		case  4: dst16Reg = RegNum._S; large = 1;  break;						
/*TODO*///    		case  5: dst16Reg = RegNum._PC; large = 1; break;						
/*TODO*///    		case  6: dst16Reg = RegNum._W; large = 1;  break;						
/*TODO*///    		case  7: dst16Reg = RegNum._V; large = 1;  break;						
/*TODO*///    		case  8: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._A; break;		
/*TODO*///    		case  9: if (promote!=0) dst16Reg = RegNum._D; else dst8Reg = RegNum._B; break;		
/*TODO*///    		case 10: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._CC; break;	
/*TODO*///    		case 11: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._DP; break;	
/*TODO*///    		case 12: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 13: if (promote!=0) dst16Reg = RegNum._z16; else dst8Reg = RegNum._z8; break;	
/*TODO*///    		case 14: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._E; break;		
/*TODO*///    		default: if (promote!=0) dst16Reg = RegNum._W; else dst8Reg = RegNum._F; break;		
/*TODO*///    	}
/*TODO*///        //	END REGREG_PREAMBLE;
/*TODO*///    
/*TODO*///    	if ( large != 0 )
/*TODO*///    	{
/*TODO*///    		r16 = getDstReg(dst16Reg) - getDstReg(src16Reg);
/*TODO*///    		CLR_NZVC();
/*TODO*///    		SET_FLAGS16(getDstReg(dst16Reg), getDstReg(src16Reg), r16);
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    	{
/*TODO*///    		r8 = getDstReg(dst8Reg) - getDstReg(src8Reg);
/*TODO*///    		CLR_NZVC();
/*TODO*///    		SET_FLAGS8(getDstReg(dst8Reg), getDstReg(src8Reg), r8);
/*TODO*///    	}
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1138 TFM R0+,R1+ */
/*TODO*///    public static void tfmpp()
/*TODO*///    {
/*TODO*///    	int	tb=0, srcValue = 0;
/*TODO*///    	int 	done = 0;
/*TODO*///    
/*TODO*///    	tb = IMMBYTE(tb);
/*TODO*///    
/*TODO*///    	if ( W() != 0 )
/*TODO*///    	{
/*TODO*///    		switch(tb>>4) {
/*TODO*///    			case  0: srcValue = RM(D()); D(D()+1); break;
/*TODO*///    			case  1: srcValue = RM(X()); X(X()+1); break;
/*TODO*///    			case  2: srcValue = RM(Y()); Y(Y()+1); break;
/*TODO*///    			case  3: srcValue = RM(U()); U(U()+1); break;
/*TODO*///    			case  4: srcValue = RM(S()); S(S()+1); break;
/*TODO*///    			case  5: /* PC */ done = 1; break;
/*TODO*///    			case  6: /* W  */ done = 1; break;
/*TODO*///    			case  7: /* V  */ done = 1; break;
/*TODO*///    			case  8: /* A  */ done = 1; break;
/*TODO*///    			case  9: /* B  */ done = 1; break;
/*TODO*///    			case 10: /* CC */ done = 1; break;
/*TODO*///    			case 11: /* DP */ done = 1; break;
/*TODO*///    			case 12: /* 0  */ done = 1; break;
/*TODO*///    			case 13: /* 0  */ done = 1; break;
/*TODO*///    			case 14: /* E  */ done = 1; break;
/*TODO*///    			default: /* F  */ done = 1; break;
/*TODO*///    		}
/*TODO*///    
/*TODO*///    		if (done == 0)
/*TODO*///    		{
/*TODO*///    			switch(tb&15) {
/*TODO*///    				case  0: WM(D(), srcValue); D(D()+1); break;
/*TODO*///    				case  1: WM(X(), srcValue); X(X()+1); break;
/*TODO*///    				case  2: WM(Y(), srcValue); Y(Y()+1); break;
/*TODO*///    				case  3: WM(U(), srcValue); U(U()+1); break;
/*TODO*///    				case  4: WM(S(), srcValue); S(S()+1); break;
/*TODO*///    				case  5: /* PC */ done = 1; break;
/*TODO*///    				case  6: /* W  */ done = 1; break;
/*TODO*///    				case  7: /* V  */ done = 1; break;
/*TODO*///    				case  8: /* A  */ done = 1; break;
/*TODO*///    				case  9: /* B  */ done = 1; break;
/*TODO*///    				case 10: /* CC */ done = 1; break;
/*TODO*///    				case 11: /* DP */ done = 1; break;
/*TODO*///    				case 12: /* 0  */ done = 1; break;
/*TODO*///    				case 13: /* 0  */ done = 1; break;
/*TODO*///    				case 14: /* E  */ done = 1; break;
/*TODO*///    				default: /* F  */ done = 1; break;
/*TODO*///    			}
/*TODO*///    
/*TODO*///    			PCD( PCD() - 3 );
/*TODO*///    			CHANGE_PC();
/*TODO*///    			W( W() - 1 );
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    		hd6309_ICount[0] -= 3;   /* Needs three aditional cycles  to get the 6+3n */
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1139 TFM R0-,R1- */
/*TODO*///    public static void tfmmm()
/*TODO*///    {
/*TODO*///    	int	tb=0, srcValue = 0;
/*TODO*///    	int 	done = 0;
/*TODO*///    
/*TODO*///    	tb = IMMBYTE(tb);
/*TODO*///    
/*TODO*///    	if ( W() != 0 )
/*TODO*///    	{
/*TODO*///    		switch(tb>>4) {
/*TODO*///    			case  0: srcValue = RM(D()); D(D()-1); break;
/*TODO*///    			case  1: srcValue = RM(X()); X(X()-1); break;
/*TODO*///    			case  2: srcValue = RM(Y()); Y(Y()-1); break;
/*TODO*///    			case  3: srcValue = RM(U()); U(U()-1); break;
/*TODO*///    			case  4: srcValue = RM(S()); S(S()-1); break;
/*TODO*///    			case  5: /* PC */ done = 1; break;
/*TODO*///    			case  6: /* W  */ done = 1; break;
/*TODO*///    			case  7: /* V  */ done = 1; break;
/*TODO*///    			case  8: /* A  */ done = 1; break;
/*TODO*///    			case  9: /* B  */ done = 1; break;
/*TODO*///    			case 10: /* CC */ done = 1; break;
/*TODO*///    			case 11: /* DP */ done = 1; break;
/*TODO*///    			case 12: /* 0  */ done = 1; break;
/*TODO*///    			case 13: /* 0  */ done = 1; break;
/*TODO*///    			case 14: /* E  */ done = 1; break;
/*TODO*///    			default: /* F  */ done = 1; break;
/*TODO*///    		}
/*TODO*///    
/*TODO*///    		if (done == 0)
/*TODO*///    		{
/*TODO*///    			switch(tb&15) {
/*TODO*///    				case  0: WM(D(), srcValue); D(D()-1); break;
/*TODO*///    				case  1: WM(X(), srcValue); X(X()-1); break;
/*TODO*///    				case  2: WM(Y(), srcValue); Y(Y()-1); break;
/*TODO*///    				case  3: WM(U(), srcValue); U(U()-1); break;
/*TODO*///    				case  4: WM(S(), srcValue); S(S()-1); break;
/*TODO*///    				case  5: /* PC */ done = 1; break;
/*TODO*///    				case  6: /* W  */ done = 1; break;
/*TODO*///    				case  7: /* V  */ done = 1; break;
/*TODO*///    				case  8: /* A  */ done = 1; break;
/*TODO*///    				case  9: /* B  */ done = 1; break;
/*TODO*///    				case 10: /* CC */ done = 1; break;
/*TODO*///    				case 11: /* DP */ done = 1; break;
/*TODO*///    				case 12: /* 0  */ done = 1; break;
/*TODO*///    				case 13: /* 0  */ done = 1; break;
/*TODO*///    				case 14: /* E  */ done = 1; break;
/*TODO*///    				default: /* F  */ done = 1; break;
/*TODO*///    			}
/*TODO*///    
/*TODO*///    			PCD( PCD() - 3 );
/*TODO*///    			CHANGE_PC();
/*TODO*///    			W(W()-1); ;
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    		hd6309_ICount[0] -= 3;   /* Needs three aditional cycles  to get the 6+3n */
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $113A TFM R0+,R1 */
/*TODO*///    public static void tfmpc()
/*TODO*///    {
/*TODO*///    	int	tb=0, srcValue = 0;
/*TODO*///    	int 	done = 0;
/*TODO*///    
/*TODO*///    	tb = IMMBYTE(tb);
/*TODO*///    
/*TODO*///    	if ( W() != 0 )
/*TODO*///    	{
/*TODO*///    		switch(tb>>4) {
/*TODO*///    			case  0: srcValue = RM(D()); D(D()+1); break;
/*TODO*///    			case  1: srcValue = RM(X()); X(X()+1); break;
/*TODO*///    			case  2: srcValue = RM(Y()); Y(Y()+1); break;
/*TODO*///    			case  3: srcValue = RM(U()); U(U()+1); break;
/*TODO*///    			case  4: srcValue = RM(S()); S(S()+1); break;
/*TODO*///    			case  5: /* PC */ done = 1; break;
/*TODO*///    			case  6: /* W  */ done = 1; break;
/*TODO*///    			case  7: /* V  */ done = 1; break;
/*TODO*///    			case  8: /* A  */ done = 1; break;
/*TODO*///    			case  9: /* B  */ done = 1; break;
/*TODO*///    			case 10: /* CC */ done = 1; break;
/*TODO*///    			case 11: /* DP */ done = 1; break;
/*TODO*///    			case 12: /* 0  */ done = 1; break;
/*TODO*///    			case 13: /* 0  */ done = 1; break;
/*TODO*///    			case 14: /* E  */ done = 1; break;
/*TODO*///    			default: /* F  */ done = 1; break;
/*TODO*///    		}
/*TODO*///    
/*TODO*///    		if (done == 0)
/*TODO*///    		{
/*TODO*///    			switch(tb&15) {
/*TODO*///    				case  0: WM(D(), srcValue); break;
/*TODO*///    				case  1: WM(X(), srcValue); break;
/*TODO*///    				case  2: WM(Y(), srcValue); break;
/*TODO*///    				case  3: WM(U(), srcValue); break;
/*TODO*///    				case  4: WM(S(), srcValue); break;
/*TODO*///    				case  5: /* PC */ done = 1; break;
/*TODO*///    				case  6: /* W  */ done = 1; break;
/*TODO*///    				case  7: /* V  */ done = 1; break;
/*TODO*///    				case  8: /* A  */ done = 1; break;
/*TODO*///    				case  9: /* B  */ done = 1; break;
/*TODO*///    				case 10: /* CC */ done = 1; break;
/*TODO*///    				case 11: /* DP */ done = 1; break;
/*TODO*///    				case 12: /* 0  */ done = 1; break;
/*TODO*///    				case 13: /* 0  */ done = 1; break;
/*TODO*///    				case 14: /* E  */ done = 1; break;
/*TODO*///    				default: /* F  */ done = 1; break;
/*TODO*///    			}
/*TODO*///    
/*TODO*///    			PCD( PCD() - 3 );
/*TODO*///    			CHANGE_PC();
/*TODO*///    			W(W()-1);
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    		hd6309_ICount[0] -= 3;   /* Needs three aditional cycles  to get the 6+3n */
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $113B TFM R0,R1+ */
/*TODO*///    public static void tfmcp()
/*TODO*///    {
/*TODO*///    	int	tb=0, srcValue = 0;
/*TODO*///    	int 	done = 0;
/*TODO*///    
/*TODO*///    	tb = IMMBYTE(tb);
/*TODO*///    
/*TODO*///    	if ( W() != 0 )
/*TODO*///    	{
/*TODO*///    		switch(tb>>4) {
/*TODO*///    			case  0: srcValue = RM(D()); break;
/*TODO*///    			case  1: srcValue = RM(X()); break;
/*TODO*///    			case  2: srcValue = RM(Y()); break;
/*TODO*///    			case  3: srcValue = RM(U()); break;
/*TODO*///    			case  4: srcValue = RM(S()); break;
/*TODO*///    			case  5: /* PC */ done = 1; break;
/*TODO*///    			case  6: /* W  */ done = 1; break;
/*TODO*///    			case  7: /* V  */ done = 1; break;
/*TODO*///    			case  8: /* A  */ done = 1; break;
/*TODO*///    			case  9: /* B  */ done = 1; break;
/*TODO*///    			case 10: /* CC */ done = 1; break;
/*TODO*///    			case 11: /* DP */ done = 1; break;
/*TODO*///    			case 12: /* 0  */ done = 1; break;
/*TODO*///    			case 13: /* 0  */ done = 1; break;
/*TODO*///    			case 14: /* E  */ done = 1; break;
/*TODO*///    			default: /* F  */ done = 1; break;
/*TODO*///    		}
/*TODO*///    
/*TODO*///    		if (done == 0)
/*TODO*///    		{
/*TODO*///    			switch(tb&15) {
/*TODO*///    				case  0: WM(D(), srcValue); D(D()+1); break;
/*TODO*///    				case  1: WM(X(), srcValue); X(X()+1); break;
/*TODO*///    				case  2: WM(Y(), srcValue); Y(Y()+1); break;
/*TODO*///    				case  3: WM(U(), srcValue); U(U()+1); break;
/*TODO*///    				case  4: WM(S(), srcValue); S(S()+1); break;
/*TODO*///    				case  5: /* PC */ done = 1; break;
/*TODO*///    				case  6: /* W  */ done = 1; break;
/*TODO*///    				case  7: /* V  */ done = 1; break;
/*TODO*///    				case  8: /* A  */ done = 1; break;
/*TODO*///    				case  9: /* B  */ done = 1; break;
/*TODO*///    				case 10: /* CC */ done = 1; break;
/*TODO*///    				case 11: /* DP */ done = 1; break;
/*TODO*///    				case 12: /* 0  */ done = 1; break;
/*TODO*///    				case 13: /* 0  */ done = 1; break;
/*TODO*///    				case 14: /* E  */ done = 1; break;
/*TODO*///    				default: /* F  */ done = 1; break;
/*TODO*///    			}
/*TODO*///    
/*TODO*///    			PCD( PCD() - 3 );
/*TODO*///    			CHANGE_PC();
/*TODO*///    			W(W()-1); ;
/*TODO*///    		}
/*TODO*///    	}
/*TODO*///    	else
/*TODO*///    		hd6309_ICount[0] -= 3;   /* Needs three aditional cycles  to get the 6+3n */
/*TODO*///    }
/*TODO*///    
/*TODO*///    /*TODO*////* $30 LEAX indexed --*-- */
/*TODO*///    /*TODO*///INLINE void leax( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	X = EA;
/*TODO*///    /*TODO*///	CLR_Z;
/*TODO*///    /*TODO*///	SET_Z(X);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $31 LEAY indexed --*-- */
/*TODO*///    /*TODO*///INLINE void leay( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	Y = EA;
/*TODO*///    /*TODO*///	CLR_Z;
/*TODO*///    /*TODO*///	SET_Z(Y);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $32 LEAS indexed ----- */
/*TODO*///    /*TODO*///INLINE void leas( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	S = EA;
/*TODO*///    /*TODO*///	hd6309.int_state |= HD6309_LDS;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $33 LEAU indexed ----- */
/*TODO*///    /*TODO*///INLINE void leau( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	U = EA;
/*TODO*///    /*TODO*///}
/*TODO*///    
/*TODO*///    /* $34 PSHS inherent ----- */
/*TODO*///    public static void pshs()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	if(( t&0x80 )!=0) { PUSHWORD(pPC()); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x40 )!=0) { PUSHWORD(pU());  hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x20 )!=0) { PUSHWORD(pY());  hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x10 )!=0) { PUSHWORD(pX());  hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x08 )!=0) { PUSHBYTE(DP());  hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x04 )!=0) { PUSHBYTE(B());   hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x02 )!=0) { PUSHBYTE(A());   hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x01 )!=0) { PUSHBYTE(CC());  hd6309_ICount[0] -= 1; }
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1038 PSHSW inherent ----- */
/*TODO*///    public static void pshsw()
/*TODO*///    {
/*TODO*///    	PUSHWORD(pW());
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $103a PSHUW inherent ----- */
/*TODO*///    public static void pshuw()
/*TODO*///    {
/*TODO*///    	PSHUWORD(pW());
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $35 PULS inherent ----- */
/*TODO*///    public static void puls()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	if(( t&0x01 ) != 0) { CC(PULLBYTE(CC())); hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x02 ) != 0) { A(PULLBYTE(A()));  hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x04 ) != 0) { B(PULLBYTE(B()));  hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x08 ) != 0) { DP(PULLBYTE(DP())); hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x10 ) != 0) { XD(PULLWORD(XD())); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x20 ) != 0) { YD(PULLWORD(YD())); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x40 ) != 0) { UD(PULLWORD(UD())); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x80 ) != 0) { PCD(PULLWORD(PCD())); CHANGE_PC(); hd6309_ICount[0] -= 2; }
/*TODO*///    
/*TODO*///    	/* HJB 990225: moved check after all PULLs */
/*TODO*///    	if(( t&0x01 ) != 0) { CHECK_IRQ_LINES(); }
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $1039 PULSW inherent ----- */
/*TODO*///    public static void pulsw()
/*TODO*///    {
/*TODO*///    	W(PULLWORD(W()));
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $103b PULUW inherent ----- */
/*TODO*///    public static void puluw()
/*TODO*///    {
/*TODO*///    	W(PULUWORD(W()));
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $36 PSHU inherent ----- */
/*TODO*///    public static void pshu()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	if(( t&0x80 ) != 0) { PSHUWORD(pPC()); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x40 ) != 0) { PSHUWORD(pS());  hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x20 ) != 0) { PSHUWORD(pY());  hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x10 ) != 0) { PSHUWORD(pX());  hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x08 ) != 0) { PSHUBYTE(DP());  hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x04 ) != 0) { PSHUBYTE(B());   hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x02 ) != 0) { PSHUBYTE(A());   hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x01 ) != 0) { PSHUBYTE(CC());  hd6309_ICount[0] -= 1; }
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* 37 PULU inherent ----- */
/*TODO*///    public static void pulu()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	if(( t&0x01 ) != 0) { CC(PULUBYTE(CC())); hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x02 ) != 0) { A(PULUBYTE(A()));  hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x04 ) != 0) { B(PULUBYTE(B()));  hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x08 ) != 0) { DP(PULUBYTE(DP())); hd6309_ICount[0] -= 1; }
/*TODO*///    	if(( t&0x10 ) != 0) { XD(PULUWORD(XD())); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x20 ) != 0) { YD(PULUWORD(YD())); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x40 ) != 0) { SD(PULUWORD(SD())); hd6309_ICount[0] -= 2; }
/*TODO*///    	if(( t&0x80 ) != 0) { PCD(PULUWORD(PCD())); CHANGE_PC(); hd6309_ICount[0] -= 2; }
/*TODO*///    
/*TODO*///    	/* HJB 990225: moved check after all PULLs */
/*TODO*///    	if(( t&0x01 ) != 0) { CHECK_IRQ_LINES(); }
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $38 ILLEGAL */
/*TODO*///    
/*TODO*///    /* $39 RTS inherent ----- */
/*TODO*///    public static void rts()
/*TODO*///    {
/*TODO*///    	PCD(PULLWORD(PCD()));
/*TODO*///    	CHANGE_PC();
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $3A ABX inherent ----- */
/*TODO*///    public static void abx()
/*TODO*///    {
/*TODO*///    	X( X() + B() );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $3B RTI inherent ##### */
/*TODO*///    public static void rti()
/*TODO*///    {
/*TODO*///    	int t;
/*TODO*///    	CC(PULLBYTE(CC()));
/*TODO*///    	t = CC() & CC_E;		/* HJB 990225: entire state saved? */
/*TODO*///    	if(t != 0)
/*TODO*///    	{
/*TODO*///    		hd6309_ICount[0] -= 9;
/*TODO*///    		A(PULLBYTE(A()));
/*TODO*///    		B(PULLBYTE(B()));
/*TODO*///    		if (( MD() & MD_EM ) != 0)
/*TODO*///    		{
/*TODO*///    			E(PULLBYTE(E()));
/*TODO*///    			F(PULLBYTE(F()));
/*TODO*///    			hd6309_ICount[0] -= 2;
/*TODO*///    		}
/*TODO*///    		DP(PULLBYTE(DP()));
/*TODO*///    		XD(PULLWORD(XD()));
/*TODO*///    		YD(PULLWORD(YD()));
/*TODO*///    		UD(PULLWORD(UD()));
/*TODO*///    	}
/*TODO*///    	PCD(PULLWORD(PCD()));
/*TODO*///    	CHANGE_PC();
/*TODO*///    	CHECK_IRQ_LINES();	/* HJB 990116 */
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $3C CWAI inherent ----1 */
/*TODO*///    public static void cwai()
/*TODO*///    {
/*TODO*///    	int t=0;
/*TODO*///    	t = IMMBYTE(t);
/*TODO*///    	CC( CC() & t );
/*TODO*///    	/*
/*TODO*///    	 * CWAI stacks the entire machine state on the hardware stack,
/*TODO*///    	 * then waits for an interrupt; when the interrupt is taken
/*TODO*///    	 * later, the state is *not* saved again after CWAI.
/*TODO*///    	 */
/*TODO*///    	CC( CC() | CC_E ); 		/* HJB 990225: save entire state */
/*TODO*///    	PUSHWORD(pPC());
/*TODO*///    	PUSHWORD(pU());
/*TODO*///    	PUSHWORD(pY());
/*TODO*///    	PUSHWORD(pX());
/*TODO*///    	PUSHBYTE(DP());
/*TODO*///    	if (( MD() & MD_EM ) != 0)
/*TODO*///    	{
/*TODO*///    		PUSHBYTE(E());
/*TODO*///    		PUSHBYTE(F());
/*TODO*///    	}
/*TODO*///    	PUSHBYTE(B());
/*TODO*///    	PUSHBYTE(A());
/*TODO*///    	PUSHBYTE(CC());
/*TODO*///    	hd6309.int_state |= HD6309_CWAI;	 /* HJB 990228 */
/*TODO*///    	CHECK_IRQ_LINES();	  /* HJB 990116 */
/*TODO*///    	if(( hd6309.int_state & HD6309_CWAI ) != 0)
/*TODO*///    		if( hd6309_ICount[0] > 0 )
/*TODO*///    			hd6309_ICount[0] = 0;
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $3D MUL inherent --*-@ */
/*TODO*///    public static void mul()
/*TODO*///    {
/*TODO*///    	int t;
/*TODO*///    	t = A() * B();
/*TODO*///    	CLR_ZC(); SET_Z16(t); if((t&0x80)!=0) SEC();
/*TODO*///    	D( t );
/*TODO*///    }
/*TODO*///    
/*TODO*///    /* $3E ILLEGAL */
/*TODO*///    
/*TODO*///    /* $3F SWI (SWI2 SWI3) absolute indirect ----- */
/*TODO*///    public static void swi()
/*TODO*///    {
/*TODO*///    	CC( CC() | CC_E ); 			/* HJB 980225: save entire state */
/*TODO*///    	PUSHWORD(pPC());
/*TODO*///    	PUSHWORD(pU());
/*TODO*///    	PUSHWORD(pY());
/*TODO*///    	PUSHWORD(pX());
/*TODO*///    	PUSHBYTE(DP());
/*TODO*///    	if (( MD() & MD_EM ) != 0)
/*TODO*///    	{
/*TODO*///    		PUSHBYTE(F());
/*TODO*///    		PUSHBYTE(E());
/*TODO*///    		hd6309_ICount[0] -= 2;
/*TODO*///    	}
/*TODO*///    	PUSHBYTE(B());
/*TODO*///    	PUSHBYTE(A());
/*TODO*///    	PUSHBYTE(CC());
/*TODO*///    	CC( CC() | CC_IF | CC_II );	/* inhibit FIRQ and IRQ */
/*TODO*///    	PCD(RM16(0xfffa));
/*TODO*///    	CHANGE_PC();
/*TODO*///    }
/*TODO*///    
/*TODO*///    /*TODO*////* $1130 BAND */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#define decodePB_tReg(n)	((n)&3)
/*TODO*///    /*TODO*///#define decodePB_src(n) 	(((n)>>2)&7)
/*TODO*///    /*TODO*///#define decodePB_dst(n) 	(((n)>>5)&7)
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///static unsigned char *	regTable[4] = { &(CC), &(A), &(B), &(E) };
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///static UINT8	bitTable[] = { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80 };
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void band( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( ( *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)] ) && ( db & bitTable[decodePB_src(pb)] ))
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1131 BIAND */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void biand( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( ( *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)] ) && ( (~db) & bitTable[decodePB_src(pb)] ))
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1132 BOR */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void bor( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( ( *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)] ) || ( db & bitTable[decodePB_src(pb)] ))
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1133 BIOR */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void bior( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( ( *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)] ) || ( (~db) & bitTable[decodePB_src(pb)] ))
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1134 BEOR */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void beor( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///	UINT8		tReg, tMem;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	tReg = *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	tMem = db & bitTable[decodePB_src(pb)];
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( (tReg || tMem ) && !(tReg && tMem) )
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1135 BIEOR */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void bieor( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///	UINT8		tReg, tMem;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	tReg = *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	tMem = (~db) & bitTable[decodePB_src(pb)];
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( (tReg || tMem ) && !(tReg && tMem) )
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1133 LDBT */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void ldbt( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( ( db & bitTable[decodePB_src(pb)] ) )
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) |= bitTable[decodePB_dst(pb)];
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		*(regTable[decodePB_tReg(pb)]) &= (~bitTable[decodePB_dst(pb)]);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1134 STBT */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///INLINE void stbt( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8		pb;
/*TODO*///    /*TODO*///	UINT16		db;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	pb = IMMBYTE(pb);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	db = DIRBYTE(db);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if ( ( *(regTable[decodePB_tReg(pb)]) & bitTable[decodePB_dst(pb)] ) )
/*TODO*///    /*TODO*///		WM( EAD, db | bitTable[decodePB_src(pb)] );
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		WM( EAD, db & (~bitTable[decodePB_src(pb)]) );
/*TODO*///    /*TODO*///}
/*TODO*///    
/*TODO*///    /* $103F SWI2 absolute indirect ----- */
/*TODO*///    public static void swi2()
/*TODO*///    {
/*TODO*///    	CC( CC() | CC_E ); 			/* HJB 980225: save entire state */
/*TODO*///    	PUSHWORD(pPC());
/*TODO*///    	PUSHWORD(pU());
/*TODO*///    	PUSHWORD(pY());
/*TODO*///    	PUSHWORD(pX());
/*TODO*///    	PUSHBYTE(DP());
/*TODO*///    	if (( MD() & MD_EM ) != 0)
/*TODO*///    	{
/*TODO*///    		PUSHBYTE(F());
/*TODO*///    		PUSHBYTE(E());
/*TODO*///    		hd6309_ICount[0] -= 2;
/*TODO*///    	}
/*TODO*///    	PUSHBYTE(B());
/*TODO*///    	PUSHBYTE(A());
/*TODO*///    	PUSHBYTE(CC());
/*TODO*///    	PCD( RM16(0xfff4) );
/*TODO*///    	CHANGE_PC();
/*TODO*///    }
/*TODO*///    
/*TODO*///    /*TODO*////* $113F SWI3 absolute indirect ----- */
/*TODO*///    /*TODO*///INLINE void swi3( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CC |= CC_E; 			/* HJB 980225: save entire state */
/*TODO*///    /*TODO*///	PUSHWORD(pPC);
/*TODO*///    /*TODO*///	PUSHWORD(pU);
/*TODO*///    /*TODO*///	PUSHWORD(pY);
/*TODO*///    /*TODO*///	PUSHWORD(pX);
/*TODO*///    /*TODO*///	PUSHBYTE(DP);
/*TODO*///    /*TODO*///	if ( MD & MD_EM )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		PUSHBYTE(F);
/*TODO*///    /*TODO*///		PUSHBYTE(E);
/*TODO*///    /*TODO*///		hd6309_ICount -= 2;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	PUSHBYTE(B);
/*TODO*///    /*TODO*///	PUSHBYTE(A);
/*TODO*///    /*TODO*///	PUSHBYTE(CC);
/*TODO*///    /*TODO*///	PCD = RM16(0xfff2);
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____4x____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $40 NEGA inherent ?**** */
/*TODO*///    /*TODO*///INLINE void nega( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	r = -A;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(0,A,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $41 ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $42 ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $43 COMA inherent -**01 */
/*TODO*///    /*TODO*///INLINE void coma( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	A = ~A;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $44 LSRA inherent -0*-* */
/*TODO*///    /*TODO*///INLINE void lsra( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (A & CC_C);
/*TODO*///    /*TODO*///	A >>= 1;
/*TODO*///    /*TODO*///	SET_Z8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $45 ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $46 RORA inherent -**-* */
/*TODO*///    /*TODO*///INLINE void rora( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 r;
/*TODO*///    /*TODO*///	r = (CC & CC_C) << 7;
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (A & CC_C);
/*TODO*///    /*TODO*///	r |= A >> 1;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $47 ASRA inherent ?**-* */
/*TODO*///    /*TODO*///INLINE void asra( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (A & CC_C);
/*TODO*///    /*TODO*///	A = (A & 0x80) | (A >> 1);
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $48 ASLA inherent ?**** */
/*TODO*///    /*TODO*///INLINE void asla( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	r = A << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,A,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $49 ROLA inherent -**** */
/*TODO*///    /*TODO*///INLINE void rola( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = A;
/*TODO*///    /*TODO*///	r = (CC & CC_C) | (t<<1);
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(t,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $4A DECA inherent -***- */
/*TODO*///    /*TODO*///INLINE void deca( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	--A;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8D(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $4B ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $4C INCA inherent -***- */
/*TODO*///    /*TODO*///INLINE void inca( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	++A;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8I(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $4D TSTA inherent -**0- */
/*TODO*///    /*TODO*///INLINE void tsta( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    
/*TODO*///    /* $4E ILLEGAL */
/*TODO*///    
/*TODO*///    /* $4F CLRA inherent -0100 */
/*TODO*///    public static void clra()
/*TODO*///    {
/*TODO*///    	A( 0 );
/*TODO*///    	CLR_NZVC(); SEZ();
/*TODO*///    }
/*TODO*///    
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____5x____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $50 NEGB inherent ?**** */
/*TODO*///    /*TODO*///INLINE void negb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	r = -B;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(0,B,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1040 NEGD inherent ?**** */
/*TODO*///    /*TODO*///INLINE void negd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	r = -D;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(0,D,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $51 ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $52 ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $53 COMB inherent -**01 */
/*TODO*///    /*TODO*///INLINE void comb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	B = ~B;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1143 COME inherent -**01 */
/*TODO*///    /*TODO*///INLINE void come( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	E = ~E;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1153 COMF inherent -**01 */
/*TODO*///    /*TODO*///INLINE void comf( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	F = ~F;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1043 COMD inherent -**01 */
/*TODO*///    /*TODO*///INLINE void comd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	D = ~D;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1053 COMW inherent -**01 */
/*TODO*///    /*TODO*///INLINE void comw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	W = ~W;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $54 LSRB inherent -0*-* */
/*TODO*///    /*TODO*///INLINE void lsrb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (B & CC_C);
/*TODO*///    /*TODO*///	B >>= 1;
/*TODO*///    /*TODO*///	SET_Z8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1044 LSRD inherent -0*-* */
/*TODO*///    /*TODO*///INLINE void lsrd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (B & CC_C);
/*TODO*///    /*TODO*///	D >>= 1;
/*TODO*///    /*TODO*///	SET_Z16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1054 LSRW inherent -0*-* */
/*TODO*///    /*TODO*///INLINE void lsrw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (F & CC_C);
/*TODO*///    /*TODO*///	W >>= 1;
/*TODO*///    /*TODO*///	SET_Z16(W);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $55 ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $56 RORB inherent -**-* */
/*TODO*///    /*TODO*///INLINE void rorb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 r;
/*TODO*///    /*TODO*///	r = (CC & CC_C) << 7;
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (B & CC_C);
/*TODO*///    /*TODO*///	r |= B >> 1;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1046 RORD inherent -**-* */
/*TODO*///    /*TODO*///INLINE void rord( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	r = (CC & CC_C) << 15;
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (D & CC_C);
/*TODO*///    /*TODO*///	r |= D >> 1;
/*TODO*///    /*TODO*///	SET_NZ16(r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1056 RORW inherent -**-* */
/*TODO*///    /*TODO*///INLINE void rorw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	r = (CC & CC_C) << 15;
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (W & CC_C);
/*TODO*///    /*TODO*///	r |= W >> 1;
/*TODO*///    /*TODO*///	SET_NZ16(r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $57 ASRB inherent ?**-* */
/*TODO*///    /*TODO*///INLINE void asrb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (B & CC_C);
/*TODO*///    /*TODO*///	B= (B & 0x80) | (B >> 1);
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1047 ASRD inherent ?**-* */
/*TODO*///    /*TODO*///INLINE void asrd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (D & CC_C);
/*TODO*///    /*TODO*///	D= (D & 0x8000) | (D >> 1);
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $58 ASLB inherent ?**** */
/*TODO*///    /*TODO*///INLINE void aslb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	r = B << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,B,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1048 ASLD inherent ?**** */
/*TODO*///    /*TODO*///INLINE void asld( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	r = D << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,D,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $59 ROLB inherent -**** */
/*TODO*///    /*TODO*///INLINE void rolb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = B;
/*TODO*///    /*TODO*///	r = CC & CC_C;
/*TODO*///    /*TODO*///	r |= t << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(t,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1049 ROLD inherent -**** */
/*TODO*///    /*TODO*///INLINE void rold( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 t,r;
/*TODO*///    /*TODO*///	t = D;
/*TODO*///    /*TODO*///	r = CC & CC_C;
/*TODO*///    /*TODO*///	r |= t << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(t,t,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1059 ROLW inherent -**** */
/*TODO*///    /*TODO*///INLINE void rolw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 t,r;
/*TODO*///    /*TODO*///	t = W;
/*TODO*///    /*TODO*///	r = CC & CC_C;
/*TODO*///    /*TODO*///	r |= t << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(t,t,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $5A DECB inherent -***- */
/*TODO*///    /*TODO*///INLINE void decb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	--B;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8D(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $114a DECE inherent -***- */
/*TODO*///    /*TODO*///INLINE void dece( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	--E;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8D(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $115a DECF inherent -***- */
/*TODO*///    /*TODO*///INLINE void decf( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	--F;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8D(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $104a DECD inherent -***- */
/*TODO*///    /*TODO*///INLINE void decd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	r = D - 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,D,r)
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $105a DECW inherent -***- */
/*TODO*///    /*TODO*///INLINE void decw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	r = W - 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(W,W,r)
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $5B ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $5C INCB inherent -***- */
/*TODO*///    /*TODO*///INLINE void incb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	++B;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8I(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $114c INCE inherent -***- */
/*TODO*///    /*TODO*///INLINE void ince( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	++E;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8I(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $115c INCF inherent -***- */
/*TODO*///    /*TODO*///INLINE void incf( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	++F;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_FLAGS8I(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $104c INCD inherent -***- */
/*TODO*///    /*TODO*///INLINE void incd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	r = D + 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,D,r)
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $105c INCW inherent -***- */
/*TODO*///    /*TODO*///INLINE void incw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	r = W + 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(W,W,r)
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $5D TSTB inherent -**0- */
/*TODO*///    /*TODO*///INLINE void tstb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $104d TSTD inherent -**0- */
/*TODO*///    /*TODO*///INLINE void tstd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $105d TSTW inherent -**0- */
/*TODO*///    /*TODO*///INLINE void tstw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $114d TSTE inherent -**0- */
/*TODO*///    /*TODO*///INLINE void tste( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $115d TSTF inherent -**0- */
/*TODO*///    /*TODO*///INLINE void tstf( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $5E ILLEGAL */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $5F CLRB inherent -0100 */
/*TODO*///    /*TODO*///INLINE void clrb( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	B = 0;
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $104f CLRD inherent -0100 */
/*TODO*///    /*TODO*///INLINE void clrd( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	D = 0;
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $114f CLRE inherent -0100 */
/*TODO*///    /*TODO*///INLINE void clre( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	E = 0;
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $115f CLRF inherent -0100 */
/*TODO*///    /*TODO*///INLINE void clrf( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	F = 0;
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $105f CLRW inherent -0100 */
/*TODO*///    /*TODO*///INLINE void clrw( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	W = 0;
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____6x____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $60 NEG indexed ?**** */
/*TODO*///    /*TODO*///INLINE void neg_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r,t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r=-t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(0,t,r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $61 OIM indexed */
/*TODO*///    /*TODO*///INLINE void oim_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	r = im | RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $62 AIM indexed */
/*TODO*///    /*TODO*///INLINE void aim_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	r = im & RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $63 COM indexed -**01 */
/*TODO*///    /*TODO*///INLINE void com_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = ~RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(t);
/*TODO*///    /*TODO*///	SEC;
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $64 LSR indexed -0*-* */
/*TODO*///    /*TODO*///INLINE void lsr_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (t & CC_C);
/*TODO*///    /*TODO*///	t>>=1; SET_Z8(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $65 EIM indexed */
/*TODO*///    /*TODO*///INLINE void eim_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	r = im ^ RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*////* $66 ROR indexed -**-* */
/*TODO*///    /*TODO*///INLINE void ror_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM(EAD);
/*TODO*///    /*TODO*///	r = (CC & CC_C) << 7;
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (t & CC_C);
/*TODO*///    /*TODO*///	r |= t>>1; SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $67 ASR indexed ?**-* */
/*TODO*///    /*TODO*///INLINE void asr_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZC;
/*TODO*///    /*TODO*///	CC |= (t & CC_C);
/*TODO*///    /*TODO*///	t=(t&0x80)|(t>>1);
/*TODO*///    /*TODO*///	SET_NZ8(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $68 ASL indexed ?**** */
/*TODO*///    /*TODO*///INLINE void asl_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM(EAD);
/*TODO*///    /*TODO*///	r = t << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(t,t,r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $69 ROL indexed -**** */
/*TODO*///    /*TODO*///INLINE void rol_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM(EAD);
/*TODO*///    /*TODO*///	r = CC & CC_C;
/*TODO*///    /*TODO*///	r |= t << 1;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(t,t,r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $6A DEC indexed -***- */
/*TODO*///    /*TODO*///INLINE void dec_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD) - 1;
/*TODO*///    /*TODO*///	CLR_NZV; SET_FLAGS8D(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $6B TIM indexed */
/*TODO*///    /*TODO*///INLINE void tim_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,im,m;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	m = RM(EAD);
/*TODO*///    /*TODO*///	r = im & m;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $6C INC indexed -***- */
/*TODO*///    /*TODO*///INLINE void inc_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD) + 1;
/*TODO*///    /*TODO*///	CLR_NZV; SET_FLAGS8I(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $6D TST indexed -**0- */
/*TODO*///    /*TODO*///INLINE void tst_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $6E JMP indexed ----- */
/*TODO*///    /*TODO*///INLINE void jmp_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	PCD = EAD;
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $6F CLR indexed -0100 */
/*TODO*///    /*TODO*///INLINE void clr_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	WM(EAD,0);
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____7x____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $70 NEG extended ?**** */
/*TODO*///    /*TODO*///INLINE void neg_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r,t;
/*TODO*///    /*TODO*///	EXTBYTE(t); r=-t;
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(0,t,r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $71 OIM extended */
/*TODO*///    /*TODO*///INLINE void oim_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,t,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = im | t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $72 AIM extended */
/*TODO*///    /*TODO*///INLINE void aim_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,t,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = im & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $73 COM extended -**01 */
/*TODO*///    /*TODO*///INLINE void com_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t); t = ~t;
/*TODO*///    /*TODO*///	CLR_NZV; SET_NZ8(t); SEC;
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $74 LSR extended -0*-* */
/*TODO*///    /*TODO*///INLINE void lsr_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t); CLR_NZC; CC |= (t & CC_C);
/*TODO*///    /*TODO*///	t>>=1; SET_Z8(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $75 EIM extended */
/*TODO*///    /*TODO*///INLINE void eim_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,t,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = im ^ t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $76 ROR extended -**-* */
/*TODO*///    /*TODO*///INLINE void ror_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t); r=(CC & CC_C) << 7;
/*TODO*///    /*TODO*///	CLR_NZC; CC |= (t & CC_C);
/*TODO*///    /*TODO*///	r |= t>>1; SET_NZ8(r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $77 ASR extended ?**-* */
/*TODO*///    /*TODO*///INLINE void asr_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t); CLR_NZC; CC |= (t & CC_C);
/*TODO*///    /*TODO*///	t=(t&0x80)|(t>>1);
/*TODO*///    /*TODO*///	SET_NZ8(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $78 ASL extended ?**** */
/*TODO*///    /*TODO*///INLINE void asl_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t); r=t<<1;
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(t,t,r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $79 ROL extended -**** */
/*TODO*///    /*TODO*///INLINE void rol_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t); r = (CC & CC_C) | (t << 1);
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(t,t,r);
/*TODO*///    /*TODO*///	WM(EAD,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $7A DEC extended -***- */
/*TODO*///    /*TODO*///INLINE void dec_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t); --t;
/*TODO*///    /*TODO*///	CLR_NZV; SET_FLAGS8D(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $7B TIM extended */
/*TODO*///    /*TODO*///INLINE void tim_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	r,t,im;
/*TODO*///    /*TODO*///	im = IMMBYTE(im);
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = im & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $7C INC extended -***- */
/*TODO*///    /*TODO*///INLINE void inc_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t); ++t;
/*TODO*///    /*TODO*///	CLR_NZV; SET_FLAGS8I(t);
/*TODO*///    /*TODO*///	WM(EAD,t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $7D TST extended -**0- */
/*TODO*///    /*TODO*///INLINE void tst_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t); CLR_NZV; SET_NZ8(t);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $7E JMP extended ----- */
/*TODO*///    /*TODO*///INLINE void jmp_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	PCD = EAD;
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $7F CLR extended -0100 */
/*TODO*///    /*TODO*///INLINE void clr_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM(EAD,0);
/*TODO*///    /*TODO*///	CLR_NZVC; SEZ;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____8x____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $80 SUBA immediate ?**** */
/*TODO*///    /*TODO*///INLINE void suba_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $81 CMPA immediate ?**** */
/*TODO*///    /*TODO*///INLINE void cmpa_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $82 SBCA immediate ?**** */
/*TODO*///    /*TODO*///INLINE void sbca_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = A - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $83 SUBD (CMPD CMPU) immediate -**** */
/*TODO*///    /*TODO*///INLINE void subd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1080 SUBW immediate -**** */
/*TODO*///    /*TODO*///INLINE void subw_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1083 CMPD immediate -**** */
/*TODO*///    /*TODO*///INLINE void cmpd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1081 CMPW immediate -**** */
/*TODO*///    /*TODO*///INLINE void cmpw_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1183 CMPU immediate -**** */
/*TODO*///    /*TODO*///INLINE void cmpu_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r, d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = U;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $84 ANDA immediate -**0- */
/*TODO*///    /*TODO*///INLINE void anda_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	A &= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $85 BITA immediate -**0- */
/*TODO*///    /*TODO*///INLINE void bita_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = A & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $86 LDA immediate -**0- */
/*TODO*///    /*TODO*///INLINE void lda_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	A(IMMBYTE(A));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $88 EORA immediate -**0- */
/*TODO*///    /*TODO*///INLINE void eora_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	A ^= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $89 ADCA immediate ***** */
/*TODO*///    /*TODO*///INLINE void adca_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = A + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $8A ORA immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ora_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	A |= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $8B ADDA immediate ***** */
/*TODO*///    /*TODO*///INLINE void adda_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = A + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $8C CMPX (CMPY CMPS) immediate -**** */
/*TODO*///    /*TODO*///INLINE void cmpx_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = X;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $108C CMPY immediate -**** */
/*TODO*///    /*TODO*///INLINE void cmpy_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = Y;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $118C CMPS immediate -**** */
/*TODO*///    /*TODO*///INLINE void cmps_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = S;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $8D BSR ----- */
/*TODO*///    /*TODO*///INLINE void bsr( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	PUSHWORD(pPC);
/*TODO*///    /*TODO*///	PC += SIGNED(t);
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $8E LDX (LDY) immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldx_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	pX = IMMWORD(pX);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $CD LDQ immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldq_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	IMMLONG(q);
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $108E LDY immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldy_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	pY = IMMWORD(pY);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $118f MULD immediate */
/*TODO*///    /*TODO*///INLINE void muld_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t, q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	t = IMMWORD( t );
/*TODO*///    /*TODO*///	q.d = (INT16) D * (INT16)t.w.l;
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $118d DIVD immediate */
/*TODO*///    /*TODO*///INLINE void divd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8   t;
/*TODO*///    /*TODO*///	INT16   v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	t = IMMBYTE( t );
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT16) D / (INT8) t;
/*TODO*///    /*TODO*///		A = (INT16) D % (INT8) t;
/*TODO*///    /*TODO*///		B = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ8(B);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( B & 0x01 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 127) || (v < -128) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		hd6309_ICount -= 8;
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $118e DIVQ immediate */
/*TODO*///    /*TODO*///INLINE void divq_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t,q;
/*TODO*///    /*TODO*///	INT32	v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	t = IMMWORD( t );
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t.w.l != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT32) q.d / (INT16) t.w.l;
/*TODO*///    /*TODO*///		D = (INT32) q.d % (INT16) t.w.l;
/*TODO*///    /*TODO*///		W = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ16(W);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( W & 0x0001 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 65534) || (v < -65535) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____9x____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $90 SUBA direct ?**** */
/*TODO*///    /*TODO*///INLINE void suba_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $91 CMPA direct ?**** */
/*TODO*///    /*TODO*///INLINE void cmpa_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $92 SBCA direct ?**** */
/*TODO*///    /*TODO*///INLINE void sbca_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = A - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $93 SUBD (CMPD CMPU) direct -**** */
/*TODO*///    /*TODO*///INLINE void subd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1090 SUBW direct -**** */
/*TODO*///    /*TODO*///INLINE void subw_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1093 CMPD direct -**** */
/*TODO*///    /*TODO*///INLINE void cmpd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1091 CMPW direct -**** */
/*TODO*///    /*TODO*///INLINE void cmpw_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1193 CMPU direct -**** */
/*TODO*///    /*TODO*///INLINE void cmpu_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = U;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(U,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $94 ANDA direct -**0- */
/*TODO*///    /*TODO*///INLINE void anda_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	A &= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $95 BITA direct -**0- */
/*TODO*///    /*TODO*///INLINE void bita_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = A & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $96 LDA direct -**0- */
/*TODO*///    /*TODO*///INLINE void lda_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	A(DIRBYTE(A));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $113d LDMD direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldmd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	MD(DIRBYTE(MD));
/*TODO*///    /*TODO*///	UpdateState();
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $97 STA direct -**0- */
/*TODO*///    /*TODO*///INLINE void sta_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM(EAD,A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $98 EORA direct -**0- */
/*TODO*///    /*TODO*///INLINE void eora_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	A ^= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $99 ADCA direct ***** */
/*TODO*///    /*TODO*///INLINE void adca_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = A + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $9A ORA direct -**0- */
/*TODO*///    /*TODO*///INLINE void ora_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	A |= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $9B ADDA direct ***** */
/*TODO*///    /*TODO*///INLINE void adda_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = A + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $9C CMPX (CMPY CMPS) direct -**** */
/*TODO*///    /*TODO*///INLINE void cmpx_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = X;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $109C CMPY direct -**** */
/*TODO*///    /*TODO*///INLINE void cmpy_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = Y;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $119C CMPS direct -**** */
/*TODO*///    /*TODO*///INLINE void cmps_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = S;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $9D JSR direct ----- */
/*TODO*///    /*TODO*///INLINE void jsr_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	PUSHWORD(pPC);
/*TODO*///    /*TODO*///	PCD = EAD;
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $9E LDX (LDY) direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldx_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	DIRWORD(pX);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $119f MULD direct -**0- */
/*TODO*///    /*TODO*///INLINE void muld_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t,q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	q.d = (INT16) D * (INT16)t.w.l;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $119d DIVD direct -**0- */
/*TODO*///    /*TODO*///INLINE void divd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	t;
/*TODO*///    /*TODO*///	INT16   v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT16) D / (INT8) t;
/*TODO*///    /*TODO*///		A = (INT16) D % (INT8) t;
/*TODO*///    /*TODO*///		B = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ8(B);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( B & 0x01 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 127) || (v < -128) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		hd6309_ICount -= 8;
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $119e DIVQ direct -**0- */
/*TODO*///    /*TODO*///INLINE void divq_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t, q;
/*TODO*///    /*TODO*///	INT32	v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t.w.l != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT32) q.d / (INT16) t.w.l;
/*TODO*///    /*TODO*///		D = (INT32) q.d % (INT16) t.w.l;
/*TODO*///    /*TODO*///		W = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ16(W);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( W & 0x0001 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 65534) || (v < -65535) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10dc LDQ direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldq_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	DIRLONG(q);
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $109E LDY direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldy_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	DIRWORD(pY);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $9F STX (STY) direct -**0- */
/*TODO*///    /*TODO*///INLINE void stx_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM16(EAD,&pX);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10dd STQ direct -**0- */
/*TODO*///    /*TODO*///INLINE void stq_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM32(EAD,&q);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $109F STY direct -**0- */
/*TODO*///    /*TODO*///INLINE void sty_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM16(EAD,&pY);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____Ax____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a0 SUBA indexed ?**** */
/*TODO*///    /*TODO*///INLINE void suba_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a1 CMPA indexed ?**** */
/*TODO*///    /*TODO*///INLINE void cmpa_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a2 SBCA indexed ?**** */
/*TODO*///    /*TODO*///INLINE void sbca_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = A - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a3 SUBD (CMPD CMPU) indexed -**** */
/*TODO*///    /*TODO*///INLINE void subd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a0 SUBW indexed -**** */
/*TODO*///    /*TODO*///INLINE void subw_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a3 CMPD indexed -**** */
/*TODO*///    /*TODO*///INLINE void cmpd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a1 CMPW indexed -**** */
/*TODO*///    /*TODO*///INLINE void cmpw_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11a3 CMPU indexed -**** */
/*TODO*///    /*TODO*///INLINE void cmpu_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	r = U - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(U,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a4 ANDA indexed -**0- */
/*TODO*///    /*TODO*///INLINE void anda_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	A &= RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a5 BITA indexed -**0- */
/*TODO*///    /*TODO*///INLINE void bita_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	r = A & RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a6 LDA indexed -**0- */
/*TODO*///    /*TODO*///INLINE void lda_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	A = RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a7 STA indexed -**0- */
/*TODO*///    /*TODO*///INLINE void sta_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///	WM(EAD,A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a8 EORA indexed -**0- */
/*TODO*///    /*TODO*///INLINE void eora_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	A ^= RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $a9 ADCA indexed ***** */
/*TODO*///    /*TODO*///INLINE void adca_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = A + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $aA ORA indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ora_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	A |= RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $aB ADDA indexed ***** */
/*TODO*///    /*TODO*///INLINE void adda_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = A + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $aC CMPX (CMPY CMPS) indexed -**** */
/*TODO*///    /*TODO*///INLINE void cmpx_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = X;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10aC CMPY indexed -**** */
/*TODO*///    /*TODO*///INLINE void cmpy_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = Y;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11aC CMPS indexed -**** */
/*TODO*///    /*TODO*///INLINE void cmps_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = S;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $aD JSR indexed ----- */
/*TODO*///    /*TODO*///INLINE void jsr_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	PUSHWORD(pPC);
/*TODO*///    /*TODO*///	PCD = EAD;
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $aE LDX (LDY) indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldx_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	X=RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11af MULD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void muld_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///	UINT16	t;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM16(EAD);
/*TODO*///    /*TODO*///	q.d = (INT16) D * (INT16)t;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11ad DIVD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void divd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	t;
/*TODO*///    /*TODO*///	INT16   v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM(EAD);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT16) D / (INT8) t;
/*TODO*///    /*TODO*///		A = (INT16) D % (INT8) t;
/*TODO*///    /*TODO*///		B = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ8(B);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( B & 0x01 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 127) || (v < -128) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		hd6309_ICount -= 8;
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11ae DIVQ indexed -**0- */
/*TODO*///    /*TODO*///INLINE void divq_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	t;
/*TODO*///    /*TODO*///	INT32	v;
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t=RM16(EAD);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT32) q.d / (INT16) t;
/*TODO*///    /*TODO*///		D = (INT32) q.d % (INT16) t;
/*TODO*///    /*TODO*///		W = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ16(W);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( W & 0x0001 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 65534) || (v < -65535) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10ec LDQ indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldq_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	q.d=RM32(EAD);
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10aE LDY indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldy_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	Y=RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $aF STX (STY) indexed -**0- */
/*TODO*///    /*TODO*///INLINE void stx_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///	WM16(EAD,&pX);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10ed STQ indexed -**0- */
/*TODO*///    /*TODO*///INLINE void stq_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	WM32(EAD,&q);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10aF STY indexed -**0- */
/*TODO*///    /*TODO*///INLINE void sty_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///	WM16(EAD,&pY);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____Bx____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b0 SUBA extended ?**** */
/*TODO*///    /*TODO*///INLINE void suba_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b1 CMPA extended ?**** */
/*TODO*///    /*TODO*///INLINE void cmpa_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = A - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b2 SBCA extended ?**** */
/*TODO*///    /*TODO*///INLINE void sbca_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = A - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b3 SUBD (CMPD CMPU) extended -**** */
/*TODO*///    /*TODO*///INLINE void subd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b0 SUBW extended -**** */
/*TODO*///    /*TODO*///INLINE void subw_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b3 CMPD extended -**** */
/*TODO*///    /*TODO*///INLINE void cmpd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b1 CMPW extended -**** */
/*TODO*///    /*TODO*///INLINE void cmpw_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11b3 CMPU extended -**** */
/*TODO*///    /*TODO*///INLINE void cmpu_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = U;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b4 ANDA extended -**0- */
/*TODO*///    /*TODO*///INLINE void anda_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	A &= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b5 BITA extended -**0- */
/*TODO*///    /*TODO*///INLINE void bita_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = A & t;
/*TODO*///    /*TODO*///	CLR_NZV; SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b6 LDA extended -**0- */
/*TODO*///    /*TODO*///INLINE void lda_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTBYTE(A);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b7 STA extended -**0- */
/*TODO*///    /*TODO*///INLINE void sta_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM(EAD,A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b8 EORA extended -**0- */
/*TODO*///    /*TODO*///INLINE void eora_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	A ^= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $b9 ADCA extended ***** */
/*TODO*///    /*TODO*///INLINE void adca_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = A + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $bA ORA extended -**0- */
/*TODO*///    /*TODO*///INLINE void ora_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	A |= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(A);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $bB ADDA extended ***** */
/*TODO*///    /*TODO*///INLINE void adda_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = A + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(A,t,r);
/*TODO*///    /*TODO*///	SET_H(A,t,r);
/*TODO*///    /*TODO*///	A = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $bC CMPX (CMPY CMPS) extended -**** */
/*TODO*///    /*TODO*///INLINE void cmpx_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = X;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10bC CMPY extended -**** */
/*TODO*///    /*TODO*///INLINE void cmpy_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = Y;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11bC CMPS extended -**** */
/*TODO*///    /*TODO*///INLINE void cmps_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = S;
/*TODO*///    /*TODO*///	r = d - b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $bD JSR extended ----- */
/*TODO*///    /*TODO*///INLINE void jsr_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	PUSHWORD(pPC);
/*TODO*///    /*TODO*///	PCD = EAD;
/*TODO*///    /*TODO*///	CHANGE_PC;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $bE LDX (LDY) extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldx_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTWORD(pX);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11bf MULD extended -**0- */
/*TODO*///    /*TODO*///INLINE void muld_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t, q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///	q.d = (INT16) D * (INT16)t.w.l;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11bd DIVD extended -**0- */
/*TODO*///    /*TODO*///INLINE void divd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8	t;
/*TODO*///    /*TODO*///	INT16   v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT16) D / (INT8) t;
/*TODO*///    /*TODO*///		A = (INT16) D % (INT8) t;
/*TODO*///    /*TODO*///		B = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ8(B);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( B & 0x01 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 127) || (v < -128) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		hd6309_ICount -= 8;
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11be DIVQ extended -**0- */
/*TODO*///    /*TODO*///INLINE void divq_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t, q;
/*TODO*///    /*TODO*///	INT32	v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	if( t.w.l != 0 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		v = (INT32) q.d / (INT16) t.w.l;
/*TODO*///    /*TODO*///		D = (INT32) q.d % (INT16) t.w.l;
/*TODO*///    /*TODO*///		W = v;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		CLR_NZVC;
/*TODO*///    /*TODO*///		SET_NZ16(W);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if( W & 0x0001 )
/*TODO*///    /*TODO*///			SEC;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		if ( (v > 65534) || (v < -65535) )
/*TODO*///    /*TODO*///			SEV;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///	else
/*TODO*///    /*TODO*///		DZError();
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10fc LDQ extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldq_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	EXTLONG(q);
/*TODO*///    /*TODO*///	D = q.w.h;
/*TODO*///    /*TODO*///	W = q.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10bE LDY extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldy_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTWORD(pY);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $bF STX (STY) extended -**0- */
/*TODO*///    /*TODO*///INLINE void stx_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(X);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM16(EAD,&pX);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10fd STQ extended -**0- */
/*TODO*///    /*TODO*///INLINE void stq_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	q;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	q.w.h = D;
/*TODO*///    /*TODO*///	q.w.l = W;
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM32(EAD,&q);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_N8(A);
/*TODO*///    /*TODO*///	SET_Z(q.d);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10bF STY extended -**0- */
/*TODO*///    /*TODO*///INLINE void sty_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(Y);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM16(EAD,&pY);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____Cx____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c0 SUBB immediate ?**** */
/*TODO*///    /*TODO*///INLINE void subb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1180 SUBE immediate ?**** */
/*TODO*///    /*TODO*///INLINE void sube_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11C0 SUBF immediate ?**** */
/*TODO*///    /*TODO*///INLINE void subf_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c1 CMPB immediate ?**** */
/*TODO*///    /*TODO*///INLINE void cmpb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1181 CMPE immediate ?**** */
/*TODO*///    /*TODO*///INLINE void cmpe_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11C1 CMPF immediate ?**** */
/*TODO*///    /*TODO*///INLINE void cmpf_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC; SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c2 SBCB immediate ?**** */
/*TODO*///    /*TODO*///INLINE void sbcb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = B - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1082 SBCD immediate ?**** */
/*TODO*///    /*TODO*///INLINE void sbcd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///	UINT32	 r;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	r = D - t.w.l - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t.w.l,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c3 ADDD immediate -**** */
/*TODO*///    /*TODO*///INLINE void addd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $108b ADDW immediate -**** */
/*TODO*///    /*TODO*///INLINE void addw_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	b = IMMWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $118b ADDE immediate -**** */
/*TODO*///    /*TODO*///INLINE void adde_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = E + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	SET_H(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11Cb ADDF immediate -**** */
/*TODO*///    /*TODO*///INLINE void addf_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = F + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	SET_H(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c4 ANDB immediate -**0- */
/*TODO*///    /*TODO*///INLINE void andb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	B &= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1084 ANDD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void andd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	D &= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c5 BITB immediate -**0- */
/*TODO*///    /*TODO*///INLINE void bitb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = B & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1085 BITD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void bitd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///	UINT16	r;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	r = B & t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $113c BITMD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void bitmd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = MD & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	CLDZ;
/*TODO*///    /*TODO*///	CLII;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c6 LDB immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	B(IMMBYTE(B));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $113d LDMD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldmd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	MD(IMMBYTE(MD));
/*TODO*///    /*TODO*////*	CLR_NZV;	*/
/*TODO*///    /*TODO*////*	SET_NZ8(B); */
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1186 LDE immediate -**0- */
/*TODO*///    /*TODO*///INLINE void lde_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	E(IMMBYTE(E));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11C6 LDF immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldf_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	F(IMMBYTE(F));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c8 EORB immediate -**0- */
/*TODO*///    /*TODO*///INLINE void eorb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	B ^= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1088 EORD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void eord_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	D ^= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $c9 ADCB immediate ***** */
/*TODO*///    /*TODO*///INLINE void adcb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = B + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1089 ADCD immediate ***** */
/*TODO*///    /*TODO*///INLINE void adcd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///	UINT32	r;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	r = D + t.w.l + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t.w.l,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $cA ORB immediate -**0- */
/*TODO*///    /*TODO*///INLINE void orb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	B |= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $108a ORD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ord_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	D |= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $cB ADDB immediate ***** */
/*TODO*///    /*TODO*///INLINE void addb_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = IMMBYTE(t);
/*TODO*///    /*TODO*///	r = B + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $cC LDD immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldd_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	D=t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1086 LDW immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldw_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///	t = IMMWORD(t);
/*TODO*///    /*TODO*///	W=t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $cE LDU (LDS) immediate -**0- */
/*TODO*///    /*TODO*///INLINE void ldu_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	pU = IMMWORD(pU);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10cE LDS immediate -**0- */
/*TODO*///    /*TODO*///INLINE void lds_im( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	pS = IMMWORD(pS);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	hd6309.int_state |= HD6309_LDS;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____Dx____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d0 SUBB direct ?**** */
/*TODO*///    /*TODO*///INLINE void subb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1190 SUBE direct ?**** */
/*TODO*///    /*TODO*///INLINE void sube_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11d0 SUBF direct ?**** */
/*TODO*///    /*TODO*///INLINE void subf_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d1 CMPB direct ?**** */
/*TODO*///    /*TODO*///INLINE void cmpb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1191 CMPE direct ?**** */
/*TODO*///    /*TODO*///INLINE void cmpe_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11D1 CMPF direct ?**** */
/*TODO*///    /*TODO*///INLINE void cmpf_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d2 SBCB direct ?**** */
/*TODO*///    /*TODO*///INLINE void sbcb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = B - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1092 SBCD direct ?**** */
/*TODO*///    /*TODO*///INLINE void sbcd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///	UINT32	r;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	r = D - t.w.l - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t.w.l,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d3 ADDD direct -**** */
/*TODO*///    /*TODO*///INLINE void addd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $109b ADDW direct -**** */
/*TODO*///    /*TODO*///INLINE void addw_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	DIRWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $119b ADDE direct -**** */
/*TODO*///    /*TODO*///INLINE void adde_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = E + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	SET_H(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11db ADDF direct -**** */
/*TODO*///    /*TODO*///INLINE void addf_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = F + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	SET_H(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d4 ANDB direct -**0- */
/*TODO*///    /*TODO*///INLINE void andb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	B &= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1094 ANDD direct -**0- */
/*TODO*///    /*TODO*///INLINE void andd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	D &= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d5 BITB direct -**0- */
/*TODO*///    /*TODO*///INLINE void bitb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = B & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1095 BITD direct -**0- */
/*TODO*///    /*TODO*///INLINE void bitd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR	t;
/*TODO*///    /*TODO*///	UINT16	r;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	r = B & t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d6 LDB direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	B(DIRBYTE(B));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1196 LDE direct -**0- */
/*TODO*///    /*TODO*///INLINE void lde_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	E(DIRBYTE(E));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11d6 LDF direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldf_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	F(DIRBYTE(F));
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d7 STB direct -**0- */
/*TODO*///    /*TODO*///INLINE void stb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM(EAD,B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1197 STE direct -**0- */
/*TODO*///    /*TODO*///INLINE void ste_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM(EAD,E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11D7 STF direct -**0- */
/*TODO*///    /*TODO*///INLINE void stf_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM(EAD,F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d8 EORB direct -**0- */
/*TODO*///    /*TODO*///INLINE void eorb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	B ^= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1098 EORD direct -**0- */
/*TODO*///    /*TODO*///INLINE void eord_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	D ^= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $d9 ADCB direct ***** */
/*TODO*///    /*TODO*///INLINE void adcb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = B + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1099 adcd direct ***** */
/*TODO*///    /*TODO*///INLINE void adcd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = D + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $dA ORB direct -**0- */
/*TODO*///    /*TODO*///INLINE void orb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	B |= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $109a ORD direct -**0- */
/*TODO*///    /*TODO*///INLINE void ord_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	D |= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $dB ADDB direct ***** */
/*TODO*///    /*TODO*///INLINE void addb_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	t = DIRBYTE(t);
/*TODO*///    /*TODO*///	r = B + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $dC LDD direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldd_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	D=t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1096 LDW direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldw_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t;
/*TODO*///    /*TODO*///	DIRWORD(t);
/*TODO*///    /*TODO*///	W=t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $dD STD direct -**0- */
/*TODO*///    /*TODO*///INLINE void std_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM16(EAD,&pD);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $1097 STW direct -**0- */
/*TODO*///    /*TODO*///INLINE void stw_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM16(EAD,&pW);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $dE LDU (LDS) direct -**0- */
/*TODO*///    /*TODO*///INLINE void ldu_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	DIRWORD(pU);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10dE LDS direct -**0- */
/*TODO*///    /*TODO*///INLINE void lds_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	DIRWORD(pS);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	hd6309.int_state |= HD6309_LDS;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $dF STU (STS) direct -**0- */
/*TODO*///    /*TODO*///INLINE void stu_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM16(EAD,&pU);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10dF STS direct -**0- */
/*TODO*///    /*TODO*///INLINE void sts_di( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	DIRECT;
/*TODO*///    /*TODO*///	WM16(EAD,&pS);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____Ex____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e0 SUBB indexed ?**** */
/*TODO*///    /*TODO*///INLINE void subb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11a0 SUBE indexed ?**** */
/*TODO*///    /*TODO*///INLINE void sube_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11e0 SUBF indexed ?**** */
/*TODO*///    /*TODO*///INLINE void subf_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e1 CMPB indexed ?**** */
/*TODO*///    /*TODO*///INLINE void cmpb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11a1 CMPE indexed ?**** */
/*TODO*///    /*TODO*///INLINE void cmpe_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11e1 CMPF indexed ?**** */
/*TODO*///    /*TODO*///INLINE void cmpf_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e2 SBCB indexed ?**** */
/*TODO*///    /*TODO*///INLINE void sbcb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = B - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a2 SBCD indexed ?**** */
/*TODO*///    /*TODO*///INLINE void sbcd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32	  t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM16(EAD);
/*TODO*///    /*TODO*///	r = D - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e3 ADDD indexed -**** */
/*TODO*///    /*TODO*///INLINE void addd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10ab ADDW indexed -**** */
/*TODO*///    /*TODO*///INLINE void addw_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	b.d=RM16(EAD);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11ab ADDE indexed -**** */
/*TODO*///    /*TODO*///INLINE void adde_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = E + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	SET_H(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11eb ADDF indexed -**** */
/*TODO*///    /*TODO*///INLINE void addf_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = F + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	SET_H(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e4 ANDB indexed -**0- */
/*TODO*///    /*TODO*///INLINE void andb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	B &= RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a4 ANDD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void andd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	D &= RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e5 BITB indexed -**0- */
/*TODO*///    /*TODO*///INLINE void bitb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	r = B & RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a5 BITD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void bitd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	r = D & RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e6 LDB indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	B = RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11a6 LDE indexed -**0- */
/*TODO*///    /*TODO*///INLINE void lde_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	E = RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11e6 LDF indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldf_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	F = RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e7 STB indexed -**0- */
/*TODO*///    /*TODO*///INLINE void stb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///	WM(EAD,B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11a7 STE indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ste_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///	WM(EAD,E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11e7 STF indexed -**0- */
/*TODO*///    /*TODO*///INLINE void stf_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///	WM(EAD,F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e8 EORB indexed -**0- */
/*TODO*///    /*TODO*///INLINE void eorb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	B ^= RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a8 EORD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void eord_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	D ^= RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $e9 ADCB indexed ***** */
/*TODO*///    /*TODO*///INLINE void adcb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = B + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a9 ADCD indexed ***** */
/*TODO*///    /*TODO*///INLINE void adcd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = D + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $eA ORB indexed -**0- */
/*TODO*///    /*TODO*///INLINE void orb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	B |= RM(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10aa ORD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ord_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	D |= RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $eB ADDB indexed ***** */
/*TODO*///    /*TODO*///INLINE void addb_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	t = RM(EAD);
/*TODO*///    /*TODO*///	r = B + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $eC LDD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldd_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	D=RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV; SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a6 LDW indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldw_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	W=RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV; SET_NZ16(W);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $eD STD indexed -**0- */
/*TODO*///    /*TODO*///INLINE void std_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///	WM16(EAD,&pD);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10a7 STW indexed -**0- */
/*TODO*///    /*TODO*///INLINE void stw_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///	WM16(EAD,&pW);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $eE LDU (LDS) indexed -**0- */
/*TODO*///    /*TODO*///INLINE void ldu_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	U=RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10eE LDS indexed -**0- */
/*TODO*///    /*TODO*///INLINE void lds_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	S=RM16(EAD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	hd6309.int_state |= HD6309_LDS;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $eF STU (STS) indexed -**0- */
/*TODO*///    /*TODO*///INLINE void stu_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///	WM16(EAD,&pU);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10eF STS indexed -**0- */
/*TODO*///    /*TODO*///INLINE void sts_ix( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	fetch_effective_address();
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	WM16(EAD,&pS);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef macintosh
/*TODO*///    /*TODO*///#pragma mark ____Fx____
/*TODO*///    /*TODO*///#endif
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f0 SUBB extended ?**** */
/*TODO*///    /*TODO*///INLINE void subb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11b0 SUBE extended ?**** */
/*TODO*///    /*TODO*///INLINE void sube_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11f0 SUBF extended ?**** */
/*TODO*///    /*TODO*///INLINE void subf_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f1 CMPB extended ?**** */
/*TODO*///    /*TODO*///INLINE void cmpb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = B - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11b1 CMPE extended ?**** */
/*TODO*///    /*TODO*///INLINE void cmpe_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = E - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11f1 CMPF extended ?**** */
/*TODO*///    /*TODO*///INLINE void cmpf_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = F - t;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f2 SBCB extended ?**** */
/*TODO*///    /*TODO*///INLINE void sbcb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16	  t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = B - t - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b2 SBCD extended ?**** */
/*TODO*///    /*TODO*///INLINE void sbcd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t = {{0,}};
/*TODO*///    /*TODO*///	UINT32 r;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///	r = D - t.w.l - (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t.w.l,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f3 ADDD extended -**** */
/*TODO*///    /*TODO*///INLINE void addd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = D;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10bb ADDW extended -**** */
/*TODO*///    /*TODO*///INLINE void addw_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 r,d;
/*TODO*///    /*TODO*///	PAIR b = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(b);
/*TODO*///    /*TODO*///	d = W;
/*TODO*///    /*TODO*///	r = d + b.d;
/*TODO*///    /*TODO*///	CLR_NZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(d,b.d,r);
/*TODO*///    /*TODO*///	W = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11bb ADDE extended -**** */
/*TODO*///    /*TODO*///INLINE void adde_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = E + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(E,t,r);
/*TODO*///    /*TODO*///	SET_H(E,t,r);
/*TODO*///    /*TODO*///	E = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11fb ADDF extended -**** */
/*TODO*///    /*TODO*///INLINE void addf_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = F + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(F,t,r);
/*TODO*///    /*TODO*///	SET_H(F,t,r);
/*TODO*///    /*TODO*///	F = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f4 ANDB extended -**0- */
/*TODO*///    /*TODO*///INLINE void andb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	B &= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b4 ANDD extended -**0- */
/*TODO*///    /*TODO*///INLINE void andd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///	D &= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f5 BITB extended -**0- */
/*TODO*///    /*TODO*///INLINE void bitb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = B & t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b5 BITD extended -**0- */
/*TODO*///    /*TODO*///INLINE void bitd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t = {{0,}};
/*TODO*///    /*TODO*///	UINT8 r;
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///	r = B & t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(r);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f6 LDB extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTBYTE(B);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11b6 LDE extended -**0- */
/*TODO*///    /*TODO*///INLINE void lde_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTBYTE(E);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11f6 LDF extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldf_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTBYTE(F);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f7 STB extended -**0- */
/*TODO*///    /*TODO*///INLINE void stb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM(EAD,B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11b7 STE extended -**0- */
/*TODO*///    /*TODO*///INLINE void ste_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(E);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM(EAD,E);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11f7 STF extended -**0- */
/*TODO*///    /*TODO*///INLINE void stf_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(F);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM(EAD,F);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f8 EORB extended -**0- */
/*TODO*///    /*TODO*///INLINE void eorb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	B ^= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b8 EORD extended -**0- */
/*TODO*///    /*TODO*///INLINE void eord_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///	D ^= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $f9 ADCB extended ***** */
/*TODO*///    /*TODO*///INLINE void adcb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = B + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b9 ADCD extended ***** */
/*TODO*///    /*TODO*///INLINE void adcd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT32 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = D + t + (CC & CC_C);
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS16(D,t,r);
/*TODO*///    /*TODO*///	D = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $fA ORB extended -**0- */
/*TODO*///    /*TODO*///INLINE void orb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 t;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	B |= t;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(B);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10ba ORD extended -**0- */
/*TODO*///    /*TODO*///INLINE void ord_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	PAIR t = {{0,}};
/*TODO*///    /*TODO*///	EXTWORD(t);
/*TODO*///    /*TODO*///	D |= t.w.l;
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ8(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $fB ADDB extended ***** */
/*TODO*///    /*TODO*///INLINE void addb_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT16 t,r;
/*TODO*///    /*TODO*///	EXTBYTE(t);
/*TODO*///    /*TODO*///	r = B + t;
/*TODO*///    /*TODO*///	CLR_HNZVC;
/*TODO*///    /*TODO*///	SET_FLAGS8(B,t,r);
/*TODO*///    /*TODO*///	SET_H(B,t,r);
/*TODO*///    /*TODO*///	B = r;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $fC LDD extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldd_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTWORD(pD);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b6 LDW extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldw_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTWORD(pW);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $fD STD extended -**0- */
/*TODO*///    /*TODO*///INLINE void std_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(D);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM16(EAD,&pD);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10b7 STW extended -**0- */
/*TODO*///    /*TODO*///INLINE void stw_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(W);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM16(EAD,&pW);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $fE LDU (LDS) extended -**0- */
/*TODO*///    /*TODO*///INLINE void ldu_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTWORD(pU);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10fE LDS extended -**0- */
/*TODO*///    /*TODO*///INLINE void lds_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	EXTWORD(pS);
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	hd6309.int_state |= HD6309_LDS;
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $fF STU (STS) extended -**0- */
/*TODO*///    /*TODO*///INLINE void stu_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(U);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM16(EAD,&pU);
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $10fF STS extended -**0- */
/*TODO*///    /*TODO*///INLINE void sts_ex( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	CLR_NZV;
/*TODO*///    /*TODO*///	SET_NZ16(S);
/*TODO*///    /*TODO*///	EXTENDED;
/*TODO*///    /*TODO*///	WM16(EAD,&pS);
/*TODO*///    /*TODO*///}
/*TODO*///    
/*TODO*///    /* $10xx opcodes */
/*TODO*///    /*TODO*///public static void pref10()
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	int ireg2 = ROP(PCD());
/*TODO*///    /*TODO*///	PC(PC()+1);
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef BIG_SWITCH
/*TODO*///    /*TODO*///	switch( ireg2 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		case 0x21: lbrn();			break;
/*TODO*///    /*TODO*///		case 0x22: lbhi();			break;
/*TODO*///    /*TODO*///		case 0x23: lbls();			break;
/*TODO*///    /*TODO*///		case 0x24: lbcc();			break;
/*TODO*///    /*TODO*///		case 0x25: lbcs();			break;
/*TODO*///    /*TODO*///		case 0x26: lbne();			break;
/*TODO*///    /*TODO*///		case 0x27: lbeq();			break;
/*TODO*///    /*TODO*///		case 0x28: lbvc();			break;
/*TODO*///    /*TODO*///		case 0x29: lbvs();			break;
/*TODO*///    /*TODO*///		case 0x2a: lbpl();			break;
/*TODO*///    /*TODO*///		case 0x2b: lbmi();			break;
/*TODO*///    /*TODO*///		case 0x2c: lbge();			break;
/*TODO*///    /*TODO*///		case 0x2d: lblt();			break;
/*TODO*///    /*TODO*///		case 0x2e: lbgt();			break;
/*TODO*///    /*TODO*///		case 0x2f: lble();			break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x30: addr_r();		break;
/*TODO*///    /*TODO*///		case 0x31: adcr();			break;
/*TODO*///    /*TODO*///		case 0x32: subr();			break;
/*TODO*///    /*TODO*///		case 0x33: sbcr();			break;
/*TODO*///    /*TODO*///		case 0x34: andr();			break;
/*TODO*///    /*TODO*///		case 0x35: orr();			break;
/*TODO*///    /*TODO*///		case 0x36: eorr();			break;
/*TODO*///    /*TODO*///		case 0x37: cmpr();			break;
/*TODO*///    /*TODO*///		case 0x38: pshsw(); 		break;
/*TODO*///    /*TODO*///		case 0x39: pulsw(); 		break;
/*TODO*///    /*TODO*///		case 0x3a: pshuw(); 		break;
/*TODO*///    /*TODO*///		case 0x3b: puluw(); 		break;
/*TODO*///    /*TODO*///		case 0x3f: swi2();		    break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x40: negd();			break;
/*TODO*///    /*TODO*///		case 0x43: comd();			break;
/*TODO*///    /*TODO*///		case 0x44: lsrd();			break;
/*TODO*///    /*TODO*///		case 0x46: rord();			break;
/*TODO*///    /*TODO*///		case 0x47: asrd();			break;
/*TODO*///    /*TODO*///		case 0x48: asld();			break;
/*TODO*///    /*TODO*///		case 0x49: rold();			break;
/*TODO*///    /*TODO*///		case 0x4a: decd();			break;
/*TODO*///    /*TODO*///		case 0x4c: incd();			break;
/*TODO*///    /*TODO*///		case 0x4d: tstd();			break;
/*TODO*///    /*TODO*///		case 0x4f: clrd();			break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x53: comw();			break;
/*TODO*///    /*TODO*///		case 0x54: lsrw();			break;
/*TODO*///    /*TODO*///		case 0x56: rorw();			break;
/*TODO*///    /*TODO*///		case 0x59: rolw();			break;
/*TODO*///    /*TODO*///		case 0x5a: decw();			break;
/*TODO*///    /*TODO*///		case 0x5c: incw();			break;
/*TODO*///    /*TODO*///		case 0x5d: tstw();			break;
/*TODO*///    /*TODO*///		case 0x5f: clrw();			break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x80: subw_im();		break;
/*TODO*///    /*TODO*///		case 0x81: cmpw_im();		break;
/*TODO*///    /*TODO*///		case 0x82: sbcd_im();		break;
/*TODO*///    /*TODO*///		case 0x83: cmpd_im();		break;
/*TODO*///    /*TODO*///		case 0x84: andd_im();		break;
/*TODO*///    /*TODO*///		case 0x85: bitd_im();		break;
/*TODO*///    /*TODO*///		case 0x86: ldw_im();		break;
/*TODO*///    /*TODO*///		case 0x88: eord_im();		break;
/*TODO*///    /*TODO*///		case 0x89: adcd_im();		break;
/*TODO*///    /*TODO*///		case 0x8a: ord_im();		break;
/*TODO*///    /*TODO*///		case 0x8b: addw_im();		break;
/*TODO*///    /*TODO*///		case 0x8c: cmpy_im();		break;
/*TODO*///    /*TODO*///		case 0x8e: ldy_im();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x90: subw_di();		break;
/*TODO*///    /*TODO*///		case 0x91: cmpw_di();		break;
/*TODO*///    /*TODO*///		case 0x92: sbcd_di();		break;
/*TODO*///    /*TODO*///		case 0x93: cmpd_di();		break;
/*TODO*///    /*TODO*///		case 0x94: andd_di();		break;
/*TODO*///    /*TODO*///		case 0x95: bitd_di();		break;
/*TODO*///    /*TODO*///		case 0x96: ldw_di();		break;
/*TODO*///    /*TODO*///		case 0x97: stw_di();		break;
/*TODO*///    /*TODO*///		case 0x98: eord_di();		break;
/*TODO*///    /*TODO*///		case 0x99: adcd_di();		break;
/*TODO*///    /*TODO*///		case 0x9a: ord_di();		break;
/*TODO*///    /*TODO*///		case 0x9b: addw_di();		break;
/*TODO*///    /*TODO*///		case 0x9c: cmpy_di();		break;
/*TODO*///    /*TODO*///		case 0x9e: ldy_di();		break;
/*TODO*///    /*TODO*///		case 0x9f: sty_di();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xa0: subw_ix();		break;
/*TODO*///    /*TODO*///		case 0xa1: cmpw_ix();		break;
/*TODO*///    /*TODO*///		case 0xa2: sbcd_ix();		break;
/*TODO*///    /*TODO*///		case 0xa3: cmpd_ix();		break;
/*TODO*///    /*TODO*///		case 0xa4: andd_ix();		break;
/*TODO*///    /*TODO*///		case 0xa5: bitd_ix();		break;
/*TODO*///    /*TODO*///		case 0xa6: ldw_ix();		break;
/*TODO*///    /*TODO*///		case 0xa7: stw_ix();		break;
/*TODO*///    /*TODO*///		case 0xa8: eord_ix();		break;
/*TODO*///    /*TODO*///		case 0xa9: adcd_ix();		break;
/*TODO*///    /*TODO*///		case 0xaa: ord_ix();		break;
/*TODO*///    /*TODO*///		case 0xab: addw_ix();		break;
/*TODO*///    /*TODO*///		case 0xac: cmpy_ix();		break;
/*TODO*///    /*TODO*///		case 0xae: ldy_ix();		break;
/*TODO*///    /*TODO*///		case 0xaf: sty_ix();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xb0: subw_ex();		break;
/*TODO*///    /*TODO*///		case 0xb1: cmpw_ex();		break;
/*TODO*///    /*TODO*///		case 0xb2: sbcd_ex();		break;
/*TODO*///    /*TODO*///		case 0xb3: cmpd_ex();		break;
/*TODO*///    /*TODO*///		case 0xb4: andd_ex();		break;
/*TODO*///    /*TODO*///		case 0xb5: bitd_ex();		break;
/*TODO*///    /*TODO*///		case 0xb6: ldw_ex();		break;
/*TODO*///    /*TODO*///		case 0xb7: stw_ex();		break;
/*TODO*///    /*TODO*///		case 0xb8: eord_ex();		break;
/*TODO*///    /*TODO*///		case 0xb9: adcd_ex();		break;
/*TODO*///    /*TODO*///		case 0xba: ord_ex();		break;
/*TODO*///    /*TODO*///		case 0xbb: addw_ex();		break;
/*TODO*///    /*TODO*///		case 0xbc: cmpy_ex();		break;
/*TODO*///    /*TODO*///		case 0xbe: ldy_ex();		break;
/*TODO*///    /*TODO*///		case 0xbf: sty_ex();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xce: lds_im();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xdc: ldq_di();		break;
/*TODO*///    /*TODO*///		case 0xdd: stq_di();		break;
/*TODO*///    /*TODO*///		case 0xde: lds_di();		break;
/*TODO*///    /*TODO*///		case 0xdf: sts_di();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xec: ldq_ix();		break;
/*TODO*///    /*TODO*///		case 0xed: stq_ix();		break;
/*TODO*///    /*TODO*///		case 0xee: lds_ix();		break;
/*TODO*///    /*TODO*///		case 0xef: sts_ix();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xfc: ldq_ex();		break;
/*TODO*///    /*TODO*///		case 0xfd: stq_ex();		break;
/*TODO*///    /*TODO*///		case 0xfe: lds_ex();		break;
/*TODO*///    /*TODO*///		case 0xff: sts_ex();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		default:  IIError();        break;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///#else
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	(*hd6309_page01[ireg2])();
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#endif /* BIG_SWITCH */
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	hd6309_ICount -= cycle_counts_page01[ireg2];
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*////* $11xx opcodes */
/*TODO*///    /*TODO*///INLINE void pref11( void )
/*TODO*///    /*TODO*///{
/*TODO*///    /*TODO*///	UINT8 ireg2 = ROP(PCD);
/*TODO*///    /*TODO*///	PC++;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#ifdef BIG_SWITCH
/*TODO*///    /*TODO*///	switch( ireg2 )
/*TODO*///    /*TODO*///	{
/*TODO*///    /*TODO*///		case 0x30: band();			break;
/*TODO*///    /*TODO*///		case 0x31: biand(); 		break;
/*TODO*///    /*TODO*///		case 0x32: bor();			break;
/*TODO*///    /*TODO*///		case 0x33: bior();			break;
/*TODO*///    /*TODO*///		case 0x34: beor();			break;
/*TODO*///    /*TODO*///		case 0x35: bieor(); 		break;
/*TODO*///    /*TODO*///		case 0x36: ldbt();			break;
/*TODO*///    /*TODO*///		case 0x37: stbt();			break;
/*TODO*///    /*TODO*///		case 0x38: tfmpp(); 		break;	/* Timing for TFM is actually 6+3n.        */
/*TODO*///    /*TODO*///		case 0x39: tfmmm(); 		break;	/* To avoid saving the state, I decided    */
/*TODO*///    /*TODO*///		case 0x3a: tfmpc(); 		break;	/* to push the initial 6 cycles to the end */
/*TODO*///    /*TODO*///		case 0x3b: tfmcp(); 		break;  /* We will soon see how this fairs!        */
/*TODO*///    /*TODO*///		case 0x3c: bitmd_im();		break;
/*TODO*///    /*TODO*///		case 0x3d: ldmd_im();		break;
/*TODO*///    /*TODO*///		case 0x3f: swi3();			break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x43: come();			break;
/*TODO*///    /*TODO*///		case 0x4a: dece();			break;
/*TODO*///    /*TODO*///		case 0x4c: ince();			break;
/*TODO*///    /*TODO*///		case 0x4d: tste();			break;
/*TODO*///    /*TODO*///		case 0x4f: clre();			break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x53: comf();			break;
/*TODO*///    /*TODO*///		case 0x5a: decf();			break;
/*TODO*///    /*TODO*///		case 0x5c: incf();			break;
/*TODO*///    /*TODO*///		case 0x5d: tstf();			break;
/*TODO*///    /*TODO*///		case 0x5f: clrf();			break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x80: sube_im();		break;
/*TODO*///    /*TODO*///		case 0x81: cmpe_im();		break;
/*TODO*///    /*TODO*///		case 0x83: cmpu_im();		break;
/*TODO*///    /*TODO*///		case 0x86: lde_im();		break;
/*TODO*///    /*TODO*///		case 0x8b: adde_im();		break;
/*TODO*///    /*TODO*///		case 0x8c: cmps_im();		break;
/*TODO*///    /*TODO*///		case 0x8d: divd_im();		break;
/*TODO*///    /*TODO*///		case 0x8e: divq_im();		break;
/*TODO*///    /*TODO*///		case 0x8f: muld_im();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0x90: sube_di();		break;
/*TODO*///    /*TODO*///		case 0x91: cmpe_di();		break;
/*TODO*///    /*TODO*///		case 0x93: cmpu_di();		break;
/*TODO*///    /*TODO*///		case 0x96: lde_di();		break;
/*TODO*///    /*TODO*///		case 0x97: ste_di();		break;
/*TODO*///    /*TODO*///		case 0x9b: adde_di();		break;
/*TODO*///    /*TODO*///		case 0x9c: cmps_di();		break;
/*TODO*///    /*TODO*///		case 0x9d: divd_di();		break;
/*TODO*///    /*TODO*///		case 0x9e: divq_di();		break;
/*TODO*///    /*TODO*///		case 0x9f: muld_di();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xa0: sube_ix();		break;
/*TODO*///    /*TODO*///		case 0xa1: cmpe_ix();		break;
/*TODO*///    /*TODO*///		case 0xa3: cmpu_ix();		break;
/*TODO*///    /*TODO*///		case 0xa6: lde_ix();		break;
/*TODO*///    /*TODO*///		case 0xa7: ste_ix();		break;
/*TODO*///    /*TODO*///		case 0xab: adde_ix();		break;
/*TODO*///    /*TODO*///		case 0xac: cmps_ix();		break;
/*TODO*///    /*TODO*///		case 0xad: divd_ix();		break;
/*TODO*///    /*TODO*///		case 0xae: divq_ix();		break;
/*TODO*///    /*TODO*///		case 0xaf: muld_ix();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xb0: sube_ex();		break;
/*TODO*///    /*TODO*///		case 0xb1: cmpe_ex();		break;
/*TODO*///    /*TODO*///		case 0xb3: cmpu_ex();		break;
/*TODO*///    /*TODO*///		case 0xb6: lde_ex();		break;
/*TODO*///    /*TODO*///		case 0xb7: ste_ex();		break;
/*TODO*///    /*TODO*///		case 0xbb: adde_ex();		break;
/*TODO*///    /*TODO*///		case 0xbc: cmps_ex();		break;
/*TODO*///    /*TODO*///		case 0xbd: divd_ex();		break;
/*TODO*///    /*TODO*///		case 0xbe: divq_ex();		break;
/*TODO*///    /*TODO*///		case 0xbf: muld_ex();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xc0: subf_im();		break;
/*TODO*///    /*TODO*///		case 0xc1: cmpf_im();		break;
/*TODO*///    /*TODO*///		case 0xc6: ldf_im();		break;
/*TODO*///    /*TODO*///		case 0xcb: addf_im();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xd0: subf_di();		break;
/*TODO*///    /*TODO*///		case 0xd1: cmpf_di();		break;
/*TODO*///    /*TODO*///		case 0xd6: ldf_di();		break;
/*TODO*///    /*TODO*///		case 0xd7: stf_di();		break;
/*TODO*///    /*TODO*///		case 0xdb: addf_di();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xe0: subf_ix();		break;
/*TODO*///    /*TODO*///		case 0xe1: cmpf_ix();		break;
/*TODO*///    /*TODO*///		case 0xe6: ldf_ix();		break;
/*TODO*///    /*TODO*///		case 0xe7: stf_ix();		break;
/*TODO*///    /*TODO*///		case 0xeb: addf_ix();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		case 0xf0: subf_ex();		break;
/*TODO*///    /*TODO*///		case 0xf1: cmpf_ex();		break;
/*TODO*///    /*TODO*///		case 0xf6: ldf_ex();		break;
/*TODO*///    /*TODO*///		case 0xf7: stf_ex();		break;
/*TODO*///    /*TODO*///		case 0xfb: addf_ex();		break;
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///		default:   IIError();		break;
/*TODO*///    /*TODO*///	}
/*TODO*///    /*TODO*///#else
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///	(*hd6309_page11[ireg2])();
/*TODO*///    /*TODO*///
/*TODO*///    /*TODO*///#endif /* BIG_SWITCH */
/*TODO*///    /*TODO*///	hd6309_ICount -= cycle_counts_page11[ireg2];
/*TODO*///    /*TODO*///}
/*TODO*///    /*TODO*///
/*TODO*///    
    
}
