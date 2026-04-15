package dev.sleepmultiplier.core;

public record NightSummary(
        int activeSleepers,
        int recentSleepers,
        int totalSleepers,
        long totalContributionMilliTicks
) {
}
