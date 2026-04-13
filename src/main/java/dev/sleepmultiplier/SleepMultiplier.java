package dev.sleepmultiplier;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SleepMultiplier extends JavaPlugin implements Listener {
    private static final long NIGHT_START_TICK = 12541L;
    private static final long NIGHT_END_TICK = 23458L;

    private final Map<UUID, Set<UUID>> worldSleepers = new HashMap<>();
    private final Map<UUID, Boolean> worldWasNight = new HashMap<>();
    private final Map<UUID, Double> worldTickRemainder = new HashMap<>();
    private double extraTicksPerSleeper;
    private String sleepMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                tickWorld(world);
            }
        }, 1L, 1L);
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!isNight(world)) {
            return;
        }

        player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        worldSleepers.computeIfAbsent(world.getUID(), key -> new HashSet<>()).add(player.getUniqueId());
        int sleepers = countActiveSleepers(world);
        if (!sleepMessage.isEmpty()) {
            player.sendMessage(formatSleepMessage(sleepers));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeSleeper(event.getPlayer());
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        removeSleeper(event.getPlayer(), event.getFrom());
    }

    private void removeSleeper(Player player) {
        World world = player.getWorld();
        Set<UUID> sleepers = worldSleepers.get(world.getUID());
        if (sleepers != null) {
            sleepers.remove(player.getUniqueId());
        }
    }

    private void removeSleeper(Player player, World world) {
        Set<UUID> sleepers = worldSleepers.get(world.getUID());
        if (sleepers != null) {
            sleepers.remove(player.getUniqueId());
        }
    }

    private void tickWorld(World world) {
        boolean isNight = isNight(world);
        boolean wasNight = worldWasNight.getOrDefault(world.getUID(), false);

        if (wasNight && !isNight) {
            worldSleepers.remove(world.getUID());
            worldTickRemainder.remove(world.getUID());
        }

        worldWasNight.put(world.getUID(), isNight);

        if (!isNight) {
            return;
        }

        int sleepers = countActiveSleepers(world);
        if (sleepers <= 0) {
            worldTickRemainder.remove(world.getUID());
            return;
        }

        double extraTicks = sleepers * extraTicksPerSleeper;
        double remainder = worldTickRemainder.getOrDefault(world.getUID(), 0.0D) + extraTicks;
        long applyTicks = (long) remainder;
        if (applyTicks <= 0) {
            worldTickRemainder.put(world.getUID(), remainder);
            return;
        }
        worldTickRemainder.put(world.getUID(), remainder - applyTicks);
        world.setFullTime(world.getFullTime() + applyTicks);
    }

    private int countActiveSleepers(World world) {
        Set<UUID> sleepers = worldSleepers.get(world.getUID());
        if (sleepers == null || sleepers.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (UUID sleeperId : sleepers) {
            Player player = Bukkit.getPlayer(sleeperId);
            if (player != null && player.isOnline() && player.getWorld().equals(world)) {
                count++;
            }
        }
        return count;
    }

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= NIGHT_START_TICK && time <= NIGHT_END_TICK;
    }

    private String formatSpeedMultiplier(int sleepers) {
        double speed = 1.0D + sleepers * extraTicksPerSleeper;
        return String.format(Locale.US, "%.2fx", speed);
    }

    private String formatSleepMessage(int sleepers) {
        String message = sleepMessage
                .replace("{sleepers}", Integer.toString(sleepers))
                .replace("{speed}", formatSpeedMultiplier(sleepers));
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void loadSettings() {
        extraTicksPerSleeper = getConfig().getDouble("speed.extra-ticks-per-sleeper", 0.5D);
        sleepMessage = getConfig().getString(
                "messages.sleep-registered",
                "&aSleep registered&7. &fSleepers: &b{sleepers} &7| &fNight speed: &b{speed}"
        );
    }
}
