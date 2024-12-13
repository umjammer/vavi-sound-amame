/*****************************************************************************
 *
 *	 ill02.h
 *	 Addressing mode and opcode macros for the NMOS 6502 illegal opcodes
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

/* test with the excellent C64 Emulator test suite
   ? at www.funet.fi/pub/cbm/documents/chipdata/tsuit215.zip
   good reference in the vice emulator (source) distribution doc/64doc.txt

   $ab=OAL like in 6502-NMOS.extra.opcodes, vice so in vice (lxa)
*/

package mame056.cpu.m6502;

import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.m6502.*;

public class ill02H {
    /***************************************************************
     ***************************************************************
     *			Macros to emulate the 6510 opcodes
     ***************************************************************
     ***************************************************************/
    
    /* 6510 ********************************************************
     *	ANC logical and, set carry from bit of A
     ***************************************************************/
    public static void ANC(int tmp){
    	m6502.u8_p &= ~F_C;													
    	m6502.u8_a = (m6502.u8_a & tmp);										
    	if ((m6502.u8_a & 0x80) != 0)
    		m6502.u8_p |= F_C;												
    	SET_NZ(m6502.u8_a);
    }
    
    /* 6510 ********************************************************
     *	ASR logical and, logical shift right
     ***************************************************************/
    public static int ASR(int tmp){
    	tmp &= m6502.u8_a; 									
    	LSR(tmp);
        
        return tmp;
    }
    
    /* 6510 ********************************************************
     * AST	and stack; transfer to accumulator and index X
     * logical and stack (LSB) with data, transfer result to S
     * transfer result to accumulator and index X also
     ***************************************************************/
    public static void AST(int tmp){
    	m6502.sp.SetL(m6502.sp.L & tmp);
    	m6502.u8_a = m6502.sp.L;													
        m6502.u8_x = m6502.sp.L;
    	SET_NZ(m6502.u8_a);
    }
    
    /* 6510 ********************************************************
     *	ARR logical and, rotate right
     ***************************************************************/
    public static int ARR(int tmp){
    	if(( m6502.u8_p & F_D ) != 0)
    	{															
    		int lo, hi, t;											
    		tmp &= m6502.u8_a;												
    		t = tmp;												
    		hi = tmp &0xf0; 										
    		lo = tmp &0x0f; 										
    		if(( m6502.u8_p & F_C ) != 0)
    		{														
    			tmp = (tmp >> 1) | 0x80;							
    			m6502.u8_p |= F_N;											
    		}														
    		else													
    		{														
    			tmp >>= 1;											
    			m6502.u8_p &= ~F_N;											
    		}														
    		if( tmp != 0 )												
    			m6502.u8_p &= ~F_Z;											
    		else													
                m6502.u8_p |= F_Z;                                           
    		if(( (t^tmp) & 0x40 ) != 0)
    			m6502.u8_p|=F_V; 											
    		else													
    			m6502.u8_p &= ~F_V;											
    		if( lo + (lo & 0x01) > 0x05 )							
    			tmp = (tmp & 0xf0) | ((tmp+6) & 0xf);				
    		if( hi + (hi & 0x10) > 0x50 )							
    		{														
    			m6502.u8_p |= F_C;											
    			tmp = (tmp+0x60) & 0xff;							
    		}														
    		else													
    			m6502.u8_p &= ~F_C;											
    	}															
    	else														
    	{															
    		tmp &= m6502.u8_a;												
    		ROR(tmp);													
    		m6502.u8_p &=~(F_V|F_C); 										
    		if(( tmp & 0x40 ) != 0)
    			m6502.u8_p|=F_C; 											
    		if( (tmp & 0x60) == 0x20 || (tmp & 0x60) == 0x40 )		
    			m6502.u8_p|=F_V; 											
    	}
        
        return tmp;
    }
    
    /* 6510 ********************************************************
     *	ASX logical and X w/ A, subtract data from X
     ***************************************************************/
    public static void ASX(int tmp){
    	m6502.u8_p &= ~F_C;													
    	m6502.u8_x &= m6502.u8_a; 													
    	if (m6502.u8_x >= tmp)												
    		m6502.u8_p |= F_C;												
    	m6502.u8_x = (m6502.u8_x - tmp);										
    	SET_NZ(m6502.u8_x);
    }
    
    /* 6510 ********************************************************
     *	AXA transfer index X to accumulator, logical and
     * depends on the data of the dma device (videochip) fetched
     * between opcode read and operand read
     ***************************************************************/
    public static void AXA(int tmp){
    	m6502.u8_a = ( (m6502.u8_a|0xee)& m6502.u8_x & tmp);							
    	SET_NZ(m6502.u8_a);
    }
    
    /* 6510 ********************************************************
     *	DCP decrement data and compare
     ***************************************************************/
    public static int DCP(int tmp){
    	tmp = (tmp-1); 										
    	m6502.u8_p &= ~F_C;													
    	if (m6502.u8_a >= tmp)												
    		m6502.u8_p |= F_C;												
    	SET_NZ((m6502.u8_a - tmp));
        
        return tmp;
    }
    
