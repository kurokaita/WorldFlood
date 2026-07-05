package net.waterworld.worldflood.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.waterworld.worldflood.WorldFlood;
import net.waterworld.worldflood.WorldFloodConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class StructureConfigPopulator {

    public static void populate(MinecraftServer server) {
        if (!WorldFloodConfig.AUTO_POPULATE_STRUCTURES.get()) {
            return;
        }

        List<? extends String> existing = WorldFloodConfig.STRUCTURE_OVERRIDES.get();
        // Only auto-populate on first launch (or if the user has deliberately cleared the list).
        // This prevents the mod from constantly re-adding structures the user has removed.
        if (!existing.isEmpty()) {
            return;
        }

        var registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<String> result = new ArrayList<>();
        for (ResourceLocation id : new TreeSet<>(registry.keySet())) {
            result.add(id + " NONE 0");
        }

        WorldFloodConfig.STRUCTURE_OVERRIDES.set(result);
        try {
            WorldFloodConfig.SPEC.save();
            WorldFlood.LOGGER.info("Auto-populated {} structure overrides.", result.size());
        } catch (Exception e) {
            WorldFlood.LOGGER.error("Failed to save auto-populated structure overrides", e);
        }
    }
}
