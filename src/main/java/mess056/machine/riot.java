/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mess056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mess056.machine.riotH.*;

/**
 *
 * @author jagsanchez
 */
public class riot {
    
/*TODO*///    #define VERBOSE 1

/*TODO*///    #if VERBOSE
/*TODO*///    #define LOG(x) if( errorlog ) fprintf x
/*TODO*///    #else
/*TODO*///    #define LOG(x) /* x */
/*TODO*///    #endif

    public static class RIOT {
            public int	dria;		/* Data register A input */
            public int	droa;		/* Data register A output */
            public int	ddra;		/* Data direction register A; 1 bits = output */
            public int	drib;		/* Data register B input */
            public int	drob;		/* Data register B output */
            public int	ddrb;		/* Data direction register B; 1 bits = output */
            public int	irqen;		/* IRQ enabled ? */
            public int	state;		/* current timer state (bit 7) */
            public double	clock;		/* baseclock/1(,8,64,1024) */
            public timer_entry timer; 	/* timer callback */
            public double  baseclock;  /* copied from interface */
            public ReadHandlerPtr port_a_r;
            public ReadHandlerPtr port_b_r;
            public WriteHandlerPtr port_a_w;
            public WriteHandlerPtr port_b_w;
            public ReadHandlerPtr irq_callback;
    };

    static RIOT[] riot = new RIOT[MAX_RIOTS];
    

    public static void riot_init(RIOTinterface r)
    {
            int i;
            for( i = 0; i < MAX_RIOTS && i < r.num_chips; i++ )
            {
                    riot[i] = new RIOT();
                    riot[i].baseclock = r.baseclock[i];
                    riot[i].port_a_r = r.port_a_r[i];
                    riot[i].port_b_r = r.port_b_r[i];
                    riot[i].port_a_w = r.port_a_w[i];
                    riot[i].port_b_w = r.port_b_w[i];
                    riot[i].irq_callback = r.irq_callback[i];
            }
/*TODO*///            LOG((errorlog, "RIOT - successfully initialised\n"));
    }

    public static int riot_r(int chip, int offset)
    {
            int data = 0xff;
/*TODO*///            LOG((errorlog, "RIOT read - offset is $%02x\n", offset));
            switch( offset )
        {
            case 0x0: case 0x8: /* Data register A */
                    if( riot[chip].port_a_r != null )
                            data = (riot[chip].port_a_r).handler(chip);
                    /* mask input bits and combine with output bits */
                    data = (data & ~riot[chip].ddra) | (riot[chip].droa & riot[chip].ddra);
/*TODO*///                    LOG((errorlog, "riot(%d) DRA   read : $%02x\n", chip, data));
                    break;
            case 0x1: case 0x9: /* Data direction register A */
                    data = riot[chip].ddra;
/*TODO*///                    LOG((errorlog, "riot(%d) DDRA  read : $%02x\n", chip, data));
                    break;
            case 0x2: case 0xa: /* Data register B */
                    if( riot[chip].port_b_r != null )
                            data = (riot[chip].port_b_r).handler(chip);
                    /* mask input bits and combine with output bits */
                    data = (data & ~riot[chip].ddrb) | (riot[chip].drob & riot[chip].ddrb);
/*TODO*///            LOG((errorlog, "riot(%d) DRB   read : $%02x\n", chip, data));
                    break;
            case 0x3: case 0xb: /* Data direction register B */
                    data = riot[chip].ddrb;
/*TODO*///                    LOG((errorlog, "riot(%d) DDRB  read : $%02x\n", chip, data));
                    break;
            case 0x4: case 0xc: /* Timer count read (not supported?) */
/*TODO*///                    LOG((errorlog, "riot(%d) TIMR  read : $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":"    ")));
                    data = (int)(256 * timer_timeleft(riot[chip].timer) / TIME_IN_HZ(riot[chip].clock));
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
/*TODO*///                    LOG((errorlog, "riot(%d) TIMR  read : $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":"    ")));
                    break;
            case 0x5: case 0xd: /* Timer count read (not supported?) */
                    data = (int)(256 * timer_timeleft(riot[chip].timer) / TIME_IN_HZ(riot[chip].clock));
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
/*TODO*///                    LOG((errorlog, "riot(%d) TIMR  read : $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":"    ")));
                    break;
            case 0x6: case 0xe: /* Timer count read */
                    data = (int)(256 * timer_timeleft(riot[chip].timer) / TIME_IN_HZ(riot[chip].clock));
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
/*TODO*///                    LOG((errorlog, "riot(%d) TIMR  read : $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":"    ")));
                    break;
            case 0x7: case 0xf: /* Timer status read */
                    data = riot[chip].state;
                    riot[chip].state &= ~0x80;
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
/*TODO*///                    LOG((errorlog, "riot(%d) STAT  read : $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":"    ")));
                    break;
        }
            return data;
    }

    public static timer_callback riot_timer_cb = new timer_callback() {
        public void handler(int chip) {
            /*TODO*///            LOG((errorlog, "riot(%d) timer expired\n", chip));
            riot[chip].state |= 0x80;
            if( riot[chip].irqen != 0 ) /* with IRQ? */
            {
                    if( riot[chip].irq_callback != null )
                            (riot[chip].irq_callback).handler(chip);
            }

        }
    };

