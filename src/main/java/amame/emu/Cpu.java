/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

/**
 * Something the scheduler runs by cycles: a processor, or a dsp that runs its program.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public abstract class Cpu {

    /** cycles per second */
    protected final double clock;

    /** the time this cpu has run up to */
    double localTime;

    /** cycles asked of the slice running now, and the time it started at */
    private int sliceCycles;
    private double sliceStart;
    /** the cycles cut off the slice running now */
    private int aborted;

    protected Cpu(double clock) {
        this.clock = clock;
    }

    public double getClock() {
        return clock;
    }

    /**
     * runs the core
     *
     * @return the cycles actually run, may overshoot what was asked
     */
    protected abstract int execute(int cycles);

    /** the cycles of the slice running now that are left to run */
    protected abstract int remainingCycles();

    /** sets the cycles left of the slice running now, the core stops after its instruction at 0 */
    protected abstract void setRemainingCycles(int cycles);

    public abstract void reset();

    final int run(int cycles) {
        sliceCycles = cycles;
        sliceStart = localTime;
        aborted = 0;
        int ran = execute(cycles) - aborted;
        localTime = sliceStart + ran / clock;
        sliceCycles = 0;
        aborted = 0;
        return ran;
    }

    /** the time this cpu is at in the middle of running a slice */
    final double currentTime() {
        return sliceStart + (sliceCycles - remainingCycles() - aborted) / clock;
    }

    /**
     * ends the slice running now at a time before its end, mame's abort_timeslice: something
     * is due then that the cpu may want to see
     */
    final void abortSlice(double at) {
        int remaining = remainingCycles();
        int keep = Math.max(0, (int) Math.ceil((at - currentTime()) * clock));
        if (keep >= remaining) return;
        aborted += remaining - keep;
        setRemainingCycles(keep);
    }
}
