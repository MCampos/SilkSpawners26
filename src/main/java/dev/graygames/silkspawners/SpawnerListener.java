package dev.graygames.silkspawners;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a mined spawner into a spawner item carrying the block's state, and makes sure
 * a placed spawner item restores that state.
 */
public final class SpawnerListener implements Listener {

    private final SilkSpawnersConfig config;

    public SpawnerListener(@NotNull SilkSpawnersConfig config) {
        this.config = config;
    }

    // ------------------------------------------------------------------ pickup

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }
        Player player = event.getPlayer();
        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            return;
        }
        if (!player.hasPermission(SilkSpawnersConfig.PICKUP_PERMISSION)) {
            return;
        }
        if (!isSilkTouchTool(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            return;
        }
        if (!config.isAllowed(spawner.getSpawnedType())) {
            return;
        }

        // Silk Touch pickup: no vanilla XP, no vanilla drops (a spawner has none anyway).
        event.setExpToDrop(0);
        event.setDropItems(false);

        if (config.dropChance() < 1.0 && ThreadLocalRandom.current().nextDouble() >= config.dropChance()) {
            return;
        }

        ItemStack item = createSpawnerItem(spawner);
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().dropItemNaturally(center, item);
    }

    private boolean isSilkTouchTool(@NotNull ItemStack tool) {
        if (tool.isEmpty()) {
            return false;
        }
        Material type = tool.getType();
        boolean rightPickaxe = config.requireNetherite()
                ? type == Material.NETHERITE_PICKAXE
                : Tag.ITEMS_PICKAXES.isTagged(type);
        return rightPickaxe && tool.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    /**
     * Builds a spawner item whose BlockStateMeta holds a copy of the broken block's state:
     * spawned type (including custom spawn data), delays, counts and ranges.
     */
    private static @NotNull ItemStack createSpawnerItem(@NotNull CreatureSpawner spawner) {
        ItemStack item = ItemStack.of(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta blockMeta) {
            blockMeta.setBlockState(spawner);
            blockMeta.displayName(Component.text(displayName(spawner.getSpawnedType()))
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(blockMeta);
        }
        return item;
    }

    /** "ZOMBIE" -> "Zombie Spawner", "CAVE_SPIDER" -> "Cave Spider Spawner", null -> "Empty Spawner". */
    static @NotNull String displayName(@Nullable EntityType type) {
        if (type == null) {
            return "Empty Spawner";
        }
        StringBuilder out = new StringBuilder();
        for (String word : type.name().toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length()).append(' ');
        }
        return out.append("Spawner").toString();
    }

    // ------------------------------------------------------------------ placement

    /**
     * Paper applies an item's block-entity data when the block is placed, so normally the placed
     * spawner already carries the saved state when this handler runs. This is a safety net: if the
     * placed block does not match the item's state (another plugin or a future server change
     * interfered), copy the saved state onto the block.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.SPAWNER) {
            return;
        }
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER || !(item.getItemMeta() instanceof BlockStateMeta meta)) {
            return;
        }
        if (!meta.hasBlockState() || !(meta.getBlockState() instanceof CreatureSpawner saved)) {
            return;
        }
        if (!(placed.getState() instanceof CreatureSpawner target)) {
            return;
        }
        if (saved.getSpawnedType() == target.getSpawnedType()) {
            return; // the server already applied the item's state
        }
        copySpawner(saved, target);
        target.update(true, false);
    }

    private static void copySpawner(@NotNull CreatureSpawner from, @NotNull CreatureSpawner to) {
        if (from.getSpawnedEntity() != null) {
            to.setSpawnedEntity(from.getSpawnedEntity());
        } else {
            to.setSpawnedType(from.getSpawnedType());
        }
        // min must stay <= max at every step, so lower min first, then set max, then min.
        to.setMinSpawnDelay(Math.min(from.getMinSpawnDelay(), to.getMaxSpawnDelay()));
        to.setMaxSpawnDelay(from.getMaxSpawnDelay());
        to.setMinSpawnDelay(from.getMinSpawnDelay());
        to.setDelay(from.getDelay());
        to.setSpawnCount(from.getSpawnCount());
        to.setMaxNearbyEntities(from.getMaxNearbyEntities());
        to.setRequiredPlayerRange(from.getRequiredPlayerRange());
        to.setSpawnRange(from.getSpawnRange());
    }
}
