/*****************************************************************************
 *
 *	 m6502ops.h
 *	 Addressing mode and opcode macros for 6502,65c02,65sc02,6510,n2a03 CPUs
 *
 *	 Copyright (c) 1998,1999,2000 Juergen Buchmueller, all rights reserved.
 *	 65sc02 core Copyright (c) 2000 Peter Trauner, all rights reserved.
 *
 *	 - This source code is released as freeware for non-commercial purposes.
 *	 - You are free to use and redistribute this code in modified or
 *	   unmodified form, provided you list me in the credits.
 *	 - If you modify this source code, you must add a notice to each modified
 *	   source file that it has been changed.  If you're a nice person, you
 *	   will clearly mark each change too.  :)
 *	 - If you wish to use this for commercial purposes, please contact me at
 *	   pullmoll@t-online.de
 *	 - The author of this copywritten work reserves the right to change the
 *	   terms of its usage and license at any time, including retroactively
 *	 - This entire notice must remain in the source code.
 *
 *****************************************************************************/
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.M6502_IRQ_VEC;
import static mame056.cpu.m6502.m6502.m6502;
import static mame056.cpu.m6502.m6502.m6502_ICount;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.memoryH.change_pc16;

public class opsc02H {
/*TODO*////***************************************************************
/*TODO*/// *	EA = indirect (only used by JMP)
/*TODO*/// * correct overflow handling
/*TODO*/// ***************************************************************/
/*TODO*///#undef EA_IND
    public static void EA_IND() {
        EA_ABS();
        int tmp = RDMEM(m6502.ea.D);
        if (m6502.ea.L==0xff) m6502_ICount[0]++;
        m6502.ea.AddL(1);
        m6502.ea.SetH(RDMEM(m6502.ea.D));
        m6502.ea.SetL(tmp);
    }

    /***************************************************************
     *	EA = zero page indirect (65c02 pre indexed w/o X)
     ***************************************************************/
    public static void EA_ZPI(){													
	m6502.zp.L = RDOPARG();											
	m6502.ea.L = RDMEM(m6502.zp.D);											
        m6502.zp.L++;														
	m6502.ea.H = RDMEM(m6502.zp.D);
    }
    
    /***************************************************************
     *	EA = indirect plus x (only used by 65c02 JMP)
     ***************************************************************/
    public static void EA_IAX(int tmp){
	 EA_ABS();													
	 if (m6502.ea.L + m6502.u8_x > 0xff) /* assumption; probably wrong ? */ 	
		 m6502_ICount[0]--;										
	 m6502.ea.L += m6502.u8_x;													
	 tmp = RDMEM(m6502.ea.D);											
	 if (m6502.ea.L==0xff) m6502_ICount[0]++; 							
	 m6502.ea.D++; 													
	 m6502.ea.H = RDMEM(m6502.ea.D);											
	 m6502.ea.L = tmp;
    }

    public static void RD_ZPI(int tmp) {
        EA_ZPI();
        tmp = RDMEM(m6502.ea.D);
    }

    /* write a value from tmp */
    public static void WR_ZPI(int tmp) {
        EA_ZPI();
        WRMEM(m6502.ea.D, tmp);
    }

/*TODO*////***************************************************************
/*TODO*/// ***************************************************************
/*TODO*/// *			Macros to emulate the 65C02 opcodes
/*TODO*/// ***************************************************************
/*TODO*/// ***************************************************************/
/*TODO*///
/*TODO*///
/*TODO*////* 65C02 ********************************************************
/*TODO*/// *	ADC Add with carry
/*TODO*/// * different setting of flags in decimal mode
/*TODO*/// ***************************************************************/
/*TODO*///#undef ADC
/*TODO*///#define ADC 
    public static void ADC(int tmp) {
	if ((m6502.u8_p & F_D) != 0)
	{															
		int c = (m6502.u8_p & F_C);										
		int lo = (m6502.u8_a & 0x0f) + (tmp & 0x0f) + c; 				
		int hi = (m6502.u8_a & 0xf0) + (tmp & 0xf0); 					
		m6502.u8_p &= ~(F_V | F_C);										
		if( lo > 0x09 ) 										
		{														
			hi += 0x10; 										
			lo += 0x06; 										
		}														
		if(( ~(m6502.u8_a^tmp) & (m6502.u8_a^hi) & F_N ) != 0)
			m6502.u8_p |= F_V;											
		if( hi > 0x90 ) 										
			hi += 0x60; 										
		if(( hi & 0xff00 ) != 0)
			m6502.u8_p |= F_C;											
		m6502.u8_a = (lo & 0x0f) + (hi & 0xf0);							
	}															
	else														
	{															
		int c = (m6502.u8_p & F_C);										
		int sum = m6502.u8_a + tmp + c;									
		m6502.u8_p &= ~(F_V | F_C);										
		if(( ~(m6502.u8_a^tmp) & (m6502.u8_a^sum) & F_N ) != 0)
			m6502.u8_p |= F_V;											
		if(( sum & 0xff00 ) != 0)
			m6502.u8_p |= F_C;											
		m6502.u8_a = sum;										
	}															
	SET_NZ(m6502.u8_a);
    }
/*TODO*////* 65C02 ********************************************************
/*TODO*/// *	SBC Subtract with carry
/*TODO*/// * different setting of flags in decimal mode
/*TODO*/// ***************************************************************/
/*TODO*///#undef SBC
/*TODO*///#define SBC
    public static void SBC(int tmp) {
	if ((m6502.u8_p & F_D) != 0)
	{															
		int c = (m6502.u8_p & F_C) ^ F_C;								
		int sum = m6502.u8_a - tmp - c;									
		int lo = (m6502.u8_a & 0x0f) - (tmp & 0x0f) - c; 				
		int hi = (m6502.u8_a & 0xf0) - (tmp & 0xf0); 					
		m6502.u8_p &= ~(F_V | F_C);										
		if(( (m6502.u8_a^tmp) & (m6502.u8_a^sum) & F_N ) != 0)
			m6502.u8_p |= F_V;											
		if(( lo & 0xf0 ) != 0)
			lo -= 6;											
		if(( lo & 0x80 ) != 0)
			hi -= 0x10; 										
		if(( hi & 0x0f00 ) != 0)
			hi -= 0x60; 										
		if( (sum & 0xff00) == 0 )								
			m6502.u8_p |= F_C;											
		m6502.u8_a = (lo & 0x0f) + (hi & 0xf0);							
	}															
	else														
	{															
		int c = (m6502.u8_p & F_C) ^ F_C;								
		int sum = m6502.u8_a - tmp - c;									
		m6502.u8_p &= ~(F_V | F_C);										
		if(( (m6502.u8_a^tmp) & (m6502.u8_a^sum) & F_N ) != 0)
			m6502.u8_p |= F_V;											
		if( (sum & 0xff00) == 0 )								
			m6502.u8_p |= F_C;											
		m6502.u8_a = sum;										
	}															
	SET_NZ(m6502.u8_a);
    }

