package com.github.kurokaita.worldflood;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class WorldFloodConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue WATER_LEVEL;
    public static final ModConfigSpec.BooleanValue ONLY_SURFACE;
    public static final ModConfigSpec.BooleanValue RAISE_STRUCTURES;
    public static final ModConfigSpec.BooleanValue AUTO_POPULATE_STRUCTURES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_OVERRIDES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ENABLED = builder.define("enabled", true);
        WATER_LEVEL = builder.defineInRange("water_level", 100, -64, 319);
        ONLY_SURFACE = builder.comment("Only flood columns open to sky").define("only_surface", false);

        RAISE_STRUCTURES = builder
                .comment("Raise structures before they are placed so they sit at/above the water surface.")
                .define("raise_structures", false);

        AUTO_POPULATE_STRUCTURES = builder
                .comment(
                        "Automatically add every loaded structure to structure_overrides with mode NONE.",
                        "This only happens once, when the list is empty."
                )
                .define("auto_populate_structures", true);

        STRUCTURE_OVERRIDES = builder
                .comment(
                        "Per-structure overrides. Format: \"modid:structure_id MODE offset\"",
                        "Modes: NONE, RAISE_TO_WATER, RAISE_ABOVE, DRY, RAISE_TO_WATER_DRY, RAISE_ABOVE_DRY",
                        "Offset is in blocks relative to the water level. Negative values sink the structure below the water.",
                        "DRY variants keep the structure's interior air blocks from being flooded after it is raised.",
                        "Use \"modid:*\" to match every structure from that mod.",
                        "This list is auto-populated once on first launch if auto_populate_structures is true.",
                        "Malformed entries are ignored at runtime rather than removed from the list."
                )
                .defineListAllowEmpty(
                        "structure_overrides",
                        List.of(),
                        obj -> obj instanceof String s && !s.isBlank()
                );

        SPEC = builder.build();
    }
}
