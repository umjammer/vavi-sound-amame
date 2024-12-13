/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mess056.includes;

import static arcadeflex056.fucPtr.*;

public class cia6526H {
/**********************************************************************
	Metal Oxid Semiconductor / Commodore Business Machines
        Complex Interface Adapter 6526

	based on 6522via emulation
**********************************************************************/

    public static int MAX_CIA = 8;

    public static class cia6526_interface
    {
            public ReadHandlerPtr in_a_func;
            public ReadHandlerPtr in_b_func;
            public WriteHandlerPtr out_a_func;
            public WriteHandlerPtr out_b_func;
            public WriteHandlerPtr out_pc_func;
            public ReadHandlerPtr in_sp_func;
            public ReadHandlerPtr out_sp_func;
            public ReadHandlerPtr in_cnt_func;
            public ReadHandlerPtr out_cnt_func;
            public ReadHandlerPtr irq_func;
            public int a_pullup, b_pullup;
            public int todin50hz;
            
            public cia6526_interface(ReadHandlerPtr in_a_func, ReadHandlerPtr in_b_func, WriteHandlerPtr out_a_func, WriteHandlerPtr out_b_func, 
                    WriteHandlerPtr out_pc_func, ReadHandlerPtr in_sp_func, ReadHandlerPtr out_sp_func, ReadHandlerPtr in_cnt_func, ReadHandlerPtr out_cnt_func,
                    ReadHandlerPtr irq_func, int a_pullup, int b_pullup, int todin50hz){
                
                this.in_a_func=in_a_func;
                this.in_b_func=in_b_func;
                this.out_a_func=out_a_func;
                this.out_b_func=out_b_func;
                this.out_pc_func=out_pc_func;
                this.in_sp_func=in_sp_func;
                this.out_sp_func=out_sp_func;
                this.in_cnt_func=in_cnt_func;
                this.out_cnt_func=out_cnt_func;
                this.irq_func=irq_func;
                this.a_pullup=a_pullup;
                this.b_pullup=b_pullup;
                this.todin50hz=todin50hz;
            }
    };
    
}
