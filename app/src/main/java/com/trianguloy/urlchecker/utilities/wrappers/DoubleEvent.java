package com.trianguloy.urlchecker.utilities.wrappers;

/** A small utility to detect repeated events in a short time */
public class DoubleEvent {

    private long lastTime;

    private final long delay;

    /** Two events separated by [delayMillis] will be considered equal */
    public DoubleEvent(long delayMillis) {
        this.delay = delayMillis;
        reset();
    }

    /**
     * returns true if an event now happens less than delay from before
     * Doesn't trigger an event
     */
    public boolean check() {
        return System.currentTimeMillis() - lastTime < delay;
    }

    /** Triggers an event */
    public void trigger() {
        lastTime = System.currentTimeMillis();
    }

    /**
     * returns true if an event now happens less than delay from before
     * Triggers an event afterward
     */
    public boolean checkAndTrigger() {
        var check = check();
        trigger();
        return check;
    }

    /** Resets the event trigger, next check will always return false */
    public void reset() {
        lastTime = -1;
    }

}
