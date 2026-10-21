/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package amame.emu;

/**
 * A timer of the emulated machine.
 * <p>
 * Times are seconds of emulated time in doubles, which keeps a precision far below one cycle
 * of any clock here for longer than anybody plays a synthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-10-21 nsano initial version <br>
 */
public final class EmuTimer {

    /** the time a disabled timer expires at */
    static final double NEVER = Double.POSITIVE_INFINITY;

    private final Machine machine;
    private final Runnable callback;

    double expire = NEVER;
    private double period;

    EmuTimer(Machine machine, Runnable callback) {
        this.machine = machine;
        this.callback = callback;
    }

    /** fires once after the delay */
    public void adjust(double delay) {
        adjust(delay, 0);
    }

    /**
     * fires after the delay, and then every period
     *
     * @param period 0 for a one shot
     */
    public void adjust(double delay, double period) {
        this.expire = machine.time() + Math.max(0, delay);
        this.period = period;
    }

    /**
     * @return whether the timer was enabled before
     */
    public boolean enable(boolean enable) {
        boolean old = isEnabled();
        if (!enable) {
            expire = NEVER;
        }
        return old;
    }

    public boolean isEnabled() {
        return expire != NEVER;
    }

    /** seconds until the timer fires, infinite when it does not */
    public double remaining() {
        return isEnabled() ? Math.max(0, expire - machine.time()) : NEVER;
    }

    /** advances the timer and runs the callback, the timer may be adjusted from the callback */
    void fire() {
        double at = expire;
        expire = period > 0 ? at + period : NEVER;
        callback.run();
    }
}
