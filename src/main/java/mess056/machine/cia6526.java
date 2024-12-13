/**********************************************************************
	Metal Oxid Semiconductor / Commodore Business Machines
        Complex Interface Adapter 6526

	based on 6522via emulation
**********************************************************************/
/*
 only few tested (for c64)

 state:
 port a,b
 handshake support, flag input, not pc output
 timer a,b
  not counting of external clocks
  not switching port b pins
 interrupt system
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mess056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.includes.cia6526H.*;

public class cia6526
{
	
/*TODO*///#define VERBOSE_DBG 1				   /* general debug messages */
/*TODO*///	
/*TODO*///	
/*TODO*///	/* todin pin 50 or 60 hertz frequency */
	public static int TODIN_50HZ(CIA6526 This){ return (This.cra&0x80); }  /* else 60 Hz */
/*TODO*///	
/*TODO*///	#define TIMER1_B6 (This.cra&2)
/*TODO*///	#define TIMER1_B6_TOGGLE (This.cra&4)	/* else single pulse */
	public static int TIMER1_ONESHOT(CIA6526 This){ return (This.cra&8); } /* else continuous */
	public static int TIMER1_STOP(CIA6526 This){ return (This.cra&1)!=0?0:1; }
	public static int TIMER1_RELOAD(CIA6526 This){ return (This.cra&0x10); }
	public static int TIMER1_COUNT_CNT(CIA6526 This){ return (This.cra&0x20); }	/* else clock 2 input */

	public static int TIMER2_ONESHOT(CIA6526 This){ return (This.crb&8); } /* else continuous */
	public static int TIMER2_STOP(CIA6526 This){ return (This.crb&1)!=0?0:1; }
	public static int TIMER2_RELOAD(CIA6526 This){ return (This.crb&0x10); }
	public static int TIMER2_COUNT_CLOCK(CIA6526 This){ return ((This.crb&0x60)!=0)?0:1; }
