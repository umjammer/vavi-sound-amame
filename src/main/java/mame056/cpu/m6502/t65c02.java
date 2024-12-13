/*****************************************************************************
 *
 *	 tbl65c02.c
 *	 65c02 opcode functions and function pointer table
 *
 *	 Copyright (c) 1998,1999,2000 Juergen Buchmueller, all rights reserved.
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

import static mame056.cpu.m6502.m6502.*;
/*import static mame056.cpu.m6502.ops02H.AND;
import static mame056.cpu.m6502.ops02H.BIT;
import static mame056.cpu.m6502.ops02H.BRA;
import static mame056.cpu.m6502.ops02H.CMP;
import static mame056.cpu.m6502.ops02H.EOR;
import static mame056.cpu.m6502.ops02H.JMP;
import static mame056.cpu.m6502.ops02H.JSR;
import static mame056.cpu.m6502.ops02H.LDA;
import static mame056.cpu.m6502.ops02H.ORA;
import static mame056.cpu.m6502.ops02H.RD_ABS;
import static mame056.cpu.m6502.ops02H.RD_ABX;
import static mame056.cpu.m6502.ops02H.RD_IMM;
import static mame056.cpu.m6502.ops02H.RD_ZPG;
import static mame056.cpu.m6502.ops02H.RD_ZPX;
import static mame056.cpu.m6502.ops02H.STA;
import static mame056.cpu.m6502.ops02H.WB_EA;
import static mame056.cpu.m6502.ops02H.WR_ABS;
import static mame056.cpu.m6502.ops02H.WR_ABX;
import static mame056.cpu.m6502.ops02H.WR_ZPG;
import static mame056.cpu.m6502.ops02H.WR_ZPX;*/
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.opsc02H.*;
import static mame056.cpu.m6502.ill02H.*;
import static mame056.cpu.m6502.t6502.*;

/**
 * ported to v0.56
 */
public class t65c02 {
/*****************************************************************************
 *****************************************************************************
 *
 *	 overrides for 65C02 opcodes
 *
 *****************************************************************************
 * op	 temp	  cycles			 rdmem	 opc  wrmem   ********************/
    //OP(00)
    static opcode m65c02_00 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            opsc02H.BRK();
        }
    };/* 7 BRK */

    static opcode m65c02_20 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            JSR();
        }
    };									/* 6 JSR ABS */
    static opcode m65c02_40 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTI();
        }
    };									/* 6 RTI */
    
    static opcode m65c02_60 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTS();
        }
    };									/* 6 RTS */
    
    static opcode m65c02_80 = new opcode() {
        public void handler() {
            int tmp=0;
            BRA(true);
        }
    }; /* 2 BRA */  

    static opcode m65c02_a0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDY(tmp);
        }
    };									/* 2 LDY IMM */
    
    static opcode m65c02_c0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPY(tmp);
        }
    };									/* 2 CPY IMM */
    
    static opcode m65c02_e0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPX(tmp);
        }
    };									/* 2 CPX IMM */
    

static opcode m65c02_10 = new opcode() {
        public void handler() {
            BPL();
        }
    };									/* 2 BPL */

static opcode m65c02_30 = new opcode() {
        public void handler() {
            BMI();
        }
    };									/* 2 BMI */

static opcode m65c02_50 = new opcode() {
        public void handler() {
            BVC();
        }
    };									/* 2 BVC */

static opcode m65c02_70 = new opcode() {
        public void handler() {
            BVS();
        }
    };									/* 2 BVS */

static opcode m65c02_90 = new opcode() {
        public void handler() {
            BCC();
        }
    };									/* 2 BCC */

static opcode m65c02_b0 = new opcode() {
        public void handler() {
            BCS();
        }
    };									/* 2 BCS */

static opcode m65c02_d0 = new opcode() {
        public void handler() {
            BNE();
        }
    };									/* 2 BNE */

static opcode m65c02_f0 = new opcode() {
        public void handler() {
            BEQ();
        }
    };									/* 2 BEQ */


static opcode m65c02_01 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ORA(tmp);
        }
    };									/* 6 ORA IDX */

static opcode m65c02_21 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            AND(tmp);
        }
    };									/* 6 AND IDX */

static opcode m65c02_41 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            EOR(tmp);
        }
    };									/* 6 EOR IDX */

static opcode m65c02_61 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            opsc02H.ADC(tmp);
        }
    };									/* 6 ADC IDX */

