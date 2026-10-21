/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.sound;

import org.junit.jupiter.api.Test;

import amame.AmameTestSupport.Bench;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Timer B of the YM2164 runs at half the rate of the YM2151's.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
class YM2164Test {

    @Test
    void timerB() {
        // 0xc5 is what the FB-01 sets timer B to
        assertEquals(2048 * (256 - 0xc5) / 4e6, timerB(new YM2164()), 1e-6);
    }

    @Test
    void timerBOfYm2151() {
        assertEquals(1024 * (256 - 0xc5) / 4e6, timerB(new YM2151()), 1e-6);
    }

    /** the time from loading timer b to its irq */
    private static double timerB(YM2151 ym) {
        Bench m = new Bench();
        ym.ym2151_init(m, 4_000_000);
        double[] irq = {-1};
        ym.setIrqHandler(state -> {
            if (state != 0 && irq[0] < 0) irq[0] = m.time();
        });
        ym.ym2151_register_port_0_w(0x12);
        ym.ym2151_data_port_0_w(0xc5);
        ym.ym2151_register_port_0_w(0x14);
        ym.ym2151_data_port_0_w(0x0a); // load timer b, its irq on
        double start = m.time();
        m.step(0.1);
        return irq[0] - start;
    }
}
