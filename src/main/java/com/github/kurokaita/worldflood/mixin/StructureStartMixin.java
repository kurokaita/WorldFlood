package com.github.kurokaita.worldflood.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import com.github.kurokaita.worldflood.WorldFlood;
import com.github.kurokaita.worldflood.WorldFloodConfig;
import com.github.kurokaita.worldflood.worldgen.StructureOverrideManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(StructureStart.class)
public abstract class StructureStartMixin {

    @Shadow
    public abstract List<StructurePiece> getPieces();

    @Unique
    private boolean worldflood$raised = false;

    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void worldflood$onPlaceInChunk(WorldGenLevel level, StructureManager structureManager,
                                           ChunkGenerator chunkGenerator, RandomSource random,
                                           BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        if (!WorldFloodConfig.RAISE_STRUCTURES.get()) return;

        StructureStart self = (StructureStart) (Object) this;
        synchronized (self) {
            if (worldflood$raised) return;

            ResourceLocation id = level.getLevel().registryAccess()
                    .registryOrThrow(Registries.STRUCTURE).getKey(self.getStructure());
            if (id == null) {
                WorldFlood.LOGGER.debug("WorldFlood: StructureStart has no registry key, skipping.");
                return;
            }

            StructureOverrideManager.Override override = StructureOverrideManager.getOverride(id);
            if (override.mode() == StructureOverrideManager.Mode.NONE
                    || override.mode() == StructureOverrideManager.Mode.DRY) return;
            // DRY variants are raised like their non-DRY counterparts.

            int waterY = WorldFloodConfig.WATER_LEVEL.get();
            int desiredBottom = waterY + override.offset();

            int currentBottom = Integer.MAX_VALUE;
            for (StructurePiece piece : getPieces()) {
                currentBottom = Math.min(currentBottom, piece.getBoundingBox().minY());
            }
            if (currentBottom == Integer.MAX_VALUE) return;

            int shift = desiredBottom - currentBottom;
            if (shift != 0) {
                WorldFlood.LOGGER.debug("WorldFlood: raising {} by {} blocks ({} -> {})",
                        id, shift, currentBottom, desiredBottom);
                for (StructurePiece piece : getPieces()) {
                    piece.move(0, shift, 0);
                }
            }

            worldflood$raised = true;
        }
    }
}
