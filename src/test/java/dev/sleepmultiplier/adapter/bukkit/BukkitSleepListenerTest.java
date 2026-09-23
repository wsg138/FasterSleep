package dev.sleepmultiplier.adapter.bukkit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sleepmultiplier.core.SleepFeedback;
import dev.sleepmultiplier.core.SleepService;
import java.util.Optional;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BukkitSleepListenerTest {
    private SleepService service;
    private BukkitSleepListener listener;
    private Player player;

    @BeforeEach
    void setUp() {
        service = mock(SleepService.class);
        listener = new BukkitSleepListener(service);
        player = mock(Player.class);
    }

    @Test
    void rejectedBedEntryIsIgnored() {
        PlayerBedEnterEvent event = mock(PlayerBedEnterEvent.class);
        when(event.getBedEnterResult()).thenReturn(PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_NOW);

        listener.onBedEnter(event);

        verify(event, never()).getPlayer();
        verify(service, never()).handleBedEnter(player);
    }

    @Test
    void acceptedBedEntryWithoutFeedbackSendsNothing() {
        PlayerBedEnterEvent event = acceptedBedEvent();
        when(service.handleBedEnter(player)).thenReturn(Optional.empty());

        listener.onBedEnter(event);

        verify(service).handleBedEnter(player);
        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void acceptedBedEntrySendsFormattedFeedback() {
        PlayerBedEnterEvent event = acceptedBedEvent();
        SleepFeedback feedback = new SleepFeedback(2, 1, 3, 2500);
        when(service.handleBedEnter(player)).thenReturn(Optional.of(feedback));
        when(service.formatSleepRegisteredMessage(feedback)).thenReturn("sleep registered");

        listener.onBedEnter(event);

        verify(player).sendMessage("sleep registered");
    }

    @Test
    void emptyFormattedFeedbackSuppressesChat() {
        PlayerBedEnterEvent event = acceptedBedEvent();
        SleepFeedback feedback = new SleepFeedback(1, 0, 1, 1000);
        when(service.handleBedEnter(player)).thenReturn(Optional.of(feedback));
        when(service.formatSleepRegisteredMessage(feedback)).thenReturn("");

        listener.onBedEnter(event);

        verify(player, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void leaveQuitKickAndDeathForwardCleanup() {
        PlayerBedLeaveEvent leave = mock(PlayerBedLeaveEvent.class);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        PlayerKickEvent kick = mock(PlayerKickEvent.class);
        PlayerDeathEvent death = mock(PlayerDeathEvent.class);
        when(leave.getPlayer()).thenReturn(player);
        when(quit.getPlayer()).thenReturn(player);
        when(kick.getPlayer()).thenReturn(player);
        when(death.getEntity()).thenReturn(player);

        listener.onBedLeave(leave);
        listener.onQuit(quit);
        listener.onKick(kick);
        listener.onDeath(death);

        verify(service).handleBedLeave(player);
        verify(service, org.mockito.Mockito.times(3)).handlePlayerQuit(player);
    }

    @Test
    void worldChangeForwardsPlayerAndSourceWorld() {
        World from = mock(World.class);
        PlayerChangedWorldEvent event = mock(PlayerChangedWorldEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(from);

        listener.onWorldChange(event);

        verify(service).handleWorldChange(player, from);
    }

    private PlayerBedEnterEvent acceptedBedEvent() {
        PlayerBedEnterEvent event = mock(PlayerBedEnterEvent.class);
        when(event.getBedEnterResult()).thenReturn(PlayerBedEnterEvent.BedEnterResult.OK);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }
}
