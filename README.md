# SilkSpawners26

A small Paper plugin for Minecraft 26.3: mine a mob spawner with a **Silk Touch netherite
pickaxe** and it drops as an item named after its mob ("Zombie Spawner"). Place it back down
and the spawner keeps the same mob type, spawn delays, spawn count and ranges.

No dependencies. One listener, one config file, no commands.

## How it works

- Breaking a `SPAWNER` in survival or adventure mode with a Silk Touch netherite pickaxe
  (any Silk Touch pickaxe if `require-netherite: false`) drops no XP and instead drops a
  spawner item whose block-entity data is copied from the broken block.
- Spawner items with the same mob and settings stack; different ones do not.
- Placing the item restores the saved state. Paper applies the item's block-entity data on
  placement by itself; the plugin re-applies it only if the placed block does not match.
- Players need the permission `silkspawners.pickup` (default: everyone). Revoke it with a
  permissions plugin to restrict pickup.

## Config (`plugins/SilkSpawners26/config.yml`)

| Key                 | Default | Meaning                                                                      |
|---------------------|---------|------------------------------------------------------------------------------|
| `require-netherite` | `true`  | `false` lets any Silk Touch pickaxe pick up spawners                         |
| `drop-chance`       | `1.0`   | Probability (0.0 to 1.0) that a mined spawner drops as an item               |
| `allowed-mobs`      | `[]`    | Entity ids that may be picked up (`zombie`, `cave_spider`, ...); empty = all |

## Build

Requires JDK 25 and Maven 3.9+.

```bash
mvn clean package
```

The plugin jar is written to `target/SilkSpawners26-<version>.jar`. Drop it into the server's
`plugins/` folder and restart.

## License

MIT, see [LICENSE](LICENSE).