static opcode m65c02_81 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDX(tmp);
        }
    };									/* 6 STA IDX */

static opcode m65c02_a1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            LDA(tmp);
        }
    };									/* 6 LDA IDX */

static opcode m65c02_c1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            CMP(tmp);
        }
    };									/* 6 CMP IDX */

static opcode m65c02_e1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            opsc02H.SBC(tmp);
        }
    };									/* 6 SBC IDX */

static opcode m65c02_11 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ORA(tmp);
        }
    };									/* 5 ORA IDY; */

static opcode m65c02_31 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            AND(tmp);
        }
    };									/* 5 AND IDY; */

static opcode m65c02_51 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            EOR(tmp);
        }
    };									/* 5 EOR IDY; */

static opcode m65c02_71 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            opsc02H.ADC(tmp);
        }
    };									/* 5 ADC IDY; */

static opcode m65c02_91 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDY(tmp);
        }
    };									/* 6 STA IDY; */

static opcode m65c02_b1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            LDA(tmp);
        }
    };									/* 5 LDA IDY; */

static opcode m65c02_d1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            CMP(tmp);
        }
    };									/* 5 CMP IDY; */

static opcode m65c02_f1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            opsc02H.SBC(tmp);
        }
    };									/* 5 SBC IDY; */

static opcode m65c02_02 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_22 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_42 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_62 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_82 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_a2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDX(tmp);
        }
    };									/* 2 LDX IMM */

static opcode m65c02_c2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_e2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */


/*TODO*///#ifndef CORE_M65CE02
    static opcode m65c02_12 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            ORA(tmp);
        }
    }; /* 3 ORA ZPI */

    static opcode m65c02_32 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            AND(tmp);
        } 
    }; /* 3 AND ZPI */

    static opcode m65c02_52 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            EOR(tmp);
        } 
    }; /* 3 EOR ZPI */

    static opcode m65c02_72 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            opsc02H.ADC(tmp);
        } 
    }; /* 3 ADC ZPI */

    static opcode m65c02_92 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 4;
            tmp=STA(); 
            WR_ZPI(tmp); 
        } 
    }; /* 3 STA ZPI */

    static opcode m65c02_b2 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            LDA(tmp);
        } 
    }; /* 3 LDA ZPI */

    static opcode m65c02_d2 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            CMP(tmp);
        } 
    }; /* 3 CMP ZPI */

    static opcode m65c02_f2 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            RD_ZPI(tmp); 
            opsc02H.SBC(tmp);
        } 
    }; /* 3 SBC ZPI */

/*TODO*///#endif

static opcode m65c02_03 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_23 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_43 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_63 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_83 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_a3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_c3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_e3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_13 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_33 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_53 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_73 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_93 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_b3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_d3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_f3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

    static opcode m65c02_04 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            tmp=RD_ZPG(); 
            TSB(tmp); 
            WB_EA(tmp);
        } 
    }; /* 3 TSB ZPG */

static opcode m65c02_24 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            BIT(tmp);
        }
    };									/* 3 BIT ZPG */

static opcode m65c02_44 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

    static opcode m65c02_64 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 2;
            STZ(tmp); 
            WR_ZPG(tmp);
        } 
    }; /* 3 STZ ZPG */

static opcode m65c02_84 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STY();
            WR_ZPG(tmp);
        }
    };									/* 3 STY ZPG */

static opcode m65c02_a4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDY(tmp);
        }
    };									/* 3 LDY ZPG */

static opcode m65c02_c4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPY(tmp);
        }
    };									/* 3 CPY ZPG */

static opcode m65c02_e4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPX(tmp);
        }
    };									/* 3 CPX ZPG */


    static opcode m65c02_14 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 3; 
            tmp=RD_ZPG(); 
            TRB(tmp); 
            WB_EA(tmp);  
        } 
    }; /* 3 TRB ZPG */

    static opcode m65c02_34 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 4; 
            tmp=RD_ZPX(); 
            BIT(tmp);
        } 
    }; /* 4 BIT ZPX */

static opcode m65c02_54 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

    static opcode m65c02_74 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 4;
            STZ(tmp);
            WR_ZPX(tmp); 
        } 
    }; /* 4 STZ ZPX */

static opcode m65c02_94 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ZPX(tmp);
        }
    };									/* 4 STY ZPX */