/*TODO*///	#define TIMER2_COUNT_CNT ((This.crb&0x60)==0x20)
	public static int TIMER2_COUNT_TIMER1(CIA6526 This){ return ((This.crb&0x60)==0x40) ? 1 : 0; }
	public static int TIMER2_COUNT_TIMER1_CNT(CIA6526 This){ return ((This.crb&0x60)==0x60) ? 1 : 0; }
	
	public static int SERIAL_MODE_OUT(CIA6526 This){ return (This.cra&0x40); }
	public static int TOD_ALARM(CIA6526 This){ return (This.crb&0x80); }   /* else write to tod clock */
	public static void BCD_INC(int v){ if((v&0xf)==9){
                                                v+=0x10-9;
                                            } else { 
                                                v++;
                                            } 
                                            //return v;
                                         }
	
	public static class CIA6526 {
                public int active = 0;
                public int number; /* number of cia, to allow callback generate address */
		public cia6526_interface intf;
	
		public int in_a;
		public int out_a;
		public int ddr_a;
	
		public int in_b;
		public int out_b;
		public int ddr_b;
	
		public int t1c;
		public int t1l;
		public timer_entry timer1;
		public int timer1_state;
	
		public int t2c;
		public int t2l;
		public timer_entry timer2;
		public int timer2_state;
	
		public int tod10ths, todsec, todmin, todhour;
		public int alarm10ths, alarmsec, alarmmin, alarmhour;
	
		public int latch10ths, latchsec, latchmin, latchhour;
		public int todlatched;
	
		int todstopped;
		public timer_entry todtimer;
	
		public int flag;						   /* input */
	
		public int sdr;
		public int cnt,						   /* input or output */
		 sp;							   /* input or output */
		public int serial, shift, loaded;
	
		public int cra, crb;
	
		public int ier;
		public int ifr;
	};
	
	public static CIA6526[] cia;
        
        public static void cia6526_init()
        {
            cia = new CIA6526[MAX_CIA];
            if (cia==null)
                    return;  
            
            for (int i = 0; i < MAX_CIA; i++)
            {
                cia[i] = new CIA6526();
            }
        }

        public static void cia6526_config (int which, cia6526_interface intf)
        {
                if (which >= MAX_CIA)
                        return;
                //memset (cia + which, 0, sizeof (cia[which]));
                cia[which] = new CIA6526();
                cia[which].active = 1;
                cia[which].number = which;
                cia[which].intf = intf;
                /*cia[which].timer1 = new timer_entry();cia[which].timer1.callback=cia_timer1_timeout;
                cia[which].timer2 = new timer_entry();cia[which].timer1.callback=cia_timer2_timeout;
                cia[which].todtimer = new timer_entry();cia[which].timer1.callback=cia_tod_timeout;*/
        }


        /******************* reset *******************/

        public static void cia6526_reset ()
        {
            System.out.println("cia6526_reset");
                int i;

/*TODO*///                assert (((int) cia[0].intf & 3) == 0);

                /* zap each structure, preserving the interface and swizzle */
                for (i = 0; i < MAX_CIA; i++)
                {
                    //System.out.println("cia["+i+"].active="+cia[i].active);
                        if (cia[i].active != 0)
                        {
                                cia6526_interface intf = cia[i].intf;
                                timer_entry timer1 = cia[i].timer1;
                                timer_entry timer2 = cia[i].timer2;
                                timer_entry todtimer = cia[i].todtimer;

                                //memset (&cia[i], 0, sizeof (cia[i]));
                                cia[i] = new CIA6526();
                                cia[i].active = 1;
                                cia[i].number = i;
                                cia[i].intf = intf;
                                cia[i].t1l = 0xffff;
                                cia[i].t2l = 0xffff;
                                cia[i].timer1 = timer1;
                                cia[i].timer2 = timer2;
                                cia[i].todtimer = todtimer;

                                /*if (cia[i].timer1 != null)
                                    timer_reset(cia[i].timer1, TIME_NEVER);
                                if (cia[i].timer2 != null)
                                    timer_reset(cia[i].timer2, TIME_NEVER);*/
                                
                                if (cia[i].todtimer == null){
                                        /*TODO*///timer_adjust(cia[i].todtimer, 0.1, /*i,*/ 0);
                                        cia[i].todtimer=timer_set(0.1,i,cia_tod_timeout);
                                } else {
                                        timer_reset(cia[i].todtimer, TIME_NEVER);
                                }
                                /*if (cia[i].todtimer != null)
                                    timer_reset(cia[i].todtimer, TIME_NEVER);*/
                        }
                }
        }

    /******************* external interrupt check *******************/

    static void cia_set_interrupt (CIA6526 This, int data)
    {
            This.ifr |= data;
            if ((This.ier & data) != 0)
            {
                    if ((This.ifr & 0x80)==0)
                    {
    /*TODO*///			DBG_LOG (3, "cia set interrupt", ("%d %.2x\n",
    /*TODO*///											  This.number, data));
                            if (This.intf.irq_func != null)
                                    This.intf.irq_func.handler(1);
                            This.ifr |= 0x80;
                    }
            }
            
            //cia[This.number] = This;
    }

    static void cia_clear_interrupt (CIA6526 This, int data)
    {
            This.ifr &= ~data;
            if ((This.ifr & 0x9f) == 0x80)
            {
                    This.ifr &= ~0x80;
                    if (This.intf.irq_func != null)
                            This.intf.irq_func.handler(0);
            }
            
            //cia[This.number] = This;
    }

        /******************* Timer timeouts *************************/
        public static timer_callback cia_tod_timeout = new timer_callback() {
            public void handler(int which) {
                CIA6526 This = cia[which];

                This.tod10ths++;
                if (This.tod10ths > 9)
                {
                        This.tod10ths = 0;
                        //This.todsec = BCD_INC (This.todsec);
                        BCD_INC (This.todsec);
                        if (This.todsec > 0x59)
                        {
                                This.todsec = 0;
                                //This.todmin = BCD_INC (This.todmin);
                                BCD_INC (This.todmin);
                                if (This.todmin > 0x59)
                                {
                                        This.todmin = 0;
                                        if (This.todhour == 0x91)
                                                This.todhour = 0;
                                        else if (This.todhour == 0x89)
                                                This.todhour = 0x90;
                                        else if (This.todhour == 0x11)
                                                This.todhour = 0x80;
                                        else if (This.todhour == 0x09)
                                                This.todhour = 0x10;
                                        else
                                                This.todhour++;
                                }
                        }
                }
                if ((This.todhour == This.alarmhour)
                        && (This.todmin == This.alarmmin)
                        && (This.todsec == This.alarmsec)
                        && (This.tod10ths == This.alarm10ths))
                        cia_set_interrupt (This, 4);
                if (TODIN_50HZ(This) != 0)
                {
                        if (This.intf.todin50hz != 0)
                                timer_reset (This.todtimer, 0.1);
                        else
                                timer_reset (This.todtimer, 5.0 / 60);
                }
                else
                {
                        if (This.intf.todin50hz!= 0)
                                timer_reset (This.todtimer, 6.0 / 50);
                        else
                                timer_reset (This.todtimer, 0.1);
                }
                
                //cia[which] = This;
            }
        };

        static void cia_timer1_state (CIA6526 This)
        {

/*TODO*///                DBG_LOG (1, "timer1 state", ("%d\n", This.timer1_state));
                switch (This.timer1_state)
                {
                case 0:						   /* timer stopped */
                        if (TIMER1_RELOAD(This) != 0)
                        {
                                This.cra &= ~0x10;
                                This.t1c = This.t1l;
                        }
                        if (TIMER1_STOP(This) == 0)
                        {
                                if (TIMER1_COUNT_CNT(This) != 0)
                                {
                                        This.timer1_state = 2;
                                }
                                else
                                {
                                        This.timer1_state = 1;
                                        //timer_adjust(This.timer1, TIME_IN_CYCLES (This.t1c, 0), /*This.number,*/ 0);
                                        This.timer1 = timer_set(TIME_IN_CYCLES(This.t1c, 0),This.number, cia_timer1_timeout);
                                }
                        }
                        break;
                case 1:						   /* counting clock input */
                        if (TIMER1_RELOAD(This) != 0)
                        {
                                This.cra &= ~0x10;
                                This.t1c = This.t1l;
                                if (TIMER1_STOP(This) == 0)
                                        timer_reset (This.timer1, TIME_IN_CYCLES (This.t1c, 0));
                        }
                        if (TIMER1_STOP(This) != 0)
                        {
                                This.timer1_state = 0;
                                timer_reset(This.timer1, TIME_NEVER);
                        }
                        else if (TIMER1_COUNT_CNT(This) != 0)
                        {
                                This.timer1_state = 2;
                                timer_reset(This.timer1, TIME_NEVER);
                        }
                        break;
                case 2:						   /* counting cnt input */
                        if (TIMER1_RELOAD(This) != 0)
                        {
                                This.cra &= ~0x10;
                                This.t1c = This.t1l;
                        }
                        if (TIMER1_STOP(This) != 0)
                        {
                                This.timer1_state = 0;
                        }
                        else if (TIMER1_COUNT_CNT(This) == 0)
                        {
                                //timer_adjust(This.timer1, TIME_IN_CYCLES (This.t1c, 0), /*This.number,*/ 0);
                                This.timer1 = timer_set(TIME_IN_CYCLES(This.t1c, 0),This.number, cia_timer1_timeout);
                                This.timer1_state = 1;
                        }
                        break;
                }
/*TODO*///                DBG_LOG (1, "timer1 state", ("%d\n", This.timer1_state));
                //cia[This.number] = This;
        }

        static void cia_timer2_state (CIA6526 This)
        {
                switch (This.timer2_state)
                {
                case 0:						   /* timer stopped */
                        if (TIMER2_RELOAD(This) != 0)
                        {
                                This.crb &= ~0x10;
                                This.t2c = This.t2l;
                        }
                        if (TIMER2_STOP(This) == 0)
                        {
                                if (TIMER2_COUNT_CLOCK(This) != 0)
                                {
                                        This.timer2_state = 1;
                                        //timer_adjust(This.timer2, TIME_IN_CYCLES(This.t2c, 0), /*This.number,*/ 0);
                                        This.timer2 = timer_set(TIME_IN_CYCLES(This.t2c, 0),This.number, cia_timer2_timeout);
                                }
                                else
                                {
                                        This.timer2_state = 2;
                                }
                        }
                        break;
                case 1:						   /* counting clock input */
                        if (TIMER2_RELOAD(This) != 0)
                        {
                                This.crb &= ~0x10;
                                This.t2c = This.t2l;
                                timer_reset (This.timer2, TIME_IN_CYCLES (This.t2c, 0));
                        }
                        if (TIMER2_STOP(This) != 0)
                        {
                                This.timer2_state = 0;
                                timer_reset(This.timer2, TIME_NEVER);
                        }
                        else if (TIMER2_COUNT_CLOCK(This) == 0)
                        {
                                This.timer2_state = 2;
                                timer_reset(This.timer2, TIME_NEVER);
                        }
                        break;
                case 2:						   /* counting cnt, timer1  input */
                        if (This.t2c == 0)
                        {
                                cia_set_interrupt (This, 2);
                                This.crb |= 0x10;
                        }
                        if (TIMER2_RELOAD(This) != 0)
                        {
                                This.crb &= ~0x10;
                                This.t2c = This.t2l;
                        }
                        if (TIMER2_STOP(This) != 0)
                        {
                                This.timer2_state = 0;
                        }
                        else if (TIMER2_COUNT_CLOCK(This) != 0)
                        {
                                //timer_adjust(This.timer2, TIME_IN_CYCLES(This.t2c, 0), /*This.number,*/ 0);
                                This.timer2 = timer_set(TIME_IN_CYCLES(This.t2c, 0), This.number, cia_timer2_timeout);
                                This.timer2_state = 1;
                        }
                        break;
                }
                
                //cia[This.number] = This;
        }

        public static timer_callback cia_timer1_timeout = new timer_callback() {
            public void handler(int which) {

                CIA6526 This = cia[which];

                This.t1c = This.t1l;

                if (TIMER1_ONESHOT(This) != 0)
                {
                        This.cra &= ~1;
                        This.timer1_state = 0;
                }
                else
                {
                        timer_reset (This.timer1, TIME_IN_CYCLES (This.t1c, 0));
                }
                cia_set_interrupt (This, 1);
                if (SERIAL_MODE_OUT(This) != 0)
                {
                        if (This.shift!=0 || This.loaded!=0)
                        {
                                if (This.cnt!=0)
                                {
                                        if (This.shift == 0)
                                        {
                                                This.loaded = 0;
                                                This.serial = This.sdr;
                                        }
                                        This.sp = (This.serial & 0x80)!=0 ? 1 : 0;
                                        This.shift++;
                                        This.serial <<= 1;
                                        This.cnt = 0;
                                }
                                else
                                {
                                        This.cnt = 1;
                                        if (This.shift == 8)
                                        {
                                                cia_set_interrupt (This, 8);
                                                This.shift = 0;
                                        }
                                }
                        }
                }

                /*  cia_timer1_state(This); */

                if (TIMER2_COUNT_TIMER1(This) != 0 || ((TIMER2_COUNT_TIMER1_CNT(This) != 0 ) && (This.cnt!=0)))
                {
                        This.t2c--;
                        cia_timer2_state (This);
                }
                
                //cia[which] = This;
            }
        };

        public static timer_callback cia_timer2_timeout = new timer_callback() {
            public void handler(int which) {
                CIA6526 This = cia[which];

                This.t2c = This.t2l;

                if (TIMER2_ONESHOT(This) != 0)
                {
                        This.crb &= ~1;
                        This.timer2_state = 0;
                }
                else
                {
                        timer_reset (This.timer2, TIME_IN_CYCLES (This.t2c, 0));
                }

                cia_set_interrupt (This, 2);
                /*  cia_timer2_state(This); */
                
                //cia[which] = This;
            }
        };


    /******************* CPU interface for VIA read *******************/

    static int cia6526_read (CIA6526 This, int offset)
    {
            int val = 0;

            offset &= 0xf;
            switch (offset)
            {
            case 0:
                    if (This.intf.in_a_func != null)
                            This.in_a = This.intf.in_a_func.handler(This.number);
                    val = ((This.out_a & This.ddr_a)
                               | (This.intf.a_pullup & ~This.ddr_a)) & This.in_a;
                    break;
            case 1:
                    if (This.intf.in_b_func != null)
                            This.in_b = This.intf.in_b_func.handler(This.number);
                    val = ((This.out_b & This.ddr_b)
                               | (This.intf.b_pullup & ~This.ddr_b)) & This.in_b;
                    break;
            case 2:
                    val = This.ddr_a;
                    break;
            case 3:
                    val = This.ddr_b;
                    break;
            case 8:
                    if (This.todlatched != 0)
                            val = This.latch10ths;
                    else
                            val = This.tod10ths;
                    This.todlatched = 0;
                    break;
            case 9:
                    if (This.todlatched != 0)
                            val = This.latchsec;
                    else
                            val = This.todsec;
                    break;
            case 0xa:
                    if (This.todlatched != 0)
                            val = This.latchmin;
                    else
                            val = This.todmin;
                    break;
            case 0xb:
                    This.latch10ths = This.tod10ths;
                    This.latchsec = This.todsec;
                    This.latchmin = This.todmin;
                    val = This.latchhour = This.todhour;
                    This.todlatched = 1;
                    break;
            case 0xd:
                    val = This.ifr & ~0x60;
                    cia_clear_interrupt (This, 0x1f);
                    break;
            case 4:
                    if (This.timer1 != null)
                            val = TIME_TO_CYCLES (0, timer_timeleft (This.timer1)) & 0xff;
                    else
                            val = This.t1c & 0xff;
/*TODO*///                    DBG_LOG (3, "cia timer 1 lo", ("%d %.2x\n", This.number, val));
                    break;
            case 5:
                    if (This.timer1 != null)
                            val = TIME_TO_CYCLES (0, timer_timeleft (This.timer1)) >> 8;
                    else
                            val = This.t1c >> 8;
/*TODO*///                    DBG_LOG (3, "cia timer 1 hi", ("%d %.2x\n", This.number, val));
                    break;
            case 6:
                    if (This.timer2 != null)
                            val = TIME_TO_CYCLES (0, timer_timeleft (This.timer2)) & 0xff;
                    else
                            val = This.t2c & 0xff;
/*TODO*///                    DBG_LOG (3, "cia timer 2 lo", ("%d %.2x\n", This.number, val));
                    break;
            case 7:
                    if (This.timer2 != null)
                            val = TIME_TO_CYCLES (0, timer_timeleft (This.timer2)) >> 8;
                    else
                            val = This.t2c >> 8;
/*TODO*///                    DBG_LOG (3, "cia timer 2 hi", ("%d %.2x\n", This.number, val));
                    break;
            case 0xe:
                    val = This.cra;
                    break;
            case 0xf:
                    val = This.crb;
                    break;
            case 0xc:
                    val = This.sdr;
                    break;
            }
    /*TODO*///	DBG_LOG (1, "cia read", ("%d %.2x:%.2x\n", This.number, offset, val));
            
            //cia[This.number] = This;
            
            return val;
    }

    public static int ciaB=0;

        /******************* CPU interface for VIA write *******************/

        static void cia6526_write (CIA6526 This, int offset, int data)
        {
/*TODO*///                DBG_LOG (1, "cia write", ("%d %.2x:%.2x\n", This.number, offset, data));
                offset &= 0xf;

                switch (offset)
                {
                case 0:
                        This.out_a = data;
                        if (This.intf.out_a_func != null){
                            //System.out.println("A: "+offset);
                                This.intf.out_a_func.handler(This.number, (This.out_a & This.ddr_a)
                                                                         | (~This.ddr_a & This.intf.a_pullup));
                        }
                        break;
                case 1:
                        This.out_b = data;
                        if (This.intf.out_b_func != null)
                                This.intf.out_b_func.handler(This.number, (This.out_b & This.ddr_b)
                                                                         | (~This.ddr_b & This.intf.b_pullup));
                        break;
                case 2:
                        This.ddr_a = data;
                        if (This.intf.out_a_func != null){
                            //System.out.println("B: "+offset);
                            ciaB=2;
                                This.intf.out_a_func.handler(This.number, (This.out_a & This.ddr_a)
                                                                         | (~This.ddr_a & This.intf.a_pullup));
                        }
                        break;
                case 3:
                        This.ddr_b = data;
                        if (This.intf.out_b_func != null)
                                This.intf.out_b_func.handler(This.number, (This.out_b & This.ddr_b)
                                                                         | (~This.ddr_b & This.intf.b_pullup));
                        break;
                case 8:
                        if (TOD_ALARM(This) != 0)
                                This.alarm10ths = data;
                        else
                        {
                                This.tod10ths = data;
                                if (This.todstopped != 0)
                                {
                                        if (TODIN_50HZ(This) != 0)
                                        {
                                                //timer_adjust(This.todtimer, (This.intf.todin50hz)!=0 ? 0.1 : 5/60, /*This.number,*/ 0);
                                                This.todtimer = timer_set((This.intf.todin50hz)!=0 ? 0.1 : 5/60,This.number,cia_tod_timeout);
                                        }
                                        else
                                        {
                                                //timer_adjust(This.todtimer, (This.intf.todin50hz)!=0 ? 60 / 5 : 0.1, /*This.number,*/ 0);
                                                This.todtimer = timer_set((This.intf.todin50hz)!=0 ? 60 / 5 : 0.1, This.number,cia_tod_timeout);
                                        }
                                }
                                This.todstopped = 0;
                        }
                        break;
                case 9:
                        if (TOD_ALARM(This) != 0)
                                This.alarmsec = data;
                        else
                                This.todsec = data;
                        break;
                case 0xa:
                        if (TOD_ALARM(This) != 0)
                                This.alarmmin = data;
                        else
                                This.todmin = data;
                        break;
                case 0xb:
                        if (TOD_ALARM(This) != 0)
                                This.alarmhour = data;
                        else
                        {
                                timer_reset(This.todtimer, TIME_NEVER);
                                This.todstopped = 1;
                                This.todhour = data;
                        }
                        break;
                case 0xd:
/*TODO*///                        DBG_LOG (1, "cia interrupt enable", ("%d %.2x\n", This.number, data));
                        if ((data & 0x80) != 0)
                        {
                                This.ier |= data;
                                cia_set_interrupt (This, 0);
                        }
                        else
                        {
                                This.ier &= ~data;
                                cia_clear_interrupt (This, data & 0x1f);
                        }
                        break;
                case 4:
                        This.t1l = (This.t1l & ~0xff) | data;
                        if (This.t1l == 0)
                                This.t1l = 0x10000;		   /*avoid hanging in timer_schedule */
/*TODO*///                        DBG_LOG (3, "cia timer 1 lo write", ("%d %.2x\n", This.number, data));
                        break;
                case 5:
                        This.t1l = (This.t1l & 0xff) | (data << 8);
                        if (This.t1l == 0)
                                This.t1l = 0x10000;		   /*avoid hanging in timer_schedule */
                        if (TIMER1_STOP(This) != 0)
                                This.t1c = This.t1l;
/*TODO*///                        DBG_LOG (3, "cia timer 1 hi write", ("%d %.2x\n", This.number, data));
                        break;
                case 6:
                        This.t2l = (This.t2l & ~0xff) | data;
                        if (This.t2l == 0)
                                This.t2l = 0x10000;		   /*avoid hanging in timer_schedule */
/*TODO*///                        DBG_LOG (3, "cia timer 2 lo write", ("%d %.2x\n", This.number, data));
                        break;
                case 7:
                        This.t2l = (This.t2l & 0xff) | (data << 8);
                        if (This.t2l == 0)
                                This.t2l = 0x10000;		   /*avoid hanging in timer_schedule */
                        if (TIMER2_STOP(This) != 0)
                                This.t2c = This.t2l;
/*TODO*///                        DBG_LOG (3, "cia timer 2 hi write", ("%d %.2x\n", This.number, data));
                        break;
                case 0xe:
/*TODO*///                        DBG_LOG (3, "cia write cra", ("%d %.2x\n", This.number, data));
                        if ((This.cra & 0x40) != (data & 0x40))
                        {
                                if ((This.cra & 0x40)==0)
                                {
                                        This.loaded = 0;
                                        This.shift = 0;
                                        This.cnt = 1;
                                }
                        }
                        This.cra = data;
                        cia_timer1_state (This);
                        break;
                case 0xf:
/*TODO*///                        DBG_LOG (3, "cia write crb", ("%d %.2x\n", This.number, data));
                        This.crb = data;
                        cia_timer2_state (This);
                        break;
                case 0xc:
                        This.sdr = data;
                        if (SERIAL_MODE_OUT(This) != 0)
                        {
                                This.loaded = 1;
                        }
                        break;
                }
                
                //cia[This.number] = This;
        }

