/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.sound;


/**
 * Yamaha YM2164 OPP, the YM2151 of the FB-01 and the DX21 family.
 * <p>
 * As ymfm says, it is almost 100% the YM2151, except for some mystery registers in the 00-07
 * range, which nothing is known to write, and timer B, which runs at half the rate of the
 * YM2151's. mame's ym2164 has the half rate timer B in its comment only.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public class YM2164 extends YM2151 {

    @Override
    protected int timerBPrescaler() {
        return 2048;
    }
}
