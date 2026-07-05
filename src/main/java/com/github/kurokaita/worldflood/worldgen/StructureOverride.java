package com.github.kurokaita.worldflood.worldgen;

import net.minecraft.resources.ResourceLocation;

public record StructureOverride(ResourceLocation structureId, Mode mode, int offset) {
    public enum Mode {
        RAISE_TO_WATER,
        RAISE_ABOVE
    }

    public static StructureOverride parse(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid structure override: " + line);
        }
        ResourceLocation id = ResourceLocation.parse(parts[0]);
        Mode mode = Mode.valueOf(parts[1].toUpperCase());
        int offset = Integer.parseInt(parts[2]);
        return new StructureOverride(id, mode, offset);
    }
}
