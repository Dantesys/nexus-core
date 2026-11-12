package org.dantesys.nexus.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class IgneaMiniVolcanoFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();

    public IgneaMiniVolcanoFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos surface = findSurface(level, context.origin());
        int height = 5 + random.nextInt(3);
        int baseRadius = 4;

        if (surface.getY() <= level.getMinY() + 1 || surface.getY() + height + 1 >= level.getMaxY()) {
            return false;
        }
        if (!hasStableFootprint(level, surface, baseRadius)) {
            return false;
        }

        for (int y = 0; y <= height; y++) {
            int radius = Math.max(1, baseRadius - y / 2);
            float radiusSq = radius * radius + 0.45F;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int distanceSq = x * x + z * z;
                    if (distanceSq > radiusSq) {
                        continue;
                    }

                    BlockPos pos = surface.offset(x, y, z);
                    if (y >= height - 1 && distanceSq <= 1) {
                        placeIfFree(level, pos, y == height ? LAVA : MAGMA);
                    } else if (random.nextFloat() < magmaChance(y, distanceSq)) {
                        placeIfFree(level, pos, MAGMA);
                    } else {
                        placeIfFree(level, pos, BLACKSTONE);
                    }
                }
            }
        }

        return true;
    }

    private BlockPos findSurface(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        while (pos.getY() > level.getMinY() + 1 && level.getBlockState(pos).isAir()) {
            pos.move(0, -1, 0);
        }
        return pos.immutable();
    }

    private boolean hasStableFootprint(WorldGenLevel level, BlockPos surface, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos ground = surface.offset(x, -1, z);
                if (level.getBlockState(ground).isAir() || !level.getFluidState(ground).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void placeIfFree(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState current = level.getBlockState(pos);
        if (current.isAir() || current.is(BlockTags.REPLACEABLE_BY_TREES) || current.canBeReplaced()) {
            level.setBlock(pos, state, 19);
        }
    }

    private float magmaChance(int y, int distanceSq) {
        if (distanceSq <= 1) {
            return 0.42F;
        }
        return y <= 1 ? 0.12F : 0.22F;
    }
}
