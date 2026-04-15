package dev.sleepmultiplier.core;

public record SleepFeedback(
        int activeSleepers,
        int recentSleepers,
        int totalSleepers,
        long totalContributionMilliTicks
) {
    public static SleepFeedback fromSummary(NightSummary summary) {
        return new SleepFeedback(
                summary.activeSleepers(),
                summary.recentSleepers(),
                summary.totalSleepers(),
                summary.totalContributionMilliTicks()
        );
    }
}
