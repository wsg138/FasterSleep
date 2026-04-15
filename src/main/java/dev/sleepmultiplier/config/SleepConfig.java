package dev.sleepmultiplier.config;

public record SleepConfig(
        String targetWorldName,
        int nightStartTick,
        int nightEndTick,
        int activeSleepMilliTicks,
        int recentSleepMilliTicks,
        long phantomResetThresholdTicks,
        MessageConfig messages
) {
    public static final int FIXED_POINT_SCALE = 1000;

    public boolean isTargetWorld(String worldName) {
        return targetWorldName.equals(worldName);
    }

    public boolean isNight(long worldTime) {
        return worldTime >= nightStartTick && worldTime <= nightEndTick;
    }

    public boolean phantomResetDisabled() {
        return phantomResetThresholdTicks < 0L;
    }
}
