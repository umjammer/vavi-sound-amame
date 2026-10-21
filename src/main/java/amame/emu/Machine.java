/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * One emulated machine and its scheduler.
 * <p>
 * Everything is instance state, so any number of machines run side by side, one per plug-in.
 * The machine is driven from the outside, by the block of audio the host wants: {@link #render}
 * runs the cpus and the timers up to the end of the block, interleaved in slices of at most
 * {@link #quantum}, and mixes the streams of the chips down to the host rate.
 * <p>
 * The emulation is single threaded and runs on whatever thread calls {@link #render}. Anything
 * from another thread, a button of the panel or a note of the virtual keyboard, is posted with
 * {@link #post} and runs at the start of the next block.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public abstract class Machine {

    /** stereo */
    public static final int SPEAKERS = 2;

    /** the longest the cpus run apart */
    protected double quantum = 50e-6;

    private final List<Cpu> cpus = new ArrayList<>();
    private final List<EmuTimer> timers = new ArrayList<>();
    private final List<SoundStream> streams = new ArrayList<>();

    /** the time of the scheduler, the cpus may be a little ahead of it */
    private double now;

    /** the cpu running at the moment, null between slices */
    private Cpu executing;

    /** host frames rendered so far, which the block boundaries are computed from */
    private long hostPosition;
    private double hostRate;

    private final ConcurrentLinkedQueue<Runnable> posted = new ConcurrentLinkedQueue<>();

    /** the short name, what mame calls the driver, e.g. "fb01" */
    public abstract String getName();

    /** the long name, e.g. "Yamaha FB-01" */
    public abstract String getDescription();

    /** builds the machine, loads the roms and resets it */
    public abstract void start() throws java.io.IOException;

    /** a hardware reset, the cpus and every device */
    public void reset() {
        for (Cpu cpu : cpus) {
            cpu.reset();
        }
    }

    /** the front panel, what is to be seen and pressed */
    public abstract Panel getPanel();

    /** midi going into the machine, a whole message, sysex included */
    public abstract void midiIn(byte[] message);

    /** the battery backed memory, what the machine remembers when it is switched off, null for none */
    public byte[] getNvram() {
        return null;
    }

    /** restores the battery backed memory, before {@link #reset()} or after, as the machine allows */
    public void setNvram(byte[] data) {
    }

    /** where the machine sends its midi out */
    public void setMidiOut(MidiOut midiOut) {
        this.midiOut = midiOut;
    }

    /** midi coming out of the machine, byte by byte as the uart sends them */
    @FunctionalInterface
    public interface MidiOut {
        void write(int data);
    }

    protected MidiOut midiOut = data -> {};

    // building

    protected <T extends Cpu> T addCpu(T cpu) {
        cpu.localTime = now;
        cpus.add(cpu);
        return cpu;
    }

    public EmuTimer timer(Runnable callback) {
        EmuTimer timer = new EmuTimer(this, callback);
        timers.add(timer);
        return timer;
    }

    public SoundStream stream(int channels, double rate, SoundStream.Generator generator) {
        SoundStream stream = new SoundStream(this, channels, rate, generator);
        streams.add(stream);
        return stream;
    }

    public List<Cpu> getCpus() {
        return Collections.unmodifiableList(cpus);
    }

    // running

    /** the present, which is the cpu's own time while one is running */
    public double time() {
        return executing != null ? executing.currentTime() : now;
    }

    /** runs a task on the emulation thread before the next block */
    public void post(Runnable task) {
        posted.add(task);
    }

    /** runs the machine up to a time */
    public void run(double until) {
        while (true) {
            fireTimers();
            if (now >= until) break;
            double target = Math.min(until, now + quantum);
            double next = nextTimer();
            if (next < target) target = Math.max(next, now);
            for (Cpu cpu : cpus) {
                if (cpu.localTime >= target) continue;
                int cycles = (int) Math.ceil((target - cpu.localTime) * cpu.clock);
                if (cycles <= 0) continue;
                executing = cpu;
                try {
                    cpu.run(cycles);
                } finally {
                    executing = null;
                }
            }
            now = target;
        }
    }

    private double nextTimer() {
        double next = EmuTimer.NEVER;
        for (EmuTimer timer : timers) {
            if (timer.expire < next) next = timer.expire;
        }
        return next;
    }

    private void fireTimers() {
        while (true) {
            EmuTimer due = null;
            for (EmuTimer timer : timers) {
                if (timer.expire <= now && (due == null || timer.expire < due.expire)) due = timer;
            }
            if (due == null) return;
            due.fire();
        }
    }

    /**
     * Renders one block of audio for the host.
     *
     * @param outputs one array per speaker, filled from 0
     * @param frames how many
     * @param rate the host's sample rate
     * @param events what to do at which frame of the block, in frame order, may be null
     */
    public void render(float[][] outputs, int frames, double rate, List<TimedEvent> events) {
        if (rate != hostRate) {
            // a new rate starts the count afresh from where the machine is
            hostRate = rate;
            hostPosition = (long) Math.ceil(now * rate);
        }
        Runnable task;
        while ((task = posted.poll()) != null) {
            task.run();
        }

        long start = hostPosition;
        if (events != null) {
            for (TimedEvent event : events) {
                int frame = Math.max(0, Math.min(frames - 1, event.frame()));
                run((start + frame) / rate);
                event.task().run();
            }
        }
        double end = (start + frames) / rate;
        run(end);
        for (SoundStream stream : streams) {
            stream.update(end);
        }

        for (int i = 0; i < Math.min(outputs.length, SPEAKERS); i++) {
            java.util.Arrays.fill(outputs[i], 0, frames, 0f);
        }
        for (SoundStream stream : streams) {
            for (int ch = 0; ch < stream.getChannels(); ch++) {
                float[] gains = stream.routes[ch];
                for (int sp = 0; sp < Math.min(outputs.length, SPEAKERS); sp++) {
                    float gain = gains[sp];
                    if (gain == 0) continue;
                    float[] out = outputs[sp];
                    for (int k = 0; k < frames; k++) {
                        out[k] += stream.sample(ch, (start + k) / rate) * gain;
                    }
                }
            }
            stream.discard(end);
        }
        hostPosition = start + frames;
    }

    /** something to do at a frame of a block, a midi event of the host */
    public record TimedEvent(int frame, Runnable task) {}
}
