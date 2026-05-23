package dev.sleepmultiplier.core;

import dev.sleepmultiplier.config.MessageConfig;
import dev.sleepmultiplier.config.SleepConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class SleepService {
    private final Logger logger;
    private final Map<UUID, PlayerSleepState> trackedPlayers = new HashMap<>();

    private SleepConfig config;
    private boolean wasNight;
    private long fractionalTickAccumulator;

    public SleepService(Logger logger, SleepConfig config) {
        this.logger = logger;
        this.config = config;
    }

    public void reload(SleepConfig config) {
        this.config = config;
        clear();
    }

    public void clear() {
        trackedPlayers.clear();
        wasNight = false;
        fractionalTickAccumulator = 0;
    }

    public Optional<SleepFeedback> handleBedEnter(Player player) {
        World world = player.getWorld();
        if (!config.isTargetWorld(world.getName()) || !config.isNight(world.getTime())) {
            return Optional.empty();
        }

        PlayerSleepState state = trackedPlayers.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerSleepState());
        state.setInBed(true);

        if (!config.phantomResetDisabled() && config.phantomResetThresholdTicks() == 0L) {
            grantPhantomProtection(player, state);
        }

        return Optional.of(SleepFeedback.fromSummary(summarize(world)));
    }

    public void handleBedLeave(Player player) {
        PlayerSleepState state = trackedPlayers.get(player.getUniqueId());
        if (state != null) {
            state.setInBed(false);
            state.stopSleeping(System.nanoTime());
        }
    }

    public void handlePlayerQuit(Player player) {
        trackedPlayers.remove(player.getUniqueId());
    }

    public void handleWorldChange(Player player, World fromWorld) {
        if (config.isTargetWorld(fromWorld.getName())) {
            trackedPlayers.remove(player.getUniqueId());
        }
    }

    public void tick() {
        World world = Bukkit.getWorld(config.targetWorldName());
        if (world == null) {
            if (wasNight || !trackedPlayers.isEmpty()) {
                logger.warning("Configured sleep world '" + config.targetWorldName() + "' is not loaded. Clearing active sleep state.");
                clear();
            }
            return;
        }

        boolean isNight = config.isNight(world.getTime());
        if (wasNight && !isNight) {
            clear();
        }
        wasNight = isNight;

        if (!isNight) {
            return;
        }

        NightSummary summary = summarizeAndUpdate(world);
        if (summary.totalSleepers() <= 0) {
            fractionalTickAccumulator = 0;
            return;
        }

        fractionalTickAccumulator += summary.totalContributionMilliTicks();
        long ticksToAdvance = fractionalTickAccumulator / SleepConfig.FIXED_POINT_SCALE;
        fractionalTickAccumulator %= SleepConfig.FIXED_POINT_SCALE;

        if (ticksToAdvance > 0L) {
            world.setFullTime(world.getFullTime() + ticksToAdvance);
        }
    }

    public String formatSleepRegisteredMessage(SleepFeedback feedback) {
        MessageConfig messages = config.messages();
        String template = messages.sleepRegistered();
        if (template == null || template.isEmpty()) {
            return "";
        }

        return colorize(template
                .replace("{sleepers}", Integer.toString(feedback.totalSleepers()))
                .replace("{active_sleepers}", Integer.toString(feedback.activeSleepers()))
                .replace("{recent_sleepers}", Integer.toString(feedback.recentSleepers()))
                .replace("{speed}", formatSpeed(feedback.totalContributionMilliTicks())));
    }

    public String formatReloadSuccessMessage() {
        return colorize(config.messages().reloadSuccess());
    }

    public String formatReloadFailureMessage() {
        return colorize(config.messages().reloadFailed());
    }

    private String formatPhantomsDisabledMessage() {
        return colorize(config.messages().phantomsDisabled());
    }

    private NightSummary summarize(World world) {
        int activeSleepers = 0;
        int recentSleepers = 0;

        for (PlayerSleepState state : trackedPlayers.values()) {
            if (state.isInBed()) {
                activeSleepers++;
            } else if (state.isRecentSleeper()) {
                recentSleepers++;
            }
        }

        long totalContribution = (long) activeSleepers * config.activeSleepMilliTicks()
                + (long) recentSleepers * config.recentSleepMilliTicks();
        return new NightSummary(activeSleepers, recentSleepers, activeSleepers + recentSleepers, totalContribution);
    }

    private NightSummary summarizeAndUpdate(World world) {
        int activeSleepers = 0;
        int recentSleepers = 0;
        long nowNanos = System.nanoTime();

        Iterator<Map.Entry<UUID, PlayerSleepState>> iterator = trackedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerSleepState> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || player.isDead() || !world.equals(player.getWorld())) {
                iterator.remove();
                continue;
            }

            PlayerSleepState state = entry.getValue();
            boolean sleepingNow = player.isSleeping();
            if (state.isInBed() && !sleepingNow) {
                state.setInBed(false);
                state.stopSleeping(nowNanos);
            }

            if (sleepingNow) {
                state.setInBed(true);
                state.setRecentSleeper(true);
                state.startSleeping(nowNanos);
                activeSleepers++;

                if (!config.phantomResetDisabled() && !state.isPhantomProtectionGranted()) {
                    long requiredNanos = config.phantomResetThresholdTicks() * 50_000_000L;
                    if (state.getTotalSleepingNanos(nowNanos) >= requiredNanos) {
                        grantPhantomProtection(player, state);
                    }
                }
                continue;
            }

            if (state.isRecentSleeper()) {
                recentSleepers++;
            }
        }

        long totalContribution = (long) activeSleepers * config.activeSleepMilliTicks()
                + (long) recentSleepers * config.recentSleepMilliTicks();
        return new NightSummary(activeSleepers, recentSleepers, activeSleepers + recentSleepers, totalContribution);
    }

    private void grantPhantomProtection(Player player, PlayerSleepState state) {
        player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        state.setPhantomProtectionGranted(true);

        String message = formatPhantomsDisabledMessage();
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    private static String formatSpeed(long contributionMilliTicks) {
        double multiplier = 1.0D + (double) contributionMilliTicks / SleepConfig.FIXED_POINT_SCALE;
        return String.format(Locale.US, "%.2fx", multiplier);
    }

    private static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
