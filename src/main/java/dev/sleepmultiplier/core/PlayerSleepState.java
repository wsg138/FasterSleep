package dev.sleepmultiplier.core;

final class PlayerSleepState {
    private static final long NO_ACTIVE_SLEEP_START = -1L;

    private boolean inBed;
    private boolean recentSleeper;
    private boolean phantomProtectionGranted;
    private long accumulatedSleepingNanos;
    private long currentSleepStartNanos = NO_ACTIVE_SLEEP_START;

    public boolean isInBed() {
        return inBed;
    }

    public void setInBed(boolean inBed) {
        this.inBed = inBed;
    }

    public boolean isRecentSleeper() {
        return recentSleeper;
    }

    public void setRecentSleeper(boolean recentSleeper) {
        this.recentSleeper = recentSleeper;
    }

    public void startSleeping(long nowNanos) {
        if (currentSleepStartNanos == NO_ACTIVE_SLEEP_START) {
            currentSleepStartNanos = nowNanos;
        }
    }

    public void stopSleeping(long nowNanos) {
        if (currentSleepStartNanos != NO_ACTIVE_SLEEP_START) {
            accumulatedSleepingNanos += Math.max(0L, nowNanos - currentSleepStartNanos);
            currentSleepStartNanos = NO_ACTIVE_SLEEP_START;
        }
    }

    public long getTotalSleepingNanos(long nowNanos) {
        if (currentSleepStartNanos == NO_ACTIVE_SLEEP_START) {
            return accumulatedSleepingNanos;
        }
        return accumulatedSleepingNanos + Math.max(0L, nowNanos - currentSleepStartNanos);
    }

    public boolean isPhantomProtectionGranted() {
        return phantomProtectionGranted;
    }

    public void setPhantomProtectionGranted(boolean phantomProtectionGranted) {
        this.phantomProtectionGranted = phantomProtectionGranted;
    }
}
