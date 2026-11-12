package org.dantesys.nexus.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BlockStateProviders;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.BendingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.worldgen.features.IgneaMagmaClusterFeature;
import org.dantesys.nexus.worldgen.features.IgneaMiniVolcanoFeature;
import org.dantesys.nexus.worldgen.features.VidaTreeFeature;

import java.util.List;

/**
 * Desde a 26.3 não existe mais "ConfiguredFeature": a própria Feature já carrega a configuração
 * e fica no registro {@code worldgen/feature}.
 */
public class ModConfiguratedFeatures {
    public static final ResourceKey<Feature> IGNEA_KEY = registerKey("ignea");
    public static final ResourceKey<Feature> TEMPESTADE_KEY = registerKey("tempestade");
    public static final ResourceKey<Feature> SOMBRIA_KEY = registerKey("sombria");
    public static final ResourceKey<Feature> LUZ_KEY = registerKey("luz");
    public static final ResourceKey<Feature> METAL_KEY = registerKey("metal");
    public static final ResourceKey<Feature> METAL_PILAR_KEY = registerKey("metal_pilar");
    public static final ResourceKey<Feature> VIDA_KEY = registerKey("vida");
    public static final ResourceKey<Feature> VIDA_LAKE_KEY = registerKey("vida_lake");
    public static final ResourceKey<Feature> IGNEA_LAVA_LAKE_KEY = registerKey("ignea_lava_lake");
    public static final ResourceKey<Feature> IGNEA_MAGMA_CLUSTER_KEY = registerKey("ignea_magma_cluster");
    public static final ResourceKey<Feature> IGNEA_ROCK_KEY = registerKey("ignea_rock");
    public static final ResourceKey<Feature> IGNEA_MINI_VOLCANO_KEY = registerKey("ignea_mini_volcano");

    public static void bootstrap(BootstrapContext<Feature> context) {
        Holder<BlockStateProvider> solo = context.lookup(Registries.BLOCK_STATE_PROVIDER)
                .getOrThrow(BlockStateProviders.SOIL_BENEATH_TREE);

        registerIgneaTree(context, solo);
        registerTempestadeTree(context, solo);
        registerSombriaTree(context, solo);
        registerLuzTree(context, solo);
        registerMetalTree(context, solo);
        context.register(VIDA_KEY, new VidaTreeFeature(
                BlockStateProvider.holderOf(ModBlocks.VIDA_LOG.get()),
                BlockStateProvider.holderOf(ModBlocks.VIDA_LEAVES.get()),
                List.of(new BeehiveDecorator(0.12F))));
        context.register(VIDA_LAKE_KEY, lago(Blocks.WATER, Blocks.DIRT));
        context.register(IGNEA_LAVA_LAKE_KEY, lago(Blocks.LAVA, Blocks.BLACKSTONE));
        context.register(IGNEA_MAGMA_CLUSTER_KEY, IgneaMagmaClusterFeature.INSTANCE);
        context.register(IGNEA_ROCK_KEY, new BlockBlobFeature(Blocks.BLACKSTONE.defaultBlockState(), BlockPredicate.alwaysTrue()));
        context.register(IGNEA_MINI_VOLCANO_KEY, IgneaMiniVolcanoFeature.INSTANCE);
        registerMetalClusters(context);
    }

    public static ResourceKey<Feature> registerKey(String name) {
        return ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }

