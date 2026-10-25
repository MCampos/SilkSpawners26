package dev.graygames.silkspawners;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point. Loads the config and registers the single spawner listener.
 */
public final class SilkSpawnersPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        SilkSpawnersConfig config = SilkSpawnersConfig.load(this);
        getServer().getPluginManager().registerEvents(new SpawnerListener(config), this);
        getLogger().info("Spawner pickup enabled (require-netherite=" + config.requireNetherite()
                + ", drop-chance=" + config.dropChance()
                + ", allowed-mobs=" + (config.allowedMobs().isEmpty() ? "all" : config.allowedMobs()) + ")");
    }
}