    /*TODO*////* 6502 ********************************************************
    /*TODO*/// *	DOP double no operation
    /*TODO*/// ***************************************************************/
    /*TODO*///#define DOP 													
    /*TODO*///	PCW++
    /*TODO*///
    /* 6510 ********************************************************
     *	ISB increment and subtract with carry
     ***************************************************************/
    public static int ISB(int tmp){
    	tmp = (tmp+1); 										
    	SBC(tmp);
        return tmp;
    }
    
    /* 6510 ********************************************************
     *	LAX load accumulator and index X
     ***************************************************************/
    public static void LAX(int tmp){
    	m6502.u8_a = tmp;
        m6502.u8_x = tmp; 										
    	SET_NZ(m6502.u8_a);
    }
    
    /* 6510 ********************************************************
     *	OAL load accumulator and index X
     ***************************************************************/
    public static void OAL(int tmp){
    	m6502.u8_a = ((m6502.u8_a|0xee)&tmp);
        m6502.u8_x = ((m6502.u8_a|0xee)&tmp);
    	SET_NZ(m6502.u8_a);
    }
    
    /* 6510 ********************************************************
     * RLA	rotate left and logical and accumulator
     *	new C <- [7][6][5][4][3][2][1][0] <- C
     ***************************************************************/
    public static int RLA(int tmp){
            tmp = (tmp << 1) | (m6502.u8_p & F_C);								
            m6502.u8_p = (m6502.u8_p & ~F_C) | ((tmp >> 8) & F_C);						
            tmp = tmp;											
            m6502.u8_a &= tmp;													
            SET_NZ(m6502.u8_a);
            return tmp;
    }

    /* 6510 ********************************************************
     * RRA	rotate right and add with carry
     *	C -> [7][6][5][4][3][2][1][0] -> C
     ***************************************************************/
    public static int RRA(int tmp){
    	tmp |= (m6502.u8_p & F_C) << 8;										
    	m6502.u8_p = (m6502.u8_p & ~F_C) | (tmp & F_C);								
    	tmp = (tmp >> 1);									
    	ADC(tmp);
        return tmp;
    }
    
    /* 6510 ********************************************************
     * SAX	logical and accumulator with index X and store
     ***************************************************************/
    public static int SAX(int tmp){
            tmp = m6502.u8_a & m6502.u8_x;
            return tmp;
    }

    /* 6510 ********************************************************
     *	SLO shift left and logical or
     ***************************************************************/
    public static int SLO(int tmp){
            m6502.u8_p = (m6502.u8_p & ~F_C) | ((tmp >> 7) & F_C);
            tmp = (tmp << 1);
            m6502.u8_a |= tmp;
            SET_NZ(m6502.u8_a);
            return tmp;
    }

    /* 6510 ********************************************************
     *	SRE logical shift right and logical exclusive or
     *	0 -> [7][6][5][4][3][2][1][0] -> C
     ***************************************************************/
    public static int SRE(int tmp){
    	m6502.u8_p = (m6502.u8_p & ~F_C) | (tmp & F_C);								
    	tmp = tmp >> 1;										
    	m6502.u8_a ^= tmp;													
    	SET_NZ(m6502.u8_a);
        return tmp;
    }
    
    /* 6510 ********************************************************
     * SAH	store accumulator and index X and high + 1
     * result = accumulator and index X and memory [PC+1] + 1
     ***************************************************************/
    public static int SAH(int tmp){
        tmp = m6502.u8_a & m6502.u8_x & (m6502.ea.H+1);
        return tmp;
    }
    
    /*TODO*////* 6510 ********************************************************
    /*TODO*/// * SSH	store stack high
    /*TODO*/// * logical and accumulator with index X, transfer result to S
    /*TODO*/// * logical and result with memory [PC+1] + 1
    /*TODO*/// ***************************************************************/
    public static int SSH(int tmp){
    	m6502.sp.SetL( m6502.u8_a & m6502.u8_x );
        tmp = m6502.sp.L & (m6502.ea.H+1);
    
    /*TODO*///#if 0
    /*TODO*///	tmp = S = A & X;											
    /*TODO*///	tmp &= (UINT8)(cpu_readop_arg((PCW + 1) & 0xffff) + 1)
    /*TODO*///#endif
        return tmp;
    }
    
    /* 6510 ********************************************************
     * SXH	store index X high
     * logical and index X with memory[PC+1] and store the result
     ***************************************************************/
    public static int SXH(int tmp){
        tmp = m6502.u8_x & (m6502.ea.H+1);
        return tmp;
    }
    
    /* 6510 ********************************************************
     * SYH	store index Y and (high + 1)
     * logical and index Y with memory[PC+1] + 1 and store the result
     ***************************************************************/
    public static int SYH(int tmp){ 
        tmp = m6502.u8_y & (m6502.ea.H+1);
        return tmp;
    }
    
    /* 6510 ********************************************************
     *	TOP triple no operation
     ***************************************************************/
    public static void TOP(){
        m6502.pc.AddD(2);
    }

    /* 6510 ********************************************************
     *	KIL Illegal opcode
     * processor halted: no hardware interrupt will help,
     * only reset
     ***************************************************************/

    public static void KIL(){
	ILL();
    }
}
