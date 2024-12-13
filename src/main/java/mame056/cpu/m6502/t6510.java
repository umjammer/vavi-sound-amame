/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.ill02H.*;

public class t6510 {
    
    static opcode m6510_00 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            BRK();
        }
    };
    
    static opcode m6510_20 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            JSR();
        }
    };
    
    static opcode m6510_40 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTI();
        }
    };
    
    static opcode m6510_60 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTS();
        }
    };
    
    //OP(80) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_80 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    static opcode m6510_a0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDY(tmp);
        }
    };
    
    static opcode m6510_c0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPY(tmp);
        }
    };
    
    static opcode m6510_e0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPX(tmp);
        }
    };
    
    //#define m6510_10 m6502_10									/* 2 BPL */
    static opcode m6510_10 = new opcode() {
        public void handler() {
            BPL();
        }
    };
    
    //#define m6510_30 m6502_30									/* 2 BMI */
    static opcode m6510_30 = new opcode() {
        public void handler() {
            BMI();
        }
    };
    
    //#define m6510_50 m6502_50									/* 2 BVC */
    static opcode m6510_50 = new opcode() {
        public void handler() {
            BVC();
        }
    };
    
    //#define m6510_70 m6502_70									/* 2 BVS */
    static opcode m6510_70 = new opcode() {
        public void handler() {
            BVS();
        }
    };
    
    //#define m6510_90 m6502_90									/* 2 BCC */
    static opcode m6510_90 = new opcode() {
        public void handler() {
            BCC();
        }
    };
    
    //#define m6510_b0 m6502_b0									/* 2 BCS */
    static opcode m6510_b0 = new opcode() {
        public void handler() {
            BCS();
        }
    };
    
    //#define m6510_d0 m6502_d0									/* 2 BNE */
    static opcode m6510_d0 = new opcode() {
        public void handler() {
            BNE();
        }
    };
    
    //#define m6510_f0 m6502_f0									/* 2 BEQ */
    static opcode m6510_f0 = new opcode() {
        public void handler() {
            BEQ();
        }
    };

    //#define m6510_01 m6502_01									/* 6 ORA IDX */
    static opcode m6510_01 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ORA(tmp);
        }
    };
    
    //#define m6510_21 m6502_21									/* 6 AND IDX */
    static opcode m6510_21 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            AND(tmp);
        }
    };
    
    //#define m6510_41 m6502_41									/* 6 EOR IDX */
    static opcode m6510_41 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            EOR(tmp);
        }
    };
    
    //#define m6510_61 m6502_61									/* 6 ADC IDX */
    static opcode m6510_61 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ADC(tmp);
        }
    };
    
    //#define m6510_81 m6502_81									/* 6 STA IDX */
    static opcode m6510_81 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDX(tmp);
        }
    };
    
    //#define m6510_a1 m6502_a1									/* 6 LDA IDX */
    static opcode m6510_a1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            LDA(tmp);
        }
    };
    
    //#define m6510_c1 m6502_c1									/* 6 CMP IDX */
    static opcode m6510_c1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            CMP(tmp);
        }
    };
    
    //#define m6510_e1 m6502_e1									/* 6 SBC IDX */
    static opcode m6510_e1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            SBC(tmp);
        }
    };

    //#define m6510_11 m6502_11									/* 5 ORA IDY */
    static opcode m6510_11 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ORA(tmp);
        }
    };
    
    //#define m6510_31 m6502_31									/* 5 AND IDY */
    static opcode m6510_31 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            AND(tmp);
        }
    };
    
    //#define m6510_51 m6502_51									/* 5 EOR IDY */
    static opcode m6510_51 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            EOR(tmp);
        }
    };
    
    //#define m6510_71 m6502_71									/* 5 ADC IDY */
    static opcode m6510_71 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ADC(tmp);
        }
    };
    
    //#define m6510_91 m6502_91									/* 6 STA IDY */
    static opcode m6510_91 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDY(tmp);
        }
    };
    
    //#define m6510_b1 m6502_b1									/* 5 LDA IDY */
    static opcode m6510_b1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            LDA(tmp);
        }
    };
    
    //#define m6510_d1 m6502_d1									/* 5 CMP IDY */
    static opcode m6510_d1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            CMP(tmp);
        }
    };
    
    //#define m6510_f1 m6502_f1									/* 5 SBC IDY */
    static opcode m6510_f1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            SBC(tmp);
        }
    };
    
    //OP(02) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_02 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(22) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_22 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(42) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_42 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(62) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_62 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    static void DOP(){
        m6502.pc.AddD(1);
    }
    
    //OP(82) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_82 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //#define m6510_a2 m6502_a2									/* 2 LDX IMM */
    static opcode m6510_a2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDX(tmp);
        }
    };
    //OP(c2) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_c2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //OP(e2) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_e2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    

    //OP(12) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_12 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(32) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_32 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(52) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_52 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(72) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_72 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(92) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_92 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(b2) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_b2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(d2) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_d2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    
    //OP(f2) {		  m6502_ICount -= 2;		 KIL;		  } /* 2 KIL */
    static opcode m6510_f2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 KIL();
        }
    };
    

    //OP(03) { int tmp; m6502_ICount -= 7; RD_IDX; SLO; WB_EA;  } /* 7 SLO IDX */
    static opcode m6510_03 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_IDX(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(23) { int tmp; m6502_ICount -= 7; RD_IDX; RLA; WB_EA;  } /* 7 RLA IDX */
    static opcode m6510_23 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_IDX(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(43) { int tmp; m6502_ICount -= 7; RD_IDX; SRE; WB_EA;  } /* 7 SRE IDX */
    static opcode m6510_43 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_IDX(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(63) { int tmp; m6502_ICount -= 7; RD_IDX; RRA; WB_EA;  } /* 7 RRA IDX */
    static opcode m6510_63 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_IDX(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(83) { int tmp; m6502_ICount -= 6;		 SAX; WR_IDX; } /* 6 SAX IDX */
    static opcode m6510_83 = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 6;		 int tmp2=SAX(tmp); WR_IDX(tmp2);
        }
    };
    
    //OP(a3) { int tmp; m6502_ICount -= 6; RD_IDX; LAX;		  } /* 6 LAX IDX */
    static opcode m6510_a3 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDX(); LAX(tmp);
        }
    };
    
    //OP(c3) { int tmp; m6502_ICount -= 7; RD_IDX; DCP; WB_EA;  } /* 7 DCP IDX */
    static opcode m6510_c3 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_IDX(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(e3) { int tmp; m6502_ICount -= 7; RD_IDX; ISB; WB_EA;  } /* 7 ISB IDX */
    static opcode m6510_e3 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_IDX(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(13) { int tmp; m6502_ICount -= 6; RD_IDY; SLO; WB_EA;  } /* 6 SLO IDY */
    static opcode m6510_13 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDY(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(33) { int tmp; m6502_ICount -= 6; RD_IDY; RLA; WB_EA;  } /* 6 RLA IDY */
    static opcode m6510_33 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDY(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(53) { int tmp; m6502_ICount -= 6; RD_IDY; SRE; WB_EA;  } /* 6 SRE IDY */
    static opcode m6510_53 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDY(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(73) { int tmp; m6502_ICount -= 6; RD_IDY; RRA; WB_EA;  } /* 6 RRA IDY */
    static opcode m6510_73 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDY(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(93) { int tmp; m6502_ICount -= 5; EA_IDY; SAH; WB_EA;  } /* 5 SAH IDY */
    static opcode m6510_93 = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 5; EA_IDY(); int tmp2=SAH(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(b3) { int tmp; m6502_ICount -= 5; RD_IDY; LAX;		  } /* 5 LAX IDY */
    static opcode m6510_b3 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_IDY(); LAX(tmp);
        }
    };
    
    //OP(d3) { int tmp; m6502_ICount -= 6; RD_IDY; DCP; WB_EA;  } /* 6 DCP IDY */
    static opcode m6510_d3 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDY(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(f3) { int tmp; m6502_ICount -= 6; RD_IDY; ISB; WB_EA;  } /* 6 ISB IDY */
    static opcode m6510_f3 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_IDY(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(04) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_04 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    
    //#define m6510_24 m6502_24									/* 3 BIT ZPG */
    static opcode m6510_24 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            BIT(tmp);
        }
    };
    
    //OP(44) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_44 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //OP(64) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_64 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //#define m6510_84 m6502_84									/* 3 STY ZPG */
    static opcode m6510_84 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STY();
            WR_ZPG(tmp);
        }
    };
    
    //#define m6510_a4 m6502_a4									/* 3 LDY ZPG */
    static opcode m6510_a4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDY(tmp);
        }
    };
    
    //#define m6510_c4 m6502_c4									/* 3 CPY ZPG */
    static opcode m6510_c4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPY(tmp);
        }
    };
    
    //#define m6510_e4 m6502_e4									/* 3 CPX ZPG */
    static opcode m6510_e4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPX(tmp);
        }
    };

    //OP(14) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_14 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //OP(34) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_34 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //OP(54) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_54 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
   // OP(74) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_74 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    
    //#define m6510_94 m6502_94									/* 4 STY ZP_X */
    static opcode m6510_94 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ZPX(tmp);
        }
    };
    
    //#define m6510_b4 m6502_b4									/* 4 LDY ZP_X */
    static opcode m6510_b4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDY(tmp);
        }
    };
    
    //OP(d4) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_d4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //OP(f4) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_f4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    

    //#define m6510_05 m6502_05									/* 3 ORA ZPG */
    static opcode m6510_05 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ORA(tmp);
        }
    };
    
    //#define m6510_25 m6502_25									/* 3 AND ZPG */
    static opcode m6510_25 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            AND(tmp);
        }
    };
    
    //#define m6510_45 m6502_45									/* 3 EOR ZPG */
    static opcode m6510_45 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            EOR(tmp);
        }
    };
    
    //#define m6510_65 m6502_65									/* 3 ADC ZPG */
    static opcode m6510_65 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ADC(tmp);
        }
    };
    
    //#define m6510_85 m6502_85									/* 3 STA ZPG */
    static opcode m6510_85 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STA();
            WR_ZPG(tmp);
        }
    };
    
    //#define m6510_a5 m6502_a5									/* 3 LDA ZPG */
    static opcode m6510_a5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDA(tmp);
        }
    };
    
    //#define m6510_c5 m6502_c5									/* 3 CMP ZPG */
    static opcode m6510_c5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CMP(tmp);
        }
    };
    
    //#define m6510_e5 m6502_e5									/* 3 SBC ZPG */
    static opcode m6510_e5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            SBC(tmp);
        }
    };
    

    //#define m6510_15 m6502_15									/* 4 ORA ZPX */
    static opcode m6510_15 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ORA(tmp);
        }
    };
    
    //#define m6510_35 m6502_35									/* 4 AND ZPX */
    static opcode m6510_35 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            AND(tmp);
        }
    };
    
    //#define m6510_55 m6502_55									/* 4 EOR ZPX */
    static opcode m6510_55 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            EOR(tmp);
        }
    };
    
    //#define m6510_75 m6502_75									/* 4 ADC ZPX */
    static opcode m6510_75 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ADC(tmp);
        }
    };
    
    //#define m6510_95 m6502_95									/* 4 STA ZPX */
    static opcode m6510_95 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ZPX(tmp);
        }
    };
    
    //#define m6510_b5 m6502_b5									/* 4 LDA ZPX */
    static opcode m6510_b5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDA(tmp);
        }
    };
    
    //#define m6510_d5 m6502_d5									/* 4 CMP ZPX */
    static opcode m6510_d5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            CMP(tmp);
        }
    };
    
    //#define m6510_f5 m6502_f5									/* 4 SBC ZPX */
    static opcode m6510_f5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            SBC(tmp);
        }
    };
    

    //OP(06) { int tmp; m6502_ICount -= 5; RD_ZPG; WB_EA; ASL; WB_EA;  } /* 5 ASL ZPG */
    static opcode m6510_06 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); WB_EA(tmp); int tmp2=ASL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(26) { int tmp; m6502_ICount -= 5; RD_ZPG; WB_EA; ROL; WB_EA;  } /* 5 ROL ZPG */
    static opcode m6510_26 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); WB_EA(tmp); int tmp2=ROL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(46) { int tmp; m6502_ICount -= 5; RD_ZPG; WB_EA; LSR; WB_EA;  } /* 5 LSR ZPG */
    static opcode m6510_46 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); WB_EA(tmp); int tmp2=LSR(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(66) { int tmp; m6502_ICount -= 5; RD_ZPG; WB_EA; ROR; WB_EA;  } /* 5 ROR ZPG */
    static opcode m6510_66 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); WB_EA(tmp); int tmp2=ROR(tmp); WB_EA(tmp2);
        }
    };
    
    //#define m6510_86 m6502_86									/* 3 STX ZPG */
    static opcode m6510_86 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STX();
            WR_ZPG(tmp);
        }
    };
    
    //#define m6510_a6 m6502_a6									/* 3 LDX ZPG */
    static opcode m6510_a6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDX(tmp);
        }
    };
    
    //OP(c6) { int tmp; m6502_ICount -= 5; RD_ZPG; WB_EA; DEC; WB_EA;  } /* 5 DEC ZPG */
    static opcode m6510_c6 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); WB_EA(tmp); int tmp2=DEC(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(e6) { int tmp; m6502_ICount -= 5; RD_ZPG; WB_EA; INC; WB_EA;  } /* 5 INC ZPG */
    static opcode m6510_e6 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); WB_EA(tmp); int tmp2=INC(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(16) { int tmp; m6502_ICount -= 6; RD_ZPX; WB_EA; ASL; WB_EA;  } /* 6 ASL ZPX */
    static opcode m6510_16 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); WB_EA(tmp); int tmp2=ASL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(36) { int tmp; m6502_ICount -= 6; RD_ZPX; WB_EA; ROL; WB_EA;  } /* 6 ROL ZPX */
    static opcode m6510_36 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); WB_EA(tmp); int tmp2=ROL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(56) { int tmp; m6502_ICount -= 6; RD_ZPX; WB_EA; LSR; WB_EA;  } /* 6 LSR ZPX */
    static opcode m6510_56 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); WB_EA(tmp); int tmp2=LSR(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(76) { int tmp; m6502_ICount -= 6; RD_ZPX; WB_EA; ROR; WB_EA;  } /* 6 ROR ZPX */
    static opcode m6510_76 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); WB_EA(tmp); int tmp2=ROR(tmp); WB_EA(tmp2);
        }
    };
    
    //#define m6510_96 m6502_96									/* 4 STX ZPY */
    static opcode m6510_96 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STX();
            WR_ZPY(tmp);
        }
    };
    
    //#define m6510_b6 m6502_b6									/* 4 LDX ZPY */
    static opcode m6510_b6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPY();
            LDX(tmp);
        }
    };
    
    //OP(d6) { int tmp; m6502_ICount -= 6; RD_ZPX; WB_EA; DEC; WB_EA;  } /* 6 DEC ZPX */
    static opcode m6510_d6 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); WB_EA(tmp); int tmp2=DEC(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(f6) { int tmp; m6502_ICount -= 6; RD_ZPX; WB_EA; INC; WB_EA;  } /* 6 INC ZPX */
    static opcode m6510_f6 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); WB_EA(tmp); int tmp2=INC(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(07) { int tmp; m6502_ICount -= 5; RD_ZPG; SLO; WB_EA;  } /* 5 SLO ZPG */
    static opcode m6510_07 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(27) { int tmp; m6502_ICount -= 5; RD_ZPG; RLA; WB_EA;  } /* 5 RLA ZPG */
    static opcode m6510_27 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(47) { int tmp; m6502_ICount -= 5; RD_ZPG; SRE; WB_EA;  } /* 5 SRE ZPG */
    static opcode m6510_47 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(67) { int tmp; m6502_ICount -= 5; RD_ZPG; RRA; WB_EA;  } /* 5 RRA ZPG */
    static opcode m6510_67 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(87) { int tmp; m6502_ICount -= 3;		 SAX; WR_ZPG; } /* 3 SAX ZPG */
    static opcode m6510_87 = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 3;		 int tmp2=SAX(tmp); WR_ZPG(tmp2);
        }
    };
    
    //OP(a7) { int tmp; m6502_ICount -= 3; RD_ZPG; LAX;		  } /* 3 LAX ZPG */
    static opcode m6510_a7 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 3; tmp=RD_ZPG(); LAX(tmp);
        }
    };
    
    //OP(c7) { int tmp; m6502_ICount -= 5; RD_ZPG; DCP; WB_EA;  } /* 5 DCP ZPG */
    static opcode m6510_c7 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(e7) { int tmp; m6502_ICount -= 5; RD_ZPG; ISB; WB_EA;  } /* 5 ISB ZPG */
    static opcode m6510_e7 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ZPG(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(17) { int tmp; m6502_ICount -= 6; RD_ZPX; SLO; WB_EA;  } /* 4 SLO ZPX */
    static opcode m6510_17 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(37) { int tmp; m6502_ICount -= 6; RD_ZPX; RLA; WB_EA;  } /* 4 RLA ZPX */
    static opcode m6510_37 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(57) { int tmp; m6502_ICount -= 6; RD_ZPX; SRE; WB_EA;  } /* 4 SRE ZPX */
    static opcode m6510_57 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(77) { int tmp; m6502_ICount -= 6; RD_ZPX; RRA; WB_EA;  } /* 4 RRA ZPX */
    static opcode m6510_77 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(97) { int tmp; m6502_ICount -= 4;		 SAX; WR_ZPY; } /* 4 SAX ZPY */
    static opcode m6510_97 = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 4;		 int tmp2=SAX(tmp); WR_ZPY(tmp2);
        }
    };
    
    //OP(b7) { int tmp; m6502_ICount -= 4; RD_ZPY; LAX;		  } /* 4 LAX ZPY */
    static opcode m6510_b7 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ZPY(); LAX(tmp);
        }
    };
    
    //OP(d7) { int tmp; m6502_ICount -= 6; RD_ZPX; DCP; WB_EA;  } /* 6 DCP ZPX */
    static opcode m6510_d7 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(f7) { int tmp; m6502_ICount -= 6; RD_ZPX; ISB; WB_EA;  } /* 6 ISB ZPX */
    static opcode m6510_f7 = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ZPX(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    //#define m6510_08 m6502_08									/* 2 PHP */
    static opcode m6510_08 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHP();
        }
    };
    
    //#define m6510_28 m6502_28									/* 2 PLP */
    static opcode m6510_28 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLP();
        }
    };
    
    //#define m6510_48 m6502_48									/* 2 PHA */
    static opcode m6510_48 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHA();
        }
    };
    
    //#define m6510_68 m6502_68									/* 2 PLA */
    static opcode m6510_68 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLA();
        }
    };
    
    //#define m6510_88 m6502_88									/* 2 DEY */
    static opcode m6510_88 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEY();
        }
    };
    
    //#define m6510_a8 m6502_a8									/* 2 TAY */
    static opcode m6510_a8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAY();
        }
    };
    
    //#define m6510_c8 m6502_c8									/* 2 INY */
    static opcode m6510_c8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INY();
        }
    };
    
    //#define m6510_e8 m6502_e8									/* 2 INX */
    static opcode m6510_e8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INX();
        }
    };
    

    //#define m6510_18 m6502_18									/* 2 CLC */
    static opcode m6510_18 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLC();
        }
    };
    
    //#define m6510_38 m6502_38									/* 2 SEC */
    static opcode m6510_38 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEC();
        }
    };
    
    //#define m6510_58 m6502_58									/* 2 CLI */
    static opcode m6510_58 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLI();
        }
    };
    
    //#define m6510_78 m6502_78									/* 2 SEI */
    static opcode m6510_78 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEI();
        }
    };
    
    //#define m6510_98 m6502_98									/* 2 TYA */
    static opcode m6510_98 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TYA();
        }
    };
    
    //#define m6510_b8 m6502_b8									/* 2 CLV */
    static opcode m6510_b8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLV();
        }
    };
    
    //#define m6510_d8 m6502_d8									/* 2 CLD */
    static opcode m6510_d8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLD();
        }
    };
    
    //#define m6510_f8 m6502_f8									/* 2 SED */
    static opcode m6510_f8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SED();
        }
    };
    

    //#define m6510_09 m6502_09									/* 2 ORA IMM */
    static opcode m6510_09 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ORA(tmp);
        }
    };
    
    //#define m6510_29 m6502_29									/* 2 AND IMM */
    static opcode m6510_29 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            AND(tmp);
        }
    };
    
    //#define m6510_49 m6502_49									/* 2 EOR IMM */
    static opcode m6510_49 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            EOR(tmp);
        }
    };
    
    //#define m6510_69 m6502_69									/* 2 ADC IMM */
    static opcode m6510_69 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ADC(tmp);
        }
    };
    
    //OP(89) {		  m6502_ICount -= 2;		 DOP;		  } /* 2 DOP */
    static opcode m6510_89 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 DOP();
        }
    };
    
    //#define m6510_a9 m6502_a9									/* 2 LDA IMM */
    static opcode m6510_a9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDA(tmp);
        }
    };
    
    //#define m6510_c9 m6502_c9									/* 2 CMP IMM */
    static opcode m6510_c9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CMP(tmp);
        }
    };
    
    //#define m6510_e9 m6502_e9									/* 2 SBC IMM */
    static opcode m6510_e9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            SBC(tmp);
        }
    };
    

    //#define m6510_19 m6502_19									/* 4 ORA ABY */
    static opcode m6510_19 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ORA(tmp);
        }
    };
    
    //#define m6510_39 m6502_39									/* 4 AND ABY */
    static opcode m6510_39 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            AND(tmp);
        }
    };
    
    //#define m6510_59 m6502_59									/* 4 EOR ABY */
    static opcode m6510_59 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            EOR(tmp);
        }
    };
    
    //#define m6510_79 m6502_79									/* 4 ADC ABY */
    static opcode m6510_79 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ADC(tmp);
        }
    };
    
    //#define m6510_99 m6502_99									/* 5 STA ABY */
    static opcode m6510_99 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABY(tmp);
        }
    };
    
    //#define m6510_b9 m6502_b9									/* 4 LDA ABY */
    static opcode m6510_b9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDA(tmp);
        }
    };
    
    //#define m6510_d9 m6502_d9									/* 4 CMP ABY */
    static opcode m6510_d9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            CMP(tmp);
        }
    };
    
    //#define m6510_f9 m6502_f9									/* 4 SBC ABY */
    static opcode m6510_f9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            SBC(tmp);
        }
    };
    

    //#define m6510_0a m6502_0a									/* 2 ASL A */
    static opcode m6510_0a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ASL(tmp);
            WB_ACC(tmp2);
        }
    };
    
    //#define m6510_2a m6502_2a									/* 2 ROL A */
    static opcode m6510_2a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROL(tmp);
            WB_ACC(tmp2);
        }
    };
    
    //#define m6510_4a m6502_4a									/* 2 LSR A */
    static opcode m6510_4a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = LSR(tmp);
            WB_ACC(tmp2);
        }
    };
    
    //#define m6510_6a m6502_6a									/* 2 ROR A */
    static opcode m6510_6a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROR(tmp);
            WB_ACC(tmp2);
        }
    };
    
    //#define m6510_8a m6502_8a									/* 2 TXA */
    static opcode m6510_8a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXA();
        }
    };
    
    //#define m6510_aa m6502_aa									/* 2 TAX */
    static opcode m6510_aa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAX();
        }
    };
    
    //#define m6510_ca m6502_ca									/* 2 DEX */
    static opcode m6510_ca = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEX();
        }
    };
    
    //#define m6510_ea m6502_ea									/* 2 NOP */
    static opcode m6510_ea = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            NOP();
        }
    };
    

    //OP(1a) {		  m6502_ICount -= 2;		 NOP;		  } /* 2 NOP */
    static opcode m6510_1a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 NOP();
        }
    };
    
    //OP(3a) {		  m6502_ICount -= 2;		 NOP;		  } /* 2 NOP */
    static opcode m6510_3a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 NOP();
        }
    };
    
    //OP(5a) {		  m6502_ICount -= 2;		 NOP;		  } /* 2 NOP */
    static opcode m6510_5a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 NOP();
        }
    };
    
    //OP(7a) {		  m6502_ICount -= 2;		 NOP;		  } /* 2 NOP */
    static opcode m6510_7a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 NOP();
        }
    };
    
    //#define m6510_9a m6502_9a									/* 2 TXS */
    static opcode m6510_9a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXS();
        }
    };
    
    //#define m6510_ba m6502_ba									/* 2 TSX */
    static opcode m6510_ba = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TSX();
        }
    };
    
    //OP(da) {		  m6502_ICount -= 2;		 NOP;		  } /* 2 NOP */
    static opcode m6510_da = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 NOP();
        }
    };
    
    //OP(fa) {		  m6502_ICount -= 2;		 NOP;		  } /* 2 NOP */
    static opcode m6510_fa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 NOP();
        }
    };
    

    //OP(0b) { int tmp; m6502_ICount -= 2; RD_IMM; ANC;		  } /* 2 ANC IMM */
    static opcode m6510_0b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); ANC(tmp);
        }
    };
    
    //OP(2b) { int tmp; m6502_ICount -= 2; RD_IMM; ANC;		  } /* 2 ANC IMM */
    static opcode m6510_2b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); ANC(tmp);
        }
    };
    
    //OP(4b) { int tmp; m6502_ICount -= 2; RD_IMM; ASR; WB_ACC; } /* 2 ASR IMM */
    static opcode m6510_4b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); int tmp2=ASR(tmp); WB_ACC(tmp2);
        }
    };
    
    //OP(6b) { int tmp; m6502_ICount -= 2; RD_IMM; ARR; WB_ACC; } /* 2 ARR IMM */
    static opcode m6510_6b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); int tmp2=ARR(tmp); WB_ACC(tmp2);
        }
    };
    
    //OP(8b) { int tmp; m6502_ICount -= 2; RD_IMM; AXA;         } /* 2 AXA IMM */
    static opcode m6510_8b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); AXA(tmp);
        }
    };
    
    //OP(ab) { int tmp; m6502_ICount -= 2; RD_IMM; OAL;         } /* 2 OAL IMM */
    static opcode m6510_ab = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); OAL(tmp);
        }
    };
    
    //OP(cb) { int tmp; m6502_ICount -= 2; RD_IMM; ASX;		  } /* 2 ASX IMM */
    static opcode m6510_cb = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); ASX(tmp);
        }
    };
    
    //OP(eb) { int tmp; m6502_ICount -= 2; RD_IMM; SBC;		  } /* 2 SBC IMM */
    static opcode m6510_eb = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 2; tmp=RD_IMM(); SBC(tmp);
        }
    };
    

    //OP(1b) { int tmp; m6502_ICount -= 4; RD_ABY; SLO; WB_EA;  } /* 4 SLO ABY */
    static opcode m6510_1b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABY(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(3b) { int tmp; m6502_ICount -= 4; RD_ABY; RLA; WB_EA;  } /* 4 RLA ABY */
    static opcode m6510_3b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABY(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(5b) { int tmp; m6502_ICount -= 4; RD_ABY; SRE; WB_EA;  } /* 4 SRE ABY */
    static opcode m6510_5b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABY(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(7b) { int tmp; m6502_ICount -= 4; RD_ABY; RRA; WB_EA;  } /* 4 RRA ABY */
    static opcode m6510_7b = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABY(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(9b) { int tmp; m6502_ICount -= 5; EA_ABY; SSH; WB_EA;  } /* 5 SSH ABY */
    static opcode m6510_9b = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 5; EA_ABY(); int tmp2=SSH(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(bb) { int tmp; m6502_ICount -= 4; RD_ABY; AST;		  } /* 4 AST ABY */
    static opcode m6510_bb = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABY(); AST(tmp);
        }
    };
    
    //OP(db) { int tmp; m6502_ICount -= 6; RD_ABY; DCP; WB_EA;  } /* 6 DCP ABY */
    static opcode m6510_db = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABY(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(fb) { int tmp; m6502_ICount -= 6; RD_ABY; ISB; WB_EA;  } /* 6 ISB ABY */
    static opcode m6510_fb = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABY(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(0c) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_0c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    
    //#define m6510_2c m6502_2c									/* 4 BIT ABS */
    static opcode m6510_2c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            BIT(tmp);
        }
    };
    
    //#define m6510_4c m6502_4c									/* 3 JMP ABS */
    static opcode m6510_4c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            EA_ABS();
            JMP();
        }
    };
    
    //#define m6510_6c m6502_6c									/* 5 JMP IND */
    static opcode m6510_6c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            EA_IND();
            JMP();
        }
    };
    
    //#define m6510_8c m6502_8c									/* 4 STY ABS */
    static opcode m6510_8c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ABS(tmp);
        }
    };
    
    //#define m6510_ac m6502_ac									/* 4 LDY ABS */
    static opcode m6510_ac = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDY(tmp);
        }
    };
    
    //#define m6510_cc m6502_cc									/* 4 CPY ABS */
    static opcode m6510_cc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPY(tmp);
        }
    };
    
    //#define m6510_ec m6502_ec									/* 4 CPX ABS */
    static opcode m6510_ec = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPX(tmp);
        }
    };
    

    //OP(1c) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_1c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    
    //OP(3c) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_3c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    
    //OP(5c) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_5c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    
    //OP(7c) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_7c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    
    //OP(9c) { int tmp; m6502_ICount -= 5; EA_ABX; SYH; WB_EA;  } /* 5 SYH ABX */
    static opcode m6510_9c = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 5; EA_ABX(); int tmp2=SYH(tmp); WB_EA(tmp2);
        }
    };
    
    //#define m6510_bc m6502_bc									/* 4 LDY ABX */
    static opcode m6510_bc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDY(tmp);
        }
    };
    
    //OP(dc) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_dc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    
    //OP(fc) {		  m6502_ICount -= 2;		 TOP;		  } /* 2 TOP */
    static opcode m6510_fc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;		 TOP();
        }
    };
    

    //#define m6510_0d m6502_0d									/* 4 ORA ABS */
    static opcode m6510_0d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ORA(tmp);
        }
    };
    
    //#define m6510_2d m6502_2d									/* 4 AND ABS */
    static opcode m6510_2d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            AND(tmp);
        }
    };
    
    //#define m6510_4d m6502_4d									/* 4 EOR ABS */
    static opcode m6510_4d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            EOR(tmp);
        }
    };
    
    //#define m6510_6d m6502_6d									/* 4 ADC ABS */
    static opcode m6510_6d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ADC(tmp);
        }
    };
    
    //#define m6510_8d m6502_8d									/* 4 STA ABS */
    static opcode m6510_8d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ABS(tmp);
        }
    };
    
    //#define m6510_ad m6502_ad									/* 4 LDA ABS */
    static opcode m6510_ad = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDA(tmp);
        }
    };
    
    //#define m6510_cd m6502_cd									/* 4 CMP ABS */
    static opcode m6510_cd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CMP(tmp);
        }
    };
    
    //#define m6510_ed m6502_ed									/* 4 SBC ABS */
    static opcode m6510_ed = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            SBC(tmp);
        }
    };
    

    //#define m6510_1d m6502_1d									/* 4 ORA ABX */
    static opcode m6510_1d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ORA(tmp);
        }
    };
    
    //#define m6510_3d m6502_3d									/* 4 AND ABX */
    static opcode m6510_3d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            AND(tmp);
        }
    };
    
    //#define m6510_5d m6502_5d									/* 4 EOR ABX */
    static opcode m6510_5d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            EOR(tmp);
        }
    };
    
    //#define m6510_7d m6502_7d									/* 4 ADC ABX */
    static opcode m6510_7d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ADC(tmp);
        }
    };
    
    //#define m6510_9d m6502_9d									/* 5 STA ABX */
    static opcode m6510_9d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABX(tmp);
        }
    };
    
    //#define m6510_bd m6502_bd									/* 4 LDA ABX */
    static opcode m6510_bd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDA(tmp);
        }
    };
    
    //#define m6510_dd m6502_dd									/* 4 CMP ABX */
    static opcode m6510_dd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            CMP(tmp);
        }
    };
    
    //#define m6510_fd m6502_fd									/* 4 SBC ABX */
    static opcode m6510_fd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            SBC(tmp);
        }
    };
    


    //OP(0e) { int tmp; m6502_ICount -= 6; RD_ABS; WB_EA; ASL; WB_EA;  } /* 6 ASL ABS */
    static opcode m6510_0e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); WB_EA(tmp); int tmp2=ASL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(2e) { int tmp; m6502_ICount -= 6; RD_ABS; WB_EA; ROL; WB_EA;  } /* 6 ROL ABS */
    static opcode m6510_2e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); WB_EA(tmp); int tmp2=ROL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(4e) { int tmp; m6502_ICount -= 6; RD_ABS; WB_EA; LSR; WB_EA;  } /* 6 LSR ABS */
    static opcode m6510_4e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); WB_EA(tmp); int tmp2=LSR(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(6e) { int tmp; m6502_ICount -= 6; RD_ABS; WB_EA; ROR; WB_EA;  } /* 6 ROR ABS */
    static opcode m6510_6e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); WB_EA(tmp); int tmp2=ROR(tmp); WB_EA(tmp2);
        }
    };
    
    //#define m6510_8e m6502_8e									/* 5 STX ABS */
    static opcode m6510_8e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STX();
            WR_ABS(tmp);
        }
    };
    
    //#define m6510_ae m6502_ae									/* 4 LDX ABS */
    static opcode m6510_ae = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDX(tmp);
        }
    };
    
    //OP(ce) { int tmp; m6502_ICount -= 6; RD_ABS; WB_EA; DEC; WB_EA;  } /* 6 DEC ABS */
    static opcode m6510_ce = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); WB_EA(tmp); int tmp2=DEC(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(ee) { int tmp; m6502_ICount -= 6; RD_ABS; WB_EA; INC; WB_EA;  } /* 6 INC ABS */
    static opcode m6510_ee = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); WB_EA(tmp); int tmp2=INC(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(1e) { int tmp; m6502_ICount -= 7; RD_ABX; WB_EA; ASL; WB_EA;  } /* 7 ASL ABX */
    static opcode m6510_1e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); WB_EA(tmp); int tmp2=ASL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(3e) { int tmp; m6502_ICount -= 7; RD_ABX; WB_EA; ROL; WB_EA;  } /* 7 ROL ABX */
    static opcode m6510_3e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); WB_EA(tmp); int tmp2=ROL(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(5e) { int tmp; m6502_ICount -= 7; RD_ABX; WB_EA; LSR; WB_EA;  } /* 7 LSR ABX */
    static opcode m6510_5e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); WB_EA(tmp); int tmp2=LSR(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(7e) { int tmp; m6502_ICount -= 7; RD_ABX; WB_EA; ROR; WB_EA;  } /* 7 ROR ABX */
    static opcode m6510_7e = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); WB_EA(tmp); int tmp2=ROR(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(9e) { int tmp; m6502_ICount -= 2; EA_ABY; SXH; WB_EA;  } /* 2 SXH ABY */
    static opcode m6510_9e = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 2; EA_ABY(); int tmp2=SXH(tmp); WB_EA(tmp2);
        }
    };
    
    //#define m6510_be m6502_be									/* 4 LDX ABY */
    static opcode m6510_be = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDX(tmp);
        }
    };
    
    //OP(de) { int tmp; m6502_ICount -= 7; RD_ABX; WB_EA; DEC; WB_EA;  } /* 7 DEC ABX */
    static opcode m6510_de = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); WB_EA(tmp); int tmp2=DEC(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(fe) { int tmp; m6502_ICount -= 7; RD_ABX; WB_EA; INC; WB_EA;  } /* 7 INC ABX */
    static opcode m6510_fe = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); WB_EA(tmp); int tmp2=INC(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(0f) { int tmp; m6502_ICount -= 6; RD_ABS; SLO; WB_EA;  } /* 4 SLO ABS */
    static opcode m6510_0f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(2f) { int tmp; m6502_ICount -= 6; RD_ABS; RLA; WB_EA;  } /* 4 RLA ABS */
    static opcode m6510_2f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(4f) { int tmp; m6502_ICount -= 6; RD_ABS; SRE; WB_EA;  } /* 4 SRE ABS */
    static opcode m6510_4f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(6f) { int tmp; m6502_ICount -= 6; RD_ABS; RRA; WB_EA;  } /* 4 RRA ABS */
    static opcode m6510_6f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(8f) { int tmp; m6502_ICount -= 4;		 SAX; WR_ABS; } /* 4 SAX ABS */
    static opcode m6510_8f = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 4;		 int tmp2=SAX(tmp); WR_ABS(tmp2);
        }
    };
    
    //OP(af) { int tmp; m6502_ICount -= 5; RD_ABS; LAX;		  } /* 4 LAX ABS */
    static opcode m6510_af = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 5; tmp=RD_ABS(); LAX(tmp);
        }
    };
    
    //OP(cf) { int tmp; m6502_ICount -= 6; RD_ABS; DCP; WB_EA;  } /* 6 DCP ABS */
    static opcode m6510_cf = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(ef) { int tmp; m6502_ICount -= 6; RD_ABS; ISB; WB_EA;  } /* 6 ISB ABS */
    static opcode m6510_ef = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABS(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    //OP(1f) { int tmp; m6502_ICount -= 4; RD_ABX; SLO; WB_EA;  } /* 4 SLO ABX */
    static opcode m6510_1f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABX(); int tmp2=SLO(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(3f) { int tmp; m6502_ICount -= 4; RD_ABX; RLA; WB_EA;  } /* 4 RLA ABX */
    static opcode m6510_3f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABX(); int tmp2=RLA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(5f) { int tmp; m6502_ICount -= 4; RD_ABX; SRE; WB_EA;  } /* 4 SRE ABX */
    static opcode m6510_5f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABX(); int tmp2=SRE(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(7f) { int tmp; m6502_ICount -= 4; RD_ABX; RRA; WB_EA;  } /* 4 RRA ABX */
    static opcode m6510_7f = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 4; tmp=RD_ABX(); int tmp2=RRA(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(9f) { int tmp; m6502_ICount -= 6; EA_ABY; SAH; WB_EA;  } /* 5 SAH ABY */
    static opcode m6510_9f = new opcode() {
        public void handler() {
            int tmp=0; m6502_ICount[0] -= 6; EA_ABY(); int tmp2=SAH(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(bf) { int tmp; m6502_ICount -= 6; RD_ABY; LAX;		  } /* 4 LAX ABY */
    static opcode m6510_bf = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 6; tmp=RD_ABY(); LAX(tmp);
        }
    };
    
    //OP(df) { int tmp; m6502_ICount -= 7; RD_ABX; DCP; WB_EA;  } /* 7 DCP ABX */
    static opcode m6510_df = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); int tmp2=DCP(tmp); WB_EA(tmp2);
        }
    };
    
    //OP(ff) { int tmp; m6502_ICount -= 7; RD_ABX; ISB; WB_EA;  } /* 7 ISB ABX */
    static opcode m6510_ff = new opcode() {
        public void handler() {
            int tmp; m6502_ICount[0] -= 7; tmp=RD_ABX(); int tmp2=ISB(tmp); WB_EA(tmp2);
        }
    };
    

    public static opcode[] insn6510 = {
        m6510_00,m6510_01,m6510_02,m6510_03,m6510_04,m6510_05,m6510_06,m6510_07,
	m6510_08,m6510_09,m6510_0a,m6510_0b,m6510_0c,m6510_0d,m6510_0e,m6510_0f,
	m6510_10,m6510_11,m6510_12,m6510_13,m6510_14,m6510_15,m6510_16,m6510_17,
	m6510_18,m6510_19,m6510_1a,m6510_1b,m6510_1c,m6510_1d,m6510_1e,m6510_1f,
	m6510_20,m6510_21,m6510_22,m6510_23,m6510_24,m6510_25,m6510_26,m6510_27,
	m6510_28,m6510_29,m6510_2a,m6510_2b,m6510_2c,m6510_2d,m6510_2e,m6510_2f,
	m6510_30,m6510_31,m6510_32,m6510_33,m6510_34,m6510_35,m6510_36,m6510_37,
	m6510_38,m6510_39,m6510_3a,m6510_3b,m6510_3c,m6510_3d,m6510_3e,m6510_3f,
	m6510_40,m6510_41,m6510_42,m6510_43,m6510_44,m6510_45,m6510_46,m6510_47,
	m6510_48,m6510_49,m6510_4a,m6510_4b,m6510_4c,m6510_4d,m6510_4e,m6510_4f,
	m6510_50,m6510_51,m6510_52,m6510_53,m6510_54,m6510_55,m6510_56,m6510_57,
	m6510_58,m6510_59,m6510_5a,m6510_5b,m6510_5c,m6510_5d,m6510_5e,m6510_5f,
	m6510_60,m6510_61,m6510_62,m6510_63,m6510_64,m6510_65,m6510_66,m6510_67,
	m6510_68,m6510_69,m6510_6a,m6510_6b,m6510_6c,m6510_6d,m6510_6e,m6510_6f,
	m6510_70,m6510_71,m6510_72,m6510_73,m6510_74,m6510_75,m6510_76,m6510_77,
	m6510_78,m6510_79,m6510_7a,m6510_7b,m6510_7c,m6510_7d,m6510_7e,m6510_7f,
	m6510_80,m6510_81,m6510_82,m6510_83,m6510_84,m6510_85,m6510_86,m6510_87,
	m6510_88,m6510_89,m6510_8a,m6510_8b,m6510_8c,m6510_8d,m6510_8e,m6510_8f,
	m6510_90,m6510_91,m6510_92,m6510_93,m6510_94,m6510_95,m6510_96,m6510_97,
	m6510_98,m6510_99,m6510_9a,m6510_9b,m6510_9c,m6510_9d,m6510_9e,m6510_9f,
	m6510_a0,m6510_a1,m6510_a2,m6510_a3,m6510_a4,m6510_a5,m6510_a6,m6510_a7,
	m6510_a8,m6510_a9,m6510_aa,m6510_ab,m6510_ac,m6510_ad,m6510_ae,m6510_af,
	m6510_b0,m6510_b1,m6510_b2,m6510_b3,m6510_b4,m6510_b5,m6510_b6,m6510_b7,
	m6510_b8,m6510_b9,m6510_ba,m6510_bb,m6510_bc,m6510_bd,m6510_be,m6510_bf,
	m6510_c0,m6510_c1,m6510_c2,m6510_c3,m6510_c4,m6510_c5,m6510_c6,m6510_c7,
	m6510_c8,m6510_c9,m6510_ca,m6510_cb,m6510_cc,m6510_cd,m6510_ce,m6510_cf,
	m6510_d0,m6510_d1,m6510_d2,m6510_d3,m6510_d4,m6510_d5,m6510_d6,m6510_d7,
	m6510_d8,m6510_d9,m6510_da,m6510_db,m6510_dc,m6510_dd,m6510_de,m6510_df,
	m6510_e0,m6510_e1,m6510_e2,m6510_e3,m6510_e4,m6510_e5,m6510_e6,m6510_e7,
	m6510_e8,m6510_e9,m6510_ea,m6510_eb,m6510_ec,m6510_ed,m6510_ee,m6510_ef,
	m6510_f0,m6510_f1,m6510_f2,m6510_f3,m6510_f4,m6510_f5,m6510_f6,m6510_f7,
	m6510_f8,m6510_f9,m6510_fa,m6510_fb,m6510_fc,m6510_fd,m6510_fe,m6510_ff
    };
    
    public abstract interface opcode {
        public abstract void handler();
    }
}
