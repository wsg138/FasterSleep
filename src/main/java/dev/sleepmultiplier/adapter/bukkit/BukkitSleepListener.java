package dev.sleepmultiplier.adapter.bukkit;

import dev.sleepmultiplier.core.SleepFeedback;
import dev.sleepmultiplier.core.SleepService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class BukkitSleepListener implements Listener {
    private final SleepService sleepService;

    public BukkitSleepListener(SleepService sleepService) {
        this.sleepService = sleepService;
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }

        Player player = event.getPlayer();
        sleepService.handleBedEnter(player).ifPresent(feedback -> sendSleepFeedback(player, feedback));
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        sleepService.handleBedLeave(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sleepService.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        sleepService.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        sleepService.handlePlayerQuit(event.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        sleepService.handleWorldChange(event.getPlayer(), event.getFrom());
    }

    private void sendSleepFeedback(Player player, SleepFeedback feedback) {
        String message = sleepService.formatSleepRegisteredMessage(feedback);
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}
