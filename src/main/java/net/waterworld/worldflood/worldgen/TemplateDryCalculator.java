package net.waterworld.worldflood.worldgen;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.waterworld.worldflood.WorldFlood;

import java.util.BitSet;

/**
 * Precise dry-volume calculation for jigsaw/template structures.
 *
 * <p>Rather than treating the whole piece bounding box as dry, this loads the
 * structure template and flood-fills the template's local air from its boundary.
 * Air that is sealed off by solid template blocks is considered interior and is
 * kept dry. Air connected to the template boundary is exterior and is allowed to
 * flood, so water can touch the structure's hull.</p>
 */
public class TemplateDryCalculator {

    public static boolean isTemplatePiece(StructurePiece piece) {
        return piece instanceof PoolElementStructurePiece poolPiece
                && poolPiece.getElement() instanceof SinglePoolElement;
    }

    public static LongSet computeDryPositions(
            StructurePiece piece,
            StructureTemplateManager manager,
            int waterLevel,
            BoundingBox chunkBox
    ) {
        LongSet dry = new LongOpenHashSet();

        if (!(piece instanceof PoolElementStructurePiece poolPiece)) {
            return dry;
        }
        if (!(poolPiece.getElement() instanceof SinglePoolElement single)) {
            return dry;
        }

        StructureTemplate template;
        try {
            template = single.getTemplate(manager);
        } catch (Exception e) {
            WorldFlood.LOGGER.warn("Failed to load template for dry structure piece {}", piece, e);
            return dry;
        }

        if (template.palettes.isEmpty()) {
            return dry;
        }

        BlockPos size = new BlockPos(template.getSize());
        int sx = size.getX();
        int sy = size.getY();
        int sz = size.getZ();
        if (sx <= 0 || sy <= 0 || sz <= 0) {
            return dry;
        }

        int total = sx * sy * sz;
        if (total > 200_000) {
            WorldFlood.LOGGER.warn("Skipping dry calculation for oversized template {} ({} blocks)", template, total);
            return dry;
        }

        BitSet solid = new BitSet(total);
        StructureTemplate.Palette palette = template.palettes.get(0);
        for (StructureTemplate.StructureBlockInfo info : palette.blocks()) {
            BlockState state = info.state();
            if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) {
                BlockPos p = info.pos();
                if (p.getX() >= 0 && p.getX() < sx && p.getY() >= 0 && p.getY() < sy && p.getZ() >= 0 && p.getZ() < sz) {
                    solid.set(index(p.getX(), p.getY(), p.getZ(), sx, sy));
                }
            }
        }

        BitSet visited = new BitSet(total);
        IntQueue queue = new IntQueue(total);

        // Flood-fill from every boundary cell that is air.
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    if (x != 0 && x != sx - 1 && y != 0 && y != sy - 1 && z != 0 && z != sz - 1) {
                        continue;
                    }
                    int idx = index(x, y, z, sx, sy);
                    if (!solid.get(idx) && !visited.get(idx)) {
                        visited.set(idx);
                        queue.enqueue(idx);
                        flood(solid, visited, queue, sx, sy, sz);
                    }
                }
            }
        }

        Rotation rotation = poolPiece.getRotation();
        BlockPos anchor = poolPiece.getPosition();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setKnownShape(true);

        // Any remaining air cell is interior. Keep the ones that can be flooded (at/below water level)
        // and that fall inside this chunk.
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int idx = index(x, y, z, sx, sy);
                    if (!solid.get(idx) && !visited.get(idx)) {
                        BlockPos localPos = new BlockPos(x, y, z);
                        BlockPos worldPos = StructureTemplate.calculateRelativePosition(settings, localPos).offset(anchor);
                        if (worldPos.getY() <= waterLevel && chunkBox.isInside(worldPos)) {
                            dry.add(worldPos.asLong());
                        }
                    }
                }
            }
        }

        return dry;
    }

    private static void flood(BitSet solid, BitSet visited, IntQueue queue, int sx, int sy, int sz) {
        int[] dx = {1, -1, 0, 0, 0, 0};
        int[] dy = {0, 0, 1, -1, 0, 0};
        int[] dz = {0, 0, 0, 0, 1, -1};

        while (!queue.isEmpty()) {
            int idx = queue.dequeue();
            int x = idx % sx;
            int y = (idx / sx) % sy;
            int z = idx / (sx * sy);

            for (int i = 0; i < 6; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];
                int nz = z + dz[i];
                if (nx < 0 || nx >= sx || ny < 0 || ny >= sy || nz < 0 || nz >= sz) {
                    continue;
                }
                int nidx = (nz * sy + ny) * sx + nx;
                if (!solid.get(nidx) && !visited.get(nidx)) {
                    visited.set(nidx);
                    queue.enqueue(nidx);
                }
            }
        }
    }

    private static int index(int x, int y, int z, int sx, int sy) {
        return (z * sy + y) * sx + x;
    }

    private static class IntQueue {
        private final int[] data;
        private int head = 0;
        private int tail = 0;
        private int size = 0;

        IntQueue(int capacity) {
            this.data = new int[capacity];
        }

        void enqueue(int value) {
            data[tail] = value;
            tail = (tail + 1) % data.length;
            size++;
        }

        int dequeue() {
            int value = data[head];
            head = (head + 1) % data.length;
            size--;
            return value;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
