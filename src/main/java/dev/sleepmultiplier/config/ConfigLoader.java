package dev.sleepmultiplier.config;

import dev.sleepmultiplier.SleepMultiplier;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigLoader {
    private static final double MINIMUM_SCALED_TICKS = 0.0D;

    private static final String DEFAULT_SLEEP_MESSAGE =
            "&aSleep registered&7. &fSleepers: &b{sleepers} &7| &fNight speed: &b{speed}";
    private static final String DEFAULT_PHANTOMS_DISABLED =
            "&aPhantom timer reset&7. &fYou are protected from phantoms.";
    private static final String DEFAULT_RELOAD_SUCCESS =
            "&aSleepMultiplier configuration reloaded.";
    private static final String DEFAULT_RELOAD_FAILURE =
            "&cSleepMultiplier configuration reload failed. Check console.";

    private final SleepMultiplier plugin;

    public ConfigLoader(SleepMultiplier plugin) {
        this.plugin = plugin;
    }

    public SleepConfig load() {
        FileConfiguration config = plugin.getConfig();

        String targetWorldName = requireText(
                config.getString("world.target-name", "world"),
                "world.target-name"
        );

        int nightStartTick = readTick(config, "world.night-start-tick", 12541);
        int nightEndTick = readTick(config, "world.night-end-tick", 23458);

        int activeSleepMilliTicks = readScaledTicks(
                config,
                "speed.extra-ticks-per-active-sleeper",
                config.contains("speed.extra-ticks-per-active-sleeper")
                        ? config.getDouble("speed.extra-ticks-per-active-sleeper")
                        : config.getDouble("speed.extra-ticks-per-sleeper", 0.5D)
        );
        int recentSleepMilliTicks = readScaledTicks(
                config,
                "speed.extra-ticks-per-recent-sleeper",
                config.getDouble("speed.extra-ticks-per-recent-sleeper", 0.0D)
        );

        long phantomResetTicks = readPhantomThresholdTicks(
                config.getLong("phantoms.disable-after-seconds-in-bed", 30L)
        );

        MessageConfig messages = new MessageConfig(
                config.getString("messages.sleep-registered", DEFAULT_SLEEP_MESSAGE),
                config.getString("messages.phantoms-disabled", DEFAULT_PHANTOMS_DISABLED),
                config.getString("messages.reload-success", DEFAULT_RELOAD_SUCCESS),
                config.getString("messages.reload-failed", DEFAULT_RELOAD_FAILURE)
        );

        return new SleepConfig(
                targetWorldName,
                nightStartTick,
                nightEndTick,
                activeSleepMilliTicks,
                recentSleepMilliTicks,
                phantomResetTicks,
                messages
        );
    }

    private static String requireText(String input, String path) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Missing or blank config value at '" + path + "'");
        }
        return input.strip();
    }

    private static int readTick(FileConfiguration config, String path, int defaultValue) {
        int value = config.getInt(path, defaultValue);
        if (value < 0 || value > 23999) {
            throw new IllegalArgumentException("Config '" + path + "' must be between 0 and 23999.");
        }
        return value;
    }

    private static int readScaledTicks(FileConfiguration config, String path, double defaultValue) {
        double value = config.getDouble(path, defaultValue);
        if (value < MINIMUM_SCALED_TICKS) {
            throw new IllegalArgumentException("Config '" + path + "' cannot be negative.");
        }

        long scaled = Math.round(value * SleepConfig.FIXED_POINT_SCALE);
        if (scaled > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Config '" + path + "' is too large.");
        }

        return (int) scaled;
    }

    private static long readPhantomThresholdTicks(long seconds) {
        if (seconds < -1L) {
            throw new IllegalArgumentException("Config 'phantoms.disable-after-seconds-in-bed' must be -1 or higher.");
        }
        if (seconds == -1L) {
            return -1L;
        }
        return Math.multiplyExact(seconds, 20L);
    }
}
