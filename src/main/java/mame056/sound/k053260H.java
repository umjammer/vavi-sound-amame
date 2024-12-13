/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static mame056.timer.timer_callback;

public class k053260H {

    public static final int MAX_053260 = 2;

    public static class K053260_interface {
            public int	num;								/* number of chips */
            public int[] clock = new int[MAX_053260];					/* clock */
            public int[] region = new int[MAX_053260];					/* memory region of sample ROM(s) */
            public int[][] mixing_level = new int[MAX_053260][2];                       /* volume */
            public timer_callback[] irq = new timer_callback[MAX_053260];         /* called on SH1 complete cycle ( clock / 32 ) */
            
            public K053260_interface(int num, int[] clock, int[] region, int[][] mixing_level, timer_callback[] irq) {
                this.num = num;
                this.clock = clock;
                this.region = region;
                this.mixing_level = mixing_level;
                this.irq = irq;
            }
    };

}
