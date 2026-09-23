package dev.sleepmultiplier.adapter.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sleepmultiplier.SleepMultiplier;
import dev.sleepmultiplier.core.SleepService;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SleepCommandTest {
    private SleepMultiplier plugin;
    private SleepService service;
    private SleepCommand command;
    private CommandSender sender;
    private Command bukkitCommand;

    @BeforeEach
    void setUp() {
        plugin = mock(SleepMultiplier.class);
        service = mock(SleepService.class);
        command = new SleepCommand(plugin, service);
        sender = mock(CommandSender.class);
        bukkitCommand = mock(Command.class);
    }

    @Test
    void invalidArgumentsReturnUsageWithoutReloading() {
        assertTrue(command.onCommand(sender, bukkitCommand, "sleepmultiplier", new String[0]));

        verify(sender).sendMessage("/sleepmultiplier reload");
        verify(sender, never()).hasPermission("sleepmultiplier.reload");
        verify(plugin, never()).reloadPlugin();
    }

    @Test
    void extraArgumentsReturnUsageWithoutReloading() {
        assertTrue(command.onCommand(sender, bukkitCommand, "sleep", new String[]{"reload", "extra"}));

        verify(sender).sendMessage("/sleep reload");
        verify(plugin, never()).reloadPlugin();
    }

    @Test
    void permissionDenialFailsClosed() {
        when(sender.hasPermission("sleepmultiplier.reload")).thenReturn(false);

        assertTrue(command.onCommand(sender, bukkitCommand, "sleepmultiplier", new String[]{"RELOAD"}));

        verify(sender).sendMessage("You do not have permission to use this command.");
        verify(plugin, never()).reloadPlugin();
    }

    @Test
    void successfulReloadUsesConfiguredSuccessMessage() {
        when(sender.hasPermission("sleepmultiplier.reload")).thenReturn(true);
        when(plugin.reloadPlugin()).thenReturn(true);
        when(service.formatReloadSuccessMessage()).thenReturn("reload ok");

        assertTrue(command.onCommand(sender, bukkitCommand, "sleepmultiplier", new String[]{"reload"}));

        verify(plugin).reloadPlugin();
        verify(service).formatReloadSuccessMessage();
        verify(service, never()).formatReloadFailureMessage();
        verify(sender).sendMessage("reload ok");
    }

    @Test
    void failedReloadUsesConfiguredFailureMessage() {
        when(sender.hasPermission("sleepmultiplier.reload")).thenReturn(true);
        when(plugin.reloadPlugin()).thenReturn(false);
        when(service.formatReloadFailureMessage()).thenReturn("reload bad");

        assertTrue(command.onCommand(sender, bukkitCommand, "sleepmultiplier", new String[]{"reload"}));

        verify(plugin).reloadPlugin();
        verify(service).formatReloadFailureMessage();
        verify(service, never()).formatReloadSuccessMessage();
        verify(sender).sendMessage("reload bad");
    }

    @Test
    void tabCompletionOnlyOffersReloadForTheFirstArgument() {
        assertEquals(List.of("reload"), command.onTabComplete(sender, bukkitCommand, "sleepmultiplier", new String[]{""}));
        assertEquals(List.of("reload"), command.onTabComplete(sender, bukkitCommand, "sleepmultiplier", new String[]{"r"}));
        assertEquals(List.of(), command.onTabComplete(sender, bukkitCommand, "sleepmultiplier", new String[0]));
        assertEquals(List.of(), command.onTabComplete(sender, bukkitCommand, "sleepmultiplier", new String[]{"reload", ""}));
    }
}
