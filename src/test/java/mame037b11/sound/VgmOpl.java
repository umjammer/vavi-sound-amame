/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mame037b11.sound;

import mame037b11.sound.fmoplH.FM_OPL;
import mame037b11.sound.fmoplH.OPL_CH;

import static mame037b11.sound.fmopl.*;
import static mame037b11.sound.fmoplH.*;


/**
 * Generates YM3812/YM3526 samples without the 16 bit limit of {@link fmopl#YM3812UpdateOne}.
 * <p>
 * fmopl (mame 0.37) is about 3 times louder than ymfm and saturates inside the core,
 * so the caller can scale the raw output instead.
 * lives in this package because the fmopl work state is package-private.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class VgmOpl {

    private VgmOpl() {
    }

    /** generates one sample, same as YM3812UpdateOne except for the limit check */
    public static int update(FM_OPL OPL) {
        int rythm = OPL.rythm & 0x20;
        if (OPL != cur_chip) {
            cur_chip = OPL;
            // channel pointers
            S_CH = OPL.P_CH;
            E_CH = 9;
            // rythm slot
            SLOT7_1 = S_CH[7].SLOT[SLOT1];
            SLOT7_2 = S_CH[7].SLOT[SLOT2];
            SLOT8_1 = S_CH[8].SLOT[SLOT1];
            SLOT8_2 = S_CH[8].SLOT[SLOT2];
            // LFO state
            amsIncr = OPL.amsIncr;
            vibIncr = OPL.vibIncr;
            ams_table = OPL.ams_table;
            vib_table = OPL.vib_table;
        }
        int R_CH = rythm != 0 ? 6 : E_CH;
        // LFO
        long amsCnt = (OPL.amsCnt + amsIncr) & 0xFFFFFFFFL;
        long vibCnt = (OPL.vibCnt + vibIncr) & 0xFFFFFFFFL;
        ams = ams_table.read((int) (amsCnt >> AMS_SHIFT));
        vib = vib_table.read((int) (vibCnt >> VIB_SHIFT));
        outd[0] = 0;
        // FM part
        for (int k = 0; k != R_CH; k++) {
            OPL_CH CH = S_CH[k];
            OPL_CALC_CH(CH);
        }
        // rythm part
        if (rythm != 0) {
            OPL_CALC_RH(S_CH);
        }
        OPL.amsCnt = (int) amsCnt;
        OPL.vibCnt = (int) vibCnt;
        return outd[0] >> OPL_OUTSB;
    }
}
