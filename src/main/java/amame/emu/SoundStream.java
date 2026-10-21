/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

import java.util.Arrays;


/**
 * The output of one sound chip at its own rate.
 * <p>
 * A chip renders lazily: its stream is brought up to the present before anything that changes
 * the sound is written to it, the way mame's streams work, and once more at the end of every
 * block the host asks for. The samples wait here until the {@link Machine} mixes them down to the
 * host rate.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class SoundStream {

    /** renders samples of a chip */
    @FunctionalInterface
    public interface Generator {

        /**
         * @param outputs one array per output channel, written from offset
         */
        void generate(float[][] outputs, int offset, int samples);
    }

    private final Machine machine;
    private final Generator generator;
    private final int channels;
    private double rate;

    /** samples rendered since the start */
    private long position;

    /** samples waiting to be mixed, buffer[ch][0] is sample {@link #base} */
    private float[][] buffer;
    private long base;
    private int length;

    /** the gain of every output channel into each speaker, [channel][speaker] */
    final float[][] routes;

    SoundStream(Machine machine, int channels, double rate, Generator generator) {
        this.machine = machine;
        this.channels = channels;
        this.rate = rate;
        this.generator = generator;
        this.buffer = new float[channels][1024];
        this.routes = new float[channels][Machine.SPEAKERS];
        this.position = (long) Math.floor(machine.time() * rate);
        this.base = position;
    }

    public double getRate() {
        return rate;
    }

    /**
     * changes the rate, from now on. the samples waiting to be mixed are taken to be at the new
     * rate too, which shifts them by a fraction of a block, once.
     */
    public void setRate(double rate) {
        if (rate == this.rate || rate <= 0) return;
        update();
        this.rate = rate;
        position = (long) Math.floor(machine.time() * rate);
        base = position - length;
    }

    public int getChannels() {
        return channels;
    }

    /** sends an output channel of the chip to a speaker */
    public SoundStream route(int channel, int speaker, float gain) {
        routes[channel][speaker] = gain;
        return this;
    }

    /** renders up to the present */
    public void update() {
        update(machine.time());
    }

    void update(double time) {
        long target = (long) Math.floor(time * rate);
        int samples = (int) (target - position);
        if (samples <= 0) return;
        ensure(length + samples);
        generator.generate(buffer, length, samples);
        length += samples;
        position = target;
    }

    private void ensure(int capacity) {
        if (capacity <= buffer[0].length) return;
        int size = Math.max(capacity, buffer[0].length * 2);
        for (int ch = 0; ch < channels; ch++) {
            buffer[ch] = Arrays.copyOf(buffer[ch], size);
        }
    }

    /**
     * the sample of a channel at a time, linearly interpolated. a time past what has been
     * rendered gets the last sample.
     */
    float sample(int channel, double time) {
        double index = time * rate - base;
        if (length == 0) return 0;
        if (index <= 0) return buffer[channel][0];
        int i = (int) index;
        if (i >= length - 1) return buffer[channel][length - 1];
        float frac = (float) (index - i);
        float a = buffer[channel][i];
        return a + (buffer[channel][i + 1] - a) * frac;
    }

    /** drops what is older than the time, keeping one sample for the interpolation */
    void discard(double time) {
        int drop = (int) (Math.floor(time * rate) - base) - 1;
        if (drop <= 0) return;
        drop = Math.min(drop, length);
        for (int ch = 0; ch < channels; ch++) {
            System.arraycopy(buffer[ch], drop, buffer[ch], 0, length - drop);
        }
        length -= drop;
        base += drop;
    }
}