static opcode m65c02_b4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDY(tmp);
        }
    };									/* 4 LDY ZPX */

static opcode m65c02_d4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_f4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_05 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ORA(tmp);
        }
    };									/* 3 ORA ZPG */

static opcode m65c02_25 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            AND(tmp);
        }
    };									/* 3 AND ZPG */

static opcode m65c02_45 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            EOR(tmp);
        }
    };									/* 3 EOR ZPG */

static opcode m65c02_65 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            opsc02H.ADC(tmp);
        }
    };									/* 3 ADC ZPG */

static opcode m65c02_85 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STA();
            WR_ZPG(tmp);
        }
    };									/* 3 STA ZPG */

static opcode m65c02_a5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDA(tmp);
        }
    };									/* 3 LDA ZPG */

static opcode m65c02_c5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CMP(tmp);
        }
    };									/* 3 CMP ZPG */

static opcode m65c02_e5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            opsc02H.SBC(tmp);
        }
    };									/* 3 SBC ZPG */

static opcode m65c02_15 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ORA(tmp);
        }
    };									/* 4 ORA ZPX */

static opcode m65c02_35 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            AND(tmp);
        }
    };									/* 4 AND ZPX */

static opcode m65c02_55 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            EOR(tmp);
        }
    };									/* 4 EOR ZPX */

static opcode m65c02_75 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            opsc02H.ADC(tmp);
        }
    };									/* 4 ADC ZPX */

static opcode m65c02_95 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ZPX(tmp);
        }
    };									/* 4 STA ZPX */

static opcode m65c02_b5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDA(tmp);
        }
    };									/* 4 LDA ZPX */

static opcode m65c02_d5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            CMP(tmp);
        }
    };									/* 4 CMP ZPX */

static opcode m65c02_f5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            opsc02H.SBC(tmp);
        }
    };									/* 4 SBC ZPX */

static opcode m65c02_06 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };									/* 5 ASL ZPG */

static opcode m65c02_26 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };									/* 5 ROL ZPG */

static opcode m65c02_46 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };									/* 5 LSR ZPG */

static opcode m65c02_66 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };									/* 5 ROR ZPG */

static opcode m65c02_86 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STX();
            WR_ZPG(tmp);
        }
    };									/* 3 STX ZPG */

static opcode m65c02_a6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDX(tmp);
        }
    };									/* 3 LDX ZPG */

static opcode m65c02_c6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };									/* 5 DEC ZPG */

static opcode m65c02_e6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };									/* 5 INC ZPG */

static opcode m65c02_16 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 ASL ZPX */

static opcode m65c02_36 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 ROL ZPX */

static opcode m65c02_56 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 LSR ZPX */

static opcode m65c02_76 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 ROR ZPX */

static opcode m65c02_96 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STX();
            WR_ZPY(tmp);
        }
    };									/* 4 STX ZPY */

static opcode m65c02_b6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPY();
            LDX(tmp);
        }
    };									/* 4 LDX ZPY */

static opcode m65c02_d6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 DEC ZPX */

static opcode m65c02_f6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 INC ZPX */

    static opcode m65c02_07 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 0);
            WB_EA(tmp);
        } 
    }; /* 5 RMB0 ZPG */

    static opcode m65c02_27 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 2);
            WB_EA(tmp);
        } 
    }; /* 5 RMB2 ZPG */

    static opcode m65c02_47 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 4);
            WB_EA(tmp);
        } 
    }; /* 5 RMB4 ZPG */

    static opcode m65c02_67 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 6);
            WB_EA(tmp);
        } 
    }; /* 5 RMB6 ZPG */

    static opcode m65c02_87 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 0);
            WB_EA(tmp);
        } 
    }; /* 5 SMB0 ZPG */

    static opcode m65c02_a7 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 2);
            WB_EA(tmp);
        } 
    }; /* 5 SMB2 ZPG */

    static opcode m65c02_c7 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 4);
            WB_EA(tmp);
        } 
    }; /* 5 SMB4 ZPG */

    static opcode m65c02_e7 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 6);
            WB_EA(tmp);
        } 
    }; /* 5 SMB6 ZPG */


    static opcode m65c02_17 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 1);
            WB_EA(tmp);
        } 
    }; /* 5 RMB1 ZPG */

    static opcode m65c02_37 = new opcode() {
        public void handler() {int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 3);
            WB_EA(tmp);
        } 
    }; /* 5 RMB3 ZPG */
    
    static opcode m65c02_57 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 5);
            WB_EA(tmp);
        } 
    }; /* 5 RMB5 ZPG */
    
    static opcode m65c02_77 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            RMB(tmp, 7);
            WB_EA(tmp);
        } 
    }; /* 5 RMB7 ZPG */
    
    static opcode m65c02_97 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 1);
            WB_EA(tmp);
        } 
    }; /* 5 SMB1 ZPG */
    
    static opcode m65c02_b7 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 3);
            WB_EA(tmp);
        } 
    }; /* 5 SMB3 ZPG */
    
    static opcode m65c02_d7 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 5);
            WB_EA(tmp);
        } 
    }; /* 5 SMB5 ZPG */
    
    static opcode m65c02_f7 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            SMB(tmp, 7);
            WB_EA(tmp);
        } 
    }; /* 5 SMB7 ZPG */
    

