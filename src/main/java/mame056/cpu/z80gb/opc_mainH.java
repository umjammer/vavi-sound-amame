package mame056.cpu.z80gb;

import static mame056.cpu.z80gb.z80gb.*;

public class opc_mainH {
    
    public static void SBC_A_X(int x)
    {
      int r1,r2;
      int f;
      r1=((Regs.A&0xF)-((x)&0xF)-((Regs.F&FLAG_C)!=0?1:0))&0xffff;
      r2=(Regs.A-(x)-((Regs.F&FLAG_C)!=0?1:0))&0xffff;
      Regs.A=r2&0xff;
      if( (r2&0xff)==0 ) f=FLAG_N|FLAG_Z;
        else f=FLAG_N;
      if( r2>0xFF ) f|=FLAG_C;
      if( r1>0xF )  f|=FLAG_H;
      Regs.F=f;
    }
    
}
