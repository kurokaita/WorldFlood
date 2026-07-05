package com.github.kurokaita.worldflood.worldgen;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import com.github.kurokaita.worldflood.WorldFlood;
import com.github.kurokaita.worldflood.WorldFloodConfig;
import com.github.kurokaita.worldflood.worldgen.StructureOverrideManager.Mode;

import java.util.List;

public class FloodProcessor {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    public static void process(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (!WorldFloodConfig.ENABLED.getAsBoolean()) {
            return;
        }

        // Only flood the Overworld for now.
        if (level.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }

        int waterLevel = WorldFloodConfig.WATER_LEVEL.getAsInt();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        if (waterLevel <= minY || waterLevel > maxY) {
            WorldFlood.LOGGER.warn("Invalid water level {} for dimension range {}-{}.", waterLevel, minY, maxY);
            return;
        }

        LongSet dryPositions = collectDryPositions(level, structureManager, chunk, waterLevel);

        boolean onlySurface = WorldFloodConfig.ONLY_SURFACE.getAsBoolean();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = waterLevel; y >= minY; y--) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);

                    if (dryPositions.contains(pos.asLong())) {
                        continue;
                    }

                    BlockState state = chunk.getBlockState(pos);

                    if (!state.isAir()) {
                        // Hit a solid/liquid block. If we are only flooding surface columns,
                        // encountering any block means the rest of this column below is protected.
                        if (onlySurface) {
                            break;
                        }
                        continue;
                    }

                    if (onlySurface && !level.canSeeSky(pos)) {
                        // Under a ceiling — don't flood this or anything below it in this column.
                        break;
                    }

                    chunk.setBlockState(pos, WATER, false);
                }
            }
        }
    }

    private static LongSet collectDryPositions(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk, int waterLevel) {
        LongSet dryPositions = new LongOpenHashSet();
        var registry = structureManager.registryAccess().registryOrThrow(Registries.STRUCTURE);

        List<StructureStart> dryStarts = structureManager.startsForStructure(chunk.getPos(), structure -> {
            ResourceLocation id = registry.getKey(structure);
            if (id == null) return false;
            Mode mode = StructureOverrideManager.getOverride(id).mode();
            return mode == Mode.DRY || mode == Mode.RAISE_TO_WATER_DRY || mode == Mode.RAISE_ABOVE_DRY;
        });

        if (dryStarts.isEmpty()) {
            return dryPositions;
        }

        ServerLevel serverLevel = level.getLevel();
        StructureTemplateManager templateManager = serverLevel.getStructureManager();

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        BoundingBox chunkBox = new BoundingBox(
                chunkMinX, level.getMinBuildHeight(), chunkMinZ,
                chunkMinX + 15, level.getMaxBuildHeight(), chunkMinZ + 15
        );

        for (StructureStart start : dryStarts) {
            for (StructurePiece piece : start.getPieces()) {
                BoundingBox pieceBox = piece.getBoundingBox();
                if (!pieceBox.intersects(chunkBox)) {
                    continue;
                }

                if (TemplateDryCalculator.isTemplatePiece(piece)) {
                    LongSet pieceDry = TemplateDryCalculator.computeDryPositions(piece, templateManager, waterLevel, chunkBox);
                    dryPositions.addAll(pieceDry);
                } else {
                    // Non-template pieces: fall back to the original bounding-box dry.
                    addBoxDryPositions(dryPositions, pieceBox, chunkBox, waterLevel);
                }
            }
        }

        return dryPositions;
    }

    private static void addBoxDryPositions(LongSet dryPositions, BoundingBox pieceBox, BoundingBox chunkBox, int waterLevel) {
        int minX = Math.max(pieceBox.minX(), chunkBox.minX());
        int maxX = Math.min(pieceBox.maxX(), chunkBox.maxX());
        int minY = Math.max(pieceBox.minY(), chunkBox.minY());
        int maxY = Math.min(pieceBox.maxY(), chunkBox.maxY());
        int minZ = Math.max(pieceBox.minZ(), chunkBox.minZ());
        int maxZ = Math.min(pieceBox.maxZ(), chunkBox.maxZ());

        for (int y = minY; y <= maxY && y <= waterLevel; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    dryPositions.add(BlockPos.asLong(x, y, z));
                }
            }
        }
    }
}