/*TODO*///static void cia_set_input_a (CIA6526 *This, int data)
/*TODO*///{
/*TODO*///	This.in_a = data;
/*TODO*///}
/*TODO*///
/*TODO*///static void cia_set_input_b (CIA6526 *This, int data)
/*TODO*///{
/*TODO*///	This.in_b = data;
/*TODO*///}

    static void cia6526_set_input_flag (CIA6526 This, int data)
    {
            if (This.flag!=0 && data==0)
                    cia_set_interrupt (This, 0x10);
            This.flag = data;
            
            //cia[This.number] = This;
    }

/*TODO*///static void cia6526_set_input_sp (CIA6526 *This, int data)
/*TODO*///{
/*TODO*///	This.sp = data;
/*TODO*///}
/*TODO*///
/*TODO*///static void cia6526_set_input_cnt (CIA6526 *This, int data)
/*TODO*///{
/*TODO*///	if (!This.cnt && data)
/*TODO*///	{
/*TODO*///		if (!SERIAL_MODE_OUT)
/*TODO*///		{
/*TODO*///			This.serial >>= 1;
/*TODO*///			if (This.sp)
/*TODO*///				This.serial |= 0x80;
/*TODO*///			if (++This.shift == 8)
/*TODO*///			{
/*TODO*///				This.sdr = This.serial;
/*TODO*///				This.serial = 0;
/*TODO*///				This.shift = 0;
/*TODO*///				cia_set_interrupt (This, 8);
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	This.cnt = data;
/*TODO*///}
/*TODO*///
        public static ReadHandlerPtr cia6526_0_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return cia6526_read (cia[0], offset);
            }
        };
        
        public static ReadHandlerPtr cia6526_1_port_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return cia6526_read (cia[1], offset);
            }
        };

