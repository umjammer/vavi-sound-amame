package mess056.machine;

import static arcadeflex056.fucPtr.*;


public class riotH {
    
    public static int MAX_RIOTS   = 4;


    public static class RIOTinterface {
            public int num_chips;
            public int[] baseclock = new int[MAX_RIOTS];
            public ReadHandlerPtr[] port_a_r = new ReadHandlerPtr[MAX_RIOTS];
            public ReadHandlerPtr[] port_b_r = new ReadHandlerPtr[MAX_RIOTS];
            public WriteHandlerPtr[] port_a_w= new WriteHandlerPtr[MAX_RIOTS];
            public WriteHandlerPtr[] port_b_w= new WriteHandlerPtr[MAX_RIOTS];
            public ReadHandlerPtr[] irq_callback = new ReadHandlerPtr[MAX_RIOTS];
            
            public RIOTinterface(   int num_chips, int[] baseclock,
                                    ReadHandlerPtr[] port_a_r, ReadHandlerPtr[] port_b_r,
                                    WriteHandlerPtr[] port_a_w, WriteHandlerPtr[] port_b_w,
                                    ReadHandlerPtr[] irq_callback){
                
                this.num_chips = num_chips;
                this.baseclock = baseclock;
                this.port_a_r = port_a_r;
                this.port_b_r = port_b_r;
                this.port_a_w= port_a_w;
                this.port_b_w= port_b_w;
                this.irq_callback = irq_callback;
                
            }
    };
    
}
