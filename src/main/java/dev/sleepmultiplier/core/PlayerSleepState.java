package dev.sleepmultiplier.core;

final class PlayerSleepState {
    private boolean inBed;
    private boolean recentSleeper;
    private boolean phantomProtectionGranted;
    private long accumulatedSleepingNanos;
    private long currentSleepStartNanos = -1L;

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
        if (currentSleepStartNanos < 0L) {
            currentSleepStartNanos = nowNanos;
        }
    }

    public void stopSleeping(long nowNanos) {
        if (currentSleepStartNanos >= 0L) {
            accumulatedSleepingNanos += Math.max(0L, nowNanos - currentSleepStartNanos);
            currentSleepStartNanos = -1L;
        }
    }

    public long getTotalSleepingNanos(long nowNanos) {
        if (currentSleepStartNanos < 0L) {
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
