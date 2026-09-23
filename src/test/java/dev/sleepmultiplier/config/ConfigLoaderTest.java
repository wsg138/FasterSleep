package dev.sleepmultiplier.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sleepmultiplier.SleepMultiplier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {
    private YamlConfiguration config;
    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        config = new YamlConfiguration();
        SleepMultiplier plugin = mock(SleepMultiplier.class);
        when(plugin.getConfig()).thenReturn(config);
        loader = new ConfigLoader(plugin);
    }

    @Test
    void defaultsMatchTheShippedBehavior() {
        SleepConfig loaded = loader.load();

        assertAll(
                () -> assertEquals("world", loaded.targetWorldName()),
                () -> assertEquals(12541, loaded.nightStartTick()),
                () -> assertEquals(23458, loaded.nightEndTick()),
                () -> assertEquals(500, loaded.activeSleepMilliTicks()),
                () -> assertEquals(0, loaded.recentSleepMilliTicks()),
                () -> assertEquals(600L, loaded.phantomResetThresholdTicks()),
                () -> assertEquals(
                        "&aSleep registered&7. &fSleepers: &b{sleepers} &7| &fNight speed: &b{speed}",
                        loaded.messages().sleepRegistered()
                ),
                () -> assertEquals(
                        "&aPhantom timer reset&7. &fYou are protected from phantoms.",
                        loaded.messages().phantomsDisabled()
                )
        );
    }

    @Test
    void customValuesAreTrimmedScaledAndConverted() {
        config.set("world.target-name", "  survival  ");
        config.set("world.night-start-tick", 12000);
        config.set("world.night-end-tick", 23999);
        config.set("speed.extra-ticks-per-active-sleeper", 1.2344D);
        config.set("speed.extra-ticks-per-recent-sleeper", 0.2506D);
        config.set("phantoms.disable-after-seconds-in-bed", 45L);
        config.set("messages.reload-success", "ok");
        config.set("messages.reload-failed", "bad");

        SleepConfig loaded = loader.load();

        assertAll(
                () -> assertEquals("survival", loaded.targetWorldName()),
                () -> assertEquals(12000, loaded.nightStartTick()),
                () -> assertEquals(23999, loaded.nightEndTick()),
                () -> assertEquals(1234, loaded.activeSleepMilliTicks()),
                () -> assertEquals(251, loaded.recentSleepMilliTicks()),
                () -> assertEquals(900L, loaded.phantomResetThresholdTicks()),
                () -> assertEquals("ok", loaded.messages().reloadSuccess()),
                () -> assertEquals("bad", loaded.messages().reloadFailed())
        );
    }

    @Test
    void legacyActiveSleeperSpeedIsUsedOnlyWhenTheNewKeyIsAbsent() {
        config.set("speed.extra-ticks-per-sleeper", 2.5D);
        assertEquals(2500, loader.load().activeSleepMilliTicks());

        config.set("speed.extra-ticks-per-active-sleeper", 0.75D);
        assertEquals(750, loader.load().activeSleepMilliTicks());
    }

    @Test
    void tickBoundsAreInclusive() {
        config.set("world.night-start-tick", 0);
        config.set("world.night-end-tick", 23999);

        SleepConfig loaded = loader.load();
        assertEquals(0, loaded.nightStartTick());
        assertEquals(23999, loaded.nightEndTick());
    }

    @Test
    void invalidTickBoundsFailClosed() {
        config.set("world.night-start-tick", -1);
        assertThrows(IllegalArgumentException.class, loader::load);

        config.set("world.night-start-tick", 0);
        config.set("world.night-end-tick", 24000);
        assertThrows(IllegalArgumentException.class, loader::load);
    }

    @Test
    void blankWorldNameFailsClosed() {
        config.set("world.target-name", "   ");
        assertThrows(IllegalArgumentException.class, loader::load);
    }

    @Test
    void negativeOrUnrepresentableSpeedFailsClosed() {
        config.set("speed.extra-ticks-per-active-sleeper", -0.001D);
        assertThrows(IllegalArgumentException.class, loader::load);

        config.set("speed.extra-ticks-per-active-sleeper", Double.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, loader::load);
    }

    @Test
    void phantomThresholdSupportsDisabledAndRejectsInvalidValues() {
        config.set("phantoms.disable-after-seconds-in-bed", -1L);
        assertEquals(-1L, loader.load().phantomResetThresholdTicks());

        config.set("phantoms.disable-after-seconds-in-bed", -2L);
        assertThrows(IllegalArgumentException.class, loader::load);

        config.set("phantoms.disable-after-seconds-in-bed", Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, loader::load);
    }
}
