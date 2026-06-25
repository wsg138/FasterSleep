package dev.sleepmultiplier.core;

import dev.sleepmultiplier.config.MessageConfig;
import dev.sleepmultiplier.config.SleepConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SleepService {
    private static final long NO_TICKS_TO_ADVANCE = 0L;
    private static final long NO_FRACTIONAL_TICKS = 0L;

    private final Logger logger;
    private final Map<UUID, PlayerSleepState> trackedPlayers = new ConcurrentHashMap<>();

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
        fractionalTickAccumulator = NO_FRACTIONAL_TICKS;
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

        return Optional.of(SleepFeedback.fromSummary(summarize()));
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
        World world = resolveTargetWorld();
        if (world == null) {
            return;
        }

        if (!updateNightState(world)) {
            return;
        }

        advanceNight(world);
    }

    private World resolveTargetWorld() {
        World world = Bukkit.getWorld(config.targetWorldName());
        if (world != null) {
            return world;
        }

        if (wasNight || !trackedPlayers.isEmpty()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Configured sleep world '" + config.targetWorldName()
                        + "' is not loaded. Clearing active sleep state.");
            }
            clear();
        }
        return null;
    }

    private boolean updateNightState(World world) {
        boolean isNight = config.isNight(world.getTime());
        if (wasNight && !isNight) {
            clear();
        }
        wasNight = isNight;
        return isNight;
    }

    private void advanceNight(World world) {
        NightSummary summary = summarizeAndUpdate(world);
        if (summary.totalSleepers() <= 0) {
            fractionalTickAccumulator = NO_FRACTIONAL_TICKS;
            return;
        }

        fractionalTickAccumulator += summary.totalContributionMilliTicks();
        long ticksToAdvance = fractionalTickAccumulator / SleepConfig.FIXED_POINT_SCALE;
        fractionalTickAccumulator %= SleepConfig.FIXED_POINT_SCALE;

        if (ticksToAdvance > NO_TICKS_TO_ADVANCE) {
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

    private NightSummary summarize() {
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

        for (Map.Entry<UUID, PlayerSleepState> entry : trackedPlayers.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (shouldRemoveTrackedPlayer(player, world)) {
                trackedPlayers.remove(entry.getKey());
                continue;
            }

            PlayerSleepState state = entry.getValue();
            if (updateSleepingState(player, state, nowNanos)) {
                activeSleepers++;
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

    private boolean shouldRemoveTrackedPlayer(Player player, World world) {
        return player == null || !player.isOnline() || player.isDead() || !world.equals(player.getWorld());
    }

    private boolean updateSleepingState(Player player, PlayerSleepState state, long nowNanos) {
        boolean sleepingNow = player.isSleeping();
        if (state.isInBed() && !sleepingNow) {
            state.setInBed(false);
            state.stopSleeping(nowNanos);
        }

        if (!sleepingNow) {
            return false;
        }

        state.setInBed(true);
        state.setRecentSleeper(true);
        state.startSleeping(nowNanos);
        grantPhantomProtectionIfReady(player, state, nowNanos);
        return true;
    }

    private void grantPhantomProtectionIfReady(Player player, PlayerSleepState state, long nowNanos) {
        if (config.phantomResetDisabled() || state.isPhantomProtectionGranted()) {
            return;
        }

        long requiredNanos = config.phantomResetThresholdTicks() * 50_000_000L;
        if (state.getTotalSleepingNanos(nowNanos) >= requiredNanos) {
            grantPhantomProtection(player, state);
        }
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
