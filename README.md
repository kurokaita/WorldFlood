# World Flood (NeoForge 1.21.1)

A small NeoForge mod that floods the Overworld **after** chunk generation. Because
structures, cities, forests, and biomes generate normally first, you get a true
"world submerged" look instead of broken structure spawns.

## How it works

The mod uses a Mixin to hook `ChunkGenerator.applyBiomeDecoration()` at the tail.
After vanilla features and structures have been placed in a chunk, it scans every
column and replaces air blocks below the configured `water_level` with water.

Structures see the normal Y=63 sea level, so they spawn correctly. The flood
pass then drowns everything underneath.

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download the latest `worldflood-*.jar` from the [releases page](../../releases).
3. Copy the jar into your `mods/` folder.
4. Launch the game/world.

## Configuration

The config is created at `config/worldflood-common.toml` on first run.

```toml
[general]
    # Enable the post-generation flood pass.
    enabled = true

    # The Y level up to which air will be flooded with water.
    # Default: 100
    water_level = 100

    # If true, only air columns exposed to the sky are flooded (keeps caves dry).
    only_surface = false

    # Enable raising structures before they are placed so they sit at/above the water surface.
    raise_structures = false

    # Automatically add every loaded structure to structure_overrides with mode NONE.
    # This only happens once, when the list is empty, so you can edit or clear it afterwards.
    auto_populate_structures = true

    # List of structure overrides. Format:
    #   "modid:structure_id MODE offset"
    # Use "modid:*" to match every structure from that mod.
    # Modes:
    #   NONE                       -> leave the structure as-is
    #   RAISE_TO_WATER             -> bottom of structure is placed at water_level
    #   RAISE_ABOVE                -> bottom of structure is placed at water_level + offset
    #   DRY                        -> keep the structure's interior air blocks dry (no raising)
    #   RAISE_TO_WATER_DRY         -> RAISE_TO_WATER plus keep the interior dry
    #   RAISE_ABOVE_DRY            -> RAISE_ABOVE plus keep the interior dry
    # Offset is in blocks relative to the water level. Negative values sink the structure.
    #
    # Note: DRY modes are experimental. They currently carve out a rectangular
    # dry box around and under the structure because the mod cannot precisely
    # detect the structure's actual placed boundaries after generation.
    structure_overrides = []
```

### Raising structures above the flood

If you have a mod that adds ships, villages, or outposts that should sit on the
water instead of under it, add their structure IDs to `structure_overrides`.

Example:

```toml
structure_overrides = [
    "some_mod:pillager_warship RAISE_TO_WATER 0",
    "some_mod:villager_ship RAISE_ABOVE 2",
    "another_mod:* RAISE_TO_WATER 0"
]
```

To find a structure's ID, you can usually see it in the mod's jar under
`data/<modid>/worldgen/structure/`, or by using `/locate structure` autocomplete
in-game.

### Auto-detected structures

On first server start, World Flood scans every loaded structure and writes two
helpful files to your `config/` folder:

- `config/worldflood-detected.toml` — a reference list of every detected
  structure, grouped by mod, plus suggestions for structures whose names look
  like they belong on the water (ships, villages, outposts, etc.). This file is
  only created if it does not already exist, so it is safe to edit or delete.
- `config/worldflood-common.toml` — if `auto_populate_structures` is `true` and
  `structure_overrides` is empty, the mod pre-fills `structure_overrides` with
  every loaded structure set to `NONE 0`. This gives you a starting list you can
  tweak without having to look up IDs by hand.

The auto-population only happens once. After you edit `structure_overrides`, the
mod will not overwrite your changes.

## Building from source

Requirements:
- JDK 21 or newer

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Contributing

Contributions are welcome. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for
guidelines.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for
details.

Some files are derived from the NeoForge Mod Development Kit (MDK) and are used
under the terms of [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).

## Important notes

- **Performance:** At large water levels the mod touches many blocks per chunk.
  First-time world generation may be slower until chunks settle.
- **Existing chunks:** The flood pass runs during chunk generation, so already
  generated chunks are not retroactively flooded.
- **Caves:** By default every air pocket below `water_level` is filled,
  including caves and ravines. Set `only_surface = true` to flood only
  surface-exposed columns.
- **Water updates:** Water is placed with minimal updates during generation to
  avoid cascading fluid ticks, but some flowing water at the surface edges is
  normal.
- **Structure raising:** Only standard data-driven structures can be lifted.
  Code-generated terrain (like Lost Cities' worldgen) is not affected by the
  override list.
- **DRY modes:** The DRY override modes are experimental. They carve out a
  rectangular dry box around and under a structure because the mod cannot
  precisely detect the structure's actual placed boundaries after generation.