static opcode m65c02_08 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHP();
        }
    };									/* 3 PHP */

static opcode m65c02_28 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLP();
        }
    };									/* 4 PLP */

static opcode m65c02_48 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHA();
        }
    };									/* 3 PHA */

static opcode m65c02_68 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLA();
        }
    };									/* 4 PLA */

static opcode m65c02_88 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEY();
        }
    };									/* 2 DEY */

static opcode m65c02_a8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAY();
        }
    };									/* 2 TAY */

static opcode m65c02_c8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INY();
        }
    };									/* 2 INY */

static opcode m65c02_e8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INX();
        }
    };									/* 2 INX */

static opcode m65c02_18 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLC();
        }
    };									/* 2 CLC */

static opcode m65c02_38 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEC();
        }
    };									/* 2 SEC */

static opcode m65c02_58 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLI();
        }
    };									/* 2 CLI */

static opcode m65c02_78 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEI();
        }
    };									/* 2 SEI */

static opcode m65c02_98 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TYA();
        }
    };									/* 2 TYA */

static opcode m65c02_b8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLV();
        }
    };									/* 2 CLV */

static opcode m65c02_d8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLD();
        }
    };									/* 2 CLD */

static opcode m65c02_f8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SED();
        }
    };									/* 2 SED */

static opcode m65c02_09 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ORA(tmp);
        }
    };									/* 2 ORA IMM */

static opcode m65c02_29 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            AND(tmp);
        }
    };									/* 2 AND IMM */

static opcode m65c02_49 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            EOR(tmp);
        }
    };									/* 2 EOR IMM */

static opcode m65c02_69 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            opsc02H.ADC(tmp);
        }
    };									/* 2 ADC IMM */

    static opcode m65c02_89 = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 2; 
            tmp=RD_IMM(); 
            BIT(tmp);		  
        } 
    }; /* 2 BIT IMM */
    
static opcode m65c02_a9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDA(tmp);
        }
    };									/* 2 LDA IMM */

static opcode m65c02_c9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CMP(tmp);
        }
    };									/* 2 CMP IMM */

static opcode m65c02_e9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            opsc02H.SBC(tmp);
        }
    };									/* 2 SBC IMM */

static opcode m65c02_19 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ORA(tmp);
        }
    };									/* 4 ORA ABY */

static opcode m65c02_39 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            AND(tmp);
        }
    };									/* 4 AND ABY */

static opcode m65c02_59 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            EOR(tmp);
        }
    };									/* 4 EOR ABY */

static opcode m65c02_79 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            opsc02H.ADC(tmp);
        }
    };									/* 4 ADC ABY */

static opcode m65c02_99 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABY(tmp);
        }
    };									/* 5 STA ABY */

static opcode m65c02_b9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDA(tmp);
        }
    };									/* 4 LDA ABY */

static opcode m65c02_d9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            CMP(tmp);
        }
    };									/* 4 CMP ABY */

static opcode m65c02_f9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            opsc02H.SBC(tmp);
        }
    };									/* 4 SBC ABY */

static opcode m65c02_0a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ASL(tmp);
            WB_ACC(tmp2);
        }
    };									/* 2 ASL */

static opcode m65c02_2a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROL(tmp);
            WB_ACC(tmp2);
        }
    };									/* 2 ROL */

static opcode m65c02_4a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = LSR(tmp);
            WB_ACC(tmp2);
        }
    };									/* 2 LSR */

static opcode m65c02_6a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROR(tmp);
            WB_ACC(tmp2);
        }
    };									/* 2 ROR */

