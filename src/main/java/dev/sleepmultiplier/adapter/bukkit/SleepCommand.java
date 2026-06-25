package dev.sleepmultiplier.adapter.bukkit;

import dev.sleepmultiplier.SleepMultiplier;
import dev.sleepmultiplier.core.SleepService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class SleepCommand implements CommandExecutor, TabCompleter {
    private static final int RELOAD_ARGUMENT_COUNT = 1;
    private static final List<String> RELOAD_COMPLETIONS = List.of("reload");

    private final SleepMultiplier plugin;
    private final SleepService sleepService;

    public SleepCommand(SleepMultiplier plugin, SleepService sleepService) {
        this.plugin = plugin;
        this.sleepService = sleepService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            sender.sendMessage("/" + label + " reload");
            return true;
        }

        if (!sender.hasPermission("sleepmultiplier.reload")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        boolean success = plugin.reloadPlugin();
        sender.sendMessage(success
                ? sleepService.formatReloadSuccessMessage()
                : sleepService.formatReloadFailureMessage());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == RELOAD_ARGUMENT_COUNT) {
            return RELOAD_COMPLETIONS;
        }
        return List.of();
    }
}
