/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.sound;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;


/**
 * Ensoniq ES5505 OTIS, after mame's es5506.cpp (Aaron Giles).
 * <p>
 * 32 voices of interpolated wavetable with a 4 pole filter each, mixed onto up to four stereo
 * channels. The chip is not a stream of its own here, the board around it pulls the samples
 * with {@link #generate} at the rate the active voices make, {@link #getSampleRate()}, and
 * whoever owns the stream calls it, the VFX's pump between this and the ESP.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class ES5505 {

    private static final int FINE_FILTER_BIT = 16;
    private static final int FILTER_BIT = 12;
    private static final int FILTER_SHIFT = FINE_FILTER_BIT - FILTER_BIT;

    private static final int ULAW_MAXBITS = 8;

    private static final int CONTROL_IRQ = 0x0080;
    private static final int CONTROL_DIR = 0x0040;
    private static final int CONTROL_IRQE = 0x0020;
    private static final int CONTROL_BLE = 0x0010;
    private static final int CONTROL_LPE = 0x0008;
    private static final int CONTROL_STOP1 = 0x0002;
    private static final int CONTROL_STOP0 = 0x0001;
    private static final int CONTROL_LOOPMASK = CONTROL_BLE | CONTROL_LPE;
    private static final int CONTROL_STOPMASK = CONTROL_STOP1 | CONTROL_STOP0;

    private static final int LP3 = 1, LP4 = 2, LP_MASK = LP3 | LP4;

    private static final int VOLUME_ACC_BIT = 20;
    private static final int ADDRESS_FRAC_BIT = 11;

    private static final int VOLUME_BIT_ES5505 = 8;
    private static final int ADDRESS_INTEGER_BIT_ES5505 = 20;
    private static final int ADDRESS_FRAC_BIT_ES5505 = 9;

    private static final class Voice {
        int control;
        long freqcount;
        long start;
        int lvol;
        long end;
        long accum;
        int rvol;
        int k2;
        int k1;
        int o4n1;
        int o3n1;
        int o3n2;
        int o2n1;
        int o2n2;
        int o1n1;
    }

    /** the sample roms of the two banks, bytes as mame keeps them, a word is read little endian */
    private final byte[][] banks = new byte[2][];

    private final int masterClock;
    private int sampleRate;
    private int addressAccShift;
    private long addressAccMask;
    private int volumeShift;
    private int volumeAccShift;
    private int currentPage;
    private int activeVoices = 0x1f;
    private int mode;
    private int irqv = 0x80;
    private final Voice[] voices = new Voice[32];
    private final int[] volumeLookup;
    private final int channels;

    private IntConsumer irqCallback = state -> {};
    private IntSupplier readPort = () -> 0;
    private IntConsumer sampleRateChanged = rate -> {};
    /** brought up to date before a register is touched, the stream that pulls the samples */
    private Runnable update = () -> {};

    /**
     * @param channels the stereo outputs, 1 to 4
     */
    public ES5505(int clock, int channels) {
        this.masterClock = clock;
        this.channels = Math.max(1, Math.min(4, channels));
        for (int i = 0; i < 32; i++) voices[i] = new Voice();

        // compute_tables(VOLUME_BIT_ES5505, 4, 4): 4 bit exponent, 4 bit mantissa
        int totalVolumeBit = VOLUME_BIT_ES5505, exponentBit = 4, mantissaBit = 4;
        int volumeBit = exponentBit + mantissaBit;
        volumeShift = totalVolumeBit - volumeBit;
        int volumeLen = 1 << volumeBit;
        volumeLookup = new int[volumeLen];
        int exponentShift = 1 << exponentBit;
        int exponentMask = exponentShift - 1;
        int mantissaLen = 1 << mantissaBit;
        int mantissaMask = mantissaLen - 1;
        int mantissaShift = exponentShift - mantissaBit - 1;
        for (int i = 0; i < volumeLen; i++) {
            int exponent = (i >> mantissaBit) & exponentMask;
            int mantissa = (i & mantissaMask) | mantissaLen;
            volumeLookup[i] = (int) (((long) mantissa << mantissaShift) >>> (exponentShift - exponent));
        }
        volumeAccShift = (16 + exponentMask) - VOLUME_ACC_BIT;
        for (int j = 0; j < 32; j++) {
            voices[j].control = CONTROL_STOPMASK;
            voices[j].lvol = 1 << (totalVolumeBit - 1);
            voices[j].rvol = 1 << (totalVolumeBit - 1);
        }

        // get_accum_mask: 20 bit integer and 9 bit fraction
        addressAccShift = ADDRESS_FRAC_BIT - ADDRESS_FRAC_BIT_ES5505;
        addressAccMask = lshiftSigned(((((1L << ADDRESS_INTEGER_BIT_ES5505) - 1) << ADDRESS_FRAC_BIT_ES5505) | ((1L << ADDRESS_FRAC_BIT_ES5505) - 1)), addressAccShift);
        if (addressAccShift > 0)
            addressAccMask |= (1L << addressAccShift) - 1;

        sampleRate = masterClock / (16 * (activeVoices + 1));
    }

    public void setRegion(int bank, byte[] rom) {
        banks[bank] = rom;
    }

    public void setIrqCallback(IntConsumer callback) {
        irqCallback = callback;
    }

    /** the adc, the analog value selected by the board */
    public void setReadPort(IntSupplier readPort) {
        this.readPort = readPort;
    }

    public void setSampleRateChanged(IntConsumer callback) {
        sampleRateChanged = callback;
    }

    public void setUpdate(Runnable update) {
        this.update = update;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    /** the outputs, two per stereo channel */
    public int getOutputs() {
        return channels * 2;
    }

    private static long lshiftSigned(long val, int shift) {
        return shift >= 0 ? val << shift : val >>> -shift;
    }

    private static long rshiftSigned(long val, int shift) {
        return shift >= 0 ? val >>> shift : val << -shift;
    }

    private long getVolume(int volume) {
        return volumeLookup[(int) rshiftSigned(volume & 0xffff_ffffL, volumeShift)] & 0xffff_ffffL;
    }

    private long accShifted(long val) {
        return lshiftSigned(val, addressAccShift);
    }

    private long accShifted(long val, int bias) {
        return lshiftSigned(val, addressAccShift - bias);
    }

    private long accRes(long val) {
        return rshiftSigned(val, addressAccShift);
    }

    private long accRes(long val, int bias) {
        return rshiftSigned(val, addressAccShift - bias);
    }

    private long getIntegerAddr(long accum, int bias) {
        return ((accum + ((long) bias << ADDRESS_FRAC_BIT)) & addressAccMask) >>> ADDRESS_FRAC_BIT;
    }

    private long getSample(int sample, int volume) {
        long v = sample * getVolume(volume);
        return volumeAccShift >= 0 ? v >> volumeAccShift : v << -volumeAccShift;
    }

    private int interpolate(int sample1, int sample2, long accum) {
        int shifted = 1 << ADDRESS_FRAC_BIT;
        int mask = shifted - 1;
        accum &= mask & addressAccMask;
        return (int) ((sample1 * (long) (shifted - accum) + sample2 * accum) >> ADDRESS_FRAC_BIT);
    }

    private static int applyLowpass(int out, int cutoff, int in) {
        return ((cutoff >> FILTER_SHIFT) * (out - in) / (1 << FILTER_BIT)) + in;
    }

    private static int applyHighpass(int out, int cutoff, int in, int prev) {
        return out - prev + ((cutoff >> FILTER_SHIFT) * in) / (1 << (FILTER_BIT + 1)) + in / 2;
    }

    private static int getLp(int control) {
        return (control >> 10) & LP_MASK;
    }

    private static int getCa(int control) {
        return (control >> 8) & 3;
    }

    private static int getBank(int control) {
        return (control >> 2) & 1;
    }

    private int applyFilters(Voice voice, int sample) {
        // pole 1 is always low-pass using K1
        sample = applyLowpass(sample, voice.k1, voice.o1n1);
        voice.o1n1 = sample;

        // pole 2 is always low-pass using K1
        sample = applyLowpass(sample, voice.k1, voice.o2n1);
        voice.o2n2 = voice.o2n1;
        voice.o2n1 = sample;

        // remaining poles depend on the current filter setting
        switch (getLp(voice.control)) {
            case 0 -> {
                // pole 3 is high-pass using K2
                sample = applyHighpass(sample, voice.k2, voice.o3n1, voice.o2n2);
                voice.o3n2 = voice.o3n1;
                voice.o3n1 = sample;
                // pole 4 is high-pass using K2
                sample = applyHighpass(sample, voice.k2, voice.o4n1, voice.o3n2);
                voice.o4n1 = sample;
            }
            case LP3 -> {
                // pole 3 is low-pass using K1
                sample = applyLowpass(sample, voice.k1, voice.o3n1);
                voice.o3n2 = voice.o3n1;
                voice.o3n1 = sample;
                // pole 4 is high-pass using K2
                sample = applyHighpass(sample, voice.k2, voice.o4n1, voice.o3n2);
                voice.o4n1 = sample;
            }
            case LP4 -> {
                // pole 3 is low-pass using K2
                sample = applyLowpass(sample, voice.k2, voice.o3n1);
                voice.o3n2 = voice.o3n1;
                voice.o3n1 = sample;
                // pole 4 is low-pass using K2
                sample = applyLowpass(sample, voice.k2, voice.o4n1);
                voice.o4n1 = sample;
            }
            default -> {
                // pole 3 is low-pass using K1
                sample = applyLowpass(sample, voice.k1, voice.o3n1);
                voice.o3n2 = voice.o3n1;
                voice.o3n1 = sample;
                // pole 4 is low-pass using K2
                sample = applyLowpass(sample, voice.k2, voice.o4n1);
                voice.o4n1 = sample;
            }
        }
        return sample;
    }

    /** BLE is ignored when LPE = 0 */
    private long checkForEndForward(Voice voice, long accum) {
        if (Long.compareUnsigned(accum, voice.end) > 0) {
            if ((voice.control & CONTROL_IRQE) != 0)
                voice.control |= CONTROL_IRQ;
            switch (voice.control & CONTROL_LOOPMASK) {
                case 0, CONTROL_BLE -> voice.control |= CONTROL_STOP0;
                case CONTROL_LPE -> accum = (voice.start + (accum - voice.end)) & addressAccMask;
                default -> {
                    accum = (voice.end - (accum - voice.end)) & addressAccMask;
                    voice.control ^= CONTROL_DIR;
                }
            }
        }
        return accum;
    }

    private long checkForEndReverse(Voice voice, long accum) {
        if (Long.compareUnsigned(accum, voice.start) < 0) {
            if ((voice.control & CONTROL_IRQE) != 0)
                voice.control |= CONTROL_IRQ;
            switch (voice.control & CONTROL_LOOPMASK) {
                case 0, CONTROL_BLE -> voice.control |= CONTROL_STOP0;
                case CONTROL_LPE -> accum = (voice.end - (voice.start - accum)) & addressAccMask;
                default -> {
                    accum = (voice.start + (voice.start - accum)) & addressAccMask;
                    voice.control ^= CONTROL_DIR;
                }
            }
        }
        return accum;
    }

    private int readSample(Voice voice, long addr) {
        byte[] rom = banks[getBank(voice.control)];
        if (rom == null) return 0;
        int a = (int) ((addr & 0xf_ffff) << 1);
        if (a + 1 >= rom.length) return 0;
        return ((rom[a + 1] & 0xff) << 8) | (rom[a] & 0xff);
    }

    private void generatePcm(Voice voice, long[] dest, int l) {
        long freqcount = voice.freqcount;
        long accum = voice.accum & addressAccMask;

        if ((voice.control & CONTROL_STOPMASK) == 0) {
            int val1 = (short) readSample(voice, getIntegerAddr(accum, 0));
            int val2 = (short) readSample(voice, getIntegerAddr(accum, 1));

            val1 = interpolate(val1, val2, accum);
            if ((voice.control & CONTROL_DIR) == 0) {
                accum = (accum + freqcount) & addressAccMask;
            } else {
                accum = (accum - freqcount) & addressAccMask;
            }

            val1 = applyFilters(voice, val1);

            dest[l] += getSample(val1, voice.lvol);
            dest[l + 1] += getSample(val1, voice.rvol);

            if ((voice.control & CONTROL_DIR) == 0) {
                accum = checkForEndForward(voice, accum);
            } else {
                accum = checkForEndReverse(voice, accum);
            }
        }
        voice.accum = accum;
    }

    private void generateIrq(Voice voice, int v) {
        if ((voice.control & CONTROL_IRQ) != 0) {
            // only update voice vector if existing IRQ is acked by host
            if ((irqv & 0x80) != 0) {
                irqv = v & 0x1f;
                voice.control &= ~CONTROL_IRQ;
                irqCallback.accept(1);
            }
        }
    }

    private final long[] cursample = new long[8];

    /**
     * renders samples, the outputs are 20 bit and come out scaled to -1 .. 1
     *
     * @param outputs {@link #getOutputs()} arrays
     */
    public void generate(float[][] outputs, int offset, int samples) {
        for (int s = 0; s < samples; s++) {
            java.util.Arrays.fill(cursample, 0);
            for (int v = 0; v <= activeVoices; v++) {
                Voice voice = voices[v];
                int channel = getCa(voice.control) % channels;
                generatePcm(voice, cursample, channel << 1);
                generateIrq(voice, v);
            }
            for (int c = 0; c < channels * 2; c++) {
                long value = Math.max(-(1 << 19), Math.min((1 << 19) - 1, cursample[c]));
                outputs[c][offset + s] = value / (float) (1 << 19);
            }
        }
    }

    private void setActive(int data) {
        activeVoices = data & 0x1f;
        sampleRate = masterClock / (16 * (activeVoices + 1));
        sampleRateChanged.accept(sampleRate);
    }

    private static boolean lo(int mask) {
        return (mask & 0x00ff) != 0;
    }

    private static boolean hi(int mask) {
        return (mask & 0xff00) != 0;
    }

    private void regWriteLow(Voice voice, int offset, int data, int mask) {
        switch (offset) {
            case 0x00 -> { // CR
                voice.control |= 0xf000; // bit 15-12 always 1
                if (lo(mask)) {
                    voice.control &= ~0x00ff;
                    voice.control |= data & 0x00ff;
                }
                if (hi(mask))
                    voice.control = (voice.control & ~0x0f00) | (data & 0x0f00);
            }
            case 0x01 -> { // FC
                if (lo(mask))
                    voice.freqcount = (voice.freqcount & ~accShifted(0x00fe, 1)) | accShifted(data & 0x00fe, 1);
                if (hi(mask))
                    voice.freqcount = (voice.freqcount & ~accShifted(0xff00, 1)) | accShifted(data & 0xff00, 1);
            }
            case 0x02 -> { // STRT (hi)
                if (lo(mask))
                    voice.start = (voice.start & ~accShifted(0x00ff0000L)) | accShifted((long) (data & 0x00ff) << 16);
                if (hi(mask))
                    voice.start = (voice.start & ~accShifted(0x1f000000L)) | accShifted((long) (data & 0x1f00) << 16);
            }
            case 0x03 -> { // STRT (lo)
                if (lo(mask))
                    voice.start = (voice.start & ~accShifted(0x000000e0L)) | accShifted(data & 0x00e0);
                if (hi(mask))
                    voice.start = (voice.start & ~accShifted(0x0000ff00L)) | accShifted(data & 0xff00);
            }
            case 0x04 -> { // END (hi)
                if (lo(mask))
                    voice.end = (voice.end & ~accShifted(0x00ff0000L)) | accShifted((long) (data & 0x00ff) << 16);
                if (hi(mask))
                    voice.end = (voice.end & ~accShifted(0x1f000000L)) | accShifted((long) (data & 0x1f00) << 16);
            }
            case 0x05 -> { // END (lo)
                if (lo(mask))
                    voice.end = (voice.end & ~accShifted(0x000000e0L)) | accShifted(data & 0x00e0);
                if (hi(mask))
                    voice.end = (voice.end & ~accShifted(0x0000ff00L)) | accShifted(data & 0xff00);
            }
            case 0x06 -> { // K2
                if (lo(mask))
                    voice.k2 = (voice.k2 & ~0x00f0) | (data & 0x00f0);
                if (hi(mask))
                    voice.k2 = (voice.k2 & ~0xff00) | (data & 0xff00);
            }
            case 0x07 -> { // K1
                if (lo(mask))
                    voice.k1 = (voice.k1 & ~0x00f0) | (data & 0x00f0);
                if (hi(mask))
                    voice.k1 = (voice.k1 & ~0xff00) | (data & 0xff00);
            }
            case 0x08 -> { // LVOL
                if (hi(mask))
                    voice.lvol = (voice.lvol & ~0xff) | ((data & 0xff00) >> 8);
            }
            case 0x09 -> { // RVOL
                if (hi(mask))
                    voice.rvol = (voice.rvol & ~0xff) | ((data & 0xff00) >> 8);
            }
            case 0x0a -> { // ACC (hi)
                if (lo(mask))
                    voice.accum = (voice.accum & ~accShifted(0x00ff0000L)) | accShifted((long) (data & 0x00ff) << 16);
                if (hi(mask))
                    voice.accum = (voice.accum & ~accShifted(0x1f000000L)) | accShifted((long) (data & 0x1f00) << 16);
            }
            case 0x0b -> { // ACC (lo)
                if (lo(mask))
                    voice.accum = (voice.accum & ~accShifted(0x000000ffL)) | accShifted(data & 0x00ff);
                if (hi(mask))
                    voice.accum = (voice.accum & ~accShifted(0x0000ff00L)) | accShifted(data & 0xff00);
            }
            default -> regWriteGlobal(offset, data, mask);
        }
    }

    private void regWriteHigh(Voice voice, int offset, int data, int mask) {
        switch (offset) {
            case 0x00 -> { // CR
                voice.control |= 0xf000;
                if (lo(mask))
                    voice.control = (voice.control & ~0x00ff) | (data & 0x00ff);
                if (hi(mask))
                    voice.control = (voice.control & ~0x0f00) | (data & 0x0f00);
            }
            case 0x01 -> voice.o4n1 = writeFilter(voice.o4n1, data, mask);
            case 0x02 -> voice.o3n1 = writeFilter(voice.o3n1, data, mask);
            case 0x03 -> voice.o3n2 = writeFilter(voice.o3n2, data, mask);
            case 0x04 -> voice.o2n1 = writeFilter(voice.o2n1, data, mask);
            case 0x05 -> voice.o2n2 = writeFilter(voice.o2n2, data, mask);
            case 0x06 -> voice.o1n1 = writeFilter(voice.o1n1, data, mask);
            default -> regWriteGlobal(offset, data, mask);
        }
    }

    private static int writeFilter(int value, int data, int mask) {
        if (lo(mask))
            value = (value & ~0x00ff) | (data & 0x00ff);
        if (hi(mask))
            value = (short) ((value & ~0xff00) | (data & 0xff00));
        return value;
    }

    private void regWriteTest(int offset, int data, int mask) {
        if (offset == 0x08) { // SERMODE
            mode |= 0x7f8; // bit 10-3 always 1
            if (hi(mask))
                mode = (mode & ~0xf800) | (data & 0xf800);
            if (lo(mask))
                mode = (mode & ~0x0007) | (data & 0x0007);
        } else {
            regWriteGlobal(offset, data, mask);
        }
    }

    /** ACT and PAGE, which every page reaches */
    private void regWriteGlobal(int offset, int data, int mask) {
        switch (offset) {
            case 0x0d -> { // ACT
                if (lo(mask)) setActive(data);
            }
            case 0x0f -> { // PAGE
                if (lo(mask)) currentPage = data & 0x7f;
            }
            default -> {}
        }
    }

    /**
     * a host write
     *
     * @param offset the word register
     * @param mask which bytes of the word, 0xff00 high, 0x00ff low
     */
    public void write(int offset, int data, int mask) {
        Voice voice = voices[currentPage & 0x1f];
        update.run();
        offset &= 0x0f;
        if (currentPage < 0x20)
            regWriteLow(voice, offset, data, mask);
        else if (currentPage < 0x40)
            regWriteHigh(voice, offset, data, mask);
        else
            regWriteTest(offset, data, mask);
    }

    private int regReadLow(Voice voice, int offset) {
        return switch (offset) {
            case 0x00 -> voice.control | 0xf000;
            case 0x01 -> (int) accRes(voice.freqcount, 1);
            case 0x02 -> (int) (accRes(voice.start) >>> 16);
            case 0x03 -> (int) accRes(voice.start);
            case 0x04 -> (int) (accRes(voice.end) >>> 16);
            case 0x05 -> (int) accRes(voice.end);
            case 0x06 -> voice.k2;
            case 0x07 -> voice.k1;
            case 0x08 -> voice.lvol << 8;
            case 0x09 -> voice.rvol << 8;
            case 0x0a -> (int) (accRes(voice.accum) >>> 16);
            case 0x0b -> (int) accRes(voice.accum);
            default -> regReadGlobal(offset);
        } & 0xffff;
    }

    private int regReadHigh(Voice voice, int offset) {
        return switch (offset) {
            case 0x00 -> voice.control | 0xf000;
            case 0x01 -> voice.o4n1;
            case 0x02 -> voice.o3n1;
            case 0x03 -> voice.o3n2;
            case 0x04 -> voice.o2n1;
            case 0x05 -> voice.o2n2;
            case 0x06 -> {
                // the taito f3 games read raw rom through a stopped voice
                if ((voice.control & CONTROL_STOPMASK) != 0) {
                    voice.o1n1 = readSample(voice, getIntegerAddr(voice.accum, 0));
                }
                yield voice.o1n1;
            }
            default -> regReadGlobal(offset);
        } & 0xffff;
    }

    private int regReadTest(int offset) {
        return switch (offset) {
            case 0x08 -> mode | 0x7f8;
            case 0x09 -> readPort.getAsInt() & 0xffc0; // 10 bit, 15:6
            default -> regReadGlobal(offset);
        } & 0xffff;
    }

    private int regReadGlobal(int offset) {
        return switch (offset) {
            case 0x0d -> activeVoices;
            case 0x0e -> {
                int result = irqv;
                // the host acks the voice irq by reading the vector
                irqv = 0x80;
                irqCallback.accept(0);
                yield result;
            }
            case 0x0f -> currentPage;
            default -> 0;
        };
    }

    public int read(int offset) {
        Voice voice = voices[currentPage & 0x1f];
        update.run();
        offset &= 0x0f;
        if (currentPage < 0x20)
            return regReadLow(voice, offset);
        else if (currentPage < 0x40)
            return regReadHigh(voice, offset);
        else
            return regReadTest(offset);
    }
}
