package dev.sleepmultiplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginDescriptorContractTest {
    @Test
    void pluginIdentityAndApiVersionRemainStable() throws Exception {
        YamlConfiguration descriptor = descriptor();

        assertEquals("SleepMultiplier", descriptor.getString("name"));
        assertEquals("dev.sleepmultiplier.SleepMultiplier", descriptor.getString("main"));
        assertEquals("1.2.0", descriptor.getString("version"));
        assertEquals("1.21", descriptor.getString("api-version"));
    }

    @Test
    void reloadCommandSurfaceRemainsExplicit() throws Exception {
        YamlConfiguration descriptor = descriptor();
        ConfigurationSection commands = descriptor.getConfigurationSection("commands");
        assertNotNull(commands);
        assertEquals(Set.of("sleepmultiplier"), commands.getKeys(false));
        assertEquals("sleepmultiplier.reload", descriptor.getString("commands.sleepmultiplier.permission"));
        assertEquals("/<command> reload", descriptor.getString("commands.sleepmultiplier.usage"));
        assertEquals(java.util.List.of("fastersleep"), descriptor.getStringList("commands.sleepmultiplier.aliases"));
    }

    @Test
    void reloadPermissionRetainsOperatorDefault() throws Exception {
        YamlConfiguration descriptor = descriptor();
        assertEquals("op", descriptor.getString("permissions.sleepmultiplier.reload.default"));
    }

    private static YamlConfiguration descriptor() throws Exception {
        try (InputStream input = PluginDescriptorContractTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input, "plugin.yml must be present on the test classpath");
            try (InputStreamReader reader = new InputStreamReader(input, UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        }
    }
}