static opcode m65c02_8a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXA();
        }
    };									/* 2 TXA */

static opcode m65c02_aa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAX();
        }
    };									/* 2 TAX */

static opcode m65c02_ca = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEX();
        }
    };									/* 2 DEX */

static opcode m65c02_ea = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            NOP();
        }
    };									/* 2 NOP */

    static opcode m65c02_1a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INA();
        } 
    }; /* 2 INA */

    static opcode m65c02_3a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEA();
        } 
    }; /* 2 DEA */

    static opcode m65c02_5a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            PHY();
        } 
    }; /* 3 PHY */

    static opcode m65c02_7a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            PLY();
        } 
    }; /* 4 PLY */

static opcode m65c02_9a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXS();
        }
    };									/* 2 TXS */

static opcode m65c02_ba = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TSX();
        }
    };									/* 2 TSX */

    static opcode m65c02_da = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            PHX();
        } 
    }; /* 3 PHX */

    static opcode m65c02_fa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            PLX();
        } 
    }; /* 4 PLX */
//HERE
static opcode m65c02_0b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_2b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_4b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_6b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_8b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_ab = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_cb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_eb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_1b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_3b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_5b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_7b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_9b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_bb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_db = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
static opcode m65c02_fb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */
// END HERE

    static opcode m65c02_0c = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 2; 
            tmp=RD_ABS(); 
            TSB(tmp); 
            WB_EA(tmp);
        } 
    }; /* 4 TSB ABS */

static opcode m65c02_2c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            BIT(tmp);
        }
    };									/* 4 BIT ABS */

static opcode m65c02_4c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            EA_ABS();
            JMP();
        }
    };									/* 3 JMP ABS */

    static opcode m65c02_6c = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            opsc02H.EA_IND(); 
            JMP();
        } 
    }; /* 5 JMP IND */

static opcode m65c02_8c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ABS(tmp);
        }
    };									/* 4 STY ABS */

static opcode m65c02_ac = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDY(tmp);
        }
    };									/* 4 LDY ABS */

static opcode m65c02_cc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPY(tmp);
        }
    };									/* 4 CPY ABS */

static opcode m65c02_ec = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPX(tmp);
        }
    };									/* 4 CPX ABS */

    static opcode m65c02_1c = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 4; 
            tmp=RD_ABS(); 
            TRB(tmp); 
            WB_EA(tmp);  
        } 
    }; /* 4 TRB ABS */

    static opcode m65c02_3c = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 4; 
            tmp=RD_ABX(); 
            BIT(tmp);
        } 
    }; /* 4 BIT ABX */

static opcode m65c02_5c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

    static opcode m65c02_7c = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 2; 
            EA_IAX(tmp); 
            JMP();
        } 
    }; /* 6 JMP IAX */

    static opcode m65c02_9c = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 4;
            STZ(tmp); 
            WR_ABS(tmp); 
        } 
    }; /* 4 STZ ABS */
    
static opcode m65c02_bc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDY(tmp);
        }
    };									/* 4 LDY ABX */

static opcode m65c02_dc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_fc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };									/* 2 ILL */

static opcode m65c02_0d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ORA(tmp);
        }
    };									/* 4 ORA ABS */

static opcode m65c02_2d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            AND(tmp);
        }
    };									/* 4 AND ABS */

static opcode m65c02_4d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            EOR(tmp);
        }
    };									/* 4 EOR ABS */

static opcode m65c02_6d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            opsc02H.ADC(tmp);
        }
    };									/* 4 ADC ABS */

static opcode m65c02_8d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ABS(tmp);
        }
    };									/* 4 STA ABS */

static opcode m65c02_ad = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDA(tmp);
        }
    };									/* 4 LDA ABS */

static opcode m65c02_cd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CMP(tmp);
        }
    };									/* 4 CMP ABS */

static opcode m65c02_ed = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            opsc02H.SBC(tmp);
        }
    };									/* 4 SBC ABS */

static opcode m65c02_1d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ORA(tmp);
        }
    };									/* 4 ORA ABX */

static opcode m65c02_3d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            AND(tmp);
        }
    };									/* 4 AND ABX */

static opcode m65c02_5d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            EOR(tmp);
        }
    };									/* 4 EOR ABX */

static opcode m65c02_7d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            opsc02H.ADC(tmp);
        }
    };									/* 4 ADC ABX */

