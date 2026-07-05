package net.waterworld.worldflood.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StructureDetector {

    private static final List<String> SUGGESTION_KEYWORDS = List.of(
            "ship", "warship", "boat", "vessel", "galleon", "frigate",
            "village", "outpost", "camp", "pillager", "illager"
    );

    public static void writeDetectedFile(MinecraftServer server) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve("worldflood-detected.toml");
        if (Files.exists(file)) {
            return; // Don't overwrite user edits.
        }

        var registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<String, List<ResourceLocation>> byNamespace = new TreeMap<>();
        List<ResourceLocation> suggested = new ArrayList<>();

        for (ResourceLocation id : registry.keySet().stream().sorted().toList()) {
            byNamespace.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(id);
            if (shouldSuggest(id)) {
                suggested.add(id);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# World Flood - detected structures\n");
        sb.append("# Copy entries you want to override into worldflood-common.toml under structure_overrides\n");
        sb.append("# Format: \"modid:structure_id MODE offset\"  (Modes: RAISE_TO_WATER, RAISE_ABOVE)\n\n");

        sb.append("# Wildcard example: raise every structure from a specific mod to the water surface.\n");
        sb.append("# \"some_mod:* RAISE_TO_WATER 0\"\n\n");

        sb.append("[suggestions]\n");
        if (suggested.isEmpty()) {
            sb.append("    # No structures matched the suggestion keywords.\n");
        } else {
            sb.append("    # Structures whose names look like they might belong on the water.\n");
            for (ResourceLocation id : suggested) {
                sb.append("    # \"").append(id).append(" RAISE_TO_WATER 0\"\n");
            }
        }

        sb.append("\n[all_structures]\n");
        for (Map.Entry<String, List<ResourceLocation>> entry : byNamespace.entrySet()) {
            sb.append("\n    # ").append(entry.getKey()).append("\n");
            for (ResourceLocation id : entry.getValue()) {
                sb.append("    # \"").append(id).append("\"\n");
            }
        }

        try {
            Files.createDirectories(configDir);
            Files.writeString(file, sb.toString());
        } catch (IOException e) {
            net.waterworld.worldflood.WorldFlood.LOGGER.error("Failed to write detected structures file", e);
        }
    }

    private static boolean shouldSuggest(ResourceLocation id) {
        String lower = id.toString().toLowerCase(Locale.ROOT);
        return SUGGESTION_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
