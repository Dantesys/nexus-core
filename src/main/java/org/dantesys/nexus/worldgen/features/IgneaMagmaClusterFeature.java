package org.dantesys.nexus.worldgen.features;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class IgneaMagmaClusterFeature implements Feature {
    public static final IgneaMagmaClusterFeature INSTANCE = new IgneaMagmaClusterFeature();
    public static final MapCodec<IgneaMagmaClusterFeature> CODEC = MapCodec.unit(INSTANCE);

    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();

    private IgneaMagmaClusterFeature() {
    }

    @Override
    public MapCodec<IgneaMagmaClusterFeature> codec() {
        return CODEC;
    }



    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos start) {
        BlockPos origin = findSurface(level, start);
        int radius = 2 + random.nextInt(2);

        if (origin.getY() <= level.getMinY() + 1 || origin.getY() + 2 >= level.getMaxY()) {
            return false;
        }

        boolean placed = false;
        float radiusSq = radius * radius + 0.5F;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distanceSq = x * x + z * z;
                if (distanceSq > radiusSq || random.nextFloat() < edgeSkipChance(radius, distanceSq)) {
                    continue;
                }

                BlockPos pos = origin.offset(x, random.nextInt(2), z);
                if (canReplace(level, pos) && !level.getBlockState(pos.below()).isAir()) {
                    level.setBlock(pos, MAGMA, 19);
                    placed = true;
                }
            }
        }

        return placed;
    }

    private BlockPos findSurface(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        while (pos.getY() > level.getMinY() + 1 && level.getBlockState(pos).isAir()) {
            pos.move(0, -1, 0);
        }
        return pos.immutable();
    }

    private boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.canBeReplaced();
    }

    private float edgeSkipChance(int radius, int distanceSq) {
        return distanceSq >= radius * radius ? 0.45F : 0.08F;
    }
}
