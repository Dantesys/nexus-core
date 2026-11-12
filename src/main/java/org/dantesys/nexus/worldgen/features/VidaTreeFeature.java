package org.dantesys.nexus.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class VidaTreeFeature extends Feature<TreeConfiguration> {
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final TreeVariant ROUNDED = new TreeVariant(8, 12, 5, 0.55F, 2, 0);
    private static final TreeVariant TALL_LAYERED = new TreeVariant(10, 13, 4, 0.45F, 2, 1);
    private static final TreeVariant WIDE_NATURAL = new TreeVariant(8, 11, 6, 0.68F, 3, 2);
    private static final TreeVariant ANCESTRAL = new TreeVariant(15, 19, 7, 0.92F, 4, 3);

    public VidaTreeFeature(Codec<TreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TreeConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        TreeConfiguration config = context.config();
        TreeVariant variant = selectVariant(random);
        int height = variant.minHeight() + random.nextInt(variant.maxHeight() - variant.minHeight() + 1);

        if (origin.getY() < level.getMinY() + 1 || origin.getY() + height + 4 >= level.getMaxY()) {
            return false;
        }
        if (!hasRoom(level, origin, height, variant)) {
            return false;
        }

        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        Set<BlockPos> roots = new HashSet<>();
        Set<BlockPos> decorations = new HashSet<>();

        BlockState log = config.trunkProvider.getState(level, random, origin);
        BlockState leaf = withLeafDistance(config.foliageProvider.getState(level, random, origin), 7);

        placeRoots(level, random, origin, log, roots, variant);
        for (int y = 0; y < height; y++) {
            placeLog(level, origin.above(y), log, logs);
        }

        placeCanopy(level, random, origin, height, variant, log, leaf, logs, leaves);
        updateLeafDistances(level, leaves, logs, roots);

        BiConsumer<BlockPos, BlockState> decorationSetter = (pos, state) -> {
            decorations.add(pos.immutable());
            level.setBlock(pos, state, 19);
        };
        if (!config.decorators.isEmpty()) {
            TreeDecorator.Context decoratorContext = new TreeDecorator.Context(level, decorationSetter, random, logs, leaves, roots);
            config.decorators.forEach(decorator -> decorator.place(decoratorContext));
        }

        return true;
    }

    private TreeVariant selectVariant(RandomSource random) {
        if (random.nextFloat() < 0.045F) {
            return ANCESTRAL;
        }

        int variant = random.nextInt(3);
        if (variant == 0) {
            return ROUNDED;
        }
        if (variant == 1) {
            return TALL_LAYERED;
        }
        return WIDE_NATURAL;
    }

    private boolean hasRoom(WorldGenLevel level, BlockPos origin, int height, TreeVariant variant) {
        for (int y = 0; y <= height + 4; y++) {
            int radius = y < height - 6 ? 0 : variant.roomRadius();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (!canReplace(level, origin.offset(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void placeRoots(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState log, Set<BlockPos> roots, TreeVariant variant) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (random.nextFloat() > variant.rootChance()) {
                continue;
            }
            int length = 1 + random.nextInt(variant.maxRootLength());
            for (int step = 1; step <= length; step++) {
                BlockPos rootPos = origin.relative(direction, step);
                if (canReplace(level, rootPos) && !level.getBlockState(rootPos.below()).isAir()) {
                    placeLog(level, rootPos, log, roots);
                }
            }
        }
    }

    private void placeCanopy(WorldGenLevel level, RandomSource random, BlockPos origin, int height, TreeVariant variant, BlockState log, BlockState leaf, Set<BlockPos> logs, Set<BlockPos> leaves) {
        int top = height;
        switch (variant.shape()) {
            case 1 -> placeTallLayeredCanopy(level, random, origin, top, log, leaf, logs, leaves);
            case 2 -> placeWideNaturalCanopy(level, random, origin, top, log, leaf, logs, leaves);
            case 3 -> placeAncestralCanopy(level, random, origin, top, log, leaf, logs, leaves);
            default -> placeRoundedCanopy(level, random, origin, top, leaf, leaves);
        }
    }

    private void placeRoundedCanopy(WorldGenLevel level, RandomSource random, BlockPos origin, int top, BlockState leaf, Set<BlockPos> leaves) {
        placeLeafLayer(level, random, origin, leaf, leaves, top - 5, 2, 0.72F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 4, 3, 0.86F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 3, 4, 0.94F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 2, 5, 0.95F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 1, 4, 0.98F);
        placeLeafLayer(level, random, origin, leaf, leaves, top, 4, 0.96F);
        placeLeafLayer(level, random, origin, leaf, leaves, top + 1, 3, 0.90F);
        placeLeafLayer(level, random, origin, leaf, leaves, top + 2, 2, 0.74F);
        addLeafClusters(level, random, origin.above(top - 2), leaf, leaves, 3, 2, 0.78F);
    }

    private void placeTallLayeredCanopy(WorldGenLevel level, RandomSource random, BlockPos origin, int top, BlockState log, BlockState leaf, Set<BlockPos> logs, Set<BlockPos> leaves) {
        placeLeafLayer(level, random, origin, leaf, leaves, top - 7, 2, 0.62F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 6, 3, 0.78F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 5, 2, 0.86F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 4, 4, 0.88F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 3, 3, 0.94F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 2, 4, 0.94F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 1, 3, 0.96F);
        placeLeafLayer(level, random, origin, leaf, leaves, top, 3, 0.94F);
        placeLeafLayer(level, random, origin, leaf, leaves, top + 1, 2, 0.82F);
        addBranches(level, random, origin, top - 5, log, logs, 2, 0.65F);
        addLeafClusters(level, random, origin.above(top - 3), leaf, leaves, 3, 2, 0.72F);
    }

    private void placeWideNaturalCanopy(WorldGenLevel level, RandomSource random, BlockPos origin, int top, BlockState log, BlockState leaf, Set<BlockPos> logs, Set<BlockPos> leaves) {
        placeLeafLayer(level, random, origin, leaf, leaves, top - 4, 4, 0.76F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 3, 5, 0.88F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 2, 6, 0.92F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 1, 5, 0.96F);
        placeLeafLayer(level, random, origin, leaf, leaves, top, 4, 0.94F);
        placeLeafLayer(level, random, origin, leaf, leaves, top + 1, 3, 0.80F);
        addBranches(level, random, origin, top - 4, log, logs, 3, 0.82F);
        addLeafClusters(level, random, origin.above(top - 2), leaf, leaves, 4, 2, 0.76F);
    }

    private void placeAncestralCanopy(WorldGenLevel level, RandomSource random, BlockPos origin, int top, BlockState log, BlockState leaf, Set<BlockPos> logs, Set<BlockPos> leaves) {
        placeLeafLayer(level, random, origin, leaf, leaves, top - 8, 3, 0.66F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 7, 4, 0.78F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 6, 5, 0.86F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 5, 6, 0.92F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 4, 7, 0.92F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 3, 6, 0.96F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 2, 6, 0.96F);
        placeLeafLayer(level, random, origin, leaf, leaves, top - 1, 5, 0.94F);
        placeLeafLayer(level, random, origin, leaf, leaves, top, 4, 0.90F);
        placeLeafLayer(level, random, origin, leaf, leaves, top + 1, 3, 0.78F);
        placeLeafLayer(level, random, origin, leaf, leaves, top + 2, 2, 0.62F);
        addBranches(level, random, origin, top - 7, log, logs, 4, 1.0F);
        addLeafClusters(level, random, origin.above(top - 4), leaf, leaves, 5, 3, 0.84F);
    }

    private void placeLeafLayer(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState leaf, Set<BlockPos> leaves, int y, int radius, float density) {
        if (y < 0) {
            return;
        }
        float radiusSq = radius * radius + 0.35F;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                float distanceSq = x * x + z * z;
                if (distanceSq > radiusSq) {
                    continue;
                }
                boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
                float localDensity = edge ? density - 0.18F : density;
                if (random.nextFloat() <= localDensity) {
                    placeLeaf(level, origin.offset(x, y, z), leaf, leaves);
                }
            }
        }
    }

    private void addBranches(WorldGenLevel level, RandomSource random, BlockPos origin, int y, BlockState log, Set<BlockPos> logs, int maxLength, float chance) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (random.nextFloat() > chance) {
                continue;
            }
            int length = 2 + random.nextInt(maxLength);
            for (int step = 1; step <= length; step++) {
                BlockPos branchPos = origin.above(y + step / 3).relative(direction, step);
                placeLog(level, branchPos, log, logs);
            }
        }
    }

    private void addLeafClusters(WorldGenLevel level, RandomSource random, BlockPos center, BlockState leaf, Set<BlockPos> leaves, int distance, int radius, float density) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (random.nextFloat() > 0.75F) {
                continue;
            }
            BlockPos clusterCenter = center.relative(direction, distance).above(random.nextInt(2));
            placeLeafLayer(level, random, clusterCenter, leaf, leaves, 0, radius, density);
        }
    }

    private void placeLog(WorldGenLevel level, BlockPos pos, BlockState log, Set<BlockPos> logs) {
        if (canReplace(level, pos)) {
            logs.add(pos.immutable());
            level.setBlock(pos, log, 19);
        }
    }

    private void placeLeaf(WorldGenLevel level, BlockPos pos, BlockState leaf, Set<BlockPos> leaves) {
        if (canReplace(level, pos)) {
            leaves.add(pos.immutable());
            level.setBlock(pos, leaf, 19);
        }
    }

    private void updateLeafDistances(WorldGenLevel level, Set<BlockPos> leaves, Set<BlockPos> logs, Set<BlockPos> roots) {
        Set<BlockPos> anchors = new HashSet<>(logs);
        anchors.addAll(roots);
        for (BlockPos leafPos : leaves) {
            int distance = 7;
            for (BlockPos anchor : anchors) {
                int currentDistance = Math.abs(leafPos.getX() - anchor.getX())
                        + Math.abs(leafPos.getY() - anchor.getY())
                        + Math.abs(leafPos.getZ() - anchor.getZ());
                if (currentDistance < distance) {
                    distance = currentDistance;
                }
            }
            level.setBlock(leafPos, withLeafDistance(level.getBlockState(leafPos), Math.max(1, Math.min(6, distance))), 19);
        }
    }

    private boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS);
    }

    private BlockState withLeafDistance(BlockState state, int distance) {
        if (state.hasProperty(LeavesBlock.DISTANCE)) {
            return state.setValue(LeavesBlock.DISTANCE, distance);
        }
        return state;
    }

    private record TreeVariant(int minHeight, int maxHeight, int roomRadius, float rootChance, int maxRootLength, int shape) {
    }
}
