package dev.graygames.silkspawners;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of config.yml.
 *
 * @param requireNetherite only a netherite pickaxe may pick up spawners; otherwise any pickaxe
 * @param allowedMobs      spawner mob types that may be picked up; empty means every type
 * @param dropChance       probability in [0, 1] that a mined spawner drops as an item
 */
public record SilkSpawnersConfig(boolean requireNetherite, @NotNull Set<EntityType> allowedMobs, double dropChance) {

    /** Permission a player needs to pick up a spawner. Declared in plugin.yml with default true. */
    public static final String PICKUP_PERMISSION = "silkspawners.pickup";

    public static @NotNull SilkSpawnersConfig load(@NotNull SilkSpawnersPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();

        boolean requireNetherite = cfg.getBoolean("require-netherite", true);

        double dropChance = cfg.getDouble("drop-chance", 1.0);
        if (dropChance < 0.0 || dropChance > 1.0) {
            plugin.getLogger().warning("drop-chance " + dropChance + " is outside 0.0-1.0; clamping.");
            dropChance = Math.clamp(dropChance, 0.0, 1.0);
        }

        Set<EntityType> allowedMobs = EnumSet.noneOf(EntityType.class);
        List<String> names = cfg.getStringList("allowed-mobs");
        for (String name : names) {
            EntityType type = parseEntityType(name);
            if (type == null) {
                plugin.getLogger().warning("allowed-mobs: unknown entity type '" + name + "' ignored.");
            } else {
                allowedMobs.add(type);
            }
        }

        return new SilkSpawnersConfig(requireNetherite, Set.copyOf(allowedMobs), dropChance);
    }

    /** Accepts "ZOMBIE", "zombie", "minecraft:zombie" or "cave_spider". */
    private static @Nullable EntityType parseEntityType(@NotNull String raw) {
        String name = raw.trim();
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }
        EntityType byName = EntityType.fromName(name.toLowerCase(Locale.ROOT));
        if (byName != null) {
            return byName;
        }
        try {
            return EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** True when this spawner type may be picked up. A spawner with no mob set counts as allowed when the list is empty. */
    public boolean isAllowed(@Nullable EntityType type) {
        if (allowedMobs.isEmpty()) {
            return true;
        }
        return type != null && allowedMobs.contains(type);
    }
}