/*TODO*///READ_HANDLER ( cia6526_2_port_r )
/*TODO*///{
/*TODO*///	return cia6526_read (cia+2, offset);
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_3_port_r )
/*TODO*///{
/*TODO*///	return cia6526_read (cia+3, offset);
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_4_port_r )
/*TODO*///{
/*TODO*///	return cia6526_read (cia+4, offset);
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_5_port_r )
/*TODO*///{
/*TODO*///	return cia6526_read (cia+5, offset);
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_6_port_r )
/*TODO*///{
/*TODO*///	return cia6526_read (cia+6, offset);
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_7_port_r )
/*TODO*///{
/*TODO*///	return cia6526_read (cia+7, offset);
/*TODO*///}
/*TODO*///
        public static WriteHandlerPtr cia6526_0_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                cia6526_write (cia[0], offset, data);
            }
        };

        
        public static WriteHandlerPtr cia6526_1_port_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                cia6526_write (cia[1], offset, data);
            }
        };
            
/*TODO*///WRITE_HANDLER ( cia6526_2_port_w )
/*TODO*///{
/*TODO*///	cia6526_write (cia+2, offset, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_3_port_w )
/*TODO*///{
/*TODO*///	cia6526_write (cia+3, offset, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_4_port_w )
/*TODO*///{
/*TODO*///	cia6526_write (cia+4, offset, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_5_port_w )
/*TODO*///{
/*TODO*///	cia6526_write (cia+5, offset, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_6_port_w )
/*TODO*///{
/*TODO*///	cia6526_write (cia+6, offset, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_7_port_w )
/*TODO*///{
/*TODO*///	cia6526_write (cia+7, offset, data);
/*TODO*///}
/*TODO*///
/*TODO*////******************* 8-bit A/B port interfaces *******************/
/*TODO*///
/*TODO*///WRITE_HANDLER ( cia6526_0_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_1_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+1, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_2_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+2, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_3_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+3, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_4_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+4, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_5_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+5, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_6_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+6, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_7_porta_w )
/*TODO*///{
/*TODO*///	cia_set_input_a (cia+7, data);
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER ( cia6526_0_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_1_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+1, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_2_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+2, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_3_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+3, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_4_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+4, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_5_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+5, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_6_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+6, data);
/*TODO*///}
/*TODO*///WRITE_HANDLER ( cia6526_7_portb_w )
/*TODO*///{
/*TODO*///	cia_set_input_b (cia+7, data);
/*TODO*///}
/*TODO*///
/*TODO*///READ_HANDLER ( cia6526_0_porta_r )
/*TODO*///{
/*TODO*///	return cia[0].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_1_porta_r )
/*TODO*///{
/*TODO*///	return cia[1].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_2_porta_r )
/*TODO*///{
/*TODO*///	return cia[2].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_3_porta_r )
/*TODO*///{
/*TODO*///	return cia[3].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_4_porta_r )
/*TODO*///{
/*TODO*///	return cia[4].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_5_porta_r )
/*TODO*///{
/*TODO*///	return cia[5].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_6_porta_r )
/*TODO*///{
/*TODO*///	return cia[6].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_7_porta_r )
/*TODO*///{
/*TODO*///	return cia[7].in_a;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_0_portb_r )
/*TODO*///{
/*TODO*///	return cia[0].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_1_portb_r )
/*TODO*///{
/*TODO*///	return cia[1].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_2_portb_r )
/*TODO*///{
/*TODO*///	return cia[2].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_3_portb_r )
/*TODO*///{
/*TODO*///	return cia[3].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_4_portb_r )
/*TODO*///{
/*TODO*///	return cia[4].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_5_portb_r )
/*TODO*///{
/*TODO*///	return cia[5].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_6_portb_r )
/*TODO*///{
/*TODO*///	return cia[6].in_b;
/*TODO*///}
/*TODO*///READ_HANDLER ( cia6526_7_portb_r )
/*TODO*///{
/*TODO*///	return cia[7].in_b;
/*TODO*///}
/*TODO*///
    public static void cia6526_0_set_input_flag (int data)
    {
	cia6526_set_input_flag (cia[0], data);
    }
