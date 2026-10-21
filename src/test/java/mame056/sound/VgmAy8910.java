/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mame056.sound;

import common.ptr.ShortPtr;

import static mame056.sound.ay8910.AYPSG;
import static mame056.sound.ay8910.AY8910Update;
import static mame056.sound.ay8910.AY8910_reset;
import static mame056.sound.ay8910.build_mixer_table;


/**
 * Drives the SSG part (AY8910) of YM2203/YM2608/YM2610 without MAME's stream/mixer system.
 * <p>
 * lives in this package because the AY8910 state is package-private.
 * must be initialized before YM2203Init etc., because their reset sets the SSG clock
 * using the sample rate set here.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class VgmAy8910 {

    private VgmAy8910() {
    }

    private static final ShortPtr[] buffer = {new ShortPtr(1), new ShortPtr(1), new ShortPtr(1)};

    /** sets up ssg chips 0 ~ chips - 1 */
    public static void init(int chips, int rate) {
        new ay8910(); // (re)creates AYPSG entries
        for (int i = 0; i < chips; i++) {
            AYPSG[i].SampleRate = rate;
            build_mixer_table(i);
            AY8910_reset(i);
        }
    }

    /** generates one sample, returns the sum of 3 channels */
    public static int update(int chip) {
        for (ShortPtr b : buffer) b.offset = 0;
        AY8910Update.handler(chip, buffer, 1);
        for (ShortPtr b : buffer) b.offset = 0; // AY8910Update uses writeinc
        return buffer[0].read(0) + buffer[1].read(0) + buffer[2].read(0);
    }
}