    private static LakeFeature lago(Block fluido, Block borda) {
        // Mesmas regras de substituição do lago de lava vanilla.
        return new LakeFeature(
                BlockStateProvider.holderOf(fluido),
                BlockStateProvider.holderOf(borda),
                BlockPredicate.alwaysTrue(),
                BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.FEATURES_CANNOT_REPLACE)),
                BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)));
    }

    private static Holder<Feature> arvore(Block tronco, Block folhas, TrunkPlacer trunk, FoliagePlacer foliage,
                                          Holder<BlockStateProvider> solo) {
        return Holder.direct(new TreeFeature.Builder(
                BlockStateProvider.of(tronco), trunk,
                BlockStateProvider.of(folhas), foliage,
                new TwoLayersFeatureSize(1, 0, 1),
                solo
        ).ignoreVines().build());
    }

    private static RandomSelectorFeature seletor(Holder<Feature> padrao, Holder<Feature> a, float chanceA,
                                                 Holder<Feature> b, float chanceB, Holder<Feature> c, float chanceC) {
        return new RandomSelectorFeature(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(a), chanceA),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(b), chanceB),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(c), chanceC)
                ),
                PlacementUtils.inlinePlaced(padrao));
    }

    private static void registerIgneaTree(BootstrapContext<Feature> context, Holder<BlockStateProvider> solo) {
        Block log = ModBlocks.IGNEA_LOG.get(), leaves = ModBlocks.IGNEA_LEAVES.get();
        Holder<Feature> flameCrown = arvore(log, leaves, new StraightTrunkPlacer(6, 4, 0),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)), solo);
        Holder<Feature> narrowFlame = arvore(log, leaves, new StraightTrunkPlacer(7, 3, 0),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(4, 6)), solo);
        Holder<Feature> leaningFlame = arvore(log, leaves, new BendingTrunkPlacer(6, 4, 0, 4, UniformInt.of(1, 2)),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 4)), solo);
        Holder<Feature> forkedFlame = arvore(log, leaves, new ForkingTrunkPlacer(6, 3, 0),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)), solo);

        context.register(IGNEA_KEY, seletor(flameCrown, narrowFlame, 0.28F, leaningFlame, 0.28F, forkedFlame, 0.20F));
    }

    private static void registerTempestadeTree(BootstrapContext<Feature> context, Holder<BlockStateProvider> solo) {
        Block log = ModBlocks.TEMPESTADE_LOG.get(), leaves = ModBlocks.TEMPESTADE_LEAVES.get();
        Holder<Feature> sweptCrown = arvore(log, leaves, new StraightTrunkPlacer(8, 4, 0),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 26), solo);
        Holder<Feature> leaningGale = arvore(log, leaves, new BendingTrunkPlacer(8, 4, 0, 5, UniformInt.of(1, 2)),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 22), solo);
        Holder<Feature> forkedWind = arvore(log, leaves, new ForkingTrunkPlacer(8, 3, 1),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3), solo);
        Holder<Feature> highGust = arvore(log, leaves, new BendingTrunkPlacer(9, 3, 0, 5, UniformInt.of(1, 2)),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)), solo);

        context.register(TEMPESTADE_KEY, seletor(sweptCrown, leaningGale, 0.32F, forkedWind, 0.24F, highGust, 0.24F));
    }

    private static void registerSombriaTree(BootstrapContext<Feature> context, Holder<BlockStateProvider> solo) {
        Block log = ModBlocks.SOMBRIA_LOG.get(), leaves = ModBlocks.SOMBRIA_LEAVES.get();
        Holder<Feature> warpedCrown = arvore(log, leaves, new ForkingTrunkPlacer(7, 4, 0),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3), solo);
        Holder<Feature> leaningShade = arvore(log, leaves, new BendingTrunkPlacer(7, 4, 0, 5, UniformInt.of(1, 2)),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 24), solo);
        Holder<Feature> splitShade = arvore(log, leaves, new ForkingTrunkPlacer(8, 3, 0),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3), solo);
        Holder<Feature> raggedCanopy = arvore(log, leaves, new StraightTrunkPlacer(8, 3, 0),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 22), solo);

        context.register(SOMBRIA_KEY, seletor(warpedCrown, leaningShade, 0.28F, splitShade, 0.26F, raggedCanopy, 0.24F));
    }

    private static void registerLuzTree(BootstrapContext<Feature> context, Holder<BlockStateProvider> solo) {
        Block log = ModBlocks.LUZ_LOG.get(), leaves = ModBlocks.LUZ_LEAVES.get();
        Holder<Feature> luminousSpire = arvore(log, leaves, new StraightTrunkPlacer(9, 4, 0),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(4, 6)), solo);
        Holder<Feature> refinedCrown = arvore(log, leaves, new StraightTrunkPlacer(10, 3, 0),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(4, 5)), solo);
        Holder<Feature> balancedColumn = arvore(log, leaves, new StraightTrunkPlacer(9, 3, 1),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)), solo);
        Holder<Feature> compactHalo = arvore(log, leaves, new StraightTrunkPlacer(9, 2, 0),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2), solo);

        context.register(LUZ_KEY, seletor(luminousSpire, refinedCrown, 0.30F, balancedColumn, 0.28F, compactHalo, 0.18F));
    }

    private static void registerMetalTree(BootstrapContext<Feature> context, Holder<BlockStateProvider> solo) {
        Block log = ModBlocks.METAL_LOG.get(), leaves = ModBlocks.METAL_LEAVES.get();
        Holder<Feature> facetedCrown = arvore(log, leaves, new StraightTrunkPlacer(7, 3, 0),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2), solo);
        Holder<Feature> narrowCrystal = arvore(log, leaves, new StraightTrunkPlacer(8, 2, 0),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(2, 3)), solo);
        Holder<Feature> tieredCrystal = arvore(log, leaves, new StraightTrunkPlacer(7, 2, 1),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(2, 4)), solo);
        Holder<Feature> compactCrown = arvore(log, leaves, new StraightTrunkPlacer(7, 2, 0),
                new BlobFoliagePlacer(ConstantInt.of(1), ConstantInt.of(0), 2), solo);

        context.register(METAL_KEY, seletor(facetedCrown, narrowCrystal, 0.30F, tieredCrystal, 0.28F, compactCrown, 0.22F));
    }

    private static void registerMetalClusters(BootstrapContext<Feature> context) {
        Holder<Feature> rawIron = Holder.direct(new BlockBlobFeature(Blocks.RAW_IRON_BLOCK.defaultBlockState(), BlockPredicate.alwaysTrue()));
        Holder<Feature> rawCopper = Holder.direct(new BlockBlobFeature(Blocks.RAW_COPPER_BLOCK.defaultBlockState(), BlockPredicate.alwaysTrue()));
        Holder<Feature> rawGold = Holder.direct(new BlockBlobFeature(Blocks.RAW_GOLD_BLOCK.defaultBlockState(), BlockPredicate.alwaysTrue()));

        context.register(METAL_PILAR_KEY, new RandomSelectorFeature(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(rawCopper), 0.42F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(rawGold), 0.18F)
                ),
                PlacementUtils.inlinePlaced(rawIron)));
    }
}