    public static void riot_w(int chip, int offset, int data)
    {
/*TODO*///            LOG((errorlog, "RIOT write - offset is $%02x\n", offset));
            switch( offset )
        {
            case 0x0: case 0x8: /* Data register A */
/*TODO*///                    LOG((errorlog, "riot(%d) DRA  write: $%02x\n", chip, data));
                    riot[chip].droa = data;
                    if( riot[chip].port_a_w != null )
                            (riot[chip].port_a_w).handler(chip,data);
                    break;
            case 0x1: case 0x9: /* Data direction register A */
/*TODO*///                    LOG((errorlog, "riot(%d) DDRA  write: $%02x\n", chip, data));
                    riot[chip].ddra = data;
                    break;
            case 0x2: case 0xa: /* Data register B */
/*TODO*///                    LOG((errorlog, "riot(%d) DRB   write: $%02x\n", chip, data));
                    riot[chip].drob = data;
                    if( riot[chip].port_b_w != null )
                            (riot[chip].port_b_w).handler(chip,data);
            break;
            case 0x3: case 0xb: /* Data direction register B */
/*TODO*///                    LOG((errorlog, "riot(%d) DDRB  write: $%02x\n", chip, data));
                    riot[chip].ddrb = data;
                    break;
            case 0x4: case 0xc: /* Timer 1 start */
/*TODO*///                    LOG((errorlog, "riot(%d) TMR1  write: $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":" ")));
                    riot[chip].state &= ~0x80;
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
                    if( riot[chip].timer != null )
                            timer_remove(riot[chip].timer);
                    riot[chip].clock = (double)riot[chip].baseclock / 1;
                    riot[chip].timer = timer_pulse(TIME_IN_HZ((data+1) * riot[chip].clock / 256 / 256), chip, riot_timer_cb);
                    break;
            case 0x5: case 0xd: /* Timer 8 start */
/*TODO*///                    LOG((errorlog, "riot(%d) TMR8  write: $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":" ")));
                    riot[chip].state &= ~0x80;
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
                    if( riot[chip].timer != null )
                            timer_remove(riot[chip].timer);
                    riot[chip].clock = (double)riot[chip].baseclock / 8;
                    riot[chip].timer = timer_pulse(TIME_IN_HZ((data+1) * riot[chip].clock / 256 / 256), chip, riot_timer_cb);
            break;
            case 0x6: case 0xe: /* Timer 64 start */
/*TODO*///                    LOG((errorlog, "riot(%d) TMR64 write: $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":" ")));
                    riot[chip].state &= ~0x80;
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
                    if( riot[chip].timer != null )
                            timer_remove(riot[chip].timer);
                    riot[chip].clock = (double)riot[chip].baseclock / 64;
                    riot[chip].timer = timer_pulse(TIME_IN_HZ((data+1) * riot[chip].clock / 256 / 256), chip, riot_timer_cb);
            break;
            case 0x7: case 0xf: /* Timer 1024 start */
/*TODO*///                    LOG((errorlog, "riot(%d) TMR1K write: $%02x%s\n", chip, data, (char*)((offset & 8) ? " (IRQ)":" ")));
                    riot[chip].state &= ~0x80;
                    riot[chip].irqen = (offset & 8)!=0 ? 1 : 0;
                    if( riot[chip].timer != null )
                            timer_remove(riot[chip].timer);
                    riot[chip].clock = (double)riot[chip].baseclock / 1024;
                    riot[chip].timer = timer_pulse(TIME_IN_HZ((data+1) * riot[chip].clock / 256 / 256), chip, riot_timer_cb);
            break;
        }
    }


    public static ReadHandlerPtr riot_0_r = new ReadHandlerPtr() {
        public int handler(int offs) {
            return riot_r(0,offs);
        }
    };
   
    public static WriteHandlerPtr riot_0_w = new WriteHandlerPtr() {
        public void handler(int offs, int data) {
            riot_w(0,offs,data);
        }
    };
    
    public static ReadHandlerPtr riot_1_r = new ReadHandlerPtr() {
        public int handler(int offs) {
            return riot_r(1,offs);
        } 
    };
    
    public static WriteHandlerPtr riot_1_w = new WriteHandlerPtr() {
        public void handler(int offs, int data) {
            riot_w(1,offs,data);
        } 
    };
    
    public static ReadHandlerPtr riot_2_r = new ReadHandlerPtr() {
        public int handler(int offs) {
            return riot_r(2,offs);
        } 
    };
    
    public static WriteHandlerPtr riot_2_w = new WriteHandlerPtr() {
        public void handler(int offs, int data) {
            riot_w(2,offs,data);
        } 
    };
    
    public static ReadHandlerPtr riot_3_r = new ReadHandlerPtr() {
        public int handler(int offs) {
            return riot_r(3,offs);
        } 
    };
    
    public static WriteHandlerPtr riot_3_w = new WriteHandlerPtr() {
        public void handler(int offs, int data) {
            riot_w(3,offs,data);
        } 
    };
    
}