static opcode m65c02_9d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABX(tmp);
        }
    };									/* 5 STA ABX */

static opcode m65c02_bd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDA(tmp);
        }
    };									/* 4 LDA ABX */

static opcode m65c02_dd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            CMP(tmp);
        }
    };									/* 4 CMP ABX */

static opcode m65c02_fd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            opsc02H.SBC(tmp);
        }
    };									/* 4 SBC ABX */

static opcode m65c02_0e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 ASL ABS */

static opcode m65c02_2e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 ROL ABS */

static opcode m65c02_4e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 LSR ABS */

static opcode m65c02_6e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 ROR ABS */

static opcode m65c02_8e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STX();
            WR_ABS(tmp);
        }
    };									/* 4 STX ABS */

static opcode m65c02_ae = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDX(tmp);
        }
    };									/* 4 LDX ABS */

static opcode m65c02_ce = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 DEC ABS */

static opcode m65c02_ee = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };									/* 6 INC ABS */

static opcode m65c02_1e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };									/* 7 ASL ABX */

static opcode m65c02_3e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };									/* 7 ROL ABX */

static opcode m65c02_5e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };									/* 7 LSR ABX */

static opcode m65c02_7e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };									/* 7 ROR ABX */

    static opcode m65c02_9e = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5;
            STZ(tmp); 
            WR_ABX(tmp); 
        } 
    }; /* 5 STZ ABX */
    
static opcode m65c02_be = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDX(tmp);
        }
    };									/* 4 LDX ABY */

static opcode m65c02_de = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };									/* 7 DEC ABX */

