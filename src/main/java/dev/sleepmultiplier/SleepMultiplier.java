package dev.sleepmultiplier;

import dev.sleepmultiplier.adapter.bukkit.BukkitSleepListener;
import dev.sleepmultiplier.adapter.bukkit.SleepCommand;
import dev.sleepmultiplier.config.ConfigLoader;
import dev.sleepmultiplier.config.SleepConfig;
import dev.sleepmultiplier.core.SleepService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SleepMultiplier extends JavaPlugin {
    private ConfigLoader configLoader;
    private SleepService sleepService;
    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configLoader = new ConfigLoader(this);
        SleepConfig initialConfig = configLoader.load();

        sleepService = new SleepService(getLogger(), initialConfig);

        Bukkit.getPluginManager().registerEvents(new BukkitSleepListener(sleepService), this);
        registerCommand();
        startTickTask();
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        HandlerList.unregisterAll(this);

        if (sleepService != null) {
            sleepService.clear();
        }
    }

    public boolean reloadPlugin() {
        try {
            reloadConfig();
            SleepConfig updatedConfig = configLoader.load();
            sleepService.reload(updatedConfig);
            return true;
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to reload configuration: " + exception.getMessage());
            getLogger().severe("Keeping the previous in-memory configuration and clearing active sleep state.");
            sleepService.clear();
            return false;
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("sleepmultiplier");
        if (command == null) {
            throw new IllegalStateException("Command 'sleepmultiplier' is missing from plugin.yml");
        }

        SleepCommand executor = new SleepCommand(this, sleepService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(this, sleepService::tick, 1L, 1L);
    }
}
