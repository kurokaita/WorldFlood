package net.waterworld.worldflood.worldgen;

import net.minecraft.resources.ResourceLocation;
import net.waterworld.worldflood.WorldFloodConfig;

import java.util.ArrayList;
import java.util.List;

public class StructureOverrideManager {

    public enum Mode {
        NONE,
        RAISE_TO_WATER,
        RAISE_ABOVE,
        DRY,
        RAISE_TO_WATER_DRY,
        RAISE_ABOVE_DRY
    }

    public record Override(Mode mode, int offset) {
    }

    private static List<Entry> cache = null;

    private record Entry(ResourceLocation id, boolean wildcard, Mode mode, int offset) {
    }

    public static Override getOverride(ResourceLocation structureId) {
        if (cache == null) {
            buildCache();
        }
        for (Entry entry : cache) {
            if (entry.wildcard) {
                if (entry.id.getNamespace().equals(structureId.getNamespace())) {
                    return new Override(entry.mode, entry.offset);
                }
            } else if (entry.id.equals(structureId)) {
                return new Override(entry.mode, entry.offset);
            }
        }
        return new Override(Mode.NONE, 0);
    }

    public static void invalidateCache() {
        cache = null;
    }

    private static void buildCache() {
        cache = new ArrayList<>();
        for (String raw : WorldFloodConfig.STRUCTURE_OVERRIDES.get()) {
            String[] parts = raw.trim().split("\\s+");
            if (parts.length < 2 || parts.length > 3) {
                net.waterworld.worldflood.WorldFlood.LOGGER.warn("Skipping malformed structure override: {}", raw);
                continue;
            }

            String idStr = parts[0];
            Mode mode;
            try {
                mode = Mode.valueOf(parts[1]);
            } catch (IllegalArgumentException e) {
                net.waterworld.worldflood.WorldFlood.LOGGER.warn("Unknown structure override mode in: {}", raw);
                continue;
            }

            int offset = 0;
            if (parts.length == 3) {
                try {
                    offset = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    net.waterworld.worldflood.WorldFlood.LOGGER.warn("Invalid offset in structure override: {}", raw);
                    continue;
                }
            }

            if (!idStr.matches("^[a-z0-9_.-]+:[a-z0-9_*.\\\\-]+(/[a-z0-9_*.\\\\-]+)?$")) {
                net.waterworld.worldflood.WorldFlood.LOGGER.warn("Invalid structure id in override: {}", raw);
                continue;
            }

            if (mode == Mode.DRY) {
                // DRY does not use an offset; ignore whatever was supplied.
                offset = 0;
            }

            boolean wildcard = idStr.contains("*");
            if (wildcard) {
                // Normalize wildcard to just the namespace.
                idStr = idStr.substring(0, idStr.indexOf(':') + 1) + "*";
            }
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            if (id == null) {
                net.waterworld.worldflood.WorldFlood.LOGGER.warn("Could not parse structure override id: {}", raw);
                continue;
            }
            cache.add(new Entry(id, wildcard, mode, offset));
        }
    }
}