static opcode m65c02_fe = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };									/* 7 INC ABX */

    static opcode m65c02_0f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 0);
        } 
    }; /* 5 BBR0 ZPG */
    
    static opcode m65c02_2f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 2);	  
        } 
    }; /* 5 BBR2 ZPG */
    
    static opcode m65c02_4f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 4);
        } 
    }; /* 5 BBR4 ZPG */
    
    static opcode m65c02_6f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 6);
        } 
    }; /* 5 BBR6 ZPG */
    
    static opcode m65c02_8f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 0);
        } 
    }; /* 5 BBS0 ZPG */
    
    static opcode m65c02_af = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 2);
        } 
    }; /* 5 BBS2 ZPG */
    
    static opcode m65c02_cf = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 4);
        } 
    }; /* 5 BBS4 ZPG */
    
    static opcode m65c02_ef = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 6);
        } 
    }; /* 5 BBS6 ZPG */
    
    static opcode m65c02_1f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 1);
        } 
    }; /* 5 BBR1 ZPG */
    
    static opcode m65c02_3f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 3);
        } 
    }; /* 5 BBR3 ZPG */
    
    static opcode m65c02_5f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 5);
        } 
    }; /* 5 BBR5 ZPG */
    
    static opcode m65c02_7f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBR(tmp, 7);
        } 
    }; /* 5 BBR7 ZPG */
    
    static opcode m65c02_9f = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 1);
        } 
    }; /* 5 BBS1 ZPG */
    
    static opcode m65c02_bf = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 3);
        } 
    }; /* 5 BBS3 ZPG */
    
    static opcode m65c02_df = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5; 
            tmp=RD_ZPG(); 
            BBS(tmp, 5);
        } 
    }; /* 5 BBS5 ZPG */
    
    static opcode m65c02_ff = new opcode() {
        public void handler() {
            int tmp=0;
            m6502_ICount[0] -= 5;
            tmp=RD_ZPG();
            BBS(tmp, 7);
        } 
    }; /* 5 BBS7 ZPG */

    public static opcode[] insn65c02 = {
	m65c02_00,m65c02_01,m65c02_02,m65c02_03,m65c02_04,m65c02_05,m65c02_06,m65c02_07,
	m65c02_08,m65c02_09,m65c02_0a,m65c02_0b,m65c02_0c,m65c02_0d,m65c02_0e,m65c02_0f,
	m65c02_10,m65c02_11,m65c02_12,m65c02_13,m65c02_14,m65c02_15,m65c02_16,m65c02_17,
	m65c02_18,m65c02_19,m65c02_1a,m65c02_1b,m65c02_1c,m65c02_1d,m65c02_1e,m65c02_1f,
	m65c02_20,m65c02_21,m65c02_22,m65c02_23,m65c02_24,m65c02_25,m65c02_26,m65c02_27,
	m65c02_28,m65c02_29,m65c02_2a,m65c02_2b,m65c02_2c,m65c02_2d,m65c02_2e,m65c02_2f,
	m65c02_30,m65c02_31,m65c02_32,m65c02_33,m65c02_34,m65c02_35,m65c02_36,m65c02_37,
	m65c02_38,m65c02_39,m65c02_3a,m65c02_3b,m65c02_3c,m65c02_3d,m65c02_3e,m65c02_3f,
	m65c02_40,m65c02_41,m65c02_42,m65c02_43,m65c02_44,m65c02_45,m65c02_46,m65c02_47,
	m65c02_48,m65c02_49,m65c02_4a,m65c02_4b,m65c02_4c,m65c02_4d,m65c02_4e,m65c02_4f,
	m65c02_50,m65c02_51,m65c02_52,m65c02_53,m65c02_54,m65c02_55,m65c02_56,m65c02_57,
	m65c02_58,m65c02_59,m65c02_5a,m65c02_5b,m65c02_5c,m65c02_5d,m65c02_5e,m65c02_5f,
	m65c02_60,m65c02_61,m65c02_62,m65c02_63,m65c02_64,m65c02_65,m65c02_66,m65c02_67,
	m65c02_68,m65c02_69,m65c02_6a,m65c02_6b,m65c02_6c,m65c02_6d,m65c02_6e,m65c02_6f,
	m65c02_70,m65c02_71,m65c02_72,m65c02_73,m65c02_74,m65c02_75,m65c02_76,m65c02_77,
	m65c02_78,m65c02_79,m65c02_7a,m65c02_7b,m65c02_7c,m65c02_7d,m65c02_7e,m65c02_7f,
	m65c02_80,m65c02_81,m65c02_82,m65c02_83,m65c02_84,m65c02_85,m65c02_86,m65c02_87,
	m65c02_88,m65c02_89,m65c02_8a,m65c02_8b,m65c02_8c,m65c02_8d,m65c02_8e,m65c02_8f,
	m65c02_90,m65c02_91,m65c02_92,m65c02_93,m65c02_94,m65c02_95,m65c02_96,m65c02_97,
	m65c02_98,m65c02_99,m65c02_9a,m65c02_9b,m65c02_9c,m65c02_9d,m65c02_9e,m65c02_9f,
	m65c02_a0,m65c02_a1,m65c02_a2,m65c02_a3,m65c02_a4,m65c02_a5,m65c02_a6,m65c02_a7,
	m65c02_a8,m65c02_a9,m65c02_aa,m65c02_ab,m65c02_ac,m65c02_ad,m65c02_ae,m65c02_af,
	m65c02_b0,m65c02_b1,m65c02_b2,m65c02_b3,m65c02_b4,m65c02_b5,m65c02_b6,m65c02_b7,
	m65c02_b8,m65c02_b9,m65c02_ba,m65c02_bb,m65c02_bc,m65c02_bd,m65c02_be,m65c02_bf,
	m65c02_c0,m65c02_c1,m65c02_c2,m65c02_c3,m65c02_c4,m65c02_c5,m65c02_c6,m65c02_c7,
	m65c02_c8,m65c02_c9,m65c02_ca,m65c02_cb,m65c02_cc,m65c02_cd,m65c02_ce,m65c02_cf,
	m65c02_d0,m65c02_d1,m65c02_d2,m65c02_d3,m65c02_d4,m65c02_d5,m65c02_d6,m65c02_d7,
	m65c02_d8,m65c02_d9,m65c02_da,m65c02_db,m65c02_dc,m65c02_dd,m65c02_de,m65c02_df,
	m65c02_e0,m65c02_e1,m65c02_e2,m65c02_e3,m65c02_e4,m65c02_e5,m65c02_e6,m65c02_e7,
	m65c02_e8,m65c02_e9,m65c02_ea,m65c02_eb,m65c02_ec,m65c02_ed,m65c02_ee,m65c02_ef,
	m65c02_f0,m65c02_f1,m65c02_f2,m65c02_f3,m65c02_f4,m65c02_f5,m65c02_f6,m65c02_f7,
	m65c02_f8,m65c02_f9,m65c02_fa,m65c02_fb,m65c02_fc,m65c02_fd,m65c02_fe,m65c02_ff
    };
    
    /*public abstract interface opcode {
        public abstract void handler();
    }*/
    
}
