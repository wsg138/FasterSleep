package dev.sleepmultiplier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sleepmultiplier.config.MessageConfig;
import dev.sleepmultiplier.config.SleepConfig;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class SleepServiceTest {
    private static final UUID PLAYER_ONE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_TWO = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test
    void bedEnterIsIgnoredOutsideTargetWorldOrNight() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = mock(World.class);
        Player player = player(PLAYER_ONE, world);

        when(world.getName()).thenReturn("other");
        when(world.getTime()).thenReturn(15000L);
        assertTrue(service.handleBedEnter(player).isEmpty());

        when(world.getName()).thenReturn("world");
        when(world.getTime()).thenReturn(1000L);
        assertTrue(service.handleBedEnter(player).isEmpty());
    }

    @Test
    void bedEnterAndLeaveUpdateTheSummary() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        Player first = player(PLAYER_ONE, world);
        Player second = player(PLAYER_TWO, world);

        SleepFeedback firstFeedback = service.handleBedEnter(first).orElseThrow();
        assertEquals(new SleepFeedback(1, 0, 1, 1000), firstFeedback);

        service.handleBedLeave(first);
        SleepFeedback secondFeedback = service.handleBedEnter(second).orElseThrow();
        assertEquals(new SleepFeedback(1, 0, 1, 1000), secondFeedback);
    }

    @Test
    void quitAndTargetWorldChangeRemoveTrackedPlayers() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        Player first = player(PLAYER_ONE, world);
        Player second = player(PLAYER_TWO, world);

        service.handleBedEnter(first);
        service.handlePlayerQuit(first);
        assertEquals(1, service.handleBedEnter(second).orElseThrow().totalSleepers());

        service.handleWorldChange(second, world);
        assertEquals(1, service.handleBedEnter(first).orElseThrow().totalSleepers());
    }

    @Test
    void worldChangeFromAnotherWorldDoesNotRemoveTrackedPlayer() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        World other = mock(World.class);
        when(other.getName()).thenReturn("other");
        Player first = player(PLAYER_ONE, world);
        Player second = player(PLAYER_TWO, world);

        service.handleBedEnter(first);
        service.handleWorldChange(first, other);

        assertEquals(2, service.handleBedEnter(second).orElseThrow().totalSleepers());
    }

    @Test
    void zeroPhantomThresholdGrantsProtectionImmediatelyOnce() {
        SleepService service = service(config(
                1000,
                500,
                0L,
                new MessageConfig("sleep", "&aProtected", "ok", "bad")
        ));
        World world = nightWorld();
        Player player = player(PLAYER_ONE, world);

        service.handleBedEnter(player);
        service.handleBedEnter(player);

        verify(player).setStatistic(Statistic.TIME_SINCE_REST, 0);
        verify(player).sendMessage("§aProtected");
    }

    @Test
    void registeredMessageExpandsCountsSpeedAndColors() {
        MessageConfig messages = new MessageConfig(
                "&a{sleepers}|{active_sleepers}|{recent_sleepers}|{speed}",
                "phantom",
                "&aReloaded",
                "&cFailed"
        );
        SleepService service = service(config(1000, 500, -1L, messages));

        assertEquals(
                "§a3|1|2|3.50x",
                service.formatSleepRegisteredMessage(new SleepFeedback(1, 2, 3, 2500))
        );
        assertEquals("§aReloaded", service.formatReloadSuccessMessage());
        assertEquals("§cFailed", service.formatReloadFailureMessage());
    }

    @Test
    void emptyRegisteredMessageDisablesFeedback() {
        MessageConfig messages = new MessageConfig("", "phantom", "ok", "bad");
        SleepService service = service(config(1000, 500, -1L, messages));

        assertEquals("", service.formatSleepRegisteredMessage(new SleepFeedback(1, 0, 1, 1000)));
    }

    @Test
    void reloadClearsTrackedPlayersAndUsesNewMessages() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        Player first = player(PLAYER_ONE, world);
        Player second = player(PLAYER_TWO, world);
        service.handleBedEnter(first);

        service.reload(config(2000, 100, -1L, new MessageConfig("new", "phantom", "yes", "no")));

        SleepFeedback feedback = service.handleBedEnter(second).orElseThrow();
        assertEquals(new SleepFeedback(1, 0, 1, 2000), feedback);
        assertEquals("yes", service.formatReloadSuccessMessage());
    }

    @Test
    void activeSleeperAdvancesNightWithFixedPointContribution() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        when(world.getFullTime()).thenReturn(9000L);
        Player player = activePlayer(PLAYER_ONE, world);
        service.handleBedEnter(player);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            bukkit.when(() -> Bukkit.getPlayer(PLAYER_ONE)).thenReturn(player);

            service.tick();
        }

        verify(world).setFullTime(9001L);
    }

    @Test
    void fractionalContributionCarriesAcrossTicks() {
        SleepService service = service(config(500, 0, -1L, messages()));
        World world = nightWorld();
        when(world.getFullTime()).thenReturn(9000L);
        Player player = activePlayer(PLAYER_ONE, world);
        service.handleBedEnter(player);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            bukkit.when(() -> Bukkit.getPlayer(PLAYER_ONE)).thenReturn(player);

            service.tick();
            verify(world, never()).setFullTime(org.mockito.ArgumentMatchers.anyLong());
            service.tick();
        }

        verify(world).setFullTime(9001L);
    }

    @Test
    void missingTrackedPlayerIsRemovedAndCannotContribute() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        Player first = player(PLAYER_ONE, world);
        service.handleBedEnter(first);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            bukkit.when(() -> Bukkit.getPlayer(PLAYER_ONE)).thenReturn(null);
            service.tick();
        }

        verify(world, never()).setFullTime(org.mockito.ArgumentMatchers.anyLong());
        Player second = player(PLAYER_TWO, world);
        assertEquals(1, service.handleBedEnter(second).orElseThrow().totalSleepers());
    }

    @Test
    void leavingNightClearsTrackedState() {
        SleepService service = service(config(1000, 500, -1L, messages()));
        World world = nightWorld();
        Player first = activePlayer(PLAYER_ONE, world);
        service.handleBedEnter(first);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            bukkit.when(() -> Bukkit.getPlayer(PLAYER_ONE)).thenReturn(first);
            service.tick();

            when(world.getTime()).thenReturn(1000L);
            service.tick();
        }

        when(world.getTime()).thenReturn(15000L);
        Player second = player(PLAYER_TWO, world);
        assertEquals(1, service.handleBedEnter(second).orElseThrow().totalSleepers());
    }

    private static SleepService service(SleepConfig config) {
        return new SleepService(Logger.getLogger("SleepServiceTest"), config);
    }

    private static SleepConfig config(
            int activeMilliTicks,
            int recentMilliTicks,
            long phantomThreshold,
            MessageConfig messages
    ) {
        return new SleepConfig(
                "world",
                12000,
                23000,
                activeMilliTicks,
                recentMilliTicks,
                phantomThreshold,
                messages
        );
    }

    private static MessageConfig messages() {
        return new MessageConfig("sleep {sleepers} {speed}", "phantom", "reload ok", "reload bad");
    }

    private static World nightWorld() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getTime()).thenReturn(15000L);
        return world;
    }

    private static Player player(UUID id, World world) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getWorld()).thenReturn(world);
        when(player.isOnline()).thenReturn(true);
        when(player.isDead()).thenReturn(false);
        return player;
    }

    private static Player activePlayer(UUID id, World world) {
        Player player = player(id, world);
        when(player.isSleeping()).thenReturn(true);
        return player;
    }
}
