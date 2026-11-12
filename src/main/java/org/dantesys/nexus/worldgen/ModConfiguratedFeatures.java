package org.dantesys.nexus.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.*;
import net.minecraft.util.valueproviders.ConstantInt;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.worldgen.features.ModFeatures;

import java.util.List;

public class ModConfiguratedFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> IGNEA_KEY = registerKey("ignea");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TEMPESTADE_KEY = registerKey("tempestade");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SOMBRIA_KEY = registerKey("sombria");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LUZ_KEY = registerKey("luz");
    public static final ResourceKey<ConfiguredFeature<?, ?>> METAL_KEY = registerKey("metal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> METAL_PILAR_KEY = registerKey("metal_pilar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> VIDA_KEY = registerKey("vida");
    public static final ResourceKey<ConfiguredFeature<?, ?>> VIDA_LAKE_KEY = registerKey("vida_lake");
    public static final ResourceKey<ConfiguredFeature<?, ?>> IGNEA_LAVA_LAKE_KEY = registerKey("ignea_lava_lake");
    public static final ResourceKey<ConfiguredFeature<?, ?>> IGNEA_MAGMA_CLUSTER_KEY = registerKey("ignea_magma_cluster");
    public static final ResourceKey<ConfiguredFeature<?, ?>> IGNEA_ROCK_KEY = registerKey("ignea_rock");
    public static final ResourceKey<ConfiguredFeature<?, ?>> IGNEA_MINI_VOLCANO_KEY = registerKey("ignea_mini_volcano");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        registerIgneaTree(context);
        registerTempestadeTree(context);
        registerSombriaTree(context);
        registerLuzTree(context);
        registerMetalTree(context);
        register(context, VIDA_KEY, ModFeatures.VIDA_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.VIDA_LOG.get()),
                new StraightTrunkPlacer(8, 4, 0),
                BlockStateProvider.simple(ModBlocks.VIDA_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), 5),
                new TwoLayersFeatureSize(1, 0, 1)
        ).decorators(List.of(new BeehiveDecorator(0.12F))).ignoreVines().build());
        register(context, VIDA_LAKE_KEY, Feature.LAKE, new LakeFeature.Configuration(
                BlockStateProvider.simple(Blocks.WATER.defaultBlockState()),
                BlockStateProvider.simple(Blocks.DIRT.defaultBlockState())
        ));
        register(context, IGNEA_LAVA_LAKE_KEY, Feature.LAKE, new LakeFeature.Configuration(
                BlockStateProvider.simple(Blocks.LAVA.defaultBlockState()),
                BlockStateProvider.simple(Blocks.BLACKSTONE.defaultBlockState())
        ));
        register(context, IGNEA_MAGMA_CLUSTER_KEY, ModFeatures.IGNEA_MAGMA_CLUSTER.get(), NoneFeatureConfiguration.INSTANCE);
        register(context, IGNEA_ROCK_KEY, Feature.BLOCK_BLOB, new BlockBlobConfiguration(Blocks.BLACKSTONE.defaultBlockState(), BlockPredicate.alwaysTrue()));
        register(context, IGNEA_MINI_VOLCANO_KEY, ModFeatures.IGNEA_MINI_VOLCANO.get(), NoneFeatureConfiguration.INSTANCE);
        registerMetalClusters(context);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }

    private static void registerIgneaTree(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Holder<ConfiguredFeature<?, ?>> flameCrown = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.IGNEA_LOG.get()),
                new StraightTrunkPlacer(6, 4, 0),
                BlockStateProvider.simple(ModBlocks.IGNEA_LEAVES.get()),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> narrowFlame = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.IGNEA_LOG.get()),
                new StraightTrunkPlacer(7, 3, 0),
                BlockStateProvider.simple(ModBlocks.IGNEA_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(4, 6)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> leaningFlame = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.IGNEA_LOG.get()),
                new BendingTrunkPlacer(6, 4, 0, 4, UniformInt.of(1, 2)),
                BlockStateProvider.simple(ModBlocks.IGNEA_LEAVES.get()),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 4)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> forkedFlame = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.IGNEA_LOG.get()),
                new ForkingTrunkPlacer(6, 3, 0),
                BlockStateProvider.simple(ModBlocks.IGNEA_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));

        register(context, IGNEA_KEY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(narrowFlame), 0.28F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(leaningFlame), 0.28F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(forkedFlame), 0.20F)
                ),
                PlacementUtils.inlinePlaced(flameCrown)
        ));
    }

    private static void registerTempestadeTree(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Holder<ConfiguredFeature<?, ?>> sweptCrown = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LOG.get()),
                new StraightTrunkPlacer(8, 4, 0),
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LEAVES.get()),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 26),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> leaningGale = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LOG.get()),
                new BendingTrunkPlacer(8, 4, 0, 5, UniformInt.of(1, 2)),
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LEAVES.get()),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 22),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> forkedWind = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LOG.get()),
                new ForkingTrunkPlacer(8, 3, 1),
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> highGust = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LOG.get()),
                new BendingTrunkPlacer(9, 3, 0, 5, UniformInt.of(1, 2)),
                BlockStateProvider.simple(ModBlocks.TEMPESTADE_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));

        register(context, TEMPESTADE_KEY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(leaningGale), 0.32F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(forkedWind), 0.24F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(highGust), 0.24F)
                ),
                PlacementUtils.inlinePlaced(sweptCrown)
        ));
    }

    private static void registerSombriaTree(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Holder<ConfiguredFeature<?, ?>> warpedCrown = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LOG.get()),
                new ForkingTrunkPlacer(7, 4, 0),
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> leaningShade = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LOG.get()),
                new BendingTrunkPlacer(7, 4, 0, 5, UniformInt.of(1, 2)),
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LEAVES.get()),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 24),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> splitShade = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LOG.get()),
                new ForkingTrunkPlacer(8, 3, 0),
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> raggedCanopy = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LOG.get()),
                new StraightTrunkPlacer(8, 3, 0),
                BlockStateProvider.simple(ModBlocks.SOMBRIA_LEAVES.get()),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), UniformInt.of(2, 4), 22),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));

        register(context, SOMBRIA_KEY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(leaningShade), 0.28F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(splitShade), 0.26F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(raggedCanopy), 0.24F)
                ),
                PlacementUtils.inlinePlaced(warpedCrown)
        ));
    }

    private static void registerLuzTree(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Holder<ConfiguredFeature<?, ?>> luminousSpire = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.LUZ_LOG.get()),
                new StraightTrunkPlacer(9, 4, 0),
                BlockStateProvider.simple(ModBlocks.LUZ_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(4, 6)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> refinedCrown = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.LUZ_LOG.get()),
                new StraightTrunkPlacer(10, 3, 0),
                BlockStateProvider.simple(ModBlocks.LUZ_LEAVES.get()),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(4, 5)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> balancedColumn = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.LUZ_LOG.get()),
                new StraightTrunkPlacer(9, 3, 1),
                BlockStateProvider.simple(ModBlocks.LUZ_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(3, 5)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> compactHalo = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.LUZ_LOG.get()),
                new StraightTrunkPlacer(9, 2, 0),
                BlockStateProvider.simple(ModBlocks.LUZ_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));

        register(context, LUZ_KEY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(refinedCrown), 0.30F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(balancedColumn), 0.28F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(compactHalo), 0.18F)
                ),
                PlacementUtils.inlinePlaced(luminousSpire)
        ));
    }

    private static void registerMetalTree(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Holder<ConfiguredFeature<?, ?>> facetedCrown = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.METAL_LOG.get()),
                new StraightTrunkPlacer(7, 3, 0),
                BlockStateProvider.simple(ModBlocks.METAL_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> narrowCrystal = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.METAL_LOG.get()),
                new StraightTrunkPlacer(8, 2, 0),
                BlockStateProvider.simple(ModBlocks.METAL_LEAVES.get()),
                new SpruceFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(2, 3)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> tieredCrystal = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.METAL_LOG.get()),
                new StraightTrunkPlacer(7, 2, 1),
                BlockStateProvider.simple(ModBlocks.METAL_LEAVES.get()),
                new PineFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), UniformInt.of(2, 4)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));
        Holder<ConfiguredFeature<?, ?>> compactCrown = Holder.direct(new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.METAL_LOG.get()),
                new StraightTrunkPlacer(7, 2, 0),
                BlockStateProvider.simple(ModBlocks.METAL_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(1), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1)
        ).ignoreVines().build()));

        register(context, METAL_KEY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(narrowCrystal), 0.30F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(tieredCrystal), 0.28F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(compactCrown), 0.22F)
                ),
                PlacementUtils.inlinePlaced(facetedCrown)
        ));
    }

    private static void registerMetalClusters(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Holder<ConfiguredFeature<?, ?>> rawIron = Holder.direct(new ConfiguredFeature<>(Feature.BLOCK_BLOB,
                new BlockBlobConfiguration(Blocks.RAW_IRON_BLOCK.defaultBlockState(), BlockPredicate.alwaysTrue())));
        Holder<ConfiguredFeature<?, ?>> rawCopper = Holder.direct(new ConfiguredFeature<>(Feature.BLOCK_BLOB,
                new BlockBlobConfiguration(Blocks.RAW_COPPER_BLOCK.defaultBlockState(), BlockPredicate.alwaysTrue())));
        Holder<ConfiguredFeature<?, ?>> rawGold = Holder.direct(new ConfiguredFeature<>(Feature.BLOCK_BLOB,
                new BlockBlobConfiguration(Blocks.RAW_GOLD_BLOCK.defaultBlockState(), BlockPredicate.alwaysTrue())));

        register(context, METAL_PILAR_KEY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(rawCopper), 0.42F),
                        new WeightedPlacedFeature(PlacementUtils.inlinePlaced(rawGold), 0.18F)
                ),
                PlacementUtils.inlinePlaced(rawIron)
        ));
    }
}