/*TODO*///void cia6526_1_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+1, data);
/*TODO*///}
/*TODO*///void cia6526_2_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+2, data);
/*TODO*///}
/*TODO*///void cia6526_3_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+3, data);
/*TODO*///}
/*TODO*///void cia6526_4_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+4, data);
/*TODO*///}
/*TODO*///void cia6526_5_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+5, data);
/*TODO*///}
/*TODO*///void cia6526_6_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+6, data);
/*TODO*///}
/*TODO*///void cia6526_7_set_input_flag (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_flag (cia+7, data);
/*TODO*///}
/*TODO*///
/*TODO*///void cia6526_0_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia, data);
/*TODO*///}
/*TODO*///void cia6526_1_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+1, data);
/*TODO*///}
/*TODO*///void cia6526_2_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+2, data);
/*TODO*///}
/*TODO*///void cia6526_3_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+3, data);
/*TODO*///}
/*TODO*///void cia6526_4_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+4, data);
/*TODO*///}
/*TODO*///void cia6526_5_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+5, data);
/*TODO*///}
/*TODO*///void cia6526_6_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+6, data);
/*TODO*///}
/*TODO*///void cia6526_7_set_input_sp (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_sp (cia+7, data);
/*TODO*///}
/*TODO*///
/*TODO*///void cia6526_0_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia, data);
/*TODO*///}
/*TODO*///void cia6526_1_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+1, data);
/*TODO*///}
/*TODO*///void cia6526_2_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+2, data);
/*TODO*///}
/*TODO*///void cia6526_3_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+3, data);
/*TODO*///}
/*TODO*///void cia6526_4_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+4, data);
/*TODO*///}
/*TODO*///void cia6526_5_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+5, data);
/*TODO*///}
/*TODO*///void cia6526_6_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+6, data);
/*TODO*///}
/*TODO*///void cia6526_7_set_input_cnt (int data)
/*TODO*///{
/*TODO*///	cia6526_set_input_cnt (cia+7, data);
/*TODO*///}
/*TODO*///	void cia6526_status (char *text, int size)
/*TODO*///	{
/*TODO*///		text[0] = 0;
/*TODO*///	#if VERBOSE_DBG
/*TODO*///	#if 0
/*TODO*///		snprintf (text, size, "cia ier:%.2x ifr:%.2x %d", cia[1].ier, cia[1].ifr,
/*TODO*///				  cia[1].flag);
/*TODO*///	#endif
/*TODO*///	#if 0
/*TODO*///		snprintf (text, size, "cia 1 %.2x %.2x %.2x %.2x",
/*TODO*///				  cia[1].tod10ths, cia[1].todsec,
/*TODO*///				  cia[1].todmin, cia[1].todhour);
/*TODO*///	
/*TODO*///	#endif
/*TODO*///	#endif
/*TODO*///	}
}