    /* 65C02 *******************************************************
     *	BBR Branch if bit is reset
     ***************************************************************/
    public static void BBR(int tmp, int bit){												
	BRA((tmp & (1<<bit))==0?true:false);
    }
    
    /* 65C02 *******************************************************
     *	BBS Branch if bit is set
     ***************************************************************/
    public static void BBS(int tmp, int bit){												
	BRA(((tmp & (1<<bit))!=0) ? true : false);
    }
    
/*TODO*////* 65c02 ********************************************************
/*TODO*/// *	BRK Break
/*TODO*/// *	increment PC, push PC hi, PC lo, flags (with B bit set),
/*TODO*/// *	set I flag, reset D flag and jump via IRQ vector
/*TODO*/// ***************************************************************/
/*TODO*///#undef BRK
/*TODO*///#define BRK 
    public static void BRK() {
	m6502.pc.AddD(1);														
	PUSH(m6502.pc.H);													
	PUSH(m6502.pc.L);													
	PUSH(m6502.u8_p | F_B);												
	m6502.u8_p = (m6502.u8_p | F_I) & ~F_D;										
	m6502.pc.L = RDMEM(M6502_IRQ_VEC); 								
	m6502.pc.H = RDMEM(M6502_IRQ_VEC+1);								
	change_pc16(m6502.pc.D);
    }

    /* 65C02 *******************************************************
     *	DEA Decrement accumulator
     ***************************************************************/
    public static void DEA(){
	m6502.u8_a = m6502.u8_a--;										
	SET_NZ(m6502.u8_a);
    }

    /* 65C02 *******************************************************
     *	INA Increment accumulator
     ***************************************************************/
    public static void INA(){
	m6502.u8_a = m6502.u8_a++;
	SET_NZ(m6502.u8_a);
    }
    
    /* 65C02 *******************************************************
     *	PHX Push index X
     ***************************************************************/
    public static void PHX(){
            PUSH(m6502.u8_x);
    }

    /* 65C02 *******************************************************
     *	PHY Push index Y
     ***************************************************************/
    public static void PHY(){
            PUSH(m6502.u8_y);
    }

    /* 65C02 *******************************************************
     *	PLX Pull index X
     ***************************************************************/
    public static void PLX(){
            m6502.u8_x = PULL(); 
            SET_NZ(m6502.u8_x);
    }

    /* 65C02 *******************************************************
     *	PLY Pull index Y
     ***************************************************************/
    public static void PLY(){
            m6502.u8_y = PULL(); 
            SET_NZ(m6502.u8_y);
    }

    /* 65C02 *******************************************************
     *	RMB Reset memory bit
     ***************************************************************/
    public static void RMB(int tmp, int bit){
        tmp &= ~(1<<bit);
    }
    
    /* 65C02 *******************************************************
     *	SMB Set memory bit
     ***************************************************************/
    public static void SMB(int tmp, int bit){
	tmp |= (1<<bit);
    }

    /* 65C02 *******************************************************
     * STZ	Store zero
     ***************************************************************/
    public static void STZ(int tmp) { 													
	tmp = 0;
    }
    
    /* 65C02 *******************************************************
     * TRB	Test and reset bits
     ***************************************************************/
    public static void TRB(int tmp){	
	SET_Z(tmp&m6502.u8_a);												
	tmp &= ~m6502.u8_a;
    }

    /* 65C02 *******************************************************
     * TSB	Test and set bits
     ***************************************************************/
    public static void TSB(int tmp) { 													
	SET_Z(tmp&m6502.u8_a);												
	tmp |= m6502.u8_a;
    }

    /***************************************************************
     ***************************************************************
     *			Macros to emulate the 65sc02 opcodes
     ***************************************************************
     ***************************************************************/


    /* 65sc02 ********************************************************
     *	BSR Branch to subroutine
     ***************************************************************/
    public static void BSR(){
/*TODO*///	EAL = RDOPARG();
        m6502.ea.L = RDOPARG();
        PUSH(m6502.pc.H);
        PUSH(m6502.pc.L);													
        m6502.ea.H = RDOPARG();											
/*TODO*///	EAW = PCW + (INT16)(EAW-1); 
        m6502.ea.SetD(m6502.pc.D + m6502.ea.D-1);
/*TODO*///	PCD = EAD;
        m6502.pc.SetD(m6502.ea.D);
/*TODO*///	CHANGE_PC
        change_pc16(m6502.pc.D);
    }

    
}
